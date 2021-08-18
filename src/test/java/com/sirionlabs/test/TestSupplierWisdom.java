package com.sirionlabs.test;


import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.supplierWisdom.GetMyTarget;
import com.sirionlabs.api.supplierWisdom.GetTargetRating;
import com.sirionlabs.api.supplierWisdom.GetTargetType;
import com.sirionlabs.api.supplierWisdom.GetTokenAuth;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.FileUtils;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.csvutils.DumpResultsIntoCSV;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
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
 * @author shiv.ashish
 */

public class TestSupplierWisdom extends TestRailBase {

	private final static Logger logger = LoggerFactory.getLogger(TestSupplierWisdom.class);
	String authToken = null;
	//CustomAssert csAssert;
	CustomAssert customAssert;

	int targetType;
	int supplierWisdomIdExisting;
	String periodQuarter;
	int periodYear;
	Boolean isVerify;

	String supplierWisdomConfigFilePath;
	String supplierWisdomConfigFileName;


	HashMap<Integer, JSONObject> myTargetMap;
	HashMap<Integer, String> targetTypesMap;
	HashMap<Integer, JSONObject> targetRatingMapforExisingSupplier;

	HashMap<Integer, Integer> mappingofsupplierIdWithSuppilerWisdomId; // this will map the supplier Id with their supplier wisdom Id

	FileUtils fileUtils;
	Show show;
	Integer supplierTypeId;

	DumpResultsIntoCSV dumpResultsObj;
	String TestResultCSVFilePath;
	int globalIndex = 0;


	private List<String> setHeadersInCSVFile() {
		List<String> headers = new ArrayList<String>();
		String allColumns[] = {"Index", "TestMethodName", "TestMethodResult", "Comments", "ErrorMessage"};
		for (String columnName : allColumns)
			headers.add(columnName);
		return headers;
	}


