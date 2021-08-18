package com.sirionlabs.utils.commonUtils;

import com.sirionlabs.api.commonAPI.Options;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.test.reportRenderer.FilterData;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author manoj.upreti
 */
public class FilterUtils {
	private final static Logger logger = LoggerFactory.getLogger(FilterUtils.class);


	//This method is to provide the filters Map from the defaultMetadata Responce
	public Map<Integer, Map<String, String>> getFilters(String defaultMetadataResponceJson) {
		logger.debug("Started creating filters using the response of defaultMetadata Responce");
		Map<Integer, Map<String, String>> filtersMap = new HashMap<>();
		try {

			JSONObject jsonObj = new JSONObject(defaultMetadataResponceJson);
			JSONUtility jsonUtilObj = new JSONUtility(jsonObj);
			JSONArray filtersArrayInResponce = jsonUtilObj.getArrayJsonValue("filterMetadatas");
			if (filtersArrayInResponce.length() > 0) {
				for (int count = 0; count < filtersArrayInResponce.length(); count++) {
					try {
						JSONObject filter = filtersArrayInResponce.getJSONObject(count);
						logger.debug("Creating the filterData Map for the filter [ {} ]", filter);
						JSONUtility filterObj = new JSONUtility(filter);

						int filterId = filterObj.getIntegerJsonValue("id");
						logger.debug("The Filter ID for the filter [ {} ] is [ {} ]", filter, filterId);
						Map<String, String> filterPropertiesMap = new HashMap<>();
						filterPropertiesMap.put("defaultName", filterObj.getStringJsonValue("defaultName"));
						filterPropertiesMap.put("name", filterObj.getStringJsonValue("name"));
						filterPropertiesMap.put("type", filterObj.getStringJsonValue("type"));
						filterPropertiesMap.put("uiType", filterObj.getStringJsonValue("uiType"));
						filterPropertiesMap.put("queryName", filterObj.getStringJsonValue("queryName"));

						filtersMap.put(filterId, filterPropertiesMap);
					} catch (Exception e) {
						logger.error("Got the Exception while filling the Metadata into Map , please check", e.getStackTrace());
					}

				}
			} else {
				logger.error("The filterMetadatas Array size is 0 please check , the returned value will be empty Map");
			}
		} catch (Exception e) {
			logger.error("Got Exception , Cause : [ {} ] , StackTrace : [ {} ] while fetching metadata , please check", e.getMessage(), e.getStackTrace());
			e.printStackTrace();
		}
		return filtersMap;
	}

	//Below method is for getting specific filters from metaData response. Possible values can be filterCategory = primary or advanced
	public Map<Integer, Map<String, String>> getSpecificFilters(String defaultMetadataResponseJson, String filterCategory) {
		logger.debug("Started creating filters using the response of defaultMetadata Responce");
		Map<Integer, Map<String, String>> filtersMap = new HashMap<>();
		try {

			JSONObject jsonObj = new JSONObject(defaultMetadataResponseJson);
			JSONUtility jsonUtilObj = new JSONUtility(jsonObj);
			JSONArray filtersArrayInResponce = jsonUtilObj.getArrayJsonValue("filterMetadatas");
			if (filtersArrayInResponce.length() > 0) {
				for (int count = 0; count < filtersArrayInResponce.length(); count++) {
					try {
						JSONObject filter = filtersArrayInResponce.getJSONObject(count);
						logger.debug("Creating the filterData Map for the filter [ {} ]", filter);
						JSONUtility filterObj = new JSONUtility(filter);
						String displayFormat = filterObj.getStringJsonValue("displayFormat").toLowerCase();
						if (filterCategory.equalsIgnoreCase("primary")) {
							if (!displayFormat.contains("primary"))
								continue;
						} else if (filterCategory.equalsIgnoreCase("advanced")) {
							if (displayFormat.contains("primary"))
								continue;
						}
						int filterId = filterObj.getIntegerJsonValue("id");
						logger.debug("The Filter ID for the filter [ {} ] is [ {} ]", filter, filterId);
						Map<String, String> filterPropertiesMap = new HashMap<>();
						filterPropertiesMap.put("defaultName", filterObj.getStringJsonValue("defaultName"));
						filterPropertiesMap.put("name", filterObj.getStringJsonValue("name"));
						filterPropertiesMap.put("type", filterObj.getStringJsonValue("type"));
						filterPropertiesMap.put("uiType", filterObj.getStringJsonValue("uiType"));
						filterPropertiesMap.put("queryName", filterObj.getStringJsonValue("queryName"));

						filtersMap.put(filterId, filterPropertiesMap);
					} catch (Exception e) {
						logger.error("Got the Exception while filling the Metadata into Map , please check", e.getStackTrace());
					}

				}
			} else {
				logger.error("The filterMetadatas Array size is 0 please check , the returned value will be empty Map");
			}
		} catch (Exception e) {
			logger.error("Got Exception , Cause : [ {} ] , StackTrace : [ {} ] while fetching metadata , please check", e.getMessage(), e.getStackTrace());
			e.printStackTrace();
		}
		return filtersMap;
	}

