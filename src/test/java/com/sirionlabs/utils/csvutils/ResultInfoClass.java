package com.sirionlabs.utils.csvutils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author manoj.upreti
 */
public abstract class ResultInfoClass {

	private final static Logger logger = LoggerFactory.getLogger(ResultInfoClass.class);

	Map<String, String> resultDataMap;

	ResultInfoClass(String entityId, String entityName, String reportID, String reportName, String filterId, String filterName, String requestPayload, String isResponceValidJson, String remark, String offset, String pageSize, String orderByColumnName, String orderDirection) {
		resultDataMap = new LinkedHashMap<>();
		resultDataMap.put("EntityId", entityId);
		resultDataMap.put("EntityName", entityName);
		resultDataMap.put("ReportID", reportID);
		resultDataMap.put("ReportName", reportName);
		resultDataMap.put("FilterId", filterId);
		resultDataMap.put("FilterName", filterName);
		resultDataMap.put("RequestPayload", requestPayload);
		resultDataMap.put("isResponceValidJson", isResponceValidJson);
		resultDataMap.put("Remark", remark);
		resultDataMap.put("PageSize", pageSize);
		resultDataMap.put("Offset", offset);
		resultDataMap.put("OrderByColumnName", orderByColumnName);
		resultDataMap.put("OrderDirection", orderDirection);

	}

	//Constructor for Dashboard Report-Local filters
	ResultInfoClass(String chartID, String dashboardDataRequestPayload, String isDashboardDataResponseValidJson, String currentOutputGroup, String attributeName, String option, String dashboardAnalysisRequestPayload, String isDashboardAnalysisResponseValidJson, String link, String dashboardRecordsRequestPayload, String isDashboardRecordsResponseValidJson, String dataValidation, String testStatus, String additional) {
		resultDataMap = new LinkedHashMap<>();
		resultDataMap.put("ChartID", chartID);
		resultDataMap.put("DashboardDataRequestPayload", dashboardDataRequestPayload);
		resultDataMap.put("isDashboardDataResponseValidJson", isDashboardDataResponseValidJson);
		resultDataMap.put("currentOutputGroup", currentOutputGroup);
		resultDataMap.put("AttributeName", attributeName);
		resultDataMap.put("Option", option);
		resultDataMap.put("dashboardAnalysisRequestPayload", dashboardAnalysisRequestPayload);
		resultDataMap.put("isDashboardAnalysisResponseValidJson", isDashboardAnalysisResponseValidJson);
		resultDataMap.put("link(javaScript)", link);
		resultDataMap.put("DashboardRecordsRequestPayload", dashboardRecordsRequestPayload);
		resultDataMap.put("isDashboardRecordsResponseValidJson", isDashboardRecordsResponseValidJson);
		resultDataMap.put("DataVaidation", dataValidation);
		resultDataMap.put("testStatus", testStatus);
	}

	//Constructor for Dashboard Report-Global filters
	ResultInfoClass(String chartID, String isMetaDataFilterDataResponseValidJson, String filterName, String optionId, String optionName, String dashboardDataRequestPayload, String isDashboardDataResponseValidJson, String testStatus) {
		resultDataMap = new LinkedHashMap<>();
		resultDataMap.put("ChartID", chartID);
		resultDataMap.put("isMetaDataFilterDataResponseValidJson", isMetaDataFilterDataResponseValidJson);
		resultDataMap.put("FilterName", filterName);
		resultDataMap.put("OptionId", optionId);
		resultDataMap.put("OptionName", optionName);
		resultDataMap.put("DashboardDataRequestPayload", dashboardDataRequestPayload);
		resultDataMap.put("isDashboardDataResponseValidJson", isDashboardDataResponseValidJson);
		resultDataMap.put("TestStatus", testStatus);
	}

	//Constructor for Dashboard ChartId-ChartName mapping file
	ResultInfoClass(String chartID, String chartName) {
		resultDataMap = new LinkedHashMap<>();
		resultDataMap.put("ChartID", chartID.toString());
		resultDataMap.put("ChartName", chartName);
	}

	//Constructor for ListData Results
	ResultInfoClass(String entityName, String apiName, String isApiResponseValidJson, String isApplicationError, String isPermissionDenied, String responseTime, String testStatus) {
		resultDataMap = new LinkedHashMap<>();
		//resultDataMap.put("EntityId", entityId);
		resultDataMap.put("EntityName", entityName);
		resultDataMap.put("apiName", apiName);
		resultDataMap.put("isApiResponseValidJson", isApiResponseValidJson);
		resultDataMap.put("isApplicationError", isApplicationError);
		resultDataMap.put("isPermissionDenied", isPermissionDenied);
		resultDataMap.put("responseTime", responseTime);
		resultDataMap.put("TestStatus", testStatus);
	}

