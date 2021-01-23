# Embedded statefun example

Similar to the classic [shopping-cart](https://github.com/apache/flink-statefun/tree/master/statefun-examples/statefun-shopping-cart-example) example, this function demonstrates two "statefun" functions: one to represent users' shopping baskets -- into which `products` can be added -- and another to represent an `supply` of products.

## walkthrough

Generate the Protobuf objects for Java: `./gradlew protocols:build`

Run the harness which generates random `Supply.Restock` and `Basket.Add` messages. These messages are delivered to the functions' ingress: `./gradlew run`
