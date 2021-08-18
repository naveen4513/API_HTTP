package com.sirionlabs.test.pod.ca;

import com.sirionlabs.api.listRenderer.ListRendererFilterData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.ListRendererFilterDataHelper;
import com.sirionlabs.helper.OptionsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestSourceOptionsSIR203401 {

	private final static Logger logger = LoggerFactory.getLogger(TestSourceOptionsSIR203401.class);

	private List<Map<String, String>> allSupplierOptions;
	private String[] allContractTypes = {"MSA", "SOW", "Work Order"};
	private OptionsHelper optionsHelperObj = new OptionsHelper();
	private int randomizationSize = 5;
	private int customerFilterId = -1;
	private int supplierFilterId = -1;
	private int documentTypeFilterId = -1;
	private int statusFilterId = -1;
	private List<Map<String, String>> allOptionsOfStatusFilter;


	@BeforeClass
	public void beforeClass() {
		allSupplierOptions = optionsHelperObj.getAllSupplierOptions(160, null, true);

		ListRendererFilterData filterDataObj = new ListRendererFilterData();
		filterDataObj.hitListRendererFilterData(2);
		String filterDataResponse = filterDataObj.getListRendererFilterDataJsonStr();

		if (ParseJsonResponse.validJsonResponse(filterDataResponse)) {
			customerFilterId = ListRendererFilterDataHelper.getFilterIdFromFilterName(filterDataResponse, "customer");
			supplierFilterId = ListRendererFilterDataHelper.getFilterIdFromFilterName(filterDataResponse, "supplier");
			documentTypeFilterId = ListRendererFilterDataHelper.getFilterIdFromFilterName(filterDataResponse, "documentType");
			statusFilterId = ListRendererFilterDataHelper.getFilterIdFromFilterName(filterDataResponse, "status");
			allOptionsOfStatusFilter = ListRendererFilterDataHelper.getAllOptionsOfFilter(filterDataResponse, "status");
		}
	}


	@Test(priority = 1)
	public void testAllSourceOptionsWithCustomerValue() {
		if (allSupplierOptions == null || allSupplierOptions.isEmpty()) {
			logger.warn("Couldn't Get All Supplier Options. Hence skipping test.");
			throw new SkipException("");
		}

		CustomAssert csAssert = new CustomAssert();

		for (String contractType : allContractTypes) {
			try {
				int parentEntityId = ConfigureConstantFields.getEntityIdByName(contractType);

				int[] randomNumbersForSuppliers = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, allSupplierOptions.size() - 1, randomizationSize);
				int[] randomNumbersForCustomers = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, allSupplierOptions.size() - 1, randomizationSize);

				for (int customerNo : randomNumbersForCustomers) {
					Map<String, String> customerMap = allSupplierOptions.get(customerNo);
					String customerName = customerMap.get("name");
					int customerId = Integer.parseInt(customerMap.get("id"));

					for (int supplierNo : randomNumbersForSuppliers) {
						Map<String, String> supplierMap = allSupplierOptions.get(supplierNo);

						String supplierName = supplierMap.get("name");
						int supplierId = Integer.parseInt(supplierMap.get("id"));

						logger.info("Verifying All Source Options with Customer Value for Contract Type: {}, Customer {} and Supplier {}", contractType,
								customerName, supplierName);

						List<Map<String, String>> allSourceOptions = optionsHelperObj.getAllContractOptionsForSupplierAndCustomer(parentEntityId, customerId, supplierId,
								null, true);

						verifyCustomerAndSupplierField(customerName, customerId, supplierName, supplierId, allSourceOptions, contractType, parentEntityId,
								csAssert);
					}
				}
			} catch (Exception e) {
				csAssert.assertTrue(false, "Exception while Validating All Source Options with Customer Value for Contract Type " + contractType +
						". " + e.getMessage());
			}
		}

		csAssert.assertAll();
	}

	@Test
	public void testAllSourceOptionsWithoutCustomer() {
		if (allSupplierOptions == null || allSupplierOptions.isEmpty()) {
			logger.warn("Couldn't Get All Supplier Options. Hence skipping test.");
			throw new SkipException("");
		}

		CustomAssert csAssert = new CustomAssert();

		for (String contractType : allContractTypes) {
			try {
				int parentEntityId = ConfigureConstantFields.getEntityIdByName(contractType);

				int[] randomNumbersForSuppliers = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, allSupplierOptions.size() - 1, randomizationSize);

				for (int supplierNo : randomNumbersForSuppliers) {
					Map<String, String> supplierMap = allSupplierOptions.get(supplierNo);

					String supplierName = supplierMap.get("name");
					int supplierId = Integer.parseInt(supplierMap.get("id"));

					logger.info("Verifying All Source Options without Customer for Contract Type: {} and Supplier {}", contractType, supplierName);

					List<Map<String, String>> allSourceOptions = optionsHelperObj.getAllContractOptionsForSupplierAndCustomer(parentEntityId, supplierId,
							null, true);

					verifyCustomerAndSupplierField(null, -1, supplierName, supplierId, allSourceOptions, contractType, parentEntityId,
							csAssert);
				}
			} catch (Exception e) {
				csAssert.assertTrue(false, "Exception while Validating All Source Options without Customer for Contract Type " + contractType +
						". " + e.getMessage());
			}
		}

		csAssert.assertAll();
	}

	private void verifyCustomerAndSupplierField(String customerName, int customerId, String supplierName, int supplierId, List<Map<String, String>> allSourceOptions,
	                                            String contractType, int contractEntityTypeId, CustomAssert csAssert) {
		String payload;
		String statusSelectedData = "";

		for (Map<String, String> statusOptionMap : allOptionsOfStatusFilter) {
			String optionName = statusOptionMap.get("name");
			String optionId = statusOptionMap.get("id");

			if (optionName.equalsIgnoreCase("On Hold") || optionName.equalsIgnoreCase("Archived")) {
				continue;
			}

			statusSelectedData = statusSelectedData.concat("{\"id\":\"" + optionId + "\",\"name\":\"" + optionName + "\"},");
		}

		statusSelectedData = statusSelectedData.substring(0, statusSelectedData.length() - 1);

		String statusFilterJson = "\"" + statusFilterId + "\":{\"multiselectValues\":{\"SELECTEDDATA\":[" + statusSelectedData + "]},\"filterId\":" +
				statusFilterId + ",\"filterName\":\"status\"}";

		if (customerName == null) {
			payload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":2000,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
					"\"filterJson\":{\"" + supplierFilterId + "\":{\"filterId\":\"" + supplierFilterId + "\",\"filterName\":\"supplier\"," +
					"\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + supplierId + "\",\"name\":\"" + supplierName + "\"}]}},\"" + documentTypeFilterId +
					"\":{\"filterId\":\"" + documentTypeFilterId + "\",\"filterName\":\"documentType\"," + "\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" +
					contractEntityTypeId + "\",\"name\":\"" + contractType + "\"}]}}, " + statusFilterJson + "}},\"selectedColumns\":[]}";
		} else {
			payload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":2000,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
					"\"filterJson\":{\"" + supplierFilterId + "\":{\"filterId\":\"" + supplierFilterId + "\",\"filterName\":\"supplier\"," +
					"\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + supplierId + "\"," + "\"name\":\"" + supplierName + "\"}]}},\"" +
					documentTypeFilterId + "\":{\"filterId\":\"" + documentTypeFilterId + "\",\"filterName\":\"documentType\"," +
					"\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + contractEntityTypeId + "\",\"name\":\"" + contractType + "\"}]}}," +
					"\"" + customerFilterId + "\":{\"filterId\":\"" + customerFilterId + "\",\"filterName\":\"customer\"," +
					"\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + customerId + "\",\"name\":\"" + customerName + "\"}]}}, " + statusFilterJson +
					"}},\"selectedColumns\":[]}";
		}

		for (Map<String, String> sourceMap : allSourceOptions) {
			String sourceName = sourceMap.get("name");
			int sourceId = Integer.parseInt(sourceMap.get("id"));
			logger.info("Validating Source Option {} for Customer {} and Supplier {}. Contract Type: {}", sourceName, customerName, supplierName, contractType);

			try {
				if (customerName != null) {
					String actualCustomerId = ShowHelper.getValueOfField(61, sourceId, "customerid");

					if (actualCustomerId == null) {
						csAssert.assertTrue(false, "Couldn't get Customer Id from Show Page of Source Id " + sourceId);
					} else {
						if (customerId != Integer.parseInt(actualCustomerId)) {
							csAssert.assertTrue(false, "Customer Validation failed for Source Option " + sourceName + ", Customer " + customerName +
									" and Supplier " + supplierName + ". Contract Type: " + contractType + ". Expected Customer Id: " + customerId +
									" and Actual Customer Id: " + actualCustomerId);
						}
					}
				}

				List<String> actualSupplierId = ShowHelper.getAllSelectIdsOfField(61, sourceId, "relations");

				if (actualSupplierId.size() == 0) {
					csAssert.assertTrue(false, "Couldn't get Supplier Id from Show Page of Source Id " + sourceId);
				} else {
					String supplierIdStr = Integer.toString(supplierId);
					if(!actualSupplierId.stream().anyMatch(s -> s.contains(supplierIdStr))) {
						csAssert.assertTrue(false, "Supplier Validation failed for Source Option " + sourceName + " and Supplier " +
								supplierName + ". Contract Type:  " + contractType + ". Expected Supplier Id: " + supplierId + " and Actual Supplier Id: " + actualSupplierId);
					}
				}
			} catch (Exception e) {
				csAssert.assertTrue(false, "Exception while Validating Source Option " + sourceName + " for Customer " + customerName +
						" and Supplier " + supplierName + ". Contract Type: " + contractType);
			}
		}

		//Verify Data count.
		String listDataResponse = ListDataHelper.getListDataResponse("contracts", payload);

		if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
			int filteredCount = ListDataHelper.getFilteredListDataCount(listDataResponse);

			if (filteredCount != allSourceOptions.size()) {
				csAssert.assertTrue(false, "Source data count failed. No of Source Options: " + allSourceOptions.size() +
						" and Filtered Count in List Data: " + filteredCount + " for Contract Type: " + contractType + ", Supplier: " + supplierName +
						" and Customer: " + customerName);
			}
		} else {
			csAssert.assertTrue(false, "ListData API Response for Contracts with Supplier " + supplierName + ", Contract Type " + contractType +
					" and Customer " + customerName + " is an Invalid JSON.");
		}
	}
}