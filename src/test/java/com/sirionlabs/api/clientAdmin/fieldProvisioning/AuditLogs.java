package com.sirionlabs.api.clientAdmin.fieldProvisioning;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class AuditLogs {

	private final static Logger logger = LoggerFactory.getLogger(AuditLogs.class);

	public static String getAPIPath(int entityTypeId) {
		return "/fieldprovisioning/auditlogs/" + entityTypeId;
	}

	public static HashMap<String, String> getHeaders() {
		HashMap<String, String> headers = new HashMap<>();
		headers.put("Accept", "application/json, text/javascript, */*; q=0.01");
		headers.put("Content-Type", "application/json; charset=utf-8");

		return headers;
	}
}