package com.sirionlabs.test;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.commonAPI.UpdateAccount;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.regex.Pattern;

public class TestMSATagging {

	private final static Logger logger = LoggerFactory.getLogger(TestMSATagging.class);
	private static String configFilePath = null;
	private static String configFileName = null;
	private static Boolean userSettingsUpdated = false;
	private static Map<String, String> paramsMap;
	private static Integer listSize = 20;
	private static Integer listOffset = 0;
	private static Boolean applyRandomization = false;
	private static Integer maxNoOfRecordsToValidate = 10;

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("MSATaggingTestConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("MSATaggingTestConfigFileName");

		logger.info("Getting Default User Settings");
		Map<String, String> defaultUserSettingsMap = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileName, "default properties");

		paramsMap = new LinkedHashMap<>();
		for (Map.Entry<String, String> defaultSetting : defaultUserSettingsMap.entrySet()) {
			paramsMap.put(defaultSetting.getKey(), defaultSetting.getValue().trim());
		}

		paramsMap.put("language.id", paramsMap.get("language"));
		paramsMap.put("timeZone.id", paramsMap.get("timeZone"));
		paramsMap.remove("language");
		paramsMap.remove("timeZone");
		paramsMap.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("x-csrf-token"));

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "listSize");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			listSize = Integer.parseInt(temp.trim());

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "listOffset");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			listOffset = Integer.parseInt(temp.trim());

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "applyRandomization");
		if (temp != null && temp.trim().equalsIgnoreCase("true"))
			applyRandomization = true;

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "maxNoOfRecordsToValidate");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			maxNoOfRecordsToValidate = Integer.parseInt(temp.trim());
	}

	@AfterClass
	public void afterClass() {
		UpdateAccount updateObj = new UpdateAccount();

		Integer updateStatusCode = updateObj.hitUpdateAccount(paramsMap);

		if (updateStatusCode == 302) {
			logger.info("User Settings restored to Original State.");
		} else {
			logger.error("Couldn't restore User Settings to Original State.");
		}
	}

	@DataProvider
	public Object[][] dataProviderForMSATagging() {
		List<Object[]> allTestData = new ArrayList<>();
		try {
			logger.info("Setting all Flows to Test.");
			String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowsToTest").trim().split(Pattern.quote(","));
			String[] entitiesToTest = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entitiesToTest").trim().split(Pattern.quote(","));

			for (String flow : flowsToTest) {
				boolean updateUserSetting = true;

				for (String entityName : entitiesToTest) {
					allTestData.add(new Object[]{flow.trim(), entityName.trim(), updateUserSetting});
					updateUserSetting = false;
				}
			}

			logger.info("Total Flows to Test : {}", allTestData.size());
		} catch (Exception e) {
			logger.error("Exception while Setting all flows to test for MSA Tagging. {}", e.getMessage());
		}
		return allTestData.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForMSATagging")
	public void testMSATagging(String flowToTest, String entityName, boolean updateUserSetting) {
		CustomAssert csAssert = new CustomAssert();

		ExecutorService executor = Executors.newFixedThreadPool(ConfigureEnvironment.noOfThreads);

		try {
			logger.info("Validating MSA Tagging Flow [{}] and Entity {}", flowToTest, entityName);
			Map<String, String> params = new LinkedHashMap<>();

			if (updateUserSetting) {
				logger.info("Update User Setting flag is Turned On for Flow [{}] and Entity {}", flowToTest, entityName);
				logger.info("Getting Custom User Settings for Flow [{}] and Entity {}", flowToTest, entityName);
				Map<String, String> flowUserSettings = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileName, flowToTest);

				params.putAll(paramsMap);

				for (Map.Entry<String, String> flowSetting : flowUserSettings.entrySet()) {
					if (flowSetting.getKey().trim().equalsIgnoreCase("language")) {
						params.put("language.id", flowSetting.getValue());
					} else if (flowSetting.getKey().trim().equalsIgnoreCase("timeZone")) {
						params.put("timeZone.id", flowSetting.getValue());
					} else {
						params.put(flowSetting.getKey(), flowSetting.getValue());
					}
				}

				UpdateAccount updateObj = new UpdateAccount();
				Integer updateStatusCode = updateObj.hitUpdateAccount(params);

				if (updateStatusCode == 302) {
					userSettingsUpdated = true;
					logger.info("User Account Settings updated for Flow [{}] and Entity {}", flowToTest, entityName);
				} else {
					userSettingsUpdated = false;
					throw new SkipException("Couldn't update User Account Settings for Flow [" + flowToTest + "] and Entity " + entityName + ". Hence skipping test.");
				}
			}


			if (userSettingsUpdated) {
				logger.info("Validating Listing of Entity {} and Flow [{}]", entityName, flowToTest);
				int list_id = -1;
				String listIdStr = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFilePath"),
						ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile"), entityName.trim(), "entity_url_id");

				if (listIdStr != null && NumberUtils.isParsable(listIdStr.trim()))
					list_id = Integer.parseInt(listIdStr);

				if (list_id != -1) {
					int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);

					logger.info("Hitting List Data API for Entity {} and Flow [{}]", entityName, flowToTest);
					ListRendererListData listDataObj = new ListRendererListData();
					String listDataPayload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + listOffset + ",\"size\": " + listSize +
							",\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{}}}";
					listDataObj.hitListRendererListData(list_id, listDataPayload);
					String listDataResponse = listDataObj.getListDataJsonStr();

					if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
						listDataObj.setListData(listDataResponse);
						List<Map<Integer, Map<String, String>>> listData = listDataObj.getListData();
						logger.info("Total Records found for Entity {} and Flow [{}]: {}", entityName, flowToTest, listData.size());
						List<Map<Integer, Map<String, String>>> recordsToValidate = new ArrayList<>();

						if (applyRandomization) {
							logger.info("ApplyRandomization flag is set to True. Hence selecting {} Random records for Validation.", maxNoOfRecordsToValidate);
							int randomNumbers[] = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, listData.size() - 1, maxNoOfRecordsToValidate);

							for (int number : randomNumbers) {
								recordsToValidate.add(listData.get(number));
							}
						} else {
							logger.info("ApplyRandomization flag is set to False. Hence selecting all Records for Validation.");
							recordsToValidate.addAll(listData);
						}

						Integer tierId;

						if (ParseConfigFile.hasPropertyCaseSensitive(configFilePath, configFileName, flowToTest.trim(), "tierId")) {
							tierId = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, flowToTest.trim(),
									"tierId").trim());
						} else {
							tierId = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "default properties",
									"tierId").trim());
						}

						String tierValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "tierMapping", tierId.toString());

						if (tierValue == null || tierValue.trim().equalsIgnoreCase("")) {
							throw new SkipException("Couldn't get Tier Value of Tier Id " + tierId + " from Mapping Section. Hence skipping test for Entity " + entityName +
									" and Flow [" + flowToTest + "].");
						} else {
							int idColumn = listDataObj.getColumnIdFromColumnName("id");
							List<FutureTask<Boolean>> taskList = new ArrayList<>();

							logger.info("Total Records to Validate for Entity {} and Flow [{}]: {}", entityName, flowToTest, recordsToValidate.size());
							for (int i = 0; i < recordsToValidate.size(); i++) {
								final int index = i;
								Integer recordId = Integer.parseInt(recordsToValidate.get(index).get(idColumn).get("valueId"));

								FutureTask<Boolean> result = new FutureTask<>(() -> {

									logger.info("Validating Tier Value of Record #{} having Id {} for Entity {} and Flow [{}]", (index + 1), recordId, entityName,
											flowToTest);
									logger.info("Hitting Show API for Record #{} having Id {} of Entity {} and Flow [{}]", (index + 1), recordId, entityName,
											flowToTest);

									Show showObj = new Show();
									showObj.hitShow(entityTypeId, recordId);
									String showResponse = showObj.getShowJsonStr();

									if (ParseJsonResponse.validJsonResponse(showResponse)) {
										if (ParseJsonResponse.hasPermissionError(showResponse)) {
											throw new SkipException("Doesn't have Permission to Access Show Page of Record #" + (index + 1) + " having Id " +
													recordId + " for Entity " + entityName + " and Flow [" + flowToTest + "]. Hence skipping test.");
										} else {
											boolean showResult = showObj.verifyShowField(showResponse, "tier", tierValue, entityTypeId);

											if (!showResult) {
												boolean hierarchyFound = false;

												try {
													JSONObject showJsonObj = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data")
															.getJSONObject("tier");

													if (showJsonObj.has("values") && !showJsonObj.isNull("values")) {
														hierarchyFound = true;
													}
												} catch (Exception e) {
													logger.warn("Exception while Accessing Tier Hierarchy.");
												}

												if (hierarchyFound) {
													logger.error("Validation of Record #{} having Id {} for Entity {} and Flow [{}] failed on Show Page.", (index + 1),
															recordId, entityName, flowToTest);
													csAssert.assertTrue(false, "Validation of Record #" + (index + 1) + " having Id " + recordId +
															" for Entity " + entityName + " and Flow [" + flowToTest + "] failed on Show Page.");
												}
											}
										}
									} else {
										logger.error("Show API Response for Entity {}, Id {} and Flow [{}] is an Invalid JSON.", entityName, recordId, flowToTest);
										csAssert.assertTrue(false, "Show API Response for Entity " + entityName + ", Id " + recordId +
												" and Flow [" + flowToTest + "] is an Invalid JSON.");
									}
									return true;
								});
								taskList.add(result);
								executor.execute(result);
							}
							for (FutureTask<Boolean> task : taskList)
								task.get();
						}
					} else {
						logger.error("List Data API Response for Entity {} and Flow [{}] is an Invalid JSON.", entityName, flowToTest);
						csAssert.assertTrue(false, "List Data API Response for Entity " + entityName + " and Flow [" + flowToTest +
								"] is an Invalid JSON.");
					}
				} else {
					throw new SkipException("Couldn't get List Id for Entity " + entityName + " and Flow [" + flowToTest + "]. Hence skipping test.");
				}
			}
		} catch (SkipException e) {
			userSettingsUpdated = false;
			logger.warn("Skip Message: " + e.getMessage());
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			executor.shutdownNow();
			csAssert.assertTrue(false, "Exception while validating MSA Tagging. " + e.getMessage());
			userSettingsUpdated = false;
		}

		csAssert.assertAll();
	}
}
