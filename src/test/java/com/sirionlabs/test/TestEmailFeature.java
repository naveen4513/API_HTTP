package com.sirionlabs.test;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestResult;
import org.testng.annotations.*;


import java.io.IOException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by shivashish on 4/4/18.
 */
public class TestEmailFeature {

	private final static Logger logger = LoggerFactory.getLogger(TestEmailFeature.class);
	CustomAssert csAssert;

	String emailScenarioConfigFilePath;
	String emailScenarioConfigFileName;
	PostgreSQLJDBC postgreSQLJDBC;
	APIUtils apiUtils;


	/**
	 * beforeClass
	 *
	 * @throws IOException
	 * @throws ConfigurationException
	 */
	@BeforeClass
	public void beforeClass() throws IOException, ConfigurationException {
		logger.info("In Before Class method");
		postgreSQLJDBC = new PostgreSQLJDBC();
		apiUtils = new APIUtils();
		emailScenarioConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("EmailScenarioConfigFilePath");
		emailScenarioConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("EmailScenarioConfigFileName");

	}

	/**
	 * beforeMethod
	 *
	 * @param method
	 */
	@BeforeMethod
	public void beforeMethod(Method method) {
		logger.info("***********************************************************************************************************************");
		logger.info("In Before Method");
		logger.info("method name is: {} ", method.getName());
		logger.info("................................................Test Starts Here................................................");

	}

	private String getdateFromWhichRecordsNeedsToBeFilter(int daywindow) {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		Calendar c = Calendar.getInstance();

		//c.setTime(new Date()); // Now use today date.
		//String todayDate = String.valueOf(sdf.format(c.getTime()));

		c.setTime(new Date());
		c.add(Calendar.DATE, daywindow); //
		String dateFromWhichRecordsNeedsToBeFilter = String.valueOf(sdf.format(c.getTime()));


		return dateFromWhichRecordsNeedsToBeFilter;


	}



