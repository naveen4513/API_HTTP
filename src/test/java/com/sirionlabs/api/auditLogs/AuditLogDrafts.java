package com.sirionlabs.api.auditLogs;

import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class AuditLogDrafts extends TestAPIBase {

    public static String getApiPath(int entityTypeId, String logId) {
        return "/tblauditlogs/drafts/" + entityTypeId + "/" + logId;
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }

    public static String getDraftsResponse(int entityTypeId, String logId) {
        return executor.get(getApiPath(entityTypeId, logId), getHeaders()).getResponse().getResponseBody();
    }
}