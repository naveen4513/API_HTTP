package com.sirionlabs.test.workflowPod;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.workflowRoleGroupFlowDown.WorkflowRoleGroupFlowDownCreate;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.dbHelper.RoleGroupFlowDownDbHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

public class TestWOR525EndToEnd {

    private final static Logger logger = LoggerFactory.getLogger(TestWOR525EndToEnd.class);

    @BeforeClass
    public void beforeClass() {
        new AdminHelper().loginWithClientAdminUser();
    }

    @AfterClass
    public void afterClass() {
        new Check().hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
    }

    @Test
    public void testWOR525EndToEndFlow() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating Workflow RoleGroup FlowDown WOR-525 End to End flow.");

            String[] parentEntitiesPool = {"suppliers", "contracts"};
            String[] childEntitiesPool = {"contracts", "service levels", "obligations", "child obligations", "actions", "issues", "change requests"};

            int randomNoForParentEntity = RandomNumbers.getRandomNumberWithinRangeIndex(0, parentEntitiesPool.length);
            String parentEntity = parentEntitiesPool[randomNoForParentEntity];
            int parentEntityTypeId = ConfigureConstantFields.getEntityIdByName(parentEntity);

            logger.info("Validating End to End flow for Parent Entity {}", parentEntity);

            //Create Workflow RoleGroup FlowDown
            String[] parentEntityTypeIdArr = {String.valueOf(parentEntityTypeId)};
            int clientId = new AdminHelper().getClientId();