	/**
	 * @throws Exception
	 * TS-80699:Verify that Schedule Email is working
	 */
	@Test(priority = 2)
	public void testTS80699() throws Exception {
		csAssert = new CustomAssert();


		String scenarioName = "ts-80699";
		try {
			String testName = ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "test");

			logger.info("Verifying the Test Case : {}", testName);

			String tableName = ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "tablename");
			String parser = ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "parser");

			List<String> columnName = Arrays.asList(ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "columnname").split(parser));
			List<String> comparator = Arrays.asList(ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "comparator").split(parser));
			List<Object> columnValue = Arrays.asList(ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "columnValue").split(parser));

			int daywindow = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "daywindow"));
			List<String> tableColumnNameToSelect = Arrays.asList(ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "tablecolumnnametoselect").split(parser));
			String filterRecordsOrderByQuery = ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "filterrecordsorderbyquery");
			String reportName = ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "reportname");


			if ((columnName.size() == comparator.size()) && (columnName.size() == columnValue.size())) {

				Map<String, Object> columnNameValueMap = new LinkedHashMap<>();
				for (int i = 0; i < columnName.size(); i++) {
					columnNameValueMap.put(columnName.get(i), columnValue.get(i));
				}
				columnNameValueMap.put("date_created", getdateFromWhichRecordsNeedsToBeFilter(daywindow));

				String query = postgreSQLJDBC.getQueryClauses(tableName, tableColumnNameToSelect, columnNameValueMap, comparator) + " " + filterRecordsOrderByQuery + " ";


				logger.debug("query is {}", query);

				List<List<String>> result = postgreSQLJDBC.doSelect(query);

				if (result.isEmpty()) {
					csAssert.assertTrue(false, "There is no such entry in " + tableName + " table based on given filter in config file ");
				} else {

					String htmlbodytext = result.get(0).get(result.get(0).size() - 1); //putting hardcoded 0 index because there is always going to be one record on which we will verify the body content

					// these following assertion is for checking 'report title' , 'user name' who has schedule the report and 'additional comment' in email body
					csAssert.assertTrue(htmlbodytext.contains("The report titled " + reportName + " has been scheduled"), "Email body don't have  Report Name");
					csAssert.assertTrue(htmlbodytext.contains("scheduled by " + ConfigureEnvironment.getEnvironmentProperty("j_username") + " User for your use."), "Email body don't have  correct schedular User Name");
					csAssert.assertTrue(htmlbodytext.contains("automation_" + reportName), "Email body don't have  correct additional comment ");


					// TODO: 6/4/18 check for schedule date need to be done which is subjected to date pattern in email needed to be discussed

				}


			} else {
				csAssert.assertTrue(false, "Config File is incorrect for this test cast : Plz Check Again");
			}


		} catch (Exception e) {

			logger.debug("Error in Fetching the Config Details from config file-- {} ", e.getMessage());
		}


		csAssert.assertAll();

	}

	/**
	 * @throws Exception
	 * TS-80704:Verify that Failed Entity Report is working
	 */
	//@Test(priority = 0)
	public void testTS80704() throws Exception {
		csAssert = new CustomAssert();


		String scenarioName = "ts-80704";
		try {
			String testName = ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "test");

			logger.info("Verifying the Test Case : {}", testName);

			String tableName = ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "tablename");
			String filterRecordsOrderByQuery = ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "filterrecordsorderbyquery");
			String parser = ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "parser");

			List<String> columnName = Arrays.asList(ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "columnname").split(parser));
			List<String> comparator = Arrays.asList(ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "comparator").split(parser));
			List<Object> columnValue = Arrays.asList(ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "columnValue").split(parser));
			int daywindow = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "daywindow"));


			if ((columnName.size() == comparator.size()) && (columnName.size() == columnValue.size())) {

				Map<String, Object> columnNameValueMap = new LinkedHashMap<>();
				for (int i = 0; i < columnName.size(); i++) {
					columnNameValueMap.put(columnName.get(i), columnValue.get(i));
				}
				columnNameValueMap.put("date_created", getdateFromWhichRecordsNeedsToBeFilter(daywindow));

				String query = postgreSQLJDBC.getQueryClauses(tableName, columnNameValueMap, comparator, null) + " " + filterRecordsOrderByQuery + " ";

				logger.debug("query is {}", query);

				List<List<String>> result = postgreSQLJDBC.doSelect(query);

				if (result.isEmpty()) {
					csAssert.assertTrue(false, "There is no such entry in " + tableName + " table based on given filter in config file ");
				}

			} else {
				csAssert.assertTrue(false, "Config File is incorrect for this test cast : Plz Check Again");
			}


		} catch (Exception e) {

			logger.debug("Error in Fetching the Config Details from config file-- {} ", e.getMessage());
		}


		csAssert.assertAll();

	}

	/**
	 * @throws Exception
	 * TS-80703:Verify that User Activity Report is working
	 */
	//@Test(priority = 1)
	public void testTS80703() throws Exception {
		csAssert = new CustomAssert();


		String scenarioName = "ts-80703";
		try {
			String testName = ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "test");

			logger.info("Verifying the Test Case : {}", testName);

			String tableName = ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "tablename");
			String filterRecordsOrderByQuery = ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "filterrecordsorderbyquery");
			String parser = ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "parser");

			List<String> columnName = Arrays.asList(ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "columnname").split(parser));
			List<String> comparator = Arrays.asList(ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "comparator").split(parser));
			List<Object> columnValue = Arrays.asList(ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "columnValue").split(parser));
			int daywindow = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "daywindow"));


			if ((columnName.size() == comparator.size()) && (columnName.size() == columnValue.size())) {

				Map<String, Object> columnNameValueMap = new LinkedHashMap<>();
				for (int i = 0; i < columnName.size(); i++) {
					columnNameValueMap.put(columnName.get(i), columnValue.get(i));
				}
				columnNameValueMap.put("date_created", getdateFromWhichRecordsNeedsToBeFilter(daywindow));


				String query = postgreSQLJDBC.getQueryClauses(tableName, columnNameValueMap, comparator) + " " + filterRecordsOrderByQuery + " ";

				logger.debug("query is {}", query);

				List<List<String>> result = postgreSQLJDBC.doSelect(query);

				if (result.isEmpty()) {
					csAssert.assertTrue(false, "There is no such entry in " + tableName + " table based on given filter in config file ");
				}

			} else {
				csAssert.assertTrue(false, "Config File is incorrect for this test cast : Plz Check Again");
			}


		} catch (Exception e) {

			logger.debug("Error in Fetching the Config Details from config file-- {} ", e.getMessage());
		}


		csAssert.assertAll();

	}




	/**
	 * @throws Exception
	 * TS-80698:Verify that Schedule Large Report is working
	 */
	//@Test(priority = 3)
	public void testTS80698() throws Exception {
		csAssert = new CustomAssert();


		String scenarioName = "ts-80698";
		try {
			String testName = ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "test");

			logger.info("Verifying the Test Case : {}", testName);

			String tableName = ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "tablename");
			String parser = ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "parser");

			List<String> columnName = Arrays.asList(ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "columnname").split(parser));
			List<String> comparator = Arrays.asList(ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "comparator").split(parser));
			List<Object> columnValue = Arrays.asList(ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "columnValue").split(parser));

			int daywindow = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "daywindow"));
			List<String> tableColumnNameToSelect = Arrays.asList(ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "tablecolumnnametoselect").split(parser));
			String filterRecordsOrderByQuery = ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "filterrecordsorderbyquery");
			String reportName = ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "reportname");


			if ((columnName.size() == comparator.size()) && (columnName.size() == columnValue.size())) {

				Map<String, Object> columnNameValueMap = new LinkedHashMap<>();
				for (int i = 0; i < columnName.size(); i++) {
					columnNameValueMap.put(columnName.get(i), columnValue.get(i));
				}
				columnNameValueMap.put("date_created", getdateFromWhichRecordsNeedsToBeFilter(daywindow));

				String query = postgreSQLJDBC.getQueryClauses(tableName, tableColumnNameToSelect, columnNameValueMap, comparator) + " " + filterRecordsOrderByQuery + " ";
				logger.debug("query is {}", query);
				List<List<String>> result = postgreSQLJDBC.doSelect(query);
				if (result.isEmpty()) {
					csAssert.assertTrue(false, "There is no such entry in " + tableName + " table based on given filter in config file ");
				} else {

					String htmlbodytext = result.get(0).get(result.get(0).size() - 1); //putting hardcoded 0 index because there is always going to be one record on which we will verify the body content

					// this  assertion is for checking 'report title'
					csAssert.assertTrue(htmlbodytext.contains(reportName), "Email body don't have Large Report Name");


					// these following statements is for verifying link hypertext in the body
					Document doc = Jsoup.parse(htmlbodytext);
					Elements link = doc.select("a");
					if (link.attr("href").toString().contains("http://" + ConfigureEnvironment.getEnvironmentProperty("Host") + "/scheduleLargeReport/downloadReport?id=")) {
						// TODO: 9/4/18  this code is not verified yet since no entry with dft.auto environment was there in database
						APIUtils apiUtils = new APIUtils();
						Boolean isLinkValid = apiUtils.isLinkValid(link.attr("href").split("/")[link.attr("href").split("/").length - 2] + "/" + link.attr("href").split("/")[link.attr("href").split("/").length - 1]);
						csAssert.assertTrue(isLinkValid, "Link mentioned in the body of email is not valid ");
					} else {
						csAssert.assertTrue(false, "Link in the body of Message --> [ " + link.attr("href").toString() + "] is not correct it should be -->[ " + "http://" + ConfigureEnvironment.getEnvironmentProperty("Host") + "/scheduleLargeReport/downloadReport?id={reportId}" + "]");

					}
				}
			} else {
				csAssert.assertTrue(false, "Config File is incorrect for this test cast : Plz Check Again");
			}


		} catch (Exception e) {

			logger.debug("Error in Fetching the Config Details from config file-- {} ", e.getMessage());
		}


		csAssert.assertAll();

	}

	/**
	 * @throws Exception
	 * TS-80700:Verify that Manual Notification Alert is working
	 */
	//@Test(priority = 4)
	public void testTS80700() throws Exception {
		csAssert = new CustomAssert();


		String scenarioName = "ts-80700";
		try {
			String testName = ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "test");

			logger.info("Verifying the Test Case : {}", testName);

			String tableName = ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "tablename");
			String parser = ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "parser");

			List<String> columnName = Arrays.asList(ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "columnname").split(parser));
			List<String> comparator = Arrays.asList(ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "comparator").split(parser));
			List<Object> columnValue = Arrays.asList(ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "columnValue").split(parser));

			int daywindow = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "daywindow"));
			List<String> tableColumnNameToSelect = Arrays.asList(ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "tablecolumnnametoselect").split(parser));
			String filterRecordsOrderByQuery = ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "filterrecordsorderbyquery");


			String entityName = ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "entityname");
			String entityClientId = ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "entityclientid");
			String entityParentEntityName = ParseConfigFile.getValueFromConfigFile(emailScenarioConfigFilePath, emailScenarioConfigFileName, scenarioName, "entityparententityname");


			if ((columnName.size() == comparator.size()) && (columnName.size() == columnValue.size())) {

				Map<String, Object> columnNameValueMap = new LinkedHashMap<>();
				for (int i = 0; i < columnName.size(); i++) {
					columnNameValueMap.put(columnName.get(i), columnValue.get(i));
				}
				columnNameValueMap.put("date_created", getdateFromWhichRecordsNeedsToBeFilter(daywindow));

				String query = postgreSQLJDBC.getQueryClauses(tableName, tableColumnNameToSelect, columnNameValueMap, comparator) + " " + filterRecordsOrderByQuery + " ";
				logger.debug("query is {}", query);
				List<List<String>> result = postgreSQLJDBC.doSelect(query);
				if (result.isEmpty()) {
					csAssert.assertTrue(false, "There is no such entry in " + tableName + " table based on given filter in config file ");
				} else {

					String htmlbodytext = result.get(0).get(result.get(0).size() - 1); //putting hardcoded 0 index because there is always going to be one record on which we will verify the body content


					Document doc = Jsoup.parse(htmlbodytext);
					Elements links = doc.select("a");

					for (Element link : links) {

						if (link.toString().contains("axigen")) // this hyperlink needed to be ignored
							continue;

						else {
							if (link.attr("href").toString().contains(ConfigureEnvironment.getEnvironmentProperty("Host"))) {
								continue;
							} else {
								csAssert.assertTrue(false, "Link in the body of Message --> [ " + link.attr("href").toString() + "] is not correct it should have link of this enviroment -->[ " + "http://" + ConfigureEnvironment.getEnvironmentProperty("Host") + "]");
							}
						}

					}


					// these following assertion is for checking 'entity name' , 'entityclientId' and 'parententityName' in email body
					csAssert.assertTrue(htmlbodytext.contains(entityName), "Email body don't have  Correct Entity Name");
					csAssert.assertTrue(htmlbodytext.contains(entityClientId), "Email body don't have  Correct Entity Client Id");
					csAssert.assertTrue(htmlbodytext.contains(entityParentEntityName), "Email body don't have  Correct Entity Parent Entity Name ");


				}
			} else {
				csAssert.assertTrue(false, "Config File is incorrect for this test cast : Plz Check Again");
			}


		} catch (Exception e) {

			logger.debug("Error in Fetching the Config Details from config file-- {} ", e.getMessage());
		}


		csAssert.assertAll();

	}


	/**
	 * afterMethod
	 *
	 * @param result
	 */
	@AfterMethod
	public void afterMethod(ITestResult result) {
		logger.info("................................................Test Ends Here................................................");
		logger.info("In After Method");
		logger.info("method name is: {}", result.getMethod().getMethodName());
		logger.info("***********************************************************************************************************************");

	}


	/**
	 * afterClass
	 */
	@AfterClass
	public void afterClass() {
		logger.info("In After Class method");
	}

}