	// default constructor
	ResultInfoClass() {
		logger.info("Inside Default Construct of Class : ResultInfoClass");
	}


	//Constructor for Dashboard- SL Performance results
	ResultInfoClass(String isMetaDataValidJson, String isFilterDataValidJson, String primaryFilterName, String primaryFilterOptionName, String advancedFilterName, String advancedFilterOptionName, String slachartPayload, String isSlaChartValidJsonResponse, String isDataValidationPassed, String testStatus) {
		resultDataMap = new LinkedHashMap<>();
		resultDataMap.put("isMetaDataValidJson", isMetaDataValidJson);
		resultDataMap.put("isFilterDataValidJson", isFilterDataValidJson);
		resultDataMap.put("PrimaryFilterName", primaryFilterName);
		resultDataMap.put("PrimaryFilterOptionName", primaryFilterOptionName);
		resultDataMap.put("AdvancedFilterName", advancedFilterName);
		resultDataMap.put("AdvancedFilterOptionName", advancedFilterOptionName);
		resultDataMap.put("slachartPayload", slachartPayload);
		resultDataMap.put("isSlaChartValidJsonResponse", isSlaChartValidJsonResponse);
		resultDataMap.put("isDataValidationPassed", isDataValidationPassed);
		resultDataMap.put("TestStatus", testStatus);
	}

	//Constructor for Dashboard- SL Performance Sorting Order validation
	ResultInfoClass(String isMetaDataValidJson, String isFilterDataValidJson, String primaryFilterName, String sortingCriteria, String sortingOrder, String slaChartPayload, String isSlaChartValidJsonResponse, String isResponseInSortedOrder, String testStatus) {
		resultDataMap = new LinkedHashMap<>();
		resultDataMap.put("isMetaDataValidJson", isMetaDataValidJson);
		resultDataMap.put("isFilterDataValidJson", isFilterDataValidJson);
		resultDataMap.put("PrimaryFilterName", primaryFilterName);
		resultDataMap.put("sortingCriteria", sortingCriteria);
		resultDataMap.put("sortingOrder", sortingOrder);
		resultDataMap.put("slaChartPayload", slaChartPayload);
		resultDataMap.put("isSlaChartValidJsonResponse", isSlaChartValidJsonResponse);
		resultDataMap.put("isResponseInSortedOrder", isResponseInSortedOrder);
		resultDataMap.put("TestStatus", testStatus);
	}

	//Constructor for Dashboard- SL ExecutiveData results
	ResultInfoClass(String testMethodName, String filterName, String filterOption, String payload, String isSLExecutiveDataResValidJson, String yAxisValue, String mandatoryParam, String xAxisValue, String payloadForDifferentAxisValues, String isSlExecutiveDataValidJson, String isClickableChartsFound, String additionalParamForClickableCharts, String payloadForClickableChartData, String isClickableChartValidJson, String payloadForComparisonTab, String isComparisonTabResValidJson, String testStatus) {
		resultDataMap = new LinkedHashMap<>();
		resultDataMap.put("testMethodName", testMethodName);
		resultDataMap.put("filterName", filterName);
		resultDataMap.put("filterOption", filterOption);
		resultDataMap.put("payload", payload);
		resultDataMap.put("isSLExecutiveDataResValidJson", isSLExecutiveDataResValidJson);
		resultDataMap.put("yAxisValue", yAxisValue);
		resultDataMap.put("mandatoryParam", mandatoryParam);
		resultDataMap.put("xAxisValue", xAxisValue);
		resultDataMap.put("payloadForDifferentAxisValues", payloadForDifferentAxisValues);
		resultDataMap.put("isSlExecutiveDataValidJson", isSlExecutiveDataValidJson);
		resultDataMap.put("isClickableChartsFound", isClickableChartsFound);
		resultDataMap.put("additionalParamForClickableCharts", additionalParamForClickableCharts);
		resultDataMap.put("payloadForClickableChartData", payloadForClickableChartData);
		resultDataMap.put("isClickableChartValidJson", isClickableChartValidJson);
		resultDataMap.put("payloadForComparisonTab", payloadForComparisonTab);
		resultDataMap.put("isComparisonTabResValidJson", isComparisonTabResValidJson);
		resultDataMap.put("testStatus", testStatus);
	}
}
