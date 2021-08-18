package com.sirionlabs.test.contract;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.entityCreation.ApplicationGroup;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Listeners(value = MyTestListenerAdapter.class)
public class TestApplicationGroup {

	private final static Logger logger = LoggerFactory.getLogger(TestApplicationGroup.class);
	private static String configFilePath = null;
	private static String configFileName = null;
	private static String extraFieldsConfigFilePath = null;
	private static String extraFieldsConfigFileName = null;
	private static Integer applicationGroupEntityTypeId;

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ApplicationGroupConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("ApplicationGroupConfigFileName");
		extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
		extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
		applicationGroupEntityTypeId = ConfigureConstantFields.getEntityIdByName("applicationgroups");
	}

	@DataProvider
	public Object[][] dataProviderForTestApplicationGroup() throws ConfigurationException {
		logger.info("Setting all Application Group Flows to Validate");
		List<Object[]> allTestData = new ArrayList<>();
		List<String> flowsToTest = new ArrayList<>();

		String allFlows[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowsToValidate").split(Pattern.quote(","));
		for (String flow : allFlows) {
			if (ParseConfigFile.containsSection(configFilePath, configFileName, flow.trim())) {
				flowsToTest.add(flow.trim());
			} else {
				logger.info("Flow having name [{}] not found in Application Group Config File.", flow.trim());
			}
		}

		for (String flowToTest : flowsToTest) {
			allTestData.add(new Object[]{flowToTest});
		}
		return allTestData.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForTestApplicationGroup")
	public void testApplicationGroup(String flowToTest) {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Validating Application Group Flow [{}]", flowToTest);

			//Validate Application Group Creation
			Map<String, String> properties = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileName, flowToTest);
			String creationFlow = properties.get("creationsection");

			logger.info("Creating Application Group for Flow [{}]", flowToTest);
			String createResponse = ApplicationGroup.createApplicationGroup(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName,
					creationFlow);
			String expectedResult = properties.get("expectedresult");

			if (ParseJsonResponse.validJsonResponse(createResponse)) {
				JSONObject jsonObj = new JSONObject(createResponse);
				String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
				logger.info("Create Status for Flow [{}]: {}", flowToTest, createStatus);

				if (expectedResult.trim().equalsIgnoreCase("success")) {
					if (createStatus.equalsIgnoreCase("success")) {
						Integer applicationGroupId = CreateEntity.getNewEntityId(createResponse, "applicationgroups");

						if (applicationGroupId == -1) {
							throw new SkipException("Couldn't get Id of Newly Created Application Group of Flow [" + flowToTest + "]");
						}

						//Verify Show Page Validations
						verifyShowPage(flowToTest, creationFlow, applicationGroupId, csAssert);

						//Delete Application Group
						logger.info("Deleting Application Group having Id {} for Flow [{}]", applicationGroupId, flowToTest);
						Boolean deleteApplicationGroup = EntityOperationsHelper.deleteEntityRecord("applicationgroups", applicationGroupId);

						if (!deleteApplicationGroup) {
							csAssert.assertTrue(false, "Couldn't delete Application Group having Id " + applicationGroupId + " for Flow [" +
									flowToTest + "]");
						} else {
							//Verify Show Page after Deletion
							verifyShowPageAfterDeletion(flowToTest, applicationGroupId, csAssert);
						}
					} else {
						csAssert.assertTrue(false, "Couldn't create Application Group for Flow [" + flowToTest +
								"] whereas it was expected to create successfully.");
					}
				} else {
					if (createStatus.equalsIgnoreCase("success")) {
						csAssert.assertTrue(false, "Application Group created successfully for Flow [" + flowToTest +
								"] whereas it was expected to fail.");
					}
				}
			} else {
				csAssert.assertTrue(false, "Create API Response for Credit EarnBack Creation Flow [" + flowToTest + "] is an Invalid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Credit EarnBack Creation Flow [" + flowToTest + "]. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	private void verifyShowPage(String flowToTest, String creationFlow, Integer applicationGroupId, CustomAssert csAssert) {
		try {
			logger.info("Verifying Show Page of Application Group Id {} and Flow [{}]", applicationGroupId, flowToTest);
			logger.info("Hitting Show Page API for Application Group Id {} and Flow [{}]", applicationGroupId, flowToTest);

			Show showObj = new Show();
			showObj.hitShow(applicationGroupEntityTypeId, applicationGroupId);
			String showResponse = showObj.getShowJsonStr();

			if (ParseJsonResponse.validJsonResponse(showResponse)) {
				//Verify Contract Url
				String contractUrl = ShowHelper.getValueOfField("contract url", showResponse);
				if (contractUrl == null) {
					throw new SkipException("Couldn't get Contract Url from Show Page Response of Application Group Id " + applicationGroupId + " and Flow [" +
							flowToTest + "]");
				}

				String expectedContractId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, creationFlow, "sourceId").trim();
				if (!contractUrl.trim().contains(expectedContractId)) {
					csAssert.assertTrue(false, "Contract Url doesn't contain the expected Contract Id in Show Page API Response of Application Group Id " +
							applicationGroupId + " and Flow [" + flowToTest + "]");
				}

				//Verify Contract is Non-Editable
				String contractDisplayMode = ParseJsonResponse.getFieldAttributeFromName(showResponse, "contract", "displayMode");
				if (contractDisplayMode == null) {
					throw new SkipException("Couldn't get Display Mode of Contract from Show Page API Response of Application Group Id " + applicationGroupId +
							" and Flow [" + flowToTest + "]");
				}

				if (!contractDisplayMode.trim().equalsIgnoreCase("display")) {
					csAssert.assertTrue(false, "Contract field of Application Group Id " + applicationGroupId +
							" is Editable whereas it was expected to be Non-Editable");
				}
			} else {
				csAssert.assertTrue(false, "Show Page API Response for Application Group Id " + applicationGroupId + " and Flow [" + flowToTest +
						"] is an Invalid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Show Page of Application Group Id " + applicationGroupId + " and Flow [" +
					flowToTest + ". " + e.getMessage());
		}
	}

	private void verifyShowPageAfterDeletion(String flowToTest, Integer applicationGroupId, CustomAssert csAssert) {
		try {
			logger.info("Verifying Show Page after Deletion for Application Group Id {} and Flow [{}]", applicationGroupId, flowToTest);
			logger.info("Hitting Show Page API after Deletion for Application Group Id {} and Flow [{}]", applicationGroupId, flowToTest);

			Show showObj = new Show();
			showObj.hitShow(applicationGroupEntityTypeId, applicationGroupId);
			String showResponse = showObj.getShowJsonStr();

			if (ParseJsonResponse.validJsonResponse(showResponse)) {
				JSONObject jsonObj = new JSONObject(showResponse);
				String status = jsonObj.getJSONObject("header").getJSONObject("response").getString("status");

				if (!status.trim().equalsIgnoreCase("applicationError")) {
					csAssert.assertTrue(false, "Show Page API Status doesn't match with Expected Status i.e. applicationError for Application Group Id " +
							applicationGroupId + " and Flow [" + flowToTest + "]");
				} else {
					String errorMessage = jsonObj.getJSONObject("header").getJSONObject("response").getString("errorMessage").trim();

					if (!errorMessage.toLowerCase().contains("has been deleted")) {
						csAssert.assertTrue(false, "Show Page API Error Message doesn't contain Expected Message i.e. [has been deleted]");
					}
				}
			} else {
				csAssert.assertTrue(false, "Show Page API Response after Deletion for Application Group Id " + applicationGroupId + " and Flow [" +
						flowToTest + "] is an Invalid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while Validating Show Page after Deletion for Application Group Id " + applicationGroupId +
					" and Flow [" + flowToTest + "]. " + e.getMessage());
		}
	}
}