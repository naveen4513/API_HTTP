package com.sirionlabs.test;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.EmailReader;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.ParseException;
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
import java.io.IOException;

/**
 * Created by shivashish on 22/6/17.
 */

public class TestUserAccountEmailNotification {

	private final static Logger logger = LoggerFactory.getLogger(TestUserAccountEmailNotification.class);
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
	Boolean attachmentsToBeDownload;
	int numofDays;
	EmailReader emailReader = null;

	@BeforeClass
	public void beforeClass() throws IOException, ConfigurationException {
		logger.info("In Before Class method");
		configFilePath = ConfigureConstantFields.getConstantFieldsProperty("EmailConfigPropertiesFilePath");
		configFileName = ConfigureConstantFields.getConstantFieldsProperty("EmailTestUserAccountConfigFileName");
		getUserAccountConfigData();

		// initializing the Email Reader Utils with Config Specific mailBoxHost , mailBoxUserName , mailBoxPassword ,mailBoxIsSSL,mailBoxPort
		emailReader = new EmailReader(mailBoxHost, mailBoxUserName, mailBoxPassword, mailBoxIsSSL, mailBoxPort);
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
		emailDownloadDirectory = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "DownloadDirectory");
		attachmentsToBeDownload = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "attachmentsToBeDownload"));

	}

	// function to verify whether Mail with given Subject , Recipient and within duration exist in Mailbox
	public boolean verifytheEmailExistence(String recipient, int numofDays, String subject) throws MessagingException, IOException {
		logger.info("Filtering the Mailbox with given subject{} , recipient{} and withing numofDays{}", subject, recipient, numofDays);
		logger.info("message to be saved flag is {} , and emailDownloadDirectory is {}", messagestobesaved, emailDownloadDirectory);
		Message[] mails = emailReader.showMailsByUserAndDaysAndSubject(recipient, numofDays, subject, messagestobesaved, emailDownloadDirectory, attachmentsToBeDownload);

		if (mails != null)
			return true;
		else
			return false;
	}

	@Test(dataProvider = "SubjectsList")
	public void verifyEmailfromSubjects(String strSubject) throws MessagingException, IOException {
		Assert.assertTrue(verifytheEmailExistence(userEmail, numofDays, strSubject), "Error : Mail is not getting generated for " + userEmail + "having subject" + strSubject);

	}

	public String getMessageSubject(String str) {

		return str.replace("\"", "").replace("[", "").replace("]", "");
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
