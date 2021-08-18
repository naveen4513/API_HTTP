package com.sirionlabs.helper;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;


import java.util.ArrayList;
import java.util.List;

public class UserAccessHelper {

	private final static Logger logger = LoggerFactory.getLogger(UserAccessHelper.class);

	public void verifyUserLevelTabAccessInShowAPI(String entityName, List<String> expectedHiddenTabs, int listId, int listDataOffset, int listDataSize,
	                                              Boolean applyRandomization, int maxNoOfRecordsToValidate, CustomAssert csAssert) {
		try {
			int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
			List<Integer> recordIdsToValidate = getRecordIdsToValidateUserLevelTabAccess(entityName, entityTypeId, listId, listDataOffset, listDataSize, applyRandomization,
					maxNoOfRecordsToValidate);

			if (recordIdsToValidate.isEmpty()) {
				logger.error("Couldn't get Records to Validate User Level Tab Access in Show API from ListData API for Entity {}. Hence skipping test", entityName);
				throw new SkipException("Couldn't get Records to Validate User Level Tab Access in Show API from ListData API for Entity " + entityName +
						". Hence skipping test");
			}

			logger.info("Total Records to Validate: {}", recordIdsToValidate.size());
			Show showObj = new Show();

			for (int i = 0; i < recordIdsToValidate.size(); i++) {
				int recordId = recordIdsToValidate.get(i);
				logger.info("Validating Record #{} for Entity {}", (i + 1), entityName);
				logger.info("Hitting Show API for Record Id {} of Entity {}.", recordId, entityName);
				showObj.hitShow(entityTypeId, recordId);
				String showResponse = showObj.getShowJsonStr();

				if (ParseJsonResponse.validJsonResponse(showResponse)) {
					if (showObj.isShowPageAccessible(showResponse)) {
						List<String> allTabLabels = ParseJsonResponse.getAllTabLabels(showResponse);
						logger.info("Validating User Level Tabs in Show API Response for Record Id {} of Entity {}", recordId, entityName);

						matchUserLevelTabs(allTabLabels, expectedHiddenTabs, csAssert);
					} else {
						logger.warn("Permission denied to access Show Page of Record Id {} of Entity {}", recordId, entityName);
					}
				} else {
					logger.error("Show API Response for Entity {} and Record Id {} is an Invalid JSON.", entityName, recordId);
					csAssert.assertTrue(false, "Show API Response for Entity " + entityName + " and Record Id " + recordId + " is an Invalid JSON");
				}
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			logger.error("Exception while Verifying User Level Tab Access in Show API for Entity {}. {}", entityName, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while Verifying User Level Tab Access in Show API for Entity " + entityName + ". " + e.getMessage());
		}
	}

	private List<Integer> getRecordIdsToValidateUserLevelTabAccess(String entityName, int entityTypeId, int listId, int listDataOffset, int listDataSize,
	                                                               Boolean applyRandomization, int maxNoOfRecordsToValidate) {
		List<Integer> recordIdsToValidate = new ArrayList<>();

		try {
			logger.info("Hitting ListData API for Entity {} with Offset {} and Size {}", entityName, listDataOffset, listDataSize);
			ListRendererListData listDataObj = new ListRendererListData();
			listDataObj.hitListRendererListData(listId, ListDataHelper.getPayloadForListData(entityTypeId, listDataSize, listDataOffset));
			String listDataResponse = listDataObj.getListDataJsonStr();

			if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
				List<Integer> recordsToValidate = new ArrayList<>();
				JSONObject jsonObj = new JSONObject(listDataResponse);

				int filteredDataCount = ListDataHelper.getFilteredListDataCount(listDataResponse);
				filteredDataCount = filteredDataCount < listDataSize ? filteredDataCount : listDataSize;

				if (filteredDataCount == 0) {
					logger.info("No Record found in ListData API for Entity {} with Offset {} and Size {}", entityName, listDataOffset, listDataSize);
					return recordIdsToValidate;
				}

				if (applyRandomization) {
					logger.info("Selecting Random records from List Data API.");
					int[] randomNumbers = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, filteredDataCount - 1, maxNoOfRecordsToValidate);

					for (int recordNumber : randomNumbers) {
						recordsToValidate.add(recordNumber);
					}
				} else {
					for (int i = 0; i < filteredDataCount; i++)
						recordsToValidate.add(i);
				}

				int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
				JSONArray jsonArr = jsonObj.getJSONArray("data");

				for (Integer record : recordsToValidate) {
					String idValue = jsonArr.getJSONObject(record).getJSONObject(String.valueOf(idColumnNo)).getString("value");
					int recordId = ListDataHelper.getRecordIdFromValue(idValue);
					recordIdsToValidate.add(recordId);
				}
			} else {
				logger.error("List Data API Response for Entity {} with ListDataSize {} and Offset {} is an Invalid JSON.", entityName, listDataSize, listDataOffset);
			}
		} catch (Exception e) {
			logger.error("Exception while Getting Records to Validate User Level Tab Access from ListData API for Entity {}. {}", entityName, e.getStackTrace());
		}
		return recordIdsToValidate;
	}

	private void matchUserLevelTabs(List<String> actualTabs, List<String> expectedHiddenTabs, CustomAssert csAssert) {
		for (String tabLabel : expectedHiddenTabs) {
			logger.info("Validating that Tab {} is not present in the Response.", tabLabel);

			if (actualTabs.contains(tabLabel)) {
				logger.error("Tab {} is visible and not hidden in the Response.", tabLabel);
				csAssert.assertTrue(false, "Tab " + tabLabel + " is visible and not hidden in the Response");
			}
		}
	}

	public List<String> getAllEntitiesFromFieldProvisioningAPI(String fieldProvisioningResponse) {
		List<String> allEntities = new ArrayList<>();

		try {
			Document html = Jsoup.parse(fieldProvisioningResponse);
			Element div = html.getElementById("tabs").getElementById("generalInfo");

			Elements classes = div.getElementsByClass("top-heading");
			Element table = classes.select("table").get(0);

			Elements allRows = table.select("tr");

			for (Element row : allRows) {
				Elements columns = row.select("td");

				for (int i = 0; i < columns.size(); i++) {
					Element column = columns.get(i);

					if (column.text().trim().toLowerCase().equalsIgnoreCase("Entity :")) {
						Elements allOptions = columns.get(i + 1).select("option");

						for (int j = 1; j < allOptions.size(); j++) {
							Element option = allOptions.get(j);
							allEntities.add(option.text().trim());
						}

						return allEntities;
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Getting All Entities from Field Provisioning API. {}", e.getMessage());
			return null;
		}
		return allEntities;
	}

	public Integer getIdForUser(String userName) {
		OptionsHelper helperObj = new OptionsHelper();
		return helperObj.getIdForUser(userName);
	}
}