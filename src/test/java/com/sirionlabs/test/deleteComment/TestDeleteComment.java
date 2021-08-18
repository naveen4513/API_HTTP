package com.sirionlabs.test.deleteComment;
import com.sirionlabs.api.commonAPI.Comment;
import com.sirionlabs.api.deleteComment.CommentAttachmentDelete;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.DocumentHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.entityCreation.*;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.RandomString;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import java.util.ArrayList;
import java.util.List;

@Listeners(value = MyTestListenerAdapter.class)
public class TestDeleteComment {
    private final static Logger logger = LoggerFactory.getLogger(TestDeleteComment.class);
    @DataProvider()//parallel = true)
    public Object[][] dataProviderForDeleteComment() {
        List<Object[]> allTestData = new ArrayList<>();

        String[] entitiesArr = {"service levels","contract draft request","change requests","interpretations","contracts","invoices","issues","disputes", "actions","governance body", "obligations"
                ,"contract templates"};

        for (String entity : entitiesArr) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForDeleteComment")
    public void testCommentDelete(String entityName) {
        CustomAssert customAssert=new CustomAssert();
        int entityId=0;
        String auditLogId=null;
        Integer entityTypeID=0;
        try {
            logger.info("************Create {}****************", entityName);
            String entityResponse = null;
           ;
            switch (entityName) {
                case "governance body":
                    entityResponse = GovernanceBody.createGB("governance_bodies_delete_comment", true);
                    entityTypeID= ConfigureConstantFields.getEntityIdByName(entityName);
                    break;
                case "disputes":
                    entityResponse = Dispute.createDispute("dispute_delete_comment", true);
                    entityTypeID= ConfigureConstantFields.getEntityIdByName(entityName);
                    break;
                case "obligations":
                    entityResponse = Obligations.createObligation("obligation_delete_comment", true);
                    entityTypeID= ConfigureConstantFields.getEntityIdByName(entityName);
                    break;
                case "contracts":
                    entityResponse = Contract.createContract("contract_delete_comment", true);
                    entityTypeID= ConfigureConstantFields.getEntityIdByName(entityName);
                    break;
                case "actions":
                    entityResponse = Action.createAction("action_delete_comment", true);
                    entityTypeID= ConfigureConstantFields.getEntityIdByName(entityName);
                    break;
                case "issues":
                    entityResponse = Issue.createIssue("issue_delete_comment", true);
                    entityTypeID= ConfigureConstantFields.getEntityIdByName(entityName);
                    break;
                case "vendors":
                    entityResponse = VendorHierarchy.createVendorHierarchy("vendor_delete_comment", true);
                    entityTypeID= ConfigureConstantFields.getEntityIdByName(entityName);
                    break;
                case "suppliers":
                    entityResponse = Supplier.createSupplier("supplier_delete_comment", true);
                    entityTypeID= ConfigureConstantFields.getEntityIdByName(entityName);
                    break;
                case "change requests":
                    entityResponse = ChangeRequest.createChangeRequest("change_request_delete_comment", true);
                    entityTypeID= ConfigureConstantFields.getEntityIdByName(entityName);
                    break;
                case "interpretations":
                    entityResponse = Interpretation.createInterpretation("interpretation_delete_comment", true);
                    entityTypeID= ConfigureConstantFields.getEntityIdByName(entityName);
                    break;
                case "contract draft request":
                    entityResponse = ContractDraftRequest.createCDR("cdr_delete_comment", true);
                    entityTypeID= ConfigureConstantFields.getEntityIdByName(entityName);
                    break;
                case "contract templates":
                    entityResponse = ContractTemplate.createContractTemplate("contract_template_delete_comment", true);
                    entityTypeID= ConfigureConstantFields.getEntityIdByName(entityName);
                    break;
                case "service data":
                    entityResponse = ServiceData.createServiceData("", true);
                    entityTypeID= ConfigureConstantFields.getEntityIdByName(entityName);
                    break;
                case "invoices":
                    entityResponse = Invoice.createInvoice("invoice_delete_comment", true);
                    entityTypeID= ConfigureConstantFields.getEntityIdByName(entityName);
                    break;
                case "service levels":
                    entityResponse = ServiceLevel.createServiceLevel("service_level_delete_comment", true);
                    entityTypeID= ConfigureConstantFields.getEntityIdByName(entityName);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + entityName);

            }
            if (ParseJsonResponse.validJsonResponse(entityResponse))
                entityId = CreateEntity.getNewEntityId(entityResponse);
            else if(entityId<=0)
                customAssert.assertTrue(false,"entity not created"+entityName);
            else
                customAssert.assertTrue(false,"Invalid json response for create entity"+entityName);

            upload_document(entityName,entityTypeID,entityId);

            logger.info("created entity id {}",entityId);
            TabListData tabListData = new TabListData();
            tabListData.hitTabListData(61, entityTypeID, entityId);
            String tabListDataResponseStrResponse = tabListData.getTabListDataResponseStr();
            int length = new JSONObject(tabListDataResponseStrResponse).getJSONArray("data").length();
            JSONObject jsonObject = new JSONObject(tabListDataResponseStrResponse).getJSONArray("data").getJSONObject(length-1);
            JSONArray jsonObjectName = jsonObject.names();
            for (int i = 0; i < jsonObjectName.length(); i++) {
                if (jsonObject.getJSONObject(jsonObjectName.getString(i)).getString("columnName").equalsIgnoreCase("history")) {
                    String[] value = jsonObject.getJSONObject(jsonObjectName.getString(i)).getString("value").split("/");
                    auditLogId = value[3];
                    break;
                }
            }
            APIResponse apiResponse = new CommentAttachmentDelete().getResponse(String.valueOf(entityTypeID),String.valueOf(entityId), auditLogId);
            String deleteCommentAPIResponse=apiResponse.getResponseBody();
            customAssert.assertTrue(deleteCommentAPIResponse.contains("Comment/Attachment deleted successfully"),"Comment/Attachment not deleted successfully");
        } catch (Exception e) {
            logger.error("Exception while verifying Comment/Attachment deleted successfully{}", e.getMessage());
        }
        finally {
            ShowHelper.deleteEntity(entityName, entityTypeID, entityId);
        }
        customAssert.assertAll();
    }

