from statefun import StatefulFunctions
from statefun import RequestReplyHandler
from statefun import kafka_egress_record

import typing

from messages_pb2 import Supply, Restock, Purchase, SupplyChanged
from google.protobuf.any_pb2 import Any

functions = StatefulFunctions()

@functions.bind("shopping/product")
def product(context, message: typing.Union[Restock, Purchase]):

    if type(message) is Restock:
        quantity_to_change = message.quantity
    elif type(message) is Purchase:
        quantity_to_change = -message.quantity
    else:
        raise TypeError(f'Unexpected message type {type(message)}')

    state = context.state('supply').unpack(Supply)
    if not state:
        state = Supply()
        state.id = message.id
        state.quantity = quantity_to_change
    else:
        state.quantity += quantity_to_change

    context.state('supply').pack(state)

    event = SupplyChanged()
    event.id = message.id
    event.total_quantity = state.quantity
    event.difference = quantity_to_change

    egress_message = kafka_egress_record(topic="supply-changed", key=message.id, value=event)
    context.pack_and_send_egress("shopping/supply-changed", egress_message)


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
