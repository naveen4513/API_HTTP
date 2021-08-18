package com.sirionlabs.api.workflowTwoAPI;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.api.APIExecutor;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;

import java.util.HashMap;
import java.util.Map;

public class TimeZoneDataAPI {

    private static final String TimezoneDataAPIFilePath;
    private static final String TimezoneDataAPIFileName;

    private static Map<String, String> map;

    static {
        TimezoneDataAPIFilePath = ConfigureConstantFields.getConstantFieldsProperty("TimezoneDataAPIFilePath");
        TimezoneDataAPIFileName = ConfigureConstantFields.getConstantFieldsProperty("TimezoneDataAPIFileName");
    }

    public APIValidator hitGetTimezoneDataAPICall(APIExecutor executor, String domain, String timezoneId, String authToken, String isAuth) {
        HashMap<String, String> headers = new HashMap<>();
        if (isAuth.equalsIgnoreCase("yes")) {
            headers.put("Content-Type", "application/json");
            headers.put("Authorization", authToken);
        }
        String queryString = "/workflow-v2/timezone/" + timezoneId;
        return executor.getWithoutAuthorization(domain, queryString, headers);
    }

    public static Map<String, String> getAllConfigForGetTimezoneDataAPI(String section_name) {
        map = ParseConfigFile.getAllConstantProperties(TimezoneDataAPIFilePath, TimezoneDataAPIFileName, section_name);
        return map;
    }
}
