package com.sirionlabs.test;
//

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.recentlyViewed.RecentlyViewed;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.accountInfo.AccountInfo;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import static com.sirionlabs.utils.commonUtils.DateUtils.getCurrentDateInMM_DD_YYYY;

/**
 * Created by gaurav.bhadani on 6/7/2018.
 */
@Listeners(value = MyTestListenerAdapter.class)
public class TestRecentlyVisited {

	private final static Logger logger = LoggerFactory.getLogger(TestRecentlyVisited.class);
	static String configRecentlyViewedFilePath;
	static String configRecentlyViewedFileName;

	String userid;
	String RecentlyViewedDataString;
	Integer statusCode = -1;
	List<Map<String,String>> recentlyvieweddatalist = new ArrayList<>();
	RecentlyViewed recentlyViewed = new RecentlyViewed();

	@BeforeClass
	public void beforeClass() throws Exception {
		logger.info("In Before Class method");

		configRecentlyViewedFilePath = ConfigureConstantFields.getConstantFieldsProperty("RecentlyViewedConfigFilePath");
		configRecentlyViewedFileName = ConfigureConstantFields.getConstantFieldsProperty("RecentlyViewedConfigFileName");
		userid = ParseConfigFile.getValueFromConfigFile(configRecentlyViewedFilePath, configRecentlyViewedFileName, "user_id");
	}

