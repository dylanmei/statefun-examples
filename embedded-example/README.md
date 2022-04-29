# Embedded statefun example

Similar to the classic [shopping-cart](https://github.com/apache/flink-statefun-playground/tree/main/java/shopping-cart) example, this example demonstrates two "statefun" functions:
- `shopping/basket` represent users' shopping baskets
- `shopping/supply` represents a finite supply of products

The state is managed by the Flink cluster, and function invocations occur within the same JVM. These functions gain direct access to messages and state and additional Flink actions, but lost the _physical separation_ of our remote functions example.

## walkthrough

Generate the Protobuf objects for Java: `./gradlew protocols:build`

Run the harness which generates random `Supply.Restock` and `Basket.Add` messages. These messages are delivered to the functions' ingress:

```sh
./gradlew run
```

Generate random `Basket.Add` messages after pre-loading the supply from a savepoint saved by the _bootstrap-example_ project:

```sh
./gradlew run --args="--savepoint.path /tmp/shopping.state"
```

Both `Supply.Changed` events and `Basket.Snapshot` objects are sent to egress, and printed to stdout. Customize this scenario or add additional scenarios by editing `Harness.kt` and `Generators.kt`.

