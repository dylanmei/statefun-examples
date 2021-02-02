# Remote statefun example

Similar to the classic [shopping-cart](https://github.com/apache/flink-statefun/tree/master/statefun-examples/statefun-shopping-cart-example) example, this example demonstrates two "statefun" functions:
- `shopping/basket` represent users' shopping baskets
- `shopping/supply` represents a finite supply of products

While the state is managed by the Flink cluster, the function invocations occur remotely, demonstrating both _Logical Compute/State Co-location_ and _Physical Compute/State Separation_.

## walkthrough

First, bring the `apache_flink_statefun` _WHEEL_ into the project. I did this with these steps:
- Clone the [apache/flink-statefun](https://github.com/apache/flink-statefun/tree/master/statefun-examples) project
- Check-out the `release-2.2` branch
- Navigate to the `statefun-python-sdk` directory
- Run `./build-distribution.sh`
- Copy `./dist/apache_flink_statefun-2.2_SNAPSHOT-py3-none-any.whl` to this example directory

Generate the Protobuf objects for Python: `protoc *.proto --python_out=.`

Build the Flink images defined in the `docker-compose.yaml` file: `docker-compose build`. You'll do this again if you make changes to `main.py`, `module.yaml`, or re-generate the Protobuf objects.

Run the function and the accompanying Flink cluster: `docker-compose up`. _If the Kafka broker appears to crash, run `docker-compose restart kafka-broker` or restart this step._

Now we have our Flink Stateful function running, but we don't have any state. We'll use `harness.py` to create and interact with our `supply` data.
- Install a few requirements: `pip3 install beautifultable pluralizer`
- In a new shell, run `python3 harness.py restock` to add to our supply of products.
- In a new shell, run `python3 harness.py add-to-basket` to take from our supply of products.
- In a new shell, run `python3 harness.py print-supply` to show a live-feed of changes to our supply of products.
- In a new shell, run `python3 harness.py print-baskets` to show a live-feed of the current state of shopping baskets.
