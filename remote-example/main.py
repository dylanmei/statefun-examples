import typing

from statefun import StatefulFunctions
from statefun import RequestReplyHandler
from statefun import kafka_egress_record

from shopping_pb2 import Supply, Basket, Availability
from google.protobuf.any_pb2 import Any
from pluralizer import Pluralizer

functions = StatefulFunctions()
pluralizer = Pluralizer()

@functions.bind("shopping/supply")
def supply(context, message: typing.Union[Supply.Restock, Supply.Request]):
    if type(message) is Supply.Restock:
        app.logger.debug(f"{context.address.typename()} Adding {pluralizer.pluralize(message.id, message.quantity, True)}")

        quantity_to_change = message.quantity
        state = context.state('supply').unpack(Supply)
        if not state:
            state = Supply()
            state.quantity = quantity_to_change
        else:
            state.quantity += quantity_to_change

        # Update our supply
        context.state('supply').pack(state)

        # Record the change event
        event = Supply.Changed()
        event.id = message.id
        event.total_quantity = state.quantity
        event.difference = quantity_to_change

        egress_message = kafka_egress_record(topic="supply-changed", key=message.id, value=event)
        context.pack_and_send_egress("shopping/supply-changed", egress_message)

    elif type(message) is Supply.Request:
        app.logger.debug(f"{context.address.typename()} {context.caller.identity} is requesting {pluralizer.pluralize(message.id, message.quantity, True)}")

        state = context.state('supply').unpack(Supply)
        if not state:
            current_quantity = 0
        else:
            current_quantity = state.quantity

        # Prepare a reply to the request for items
        received = Supply.Received()
        received.id = message.id

        if current_quantity - message.quantity < 0:
            # We don't have enough availability
            received.quantity = 0
            received.status = Availability.OUT_OF_STOCK
        else:
            received.quantity = message.quantity
            received.status = Availability.IN_STOCK

            # Update our supply
            state.quantity -= message.quantity
            context.state('supply').pack(state)

            # Record the change event
            event = Supply.Changed()
            event.id = message.id
            event.total_quantity = state.quantity
            event.difference = -message.quantity

            egress_message = kafka_egress_record(topic="supply-changed", key=event.id, value=event)
            context.pack_and_send_egress("shopping/supply-changed", egress_message)

        # Reply to the requestor
        context.pack_and_reply(received)

    else:
        raise TypeError(f'Unexpected message type {type(message)}')


@functions.bind("shopping/basket")
def supply(context, message: typing.Union[Basket.Add, Supply.Received]):

    if type(message) is Basket.Add:
        request = Supply.Request()
        request.id = message.product_id
        request.quantity = message.quantity

        context.pack_and_send("shopping/supply", message.product_id, request)

    elif type(message) is Supply.Received:
        if message.status == Availability.OUT_OF_STOCK:
            app.logger.warn(f"{context.address.typename()} OUT OF STOCK! Not enough {pluralizer.plural(message.id)} available")
        else:
            app.logger.debug(f"{context.address.typename()} Received {pluralizer.pluralize(message.id, message.quantity, True)}")
            state = context.state('basket').unpack(Basket)
            if not state:
                state = Basket()
            for item in state.items:
                if item.id == message.id:
                    item.quantity += message.quantity
                    break
            else:
                item = Basket.Item()
                item.id = message.id
                item.quantity = message.quantity
                state.items.append(item)

            context.state('basket').pack(state)

            snapshot = Basket.Snapshot()
            snapshot.id = context.address.identity
            for item in state.items:
                snapshot.items.append(item)

            egress_message = kafka_egress_record(topic="basket-snapshots", key=snapshot.id, value=snapshot)
            context.pack_and_send_egress("shopping/basket-snapshots", egress_message)

    else:
        raise TypeError(f'Unexpected message type {type(message)}')


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
    response_data = handler(request.data)
    response = make_response(response_data)
    response.headers.set('Content-Type', 'application/octet-stream')
    return response


if __name__ == "__main__":
    app.run()
