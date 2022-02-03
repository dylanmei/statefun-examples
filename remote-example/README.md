# Remote statefun example

Similar to the classic [shopping-cart](https://github.com/apache/flink-statefun-playground/tree/release-3.2) example, this example demonstrates two "statefun" functions:
- `shopping/basket` represent users' shopping baskets
- `shopping/supply` represents a finite supply of products

While the state is managed by the Flink cluster, the function invocations occur remotely, demonstrating both _Logical Compute/State Co-location_ and _Physical Compute/State Separation_.

## walkthrough

First, generate the Protobuf objects for Python: `protoc *.proto --python_out=.`

Build the Flink images defined in the `docker-compose.yaml` file: `docker-compose build`. You'll do this again if you make changes to `main.py`, `module.yaml`, or re-generate the Protobuf objects.

Run the function and the accompanying Flink cluster: `docker-compose up`. _If the Kafka broker appears to crash, run `docker-compose restart kafka-broker` or restart this step._

Now we have our Flink Stateful function running, but we don't have any state. We'll use `harness.py` to create and interact with our `supply` data.
- Install a few requirements: `pip3 install beautifultable pluralizer`
- In a new shell, run `python3 harness.py restock` to add to our supply of products.
- In a new shell, run `python3 harness.py add-to-basket` to take from our supply of products.
- In a new shell, run `python3 harness.py print-supply` to show a live-feed of changes to our supply of products.
- In a new shell, run `python3 harness.py print-baskets` to show a live-feed of the current state of shopping baskets.
