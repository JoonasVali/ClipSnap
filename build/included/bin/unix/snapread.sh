#!/bin/bash

# Get the directory of the script (ensures correct relative paths)
APP_DIR="$(cd "$(dirname "$0")" && pwd)"

# Run Java with Logback configuration and proper memory settings
"$APP_DIR/jre/bin/java" -Dlogback.configurationFile="$APP_DIR/logback.xml" -Xmx2048M -jar "$APP_DIR/lib/bookreader-core.jar"
