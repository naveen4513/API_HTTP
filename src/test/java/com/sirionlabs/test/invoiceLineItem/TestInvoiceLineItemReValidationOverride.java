package com.sirionlabs.test.invoiceLineItem;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.test.invoice.InvoiceValidationStatusForInvoice;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestInvoiceLineItemReValidationOverride {
    public static  final Logger logger = LoggerFactory.getLogger(TestInvoiceLineItemReValidationOverride.class);
    private CustomAssert customAssert = new CustomAssert();
    private int entityTypeId = 67;
    private Map<Integer, String> deleteEntityMap = new HashMap<>();
    private String configFilePath;
    private String configFileName;
    private String flowsConfigFilePath;
    private String flowsConfigFileName;
    private boolean killAllSchedulerTasks = false;
    private String pricingTemplateFilePath;
    private String forecastTemplateFilePath;
    private boolean failTestIfJobNotCompletedWithinSchedulerTimeOut = true;
    private String contractConfigFilePath;
    private String contractConfigFileName;
    private String contractExtraFieldsConfigFileName;
    private String serviceDataConfigFilePath;
    private String serviceDataConfigFileName;
    private String serviceDataExtraFieldsConfigFileName;
    private String invoiceConfigFilePath;
    private String invoiceConfigFileName;
    private String invoiceExtraFieldsConfigFileName;
    private String invoiceLineItemConfigFilePath;
    private String invoiceLineItemConfigFileName;
    private String invoiceLineItemExtraFieldsConfigFileName;
    private int serviceDataEntityTypeId = -1;
    private String serviceDataEntitySectionUrlName;
    private String consumptionEntitySectionUrlName;

    //	private Show show;
    private String approveAction = "approve";
    //	private Edit edit;
    private String contractEntity = "contracts";
    private String serviceDataEntity = "service data";
    private String consumptionEntity = "consumptions";
    private String invoiceEntity = "invoices";
    private String invoiceLineItemEntity = "invoice line item";

    @BeforeClass
    public void beforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceTestConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceTestConfigFileName");
        pricingTemplateFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "pricingtemplatefilepath");
        forecastTemplateFilePath = pricingTemplateFilePath;
        flowsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowsconfigfilepath");
        flowsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowsconfigfilename");

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

        //Invoice Line Item Config files
        invoiceLineItemConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceLineItemFilePath");
        invoiceLineItemConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceLineItemFileName");
        invoiceLineItemExtraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("invoiceLineItemExtraFieldsFileName");

        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "killallschedulertasks");
        if (temp != null && temp.trim().equalsIgnoreCase("true"))
            killAllSchedulerTasks = true;

        temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "failTestIfJobNotCompletedWithinSchedulerTimeOut");
        if (temp != null && temp.trim().equalsIgnoreCase("false"))
            failTestIfJobNotCompletedWithinSchedulerTimeOut = false;


        String entityIdMappingFileName;
        String baseFilePath;

        // for publishing of service data
        entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
        baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConfigFileBasePath");
        serviceDataEntitySectionUrlName = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, serviceDataEntity, "url_name");
        serviceDataEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, serviceDataEntity, "entity_type_id"));
        consumptionEntitySectionUrlName = ParseConfigFile.getValueFromConfigFile(baseFilePath, entityIdMappingFileName, consumptionEntity, "url_name");

        deleteEntityMap = new HashMap<>();
    }

    @Test
    public void LineItemValidationAfterConsumptionOverride(String flowToTest){
        try{
            int contractID = InvoiceHelper.getContractId(contractConfigFilePath,contractConfigFileName,contractExtraFieldsConfigFileName,flowToTest);
            if(contractID==-1){
                logger.info("Contract not created for flow {}, hence terminating");
                customAssert.assertFalse(true,"Contract not created for flow "+flowToTest);
            }

        }
        catch(Exception e){

        }

        customAssert.assertAll();
    }

    public boolean getLineItemValidationJobStatusFromDB(String clientId, int jobID, CustomAssert customAssert,int entityID){
        List<List<String>> result = new ArrayList<>();
        try{
            logger.info("Starting search for DB entry for task");

            String query = "select * from scheduled_job where job_id = "+jobID+" and client_id = "+clientId;
            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
            logger.info("Running query {}",query);
            result = postgreSQLJDBC.doSelect(query);
            logger.info("Result : {}",result);
            if(result.size()==0){
                customAssert.assertFalse(true,"Cannot find the task_job_id for client "+clientId+" and job id "+jobID);
                logger.info("Cannot find the task_job_id for client {} and job id {}",clientId,jobID);
                customAssert.assertAll();
            }
            //TODO change scheduled_job_id below
            query = "select * from task where scheduled_job_id = 2323 and entity_id="+entityID;
            logger.info("Running query {}",query);
            result = postgreSQLJDBC.doSelect(query);
            if(result.size()==0){
                customAssert.assertFalse(true,"Cannot find the task for entity id "+entityID+" and job id "+jobID);
                logger.info("Cannot find the task for entity id {} and job id {}",entityID,jobID);
                customAssert.assertAll();
            }
            else {
                logger.info("Search for entry in DB successful");
                return true;
            }


        }
        catch (Exception e){
            logger.info("Exception Caught while fetching Data from DB {}",e.toString());
            customAssert.assertFalse(true,"Exception Caught while fetching Data from DB "+e.toString());
        }

        return false;
    }
}
