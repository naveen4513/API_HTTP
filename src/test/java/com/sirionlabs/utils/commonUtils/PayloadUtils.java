package com.sirionlabs.utils.commonUtils;

import com.sirionlabs.api.dashboard.DashboardLocalFilters;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.test.dashboard.TestDashboard;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by akshay.rohilla on 7/13/2017.
 */
public class PayloadUtils {

	private final static Logger logger = LoggerFactory.getLogger(PayloadUtils.class);
	private static String entityCreationExtraFieldsDelimiterForValues = "->";

	public static String getPayloadForNewGovernanceBody(String contractName, int contractId, String supplierName, int supplierId) {
		if (contractName != null)
			return "{\"body\":{\"data\":{\"supplier\":{\"name\":\"supplier\",\"values\":{\"name\":\"" + supplierName + "\",\"id\":" + supplierId + "}}," +
					"\"contracts\":{\"name\":\"contracts\",\"values\":[{\"name\":\"" + contractName + "\",\"id\":" + contractId + ",\"parentId\":" + supplierId + "}]}}}}";
		else
			return "{\"body\":{\"data\":{\"supplier\":{\"name\":\"supplier\",\"values\":{\"name\":\"" + supplierName + "\",\"id\":" + supplierId + "}}}}}";
	}

	public static String getPayloadForNew(String supplierName, int supplierId, String parentEntityTypeName, int parentEntityTypeId) {
		return getPayloadForNew(null, -1, supplierName, supplierId, parentEntityTypeName,
				parentEntityTypeId);
	}

	public static String getPayloadForNew(String sourceTitleName, int sourceTitleId, String supplierName, int supplierId,
	                                      String parentEntityTypeName, int parentEntityTypeId) {
		String payload = "{\"body\":{\"data\":{\"sourceTitle\":{";
		if (sourceTitleName != null && sourceTitleId != -1)
			payload += "\"values\":{\"name\":\"" + sourceTitleName + "\",\"id\":" + sourceTitleId + "}}";
		else
			payload += "}";

		payload += ",\"supplier\":{\"name\":\"supplier\",\"values\":{\"name\":\"" + supplierName + "\",\"id\":" + supplierId + "}}," +
				"\"parentEntityType\":{\"name\":\"parentEntityType\"" + ",\"values\":{\"name\":\"" + parentEntityTypeName + "\",\"id\":" + parentEntityTypeId + "}}}}}";

		return payload;
	}

	public static String getPayloadForNew(String sourceTitleName, int sourceTitleId, String supplierName, int supplierId, String parentEntityTypeName, int parentEntityTypeId
			, String parentName, int parentId) {
		return "{\"body\":{\"data\":{\"sourceTitle\":{\"values\":{\"name\":\"" + sourceTitleName + "\",\"id\":" + sourceTitleId +
				",\"parentId\":" + parentId + ",\"parentName\":\"" + parentName + "\"}},\"supplier\":{\"name\":\"supplier\",\"values\":{\"name\":\"" + supplierName
				+ "\",\"id\":" + supplierId + "}},\"parentEntityType\":{\"name\":\"parentEntityType\",\"values\":{\"name\":\"" + parentEntityTypeName +
				"\",\"id\":" + parentEntityTypeId + "}}}}}";
	}

	public static String getPayloadForCreate(String newResponseStr, Map<String, String> allRequiredFields, Map<String, String> extraFields, Map<String, String> excelFields) {
		return getPayloadForCreate(newResponseStr, allRequiredFields, extraFields, excelFields, null, null);
	}

