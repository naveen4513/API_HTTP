package com.sirionlabs.test.pod.ca;

import com.sirionlabs.api.clientAdmin.masterUserRoleGroups.MasterUserRoleGroupsUpdate;
import com.sirionlabs.api.commonAPI.*;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.entityCreation.ContractDraftRequest;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TestClauseReviewAndUpdateCA1117 extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestClauseReviewAndUpdateCA1117.class);

    private String configFilePath;
    private String configFileName;
    private String extraFieldsConfigFileName;

    private long maxWaitTime = 300000;
    private long pollingTime = 10000;

    private Check checkObj = new Check();
    private AdminHelper adminHelperObj = new AdminHelper();

    @BeforeClass
    public void beforeClass() {
        configFilePath = "src/test/resources/TestConfig/CAPod/ClauseReviewAndUpdate";
        configFileName = "TestClauseReviewAndUpdate.cfg";
        extraFieldsConfigFileName = "ExtraFields.cfg";
    }

    /*
    TC-C90879: Verify Permission Cancel Review under CDR in URG.
     */
    @Test
    public void testC90879() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C90879: Verify Permission Cancel Review under CDR in URG.");
            adminHelperObj.loginWithClientAdminUser();

            int roleGroupId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "default", "rolegroupid"));
            String roleGroupResponse = MasterUserRoleGroupsUpdate.getUpdateResponse(roleGroupId);
            Document html = Jsoup.parse(roleGroupResponse);
            Elements allGroups = html.getElementsByClass("accordion user_permission").get(0).children();

            boolean cdrGroupFound = false;
            for (int i = 0; i < allGroups.size(); i = i + 2) {
                String groupName = allGroups.get(i).child(0).child(0).childNode(0).toString().trim().replace(":", "");

                if (groupName.equalsIgnoreCase("Contract Draft Request")) {
                    cdrGroupFound = true;

                    Elements allPermissions = allGroups.get(i + 1).child(0).child(0).child(0).child(0).children();
                    boolean permissionFound = false;

                    for (Element permission : allPermissions) {
                        String permissionName = permission.childNode(5).toString().trim();

                        if (permissionName.equalsIgnoreCase("Cancel Review")) {
                            permissionFound = true;
                            break;
                        }
                    }

                    if (!permissionFound) {
                        csAssert.assertFalse(true, "Cancel Review Permission not found under CDR Group.");
                    }

                    break;
                }
            }

            if (!cdrGroupFound) {
                csAssert.assertFalse(true, "Couldn't find Contract Draft Request Group in URG.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C90879: " + e.getMessage());
        } finally {
            checkObj.hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));
        }

        csAssert.assertAll();
    }


    /*
    TC-C90881: Verify Permission Allow Clause Edit under CDR in CDR Role Group.
     */
    @Test
    public void testC90881() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C90881: Verify Permission Allow Clause Edit under CDR in CDR Role Group.");
            adminHelperObj.loginWithClientAdminUser();

            int cdrRoleGroupId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "default",
                    "cdrparentrolegroupid"));
            String roleGroupResponse = MasterUserRoleGroupsUpdate.getUpdateResponse(cdrRoleGroupId);
            Document html = Jsoup.parse(roleGroupResponse);
            Elements allGroups = html.getElementsByClass("accordion user_permission").get(0).children();

            boolean cdrGroupFound = false;
            for (int i = 0; i < allGroups.size(); i = i + 2) {
                String groupName = allGroups.get(i).child(0).child(0).childNode(0).toString().trim().replace(":", "");

                if (groupName.equalsIgnoreCase("Contract Draft Request")) {
                    cdrGroupFound = true;

                    Elements allPermissions = allGroups.get(i + 1).child(0).child(0).child(0).child(0).children();
                    boolean permissionFound = false;

                    for (Element permission : allPermissions) {
                        String permissionName = permission.childNode(5).toString().trim();

                        if (permissionName.equalsIgnoreCase("Allow Clause Edit")) {
                            permissionFound = true;
                            break;
                        }
                    }

                    if (!permissionFound) {
                        csAssert.assertFalse(true, "Allow Clause Edit Permission not found under CDR Group.");
                    }

                    break;
                }
            }

            if (!cdrGroupFound) {
                csAssert.assertFalse(true, "Couldn't find Contract Draft Request Group in URG.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C90881: " + e.getMessage());
        } finally {
            checkObj.hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));
        }

        csAssert.assertAll();
    }


    /*
    TC-C90880: Verify End User having Cancel Review permission is able to Cancel Review initiated on Clause
    TC-C90885: Verify Permission Allow Clause Edit at End User
     */
    @Test
    public void testC90880() {
        CustomAssert csAssert = new CustomAssert();
        int cdrId = -1;

        try {
            logger.info("Starting Test TC-C90880: Verify End User having Cancel Review permission is able to Cancel Review initiated on Clause.");
            cdrId = createAndUpdateCDR();

            if (cdrId != -1) {
                String tabListResponse = TabListDataHelper.getTabListDataResponse(160, cdrId, 367);
                JSONObject jsonObj = new JSONObject(tabListResponse).getJSONArray("data").getJSONObject(0);

                String documentNameColumnId = TabListDataHelper.getColumnIdFromColumnName(tabListResponse, "documentname");
                String documentId = jsonObj.getJSONObject(documentNameColumnId).getString("value").split(":;")[4];

                long timeSpent = 0L;
                boolean uploadCompleted = false;

                while (timeSpent <= maxWaitTime) {
                    String deviationSummaryResponse = DeviationSummary.getDeviationSummaryResponse("contract draft request", cdrId, documentId);
                    uploadCompleted = DeviationSummary.isDeviationCompleted(deviationSummaryResponse);

                    if (uploadCompleted) {
                        break;
                    } else {
                        Thread.sleep(pollingTime);
                        timeSpent += pollingTime;
                    }
                }

                if (uploadCompleted) {
                    //Perform Workflow Action
                    if (performActionOnClause(cdrId, "SendForClientReview")) {
                        String listDataPayload = getContractClausesPayload(documentId, cdrId, null);

                        String listDataResponse = ListDataHelper.getListDataResponseVersion2(492, listDataPayload, false, null);
                        String reviewEditableColumnId = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "reviewEditable");
                        jsonObj = new JSONObject(listDataResponse);

                        if (jsonObj.getJSONArray("data").length() > 0) {
                            jsonObj = jsonObj.getJSONArray("data").getJSONObject(0);

                            String reviewEditableValue = jsonObj.getJSONObject(reviewEditableColumnId).getString("value");

                            //Verify Negative Flow after Revoking permission.
                            validateCancelReviewNegativeFlow(documentId, cdrId, csAssert);

                            validateCancelReviewPositiveFlow(documentId, cdrId, csAssert);

                            //Validate Allow Clause Edit Permission
                            testC90885(documentId, cdrId, csAssert);

                            if (reviewEditableValue.equalsIgnoreCase("true")) {
                                //Hit API to Cancel Review
                                String contentControlColumnId = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "contentControlId");
                                String contentControlIdValue = jsonObj.getJSONObject(contentControlColumnId).getString("value");
                                String cancelReviewPayload = "{\"contentControlId\":\"" + contentControlIdValue + "\",\"documentFileId\":\"" + documentId +
                                        "\",\"modifiedText\":null,\"reviewTaskDetail\":{\"taskId\":1002,\"clientId\":0,\"initialTask\":false,\"finalTask\":true," +
                                        "\"labelId\":29921,\"taskName\":\"Review Cancelled\",\"taskButtonName\":\"Cancel Review\",\"visibleOutward\":true," +
                                        "\"buttonLabelId\":30056,\"reviewPermission\":939}}";

                                int cancelReviewResponseCode = PerformTask.cancelReview("contract draft request", cancelReviewPayload);

                                if (cancelReviewResponseCode == 200) {
                                    //Verify Review Editable is False.
                                    listDataPayload = getContractClausesPayload(documentId, cdrId, "null");

                                    listDataResponse = ListDataHelper.getListDataResponseVersion2(492, listDataPayload, false, null);
                                    jsonObj = new JSONObject(listDataResponse);

                                    if (jsonObj.getJSONArray("data").length() > 0) {
                                        jsonObj = jsonObj.getJSONArray("data").getJSONObject(0);

                                        reviewEditableValue = jsonObj.getJSONObject(reviewEditableColumnId).getString("value");

                                        if (reviewEditableValue.equalsIgnoreCase("true")) {
                                            csAssert.assertFalse(true, "Review Editable is still true in ListData 492 API Response.");
                                        }
                                    } else {
                                        csAssert.assertFalse(true, "No Record coming in ListData 492 API after Cancelling Review.");
                                    }
                                } else {
                                    csAssert.assertFalse(true, "Couldn't Cancel Review for Document Id " + documentId + " on CDR Contract Clauses Tab.");
                                }
                            } else {
                                csAssert.assertFalse(true, "Review Button not found in Contract Clauses Tab of CDR.");
                            }
                        } else {
                            csAssert.assertFalse(true, "Review button not coming up in Contract Clauses Tab of CDR.");
                        }
                    } else {
                        csAssert.assertFalse(true, "Workflow Action on CDR failed.");
                    }
                } else {
                    csAssert.assertFalse(true, "Deviation not completed within Specified Time. Hence couldn't not validate further.");
                }
            } else {
                csAssert.assertFalse(true, "Couldn't Create/Update CDR");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C90880: " + e.getMessage());
        } finally {
            if (cdrId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId);
            }
        }

        csAssert.assertAll();
    }

    private int createAndUpdateCDR() throws Exception {
        //Create CDR
        String cdrCreateResponse = ContractDraftRequest.createCDR(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName,
                "c90880 cdr creation", true);

        String cdrResult = ParseJsonResponse.getStatusFromResponse(cdrCreateResponse);

        if (cdrResult.equalsIgnoreCase("success")) {
            int cdrId = CreateEntity.getNewEntityId(cdrCreateResponse);

            logger.info("Adding Template to Newly Created CDR.");
            Edit editObj = new Edit();

            String editGetResponse = editObj.getEditPayload("contract draft request", cdrId);
            Map<String, String> editProperties = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileName,
                    "c90880 cdr edit");

            String mappedTemplatePayload = editProperties.get("mappedContractTemplates");

            JSONObject jsonObj = new JSONObject(editGetResponse).getJSONObject("body").getJSONObject("data");

            jsonObj.getJSONObject("mappedContractTemplates").put("values", new JSONObject(mappedTemplatePayload).getJSONArray("values"));
            String updatePayload = "{\"body\":{\"data\":" + jsonObj.toString() + "}}";
            String updateResponse = editObj.hitEdit("contract draft request", updatePayload);
            String updateResult = ParseJsonResponse.getStatusFromResponse(updateResponse);

            if (updateResult.equalsIgnoreCase("success")) {
                //Add new template doc to get some deviation here.
                return cdrId;
            } else {
                return -1;
            }
        } else {
            return -1;
        }
    }

    private boolean performActionOnClause(int cdrId, String actionName) {
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

    private String getContractClausesPayload(String documentId, int cdrId, String deviationStatus) {
        return "{\"filterMap\":{\"offset\":0,\"size\":1,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\"," +
                "\"entityTypeId\":160,\"customFilter\":{\"clauseDeviationFilter\":{\"deviationStatus\":" + deviationStatus + "," +
                "\"documentFileId\":\"" + documentId + "\",\"entityId\":" + cdrId + "}},\"filterJson\":{}}}";
    }

    private void validateCancelReviewNegativeFlow(String documentId, int cdrId, CustomAssert csAssert) {
        String userName = ConfigureEnvironment.getEndUserLoginId();
        int clientId = adminHelperObj.getClientId();
        Set<String> allPermissions = adminHelperObj.getAllPermissionsForUser(userName, clientId);

        try {
            //Revoke Permission and Check for negative flow
            Set<String> newPermissions = new HashSet<>(allPermissions);
            newPermissions.remove("939");

            String newPermissionsStr = newPermissions.toString().replace("[", "{").replace("]", "}");
            adminHelperObj.updatePermissionsForUser(userName, clientId, newPermissionsStr);

            String listDataPayload = getContractClausesPayload(documentId, cdrId, "5");
            String listDataResponse = ListDataHelper.getListDataResponseVersion2(492, listDataPayload, false, null);
            JSONObject jsonObj = new JSONObject(listDataResponse);

            if (jsonObj.getJSONArray("data").length() > 0) {
                jsonObj = jsonObj.getJSONArray("data").getJSONObject(0);
                String reviewTaskDetailColumnId = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "reviewTaskDetail");
                String reviewTaskDetailValue = jsonObj.getJSONObject(reviewTaskDetailColumnId).getString("value").replaceAll("\\\\", "");
                JSONObject reviewTaskJsonObj = new JSONObject(reviewTaskDetailValue).getJSONObject("currentTask");
                JSONArray jsonArr = reviewTaskJsonObj.getJSONArray("nextTaskIds");

                for (int i = 0; i < jsonArr.length(); i++) {
                    String taskButtonName = jsonArr.getJSONObject(i).getString("taskButtonName");

                    if (taskButtonName.equalsIgnoreCase("Cancel Review")) {
                        csAssert.assertFalse(true, "Even after Revoking Cancel Review Permission, Still End User able to see Cancel Review button");
                        break;
                    }
                }
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Negative Flow of Cancel Review Permission. " + e.getMessage());
        } finally {
            //Give Permission back to Cancel Review
            String permissionStr = allPermissions.toString().replace("[", "{").replace("]", "}");
            adminHelperObj.updatePermissionsForUser(userName, clientId, permissionStr);
        }
    }

    private void validateCancelReviewPositiveFlow(String documentId, int cdrId, CustomAssert csAssert) {
        try {
            String listDataPayload = getContractClausesPayload(documentId, cdrId, "5");
            String listDataResponse = ListDataHelper.getListDataResponseVersion2(492, listDataPayload, false, null);
            JSONObject jsonObj = new JSONObject(listDataResponse);

            if (jsonObj.getJSONArray("data").length() > 0) {
                jsonObj = jsonObj.getJSONArray("data").getJSONObject(0);
                String reviewTaskDetailColumnId = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "reviewTaskDetail");
                String reviewTaskDetailValue = jsonObj.getJSONObject(reviewTaskDetailColumnId).getString("value").replaceAll("\\\\", "");
                JSONObject reviewTaskJsonObj = new JSONObject(reviewTaskDetailValue).getJSONObject("currentTask");
                JSONArray jsonArr = reviewTaskJsonObj.getJSONArray("nextTaskIds");

                boolean cancelReviewButtonFound = false;

                for (int i = 0; i < jsonArr.length(); i++) {
                    String taskButtonName = jsonArr.getJSONObject(i).getString("taskButtonName");

                    if (taskButtonName.equalsIgnoreCase("Cancel Review")) {
                        cancelReviewButtonFound = true;

                        if (!jsonArr.getJSONObject(i).getBoolean("visibleOutward")) {
                            csAssert.assertFalse(true,
                                    "VisibleOutward flag is off in ListData API Response for CDR Contract Clauses Tab. However Task is present.");
                        }

                        break;
                    }
                }

                if (!cancelReviewButtonFound) {
                    csAssert.assertFalse(true, "Cancel Review Button not found even after the End User having permission.");
                }
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Positive Flow of Cancel Review Permission. " + e.getMessage());
        }
    }

    /*private String getTagId(int cdrId) {
        String cdrShowResponse = ShowHelper.getShowResponseVersion2(160, cdrId);
        JSONObject jsonObj = new JSONObject(cdrShowResponse).getJSONObject("body").getJSONObject("data").getJSONObject("mappedContractTemplates")
                .getJSONArray("values").getJSONObject(0).getJSONObject("mappedTags");

        return JSONObject.getNames(jsonObj)[0];
    }*/

    private void testC90885(String documentId, int cdrId, CustomAssert csAssert) {
        try {
            logger.info("Validating Allow Clause Edit Permission after Revoking Permission.");

            //Revoke Allow Clause Edit permission in DB.
            PostgreSQLJDBC postgresObj = new PostgreSQLJDBC();
            String rgId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "default", "cdrparentrolegroupid");
            postgresObj.updateDBEntry("delete from link_user_group_role where role_id = 940 and user_group_id = " + rgId);

            String listDataPayload = getContractClausesPayload(documentId, cdrId, null);
            String listDataResponse = ListDataHelper.getListDataResponseVersion2(492, listDataPayload, false, null);
            JSONObject jsonObj = new JSONObject(listDataResponse).getJSONArray("data").getJSONObject(0);

            String reviewTaskDetailColumnId = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "reviewTaskDetail");
            String reviewTaskDetailValue = jsonObj.getJSONObject(reviewTaskDetailColumnId).getString("value").replaceAll("\\\\", "");

            jsonObj = new JSONObject(reviewTaskDetailValue).getJSONObject("currentTask");

            if (jsonObj.getBoolean("editAllowed")) {
                csAssert.assertFalse(true, "Clause Edit Allowed even after Revoking permission.");
            }

            //Giver Permission to Allow Clause Edit.
            int lastId = Integer.parseInt(postgresObj.doSelect("select id from link_user_group_role order by id desc limit 1").get(0).get(0)) + 1;

            postgresObj.updateDBEntry("insert into link_user_group_role values(" + lastId + ", 940, 1020, false, now(), now(), 1, 1)");
            listDataResponse = ListDataHelper.getListDataResponseVersion2(492, listDataPayload, false, null);
            jsonObj = new JSONObject(listDataResponse).getJSONArray("data").getJSONObject(0);

            reviewTaskDetailValue = jsonObj.getJSONObject(reviewTaskDetailColumnId).getString("value").replaceAll("\\\\", "");
            jsonObj = new JSONObject(reviewTaskDetailValue).getJSONObject("currentTask");

            if (!jsonObj.getBoolean("editAllowed")) {
                csAssert.assertFalse(true, "Clause Edit Not Allowed even after providing permission.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C90885: " + e.getMessage());
        }
    }
}