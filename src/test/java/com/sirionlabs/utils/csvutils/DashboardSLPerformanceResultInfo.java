package com.sirionlabs.utils.csvutils;

public class DashboardSLPerformanceResultInfo extends ResultInfoClass {
	//Constructor for generating csv column headings for SL Performance- Primary and Advanced Filter Validation
	public DashboardSLPerformanceResultInfo() {
		super("isMetaDataValidJson", "isFilterDataValidJson", "PrimaryFilterName", "PrimaryFilterOptionName", "AdvancedFilterName", "AdvancedFilterOptionName", "slaChart API Payload", "isSlaChartValidJsonResponse", "isDataValidationPassed", "TestStatus");
	}

	//Constructor for appending rows in csv for SL Performance- Primary and Advanced Filter Validation
	public DashboardSLPerformanceResultInfo(String isMetaDataValidJson, String isFilterDataValidJson, String primaryFilterName, String primaryFilterOptionName, String advancedFilterName, String advancedFilterOptionName, String slachartPayload, String isSlaChartValidJsonResponse, String isDataValidationPassed, String testStatus) {

		super(isMetaDataValidJson, isFilterDataValidJson, primaryFilterName, primaryFilterOptionName, advancedFilterName, advancedFilterOptionName, slachartPayload, isSlaChartValidJsonResponse, isDataValidationPassed, testStatus);
	}

	//Constructor for generating csv column heading for SL Performance- Sorting Order Validation
	public DashboardSLPerformanceResultInfo(String testSortingOrder) {
		super("isMetaDataValidJson", "isFilterDataValidJson", "PrimaryFilterName", "SortingCriteria", "SortingOrder", "SlaChartPayload", "isSlaChartValidJsonResponse", "isResponseInSortedOrder", "TestStatus");
	}

	//Constructor for appending rows in csv for SL Performance Sorting Order Validation
	public DashboardSLPerformanceResultInfo(String isMetaDataValidJson, String isFilterDataValidJson, String primaryFilterName, String sortingCriteria, String sortingOrder, String slaChartPayload, String isSlaChartValidJsonResponse, String isResponseInSortedOrder, String testStatus) {
		super(isMetaDataValidJson, isFilterDataValidJson, primaryFilterName, sortingCriteria, sortingOrder, slaChartPayload, isSlaChartValidJsonResponse, isResponseInSortedOrder, testStatus);
	}
}