	public static String getPayloadForCreate(String newResponseStr, Map<String, String> allRequiredFields, Map<String, String> extraFields, Map<String, String> excelFields,
	                                         String extraFieldsConfigFilePath, String extraFieldsConfigFileName) {
		JSONObject finalJsonObj = null;

		try {
			String delimiterForLevel = ConfigureConstantFields.getConstantFieldsProperty("JsonObjectsOrderDelimiter");

			JSONObject jsonObj = new JSONObject(newResponseStr);
			String payload = "{" + "\"body\":{" + "\"data\":";
			payload += jsonObj.getJSONObject("body").getJSONObject("data").toString();
			payload += "}}";

			finalJsonObj = new JSONObject(payload);
			JSONObject partialJsonObj;

			if (allRequiredFields != null) {
				List<String> allRequiredFieldsKeys = new ArrayList<>(allRequiredFields.keySet());
				logger.info("Building Json for Required Fields");
				for (int i = 0; i < allRequiredFields.size(); i++) {
					Map<String, String> field = ParseJsonResponse.getFieldByName(newResponseStr, allRequiredFieldsKeys.get(i));
					if (field.size() > 0) {
						try {
							String fieldHierarchy = ParseJsonResponse.getFieldHierarchy(field);
							String fieldType = field.get("type");

							if (fieldType != null) {
								logger.info("Checking if Response contains hierarchy {}", fieldHierarchy);
								if (ParseJsonResponse.containsHierarchy(newResponseStr, fieldType, fieldHierarchy)) {
									String levels[] = fieldHierarchy.split(delimiterForLevel);

									logger.info("Checking if Last Node is populated for hierarchy {}", fieldHierarchy);
									if (!ParseJsonResponse.isLastNodePopulated(newResponseStr, fieldType, field.get("multiple"), fieldHierarchy)) {
										JSONObject tempJsonObj;
										tempJsonObj = PayloadUtils.buildJsonObjectForCreate(new JSONObject(finalJsonObj.getJSONObject("body").toString()), fieldHierarchy,
												field, levels[levels.length - 2], allRequiredFields.get(allRequiredFieldsKeys.get(i)));
										partialJsonObj = new JSONObject();
										partialJsonObj.put("body", tempJsonObj);
										finalJsonObj = partialJsonObj;
									}
								} else
									logger.error("Hierarchy {} not found. Couldn't create payload for Create Api", fieldHierarchy);
							} else {
								logger.error("Couldn't find Field Type for field {}.", field.get("name"));
							}
						} catch (Exception e) {
							logger.error("Exception while Creating Payload for Field {}. {}", field.get("label"), e.getStackTrace());
						}
					}
				}
			}

			if (excelFields != null) {
				List<String> excelFieldsKeys = new ArrayList<>(excelFields.keySet());
				logger.info("Building Json for Excel Fields");
				for (int l = 0; l < excelFields.size(); l++) {

					if (excelFields.get(excelFieldsKeys.get(l)) == null || !excelFields.get(excelFieldsKeys.get(l)).trim().equalsIgnoreCase("")) {
						Map<String, String> field = ParseJsonResponse.getFieldByLabel(newResponseStr, excelFieldsKeys.get(l));
						if (field.size() > 0) {
							try {
								String fieldHierarchy = ParseJsonResponse.getFieldHierarchy(field);
								String fieldType = field.get("type");

								if (fieldType != null) {
									logger.info("Checking if Response contains hierarchy {}", fieldHierarchy);
									if (ParseJsonResponse.containsHierarchy(newResponseStr, fieldType, fieldHierarchy)) {
										String levels[] = fieldHierarchy.split(delimiterForLevel);

										logger.info("Checking if Last Node is populated for hierarchy {}", fieldHierarchy);
										if (!ParseJsonResponse.isLastNodePopulated(newResponseStr, fieldType, field.get("multiple"), fieldHierarchy)) {
											JSONObject tempJsonObj;
											tempJsonObj = PayloadUtils.buildJsonObjectForCreate(new JSONObject(finalJsonObj.getJSONObject("body").toString()), fieldHierarchy,
													field, levels[levels.length - 2], excelFields.get(excelFieldsKeys.get(l)));
											partialJsonObj = new JSONObject();
											partialJsonObj.put("body", tempJsonObj);
											finalJsonObj = partialJsonObj;
										}
									} else
										logger.error("Hierarchy {} not found. Couldn't create payload for Create Api", fieldHierarchy);
								} else {
									logger.error("Couldn't find Field Type for field {}.", field.get("name"));
								}
							} catch (Exception e) {
								logger.error("Exception while Creating Payload for Field {}. {}", field.get("label"), e.getStackTrace());
							}
						}
					}
				}
			}

			logger.info("Building Json for Extra Fields");
			List<String> allExtraFieldsKeys = new ArrayList<>(extraFields.keySet());
			for (int j = 0; j < extraFields.size(); j++) {
				JSONObject tempJsonObj = null;

				if (extraFields.get(allExtraFieldsKeys.get(j)) != null) {
					String extraFieldSplit[] = extraFields.get(allExtraFieldsKeys.get(j)).split(PayloadUtils.entityCreationExtraFieldsDelimiterForValues);
					String extraFieldHierarchy = "body" + delimiterForLevel + "data" + delimiterForLevel + allExtraFieldsKeys.get(j);
					String fieldValue;
					String endNode;

					if (extraFieldSplit.length > 1) {
						extraFieldHierarchy += delimiterForLevel + extraFieldSplit[0].trim();
						fieldValue = extraFieldSplit[1].trim();
						endNode = allExtraFieldsKeys.get(j);
					} else {
						fieldValue = extraFieldSplit[0].trim();
						endNode = "data";
					}
					tempJsonObj = PayloadUtils.buildJsonObjectForCreate(new JSONObject(finalJsonObj.getJSONObject("body").toString()), extraFieldHierarchy, null,
							endNode, fieldValue);
				}
				partialJsonObj = new JSONObject();
				partialJsonObj.put("body", tempJsonObj);
				finalJsonObj = partialJsonObj;
			}

			JSONArray allFields = finalJsonObj.getJSONObject("body").getJSONObject("data").names();
			logger.info("Removing Options from Json");
			for (int k = 0; k < allFields.length(); k++) {
				String fieldName = allFields.get(k).toString();

				if (finalJsonObj.getJSONObject("body").getJSONObject("data").getJSONObject(fieldName).has("options"))
					finalJsonObj.getJSONObject("body").getJSONObject("data").getJSONObject(fieldName).remove("options");
			}
		} catch (Exception e) {
			logger.error("Exception while creating payload for Create Api. {}", e.getMessage());
		}
		return finalJsonObj.toString();
	}

