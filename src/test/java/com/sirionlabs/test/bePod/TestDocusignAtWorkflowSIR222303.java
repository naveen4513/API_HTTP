package com.sirionlabs.test.bePod;

import com.sirionlabs.api.clientAdmin.workflow.WorkFlowCreate;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.DefaultUserListMetadataHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.entityCreation.ChangeRequest;
import com.sirionlabs.helper.entityCreation.ContractDraftRequest;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.WorkOrderRequest;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Listeners(value = MyTestListenerAdapter.class)
public class TestDocusignAtWorkflowSIR222303 {

    private final static Logger logger = LoggerFactory.getLogger(TestDocusignAtWorkflowSIR222303.class);

    private String configFilePath;
    private String configFileName;

    private WorkFlowCreate workFlowCreateObj = new WorkFlowCreate();
    private WorkflowActionsHelper workflowHelperObj = new WorkflowActionsHelper();
    private DefaultUserListMetadataHelper defaultMetadataHelperObj = new DefaultUserListMetadataHelper();

    @BeforeClass
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestDocusignAtWorkflowConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestDocusignAtWorkflowConfigFileName");
    }


    @DataProvider
    public Object[][] dataProviderForDocusign() {
        List<Object[]> allTestData = new ArrayList<>();

        logger.info("Setting all Flows to Test.");
        List<String> allFlowsToTest = getFlowsToTest();
        for (String flowToTest : allFlowsToTest) {
            allTestData.add(new Object[]{flowToTest.trim()});
        }
        logger.info("Total Flows to Test : {}", allTestData.size());
        return allTestData.toArray(new Object[0][]);
    }

    private List<String> getFlowsToTest() {
        List<String> flowsToTest = new ArrayList<>();

        try {
            String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "default", "testAllFlows");

            if (temp != null && !temp.trim().equalsIgnoreCase("false")) {
                logger.info("TestAllFlows property is set to True. Therefore all the flows are to validated");

                flowsToTest = ParseConfigFile.getAllSectionNames(configFilePath, configFileName);
                flowsToTest.remove("default");
            } else {
                String[] allFlows = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "default",
                        "flowstotest").split(Pattern.quote(","));

                for (String flow : allFlows) {
                    if (ParseConfigFile.containsSection(configFilePath, configFileName, flow.trim())) {
                        flowsToTest.add(flow.trim());
                    } else {
                        logger.info("Flow having name [{}] not found in Config File.", flow.trim());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while getting Flows to Test for Docusign at Workflow Step Validation. {}", e.getMessage());
        }
        return flowsToTest;
    }


    @Test(dataProvider = "dataProviderForDocusign")
    public void testDocusignAtWorkflow(String flowToTest) {
        CustomAssert csAssert = new CustomAssert();
        List<Integer> newRecordsList = new ArrayList<>();
        List<String> newWorkflowIdsList = new ArrayList<>();
        String entityName = null;

        try {
            logger.info("Starting Test for Flow [{}]", flowToTest);
            Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, flowToTest);

            entityName = flowProperties.get("entity");
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);

            logger.info("Validating Docusign at Workflow Step for Entity {}", entityName);

            //Upload new Workflow with flag on
            String workflowFileName = flowProperties.get("workflowfilenameflagon");
            String newWorkflowId = uploadWorkflow(flowToTest, entityName, entityTypeId, workflowFileName, true, csAssert);

            if (newWorkflowId == null) {
                throw new SkipException("Workflow with Flag On Upload failed for Flow [" + flowToTest + "]");
            }

            newWorkflowIdsList.add(newWorkflowId);

            //Create new entity with Flag on.
            String creationSectionName = flowProperties.get("creationsectionname");
            int newRecordId = createNewRecord(flowToTest, entityName, creationSectionName, csAssert);

            if (newRecordId == -1) {
                throw new SkipException("Couldn't Create New Record after Uploading Workflow for Entity " + entityName + " and Flow " + flowToTest);
            }

            newRecordsList.add(newRecordId);

            //Validate docusign for Flag On.
            validateDocusign(flowToTest, entityName, entityTypeId, newRecordId, true, csAssert);

            //Upload new workflow with Flag off.
            workflowFileName = flowProperties.get("workflowfilenameflagoff");
            newWorkflowId = uploadWorkflow(flowToTest, entityName, entityTypeId, workflowFileName, false, csAssert);

            if (newWorkflowId == null) {
                throw new SkipException("Workflow with Flag Off Upload failed for Flow [" + flowToTest + "]");
            }

            newWorkflowIdsList.add(newWorkflowId);

            //Create new Record with Flag off.
            newRecordId = createNewRecord(flowToTest, entityName, creationSectionName, csAssert);

            if (newRecordId == -1) {
                throw new SkipException("Couldn't Create New Record after Turning off Flag for Entity " + entityName + " and Flow " + flowToTest);
            }

            newRecordsList.add(newRecordId);

            //Validate after turning flag off.
            validateDocusign(flowToTest, entityName, entityTypeId, newRecordId, false, csAssert);

        } catch (SkipException e) {
            logger.error(e.getMessage());
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Docusign at Workflow Step. " + e.getMessage());
        } finally {
            //Delete record
            if (!newRecordsList.isEmpty()) {
                for (Integer recordId : newRecordsList) {
                    EntityOperationsHelper.deleteEntityRecord(entityName, recordId);
                }
            }

            //Delete workflow
            if (!newWorkflowIdsList.isEmpty()) {
                if (!entityName.equalsIgnoreCase("contract draft request")) {
                    for (String workflowId : newWorkflowIdsList) {
                        workflowHelperObj.deleteWorkflow(entityName, workflowId);
                    }
                } else {
                    for (int i = 0; i < newWorkflowIdsList.size(); i++) {
                        workflowHelperObj.deleteWorkflow(entityName, newRecordsList.get(i), newWorkflowIdsList.get(i));
                    }
                }
            }
        }

        csAssert.assertAll();
    }

    private String uploadWorkflow(String flowToTest, String entityName, int entityTypeId, String workflowFileName, boolean flagOn, CustomAssert csAssert) {
        try {
            String oldWorkflowId = workflowHelperObj.getLatestWorkflowIdForEntity(entityTypeId);
            if (oldWorkflowId == null) {
                throw new SkipException("Couldn't Get Old Workflow Id for Entity " + entityName + " before Uploading new Workflow for Flow " + flowToTest);
            }

            logger.info("Uploading Workflow [{}] for Entity{} and Flow [{}]", workflowFileName, entityName, flowToTest);
            String newWorkflowName = "Docusign Workflow " + entityTypeId;

            newWorkflowName = flagOn ? newWorkflowName.concat(" Flag On") : newWorkflowName.concat(" Flag Off");

            workFlowCreateObj.hitWorkflowCreate(newWorkflowName, "", String.valueOf(entityTypeId), configFilePath, workflowFileName);

            String newWorkflowId = workflowHelperObj.getLatestWorkflowIdForEntity(entityTypeId);
            if (newWorkflowId == null) {
                throw new SkipException("Couldn't Get New Workflow Id for Entity " + entityName + " and Flow " + flowToTest);
            }

            if (newWorkflowId.equalsIgnoreCase(oldWorkflowId)) {
                throw new SkipException("Workflow Upload failed for Flow " + flowToTest);
            }

            return newWorkflowId;
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Uploading Workflow [" + workflowFileName + "] for entity " + entityName +
                    " and flow [" + flowToTest + "]");
        }

        return null;
    }

    private int createNewRecord(String flowToTest, String entityName, String creationSectionName, CustomAssert csAssert) {
        try {
            logger.info("Creating New Record for Entity {} and Flow [{}]", entityName, flowToTest);
            String createResponse = null;

            switch (entityName) {
                case "work order requests":
                    createResponse = WorkOrderRequest.createWOR(creationSectionName, true);
                    break;

                case "change requests":
                    createResponse = ChangeRequest.createChangeRequest(creationSectionName, true);
                    break;

                case "contract draft request":
                    createResponse = ContractDraftRequest.createCDR(creationSectionName, true);
                    break;
            }

            if (createResponse == null) {
                throw new SkipException("Couldn't Create New Record for Entity " + entityName + " and Flow [" + flowToTest + "]");
            }

            String status = ParseJsonResponse.getStatusFromResponse(createResponse);

            if (status.equalsIgnoreCase("success")) {
                return CreateEntity.getNewEntityId(createResponse);
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Creating New Record for Entity " + entityName + " and Flow [" + flowToTest + "]");
        }

        return -1;
    }

    private void validateDocusign(String flowToTest, String entityName, int entityTypeId, int newRecordId, boolean digitalSignatureFlagOn, CustomAssert csAssert) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("entityTypeId", String.valueOf(entityTypeId));
            params.put("entityId", String.valueOf(newRecordId));

            String defaultUserListMetadataResponse = defaultMetadataHelperObj.getDefaultUserListMetadataResponse(65, params);

            Boolean sendForSignature = null;

            if (ParseJsonResponse.validJsonResponse(defaultUserListMetadataResponse)) {
                JSONObject jsonObj = new JSONObject(defaultUserListMetadataResponse);

                if (jsonObj.has("sendForSignature")) {
                    sendForSignature = jsonObj.getBoolean("sendForSignature");
                }

                if (sendForSignature == null) {
                    throw new SkipException("Couldn't find Send for Signature flag in DefaultUserListMetadata API Response for Communications Tab of Entity " +
                            entityName + " and flow " + flowToTest);
                }

                if (digitalSignatureFlagOn) {
                    csAssert.assertTrue(sendForSignature, "Send for Signature flag is false in DefaultUserListMetadata API Response for Communications Tab of Entity "
                            + entityName + " and flow " + flowToTest);
                } else {
                    csAssert.assertTrue(!sendForSignature, "Send for Signature flag is true in DefaultUserListMetadata API Response for Communications Tab of Entity "
                            + entityName + " and flow " + flowToTest);
                }
            } else {
                csAssert.assertTrue(false, "DefaultUserListMetadata API Response for Communications Tabs of Entity " + entityName +
                        " and Flow " + flowToTest + " is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Docusign for Entity " + entityName + " and Flow " + flowToTest);
        }
    }
}