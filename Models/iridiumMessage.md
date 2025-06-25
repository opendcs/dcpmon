# iridiumMessage
## Properties

| Name | Type | Description | Notes |
|------------ | ------------- | ------------- | -------------|
| **receiveTime** | **Date** | Time the message was received by the system (in UTC). | [optional] [default to null] |
| **dataSource** | [**dataSource**](dataSource.md) |  | [optional] [default to null] |
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
| **data** | **String** | DCP data content that follows the header. | [optional] [default to null] |

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)