	public static String getPayloadForCreateExcel(String newResponseStr, Map<String, String> allRequiredFields, Map<String, String> extraFields, Map<String, String> excelFields,
	                                              String extraFieldsConfigFilePath, String extraFieldsConfigFileName) {
		JSONObject finalJsonObj = null;

		try {
			String delimiterForLevel = ConfigureConstantFields.getConstantFieldsProperty("JsonObjectsOrderDelimiter");

			JSONObject jsonObj = new JSONObject(newResponseStr);
			String payload = "{" + "\"body\":{" + "\"data\":";
			payload += jsonObj.getJSONObject("body").getJSONObject("data").toString();
			payload += "}}";

			finalJsonObj = new JSONObject(payload);
			JSONObject partialJsonObj;

			if (allRequiredFields != null) {
				List<String> allRequiredFieldsKeys = new ArrayList<>(allRequiredFields.keySet());
				logger.info("Building Json for Required Fields");
				for (int i = 0; i < allRequiredFields.size(); i++) {
					Map<String, String> field = ParseJsonResponse.getFieldByName(newResponseStr, allRequiredFieldsKeys.get(i));
					if (field.size() > 0) {
						try {
							String fieldHierarchy = ParseJsonResponse.getFieldHierarchy(field);
							String fieldType = field.get("type");

							if (fieldType != null) {
								logger.info("Checking if Response contains hierarchy {}", fieldHierarchy);
								if (ParseJsonResponse.containsHierarchy(newResponseStr, fieldType, fieldHierarchy)) {
									String levels[] = fieldHierarchy.split(delimiterForLevel);

									/*logger.info("Checking if Last Node is populated for hierarchy {}", fieldHierarchy);
									if (!ParseJsonResponse.isLastNodePopulated(newResponseStr, fieldType, field.get("multiple"), fieldHierarchy)) {*/

									//For Stakeholder
									if (field.get("model") != null && field.get("model").trim().equalsIgnoreCase("stakeHolders.values")) {
										JSONObject tempJsonObj;
										tempJsonObj = PayloadUtils.buildJsonObjectForCreate(new JSONObject(finalJsonObj.getJSONObject("body").toString()), fieldHierarchy,
												field, levels[levels.length - 2], allRequiredFields.get("stakeHolders"));
										partialJsonObj = new JSONObject();
										partialJsonObj.put("body", tempJsonObj);
										finalJsonObj = partialJsonObj;
									} else if (field.get("displayMode").trim().equalsIgnoreCase("editable")) {
										JSONObject tempJsonObj;
										tempJsonObj = PayloadUtils.buildJsonObjectForCreate(new JSONObject(finalJsonObj.getJSONObject("body").toString()), fieldHierarchy,
												field, levels[levels.length - 2], allRequiredFields.get(allRequiredFieldsKeys.get(i)));
										partialJsonObj = new JSONObject();
										partialJsonObj.put("body", tempJsonObj);
										finalJsonObj = partialJsonObj;
									}
//									}
								} else
									logger.error("Hierarchy {} not found. Couldn't create payload for Create Api", fieldHierarchy);
							} else {
								logger.error("Couldn't find Field Type for field {}.", field.get("name"));
							}
						} catch (Exception e) {
							logger.error("Exception while Creating Payload for Field {}. {}", field.get("label"), e.getStackTrace());
						}
					}
				}
			}

			if (excelFields != null) {
				List<String> excelFieldsKeys = new ArrayList<>(excelFields.keySet());
				logger.info("Building Json for Excel Fields");
				for (int l = 0; l < excelFields.size(); l++) {

					if (excelFields.get(excelFieldsKeys.get(l)) == null || !excelFields.get(excelFieldsKeys.get(l)).trim().equalsIgnoreCase("")) {
						Map<String, String> field = ParseJsonResponse.getFieldByLabel(newResponseStr, excelFieldsKeys.get(l));
						if (field.size() > 0) {
							try {
								String fieldHierarchy = ParseJsonResponse.getFieldHierarchy(field);
								String fieldType = field.get("type");

								if (fieldType != null) {
									logger.info("Checking if Response contains hierarchy {}", fieldHierarchy);
									if (ParseJsonResponse.containsHierarchy(newResponseStr, fieldType, fieldHierarchy)) {
										String levels[] = fieldHierarchy.split(delimiterForLevel);

										/*logger.info("Checking if Last Node is populated for hierarchy {}", fieldHierarchy);
										if (!ParseJsonResponse.isLastNodePopulated(newResponseStr, fieldType, field.get("multiple"), fieldHierarchy)) {*/
										if (field.get("model") != null && field.get("model").trim().equalsIgnoreCase("stakeHolders.values")) {
											//do nothing
											continue;
										}

										if (field.get("name").equalsIgnoreCase("clauseText")) {

											JSONObject clauseText = finalJsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("clauseText");
											clauseText.remove("values");
											JSONObject values = new JSONObject();
											String text;
											if (excelFields.get("Clause Text") != null) {
												text = excelFields.get("Clause Text").trim();
												values.put("text", text);

												if (excelFields.get("Clause htmlText") == null || excelFields.get("Clause htmlText").equals("")) {
													String htmlText = "<style>.table-bordered, .table-bordered>tbody>tr>td, .table-bordered>tbody>tr>th, .table-bordered>tfoot>tr>td, .table-bordered>tfoot>tr>th, .table-bordered>thead>tr>td, .table-bordered>thead>tr>th{\\t border: 1px solid #ddd;      border-collapse: collapse;}.table {   width: 100%;    max-width: 100%;    margin-bottom: 20px;}</style><p>" + text + "<br></p>";
													values.put("htmlText", htmlText);
												} else {
													values.put("htmlText", excelFields.get("Clause htmlText").trim());
												}
											} else {
												String defaultText = "";
												String defaultHtmlText = "<style>.table-bordered, .table-bordered>tbody>tr>td, .table-bordered>tbody>tr>th, .table-bordered>tfoot>tr>td, .table-bordered>tfoot>tr>th, .table-bordered>thead>tr>td, .table-bordered>thead>tr>th{\\t border: 1px solid #ddd;      border-collapse: collapse;}.table {   width: 100%;    max-width: 100%;    margin-bottom: 20px;}</style><p><br></p>";
												values.put("text", defaultText);
												values.put("htmlText", defaultHtmlText);
											}
											clauseText.put("values", values);

											finalJsonObj.getJSONObject("body").getJSONObject("data").put("clauseText", clauseText);
											continue;
										}

										if (field.get("displayMode").trim().equalsIgnoreCase("editable")) {
											JSONObject tempJsonObj;

											if (!excelFields.get(excelFieldsKeys.get(l)).trim().equalsIgnoreCase(""))
												tempJsonObj = PayloadUtils.buildJsonObjectForCreate(new JSONObject(finalJsonObj.getJSONObject("body").toString()), fieldHierarchy,
														field, levels[levels.length - 2], excelFields.get(excelFieldsKeys.get(l)));
											else
												tempJsonObj = PayloadUtils.buildJsonObjectForCreate(new JSONObject(finalJsonObj.getJSONObject("body").toString()), fieldHierarchy,
														field, levels[levels.length - 2], allRequiredFields.get(field.get("name").trim()));
											partialJsonObj = new JSONObject();
											partialJsonObj.put("body", tempJsonObj);
											finalJsonObj = partialJsonObj;
										}
//										}
									} else
										logger.error("Hierarchy {} not found. Couldn't create payload for Create Api", fieldHierarchy);
								} else {
									logger.error("Couldn't find Field Type for field {}.", field.get("name"));
								}
							} catch (Exception e) {
								logger.error("Exception while Creating Payload for Field {}. {}", field.get("label"), e.getStackTrace());
							}
						}
					}
				}
			}

			logger.info("Building Json for Extra Fields");
			for (int j = 0; j < extraFields.size(); j++) {
				JSONObject tempJsonObj = null;
				List<String> allExtraFieldsKeys = new ArrayList<>(extraFields.keySet());

				if (extraFields.get(allExtraFieldsKeys.get(j)) != null) {
					String extraFieldSplit[] = extraFields.get(allExtraFieldsKeys.get(j)).split(PayloadUtils.entityCreationExtraFieldsDelimiterForValues);
					String extraFieldHierarchy = "body" + delimiterForLevel + "data" + delimiterForLevel + allExtraFieldsKeys.get(j);
					String fieldValue;
					String endNode;

					if (extraFieldSplit.length > 1) {
						extraFieldHierarchy += delimiterForLevel + extraFieldSplit[0].trim();
						fieldValue = extraFieldSplit[1].trim();
						endNode = allExtraFieldsKeys.get(j);
					} else {
						fieldValue = extraFieldSplit[0].trim();
						endNode = "data";
					}
					tempJsonObj = PayloadUtils.buildJsonObjectForCreate(new JSONObject(finalJsonObj.getJSONObject("body").toString()), extraFieldHierarchy, null,
							endNode, fieldValue);
				}
				partialJsonObj = new JSONObject();
				partialJsonObj.put("body", tempJsonObj);
				finalJsonObj = partialJsonObj;
			}

			JSONArray allFields = finalJsonObj.getJSONObject("body").getJSONObject("data").names();
			logger.info("Removing Options from Json");
			for (int k = 0; k < allFields.length(); k++) {
				String fieldName = allFields.get(k).toString();

				if (finalJsonObj.getJSONObject("body").getJSONObject("data").getJSONObject(fieldName).has("options"))
					finalJsonObj.getJSONObject("body").getJSONObject("data").getJSONObject(fieldName).remove("options");
			}
		} catch (Exception e) {
			logger.error("Exception while creating payload for Create Api. {}", e.getMessage());
		}
		return finalJsonObj.toString();
	}