            for (String childEntity : childEntitiesPool) {
                if (parentEntityTypeId == 61 && childEntity.equalsIgnoreCase("contracts")) {
                    continue;
                }

                int childEntityTypeId = ConfigureConstantFields.getEntityIdByName(childEntity);
                String roleGroupId = (parentEntityTypeId == 1) ? "2451" : "2092";

                String[] childEntityTypeIdArr = {String.valueOf(childEntityTypeId)};
                String[] roleGroupIdArr = {roleGroupId};
                String[] clientIdArr = {String.valueOf(clientId)};
                String[] deletedArr = {"false"};

                logger.info("Creating Workflow RoleGroup FlowDown for Parent Entity {} and Child Entity {}.", parentEntity, childEntity);

                String payloadForCreate = WorkflowRoleGroupFlowDownCreate.getPayload(parentEntityTypeIdArr, childEntityTypeIdArr, roleGroupIdArr, clientIdArr, deletedArr);

                String createResponse = WorkflowRoleGroupFlowDownCreate.getCreateResponse(payloadForCreate).getResponseBody();

                if (ParseJsonResponse.validJsonResponse(createResponse)) {
                    boolean flowCreated = new JSONObject(createResponse).getBoolean("success");

                    if (flowCreated) {
                        List<String> dataInDb = RoleGroupFlowDownDbHelper.getRoleGroupFlowDownDataFromDB("id", parentEntityTypeIdArr[0], childEntityTypeIdArr[0],
                                roleGroupIdArr[0], clientIdArr[0]);

                        if (!dataInDb.isEmpty()) {
                            int entryId = Integer.parseInt(dataInDb.get(0));

                            //Validate Listing
                            validateListingData(parentEntityTypeId, childEntityTypeId, roleGroupId, Boolean.parseBoolean(deletedArr[0]), clientId, csAssert);

                            //Validate Edit/Update
                            validateRoleGroupFlowDownEdit(parentEntityTypeIdArr, childEntityTypeIdArr, roleGroupIdArr, clientIdArr, csAssert);

                            //Delete RoleGroup FlowDown
                            logger.info("Deleting Workflow RoleGroup FlowDown Id: {}", entryId);
                            RoleGroupFlowDownDbHelper.deleteRoleGroupFlowDownDataInDb(entryId);
                        } else {
                            csAssert.assertTrue(false, "Couldn't get RoleGroup FlowDown data from DB for Parent Entity " + parentEntity +
                                    " and Child Entity " + childEntity);
                        }
                    } else {
                        csAssert.assertTrue(false, "API Response Validation failed. Expected Result: Success and Actual Result: Failure");
                    }
                } else {
                    csAssert.assertTrue(false, "Workflow RoleGroup FlowDown Create API Response is an Invalid JSON for Parent Entity " + parentEntity +
                            ", Child Entity " + childEntity);
                }
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Workflow RoleGroup FlowDown End to End Flow. " + e.getMessage());
        }
        csAssert.assertAll();
    }

    private void validateListingData(int parentEntityTypeId, int childEntityTypeId, String roleGroupId, boolean deleted, int clientId, CustomAssert csAssert) {
        try {
            logger.info("Validating RoleGroup FlowDown Listing.");
            ListRendererListData listDataObj = new ListRendererListData();
            String payload = "{\"filterMap\":{\"parentEntityTypeId\":" + parentEntityTypeId + ",\"roleGroupId\":" + roleGroupId + "}}";

            listDataObj.hitListRendererListData(495, true, payload, null);
            String listDataResponse = listDataObj.getListDataJsonStr();

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                List<List<String>> allExpectedChildEntityTypeIds = RoleGroupFlowDownDbHelper.getAllChildEntityTypeIdsForParentEntityTypeIdAndRoleGroupId(parentEntityTypeId,
                        Integer.parseInt(roleGroupId), clientId);

                if (allExpectedChildEntityTypeIds == null) {
                    throw new SkipException("Couldn't Get All Expected Child Entity Type Ids for Parent Entity Type Id " + parentEntityTypeId + " and RoleGroupId " +
                            roleGroupId + " from DB.");
                }

                JSONArray jsonArr = new JSONObject(listDataResponse).getJSONArray("data");

                if (allExpectedChildEntityTypeIds.isEmpty()) {
                    csAssert.assertTrue(jsonArr.length() == 0, "List Data Validation failed. Expected No of records: 0 and Actual No of Records: " +
                            jsonArr.length());
                } else {
                    for (List<String> oneRowData : allExpectedChildEntityTypeIds) {
                        String expectedChildEntityTypeId = oneRowData.get(0);
                        boolean childEntityFound = false;

                        String childEntityTypeColumnId = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "child_entity_type");
                        String deletedColumnId = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "deleted");

                        for (int i = 0; i < jsonArr.length(); i++) {
                            //Validate Child Entity Type Id in Listing
                            String actualChildEntityTypeId = jsonArr.getJSONObject(i).getJSONObject(childEntityTypeColumnId).getString("value");
                            if (actualChildEntityTypeId.equalsIgnoreCase(expectedChildEntityTypeId)) {
                                childEntityFound = true;

                                //Validate Deleted
                                String actualDeletedValue = jsonArr.getJSONObject(0).getJSONObject(deletedColumnId).getString("value");
                                csAssert.assertTrue(actualDeletedValue.equalsIgnoreCase(String.valueOf(deleted)), "Expected Deleted Value: " + deleted +
                                        " and Actual Deleted Value: " + actualDeletedValue + " in ListData API Response");

                                break;
                            }
                        }

                        csAssert.assertTrue(childEntityFound, "Child Entity Type Id: " + childEntityTypeId +
                                " not found in ListData API Response for ParentEntityTypeId " + parentEntityTypeId + " and RoleGroupId " + roleGroupId);
                    }
                }
            } else {
                csAssert.assertTrue(false, "ListData API Response for Workflow RoleGroup FlowDown is an Invalid JSON for Parent Entity Type Id: " +
                        parentEntityTypeId + " and Role Group Id: " + roleGroupId);
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Listing for RoleGroup FlowDown for Parent Entity Type Id: " +
                    parentEntityTypeId + " and Role Group Id: " + roleGroupId + ". " + e.getMessage());
        }
    }

    private void validateRoleGroupFlowDownEdit(String[] parentEntityTypeIdArr, String[] childEntityTypeIdArr, String[] roleGroupIdArr, String[] clientIdArr,
                                               CustomAssert csAssert) {
        try {
            logger.info("Validating Workflow RoleGroup FlowDown Edit.");
            String[] deletedArr = {"true"};
            String payload = WorkflowRoleGroupFlowDownCreate.getPayload(parentEntityTypeIdArr, childEntityTypeIdArr, roleGroupIdArr, clientIdArr, deletedArr);
            String editResponse = WorkflowRoleGroupFlowDownCreate.getCreateResponse(payload).getResponseBody();

            if (ParseJsonResponse.validJsonResponse(editResponse)) {
                boolean flowUpdated = new JSONObject(editResponse).getBoolean("success");

                if (flowUpdated) {
                    //Validate Listing
                    validateListingData(Integer.parseInt(parentEntityTypeIdArr[0]), Integer.parseInt(childEntityTypeIdArr[0]), roleGroupIdArr[0], false,
                            Integer.parseInt(clientIdArr[0]), csAssert);
                } else {
                    csAssert.assertTrue(false, "Workflow RoleGroup FlowDown Edit failed.");
                }
            } else {
                csAssert.assertTrue(false, "Workflow RoleGroup FlowDown Edit Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Edit of Workflow RoleGroup FlowDown for ParentEntityTypeId " +
                    parentEntityTypeIdArr[0] + " and ChildEntityTypeId " + childEntityTypeIdArr[0] + ". " + e.getMessage());
        }
    }
}