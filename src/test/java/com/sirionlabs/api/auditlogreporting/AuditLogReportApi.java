package com.sirionlabs.api.auditlogreporting;


import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class AuditLogReportApi extends TestAPIBase {
    private final static Logger logger = LoggerFactory.getLogger(AuditLogReportApi.class);

    private  String getAuditLogReportFetchDataPath() {
          return "/auditlog/fetch-search-data";
    }

    public String hitAuditLogReportFetchDataApi(String payload){
        String response;
        String AuditLogReportConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("AuditLogReportConfigFilePath");
        String AuditLogReportConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("AuditLogReportConfigFileName");
        String hosturl =ParseConfigFile.getValueFromConfigFile(AuditLogReportConfigFilePath,AuditLogReportConfigFileName,"default","scheme")
               + "://"+ParseConfigFile.getValueFromConfigFile(AuditLogReportConfigFilePath,AuditLogReportConfigFileName,"default","ip")
                + ":"+ParseConfigFile.getValueFromConfigFile(AuditLogReportConfigFilePath,AuditLogReportConfigFileName,"default","port");

        response = executor.post(hosturl,getAuditLogReportFetchDataPath(), getHeaders(), payload).getResponse().getResponseBody();
        return response;
    }

    private  HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return headers;
    }

}
