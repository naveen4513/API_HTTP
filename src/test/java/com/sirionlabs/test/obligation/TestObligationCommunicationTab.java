package com.sirionlabs.test.obligation;


import com.sirionlabs.api.auditLogs.AuditLog;
import com.sirionlabs.api.commonAPI.Comment;
import com.sirionlabs.api.download.DownloadCommunicationDocument;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.DocumentHelper;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.Obligations;
import com.sirionlabs.utils.commonUtils.*;
import net.minidev.json.JSONArray;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;

public class TestObligationCommunicationTab {

    private final static Logger logger = LoggerFactory.getLogger(TestObligationCommunicationTab.class);

    private static String configFilePath;
    private static String configFileName;
    private String AuditLogConfigFilePath;
    private String AuditLogConfigFileName;
    private String filtermap;
    private static String extraFieldsConfigFilePath;
    private static String extraFieldsConfigFileName;
    private static Integer obligationEntityTypeId;
    private static Integer obligationEntityId = -1;
    private static Boolean deleteEntity = true;
    private static Comment comment;
    private static String commentFilePath;
    private static String commentFileName;
    private TabListData tabObj = new TabListData();
    DownloadCommunicationDocument downloadObj = new DownloadCommunicationDocument();
    AuditLog auditlog = new AuditLog();


    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ObligationCreationTestConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("ObligationTestConfigFileName");
        extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
        extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
        obligationEntityTypeId = ConfigureConstantFields.getEntityIdByName("obligations");

        commentFilePath = ConfigureConstantFields.getConstantFieldsProperty("commentConfigFilePath");
        commentFileName = ConfigureConstantFields.getConstantFieldsProperty("commentConfigFileName");

        AuditLogConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestAuditLogConfigFilePath");
        AuditLogConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("TestAuditLogConfigFileName");
        filtermap = ParseConfigFile.getValueFromConfigFile(AuditLogConfigFilePath, AuditLogConfigFileName, "filterjson", String.valueOf(obligationEntityTypeId));

        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity");
        if (temp != null && temp.trim().equalsIgnoreCase("false"))
            deleteEntity = false;
    }

    @Test
    public void TestObligationCommunicationTab() throws InterruptedException {
        String flowToTest = "flow 11";
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating Obligation Creation Flow [{}]", flowToTest);

            //Validate Obligation Creation
            logger.info("Creating Obligation for Flow [{}]", flowToTest);
            String createResponse = Obligations.createObligation(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest,
                    true);

            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                JSONObject jsonObj = new JSONObject(createResponse);
                String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
                logger.info("Create Status for Flow [{}]: {}", flowToTest, createStatus);

                if (createStatus.equalsIgnoreCase("success")) {
                    obligationEntityId = CreateEntity.getNewEntityId(createResponse, "obligations");
                }else{
                    csAssert.assertFalse(true,"obligation create status does is not" + "success"+ createStatus  );
                }
            }
            else{
                csAssert.assertFalse(true,"Obligation create response is not valid json");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Obligation Creation Flow [" + flowToTest + "]. " + e.getMessage());
        }

        logger.info("add log without contract");
        doComment(false,csAssert);
        logger.info("add log with contract");
        doComment(true,csAssert);
        logger.info("download document");
        downloadDocument(obligationEntityTypeId,csAssert);
        logger.info("validate audit ");
        validateAuditLog(csAssert);

        if (deleteEntity && obligationEntityId != -1) {
            logger.info("Deleting Obligation having Id {}", obligationEntityId);
            EntityOperationsHelper.deleteEntityRecord("obligations", obligationEntityId);

            csAssert.assertAll();
        }
    }


     private void doComment(Boolean withDocument, CustomAssert csAssert){

        String sectionName = "obligations";
         ArrayList<String> randomList = new ArrayList<>();
         String commentdocuments = null;

         String requestedby = ParseConfigFile.getValueFromConfigFile(commentFilePath, commentFileName, sectionName, "requestedby");
         String sharewithsupplier = ParseConfigFile.getValueFromConfigFile(commentFilePath, commentFileName, sectionName, "sharewithsupplier");
         String comments = ParseConfigFile.getValueFromConfigFile(commentFilePath, commentFileName, sectionName, "comments");
         String documenttags = ParseConfigFile.getValueFromConfigFile(commentFilePath, commentFileName, sectionName, "documenttags");
         String draft =ParseConfigFile.getValueFromConfigFile(commentFilePath, commentFileName, sectionName, "draft");
         String actualdate =ParseConfigFile.getValueFromConfigFile(commentFilePath, commentFileName, sectionName, "actualdate");
         String privatecommunication =ParseConfigFile.getValueFromConfigFile(commentFilePath, commentFileName, sectionName, "privatecommunication");
         String changerequest =ParseConfigFile.getValueFromConfigFile(commentFilePath, commentFileName, sectionName, "changerequest");
         String workorderrequest =ParseConfigFile.getValueFromConfigFile(commentFilePath, commentFileName, sectionName, "workorderrequest");
         if(withDocument){

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
                commentdocuments =ParseConfigFile.getValueFromConfigFile(commentFilePath, commentFileName, sectionName, "commentdocuments");

            }catch (Exception e){
              csAssert.assertTrue(false,"document is not uploaded in the comment section");
            }
            }
         comment = new Comment();
         String payload = comment.createCommentPayload(obligationEntityTypeId,obligationEntityId,requestedby, sharewithsupplier, comments, documenttags, draft, actualdate, privatecommunication, changerequest, workorderrequest, commentdocuments, randomList );
         comment.hitComment("obligations",payload);
     }



     private  void downloadDocument(int obligationEntityTypeId, CustomAssert csAssert ){

        //Getting Id and fileId from communication tab Api
        String payloadForCommunication = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":100,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
        String communicationTabResponse = tabObj.hitTabListData(65,obligationEntityTypeId,obligationEntityId,payloadForCommunication);
        String document = ((JSONArray)JSONUtility.parseJson(communicationTabResponse,"$.data[*].[*][?(@.columnName=='document')].value")).get(0).toString();
        String id = document.split(":;")[0];
        String fileId = document.split(":;")[document.split(":;").length-1];

        //Download communication tab uploaded document
         if (!downloadObj.hitDownloadCommunicationDocument("src/test/output", "UploadDocumentFile1.txt",Integer.valueOf(id),
                 78, obligationEntityTypeId, Integer.valueOf(fileId))) {
             csAssert.assertTrue(false, "Couldn't download Document File at Location [src/test/output/UploadDocumentFile1.txt]");
         }

    }

    private  void validateAuditLog(CustomAssert csassert) throws InterruptedException {

        String auditlog_response = auditlog.hitAuditLogDataApi(String.valueOf(obligationEntityTypeId),String.valueOf(obligationEntityId),filtermap);
        JSONArray documentColumn = null;
        try {
             documentColumn = (JSONArray) JSONUtility.parseJson(auditlog_response, "$.data[*].[*][?(@.columnName=='document')].value");
        }catch (Exception e){
            System.out.println(e.getStackTrace());
            Thread.sleep(5000);
            documentColumn = (JSONArray) JSONUtility.parseJson(auditlog_response, "$.data[*].[*][?(@.columnName=='document')].value");
        }
        csassert.assertEquals(documentColumn.get(0).toString(),"Yes","Document column in audit log tab is not yes after uploading document");
        csassert.assertEquals(documentColumn.get(1).toString(),"No","Document column in audit log tab is not \"No\" after without uploading document");
        csassert.assertEquals(documentColumn.get(2).toString(),"No","Document column in audit log tab is not \"No\" after without uploading document");

    }



}