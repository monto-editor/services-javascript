#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

eclipse_jar="$DIR/dist/services-javascript.jar"
gradle_jar="$DIR/build/libs/services-javascript.jar"

if [ -f "$eclipse_jar" ]; then
    jar="$eclipse_jar"
elif [ -f "$gradle_jar" ]; then
    jar="$gradle_jar"
else
    printf "No jar found. Please build the project first.\n" >&2
    exit 99
fi

java -jar "$jar" \
     -t -p -o -c -f -s \
     -address tcp://* \
     -registration tcp://*:5004 \
     -configuration tcp://*:5007 \
     -resources 5051 \
     -dyndeps tcp://*:5009 \
     -flowlocation dist/
