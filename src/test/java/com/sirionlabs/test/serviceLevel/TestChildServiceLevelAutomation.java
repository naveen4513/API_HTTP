package com.sirionlabs.test.serviceLevel;

import com.sirionlabs.api.bulkedit.BulkeditEdit;
import com.sirionlabs.api.bulkupload.Download;
import com.sirionlabs.api.bulkupload.UploadBulkData;
import com.sirionlabs.api.bulkupload.UploadRawData;

import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;

import com.sirionlabs.api.listRenderer.DownloadListWithData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.SLDetails;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.servicedata.TblauditlogsFieldHistory;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.WorkflowActionsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.servicelevel.ServiceLevelHelper;
import com.sirionlabs.helper.servicelevel.TimeDifferenceFunction;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.HttpResponse;

import org.apache.kafka.common.protocol.types.Field;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.*;

import java.io.File;
import java.sql.SQLOutput;
import java.util.*;
import java.util.concurrent.*;

public class TestChildServiceLevelAutomation {

	private final static Logger logger = LoggerFactory.getLogger(TestChildServiceLevelAutomation.class);

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

	private String rawDataFileName;
	private String auditLogUser;
	private String adminUser;
	private String rawDataFileValidMsg;
	private Integer slEntityTypeId;
	private Integer cslEntityTypeId;
	private Integer contractEntityTypeId;
	private Integer supplierEntityTypeId;

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
		contractEntityTypeId = ConfigureConstantFields.getEntityIdByName("contracts");
		supplierEntityTypeId = ConfigureConstantFields.getEntityIdByName("suppliers");

		uploadFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "uploadfilepath");
		downloadFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "downloadfilepath");
		bulkUpdateFilename = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulkupdatefilename");
		cslBulkUpdateTemplateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "cslbulkupdatetemplateid"));
		slMetaDataUploadTemplateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "slmetadatauploadtemplateid"));
		uploadIdSL_PerformanceDataTab = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "uploadidslperformancecdatatab"));
		rawDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilename");
		auditLogUser = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "createdbyuser");
		adminUser = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "adminuser");
		rawDataFileValidMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilesuccessmsg");
		slTemplateFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilename");

		currentDateIn_MM_DD_YYYY_Format = DateUtils.getCurrentDateInMM_DD_YYYY();
		previousDateIn_MM_DD_YYYY_Format = DateUtils.getPreviousDateInMM_DD_YYYY(currentDateIn_MM_DD_YYYY_Format);
//		nextDateIn_MM_DD_YYYY_Format = DateUtils.getNextDateInMM_DD_YYYY(currentDateIn_MM_DD_YYYY_Format);

		currentDateIn_MMM_DD_YYYY_Format = DateUtils.getCurrentDateInMMM_DD_YYYY();
		previousDateIn_MMM_DD_YYYY_Format = DateUtils.getPreviousDateInMMM_DD_YYYY(currentDateIn_MMM_DD_YYYY_Format);
//		nextDateIn_MMM_DD_YYYY_Format = DateUtils.getNextDateInMMM_DD_YYYY(currentDateIn_MMM_DD_YYYY_Format);

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
	@DataProvider(name = "slMetStatusValidationFlows", parallel = false)
	public Object[][] slMetStatusValidationFlows() {

		List<Object[]> allTestData = new ArrayList<>();
		String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "slmetflowstovalidate").split(",");

		for (String flowToTest : flowsToTest) {
			allTestData.add(new Object[]{flowToTest});
		}
		return allTestData.toArray(new Object[0][]);
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

	@DataProvider(name = "slMetGraphValidationFlows", parallel = true)
	public Object[][] slMetGraphValidationFlows() {

		List<Object[]> allTestData = new ArrayList<>();
		String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "slmetgraphflowstovalidate").split(",");

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


	@DataProvider(name = "flowsForIrrelevantTargets", parallel = true)
	public Object[][] flowsForIrrelevantTargets() {

		List<Object[]> allTestData = new ArrayList<>();

		String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "slflowsforirrelevanttargets", "flowstotest").split(",");
		String expectedMsg = "";
		for (String flowToTest : flowsToTest) {

			if (flowToTest.equalsIgnoreCase("sl max level 1") || flowToTest.equalsIgnoreCase("sl min level 1")) {
				expectedMsg = "Target Field Maximum Is Not Relevant For The Entity.";

			} else if (flowToTest.equalsIgnoreCase("sl max level 2") || flowToTest.equalsIgnoreCase("sl min level 2")) {

				expectedMsg = "Target Field Significantly Maximum Is Not Relevant For The Entity.";

			} else if (flowToTest.equalsIgnoreCase("sl rag applicable no")) {

				expectedMsg = "Error found while setting metadata value using elastic search query.";
			}
			allTestData.add(new Object[]{flowToTest, expectedMsg});
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

	@Test(groups = {"sanity","PCQ DCQ UDC Update1","PCQ DCQ UDC Update2"},enabled = false)
	public void TestCSLCreation() {

		CustomAssert customAssert = new CustomAssert();

		try {

//			executeParallelServiceToGetActiveServiceLevelId();

			serviceLevelId1 = 20800;
			serviceLevelId12 = 20801;
			slToDelete.add(serviceLevelId1);
			slToDelete.add(serviceLevelId12);

			if (serviceLevelId1 != -1) {

				childserviceLevelId1s = checkIfCSLCreatedOnServiceLevel(serviceLevelId1, customAssert);
				childserviceLevelId1s1 = checkIfCSLCreatedOnServiceLevel(serviceLevelId12, customAssert);

				addCSLToDelete(childserviceLevelId1s);
				addCSLToDelete(childserviceLevelId1s1);

//				int numberOfChildServiceLevel = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "numberofchildservicelevel"));
//
//				if (!(childserviceLevelId1s.size() >= numberOfChildServiceLevel)) {
//
//					customAssert.assertTrue(false, "For Service Level Id " + serviceLevelId1 + " Number of Child Service Level Expected are " + numberOfChildServiceLevel + " Actual " + childserviceLevelId1s.size());
//					customAssert.assertAll();
//				}
//
//				if (!(childserviceLevelId1s1.size() >= numberOfChildServiceLevel)) {
//
//					customAssert.assertTrue(false, "For Service Level Id 2 " + serviceLevelId12 + " Number of Child Service Level Expected are " + numberOfChildServiceLevel + " Actual " + childserviceLevelId1s1.size());
//					customAssert.assertAll();
//				}
//
//				//Validating there should not be any data on view template on structure performance data when no template is uploaded
//				if (!validateViewTemplate(Integer.parseInt(childserviceLevelId1s.get(0)), null, customAssert)) {
//					customAssert.assertTrue(false, "View Template validated unsuccessfully when no template uploaded on service level");
//				}
//
//				if (!validatePerformanceDataFormatTab(serviceLevelId1, "", customAssert)) {
//
//					customAssert.assertTrue(false, "Error while validating Performance Data Format Tab");
//				}
//
//				String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilename");
//				String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");
////
//				currentTimeStampForSLTemplateUploadCSLCreation = getCurrentTimeStamp();
//
//				if (!uploadPerformanceDataFormat(serviceLevelId1, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
//					customAssert.assertTrue(false, "Error while performance data file upload");
////					customAssert.assertAll();
//				}
//
//				if (!validatePerformanceDataFormatTab(serviceLevelId1, performanceDataFormatFileName, customAssert)) {
//
//					customAssert.assertTrue(false, "Error while validating Performance Data Format Tab");
//				}
//
//				if (!validateDownloadFileFromPerformanceTab(serviceLevelId1, performanceDataFormatFileName + RandomNumbers.getRandomNumberWithinRange(1, 10), performanceDataFormatFileName, customAssert)) {
//					customAssert.assertTrue(false, "Error while validating Download File From Performance Tab for SL ID " + serviceLevelId1);
//				}
//
//				if (!uploadPerformanceDataFormat(serviceLevelId12, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
//					customAssert.assertTrue(false, "Error while performance data file upload for Service Level 2");
//				}
//
//				if (!validatePerformanceDataFormatTab(serviceLevelId12, performanceDataFormatFileName, customAssert)) {
//
//					customAssert.assertTrue(false, "Error while validating Performance Data Format Tab for Service Level 2");
//				}
//
//				if (!validateDownloadFileFromPerformanceTab(serviceLevelId12, performanceDataFormatFileName + RandomNumbers.getRandomNumberWithinRange(1, 10), performanceDataFormatFileName, customAssert)) {
//					customAssert.assertTrue(false, "Error while validating Download File From Performance Tab for service level  2" );
//				}



			}

		} catch (Exception e) {
			logger.error("Exception while creation of CSL");
			customAssert.assertTrue(false, "Exception while creation of CSL" + e.getMessage());
		}

		customAssert.assertAll();
	}

	//C10440  C13523
	@Test(groups = {"sanity"},dependsOnMethods = "TestCSLCreation", enabled = false)     //Completed
	public void TestCSLComputationStatusDataNotUploaded() {

		CustomAssert customAssert = new CustomAssert();

		logger.info("Validating Child Service Level Computation Status");

		int cSLId = Integer.parseInt(childserviceLevelId1s.get(0));
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
	@Test(groups = {"sanity","PCQ DCQ UDC Update1"},dependsOnMethods = "TestCSLCreation", enabled = false)     //Completed
	public void TestCSLComputationStatusDataMarkedForAggregation() {

		CustomAssert customAssert = new CustomAssert();
		Show show = new Show();

		logger.info("Validating Child Service Level Computation Status");

		int cSLId = Integer.parseInt(childserviceLevelId1s.get(1));
		String rawDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilename");
		String completedBy = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "createdbyuser");
		if (!uploadRawDataCSL(cSLId, rawDataFileName, rawDataFileValidMsg, customAssert)) {

			customAssert.assertTrue(false, "Raw Data Uploaded unsuccessfully on CSL " + cSLId);
//			customAssert.assertAll();

		} else {
			if (!validateStructuredPerformanceDataCSL(cSLId, rawDataFileName, "Done", "View Structured Data", completedBy, customAssert)) {
				customAssert.assertTrue(false, "Raw Data File Upload validation unsuccessful");
//				customAssert.assertAll();
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

	//C10440 C13527 C13528 C10565 C10775
	@Test(groups = {"PCQ DCQ UDC Update1"},dependsOnMethods = "TestCSLCreation", enabled = false)     //Completed
	public void TestCSLCompStatus_DataMarkedComputation_And_ComputationCompletedSuccessfully() {

		CustomAssert customAssert = new CustomAssert();

		logger.info("Validating Child Service Level Computation Status");

		try {

			int cSLId = Integer.parseInt(childserviceLevelId1s.get(1));

			String rawDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilename");

			if (!uploadRawDataCSL(cSLId, rawDataFileName, rawDataFileValidMsg, customAssert)) {

				customAssert.assertTrue(false, "Raw Data Uploaded unsuccessfully on CSL " + cSLId);
//				customAssert.assertAll();
			} else {

				if (!validateStructuredPerformanceDataCSL(cSLId, rawDataFileName, "Done", "View Structured Data", auditLogUser, customAssert)) {
					customAssert.assertTrue(false, "Raw Data Upload validation unsuccessful for CSL ID " + cSLId);
//					customAssert.assertAll();
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
//				String workFlowActionToPerform = "ReComputePerformance";
//				WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
//				Boolean workFlowStatus = workflowActionsHelper.performWorkflowAction(cslEntityTypeId, childserviceLevelId1, workFlowActionToPerform);

			if (!workFlowStatus) {
//					customAssert.assertTrue(false, "Unable to perform " + workFlowActionToPerform + " computation on CSL Id " + childserviceLevelId1);
				customAssert.assertTrue(false, "Unable to perform  computation on CSL Id " + cSLId);
			}
//			if (!workFlowStatus) {
//				customAssert.assertTrue(false, "Unable to perform " + workFlowActionToPerform + " on CSL Id " + cSLId);
//			}
			show.hitShowVersion2(cslEntityTypeId, cSLId);
			showPageResponse = show.getShowJsonStr();

			String computationStatus = ShowHelper.getValueOfField("computationstatus", showPageResponse);

			if (!(computationStatus.equalsIgnoreCase("Data Marked for Aggregation") || computationStatus.equalsIgnoreCase("Computation Completed Successfully"))) {

				customAssert.assertTrue(false, "Computation Status Expected \"Data Marked for Computation\" Actual Computation Status " + computationStatus);

			} else {
				customAssert.assertTrue(true, "Computation Status validated successfully");
			}



			Thread.sleep(12000);

			show.hitShowVersion2(cslEntityTypeId, cSLId);
			showPageResponse = show.getShowJsonStr();
			computationStatus = ShowHelper.getValueOfField("computationStatus", showPageResponse);

			if (!computationStatus.equalsIgnoreCase("Computation Completed Successfully")) {

				customAssert.assertTrue(false, "Computation Status Expected \"Computation Completed Successfully\" Actual Computation Status " + computationStatus);

			} else {
				customAssert.assertTrue(true, "Computation Status validated successfully");
			}

		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while validating status Computation Completed Successfully");
		}

		customAssert.assertAll();
	}

	//    C10562
	@Test(groups = {"PCQ DCQ UDC Update1"},dependsOnMethods = "TestCSLCreation", enabled = false)         //Revalidated 17 june  //Completed
	public void TestUploadPerformanceDataFormatWrongFileType() {

		CustomAssert customAssert = new CustomAssert();

		String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "sltemplatefilenamepdf");

		String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadfailuremsgwrongfiletype");

		if (!uploadPerformanceDataFormat(serviceLevelId1, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
			customAssert.assertTrue(false, "Wrong data file upload validation unsuccessful");
//			customAssert.assertAll();
		}

		performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "sltemplatefilenametxt");
		expectedMsg = "500:;basic:;Please attach data sheet";
		if (!uploadPerformanceDataFormat(serviceLevelId1, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
			customAssert.assertTrue(false, "Wrong data file upload validation unsuccessful");
//			customAssert.assertAll();
		}
		customAssert.assertAll();
	}

	//C10440  C10484 C13529 C10775 C10423
	@Test(groups = {"sanity","PCQ DCQ UDC Update1"}, dependsOnMethods = "TestCSLCreation",enabled = false,priority = 0)
	public void TestCSLCompStatus_ErrorInComputation() {

		CustomAssert customAssert = new CustomAssert();
		String flowToTest = "sl automation flow error in computation";
		Show show = new Show();

		//Adding performanceComputationCalculationQuery hardcoded as unable to read from cfg file properly
		String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"iffff(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['Total Business Elapsed Time (Days)'].value < 20){if(doc['exception'].value== false){params._agg.map.met++}else{params._agg.map.notMet++}}else{if(doc['exception'].value== false){params._agg.map.notMet++}else{params._agg.map.met++}}}} if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['exception'].value== false){params._agg.map.credit += doc['Applicable Credit'].value}}} \", \"init_script\": \"params._agg['map'] =['met': 0.0, 'notMet': 0, 'credit':0]\", \"reduce_script\": \"params.return_map = ['Final_Numerator':0.0, 'Final_Denominator':0, 'Final_Performance':0 ,'Actual_Numerator':0.0, 'Actual_Denominator':0, 'Actual_Performance':0 ,'SL_Met':0 ,     'Calculated_Credit_Amount':0.0]; for (a in params._aggs){params.return_map.Final_Numerator += (float)(a.map.met); params.return_map.Final_Denominator += (float)(a.map.met + a.map.notMet) ; params.return_map.Calculated_Credit_Amount += (float)(a.map.credit);}  if(params.return_map.Final_Denominator > 0){params.return_map.Final_Performance = Math.round(((float)(params.return_map.Final_Numerator*100)/params.return_map.Final_Denominator)*100.0)/100.0}else{params.return_map.Final_Performance ='';params.return_map.SL_Met =5} params.return_map.Final_Numerator = Math.round(params.return_map.Final_Numerator *100.0)/100.0 ;params.return_map.Actual_Numerator = params.return_map.Final_Numerator; params.return_map.Actual_Denominator = params.return_map.Final_Denominator; params.return_map.Actual_Performance = params.return_map.Final_Performance; if(params.return_map.Final_Denominator > 0){if(params.return_map.Final_Performance < 90){params.return_map.Calculated_Credit_Amount = params.return_map.Calculated_Credit_Amount} else{params.return_map.Calculated_Credit_Amount = ''}} else{params.return_map.Calculated_Credit_Amount = ''}  return params.return_map\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}, {\"matcheeee\": {\"useInComputation\": true}}]}}}";
		String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
		String UDC = "Incident ID";

		int childserviceLevelId1ErrorInComputation = -1;

		try {

//			serviceLevelId1ErrorInComputation = getActiveserviceLevelId1(flowToTest, PCQ, DCQ, auditLogUser, customAssert);

			ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
			Boolean editStatus = serviceLevelHelper.editPCQDCQUDC(serviceLevelId1,PCQ,DCQ,UDC,customAssert);

			if(!editStatus){
				customAssert.assertTrue(false,"Error while editing PCQ DCQ UDC for SL" );
				customAssert.assertAll();
			}

			String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilename");
			String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");
			if (!uploadPerformanceDataFormat(serviceLevelId1, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
				customAssert.assertTrue(false, "Error while performance data file upload");
			}

			if (!validatePerformanceDataFormatTab(serviceLevelId1, performanceDataFormatFileName, customAssert)) {

				customAssert.assertTrue(false, "Error while validating Performance Data Format Tab for SL Id " + serviceLevelId1);
			}

//				childserviceLevelId1ErrorInComputation = Integer.parseInt(childserviceLevelId1sErrorInComputation.get(1));
			childserviceLevelId1ErrorInComputation = Integer.parseInt(childserviceLevelId1s.get(8));

			String rawDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilename");

			if (!uploadRawDataCSL(childserviceLevelId1ErrorInComputation, rawDataFileName, rawDataFileValidMsg, customAssert)) {
//
				customAssert.assertTrue(false, "Raw Data Uploaded unsuccessfully on CSL " + childserviceLevelId1ErrorInComputation);
//						customAssert.assertAll();
			}

			if (!validateStructuredPerformanceDataCSL(childserviceLevelId1ErrorInComputation, rawDataFileName, "Done", "View Structured Data", auditLogUser, customAssert)) {
				customAssert.assertTrue(false, "Structure Performance Data Tab validated unsuccessfully for CSL ID " + childserviceLevelId1ErrorInComputation);
			}

			List<String> workFlowSteps = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "cslerrorincomputationworkflowsteps").split("->"));

			String timeStamp = getCurrentTimeStamp().get(0).get(0);

			//Performing workflow Actions till Error in computation
			if (!performWorkFlowActions(cslEntityTypeId, childserviceLevelId1ErrorInComputation, workFlowSteps, auditLogUser, customAssert)) {
				customAssert.assertTrue(false, "Error while performing workflow actions on CSL ID " + childserviceLevelId1ErrorInComputation);
				customAssert.assertAll();
			}

			Thread.sleep(15000);
			show.hitShowVersion2(cslEntityTypeId, childserviceLevelId1ErrorInComputation);
			String showPageResponse = show.getShowJsonStr();

			String computationStatus = ShowHelper.getValueOfField("computationstatus", showPageResponse);

			if (!computationStatus.equalsIgnoreCase("Error in Computation")) {

				customAssert.assertTrue(false, "Computation Status Expected \"Error in Computation\" Actual Computation Status " + computationStatus);

			} else {
				customAssert.assertTrue(true, "Computation Status validated successfully");
			}

			show.hitShowVersion2(cslEntityTypeId, childserviceLevelId1ErrorInComputation);
			String showResponse = show.getShowJsonStr();

			String shortCodeId = ShowHelper.getValueOfField("short code id", showResponse);
			String description = ShowHelper.getValueOfField("description", showResponse);

//                    C10423 C10483
			String subjectLine = "Error in performance computation - (#" + shortCodeId + ")-(#" + description + ")";

			List<List<String>> recordFromSystemEmailTable = getRecordFromSystemEmailTable(subjectLine, timeStamp);
			if (recordFromSystemEmailTable.size() == 0) {
				customAssert.assertTrue(false, "No entry in system email table for Error in performance computation for CSL ID" + shortCodeId);
			}

			if (!validateAttachmentName(recordFromSystemEmailTable, null, customAssert)) {
				customAssert.assertTrue(false, "Attachment Name validated unsuccessfully in system emails table for CSL Raw Data Upload for CSL " + shortCodeId);
			}

//					if (!validateSentSuccessfullyFlag(recordFromSystemEmailTable, customAssert)) {
//						customAssert.assertTrue(false, "Sent Successfully Flag validated unsuccessfully in system emails table for CSL Raw Data Metadata Upload for SL " + shortCodeId);
//					}

		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while validating computation status for SL ID " + serviceLevelId1 + "and CSL ID " + childserviceLevelId1ErrorInComputation + e.getMessage());
		}

		customAssert.assertAll();
	}

	//C10423
	@Test(groups = {"sanity","PCQ DCQ UDC Update1"},dependsOnMethods = "TestCSLCreation",enabled = false)    //Completed
	public void TestCSLStatus_DCQInError() {

		CustomAssert customAssert = new CustomAssert();
		String flowToTest = "sl automation flow error in computation";
		Show show = new Show();
		int serviceLevelId1ErrorInComputation = serviceLevelId1;
		int childserviceLevelId1ErrorInComputation = -1;

		//Adding performanceComputationCalculationQuery hardcoded as unable to read from cfg file properly
		String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['Total Business Elapsed Time (Days)'].value < 20){if(doc['exception'].value== false){params._agg.map.met++}else{params._agg.map.notMet++}}else{if(doc['exception'].value== false){params._agg.map.notMet++}else{params._agg.map.met++}}}} if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['exception'].value== false){params._agg.map.credit += doc['Applicable Credit'].value}}} \", \"init_script\": \"params._agg['map'] =['met': 0.0, 'notMet': 0, 'credit':0]\", \"reduce_script\": \"params.return_map = ['Final_Numerator':0.0, 'Final_Denominator':0, 'Final_Performance':0 ,'Actual_Numerator':0.0, 'Actual_Denominator':0, 'Actual_Performance':0 ,'SL_Met':0 ,     'Calculated_Credit_Amount':0.0]; for (a in params._aggs){params.return_map.Final_Numerator += (float)(a.map.met); params.return_map.Final_Denominator += (float)(a.map.met + a.map.notMet) ; params.return_map.Calculated_Credit_Amount += (float)(a.map.credit);}  if(params.return_map.Final_Denominator > 0){params.return_map.Final_Performance = Math.round(((float)(params.return_map.Final_Numerator*100)/params.return_map.Final_Denominator)*100.0)/100.0}else{params.return_map.Final_Performance ='';params.return_map.SL_Met =5} params.return_map.Final_Numerator = Math.round(params.return_map.Final_Numerator *100.0)/100.0 ;params.return_map.Actual_Numerator = params.return_map.Final_Numerator; params.return_map.Actual_Denominator = params.return_map.Final_Denominator; params.return_map.Actual_Performance = params.return_map.Final_Performance; if(params.return_map.Final_Denominator > 0){if(params.return_map.Final_Performance < 90){params.return_map.Calculated_Credit_Amount = params.return_map.Calculated_Credit_Amount} else{params.return_map.Calculated_Credit_Amount = ''}} else{params.return_map.Calculated_Credit_Amount = ''}  return params.return_map\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}, {\"match\": {\"useInComputation\": true}}]}}}";
		String DCQ = "{\"query123\": {\"bool\": {\"must\": [{\"match\": {\"childslaId12345\": \"childSLAId\"}}]}}}";

		try {

			String UDC = "Incident ID";
			ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();

			Boolean editStatus = serviceLevelHelper.editPCQDCQUDC(serviceLevelId1,PCQ,DCQ,UDC,customAssert);

			if(!editStatus){
				customAssert.assertTrue(false,"Error while editing PCQ DCQ UDC for SL" );
				customAssert.assertAll();
			}

			if (serviceLevelId1ErrorInComputation != -1) {

				String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilename");
				String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");
				if (!uploadPerformanceDataFormat(serviceLevelId1ErrorInComputation, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
					customAssert.assertTrue(false, "Error while performance data file upload");
//					customAssert.assertAll();
				}

				if (!validatePerformanceDataFormatTab(serviceLevelId1ErrorInComputation, performanceDataFormatFileName, customAssert)) {

					customAssert.assertTrue(false, "Error while validating Performance Data Format Tab for SL Id " + serviceLevelId1);
				}

				childserviceLevelId1ErrorInComputation = Integer.parseInt(childserviceLevelId1s.get(2));

				String rawDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilename");

				String timeStamp = getCurrentTimeStamp().get(0).get(0);
				Thread.sleep(5000);
				if (childserviceLevelId1ErrorInComputation != -1) {

					if (!uploadRawDataCSL(childserviceLevelId1ErrorInComputation, rawDataFileName, rawDataFileValidMsg, customAssert)) {
//
						customAssert.assertTrue(false, "Raw Data Uploaded unsuccessfully on CSL " + childserviceLevelId1ErrorInComputation);
//						customAssert.assertAll();
					}

					if (!validateStructuredPerformanceDataCSL(childserviceLevelId1ErrorInComputation, rawDataFileName, "Done With Error", null, auditLogUser, customAssert)) {
						customAssert.assertTrue(false, "Structure Performance Data Tab validated unsuccessfully for CSL ID " + childserviceLevelId1ErrorInComputation);
					}

					show.hitShowVersion2(cslEntityTypeId, childserviceLevelId1ErrorInComputation);
					String showResponse = show.getShowJsonStr();

					String shortCodeId = ShowHelper.getValueOfField("short code id", showResponse);
					String description = ShowHelper.getValueOfField("description", showResponse);

//                    C10423 C13535 C13536 C13531
					String subjectLine = "Error in data calculation (#" + shortCodeId + ")-(#" + description + ")";

					List<List<String>> recordFromSystemEmailTable = getRecordFromSystemEmailTable(subjectLine, timeStamp);
					if (recordFromSystemEmailTable.size() == 0) {
						customAssert.assertTrue(false, "No entry in system email table for CSL Raw Data Upload for CSL " + shortCodeId);
					}

					if (!validateAttachmentName(recordFromSystemEmailTable, null, customAssert)) {
						customAssert.assertTrue(false, "Attachment Name validated unsuccessfully in system emails table for CSL Raw Data Upload for CSL " + shortCodeId);
					}

//                    if(!validateSentSuccessfullyFlag(recordFromSystemEmailTable,customAssert)){
//                        customAssert.assertTrue(false,"Sent Successfully Flag validated unsuccessfully in system emails table for CSL Raw Data Metadata Upload for SL " + shortCodeId);
//                    }

//					List<String> workFlowSteps = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "cslerrorincomputationworkflowsteps").split("->"));
//
//					//Performing workflow Actions till Error in computation
//					if (!performWorkFlowActions(cslEntityTypeId, childserviceLevelId1ErrorInComputation, workFlowSteps, auditLogUser, customAssert)) {
//						customAssert.assertTrue(false, "Error while performing workflow actions on SL ID " + serviceLevelId1ErrorInComputation);
//						customAssert.assertAll();
//					}

					performComputationCSL(childserviceLevelId1ErrorInComputation,customAssert);

					Thread.sleep(5000);

					show.hitShowVersion2(cslEntityTypeId, childserviceLevelId1ErrorInComputation);
					String showPageResponse = show.getShowJsonStr();

					String computationStatus = ShowHelper.getValueOfField("performancestatus", showPageResponse);

					if (!computationStatus.equalsIgnoreCase("Computation Approved")) {

						customAssert.assertTrue(false, "Computation Status Expected \"Computation Approved\" Actual Computation Status " + computationStatus);

					} else {
						customAssert.assertTrue(true, "Computation Status validated successfully");
					}

				} else {
					customAssert.assertTrue(false, "Child service not created for SL ID " + serviceLevelId1ErrorInComputation);
				}

			} else {
				customAssert.assertTrue(false, "Service level id equals to -1");
			}


		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while validating computation status for SL ID " + serviceLevelId1ErrorInComputation + "and CSL ID " + childserviceLevelId1ErrorInComputation + e.getMessage());
		}

		customAssert.assertAll();
	}

	//    C10647
	@Test(groups = {"PCQ DCQ UDC Update1"},dependsOnMethods = "TestCSLCreation", enabled = false)     //Completed
	public void TestRawDataFileMultipleSheets() {

		CustomAssert customAssert = new CustomAssert();
		int cSLId = -1;
		try {
			cSLId = Integer.parseInt(childserviceLevelId1s.get(5));
			String rawDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilenamemultiplesheets");
			String rawDataFileIncorrectTemplateMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilemultiplesheeterrmsg");

			if (!uploadRawDataCSL(cSLId, rawDataFileName, rawDataFileIncorrectTemplateMsg, customAssert)) {

				customAssert.assertTrue(false, "Raw Data Uploaded unsuccessfully on CSL " + cSLId);
//				customAssert.assertAll();
			}

		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while validating raw data upload status for CSL ID " + cSLId + e.getMessage());
		}

		customAssert.assertAll();
	}

	//    C10591 C10746
	@Test(groups = {"sanity","PCQ DCQ UDC Update1"},dependsOnMethods = "TestCSLCreation", enabled = false)
	public void TestOperationOnRawDataTab() {

		CustomAssert customAssert = new CustomAssert();
		HashMap<String, String> viewHistoryMap = new HashMap<>();
		List<Map<String, String>> viewHistoryMapList = new ArrayList<>();

		try {
			int cSlId = Integer.parseInt(childserviceLevelId1s.get(5));
//		cSlId = 37574;

			String rawDataFile = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilename");

			try {
				if (!uploadRawDataCSL(cSlId, rawDataFile, rawDataFileValidMsg, customAssert)) {
					customAssert.assertTrue(false, "Raw Data upload unsuccessful on CSL ID " + cSlId);
//			customAssert.assertAll();
				}
				if (!validateStructuredPerformanceDataCSL(cSlId, rawDataFile, "Done", "View Structured Data", auditLogUser, customAssert)) {

					customAssert.assertTrue(false, "Error validating structure performance data tab on CSL ID " + cSlId);
//			customAssert.assertAll();
				}
			}catch (Exception e){
				customAssert.assertTrue(false,"Exception while uploading raw data " + e.getStackTrace());
			}

//		int numberOfLineItemsOnRawData = 10;
			int numberOfLineItemsOnRawData = 6;
			try {
				if (!validateAddException(cSlId, numberOfLineItemsOnRawData, customAssert)) {
					customAssert.assertTrue(false, "Add Exception Case Validated Successfully for CSL Id " + cSlId);
				}

				viewHistoryMap = createViewHistoryMap("No", "Yes", "Exception", "Add Exception");

				viewHistoryMapList.add(viewHistoryMap);
				if (!validateAuditLogWithHistory(cslEntityTypeId, cSlId, "Performance data updated", viewHistoryMapList, auditLogUser, customAssert)) {
					customAssert.assertTrue(false, "Audit Log validation after Add Exception validated unsuccessfully");
				}
			}catch (Exception e){
				logger.error("Exception while validating Add Exception");
				customAssert.assertTrue(false,"Exception while validating Add Exception");
			}

			try {
				if (!validateRemoveException(cSlId, numberOfLineItemsOnRawData, customAssert)) {
					customAssert.assertTrue(false, "Remove Exception Case Validated Successfully for CSL Id " + cSlId);
				}

				viewHistoryMap = createViewHistoryMap("Yes", "No", "Exception", "Remove Exception");
				viewHistoryMapList.clear();
				viewHistoryMapList.add(viewHistoryMap);

				if (!validateAuditLogWithHistory(cslEntityTypeId, cSlId, "Performance data updated", viewHistoryMapList, auditLogUser, customAssert)) {
					customAssert.assertTrue(false, "Audit Log validation after Remove Exception validated unsuccessfully");
				}
			}catch (Exception e){
				logger.error("Exception while validating Remove Exception");
				customAssert.assertTrue(false,"Exception while validating Remove Exception");
			}

			try {
				if (!validateInclude(cSlId, numberOfLineItemsOnRawData, customAssert)) {
					customAssert.assertTrue(false, "Include Case Validated UnSuccessfully for CSL Id " + cSlId);
				}

				viewHistoryMap = createViewHistoryMap("No", "Yes", "Active", "Include");
				viewHistoryMapList.clear();
				viewHistoryMapList.add(viewHistoryMap);
				if (!validateAuditLogWithHistory(cslEntityTypeId, cSlId, "Performance data updated", viewHistoryMapList, auditLogUser, customAssert)) {
					customAssert.assertTrue(false, "Audit Log validation after include function validated unsuccessfully");
				}
			}catch (Exception e){
				logger.error("Exception while validating Include");
				customAssert.assertTrue(false,"Exception while validating Include");
			}

			try {
				if (!validateExclude(cSlId, numberOfLineItemsOnRawData, customAssert)) {
					customAssert.assertTrue(false, "Exclude Case Validated Successfully for CSL Id " + cSlId);
				}


				viewHistoryMap = createViewHistoryMap("Yes", "No", "Active", "Exclude");
				viewHistoryMapList.clear();
				viewHistoryMapList.add(viewHistoryMap);
				if (!validateAuditLogWithHistory(cslEntityTypeId, cSlId, "Performance data updated", viewHistoryMapList, auditLogUser, customAssert)) {
					customAssert.assertTrue(false, "Audit Log validation after include function validated unsuccessfully");
				}
			}catch (Exception e){
				logger.error("Exception while validating Exclude");
				customAssert.assertTrue(false,"Exception while validating Exclude");
			}

			try {
				String filterQueryRawDataTab = "{\"query\": {\"bool\": {\"filter\": []}}}";

				int startingRecordForRawData = 8;
				if (!validateDownloadRawData(String.valueOf(cSlId), filterQueryRawDataTab,
						"", startingRecordForRawData, customAssert)) {
					customAssert.assertTrue(false, "Download Raw Data Validation Unsuccessful for CSL ID " + cSlId);
				}

			}catch (Exception e){
				logger.error("Exception while validating download raw data functionality");
				customAssert.assertTrue(false,"Exception while validating download raw data functionality");
			}
			try {
				String excelSheetNameRawData = "Format Sheet";
				int startingRowNum = 2;
				String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilename");
				List<List<String>> viewTemplateListExcel = prepareViewTemplateListExcel(uploadFilePath, performanceDataFormatFileName, excelSheetNameRawData, startingRowNum, 5, customAssert);

				if (!validateViewTemplate(cSlId, viewTemplateListExcel, customAssert)) {
					customAssert.assertTrue(false, "View Template functionality validated unsuccessfully for CSL ID " + cSlId + " using excel template " + rawDataFile);
				}
			}catch (Exception e){
				logger.error("Exception while validating download view Template List Excel");
				customAssert.assertTrue(false,"Exception while validating download view Template List Excel");
			}
		}catch (Exception e){
			logger.error("Exception while validating the scenario " + e.getStackTrace());
			customAssert.assertTrue(false,"Exception while validating the scenario " + e.getStackTrace());
		}

		customAssert.assertAll();
	}

	@Test(groups = {"sanity","PCQ DCQ UDC Update1"},dependsOnMethods = "TestCSLCreation",enabled = false,priority = 0)
	public void TestIgnoreFileScenario(){

		CustomAssert customAssert = new CustomAssert();
		int cslID = Integer.parseInt(childserviceLevelId1s1.get(15));

		try {
			//Checking ignore file scenario

			String rawDataFile = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilename");
			String timeStamp = getCurrentTimeStamp().get(0).get(0);
			if (!uploadRawDataCSL(cslID, rawDataFile, rawDataFileValidMsg, customAssert)) {
				customAssert.assertTrue(false, "Raw Data upload unsuccessful on CSL ID " + cslID);
			}
			if (!validateStructuredPerformanceDataCSL(cslID, rawDataFile, "Done", "View Structured Data", auditLogUser, customAssert)) {

				customAssert.assertTrue(false, "Error validating structure performance data tab on CSL ID " + cslID);
//			customAssert.assertAll();
			}

			List<List<String>> recordFromSystemEmailTable;
			Show show = new Show();

			try {
				show.hitShowVersion2(slEntityTypeId, serviceLevelId1);
				String showResponse = show.getShowJsonStr();
				String shortCodeId = ShowHelper.getValueOfField("short code id", showResponse);
				String subjectLine = "SL Raw Data Metadata Upload " + shortCodeId;

				//				C10582

				recordFromSystemEmailTable= getRecordFromSystemEmailTable(subjectLine, timeStamp);
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
					String subjectLine = "SL Computation (" + shortCodeId + ") -  ignore raw data file request response";
					List<String> expectedSentencesInBody = new ArrayList<>();
					expectedSentencesInBody.add("To calculate the updated scores, please re-compute the performance.");
					expectedSentencesInBody.add("Your ignore raw data file request has been completed.");

					recordFromSystemEmailTable = getRecordFromSystemEmailTable(subjectLine, timeStamp);

					if (recordFromSystemEmailTable.size() == 0) {
						customAssert.assertTrue(false, "No entry in system email table for subject Line " + subjectLine);
					}

					if (!validateAttachmentName(recordFromSystemEmailTable, null, customAssert)) {
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

	//    C10402  C10442 C10444 C13518 C13520 C10470 C10554
	@Test(groups = {"sanity","PCQ DCQ UDC Update1"},dependsOnMethods = "TestCSLCreation",enabled = false)
	public void TestTargetValueCalculationBasedOnPCQ1() {

		CustomAssert customAssert = new CustomAssert();
//		String flowToTest = "sl misc 3";

		//Adding performanceComputationCalculationQuery hardcoded as unable to read from cfg file properly
		String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\", \"init_script\": \"state['map'] = ['met':0, 'notMet':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;params.result.Actual_Numerator = 20;params.result.Actual_Denominator = 100;params.result.Supplier_Numerator = 10;params.result.Supplier_Denominator = 0;params.result.Final_Numerator = 10;params.result.Final_Denominator = 30;} return params.result\", \"combine_script\": \"return state;\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
		String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
		String UDC = "Incident ID";
		try {

			ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
			Boolean editStatus = serviceLevelHelper.editPCQDCQUDC(serviceLevelId1,PCQ,DCQ,UDC,customAssert);

			if(!editStatus){
				customAssert.assertTrue(false,"Error while editing PCQ DCQ UDC for SL" );
				customAssert.assertAll();
			}

			int serviceLevelId1MiscFlow = serviceLevelId1;

			String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "sltemplatepcqcalc");
			String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");
//
			if (!uploadPerformanceDataFormat(serviceLevelId1MiscFlow, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
				customAssert.assertTrue(false, "Error while performance data file upload");
//				customAssert.assertAll();

			}

			if (!validatePerformanceDataFormatTab(serviceLevelId1MiscFlow, performanceDataFormatFileName, customAssert)) {

				customAssert.assertTrue(false, "Error while validating Performance Data Format Tab");
			}


			int cSLId = Integer.parseInt(childserviceLevelId1s.get(16));

			String rawDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "cslrawdatapcqcalc");

			if (!uploadRawDataCSL(cSLId, rawDataFileName, rawDataFileValidMsg, customAssert)) {

				customAssert.assertTrue(false, "Raw Data Uploaded unsuccessfully on CSL " + cSLId);
//					customAssert.assertAll();
			} else {
				if (!validateStructuredPerformanceDataCSL(cSLId, rawDataFileName, "Done", "View Structured Data", auditLogUser, customAssert)) {
					customAssert.assertTrue(false, "Raw Data File Upload validation unsuccessful for CSL ID " + cSLId);
//						customAssert.assertAll();
				}
			}

			Boolean workFlowStatus = performComputationCSL(cSLId, customAssert);

			if (!workFlowStatus) {
//					customAssert.assertTrue(false, "Unable to perform " + workFlowActionToPerform + " computation on CSL Id " + childserviceLevelId1);
				customAssert.assertTrue(false, "Unable to perform  computation on CSL Id " + cSLId);
			}

			Show show = new Show();

			show.hitShowVersion2(cslEntityTypeId, cSLId);
			String showPageResponse = show.getShowJsonStr();

			String computationStatus = ShowHelper.getValueOfField("computationstatus", showPageResponse);

			if (!computationStatus.equalsIgnoreCase("Computation Completed Successfully")) {

				customAssert.assertTrue(false, "After 300 seconds Computation Status Expected \"Computation Completed Successfully\" Actual Computation Status " + computationStatus);
			} else {
				customAssert.assertTrue(true, "Computation Status validated successfully");
			}

			String sectionName = "tagetvaluescalculationflow1";

			List<String> allPropertiesOfSection = ParseConfigFile.getAllPropertiesOfSection(configFilePath, configFileName, sectionName);
			String propertyValue;
			HashMap<String, String> targetValuesMap = new HashMap<>();

			for (String property : allPropertiesOfSection) {
				propertyValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, sectionName, property);
				targetValuesMap.put(property, propertyValue);
			}

			if (!validateTargetValuesCsl(cSLId, targetValuesMap, customAssert)) {
				customAssert.assertTrue(false, "Target Values validated unsuccessfully on CSL " + cSLId);
			}


		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while validating Scenario TargetValueCalculationBasedOnPCQ1 " + e.getMessage());
		}

		customAssert.assertAll();
	}

	//C8168 C8172  C13518 5 july    Query corrected to test once
	@Test(groups = {"PCQ DCQ UDC Update2"},dataProvider = "slMetCalculationThroughES",dependsOnMethods = "TestCSLCreation", enabled = false)
	public void TestSLMETStatusCalculationESQuery(String flow, String DCQ, String PCQ, String expectedSLMetValue,int cslIdToUse) {

		CustomAssert customAssert = new CustomAssert();
		logger.info("Validating SL MET Status Calculation ES Query for the flow " + flow);

		Show show = new Show();

		int slIdForSLMetCalcESQuery;
		int cslIdForSLMetCalcESQuery;

		try {

			slIdForSLMetCalcESQuery = serviceLevelId12;

			if (slIdForSLMetCalcESQuery != -1) {

				ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();

				String UDC = "Incident ID";

				Boolean editStatus = serviceLevelHelper.editPCQDCQUDC(serviceLevelId1,PCQ,DCQ,UDC,customAssert);

				if(!editStatus){
					customAssert.assertTrue(false,"Error while editing PCQ DCQ UDC for SL" );
					customAssert.assertAll();
				}

				String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilenameesquery");
				String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");


				if (!validatePerformanceDataFormatTab(slIdForSLMetCalcESQuery, performanceDataFormatFileName, customAssert)) {

					if (!uploadPerformanceDataFormat(slIdForSLMetCalcESQuery, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
						customAssert.assertTrue(false, "Error while performance data file upload");
					}

					if (!validatePerformanceDataFormatTab(slIdForSLMetCalcESQuery, performanceDataFormatFileName, customAssert)) {

						customAssert.assertTrue(false, "Error while validating Performance Data Format Tab on Service Level");
					}
				}
				cslIdForSLMetCalcESQuery = Integer.parseInt(childserviceLevelId1s.get(cslIdToUse));

				String rawDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilenameesquery");

				if (!uploadRawDataCSL(cslIdForSLMetCalcESQuery, rawDataFileName, rawDataFileValidMsg, customAssert)) {

					customAssert.assertTrue(false, "Raw Data Uploaded unsuccessfully on CSL " + cslIdForSLMetCalcESQuery);

				} else {

					if (!validateStructuredPerformanceDataCSL(cslIdForSLMetCalcESQuery, rawDataFileName, "Done", "View Structured Data", auditLogUser, customAssert)) {
						customAssert.assertTrue(false, "Raw Data Upload validation unsuccessful for CSL ID " + cslIdForSLMetCalcESQuery);

					}
				}

				String showPageResponse;
				show.hitShowVersion2(cslEntityTypeId, cslIdForSLMetCalcESQuery);
				showPageResponse = show.getShowJsonStr();

				//Creating the List for Audit Log Verification

				String actualSupplierNumeratorBefore = ShowHelper.getValueOfField("suppliernumerator", showPageResponse);
				String actualSupplierDenominatorBefore = ShowHelper.getValueOfField("supplierdenominator", showPageResponse);
				String actualActualNumeratorBefore = ShowHelper.getValueOfField("actualnumerator", showPageResponse);
				String actualActualDenominatorBefore = ShowHelper.getValueOfField("actualdenominator", showPageResponse);
				String actualFinalNumeratorBefore = ShowHelper.getValueOfField("finalnumerator", showPageResponse);
				String actualFinalDenominatorBefore = ShowHelper.getValueOfField("finaldenominator", showPageResponse);
				String actualMaximumBefore = ShowHelper.getValueOfField("minmax", showPageResponse);
				String actualExpectedBefore = ShowHelper.getValueOfField("expected", showPageResponse);
				String actualSigMaxBefore = ShowHelper.getValueOfField("sigminmax", showPageResponse);

				Boolean workFlowStatus;

				workFlowStatus = performComputationCSL(cslIdForSLMetCalcESQuery, customAssert);
//				workFlowStatus = true;
				if (!workFlowStatus) {

					customAssert.assertTrue(false, "Unable to perform  computation on CSL Id " + cslIdForSLMetCalcESQuery);
				} else {
					Thread.sleep(5000);
					show.hitShowVersion2(cslEntityTypeId, cslIdForSLMetCalcESQuery);
					showPageResponse = show.getShowJsonStr();

					logger.info("Validating target field values for CSL ID " + cslIdForSLMetCalcESQuery);

					String actualActualNumerator = ShowHelper.getValueOfField("actualnumerator", showPageResponse);
					String actualActualDenominator = ShowHelper.getValueOfField("actualdenominator", showPageResponse);
					String actualSupplierNumerator = ShowHelper.getValueOfField("suppliernumerator", showPageResponse);
					String actualSupplierDenominator = ShowHelper.getValueOfField("supplierdenominator", showPageResponse);
					String actualPerformance = ShowHelper.getValueOfField("actualperformance", showPageResponse);
					String actualFinalNumerator = ShowHelper.getValueOfField("finalnumerator", showPageResponse);
					String actualFinalDenominator = ShowHelper.getValueOfField("finaldenominator", showPageResponse);
					String actualFinalPerformance = ShowHelper.getValueOfField("finalperformance", showPageResponse);
					String actualSLMetValue = ShowHelper.getValueOfField("slmetval", showPageResponse);
					String actualMaximum = ShowHelper.getValueOfField("minmax", showPageResponse);
					String actualExpected = ShowHelper.getValueOfField("expected", showPageResponse);
					String actualSigMax = ShowHelper.getValueOfField("sigminmax", showPageResponse);

					String expectedActualNumerator = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "estargetvaluescalculation", "actualnumerator");
					String expectedActualDenominator = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "estargetvaluescalculation", "actualdenominator");
					String expectedSupplierNumerator = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "estargetvaluescalculation", "suppliernumerator");
					String expectedSupplierDenominator = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "estargetvaluescalculation", "supplierdenominator");
					String expectedPerformance = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "estargetvaluescalculation", "actualperformance");
					String expectedFinalNumerator = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "estargetvaluescalculation", "finalnumerator");
					String expectedFinalDenominator = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "estargetvaluescalculation", "finaldenominator");
					String expectedFinalPerformance = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "estargetvaluescalculation", "finalperformance");

					if(flow.equalsIgnoreCase("SLMet4")
							|| flow.equalsIgnoreCase("SLMet5")){
//							|| flow.equalsIgnoreCase("SLMet7")) {
						expectedActualNumerator = null;
						expectedActualDenominator = null;
						expectedSupplierNumerator = null;
						expectedSupplierDenominator = null;
						expectedPerformance = null;
						expectedFinalNumerator = null;
						expectedFinalDenominator = null;
						expectedFinalPerformance = null;
					}else if(flow.equalsIgnoreCase("SLMet7")){
						expectedFinalNumerator = null;
						expectedFinalDenominator = null;
						expectedFinalPerformance = null;
					}


					if(actualActualNumerator != null) {
						if (!actualActualNumerator.equalsIgnoreCase(expectedActualNumerator)) {

							customAssert.assertTrue(false, "Expected and Actual Value for Actual Numerator mismatched after ES Calculation");
							customAssert.assertTrue(false, "Expected Actual Numerator value : " + expectedActualNumerator + " Actual Actual Numerator value : " + expectedActualNumerator);
						}
					}else {
						if(actualActualNumerator != expectedActualNumerator) {

							customAssert.assertTrue(false, "Expected Actual Numerator value : " + expectedActualNumerator + " Actual Actual Numerator value : " + expectedActualNumerator);
						}
					}

					if(actualActualDenominator != null) {
						if (!actualActualDenominator.equalsIgnoreCase(expectedActualDenominator)) {
							customAssert.assertTrue(false, "Expected and Actual Value for Actual Denominator mismatched after ES Calculation");
							customAssert.assertTrue(false, "Expected Actual Denominator value : " + expectedActualDenominator + " Actual Actual Denominator value : " + actualActualDenominator);
						}
					}else {
						if(actualActualDenominator != expectedActualDenominator) {

							customAssert.assertTrue(false, "Expected Actual Denominator value : " + expectedActualDenominator + " Actual Actual Denominator value : " + actualActualDenominator);
						}
					}

					if(actualSupplierNumerator != null) {
						if (!actualSupplierNumerator.equalsIgnoreCase(expectedSupplierNumerator)) {
							customAssert.assertTrue(false, "Expected and Actual Value for Supplier Numerator mismatched after ES Calculation");
							customAssert.assertTrue(false, "Expected Supplier Numerator value : " + expectedSupplierNumerator + " Actual Supplier Numerator value : " + actualSupplierNumerator);
						}
					}else {
						if(actualSupplierNumerator != expectedSupplierNumerator) {
							customAssert.assertTrue(false, "Expected Supplier Numerator value : " + expectedSupplierNumerator + " Actual Supplier Numerator value : " + actualSupplierNumerator);
						}

					}

					if(actualSupplierDenominator != null) {
						if (!actualSupplierDenominator.equalsIgnoreCase(expectedSupplierDenominator)) {
							customAssert.assertTrue(false, "Expected and Actual Value for Supplier Denominator mismatched after ES Calculation");
							customAssert.assertTrue(false, "Expected Supplier Denominator value : " + expectedSupplierDenominator + " Actual Supplier Denominator value : " + actualSupplierDenominator);
						}
					}else {
						if(actualSupplierDenominator != expectedSupplierDenominator) {
							customAssert.assertTrue(false, "Expected Supplier Denominator value : " + expectedSupplierDenominator + " Actual Supplier Denominator value : " + actualSupplierDenominator);
						}
					}

					if(actualPerformance != null) {
						if (!actualPerformance.equalsIgnoreCase(expectedPerformance)) {
							customAssert.assertTrue(false, "Expected and Actual Value for Actual Performance mismatched after ES Calculation");
							customAssert.assertTrue(false, "Expected Actual Performance value : " + expectedPerformance + " Actual Actual Performance value : " + actualPerformance);
						}

					}else {
						if(actualPerformance != expectedPerformance) {

							customAssert.assertTrue(false, "Expected Actual ExpectedPerformance value : " + expectedPerformance + " Actual ActualPerformance value : " + actualPerformance);
						}

					}

					if(actualFinalNumerator != null) {
						if (!actualFinalNumerator.equalsIgnoreCase(expectedFinalNumerator)) {
							customAssert.assertTrue(false, "Expected and Actual Value for Final Numerator mismatched after ES Calculation");
							customAssert.assertTrue(false, "Expected Final Numerator value : " + expectedFinalNumerator + " Actual Final Numerator value : " + actualFinalNumerator);
						}
					}else {

						if(actualFinalNumerator != expectedFinalNumerator) {
							customAssert.assertTrue(false, "Expected Actual actualFinalNumerator value : " + expectedFinalNumerator + " Actual ActualDenominator value : " + actualFinalNumerator);
						}
					}

					if(actualFinalDenominator != null) {
						if (!actualFinalDenominator.equalsIgnoreCase(expectedFinalDenominator)) {
							customAssert.assertTrue(false, "Expected and Actual Value for Final Denominator mismatched after ES Calculation");
							customAssert.assertTrue(false, "Expected Final Denominator value : " + expectedFinalDenominator + " Actual Final Denominator value : " + actualFinalDenominator);
						}
					}else {

						if(actualFinalDenominator != expectedFinalDenominator) {

							customAssert.assertTrue(false, "Expected Actual FinalDenominator value : " + expectedFinalDenominator + " Actual ActualDenominator value : " + actualFinalDenominator);
						}
					}

					if(actualFinalPerformance != null) {
						if (!actualFinalPerformance.equalsIgnoreCase(expectedFinalPerformance)) {
							customAssert.assertTrue(false, "Expected and Actual Value for Final Performance mismatched after ES Calculation");
							customAssert.assertTrue(false, "Expected Final Performance value : " + expectedFinalPerformance + " Actual Final Performance value : " + actualFinalNumerator);
						}
					}else {
						if(actualFinalPerformance != expectedFinalPerformance) {

							customAssert.assertTrue(false, "Expected Actual FinalPerformance value : " + expectedFinalPerformance + " Actual FinalPerformance value : " + actualFinalPerformance);
						}
					}

					if(actualSLMetValue != null) {
						if (!actualSLMetValue.equalsIgnoreCase(expectedSLMetValue)) {
							customAssert.assertTrue(false, "Expected and Actual Value for SL Met mismatched after ES Calculation");
							customAssert.assertTrue(false, "Expected SL Met value : " + expectedSLMetValue + " Actual SL Met value : " + actualSLMetValue);
						}
					}else{
						customAssert.assertTrue(false,"Actual value for actualSLMetValue is null on show page for CSL ID " + cslIdForSLMetCalcESQuery);
					}

					Map<String, String> viewHistoryMap;
					List<Map<String, String>> viewHistoryMapList = new ArrayList<>();

					viewHistoryMap = new HashMap<>();
					if (actualSupplierNumeratorBefore != null) {
						viewHistoryMap.put("oldValue", actualSupplierNumeratorBefore);
						viewHistoryMap.put("newValue", actualSupplierNumerator);
						viewHistoryMapList.add(viewHistoryMap);
					} else {
						viewHistoryMap.put("oldValue", "null");
						viewHistoryMap.put("newValue", actualSupplierNumerator);
						viewHistoryMapList.add(viewHistoryMap);
					}

					viewHistoryMap = new HashMap<>();
					if (actualSupplierDenominatorBefore != null) {
						viewHistoryMap = new HashMap<>();
						viewHistoryMap.put("oldValue", actualSupplierDenominatorBefore);
						viewHistoryMap.put("newValue", actualSupplierDenominator);
						viewHistoryMapList.add(viewHistoryMap);
					} else {
						viewHistoryMap.put("oldValue", "null");//viewHistoryMap.put("oldValue", "0");
						viewHistoryMap.put("newValue", actualSupplierDenominator);
						viewHistoryMapList.add(viewHistoryMap);
					}

					viewHistoryMap = new HashMap<>();
					if (actualActualNumeratorBefore != null) {
						viewHistoryMap = new HashMap<>();
						viewHistoryMap.put("oldValue", actualActualNumeratorBefore);
						viewHistoryMap.put("newValue", actualActualNumerator);
						viewHistoryMapList.add(viewHistoryMap);
					} else {
						viewHistoryMap.put("oldValue", "null");
						viewHistoryMap.put("newValue", actualActualNumerator);
						viewHistoryMapList.add(viewHistoryMap);
					}

					viewHistoryMap = new HashMap<>();
					if (actualActualDenominatorBefore != null) {
						viewHistoryMap = new HashMap<>();
						viewHistoryMap.put("oldValue", actualActualDenominatorBefore);
						viewHistoryMap.put("newValue", actualActualDenominator);
						viewHistoryMapList.add(viewHistoryMap);
					} else {
						viewHistoryMap.put("oldValue", "null");
						viewHistoryMap.put("newValue", actualActualDenominatorBefore);

						viewHistoryMapList.add(viewHistoryMap);
					}

					viewHistoryMap = new HashMap<>();
					if (actualFinalNumeratorBefore != null) {
						viewHistoryMap = new HashMap<>();
						viewHistoryMap.put("oldValue", actualFinalNumeratorBefore);
						viewHistoryMap.put("newValue", actualFinalNumerator);

						viewHistoryMapList.add(viewHistoryMap);
					} else {
						viewHistoryMap.put("oldValue", "null");
						viewHistoryMap.put("newValue", actualFinalNumerator);
						viewHistoryMapList.add(viewHistoryMap);
					}

					viewHistoryMap = new HashMap<>();
					if (actualFinalDenominatorBefore != null) {
						viewHistoryMap = new HashMap<>();
						viewHistoryMap.put("oldValue", actualFinalDenominatorBefore);
						viewHistoryMap.put("newValue", actualFinalDenominator);
						viewHistoryMapList.add(viewHistoryMap);
					} else {
						viewHistoryMap.put("oldValue", "null");
						viewHistoryMap.put("newValue", actualFinalDenominator);
						viewHistoryMapList.add(viewHistoryMap);
					}

					viewHistoryMap = new HashMap<>();
					if (actualMaximumBefore != null) {
						viewHistoryMap.put("oldValue", actualMaximumBefore);
						viewHistoryMap.put("newValue", actualMaximum);

						viewHistoryMapList.add(viewHistoryMap);
					} else {
						viewHistoryMap.put("oldValue", "null");
						viewHistoryMap.put("newValue", actualMaximum);

						viewHistoryMapList.add(viewHistoryMap);
					}

					viewHistoryMap = new HashMap<>();
					if (actualExpectedBefore != null) {
						viewHistoryMap.put("oldValue", actualExpectedBefore);
						viewHistoryMap.put("newValue", actualExpected);

						viewHistoryMapList.add(viewHistoryMap);
					} else {
						viewHistoryMap.put("oldValue", "null");
						viewHistoryMap.put("newValue", actualExpected);

						viewHistoryMapList.add(viewHistoryMap);
					}

					viewHistoryMap = new HashMap<>();
					if (actualSigMaxBefore != null) {
						viewHistoryMap.put("oldValue", actualSigMaxBefore);
						viewHistoryMap.put("newValue", actualSigMax);
						viewHistoryMapList.add(viewHistoryMap);
					} else {
						viewHistoryMap.put("oldValue", "null");
						viewHistoryMap.put("newValue", actualSigMax);
						viewHistoryMapList.add(viewHistoryMap);
					}

