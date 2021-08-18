package com.sirionlabs.test.IntegrationListing;

import com.sirionlabs.api.IntegrationListing.IntegrationListingListData;
import com.sirionlabs.api.IntegrationListing.IntegrationListingListFilterData;
import com.sirionlabs.api.IntegrationListing.IntegrationListingListMetaData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.ParseException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


import java.io.IOException;

/**
 * Created by shiv.ashish on 06/11/2017.
 */
public class TestIntegrationListing {

	private final static Logger logger = LoggerFactory.getLogger(TestIntegrationListing.class);

	static int size;
	static int offset;
	static String integrationListingConfigFilePath;
	static String integrationListingConfigFileName;
	static String orderByColumnName;
	static String orderDirection;
	static String listId;

	IntegrationListingListData integrationListingListData;
	IntegrationListingListFilterData integrationListingListFilterData;
	IntegrationListingListMetaData integrationListingListMetaData;


	@BeforeClass(alwaysRun = true)
	public void beforeClass() throws IOException, ConfigurationException {
		logger.info("In Before Class method");
		getListDataConfigData();
	}

	private void getListDataConfigData() throws ParseException, IOException, ConfigurationException {
		logger.info("Getting Test Data for listData api");
		integrationListingListData = new IntegrationListingListData();
		integrationListingListFilterData = new IntegrationListingListFilterData();
		integrationListingListMetaData = new IntegrationListingListMetaData();
		TestIntegrationListing.integrationListingConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("IntegrationListingConfigFilePath");
		TestIntegrationListing.integrationListingConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("IntegrationListingConfigFileName");
		TestIntegrationListing.size = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(TestIntegrationListing.integrationListingConfigFilePath, TestIntegrationListing.integrationListingConfigFileName,
				"size"));
		TestIntegrationListing.offset = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(TestIntegrationListing.integrationListingConfigFilePath, TestIntegrationListing.integrationListingConfigFileName,
				"offset"));
		TestIntegrationListing.orderByColumnName = ParseConfigFile.getValueFromConfigFile(TestIntegrationListing.integrationListingConfigFilePath, TestIntegrationListing.integrationListingConfigFileName,
				"orderByColumnName");
		TestIntegrationListing.orderDirection = ParseConfigFile.getValueFromConfigFile(TestIntegrationListing.integrationListingConfigFilePath, TestIntegrationListing.integrationListingConfigFileName,
				"orderDirection");
		TestIntegrationListing.listId = ParseConfigFile.getValueFromConfigFile(TestIntegrationListing.integrationListingConfigFilePath, TestIntegrationListing.integrationListingConfigFileName,
				"listid");

	}

	@Test(groups = "sanity", priority = 0)
	public void verifyIntegrationListingGetMetaData() {
		CustomAssert csAssertion = new CustomAssert();
		try {
			integrationListingListMetaData.hitIntegrationListingListMetaData(listId);
			csAssertion.assertTrue(integrationListingListMetaData.getApiStatusCode().contains("200"), "Integration Listing Get List Meta Data API Status Code is Incorrect ");
			csAssertion.assertTrue(APIUtils.validJsonResponse(integrationListingListMetaData.getApiResponse()), "Integration Listing Get List Meta Data API Response is not valid Json");

			boolean isListDataApplicationError = APIUtils.isApplicationErrorInResponse(integrationListingListMetaData.getApiResponse());
			boolean isListDataPermissionDenied = APIUtils.isPermissionDeniedInResponse(integrationListingListMetaData.getApiResponse());
			csAssertion.assertTrue(!isListDataApplicationError, "Application error found while hitting Integration Listing Get List Meta Data API");
			csAssertion.assertTrue(!isListDataPermissionDenied, "Permission Denied error found while hitting Integration Listing Get List Meta Data API");
			csAssertion.assertAll();
		} catch (Exception e) {
			e.printStackTrace();
		}


	}

	@Test(groups = "sanity", dependsOnMethods = "verifyIntegrationListingGetMetaData")
	public void verifyIntegrationListingGetFilterData() {
		CustomAssert csAssertion = new CustomAssert();
		try {
			integrationListingListFilterData.hitIntegrationListingListFilterData(listId);
			csAssertion.assertTrue(integrationListingListFilterData.getApiStatusCode().contains("200"), "Integration Listing Get List Filter Data API Status Code is Incorrect");
			csAssertion.assertTrue(APIUtils.validJsonResponse(integrationListingListFilterData.getApiResponse()), "Integration Listing Get List Filter Data API Response is not valid Json");

			boolean isListDataApplicationError = APIUtils.isApplicationErrorInResponse(integrationListingListFilterData.getApiResponse());
			boolean isListDataPermissionDenied = APIUtils.isPermissionDeniedInResponse(integrationListingListFilterData.getApiResponse());
			csAssertion.assertTrue(!isListDataApplicationError, "Application error found while hitting Integration Listing Get List Filter Data API");
			csAssertion.assertTrue(!isListDataPermissionDenied, "Permission Denied error found while hitting Integration Listing Get List Filter Data API");
			csAssertion.assertAll();
		} catch (Exception e) {
			e.printStackTrace();
		}


	}

	@Test(groups = "sanity", dependsOnMethods = "verifyIntegrationListingGetFilterData")
	public void verifyIntegrationListingGetListData() {
		CustomAssert csAssertion = new CustomAssert();
		try {
			JSONObject jsonObject = new JSONObject();
			JSONArray jsonArray = new JSONArray();
			jsonObject.put("offset", offset);
			jsonObject.put("size", size);
			jsonObject.put("orderByColumnName", orderByColumnName);
			jsonObject.put("orderDirection", orderDirection);
			jsonObject.put("filterDataList", jsonArray);

			integrationListingListData.hitIntegrationListingListData(listId, null, jsonObject.toString());
			csAssertion.assertTrue(integrationListingListData.getApiStatusCode().contains("200"), "Integration Listing Get List  Data API Status Code is Incorrect");
			csAssertion.assertTrue(APIUtils.validJsonResponse(integrationListingListData.getApiResponse()), "Integration Listing Get List Data API Response is not valid Json");

			boolean isListDataApplicationError = APIUtils.isApplicationErrorInResponse(integrationListingListData.getApiResponse());
			boolean isListDataPermissionDenied = APIUtils.isPermissionDeniedInResponse(integrationListingListData.getApiResponse());
			csAssertion.assertTrue(!isListDataApplicationError, "Application error found while hitting Integration Listing Get ListData API");
			csAssertion.assertTrue(!isListDataPermissionDenied, "Permission Denied error found while hitting Integration Listing Get ListData API");
			csAssertion.assertAll();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
