package com.sirionlabs.api.ServiceLevel;

import com.sirionlabs.helper.api.APIExecutor;
import com.sirionlabs.helper.api.APIResponse;

import java.util.HashMap;

public class SLIF_Schemas {

    public static APIResponse hitGetSchemaDetails(APIExecutor executor, String toolId) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        String queryString = "/slintegration/source/request-schema/"+toolId+"";
        return executor.get(queryString,headers).getResponse();
    }

    public static APIResponse hitGetAuthDetails(APIExecutor executor, String toolId) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        String queryString = "/slintegration/auth-schema/"+toolId+"";
        return executor.get(queryString,headers).getResponse();
    }

    public static APIResponse hitGetCreateSource(APIExecutor executor) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        String queryString = "/slintegration/source/new";
        return executor.get(queryString,headers).getResponse();
    }

    public static APIResponse hitPostCreateSource(APIExecutor executor, String payload) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        String queryString = "/slintegration/source/create";
        return executor.post(queryString,headers,payload).getResponse();
    }

    public static APIResponse hitGetEditSource(APIExecutor executor, int sourceId) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        String queryString = "/slintegration/source/edit/"+sourceId+"";
        return executor.get(queryString,headers).getResponse();
    }

    public static APIResponse hitPostEditSource(APIExecutor executor, String payload) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        String queryString = "/slintegration/source/edit";
        return executor.post(queryString,headers,payload).getResponse();
    }

    public static APIResponse hitGetEditDestination(APIExecutor executor, int destinationId) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        String queryString = "/slintegration/destination/edit/"+destinationId+"";
        return executor.get(queryString,headers).getResponse();
    }

    public static APIResponse hitPostEditDestination(APIExecutor executor, String payload) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        String queryString = "/slintegration/destination/edit";
        return executor.post(queryString,headers,payload).getResponse();
    }

    public static APIResponse hitGetShowSource(APIExecutor executor, int sourceId) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        String queryString = "/slintegration/source/show/"+sourceId+"";
        return executor.get(queryString,headers).getResponse();
    }

    public static APIResponse hitGetShowDestination(APIExecutor executor, int destinationId) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        String queryString = "/slintegration/destination/show/"+destinationId+"";
        return executor.get(queryString,headers).getResponse();
    }

    public static APIResponse hitPostCreateDestination(APIExecutor executor, String payload) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        String queryString = "/slintegration/destination/create";
        return executor.post(queryString,headers,payload).getResponse();
    }
}