//					validateAuditLogWithHistory(cslEntityTypeId, cslIdForSLMetCalcESQuery, "Computation Approved", viewHistoryMapList, auditLogUser, customAssert);

				}
			} else {
				customAssert.assertTrue(false, "Unable to create service level ID");
			}

		} catch (Exception e) {
			logger.error("Exception while performing SL Computation through ES Query");
			customAssert.assertTrue(false, "Exception while performing SL Computation" + e.getMessage());
		}

		customAssert.assertAll();
	}

	//
//	//C10467 C10468
	@Test(groups = {"PCQ DCQ UDC Update1"},dependsOnMethods = "TestCSLCreation",enabled = false)
	public void TestCustomFieldValidation() {

		CustomAssert customAssert = new CustomAssert();
		Show show = new Show();
		String flowToTest = "sl automation flow";
		try {

			String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\", \"init_script\": \"state['map'] = ['met':0, 'notMet':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30,'Service_Level_Integer_Custom_Field_API_Automation':10,'Service_Level_String_Custom_Field_API_Automation':'String']; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;params.result.Actual_Numerator = 20;params.result.Actual_Denominator = 100;params.result.Supplier_Numerator = 10;params.result.Supplier_Denominator = 0;params.result.Final_Numerator = 10;params.result.Final_Denominator = 30;} return params.result\", \"combine_script\": \"return state;\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
			String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Met/Missed\": {\"script\": \"if(params['_source']['Time Taken (Secondss1)'] != ''){if(params['_source']['Time Taken (Seconds)'] < 110000){return 'Met'}else{return 'Missed'}}else{return 'Missed'}\"}}}";

			slToDelete.add(serviceLevelId1);

			if (serviceLevelId1 != -1) {
//
				String UDC = "Incident ID";

				ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
				Boolean editStatus = serviceLevelHelper.editPCQDCQUDC(serviceLevelId1,PCQ,DCQ,UDC,customAssert);

				if(!editStatus){
					customAssert.assertTrue(false,"Error while editing PCQ DCQ UDC for SL" );
					customAssert.assertAll();
				}

				String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilenameesquery");
				String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");

				if (!uploadPerformanceDataFormat(serviceLevelId1, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
					customAssert.assertTrue(false, "Error while performance data file upload");
//					customAssert.assertAll();
				}

				if (!validatePerformanceDataFormatTab(serviceLevelId1, performanceDataFormatFileName, customAssert)) {

					customAssert.assertTrue(false, "Error while validating Performance Data Format Tab on Service Level");
//					customAssert.assertAll();
				}

				int childserviceLevelId1 = Integer.parseInt(childserviceLevelId1s.get(19));

				String rawDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilenameesquery");

				if (!uploadRawDataCSL(childserviceLevelId1, rawDataFileName, rawDataFileValidMsg, customAssert)) {

					customAssert.assertTrue(false, "Raw Data Uploaded unsuccessfully on CSL " + childserviceLevelId1);

				} else {

					if (!validateStructuredPerformanceDataCSL(childserviceLevelId1, rawDataFileName, "Done", "View Structured Data", auditLogUser, customAssert)) {
						customAssert.assertTrue(false, "Raw Data Upload validation unsuccessful for CSL ID " + childserviceLevelId1);
//						customAssert.assertAll();
					}
				}

				Boolean workFlowStatus = performComputationCSL(childserviceLevelId1, customAssert);
//				String workFlowActionToPerform = "ReComputePerformance";
//				WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
//				Boolean workFlowStatus = workflowActionsHelper.performWorkflowAction(cslEntityTypeId, childserviceLevelId1, workFlowActionToPerform);

				if (!workFlowStatus) {
//					customAssert.assertTrue(false, "Unable to perform " + workFlowActionToPerform + " computation on CSL Id " + childserviceLevelId1);
					customAssert.assertTrue(false, "Unable to perform  computation on CSL Id " + childserviceLevelId1);
				} else {
					show.hitShowVersion2(cslEntityTypeId, childserviceLevelId1);
					String showResponse = show.getShowJsonStr();
					String[] customFieldsToValidate = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "customfieldvalidation", "customfieldstocheck").split(",");
					String[] customFieldsValues = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "customfieldvalidation", "customfieldvalidation").split(",");

					if (customFieldsToValidate.length != customFieldsValues.length) {
						customAssert.assertTrue(false, "Length of custom fields and corresponding values is not equal , Please correct the configuration file");
//						customAssert.assertAll();

					}

					String actualFieldValue;
					for (int i = 0; i < customFieldsToValidate.length; i++) {

						actualFieldValue = ShowHelper.getValueOfField(customFieldsToValidate[i], showResponse);

						if(actualFieldValue == null){
							actualFieldValue = "null";
						}
						if (!customFieldsValues[i].equals(actualFieldValue)) {
							customAssert.assertTrue(false, "Expected and Actual value for Custom Field " + customFieldsToValidate[i] + " didn't match");
						}
					}
				}
			}
		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while validating custom fields on child service level " + e.getMessage());
		}
		customAssert.assertAll();
	}

	//
	//C10471 C10474 C10479
	@Test(groups = {"PCQ DCQ UDC Update2"},dependsOnMethods = "TestCSLCreation",enabled = false)
	public void TestUpdateInvalidCustomFields() {

		CustomAssert customAssert = new CustomAssert();
		Show show = new Show();
		String flowToTest = "sl automation flow";
		try {

			String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\", \"init_script\": \"state['map'] = ['met':0, 'notMet':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30,'Service_Level_Integer_Custom_Field_API_Automation123':10,'Service_Level_String_Custom_Field_API_Automation123':'String']; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;params.result.Actual_Numerator = 20;params.result.Actual_Denominator = 100;params.result.Supplier_Numerator = 10;params.result.Supplier_Denominator = 0;params.result.Final_Numerator = 10;params.result.Final_Denominator = 30;} return params.result\", \"combine_script\": \"return state;\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
			String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Met/Missed\": {\"script\": \"if(params['_source']['Time Taken (Secondss1)'] != ''){if(params['_source']['Time Taken (Seconds)'] < 110000){return 'Met'}else{return 'Missed'}}else{return 'Missed'}\"}}}";

			List<List<String>> currentTimeStamp = getCurrentTimeStamp();

			int serviceLevelId = serviceLevelId12;
			if (serviceLevelId != -1) {
//
				String UDC = "Incident ID";

				ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
				Boolean editStatus = serviceLevelHelper.editPCQDCQUDC(serviceLevelId,PCQ,DCQ,UDC,customAssert);

				if(!editStatus){
					customAssert.assertTrue(false,"Error while editing PCQ DCQ UDC for SL" );
					customAssert.assertAll();
				}

				String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilenameesquery");
				String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");

				if (!uploadPerformanceDataFormat(serviceLevelId, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
					customAssert.assertTrue(false, "Error while performance data file upload");
				}

				if (!validatePerformanceDataFormatTab(serviceLevelId, performanceDataFormatFileName, customAssert)) {

					customAssert.assertTrue(false, "Error while validating Performance Data Format Tab on Service Level");

				}

				int childserviceLevelId1 = Integer.parseInt(childserviceLevelId1s1.get(0));

				String rawDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilenameesquery");

				if (!uploadRawDataCSL(childserviceLevelId1, rawDataFileName, rawDataFileValidMsg, customAssert)) {

					customAssert.assertTrue(false, "Raw Data Uploaded unsuccessfully on CSL " + childserviceLevelId1);

				} else {

					if (!validateStructuredPerformanceDataCSL(childserviceLevelId1, rawDataFileName, "Done", "View Structured Data", auditLogUser, customAssert)) {
						customAssert.assertTrue(false, "Raw Data Upload validation unsuccessful for CSL ID " + childserviceLevelId1);
//						customAssert.assertAll();
					}
				}

				Boolean workFlowStatus = performComputationCSL(childserviceLevelId1, customAssert);
//				String workFlowActionToPerform = "ReComputePerformance";
//				WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
//				Boolean workFlowStatus = workflowActionsHelper.performWorkflowAction(cslEntityTypeId, childserviceLevelId1, workFlowActionToPerform);

				if (!workFlowStatus) {
//					customAssert.assertTrue(false, "Unable to perform " + workFlowActionToPerform + " computation on CSL Id " + childserviceLevelId1);
					customAssert.assertTrue(false, "Unable to perform  computation on CSL Id " + childserviceLevelId1);
				} else {
					show.hitShowVersion2(cslEntityTypeId, childserviceLevelId1);

					String showResponse = show.getShowJsonStr();

					String shortCodeId = ShowHelper.getValueOfField("short code id", showResponse);
					String title = ShowHelper.getValueOfField("description", showResponse);

					String subjectLine = "Error in performance computation - (#" + shortCodeId + ")-(#" + title + ")";

					List<List<String>> recordFromSystemEmailTable = getRecordFromSystemEmailTable(subjectLine, currentTimeStamp.get(0).get(0));

					List<String> expectedSentencesInBody = new ArrayList<>();
					expectedSentencesInBody.add("Metadata : Service_Level_String_Custom_Field_API_Automation123, Service_Level_Integer_Custom_Field_API_Automation123 not found.");

					if (recordFromSystemEmailTable.size() == 0) {
						customAssert.assertTrue(false, "No entry in system email table for subject Line " + subjectLine);
					}

					if (!validateAttachmentName(recordFromSystemEmailTable, null, customAssert)) {
						customAssert.assertTrue(false, "Attachment Name validated unsuccessfully in system emails table for subjectLine " + subjectLine);
					}

					if (!validateBodyOfEmail(recordFromSystemEmailTable, expectedSentencesInBody, customAssert)) {
						customAssert.assertTrue(false, "Body validated unsuccessfully in system emails table for subjectLine " + subjectLine);
					}
				}
			}
		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while validating custom fields on child service level " + e.getMessage());
		}
	}

	//C10472
	@Test(groups = {"PCQ DCQ UDC Update2"},dependsOnMethods = "TestCSLCreation",enabled = false)
	public void TestUpdateInActiveCustomFields() {

		CustomAssert customAssert = new CustomAssert();
		Show show = new Show();
		String flowToTest = "sl automation flow";
		try {

			String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Met/Missed\": {\"script\": \"if(params['_source']['Time Taken (Secondss1)'] != ''){if(params['_source']['Time Taken (Seconds)'] < 110000){return 'Met'}else{return 'Missed'}}else{return 'Missed'}\"}}}";
			String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if (doc['exception'].value=='F'){if(params['_source']['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(params['_source']['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\", \"init_script\": \"state['map'] = ['met':0, 'notMet':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30,'SL_Met':0, 'InActive_SL_String_Custom_Field_API_Automation':'check','InActive_SL_Integer_Custom_Field_API_Automation':10]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;} return params.result\", \"combine_script\": \"return state;\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";

			List<List<String>> currentTimeStamp = getCurrentTimeStamp();

			int serviceLevelId = serviceLevelId12;

			slToDelete.add(serviceLevelId);

			if (serviceLevelId1 != -1) {
//
				String UDC = "Incident ID";

				ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
				Boolean editStatus = serviceLevelHelper.editPCQDCQUDC(serviceLevelId1,PCQ,DCQ,UDC,customAssert);

				if(!editStatus){
					customAssert.assertTrue(false,"Error while editing PCQ DCQ UDC for SL" );
					customAssert.assertAll();
				}

				String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilenameesquery");
				String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");

				if (!uploadPerformanceDataFormat(serviceLevelId, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
					customAssert.assertTrue(false, "Error while performance data file upload");

				}

				if (!validatePerformanceDataFormatTab(serviceLevelId, performanceDataFormatFileName, customAssert)) {

					customAssert.assertTrue(false, "Error while validating Performance Data Format Tab on Service Level");

				}

				int childserviceLevelId1 = Integer.parseInt(childserviceLevelId1s1.get(1));

				String rawDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilenameesquery");

				if (!uploadRawDataCSL(childserviceLevelId1, rawDataFileName, rawDataFileValidMsg, customAssert)) {

					customAssert.assertTrue(false, "Raw Data Uploaded unsuccessfully on CSL " + childserviceLevelId1);
//					customAssert.assertAll();
				} else {

					if (!validateStructuredPerformanceDataCSL(childserviceLevelId1, rawDataFileName, "Done", "View Structured Data", auditLogUser, customAssert)) {
						customAssert.assertTrue(false, "Raw Data Upload validation unsuccessful for CSL ID " + childserviceLevelId1);
//						customAssert.assertAll();
					}
				}

				Boolean workFlowStatus = performComputationCSL(childserviceLevelId1, customAssert);
//				String workFlowActionToPerform = "ReComputePerformance";
//				WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
//				Boolean workFlowStatus = workflowActionsHelper.performWorkflowAction(cslEntityTypeId, childserviceLevelId1, workFlowActionToPerform);

				if (!workFlowStatus) {
//					customAssert.assertTrue(false, "Unable to perform " + workFlowActionToPerform + " computation on CSL Id " + childserviceLevelId1);
					customAssert.assertTrue(false, "Unable to perform  computation on CSL Id " + childserviceLevelId1);
				} else {

					Thread.sleep(5000);

					show.hitShowVersion2(cslEntityTypeId, childserviceLevelId1);

					String showResponse = show.getShowJsonStr();

					String shortCodeId = ShowHelper.getValueOfField("short code id", showResponse);
					String title = ShowHelper.getValueOfField("description", showResponse);

					String subjectLine = "Error in performance computation - (#" + shortCodeId + ")-(#" + title + ")";

					List<List<String>> recordFromSystemEmailTable = getRecordFromSystemEmailTable(subjectLine, currentTimeStamp.get(0).get(0));

					List<String> expectedSentencesInBody = new ArrayList<>();
					expectedSentencesInBody.add("Metadata : string not found.");

					if (recordFromSystemEmailTable.size() == 0) {
						customAssert.assertTrue(false, "No entry in system email table for subject Line " + subjectLine);
					}

					if (!validateAttachmentName(recordFromSystemEmailTable, null, customAssert)) {
						customAssert.assertTrue(false, "Attachment Name validated unsuccessfully in system emails table for subjectLine " + subjectLine);
					}

					if (!validateBodyOfEmail(recordFromSystemEmailTable, expectedSentencesInBody, customAssert)) {
						customAssert.assertTrue(false, "Body validated unsuccessfully in system emails table for subjectLine " + subjectLine);
					}
				}
			}
		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while validating custom fields on child service level " + e.getMessage());
		}
	}

	//	C10477
	@Test(groups = {"PCQ DCQ UDC Update2"},dependsOnMethods = "TestCSLCreation",enabled = false)
	public void TestESQueryMetaDataFoundSupportedValueIncorrect() {

		CustomAssert customAssert = new CustomAssert();
		Show show = new Show();
		String flowToTest = "sl automation flow";
		try {

			String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\", \"init_script\": \"state['map'] = ['met':0, 'notMet':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30,'Service_Level_Integer_Custom_Field_API_Automation':'String','Service_Level_String_Custom_Field_API_Automation':10]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;params.result.Actual_Numerator = 20;params.result.Actual_Denominator = 100;params.result.Supplier_Numerator = 10;params.result.Supplier_Denominator = 0;params.result.Final_Numerator = 10;params.result.Final_Denominator = 30;} return params.result\", \"combine_script\": \"return state;\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
			String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";

			List<List<String>> currentTimeStamp = getCurrentTimeStamp();

			int serviceLevelId1 = serviceLevelId12;
			if (serviceLevelId1 != -1) {
//
				String UDC = "Incident ID";

				ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
				Boolean editStatus = serviceLevelHelper.editPCQDCQUDC(serviceLevelId1,PCQ,DCQ,UDC,customAssert);

				if(!editStatus){
					customAssert.assertTrue(false,"Error while editing PCQ DCQ UDC for SL" );
					customAssert.assertAll();
				}

				String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilenameesquery");
				String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");

				if (!uploadPerformanceDataFormat(serviceLevelId1, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
					customAssert.assertTrue(false, "Error while performance data file upload");
//					customAssert.assertAll();
				}

				if (!validatePerformanceDataFormatTab(serviceLevelId1, performanceDataFormatFileName, customAssert)) {

					customAssert.assertTrue(false, "Error while validating Performance Data Format Tab on Service Level");
//					customAssert.assertAll();
				}

				int childserviceLevelId1 = Integer.parseInt(childserviceLevelId1s1.get(2));

				String rawDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilenameesquery");

				if (!uploadRawDataCSL(childserviceLevelId1, rawDataFileName, rawDataFileValidMsg, customAssert)) {

					customAssert.assertTrue(false, "Raw Data Uploaded unsuccessfully on CSL " + childserviceLevelId1);
//					customAssert.assertAll();
				} else {

					if (!validateStructuredPerformanceDataCSL(childserviceLevelId1, rawDataFileName, "Done", "View Structured Data", auditLogUser, customAssert)) {
						customAssert.assertTrue(false, "Raw Data Upload validation unsuccessful for CSL ID " + childserviceLevelId1);
//						customAssert.assertAll();
					}
				}

				String currentTimeStamp1 = getCurrentTimeStamp().get(0).get(0);

				Boolean workFlowStatus = performComputationCSL(childserviceLevelId1, customAssert);
//				String workFlowActionToPerform = "ReComputePerformance";
//				WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
//				Boolean workFlowStatus = workflowActionsHelper.performWorkflowAction(cslEntityTypeId, childserviceLevelId1, workFlowActionToPerform);

				if (!workFlowStatus) {
//					customAssert.assertTrue(false, "Unable to perform " + workFlowActionToPerform + " computation on CSL Id " + childserviceLevelId1);
					customAssert.assertTrue(false, "Unable to perform  computation on CSL Id " + childserviceLevelId1);
				} else {
					show.hitShowVersion2(cslEntityTypeId, childserviceLevelId1);

					String showResponse = show.getShowJsonStr();

					String shortCodeId = ShowHelper.getValueOfField("short code id", showResponse);
					String title = ShowHelper.getValueOfField("description", showResponse);

					String subjectLine = "Error in performance computation - (#" + shortCodeId + ")-(#" + title + ")";

					List<List<String>> recordFromSystemEmailTable = getRecordFromSystemEmailTable(subjectLine, currentTimeStamp1);

					List<String> expectedSentencesInBody = new ArrayList<>();
					expectedSentencesInBody.add("Error found while setting metadata value using elastic search query.");
//					expectedSentencesInBody.add("Metadata : integer cannot accept the value.");

					if (recordFromSystemEmailTable.size() == 0) {
						customAssert.assertTrue(false, "No entry in system email table for subject Line " + subjectLine);
					}

					if (!validateAttachmentName(recordFromSystemEmailTable, null, customAssert)) {
						customAssert.assertTrue(false, "Attachment Name validated unsuccessfully in system emails table for subjectLine " + subjectLine);
					}

					if (!validateBodyOfEmail(recordFromSystemEmailTable, expectedSentencesInBody, customAssert)) {
						customAssert.assertTrue(false, "Body validated unsuccessfully in system emails table for subjectLine " + subjectLine);
					}
				}
			}
		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while validating custom fields on child service level " + e.getMessage());
		}

	}

	//	C46220 C10750 C46229 C46230
	@Test(groups = {"PCQ DCQ UDC Update2"},dataProvider = "timeDifferenceFunctionCalculationDifferentFlows",dependsOnMethods = "TestCSLCreation",enabled = false)
	public void TestTimeDifferenceFunction(String dateFormat){

		CustomAssert customAssert = new CustomAssert();

		String flowToTest = "sl automation flow";

		//Adding performanceComputationCalculationQuery hardcoded as unable to read from cfg file properly
		String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\", \"init_script\": \"state['map'] = ['met':0, 'notMet':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;} return params.result\", \"combine_script\": \"return state;\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
		String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";

		try{
			int serviceLevelId1 = serviceLevelId12;

			if (serviceLevelId1 != -1) {

				String UDC = "Incident ID";

				ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();
				Boolean editStatus = serviceLevelHelper.editPCQDCQUDC(serviceLevelId1,PCQ,DCQ,UDC,customAssert);

				if(!editStatus){
					customAssert.assertTrue(false,"Error while editing PCQ DCQ UDC for SL" );
					customAssert.assertAll();
				}


				String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformat_filename_timedifference");
				String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");

				//Updating open time and resolved time date format
				XLSUtils.updateColumnValue(uploadFilePath,performanceDataFormatFileName,"Format Sheet",4,2,dateFormat);
				XLSUtils.updateColumnValue(uploadFilePath,performanceDataFormatFileName,"Format Sheet",5,2,dateFormat);

				if (!uploadPerformanceDataFormat(serviceLevelId1, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
					customAssert.assertTrue(false, "Error while performance data file upload");
//					customAssert.assertAll();
				}

				if (!validatePerformanceDataFormatTab(serviceLevelId1, performanceDataFormatFileName, customAssert)) {

					customAssert.assertTrue(false, "Error while validating Performance Data Format Tab");
				}

				String rawDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafile_timedifference");

				int childserviceLevelId1 = Integer.parseInt(childserviceLevelId1s1.get(3));

//				int childserviceLevelId1 = 34806;
				String rawDataFileNameToUpload = "RawDataTimeDifference_DifferentDateFormats.xlsx";
				FileUtils.copyFile(uploadFilePath,rawDataFileName,uploadFilePath,rawDataFileNameToUpload);

				if (dateFormat.equalsIgnoreCase("MM/dd/yyyy") || dateFormat.equalsIgnoreCase("dd/MM/yyyy")) {

					String startDate;
					String endDate;String sheetName = "Sheet1";
					for (int i = 1; i < 8; i++) {


						startDate = XLSUtils.getOneCellValue(uploadFilePath, rawDataFileNameToUpload, sheetName, i, 2);
						startDate = startDate.replaceAll("-", "/");
						XLSUtils.updateColumnValue(uploadFilePath, rawDataFileNameToUpload, sheetName, i, 2, startDate);


						endDate = XLSUtils.getOneCellValue(uploadFilePath, rawDataFileNameToUpload, sheetName, i, 2);
						endDate = endDate.replaceAll("-", "/");
						XLSUtils.updateColumnValue(uploadFilePath, rawDataFileNameToUpload, sheetName, i, 3, endDate);

					}
				}

				if (!uploadRawDataCSL(childserviceLevelId1, rawDataFileNameToUpload, rawDataFileValidMsg, customAssert)) {

					customAssert.assertTrue(false, "Raw Data Uploaded unsuccessfully on CSL " + childserviceLevelId1);
//					customAssert.assertAll();
				} else {

					if (!validateStructuredPerformanceDataCSL(childserviceLevelId1, rawDataFileNameToUpload, "Done", "View Structured Data", auditLogUser, customAssert)) {
						customAssert.assertTrue(false, "Raw Data Upload validation unsuccessful for CSL ID " + childserviceLevelId1);
//						customAssert.assertAll();
					}
				}

				TimeDifferenceFunction timeDifferenceFunction = new TimeDifferenceFunction();

				String startDateFormat;
				String endDateFormat;
				String startDate;
				String endDate;
				String timeZone;
				String weekTypeStr;
				String workingHours;
				String timeDifferenceExpected;
				ArrayList<String> timeDifferenceExpectedList = new ArrayList<>();
				try{
					startDateFormat = XLSUtils.getOneCellValue(uploadFilePath,performanceDataFormatFileName,"Format Sheet",4,2);
					endDateFormat = XLSUtils.getOneCellValue(uploadFilePath,performanceDataFormatFileName,"Format Sheet",5,2);


					for(int i =1 ;i<8;i++) {

						try {

							String sheetName = "Sheet1";
							startDate = XLSUtils.getOneCellValue(uploadFilePath, rawDataFileNameToUpload, sheetName, i, 2);


							endDate = XLSUtils.getOneCellValue(uploadFilePath, rawDataFileNameToUpload, sheetName, i, 3);


							weekTypeStr = XLSUtils.getOneCellValue(uploadFilePath, rawDataFileNameToUpload, sheetName, i, 5);
							workingHours = XLSUtils.getOneCellValue(uploadFilePath, rawDataFileNameToUpload, sheetName, i, 6);
							timeZone = XLSUtils.getOneCellValue(uploadFilePath, rawDataFileNameToUpload, sheetName, i, 7);

							timeDifferenceExpected = timeDifferenceFunction.timeDifferenceFunctionCalculation(startDate, endDate, startDateFormat, endDateFormat, timeZone, weekTypeStr, workingHours);

							timeDifferenceExpectedList.add(timeDifferenceExpected);

						} catch (Exception e) {
							customAssert.assertTrue(false, "Exception while getting expected data for time difference for rownumber " + i);
						}

					}

				}catch (Exception e){
					logger.error("Exception while getting expected data");
					customAssert.assertTrue(false,"Exception while getting expected data");
				}

				Boolean timeDifferenceValidationStatus =  validateTimeDifferenceValues(childserviceLevelId1,timeDifferenceExpectedList,customAssert);

				if(!timeDifferenceValidationStatus){
					customAssert.assertTrue(false,"Time difference Function validated unsuccessfully");
				}

			}

		}catch (Exception e){
			customAssert.assertTrue(false,"Exception while validating Time Difference Function " + e.getMessage());
		}

		customAssert.assertAll();
	}

	@Test(groups = {"sanity","PCQ DCQ UDC Update1"},dependsOnMethods = "TestCSLCreation",enabled = false)
	public void TestDownloadRawDataFormat(){

		CustomAssert customAssert = new CustomAssert();

		try{
//			int serviceLevelId1 = 6798;
			Map<String,String> urlStringMap = new HashMap<>();
//
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

	//SIR-1088 SL Automation - Reading metadata value via ES query
	@Test(groups = {"sanity","PCQ DCQ UDC Update2"},dependsOnMethods = "TestCSLCreation",enabled = false)
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

			if(!editStatus){
				customAssert.assertTrue(false,"Error while editing PCQ DCQ UDC for SL" );
				customAssert.assertAll();
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

			String completedBy = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "createdbyuser");
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

				Boolean auditLogVerificationStatus = verifyAuditLog(cslEntityTypeId,childserviceLevelId1,"Performance data uploaded",auditLogUser,customAssert);

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

	//    C10775
//    C13526
	@Test(dependsOnMethods = "TestCSLCreation", enabled = false)
	public void TestCSLCompStatus_DuplicateRecords() {

		CustomAssert customAssert = new CustomAssert();
		Show show = new Show();

		int cSLId = -1;
		try {
			cSLId = Integer.parseInt(childserviceLevelId1s.get(6));

			String rawDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilenduplicaterecords");

			String currentTimeStamp = getCurrentTimeStamp().get(0).get(0);

//			for (int i = 0; i < 2; i++) {
//				if (!uploadRawDataCSL(cSLId, rawDataFileName, rawDataFileValidMsg, customAssert)) {
//
//					customAssert.assertTrue(false, "Raw Data Uploaded unsuccessfully on CSL " + cSLId);
////					customAssert.assertAll();
//					return;
//				} else {
//
//					if (!validateStructuredPerformanceDataCSL(cSLId, rawDataFileName, "Done", "View Structured Data", auditLogUser, customAssert)) {
//						customAssert.assertTrue(false, "Raw Data Upload validation unsuccessful for CSL ID " + cSLId);
////						customAssert.assertAll();
//						return;
//					}
//				}
//				Thread.sleep(60000);
//			}
//			Thread.sleep(300000);

			if (!uploadRawDataCSL(cSLId, rawDataFileName, rawDataFileValidMsg, customAssert)) {

				customAssert.assertTrue(false, "Raw Data Uploaded unsuccessfully on CSL " + cSLId);
//					customAssert.assertAll();
				return;
			} else {

				if (!validateStructuredPerformanceDataCSL(cSLId, rawDataFileName, "Done", "View Structured Data", auditLogUser, customAssert)) {
					customAssert.assertTrue(false, "Raw Data Upload validation unsuccessful for CSL ID " + cSLId);
//						customAssert.assertAll();
					return;
				}
			}

			if (performComputationCSL(cSLId, customAssert)) {

				Thread.sleep(2000);
				show.hitShowVersion2(cslEntityTypeId, cSLId);
				String showPageResponse = show.getShowJsonStr();

				String computationStatus = ShowHelper.getValueOfField("computationstatus", showPageResponse);

				if (!computationStatus.equalsIgnoreCase("Duplicate Data Recorded")) {

					customAssert.assertTrue(false, "Computation Status Expected \"Duplicate Data Recorded\" Actual Computation Status " + computationStatus);

				} else {
					customAssert.assertTrue(true, "Computation Status validated successfully");
				}

				String shortCodeId = ShowHelper.getValueOfField("short code id", showPageResponse);
				String description = ShowHelper.getValueOfField("description", showPageResponse);

//          C13572
				String subjectLine = "Duplicates found - (#" + shortCodeId + ")-(#" + description + ")";
				List<String> expectedSentencesInBody = new ArrayList<>();
				expectedSentencesInBody.add("20 duplicate records have been identified in performance data.");


				if (!validateEmailSentSuccessfully(shortCodeId, subjectLine, null, expectedSentencesInBody, currentTimeStamp, customAssert)) {
					customAssert.assertTrue(false, "Email Sent Successfully validated unsuccessfully");
				}
			} else {
				customAssert.assertTrue(false, "Unable to perform computation on CSL " + cSLId);
			}
		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while validating computation status for CSL ID " + cSLId + e.getMessage());
		}

		customAssert.assertAll();
	}


	@Test(dataProvider = "", enabled = false)
	public void TestUnEditOfFieldsAtCSL(String fieldName, String id, String value,
										String errorField, String expectedMsg) {

		logger.info("Checking whether fields are uneditable for a particular CSL");

		CustomAssert customAssert = new CustomAssert();
		Edit edit = new Edit();
		int cSLId = -1;

		try {

			JSONObject editPayloadJson = editPayloadJson(cslEntity, cSLId);
			editPayloadJson.getJSONObject("body").getJSONObject("data")
					.getJSONObject(fieldName).getJSONObject("values").put("name", value);
			editPayloadJson.getJSONObject("body").getJSONObject("data")
					.getJSONObject(fieldName).getJSONObject("values").put("id", id);


			String editResponse = edit.hitEdit(cslEntity, editPayloadJson.toString());
			JSONObject editResponseJson;
			if (APIUtils.validJsonResponse(editResponse)) {

				editResponseJson = new JSONObject(editResponse);

				JSONObject fieldErrorsJson = editResponseJson.getJSONObject("body").getJSONObject("errors").getJSONObject("fieldErrors");

				String msg = fieldErrorsJson.getJSONObject(errorField).getString("message");

				if (!msg.equalsIgnoreCase(expectedMsg)) {
					customAssert.assertTrue(false, "Expected and Actual message are not same");
					customAssert.assertTrue(false, "Possible reason change in error msg or field has become editable");
				}
			} else {
				logger.error("Edit Response is not a valid Json");
			}

		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while validating uneditable fields on CSL ID " + cSLId + e.getMessage());
		}

		customAssert.assertAll();
	}

	//    C3907 C3908
	@Test(dataProvider = "slMetStatusValidationFlows", enabled = false)      //Completed
	public void TestTargetsValueFlowDownFromSLToCSL(String flowToTest) {

		CustomAssert customAssert = new CustomAssert();

		//Adding performanceComputationCalculationQuery hardcoded as unable to read from cfg file properly
		String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['Total Business Elapsed Time (Days)'].value < 20){if(doc['exception'].value== false){params._agg.map.met++}else{params._agg.map.notMet++}}else{if(doc['exception'].value== false){params._agg.map.notMet++}else{params._agg.map.met++}}}} if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['exception'].value== false){params._agg.map.credit += doc['Applicable Credit'].value}}} \", \"init_script\": \"params._agg['map'] =['met': 0.0, 'notMet': 0, 'credit':0]\", \"reduce_script\": \"params.return_map = ['Final_Numerator':0.0, 'Final_Denominator':0, 'Final_Performance':0 ,'Actual_Numerator':0.0, 'Actual_Denominator':0, 'Actual_Performance':0 ,'SL_Met':0 ,     'Calculated_Credit_Amount':0.0]; for (a in params._aggs){params.return_map.Final_Numerator += (float)(a.map.met); params.return_map.Final_Denominator += (float)(a.map.met + a.map.notMet) ; params.return_map.Calculated_Credit_Amount += (float)(a.map.credit);}  if(params.return_map.Final_Denominator > 0){params.return_map.Final_Performance = Math.round(((float)(params.return_map.Final_Numerator*100)/params.return_map.Final_Denominator)*100.0)/100.0}else{params.return_map.Final_Performance ='';params.return_map.SL_Met =5} params.return_map.Final_Numerator = Math.round(params.return_map.Final_Numerator *100.0)/100.0 ;params.return_map.Actual_Numerator = params.return_map.Final_Numerator; params.return_map.Actual_Denominator = params.return_map.Final_Denominator; params.return_map.Actual_Performance = params.return_map.Final_Performance; if(params.return_map.Final_Denominator > 0){if(params.return_map.Final_Performance < 90){params.return_map.Calculated_Credit_Amount = params.return_map.Calculated_Credit_Amount} else{params.return_map.Calculated_Credit_Amount = ''}} else{params.return_map.Calculated_Credit_Amount = ''}  return params.return_map\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}, {\"match\": {\"useInComputation\": true}}]}}}";
		String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
		Show show = new Show();

		int serviceLevelId1TargetValues;

		try {
			serviceLevelId1TargetValues = getActiveserviceLevelId1(flowToTest, PCQ, DCQ, auditLogUser, customAssert);
			Thread.sleep(5000);
			if (serviceLevelId1TargetValues != -1) {

				ArrayList<String> childserviceLevelId1 = checkIfCSLCreatedOnServiceLevel(serviceLevelId1TargetValues, customAssert);

				if (childserviceLevelId1.size() == 0) {
					customAssert.assertTrue(false, "Child Service Level not created ");
					customAssert.assertAll();
				}
				addCSLToDelete(childserviceLevelId1);

				int numberOfChildServiceLevel = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "numberofchildservicelevel"));

				if (childserviceLevelId1.size() != numberOfChildServiceLevel) {

					customAssert.assertTrue(false, "For Service Level Id " + serviceLevelId1TargetValues + " Number of Child Service Level Expected are " + numberOfChildServiceLevel + " Actual " + childserviceLevelId1.size());
					customAssert.assertAll();
				}
				show.hitShowVersion2(slEntityTypeId, serviceLevelId1TargetValues);
				String showResponseSL = show.getShowJsonStr();

				int csl = Integer.parseInt(childserviceLevelId1.get(0));
				show.hitShowVersion2(cslEntityTypeId, csl);

				String showResponseCSL = show.getShowJsonStr();

				//C3908
				if (!validateTargetValuesFlowDownCSL(flowToTest, csl, showResponseSL, showResponseCSL, customAssert)) {
					customAssert.assertTrue(false, "Target values flow down validated unsuccessfully");
				}

			}

		} catch (Exception e) {
			logger.error("Exception while validating target fields flow down");
			customAssert.assertTrue(false, "Exception while validating target fields flow down" + e.getMessage());
		}

		customAssert.assertAll();

	}

	//    C13292
	@Test(dataProvider = "slMetGraphValidationFlows", enabled = false)
	public void TestGraph(String flowToTest) {

		logger.info("Validating Graph functionality on Service level tab Child Service Levels");

		CustomAssert customAssert = new CustomAssert();
		Show show = new Show();
		int serviceLevelId1ForGraphValidation;
		int cslID;

		//Adding performanceComputationCalculationQuery hardcoded as unable to read from cfg file properly
		//String PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['Total Business Elapsed Time (Days)'].value < 20){if(doc['exception'].value== false){state.map.met++}else{state.map.notMet++}}else{if(doc['exception'].value== false){state.map.notMet++}else{state.map.met++}}}} if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['exception'].value== false){state.map.credit += doc['Applicable Credit'].value}}} \",\"init_script\":\"state['map'] =['met': 0.0, 'notMet': 0, 'credit':0]\",\"reduce_script\":\"params.return_map = ['Final_Numerator':0.0, 'Final_Denominator':0, 'Final_Performance':0 ,'Actual_Numerator':0.0, 'Actual_Denominator':0, 'Actual_Performance':0 ,'SL_Met':0 ,     'Calculated_Credit_Amount':0.0]; for (a in states){params.return_map.Final_Numerator += (float)(a.map.met); params.return_map.Final_Denominator += (float)(a.map.met + a.map.notMet) ; params.return_map.Calculated_Credit_Amount += (float)(a.map.credit);}  if(params.return_map.Final_Denominator > 0){params.return_map.Final_Performance = Math.round(((float)(params.return_map.Final_Numerator*100)/params.return_map.Final_Denominator)*100.0)/100.0}else{params.return_map.Final_Performance ='';params.return_map.SL_Met =5} params.return_map.Final_Numerator = Math.round(params.return_map.Final_Numerator *100.0)/100.0 ;params.return_map.Actual_Numerator = params.return_map.Final_Numerator; params.return_map.Actual_Denominator = params.return_map.Final_Denominator; params.return_map.Actual_Performance = params.return_map.Final_Performance; if(params.return_map.Final_Denominator > 0){if(params.return_map.Final_Performance < 90){params.return_map.Calculated_Credit_Amount = params.return_map.Calculated_Credit_Amount} else{params.return_map.Calculated_Credit_Amount = ''}} else{params.return_map.Calculated_Credit_Amount = ''}  return params.return_map\",\"combine_script\":\"return state;\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}},{\"match\":{\"useInComputation\":true}}]}}}";
		String PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\",\"init_script\":\"state['map'] = ['met':0, 'notMet':0]\",\"reduce_script\":\"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;} return params.result\",\"combine_script\":\"return state;\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}}]}}}";
		String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";

		try {

			serviceLevelId1ForGraphValidation = getserviceLevelId1(flowToTest, PCQ, DCQ, customAssert);
//			serviceLevelId1ForGraphValidation = 7916;
			slToDelete.add(serviceLevelId1ForGraphValidation);

			if (serviceLevelId1ForGraphValidation != -1) {

				List<String> workFlowSteps = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "slactiveworkflowsteps").split("->"));
//				Performing workflow Actions till Active

				if (!performWorkFlowActions(slEntityTypeId, serviceLevelId1ForGraphValidation, workFlowSteps, auditLogUser, customAssert)) {
					customAssert.assertTrue(false, "Error while performing workflow actions on SL ID " + serviceLevelId1);
					customAssert.assertAll();
				}

				String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilename");

				String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");

				if (!uploadPerformanceDataFormat(serviceLevelId1ForGraphValidation, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
					customAssert.assertTrue(false, "Error while performance data file upload");
				}

				if (!validatePerformanceDataFormatTab(serviceLevelId1ForGraphValidation, performanceDataFormatFileName, customAssert)) {

					customAssert.assertTrue(false, "Error while validating Performance Data Format Tab");
				}

				ArrayList<String> childserviceLevelId1s = checkIfCSLCreatedOnServiceLevel(serviceLevelId1ForGraphValidation, customAssert);

				if (childserviceLevelId1s.size() == 0) {
					customAssert.assertTrue(false, "Child Service Level not created ");
					customAssert.assertAll();
				}

				addCSLToDelete(childserviceLevelId1s);

				int numberOfGraphToValidate = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "graphvalidationscenario", flowToTest));

				String finalNumerator = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "graphvalidationscenario", "finalnumerator");
				String finalDenominator = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "graphvalidationscenario", "finaldenominator");
				String finalPerformance = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "graphvalidationscenario", "finalperformance");
				String expected = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "graphvalidationscenario", flowToTest + " expected");
				String minimum = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "graphvalidationscenario", flowToTest + " minimum");
				String sigMinimum = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "graphvalidationscenario", flowToTest + " sigminimum");

				int numberOfCSLToUpdate = 2;

				for (int i = 0; i < numberOfCSLToUpdate; i++) {

					cslID = Integer.parseInt(childserviceLevelId1s.get(i));

					if (!updateFinalPerformanceValues(cslID, finalNumerator, finalDenominator, finalPerformance, customAssert)) {

						customAssert.assertTrue(false, "Final Performance Values updated unsuccessfully for CSL ID " + cslID);

					}
//					WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();

//					if (!workflowActionsHelper.performWorkflowAction(cslEntityTypeId, cslID, "ReComputePerformance")) {
//						customAssert.assertTrue(false, "Unable to perform ReComputePerformance for the CSL ID " + cslID);
//					}

//					Boolean workFlowStatus = performComputationCSL(cslID, customAssert);
//				String workFlowActionToPerform = "ReComputePerformance";
//				WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
//				Boolean workFlowStatus = workflowActionsHelper.performWorkflowAction(cslEntityTypeId, childserviceLevelId1, workFlowActionToPerform);

//					if (!workFlowStatus) {
////					customAssert.assertTrue(false, "Unable to perform " + workFlowActionToPerform + " computation on CSL Id " + childserviceLevelId1);
//						customAssert.assertTrue(false, "Unable to perform  computation on CSL Id " + cslID);
//					}
//					Thread.sleep(5000);
				}
				//Thread.sleep(300000);

				int listId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "graphvalidationscenario", "listid"));
				int calendarType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "graphvalidationscenario", "calendartype"));

				if (!checkIfGraphDataGenerated(listId, serviceLevelId1ForGraphValidation, calendarType, customAssert)) {
					customAssert.assertTrue(false, "Unable to generate Graph in specific period for SL ID " + serviceLevelId1ForGraphValidation);
					customAssert.assertAll();
				}

				ListRendererListData listRendererListData = new ListRendererListData();
				listRendererListData.hitListRendererSlaSpecificGraph(listId, serviceLevelId1ForGraphValidation, calendarType);
				String listDataResponse = listRendererListData.getListDataJsonStr();

				if (APIUtils.validJsonResponse(listDataResponse)) {

					JSONArray listDataResponseJson = new JSONArray(listDataResponse);
					show.hitShowVersion2(slEntityTypeId, serviceLevelId1ForGraphValidation);
					String showResponse = show.getShowJsonStr();

					String expectedChartName = ShowHelper.getValueOfField("name", showResponse);
					String actualChartName = listDataResponseJson.getJSONObject(0).getString("chartName");

					if (!expectedChartName.equalsIgnoreCase(actualChartName)) {
						customAssert.assertTrue(false, "Expected and Actual Chart Names are not equal Expected : " +
								expectedChartName + "Actual : " + actualChartName);
					}

					int chartIdActual = Integer.parseInt(listDataResponseJson.getJSONObject(0).get("chartId").toString());

					if (chartIdActual != serviceLevelId1ForGraphValidation) {
						customAssert.assertTrue(false, "Expected and Actual Chart Ids are not equal Expected : " +
								serviceLevelId1ForGraphValidation + "Actual : " + chartIdActual);
					}

					JSONArray dataSetArray = listDataResponseJson.getJSONObject(0).getJSONArray("dataset");

					String serviceDate;

					String[] serviceDateSplitArray;
					String serviceDateOnChart;
					ArrayList<String> serviceDateList = new ArrayList<>();
					JSONArray dataArray;

					String actualToolText;
					String expectedToolText = "";
					String expectedValue = "";
					String actualValue = "";
					String actualLabel = "";

					ArrayList<String> expectedToolTextList = new ArrayList();

					expectedToolTextList.add("Final Performance");
					expectedToolTextList.add("Expected");

					if (flowToTest.contains("min")) {

						//expected = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "graphvalidationscenario", "expectedmin");
						//minimum = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "graphvalidationscenario", "minimummin");
						expectedToolTextList.add("Minimum ");
						expectedToolTextList.add("Significantly Minimum");

					} else if (flowToTest.contains("max")) {

						//expected = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "graphvalidationscenario", "expected");
						//minimum = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "graphvalidationscenario", "minimum");
						expectedToolTextList.add("Maximum ");
						expectedToolTextList.add("Significantly Maximum");
					}

					for (int j = 0; j < numberOfCSLToUpdate; j++) {
						show.hitShowVersion2(cslEntityTypeId, Integer.parseInt(childserviceLevelId1s.get(j)));
						showResponse = show.getShowJsonStr();


						serviceDate = ShowHelper.getValueOfField("duedate", showResponse);

						serviceDateSplitArray = serviceDate.split("-");

						serviceDateOnChart = serviceDateSplitArray[0] + "-" + serviceDateSplitArray[2].substring(2, 4);
						serviceDateList.add(serviceDateOnChart);

						for (int i = 0; i < numberOfGraphToValidate; i++) {

							dataArray = dataSetArray.getJSONObject(i).getJSONArray("data");
							if (i == 0) {
								expectedToolText = expectedToolTextList.get(i) + ": " + finalPerformance + "," + " Service Date :" + serviceDateList.get(j);
								expectedValue = finalPerformance;
							} else if (i == 1) {
								expectedToolText = expectedToolTextList.get(i) + ": " + expected + "," + " Service Date :" + serviceDateList.get(j);
								expectedValue = expected;
							} else if (i == 2) {
								expectedToolText = expectedToolTextList.get(i) + ": " + minimum + "," + " Service Date :" + serviceDateList.get(j);
								expectedValue = minimum;
							} else if (i == 3) {
								expectedToolText = expectedToolTextList.get(i) + " : " + sigMinimum + "," + " Service Date :" + serviceDateList.get(j);
								expectedValue = sigMinimum;
							}

							//for(int j=0;j<numberOfCSLToUpdate;j++){

							actualToolText = dataArray.getJSONObject(j).get("toolText").toString();
							actualValue = dataArray.getJSONObject(j).get("value").toString();
							actualLabel = dataArray.getJSONObject(j).get("label").toString();
							if (!expectedToolText.equalsIgnoreCase(actualToolText)) {
								customAssert.assertTrue(false, "Expected and Actual value for tool text mismatched");
							}

							if (!expectedValue.equalsIgnoreCase(actualValue)) {
								customAssert.assertTrue(false, "Expected value : " + expectedValue + " and Actual value : " + actualValue + " for " + expectedToolText + " + mismatched");
							}

							if (!serviceDateList.get(j).equalsIgnoreCase(actualLabel)) {
								customAssert.assertTrue(false, "Expected Label : " + serviceDateList.get(j) + " and Actual Label : " + actualLabel + "for " + expectedToolText + " + mismatched");
							}
						}
					}
				} else {
					customAssert.assertTrue(false, "List Renderer sla specific graph response is not a valid json");
				}
			}else {
				customAssert.assertTrue(false,"Unable to create service level id for the flow " + flowToTest);
			}

		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while validating Graph Functionality on Service level tab Child Service Levels " + e.getMessage());
		}

		customAssert.assertAll();
	}

	//C10749 C10579
	@Test(enabled = false)
	public void TestUploadMetaDataDiffCombinations() {

		CustomAssert customAssert = new CustomAssert();
		try {
			String flowToTest = "sl automation flow";

			//Adding performanceComputationCalculationQuery hardcoded as unable to read from cfg file properly
			String PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['Total Business Elapsed Time (Days)'].value < 20){if(doc['exception'].value== false){state.map.met++}else{state.map.notMet++}}else{if(doc['exception'].value== false){state.map.notMet++}else{state.map.met++}}}} if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['exception'].value== false){state.map.credit += doc['Applicable Credit'].value}}} \",\"init_script\":\"state['map'] =['met': 0.0, 'notMet': 0, 'credit':0]\",\"reduce_script\":\"params.return_map = ['Final_Numerator':0.0, 'Final_Denominator':0, 'Final_Performance':0 ,'Actual_Numerator':0.0, 'Actual_Denominator':0, 'Actual_Performance':0 ,'SL_Met':0 ,     'Calculated_Credit_Amount':0.0]; for (a in states){params.return_map.Final_Numerator += (float)(a.map.met); params.return_map.Final_Denominator += (float)(a.map.met + a.map.notMet) ; params.return_map.Calculated_Credit_Amount += (float)(a.map.credit);}  if(params.return_map.Final_Denominator > 0){params.return_map.Final_Performance = Math.round(((float)(params.return_map.Final_Numerator*100)/params.return_map.Final_Denominator)*100.0)/100.0}else{params.return_map.Final_Performance ='';params.return_map.SL_Met =5} params.return_map.Final_Numerator = Math.round(params.return_map.Final_Numerator *100.0)/100.0 ;params.return_map.Actual_Numerator = params.return_map.Final_Numerator; params.return_map.Actual_Denominator = params.return_map.Final_Denominator; params.return_map.Actual_Performance = params.return_map.Final_Performance; if(params.return_map.Final_Denominator > 0){if(params.return_map.Final_Performance < 90){params.return_map.Calculated_Credit_Amount = params.return_map.Calculated_Credit_Amount} else{params.return_map.Calculated_Credit_Amount = ''}} else{params.return_map.Calculated_Credit_Amount = ''}  return params.return_map\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}},{\"match\":{\"useInComputation\":true}}]}}}";
			String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";

			int serviceLevelId1 = getActiveserviceLevelId1(flowToTest, PCQ, DCQ, auditLogUser, customAssert);
			slToDelete.add(serviceLevelId1);

			Thread.sleep(5000);

			String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "sltemplatefilediffcomb");
			String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");
			if (!uploadPerformanceDataFormat(serviceLevelId1, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
				customAssert.assertTrue(false, "Error while performance data file upload");
//				customAssert.assertAll();
			}

			if (!validatePerformanceDataFormatTab(serviceLevelId1, performanceDataFormatFileName, customAssert)) {

				customAssert.assertTrue(false, "Error while validating Performance Data Format Tab");
			}

		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while validating meta upload with different combinations");
		}
		customAssert.assertAll();
	}

	//C7604
	@Test(enabled = false)      //Completed
	public void TestRAGApplicableFalse() {

		CustomAssert customAssert = new CustomAssert();
		String flowToTest = "sl rag applicable no";
		Show show = new Show();

		try {

			//Adding performanceComputationCalculationQuery hardcoded as unable to read from cfg file properly
			String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if (doc['exception'].value=='F'){if(params['_source']['Time Taken (Seconds)'] != ''){if(doc['Time Taken (Seconds)'].value < 28800){params._agg.map.met++; }else{params._agg.map.notMet++}}}else{if(params['_source']['Time Taken (Seconds)'] != ''){if(doc['Time Taken (Seconds)'].value < 28800){params._agg.map.notMet++; }else{params._agg.map.met++}}}\", \"init_script\": \"params._agg['map'] = ['met':0, 'notMet':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;} return params.result\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
			String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Met/Missed\": {\"script\": \"if(params['_source']['Time Taken (Secondss1)'] != ''){if(params['_source']['Time Taken (Seconds)'] < 110000){return 'Met'}else{return 'Missed'}}else{return 'Missed'}\"}}}";

			int serviceLevelId1 = getActiveserviceLevelId1(flowToTest,PCQ,DCQ,auditLogUser,customAssert);
			List<List<String>> currentTimeStamp = getCurrentTimeStamp();
			String currentTimeStamp1 = currentTimeStamp.get(0).get(0);
//			int serviceLevelId1 = getserviceLevelId1(flowToTest, PCQ, DCQ, customAssert);
			slToDelete.add(serviceLevelId1);

			String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilename");
			String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");

			if (!uploadPerformanceDataFormat(serviceLevelId1, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
				customAssert.assertTrue(false, "Error while performance data file upload");
//				customAssert.assertAll();
			}

			//Setting Through workflow step set CreateChild Flag as true To be done
			Boolean entityToEdit = false;
			if(entityToEdit) {
				//Edit the value of RAG and SET as False.
				Edit edit = new Edit();
				String editResponse = edit.hitEdit(slEntity, serviceLevelId1);

				JSONObject editResponseJson = new JSONObject(editResponse);
				editResponseJson.remove("header");
				editResponseJson.remove("session");
				editResponseJson.remove("actions");
				editResponseJson.remove("createLinks");
				editResponseJson.getJSONObject("body").remove("layoutInfo");
				editResponseJson.getJSONObject("body").remove("globalData");
				editResponseJson.getJSONObject("body").remove("errors");
				editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("ragApplicable").put("options", "null");
				editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("ragApplicable").getJSONObject("values").put("name", "No");
				editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("ragApplicable").getJSONObject("values").put("id", "1002");
				editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("expected").remove("values");
				editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("sigMinMax").remove("values");
				editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("minimum").remove("values");

				editResponse = edit.hitEdit(slEntity, editResponseJson.toString());

				if (!editResponse.contains("success")) {
					customAssert.assertTrue(false, "Unable to edit RAG flag as No on SL ID " + serviceLevelId1);
				}

			}

			if (serviceLevelId1 != -1) {
				List<String> workFlowSteps;
				workFlowSteps = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "slactiveworkflowsteps").split("->"));
				if (!performWorkFlowActions(slEntityTypeId, serviceLevelId1, workFlowSteps, auditLogUser, customAssert)) {
					customAssert.assertTrue(false, "Error while performing workflow actions on SL ID " + serviceLevelId1);
//					customAssert.assertAll();

				}
			} else {
				customAssert.assertTrue(false, "Service Level id = -1");
//				customAssert.assertAll();
			}

			ArrayList<String> cslIds = checkIfCSLCreatedOnServiceLevel(serviceLevelId1, customAssert);

			if (cslIds.size() == 0) {
				customAssert.assertTrue(false, "Child Service Level not created ");
//				customAssert.assertAll();
			}

			addCSLToDelete(cslIds);

			int cslId = Integer.parseInt(cslIds.get(0));

			String rawDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilename");
			String completedBy = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "createdbyuser");
			if (!uploadRawDataCSL(cslId, rawDataFileName, rawDataFileValidMsg, customAssert)) {

				customAssert.assertTrue(false, "Raw Data Uploaded unsuccessfully on CSL " + cslId);
//				customAssert.assertAll();

			} else {
				if (!validateStructuredPerformanceDataCSL(cslId, rawDataFileName, "Done", "View Structured Data", completedBy, customAssert)) {
					customAssert.assertTrue(false, "Raw Data File Upload validation unsuccessful");
					customAssert.assertAll();

				}
			}

			show.hitShowVersion2(cslEntityTypeId, cslId);
			String showResponse = show.getShowJsonStr();
			String shortCodeId = ShowHelper.getValueOfField("short code id", showResponse);
			String description = ShowHelper.getValueOfField("description", showResponse);
			JSONObject showResponseJson = new JSONObject(showResponse);
			JSONObject slMetStatusJson = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("slMet");

