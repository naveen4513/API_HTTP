package com.sirionlabs.test.workflowPod;

import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.New;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.*;

public class TestFlowDownContractListingSupport {

    private final static Logger logger = LoggerFactory.getLogger(TestFlowDownContractListingSupport.class);

    private String configFilePath;
    private String configFileName;
    private String extraFieldsConfigFileName;

    private String crgId = "2000";
    private String rgShortName;

    @BeforeClass
    public void beforeClass() throws SQLException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestWF630ConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestWF630ConfigFileName");
        extraFieldsConfigFileName = "ExtraFields.cfg";

        PostgreSQLJDBC postgresObj = new PostgreSQLJDBC();
        rgShortName = postgresObj
                .doSelect("select short_name from role_group where flowdown_rolegroup = " + crgId + " and entity_type_id =61").get(0).get(0);

        postgresObj.closeConnection();
    }


    /*
    TC-C90470: Verify if Contract is created from multiple suppliers then RG Flow Down will be Union of all Parents.
     */
    @Test
    public void testC90470() {
        CustomAssert csAssert = new CustomAssert();
        int contractId = -1;

        try {
            logger.info("Starting Test TC-C90470: Verify if Contract is created from Multiple Suppliers then RG Flow Down will be Union of all Parents.");
            logger.info("Creating Multi-Supplier Contract.");

            String createSection = "multi supplier contract";
            Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, createSection);

            String[] parentSupplierIdArr = flowProperties.get("sourceid").split(",");

            contractId = createMultiSupplierContract(createSection);

            if (contractId != -1) {
                validateStakeholders(parentSupplierIdArr, contractId, csAssert, "");

                //Remove one stakeholder from one supplier
                flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, "supplier remove stakeholder");
                int supplierId = Integer.parseInt(flowProperties.get("supplierid"));
                editSupplier(supplierId, flowProperties.get("stakeholder"), csAssert, "Removing One Stakeholder from Supplier.");
                validateStakeholders(parentSupplierIdArr, contractId, csAssert, "Validation after Removing one Stakeholder from Supplier.");

                //Add one stakeholder to one supplier
                flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, "supplier add stakeholder");
                supplierId = Integer.parseInt(flowProperties.get("supplierid"));
                editSupplier(supplierId, flowProperties.get("stakeholder"), csAssert, "Adding One Stakeholder to Supplier.");
                validateStakeholders(parentSupplierIdArr, contractId, csAssert, "Validation after Adding one Stakeholder from Supplier.");
            } else {
                csAssert.assertFalse(true, "Couldn't Create Multi-Supplier Contract.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C90470: " + e.getMessage());
        } finally {
            if (contractId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contracts", contractId);
            }
        }

        csAssert.assertAll();
    }

    private void validateStakeholders(String[] parentSupplierIdArr, int contractId, CustomAssert csAssert, String additionalInfo) {
        try {
            List<String> allExpectedStakeHolders = getAllExpectedStakeholders(parentSupplierIdArr);

            if (allExpectedStakeHolders != null) {
                List<String> allActualStakeHolders = getAllActualStakeholders(contractId);

                if (allActualStakeHolders != null) {
                    if (allActualStakeHolders.size() == allExpectedStakeHolders.size()) {
                        for (String expectedStakeholder : allExpectedStakeHolders) {
                            csAssert.assertEquals(allActualStakeHolders.contains(expectedStakeholder), true, "Expected Stakeholder " +
                                    expectedStakeholder + " not found in Contract Flow Down. " + additionalInfo);
                        }
                    } else {
                        csAssert.assertFalse(true, "Flow Down failed. Expected Stakeholders: " + allExpectedStakeHolders + " and Actual Stakeholders: " +
                                allActualStakeHolders + ". " + additionalInfo);
                    }
                } else {
                    csAssert.assertFalse(true, "Couldn't get All Actual Stakeholders from Contract.");
                }
            } else {
                csAssert.assertFalse(true, "Couldn't get All Expected Stakeholders from All Parent Suppliers.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Stakeholders. " + additionalInfo + ". " + e.getMessage());
        }
    }

    private int createMultiSupplierContract(String contractCreateSection) {
        try {
            Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, contractCreateSection);

            String[] parentSupplierIdsArr = flowProperties.get("sourceid").split(",");
            String payload = "{\"documentTypeId\":4,\"parentEntity\":{\"entityIds\":" + Arrays.toString(parentSupplierIdsArr) + ",\"entityTypeId\":1}," +
                    "\"actualParentEntity\":{\"entityIds\":[" + parentSupplierIdsArr[0] + "],\"entityTypeId\":1},\"sourceEntity\":{\"entityIds\":[" +
                    parentSupplierIdsArr[0] + "],\"entityTypeId\":1}}";

            String createResponse = multiSupplierCreateResponse(payload, contractCreateSection);

            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                String status = ParseJsonResponse.getStatusFromResponse(createResponse);

                if (status.equalsIgnoreCase("success")) {
                    return CreateEntity.getNewEntityId(createResponse, "contracts");
                } else {
                    logger.error("Multi Supplier Contract Creation failed due to " + status);
                }
            } else {
                logger.error("Contract Create API Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Multi Supplier Contract. " + e.getMessage());
        }

        return -1;
    }

    private String multiSupplierCreateResponse(String newPayload, String contractCreateSection) {
        logger.info("Hitting New V1 API for Contracts");
        New newObj = new New();
        newObj.hitNewV1ForMultiSupplier("contracts", newPayload);
        String newResponse = newObj.getNewJsonStr();

        if (newResponse != null) {
            if (ParseJsonResponse.validJsonResponse(newResponse)) {
                CreateEntity createEntityHelperObj = new CreateEntity(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName,
                        contractCreateSection);

                Map<String, String> extraFields = createEntityHelperObj.setExtraRequiredFields("contracts");
                newObj.setAllRequiredFields(newResponse);
                Map<String, String> allRequiredFields = newObj.getAllRequiredFields();
                allRequiredFields = createEntityHelperObj.processAllChildFields(allRequiredFields, newResponse);
                allRequiredFields = createEntityHelperObj.processNonChildFields(allRequiredFields, newResponse);

                String createPayload = PayloadUtils.getPayloadForCreate(newResponse, allRequiredFields, extraFields, null, configFilePath,
                        extraFieldsConfigFileName);

                if (createPayload != null) {
                    logger.info("Hitting Create Api for Entity for Multi Supplier Contract");
                    Create createObj = new Create();
                    createObj.hitCreate("contracts", createPayload);
                    return createObj.getCreateJsonStr();
                } else {
                    logger.error("Contract Create Payload is null and hence cannot create Multi Supplier Contract.");
                }
            } else {
                logger.error("New V1 API Response is an Invalid JSON for Contracts.");
            }
        } else {
            logger.error("New API Response is null.");
        }

        return null;
    }

    private List<String> getAllExpectedStakeholders(String[] parentSupplierIdArr) {
        try {
            List<String> allExpectedStakeholders = new ArrayList<>();

            for (String supplierId : parentSupplierIdArr) {
                String showResponse = ShowHelper.getShowResponseVersion2(1, Integer.parseInt(supplierId));
                JSONObject jsonObj = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values");
                jsonObj = jsonObj.getJSONObject("rg_" + crgId);

                JSONArray jsonArr = jsonObj.getJSONArray("values");

                for (int i = 0; i < jsonArr.length(); i++) {
                    String name = jsonArr.getJSONObject(i).getString("name");

                    if (!allExpectedStakeholders.contains(name)) {
                        allExpectedStakeholders.add(name);
                    }
                }
            }

            return allExpectedStakeholders;
        } catch (Exception e) {
            logger.error("Exception while Getting All Expected Stakeholders from Parent Suppliers. " + e.getMessage());
            return null;
        }
    }

    private List<String> getAllActualStakeholders(int contractId) {
        try {
            List<String> allActualStakeholders = new ArrayList<>();

            String showResponse = ShowHelper.getShowResponseVersion2(61, contractId);
            JSONObject jsonObj = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values");
            jsonObj = jsonObj.getJSONObject(rgShortName);
            JSONArray jsonArr = jsonObj.getJSONArray("values");

            for (int i = 0; i < jsonArr.length(); i++) {
                allActualStakeholders.add(jsonArr.getJSONObject(i).getString("name"));
            }

            return allActualStakeholders;
        } catch (Exception e) {
            logger.error("Exception while Getting All Actual Stakeholders from Contract. " + e.getMessage());
            return null;
        }
    }

    private void editSupplier(int supplierId, String stakeHolderPayload, CustomAssert csAssert, String additionalInfo) {
        try {
            Edit editObj = new Edit();
            String editGetResponse = editObj.hitEdit("suppliers", supplierId);

            Map<String, String> fieldsPayloadMap = new HashMap<>();
            fieldsPayloadMap.put("stakeHolders", stakeHolderPayload);

            String editPostPayload = EntityOperationsHelper.createPayloadForEditPost(editGetResponse, fieldsPayloadMap);

            if (editPostPayload != null) {
                String editPostResponse = editObj.hitEdit("suppliers", editPostPayload);

                JSONObject jsonObj = new JSONObject(editPostResponse);
                String editStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();

                if (!editStatus.equalsIgnoreCase("success")) {
                    csAssert.assertTrue(false, "Couldn't update Supplier Id " + supplierId + ". " + additionalInfo);
                }
            } else {
                csAssert.assertFalse(true, "Couldn't get Supplier Edit Payload. " + additionalInfo);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Updating Supplier. " + additionalInfo + ". " + e.getMessage());
        }
    }
}