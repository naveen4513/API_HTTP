package com.sirionlabs.test.invoice;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityWorkFlowActionsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.*;

import java.io.IOException;
import java.lang.reflect.Method;

public class TestPublishServiceData {
	private final static Logger logger = LoggerFactory.getLogger(TestPublishServiceData.class);

	String entityIdMappingFileName, baseFilePath;
	String entityName = "service data";
	int entityTypeId = -1;
	String entitySectionUrlName;
	String publishAction = "publish";

	int serviceDataIdForFixedFee;
	int serviceDataIdForARCRRC;
	int serviceDataIdForForeCast;


	@BeforeClass
	public void beforeClass() throws IOException, ConfigurationException {
		logger.info("In Before Class method");
		getTestPublishServiceData();
	}

	@BeforeMethod
	public void beforeMethod(Method method) {
		logger.info("In Before Method");
		logger.info("method name is: {} ", method.getName());
		logger.info("----------------------------------------------------Test Starts Here-----------------------------------------------------------------------");

	}


	public void getTestPublishServiceData() throws ParseException, IOException, ConfigurationException {
		logger.info("Getting Test Data for EntityClickable Actions api");

		entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
		baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");

		entitySectionUrlName = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entityName, "url_name");
		entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entityName, "entity_type_id"));


		serviceDataIdForFixedFee = 18484;
		serviceDataIdForARCRRC = 18485;
		serviceDataIdForForeCast = 18484;


	}


	// Test Publish Button for Fixed Fee Type Service data
	@Test(priority = 0)
	public void testPublishServiceDataForFixedFeeType() throws Exception {
		CustomAssert csAssertion = new CustomAssert();


		if (serviceDataIdForFixedFee != -1 && ShowHelper.isActionCanBePerformed(entityTypeId,serviceDataIdForFixedFee,publishAction) ) {
			logger.info("###################################################:Test Starting for Fixed Fee Entity:{} ##################################################################", entityName);


			logger.info("Publishing the Service Data for DB ID [{}] which is of type Fixed Fee", serviceDataIdForFixedFee);


			boolean result = EntityWorkFlowActionsHelper.performAction(publishAction, serviceDataIdForFixedFee, entityName, entitySectionUrlName);
			csAssertion.assertTrue(result, "Not Being able to Perform  " + publishAction + " Action on " + entityName + "having id : " + serviceDataIdForFixedFee);


			logger.info("###################################################:Test Ending for Fixed Fee Entity:{}##################################################################", entityName);
		} else {

			if(serviceDataIdForFixedFee == -1) {
				logger.error("Service Data Creation is failing for Fixed Fee type of service data so Skipping this test");
				throw new SkipException("Skipping this Test");
			}
			else
			{
				logger.error("Service Data is not in Correct State so that Publish Action Can be Performed");
				throw new SkipException("Skipping this Test");
			}

		}
		csAssertion.assertAll();
	}


	// Test Publish Button for ARC/RRC Type Service data
	@Test(priority = 1)
	public void testPublishServiceDataForARCRRCType() throws Exception {
		CustomAssert csAssertion = new CustomAssert();

		if (serviceDataIdForARCRRC != -1 && ShowHelper.isActionCanBePerformed(entityTypeId,serviceDataIdForARCRRC,publishAction)) {
			logger.info("###################################################:Test Starting for ARC/RRC Entity:{}##################################################################", entityName);


			logger.info("Publishing the Service Data for DB ID [{}] which is of type ARC/RRC", serviceDataIdForARCRRC);


			boolean result = EntityWorkFlowActionsHelper.performAction(publishAction, serviceDataIdForARCRRC, entityName, entitySectionUrlName);
			csAssertion.assertTrue(result, "Not Being able to Perform  " + publishAction + " Action on " + entityName + "having id : " + serviceDataIdForARCRRC);


			logger.info("###################################################:Test Ending for ARC/RRC Entity:{}##################################################################", entityName);
		} else {
			if(serviceDataIdForARCRRC == -1) {
				logger.error("Service Data Creation is failing for Fixed Fee type of service data so Skipping this test");
				throw new SkipException("Skipping this Test");
			}
			else
			{
				logger.error("Service Data is not in Correct State so that Publish Action Can be Performed");
				throw new SkipException("Skipping this Test");
			}

		}
		csAssertion.assertAll();

	}


	// Test Publish Button for Forcast Type Service data
	@Test(priority = 2)
	public void testPublishServiceDataForForeCastType() throws Exception {
		CustomAssert csAssertion = new CustomAssert();



		if (serviceDataIdForForeCast != -1 && ShowHelper.isActionCanBePerformed(entityTypeId,serviceDataIdForForeCast,publishAction)) {
			logger.info("###################################################:Test Starting for Forecast Entity:{}##################################################################", entityName);


			logger.info("Publishing the Service Data for DB ID [{}] which is of type Forecast ", serviceDataIdForForeCast);


			boolean result = EntityWorkFlowActionsHelper.performAction(publishAction, serviceDataIdForForeCast, entityName, entitySectionUrlName);
			csAssertion.assertTrue(result, "Not Being able to Perform  " + publishAction + " Action on " + entityName + "having id : " + serviceDataIdForForeCast);


			logger.info("###################################################:Test Ending for Forecast Entity:{}##################################################################", entityName);
		} else {

			if(serviceDataIdForForeCast == -1) {
				logger.error("Service Data Creation is failing for Fixed Fee type of service data so Skipping this test");
				throw new SkipException("Skipping this Test");
			}
			else
			{
				logger.error("Service Data is not in Correct State so that Publish Action Can be Performed");
				throw new SkipException("Skipping this Test");
			}
		}
		csAssertion.assertAll();

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
	}


}
