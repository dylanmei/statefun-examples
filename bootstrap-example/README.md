Bootstrap state example
-----------------------

Uses Flink's [State Processor API](https://ci.apache.org/projects/flink/flink-docs-release-1.14/dev/libs/state_processor_api.html) to generate _supply_ data in order to bootstrap our `remote-example` and `embedded-example` statefun applications.

This savepoint can be used to pre-load the supply data in the _embedded-statefun_ example project.

## walkthrough

Run the bootstrap application, which will record a savepoint file at `/tmp/shopping.state`: `./gradlew run`
