package com.sirionlabs.test.invoice;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.download.Downloadentitydata;
import com.sirionlabs.api.listRenderer.ListRendererFilterData;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.ListRendererFilterDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.InvoiceLineItem;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

//Updated by gaurav bhadani on 07 august 2018

public class TestInvoiceDetails {

	private final static Logger logger = LoggerFactory.getLogger(TestInvoiceDetails.class);
	private static String configFilePath = null;
	private static String configFileName = null;
	private static String extraFieldsConfigFilePath = null;
	private static String extraFieldsConfigFileName = null;
	private static Integer invoiceDetailsTabId = -1;
	private static Integer invoiceEntityTypeId = -1;
	private static Integer lineItemEntityTypeId;
	private static Long lineItemValidationTimeOut = 600000L;
	private static Long lineItemValidationPollingTime = 5000L;
	private static Boolean deleteEntity = true;
	private static String systemAmountColumnId = null;
	//Added by gaurav bhadani on 8 august 2018
	private String flowsConfigFilePath;
	private String flowsConfigFileName;
	private String invoiceLineItemSectionName;
	private String invoiceLineItemConfigFilePath;
	private String invoiceLineItemExtraFieldsConfigFileName;
	private String invoiceLineItemConfigFileName;

	private static String filterDataResponse = null;
	private static List<String> filtersToIgnoreList;
	private static boolean applyRandomization = false;
	private static int maxFiltersToValidate = 3;
	private static int maxFilterOptionsToValidate = 3;
	private static int maxRecordsToValidate = 3;
	private static Integer listDataOffset = 0;
	private static Integer listDataSize = 20;
	private static String defaultStartDate = null;
	private static String defaultEndDate = null;

	private static String filtersconfigFilePath = null;
	private static String filtersconfigFileName = null;
	private static String orderDirection = "asc";
	private static String orderByColumnName = "id";
	private static String invoiceUrlName;
	private static String outputExcelFilePath;
	private static String outputFileFormatForDownloadListWithData;
	private static String csrfToken;
	private static List<String> lineitemlisttobedeleted = new ArrayList<>();

	@BeforeClass
	public void beforeClass() throws ConfigurationException {
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceDetailsConfigFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceDetailsConfigFileName");
		extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
		extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");

		String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity");
		if (temp != null && temp.trim().equalsIgnoreCase("false"))
			deleteEntity = false;

		invoiceDetailsTabId = TabListDataHelper.getIdForTab("invoice details");
		invoiceEntityTypeId = ConfigureConstantFields.getEntityIdByName("invoices");
		lineItemEntityTypeId = ConfigureConstantFields.getEntityIdByName("invoice line item");

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "lineItemValidationTimeOut");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			lineItemValidationTimeOut = Long.parseLong(temp.trim());

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "lineItemValidationPollingTime");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			lineItemValidationPollingTime = Long.parseLong(temp.trim());

		//Added by gaurav bhadani
		flowsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowsconfigfilepath");
		flowsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowsconfigfilename");

		//Invoice Line Item Config files
//		invoiceLineItemConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceLineItemFilePath");
//		invoiceLineItemConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceLineItemFileName");
//		invoiceLineItemExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("invoiceLineItemExtraFieldsFileName");

		invoiceLineItemConfigFilePath = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "InvoiceLineItemFilePath");
		invoiceLineItemConfigFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "InvoiceLineItemFileName");
		invoiceLineItemExtraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "InvoiceLineItemExtraFieldsFileName");

		filtersToIgnoreList = new ArrayList<>();
		if (ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "filtersToIgnore") != null &&
				!ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "filtersToIgnore").trim().equalsIgnoreCase("")) {
			String ignoreFiltersStr[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
					"filtersToIgnore").trim().split(Pattern.quote(","));

			for (String filter : ignoreFiltersStr) {
				filtersToIgnoreList.add(filter.trim());
			}
		}

		if (applyRandomization) {
			temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "maxRecordsToValidate");
			if (temp != null && !temp.trim().equalsIgnoreCase("") && NumberUtils.isParsable(temp))
				maxRecordsToValidate = Integer.parseInt(temp);

			temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "maxNoOfFiltersToValidate");
			if (temp != null && !temp.trim().equalsIgnoreCase("") && NumberUtils.isParsable(temp))
				maxFiltersToValidate = Integer.parseInt(temp);

			temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "maxNoOfFilterOptionsToValidate");
			if (temp != null && !temp.trim().equalsIgnoreCase("") && NumberUtils.isParsable(temp))
				maxFilterOptionsToValidate = Integer.parseInt(temp);
		}

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "offset");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			listDataOffset = Integer.parseInt(temp);

		temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "size");
		if (temp != null && NumberUtils.isParsable(temp.trim()))
			listDataSize = Integer.parseInt(temp);

		defaultStartDate = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "defaultStartDate");
		defaultEndDate = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "defaultEndDate");
		filtersconfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceLineFilterFilePath");
		filtersconfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceLineFilterFileName");
		invoiceUrlName = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFilePath"),
				ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile"), "invoices", "url_name");

		outputFileFormatForDownloadListWithData = ConfigureConstantFields.getConstantFieldsProperty("DownloadListDataFileFormat");
		csrfToken = ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN");
		outputExcelFilePath = ConfigureConstantFields.getConstantFieldsProperty("DownloadListDataFilePath");

		//Service Data Config files
