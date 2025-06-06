#!/bin/bash
set -e

echo "Generating OpenAPI client..."
docker run --rm \
  -v ${PWD}:/local \
  openapitools/openapi-generator-cli generate \
  -g typescript-axios \
  -i /local/api-client/dds-http.yaml \
  -o /local/api-client/generated \
  --additional-properties=npmName=dds-api,npmVersion=1.0.0

echo "Fixing permissions..."
sudo chown -R $USER:$USER ./api-client/generated

echo "Installing dependencies..."
cd ./api-client/generated
npm install axios
npx tsc

echo "Client ready at api-client/generated/dist/"
