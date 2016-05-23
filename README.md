Javascript Service for the Monto broker
=======================================

Building
--------
This repository depends on [services-base-java](https://github.com/monto-editor/services-base-java). Dependency are managed with Gradle. You will need our [Gradle multi-project](https://github.com/monto-editor/services-gradle) to build it successfully. Follow the instruction there.

After setting it up, you can build a jar with `./gradlew :services-javascript:jar`. The jar will be put under `services-javascript/build/libs/services-javascirpt.jar`.

Running
-------
1. Install aspell and the dictionaries of your choice.
2. Start services with `./start.sh`.

CLI Options
-----------
See [JavaScriptServices.java](src/monto/service/javascript/JavaScriptServices.java).

Developing
----------
Out of the box supported IDEs are Eclipse (with the Gradle Buildship Plug-in) and IntelliJ. Follow the instruction at [services-gradle](https://github.com/monto-editor/services-gradle).
