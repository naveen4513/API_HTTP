package com.sirionlabs.test;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.ParseException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Created by shivashish on 4/7/17.
 * Here the DAtaProvider will provide Object array on the basis on ITestContext
 *
 * @return
 */

public class TestEmailAttachementData {

	private final static Logger logger = LoggerFactory.getLogger(TestEmailAttachementData.class);
	static String configFilePath;
	static String configFileName;

	String userName;
	String userEmail;
	String[] subjects;
	String mailBoxHost;
	String mailBoxUserName;
	String mailBoxPassword;
	String mailBoxPort;
	String emailDownloadDirectory;
	Boolean mailBoxIsSSL;
	Boolean messagestobesaved;
	Boolean messagebodytobeverified;
	Boolean attachmentsToBeDownload;
	String emailXLSReaderConfigPropertiesFilePath;
	String emailXLSReaderConfigFileName;
	String emailXLSReaderJsonFileName;
	int numofDays;
	EmailReader emailReader = null;
	Message[] mails;
	Show show;
	CustomAssert csAssertion = new CustomAssert();


	@BeforeClass
	public void beforeClass() throws IOException, ConfigurationException {
		logger.info("In Before Class method");
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("EmailConfigPropertiesFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("EmailTestUserAccountConfigFileName");

		getUserAccountConfigData();
		//initializing the Email Reader Utils with Config Specific mailBoxHost , mailBoxUserName , mailBoxPassword ,mailBoxIsSSL,mailBoxPort
		emailReader = new EmailReader(mailBoxHost, mailBoxUserName, mailBoxPassword, mailBoxIsSSL, mailBoxPort);
		show = new Show();
	}

	public void getUserAccountConfigData() throws ParseException, IOException, ConfigurationException {
		logger.info("Getting Test Data");

		userName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "UserName");
		userEmail = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "UserEmail");
		subjects = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "Subjects").split(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "SubjectsDelimiter"));
		numofDays = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "NumberOfDays"));


		mailBoxHost = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "mailBoxHost");
		mailBoxUserName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "mailBoxUserName");
		mailBoxPassword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "mailBoxPassword");
		mailBoxPort = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "mailBoxPort");
		mailBoxIsSSL = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "mailBoxIsSSL"));
		messagestobesaved = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "messagestobesaved"));
		messagebodytobeverified = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "messagebodytobeverified"));
		emailDownloadDirectory = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "DownloadDirectory");
		attachmentsToBeDownload = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "attachmentsToBeDownload"));


		emailXLSReaderConfigPropertiesFilePath = ConfigureConstantFields.getConstantFieldsProperty("EmailXLSReaderConfigPropertiesFilePath");
		emailXLSReaderConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("EmailXLSReaderConfigFileName");
		emailXLSReaderJsonFileName = ConfigureConstantFields.getConstantFieldsProperty("EmailXLSReaderJsonFileName");
	}

	public String getMessageSubject(String str) {

		return str.replace("\"", "").replace("[", "").replace("]", "");
	}

	// function to verify whether Mail with given Subject , Recipient and within duration exist in Mailbox
	public boolean verifyTheEmailExistence(String recipient, int numofDays, String subject) throws MessagingException, IOException {
		logger.info("Filtering the Mailbox with given subject{} , recipient{} and withing numofDays{}", subject, recipient, numofDays);
		logger.info("message to be saved flag is {} , and emailDownloadDirectory is {}", messagestobesaved, emailDownloadDirectory);
		mails = emailReader.showMailsByUserAndDaysAndSubject(recipient, numofDays, subject, messagestobesaved, emailDownloadDirectory, attachmentsToBeDownload);

		if (mails != null)
			return true;
		else
			return false;
	}

	// function to verify Environment Datail Based on the hyperlink of the Entity Data Row
	public boolean verifyTheEnvironmentForAllRows(List<List<String>> allRows, XLSUtils xlsReader) throws MessagingException, IOException {

		for (List<String> mydata : allRows) {
			logger.info("{}", mydata);
			if (mydata.get(0).contains("Link")) {
				return xlsReader.verifytheEnvironment(mydata.get(0).split("Link:")[1]);
			} else {
				logger.info("Error : Excel Sheet Don't have Hyperlink to Get Environment Detail");
				return false;
			}
		}

		logger.info("Error : No Data to Validate in Excel ");
		return false;

	}


	// function to verify Each Sheet from XLS
	public void verifytheSheetData(String sheetName, XLSUtils xlsReader, String attachmentsFileName) throws MessagingException, IOException, java.text.ParseException, ConfigurationException {

		logger.info("SheetName  is : {} and XLS File Name is : {} ", sheetName, attachmentsFileName);
		String xlsParserInternalPropertiesDelimiter = ",";

		XLSJsonUtils jsonUtility = new XLSJsonUtils(emailXLSReaderConfigPropertiesFilePath, emailXLSReaderJsonFileName);

		int columnMax = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(emailXLSReaderConfigPropertiesFilePath, emailXLSReaderConfigFileName, "columnmax"));
		String[] columnInternalProperties = ParseConfigFile.getValueFromConfigFile(emailXLSReaderConfigPropertiesFilePath, emailXLSReaderConfigFileName, "columninternalproperties").split(xlsParserInternalPropertiesDelimiter);

		// It will create Full HapMap Based on the XLS Parser.cfg and will put null for keys where they didn't exist for the Sheet (Entity)
		HashMap<String, HashMap<String, String>> hashMapofEnitity = jsonUtility.createFullMap(sheetName, emailXLSReaderConfigPropertiesFilePath, emailXLSReaderConfigFileName);
		List<List<String>> DataToBeValidated = xlsReader.getAllExcelData(sheetName, hashMapofEnitity.get("rowColInfo"));

		// For Environment Check
		Assert.assertTrue(verifyTheEnvironmentForAllRows(DataToBeValidated, xlsReader), "Environment Host is not Correct in Excel File");

		// Data Validation Starts from Here
		for (List<String> mydata : DataToBeValidated) {
			logger.info("-----------------------------------------------------------------");
			logger.info("Verifying this row --->:{} of Sheet :{} ", mydata, sheetName);

			String showPageId = mydata.get(0).substring(mydata.get(0).lastIndexOf("/") + 1); // Getting the Show Page Id
			logger.debug("Sheet name is : {} , and Entity Type Id is :  {}", sheetName, (ConfigureConstantFields.getEntityIdByName(sheetName)));

			show.hitShowAPI(ConfigureConstantFields.getEntityIdByName(sheetName), Integer.parseInt(showPageId));
			String showPageResponseStr = show.getShowJsonStr();
			logger.info("Show Page Response is : {}  ,XLS row Data is :{}", showPageResponseStr, mydata);

			csAssertion.assertTrue(xlsReader.verifyTheResponseWithXLSData(showPageResponseStr, mydata, hashMapofEnitity, columnMax, columnInternalProperties), "Error : XLS Data is not matching with Show API Response for " + sheetName + " : " + Integer.parseInt(showPageId) + " in XLS File");


			logger.info("Verified this row --->:{} of Sheet :{} ", mydata, sheetName);
			logger.info("-----------------------------------------------------------------");
		}

		csAssertion.assertAll();

	}

	/**
	 * TS-80701:Verify that Weekly Reminder is working
	 *
	 * @param strSubject
	 * @throws MessagingException
	 * @throws IOException
	 */
	@Test(priority = 0, dataProvider = "SubjectsList")
	public void verifyEmailfromSubjects(String strSubject) throws MessagingException, IOException {
		Assert.assertTrue(verifyTheEmailExistence(userEmail, numofDays, strSubject), "Error : Mail is not getting generated for " + userEmail + "having subject" + strSubject);

	}

	/**
	 * TS-80701:Verify that Weekly Reminder is working (Environment Check Only in the body of EMAIL  )
	 *
	 * @param attachmentsFilePath
	 * @param attachmentsFileName
	 * @throws MessagingException
	 * @throws IOException
	 * @throws java.text.ParseException
	 * @throws ConfigurationException
	 */

	@Test(priority = 1, dataProvider = "MailsBody", dependsOnMethods = "verifyEmailfromSubjects")
	public void verifyEmailsBody(String attachmentsFilePath, String attachmentsFileName) throws MessagingException, IOException, java.text.ParseException, ConfigurationException {

		if (messagebodytobeverified == true) {
			logger.info("file Path is : {} and Name is : {}", attachmentsFilePath, attachmentsFileName);
			String content = null;

			StringBuilder contentBuilder = new StringBuilder();
			try {
				BufferedReader in = new BufferedReader(new FileReader(attachmentsFilePath + "/" + attachmentsFileName));
				String str;
				while ((str = in.readLine()) != null) {
					contentBuilder.append(str);
				}
				in.close();
				content = contentBuilder.toString();
			} catch (IOException e) {
				logger.debug("Not being able to convert HTML file to String ");
			}


			Document doc = Jsoup.parse(content);
			Elements links = doc.select("a");

			for (Element link : links) {

				if (link.toString().contains("axigen")) // this hyperlink needed to be ignored
					continue;

				else {
					if (link.attr("href").toString().contains(ConfigureEnvironment.getEnvironmentProperty("Host"))) {
						continue;
					} else {
						Assert.fail("Link in the body of Message --> [ " + link.attr("href").toString() + "] is not correct it should have link to this enviroment -->[ " + "http://" + ConfigureEnvironment.getEnvironmentProperty("Host") + "]");
						break;
					}
				}

			}


		} else {

			logger.info("Verify messagebodytobeverified flag is false in Config So Skipping this test ");
		}


	}


	//@Test(dataProvider = "AttachmentsFilesName", dependsOnMethods = {"verifyEmailfromSubjects"})
	public void verifyEmailAttachments(String attachmentsFilePath, String attachmentsFileName) throws MessagingException, IOException, java.text.ParseException, ConfigurationException {

		if (attachmentsToBeDownload == true) {
			logger.info("file Path is : {} and Name is : {}", attachmentsFilePath, attachmentsFileName);
			XLSUtils xlsReader = new XLSUtils(attachmentsFilePath, attachmentsFileName); // Initialize the XLS Reader Utils for XLS file
			List<String> Sheets = xlsReader.getSheetNames();// this will fetch all the sheets in XLS file

			if (Sheets.size() > 0) // If Only there is any Sheet in XLS file
			{

				for (int i = 0; i < Sheets.size(); i++) // iterate for each sheet
				{
					logger.info("::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*");
					logger.info("Verifying Sheet :{}", Sheets.get(i));
					verifytheSheetData(Sheets.get(i), xlsReader, attachmentsFileName);
					logger.info("Verified Sheet Succcessfully:{}", Sheets.get(i));
					logger.info("::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*::*");
				}
			} else {
				Assert.fail("Error : there is not any sheet in filePath :" + attachmentsFilePath + "having fileName:" + attachmentsFileName);
			}
		}


	}


	@DataProvider(name = "MailsBody")
	public Object[][] getMailsBodyDataProvider(ITestContext c) {
		Object[][] groupArray = null;
		File dirContainingAttachments = null;
		int i = 0;
		File[] directories = new File(emailDownloadDirectory).listFiles(File::isDirectory);
		for (File dirName : directories) {
			if (!dirName.getName().contains("Attachments") && dirName.getName().contains(userEmail.split("@")[0])) {
				dirContainingAttachments = dirName;
			}
			logger.info("Dir Path is : {} ", dirName.getAbsolutePath());
		}

		File folder = new File(dirContainingAttachments.getAbsolutePath());
		File[] listOfFiles = folder.listFiles();

		groupArray = new Object[listOfFiles.length][];


		for (File file : listOfFiles) {
			groupArray[i] = new Object[2];
			groupArray[i][0] = getMessageSubject(dirContainingAttachments.getAbsolutePath());
			groupArray[i][1] = getMessageSubject(file.getName());
			i++;
		}


		return groupArray;
	}


	/**
	 * Here the DAtaProvider will provide Object array on the basis on ITestContext
	 *
	 * @return
	 */
	@DataProvider(name = "AttachmentsFilesName")
	public Object[][] getXLSFileListDataProvider(ITestContext c) {
		Object[][] groupArray = null;
		if (attachmentsToBeDownload == true) {
			File dirContainingAttachments = null;
			int i = 0;
			File[] directories = new File(emailDownloadDirectory).listFiles(File::isDirectory);
			for (File dirName : directories) {
				if (dirName.getName().contains("Attachments") && dirName.getName().contains(userEmail.split("@")[0])) {
					dirContainingAttachments = dirName;
				}
				logger.info("Dir Path is : {} ", dirName.getAbsolutePath());
			}

			File folder = new File(dirContainingAttachments.getAbsolutePath());
			File[] listOfFiles = folder.listFiles();

			groupArray = new Object[listOfFiles.length][];


			for (File file : listOfFiles) {
				groupArray[i] = new Object[2];
				groupArray[i][0] = getMessageSubject(dirContainingAttachments.getAbsolutePath());
				groupArray[i][1] = getMessageSubject(file.getName());
				i++;
			}
		}

		return groupArray;
	}


	/**
	 * Here the DAtaProvider will provide Object array on the basis on ITestContext
	 *
	 * @return
	 */
	@DataProvider(name = "SubjectsList")
	public Object[][] getDataFromDataprovider(ITestContext c) {
		Object[][] groupArray = new Object[subjects.length][];

		for (int i = 0; i < subjects.length; i++) {
			groupArray[i] = new Object[1];
			groupArray[i][0] = getMessageSubject(subjects[i]);
		}

		return groupArray;
	}

	@AfterClass
	public void afterClass() {
		logger.info("In After Class method");
	}


}
