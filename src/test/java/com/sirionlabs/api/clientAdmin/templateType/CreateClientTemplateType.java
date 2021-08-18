package com.sirionlabs.api.clientAdmin.templateType;

import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;
import java.util.Map;

public class CreateClientTemplateType extends TestAPIBase {

    public static String getAPIPath() {
        return "/templateTypeList/createClientTemplateType";
    }

    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "text/html, */*; q=0.01");
        headers.put("Accept-Encoding", "gzip, deflate");
        headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        return headers;
    }

    public static Map<String, String> getParamsMap(String id, String name, boolean active) {
        Map<String, String> params = new HashMap<>();

        params.put("id", id);
        params.put("name", name);
        params.put("active", String.valueOf(active));
        params.put("_active", "on");
        params.put("history", "{}");

        return params;
    }

    public static int hitCreateClientTemplateType(Map<String, String> params) {
        return executor.post(getAPIPath(), getHeaders(), null, params).getResponse().getResponseCode();
    }
}