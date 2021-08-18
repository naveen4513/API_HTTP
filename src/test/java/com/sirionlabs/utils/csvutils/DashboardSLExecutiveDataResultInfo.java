package com.sirionlabs.utils.csvutils;

public class DashboardSLExecutiveDataResultInfo extends ResultInfoClass {
	//Constructor for generating csv column headings for SL ExecutiveData- Filter Validation
	public DashboardSLExecutiveDataResultInfo() {
		super("TestMethodName", "FilterName", "FilterOption", "Payload", "isSLExecutiveDataResValidJson", "Y-Axis Value", "MandatoryParameterForY-Axis", "X-Axis Value", "PayloadForDifferentAxisValues", "isSlExecutiveDataValidJson", "isClickableChartsFound", "AdditionalParamForClickableCharts", "PayloadForClickableChartData", "isClickableChartValidJson", "PayloadForComparisonTab", "isComparisonTabResValidJson", "TestStatus");
	}

	//Constructor for appending rows in csv for SL ExecutiveData- Filter Validation
	public DashboardSLExecutiveDataResultInfo(String testMethodName, String filterName, String filterOption, String payload, String isSLExecutiveDataResValidJson, String yAxisValue, String mandatoryParam, String xAxisValue, String payloadForDifferentAxisValues, String isSlExecutiveDataValidJson, String isClickableChartsFound, String additionalParamForClickableCharts, String payloadForClickableChartData, String isClickableChartValidJson, String payloadForComparisonTab, String isComparisonTabResValidJson, String testStatus) {

		super(testMethodName, filterName, filterOption, payload, isSLExecutiveDataResValidJson, yAxisValue, mandatoryParam, xAxisValue, payloadForDifferentAxisValues, isSlExecutiveDataValidJson, isClickableChartsFound, additionalParamForClickableCharts, payloadForClickableChartData, isClickableChartValidJson, payloadForComparisonTab, isComparisonTabResValidJson, testStatus);
	}

	//Constructor for generating csv column heading for SL Performance- Sorting Order Validation
	public DashboardSLExecutiveDataResultInfo(String testSortingOrder) {
		super("isMetaDataValidJson", "isFilterDataValidJson", "PrimaryFilterName", "SortingCriteria", "SortingOrder", "SlaChartPayload", "isSlaChartValidJsonResponse", "isResponseInSortedOrder", "TestStatus");
	}

	//Constructor for appending rows in csv for SL Performance Sorting Order Validation
	/*public DashboardSLExecutiveDataResultInfo(String isMetaDataValidJson, String isFilterDataValidJson, String primaryFilterName, String sortingCriteria, String sortingOrder, String slaChartPayload, String isSlaChartValidJsonResponse, String isResponseInSortedOrder, String testStatus) {
		super(isMetaDataValidJson, isFilterDataValidJson, primaryFilterName, sortingCriteria, sortingOrder, slaChartPayload, isSlaChartValidJsonResponse, isResponseInSortedOrder, testStatus);
	}*/
}
