package com.sirionlabs.test.invoiceLineItem;

import com.sirionlabs.api.clientAdmin.fieldLabel.FieldRenaming;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.UpdateAccount;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientSetup.ClientSetupHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreditDebitMemoAdjustmentValidation extends TestAPIBase {
    private final static Logger logger = LoggerFactory.getLogger(CreditDebitMemoAdjustmentValidation.class);

    @Test
    public void VerifyClientAdminConfigureAdjustmentLineItemType() {
        CustomAssert customAssert = new CustomAssert();

        int random = RandomNumbers.getRandomNumberWithinRangeIndex(100,999);
        APIUtils apiUtils;
        String lineItemTypeName = "line_item_"+random+"a";
        String lineItemTypeName2 = "line_item_"+random+"b";

        try {
            logger.info("Line item type names : {}, {}",lineItemTypeName,lineItemTypeName2);
            ClientSetupHelper clientSetupHelper = new ClientSetupHelper();
            clientSetupHelper.loginWithSuperAdmin();
            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "text/html, */*; q=0.01");
            headers.put("Accept-Encoding", "gzip, deflate");
            headers.put("Content-Type", "application/x-www-form-urlencoded");
            headers.put("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));

            Map<String, String> params = new HashMap<>();
            params.put("name", lineItemTypeName);
            params.put("ajax", "true");

            APIValidator apiValidator = executor.postMultiPartFormData("/lineitemtypes/create", headers, params);
            APIResponse apiResponse = apiValidator.getResponse();
            String response = apiResponse.getResponseBody();

            if (response.contains("This name already exists in the system")) {
                logger.info("Cannot create line item type in Super Admin, hence terminating");
                customAssert.assertFalse(true, "Cannot create line item type in Super Admin");
                customAssert.assertAll();
            }

            headers = new HashMap<>();
            headers.put("Accept", "text/html, */*; q=0.01");
            headers.put("Accept-Encoding", "gzip, deflate");
            headers.put("Content-Type", "application/x-www-form-urlencoded");
            headers.put("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));

            params = new HashMap<>();
            params.put("name", lineItemTypeName2);
            params.put("ajax", "true");

            apiValidator = executor.postMultiPartFormData("/lineitemtypes/create", headers, params);
            apiResponse = apiValidator.getResponse();
            response = apiResponse.getResponseBody();

            if (response.contains("This name already exists in the system")) {
                logger.info("Cannot create second line item type in Super Admin, hence terminating");
                customAssert.assertFalse(true, "Cannot create second line item type in Super Admin");
                customAssert.assertAll();
            }

            Check check = new Check();
            check.hitCheck("naveen_admin", "admin123");
            apiUtils = new APIUtils();
            HttpGet httpGet;
            HttpResponse httpResponse;
            Document html;
            Elements elements;

            //starting create list item type
            logger.info("starting create list item type");
            httpGet = new HttpGet();
            httpGet = apiUtils.generateHttpGetRequestWithQueryString("/lineitemtypes/create", "");
            httpGet.addHeader("Content-Type", "text/html;charset=UTF-8");
            httpGet.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
            httpGet.addHeader("Accept-Encoding", "gzip, deflate");

            httpResponse = apiUtils.getRequest(httpGet);
            response = EntityUtils.toString(httpResponse.getEntity());
            html = Jsoup.parse(response);
            int optionLength = html.getElementById("_id_id").getElementsByTag("option").size() - 1;
            elements = html.getElementById("_id_id").getElementsByTag("option");
            int optionId = -1;
            for (int optionIndex = 1; optionIndex <= optionLength; optionIndex++) {
                if (elements.get(optionIndex).getElementsByTag("option").text().equalsIgnoreCase(lineItemTypeName)) {
                    optionId = Integer.valueOf(elements.get(optionIndex).getElementsByTag("option").attr("value"));
                    break;
                }
            }
            if (optionId == -1) {
                logger.info("Created line item type through super admin is not present in the client admin, hence terminating");
                customAssert.assertFalse(true, "Created line item type through super admin is not present in the client admin");
            }

            params = new HashMap<>();
            params.put("id", String.valueOf(optionId));
            params.put("name", lineItemTypeName);
            params.put("validationAllowed", "true");
            params.put("_validationAllowed", "on");
            params.put("active", "true");
            params.put("_active", "on");
            params.put("_adjustmentAllowed", "on");
            params.put("adjustmentLineItemType.id", "");
            params.put("ajax", "true");

            apiValidator = executor.postMultiPartFormData("/lineitemtypes/createClientLineItemType", headers, params);
            apiResponse = apiValidator.getResponse();
            response = apiResponse.getResponseBody();

            httpGet = apiUtils.generateHttpGetRequestWithQueryString("/lineitemtypes/list", "");
            httpGet.addHeader("Content-Type", "text/html;charset=UTF-8");
            httpGet.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
            httpGet.addHeader("Accept-Encoding", "gzip, deflate");

            httpResponse = apiUtils.getRequest(httpGet);
            response = EntityUtils.toString(httpResponse.getEntity());
            html = Jsoup.parse(response);
            elements = html.getElementById("l_com_sirionlabs_model_LineItemType").getElementsByTag("tr");
            int listLength = elements.size();
            List<String> urlList = new ArrayList<>();
            List<String> forLineItemTypeList = new ArrayList<>();
            List<String> lineItemTypeList = new ArrayList<>();


            for (int urlIndex = 1; urlIndex < listLength; urlIndex++) {
                String url = html.getElementById("l_com_sirionlabs_model_LineItemType").getElementsByTag("tr").get(urlIndex).getElementsByTag("td").get(0).getElementsByTag("a").attr("href").toString().split(";")[0];
                if (html.getElementById("l_com_sirionlabs_model_LineItemType").getElementsByTag("tr").get(urlIndex).getElementsByTag("td").get(4).getElementsByClass("listTdDivFont").text().equalsIgnoreCase("yes"))
                    urlList.add(url);
                else {
                    lineItemTypeList.add(html.getElementById("l_com_sirionlabs_model_LineItemType").getElementsByTag("tr").get(urlIndex).getElementsByTag("td").get(2).text());
                }
            }

            for (int urlIndex = 0; urlIndex < urlList.size(); urlIndex++) {

                httpGet = new HttpGet();
                httpGet = apiUtils.generateHttpGetRequestWithQueryString(urlList.get(urlIndex), "");
                httpGet.addHeader("Content-Type", "text/html;charset=UTF-8");
                httpGet.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
                httpGet.addHeader("Accept-Encoding", "gzip, deflate");

                httpResponse = apiUtils.getRequest(httpGet);
                response = EntityUtils.toString(httpResponse.getEntity());
                html = Jsoup.parse(response);
                try {
                    forLineItemTypeList.add(html.getElementById("_s_com_sirionlabs_model_lineitemtype_forlineitemtype_adjustmentLineItemType.name_id").text());
                }
                catch (Exception e){}
            }


            logger.info("starting second create list item type");
            httpGet = new HttpGet();
            httpGet = apiUtils.generateHttpGetRequestWithQueryString("/lineitemtypes/create", "");
            httpGet.addHeader("Content-Type", "text/html;charset=UTF-8");
            httpGet.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");
            httpGet.addHeader("Accept-Encoding", "gzip, deflate");

            httpResponse = apiUtils.getRequest(httpGet);
            response = EntityUtils.toString(httpResponse.getEntity());
            html = Jsoup.parse(response);
            optionLength = html.getElementById("_id_id").getElementsByTag("option").size() - 1;
            elements = html.getElementById("_id_id").getElementsByTag("option");
            optionId = -1;
            for (int optionIndex = 1; optionIndex <= optionLength; optionIndex++) {
                if (elements.get(optionIndex).getElementsByTag("option").text().equalsIgnoreCase(lineItemTypeName2)) {
                    optionId = Integer.valueOf(elements.get(optionIndex).getElementsByTag("option").attr("value"));
                    break;
                }
            }
            if (optionId == -1) {
                logger.info("Created line item type through super admin is not present in the client admin, hence terminating");
                customAssert.assertFalse(true, "Created line item type through super admin is not present in the client admin");
            }

            optionLength = html.getElementById("_adjustmentLineItemType.id_id").getElementsByTag("option").size() - 1;
            elements = html.getElementById("_adjustmentLineItemType.id_id").getElementsByTag("option");
            int optionId2 = -1;
            String optionValue;
            for (int optionIndex = 1; optionIndex <= optionLength; optionIndex++) {
                optionValue = elements.get(optionIndex).getElementsByTag("option").text();
                if (forLineItemTypeList.contains(optionValue) || !lineItemTypeList.contains(optionValue)) {
                    logger.info("For List Type Item value {} should not be present, hence terminating program", optionValue);
                    customAssert.assertFalse(true, "For List Type Item value " + optionValue + " should not be present, hence terminating program");
                }
                if (optionValue.equalsIgnoreCase(lineItemTypeName)) {
                    optionId2 = Integer.valueOf(elements.get(optionIndex).getElementsByTag("option").attr("value"));
                }
            }

            if (optionId2 == -1) {
                logger.info("Created line item type through super admin is not present in the client admin, hence terminating");
                customAssert.assertFalse(true, "Created line item type through super admin is not present in the client admin");
            }


            params = new HashMap<>();
            params.put("id", String.valueOf(optionId));
            params.put("name", lineItemTypeName2);
            params.put("validationAllowed", "true");
            params.put("_validationAllowed", "on");
            params.put("_active", "on");
            params.put("adjustmentAllowed", "true");
            params.put("_adjustmentAllowed", "on");
            params.put("adjustmentLineItemType.id", String.valueOf(optionId2));
            params.put("ajax", "true");

            apiValidator = executor.postMultiPartFormData("/lineitemtypes/createClientLineItemType", headers, params);
            apiResponse = apiValidator.getResponse();
            response = apiResponse.getResponseBody();

            if (response.contains("This name already exists in the system")) {
                logger.info("Cannot create second line item type in client admin, hence terminating");
                customAssert.assertFalse(true, "Cannot create second line item type in Super Admin");
                customAssert.assertAll();
            }

            String query = "Delete from line_item_type where name =";
            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
            int temp1 = postgreSQLJDBC.deleteDBEntry(query+"'"+lineItemTypeName+"'");
            int temp2 = postgreSQLJDBC.deleteDBEntry(query+"'"+lineItemTypeName2+"'");
            if(temp2!=2||temp1!=2){
                logger.info("Created values not deleted and skipping deletion");
            }

        } catch (Exception e) {
            logger.info("Exception caught {}",e.toString());
            customAssert.assertFalse(true,"Exception Caught "+e.toString());
        }

        customAssert.assertAll();

    }

    @Test
    public void VerifyRenameAdjustmentTab() {

        String adminUserName = "naveen_user";
        String adminPassword = "admin123";
        Map<String, String> fieldNames = new HashMap<>();
        int random = RandomNumbers.getRandomNumberWithinRange(100, 999);
        String changedAdjustmentName = "Adjustment" + random;

        CustomAssert csAssert = new CustomAssert();
        logger.info("Starting Test TC-C88375.");

        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        FieldRenaming fieldRenamingObj = new FieldRenaming();
        UpdateAccount updateAccountObj = new UpdateAccount();

        int currentLanguageId = updateAccountObj.getCurrentLanguageIdForUser(lastLoggedInUserName, 1005);

        if (currentLanguageId == -1) {
            throw new SkipException("Couldn't get Current Language Id for User " + lastLoggedInUserName);
        }

        //Update Language Id for User.
        if (!updateAccountObj.updateUserLanguage(lastLoggedInUserName, 1005, 1000)) {
            throw new SkipException("Couldn't Change Language for User " + lastLoggedInUserName + " to Russian");
        }

        new Check().hitCheck(adminUserName, adminPassword);

        int groupId = 91;
        int fieldsReplacedCount = 0;
        try {
            String fieldRenamingResponse = fieldRenamingObj.hitFieldRenamingUpdate(1000, groupId);
            String payloadForFieldRenamingUpdate = fieldRenamingResponse;
            JSONObject jsonObject = new JSONObject(payloadForFieldRenamingUpdate);
            JSONArray jsonArray = jsonObject.getJSONArray("childGroups");
            int i;
            for (i = 0; i < jsonArray.length(); i++) {
                if (jsonArray.getJSONObject(i).get("name").toString().equalsIgnoreCase("Tab labels")) {
                    jsonArray = jsonArray.getJSONObject(i).getJSONArray("fieldLabels");
                    break;
                }
            }
            for (int counter = 0; counter < jsonArray.length(); counter++) {
                if (jsonArray.getJSONObject(counter).get("name").toString().equalsIgnoreCase("EXPECTED MEMO")) {
                    jsonArray.getJSONObject(counter).put("clientFieldName", changedAdjustmentName);
                    fieldsReplacedCount++;
                }
            }
            jsonObject.getJSONArray("childGroups").getJSONObject(i).put("fieldLabels", jsonArray);
            payloadForFieldRenamingUpdate = jsonObject.toString();

            if (fieldsReplacedCount == 0) {
                logger.info("No matching fields found, so asserting false");
                csAssert.assertFalse(true, "No matching fields found");
            } else {
                logger.info("Total No of Fields Selected for Renaming: {} for Entity {}", fieldsReplacedCount);
                String fieldUpdateResponse = fieldRenamingObj.hitFieldUpdate(payloadForFieldRenamingUpdate);

                JSONObject jsonObj = new JSONObject(fieldUpdateResponse);
                if (!jsonObj.getBoolean("isSuccess")) {
                    throw new SkipException("Couldn't update Field Labels");
                }

                new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);
                if (!updateAccountObj.updateUserLanguage(lastLoggedInUserName, 1005, 1000)) {
                    throw new SkipException("Couldn't Change Language for User " + lastLoggedInUserName + " to Russian");
                }
                //login with user credential
                //ToDo add check changes in user end
            }
        }
        catch (Exception e){
            logger.info("Exception caught : {}",e.toString());
            csAssert.assertFalse(true,"Exception caught : "+e.toString());
        }

        csAssert.assertAll();
    }
}
