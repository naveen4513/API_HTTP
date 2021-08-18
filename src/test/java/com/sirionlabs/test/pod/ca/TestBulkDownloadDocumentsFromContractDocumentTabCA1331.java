package com.sirionlabs.test.pod.ca;

import com.sirionlabs.api.clientAdmin.masterUserRoleGroups.MasterUserRoleGroupsUpdate;
import com.sirionlabs.api.clientSetup.provisioning.ProvisioningEdit;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.SaveZipRequest;
import com.sirionlabs.api.commonAPI.ZipDownload;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.clientSetup.ClientSetupHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class TestBulkDownloadDocumentsFromContractDocumentTabCA1331 extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestBulkDownloadDocumentsFromContractDocumentTabCA1331.class);

    private String configFilePath = "src/test/resources/TestConfig/CAPod/BulkDownloadDocuments";
    private String configFileName = "TestCA1331.cfg";

    private long timeOutWaitTime = 300000;
    private long pollingTime = 15000;

    private AdminHelper adminHelperObj = new AdminHelper();
    private Check checkObj = new Check();
    private int clientId;

    private String[] entities = {"Contracts", "Contract Draft Request"};

    @BeforeClass
    public void beforeClass() {
        clientId = adminHelperObj.getClientId();
    }

    /*
    TC-C140996: Verify Bulk Download Documents Permission on Sirion Setup Admin.
     */
    @Test(enabled = true)
    public void testC140996() {
        CustomAssert csAssert = new CustomAssert();


        try {
            logger.info("Starting TC-C140996: Verify Bulk Download Documents Permission on Sirion Setup Admin");
            ClientSetupHelper clientSetupHelperObj = new ClientSetupHelper();
            String clientName = clientSetupHelperObj.getClientNameFromId(clientId);

            String provisioningEditResponse = ProvisioningEdit.getProvisioningEditResponse(clientId, clientName);

            Document html = Jsoup.parse(provisioningEditResponse);
            Elements allDivs = html.getElementById("permission").child(1).child(0).children();

            for (String entityName : entities) {
                boolean permissionFound = false;
                String expectedEntityName = entityName.equalsIgnoreCase("contracts") ? "Contract" : "Contract Draft Request";

                for (int i = 0; i < allDivs.size(); i = i + 2) {
                    Element div = allDivs.get(i);

                    if (div.child(0).child(0).childNode(0).toString().replace(":", "").trim().equalsIgnoreCase(expectedEntityName)) {
                        div = allDivs.get(i + 1);
                        Elements allPermissionsOfCDR = div.child(0).child(0).child(0).child(0).children();

                        for (Element permissionDiv : allPermissionsOfCDR) {
                            String permissionName = permissionDiv.childNode(3).toString().trim();

                            if (permissionName.equalsIgnoreCase("Bulk Download Documents")) {
                                permissionFound = true;
                                break;
                            }
                        }

                        break;
                    }
                }

                csAssert.assertTrue(permissionFound, "Bulk Download Documents Permission not found in Application Provisioning on Sirion Admin for Entity " +
                        entityName);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C140996. " + e.getMessage());
        } finally {
            checkObj.hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));
        }

        csAssert.assertAll();
    }

    /*
    TC-C140997: Verify Bulk Download Documents Permission on Client Admin
     */
    @Test(enabled = true)
    public void testC140997() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C140997: Verify Bulk Download Documents Permission on Client Admin.");
            int urgId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "default", "urgId"));

            adminHelperObj.loginWithClientAdminUser();
            String urgUpdateResponse = MasterUserRoleGroupsUpdate.getUpdateResponse(urgId);

            Document html = Jsoup.parse(urgUpdateResponse);
            Elements allElements = html.getElementById("permission").children();

            for (String entityName : entities) {
                boolean permissionFound = false;
                String expectedEntityName = entityName.equalsIgnoreCase("contracts") ? "Contract" : "Contract Draft Request";

                for (int i = 0; i < allElements.size(); i = i + 2) {
                    Element div = allElements.get(i);
                    String divName = div.child(0).child(0).childNode(0).toString().replace(":", "").trim();

                    if (divName.equalsIgnoreCase(expectedEntityName)) {
                        div = allElements.get(i + 1);

                        Elements allPermissionNames = div.child(0).child(0).child(0).child(0).children();
                        for (Element permissionName : allPermissionNames) {
                            if (permissionName.child(3).attr("value").equalsIgnoreCase("Bulk Download Documents")) {
                                permissionFound = true;
                                break;
                            }
                        }

                        break;
                    }
                }

                csAssert.assertTrue(permissionFound, "Bulk Download Documents Permission not found under CDR on URG Id " + urgId + " for Entity " + entityName);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C140997: " + e.getMessage());
        } finally {
            checkObj.hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));
        }

        csAssert.assertAll();
    }


    @Test(enabled = true)
    public void testCompleteFlow() {
        CustomAssert csAssert = new CustomAssert();

        for (String entityName : entities) {
            try {
                logger.info("Validating Complete Flow of Bulk Download Documents for Entity {}", entityName);
                int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);

                validateC140998(entityName, entityTypeId, csAssert);
            } catch (Exception e) {
                csAssert.assertFalse(true, "Exception while Validating Bulk Download Documents Complete Flow for Entity " + entityName +
                        ". " + e.getMessage());
            }
        }

        csAssert.assertAll();
    }

    /*
    TC-C140998: Verify User able to Select documents and download them
     */
    private void validateC140998(String entityName, int entityTypeId, CustomAssert csAssert) {
        try {
            int recordId = -1;

            if (entityTypeId == 160) {
                recordId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "default", "cdrId"));
            } else if (entityTypeId == 61){
                recordId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "default", "contractId"));
            }

            String saveZipRequestResponse = SaveZipRequest.getSaveZipRequestResponseType3(entityTypeId, recordId, "{}");
            if (saveZipRequestResponse != null && ParseJsonResponse.validJsonResponse(saveZipRequestResponse)) {
                String message = new JSONObject(saveZipRequestResponse).getString("Message");

                if (message.contains("Your request has been successfully submitted. You will be notified via email")) {
                    String statusId = getStatusIdFromDB(entityTypeId, recordId);

                    if (statusId == null) {
                        throw new Exception("Couldn't get Status from DB for Entity " + entityName + " and Record Id " + recordId);
                    }

                    if (statusId.equalsIgnoreCase("1")) {
                        validateC141157(entityName, entityTypeId, recordId, csAssert);
                    }

                    validateC141009(entityName, entityTypeId, recordId, csAssert);

                    long timeSpent = 0L;
                    boolean jobPassed = true;

                    while (!statusId.equalsIgnoreCase("3")) {
                        statusId = getStatusIdFromDB(entityTypeId, recordId);

                        if (statusId == null) {
                            throw new Exception("Couldn't get Status from DB for Entity " + entityName + " and Record Id " + recordId);
                        }

                        if (statusId.equalsIgnoreCase("4")) {
                            jobPassed = false;
                            csAssert.assertFalse(true, "Bulk Download Documents Job failed for Entity " + entityName + " and Record Id " + recordId);
                            break;
                        } else {
                            Thread.sleep(pollingTime);
                            timeSpent += pollingTime;
                        }

                        if (timeSpent > timeOutWaitTime) {
                            jobPassed = false;
                            csAssert.assertFalse(true, "Bulk Download Documents Job not completed within specified time for Entity " + entityName +
                                    " and Record Id " + recordId);
                            break;
                        }
                    }

                    if (jobPassed) {
                        validateC141009(entityName, entityTypeId, recordId, csAssert);
                    }
                } else {
                    csAssert.assertFalse(true, "Bulk Download failed for Entity " + entityName + " and Record Id " + recordId);
                }
            } else {
                csAssert.assertFalse(true, "SaveZipRequest API call failed for Entity " + entityName + " and Record Id " +
                        recordId + ". Hence couldn't download Bulk Documents.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Downloading Bulk Documents for Entity " + entityName + ". " + e.getMessage());
        }
    }

    /*
    TC-C141009: Validate entry under Document Download Tab in User Profile Schedule Section
     */
    private void validateC141009(String entityName, int entityTypeId, int recordId, CustomAssert csAssert) {
        try {
            logger.info("Validating Entry under Document Download Tab in User Profile Schedule Section.");
            String payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";

            String scheduleDataResponse = executor.post("/listRenderer/list/443/scheduledata/0", ApiHeaders.getDefaultLegacyHeaders(),
                    payload).getResponse().getResponseBody();

            String showResponse = ShowHelper.getShowResponseVersion2(entityTypeId, recordId);
            String expectedFileName = ShowHelper.getActualValue(showResponse, ShowHelper.getShowFieldHierarchy("name", entityTypeId));

            JSONObject jsonObj = new JSONObject(scheduleDataResponse).getJSONArray("data").getJSONObject(0);
            String fileNameColumnId = TabListDataHelper.getColumnIdFromColumnName(scheduleDataResponse, "filename");

            String actualFileName = jsonObj.getJSONObject(fileNameColumnId).getString("value");

            if (actualFileName.equalsIgnoreCase(expectedFileName)) {
                String prefixMsg = "Document Download Tab under User Profile Schedule Section Validation failed for Entity " + entityName + " and Record Id " + recordId + ". ";

                //Validate Document Type
                String documentTypeColumnId = TabListDataHelper.getColumnIdFromColumnName(scheduleDataResponse, "documenttype");
                csAssert.assertTrue(jsonObj.getJSONObject(documentTypeColumnId).getString("value").equalsIgnoreCase("Contract Documents Download"),
                        prefixMsg + "Expected Document Type [Contract Documents Download]" + "and Actual Document Type [" +
                                jsonObj.getJSONObject(documentTypeColumnId).getString("value") + "]");

                //Validate Status
                String statusId = getStatusIdFromDB(entityTypeId, recordId);

                if (statusId != null) {
                    String expectedStatusValue = getExpectedStatus(statusId);
                    String expectedDownloadLink = getExpectedDownloadLink(entityTypeId, recordId);

                    String statusColumnId = TabListDataHelper.getColumnIdFromColumnName(scheduleDataResponse, "status");
                    csAssert.assertTrue(jsonObj.getJSONObject(statusColumnId).getString("value").equalsIgnoreCase(expectedStatusValue),
                            prefixMsg + "Expected Status [" + expectedStatusValue + "]" + "and Actual Status [" +
                                    jsonObj.getJSONObject(statusColumnId).getString("value") + "]");

                    //Validate Download Link
                    String downloadLinkColumnId = TabListDataHelper.getColumnIdFromColumnName(scheduleDataResponse, "downloadlink");
                    if (expectedDownloadLink == null) {
                        csAssert.assertTrue(jsonObj.getJSONObject(downloadLinkColumnId).isNull("value"),
                                prefixMsg + "Download Link is Not Null when the Job is not completed yet.");
                    } else {
                        String expectedDownloadPath = getExpectedDownloadPath(entityTypeId, recordId);
                        String downloadId = expectedDownloadPath.split("/")[1];

                        String fullDownloadLink = downloadId + ":;" + expectedDownloadLink;

                        csAssert.assertTrue(jsonObj.getJSONObject(downloadLinkColumnId).getString("value").equalsIgnoreCase(fullDownloadLink),
                                prefixMsg + "Expected Download Link [" + fullDownloadLink + "] and Actual Download Link [" +
                                        jsonObj.getJSONObject(downloadLinkColumnId).getString("value") + "]");

                        //Validate Entry in Table
                        validateEntryInSystemEmail(entityName, recordId, downloadId, expectedDownloadLink, csAssert);

                        //Validate Zip Download
                        validateDownloadZipContent(entityName, entityTypeId, recordId, expectedDownloadLink, downloadId, csAssert);
                    }

                    //Validate details for Other User
                    validateC141014(entityName, expectedFileName, csAssert);
                } else {
                    csAssert.assertTrue(false, "Couldn't get Status from DB for Entity " + entityName + " and Record Id " + recordId);
                }
            } else {
                csAssert.assertFalse(true,
                        "Couldn't find entry for Bulk Document Download under Document Download Tab in User Profile Schedule Section for Entity " +
                                entityName + " and Record Id " + recordId);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Entry Under Document Download Tab in User Profile Schedule Section for Entity " +
                    entityName + " and Record Id " + recordId + ". " + e.getMessage());
        }
    }

    /*
    TC-C141157: Verify Submitting same request again would fail if previous request is still not processed.
     */
    private void validateC141157(String entityName, int entityTypeId, int recordId, CustomAssert csAssert) {
        try {
            String saveZipRequestResponse = SaveZipRequest.getSaveZipRequestResponseType3(entityTypeId, recordId, "{}");
            if (saveZipRequestResponse != null && ParseJsonResponse.validJsonResponse(saveZipRequestResponse)) {
                String message = new JSONObject(saveZipRequestResponse).getString("Message");

                if (!message.contains("You have already submitted similar request, please wait for completion")) {
                    csAssert.assertFalse(true,
                            "TC-C141157: [Verify Submitting same request again would fail if previous request is still not processed] failed for Entity " +
                                    entityName + " and Record Id " + recordId);
                }
            } else {
                csAssert.assertFalse(true, "SaveZipRequest API call failed for Entity " + entityName + " and Record Id " +
                        recordId + ". Hence couldn't download Bulk Documents.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false,
                    "Exception while Validating Scenario: [Verify Submitting Same Request Again would fail if previous request is still not processed.] " +
                            e.getMessage());
        }
    }

    private String getStatusIdFromDB(int entityTypeId, int recordId) {
        try {
            PostgreSQLJDBC postgresObj = new PostgreSQLJDBC();
            return postgresObj.doSelect("select status from document_download_request where job_id = 138 and entity_type_id = " + entityTypeId +
                    " and entity_id = " + recordId + " and client_id = " + clientId + " order by id desc limit 1").get(0).get(0);
        } catch (Exception e) {
            return null;
        }
    }

    private String getExpectedStatus(String statusId) {
        switch (statusId) {
            case "1":
                return "Submitted";

            case "2":
                return "In Progress";

            case "3":
                return "Completed";

            case "4":
                return "Failed";

            default:
                return "";
        }
    }

    private String getExpectedDownloadLink(int entityTypeId, int recordId) {
        try {
            PostgreSQLJDBC postgresObj = new PostgreSQLJDBC();
            return postgresObj.doSelect("select file_name from document_download_request where job_id = 138 and entity_type_id = " + entityTypeId +
                    " and entity_id = " + recordId + " and client_id = " + clientId + " order by id desc limit 1").get(0).get(0);
        } catch (Exception e) {
            return null;
        }
    }

    private String getExpectedDownloadPath(int entityTypeId, int recordId) {
        try {
            PostgreSQLJDBC postgresObj = new PostgreSQLJDBC();
            return postgresObj.doSelect("select file_path from document_download_request where job_id = 138 and entity_type_id = " + entityTypeId +
                    " and entity_id = " + recordId + " and client_id = " + clientId + " order by id desc limit 1").get(0).get(0);
        } catch (Exception e) {
            return null;
        }
    }

    /*
    TC-C141011: Verify Email Part
     */
    private void validateEntryInSystemEmail(String entityName, int recordId, String downloadId, String fileName, CustomAssert csAssert) {
        try {
            logger.info("Validating Email Part for Entity {} and Record Id {}", entityName, recordId);

            PostgreSQLJDBC postgresObj = new PostgreSQLJDBC();
            List<String> systemEmailData = postgresObj.doSelect("select subject, body, sent_succesfully from system_emails order by id desc limit 1").get(0);
            String prefixMsg = "System Email Table Data Validation failed for Entity " + entityName + " and Record Id " + recordId + ". ";

            //Validate Email Subject
            /*csAssert.assertTrue(systemEmailData.get(0).equalsIgnoreCase("Document Download Report Generated"), prefixMsg +
                    "Expected Email Subject [Document Download Report Generated] and Actual Subject: " + systemEmailData.get(0));*/

            //Validate Email Sent Flag
            csAssert.assertTrue(systemEmailData.get(2).equalsIgnoreCase("t"), prefixMsg + "Email not sent successfully for Entity " +
                    entityName + " and Record Id " + recordId);

            //Validate Email Body
            String emailBody = systemEmailData.get(1);
            Node node = Jsoup.parse(emailBody).getElementsByClass("main-content").get(0).childNode(1).childNode(1).childNode(1).childNode(0);

            String actualEmailMessage = node.childNode(0).toString();
            String actualDownloadLink = node.childNode(3).attr("href");

            csAssert.assertTrue(actualEmailMessage.contains(fileName), prefixMsg + "Email Message doesn't contain File Name [" + fileName + "]");
            csAssert.assertTrue(actualDownloadLink.contains("zipDownload?id=" + downloadId), prefixMsg + "Document Downloads Link Id " + downloadId +
                    " not found in Email Body.");

            //Validate Expiry Days
            String expiryMessage = node.childNode(4).toString();
            csAssert.assertTrue(expiryMessage.contains("15 days"), prefixMsg + "Email Body doesn't contain 15 Days as Expiry.");
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Email Part for Entity " + entityName + " and Record Id " +
                    recordId + ". " + e.getMessage());
        }
    }

    /*
    TC-C141015: Verify Download Zip Name
    TC-C141017: Verify Download Zip Content
     */
    private void validateDownloadZipContent(String entityName, int entityTypeId, int recordId, String downloadLinkName, String downloadId, CustomAssert csAssert) {
        try {
            logger.info("Validating Download Zip Content for Entity {} and Record Id {}", entityName, recordId);
            String prefixMsg = "Zip Content Validation failed for Entity " + entityName + " and Record Id " + recordId;

            //Validate Zip Name in Response.
            HttpResponse response = ZipDownload.hitZipDownload(downloadId);
            String downloadZipName = response.getHeaders("Content-disposition")[0].toString().split("filename=")[1];

            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = new Date();
            String currentDate = dateFormat.format(date);

            String expectedZipName = "\"" + downloadLinkName + "_" + currentDate + ".zip" + "\"";
            csAssert.assertEquals(downloadZipName, expectedZipName, prefixMsg + "Expected Zip File Name: " + expectedZipName +
                    " and Actual File Name: " + downloadZipName);

            //Validate Zip Content
            String outputFilePath = "src/test";
            String outputFileName = "CA1331.zip";

            boolean saveZipFile = ZipDownload.saveZipFile(response, outputFilePath, outputFileName);

            if (saveZipFile) {
                FileUtils.unzip(outputFilePath + "/" + outputFileName, "src/test/CA1331");
                int noOfFilesInZip = new File("src/test/CA1331/Documents").listFiles().length;

                int tabId = entityTypeId == 61 ? 366 : 367;
                String listDataResponse = TabListDataHelper.getTabListDataResponse(entityTypeId, recordId, tabId);
                int expectedNoOfFiles = ListDataHelper.getFilteredListDataCount(listDataResponse);

                csAssert.assertEquals(noOfFilesInZip, expectedNoOfFiles, prefixMsg + "Expected No of Files in Zip: " + expectedNoOfFiles +
                        " and Actual No of Files: " + noOfFilesInZip);

                //Delete Downloaded Zip and Files
                FileUtils.deleteFile(outputFilePath + "/" + outputFileName);

                FileUtils.deleteDirectory(new File("src/test/CA1331"));
            } else {
                csAssert.assertFalse(true, "Couldn't Save Zip File for Entity " + entityName + " and Record Id " + recordId);
            }
        } catch (Exception e) {
            csAssert.assertTrue(true, "Exception while Validating Download Zip for Entity " + entityName + " and Record Id " + recordId +
                    ". " + e.getMessage());
        }
    }

    /*
    TC-C141014: Verify Other User doesn't have permission to show Download Link.
     */
    private void validateC141014(String entityName, String fileName, CustomAssert csAssert) {
        try {
            Map<String, String> map = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, "other user details");
            String user = map.get("username");
            String password = map.get("password");

            checkObj.hitCheck(user, password);

            String payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
            String scheduleDataResponse = executor.post("/listRenderer/list/443/scheduledata/0", ApiHeaders.getDefaultLegacyHeaders(),
                    payload).getResponse().getResponseBody();

            String fileNameColumnId = TabListDataHelper.getColumnIdFromColumnName(scheduleDataResponse, "filename");
            JSONObject jsonObj = new JSONObject(scheduleDataResponse);

            if (jsonObj.getJSONArray("data").length() > 0) {
                String value = jsonObj.getJSONArray("data").getJSONObject(0).getJSONObject(fileNameColumnId).getString("value");

                csAssert.assertFalse(value.equalsIgnoreCase(fileName), "Other User able to visit Download Document Request for Entity " + entityName);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating [Other User shouldn't have permission to access Download Link] for Entity " +
                    entityName + ". " + e.getMessage());
        } finally {
            checkObj.hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));
        }
    }
}