package com.sirionlabs.api.clientAdmin.customField;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.test.invoice.ProformaInvoice;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CustomField extends TestAPIBase {

    private Document html;
    private String listUrl = "/dynamicMetadata/list";
    private boolean customFieldAlreadyPresent = false;

    private static Logger logger = LoggerFactory.getLogger(ProformaInvoice.class);

    public Document getHtmlFromListing(CustomAssert customAssert){

        try {
            APIValidator apiValidator = executor.get(listUrl,null);
            APIResponse apiResponse = apiValidator.getResponse();
            String res = apiResponse.getResponseBody();

            Document html;

            html = Jsoup.parse(res);
            this.html = html;
            return this.html;
        }
        catch (Exception e){
            customAssert.assertTrue(false,"Exception caught while fetching html from url "+listUrl);
            logger.info("Exception caught while fetching html from url {}",listUrl);
        }

        return null;
    }

    public void createCustomField(String customFieldName, CustomAssert customAssert,String sectionName){

        try{
            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "text/html, */*; q=0.01");
            headers.put("Accept-Encoding", "gzip, deflate");
            headers.put("Content-Type", "application/x-www-form-urlencoded");
            headers.put("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));

            Map<String,String> tempParams ;
            Map<String,String> commonParams ;
            Map<String,String> params = new HashMap<>();

            commonParams = ParseConfigFile.getAllConstantPropertiesCaseSensitive(ConfigureConstantFields.getConstantFieldsProperty("customFieldCreationParamsPath"),ConfigureConstantFields.getConstantFieldsProperty("customFieldCreationParamsName"),"common");
            String temp;
            for(Map.Entry<String,String> entry : commonParams.entrySet()){
                if(entry.getKey().contains("--")) {
                    temp = entry.getKey().replaceAll("--", "[0].");
                    params.put(temp, entry.getValue());
                }
                else if(entry.getKey().contains("-")) {
                    temp = entry.getKey().replaceAll("-", ".");
                    params.put(temp, entry.getValue());
                }
                else{
                    params.put(entry.getKey(),entry.getValue());
                }
            }

            tempParams = ParseConfigFile.getAllConstantPropertiesCaseSensitive(ConfigureConstantFields.getConstantFieldsProperty("customFieldCreationParamsPath"),ConfigureConstantFields.getConstantFieldsProperty("customFieldCreationParamsName"),sectionName);
            for(Map.Entry<String,String> entry : tempParams.entrySet()){
                String replaceString = "-";
                int index ;
                while(entry.getKey().contains(replaceString))
                    replaceString = replaceString.concat("-");
                replaceString = replaceString.substring(1);
                if(replaceString.length()>=2){
                    index = replaceString.length()-2;
                        temp = entry.getKey().replaceAll(replaceString, "["+index+"].");
                        params.put(temp, entry.getValue());
                }
                if(entry.getKey().contains("-")) {
                    temp = entry.getKey().replaceAll("-", ".");
                    params.put(temp, entry.getValue());
                }
                else{
                    params.put(entry.getKey(),entry.getValue());
                }
            }

            params.put("name",customFieldName);
            params.put("displayName",customFieldName);
            params.put("_csrf_token",ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
            params.put("orderSeq",String.valueOf(getUniqueOrderSeq()).substring(0,10));

            APIValidator apiValidator = executor.postMultiPartFormData("/dynamicMetadata/create", headers, params);

            APIResponse apiResponse = apiValidator.getResponse();
        }
        catch (Exception e){
            logger.info("Exception caught at creating custom");
            customAssert.assertTrue(false,"Exception caught at creating custom");
        }
    }

    private long getUniqueOrderSeq(){
        Date date = new Date();
        return date.getTime();
    }

    public boolean checkCreateCustomField(String customFieldCreatePayloadSectionName, CustomAssert customAssert){
        return checkCreateCustomField(customFieldCreatePayloadSectionName,customFieldCreatePayloadSectionName,customAssert);
    }

    public boolean checkCreateCustomField(String customFieldName, String customFieldCreatePayloadSectionName, CustomAssert customAssert) {
        try {

            logger.info("Checking custom filed name [{}], payload section name [{}]", customFieldName, customFieldCreatePayloadSectionName);
            logger.info("checking if the required custom field is present or not");
            boolean customFieldFound = checkCustomFieldForFieldType(customAssert, customFieldName); // checking if the required custom field is present or not

            if (!customFieldFound) {
                logger.info("creating the custom field if not present");
                new CustomField().createCustomField(customFieldName, customAssert, customFieldCreatePayloadSectionName); //creating the custom field if not present

                logger.info("checking the custom field after creation");
                customFieldFound = checkCustomFieldForFieldType(customAssert, customFieldName); //checking the custom field after creation

                if (!customFieldFound) {
                    logger.info("Custom field not found and not even created hence terminating [{}]", customFieldName);
                    customAssert.assertTrue(false, "Custom field not found and not even created hence terminating " + customFieldName);
                    customAssert.assertAll();
                } else {
                    logger.info("Custom field found successfully after creating");
                    customAssert.assertTrue(true, "Custom field found successfully after creating");
                    return true;
                }
            } else {
                logger.info("Custom field is already present");
                customAssert.assertTrue(true, "Custom field is already present");
                customFieldAlreadyPresent = true;
                return true;
            }
        } catch (Exception e) {
            logger.info("Exception caught while creating rule [{}]", e.toString());
            customAssert.assertTrue(false, "Exception caught while creating custom field " + e.toString());
            return false;
        }
        return false;
    }

    private boolean checkCustomFieldForFieldType(CustomAssert customAssert, String customFieldName) {

        try {
            Document html = getHtmlFromURLClientAdmin(customAssert);
            if (html == null)
                return false;
            int htmlLength = html.getElementById("_title_pl_com_sirionlabs_model_MasterGroup_id").getElementsByTag("a").size();
            String temp;
            for (int index = 0; index < htmlLength; index++) {
                temp = html.getElementById("_title_pl_com_sirionlabs_model_MasterGroup_id").getElementsByTag("a").get(index).getElementsByTag("a").text();
                if (temp.equalsIgnoreCase(customFieldName)) {
                    return true;
                }
            }
        } catch (Exception e) {
            logger.info("Exception caught while parsing html document");
            customAssert.assertTrue(false, "Exception caught while parsing html document");
        }
        return false;
    }

    private Document getHtmlFromURLClientAdmin(CustomAssert customAssert) {
        try {
            return new CustomField().getHtmlFromListing(customAssert);
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception caught while fetching html");
            logger.info("Exception caught while fetching html");
        }
        return null;
    }

    public boolean getCustomFieldAlreadyPresent(){
        return customFieldAlreadyPresent;
    }

}
