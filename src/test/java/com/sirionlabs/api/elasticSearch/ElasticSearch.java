package com.sirionlabs.api.elasticSearch;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;
import java.util.Map;

public class ElasticSearch extends TestAPIBase {

    String hostUrl = "http://192.168.2.235:9400";

    public String getAPIPathDeleteByQuery(String indexId) {
        return "/" + indexId + "/_delete_by_query/";
    }

    public HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }

    public HashMap<String, String> getHeadersFormType() {

        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept","*/*");
        headers.put("Accept-Encoding","gzip, deflate");
        headers.put("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");
        headers.put("X-Requested-With", "XMLHttpRequest");
        return headers;
    }

    public String deleteIndexData(String index,String payload) {

        String responseBody = null;
        try {
            responseBody = executor.post(hostUrl,getAPIPathDeleteByQuery(index), getHeaders(),payload).getResponse().getResponseBody();

        }catch (Exception e){
            System.out.println();
        }
        return responseBody;
    }

}
