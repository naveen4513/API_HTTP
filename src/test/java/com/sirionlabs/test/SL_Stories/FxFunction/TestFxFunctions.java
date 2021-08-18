package com.sirionlabs.test.SL_Stories.FxFunction;

import com.sirionlabs.api.bulkedit.BulkeditEdit;
import com.sirionlabs.api.bulkupload.UploadBulkData;
import com.sirionlabs.api.bulkupload.UploadRawData;
import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.servicelevel.ServiceLevelHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

public class TestFxFunctions {

    private final static Logger logger = LoggerFactory.getLogger(TestFxFunctions.class);

    private String slCreationConfigFilePath = null;
    private String slCreationConfigFileName = null;
    private String extraFieldsConfigFilePath = null;
    private String extraFieldsConfigFileName = null;
//    private String configFilePath = null;
//    private String configFileName = null;

    private String slEntity = "service levels";
    private String cslEntity = "child service levels";

    private String configFilePath;
    private String configFileName;

    private int slEntityTypeId = 14;
    private int cslEntityTypeId = 15;
    private int contractEntityTypeId = 61;
    private int uploadIdSL_PerformanceDataTab;

    private final int childServiceLevelTabId = 7;
    private int slMetaDataUploadTemplateId;
    private String uploadFilePath;

    private String errorInComputation = "Error in Computation";
    private String computationPerformedSuccessfully = "Computation Completed Successfully";
    private String createdByUser = "vikas J jaiswal";

    PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();

    SCPUtils scpUtils = new SCPUtils();
    int serviceLevelId;

    private ArrayList<Integer> slToDelete = new ArrayList<>();
    private ArrayList<Integer> cslToDelete = new ArrayList<>();
    private ArrayList<String> childServiceLevelId1s = new ArrayList<>();

    String UDC = "Incident ID";

    @BeforeClass
    public void BeforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFileName");

