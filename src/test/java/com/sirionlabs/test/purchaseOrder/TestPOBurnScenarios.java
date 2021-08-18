package com.sirionlabs.test.purchaseOrder;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.PurchaseOrder;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestPOBurnScenarios {

    private final static Logger logger = LoggerFactory.getLogger(TestPOBurnScenarios.class);

    private String configFilePath;
    private String configFileName;
    private String serviceDataConfigFilePath;
    private String serviceDataConfigFileName;
    private String serviceDataExtraFieldsConfigFileName;

    private String invoiceConfigFilePath;
    private String invoiceConfigFileName;
    private String invoiceExtraFieldsConfigFileName;

    private String invoiceLineItemConfigFilePath;
    private String invoiceLineItemConfigFileName;
    private String invoiceLineItemExtraFieldsConfigFileName;

    private String poConfigFilePath;
    private String poConfigFileName;
    private String poExtraFieldsConfigFileName;

    private int serviceDataEntityTypeId;
    private int consumptionEntityTypeId;
    private int invoiceEntityTypeId;
    private int invoiceLineItemEntityTypeId;
    private int poEntityTypeId;


    private String publishAction;
    private String approveCons;
    private String approveInvoice;
    private String approveLineItem;
    private String archive;

    @BeforeClass
    public void beforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("POBurnFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("POBurnFileName");

        poConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("PurchaseOrderFilePath");
        poConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("PurchaseOrderFileName");
        poExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("PurchaseOrderExtraFieldsFileName");

        //Service Data Config files
        serviceDataConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("serviceDataFilePath");
        serviceDataConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("serviceDataFileName");
        serviceDataExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataExtraFieldsFileName");

        //Invoice Config files
        invoiceConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceFilePath");
        invoiceConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceFileName");
        invoiceExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceExtraFieldsFileName");

        //Invoice Line Item Config files
        invoiceLineItemConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceLineItemFilePath");
        invoiceLineItemConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceLineItemFileName");
        invoiceLineItemExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("invoiceLineItemExtraFieldsFileName");

        publishAction = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"publish action name");
        approveCons = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"approve consumption");
        approveInvoice = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"approve inv action name");
        approveLineItem = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"approve line item action name");
        archive = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"archive line item action name");

        serviceDataEntityTypeId = 64;
        consumptionEntityTypeId = 176;
        invoiceEntityTypeId = 67;
        invoiceLineItemEntityTypeId = 165;
        poEntityTypeId = 181;

    }

    @DataProvider(parallel = false)
    public Object[][] dataProviderForPOFlows() {
        logger.info("Setting all Purchase Order Flows to Validate");
        List<Object[]> allTestData = new ArrayList<>();
        String[] allFlowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"flows to test").split(",");

        for (String flowToTest : allFlowsToTest) {

            allTestData.add(new Object[]{flowToTest});
        }

        return allTestData.toArray(new Object[0][]);
    }

    /*
    Test case logic is such that it will first create a
    PO than Service Data( w/o this PO) than invoice (with this single PO)
    Than Check on Invoice Line Item has this PO Linked Automatically
    Than Approve Invoice and Invoice Line Item
    Than check for PO Finance Field Values
    Total PO Value Remains Unchanged
    Expected PO Burn Value have the addition of previous value and supplier amount value at line item
    PO Burn Value have the addition of previous value and system amount value at line item
    PO Available Value have the subtraction of previous value and system amount value at line item
    */
    @Test(dataProvider = "dataProviderForPOFlows", enabled = true)
    public void Test_POBurn_SinglePO(String flowToTest) {

        CustomAssert customAssert = new CustomAssert();

        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
        InvoiceHelper invoiceHelper = new InvoiceHelper();
        int poId = -1;
        ArrayList<Integer> poIdList = new ArrayList<>();
        ArrayList<String> poTotalList = new ArrayList<>();
        try {

            //For mulitple PO Scenario Testing PO1 will be the active PO as it has greter PO available
            poTotalList.add("800");
            poTotalList.add("600");

            String uniqueString = DateUtils.getCurrentTimeStamp();
            String contractId =ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"base contract id");
            String numOfPo = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,flowToTest,"num of po");
            int poToCreate = 1;
            if(numOfPo == null){
                poToCreate = 1;
            }else if(numOfPo.equals("")){
                poToCreate = 1;
            }else {
                poToCreate = Integer.parseInt(numOfPo);
            }
            if(contractId == null){

                customAssert.assertFalse(true,"Contract Id value is null");
                customAssert.assertAll();
                return;
            }else if(contractId.equals("")){
                customAssert.assertFalse(true,"Contract Id value is blank");
                customAssert.assertAll();
                return;
            }else {
                for(int i =1;i<=poToCreate;i++) {
                    uniqueString = DateUtils.getCurrentTimeStamp();
                    uniqueString = uniqueString.replaceAll("_", "");
                    uniqueString = uniqueString.replaceAll(" ", "");

                    uniqueString = uniqueString.substring(10);

                    UpdateFile.updateConfigFileProperty(poConfigFilePath, poExtraFieldsConfigFileName, flowToTest, "poNumber", "unqString", uniqueString);
                    UpdateFile.updateConfigFileProperty(poConfigFilePath, poExtraFieldsConfigFileName, flowToTest, "name", "unqString", uniqueString);
                    UpdateFile.updateConfigFileProperty(poConfigFilePath, poExtraFieldsConfigFileName, flowToTest, "poTotal", "pototalamount", poTotalList.get(i-1));

                    String createResponse = PurchaseOrder.createPurchaseOrder(poConfigFilePath, poConfigFileName, poConfigFilePath, poExtraFieldsConfigFileName, flowToTest,
                            true);
                    UpdateFile.updateConfigFileProperty(poConfigFilePath, poExtraFieldsConfigFileName, flowToTest, "poTotal",  poTotalList.get(i-1),"pototalamount");

                    if (ParseJsonResponse.validJsonResponse(createResponse)) {
                        JSONObject jsonObj = new JSONObject(createResponse);
                        String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();

                        UpdateFile.updateConfigFileProperty(poConfigFilePath, poExtraFieldsConfigFileName, flowToTest, "poNumber",  uniqueString,"unqString");
                        UpdateFile.updateConfigFileProperty(poConfigFilePath, poExtraFieldsConfigFileName, flowToTest, "name",  uniqueString,"unqString");

                        if (createStatus.trim().equalsIgnoreCase("success")) {
                            poId = CreateEntity.getNewEntityId(createResponse, "purchase orders");

                            if (poId == -1) {
                                logger.error("Unable to create purchase order for the flow " + flowToTest);
                                customAssert.assertTrue(false, "Unable to create purchase order for the flow " + flowToTest);
                                customAssert.assertAll();
                                return;
                            }else {
                                poIdList.add(poId);
                            }
                        } else {
                            logger.error("Unable to create purchase order for the flow " + flowToTest);
                            customAssert.assertTrue(false, "Unable to create purchase order for the flow " + flowToTest);
                            customAssert.assertAll();
                            return;
                        }
                    }
                }
                InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath, serviceDataConfigFileName, invoiceConfigFilePath,
                        invoiceConfigFileName, flowToTest, Integer.parseInt(contractId));
                for (int i =0;i<poIdList.size();i++){
                    UpdateFile.updateConfigFileProperty(serviceDataConfigFilePath,serviceDataExtraFieldsConfigFileName,flowToTest,"purchaseOrder","poId" + (i +1),String.valueOf(poIdList.get(i)));
                }

                int serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath, serviceDataConfigFileName, serviceDataExtraFieldsConfigFileName, flowToTest, uniqueString);

                if (serviceDataId == -1) {
                    logger.error("Unable to create Service Data");
                    customAssert.assertFalse(true, "Unable to create Service Data");
                } else {

                    //Pricing upload will happen during entity creation itself
                    boolean result = workflowActionsHelper.performWorkflowAction(serviceDataEntityTypeId, serviceDataId, publishAction);
                    if (result == false) {
                        logger.error("Error while publishing the service data");
                        customAssert.assertTrue(false, "Error while publishing the service data");
                        customAssert.assertAll();
                        return;
                    }
                    ArrayList<Integer> consumptionIds = new ArrayList<>();
                    if (flowToTest.equals("po burn scenario sd without po line item with po") ||
                            flowToTest.equals("po burn scenario sd with po line item without po")||
                            flowToTest.equals("po burn scenario multiple po")) {

                        invoiceHelper.waitForConsumptionToBeCreated(flowToTest, serviceDataId, consumptionIds);

                        if (consumptionIds.size() == 0) {
                            logger.error("Consumptions not created for service data id " + serviceDataId);
                            customAssert.assertTrue(false, "Consumptions not created for service data id " + serviceDataId);
                            customAssert.assertAll();
                            return;
                        } else {
                            Double finalConsumption = Double.parseDouble(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "finalconsumptionvalues"));
                            for (int i = 0; i < consumptionIds.size(); i++) {

                                InvoiceHelper.updateFinalConsumption(flowToTest, consumptionIds.get(i), finalConsumption);
                                Boolean consApprovalStatus = workflowActionsHelper.performWorkFlowStepV2(consumptionEntityTypeId, consumptionIds.get(i), approveCons);

                                if (!consApprovalStatus) {
                                    logger.error("Error while approving consumption");
                                    customAssert.assertTrue(false, "Error while approving consumption");
                                }
                            }
                        }
                    }

                    UpdateFile.updateConfigFileProperty(invoiceConfigFilePath, invoiceExtraFieldsConfigFileName, flowToTest, "purchaseOrder", "poId", String.valueOf(poId));
                    UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemExtraFieldsConfigFileName, flowToTest, "purchaseOrder", "poId", String.valueOf(poId));

                    int invoiceId = InvoiceHelper.getInvoiceId(invoiceConfigFilePath, invoiceConfigFileName, invoiceExtraFieldsConfigFileName,
                            invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, flowToTest);

                    if (invoiceId == -1) {
                        logger.error("Invoice Id not created");
                        UpdateFile.updateConfigFileProperty(invoiceConfigFilePath, invoiceExtraFieldsConfigFileName, flowToTest, "purchaseOrder",  String.valueOf(poId),"poId");
                        UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemExtraFieldsConfigFileName, flowToTest, "purchaseOrder",String.valueOf(poId), "poId");
                        customAssert.assertTrue(false, "Invoice Id not created");
                        customAssert.assertAll();
                        return;
                    } else {

                        int invLiId = InvoiceHelper.getInvoiceLineItemId(invoiceLineItemConfigFilePath, invoiceLineItemConfigFileName, invoiceLineItemExtraFieldsConfigFileName, flowToTest, serviceDataId);

                        UpdateFile.updateConfigFileProperty(invoiceConfigFilePath, invoiceExtraFieldsConfigFileName, flowToTest, "purchaseOrder",  String.valueOf(poId),"poId");
                        UpdateFile.updateConfigFileProperty(invoiceLineItemConfigFilePath, invoiceLineItemExtraFieldsConfigFileName, flowToTest, "purchaseOrder",String.valueOf(poId), "poId");

                        if (invLiId == -1) {
                            logger.error("Invoice Line Item Id not created corresponding to child service data 1");
                            customAssert.assertTrue(false, "Invoice Line Item Id not created corresponding to child service data 1");
                            customAssert.assertAll();
                            return;
                        } else {
                            String liValidationStatus = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "line item validation status");
                            invoiceHelper.verifyInvoiceLineItemValidationStatus(liValidationStatus, invLiId, customAssert);
                            if(flowToTest.equals("po burn scenario multiple po")){
                                Map<Integer,Map<String,String>> poFinanceFieldsMap = getPOFinFieldsMultPO(poIdList,customAssert);

                                workflowActionsHelper.performWorkFlowStepV2(invoiceEntityTypeId, invoiceId, approveInvoice, customAssert);
                                workflowActionsHelper.performWorkFlowStepV2(invoiceLineItemEntityTypeId, invLiId, approveLineItem, customAssert);

                                validatePOBurnMultiplePO(poIdList,invLiId,poFinanceFieldsMap,customAssert);
                                workflowActionsHelper.performWorkFlowStepV2(invoiceLineItemEntityTypeId, invLiId, archive, customAssert);
                                valPOUnBurnMultiplePO(flowToTest,poIdList,poFinanceFieldsMap,customAssert);

                            }else {
                                Map<String,String> poFinanceFieldsMap = getPOFinanceFields(poId,customAssert);
                                workflowActionsHelper.performWorkFlowStepV2(invoiceEntityTypeId, invoiceId, approveInvoice, customAssert);
                                workflowActionsHelper.performWorkFlowStepV2(invoiceLineItemEntityTypeId, invLiId, approveLineItem, customAssert);


                                valPOBurnSinglePO(flowToTest,invLiId,poId,poFinanceFieldsMap,customAssert);
                                workflowActionsHelper.performWorkFlowStepV2(invoiceLineItemEntityTypeId, invLiId, archive, customAssert);
                                valPOUnBurnSinglePO(flowToTest,poId,poFinanceFieldsMap,customAssert);
                            }

                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Exception while validating the scenario " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while validating the scenario " + e.getStackTrace());
        }
    }

    /*
    Expected PO Burn Value have the addition of previous value and supplier amount value at line item
    PO Burn Value have the addition of previous value and system amount value at line item
    PO Available Value have the subtraction of previous value and system amount value at line item
     */
    private void valPOBurnSinglePO(String flowToTest,int invLiItemId,int poId,Map<String,String> poFinanceFieldsMap,CustomAssert customAssert){

        Show show = new Show();
        try{
            show.hitShowVersion2(poEntityTypeId,poId);
            String poShowResp = show.getShowJsonStr();
            show.hitShowVersion2(invoiceLineItemEntityTypeId,invLiItemId);
            String liShowResp = show.getShowJsonStr();

            String poTotalNew = ShowHelper.getValueOfField("pototal",poShowResp);
            String expectedPoBurnNew = ShowHelper.getValueOfField("expectedpoburn",poShowResp);
            String poBurnNew = ShowHelper.getValueOfField("poburn",poShowResp);
            String poAvailNew = ShowHelper.getValueOfField("poavailable",poShowResp);

            String liSupplierAmt = ShowHelper.getValueOfField("amount",liShowResp);
            String liSystemAmt = ShowHelper.getValueOfField("systemamount",liShowResp);

            if(!poTotalNew.equals(poFinanceFieldsMap.get("poTotal"))){
                logger.error("While Validating PO Burn Scenario for the flow " + flowToTest + " Expected and actual value of PO Total not matched ");
                customAssert.assertTrue(false,"While Validating PO Burn Scenario for the flow " + flowToTest + " Expected and actual value of PO Total not matched ");
            }
            try {
                if (!expectedPoBurnNew.equals(String.valueOf(Double.parseDouble(poFinanceFieldsMap.get("expectedPoBurn")) + Double.parseDouble(liSupplierAmt)))) {
                    logger.error("While Validating PO Burn Scenario for the flow " + flowToTest + " Expected and actual value of expectedPoBurn not matched ");
                    customAssert.assertTrue(false,"While Validating PO Burn Scenario for the flow " + flowToTest + " Expected and actual value of expectedPoBurn not matched ");
                }
            }catch (Exception e){
                logger.error("Exception while validating PO expected PO Burn values");
                customAssert.assertTrue(false,"Exception while validating PO expected PO Burn values");
            }
            try {
                if (!poBurnNew.equals(String.valueOf(Double.parseDouble(poFinanceFieldsMap.get("poBurn")) + Double.parseDouble(liSystemAmt)))) {
                    logger.error("While Validating PO Burn Scenario for the flow " + flowToTest + " Expected and actual value of PoBurn not matched ");
                    customAssert.assertTrue(false,"While Validating PO Burn Scenario for the flow " + flowToTest + " Expected and actual value of PoBurn not matched ");
                }
            }catch (Exception e){
                logger.error("Exception while validating PO Burn values");
                customAssert.assertTrue(false,"Exception while validating PO Burn values");
            }

            try {
                if (!poAvailNew.equals(String.valueOf(Double.parseDouble(poFinanceFieldsMap.get("poAvail")) - Double.parseDouble(liSystemAmt)))) {
                    logger.error("While Validating PO Burn Scenario for the flow " + flowToTest + " Expected and actual value of PoAvailable not matched ");
                    customAssert.assertTrue(false,"While Validating PO Burn Scenario for the flow " + flowToTest + " Expected and actual value of PoAvailable not matched ");
                }
            }catch (Exception e){
                logger.error("Exception while validating PO Available values");
                customAssert.assertTrue(false,"Exception while validating PO Available values");
            }
//            String poAvail = ShowHelper.getValueOfField("",liShowResp);

        }catch (Exception e){
            logger.error("Exception while validating PO financial fields");
            customAssert.assertTrue(false,"Exception while validating PO financial fields");
        }

    }

    /*
    Values will be that of previous PO values
     */
    private void valPOUnBurnSinglePO(String flowToTest,int poId,Map<String,String> poFinanceFieldsMap,CustomAssert customAssert){

        Show show = new Show();
        try{
            show.hitShowVersion2(poEntityTypeId,poId);
            String poShowResp = show.getShowJsonStr();

            String poTotalNew = ShowHelper.getValueOfField("pototal",poShowResp);
            String expectedPoBurnNew = ShowHelper.getValueOfField("expectedpoburn",poShowResp);
            String poBurnNew = ShowHelper.getValueOfField("poburn",poShowResp);
            String poAvailNew = ShowHelper.getValueOfField("poavailable",poShowResp);

            if(!poTotalNew.equals(poFinanceFieldsMap.get("poTotal"))){
                logger.error("While Validating PO Burn Scenario for the flow " + flowToTest + " Expected and actual value of PO Total not matched ");
                customAssert.assertTrue(false,"While Validating PO Burn Scenario for the flow " + flowToTest + " Expected and actual value of PO Total not matched ");
            }
            try {
                if (!expectedPoBurnNew.equals(String.valueOf(Double.parseDouble(poFinanceFieldsMap.get("expectedPoBurn"))))) {
                    logger.error("While Validating PO Burn Scenario for the flow " + flowToTest + " Expected and actual value of expectedPoBurn not matched ");
                    customAssert.assertTrue(false,"While Validating PO Burn Scenario for the flow " + flowToTest + " Expected and actual value of expectedPoBurn not matched ");
                }
            }catch (Exception e){
                logger.error("Exception while validating PO expected PO Burn values");
                customAssert.assertTrue(false,"Exception while validating PO expected PO Burn values");
            }
            try {
                if (!poBurnNew.equals(String.valueOf(Double.parseDouble(poFinanceFieldsMap.get("poBurn"))))) {
                    logger.error("While Validating PO Burn Scenario for the flow " + flowToTest + " Expected and actual value of PoBurn not matched ");
                    customAssert.assertTrue(false,"While Validating PO Burn Scenario for the flow " + flowToTest + " Expected and actual value of PoBurn not matched ");
                }
            }catch (Exception e){
                logger.error("Exception while validating PO Burn values");
                customAssert.assertTrue(false,"Exception while validating PO Burn values");
            }
            try {
                if (!poAvailNew.equals(String.valueOf(Double.parseDouble(poFinanceFieldsMap.get("poAvail"))))) {
                    logger.error("While Validating PO Burn Scenario for the flow " + flowToTest + " Expected and actual value of PoAvailable not matched ");
                    customAssert.assertTrue(false,"While Validating PO Burn Scenario for the flow " + flowToTest + " Expected and actual value of PoAvailable not matched ");
                }
            }catch (Exception e){
                logger.error("Exception while validating PO Available values");
                customAssert.assertTrue(false,"Exception while validating PO Available values");
            }

        }catch (Exception e){
            logger.error("Exception while validating PO financial fields");
            customAssert.assertTrue(false,"Exception while validating PO financial fields");
        }

    }

    /*
    Values will be that of previous PO values
     */
    private void valPOUnBurnMultiplePO(String flowToTest,ArrayList<Integer> poList,Map<Integer,Map<String,String>> poFinanceFieldsMap,CustomAssert customAssert){

        Show show = new Show();
        try{
            for(Integer poId : poList) {
                show.hitShowVersion2(poEntityTypeId, poId);
                String poShowResp = show.getShowJsonStr();

                String poTotalNew = ShowHelper.getValueOfField("pototal", poShowResp);
                String expectedPoBurnNew = ShowHelper.getValueOfField("expectedpoburn", poShowResp);
                String poBurnNew = ShowHelper.getValueOfField("poburn", poShowResp);
                String poAvailNew = ShowHelper.getValueOfField("poavailable", poShowResp);

                if (!poTotalNew.equals(poFinanceFieldsMap.get("poTotal"))) {
                    logger.error("While Validating PO Burn Scenario for the flow " + flowToTest + " Expected and actual value of PO Total not matched during Un Burn Scenario");
                    customAssert.assertTrue(false, "While Validating PO Burn Scenario for the flow " + flowToTest + " Expected and actual value of PO Total not matched during Un Burn Scenario");
                }
                try {
                    if (!expectedPoBurnNew.equals(String.valueOf(Double.parseDouble(poFinanceFieldsMap.get(poId).get("expectedPoBurn"))))) {
                        logger.error("While Validating PO Burn Scenario for the flow " + flowToTest + " Expected and actual value of expectedPoBurn not matched ");
                        customAssert.assertTrue(false, "While Validating PO Burn Scenario for the flow " + flowToTest + " Expected and actual value of expectedPoBurn not matched ");
                    }
                } catch (Exception e) {
                    logger.error("Exception while validating PO expected PO Burn values during Un Burn Scenario");
                    customAssert.assertTrue(false, "Exception while validating PO expected PO UnBurn values during Un Burn Scenario");
                }
                try {
                    if (!poBurnNew.equals(String.valueOf(Double.parseDouble(poFinanceFieldsMap.get(poId).get("poBurn"))))) {
                        logger.error("While Validating PO Burn Scenario for the flow " + flowToTest + " Expected and actual value of PoBurn not matched during Un Burn Scenario");
                        customAssert.assertTrue(false, "While Validating PO Burn Scenario for the flow " + flowToTest + " Expected and actual value of PoBurn not matched during Un Burn Scenario");
                    }
                } catch (Exception e) {
                    logger.error("Exception while validating PO Burn values during Un Burn Scenario");
                    customAssert.assertTrue(false, "Exception while validating PO Burn values during Un Burn Scenario");
                }
                try {
                    if (!poAvailNew.equals(String.valueOf(Double.parseDouble(poFinanceFieldsMap.get(poId).get("poAvail"))))) {
                        logger.error("While Validating PO Burn Scenario for the flow " + flowToTest + " Expected and actual value of PoAvailable not matched during Un Burn Scenario");
                        customAssert.assertTrue(false, "While Validating PO Burn Scenario for the flow " + flowToTest + " Expected and actual value of PoAvailable not matched during Un Burn Scenario");
                    }
                } catch (Exception e) {
                    logger.error("Exception while validating PO Available values during Un Burn Scenario");
                    customAssert.assertTrue(false, "Exception while validating PO Available values during Un Burn Scenario");
                }

            }
        }catch (Exception e){
            logger.error("Exception while validating PO financial fields");
            customAssert.assertTrue(false,"Exception while validating PO financial fields");
        }

    }

    private Map<String,String> getPOFinanceFields(int poId, CustomAssert customAssert){

        Map<String,String> poFinanceFieldsMap = new HashMap<>();
        Show show = new Show();
        try{
            show.hitShowVersion2(poEntityTypeId,poId);
            String poShowResp = show.getShowJsonStr();

            String poTotal = ShowHelper.getValueOfField("pototal",poShowResp);
            String expectedPoBurn = ShowHelper.getValueOfField("expectedpoburn",poShowResp);
            String poBurn = ShowHelper.getValueOfField("poburn",poShowResp);
            String poAvail = ShowHelper.getValueOfField("poavailable",poShowResp);

            poFinanceFieldsMap.put("poTotal",poTotal);
            poFinanceFieldsMap.put("expectedPoBurn",expectedPoBurn);
            poFinanceFieldsMap.put("poBurn",poBurn);
            poFinanceFieldsMap.put("poAvail",poAvail);

        }catch (Exception e){
            logger.error("Exception while getting PO financial fields");
            customAssert.assertTrue(false,"Exception while getting PO financial fields");
        }
        return poFinanceFieldsMap;
    }

    private Map<Integer,Map<String,String>> getPOFinFieldsMultPO(ArrayList<Integer> poList, CustomAssert customAssert){

        Map<Integer,Map<String,String>> poFinFieldsMultPO = new HashMap<>();
        Show show = new Show();
        try{

            for(Integer poId : poList) {
                Map<String,String> poFinanceFieldsMap = new HashMap<>();

                show.hitShowVersion2(poEntityTypeId, poId);
                String poShowResp = show.getShowJsonStr();

                String poTotal = ShowHelper.getValueOfField("pototal", poShowResp);
                String expectedPoBurn = ShowHelper.getValueOfField("expectedpoburn", poShowResp);
                String poBurn = ShowHelper.getValueOfField("poburn", poShowResp);
                String poAvail = ShowHelper.getValueOfField("poavailable", poShowResp);

                poFinanceFieldsMap.put("poTotal", poTotal);
                poFinanceFieldsMap.put("expectedPoBurn", expectedPoBurn);
                poFinanceFieldsMap.put("poBurn", poBurn);
                poFinanceFieldsMap.put("poAvail", poAvail);

                poFinFieldsMultPO.put(poId,poFinanceFieldsMap);
            }
        }catch (Exception e){
            logger.error("Exception while getting PO financial fields");
            customAssert.assertTrue(false,"Exception while getting PO financial fields");
        }
        return poFinFieldsMultPO;
    }

    private void validatePOBurnMultiplePO(ArrayList<Integer> poList,int invLineItemId,Map<Integer,Map<String,String>> poFinanceFieldsMap,CustomAssert customAssert){

        try{
            Show show = new Show();
            Double poAdjAmount = 0.0;
            String poAvalAmountExp = "";

            String poTotalAmountExp = "";
            String poExpectedAmtBurnExp = "";
            String poBurnExp = "";
            int currentActivePO = 0;

            //PO Burn Order is in the order of PO in the polist
            for(Integer poId : poList) {

                show.hitShowVersion2(invoiceLineItemEntityTypeId, invLineItemId);
                String liShowResp = show.getShowJsonStr();

                String liSupplierAmt = ShowHelper.getValueOfField("amount", liShowResp);
                String liSystemAmt = ShowHelper.getValueOfField("systemamount", liShowResp);
                Double liSystemAmtD = 0.0;
                if (liSupplierAmt == null || liSupplierAmt.equals("null") || liSupplierAmt.equals("")) {
                    logger.error("On Invoice Line Item value of system amount is blank ");
                    customAssert.assertTrue(false, "On Invoice Line Item value of system amount is blank ");
                } else {
                    liSystemAmtD = Double.parseDouble(liSystemAmt);
                }
                if ((poAdjAmount < liSystemAmtD) && (currentActivePO < poList.size() - 1)) {

                    poAdjAmount = Double.parseDouble(poFinanceFieldsMap.get(poId).get("poAvail"));
                    poTotalAmountExp = String.valueOf(Double.parseDouble(poFinanceFieldsMap.get(poId).get("poTotal")));
                    poExpectedAmtBurnExp = String.valueOf(Double.parseDouble(poFinanceFieldsMap.get(poId).get("expectedPoBurn")) + poAdjAmount);
                    poBurnExp = String.valueOf(Double.parseDouble(poFinanceFieldsMap.get(poId).get("poBurn")) + poAdjAmount);
                    poAvalAmountExp = "0.0";
                    poAdjAmount = liSystemAmtD - Double.parseDouble(poFinanceFieldsMap.get(poId).get("poAvail"));

                } else if ((poAdjAmount < liSystemAmtD) && (currentActivePO == poList.size() - 1)) {

                    poTotalAmountExp = String.valueOf(Double.parseDouble(poFinanceFieldsMap.get(poId).get("poTotal")));
                    poExpectedAmtBurnExp = String.valueOf(Double.parseDouble(liSupplierAmt) -Double.parseDouble(poFinanceFieldsMap.get(poId).get("poAvail")));
                    poBurnExp = String.valueOf(Double.parseDouble(poFinanceFieldsMap.get(poId).get("poBurn")) + poAdjAmount);

                    poAvalAmountExp = String.valueOf(Double.parseDouble(poFinanceFieldsMap.get(poId).get("poAvail")) - poAdjAmount);
                }

                Map<String,String> poActualValuesMap = getPOFinanceFields(poId,customAssert);

                if(!poActualValuesMap.get("poTotal").equals(poTotalAmountExp)){
                    logger.error("For current Active PO " + currentActivePO + " PO ID " + poId +" value of PO Total not as expected Expected PO Total : " + poTotalAmountExp + " Actual PO Total " + poActualValuesMap.get("poTotal"));
                    customAssert.assertTrue(false,"For current Active PO " + currentActivePO + " PO ID " + poId +" value of PO Total not as expected Expected PO Total : " + poTotalAmountExp + " Actual PO Total " + poActualValuesMap.get("poTotal"));
                }
                if(!poActualValuesMap.get("expectedPoBurn").equals(poExpectedAmtBurnExp)){
                    logger.error("For current Active PO " + currentActivePO + " PO ID " + poId +" value of PO Expected Amt Burn not as expected Expected PO Expected Amt Burn : " + poExpectedAmtBurnExp + " Actual PO Total " + poActualValuesMap.get("expectedPoBurn"));
                    customAssert.assertTrue(false,"For current Active PO " + currentActivePO + " PO ID " + poId +" value of PO Expected Amt Burn not as expected Expected PO Expected Amt Burn : " + poExpectedAmtBurnExp + " Actual PO Total " + poActualValuesMap.get("expectedPoBurn"));
                }
                if(!poActualValuesMap.get("poBurn").equals(poBurnExp)){
                    logger.error("For current Active PO " + currentActivePO + " PO ID " + poId +" value of PO Burn not as expected Expected PO Burn : " + poBurnExp + " Actual PO Total " + poActualValuesMap.get("poBurnExp"));
                    customAssert.assertTrue(false,"For current Active PO " + currentActivePO + " PO ID " + poId +" value of PO Burn not as expected Expected PO Burn : " + poBurnExp + " Actual PO Total " + poActualValuesMap.get("poBurnExp"));
                }
                if(!poActualValuesMap.get("poAvail").equals(poAvalAmountExp)){
                    logger.error("For current Active PO " + currentActivePO + " PO ID " + poId +" value of PO Avail Amount not as expected Expected PO Avail Amount : " + poAvalAmountExp + " Actual PO Avail Amount " + poActualValuesMap.get("poAvail"));
                    customAssert.assertTrue(false,"For current Active PO " + currentActivePO + " PO ID " + poId +" value of PO Avail Amount not as expected Expected PO Avail Amount : " + poAvalAmountExp + " Actual PO Avail Amount " + poActualValuesMap.get("poAvail"));
                }
                currentActivePO++;
            }

        }catch (Exception e){
            logger.error("Exception while validating PO Burn for multiple PO ");
            customAssert.assertTrue(false,"Exception while validating PO Burn for multiple PO ");

        }
    }

}
