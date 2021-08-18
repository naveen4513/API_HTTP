package com.sirionlabs.test.SL;

import com.sirionlabs.api.commonAPI.Delete;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.servicelevel.ServiceLevelHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Test_RC1_36_Bugs {

    private final static Logger logger = LoggerFactory.getLogger(Test_RC1_36_Bugs.class);

    private String configFilePath;
    private String configFileName;
    private String uploadFilePath;
    private String auditLogUser;
    private String adminUser;
    private String rawDataFileValidMsg;

    private int slEntityTypeId;
    private int cslEntityTypeId;

    private ArrayList<Integer> slToDelete = new ArrayList<>();

    @BeforeClass
    public void BeforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFileName");
        slEntityTypeId = ConfigureConstantFields.getEntityIdByName("service levels");
        cslEntityTypeId = ConfigureConstantFields.getEntityIdByName("child service levels");

        auditLogUser = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "createdbyuser");
        adminUser = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "adminuser");
        uploadFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "uploadfilepath");

        rawDataFileValidMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilesuccessmsg");
    }

    //This test case is disabled as of now as needs to update the test after Akhil comes
    @Test(enabled = false)
    public void Test_SIR_218518_BulkUploadRawDataCSLParentHasDifferentMetaDataTemplate(){

        CustomAssert customAssert = new CustomAssert();
        ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();

        String flowToTest = "sl automation flow";
        ArrayList<String> childServiceLevelIds;

        //Adding performanceComputationCalculationQuery hardcoded as unable to read from cfg file properly
        String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['Total Business Elapsed Time (Days)'].value < 20){if(doc['exception'].value== false){params._agg.map.met++}else{params._agg.map.notMet++}}else{if(doc['exception'].value== false){params._agg.map.notMet++}else{params._agg.map.met++}}}} if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['exception'].value== false){params._agg.map.credit += doc['Applicable Credit'].value}}} \", \"init_script\": \"params._agg['map'] =['met': 0.0, 'notMet': 0, 'credit':0]\", \"reduce_script\": \"params.return_map = ['Final_Numerator':0.0, 'Final_Denominator':0, 'Final_Performance':0 ,'Actual_Numerator':0.0, 'Actual_Denominator':0, 'Actual_Performance':0 ,'SL_Met':0 ,     'Calculated_Credit_Amount':0.0]; for (a in params._aggs){params.return_map.Final_Numerator += (float)(a.map.met); params.return_map.Final_Denominator += (float)(a.map.met + a.map.notMet) ; params.return_map.Calculated_Credit_Amount += (float)(a.map.credit);}  if(params.return_map.Final_Denominator > 0){params.return_map.Final_Performance = Math.round(((float)(params.return_map.Final_Numerator*100)/params.return_map.Final_Denominator)*100.0)/100.0}else{params.return_map.Final_Performance ='';params.return_map.SL_Met =5} params.return_map.Final_Numerator = Math.round(params.return_map.Final_Numerator *100.0)/100.0 ;params.return_map.Actual_Numerator = params.return_map.Final_Numerator; params.return_map.Actual_Denominator = params.return_map.Final_Denominator; params.return_map.Actual_Performance = params.return_map.Final_Performance; if(params.return_map.Final_Denominator > 0){if(params.return_map.Final_Performance < 90){params.return_map.Calculated_Credit_Amount = params.return_map.Calculated_Credit_Amount} else{params.return_map.Calculated_Credit_Amount = ''}} else{params.return_map.Calculated_Credit_Amount = ''}  return params.return_map\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}, {\"match\": {\"useInComputation\": true}}]}}}";
        String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";

        try {
            int serviceLevelId = serviceLevelHelper.getServiceLevelId(flowToTest, PCQ, DCQ, customAssert);
            slToDelete.add(serviceLevelId);

            if (serviceLevelId != -1) {

                List<String> workFlowSteps = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "slactiveworkflowsteps").split("->"));
                //Performing workflow Actions till Active

                if (!serviceLevelHelper.performWorkFlowActions(slEntityTypeId, serviceLevelId, workFlowSteps, auditLogUser, customAssert)) {
                    customAssert.assertTrue(false, "Error while performing workflow actions on SL ID " + serviceLevelId);
                    customAssert.assertAll();
                }

                childServiceLevelIds = serviceLevelHelper.checkIfCSLCreatedOnServiceLevel(serviceLevelId, customAssert);

                int numberOfChildServiceLevel = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "numberofchildservicelevel"));

                if (childServiceLevelIds.size() != numberOfChildServiceLevel) {

                    customAssert.assertTrue(false, "For Service Level Id " + serviceLevelId + " Number of Child Service Level Expected are " + numberOfChildServiceLevel + " Actual " + childServiceLevelIds.size());
                }

                String performanceDataFormatFileName1 = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilename");
                String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");


                int uploadIdSL_PerformanceDataTab = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "uploadidslperformancecdatatab"));
                int slMetaDataUploadTemplateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "slmetadatauploadtemplateid"));

                if (!serviceLevelHelper.uploadPerformanceDataFormat(serviceLevelId, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName1, expectedMsg, customAssert)) {
                    customAssert.assertTrue(false, "Error while performance data file upload");
                    customAssert.assertAll();
                }

                if (!serviceLevelHelper.validatePerformanceDataFormatTab(serviceLevelId, performanceDataFormatFileName1, customAssert)) {

                    customAssert.assertTrue(false, "Error while validating Performance Data Format Tab");
                }

            }

        } catch (Exception e) {
            logger.error("Exception while performing SL Computation ");
            customAssert.assertTrue(false, "Exception while performing SL Computation" + e.getStackTrace());
        }

        customAssert.assertAll();
    }

    @Test
    public void Test_SIR_218508_NegativeValuesInTargetFieldsThroughES(){

        CustomAssert customAssert = new CustomAssert();
        ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
        Show show = new Show();

        String flowToTest = "sl automation flow";
        ArrayList<String> childServiceLevelIds;

        //Adding performanceComputationCalculationQuery hardcoded as unable to read from cfg file properly
        String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if (doc['exception'].value=='F'){if(params['_source']['Time Taken (Seconds)'] != ''){if(doc['Time Taken (Seconds)'].value < 28800){params._agg.map.met++; }else{params._agg.map.notMet++}}}else{if(params['_source']['Time Taken (Seconds)'] != ''){if(doc['Time Taken (Seconds)'].value < 28800){params._agg.map.notMet++; }else{params._agg.map.met++}}}\", \"init_script\": \"params._agg['map'] = ['met':0, 'notMet':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30, 'SL_Met':0]; if(params.result.Final_Denominator <= 100){params.result.Target = -100; params.result.Breach = 102; params.result.Default = 107011198765.23456789876543; params.result.SL_Met = 4;} return params.result\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
        String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Met/Missed\": {\"script\": \"if(params['_source']['Time Taken (Secondss1)'] != ''){if(params['_source']['Time Taken (Seconds)'] < 110000){return 'Met'}else{return 'Missed'}}else{return 'Missed'}\"}}}";

        try {
            int serviceLevelId = serviceLevelHelper.getServiceLevelId(flowToTest, PCQ, DCQ, customAssert);
            slToDelete.add(serviceLevelId);
            if (serviceLevelId != -1) {

                List<String> workFlowSteps = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "slactiveworkflowsteps").split("->"));
                //Performing workflow Actions till Active

                if (!serviceLevelHelper.performWorkFlowActions(slEntityTypeId, serviceLevelId, workFlowSteps, auditLogUser, customAssert)) {
                    customAssert.assertTrue(false, "Error while performing workflow actions on SL ID " + serviceLevelId);
                    customAssert.assertAll();
                }

                childServiceLevelIds = serviceLevelHelper.checkIfCSLCreatedOnServiceLevel(serviceLevelId, customAssert);

                int numberOfChildServiceLevel = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "numberofchildservicelevel"));

                if (childServiceLevelIds.size() != numberOfChildServiceLevel) {

                    customAssert.assertTrue(false, "For Service Level Id " + serviceLevelId + " Number of Child Service Level Expected are " + numberOfChildServiceLevel + " Actual " + childServiceLevelIds.size());
                }

                String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilenameesquery");
                String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");


                int uploadIdSL_PerformanceDataTab = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "uploadidslperformancecdatatab"));
                int slMetaDataUploadTemplateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "slmetadatauploadtemplateid"));

                if (!serviceLevelHelper.uploadPerformanceDataFormat(serviceLevelId, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
                    customAssert.assertTrue(false, "Error while performance data file upload");
                    customAssert.assertAll();
                }

                if (!serviceLevelHelper.validatePerformanceDataFormatTab(serviceLevelId, performanceDataFormatFileName, customAssert)) {

                    customAssert.assertTrue(false, "Error while validating Performance Data Format Tab");
                }

                int cSLId = Integer.parseInt(childServiceLevelIds.get(1));
                String rawDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilenameesquery");
                String completedBy = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "createdbyuser");

                if (!serviceLevelHelper.uploadRawDataCSL(cSLId, uploadFilePath,rawDataFileName, rawDataFileValidMsg, customAssert)) {

                    customAssert.assertTrue(false, "Raw Data Uploaded unsuccessfully on CSL " + cSLId);
                    customAssert.assertAll();

                } else {
                    if (!serviceLevelHelper.validateStructuredPerformanceDataCSL(cSLId, rawDataFileName, "Done", "View Structured Data", completedBy, customAssert)) {
                        customAssert.assertTrue(false, "Raw Data File Upload validation unsuccessful");
                        customAssert.assertAll();
                    }
                }

                show.hitShow(cslEntityTypeId, cSLId);
                String showPageResponse = show.getShowJsonStr();

                String workFlowActionToPerform = "ReComputePerformance";
                WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
                Boolean workFlowStatus = workflowActionsHelper.performWorkflowAction(cslEntityTypeId, cSLId, workFlowActionToPerform);

                if (!workFlowStatus) {
                    customAssert.assertTrue(false, "Unable to perform " + workFlowActionToPerform + " on CSL Id " + cSLId);
                }
                Thread.sleep(10000);
                show.hitShow(cslEntityTypeId, cSLId);
                showPageResponse = show.getShowJsonStr();

                String computationStatus = ShowHelper.getValueOfField("computationstatus", showPageResponse);

                if (!computationStatus.equalsIgnoreCase("Error in Computation")) {

                    customAssert.assertTrue(false, "Computation Status Expected : \"Error in Computation\" Actual Computation Status :" + computationStatus);

                } else {
                    customAssert.assertTrue(true, "Computation Status validated successfully");
                }


            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating Negative Values InTarget Fields Through ES");
        }

        customAssert.assertAll();
    }

    @AfterClass
    public void AfterClass(){

        EntityOperationsHelper.deleteMultipleRecords("service levels",slToDelete);
    }
}
