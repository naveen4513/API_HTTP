package com.sirionlabs.test.invoice;

import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;


public class TestInvoiceValidationsCreatePage {

    private final static Logger logger = LoggerFactory.getLogger(TestInvoiceValidationsCreatePage.class);

    private String configFilePath;
    private String configFileName;
    private String contractConfigFilePath;
    private String contractConfigFileName;
    private String contractExtraFieldsConfigFileName;
    private String serviceDataConfigFilePath;
    private String serviceDataConfigFileName;
    private String serviceDataExtraFieldsConfigFileName;

    private String invoiceConfigFilePath;
    private String invoiceConfigFileName;
    private String invoiceExtraFieldsConfigFileName;

    @BeforeClass
    public void beforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceValidationsCreatePageConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceValidationsCreatePageConfigFileName");

        //Contract Config files
        contractConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("contractFilePath");
        contractConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("contractFileName");
        contractExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("contractExtraFieldsFileName");

        //Service Data Config files
        serviceDataConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("serviceDataFilePath");
        serviceDataConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("serviceDataFileName");
        serviceDataExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataExtraFieldsFileName");

        //Invoice Config files
        invoiceConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceFilePath");
        invoiceConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceFileName");
        invoiceExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceExtraFieldsFileName");
    }

    @DataProvider
    public Object[][] dataProviderForFields(){

        logger.info("Setting all absent fields to Validate");
        List<Object[]> allTestData = new ArrayList<>();
        List<String> allFlowsToTest = getAbsentFieldsToTest();
        for (String flowToTest : allFlowsToTest) {
            allTestData.add(new Object[]{flowToTest});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForFields")
    public void TestInvoiceValidationsCreatePage(String absentFieldsToValidate){

        CustomAssert csAssert = new CustomAssert();
        String sectionName = "arc flow 1";
        String invoiceEntityName = "invoices";
        int contractId = 0;
        try {
            contractId = InvoiceHelper.getContractId(contractConfigFilePath,contractConfigFileName,contractExtraFieldsConfigFileName,sectionName);

            InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath,serviceDataConfigFileName,invoiceConfigFilePath,
                    invoiceConfigFileName,sectionName,contractId);

            CreateEntity createEntity = new CreateEntity(invoiceConfigFilePath, invoiceConfigFileName, invoiceConfigFilePath,
                    invoiceExtraFieldsConfigFileName, sectionName);

            String createPayload = createEntity.getCreatePayload(invoiceEntityName, true, false);

            createPayload = updatePayload(createPayload, absentFieldsToValidate);

            Create createObj = new Create();
            createObj.hitCreate(invoiceEntityName, createPayload);
            String createResponse = createObj.getCreateJsonStr();

            JSONObject createResponseJson = new JSONObject(createResponse);
            JSONObject fieldErrorsJson = createResponseJson.getJSONObject("body").getJSONObject("errors").getJSONObject("fieldErrors");
            String expectedMessage = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,"expectedmessage",absentFieldsToValidate);
            if(fieldErrorsJson.has(absentFieldsToValidate)){
                csAssert.assertTrue(true,"Field Errors have validation error for field " + absentFieldsToValidate);

                String actualValidationErrorMessage = fieldErrorsJson.getJSONObject(absentFieldsToValidate).get("message").toString();

                if(!actualValidationErrorMessage.trim().equalsIgnoreCase(expectedMessage.trim())){
                    csAssert.assertTrue(false,"Actual message didn't match expected message");
                    csAssert.assertTrue(false,"Actual Message " + actualValidationErrorMessage + " Expected Message " + expectedMessage);
                }
            }else {
                logger.error("Field Errors doesn't have validation error");
                csAssert.assertTrue(false,"Field Errors doesn't have validation error for field " + absentFieldsToValidate);
            }

        }catch (Exception e){
            csAssert.assertTrue(false,"Exception while validating field error");
        }finally {
            EntityOperationsHelper.deleteEntityRecord("contracts",contractId);
        }
        csAssert.assertAll();
    }

    private String updatePayload(String createPayload,String fieldName){

        JSONObject createPayloadJson = new JSONObject(createPayload);

//        if(fieldName.equals("currency")){
//            createPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject(fieldName).getJSONObject("values").remove("id");
//
//        }else {
            createPayloadJson.getJSONObject("body").getJSONObject("data").getJSONObject(fieldName).put("values","");
        //}

        return createPayloadJson.toString();

    }

    private List<String> getAbsentFieldsToTest() {
        List<String> flowsToTest = new ArrayList<>();

        try {
            flowsToTest = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "absentfieldstovalidate").split(Pattern.quote(",")));
        } catch (Exception e) {
            logger.error("Exception while getting Absent Fields to Test {}", e.getMessage());
        }
        return flowsToTest;
    }

}
