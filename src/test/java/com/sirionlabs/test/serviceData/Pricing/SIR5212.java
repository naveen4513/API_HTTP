package com.sirionlabs.test.serviceData.Pricing;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.invoice.InvoicePricingTemplateDownload;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.entityCreation.*;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class SIR5212 {

    private final Logger logger = LoggerFactory.getLogger(SIR5212.class);
    private final String configFilePath = "src/test/resources/TestConfig/ServiceData/Pricing";
    private final String configFileName = "SIR5212.cfg";
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
    private int crEntityTypeId = 63;
    private int cdrEntityTypeId = 160;
    private List<EntityPojo> toBeDeleted = new ArrayList<>();
    private int cdrListId = 279,columnIdForIDCDR = 12259;

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

    @Test
  public void C90831(){ //also covering C90830, has step 2 of C90577(partial), partial C90591
        /*
        Create contract c3 from supplier s2
        Create ccr ccr3 and cdr cdr3 from contract c3


        Create a contract c1 from supplier s1
        Create ccr ccr1 and cdr cdr1 from c1
        Create service data sd1 from c1
        Create ccr ccr2 from supplier s1
        Download the pricing template of sd1
        only ccr1, ccr2 and cdr1 only will be present in the template master data(ccr3 and cdr3 should not come)


        Create cdr cdr2 globally
        Create Contract c2 from cdr2
        Create Service data sd2 from c2
        Download the pricing template of sd2
        only cdr2 will be present in the template master data(no other data)
         */

        CustomAssert customAssert = new CustomAssert();

        try{



            String createApiResponse = Supplier.createSupplier("flow 1", true);
            logger.info("Create api response : {}", createApiResponse);
            int sup1=-1;
            if (APIUtils.validJsonResponse(createApiResponse, "createApiResponse")) {
                JSONObject jsonRes = new JSONObject(createApiResponse);
                String status = jsonRes.getJSONObject("header").getJSONObject("response").getString("status");
                if (status.equalsIgnoreCase("success")) {
                    String notification = jsonRes.getJSONObject("header").getJSONObject("response").getJSONObject("properties").getString("notification");
                    //<a id=\"hrefElemId\" href=\"#/show/tblrelations/1300\" style=\"color: #9E866A; font-size: 11px;\">SP01183</a> created successfully.

                    int startIndex = notification.indexOf("show");
                    int endIndex = notification.indexOf("\"", startIndex);
                    String temp = notification.substring(startIndex, endIndex); // show/tblrelations/1300
                    sup1 = Integer.parseInt(temp.split("/")[2].trim());
                }
            }

            assert sup1 !=-1:"sup1 not created";


            createApiResponse = Supplier.createSupplier("flow 1", true);
            logger.info("Create api response : {}", createApiResponse);
            int sup2=-1;
            if (APIUtils.validJsonResponse(createApiResponse, "createApiResponse")) {
                JSONObject jsonRes = new JSONObject(createApiResponse);
                String status = jsonRes.getJSONObject("header").getJSONObject("response").getString("status");
                if (status.equalsIgnoreCase("success")) {
                    String notification = jsonRes.getJSONObject("header").getJSONObject("response").getJSONObject("properties").getString("notification");
                    //<a id=\"hrefElemId\" href=\"#/show/tblrelations/1300\" style=\"color: #9E866A; font-size: 11px;\">SP01183</a> created successfully.

                    int startIndex = notification.indexOf("show");
                    int endIndex = notification.indexOf("\"", startIndex);
                    String temp = notification.substring(startIndex, endIndex); // show/tblrelations/1300
                    sup2 = Integer.parseInt(temp.split("/")[2].trim());
                }
            }

            assert sup2 !=-1:"sup2 not created";

            toBeDeleted.add(new EntityPojo(sup1,"suppliers"));
            toBeDeleted.add(new EntityPojo(sup2,"suppliers"));

            String contractC1FlowName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"flows","c1");
            String contractC2FlowName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"flows","c2");

            assert contractC1FlowName!=null&&contractC2FlowName!=null:"Contract flows name are blank";

            UpdateFile.updateConfigFileProperty(contractConfigFilePath,contractConfigFileName,contractC1FlowName,"sourceid",String.valueOf(sup1));
            UpdateFile.updateConfigFileProperty(contractConfigFilePath,contractConfigFileName,contractC2FlowName,"sourceid",String.valueOf(sup2));

            int contract3 = InvoiceHelper.getContractId(contractConfigFilePath,contractConfigFileName,contractExtraFieldsConfigFileName,contractC2FlowName);
            assert contract3!=-1:"Contract3 not created";

            toBeDeleted.add(new EntityPojo(contract3,"contracts"));
            logger.info("contract3 id : [{}]",contract3);

            String ccr3Flow = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"flows","ccr3");
            assert ccr3Flow!=null:"ccr3 flow name null";
            String cdr3Flow = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"flows","cdr3");
            assert cdr3Flow!=null:"cdr3 flow name null";

            //updating config files for ccr and cdr
            UpdateFile.updateConfigFileProperty(changeRequestConfigFilePath, changeRequestConfigFileName, ccr3Flow, "sourceid", String.valueOf(contract3));
            UpdateFile.updateConfigFileProperty(cdrConfigFilePath, cdrConfigFileName, cdr3Flow, "sourceid", String.valueOf(contract3));


            //*************************************************** create ccr 3 ****************************
            String createResponse = ChangeRequest.createChangeRequest(changeRequestConfigFilePath, changeRequestConfigFileName, changeRequestConfigFilePath, changeRequestExtraFieldsConfigFileName, ccr3Flow,
                    true);
            int ccr3 =-1;
            String ccr3Name = null;
            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                JSONObject jsonObj = new JSONObject(createResponse);
                String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
                logger.info("Create Status for Flow [{}]: {}", ccr3Flow, createStatus);

                if (createStatus.equalsIgnoreCase("success"))
                    ccr3 = CreateEntity.getNewEntityId(createResponse, "change requests");

                if (createStatus.equalsIgnoreCase("success")) {
                    if (ccr3 != -1) {
                        logger.info("CR Created Successfully with Id {}: ", ccr3);
                        logger.info("Hitting Show API for CR Id {}", ccr3);
                        Show showObj = new Show();
                        showObj.hitShow(crEntityTypeId, ccr3);
                        String showResponse = showObj.getShowJsonStr();

                        if (ParseJsonResponse.validJsonResponse(showResponse)) {
                            if (!showObj.isShowPageAccessible(showResponse)) {
                                customAssert.assertTrue(false, "Show Page is Not Accessible for CR Id " + ccr3);
                            }

                            ccr3Name = "(" + new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("shortCodeId").getString("values") + ") " + new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("name").getString("values");
                        } else {
                            customAssert.assertTrue(false, "Show API Response for CR Id " + ccr3 + " is an Invalid JSON.");
                        }
                    }
                } else {
                    customAssert.assertTrue(false, "Couldn't create CR for Flow [" + ccr3Flow + "] due to " + createStatus);
                }
            } else {
                customAssert.assertTrue(false, "Create API Response for CR Creation Flow [" + ccr3Flow + "] is an Invalid JSON.");
            }
            assert ccr3!=-1:"ccr3 not created";

            toBeDeleted.add(new EntityPojo(ccr3,"change requests"));
            logger.info("ccr3 id : [{}] and name : [{}]",ccr3,ccr3Name);

            //*************************************************** create cdr 3 ****************************
            createResponse = ContractDraftRequest.createCDR(cdrConfigFilePath,cdrConfigFileName, cdrConfigFilePath, cdrExtraFieldsConfigFileName, cdr3Flow,
                    true);
            int cdr3=-1;
            String cdr3Name=null;
            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                JSONObject jsonObj = new JSONObject(createResponse);
                String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
                logger.info("Create Status for Flow [{}]: {}", cdr3Flow, createStatus);

                if (createStatus.equalsIgnoreCase("success"))
                    cdr3 = CreateEntity.getNewEntityId(createResponse, "change requests");

                if (createStatus.equalsIgnoreCase("success")) {
                        if (cdr3 != -1) {
                            logger.info("CDR Created Successfully with Id {}: ", cdr3);
                            logger.info("Hitting Show API for CDR Id {}", cdr3);
                            Show showObj = new Show();
                            showObj.hitShow(cdrEntityTypeId, cdr3);
                            String showResponse = showObj.getShowJsonStr();

                            if (ParseJsonResponse.validJsonResponse(showResponse)) {
                                if (!showObj.isShowPageAccessible(showResponse)) {
                                    customAssert.assertTrue(false, "Show Page is Not Accessible for CDR Id " + cdr3);
                                }

                                cdr3Name = "("+new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("shortCodeId").getString("values")+") "+new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("name").getString("values");
                            } else {
                                customAssert.assertTrue(false, "Show API Response for CDR Id " + cdr3 + " is an Invalid JSON.");
                            }
                        }
                    } else {
                        customAssert.assertTrue(false, "Couldn't create CDR for Flow [" + cdr3Flow + "] due to " + createStatus);
                    }
            } else {
                customAssert.assertTrue(false, "Create API Response for CDR Creation Flow [" + cdr3Flow + "] is an Invalid JSON.");
            }

            assert cdr3!=-1:"cdr3 not created";

            toBeDeleted.add(new EntityPojo(cdr3,"contract draft request"));
            logger.info("cdr3 id : [{}] and name : [{}]",cdr3,cdr3Name);

            //******************************** part 1 complete *********************************************


            int contract1 = InvoiceHelper.getContractId(contractConfigFilePath,contractConfigFileName,contractExtraFieldsConfigFileName,contractC1FlowName);
            assert contract1!=-1:"Contract1 not created";

            toBeDeleted.add(new EntityPojo(contract1,"contracts"));
            logger.info("contract1 id : [{}]",contract1);

            //updating config files for ccr and cdr
            UpdateFile.updateConfigFileProperty(changeRequestConfigFilePath, changeRequestConfigFileName, ccr3Flow, "sourceid", String.valueOf(contract1));
            UpdateFile.updateConfigFileProperty(cdrConfigFilePath, cdrConfigFileName, cdr3Flow, "sourceid", String.valueOf(contract1));


            //*************************************************** create ccr 1 ****************************
            createResponse = ChangeRequest.createChangeRequest(changeRequestConfigFilePath, changeRequestConfigFileName, changeRequestConfigFilePath, changeRequestExtraFieldsConfigFileName, ccr3Flow,
                    true);
            int ccr1 =-1;
            String ccr1Name = null;
            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                JSONObject jsonObj = new JSONObject(createResponse);
                String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
                logger.info("Create Status for Flow [{}]: {}", ccr3Flow, createStatus);

                if (createStatus.equalsIgnoreCase("success"))
                    ccr1 = CreateEntity.getNewEntityId(createResponse, "change requests");

                if (createStatus.equalsIgnoreCase("success")) {
                    if (ccr1 != -1) {
                        logger.info("CR Created Successfully with Id {}: ", ccr1);
                        logger.info("Hitting Show API for CR Id {}", ccr1);
                        Show showObj = new Show();
                        showObj.hitShow(crEntityTypeId, ccr1);
                        String showResponse = showObj.getShowJsonStr();

                        if (ParseJsonResponse.validJsonResponse(showResponse)) {
                            if (!showObj.isShowPageAccessible(showResponse)) {
                                customAssert.assertTrue(false, "Show Page is Not Accessible for CR Id " + ccr1);
                            }

                            ccr1Name = "(" + new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("shortCodeId").getString("values") + ") " + new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("name").getString("values");
                        } else {
                            customAssert.assertTrue(false, "Show API Response for CR Id " + ccr1 + " is an Invalid JSON.");
                        }
                    }
                } else {
                    customAssert.assertTrue(false, "Couldn't create CR for Flow [" + ccr3Flow + "] due to " + createStatus);
                }
            } else {
                customAssert.assertTrue(false, "Create API Response for CR Creation Flow [" + ccr3Flow + "] is an Invalid JSON.");
            }
            assert ccr1!=-1:"ccr1 not created";

            toBeDeleted.add(new EntityPojo(ccr1,"change requests"));
            logger.info("ccr1 id : [{}] and name : [{}]",ccr1,ccr1Name);

            //*************************************************** create cdr 1 ****************************
            createResponse = ContractDraftRequest.createCDR(cdrConfigFilePath,cdrConfigFileName, cdrConfigFilePath, cdrExtraFieldsConfigFileName, cdr3Flow,
                    true);
            int cdr1=-1;
            String cdr1Name=null;
            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                JSONObject jsonObj = new JSONObject(createResponse);
                String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
                logger.info("Create Status for Flow [{}]: {}", cdr3Flow, createStatus);

                if (createStatus.equalsIgnoreCase("success"))
                    cdr1 = CreateEntity.getNewEntityId(createResponse, "change requests");

                if (createStatus.equalsIgnoreCase("success")) {
                    if (cdr1 != -1) {
                        logger.info("CDR Created Successfully with Id {}: ", cdr1);
                        logger.info("Hitting Show API for CDR Id {}", cdr1);
                        Show showObj = new Show();
                        showObj.hitShow(cdrEntityTypeId, cdr1);
                        String showResponse = showObj.getShowJsonStr();

                        if (ParseJsonResponse.validJsonResponse(showResponse)) {
                            if (!showObj.isShowPageAccessible(showResponse)) {
                                customAssert.assertTrue(false, "Show Page is Not Accessible for CDR Id " + cdr1);
                            }

                            cdr1Name = "("+new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("shortCodeId").getString("values")+") "+new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("name").getString("values");
                        } else {
                            customAssert.assertTrue(false, "Show API Response for CDR Id " + cdr1 + " is an Invalid JSON.");
                        }
                    }
                } else {
                    customAssert.assertTrue(false, "Couldn't create CDR for Flow [" + cdr3Flow + "] due to " + createStatus);
                }
            } else {
                customAssert.assertTrue(false, "Create API Response for CDR Creation Flow [" + cdr3Flow + "] is an Invalid JSON.");
            }

            assert cdr1!=-1:"cdr1 not created";

            toBeDeleted.add(new EntityPojo(cdr1,"contract draft request"));
            logger.info("cdr1 id : [{}] and name : [{}]",cdr1,cdr1Name);

            //***************************************************** create service data sd1 **************

            String sdFlow = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"flows","sd1");
            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataConfigFileName, sdFlow, "sourceid", String.valueOf(contract1));
            assert sdFlow!=null:"Service data flow is null";

            int sd1 = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath,serviceDataConfigFileName,serviceDataExtraFieldsConfigFileName,sdFlow,contract1);

            assert sd1!=-1:"sd1 not created";

            toBeDeleted.add(new EntityPojo(sd1,"service data"));
            logger.info("sd1 id : [{}]",sd1);


            //****************************************************** create ccr2 *************************

            String supplier1Id = ParseConfigFile.getValueFromConfigFile(contractConfigFilePath,contractConfigFileName,contractC1FlowName,"sourceid");
            assert supplier1Id!=null:"Supplier id is null";

            UpdateFile.updateConfigFileProperty(changeRequestConfigFilePath, changeRequestConfigFileName, ccr3Flow, "sourceid", supplier1Id);
            UpdateFile.updateConfigFileProperty(changeRequestConfigFilePath, changeRequestConfigFileName, ccr3Flow, "sourceentity", "suppliers");
            createResponse = ChangeRequest.createChangeRequest(changeRequestConfigFilePath, changeRequestConfigFileName, changeRequestConfigFilePath, changeRequestExtraFieldsConfigFileName, ccr3Flow,
                    true);
            int ccr2 =-1;
            String ccr2Name = null;
            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                JSONObject jsonObj = new JSONObject(createResponse);
                String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
                logger.info("Create Status for Flow [{}]: {}", ccr3Flow, createStatus);

                if (createStatus.equalsIgnoreCase("success"))
                    ccr2 = CreateEntity.getNewEntityId(createResponse, "change requests");

                if (createStatus.equalsIgnoreCase("success")) {
                    if (ccr2 != -1) {
                        logger.info("CR Created Successfully with Id {}: ", ccr2);
                        logger.info("Hitting Show API for CR Id {}", ccr2);
                        Show showObj = new Show();
                        showObj.hitShow(crEntityTypeId, ccr2);
                        String showResponse = showObj.getShowJsonStr();

                        if (ParseJsonResponse.validJsonResponse(showResponse)) {
                            if (!showObj.isShowPageAccessible(showResponse)) {
                                customAssert.assertTrue(false, "Show Page is Not Accessible for CR Id " + ccr2);
                            }

                            ccr2Name = "(" + new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("shortCodeId").getString("values") + ") " + new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("name").getString("values");
                        } else {
                            customAssert.assertTrue(false, "Show API Response for CR Id " + ccr2 + " is an Invalid JSON.");
                        }
                    }
                } else {
                    customAssert.assertTrue(false, "Couldn't create CR for Flow [" + ccr3Flow + "] due to " + createStatus);
                }
            } else {
                customAssert.assertTrue(false, "Create API Response for CR Creation Flow [" + ccr3Flow + "] is an Invalid JSON.");
            }
            assert ccr2!=-1:"ccr2 not created";

            toBeDeleted.add(new EntityPojo(ccr2,"change requests"));
            logger.info("ccr2 id : [{}] and name : [{}]",ccr2,ccr2Name);

            //*************************************************** Download pricing template ******************************

            String downloadFilePath = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,"downloadFilePath");
            String downloadFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,"downloadFileName");

            assert downloadFileName!=null&&downloadFilePath!=null:"download file details not found";

            InvoicePricingTemplateDownload invoicePricingTemplateDownload = new InvoicePricingTemplateDownload();
            invoicePricingTemplateDownload.downloadInvoicePricingTemplateFile(downloadFilePath + downloadFileName, "", "", "64", Collections.singletonList(String.valueOf(sd1)));
            if (!FileUtils.fileExists(downloadFilePath, downloadFileName)) {
                customAssert.assertTrue(false, "Downloaded pricing file doesn't exist for only pricing available entity");
            }

            long rowCount = XLSUtils.getNoOfRows(downloadFilePath,downloadFileName,"Master Data");
            List<String> masterDataSheet = XLSUtils.getOneColumnDataFromMultipleRows(downloadFilePath,downloadFileName,"Master Data",0,4,(int)rowCount);

            logger.info("Master Data for Part 2 : {}",masterDataSheet);
            assert masterDataSheet.size()==3:"Master Data count of cdr and ccr are incorrect";

            assert masterDataSheet.contains(ccr1Name):"Master data doesn't contain CCR1";
            assert masterDataSheet.contains(ccr2Name):"Master data doesn't contain CCR2";
            assert masterDataSheet.contains(cdr1Name):"Master data doesn't contain CDR1";

            //******************************************************* Part 2 complete *****************************
            ListRendererListData cdrListRendererListData = new ListRendererListData();
            cdrListRendererListData.hitListRendererListData(cdrEntityTypeId, 10, 10, "id", "desc", cdrListId);
            String cdrListDataResponse = cdrListRendererListData.getListDataJsonStr(); //Calling dateColumnIds API for CDR

            JSONObject cdrListRenderResponseJson = new JSONObject(cdrListDataResponse);

            ParseJsonResponse parseJsonResponse = new ParseJsonResponse();
            try {
                parseJsonResponse.getNodeFromJsonWithValue(cdrListRenderResponseJson, Collections.singletonList("columnId"), columnIdForIDCDR); //Extracting columnIdStatus for the first CDR in dateColumnIds
            } catch (Exception e) {
                logger.error("Exception in extracting value of the dateColumnIds data API");
                customAssert.assertTrue(false, "Cannot extract CDR from cdr listing, hence marking it failed");
                customAssert.assertAll();
            }

            int cdr2 = -1;
            if (parseJsonResponse.getJsonNodeValue() instanceof String)
                cdr2 = Integer.parseInt(((String) parseJsonResponse.getJsonNodeValue()).split(":;")[1]); //Extracting id from the column value of the CDR dateColumnIds
            else {
                logger.error("columnIdStatus could not be retrieved correctly");
            }

            assert cdr2!=-1:"cdr2 not created";


            createResponse = ContractDraftRequest.createCDR();

            String cdr2Name = null;

            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                JSONObject jsonObj = new JSONObject(createResponse);
                String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
                logger.info("Create Status for cdr2 {}", createStatus);

                if (createStatus.equalsIgnoreCase("success"))
                    cdr2 = CreateEntity.getNewEntityId(createResponse, "change requests");

                if (createStatus.equalsIgnoreCase("success")) {
                    if (cdr2 != -1) {
                        logger.info("CDR Created Successfully with Id {}: ", cdr2);
                        logger.info("Hitting Show API for CDR Id {}", cdr2);
                        Show showObj = new Show();
                        showObj.hitShow(cdrEntityTypeId, cdr2);
                        String showResponse = showObj.getShowJsonStr();

                        if (ParseJsonResponse.validJsonResponse(showResponse)) {
                            if (!showObj.isShowPageAccessible(showResponse)) {
                                customAssert.assertTrue(false, "Show Page is Not Accessible for CDR Id " + cdr2);
                            }

                            cdr2Name = "("+new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("shortCodeId").getString("values")+") "+new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("name").getString("values");
                        } else {
                            customAssert.assertTrue(false, "Show API Response for CDR Id " + cdr2 + " is an Invalid JSON.");
                        }
                    }
                } else {
                    customAssert.assertTrue(false, "Couldn't create CDR2 due to " + createStatus);
                }
            } else {
                customAssert.assertTrue(false, "Create API Response for CDR2 is an Invalid JSON.");
            }

            assert cdr2!=-1:"cdr2 not created";


            logger.info("cdr2 name {}",cdr2Name);

            UpdateFile.addPropertyToConfigFile(contractConfigFilePath,contractExtraFieldsConfigFileName,contractC1FlowName,"sourceEntityId","{\"values\":"+cdr2+"}");
            UpdateFile.addPropertyToConfigFile(contractConfigFilePath,contractExtraFieldsConfigFileName,contractC1FlowName,"sourceEntityTypeId","{\"values\":"+cdrEntityTypeId+"}");

            createApiResponse = Supplier.createSupplier("flow 1", true);
            logger.info("Create api response : {}", createApiResponse);
            int sup3=-1;
            if (APIUtils.validJsonResponse(createApiResponse, "createApiResponse")) {
                JSONObject jsonRes = new JSONObject(createApiResponse);
                String status = jsonRes.getJSONObject("header").getJSONObject("response").getString("status");
                if (status.equalsIgnoreCase("success")) {
                    String notification = jsonRes.getJSONObject("header").getJSONObject("response").getJSONObject("properties").getString("notification");
                    //<a id=\"hrefElemId\" href=\"#/show/tblrelations/1300\" style=\"color: #9E866A; font-size: 11px;\">SP01183</a> created successfully.

                    int startIndex = notification.indexOf("show");
                    int endIndex = notification.indexOf("\"", startIndex);
                    String temp = notification.substring(startIndex, endIndex); // show/tblrelations/1300
                    sup3 = Integer.parseInt(temp.split("/")[2].trim());
                }
            }

            assert sup3 !=-1:"sup3 not created";

            toBeDeleted.add(new EntityPojo(sup3,"suppliers"));
            UpdateFile.updateConfigFileProperty(contractConfigFilePath,contractConfigFileName,contractC1FlowName,"sourceid",String.valueOf(sup3));


            int contract2 = InvoiceHelper.getContractId(contractConfigFilePath, contractConfigFileName, contractExtraFieldsConfigFileName, contractC1FlowName);
            assert contract2!=-1:"contract2 not created";

            toBeDeleted.add(new EntityPojo(contract2,"contracts"));
            logger.info("contract2 id : [{}]",contract2);


            //***************************************************** create service data sd1 **************

            UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath, serviceDataConfigFileName, sdFlow, "sourceid", String.valueOf(contract2));

            int sd2 = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath,serviceDataConfigFileName,serviceDataExtraFieldsConfigFileName,sdFlow,contract2);

            assert sd2!=-1:"sd2 not created";

            toBeDeleted.add(new EntityPojo(sd2,"service data"));
            logger.info("sd2 id : [{}]",sd2);

            //*************************************************** Download pricing template ******************************

            invoicePricingTemplateDownload = new InvoicePricingTemplateDownload();
            invoicePricingTemplateDownload.downloadInvoicePricingTemplateFile(downloadFilePath + downloadFileName, "", "", "64", Collections.singletonList(String.valueOf(sd2)));
            if (!FileUtils.fileExists(downloadFilePath, downloadFileName)) {
                customAssert.assertTrue(false, "Downloaded pricing file doesn't exist for only pricing available entity");
            }

            rowCount = XLSUtils.getNoOfRows(downloadFilePath,downloadFileName,"Master Data");
            masterDataSheet = XLSUtils.getOneColumnDataFromMultipleRows(downloadFilePath,downloadFileName,"Master Data",0,4,(int)rowCount);

            logger.info("Master Data for Part 3 : {}",masterDataSheet);
            assert masterDataSheet.size()==1:"Master Data count of cdr and ccr are incorrect";

            assert masterDataSheet.contains(cdr2Name):"Master data doesn't contain CDR2";

        }
        catch (Exception e){
            customAssert.assertTrue(false,"Exception occurred "+ Arrays.toString(e.getStackTrace()));
        }

        customAssert.assertAll();

    }

}

class EntityPojo{
    public EntityPojo(int id,String name){
        this.entityId=id;
        this.name=name;
    }
    int entityId;
    String name;
}