//            C7607
			if (slMetStatusJson.has("values")) {
				customAssert.assertTrue(false, "Show Response Json contains slMet with Values Json while it should not as RAG applicable is false");
			}

			//Further validation pending
			Boolean workFlowStatus = performComputationCSL(cslId, customAssert);
//				String workFlowActionToPerform = "ReComputePerformance";
//				WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
//				Boolean workFlowStatus = workflowActionsHelper.performWorkflowAction(cslEntityTypeId, childserviceLevelId1, workFlowActionToPerform);

			if (!workFlowStatus) {
//					customAssert.assertTrue(false, "Unable to perform " + workFlowActionToPerform + " computation on CSL Id " + childserviceLevelId1);
				customAssert.assertTrue(false, "Unable to perform  computation on CSL Id " + cslId);
			}

//            Email Validation part
			String expectedAttachment = null;

			String subjectLineExpected = "Error in performance computation - (#" + shortCodeId + ")-(#" + description + ")";
			List<List<String>> recordFromSystemEmailTable = getRecordFromSystemEmailTable(subjectLineExpected, currentTimeStamp1);

			List<String> expectedSentencesInBody = new ArrayList<>();
			expectedSentencesInBody.add("Rag Applicable? value is NO, SL Met field is not relevant.");

			if (recordFromSystemEmailTable.size() == 0) {
				customAssert.assertTrue(false, "No Record Got From System Email Table after performing computation on CSL ID " + shortCodeId + " with RAG flag as false ");
			} else {

				if (!validateAttachmentName(recordFromSystemEmailTable, expectedAttachment, customAssert)) {
					customAssert.assertTrue(false, "Attachment name validated unsuccessfully for entity " + shortCodeId);
				}

//				if (!validateSentSuccessfullyFlag(recordFromSystemEmailTable, customAssert)) {
//					customAssert.assertTrue(false, "Sent successfully flag validated unsuccessfully for entity " + shortCodeId);
//				}

				if (!validateBodyOfEmail(recordFromSystemEmailTable, expectedSentencesInBody, customAssert)) {
					customAssert.assertTrue(false, "Body validated unsuccessfully in system emails table for subjectLine " + subjectLineExpected);
				}

			}

		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while validating RAG applicable No Scenario " + e.getMessage());
		}

		customAssert.assertAll();
	}

	@Test(enabled = true)      //Completed
	public void TestRAGApplicableTrueToFalseEditAfterCslCreated() {

		CustomAssert customAssert = new CustomAssert();
		String flowToTest = "sl rag applicable no";
		Show show = new Show();

		try {

			//Adding performanceComputationCalculationQuery hardcoded as unable to read from cfg file properly
			String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if (doc['exception'].value=='F'){if(params['_source']['Time Taken (Seconds)'] != ''){if(doc['Time Taken (Seconds)'].value < 28800){params._agg.map.met++; }else{params._agg.map.notMet++}}}else{if(params['_source']['Time Taken (Seconds)'] != ''){if(doc['Time Taken (Seconds)'].value < 28800){params._agg.map.notMet++; }else{params._agg.map.met++}}}\", \"init_script\": \"params._agg['map'] = ['met':0, 'notMet':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;} return params.result\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
			String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Met/Missed\": {\"script\": \"if(params['_source']['Time Taken (Secondss1)'] != ''){if(params['_source']['Time Taken (Seconds)'] < 110000){return 'Met'}else{return 'Missed'}}else{return 'Missed'}\"}}}";

//			int serviceLevelId1 = getActiveserviceLevelId1(flowToTest,PCQ,DCQ,auditLogUser,customAssert);
			int serviceLevelId1 = 20877;
			slToDelete.add(serviceLevelId1);

			ArrayList<String> cslIds = checkIfCSLCreatedOnServiceLevel(serviceLevelId1, customAssert);

			if (cslIds.size() == 0) {
				customAssert.assertTrue(false, "Child Service Level not created ");
				customAssert.assertAll();
			}
			addCSLToDelete(cslIds);

			//Setting Through workflow step set CreateChild Flag as true To be done
			Boolean entityToEdit = false;
			if(serviceLevelId1 !=1){
				entityToEdit = true;
			}
			if(entityToEdit) {
				//Edit the value of RAG and SET as False.
				Edit edit = new Edit();
				String editResponse = edit.hitEdit(slEntity, serviceLevelId1);
				String expectedMessage = "";
				String actualMessage;

				JSONObject editResponseJson = new JSONObject(editResponse);
				editResponseJson.remove("header");
				editResponseJson.remove("session");
				editResponseJson.remove("actions");
				editResponseJson.remove("createLinks");
				editResponseJson.getJSONObject("body").remove("layoutInfo");
				editResponseJson.getJSONObject("body").remove("globalData");
				editResponseJson.getJSONObject("body").remove("errors");
				editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("ragApplicable").put("options", "null");
				editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("ragApplicable").getJSONObject("values").put("name", "No");
				editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("ragApplicable").getJSONObject("values").put("id", "1002");
				editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("expected").remove("values");
				editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("sigMinMax").remove("values");
				editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("minimum").remove("values");

				editResponse = edit.hitEdit(slEntity, editResponseJson.toString());
				JSONObject errorEditResponse = new JSONObject(editResponse);

				if (!editResponse.contains("success")) {
					actualMessage = errorEditResponse.getJSONObject("body").getJSONObject("errors").getJSONObject("fieldErrors").getJSONObject("ragApplicable").get("message").toString();
					if(expectedMessage.equalsIgnoreCase(actualMessage)){
						logger.info("Unable to edit Rag Applicable as Child SLs are already created for SL ID"+serviceLevelId1);
					}
				}
				else{
					customAssert.assertTrue(false, "Able to edit RAG flag as No on SL ID " + serviceLevelId1+"though it should not be edited as CSL are already created");
				}

			} else {
				customAssert.assertTrue(false, "Service Level id = -1");
				customAssert.assertAll();
			}

			int cslId = Integer.parseInt(cslIds.get(6));
			String expectedMaximum = "90";
			String expectedValue = "80";
			String expectedThreshold = "Maximum - 2 level";

			show.hitShowVersion2(cslEntityTypeId, cslId);
			String showResponse = show.getShowJsonStr();
			JSONObject showResponseJson = new JSONObject(showResponse);
			String maximum = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("minMax").get("values").toString();
			String expected = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("expected").get("values").toString();
			String threshold = showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("threshold").getJSONObject("values").get("name").toString();

			if (expectedMaximum.equalsIgnoreCase(maximum) && expectedValue.equalsIgnoreCase(expected) && expectedThreshold.equalsIgnoreCase(threshold)){
				customAssert.assertTrue(true,"show page of CSL Rag applicable fields validated successfully");
				customAssert.assertAll();
			}else{
				customAssert.assertTrue(false,"show page of CSL Rag applicable fields validated unsuccessfully");
				customAssert.assertAll();
			}
		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while validating RAG applicable No Scenario " + e.getMessage());
		}

		customAssert.assertAll();
	}

	@Test(enabled = false)      //Completed	 Bug 24 Sept in the application
	public void TestRAGApplicableTrueToFalse() {

		CustomAssert customAssert = new CustomAssert();
		String flowToTest = "sl automation flow";
		try {

			//Adding performanceComputationCalculationQuery hardcoded as unable to read from cfg file properly
			String PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['Total Business Elapsed Time (Days)'].value < 20){if(doc['exception'].value== false){state.map.met++}else{state.map.notMet++}}else{if(doc['exception'].value== false){state.map.notMet++}else{state.map.met++}}}} if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['exception'].value== false){state.map.credit += doc['Applicable Credit'].value}}} \",\"init_script\":\"state['map'] =['met': 0.0, 'notMet': 0, 'credit':0]\",\"reduce_script\":\"params.return_map = ['Final_Numerator':0.0, 'Final_Denominator':0, 'Final_Performance':0 ,'Actual_Numerator':0.0, 'Actual_Denominator':0, 'Actual_Performance':0 ,'SL_Met':0 ,     'Calculated_Credit_Amount':0.0]; for (a in states){params.return_map.Final_Numerator += (float)(a.map.met); params.return_map.Final_Denominator += (float)(a.map.met + a.map.notMet) ; params.return_map.Calculated_Credit_Amount += (float)(a.map.credit);}  if(params.return_map.Final_Denominator > 0){params.return_map.Final_Performance = Math.round(((float)(params.return_map.Final_Numerator*100)/params.return_map.Final_Denominator)*100.0)/100.0}else{params.return_map.Final_Performance ='';params.return_map.SL_Met =5} params.return_map.Final_Numerator = Math.round(params.return_map.Final_Numerator *100.0)/100.0 ;params.return_map.Actual_Numerator = params.return_map.Final_Numerator; params.return_map.Actual_Denominator = params.return_map.Final_Denominator; params.return_map.Actual_Performance = params.return_map.Final_Performance; if(params.return_map.Final_Denominator > 0){if(params.return_map.Final_Performance < 90){params.return_map.Calculated_Credit_Amount = params.return_map.Calculated_Credit_Amount} else{params.return_map.Calculated_Credit_Amount = ''}} else{params.return_map.Calculated_Credit_Amount = ''}  return params.return_map\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}},{\"match\":{\"useInComputation\":true}}]}}}";
			String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Met/Missed\": {\"script\": \"if(params['_source']['Time Taken (Secondss1)'] != ''){if(params['_source']['Time Taken (Seconds)'] < 110000){return 'Met'}else{return 'Missed'}}else{return 'Missed'}\"}}}";

			int serviceLevelId1 = getserviceLevelId1(flowToTest, PCQ, DCQ, customAssert);
			slToDelete.add(serviceLevelId1);

			Edit edit = new Edit();
			String editResponse = edit.hitEdit(slEntity, serviceLevelId1);

			JSONObject editResponseJson = new JSONObject(editResponse);
			editResponseJson.remove("header");
			editResponseJson.remove("session");
			editResponseJson.remove("actions");
			editResponseJson.remove("createLinks");
			editResponseJson.getJSONObject("body").remove("layoutInfo");
			editResponseJson.getJSONObject("body").remove("globalData");
			editResponseJson.getJSONObject("body").remove("errors");
			editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("ragApplicable").put("options", "null");
			editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("ragApplicable").getJSONObject("values").put("name", "No");
			editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("ragApplicable").getJSONObject("values").put("id", "1002");
			editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("threshold").remove("values");
			editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("expected").remove("values");
			editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("minimum").remove("values");
			editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("sigMinMax").remove("values");

			editResponse = edit.hitEdit(slEntity, editResponseJson.toString());

			if (!editResponse.contains("success")) {
				customAssert.assertTrue(false, "Edit Response is unsuccessful");
			}
			List<Map<String, String>> viewHistoryMapList = new ArrayList<>();
			Map<String, String> viewHistoryMap = new HashMap<>();

			viewHistoryMap.put("oldValue", "Maximum - 3 level");
			viewHistoryMap.put("newValue", null);
			viewHistoryMap.put("property", "Minimum/Maximum?");
			viewHistoryMap.put("state", "REMOVED");
			viewHistoryMapList.add(viewHistoryMap);

			viewHistoryMap = new HashMap<>();
			viewHistoryMap.put("oldValue", "110");
			viewHistoryMap.put("newValue", null);
			viewHistoryMap.put("property", "Minimum");
			viewHistoryMap.put("state", "REMOVED");
			viewHistoryMapList.add(viewHistoryMap);

			viewHistoryMap = new HashMap<>();
			viewHistoryMap.put("oldValue", "80");
			viewHistoryMap.put("newValue", null);
			viewHistoryMap.put("property", "Expected");
			viewHistoryMap.put("state", "REMOVED");
			viewHistoryMapList.add(viewHistoryMap);

			viewHistoryMap = new HashMap<>();
			viewHistoryMap.put("oldValue", "120");
			viewHistoryMap.put("newValue", null);
			viewHistoryMap.put("property", "Significantly Maximum");
			viewHistoryMap.put("state", "REMOVED");
			viewHistoryMapList.add(viewHistoryMap);


			viewHistoryMap = new HashMap<>();
			viewHistoryMap.put("oldValue", "Yes");
			viewHistoryMap.put("newValue", "No");
			viewHistoryMap.put("property", "Rag Applicable?");
			viewHistoryMap.put("state", "MODIFIED");
			viewHistoryMapList.add(viewHistoryMap);

