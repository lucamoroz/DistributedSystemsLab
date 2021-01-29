Distributed Systems Lab - TUWien WS2020
=======================

Using gradle
------------

### Compile & Test

Gradle is the build tool we are using. Here are some instructions:

Compile the project using the gradle wrapper:

    ./gradlew assemble

Compile and run the tests:

    ./gradlew build

### Run the applications

The gradle config config contains several tasks that start application components for you.
You can list them with

    ./gradlew tasks --all

And search for 'Other tasks' starting with `run-`. For example, to run the monitoring server, execute:
(the `--console=plain` flag disables CLI features, like color output, that may break the console output when running a interactive application)

    ./gradlew --console=plain run-monitoring
    
#### Example usage
Run nameservers:
```
./gradlew --console=plain run-ns-root
./gradlew --console=plain run-ns-planet
./gradlew --console=plain run-ns-ze
./gradlew --console=plain run-ns-univer
```

Run mailbox servers:
```
./gradlew --console=plain run-mailbox-earth-planet
./gradlew --console=plain run-mailbox-univer-ze
```

Run transfer servers:
```
./gradlew --console=plain run-transfer-1
./gradlew --console=plain run-transfer-2
```

Finally, run the client:
`./gradlew --console=plain run-client-trillian`

On the client task, send messages with `msg <to> "<subject>" "<data>"`
Type $help to see all available commands.
