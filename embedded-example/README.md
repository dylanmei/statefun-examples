# Embedded statefun example

Similar to the classic [shopping-cart](https://github.com/apache/flink-statefun/tree/master/statefun-examples/statefun-shopping-cart-example) example, this example demonstrates two "statefun" functions:
- `shopping/basket` represent users' shopping baskets
- `shopping/supply` represents a finite supply of products

The state is managed by the Flink cluster, and function invocations occur within the same JVM. These functions gain direct access to messages and state and additional Flink actions, but lost the _physical separation_ of remote functions.

## walkthrough

Generate the Protobuf objects for Java: `./gradlew protocols:build`

Run the harness which generates random `Supply.Restock` and `Basket.Add` messages. These messages are delivered to the functions' ingress: `./gradlew run`. `Supply.Changed` events and `Basket.Snapshot` objects are sent to egress.
