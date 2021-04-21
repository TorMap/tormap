#!/bin/sh
pkill -f 'gradle'

cd ../backend || exit
if [ -f './gradlew' ]; then
  ./gradlew > /dev/null 2>&1
  ./gradlew bootRun > /dev/null 2>&1
else
  echo 'ERROR: No Gradle wrapper found. Please make sure /backend/gradlew is available.'
fi
