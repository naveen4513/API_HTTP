package com.sirionlabs.test.serviceLevel;

import com.sirionlabs.api.bulkedit.BulkeditEdit;
import com.sirionlabs.api.bulkupload.UploadBulkData;
import com.sirionlabs.api.bulkupload.UploadRawData;
import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.DownloadListWithData;
import com.sirionlabs.api.listRenderer.SLDetails;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.servicelevel.ServiceLevelHelper;
import com.sirionlabs.utils.commonUtils.*;
//import jdk.javadoc.internal.doclets.toolkit.util.ClassUseMapper;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.HttpResponse;
import org.apache.kafka.common.protocol.types.Field;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.*;
import com.sirionlabs.helper.EntityOperationsHelper;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TestChildServiceLevelAutomation2 {
    private final static Logger logger = LoggerFactory.getLogger(TestChildServiceLevelAutomation2.class);

    private String slCreationConfigFilePath = null;
    private String slCreationConfigFileName = null;
    private String extraFieldsConfigFilePath = null;
    private String extraFieldsConfigFileName = null;
    private String configFilePath = null;
    private String configFileName = null;

    private String slEntity = "service levels";
    private String cslEntity = "child service levels";
    private String uploadFilePath;
    private String downloadFilePath;
    private String bulkUpdateFilename;

    private String auditLogUser;
    private String completedBy;
    private String rawDataFileValidMsg;
    private Integer slEntityTypeId;
    private Integer cslEntityTypeId;

    private int serviceLevelId1;
    private int serviceLevelId12;

    private int cslBulkUpdateTemplateId;
    private int slMetaDataUploadTemplateId;
    private int uploadIdSL_PerformanceDataTab;
    private final int childServiceLevelTabId = 7;

    private ArrayList<String> childserviceLevelId1s = new ArrayList<>();
    private ArrayList<String> childserviceLevelId1s1 = new ArrayList<>();
    private ArrayList<String> childserviceLevelId1sForFlowDownOfValues = new ArrayList<>();

    private String slTemplateFileName;
    private ArrayList<Integer> slToDelete = new ArrayList<>();
    private ArrayList<Integer> cslToDelete = new ArrayList<>();

//	PostgreSQLJDBC postgreSQLJDBC;

    private String currentDateIn_MM_DD_YYYY_Format;
    private String previousDateIn_MM_DD_YYYY_Format;
    private String nextDateIn_MM_DD_YYYY_Format;

    private String currentDateIn_MMM_DD_YYYY_Format;
    private String previousDateIn_MMM_DD_YYYY_Format;
    private String nextDateIn_MMM_DD_YYYY_Format;

    private String[] workFlowActionToPerformCSLComputePerformance;
    private List<List<String>> currentTimeStampForSLTemplateUploadCSLCreation;

    static int randomFileNumber = 1;

    String dbHostName;
    String dbPortName;
    String dbName;
    String dbUserName;
    String dbPassowrd;

    @BeforeClass(groups = {"sanity","sprint"})
    public void beforeClass() throws ConfigurationException {

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFileName");

        slCreationConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ServiceLevelsFilePath");
        slCreationConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceLevelsFileName");

        extraFieldsConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ServiceLevelsFilePath");
        extraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceLevelsExtraFieldsFileName");
        slEntityTypeId = ConfigureConstantFields.getEntityIdByName("service levels");
        cslEntityTypeId = ConfigureConstantFields.getEntityIdByName("child service levels");

        uploadFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "uploadfilepath");
        downloadFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "downloadfilepath");
        bulkUpdateFilename = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulkupdatefilename");
        cslBulkUpdateTemplateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "cslbulkupdatetemplateid"));
        slMetaDataUploadTemplateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "slmetadatauploadtemplateid"));
        uploadIdSL_PerformanceDataTab = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "uploadidslperformancecdatatab"));
        auditLogUser = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "auditloguser");
        completedBy = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "completedby");
        rawDataFileValidMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilesuccessmsg");
        slTemplateFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilename");

        currentDateIn_MM_DD_YYYY_Format = DateUtils.getCurrentDateInMM_DD_YYYY();
        previousDateIn_MM_DD_YYYY_Format = DateUtils.getPreviousDateInMM_DD_YYYY(currentDateIn_MM_DD_YYYY_Format);

        currentDateIn_MMM_DD_YYYY_Format = DateUtils.getCurrentDateInMMM_DD_YYYY();
        previousDateIn_MMM_DD_YYYY_Format = DateUtils.getPreviousDateInMMM_DD_YYYY(currentDateIn_MMM_DD_YYYY_Format);

        workFlowActionToPerformCSLComputePerformance = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "cslperformancecalulation").split("->");

        dbHostName = ConfigureEnvironment.getEnvironmentProperty("dbHostAddress");
        dbPortName = ConfigureEnvironment.getEnvironmentProperty("dbPortName");
        dbName = ConfigureEnvironment.getEnvironmentProperty("dbName");
        dbUserName = ConfigureEnvironment.getEnvironmentProperty("dbUserName");
        dbPassowrd = ConfigureEnvironment.getEnvironmentProperty("dbPassword");

        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostName,dbPortName,dbName,dbUserName,dbPassowrd);

        try {
//			postgreSQLJDBC.deleteDBEntry("delete from system_emails");
        } catch (Exception e) {
            logger.error("Exception while deleting from system_emails table");
        }finally {
            postgreSQLJDBC.closeConnection();
        }

    }

    //Always set parallel to false else test fails
    @DataProvider(name = "bulkCreateFlows", parallel = false)
    public Object[][] bulkCreateFlows() {

        List<Object[]> allTestData = new ArrayList<>();
        String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,"bulk create", "flows to test").split(",");

        for (String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @DataProvider(name = "slMetCalculationThroughES", parallel = false)
    public Object[][] slMetCalculationThroughES() {

        List<Object[]> allTestData = new ArrayList<>();

        String DCQ = "";
        String PCQ = "";
        String expectedSLMetValue = "";
        int cslIdToUse = -1;
        String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "estargetvaluescalculation", "flowstotest").split(",");

        for (String flowToTest : flowsToTest) {

            if (flowToTest.equalsIgnoreCase("SLMet0")) {

                PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\", \"init_script\": \"state['map'] = ['met':0, 'notMet':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30,'SL_Met':0]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;params.result.Actual_Numerator = 20;params.result.Actual_Denominator = 100;params.result.Supplier_Numerator = 10;params.result.Supplier_Denominator = 0;params.result.Final_Numerator = 10;params.result.Final_Denominator = 30;} return params.result\", \"combine_script\": \"return state;\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
                DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Met/Missed\": {\"script\": \"if(params['_source']['Time Taken (Seconds1)'] != ''){if(params['_source']['Time Taken (Seconds)'] < 110000){return 'Met'}else{return 'Missed'}}else{return 'Missed'}\"}}}";
//				expectedSLMetValue = "Met Expected";
                expectedSLMetValue = "Met Exp"; //BA1
                cslIdToUse = 5;
            } else if (flowToTest.equals("SLMet1")) {

                PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\", \"init_script\": \"state['map'] = ['met':0, 'notMet':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30,'SL_Met':1]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;params.result.Actual_Numerator = 20;params.result.Actual_Denominator = 100;params.result.Supplier_Numerator = 10;params.result.Supplier_Denominator = 0;params.result.Final_Numerator = 10;params.result.Final_Denominator = 30;} return params.result\", \"combine_script\": \"return state;\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
                DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Met/Missed\": {\"script\": \"if(params['_source']['Time Taken (Seconds1)'] != ''){if(params['_source']['Time Taken (Seconds)'] < 110000){return 'Met'}else{return 'Missed'}}else{return 'Missed'}\"}}}";
                expectedSLMetValue = "Not Met";
                cslIdToUse = 6;

            } else if (flowToTest.equals("SLMet2")) {

                PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\", \"init_script\": \"state['map'] = ['met':0, 'notMet':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30,'SL_Met':2]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;params.result.Actual_Numerator = 20;params.result.Actual_Denominator = 100;params.result.Supplier_Numerator = 10;params.result.Supplier_Denominator = 0;params.result.Final_Numerator = 10;params.result.Final_Denominator = 30;} return params.result\", \"combine_script\": \"return state;\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
                DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Met/Missed\": {\"script\": \"if(params['_source']['Time Taken (Seconds1)'] != ''){if(params['_source']['Time Taken (Seconds)'] < 110000){return 'Met'}else{return 'Missed'}}else{return 'Missed'}\"}}}";
                expectedSLMetValue = "Met Min";
                cslIdToUse = 7;

            } else if (flowToTest.equals("SLMet3")) {

                PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\", \"init_script\": \"state['map'] = ['met':0, 'notMet':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30,'SL_Met':3]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;params.result.Actual_Numerator = 20;params.result.Actual_Denominator = 100;params.result.Supplier_Numerator = 10;params.result.Supplier_Denominator = 0;params.result.Final_Numerator = 10;params.result.Final_Denominator = 30;} return params.result\", \"combine_script\": \"return state;\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
                DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Met/Missed\": {\"script\": \"if(params['_source']['Time Taken (Seconds1)'] != ''){if(params['_source']['Time Taken (Seconds)'] < 110000){return 'Met'}else{return 'Missed'}}else{return 'Missed'}\"}}}";
                expectedSLMetValue = "Met Exp";
                cslIdToUse = 8;

            } else if (flowToTest.equals("SLMet4")) {

                PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\", \"init_script\": \"state['map'] = ['met':0, 'notMet':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30,'SL_Met':4]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;params.result.Actual_Numerator = 20;params.result.Actual_Denominator = 100;params.result.Supplier_Numerator = 10;params.result.Supplier_Denominator = 0;params.result.Final_Numerator = 10;params.result.Final_Denominator = 30;} return params.result\", \"combine_script\": \"return state;\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
                DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Met/Missed\": {\"script\": \"if(params['_source']['Time Taken (Secondss1)'] != ''){if(params['_source']['Time Taken (Seconds)'] < 110000){return 'Met'}else{return 'Missed'}}else{return 'Missed'}\"}}}";
                expectedSLMetValue = "Not Reported";
                cslIdToUse = 9;

            } else if (flowToTest.equals("SLMet5")) {

                PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\", \"init_script\": \"state['map'] = ['met':0, 'notMet':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30,'SL_Met':5]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;params.result.Actual_Numerator = 20;params.result.Actual_Denominator = 100;params.result.Supplier_Numerator = 10;params.result.Supplier_Denominator = 0;params.result.Final_Numerator = 10;params.result.Final_Denominator = 30;} return params.result\", \"combine_script\": \"return state;\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
                DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Met/Missed\": {\"script\": \"if(params['_source']['Time Taken (Secondss1)'] != ''){if(params['_source']['Time Taken (Seconds)'] < 110000){return 'Met'}else{return 'Missed'}}else{return 'Missed'}\"}}}";
                expectedSLMetValue = "No Data Available";
                cslIdToUse = 10;

            } else if (flowToTest.equals("SLMet6")) {

                PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\", \"init_script\": \"state['map'] = ['met':0, 'notMet':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30,'SL_Met':6]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;params.result.Actual_Numerator = 20;params.result.Actual_Denominator = 100;params.result.Supplier_Numerator = 10;params.result.Supplier_Denominator = 0;params.result.Final_Numerator = 10;params.result.Final_Denominator = 30;} return params.result\", \"combine_script\": \"return state;\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
                DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Met/Missed\": {\"script\": \"if(params['_source']['Time Taken (Secondss1)'] != ''){if(params['_source']['Time Taken (Seconds)'] < 110000){return 'Met'}else{return 'Missed'}}else{return 'Missed'}\"}}}";
                expectedSLMetValue = "Not Applicable";
                cslIdToUse = 11;

            } else if (flowToTest.equals("SLMet7")) {

                PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\", \"init_script\": \"state['map'] = ['met':0, 'notMet':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30,'SL_Met':7]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;params.result.Actual_Numerator = 20;params.result.Actual_Denominator = 100;params.result.Supplier_Numerator = 10;params.result.Supplier_Denominator = 0;params.result.Final_Numerator = 10;params.result.Final_Denominator = 30;} return params.result\", \"combine_script\": \"return state;\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
                DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Met/Missed\": {\"script\": \"if(params['_source']['Time Taken (Secondss1)'] != ''){if(params['_source']['Time Taken (Seconds)'] < 110000){return 'Met'}else{return 'Missed'}}else{return 'Missed'}\"}}}";
                expectedSLMetValue = "Work In Progress";
                cslIdToUse = 12;

            } else if (flowToTest.equals("SLMet8")) {

                PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\", \"init_script\": \"state['map'] = ['met':0, 'notMet':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30,'SL_Met':8]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;params.result.Actual_Numerator = 20;params.result.Actual_Denominator = 100;params.result.Supplier_Numerator = 10;params.result.Supplier_Denominator = 0;params.result.Final_Numerator = 10;params.result.Final_Denominator = 30;} return params.result\", \"combine_script\": \"return state;\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
                DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Met/Missed\": {\"script\": \"if(params['_source']['Time Taken (Secondss1)'] != ''){if(params['_source']['Time Taken (Seconds)'] < 110000){return 'Met'}else{return 'Missed'}}else{return 'Missed'}\"}}}";
//				expectedSLMetValue = "Low Volume Rajesh 123";
                expectedSLMetValue = "LV";
                cslIdToUse = 13;

            } else if (flowToTest.equals("SLMet9")) {

                PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\", \"init_script\": \"state['map'] = ['met':0, 'notMet':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30,'SL_Met':9]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;params.result.Actual_Numerator = 20;params.result.Actual_Denominator = 100;params.result.Supplier_Numerator = 10;params.result.Supplier_Denominator = 0;params.result.Final_Numerator = 10;params.result.Final_Denominator = 30;} return params.result\", \"combine_script\": \"return state;\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
                DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Met/Missed\": {\"script\": \"if(params['_source']['Time Taken (Secondss1)'] != ''){if(params['_source']['Time Taken (Seconds)'] < 110000){return 'Met'}else{return 'Missed'}}else{return 'Missed'}\"}}}";
                expectedSLMetValue = "Met Significantly Minimum";
                cslIdToUse = 14;
            }

            allTestData.add(new Object[]{flowToTest, DCQ, PCQ, expectedSLMetValue,cslIdToUse});
        }
        return allTestData.toArray(new Object[0][]);
    }

    //Always set parallel to false else test fails
    @DataProvider(name = "timeDifferenceFunctionCalculationDifferentFlows", parallel = false)
    public Object[][] timeDifferenceFunctionCalculationDifferentFlows() {

        List<Object[]> allTestData = new ArrayList<>();
        String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "time_difference_calculation_diff_flows","dateformatstovalidate").split(",");

        for (String flowToTest : flowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(groups = {"sanity","PCQ DCQ UDC Update1","PCQ DCQ UDC Update2"},enabled = true)
    public void TestCSLCreation() {

        CustomAssert customAssert = new CustomAssert();

        try {

            executeParallelServiceToGetActiveServiceLevelId();

            slToDelete.add(serviceLevelId1);
            slToDelete.add(serviceLevelId12);

            if (serviceLevelId1 != -1) {

                childserviceLevelId1s = checkIfCSLCreatedOnServiceLevel(serviceLevelId1, customAssert);
                childserviceLevelId1s1 = checkIfCSLCreatedOnServiceLevel(serviceLevelId12, customAssert);

                addCSLToDelete(childserviceLevelId1s);
                addCSLToDelete(childserviceLevelId1s1);

                int numberOfChildServiceLevel = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "numberofchildservicelevel"));

                if (!(childserviceLevelId1s.size() >= numberOfChildServiceLevel)) {

                    customAssert.assertTrue(false, "For Service Level Id " + serviceLevelId1 + " Number of Child Service Level Expected are " + numberOfChildServiceLevel + " Actual " + childserviceLevelId1s.size());
                    customAssert.assertAll();
                }

                if (!(childserviceLevelId1s1.size() >= numberOfChildServiceLevel)) {

                    customAssert.assertTrue(false, "For Service Level Id 2 " + serviceLevelId12 + " Number of Child Service Level Expected are " + numberOfChildServiceLevel + " Actual " + childserviceLevelId1s1.size());
                    customAssert.assertAll();
                }

                String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilename");
                String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");

                if (!uploadPerformanceDataFormat(serviceLevelId1, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
                    customAssert.assertTrue(false, "Error while performance data file upload");
                }

                if (!validatePerformanceDataFormatTab(serviceLevelId1, performanceDataFormatFileName, customAssert)) {

                    customAssert.assertTrue(false, "Error while validating Performance Data Format Tab");
                }
//
//                if (!validateDownloadFileFromPerformanceTab(serviceLevelId1, performanceDataFormatFileName + RandomNumbers.getRandomNumberWithinRange(1, 10), performanceDataFormatFileName, customAssert)) {
//                    customAssert.assertTrue(false, "Error while validating Download File From Performance Tab for SL ID " + serviceLevelId1);
//                }
//
                if (!uploadPerformanceDataFormat(serviceLevelId12, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
                    customAssert.assertTrue(false, "Error while performance data file upload for Service Level 2");
                }

                if (!validatePerformanceDataFormatTab(serviceLevelId12, performanceDataFormatFileName, customAssert)) {

                    customAssert.assertTrue(false, "Error while validating Performance Data Format Tab for Service Level 2");
                }

//                if (!validateDownloadFileFromPerformanceTab(serviceLevelId12, performanceDataFormatFileName + RandomNumbers.getRandomNumberWithinRange(1, 10), performanceDataFormatFileName, customAssert)) {
//                    customAssert.assertTrue(false, "Error while validating Download File From Performance Tab for service level  2" );
//                }
            }

        } catch (Exception e) {
            logger.error("Exception while creation of CSL");
            customAssert.assertTrue(false, "Exception while creation of CSL" + e.getMessage());
        }

        customAssert.assertAll();
    }

    @Test(groups = {"sanity","PCQ DCQ UDC Update1"},dependsOnMethods = "TestCSLCreation",enabled = true)
    public void TestDownloadRawDataFormat(){

        CustomAssert customAssert = new CustomAssert();

        try{
            Map<String,String> urlStringMap = new HashMap<>();

            urlStringMap.put("entityId",String.valueOf(serviceLevelId1));
            urlStringMap.put("entityTypeId",String.valueOf(slEntityTypeId));
            urlStringMap.put("tabList","true");
            Show show = new Show();

            show.hitShowVersion2(slEntityTypeId,serviceLevelId1);

            String showResponse  = show.getShowJsonStr();

            String slID = ShowHelper.getValueOfField("short code id",showResponse);

            urlStringMap.put("clientEntitySeqId",slID);

            int entityUrlId = 331;

            DownloadListWithData downloadListWithData = new DownloadListWithData();

            Map<String, String> formParam = downloadListWithData.getDownloadListWithDataPayload(slEntityTypeId);

            logger.info("formParam is : [{}]", formParam);

            HttpResponse response = downloadListWithData.hitListRendererDownload(formParam,urlStringMap, entityUrlId);

            FileUtils fileUtils = new FileUtils();
            String outputFilePath = "src\\test\\resources\\TestConfig\\ServiceLevel\\SLAutomation\\SIR_529";
            Boolean fileDownloadStatus = fileUtils.dumpDownloadListWithDataResponseIntoFile(response, outputFilePath, ".xlsm","RawDataFormat", slEntity);

            if(!fileDownloadStatus){
                customAssert.assertTrue(false,"File Download done unsuccessfully for SL ID " + serviceLevelId1);
            }
            String actualColumnName;
            for(int i =0;i<5;i++) {

                actualColumnName = XLSUtils.getOneCellValue(outputFilePath, "RawDataFormat.xlsm", "Format Sheet", 0, i);

                switch (i) {
                    case 0:
                        if (!actualColumnName.equalsIgnoreCase("Column Name"))
                            customAssert.assertTrue(false, "Expected column name in Format Sheet " + "Column Name" + "Actual column name in Format Sheet " + actualColumnName);
                        break;
                    case 1:
                        if (!actualColumnName.equalsIgnoreCase("Column Type"))
                            customAssert.assertTrue(false, "Expected column name in Format Sheet " + "Column Type" + "Actual column name in Format Sheet " + actualColumnName);
                        break;
                    case 2:
                        if (!actualColumnName.equalsIgnoreCase("Format"))
                            customAssert.assertTrue(false, "Expected column name in Format Sheet " + "Format" + "Actual column name in Format Sheet " + actualColumnName);
                        break;
                    case 3:
                        if (!actualColumnName.equalsIgnoreCase("Type"))
                            customAssert.assertTrue(false, "Expected column name in Format Sheet " + "Type" + "Actual column name in Format Sheet " + actualColumnName);
                        break;
                    case 4:
                        if (!actualColumnName.equalsIgnoreCase("Sirion Function"))
                            customAssert.assertTrue(false, "Expected column name in Format Sheet " + "Sirion Function" + "Actual column name in Format Sheet " + actualColumnName);
                        break;
                }


            }

            for(int i =0;i<3;i++) {

                actualColumnName = XLSUtils.getOneCellValue(outputFilePath, "RawDataFormat.xlsm", "MasterData", 0, i);

                switch (i) {
                    case 0:
                        if (!actualColumnName.equalsIgnoreCase("COLUMN_TYPE"))
                            customAssert.assertTrue(false, "Expected column name in MasterData Sheet " + "COLUMN_TYPE" + "Actual column name in Format Sheet " + actualColumnName);
                        break;
                    case 1:
                        if (!actualColumnName.equalsIgnoreCase("FORMAT"))
                            customAssert.assertTrue(false, "Expected column name in MasterData Sheet " + "FORMAT" + "Actual column name in Format Sheet " + actualColumnName);
                        break;
                    case 2:
                        if (!actualColumnName.equalsIgnoreCase("TYPE"))
                            customAssert.assertTrue(false, "Expected column name in MasterData Sheet " + "TYPE" + "Actual column name in Format Sheet " + actualColumnName);
                        break;
                }
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating Download RawData Format " + e.getMessage());
            logger.error("Exception while validating Download RawData Format " + e.getMessage());
        }
        customAssert.assertAll();
    }

    //C10440  C13523
    @Test(groups = {"sanity"},dependsOnMethods = "TestCSLCreation", enabled = true)     //Completed
    public void TestCSLComputationStatusDataNotUploaded() {

        CustomAssert customAssert = new CustomAssert();

        logger.info("Validating Child Service Level Computation Status");

        int cSLId = Integer.parseInt(childserviceLevelId1s.get(1));
        Show show = new Show();
        show.hitShowVersion2(cslEntityTypeId, cSLId);
        String showPageResponse = show.getShowJsonStr();

        String computationStatus = ShowHelper.getValueOfField("computationstatus", showPageResponse);

        if (!computationStatus.equalsIgnoreCase("Data Not Uploaded")) {

            customAssert.assertTrue(false, "Computation Status Expected \"Data Not Uploaded\" Actual Computation Status " + computationStatus);

        } else {
            customAssert.assertTrue(true, "Computation Status validated successfully");
        }

        customAssert.assertAll();
    }

    //C10440 C13524 C10774 C10775
    @Test(groups = {"sanity","PCQ DCQ UDC Update1"},dependsOnMethods = "TestCSLCreation", enabled = true)     //Completed
    public void TestCSLComputationStatusDataMarkedForAggregation() {

        CustomAssert customAssert = new CustomAssert();
        Show show = new Show();

        logger.info("Validating Child Service Level Computation Status");

        int cSLId = Integer.parseInt(childserviceLevelId1s.get(2));
        String rawDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilename");
        String completedBy = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "createdbyuser");
        if (!uploadRawDataCSL(cSLId, rawDataFileName, rawDataFileValidMsg, customAssert)) {

            customAssert.assertTrue(false, "Raw Data Uploaded unsuccessfully on CSL " + cSLId);

        } else {
            if (!validateStructuredPerformanceDataCSL(cSLId, rawDataFileName, "Done", "View Structured Data", completedBy, customAssert)) {
                customAssert.assertTrue(false, "Raw Data File Upload validation unsuccessful");
            }
        }

        show.hitShowVersion2(cslEntityTypeId, cSLId);
        String showPageResponse = show.getShowJsonStr();

        String computationStatus = ShowHelper.getValueOfField("computationstatus", showPageResponse);

        if (!computationStatus.equalsIgnoreCase("Data Marked for Aggregation")) {

            customAssert.assertTrue(false, "Computation Status Expected \"Data Marked for Aggregation\" Actual Computation Status " + computationStatus);

        } else {
            customAssert.assertTrue(true, "Computation Status validated successfully");
        }

        customAssert.assertAll();
    }

    @Test(groups = {"sanity","PCQ DCQ UDC Update1"},dependsOnMethods = "TestCSLCreation",enabled = true,priority = 0)
    public void TestIgnoreFileScenario(){

        CustomAssert customAssert = new CustomAssert();
        int cslID = Integer.parseInt(childserviceLevelId1s1.get(0));

        try {
            String rawDataFile = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilename");
            String timeStamp = getCurrentTimeStamp().get(0).get(0);
            if (!uploadRawDataCSL(cslID, rawDataFile, rawDataFileValidMsg, customAssert)) {
                customAssert.assertTrue(false, "Raw Data upload unsuccessful on CSL ID " + cslID);
            }
            if (!validateStructuredPerformanceDataCSL(cslID, rawDataFile, "Done", "View Structured Data", completedBy, customAssert)) {

                customAssert.assertTrue(false, "Error validating structure performance data tab on CSL ID " + cslID);
			}

            List<List<String>> recordFromSystemEmailTable;
            Show show = new Show();

            try {
                show.hitShowVersion2(slEntityTypeId, serviceLevelId1);
                String showResponse = show.getShowJsonStr();
                String shortCodeId = ShowHelper.getValueOfField("short code id", showResponse);
                String subjectLine = "SL Raw Data Metadata Upload " + shortCodeId;

                //				C10582

                recordFromSystemEmailTable= getRecordFromSystemEmailTable(subjectLine, currentTimeStampForSLTemplateUploadCSLCreation.get(0).get(0));
                if (recordFromSystemEmailTable.size() == 0) {
                    customAssert.assertTrue(false, "No entry in system email table for SL Raw Data Metadata Upload for SL " + shortCodeId);
                }

                String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilename");

                if (!validateAttachmentName(recordFromSystemEmailTable, performanceDataFormatFileName, customAssert)) {
                    customAssert.assertTrue(false, "Attachment Name validated unsuccessfully in system emails table for SL Raw Data Metadata Upload for SL " + shortCodeId);
                }


            }catch (Exception e){
                customAssert.assertTrue(false,"Exception while validating email for SL Template Upload");
            }

            timeStamp = getCurrentTimeStamp().get(0).get(0);
            if (!validateIgnoreFileScenario(cslID, customAssert)) {
                customAssert.assertTrue(false, "Ignore File Scenario validated unsuccessfully for CSL ID " + cslID);
            } else {
                try {

                    show.hitShowVersion2(cslEntityTypeId, cslID);
                    String showPageResponse = show.getShowJsonStr();

                    //C13573
                    String shortCodeId = ShowHelper.getValueOfField("short code id", showPageResponse);

//                  C13573 C10613 C13573
                    String subjectLine = "SL Computation ("+ shortCodeId +") -  ignore raw data file request response";
                    List<String> expectedSentencesInBody = new ArrayList<>();
                    expectedSentencesInBody.add("To calculate the updated scores, please re-compute the performance.");
                    expectedSentencesInBody.add("Your ignore raw data file request has been completed.");

                    recordFromSystemEmailTable = getRecordFromSystemEmailTable(subjectLine, timeStamp);

                    if (recordFromSystemEmailTable.size() == 0) {
                        customAssert.assertTrue(false, "No entry in system email table for subject Line " + subjectLine);
                    }

                    if (!validateAttachmentName(recordFromSystemEmailTable, subjectLine, customAssert)) {
                        customAssert.assertTrue(false, "Attachment Name validated unsuccessfully in system emails table for subjectLine " + subjectLine);
                    }

//					if (!validateSentSuccessfullyFlag(recordFromSystemEmailTable, customAssert)) {
//						customAssert.assertTrue(false, "Sent Successfully Flag validated unsuccessfully in system emails table for subjectLine " + subjectLine);
//					}

                    if (!validateBodyOfEmail(recordFromSystemEmailTable, expectedSentencesInBody, customAssert)) {
                        customAssert.assertTrue(false, "Body validated unsuccessfully in system emails table for subjectLine " + subjectLine);
                    }
                }catch (Exception e){
                    customAssert.assertTrue(false,"Exception while validating ignore file scenario email " + e.getStackTrace());
                }
            }

        } catch (Exception e) {
            customAssert.assertTrue(false,"Exception while validating ignore file scenario " + e.getStackTrace());
        }
        customAssert.assertAll();
    }

    //SIR-1088 SL Automation - Reading metadata value via ES query
    @Test(groups = {"sanity","PCQ DCQ UDC Update2"},dependsOnMethods = "TestCSLCreation",enabled = true)
    public void TestReadMetaDataViaEsQuery(){

        CustomAssert customAssert = new CustomAssert();

        String flowToFindInConfigFile = "read metadata via es query";

        //Adding performanceComputationCalculationQuery hardcoded as unable to read from cfg file properly
        String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\", \"init_script\": \"state['map'] = ['met':0, 'notMet':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;} return params.result\", \"combine_script\": \"return state;\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
        String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Service Module Name Check\": {\"script\": \"if(params['_source']['Service Module Name Test'] == '${Title}'){return 'Supported'}else{return 'NA'}\"}}}";

        try {

            String UDC = "Incident ID";

            int serviceLevelId1 = serviceLevelId12;

            ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
            Boolean editStatus = serviceLevelHelper.editPCQDCQUDC(serviceLevelId1,PCQ,DCQ,UDC,customAssert);

            if(!editStatus) {
                customAssert.assertTrue(false, "Error while editing PCQ DCQ UDC for SL");
            }

            String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToFindInConfigFile,"sltemplatefile");
            String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");

            String fileUploadPath = uploadFilePath + "/SIR_1088";

            if (!uploadPerformanceDataFormat(serviceLevelId1, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, fileUploadPath, performanceDataFormatFileName, expectedMsg, customAssert)) {
                customAssert.assertTrue(false, "Error while performance data file upload");
            }

            int childserviceLevelId1 = Integer.parseInt(childserviceLevelId1s1.get(4));

            String rawDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToFindInConfigFile,"rawdatafile");

            if (!uploadRawDataCSL(childserviceLevelId1,fileUploadPath, rawDataFileName, rawDataFileValidMsg, customAssert)) {
                customAssert.assertTrue(false, "Raw Data upload unsuccessful on CSL ID " + childserviceLevelId1);
            }

            if (!validateStructuredPerformanceDataCSL(childserviceLevelId1, rawDataFileName, "Done", "View Structured Data", completedBy, customAssert)) {
                customAssert.assertTrue(false, "Raw Data File Upload validation unsuccessful");

            }


            Thread.sleep(5000);

            String structurePerformanceDataResponse = serviceLevelHelper.getStructuredPerformanceData(childserviceLevelId1);
            String documentId = serviceLevelHelper.getDocumentIdFromCSLRawDataTab(structurePerformanceDataResponse);

            String payload = "{\"documentId\":" + documentId + ",\"offset\":0,\"size\":20,\"childSlaId\":" + childserviceLevelId1 +"}";

            SLDetails slDetails = new SLDetails();
            slDetails.hitSLDetailsList(payload);
            String slDetailsResponse = slDetails.getSLDetailsResponseStr();

            if(JSONUtility.validjson(slDetailsResponse)){

                JSONObject slDetailsResponseJson = new JSONObject(slDetailsResponse);
                JSONArray dataArray = slDetailsResponseJson.getJSONArray("data");
                JSONObject indRow;
                JSONArray indRowArray;
                String columnName;
                String columnValue = "";

                for(int i =0;i<2;i++){

                    try {
                        indRow = dataArray.getJSONObject(i);

                        indRowArray = JSONUtility.convertJsonOnjectToJsonArray(indRow);

                        //This logic is build in such a way that Raw data file must contain 2 rows only
                        // and 1st row should have "supported" text for "Check" fields and "NA" for 2 nd Row
                        for(int j =0;j<indRowArray.length();j++) {

                            columnName = indRowArray.getJSONObject(j).get("columnName").toString();

                            if (columnName.contains("Check")) {
                                columnValue = indRowArray.getJSONObject(j).get("columnValue").toString();

                                if (i == 0) {
                                    if (!columnValue.equalsIgnoreCase("Supported")) {
                                        customAssert.assertTrue(false, "Expected values of in row 1 for calculated values should be Supported " +
                                                "Either you have uploaded the wrong raw data file for row 1 or there is a bug");
                                    }
                                } else if (i == 1) {
                                    if (!columnValue.equalsIgnoreCase("NA")) {
                                        customAssert.assertTrue(false, "Expected values of in row 2 for calculated values should be NA " +
                                                "Either you have uploaded the wrong raw data file for row 2 or there is a bug");
                                    }
                                }
                            }
                        }

                    }catch (Exception e){
                        logger.warn("Number of data while opening view structure data is less than 2");
                        customAssert.assertTrue(false,"Number of data while opening view structure data is less than 2");
                    }
                }

                Boolean auditLogVerificationStatus = verifyAuditLog(cslEntityTypeId,childserviceLevelId1,"Performance data uploaded",completedBy,customAssert);

//				C10555
                if(!auditLogVerificationStatus){
                    customAssert.assertTrue(false,"Audit Log Validated unsuccessfully when DCQ is executed");
                }

            }else {
                logger.error("SL Details Response is not a valid json");
                customAssert.assertTrue(false,"SL Details Response is not a valid json");
            }

        }catch (Exception e){
            logger.error("Exception while Read MetaData Via Es Query " + e.getMessage());
        }

        customAssert.assertAll();
    }

    //    C10562
    @Test(groups = {"PCQ DCQ UDC Update1"},dependsOnMethods = "TestCSLCreation", enabled = true)         //Revalidated 17 june  //Completed
    public void TestUploadPerformanceDataFormatWrongFileType() {

        CustomAssert customAssert = new CustomAssert();

        String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "sltemplatefilenamepdf");

        String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadfailuremsgwrongfiletype");

        if (!uploadPerformanceDataFormat(serviceLevelId1, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
            customAssert.assertTrue(false, "Wrong data file upload validation unsuccessful");

        }

        performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "sltemplatefilenametxt");
        expectedMsg = "500:;basic:;Please Attach Data Sheet";
        if (!uploadPerformanceDataFormat(serviceLevelId1, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
            customAssert.assertTrue(false, "Wrong data file upload validation unsuccessful");
        }

        customAssert.assertAll();
    }

    //    C10775
