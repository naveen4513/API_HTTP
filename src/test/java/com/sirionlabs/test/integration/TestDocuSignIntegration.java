package com.sirionlabs.test.integration;

import com.sirionlabs.api.integration.docuSign.DocuSignSend;
import com.sirionlabs.helper.selenium.Configuration;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.dbHelper.DocusignHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import static com.sirionlabs.helper.dbHelper.DocusignHelper.*;
import static org.openqa.selenium.By.*;

public class TestDocuSignIntegration {

    private final static Logger logger = LoggerFactory.getLogger(TestDocuSignIntegration.class);
    public static WebDriver driver = Configuration.invokeDriver();
    private String docuSignConfigFilePath;
    private String docuSignConfigFileName;
    private List<String> entitiesToTest;
    private String username = null;
    private String password = null;
    private static String recipientsEmail;
    private String qaUrl = null;
    private String qaUsername = null;
    private String qaPassword = null;
    private static String envelopeID = null;
    private static int integrationId;
    private static int otherAuditLogId;
    private static int otherAuditLogDocFileId;
    private static String entityId;
    private static String documentName;
    private static String latestEnvelopeId;
    private static String latestDocumentId;

    @BeforeClass
    public void setConfig() {
        try {
            docuSignConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("DocuSignNewModuleConfigFilePath");
            docuSignConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("DocuSignNewModuleConfigFileName");
            DocusignHelper dbHelper = new DocusignHelper();
            username = ParseConfigFile.getValueFromConfigFile(docuSignConfigFilePath, docuSignConfigFileName, "default", "docuusername");
            password = ParseConfigFile.getValueFromConfigFile(docuSignConfigFilePath, docuSignConfigFileName, "default", "docupassword");
            qaUsername = ParseConfigFile.getValueFromConfigFile(docuSignConfigFilePath, docuSignConfigFileName, "default", "qausername");
            qaPassword = ParseConfigFile.getValueFromConfigFile(docuSignConfigFilePath, docuSignConfigFileName, "default", "qapassword");
            qaUrl = ParseConfigFile.getValueFromConfigFile(docuSignConfigFilePath, docuSignConfigFileName, "default", "qaurl");
            documentName = ParseConfigFile.getValueFromConfigFile(docuSignConfigFilePath, docuSignConfigFileName, "documents", "documentname");
            entitiesToTest = getPropertyList("entitiestotest");
        } catch (Exception e) {
            logger.error("Exception while setting config properties for DocuSign Integration. error : {}", e.getMessage());
        }
    }

    @DataProvider(name = "getEntitiesToTest", parallel = false)
    public Object[][] getEntitiesToTest() {

        int i = 0;
        Object[][] entityArray;

        entityArray = new Object[entitiesToTest.size()][];

        for (String entry : entitiesToTest) {
            entityArray[i] = new Object[1];
            entityArray[i][0] = entry; // sectionName
            i++;
        }
        return entityArray;
    }

    @BeforeMethod
    void findLatestValuesFromDocSignDB(){
        List<String> list = getLatestValuesFromDocSignDB();
        latestDocumentId = list.get(1);
    }

