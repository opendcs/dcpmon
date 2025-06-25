# Documentation for DDS over HTTP

<a name="documentation-for-api-endpoints"></a>
## Documentation for API Endpoints

All URIs are relative to *https://lrgs.usace.army.mil/api*

| Class | Method | HTTP request | Description |
|------------ | ------------- | ------------- | -------------|
| *DefaultApi* | [**ddsDataNextGet**](Apis/DefaultApi.md#ddsdatanextget) | **GET** /dds/data/next | Retrieve DCS data |
*DefaultApi* | [**ddsDataQueryGet**](Apis/DefaultApi.md#ddsdataqueryget) | **GET** /dds/data/query | Query DCP messages on the fly |
*DefaultApi* | [**ddsDataSearchPost**](Apis/DefaultApi.md#ddsdatasearchpost) | **POST** /dds/data/search | Provide criteria for limiting data returned. |
*DefaultApi* | [**ddsDataSummaryGet**](Apis/DefaultApi.md#ddsdatasummaryget) | **GET** /dds/data/summary | Retrieve status summary for a group |
| *SourcesApi* | [**ddsSourcesGet**](Apis/SourcesApi.md#ddssourcesget) | **GET** /dds/sources | Provide information about data sources available from this instance |


<a name="documentation-for-models"></a>
## Documentation for Models

 - [dataSource](./Models/dataSource.md)
 - [dataSourceType](./Models/dataSourceType.md)
 - [dcpMessage](./Models/dcpMessage.md)
 - [dcpMessages](./Models/dcpMessages.md)
 - [goesMessage](./Models/goesMessage.md)
 - [groupingKey](./Models/groupingKey.md)
 - [iridiumMessage](./Models/iridiumMessage.md)
 - [knownSourceTypes](./Models/knownSourceTypes.md)
 - [searchCriteria](./Models/searchCriteria.md)
 - [statusGroupSummary](./Models/statusGroupSummary.md)
 - [statusGroupSummary_counts](./Models/statusGroupSummary_counts.md)
 - [statusGroupSummary_locations_value](./Models/statusGroupSummary_locations_value.md)


<a name="documentation-for-authorization"></a>
## Documentation for Authorization

All endpoints do not require authorization.