	private static JSONObject buildJsonObjectForCreate(JSONObject initialJsonObj, String hierarchy, Map<String, String> field, String endNode, String fieldValue) {
		try {
			String delimiterForLevel = ConfigureConstantFields.getConstantFieldsProperty("JsonObjectsOrderDelimiter");
			String levels[] = hierarchy.split(delimiterForLevel);
			JSONObject childJsonObj;

			if (levels[0].equalsIgnoreCase(endNode)) {

				if (field == null) {
					if (fieldValue.startsWith("["))
						initialJsonObj.put(levels[levels.length - 1], new JSONArray(fieldValue));
					else
						initialJsonObj.put(levels[levels.length - 1], new JSONObject(fieldValue));
				} else {
					//For Stakeholder
					if (field.get("model") != null && field.get("model").trim().equalsIgnoreCase("stakeHolders.values")) {
						initialJsonObj.put(levels[levels.length - 1], new JSONArray("[" + fieldValue + "], \"name\":\"" + field.get("name") +
								"\",\"label\":\"" + field.get("label") + "\""));
					} else {
						String fieldType = field.get("type");
						switch (fieldType) {
							case "checkbox":
								initialJsonObj.put(levels[levels.length - 1], Boolean.parseBoolean(fieldValue));
								break;

							case "select":
								if (field.get("multiple").equalsIgnoreCase("false"))
									initialJsonObj.put(levels[levels.length - 1], new JSONObject(fieldValue));
								else
									initialJsonObj.put(levels[levels.length - 1], new JSONArray("[" + fieldValue + "]"));
								break;

							case "hreftext":
								initialJsonObj.put(levels[levels.length - 1], new JSONObject(fieldValue));
								break;

							case "date":
							case "dateTime":
								initialJsonObj.put(levels[levels.length - 1], fieldValue + " 00:00:00");
								break;

							default:
								initialJsonObj.put(levels[levels.length - 1], fieldValue);
								break;
						}
					}
				}
			} else {
				String newHierarchy = "";

				for (int i = 1; i < levels.length - 1; i++)
					newHierarchy = newHierarchy.concat(levels[i] + delimiterForLevel);

				newHierarchy += levels[levels.length - 1];
				childJsonObj = initialJsonObj.getJSONObject(levels[1]);
				buildJsonObjectForCreate(childJsonObj, newHierarchy, field, endNode, fieldValue);
			}
		} catch (Exception e) {
			logger.error("Exception while building Json for Field {}. {}", field.get("name"), e.getStackTrace());
		}
		return initialJsonObj;
	}

