package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.api.autoExtraction.ContractShow;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AEExtractionSummary {
    private final static Logger logger = LoggerFactory.getLogger(AEExtractionSummary.class);
    static String configAutoExtractionFilePath;
    static String configAutoExtractionFileName;
    static String contractCreationConfigFilePath;
    static String contractCreationConfigFileName;
    static String contractCreationConfigFileNameVfSandbox;
    String templateFileName;
    static Integer documentCount;
    static int sumOfDocuments;
    static String entity;
    static String entityForVFSandbox;
    private static String relationId;
    private static String relationIdForVFSandbox;
    private static String relationIdForVFProd;
    private static String statusName;
    static Integer contractId=-1;


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
        relationIdForVFProd = ParseConfigFile.getValueFromConfigFile(contractCreationConfigFilePath, contractCreationConfigFileNameVfSandbox, "contractsvfprod", "sourceid");
    }

    @Parameters("Environment")
    @Test(priority = 1, enabled = true)
    public void testAEDocumentUploadFromContracts(String environment) {
        CustomAssert csAssert = new CustomAssert();
        try {
            String templateFilePath = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "docx attachment", "fileuploadpath");
            templateFileName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "docx attachment", "fileuploadname");
            if (environment.equals("autoextraction")) {
                contractId = TestContractCreationAPI.getNewlyCreatedContractId(contractCreationConfigFilePath, contractCreationConfigFileName, entity, templateFilePath, templateFileName, relationId, true);
                logger.info("Contract Id: " +contractId);


            } else if (environment.equals("Sandbox/AEVF")) {
                contractId = TestContractCreationAPI.getNewlyCreatedContractId(contractCreationConfigFilePath, contractCreationConfigFileNameVfSandbox, entityForVFSandbox, templateFilePath, templateFileName, relationIdForVFSandbox, true);
                logger.info("Contract Id: " +contractId);
            }

            if (contractId == -1 || contractId ==0) {
                throw new SkipException("Couldn't Create Contract. Hence couldn't validate further.");
            }
        }
        catch (SkipException e)
        {
            throw new SkipException(e.getMessage());
        }

        catch (Exception e)
        {
            csAssert.assertFalse(true, "Exception while Creating a Contract" + e.getMessage());
        }
        csAssert.assertAll();
    }

    /*Test Case to check when user clicks on extraction summary it should show the extraction status with doc Doc count : TC Id: C153652 */
    @Test(priority = 2,dependsOnMethods = "testAEDocumentUploadFromContracts")
    public void testExtractionSummary()
    {
        CustomAssert csAssert = new CustomAssert();
        try{
            logger.info("Hitting View Extraction Summary API Present on Contract Show page");
            Thread.sleep(7000);
            HttpResponse extractionSummaryResponse = AutoExtractionHelper.showExtractionSummary(contractId);
            csAssert.assertTrue(extractionSummaryResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid for show Extraction Summary API");
            String extractionSummaryStr = EntityUtils.toString(extractionSummaryResponse.getEntity());
            JSONObject extractionSummaryJson = new JSONObject(extractionSummaryStr);
            JSONArray extractionSummaryJsonArray = extractionSummaryJson.getJSONObject("response").getJSONArray("statusData");
            int totalData = extractionSummaryJsonArray.length();
            Map<String,Integer> statusWithDocCount = new HashMap<>();
            if(totalData>0) {
                for (int i = 0; i < totalData; i++) {
                    statusName = (String) extractionSummaryJsonArray.getJSONObject(i).get("statusName");
                    documentCount = (Integer) extractionSummaryJsonArray.getJSONObject(i).get("count");
                    statusWithDocCount.put(statusName, documentCount);
                }

                Collection<Integer> totalCountOfDocuments = statusWithDocCount.values();
                sumOfDocuments = totalCountOfDocuments.stream().mapToInt(Integer::intValue).sum();
                logger.info("Sum of Documents in Extraction Summary Section");

            }
            else {
                throw new Exception("There is no data in Extraction Summary List, Can't validate further");
            }

        }
        catch (Exception e)
        {
            csAssert.assertFalse(true, "Exception while Checking Extraction Summary on Contract Show page" + e.getMessage());

        }
        csAssert.assertAll();
    }

    /* Verify that it shows an option "Show more Details" link when user clicks on View Summary Link :TC Id: C153654 */
    @Test(priority = 3,dependsOnMethods = "testAEDocumentUploadFromContracts")
    public void validateCountOnAEListing()
    {
        CustomAssert csAssert = new CustomAssert();
        try{
            logger.info("Now checking Show More Details Link Listing");
            HttpResponse listingResponse = AutoExtractionHelper.listDataAPILinkedWithContracts(contractId);
            csAssert.assertTrue(listingResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
            String listingResponseStr = EntityUtils.toString(listingResponse.getEntity());
            JSONObject listingResponseJson = new JSONObject(listingResponseStr);
            int dataInListing = listingResponseJson.getJSONArray("data").length();
            logger.info("Data in Extraction Summary is: " + sumOfDocuments + " Data in AE listing corresponding to contract Id : "+contractId + " is:" +dataInListing);
            csAssert.assertEquals(dataInListing,sumOfDocuments,"Mismatch in Count of Extraction Summary and Listing" + " Data in Listing is: " +dataInListing
                    + " Data in Summary Section " +sumOfDocuments);
        }
        catch (Exception e)
        {
            csAssert.assertFalse(true, "Exception occured while validating AE listdata API" + e.getMessage());

        }
        csAssert.assertAll();
    }

    //TC:C153877 End User : Verify that when user is having permission of Show duplicate docs on contract show page then show duplicate docs option should be visible on contract show page
    //TC: C153878 End User: Verify columns when user clicks on Duplicate Docs on Contract Show Page
    @Test(priority = 4,dependsOnMethods = "testAEDocumentUploadFromContracts")
    public void testShowDuplicateDocs()
    {
        CustomAssert csAssert = new CustomAssert();
        try
        {
            AutoExtractionHelper autoExtractionHelper=new AutoExtractionHelper();
            logger.info("Start Test: Validate show duplicate docs on Contract show page");
            logger.info("Getting contract document id from contract having id "+contractId);
            logger.info("Hitting show duplicate docs API on contract show page having contract Id "+contractId);
            HttpResponse showDuplicateResponse=autoExtractionHelper.showDuplicateDocs(contractId);
            csAssert.assertTrue(showDuplicateResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
            String showDuplicateResponseStr = EntityUtils.toString(showDuplicateResponse.getEntity());
            JSONObject showDuplicateResponseJson = new JSONObject(showDuplicateResponseStr);
            String[] docIdValue=showDuplicateResponseJson.getJSONObject("response").getJSONArray("data").getJSONObject(0).getString("docId").split(":;");
            String docId=docIdValue[1];
            String[] duplicateDocIdValue=showDuplicateResponseJson.getJSONObject("response").getJSONArray("data").getJSONObject(0).getString("duplicateDocId").split(":;");
            String duplicateDocId=duplicateDocIdValue[1];
            logger.info("Validating that Document id is not null");
            csAssert.assertTrue(!(docId.equalsIgnoreCase("null")),"Document id value in show duplicate viewer on contract "+contractId+" is null");
            csAssert.assertTrue(!(duplicateDocId.equalsIgnoreCase("null")),"Duplicate document id value in show duplicate viewer on contract "+contractId+" is null");
            logger.info("Hitting Automation listing API");
            HttpResponse listDataResponse=AutoExtractionHelper.aeDocListing();
            csAssert.assertTrue(listDataResponse.getStatusLine().getStatusCode()==200,"List data API Response code is invalid");
            String listDataResponseStr=EntityUtils.toString(listDataResponse.getEntity());
            JSONObject jsonObj=new JSONObject(listDataResponseStr);
            int recordColumn = ListDataHelper.getColumnIdFromColumnName(listDataResponseStr, "documentname");
            String[] idValue = jsonObj.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(recordColumn)).get("value").toString().split(":;");
            String docIdFromAEListing = idValue[1];
            csAssert.assertEquals(docIdFromAEListing,docId,"Document uploaded via contract "+ contractId+" is not being displayed on AE Listing page");
        }
        catch (Exception e)
        {
            csAssert.assertTrue(false,"Exception occurred while validating Show duplicate docs on contract show page");
        }

        finally {
            //Delete Contract
            logger.info("Deleting Newly Created Contract Entity");
            EntityOperationsHelper.deleteEntityRecord("contracts", contractId);
        }
        csAssert.assertAll();
    }


}
