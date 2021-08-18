package com.sirionlabs.test.cdr;

import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.New;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.moveToTree.GetCRIdsForContract;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.entityCreation.ChangeRequest;
import com.sirionlabs.helper.entityCreation.Contract;
import com.sirionlabs.helper.entityCreation.ContractDraftRequest;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.preSignature.PreSignatureHelper;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.test.filters.TestFilters;
import com.sirionlabs.test.workflowPod.TestMultiSupplierContract;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Chayanika Ghosh
 */
@Listeners(value = MyTestListenerAdapter.class)
public class TestMoveToTreeCDR extends TestRailBase{

    private final static Logger logger = LoggerFactory.getLogger(TestFilters.class);

    private String filePath = "src/test/resources/Helper/EntityCreation/Contract Draft Request";
    private String extraFieldsFileName = "cdrExtraFields.cfg";
    private String configFileName = "cdr.cfg";
    private TabListData tabListObj = new TabListData();
    private int docId = -1;
    ContractDraftRequest cdrObj = new ContractDraftRequest();
    String entityResponse = null;
    Integer entityTypeID=0;
    PreSignatureHelper preSignatureHelperObj = new PreSignatureHelper();

    @Test
    public void testMoveToTreeDocumentCDR() {
        CustomAssert csAssert = new CustomAssert();
        Integer contractId = -1;
        Integer cdrId = -1;

        String  ContractDraftRequestResponse = ContractDraftRequest.createCDR("presignature flow", false);

        try {
            cdrId = PreSignatureHelper.getNewlyCreatedId(ContractDraftRequestResponse);
            logger.info("cdrId id is: "+ cdrId);
            if (cdrId == null|| cdrId ==-1) {
                logger.info("Couldn't create Contract.");
                throw new SkipException("Couldn't create Contract.");
            }

            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
            File fileToUpload = new File(System.getProperty("user.dir") + "\\src\\test\\resources\\TestConfig\\PreSignature\\Files\\UploadFiles\\ContractDocumentUpload.docx");
            // upload file
            String fileUploadDraftResponse = PreSignatureHelper.fileUploadDraftWithNewDocument("ContractDocumentUpload","docx",randomKeyForFileUpload,"160",String.valueOf(contractId),fileToUpload);


            ShowHelper showHelper = new ShowHelper();
            String showResponse = showHelper.getShowResponseVersion2(160, cdrId);
            JSONObject jsonObject = new JSONObject(showResponse);
            jsonObject = jsonObject.getJSONObject("body").getJSONObject("data");
            String commentsPayload = "{\"requestedBy\":{\"name\":\"requestedBy\",\"id\":12244,\"options\":null," +
                    "\"multiEntitySupport\":false,\"values\":null},\"shareWithSupplier\":{\"name\":\"shareWithSupplier\",\"id\":12409," +
                    "\"multiEntitySupport\":false,\"values\":false},\"comments\":{\"name\":\"comments\",\"id\":86,\"multiEntitySupport\":false,\"values\":null}," +
                    "\"documentTags\":{\"name\":\"documentTags\",\"id\":12428,\"options\":null,\"multiEntitySupport\":false,\"values\":null}," +
                    "\"draft\":{\"name\":\"draft\",\"multiEntitySupport\":false,\"values\":true},\"actualDate\":{\"name\":\"actualDate\",\"id\":12243," +
                    "\"multiEntitySupport\":false,\"values\":null},\"privateCommunication\":{\"name\":\"privateCommunication\",\"id\":12242," +
                    "\"multiEntitySupport\":false,\"values\":null},\"changeRequest\":{\"name\":\"changeRequest\",\"id\":12246,\"options\":null," +
                    "\"multiEntitySupport\":false,\"values\":null},\"workOrderRequest\":{\"name\":\"workOrderRequest\",\"id\":12247,\"multiEntitySupport\":false," +
                    "\"values\":null},\"commentDocuments\":{\"values\":[{\"templateTypeId\":1001,\"documentFileId\":null,\"documentSize\":50728," +
                    "\"key\":\""+randomKeyForFileUpload+"\",\"documentStatusId\":2,\"permissions\":{\"financial\":false,\"legal\":false,\"businessCase\":false}," +
                    "\"performanceData\":false,\"searchable\":false,\"shareWithSupplierFlag\":false}]}}";

            jsonObject.put("comment", new JSONObject(commentsPayload));

            String submitDraftPayload ="{\"body\":{\"data\":"+ jsonObject.toString() +"}}";
            HttpResponse contractDraftRequestResponse = PreSignatureHelper.getContractDraftRequestEditPageResponse(contractId);
            JSONObject contractDraftRequestJson = PreSignatureHelper.getJsonObjectForResponse(contractDraftRequestResponse);

            // submit file
            HttpResponse submitFileDraftResponse =  PreSignatureHelper.submitFileDraft(submitDraftPayload);
            String getStatus = EntityUtils.toString(submitFileDraftResponse.getEntity());
            JSONObject jobj = new JSONObject(getStatus);
            String newStatus = jobj.getJSONObject("header").getJSONObject("response").get("status").toString();
            String expectedStatus = "success";
            if(expectedStatus.equals(newStatus)){
                logger.info("File upload successfully!");
            }else{
                csAssert.assertTrue(false, "File upload failed!");

            }
            String sectionName = "pre signature cdr to contract";

            Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(filePath, configFileName, sectionName);

            //Validate Contract Creation
            logger.info("Creating Contract for Flow [{}]", "Pre signature CDR to Contract");
            String createResponse;

            if (flowProperties.get("sourceentity").trim().equalsIgnoreCase("contract draft request")) {
                String[] parentSupplierIdsArr = flowProperties.get("supplierids").split(",");
                String payload = "{\"documentTypeId\":4,\"parentEntity\":{\"entityIds\":" + Arrays.toString(parentSupplierIdsArr) +
                        ",\"entityTypeId\":1},\"sourceEntity\":{\"entityIds\":[" + flowProperties.get("sourceid") + "],\"entityTypeId\":160}," +
                        "\"actualParentEntity\":{\"entityIds\":" + Arrays.toString(parentSupplierIdsArr) + ",\"entityTypeId\":1}}";
                createResponse = createContractFromCDRResponse(payload, sectionName);
            } else {
                createResponse = Contract.createContract(filePath, configFileName, filePath, configFileName, sectionName,
                        true);
            }

            if (createResponse == null) {
                csAssert.assertTrue(false, "Create Response is null.");
            }
            jobj = new JSONObject(createResponse);
            contractId = (Integer) jobj.getJSONObject("header").getJSONObject("response").get("entityId");


            String documentName = null;
            String documentStatus = null;
            String documentId = null;
            String contractDocumentTemplateTypeId = null;
            // Validate Contract Template in Contract Document tab of CDR
            HttpResponse defaultUserListMetaDataResponse = PreSignatureHelper.defaultUserListMetaDataAPI("367", 160, "{}");
            if(defaultUserListMetaDataResponse.getStatusLine().getStatusCode() == 200){
                csAssert.assertTrue(false, "Default User List Meta Data API Response Code is not valid");
            }
            JSONObject defaultUserListMetaDataJson = PreSignatureHelper.getJsonObjectForResponse(defaultUserListMetaDataResponse);
            List<Integer> columnIds = PreSignatureHelper.getDefaultColumns(defaultUserListMetaDataJson.getJSONArray("columns"));
            defaultUserListMetaDataJson.getJSONArray("columns");
            String tabListDataPayload = "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
            HttpResponse tabListDataResponse = PreSignatureHelper.tabListDataAPI("367", "160", cdrId, tabListDataPayload);
            if(tabListDataResponse.getStatusLine().getStatusCode() == 200){
                csAssert.assertTrue(false, "Tab List Data API Response is not valid as expected status was 200 and actual is: "+tabListDataResponse.getStatusLine().getStatusCode());
            }
            JSONObject tabListDataJson = PreSignatureHelper.getJsonObjectForResponse(tabListDataResponse);

            for (int i = 0; i < columnIds.size(); i++) {
                if (tabListDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnIds.get(i))).get("columnName").toString().equals("documentname")) {
                    documentName = tabListDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnIds.get(i))).get("value").toString();
                } else if (tabListDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnIds.get(i))).get("columnName").toString().equals("documentstatus")) {
                    documentStatus = tabListDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnIds.get(i))).get("value").toString();
                }
                else if(tabListDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnIds.get(i))).get("columnName").toString().equals("id")){
                    documentId = tabListDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnIds.get(i))).get("value").toString();
                }
                else if(tabListDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnIds.get(i))).get("columnName").toString().equals("template_type_id")){
                    contractDocumentTemplateTypeId = tabListDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnIds.get(i))).get("value").toString();
                }
            }
            logger.info("Document ID: "+documentId+"DocumentName: "+documentName+"DocumentStatus: "+documentStatus+"ContractDocumentTemplateTypeId: "+contractDocumentTemplateTypeId);
            String moveToTreePayload = "{\"baseEntityId\":"+contractId+",\"baseEntityTypeId\":61,\"sourceEntityTypeId\":160,\"sourceEntityId\":"+cdrId+",\"entityTypeId\":61,\"entityId\":"+contractId+",\"auditLogDocTreeFlowDocs\":[{\"auditLogDocFileId\":\""+ documentName.split(":;")[documentName.split(":;").length-1].trim() +"\"}],\"sourceTabId\":2,\"statusId\":1}";
            HttpResponse moveToTreeResponse = PreSignatureHelper.moveToTree(moveToTreePayload);
            if(moveToTreeResponse.getStatusLine().getStatusCode() == 200){
                csAssert.assertTrue(false, "Move To Tree Response is not Valid");
            }



        }catch (Exception e){
            csAssert.assertTrue(false, e.getMessage());
        }

    }


    private String createContractFromCDRResponse(String newPayload, String contractCreateSection) {
        logger.info("Hitting New V1 API for Contracts");
        New newObj = new New();
        newObj.hitNewV1ForMultiSupplier("contracts", newPayload);
        String newResponse = newObj.getNewJsonStr();

        if (newResponse != null) {
            if (ParseJsonResponse.validJsonResponse(newResponse)) {
                CreateEntity createEntityHelperObj = new CreateEntity(filePath, configFileName, filePath, extraFieldsFileName,
                        contractCreateSection);

                Map<String, String> extraFields = createEntityHelperObj.setExtraRequiredFields("contracts");
                newObj.setAllRequiredFields(newResponse);
                Map<String, String> allRequiredFields = newObj.getAllRequiredFields();
                allRequiredFields = createEntityHelperObj.processAllChildFields(allRequiredFields, newResponse);
                allRequiredFields = createEntityHelperObj.processNonChildFields(allRequiredFields, newResponse);

                String createPayload = PayloadUtils.getPayloadForCreate(newResponse, allRequiredFields, extraFields, null, filePath,
                        extraFieldsFileName);

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


}
