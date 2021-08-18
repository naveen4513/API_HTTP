package com.sirionlabs.test.IntegrationListing;

import com.sirionlabs.api.IntegrationListing.IntegrationListingGetTypes;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


import java.io.IOException;

/**
 * Created by shivashish on 8/12/17.
 */
public class TestGetListTypes {

	private final static Logger logger = LoggerFactory.getLogger(TestGetListTypes.class);


	static String integrationListingConfigFilePath;
	static String integrationListingConfigFileName;
	static String integrationSystemId;

	IntegrationListingGetTypes integrationListingGetTypes;


	@BeforeClass(alwaysRun = true)
	public void beforeClass() throws IOException, ConfigurationException {
		logger.info("In Before Class method");
		getListDataConfigData();
	}

	private void getListDataConfigData() throws ParseException, IOException, ConfigurationException {
		logger.info("Getting Test Data for listData api");
		integrationListingGetTypes = new IntegrationListingGetTypes();
		TestGetListTypes.integrationListingConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("IntegrationListingConfigFilePath");
		TestGetListTypes.integrationListingConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("IntegrationListingConfigFileName");
		TestGetListTypes.integrationSystemId = ParseConfigFile.getValueFromConfigFile(TestGetListTypes.integrationListingConfigFilePath, TestGetListTypes.integrationListingConfigFileName,
				"integrationsystemid");

	}

	@Test(groups = "sanity", priority = 0)
	public void verifyIntegrationListingGetTypes() {
		CustomAssert csAssertion = new CustomAssert();
		try {
			integrationListingGetTypes.hitIntegrationListingGetTypes(integrationSystemId);
			csAssertion.assertTrue(integrationListingGetTypes.getApiStatusCode().contains("200"), "Integration Listing Get Types API Status Code is Incorrect ");
			csAssertion.assertTrue(APIUtils.validJsonResponse(integrationListingGetTypes.getApiResponse()), "Integration Listing Get Types API Response is not valid Json");

			boolean isListDataApplicationError = APIUtils.isApplicationErrorInResponse(integrationListingGetTypes.getApiResponse());
			boolean isListDataPermissionDenied = APIUtils.isPermissionDeniedInResponse(integrationListingGetTypes.getApiResponse());
			csAssertion.assertTrue(!isListDataApplicationError, "Application error found while hitting Integration Listing Get Types API");
			csAssertion.assertTrue(!isListDataPermissionDenied, "Permission Denied error found while hitting Integration Listing Get Types API");
			csAssertion.assertAll();
		} catch (Exception e) {
			e.printStackTrace();
		}


	}

}
