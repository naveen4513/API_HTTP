package com.sirionlabs.test.integration;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Delete;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.integration.supplierWisdomNewModule.PublishSupplierWisdom;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityWorkFlowActionsHelper;
import com.sirionlabs.helper.EnvironmentHelper;
import com.sirionlabs.helper.entityCreation.Supplier;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TestSupplierWisdomNewModule {

	private final static Logger logger = LoggerFactory.getLogger(TestSupplierWisdomNewModule.class);

	private String supplierWisdomConfigFilePath;
	private String supplierWisdomConfigFileName;
	private List<String> allApiSpecificTestCases;
	private List<String> selectedApiSpecificTestCases;
	private Boolean runAllApiSpecificTestCases;
	private String integrationEnvFileName;
	private String masterSuiteEnvFileName;
	private String clientId;
	private String supplierId;
	private String entityId;
	private String entityTypeId;
	private Integer schedulerWaitingTime;
	private String[] supplierStatusToValidate;
	private Boolean createNewEntity;
	private Boolean deleteNewlyCreatedEntity;

	@BeforeClass
	public void setConfig() {
		try {
			supplierWisdomConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("SupplierWisdomNewModuleConfigFilePath");
			supplierWisdomConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("SupplierWisdomNewModuleConfigFileName");

			runAllApiSpecificTestCases = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(supplierWisdomConfigFilePath, supplierWisdomConfigFileName, "runallapispecifictestcases"));
			createNewEntity = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(supplierWisdomConfigFilePath, supplierWisdomConfigFileName, "createnewentity"));
			deleteNewlyCreatedEntity = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(supplierWisdomConfigFilePath, supplierWisdomConfigFileName, "deletenewlycreatedentity"));
			allApiSpecificTestCases = ParseConfigFile.getAllPropertiesOfSection(supplierWisdomConfigFilePath, supplierWisdomConfigFileName, "api specific test cases");

			integrationEnvFileName = ParseConfigFile.getValueFromConfigFile(supplierWisdomConfigFilePath, supplierWisdomConfigFileName, "integrationEnvFileName");
			clientId = ParseConfigFile.getValueFromConfigFile(supplierWisdomConfigFilePath, supplierWisdomConfigFileName, "clientid");
			supplierId = ParseConfigFile.getValueFromConfigFile(supplierWisdomConfigFilePath, supplierWisdomConfigFileName, "supplierid");
			entityId = ParseConfigFile.getValueFromConfigFile(supplierWisdomConfigFilePath, supplierWisdomConfigFileName, "entityid");
			entityTypeId = ParseConfigFile.getValueFromConfigFile(supplierWisdomConfigFilePath, supplierWisdomConfigFileName, "entitytypeid");
			schedulerWaitingTime = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(supplierWisdomConfigFilePath, supplierWisdomConfigFileName, "schedulerWaitingTime"));
			supplierStatusToValidate = ParseConfigFile.getValueFromConfigFile(supplierWisdomConfigFilePath, supplierWisdomConfigFileName, "supplierstatustovalidate").split(",");

			selectedApiSpecificTestCases = getPropertyList("selectedapispecifictestcases");

			masterSuiteEnvFileName = ConfigureEnvironment.environment;

		} catch (Exception e) {
			logger.error("Exception while setting config properties for Supplier Wisdom. error : {}", e.getMessage());
		}
	}

	@DataProvider(name = "getApiSpecificTestCasesToExecute", parallel = false)
	public Object[][] getApiSpecificTestCasesToExecute() throws ConfigurationException {

		int i = 0;
		Object[][] testCaseArray = null;
		List<String> testCaseList = new ArrayList<>();
		if (runAllApiSpecificTestCases) {
			testCaseArray = new Object[allApiSpecificTestCases.size()][];
			testCaseList = allApiSpecificTestCases;
		} else {
			testCaseArray = new Object[selectedApiSpecificTestCases.size()][];
			for (String temp : selectedApiSpecificTestCases) {
				testCaseList.add(temp);
			}
		}

		for (String entry : testCaseList) {
			testCaseArray[i] = new Object[1];
			testCaseArray[i][0] = entry; // sectionName
			i++;
		}
		return testCaseArray;
	}

	@DataProvider(name = "getSupplierStatusToValidate", parallel = false)
	public Object[][] getSupplierStatusToValidate() throws ConfigurationException {

		int i = 0;
		Object[][] statusArray = null;
		List<String> statusList = new ArrayList<>();

		statusArray = new Object[supplierStatusToValidate.length][];
		for (String temp : supplierStatusToValidate) {
			statusList.add(temp.trim());
		}


		for (String entry : statusList) {
			statusArray[i] = new Object[1];
			statusArray[i][0] = entry; // sectionName
			i++;
		}
		return statusArray;
	}

	@Test(testName = "verifyIntegrationServiceIsUpAndRunning", priority = 0, enabled = true)
	public void verifyIntegrationServiceIsUpAndRunning() {
		CustomAssert csAssert = new CustomAssert();

		try {
			newEnvironmentPropertySetup(integrationEnvFileName);
			String payload = "{\"processData\":{\"clientId\":\"" + clientId + "\",\"action\":\"SupplyWisdom\"},\"rawData\":{\"supplierId\":" + supplierId + ",\"entityId\":" + entityId + ",\"entityTypeId\":" + entityTypeId + "}}";
			PublishSupplierWisdom publishSupplierWisdom = new PublishSupplierWisdom();
			publishSupplierWisdom.hitPublishSupplierWisdom(payload);
			String statusCode = publishSupplierWisdom.getApiStatusCode();
			String response = publishSupplierWisdom.getPublishSupplierWisdomJsonStr();
			if (statusCode == null || !statusCode.equalsIgnoreCase("200") || !APIUtils.validJsonResponse(response, "publishSupplierWisdom api response")) {
				logger.error("Integration Service is not Up and Running. publish api status code : {}, response : {}", statusCode, response);
				csAssert.assertTrue(false, "Integration Service is not Up and Running. publish api status code : " + statusCode + ", response : " + response);
			}
		} catch (Exception e) {
			logger.error("Exception while verifying Integration server Running status. error : {}", e.getMessage());
			csAssert.assertTrue(false, "Exception while verifying Integration server Running status. error : " + e.getMessage());
		}
		csAssert.assertAll();
	}

	@Test(dataProvider = "getApiSpecificTestCasesToExecute", dependsOnMethods = "verifyIntegrationServiceIsUpAndRunning", priority = 1, enabled = true)
	public void testApiSpecificTestCases(String testScenario) {
		CustomAssert csAssert = new CustomAssert();

		logger.info("############################ Executing Test Case : {} #############################", testScenario);
		try {
			String payload = ParseConfigFile.getValueFromConfigFile(supplierWisdomConfigFilePath, supplierWisdomConfigFileName, "api specific test cases", testScenario);
			if (payload == null) {
				logger.error("payload is not defined for the test scenario : {}", testScenario);
				csAssert.assertTrue(false, "payload is not defined for the test scenario : " + testScenario);

			} else {
				PublishSupplierWisdom publishSupplierWisdom = new PublishSupplierWisdom();
				publishSupplierWisdom.hitPublishSupplierWisdom(payload);
				String response = publishSupplierWisdom.getPublishSupplierWisdomJsonStr();

				String actualStatusCode = publishSupplierWisdom.getApiStatusCode();
				String expectedStatusCode = getExpectedStatusCode(testScenario);
				logger.info("publishSupplierWisdom api StatusCode : {} , Response(test case : {}) : {}", testScenario, response);

				if (actualStatusCode.equals(expectedStatusCode)) {
					if (testScenario.equalsIgnoreCase("integrationservicewithcorrectparams")) {
						Boolean isValidJson = APIUtils.validJsonResponse(response, "publishSupplierWisdom api response");
						if (isValidJson) {
							JSONObject responseJson = new JSONObject(response);
							if (!responseJson.has("data")) {
								logger.error("publishSupplierWisdom api response does not contain data object. test case : {}", testScenario);
								csAssert.assertTrue(false, "publishSupplierWisdom api response does not contain data object. test case : " + testScenario);
							}
						} else {
							logger.error("publishSupplierWisdom api response is not valid json. test case : {}", testScenario);
							csAssert.assertTrue(false, "publishSupplierWisdom api response is not valid json. test case : " + testScenario);
						}
					}
				} else {
					logger.error("API Status Code does not match with expected status code. actual : {} but expected : {}. Test Scenario : {}", actualStatusCode, expectedStatusCode, testScenario);
					csAssert.assertTrue(false, "API Status Code does not match with expected status code. actual : " + actualStatusCode + " but expected : " + expectedStatusCode + ". Test Scenario : " + testScenario);
				}
			}
		} catch (Exception e) {
			logger.error("Exception while validating test case : {}, error = {}", testScenario, e.getMessage());
			e.printStackTrace();
			csAssert.assertTrue(false, "Exception while validating test case : " + testScenario + ", error : " + e.getMessage());
		}
		csAssert.assertAll();
	}

	@Test(dataProvider = "getSupplierStatusToValidate", dependsOnMethods = "verifyIntegrationServiceIsUpAndRunning", priority = 2, enabled = true)
	public void testSupplierWisdomFieldsOnShowPage(String status) {
		CustomAssert csAssert = new CustomAssert();
		logger.info("############################# VALIDATION STARTED FOR SUPPLIER STATUS : {} ###########################", status);
		try {
			String newSupplierDbId = null;
			Boolean newSupplierCreated = false;
			loggingOnEnv(masterSuiteEnvFileName);

			if (createNewEntity) {
				newSupplierDbId = createNewSupplier();
				if (newSupplierDbId != null) {
					newSupplierCreated = true;
					logger.info("Supplier created successfully for supplier status flow : {}. DbID : {}", status, newSupplierDbId);
				} else {
					logger.error("Entity: Supplier creation failed for the Supplier Status : {}", status);
					csAssert.assertTrue(false, "Entity: Supplier creation failed for the Supplier Status : " + status);
					csAssert.assertAll();
					return;
				}
			} else {
				newSupplierDbId = getExistingSupplierDbId(status);
				if (newSupplierDbId != null)
					logger.info("Validating for existing entity. DbId : {}", newSupplierDbId);
				else {
					logger.error("DbId not found in config file for the Supplier Status : {}", status);
					csAssert.assertTrue(false, "DbId not found in config file for the Supplier Status : " + status);
					csAssert.assertAll();
					return;
				}
			}
			if (!status.equalsIgnoreCase("newlycreated") && createNewEntity) {
				Boolean actionPerformed = performWorkflowAction(newSupplierDbId, status);
				if (!actionPerformed) {
					logger.error("Unable to perform workflow action for Supplier dbId : {}, Action : {}", newSupplierDbId, status);
					csAssert.assertTrue(false, "Unable to perform workflow action for Supplier dbId : " + newSupplierDbId + ", Action : " + status);
					csAssert.assertAll();
					return;
				}
			}

			/*waiting for scheduler to pick and update the supplier wisdom fields*/
			if (newSupplierCreated)
				waitForSchedulerToUpdateSupplierWisdomField();

			/*Validation of supplier wisdom fields on show page */
			validateSupplierWisdomFieldsOnShowPage(newSupplierDbId, status, csAssert);

			if (newSupplierCreated && deleteNewlyCreatedEntity) {
				logger.info("Deleting newly created entity. DbId {}", newSupplierDbId);
				deleteNewEntity("suppliers", Integer.parseInt(newSupplierDbId));
			}

		} catch (Exception e) {
			logger.error("Exception while validating supply wisdom data on Sirion entity show page. error : {}", e.getMessage());
			e.printStackTrace();
			csAssert.assertTrue(false, "Exception while validating supply wisdom data on Sirion entity show page. error : " + e.getMessage());
		}
		csAssert.assertAll();
	}

	private Boolean performWorkflowAction(String dbId, String status) {
		Boolean isPassed = true;

		try {
			String entityName = "suppliers";
			String entitySectionUrlName = "relations";
			String actionName = null;

			if (status.equalsIgnoreCase("inactive"))
				actionName = "inactivate";
			else if (status.equalsIgnoreCase("archive"))
				actionName = "archive";
			else if (status.equalsIgnoreCase("active"))
				actionName = "activate";

			isPassed = performAction(actionName, Integer.parseInt(dbId), entityName, entitySectionUrlName);
		} catch (Exception e) {
			logger.error("Exception while performing workflow action for Supplier DbId : {}, Status : {}", dbId, status);
			e.printStackTrace();
		}

		return isPassed;
	}

	private Boolean performAction(String actionName, Integer entityDbId, String entityName, String entitySectionUrlName) {
		Boolean result = true;
		try {
			if (actionName.toLowerCase().contentEquals("onhold")) {
				EntityWorkFlowActionsHelper.onHoldEntity(entityName, entityDbId, entitySectionUrlName);
			} else if (actionName.toLowerCase().contentEquals("archive")) {
				EntityWorkFlowActionsHelper.archiveEntity(entityName, entityDbId, entitySectionUrlName);
			} else {
				String workFlowOrderSequence = ParseConfigFile.getValueFromConfigFile(supplierWisdomConfigFilePath, supplierWisdomConfigFileName, entityName, actionName);
				if (workFlowOrderSequence != null) {

					if (!workFlowOrderSequence.equalsIgnoreCase("independent")) {

						String actions[] = workFlowOrderSequence.trim().split("->");

						logger.info("Setting pre-requisite for workFlow action : {} and entity : {}, workflow order sequence : {}", actionName, entityName, Arrays.asList(actions));
						for (String action : actions) {

							result = EntityWorkFlowActionsHelper.performActionGeneric(entityName, entityDbId, entitySectionUrlName, action);
							if (!result)
								break;
						}
					}

					if (!result) { // if any of the pre-requisite action got failed
						logger.info("Failed in Performing [{}] action on entityid  [{}] of entity [{}] because pre-requisite action got failed", actionName, entityDbId, entityName);
					} else {
						logger.info("Hitting workflow action :{} on entity : {}", actionName, entityName);
						result = EntityWorkFlowActionsHelper.performActionGeneric(entityName, entityDbId, entitySectionUrlName, actionName);
					}
				} else {
					result = false;
					logger.warn("workFlowOrderSequence not found for action : {} and entity : {} ", actionName, entityName);
				}
			}
		} catch (Exception e) {
			result = false;
			logger.error("Exception occurred in performAction method. error = {}", e.getMessage());
			e.printStackTrace();
		}
		return result;
	}

	private String getExistingSupplierDbId(String status) {
		String supplierDbId = null;

		try {
			Map<String, String> propertyMap = ParseConfigFile.getAllConstantProperties(supplierWisdomConfigFilePath, supplierWisdomConfigFileName, status);

			supplierDbId = propertyMap.get("dbid");
		} catch (Exception e) {
			logger.error("Exception while getting existing supplier dbId from config. error : {}", e.getMessage());
			e.printStackTrace();
		}
		return supplierDbId;
	}

	private void validateSupplierWisdomFieldsOnShowPage(String supplierDbId, String status, CustomAssert csAssert) {
		try {
			String showPageResponse = getShowResponse(supplierDbId);
			if (APIUtils.validJsonResponse(showPageResponse, "show api response")) {
				Map<String, String> supplierWisdomFieldMapping = ParseConfigFile.getAllConstantProperties(supplierWisdomConfigFilePath, supplierWisdomConfigFileName, "supplier wisdom fields");

				List<String> actualValues = getSupplierWisdomFieldActualValues(showPageResponse, supplierWisdomFieldMapping);

				if (status.equalsIgnoreCase("active") || status.equalsIgnoreCase("newlycreated")) {
					List<String> expectedValues = getSupplierWisdomFieldExpectedValues(supplierDbId);
					Boolean isSuccess = true;
					for (String expValue : expectedValues) {
						if (!actualValues.contains(expValue)) {
							isSuccess = false;
							break;
						}
					}
					if (isSuccess)
						logger.info("*#*#*#*#------ Validation passed for Supplier DbId : {}, Status : {} ---------- #*#*#*#*#", supplierDbId, status);
					else {
						logger.error("Supplier wisdom field value not matched. DbId : {}, supplier status : {}, actual : {} but expected : {}", supplierDbId, status, actualValues, expectedValues);
						csAssert.assertTrue(false, "Supplier wisdom field value not matched. DbId : " + supplierDbId + ", supplier status : " + status + ", actual : " + actualValues + " but expected : " + expectedValues);
					}
				} else if (status.equalsIgnoreCase("inactive") || status.equalsIgnoreCase("archive")) {

					if (createNewEntity) {
						boolean flag = true;

						for (String value : actualValues) {
							if (value != null)
								flag = false;
						}
						if (flag) {
							logger.info("Validation passed for Supplier DbId : {}, Status : {}", supplierDbId, status);
						} else {
							logger.error("Supplier wisdom field value gets populated for Supplier DbId : {} and Status : {} , actual values : {}", supplierDbId, status, actualValues);
							csAssert.assertTrue(false, "Supplier wisdom field value gets populated for Supplier DbId : " + supplierDbId + " and Status : " + status + ", actual values : " + actualValues);
						}
					} else {
						String fieldValue = ParseConfigFile.getAllConstantProperties(supplierWisdomConfigFilePath, supplierWisdomConfigFileName, status).get("fieldvalues");
						if (fieldValue == null) {
							logger.error("Supplier wisdom field values are not found in config file. section : {}, dbId : {}", status, supplierDbId);
							csAssert.assertTrue(false, "Supplier wisdom field values are not found in config file. section : " + status + ", dbId : " + supplierDbId);
						} else {
							String[] expValues = fieldValue.split(",");

							boolean flag = true;
							for (String value : expValues) {
								if (!actualValues.contains(value.trim())) {
									flag = false;
								}
							}
							if (flag)
								logger.info("********************** Validation passed for Supplier DbId : {}, Status : {} **************************", supplierDbId, status);
							else {
								logger.error("Supplier wisdom field value not matched. DbId : {}, supplier status : {}, actual : {} but expected : {}", supplierDbId, status, actualValues, Arrays.asList(expValues));
								csAssert.assertTrue(false, "Supplier wisdom field value not matched. DbId : " + supplierDbId + ", supplier status : " + status + ", actual : " + actualValues + " but expected : " + Arrays.asList(expValues));
							}
						}
					}
				} else {
					logger.error("Unknown status : {}", status);
					csAssert.assertTrue(false, "Unknown status : " + status);
				}
			} else {
				logger.error("Show API response is not valid json for dbId : {}, supplier status : {}", supplierDbId, status);
				csAssert.assertTrue(false, "Show API response is not valid json for dbId : " + supplierDbId + " , Supplier Status : " + status);
			}
		} catch (Exception e) {
			logger.error("Exception while validating supplier wisdom fields on show page. Supplier status : {}, error : {}", status, e.getMessage());
			e.printStackTrace();
			csAssert.assertTrue(false, "Exception while validating supplier wisdom fields on show page. Supplier status : " + status + " , error : " + e.getMessage());
		}
	}

	private List<String> getSupplierWisdomFieldExpectedValues(String supplierDbId) {
		List<String> expValues = new ArrayList<>();
		newEnvironmentPropertySetup(integrationEnvFileName);

		try {
			String payload = "{\"processData\":{\"clientId\":\"" + clientId + "\",\"action\":\"SupplyWisdom\"},\"rawData\":{\"supplierId\":" + supplierId + ",\"entityId\":" + supplierDbId + ",\"entityTypeId\":" + entityTypeId + "}}";
			PublishSupplierWisdom publishSupplierWisdom = new PublishSupplierWisdom();
			publishSupplierWisdom.hitPublishSupplierWisdom(payload);
			String response = publishSupplierWisdom.getPublishSupplierWisdomJsonStr();

			if (APIUtils.validJsonResponse(response, "publishSupplierWisdom api response")) {
				JSONObject jsonObject = new JSONObject(response);
				if (jsonObject.has("data")) {
					JSONArray dataArray = JSONUtility.convertJsonOnjectToJsonArray(jsonObject.getJSONObject("data"));
					for (int i = 0; i < dataArray.length(); i++) {
						expValues.add(dataArray.get(i).toString());
					}
				}
			}

		} catch (Exception e) {
			logger.error("Exception while getting expected values for the supplier wisdom fields. error : {}", e.getMessage());
			e.printStackTrace();
		}
		loggingOnEnv(masterSuiteEnvFileName);
		return expValues;
	}

	private List<String> getSupplierWisdomFieldActualValues(String showPageResponse, Map<String, String> supplierWisdomFieldMapping) {
		List<String> actualValues = new ArrayList<>();
		try {
			Show show = new Show();
			for (Map.Entry<String, String> entry : supplierWisdomFieldMapping.entrySet()) {
				String value = show.getValueOfDynamicField(showPageResponse, entry.getValue());
				actualValues.add(value);
			}
		} catch (Exception e) {
			logger.error("Exception while getting field values for supplier wisdom fields. error : {}", e.getMessage());
			e.printStackTrace();
		}

		return actualValues;
	}

	private String getShowResponse(String contractDbId) {
		String response = null;

		Show show = new Show();
		show.hitShow(Integer.parseInt(entityTypeId), Integer.parseInt(contractDbId));
		response = show.getShowJsonStr();

		return response;

	}

	private void waitForSchedulerToUpdateSupplierWisdomField() {
		try {
			logger.info("Waiting for {} sec", schedulerWaitingTime);
			Thread.sleep(schedulerWaitingTime * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	private String createNewSupplier() {
		String newSupplierDbId = null;

		try {
			String createApiResponse = Supplier.createSupplier("supplier wisdom", true);
			logger.info("Create api response : {}", createApiResponse);
			if (APIUtils.validJsonResponse(createApiResponse, "createApiResponse")) {
				JSONObject jsonRes = new JSONObject(createApiResponse);
				String status = jsonRes.getJSONObject("header").getJSONObject("response").getString("status");
				if (status.equalsIgnoreCase("success")) {
					String notification = jsonRes.getJSONObject("header").getJSONObject("response").getJSONObject("properties").getString("notification");
					//<a id=\"hrefElemId\" href=\"#/show/tblrelations/1300\" style=\"color: #9E866A; font-size: 11px;\">SP01183</a> created successfully.

					int startIndex = notification.indexOf("show");
					int endIndex = notification.indexOf("\"", startIndex);
					String temp = notification.substring(startIndex, endIndex); // show/tblrelations/1300
					newSupplierDbId = temp.split("/")[2].trim();
				}
			}
		} catch (Exception e) {
			logger.error("Exception while creting new supplier. error = {}", e.getMessage());
			e.printStackTrace();
		}
		return newSupplierDbId;
	}

	private String getExpectedStatusCode(String testScenario) {
		String expStatusCode = null;

		switch (testScenario) {
			case "integrationservicewithcorrectparams":
				expStatusCode = "200";
				break;
			case "integrationservicewithoutentitytypeid":
				expStatusCode = "200";
				break;
			case "integrationservicewithincorrectentitytypeid":
				expStatusCode = "200";
				break;
			case "integrationservicewithincorrectentityid":
				expStatusCode = "200";
				break;
			case "integrationservicewithincorrectsupplierid":
				expStatusCode = "404";
				break;

			default:
				expStatusCode = "400";

		}

		return expStatusCode;
	}

	private List<String> getPropertyList(String propertyName) throws ConfigurationException {
		String value = ParseConfigFile.getValueFromConfigFile(this.supplierWisdomConfigFilePath, this.supplierWisdomConfigFileName, propertyName);
		List<String> list = new ArrayList<String>();

		if (!value.trim().equalsIgnoreCase("")) {
			String properties[] = ParseConfigFile.getValueFromConfigFile(this.supplierWisdomConfigFilePath, this.supplierWisdomConfigFileName, propertyName).split(",");

			for (int i = 0; i < properties.length; i++)
				list.add(properties[i].trim());
		}
		return list;
	}

	private void newEnvironmentPropertySetup(String newEnv) {
		logger.info("Setting new Environment property. env : {}", newEnv);
		EnvironmentHelper.setEnvironmentProperties(newEnv);
	}

	private void loggingOnEnv(String environment) {
		try {
			logger.info("Logging on separate environment. env file name : {}", environment);

			ConfigureEnvironment.configureProperties(environment, true);

			Check checkObj = new Check();
			//Login on different env.
			checkObj.hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
			Assert.assertTrue(Check.getAuthorization() != null, "Login for vf integration Suite. Authorization not set by Check Api. Environment = " + environment);

		} catch (Exception e) {
			logger.error("Exception occurred while logging into alternate environment for vf integration. Error = {}", e.getMessage());
			e.printStackTrace();
		}
	}

	private void deleteNewEntity(String entityName, Integer entityId) {
		try {
			String urlName = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath"),
					ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile"), entityName, "url_name");
			logger.info("Hitting Show API for Entity {} having Id {}.", entityName, entityId);
			Show showObj = new Show();
			int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
			showObj.hitShow(entityTypeId, entityId);
			if (ParseJsonResponse.validJsonResponse(showObj.getShowJsonStr())) {
				JSONObject jsonObj = new JSONObject(showObj.getShowJsonStr());
				String prefix = "{\"body\":{\"data\":";
				String suffix = "}}";
				String showBodyStr = jsonObj.getJSONObject("body").getJSONObject("data").toString();
				String deletePayload = prefix + showBodyStr + suffix;

				logger.info("Deleting Entity {} having Id {}.", entityName, entityId);
				Delete deleteObj = new Delete();
				deleteObj.hitDelete(entityName, deletePayload, urlName);
				String deleteJsonStr = deleteObj.getDeleteJsonStr();
				jsonObj = new JSONObject(deleteJsonStr);
				String status = jsonObj.getJSONObject("header").getJSONObject("response").getString("status");
				if (status.trim().equalsIgnoreCase("success"))
					logger.info("Entity having Id {} is deleted Successfully.", entityId);
			}
		} catch (Exception e) {
			logger.error("Exception while deleting Entity {} having Id {}. {}", entityName, entityId, e.getStackTrace());
		}
	}

	@AfterClass
	public void afterClass() {
		loggingOnEnv(masterSuiteEnvFileName);
	}
}