        uploadIdSL_PerformanceDataTab = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "uploadidslperformancecdatatab"));
        slMetaDataUploadTemplateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "slmetadatauploadtemplateid"));

        slCreationConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ServiceLevelsFilePath");
        slCreationConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceLevelsFileName");

        extraFieldsConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ServiceLevelsFilePath");
        extraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceLevelsExtraFieldsFileName");

        uploadFilePath = "src\\test\\resources\\TestConfig\\ServiceLevel\\Fx Function";
        String schedulerHost = ConfigureEnvironment.getEnvironmentProperty("schedulerHost");
        String schedulerUserName = ConfigureEnvironment.getEnvironmentProperty("schedulerUserName");
        String schedulerPassword = ConfigureEnvironment.getEnvironmentProperty("schedulerPassword");
        int port = 22;
        scpUtils = new SCPUtils(schedulerHost,schedulerUserName,schedulerPassword,port);

    }

    @Test(enabled = true )
    public void TestCSLCreation() {

        CustomAssert customAssert = new CustomAssert();
        String flowToTest = "sl automation flow";


        try {

            String mapQuery = "@SUMIF(state.map.fixed, 1, AND(NEQ(doc['exception'], false), EQ(src['Valid'], 'No'),LTE(doc['Number'], 5))) ;@SUMIF(state.map.notfixed, 1, AND(NEQ(src['Valid'], 'Yes'),LT(doc['Number'], 5)));@SUMIF(state.map.fixed1, 1, OR(NEQ(src['Valid'], 'Yes'),GT(doc['Number'], 5)));@SUMIF(state.map.fixed2, 1, OR(NEQ(src['Valid'], 'Yes'),GTE(doc['Number'], 5)));";
            String PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"" + mapQuery + "\",\"init_script\":\"state['map'] =['fixed': 0, 'notfixed': 0, 'fixed1': 0, 'fixed2': 0]\",\"reduce_script\":\"params.return_map = ['Final_Performance':0 ,'Supplier_Calculation':0]; for (a in states){params.return_map.Final_Performance += (a.map.fixed);params.return_map.Supplier_Calculation += (a.map.fixed + a.map.notfixed);}return params.return_map\",\"combine_script\":\"return state;\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}},{\"match\":{\"useInComputation\":true}}]}}}";
            String DCQ = "{\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}}]}},\"script_fields\":{\"Valid\":{\"script\":\"if(params['_source']['Resolution'] == 'Fixed'){return 'Yes'}else{return 'No'}\"}}}";

//                        9013
            serviceLevelId = getActiveServiceLevelId(flowToTest,PCQ,DCQ,customAssert);

            slToDelete.add(serviceLevelId);

            if (serviceLevelId != -1) {

                childServiceLevelId1s = checkIfCSLCreatedOnServiceLevel(serviceLevelId, customAssert);

                addCSLToDelete(childServiceLevelId1s);

            }

        } catch (Exception e) {
            logger.error("Exception while creation of CSL");
            customAssert.assertTrue(false, "Exception while creation of CSL" + e.getMessage());
        }

        customAssert.assertAll();
    }

    //SUMIF GT LTE EQ NEQ
    @Test(enabled = true,priority = 0,dependsOnMethods = "TestCSLCreation")
    public void TestFxFunctionScenario1(){

        CustomAssert customAssert = new CustomAssert();

        try{

            ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();

            //All caps
            String mapQuery = "@SUMIF(state.map.fixed, 1, AND(NEQ(doc['exception'], false), EQ(src['Valid'], 'No'),LTE(doc['Number'], 5))) ;@SUMIF(state.map.notfixed, 1, AND(NEQ(src['Valid'], 'Yes'),LT(doc['Number'], 5)));@SUMIF(state.map.fixed1, 1, OR(NEQ(src['Valid'], 'Yes'),GT(doc['Number'], 5)));@SUMIF(state.map.fixed2, 1, OR(NEQ(src['Valid'], 'Yes'),GTE(doc['Number'], 5)))";
            String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \""+ mapQuery + ";\", \"init_script\": \"state['map'] =['fixed': 0, 'notfixed': 0, 'fixed1': 0, 'fixed2': 0]\", \"reduce_script\": \"params.return_map = ['Final_Performance':0 ,'Supplier_Calculation':0]; for (a in states){params.return_map.Final_Performance += (a.map.fixed);params.return_map.Supplier_Calculation += (a.map.fixed + a.map.notfixed);}return params.return_map\", \"combine_script\": \"return state;\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}, {\"match\": {\"useInComputation\": true}}]}}}";
            String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Valid\": {\"script\": \"if(params['_source']['Resolution'] == 'Fixed'){return 'Yes'}else{return 'No'}\"}}}";

            Boolean editStatus = serviceLevelHelper.editPCQDCQUDC(serviceLevelId,PCQ,DCQ,UDC,customAssert);

            if(!editStatus){
                customAssert.assertTrue(false,"Error while editing PCQ DCQ UDC for SL" );
            }

            String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fx function","template file 1");
            String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");


            if (!uploadPerformanceDataFormat(serviceLevelId, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
                customAssert.assertTrue(false, "Error while performance data file upload");
            }

            if (!validatePerformanceDataFormatTab(serviceLevelId, performanceDataFormatFileName, customAssert)) {

                customAssert.assertTrue(false, "Error while validating Performance Data Format Tab");
            }


            String rawDataFile = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fx function","raw data file 1");

            Boolean uploadStatus;

            int cslId = Integer.parseInt(childServiceLevelId1s.get(0));

            uploadStatus = uploadRawDataCSL(cslId,rawDataFile,customAssert);

            if(!uploadStatus){
                logger.error("Raw Data upload unsuccessful for CSL " + cslId);
                customAssert.assertTrue(false,"Raw Data upload unsuccessful for CSL " + cslId);
            }

            if (!validateStructuredPerformanceDataCSL(cslId, rawDataFile, "Done", "View Structured Data", createdByUser, customAssert)) {
                customAssert.assertTrue(false, "Raw Data File Upload validation unsuccessful");

            }

            Thread.sleep(2000);
            mapQuery = "@SUMIF(state.map.fixed, 1, and(NEQ(doc['exception'], false), EQ(src['Valid'], 'No'),LTE(doc['Number'], 5))) ;@SUMIF(state.map.notfixed, 1, and(NEQ(src['Valid'], 'Yes'),LT(doc['Number'], 5)));@SUMIF(state.map.fixed1, 1, OR(NEQ(src['Valid'], 'Yes'),GT(doc['Number'], 5)));@SUMIF(state.map.fixed2, 1, OR(NEQ(src['Valid'], 'Yes'),GTE(doc['Number'], 5)));";
            PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"" + mapQuery + "\",\"init_script\":\"state['map'] =['fixed': 0, 'notfixed': 0, 'fixed1': 0, 'fixed2': 0]\",\"reduce_script\":\"params.return_map = ['Final_Performance':0 ,'Supplier_Calculation':0]; for (a in states){params.return_map.Final_Performance += (a.map.fixed);params.return_map.Supplier_Calculation += (a.map.fixed + a.map.notfixed);}return params.return_map\",\"combine_script\":\"return state;\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}},{\"match\":{\"useInComputation\":true}}]}}}";

            //All caps
            Boolean validationComputationStatus =  validateComputationStatusOnCSLAfterUpdatingPCQ(serviceLevelId,cslId,PCQ,computationPerformedSuccessfully,customAssert);

            int scenario =1;
            if(!validationComputationStatus){
                customAssert.assertTrue(false,"Computation Status validated unsuccessfully On CSL After Updating PCQ for scenario " + scenario);
            }

            cslId = Integer.parseInt(childServiceLevelId1s.get(1));

            uploadStatus = uploadRawDataCSL(cslId,rawDataFile,customAssert);

            if(!uploadStatus){
                logger.error("Raw Data upload unsuccessful for CSL " + cslId);
                customAssert.assertTrue(false,"Raw Data upload unsuccessful for CSL " + cslId);
            }

            if (!validateStructuredPerformanceDataCSL(cslId, rawDataFile, "Done", "View Structured Data", createdByUser, customAssert)) {
                customAssert.assertTrue(false, "Raw Data File Upload validation unsuccessful");

            }

            //All small
            mapQuery = "@SUMIF(state.map.fixed, 1, and(neq(doc['exception'], false), eq(src['Valid'], 'No'),lte(doc['Number'], 5))) ;@SUMIF(state.map.notfixed, 1, and(neq(src['Valid'], 'Yes'),lt(doc['Number'], 5)));@SUMIF(state.map.fixed1, 1, or(neq(src['Valid'], 'Yes'),gt(doc['Number'], 5)));@SUMIF(state.map.fixed2, 1, or(neq(src['Valid'], 'Yes'),gte(doc['Number'], 5)));";
            PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"" + mapQuery + "\",\"init_script\":\"state['map'] =['fixed': 0, 'notfixed': 0]\",\"reduce_script\":\"params.return_map = ['Final_Performance':0 ,'Supplier_Calculation':0]; for (a in states){params.return_map.Final_Performance += (a.map.fixed);params.return_map.Supplier_Calculation += (a.map.fixed + a.map.notfixed);}return params.return_map\",\"combine_script\":\"return state;\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}},{\"match\":{\"useInComputation\":true}}]}}}";

            validationComputationStatus =  validateComputationStatusOnCSLAfterUpdatingPCQ(serviceLevelId,cslId,PCQ,errorInComputation,customAssert);

            scenario +=1;
            if(!validationComputationStatus){
                customAssert.assertTrue(false,"Computation Status validated unsuccessfully On CSL After Updating PCQ for scenario " + scenario);
            }


            cslId = Integer.parseInt(childServiceLevelId1s.get(2));

            uploadStatus = uploadRawDataCSL(cslId,rawDataFile,customAssert);

            if(!uploadStatus){
                logger.error("Raw Data upload unsuccessful for CSL " + cslId);
                customAssert.assertTrue(false,"Raw Data upload unsuccessful for CSL " + cslId);
            }

            if (!validateStructuredPerformanceDataCSL(cslId, rawDataFile, "Done", "View Structured Data", createdByUser, customAssert)) {
                customAssert.assertTrue(false, "Raw Data File Upload validation unsuccessful");

            }

            Thread.sleep(3000);
            //First character in Caps
            mapQuery = "@SUMIF(state.map.fixed, 1, and(Neq(doc['exception'], false), Eq(src['Valid'], 'No'),Lte(doc['Number'], 5))) ;@SUMIF(state.map.notfixed, 1, and(Neq(src['Valid'], 'Yes'),Lt(doc['Number'], 5)));@SUMIF(state.map.fixed1, 1, or(Neq(src['Valid'], 'Yes'),Gt(doc['Number'], 5)));@SUMIF(state.map.fixed2, 1, Or(Neq(src['Valid'], 'Yes'),Gte(doc['Number'], 5)));";
            PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"" + mapQuery + "\",\"init_script\":\"state['map'] =['fixed': 0, 'notfixed': 0]\",\"reduce_script\":\"params.return_map = ['Final_Performance':0 ,'Supplier_Calculation':0]; for (a in states){params.return_map.Final_Performance += (a.map.fixed);params.return_map.Supplier_Calculation += (a.map.fixed + a.map.notfixed);}return params.return_map\",\"combine_script\":\"return state;\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}},{\"match\":{\"useInComputation\":true}}]}}}";

            validationComputationStatus =  validateComputationStatusOnCSLAfterUpdatingPCQ(serviceLevelId,cslId,PCQ,errorInComputation,customAssert);

            scenario +=1;
            if(!validationComputationStatus){
                customAssert.assertTrue(false,"Computation Status validated unsuccessfully On CSL After Updating PCQ for scenario " + scenario);
            }


        }catch (Exception e){
            logger.error("Exception while validating frequency module test cases " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while validating frequency module test cases " + e.getStackTrace());
        }

        customAssert.assertAll();
    }


    //    http://qa.ba1.office/ux/#/show/tblslas/9046
    @Test(enabled = true,dependsOnMethods = "TestCSLCreation",priority = 1)
    public void TestFxFunctionScenario2(){

        CustomAssert customAssert = new CustomAssert();
        String flowToTest = "sl automation flow";

        try{

            //All caps
            String mapQuery = "if (params['_source']['Valid'] == 'Yes') {@COUNT(state.map.an);} else {@COUNT(state.map.ad);}@COUNTIF(state.map.sn, OR(gt(doc['Number'], 5), lt(doc['Data ID'], 9)));@COUNTIF(state.map.sd, AND(gt(doc['Number'], 1), lte(doc['Data ID'], 9)));@COUNTIFS(state.map.sd, eq(doc['Number'], 5), lte(doc['Number'], 6));";
            String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"" + mapQuery + "));\", \"init_script\": \"state['map'] =['an': 0, 'ad': 0, 'sn': 0, 'sd': 0, 'fn': 0, 'fd': 0]\", \"reduce_script\": \"params.return_map =['Actual_Numerator': 0,'Actual_Denominator': 0,'Supplier_Numerator': 0, 'Supplier_Denominator': 0,'Final_Numerator': 0, 'Final_Denominator': 0, 'Final_Performance': 0, 'SL_Met': 0]; @FOREACH(a, states, SUM(params.return_map.Actual_Numerator, a.map.an), SUM(params.return_map.Actual_Denominator, a.map.ad), SUM(params.return_map.Supplier_Numerator, a.map.sn), SUM(params.return_map.Supplier_Denominator, a.map.sd), SUM(params.return_map.Final_Numerator, a.map.fn), SUM(params.return_map.Final_Denominator, a.map.fd)); if (params.return_map.Final_Denominator != 0) {params.return_map.Final_Performance = ((params.return_map.Final_Numerator * 100) / params.return_map.Final_Denominator)} else {params.return_map.Final_Performance = ''; params.return_map.SL_Met = 6;}return params.return_map\", \"combine_script\": \"return state;\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}, {\"match\": {\"useInComputation\": true}}]}}}";
            String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Valid SUM\": {\"script\": \"def A = 0; @COUNT(A); return A;\"}, \"Valid SUMIF\": {\"script\": \"def B = 0; @COUNTIF(B, AND(eq(src['Valid'], {'Yes','No'}), eq(src['Case'], 'A'))); return B;\"}, \"Valid SUMIFS\": {\"script\": \"def C = 0; @COUNTIFS(C, eq(src['Valid'], 'Yes')); return C;\"}}}";

            ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
            Boolean editStatus = serviceLevelHelper.editPCQDCQUDC(serviceLevelId,PCQ,DCQ,UDC,customAssert);

            if(!editStatus){
                customAssert.assertTrue(false,"Error while editing PCQ DCQ UDC for SL" );
            }

            String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fx function","template file 2");
            String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");


            if (!uploadPerformanceDataFormat(serviceLevelId, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
                customAssert.assertTrue(false, "Error while performance data file upload");
            }

            if (!validatePerformanceDataFormatTab(serviceLevelId, performanceDataFormatFileName, customAssert)) {

                customAssert.assertTrue(false, "Error while validating Performance Data Format Tab");
            }



            String rawDataFile = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fx function","raw data file 2");

            Boolean uploadStatus;

            int cslId = Integer.parseInt(childServiceLevelId1s.get(3));

            uploadStatus = uploadRawDataCSL(cslId,rawDataFile,customAssert);

            if(!uploadStatus){
                logger.error("Raw Data upload unsuccessful for CSL " + cslId);
                customAssert.assertTrue(false,"Raw Data upload unsuccessful for CSL " + cslId);
            }

            if (!validateStructuredPerformanceDataCSL(cslId, rawDataFile, "Done", "View Structured Data", createdByUser, customAssert)) {
                customAssert.assertTrue(false, "Raw Data File Upload validation unsuccessful");

            }

            mapQuery = "@SUMIF(state.map.fixed, 1, AND(NEQ(doc['exception'], false), EQ(src['Valid'], 'No'),LTE(doc['Number'], 5))) ;@SUMIF(state.map.notfixed, 1, AND(NEQ(src['Valid'], 'Yes'),LT(doc['Number'], 5)));@SUMIF(state.map.fixed1, 1, OR(NEQ(src['Valid'], 'Yes'),GT(doc['Number'], 5)));@SUMIF(state.map.fixed2, 1, OR(NEQ(src['Valid'], 'Yes'),GTE(doc['Number'], 5)));";
            PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"" + mapQuery + "\",\"init_script\":\"state['map'] =['fixed': 0, 'notfixed': 0, 'fixed1': 0, 'fixed2': 0]\",\"reduce_script\":\"params.return_map = ['Final_Performance':0 ,'Supplier_Calculation':0]; for (a in states){params.return_map.Final_Performance += (a.map.fixed);params.return_map.Supplier_Calculation += (a.map.fixed + a.map.notfixed);}return params.return_map\",\"combine_script\":\"return state;\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}},{\"match\":{\"useInComputation\":true}}]}}}";

            //All caps
            Boolean validationComputationStatus =  validateComputationStatusOnCSLAfterUpdatingPCQ(serviceLevelId,cslId,PCQ,computationPerformedSuccessfully,customAssert);

            int scenario =1;
            if(!validationComputationStatus){
                customAssert.assertTrue(false,"Computation Status validated unsuccessfully On CSL After Updating PCQ for scenario " + scenario);
            }

        }catch (Exception e){
            logger.error("Exception while validating frequency module test cases " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while validating frequency module test cases " + e.getStackTrace());
        }

        customAssert.assertAll();
    }

    //Fx with Sirion function
//    http://qa.ba1.office/ux/#/show/tblslas/9063?_t=1584026402515
    @Test(enabled = true,dependsOnMethods = "TestCSLCreation",priority = 2)
    public void TestFxFunctionScenario3(){

        CustomAssert customAssert = new CustomAssert();
        String flowToTest = "sl automation flow";

        try{

            //All caps
            String mapQuery = "@SUM(state.map.total, doc['Time Taken (Seconds)']);@SUMIF(state.map.totalif, doc['Time Taken (Seconds)'], gt(doc['Time Taken (Seconds)'], 1));";
            String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"" + mapQuery + "\", \"init_script\": \"state['map'] = ['met':0, 'notMet':0, 'total':0, 'totalif':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator': 0, 'Actual_Denominator': 0, 'Supplier_Numerator': 0, 'Supplier_Denominator': 0, 'Final_Numerator': 0, 'Final_Denominator': 0];@FOREACH(a, states, SUM(params.result.Final_Denominator, a.map.totalif), SUM(params.result.Final_Numerator, a.map.total));if (params.result.Final_Denominator <= 100) {params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;}return params.result\", \"combine_script\": \"return state;\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
            String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Met/Missed\": {\"script\": \"if(params['_source']['Time Taken (Seconds)'] != ''){if(params['_source']['Time Taken (Seconds)'] < 110000){return 'Met'}else{return 'Missed'}}else{return 'Missed'}\"}}}";

            ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
            Boolean editStatus = serviceLevelHelper.editPCQDCQUDC(serviceLevelId,PCQ,DCQ,UDC,customAssert);

            if(!editStatus){
                customAssert.assertTrue(false,"Error while editing PCQ DCQ UDC for SL" );
            }

            String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fx function","template file 3");
            String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");


            if (!uploadPerformanceDataFormat(serviceLevelId, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
                customAssert.assertTrue(false, "Error while performance data file upload");
            }

            if (!validatePerformanceDataFormatTab(serviceLevelId, performanceDataFormatFileName, customAssert)) {

                customAssert.assertTrue(false, "Error while validating Performance Data Format Tab");
            }


            String rawDataFile = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fx function","raw data file 3");

            Boolean uploadStatus;

            int cslId = Integer.parseInt(childServiceLevelId1s.get(4));

            uploadStatus = uploadRawDataCSL(cslId,rawDataFile,customAssert);

            if(!uploadStatus){
                logger.error("Raw Data upload unsuccessful for CSL " + cslId);
                customAssert.assertTrue(false,"Raw Data upload unsuccessful for CSL " + cslId);
            }

            if (!validateStructuredPerformanceDataCSL(cslId, rawDataFile, "Done", "View Structured Data", createdByUser, customAssert)) {
                customAssert.assertTrue(false, "Raw Data File Upload validation unsuccessful");

            }

            mapQuery = "@SUM(state.map.total, doc['Time Taken (Seconds)']);@SUMIF(state.map.totalif, doc['Time Taken (Seconds)'], gt(doc['Time Taken (Seconds)'], 1));";
            PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"" + mapQuery + "\", \"init_script\": \"state['map'] = ['met':0, 'notMet':0, 'total':0, 'totalif':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator': 0, 'Actual_Denominator': 0, 'Supplier_Numerator': 0, 'Supplier_Denominator': 0, 'Final_Numerator': 0, 'Final_Denominator': 0];@FOREACH(a, states, SUM(params.result.Final_Denominator, a.map.totalif), SUM(params.result.Final_Numerator, a.map.total));if (params.result.Final_Denominator <= 100) {params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;}return params.result\", \"combine_script\": \"return state;\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";

            //All caps
            Boolean validationComputationStatus =  validateComputationStatusOnCSLAfterUpdatingPCQ(serviceLevelId,cslId,PCQ,computationPerformedSuccessfully,customAssert);

            int scenario =1;
            if(!validationComputationStatus){
                customAssert.assertTrue(false,"Computation Status validated unsuccessfully On CSL After Updating PCQ for scenario " + scenario);
            }


        }catch (Exception e){
            logger.error("Exception while validating frequency module test cases " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while validating frequency module test cases " + e.getStackTrace());
        }

        customAssert.assertAll();
    }

    //Count Combinations
//    http://qa.ba1.office/ux/#/show/tblslas/9059
//    http://qa.ba1.office/ux/#/show/tblslas/9060?
    @Test(enabled = true,dependsOnMethods = "TestCSLCreation",priority = 3)
    public void TestFxFunctionScenario4(){

        CustomAssert customAssert = new CustomAssert();
        String flowToTest = "sl automation flow";

        try{

            //All caps
            String mapQuery = "@COUNT(state.map.total);@COUNTIFS(state.map.notmet, neq(src['Valid'], 'Yes'), eq(src['Status'], 'NotMet'))";
            String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"" + mapQuery+ ";\", \"init_script\": \"state['map'] =['met': 0, 'notmet': 0, 'total': 0]\", \"reduce_script\": \"params.return_map =['Final_Performance': 0, 'Final_Numerator': 0, 'Final_Denominator': 0, 'Supplier_Calculation': 0, 'SL_Met': 0]; for (a in states) {params.return_map.Final_Denominator += a.map.total; params.return_map.Final_Numerator += (a.map.met + a.map.notmet);}if (params.return_map.Final_Denominator != 0) {params.return_map.Final_Performance = ((params.return_map.Final_Numerator * 100) / params.return_map.Final_Denominator)} else {params.return_map.Final_Performance = ''; params.return_map.SL_Met = 5;}return params.return_map\", \"combine_script\": \"return state;\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}, {\"match\": {\"useInComputation\": true}}]}}}";
            String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Valid Issue\": {\"script\": \"if(params['_source']['Resolution'] == 'Fixed'){return 'Yes'}else{return 'No'}\"}}}";


            ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
            Boolean editStatus = serviceLevelHelper.editPCQDCQUDC(serviceLevelId,PCQ,DCQ,UDC,customAssert);

            if(!editStatus){
                customAssert.assertTrue(false,"Error while editing PCQ DCQ UDC for SL" );
            }


            String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fx function","template file 1");
            String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");


            if (!uploadPerformanceDataFormat(serviceLevelId, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
                customAssert.assertTrue(false, "Error while performance data file upload");
            }

            if (!validatePerformanceDataFormatTab(serviceLevelId, performanceDataFormatFileName, customAssert)) {

                customAssert.assertTrue(false, "Error while validating Performance Data Format Tab");
            }

//            ArrayList<String> childServiceLevelIds = checkIfCSLCreatedOnServiceLevel(serviceLevelId, customAssert);
//            addCSLToDelete(childServiceLevelIds);

            String rawDataFile = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fx function","raw data file 1");

            Boolean uploadStatus;

            int cslId = Integer.parseInt(childServiceLevelId1s.get(5));

            uploadStatus = uploadRawDataCSL(cslId,rawDataFile,customAssert);

            if(!uploadStatus){
                logger.error("Raw Data upload unsuccessful for CSL " + cslId);
                customAssert.assertTrue(false,"Raw Data upload unsuccessful for CSL " + cslId);
            }

            if (!validateStructuredPerformanceDataCSL(cslId, rawDataFile, "Done", "View Structured Data", createdByUser, customAssert)) {
                customAssert.assertTrue(false, "Raw Data File Upload validation unsuccessful");

            }

            mapQuery = "@COUNT(state.map.total);@COUNTIFS(state.map.notmet, neq(src['Valid'], 'Yes'), eq(src['Status'], 'NotMet'));";
            PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"" + mapQuery + " \",\"init_script\":\"state['map'] =['met': 0, 'notmet': 0, 'total': 0]\",\"reduce_script\":\"params.return_map =['Final_Performance': 0, 'Final_Numerator': 0, 'Final_Denominator': 0, 'Supplier_Calculation': 0, 'SL_Met': 0]; for (a in states) {params.return_map.Final_Denominator += a.map.total; params.return_map.Final_Numerator += (a.map.met + a.map.notmet);}if (params.return_map.Final_Denominator != 0) {params.return_map.Final_Performance = ((params.return_map.Final_Numerator * 100) / params.return_map.Final_Denominator)} else {params.return_map.Final_Performance = ''; params.return_map.SL_Met = 5;}return params.return_map\",\"combine_script\":\"return state;\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}},{\"match\":{\"useInComputation\":true}}]}}}";

            //All caps
            Boolean validationComputationStatus =  validateComputationStatusOnCSLAfterUpdatingPCQ(serviceLevelId,cslId,PCQ,computationPerformedSuccessfully,customAssert);

            int scenario =1;
            if(!validationComputationStatus){
                customAssert.assertTrue(false,"Computation Status validated unsuccessfully On CSL After Updating PCQ for scenario " + scenario);
            }


            cslId = Integer.parseInt(childServiceLevelId1s.get(6));

            uploadStatus = uploadRawDataCSL(cslId,rawDataFile,customAssert);

            if(!uploadStatus){
                logger.error("Raw Data upload unsuccessful for CSL " + cslId);
                customAssert.assertTrue(false,"Raw Data upload unsuccessful for CSL " + cslId);
            }

            if (!validateStructuredPerformanceDataCSL(cslId, rawDataFile, "Done", "View Structured Data", createdByUser, customAssert)) {
                customAssert.assertTrue(false, "Raw Data File Upload validation unsuccessful");

            }

            mapQuery = "@COUNTIFS(state.map.notmet, neq(src['Valid'], 'Yes'), eq(src['Status'], 'NotMet'));";
            PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"" + mapQuery + "\",\"init_script\":\"state['map'] =['met': 0, 'notmet': 0, 'total': 0]\",\"reduce_script\":\"params.return_map =['Final_Performance': 0, 'Final_Numerator': 0, 'Final_Denominator': 0, 'Supplier_Calculation': 0, 'SL_Met': 0]; for (a in states) {params.return_map.Final_Denominator += a.map.total; params.return_map.Final_Numerator += (a.map.met + a.map.notmet);}if (params.return_map.Final_Denominator != 0) {params.return_map.Final_Performance = ((params.return_map.Final_Numerator * 100) / params.return_map.Final_Denominator)} else {params.return_map.Final_Performance = ''; params.return_map.SL_Met = 5;}return params.return_map\",\"combine_script\":\"return state;\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}},{\"match\":{\"useInComputation\":true}}]}}}";

            //All caps
            validationComputationStatus =  validateComputationStatusOnCSLAfterUpdatingPCQ(serviceLevelId,cslId,PCQ,computationPerformedSuccessfully,customAssert);

            scenario +=1;
            if(!validationComputationStatus){
                customAssert.assertTrue(false,"Computation Status validated unsuccessfully On CSL After Updating PCQ for scenario " + scenario);
            }

            System.out.println("abc");


        }catch (Exception e){
            logger.error("Exception while validating frequency module test cases " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while validating frequency module test cases " + e.getStackTrace());
        }

        customAssert.assertAll();
    }


    @Test(enabled = true,dependsOnMethods = "TestCSLCreation",priority = 4)
    public void TestWrongArgumentsOFFxFunction(){

        CustomAssert customAssert = new CustomAssert();

        String flowToTest = "sl automation flow";

        try{

            //All caps
            String mapQuery = "@COUNT(state.map.total);";
            String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"" + mapQuery + ";\", \"init_script\": \"state['map'] =['met': 0, 'notmet': 0, 'total': 0]\", \"reduce_script\": \"params.return_map =['Final_Performance': 0, 'Final_Numerator': 0, 'Final_Denominator': 0, 'Supplier_Calculation': 0, 'SL_Met': 0]; for (a in states) {params.return_map.Final_Denominator += a.map.total; params.return_map.Final_Numerator += (a.map.met + a.map.notmet);}if (params.return_map.Final_Denominator != 0) {params.return_map.Final_Performance = ((params.return_map.Final_Numerator * 100) / params.return_map.Final_Denominator)} else {params.return_map.Final_Performance = ''; params.return_map.SL_Met = 5;}return params.return_map\", \"combine_script\": \"return state;\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}, {\"match\": {\"useInComputation\": true}}]}}}";
            String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Valid Issue\": {\"script\": \"if(params['_source']['Resolution'] == 'Fixed'){return 'Yes'}else{return 'No'}\"}}}";


            ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
            Boolean editStatus = serviceLevelHelper.editPCQDCQUDC(serviceLevelId,PCQ,DCQ,UDC,customAssert);

            if(!editStatus){
                customAssert.assertTrue(false,"Error while editing PCQ DCQ UDC for SL" );
            }


            String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fx function","template file 1");
            String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");

            if (!uploadPerformanceDataFormat(serviceLevelId, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
                customAssert.assertTrue(false, "Error while performance data file upload");
            }

            if (!validatePerformanceDataFormatTab(serviceLevelId, performanceDataFormatFileName, customAssert)) {

                customAssert.assertTrue(false, "Error while validating Performance Data Format Tab");
            }

            String rawDataFile = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fx function","raw data file 1");

            Boolean uploadStatus;

            int cslId = Integer.parseInt(childServiceLevelId1s.get(7));

            uploadStatus = uploadRawDataCSL(cslId,rawDataFile,customAssert);

            if(!uploadStatus){
                logger.error("Raw Data upload unsuccessful for CSL " + cslId);
                customAssert.assertTrue(false,"Raw Data upload unsuccessful for CSL " + cslId);
            }

            if (!validateStructuredPerformanceDataCSL(cslId, rawDataFile, "Done", "View Structured Data", createdByUser, customAssert)) {
                customAssert.assertTrue(false, "Raw Data File Upload validation unsuccessful");

            }

            mapQuery = "@COUNTIFS(state.map.notmet, neq(src['Valid'], 'Yes',1), eq(src['Status'], 'NotMet',1),1);";
            PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"" + mapQuery + " \",\"init_script\":\"state['map'] =['met': 0, 'notmet': 0, 'total': 0]\",\"reduce_script\":\"params.return_map =['Final_Performance': 0, 'Final_Numerator': 0, 'Final_Denominator': 0, 'Supplier_Calculation': 0, 'SL_Met': 0]; for (a in states) {params.return_map.Final_Denominator += a.map.total; params.return_map.Final_Numerator += (a.map.met + a.map.notmet);}if (params.return_map.Final_Denominator != 0) {params.return_map.Final_Performance = ((params.return_map.Final_Numerator * 100) / params.return_map.Final_Denominator)} else {params.return_map.Final_Performance = ''; params.return_map.SL_Met = 5;}return params.return_map\",\"combine_script\":\"return state;\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}},{\"match\":{\"useInComputation\":true}}]}}}";

            //All caps
            Boolean updateStatus = updatePCQOnSL(serviceLevelId,PCQ,"validationError",customAssert);

            int scenario =1;
            if(!updateStatus){
                customAssert.assertTrue(false,"PCQ updated successfully for scenario " + scenario);
            }


            cslId = Integer.parseInt(childServiceLevelId1s.get(8));

            uploadStatus = uploadRawDataCSL(cslId,rawDataFile,customAssert);

            if(!uploadStatus){
                logger.error("Raw Data upload unsuccessful for CSL " + cslId);
                customAssert.assertTrue(false,"Raw Data upload unsuccessful for CSL " + cslId);
            }

            if (!validateStructuredPerformanceDataCSL(cslId, rawDataFile, "Done", "View Structured Data", createdByUser, customAssert)) {
                customAssert.assertTrue(false, "Raw Data File Upload validation unsuccessful");

            }

            mapQuery = "@SUMIF(state.map.fixed, 1, AND(NEQ(doc['exception'], false), EQ(src['Valid'], 'No'),LTE(doc['Number'], 5)),1,9) ;";

            PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"" + mapQuery + "\",\"init_script\":\"state['map'] =['met': 0, 'notmet': 0, 'total': 0]\",\"reduce_script\":\"params.return_map =['Final_Performance': 0, 'Final_Numerator': 0, 'Final_Denominator': 0, 'Supplier_Calculation': 0, 'SL_Met': 0]; for (a in states) {params.return_map.Final_Denominator += a.map.total; params.return_map.Final_Numerator += (a.map.met + a.map.notmet);}if (params.return_map.Final_Denominator != 0) {params.return_map.Final_Performance = ((params.return_map.Final_Numerator * 100) / params.return_map.Final_Denominator)} else {params.return_map.Final_Performance = ''; params.return_map.SL_Met = 5;}return params.return_map\",\"combine_script\":\"return state;\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}},{\"match\":{\"useInComputation\":true}}]}}}";

            //All caps
            updateStatus = updatePCQOnSL(serviceLevelId,PCQ,"validationError",customAssert);
            scenario +=1;
            if(!updateStatus){
                customAssert.assertTrue(false,"PCQ updated successfully for scenario " + scenario);
            }


        }catch (Exception e){
            logger.error("Exception while validating frequency module test cases " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while validating frequency module test cases " + e.getStackTrace());
        }

        customAssert.assertAll();

    }

    //Simple Edit Bulk Edit Bulk Update Bulk Create
    @Test(enabled = true,dependsOnMethods = "TestCSLCreation",priority = 5)
    public void TestWrongSpellingOFFxFunction(){

        CustomAssert customAssert = new CustomAssert();

        String flowToTest = "sl automation flow";
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
        try{

            //All caps
            String mapQuery = "@COUNT(state.map.total);";
            String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"" + mapQuery + ";\", \"init_script\": \"state['map'] =['met': 0, 'notmet': 0, 'total': 0]\", \"reduce_script\": \"params.return_map =['Final_Performance': 0, 'Final_Numerator': 0, 'Final_Denominator': 0, 'Supplier_Calculation': 0, 'SL_Met': 0]; for (a in states) {params.return_map.Final_Denominator += a.map.total; params.return_map.Final_Numerator += (a.map.met + a.map.notmet);}if (params.return_map.Final_Denominator != 0) {params.return_map.Final_Performance = ((params.return_map.Final_Numerator * 100) / params.return_map.Final_Denominator)} else {params.return_map.Final_Performance = ''; params.return_map.SL_Met = 5;}return params.return_map\", \"combine_script\": \"return state;\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}, {\"match\": {\"useInComputation\": true}}]}}}";
            String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Valid Issue\": {\"script\": \"if(params['_source']['Resolution'] == 'Fixed'){return 'Yes'}else{return 'No'}\"}}}";

            ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
            Boolean editStatus = serviceLevelHelper.editPCQDCQUDC(serviceLevelId,PCQ,DCQ,UDC,customAssert);

            if(!editStatus){
                customAssert.assertTrue(false,"Error while editing PCQ DCQ UDC for SL" );
            }

            //Single Edit Expression Error
            int cslId = Integer.parseInt(childServiceLevelId1s.get(8));



            mapQuery = "COUNTIFSS(state.map.notmet, neq(src['Valid'], 'Yes',1), eq(src['Status'], 'NotMet',1))";
            PCQ = "{\\\"aggs\\\":{\\\"group_by_sl_met\\\":{\\\"scripted_metric\\\":{\\\"map_script\\\":\\\"@" + mapQuery +"; \\\",\\\"init_script\\\":\\\"state['map'] =['met': 0, 'notmet': 0, 'total': 0]\\\",\\\"reduce_script\\\":\\\"params.return_map =['Final_Performance': 0, 'Final_Numerator': 0, 'Final_Denominator': 0, 'Supplier_Calculation': 0, 'SL_Met': 0]; for (a in states) {params.return_map.Final_Denominator += a.map.total; params.return_map.Final_Numerator += (a.map.met + a.map.notmet);}if (params.return_map.Final_Denominator != 0) {params.return_map.Final_Performance = ((params.return_map.Final_Numerator * 100) / params.return_map.Final_Denominator)} else {params.return_map.Final_Performance = ''; params.return_map.SL_Met = 5;}return params.return_map\\\",\\\"combine_script\\\":\\\"return state;\\\"}}},\\\"size\\\":0,\\\"query\\\":{\\\"bool\\\":{\\\"must\\\":[{\\\"match\\\":{\\\"childslaId\\\":\\\"childSLAId\\\"}},{\\\"match\\\":{\\\"useInComputation\\\":true}}]}}}";
            Boolean PCQUpdateStatus =  updatePCQOnSL(serviceLevelId,PCQ,"validationError",customAssert);

            if(!PCQUpdateStatus){
                customAssert.assertTrue(false,"PCQ updated unsuccessfully");
            }

            int PCQFieldId = 298;

            //Bulk Edit
            try {

                Show show = new Show();
                show.hitShowVersion2(slEntityTypeId,serviceLevelId);
                String showResponse = show.getShowJsonStr();

                String slIdExpected = ShowHelper.getValueOfField("short code id",showResponse);

                String bulkEditPayload = "{\"body\":{\"data\":{\"performanceComputationCalculationQuery\":{\"name\":\"performanceComputationCalculationQuery\",\"id\":298,\"multiEntitySupport\":false,\"values\":\"" + PCQ + "\"}}," +
                        "\"globalData\":{\"entityIds\":[" + serviceLevelId + "],\"fieldIds\":[" + PCQFieldId + "],\"isGlobalBulk\":true}}}";

                BulkeditEdit bulkeditEdit = new BulkeditEdit();
                bulkeditEdit.hitBulkeditEdit(slEntityTypeId, bulkEditPayload);
                String bulkEditResponse = bulkeditEdit.getBulkeditEditJsonStr();

                if (!bulkEditResponse.contains("success")) {

                    customAssert.assertTrue(false, "Bulk edit done unsuccessfully ");
                }

                Thread.sleep(6000);

                String remoteFilePath = "/data/temp-session";

                String currentDate =  DateUtils.getCurrentDateInAnyFormat("MM-dd-YYYY");
                currentDate = currentDate.replace("-","");

                String remoteFileName = "Bulk Edit Response Report - " + currentDate + ".xls";

                String localDir = "src\\test\\resources\\TestConfig\\ServiceLevel\\Fx Function\\";

                String localFileName = "Bulk Edit Response Report.xls";

                Boolean scpStatus = scpUtils.getFileFromRemoteServerToLocalServer(remoteFilePath,remoteFileName,localDir,localFileName);

                if(scpStatus) {

                    XLSUtils xlsUtils = new XLSUtils(uploadFilePath, localFileName);

                    String excelSheetName = "Error";
                    String errorMessage = xlsUtils.getCellData(excelSheetName, 1, 3);

                    String expectedErrorMsgBulkEdit = "I18n Syntax Error In Following Expression: " + mapQuery;

                    if (!errorMessage.equalsIgnoreCase(expectedErrorMsgBulkEdit)) {
                        logger.error("Expected and Actual Error Message while Bulk Edit didn't match");
                        customAssert.assertTrue(false, "Expected and Actual Error Message while Bulk Edit didn't match");

                    }

                    String slIDInExcel = xlsUtils.getCellData(excelSheetName, 0, 3);

                    if (!slIdExpected.equalsIgnoreCase(slIDInExcel)) {
                        logger.error("Expected and Actual Error SLID in Bulk Edit Error Response Attachment Excel didn't match");
                        customAssert.assertTrue(false, "Expected and Actual Error SLID in Bulk Edit Error Response Attachment Excel didn't match");

                    }

                }else {
                    customAssert.assertTrue(false,"Bulk Edit Response File SCP Done unsuccessfully");
                }
            }catch (Exception e){
                logger.error("Exception while validating bulk edit scenario " + e.getStackTrace());
            }

            //Bulk Update
//            logger.info("Validating Bulk Update Error Scenario");
//
//            try{
//
//                String downloadFilePath = uploadFilePath;
//                String bulkUpdateFileName = "BulkUpdateTemplate.xlsm";
//
//                int bulkUpdateTemplateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "slbulkupdatetemplateid"));
//
//                Boolean bulkTemplateDownloadStatus = BulkTemplate.downloadBulkUpdateTemplate(downloadFilePath, bulkUpdateFileName, bulkUpdateTemplateId, slEntityTypeId, String.valueOf(serviceLevelId));
//
//                if (bulkTemplateDownloadStatus) {
//
//                    String sheetName = "Sla";
//                    XLSUtils.updateColumnValue(downloadFilePath, bulkUpdateFileName, sheetName, 6, 4, PCQ);
//
//                    String bulkUpdateUploadResponse = BulkTemplate.uploadBulkUpdateTemplate(downloadFilePath, bulkUpdateFileName, slEntityTypeId, bulkUpdateTemplateId);
//
//                    if (bulkUpdateUploadResponse.contains("200")) {
//
//                        String sqlQuery = "select id from bulk_edit_request order by id desc limit 1;";
//
//                        List<List<String>> sqlResult = postgreSQLJDBC.doSelect(sqlQuery);
//                        if(sqlResult.size() == 0){
//                            customAssert.assertTrue(false,"DB record not found");
//                        }else {
//                            String bulkEditRequestId = sqlResult.get(0).get(0);
//
//                            //Code for scp
//                            String bulkUpdateResponseFileName = "BulkUpdateResponse.xlsm";
//
//                            Thread.sleep(6000);
//
//                            String remoteFilePath = "/data/temp-session/bulktask/" + bulkEditRequestId;
//
//                            String remoteFileName = "BulkUpdateTemplate.xlsm";
//
//                            String localDir = "src\\test\\resources\\TestConfig\\ServiceLevel\\Fx Function\\";
//
//                            String localFileName = "BulkUpdateResponse.xlsm";
//
//                            Boolean scpStatus = scpUtils.getFileFromRemoteServerToLocalServer(remoteFilePath, remoteFileName, localDir, localFileName);
//
//                            if (scpStatus) {
//
//                                XLSUtils xlsUtils = new XLSUtils(downloadFilePath, bulkUpdateResponseFileName);
//
//                                String errorResponseActual = xlsUtils.getCellData(sheetName, 15, 6);
//
//                                String errorResponseExpected = "Failure :(Basic Information - Performance Computation Calculation,I18n Syntax Error In Following Expression: " + mapQuery + " ";
//
//                                if (!errorResponseActual.equalsIgnoreCase(errorResponseExpected)) {
//
//                                    logger.error("Error Response Expected and Actual are not equal");
//                                    customAssert.assertTrue(false, "Error Response Expected and Actual are not equal");
//                                }
//
//                            } else {
//                                customAssert.assertTrue(false, "Bulk Update Response File SCP Done unsuccessfully");
//                            }
//                        }


                        //End Code For Scp

//                    } else {
//                        customAssert.assertTrue(false, "Bulk Update done unsuccessfully Actual Bulk Update Status " + bulkUpdateUploadResponse);
//                    }

//                } else {
//                    customAssert.assertTrue(false, "Bulk Update Template Downloaded unsuccessfully ");
//                }

//            }catch (Exception e){
//                logger.error("Exception while validating Bulk Update Error Scenario");
//                customAssert.assertTrue(false,"Exception while validating Bulk Update Error Scenario");
//            }
//
//            logger.info("Bulk Update Error Scenario Ends Here ");

            //Bulk Create
//            logger.info("Validating Bulk Create Error Scenario");
//
//            try {
//
//                int bulkCreateTemplateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"slbulkcreatetemplateid"));
//                String contractId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"fx function","contractid");
//
//                Map<String, String> payloadMap = new HashMap<>();
//                payloadMap.put("parentEntityTypeId",String.valueOf(contractEntityTypeId));
//                payloadMap.put("parentEntityId",contractId);
//                payloadMap.put("_csrf_token","48f4c2a5-38c3-4ab9-b96e-4163ee961df4");
//
//                UploadBulkData uploadBulkData = new UploadBulkData();
//                String bulkCreateFileName = "BulkCreateTemplate.xlsm";
//
//                BulkTemplate.uploadBulkCreateTemplate(uploadFilePath, bulkCreateFileName, contractEntityTypeId, Integer.parseInt(contractId),
//                        slEntityTypeId, bulkCreateTemplateId);
//                String bulkCreateResponse = uploadBulkData.getUploadBulkDataJsonStr();
//
//                if(!bulkCreateResponse.contains("200")){
//                    customAssert.assertTrue(false,"Bulk Create Response Unsuccessful");
//                }else {
//                    Thread.sleep(10000);
//                    String sqlQuery = "select id from bulk_edit_request order by id desc limit 1;";
//
//                    List<List<String>> sqlResult = postgreSQLJDBC.doSelect(sqlQuery);
//
//                    String bulkEditRequestId = sqlResult.get(0).get(0);
//
//                    //Code for scp
//
//                    Thread.sleep(6000);
//
//                    String remoteFilePath = "/data/temp-session/bulktask/" + bulkEditRequestId;
//
//                    String remoteFileName = "BulkCreateTemplate.xlsm";
//
//                    String localDir = "src\\test\\resources\\TestConfig\\ServiceLevel\\Fx Function\\";
//
//                    String localFileName = "BulkCreateTemplate.xlsm";
//
//                    Boolean scpStatus = scpUtils.getFileFromRemoteServerToLocalServer(remoteFilePath,remoteFileName,localDir,localFileName);
//
//                    if(scpStatus) {
//
//                        XLSUtils xlsUtils = new XLSUtils(localDir, localFileName);
//
//                        String errorResponseActual = xlsUtils.getCellData("Sla", 103, 6);
//
//                        if (!errorResponseActual.contains("Failure")) {
//
//                            logger.error("Error Response Expected and Actual are not equal");
//                            customAssert.assertTrue(false, "Error Response Expected and Actual are not equal");
//                        }
//
//                        if (!errorResponseActual.contains(mapQuery)) {
//
//                            logger.error("Error Response Expected and Actual are not equal map Query");
//                            customAssert.assertTrue(false, "Error Response Expected and Actual are not equal map Query");
//                        }
//
//                    }else {
//                        customAssert.assertTrue(false,"Bulk Create Response File SCP Done unsuccessfully");
//                    }
//
//                }
//
//
//            }catch (Exception e){
//                logger.error("Exception while validating Bulk Create Error Scenario");
//                customAssert.assertTrue(false,"Exception while validating Bulk Create Error Scenario");
//            }

            logger.info("Bulk Create Error Scenario Ends Here ");

        }catch (Exception e){
            logger.error("Exception while validating frequency module test cases " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while validating frequency module test cases " + e.getStackTrace());
        }finally {
//            postgreSQLJDBC.closeConnection();
        }

        customAssert.assertAll();

    }

    private Boolean updatePCQOnSL(int slId,String updatedPCQ,CustomAssert customAssert){

        Boolean updateStatus = true;
        Edit edit = new Edit();
        try{

            String editResponse = edit.hitEdit(slEntity,slId);

            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("performanceComputationCalculationQuery").put("values",updatedPCQ);

            String editPayload = editResponseJson.toString();

            editResponse = edit.hitEdit(slEntity,editPayload);

            if(!editResponse.contains("success") ){
                logger.error("PCQ updated unsuccessfully on SL ID " + slId);
                updateStatus = false;
            }

        }catch (Exception e) {

            logger.error("Exception while updating PCQ on SL " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while updating PCQ on SL " + e.getStackTrace());
            updateStatus = false;
        }
        return updateStatus;
    }

    private Boolean updatePCQOnSL(int slId,String updatedPCQ,String expectedMsg,CustomAssert customAssert){

        Boolean updateStatus = true;
        Edit edit = new Edit();
        try{

            String editResponse = edit.hitEdit(slEntity,slId);

            JSONObject editResponseJson = new JSONObject(editResponse);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

            editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("performanceComputationCalculationQuery").put("values",updatedPCQ);

            String editPayload = editResponseJson.toString();

            editResponse = edit.hitEdit(slEntity,editPayload);

            if(!editResponse.contains(expectedMsg) ){
                logger.error("PCQ updated unsuccessfully on SL ID " + slId);
                updateStatus = false;
            }

        }catch (Exception e) {

            logger.error("Exception while updating PCQ on SL " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while updating PCQ on SL " + e.getStackTrace());
            updateStatus = false;
        }
        return updateStatus;
    }

    private ArrayList<String> checkIfCSLCreatedOnServiceLevel(int serviceLevelId, CustomAssert customAssert) {

        logger.info("Checking if CSL created on service level");

        long timeSpent = 0;
        long cSLCreationTimeOut = 5000000L;
        long pollingTime = 5000L;
        ArrayList<String> childServiceLevelIds = new ArrayList<>();
        try {
            JSONObject tabListResponseJson;
            JSONArray dataArray = new JSONArray();

            TabListData tabListData = new TabListData();
            tabListData.hitTabListData(childServiceLevelTabId, slEntityTypeId, serviceLevelId,0,100);
            String tabListResponse = tabListData.getTabListDataResponseStr();

            if (JSONUtility.validjson(tabListResponse)) {

                while (timeSpent < cSLCreationTimeOut) {
                    logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                    Thread.sleep(pollingTime);

                    tabListData.hitTabListData(childServiceLevelTabId, slEntityTypeId, serviceLevelId,0,100);
                    tabListResponse = tabListData.getTabListDataResponseStr();

                    if (!JSONUtility.validjson(tabListResponse)) {

                        customAssert.assertTrue(false, "Service level tab Child Service Level has invalid Json Response for service level id " + serviceLevelId);
                        break;
                    }

                    tabListResponseJson = new JSONObject(tabListResponse);
                    dataArray = tabListResponseJson.getJSONArray("data");

                    if (dataArray.length() > 0) {

                        customAssert.assertTrue(true, "Child Service Level created successfully ");

                        childServiceLevelIds = (ArrayList) ListDataHelper.getColumnIds(tabListResponse);
                        break;
                    } else {
                        timeSpent += pollingTime;
                        logger.info("Child Service Level not created yet ");
                    }
                }
                if (childServiceLevelIds.size() == 0) {
//					customAssert.assertTrue(false, "Child Service level not created in " + cSLCreationTimeOut + " milli seconds for service level id " + serviceLevelId);
                }

            } else {
                customAssert.assertTrue(false, "Service level tab Child Service Level has invalid Json Response for service level id " + serviceLevelId);
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while checking child service level tab on ServiceLevel " + serviceLevelId + " " + e.getMessage());
        }

        return childServiceLevelIds;
    }

    private int getActiveServiceLevelId(String flowToTest, String PCQ, String DCQ, CustomAssert customAssert) {

        int serviceLevelId = -1;
        try {

            serviceLevelId = getServiceLevelId(flowToTest, PCQ, DCQ, customAssert);

            if (serviceLevelId != -1) {
                List<String> workFlowSteps;
                workFlowSteps = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "slactiveworkflowsteps").split("->"));
                if (!performWorkFlowActions(slEntityTypeId, serviceLevelId, workFlowSteps, customAssert)) {
                    customAssert.assertTrue(false, "Error while performing workflow actions on SL ID " + serviceLevelId);
                    return -1;
                }
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while getting an active an active service level id " + e.getMessage());
        }
        return serviceLevelId;
    }

    private Boolean performWorkFlowActions(int entityTypeId, int entityId, List<String> workFlowSteps, CustomAssert customAssert) {

        Boolean workFlowStepActionStatus;

        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();

        try {
            for (String workFlowStepToBePerformed : workFlowSteps) {

                workFlowStepActionStatus = workflowActionsHelper.performWorkFlowStepV2(entityTypeId,entityId,workFlowStepToBePerformed,customAssert);

                if (!workFlowStepActionStatus) {

                    customAssert.assertTrue(false, "Unable to perform workflow action " + workFlowStepToBePerformed + " on service level id " + entityId);
                    return false;
                }
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while performing Workflow actions for service level id " + entityId + e.getMessage());
            return false;

        }
        return true;
    }

    private int getServiceLevelId(String flowToTest, String PCQ, String DCQ, CustomAssert customAssert) {

        int serviceLevelId = -1;

        CreateEntity createEntity = new CreateEntity(slCreationConfigFilePath, slCreationConfigFileName,
                extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest);

        String createPayload = createEntity.getCreatePayload("service levels", true, false);
        //Updating payload according to PCQ
        if (!JSONUtility.validjson(createPayload)) {
            throw new SkipException("Couldn't get Create Payload as valid Json for Flow [" + flowToTest + "] Thus Skipping the test");
        }
        JSONObject createPayloadJson = new JSONObject(createPayload);
        createPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("performanceDataCalculationQuery").put("values", DCQ);
        createPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject("performanceComputationCalculationQuery").put("values", PCQ);
        createPayload = createPayloadJson.toString();

        String createResponse = null;

        if (createPayload != null) {
            logger.info("Hitting Create Api for Entity {}.", slEntity);
            Create createObj = new Create();
            createObj.hitCreate(slEntity, createPayload);
            createResponse = createObj.getCreateJsonStr();

            if (!ParseJsonResponse.validJsonResponse(createResponse)) {
                FileUtils.saveResponseInFile(slEntity + " Create API HTML.txt", createResponse);
            }
        }

        if (createResponse == null) {
            throw new SkipException("Couldn't get Create Response for Flow [" + flowToTest + "] Thus Skipping the test");
        }

        if (ParseJsonResponse.validJsonResponse(createResponse)) {
            JSONObject jsonObj = new JSONObject(createResponse);
            String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();

            logger.info("Create Status for Flow [{}]: {}", flowToTest, createStatus);

            if (createStatus.equalsIgnoreCase("success"))
                serviceLevelId = CreateEntity.getNewEntityId(createResponse, "service levels");

        } else {
            throw new SkipException("Couldn't get JSON Response for Create Flow [" + flowToTest + "] Thus Skipping the test");
        }
        return serviceLevelId;
    }

    private Boolean validateComputationStatusOnCSLAfterUpdatingPCQ(int serviceLevelId,int cslId,String PCQ,String expectedComputationStatus,CustomAssert customAssert){

        Boolean validationStatus = true;

        try{

            Boolean PCQUpdateStatus =  updatePCQOnSL(serviceLevelId,PCQ,customAssert);

            if(!PCQUpdateStatus){
                customAssert.assertTrue(false,"PCQ updated unsuccessfully");
            }

            WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
            String recomputeWorkflowStepName = "ReComputePerformance";
            Boolean workFlowActionStatus = workflowActionsHelper.performWorkFlowStepV2(cslEntityTypeId,cslId,recomputeWorkflowStepName,customAssert);

            if(!workFlowActionStatus){
                customAssert.assertTrue(false,"Recompute performed unsuccessfully for CSL");
                validationStatus = false;
                return validationStatus;
            }
            Thread.sleep(10000);

            Show show = new Show();
            show.hitShowVersion2(cslEntityTypeId,cslId);

            String showResponse = show.getShowJsonStr();

            JSONObject showResponseJson = new JSONObject(showResponse);

            String computationStatus = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("computationStatus").getJSONObject("values").get("name").toString();

            if(!computationStatus.equalsIgnoreCase(expectedComputationStatus)){
                customAssert.assertTrue(false,"Error in Computation while computing Performance");
                validationStatus = false;
            }


        }catch (Exception e){
            logger.error("Exception occurred while validating Computation Status On CSL After Updating PCQ");
            validationStatus = false;
        }

        return validationStatus;
    }

    private Boolean uploadPerformanceDataFormat(int entityId, int uploadId, int templateId, String performanceDataFormatFilePath, String performanceDataFormatFileName, String expectedMsg, CustomAssert customAssert) {

        logger.info("Uploading Performance Data Format on " + entityId);
        UploadBulkData uploadBulkData = new UploadBulkData();

        Map<String, String> payloadMap = new HashMap<>();
        payloadMap.put("parentEntityId", String.valueOf(entityId));
        payloadMap.put("parentEntityTypeId", String.valueOf(slEntityTypeId));

        uploadBulkData.hitUploadBulkData(uploadId, templateId, performanceDataFormatFilePath, performanceDataFormatFileName, payloadMap);

        String fileUploadResponse = uploadBulkData.getUploadBulkDataJsonStr();

        if (!fileUploadResponse.contains("200:;")) {

            customAssert.assertTrue(false, "Error while performance data format upload or SL Template Upload message has been changed");
            return false;
        }
        return true;
    }

    private Boolean validatePerformanceDataFormatTab(int entityId, String uploadFileName, CustomAssert customAssert) {

        JSONObject fileUploadDetailsJson;
        JSONObject tabListDataResponseJson;

        JSONArray indRowData;
        JSONArray dataArray = new JSONArray();

        String columnName;
        String columnValue;
        String tabListDataResponse;

        int performanceDataFormatTabId = 331;

        long timeSpent = 0L;
        long cSLPerformanceDataFormatTabTimeOut = 60000L;
        long pollingTime = 5000L;

        Boolean validationStatus = true;
        TabListData tabListData = new TabListData();

        try {
            Thread.sleep(3000);
            while (timeSpent < cSLPerformanceDataFormatTabTimeOut) {

                tabListData.hitTabListData(performanceDataFormatTabId, slEntityTypeId, entityId,0,50,"id","asc");

                tabListDataResponse = tabListData.getTabListDataResponseStr();

                if (JSONUtility.validjson(tabListDataResponse)) {

                    tabListDataResponseJson = new JSONObject(tabListDataResponse);
                    dataArray = tabListDataResponseJson.getJSONArray("data");

                    if (dataArray.length() >= 1) {
                        break;

                    }
                } else {
                    customAssert.assertTrue(false, "Performance Data Format Tab list Response is not a valid Json");
                    return false;
                }
                Thread.sleep(pollingTime);
                timeSpent = timeSpent + pollingTime;
            }
            //C10739
            if (uploadFileName.equalsIgnoreCase("")) {
                if (dataArray.length() != 0) {
                    customAssert.assertTrue(false, "Performance Data Format Tab data Count not equal to 0");
                    return false;
                } else return true;
            }

            fileUploadDetailsJson = dataArray.getJSONObject(0);

            indRowData = JSONUtility.convertJsonOnjectToJsonArray(fileUploadDetailsJson);

            for (int i = 0; i < indRowData.length(); i++) {

                columnName = indRowData.getJSONObject(i).get("columnName").toString();

                if (columnName.equalsIgnoreCase("filename")) {
                    columnValue = indRowData.getJSONObject(i).get("value").toString().split(":;")[0];
                    if (!columnValue.equalsIgnoreCase(uploadFileName)) {

                        customAssert.assertTrue(false, "Performance Data Format Tab Upload File Name Expected and Actual values mismatch");
                        customAssert.assertTrue(false, "Expected File Name : " + uploadFileName + " Actual File Name : " + columnValue);validationStatus = false;
                    }
                }

                if (columnName.equalsIgnoreCase("createdby")) {

                    columnValue = indRowData.getJSONObject(i).get("value").toString();

                    String createdBy = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "createdbyuser");
                    if (!columnValue.equalsIgnoreCase(createdBy)) {

                        customAssert.assertTrue(false, "Performance Data Format Tab created by field Expected and Actual values mismatch");
                        customAssert.assertTrue(false, "Expected createdby : " + createdBy + " Actual createdby : " + columnValue);
                        validationStatus = false;
                    }
                }
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating performance data format tab for SL ID " + entityId + " " + e.getMessage());
            validationStatus = false;
        }
        return validationStatus;
    }

    private Boolean uploadRawDataCSL(int cslId, String rawDataFileName, CustomAssert customAssert) {

        logger.info("Uploading Raw Data on child service level");

        Boolean uploadRawDataStatus = true;
        try {

            Map<String, String> payloadMap = new HashMap<>();
            payloadMap.put("parentEntityId", String.valueOf(cslId));
            payloadMap.put("parentEntityTypeId", String.valueOf(cslEntityTypeId));

            UploadRawData uploadRawData = new UploadRawData();
            uploadRawData.hitUploadRawData(uploadFilePath, rawDataFileName, payloadMap);
            String uploadRawDataString = uploadRawData.getUploadRawDataJsonStr();

//            if (uploadRawDataString.contains("200:;basic:;Your request has been successfully submitted")) {
            if (uploadRawDataString.contains("200:;")) {
                customAssert.assertTrue(true, "Raw data uploaded successfully on Child Service Level " + cslId);
                uploadRawDataStatus = true;
            } else {
                customAssert.assertTrue(false, "Raw data uploaded unsuccessfully on Child Service Level " + cslId + "");
                uploadRawDataStatus = false;
                return uploadRawDataStatus;
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while uploading raw data on Child Service Level Id " + cslId + " " + e.getMessage());
            uploadRawDataStatus = false;
        }
        return uploadRawDataStatus;
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

    @AfterClass
    public void afterClass() {

        logger.debug("Number CSL To Delete " + cslToDelete.size());
        EntityOperationsHelper.deleteMultipleRecords("child service levels",cslToDelete);

        logger.debug("Number SL To Delete " + slToDelete.size());
        EntityOperationsHelper.deleteMultipleRecords("service levels",slToDelete);
    }

    private Boolean validateStructuredPerformanceDataCSL(int CSLId, String expectedFileName, String computationStatus, String expectedPerformanceData, String expectedCompletedBy, CustomAssert customAssert) {

        logger.info("Validating Structured Performance Data tab on CSL " + CSLId);
        Boolean validationStatus = true;
        long timeSpent = 0;
        long fileUploadTimeOut = 60000L;
        long pollingTime = 5000L;
        int structuredPerformanceDataTabId = 207;
        JSONArray dataArray = new JSONArray();

        try {
            JSONObject tabListResponseJson;

            TabListData tabListData = new TabListData();
            tabListData.hitTabListData(structuredPerformanceDataTabId, cslEntityTypeId, CSLId);
            String tabListResponse = tabListData.getTabListDataResponseStr();

            if (JSONUtility.validjson(tabListResponse)) {

                while (timeSpent < fileUploadTimeOut) {
                    logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                    Thread.sleep(pollingTime);

                    tabListData.hitTabListData(structuredPerformanceDataTabId, cslEntityTypeId, CSLId);
                    tabListResponse = tabListData.getTabListDataResponseStr();

                    if (!JSONUtility.validjson(tabListResponse)) {

                        customAssert.assertTrue(false, "Structured Performance Data tab in Child Service Level has invalid Json Response for child service level id " + CSLId);
                        break;
                    }

                    tabListResponseJson = new JSONObject(tabListResponse);
                    dataArray = tabListResponseJson.getJSONArray("data");

                    //Case when bulk upload is not done and no entry is expected
                    if (expectedFileName.equalsIgnoreCase("")) {
                        if (dataArray.length() == 0) {

                            customAssert.assertTrue(true, "Expected : No Row is expected under Performance Data Tab For CSL " +
                                    CSLId + "Actual : Row Doesn't exists");
                        } else {
                            customAssert.assertTrue(false, "Expected : No Row is expected under Performance Data Tab For CSL " +
                                    CSLId + "Actual : Row exists");
                            validationStatus = false;
                        }
                        return validationStatus;
                    }

                    if (dataArray.length() > 0) {

                        customAssert.assertTrue(true, "Raw Data File Upload row created");
                        break;
                    } else {
                        timeSpent += pollingTime;
                        logger.info("Raw Data File not Uploaded yet");
                    }
                }
                if (dataArray.length() == 0) {
                    customAssert.assertTrue(false, "Raw Data File not Uploaded in " + fileUploadTimeOut + " milli seconds");
                    validationStatus = false;
                    return validationStatus;
                }
            } else {
                customAssert.assertTrue(false, "Raw Data File not Uploaded has invalid Json Response for child service level id " + CSLId);
                validationStatus = false;
                return validationStatus;
            }
            Thread.sleep(15000);

            tabListData.hitTabListData(structuredPerformanceDataTabId, cslEntityTypeId, CSLId);
            tabListResponse = tabListData.getTabListDataResponseStr();
            tabListResponseJson = new JSONObject(tabListResponse);
            dataArray = tabListResponseJson.getJSONArray("data");

            JSONObject individualRowData = dataArray.getJSONObject(dataArray.length() - 1);

            JSONArray individualRowDataJsonArray = JSONUtility.convertJsonOnjectToJsonArray(individualRowData);
            JSONObject individualColumnJson;
            String columnName;
            String columnValue;
            for (int i = 0; i < individualRowDataJsonArray.length(); i++) {

                individualColumnJson = individualRowDataJsonArray.getJSONObject(i);

                columnName = individualColumnJson.get("columnName").toString();
                columnValue = individualColumnJson.get("value").toString();


                if (columnName.equalsIgnoreCase("filename")) {

                    if (expectedFileName.equalsIgnoreCase("Check for Snow File")) {

                        String fileName = columnValue.split(":;")[0];

                        if (fileName ==null) {

                            customAssert.assertTrue(false, "Structure Performance Data File Name Expected : " + expectedFileName + " Actual File Name : " + fileName);
                            validationStatus = false;
                        }

                    } else {

                        String fileName = columnValue.split(":;")[0];

                        if (!fileName.equalsIgnoreCase(expectedFileName)) {
                            customAssert.assertTrue(false, "Structure Performance Data File Name Expected : " + expectedFileName + " Actual File Name : " + fileName);
                            validationStatus = false;
                        }
                    }
                }

                if (columnName.equalsIgnoreCase("status")) {

                    String status = columnValue.split(":;")[0];

                    if (!status.equalsIgnoreCase(computationStatus)) {
                        customAssert.assertTrue(false, "Structure Performance Data Status Expected : " + computationStatus + " Actual Status : " + status);
                        validationStatus = false;
                    }
                }

                if (columnName.equalsIgnoreCase("performancedata")) {

                    if (expectedPerformanceData == null) {

                    } else {
                        String performanceData = columnValue.split(":;")[0];

                        if (!performanceData.equalsIgnoreCase(expectedPerformanceData)) {
                            customAssert.assertTrue(false, "Structure Performance Data Performance Data Expected : " + expectedPerformanceData + " Actual Status : " + performanceData);
                            validationStatus = false;
                        }
                    }
                }

                if (columnName.equalsIgnoreCase("completedby")) {

                    String completedBy = columnValue.split(":;")[0];

                    if (!completedBy.equalsIgnoreCase(expectedCompletedBy)) {
                        customAssert.assertTrue(false, "Structure Performance Data CompletedBy : " + expectedCompletedBy + " Actual completedBy : " + completedBy);
                        validationStatus = false;
                    }
                }

                if (columnName.equalsIgnoreCase("timeofaction")) {

                    String timeOfAction = columnValue.split(":;")[0];
//					String currentDate = DateUtils.getCurrentDateInMMM_DD_YYYY();
//					String previousDate = DateUtils.getPreviousDateInMMM_DD_YYYY(currentDate);
//					String nextDate = DateUtils.getNextDateInDDMMYYYY(currentDate);

//					if (!(timeOfAction.contains(currentDateIn_MMM_DD_YYYY_Format) || timeOfAction.contains(previousDateIn_MMM_DD_YYYY_Format) || timeOfAction.contains(nextDateIn_MMM_DD_YYYY_Format))) {
//						customAssert.assertTrue(false, "Structure Performance Data TimeOfAction Expected value : " + currentDateIn_MMM_DD_YYYY_Format + " Actual timeOfAction : " + timeOfAction);
//						validationStatus = false;
//					}
                }
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating Structured Performance Data on CSL " + CSLId + " " + e.getMessage());
            validationStatus = false;
        }

        return validationStatus;

    }
}