	public static String getPayloadForDashboardData() {
		return getPayloadForDashboardData(-1);
	}

	public static String getPayloadForDashboardData(int chartId) {
		String payload = null;
		try {
			if (chartId == -1) {
				logger.info("No Chart Id Specified. Proceeding with Dashboard Data Payload for All Charts");
				payload = "{\"baseSubFilter\":null, \"baseFilter\":\"Supplier\" ,\"discipline\":\"\",\"userSpecificView\":\"false\",\"homePageView\":" +
						"\"MODERN\" ,\"staging\":\"false\", \"reset\":\"true\" ,\"suppliers\":[],\"regions\":[],\"functions\":[]}";
			} else
				payload = "{\"baseSubFilter\":null,\"baseFilter\":\"Supplier\",\"discipline\":\"\",\"userSpecificView\":\"false\",\"homePageView\":\"MODERN\"," +
						"\"staging\":\"false\",\"reset\":false,\"charts\" :[" + chartId + "],\"savePreferences\" : false, \"additionalParams\" : {}, " +
						"\"preferenceApplied\" : true}";
		} catch (Exception e) {
			logger.error("Exception while creating Payload for DashBoard Data Api. {}", e.getMessage());
		}
		return payload;
	}

	public static String getPayloadForDashboardDataWithFilter(int chartId, String filterName, String optionId) {
		String payload = null;
		try {
			payload = "{\"baseSubFilter\":null,\"baseFilter\":\"Supplier\",\"discipline\":\"\",\"loadExecFilters\":true,\"userSpecificView\":\"false\",\"homePageView\":\"MODERN\",\"staging\":\"false\",\"reset\":false," +
					"\"" + filterName + "\":[{\"id\":\"" + optionId + "\"}],\"charts\":[" + chartId + "],\"savePreferences\":false,\"additionalParams\":{},\"preferenceApplied\":true}";
		} catch (Exception e) {
			logger.error("Exception while creating Payload for DashBoard Data Api with filters. {}", e.getMessage());
		}
		return payload;
	}

