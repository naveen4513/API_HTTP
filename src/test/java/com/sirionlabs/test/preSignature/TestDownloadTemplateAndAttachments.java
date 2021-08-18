package com.sirionlabs.test.preSignature;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.SaveZipRequest;
import com.sirionlabs.api.commonAPI.ZipDownload;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.entityCreation.ContractTemplate;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class TestDownloadTemplateAndAttachments extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestDownloadTemplateAndAttachments.class);

    private String configFilePath = "src/test/resources/TestConfig/PreSignature/DownloadTemplateAndAttachments";
    private String configFileName = "TestDownloadTemplateAndAttachments.cfg";

    private long timeOutWaitTime = 300000;
    private long pollingTime = 15000;

    private AdminHelper adminHelperObj = new AdminHelper();
    private Check checkObj = new Check();
    private int clientId;

    @BeforeClass
    public void beforeClass() {
        clientId = adminHelperObj.getClientId();

        //Enable Permission if it is disabled
        adminHelperObj.addPermissionForUser(ConfigureEnvironment.getEndUserLoginId(), clientId, "912");
    }

    @Test
    public void testCompleteFlow() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating Complete Flow of Download Template and Attachments");

            String ctCreateResponse = ContractTemplate.createContractTemplate(configFilePath, configFileName, configFilePath, "ExtraFields.cfg",
                    "ct creation", false);
            String status = ParseJsonResponse.getStatusFromResponse(ctCreateResponse);

            if (status.equalsIgnoreCase("success")) {
                int ctId = CreateEntity.getNewEntityId(ctCreateResponse);
                validateDownloadZip(ctId, csAssert);
            } else {
                csAssert.assertFalse(true, "Couldn't Create CT with Attachments due to " + status);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Complete Flow of Download Template and Attachments. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    /*
    Verify User able to Select documents and download them
     */
    private void validateDownloadZip(int recordId, CustomAssert csAssert) {
        try {
            String saveZipRequestResponse = SaveZipRequest.getSaveZipRequestResponseType2(140, recordId, "{}");
            if (saveZipRequestResponse != null && ParseJsonResponse.validJsonResponse(saveZipRequestResponse)) {
                String message = new JSONObject(saveZipRequestResponse).getString("Message");

                if (message.contains("Your request has been successfully submitted. You will be notified via email")) {
                    String statusId = getStatusIdFromDB(recordId);

                    if (statusId == null) {
                        throw new Exception("Couldn't get Status from DB for Entity CT and Record Id " + recordId);
                    }

                    if (statusId.equalsIgnoreCase("1")) {
                        validateSubmittingRequestAgain(recordId, csAssert);
                    }

                    validateDocumentDownloadTab(recordId, csAssert);

                    validateAuditLog(recordId, csAssert);

                    long timeSpent = 0L;
                    boolean jobPassed = true;

                    while (!statusId.equalsIgnoreCase("3")) {
                        statusId = getStatusIdFromDB(recordId);

                        if (statusId == null) {
                            throw new Exception("Couldn't get Status from DB Record Id " + recordId);
                        }

                        if (statusId.equalsIgnoreCase("4")) {
                            jobPassed = false;
                            csAssert.assertFalse(true, "Download Template and Attachments Job failed for Entity Record Id " + recordId);
                            break;
                        } else {
                            Thread.sleep(pollingTime);
                            timeSpent += pollingTime;
                        }

                        if (timeSpent > timeOutWaitTime) {
                            jobPassed = false;
                            break;
                        }
                    }

                    if (jobPassed) {
                        validateDocumentDownloadTab(recordId, csAssert);
                        EntityOperationsHelper.deleteEntityRecord("contract templates", recordId);
                    } else {
                        if (statusId.equalsIgnoreCase("1") || statusId.equalsIgnoreCase("2")) {
                            throw new SkipException("Scheduled Job Not Picked within Max TimeOut. Hence Couldn't validate further.");
                        }
                    }
                } else {
                    csAssert.assertFalse(true, "Download Template and Attachment failed for Record Id " + recordId);
                }
            } else {
                csAssert.assertFalse(true, "SaveZipRequest API call failed for Record Id " + recordId +
                        ". Hence couldn't Download Template and Attachment.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Downloading Template and Attachment. " + e.getMessage());
        }
    }

    /*
    Validate entry under Document Download Tab in User Profile Schedule Section
     */
    private void validateDocumentDownloadTab(int recordId, CustomAssert csAssert) {
        try {
            logger.info("Validating Entry under Document Download Tab in User Profile Schedule Section.");
            String payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";

            String scheduleDataResponse = executor.post("/listRenderer/list/443/scheduledata/0", ApiHeaders.getDefaultLegacyHeaders(),
                    payload).getResponse().getResponseBody();

            String showResponse = ShowHelper.getShowResponseVersion2(140, recordId);
            String expectedFileName = ShowHelper.getActualValue(showResponse, ShowHelper.getShowFieldHierarchy("name", 140));

            JSONObject jsonObj = new JSONObject(scheduleDataResponse).getJSONArray("data").getJSONObject(0);
            String fileNameColumnId = TabListDataHelper.getColumnIdFromColumnName(scheduleDataResponse, "filename");

            String actualFileName = jsonObj.getJSONObject(fileNameColumnId).getString("value");

            if (actualFileName.equalsIgnoreCase(expectedFileName)) {
                String prefixMsg = "Document Download Tab under User Profile Schedule Section Validation failed for Entity CT and Record Id " + recordId + ". ";

                //Validate Document Type
                String documentTypeColumnId = TabListDataHelper.getColumnIdFromColumnName(scheduleDataResponse, "documenttype");
                csAssert.assertTrue(jsonObj.getJSONObject(documentTypeColumnId).getString("value").equalsIgnoreCase("Contract Template Download"),
                        prefixMsg + "Expected Document Type [Contract Template Download]" + "and Actual Document Type [" +
                                jsonObj.getJSONObject(documentTypeColumnId).getString("value") + "]");

                //Validate Status
                String statusId = getStatusIdFromDB(recordId);

                if (statusId != null) {
                    String expectedStatusValue = getExpectedStatus(statusId);
                    String expectedDownloadLink = getExpectedDownloadLink(recordId);

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
                        String expectedDownloadPath = getExpectedDownloadPath(recordId);
                        String downloadId = expectedDownloadPath.split("/")[1];

                        String fullDownloadLink = downloadId + ":;" + expectedDownloadLink;

                        csAssert.assertTrue(jsonObj.getJSONObject(downloadLinkColumnId).getString("value").equalsIgnoreCase(fullDownloadLink),
                                prefixMsg + "Expected Download Link [" + fullDownloadLink + "] and Actual Download Link [" +
                                        jsonObj.getJSONObject(downloadLinkColumnId).getString("value") + "]");

                        //Validate Entry in Table
                        validateEntryInSystemEmail(recordId, downloadId, expectedDownloadLink, csAssert);

                        //Validate Zip Download
                        validateDownloadZipContent(recordId, expectedDownloadLink, downloadId, csAssert);
                    }

                    //Validate details for Other User
                    validateC63559(expectedFileName, csAssert);
                } else {
                    csAssert.assertTrue(false, "Couldn't get Status from DB for Record Id " + recordId);
                }
            } else {
                csAssert.assertFalse(true, "Couldn't find entry for Bulk Document Download under Document Download Tab in User " +
                        "Profile Schedule Section for Entity CT and Record Id " + recordId);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Entry Under Document Download Tab in User Profile Schedule Section for Entity CT " +
                    "and Record Id " + recordId + ". " + e.getMessage());
        }
    }

    /*
    Verify Submitting same request again would fail if previous request is still not processed.
     */
    private void validateSubmittingRequestAgain(int recordId, CustomAssert csAssert) {
        try {
            String saveZipRequestResponse = SaveZipRequest.getSaveZipRequestResponseType2(140, recordId, "{}");
            if (saveZipRequestResponse != null && ParseJsonResponse.validJsonResponse(saveZipRequestResponse)) {
                String message = new JSONObject(saveZipRequestResponse).getString("Message");

                if (!message.contains("You have already submitted similar request, please wait for completion")) {
                    csAssert.assertFalse(true, "[Verify Submitting same request again would fail if previous request is " +
                            "still not processed] failed for Record Id " + recordId);
                }
            } else {
                csAssert.assertFalse(true, "SaveZipRequest API call failed for Record Id " + recordId + ". Hence couldn't validate further.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false,
                    "Exception while Validating Scenario: [Verify Submitting Same Request Again would fail if previous request is still not processed.] " +
                            e.getMessage());
        }
    }

    private String getStatusIdFromDB(int recordId) {
        try {
            PostgreSQLJDBC postgresObj = new PostgreSQLJDBC();
            return postgresObj.doSelect("select status from document_download_request where job_id = 138 and entity_type_id = 140 and entity_id = " +
                    recordId + " and client_id = " + clientId + " order by id desc limit 1").get(0).get(0);
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

    private String getExpectedDownloadLink(int recordId) {
        try {
            PostgreSQLJDBC postgresObj = new PostgreSQLJDBC();
            return postgresObj.doSelect("select file_name from document_download_request where job_id = 138 and entity_type_id = 140 and entity_id = " +
                    recordId + " and client_id = " + clientId + " order by id desc limit 1").get(0).get(0);
        } catch (Exception e) {
            return null;
        }
    }

    private String getExpectedDownloadPath(int recordId) {
        try {
            PostgreSQLJDBC postgresObj = new PostgreSQLJDBC();
            return postgresObj.doSelect("select file_path from document_download_request where job_id = 138 and entity_type_id = 140 and entity_id = " +
                    recordId + " and client_id = " + clientId + " order by id desc limit 1").get(0).get(0);
        } catch (Exception e) {
            return null;
        }
    }

    /*
    Verify Email Part
     */
    private void validateEntryInSystemEmail(int recordId, String downloadId, String fileName, CustomAssert csAssert) {
        try {
            logger.info("Validating Email Part for Record Id {}", recordId);

            PostgreSQLJDBC postgresObj = new PostgreSQLJDBC();
            List<String> systemEmailData = postgresObj.doSelect("select subject, body, sent_succesfully from system_emails order by id desc limit 1").get(0);
            String prefixMsg = "System Email Table Data Validation failed for Record Id " + recordId + ". ";

            //Validate Email Subject
            csAssert.assertTrue(systemEmailData.get(0).equalsIgnoreCase("Document Download Report Generated"), prefixMsg +
                    "Expected Email Subject [Document Download Report Generated] and Actual Subject: " + systemEmailData.get(0));

            //Validate Email Sent Flag
            csAssert.assertTrue(systemEmailData.get(2).equalsIgnoreCase("t"), prefixMsg + "Email not sent successfully for Record Id " + recordId);

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
            csAssert.assertFalse(true, "Exception while Validating Email Part for Record Id " + recordId + ". " + e.getMessage());
        }
    }

    /*
    Verify Download Zip Name
    Verify Download Zip Content
     */
    private void validateDownloadZipContent(int recordId, String downloadLinkName, String downloadId, CustomAssert csAssert) {
        try {
            logger.info("Validating Download Zip Content for Record Id {}", recordId);
            String prefixMsg = "Zip Content Validation failed for Record Id " + recordId;

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
            String outputFileName = "TemplateAttachments.zip";

            boolean saveZipFile = ZipDownload.saveZipFile(response, outputFilePath, outputFileName);

            if (saveZipFile) {
                FileUtils.unzip(outputFilePath + "/" + outputFileName, "src/test/TemplateAttachments");

                //C63553: Validate Template doc file
                boolean templateDocFilePresent = FileUtils.fileExists("src/test/TemplateAttachments", downloadLinkName + ".docx");
                csAssert.assertTrue(templateDocFilePresent, "Template Doc File not found in downloaded zip.");

                int noOfFilesInZip = new File("src/test/TemplateAttachments/ATTACHMENTS").listFiles().length;

                int tabId = 360;
                String listDataResponse = TabListDataHelper.getTabListDataResponse(140, recordId, tabId);
                int expectedNoOfFiles = ListDataHelper.getFilteredListDataCount(listDataResponse);

                csAssert.assertEquals(noOfFilesInZip, expectedNoOfFiles, prefixMsg + "Expected No of Files in Zip: " + expectedNoOfFiles +
                        " and Actual No of Files: " + noOfFilesInZip);

                //Delete Downloaded Zip and Files
                FileUtils.deleteFile(outputFilePath + "/" + outputFileName);

                FileUtils.deleteDirectory(new File("src/test/TemplateAttachments"));
            } else {
                csAssert.assertFalse(true, "Couldn't Save Zip File for Record Id " + recordId);
            }
        } catch (Exception e) {
            csAssert.assertTrue(true, "Exception while Validating Download Zip for Record Id " + recordId + ". " + e.getMessage());
        }
    }

    /*
    TC-C63559: Verify Other User doesn't have permission to show Download Link.
     */
    private void validateC63559(String fileName, CustomAssert csAssert) {
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

                csAssert.assertFalse(value.equalsIgnoreCase(fileName), "Other User able to visit Download Document Request");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating [Other User shouldn't have permission to access Download Link]. " + e.getMessage());
        } finally {
            checkObj.hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));
        }
    }

    /*
    TC-C63562: Verify Audit Log Entry.
     */
    private void validateAuditLog(int recordId, CustomAssert csAssert) {
        try {
            logger.info("Validating Audit Log Entry");
            String payload = "{\"filterMap\":{\"entityTypeId\":140,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
            String auditLogResponse = TabListDataHelper.getTabListDataResponse(140, recordId, 61, payload);

            JSONObject jsonObj = new JSONObject(auditLogResponse).getJSONArray("data").getJSONObject(0);
            String actionNameColumnId = TabListDataHelper.getColumnIdFromColumnName(auditLogResponse, "action_name");

            String actionNameValue = jsonObj.getJSONObject(actionNameColumnId).getString("value");
            csAssert.assertTrue(actionNameValue.equalsIgnoreCase("Bulk Document Download Request"),
                    "Audit Log Validation failed for CT Entity and Record Id " + recordId);
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Audit Log Entry. " + e.getMessage());
        }
    }
}