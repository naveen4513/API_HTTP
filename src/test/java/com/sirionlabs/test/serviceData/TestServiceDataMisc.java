package com.sirionlabs.test.serviceData;

import com.sirionlabs.api.commonAPI.New;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.DownloadListWithData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.api.tabListData.GetTabListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.*;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.entityCreation.ChangeRequest;
import com.sirionlabs.helper.entityCreation.Contract;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.ServiceData;
import com.sirionlabs.helper.invoice.InvoicePricingHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.annotations.*;


import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Listeners(value = MyTestListenerAdapter.class)
public class TestServiceDataMisc {


	private final static Logger logger = LoggerFactory.getLogger(TestServiceDataMisc.class);
	static String csrfToken;
	static String outputFilePath;
	static String outputFileFormatForDownloadListWithData;
	CustomAssert csAssertion;
	boolean failedInConfigReading = false;
	int contractId = -1;
	int serviceDataIdForFixedFee = -1;
	int serviceDataIdForARCRRC = -1;
	int serviceDataIdForForecast = -1;
	String outputFileListDataDownload = null;
	String outputFilePathListDataDownload = null;
	String defaultClientServiceIdPrefix = "new client";
	String defaultSupplierServiceIdPrefix = "new supplier";
	private String contractConfigFilePath;
	private String contractConfigFileName;
	private String contractExtraFieldsConfigFileName;
	private String serviceDataConfigFilePath;
	private String serviceDataConfigFileName;
	private String serviceDataExtraFieldsConfigFileName;
	private String configFilePath;
	private String configFileName;
	private String serviceDataEntity = "service data";
	private String contractEntity = "contracts";
	private String entityIdMappingFileName;
	private String baseFilePath;
	private int serviceDataEntityTypeId;
	private int contractEntityTypeId;
	private int serviceDataEntityListId;
	private int randomCountToValidate = 5;

	private int chargesTabId = 309;
	private String ARCRRCSheetNameInXLSXFile = "ARCRRC";
	private Boolean failTestIfJobNotCompletedWithinSchedulerTimeOut = true;
	private String changeRequestConfigFilePath;
	private String changeRequestConfigFileName;
	private String changeRequestExtraFieldsConfigFileName;
	private String changeRequestEntity = "change requests";
	private final String changeRequestUrlName = "ccrs";
	private String pricingTemplateFilePath;