	public static String getPayloadForDashboardAnalysis(int chartId) {
		return getPayloadForDashboardAnalysis(chartId, null, null, null, null, null);
	}

	public static String getPayloadForDashboardAnalysis(int chartId, Map<String, String> filterParams, String dashboardLocalFiltersResponseStr, String currentOutputGroup) {
		return getPayloadForDashboardAnalysis(chartId, filterParams, null, dashboardLocalFiltersResponseStr, null, currentOutputGroup);
	}

	public static String getPayloadForDashboardAnalysis(int chartId, Map<String, String> filterParams, Map<String, String> childParams, String dashboardLocalFiltersResponseStr,
	                                                    String stakeholderObjectName, String currentOutputGroup) {
		String payload = null;
		try {
			if (filterParams == null) {
				payload = "{\"chartId\":" + chartId + ",\"userRequestId\":\"\",\"filterParam\":[],\"userSpecificView\":false,\"drillDown\":false}";

			} else if (currentOutputGroup == null) {
				String filterParamString = getFilterParamString(filterParams, childParams, dashboardLocalFiltersResponseStr, stakeholderObjectName);
				payload = "{\"chartId\":" + chartId + ",\"userRequestId\":\"\",\"filterParam\":" + filterParamString + ",\"userSpecificView\":false,\"newFilterApplied\":true}";

			} else {
				String filterParamString = getFilterParamString(filterParams, childParams, dashboardLocalFiltersResponseStr, stakeholderObjectName);
				payload = "{\"chartId\":" + chartId + ",\"userRequestId\":\"\",\"currentOutputGroup\":\"" + currentOutputGroup + "\",\"filterParam\":" + filterParamString + ",\"userSpecificView\":false,\"newFilterApplied\":true}";
			}
		} catch (Exception e) {
			logger.error("Exception while creating Payload for Dashboard Analysis Api. {}", e.getMessage());
		}
		return payload;
	}

