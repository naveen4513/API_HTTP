package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.api.auditLogs.AuditLog;
import com.sirionlabs.api.autoExtraction.ContractShow;
import com.sirionlabs.helper.autoextraction.DocumentShowPageHelper;
import com.sirionlabs.helper.autoextraction.DocumentUploadHelper;
import com.sirionlabs.helper.autoextraction.GetDocumentIdHelper;
import com.sirionlabs.api.autoExtraction.ProjectCreationAPI;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ContractCreationViaAE {
    private final static Logger logger = LoggerFactory.getLogger(ContractCreationViaAE.class);
    static int newlyCreatedProjectId;
    private static String postgresHost;
    private static String postgresPort;
    private static String postgresDbName;
    private static String postgresDbUsername;
    private static String postgresDbPassword;
    static String configAutoExtractionFilePath;
    static String configAutoExtractionFileName;
    static int docId, initialCountOfDNO, initialContractCount, contractId,finalCountOfDNO;
    private String filterMap;
    private AuditLog auditlog;
    private String AuditLogConfigFilePath;
    private String AuditLogConfigFileName;
    static int finalCountOfContracts;
    private static JSONObject jsonObject;
    Map<Integer, String> mappedDynamicFieldsOfAE;
    List<String> mappedDynamicFields;
    public static String newlyCreatedContractShowResponseStr;
    static int fieldId;
    static String fieldName;
    private int contractsEntityTypeId;


    @BeforeClass
    public void beforeClass() {
        configAutoExtractionFilePath = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFilePath");
        configAutoExtractionFileName = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFileName");
        postgresHost = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "postres sirion db details", "host");
        postgresPort = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "postres sirion db details", "port");
        postgresDbName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "postres sirion db details", "dbname");
        postgresDbUsername = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "postres sirion db details", "username");
        postgresDbPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "postres sirion db details", "password");
        auditlog = new AuditLog();
        AuditLogConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionTestAuditLogConfigFilePath");
        AuditLogConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionTestAuditLogConfigFileName");
        filterMap = ParseConfigFile.getValueFromConfigFile(AuditLogConfigFilePath, AuditLogConfigFileName, "filterjson", "61");
        contractsEntityTypeId = ConfigureConstantFields.getEntityIdByName("contracts");
    }

    @BeforeClass
    public void createProjectAPI() {
        CustomAssert csAssert = new CustomAssert();
        try {
            logger.info("Creating a new Project");
            APIResponse projectCreationResponse = ProjectCreationAPI.projectCreateAPIResponse(ProjectCreationAPI.getAPIPath(), ProjectCreationAPI.getHeaders(), ProjectCreationAPI.getPayload());
            Integer APIResponseCode = projectCreationResponse.getResponseCode();
            csAssert.assertTrue(APIResponseCode == 200, "Response Code is Invalid");
            String projectCreationStr = projectCreationResponse.getResponseBody();
            JSONObject projectCreationJson = new JSONObject(projectCreationStr);
            csAssert.assertTrue(projectCreationJson.get("success").toString().equals("true"), "Project is not created successfully");
            newlyCreatedProjectId = Integer.valueOf(projectCreationJson.getJSONObject("response").get("id").toString());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Project Creation API is not working" + e.getMessage());
        }
    }

    public static JSONObject contractListingAPI() {
        CustomAssert csAsset = new CustomAssert();
        logger.info("Hitting Contract Listing API to get the count of documents");
        ListRendererListData listDataObj = new ListRendererListData();
        listDataObj.hitListRendererListData(2);
        String listRendererJsonStr = listDataObj.getListDataJsonStr();
        JSONObject listDataJson = new JSONObject(listRendererJsonStr);
        return listDataJson;
    }
    public static JSONObject contractShowResponse() throws IOException {
        String apiPath = ContractShow.getAPIPath();
        apiPath = String.format(apiPath + contractId);
        HttpGet httpGet = new HttpGet(apiPath);
        httpGet.addHeader("Content-Type", "application/json;charset=UTF-8");
        httpGet.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
        HttpResponse newlyCreatedContractShowResponse = APIUtils.getRequest(httpGet);
        newlyCreatedContractShowResponseStr = EntityUtils.toString(newlyCreatedContractShowResponse.getEntity());
        jsonObject = new JSONObject(newlyCreatedContractShowResponseStr);
        return jsonObject;
    }

    @Test(priority = 0)
    public void uploadDocumentInAEListing() throws IOException, InterruptedException {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Uploading a new document in AE listing");
        APIResponse documentUploadResponse = DocumentUploadHelper.documentUploadAPIResponse(DocumentUploadHelper.getAPIPath(), DocumentUploadHelper.getHeaders(), DocumentUploadHelper.getGlobalUploadAPIPayload(newlyCreatedProjectId));
        Integer APIResponseCode = documentUploadResponse.getResponseCode();
        csAssert.assertTrue(APIResponseCode == 200, "Response Code is Invalid");
        logger.info("Checking whether Extraction Status is complete or not");
        boolean isExtractionCompleted = AutoExtractionHelper.getExtractionStatus(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
        csAssert.assertTrue(isExtractionCompleted, "Document is not getting completed");
    }

    @Test(dependsOnMethods = "uploadDocumentInAEListing", priority = 1,enabled = true)
    public void checkNewlyCreatedContractInListing() {
        CustomAssert csAssert = new CustomAssert();
        boolean p = false;
        try {
            docId = GetDocumentIdHelper.getDocIdOfLatestDocument();
            //Now Running the Query to update the supplier in Auto-extraction Document Request Table
            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(postgresHost, postgresPort, postgresDbName, postgresDbUsername, postgresDbPassword);
            String query = "update autoextraction_document_request set ae_supplier_ref_id ='Baxter Healthcare S.A.' where client_id=1007 and id =" + docId + "";
            logger.info("ADR update ae_supplier_id query formed is [{}]", query);
            try {
                boolean queryExecuted = postgreSQLJDBC.updateDBEntry(query);
            } catch (Exception e) {
                csAssert.assertTrue(false, "Exception occurred in updating ae supplier Id in DB");
            }
            //Check the count of Contracts in AE listing
            try {
                JSONObject listDataJson = ContractCreationViaAE.contractListingAPI();
                initialContractCount = (int) listDataJson.get("filteredCount");
                //Check initial count in DNO Listing
                JSONObject dnoListDataObj = AEPorting.ObligationListing();
                initialCountOfDNO = (int) dnoListDataObj.get("filteredCount");
                //Now Reset the document and wait for completion
                HttpResponse resetDocumentResponse = AutoExtractionHelper.documentResetAPI(docId);
                csAssert.assertTrue(resetDocumentResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
                logger.info("Wait for Document Completion");
                boolean isExtractionCompleted = AutoExtractionHelper.getExtractionStatus(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
                csAssert.assertTrue(isExtractionCompleted, "Document is not getting completed");
                JSONObject listObj = ContractCreationViaAE.contractListingAPI();
                finalCountOfContracts = (int) listObj.get("filteredCount");
                /*TC Id: C154076 Verify that a contract should get created successfully if Draft Flag is True*/
                csAssert.assertEquals(finalCountOfContracts, initialContractCount + 1, "Contract has not been created ");

                //Now Check the ID/Title of Newly Created Contract Via AE
                /*TC: C154078:Verify that the Title of newly created contract should be in format: AEID_documentId*/
                int columnIdForContractTitle = ListDataHelper.getColumnIdFromColumnName(String.valueOf(listObj), "documenttitle");
                String contractTitleDetails = String.valueOf(listObj.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnIdForContractTitle)).get("value"));
                String[] contractDetails = contractTitleDetails.split(":;");
                String contractTitle = contractDetails[0];
                contractId = Integer.parseInt(contractDetails[1]);
                //contractId=135166;
                String[] documentName = contractTitle.split("_");
                int documentId = Integer.parseInt(documentName[1]);
                csAssert.assertEquals(documentId, docId, "Document Id are Different for the newly created contract: " + "Document Id of AE listing Doc " + docId +
                        " and Document Id of Contract " + documentId);
            } catch (Exception e) {
                csAssert.assertTrue(false, "Contract List Data API is not working " + e.getMessage());

            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "AE Listing API is not working because of:" + e.getMessage());
        }
        csAssert.assertAll();

    }

    @Test(dependsOnMethods = "uploadDocumentInAEListing", priority = 2,enabled = true)
    public void validateMetadataOfContract() {
        CustomAssert csAssert = new CustomAssert();
        try {
            /* Test Cases Automated :
             * C154079: Porting should be enabled for that client along with mapping of dynamic fields with AE fields
             * C154080: Porting should be enabled for that client along with mapping of dynamic fields with AE fields
             * C154081: Verify that the value of field: Porting status should get updated as Yes after successful porting */
            logger.info("Test Case of metadata porting is working fine in Contracts");
            logger.info("Waiting for porting to complete");
            Thread.sleep(5000);
            jsonObject = contractShowResponse();
            mappedDynamicFieldsOfAE = new HashMap<>();
            JSONArray fieldsJsonArray = jsonObject.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent").getJSONArray("fields")
                    .getJSONObject(0).getJSONArray("fields").getJSONObject(0).getJSONArray("fields");
            int totalFields = fieldsJsonArray.length();
            for (int i = 0; i < totalFields; i++) {
                fieldName = (String) fieldsJsonArray.getJSONObject(i).get("label");
                if (fieldName.equalsIgnoreCase("COPO1") || fieldName.equalsIgnoreCase("COPO2") || fieldName.equalsIgnoreCase("Contract Porting Status")
                        || fieldName.equalsIgnoreCase("Clause Text")) {
                    fieldId = (int) fieldsJsonArray.getJSONObject(i).get("id");
                    mappedDynamicFieldsOfAE.put(fieldId, fieldName);
                }

            }
            try {
                List<String> metadataValue = new LinkedList<>();
                mappedDynamicFields = new LinkedList<>();
                logger.info("Checking the value against the mapped dynamic fields");
                JSONObject dynamicMetadataJson = jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata");
                for (Map.Entry<Integer, String> entry : mappedDynamicFieldsOfAE.entrySet()) {
                    mappedDynamicFields.add(entry.getValue());
                    fieldName = entry.getValue();
                    fieldId = entry.getKey();
                    String dynamicFieldSystemName = "dyn" + fieldId;
                    metadataValue.add((String) dynamicMetadataJson.getJSONObject(dynamicFieldSystemName).get("values"));
                    logger.info("Metadata Value for fieldName " + fieldName + " and fieldId: " + fieldId + "is: " + metadataValue);
                }
                csAssert.assertEquals(metadataValue.size(), 4, "Metadata has not been ported in all four mapped fields");
            } catch (Exception e) {
                logger.info("Exception occured while hitting contract show API");
                csAssert.assertTrue(false, e.getMessage());
            }
        }
        catch (Exception e)
        {
            logger.info("Exception occured while hitting Contract show API");
            csAssert.assertTrue(false, e.getMessage());
        }
        csAssert.assertAll();
    }

    @Test(dependsOnMethods = "uploadDocumentInAEListing", priority = 3,enabled = true)
    public void checkDNOInListing()
    {
        CustomAssert csAssert = new CustomAssert();
        /*TC Id: C154082: Verify that DNO porting should be working fine after successful contract creation via AE */
        try {
            logger.info("Check DNO Created after successful contract porting");
            Thread.sleep(9000);
            JSONObject dnoListDataObj = AEPorting.ObligationListing();
            finalCountOfDNO = (int) dnoListDataObj.get("filteredCount");
            csAssert.assertTrue(finalCountOfDNO > initialCountOfDNO, "DNO Porting is not working after successful contract creation");
        }
        catch (Exception e)
        {
            csAssert.assertTrue(false,"List Data API of DNO is not working" + e.getMessage());
        }
        csAssert.assertAll();

    }
    @Test(dependsOnMethods = "uploadDocumentInAEListing", priority = 4,enabled = true)
    public void documentResetTestCase()
    {
        CustomAssert csAssert = new CustomAssert();
        /*TC:C154084: Test Case to verify after document reset a new Contract should not get created*/
        /* TC:C154086: Verify if a user reset the document then metadata should get updated in contract */
        /*TC: C154087 : Verify that the value in metadata field should get updated if a user edit/insert a clause and reset the document */


        try {
            logger.info("Perform Metadata and clause Create Operation and check contract is updated or not");
            DocumentShowPageHelper crudHelper = new DocumentShowPageHelper(String.valueOf(docId));
            logger.info("Getting field Id for Performing create operation");
            logger.info("Getting Category Id to edit from metadata tab");
            String categoryId = crudHelper.getClauseIdLinkedToMetadata();
            logger.info("Getting Text Id from metadata tab");
            String textId = crudHelper.getMetadataTextId();
            String fieldId = crudHelper.getMetadataId();
            try {
                logger.info("Now Performing Create Operation for Metadata");
                HttpResponse metadataCreateResponse = AutoExtractionHelper.metadataCreateOperation(textId, categoryId, docId, fieldId);
                csAssert.assertTrue(metadataCreateResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
                String metadataResponseStr = EntityUtils.toString(metadataCreateResponse.getEntity());
                JSONObject metadataJson = new JSONObject(metadataResponseStr);
                String createResponseMessage = (String) metadataJson.getJSONObject("response").get("message");
                csAssert.assertEquals(createResponseMessage, "Action completed successfully");

                logger.info("Getting Clause Text to update a clause");
                String clauseText = crudHelper.getCategoryText();
                logger.info("Getting text Id to update a clause");
                String updatedTextId = crudHelper.getCategoryTextId();
                logger.info("Fetching Page Number to perform update operation on clause");
                String clausePageNo = crudHelper.getCategoryPageNo();
                try {
                    logger.info("Performing Update Operation on a Clause");
                    logger.info("Fetching the Initial Count of Clauses in General Category");
                    HttpResponse clauseUpdateOperationResponse = AutoExtractionHelper.clauseUpdateOperation(clauseText, textId, clausePageNo, docId);
                    csAssert.assertTrue(clauseUpdateOperationResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
                    String clauseUpdateStr = EntityUtils.toString(clauseUpdateOperationResponse.getEntity());
                    JSONObject clauseUpdateJson = new JSONObject(clauseUpdateStr);
                    String responseOfClauseUpdateOperation = (String) clauseUpdateJson.get("response");
                    csAssert.assertEquals(responseOfClauseUpdateOperation, "Success", "Clause Update Operation Response is:");
                    try {
                        logger.info("Reset the document" + docId + " in AE listing");
                        HttpResponse resetAPIResponse = AutoExtractionHelper.documentResetAPI(docId);
                        csAssert.assertTrue(resetAPIResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid");
                        logger.info("Wait for Document Completion");
                        boolean isExtractionCompleted = AutoExtractionHelper.getExtractionStatus(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
                        csAssert.assertTrue(isExtractionCompleted, "Document is not getting completed");
                        JSONObject listObj = ContractCreationViaAE.contractListingAPI();
                        int contractListCountAfterReset = (int) listObj.get("filteredCount");
                        csAssert.assertTrue(contractListCountAfterReset == finalCountOfContracts, "Mismatch in count of contracts after document Reset :" +
                                " Count after document Reset " + contractListCountAfterReset + "Final Count in Contracts Listing before Reset : " + finalCountOfContracts);

                        //Check in Audit log if metadata is updated or not
                        logger.info("Checking Audit Log of Contract After successful porting of metadata for Contract Id: " + contractId);
                        String auditLog_response = auditlog.hitAuditLogDataApi(String.valueOf(61), String.valueOf(contractId), filterMap);
                        JSONObject auditLogJson = new JSONObject(auditLog_response);
                        int actionColumnId = ListDataHelper.getColumnIdFromColumnName(auditLog_response, "action_name");
                        String actionValue = auditLogJson.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(actionColumnId)).getString("value");
                        logger.info("Action name for contractId " + contractId + "is: " + actionValue);
                        csAssert.assertEquals(actionValue, "Auto Update", "Showing an incorrect Action name after Contract Porting: " + actionValue);

                        try {
                            logger.info("Verify that View History section shows the ported metadata in mapped dynamic field");
                            int historyColumnId = ListDataHelper.getColumnIdFromColumnName(auditLog_response, "history");
                            String historyURL = auditLogJson.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(historyColumnId)).getString("value");
                            HttpResponse viewHistoryAPIResponse = AutoExtractionHelper.viewHistoryAPI(historyURL);
                            csAssert.assertTrue(viewHistoryAPIResponse.getStatusLine().getStatusCode() == 200, "Response Code is Invalid for view History API");
                            String viewHistoryStr = EntityUtils.toString(viewHistoryAPIResponse.getEntity());
                            JSONObject viewHistoryJson = new JSONObject(viewHistoryStr);
                            int totalModifiedData = viewHistoryJson.getJSONArray("value").length();
                            csAssert.assertEquals(totalModifiedData, 1, "Metadata is not getting updated in contract after doc reset");

                            //Now Check DNO listing After Doc Reset
                            /* TC: C154085 : Verify that if a user reset the same document in AE listing then it should not re-create the DNO*/
                            try {
                                Thread.sleep(7000);
                                JSONObject dnoListDataObj = AEPorting.ObligationListing();
                                int dnoCountAfterDocReset = (int) dnoListDataObj.get("filteredCount");
                                //csAssert.assertEquals(dnoCountAfterDocReset, finalCountOfDNO, "Mismatch in DNO listing after document Reset");

                            } catch (Exception e) {
                                csAssert.assertTrue(false, "Exception occured while hitting DNO listData API :" + e.getMessage());
                            }
                        } catch (Exception e) {
                            csAssert.assertTrue(false, "Exception occured while View History API :" + e.getMessage());

                        }
                    } catch (Exception e) {
                        csAssert.assertTrue(false, "Exception occured while hitting Document Reset API :" + e.getMessage());

                    }
                } catch (Exception e) {
                    csAssert.assertTrue(false, "Exception occured while hitting Metadata Create API:" + e.getMessage());
                }
            }
            catch (Exception e)
            {
                csAssert.assertTrue(false,"Exception occured while hitting clause update API");
            }

        }
        catch (Exception e)
        {
            csAssert.assertTrue(false,"Exception occured while hitting Metadata Tab API:" + e.getMessage());

        }

        csAssert.assertAll();

    }

    @Test(dependsOnMethods = "uploadDocumentInAEListing", priority = 5,enabled = true)
    public void checkCommentAfterContractCreation()
    {
        CustomAssert csAssert = new CustomAssert();
        String comment = null;
        try{
            TabListData tabListObj = new TabListData();
            String tabListDataResponse = tabListObj.hitTabListData(65, contractsEntityTypeId, contractId);
            JSONObject tabListDataJson = new JSONObject(tabListDataResponse);
            if (ParseJsonResponse.validJsonResponse(tabListDataResponse))
            {
                int columnIdOfComment = ListDataHelper.getColumnIdFromColumnName(tabListDataResponse,"comment");
                String completeComment = tabListDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnIdOfComment)).get("value").toString().trim();
                String[] subPartOfString = completeComment.split(":")[1].split("\\|\\|");
                comment = subPartOfString[0].trim();
            }
            csAssert.assertTrue(comment.contains("COPO1"),"Message is Incorrect in Comment section " + comment);
        }
        catch (Exception e)
        {
            csAssert.assertTrue(false,"Exception occured while hitting comment section API for entity Type Id : " +contractsEntityTypeId + " and Entity Id: " +contractId) ;
        }

        csAssert.assertAll();
    }

    /* TC Id: C154499: Verify that document uploaded in AE listing should get attached in contract Tree after successful contract creation*/
    @Test(dependsOnMethods = "uploadDocumentInAEListing", priority = 6)
    public void contractDocument()
    {
        CustomAssert csAssert = new CustomAssert();
        try{
            logger.info("Check if Document uploaded in AE listing has been tagged in Contract Tree");
            HttpResponse contractTreeResponse = AutoExtractionHelper.verifyDocumentOnTree(contractId,61,1901,"{}");
            csAssert.assertTrue(contractTreeResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid for Contract Tree API");
            String contractTreeStr = EntityUtils.toString(contractTreeResponse.getEntity());
            String attachedDocument = null;
            String expectedDoc = "KAMADALTD_DRSADraftR_3252013";
            if(ParseJsonResponse.validJsonResponse(contractTreeStr))
            {
                JSONObject contractTreeJson = new JSONObject(contractTreeStr);
                int totalChild = contractTreeJson.getJSONObject("body").getJSONObject("data").getJSONArray("children").length();
                if(totalChild!=0)
                {
                    attachedDocument = (String) contractTreeJson.getJSONObject("body").getJSONObject("data").getJSONArray("children").getJSONObject(0).get("text");
                }
                else
                {
                    throw new SkipException("Couldn't Get All Children Map from Contract Tree V1 Response.");
                }

            }
            else {
                csAssert.assertTrue(false, "Contract Tree API V1 Response is an Invalid JSON for Supplier Id ");
            }
            csAssert.assertEquals(attachedDocument,expectedDoc,"Same Document is not attached in doc tree of Contract");

        }
        catch (Exception e)
        {
            csAssert.assertTrue(false, "Contract Tree API V1 Response is not working: " + e.getMessage());

        }
        finally {
            //Delete Contract
            logger.info("Deleting Newly Created Contract Entity");
            EntityOperationsHelper.deleteEntityRecord("contracts", contractId);
        }
        csAssert.assertAll();
    }

}