//            C7616
			if (!validateAuditLogWithHistory(slEntityTypeId, serviceLevelId1, "Updated", viewHistoryMapList, auditLogUser, customAssert)) {
				customAssert.assertTrue(false, "Audit Log validated unsuccessfully after edit of target field values");
			}

		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while validating scenarios for RAG Applicable from true to false");
		}

		customAssert.assertAll();
	}

	//    C7608
	@Test(enabled = false) //Added on 25 june 2019
	public void TestDiffScenarioOfErrorForSLMetRAGApplicableFalse() {

		CustomAssert customAssert = new CustomAssert();

		try {

			String[] valuesToBeAbsent = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "sl rag applicable no scenario", "fieldstobeabsent").split(",");

//			*********************Start Of Bulk Create Step******************************************************************************8
			List<List<String>> currentTimeStamp = getCurrentTimeStamp();

			UploadBulkData uploadBulkData = new UploadBulkData();

			Map<String, String> payloadMap = new HashMap<>();
			int entityIdContract = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulkcreatesloncontract"));
			payloadMap.put("parentEntityId", String.valueOf(entityIdContract));
			payloadMap.put("parentEntityTypeId", String.valueOf(contractEntityTypeId));

			int templateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "slbulkcreatetemplateid"));
			String bulkCreateSLFile = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulkcreateragno");

			uploadBulkData.hitUploadBulkData(slEntityTypeId, templateId, uploadFilePath, bulkCreateSLFile, payloadMap);

			String fileUploadResponse = uploadBulkData.getUploadBulkDataJsonStr();

			if (!fileUploadResponse.contains("200:;")) {

				customAssert.assertTrue(false, "Error while Bulk Create File Upload " + fileUploadResponse);

			} else {

				customAssert.assertTrue(false,"File Upload Response " + fileUploadResponse);
				if (!checkIfAllRecordsFailed()) {

					customAssert.assertTrue(false, "Expected scheduler status should be fail but scheduler job passed successfully or any other test failure happened");
				} else {

					String subjectLine = "Bulk create request response";
//
					List<List<String>> recordFromSystemEmailTable = getRecordFromSystemEmailTable(subjectLine, currentTimeStamp.get(0).get(0));

//					C13569
					List<String> expectedSentencesInBody = new ArrayList<>();

					expectedSentencesInBody.add("Entities requested to create: 6");
					expectedSentencesInBody.add("Entities successfully created : 0");
					expectedSentencesInBody.add("Entities failed to create : 6");

					if (recordFromSystemEmailTable.size() == 0) {
						customAssert.assertTrue(false, "No entry in system email table for subject Line " + subjectLine);
					}

					if (!validateAttachmentName(recordFromSystemEmailTable, bulkCreateSLFile, customAssert)) {
						customAssert.assertTrue(false, "Attachment Name validated unsuccessfully in system emails table for subjectLine " + subjectLine);
					}

					if (!validateBodyOfEmail(recordFromSystemEmailTable, expectedSentencesInBody, customAssert)) {
						customAssert.assertTrue(false, "Body validated unsuccessfully in system emails table for subjectLine " + subjectLine);
					}
				}
			}

