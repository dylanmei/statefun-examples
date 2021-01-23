Bootstrap state example
-----------------------

Uses Flink's [State Processor API](https://ci.apache.org/projects/flink/flink-docs-release-1.11/dev/libs/state_processor_api.html) to generate _supply_ data in order to bootstrap our `remote-example` and `embedded-example` statefun applications.

## walkthrough

Run the bootstrap application, which will record a savepoint file at `/tmp/shopping.state`: `./gradlew run`
