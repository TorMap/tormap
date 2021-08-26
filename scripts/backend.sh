#!/bin/bash
pkill -f 'gradle'

cd ../backend || exit
if [ -f './gradlew' ]; then
  ./gradlew
  ./gradlew bootRun
else
  echo 'ERROR: No Gradle wrapper found. Please make sure /backend/gradlew is available.'
fi
