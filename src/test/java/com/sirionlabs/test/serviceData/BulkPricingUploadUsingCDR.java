package com.sirionlabs.test.serviceData;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.entityCreation.ContractDraftRequest;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class BulkPricingUploadUsingCDR {

    private String serviceDataConfigFilePath;
    private String serviceDataConfigFileName;
    private String serviceDataExtraFieldsConfigFileName;
    private String cdrConfigFilePath;
    private String cdrConfigFileName;
    private String cdrExtraFieldsConfigFileName;
    private String contractConfigFilePath;
    private String contractConfigFileName;
    private String contractExtraFieldsConfigFileName;

    private final static Logger logger = LoggerFactory.getLogger(BulkPricingUploadUsingCDR.class);

    @BeforeClass
    public void beforeClass() throws Exception {

        //Service Data Config files
        serviceDataConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("serviceDataFilePath");
        serviceDataConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("serviceDataFileName");
        serviceDataExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataExtraFieldsFileName");

        //Service Data Config files
        serviceDataConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("serviceDataFilePath");
        serviceDataConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("serviceDataFileName");
        serviceDataExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("ServiceDataExtraFieldsFileName");


        //CDR Config files
        cdrConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("CDRFilePath");
        cdrConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("CDRFileName");
        cdrExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("CDRExtraFieldsFileName");


        //Contract Config files
        contractConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("contractFilePath");
        contractConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("contractFileName");
        contractExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("contractExtraFieldsFileName");


    }


    @Test
    public void testBulkPricingUpload(){

        CustomAssert customAssert = new CustomAssert();

        String flowToExecute = "fixed fee flow 1 qavf";
        String serviceDataEntity = "service data";
        String cdrSectionName = "presignature flow";
        String cdrEntity = "contract draft request";

        int contractId = InvoiceHelper.getContractId(contractConfigFilePath,contractConfigFileName,contractExtraFieldsConfigFileName,flowToExecute);
        customAssert.assertTrue(contractId==-1,"___________Contract not created___________");

        logger.info("Contract Created is : [{}]",contractId);
        InvoiceHelper.updateServiceDataAndInvoiceConfig(serviceDataConfigFilePath,serviceDataConfigFileName,serviceDataConfigFilePath,serviceDataConfigFileName,flowToExecute,contractId);


        int serviceDataId = InvoiceHelper.getServiceDataId(serviceDataConfigFilePath,serviceDataConfigFileName,serviceDataExtraFieldsConfigFileName,flowToExecute,contractId);
        //int serviceDataId = CreateEntity.getNewEntityId(serviceDataCreateResponse, serviceDataEntity);
        customAssert.assertTrue(serviceDataId==-1,"___________Service Data not created___________");

        logger.info("Service Data Created is : [{}]",serviceDataId);


        String cdrCreateResponse = ContractDraftRequest.createCDR(cdrConfigFilePath, cdrConfigFileName, cdrConfigFilePath,
                cdrExtraFieldsConfigFileName, cdrSectionName, false);
        int cdrId = CreateEntity.getNewEntityId(cdrCreateResponse, cdrEntity);

        logger.info("Contract Draft Request Created is : [{}]",cdrId);

        customAssert.assertAll();



    }
}
