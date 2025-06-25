# DefaultApi

All URIs are relative to *https://lrgs.usace.army.mil/api*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**ddsDataNextGet**](DefaultApi.md#ddsDataNextGet) | **GET** /dds/data/next | Retrieve DCS data |
| [**ddsDataQueryGet**](DefaultApi.md#ddsDataQueryGet) | **GET** /dds/data/query | Query DCP messages on the fly |
| [**ddsDataSearchPost**](DefaultApi.md#ddsDataSearchPost) | **POST** /dds/data/search | Provide criteria for limiting data returned. |
| [**ddsDataSummaryGet**](DefaultApi.md#ddsDataSummaryGet) | **GET** /dds/data/summary | Retrieve status summary for a group |


<a name="ddsDataNextGet"></a>
# **ddsDataNextGet**
> dcpMessages ddsDataNextGet(criteria)

Retrieve DCS data

    Retrieve message for the given, or all if none provided, criteria. This uses HTTP long-polling. After 9 seconds, if no new data is available from the server a 204 Response is sent to the client to indicate the client should connect again to wait for more data.

### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **criteria** | **String**| Name of previously provided criteria to use | [optional] [default to null] |

### Return type

[**dcpMessages**](../Models/dcpMessages.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="ddsDataQueryGet"></a>
# **ddsDataQueryGet**
> dcpMessages ddsDataQueryGet(dcpName, dcpAddress, since, until, ascending, spacecraft, source, single)

Query DCP messages on the fly

    Allows direct querying of DCP messages using search criteria fields passed as query parameters. Equivalent to providing a temporary criteria file. 

### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **dcpName** | [**List**](../Models/String.md)| Specific DCP name(s) to filter | [optional] [default to null] |
| **dcpAddress** | [**List**](../Models/String.md)| Specific DCP address(es) to filter | [optional] [default to null] |
| **since** | **Date**| Lower bound of receive time (UTC) | [optional] [default to null] |
| **until** | **Date**| Upper bound of receive time (UTC) | [optional] [default to null] |
| **ascending** | **Boolean**| Return results in ascending time | [optional] [default to null] |
| **spacecraft** | **String**| Satellite selection (&#39;E&#39;, &#39;W&#39;, or &#39;A&#39;) | [optional] [default to null] [enum: E, W, A] |
| **source** | [**List**](../Models/String.md)| Filter by source types (e.g., GOES, Iridium) | [optional] [default to null] |
| **single** | **Boolean**| Whether to only return a single message | [optional] [default to null] |

### Return type

[**dcpMessages**](../Models/dcpMessages.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

<a name="ddsDataSearchPost"></a>
# **ddsDataSearchPost**
> ddsDataSearchPost(searchCriteria)

Provide criteria for limiting data returned.

    Allows the client to inform the system exactly which data should be returned.

### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **searchCriteria** | [**searchCriteria**](../Models/searchCriteria.md)|  | |

### Return type

null (empty response body)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: application/json
- **Accept**: Not defined

<a name="ddsDataSummaryGet"></a>
# **ddsDataSummaryGet**
> statusGroupSummary ddsDataSummaryGet(group)

Retrieve status summary for a group

    Returns a summarized status report for a group of DCP locations, including message counts, parity errors, and battery health. 

### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **group** | **String**| The name of the group to summarize (e.g., \&quot;SWT\&quot;) | [default to null] |

### Return type

[**statusGroupSummary**](../Models/statusGroupSummary.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: application/json

