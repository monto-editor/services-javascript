java -jar dist/services-javascript.jar \
    -t -p -o -c -f -s \
    -address tcp://* \
    -registration tcp://*:5004 \
    -configuration tcp://*:5007 \
    -resources 5051 \
    -dyndeps tcp://*:5009 \
    -flowlocation dist/