	//This method is to provide the filters Data List from the filterData Responce
	public List<FilterData> getFiltersData(String filterDataResponceJson, String reportId, String entityTypeId) {
		logger.debug("Started the getFiltersData method to create the filter Data from the response of filterData Responce Json");
		List<FilterData> filterDataList = new ArrayList<>();
		//try {
		JSONObject jsonObject = new JSONObject(filterDataResponceJson);
		JSONArray filtersArrayInResponce = JSONUtility.convertJsonOnjectToJsonArray(jsonObject);
		if (filtersArrayInResponce.length() > 0) {
			for (int count = 0; count < filtersArrayInResponce.length(); count++) {
				JSONObject filter = filtersArrayInResponce.getJSONObject(count);
				logger.debug("Getting the filterData Map for the filter [ {} ]", filter);
				JSONUtility filterObj = new JSONUtility(filter);
				String filterName = filterObj.getStringJsonValue("filterName").toLowerCase();
				String filterUitype = filterObj.getStringJsonValue("uitype").toLowerCase();
				int filterId = filterObj.getIntegerJsonValue("filterId");
				logger.info("The Filter ID for the filter [ {} ] is [ {} ]", filterName, filterId);
				logger.info("---------------------------------------------------------------------");

				try {
					// this below check is only for slider type of filters since in that there will no json object for multiselectValues key
					if (!filterObj.getStringJsonValue("multiselectValues").contentEquals("null")) {

						JSONObject jsonObjTillOptions = filterObj.getJsonObject("multiselectValues").getJSONObject("OPTIONS");
						JSONUtility jsonUtilObjTillOptions = new JSONUtility(jsonObjTillOptions);
						boolean autoComplete = false;
						if(jsonObjTillOptions.has("autoComplete"))
						autoComplete = jsonObjTillOptions.getBoolean("autoComplete");

						if (!autoComplete || filterName.equalsIgnoreCase("stakeholder") || filterId == 53) {
							List<FilterData.DataClass> dataClassList = getFilterDataList(jsonUtilObjTillOptions, filterId);
							FilterData filterDataClassObj = new FilterData(filterName,filterUitype, filterId, autoComplete, dataClassList);
							filterDataList.add(filterDataClassObj);
						} else {
							logger.debug("This filter is autocomplete enabled ,getting the Values From Config File");
							Map<String, String> mapOfNameIDForAutoComplete = getMapsOfNameIDForAutoComplete(filterName, reportId, entityTypeId, "listdata");
							List<FilterData.DataClass> dataClassList = new ArrayList<>();
							if (mapOfNameIDForAutoComplete.size() != 0) {
								for (Map.Entry<String, String> entry : mapOfNameIDForAutoComplete.entrySet()) {
									FilterData.DataClass dataClassObj = new FilterData.DataClass();
									dataClassObj.setDataName(entry.getKey());
									dataClassObj.setDataValue(entry.getValue());
									dataClassList.add(dataClassObj);
								}
							} else {
								logger.warn("The data response of autocomplete filter is null for entity : [ {} ] , Report : [ {} ], Filter : [ {} ]", entityTypeId, reportId, filterName);
							}
							FilterData filterDataClassObj = new FilterData(filterName,filterUitype, filterId, autoComplete, dataClassList);
							filterDataList.add(filterDataClassObj);
						}
					} else {
						FilterData filterDataClassObj = new FilterData(filterName,filterUitype, filterId, false, null);
						filterDataList.add(filterDataClassObj);
					}


					logger.info("*********************************************************************");


				} catch (Exception e) {
					logger.error("Got Exception while getting Auto Complete option  for Entity : [ {} ] , Report : [ {} ], Filter : [ {} ] and the Cause : [ {} ], Exception : [ {} ] , making the autoComplete value to false ", entityTypeId, reportId, filterName, e.getMessage(), e.getStackTrace());
					e.printStackTrace();
				}


			}
		} else {
			logger.error("The filters Array size is 0  In Filter Data API Responce for Entity : [ {} ] , Report : [ {} ], the returned value will be empty List", entityTypeId, reportId);
		}
		/*} catch (Exception e) {
			logger.error("Got Exception while getting filter Data for Entity : [ {} ] , Report : [ {} ], and the Cause : [ {} ],  Exception : [ {} ]", entityTypeId, reportId, e.getMessage(), e.getStackTrace());
			e.printStackTrace();
		}*/
		return filterDataList;
	}

