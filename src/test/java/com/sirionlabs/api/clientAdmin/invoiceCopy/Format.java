package com.sirionlabs.api.clientAdmin.invoiceCopy;

import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class Format extends TestAPIBase {

    private String getAPIPathCreate() {
        return "/invoice-template-format";
    }

    private String getAPIPathGetFormat(String formatId) {
        return "/invoice-template-format/" + formatId;
    }

    private String getAPIPathUpdateFormat() {
        return "/invoice-template-format";
    }

    private String getAPIPathDeleteFormat(String formatId) {
        return "/invoice-template-format/" + formatId;
    }

    private String getAPIPathTemplateFormatList(String clientId) {
        return "/invoice-template-format/list?clientId=" + clientId;
    }

    public HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Content-Type", "application/json; charset=utf-8");

        return headers;
    }

    public String createFormat(String payload) {

        String responseBody = null;
        try {
            responseBody = executor.post(getAPIPathCreate(), getHeaders(),payload).getResponse().getResponseBody();

        }catch (Exception e){

        }
        return responseBody;
    }

    public String getFormat(String formatId) {

        String responseBody = null;
        try {
            responseBody = executor.get(getAPIPathGetFormat(formatId), getHeaders()).getResponse().getResponseBody();

        }catch (Exception e){

        }
        return responseBody;
    }

    public String updateFormat(String payload) {

        String responseBody = null;
        try {
            responseBody = executor.put(getAPIPathUpdateFormat(), getHeaders(),payload).getResponse().getResponseBody();

        }catch (Exception e){

        }
        return responseBody;
    }

    public String deleteFormat(String formatId) {

        String responseBody = null;
        try {
            responseBody = executor.delete(getAPIPathDeleteFormat(formatId), getHeaders()).getResponse().getResponseBody();

        }catch (Exception e){

        }
        return responseBody;
    }

    public String getFormatList(String clientId) {

        String responseBody = null;
        try {
            responseBody = executor.get(getAPIPathTemplateFormatList(clientId), getHeaders()).getResponse().getResponseBody();

        }catch (Exception e){

        }
        return responseBody;
    }

}
