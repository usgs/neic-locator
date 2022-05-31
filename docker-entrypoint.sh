#! /bin/bash

echo "*** Docker Entry Point Is Launching Location Services *** "
exec /usr/bin/java \
    -jar \
    neic-locator-service.jar \
    --mode=service