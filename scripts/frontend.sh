#!/bin/bash
cd ../frontend || exit

if [ "$(command -v yarn)" ]; then
  yarn
  yarn start
else
  echo 'ERROR: Yarn program not found. Please make sure yarn is installed.'
fi
