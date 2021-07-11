from statefun import make_protobuf_type
from shopping_pb2 import Basket, Supply

SUPPLY_TYPE = make_protobuf_type(Supply, "example")
SUPPLY_RESTOCK_TYPE = make_protobuf_type(Supply.Restock, "example")
SUPPLY_REQUEST_TYPE = make_protobuf_type(Supply.Request, "example")
SUPPLY_RECEIVED_TYPE = make_protobuf_type(Supply.Received, "example")
SUPPLY_CHANGED_TYPE = make_protobuf_type(Supply.Changed, "example")

BASKET_TYPE = make_protobuf_type(Basket, "example")
BASKET_ADD_TYPE = make_protobuf_type(Basket.Add, "example")
BASKET_SNAPSHOT_TYPE = make_protobuf_type(Basket.Snapshot, "example")
