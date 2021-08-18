package com.sirionlabs.test.workflowPod;

import com.sirionlabs.api.clientAdmin.workflow.WorkFlowCreate;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.entityCreation.*;
import com.sirionlabs.helper.entityWorkflowAction.EntityWorkflowActionHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TestNegativeCaseManualTask {
    private final static Logger logger = LoggerFactory.getLogger(TestNegativeCaseManualTask.class);
    private String configFilePath;
    private String configFileName;
    private String dataFilePath = "src/test/resources/TestData/WorkFlowTask";
    private String dataFileName = "ContractManualNeg.xlsx";

    @BeforeClass
    public void beforeClass() {
        WorkFlowCreate workFlowCreate=new WorkFlowCreate();
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        workFlowCreate.hitWorkflowCreate(dataFileName+sdf.format(cal.getTime()),String.valueOf(5636),String.valueOf(61),dataFilePath,dataFileName);
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestWorkFlowTaskConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestWorkFlowTaskConfigFileName");
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
            logger.info("Perform Entity Workflow Action For Created Entity");
            EntityWorkflowActionHelper entityWorkflowActionHelper = new EntityWorkflowActionHelper();

            String[] workFlowStep = new String[]{"Publish"};
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
            logger.error("Exception while verifying Work Flow Manual Task Perform successfully{}", e.getMessage());
        }
        finally {
            ShowHelper.deleteEntity(entityName, entityTypeID, entityId);
        }
        customAssert.assertAll();
    }
    
}
