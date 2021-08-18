package com.sirionlabs.test.workflowPod;
import com.sirionlabs.api.clientAdmin.workflow.WorkFlowCreate;
import com.sirionlabs.api.commonAPI.CreateLinks;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.entityCreation.*;
import com.sirionlabs.helper.entityWorkflowAction.EntityWorkflowActionHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
public class TestWorkFlowTask {
    private final static Logger logger = LoggerFactory.getLogger(TestWorkFlowTask.class);
    private String configFilePath;
    private String configFileName;
    private String dataFilePath = "src/test/resources/TestData/WorkFlowTask";
    private String dataFileName = "Contract.xlsx";

    @BeforeClass
    public void beforeClass() {
        WorkFlowCreate workFlowCreate=new WorkFlowCreate();
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        workFlowCreate.hitWorkflowCreate(dataFileName+sdf.format(cal.getTime()),String.valueOf(5636),String.valueOf(61),dataFilePath,dataFileName);
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestWorkFlowTaskConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestWorkFlowTaskConfigFileName");
    }

    @DataProvider(name="testData")
    public Object[][] dataProvider() {
        List<Object[]> allTestData = new ArrayList<>();
        if (configFileName != null && configFilePath != null) {
            String[] entityToTest = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entitytotest").split(",");
            for (int i = 0; i < entityToTest.length; i++) {
                Map<String, String> allConstantProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, entityToTest[i].trim().toLowerCase());
                if (!allConstantProperties.isEmpty()) {
                    allTestData.add(new Object[]{allConstantProperties.get("functions"), allConstantProperties.get("services"), allConstantProperties.get("parententityid"), allConstantProperties.get("parententitytypeid"), allConstantProperties.get("parententityname"), allConstantProperties.get("childentityname"), allConstantProperties.get("childentitytypeid")});
                }

            }
        }

        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "testData",priority = 0)
    public void testInheritanceProperties(String functions, String services, String parentEntityId, String parentEntityTypeId, String parentEntityName, String childEntityName, String childEntityTypeId) {
        CustomAssert customAssert = new CustomAssert();
        try {
            List<String> expectedFunctions = getActualValue(functions);
            List<String> expectedServices = getActualValue(services);
            String createLinksV2Response = CreateLinks.getCreateLinksV2Response(Integer.parseInt(parentEntityTypeId), Integer.parseInt(parentEntityId));
            String createLinkForEntity = CreateLinks.getCreateLinkForEntity(createLinksV2Response, Integer.parseInt(childEntityTypeId));
            String jspApiResponse = CreateLinks.getJSPAPIResponse(createLinkForEntity);
            JSONObject jsonObject = new JSONObject(jspApiResponse).getJSONObject("body").getJSONObject("data");
            List<String> actualFunctions = getValue("functions", jsonObject);
            List<String> actualServices = getValue("services", jsonObject);
            for (String expectedFunction : expectedFunctions) {
                if (!actualFunctions.contains(expectedFunction)) {
                    customAssert.assertTrue(false, "parent function name { " + expectedFunction + " } not found in child entity");
                }
            }
            customAssert.assertTrue(expectedFunctions.size() == actualFunctions.size(), "number of expected function on parent entity and child entity are different");

            for (String expectedService : expectedServices) {
                if (!actualServices.contains(expectedService)) {
                    customAssert.assertTrue(false, "parent service name { " + expectedServices + " } not found in child entity");
                }
            }
            customAssert.assertTrue(expectedServices.size() == actualServices.size(), "number of expected services on parent entity and child entity are different");
        } catch (Exception e) {
            logger.error("Exception while verify function and services on child entity");
            customAssert.assertTrue(false, e.getMessage());
        }
        customAssert.assertAll();
    }

    public List<String> getValue(String fieldName, JSONObject data) {
        List<String> value = new ArrayList<>();
        JSONArray jsonArray = data.getJSONObject(fieldName).getJSONArray("values");
        for (int i = 0; i < jsonArray.length(); i++) {
            value.add(jsonArray.getJSONObject(i).getString("name").toLowerCase());

        }

        return value;
    }

    private List<String> getActualValue(String value) {

        List<String> allFiledName = new ArrayList<>();
        String[] columnName = value.split(",");
        for (String column : columnName) allFiledName.add(column.toLowerCase().trim());
        return allFiledName;
    }

    @DataProvider
    public Object[][] dataProviderForEntity() {
        List<Object[]> allTestData = new ArrayList<>();
        String[] entitiesArr = {"contracts"};
        for (String entity : entitiesArr) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider ="dataProviderForEntity" ,priority = 1)
    public void testAllManualTask(String entityName) {

        CustomAssert customAssert = new CustomAssert();
        int entityId = 0;
        Integer entityTypeID = 0;
        try {
            logger.info("************Create {}****************", entityName);
            String entityResponse = null;
            switch (entityName) {
                case "governance body":
                    entityResponse = GovernanceBody.createGB("governance_bodies_work_flow_task", true);
                    entityTypeID = ConfigureConstantFields.getEntityIdByName(entityName);
                    break;
                case "disputes":
                    entityResponse = Dispute.createDispute("dispute_work_flow_task", true);
                    entityTypeID = ConfigureConstantFields.getEntityIdByName(entityName);
                    break;
                case "obligations":
                    entityResponse = Obligations.createObligation("obligation_work_flow_task", true);
                    entityTypeID = ConfigureConstantFields.getEntityIdByName(entityName);
                    break;
                case "contracts":
                    entityResponse = Contract.createContract("contract_work_flow_task", true);
                    entityTypeID = ConfigureConstantFields.getEntityIdByName(entityName);
                    break;
                case "actions":
                    entityResponse = Action.createAction("action_work_flow_task", true);
                    entityTypeID = ConfigureConstantFields.getEntityIdByName(entityName);
                    break;
                case "issues":
                    entityResponse = Issue.createIssue("issue_work_flow_task", true);
                    entityTypeID = ConfigureConstantFields.getEntityIdByName(entityName);
                    break;
                case "vendors":
                    entityResponse = VendorHierarchy.createVendorHierarchy("vendor_work_flow_task", true);
                    entityTypeID = ConfigureConstantFields.getEntityIdByName(entityName);
                    break;
                case "suppliers":
                    entityResponse = Supplier.createSupplier("supplier_work_flow_task", true);
                    entityTypeID = ConfigureConstantFields.getEntityIdByName(entityName);
                    break;
                case "change requests":
                    entityResponse = ChangeRequest.createChangeRequest("change_request_work_flow_task", true);
                    entityTypeID = ConfigureConstantFields.getEntityIdByName(entityName);
                    break;
                case "interpretations":
                    entityResponse = Interpretation.createInterpretation("interpretation_work_flow_task", true);
                    entityTypeID = ConfigureConstantFields.getEntityIdByName(entityName);
                    break;
                case "contract draft request":
                    entityResponse = ContractDraftRequest.createCDR("cdr_work_flow_task", true);
                    entityTypeID = ConfigureConstantFields.getEntityIdByName(entityName);
                    break;
                case "contract templates":
                    entityResponse = ContractTemplate.createContractTemplate("contract_template_work_flow_task", true);
                    entityTypeID = ConfigureConstantFields.getEntityIdByName(entityName);
                    break;
                case "service data":
                    entityResponse = ServiceData.createServiceData("", true);
                    entityTypeID = ConfigureConstantFields.getEntityIdByName(entityName);
                    break;
                case "invoices":
                    entityResponse = Invoice.createInvoice("invoice_work_flow_task", true);
                    entityTypeID = ConfigureConstantFields.getEntityIdByName(entityName);
                    break;
                case "service levels":
                    entityResponse = ServiceLevel.createServiceLevel("service_level_work_flow_task", true);
                    entityTypeID = ConfigureConstantFields.getEntityIdByName(entityName);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + entityName);

            }
            if (ParseJsonResponse.validJsonResponse(entityResponse))
                entityId = CreateEntity.getNewEntityId(entityResponse);
            else if (entityId <= 0)
                customAssert.assertTrue(false, "entity not created" + entityName);
            else
                customAssert.assertTrue(false, "Invalid json response for create entity" + entityName);

            logger.info(entityName+"Created with Entity id: " + entityId);
            logger.info("Perform Entity Workflow Action For Created entity");
            EntityWorkflowActionHelper entityWorkflowActionHelper = new EntityWorkflowActionHelper();

            String[] workFlowStep = new String[]{"Send For Peer Review","Peer Review Complete","Send For Internal Review", "Internal Review Complete", "Send For Client Review", "Approve", "Publish"};
            for (String actionLabel : workFlowStep) {
                logger.info(actionLabel);
                entityWorkflowActionHelper.hitWorkflowAction(entityName, entityTypeID, entityId, actionLabel);
                 String JsonStr=entityWorkflowActionHelper.getResponse();
                String status =  JSONUtility.parseJson(JsonStr,"$.header.response.status").toString();
                if(!status.equals("success")){
                    logger.error("Exception while hitting workflow API for Entity {}",entityName);
                    customAssert.assertTrue(false,"Exception while hitting workflow API for Entity"+entityName);
                }
            }
        }
        catch (Exception e)
        {
            logger.error("Exception while verifying work flow task perform successfully{}", e.getMessage());
        }
        finally {
            ShowHelper.deleteEntity(entityName, entityTypeID, entityId);
        }
        customAssert.assertAll();
    }
}