//		serviceDataConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("serviceDataFilePath");
//		serviceDataConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("serviceDataFileName");
//		serviceDataExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataExtraFieldsFileName");

		//Invoice Config files
//		invoiceConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceFilePath");
//		invoiceConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceFileName");
//		invoiceExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceExtraFieldsFileName");
		//

	}

	@AfterClass
	public void afterClass() {

		deleteinvoicelineitemids();
	}

	public void deleteinvoicelineitemids() {

		for (String showpageidinvoicelineitem : lineitemlisttobedeleted) {
			logger.info("Deleting invoiceline item id " + showpageidinvoicelineitem);
			EntityOperationsHelper.deleteEntityRecord("invoice line item", Integer.parseInt(showpageidinvoicelineitem));
		}
	}

	@Test(dataProvider = "dataProviderForTestValidationImprovement",enabled = false)
	public void testValidationImprovement(String flowToTest) {
		CustomAssert csAssert = new CustomAssert();
		Integer lineItemId = -1;

		try {
			logger.info("Validating Improvement Flow [{}]", flowToTest);
			Map<String, String> properties = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileName, flowToTest);
			String lineItemCreationSection = properties.get("lineitemcreationsection").trim();

			logger.info("Creating Invoice Line Item using Creation Section [{}] for Flow [{}]", lineItemCreationSection, flowToTest);
			String createResponse = InvoiceLineItem.createInvoiceLineItem(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName,
					lineItemCreationSection, true);

			if (ParseJsonResponse.validJsonResponse(createResponse)) {
				JSONObject jsonObj = new JSONObject(createResponse);
				String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
				logger.info("Create Status for Flow [{}]: {}", flowToTest, createStatus);

				if (!createStatus.trim().equalsIgnoreCase("success")) {
					throw new SkipException("Couldn't create Invoice Line Item for Flow [" + flowToTest + "]. Hence skipping test.");
				}

				lineItemId = CreateEntity.getNewEntityId(createResponse, "invoice line item");

				if (lineItemId == -1) {
					throw new SkipException("Couldn't get Id of Newly Created Invoice Line Item for Flow [" + flowToTest + "]. Hence skipping test.");
				}

				if (invoiceDetailsTabId == -1) {
					throw new SkipException("Couldn't get Id of Invoice Details Tab. Hence skipping test.");
				}

				//Wait till Line Item completes Validation
				waitForLineItemValidation(flowToTest, lineItemId);

				Integer invoiceId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, lineItemCreationSection, "sourceId"));
				String tabListPayload = "{\"filterMap\":{\"entityTypeId\":165,\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\"," +
						"\"filterJson\":{}}}";

				logger.info("Hitting TabListData API for Details Tab of Invoice Id {} and Flow [{}]", invoiceId, flowToTest);
				TabListData tabListObj = new TabListData();
				String tabListResponse = tabListObj.hitTabListData(invoiceDetailsTabId, invoiceEntityTypeId, invoiceId, tabListPayload);

				if (ParseJsonResponse.validJsonResponse(tabListResponse)) {
					List<Map<Integer, Map<String, String>>> tabListData = ListDataHelper.getListData(tabListResponse);

					if (tabListData.isEmpty()) {
						throw new SkipException("Couldn't get Data from TabListData API Response for Flow [" + flowToTest + "] and Invoice Id " + invoiceId);
					}

					String systemAmountColumnId = getSystemAmountColumnId(tabListResponse);
					if (systemAmountColumnId == null) {
						throw new SkipException("Couldn't get Id for systemAmount Column from TabListData API Response for Flow [" + flowToTest + "] and Invoice Id " +
								invoiceId);
					}

					String actualSystemAmount = tabListData.get(0).get(Integer.parseInt(systemAmountColumnId)).get("value");
					if (actualSystemAmount == null) {
						throw new SkipException("Couldn't get System Amount Value from TabListData API Response for Flow [" + flowToTest + "] and Invoice Id " + invoiceId);
					}

					String expectedSystemAmount = properties.get("expectedamount").trim();
					logger.info("Expected System Amount: {} and Actual System Amount: {} for Flow [{}] and Invoice Id {}", expectedSystemAmount, actualSystemAmount,
							flowToTest, invoiceId);

					if (Double.parseDouble(expectedSystemAmount) != Double.parseDouble(actualSystemAmount)) {
						csAssert.assertTrue(false, "System Amount for Invoice Id " + invoiceId + " and Flow [" + flowToTest +
								"] doesn't match with Expected Value");
					}
				} else {
					csAssert.assertTrue(false, "TabListData API Response for Invoice Id " + invoiceId + " and Details Tab is an Invalid JSON.");
				}
			} else {
				csAssert.assertTrue(false, "Create API Response for Invoice Line Item Creation Flow [" + flowToTest + "] is an Invalid JSON.");
			}
		} catch (SkipException e) {
			logger.error(e.getMessage());

			if (deleteEntity && lineItemId != -1) {
				EntityOperationsHelper.deleteEntityRecord("invoice line item", lineItemId);
			}

			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			csAssert.assertTrue(false, "Exception while validating Invoice Line Item Creation Flow [" + flowToTest + "]. " + e.getMessage());
		} finally {
			if (deleteEntity && lineItemId != -1) {
				EntityOperationsHelper.deleteEntityRecord("invoice line item", lineItemId);
				csAssert.assertAll();
			}
		}
	}

	@Test(dataProvider = "dataProviderForInvoiceFlow",enabled = false)
	public void testLineItemCreation(String flowToTest, String serviceDataid) {
		CustomAssert csAssert = new CustomAssert();

		int invoiceLineItemId =- 1;

		try {
			clearEntitySections();

			int serviceDataId = Integer.parseInt(serviceDataid);

			logger.info("Using Service Data Id {} for Validation of Flow [{}]", serviceDataId, flowToTest);

			invoiceLineItemId = getInvoiceLineItemId(flowToTest, serviceDataId);
			logger.info("Created invoiceLineItemId Id is : [{}]", invoiceLineItemId);

			if (invoiceLineItemId != -1) {

				logger.info("Invoice Line Item Creation successful for the flow [{}] and Service Data Id [{}]", flowToTest, serviceDataid);
				csAssert.assertTrue(true, "Invoice Line Item Creation successful for the flow [" + flowToTest + "] and Service Data Id [" + serviceDataid + "]");

			} else {
				logger.error("Invoice Line Item Creation unsuccessful for the flow [{}] and Service Data Id [{}]", flowToTest, serviceDataid);
				csAssert.assertTrue(false, "Invoice Line Item Creation successful for the flow [" + flowToTest + "] and Service Data Id [" + serviceDataid + "]");
			}
		} catch (Exception e) {
			logger.error("Exception while Validating Invoice Flow [{}]. {}", flowToTest, e.getStackTrace());
			csAssert.assertTrue(false, "Exception while Validating Invoice Flow [" + flowToTest + "]. " + e.getMessage());
		}

		csAssert.assertAll();
	}

	@Test(dependsOnMethods = "testLineItemCreation",enabled = false)
	public void testVerifyGroupBy() {

		TabListData tabListData = new TabListData();
		CustomAssert csAssert = new CustomAssert();
		try {
			int tablistid = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "tablistid"));
			int invoiceid = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "invoiceid"));

			int offset = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "group by meta fields", "offset"));
			int size = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "group by meta fields", "size"));
			int filterid = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "group by meta fields", "filterid"));
			String orderDirection = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "group by meta fields", "orderdirection");
			String filtername = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "group by meta fields", "filtername");
			String[] groupbyoptions = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "group by meta fields", "groupbyoptions").split(",");
			int selectdataid = 0;
			String groupbyname = null;
			String filterjson;
			String tablistresponse;
			Boolean result;
			for (String groupbyoption : groupbyoptions) {

				selectdataid = Integer.parseInt(groupbyoption.split("->")[0]);
				groupbyname = groupbyoption.split("->")[1];

				filterjson = createFilterPayloadforGroupBy(lineItemEntityTypeId, offset, size, orderDirection, filterid, filtername, selectdataid, groupbyname);

				logger.info("Hitting tab list api for the payload " + filterjson);
				tablistresponse = tabListData.hitTabListData(tablistid, invoiceEntityTypeId, invoiceid, filterjson);


				logger.info("Validating invoice line Details tab for group by option " + groupbyname);
				result = validateGroupByOption(tablistresponse, groupbyname);

				if (result == false) {
					csAssert.assertTrue(false, "Error validating invoice line Details tab for group by option " + groupbyname);
				} else {
					csAssert.assertTrue(true, "Successful Validation of invoice line Details tab for group by option " + groupbyname);
				}
			}

		} catch (Exception e) {
			logger.error("Exception while validating invoice line item Details tab");
			csAssert.assertTrue(false, "Exception while validating invoice line item Details tab");
		}

	}

	@Test(dataProvider = "dataProviderForInvoiceDetailsTabFilters", priority = 0)
	public void testInvoiceDetailsTabFilters(String filterName) {
		CustomAssert csAssert = new CustomAssert();

		try {

			String filterpayload;
			int tablistid = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "tablistid"));
			int invoiceid = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "invoiceid"));
			logger.info("************************************************");
			logger.info("Verifying Filter: {}", filterName);
			logger.info("Getting Id for Filter: {}", filterName);
			Integer filterId = ListRendererFilterDataHelper.getFilterIdFromFilterName(filterDataResponse, filterName);
			Map<String, String> seriveDataMap;
			Map<String, String> seriveDataIndvMap;

			if (filterName.equals("serviceData")) {
				createFilterJson();
				seriveDataMap = getServieDataList(tablistid, invoiceid, createFilterJson());
				for (Map.Entry<String, String> pair : seriveDataMap.entrySet()) {
					filterpayload = createFilterforServiceData(filterId, pair.getKey(), pair.getValue());

					seriveDataIndvMap = getServieDataList(tablistid, invoiceid, filterpayload);
					for (Map.Entry<String, String> seriveDataIndvColMap : seriveDataIndvMap.entrySet()) {
						if (seriveDataIndvColMap.getValue().equals(pair.getValue())) {
							logger.info("Valid value for service data filter " + pair.getValue());
						} else {
							logger.error("Unexpected value for service data expected value " + pair.getValue() + " Actual value" + seriveDataIndvColMap.getValue());
						}
					}
				}

				csAssert.assertAll();
				return;
			}

			if (filterId != -1) {
				logger.info("Getting Show Page Object Name Mapping of Filter {}", filterName);
				String showPageObjectName = getShowPageObjectNameMapping(filterName);

				if (showPageObjectName != null) {
					ListRendererFilterDataHelper.validateFilter(filterDataResponse, filterName, filterId, "invoice line item", lineItemEntityTypeId,
							listDataOffset, listDataSize, defaultStartDate, defaultEndDate, "1", "500000", showPageObjectName, csAssert);
				} else {
					throw new SkipException("Couldn't get Show Page Object Name of Filter " + filterName + ". Hence skipping test.");
				}
			} else {
				throw new SkipException("Couldn't get Id for Filter " + filterName + ". Hence skipping test.");
			}
		} catch (SkipException e) {
			throw new SkipException(e.getMessage());
		} catch (Exception e) {
			logger.error("Exception while Validating Contract Price Book Filters. {}", e.getMessage());
			csAssert.assertTrue(false, "Exception while Validating Contract Price Book Filters. " + e.getMessage());
		}
		csAssert.assertAll();
	}

	@Test(dependsOnMethods = "testLineItemCreation",enabled = false)
	public void testDetailsTabDownload() {

		logger.info("Validating invoice details tab download functionality");
		CustomAssert csAssert = new CustomAssert();

		downloadListDataForAllColumns(lineItemEntityTypeId, "invoice line item");

		Boolean exceldownloadvalidationstatus = validatedownloadedexceldetailstab(csAssert);
		if (exceldownloadvalidationstatus == true) {
			logger.info("Downloaded excel data from invoices details tab validated successfully");
			csAssert.assertTrue(true, "Downloaded excel data from invoices details tab validated successfully");
		} else {
			logger.error("Error while validating downloaded excel data from invoices details tab");
			csAssert.assertTrue(false, "Error while validating downloaded excel data from invoices details tab");
		}

		csAssert.assertAll();

	}

	@DataProvider
	public Object[][] dataProviderForTestValidationImprovement() throws ConfigurationException {
		logger.info("Setting all Validation Improvement Flows to Validate");
		List<Object[]> allTestData = new ArrayList<>();
		List<String> flowsToTest = new ArrayList<>();

		String allFlows[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
				"validationImprovementFlowsToValidate").split(Pattern.quote(","));

		for (String flow : allFlows) {
			if (ParseConfigFile.containsSection(configFilePath, configFileName, flow.trim())) {
				flowsToTest.add(flow.trim());
			} else {
				logger.info("Flow having name [{}] not found in Invoice Details Config File.", flow.trim());
			}
		}

		for (String flowToTest : flowsToTest) {
			allTestData.add(new Object[]{flowToTest});
		}
		return allTestData.toArray(new Object[0][]);
	}

	@DataProvider(name = "dataProviderForInvoiceDetailsTabFilters", parallel = false)
	public Object[][] dataProviderForInvoiceDetailsTabFilters() {
		List<Object[]> filtersToTest = new ArrayList<>();

		logger.info("Setting All Filters to Test");
		logger.info("Hitting Invoice Details Tab Filter Data Api.");
		ListRendererFilterData filterObj = new ListRendererFilterData();
		String payloadForFilterData = "{}";
		filterObj.hitListRendererFilterData(invoiceDetailsTabId, payloadForFilterData);
		String filterResponse = filterObj.getListRendererFilterDataJsonStr();

		if (ParseJsonResponse.validJsonResponse(filterResponse)) {
			filterDataResponse = filterResponse;
			List<String> allFilterNames = ListRendererFilterDataHelper.getAllFilterNames(filterResponse);
			if (allFilterNames.size() > 0) {
				logger.info("Total Filters found: {}", allFilterNames.size());
				logger.info("Removing Filters (if any) that are to be Ignored.");

				List<String> eligibleFilters = new ArrayList<>();
				eligibleFilters.addAll(allFilterNames);

				for (String filterToIgnore : filtersToIgnoreList) {
					if (allFilterNames.contains(filterToIgnore))
						eligibleFilters.remove(filterToIgnore);
				}

				if (eligibleFilters.size() > 0) {
					List<String> allFiltersToValidate = new ArrayList<>();
					if (applyRandomization) {
						logger.info("Maximum No of Filters to Validate: {}", maxFiltersToValidate);
						int[] randomNumbersForFilters = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, eligibleFilters.size() - 1, maxFiltersToValidate);
						for (int randomNumber : randomNumbersForFilters) {
							allFiltersToValidate.add(eligibleFilters.get(randomNumber));
						}
					} else {
						allFiltersToValidate.addAll(eligibleFilters);
					}

					for (String filter : allFiltersToValidate) {
						filtersToTest.add(new Object[]{filter});
					}
				} else {
					logger.error("No Eligible Filter found for Contract Price Book.");
				}
			} else {
				logger.error("No Filter found for Contract Price Book.");
			}
		}
		return filtersToTest.toArray(new Object[0][]);
	}

	@DataProvider
	public Object[][] dataProviderForInvoiceFlow() {
		logger.info("Setting all Invoice Flows to Validate");
		List<Object[]> allTestData = new ArrayList<>();
		List<String> allFlowsToTest = getFlowsToTest();
		String servicedataid;
		try {
			for (String flowToTest : allFlowsToTest) {
				servicedataid = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "servicedata");
				allTestData.add(new Object[]{flowToTest, servicedataid});
			}
		} catch (Exception e) {
			logger.error("Error while parsing " + flowsConfigFilePath + " " + flowsConfigFileName);
		}

		return allTestData.toArray(new Object[0][]);
	}

	private Boolean validateGroupByOption(String tabListDataResponse, String groupByOption) {

		Boolean status = true;

		JSONObject tablistdataresponsejson = new JSONObject(tabListDataResponse);
		JSONArray dataarray = tablistdataresponsejson.getJSONArray("data");
		JSONArray indvdata;
		JSONObject data;
		JSONObject columnjson;
		String groupbyoptionname = null;
		String prevrowvalue = "validation not started";
		String currrowvalue;
		Map<String, Integer> groupbyoptionmapping = new HashMap<>();
		Integer valueocurredontheposition = 0;
		try {
			groupbyoptionname = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "groupbyfilterlistcolumnmapping", groupByOption);
			for (int i = 0; i < dataarray.length(); i++) {
				data = dataarray.getJSONObject(i);
				indvdata = JSONUtility.convertJsonOnjectToJsonArray(data);
				indvdataloop:
				for (int j = 0; j < indvdata.length(); j++) {
					columnjson = indvdata.getJSONObject(j);
					if (columnjson.get("columnName").toString().equals(groupbyoptionname)) {
						currrowvalue = columnjson.get("value").toString();
						if (prevrowvalue.equals("validation not started") || (!prevrowvalue.equals(currrowvalue))) {
							valueocurredontheposition = valueocurredontheposition + 1;
							prevrowvalue = currrowvalue;
							groupbyoptionmapping.put(currrowvalue, valueocurredontheposition);
						} else {
							if (!(valueocurredontheposition.equals(groupbyoptionmapping.get(currrowvalue)))) {
								status = false;
								logger.error("Value doesn't lie in the group by " + currrowvalue);
							}
						}
						break indvdataloop;
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while validating group by functionality on details tab " + e.getMessage());
		}

		return status;
	}

	private String createFilterPayloadforGroupBy(int entityTypeId, int offset, int size, String orderDirection, int filterid,
	                                             String filtername, int selectdataid, String groupbyname) {

		String FilterJson = "{\"filterMap\": {\"entityTypeId\": " + entityTypeId + ",\"offset\": " + offset +
				",\"size\": " + size + ",\"orderByColumnName\": \"id\",\"orderDirection\": \"" + orderDirection +
				"\",\"filterJson\": {\"" + filterid + "\": {\"filterId\": " + filterid + ",\"filterName\": \"" + filtername +
				"\",\"multiselectValues\": {\"SELECTEDDATA\": [{\"id\": \"" + selectdataid + "\",\"name\": \"" + groupbyname +
				"\"}]}}}}}";

		return FilterJson;
	}

	private boolean validatedownloadedexceldetailstab(CustomAssert csAssert) {
		Boolean status = true;
		try {
			int tablistid = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "tablistid"));
			int invoiceid = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "invoiceid"));

			TabListData tabListData = new TabListData();
			String tablistresponse;

			tablistresponse = tabListData.hitTabListData(tablistid, invoiceEntityTypeId, invoiceid, createFilterJson());

			Map<Integer, Map<Integer, String>> detailstabrowcolmapping = detailstabrowcolmapping(tablistresponse);

			status = validateDownloadedExcel(detailstabrowcolmapping,csAssert);

		} catch (Exception e) {
			logger.error("Exception while validating downloaded excel from detials tab " + e.getMessage());
		}
		return status;
	}

	private Boolean validateDownloadedExcel(Map<Integer, Map<Integer, String>> detailstabrowcolmapping,CustomAssert csAssert) {

		Boolean excelvalidationstatus = true;
		try {
			String outputfilepath = outputExcelFilePath + "TestDownloadEntityData\\" + "invoice line item";
			XLSUtils xlsUtils = new XLSUtils(outputExcelFilePath, "AllColumn.xlsx");
			int columnum = 0;
			String exceldata;
			String expectedexcelcelldata;
			String date[];
			for (Map.Entry<Integer, Map<Integer, String>> entry : detailstabrowcolmapping.entrySet()) {
				Integer rownum = entry.getKey();
				rownum = rownum + 3;
//				System.out.println(rownum);
				Map<Integer, String> colnumval = entry.getValue();
				columnum = 0;
				String month;
				int dateconv;
				String year;
				for (Map.Entry<Integer, String> entry1 : colnumval.entrySet()) {
					expectedexcelcelldata = entry1.getValue();
					if (isValidDate(expectedexcelcelldata)) {//To conv date in YYYYMMMDD

						date = expectedexcelcelldata.split("-");

						month = DateUtils.getMonthindigit(date[0]);
						dateconv = Integer.parseInt(date[1]);
						year = date[2].substring(2,4);
						//expectedexcelcelldata = year + month + dateconv;
						expectedexcelcelldata = month + "/" + dateconv + "/" + year;
					}
//					System.out.println(expectedexcelcelldata);
					exceldata = xlsUtils.getCellData("Data", columnum, rownum);

					if(Pattern.matches("[\\d]*+-+[\\d]*+-+[\\d]*",exceldata)){
						exceldata.replace("-","/");
						if(exceldata.equalsIgnoreCase(expectedexcelcelldata)){
							columnum = columnum + 1;
							continue;
						}

					}
					if (expectedexcelcelldata.equals("null")) {
						if (exceldata.equals("-") || exceldata.equals("")) {
							logger.info("Expected and Actual values match for Data Sheet for downloaded excel for rownum " + rownum + " and column number " + columnum);
						} else {
							logger.error("Expected and Actual values mismatch for Data Sheet for downloaded excel for rownum " + rownum + " and column number " + columnum);
							excelvalidationstatus = false;
						}
					} else if (isNumeric(expectedexcelcelldata)) {
						Double expexcelval = Double.parseDouble(expectedexcelcelldata);
						Double actexceldata = Double.parseDouble(exceldata);

						if (expexcelval.equals(actexceldata)) {
							logger.info("Expected and Actual values match for Data Sheet for downloaded excel for rownum " + rownum + " and column number " + columnum);
						} else {
							logger.error("Expected and Actual values mismatch for Data Sheet for downloaded excel for rownum " + rownum + " and column number " + columnum);
							csAssert.assertTrue(false,"Expected and Actual values mismatch for Data Sheet for downloaded excel for rownum " + rownum + " and column number " + columnum);
							excelvalidationstatus = false;
						}
					} else if (exceldata.equals(expectedexcelcelldata)) {
						logger.info("Expected and Actual values match for Data Sheet for downloaded excel for rownum " + rownum + " and column number " + columnum);
					} else {
						logger.error("Expected and Actual values mismatch for Data Sheet for downloaded excel for rownum " + rownum + " and column number " + columnum);
						csAssert.assertTrue(false,"Expected and Actual values mismatch for Data Sheet for downloaded excel for rownum " + rownum + " and column number " + columnum);
						excelvalidationstatus = false;
					}

					columnum = columnum + 1;
				}
			}
		} catch (Exception e) {
			logger.error("Exception while validating data from downloaded excel for invoices details tab " + e.getMessage());
			csAssert.assertTrue(false,"Exception while validating data from downloaded excel for invoices details tab " + e.getMessage());
			return false;
		}
		return excelvalidationstatus;
	}

	private String downloadListDataForAllColumns(Integer entityTypeId, String entityName) {

		try {
			int showPageId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "invoiceid"));
			String filtejson = "\"156\": {\"filterId\": 156,\"filterName\": \"invoiceId\",\"multiselectValues\": {\"SELECTEDDATA\": [{\"id\": \"" + showPageId + "\"}]}}";
			Map<String, String> formParam = getDownloadListWithDataPayload(entityTypeId, null, filtejson);
			Downloadentitydata downloadentitydata = new Downloadentitydata();


			logger.info("formParam is : [{}]", formParam);

			HttpResponse response = downloadentitydata.hitDownloadListWithData(formParam, invoiceUrlName, invoiceDetailsTabId, showPageId);

			if (response.getStatusLine().toString().contains("200")) {
				/*
				 * dumping response into file
				 * */
				return dumpDownloadListWithDataResponseIntoFile(response, outputExcelFilePath, "TestDownloadEntityData", entityName, "AllColumn");
			} else {
				return null;
			}
		} catch (Exception e) {
			logger.error("Exception while downloading list data for invoices details tab");
		}
		return null;
	}

	private String dumpDownloadListWithDataResponseIntoFile(HttpResponse response, String outputFilePath, String featureName, String entityName, String columnStatus) {
		String outputFile = null;
		String outputFileName;
		FileUtils fileUtil = new FileUtils();
		Boolean isFolderSuccessfullyCreated = fileUtil.createNewFolder(outputFilePath, featureName);
		Boolean isFolderWithEntityNameCreated = fileUtil.createNewFolder(outputFilePath + "/" + featureName + "/", entityName);
		if (isFolderSuccessfullyCreated && isFolderWithEntityNameCreated) {
			outputFilePath = outputFilePath + "/" + featureName + "/" + entityName;
			outputExcelFilePath = outputFilePath;
			outputFileName = columnStatus + outputFileFormatForDownloadListWithData;
			outputFile = outputFilePath + "/" + outputFileName;
			Boolean status = fileUtil.writeResponseIntoFile(response, outputFile);
			if (status) {
				logger.info("DownloadListWithData file generated at {}", outputFile);
				return outputFileName;
			} else {
				logger.error("DownloadListWithData file not generated at {}", outputFile);
				return null;
			}
		} else {
			return null;
		}
	}

	private Map<String, String> getDownloadListWithDataPayload(Integer entityTypeId, Map<Integer, String> selectedColumnMap, String filterjson) {

		Map<String, String> formParam = new HashMap<String, String>();
		String jsonData = null;

		if (selectedColumnMap == null) {
			jsonData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + listDataOffset + ",\"size\":" + 20 + ",\"orderByColumnName\":\"" + orderByColumnName + "\",\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":{" + filterjson + "}}}";
		} else {
			String selectedColumnArray = "\"selectedColumns\":[";
			for (Map.Entry<Integer, String> entryMap : selectedColumnMap.entrySet()) {
				selectedColumnArray += "{\"columnId\":" + entryMap.getKey() + ",\"columnQueryName\":\"" + entryMap.getValue() + "\"},";
			}
			selectedColumnArray = selectedColumnArray.substring(0, selectedColumnArray.length() - 1);
			selectedColumnArray += "]";

			jsonData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + listDataOffset + ",\"size\":" + listDataSize + ",\"orderByColumnName\":\"" + orderByColumnName + "\",\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":{}}," + selectedColumnArray + "}";
		}

		logger.debug("json for downloading list : [{}]", jsonData);
		formParam.put("jsonData", jsonData);
		formParam.put("_csrf_token", csrfToken);

		return formParam;
	}

	private Map<String, String> getServieDataList(int tablistid, int invoiceid, String payload) {

		TabListData tabListData = new TabListData();
		String tablistresponse;

		tablistresponse = tabListData.hitTabListData(tablistid, invoiceEntityTypeId, invoiceid, payload);

		Map<String, String> serviceData = new HashMap<>();

		JSONObject tablistresponsejson = new JSONObject(tablistresponse);
		JSONArray dataarray = tablistresponsejson.getJSONArray("data");
		JSONObject dataobject;
		JSONArray indvdataarray;
		JSONObject indvcoljson;
		String indvcoljsonvalue;
		String indvcoljsoncolid;
		for (int i = 0; i < dataarray.length(); i++) {
			dataobject = dataarray.getJSONObject(i);
			indvdataarray = JSONUtility.convertJsonOnjectToJsonArray(dataobject);
			innerloop:
			for (int j = 0; j < indvdataarray.length(); j++) {
				indvcoljson = indvdataarray.getJSONObject(j);
				if (indvcoljson.get("columnName").equals("serviceData")) {
					indvcoljsonvalue = indvcoljson.get("value").toString().split(":;")[0];
					indvcoljsoncolid = indvcoljson.get("value").toString().split(":;")[1];
					serviceData.put(indvcoljsoncolid, indvcoljsonvalue);
					break innerloop;
				}
			}
		}
		return serviceData;
	}

	private Map<Integer, Map<Integer, String>> detailstabrowcolmapping(String tablistresponse) {

		JSONArray dataarray = new JSONObject(tablistresponse).getJSONArray("data");
		JSONObject indvjsonobj;
		JSONObject coljsonobj;
		JSONArray indvjsonarray;
		String colname;
		String colval;
		Map<String, String> metadatacolumnmap = createmetadatacolumnname();
		TreeMap<Integer, Map<Integer, String>> rowcolmap = new TreeMap<>();
		Integer order;
		String showpageid = null;

		try {
			for (int i = 0; i < dataarray.length(); i++) {
				indvjsonobj = dataarray.getJSONObject(i);
				indvjsonarray = JSONUtility.convertJsonOnjectToJsonArray(indvjsonobj);

				TreeMap<Integer, String> colnamevaluemap = new TreeMap<>();
				TreeMap<Integer, String> colnamecolunummap = new TreeMap<>();

				for (int j = 0; j < indvjsonarray.length(); j++) {
					coljsonobj = indvjsonarray.getJSONObject(j);
					String columnid = coljsonobj.get("columnId").toString();
					colname = (metadatacolumnmap.get(columnid));


					if (!(colname == null)) {
						colname = (metadatacolumnmap.get(columnid)).toString().toUpperCase();  //play here
						colval = coljsonobj.get("value").toString();
						order = Integer.parseInt(colname.split("->")[1]);
						colname = colname.split("->")[0];
						colnamecolunummap.put(order, colname);

						if (colval.contains(":;")) {
							showpageid = colval.split(":;")[1];
							colval = colval.split(":;")[0];
						}

						if (colname.equals("ID")) {
							colval = "LI0" + colval;
							lineitemlisttobedeleted.add(showpageid);
						}
						if (colname.equals("CHECKBOX")) {
							continue;
						}

						colnamevaluemap.put(order, colval);
						rowcolmap.put(0, colnamecolunummap);
						rowcolmap.put(i + 1, colnamevaluemap);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while parsing meta data response for invoice details tab " + e.getStackTrace());
		}

		return rowcolmap;
	}

	private Map<String, String> createmetadatacolumnname() {

		String queryname;
		String defaultname;
		String columnId;

		ListRendererTabListData listmetadata = new ListRendererTabListData();
		listmetadata.hitListRendererTabListMetaData(invoiceDetailsTabId, invoiceEntityTypeId, "{}");

		String metadataresponse = listmetadata.getTabListDataJsonStr();
		if(!(APIUtils.validJsonResponse(metadataresponse))){

			logger.error("Not a valid json for metadataresponse");
		}
		JSONArray columnnames = new JSONObject(metadataresponse).getJSONArray("columns");

		Map<String, String> querycolmap = new HashMap<>();
		for (int i = 0; i < columnnames.length(); i++) {
			if (columnnames.getJSONObject(i).get("displayFormat").toString().equals("{\"hideColumn\" : true}")) {
				continue;
			}
			queryname = columnnames.getJSONObject(i).get("queryName").toString();
			columnId = columnnames.getJSONObject(i).get("id").toString();
			if (queryname.equals("bulkcheckbox")) {
				continue;
			}
			defaultname = columnnames.getJSONObject(i).get("name").toString();
			if (defaultname.equals("null")) {
				defaultname = columnnames.getJSONObject(i).get("defaultName").toString();
			}
			defaultname = defaultname + "->" + columnnames.getJSONObject(i).get("order").toString();
			querycolmap.put(columnId, defaultname);
		}

		return querycolmap;
	}

	private String createFilterforServiceData(Integer filterId, String selectdataid, String selectdataname) {
		String filterjsonpayload;

		filterjsonpayload = "{\"filterMap\": {\"entityTypeId\": " + lineItemEntityTypeId + ",\"offset\": " + listDataOffset + ",\"size\": " + listDataSize + ",\"orderByColumnName\": \"id\",\"orderDirection\": \"asc\",\"filterJson\": {\"" + filterId + "\": {\"filterId\": \"" + filterId + "\",\"filterName\": \"serviceData\",\"entityFieldId\": null,\"entityFieldHtmlType\": null,\"multiselectValues\": {\"SELECTEDDATA\": [{\"id\": \"" + selectdataid + "\",\"name\": \"" + selectdataname + "\"}]}}}}}";
		return filterjsonpayload;
	}

	private String createFilterJson() {
		String filterjsonpayload;

		filterjsonpayload = "{\"filterMap\": {\"entityTypeId\": " + lineItemEntityTypeId + ",\"offset\": " + listDataOffset + ",\"size\": " + listDataSize + ",\"orderByColumnName\": \"id\",\"orderDirection\": \"asc\",\"filterJson\": {}}}";
		return filterjsonpayload;
	}

	private String getShowPageObjectNameMapping(String filterName) {
		String showPageObjectName = null;

		try {
			showPageObjectName = ParseConfigFile.getValueFromConfigFileCaseSensitive(filtersconfigFilePath, filtersconfigFileName, "filterNameShowPageObjectMapping", filterName);
		} catch (Exception e) {
			logger.error("Exception while getting Show Page Object Name Mapping of Filter {}. {}", filterName, e.getStackTrace());
		}
		return showPageObjectName;
	}

	private boolean isNumeric(String str) {
		try {
			Double.parseDouble(str);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	private boolean isValidDate(String dateString) {
		SimpleDateFormat df = new SimpleDateFormat("MMM-dd-yyyy");
		try {
			df.parse(dateString);
			return true;
		} catch (ParseException e) {
			return false;
		}
	}

	private void waitForLineItemValidation(String flowToTest, Integer lineItemId) {
		try {
			logger.info("Checking Line Item Undergoing Validation for Flow [{}] and Line Item Id {}", flowToTest, lineItemId);
			Long timeSpent = 0L;
			Boolean lineItemValidated = false;
			Show showObj = new Show();

			while (timeSpent <= lineItemValidationTimeOut) {
				showObj.hitShow(lineItemEntityTypeId, lineItemId);
				String showResponse = showObj.getShowJsonStr();

				if (ParseJsonResponse.validJsonResponse(showResponse)) {
					if (ShowHelper.isLineItemUnderOngoingValidation(lineItemEntityTypeId, lineItemId)) {
						logger.info("Invoice Line Item having Id {} is still undergoing Validation for Flow [{}]. Putting Thread on Sleep for {} milliseconds. " +
										"Time Spent: [{}] milliseconds and Time Out: [{}] milliseconds", flowToTest, lineItemId, lineItemValidationPollingTime,
								timeSpent, lineItemValidationTimeOut);

						Thread.sleep(lineItemValidationPollingTime);
						timeSpent += lineItemValidationPollingTime;
					} else {
						lineItemValidated = true;
						break;
					}
				} else {
					throw new SkipException("Show API Response for Invoice Line Item Id " + lineItemId + " is an Invalid JSON.");
				}
			}

			if (!lineItemValidated) {
				throw new SkipException("Invoice Line Item Validation didn't Complete within Time Out [" + lineItemValidationTimeOut + "] milliseconds for Flow [" +
						flowToTest + "] and Id " + lineItemId + ". Hence skipping test.");
			}
		} catch (Exception e) {
			throw new SkipException(e.getMessage());
		}
	}

	private String getSystemAmountColumnId(String tabListResponse) {
		if (systemAmountColumnId == null) {
			systemAmountColumnId = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "systemAmount");
		}

		return systemAmountColumnId;
	}

	private int getInvoiceLineItemId(String flowToTest, int serviceDataId) {
		int lineItemId = -1;
		String invoiceLineItemEntity = "invoice line item";
		try {
			String temp = ParseConfigFile.getValueFromConfigFile(flowsConfigFilePath, flowsConfigFileName, flowToTest, "invoicelineitemsectionname");
			if (temp != null)
				invoiceLineItemSectionName = temp.trim();

			logger.info("Updating Invoice Line Item Property Service Id Supplier in Extra Fields Config File for Flow [{}] and Service Data Id {}.",
					invoiceLineItemSectionName, serviceDataId);

			String createResponse = InvoiceLineItem.createInvoiceLineItem(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemConfigFilePath,
					invoiceLineItemExtraFieldsConfigFileName, invoiceLineItemSectionName, true);
			lineItemId = CreateEntity.getNewEntityId(createResponse, invoiceLineItemEntity);

		} catch (Exception e) {
			logger.error("Exception while getting Invoice Line Item Id using Flow Section [{}]. {}", flowToTest, e.getStackTrace());
		}
		return lineItemId;
	}

	private List<String> getFlowsToTest() {
		List<String> flowsToTest = new ArrayList<>();

		try {
			String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "testallflows");
			if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
				logger.info("TestAllFlows property is set to True. Therefore all the flows are to validated");
				flowsToTest = ParseConfigFile.getAllSectionNames(flowsConfigFilePath, flowsConfigFileName);
			} else {
				String allFlows[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowstovalidate").split(Pattern.quote(","));
				for (String flow : allFlows) {
					if (ParseConfigFile.containsSection(flowsConfigFilePath, flowsConfigFileName, flow.trim())) {
						flowsToTest.add(flow.trim());
					} else {
						logger.info("Flow having name [{}] not found in Invoice Config File.", flow.trim());
					}
				}
			}
		} catch (Exception e) {
			logger.error("Exception while getting Flows to Test for Invoice Validation. {}", e.getMessage());
		}
		return flowsToTest;
	}

	//Added by gaurav bhadani
	private void clearEntitySections() {
		invoiceLineItemSectionName = "default";
	}
}