//*********************End Of Bulk Create Step******************************************************************************8
//*********************Start of normal sl create with rag applicable as No***************************************************
			String flowToTest = "sl with rag applicable as no";
			String PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['Total Business Elapsed Time (Days)'].value < 20){if(doc['exception'].value== false){state.map.met++}else{state.map.notMet++}}else{if(doc['exception'].value== false){state.map.notMet++}else{state.map.met++}}}} if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['exception'].value== false){state.map.credit += doc['Applicable Credit'].value}}} \",\"init_script\":\"state['map'] =['met': 0.0, 'notMet': 0, 'credit':0]\",\"reduce_script\":\"params.return_map = ['Final_Numerator':0.0, 'Final_Denominator':0, 'Final_Performance':0 ,'Actual_Numerator':0.0, 'Actual_Denominator':0, 'Actual_Performance':0 ,'SL_Met':0 ,     'Calculated_Credit_Amount':0.0]; for (a in states){params.return_map.Final_Numerator += (float)(a.map.met); params.return_map.Final_Denominator += (float)(a.map.met + a.map.notMet) ; params.return_map.Calculated_Credit_Amount += (float)(a.map.credit);}  if(params.return_map.Final_Denominator > 0){params.return_map.Final_Performance = Math.round(((float)(params.return_map.Final_Numerator*100)/params.return_map.Final_Denominator)*100.0)/100.0}else{params.return_map.Final_Performance ='';params.return_map.SL_Met =5} params.return_map.Final_Numerator = Math.round(params.return_map.Final_Numerator *100.0)/100.0 ;params.return_map.Actual_Numerator = params.return_map.Final_Numerator; params.return_map.Actual_Denominator = params.return_map.Final_Denominator; params.return_map.Actual_Performance = params.return_map.Final_Performance; if(params.return_map.Final_Denominator > 0){if(params.return_map.Final_Performance < 90){params.return_map.Calculated_Credit_Amount = params.return_map.Calculated_Credit_Amount} else{params.return_map.Calculated_Credit_Amount = ''}} else{params.return_map.Calculated_Credit_Amount = ''}  return params.return_map\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}},{\"match\":{\"useInComputation\":true}}]}}}";
			String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Met/Missed\": {\"script\": \"if(params['_source']['Time Taken (Secondss1)'] != ''){if(params['_source']['Time Taken (Seconds)'] < 110000){return 'Met'}else{return 'Missed'}}else{return 'Missed'}\"}}}";

			int serviceLevelId1WithRagAsNo_1 = getActiveserviceLevelId1(flowToTest, PCQ, DCQ, auditLogUser, customAssert);
			int cslWithRagApplicableNo = -1;
			if (!validateShowPageFieldWhenRAGIsNo(slEntityTypeId, serviceLevelId1WithRagAsNo_1, valuesToBeAbsent, customAssert)) {
				customAssert.assertTrue(false, "CSL Show page fields validated unsuccessfully for RAG Applicable No ");
			}

			ArrayList<String> cslCreated1 = checkIfCSLCreatedOnServiceLevel(serviceLevelId1WithRagAsNo_1, customAssert);

			if (cslCreated1.size() == 0) {
				customAssert.assertTrue(false, "Child Service Level not created ");
				customAssert.assertAll();
			}

			addCSLToDelete(cslCreated1);

			if (cslCreated1.size() > 0) {

				cslWithRagApplicableNo = Integer.parseInt(cslCreated1.get(0));

				if (!validateShowPageFieldWhenRAGIsNo(cslEntityTypeId, cslWithRagApplicableNo, valuesToBeAbsent, customAssert)) {
					customAssert.assertTrue(false, "CSL Show page fields validated unsuccessfully for RAG Applicable No ");
				}
			} else {
				logger.error("CSL not created for the SL ID with RAG Applicable as No");
				customAssert.assertTrue(false, "CSL not created for the SL ID with RAG Applicable as No");
			}

//*********************End of normal sl create with rag applicable as No*****************************************************
//*********************Start Of Bulk Edit Step******************************************************************************8
			String bulkEditFieldIds = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "bulkedit", "fieldids");

			String expected = "80.0";
			String minMax = "100.0";
			String sigMinMax = "110.0";
			String supplierNumerator = "100.0";
			String supplierDenominator = "100.0";
			String finalNumerator = "100.0";
			String finalDenominator = "100.0";
			String actualNumerator = "100.0";
			String actualDenominator = "100.0";
			String supplierCalculation = "100.0";
			String actualPerformance = "100.0";
			String finalPerformance = "100.0";
			String discrepancy = "100.0";

			String bulkEditPayload = "{\"body\": {\"data\": {" +
					"\"finalPerformance\": {\"name\": \"finalPerformance\",\"id\": 1103,\"values\": \"" + finalPerformance + "\"}," +
					"\"discrepancy\": {\"name\": \"discrepancy\",\"id\": 1104,\"values\": \"" + discrepancy + "\"}," +
					"\"actualPerformance\": {\"name\": \"actualPerformance\",\"id\": 1102,\"values\": \"" + actualPerformance + "\"}," +
					"\"supplierCalculation\": {\"name\": \"supplierCalculation\",\"id\": 1101,\"values\": \"" + supplierCalculation + "\"}," +
					"\"expected\": {\"name\": \"expected\",\"id\": 1148,\"values\": \"" + expected + "\"}," +
					"\"minMax\": {\"name\": \"minMax\",\"id\": 1147,\"values\": \"" + minMax + "\"}," +
					"\"sigMinMax\": {\"name\": \"sigMinMax\",\"id\": 1149,\"values\": \"" + sigMinMax + "\"}," +
					"\"supplierNumerator\": {\"name\": \"supplierNumerator\",\"id\": 1106,\"values\": \"" + supplierNumerator + "\"}," +
					"\"supplierDenominator\": {\"name\": \"supplierDenominator\",\"id\": 1107,\"values\": \"" + supplierDenominator + "\"}," +
					"\"finalNumerator\": {\"name\": \"finalNumerator\",\"id\": 1110,\"values\": \"" + finalNumerator + "\"}," +
					"\"finalDenominator\": {\"name\": \"finalDenominator\",\"id\": 1111,\"values\": \"" + finalDenominator + "\"}," +
					"\"actualNumerator\": {\"name\": \"actualNumerator\",\"id\": 1108,\"values\": \"" + actualNumerator + "\"}," +
					"\"actualDenominator\": {\"name\": \"actualDenominator\",\"id\": 1109,\"values\": \"" + actualDenominator + "\"}}," +
					"\"globalData\": {\"entityIds\": [" + cslWithRagApplicableNo + "],\"fieldIds\": [" + bulkEditFieldIds + "],\"isGlobalBulk\": true}}}";

			BulkeditEdit bulkeditEdit = new BulkeditEdit();
			bulkeditEdit.hitBulkeditEdit(cslEntityTypeId, bulkEditPayload);
			String bulkEditResponse = bulkeditEdit.getBulkeditEditJsonStr();

			if (!bulkEditResponse.contains("success")) {

				customAssert.assertTrue(false, "Bulk edit done successfully Expected \"should be done unsuccessfully \"");
			} else {

				if (!checkIfAllRecordsFailed()) {

					customAssert.assertTrue(false, "Expected scheduler status should be fail but scheduler job passed successfully or any other test failure happened");

				}

			}
//*********************End Of Bulk Edit Step***********************************************************************************
//*********************Start Of Bulk Update Step******************************************************************************8
			String bulkTemplateFileNameWithRagNo = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "sl rag applicable no scenario", "bulkupdatefilename");
			int bulkUpdateTemplateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "cslbulkupdatetemplateid"));


			String cslIdsForBulkUpdate = "";
			for (int i = 1; i < 6; i++) {

				if (i == 5) {
					cslIdsForBulkUpdate += cslCreated1.get(i);
				} else {
					cslIdsForBulkUpdate += cslCreated1.get(i) + ",";
				}

			}

			Boolean BulkTemplateDownloadStatus = BulkTemplate.downloadBulkUpdateTemplate(downloadFilePath, bulkTemplateFileNameWithRagNo, bulkUpdateTemplateId, cslEntityTypeId, cslIdsForBulkUpdate);

			if (BulkTemplateDownloadStatus) {
				int rowNumToStart = 6;
				int numOfRowsToUpdate = 5;
				Map<Integer, Map<Integer, Object>> columnDataMap = new HashMap<>();

				for (int excelColNum = rowNumToStart; excelColNum < rowNumToStart + numOfRowsToUpdate; excelColNum++) {
					Map<Integer, Object> columnData = new HashMap<>();
					int columnNoToUpdate[] = {20, 21, 22, 23, 24, 25, 28, 29, 30, 32, 33, 34};
					for (int colNo : columnNoToUpdate) {
						columnData.put(colNo, 100);
					}
					columnDataMap.put(excelColNum, columnData);
				}
				String sheetName = "Child Sla";
				XLSUtils.editMultipleRowsData(downloadFilePath, bulkTemplateFileNameWithRagNo, sheetName, columnDataMap);
				String bulkUpdateUploadResponse = BulkTemplate.uploadBulkUpdateTemplate(downloadFilePath, bulkTemplateFileNameWithRagNo, cslEntityTypeId, bulkUpdateTemplateId);

				if (bulkUpdateUploadResponse.contains("200")) {

					if (!checkIfAllRecordsFailed()) {
						customAssert.assertTrue(false, "Bulk Update done successfully Expected \" Should be done successfully \"");
					}

				} else {
					customAssert.assertTrue(false, "Bulk Update done unsuccessfully Actual Bulk Update Status " + bulkUpdateUploadResponse);
				}

			} else {
				customAssert.assertTrue(false, "Bulk Template downloaded unsuccessfully ");
			}

//*********************End Of Bulk Update Step******************************************************************************8

		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while validating Diff Scenario Of Error For SLMet RAGApplicable as False");
		}

		customAssert.assertAll();
	}

	//C8178 Query corrected to test
	@Test(dataProvider = "flowsForIrrelevantTargets", enabled = false)
	public void TestMetaDataUpdateIrrelevantTargetsES(String flowToTest, String expectedMsgInBody) {

		CustomAssert customAssert = new CustomAssert();
		Show show = new Show();

		try {

			String PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\",\"init_script\":\"state['map'] = ['met':0, 'notMet':0]\",\"reduce_script\":\"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;} return params.result\",\"combine_script\":\"return state;\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}}]}}}";
			String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Met/Missed\": {\"script\": \"if(params['_source']['Time Taken (Secondss1)'] != ''){if(params['_source']['Time Taken (Seconds)'] < 110000){return 'Met'}else{return 'Missed'}}else{return 'Missed'}\"}}}";

			int serviceLevelId1 = getActiveserviceLevelId1(flowToTest, PCQ, DCQ, auditLogUser, customAssert);
			slToDelete.add(serviceLevelId1);

			List<List<String>> currentTimeStamp = getCurrentTimeStamp();

			if (serviceLevelId1 != -1) {
//
				ArrayList<String> childserviceLevelId1s = checkIfCSLCreatedOnServiceLevel(serviceLevelId1, customAssert);

				if (childserviceLevelId1s.size() == 0) {
					customAssert.assertTrue(false, "Child Service Level not created ");
					customAssert.assertAll();
				}

				addCSLToDelete(childserviceLevelId1s);
				Thread.sleep(5000);
				String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilenameesquery");
				String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");

				if (!uploadPerformanceDataFormat(serviceLevelId1, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
					customAssert.assertTrue(false, "Error while performance data file upload");
//					customAssert.assertAll();
				}

				if (!validatePerformanceDataFormatTab(serviceLevelId1, performanceDataFormatFileName, customAssert)) {

					customAssert.assertTrue(false, "Error while validating Performance Data Format Tab on Service Level");
//					customAssert.assertAll();
				}

				int childserviceLevelId1 = Integer.parseInt(childserviceLevelId1s.get(1));

				String rawDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilenameesquery");

				if (!uploadRawDataCSL(childserviceLevelId1, rawDataFileName, rawDataFileValidMsg, customAssert)) {

					customAssert.assertTrue(false, "Raw Data Uploaded unsuccessfully on CSL " + childserviceLevelId1);
//					customAssert.assertAll();
				} else {

					if (!validateStructuredPerformanceDataCSL(childserviceLevelId1, rawDataFileName, "Done", "View Structured Data", auditLogUser, customAssert)) {
						customAssert.assertTrue(false, "Raw Data Upload validation unsuccessful for CSL ID " + childserviceLevelId1);
						customAssert.assertAll();
					}
				}

				Boolean workFlowStatus = performComputationCSL(childserviceLevelId1, customAssert);
//				String workFlowActionToPerform = "ReComputePerformance";
//				WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
//				Boolean workFlowStatus = workflowActionsHelper.performWorkflowAction(cslEntityTypeId, childserviceLevelId1, workFlowActionToPerform);

				if (!workFlowStatus) {
//					customAssert.assertTrue(false, "Unable to perform " + workFlowActionToPerform + " computation on CSL Id " + childserviceLevelId1);
					customAssert.assertTrue(false, "Unable to perform  computation on CSL Id " + childserviceLevelId1);
				} else {
					show.hitShowVersion2(cslEntityTypeId, childserviceLevelId1);
					String showResponse = show.getShowJsonStr();

					String shortCodeId = ShowHelper.getValueOfField("short code id", showResponse);
					String title = ShowHelper.getValueOfField("description", showResponse);

					String subjectLine = "Error in performance computation - (#" + shortCodeId + ")-(#" + title + ")";

					List<List<String>> recordFromSystemEmailTable = getRecordFromSystemEmailTable(subjectLine, currentTimeStamp.get(0).get(0));

//					C13569
					List<String> expectedSentencesInBody = new ArrayList<>();
					expectedSentencesInBody.add(expectedMsgInBody);
					expectedSentencesInBody.add("Error Type: Processing Error");
					expectedSentencesInBody.add("Error Details:");
					expectedSentencesInBody.add("Error found while setting metadata value using elastic search query.");
					expectedSentencesInBody.add("Please contact Sirion servicedesk at support@sirioncloud.com for any assistance.");

					if (recordFromSystemEmailTable.size() == 0) {
						customAssert.assertTrue(false, "No entry in system email table for subject Line " + subjectLine);
					}

					if (!validateAttachmentName(recordFromSystemEmailTable, null, customAssert)) {
						customAssert.assertTrue(false, "Attachment Name validated unsuccessfully in system emails table for subjectLine " + subjectLine);
					}

					if (!validateBodyOfEmail(recordFromSystemEmailTable, expectedSentencesInBody, customAssert)) {
						customAssert.assertTrue(false, "Body validated unsuccessfully in system emails table for subjectLine " + subjectLine);
					}
				}
			}
		} catch (Exception e) {

			customAssert.assertTrue(false, "Exception while Validating Setting Meta Data Update Irrelevant Targets RAGApplicable " + e.getMessage());
		}

		customAssert.assertAll();

	}

	//C10544
	@Test(enabled = false)   //Validated 30 Sept
	public void TestESQueryUpdateWhenCompStatusDataNotUploaded() {

		CustomAssert customAssert = new CustomAssert();

		String flowToTest = "sl automation flow";

		try {

			String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if (doc['exception'].value=='F'){if(params['_source']['Time Taken (Seconds)'] != ''){if(doc['Time Taken (Seconds)'].value < 28800){params._agg.map.met++; }else{params._agg.map.notMet++}}}else{if(params['_source']['Time Taken (Seconds)'] != ''){if(doc['Time Taken (Seconds)'].value < 28800){params._agg.map.notMet++; }else{params._agg.map.met++}}}\", \"init_script\": \"params._agg['map'] = ['met':0, 'notMet':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30, 'SL_Met':0, 'integer':'abcd']; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107; params.result.SL_Met = 4;} return params.result\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
			String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}, \"script_fields\": {\"Met/Missed\": {\"script\": \"if(params['_source']['Time Taken (Secondss1)'] != ''){if(params['_source']['Time Taken (Seconds)'] < 110000){return 'Met'}else{return 'Missed'}}else{return 'Missed'}\"}}}";

			int serviceLevelId1 = getActiveserviceLevelId1(flowToTest, PCQ, DCQ, auditLogUser, customAssert);
			Thread.sleep(5000);
			slToDelete.add(serviceLevelId1);

			if (serviceLevelId1 != -1) {
//
				ArrayList<String> childserviceLevelId1s = checkIfCSLCreatedOnServiceLevel(serviceLevelId1, customAssert);

				if (childserviceLevelId1s.size() == 0) {
					customAssert.assertTrue(false, "Child Service Level not created ");
					customAssert.assertAll();
				}

				addCSLToDelete(childserviceLevelId1s);

				String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilenameesquery");
				String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");

				if (!uploadPerformanceDataFormat(serviceLevelId1, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
					customAssert.assertTrue(false, "Error while performance data file upload");
//					customAssert.assertAll();
				}

				if (!validatePerformanceDataFormatTab(serviceLevelId1, performanceDataFormatFileName, customAssert)) {

					customAssert.assertTrue(false, "Error while validating Performance Data Format Tab on Service Level");
//					customAssert.assertAll();
				}

				int childserviceLevelId1 = Integer.parseInt(childserviceLevelId1s.get(1));
				String PCQUpdated = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if (doc['exception'].value=='F'){if(params['_source']['Time Taken (Seconds)'] != ''){if(doc['Time Taken (Seconds)'].value < 28800){params._agg.map.met++; }else{params._agg.map.notMet++}}}else{if(params['_source']['Time Taken (Seconds)'] != ''){if(doc['Time Taken (Seconds)'].value < 28800){params._agg.map.notMet++; }else{params._agg.map.met++}}}\", \"init_script\": \"params._agg['map'] = ['met':0, 'notMet':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30, 'SL_Met':1]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107; params.result.SL_Met = 4;} return params.result\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
				if (updatePCQonMSL(serviceLevelId1, PCQUpdated, customAssert)) {

					if (!checkDCQPCQUDCOnCSL(childserviceLevelId1, DCQ, PCQUpdated, "Incident ID", customAssert)) {
						customAssert.assertTrue(false, "DCQ PCQ validated unsuccessfully on CSL ID " + childserviceLevelId1);
					}

				} else {
					customAssert.assertTrue(false, "PCQ updated unsuccessfully on SL " + serviceLevelId1);
				}

			} else {
				customAssert.assertTrue(false, "Service level id not created");
			}
		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while validating ESQuery Update When Computation Status Data Not Uploaded");
		}

		customAssert.assertAll();
	}

	//C10544		Workflow to be corrected for BA1 Env  1 oct
	@Test(enabled = false)
	public void TestESQueryUpdateWhenCompStatusErrorInComputation() {

		CustomAssert customAssert = new CustomAssert();
		String flowToTest = "sl automation flow error in computation";
		Show show = new Show();
		WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();

		String showPageResponse;

		int serviceLevelId1ErrorInComputation = -1;

		ArrayList<String> childserviceLevelId1sErrorInComputation = null;
		//Adding performanceComputationCalculationQuery hardcoded as unable to read from cfg file properly
		String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"iffff(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['Total Business Elapsed Time (Days)'].value < 20){if(doc['exception'].value== false){params._agg.map.met++}else{params._agg.map.notMet++}}else{if(doc['exception'].value== false){params._agg.map.notMet++}else{params._agg.map.met++}}}} if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['exception'].value== false){params._agg.map.credit += doc['Applicable Credit'].value}}} \", \"init_script\": \"params._agg['map'] =['met': 0.0, 'notMet': 0, 'credit':0]\", \"reduce_script\": \"params.return_map = ['Final_Numerator':0.0, 'Final_Denominator':0, 'Final_Performance':0 ,'Actual_Numerator':0.0, 'Actual_Denominator':0, 'Actual_Performance':0 ,'SL_Met':0 ,     'Calculated_Credit_Amount':0.0]; for (a in params._aggs){params.return_map.Final_Numerator += (float)(a.map.met); params.return_map.Final_Denominator += (float)(a.map.met + a.map.notMet) ; params.return_map.Calculated_Credit_Amount += (float)(a.map.credit);}  if(params.return_map.Final_Denominator > 0){params.return_map.Final_Performance = Math.round(((float)(params.return_map.Final_Numerator*100)/params.return_map.Final_Denominator)*100.0)/100.0}else{params.return_map.Final_Performance ='';params.return_map.SL_Met =5} params.return_map.Final_Numerator = Math.round(params.return_map.Final_Numerator *100.0)/100.0 ;params.return_map.Actual_Numerator = params.return_map.Final_Numerator; params.return_map.Actual_Denominator = params.return_map.Final_Denominator; params.return_map.Actual_Performance = params.return_map.Final_Performance; if(params.return_map.Final_Denominator > 0){if(params.return_map.Final_Performance < 90){params.return_map.Calculated_Credit_Amount = params.return_map.Calculated_Credit_Amount} else{params.return_map.Calculated_Credit_Amount = ''}} else{params.return_map.Calculated_Credit_Amount = ''}  return params.return_map\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}, {\"matcheeee\": {\"useInComputation\": true}}]}}}";
		String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";

		try{

			serviceLevelId1ErrorInComputation = getActiveserviceLevelId1(flowToTest, PCQ, DCQ, auditLogUser, customAssert);

			if (serviceLevelId1ErrorInComputation != -1) {

				slToDelete.add(serviceLevelId1ErrorInComputation);
				String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilename");
				String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");

				if (!uploadPerformanceDataFormat(serviceLevelId1ErrorInComputation, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
					customAssert.assertTrue(false, "Error while performance data file upload");
//					customAssert.assertAll();
				}

				if (!validatePerformanceDataFormatTab(serviceLevelId1ErrorInComputation, performanceDataFormatFileName, customAssert)) {

					customAssert.assertTrue(false, "Error while validating Performance Data Format Tab for SL Id " + serviceLevelId1);
				}

				childserviceLevelId1sErrorInComputation = checkIfCSLCreatedOnServiceLevel(serviceLevelId1ErrorInComputation, customAssert);

				if (childserviceLevelId1sErrorInComputation.size() == 0) {
					customAssert.assertTrue(false, "Child Service Level not created ");
					customAssert.assertAll();
				}

				addCSLToDelete(childserviceLevelId1sErrorInComputation);

				int childserviceLevelId1ErrorInComputation = -1;

				childserviceLevelId1ErrorInComputation = Integer.parseInt(childserviceLevelId1sErrorInComputation.get(0));

				String rawDataFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatafilename");

				if (childserviceLevelId1ErrorInComputation != -1) {

					if (!uploadRawDataCSL(childserviceLevelId1ErrorInComputation, rawDataFileName, rawDataFileValidMsg, customAssert)) {
//
						customAssert.assertTrue(false, "Raw Data Uploaded unsuccessfully on CSL " + childserviceLevelId1ErrorInComputation);
//						customAssert.assertAll();
					}

					if (!validateStructuredPerformanceDataCSL(childserviceLevelId1ErrorInComputation, rawDataFileName, "Done", "View Structured Data", auditLogUser, customAssert)) {
						customAssert.assertTrue(false, "Structure Performance Data Tab validated unsuccessfully for CSL ID " + childserviceLevelId1ErrorInComputation);
					}

					List<String> workFlowSteps = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "cslerrorincomputationworkflowsteps").split("->"));

					//Performing workflow Actions till Error in computation
					if (!performComputationCSL(childserviceLevelId1ErrorInComputation,customAssert)) {
						customAssert.assertTrue(false, "Error while performing workflow actions on SL ID " + serviceLevelId1ErrorInComputation);
//						customAssert.assertAll();
					}

					Thread.sleep(55000);

					show.hitShowVersion2(cslEntityTypeId, childserviceLevelId1ErrorInComputation);
					showPageResponse = show.getShowJsonStr();

					String computationStatus = ShowHelper.getValueOfField("computationstatus", showPageResponse);

					if (!computationStatus.equalsIgnoreCase("Error in Computation")) {

						customAssert.assertTrue(false, "Computation Status Expected \"Error in Computation\" Actual Computation Status " + computationStatus);
//						customAssert.assertAll();
					}else {

						logger.info("Checking DCQ PCQ UDC when CSL in Error in Computation " + childserviceLevelId1ErrorInComputation);

						//After The status Data Marked for Computation updating the PCQ
						String PCQUpdated = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if (doc['exception'].value=='F'){if(params['_source']['Time Taken (Seconds)'] != ''){if(doc['Time Taken (Seconds)'].value < 28800){params._agg.map.met++; }else{params._agg.map.notMet++}}}else{if(params['_source']['Time Taken (Seconds)'] != ''){if(doc['Time Taken (Seconds)'].value < 28800){params._agg.map.notMet++; }else{params._agg.map.met++}}}\", \"init_script\": \"params._agg['map'] = ['met':0, 'notMet':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30, 'SL_Met':0, 'integer':'abcd']; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107; params.result.SL_Met = 4;} return params.result\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
						if (updatePCQonMSL(serviceLevelId1ErrorInComputation, PCQUpdated, customAssert)) {

							if (!checkDCQPCQUDCOnCSL(childserviceLevelId1ErrorInComputation, DCQ, PCQUpdated, "Number", customAssert)) {
								customAssert.assertTrue(false, "DCQ PCQ validated unsuccessfully on CSL ID " + childserviceLevelId1ErrorInComputation);
							}
						}else {
							customAssert.assertTrue(false,"PCQ updated unsuccessfully on SL ID " + serviceLevelId1ErrorInComputation);
						}

						String workFlowActionToPerform = "ReComputePerformance";
						if (!workflowActionsHelper.performWorkFlowStepV2(cslEntityTypeId, childserviceLevelId1ErrorInComputation, workFlowActionToPerform,customAssert)) {
							customAssert.assertTrue(false, "Unable to perform recompute performance for the entity " + childserviceLevelId1ErrorInComputation);

						}

						Thread.sleep(55000);

						show.hitShowVersion2(cslEntityTypeId, childserviceLevelId1ErrorInComputation);
						showPageResponse = show.getShowJsonStr();

						computationStatus = ShowHelper.getValueOfField("computationstatus", showPageResponse);

						if (!computationStatus.equals("Data Marked for Computation")) {
							customAssert.assertTrue(false, "Expected status is Data Marked for Computation after PCQ update after 55000 ms");
						}

						workFlowActionToPerform = "ApproveComputation";
						if (!workflowActionsHelper.performWorkFlowStepV2(cslEntityTypeId, childserviceLevelId1ErrorInComputation, workFlowActionToPerform,customAssert)) {
							customAssert.assertTrue(false, "Unable to perform recompute performance for the entity " + childserviceLevelId1ErrorInComputation);
							customAssert.assertAll();
						}
						Thread.sleep(55000);

						show.hitShowVersion2(cslEntityTypeId, childserviceLevelId1ErrorInComputation);
						showPageResponse = show.getShowJsonStr();

						computationStatus = ShowHelper.getValueOfField("computationstatus", showPageResponse);

						if (!computationStatus.equals("Computation Completed Successfully")) {

							customAssert.assertTrue(false, "Expected status is Computation Completed Successfully after PCQ update");
						}else {
							PCQUpdated = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if (doc['exception'].value=='F'){if(params['_source']['Time Taken (Seconds)'] != ''){if(doc['Time Taken (Seconds)'].value < 28800){params._agg.map.met++; }else{params._agg.map.notMet++}}}else{if(params['_source']['Time Taken (Seconds)'] != ''){if(doc['Time Taken (Seconds)'].value < 28800){params._agg.map.notMet++; }else{params._agg.map.met++}}}\", \"init_script\": \"params._agg['map'] = ['met':0, 'notMet':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30, 'SL_Met':7]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107; params.result.SL_Met = 8;} return params.result\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
							if (updatePCQonMSL(serviceLevelId1ErrorInComputation, PCQUpdated, customAssert)) {

								if (!checkDCQPCQUDCOnCSL(childserviceLevelId1ErrorInComputation, DCQ, PCQUpdated, "Number", customAssert)) {
									customAssert.assertTrue(false, "DCQ PCQ validated unsuccessfully on CSL ID " + childserviceLevelId1ErrorInComputation);
								}else {
									logger.info("DCQ PCQ UDC validated successfully on CSL " + childserviceLevelId1ErrorInComputation + " after Computation Completed Successfully");
								}
							}else {
								logger.error("PCQ updated unsuccessfully on SL ID " + serviceLevelId1ErrorInComputation);
								customAssert.assertTrue(false,"PCQ updated unsuccessfully on SL ID " + serviceLevelId1ErrorInComputation);
							}
						}
					}

				}else {
					logger.error("Child Service Level ID = -1 ");
					customAssert.assertTrue(false, "Child service not created for SL ID " + serviceLevelId1ErrorInComputation);
				}

			}

		}catch (Exception e){
			logger.error("Exception while validating ESQuery Update When CompStatus ErrorInComputation " + e.getMessage());
			customAssert.assertTrue(false,"Exception while validating ESQuery Update When CompStatus ErrorInComputation " + e.getMessage());
		}

		customAssert.assertAll();
	}

	@Test(enabled = false)
	public void TestSingleFormatBulkUpload(){

		CustomAssert customAssert = new CustomAssert();
		ArrayList<String> FileToDeleteList = new ArrayList<>();
		String fileUploadPath;
		try{

			String flowToTest = "sl automation flow";

			//Adding performanceComputationCalculationQuery hardcoded as unable to read from cfg file properly

			int serviceLevelId11;
			int serviceLevelId12;
			ArrayList<String> childserviceLevelId1s1;
			ArrayList<String> childserviceLevelId1s2;

			Map<String, String> textBodyMap = new LinkedHashMap<>();
			Map<String, File> fileToUpload = new LinkedHashMap<>();

			String PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\",\"init_script\":\"state['map'] = ['met':0, 'notMet':0]\",\"reduce_script\":\"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;} return params.result\",\"combine_script\":\"return state;\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}}]}}}";
			String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";

			serviceLevelId11 = getActiveserviceLevelId1(flowToTest, PCQ, DCQ,auditLogUser,false, customAssert);
			serviceLevelId12 = getActiveserviceLevelId1(flowToTest, PCQ, DCQ,auditLogUser,false, customAssert);

			slToDelete.add(serviceLevelId11);
			slToDelete.add(serviceLevelId12);

			childserviceLevelId1s1 = checkIfCSLCreatedOnServiceLevel(serviceLevelId11,customAssert);
			childserviceLevelId1s2 = checkIfCSLCreatedOnServiceLevel(serviceLevelId12,customAssert);
			Thread.sleep(5000);
			fileUploadPath = uploadFilePath + "/SIR_528";

			String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilename");

			String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");


			if (!uploadPerformanceDataFormat(serviceLevelId11, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, fileUploadPath, performanceDataFormatFileName, expectedMsg, customAssert)) {
				customAssert.assertTrue(false, "Error while performance data file upload");
			}

			if (!uploadPerformanceDataFormat(serviceLevelId12, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, fileUploadPath, performanceDataFormatFileName, expectedMsg, customAssert)) {
				customAssert.assertTrue(false, "Error while performance data file upload");
			}

			int numberOfCslCreated = childserviceLevelId1s1.size();


			String masterFileName = "RawDataFile";
//			String masterFileName = "CSL Raw Data BA POD";
			String masterFileExtension = ".xlsx";
			String masterFileActual = masterFileName + masterFileExtension;

			String fileNameForCsl1;
			String fileNameForCsl2;
			int cslSizeToUploadRawData = 2;
			int j =0;
			for(int i =0;i<cslSizeToUploadRawData;i++){

				try{
					childserviceLevelId1s1.get(i);
					childserviceLevelId1s2.get(i);

					textBodyMap.put("cslIds[" + j +"]",childserviceLevelId1s1.get(i));
					j =j +1;
					textBodyMap.put("cslIds[" + j +"]",childserviceLevelId1s2.get(i));
					j =j +1;

				}catch (Exception e){
					logger.error("Exception while preparing form data for API /slRawData/v1/singleFormatBulkUpload " + e.getMessage());
				}
			}

			fileNameForCsl1  = masterFileName + "0" + masterFileExtension;
			fileNameForCsl2  = masterFileName + "1" + masterFileExtension;

			FileUtils.copyFile(fileUploadPath , masterFileActual,fileUploadPath,fileNameForCsl1);
			FileUtils.copyFile(fileUploadPath , masterFileActual,fileUploadPath,fileNameForCsl2);

			FileToDeleteList.add(fileNameForCsl1);
			FileToDeleteList.add(fileNameForCsl2);

			fileToUpload.put("rawDataFiles[" + "0" +"]",new File(fileUploadPath + "/" + fileNameForCsl1));
			fileToUpload.put("rawDataFiles[" + "1" +"]",new File(fileUploadPath + "/" + fileNameForCsl2));

			UploadRawData uploadRawData = new UploadRawData();
			uploadRawData.hitBulkUploadRawDataDiffCsl(textBodyMap,fileToUpload);
			String uploadRawDataResponse = uploadRawData.getUploadRawDataJsonStr();


			if(!uploadRawDataResponse.contains("200:;")){
				customAssert.assertTrue(false,"Bulk Upload unsuccessful"+ uploadRawDataResponse);
			}
			int cslId;

			Boolean validationStatus;
			Thread.sleep(60000);

			for(int i =0;i<cslSizeToUploadRawData;i++) {
				cslId = Integer.parseInt(childserviceLevelId1s1.get(i));

				validationStatus = 	validateStructuredPerformanceDataCSL(cslId, "Done", customAssert);

				if(!validationStatus){
					customAssert.assertTrue(false,"Validation Status not true for CSL Number " + i + " CSL ID " + cslId);
				}

			}

			for(int i =0;i<cslSizeToUploadRawData;i++) {
				cslId = Integer.parseInt(childserviceLevelId1s2.get(i));

				validationStatus = 	validateStructuredPerformanceDataCSL(cslId, "Done", customAssert);

				if(!validationStatus){
					customAssert.assertTrue(false,"Validation Status not true for CSL Number " + i + " CSL ID " + cslId);
				}

			}

		}catch (Exception e){
			logger.error("Exception while validating single Format Bulk Upload " + e.getMessage());
		}finally {

			for(String fileToDelete: FileToDeleteList){

				FileUtils.deleteFile(uploadFilePath ,fileToDelete);

			}
		}

		customAssert.assertAll();

	}


	@Test(enabled = false)
	public void TestServiceNowIntegration(){

		CustomAssert customAssert = new CustomAssert();

		try{

			String flowToTest = "sl for snow integration";

			String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['Total Business Elapsed Time (Days)'].value < 20){if(doc['exception'].value== false){params._agg.map.met++}else{params._agg.map.notMet++}}else{if(doc['exception'].value== false){params._agg.map.notMet++}else{params._agg.map.met++}}}} if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['exception'].value== false){params._agg.map.credit += doc['Applicable Credit'].value}}} \", \"init_script\": \"params._agg['map'] =['met': 0.0, 'notMet': 0, 'credit':0]\", \"reduce_script\": \"params.return_map = ['Final_Numerator':0.0, 'Final_Denominator':0, 'Final_Performance':0 ,'Actual_Numerator':0.0, 'Actual_Denominator':0, 'Actual_Performance':0 ,'SL_Met':0 ,     'Calculated_Credit_Amount':0.0]; for (a in params._aggs){params.return_map.Final_Numerator += (float)(a.map.met); params.return_map.Final_Denominator += (float)(a.map.met + a.map.notMet) ; params.return_map.Calculated_Credit_Amount += (float)(a.map.credit);}  if(params.return_map.Final_Denominator > 0){params.return_map.Final_Performance = Math.round(((float)(params.return_map.Final_Numerator*100)/params.return_map.Final_Denominator)*100.0)/100.0}else{params.return_map.Final_Performance ='';params.return_map.SL_Met =5} params.return_map.Final_Numerator = Math.round(params.return_map.Final_Numerator *100.0)/100.0 ;params.return_map.Actual_Numerator = params.return_map.Final_Numerator; params.return_map.Actual_Denominator = params.return_map.Final_Denominator; params.return_map.Actual_Performance = params.return_map.Final_Performance; if(params.return_map.Final_Denominator > 0){if(params.return_map.Final_Performance < 90){params.return_map.Calculated_Credit_Amount = params.return_map.Calculated_Credit_Amount} else{params.return_map.Calculated_Credit_Amount = ''}} else{params.return_map.Calculated_Credit_Amount = ''}  return params.return_map\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}, {\"match\": {\"useInComputation\": true}}]}}}";
			String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";

			ArrayList<String> childserviceLevelId1ForSNowIntegrationList;
			ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();

			int serviceLevelId1ForSNowIntegration = getActiveserviceLevelId1(flowToTest,PCQ,DCQ,auditLogUser,false,customAssert);
