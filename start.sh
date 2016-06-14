#!/usr/bin/env bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

gradle_shadow_jar="$DIR/build/libs/services-javascript-all.jar"

if [ ! -f "$gradle_shadow_jar" ]; then
    printf "No jar found. Please build the project first.\n" >&2
    exit 99
fi

java -jar "$gradle_shadow_jar" \
     -t -p -o -c -f -s \
     -address tcp://* \
     -registration tcp://*:5002 \
     -resources 5051 \
     -flowlocation dist/