	public String getPayloadForFilter(String entityTypeId, String uiType, String filterId, String filterName, FilterData.DataClass dataClassObj, int offset, int size) {

		String orderByColumnName = ConfigureConstantFields.getConstantFieldsProperty("orderby");
		String orderDirection = ConfigureConstantFields.getConstantFieldsProperty("orderdirection");

		if (uiType.equalsIgnoreCase("MULTISELECT")) {
			JSONObject payload = new JSONObject();

			JSONObject filterMap = new JSONObject();
			filterMap.put("entityTypeId", Integer.parseInt(entityTypeId));
			filterMap.put("offset", offset);
			filterMap.put("size", size);
			filterMap.put("orderByColumnName", orderByColumnName);
			filterMap.put("orderDirection", orderDirection);

			JSONObject filterJson = new JSONObject();
			JSONObject filterIdJson = new JSONObject();
			filterIdJson.put("filterId", filterId);
			filterIdJson.put("filterName", filterName);
			filterIdJson.put("entityFieldId", JSONObject.NULL);
			filterIdJson.put("entityFieldHtmlType", JSONObject.NULL);
			logger.debug("filterIdJson [ {} ]", filterIdJson);

			JSONObject multiselectValues = new JSONObject();
			JSONArray SELECTEDDATA = new JSONArray();

			JSONObject SELECTEDDATAInternal = new JSONObject();
			SELECTEDDATAInternal.put("id", dataClassObj.getDataName());
			SELECTEDDATAInternal.put("name", dataClassObj.getDataValue());
			SELECTEDDATA.put(SELECTEDDATAInternal);
			multiselectValues.put("SELECTEDDATA", SELECTEDDATA);
			filterIdJson.put("multiselectValues", multiselectValues);
			filterJson.put(filterId, filterIdJson);
			filterMap.put("filterJson", filterJson);
			payload.put("filterMap", filterMap);
			logger.debug("Payload for MULTISELECT Filter is : {}", payload);
			return payload.toString();

		} else if (uiType.equalsIgnoreCase("DATE")) {
			JSONObject payload = new JSONObject();
			JSONObject filterMap = new JSONObject();

			filterMap.put("entityTypeId", Integer.parseInt(entityTypeId));
			filterMap.put("offset", offset);
			filterMap.put("size", size);
			filterMap.put("orderByColumnName", orderByColumnName);
			filterMap.put("orderDirection", orderDirection);

			JSONObject filterJson = new JSONObject();
			JSONObject filterIdJson = new JSONObject();
			filterIdJson.put("filterId", filterId);
			filterIdJson.put("filterName", filterName);
			filterIdJson.put("entityFieldId", JSONObject.NULL);
			filterIdJson.put("entityFieldHtmlType", JSONObject.NULL);
			filterIdJson.put("start", dataClassObj.getDataName());
			filterIdJson.put("end", dataClassObj.getDataValue());
			logger.debug("filterIdJson [ {} ]", filterIdJson);
			filterJson.put(filterId, filterIdJson);
			filterMap.put("filterJson", filterJson);
			payload.put("filterMap", filterMap);
			logger.debug("Payload for DATE filter is : {}", payload);
			return payload.toString();
		} else if (uiType.equalsIgnoreCase("STATUS")) {

			JSONObject payload = new JSONObject();
			JSONObject filterMap = new JSONObject();
			filterMap.put("entityTypeId", Integer.parseInt(entityTypeId));
			filterMap.put("offset", offset);
			filterMap.put("size", size);
			filterMap.put("orderByColumnName", orderByColumnName);
			filterMap.put("orderDirection", orderDirection);

			JSONObject filterJson = new JSONObject();
			JSONObject filterIdJson = new JSONObject();
			filterIdJson.put("filterId", filterId);
			filterIdJson.put("filterName", filterName);
			filterIdJson.put("entityFieldId", JSONObject.NULL);
			filterIdJson.put("entityFieldHtmlType", JSONObject.NULL);
			logger.debug("filterIdJson [ {} ]", filterIdJson);

			JSONObject multiselectValues = new JSONObject();
			JSONArray SELECTEDDATA = new JSONArray();

			JSONObject SELECTEDDATAInternal = new JSONObject();
			SELECTEDDATAInternal.put("id", dataClassObj.getDataName());
			SELECTEDDATAInternal.put("name", dataClassObj.getDataValue());

			SELECTEDDATA.put(SELECTEDDATAInternal);
			multiselectValues.put("SELECTEDDATA", SELECTEDDATA);
			filterIdJson.put("multiselectValues", multiselectValues);
			filterJson.put(filterId, filterIdJson);
			filterMap.put("filterJson", filterJson);
			payload.put("filterMap", filterMap);

			logger.debug("Payload for STATUS Filter is : {}", payload);
			return payload.toString();

		} else if (uiType.equalsIgnoreCase("SLIDER")) {
			JSONObject finalPayload = new JSONObject();
			JSONObject filterMap = new JSONObject();
			filterMap.put("entityTypeId", Integer.parseInt(entityTypeId));
			filterMap.put("offset", offset);
			filterMap.put("size", size);
			filterMap.put("orderByColumnName", orderByColumnName);
			filterMap.put("orderDirection", orderDirection);

			JSONObject filterJson = new JSONObject();
			JSONObject filterIdJson = new JSONObject();
			filterIdJson.put("filterId", filterId);
			filterIdJson.put("filterName", filterName);
			filterIdJson.put("entityFieldId", JSONObject.NULL);
			filterIdJson.put("entityFieldHtmlType", JSONObject.NULL);
			filterIdJson.put("min", dataClassObj.getDataName());
			filterIdJson.put("max", dataClassObj.getDataValue());
			filterIdJson.put("suffix", JSONObject.NULL);

			logger.debug("filterIdJson [ {} ]", filterIdJson);

			filterJson.put(filterId, filterIdJson);

			filterMap.put("filterJson", filterJson);

			finalPayload.put("filterMap", filterMap);

			logger.debug("Payload for SLIDER Filter is : {}", finalPayload);
			return finalPayload.toString();
		} else {
			logger.error("The Filter UI Type is not matching , for entityTypeId [ {} ], uiType [ {} ], filterId [ {} ] , filterName [ {} ]", entityTypeId, uiType, filterId, filterName);
			return null;
		}
	}

