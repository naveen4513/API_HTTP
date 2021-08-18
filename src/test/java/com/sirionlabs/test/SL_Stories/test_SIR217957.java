package com.sirionlabs.test.SL_Stories;

import com.sirionlabs.api.clientAdmin.workflow.WorkFlowCreate;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Clone;
import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.servicelevel.ServiceLevelHelper;

import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.DateUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class test_SIR217957 {

    private final static Logger logger = LoggerFactory.getLogger(test_SIR217957.class);

    private String configFilePath;
    private String configFileName;

    private String slConfigFilePath;
    private String slConfigFileName;

    private String auditLogUser;
    private String adminUser;

    private int slEntityTypeId;
    private int cslEntityTypeId;

    private ArrayList<Integer> slToDelete = new ArrayList<>();
    private ArrayList<Integer> cslToDelete = new ArrayList<>();

    private WorkFlowCreate workFlowCreate = new WorkFlowCreate();
    private String workflowFilePath;
    private String workflowFileName;
    private String relationId;

    @BeforeClass
    public void BeforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("testSIR217957ConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("testSIR217957ConfigFileName");

        slConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFilePath");
        slConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFileName");

        slEntityTypeId = ConfigureConstantFields.getEntityIdByName("service levels");
        cslEntityTypeId = ConfigureConstantFields.getEntityIdByName("child service levels");

        auditLogUser = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "createdbyuser");
        adminUser = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "adminuser");

        String workflowName = "CSL Workflow " + DateUtils.getCurrentTimeStamp();

        workflowFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"baseworkflowfilepath");
        workflowFileName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"baseworkflowfilename");
        relationId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"supplierid");

        workFlowCreate.hitWorkflowCreate(workflowName,relationId,String.valueOf(cslEntityTypeId),workflowFilePath,workflowFileName);

    }

    //This test needs to be run in serial fashion always as it uploads new workflow every time
    @Test
    public void AllowValueUpdatesForNestedObjectsEntityPages(){

        CustomAssert customAssert = new CustomAssert();

        ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();

        String flowToTest = "sl automation flow";
        ArrayList<String> childServiceLevelIds;

        Show show = new Show();
        String showResponse;

        //Adding performanceComputationCalculationQuery hardcoded as unable to read from cfg file properly
        String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['Total Business Elapsed Time (Days)'].value < 20){if(doc['exception'].value== false){params._agg.map.met++}else{params._agg.map.notMet++}}else{if(doc['exception'].value== false){params._agg.map.notMet++}else{params._agg.map.met++}}}} if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['exception'].value== false){params._agg.map.credit += doc['Applicable Credit'].value}}} \", \"init_script\": \"params._agg['map'] =['met': 0.0, 'notMet': 0, 'credit':0]\", \"reduce_script\": \"params.return_map = ['Final_Numerator':0.0, 'Final_Denominator':0, 'Final_Performance':0 ,'Actual_Numerator':0.0, 'Actual_Denominator':0, 'Actual_Performance':0 ,'SL_Met':0 ,     'Calculated_Credit_Amount':0.0]; for (a in params._aggs){params.return_map.Final_Numerator += (float)(a.map.met); params.return_map.Final_Denominator += (float)(a.map.met + a.map.notMet) ; params.return_map.Calculated_Credit_Amount += (float)(a.map.credit);}  if(params.return_map.Final_Denominator > 0){params.return_map.Final_Performance = Math.round(((float)(params.return_map.Final_Numerator*100)/params.return_map.Final_Denominator)*100.0)/100.0}else{params.return_map.Final_Performance ='';params.return_map.SL_Met =5} params.return_map.Final_Numerator = Math.round(params.return_map.Final_Numerator *100.0)/100.0 ;params.return_map.Actual_Numerator = params.return_map.Final_Numerator; params.return_map.Actual_Denominator = params.return_map.Final_Denominator; params.return_map.Actual_Performance = params.return_map.Final_Performance; if(params.return_map.Final_Denominator > 0){if(params.return_map.Final_Performance < 90){params.return_map.Calculated_Credit_Amount = params.return_map.Calculated_Credit_Amount} else{params.return_map.Calculated_Credit_Amount = ''}} else{params.return_map.Calculated_Credit_Amount = ''}  return params.return_map\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}, {\"match\": {\"useInComputation\": true}}]}}}";
        String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";

        int serviceLevelId;
        int cSLId;
        List<String> workFlowSteps;

        String calculatedCreditAmountActual;
        String finalCreditAmountActual;
        String creditAmountPaidActual;
        String creditAmountBalanceActual;
        String earnBackValueActual;
        String calculatedEarnBackAmountActual;
        String finalEarnBackAmountActual;
        String earnBackAmountPaidActual;
        String earnBackAmountBalanceActual;

        String calculatedCreditAmountExpectedQuery;
        String finalCreditAmountExpectedQuery;
        String creditAmountPaidExpectedQuery;
        String creditAmountBalanceExpectedQuery;
        String earnBackValueExpectedQuery;
        String calculatedEarnBackAmountExpectedQuery;
        String finalEarnBackAmountExpectedQuery;
        String earnBackAmountPaidExpectedQuery;
        String earnBackAmountBalanceExpectedQuery;

        String calculatedCreditAmountExpected;
        String finalCreditAmountExpected;
        String creditAmountPaidExpected;
        String creditAmountBalanceExpected;
        String earnBackValueExpected;
        String calculatedEarnBackAmountExpected;
        String finalEarnBackAmountExpected;
        String earnBackAmountPaidExpected;
        String earnBackAmountBalanceExpected;

        try {

            String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "performancedataformatfilename");
            String rawDataFileName = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "rawdatafilename");
            String expectedMsg = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "performancedatuploadsuccessmsg");
            String uploadFilePath = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "uploadfilepath");
            String rawDataFileValidMsg = ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "rawdatafilesuccessmsg");

            int uploadIdSL_PerformanceDataTab = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "uploadidslperformancecdatatab"));
            int slMetaDataUploadTemplateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "slmetadatauploadtemplateid"));

            String workflowName = "CSL Workflow " + DateUtils.getCurrentTimeStamp() + RandomNumbers.getRandomNumberWithinRange(1,10);

            WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
            Boolean workFlowStatus;

            String workFlowActionRecomputePerformance = "ReComputePerformance";

            for(int i = 1;i<6;i++) {
                try {


                    workflowFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "storyspecificworkflowsfilepath");
                    workflowFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "storyspecificworkflowsfilename");
                    relationId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "supplierid");

                    workflowFileName = workflowFileName.replace("{updatevalue}",String.valueOf(i));
                    workflowName = "CSL Workflow " + DateUtils.getCurrentTimeStamp() + RandomNumbers.getRandomNumberWithinRange(1,10);

                    workFlowCreate.hitWorkflowCreate(workflowName, relationId, String.valueOf(cslEntityTypeId), workflowFilePath, workflowFileName);

                    Thread.sleep(10000);
                    serviceLevelId = serviceLevelHelper.getServiceLevelId(flowToTest, PCQ, DCQ, customAssert);

                    if (serviceLevelId != -1) {
                        slToDelete.add(serviceLevelId);

                        workFlowSteps = Arrays.asList(ParseConfigFile.getValueFromConfigFile(slConfigFilePath, slConfigFileName, "slactiveworkflowsteps").split("->"));
                        //Performing workflow Actions till Active

                        if (!serviceLevelHelper.performWorkFlowActions(slEntityTypeId, serviceLevelId, workFlowSteps, auditLogUser, customAssert)) {
                            customAssert.assertTrue(false, "Error while performing workflow actions on SL ID " + serviceLevelId);
                            customAssert.assertAll();
                        }

                        if (!serviceLevelHelper.uploadPerformanceDataFormat(serviceLevelId, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
                            customAssert.assertTrue(false, "Error while performance data file upload");
                            customAssert.assertAll();
                        }

                        childServiceLevelIds = serviceLevelHelper.checkIfCSLCreatedOnServiceLevel(serviceLevelId, customAssert);

                        addCSLToDelete(childServiceLevelIds);

                        int numberOfChildServiceLevel = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "numberofchildservicelevel"));

                        if (childServiceLevelIds.size() != numberOfChildServiceLevel) {

                            customAssert.assertTrue(false, "For Service Level Id " + serviceLevelId + " Number of Child Service Level Expected are " + numberOfChildServiceLevel + " Actual " + childServiceLevelIds.size());
                        }
                        cSLId = Integer.parseInt(childServiceLevelIds.get(0));

                        if (!serviceLevelHelper.uploadRawDataCSL(cSLId, uploadFilePath, rawDataFileName, rawDataFileValidMsg, customAssert)) {

                            customAssert.assertTrue(false, "Raw Data Uploaded unsuccessfully on CSL " + cSLId);
                            customAssert.assertAll();
                        }


                        workFlowStatus = workflowActionsHelper.performWorkflowAction(cslEntityTypeId, cSLId, workFlowActionRecomputePerformance);
                        if (!workFlowStatus) {
                            customAssert.assertTrue(false, "Unable to perform " + workFlowActionRecomputePerformance + " on CSL Id " + cSLId);
                        }

                        try {

                            show.hitShow(cslEntityTypeId,cSLId);
                            showResponse = show.getShowJsonStr();

                            calculatedCreditAmountActual = ShowHelper.getValueOfField("calculatedcreditamount", showResponse);
                            finalCreditAmountActual = ShowHelper.getValueOfField("finalcreditamount", showResponse);
                            creditAmountPaidActual = ShowHelper.getValueOfField("creditamountpaid", showResponse);
                            creditAmountBalanceActual = ShowHelper.getValueOfField("creditamountbalance", showResponse);
                            earnBackValueActual = ShowHelper.getValueOfField("earnbackvalue", showResponse);
                            calculatedEarnBackAmountActual = ShowHelper.getValueOfField("calculatedearnbackamount", showResponse);
                            finalEarnBackAmountActual = ShowHelper.getValueOfField("finalearnbackamount", showResponse);
                            earnBackAmountPaidActual = ShowHelper.getValueOfField("earnbackamountpaid", showResponse);
                            earnBackAmountBalanceActual = ShowHelper.getValueOfField("earnbackamountbalance", showResponse);

                            if(calculatedCreditAmountActual == null){
                                calculatedCreditAmountActual = "null";
                            }

                            if(finalCreditAmountActual == null){
                                finalCreditAmountActual = "null";
                            }
                            if(creditAmountPaidActual == null){
                                creditAmountPaidActual = "null";
                            }
                            if(calculatedEarnBackAmountActual == null){
                                calculatedEarnBackAmountActual = "null";
                            }
                            if(finalEarnBackAmountActual == null){
                                finalEarnBackAmountActual = "null";
                            }
                            if(earnBackAmountPaidActual == null){
                                earnBackAmountPaidActual = "null";
                            }

                            calculatedCreditAmountExpectedQuery = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "workflow" + i, "calculatedcreditamount");
                            calculatedCreditAmountExpected = calculateField(calculatedCreditAmountExpectedQuery, showResponse);

                            if (!calculatedCreditAmountActual.equalsIgnoreCase(calculatedCreditAmountExpected)) {       //To check
                                customAssert.assertTrue(false, "Expected and Actual value for Calculated Credit Amount mismatched for workflow " + i);
                            }

                            finalCreditAmountExpectedQuery = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "workflow" + i, "finalcreditamount");
                            finalCreditAmountExpected = calculateField(finalCreditAmountExpectedQuery, showResponse);

                            if (!finalCreditAmountActual.equalsIgnoreCase(finalCreditAmountExpected)) {
                                customAssert.assertTrue(false, "Expected and Actual value for Final Credit Amount Expected mismatched for workflow " + i);
                            }

                            creditAmountPaidExpectedQuery = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "workflow" + i, "creditamountpaid");
                            creditAmountPaidExpected = calculateField(creditAmountPaidExpectedQuery, showResponse);

                            if (!creditAmountPaidActual.equalsIgnoreCase(creditAmountPaidExpected)) {       //To check
                                customAssert.assertTrue(false, "Expected and Actual value for Credit Amount Paid mismatched for workflow " + i);
                            }

                            //creditAmountBalanceExpectedQuery = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"workflow" + i,"creditamountbalance");
                            //creditAmountBalanceExpected = calculateField(creditAmountPaidExpectedQuery,showResponse);

                            //earnBackValueExpectedQuery = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"workflow" + i,"earnbackvalue");
                            //earnBackValueExpected = calculateField(creditAmountPaidExpectedQuery,showResponse);

                            calculatedEarnBackAmountExpectedQuery = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "workflow" + i, "calculatedearnbackamount");
                            calculatedEarnBackAmountExpected = calculateField(calculatedEarnBackAmountExpectedQuery, showResponse);

                            if (!calculatedEarnBackAmountActual.equalsIgnoreCase(calculatedEarnBackAmountExpected)) {       //To check
                                customAssert.assertTrue(false, "Expected and Actual value for Calculated EarnBack Amount mismatched for workflow " + i);
                            }

                            finalEarnBackAmountExpectedQuery = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "workflow" + i, "finalearnbackamount");
                            finalEarnBackAmountExpected = calculateField(finalEarnBackAmountExpectedQuery, showResponse);

                            if (!finalEarnBackAmountActual.equalsIgnoreCase(finalEarnBackAmountExpected)) {     //To check
                                customAssert.assertTrue(false, "Expected and Actual value for Final Earn Back Amount mismatched for workflow " + i);
                            }

                            earnBackAmountPaidExpectedQuery = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "workflow" + i, "earnbackamountpaid");
                            earnBackAmountPaidExpected = calculateField(earnBackAmountPaidExpectedQuery, showResponse);


                            if (!earnBackAmountPaidActual.equalsIgnoreCase(earnBackAmountPaidExpected)) {       //To check
                                customAssert.assertTrue(false, "Expected and Actual value for Earn Back Amount Paid mismatched for workflow " + i);
                            }

                            //earnBackAmountBalanceExpectedQuery = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"workflow" + i,"earnbackamountbalance");
                            //earnBackAmountBalanceExpected = calculateField(earnBackAmountBalanceExpectedQuery,showResponse);

                        }catch (Exception e){
                            logger.error(e.getMessage());

                        }


                    }
                }catch (Exception e){
                    customAssert.assertTrue(false,"Exception while validating for the flow " + i + e.getMessage());
                }
            }

        } catch (Exception e) {
            logger.error("Exception while performing SL Computation ");
            customAssert.assertTrue(false, "Exception while performing SL Computation" + e.getMessage());
        }

        customAssert.assertAll();
    }

    @AfterClass
    public void afterClass() {

        try {
            logger.debug("Number CSL To Delete " + cslToDelete.size());
            EntityOperationsHelper.deleteMultipleRecords("child service levels", cslToDelete);

            logger.debug("Number SL To Delete " + slToDelete.size());
            EntityOperationsHelper.deleteMultipleRecords("service levels", slToDelete);
        }catch (Exception e){
            logger.error("Exception while deleting entities");
        }finally {

            String workflowName = "CSL Workflow " + DateUtils.getCurrentTimeStamp() + RandomNumbers.getRandomNumberWithinRange(1,10);

            workflowFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"baseworkflowfilepath");
            workflowFileName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"baseworkflowfilename");
            relationId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"supplierid");

            workFlowCreate.hitWorkflowCreate(workflowName,relationId,String.valueOf(cslEntityTypeId),workflowFilePath,workflowFileName);

        }

    }

    private void addCSLToDelete(ArrayList<String> cslToDeleteList){

        try {
            for (String cslIDToDelete : cslToDeleteList) {
                cslToDelete.add(Integer.parseInt(cslIDToDelete));
            }
        }catch (Exception e){
            logger.error("Error while adding child service level to deleted list");
        }
    }

    private String calculateField(String query,String showResponse){

        String calculatedValue = "null";
        String operator;
        String []operands;

        try {
            String[] querySteps = query.split("->");

            if (querySteps[0].equalsIgnoreCase("noncalculate")) {
                if (querySteps[1].equalsIgnoreCase("fromshowpagefield")) {

                    calculatedValue = ShowHelper.getValueOfField(querySteps[2], showResponse);
                }else if(querySteps[1].equalsIgnoreCase("hardcode")) {
                    calculatedValue = querySteps[2];
                }

            }else if(querySteps[0].equalsIgnoreCase("calculate")) {

                operands = querySteps[1].split(":calculationtype:");
                operator = querySteps[2];

                if ((operands[0].equalsIgnoreCase("0")|| operands[1].equalsIgnoreCase("0")) && (operator.equalsIgnoreCase("*"))) {
                    calculatedValue = "0.0";
                } else if ((operands[0].equalsIgnoreCase("0")|| operands[1].equalsIgnoreCase("0")) && (operator.equalsIgnoreCase("/"))) {
                    calculatedValue = "null";
                }else {
                    if(isNumeric(operands[0])&& isNumeric(operands[1])){

                        calculatedValue = String.valueOf(doOperation(Double.parseDouble(operands[0]),Double.parseDouble(operands[0]),operator));
                    }else if(isNumeric(operands[0])){
                        String operandValue =  ShowHelper.getValueOfField(operands[1],showResponse);

                        if(operandValue == null){
                            return "null";
                        }
                        calculatedValue = String.valueOf(doOperation(Double.parseDouble(operands[0]),Double.parseDouble(operandValue),operator));
                    }else if(isNumeric(operands[1])){
                        String operandValue =  ShowHelper.getValueOfField(operands[0],showResponse);

                        if(operandValue == null){
                            return "null";
                        }
                        calculatedValue = String.valueOf(doOperation(Double.parseDouble(operandValue),Double.parseDouble(operands[1]),operator));

                    }else {
                        String operandValue1 =  ShowHelper.getValueOfField(operands[0],showResponse);
                        String operandValue2 =  ShowHelper.getValueOfField(operands[1],showResponse);

                        if(operandValue1 == null){
                            return "null";
                        }

                        if(operandValue2 == null){
                            return "null";
                        }
                        calculatedValue = String.valueOf(doOperation(Double.parseDouble(operandValue1),Double.parseDouble(operandValue2),operator));
                    }
                }

            }
        }catch (Exception e){
            logger.error("Exception while getting expected field");
            calculatedValue = null;
        }
        return calculatedValue;
    }

    private boolean isNumeric(String field){

        Boolean isNumeric = true;
        try {

            Double.parseDouble(field);
        }catch (NumberFormatException nfe){
            isNumeric = false;
        }
        return isNumeric;
    }

    private double doOperation(Double first, Double second, String op ){

        switch(op){
            case "+":
                return first + second;
            case "-":
                return first - second;
            case "*":
                return first * second;
            case "/":
                return first / second;
            default:
                return 0;
        }
    }
}
