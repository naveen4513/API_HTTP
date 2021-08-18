package com.sirionlabs.utils.csvutils;

/**
 * @author manoj.upreti
 */
public class ReportResultInfoClass extends ResultInfoClass {

	public ReportResultInfoClass() {
		super("EntityId", "EntityName", "ReportID", "ReportName", "FilterId", "FilterName", "RequestPayload", "isResponceValidJson", "Remark", "Offset", "PageSize", "OrderByColumnName", "OrderDirection");
	}

	public ReportResultInfoClass(String entityId, String entityName, String reportID, String reportName, String filterId, String filterName, String requestPayload, String isResponceValidJson, String remark, String offset, String pageSize, String orderByColumnName, String orderDirection) {
		super(entityId, entityName, reportID, reportName, filterId, filterName, requestPayload, isResponceValidJson, remark, pageSize, offset, orderByColumnName, orderDirection);
	}
}
