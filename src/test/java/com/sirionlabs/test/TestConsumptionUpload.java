package com.sirionlabs.test;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.DateUtils;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;


import javax.swing.plaf.synth.SynthTextAreaUI;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Listeners(value = MyTestListenerAdapter.class)
public class TestConsumptionUpload {

    private final static Logger logger = LoggerFactory.getLogger(TestConsumptionUpload.class);
    private static String configFilePath;
    private static String configFileName;
    private static Long waittimeout;
    private static Long pollingtime;
    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConsumptionConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("ConsumptionConfigFileName");
        waittimeout = Long.parseLong(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"waittimeout"));
        pollingtime = Long.parseLong(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"pollingtime"));
    }

    @Test(enabled = true)
    public void testBulkUploadConsumptionWrongFileType(){

        BulkTemplate bulkTemplate = new BulkTemplate();
        CustomAssert csAssert = new CustomAssert();

        try {
            String bulkTemplateConsumptionResponse = null;
            String templateFilePath = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"consumptionupload", "ConsumptionUploadFilePath");
            String templateFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"consumptionupload", "ConsumptionUploadFileNameIncExtn");
            int entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "entityTypeId"));
            int templateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "templateId"));
           // bulktemplateConsumptionResponse = bulkTemplate.uploadConsumptionTemplate(templateFilePath,templateFileName,entityTypeId,templateId);
            bulkTemplateConsumptionResponse = bulkTemplate.uploadBulkUpdateTemplate(templateFilePath,templateFileName,entityTypeId,templateId);
            String expectedMessageIncFileExtn = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"consumptionupload", "ConsumptionUploadIncorrectFileExtnMessage");
            if(bulkTemplateConsumptionResponse.contains(expectedMessageIncFileExtn)) {
                logger.info("Valid error message while uploading a file with incorrect File Type for Consumption");
                csAssert.assertTrue(true, "Valid error message while uploading a file with incorrect File Type for Consumption");
            }else {
                logger.error("Invalid error message while uploading a file with incorrect File Type for Consumption");
                csAssert.assertTrue(false, "Invalid error message while uploading a file with incorrect File Type for Consumption");
            }
        }catch (Exception ex){
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            csAssert.assertTrue(false, "Exception while uploading consumption file\n" + errors.toString());
        }
        csAssert.assertAll();
    }

    @Test(enabled = true,priority = 0)
    public void testBulkUploadConsumptionExtraHeader(){

        BulkTemplate bulkTemplate = new BulkTemplate();
        CustomAssert csAssert = new CustomAssert();

        try {
            String bulkTemplateConsumptionResponse = null;
            String templateFilePath = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"consumptionupload", "ConsumptionUploadFilePath");
            String templateFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"consumptionupload", "ConsumptionUploadFileNameExtraHeader");
            int entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "entityTypeId"));
            int templateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "templateId"));
            // bulktemplateConsumptionResponse = bulkTemplate.uploadConsumptionTemplate(templateFilePath,templateFileName,entityTypeId,templateId);
            bulkTemplateConsumptionResponse = bulkTemplate.uploadBulkUpdateTemplate(templateFilePath,templateFileName,entityTypeId,templateId);
            String expectedMessageExtraHeader = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"consumptionupload", "ConsumptionUploadIncorrectFileHeadersMessage");
            if(bulkTemplateConsumptionResponse.contains(expectedMessageExtraHeader)) {
                logger.info("Valid error message while uploading a file with extra headers");
                csAssert.assertTrue(true, "Valid error message while uploading a file with extra headers");
            }else {
                logger.error("Invalid error message while uploading a file with extra headers , actual message {}",bulkTemplateConsumptionResponse);
                csAssert.assertTrue(false, "Invalid error message while uploading a file with extra headers");
            }
        }catch (Exception ex){
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            csAssert.assertTrue(false, "Exception while uploading consumption file\n" + errors.toString());
        }
        csAssert.assertAll();
    }

    @Test(enabled = true)//passed and done
    public void testBulkUploadConsumptionValidData(){

        BulkTemplate bulkTemplate = new BulkTemplate();
        CustomAssert csAssert = new CustomAssert();

        try {
            String bulkTemplateConsumptionResponse = null;
            String templateFilePath = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"consumptionupload", "ConsumptionUploadFilePath");
            String templateFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"consumptionupload", "ConsumptionUploadFileNameValidData");
            int entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "entityTypeId"));
            int templateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "templateId"));

            String fetchJobSchStr =  UserTasksHelper.fetchJobScheduler();
            List<Integer> oldTaskId = UserTasksHelper.getAllTaskIds(fetchJobSchStr);

            bulkTemplateConsumptionResponse = bulkTemplate.uploadBulkUpdateTemplate(templateFilePath,templateFileName,entityTypeId,templateId);

            if(bulkTemplateConsumptionResponse.contains("Template is not correct")){

                logger.error("Incorrect Template while bulk upload");
                csAssert.assertTrue(false,"Incorrect Template while bulk upload Consumption");
                csAssert.assertAll();
                return;
            }

            fetchJobSchStr =  UserTasksHelper.fetchJobScheduler();

            int newTaskId =  UserTasksHelper.getNewTaskId(fetchJobSchStr,oldTaskId);


            Map<String,String> jobStatuses = UserTasksHelper.waitForScheduler(waittimeout,pollingtime,newTaskId);
            String jobStatus = jobStatuses.get("jobPassed");
            if(jobStatus.equals("true")){
                logger.info("Consumption data uploaded successfully");
            }
            else {
                logger.error("Error while uploading Consumption data");
            }

            String expectedMessageValidData = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"consumptionupload" ,"ConsumptionUploadValidHeaderMessage");
            if(bulkTemplateConsumptionResponse.contains(expectedMessageValidData)) {
                logger.info("Valid message while uploading a consumption file with valid data");
                logger.info("Consumption Data uploaded successfully");
                csAssert.assertTrue(true, "Consumption Data uploaded successfully");
            }else {
                logger.error("Consumption Data uploaded unsuccessfully");
                csAssert.assertTrue(false, "Consumption Data uploaded unsuccessfully");
            }

            logger.info("Validating the show page for consumption uploaded");
            String showPageId = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"consumptionupload", "showpageidconsumptionupload");
            String showPageResponse = getShowResponse(entityTypeId,Integer.parseInt(showPageId));

            String consumptionValAct = new JSONObject(showPageResponse).getJSONObject("body").getJSONObject("data").getJSONObject("finalConsumption").get("values").toString();
            if(consumptionValAct.equals(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"consumptionupload", "consumptionvalue"))){
                logger.info("Final Consumption value updated correctly on the consumptions with show page Id" + showPageId);
                csAssert.assertTrue(true,"Final Consumption value updated correctly on the consumptions with show page Id" + showPageId);
            }
            else {
                logger.error("Final Consumption value updated incorrectly on the consumptions with show page Id" + showPageId);
                csAssert.assertTrue(false,"Final Consumption value updated incorrectly on the consumptions with show page Id" + showPageId);
            }
        }catch (Exception ex){
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            csAssert.assertTrue(false, "Exception while uploading consumption file\n" + errors.toString());
        }
        csAssert.assertAll();
    }

    @Test(enabled = true, dependsOnMethods = "testBulkUploadConsumptionValidData")
    public void testAuditLogConsumptions(){

        CustomAssert csAssert = new CustomAssert();
        logger.info("Validating the audit logs updated for uploaded consumptions");
        try {
            int entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "entityTypeId"));
            int templateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "templateId"));

            String payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{}}}";
            String consumptionAuditResponse = getListTabResponse(61,entityTypeId,416379,payload);
            JSONArray dataarray = new JSONObject(consumptionAuditResponse).getJSONArray("data");
            int i = 0;
            int j;
            int k =0;
            String currentDate = DateUtils.getCurrentDateInMM_DD_YYYY();
            String user = ConfigureEnvironment.getEnvironmentProperty("j_username");
            user = user.replace("_"," ");
            int passtest = 0;
            loop:
            {
                while (i < dataarray.length()) {
                    JSONObject jboj1 = (JSONObject) dataarray.get(i);
                    JSONArray jarray = JSONUtility.convertJsonOnjectToJsonArray(jboj1);
                    j = 0;
                    while (j < jarray.length()) {
                        JSONObject jobj2 = (JSONObject) jarray.get(j);
                        if (jobj2.get("columnName").toString().equals("audit_log_date_created")) {

                            String dateFromResponse = jobj2.get("value").toString().substring(0, 10);
                            if (dateFromResponse.equals(currentDate)) {
                                k = 0;
                                logger.info("Validating the audit log response for current consumption bulk upload");
                                while (k < jarray.length()) {
                                    jobj2 = (JSONObject) jarray.get(k);

                                    String col = jobj2.get("columnName").toString();
                                    if (col.equals("action_name")) {
                                        if (jobj2.get("value").toString().equals("Updated (Bulk)")) {

                                            csAssert.assertTrue(true, "Valid value for action_name after pricing data upload");
                                            passtest = passtest + 1;
                                        } else {
                                            csAssert.assertTrue(false, "Invalid value for action_name after pricing data upload");
                                        }
                                    }
                                    if (col.equals("requested_by")) {
                                        if (jobj2.get("value").toString().equalsIgnoreCase(user)) {
                                            csAssert.assertTrue(true, "Valid value for user after pricing data upload");
                                            passtest = passtest + 1;
                                        } else {
                                            csAssert.assertTrue(false, "Invalid value for user after pricing data upload");
                                        }
                                    }
                                    if (col.equals("completed_by")) {
                                        if (jobj2.get("value").toString().equalsIgnoreCase(user)) {
                                            passtest = passtest + 1;
                                            csAssert.assertTrue(true, "Valid value for completed by after pricing data upload");
                                        } else {
                                            csAssert.assertTrue(false, "Invalid value for completed by after pricing data upload");
                                        }
                                    }
                                    if (col.equals("audit_log_user_date")) {
                                        if (jobj2.get("value").toString().substring(0, 10).equals(currentDate)) {
                                            passtest = passtest + 1;
                                            csAssert.assertTrue(true, "Valid value for audit_log_user_date after pricing data upload");
                                        } else {
                                            csAssert.assertTrue(false, "Invalid value for audit_log_user_date after pricing data upload");
                                        }
                                    }
                                    if (col.equals("comment")) {
                                        if (jobj2.get("value").toString().equals("No")) {
                                            passtest = passtest + 1;
                                            csAssert.assertTrue(true, "Valid value for comment after pricing data upload");
                                        } else {
                                            csAssert.assertTrue(false, "Invalid value for comment after pricing data upload");
                                        }

                                    }
                                    if (col.equals("document")) {
                                        if (jobj2.get("value").toString().equals("No")) {
                                            csAssert.assertTrue(true, "Valid value for document after pricing data upload");
                                            passtest = passtest + 1;
                                        } else {
                                            csAssert.assertTrue(false, "Invalid value for document after pricing data upload");
                                        }
                                    }
                                    k++;
                                }
                                if(k > 0 && passtest != 6){
                                    logger.error("Error validating audit logs for consumptions");
                                    csAssert.assertTrue(false,"Error validating audit logs for consumptions");
                                    break loop;
                                }
                                else{
                                    logger.info("Audit logs validated for consumptions successfully");
                                    csAssert.assertTrue(true,"Audit logs validated for consumptions successfully");
                                    break loop;
                                }
                            }
                        }
                        j++;
                    }
                    i++;
                }
            }

        }catch (Exception ex){
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            csAssert.assertTrue(false, "Exception while uploading consumption file\n" + errors.toString());
        }
        csAssert.assertAll();

    }

    @Test(enabled = false)
    public void testBulkUploadPricingData(){

        BulkTemplate bulkTemplate = new BulkTemplate();
        CustomAssert csAssert = new CustomAssert();
        String tabListResponse;
        int k = 1;
        while (k <= 2) {
        try {
            //loop is there to test for price upload 2 times as it might return old values

            int entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "consumptionupload", "pricingentityid"));
            int templateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "consumptionupload", "pricingtemplateid"));
            String templateFilePath = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "consumptionupload", "ConsumptionUploadFilePath");

                String bulkTemplateResponse = null;

                String PriceUploadFile = "PricingUpload" + k;
                String PricingUploadFile = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "consumptionupload", PriceUploadFile);
                // bulktemplateConsumptionResponse = bulkTemplate.uploadConsumptionTemplate(templateFilePath,templateFileName,entityTypeId,templateId);

            String fetchJobSchStr =  UserTasksHelper.fetchJobScheduler();
            List<Integer> oldTaskId = UserTasksHelper.getAllTaskIds(fetchJobSchStr);

            bulkTemplateResponse = bulkTemplate.uploadBulkUpdateTemplate(templateFilePath,PricingUploadFile,entityTypeId,templateId);
            Thread.sleep(10000);

            fetchJobSchStr =  UserTasksHelper.fetchJobScheduler();

            int newTaskId =  UserTasksHelper.getNewTaskId(fetchJobSchStr,oldTaskId);

            String expectedMessage = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "consumptionupload", "PricingUploadValidHeaderMessage");

                if (bulkTemplateResponse.contains(expectedMessage)) {
                    logger.info("Valid message while uploading pricing data file");
                    csAssert.assertTrue(true, "Valid message while uploading pricing data file");
                } else {
                    logger.error("Invalid message while uploading pricing data file");
                    csAssert.assertTrue(false, "Invalid message while uploading pricing data file");
                }

                ListRendererListData list = new ListRendererListData();

                String payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":20,\"filterJson\":{\"214\":{\"entityFieldHtmlType\":null,\"entityFieldId\":null,\"filterId\":214,\"filterName\":\"pricingVersion\",\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1166\",\"name\":\"Original\"}]}}},\"orderByColumnName\":\"enddate\",\"orderDirection\":\"desc nulls last\"}}";

                int serviceDataChargesTabId = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "consumptionupload", "showpageservicedatalistid"));
                int tabListId = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "consumptionupload", "showpageservicedatatablistid"));
                int id = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "consumptionupload", "showpageservicedataid"));

                list.hitListRendererTabListData(serviceDataChargesTabId, tabListId, id, payload);
                tabListResponse = list.getListDataJsonStr();

                JSONArray jarray = new JSONObject(tabListResponse).getJSONArray("data");
                JSONArray jarray2;
                int i = 0;
                int j = 0;
                JSONObject jboj;
                while (i < jarray.length()) {

                    jboj = jarray.getJSONObject(i);
                    jarray2 = JSONUtility.convertJsonOnjectToJsonArray(jboj);
                    while (j < jarray2.length()) {
                        if (jarray2.getJSONObject(j).get("columnName").toString().equals("volume")) {
                            String vol = jarray2.getJSONObject(j).get("value").toString();
                            vol = vol.substring(0, 3);
                            String volume = "volume" + k;
                            if (vol.equals(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "pricingupload", volume))) {
                                logger.info("Volume value updated successfully on the service data");
                                csAssert.assertEquals(vol, ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "pricingupload", volume));
                            } else {
                                csAssert.assertEquals(vol, ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "pricingupload", volume));
                                logger.error("Volume value updated unsuccessfully on the service data");
                            }

                        }
                        j++;
                    }
                    i++;
                }

            }catch(Exception ex){
                StringWriter errors = new StringWriter();
                ex.printStackTrace(new PrintWriter(errors));
                csAssert.assertTrue(false, "Exception while uploading pricing data file\n" + errors.toString());
            }

            k++;
        }
        csAssert.assertAll();
    }

    @Test(enabled = true)
    public void testBulkUploadConsumptionIncorrectID(){

        BulkTemplate bulkTemplate = new BulkTemplate();
        CustomAssert csAssert = new CustomAssert();

        try {
            String bulktemplateConsumptionResponse = null;
            String templateFilePath = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"consumptionupload", "ConsumptionUploadFilePath");
            String templateFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"consumptionupload", "ConsumptionUploadFileNameIncorrectID");
            int entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "entityTypeId"));
            int templateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "templateId"));
            // bulktemplateConsumptionResponse = bulkTemplate.uploadConsumptionTemplate(templateFilePath,templateFileName,entityTypeId,templateId);

            String fetchJobSchStr =  UserTasksHelper.fetchJobScheduler();
            List<Integer> oldTaskId = UserTasksHelper.getAllTaskIds(fetchJobSchStr);

            bulktemplateConsumptionResponse = bulkTemplate.uploadBulkUpdateTemplate(templateFilePath,templateFileName,entityTypeId,templateId);

            fetchJobSchStr =  UserTasksHelper.fetchJobScheduler();

            int newtaskId =  UserTasksHelper.getNewTaskId(fetchJobSchStr,oldTaskId);
            Map<String,String> jobMap = UserTasksHelper.waitForScheduler(1200000L,20L,newtaskId);
            JSONArray jobstatusArray = new JSONObject(fetchJobSchStr).getJSONObject("pickedTasksBox").getJSONArray("currentDayUserTasks");
            int failedrecordsCount = Integer.parseInt(jobstatusArray.getJSONObject(0).get("failedRecordsCount").toString());
            if(failedrecordsCount > 0){
                logger.info("Incorrect ID records failed to upload");
            }
            else {
                logger.error("Incorrect ID records updated");
            }
            String jobStatus = jobMap.get("jobPassed");
            if(jobStatus.equals("false")){
                csAssert.assertTrue(true, "Job Status is in failed status when incorrect id records updated");
                logger.info("Job Status is in failed status when incorrect id records updated");
            }
            else {
                csAssert.assertTrue(false, "Job Status is not in failed status when incorrect id records updated");
                logger.error("Job Status is not in failed status when incorrect id records updated");
            }

        }catch (Exception ex){
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            csAssert.assertTrue(false, "Exception while uploading consumption file\n" + errors.toString());
        }
        csAssert.assertAll();
    }

    @Test(enabled = true)
    public void testBulkUploadConsumptionMandFieldAbs(){

        BulkTemplate bulkTemplate = new BulkTemplate();
        CustomAssert csAssert = new CustomAssert();

        try {
            String bulktemplateConsumptionResponse = null;
            String templateFilePath = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"consumptionupload", "ConsumptionUploadFilePath");
            String templateFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"consumptionupload", "ConsumptionUploadFileNameMandFieldAbs");
            int entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "entityTypeId"));
            int templateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "templateId"));
            // bulktemplateConsumptionResponse = bulkTemplate.uploadConsumptionTemplate(templateFilePath,templateFileName,entityTypeId,templateId);

            String fetchJobSchStr =  UserTasksHelper.fetchJobScheduler();
            List<Integer> oldTaskId = UserTasksHelper.getAllTaskIds(fetchJobSchStr);

            bulktemplateConsumptionResponse = bulkTemplate.uploadBulkUpdateTemplate(templateFilePath,templateFileName,entityTypeId,templateId);

            fetchJobSchStr =  UserTasksHelper.fetchJobScheduler();

            int newtaskId =  UserTasksHelper.getNewTaskId(fetchJobSchStr,oldTaskId);
            int i =0;
            JSONArray jobstatusArray = new JSONObject(fetchJobSchStr).getJSONObject("pickedTasksBox").getJSONArray("currentDayUserTasks");
            Boolean mandatoryRecordsCheckPassed = false;
            while (i<jobstatusArray.length())
            {
                int failedrecordsCount = Integer.parseInt(jobstatusArray.getJSONObject(i).get("failedRecordsCount").toString());

                if(failedrecordsCount > 0){
                    mandatoryRecordsCheckPassed = true;
                    break;
                }
                i++;
            }
            if(mandatoryRecordsCheckPassed == true){

                logger.info("Empty Mandatory Field records failed to upload");
            }
            else {
                logger.error("Empty Mandatory Field records updated successfully");
            }

        }catch (Exception ex){
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            csAssert.assertTrue(false, "Exception while uploading consumption file\n" + errors.toString());
        }
        csAssert.assertAll();
    }

    @Test(enabled = true)
    public void testBulkUploadConsumptionExceedConsumptionValues(){

        BulkTemplate bulkTemplate = new BulkTemplate();
        CustomAssert csAssert = new CustomAssert();

        try {
            String bulkTemplateConsumptionResponse = null;
            String templateFilePath = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"consumptionupload", "ConsumptionUploadFilePath");
            String templateFileName = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,"consumptionupload", "ConsumptionUploadFileNameExceedConsValues");
            int entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "entityTypeId"));
            int templateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "templateId"));
            // bulktemplateConsumptionResponse = bulkTemplate.uploadConsumptionTemplate(templateFilePath,templateFileName,entityTypeId,templateId);

            String fetchJobSchStr =  UserTasksHelper.fetchJobScheduler();
            List<Integer> oldTaskId = UserTasksHelper.getAllTaskIds(fetchJobSchStr);

            bulkTemplateConsumptionResponse = bulkTemplate.uploadBulkUpdateTemplate(templateFilePath,templateFileName,entityTypeId,templateId);

            fetchJobSchStr =  UserTasksHelper.fetchJobScheduler();

            int newTaskId =  UserTasksHelper.getNewTaskId(fetchJobSchStr,oldTaskId);
            int i =0;
            JSONArray jobStatusArray = new JSONObject(fetchJobSchStr).getJSONObject("pickedTasksBox").getJSONArray("currentDayUserTasks");
            Boolean ExceedConsCheckPassed = false;
            while (i < jobStatusArray.length())
            {
                int failedRecordsCount = Integer.parseInt(jobStatusArray.getJSONObject(i).get("failedRecordsCount").toString());

                if(failedRecordsCount > 0){
                    ExceedConsCheckPassed = true;
                    break;
                }
                i++;
            }
            if(ExceedConsCheckPassed == true){

                logger.info("Empty Mandatory Field records failed to upload");
            }
            else {
                logger.error("Empty Mandatory Field records updated successfully");
            }

            Map<String,String> jobMap = UserTasksHelper.waitForScheduler(1200000L,20L,newTaskId);
            String jobStatus = jobMap.get("jobPassed");
            if(jobStatus.equals("false")){
                logger.info("Scheduler Job getting failed when consumption data upload unsuccessfull due to exceeded values in consumption upload sheet");
            }
            else {
                logger.error("Scheduler Job getting passed when consumption data upload unsuccessfull due to exceeded values in consumption upload sheet");
            }

        }catch (Exception ex){
            StringWriter errors = new StringWriter();
            ex.printStackTrace(new PrintWriter(errors));
            csAssert.assertTrue(false, "Exception while uploading consumption file\n" + errors.toString());
        }
        csAssert.assertAll();
    }

    public String getListTabResponse(int listId, int tablistId,int id,String payload) {
        ListRendererListData list = new ListRendererListData();
        list.hitListRendererTabListData(listId,tablistId,id,payload);

        return list.getListDataJsonStr();

    }

    public String getShowResponse(int entityTypeId, int dbId) {
        Show showObj = new Show();
        showObj.hitShow(entityTypeId, dbId);

        return showObj.getShowJsonStr();

    }
}
