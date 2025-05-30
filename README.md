# DCP Monitor

Join the scope/initial discussion here : https://github.com/opendcs/dcpmon/discussions/1

## License
Apache 2.0


## Other Links
- OpenDCS Project: https://github.com/opendcs/opendcs
- OpenDCS Web App: https://github.com/opendcs/rest_api/tree/main/opendcs-web-client

## TODO:
- Using existing code after mocking to talk to actual LRGS instance

## Design Convos

- Design point, if we make "GOES Channel" it's own endpoint and it's expected, then we're commiting again to GOES first instead of "data first". The channel knowledge seems like a detail that should've be provided by the LRGS itself, or if so a more generic /datasource/metadata type end point. 