	public String getPayloadForFilter(String entityTypeId, String uiType, String filterId, String filterName, FilterData.DataClass dataClassObj) {

		int offset = Integer.parseInt(ConfigureConstantFields.getConstantFieldsProperty("offset"));
		int size = Integer.parseInt(ConfigureConstantFields.getConstantFieldsProperty("pagesize"));
		return getPayloadForFilter(entityTypeId, uiType, filterId, filterName, dataClassObj, offset, size);


	}



	public String getPayloadForSTAKEHOLDERFilter(String entityTypeId, String uiType, String filterId, String filterName, FilterData.DataClass dataClassObj, String stakeHolderKey) {
		int offset = Integer.parseInt(ConfigureConstantFields.getConstantFieldsProperty("offset"));
		int size = Integer.parseInt(ConfigureConstantFields.getConstantFieldsProperty("pagesize"));
		String orderByColumnName = ConfigureConstantFields.getConstantFieldsProperty("orderby");
		String orderDirection = ConfigureConstantFields.getConstantFieldsProperty("orderdirection");

		if (uiType.equalsIgnoreCase("STAKEHOLDER")) {
			JSONArray SELECTEDDATA = new JSONArray();
			JSONObject SELECTEDDATAJson = new JSONObject();
			SELECTEDDATAJson.put("id", dataClassObj.getDataName());
			SELECTEDDATAJson.put("name", dataClassObj.getDataValue());
			SELECTEDDATAJson.put("group", generateStakeHolderGroup(dataClassObj.getMapOfData(), stakeHolderKey));
			logger.debug("SELECTEDDATAJson [ {} ]", SELECTEDDATAJson);

			SELECTEDDATA.put(SELECTEDDATAJson);
			logger.debug("SELECTEDDATA [ {} ]", SELECTEDDATA);

			JSONObject multiselectValues = new JSONObject();
			multiselectValues.put("SELECTEDDATA", SELECTEDDATA);
			logger.debug("multiselectValues [ {} ]", multiselectValues);

			JSONObject filterIdJson = new JSONObject();
			filterIdJson.put("filterId", Integer.parseInt(filterId));
			filterIdJson.put("filterName", filterName);
			filterIdJson.put("multiselectValues", multiselectValues);
			filterIdJson.put("primary", "false");
			filterIdJson.put("uitype", "STAKEHOLDER");
			logger.debug("filterIdJson [ {} ]", filterIdJson);

			JSONObject filterTypeSpecificPayloadJson = new JSONObject();
			filterTypeSpecificPayloadJson.put(filterId, filterIdJson);
			logger.debug("filterTypeSpecificPayloadJson [ {} ]", filterTypeSpecificPayloadJson);


			JSONObject filterMapJson = new JSONObject();
			filterMapJson.put("entityTypeId", Integer.parseInt(entityTypeId));
			filterMapJson.put("offset", offset);
			filterMapJson.put("size", size);
			filterMapJson.put("orderByColumnName", orderByColumnName);
			filterMapJson.put("orderDirection", orderDirection);
			filterMapJson.put("filterJson", filterTypeSpecificPayloadJson);
			logger.debug("filterMapJson [ {} ]", filterMapJson);

			JSONObject finalPayload = new JSONObject();
			finalPayload.put("filterMap", filterMapJson);
			logger.debug("Payload for STAKEHOLDER Filter is : {}", finalPayload);
			return finalPayload.toString();
		} else {
			logger.error("Please Call the method in STAKEHOLDER Filter , returning empty String");
			return "";
		}
	}


