package com.sirionlabs.test.insights;

import com.sirionlabs.api.insights.EntityList;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererFilterData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import com.sirionlabs.config.ConfigureEnvironment;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
//@Listeners(value = MyTestListenerAdapter.class)
public class TestInsights extends TestRailBase {

	private final static Logger logger = LoggerFactory.getLogger(TestInsights.class);
	private static Integer size;
	private static Integer offset;
	private static String orderByColumnName;
	private static String orderDirection;
	private Boolean isAllEntitiesToBeTested = true;
	private Boolean validateAllInsightsForEntity = false;
	private List<String> allEntitySection;
	private String configFilePath;
	private String configFileName;
	private String entityIdMappingFileName;
	private String baseFilePath;
	private List<String> entitiesToTest = new ArrayList<String>();

	@BeforeClass(groups = { "minor" })
	public void configureProperties() {
		try {
			configFilePath = ConfigureConstantFields.getConstantFieldsProperty("InsightsConfigFilePath");
			configFileName = ConfigureConstantFields.getConstantFieldsProperty("InsightsConfigFileName");

			entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
			baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");

			String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "isallentitiestobetested");
			if (temp != null && temp.trim().equalsIgnoreCase("false"))
				isAllEntitiesToBeTested = false;

			String temp2 = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "validateAllInsightsForEntity");
			if (temp2 != null && temp2.trim().equalsIgnoreCase("true"))
				validateAllInsightsForEntity = true;

			entitiesToTest = this.getList("entitiestotest");
			allEntitySection = ParseConfigFile.getAllSectionNames(configFilePath, configFileName);

			size = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "size"));
			offset = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "offset"));
			orderByColumnName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "orderByColumnName");
			orderDirection = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "orderDirection");

		} catch (Exception e) {
			logger.error("Exception occurred while setting the configuration properties for insights validation. error = {}", e.getMessage());
			e.printStackTrace();
		}
		testCasesMap = getTestCasesMapping();
	}

//	@DataProvider(name = "getAllEntitySections")
//	public Object[][] getAllEntitySections() {
//		int i = 0;
//		Object[][] groupArray = null;
//		List<String> entitySectionsToTest = null;
//
//		try {
//			if (isAllEntitiesToBeTested) {
//				groupArray = new Object[allEntitySection.size()][];
//				entitySectionsToTest = allEntitySection;
//			} else {
//				if (entitiesToTest.size() > 0) {
//					groupArray = new Object[entitiesToTest.size()][];
//					entitySectionsToTest = entitiesToTest;
//				}
//			}
//
//			if (entitySectionsToTest != null) {
//				for (String entitySection : entitySectionsToTest) {
//					groupArray[i] = new Object[2];
//					Integer entitySectionTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entitySection, "entity_type_id"));
//					groupArray[i][0] = entitySection; // EntityName
//					groupArray[i][1] = entitySectionTypeId; // EntityTypeId
//					i++;
//				}
//			}
//		} catch (Exception e) {
//			logger.error("Exception occurred while getting entities section for dataProvider");
//			e.printStackTrace();
//		}
//
//		return groupArray;
//	}

//	@Test(dataProvider = "getAllEntitySections",enabled = true)
//	public void test(String entityName, Integer entityTypeId) {
	@Test(groups = { "minor" })
	public void test() throws InterruptedException, ExecutionException {
		int i = 0;
		Object[][] groupArray = null;
		List<String> entitySectionsToTest = null;

		try {
			if (isAllEntitiesToBeTested) {
				groupArray = new Object[allEntitySection.size()][];
				entitySectionsToTest = allEntitySection;
			} else {
				if (entitiesToTest.size() > 0) {
					groupArray = new Object[entitiesToTest.size()][];
					entitySectionsToTest = entitiesToTest;
				}
			}

			if (entitySectionsToTest != null) {
				for (String entitySection : entitySectionsToTest) {
					groupArray[i] = new Object[2];
					Integer entitySectionTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entitySection, "entity_type_id"));
					groupArray[i][0] = entitySection; // EntityName
					groupArray[i][1] = entitySectionTypeId; // EntityTypeId
					i++;
				}
			}
		} catch (Exception e) {
			logger.error("Exception occurred while getting entities section for dataProvider");
			e.printStackTrace();
		}

		logger.info("Staring test");
		CustomAssert customAssert = new CustomAssert();
		String entityName;
		Integer entityTypeId;

		ExecutorService executor = Executors.newFixedThreadPool(ConfigureEnvironment.noOfThreads);
		List<FutureTask<Boolean>> taskList = new ArrayList<>();

		for(i =0;i<groupArray.length;i++ ) {
			entityName = (String) groupArray[i][0];
			entityTypeId = (Integer) groupArray[i][1];

			final String finalEntityName = entityName;
			final int finalEntityTypeId = entityTypeId;

			FutureTask<Boolean> result = new FutureTask<>(() -> {

				//CustomAssert csAssert = new CustomAssert();
			try {

				EntityList entityListObj = new EntityList();
				entityListObj.hitInsightsEntityList();
				String response = entityListObj.getInsightsEntityListJsonStr();
				logger.info("insights entity-list response = {}", response);

				Integer urlId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, finalEntityName, "entity_url_id"));

				Boolean isValidJson = APIUtils.validJsonResponse(response, "insights entity-list.");
				if (isValidJson) {
					validate(response, String.valueOf(finalEntityTypeId), urlId, finalEntityName, customAssert);

				} else {
					logger.error("insights entity-list api response is not valid json.");
					customAssert.assertTrue(false, "insights entity-list api response is not valid json.");
				}
			} catch (Exception e) {
				logger.error("Exception occurred while validating insights entity-list. error = {}", e.getMessage());
				e.printStackTrace();
				customAssert.assertTrue(false, "Exception occurred while validating insights. error = " + e.getMessage());
			}
				return true;
			});
			taskList.add(result);
			executor.execute(result);


		}

		for (FutureTask<Boolean> task : taskList)
			task.get();

