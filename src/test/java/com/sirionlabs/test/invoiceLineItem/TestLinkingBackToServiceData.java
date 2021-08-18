package com.sirionlabs.test.invoiceLineItem;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.EntityWorkFlowActionsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.InvoiceLineItem;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
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
import java.util.regex.Pattern;

@Listeners(value = MyTestListenerAdapter.class)
public class TestLinkingBackToServiceData {

	private final static Logger logger = LoggerFactory.getLogger(TestLinkingBackToServiceData.class);
	private static String configFilePath = null;
	private static String configFileName = null;
	private static String extraFieldsConfigFilePath = null;
	private static String extraFieldsConfigFileName = null;
	private static Integer lineItemEntityTypeId;
	private static Integer serviceDataEntityTypeId;
	private static Long lineItemValidationTimeOut = 600000L;
	private static Long lineItemValidationPollingTime = 5000L;
	private static Boolean deleteEntity = true;

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("LineItemLinkingBackTestConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("LineItemLinkingBackTestConfigFileName");
		extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
		extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
		lineItemEntityTypeId = ConfigureConstantFields.getEntityIdByName("invoice line item");
		serviceDataEntityTypeId = ConfigureConstantFields.getEntityIdByName("service data");

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity");
		if (temp != null && temp.trim().equalsIgnoreCase("false"))
			deleteEntity = false;

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "lineItemValidationTimeOut");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			lineItemValidationTimeOut = Long.parseLong(temp.trim());

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "lineItemValidationPollingTime");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			lineItemValidationPollingTime = Long.parseLong(temp.trim());
	}

	@DataProvider
	public Object[][] dataProviderForTestLineItemLinkingBackToServiceData() throws ConfigurationException {
		logger.info("Setting all Invoice Line Item Linking Back to Service Data Flows to Validate");
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
					logger.info("Flow having name [{}] not found in Invoice Line Item Linking Back to Service Data Config File.", flow.trim());
				}
			}
		}

		for (String flowToTest : flowsToTest) {
			allTestData.add(new Object[]{flowToTest});
		}
		return allTestData.toArray(new Object[0][]);
	}

	@Test(dataProvider = "dataProviderForTestLineItemLinkingBackToServiceData")
	public void testLineItemLinkingBackToServiceData(String flowToTest) {
		CustomAssert csAssert = new CustomAssert();
		int lineItemId = -1;

		try {
			logger.info("Validating Invoice Line Item Linking Back to Service Data Flow [{}]", flowToTest);

			logger.info("Creating Invoice Line Item for Flow [{}]", flowToTest);
			String createResponse = InvoiceLineItem.createInvoiceLineItem(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest,
					true);

			if (ParseJsonResponse.validJsonResponse(createResponse)) {
				JSONObject jsonObj = new JSONObject(createResponse);
				String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();

				if (createStatus.trim().equalsIgnoreCase("success")) {
					lineItemId = CreateEntity.getNewEntityId(createResponse, "invoice line item");

					if (lineItemId != -1) {
						String invoicingType = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "invoicingType");
						if (invoicingType != null && invoicingType.trim().toLowerCase().contains("arc")) {
							//Approve ARC Line Item for Validation.

							if(!new WorkflowActionsHelper().performWorkFlowStepV2(165,lineItemId,"approve",csAssert)) {
								throw new SkipException("Couldn't Submit Invoice Line Item Id " + lineItemId + " for Approval. Hence skipping test.");
							}
						}

						//Validate url contains correct URL.
						validateServiceDataUrl(flowToTest, lineItemId, csAssert);

						//Validate Line Item Validation Status
//						Long timeSpent = 0L;
//						boolean lineItemValidated = false;
//
//						while (timeSpent <= lineItemValidationTimeOut) {
//							Show showObj = new Show();
//							showObj.hitShow(lineItemEntityTypeId, lineItemId);
//							String showResponse = showObj.getShowJsonStr();
//
//							if (ParseJsonResponse.validJsonResponse(showResponse)) {
//								if (ShowHelper.isLineItemUnderOngoingValidation(lineItemEntityTypeId, lineItemId)) {
//									logger.info("Invoice Line Item having Id {} is still undergoing Validation for Flow [{}]. Putting Thread on Sleep for {} milliseconds. " +
//													"Time Spent: [{}] milliseconds and Time Out: [{}] milliseconds", flowToTest, lineItemId, lineItemValidationPollingTime,
//											timeSpent, lineItemValidationTimeOut);
//
//									Thread.sleep(lineItemValidationPollingTime);
//									timeSpent += lineItemValidationPollingTime;
//								} else {
//									lineItemValidated = true;
//									break;
//								}
//							} else {
//								csAssert.assertTrue(false, "Show API Response for Invoice Line Item Id " + lineItemId + " is an Invalid JSON.");
//								break;
//							}
//						}
//
//						if (lineItemValidated) {
//							String actualValidationStatus = ShowHelper.getValueOfField(lineItemEntityTypeId, lineItemId, "validationStatus");
//							String expectedValidationStatus = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "expectedResult");
//
//							if (actualValidationStatus == null || !actualValidationStatus.trim().equalsIgnoreCase(expectedValidationStatus.trim())) {
//								csAssert.assertTrue(false, "Expected Validation Status: [" + expectedValidationStatus +
//										"] and Actual Validation Status: [" + actualValidationStatus + "] for Flow [" + flowToTest + "]");
//							}
//						} else {
//							throw new SkipException("Invoice Line Item Validation didn't Complete within Time Out [" + lineItemValidationTimeOut + "] milliseconds for Flow [" +
//									flowToTest + "] and Id " + lineItemId + ". Hence skipping test.");
//						}
					}
				} else {
					csAssert.assertTrue(false, "Couldn't create Invoice Line Item for Flow [" + flowToTest + "] due to " + createStatus);
				}
			} else {
				csAssert.assertTrue(false, "Create API Response for Invoice Line Item Flow [" + flowToTest + "] is an Invalid JSON.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Invoice Line Item Flow [" + flowToTest + "]. " + e.getMessage());
		} finally {
			if (deleteEntity && lineItemId != -1) {
				logger.info("Deleting Invoice Line Item having Id {}", lineItemId);
				EntityOperationsHelper.deleteEntityRecord("invoice line item", lineItemId);
			}
		}
		csAssert.assertAll();
	}

	private void validateServiceDataUrl(String flowToTest, int lineItemId, CustomAssert csAssert) {
		try {
			//Validate Service Data is hyperlinked.
			logger.info("Hitting Show API for Invoice Line Item Id {}", lineItemId);
			Show showObj = new Show();
			showObj.hitShow(lineItemEntityTypeId, lineItemId);
			String showResponse = showObj.getShowJsonStr();

			if (ParseJsonResponse.validJsonResponse(showResponse)) {
				JSONObject jsonObj = new JSONObject(showResponse);
				String serviceDataUrl = jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("serviceIdSupplier")
						.getJSONObject("values").getString("url");

				if (serviceDataUrl == null || serviceDataUrl.trim().equalsIgnoreCase("")) {
					csAssert.assertTrue(false, "Service Data is not HyperLinked in Show Page API Response for Line Item Id " + lineItemId +
							" and Flow [" + flowToTest + "]");
				} else {
					logger.info("Getting Service Data Id from URL for Flow [{}] and URL [{}]", flowToTest, serviceDataUrl);
					int serviceDataId = Integer.parseInt(serviceDataUrl.substring(serviceDataUrl.lastIndexOf("/") + 1).trim());

					logger.info("Hitting Show Page API for Service Data Id {}", serviceDataId);
					showObj.hitShow(serviceDataEntityTypeId, serviceDataId);
					showResponse = showObj.getShowJsonStr();

					if (ParseJsonResponse.validJsonResponse(showResponse)) {
						if (!showObj.isShowPageAccessible(showResponse)) {
							csAssert.assertTrue(false, "Show Page Not Accessible for Service Data Id " + serviceDataId);
						}
						else{
							logger.debug("Show page of the linked service data is accessible.");
						}
					} else {
						csAssert.assertTrue(false, "Show Page API Response for Service Data Id " + serviceDataId + " is an Invalid JSON.");
					}
				}
			} else {
				csAssert.assertTrue(false, "Show Page API Response for Invoice Line Item Id " + lineItemId + "and Flow [" + flowToTest +
						"] is an Invalid JSON.");
			}
		} catch (Exception e) {
			throw new SkipException("Exception while validating Service Data Url for Flow [" + flowToTest + "]. " + e.getMessage());
		}
	}
}