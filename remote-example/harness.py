import signal
import sys
import time
import threading
import random

from kafka.errors import NoBrokersAvailable
from shopping_pb2 import Supply, Basket
from beautifultable import BeautifulTable
from pluralizer import Pluralizer

from kafka import KafkaProducer
from kafka import KafkaConsumer

KAFKA_ADDR = "localhost:9092"
PRODUCTS = {'👞️': "Shoes", '🧥️': "Coat", '🎒️': "Backpack", '👛️': "Purse", '🎩️': "Hat", '⌚️': "Watch", '👖️': "Pants", '🌂️': "Umbrella"}
DELAY_SECONDS = 5
pluralizer = Pluralizer()


def produce_restock_messages():
    producer = KafkaProducer(bootstrap_servers=[KAFKA_ADDR])
    for icon, name in PRODUCTS.items():
        message = Supply.Restock()
        message.id = name
        message.quantity = 10

        print(f"{icon_of(message.id)} Restocking {pluralizer.pluralize(message.id, message.quantity, True)}.")
        key = message.id.encode('utf-8')
        val = message.SerializeToString()
        producer.send(topic='restock', key=key, value=val)
    producer.flush()


def produce_add_to_basket_messages():
    producer = KafkaProducer(bootstrap_servers=[KAFKA_ADDR])

    for message in generate_add_to_basket_messages():
        print(f"{icon_of(message.product_id)} Adding {pluralizer.pluralize(message.product_id, message.quantity, True)} to {message.id}'s shopping basket...")
        key = message.id.encode('utf-8')
        val = message.SerializeToString()
        producer.send(topic='add-to-basket', key=key, value=val)
        producer.flush()
        time.sleep(DELAY_SECONDS)


def generate_add_to_basket_messages():
    """Generate infinite sequence of random Purchase messages."""
    while True:
        message = Basket.Add()
        message.id = "Dylan"
        message.product_id = random.choice(list(PRODUCTS.values()))
        message.quantity = int(random.triangular(1, 5.5, 1))
        yield message


def consume_supply_events():
    table = BeautifulTable()
    table.set_style(BeautifulTable.STYLE_BOX)
    table.columns.header = [
        "   ", "Product  ", "Qty", "Diff"
    ]
    table.columns.alignment["Product  "] = BeautifulTable.ALIGN_LEFT
    table.columns.alignment["Qty"] = BeautifulTable.ALIGN_RIGHT
    table.columns.alignment["Diff"] = BeautifulTable.ALIGN_RIGHT

    consumer = KafkaConsumer(
        'supply-changed',
        bootstrap_servers=[KAFKA_ADDR],
        auto_offset_reset='earliest',
        group_id='event-consumer')

    for row in table.stream(format_supply_events(consumer)):
        print(row)


def format_supply_events(consumer):
    for message in consumer:
        event = Supply.Changed()
        event.ParseFromString(message.value)
        sign = '˖' if event.difference > 0 else ''
        yield [
            icon_of(event.id),
            event.id,
            event.total_quantity,
            f'{sign}{event.difference}'
        ]


def consume_basket_snapshots():
    consumer = KafkaConsumer(
        'basket-snapshots',
        bootstrap_servers=[KAFKA_ADDR],
        auto_offset_reset='earliest',
        group_id='event-consumer')

    for message in consumer:
        snapshot = Basket.Snapshot()
        snapshot.ParseFromString(message.value)
        label = f"{snapshot.id}'s BASKET SNAPSHOT"

        table = BeautifulTable()
        table.set_style(BeautifulTable.STYLE_BOX)
        table.columns.header = [
            "   ", label, "Qty"
        ]
        table.columns.alignment[label] = BeautifulTable.ALIGN_LEFT
        table.columns.alignment["Qty"] = BeautifulTable.ALIGN_RIGHT
        for item in snapshot.items:
            table.rows.append([icon_of(item.id), item.id, item.quantity])
        print(table)


def icon_of(name):
    return next((icon for icon, item in PRODUCTS.items() if name == item), None)


def safe_loop(fn, run_once=False):
    while True:
        try:
            fn()
            if run_once:
                return
        except SystemExit:
            print("Good bye!")
            return
        except NoBrokersAvailable:
            time.sleep(2)
            continue
        except Exception as e:
            print(e)
            return

def term_handler(number, frame):
    sys.exit(0)


def usage(exit_code):
    print("harness.py [restock|purchase|print-supply|print-baskets]")
    sys.exit(exit_code)


def main(arg):
    signal.signal(signal.SIGTERM, term_handler)

    if arg == "restock":
        producer = threading.Thread(target=safe_loop, args=[produce_restock_messages, True])
        producer.start()
        producer.join()
    elif arg == "add-to-basket":
        producer = threading.Thread(target=safe_loop, args=[produce_add_to_basket_messages])
        producer.start()
        producer.join()
    elif arg == "print-supply":
        consumer = threading.Thread(target=safe_loop, args=[consume_supply_events])
        consumer.start()
        consumer.join()
    elif arg == "print-baskets":
        consumer = threading.Thread(target=safe_loop, args=[consume_basket_snapshots])
        consumer.start()
        consumer.join()


if __name__ == "__main__":
    args = sys.argv[1:]
    if len(args) == 0:
        usage(0)
    if args[0] not in ["restock", "add-to-basket", "print-supply", "print-baskets"]:
        usage(1)
    main(args[0])
