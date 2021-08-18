package com.sirionlabs.api.commonAPI;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeSuite;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Show extends APIUtils {

	private final static String openingDelimiterForTypeOfValue = "\\[";
	private final static String closingDelimiterForTypeOfValue = "\\]";
	private final static String delimiterForLevel = "->";
	private final static Logger logger = LoggerFactory.getLogger(Show.class);
	private static String showFieldHierarchyPrefix;

	private String fieldHierarchyMappingFilePath;
	private String fieldHierarchyMappingFileName;
	private String typeHierarchyConfigFilePath;
	private String typeHierarchyConfigFileName;

	private String showJsonStr = null;
	private String slaShowJsonStr = null;
	private List<String> tabUrl = new ArrayList<>();

	private boolean saveHtmlResponse = true;

	@BeforeSuite
	public void beforeSuite() {
		fieldHierarchyMappingFilePath = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ShowConfigFilePath"),
				ConfigureConstantFields.getConstantFieldsProperty("ShowConfigFileName"), "FieldHierarchyMappingFilePath");
		fieldHierarchyMappingFileName = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ShowConfigFilePath"),
				ConfigureConstantFields.getConstantFieldsProperty("ShowConfigFileName"), "FieldHierarchyMappingFileName");
		typeHierarchyConfigFilePath = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ShowConfigFilePath"),
				ConfigureConstantFields.getConstantFieldsProperty("ShowConfigFileName"), "FieldTypeFilePath");
		typeHierarchyConfigFileName = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ShowConfigFilePath"),
				ConfigureConstantFields.getConstantFieldsProperty("ShowConfigFileName"), "FieldTypeFileName");
	}

	public static String hitshowPageTabUrl(String url, String payload) {

		String showPageTabResponse = null;
		HttpResponse response;

		try {
			HttpPost postRequest;

			postRequest = new HttpPost(url);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");

			response = APIUtils.postRequest(postRequest, payload);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			showPageTabResponse = EntityUtils.toString(response.getEntity());

		} catch (Exception e) {

			logger.error("Exception occurred while hitting show api for the url {}. {}", url, e.getMessage());
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
		}
		return showPageTabResponse;
	}

	public HttpResponse hitShowVersion2(int entityTypeId, int id) {
		return hitShow(entityTypeId, id, true);
	}

	public HttpResponse hitShow(int entityTypeId, int id) {
		return hitShow(entityTypeId, id, false);
	}

	public HttpResponse hitShow(int entityTypeId, int id, Boolean version2) {
		showFieldHierarchyPrefix = ConfigureConstantFields.getConstantFieldsProperty("ShowFieldHierarchyPrefix");

		HttpResponse response = null;
		try {
			HttpGet getRequest;
			String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);
			String entityIdMappingConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath");
			String entityIdMappingConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
			String showPageUrl = ParseConfigFile.getValueFromConfigFile(entityIdMappingConfigFilePath, entityIdMappingConfigFileName, entityName, "showpage_url");
			String queryString = showPageUrl + id;

			if (version2) {
				queryString = queryString.concat("?version=2.0");
			}

			logger.debug("Query string url formed is {}", queryString);
			getRequest = new HttpGet(queryString);
			getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");
			response = super.getRequest(getRequest, true);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("Show response header {}", oneHeader.toString());
			}
			this.showJsonStr = EntityUtils.toString(response.getEntity());

			if (!ParseJsonResponse.validJsonResponse(showJsonStr)) {
				FileUtils.saveResponseInFile(entityTypeId + " Show API HTML.txt", showJsonStr);
			}

			fieldHierarchyMappingFilePath = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ShowConfigFilePath"),
					ConfigureConstantFields.getConstantFieldsProperty("ShowConfigFileName"), "FieldHierarchyMappingFilePath");
			fieldHierarchyMappingFileName = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ShowConfigFilePath"),
					ConfigureConstantFields.getConstantFieldsProperty("ShowConfigFileName"), "FieldHierarchyMappingFileName");
			typeHierarchyConfigFilePath = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ShowConfigFilePath"),
					ConfigureConstantFields.getConstantFieldsProperty("ShowConfigFileName"), "FieldTypeFilePath");
			typeHierarchyConfigFileName = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ShowConfigFilePath"),
					ConfigureConstantFields.getConstantFieldsProperty("ShowConfigFileName"), "FieldTypeFileName");
		} catch (Exception e) {
			logger.error("Exception while hitting Show Api. {}", e.getMessage());
		}
		return response;
	}

	public HttpResponse hitslasShow(int slaChartId) {
		HttpResponse response = null;
		try {
			HttpGet getRequest;

			String queryString = "slas/show/" + slaChartId;
			logger.debug("Query string url formed is {}", queryString);
			getRequest = new HttpGet(queryString);
			getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");
			response = super.getRequest(getRequest);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("sla Show response header {}", oneHeader.toString());
			}
			this.slaShowJsonStr = EntityUtils.toString(response.getEntity());

		} catch (Exception e) {
			logger.error("Exception while hitting Show Api. {}", e.getMessage());
		}
		return response;
	}

	public boolean verifyShowField(String showResponse, String fieldName, String expectedValue, int entityTypeId) {
		return verifyShowField(showResponse, fieldName, expectedValue, entityTypeId, null, false, false, null);
	}

	public boolean verifyShowField(String showResponse, String fieldName, String expectedValue, int entityTypeId, String fieldType) {
		return verifyShowField(showResponse, fieldName, expectedValue, entityTypeId, fieldType, false, false, null);
	}

	public boolean verifyShowField(String showResponse, String fieldName, String expectedValue, int entityTypeId, String fieldType, boolean isDynamic, boolean isMultiple) {
		return verifyShowField(showResponse, fieldName, expectedValue, entityTypeId, fieldType, isDynamic, isMultiple, null);
	}

	public boolean verifyShowField(String showResponse, String fieldName, String expectedValue, int entityTypeId, String fieldType, String dateFormat) {
		return verifyShowField(showResponse, fieldName, expectedValue, entityTypeId, fieldType, false, false, dateFormat);
	}

	public boolean verifyShowField(String showResponse, String fieldName, String expectedValue, int entityTypeId, String fieldType, boolean isDynamic, boolean isMultiple,
	                               String dateFormat) {
		boolean fieldValidationPass = false;
		String actualValue = null;
		String tempHierarchy;
		String usedHierarchy = null;

		try {
			if (ParseJsonResponse.validJsonResponse(showResponse)) {

				if (fieldType == null) {
					Map<String, String> fieldData = ParseJsonResponse.getFieldByName(showResponse, fieldName);
					if (fieldData.size() > 0 && fieldData.get("type") != null) {
						fieldType = fieldData.get("type");
						isMultiple = Boolean.parseBoolean(fieldData.get("multiple"));
					} else {
						logger.info("Couldn't fetch Type and isMultiple Properties of Field {}", fieldName);
						fieldType = "Not Defined";
						isMultiple = false;
					}
				}
				boolean skipTypeHierarchy = ParseConfigFile.containsSection(fieldHierarchyMappingFilePath, fieldHierarchyMappingFileName, fieldName);
				if (!skipTypeHierarchy) {
					logger.info("Getting Type Hierarchy for Field {} and Type {} and Value {}", fieldName, fieldType, expectedValue);
					String typeHierarchy = this.getTypeHierarchy(fieldType, isMultiple);
					tempHierarchy = showFieldHierarchyPrefix;

					if (isDynamic)
						tempHierarchy += ConfigureConstantFields.getConstantFieldsProperty("JsonObjectsOrderDelimiter") + "dynamicMetadata";

					tempHierarchy += ConfigureConstantFields.getConstantFieldsProperty("JsonObjectsOrderDelimiter") + fieldName + "[object]";

					logger.info("Trying with Default Type Hierarchy for Field {} and Type {} and Value {}", fieldName, fieldType, expectedValue);
					logger.info("Default Type Hierarchy for Field {} is {}", fieldName, tempHierarchy
							+ ConfigureConstantFields.getConstantFieldsProperty("JsonObjectsOrderDelimiter") + typeHierarchy);
					actualValue = this.getActualValue(showResponse, tempHierarchy + ConfigureConstantFields.getConstantFieldsProperty("JsonObjectsOrderDelimiter")
							+ typeHierarchy, expectedValue);

					usedHierarchy = tempHierarchy + ConfigureConstantFields.getConstantFieldsProperty("JsonObjectsOrderDelimiter") + typeHierarchy;
				} else
					logger.info("Skipping Type Hierarchy for Field {} and Type {} and Value {}", fieldName, fieldType, expectedValue);

				if (!(fieldType.equalsIgnoreCase("date") || fieldType.equalsIgnoreCase("dateTime"))) {
					if (fieldType.trim().equalsIgnoreCase("slider")) {
						Double actualSliderValue;
						String temp[] = expectedValue.split(ConfigureConstantFields.getConstantFieldsProperty("SliderRangeDelimiter"));
						Double fromValue = Double.parseDouble(temp[0]);
						Double toValue = Double.parseDouble(temp[1]);

						if (actualValue != null) {
							actualSliderValue = Double.parseDouble(actualValue);

							if (actualSliderValue >= fromValue || actualSliderValue <= toValue)
								fieldValidationPass = true;
						} else {
							logger.info("Couldn't find Expected Value with Type Hierarchy for Field {} and Type {} and Value {}", fieldName, fieldType, expectedValue);
							logger.info("Getting Field Hierarchy for Field {} having Entity Type Id {} and Type {}", fieldName, entityTypeId, fieldType);
							String fieldHierarchy = this.getFieldHierarchy(fieldName, entityTypeId);
							usedHierarchy = fieldHierarchy;

							if (fieldHierarchy != null)
								actualValue = this.getActualValue(showResponse, fieldHierarchy, expectedValue);

							if (actualValue != null) {
								actualValue = actualValue.replaceAll(",", "");
								//added by gaurav bhadani
								actualSliderValue = Double.parseDouble(actualValue);

								if (actualSliderValue >= fromValue || actualSliderValue <= toValue)
									fieldValidationPass = true;
							}
						}
					} else {
						if (fieldType.trim().equalsIgnoreCase("checkbox")) {
							expectedValue = expectedValue.trim().equalsIgnoreCase("yes") ? "true" : expectedValue;
							expectedValue = expectedValue.trim().equalsIgnoreCase("no") ? "false" : expectedValue;
						} else if (fieldType.trim().equalsIgnoreCase("number")) {
							if (actualValue != null) {
								Double actualDoubleValue = Double.parseDouble(actualValue);
								Double expectedDoubleValue = Double.parseDouble(expectedValue);

								Long actualLongValue = actualDoubleValue.longValue();
								Long expectedLongValue = expectedDoubleValue.longValue();

								fieldValidationPass = actualLongValue.equals(expectedLongValue);
							}
						}
						//Added by gaurav bhadani on 18 july
						if (fieldName.trim().equalsIgnoreCase("triggered")) {
							expectedValue = expectedValue.trim().equalsIgnoreCase("yes") ? "true" : expectedValue;
							expectedValue = expectedValue.trim().equalsIgnoreCase("no") ? "false" : expectedValue;
						}

						if (actualValue != null) {
							if (!fieldType.toLowerCase().contains("text") && StringUtils.isNumericRangeValue(expectedValue))
								fieldValidationPass = NumberUtils.verifyNumericRangeValue(actualValue, expectedValue);
							else if (actualValue.toLowerCase().contains(expectedValue.toLowerCase()))
								fieldValidationPass = true;
						}

						if (!fieldValidationPass) {
							logger.info("Getting Field Hierarchy for Field {} having EntityId {} and Type {}", fieldName, entityTypeId, fieldType);
							String fieldHierarchy = this.getFieldHierarchy(fieldName, entityTypeId);
							if (skipTypeHierarchy || fieldHierarchy != null)
								usedHierarchy = fieldHierarchy;

							if (fieldHierarchy != null)
								actualValue = this.getActualValue(showResponse, fieldHierarchy, expectedValue).trim();

							if (actualValue != null) {
								if (!fieldType.toLowerCase().contains("text") && StringUtils.isNumericRangeValue(expectedValue))
									fieldValidationPass = NumberUtils.verifyNumericRangeValue(actualValue, expectedValue);
								else if (actualValue.toLowerCase().contains(expectedValue.toLowerCase()))
									fieldValidationPass = true;
							}

							if (fieldType.trim().equalsIgnoreCase("number")) {
								if (actualValue != null) {
									Double actualDoubleValue = Double.parseDouble(actualValue);
									Double expectedDoubleValue = Double.parseDouble(expectedValue);

									Long actualLongValue = actualDoubleValue.longValue();
									Long expectedLongValue = expectedDoubleValue.longValue();

									fieldValidationPass = actualLongValue.equals(expectedLongValue);
								}
							}
						}
					}
				}
				else if(!fieldType.equalsIgnoreCase("date")){
					logger.info("Getting Field Hierarchy for Field {} having EntityId {} and Type {}", fieldName, entityTypeId, fieldType);
					String fieldHierarchy = this.getFieldHierarchy(fieldName, entityTypeId);
					if (skipTypeHierarchy || fieldHierarchy != null)
						usedHierarchy = fieldHierarchy;

					if (fieldHierarchy != null)
						actualValue = this.getActualValue(showResponse, fieldHierarchy, expectedValue);

					if(actualValue.toLowerCase().contains(expectedValue.toLowerCase()))
						fieldValidationPass = true;
					else
						fieldValidationPass = false;

				}
				else {
					String temp[] = expectedValue.split(ConfigureConstantFields.getConstantFieldsProperty("DateRangeDelimiter"));
					String fromDate = temp[0];
					String toDate = temp[1];

					if (actualValue != null && DateUtils.isDateWithinRange(actualValue, fromDate, toDate, dateFormat))
						fieldValidationPass = true;

					else {
						logger.info("Couldn't find Expected Value with Type Hierarchy for Field {} and Type {} and Value {}", fieldName, fieldType, expectedValue);
						logger.info("Getting Field Hierarchy for Field {} having Entity Type Id {} and Type {}", fieldName, entityTypeId, fieldType);
						String fieldHierarchy = this.getFieldHierarchy(fieldName, entityTypeId);
						usedHierarchy = fieldHierarchy;

						if (fieldHierarchy != null)
							actualValue = this.getActualValue(showResponse, fieldHierarchy, expectedValue);

						//Code added to find DateFormat from Show Page globalData Object for DisplayValues.
						if (fieldHierarchy != null && fieldHierarchy.trim().contains("displayValues")) {
							JSONObject jsonObj = new JSONObject(showResponse);
							dateFormat = jsonObj.getJSONObject("body").getJSONObject("globalData").getString("dateFormatToShow").trim();
						}

						if (actualValue != null && DateUtils.isDateWithinRange(actualValue, fromDate, toDate, dateFormat))
							fieldValidationPass = true;
					}
				}
				if (actualValue == null)
					logger.error("No Such hierarchy found {} for EntityTypeId {}", usedHierarchy, entityTypeId);

				else
					logger.info("Expected Value is {} and Actual Value is {}", expectedValue, actualValue);
			} else {
				logger.error("Show API Response is an Invalid JSON.");
			}
		} catch (Exception e) {
			logger.error("Exception while Verifying Field {} on Show Page of EntityTypeId {}. {}", fieldName, entityTypeId, e.getMessage());
		}
		return fieldValidationPass;
	}

	private String getActualValue(String showResponse, String hierarchy, String expectedValue) {
		String  actualValue = "";
		try {
			JSONObject jsonObj = new JSONObject(showResponse);
			JSONUtility jsonUtil = new JSONUtility(jsonObj);

			String levels[] = hierarchy.split(delimiterForLevel);
			String valueType = null;
			String valueName;
			int i;

			for (i = 0; i < levels.length - 1; i++) {
				String temp[] = levels[i].split(openingDelimiterForTypeOfValue);
				valueName = temp[0].trim();
				temp = temp[1].trim().split(closingDelimiterForTypeOfValue);
				valueType = temp[0].trim();

				if (jsonObj.has(valueName) && jsonUtil.getStringJsonValue(valueName) != null) {
					switch (valueType) {
						case "text":
						case "object":
							jsonObj = new JSONObject(jsonUtil.getStringJsonValue(valueName));
							jsonUtil = new JSONUtility(jsonObj);
							break;

						case "array":
							JSONArray jsonArr = new JSONArray(jsonUtil.getStringJsonValue(valueName));

							for (int j = 0; j < jsonArr.length(); j++) {
								jsonObj = new JSONObject(jsonArr.get(j).toString());
								jsonUtil = new JSONUtility(jsonObj);

								temp = levels[i + 1].split(openingDelimiterForTypeOfValue);
								valueName = temp[0].trim();

								if (j==0 && jsonObj.has(valueName))
									actualValue = jsonUtil.getStringJsonValue(valueName);
								else if (j!=0 && jsonObj.has(valueName))
									actualValue = actualValue+" , " + jsonUtil.getStringJsonValue(valueName);

								else {
									logger.info("Couldn't find {} inside JSONArray", valueName);
									break;
								}

								if (actualValue.toLowerCase().contains(expectedValue.toLowerCase()))
									break;
							}
							break;

						case "stakeholder":
							jsonObj = new JSONObject(jsonUtil.getStringJsonValue(valueName));

							if (jsonObj.has("values")) {
								jsonUtil = new JSONUtility(jsonObj);
								jsonObj = new JSONObject(jsonUtil.getStringJsonValue("values"));

								for (String name : JSONObject.getNames(jsonObj)) {
									JSONObject tempJson = new JSONObject(jsonObj.get(name).toString());
									JSONUtility tempJsonUtil = new JSONUtility(tempJson);

									JSONArray tempJsonArr = new JSONArray(tempJsonUtil.getArrayJsonValue("values").toString());

									for (int k = 0; k < tempJsonArr.length(); k++) {
										tempJson = new JSONObject(tempJsonArr.get(k).toString());
										tempJsonUtil = new JSONUtility(tempJson);

										if (tempJson.has("name"))
											actualValue = tempJsonUtil.getStringJsonValue("name");

										else {
											logger.info("Couldn't find name inside StakeHolder");
											break;
										}

										if (actualValue.toLowerCase().contains(expectedValue.toLowerCase()))
											break;
									}
									if (actualValue != null && actualValue.toLowerCase().contains(expectedValue.toLowerCase()))
										break;
								}
							}
							break;
					}
				} else {
					logger.info("Couldn't find {}", valueName);
					break;
				}
			}

			if (valueType != null && !valueType.equalsIgnoreCase("array") && !valueType.equalsIgnoreCase("stakeholder")) {
				String temp[] = levels[i].split(openingDelimiterForTypeOfValue);
				valueName = temp[0].trim();
				temp = temp[1].trim().split(closingDelimiterForTypeOfValue);
				valueType = temp[0].trim();

				switch (valueType) {
					case "int":
						actualValue = Integer.toString(jsonObj.getInt(valueName));
						break;

					case "boolean":
						actualValue = Boolean.toString(jsonObj.getBoolean(valueName));
						break;

					case "long":
						actualValue = Long.toString(jsonObj.getLong(valueName));
						break;

					case "double":
						actualValue = Double.toString(jsonObj.getDouble(valueName));
						break;

					default:
						actualValue = jsonObj.getString(valueName);
						break;
				}
			}
		} catch (Exception e) {
			logger.error("Exception while fetching Actual Value using Hierarchy {}. {}", hierarchy, e.getMessage());
		}
		return actualValue;
	}

	private String getTypeHierarchy(String fieldType, boolean isMultiple) throws IOException {
		String typeHierarchy;
		Map<String, String> showFieldTypeConfig = ParseConfigFile.parseConfigFile(typeHierarchyConfigFilePath, typeHierarchyConfigFileName, "=");

		//All the keys from config file are in lowercase letters only. Always specify key in lowercase while fetching.
		switch (fieldType.toLowerCase()) {
			case "select":
				if (isMultiple)
					typeHierarchy = showFieldTypeConfig.get("selectwithmultiple");
				else
					typeHierarchy = showFieldTypeConfig.get("selectwithoutmultiple");
				break;

			default:
				typeHierarchy = showFieldTypeConfig.get(fieldType.toLowerCase());
				break;
		}

		if (typeHierarchy == null)
			logger.info("Couldn't find Type Hierarchy for Type {}", fieldType);

		return typeHierarchy;
	}

	private String getFieldHierarchy(String fieldName, int entityTypeId) {
		String fieldHierarchy;
		fieldHierarchy = ParseConfigFile.getValueFromConfigFile(fieldHierarchyMappingFilePath, fieldHierarchyMappingFileName, fieldName, Integer.toString(entityTypeId));

		if (fieldHierarchy == null)
			fieldHierarchy = ParseConfigFile.getValueFromConfigFile(fieldHierarchyMappingFilePath, fieldHierarchyMappingFileName, fieldName, "0");

		if (fieldHierarchy == null)
			logger.info("Couldn't find Field Hierarchy for Field {} of EntityTypeId {}", fieldName, entityTypeId);

		else
			logger.info("Field Hierarchy for Field {} is {}", fieldName, fieldHierarchy);

		return fieldHierarchy;
	}

	public boolean verifyShowFieldForExclusion(String showResponse, String fieldName, String expectedValue, int entityTypeId, String fieldType, boolean isDynamic,
	                                           boolean isMultiple, String excludedItems[]) {
		logger.info("Validating the Exclusion Part for Field {}", fieldName);
		boolean fieldValidationPass = false;
		String actualValue = null;
		String tempHierarchy;
		try {
			boolean skipTypeHierarchy = ParseConfigFile.containsSection(fieldHierarchyMappingFilePath, fieldHierarchyMappingFileName, fieldName);

			if (!skipTypeHierarchy) {
				logger.info("Getting Type Hierarchy for Field {} and Type {} for Exclusion", fieldName, fieldType);
				String typeHierarchy = this.getTypeHierarchy(fieldType, isMultiple);

				tempHierarchy = showFieldHierarchyPrefix;

				if (isDynamic)
					tempHierarchy += ConfigureConstantFields.getConstantFieldsProperty("JsonObjectsOrderDelimiter") + "dynamicMetadata";

				tempHierarchy += ConfigureConstantFields.getConstantFieldsProperty("JsonObjectsOrderDelimiter") + fieldName + "[object]";

				logger.info("Trying with Default Type Hierarchy for Field {} and Type {} for Exclusion", fieldName, fieldType);
				logger.info("Default Type Hierarchy for Field {} is {}", fieldName, tempHierarchy
						+ ConfigureConstantFields.getConstantFieldsProperty("JsonObjectsOrderDelimiter") + typeHierarchy);
				actualValue = this.getActualValue(showResponse, tempHierarchy + ConfigureConstantFields.getConstantFieldsProperty("JsonObjectsOrderDelimiter")
						+ typeHierarchy, expectedValue);
			} else
				logger.info("Skipping Type Hierarchy for Field {} and Type {} and Value {}", fieldName, fieldType, expectedValue);

			if (actualValue != null) {
				for (String excludedItem : excludedItems) {
					logger.info("Verifying that Field {} doesn't have Excluded Item {}", fieldName, excludedItem);
					if (actualValue.toLowerCase().contains(excludedItem.trim().toLowerCase())) {
						fieldValidationPass = false;
						logger.error("Field {} of EntityTypeId {}  has Excluded Value {}. Actual Value is {}", fieldName, entityTypeId, excludedItem, actualValue);
						break;
					}

					fieldValidationPass = true;
				}
			} else {
				logger.info("Getting Field Hierarchy for Field {} having EntityId {} and Type {} for Exclusion", fieldName, entityTypeId, fieldType);
				String fieldHierarchy = this.getFieldHierarchy(fieldName, entityTypeId);

				if (fieldHierarchy != null) {
					for (String excludedItem : excludedItems) {
						logger.info("Verifying that Field {} doesn't have Excluded Item {}", fieldName, excludedItem);
						actualValue = this.getActualValue(showResponse, fieldHierarchy, excludedItem.toLowerCase());

						if (actualValue != null && actualValue.toLowerCase().contains(excludedItem.trim().toLowerCase())) {
							fieldValidationPass = false;
							logger.error("Field {} of EntityTypeId {} has Excluded Value {}. Actual Value is {}", fieldName, entityTypeId, excludedItem, actualValue);
							break;
						}

						fieldValidationPass = true;
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Verifying Field {} of EntityTypeId {} on Show Page. {}", fieldName, entityTypeId, e.getMessage());
		}
		return fieldValidationPass;
	}

	public boolean verifyShowFieldForExclusion(String showResponse, String fieldName, String expectedValue, int entityTypeId, String excludedItems[]) {
		return verifyShowFieldForExclusion(showResponse, fieldName, expectedValue, entityTypeId, "text", false, false, excludedItems);
	}

	public boolean isShowPageBlocked(String showResponse) {
		return APIUtils.isPermissionDeniedInResponse(showResponse);
	}

	public boolean isShowPageAccessible(String showResponse) {
		return !APIUtils.isPermissionDeniedInResponse(showResponse);
	}

	public String getShowJsonStr() {
		return this.showJsonStr;
	}

	public String getSlaShowJsonStr() {
		return this.slaShowJsonStr;
	}

	public boolean isShowPageBlockedForBulkAction(String showJsonStr) {
		boolean isPageBlocked = false;
		try {
			if (ParseJsonResponse.validJsonResponse(showJsonStr)) {
				JSONObject jsonObj = new JSONObject(showJsonStr);
				JSONObject jsonError = jsonObj.getJSONObject("body").getJSONObject("errors");

				if (jsonError.has("genericErrors")) {
					JSONArray jsonArr = jsonError.getJSONArray("genericErrors");

					for (int i = 0; i < jsonArr.length(); i++) {
						JSONObject temp = new JSONObject(jsonArr.getJSONObject(i).toString());
						if (temp.has("message") && temp.getString("message").toLowerCase().contains("entity is blocked for bulk action")) {
							isPageBlocked = true;
							break;
						}
					}
				}
			} else {
				logger.info("Show Page Response is not a valid JSON.");
			}
		} catch (Exception e) {
			logger.error("Exception while checking is show page blocked for Bulkaction. {}", e.getMessage());
		}
		return isPageBlocked;
	}

	public boolean isShowPageAccessibleForBulkAction(String showJsonStr) {
		return !isShowPageBlockedForBulkAction(showJsonStr);
	}

	public List<String> getShowPageTabUrl(String showPageResponseStr, TabURL requiredUrl) {
		List<String> requiredUrlList = new ArrayList<>();
		tabUrl.clear();

		try {
			JSONObject jsonResponse = new JSONObject(showPageResponseStr);
			JSONObject layoutComponent = jsonResponse.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent");
			JSONArray allFieldsArray = layoutComponent.getJSONArray("fields");
			List<String> showPageTabUrl = getallTabUrl(allFieldsArray);
			logger.info("showPageTabUrl = {}", showPageTabUrl, showPageTabUrl.size());

			for (String tabUrl : showPageTabUrl) {
				String[] splitUrl = tabUrl.split("::");
				String layoutUrl = splitUrl[0];
				String dataUrl = splitUrl[1];

				if (requiredUrl == TabURL.layoutURL)
					requiredUrlList.add(layoutUrl);

				else if (requiredUrl == TabURL.dataURL)
					requiredUrlList.add(dataUrl);

				else
					logger.error("Invalid value for requiredUrl is passed");
			}
		} catch (Exception e) {
			logger.error("Exception in getShowPageTabUrl method. {}", e.getMessage());
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
		}
		return requiredUrlList;
	}

	private List<String> getallTabUrl(JSONArray fieldsArray) throws Exception {
		int j = 0;

		while (j < fieldsArray.length()) {
			if (fieldsArray.getJSONObject(j).has("fields")) {
				//recursive call
				getallTabUrl(fieldsArray.getJSONObject(j).getJSONArray("fields"));
			}
			if (fieldsArray.getJSONObject(j).has("layoutURL")) {
				//fetching tab url

				String layoutUrl = fieldsArray.getJSONObject(j).getString("layoutURL");
				String dataUrl = fieldsArray.getJSONObject(j).getString("dataURL");
				tabUrl.add(layoutUrl + "::" + dataUrl);
			}
			j++;
		}
		return tabUrl;
	}

	// this function will check whether showJsonStr has the clickable Actions "entityActions" in it
	// if yes then it will return the list of String which will have clickable action api URL and it's type (GET or POST)
	// Obsolete Not in Use
	public List<String> getLayOutInfoActionsUrlProperties(String showJsonStr, String entityActions) {
		List<String> clickableActionApiDetails = new ArrayList<>();
		JSONObject jsonObj = new JSONObject(showJsonStr);
		JSONObject temp = jsonObj.getJSONObject("body").getJSONObject("layoutInfo");
		JSONArray viewsArray = new JSONArray(temp.get("actions").toString());
		logger.debug("viewsArray:{} ,viewsArray:{}", viewsArray, viewsArray.length());

		for (int i = 0; i < viewsArray.length(); i++) {
			JSONObject jsonObject = viewsArray.getJSONObject(i);
			if (jsonObject.has("name") && jsonObject.get("name").toString().toLowerCase().contentEquals(entityActions)) {
				clickableActionApiDetails.add(jsonObject.get("api").toString());
				clickableActionApiDetails.add(jsonObject.get("requestType").toString());
				return clickableActionApiDetails;
			}
		}
		return null;
	}

	// this function will return the hashmap of clickable actions with their few field details in internal hash Map
	public HashMap<String, HashMap<String, String>> getLayOutInfoActionsHashMap(String showJsonStr) {
		logger.debug("Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
		HashMap<String, HashMap<String, String>> hashMapForClickableActions = new HashMap<>();
		JSONObject jsonObj = new JSONObject(showJsonStr);
		JSONObject temp = jsonObj.getJSONObject("body").getJSONObject("layoutInfo");
		JSONArray viewsArray = new JSONArray(temp.get("actions").toString());
		logger.debug("viewsArray:{} ,viewsArray:{}", viewsArray, viewsArray.length());
		for (int i = 0; i < viewsArray.length(); i++) {

			HashMap<String, String> internalhashMap = new HashMap<>();
			JSONObject jsonObject = viewsArray.getJSONObject(i);
			if (jsonObject.has("name")) {
				String[] propKeys = {"name", "api", "type", "requestType"};

				for (String propKey : propKeys) {
					if (jsonObject.has(propKey)) {
						internalhashMap.put(propKey, jsonObject.get(propKey).toString());
					} else {
						internalhashMap.put(propKey, null);
					}
				}
				logger.debug("keys:{} , internalhashMap :{}", jsonObject.getString("name"), internalhashMap);
				hashMapForClickableActions.put(jsonObject.getString("name").toLowerCase(), internalhashMap);
			}
		}
		logger.debug("hashMapForClickableActions :{}", hashMapForClickableActions);
		return hashMapForClickableActions;
	}


	// this function will return dynamic field Value
	public String getValueOfDynamicField(String showJsonStr, String key) {
		String value = null;
		JSONObject jsonObj = new JSONObject(showJsonStr);
		JSONObject temp = jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject(key);

		if (temp.has("values"))
			value = temp.get("values").toString();
		else {
			logger.warn("Show Page API don't have value for key [{}] in Dynamic Field Object ", key);
		}
		return value;
	}

	// this function will return dynamic field Value which is having values as json array
	public List<String> getValueOfDynamicFieldWhenValueFieldIsArray(String showJsonStr, String key) {

		List<String> values = new ArrayList<>();
		JSONObject jsonObj = new JSONObject(showJsonStr);
		JSONObject temp = jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject(key);

		if (temp.has("values")) {
			JSONArray valuesArray = temp.getJSONArray("values");
			for (int i = 0; i < valuesArray.length(); i++) {
				if (valuesArray.getJSONObject(i).has("name"))
					values.add(valuesArray.getJSONObject(i).getString("name"));
			}
		} else {
			logger.warn("Show Page API don't have value for key [{}] in Dynamic Field Object ", key);
		}
		return values;
	}

	// this function will return dynamic field Value which is having values as json object
	public String getValueOfDynamicFieldWhenValueFieldIsObject(String showJsonStr, String key) {

		String value = null;
		JSONObject jsonObj = new JSONObject(showJsonStr);
		JSONObject temp = jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject(key);

		if (temp.has("values")) {
			if (temp.getJSONObject("values").has("name"))
				value = temp.getJSONObject("values").getString("name");
		} else {
			logger.warn("Show Page API don't have value for key [{}] in Dynamic Field Object ", key);
		}
		return value;
	}

	// this function will return stakeholder field Value
	public List<String> getValueOfStakeholderField(String showJsonStr, String key) {

		List<String> values = new ArrayList<>();
		JSONObject jsonObj = new JSONObject(showJsonStr);
		JSONObject temp = jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values");

		if (temp.has(key)) {
			JSONArray valuesArray = temp.getJSONObject(key).getJSONArray("values");
			for (int i = 0; i < valuesArray.length(); i++) {
				values.add(valuesArray.getJSONObject(i).getString("name"));
			}
		} else {
			logger.warn("Show Page API don't have value for key [{}] in Dynamic Field Object ", key);
		}
		return values;
	}

	// this function will return dynamic table field Value
	public List<String> getValueOfDynamicTableField(String showJsonStr, String dynamicTableFieldName, String key) {

		List<String> values = new ArrayList<>();
		JSONObject jsonObj = new JSONObject(showJsonStr);
		JSONObject temp = jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject(dynamicTableFieldName);

		if (temp.has("values")) {
			JSONArray valuesArray = temp.getJSONArray("values");
			for (int i = 0; i < valuesArray.length(); i++) {
				if (valuesArray.getJSONObject(i).has(key)) {
					if (valuesArray.getJSONObject(i).getJSONObject(key).has("values")) {
						String dynamicFieldValues = valuesArray.getJSONObject(i).getJSONObject(key).get("values").toString();
						if (dynamicFieldValues.startsWith("{") && dynamicFieldValues.endsWith("}")) {
							if (valuesArray.getJSONObject(i).getJSONObject(key).getJSONObject("values").has("name")) {
								values.add(valuesArray.getJSONObject(i).getJSONObject(key).getJSONObject("values").getString("name"));
							}
						} else
							values.add(dynamicFieldValues);
					}
				}
			}
		} else {
			logger.warn("Show Page API don't have value for key [{}] in Dynamic Field Object ", key);
		}
		return values;
	}


	public HttpResponse hitShowAPI(int entityTypeId, int id) throws IOException, ConfigurationException {
		logger.debug("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
		HttpResponse response;
		String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);
		String entityIdMappingConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath");
		String entityIdMappingConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
		String showPageUrl = ParseConfigFile.getValueFromConfigFile(entityIdMappingConfigFilePath, entityIdMappingConfigFileName, entityName, "showpage_url");

		String queryString = showPageUrl + id;
		HttpPost postRequest = new HttpPost(queryString);
		response = APIUtils.postRequest(postRequest, "{}");
		logger.debug("Response is : {}", response.getStatusLine().toString());

		Header[] headers = response.getAllHeaders();
		for (Header oneHeader : headers) {
			logger.debug("Show Page API : response header {}", oneHeader.toString());
		}

		showJsonStr = EntityUtils.toString(response.getEntity());
		return response;
	}


	public HttpResponse hitShowGetAPI(int entityTypeId, int id) {

		HttpResponse response = null;
		try {
			HttpGet getRequest;
			String entityName = ConfigureConstantFields.getEntityNameById(entityTypeId);
			String entityIdMappingConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath");
			String entityIdMappingConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
			String showPageUrl = ParseConfigFile.getValueFromConfigFile(entityIdMappingConfigFilePath, entityIdMappingConfigFileName, entityName, "showpage_url");
			String queryString = showPageUrl + id;
			logger.debug("Query string url formed is {}", queryString);
			getRequest = new HttpGet(queryString);
			getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");
			response = super.getRequest(getRequest, true);
			logger.debug("Response status is {}", response.getStatusLine().toString());
			Header[] headers = response.getAllHeaders();
			for (Header oneHeader : headers) {
				logger.debug("Show response header {}", oneHeader.toString());
			}
			this.showJsonStr = EntityUtils.toString(response.getEntity());
		} catch (Exception e) {
			logger.error("Exception while hitting Show Api. {}", e.getMessage());
		}
		return response;
	}

	// this function will hit the Create User Preference API taking input entityURLId and payload
	public String hitEntityClickableActionsAPIs(String uriForClickableActions, String requestType, String payload) throws Exception {
		HttpResponse response = null;
		JSONObject jsonObj;
		logger.info("uriForClickableActions: {}", uriForClickableActions);
		logger.info("payload is: {}", payload);
		jsonObj = new JSONObject(payload);

		if (requestType.contentEquals("post")) {
			HttpPost postRequest = new HttpPost(uriForClickableActions);
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");
			response = APIUtils.postRequest(postRequest, jsonObj.toString());
		}

		if (requestType.contentEquals("get")) {
			HttpGet getRequest = new HttpGet(uriForClickableActions);
			getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			getRequest.addHeader("Accept-Encoding", "gzip, deflate");
			response = super.getRequest(getRequest);
		}

		Header[] headers = response.getAllHeaders();
		for (Header oneHeader : headers) {
			logger.debug("Entity Clickable Actions API: response header {}", oneHeader.toString());
		}
		return EntityUtils.toString(response.getEntity());
	}

	private boolean isShowPageBlockedForBulkEdit(String showJsonStr) {
		boolean isPageBlocked = false;
		try {
			if (ParseJsonResponse.validJsonResponse(showJsonStr)) {
				JSONObject jsonObj = new JSONObject(showJsonStr);
				JSONObject jsonError = jsonObj.getJSONObject("body").getJSONObject("errors");

				if (jsonError.has("genericErrors")) {
					JSONArray jsonArr = jsonError.getJSONArray("genericErrors");

					for (int i = 0; i < jsonArr.length(); i++) {
						JSONObject temp = new JSONObject(jsonArr.getJSONObject(i).toString());
						if (temp.has("message") && temp.getString("message").trim().toLowerCase().contains("entity is blocked for bulk edit")) {
							isPageBlocked = true;
							break;
						}
					}
				}
			} else {
				logger.info("Show Page Response is not a valid JSON.");
			}
		} catch (Exception e) {
			logger.error("Exception while checking is show page blocked for BulkEdit. {} \nShow JsonStr: {}", e.getMessage(), showJsonStr);
		}
		return isPageBlocked;
	}

	public boolean isShowPageAccessibleForBulkEdit(String showJsonStr) {
		return !isShowPageBlockedForBulkEdit(showJsonStr);
	}

	public String hitShowForWorkflowAction(String apiPath, String showResponse, String actionName) {
		try {
			JSONObject jsonObj = new JSONObject(showResponse);
			jsonObj = jsonObj.getJSONObject("body").getJSONObject("data");
			String payload = "{\"body\":{\"data\":" + jsonObj.toString() + "}}";

			HttpPost postRequest = new HttpPost(apiPath);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");
			HttpResponse httpResponse = APIUtils.postRequest(postRequest, payload);
			return EntityUtils.toString(httpResponse.getEntity());
		} catch (Exception e) {
			logger.error("Exception while Hitting Show API for Workflow Action [{}] . {}", actionName, e.getStackTrace());
		}
		return null;
	}

	public String hitShowForWorkflowActionV2(String apiPath, String showResponse, String actionName) {
		try {
			JSONObject jsonObj = new JSONObject(showResponse);
			jsonObj = jsonObj.getJSONObject("body").getJSONObject("data");
			String payload = "{\"body\":{\"data\":" + jsonObj.toString() + "}}";

			apiPath = apiPath + "?version=2.0";

			HttpPost postRequest = new HttpPost(apiPath);
			postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
			postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
			postRequest.addHeader("Accept-Encoding", "gzip, deflate");
			HttpResponse httpResponse = APIUtils.postRequest(postRequest, payload);
			return EntityUtils.toString(httpResponse.getEntity());
		} catch (Exception e) {
			logger.error("Exception while Hitting Show API for Workflow Action [{}] . {}", actionName, e.getStackTrace());
		}
		return null;
	}
	public enum TabURL {layoutURL, dataURL}
}