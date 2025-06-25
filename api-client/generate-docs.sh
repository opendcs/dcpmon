# Clear previous docs
rm -rf ./docs/*

# Create github pages docs
docker run --rm \
  -u $(id -u):$(id -g) \
  -v "${PWD}:/local" \
  openapitools/openapi-generator-cli generate \
  -g html2 \
  -i /local/api-client/dds-http.yaml \
  -o /local/docs \
  --additional-properties=title="DDS HTTP API",disableHtmlEscaping=true,footer="OpenDCS Contributors",


# Create README files
docker run --rm \
  -u $(id -u):$(id -g) \
  -v "${PWD}:/local" \
  openapitools/openapi-generator-cli generate \
  -g markdown \
  -i /local/api-client/dds-http.yaml \
  -o /local