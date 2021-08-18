package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.helper.autoextraction.DocumentShowPageHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.LinkedList;
import java.util.List;

public class ViewExtractedTextLink {
    private final static Logger logger = LoggerFactory.getLogger(ViewExtractedTextLink.class);
    static String configAutoExtractionFilePath;
    static String configAutoExtractionFileName;
    static String contractCreationConfigFilePath;
    static String contractCreationConfigFileName;
    static String contractCreationConfigFileNameVfSandbox;
    static Integer contractId,docId;
    static String entity;
    static String entityForVFSandbox;
    static String templateFileName;
    private static String relationId;
    private static String relationIdForVFSandbox;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        configAutoExtractionFilePath = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFilePath");
        configAutoExtractionFileName = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFileName");
        contractCreationConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("contactCreationConfigFilePath");
        contractCreationConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("contactCreationConfigFileName");
        contractCreationConfigFileNameVfSandbox = ConfigureConstantFields.getConstantFieldsProperty("contactCreationConfigFileNameVFSandbox");
        entity = ParseConfigFile.getValueFromConfigFile(contractCreationConfigFilePath, contractCreationConfigFileName, "entitiytocreate");
        entityForVFSandbox = ParseConfigFile.getValueFromConfigFile(contractCreationConfigFilePath, contractCreationConfigFileName, "entitiytocreate");
        relationId = ParseConfigFile.getValueFromConfigFile(contractCreationConfigFilePath, contractCreationConfigFileName, "contracts", "sourceid");
        relationIdForVFSandbox = ParseConfigFile.getValueFromConfigFile(contractCreationConfigFilePath, contractCreationConfigFileNameVfSandbox, "contracts", "sourceid");
    }

    @Parameters("Environment")
    @Test(priority = 1)
    public void viewExtractedTextLinkOnContractShowpage(String environment) {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Creating a new Contract to check view extracted Text Link");
        String actualDocName=null;
        try {
            String templateFilePath = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "docx attachment", "fileuploadpath");
            templateFileName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "docx attachment", "fileuploadname");
            if (environment.equals("autoextraction")) {
                contractId = TestContractCreationAPI.getNewlyCreatedContractId(contractCreationConfigFilePath, contractCreationConfigFileName, entity, templateFilePath, templateFileName, relationId, true);
            } else if (environment.equals("Sandbox/AEVF")) {
                contractId = TestContractCreationAPI.getNewlyCreatedContractId(contractCreationConfigFilePath, contractCreationConfigFileNameVfSandbox, entityForVFSandbox, templateFilePath, templateFileName, relationIdForVFSandbox, true);
            }
            if (contractId == -1) {
                throw new Exception("Couldn't Create Contract. Hence couldn't validate further.");
            }
            logger.info("Checking whether Extraction Status is complete or not");
            if (!(contractId == -1)) {
                boolean isExtractionCompletedForUploadedFile = AEPorting.getExtractionStatus(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
                csAssert.assertTrue(isExtractionCompletedForUploadedFile, "Extraction not Completed");
                if (isExtractionCompletedForUploadedFile) {

                    try{
                        logger.info("Hitting View Extracted Data API for Contracts");
                        HttpResponse viewExtractedDataLinkResponse = AutoExtractionHelper.viewExtractedTextLink(contractId);
                        csAssert.assertTrue(viewExtractedDataLinkResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
                        String viewExtractedTextStr = EntityUtils.toString(viewExtractedDataLinkResponse.getEntity());
                        JSONObject viewExtractedTextJson = new JSONObject(viewExtractedTextStr);
                        int countOfDocuments = viewExtractedTextJson.getJSONArray("response").length();
                        for(int i=0;i<countOfDocuments;i++) {
                             actualDocName = (String) viewExtractedTextJson.getJSONArray("response").getJSONObject(i).getJSONObject("document").get("name");
                        }
                        csAssert.assertEquals(actualDocName,templateFileName,"Document Names are different that are being uploaded in Contract - DocumentName: "+templateFileName +
                                " Document Name in View Extracted Text link is : "+actualDocName);
                        docId = (Integer) viewExtractedTextJson.getJSONArray("response").getJSONObject(0).getJSONObject("document").getJSONObject("customData").getJSONObject("autoExtractionDocId").get("aedocid");

                    }
                    catch (Exception e)
                    {
                        csAssert.assertTrue(false, "View Extracted Data Text Link is API is not working" + e.getMessage());

                    }
                }

            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Contract Create API is not working" + e.getMessage());
        }
        csAssert.assertAll();

    }
    @Test(dependsOnMethods = "viewExtractedTextLinkOnContractShowpage",priority = 2)
    public void checkDataInEachTab()
    {
        CustomAssert csAssert = new CustomAssert();
        try{
            logger.info("Checking the Data in Metadata Tab of " + docId);
            List<String> tabResponseStr = new LinkedList<>();
            DocumentShowPageHelper crudHelper = new DocumentShowPageHelper(String.valueOf(docId));
            tabResponseStr.add(crudHelper.getMetadataTabResponse(String.valueOf(docId))) ;
            tabResponseStr.add(crudHelper.getClauseTabResponse(String.valueOf(docId)));
            for(String res : tabResponseStr)
            {
                Boolean isValidJSon = ParseJsonResponse.validJsonResponse(res);
                csAssert.assertTrue(isValidJSon,"Response is Invalid for tab List Data API");

            }
        }
        catch (Exception e)
        {
            csAssert.assertTrue(false, "Tab List Data API is not working" + e.getMessage());

        }
        csAssert.assertAll();
    }

    @Test(dependsOnMethods = "viewExtractedTextLinkOnContractShowpage",priority = 3)
    public void checkDocViewerOfAEDoc()
    {
        CustomAssert csAssert = new CustomAssert();
        try{
            logger.info("Validating the Doc viewer when user Clicks on View Extracted Text Link");
            HttpResponse checkDocViewerResponse = AutoExtractionHelper.validateDocViewer(docId);
            csAssert.assertTrue(checkDocViewerResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid for Doc Viewer API");
            String checkDocViewerStr = EntityUtils.toString(checkDocViewerResponse.getEntity());
            JSONObject checkDocViewerJson = new JSONObject(checkDocViewerStr);
            String documentURL = (String) checkDocViewerJson.getJSONObject("data").get("documentURL");
            csAssert.assertTrue(!documentURL.isEmpty(),"Not getting Document URL in response of Doc Viewer API, Result in Response " +documentURL);

        }
        catch (Exception e)
        {
            csAssert.assertTrue(false, "Exception occured while validating Doc Viewer API " + e.getMessage());

        }
        finally {
            //Delete Contract
            logger.info("Deleting Newly Created Contract Entity");
            EntityOperationsHelper.deleteEntityRecord("contracts", contractId);
        }
        csAssert.assertAll();
    }

}
