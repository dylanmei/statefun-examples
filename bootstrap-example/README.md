Bootstrap state example
-----------------------

Uses Flink's [State Processor API](https://ci.apache.org/projects/flink/flink-docs-release-1.11/dev/libs/state_processor_api.html) to generate _supply_ data in order to bootstrap our `remote-example` and `embedded-example` statefun applications.

_TODO: I'm currently struggling to get either application to accept the state._

## walkthrough

Run the bootstrap application, which will record a savepoint file at `/tmp/shopping.state`: `./gradlew run`
