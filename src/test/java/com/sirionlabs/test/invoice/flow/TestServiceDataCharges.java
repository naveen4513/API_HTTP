package com.sirionlabs.test.invoice.flow;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.CurrencyConversionHelper;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.ServiceData;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.helper.invoice.InvoicePricingHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class TestServiceDataCharges {

	private final static Logger logger = LoggerFactory.getLogger(TestServiceDataCharges.class);

	private static String configFilePath = null;
	private static String configFileName = null;
	private static String extraFieldsConfigFilePath = null;
	private static String extraFieldsConfigFileName = null;
	private static String templateDownloadFilePath = null;
	private static String templateDownloadFileName = null;
	private static Integer volumeColumnNo;
	private static Integer rateColumnNo;
	private static String showDateFormat = null;
	private static String expectedDateFormat = null;
	private static Integer serviceDataChargesTabId = -1;
	private static Boolean deleteEntity = true;
	private static Long schedulerWaitTimeOut = 1200000L;
	private static Long schedulerPollingTime = 10000L;
	private static Boolean killAllTasks = true;
	private static Integer serviceDataEntityTypeId;
	private Integer serviceDataId = -1;

	@BeforeClass
	public void beforeClass() throws Exception {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataChargesTestConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataChargesTestConfigFileName");
		extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
		extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
		templateDownloadFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "pricingFileDownloadPath");
		templateDownloadFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "pricingFileDownloadName");

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity");
		if (temp != null && temp.trim().equalsIgnoreCase("false"))
			deleteEntity = false;

		volumeColumnNo = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "volumeColumnNo"));
		rateColumnNo = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rateColumnNo"));

		temp = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFilePath"),
				ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFileName"), "tabs mapping", "service data charges");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			serviceDataChargesTabId = Integer.parseInt(temp);

		serviceDataEntityTypeId = ConfigureConstantFields.getEntityIdByName("service data");

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "schedulerWaitTimeOut");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			schedulerWaitTimeOut = Long.parseLong(temp.trim());

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "schedulerPollingTime");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			schedulerPollingTime = Long.parseLong(temp.trim());

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "killAllTasks");
		if (temp != null && temp.trim().equalsIgnoreCase("false"))
			killAllTasks = false;

		showDateFormat = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "showDateFormat");
		expectedDateFormat = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "expectedDateFormat");
	}

	@DataProvider(name = "dataProviderForServiceDataChargesTest")
	public Object[][] getAllServiceDataChargesFlow() throws ConfigurationException {
		logger.info("Setting all Service Data Charges Flows to Validate");
		List<Object[]> allTestData = new ArrayList<>();
		List<String> flowsToTest = new ArrayList<>();

		if (serviceDataChargesTabId != -1) {
			String allFlows[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowsToValidate").split(Pattern.quote(","));
			for (String flow : allFlows) {
				if (ParseConfigFile.containsSection(configFilePath, configFileName, flow.trim())) {
					flowsToTest.add(flow.trim());
				} else {
					logger.info("Flow having name [{}] not found in Service Data Charges Config File.", flow.trim());
				}
			}

			for (String flowToTest : flowsToTest) {
				allTestData.add(new Object[]{flowToTest});
			}
		} else {
			logger.error("Couldn't get Service Data Charges Tab Id. Hence skipping all tests.");
		}
		return allTestData.toArray(new Object[0][]);
	}

	private Map<String, String> setAllPreRequisiteForTest(String flowToTest) {
		Map<String, String> preRequisiteStatus = new HashMap<>();
		Boolean preRequisitePass = false;
		String errorMessage = null;

		try {
			logger.info("Creating Service Data for Flow [{}].", flowToTest);

			//Create Service Data
			String creationSectionName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "creationSection");
			String jsonStr = InvoiceHelper.getServiceDataCreateResponseForExistingContract(configFilePath, configFileName, extraFieldsConfigFileName, creationSectionName);

			if (ParseJsonResponse.validJsonResponse(jsonStr)) {
				String responseStatus = ParseJsonResponse.getStatusFromResponse(jsonStr);

				if (responseStatus.trim().equalsIgnoreCase("success")) {
					serviceDataId = CreateEntity.getNewEntityId(jsonStr);

					if (serviceDataId != -1) {
						logger.info("Service Data created successfully for Flow [{}] with Id {}", flowToTest, serviceDataId);

						InvoicePricingHelper pricingObj = new InvoicePricingHelper();
						//Download Pricing Template
						Boolean downloadPricingTemplate = pricingObj.downloadPricingTemplate(templateDownloadFilePath, templateDownloadFileName, serviceDataId);

						if (downloadPricingTemplate) {
							Map<Integer, Object> valuesMap = new HashMap<>();
							Double volume = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "volumeValue"));
							Double rate = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "rateValue"));

							valuesMap.put(volumeColumnNo, volume);
							valuesMap.put(rateColumnNo, rate);

							String serviceDataType = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "serviceDataType");

							//Edit Pricing Template
							Boolean editTemplate = pricingObj.editPricingTemplate(templateDownloadFilePath, templateDownloadFileName, "pricing sheet", valuesMap);

							if (editTemplate) {
								if (serviceDataType.trim().equalsIgnoreCase("arc") || serviceDataType.trim().equalsIgnoreCase("rrc")) {
									valuesMap = getValuesMapForArcRrcSheet(flowToTest);
									editTemplate = pricingObj.editPricingTemplate(templateDownloadFilePath, templateDownloadFileName, "arc rrc sheet", valuesMap);
								}

								if (editTemplate) {
									logger.info("Hitting Fetch API");
									Fetch fetchObj = new Fetch();
									fetchObj.hitFetch();
									List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

									if (killAllTasks) {
										logger.info("KillAllTasks flag is Turned On. Killing All Tasks.");
										UserTasksHelper.removeAllTasks();
									}

									//Upload Pricing Template
									String uploadResponse = pricingObj.uploadPricing(templateDownloadFilePath, templateDownloadFileName);

									if (uploadResponse != null && uploadResponse.trim().contains("200:;")) {
										logger.info("Hitting Fetch API to get Pricing Task Id");
										fetchObj.hitFetch();

										int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

										//Wait For Scheduler to finish Pricing Consumption Task
										Map<String, String> pricingJob = UserTasksHelper.waitForScheduler(schedulerWaitTimeOut, schedulerPollingTime, newTaskId);

										if (pricingJob.get("jobPassed").trim().equalsIgnoreCase("true")) {
											preRequisitePass = true;
										} else {
											errorMessage = pricingJob.get("errorMessage");
										}
									} else {
										errorMessage = "Pricing Template Failed due to " + uploadResponse;
									}
								} else {
									errorMessage = "Couldn't edit ARC RRC Sheet";
								}
							} else {
								errorMessage = "Couldn't edit Pricing Template Sheet";
							}
						} else {
							errorMessage = "Couldn't download Pricing Template File at Location: [" + templateDownloadFilePath + "/" + templateDownloadFileName + "]";
						}
					} else {
						errorMessage = "Couldn't get Id of Newly Created Service Data";
					}
				} else {
					errorMessage = "Couldn't create Service Data due to " + responseStatus;
				}
			} else {
				errorMessage = "Service Data Creation API Response is an Invalid JSON.";
			}
		} catch (Exception e) {
			errorMessage = "Exception in Pre-Requisite stage. [" + e.getMessage() + "]";
		} finally {
			preRequisiteStatus.put("status", preRequisitePass.toString());
			preRequisiteStatus.put("errorMessage", errorMessage);
		}
		return preRequisiteStatus;
	}


	@Test(dataProvider = "dataProviderForServiceDataChargesTest")
	public void testServiceDataCharges(String flowToTest) {
		CustomAssert csAssert = new CustomAssert();

		try {
			Map<String, String> preRequisiteStatus = setAllPreRequisiteForTest(flowToTest);

			if (preRequisiteStatus.get("status") != null && !preRequisiteStatus.get("status").trim().equalsIgnoreCase("true")) {
				if (preRequisiteStatus.get("errorMessage") != null) {
					throw new SkipException("Pre-Requisite failed for Flow [" + flowToTest + "] due to [" + preRequisiteStatus.get("errorMessage") +
							"]. Hence skipping Service Data Charges validation.");
				} else {
					throw new SkipException("Pre-Requisite failed for Flow [" + flowToTest + "]. Hence skipping Service Data Charges Validation.");
				}
			}

			logger.info("Validating Service Data Charges for Flow [{}].", flowToTest);

			//Validate Charges Tab
			int noOfRows = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "noOfRows"));

			logger.info("Hitting TabListData API for Flow [{}] and Service Data Id {}", flowToTest, serviceDataId);
			TabListData tabObj = new TabListData();
			String tabListResponse = tabObj.hitTabListData(serviceDataChargesTabId, serviceDataEntityTypeId, serviceDataId);

			if (ParseJsonResponse.validJsonResponse(tabListResponse)) {
				Double volume = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "volumeValue"));
				Double rate = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "rateValue"));

				logger.info("Hitting Show API for Service Data Id {} and Flow [{}]", serviceDataId, flowToTest);
				Show showObj = new Show();
				showObj.hitShow(serviceDataEntityTypeId, serviceDataId);
				String serviceDataShowResponse = showObj.getShowJsonStr();

				if (ParseJsonResponse.validJsonResponse(serviceDataShowResponse)) {
					//Validate Start Date
					if (!validateServiceDataChargesStartDate(flowToTest, noOfRows, serviceDataShowResponse, tabListResponse)) {
						logger.error("Service Data Charges Tab Validation failed for Field Start Date and Flow [{}]", flowToTest);
						csAssert.assertTrue(false, "Service Data Charges Tab Validation failed for Field Start Date and Flow [" + flowToTest + "]");
					}

					//Validate End Date
					if (!validateServiceDataChargesEndDate(flowToTest, noOfRows, serviceDataShowResponse, tabListResponse)) {
						logger.error("Service Data Charges Tab Validation failed for Field End Date and Flow [{}]", flowToTest);
						csAssert.assertTrue(false, "Service Data Charges Tab Validation failed for Field End Date and Flow [" + flowToTest + "]");
					}

					//Validate Unit
					if (!validateServiceDataChargesUnitType(flowToTest, serviceDataShowResponse, tabListResponse)) {
						logger.error("Service Data Charges Tab Validation failed for Field Unit and Flow [{}]", flowToTest);
						csAssert.assertTrue(false, "Service Data Charges Tab Validation failed for Field Unit and Flow [" + flowToTest + "]");
					}

					//Validate Currency Short Code
					if (!validateServiceDataChargesCurrency(flowToTest, serviceDataShowResponse, tabListResponse)) {
						logger.error("Service Data Charges Tab validation failed for Field Currency and Flow [{}]", flowToTest);
						csAssert.assertTrue(false, "Service Data Charges Tab Validation failed for Field Currency and Flow [" + flowToTest + "]");
					}

					//Validate Volume
					if (!validateServiceDataChargesVolume(flowToTest, noOfRows, tabListResponse, volume)) {
						logger.error("Service Data Charges Tab validation failed for Field Volume and Flow [{}]", flowToTest);
						csAssert.assertTrue(false, "Service Data Charges Tab Validation failed for Field Volume and Flow [" + flowToTest + "]");
					}

					//Validate Rate
					if (!validateServiceDataChargesRate(flowToTest, noOfRows, tabListResponse, rate)) {
						logger.error("Service Data Charges Tab validation failed for Field Rate and Flow [{}]", flowToTest);
						csAssert.assertTrue(false, "Service Data Charges Tab Validation failed for Field Rate and Flow [" + flowToTest + "]");
					}

					//Validate Base Amount
					if (!validateServiceDataChargesBaseAmount(flowToTest, noOfRows, tabListResponse, rate * volume)) {
						logger.error("Service Data Charges Tab validation failed for Field Base Amount and Flow [{}]", flowToTest);
						csAssert.assertTrue(false, "Service Data Charges Tab Validation failed for Field Base Amount and Flow [" + flowToTest + "]");
					}

					//Validate Client Base Amount
					Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, flowToTest);

					if(flowProperties != null) {
						String inputCurrency = flowProperties.get("inputcurrency");
						String currencyConversionMatrix = flowProperties.get("currencyconversionmatrix");

						if (!validateServiceDataChargesClientBaseAmount(flowToTest, noOfRows, tabListResponse, currencyConversionMatrix, inputCurrency,
								rate * volume)) {
							logger.error("Service Data Charges Tab validation failed for Field Client Base Amount and Flow [{}]", flowToTest);
							csAssert.assertTrue(false, "Service Data Charges Tab Validation failed for Field Client Base Amount and Flow [" +
									flowToTest + "]");
						}
					}
				} else {
					logger.error("Show API Response for Service Data {} and Flow [{}] is an Invalid JSON.", serviceDataId, flowToTest);
					csAssert.assertTrue(false, "Show API Response for Service Data " + serviceDataId + " and Flow [" + flowToTest +
							"] is an Invalid JSON.");
				}
			} else {
				logger.error("TabListData API Response for Charges Tab of Service Data {} and Flow [{}] is an Invalid JSON.", serviceDataId, flowToTest);
				csAssert.assertTrue(false, "TabListData API Response for Charges Tab of Service Data " + serviceDataId + " and Flow [" +
						flowToTest + "] is an Invalid JSON.");
			}

		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			logger.error("Exception while validating Service Data Charges for Flow [{}]. {}", flowToTest, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while validating Service Data Charges for Flow [" + flowToTest + "]. " + e.getMessage());
		} finally {
			if (deleteEntity && serviceDataId != -1) {
				logger.info("Deleting Service Data having Id {} for Flow [{}]", serviceDataId, flowToTest);
				if (EntityOperationsHelper.deleteEntityRecord("service data", serviceDataId))
					logger.info("Service Data having Id {} deleted successfully.", serviceDataId);
				else
					logger.info("Service Data having Id {} couldn't be deleted.", serviceDataId);
			}
		}
		csAssert.assertAll();
	}

	private Map<Integer, Object> getValuesMapForArcRrcSheet(String flowToTest) {
		Map<Integer, Object> valuesMap = new HashMap<>();

		try {
			String sectionName = "arc rrc sheet details";
			Integer lowerLevelColumnNo = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName,
					"lowerLevelColumnNo"));
			Double lowerLevelValue = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName,
					"defaultLowerLevelValue"));

			Integer upperLevelColumnNo = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName,
					"upperLevelColumnNo"));
			Double upperLevelValue = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName,
					"defaultUpperLevelValue"));

			Integer rateColumnNo = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName,
					"rateColumnNo"));
			Double rateValue = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName,
					"defaultRateValue"));

			Integer typeColumnNo = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName,
					"typeColumnNo"));
			String typeValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName, "defaulTypeValue");

			valuesMap.put(lowerLevelColumnNo, lowerLevelValue);
			valuesMap.put(upperLevelColumnNo, upperLevelValue);
			valuesMap.put(rateColumnNo, rateValue);
			valuesMap.put(typeColumnNo, typeValue);
		} catch (Exception e) {
			logger.error("Exception while getting Values Map for ARC RRC Sheet and Flow [{}]. {}", flowToTest, e.getStackTrace());
		}
		return valuesMap;
	}

	private Boolean validateServiceDataChargesStartDate(String flowToTest, int noOfRows, String serviceDataShowResponse, String tabListDataResponse) {
		/*As of now we are intentionally creating Service Data of one month so that it has only one row in Excel Sheet.
		So we are fetching Expected Start Date from Service Data Show Page.
		In Future when we start validating Service Data having multiple rows in Excel Sheet then will have to modify this logic and
		fetch Expected Start Date from Excel Sheet only.
		 */
		Boolean startDateMatched = false;

		try {
			String showStartDate = ShowHelper.getValueOfField(64,"startDate", serviceDataShowResponse);
			Date date = new SimpleDateFormat(showDateFormat).parse(showStartDate);

			//Converting Show Start Date to Required Start Date Format
			String expectedStartDate = new SimpleDateFormat(expectedDateFormat).format(date);
			startDateMatched = TabListDataHelper.verifyServiceDataChargesStartDate(tabListDataResponse, expectedStartDate, expectedDateFormat, 0);
		} catch (Exception e) {
			logger.error("Exception while validating Start Date for Flow [{}]. {}", flowToTest, e.getStackTrace());
		}
		return startDateMatched;
	}

	private Boolean validateServiceDataChargesEndDate(String flowToTest, int noOfRows, String serviceDataShowResponse, String tabListDataResponse) {
		/*As of now we are intentionally creating Service Data of one month so that it has only one row in Excel Sheet.
		So we are fetching Expected End Date from Service Data Show Page.
		In Future when we start validating Service Data having multiple rows in Excel Sheet then will have to modify this logic and
		fetch Expected End Date from Excel Sheet only.
		 */
		Boolean endDateMatched = false;

		try {
			String showEndDate = ShowHelper.getValueOfField(64,"endDate", serviceDataShowResponse);
			Date date = new SimpleDateFormat(showDateFormat).parse(showEndDate);

			//Converting Display End Date to Required End Date Format
			String expectedEndDate = new SimpleDateFormat(expectedDateFormat).format(date);
			endDateMatched = TabListDataHelper.verifyServiceDataChargesEndDate(tabListDataResponse, expectedEndDate, expectedDateFormat, 0);
		} catch (Exception e) {
			logger.error("Exception while validating End Date for Flow [{}]. {}", flowToTest, e.getStackTrace());
		}
		return endDateMatched;
	}

	private Boolean validateServiceDataChargesUnitType(String flowToTest, String serviceDataShowResponse, String tabListDataResponse) {
		Boolean unitTypeMatched = false;

		try {
			String unitValue = ShowHelper.getValueOfField(64,"unit", serviceDataShowResponse);
			unitTypeMatched = TabListDataHelper.verifyServiceDataChargesUnitType(tabListDataResponse, unitValue, 0);
		} catch (Exception e) {
			logger.error("Exception while validating Unit Type for Flow [{}]. {}", flowToTest, e.getStackTrace());
		}
		return unitTypeMatched;
	}

	private Boolean validateServiceDataChargesCurrency(String flowToTest, String serviceDataShowResponse, String tabListDataResponse) {
		Boolean currencyMatched = false;

		try {
			ParseJsonResponse parseJsonResponse = new ParseJsonResponse();
			parseJsonResponse.getNodeFromJsonWithValues(new JSONObject(serviceDataShowResponse),Collections.singletonList("name"),"currency");//edited
			String currency = "";
			if(parseJsonResponse.getJsonNodeValue() instanceof String){
				currency = (String)parseJsonResponse.getJsonNodeValue();
			}
			else if(parseJsonResponse.getJsonNodeValue() instanceof JSONObject){
				currency = ((JSONObject) parseJsonResponse.getJsonNodeValue()).getString("shortName");
			}
			else{
				logger.error("object not String type <currency extraction error>");
			}
			String currencyShortCode = ShowHelper.getCurrencyShortCode(serviceDataShowResponse, serviceDataEntityTypeId);
			currencyMatched = TabListDataHelper.verifyServiceDataChargesCurrency(tabListDataResponse, currency, 0);
		} catch (Exception e) {
			logger.error("Exception while validating Currency for Flow [{}]. {}", flowToTest, e.getStackTrace());
		}
		return currencyMatched;
	}

	private Boolean validateServiceDataChargesVolume(String flowToTest, int noOfRows, String tabListDataResponse, Double expectedVolume) {
		/*As of now we are intentionally creating Service Data of one month so that it has only one row in Excel Sheet.
		So we are fetching Expected End Date from Service Data Show Page.
		In Future when we start validating Service Data having multiple rows in Excel Sheet then will have to modify this logic and
		fetch Expected End Date from Excel Sheet only.
		 */
		return TabListDataHelper.verifyServiceDataChargesVolume(tabListDataResponse, expectedVolume, 0);
	}

	private Boolean validateServiceDataChargesRate(String flowToTest, int noOfRows, String tabListDataResponse, Double expectedRate) {
		/*As of now we are intentionally creating Service Data of one month so that it has only one row in Excel Sheet.
		So we are fetching Expected End Date from Service Data Show Page.
		In Future when we start validating Service Data having multiple rows in Excel Sheet then will have to modify this logic and
		fetch Expected End Date from Excel Sheet only.
		 */
		return TabListDataHelper.verifyServiceDataChargesRate(tabListDataResponse, expectedRate, 0);
	}

	private Boolean validateServiceDataChargesBaseAmount(String flowToTest, int noOfRows, String tabListDataResponse, Double expectedBaseAmount) {
		/*As of now we are intentionally creating Service Data of one month so that it has only one row in Excel Sheet.
		So we are fetching Expected End Date from Service Data Show Page.
		In Future when we start validating Service Data having multiple rows in Excel Sheet then will have to modify this logic and
		fetch Expected End Date from Excel Sheet only.
		 */
		return TabListDataHelper.verifyServiceDataChargesBaseAmount(tabListDataResponse, expectedBaseAmount, 0);
	}

	private Boolean validateServiceDataChargesClientBaseAmount(String flowToTest, int noOfRows, String tabListDataResponse, String currencyConversionMatrixName,
	                                                           String inputCurrency, Double expectedBaseAmount) {
		/*As of now we are intentionally creating Service Data of one month so that it has only one row in Excel Sheet.
		So we are fetching Expected End Date from Service Data Show Page.
		In Future when we start validating Service Data having multiple rows in Excel Sheet then will have to modify this logic and
		fetch Expected End Date from Excel Sheet only.
		 */

		boolean clientBaseAmountMatched = false;

		try {
			String actualClientBaseAmountStr = TabListDataHelper.getActualValue(tabListDataResponse, "clientbaseamount", 0);

			if (actualClientBaseAmountStr != null) {
				Double actualClientBaseAmount = Double.parseDouble(actualClientBaseAmountStr);
				CurrencyConversionHelper conversionObj = new CurrencyConversionHelper();
				Double expectedClientBaseAmount = conversionObj.convertCurrencyValueToClientBaseCurrency(currencyConversionMatrixName, inputCurrency, expectedBaseAmount);

				logger.info("Actual Client Base Amount: {} and Expected Client Base Amount: {}", actualClientBaseAmount, expectedClientBaseAmount);
				clientBaseAmountMatched = actualClientBaseAmount.equals(expectedClientBaseAmount);

				if (!clientBaseAmountMatched) {
					//Checking with converting Double into Long.
					clientBaseAmountMatched = (actualClientBaseAmount.longValue() == expectedClientBaseAmount.longValue());
				}
			} else {
				logger.error("Base Amount in TabListData API Response is Null.");
			}
		} catch (Exception e) {
			logger.error("Exception while verifying Base Amount. {}", e.getMessage());
		}
		return clientBaseAmountMatched;
	}
}
