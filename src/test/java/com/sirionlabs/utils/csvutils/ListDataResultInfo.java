package com.sirionlabs.utils.csvutils;

public class ListDataResultInfo extends ResultInfoClass {

	/*Constructor for generating column heading in listData test report*/
	public ListDataResultInfo() {
		super("EntityName", "APIName", "isApiResponseValidJson", "isApplicationError", "isPermissionDenied", "ResponseTime(sec)", "TestStatus");
	}

	/*Constructor for appending rows in listData test report*/
	public ListDataResultInfo(String entityName, String apiName, String isApiResponseValidJson, String isApplicationError, String isPermissionDenied, String responseTime, String testStatus) {
		super(entityName, apiName, isApiResponseValidJson, isApplicationError, isPermissionDenied, responseTime, testStatus);
	}
}
