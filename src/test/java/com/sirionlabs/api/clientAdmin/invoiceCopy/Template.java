package com.sirionlabs.api.clientAdmin.invoiceCopy;

import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpPost;
import org.apache.kafka.common.protocol.types.Field;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Template extends TestAPIBase {

    public String getAPIPathCreate() {
        return "/invoice-templates";
    }

    public String getAPIPathGetTemplate(String templateId) {
        return "/invoice-templates/" + templateId;
    }

    public String getAPIPathUpdateTemplate() {
        return "/invoice-templates/";
    }

    public String getAPIPathDeleteTemplate(String templateId) {
        return "/invoice-templates/" + templateId;
    }

    public String getAPIPathTemplateTemplateList(String listId) {
        return "/listRenderer/list/" + listId + "/listdata?version=2.0&isFirstCall=true&contractId=&relationId=&vendorId=&_t=1589896059975";
    }

    public String getAPIPathUploadLogo(String templateId) {
        return "/invoice-templates/" + templateId + "/logo";
    }

    public String getAPIPathDownloadLogo(String templateId) {
        return "/invoice-templates/" + templateId + "/logo";
    }

    public HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "*/*");
        headers.put("Content-Type", "application/json; charset=utf-8");
        headers.put("X-Requested-With", "XMLHttpRequest");

        return headers;
    }

    public String createTemplate(String payload) {

        String responseBody = null;
        try {
            responseBody = executor.post(getAPIPathCreate(), getHeaders(),payload).getResponse().getResponseBody();

        }catch (Exception e){

        }
        return responseBody;
    }

    public String getTemplate(String templateId) {

        String responseBody = null;
        try {
            responseBody = executor.get(getAPIPathGetTemplate(templateId), getHeaders()).getResponse().getResponseBody();

        }catch (Exception e){

        }
        return responseBody;
    }

    public String updateTemplate(String payload) {

        String responseBody = null;
        try {
            responseBody = executor.put(getAPIPathUpdateTemplate(), getHeaders(),payload).getResponse().getResponseBody();

        }catch (Exception e){

        }
        return responseBody;
    }

    public String deleteTemplate(String templateId) {

        String responseBody = null;
        try {
            responseBody = executor.delete(getAPIPathDeleteTemplate(templateId), getHeaders()).getResponse().getResponseBody();

        }catch (Exception e){

        }
        return responseBody;
    }

    public String getTemplateList(String listId,String payload) {

        String responseBody = null;
        try {
            responseBody = executor.post(getAPIPathTemplateTemplateList(listId), getHeaders(),payload).getResponse().getResponseBody();

        }catch (Exception e){

        }
        return responseBody;
    }

    public String uploadTemplateLogo(String filePath, String fileName, String templateId, String payload) {

        String responseBody = null;
        try {
            responseBody = executor.post(getAPIPathUploadLogo(templateId), getHeaders(),payload).getResponse().getResponseBody();

        }catch (Exception e){

        }
        return responseBody;
    }




    public String downloadTemplateLogo(String templateId,String filePath,String fileName) {

        String responseBody = null;
        try {
            responseBody = executor.get(getAPIPathDownloadLogo(templateId), getHeaders()).getResponse().getResponseBody();

        }catch (Exception e){

        }
        return responseBody;
    }
}