//			int serviceLevelId1ForSNowIntegration = 6674;
			String uploadFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "snow integration flow","uploadfilepath");
			String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "snow integration flow","sltemplatefile");
			String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");

			Boolean uploadPerformanceDataStatus = uploadPerformanceDataFormat(serviceLevelId1ForSNowIntegration, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert);

			if (!uploadPerformanceDataStatus) {
				customAssert.assertTrue(false, "Error while sl template file upload");
			}

			if (!validatePerformanceDataFormatTab(serviceLevelId1ForSNowIntegration, performanceDataFormatFileName, customAssert)) {

				customAssert.assertTrue(false, "Error while validating Performance Data Format Tab");
			}

			childserviceLevelId1ForSNowIntegrationList = checkIfCSLCreatedOnServiceLevel(serviceLevelId1ForSNowIntegration, customAssert);

			if (childserviceLevelId1ForSNowIntegrationList.size() == 0) {
				customAssert.assertTrue(false, "Child Service Level not created ");
				customAssert.assertAll();
			}

			addCSLToDelete(childserviceLevelId1ForSNowIntegrationList);

			String previousMonth = DateUtils.getPreviousMonth();

			String childserviceLevelId1 = serviceLevelHelper.getCSLForCurrMonth(previousMonth,serviceLevelId1ForSNowIntegration,customAssert);
			int childserviceLevelId1ForSNowIntegration = -1;

			if(childserviceLevelId1.equalsIgnoreCase("")){
				customAssert.assertTrue(false,"Unable to get CSL for previous month for SL " + serviceLevelId1ForSNowIntegration);
			}else {
				try {
					childserviceLevelId1ForSNowIntegration = Integer.parseInt(childserviceLevelId1);
				}catch (Exception e) {
					logger.error("Exception while converting child service level to integer " + e.getMessage());
					customAssert.assertTrue(false,"Exception while converting child service level to integer " + e.getMessage());
				}
			}

			if(childserviceLevelId1ForSNowIntegration == -1){
				logger.error("Child Service ID not calculated properly for previous month");
				customAssert.assertTrue(false,"Child Service ID not calculated properly for previous month");
				customAssert.assertAll();
			}

			Thread.sleep(12000);

			String status = "Done";

			String expectedCompletedBy = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"snow integration flow","expectedcompletedby");
			Boolean validationStatusCSLRawDataTab = validateStructuredPerformanceDataCSL(childserviceLevelId1ForSNowIntegration,"Check for Snow File",status,"View Structured Data",expectedCompletedBy,customAssert);

			if(!validationStatusCSLRawDataTab){
				customAssert.assertTrue(false,"Raw Data Tab validated unsuccessfully for CSL " + childserviceLevelId1ForSNowIntegration);
			}

			//Validating view Structured Data Link on Raw Data Tab
			String structurePerformanceDataResponse = serviceLevelHelper.getStructuredPerformanceData(childserviceLevelId1ForSNowIntegration);
			String documentId = serviceLevelHelper.getDocumentIdFromCSLRawDataTab(structurePerformanceDataResponse);

			String payload = "{\"documentId\":" + documentId + ",\"offset\":0,\"size\":20,\"childSlaId\":" + childserviceLevelId1ForSNowIntegration +"}";

			SLDetails slDetails = new SLDetails();
			slDetails.hitSLDetailsList(payload);
			String slDetailsResponse = slDetails.getSLDetailsResponseStr();

			if(slDetailsResponse.contains("Error in loading performance data")){
				customAssert.assertTrue(false,"Error while opening link View Structured Data For CSL " + childserviceLevelId1ForSNowIntegration );
			}else {

			}


		}catch (Exception e){

			logger.error("Exception while validating with Service Now Integration scenario " + e.getMessage());
		}


		customAssert.assertAll();
	}

	@AfterMethod(groups = { "PCQ DCQ UDC Update1" }, alwaysRun = false)
	public void resetPCQDCQUDCForGroup1(){

		CustomAssert customAssert = new CustomAssert();
		ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();

		String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\", \"init_script\": \"state['map'] = ['met':0, 'notMet':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;} return params.result\", \"combine_script\": \"return state;\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
		String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
		String UDC = "Incident ID";

		Boolean editStatus = serviceLevelHelper.editPCQDCQUDC(serviceLevelId1,PCQ,DCQ,UDC,customAssert);

		if(!editStatus){
//			customAssert.assertTrue(false,"Error while editing PCQ DCQ UDC for SL" );
//			customAssert.assertAll();
		}


		String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilename");
		String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");

		if (!validatePerformanceDataFormatTab(serviceLevelId1, performanceDataFormatFileName)) {
//
//					customAssert.assertTrue(false, "Error while validating Performance Data Format Tab for Service Level 2");
//				}
			if (!uploadPerformanceDataFormat(serviceLevelId1, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
//				customAssert.assertTrue(false, "Error while performance data file upload");
//					customAssert.assertAll();
			}
		}
		customAssert.assertAll();

	}

	@AfterMethod(groups = { "PCQ DCQ UDC Update2" }, alwaysRun = false)
	public void resetPCQDCQUDCForGroup2(){

		CustomAssert customAssert = new CustomAssert();
		ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();

		String PCQ = "{\"aggs\": {\"group_by_sl_met\": {\"scripted_metric\": {\"map_script\": \"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\", \"init_script\": \"state['map'] = ['met':0, 'notMet':0]\", \"reduce_script\": \"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;} return params.result\", \"combine_script\": \"return state;\"}}}, \"size\": 0, \"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
		String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
		String UDC = "Incident ID";

		Boolean editStatus = serviceLevelHelper.editPCQDCQUDC(serviceLevelId12,PCQ,DCQ,UDC,customAssert);

		if(!editStatus){
//			customAssert.assertTrue(false,"Error while editing PCQ DCQ UDC for SL" );
//			customAssert.assertAll();
		}


		String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilename");
		String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");

		if (!validatePerformanceDataFormatTab(serviceLevelId12, performanceDataFormatFileName)) {

			if (!uploadPerformanceDataFormat(serviceLevelId12, uploadIdSL_PerformanceDataTab, slMetaDataUploadTemplateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
//				customAssert.assertTrue(false, "Error while performance data file upload");
			}
		}

		customAssert.assertAll();

	}

	@AfterClass(groups = {"sanity","sprint"})
	public void afterClass() {

		logger.debug("Number CSL To Delete " + cslToDelete.size());
		EntityOperationsHelper.deleteMultipleRecords("child service levels", cslToDelete);

		logger.debug("Number SL To Delete " + slToDelete.size());
		EntityOperationsHelper.deleteMultipleRecords("service levels", slToDelete);

	}

    private Boolean performWorkFlowActions(int entityTypeId, int entityId, List<String> workFlowSteps, String user, CustomAssert customAssert) {

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
					actionNameAuditLog = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "auditlogactioname", workFlowStepToBePerformed);
					if (!verifyAuditLog(entityTypeId, entityId, actionNameAuditLog, user, customAssert)) {
						customAssert.assertTrue(false, "Audit Log tab verified unsuccessfully for entity id " + entityId);
					}
				}
			}

		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while performing Workflow actions for service level id " + entityId + e.getMessage());
			return false;

		}
		return true;
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

	//    C10565 C10686 C44282
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

	//    C10565 C10638
	private Boolean validatePerformanceDataFormatTab(int entityId, String uploadFileName) {

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

			while (timeSpent < cSLPerformanceDataFormatTabTimeOut) {

				tabListData.hitTabListData(performanceDataFormatTabId, slEntityTypeId, entityId);
				tabListDataResponse = tabListData.getTabListDataResponseStr();

				if (JSONUtility.validjson(tabListDataResponse)) {

					tabListDataResponseJson = new JSONObject(tabListDataResponse);
					dataArray = tabListDataResponseJson.getJSONArray("data");

					if (dataArray.length() >= 1) {
						break;

					}
				} else {
					return false;
				}
				Thread.sleep(pollingTime);
				timeSpent = timeSpent + pollingTime;
			}
			//C10739
			if (uploadFileName.equalsIgnoreCase("")) {
				if (dataArray.length() != 0) {
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

						validationStatus = false;
					}
				}
			}
		} catch (Exception e) {
			validationStatus = false;
		}
		return validationStatus;
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

	//    C10636  C10638
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

	//    C10638
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
				customAssert.assertTrue(false, "Raw Data File not Uploaded has invalid Json Response for service level id " + serviceLevelId1);
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

	private Boolean validateStructuredPerformanceDataCSL(int CSLId, String computationStatus, CustomAssert customAssert) {

		logger.info("Validating Structured Performance Data tab on CSL " + CSLId);
		Boolean validationStatus = true;
		long timeSpent = 0;
		long fileUploadTimeOut = 120000L;
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

			for(int k =0;k<dataArray.length();k++) {

				JSONObject individualRowData = dataArray.getJSONObject(k);

				JSONArray individualRowDataJsonArray = JSONUtility.convertJsonOnjectToJsonArray(individualRowData);
				JSONObject individualColumnJson;
				String columnName;
				String columnValue;

				innerLoop :
				for (int i = 0; i < individualRowDataJsonArray.length(); i++) {

					individualColumnJson = individualRowDataJsonArray.getJSONObject(i);

					columnName = individualColumnJson.get("columnName").toString();
					columnValue = individualColumnJson.get("value").toString();

					if (columnName.equalsIgnoreCase("status")) {

						String status = columnValue.split(":;")[0];

						if (!status.equalsIgnoreCase(computationStatus)) {
							customAssert.assertTrue(false, "Structure Performance Data Status Expected : " + computationStatus + " Actual Status : " + status  + " for row number " + k + " and CSL ID " + CSLId);
							validationStatus = false;
						}
						break innerLoop;
					}

				}
			}
		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while validating Structured Performance Data on CSL " + CSLId + " " + e.getMessage());
			validationStatus = false;
		}

		return validationStatus;

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

	private List<List<String>> getRecordFromSystemEmailTable(String subjectLine, String currentTimeStamp) {

		String sqlQuery = "select subject,attachment,sent_successfully,body from system_emails where subject ilike '%" + subjectLine + "%' AND date_created > " + "'" + currentTimeStamp + "'"
				+ "order by id desc";
		List<List<String>> queryResult = null;
		PostgreSQLJDBC postgreSQLJDBC;

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

	private List<List<String>> getDBQueryOutput(String sql, CustomAssert customAssert) {
		List<List<String>> dBQueryOutput = new ArrayList<>();
		PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostName,dbPortName,dbName,dbUserName,dbPassowrd);
		try {

			dBQueryOutput = postgreSQLJDBC.doSelect(sql);
		} catch (Exception e) {

			logger.error("Exception while getting DB Data while executing DB Query " + sql);
			customAssert.assertTrue(false, "Exception while getting DB Data while executing DB Query " + sql + " " + e.getMessage());
		}finally {
			postgreSQLJDBC.closeConnection();
		}

		return dBQueryOutput;
	}

	private int getActiveserviceLevelId1(String flowToTest, String PCQ, String DCQ, String user, CustomAssert customAssert) {

		int serviceLevelId1 = -1;
		try {

			serviceLevelId1 = getserviceLevelId1(flowToTest, PCQ, DCQ, customAssert);

			if (serviceLevelId1 != -1) {
				List<String> workFlowSteps;
				workFlowSteps = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "slactiveworkflowsteps").split("->"));
				if (!performWorkFlowActions(slEntityTypeId, serviceLevelId1, workFlowSteps, user,false, customAssert)) {
					customAssert.assertTrue(false, "Error while performing workflow actions on SL ID " + serviceLevelId1);
					return -1;
				}
			}

		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while getting an active an active service level id " + e.getMessage());
		}
		return serviceLevelId1;
	}

	private int getActiveserviceLevelId1(String flowToTest, String PCQ, String DCQ, String user,Boolean checkAuditLogTab, CustomAssert customAssert) {

		int serviceLevelId1 = -1;
		try {

			serviceLevelId1 = getserviceLevelId1(flowToTest, PCQ, DCQ, customAssert);

			if (serviceLevelId1 != -1) {
				List<String> workFlowSteps;
				workFlowSteps = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "slactiveworkflowsteps").split("->"));
				if (!performWorkFlowActions(slEntityTypeId, serviceLevelId1, workFlowSteps, user,checkAuditLogTab, customAssert)) {
					customAssert.assertTrue(false, "Error while performing workflow actions on SL ID " + serviceLevelId1);
					customAssert.assertAll();
					return -1;
				}
			}

		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while getting an active an active service level id " + e.getMessage());
		}
		return serviceLevelId1;
	}

	private Boolean uploadPerformanceDataFormatFile(int entityId, CustomAssert customAssert) {

		Boolean uploadStatus = true;
		try {
			int uploadId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "uploadidslperformancecdatatab"));
			int templateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "slmetadatauploadtemplateid"));

			String performanceDataFormatFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedataformatfilename");
			String expectedMsg = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "performancedatuploadsuccessmsg");
			if (!uploadPerformanceDataFormat(entityId, uploadId, templateId, uploadFilePath, performanceDataFormatFileName, expectedMsg, customAssert)) {
				customAssert.assertTrue(false, "Error while performance data file upload on entity " + entityId);

				return false;
			}

			if (!validatePerformanceDataFormatTab(entityId, performanceDataFormatFileName, customAssert)) {

				customAssert.assertTrue(false, "Error while validating Performance Data Format Tab");
				uploadStatus = false;
			}

		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while uploading Performance Data Format File " + e.getMessage());
			uploadStatus = false;
		}

		return uploadStatus;
	}

	private int approveCSL(int childserviceLevelId1,CustomAssert customAssert) {

		String actionName = "ApproveComputation";
		WorkflowActionsHelper workflowActionsHelper = new WorkflowActionsHelper();
		try {
//
//			if (!workflowActionsHelper.performWorkflowAction(cslEntityTypeId, childserviceLevelId1, actionName)) {
			if (!workflowActionsHelper.performWorkFlowStepV2(cslEntityTypeId, childserviceLevelId1, actionName,customAssert)) {
				childserviceLevelId1 = -1;
			}
		} catch (Exception e) {
			logger.error("Exception while performing " + actionName + "on CSL ID " + childserviceLevelId1 + " " + e.getMessage() );
			childserviceLevelId1 = -1;
		}

		return childserviceLevelId1;
	}

	private Boolean updateDCQPCQUDCOnSL(int serviceLevelId1, String PCQUpdated, String DCQUpdated, String UDCUpdated, CustomAssert customAssert) {

		logger.info("Updating PCQ and DCQ on Service Level ID " + serviceLevelId1);
		Boolean updateStatus = true;
		try {
			Edit edit = new Edit();
			String editResponse = edit.hitEdit("service levels", serviceLevelId1);

			if (APIUtils.validJsonResponse(editResponse)) {

				JSONObject editResponseJson = new JSONObject(editResponse);

				editResponseJson.remove("header");
				editResponseJson.remove("session");
				editResponseJson.remove("actions");
				editResponseJson.remove("createLinks");
				editResponseJson.getJSONObject("body").remove("layoutInfo");
				editResponseJson.getJSONObject("body").remove("globalData");
				editResponseJson.getJSONObject("body").remove("errors");

				editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("performanceComputationCalculationQuery").put("values", PCQUpdated);
				editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("performanceDataCalculationQuery").put("values", DCQUpdated);
				editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("uniqueDataCriteria").put("values", UDCUpdated);

				editResponse = edit.hitEdit("service levels", editResponseJson.toString());

				if (!editResponse.contains("\"status\":\"success\"")) {
					customAssert.assertTrue(false, "Service Level ID " + serviceLevelId1 + " updated unsuccessfully");
					updateStatus = false;
				}

			} else {
				customAssert.assertTrue(false, "Edit Response is not a valid Json for service Level " + serviceLevelId1);
				updateStatus = false;
			}
		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while editing service level ID " + serviceLevelId1 + " for DCQ and PCQ " + e.getMessage());
			updateStatus = false;
		}

		return updateStatus;
	}

	private Boolean checkDCQPCQUDCOnCSL(int childserviceLevelId1, String DCQExpected, String PCQExpected, String UDCExpected, CustomAssert customAssert) {

		Boolean validationStatus = true;
		Show show = new Show();

		try {
			show.hitShowVersion2(cslEntityTypeId, childserviceLevelId1);
			String showResponse = show.getShowJsonStr();

			String DCQActual = ShowHelper.getValueOfField("performancedatacalculationquery", showResponse);
			String PCQActual = ShowHelper.getValueOfField("performancecomputationcalculationquery", showResponse);
			String UDCActual = ShowHelper.getValueOfField("udc", showResponse);

			if (!DCQActual.equalsIgnoreCase(DCQExpected)) {

				customAssert.assertTrue(false, "DCQ Expected and Actual are not equal");
				//customAssert.assertTrue(false, "DCQ Expected : " + DCQExpected + "DCQ Actual : " + DCQActual);
				validationStatus = false;
			}

			if (!PCQActual.equalsIgnoreCase(PCQExpected)) {

				customAssert.assertTrue(false, "PCQ Expected and Actual are not equal");
				//customAssert.assertTrue(false, "PCQ Expected : " + PCQExpected + "PCQ Actual : " + PCQActual);
				validationStatus = false;
			}

			if (!UDCActual.equalsIgnoreCase(UDCExpected)) {

				customAssert.assertTrue(false, "UDC Expected and Actual are not equal");
				//customAssert.assertTrue(false, "UDC Expected : " + UDCExpected + "UDC Actual : " + UDCActual);
				validationStatus = false;
			}

		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while validating DCQ and  PCQ on child SL " + childserviceLevelId1);
			validationStatus = false;
		}

		return validationStatus;
	}

	private int getserviceLevelId1(String flowToTest, String PCQ, String DCQ, CustomAssert customAssert) {

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

	private JSONObject editPayloadJson(String entityName, int entityId) {

		Edit edit = new Edit();
		JSONObject editResponseJson = null;
		try {
			String editResponse = edit.hitEdit(entityName, entityId);

			if (APIUtils.validJsonResponse(editResponse)) {

				editResponseJson = new JSONObject(editResponse);

				editResponseJson.remove("header");
				editResponseJson.remove("session");
				editResponseJson.remove("actions");
				editResponseJson.remove("createLinks");
				editResponseJson.getJSONObject("body").remove("layoutInfo");
				editResponseJson.getJSONObject("body").remove("globalData");
				editResponseJson.getJSONObject("body").remove("errors");

			}
		} catch (Exception e) {
			logger.error("Exception while getting edit response for entity " + entityId);
		}
		return editResponseJson;
	}

	private String createPayloadForSLMetStatusValidation(String showResponse, String finalNum, String finalDen,
														 Double finalPerformance, CustomAssert customAssert) {
		String payloadForSLMetStatusValidation = null;

		try {
			JSONObject showResponseJson = new JSONObject(showResponse);

			showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("finalNumerator").put("values", finalNum);
			showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("finalDenominator").put("values", finalDen);
			showResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("finalPerformance").put("values", finalPerformance);

			payloadForSLMetStatusValidation = showResponseJson.toString();

		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while creating Payload For SL Met Status Validation");
		}

		return payloadForSLMetStatusValidation;
	}


	private HashMap<String, String> renameFolderAndFiles(String filePath, HashMap<String, String> filePathFileNameMap,String newFileNameAfterRename, CustomAssert customAssert) {

		try {
			File file = new File(filePath);
			File[] listFiles = file.listFiles();
			String[] list = file.list();

			for (int i = 0; i < listFiles.length; i++) {

				//newFilePath = listFiles[i].toString();
				//File newFile = new File(newFilePath);
				if (listFiles[i].isDirectory()) {
					filePathFileNameMap = renameFolderAndFiles(listFiles[i].toString(), filePathFileNameMap,newFileNameAfterRename, customAssert);
				} else if (listFiles[i].isFile()) {

					for (int j = 0; j < list.length; j++) {

						File beforeRename = listFiles[i];

						String newFileNameAfterRename1 = newFileNameAfterRename.split(".xlsx")[0] + randomFileNumber + ".xlsx";

						randomFileNumber = randomFileNumber + 1;

						File newFileAfterRename1 = new File(listFiles[i].toString().replace(list[j], newFileNameAfterRename1));

						beforeRename.renameTo(newFileAfterRename1);
						filePathFileNameMap.put(filePath, newFileNameAfterRename1);
					}
				}

			}
		} catch (Exception e) {
			logger.error("Exception while renaming files");
			customAssert.assertTrue(false, "Exception while renaming files");
		}

		return filePathFileNameMap;
	}

	private Boolean validateRawDataTab(int cSLId, int rawDataLineItemSizeExcel, String excelFilePath, String excelFileName, String sheetName, CustomAssert customAssert) {

		logger.info("Validating RawData Tab On CSL " + cSLId);
		Boolean validationStatus = true;

		try {
			HashMap<String, String> columnValuesMapExcel;
			HashMap<String, String> columnValuesMapApp;


			HashMap<Integer, HashMap<String, String>> rawDataLineItemRowValuesMap = createRawDataLineItemRowValuesMap(cSLId, rawDataLineItemSizeExcel, customAssert);
			if (rawDataLineItemRowValuesMap.size() == 0) {

				customAssert.assertTrue(false, "Either no row exists on Raw Data Tab of" +
						"CSL or Error while  creating rawDataLineItemRowValuesMap for CSL Id " + cSLId);
				validationStatus = false;
				return validationStatus;
			}

			HashMap<Integer, HashMap<String, String>> rawDataExcelRowValuesMap = createExcelRowValuesMap(excelFilePath, excelFileName, sheetName, customAssert);
			if (rawDataLineItemRowValuesMap.size() == 0) {

				customAssert.assertTrue(false, "Either no row exists in Raw Data Uploaded Excel" +
						"or Error while creating rawDataExcelRowValuesMap from Excel file " + excelFileName + "at " + excelFilePath);
				validationStatus = false;
				return validationStatus;
			}

			HashMap<String, String> columnContainingExtraValues = new HashMap<>();
			columnContainingExtraValues.put("ID", "false");
			columnContainingExtraValues.put("Active", "Yes");
			columnContainingExtraValues.put("Exception", "No");

			if (rawDataExcelRowValuesMap.size() == rawDataLineItemRowValuesMap.size()) {

				for (int i = 0; i < rawDataExcelRowValuesMap.size(); i++) {

					columnValuesMapExcel = rawDataExcelRowValuesMap.get(i + 2);
					columnValuesMapApp = rawDataLineItemRowValuesMap.get(i);

					validateMapKeyValuePairs(columnValuesMapExcel, columnValuesMapApp, columnContainingExtraValues, customAssert);


				}

			} else {
				customAssert.assertTrue(false, "Number of line Items on Raw Data Tab and Excel sheet are not equal for CSL ID " + cSLId);
			}

		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while validating Raw Data Tab on CSL ID " + cSLId);
			validationStatus = false;
		}
		return validationStatus;
	}

	private HashMap<Integer, HashMap<String, String>> createRawDataLineItemRowValuesMap(int cSLId, int rawDataLineItemSizeExcel, CustomAssert customAssert) {

		HashMap<Integer, HashMap<String, String>> rawDataLineItemRowValuesMap = new HashMap();

		try {
			String payload = "{\"offset\":0,\"size\":" + rawDataLineItemSizeExcel + ",\"childSlaId\":" + cSLId + "}";

			SLDetails slDetails = new SLDetails();
			slDetails.hitSLDetailsGlobalList(payload);
			String SideDetailsGlobalListResponse = slDetails.getSLDetailsResponseStr();

			if (!JSONUtility.validjson(SideDetailsGlobalListResponse)) {
				customAssert.assertTrue(false, "Side Details Global List Response is not valid Json");
				rawDataLineItemRowValuesMap.clear();
				return rawDataLineItemRowValuesMap;
			}

			JSONObject SideDetailsGlobalListResponseJson = new JSONObject(SideDetailsGlobalListResponse);
			JSONArray dataArray = SideDetailsGlobalListResponseJson.getJSONArray("data");
			JSONObject rawDataLineItemJson;
			JSONArray rawDataLineItemJsonArray;
			String columnName;
			String columnValue;

			HashMap<String, String> columnNameValueMap;

			//Creating a map of raw data tab values from screen
			for (int i = 0; i < dataArray.length(); i++) {

				rawDataLineItemJson = dataArray.getJSONObject(i);

				rawDataLineItemJsonArray = JSONUtility.convertJsonOnjectToJsonArray(rawDataLineItemJson);

				columnNameValueMap = new HashMap();
				for (int j = 0; j < rawDataLineItemJsonArray.length(); j++) {

					columnName = rawDataLineItemJsonArray.getJSONObject(j).get("columnName").toString().toUpperCase();
					columnValue = rawDataLineItemJsonArray.getJSONObject(j).get("columnValue").toString();
					columnNameValueMap.put(columnName, columnValue);
				}
				rawDataLineItemRowValuesMap.put(i, columnNameValueMap);
			}

		} catch (Exception e) {

			rawDataLineItemRowValuesMap.clear();
			return rawDataLineItemRowValuesMap;
		}
		return rawDataLineItemRowValuesMap;
	}

	private HashMap<Integer, HashMap<String, String>> createExcelRowValuesMap(String excelFilePath, String excelFileName, String sheetName, CustomAssert customAssert) {

		HashMap<Integer, HashMap<String, String>> createExcelRowValuesMap = new HashMap<>();
		HashMap<String, String> columnNameValueMap;

		try {
			int excelNumberOfRows = Integer.parseInt(XLSUtils.getNoOfRows(excelFilePath, excelFileName, sheetName).toString());
			List<String> excelColumnNames = XLSUtils.getExcelDataOfOneRow(excelFilePath, excelFileName, sheetName, 1);
			List<String> excelDataRowData;
			for (int excelRowNum = 2; excelRowNum <= excelNumberOfRows; excelRowNum++) {

				excelDataRowData = XLSUtils.getExcelDataOfOneRow(excelFilePath, excelFileName, sheetName, excelRowNum);

				if (excelColumnNames.size() == excelDataRowData.size()) {
					columnNameValueMap = new HashMap<>();
					for (int columnCount = 0; columnCount < excelColumnNames.size(); columnCount++) {

						columnNameValueMap.put(excelColumnNames.get(columnCount).toUpperCase(), excelDataRowData.get(columnCount));
					}

					createExcelRowValuesMap.put(excelRowNum, columnNameValueMap);
				} else {
					customAssert.assertTrue(false, "Excel Column Name Count Different from excel row data");
				}

			}
		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while creating excel Row Values Map " + e.getMessage());
			createExcelRowValuesMap.clear();
		}
		return createExcelRowValuesMap;
	}

	private Boolean validateMapKeyValuePairs(HashMap<String, String> columnValuesLargerMap, HashMap<String, String> columnValuesMapSmallerMap,
											 HashMap<String, String> extraValuesMap, CustomAssert customAssert) {

		Boolean validationStatus = true;

		String keyFromLargerMap;
		String valueFromSmallerMap;
		String valueFromLargerMap;
		String valueFromExtraValuesMap;

		try {
			for (Map.Entry<String, String> entry : columnValuesLargerMap.entrySet()) {

				keyFromLargerMap = entry.getKey();
				valueFromLargerMap = entry.getValue();

				if (columnValuesMapSmallerMap.containsKey(keyFromLargerMap)) {
					valueFromSmallerMap = columnValuesMapSmallerMap.get(keyFromLargerMap);

					if (!valueFromLargerMap.equalsIgnoreCase(valueFromSmallerMap)) {
						customAssert.assertTrue(false, "Expected value of " + keyFromLargerMap + " is not equal to Actual Value");
						customAssert.assertTrue(false, "Value from largerMap " + valueFromLargerMap + " Value from SmallerMap " + valueFromSmallerMap);
						validationStatus = false;
					}

				} else if (extraValuesMap.containsKey(keyFromLargerMap)) {

					valueFromExtraValuesMap = extraValuesMap.get(keyFromLargerMap);

					if (!valueFromLargerMap.equalsIgnoreCase(valueFromExtraValuesMap)) {
						customAssert.assertTrue(false, "Expected value of " + keyFromLargerMap + " is not equal to Actual Value");
						customAssert.assertTrue(false, "Value from largerMap " + valueFromLargerMap + " Value from ExtraFields " + valueFromExtraValuesMap);
						validationStatus = false;
					}
				} else {
					customAssert.assertTrue(false, "Key " + keyFromLargerMap + " Not found in Excel Column Values Map Or Extra Values Map");
				}

			}
		} catch (Exception e) {
			validationStatus = false;
		}

		return validationStatus;
	}

	//    C10746
	private Boolean validateAddException(int cSLID, int numberOfLineItems, CustomAssert customAssert) {

		logger.info("Validating Add Exception Functionality");

		Boolean validationStatus = true;
		String[] objectIdList;
		SLDetails slDetails = new SLDetails();
		try {

			objectIdList = getListOfSLObjectIds(cSLID, numberOfLineItems, customAssert);
			if (objectIdList.length == 0) {

				customAssert.assertTrue(false, "List Of Object Ids is zero");
			} else if (objectIdList.length != numberOfLineItems) {
				customAssert.assertTrue(false, "Number of Object Ids is not equal to expected number of line items");
			}

			logger.info("Preparing Payload for hitting Add Exception API");

			String objectIds = "";
			for (int i = 0; i < objectIdList.length; i++) {
				if (i == objectIdList.length - 1) {
					objectIds += "\"" + objectIdList[i] + "\"";
				} else {
					objectIds += "\"" + objectIdList[i] + "\"" + ",";
				}
			}

			String payLoad = "{\"filterQuery\":{\"name\":\"filterQuery\",\"values\":\"{  \\\"query\\\": {    \\\"bool\\\": {      \\\"filter\\\": [        {          \\\"bool\\\": {" +
					"            \\\"must\\\": []          }        }      ]    }  }}\"}," +
					"\"childSlaId\":{\"name\":\"childSlaId\",\"values\":" + cSLID + "},\"objectIds\":{\"name\":\"objectIds\",\"values\":[" +
					objectIds + "]}," +
					"\"comment\":{\"requestedBy\":{\"name\":\"" +
					"requestedBy\",\"id\":12244,\"multiEntitySupport\":false},\"shareWithSupplier\":" +
					"{\"name\":\"shareWithSupplier\",\"id\":12409,\"multiEntitySupport\":false}," +
					"\"comments\":{\"name\":\"comments\",\"id\":86,\"multiEntitySupport\":false,\"values\":\"\"}," +
					"\"documentTags\":{\"name\":\"documentTags\",\"id\":12428,\"multiEntitySupport\":false}," +
					"\"draft\":{\"name\":\"draft\",\"multiEntitySupport\":false}," +
					"\"actualDate\":{\"name\":\"actualDate\",\"id\":12243,\"multiEntitySupport\":false}," +
					"\"privateCommunication\":{\"name\":\"privateCommunication\",\"id\":12242,\"multiEntitySupport\":false,\"values\":false}," +
					"\"changeRequest\":{\"name\":\"changeRequest\",\"id\":12246,\"multiEntitySupport\":false}," +
					"\"workOrderRequest\":{\"name\":\"workOrderRequest\",\"id\":12247,\"multiEntitySupport\":false},\"commentDocuments\":{\"values\":[]}}}";

			slDetails.hitAddException(payLoad);
			Thread.sleep(5000);
			JSONObject addExceptionResponse = new JSONObject(slDetails.getSLDetailsResponseStr());
			String errorMsg = addExceptionResponse.getJSONArray("errorMessages").getJSONObject(0).get("errorMessage").toString();

			if (!errorMsg.equalsIgnoreCase("Operation Successful!")) {

				customAssert.assertTrue(false, "Expected Error Message : Operation Successful! || Actual Error message" +
						errorMsg);
				validationStatus = false;
			}
			String globalListPayload = "{\"offset\":0,\"size\":" + numberOfLineItems + ",\"childSlaId\":" + cSLID + "}";
			slDetails.hitSLDetailsGlobalList(globalListPayload);
			String SLDetailsResponse = slDetails.getSLDetailsResponseStr();

			if (!validateSpecificColumnValue(SLDetailsResponse, "Exception", "Yes", customAssert)) {
				customAssert.assertTrue(false, "Exception Values validated unsuccessfully for CSL Id for Add Exception Case" + cSLID);
				validationStatus = false;
			}

		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while Validating Add Exception Scenario " + e.getMessage());
			validationStatus = false;
		}

		return validationStatus;
	}

	//    C10746
	private Boolean validateRemoveException(int cSLID, int numberOfLineItems, CustomAssert customAssert) {

		logger.info("Validating Remove Exception Functionality");

		Boolean validationStatus = true;
		String[] objectIdList;
		SLDetails slDetails = new SLDetails();
		try {

			objectIdList = getListOfSLObjectIds(cSLID, numberOfLineItems, customAssert);
			if (objectIdList.length == 0) {

				customAssert.assertTrue(false, "List Of Object Ids is zero");
			} else if (objectIdList.length != numberOfLineItems) {
				customAssert.assertTrue(false, "Number of Object Ids is not equal to expected number of line items");
			}

			logger.info("Preparing Payload for hitting Remove Exception API");

			String objectIds = "";
			for (int i = 0; i < objectIdList.length; i++) {
				if (i == objectIdList.length - 1) {
					objectIds += "\"" + objectIdList[i] + "\"";
				} else {
					objectIds += "\"" + objectIdList[i] + "\"" + ",";
				}
			}

			String payLoad = "{\"filterQuery\": {\"name\": \"filterQuery\",\"values\": \"{\\\"query\\\":{\\\"bool\\\": {\\\"filter\\\": []}}}\"},\"childSlaId\": {\"name\": " +
					"\"childSlaId\",\"values\": " + cSLID + "},\"objectIds\": {\"name\": \"objectIds\"," +
					"\"values\": [" + objectIds + "]},\"comment\": {}}";

			slDetails.hitRemoveException(payLoad);
			Thread.sleep(5000);
			JSONObject addExceptionResponse = new JSONObject(slDetails.getSLDetailsResponseStr());
			String errorMsg = addExceptionResponse.getJSONArray("errorMessages").getJSONObject(0).get("errorMessage").toString();

			if (!errorMsg.equalsIgnoreCase("Operation Successful!")) {

				customAssert.assertTrue(false, "Expected Error Message : Operation Successful! || Actual Error message" +
						errorMsg);
				validationStatus = false;
			}
			String globalListPayload = "{\"offset\":0,\"size\":" + numberOfLineItems + ",\"childSlaId\":" + cSLID + "}";
			slDetails.hitSLDetailsGlobalList(globalListPayload);
			String SLDetailsResponse = slDetails.getSLDetailsResponseStr();

			if (!validateSpecificColumnValue(SLDetailsResponse, "Exception", "No", customAssert)) {
				customAssert.assertTrue(false, "Exception Values validated unsuccessfully for CSL Id for Remove Exception Case" + cSLID);
				validationStatus = false;
			}

		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while Validating Remove Exception Scenario " + e.getMessage());
			validationStatus = false;
		}

		return validationStatus;
	}

	private Boolean validateInclude(int cSLID, int numberOfLineItems, CustomAssert customAssert) {

		logger.info("Validating Include Functionality");

		Boolean validationStatus = true;
		String[] objectIdList;
		SLDetails slDetails = new SLDetails();
		try {

			objectIdList = getListOfSLObjectIds(cSLID, numberOfLineItems, customAssert);
			if (objectIdList.length == 0) {

				customAssert.assertTrue(false, "List Of Object Ids is zero");
			} else if (objectIdList.length != numberOfLineItems) {
				customAssert.assertTrue(false, "Number of Object Ids is not equal to expected number of line items");
			}

			logger.info("Preparing Payload for hitting update details API");

			String objectIds = "";
			for (int i = 0; i < objectIdList.length; i++) {
				if (i == objectIdList.length - 1) {
					objectIds += "\"" + objectIdList[i] + "\"";
				} else {
					objectIds += "\"" + objectIdList[i] + "\"" + ",";
				}
			}

			String payLoad = "{\"filterQuery\": {\"name\": \"filterQuery\",\"values\": \"{\\\"query\\\":{\\\"bool\\\": {\\\"filter\\\": []}}}\"},\"childSlaId\": {\"name\": " +
					"\"childSlaId\",\"values\": " + cSLID + "},\"objectIds\": {\"name\": \"objectIds\"," +
					"\"values\": [" + objectIds + "]},\"comment\": {}}";

			slDetails.hitUpdateData(payLoad);
			Thread.sleep(5000);
			JSONObject addExceptionResponse = new JSONObject(slDetails.getSLDetailsResponseStr());
			String errorMsg = addExceptionResponse.getJSONArray("errorMessages").getJSONObject(0).get("errorMessage").toString();

			if (!errorMsg.equalsIgnoreCase("Operation Successful!")) {

				customAssert.assertTrue(false, "Expected Error Message : Operation Successful! || Actual Error message" +
						errorMsg);
				validationStatus = false;
			}
			String globalListPayload = "{\"offset\":0,\"size\":" + numberOfLineItems + ",\"childSlaId\":" + cSLID + "}";
			slDetails.hitSLDetailsGlobalList(globalListPayload);
			String SLDetailsResponse = slDetails.getSLDetailsResponseStr();

			if (!validateSpecificColumnValue(SLDetailsResponse, "Active", "Yes", customAssert)) {
				customAssert.assertTrue(false, "Include Values validated unsuccessfully for CSL Id for Include" + cSLID);
				validationStatus = false;
			}

		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while Validating Include Exclude Scenario " + e.getMessage());
			validationStatus = false;
		}

		return validationStatus;
	}

	private Boolean validateExclude(int cSLID, int numberOfLineItems, CustomAssert customAssert) {

		logger.info("Validating Exclude Functionality");

		Boolean validationStatus = true;
		String[] objectIdList;
		SLDetails slDetails = new SLDetails();
		try {

			objectIdList = getListOfSLObjectIds(cSLID, numberOfLineItems, customAssert);
			if (objectIdList.length == 0) {

				customAssert.assertTrue(false, "List Of Object Ids is zero");
			} else if (objectIdList.length != numberOfLineItems) {
				customAssert.assertTrue(false, "Number of Object Ids is not equal to expected number of line items");
			}

			logger.info("Preparing Payload for hitting Exclude API");

			String objectIds = "";
			for (int i = 0; i < objectIdList.length; i++) {
				if (i == objectIdList.length - 1) {
					objectIds += "\"" + objectIdList[i] + "\"";
				} else {
					objectIds += "\"" + objectIdList[i] + "\"" + ",";
				}
			}

			String payLoad = "{\"filterQuery\": {\"name\": \"filterQuery\",\"values\": \"{\\\"query\\\":{\\\"bool\\\": {\\\"filter\\\": []}}}\"},\"childSlaId\": {\"name\": " +
					"\"childSlaId\",\"values\": " + cSLID + "},\"objectIds\": {\"name\": \"objectIds\"," +
					"\"values\": [" + objectIds + "]},\"comment\": {}}";

			slDetails.hitRemoveNoise(payLoad);
			Thread.sleep(5000);
			JSONObject addExceptionResponse = new JSONObject(slDetails.getSLDetailsResponseStr());
			String errorMsg = addExceptionResponse.getJSONArray("errorMessages").getJSONObject(0).get("errorMessage").toString();

			if (!errorMsg.equalsIgnoreCase("Operation Successful!")) {

				customAssert.assertTrue(false, "Expected Error Message : Operation Successful! || Actual Error message" +
						errorMsg);
				validationStatus = false;
			}
			String globalListPayload = "{\"offset\":0,\"size\":" + numberOfLineItems + ",\"childSlaId\":" + cSLID + "}";
			slDetails.hitSLDetailsGlobalList(globalListPayload);
			String SLDetailsResponse = slDetails.getSLDetailsResponseStr();

			if (!validateSpecificColumnValue(SLDetailsResponse, "Active", "No", customAssert)) {
				customAssert.assertTrue(false, "Exclude Values validated unsuccessfully for CSL Id " + cSLID);
				validationStatus = false;
			}

		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while Validating Include Exclude Scenario " + e.getMessage());
			validationStatus = false;
		}

		return validationStatus;
	}

	private Boolean validateDownloadRawData(String cSLID, String filterQuery, String expectedFilterQueryExcel,
											int startingRecordForRawData, CustomAssert customAssert) {

		logger.info("Validating Raw Data Download Functionality on Raw Data Tab of CSL ID " + cSLID);
		Boolean validationStatus = true;

		String downloadRawDataFileName;

		Show show = new Show();
		show.hitShowVersion2(cslEntityTypeId, Integer.parseInt(cSLID));
		String showResponse = show.getShowJsonStr();

		String shortCodeId = ShowHelper.getValueOfField("short code id", showResponse);
		String title = ShowHelper.getValueOfField("description", showResponse);

		String date = DateUtils.getCurrentDateInDDMMMYYYY();
		date = date.replace("/", "");
		downloadRawDataFileName = shortCodeId + "_PerformanceData_" + date + ".csv";
		String outputFilePath = downloadFilePath + "/" + downloadRawDataFileName;

		Map<String, String> formDataMap = new HashMap<>();
		try {

			formDataMap.put("_csrf_token", "");
			formDataMap.put("filterQuery", filterQuery);
			formDataMap.put("childSlaId", cSLID);

			SLDetails slDetails = new SLDetails();
			slDetails.hitDownloadRawData(outputFilePath, formDataMap);

			Thread.sleep(5000);

			if (!FileUtils.fileExists(downloadFilePath, downloadRawDataFileName)) {
				customAssert.assertTrue(false, "Downloaded Raw File " + downloadRawDataFileName + " does not exists");
				validationStatus = false;
				return validationStatus;
			} else {
				List<String> fileLines = org.apache.commons.io.FileUtils.readLines(new File(outputFilePath), "UTF-8");

				String expectedCSLIDString = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatadownloadflow", "csdidstring");
				String expectedCSLTitleString = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatadownloadflow", "csltitlestring");
				String expectedGenByString = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatadownloadflow", "generatedbystring");
				String expectedGenOnString = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatadownloadflow", "generatedonstring");
				String expectedFiltersAppliedString = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "rawdatadownloadflow", "filtersstring");

				String[] firstLine = fileLines.get(0).split(",");

				String firstLineFirstColumn = firstLine[0];
				String firstLineSecondColumn = firstLine[1];

				if (!firstLineFirstColumn.equalsIgnoreCase("\"" + expectedCSLIDString + "\"")) {
					customAssert.assertTrue(false, "First Row and First Column of the excel didn't match with the expected value");
				}
				String expectedFirstLineSecondColumnRawDataFile = "";
				try {
					expectedFirstLineSecondColumnRawDataFile = shortCodeId.split("CSL")[1];
					expectedFirstLineSecondColumnRawDataFile = "CSL0" + expectedFirstLineSecondColumnRawDataFile;
				} catch (Exception e) {
					logger.info("Exception occur when splitting short code id with 'CSL'  substring");
				}
				if (!((firstLineSecondColumn.equalsIgnoreCase("\"" + shortCodeId + "\"")) || (firstLineSecondColumn.equalsIgnoreCase("\"" + expectedFirstLineSecondColumnRawDataFile + "\"")))) {
					customAssert.assertTrue(false, "In Download Raw Data File First Row and Second Column of the excel didn't match with the expected value for CSL " + shortCodeId);
					customAssert.assertTrue(false, "Expected value : " + shortCodeId + " Actual Value " + firstLineSecondColumn);
				}

				String[] secondLine = fileLines.get(1).split(",");
				String secondLineFirstColumn = secondLine[0];
				String secondLineSecondColumn = secondLine[1];

				if (!secondLineFirstColumn.equalsIgnoreCase("\"" + expectedCSLTitleString + "\"")) {
					customAssert.assertTrue(false, "Second Row and First Column of the excel didn't match with the expected value");
				}

				if (!secondLineSecondColumn.equalsIgnoreCase("\"" + title + "\"")) {
					customAssert.assertTrue(false, "Second Row and Second Column of the excel didn't match with the expected value");
				}

				String[] thirdLine = fileLines.get(2).split(",");
				String thirdLineFirstColumn = thirdLine[0];
				String thirdLineSecondColumn = thirdLine[1];

				if (!thirdLineFirstColumn.equalsIgnoreCase("\"" + expectedGenByString + "\"")) {
					customAssert.assertTrue(false, "Third Row and First Column of the excel didn't match with the expected value");
				}

				if (!thirdLineSecondColumn.equalsIgnoreCase("\"" + auditLogUser + "\"")) {
					customAssert.assertTrue(false, "Third Row and Second Column of the excel didn't match with the expected value");
				}

				String[] fourthLine = fileLines.get(3).split(",");
				String fourthLineFirstColumn = fourthLine[0];
				String fourthLineSecondColumn = fourthLine[1];

				if (!fourthLineFirstColumn.equalsIgnoreCase("\"" + expectedGenOnString + "\"")) {
					customAssert.assertTrue(false, "Forth Row and First Column of the excel didn't match with the expected value");
				}

				if (!fourthLineSecondColumn.contains(DateUtils.getCurrentDateInDD_MM_YYYY())) {
					customAssert.assertTrue(false, "Forth Row and Second Column of the excel didn't match with the expected value");
				}

				String[] sixthLine = fileLines.get(5).split(",");
				String sixthLineSecondColumn = "";
				String sixthLineFirstColumn = sixthLine[0];
				if (!expectedFilterQueryExcel.equalsIgnoreCase("")) {
					sixthLineSecondColumn = sixthLine[1];
					if (!sixthLineSecondColumn.equalsIgnoreCase("\"" + expectedFiltersAppliedString + "\"")) {
						customAssert.assertTrue(false, "Sixth Row and Second Column of the excel didn't match with the expected value");
					}
				}

				int numberOfRecordsToValidate = 7;

				String payload = "{\"offset\":0,\"size\":" + numberOfRecordsToValidate + ",\"childSlaId\":" + cSLID + "}";

				slDetails.hitSLDetailsGlobalList(payload);
				String slDetailsResponse = slDetails.getSLDetailsResponseStr();

				JSONArray dataArray = new JSONObject(slDetailsResponse).getJSONArray("data");
				JSONArray indvDataJsonArray;
				String columnName;
				String columnValue;
				List<String[]> dataArrayRowList = new ArrayList<>();
				String[] dataArrayColList;
				int sortOrder;
				for (int i = 0; i < dataArray.length(); i++) {
					dataArrayColList = new String[50];
					indvDataJsonArray = JSONUtility.convertJsonOnjectToJsonArray(dataArray.getJSONObject(i));

					for (int j = 0; j < indvDataJsonArray.length(); j++) {
						columnName = indvDataJsonArray.getJSONObject(j).getString("columnName");
						if (columnName.equalsIgnoreCase("ID")) {
							continue;
						}
						columnValue = indvDataJsonArray.getJSONObject(j).get("columnValue").toString();
						sortOrder = Integer.parseInt(indvDataJsonArray.getJSONObject(j).get("sortOrder").toString());
						dataArrayColList[sortOrder] = columnValue;
					}
					dataArrayRowList.add(dataArrayColList);
				}
				int dataArrayStartIndex = 0;

				String[] excelColValues;
				for (int i = startingRecordForRawData; i < startingRecordForRawData + numberOfRecordsToValidate; i++) {

					dataArrayColList = dataArrayRowList.get(dataArrayStartIndex);
					excelColValues = fileLines.get(i).split("\",");

					for (int j = 0; j < excelColValues.length; j++) {
						if (dataArrayColList[j + 2] != null) {
							if (dataArrayColList[j + 2].contains("\"")) {
								dataArrayColList[j + 2] = dataArrayColList[j + 2].replaceAll("\"", "\"\"");
							}
							if (j == excelColValues.length - 1) {
								if (!excelColValues[j].equalsIgnoreCase("\"" + dataArrayColList[j + 2] + "\"")) {
									customAssert.assertTrue(false, "Downloaded Excel Value and Value on Screen are not equal for row " +
											i + " and column " + j);
									validationStatus = false;
								}
							} else {
								if (!excelColValues[j].equalsIgnoreCase("\"" + dataArrayColList[j + 2])) {
									customAssert.assertTrue(false, "Downloaded Excel Value and Value on Screen are not equal for row " +
											i + " and column " + j);
									validationStatus = false;
								}
							}
						} else {
							logger.debug("dataArrayColList" + (j + 2) + "is null");
						}
					}
					dataArrayStartIndex++;
				}
			}
		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while validating Raw Data Download Functionality on Raw Data Tab of CSL ID " + cSLID);
			validationStatus = false;
		} finally {
			FileUtils.deleteFile(outputFilePath);
		}

		return validationStatus;

	}

	private String[] getListOfSLObjectIds(int cSLID, int numberOfLineItems, CustomAssert customAssert) {

		JSONObject slDetailsResponseJson;
		JSONArray dataArray;
		JSONArray lineItemColumnArray;

		String columnName;
		String columnValue;
		String[] objectIdList = new String[numberOfLineItems];
		try {
			SLDetails slDetails = new SLDetails();
			String globalListPayload = "{\"offset\":0,\"size\":" + numberOfLineItems + ",\"childSlaId\":" + cSLID + "}";
			slDetails.hitSLDetailsGlobalList(globalListPayload);
			String slDetailsResponse = slDetails.getSLDetailsResponseStr();

			if (JSONUtility.validjson(slDetailsResponse)) {
				slDetailsResponseJson = new JSONObject(slDetailsResponse);
				dataArray = slDetailsResponseJson.getJSONArray("data");
				for (int i = 0; i < numberOfLineItems; i++) {

					lineItemColumnArray = JSONUtility.convertJsonOnjectToJsonArray(dataArray.getJSONObject(i));

					innerLoop:
					for (int j = 0; j < lineItemColumnArray.length(); j++) {
						columnName = lineItemColumnArray.getJSONObject(j).get("columnName").toString();
						columnValue = lineItemColumnArray.getJSONObject(j).get("columnValue").toString();

						if (columnName.equalsIgnoreCase("ID")) {
							objectIdList[i] = columnValue.split(":;")[1];

							break innerLoop;
						}
					}
				}
			} else {
				customAssert.assertTrue(false, "SL Details Response is not a valid Json");
			}
		} catch (Exception e) {

			customAssert.assertTrue(false, "Exception while getting Object Id List " + e.getMessage());
		}
		return objectIdList;
	}

	private Boolean validateSpecificColumnValue(String globalListResponse, String expColumnName, String expColumnValue, CustomAssert customAssert) {

		Boolean validationStatus = true;
		JSONObject slDetailsResponseJson = new JSONObject(globalListResponse);
		JSONArray dataArray = slDetailsResponseJson.getJSONArray("data");
		JSONArray lineItemColumnArray;
		String columnName;
		String columnValue;
		for (int i = 0; i < dataArray.length(); i++) {

			lineItemColumnArray = JSONUtility.convertJsonOnjectToJsonArray(dataArray.getJSONObject(i));

			innerLoop:
			for (int j = 0; j < lineItemColumnArray.length(); j++) {
				columnName = lineItemColumnArray.getJSONObject(j).get("columnName").toString();

				if (columnName.equalsIgnoreCase(expColumnName)) {
					if (columnName.equalsIgnoreCase("ID")) {
						columnValue = lineItemColumnArray.getJSONObject(j).get("columnValue").toString().split(":;")[0];
					} else {
						columnValue = lineItemColumnArray.getJSONObject(j).get("columnValue").toString();
					}
					if (!expColumnValue.equalsIgnoreCase(columnValue)) {
						customAssert.assertTrue(false, "Expected Value For Column " + expColumnName + ":" +
								expColumnValue + "and row " + i + "while Actual value : " + columnValue);
						validationStatus = false;
					}
					break;
				}
			}
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

	//    C10615 C10616 C10634 C10643
	private Boolean validateIgnoreFileScenario(int cSLId, CustomAssert customAssert) {

		logger.info("Validating Ignore Files Scenario");

		Boolean validationStatus = true;
		Long waitForIgnoreTimeOut = 120000L;
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

				Thread.sleep(120000);
				//Checking Ignored Case
				structurePerformanceData = getStructuredPerformanceData(cSLId);
				structuredPerformanceDataFileDetailsJson = new JSONObject(structurePerformanceData).getJSONArray("data").getJSONObject(0);
				dataArray = JSONUtility.convertJsonOnjectToJsonArray(structuredPerformanceDataFileDetailsJson);

				for (int i = 0; i < dataArray.length(); i++) {

					columnName = dataArray.getJSONObject(i).get("columnName").toString();

					if (columnName.equalsIgnoreCase("status")) {
						statusValue = dataArray.getJSONObject(i).get("value").toString();
						if (!statusValue.equalsIgnoreCase("Ignored:;false")) {
							customAssert.assertTrue(false, "Status For File Ignore Scenario is not as Expected for CSL ID " + cSLId);
							customAssert.assertTrue(false, "Expected : " + "Ignored:;false" + " Actual : " + statusValue);
							validationStatus = false;
						}
						break;
					}
				}
				//Verifying Audit Log After Ignoring Files
//				if (!(verifyAuditLog(cslEntityTypeId, cSLId, "Performance Data Ignored", auditLogUser, customAssert))) {
//					customAssert.assertTrue(false, "Audit Log validated unsuccessfully after Ignore Files Functionality");
//				}

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

	//    C10652
	private Boolean validateViewTemplate(int cslID, List<List<String>> viewTemplateListExcel, CustomAssert customAssert) {

		logger.info("Validating View template on CSL Id " + cslID);
		Boolean validationStatus = true;
		try {

			SLDetails slDetails = new SLDetails();
			String payload = "{\"filterMap\":{\"entityTypeId\":1,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
			slDetails.hitViewTemplate(cslID, payload);
			String slDetailsResponse = slDetails.getSLDetailsResponseStr();

			if (JSONUtility.validjson(slDetailsResponse)) {

				JSONObject slDetailsResponseJson = new JSONObject(slDetailsResponse);
				JSONArray dataArray = slDetailsResponseJson.getJSONArray("data");
				JSONArray indvRowArray;

				String columnValue;

				List<String> indvRowColumnValuesList;
				List<List<String>> viewTemplateOnScreenDataList = new ArrayList<>();

				if (viewTemplateListExcel == null) {
					if (dataArray.length() == 0) {
						customAssert.assertTrue(true, "When no template is uploaded on service level then view template has no data");
						return true;
					} else {
						customAssert.assertTrue(false, "When no template is uploaded on service level then view template has data on child service level on pressing view template button for CSL ID " + cslID);
						return false;
					}
				}

				for (int i = 0; i < dataArray.length(); i++) {

					indvRowArray = JSONUtility.convertJsonOnjectToJsonArray(dataArray.getJSONObject(i));
					indvRowColumnValuesList = new ArrayList();
					for (int j = 0; j < indvRowArray.length(); j++) {

						columnValue = indvRowArray.getJSONObject(j).get("columnValue").toString();
						if (columnValue.equalsIgnoreCase("null")) {
							columnValue = "-";
						}
						indvRowColumnValuesList.add(columnValue);
					}
					viewTemplateOnScreenDataList.add(indvRowColumnValuesList);
				}

				//Validating List Prepared from On Screen With the view Template from Excel
				List<String> excelListIndvRow;
				List<String> screenListIndvRow;

				if (viewTemplateOnScreenDataList.size() == viewTemplateListExcel.size()) {
					for (int i = 0; i < viewTemplateOnScreenDataList.size(); i++) {

						excelListIndvRow = viewTemplateListExcel.get(i);
						screenListIndvRow = viewTemplateOnScreenDataList.get(i);

						if (excelListIndvRow.size() == screenListIndvRow.size()) {

							for (int j = 0; j < excelListIndvRow.size(); j++) {

								if (!excelListIndvRow.get(j).equalsIgnoreCase(screenListIndvRow.get(j))) {
									customAssert.assertTrue(false, "Value from template and Value on Screen are not equal for row number " + i + " and column number " + j);
									customAssert.assertTrue(false, "Template value from Excel " + excelListIndvRow.get(j) + "Template Value On Screen " + screenListIndvRow.get(j));
									validationStatus = false;
								}
							}
						} else {
							customAssert.assertTrue(false, "Number of columns from excel and On Template Format Screen are unequal for CSL ID " + cslID);
							validationStatus = false;
						}
					}
				} else {
					customAssert.assertTrue(false, "Number of Rows from excel and On Template Format Screen are unequal for CSL ID " + cslID);
					validationStatus = false;
				}

			} else {
				customAssert.assertTrue(false, "API Response for View Template is not a valid Json for CSL ID " + cslID);
				validationStatus = false;
				return validationStatus;
			}

		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while validating view template on CSL ID " + cslID + e.getMessage());
			validationStatus = false;
		}

		return validationStatus;
	}

	private List<List<String>> prepareViewTemplateListExcel(String excelFilePath, String excelFileName, String sheetName, int startingRowNum,int numberOfColumns, CustomAssert customAssert) {

		logger.info("Preparing excel List for View Template validation");
		List<List<String>> viewTemplateListExcel = new ArrayList<>();

		try {
			int numOfRows = Integer.parseInt(XLSUtils.getNoOfRows(excelFilePath, excelFileName, sheetName).toString());
			viewTemplateListExcel = XLSUtils.getExcelDataOfMultipleRowsWithNullAsHyphenInAnyColumn(excelFilePath, excelFileName, sheetName, startingRowNum, numOfRows,numberOfColumns);
		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while preparing View Template List From Excel FileName : " + excelFileName);
			viewTemplateListExcel.clear();
		}
		return viewTemplateListExcel;
	}

	private Boolean validateTargetValuesCsl(int cslID, HashMap<String, String> targetValuesMap, CustomAssert customAssert) {

		logger.info("Validating Target Values on entity " + cslID);

		Boolean validationStatus = true;
		try {
			Show show = new Show();
			show.hitShowVersion2(cslEntityTypeId, cslID);

			String showResponse = show.getShowJsonStr();
			String fieldName;
			String fieldValueExpected;
			String fieldValueActual;
			for (Map.Entry<String, String> entry : targetValuesMap.entrySet()) {

				fieldName = entry.getKey();
				fieldValueExpected = entry.getValue();
				fieldValueActual = ShowHelper.getValueOfField(fieldName, showResponse);

				if (fieldValueExpected.equalsIgnoreCase("null")) {
					continue;
				}

				if (!(fieldValueActual.equalsIgnoreCase(fieldValueExpected))) {
					customAssert.assertTrue(false, "Expected and Actual Value for field name " + fieldName +
							" mismatch for CSL " + cslID + " Actual Value : " + fieldValueActual +
							" Expected Value : " + fieldValueExpected);

					validationStatus = false;
				}
			}

		} catch (Exception e) {

			customAssert.assertTrue(false, "Exception while validating target fields on CSL " + cslID + e.getMessage());
			validationStatus = false;
		}
		return validationStatus;
	}

	private Boolean updateFinalPerformanceValues(int cslID, String finalNumerator, String finalDenominator, String finalPerformance, CustomAssert customAssert) {

		logger.info("Updating final performance values");
		Boolean updateStatus = true;
		Edit edit = new Edit();
		String editResponse;
		JSONObject editResponseJson;
		try {
			editResponse = edit.hitEdit("child service levels", cslID);

			editResponseJson = new JSONObject(editResponse);
			editResponseJson.remove("header");
			editResponseJson.remove("session");
			editResponseJson.remove("actions");
			editResponseJson.remove("createLinks");
			editResponseJson.getJSONObject("body").remove("layoutInfo");
			editResponseJson.getJSONObject("body").remove("globalData");
			editResponseJson.getJSONObject("body").remove("errors");
			editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("finalNumerator").put("values", finalNumerator);
			editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("finalDenominator").put("values", finalDenominator);
			editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("finalPerformance").put("values", finalPerformance);

			editResponse = edit.hitEdit("child service levels", editResponseJson.toString());

			if (!editResponse.contains("success")) {
				customAssert.assertTrue(false, "Child service level updated unsuccessfully");
				updateStatus = false;
			}
		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while updating Final Performance values on CSL ID " + cslID);
			updateStatus = false;
		}
		return updateStatus;
	}

	//  C10634
	private Boolean validateAuditLogWithHistory(int entityTypeId, int entityId, String actionNameAuditLog, List<Map<String, String>> viewHistoryMapList,String userName, CustomAssert customAssert) {

		Boolean validationStatus = true;
		int auditLogTabId = 61;
		TabListData tabListData = new TabListData();
		TblauditlogsFieldHistory tblauditlogsFieldHistory = new TblauditlogsFieldHistory();

		String tabListResponse;
		Map<String, String> viewHistoryMap;
		String columnName = "";
		String columnValue = "";
		try {

			Thread.sleep(60000);
			String payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
			tabListData.hitTabListData(auditLogTabId, entityTypeId, entityId, payload);
			tabListResponse = tabListData.getTabListDataResponseStr();

			JSONObject tabListResponseJson = new JSONObject(tabListResponse);

			JSONArray dataArray = tabListResponseJson.getJSONArray("data");

			if(dataArray.length() == 0){
				customAssert.assertTrue(false,"No entry exists in Audit Log Tab for entity id " + entityId + " entity type id " + entityTypeId);
				return false;
			}
			JSONObject auditLogRowJson = dataArray.getJSONObject(0);

			JSONArray auditLogRowJsonArray = JSONUtility.convertJsonOnjectToJsonArray(auditLogRowJson);

			for (int i = 0; i < auditLogRowJsonArray.length(); i++) {

				columnName = auditLogRowJsonArray.getJSONObject(i).get("columnName").toString();
				columnValue = auditLogRowJsonArray.getJSONObject(i).get("value").toString();

				if (columnName.equalsIgnoreCase("action_name")) {
					if (!columnValue.equalsIgnoreCase(actionNameAuditLog)) {
						customAssert.assertTrue(false, "Audit Log Expected Action : " + actionNameAuditLog
								+ " Actual Action : " + columnValue);
						validationStatus = false;
					}
				}

//				if (columnName.equalsIgnoreCase("requested_by") || columnName.equalsIgnoreCase("completed_by")) {
//					if (!columnValue.equalsIgnoreCase(userName)) {
//						customAssert.assertTrue(false, "Audit Log Expected " + columnName + ": " + auditLogUser
//								+ " Actual " + columnValue + ": " + columnValue);
//						validationStatus = false;
//					}
//				}

				if (columnName.equalsIgnoreCase("history")) {

					tblauditlogsFieldHistory.hitTblauditlogsFieldHistoryPage(columnValue);
					String viewHistoryResponse = tblauditlogsFieldHistory.getTblAuditLogsFieldHistoryResponseStr();

					JSONObject viewHistoryResponseJson = new JSONObject(viewHistoryResponse);
					JSONArray valueArray = viewHistoryResponseJson.getJSONArray("value");
					for (int j = 0; i < valueArray.length(); j++) {

						viewHistoryMap = viewHistoryMapList.get(j);

						JSONObject viewHistoryDetailsJson = valueArray.getJSONObject(j);

						String oldValueActual = viewHistoryDetailsJson.get("oldValue").toString();
						String oldValueExpected = viewHistoryMap.get("oldValue");

						if (!oldValueExpected.equalsIgnoreCase(oldValueActual)) {
							customAssert.assertTrue(false, "Expected and Actual Value for view History option oldValue mismatch");
							customAssert.assertTrue(false, "Expected Value for oldValue : " + oldValueExpected + "and Actual Value for oldValue : " + oldValueActual);
							validationStatus = false;
						}
						String newValueActual = viewHistoryDetailsJson.get("newValue").toString();
						String newValueExpected = viewHistoryMap.get("newValue");

						String newValueActual_DoubleString = "";

						try{
							if(newValueActual !=null) {
								newValueActual_DoubleString = String.valueOf(Double.parseDouble(newValueActual));
							}
						}catch (Exception e){
							logger.error("Exception while parsing newValueActual for Double type value");
						}

						if (!(newValueExpected.equalsIgnoreCase(newValueActual) || newValueExpected.equalsIgnoreCase(newValueActual_DoubleString))) {
							customAssert.assertTrue(false, "Expected and Actual Value for view History option newValue mismatch");
							customAssert.assertTrue(false, "Expected Value for newValue : " + newValueExpected + "and Actual Value for newValue : " + newValueActual);
							validationStatus = false;
						}

						String propertyActual = viewHistoryDetailsJson.get("property").toString();
						String propertyExpected = viewHistoryMap.get("property");

						if (!propertyExpected.equalsIgnoreCase(propertyActual)) {
							customAssert.assertTrue(false, "Expected and Actual Value for view History option property mismatch");
							customAssert.assertTrue(false, "Expected Value for property : " + propertyExpected + "and Actual Value for property : " + propertyActual);
							validationStatus = false;
						}

						String stateActual = viewHistoryDetailsJson.get("state").toString();
						String stateExpected = viewHistoryMap.get("state");

						if (!stateExpected.equalsIgnoreCase(stateActual)) {
							customAssert.assertTrue(false, "Expected and Actual Value for view History option state mismatch");
							customAssert.assertTrue(false, "Expected Value for state : " + stateExpected + "and Actual Value for state : " + stateActual);
							validationStatus = false;
						}
					}
				}
			}

		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while validating Audit Log with history for column " + columnName + " and value " + columnValue);
			validationStatus = false;
		}
		return validationStatus;
	}

	private HashMap<String, String> createViewHistoryMap(String oldValue, String newValue, String property, String state) {

		HashMap<String, String> viewHistoryMap = new HashMap<>();

		viewHistoryMap.put("oldValue", oldValue);
		viewHistoryMap.put("newValue", newValue);
		viewHistoryMap.put("property", property);
		viewHistoryMap.put("state", state);

		return viewHistoryMap;
	}

	//C10772
	private Boolean validateDownloadedBulkTemplateRawDataWithUploadedSlTemplateRawData(String unzipFilePath, String bulkUploadRawDataZipDir,
																					   String slTemplateFileName,
																					   List<String> childserviceLevelId1sForBulkUpload,
																					   List<String> childServiceLevelShortCodeIdsList,
																					   List<String> childServiceLevelDescList,
																					   CustomAssert customAssert) {

		Boolean validationStatus = true;

		String unzippedFilesLocation = unzipFilePath + "/" + bulkUploadRawDataZipDir;
		String expectedFolder;
		String expectedFileName;
		String folderToGoInside;
		int numberOfRowsInUnzippedExcel;

		try {
			int numberOfRowsInPerformanceDataFileUploadedOnSL = Integer.parseInt(XLSUtils.getNoOfRows(uploadFilePath, slTemplateFileName, "Format Sheet").toString());

			List<String> columnNameListTemplateExcel = XLSUtils.getOneColumnDataFromMultipleRowsIncludingEmptyRows(uploadFilePath, slTemplateFileName, "Format Sheet", 0, 2, numberOfRowsInPerformanceDataFileUploadedOnSL - 2);
			List<String> columnTypeListTemplateExcel = XLSUtils.getOneColumnDataFromMultipleRowsIncludingEmptyRows(uploadFilePath, slTemplateFileName, "Format Sheet", 1, 2, numberOfRowsInPerformanceDataFileUploadedOnSL - 2);
			List<String> rowHeadersForExcel;

			String insideFilesPrefix = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulkuploadrawdatazipfilenameiinerfilesprefix");
			for (int i = 0; i < childserviceLevelId1sForBulkUpload.size(); i++) {

				expectedFolder = childServiceLevelShortCodeIdsList.get(i) + " - " + childServiceLevelDescList.get(i);
				folderToGoInside = unzippedFilesLocation + "/" + expectedFolder;

				if(insideFilesPrefix == null) {
					expectedFileName = childServiceLevelShortCodeIdsList.get(i) + " - " + childServiceLevelDescList.get(i) + ".xlsx";
				}else {
					expectedFileName = insideFilesPrefix + childServiceLevelShortCodeIdsList.get(i) + " - " + childServiceLevelDescList.get(i) + ".xlsx";
				}
				if (FileUtils.fileExists(folderToGoInside, expectedFileName)) {

					rowHeadersForExcel = XLSUtils.getExcelDataOfOneRow(folderToGoInside, expectedFileName, "Data Sheet", 1);

					numberOfRowsInUnzippedExcel = Integer.parseInt(XLSUtils.getNoOfRows(folderToGoInside, expectedFileName, "Format Sheet").toString());

					List<String> columnNameListUnZipExcel = XLSUtils.getOneColumnDataFromMultipleRowsIncludingEmptyRows(folderToGoInside, expectedFileName, "Format Sheet", 0, 1, numberOfRowsInUnzippedExcel - 1);
					List<String> columnTypeListUnZipExcel = XLSUtils.getOneColumnDataFromMultipleRowsIncludingEmptyRows(folderToGoInside, expectedFileName, "Format Sheet", 1, 1, numberOfRowsInUnzippedExcel - 1);

					if (columnNameListTemplateExcel.size() == columnNameListUnZipExcel.size()) {

						for (int j = 0; j < columnNameListTemplateExcel.size(); j++) {
							if (!columnNameListTemplateExcel.get(j).equalsIgnoreCase(columnNameListUnZipExcel.get(j))) {
								customAssert.assertTrue(false, "Expected and Actual value for row number " + j + " is not equal for unzipped excel " + expectedFileName
										+ " and template excel");
								validationStatus = false;
							}
						}

					} else {
						customAssert.assertTrue(false, "Number of rows for Column Name in Unzip Excel File " + expectedFileName + "and Template Excel File " + slTemplateFileName + " are not equal ");
						validationStatus = false;
					}

					if (columnTypeListTemplateExcel.size() == columnTypeListUnZipExcel.size()) {
						for (int j = 0; j < columnTypeListTemplateExcel.size(); j++) {
							if (!columnTypeListTemplateExcel.get(j).equalsIgnoreCase(columnTypeListUnZipExcel.get(j))) {
								customAssert.assertTrue(false, "Expected and Actual type for row number " + j + " is not equal for unzipped excel " + expectedFileName
										+ " and template excel");
								validationStatus = false;
							}
						}
					} else {
						customAssert.assertTrue(false, "Number of rows for Column Type in Unzip Excel File " + expectedFileName + "and Template Excel File " + slTemplateFileName + " are not equal ");
						validationStatus = false;
					}

					if (columnNameListTemplateExcel.size() == rowHeadersForExcel.size()) {

						for (int j = 0; j < columnNameListTemplateExcel.size(); j++) {
							if (!columnNameListTemplateExcel.get(j).equalsIgnoreCase(rowHeadersForExcel.get(j))) {
								customAssert.assertTrue(false, "Expected and Actual value for row number " + j + " is not equal for unzipped excel " + expectedFileName
										+ " and template excel");
								validationStatus = false;
							}
						}

					} else {
						customAssert.assertTrue(false, "Number of columns for Header Name in Unzip Excel File " + expectedFileName + "and Template Excel File " + slTemplateFileName + " are not equal ");
						validationStatus = false;
					}

				} else {
					customAssert.assertTrue(false, "File does not exists " + folderToGoInside + "/" + expectedFileName);
					validationStatus = false;

				}

			}

		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while validating Downloaded BulkTemplate RawData With Uploaded SlTemplate RawData " + e.getMessage());
			validationStatus = false;
		}
		return validationStatus;
	}

	private Boolean validateAttachmentName(List<List<String>> recordFromSystemEmailTable, String expectedAttachment, CustomAssert customAssert) {

		Boolean validationStatus = true;

		try {
			String actualAttachment = recordFromSystemEmailTable.get(0).get(1);

			if (expectedAttachment == null && actualAttachment == null) {
				return true;
			} else if (expectedAttachment == null && actualAttachment != null) {
				return false;
			}

			if (actualAttachment.equalsIgnoreCase(expectedAttachment)) {

				customAssert.assertTrue(false, "Attachment name validated unsuccessfully from system emails table");
				validationStatus = false;
			}

		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while validating Attachment Name from system email table with the expected attachment name");
			validationStatus = false;
		}

		return validationStatus;
	}

	private Boolean validateSentSuccessfullyFlag(List<List<String>> recordFromSystemEmailTable, CustomAssert customAssert) {

		Boolean validationStatus = true;

		try {

			if (!recordFromSystemEmailTable.get(0).get(2).equalsIgnoreCase("t")) {
				customAssert.assertTrue(false, "Sent Successfully flag is not true");
				validationStatus = false;
			}

		} catch (Exception e) {

			customAssert.assertTrue(false, "Exception while validating Sent Successfully Flag from system email table with the expected attachment name");
			validationStatus = false;

		}
		return validationStatus;
	}

	private Boolean validateEmailSentSuccessfully(String shortCodeId, String subjectLine, String expectedAttachment, List<String> expectedStringInBody,String timeStamp, CustomAssert customAssert) {

		Boolean validationStatus = true;

		try {
			//		String timeStamp = getCurrentTimeStamp().get(0).get(0);

			List<List<String>> recordFromSystemEmailTable = getRecordFromSystemEmailTable(subjectLine, timeStamp);

			if (recordFromSystemEmailTable.size() == 0) {
				customAssert.assertTrue(false, "No entry in system email table for subject Line " + subjectLine);
				return false;
			}

			if (!validateAttachmentName(recordFromSystemEmailTable, expectedAttachment, customAssert)) {
				customAssert.assertTrue(false, "Attachment Name validated unsuccessfully in system emails table for subjectLine " + subjectLine);
				validationStatus = false;
			}

//			if (!validateSentSuccessfullyFlag(recordFromSystemEmailTable, customAssert)) {
//				customAssert.assertTrue(false, "Sent Successfully Flag validated unsuccessfully in system emails table for subjectLine " + subjectLine);
//				validationStatus = false;
//			}

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

	private Boolean validateTargetValuesFlowDownCSL(String flowToTest, int csl, String showResponseSL, String showResponseCSL, CustomAssert customAssert) {

		Boolean validationStatus = true;

		String thresholdSL;
		String expectedSL;
		String minimumSL;
		String sigMinMaxSL;

		String thresholdCSL;
		String expectedCSL;
		String minimumCSL;
		String sigMinMaxCSL;
		try {
			if (flowToTest.equalsIgnoreCase("sl max level 1") ||
					flowToTest.equalsIgnoreCase("sl min level 1")) {

				thresholdSL = ShowHelper.getValueOfField("threshold", showResponseSL);
				expectedSL = ShowHelper.getValueOfField("expected", showResponseSL);

				thresholdCSL = ShowHelper.getValueOfField("threshold", showResponseCSL);
				expectedCSL = ShowHelper.getValueOfField("expected", showResponseCSL);

				if (!thresholdSL.equalsIgnoreCase(thresholdCSL)) {
					customAssert.assertTrue(false, "Target values flow down for threshold field did not match" +
							" for CSL ID : " + csl);
				}

				if (!expectedSL.equalsIgnoreCase(expectedCSL)) {
					customAssert.assertTrue(false, "Target values flow down for expected field did not match" +
							" for CSL ID : " + csl);
				}

			} else if (flowToTest.equalsIgnoreCase("sl max level 2") ||
					flowToTest.equalsIgnoreCase("sl min level 2")) {

				thresholdSL = ShowHelper.getValueOfField("threshold", showResponseSL);
				expectedSL = ShowHelper.getValueOfField("expected", showResponseSL);
				minimumSL = ShowHelper.getValueOfField("minimum", showResponseSL);

				thresholdCSL = ShowHelper.getValueOfField("threshold", showResponseCSL);
				expectedCSL = ShowHelper.getValueOfField("expected", showResponseCSL);
				minimumCSL = ShowHelper.getValueOfField("minmax", showResponseCSL);

				if (!thresholdSL.equalsIgnoreCase(thresholdCSL)) {
					customAssert.assertTrue(false, "Target values flow down for threshold field did not match" +
							" for CSL ID : " + csl);
				}

				if (!expectedSL.equalsIgnoreCase(expectedCSL)) {
					customAssert.assertTrue(false, "Target values flow down for expected field did not match" +
							" for CSL ID : " + csl);
				}

				if (!minimumSL.equalsIgnoreCase(minimumCSL)) {
					customAssert.assertTrue(false, "Target values flow down for minimum field did not match" +
							" for CSL ID : " + csl);
				}

			} else if (flowToTest.equalsIgnoreCase("sl max level 3") ||
					flowToTest.equalsIgnoreCase("sl min level 3")) {

				thresholdSL = ShowHelper.getValueOfField("threshold", showResponseSL);
				expectedSL = ShowHelper.getValueOfField("expected", showResponseSL);
				minimumSL = ShowHelper.getValueOfField("minimum", showResponseSL);
				sigMinMaxSL = ShowHelper.getValueOfField("sigminmax", showResponseSL);

				thresholdCSL = ShowHelper.getValueOfField("threshold", showResponseCSL);
				expectedCSL = ShowHelper.getValueOfField("expected", showResponseCSL);
				minimumCSL = ShowHelper.getValueOfField("minmax", showResponseCSL);
				sigMinMaxCSL = ShowHelper.getValueOfField("sigminmax", showResponseCSL);

				if (!thresholdSL.equalsIgnoreCase(thresholdCSL)) {
					customAssert.assertTrue(false, "Target values flow down for threshold field did not match" +
							" for CSL ID : " + csl);
					return validationStatus;
				}

				if (!expectedSL.equalsIgnoreCase(expectedCSL)) {
					customAssert.assertTrue(false, "Target values flow down for expected field did not match" +
							" for CSL ID : " + csl);
					return validationStatus;
				}

				if (!minimumSL.equalsIgnoreCase(minimumCSL)) {
					customAssert.assertTrue(false, "Target values flow down for minimum field did not match" +
							" for CSL ID : " + csl);
					return validationStatus;
				}

				if (!sigMinMaxSL.equalsIgnoreCase(sigMinMaxCSL)) {
					customAssert.assertTrue(false, "Target values flow down for sig minimum field did not match" +
							" for CSL ID : " + csl);
					return validationStatus;
				}

			}
		} catch (Exception e) {
			customAssert.assertTrue(false, "Exception while validating flow down of target values");
			validationStatus = false;
		}
		return validationStatus;
	}

	private Boolean validateBodyOfEmail(List<List<String>> recordFromSystemEmailTable, List<String> expectedStringInBody, CustomAssert customAssert) {

		Boolean validationStatus = true;

		String actualBodyHtml;
		try {

			for (int i = 0; i < recordFromSystemEmailTable.size(); i++) {
				actualBodyHtml = recordFromSystemEmailTable.get(i).get(3);

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

	private String waitForScheduler() {

		String result = "pass";
		Long schedulerTimeOut = 240000L;
		Long pollingTime = 5000L;
		try {
			logger.info("Time Out for Scheduler is {} milliseconds", schedulerTimeOut);
			long timeSpent = 0;
			logger.info("Hitting Fetch API.");
			Fetch fetchObj = new Fetch();
			fetchObj.hitFetch();
			logger.info("Getting Task Id of Current Job");
			Thread.sleep(5000);
			int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), null);

			if (newTaskId != -1) {
				Boolean taskCompleted = false;
				logger.info("Checking if Scheduler Task has completed or not.");

				while (timeSpent < schedulerTimeOut) {
					logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
					Thread.sleep(pollingTime);

					logger.info("Hitting Fetch API.");
					fetchObj.hitFetch();
					logger.info("Getting Status of Scheduler Task.");
					String newTaskStatus = UserTasksHelper.getStatusFromTaskJobId(fetchObj.getFetchJsonStr(), newTaskId);
					if (newTaskStatus != null && newTaskStatus.trim().equalsIgnoreCase("Completed")) {
						taskCompleted = true;
						result = "pass";

						break;
					} else {
						timeSpent += pollingTime;
						logger.info("Scheduler Task is not finished yet.");
					}
				}
				if (!taskCompleted && timeSpent >= schedulerTimeOut) {
					//Task didn't complete within given time.
					result = "skip";
				}
			} else {
				logger.info("Couldn't get Scheduler Task Task Job Id. Hence waiting for Task Time Out i.e. {}", schedulerTimeOut);
				Thread.sleep(schedulerTimeOut);
			}
		} catch (Exception e) {
			logger.error("Exception while Waiting for Scheduler to Finish [{}]", e.getMessage());
			result = "fail";
		}
		return result;
	}

	private Boolean checkIfAllRecordsFailed() {

		Boolean allRecordsFailed = true;
		Long schedulerTimeOut = 240000L;
		Long pollingTime = 5000L;
		try {
			logger.info("Time Out for Scheduler is {} milliseconds", schedulerTimeOut);
			long timeSpent = 0;
			logger.info("Hitting Fetch API.");
			Fetch fetchObj = new Fetch();
			fetchObj.hitFetch();
			String fetchResponse;
			logger.info("Getting Task Id of Current Job");
			int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), null);

			if (newTaskId != -1) {
				Boolean taskCompleted = false;
				logger.info("Checking if Scheduler Task has completed or not.");

				while (timeSpent < schedulerTimeOut) {
					logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
					Thread.sleep(pollingTime);

					logger.info("Hitting Fetch API.");
					fetchObj.hitFetch();
					logger.info("Getting Status of Scheduler Task.");

					fetchResponse = fetchObj.getFetchJsonStr();
					String newTaskStatus = UserTasksHelper.getStatusFromTaskJobId(fetchResponse, newTaskId);
					if (newTaskStatus != null && newTaskStatus.trim().equalsIgnoreCase("Completed")) {
						taskCompleted = true;

						allRecordsFailed = UserTasksHelper.ifAllRecordsFailedInTask(newTaskId);

						break;
					} else {
						timeSpent += pollingTime;
						logger.info("Scheduler Task is not finished yet.");
					}
				}
				if (!taskCompleted && timeSpent >= schedulerTimeOut) {
					//Task didn't complete within given time.
				}
			} else {
				logger.info("Couldn't get Scheduler Task Task Job Id. Hence waiting for Task Time Out i.e. {}", schedulerTimeOut);
				Thread.sleep(schedulerTimeOut);
			}
		} catch (Exception e) {
			logger.error("Exception while Waiting for Scheduler to Finish [{}]", e.getMessage());
			allRecordsFailed = false;

		}
		return allRecordsFailed;
	}

	private boolean validateFieldValuesOnShowPage(int entityTypeId, int entityId,
												  HashMap<String, String> showPageFieldValuesMap, CustomAssert customAssert) {

		Boolean validationStatus = true;
		Show show = new Show();

		try {
			show.hitShowVersion2(entityTypeId, entityId);
			String showResponse = show.getShowJsonStr();
			String fieldName;
			String expectedFieldValue;
			String actualFieldValue;

			for (Map.Entry<String, String> entry : showPageFieldValuesMap.entrySet()) {

				fieldName = entry.getKey();
				expectedFieldValue = entry.getValue();
				actualFieldValue = ShowHelper.getValueOfField(fieldName, showResponse);

				if (!expectedFieldValue.equalsIgnoreCase(actualFieldValue)) {
					customAssert.assertTrue(false, "Expected and Actual Field Value did not match for Field " + fieldName +
							" and  entity ID " + entityId);
					validationStatus = false;
				}
			}

		} catch (Exception e) {

			customAssert.assertTrue(false, "Exception while validating field values on show page " + e.getMessage());
			validationStatus = false;
		}

		return validationStatus;

	}

	//C10559
	private boolean validateDownloadFileFromPerformanceTab(int entityId ,String downloadFileName,String uploadedPerformanceDataFileName,CustomAssert customAssert) {

		Boolean validationStatus = true;
		int performanceTabId = 331;
		try{
			//*************Getting Document Id**********************************************//
			logger.info("Getting Document ID");
			String payload = "{\"filterMap\":{\"entityTypeId\":"+ slEntityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
			TabListData tabListData = new TabListData();
			String tabListResponse = tabListData.hitTabListData(performanceTabId,slEntityTypeId,entityId,payload);

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
			Boolean downloadStatus = download.hitDownloadSLPerformanceTab(documentId,downloadFilePath,downloadFileName);

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

	private boolean updatePCQonMSL(int serviceLevelId1,String PCQUpdated,CustomAssert customAssert){

		Boolean updationStatus = true;

		try{
			Edit edit = new Edit();
			String editResponse = edit.hitEdit("service levels",serviceLevelId1);
			JSONObject editResponseJson = new JSONObject(editResponse);
			editResponseJson.remove("header");
			editResponseJson.remove("session");
			editResponseJson.remove("actions");
			editResponseJson.remove("createLinks");
			editResponseJson.getJSONObject("body").remove("layoutInfo");
			editResponseJson.getJSONObject("body").remove("globalData");
			editResponseJson.getJSONObject("body").remove("errors");

			editResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("performanceComputationCalculationQuery").put("values",PCQUpdated);

			editResponse = edit.hitEdit("service levels",editResponseJson.toString());

			if(!editResponse.contains("success")){
				customAssert.assertTrue(false,"Error while updating PCQ on SL ID " + serviceLevelId1);
				updationStatus = false;
			}

		}catch (Exception e){
			customAssert.assertTrue(false,"Exception while updating PCQ on SL " + serviceLevelId1);
			updationStatus = false;
		}
		return updationStatus;
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

	public Boolean validateShowPageFieldWhenRAGIsNo(int entityTypeId,int entityId,String[] valuesToAbsent, CustomAssert customAssert){

		Boolean validationStatus = true;

		try{

			Show show = new Show();
			show.hitShowVersion2(entityTypeId,entityId);
			String showResponse = show.getShowJsonStr();

			JSONObject showResponseJson = new JSONObject(showResponse);

			JSONObject dataJson = showResponseJson.getJSONObject("body").getJSONObject("data");

			String ragApplicableStatus = dataJson.getJSONObject("ragApplicable").getJSONObject("values").get("name").toString();

			if(!ragApplicableStatus.equalsIgnoreCase("No")){
				customAssert.assertTrue(false,"Rag Applicable Field expected as No but actual RAG Applicable is " + ragApplicableStatus);
			}

			for(String valueToBeAbsent : valuesToAbsent){
				try {
					if (dataJson.getJSONObject(valueToBeAbsent).has("values")) {
						customAssert.assertTrue(false, valueToBeAbsent + " should have no \"values\" object in the show page response but contains the value");
						validationStatus = false;
					}
				}catch (Exception e){
					logger.debug(valueToBeAbsent + " not found in json response ");
				}
			}

		}catch (Exception e){

			logger.error("Exception while validating show page Fields when RAG applicable as NO");
			customAssert.assertTrue(false,"Exception while validating show page Fields when RAG applicable as NO");
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

	private Boolean checkIfGraphDataGenerated(int listId,int serviceLevelId1ForGraphValidation,int calendarType,CustomAssert customAssert){

		Boolean graphGenerated = true;
		ListRendererListData listRendererListData = new ListRendererListData();
		String listDataResponse;
		long timeOut = 0L;
		long pollingTime  = 5000L;
		long waitTimeOut = 900000L;
		JSONArray dataSetArray;
		JSONArray listDataResponseJson;
		try{

			while (timeOut < waitTimeOut) {
				try {
					listRendererListData.hitListRendererSlaSpecificGraph(listId, serviceLevelId1ForGraphValidation, calendarType);
					listDataResponse = listRendererListData.getListDataJsonStr();
					listDataResponseJson = new JSONArray(listDataResponse);

					if(listDataResponseJson.length() != 0) {
						dataSetArray = listDataResponseJson.getJSONObject(0).getJSONArray("dataset");

						if (dataSetArray.length() != 0) {
							return true;
						}
					}
				}catch (Exception e){
					logger.error("Exception while getting graph details ");
				}
				Thread.sleep(pollingTime);
				timeOut = timeOut + pollingTime;
			}
			graphGenerated = false;

		}catch (Exception e){

			logger.error("Exception while checking if graph is generated or not " + e.getMessage());

			customAssert.assertTrue(false,"Exception while checking if graph is generated or not " + e.getMessage());
			graphGenerated = false;
		}
		return  graphGenerated;
	}

	private Boolean validateTimeDifferenceValues(int childSlId,
												 ArrayList timeDifferenceValues,
												 CustomAssert customAssert){


		Boolean validationStatus = true;

		ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();

		try{

			String structurePerformanceDataResponse = serviceLevelHelper.getStructuredPerformanceData(childSlId);
			String documentId = serviceLevelHelper.getDocumentIdFromCSLRawDataTab(structurePerformanceDataResponse);

			String payload = "{\"documentId\":" + documentId + ",\"offset\":0,\"size\":20,\"childSlaId\":" + childSlId +"}";

			SLDetails slDetails = new SLDetails();
			slDetails.hitSLDetailsList(payload);
			String slDetailsResponse = slDetails.getSLDetailsResponseStr();

			String timeDifference = "";

			ArrayList<String> timeDifferenceListActual = new ArrayList<>() ;

			if(APIUtils.validJsonResponse(slDetailsResponse)){

				JSONObject slDetailsResponseJson = new JSONObject(slDetailsResponse);
				JSONArray dataArray = slDetailsResponseJson.getJSONArray("data");
				JSONObject indDataRecord;
				JSONArray indRowDataRecordArray;
				JSONObject indRowColumnRecord;
				String columnName;
				for(int dataArrayRecordNo = 0;dataArrayRecordNo<dataArray.length();dataArrayRecordNo++){

					indDataRecord = dataArray.getJSONObject(dataArrayRecordNo);

					indRowDataRecordArray = JSONUtility.convertJsonOnjectToJsonArray(indDataRecord);

					for(int columnNo = 0;columnNo<indRowDataRecordArray.length();columnNo++){

						indRowColumnRecord = indRowDataRecordArray.getJSONObject(columnNo);

						columnName = indRowColumnRecord.get("columnName").toString();
						switch (columnName) {
							case "Time Taken (Seconds)":
								timeDifference = indRowColumnRecord.get("columnValue").toString();
								break;
						}

					}
					timeDifferenceListActual.add(timeDifference);

				}
				int totalNumberOfRowsToValidate = timeDifferenceValues.size();

				for(int i =0;i<totalNumberOfRowsToValidate;i++){

					if(!timeDifferenceValues.get(i).equals(timeDifferenceListActual.get(i))){
						logger.error("Expected and Actual Value for Time Difference didn't match for record " + i + " and CSL ID " + childSlId);
						customAssert.assertTrue(false,"Expected and Actual Value for Time Difference didn't match for record " + i + " and CSL ID " + childSlId);
						validationStatus = false;
					}
				}
			}else {
				logger.error("View Structured is not valid Json");
				customAssert.assertTrue(false,"View Structured is not valid Json");
				validationStatus = false;
			}


		}catch (Exception e){
			logger.error("Exception while validating Time Difference Calculated Values " + e.getMessage());
			customAssert.assertTrue(false,"Exception while validating Time Difference Calculated Values " + e.getMessage());
			validationStatus = false;
		}

		return validationStatus;
	}


	private synchronized Boolean validateFieldsOnShowPage(int entityTypeId,int entityId,String dynamicField,String customFieldValue,CustomAssert customAssert){

		Boolean validationStatus = true;
		try {
			Show show = new Show();
			show.hitShowVersion2(entityTypeId, entityId);
			String showResponse = show.getShowJsonStr();

			JSONObject showResponseJSon = new JSONObject(showResponse);

			String customCurrencyFieldValue = showResponseJSon.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject(dynamicField).get("values").toString();

			if (!customCurrencyFieldValue.equalsIgnoreCase(customFieldValue)) {
				customAssert.assertTrue(false, "Expected and Actual Value Of custom field didn't match");
				validationStatus = false;

			}
		}catch (Exception e){
			customAssert.assertTrue(false,"Exception while validating values on show page " + e.getStackTrace());
			validationStatus = false;
		}
		return validationStatus;
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
		String PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\",\"init_script\":\"state['map'] = ['met':0, 'notMet':0]\",\"reduce_script\":\"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;} return params.result\",\"combine_script\":\"return state;\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}}]}}}";
		String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";

		String user = auditLogUser;
//
		CustomAssert customAssert = new CustomAssert();
		try {

			serviceLevelId = getserviceLevelId1(flowToTest, PCQ, DCQ, customAssert);

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
}