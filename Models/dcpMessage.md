# dcpMessage
## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
| **receiveTime** | **Date** | Time the message was received by the system (in UTC). | [optional] [default to null] |
| **dataSource** | [**dataSource**](dataSource.md) |  | [optional] [default to null] |
| **cType** | **String** | Carrier type code (e.g., &#39;g-s-t&#39;). | [optional] [default to null] |
| **arm** | **String** | Antenna receive mode indicator - Type Message - G&#x3D;Good,?&#x3D;Parity Error (Any other is from DAPS) | [optional] [default to null] |
| **eirp** | **String** | Signal strength, in dB (32-57) - Effective Isotropic Radiated Power (EIRP) value. Good 50 to 38dB. | [optional] [default to null] |
| **frequency** | **String** | Frequency offset used by the transmitter. +/-X in 50Hz increments (+/-A indicates 500Hz). | [optional] [default to null] |
| **modulation** | **String** | Modulation type used. High, Normal, Low (H&#x3D;? 70°, N&#x3D;60°+/-5°, L&#x3D;? 50°) | [optional] [default to null] |
| **quality** | **String** | Data signal quality indicator. Normal, Fair, Poor (Error N&lt;10^-6, Fair&gt;10^-6 to 10^-4, Poor&gt;10^-4) | [optional] [default to null] |
| **channel** | **String** | Channel number used for transmission (e.g., &#39;162W&#39;). Goes East or West Satellite. | [optional] [default to null] |
| **downlink** | **String** | Down link status - Hexadecimal coded. | [optional] [default to null] |
| **charlen** | **Integer** | Number of characters in message (includes EOT + FW but not ID). | [optional] [default to null] |
| **data** | **String** | DCP data content that follows the header. | [optional] [default to null] |
| **imei** | **String** | IMEI identifier extracted from the &#x60;ID&#x3D;&#x60; field. | [optional] [default to null] |
| **timestampOffset** | **String** | Encoded timestamp from &#x60;TIME&#x3D;&#x60; field. | [optional] [default to null] |
| **status** | **String** | Status flag from &#x60;STAT&#x3D;&#x60; field. | [optional] [default to null] |
| **mobileOriginated** | **String** | Message count for mobile-originated packets (MO&#x3D;). | [optional] [default to null] |
| **mobileTerminated** | **String** | Message count for mobile-terminated packets (MT&#x3D;). | [optional] [default to null] |
| **cdrReference** | **String** | Reference code from &#x60;CDR&#x3D;&#x60;. | [optional] [default to null] |
| **latitude** | **Float** | Latitude from &#x60;LAT&#x3D;&#x60;. | [optional] [default to null] |
| **longitude** | **Float** | Longitude from &#x60;LON&#x3D;&#x60;. | [optional] [default to null] |
| **radius** | **Integer** | Accuracy or coverage radius from &#x60;RAD&#x3D;&#x60;. | [optional] [default to null] |
| **payloadFormat** | **String** | Raw header line preceding DCP payload (e.g. &#x60;IE:0200D0&#x60;) | [optional] [default to null] |

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)