	public Map<String, String> getStakeHolderGroup(JSONArray stakeHolderGroupArray) {
		logger.debug("Starting the method Name getStakeHolderGroup , to get the Map of STAKEHOLDERS Data");
		Map<String, String> stakeHolderGroupMap = new HashMap<>();
		if (stakeHolderGroupArray.length() > 0) {
			for (int groupCount = 0; groupCount < stakeHolderGroupArray.length(); groupCount++) {
				JSONObject dataOb = stakeHolderGroupArray.getJSONObject(groupCount);
				JSONUtility dataJsonUtilObj = new JSONUtility(dataOb);
				stakeHolderGroupMap.put(dataJsonUtilObj.getStringJsonValue("id"), dataJsonUtilObj.getStringJsonValue("name"));
			}
		} else {
			logger.error("The stakeHolderGroupArray size is 0 please check, Empty Map will be returned");
		}
		return stakeHolderGroupMap;
	}

	public List<FilterData.DataClass> getFilterDataList(JSONUtility jsonUtilObjTillOptions, int filterId) {
		logger.debug("Started Method Execution for getFilterDataList ");
		List<FilterData.DataClass> dataClassList = new ArrayList<>();
		try {
			JSONArray jsonDataArray = jsonUtilObjTillOptions.getArrayJsonValue("data");
			if (jsonDataArray != null) {
				if (jsonDataArray.length() > 0) {
					for (int countData = 0; countData < jsonDataArray.length(); countData++) {

						JSONObject dataOb = jsonDataArray.getJSONObject(countData);
						JSONUtility dataJsonUtilObj = new JSONUtility(dataOb);

						if (filterId == 53) {
							logger.debug("The Filter ID is 53 , so getting the information of STAKEHOLDER Filter");
							FilterData.DataClass dataClassObj = new FilterData.DataClass();
							dataClassObj.setDataName(dataJsonUtilObj.getStringJsonValue("id"));
							dataClassObj.setDataValue(dataJsonUtilObj.getStringJsonValue("name"));
							Map<String, String> stakeHolderGroup = new HashMap<>();
							boolean autoComplete = jsonUtilObjTillOptions.getBooleanJsonValue("autoComplete");
							if (autoComplete == true) {
								stakeHolderGroup = getMapsOfNameIDForAutoComplete("stakeholder", null, null, "listdata", dataJsonUtilObj.getStringJsonValue("id"));
							} else {
								stakeHolderGroup = getStakeHolderGroup(dataJsonUtilObj.getArrayJsonValue("group"));
							}
							dataClassObj.setMapOfData(stakeHolderGroup);
							dataClassList.add(dataClassObj);
						} else {
							FilterData.DataClass dataClassObj = new FilterData.DataClass();
							dataClassObj.setDataName(dataJsonUtilObj.getStringJsonValue("id"));
							dataClassObj.setDataValue(dataJsonUtilObj.getStringJsonValue("name"));
							dataClassList.add(dataClassObj);
						}
					}
				} else {
					logger.warn("The data Array length is 0 for filter id : [ {} ]", filterId);
				}
			}
		} catch (Exception e) {
			logger.error("The Exception occurred while getting filterDataList for filter : [ {} ] , The cause : [ {} ] , exception is : [ {} ] ", filterId, e.getMessage(), e.getStackTrace());
			e.printStackTrace();
		}
		return dataClassList;
	}