	public static String getPayloadForDashboardRecords(int chartId) {
		return "{\"chartId\":" + chartId + ",\"userRequestId\":\"\"}";
	}

	public static String getPayloadForDashboardRecords(int chartId, Map<String, String> filterParams, String jsonStr, String link, Integer offset, Integer size,
	                                                   String chartFramework, String userRequestId) {
		return getPayloadForDashboardRecords(chartId, filterParams, null, jsonStr, link, offset, size, chartFramework, null, userRequestId);
	}

	public static String getPayloadForDashboardRecords(int chartId, Map<String, String> filterParams, Map<String, String> childParams, String jsonStr, String link,
	                                                   Integer offset, Integer size, String chartFramework, String stakeholderObjectName, String userRequestId) {
		String payload = null;
		try {
			logger.info("Creating payload for dashboard records. chartId ={}", chartId);
			String baseFilterParamString = getFilterParamString(filterParams, childParams, jsonStr, stakeholderObjectName).trim();
			baseFilterParamString = baseFilterParamString.substring(0, baseFilterParamString.length() - 1);

			int index = link.indexOf("(");
			String substring = link.substring(index + 1, link.indexOf(")"));

			if (chartFramework.trim().equalsIgnoreCase("new")) {
				/*
				 * new framwork javaScript sample = JavaScript:  isJavaScriptCall=true; showTableWithParams({\"chartId\":27,\"localFilters\":{\"fusionregion\":\"0\"},
				 * \"additionalFilters\":{},\"drilldown\":true})* */
				String filterParam = PayloadUtils.getDashboardRecordsFilterParamsNew(substring, baseFilterParamString);
				payload = "{\"chartId\":" + chartId + ",\"filterParam\":" + filterParam + ",\"chartTableMetadata\":{\"offset\":" + offset + ",\"size\":" + size + "}}";
			} else if (chartFramework.trim().equalsIgnoreCase("old")) {
				/*
				 * old framwork javaScript sample = JavaScript:  isJavaScriptCall=true; showTableForBarSplit('86','',null,'category','SL Category - 1','status','Approved')
				 * ##showTableForBarSplit(chartId,userRequestId,outputId,localFilters)
				 * */
				String filterParam = PayloadUtils.getDashboardRecordsFilterParamsOld(substring, baseFilterParamString, chartId);
				String linkItems[] = substring.split(",");
				userRequestId = linkItems[1].trim().replaceAll("'", "");
				payload = "{\"chartId\":" + chartId + ",\"userRequestId\":\"" + userRequestId + "\",\"filterParam\":" + filterParam + ",\"chartTableMetadata\":{\"offset\":" +
						offset + ",\"size\":" + size + "}}";
			}
			logger.info("payload formed = {}", payload);
		} catch (Exception e) {
			logger.error("Exception while creating Payload for Dashboard Records Api for Link {}. {}", link, e.getMessage());
		}
		return payload;
	}

	private static String getFilterParamString(Map<String, String> filterParams, Map<String, String> childParams, String dashboardLocalFiltersResponseStr,
	                                           String stakeholderObjectName) {
		String filterParamString = "[{";
		//For Non-Stakeholder Fields
		if (childParams == null) {
			List<String> filterParamsKeys = new ArrayList<>(filterParams.keySet());
			for (int i = 0; i < filterParams.size(); i++) {
				filterParamString = filterParamString.concat("\"key\":\"" + filterParamsKeys.get(i) + "\",\"values\":");
				String attributeType = DashboardLocalFilters.getAttributeType(dashboardLocalFiltersResponseStr, filterParamsKeys.get(i));

				switch (attributeType.toLowerCase()) {
					case "select":
						break;

					case "multiselect":
						filterParamString += "[\"" + filterParams.get(filterParamsKeys.get(i)) + "\"]";
						break;

					case "slider":
						filterParamString += filterParams.get(filterParamsKeys.get(i));
						break;
				}
			}
		} else {
			//For Stakeholder Field
			filterParamString += "\"key\":\"" + stakeholderObjectName + "\",\"values\":[{\"id\":" + filterParams.get("id") + ",\"name\":\"" + filterParams.get("name") +
					"\",\"users\":[{\"id\":" + childParams.get("id") + ",\"name\":\"" + childParams.get("name") + "\"}]}]";
		}
		filterParamString += "}]";

		return filterParamString;
	}

