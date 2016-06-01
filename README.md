# Javascript Service for the Monto Broker

## Building
Builds and dependencies are managed with Gradle.

`./gradlew shadowJar` builds an all-dependency-including fat jar (using [Shadow](https://github.com/johnrengelman/shadow)). The jar will be put under `build/libs/services-javascript-all.jar`.

This repository depends on [services-base-java](https://github.com/monto-editor/services-base-java), which is referenced using [jitpack.io](https://jitpack.io/#monto-editor/services-base-java/master-SNAPSHOT).


## Running
Start services with `./start.sh`.

### CLI Options
See [JavaScriptServices.java](src/monto/service/javascript/JavaScriptServices.java).


## Developing
If you intend to not only change this project, but also [services-base-java](https://github.com/monto-editor/services-base-java), it's probably easier to setup our [Gradle multi-project](https://github.com/monto-editor/services-gradle), because if you don't, changes to [services-base-java](https://github.com/monto-editor/services-base-java) are only visible in this project after a public commit.

If not, you can setup Eclipse and IntelliJ like this:

### IntelliJ setup
After cloning, use the `Import Project` or `File -> New -> Project from Existing Sources...` feature and select the `build.gradle` to start the import.

### Eclipse setup
Make sure you have a up-to-date Buildship Gradle Plug-in installed. At the time of writing Eclipse 4.5.2 (Mars 2) is the newest stable Eclipse build. It ships with the Buildship Gradle Plug-in version 1.0.8, but you will need at least 1.0.10, because of [these changes](https://discuss.gradle.org/t/gradle-prefs-contains-absolute-paths/11475/34). To update Buildship, use the Eclipse Marketplace's `Installed` tab.

After cloning, use the `File -> Import -> Existing Projects into Workspace` feature and select the cloned folder.
