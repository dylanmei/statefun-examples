import signal
import sys
import time
import threading
import random

from kafka.errors import NoBrokersAvailable
from messages_pb2 import Restock, Purchase, SupplyChanged
from beautifultable import BeautifulTable

from kafka import KafkaProducer
from kafka import KafkaConsumer

KAFKA_ADDR = "localhost:9092"
PRODUCT_LIST = ["👞️Shoes", "🧥️Coat", "🎒️Backpack", "👛️Purse", "🎩️Hat", "⌚️Watch", "👖️Pants", "🌂️Umbrella", "🍫️Chocolate"]
DELAY_SECONDS = 5

def produce_restock_messages():
    producer = KafkaProducer(bootstrap_servers=[KAFKA_ADDR])

    for message in generate_restock_messages():
        print(f"{message.id[0]} Restocking {message.id[1:]} ({message.quantity})")
        key = message.id.encode('utf-8')
        val = message.SerializeToString()
        producer.send(topic='restock', key=key, value=val)
        producer.flush()
        time.sleep(DELAY_SECONDS)

def generate_restock_messages():
    """Generate infinite sequence of random Restock messages."""
    while True:
        message = Restock()
        message.id = random.choice(PRODUCT_LIST)
        message.quantity = int(random.triangular(1, 5.5, 5))
        yield message

def produce_purchase_messages():
    producer = KafkaProducer(bootstrap_servers=[KAFKA_ADDR])

    for message in generate_purchase_messages():
        print(f"{message.id[0]} Purchasing {message.id[1:]} ({message.quantity})")
        key = message.id.encode('utf-8')
        val = message.SerializeToString()
        producer.send(topic='purchase', key=key, value=val)
        producer.flush()
        time.sleep(DELAY_SECONDS)

def generate_purchase_messages():
    """Generate infinite sequence of random Purchase messages."""
    while True:
        message = Purchase()
        message.id = random.choice(PRODUCT_LIST)
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
        event = SupplyChanged()
        event.ParseFromString(message.value)
        yield [
            event.id[:1],
            event.id[1:],
            event.total_quantity,
            event.difference
        ]


def term_handler(number, frame):
    sys.exit(0)


def safe_loop(fn):
    while True:
        try:
            fn()
        except SystemExit:
            print("Good bye!")
            return
        except NoBrokersAvailable:
            time.sleep(2)
            continue
        except Exception as e:
            print(e)
            return

def usage(exit_code):
    print("harness.py [restock|purchase|print-supply]")
    sys.exit(exit_code)

def main(arg):
    signal.signal(signal.SIGTERM, term_handler)

    if arg == "restock":
        producer = threading.Thread(target=safe_loop, args=[produce_restock_messages])
        producer.start()
        producer.join()
    elif arg == "purchase":
        producer = threading.Thread(target=safe_loop, args=[produce_purchase_messages])
        producer.start()
        producer.join()
    elif arg == "both":
        restock_producer = threading.Thread(target=safe_loop, args=[produce_restock_messages])
        restock_producer.start()

        purchase_producer = threading.Thread(target=safe_loop, args=[produce_purchase_messages])
        purchase_producer.start()

        restock_producer.join()
        purchase_producer.join()

    elif arg == "print-supply":
        consumer = threading.Thread(target=safe_loop, args=[consume_supply_events])
        consumer.start()
        consumer.join()

if __name__ == "__main__":
    args = sys.argv[1:]
    if len(args) == 0:
        usage(0)
    if args[0] not in ["restock", "purchase", "both", "print-supply"]:
        usage(1)
    main(args[0])