//		addTestResult(getTestCaseIdForMethodName("test"), customAssert);
		customAssert.assertAll();
	}

	private List<String> getList(String propertyName) {
		return getList(null, propertyName);
	}

	private List<String> getList(String sectionName, String propertyName) {

		List<String> idList = null;
		try {
			String value = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName, propertyName);
			idList = new ArrayList<String>();

			if (!value.trim().equalsIgnoreCase("")) {
				String ids[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName, propertyName).split(",");

				for (int i = 0; i < ids.length; i++)
					idList.add(ids[i].trim());
			}
		} catch (Exception e) {
			logger.error("Exception occurred while getting id list from config file. property : {}", propertyName);
			e.printStackTrace();
		}
		return idList;
	}

	private void validate(String insightResponse, String entityId, Integer listId, String entityName, CustomAssert csAssert) {

		JSONArray resArray = new JSONArray(insightResponse);
		String selectedColumns = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"selected col string");

		if(selectedColumns == null){
			selectedColumns = "";
		}

		for (int i = 0; i < resArray.length(); i++) {
			JSONObject entityObject = resArray.getJSONObject(i);

			String entityNameInResponse = entityObject.getString("name");
			Integer insightArrayLength = entityObject.getJSONArray("insights").length();

			if (insightArrayLength > 0) {
				Integer listIdInResponse = entityObject.getJSONArray("insights").getJSONObject(0).getInt("listId");

				if (listIdInResponse.equals(listId)) {
					logger.info("\n**************************** Validating insights for entity : {} *****************", entityNameInResponse);
					JSONArray insightArray = entityObject.getJSONArray("insights");

					List<String> allInsightNamesInConfigWithFilterName = getList(entityName, "insight_names");
					List<String> insightNamesInConfig = getInsightNames(allInsightNamesInConfigWithFilterName);
					if (validateAllInsightsForEntity) {

						logger.info("validateAllInsightsForEntity flag is set as true. Hence validating all the insights under entity section : {}", entityName);
						for (int j = 0; j < insightArray.length(); j++) {
							String insightName = insightArray.getJSONObject(j).getString("name").trim();
							if (insightNamesInConfig.contains(insightName)) {
								logger.info("Validation on listing page for insight : {}, entity : {} , started..........", insightName, entityName);
								Integer urlId = insightArray.getJSONObject(j).getInt("listId");
								Integer insightComputationId = insightArray.getJSONObject(j).getInt("insightComputationId");

								Boolean isPassed = validateListing(insightName, entityId, urlId, insightComputationId, allInsightNamesInConfigWithFilterName,selectedColumns);
								csAssert.assertTrue(isPassed, "Insights validation on listing. entity = " + entityName + ", insight name = " + insightName);

							} else {
								logger.warn("insight = {} is not present in config file. hence skipping validation for this insight.", insightName);
								continue;
							}
						}
					} else {
						//iterate for specified insights
						List<String> specificInsights = getList("insightstotest");

						for (String insightName : specificInsights) {
							if (insightNamesInConfig.contains(insightName)) {
								for (int j = 0; j < insightArray.length(); j++) {
									String insightNameInResponse = insightArray.getJSONObject(j).getString("name").trim();

									if (insightNameInResponse.equalsIgnoreCase(insightName)) {
										Integer urlId = insightArray.getJSONObject(j).getInt("listId");
										Integer insightComputationId = insightArray.getJSONObject(j).getInt("insightComputationId");

										Boolean isPassed = validateListing(insightName, entityId, urlId, insightComputationId, allInsightNamesInConfigWithFilterName,selectedColumns);
										csAssert.assertTrue(isPassed, "Insights validation on listing. entity = " + entityName + ", insight name = " + insightName);
										break;
									}
								}
							}
						}
					}
				}
			} else {
				logger.warn("No insights found for entity : {}", entityNameInResponse);
			}
		}
	}

	private Boolean validateListing(String insightName, String entityTypeId, Integer urlId, Integer insightComputationId, List<String> allInsightNamesInConfig,String selectedColumns) {
		Boolean isPassed = false;

		try {
			Map<String, String> queryParam = new HashMap<>();
			queryParam.put("insightComputationId", insightComputationId.toString());
//			queryParam.put("version","2.0");
			logger.info("Hitting metaData api for entityId = {}, insightName = {}, insightComputationId = {}", entityTypeId, insightName, insightComputationId);
			ListRendererDefaultUserListMetaData metaDataObj = new ListRendererDefaultUserListMetaData();
			metaDataObj.hitListRendererDefaultUserListMetadata(urlId, queryParam);
			String metaDataJsonStr = metaDataObj.getListRendererDefaultUserListMetaDataJsonStr();

			if (APIUtils.validJsonResponse(metaDataJsonStr, "metaData api")) {
				logger.info("Hitting filterData api for entityId = {}, insightName = {}, insightComputationId = {}", entityTypeId, insightName, insightComputationId);
				ListRendererFilterData filterDataObj = new ListRendererFilterData();
				filterDataObj.hitListRendererFilterData(urlId, queryParam);
				String filterDataJsonStr = filterDataObj.getListRendererFilterDataJsonStr();

				if (APIUtils.validJsonResponse(filterDataJsonStr, "filterData api")) {
					logger.info("Hitting ListData api for entityId = {}, insightName = {}, insightComputationId = {},", entityTypeId, insightName, insightComputationId);

					String payload = getPayloadForListData(filterDataJsonStr, entityTypeId, insightName, allInsightNamesInConfig,selectedColumns);

					logger.info("Hitting ListRendererListData. Payload : {}", payload);
					ListRendererListData listDataObj = new ListRendererListData();

					String listDataResponse = listDataObj.hitListRendererInsight(urlId, insightComputationId, payload);


					Boolean isValidJson = APIUtils.validJsonResponse(listDataResponse, "listData for insights");

					if (isValidJson) {
						logger.info("************************************* validation passed for insight : {} ***********************************\n", insightName);
						isPassed = true;
					} else {
						isPassed = false;
						logger.error("listData response is not valid json for insight = {}, payload={}", insightName, payload);
					}
				} else {
					isPassed = false;
					logger.error("Filter Data response is not valid json for entityId = {}, insightName = {}, insightComputationId = {}", entityTypeId, insightName, insightComputationId);
				}

			} else {
				isPassed = false;
				logger.error("metaData response is not valid json for entityId = {}, insightName = {}, insightComputationId = {}", entityTypeId, insightName, insightComputationId);
			}

		} catch (Exception e) {
			logger.error("Exception while validating list data for insight : {}, error = {}", insightName, e.getMessage());
			e.printStackTrace();
		}

		return isPassed;
	}

	private String getPayloadForListData(String filterDataResponse, String entityTypeId, String insightName, List<String> allInsightNamesInConfig,String selectedColumns) {
		String payload = null;
		String filterName = null;
		String filterJson = null;
		Integer filterId = null;

		for (String insight : allInsightNamesInConfig) {
			if (insight.contains(insightName)) {
				filterName = insight.trim().split("->")[1].trim();
				break;
			}
		}

		logger.info("Forming payload for list Data. entityTypeId = {}, InsightName = {}, filterName = {}", entityTypeId, insightName, filterName);
		JSONObject filterResponse = new JSONObject(filterDataResponse);
		JSONArray filterResponseArray = JSONUtility.convertJsonOnjectToJsonArray(filterResponse);

		for (int i = 0; i < filterResponseArray.length(); i++) {
			String filterNameInResponse = filterResponseArray.getJSONObject(i).getString("filterName");
			if (filterNameInResponse.equalsIgnoreCase(filterName)) {
				filterId = filterResponseArray.getJSONObject(i).getInt("filterId");
				filterJson = filterResponseArray.getJSONObject(i).toString();
				break;
			}
		}

		filterJson = "{\"" + filterId.toString() + "\":" + filterJson + "}";


		payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\"," +
				"\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":" + filterJson + "},\"selectedColumns\": [" + selectedColumns + "]}";

		return payload;
	}

	private List<String> getInsightNames(List<String> insightsWithFilterName) {
		List<String> allInsights = new ArrayList<>();

		for (String insights : insightsWithFilterName) {
			allInsights.add(insights.trim().split("->")[0].trim());
		}
		return allInsights;
	}

}
