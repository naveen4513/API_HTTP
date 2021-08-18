package com.sirionlabs.test.serviceData;

import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
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
public class TestServiceDataUpdate {

	private final static Logger logger = LoggerFactory.getLogger(TestServiceDataUpdate.class);
	private String configFilePath = null;
	private String configFileName = null;
	private String extraFieldsConfigFilePath = null;
	private String extraFieldsConfigFileName = null;

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataUpdateConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataUpdateConfigFileName");
		extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
		extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
	}

	@DataProvider
	public Object[][] dataProviderForTestServiceDataUpdate() throws ConfigurationException {
		logger.info("Setting all Service Data Update Flows to Validate");
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
					logger.info("Flow having name [{}] not found in Service Data Update Config File.", flow.trim());
				}
			}
		}

		for (String flowToTest : flowsToTest) {
			allTestData.add(new Object[]{flowToTest});
		}
		return allTestData.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForTestServiceDataUpdate")
	public void testServiceDataUpdate(String flowToTest) {
		CustomAssert csAssert = new CustomAssert();

		try {
			logger.info("Validating Service Data Update Flow [{}]", flowToTest);
			int serviceDataId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "serviceDataId"));

			logger.info("Hitting Edit Get API for Service Data Id {} and Flow [{}]", serviceDataId, flowToTest);
			Edit editObj = new Edit();
			String editGetResponse = editObj.hitEdit("service data", serviceDataId);

			if (ParseJsonResponse.validJsonResponse(editGetResponse)) {
				verifyFieldsInEditGetResponse(flowToTest, editGetResponse, serviceDataId, csAssert);

				Map<String, String> fieldsPayloadMap = ParseConfigFile.getAllConstantPropertiesCaseSensitive(extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest);

				if (fieldsPayloadMap.size() > 0) {
					String editPostPayload = EntityOperationsHelper.createPayloadForEditPost(editGetResponse, fieldsPayloadMap);

					if (editPostPayload == null) {
						throw new SkipException("Couldn't get Edit Post Payload for Flow [" + flowToTest + "]");
					}

					logger.info("Hitting Edit Post API for Service Data Id {} and Flow [{}]", serviceDataId, flowToTest);
					String editPostResponse = editObj.hitEdit("service data", editPostPayload);

					if (ParseJsonResponse.validJsonResponse(editPostResponse)) {
						JSONObject jsonObj = new JSONObject(editPostResponse);
						String editStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
						String expectedResult = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedResult").trim();
						logger.info("Actual Result: {} and Expected Result: {} for Flow [{}]", editStatus, expectedResult, flowToTest);

						if (expectedResult.equalsIgnoreCase("success")) {
							if (!editStatus.equalsIgnoreCase("success")) {
								csAssert.assertTrue(false, "Couldn't update Service Data Id " + serviceDataId + " for Flow [" + flowToTest + "]");
							}
						} else {
							if (editStatus.equalsIgnoreCase("success")) {
								csAssert.assertTrue(false, "Service Data Id " + serviceDataId + " edited successfully for Flow [" + flowToTest +
										"] whereas it was expected to fail");
							}
						}

						//Restore Service Data to Original State
						if (editStatus.equalsIgnoreCase("success")) {
							if (EntityOperationsHelper.restoreRecord("service data", serviceDataId, editGetResponse)) {
								logger.info("Service Data Id {} restored Successfully.", serviceDataId);
							} else {
								logger.error("Couldn't Restore Service Data Id {}", serviceDataId);
							}
						}
					} else {
						csAssert.assertTrue(false, "Edit Post API Response for Service Data Id " + serviceDataId + " and Flow [" + flowToTest +
								"] is an Invalid JSON.");
					}
				} else {
					throw new SkipException("Couldn't get Fields Payload Map for Flow [" + flowToTest + "]");
				}
			} else {
				csAssert.assertTrue(false, "Edit Get API Response for Service Data Id " + serviceDataId + " and Flow [" + flowToTest +
						"] is an Invalid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Service Data Update for Flow [" + flowToTest + "]. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	private void verifyFieldsInEditGetResponse(String flowToTest, String editGetResponse, int serviceDataId, CustomAssert csAssert) {
		try {
			String contractDisplayMode = ParseJsonResponse.getFieldAttributeFromName(editGetResponse, "contract", "displayMode");

			if (contractDisplayMode == null) {
				throw new SkipException("Couldn't get Display Mode of Contract for Flow [" + flowToTest + "]");
			}

			if (!contractDisplayMode.trim().equalsIgnoreCase("display")) {
				csAssert.assertTrue(false, "Contract Field in Service Data Id " + serviceDataId + " is Editable for Flow [" + flowToTest + "]");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Fields In Edit Get Response for Flow [" + flowToTest + "]. " + e.getMessage());
		}
	}
}