	private static Map<String, String> getLocalFilterMap(String localFilter) {
		Map<String, String> filterMap = new HashMap<>();
		String[] localFilters = localFilter.substring(1, localFilter.length() - 1).split(",");
		for (String filter : localFilters) {
			String[] filters = filter.split(":");
			filterMap.put(filters[0], filters[1]);
		}
		return filterMap;
	}

	private static String getDashboardRecordsFilterParamsNew(String substring, String baseFilterParamString) {
		String filterParam = null;
		try {
			String payloadLocalFilter = "";
			JSONObject javaScript = new JSONObject(substring);
			String localFilters = javaScript.get("localFilters").toString();
			String additionalFilters = javaScript.get("additionalFilters").toString();
			if (localFilters.equals("{}")) {
				filterParam = baseFilterParamString + "]," + "\"additionalParams\":" + additionalFilters;
			} else {
				Map<String, String> filterMap = getLocalFilterMap(localFilters);
				for (Map.Entry<String, String> entry : filterMap.entrySet()) {
					payloadLocalFilter = payloadLocalFilter.concat("{\"key\":" + entry.getKey() + "," + "\"values\":" + entry.getValue() + "},");
				}
				payloadLocalFilter = payloadLocalFilter.substring(0, payloadLocalFilter.length() - 1);
				filterParam = baseFilterParamString + "," + payloadLocalFilter + "]," + "\"additionalParams\":" + additionalFilters;
			}

		} catch (Exception e) {
			logger.error("Exception while Creating Dashboard Records Filter Params for New Chart Framework");
		}
		return filterParam;
	}

	private static String getDashboardRecordsFilterParamsOld(String substring, String baseFilterParamString, Integer chartId) {
		String filterParam = null;
		Integer localFiltersLength;
		try {
			String filters[] = substring.split(",");
			String payloadLocalFilter = "";
			List<String> doughnut2dCharts = Arrays.asList(TestDashboard.doughnut2dCharts);

			if (doughnut2dCharts.contains(chartId.toString())) {
				localFiltersLength = filters.length - 2;
			} else
				localFiltersLength = filters.length;

			for (int i = 3; i < localFiltersLength; i = i + 2) {
				String key = filters[i].replaceAll("'", "");
				String value = filters[i + 1].replaceAll("'", "");
				payloadLocalFilter = payloadLocalFilter.concat("{\"key\":\"" + key + "\", \"values\":[\"" + value + "\"]},");
			}
			payloadLocalFilter = payloadLocalFilter.substring(0, payloadLocalFilter.length() - 1);
			filterParam = baseFilterParamString + "," + payloadLocalFilter + "]";
		} catch (Exception e) {
			logger.error("Exception while Creating Dashboard Records Filter Params for Old Chart Framework");
		}
		return filterParam;
	}

	public static String getFieldIdsForBulkEditPayload(String createJsonStr, String fieldLabelId) {
		String fieldIds = "";
		try {
			logger.info("Getting FieldIds for Bulk Edit Payload");
			if (ParseJsonResponse.validJsonResponse(createJsonStr)) {
				fieldIds += fieldLabelId;

				JSONArray jsonArr = ParseJsonResponse.getFieldJSONArrayAttributeFromLabel(createJsonStr, "COMMENTS AND ATTACHMENTS", "fields");
				if (jsonArr != null) {
					JSONObject jsonObj;
					for (int i = 0; i < jsonArr.length(); i++) {
						jsonObj = jsonArr.getJSONObject(i);
						fieldIds = fieldIds.concat("," + jsonObj.getInt("id"));
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while getting Field Ids for Bulk Edit Payload {}", e.getMessage());
		}
		return fieldIds;
	}

	/*
	removeOptionsFromPayload method removes 'options' json object for each JSONObject in the Payload and returns the payload.
	 */
	public static String removeOptionsFromPayload(String payloadJsonStr) {
		String payload = payloadJsonStr;
		try {
			JSONObject jsonObj = new JSONObject(payload);
			JSONArray allFields = jsonObj.getJSONObject("body").getJSONObject("data").names();

			logger.info("Removing Options from Json");
			for (int k = 0; k < allFields.length(); k++) {
				String fieldName = allFields.get(k).toString();

				if (jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject(fieldName).has("options"))
					jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject(fieldName).remove("options");
			}

			payload = jsonObj.toString();
		} catch (Exception e) {
			logger.error("Exception while removing Options from Payload. {}", e.getMessage());
		}
		return payload;
	}
}