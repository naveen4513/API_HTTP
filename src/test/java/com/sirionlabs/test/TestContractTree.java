package com.sirionlabs.test;

import com.sirionlabs.api.contractTree.ContractTreeData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.csvutils.DumpResultsIntoCSV;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.*;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.*;


/**
 * Created by shivashish on 2/8/17.
 */
public class TestContractTree extends TestRailBase {


	private final static Logger logger = LoggerFactory.getLogger(TestContractTree.class);
	String baseFilePath;
	String entityIdMappingFileName = "";

	ContractTreeData contractTreeData;
	ListRendererListData listRendererListData;

	String heirarchy;
	String offset;
	String maxrecordsforlistdata;
	String entitySection = "contracts";
	Integer entityTypeId;
	int listId;
	HashSet<Integer> hashSetofDocumentIds;
	HashSet<Integer> hashSetofContractsIds;
	Boolean isDownloadable;
	Boolean isViewable;

	String contractTreeConfigFilePath;
	String contractTreeConfigFileName;


	DumpResultsIntoCSV dumpResultsObj;
	String TestResultCSVFilePath;
	int globalIndex = 0;


	private List<String> setHeadersInCSVFile() {
		List<String> headers = new ArrayList<String>();
		String allColumns[] = {"Index", "TestMethodName", "ContractId", "DocumentId", "TestMethodResult", "Comments", "ErrorMessage"};
		for (String columnName : allColumns)
			headers.add(columnName);
		return headers;
	}

