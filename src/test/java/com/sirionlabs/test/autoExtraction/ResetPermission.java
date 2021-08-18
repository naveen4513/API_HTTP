package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.sirionlabs.test.docusign.TestSend.documentIds;

public class ResetPermission {
    private final static Logger logger = LoggerFactory.getLogger(ResetPermission.class);
    CustomAssert csAssert = new CustomAssert();
    SoftAssert softAssert;
    private static String configAutoExtractionFilePath;
    private static String configAutoExtractionFileName;

    @BeforeClass
    public void beforeClass()
    {
        configAutoExtractionFilePath = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFilePath");
        configAutoExtractionFileName = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFileName");

    }

    /*Verify that user is able to Reset the document when having permission from URG-->Test Case Id:C141297*/

    @Test(priority = 2,enabled = true)
    public void ResetButtonOnListing() throws IOException {
        try {

            softAssert = new SoftAssert();
            Check check = new Check();

            // Login to Client Admin
            String clientAdminUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "client admin credentials", "username");
            String clientAdminPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "client admin credentials", "password");
            HttpResponse loginResponse = check.hitCheck(clientAdminUserName, clientAdminPassword);
            softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 200, "Response is not valid");

            String url = "/masteruserrolegroups/update/";
            String roleGroupId = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "rolegroupid");
            HttpResponse masterUserRoleGroupResponse = AutoExtractionHelper.hitMasterUserRoleGroup(url, roleGroupId);
            softAssert.assertTrue(masterUserRoleGroupResponse.getStatusLine().getStatusCode() == 200, "Master Role Group API Response is not valid");
            String masterUserRoleGroupResponseStr = EntityUtils.toString(masterUserRoleGroupResponse.getEntity());
            org.jsoup.nodes.Document document = Jsoup.parse(masterUserRoleGroupResponseStr);
            List<String> allSections = document.select("#permission h3").stream().map(m -> m.text()).collect(Collectors.toList());
            int autoExtractionPermssionIndex = 0;
            for (int i = 0; i < allSections.size(); i++) {
                if (allSections.get(i).equals("AUTOEXTRACTION:")) {
                    autoExtractionPermssionIndex = i;
                    break;
                }

            }
            List<String> allSectionsValue = document.select("#permission div").stream().map(m -> m.text()).collect(Collectors.toList());
            softAssert.assertTrue(allSectionsValue.get(autoExtractionPermssionIndex).contains(" Auto Extraction Doc Reset"), " Auto Extraction Doc Reset is not present under global permissions section");
            FileUtils fileUtils;
            Map<String, String> formData;
            Map<String, String> keyValuePair;
            String params;
            HttpResponse httpResponse;
            String endUserName = null;
            String endUserPassword = null;
            // Switch on the Reset Document Check Box
            try {
                fileUtils = new FileUtils();
                formData = new LinkedHashMap<>();
                keyValuePair = fileUtils.ReadKeyValueFromFile("src/test/resources/TestConfig/AutoExtraction/RoleGroup/AutoExtractionResetPermissionOn.txt", ":", "RoleGroup");
                for (Map.Entry<String, String> m : keyValuePair.entrySet()) {
                    formData.put(m.getKey().trim(), m.getValue().trim());
                }
                params = UrlEncodedString.getUrlEncodedString(formData);
                httpResponse = AutoExtractionHelper.updateAccessCriteria("/masteruserrolegroups/update", params);
                softAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 302, "Role Group API Response Code is not valid");

                // Login to End User
                endUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "username");
                endUserPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "password");
                loginResponse = check.hitCheck(endUserName, endUserPassword);
                softAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid");

                //Applying filter on AE doc listing to get documents having status : Completed
                String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0";
                String payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"368\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"4\",\"name\":\"COMPLETED\"}]},\"filterId\":368,\"filterName\":\"statusId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\":19105,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":18673,\"columnQueryName\":\"documentname\"},{\"columnId\":18674,\"columnQueryName\":\"contracttype\"},{\"columnId\":18770,\"columnQueryName\":\"projects\"},{\"columnId\":18675,\"columnQueryName\":\"status\"},{\"columnId\":19121,\"columnQueryName\":\"metadatacount\"},{\"columnId\":19122,\"columnQueryName\":\"clausecount\"},{\"columnId\":18685,\"columnQueryName\":\"totalpages\"}]}";
                HttpResponse filteredListResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query, payload);
                softAssert.assertTrue(filteredListResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid");
                String filteredResponseStr = EntityUtils.toString(filteredListResponse.getEntity());
                JSONObject filteredResponseJson = new JSONObject(filteredResponseStr);

                //Getting Document Id out of filtered List of Documents
                int columnId = ListDataHelper.getColumnIdFromColumnName(filteredResponseStr, "documentname");
                int documentId = Integer.parseInt(filteredResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("value").toString().split(":;")[1]);

                //Now hitting the document reset API for only Metadata
                logger.info("Resetting the document");
                query = "/autoextraction/redo";
                payload = "{\"documentIds\":["+documentId+"]}";
                HttpResponse resetDocumentResponse = AutoExtractionHelper.aeDocumentReset(query, payload);
                softAssert.assertTrue(resetDocumentResponse.getStatusLine().getStatusCode() == 200, "Response code is not valid");
                Thread.sleep(500);

                //Validating the Document status after resetting the document
                query = "/" + "autoextraction" + "/" + "document" + "/" + documentId + "/metadata";
                HttpResponse getStatusOfDocument = AutoExtractionHelper.getStatusOfDocumentAPI(query);
                softAssert.assertTrue(getStatusOfDocument.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
                String getStatusOfDocumentStr = EntityUtils.toString(getStatusOfDocument.getEntity());
                JSONObject getStatusOfDocumentJson = new JSONObject(getStatusOfDocumentStr);
                String statusOfDoc = String.valueOf(getStatusOfDocumentJson.getJSONObject("response").getJSONObject("autoExtractStatus").get("name"));
                softAssert.assertTrue(statusOfDoc.contains("SUBMITTED") || statusOfDoc.contains("INPROGRESS"), "Document Reset is not working");
                softAssert.assertAll();

            } catch (Exception e) {
                logger.info("Exception occured while hitting user role group update API");
            }


        }

        catch (Exception e)
        {
            logger.info("Exception occured while hitting Redo API");
        }
    }


    /*Verify that user is not able to Reset the document when user is not having permission from URG-->Test Case Id:C141296*/
    @Test(priority=1,enabled = true)
    public void ResetButtonOffListing() throws IOException {
        try {
                CustomAssert customAssert = new CustomAssert();
            Check check = new Check();

            // Login to Client Admin
            String clientAdminUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "client admin credentials", "username");
            String clientAdminPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "client admin credentials", "password");
            HttpResponse loginResponse = check.hitCheck(clientAdminUserName, clientAdminPassword);
            csAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 200, "Response is not valid");

            String url = "/masteruserrolegroups/update/";
            String roleGroupId = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "rolegroupid");
            HttpResponse masterUserRoleGroupResponse = AutoExtractionHelper.hitMasterUserRoleGroup(url, roleGroupId);
            csAssert.assertTrue(masterUserRoleGroupResponse.getStatusLine().getStatusCode() == 200, "Master Role Group API Response is not valid");
            String masterUserRoleGroupResponseStr = EntityUtils.toString(masterUserRoleGroupResponse.getEntity());
            org.jsoup.nodes.Document document = Jsoup.parse(masterUserRoleGroupResponseStr);
            List<String> allSections = document.select("#permission h3").stream().map(m -> m.text()).collect(Collectors.toList());
            int autoExtractionPermssionIndex = 0;
            for (int i = 0; i < allSections.size(); i++) {
                if (allSections.get(i).equals("AUTOEXTRACTION:")) {
                    autoExtractionPermssionIndex = i;
                    break;
                }

            }
            List<String> allSectionsValue = document.select("#permission div").stream().map(m -> m.text()).collect(Collectors.toList());
            csAssert.assertTrue(allSectionsValue.get(autoExtractionPermssionIndex).contains(" Auto Extraction Doc Reset"), " Auto Extraction Doc Reset is not present under global permissions section");
            FileUtils fileUtils;
            Map<String, String> formData;
            Map<String, String> keyValuePair;
            String params;
            HttpResponse httpResponse;
            String endUserName = null;
            String endUserPassword = null;
            // Switch off the Reset Document Check Box
            try {
                fileUtils = new FileUtils();
                formData = new LinkedHashMap<>();
                keyValuePair = fileUtils.ReadKeyValueFromFile("src/test/resources/TestConfig/AutoExtraction/RoleGroup/AutoExtractionResetPermissionOff.txt", ":", "RoleGroup");
                for (Map.Entry<String, String> m : keyValuePair.entrySet()) {
                    formData.put(m.getKey().trim(), m.getValue().trim());
                }
                params = UrlEncodedString.getUrlEncodedString(formData);
                httpResponse = AutoExtractionHelper.updateAccessCriteria("/masteruserrolegroups/update", params);
                csAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200, "Role Group API Response Code is not valid");

                // Login to End User
                endUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "username");
                endUserPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "password");
                loginResponse = check.hitCheck(endUserName, endUserPassword);
                csAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 200, "Response Code is not valid");

                //Applying filter on AE doc listing to get documents having status : Completed
                String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0";
                String payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"368\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"4\",\"name\":\"COMPLETED\"}]},\"filterId\":368,\"filterName\":\"statusId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[{\"columnId\":19105,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":18673,\"columnQueryName\":\"documentname\"},{\"columnId\":18674,\"columnQueryName\":\"contracttype\"},{\"columnId\":18770,\"columnQueryName\":\"projects\"},{\"columnId\":18675,\"columnQueryName\":\"status\"},{\"columnId\":19121,\"columnQueryName\":\"metadatacount\"},{\"columnId\":19122,\"columnQueryName\":\"clausecount\"},{\"columnId\":18685,\"columnQueryName\":\"totalpages\"}]}";
                HttpResponse filteredListResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query, payload);
                csAssert.assertTrue(filteredListResponse.getStatusLine().getStatusCode()==200,"Response Code is not valid");
                String filteredResponseStr = EntityUtils.toString(filteredListResponse.getEntity());
                JSONObject filteredResponseJson = new JSONObject(filteredResponseStr);

                //Getting Document Id out of filtered List of Documents
                int columnId = ListDataHelper.getColumnIdFromColumnName(filteredResponseStr, "documentname");
                int documentId= Integer.parseInt(filteredResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("value").toString().split(":;")[1]);

                //Now hitting the document reset API

                query = "/autoextraction/redo";
                payload = "{\"documentIds\":["+documentId+"]}";
                HttpResponse resetDocumentResponse = AutoExtractionHelper.aeDocumentReset(query,payload);
                csAssert.assertTrue(resetDocumentResponse.getStatusLine().getStatusCode()==200,"Response code is not valid");
                String resetDocumentResponseStr = EntityUtils.toString(resetDocumentResponse.getEntity());
                JSONObject resetDocumentJson = new JSONObject(resetDocumentResponseStr);
                String errorMessage = (String) resetDocumentJson.getJSONObject("header").getJSONObject("response").get("errorMessage");
                csAssert.assertTrue(errorMessage.contains("Either you do not have the required permissions or requested page does not exist anymore."),"User is able to reset the document even if he is not having permission");

                //Validating the Document status should not get changed even if user is not having reset the document Permission
                query= "/" +"autoextraction"+ "/"+"document"+"/"+documentId + "/metadata";
                HttpResponse getStatusOfDocument = AutoExtractionHelper.getStatusOfDocumentAPI(query);
                csAssert.assertTrue(getStatusOfDocument.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
                String getStatusOfDocumentStr = EntityUtils.toString(getStatusOfDocument.getEntity());
                JSONObject getStatusOfDocumentJson = new JSONObject(getStatusOfDocumentStr);
                String statusOfDoc = String.valueOf(getStatusOfDocumentJson.getJSONObject("response").getJSONObject("autoExtractStatus").get("name"));
                csAssert.assertTrue(statusOfDoc.contains("COMPLETE")|| statusOfDoc.contains("INPROGRESS"),"Document Reset is not working");
            }
            catch (Exception e)
            {
                logger.info("Exception occured while hitting user role group update API"+ e.getMessage());
            }


        } catch (Exception e) {
            logger.info("Exception occured while hitting User Role Group API"+ e.getMessage());
        }
        csAssert.assertAll();
    }
}