	public JSONArray generateStakeHolderGroup(Map<String, String> stakeHolderGroupMap, String stakeHolderKey) {
		logger.debug("Started Execution the method generateStakeHolderGroup");
		JSONArray jsonArray = new JSONArray();
		try {
			if (stakeHolderGroupMap.size() > 0) {
				for (Map.Entry<String, String> entry : stakeHolderGroupMap.entrySet()) {
					JSONObject payloadJson = new JSONObject();
					payloadJson.put("name", entry.getValue());
					payloadJson.put("id", Integer.parseInt(entry.getKey()));
					if (Integer.parseInt(stakeHolderKey) == Integer.parseInt(entry.getKey())) {
						payloadJson.put("selected", true);
					} else {
						payloadJson.put("selected", false);
					}

					jsonArray.put(payloadJson);
					logger.debug("jsonArray is : {}", jsonArray);
				}
			} else {
				logger.error("The StakeHolder Map size is 0 please check , empty JSON Array will be returned");
			}
		} catch (Exception e) {
			logger.error("Got the cause : [ {} ] , exception [ {} ]  please check , empty JSON Array will be returned", e.getMessage(), e.getStackTrace());
			e.printStackTrace();
		}
		logger.debug("jsonArray is : {}", jsonArray);
		return jsonArray;
	}

