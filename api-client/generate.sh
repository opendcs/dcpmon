#!/bin/bash
set -e

echo "Generating OpenAPI client..."
docker run --rm \
  -v "${PWD}:/local" \
  openapitools/openapi-generator-cli generate \
  -c /local/api-client/config.yaml

echo "Fixing permissions..."
sudo chown -R $USER:$USER ./api-client/generated


echo "Clearing previous installations..."
rm -rf ./api-client/generated/node_modules

cd ./api-client/generated
echo "Installing dependencies..."
npm install axios
npx tsc

echo "Installing generated client to local webapp..."
cd ../../webapp
npm install ../api-client/generated

echo "Client ready at api-client/generated/dist/"
