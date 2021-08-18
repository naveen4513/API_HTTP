package com.sirionlabs.test.consumption;

import com.sirionlabs.api.bulkupload.Download;
import com.sirionlabs.api.bulkupload.UploadBulkData;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.invoice.InvoiceHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class Test_SIR9698 {

    private final static Logger logger = LoggerFactory.getLogger(Test_SIR9698.class);

    private String configFilePath;
    private String configFileName;

    @BeforeClass
    public void beforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestSIR9698FilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestSIR9698FileName");
    }

    //SIR9698 Download 3000 consumptions bulk template
    // and check static custom and stakeholder columns and then do bulk update
    @Test
    public void TestBulkUpdateConsumption(){

        CustomAssert customAssert = new CustomAssert();
        InvoiceHelper invoiceHelper = new InvoiceHelper();
        try {

            ArrayList<Integer> consumptionList = new ArrayList<>();

            String[] serviceDataIdsToConsider = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"service data id").split(",");

            for(int i=0;i<serviceDataIdsToConsider.length;i++) {

                invoiceHelper.getConsumptionCreated("", Integer.parseInt(serviceDataIdsToConsider[i]),consumptionList);

            }
            String outputFilePath = "src/test/resources/TestConfig/Consumptions";
            String outputFileName = "ConsumptionBulkUpdate.xlsm";
            int templateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk update template id"));
            int entityTypeId = 176;
            int provId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"provisioning id"));

            String entityIds = "";

            for(int i =0;i<consumptionList.size();i++){

                entityIds += consumptionList.get(i) + ",";
            }
            entityIds = entityIds.substring(0,entityIds.length() -1);

            Download download = new Download();

            Boolean bulkUpdateTempDownStatus = download.hitDownload(outputFilePath,outputFileName,templateId,entityTypeId,provId,entityIds);

            if(!bulkUpdateTempDownStatus){
                customAssert.assertTrue(false,"Error while downloading bulk update template for consumptions");
            }else {

                //Kill All Scheduler Tasks if Flag is On.
                UserTasksHelper.removeAllTasks();


                logger.info("Hitting Fetch API.");
                Fetch fetchObj = new Fetch();
                fetchObj.hitFetch();
                List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                //Upload Bulk Update Template
                String bulkTemplateFileName = outputFileName;
//                int templateId = 1;

                String bulkUpdateUploadResponse = BulkTemplate.uploadBulkUpdateTemplate(outputFilePath, outputFileName, entityTypeId, templateId);
                String expectedBulkUpdateUploadResponse = "success" ;
                String expectedResult = "success";


                if (bulkUpdateUploadResponse == null) {
                    logger.error("Upload Bulk Data Response for Bulk Update, Flow [{}] and Entity {} is Null.");
                    customAssert.assertTrue(false, "Upload Bulk Data Response for Bulk Update");
                } else if (!bulkUpdateUploadResponse.trim().toLowerCase().contains(expectedBulkUpdateUploadResponse.trim().toLowerCase())) {
                    logger.error("Upload Bulk Data Response received for Bulk Update does not match with required response. " +
                            "Hence not proceeding further.");
                    customAssert.assertTrue(false, "Upload Bulk Task Response received for Bulk Update does not match with required response. Hence not proceeding further.");
                } else if (expectedResult.trim().equalsIgnoreCase("success") || expectedResult.trim().equalsIgnoreCase("failure") ||
                        expectedResult.trim().equalsIgnoreCase("recordsProcessFalse")) {

                    logger.info("Hitting Fetch API to Get Bulk Update Job Task Id for Flow ");
                    fetchObj.hitFetch();
                    logger.info("Getting Task Id of Bulk Update Job for Flow ");
                    int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);
                    long schedulerTimeOut = 180000L;        //3 mins
                    invoiceHelper.waitForScheduler("",newTaskId,5000,schedulerTimeOut, customAssert);

                    fetchObj.hitFetch();

                    String fetchResponse = fetchObj.getFetchJsonStr();
                    Boolean recordsFailed = UserTasksHelper.anyRecordFailedInTask(fetchResponse, newTaskId);

                    if(recordsFailed){
                        customAssert.assertTrue(false,"Some records failed during bulk update");
                    }

                }

            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while testing Bulk Update Consumption "  +e.getStackTrace());
        }

        customAssert.assertAll();
    }


}