	@Test()
	public void testshowPageRecentlyViewed(){

		CustomAssert csAssert = new CustomAssert();
		logger.info("Validating Recently Viewed Entities of the user");

		int recentlyViewedCounter = 0;

//		Map<String,String> recentlyViewedMap = new HashMap<>();
		Map<String,String> valuesTobeChkOnShowPage;
		String user_id;
		String showpageResponseString;
		JSONObject showpageResponseStringjson;
		String showpagename;
		String showpageid;

		//Values from recently viewed tab
		String title;
		String entityPrefix;
		String clientEntitySeqId;//
		String entityId;
		String entityTypeId;

		try {
			HttpResponse response = recentlyViewed.hitRecentlyViewed();

			String[] statusLine = response.getStatusLine().toString().trim().split(Pattern.quote("HTTP/1.1"));
			if (statusLine.length > 1) {
				statusCode = Integer.parseInt(statusLine[1].trim());
			}
			if (statusCode == 200) {
				RecentlyViewedDataString = recentlyViewed.getJsonStr();
				Boolean isValidJson = APIUtils.validJsonResponse(RecentlyViewedDataString, "[dashboardData response]");
				if (isValidJson) {

					JSONArray recentlyviewedarray = new JSONArray(RecentlyViewedDataString);

					while (recentlyViewedCounter < recentlyviewedarray.length()){
						Map<String,String> recentlyViewedMap = new HashMap<>();
						JSONObject recentlyViewedJsonObj = (JSONObject) recentlyviewedarray.get(recentlyViewedCounter);

						user_id = recentlyViewedJsonObj.get("userId").toString().trim();

						recentlyViewedMap.put("title",recentlyViewedJsonObj.get("title").toString());
						recentlyViewedMap.put("entityPrefix",recentlyViewedJsonObj.get("entityPrefix").toString());
						recentlyViewedMap.put("entityTypeDescription",recentlyViewedJsonObj.get("entityTypeDescription").toString());
						recentlyViewedMap.put("clientEntitySeqId",recentlyViewedJsonObj.get("clientEntitySeqId").toString());
						recentlyViewedMap.put("entityId",recentlyViewedJsonObj.get("entityId").toString());
						recentlyViewedMap.put("entityTypeId",recentlyViewedJsonObj.get("entityTypeId").toString());

						if(user_id.equals(userid)){
							logger.info("User is valid for " + recentlyViewedJsonObj.get("entityPrefix").toString() + "0" + recentlyViewedJsonObj.get("clientEntitySeqId").toString());
						}
						else {
							logger.error("User is invalid for " + recentlyViewedJsonObj.get("entityPrefix").toString() + "0" + "clientEntitySeqId",recentlyViewedJsonObj.get("clientEntitySeqId").toString());
						}
						recentlyViewedJsonObj = null;
						recentlyvieweddatalist.add(recentlyViewedMap);
						recentlyViewedMap = null;
						recentlyViewedCounter = recentlyViewedCounter + 1;

					}
					Iterator<Map<String,String>> iterator = recentlyvieweddatalist.iterator();
					while (iterator.hasNext()){

						valuesTobeChkOnShowPage = iterator.next();

						title = valuesTobeChkOnShowPage.get("title");
						entityPrefix = valuesTobeChkOnShowPage.get("entityPrefix");
						clientEntitySeqId = valuesTobeChkOnShowPage.get("clientEntitySeqId");

						entityId = valuesTobeChkOnShowPage.get("entityId").trim();
						entityTypeId = valuesTobeChkOnShowPage.get("entityTypeId").trim();

						Show show = new Show();
						show.hitShow(Integer.parseInt(entityTypeId),
								Integer.parseInt(entityId));
						showpageResponseString = show.getShowJsonStr();
						boolean isShowPageAccessible= ShowHelper.isShowPageAccessible(showpageResponseString);
						 boolean verifyShowPageOfDeletedRecord=ShowHelper.verifyShowPageOfDeletedRecord(showpageResponseString);
						if (isShowPageAccessible&&!verifyShowPageOfDeletedRecord) {
							showpageResponseStringjson = new JSONObject(showpageResponseString);
							showpagename = showpageResponseStringjson.getJSONObject("body").getJSONObject("data").getJSONObject("name").getString("values");
							showpageid = showpageResponseStringjson.getJSONObject("body").getJSONObject("data").getJSONObject("shortCodeId").getString("values");

							if (showpagename.trim().equals(title)) {
								logger.info("Show Page Name Response valid for ID " + entityPrefix + "0" + clientEntitySeqId);
								csAssert.assertTrue(true, "Show Page Name Response valid for ID " + entityPrefix + "0" + clientEntitySeqId);
							} else {
								logger.error("Show Page Name Response invalid for ID " + entityPrefix + "0" + clientEntitySeqId);
								csAssert.assertTrue(false, "Show Page Name Response invalid for ID " + entityPrefix + "0" + clientEntitySeqId);
							}

							if (showpageid.equals(entityPrefix + "0" + clientEntitySeqId) || showpageid.equals(entityPrefix + clientEntitySeqId)) {
								logger.info("Show Page ID Response valid for ID " + entityPrefix + "0" + clientEntitySeqId);
								csAssert.assertTrue(true, "Show Page ID Response valid for ID " + entityPrefix + "0" + clientEntitySeqId);
							} else {
								logger.error("Show Page ID Response invalid for ID " + entityPrefix + "0" + clientEntitySeqId);
								csAssert.assertTrue(false, "Show Page ID Response invalid for ID " + entityPrefix + "0" + clientEntitySeqId);
							}
							title = null;
							entityPrefix = null;
							clientEntitySeqId = null;
							entityId = null;
							entityTypeId = null;
							showpageid = null;
							showpagename = null;
							valuesTobeChkOnShowPage = null;
						}
						else
						{
							logger.info("Either you do not have the required permissions or requested page does not exist anymore.");
						}
					}
				} else {
					logger.error("Recently viewed response is not valid json for the current user");
					csAssert.assertTrue(false, "Recently viewed response is not valid json for the current user");
				}
			} else {
				logger.error("Error while getting recently viewed data invalid response code");
				csAssert.assertTrue(false, "Error while getting recently viewed data invalid response code");
			}

		}	catch (Exception e){
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			csAssert.assertTrue(false, "TestRecently Viewed Exception\n" + errors.toString());
		}
		csAssert.assertAll();
	}
	@Test(dependsOnMethods = "testshowPageRecentlyViewed")
	public void testRecentlyViewed() {

		CustomAssert csAssert = new CustomAssert();

		int recentlyViewedCounter = 0;
		String user_id = null;
		String datecreated;
		List<List<String>> currentdateindb;
		PostgreSQLJDBC postgreSQLJDBC=new PostgreSQLJDBC();
		try {
			HttpResponse response = recentlyViewed.hitRecentlyViewed();

			String[] statusLine = response.getStatusLine().toString().trim().split(Pattern.quote("HTTP/1.1"));
			if (statusLine.length > 1) {
				statusCode = Integer.parseInt(statusLine[1].trim());
			}
			if (statusCode == 200) {
				RecentlyViewedDataString = recentlyViewed.getJsonStr();
				Boolean isValidJson = APIUtils.validJsonResponse(RecentlyViewedDataString, "[dashboardData response]");
				if (isValidJson) {
					JSONArray recentlyviewedarray = new JSONArray(RecentlyViewedDataString);
					AccountInfo accountInfo=new AccountInfo();
					while (recentlyViewedCounter < recentlyviewedarray.length()) {
						Map<String, String> recentlyViewedMap = new HashMap<>();
						JSONObject recentlyViewedJsonObj = (JSONObject) recentlyviewedarray.get(recentlyViewedCounter);
						user_id = recentlyViewedJsonObj.get("userId").toString().trim();
						datecreated = recentlyViewedJsonObj.get("dateCreated").toString().substring(0,10);
						logger.info("API Date "+recentlyViewedJsonObj.get("dateCreated").toString());
                         String id=recentlyViewedJsonObj.get("id").toString();
						currentdateindb=postgreSQLJDBC.doSelect("select date_created from user_activity_tracker where user_id="+accountInfo.getUserId()+" and id="+id+"order by date_created desc");
						DateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
						utcFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
						DateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
						utcFormat.parse(recentlyViewedJsonObj.get("dateCreated").toString());
						logger.info("DB date " + dateFormat.format( utcFormat.parse(currentdateindb.get(0).get(0).substring(0,19))));
						if(datecreated.equalsIgnoreCase(dateFormat.format( utcFormat.parse(currentdateindb.get(0).get(0).substring(0,19))).substring(0,10))){
							logger.info("Recently viewed list contains valid entry for clientEntitySeqId : " + recentlyViewedJsonObj.get("clientEntitySeqId").toString());
							csAssert.assertTrue(true, "Recently viewed list contains valid entry for clientEntitySeqId : " + recentlyViewedJsonObj.get("clientEntitySeqId").toString());
						}
						else {
							logger.error("datecreated -> "+recentlyViewedJsonObj.get("dateCreated")+ " currentDate -> "+dateFormat.format( utcFormat.parse(currentdateindb.get(0).get(0).substring(0,19))));
							logger.error("Recently viewed list contains invalid entry for clientEntitySeqId : " + recentlyViewedJsonObj.get("clientEntitySeqId").toString());
							csAssert.assertTrue(false, "Recently viewed list contains invalid entry for clientEntitySeqId : " + recentlyViewedJsonObj.get("clientEntitySeqId").toString());
						}

						if (user_id.equals(userid)) {
							logger.info("User Id is valid for " + recentlyViewedJsonObj.get("entityPrefix").toString() + "0" + recentlyViewedJsonObj.get("clientEntitySeqId").toString() + " on Recently Viewed Tab");
							csAssert.assertTrue(true, "User Id is valid for " + recentlyViewedJsonObj.get("entityPrefix").toString() + "0" + recentlyViewedJsonObj.get("clientEntitySeqId").toString());
						} else {
							logger.error("User ID is invalid for " + recentlyViewedJsonObj.get("entityPrefix").toString() + "0" + "clientEntitySeqId", recentlyViewedJsonObj.get("clientEntitySeqId").toString() + " on Recently Viewed Tab");
							csAssert.assertTrue(false, "User Id is valid for " + recentlyViewedJsonObj.get("entityPrefix").toString() + "0" + recentlyViewedJsonObj.get("clientEntitySeqId").toString());
						}
						recentlyViewedJsonObj = null;
						recentlyvieweddatalist.add(recentlyViewedMap);
						recentlyViewedMap = null;
						recentlyViewedCounter = recentlyViewedCounter + 1;
					}
				}
			}
		}catch (Exception e){
			StringWriter errors = new StringWriter();
			e.printStackTrace(new PrintWriter(errors));
			csAssert.assertTrue(false, "TestRecently Viewed Exception\n" + errors.toString());
		}
		finally {
			postgreSQLJDBC.closeConnection();
		}
		csAssert.assertAll();
	}
}