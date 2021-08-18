package com.sirionlabs.test.preSignature;

import com.sirionlabs.api.auditLogs.AuditLogDrafts;
import com.sirionlabs.api.clientAdmin.masterUserRoleGroups.MasterUserRoleGroupsList;
import com.sirionlabs.api.clientAdmin.masterUserRoleGroups.MasterUserRoleGroupsUpdate;
import com.sirionlabs.api.clientSetup.provisioning.ProvisioningEdit;
import com.sirionlabs.api.commonAPI.Actions;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.file.FileUploadDraft;
import com.sirionlabs.api.presignature.SubmitDraft;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.clientSetup.ClientSetupHelper;
import com.sirionlabs.helper.entityCreation.ContractDraftRequest;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TestClassificationOptions extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestClassificationOptions.class);

    private String configFilePath = "src/test/resources/TestConfig/PreSignature/ClassificationOptions";
    private String configFileName = "TestClassificationOptions.cfg";

    private String userRoleGroupId;
    private String cdrRoleGroupId;
    private int clientId;
    private String otherUserName;
    private String otherUserPassword;

    private SubmitDraft submitDraftObj = new SubmitDraft();
    private Check checkObj = new Check();
    private AdminHelper adminHelperObj = new AdminHelper();

    @BeforeClass
    public void beforeClass() {
        Map<String, String> defaultProperties = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileName, "default");

        userRoleGroupId = defaultProperties.get("urgId");
        cdrRoleGroupId = defaultProperties.get("cdrRoleGroupId");
        clientId = adminHelperObj.getClientId();

        otherUserName = defaultProperties.get("otherUserName");
        otherUserPassword = defaultProperties.get("otherUserPassword");
    }

    private int createCDR() {
        String sectionName = "cdr creation";
        String createResponse = ContractDraftRequest.createCDR(configFilePath, configFileName, configFilePath, "ExtraFields.cfg",
                sectionName, false);
        String status = ParseJsonResponse.getStatusFromResponse(createResponse);

        if (status.equalsIgnoreCase("success")) {
            return CreateEntity.getNewEntityId(createResponse);
        } else {
            return -1;
        }
    }

    @AfterClass
    public void afterClass() {
        updateDB(true, true, true);
        updateDB(true, true, true, otherUserName);
    }

    /*
    TC-C4482: Verify options when only Business is allowed from Client Admin.
     */
    @Test
    public void testC4482() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C4482: Verify options when only Business is allowed from Client Admin.");
            updateDB(false, false, true);

            int cdrId = ListDataHelper.getLatestRecordId("contract draft request");

            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
            String draftResponse = uploadDocument(cdrId, "Sample.txt", randomKeyForFileUpload);

            JSONObject jsonObj = new JSONObject(draftResponse).getJSONObject("classification");
            csAssert.assertFalse(jsonObj.has("legal"), "Legal Option still available in Classifications List.");
            csAssert.assertFalse(jsonObj.has("financial"), "Financial Option still available in Classifications List.");
            csAssert.assertTrue(jsonObj.has("businessCase"), "Business Case Option not available in Classifications List.");

        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C4482. " + e.getMessage());
        } finally {
            updateDB(true, true, true);
        }

        csAssert.assertAll();
    }

    /*
    TC-C4488: Verify Additional Details under Communication Tab
     */
    @Test
    public void testC4488() {
        CustomAssert csAssert = new CustomAssert();
        int cdrId = createCDR();

        try {
            logger.info("Starting Test TC-C4488: Verify Additional Details under Communication Tab.");

            if (cdrId == -1) {
                throw new Exception("Couldn't Create CDR. Hence Cannot validate further.");
            }

            updateDB(true, false, false);

            String uploadFileName = "Sample_C4488.txt";
            FileUtils.copyFile(configFilePath, "Sample.txt", configFilePath, uploadFileName);

            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
            String draftResponse = uploadDocument(cdrId, uploadFileName, randomKeyForFileUpload);

            if (csAssert.getAllAssertionMessages().equalsIgnoreCase("")) {
                JSONObject jsonObj = new JSONObject(draftResponse);

                String permissions = "\"legal\":true";
                String payload = getPayloadForSubmitDraft(cdrId, jsonObj.get("documentFileId").toString(), randomKeyForFileUpload, permissions, false);
                submitDraftObj.hitSubmitDraft(payload);
                String submitDraftResponse = submitDraftObj.getSubmitDraftJsonStr();

                if (ParseJsonResponse.getStatusFromResponse(submitDraftResponse).equalsIgnoreCase("success")) {
                    payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
                    String communicationTabResponse = TabListDataHelper.getTabListDataResponse(160, cdrId, 65, payload);
                    String columnId = TabListDataHelper.getColumnIdFromColumnName(communicationTabResponse, "private_communication");
                    String value = new JSONObject(communicationTabResponse).getJSONArray("data").getJSONObject(0).getJSONObject(columnId).getString("value");
                    String logId = value.split(":;")[2];

                    checkObj.hitCheck(otherUserName, otherUserPassword);

                    String auditLogDraftsResponse = AuditLogDrafts.getDraftsResponse(160, logId);
                    jsonObj = new JSONObject(auditLogDraftsResponse).getJSONArray("data").getJSONObject(0);
                    String documentName = jsonObj.getString("documentName");
                    String prefixMsg = "AuditLogs Drafts Response Validation failed. ";
                    csAssert.assertTrue(documentName.equalsIgnoreCase(uploadFileName), prefixMsg + "Expected Document Name: " + uploadFileName +
                            " and Actual Document Name: " + documentName);

                    String classificationValue = jsonObj.getString("classification");
                    csAssert.assertTrue(classificationValue.equalsIgnoreCase("false:;true:;false"), prefixMsg +
                            "Expected Classification Value: [false:;true:;false] and Actual Value: [" + classificationValue + "]");
                } else {
                    csAssert.assertFalse(true, "Submit Draft API failed while Uploading Document on CDR Contract Document Tab.");
                }
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C4488. " + e.getMessage());
        } finally {
            checkObj.hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));

            if (cdrId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId);
            }

            updateDB(true, true, true);
        }

        csAssert.assertAll();
    }

    /*
    TC-C4489: Verify if No Documents Accessible to user then no row should be displayed under Communication Tab
    TC-C4492: Verify Downloading of Document should be allowed only if user has permission
     */
    @Test
    public void testC4489() {
        CustomAssert csAssert = new CustomAssert();
        int cdrId = createCDR();

        try {
            logger.info("Starting Test TC-C4489: Verify if No Documents Accessible to user then no row should be displayed under Communication Tab.");

            if (cdrId == -1) {
                throw new SkipException("Couldn't Create CDR. Hence couldn't validate further");
            }

            String uploadFileName = "Sample_C4489.txt";
            FileUtils.copyFile(configFilePath, "Sample.txt", configFilePath, uploadFileName);

            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
            String draftResponse = uploadDocument(cdrId, uploadFileName, randomKeyForFileUpload);

            if (csAssert.getAllAssertionMessages().equalsIgnoreCase("")) {
                JSONObject jsonObj = new JSONObject(draftResponse);

                String payload = getPayloadForSubmitDraft(cdrId, jsonObj.get("documentFileId").toString(), randomKeyForFileUpload, null, true);
                submitDraftObj.hitSubmitDraft(payload);
                String submitDraftResponse = submitDraftObj.getSubmitDraftJsonStr();

                if (ParseJsonResponse.getStatusFromResponse(submitDraftResponse).equalsIgnoreCase("success")) {
                    checkObj.hitCheck(otherUserName, otherUserPassword);

                    payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
                    String communicationTabResponse = TabListDataHelper.getTabListDataResponse(160, cdrId, 65, payload);

                    int dataLength = new JSONObject(communicationTabResponse).getJSONArray("data").length();

                    csAssert.assertTrue(dataLength == 0, "Private Uploaded Doc visible to other user under Communication Tab.");
                } else {
                    csAssert.assertFalse(true, "Submit Draft API failed while Uploading Document on CDR Contract Document Tab.");
                }
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C4489. " + e.getMessage());
        } finally {
            checkObj.hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));

            if (cdrId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId);
            }
        }

        csAssert.assertAll();
    }

    /*
    TC-C4490: Verify if User doesn't have Legal permission then no row should be displayed under Communication Tab for Doc Uploaded with Legal Permission
     */
    @Test
    public void testC4490() {
        CustomAssert csAssert = new CustomAssert();
        int cdrId = createCDR();

        try {
            logger.info("Starting Test TC-C4490: Verify if User doesn't have Legal Permission then no row should be displayed under Communication Tab for " +
                    "Doc Uploaded with Legal Permission");

            if (cdrId == -1) {
                throw new SkipException("Couldn't Create CDR. Hence couldn't validate further.");
            }

            String uploadFileName = "Sample_C4490.txt";
            FileUtils.copyFile(configFilePath, "Sample.txt", configFilePath, uploadFileName);

            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
            String draftResponse = uploadDocument(cdrId, uploadFileName, randomKeyForFileUpload);

            if (csAssert.getAllAssertionMessages().equalsIgnoreCase("")) {
                JSONObject jsonObj = new JSONObject(draftResponse);

                String permissions = "\"legal\":true";
                String payload = getPayloadForSubmitDraft(cdrId, jsonObj.get("documentFileId").toString(), randomKeyForFileUpload, permissions, false);
                submitDraftObj.hitSubmitDraft(payload);
                String submitDraftResponse = submitDraftObj.getSubmitDraftJsonStr();

                if (ParseJsonResponse.getStatusFromResponse(submitDraftResponse).equalsIgnoreCase("success")) {
                    updateDB(false, false, false, otherUserName);
                    checkObj.hitCheck(otherUserName, otherUserPassword);

                    payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
                    String communicationTabResponse = TabListDataHelper.getTabListDataResponse(160, cdrId, 65, payload);

                    int dataLength = new JSONObject(communicationTabResponse).getJSONArray("data").length();

                    csAssert.assertTrue(dataLength == 0, "Private Uploaded Doc visible to other user under Communication Tab.");
                } else {
                    csAssert.assertFalse(true, "Submit Draft API failed while Uploading Document on CDR Contract Document Tab.");
                }
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C4490. " + e.getMessage());
        } finally {
            checkObj.hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));

            if (cdrId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId);
            }
        }

        csAssert.assertAll();
    }

    /*
    TC-C4491: Verify classifications should be shown to other user based on the permissions.
     */
    @Test
    public void testC4491() {
        CustomAssert csAssert = new CustomAssert();
        int cdrId = createCDR();

        try {
            logger.info("Starting Test TC-C4491: Verify Classifications should be shown to other user based on the permissions.");

            if (cdrId == -1) {
                throw new Exception("Couldn't Create CDR. Hence Cannot validate further.");
            }

            String uploadFileName = "Sample_C4491_1.txt";
            FileUtils.copyFile(configFilePath, "Sample.txt", configFilePath, uploadFileName);

            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
            String draftResponse = uploadDocument(cdrId, uploadFileName, randomKeyForFileUpload);

            if (csAssert.getAllAssertionMessages().equalsIgnoreCase("")) {
                JSONObject jsonObj = new JSONObject(draftResponse);

                String permissions = "\"legal\":true";
                String payload = getPayloadForSubmitDraft(cdrId, jsonObj.get("documentFileId").toString(), randomKeyForFileUpload, permissions, false);
                submitDraftObj.hitSubmitDraft(payload);

                uploadFileName = "Sample_C4491_2.txt";
                FileUtils.copyFile(configFilePath, "Sample.txt", configFilePath, uploadFileName);

                randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
                draftResponse = uploadDocument(cdrId, uploadFileName, randomKeyForFileUpload);

                jsonObj = new JSONObject(draftResponse);

                permissions = "\"financial\":true";
                payload = getPayloadForSubmitDraft(cdrId, jsonObj.get("documentFileId").toString(), randomKeyForFileUpload, permissions, false);
                submitDraftObj.hitSubmitDraft(payload);

                updateDB(true, false, false, otherUserName);
                checkObj.hitCheck(otherUserName, otherUserPassword);

                payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":10,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
                String communicationTabResponse = TabListDataHelper.getTabListDataResponse(160, cdrId, 65, payload);

                int dataLength = new JSONObject(communicationTabResponse).getJSONArray("data").length();

                if (dataLength == 0) {
                    csAssert.assertFalse(true, "No Document Visible to Other User even when it has Legal Permission enabled");
                }

                if (dataLength > 1) {
                    csAssert.assertFalse(true, "Document with Financial classification is also visible to Other User even when it doesn't have permission");
                }
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C4491. " + e.getMessage());
        } finally {
            checkObj.hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));

            if (cdrId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId);
            }

            updateDB(true, true, true);
        }

        csAssert.assertAll();
    }

    /*
    TC-C4493: Verify User should be able to Update Document Classification as per access.
     */
    @Test
    public void testC4493() {
        CustomAssert csAssert = new CustomAssert();
        int cdrId = createCDR();

        try {
            logger.info("Starting Test TC-C4493: Verify User should be able to Update Document Classification as per access.");

            if (cdrId == -1) {
                throw new Exception("Couldn't Create CDR. Hence Cannot validate further.");
            }

            String uploadFileName = "Sample_C4493.txt";
            FileUtils.copyFile(configFilePath, "Sample.txt", configFilePath, uploadFileName);

            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
            String draftResponse = uploadDocument(cdrId, uploadFileName, randomKeyForFileUpload);

            if (csAssert.getAllAssertionMessages().equalsIgnoreCase("")) {
                JSONObject jsonObj = new JSONObject(draftResponse);

                String permissions = "\"legal\":true";
                String payload = getPayloadForSubmitDraft(cdrId, jsonObj.get("documentFileId").toString(), randomKeyForFileUpload, permissions, false);
                submitDraftObj.hitSubmitDraft(payload);

                updateDB(true, true, false, otherUserName);
                checkObj.hitCheck(otherUserName, otherUserPassword);

                payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":10,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
                String contractDocumentTabResponse = TabListDataHelper.getTabListDataResponse(160, cdrId, 367, payload);

                jsonObj = new JSONObject(contractDocumentTabResponse);

                if (jsonObj.getJSONArray("data").length() == 1) {
                    String documentNameColumn = TabListDataHelper.getColumnIdFromColumnName(contractDocumentTabResponse, "documentname");
                    String documentFileId = jsonObj.getJSONArray("data").getJSONObject(0).getJSONObject(documentNameColumn)
                            .getString("value").split(":;")[4];

                    Edit editObj = new Edit();
                    String editGetResponse = editObj.hitEdit("contract draft request", cdrId);
                    JSONObject jsonData = new JSONObject(editGetResponse).getJSONObject("body").getJSONObject("data");
                    JSONObject commentJsonObj = jsonData.getJSONObject("comment");

                    String commentDocumentsPayload = "{\"name\":\"commentDocuments\",\"multiEntitySupport\":false,\"values\":[{\"shareWithSupplierFlag\":false," +
                            "\"editableDocumentType\":true,\"editable\":true,\"templateTypeId\":1001,\"documentStatus\":{\"id\":1,\"name\":\"Draft\"}," +
                            "\"documentFileId\":" + documentFileId + ",\"legal\":true,\"financial\":true}]}";
                    commentJsonObj.put("commentDocuments", new JSONObject(commentDocumentsPayload));

                    jsonData.put("comment", commentJsonObj);

                    JSONObject finalPayload = new JSONObject();
                    JSONObject body = new JSONObject();
                    body.put("data", jsonData);
                    finalPayload.put("body", body);
                    payload = finalPayload.toString();

                    String editPostResponse = editObj.hitEdit("contract draft request", payload);
                    String status = ParseJsonResponse.getStatusFromResponse(editPostResponse);
                    if (!status.equalsIgnoreCase("success")) {
                        csAssert.assertFalse(true, "Other User Couldn't Update Document Classification due to " + status);
                    }
                } else {
                    csAssert.assertFalse(true, "Uploaded Document is not available to Other User.");
                }
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C4493. " + e.getMessage());
        } finally {
            checkObj.hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));

            if (cdrId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId);
            }

            updateDB(true, true, true);
        }

        csAssert.assertAll();
    }

    /*
    TC-C4557: Verify User should be able to Update Document Classification as per access for Contract
     */
    @Test
    public void testC4557() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C4557: Verify User should be able to Update Document Classification as per access for Contract.");

            int contractId = ListDataHelper.getLatestRecordId("contracts");

            String uploadFileName = "Sample_C4557.txt";
            FileUtils.copyFile(configFilePath, "Sample.txt", configFilePath, uploadFileName);

            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
            String draftResponse = uploadDocument(contractId, uploadFileName, randomKeyForFileUpload);

            if (csAssert.getAllAssertionMessages().equalsIgnoreCase("")) {
                JSONObject jsonObj = new JSONObject(draftResponse);

                String permissions = "\"legal\":true";
                String payload = getPayloadForSubmitDraft(61, contractId, jsonObj.get("documentFileId").toString(), randomKeyForFileUpload,
                        permissions, false);
                SubmitDraft.hitSubmitDraft("contracts", payload);

                updateDB(true, true, false, otherUserName);
                checkObj.hitCheck(otherUserName, otherUserPassword);

                payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":10,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
                String contractDocumentTabResponse = TabListDataHelper.getTabListDataResponse(61, contractId, 366, payload);

                jsonObj = new JSONObject(contractDocumentTabResponse);

                if (jsonObj.getJSONArray("data").length() > 0) {
                    JSONArray jsonArr = jsonObj.getJSONArray("data");
                    String documentNameColumn = TabListDataHelper.getColumnIdFromColumnName(contractDocumentTabResponse, "documentname");
                    boolean docFound = false;

                    for (int i = 0; i < jsonArr.length(); i++) {
                        String value = jsonObj.getJSONArray("data").getJSONObject(0).getJSONObject(documentNameColumn)
                                .getString("value");

                        if ((value.split(":;")[1] + "." + value.split(":;")[2]).equalsIgnoreCase(uploadFileName)) {
                            docFound = true;

                            String documentFileId = jsonObj.getJSONArray("data").getJSONObject(0).getJSONObject(documentNameColumn)
                                    .getString("value").split(":;")[4];

                            Edit editObj = new Edit();
                            String editGetResponse = editObj.hitEdit("contracts", contractId);
                            JSONObject jsonData = new JSONObject(editGetResponse).getJSONObject("body").getJSONObject("data");
                            JSONObject commentJsonObj = jsonData.getJSONObject("comment");

                            String commentDocumentsPayload = "{\"name\":\"commentDocuments\",\"multiEntitySupport\":false,\"values\":[{\"shareWithSupplierFlag\":false," +
                                    "\"editableDocumentType\":true,\"editable\":true,\"templateTypeId\":1001,\"documentStatus\":{\"id\":1,\"name\":\"Draft\"}," +
                                    "\"documentFileId\":" + documentFileId + ",\"legal\":true,\"financial\":true}]}";
                            commentJsonObj.put("commentDocuments", new JSONObject(commentDocumentsPayload));

                            jsonData.put("comment", commentJsonObj);

                            JSONObject finalPayload = new JSONObject();
                            JSONObject body = new JSONObject();
                            body.put("data", jsonData);
                            finalPayload.put("body", body);
                            payload = finalPayload.toString();

                            String editPostResponse = editObj.hitEdit("contracts", payload);
                            String status = ParseJsonResponse.getStatusFromResponse(editPostResponse);
                            if (!status.equalsIgnoreCase("success")) {
                                csAssert.assertFalse(true, "Other User Couldn't Update Document Classification due to " + status);
                            }

                            break;
                        }
                    }

                    if (!docFound) {
                        csAssert.assertFalse(true, "Uploaded Document is not available to Other User.");
                    }
                } else {
                    csAssert.assertFalse(true, "Uploaded Document is not available to Other User.");
                }
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C4493. " + e.getMessage());
        } finally {
            checkObj.hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));
            updateDB(true, true, true);
        }

        csAssert.assertAll();
    }

    /*
    TC-C4526: Verify Upload Executed Contract option on Sirion Client Setup Admin.
     */
    @Test
    public void testC4526() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C4526: Verify Upload Executed Contract option on Sirion Client Setup Admin.");
            ClientSetupHelper setupHelperObj = new ClientSetupHelper();
            String clientName = setupHelperObj.getClientNameFromId(clientId);
            setupHelperObj.loginWithClientSetupUser();

            String provisioningEditResponse = ProvisioningEdit.getProvisioningEditResponse(clientId, clientName);
            Elements nodes = Jsoup.parse(provisioningEditResponse).getElementById("permission").child(1).child(0).children();

            boolean permissionFound = false;
            for (int i = 0; i < nodes.size(); i = i + 2) {
                String entityNode = nodes.get(i).child(0).child(0).childNode(0).toString().trim().replace(":", "");

                if (entityNode.equalsIgnoreCase("Contract Draft Request")) {
                    Elements allPermissions = nodes.get(i + 1).child(0).child(0).child(0).child(0).children();

                    for (Element permissionNode : allPermissions) {
                        String permissionName = permissionNode.childNode(3).toString().trim();

                        if (permissionName.equalsIgnoreCase("Upload Executed Contract")) {
                            permissionFound = true;
                            break;
                        }
                    }

                    break;
                }
            }

            csAssert.assertTrue(permissionFound, "Upload Executed Contract Option not found under CDR on Sirion Client Setup Admin.");
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C4526. " + e.getMessage());
        } finally {
            adminHelperObj.loginWithEndUser();
        }

        csAssert.assertAll();
    }

    /*
    TC-C4527: Verify Upload executed contract option on Client Admin.
     */
    @Test
    public void testC4527() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C4527: Verify Upload Executed Contract Option on Client Admin.");
            adminHelperObj.loginWithClientAdminUser();
            String urgListResponse = MasterUserRoleGroupsList.getMasterUserRoleGroupsListResponse();
            Integer urgId = MasterUserRoleGroupsList.getUserRoleGroupId(urgListResponse, "Admin");
            String urgUpdateResponse = MasterUserRoleGroupsUpdate.getUpdateResponse(urgId);

            Elements nodes = Jsoup.parse(urgUpdateResponse).getElementById("permission").children();

            boolean permissionFound = false;
            for (int i = 0; i < nodes.size(); i = i + 2) {
                String entityNode = nodes.get(i).child(0).child(0).childNode(0).toString().trim().replace(":", "");

                if (entityNode.equalsIgnoreCase("Contract Draft Request")) {
                    Elements allPermissions = nodes.get(i + 1).child(0).child(0).child(0).child(0).children();

                    for (Element permissionNode : allPermissions) {
                        String permissionName = permissionNode.childNode(5).toString().trim();

                        if (permissionName.equalsIgnoreCase("Upload Executed Contract")) {
                            permissionFound = true;
                            break;
                        }
                    }

                    break;
                }
            }

            csAssert.assertTrue(permissionFound, "Upload Executed Contract Option not found under CDR on Client Admin.");
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C4527. " + e.getMessage());
        } finally {
            adminHelperObj.loginWithEndUser();
        }

        csAssert.assertAll();
    }

    /*
    TC-C4533: Verify Upload Executed Contract Permission behavior
     */
    @Test
    public void testC4533() {
        CustomAssert csAssert = new CustomAssert();
        int cdrId = -1;
        Set<String> defaultPermissions = adminHelperObj.getAllPermissionsForUser(ConfigureEnvironment.getEndUserLoginId(), clientId);

        try {
            logger.info("Starting Test TC-C4533: Verify Upload Executed Contract Permission behavior.");
            cdrId = createCDR();

            if (cdrId == -1) {
                throw new Exception("Couldn't Create CDR. Hence couldn't validate further.");
            }

            //Disable Upload Executed Contract Permission i.e. 743
            if (defaultPermissions.contains("743")) {
                Set<String> newPermissions = new HashSet<>(defaultPermissions);
                newPermissions.remove("743");

                String newPermissionsStr = newPermissions.toString().replace("[", "{").replace("]", "}");
                adminHelperObj.updatePermissionsForUser(ConfigureEnvironment.getEndUserLoginId(), clientId, newPermissionsStr);
            }

            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            String query = "select restrict_document_upload from contract_draft_request where id = " + cdrId;
            String value = sqlObj.doSelect(query).get(0).get(0);

            if (value.equalsIgnoreCase("f")) {
                String[] actions = {"SendForClientReview", "TestInternationalization"};
                boolean actionPerformed = true;
                String actionFailed = null;

                for (String actionName : actions) {
                    actionPerformed = performAction(cdrId, actionName);

                    if (!actionPerformed) {
                        actionFailed = actionName;
                        break;
                    }
                }

                if (actionPerformed) {
                    query = "select restrict_document_upload from contract_draft_request where id = " + cdrId;
                    value = sqlObj.doSelect(query).get(0).get(0);

                    if (!value.equalsIgnoreCase("t")) {
                        csAssert.assertFalse(true, "Restrict Document Upload is Still set to False in DB whereas it was expected to be True. " +
                                "User still has permission to upload document.");
                    }
                } else {
                    csAssert.assertFalse(true, "Action " + actionFailed + " failed. Hence skipping further validation.");
                }
            } else {
                csAssert.assertFalse(true, "User doesn't have permission to Upload Document whereas it was expected to be True.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C4533. " + e.getMessage());
        } finally {
            String defaultPermissionsStr = defaultPermissions.toString().replace("[", "{").replace("]", "}");
            adminHelperObj.updatePermissionsForUser(ConfigureEnvironment.getEndUserLoginId(), clientId, defaultPermissionsStr);

            if (cdrId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId);
            }
        }

        csAssert.assertAll();
    }

    private void updateDB(boolean legal, boolean financial, boolean business) {
        updateDB(legal, financial, business, ConfigureEnvironment.getEndUserLoginId());
    }

    private void updateDB(boolean legal, boolean financial, boolean business, String loginId) {
        PostgreSQLJDBC postgresObj = new PostgreSQLJDBC();

        String query = "update user_role_group set legal = " + legal + ", finance = " + financial + ", business_case = " + business + " where id = " + userRoleGroupId;
        postgresObj.updateDBEntry(query);

        query = "update user_role_group set legal = " + legal + ", finance = " + financial + ", business_case = " + business + " where id = " + cdrRoleGroupId;
        postgresObj.updateDBEntry(query);

        query = "update app_user set legal = " + legal + ", finance = " + financial + ", business_case = " + business + " where login_id = '" +
                loginId + "' and client_id = " + clientId;
        postgresObj.updateDBEntry(query);

        postgresObj.closeConnection();
    }

    private String uploadDocument(int cdrId, String fileName, String randomKeyForFileUpload) {
        FileUploadDraft fileUploadDraft = new FileUploadDraft();
        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("name", fileName.split("\\.")[0]);
        queryParameters.put("extension", fileName.split("\\.")[1]);
        queryParameters.put("entityTypeId", "160");
        queryParameters.put("entityId", String.valueOf(cdrId));
        queryParameters.put("key", randomKeyForFileUpload);

        return fileUploadDraft.hitFileUpload(configFilePath, fileName, queryParameters);
    }

    private String getPayloadForSubmitDraft(int cdrId, String documentFileId, String documentKey, String permissions, boolean privateCommunication) {
        return getPayloadForSubmitDraft(160, cdrId, documentFileId, documentKey, permissions, privateCommunication);
    }

    private String getPayloadForSubmitDraft(int entityTypeId, int recordId, String documentFileId, String documentKey, String permissions, boolean privateCommunication) {
        String payload = null;

        try {
            String showPageResponse = ShowHelper.getShowResponseVersion2(entityTypeId, recordId);

            if (APIUtils.validJsonResponse(showPageResponse)) {
                JSONObject jsonData = new JSONObject(showPageResponse).getJSONObject("body").getJSONObject("data");
                JSONObject commentJsonObj = jsonData.getJSONObject("comment");
                JSONObject draftObj = commentJsonObj.getJSONObject("draft");
                draftObj.put("values", true);
                commentJsonObj.put("draft", draftObj);

                String commentDocumentsPayload = "{\"values\":[{\"templateTypeId\":1001,\"documentFileId\":" + documentFileId +
                        ",\"documentTags\":[],\"documentSize\":26,\"key\":\"" + documentKey + "\",\"documentStatusId\":1,\"permissions\":{\"financial\":false," +
                        "\"legal\":false,\"businessCase\":false},\"performanceData\":false,\"searchable\":false,\"shareWithSupplierFlag\":false";

                if (permissions != null && !permissions.equalsIgnoreCase("")) {
                    commentDocumentsPayload = commentDocumentsPayload.concat("," + permissions);
                }

                commentDocumentsPayload = commentDocumentsPayload.concat("}]}");

                commentJsonObj.put("commentDocuments", new JSONObject(commentDocumentsPayload));

                if (privateCommunication) {
                    JSONObject privateCommunicationJsonObj = commentJsonObj.getJSONObject("privateCommunication");
                    privateCommunicationJsonObj.put("values", true);
                    commentJsonObj.put("privateCommunication", privateCommunicationJsonObj);
                }

                jsonData.put("comment", commentJsonObj);

                JSONObject finalPayload = new JSONObject();
                JSONObject body = new JSONObject();
                body.put("data", jsonData);
                finalPayload.put("body", body);

                payload = finalPayload.toString();
            }
        } catch (Exception e) {
            logger.error("Exception while getting payload for Submit draft. error : {}", e.getMessage());
        }

        return payload;
    }

    private boolean performAction(int cdrId, String actionName) {
        try {
            String actionsResponse = Actions.getActionsV3Response(160, cdrId);
            String apiPath = Actions.getAPIForActionV3(actionsResponse, actionName);
            String showResponse = ShowHelper.getShowResponseVersion2(160, cdrId);
            String payload = "{\"body\":{\"data\":" + new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").toString() + "}}";

            String actionPerformResponse = executor.post(apiPath, ApiHeaders.getDefaultLegacyHeaders(), payload).getResponse().getResponseBody();
            String status = ParseJsonResponse.getStatusFromResponse(actionPerformResponse);

            return status.equalsIgnoreCase("success");
        } catch (Exception e) {
            return false;
        }
    }
}