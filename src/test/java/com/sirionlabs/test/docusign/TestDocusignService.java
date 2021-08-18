package com.sirionlabs.test.docusign;

import com.sirionlabs.api.docusignService.AdminRedirectUrl;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.*;


import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Created by shivashish on 12/4/18.
 */
public class TestDocusignService {

	private final static Logger logger = LoggerFactory.getLogger(TestDocusignService.class);

	CustomAssert csAssert;


	String docusignConfigFilePath;
	String docusignConfigFileName;
	AdminRedirectUrl adminRedirectUrl;


	@BeforeClass
	public void beforeClass() throws IOException, ConfigurationException {
		logger.info("In Before Class method");
		csAssert = new CustomAssert();
		adminRedirectUrl = new AdminRedirectUrl();


		docusignConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("DocusignConfigFilePath");
		docusignConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("DocusignConfigFileName");
	}

	@BeforeMethod
	public void beforeMethod(Method method) {
		logger.info("In Before Method");
		logger.info("method name is: {} ", method.getName());
		logger.info("----------------------------------------------------Test Starts Here-----------------------------------------------------------------------");

	}


	/**
	 * http://jira.sirionlabs.office/browse/TA-1018 - Automation :/documentService/v1/admin/redirectUrl API
	 *
	 * @throws Exception
	 */
	@Test(priority = 0)
	public void testGetAdminRedirectUrl() throws Exception {
		csAssert = new CustomAssert();


		String scenarioName = "ta-1018";
		try {
			String testName = ParseConfigFile.getValueFromConfigFile(docusignConfigFilePath, docusignConfigFileName, scenarioName, "test");

			logger.info("Verifying the Test Case : {}", testName);
			String parser = ParseConfigFile.getValueFromConfigFile(docusignConfigFilePath, docusignConfigFileName, scenarioName, "parser");

			List<String> clientIds = Arrays.asList(ParseConfigFile.getValueFromConfigFile(docusignConfigFilePath, docusignConfigFileName, scenarioName, "clientids").split(parser));

			for (String clientId : clientIds) {

				logger.info("Verifying for Client Id:  {}", clientId);
				adminRedirectUrl.hitGetAdminRedirectUrlAPI(clientId);

				csAssert.assertTrue(adminRedirectUrl.getApiStatusCode().contains("200"), "Get adminRedirectUrl API Response is incorrect");
				csAssert.assertTrue(APIUtils.validJsonResponse(adminRedirectUrl.getApiResponse()), "Get adminRedirectUrl API Response is Not A Valid Json One");

				JSONObject response = new JSONObject(adminRedirectUrl.getApiResponse());
				String redirectUrl = response.getJSONObject("data").get("redirectUrl").toString();
				csAssert.assertTrue(redirectUrl != null &&
						!redirectUrl.isEmpty() &&
						redirectUrl.contains("https"), "Get adminRedirectURL API Response don't have valid redirectUrl");

				logger.info("*********************************************************************");

			}


		} catch (Exception e) {

			logger.debug("Error {} in Fetching the Config Details from config file--> {} ", e.getMessage(), docusignConfigFilePath + docusignConfigFileName);
		}


		csAssert.assertAll();

	}

	@AfterMethod
	public void afterMethod(ITestResult result) {
		logger.info("In After Method");
		logger.info("method name is: {}", result.getMethod().getMethodName());
		logger.info("***********************************************************************************************************************");


	}


	@AfterClass
	public void afterClass() {

		logger.info("In After Class method");
		csAssert.assertAll();
	}

}