	public void getContractTreeConfigData() throws ParseException, IOException, ConfigurationException {
		logger.debug("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
		logger.info("Getting Test Data");
		contractTreeData = new ContractTreeData();
		listRendererListData = new ListRendererListData();
		hashSetofDocumentIds = new HashSet<Integer>();
		hashSetofContractsIds = new HashSet<Integer>();

		contractTreeConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("ContractTreeConfigFilePath");
		contractTreeConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ContractTreeConfigFileName");

		heirarchy = ParseConfigFile.getValueFromConfigFile(contractTreeConfigFilePath, contractTreeConfigFileName, "hierarchy");
		offset = ParseConfigFile.getValueFromConfigFile(contractTreeConfigFilePath, contractTreeConfigFileName, "offset");
		maxrecordsforlistdata = ParseConfigFile.getValueFromConfigFile(contractTreeConfigFilePath, contractTreeConfigFileName, "maxrecordsforlistdata");


		entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
		baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");
		entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entitySection, "entity_type_id"));
		listId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, entitySection, "entity_url_id"));


		isDownloadable = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(contractTreeConfigFilePath, contractTreeConfigFileName, entitySection, "download"));
		isViewable = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(contractTreeConfigFilePath, contractTreeConfigFileName, entitySection, "view"));


		// for Storing the result of Sorting
		int indexOfClassName = this.getClass().toString().split(" ")[1].lastIndexOf(".");
		String className = this.getClass().toString().split(" ")[1].substring(indexOfClassName + 1);
		TestResultCSVFilePath = ConfigureConstantFields.getConstantFieldsProperty("ResultCSVFile") + className;
		logger.info("TestResultCSVFilePath is :{}", TestResultCSVFilePath);
		dumpResultsObj = new DumpResultsIntoCSV(TestResultCSVFilePath, className + ".csv", setHeadersInCSVFile());


	}

	@BeforeClass
	public void beforeClass() throws IOException, ConfigurationException {
		logger.info("In Before Class method");
		getContractTreeConfigData();

		testCasesMap = getTestCasesMapping();

	}

	@BeforeMethod
	public void beforeMethod(Method method) {
		logger.info("In Before Method");
		logger.info("method name is: {} ", method.getName());
		logger.info("----------------------------------------------------Test Starts Here-----------------------------------------------------------------------");

	}

	//  this function will return all the Contracts Ids from the List Page of Contracts
	public HashSet<Integer> getHashSetContractsIds() throws Exception {
		int entityTypeId = ConfigureConstantFields.getEntityIdByName(entitySection);
		List<Integer> allDBIds;


		String payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" +
				offset + ",\"size\":" +
				maxrecordsforlistdata + ",\"orderByColumnName\":\"id\"," +
				"\"orderDirection\":\"desc\",\"filterJson\":{}}}";


		listRendererListData.hitListRendererListData(listId, false, payload, null);

		String listDataResponse = listRendererListData.getListDataJsonStr();
		logger.debug("List Data API Response : entity={} , response={}", entitySection, listDataResponse);

		boolean isListDataValidJson = APIUtils.validJsonResponse(listDataResponse);
		Assert.assertTrue(isListDataValidJson, "List Contracts API Response is Not Valid");

		JSONObject listDataResponseObj = new JSONObject(listDataResponse);
		int noOfRecords = listDataResponseObj.getJSONArray("data").length();

		if (noOfRecords > 0) {
			listRendererListData.setListData(listDataResponse);
			int columnId = listRendererListData.getColumnIdFromColumnName("id");
			allDBIds = listRendererListData.getAllRecordDbId(columnId, listDataResponse);
			for (Integer dbId : allDBIds) {
				hashSetofContractsIds.add(dbId);
			}
		}
		return hashSetofContractsIds;
	}

	// Contract Tree API Status Code Verification and Verification whether Response is Json or Not
	public void verifyContractTreeAPI(int contractDbId, Boolean hierarchyFlag, CustomAssert csAssertion) throws Exception {
		HashMap<String, String> queryStringMap = new HashMap<String, String>();
		queryStringMap.put("hierarchy", hierarchyFlag.toString());
		queryStringMap.put("offset", offset.toString());


		HttpResponse response = contractTreeData.hitContractTreeDataListAPI(entityTypeId, contractDbId, queryStringMap);
		csAssertion.assertTrue(response.getStatusLine().toString().contains("200"), "Error: Contract Tree API Status Code is Incorrect for Contract Id" + contractDbId + " And when hierarchyFlag is : " + hierarchyFlag);
		csAssertion.assertTrue(APIUtils.validJsonResponse(contractTreeData.getResponseContractTreeData()), "Error: Contract Tree API Response is not a valid JSON Contract Id " + contractDbId + " And when hierarchyFlag is : " + hierarchyFlag);


		//  CSV generation Code Starts Here
		if (!response.getStatusLine().toString().contains("200")) {
			Map<String, String> resultsMap = new HashMap<String, String>();
			resultsMap.put("Index", String.valueOf(++globalIndex));
			resultsMap.put("TestMethodName", Thread.currentThread().getStackTrace()[1].getMethodName());
			resultsMap.put("ContractId", String.valueOf(contractDbId));
			resultsMap.put("DocumentId", "NA");
			resultsMap.put("TestMethodResult", "Fail");
			resultsMap.put("Comments", "Error: Contract Tree API Status Code is Incorrect for Contract Id" + contractDbId + " And when hierarchyFlag is : " + hierarchyFlag);
			resultsMap.put("ErrorMessage", "NA");
			dumpResultsObj.dumpOneResultIntoCSVFile(resultsMap);

		} else if (!APIUtils.validJsonResponse(contractTreeData.getResponseContractTreeData())) {
			Map<String, String> resultsMap = new HashMap<String, String>();
			resultsMap.put("Index", String.valueOf(++globalIndex));
			resultsMap.put("TestMethodName", Thread.currentThread().getStackTrace()[1].getMethodName());
			resultsMap.put("ContractId", String.valueOf(contractDbId));
			resultsMap.put("DocumentId", "NA");
			resultsMap.put("TestMethodResult", "Fail");
			resultsMap.put("Comments", "Error: Contract Tree API Response is not a valid JSON Contract Id " + contractDbId + " And when hierarchyFlag is : " + hierarchyFlag);
			resultsMap.put("ErrorMessage", "NA");
			dumpResultsObj.dumpOneResultIntoCSVFile(resultsMap);
		} else {
			Map<String, String> resultsMap = new HashMap<String, String>();
			resultsMap.put("Index", String.valueOf(++globalIndex));
			resultsMap.put("TestMethodName", Thread.currentThread().getStackTrace()[1].getMethodName());
			resultsMap.put("ContractId", String.valueOf(contractDbId));
			resultsMap.put("DocumentId", "NA");
			resultsMap.put("TestMethodResult", "Pass");
			resultsMap.put("Comments", "NA");
			resultsMap.put("ErrorMessage", "NA");
			dumpResultsObj.dumpOneResultIntoCSVFile(resultsMap);

		}
		//  CSV generation Code Ends Here


		if (hierarchyFlag == false) {
			contractTreeData.setHashSetofDocumentIds();  // this will create the Hash set of All the documents Ids
			if (contractTreeData.getHashSetofDocumentIds().size() > 0)
				hashSetofDocumentIds.addAll(contractTreeData.getHashSetofDocumentIds());
			logger.info("hashSetofDocumentIds is [{}]", hashSetofDocumentIds);
		}

	}

	// Test Contract tree API
	@Test(priority = 0)
	public void testContractTreeAPIWhenHierarchyFlagIsTrue() throws Exception {
//		CustomAssert csAssertion = new CustomAssert();
		CustomAssert customAssert = new CustomAssert();
		HashSet<Integer> allDBidForContracts = getHashSetContractsIds();
		logger.info("All Contract Id are as follows : [{}] ", allDBidForContracts);


		for (Integer contractDBId : allDBidForContracts) {
			logger.info("###################################################:Tests Starting for Contract Id:{}##################################################################", contractDBId);
			verifyContractTreeAPI(contractDBId, true, customAssert);
			logger.info("###################################################:Tests Ending for Contract Id:{}##################################################################", contractDBId);
		}
		addTestResult(getTestCaseIdForMethodName("testContractTreeAPIWhenHierarchyFlagIsTrue"), customAssert);
		customAssert.assertAll();
	}

	@Test(dependsOnMethods = "testContractTreeAPIWhenHierarchyFlagIsTrue", priority = 1)
	public void testContractTreeAPIWhenHierarchyFlagIsFalse() throws Exception {
		CustomAssert customAssert = new CustomAssert();
		HashSet<Integer> allDBidForContracts = getHashSetContractsIds();


		for (Integer contractDBId : allDBidForContracts) {
			logger.info("###################################################:Tests Starting for Contract Id:{}##################################################################", contractDBId);
			verifyContractTreeAPI(contractDBId, false, customAssert);
			logger.info("###################################################:Tests Ending for Contract Id:{}##################################################################", contractDBId);
		}
		addTestResult(getTestCaseIdForMethodName("testContractTreeAPIWhenHierarchyFlagIsFalse"), customAssert);
		customAssert.assertAll();
	}


	// Test Documents View , Download and Rename API
	@Test(enabled = false, dependsOnMethods = "testContractTreeAPIWhenHierarchyFlagIsFalse", priority = 2)
	public void testDocumentsAPI() throws Exception {

		CustomAssert customAssert = new CustomAssert();
		for (Integer documentId : hashSetofDocumentIds) {
			logger.info("###################################################:Tests Starting for Document Id:{}##################################################################", documentId);

			Boolean documentViewAPIStatus = verifyDocumentViewAPI(documentId, isViewable, customAssert);
			//  CSV generation Code Starts Here
			Map<String, String> resultsMap = new HashMap<String, String>();
			resultsMap.put("Index", String.valueOf(++globalIndex));
			resultsMap.put("TestMethodName", "verifyDocumentViewAPI");
			resultsMap.put("ContractId", "NA");
			resultsMap.put("DocumentId", String.valueOf(documentId));
			if (documentViewAPIStatus)
				resultsMap.put("TestMethodResult", "Pass");
			else
				resultsMap.put("TestMethodResult", "Fail");
			resultsMap.put("Comments", "NA");
			resultsMap.put("ErrorMessage", "NA");
			dumpResultsObj.dumpOneResultIntoCSVFile(resultsMap);
			//  CSV generation Code Ends Here


			Boolean documentDownloadAPIStatus = verifyDocumentDownloadAPI(documentId, isDownloadable, customAssert);
			//  CSV generation Code Starts Here
			resultsMap = new HashMap<String, String>();
			resultsMap.put("Index", String.valueOf(++globalIndex));
			resultsMap.put("TestMethodName", "verifyDocumentDownloadAPI");
			resultsMap.put("ContractId", "NA");
			resultsMap.put("DocumentId", String.valueOf(documentId));
			if (documentDownloadAPIStatus)
				resultsMap.put("TestMethodResult", "Pass");
			else
				resultsMap.put("TestMethodResult", "Fail");
			resultsMap.put("Comments", "NA");
			resultsMap.put("ErrorMessage", "NA");
			dumpResultsObj.dumpOneResultIntoCSVFile(resultsMap);
			//  CSV generation Code Ends Here


			Boolean documentRenameAPIStatus = verifyDocumentRenameAPI(documentId, customAssert);
			//  CSV generation Code Starts Here
			resultsMap = new HashMap<String, String>();
			resultsMap.put("Index", String.valueOf(++globalIndex));
			resultsMap.put("TestMethodName", "verifyDocumentRenameAPI");
			resultsMap.put("ContractId", "NA");
			resultsMap.put("DocumentId", String.valueOf(documentId));
			if (documentRenameAPIStatus)
				resultsMap.put("TestMethodResult", "Pass");
			else
				resultsMap.put("TestMethodResult", "Fail");
			resultsMap.put("Comments", "NA");
			resultsMap.put("ErrorMessage", "NA");
			dumpResultsObj.dumpOneResultIntoCSVFile(resultsMap);
			//  CSV generation Code Ends Here


			logger.info("###################################################:Tests Ending for Document Id:{}##################################################################", documentId);
		}
		addTestResult(getTestCaseIdForMethodName("testDocumentsAPI"), customAssert);
		customAssert.assertAll();
	}


	// Document Viewer API Status Code Verification and Verification whether Response is Json or Not
	public boolean verifyDocumentViewAPI(int documentId, Boolean flag, CustomAssert csAssertion) throws Exception {
		HttpResponse response = contractTreeData.hitDocumentViewerAPI(documentId);
		csAssertion.assertTrue(response.getStatusLine().toString().contains("200"), "Error: View Document API Status Code is Incorrect for document Id " + documentId);
		csAssertion.assertTrue(APIUtils.validJsonResponse(contractTreeData.getResponseDocumentViewerData()), "Error: View Document API Response is not a valid JSON document Id " + documentId);

		if (flag == false) {
			if (APIUtils.validJsonResponse(contractTreeData.getResponseDocumentViewerData())) {
				csAssertion.assertTrue(contractTreeData.getResponseDocumentViewerData().contains("Either you do not have the required permissions or requested page does not exist anymore."),
						"Error: View Document API Response is not a valid Since Viewable Flag is False For document Id " + documentId);
				csAssertion.assertTrue(!APIUtils.isApplicationErrorInResponse(contractTreeData.getResponseDocumentViewerData()), "Error: View Document API Response is not valid Since we don't have Viewable Permission document Id " + documentId);
			}
		}

		if (response.getStatusLine().toString().contains("200") && APIUtils.validJsonResponse(contractTreeData.getResponseDocumentViewerData())
				&& !contractTreeData.getResponseDocumentViewerData().contains("Either you do not have the required permissions or requested page does not exist anymore.")
				&& !APIUtils.isApplicationErrorInResponse(contractTreeData.getResponseDocumentViewerData())) {
			return true;

		} else
			return false;


	}

	// Document Download API Status Code Verification and Verification whether Response is Json or Not
	public boolean verifyDocumentDownloadAPI(int documentId, Boolean flag, CustomAssert csAssertion) throws Exception {
		HttpResponse response = contractTreeData.hitDocumentDownloadAPI(documentId);
		csAssertion.assertTrue(response.getStatusLine().toString().contains("200"), "Error: Download Document API Status Code is Incorrect for Document Id " + documentId);

		if (flag == false) {
			csAssertion.assertTrue(contractTreeData.getResponseDocumentDownloadData().contains("Either you do not have the required permissions or requested page does not exist anymore."),
					"Error: View Document API Response is not a valid Since Downloadable Flag is False For document Id " + documentId);
			csAssertion.assertTrue(!APIUtils.isApplicationErrorInResponse(contractTreeData.getResponseDocumentDownloadData()), "Error: View Document API Response is not valid Since we don't have Downloadable Permission document Id " + documentId);

		}

		if (response.getStatusLine().toString().contains("200")
				&& !contractTreeData.getResponseDocumentDownloadData().contains("Either you do not have the required permissions or requested page does not exist anymore.")
				) {
			return true;

		} else
			return false;
	}

	// Document Rename API Status Code Verification and Verification whether Response is Json or Not
	// as of now it will create payload which is just putting it's existing id in It , So it will not change anything
	public boolean verifyDocumentRenameAPI(int documentId, CustomAssert csAssertion) throws Exception {
		String payload = "{\"body\":{\"data\":{\"id\":{\"name\":\"id\",\"values\":" + documentId + "}}}}";
		logger.debug("Payload is {} ", payload);

		HttpResponse response = contractTreeData.hitDocumentReplaceAPI(payload);
		csAssertion.assertTrue(response.getStatusLine().toString().contains("200"), "Error: Rename API Status Code is Incorrect for Document Id " + documentId);
		csAssertion.assertTrue(APIUtils.validJsonResponse(contractTreeData.getResponseDocumentReplaceData()), "Error: Rename API Response is not a valid JSON document Id " + documentId);


		if (response.getStatusLine().toString().contains("200") && APIUtils.validJsonResponse(contractTreeData.getResponseDocumentReplaceData())) {
			return true;

		} else
			return false;
	}

	@AfterMethod
	public void afterMethod(ITestResult result) {
		logger.info("In After Method");
		logger.info("method name is: {}", result.getMethod().getMethodName());
		logger.info("***********************************************************************************************************************");


	}


	@AfterClass
	public void afterClass() {
		logger.info("In After Class method");
	}

}