	@BeforeClass
	public void beforeClass() throws IOException, ConfigurationException {
		logger.info("In Before Class method");
//		csAssert = new CustomAssert();
		customAssert = new CustomAssert();
		fileUtils = new FileUtils();
		show = new Show();


		String entityIdMappingConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath");
		String entityIdMappingConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
		supplierTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdMappingConfigFilePath, entityIdMappingConfigFileName, "suppliers", "entity_type_id"));


		supplierWisdomConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("SupplierWisdomConfigFilePath");
		supplierWisdomConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("SupplierWisdomConfigFileName");

		targetType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(supplierWisdomConfigFilePath, supplierWisdomConfigFileName, "targetType"));
		supplierWisdomIdExisting = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(supplierWisdomConfigFilePath, supplierWisdomConfigFileName, "supplieridexisting"));
		periodQuarter = ParseConfigFile.getValueFromConfigFile(supplierWisdomConfigFilePath, supplierWisdomConfigFileName, "periodquarter");
		periodYear = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(supplierWisdomConfigFilePath, supplierWisdomConfigFileName, "periodyear"));
		isVerify = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(supplierWisdomConfigFilePath, supplierWisdomConfigFileName, "isverify"));

		String[] supplierIds = ParseConfigFile.getValueFromConfigFile(supplierWisdomConfigFilePath, supplierWisdomConfigFileName, "supplierids").trim().split(",");
		String[] supplierWisdomIds = ParseConfigFile.getValueFromConfigFile(supplierWisdomConfigFilePath, supplierWisdomConfigFileName, "supplierwisdomids").trim().split(",");

		mappingofsupplierIdWithSuppilerWisdomId = new HashMap<Integer, Integer>();
		for (int i = 0; i < supplierIds.length; i++) {
			mappingofsupplierIdWithSuppilerWisdomId.put(Integer.parseInt(supplierIds[i]), Integer.parseInt(supplierWisdomIds[i]));
		}

		logger.info("mappingofsupplierIdWithSuppilerWisdomId is [{}]", mappingofsupplierIdWithSuppilerWisdomId);

		// for Storing the result of Sorting
		int indexOfClassName = this.getClass().toString().split(" ")[1].lastIndexOf(".");
		String className = this.getClass().toString().split(" ")[1].substring(indexOfClassName + 1);
		TestResultCSVFilePath = ConfigureConstantFields.getConstantFieldsProperty("ResultCSVFile") + className + "/";
		logger.info("TestResultCSVFilePath is :{}", TestResultCSVFilePath);
		dumpResultsObj = new DumpResultsIntoCSV(TestResultCSVFilePath, className + ".csv", setHeadersInCSVFile());

		testCasesMap = getTestCasesMapping();
	}

	@BeforeMethod
	public void beforeMethod(Method method) {
		logger.info("In Before Method");
		logger.info("method name is: {} ", method.getName());
		logger.info("----------------------------------------------------Test Starts Here-----------------------------------------------------------------------");

	}


	// API to Get Supplier Wisdom Get Auth API
	@Test(priority = 0)
	public void testSupplierWisdomGetAuthAPI() throws Exception {

		CustomAssert customAssert = new CustomAssert();
		if (isVerify == false) {
			GetTokenAuth getTokenAuth = new GetTokenAuth();
			getTokenAuth.hitTokenAuth();

//			Assert.assertTrue(getTokenAuth.getApiStatusCode().contains("200"), "Get Token Auth API Response Code is incorrect");
			customAssert.assertTrue(getTokenAuth.getApiStatusCode().contains("200"), "Get Token Auth API Response Code is incorrect");

			logger.debug("Token Auth is :{}", getTokenAuth.getTokenAuth());
			if (getTokenAuth.getTokenAuth() != null) {

				JSONObject jsonObj = new JSONObject(getTokenAuth.getTokenAuth());
				if (jsonObj.has("token")) {
					authToken = jsonObj.getString("token");
				} else
					logger.error("Supplier Get Auth API is not giving authorization token");


			} else {
				logger.error("Supplier Get Auth API is not giving authorization token");
			}

			logger.info("Auth Token Value is token : [{}] ", authToken);
		} else
			logger.info("Skipping the Test Since we only need to verify Already Saved Text File Result to List Page Result");

		addTestResult(getTestCaseIdForMethodName("testSupplierWisdomGetAuthAPI"), customAssert);
		customAssert.assertAll();
	}

	// API to Get Supplier Wisdom Get Target Types
	@Test(dependsOnMethods = "testSupplierWisdomGetAuthAPI", priority = 1)
	public void testSupplierWisdomGetTargetTypes() throws Exception {
		if (isVerify == false) {
			GetTargetType getTargetType = new GetTargetType();
			getTargetType.hitTargetTypeAPI("Token " + authToken);

			logger.info("API Response is :{}", getTargetType.getApiResponse());

			Assert.assertTrue(getTargetType.getApiStatusCode().contains("200"), "Get Target Types API Response Code is incorrect");
			customAssert.assertTrue(APIUtils.validJsonResponse(getTargetType.getApiResponse()), "Get Target Types API Response is not valid Json");

			targetTypesMap = getTargetType.getMapOfTargetTypes();
			//logger.info("targetTypesMap is :{}", targetTypesMap)
		} else
			logger.info("Skipping the Test Since we only need to verify Already Saved Text File Result to List Page Result");
		addTestResult(getTestCaseIdForMethodName("testSupplierWisdomGetTargetTypes"), customAssert);
		customAssert.assertAll();
	}

	// API to Get Supplier Wisdom Get My Targets for Given Target Type
	@Test(dependsOnMethods = "testSupplierWisdomGetTargetTypes", priority = 2)
	public void testSupplierWisdomGetMyTargets() throws Exception {
		if (isVerify == false) {
			GetMyTarget getMyTarget = new GetMyTarget();
			getMyTarget.hitGetMyTargetAPI("Token " + authToken, targetType);

			logger.info("API Response is :{}", getMyTarget.getApiResponse());

			Assert.assertTrue(getMyTarget.getApiStatusCode().contains("200"), "Get My Target API Response Code is incorrect");
			customAssert.assertTrue(APIUtils.validJsonResponse(getMyTarget.getApiResponse()), "Get My Target API Response is not valid Json");

			myTargetMap = getMyTarget.getMapOfMyTargets();
			//logger.info("myTargetMap is :{}", myTargetMap);

		} else
			logger.info("Skipping the Test Since we only need to verify Already Saved Text File Result to List Page Result");
		addTestResult(getTestCaseIdForMethodName("testSupplierWisdomGetMyTargets"), customAssert);
		customAssert.assertAll();
	}

	// API to Get Supplier Wisdom Get Target Ratings for Existing Supplier
	@Test(dependsOnMethods = "testSupplierWisdomGetMyTargets", priority = 3)
	public void testSupplierWisdomTargetRatingForExistingSupplier() throws Exception {
		if (isVerify == false) {
			HashMap<String, String> queryStringParams = new HashMap<String, String>();
			queryStringParams.put("target", String.valueOf(supplierWisdomIdExisting));
			queryStringParams.put("period_quarter", periodQuarter);
			queryStringParams.put("period_year", String.valueOf(periodYear));

			GetTargetRating getTargetRating = new GetTargetRating();
			getTargetRating.hitGetTargetRatingAPI("Token " + authToken, queryStringParams);

			logger.info("API Response is :{}", getTargetRating.getApiResponse());

			Assert.assertTrue(getTargetRating.getApiStatusCode().contains("200"), "For Existing Supplier : Get Target Rating API Response Code is incorrect");
			customAssert.assertTrue(APIUtils.validJsonResponse(getTargetRating.getApiResponse()), "for Existing Supplier :Get Target Rating API Response is not valid Json");


			targetRatingMapforExisingSupplier = getTargetRating.getMapOfRatingsForSupplier();
			fileUtils.dumpResponseInFile(supplierWisdomConfigFilePath + "/TargetRatingforExisingSupplier.txt", getTargetRating.getApiResponse());

			logger.debug("File content is : [{}]", fileUtils.getDataInFile(supplierWisdomConfigFilePath + "/TargetRatingforExisingSupplier.txt"));

		} else
			logger.info("Skipping the Test Since we only need to verify Already Saved Text File Result to List Page Result");
		addTestResult(getTestCaseIdForMethodName("testSupplierWisdomTargetRatingForExistingSupplier"), customAssert);
		customAssert.assertAll();
	}


	@Test(dependsOnMethods = "testSupplierWisdomTargetRatingForExistingSupplier", priority = 5)
	public void verifySupplierRatingWithShowPageAPIResponseForExisingSupplier() throws Exception {
		if (isVerify == true) {

			int existingSupplierid = -1;
			for (Map.Entry<Integer, Integer> entry : mappingofsupplierIdWithSuppilerWisdomId.entrySet()) {
				if (Objects.equals(supplierWisdomIdExisting, entry.getValue())) {
					existingSupplierid = entry.getKey();
					break;
				} else
					continue;
			}

			if (existingSupplierid != -1) {
				float riskScorefromAPIResponse = -1;
				float riskScorefromShowPageResponse = -1;


				// To Get the Risk Score from API Response
				logger.info("File content is : [{}]", fileUtils.getDataInFile(supplierWisdomConfigFilePath + "/TargetRatingforExisingSupplier.txt"));
				JSONObject ratingApiResponse = new JSONObject(fileUtils.getDataInFile(supplierWisdomConfigFilePath + "/TargetRatingforExisingSupplier.txt"));
				JSONArray resultsJsonArray = ratingApiResponse.getJSONArray("results");
				for (int i = 0; i < resultsJsonArray.length(); i++) {
					if ((periodQuarter.contentEquals((CharSequence) resultsJsonArray.getJSONObject(i).get("period_quarter"))) && Integer.parseInt((String) resultsJsonArray.getJSONObject(i).get("period_year")) == periodYear) {
						riskScorefromAPIResponse = Float.parseFloat((String) resultsJsonArray.getJSONObject(i).get("rating"));
					} else
						continue;

				}
				///////////////////////////////////////////////////////


				// To Get the Risk Score from Show Page API
				show.hitShowAPI(supplierTypeId, existingSupplierid);
				logger.debug("Show API Response is : ", show.getShowJsonStr());
				if (show.getValueOfDynamicField(show.getShowJsonStr(), "totalRiskScore") != null)
					riskScorefromShowPageResponse = Float.parseFloat(show.getValueOfDynamicField(show.getShowJsonStr(), "totalRiskScore"));
				///////////////////////////////////////////////////////

				logger.info("riskScorefromAPIResponse is [{}]", riskScorefromAPIResponse);
				logger.info("riskScorefromShowPageResponse is [{}]", riskScorefromShowPageResponse);


				if (riskScorefromAPIResponse != -1 && riskScorefromShowPageResponse != -1)
					customAssert.assertTrue(riskScorefromAPIResponse == riskScorefromShowPageResponse, "For Existing Supplier :Total Risk Score based on Supplier Wisdome API Reponse is not updated in Show Page");

				else {
					//csAssert.assertAll();
					customAssert.assertTrue(false, "Error: Something Wrong Either Supplier Wisdom API is not having total Risk Score or Show Page API Response is not having Risk Score");
				}

			} else {

				logger.error("Error: Config File SupplierWisdom.cfg has no mapping for supplierWisdomIdExisting with supplier Id with ");
			}


		} else
			logger.info("Skipping the Test Since here we only hitting the get Supplier Rating not verifying it with show Page API Response");
		addTestResult(getTestCaseIdForMethodName("verifySupplierRatingWithShowPageAPIResponseForExisingSupplier"), customAssert);
		customAssert.assertAll();
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
		customAssert.assertAll();
	}

}
