package com.sirionlabs.api.clientAdmin.emailConfig;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;

import java.util.HashMap;
import java.util.Map;

public class SystemEmailConfig extends TestAPIBase {


    public static String getApiPath(int entityTypeId) {
        return "/tblsystemEmailConfigurations/allentities?entityTypeId=" + entityTypeId;
    }

    public static String getLanguagesForEmailActionApiPath(int entityTypeId,String emailId) {
        return "/tblsystemEmailConfigurations/getLanguagesForEmailAction?entityTypeId=" + entityTypeId + "&emailId=" + emailId;
    }

    public static String getEmailConFigApiPath() {
        return "/tblsystemEmailConfigurations";
    }

    public static String getEmailDataApiPath(int entityTypeId,String emailId,String languageId) {
        return "/tblsystemEmailConfigurations/emaildata?entityTypeId=" + entityTypeId + "&emailAction=" + emailId + "&languageId=" + languageId;
    }
    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Content-Type", "application/json");

        return headers;
    }
    public static HashMap<String, String> getHeadersFormData() {
        HashMap<String, String> headers = new HashMap<>();

        String acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8";
        String contentTypeHeader = "multipart/form-data; boundary=----WebKitFormBoundarykggQHAjpfi6idAX5";

        headers.put("Accept", acceptHeader);
        headers.put("Content-Type", contentTypeHeader);

        return headers;
    }

    public static APIResponse getSystemEmailConfiguration(int entityTypeId) {

        APIResponse response = executor.post(getApiPath(entityTypeId), getHeaders(),null).getResponse();

        return response;
    }

    public static APIResponse getLanguagesForEmailAction(int entityTypeId,String emailId) {

        APIResponse response = executor.post(getLanguagesForEmailActionApiPath(entityTypeId,emailId), getHeaders(),null).getResponse();

        return response;
    }

    public static APIResponse getEmailData(int entityTypeId,String emailId,String languageId) {

        APIResponse response = executor.post(getEmailDataApiPath(entityTypeId,emailId,languageId), getHeaders(),"").getResponse();

        return response;
    }

    public static APIResponse setSysTemEmailConfiguration(Map<String,String> formParams) {

        APIResponse response = null;
        try {
            response = executor.postMultiPartFormData(getEmailConFigApiPath(), getHeadersFormData(), formParams).getResponse();
        }catch (Exception e){
            return response;
        }
        return response;
    }

}