    private  void upload_document(String entityName,int entityTypeId, int entityId){
        ArrayList<String> randomList = new ArrayList<>();
        try {
            logger.info("*********************** upload document file in comment ***********************");
            String commentFilePath = ConfigureConstantFields.getConstantFieldsProperty("commentConfigFilePath");
            String commentFileName = ConfigureConstantFields.getConstantFieldsProperty("commentConfigFileName");

            String filePath = "src/test/resources/TestConfig/GovernanceBody/Creation";
            String fileName = "UploadDocumentFile1.txt";
            String randomKeyForDocumentFile = RandomString.getRandomAlphaNumericString(18);
            String uploadResponse = DocumentHelper.uploadDocumentFile(filePath, fileName, randomKeyForDocumentFile);

            if (!(uploadResponse != null && uploadResponse.trim().startsWith("/data/") && uploadResponse.trim().contains(fileName))) {
                throw new SkipException("Couldn't upload Document File located at [" + filePath + "/" + fileName + "]. Hence skipping test.");
            }
            randomList.add(randomKeyForDocumentFile);

            String requestedby = ParseConfigFile.getValueFromConfigFile(commentFilePath, commentFileName, "governance_bodies_delete_comment", "requestedby");
            String sharewithsupplier = ParseConfigFile.getValueFromConfigFile(commentFilePath, commentFileName, "governance_bodies_delete_comment", "sharewithsupplier");
            String comments = ParseConfigFile.getValueFromConfigFile(commentFilePath, commentFileName, "governance_bodies_delete_comment", "comments");
            String documenttags = ParseConfigFile.getValueFromConfigFile(commentFilePath, commentFileName, "governance_bodies_delete_comment", "documenttags");
            String draft =ParseConfigFile.getValueFromConfigFile(commentFilePath, commentFileName, "governance_bodies_delete_comment", "draft");
            String actualdate =ParseConfigFile.getValueFromConfigFile(commentFilePath, commentFileName, "governance_bodies_delete_comment", "actualdate");
            String privatecommunication =ParseConfigFile.getValueFromConfigFile(commentFilePath, commentFileName, "governance_bodies_delete_comment", "privatecommunication");
            String changerequest =ParseConfigFile.getValueFromConfigFile(commentFilePath, commentFileName, "governance_bodies_delete_comment", "changerequest");
            String workorderrequest =ParseConfigFile.getValueFromConfigFile(commentFilePath, commentFileName, "governance_bodies_delete_comment", "workorderrequest");
            String commentdocuments =ParseConfigFile.getValueFromConfigFile(commentFilePath, commentFileName, "governance_bodies_delete_comment", "commentdocuments");


            Comment comment = new Comment();
            String payload = comment.createCommentPayload(entityTypeId,entityId,requestedby, sharewithsupplier, comments, documenttags, draft, actualdate, privatecommunication, changerequest, workorderrequest, commentdocuments, randomList );
            comment.hitComment(entityName,payload);

        }catch (Exception e){

        }
    }

}