    @Test(dataProvider = "getEntitiesToTest", priority = 1)
    public void testDocuSignViaConnectAPI(String entitySection) {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("################### DOCUSIGN VALIDATION WITH CONNECT API STARTED FOR ENTITY : {} ###########################", entitySection);

            // Update the connect API flag = true for the selected IntegrationId
            activateConnectApi(integrationId);

            String payload = getPayloadForDocuSignSendApi(entitySection, "ATTACHMENT");

            // Take latest entry in other_audit_log table for entity under test -------------------------------------------------------
            otherAuditLogId = getLatestIdFromOtherAuditLog(entityId);

            logger.info("Hitting the Docusign send api from Sirion for entity - ", entitySection);
            String response = getDocuSignSendApiResponse(payload);

            logger.info("Checking the origin of document type");
            String val = getDocumentTypeOrigin(latestDocumentId);
            csAssert.assertEquals( val,"1");



            logger.info("Validating the response body");
            if (APIUtils.validJsonResponse(response)) {

                logger.info("Response body valid");
                Boolean isDocumentSuccessfullyPosted = getDocuSignPostedStatus(response);


                if (!isDocumentSuccessfullyPosted) {
                    logger.error("Document not submitted successfully for docusign. entity : {}", entitySection);
                    csAssert.assertTrue(false, "document not submitted successfully for docuSign. entity : {}" + entitySection);
                } else {
                    logger.info("Document is successfully submitted for docusign from Sirion System. entity : {}", entitySection);
                    String documentSubmitUrl = getDocumentSubmitUrl(response);

                    logger.info("Sending the file for signature to recipients from Docusign website");
                    boolean sendForSignature = getDocumentSignedByDocuSignIntegration(documentSubmitUrl);

                    // Verifying if the entry other_audit_log table has been updated  -------------------------------------------------------
                    List<String> lst = getRecentInfoFromOtherAuditLog(otherAuditLogId, entityId);
                    csAssert.assertEquals( lst.get(0),"73");
                    csAssert.assertEquals( lst.get(1),"Documents Submitted for Signature : \n(TestDocument)");

                    if (sendForSignature) {
                        csAssert.assertTrue(sendForSignature, "document send to stakeholders");

                        // Take latest entry in other_audit_log_document_file table for entity under test -------------------------------------------------------
                        otherAuditLogDocFileId = getLatestIdFromOtherAuditLogDocFile();

                        logger.info("Signing the document by getting into received emails by recipients");
                        boolean signed = signDocument();

                        if (signed) {

                            // Wait time to ensure status change in DB.
                            Thread.sleep(5000);
                            logger.info("Verifying that envelopStatus changed from 3 to 8");

                            Integer envIdStatus = fetchenvIdStatus();

                            csAssert.assertEquals(envIdStatus.toString(), "8", "status code of envelope after signing is " + envIdStatus);
                            logger.info("Verification successful");

                            logger.info("Verifying the entry in Communication tab");

                            // Verifying if the entry in other_audit_log_document_file table has been updated  -------------------------------------------------------
                            int auditLogId  = getRecentInfoFromOtherAuditLogDocFile(otherAuditLogDocFileId, documentName);
                            // Take latest entry in other_audit_log table for entity under test -------------------------------------------------------
                            otherAuditLogId = getLatestIdFromOtherAuditLog(entityId);
                            csAssert.assertEquals(auditLogId, otherAuditLogId);

                            int actionId = getActionId(auditLogId);
                            csAssert.assertEquals(actionId, 72);

                            csAssert.assertEquals("Documents Submitted for Signature : \n(TestDocument)", lst.get(1));
                            logger.info("DocuSign validation passed for entity" + entitySection);


                        } else
                            csAssert.assertTrue(sendForSignature, "Docusign error while signing the document");

                    } else
                        csAssert.assertTrue(sendForSignature, "Docusign error while sending the document for signature");


                }

            } else {
                logger.error("Docusign send api response is not valid json. response : {}", response);
                csAssert.assertTrue(false, "Docusign send api response is not valid json");
            }
        } catch (Exception e) {
            logger.error("Exception while validating Docusign send api response. error : {}", e.getMessage());
            e.getStackTrace();
            csAssert.assertTrue(false, "Exception while validating docuSign send api response. error : " + e.getMessage());
        }
        csAssert.assertAll();
    }

    @Test(priority = 2)
    public void testDocuSignViaContractDocumentTab(String entitySection) {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("################### DOCUSIGN VALIDATION WITH CONNECT API STARTED FOR ENTITY : {} ###########################", entitySection);

            // Update the connect API flag = true for the selected IntegrationId
            activateConnectApi(integrationId);

            String payload = getPayloadForDocuSignSendApi(entitySection, "DRAFT");

            // Take latest entry in other_audit_log table for entity under test -------------------------------------------------------
            otherAuditLogId = getLatestIdFromOtherAuditLog(entityId);

            logger.info("Hitting the Docusign send api from Sirion for entity - ", entitySection);
            String response = getDocuSignSendApiResponse(payload);

            logger.info("Checking the origin of document type");
            String val = getDocumentTypeOrigin(latestDocumentId);
            csAssert.assertEquals( val,"2");

        } catch (Exception e) {
            logger.error("Exception while validating Docusign send api response. error : {}", e.getMessage());
            e.getStackTrace();
            csAssert.assertTrue(false, "Exception while validating docuSign send api response. error : " + e.getMessage());
        }
        csAssert.assertAll();
    }

    // Login to receiver's gmail and sign the document
    private boolean signDocument() throws InterruptedException {

        boolean signed = false;
        Thread.sleep(5000);
        WebDriverWait wait = new WebDriverWait(driver, 60);
        driver.manage().timeouts().implicitlyWait(20, TimeUnit.SECONDS);
        driver.get("https://mail.google.com/mail/");


        // Login into gmail
        WebElement elem;

        wait.until(ExpectedConditions.elementToBeClickable(linkText("Sign in"))).click();
        ArrayList<String> tabs3 = new ArrayList<>(driver.getWindowHandles());
        driver.switchTo().window(tabs3.get(tabs3.size() - 1));
        elem = wait.until(ExpectedConditions.elementToBeClickable(id("identifierId")));


        elem.sendKeys("docusigntestsirion@gmail.com");
        wait.until(ExpectedConditions.elementToBeClickable(driver.findElement(xpath("//span[text()='Next']")))).click();
        driver.findElement(xpath("//input[@name='password']")).sendKeys("Admin@123");

        wait.until(ExpectedConditions.elementToBeClickable(xpath("//span[text()='Next']"))).click();

        Thread.sleep(5000);
        List<WebElement> unreademeil = driver.findElements(By.xpath("//*[@class='zF']"));
        Thread.sleep(3000);
        String MyMailer = "Naveen Kumar Gupta .";

        for (WebElement webElement : unreademeil) {
            if (webElement.isDisplayed()) {
                if (webElement.getText().equals(MyMailer)) {
                    webElement.click();
                    Thread.sleep(2000);
                    wait.until(ExpectedConditions.elementToBeClickable(linkText("REVIEW DOCUMENT"))).click();
                    ArrayList<String> tabs2 = new ArrayList<>(driver.getWindowHandles());
                    driver.switchTo().window(tabs2.get(tabs2.size() - 1));
                    Thread.sleep(2000);
                    try {
                        WebElement element = driver.findElement(xpath("//input[@id='disclosureAccepted']"));

                        Actions actions = new Actions(driver);
                        actions.moveToElement(element).click().build().perform();
                    } catch (Exception e) {
                        logger.info("No checkbox");
                    }
                    wait.until(ExpectedConditions.elementToBeClickable(xpath("//button[@id='action-bar-btn-continue']"))).click();
                    wait.until(ExpectedConditions.elementToBeClickable(xpath("//*[@class='tab-image tab-image-for-signature']"))).click();
                    wait.until(ExpectedConditions.elementToBeClickable(xpath("//button[contains(text(),'Adopt and Sign')]"))).click();
                    Thread.sleep(3000);
                    wait.until(ExpectedConditions.presenceOfElementLocated(xpath("//button[@id='action-bar-btn-finish']"))).click();
                    wait.until(ExpectedConditions.elementToBeClickable(xpath("//button[@class='btn btn-lg btn-minor']"))).click();

                    signed = true;

                    break;

                }
            }
        }

        logger.info("Signed the document");

        closeAllExtratabs();

        return signed;
    }

    // Calculate the redirect URL from the docusign api's response
    private String getDocumentSubmitUrl(String response) {
        String url = null;
        try {
            JSONObject jsonObject = new JSONObject(response);
            url = jsonObject.getString("redirectURL");

        } catch (Exception e) {
            logger.error("Exception while getting document submit url. error : {}", e.getMessage());
            e.printStackTrace();
        }
        return url;
    }

    // Closing all extra tabs but keep one open
    private void closeAllExtratabs() {
        Set<String> list =     driver.getWindowHandles();
        Object arr[] = list.toArray();

        for (int i = arr.length-1; i>0 ; i--) {
            driver.switchTo().window(arr[i].toString());
            driver.close();
        }
        driver.switchTo().window(arr[0].toString());
    }

    // Waiting to get the status changed and then returning it
    private Integer fetchenvIdStatus() throws InterruptedException {
        Thread.sleep(20000);
        Integer envIdStatus = getEnvelopeStatus(envelopeID);
        return envIdStatus;
    }

    // Verify if the Docusign was successfully submitted or not
    private Boolean getDocuSignPostedStatus(String response) {
        Boolean status = true;
        try {
            JSONObject jsonObject = new JSONObject(response);
            if (jsonObject.has("status")) {
                if (!jsonObject.getString("status").equalsIgnoreCase("success")) {
                    status = false;
                    logger.error("document not posted successfully for docuSign. response : {}", response);
                } else
                    logger.info("Document Successfully posted for Docu-Sign");
            } else {
                status = false;
                logger.error("DocuSign send api response does not contains status object.");
            }

        } catch (Exception e) {
            logger.error("Exception while getting docusign posting status. error : {}", e.getMessage());
            e.printStackTrace();
        }
        return status;
    }

    // Hitting the Docusign send API and collecting the response
    private String getDocuSignSendApiResponse(String payload) {
        String response = null;
        try {
            DocuSignSend docuSignSend = new DocuSignSend();
            docuSignSend.hitDocuSignSend(payload);
            response = docuSignSend.getDocuSignSendJsonStr();

        } catch (Exception e) {
            logger.error("Exception while getting docu sign send api response. error : {}", e.getMessage());
            e.printStackTrace();
        }
        return response;
    }

    // Calculate the payload for Docusign send API
    private String getPayloadForDocuSignSendApi(String entitySection, String documentType) {
        String payload = null;
        String recipients_list = "";
        String recipient = "";
        String document = "";
        String documents_list = "";
        String names_list = "";
        String name = "";

        try {
            entityId = ParseConfigFile.getValueFromConfigFile(docuSignConfigFilePath, docuSignConfigFileName, entitySection, "entityid");
            String entityTypeId = ParseConfigFile.getValueFromConfigFile(docuSignConfigFilePath, docuSignConfigFileName, entitySection, "entitytypeid");
            String documentName = ParseConfigFile.getValueFromConfigFile(docuSignConfigFilePath, docuSignConfigFileName, "documents", "documentname");
            String documentId = ParseConfigFile.getValueFromConfigFile(docuSignConfigFilePath, docuSignConfigFileName, "documents", "documentid");
            String docType = ParseConfigFile.getValueFromConfigFile(docuSignConfigFilePath, docuSignConfigFileName, "documents", "doctype");
            integrationId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(docuSignConfigFilePath, docuSignConfigFileName, "docusign", "integrationid"));
            String recipientsUserId = ParseConfigFile.getValueFromConfigFile(docuSignConfigFilePath, docuSignConfigFileName, entitySection, "recipientsuserid");
            String recipientsName = ParseConfigFile.getValueFromConfigFile(docuSignConfigFilePath, docuSignConfigFileName, entitySection, "recipientsname");
            recipientsEmail = ParseConfigFile.getValueFromConfigFile(docuSignConfigFilePath, docuSignConfigFileName, entitySection, "recipientsemail");
            String password = ParseConfigFile.getValueFromConfigFile(docuSignConfigFilePath, docuSignConfigFileName, entitySection, "password");

            if (recipientsEmail != null) {
                String[] emails = recipientsEmail.split(",");
                for (String email : emails) {
                    recipient = recipient + "{\"recipientEmail\":\"" + email.trim() + "\"},";
                }
                recipients_list = "\"recipients\":[" + recipient.substring(0, recipient.length() - 1) + "]";
            }

            String[] doc_names = documentName.split(",");
            String[] doc_ids = documentId.split(",");
            String[] type = docType.split(",");
            String[] doc_type = documentType.split(",");
            for (int i = 0; i < doc_names.length; i++) {
                document = document + "{\"documentName\":\"" + doc_names[i].trim() + "\",\"documentId\":" + doc_ids[i].trim() + ",\"type\":\"" + type[i].trim() +   "\",\"documentType\":\"" + doc_type[i].trim() + "\"},";
            }

            documents_list = "\"documents\":[" + document.substring(0, document.length() - 1) + "]";

            if (recipientsUserId != null && recipientsName != null) {
                String[] userIds = recipientsUserId.split(",");
                String[] names = recipientsName.split(",");
                for (int i = 0; i < userIds.length; i++) {
                    name = name + "{\"userId\":" + userIds[i].trim() + ",\"name\":\"" + names[i].trim() + "\"},";
                }
                names_list = "\"recipients\":[" + name.substring(0, name.length() - 1) + "]";
            }

            payload = "{\"entityTypeInfo\":{\"entityId\":" + entityId + ",\"entityTypeId\":" + entityTypeId + "}," + documents_list + ",";
            if (!names_list.equals("")) {
                payload = payload + names_list;
            } else {
                payload = payload + recipients_list;
            }


            payload += ",\"integrationId\":" + integrationId;

            payload = payload + "}";

        } catch (Exception e) {
            logger.error("Exception while getting payload for docu sign send api. error : {}", e.getMessage());
            e.printStackTrace();
        }

        return payload;
    }

    // Calculating the properties of the test
    private List<String> getPropertyList(String propertyName) throws ConfigurationException {
        String value = ParseConfigFile.getValueFromConfigFile(this.docuSignConfigFilePath, this.docuSignConfigFileName, propertyName);
        List<String> list = new ArrayList<String>();

        if (!value.trim().equalsIgnoreCase("")) {
            String properties[] = ParseConfigFile.getValueFromConfigFile(this.docuSignConfigFilePath, this.docuSignConfigFileName, propertyName).split(",");

            for (int i = 0; i < properties.length; i++)
                list.add(properties[i].trim());
        }
        return list;
    }

    // Putting the Signature stamp on the document for each recipient
    private Boolean getDocumentSignedByDocuSignIntegration(String documentSubmitUrl) throws AWTException, InterruptedException, IOException, SQLException {

        WebDriverWait wait = new WebDriverWait(driver, 60);
        WebElement elem;
        driver.navigate().to(qaUrl);

        // Login into the Sirion System
        wait.until(ExpectedConditions.elementToBeClickable(cssSelector("#newTextUser"))).sendKeys(qaUsername);
        wait.until(ExpectedConditions.elementToBeClickable(cssSelector("#newTextPassword"))).sendKeys(qaPassword);
        wait.until(ExpectedConditions.elementToBeClickable(cssSelector("#newLoginButton"))).click();

        // Open a new tab in the browser
        Robot rb = new Robot();
        rb.keyPress(KeyEvent.VK_CONTROL);
        rb.keyPress(KeyEvent.VK_T);
        rb.keyRelease(KeyEvent.VK_CONTROL);
        rb.keyRelease(KeyEvent.VK_T);
        java.lang.Thread.sleep(1000);
        for (String winHandle : driver.getWindowHandles()) {
            driver.switchTo().window(winHandle);
        }

        // Navigate to the Docusign URL for the  submitted file
        driver.navigate().to(documentSubmitUrl);
        wait.until(ExpectedConditions.elementToBeClickable(id("username"))).sendKeys(username);
        wait.until(ExpectedConditions.elementToBeClickable(xpath("//span[contains(text(),'Continue')]"))).click();
        wait.until(ExpectedConditions.elementToBeClickable(id("password"))).sendKeys(password);
        wait.until(ExpectedConditions.elementToBeClickable(xpath("//button[@class='btn btn-main btn-lg']//div"))).click();
        wait.until(ExpectedConditions.elementToBeClickable(linkText("here"))).click();
        wait.until(ExpectedConditions.presenceOfElementLocated(id("password"))).sendKeys(password);
        wait.until(ExpectedConditions.elementToBeClickable(xpath("//button[@class='btn btn-main btn-lg']//div"))).click();

        //Get envelope ID
        envelopeID = getEnvelopeId();

        // Click on here link to move towards putting signature on the document
        elem = driver.findElement(xpath("//button[@class='btn btn-main btn-lg ng-scope']//span[@class='ng-binding'][contains(text(),'Next')]"));
        java.lang.Thread.sleep(5000);
        elem.click();
        Actions act = new Actions(driver);


        // Creating an array of all the recipients
        String[] emails = new String[0];
        if (recipientsEmail != null) {
            emails = recipientsEmail.split(",");

        }

        // Put the signature modal on the document for each recipients
        for (String email : emails) {
            wait.until(ExpectedConditions.elementToBeClickable(cssSelector("#btnRecipients"))).click();
            elem = driver.findElement(xpath("//span[contains(text(),'" + email + "')]"));
            if (elem != null) {
                elem.click();
                elem = driver.findElement(xpath("//div[@class='content_sidebar content_sidebar-left resizable ng-scope ng-isolate-scope']//div[2]//div[1]//ul[1]//li[1]//button[1]"));
                Thread.sleep(2000);
                elem.click();
                Thread.sleep(2000);
                elem = driver.findElement(xpath("//div[@class='content_main']"));
                act.moveToElement(elem);

                act.release().build().perform();
            } else {
                break;
            }
        }

        // Click on the button to complete the process.
        elem = driver.findElement(xpath("//button[@class='btn btn-main btn-lg ng-scope']"));
        act.moveToElement(elem).click().perform();
        java.lang.Thread.sleep(2000);

        logger.info("File has been successfully sent to recipients from Docusign website");
        return true;

    }

}