//    C13526
    @Test(dependsOnMethods = "TestCSLCreation", enabled = true)
    public void TestCSLCompStatus_DuplicateRecords() {

        CustomAssert customAssert = new CustomAssert();
        Show show = new Show();

        int cSLId = -1;
        try {
            cSLId = Integer.parseInt(childserviceLevelId1s1.get(5));

            String rawDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilenduplicaterecords");

            String currentTimeStamp = getCurrentTimeStamp().get(0).get(0);

            if (!uploadRawDataCSL(cSLId, rawDataFileName, rawDataFileValidMsg, customAssert)) {

                customAssert.assertTrue(false, "Raw Data Uploaded unsuccessfully on CSL " + cSLId);
                return;
            } else {

                if (!validateStructuredPerformanceDataCSL(cSLId, rawDataFileName, "Done", "View Structured Data", completedBy, customAssert)) {
                    customAssert.assertTrue(false, "Raw Data Upload validation unsuccessful for CSL ID " + cSLId);
                }
            }

            String computationStatus = "Duplicate Data Recorded";

            if(!validateComputationStatus(cSLId,show,computationStatus,customAssert)){
                customAssert.assertTrue(false, "Computation Status validated unsuccessfully" +computationStatus+cSLId);
            }

            show.hitShowVersion2(cslEntityTypeId, cSLId);
            String showPageResponse = show.getShowJsonStr();

            String shortCodeId = ShowHelper.getValueOfField("short code id", showPageResponse);
            String description = ShowHelper.getValueOfField("description", showPageResponse);

//          C13572
                String subjectLine = "Duplicates found - (#" + shortCodeId + ")-(#" + description + ")";
                List<String> expectedSentencesInBody = new ArrayList<>();
//                expectedSentencesInBody.add("7 duplicate records have been identified in performance data.");
                if (!validateEmailSentSuccessfully(shortCodeId, subjectLine, expectedSentencesInBody, currentTimeStamp, customAssert)) {
                    customAssert.assertTrue(false, "Email validated unsuccessfully");
                }
            else {
                customAssert.assertTrue(true, "Email validated successfully");
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating computation status for CSL ID " + cSLId + e.getMessage());
        }

        customAssert.assertAll();
    }

    //C10440 C13527 C13528 C10565 C10775
    @Test(groups = {"PCQ DCQ UDC Update1"},dependsOnMethods = "TestCSLCreation", enabled = true)     //Completed
    public void TestCSLCompStatus_DataMarkedComputation_And_ComputationCompletedSuccessfully() {

        CustomAssert customAssert = new CustomAssert();

        logger.info("Validating Child Service Level Computation Status");

        try {

            int cSLId = Integer.parseInt(childserviceLevelId1s.get(0));

            String rawDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilename");

            if (!uploadRawDataCSL(cSLId, rawDataFileName, rawDataFileValidMsg, customAssert)) {

                customAssert.assertTrue(false, "Raw Data Uploaded unsuccessfully on CSL " + cSLId);
            } else {

                if (!validateStructuredPerformanceDataCSL(cSLId, rawDataFileName, "Done", "View Structured Data", completedBy, customAssert)) {
                    customAssert.assertTrue(false, "Raw Data Upload validation unsuccessful for CSL ID " + cSLId);
                }

                //C10594
                if (!validateDownloadFileFromPerformanceTabCSL(cSLId, rawDataFileName + RandomNumbers.getRandomNumberWithinRange(1, 10), rawDataFileName, customAssert)) {
                    customAssert.assertTrue(false, "Download File from Structured Performance Data Tab Of CSL Functionality validated unsuccessfully on CSL : " + cSLId);
                }
            }

            Show show = new Show();
            show.hitShowVersion2(cslEntityTypeId, cSLId);
            String showPageResponse = show.getShowJsonStr();

            Boolean workFlowStatus = performComputationCSL(cSLId, customAssert);

            if (!workFlowStatus) {
                customAssert.assertTrue(false, "Unable to perform  computation on CSL Id " + cSLId);
            }

            show.hitShowVersion2(cslEntityTypeId, cSLId);
            showPageResponse = show.getShowJsonStr();

            String computationStatus = ShowHelper.getValueOfField("computationstatus", showPageResponse);

            if (!(computationStatus.equalsIgnoreCase("Data Marked for Aggregation") || computationStatus.equalsIgnoreCase("Data Marked for Computation"))) {

                customAssert.assertTrue(false, "Computation Status Expected \"Data Marked for Computation\" Actual Computation Status " + computationStatus);

            } else {
                customAssert.assertTrue(true, "Computation Status validated successfully");
            }

            computationStatus = "Computation Completed Successfully";

            if(!validateComputationStatus(cSLId,show,computationStatus,customAssert)){
                customAssert.assertTrue(false, "Computation Status validated unsuccessfully" +computationStatus+cSLId);
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating status Computation Completed Successfully");
        }

        customAssert.assertAll();
    }

    public void executeParallelServiceToGetActiveServiceLevelId() throws Exception {

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        Future<Integer> future1 = executorService.submit(this::getActiveServiceLevelId);

        Future<Integer> future2 = executorService.submit(this::getActiveServiceLevelId);

        // wait until result will be ready
        serviceLevelId1 = future1.get();

        // wait only certain timeout otherwise throw an exception
        serviceLevelId12 = future2.get(1, TimeUnit.SECONDS);

        executorService.shutdown();
    }

    private int getActiveServiceLevelId(){

//		int serviceLevelId = serviceLevelId1 + 1;
        int serviceLevelId = -1;
        String flowToTest = "sl automation flow";
//
//        String PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\",\"init_script\":\"state['map'] = ['met':0, 'notMet':0]\",\"reduce_script\":\"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;} return params.result\",\"combine_script\":\"return state;\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}}]}}}";
        String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if(doc['exception'].value== false){state.map.met++}else{state.map.notMet++}\", \"init_script\": \"state['map'] =['met': 0, 'notMet': 0]\", \"reduce_script\": \"params.return_map = ['Final_Numerator':1, 'Final_Denominator':2,'Actual_Numerator':10, 'Actual_Denominator':100]; for (a in states){params.return_map.Final_Numerator +=(float)(a.map.met); params.return_map.Final_Denominator += (float)(a.map.met + a.map.notMet);} if(params.return_map.Final_Denominator > 447){params.return_map.Final_Numerator = 95; params.return_map.Final_Denominator = 100;}return params.return_map\", \"combine_script\": \"return state;\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}, {\"match\": {\"useInComputation\": true}}]}}}";
        String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";

        String user = auditLogUser;
//
        CustomAssert customAssert = new CustomAssert();
        try {

            serviceLevelId = getServiceLevelId1(flowToTest, PCQ, DCQ, customAssert);

            if (serviceLevelId != -1) {
                List<String> workFlowSteps;
                workFlowSteps = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "slactiveworkflowsteps").split("->"));
                if (!performWorkFlowActions(slEntityTypeId, serviceLevelId, workFlowSteps, user,false, customAssert)) {
                    customAssert.assertTrue(false, "Error while performing workflow actions on SL ID " + serviceLevelId);
                    return -1;
                }
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while getting an active an active service level id " + e.getMessage());
        }
        return serviceLevelId;

    }

    private int getServiceLevelId1(String flowToTest, String PCQ, String DCQ, CustomAssert customAssert) {

        int serviceLevelId1 = -1;

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
                serviceLevelId1 = CreateEntity.getNewEntityId(createResponse, "service levels");

        } else {
            throw new SkipException("Couldn't get JSON Response for Create Flow [" + flowToTest + "] Thus Skipping the test");
        }
        return serviceLevelId1;
    }

    private Boolean performWorkFlowActions(int entityTypeId, int entityId, List<String> workFlowSteps, String user,Boolean checkAuditLogTab, CustomAssert customAssert) {

        Boolean workFlowStepActionStatus;
        String actionNameAuditLog;
        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();

        try {
            for (String workFlowStepToBePerformed : workFlowSteps) {

                //workFlowStepActionStatus = workflowActionsHelper.performWorkflowAction(entityTypeId, entityId, workFlowStepToBePerformed);
                workFlowStepActionStatus = workflowActionsHelper.performWorkFlowStepV2(entityTypeId,entityId,workFlowStepToBePerformed,customAssert);

                if (!workFlowStepActionStatus) {

                    customAssert.assertTrue(false, "Unable to perform workflow action " + workFlowStepToBePerformed + " on service level id " + entityId);
                    return false;
                } else {
                    if(checkAuditLogTab) {
                        actionNameAuditLog = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "auditlogactioname", workFlowStepToBePerformed);
                        if (!verifyAuditLog(entityTypeId, entityId, actionNameAuditLog, user, customAssert)) {
                            customAssert.assertTrue(false, "Audit Log tab verified unsuccessfully for entity id " + entityId);
                        }
                    }
                }
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while performing Workflow actions for service level id " + entityId + e.getMessage());
            return false;

        }
        return true;
    }

    private ArrayList<String> checkIfCSLCreatedOnServiceLevel(int serviceLevelId1, CustomAssert customAssert) {

        logger.info("Checking if CSL created on service level");

        long timeSpent = 0;
        long cSLCreationTimeOut = 5000000L;
        long pollingTime = 5000L;
        ArrayList<String> childserviceLevelId1s = new ArrayList<>();
        try {
            JSONObject tabListResponseJson;
            JSONArray dataArray = new JSONArray();

            TabListData tabListData = new TabListData();
            tabListData.hitTabListData(childServiceLevelTabId, slEntityTypeId, serviceLevelId1,50);
            String tabListResponse = tabListData.getTabListDataResponseStr();

            if (JSONUtility.validjson(tabListResponse)) {

                while (timeSpent < cSLCreationTimeOut) {
                    logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                    Thread.sleep(pollingTime);

                    tabListData.hitTabListData(childServiceLevelTabId, slEntityTypeId, serviceLevelId1);
                    tabListResponse = tabListData.getTabListDataResponseStr();

                    if (!JSONUtility.validjson(tabListResponse)) {

                        customAssert.assertTrue(false, "Service level tab Child Service Level has invalid Json Response for service level id " + serviceLevelId1);
                        break;
                    }

                    tabListResponseJson = new JSONObject(tabListResponse);
                    dataArray = tabListResponseJson.getJSONArray("data");

                    if (dataArray.length() > 0) {

                        customAssert.assertTrue(true, "Child Service Level created successfully ");

                        childserviceLevelId1s = (ArrayList) ListDataHelper.getColumnIds(tabListResponse);
                        break;
                    } else {
                        timeSpent += pollingTime;
                        logger.info("Child Service Level not created yet ");
                    }
                }
                if (childserviceLevelId1s.size() == 0) {
//					customAssert.assertTrue(false, "Child Service level not created in " + cSLCreationTimeOut + " milli seconds for service level id " + serviceLevelId1);
                }

            } else {
                customAssert.assertTrue(false, "Service level tab Child Service Level has invalid Json Response for service level id " + serviceLevelId1);
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while checking child service level tab on ServiceLevel " + serviceLevelId1 + " " + e.getMessage());
        }

        return childserviceLevelId1s;
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

    private List<List<String>> getCurrentTimeStamp() {

        String sqlString = "select current_timestamp";
        List<List<String>> currentTimeStamp = null;
        PostgreSQLJDBC postgreSQLJDBC;

        postgreSQLJDBC = new PostgreSQLJDBC(dbHostName,dbPortName,dbName,dbUserName,dbPassowrd);
        try {


            currentTimeStamp = postgreSQLJDBC.doSelect(sqlString);
        } catch (Exception e) {
            logger.error("Exception while getting current time stamp " + e.getMessage());
        }finally {
            postgreSQLJDBC.closeConnection();
        }
        return currentTimeStamp;
    }

    private Boolean uploadRawDataCSL(int cslId, String rawDataFileName, String expectedMsg, CustomAssert customAssert) {

        logger.info("Uploading Raw Data on child service level");

        Boolean uploadRawDataStatus = true;
        try {

            Map<String, String> payloadMap = new HashMap<>();
            payloadMap.put("parentEntityId", String.valueOf(cslId));
            payloadMap.put("parentEntityTypeId", String.valueOf(cslEntityTypeId));
            payloadMap.put("_csrf_token", "9e2897d6-7c08-493d-bfb4-31ab887e5ce8");

            XLSUtils.updateColumnValue(uploadFilePath,rawDataFileName,"Sheet1",100,200,"");


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

    private Boolean validateStructuredPerformanceDataCSL(int CSLId, String expectedFileName, String computationStatus, String expectedPerformanceData, String expectedCompletedBy, CustomAssert customAssert) {

        logger.info("Validating Structured Performance Data tab on CSL " + CSLId);
        Boolean validationStatus = true;
        long timeSpent = 0;
        long fileUploadTimeOut = 100000L;
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
                customAssert.assertTrue(false, "Raw Data File not Uploaded has invalid Json Response for service level id " + serviceLevelId1);
                validationStatus = false;
                return validationStatus;
            }
            Thread.sleep(15000);
            JSONArray individualRowDataJsonArray = null;
            JSONObject individualColumnJson;
            String columnName;
            String columnValue;

            while (timeSpent < fileUploadTimeOut) {

                logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                Thread.sleep(pollingTime);

                tabListData.hitTabListData(structuredPerformanceDataTabId, cslEntityTypeId, CSLId);
                tabListResponse = tabListData.getTabListDataResponseStr();
                tabListResponseJson = new JSONObject(tabListResponse);
                dataArray = tabListResponseJson.getJSONArray("data");
                JSONObject individualRowData = dataArray.getJSONObject(dataArray.length() - 1);
                individualRowDataJsonArray = JSONUtility.convertJsonOnjectToJsonArray(individualRowData);
                columnValue = individualRowDataJsonArray.getJSONObject(0).getString("value").split(":;")[0];
                if(columnValue.equalsIgnoreCase(computationStatus)){
                    break;
                }else {
                    timeSpent += pollingTime;
                }
            }

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

    private List<List<String>> getRecordFromSystemEmailTable(String subjectLine, String currentTimeStamp) {

        String sqlQuery = "select subject,sent_successfully,body from system_emails where subject ilike '%" + subjectLine + "%' AND date_created > " + "'" + currentTimeStamp + "'"
                + "order by id desc";
        List<List<String>> queryResult = null;
        PostgreSQLJDBC postgreSQLJDBC;
        String dbName = "letterbox-sl";

        postgreSQLJDBC = new PostgreSQLJDBC(dbHostName,dbPortName,dbName,dbUserName,dbPassowrd);

        try {
            queryResult = postgreSQLJDBC.doSelect(sqlQuery);

        } catch (Exception e) {
            logger.error("Exception while getting record from sql " + e.getMessage());
        }finally {
            postgreSQLJDBC.closeConnection();
        }
        return queryResult;

    }

    private Boolean validateAttachmentName(List<List<String>> recordFromSystemEmailTable, String expectedAttachment, CustomAssert customAssert) {

        Boolean validationStatus = true;

        try {
            String actualAttachment = recordFromSystemEmailTable.get(0).get(0);

            if (expectedAttachment == null && actualAttachment == null) {
                return true;
            } else if (expectedAttachment == null && actualAttachment != null) {
                return false;
            }

            if (!actualAttachment.equalsIgnoreCase(expectedAttachment)) {

                customAssert.assertTrue(false, "Attachment name validated unsuccessfully from system emails table");
                validationStatus = false;
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating Attachment Name from system email table with the expected attachment name");
            validationStatus = false;
        }

        return validationStatus;
    }

    //    C10615 C10616 C10634 C10643
    private Boolean validateIgnoreFileScenario(int cSLId, CustomAssert customAssert) {

        logger.info("Validating Ignore Files Scenario");

        Boolean validationStatus = true;
        long waitForIgnoreTimeOut = 300000L;
        long pollingTime = 5000L;
        long timeSpent = 0;
        try {
            String structurePerformanceData = getStructuredPerformanceData(cSLId);

            if (JSONUtility.validjson(structurePerformanceData)) {

                JSONObject structuredPerformanceDataFileDetailsJson = new JSONObject(structurePerformanceData).getJSONArray("data").getJSONObject(0);
                JSONArray dataArray = JSONUtility.convertJsonOnjectToJsonArray(structuredPerformanceDataFileDetailsJson);
                String columnName;
                String documentId = "";
                for (int i = 0; i < dataArray.length(); i++) {

                    columnName = dataArray.getJSONObject(i).get("columnName").toString();

                    if (columnName.equalsIgnoreCase("filename")) {
                        documentId = dataArray.getJSONObject(i).get("value").toString().split(":;")[1];
                        break;
                    }
                }

                SLDetails slDetails = new SLDetails();
                String payload = "{\"documentIds\":[" + documentId + "],\"childSlaId\":" + cSLId + "}";
                slDetails.hitIgnorePerformanceDataFiles(payload);

                //Checking Ignore in Progress Case
                structurePerformanceData = getStructuredPerformanceData(cSLId);
                structuredPerformanceDataFileDetailsJson = new JSONObject(structurePerformanceData).getJSONArray("data").getJSONObject(0);
                dataArray = JSONUtility.convertJsonOnjectToJsonArray(structuredPerformanceDataFileDetailsJson);
                String statusValue = "";
                for (int i = 0; i < dataArray.length(); i++) {

                    columnName = dataArray.getJSONObject(i).get("columnName").toString();

                    if (columnName.equalsIgnoreCase("status")) {
                        statusValue = dataArray.getJSONObject(i).get("value").toString();
                        if (!(statusValue.equalsIgnoreCase("Ignore in Progress:;false") || statusValue.contains("Ignored"))) {
                            customAssert.assertTrue(false, "Status For File Ignore Scenario is not as Expected for CSL ID " + cSLId);
                            customAssert.assertTrue(false, "Expected : " + "Ignore in Progress:;false" + " Actual : " + statusValue);
                            validationStatus = false;
                        }
                        break;
                    }
                }

                Thread.sleep(50000);

                //Checking Ignored Case
                while(timeSpent < waitForIgnoreTimeOut){

                    logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                    Thread.sleep(pollingTime);

                    structurePerformanceData = getStructuredPerformanceData(cSLId);
                    structuredPerformanceDataFileDetailsJson = new JSONObject(structurePerformanceData).getJSONArray("data").getJSONObject(0);
                    dataArray = JSONUtility.convertJsonOnjectToJsonArray(structuredPerformanceDataFileDetailsJson);
                    columnName = dataArray.getJSONObject(0).getString("columnName");
                    if (columnName.equalsIgnoreCase("status")){
                        statusValue = dataArray.getJSONObject(0).getString("value");
                        if (statusValue.equalsIgnoreCase("Ignored:;false")) {
                            break;
                        }
                        else{
                            timeSpent += pollingTime;
                        }
                    }
                }

                //Verifying Audit Log After Ignoring Files
				if (!(verifyAuditLog(cslEntityTypeId, cSLId, "Performance Data Ignored", completedBy, customAssert))) {
					customAssert.assertTrue(false, "Audit Log validated unsuccessfully after Ignore Files Functionality");
				}

                //Verifying Raw Data Tab when all files are ignored
                //************************************************************************************************************************************************
                payload = "{\"offset\":0,\"size\":1,\"childSlaId\":" + cSLId + "}";
                slDetails.hitSLDetailsGlobalList(payload);
                String sLDetailsResponse = slDetails.getSLDetailsResponseStr();

                dataArray = new JSONObject(sLDetailsResponse).getJSONArray("data");
                if (dataArray.length() != 0) {
                    customAssert.assertTrue(false, "Raw Data Tab contains data while all files should be ignored for CSL ID " + cSLId);
                }
                //************************************************************************************************************************************************


            } else {
                customAssert.assertTrue(false, "Structured Performance Data Response is not a valid Json for CSL Id " + cSLId);
                validationStatus = false;
            }

        } catch (Exception e) {
            logger.error("Exception while validating Ignore Files Scenario");
            customAssert.assertTrue(false, "Exception while validating Ignore Files Scenario");
            validationStatus = false;
        }
        return validationStatus;

    }

    private Boolean validateBodyOfEmail(List<List<String>> recordFromSystemEmailTable, List<String> expectedStringInBody, CustomAssert customAssert) {

        Boolean validationStatus = true;

        String actualBodyHtml;
        try {

            for (int i = 0; i < recordFromSystemEmailTable.size(); i++) {
                actualBodyHtml = recordFromSystemEmailTable.get(i).get(2);

                for (int j = 0; j < expectedStringInBody.size(); j++) {

                    if (!actualBodyHtml.contains(expectedStringInBody.get(j))) {
                        customAssert.assertTrue(false, "While validating email Body Html does not contain the expected String " + expectedStringInBody.get(j));
                        validationStatus = false;
                    }
                }


            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating body of Email");
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

        if (!fileUploadResponse.contains(expectedMsg)) {

            customAssert.assertTrue(false, "Error while performance data format upload or SL Template Upload message has been changed");
            return false;
        }
        return true;
    }

    //    C10565 C10638
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
            Thread.sleep(10000);
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

//			if (dataArray.length() != 1) {
//				customAssert.assertTrue(false, "Performance Data Format Tab data Count not equal to 1");
//				return false;
//			}

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

                if (columnName.equalsIgnoreCase("createddate")) {

                    columnValue = indRowData.getJSONObject(i).get("value").toString();

//					if (!(columnValue.equalsIgnoreCase(currentDateIn_MMM_DD_YYYY_Format) || columnValue.equalsIgnoreCase(previousDateIn_MMM_DD_YYYY_Format)
//							|| columnValue.contains(nextDateIn_MMM_DD_YYYY_Format))) {
//
//						customAssert.assertTrue(false, "Performance Data Format Tab Date Expected and Actual values mismatch");
//						customAssert.assertTrue(false, "Expected Date : " + currentDateIn_MMM_DD_YYYY_Format + " OR " + previousDateIn_MMM_DD_YYYY_Format
//								+ " OR " + nextDateIn_MMM_DD_YYYY_Format + " Actual Date : " + columnValue);
//						validationStatus = false;
//					}
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

    private Boolean performComputationCSL(int cSLId,CustomAssert customAssert) {

        WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();

        Boolean workFlowStatus = true;
        try {
            Thread.sleep(2000);
        }catch (Exception e){

        }
        for (String workFlowActionToPerform : workFlowActionToPerformCSLComputePerformance) {
            workFlowStatus = workflowActionsHelper.performWorkFlowStepV2(cslEntityTypeId, cSLId, workFlowActionToPerform,customAssert);


            if (!workFlowStatus) {
                customAssert.assertTrue(false, "Unable to perform " + workFlowActionToPerform + " on CSL Id " + cSLId);
                workFlowStatus = false;
            }
        }

        return workFlowStatus;
    }

    private Boolean uploadRawDataCSL(int cslId,String uploadFilePath, String rawDataFileName, String expectedMsg, CustomAssert customAssert) {

        logger.info("Uploading Raw Data on child service level");

        Boolean uploadRawDataStatus = true;
        try {

            XLSUtils.updateColumnValue(uploadFilePath,rawDataFileName,"Sheet1",100,200,"");

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
                customAssert.assertTrue(false, "Raw data uploaded unsuccessfully on Child Service Level " + cslId);
                uploadRawDataStatus = false;
                return uploadRawDataStatus;
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while uploading raw data on Child Service Level Id " + cslId + " " + e.getMessage());
            uploadRawDataStatus = false;
        }
        return uploadRawDataStatus;
    }

    private Boolean verifyAuditLog(int entityTypeId, int entityId, String actionNameExpected, String user, CustomAssert customAssert) {

        logger.info("Validating Audit Log Tab for entity type id " + entityTypeId + " and entity id " + entityId);

        int AuditLogTabId = 61;

        Boolean validationStatus = true;
//		int expectedValidationChecksOnAuditLogTab = 5;
        int expectedValidationChecksOnAuditLogTab = 3;
        int actualValidationChecksOnAuditLogTab = 0;

        TabListData tabListData = new TabListData();

        JSONObject latestActionRow;
        JSONObject tabListDataResponseJson;
        JSONArray dataArray;
        JSONArray latestActionRowJsonArray;

        String tabListDataResponse;
        String columnName;
        String columnValue;

        try {

            tabListData.hitTabListData(AuditLogTabId, entityTypeId, entityId);
            tabListDataResponse = tabListData.getTabListDataResponseStr();

            if (APIUtils.validJsonResponse(tabListDataResponse)) {

                tabListDataResponseJson = new JSONObject(tabListDataResponse);
                dataArray = tabListDataResponseJson.getJSONArray("data");

                if(dataArray.length() == 0){
                    customAssert.assertTrue(false,"No entry exists in Audit Log Tab for entity id " + entityId + " entity type id " + entityTypeId);
                    return false;
                }

//				latestActionRow = dataArray.getJSONObject(0);
                latestActionRow = dataArray.getJSONObject(dataArray.length() - 1);
                latestActionRowJsonArray = JSONUtility.convertJsonOnjectToJsonArray(latestActionRow);

                for (int i = 0; i < latestActionRowJsonArray.length(); i++) {

                    columnName = latestActionRowJsonArray.getJSONObject(i).get("columnName").toString();
                    columnValue = latestActionRowJsonArray.getJSONObject(i).get("value").toString();

                    switch (columnName) {

                        case "action_name":

                            if (!columnValue.equalsIgnoreCase(actionNameExpected)) {
                                customAssert.assertTrue(false, "Under Audit Log Tab action_name is validated unsuccessfully for entity id " + entityId);
                                customAssert.assertTrue(false, "Expected action_name : " + actionNameExpected + " Actual action_name : " + columnValue);
                                validationStatus = false;
                            } else {
                                actualValidationChecksOnAuditLogTab = actualValidationChecksOnAuditLogTab + 1;
                            }
                            break;

                        case "requested_by":
                            if (!columnValue.equalsIgnoreCase(user)) {
                                customAssert.assertTrue(false, "Under Audit Log Tab requested_by is validated unsuccessfully for entity id " + entityId);
                                customAssert.assertTrue(false, "Expected requested_by : " + actionNameExpected + " Actual requested_by : " + columnValue);
                                validationStatus = false;

                            } else {
                                actualValidationChecksOnAuditLogTab = actualValidationChecksOnAuditLogTab + 1;
                            }
                            break;

                        case "completed_by":
                            if (!columnValue.equalsIgnoreCase(user)) {
                                customAssert.assertTrue(false, "Under Audit Log Tab completed_by is validated unsuccessfully for entity id " + entityId);
                                customAssert.assertTrue(false, "Expected completed_by : " + actionNameExpected + " Actual completed_by : " + columnValue);
                                validationStatus = false;

                            } else {
                                actualValidationChecksOnAuditLogTab = actualValidationChecksOnAuditLogTab + 1;
                            }
                            break;

//						case "audit_log_date_created":
//
//							if (!(columnValue.contains(currentDateIn_MM_DD_YYYY_Format) || columnValue.contains(previousDateIn_MM_DD_YYYY_Format) || columnValue.contains(nextDateIn_MM_DD_YYYY_Format))) {
//
//								customAssert.assertTrue(false, "Under Audit Log Tab audit_log_date_created is validated unsuccessfully for entity id " + entityId);
//								customAssert.assertTrue(false, "Expected audit_log_date_created : " + currentDateIn_MM_DD_YYYY_Format + " OR " + previousDateIn_MM_DD_YYYY_Format
//										+ " OR " + nextDateIn_MM_DD_YYYY_Format + " Actual audit_log_date_created : " + columnValue);
//								validationStatus = false;
//
//							} else {
//								actualValidationChecksOnAuditLogTab = actualValidationChecksOnAuditLogTab + 1;
//							}
//							break;
//
//						case "audit_log_user_date":
//
//							if (!(columnValue.contains(currentDateIn_MM_DD_YYYY_Format) || columnValue.contains(previousDateIn_MM_DD_YYYY_Format) || columnValue.contains(nextDateIn_MM_DD_YYYY_Format))) {
//
//								customAssert.assertTrue(false, "Under Audit Log Tab audit_log_user_date is validated unsuccessfully for entity id " + entityId);
//								customAssert.assertTrue(false, "Expected audit_log_user_date : " + currentDateIn_MM_DD_YYYY_Format + " OR " + previousDateIn_MM_DD_YYYY_Format
//										+ " OR " + nextDateIn_MM_DD_YYYY_Format + " Actual audit_log_user_date : " + columnValue);
//								validationStatus = false;
//							} else {
//								actualValidationChecksOnAuditLogTab = actualValidationChecksOnAuditLogTab + 1;
//							}
//							break;
                    }

                }

                if (actualValidationChecksOnAuditLogTab == expectedValidationChecksOnAuditLogTab) {
                    customAssert.assertTrue(true, "All validation checks passed successfully");
                } else {
                    customAssert.assertTrue(false, "Validation check count not equal to " + expectedValidationChecksOnAuditLogTab);
                    validationStatus = false;
                }
            } else {
                customAssert.assertTrue(false, "Audit Log Tab Response is not a valid json for entity id " + entityId);
                validationStatus = false;
            }


        } catch (Exception e) {
            logger.error("Exception while validating tab list response " + e.getMessage());
            customAssert.assertTrue(false, "Exception while validating tab list response " + e.getMessage());
            validationStatus = false;
        }

        return validationStatus;
    }

    private String getStructuredPerformanceData(int cSLId) {

        logger.info("Getting Structured Performance data for CSL ID " + cSLId);
        int structuredPerformanceDataTabId = 207;

        TabListData tabListData = new TabListData();
        String payload = "{\"filterMap\":{\"entityTypeId\":" + cSLId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
        tabListData.hitTabListData(structuredPerformanceDataTabId, cslEntityTypeId, cSLId, payload);
        String tabListResponse = tabListData.getTabListDataResponseStr();

        return tabListResponse;

    }

    @AfterClass(groups = {"sanity","sprint"})
    public void afterClass() {

        logger.debug("Number CSL To Delete " + cslToDelete.size());
        EntityOperationsHelper.deleteMultipleRecords("child service levels", cslToDelete);

        logger.debug("Number SL To Delete " + slToDelete.size());
        EntityOperationsHelper.deleteMultipleRecords("service levels", slToDelete);

    }

    private Boolean validateEmailSentSuccessfully(String shortCodeId, String subjectLine, List<String> expectedStringInBody,String timeStamp, CustomAssert customAssert) {

        Boolean validationStatus = true;

        try {
            //		String timeStamp = getCurrentTimeStamp().get(0).get(0);

            List<List<String>> recordFromSystemEmailTable = getRecordFromSystemEmailTable(subjectLine, timeStamp);

            if (recordFromSystemEmailTable.size() == 0) {
                customAssert.assertTrue(false, "No entry in system email table for subject Line " + subjectLine);
                return false;
            }

            if (!validateBodyOfEmail(recordFromSystemEmailTable, expectedStringInBody, customAssert)) {
                customAssert.assertTrue(false, "Body validated unsuccessfully in system emails table for subjectLine " + subjectLine);
                validationStatus = false;
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating Email Scenario " + e.getMessage());
            validationStatus = false;
        }
        return validationStatus;
    }

    //C10594
    private boolean validateDownloadFileFromPerformanceTabCSL(int entityId ,String downloadFileName,String uploadedPerformanceDataFileName,CustomAssert customAssert) {

        Boolean validationStatus = true;
        int performanceTabId = 207;
        try{
            //*************Getting Document Id**********************************************//
            logger.info("Getting Document ID");
            String payload = "{\"filterMap\":{\"entityTypeId\":"+ cslEntityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
            TabListData tabListData = new TabListData();
            String tabListResponse = tabListData.hitTabListData(performanceTabId,cslEntityTypeId,entityId,payload);

            JSONObject tabListResponseJson = new JSONObject(tabListResponse);
            JSONObject indRow = tabListResponseJson.getJSONArray("data").getJSONObject(0);

            JSONArray indRowArray = JSONUtility.convertJsonOnjectToJsonArray(indRow);
            JSONObject indColumnDetails;
            String columnName;
            String documentId = "";
            for(int i =0;i<indRowArray.length();i++){

                indColumnDetails = indRowArray.getJSONObject(i);
                columnName = indColumnDetails.get("columnName").toString();
                if(columnName.equalsIgnoreCase("filename")){
                    documentId = indColumnDetails.get("value").toString().split(":;")[1];
                    break;
                }
            }
            //*************Getting Document Id End**********************************************//

            DownloadListWithData download = new DownloadListWithData();
            Boolean downloadStatus = download.hitDownloadCSLPerformanceTab(documentId,downloadFilePath,downloadFileName);

            if(downloadStatus == false){
                customAssert.assertTrue(false,"Download unsuccessful for performance data format tab");
            }
            if(!FileUtils.fileExists(downloadFilePath,downloadFileName)){
                customAssert.assertTrue(false,"After Download from performance data tab the file does not exist ");
                validationStatus = false;
            }else {
                List<List<String>> downloadedExcelData = XLSUtils.getAllExcelData(downloadFilePath,downloadFileName,"Service Level Raw Data Metadata");
                List<List<String>> uploadedExcelData  = XLSUtils.getAllExcelData(downloadFilePath,uploadedPerformanceDataFileName,"Service Level Raw Data Metadata");

                if(!downloadedExcelData.equals(uploadedExcelData)){
                    customAssert.assertTrue(false,"Downloaded and uploaded excel performance data mismatched");
                }
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating DownloadFile From Performance Tab for CSL ID " + e.getMessage());
            validationStatus = false;
        }finally {
            FileUtils.deleteFile(downloadFilePath + "/" + downloadFileName);
        }

        return validationStatus;
    }

    private boolean validateComputationStatus(int cSLId, Show show, String newComputationStatus, CustomAssert customAssert) throws InterruptedException {

        logger.info("Validating New Computation Status on CSL " + cSLId);
        String computationStatus = null;
        Boolean validationStatus = false;
        long timeSpent = 0;
        long fileUploadTimeOut = 160000L;
        long pollingTime = 5000L;

        while(timeSpent < fileUploadTimeOut) {

            logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
            Thread.sleep(pollingTime);

            show.hitShowVersion2(cslEntityTypeId, cSLId);
            String showPageResponse = show.getShowJsonStr();
            computationStatus = ShowHelper.getValueOfField("computationStatus", showPageResponse);
            if (computationStatus.equalsIgnoreCase(newComputationStatus)){
                break;
            }
            else{
                timeSpent += pollingTime;
            }
        }
        if (!computationStatus.equalsIgnoreCase(newComputationStatus)) {

            customAssert.assertTrue(false, "Computation Status Expected \"Computation Completed Successfully\" Actual Computation Status " + computationStatus);

        } else {
            customAssert.assertTrue(true, "Computation Status validated successfully");
            validationStatus = true;
        }
        return validationStatus;
    }
}