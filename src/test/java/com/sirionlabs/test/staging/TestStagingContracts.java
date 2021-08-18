package com.sirionlabs.test.staging;

import com.sirionlabs.api.IntegrationListing.IntegrationListingListData;
import com.sirionlabs.api.integration.IntegrationShow;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class TestStagingContracts {

	private final static Logger logger = LoggerFactory.getLogger(TestStagingContracts.class);
	private static String configFilePath = null;
	private static String configFileName = null;
	private static String listDataResponse = null;
	private static Integer noOfContractsToTest = 1;
	private static Integer listDataSize = 20;
	private static Integer listDataOffset = 0;
	private static Integer contractsEntityTypeId;

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("StagingContractsConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("StagingContractsConfigFileName");

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "noOfContractsToTest");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			noOfContractsToTest = Integer.parseInt(temp);

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "listDataSize");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			listDataSize = Integer.parseInt(temp);

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "listDataOffset");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			listDataOffset = Integer.parseInt(temp);

		contractsEntityTypeId = ConfigureConstantFields.getEntityIdByName("contracts");
	}

	@Test(priority = 1)
	public void testStagingListDataAPIResponse() {
		CustomAssert csAssert = new CustomAssert();
		try {
			logger.info("Getting List Id for Staging.");
			String listId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "stagingListId");
			logger.info("Hitting Integration Listing Get List Data API");

			IntegrationListingListData listDataObj = new IntegrationListingListData();
			String payload = "{\"offset\":" + listDataOffset + ",\"size\":" + listDataSize + ",\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterDataList\":" +
					"[{\"filterId\":3,\"filterName\":\"Client Primary Key\",\"filterValue\":{\"type\":\"TEXT\"}}]}";

			listDataObj.hitIntegrationListingListData(listId, payload);
			listDataResponse = listDataObj.getApiResponse();

			if (!ParseJsonResponse.validJsonResponse(listDataResponse)) {
				listDataResponse = null;
				logger.error("Staging List Data API Response is an Invalid JSON.");
				csAssert.assertTrue(false, "Staging List Data API Response is an Invalid JSON.");
			}
		} catch (Exception e) {
			logger.error("Exception while checking Staging List Data API Response. {}", e.getMessage());
			csAssert.assertTrue(false, "Exception while checking Staging List Data API Response. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	@DataProvider
	public Object[][] dataProviderForStagingContracts() {
		List<Object[]> allTestData = new ArrayList<>();
		try {
			if (listDataResponse != null) {
				logger.info("Setting all Contracts to Test.");
				JSONObject jsonObj = new JSONObject(listDataResponse);
				JSONArray jsonArr = jsonObj.getJSONArray("data");
				logger.info("Total Contracts available: {}", jsonArr.length());

				if (jsonArr.length() > 0) {
					int randomNumbers[] = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, jsonArr.length() - 1, noOfContractsToTest);
					for (int number : randomNumbers) {
						jsonObj = jsonArr.getJSONObject(number).getJSONObject("1");

						String[] values = jsonObj.getString("value").trim().split(Pattern.quote(":;"));
						int id = Integer.parseInt(values[0].trim());
						String contractId = values[1].trim();

						allTestData.add(new Object[]{id, contractId});
					}
					logger.info("Total Contracts to Test : {}", allTestData.size());
				} else {
					logger.info("No Contract found.");
				}
			} else {
				logger.error("Couldn't set Contracts.");
			}
		} catch (Exception e) {
			logger.error("Exception while Setting all Contracts to test for Staging. {}", e.getMessage());
		}
		return allTestData.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForStagingContracts", priority = 2)
	public void testStagingContracts(int keyId, String contractId) {
		CustomAssert csAssert = new CustomAssert();
		try {
			logger.info("Validating Contract having Id {} and Key Id {}.", contractId, keyId);
			IntegrationShow showObj = new IntegrationShow();
			logger.info("Hitting Integration Show API for Contract Id {} and Key Id {}", contractId, keyId);
			String jsonStr = showObj.hitIntegrationShow(contractsEntityTypeId, contractId);

			if (ParseJsonResponse.validJsonResponse(jsonStr)) {
				JSONObject jsonObj = new JSONObject(jsonStr);
				jsonObj = jsonObj.getJSONObject("body").getJSONObject("data");

				String otherInfo = jsonObj.getString("otherInfo");
				if (!otherInfo.trim().contains("<id>" + keyId + "</id>")) {
					logger.error("Key Id doesn't match in Integration Show Response for Contract Id {} and Key Id {}", contractId, keyId);
					csAssert.assertTrue(false, "Key Id doesn't match in Integration Show Response for Contract Id " + contractId + " and Key Id " + keyId);
				}
			} else {
				logger.error("Integration Show API Response for Contract Id {} and Key Id {} is an Invalid JSON.", contractId, keyId);
				csAssert.assertTrue(false, "Integration Show API Response for Contract Id ");
			}
		} catch (Exception e) {
			logger.error("Exception while Validating Staging Contract having Id {} and Key Id {}. {}", contractId, keyId, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while Validating Staging Contract having Id " + contractId + " and Key Id " + keyId + ". " + e.getMessage());
		}
		csAssert.assertAll();
	}
}