	// this function will return the map of NAME and ID for autocomplete filterName based on the autoCompleteQuery query define in src/test/resources/APIConfig/Options.cfg
	// taking three argument first one is FilterName (ex. Services , Vendors ) , Second One is EntityTypeID which is basically reportId and Third one is
	// pageEntityTypeId which is Entity Type Id
	public HashMap<String, String> getMapsOfNameIDForAutoComplete(String filterName, String entityTpeId, String pageEntityTypeId, String propertyName) {
		return this.getMapsOfNameIDForAutoComplete(filterName, entityTpeId, pageEntityTypeId, propertyName, null);
	}

	public HashMap<String, String> getMapsOfNameIDForAutoComplete(String filterName, String entityTpeId, String pageEntityTypeId, String propertyName, String roleGroupId) {
		HashMap<String, String> responseMap = new HashMap<>();
		try {
			Options option = new Options();
			HashMap<String, String> queryParams = new HashMap<>();


			String optionConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFilePath");
			String optionConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName");

			String pageType = ParseConfigFile.getValueFromConfigFile(optionConfigFilePath, optionConfigFileName, "pagetype", propertyName);
			String dropDownId = ParseConfigFile.getValueFromConfigFile(optionConfigFilePath, optionConfigFileName, "dropdowntype", filterName);
			if (dropDownId == null) {
				logger.warn("The filter Dropdown ID is not available in the Options.cfg file agains the filter : [ {} ]", filterName);
				return responseMap;
			}
			List<String> charValuesForAutocomplete = Arrays.asList(ParseConfigFile.getValueFromConfigFile(optionConfigFilePath, optionConfigFileName, "autocompletequery", "values").split(","));


			queryParams.put("pageType", pageType);
			queryParams.put("entityTpeId", entityTpeId);

			String entityTpeIdFromConfig = ParseConfigFile.getValueFromConfigFile(optionConfigFilePath, optionConfigFileName, "entitytpeid", filterName);
			if (roleGroupId != null) {
				queryParams.put("roleGroupId", roleGroupId);
				queryParams.put("entityTpeId", entityTpeIdFromConfig);
			} else
				queryParams.put("pageEntityTypeId", pageEntityTypeId);


			for (String autoCompleteQuery : charValuesForAutocomplete) {
				queryParams.put("query", String.valueOf(autoCompleteQuery));
				HttpResponse httpResponse = option.hitOptions(Integer.parseInt(dropDownId), queryParams);
				JSONObject optionAPIResponse = new JSONObject(option.getOptionsJsonStr());
				logger.debug("API Response is : [ {} ]", optionAPIResponse);

				JSONArray optionAPIResponseDataObject = optionAPIResponse.getJSONArray("data");
				if (optionAPIResponseDataObject.length() > 0) {
					logger.debug("Data Object from API Response is : [ {} ]", optionAPIResponseDataObject);
					for (int i = 0; i < optionAPIResponseDataObject.length(); i++) {
						JSONObject nameIdJson = (JSONObject) optionAPIResponseDataObject.get(i);

						if (nameIdJson.has("id") && nameIdJson.has("name")) {
							responseMap.put(nameIdJson.get("id").toString(), nameIdJson.get("name").toString());
						}

					}
				} else {
					logger.warn("The data responce of autocomplete filter is null for entity : [ {} ] , Report : [ {} ], Filter : [ {} ]", pageEntityTypeId, entityTpeId, filterName);
				}

			}
		} catch (Exception e) {
			logger.error("Got Exception while getting autocomplete filter data for entity : [ {} ] , Report : [ {} ], Filter : [ {} ] , Cause : [ {} ], Exception : [ {} ]", pageEntityTypeId, entityTpeId, filterName, e.getMessage(), e.getStackTrace());
			e.printStackTrace();
		}
		logger.debug("response Map is [{}]", responseMap);
		return responseMap;

	}

	public HashMap<String, String> getMapsOfNameIDForAutoComplete(String filterName, String property) {
		return this.getMapsOfNameIDForAutoComplete(filterName, "", "", property);
	}
}
