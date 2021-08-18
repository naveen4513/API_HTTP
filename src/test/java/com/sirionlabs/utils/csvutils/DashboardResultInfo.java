package com.sirionlabs.utils.csvutils;

public class DashboardResultInfo extends ResultInfoClass {

	/*Constructor for generating column heading in dashboard report on local filters*/
	public DashboardResultInfo() {
		super("ChartID", "DashboardDataRequestPayload", "isDashboardDataResponseValidJson", "CurrentOutputGroup", "AttributeName", "Option", "dashboardAnalysisRequestPayload", "isDashboardAnalysisResponseValidJson", "link(javaScript)", "DashboardRecordsRequestPayload", "isDashboardRecordsResponseValidJson", "DataValidation", "testStatus", "");
	}

	/*Constructor for appending rows in dashboard report on local filters*/
	public DashboardResultInfo(String chartID, String dashboardDataRequestPayload, String isDashboardDataResponseValidJson, String currentOutputGroup, String attributeName, String option, String dashboardAnalysisRequestPayload, String isDashboardAnalysisResponseValidJson, String link, String dashboardRecordsRequestPayload, String isDashboardRecordsResponseValidJson, String dataValidation, String testStatus) {
		super(chartID, dashboardDataRequestPayload, isDashboardDataResponseValidJson, currentOutputGroup, attributeName, option, dashboardAnalysisRequestPayload, isDashboardAnalysisResponseValidJson, link, dashboardRecordsRequestPayload, isDashboardRecordsResponseValidJson, dataValidation, testStatus, "");
	}

	/*Constructor for generating column heading of dashboard report on global filters*/
	public DashboardResultInfo(String testGlobalFilters) {
		super("ChartID", "isMetaDataFilterDataResponseValidJson", "FilterName", "OptionId", "OptionName", "DashboardDataRequestPayload", "isDashboardDataResponseValidJson", "TestStatus");
	}

	/*Constructor for appending rows in dashboard report on global filters*/
	public DashboardResultInfo(String chartID, String isMetaDataFilterDataResponseValidJson, String filterName, String optionId, String optionName, String dashboardDataRequestPayload, String isDashboardDataResponseValidJson, String testStatus) {
		super(chartID, isMetaDataFilterDataResponseValidJson, filterName, optionId, optionName, dashboardDataRequestPayload, isDashboardDataResponseValidJson, testStatus);
	}
}
