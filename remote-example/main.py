import typing

from statefun import StatefulFunctions
from statefun import RequestReplyHandler
from statefun import message_builder, kafka_egress_message
from statefun import Message
from statefun import ValueSpec

from shopping_types import *
from shopping_pb2 import Availability, Basket, Supply
from pluralizer import Pluralizer

functions = StatefulFunctions()
pluralizer = Pluralizer()

@functions.bind("shopping/supply", specs=[ValueSpec(name="supply", type=SUPPLY_TYPE)])
def supply(context, message: Message):
    if message.is_type(SUPPLY_RESTOCK_TYPE):
        restock = message.as_type(SUPPLY_RESTOCK_TYPE)
        app.logger.debug(f"{context.address.typename} " +
            f"Adding {pluralizer.pluralize(restock.id, restock.quantity, True)}")

        # Update our supply
        supply = context.storage.supply
        quantity_to_change = restock.quantity

        if not supply:
            supply = Supply()
            supply.quantity = quantity_to_change
            context.storage.supply = supply
        else:
            supply.quantity += quantity_to_change

        # Record the change event
        event = Supply.Changed()
        event.id = restock.id
        event.total_quantity = supply.quantity
        event.difference = quantity_to_change

        context.send_egress(
            kafka_egress_message(
                typename="shopping/supply-changed",
                topic="supply-changed",
                key=event.id,
                value=event,
                value_type=SUPPLY_CHANGED_TYPE))

    elif message.is_type(SUPPLY_REQUEST_TYPE):
        request = message.as_type(SUPPLY_REQUEST_TYPE)
        app.logger.debug(f"{context.address.typename} " +
            f"{context.caller.id} is requesting {pluralizer.pluralize(request.id, request.quantity, True)}")

        # Prepare a reply to the request for items
        supply = context.storage.supply
        received = Supply.Received()
        received.id = request.id

        if not supply or supply.quantity - request.quantity < 0:
            # We don't have enough availability
            received.quantity = 0
            received.status = Availability.OUT_OF_STOCK
        else:
            received.quantity = request.quantity
            received.status = Availability.IN_STOCK

            # Update our supply
            supply.quantity -= request.quantity

            # Record the change event
            event = Supply.Changed()
            event.id = request.id
            event.total_quantity = supply.quantity
            event.difference = -request.quantity

            context.send_egress(
                kafka_egress_message(
                    typename="shopping/supply-changed",
                    topic="supply-changed",
                    key=event.id,
                    value=event,
                    value_type=SUPPLY_CHANGED_TYPE))

        # Reply to the caller
        context.send(
            message_builder(
                target_typename=context.caller.typename,
                target_id=context.caller.id,
                value=received,
                value_type=SUPPLY_RECEIVED_TYPE))

    else:
        raise TypeError(f'Unexpected message type {message.value_typename()}')


@functions.bind("shopping/basket", specs=[ValueSpec(name="basket", type=BASKET_TYPE)])
def basket(context, message: Message):

    if message.is_type(BASKET_ADD_TYPE):
        add = message.as_type(BASKET_ADD_TYPE)
        request = Supply.Request()
        request.id = add.product_id
        request.quantity = add.quantity

        context.send(
            message_builder(
                target_typename="shopping/supply",
                target_id=add.product_id,
                value=request,
                value_type=SUPPLY_REQUEST_TYPE))

    elif message.is_type(SUPPLY_RECEIVED_TYPE):
        received = message.as_type(SUPPLY_RECEIVED_TYPE)

        if received.status == Availability.OUT_OF_STOCK:
            app.logger.warn(f"{context.address.typename} " +
                f"OUT OF STOCK! Not enough {pluralizer.plural(received.id)} available")
        else:
            app.logger.debug(f"{context.address.typename} " +
                f"Received {pluralizer.pluralize(received.id, received.quantity, True)}")

            basket = context.storage.basket
            if not basket:
                basket = Basket()
                context.storage.basket = basket

            item = next((item for item in basket.items if item.id == received.id), None)
            if not item:
                item = Basket.Item()
                item.id = received.id
                item.quantity = received.quantity
                basket.items.append(item)

            snapshot = Basket.Snapshot()
            snapshot.id = context.address.id
            for item in basket.items:
                snapshot.items.append(item)

            context.send_egress(
                kafka_egress_message(
                    typename="shopping/basket-snapshots",
                    topic="basket-snapshots",
                    key=snapshot.id,
                    value=snapshot,
                    value_type=BASKET_SNAPSHOT_TYPE))

    else:
        raise TypeError(f'Unexpected message type {message.value_typename()}')


handler = RequestReplyHandler(functions)

#
# Serve the endpoint
#

from flask import request
from flask import make_response
from flask import Flask

app = Flask(__name__)

@app.route('/statefun', methods=['POST'])
def handle():
    response_data = handler.handle_sync(request.data)
    response = make_response(response_data)
    response.headers.set('Content-Type', 'application/octet-stream')
    return response


if __name__ == "__main__":
    app.run()