	public void getConfigData() {
		logger.info("Getting Test Data");


		try {

			outputFilePath = ConfigureConstantFields.getConstantFieldsProperty("DownloadListDataFilePath");
			outputFileFormatForDownloadListWithData = ConfigureConstantFields.getConstantFieldsProperty("DownloadListDataFileFormat");


			csrfToken = ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN");
			configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataMiscConfigFilePath");
			configFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataMiscConfigFileName");

			entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
			baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");

			//Contract Config files
			contractConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("contractFilePath");
			contractConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("contractFileName");
			contractExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("contractExtraFieldsFileName");

			//Service Data Config files
			serviceDataConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("serviceDataFilePath");
			serviceDataConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("serviceDataFileName");
			serviceDataExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataExtraFieldsFileName");

			//Change Request Config Files
			changeRequestConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ChangeRequestFilePath");
			changeRequestConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ChangeRequestFileName");
			changeRequestExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ChangeRequestExtraFieldsFileName");

			serviceDataEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, serviceDataEntity, "entity_type_id"));
			contractEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, contractEntity, "entity_type_id"));
			serviceDataEntityListId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, serviceDataEntity, "entity_url_id"));

			pricingTemplateFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "pricingtemplatefilepath");

			String serviceDataSectionNameForFixedFee = "service data misc fixed fee";
			String serviceDataSectionNameForARCRRC = "service data misc arc rrc";
			String serviceDataSectionNameForForecast = "service data misc forecast";

			//Create New Service Data for fixed fee
			String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "servicedatasectionnameforfixedfee");
			if (temp != null)
				serviceDataSectionNameForFixedFee = temp.trim();

			String createServiceDataResponse = createServiceData(contractId, serviceDataSectionNameForFixedFee, defaultClientServiceIdPrefix, defaultSupplierServiceIdPrefix);

			if (!createServiceDataResponse.toLowerCase().contains("Something Went Wrong".toLowerCase()))
				serviceDataIdForFixedFee = CreateEntity.getNewEntityId(createServiceDataResponse, serviceDataEntity);

			//Create New Service Data for ARC RRC
			temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "servicedatasectionnameforarcrrc");
			if (temp != null)
				serviceDataSectionNameForARCRRC = temp.trim();

			createServiceDataResponse = createServiceData(contractId, serviceDataSectionNameForARCRRC);

			if (!createServiceDataResponse.toLowerCase().contains("Something Went Wrong".toLowerCase()))
				serviceDataIdForARCRRC = CreateEntity.getNewEntityId(createServiceDataResponse, serviceDataEntity);

			//Create New Service for Forecast
			temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "servicedatasectionnameforforecast");
			if (temp != null)
				serviceDataSectionNameForForecast = temp.trim();

			createServiceDataResponse = createServiceData(contractId, serviceDataSectionNameForForecast);

			if (!createServiceDataResponse.toLowerCase().contains("Something Went Wrong".toLowerCase()))
				serviceDataIdForForecast = CreateEntity.getNewEntityId(createServiceDataResponse, serviceDataEntity);


			logger.info("Created Service Data Id for fixed fee: [{}]", serviceDataIdForFixedFee);
			logger.info("Created Service Data Id for ARR RRC Type: [{}]", serviceDataIdForARCRRC);
			logger.info("Created Service Data Id for ForeCast Type: [{}]", serviceDataIdForForecast);


		} catch (Exception e) {
			logger.error("Error while fetching Configuration detail at beforeClass Level " + e.getLocalizedMessage());
			failedInConfigReading = true;
		}


	}


	@BeforeClass
	public void beforeClass() throws IOException, ConfigurationException {
		logger.info("In Before Class method");
		getConfigData();

	}


	@BeforeMethod
	public void beforeMethod(Method method) {

		if (failedInConfigReading) {
			throw new SkipException("Skipping tests because Some Error while fetching Configuration detail at beforeClass Level");
		} else {

			logger.info("In Before Method");
			logger.info("method name is: {} ", method.getName());
			logger.info("----------------------------------------------------Test Starts Here-----------------------------------------------------------------------");
		}


	}


	private boolean downloadListDataForServiceData(Integer entityTypeId, String entityName, Integer listId, String payload) {

		Map<String, String> formParam = new HashMap<String, String>();

		formParam.put("jsonData", payload);
		formParam.put("_csrf_token", csrfToken);

		logger.info("formParam is : [{}]", formParam);

		DownloadListWithData downloadListWithData = new DownloadListWithData();
		logger.debug("Hitting DownloadListWithData for entity {} [typeid = {}] and [urlid = {}]", entityName, entityTypeId, listId);
		HttpResponse response = downloadListWithData.hitDownloadListWithData(formParam, listId);

		if (!response.getStatusLine().toString().contains("200")) {
			logger.error("Download List Data API Response Code is incorrect for service data");
			return false;
		} else {


			if (!dumpDownloadListWithDataResponseIntoFile(response, outputFilePath, "ListDataDownloadOutput", entityName, "TestServiceDataMisc")) {

				logger.error("Error While Dumping list data Download Response in CSV File for Service Data");
				return false;
			}
			return true;


		}


	}

	private boolean dumpDownloadListWithDataResponseIntoFile(HttpResponse response, String outputFilePath, String featureName, String entityName, String columnStatus) {

		boolean result = false;
		try {

			FileUtils fileUtil = new FileUtils();
			Boolean isFolderSuccessfullyCreated = fileUtil.createNewFolder(outputFilePath, featureName);
			Boolean isFolderWithEntityNameCreated = fileUtil.createNewFolder(outputFilePath + "/" + featureName + "/", entityName);
			if (isFolderSuccessfullyCreated && isFolderWithEntityNameCreated) {
				String outputFile = outputFilePath + "/" + featureName + "/" + entityName + "/" + columnStatus + outputFileFormatForDownloadListWithData;

				// for reading it back
				outputFilePathListDataDownload = outputFilePath + "/" + featureName + "/" + entityName;
				outputFileListDataDownload = columnStatus + outputFileFormatForDownloadListWithData;


				Boolean status = fileUtil.writeResponseIntoFile(response, outputFile);
				if (status) {
					logger.info("DownloadListWithData file generated at {}", outputFile);
					return true;
				}


			} else {
				logger.error("Error While Creating File or Folder for Dumping Download List Data Response");
				return result;
			}
		} catch (Exception e) {
			logger.error("Error While Dumping list data Download Response in CSV File");
			return result;
		}

		return result;
	}


	boolean isShowPageResponseHasCreateIssueLink(JSONObject showPageResponse) {

		boolean result = false;
		try {

			JSONObject createLinks = showPageResponse.getJSONObject("createLinks");
			JSONArray fields = createLinks.getJSONArray("fields");

			for (int i = 0; i < fields.length(); i++) {

				JSONObject field = fields.getJSONObject(i);
				if (field.has("label") && field.get("label").toString().toLowerCase().contentEquals("issue")) {
					if (field.has("api") && field.has("jspApi"))
						return true;
				}
			}

		} catch (Exception e) {

			logger.error("Error While Parsing Show Page Json Response of Service Data");
			return result;
		}

		return result;

	}


	private int getContractId() {
		int contractId = -1;
		try {

			String contractSectionName = "";
			//Create New Service Data
			String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "contractsectionname");
			if (temp != null)
				contractSectionName = temp.trim();

			//Create New Contract
			Boolean createLocalContract = true;
			String createResponse = Contract.createContract(contractConfigFilePath, contractConfigFileName, contractConfigFilePath,
					contractExtraFieldsConfigFileName, contractSectionName, createLocalContract);

			contractId = CreateEntity.getNewEntityId(createResponse, "contracts");

		} catch (Exception e) {
			logger.error("Exception while getting Contract Id  {}", e.getStackTrace());
		}
		return contractId;
	}

	// helper method for Creating Service Data
	private String createServiceData(int contractId, String sectionName, String clientServiceIdPrefix, String SupplierServiceIdPrefix) {
		String response = "";
		try {

			int randomId = RandomNumbers.getRandomNumberWithinRangeIndex(100000,999999);

			//Update Service Data Extra Fields.
			UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, sectionName, "serviceIdClient",
					"new client", clientServiceIdPrefix + randomId);
			UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, sectionName, "serviceIdSupplier",
					"new supplier", SupplierServiceIdPrefix + randomId);

			Boolean createLocalServiceData = true;


			response = ServiceData.createServiceData(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataConfigFilePath,
					serviceDataExtraFieldsConfigFileName, sectionName, createLocalServiceData);


			//Revert Service Data Extra Fields changes.
			UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, sectionName, "serviceIdClient",
					clientServiceIdPrefix + randomId, "new client");
			UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataExtraFieldsConfigFileName, sectionName, "serviceIdSupplier",
					SupplierServiceIdPrefix + randomId, "new supplier");


		} catch (Exception e) {
			logger.error("Exception while hitting creating Service Data API with earlier Created Contract {}", e.getStackTrace());
			return "Something Went Wrong";
		}
		return response;
	}

	// helper method for Creating Service Data
	private String createServiceData(int contractId, String sectionName) {
		return createServiceData(contractId, sectionName, sectionName, sectionName);
	}


	boolean isButtonAvailable(String stringResponse, String buttonName) {

		try {
			JSONObject response = new JSONObject(stringResponse);
			JSONArray actions = response.getJSONObject("body").getJSONObject("layoutInfo").getJSONArray("actions");


			for (int i = 0; i < actions.length(); i++) {
				JSONObject action = actions.getJSONObject(i);

				if (action.has("type") && action.get("type").toString().toLowerCase().contentEquals("button")
						&& action.has("label") && action.get("label").toString().toLowerCase().contentEquals(buttonName.toLowerCase())) {
					return true;
				}

			}


		} catch (Exception e) {
			logger.error("Error While Parsing create new API response for service data Creation");
			return false;
		}


		return true;

	}


	boolean isTabAvailable(String stringResponse, String tabName) {

		try {
			JSONObject response = new JSONObject(stringResponse);
			JSONObject layoutComponent = response.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent");

			JSONArray allTabDetails = layoutComponent.getJSONArray("fields");
			for (int i = 0; i < allTabDetails.length(); i++) {
				JSONObject tabDetail = allTabDetails.getJSONObject(i);

				if (tabDetail.has("label") && tabDetail.get("label").toString().toLowerCase().contentEquals(tabName.toLowerCase())) {
					return true;
				}

			}


		} catch (Exception e) {
			logger.error("Error While Parsing Show Page Response of Service Data");
			return false;
		}


		return true;

	}

	private boolean isPricingAvailableIsTrue(Integer dbId) {

		boolean result = false;
		try {

			Show showObj = new Show();
			showObj.hitShow(serviceDataEntityTypeId, dbId);
			JSONObject showJsonResponse = new JSONObject(showObj.getShowJsonStr());

			result = showJsonResponse.getJSONObject("body").getJSONObject("data").getJSONObject("pricingAvailable").getBoolean("values");

		} catch (Exception e) {
			logger.error("Error While Parsing show page response for DB Id [{}] of Service data [{}]", dbId, e.getLocalizedMessage());
		}

		return result;
	}


	@Test(enabled = false) //no use as service data doesn't have create link
	public void TestTC84024() {
		contractId = getContractId();
		logger.info("Contract Id is : [{}]", contractId);
		try {
			if (contractId != -1) {

				if (serviceDataIdForFixedFee != -1) {
					Show showObj = new Show();
					showObj.hitShow(serviceDataEntityTypeId, serviceDataIdForFixedFee);
					JSONObject showJsonResponse = new JSONObject(showObj.getShowJsonStr());
					Assert.assertTrue(isShowPageResponseHasCreateIssueLink(showJsonResponse), "Service Data is not having Create Issue link in quick link");
				} else {
					logger.error("Error While Creating Service Data as pre-requisite");
					throw new SkipException("Skipping this tests");
				}


			} else {

				logger.error("Error While Creating Contracts as pre-requisite");
				throw new SkipException("Skipping this tests");

			}
		} catch (Exception e) {
			logger.error("Some Exception while executing this test : [{}]", e.getLocalizedMessage());
			throw new SkipException("Skipping this tests");
		}

	}

	// Test Rail Test Case
	@Test(dependsOnMethods = "TestTC84024",enabled = false) //as required false for dependency
	public void TestC3599() {
		CustomAssert csAssert = new CustomAssert();


		try {
			if (contractId != -1) {
				String serviceDataSectionName = "";

				//Create New Service Data
				String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "servicedatasectionname");
				if (temp != null)
					serviceDataSectionName = temp.trim();

				String createServiceDataResponse = createServiceData(contractId, serviceDataSectionName, defaultClientServiceIdPrefix, serviceDataSectionName);

				if (!createServiceDataResponse.isEmpty() && !createServiceDataResponse.contentEquals("Something Went Wrong")
						&& APIUtils.validJsonResponse(createServiceDataResponse)) {


					JSONObject response = new JSONObject(createServiceDataResponse);
					JSONArray genericErrors = response.getJSONObject("body").getJSONObject("errors").getJSONArray("genericErrors");


					for (int i = 0; i < genericErrors.length(); i++) {

						JSONObject genericError = genericErrors.getJSONObject(i);

						if (genericError.has("message") && genericError.getString("message").toLowerCase().contains("Service Id Client already used in system".toLowerCase())) {
							logger.debug("Correct Error Message for this scenario");
							break;
						} else {
							csAssert.assertTrue(false, "Test Case failed when creating service data with existing [Service Id Client] in System : Validation Message Erro ");
						}


					}


					logger.debug("createServiceDataResponse is : [{}]", createServiceDataResponse);

				} else {
					logger.error("there is some issue while hitting create Service Data API");
					csAssert.assertTrue(false, "there is some issue while hitting create Service Data API with existing [Service Id Client] in payload ");
				}

			} else {
				logger.error("Error While Creating Contract as pre-requisite");
				throw new SkipException("Skipping this tests");
			}

		} catch (Exception e) {
			logger.error("Some Exception while executing this method : [{}]", e.getLocalizedMessage());

		}

		csAssert.assertAll();
	}

	// Test Rail Test Case
	@Test(dependsOnMethods = "TestTC84024",enabled = false)//as required false for dependency
	public void TestC3600() {
		CustomAssert csAssert = new CustomAssert();


		try {
			if (contractId != -1) {
				String serviceDataSectionName = "";

				//Create New Service Data
				String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "servicedatasectionname");
				if (temp != null)
					serviceDataSectionName = temp.trim();

				String createServiceDataResponse = createServiceData(contractId, serviceDataSectionName, serviceDataSectionName, defaultSupplierServiceIdPrefix);

				if (!createServiceDataResponse.isEmpty() && !createServiceDataResponse.contentEquals("Something Went Wrong")
						&& APIUtils.validJsonResponse(createServiceDataResponse)) {


					JSONObject response = new JSONObject(createServiceDataResponse);
					JSONArray genericErrors = response.getJSONObject("body").getJSONObject("errors").getJSONArray("genericErrors");


					for (int i = 0; i < genericErrors.length(); i++) {

						JSONObject genericError = genericErrors.getJSONObject(i);

						if (genericError.has("message") && genericError.getString("message").toLowerCase().contains("Service Id Supplier already used in system".toLowerCase())) {
							logger.debug("Correct Error Message for this scenario");
							break;
						} else {
							csAssert.assertTrue(false, "Test Case failed when creating service data with existing [Service Id Supplier] in System : Validation Message Erro ");
						}


					}

					logger.debug("createServiceDataResponse is : [{}]", createServiceDataResponse);

				} else {
					logger.error("there is some issue while hitting create Service Data API");
					csAssert.assertTrue(false, "there is some issue while hitting create Service Data API with existing [Service Id Supplier] in payload ");
				}

			} else {
				logger.error("Error While Creating Contract as pre-requisite");
				throw new SkipException("Skipping this tests");
			}

		} catch (Exception e) {
			logger.error("Some Exception while executing this method : [{}]", e.getLocalizedMessage());

		}

		csAssert.assertAll();
	}

	// Test Rail Test Case
	@Test(dependsOnMethods = "TestTC84024",enabled = false)
	public void TestC3601() {
		CustomAssert csAssert = new CustomAssert();
		int createdServiceDataId = -1;


		try {
			if (contractId != -1) {


				String serviceDataSectionName = "";

				//Create New Service Data
				String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "servicedatasectionnameforfixedfee");
				if (temp != null)
					serviceDataSectionName = temp.trim();

				String createServiceDataResponse = createServiceData(contractId, serviceDataSectionName, defaultSupplierServiceIdPrefix, defaultClientServiceIdPrefix);
				if (!createServiceDataResponse.isEmpty() && !createServiceDataResponse.contentEquals("Something Went Wrong")
						&& APIUtils.validJsonResponse(createServiceDataResponse)) {

					createdServiceDataId = CreateEntity.getNewEntityId(createServiceDataResponse, serviceDataEntity);
					if (createdServiceDataId != -1) {
						logger.info("Created Service Data Id [{}]", createdServiceDataId);
					} else {
						logger.error("Not being able to create Service Data by putting [existing client service id in supplier service id] and [existing supplier service id in client service id]");
						csAssert.assertTrue(false, "Not being able to create Service Data by putting [existing client service id in supplier service id] and [existing supplier service id in client service id]");
					}

					logger.debug("createServiceDataResponse is : [{}]", createServiceDataResponse);

				} else {
					logger.error("there is some issue while hitting create Service Data API");
					csAssert.assertTrue(false, "there is some issue while hitting create Service Data putting [existing client service id in supplier service id] and [existing supplier service id in client service id] ");
				}


			} else {
				logger.error("Error While Creating Contract as pre-requisite");
				throw new SkipException("Skipping this tests");
			}

		} catch (Exception e) {
			logger.error("Some Exception while executing this method : [{}]", e.getLocalizedMessage());
			throw new SkipException("Skipping this tests");

		} finally {
			// cleaning up
			// TC-C3819
			if (createdServiceDataId != -1) {
				boolean result = EntityOperationsHelper.deleteEntityRecord(serviceDataEntity, createdServiceDataId);
				csAssertion.assertTrue(result, "Error in Deleting service Data ");
			}

		}

		csAssert.assertAll();
	}

	// Test Rail Test Case
	@Test(dependsOnMethods = "TestTC84024",enabled = false)
	public void TestC2431() {

		csAssertion = new CustomAssert();
		if (contractId != -1) {
			New create = new New();
			create.hitNew(serviceDataEntity, contractEntity, contractId);
			String createPageResponse = create.getNewJsonStr();

			logger.debug("create Page Response is : [{}]", createPageResponse);

			boolean isResponseValidJson = APIUtils.validJsonResponse(createPageResponse, "[Create Page response(Get) for Service Data]");
			csAssertion.assertTrue(isResponseValidJson, "Create Page Response(Get) is not valid for while creating service date with contract Id " + contractId);
			csAssertion.assertTrue(isButtonAvailable(createPageResponse, "next step"), "Create Page Response(Get) for Service Data doesn't have required  " + " [ Next Step ]" + " in It");
			csAssertion.assertTrue(isButtonAvailable(createPageResponse, "Cancel"), "Create Page Response(Get) for Service Data doesn't have required  " + " [ Next Step ]" + " in It");


		} else {
			logger.error("Error While Creating Contract as pre-requisite");
			throw new SkipException("Skipping this tests");
		}

		csAssertion.assertAll();
	}


	// Test Rail Test Case
	// TC C-3614
	@Test(enabled = true)
	public void TestTabsValidationInFixedFeeTypeOfServiceData() {

		csAssertion = new CustomAssert();
		if (serviceDataIdForFixedFee != -1) {


			Show showObj = new Show();
			showObj.hitShow(serviceDataEntityTypeId, serviceDataIdForFixedFee);

			boolean isResponseValidJson = APIUtils.validJsonResponse(showObj.getShowJsonStr(), "[show page response of service data for created fixed-fee type ]");


			Assert.assertTrue(isResponseValidJson, "[show page response of service data for created fixed-fee type ]");
			JSONObject showJsonResponse = new JSONObject(showObj.getShowJsonStr());

			csAssertion.assertTrue(isTabAvailable(showObj.getShowJsonStr(), "CHARGES"), "CHARGES TAb is not there after creating fixed-fee Type of Service data");

		} else {
			logger.error("Error While Creating service data (fixed-fee) as pre-requisite");
			throw new SkipException("Skipping this tests");
		}

		csAssertion.assertAll();
	}

	// Test Rail Test Case
	// TC C-3612 , TC C-3614 ,TC C-3615
	@Test(enabled = true)
	public void TestTabsValidationInARRRRCTypeOfServiceData() {

		csAssertion = new CustomAssert();
		if (serviceDataIdForARCRRC != -1) {


			Show showObj = new Show();
			showObj.hitShow(serviceDataEntityTypeId, serviceDataIdForARCRRC);

			boolean isResponseValidJson = APIUtils.validJsonResponse(showObj.getShowJsonStr(), "[show page response of service data for created ARR/RRC type ]");


			Assert.assertTrue(isResponseValidJson, "[show page response of service data for created ARR/RRC type ]");
			JSONObject showJsonResponse = new JSONObject(showObj.getShowJsonStr());

			csAssertion.assertTrue(isTabAvailable(showObj.getShowJsonStr(), "ARC/RRC"), "ARC/RRC TAb is not there after creating ARC/RRC Type of Service data");
			csAssertion.assertTrue(isTabAvailable(showObj.getShowJsonStr(), "CHARGES"), "CHARGES TAb is not there after creating ARC/RRC Type of Service data");
			csAssertion.assertTrue(isTabAvailable(showObj.getShowJsonStr(), "Consumption"), "Consumption TAb is not there after creating ARC/RRC Type of Service data");


		} else {
			logger.error("Error While Creating service data (ARC/RRC) as pre-requisite");
			throw new SkipException("Skipping this tests");
		}

		csAssertion.assertAll();
	}


	// Test Rail Test Case
	// TC C-3612 , TC C-3614 ,TC C-3615
	@Test(enabled = true)
	public void TestTabsValidationInForecastTypeOfServiceData() {
		csAssertion = new CustomAssert();
		if (serviceDataIdForForecast != -1) {


			Show showObj = new Show();
			showObj.hitShow(serviceDataEntityTypeId, serviceDataIdForForecast);

			boolean isResponseValidJson = APIUtils.validJsonResponse(showObj.getShowJsonStr(), "[show page response of service data for created forecast type ]");


			Assert.assertTrue(isResponseValidJson, "[show page response of service data for created forecast type ]");
			JSONObject showJsonResponse = new JSONObject(showObj.getShowJsonStr());

			csAssertion.assertTrue(isTabAvailable(showObj.getShowJsonStr(), "FORECAST"), "FORECAST TAb is not there after creating forecast/RRC Type of Service data");
			csAssertion.assertTrue(isTabAvailable(showObj.getShowJsonStr(), "CHARGES"), "CHARGES TAb is not there after creating forecast Type of Service data");
			csAssertion.assertTrue(isTabAvailable(showObj.getShowJsonStr(), "Consumption"), "Consumption TAb is not there after creating forecast Type of Service data");


		} else {
			logger.error("Error While Creating service data (Forecast) as pre-requisite");
			throw new SkipException("Skipping this tests");
		}

		csAssertion.assertAll();
	}

	@DataProvider
	public Object[][] dataProviderForCreatingServiceDataNegativeTestCases() throws ConfigurationException {
		logger.info("Setting all Section Name for Testing Negative Scenarios of service Data Creation ");
		List<Object[]> allTestData = new ArrayList<>();
		List<String> allSectionNameToTest = new ArrayList<>();

		String allSectionName[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "createservicedatanegativetestcasessectionname").split(Pattern.quote(","));
		for (String sectionName : allSectionName) {
			if (ParseConfigFile.containsSection(configFilePath, configFileName, sectionName.trim())) {
				allSectionNameToTest.add(sectionName.trim());
			} else {
				logger.error("Section having name [{}] not found in Service Data Misc Config File Name", sectionName.trim());
			}
		}

		for (String section : allSectionName) {
			allTestData.add(new Object[]{section});
		}
		return allTestData.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForCreatingServiceDataNegativeTestCases", dependsOnMethods = "TestTC84024",enabled = false)
	public void testServiceDataCreationNegativeFlow(String sectionName) {
		CustomAssert csAssert = new CustomAssert();
		logger.info("Section Name is : [{}]", sectionName);

		try {
			if (contractId != -1) {
				String createServiceDataResponse = createServiceData(contractId, sectionName);


				if (!createServiceDataResponse.isEmpty() && !createServiceDataResponse.contentEquals("Something Went Wrong")
						&& APIUtils.validJsonResponse(createServiceDataResponse))

				{
					String keyInErrors = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName, "fieldvalidationkey");
					String errorMessage = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName, "expectederrormessage");
					String errorType = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName, "errortype");


					JSONObject response = new JSONObject(createServiceDataResponse);

					JSONObject fieldErrors = null;
					JSONArray genericErrors = null;


					// for generic Error , by default we will take it as field Error
					if (errorType != null && !errorType.isEmpty() && errorType.contentEquals("generic")) {
						genericErrors = response.getJSONObject("body").getJSONObject("errors").getJSONArray("genericErrors");

						boolean isErrorMessageExist = false;
						for (int i = 0; i < genericErrors.length(); i++) {

							JSONObject genericError = genericErrors.getJSONObject(i);


							if (genericError.has("message") && genericError.getString("message").toLowerCase().contains(errorMessage.toLowerCase()))

							{
								isErrorMessageExist = true;
								logger.debug("Correct Error Message for this scenario");
								break;
							}

						}

						if (!isErrorMessageExist) {
							csAssert.assertTrue(false, "Test Case Failed Since Expected Generic Error Message is not there while creating service data with Section Name ->" + sectionName);
						}


					}


					// for Field Errors
					else {
						fieldErrors = response.getJSONObject("body").getJSONObject("errors").getJSONObject("fieldErrors");

						if (fieldErrors.has(keyInErrors) && fieldErrors.getJSONObject(keyInErrors).getString("message").toLowerCase().contains(errorMessage.toLowerCase()))
							logger.debug("Correct Error Message for this scenario");
						else {
							csAssert.assertTrue(false, "Test Case failed for Section Name " + sectionName + "Where validation message is not there when " +
									keyInErrors + "is missing while creating service data");
						}

					}


					logger.debug("createServiceDataResponse is : [{}]", createServiceDataResponse);


				} else {
					logger.error("there is some issue while hitting create Service Data API");
					csAssert.assertTrue(false, "there is some issue while hitting create Service Data API");
				}

			} else {
				logger.error("Error While Creating Contract as pre-requisite");
				throw new SkipException("Skipping this tests");
			}

		} catch (Exception e) {
			logger.error("Some Exception while executing this method : [{}]", e.getLocalizedMessage());

		}

		csAssert.assertAll();
	}

	@Test (enabled = false) //will remain false
	public void TestTC84021() {

		String filterJson = "{\"filterMap\":{\"entityTypeId\":64,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"16\":{\"filterId\":\"16\",\"filterName\":\"regions\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1002\",\"name\":\"APAC\"}]}},\"18\":{\"filterId\":\"18\",\"filterName\":\"services\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1003\",\"name\":\"LAN\"}]}}}}}";
		Assert.assertTrue(downloadListDataForServiceData(serviceDataEntityTypeId, serviceDataEntity, serviceDataEntityListId, filterJson), "Error : While Downloading List Data for Service Data");


		logger.info("Hitting ListRendererListData");
		ListRendererListData listDataObj = new ListRendererListData();
		HttpResponse response = listDataObj.hitListRendererListData(serviceDataEntityListId, false,
				filterJson, null);
		JSONObject jsonResponse = new JSONObject(listDataObj.getListDataJsonStr());
		int filterCount = jsonResponse.getInt("filteredCount");
		logger.info("Filter Count from List Data Json Response is : [{}]", filterCount);


		try {

			XLSUtils xlsUtils = new XLSUtils(outputFilePathListDataDownload, outputFileListDataDownload);
			int totalRowCount = xlsUtils.getRowCount("Data");
			logger.info("Total number of rows in the XLS file is [{}]", totalRowCount);
			// we have to deduct 6 from totalRowCount because of additional rows in xls file
			Assert.assertTrue((totalRowCount - 6) == filterCount, "RU List Data Download XLS file is having the same number of records as displayed in Listing page with Same Filter ");


		} catch (Exception e) {
			logger.error("Error While Reading the Download XLS File [{}]", outputFilePathListDataDownload + "/" + outputFileListDataDownload);
			throw new SkipException("Skipping this test");

		}


	}

	@Test(enabled = true)
	public void TestC3509() {

		CustomAssert csAssert = new CustomAssert();
		String filterJson = "{\"filterMap\":{\"entityTypeId\":" + serviceDataEntityTypeId + ",\"offset\":0,\"size\":1000,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"244\":{\"filterId\":\"244\",\"filterName\":\"pricingAvailable\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"true\",\"name\":\"Yes\"}]}}}}}";

		logger.info("Hitting ListRendererListData");
		ListRendererListData listDataObj = new ListRendererListData();
		listDataObj.hitListRendererListData(serviceDataEntityListId, false,
				filterJson, null);

		boolean isResponseValidJson = APIUtils.validJsonResponse(listDataObj.getListDataJsonStr(), "[List page response of service data is not valid Json with pricing available as true filter ]");
		Assert.assertTrue(isResponseValidJson, "[List page response of service data is not valid Json with pricing available as true filter ]");

		JSONObject jsonResponse = new JSONObject(listDataObj.getListDataJsonStr());
		ArrayList<Integer> dbIds = ListDataHelper.getAllDbIdsFromListPageResponse(listDataObj.getListDataJsonStr());

		logger.debug("DB ids are : [{}]", dbIds);


		if (!dbIds.isEmpty()) {
			ArrayList<Integer> dbIdsToValidate = new ArrayList<>();


			int[] randomIndex = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, dbIds.size(), randomCountToValidate);
			for (int i = 0; i < randomIndex.length; i++) {
				dbIdsToValidate.add(dbIds.get(randomIndex[i]));
			}

			logger.info("dbIdsToValidate are : [{}]", dbIdsToValidate);

			for (Integer dbIdToValidate : dbIdsToValidate) {
				csAssert.assertTrue(isPricingAvailableIsTrue(dbIdToValidate), "Pricing Available Should be true for this db Id " + dbIdToValidate + " of Service Data");
			}


		} else {
			logger.info("There is no record in list data after applying Pricing available Filter as true , skipping this test");
			throw new SkipException("Skipping this test");
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
		if (contractId != -1)
			EntityOperationsHelper.deleteEntityRecord(contractEntity, contractId);

		// TC-C3819
		if (serviceDataIdForFixedFee != -1)
			EntityOperationsHelper.deleteEntityRecord(serviceDataEntity, serviceDataIdForFixedFee);


		if (serviceDataIdForARCRRC != -1)
			EntityOperationsHelper.deleteEntityRecord(serviceDataEntity, serviceDataIdForARCRRC);

		if (serviceDataIdForForecast != -1)
			EntityOperationsHelper.deleteEntityRecord(serviceDataEntity, serviceDataIdForForecast);

		logger.info("In After Class method");
	}

	@Test(enabled = false) //need to check
	public void testVersionDropdownOnChargesTab(){

		logger.info("Validating Version Dropdown on Charges Tab for Service Data");

		CustomAssert csAssert = new CustomAssert();
		ListRendererTabListData listRendererTabListData = new ListRendererTabListData();

		String flowToTest = "service data version change scenario";

		int changeRequestId = getChangeRequestId(flowToTest,csAssert);

		if(!(changeRequestId == -1)){

			String shortCodeIdChangeRequest = ShowHelper.getValueOfField(63,changeRequestId,"short code id");
			String titleChangeRequest = ShowHelper.getValueOfField(63,changeRequestId,"title");
			String changeRequestColumnValueToSelect = "(" + shortCodeIdChangeRequest + ") " + titleChangeRequest;

			int startingRowNumber = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"startingrownum"));
			int crColumnNum =  Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"crcolumnnum"));

			Map<Integer, Map<Integer, Object>> columnDataMap = new HashMap<>();
			Map<Integer, Object> columnMap = new HashMap<>();
			columnMap.put(crColumnNum,changeRequestColumnValueToSelect);
			columnDataMap.put(startingRowNumber,columnMap);

			if(!uploadPricingFileWithChangeRequest(flowToTest,serviceDataIdForARCRRC,columnDataMap,csAssert)){
				logger.error("Error while uploading pricing sheet on service data " + serviceDataIdForARCRRC);
				csAssert.assertTrue(false,"Error while uploading pricing sheet on service data " + serviceDataIdForARCRRC);
			}else {

				int chargesTabId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"chargestabid"));
				int filterId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"filterid"));
				int selectId;
				String selectOptionName;
				String filterName;

				ListRendererListData listRendererListData = new ListRendererListData();
				String listRendererFilterDataPayload = "{\"entityId\":" + serviceDataIdForARCRRC + "}";
				listRendererListData.hitListRendererFilterData(chargesTabId,listRendererFilterDataPayload);
				String listDataResponseFilterData  = listRendererListData.getListDataJsonStr();

				if(APIUtils.validJsonResponse(listDataResponseFilterData)){

					JSONObject listDataResponseFilterDataJSON = new JSONObject(listDataResponseFilterData);
					JSONArray filterOptionsDataArray = listDataResponseFilterDataJSON.getJSONObject(String.valueOf(filterId)).getJSONObject("multiselectValues").getJSONObject("OPTIONS").getJSONArray("DATA");
					filterName = listDataResponseFilterDataJSON.getJSONObject(String.valueOf(filterId)).get("filterName").toString();
					selectId = Integer.parseInt(filterOptionsDataArray.getJSONObject(0).get("id").toString());
					selectOptionName = filterOptionsDataArray.getJSONObject(0).get("name").toString();

					//Creating Filter Payload which is used to validate if data has come or not
					String filterPayload = createFilterJSON(filterId,filterName,selectId,selectOptionName);
					listRendererTabListData.hitListRendererTabListData(chargesTabId,serviceDataEntityTypeId,serviceDataIdForARCRRC,filterPayload);
					String listRendererTabListDataResponse = listRendererTabListData.getTabListDataJsonStr();

					if(!APIUtils.validJsonResponse(listRendererTabListDataResponse)){
						logger.error("listRendererTabListDataResponse is not a valid Json");
						csAssert.assertTrue(false,"listRendererTabListDataResponse is not a valid Json");
					}else {
						JSONObject tabListJson = new JSONObject(listRendererTabListDataResponse);
						if(tabListJson.getJSONArray("data").length() > 0){
							logger.info("After Version change data has been created after pricing upload");
						}else {
							logger.error("After Version change data is not created after pricing upload");
							csAssert.assertTrue(false,"After Version change data is not created after pricing upload");
						}
					}
				}else {
					logger.error("listDataResponseFilterData is not a Valid Json");
					csAssert.assertTrue(false,"listDataResponseFilterData is not a Valid Json");
				}
			}
		}else {
			logger.error("Change Request Id is null, Hence skipping the test");
		}
		csAssert.assertAll();
	}

	private int getChangeRequestId(String flowToTest,CustomAssert csAssert){

		String contractId = ShowHelper.getValueOfField(serviceDataEntityTypeId,serviceDataIdForARCRRC,"contractid");
		String contractName = ShowHelper.getValueOfField(contractEntityTypeId,Integer.parseInt(contractId),"contract");
		int changeRequestId;
		try {
			UpdateFile.updateConfigFileProperty(changeRequestConfigFilePath, changeRequestConfigFileName, flowToTest, "sourceid", contractId);
			UpdateFile.updateConfigFileProperty(changeRequestConfigFilePath, changeRequestConfigFileName, flowToTest, "sourcename", contractName);

			logger.info("Creating Change Request for contract ID " + contractId);

			String createResponse = ChangeRequest.createChangeRequest(changeRequestConfigFilePath, changeRequestConfigFileName, changeRequestConfigFilePath, changeRequestExtraFieldsConfigFileName, flowToTest,
					true);
			changeRequestId = CreateEntity.getNewEntityId(createResponse);

			String[] changeRequestActionsToPerform = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "changerequestactionstoperform").split("->");
			for(String action : changeRequestActionsToPerform) {

				if (new WorkflowActionsHelper().performWorkflowAction(ConfigureConstantFields.getEntityIdByName(changeRequestEntity),changeRequestId,action)) {
					csAssert.assertTrue(false, "Unable to perform " + changeRequestActionsToPerform + " on change request id " + changeRequestId);
					changeRequestId = -1;
					break;
				}
			}
		}catch (Exception e){
			logger.error("Exception while getting change Request ID " + e.getStackTrace());
			csAssert.assertTrue(false,"Exception while getting change Request ID " + e.getStackTrace());
			changeRequestId = -1;
		}
		return changeRequestId;
	}

	private Boolean uploadPricingFileWithChangeRequest(String flowToTest,int serviceDataId,Map<Integer, Map<Integer, Object>> columnDataMap,CustomAssert csAssert ) {

		InvoicePricingHelper pricingObj = new InvoicePricingHelper();
		String serviceDataType = "";
		Boolean pricingUploadStatus = false;

		String pricingTemplateFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
				"pricingstemplatefilename");

		Boolean pricingFile = pricingObj.downloadAndEditPricingFile(pricingTemplateFilePath,pricingTemplateFileName,configFilePath,configFileName, flowToTest, serviceDataId, pricingObj);

		Map<Integer, Map<Integer, Object>> arcValuesMap = getValuesMapForArcRrcSheet(flowToTest);

		// changes for ARC RRC FLOW
		if (pricingFile) {

			// getting the actual service data Type
			if (ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
					"servicedatatype") != null) {
				serviceDataType = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
						"servicedatatype");
			}

			if (serviceDataType.contentEquals("arc") || serviceDataType.contentEquals("rrc")) {
				if(pricingObj.editPricingFileForARCRRC(pricingTemplateFilePath,pricingTemplateFileName, flowToTest,ARCRRCSheetNameInXLSXFile,arcValuesMap, serviceDataId))
				{
					pricingFile = XLSUtils.editMultipleRowsData(pricingTemplateFilePath,pricingTemplateFileName,"Data",columnDataMap);
				}else {
					pricingFile = false;
				}

			}

			if (pricingFile) {

				logger.info("Hitting Fetch API.");
				Fetch fetchObj = new Fetch();
				fetchObj.hitFetch();
				List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

				String pricingUploadResponse = pricingObj.uploadPricing(pricingTemplateFilePath, pricingTemplateFileName);

				if (pricingUploadResponse != null && pricingUploadResponse.trim().contains("Your request has been successfully submitted")) {
					String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "waitforpricingscheduler");
					if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
						//Wait for Pricing Scheduler to Complete
						String pricingSchedulerStatus = pricingObj.waitForPricingScheduler(flowToTest, allTaskIds);

						if (pricingSchedulerStatus.trim().equalsIgnoreCase("fail")) {
							logger.error("Pricing Upload Task failed. Hence skipping further validation for Flow [{}]", flowToTest);
							csAssert.assertTrue(false, "Pricing Upload Task failed. Hence skipping further validation for Flow [" +
									flowToTest + "]");

							return false;
						} else if (pricingSchedulerStatus.trim().equalsIgnoreCase("skip")) {
							logger.info("Pricing Upload Task didn't complete in specified time. Hence skipping further validation for Flow [{}]", flowToTest);
							if (failTestIfJobNotCompletedWithinSchedulerTimeOut) {
								logger.info("FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. Hence failing Flow [{}]", flowToTest);
								csAssert.assertTrue(false, "FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned on. " +
										"Hence failing Flow [" + flowToTest + "]");
							} else {
								logger.info("FailTestIfJobNotCompletedWithinSchedulerTimeOut Flag is turned off. Hence not failing Flow [{}]", flowToTest);
							}
							return false;
						} else{
							boolean isDataCreatedUnderChargesTab = isChargesCreated(serviceDataId);

							if (!isDataCreatedUnderChargesTab) {
								csAssert.assertTrue(isDataCreatedUnderChargesTab, "no data in Charges tab is getting created. Hence skipping further validation for Flow [" +
										flowToTest + "]");

								return false;
							}else {
								pricingUploadStatus = true;
							}
						}
					}
				}
			}
		}
		return pricingUploadStatus;
	}

	private String createFilterJSON(int filterId,String filterName,int id,String idName){

		String filterPayload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":20,\"filterJson\":{\"" + filterId +"\":{\"entityFieldHtmlType\":null,\"entityFieldId\":null,\"filterId\":" + filterId + ",\"filterName\":\"" + filterName + "\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + id +"\",\"name\":\"" + idName + "\"}]}}},\"orderByColumnName\":\"enddate\",\"orderDirection\":\"desc nulls last\"}}";

		return filterPayload;
	}

	// this function will check whether any data has been created under Charges tab of Service Data or Not
	boolean isChargesCreated(int serviceDataId) {

		logger.info("Checking whether data under Charges tab has/have been created and visible for serviceData" + serviceDataId);
		GetTabListData getTabListData = new GetTabListData(serviceDataEntityTypeId, chargesTabId, serviceDataId);
		getTabListData.hitGetTabListData();
		String chargesTabListDataResponse = getTabListData.getTabListDataResponse();


		boolean isListDataValidJson = APIUtils.validJsonResponse(chargesTabListDataResponse, "[Charges tab list data response]");


		if (isListDataValidJson) {


			JSONObject jsonResponse = new JSONObject(chargesTabListDataResponse);
			int filterCount = jsonResponse.getInt("filteredCount");
			if (filterCount > 0) //very lenient check cab be modified
			{
				return true;
			} else {
				logger.error("There is no data in Charges tab for Service Data Id : [{}]", serviceDataId);
				return false;
			}


		} else {
			logger.error("Charges tab List Data Response is not valid Json for Service Data  Id :[{}] ", serviceDataId);
			return false;

		}
	}

	// this will create the row , <columnNumber,value> for editing the ARC/RRC Sheet
	private Map<Integer, Map<Integer, Object>> getValuesMapForArcRrcSheet(String flowToTest) {
		Map<Integer, Map<Integer, Object>> valuesMap = new HashMap<>();

		try {

			int numberOfColumnToEditForEachRow = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
					"numberofcolumntoeditforeachrowforarc"));

			String[] arcRowNumber = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
					"arcrownumber").trim().split(Pattern.quote(","));

			String[] arcColumnNumber = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
					"arccolumnnumber").trim().split(Pattern.quote(","));

			String[] values = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest,
					"arcvalue").trim().split(Pattern.quote(","));

			for (int i = 0; i < arcRowNumber.length; i++) {

				Map<Integer, Object> innerValuesMap = new HashMap<>();
				for (int j = 0; j < numberOfColumnToEditForEachRow; j++) {
					innerValuesMap.put(Integer.parseInt(arcColumnNumber[i * numberOfColumnToEditForEachRow + j]), values[i * numberOfColumnToEditForEachRow + j]);
				}
				valuesMap.put(Integer.parseInt(arcRowNumber[i]), innerValuesMap);
			}
		} catch (Exception e) {
			logger.error("Exception while getting Values Map for Pricing Sheet and Flow [{}]. {}", flowToTest, e.getStackTrace());
		}
		return valuesMap;
	}
}
