package com.sirionlabs.helper.entityEdit;

import com.sirionlabs.api.auditLogs.FieldHistory;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class EntityEditHelper {

	private final static Logger logger = LoggerFactory.getLogger(EntityEditHelper.class);

	private boolean saveHtmlResponse = true;

	public void validateEntityEdit(String entityName, int recordId, Map<String, String> defaultProperties, Map<String, String> extraFields,
	                               Map<String, String> fieldsToIgnoreMap, int defaultNoOfFieldsToEdit, boolean negativeCase, CustomAssert csAssert) {
		validateEntityEdit(entityName, recordId, defaultProperties, extraFields, fieldsToIgnoreMap, defaultNoOfFieldsToEdit, negativeCase, true, csAssert);
	}

	public void validateEntityEdit(String entityName, int recordId, Map<String, String> defaultProperties, Map<String, String> extraFields,
	                               Map<String, String> fieldsToIgnoreMap, int defaultNoOfFieldsToEdit, boolean negativeCase, boolean restoreRecord, CustomAssert csAssert) {
		try {
			Edit editObj = new Edit();
			int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
			String editGetJsonStr = editObj.hitEdit(entityName, recordId);

			Map<String, String> fieldsPayloadMap;
			Map<String, String> currentValuesMap = new HashMap<>();

			if (ParseJsonResponse.validJsonResponse(editGetJsonStr)) {
				if (ParseJsonResponse.hasPermissionError(editGetJsonStr)) {
					throw new SkipException("Permission Denied to Edit Record having Id " + recordId + " of Entity " + entityName);
				}

				if (extraFields == null || extraFields.isEmpty()) {
					logger.info("Getting All Editable fields for Entity {}, Id {}", entityName, recordId);
					List<String> allEditableFields = ParseJsonResponse.getAllEditableFieldLabels(editGetJsonStr);

					if (allEditableFields.size() > 0) {
						List<String> filteredFields = filterEditableFields(editGetJsonStr, allEditableFields, fieldsToIgnoreMap, entityName);
						fieldsPayloadMap = getFieldsPayload(editGetJsonStr, defaultProperties, filteredFields, defaultNoOfFieldsToEdit);
					} else {
						throw new SkipException("No Editable field found for Entity " + entityName + ", Id " + recordId + ". Hence Skipping Test.");
					}
				} else {
					fieldsPayloadMap = new HashMap<>(extraFields);
				}

				if (fieldsPayloadMap.size() > 0) {
					for (Map.Entry<String, String> fieldMap : fieldsPayloadMap.entrySet()) {
						currentValuesMap.put(fieldMap.getKey(), "null");
					}

					for (Map.Entry<String, String> fieldMap : currentValuesMap.entrySet()) {
						String field = fieldMap.getKey();

						if (entityName.trim().equalsIgnoreCase("governance body") && fieldMap.getKey().equalsIgnoreCase("weekType")) {
							field = "weekTypeId";
						}

						boolean fieldOfMultiSelectType = false;

						Map<String, String> fieldAttributesMap = ParseJsonResponse.getFieldByName(editGetJsonStr, field);

						if (fieldAttributesMap != null && !fieldAttributesMap.isEmpty()) {
							if (fieldAttributesMap.get("multiple").equalsIgnoreCase("true")) {
								fieldOfMultiSelectType = true;
							}
						}

						String value = null;

						if (!fieldOfMultiSelectType) {
							value = ShowHelper.getValueOfField(entityTypeId, recordId, field, false, editGetJsonStr);
						} else {
							List<String> allValues = ShowHelper.getActualValues(editGetJsonStr, ShowHelper.getShowFieldHierarchy(field, entityTypeId));

							if (allValues != null) {
								value = allValues.toString().replace("[", "").replace("]", "");
							}
						}

						if (value != null && !value.equalsIgnoreCase("")) {
							currentValuesMap.put(fieldMap.getKey(), value);
						}
					}

					String editPostPayload = createPayloadForEditPost(editGetJsonStr, fieldsPayloadMap);

					if (editPostPayload != null) {
						logger.info("Hitting Edit Post API for Entity {}, Id {}", entityName, recordId);
						String editPostJsonStr = editObj.hitEdit(entityName, editPostPayload);

						if (ParseJsonResponse.validJsonResponse(editPostJsonStr)) {
							JSONObject jsonObj = new JSONObject(editPostJsonStr);
							String editStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status");

							if (!editStatus.trim().equalsIgnoreCase("success")) {
								logger.info("Couldn't Edit Record Id {} of Entity {} due to {}", recordId, entityName, editStatus);

								if (!negativeCase) {
									csAssert.assertTrue(false, "Edit of Entity " + entityName + ", Id " + recordId + " failed due to " + editStatus);
								}
							} else {
								logger.info("Entity {} having Id {} edited Successfully.", entityName, recordId);

								if (negativeCase) {
									csAssert.assertTrue(false, "Record Edited successfully having Id " + recordId + " of Entity " + entityName +
											" but it wasn't supposed to Edit");
								}

								logger.info("Validating Audit Logs for Entity {} having Id {}", entityName, recordId);

//								verifyAuditLogsAfterEditingRecord(editGetJsonStr, entityName, recordId, currentValuesMap, fieldsPayloadMap, csAssert);

								if (restoreRecord) {
									Boolean recordRestored = EntityOperationsHelper.restoreRecord(entityName, recordId, editGetJsonStr);

									if (recordRestored) {
										logger.info("Record of Entity {} having Id {} restored Successfully.", entityName, recordId);
									} else {
										logger.info("Record of Entity {} having Id {} couldn't be restored.", entityName, recordId);
										csAssert.assertTrue(false,"Record of Entity " + entityName  + "having Id " + recordId  +"couldn't be restored ");

									}
								}
							}
						} else {
							csAssert.assertTrue(false, "Edit Post API Response is an Invalid JSON for Entity " + entityName + " and Id " + recordId + ".");
						}
					} else {
						csAssert.assertTrue(false, "Couldn't create Edit Post Payload for Entity " + entityName + " and Id " + recordId + "");
					}
				} else {
					throw new SkipException("Couldn't get Fields Payload. Hence Skipping Test.");
				}
			} else {
				csAssert.assertTrue(false, "Edit Get API Response for Entity " + entityName + ", Id " + recordId + " is an Invalid JSON.");

				if (saveHtmlResponse) {
					FileUtils.saveResponseInFile("EditGetHtmlResponse " + entityName + " " + recordId + ".txt", editGetJsonStr);
					saveHtmlResponse = false;
				}
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Entity Edit for Entity " + entityName + " and Id " + recordId + ". " + e.getMessage());
		}
	}

	private Map<String, String> getFieldsPayload(String editGetJsonStr, Map<String, String> defaultProperties, List<String> filteredFields, int defaultNoOfFieldsToEdit) {
		Map<String, String> fieldsPayloadMap = new HashMap<>();

		try {
			if (fieldsPayloadMap.size() < defaultNoOfFieldsToEdit) {
				int noOfOtherFieldsRequired = defaultNoOfFieldsToEdit - fieldsPayloadMap.size();
				List<String> fieldsToEdit = getFieldsToEdit(filteredFields, noOfOtherFieldsRequired);

				if (fieldsToEdit.size() > 0) {
					for (String fieldLabel : fieldsToEdit) {
						String fieldName = ParseJsonResponse.getFieldAttributeFromLabel(editGetJsonStr, fieldLabel, "name");
						String payload = getPayloadForField(editGetJsonStr, defaultProperties, fieldLabel);
						if (payload != null)
							fieldsPayloadMap.put(fieldName, payload);
					}

					logger.info("Following fields (names) are to be Edited");
					List<String> editFieldNames = new ArrayList<>(fieldsPayloadMap.keySet());
					for (String name : editFieldNames) {
						logger.info(name.trim());
					}
				} else {
					logger.error("Couldn't get Random Fields to Edit");
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Creating Payload for Fields. {}", e.getMessage());
		}
		return fieldsPayloadMap;
	}


	private List<String> getFieldsToEdit(List<String> filteredFields, int noOfFieldsRequired) {
		List<String> randomFields = new ArrayList<>();

		try {
			if (filteredFields.size() > 0) {
				logger.info("Getting {} Random fields to Edit", noOfFieldsRequired);
				int[] randomNumbers = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, filteredFields.size() - 1, noOfFieldsRequired);

				for (int randomNumber : randomNumbers) {
					randomFields.add(filteredFields.get(randomNumber));
				}
			}
		} catch (Exception e) {
			logger.error("Exception while getting Fields to Edit. {}", e.getMessage());
		}

		return randomFields;
	}

	private String getPayloadForField(String editGetJsonStr, Map<String, String> defaultProperties, String fieldLabel) {
		String payload = null;
		try {
			Map<String, String> field = ParseJsonResponse.getFieldByLabel(editGetJsonStr, fieldLabel);

			if (field.size() > 0) {
				String fieldType = field.get("type").trim().toLowerCase();
				String fieldHierarchy = ParseJsonResponse.getFieldHierarchy(field);

				logger.info("Checking if Response contains hierarchy {}", fieldHierarchy);
				if (ParseJsonResponse.containsHierarchy(editGetJsonStr, fieldType, fieldHierarchy)) {
					String originalPayload = getOriginalPayloadOfField(editGetJsonStr, field.get("name").trim(), fieldHierarchy);
					String value = null;

					switch (fieldType) {
						case "text":
						case "textarea":
							value = getDefaultValueForField(defaultProperties, fieldLabel, fieldType);
							break;

						case "select":
							List<String> allOptions = ParseJsonResponse.getAllOptionsForFieldAsJsonString(editGetJsonStr, field.get("name"),
									Boolean.parseBoolean(field.get("dynamicField").trim()));
							if (allOptions.size() > 0) {
								int randomOptionNumber = RandomNumbers.getRandomNumberWithinRangeIndex(0, allOptions.size() - 1);
								value = allOptions.get(randomOptionNumber);
							} else {
								return null;
							}
							break;
					}

					payload = updateFieldJSON(originalPayload, fieldHierarchy, value, field);
				} else
					logger.error("Hierarchy {} not found. Couldn't create payload for Create Api", fieldHierarchy);
			}
		} catch (Exception e) {
			logger.error("Exception while creating Payload for Field {}. {}", fieldLabel, e.getStackTrace());
		}
		return payload;
	}

	private String getOriginalPayloadOfField(String editGetJsonStr, String fieldName, String fieldHierarchy) {
		String originalPayload = null;

		try {
			JSONObject jsonObj = new JSONObject(editGetJsonStr);
			String[] levels = fieldHierarchy.trim().split(Pattern.quote("->"));

			int i;
			for (i = 0; i < levels.length - 2; i++) {
				jsonObj = jsonObj.getJSONObject(levels[i].trim());
			}

			originalPayload = jsonObj.getJSONObject(levels[i]).toString();
		} catch (Exception e) {
			logger.error("Exception while Getting Original Payload of Field {} using Hierarchy [{}] .{}", fieldName, fieldHierarchy, e.getStackTrace());
		}
		return originalPayload;
	}

	private String getDefaultValueForField(Map<String, String> defaultProperties, String fieldLabel, String fieldType) {
		String value = null;
		logger.info("Getting default Value for Field {}, Type {}", fieldLabel, fieldType);

		try {
			String basePrefix = defaultProperties.getOrDefault("baseprefix", "Automation ");

			String currTime = Long.toString(System.currentTimeMillis());
			currTime = currTime.substring(currTime.length() - 7);
			String defaultValue;

			switch (fieldType) {
				case "text":
				case "textarea":
					defaultValue = defaultProperties.getOrDefault("defaulttext", "API ");
					value = basePrefix + defaultValue + currTime;
					break;

				case "date":
					value = defaultProperties.getOrDefault("defaultdate", "03-20-2018");
			}
		} catch (Exception e) {
			logger.error("Exception while getting Default Value for Field {}, Type{}. {}", fieldLabel, fieldType, e.getStackTrace());
		}
		return value;
	}

	private String updateFieldJSON(String originalPayload, String fieldHierarchy, String newValue, Map<String, String> field) {
		String updatedJSON = null;

		try {
			JSONObject jsonObj = new JSONObject(originalPayload);
			String[] levels = fieldHierarchy.trim().split(Pattern.quote("->"));

			String fieldType = field.get("type");
			switch (fieldType) {
				case "text":
				case "textarea":
					jsonObj = jsonObj.put(levels[levels.length - 1].trim(), newValue.trim());
					break;

				case "select":
					if (field.get("multiple").trim().equalsIgnoreCase("false"))
						jsonObj = jsonObj.put(levels[levels.length - 1].trim(), new JSONObject(newValue.trim()));
					else
						jsonObj = jsonObj.put(levels[levels.length - 1].trim(), new JSONArray("[" + newValue.trim() + "]"));
					break;
			}

			if (jsonObj.has("options"))
				jsonObj.remove("options");

			updatedJSON = jsonObj.toString();
		} catch (Exception e) {
			logger.error("Exception while Updating Text Field JSON for Field {}. {}", field.get("label"), e.getStackTrace());
		}
		return updatedJSON;
	}

	private String createPayloadForEditPost(String editGetJsonStr, Map<String, String> fieldsPayloadMap) {
		String payload;

		try {
			payload = "{\"body\":{\"data\":";
			JSONObject jsonObj = new JSONObject(editGetJsonStr);
			jsonObj = jsonObj.getJSONObject("body").getJSONObject("data");
			List<String> fieldNames = new ArrayList<>(fieldsPayloadMap.keySet());

			for (String field : fieldNames) {
				jsonObj = jsonObj.put(field.trim(), new JSONObject(fieldsPayloadMap.get(field).trim()));
			}

			payload += jsonObj.toString();
			payload += "}}";
		} catch (Exception e) {
			logger.error("Exception while Creating Edit Post Payload. {}", e.getMessage());
			return null;
		}
		return payload;
	}

	private void verifyAuditLogsAfterEditingRecord(String editGetResponse, String entityName, int recordId, Map<String, String> allPreviousValuesMap,
	                                               Map<String, String> allNewValuesMap, CustomAssert csAssert) {
		try {
			int auditLogTabId = TabListDataHelper.getIdForTab("audit log");
			int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName.trim());

			logger.info("Hitting TabListData API for Record Id {}, Entity {} and Audit Log Tab.", recordId, entityName);
			ListRendererTabListData tabListObj = new ListRendererTabListData();
			String payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
			tabListObj.hitListRendererTabListData(auditLogTabId, entityTypeId, recordId, payload);
			String tabListDataResponse = tabListObj.getTabListDataJsonStr();

			if (ParseJsonResponse.validJsonResponse(tabListDataResponse)) {
				List<Map<Integer, Map<String, String>>> listData = ListDataHelper.getListData(tabListDataResponse);

				if (listData.size() > 0) {
					int historyColumnNo = ListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "history");
					String historyValue = listData.get(0).get(historyColumnNo).get("value");

					if (historyValue != null) {
						Long historyId = TabListDataHelper.getHistoryIdFromValue(historyValue);

						logger.info("Hitting Field History API for Record Id {} and EntityTypeId {}", recordId, entityTypeId);
						FieldHistory historyObj = new FieldHistory();
						String fieldHistoryResponse = historyObj.hitFieldHistory(historyId, entityTypeId);

						if (ParseJsonResponse.validJsonResponse(fieldHistoryResponse)) {
							JSONObject jsonObj = new JSONObject(fieldHistoryResponse);
							JSONArray jsonArr = jsonObj.getJSONArray("value");

							for (Map.Entry<String, String> editedFieldMap : allNewValuesMap.entrySet()) {
								String editedFieldName = editedFieldMap.getKey();
								String editedFieldPayloadValue = editedFieldMap.getValue();
								String editedFieldPreviousValue = allPreviousValuesMap.get(editedFieldName);
								String expectedNewValue = getExpectedValueFromPayloadValue(editedFieldPayloadValue);

								if (entityName.equalsIgnoreCase("governance body") && editedFieldName.equalsIgnoreCase("weekType")) {
									expectedNewValue = ShowHelper.getValueOfField(entityTypeId, recordId, "weekTypeId");
								}

								if (expectedNewValue.equalsIgnoreCase("Not Supported")) {
									throw new SkipException("Currently Do Not Support Audit Log Validation for such field. Hence skipping test case");
								}

								if (editedFieldPreviousValue.equalsIgnoreCase(expectedNewValue)) {
									throw new SkipException("Old and New Value is same. Hence skipping case validation.");
								}

								//Special condition for Boolean type values.
								if (expectedNewValue.equalsIgnoreCase("true")) {
									expectedNewValue = "yes";
								}

								if (expectedNewValue.equalsIgnoreCase("false")) {
									expectedNewValue = "no";
								}

								//Special Condition for Multi Select Fields.
								if (expectedNewValue.contains(",") && expectedNewValue.contains(editedFieldPreviousValue)) {
									expectedNewValue = expectedNewValue.replace(editedFieldPreviousValue, "").trim();
									editedFieldPreviousValue = "null";

									if (expectedNewValue.startsWith(",")) {
										expectedNewValue = expectedNewValue.substring(1).trim();
									} else if (expectedNewValue.endsWith(",")) {
										expectedNewValue = expectedNewValue.substring(0, expectedNewValue.length() - 1);
									}
								}

								boolean fieldFoundInHistory = false;
								Map<String, String> field = ParseJsonResponse.getFieldByName(editGetResponse, editedFieldName);

								if (field.size() > 0) {
									String fieldLabel = field.get("label");

									for (int i = 0; i < jsonArr.length(); i++) {
										jsonObj = jsonArr.getJSONObject(i);

										if (jsonObj.getString("property").trim().equalsIgnoreCase(fieldLabel.trim())) {
											fieldFoundInHistory = true;
											String historyState = jsonObj.getString("state");
											String actualOldValue = jsonObj.isNull("oldValue") ? "null" : jsonObj.getString("oldValue");
											actualOldValue = actualOldValue.equalsIgnoreCase("") ? "null" : actualOldValue;
											String actualNewValue = jsonObj.isNull("newValue") ? "null" : jsonObj.getString("newValue");
											actualNewValue = actualNewValue.equalsIgnoreCase("") ? "null" : actualNewValue;

											//Verify State Value
											logger.info("Verifying State Value in Audit Logs");
											if (editedFieldPreviousValue.equalsIgnoreCase("null") || editedFieldPreviousValue.equalsIgnoreCase("")) {
												if (!historyState.trim().equalsIgnoreCase("ADDED")) {
													csAssert.assertTrue(false, "Expected History State: ADDED and Actual History State: " +
															historyState + " for History Id " + historyId + " and EntityTypeId " + entityTypeId);
												}
											} else {
												if (!editedFieldPreviousValue.equalsIgnoreCase("") && expectedNewValue.equalsIgnoreCase("null")) {
													if (!historyState.trim().equalsIgnoreCase("REMOVED")) {
														csAssert.assertTrue(false, "Expected History State: REMOVED and Actual History State: " +
																historyState + " for History Id " + historyId + " and EntityTypeId " + entityTypeId);
													}
												} else if (!historyState.trim().equalsIgnoreCase("MODIFIED")) {
													csAssert.assertTrue(false, "Expected History State: MODIFIED and Actual History State: " +
															historyState + " for History Id " + historyId + " and EntityTypeId " + entityTypeId);
												}
											}

											//Verify Old Value
											logger.info("Verifying Old Value in Audit Logs");
											String expectedOldValue = editedFieldPreviousValue;

											//Special condition for Boolean type values.
											if (expectedOldValue.equalsIgnoreCase("true")) {
												expectedOldValue = "yes";
											}

											if (expectedOldValue.equalsIgnoreCase("false")) {
												expectedOldValue = "no";
											}

											if (NumberUtils.isParsable(expectedOldValue)) {
												Double expectedOldDoubleValue = Double.parseDouble(expectedOldValue);
												Double actualOldDoubleValue = Double.parseDouble(actualOldValue);

												if (!expectedOldDoubleValue.equals(actualOldDoubleValue)) {
													csAssert.assertTrue(false, "Expected Old Value: " + expectedOldValue + " and Actual Old Value: " +
															actualOldValue);
												}
											} else {
												if (!expectedOldValue.equalsIgnoreCase(actualOldValue)) {
													csAssert.assertTrue(false, "Expected Old Value: " + expectedOldValue + " and Actual Old Value: " +
															actualOldValue);
												}
											}

											//Verify New Value
											logger.info("Verifying New Value in Audit Logs");
											if (!expectedNewValue.equalsIgnoreCase(actualNewValue)) {

												if (!(expectedNewValue.equalsIgnoreCase("")))
													csAssert.assertTrue(false, "Expected New Value: " + expectedNewValue + " and Actual New Value: " +
															actualNewValue);
											}
											break;
										}
									}

									if (!fieldFoundInHistory) {
										csAssert.assertTrue(false, "Field having Label " + fieldLabel + " and Name " + editedFieldName +
												" not found in Field History Response for History Id " + historyId + " and EntityTypeId " + entityTypeId);
									}
								} else {
									csAssert.assertTrue(false, "Couldn't get Attributes by Name for Field " + editedFieldName);
								}
							}
						} else {
							csAssert.assertTrue(false, "Field History API Response for History Id " + historyId + ", EntityTypeId " + entityTypeId +
									" is an Invalid JSON.");
						}
					} else {
						csAssert.assertTrue(false, "History Value is Null for Record Id " + recordId + ", Entity " + entityName +
								" in Audit Log Tab API Response");
					}
				} else {
					csAssert.assertTrue(false, "Couldn't get List Data in TabListData API Response for Record Id " + recordId +
							", Entity " + entityName);
				}
			} else {
				csAssert.assertTrue(false, "Audit Log Tab API Response for Record Id " + recordId + " ,EntityTypeId " + entityTypeId +
						" is an Invalid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			logger.error("Exception while Validating Audit Logs after Editing Record having Id {}, Entity {}. {}", recordId, entityName, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while Validating Audit Logs after Editing Record having Id " + recordId + ", Entity " +
					entityName + ". " + e.getMessage());
		}
	}

	private String getExpectedValueFromPayloadValue(String payloadValue) {
		try {
			JSONObject jsonObj = new JSONObject(payloadValue);
			if (jsonObj.has("values")) {
				String valuesStr = jsonObj.get("values").toString();

				if (valuesStr.startsWith("{")) {
					JSONObject valueObj = jsonObj.getJSONObject("values");

					if (valueObj.has("name")) {
						return valueObj.getString("name");
					}
				} else if (valuesStr.startsWith("[")) {
					JSONObject valuesJsonObj = new JSONObject(payloadValue);
					JSONArray valuesJsonArr = valuesJsonObj.getJSONArray("values");
					String expectedValue = "";

					for (int i = 0; i < valuesJsonArr.length(); i++) {
						String name = valuesJsonArr.getJSONObject(i).getString("name");
						expectedValue = expectedValue.concat(name + ", ");
					}

					expectedValue = expectedValue.substring(0, expectedValue.length() - 2);
					return expectedValue;
				} else {
					return valuesStr;
				}
			}

		} catch (Exception e) {
			logger.error("Exception while Getting Expected Value from Payload Value {}. {}", payloadValue, e.getStackTrace());
		}

		return "null";
	}

	private List<String> filterEditableFields(String editGetJsonStr, List<String> allEditableFields, Map<String, String> fieldsToIgnoreMap, String entityName) {
		List<String> filteredFields = new ArrayList<>();
		logger.info("Filtering Unsupported Fields");

		List<String> allFieldsToIgnore = getFieldsToBeIgnored(fieldsToIgnoreMap, entityName);

		for (String editableField : allEditableFields) {
			try {
				logger.info("Getting Attributes of Field {}.", editableField);
				Map<String, String> field = ParseJsonResponse.getFieldByLabel(editGetJsonStr, editableField.trim());

				//Remove Fields to be Ignored
				if (allFieldsToIgnore.contains(field.get("name").trim()))
					continue;

				if (field.size() > 0) {
					if (field.get("type") != null) {
						String fieldType = field.get("type").trim();

						//Filter out Fields of Types Checkbox, AutoComplete, Date, StakeHolder, Comment
						//Filter out other Fields like Alias, DynamicFields, Email.
						//Filter out fields which are either Child or Parent.
						if (fieldType.equalsIgnoreCase("checkbox") || fieldType.equalsIgnoreCase("date")
								|| fieldType.equalsIgnoreCase("dateTime")
								|| fieldType.equalsIgnoreCase("titledBox")
								|| fieldType.equalsIgnoreCase("number")
								|| (field.get("model") != null && field.get("model").trim().equalsIgnoreCase("stakeHolders.values"))
								|| (field.get("model") != null && field.get("model").trim().equalsIgnoreCase("comment"))
								|| (field.get("model") != null && field.get("model").trim().equalsIgnoreCase("currency"))
								|| field.get("name").trim().equalsIgnoreCase("alias")
								|| field.get("name").trim().equalsIgnoreCase("email")
								|| field.get("dependentField") != null
								|| field.get("groupBy") != null
								|| field.get("dynamicField").trim().equalsIgnoreCase("true")) {
							logger.info("Field of Type {} is not supported yet. Hence not considering field {} for Edit.", field.get("type"), editableField);
						} else {
							filteredFields.add(editableField);
						}
					} else {
						logger.info("Couldn't get Type of Field {}. Hence skipping it.", editableField);
					}
				} else {
					logger.info("Couldn't get Attributes for Field {}. Hence skipping it.", editableField);
				}
			} catch (Exception e) {
				logger.error("Exception while Filtering Editable Field {}. {}", editableField, e.getStackTrace());
			}
		}
		return filteredFields;
	}

	private List<String> getFieldsToBeIgnored(Map<String, String> fieldsToIgnoreMap, String entityName) {
		List<String> fieldsToIgnore = new ArrayList<>();

		try {
			logger.info("Getting All fields to be Ignored");
			if (fieldsToIgnoreMap.containsKey(entityName.trim())) {
				String ignoreFieldsStr = fieldsToIgnoreMap.get(entityName.trim());

				if (ignoreFieldsStr != null && !ignoreFieldsStr.trim().equalsIgnoreCase("")) {
					String[] fields = ignoreFieldsStr.split(Pattern.quote(","));

					for (String field : fields) {
						fieldsToIgnore.add(field.trim());
					}
				}
			} else {
				logger.info("No Mapping found for Entity {}.", entityName);
			}
		} catch (Exception e) {
			logger.error("Exception while getting Fields to be Ignored. {}", e.getMessage());
		}
		return fieldsToIgnore;
	}

	public List<Map<String, String>> getAllSupplierTypeUsersInEditPage(String editResponse) {
		List<Map<String, String>> allSupplierTypeUsers = new ArrayList<>();

		try {
			if (ParseJsonResponse.validJsonResponse(editResponse)) {
				JSONObject jsonObj = new JSONObject(editResponse);
				jsonObj = jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("options");
				JSONArray jsonArr = jsonObj.getJSONArray("data");

				for (int i = 0; i < jsonArr.length(); i++) {
					int idType = jsonArr.getJSONObject(i).getInt("idType");

					if (idType == 4) {
						Map<String, String> userMap = new HashMap<>();

						String userName = jsonArr.getJSONObject(i).getString("name");
						int userId = jsonArr.getJSONObject(i).getInt("id");

						userMap.put("name", userName);
						userMap.put("id", String.valueOf(userId));

						allSupplierTypeUsers.add(userMap);
					}
				}

				return allSupplierTypeUsers;
			} else {
				logger.error("Edit Response is an Invalid JSON.");
			}
		} catch (Exception e) {
			logger.error("Exception while Getting All Supplier Type Users from Edit Response. {}", e.getMessage());
		}

		return null;
	}

	public boolean updateField(String entity,int entityId,String fieldName,String updatedValue,CustomAssert customAssert){

		Boolean updateStatus = true;

		Edit edit = new Edit();
		String editResponse;

		try{

			editResponse = edit.hitEdit(entity,entityId);
			JSONObject editResponseJson = new JSONObject(editResponse);

			editResponseJson.remove("header");
			editResponseJson.remove("session");
			editResponseJson.remove("actions");
			editResponseJson.remove("createLinks");

			editResponseJson.getJSONObject("body").remove("layoutInfo");
			editResponseJson.getJSONObject("body").remove("globalData");
			editResponseJson.getJSONObject("body").remove("errors");

			editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject(fieldName).put("values",updatedValue);
			editResponse = edit.hitEdit(entity,editResponseJson.toString());

			if(!editResponse.contains("success")) {
				customAssert.assertTrue(false,"Entity Edit done unsuccessfully");
			}

		}catch (Exception e){
			logger.error("Exception while updating " + fieldName + e.getStackTrace());
			customAssert.assertTrue(false,"Exception while updating " + fieldName + e.getStackTrace());
		}

		return updateStatus;
	}

	public static void updateDynamicField(String entity,int entityId,String fieldName,Object updatedValue,CustomAssert customAssert){

		Edit edit = new Edit();
		String editResponse;

		try{

			editResponse = edit.hitEdit(entity,entityId);
			JSONObject editResponseJson = new JSONObject(editResponse);

			editResponseJson.remove("header");
			editResponseJson.remove("session");
			editResponseJson.remove("actions");
			editResponseJson.remove("createLinks");

			editResponseJson.getJSONObject("body").remove("layoutInfo");
			editResponseJson.getJSONObject("body").remove("globalData");
			editResponseJson.getJSONObject("body").remove("errors");

			editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject(fieldName).put("values",updatedValue);
			editResponse = edit.hitEdit(entity,editResponseJson.toString());

			if(!editResponse.contains("success")) {
				customAssert.assertTrue(false,"Update Dynamic field done unsuccessfully for entity " + entity + " entity id " + entityId);
			}

		}catch (Exception e){
			logger.error("Exception while updating " + fieldName + " for entity " + entity + " entity id " + entityId);

			customAssert.assertTrue(false,"Exception while updating " + fieldName + e.getStackTrace());
		}
	}

	public boolean updateFieldDropDown(String entity,int entityId,String fieldName,int fieldId,CustomAssert customAssert){

		Boolean updateStatus = true;

		Edit edit = new Edit();
		String editResponse;

		try{

			editResponse = edit.hitEdit(entity,entityId);
			JSONObject editResponseJson = new JSONObject(editResponse);

			editResponseJson.remove("header");
			editResponseJson.remove("session");
			editResponseJson.remove("actions");
			editResponseJson.remove("createLinks");

			editResponseJson.getJSONObject("body").remove("layoutInfo");
			editResponseJson.getJSONObject("body").remove("globalData");
			editResponseJson.getJSONObject("body").remove("errors");

			editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject(fieldName).getJSONObject("values").put("id",fieldId);
			editResponse = edit.hitEdit(entity,editResponseJson.toString());

			if(!editResponse.contains("success")) {
				customAssert.assertTrue(false,"Consumptions updated unsuccessfully");
			}

		}catch (Exception e){
			logger.error("Exception while updating " + fieldName + e.getStackTrace());
			customAssert.assertTrue(false,"Exception while updating " + fieldName + e.getStackTrace());
		}

		return updateStatus;
	}

}