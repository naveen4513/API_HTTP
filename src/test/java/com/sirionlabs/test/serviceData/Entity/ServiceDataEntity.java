package com.sirionlabs.test.serviceData.Entity;

import com.sirionlabs.api.clientAdmin.ClientShow;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.New;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.invoice.InvoicePricingTemplateDownload;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.entityCreation.ChangeRequest;
import com.sirionlabs.helper.entityCreation.ContractDraftRequest;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ServiceDataEntity {

    private final Logger logger = LoggerFactory.getLogger(ServiceDataEntity.class);
    private final String configFilePath = "src/test/resources/TestConfig/ServiceData/Entity";
    private final String configFileName = "ServiceDataEntityConfig.cfg";
    private String contractConfigFilePath;
    private String contractConfigFileName;
    private String contractExtraFieldsConfigFileName;
    private String serviceDataConfigFilePath;
    private String serviceDataConfigFileName;
    private String serviceDataExtraFieldsConfigFileName;
    private String changeRequestConfigFilePath;
    private String changeRequestConfigFileName;
    private String changeRequestExtraFieldsConfigFileName;
    private String cdrConfigFilePath;
    private String cdrConfigFileName;
    private String cdrExtraFieldsConfigFileName;
    private int serviceDataTypeId = 64;
    private int clientCurrencyCount =0;
    private int serviceDataToUse =-1;
    private int crEntityTypeId = 63;
    private int cdrEntityTypeId = 160;
    private List<EntityPojo> toBeDeleted = new ArrayList<>();


    @BeforeClass
    public void beforeClass() throws Exception {

        //Contract Config files
        contractConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("contractFilePath");
        contractConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("contractFileName");
        contractExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("contractExtraFieldsFileName");

        //Service Data Config files
        serviceDataConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("serviceDataFilePath");
        serviceDataConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("serviceDataFileName");
        serviceDataExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataExtraFieldsFileName");

        //CR config files
        changeRequestConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ChangeRequestFilePath");
        changeRequestConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ChangeRequestFileName");
        changeRequestExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ChangeRequestExtraFieldsFileName");

        //CDR Config files
        cdrConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("CDRFilePath");
        cdrConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("CDRFileName");
        cdrExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("CDRExtraFieldsFileName");

    }

    @AfterTest
    public void deleteEntities(){
        for(EntityPojo entityPojo : toBeDeleted)
            EntityOperationsHelper.deleteEntityRecord(entityPojo.name,entityPojo.entityId);
    }


    @Test(enabled = true)
    public void C88408(){

        /*

        Login Client admin
        get all the client currencies

        Login to the end user

        Create contract
        Hit the new api for service data

        - check for the number of currencies and compare with the client currencies

        - create service data without invoicing currency
        - create service data with invoicing currency and then verify on show page

         */

        CustomAssert customAssert = new CustomAssert();

        try{
            String response = ClientShow.getClientShowResponse(1002);
            List<String> clientCurrencies = ClientShow.getAllCurrencies(response);

            customAssert.assertTrue(clientCurrencies!=null,"Client currencies found are zero");
            logger.info("Got the client currencies {}", clientCurrencies!=null?Arrays.toString(clientCurrencies.toArray()):"[NULL]");

            clientCurrencyCount = clientCurrencies.size();

            String flow = "fixed fee flow 1";

            int contractId = InvoiceHelper.getContractId(contractConfigFilePath,contractConfigFileName,contractExtraFieldsConfigFileName,flow);
            if (contractId != -1) {

                logger.info("Using Contract Id {} for Validation of Flow [{}]", contractId, flow);
                InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath,serviceDataConfigFileName,"","",flow,contractId);

                New newObj = new New();
                newObj.hitNew("service data","contracts",contractId);
                String newResponse = newObj.getNewJsonStr();

                JSONObject newJson = new JSONObject(newResponse);
                JSONArray jsonArrayCurrencyOnNew = newJson.getJSONObject("body").getJSONObject("data").getJSONObject("invoicingCurrency").getJSONObject("options").getJSONArray("data");

                customAssert.assertTrue(jsonArrayCurrencyOnNew.length()==clientCurrencies.size(),"Number of currencies at the client and the new api are not same. Client "+clientCurrencies.size()+", End user "+jsonArrayCurrencyOnNew.length());

                for(Object str : jsonArrayCurrencyOnNew){

                    if(str instanceof JSONObject){
                        if(!clientCurrencies.contains(((JSONObject) str).getString("name"))){
                            logger.info("Currency not found in client currencies, client {}, end user {}",clientCurrencies,str.toString());
                        }
                    }

                }

                logger.info("New response for service data creation {}",newResponse);

                int serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath,serviceDataConfigFileName,serviceDataExtraFieldsConfigFileName,flow, contractId);
                serviceDataToUse = serviceDataId;
                if(serviceDataId==-1){
                    logger.info("Cannot create service data");
                    customAssert.assertTrue(false,"Cannot create service data with invoicing currency filled");
                }
                Show show = new Show();
                show.hitShow(serviceDataTypeId,serviceDataId);
                String showResponse = show.getShowJsonStr();

                ParseJsonResponse parseJsonResponse = new ParseJsonResponse();
                parseJsonResponse.getNodeFromJson(new JSONObject(showResponse), Collections.singletonList("id"),12460,"values");
                Object object = parseJsonResponse.getJsonNodeValue();

                JSONObject jsonObject = new JSONObject(object.toString());
                String currencyValue = jsonObject.getString("name");

                String invoicingCurrencyPayload = ParseConfigFile.getValueFromConfigFileCaseSensitive(serviceDataConfigFilePath,serviceDataExtraFieldsConfigFileName,flow,"invoicingCurrency");
                invoicingCurrencyPayload="{"+invoicingCurrencyPayload+"}";
                invoicingCurrencyPayload=invoicingCurrencyPayload.replace("->",":");
                JSONObject expectedCurrencyPayload =  new JSONObject(invoicingCurrencyPayload);


                customAssert.assertTrue(currencyValue.equalsIgnoreCase(expectedCurrencyPayload.getJSONObject("values").getString("name")),"Invoicing currency doesn't after service data creation");


                UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath,serviceDataExtraFieldsConfigFileName,flow,"invoicingCurrency","values -> {}");

                serviceDataId = InvoiceHelper.getServiceDataIdForDifferentClientAndSupplierId(serviceDataConfigFilePath,serviceDataConfigFileName,serviceDataExtraFieldsConfigFileName,flow, contractId);
                if(serviceDataId==-1){
                    logger.info("Cannot create service data");
                    customAssert.assertTrue(false,"Cannot create service data without invoicing currency filled");
                }
            }
        }
        catch (Exception e){
            logger.error("Exception caught in the main block C3599");
            customAssert.assertTrue(false,"Exception caught");
        }

        customAssert.assertAll();
    }

    @Test(dependsOnMethods = "C88408",enabled = true)
    public void C88409(){

        /*
        - check client currencies count

        fire edit api for service data before publish
        - check the change in the invoicing currency

        fire edit api for service data after publish
        - check the change in the invoicing currency
         */

        CustomAssert customAssert = new CustomAssert();

        if(clientCurrencyCount<=1){
            customAssert.assertTrue(false,"There are not enough client currencies at the client for this test case.");
            customAssert.assertAll();
        }

        if(serviceDataToUse !=-1){
            try {
                String usdCurrencyPayload = "{\"name\": \"United States Dollar (USD)\", \"id\": 1,\"shortName\":\"USD\"}";
                String inrCurrencyPayload = "{\"name\": \"Indian Rupee (INR)\", \"id\": 8,\"shortName\":\"INR\"}";

                int serviceDataId = serviceDataToUse;

                Show show = new Show();
                show.hitShow(serviceDataTypeId,serviceDataId);
                String showResponse = show.getShowJsonStr();

                ParseJsonResponse parseJsonResponse = new ParseJsonResponse();
                parseJsonResponse.getNodeFromJson(new JSONObject(showResponse), Collections.singletonList("id"),12460,"values");
                Object object = parseJsonResponse.getJsonNodeValue();

                JSONObject jsonObject = new JSONObject(object.toString());
                String initCurrencyValue = jsonObject.getString("name");

                if(!initCurrencyValue.contains("USD")){
                    logger.error("The test was based on data which is initially set to USD, but not found as expected. Needs a check");
                    throw new SkipException("The test was based on data which is initially set to USD, but not found as expected. Needs a check");
                }


                Edit edit = new Edit();
                String response = edit.hitEdit("service data",serviceDataId);

                JSONObject jsonObjectPayload = new JSONObject(response);
                jsonObjectPayload.remove("header");
                jsonObjectPayload.remove("session");
                jsonObjectPayload.remove("actions");
                jsonObjectPayload.remove("createLinks");
                jsonObjectPayload.getJSONObject("body").remove("layoutInfo");
                jsonObjectPayload.getJSONObject("body").remove("globalData");
                jsonObjectPayload.getJSONObject("body").remove("errors");

                JSONObject jsonObject1 = new JSONObject(jsonObjectPayload.toString());
                jsonObject1.getJSONObject("body").getJSONObject("data").getJSONObject("invoicingCurrency").getJSONObject("values").put("name",new JSONObject(inrCurrencyPayload).getString("name"));
                jsonObject1.getJSONObject("body").getJSONObject("data").getJSONObject("invoicingCurrency").getJSONObject("values").put("id",new JSONObject(inrCurrencyPayload).getInt("id"));
                jsonObject1.getJSONObject("body").getJSONObject("data").getJSONObject("invoicingCurrency").getJSONObject("values").put("shortName",new JSONObject(inrCurrencyPayload).getString("shortName"));


                String editResponse = edit.hitEdit("service data",jsonObject1.toString());

                if(!editResponse.contains("success")){
                    customAssert.assertTrue(false,"Edit not successful");
                    customAssert.assertAll();
                }

                show.hitShow(serviceDataTypeId,serviceDataId);
                showResponse = show.getShowJsonStr();

                parseJsonResponse = new ParseJsonResponse();
                parseJsonResponse.getNodeFromJson(new JSONObject(showResponse), Collections.singletonList("id"),12460,"values");
                object = parseJsonResponse.getJsonNodeValue();

                jsonObject = new JSONObject(object.toString());
                if(!jsonObject.getString("name").equalsIgnoreCase(new JSONObject(inrCurrencyPayload).getString("name"))){
                    customAssert.assertTrue(false,"Currency not changed to new currency value");
                }

                boolean result = new WorkflowActionsHelper().performWorkflowAction(serviceDataTypeId, serviceDataId, "publish");

                if(!result) {
                    customAssert.assertTrue(result, "Couldn't perform workflow action i.e. publish");
                    customAssert.assertAll();
                }

                //trying to change value after publishing
                editResponse = edit.hitEdit("service data",jsonObjectPayload.toString());

                if(!editResponse.contains("success")){
                    customAssert.assertTrue(false,"Edit not successful");
                    customAssert.assertAll();
                }

                show.hitShow(serviceDataTypeId,serviceDataId);
                showResponse = show.getShowJsonStr();

                parseJsonResponse = new ParseJsonResponse();
                parseJsonResponse.getNodeFromJson(new JSONObject(showResponse), Collections.singletonList("id"),12460,"values");
                object = parseJsonResponse.getJsonNodeValue();

                jsonObject = new JSONObject(object.toString());
                if(jsonObject.getString("name").equalsIgnoreCase(new JSONObject(usdCurrencyPayload).getString("name"))){
                    customAssert.assertTrue(false,"Currency changed to new currency value, which was not supposed to happen after publish");
                }
            }
            catch (Exception e){
                logger.error("Exception caught {}", (Object) e.getStackTrace());
                customAssert.assertTrue(false,"Exception caught");
                customAssert.assertAll();
            }
        }
        customAssert.assertAll();

    }

    @Test
    public void C90581(){
        CustomAssert customAssert = new CustomAssert();
        try{


            String contractFlowName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"flows","contractflow");

            assert contractFlowName!=null:"Contract flows name are blank";

            String ccrFlow = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"flows","ccrflow");
            assert ccrFlow!=null:"ccr flow name null";
            String cdrFlow = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"flows","cdrflow");
            assert cdrFlow!=null:"cdr flow name null";

            int contract = InvoiceHelper.getContractId(contractConfigFilePath,contractConfigFileName,contractExtraFieldsConfigFileName,contractFlowName);
            assert contract!=-1:"Contract not created";

            toBeDeleted.add(new EntityPojo(contract,"contracts"));
            logger.info("contract id : [{}]",contract);

            //updating config files for ccr and cdr
            UpdateFile.updateConfigFileProperty(changeRequestConfigFilePath, changeRequestConfigFileName, ccrFlow, "sourceid", String.valueOf(contract));
            UpdateFile.updateConfigFileProperty(cdrConfigFilePath, cdrConfigFileName, cdrFlow, "sourceid", String.valueOf(contract));


            //*************************************************** create ccr ****************************
            String createResponse = ChangeRequest.createChangeRequest(changeRequestConfigFilePath, changeRequestConfigFileName, changeRequestConfigFilePath, changeRequestExtraFieldsConfigFileName, ccrFlow,
                    true);
            int ccr =-1;
            String ccrName = null;
            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                JSONObject jsonObj = new JSONObject(createResponse);
                String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
                logger.info("Create Status for Flow [{}]: {}", ccrFlow, createStatus);

                if (createStatus.equalsIgnoreCase("success"))
                    ccr = CreateEntity.getNewEntityId(createResponse, "change requests");

                if (createStatus.equalsIgnoreCase("success")) {
                    if (ccr != -1) {
                        logger.info("CR Created Successfully with Id {}: ", ccr);
                        logger.info("Hitting Show API for CR Id {}", ccr);
                        Show showObj = new Show();
                        showObj.hitShow(crEntityTypeId, ccr);
                        String showResponse = showObj.getShowJsonStr();

                        if (ParseJsonResponse.validJsonResponse(showResponse)) {
                            if (!showObj.isShowPageAccessible(showResponse)) {
                                customAssert.assertTrue(false, "Show Page is Not Accessible for CR Id " + ccr);
                            }

                            ccrName = "(" + new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("shortCodeId").getString("values") + ") " + new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("name").getString("values");
                        } else {
                            customAssert.assertTrue(false, "Show API Response for CR Id " + ccr + " is an Invalid JSON.");
                        }
                    }
                } else {
                    customAssert.assertTrue(false, "Couldn't create CR for Flow [" + ccrFlow + "] due to " + createStatus);
                }
            } else {
                customAssert.assertTrue(false, "Create API Response for CR Creation Flow [" + ccrFlow + "] is an Invalid JSON.");
            }
            assert ccr!=-1:"ccr not created";

            toBeDeleted.add(new EntityPojo(ccr,"change requests"));
            logger.info("ccr id : [{}] and name : [{}]",ccr,ccrName);

            String submitWorkflowAction = "Submit";

            WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
            logger.info("Performing Workflow action on CR {}, : {}",ccr,submitWorkflowAction);
            boolean workflowTaskResponse = workflowActionsHelper.performWorkflowAction(crEntityTypeId,ccr,submitWorkflowAction);

            logger.info("Workflow task response is : {}",workflowTaskResponse);

            //*************************************************** create cdr ****************************
            createResponse = ContractDraftRequest.createCDR(cdrConfigFilePath,cdrConfigFileName, cdrConfigFilePath, cdrExtraFieldsConfigFileName, cdrFlow,
                    true);
            int cdr=-1;
            String cdrName=null;
            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                JSONObject jsonObj = new JSONObject(createResponse);
                String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
                logger.info("Create Status for Flow [{}]: {}", cdrFlow, createStatus);

                if (createStatus.equalsIgnoreCase("success"))
                    cdr = CreateEntity.getNewEntityId(createResponse, "change requests");

                if (createStatus.equalsIgnoreCase("success")) {
                    if (cdr != -1) {
                        logger.info("CDR Created Successfully with Id {}: ", cdr);
                        logger.info("Hitting Show API for CDR Id {}", cdr);
                        Show showObj = new Show();
                        showObj.hitShow(cdrEntityTypeId, cdr);
                        String showResponse = showObj.getShowJsonStr();

                        if (ParseJsonResponse.validJsonResponse(showResponse)) {
                            if (!showObj.isShowPageAccessible(showResponse)) {
                                customAssert.assertTrue(false, "Show Page is Not Accessible for CDR Id " + cdr);
                            }

                            cdrName = "("+new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("shortCodeId").getString("values")+") "+new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("name").getString("values");
                        } else {
                            customAssert.assertTrue(false, "Show API Response for CDR Id " + cdr + " is an Invalid JSON.");
                        }
                    }
                } else {
                    customAssert.assertTrue(false, "Couldn't create CDR for Flow [" + cdrFlow + "] due to " + createStatus);
                }
            } else {
                customAssert.assertTrue(false, "Create API Response for CDR Creation Flow [" + cdrFlow + "] is an Invalid JSON.");
            }

            assert cdr!=-1:"cdr not created";

            toBeDeleted.add(new EntityPojo(cdr,"contract draft request"));
            logger.info("cdr id : [{}] and name : [{}]",cdr,cdrName);

            //***************************************************** create service data sd **************

            String sdFlow = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"flows","sd");
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataConfigFileName, sdFlow, "sourceid", String.valueOf(contract));
            assert sdFlow!=null:"Service data flow is null";

            int sd = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath,serviceDataConfigFileName,serviceDataExtraFieldsConfigFileName,sdFlow,contract);

            assert sd!=-1:"sd not created";

            toBeDeleted.add(new EntityPojo(sd,"service data"));
            logger.info("sd id : [{}]",sd);

            //*************************************************** Download pricing template ******************************

            String downloadFilePath = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,"downloadFilePath");
            String downloadFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,"downloadFileName");

            assert downloadFileName!=null&&downloadFilePath!=null:"download file details not found";

            InvoicePricingTemplateDownload invoicePricingTemplateDownload = new InvoicePricingTemplateDownload();
            invoicePricingTemplateDownload.downloadInvoicePricingTemplateFile(downloadFilePath + downloadFileName, "", "", "64", Collections.singletonList(String.valueOf(sd)));
            if (!FileUtils.fileExists(downloadFilePath, downloadFileName)) {
                customAssert.assertTrue(false, "Downloaded pricing file doesn't exist for only pricing available entity");
            }

            long rowCount = XLSUtils.getNoOfRows(downloadFilePath,downloadFileName,"Master Data");
            List<String> masterDataSheet = XLSUtils.getOneColumnDataFromMultipleRows(downloadFilePath,downloadFileName,"Master Data",0,4,(int)rowCount);

            assert masterDataSheet.contains(ccrName):"Master data doesn't contain CCR, "+submitWorkflowAction+" workflow action result is "+workflowTaskResponse;
            assert masterDataSheet.contains(cdrName):"Master data doesn't contain CDR";
        }
        catch (Exception e){
            customAssert.assertTrue(false,"Exception occurred "+ Arrays.toString(e.getStackTrace()));
            customAssert.assertAll();
        }
        customAssert.assertAll();
    }


    class EntityPojo{
        public EntityPojo(int id,String name){
            this.entityId=id;
            this.name=name;
        }
        int entityId;
        String name;
    }
}
