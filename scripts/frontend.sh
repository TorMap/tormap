cd ../frontend || exit

#Check for command node and export path
if [ -z "$(command -v node)" ]; then
  export PATH=/usr/local/lib/nodejs/node-v12.14.1-linux-x64/bin:$PATH
  . ~/.profile
fi

if [ "$(command -v yarn)" ]; then
  yarn > /dev/null 2>&1
  yarn start
else
  echo 'ERROR: Yarn programm not found. Please make sure yarn is installed.'
fi
