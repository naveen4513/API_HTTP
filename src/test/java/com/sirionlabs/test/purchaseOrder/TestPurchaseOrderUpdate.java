package com.sirionlabs.test.purchaseOrder;

import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TestPurchaseOrderUpdate {

	private final static Logger logger = LoggerFactory.getLogger(TestPurchaseOrderUpdate.class);
	private String configFilePath = null;
	private String configFileName = null;
	private String extraFieldsConfigFilePath = null;
	private String extraFieldsConfigFileName = null;

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("POUpdateConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("POUpdateConfigFileName");
		extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
		extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
	}

	@DataProvider
	public Object[][] dataProviderForTestPOUpdate() throws ConfigurationException {
		logger.info("Setting all Purchase Order Update Flows to Validate");
		List<Object[]> allTestData = new ArrayList<>();
		List<String> flowsToTest = new ArrayList<>();

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "testAllFlows");
		if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
			logger.info("TestAllFlows property is set to True. Therefore all the flows are to validated");
			flowsToTest = ParseConfigFile.getAllSectionNames(configFilePath, configFileName);
		} else {
			String allFlows[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowsToValidate").split(Pattern.quote(","));
			for (String flow : allFlows) {
				if (ParseConfigFile.containsSection(configFilePath, configFileName, flow.trim())) {
					flowsToTest.add(flow.trim());
				} else {
					logger.info("Flow having name [{}] not found in Purchase Order Update Config File.", flow.trim());
				}
			}
		}

		for (String flowToTest : flowsToTest) {
			allTestData.add(new Object[]{flowToTest});
		}
		return allTestData.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForTestPOUpdate")
	public void testPOUpdate(String flowToTest) {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Validating Purchase Order Update Flow [{}]", flowToTest);
			int poId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "poId"));

			logger.info("Hitting Edit Get API for Purchase Order Id {} and Flow [{}]", poId, flowToTest);
			Edit editObj = new Edit();
			String editGetResponse = editObj.hitEdit("purchase orders", poId);

			if (ParseJsonResponse.validJsonResponse(editGetResponse)) {
				verifyFieldsOnUpdatePage(flowToTest, editGetResponse, csAssert);

				Map<String, String> fieldsPayloadMap = ParseConfigFile.getAllConstantPropertiesCaseSensitive(extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest);

				if (fieldsPayloadMap.size() > 0) {
					String editPostPayload = EntityOperationsHelper.createPayloadForEditPost(editGetResponse, fieldsPayloadMap);

					if (editPostPayload == null) {
						throw new SkipException("Couldn't get Edit Post Payload for Flow [" + flowToTest + "]");
					}

					logger.info("Hitting Edit Post API for Purchase Order Id {} and Flow [{}]", poId, flowToTest);
					String editPostResponse = editObj.hitEdit("purchase orders", editPostPayload);

					if (ParseJsonResponse.validJsonResponse(editPostResponse)) {
						JSONObject jsonObj = new JSONObject(editPostResponse);
						String editStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
						String expectedResult = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedResult").trim();
						logger.info("Actual Result: {} and Expected Result: {} for Flow [{}]", editStatus, expectedResult, flowToTest);

						if (expectedResult.equalsIgnoreCase("success")) {
							if (!editStatus.equalsIgnoreCase("success")) {
								csAssert.assertTrue(false, "Couldn't update Purchase Order Id " + poId + " for Flow [" + flowToTest + "]");
							}
						} else {
							if (editStatus.equalsIgnoreCase("success")) {
								csAssert.assertTrue(false, "Purchase Order Id " + poId + " edited successfully for Flow [" + flowToTest +
										"] whereas it was expected to fail");
							}
						}

						//Restore Purchase Order to Original State
						if (editStatus.equalsIgnoreCase("success")) {
							if (EntityOperationsHelper.restoreRecord("purchase orders", poId, editGetResponse)) {
								logger.info("Purchase Order Id {} restored Successfully.", poId);
							} else {
								logger.error("Couldn't Restore Purchase Order Id {}", poId);
							}
						}
					} else {
						csAssert.assertTrue(false, "Edit Post API Response for Purchase Order Id " + poId + " and Flow [" + flowToTest +
								"] is an Invalid JSON.");
					}
				} else {
					throw new SkipException("Couldn't get Fields Payload Map for Flow [" + flowToTest + "]");
				}
			} else {
				csAssert.assertTrue(false, "Edit Get API Response for Purchase Order Id " + poId + " and Flow [" + flowToTest +
						"] is an Invalid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Purchase Order Update for Flow [" + flowToTest + "]. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	private void verifyFieldsOnUpdatePage(String flowToTest, String editGetResponse, CustomAssert csAssert) {
		try {
			logger.info("Validating Fields on Update Page for Flow [{}]", flowToTest);
			JSONObject jsonObj = new JSONObject(editGetResponse);
			jsonObj = jsonObj.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent");
			JSONArray jsonArr = jsonObj.getJSONArray("fields");

			if (jsonArr.length() != 1) {
				csAssert.assertTrue(false, "More than 1 Tab Present in Update Page of Flow [" + flowToTest +
						"] i.e. Tab other than General is also Present");
			} else {
				if (!jsonArr.getJSONObject(0).getString("label").trim().equalsIgnoreCase("General")) {
					csAssert.assertTrue(false, "General Tab not Present in Update Page of Flow [" + flowToTest + "]");
				}
			}

			String[] readOnlyFieldsArr = {"supplier", "contract"};
			List<String> readOnlyFieldsList = new ArrayList<>(Arrays.asList(readOnlyFieldsArr));

			for (String readOnlyFieldName : readOnlyFieldsList) {
				String fieldDisplayMode = ParseJsonResponse.getFieldAttributeFromName(editGetResponse, readOnlyFieldName.trim(), "displayMode");

				if (fieldDisplayMode == null) {
					throw new SkipException("Couldn't get Display Mode for Field " + readOnlyFieldName.trim() + " from Edit Get Response.");
				}

				if (!fieldDisplayMode.trim().equalsIgnoreCase("display")) {
					csAssert.assertTrue(false, "Expected DisplayMode: display and Actual DisplayMode: " + fieldDisplayMode.trim() +
							" for Field Name " + readOnlyFieldName + " and Flow [" + flowToTest + "]");
				}
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Fields on Update Page for Flow [" + flowToTest + "]. " + e.getMessage());
		}
	}

	@Test(priority = 1)
	public void testPOUpdateArchivedAndOnHold() {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Hitting ListRenderer List Data API for Archived and On Hold Status");
			ListRendererListData listDataObj = new ListRendererListData();
			int poListId = ConfigureConstantFields.getListIdForEntity("purchase orders");
			String payload = "{\"filterMap\":{\"entityTypeId\":181,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
					"\"filterJson\":{\"6\":{\"filterId\":\"6\",\"filterName\":\"status\",\"entityFieldId\":null,\"entityFieldHtmlType\":null," +
					"\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"Archived\"},{\"id\":\"2\",\"name\":\"On Hold\"}]}}}}}";

			listDataObj.hitListRendererListData(poListId, payload);
			String listDataResponse = listDataObj.getListDataJsonStr();

			if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
				List<Map<Integer, Map<String, String>>> listData = ListDataHelper.getListData(listDataResponse);

				if (listData.isEmpty()) {
					logger.error("Couldn't get any record in List Data API for PO");
					throw new SkipException("Couldn't get any record in List Data API for PO");
				}

				int randomNumber = RandomNumbers.getRandomNumberWithinRangeIndex(0, listData.size() - 1);
				Integer idColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "name");

				if (idColumnNo == -1) {
					throw new SkipException("Couldn't get No of Column Id from List Data Response.");
				}

				int poId = Integer.parseInt(listData.get(randomNumber).get(idColumnNo).get("valueId"));

				Edit editObj = new Edit();
				String editGetResponse = editObj.hitEdit("purchase orders", poId);

				if (ParseJsonResponse.validJsonResponse(editGetResponse)) {
					JSONObject jsonObj = new JSONObject(editGetResponse);
					jsonObj = jsonObj.getJSONObject("body").getJSONObject("layoutInfo");
					JSONArray jsonArr = jsonObj.getJSONArray("actions");

					for (int i = 0; i < jsonArr.length(); i++) {
						if (jsonArr.getJSONObject(i).getString("name").trim().equalsIgnoreCase("Edit")) {
							csAssert.assertTrue(false, "Edit Button available in Edit Get Response for PO Id " + poId);
							break;
						}
					}
				} else {
					csAssert.assertTrue(false, "Edit Get Response for PO Id " + poId + " is an Invalid JSON.");
				}
			} else {
				csAssert.assertTrue(false, "ListData API Response for PO is an Invalid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating PO Update on Archived Record. " + e.getMessage());
		}
		csAssert.assertAll();
	}
}