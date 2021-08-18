package com.sirionlabs.test.bulkCreate;

import com.mifmif.common.regex.Generex;
import com.sirionlabs.api.bulkupload.Download;
import com.sirionlabs.api.bulkupload.UploadBulkData;
import com.sirionlabs.api.commonAPI.CreateLinks;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.lang3.math.NumberUtils;
import org.codehaus.stax2.ri.typed.NumberUtil;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

public class BulkCreateAllEntities {

    private final Logger logger = LoggerFactory.getLogger(BulkCreateAllEntities.class);
    final String configFilePath = "src/test/resources/TestConfig/BulkCreate";
    final String configFileName = "BulkCreateAllEntitiesConfig.cfg";
    final String envDetailsConfigFileName = "EnvironmentsServerDetails.cfg";
    Map<String,String> createLinksData = new HashMap<>();
    Map<String,String> fieldTypeDetails = new HashMap<>();
    Map<String,String> fieldValidationIDDetails = new HashMap<>();
    Map<String,List<String>> fieldValidationDetails = new HashMap<>();
    private String outputFilePath = "src/test/resources/TestConfig/ServiceData/Pricing";
    private String outputFileName = "file.xlsm";

    @BeforeClass
    public void beforeClass(){
        try{
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();

            String query = "select field_id,editable_html_type from request_field_mapping;";
                    //"select field_html_type.id,entity_field.id,entity_field.entity_type_id  from entity_field left join request_field_mapping on entity_field.id = request_field_mapping.field_id left join field_html_type on request_field_mapping.editable_html_type = field_html_type.id where entity_field.active order by entity_field.id desc;";
            List<List<String>> results = sqlObj.doSelect(query);

            for(List<String> tempList : results){
//                if (!fieldTypeDetails.containsKey(tempList.get(2))) {
//                    fieldTypeDetails.put(tempList.get(2), new HashMap<>());
//                }
                fieldTypeDetails.put(tempList.get(0),tempList.get(1));
            }

            query = "select DISTINCT entity_field.id,entity_client_field.validation_rules from entity_field left join entity_client_field on entity_field.id = entity_client_field.field_id " +
                    "group by entity_field.id,entity_client_field.validation_rules;";

            results = sqlObj.doSelect(query);

            for(List<String> tempList : results)
                fieldValidationIDDetails.put(tempList.get(0),tempList.get(1));

            query = "select id,name,parameter,pattern from entity_field_validation;";

            results = sqlObj.doSelect(query);

            for(List<String> tempList : results)
                fieldValidationDetails.put(tempList.get(0),tempList);
        }
        catch (Exception e){
            logger.error("Exception in Before class");
        }
        logger.info("Exiting before class");
    }

    @Test
    public void testBulkCreate(){

        String downloadFilePath = "src/test/resources/TestConfig/BulkCreate", downloadFileName = null;

        CustomAssert customAssert = new CustomAssert();

        try{

            String[] entities = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"casetoexecute").split(",");
            assert entities.length>0:"No entities to work on";

            for(String entity : entities){

                int entityTypeId = Integer.parseInt(entity);
                String[] parents = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entity,"parents").split(",");
                for(String parent : parents){

                    String parentId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entity,parent);
                    assert parentId!=null:"Parent id not found";

                    String downloadAPI = null, uploadAPI = null;
                    JSONObject createLinkJson;

                    if(!createLinksData.containsKey(entityTypeId+parentId)) {
                        String createLinkResponse = CreateLinks.getCreateLinksV2Response(Integer.parseInt(parent), Integer.parseInt(parentId));
                        createLinkJson = new JSONObject(createLinkResponse);
                    }
                    else
                        createLinkJson = new JSONObject(createLinksData.get(entityTypeId+parentId));

                    try{
                        JSONArray jsonArrayTemp1 = createLinkJson.getJSONArray("fields");
                        String label = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entity,"label");
                        assert label!=null:":Label is null";
                        for(Object object : jsonArrayTemp1){
                            if(((JSONObject)object).getString("label").equalsIgnoreCase(label)) {
                                JSONArray jsonArrayTemp2 = ((JSONObject) object).getJSONArray("fields");
                                for (Object object1 : jsonArrayTemp2) {
                                    if (((JSONObject) object1).getString("label").equalsIgnoreCase("Bulk")){
                                        downloadAPI = ((JSONObject) object1).getJSONObject("properties").getString("downloadAPI");
                                        uploadAPI = ((JSONObject) object1).getJSONObject("properties").getString("uploadAPI");
                                        downloadFileName = "EntityTypeId-"+entity+"-ParentId-"+parent+".xlsm";
                                    }
                                }
                            }
                        }
                    }
                    catch (Exception e){
                        customAssert.assertTrue(false,"Exception in extracting create link");
                        customAssert.assertAll();
                    }

                    assert downloadAPI!=null&&uploadAPI!=null:"Download and Upload API is not found";
                    //Extracted the download and upload apis

                    Download download = new Download();
                    assert download.hitDownload(downloadFilePath,downloadFileName,downloadAPI):"Cannot download bulk create file "+downloadFileName;

                    assert FileUtils.fileExists(downloadFilePath,downloadFileName):"Downloaded file "+downloadFileName+" not found";

                    String[] sheets = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entity,"sheets").split(",");
                    for(String sheet:sheets){
                        String rowsToFill = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entity,"rowstofill");
                        assert rowsToFill!=null:"rowstofill is null";
                        int numberOfRowsToFill = Integer.parseInt(rowsToFill);

                        List<String> sheetFieldIds = XLSUtils.getExcelDataOfOneRow(downloadFilePath,downloadFileName,sheet,2);
                        logger.info("Sheet Field Ids {}",sheetFieldIds);
                        List<String> dropDownDetails = XLSUtils.getExcelDataOfOneRow(downloadFilePath,downloadFileName,sheet,6);
                        logger.info("Sheet Field Ids {}",sheetFieldIds);

                        assert sheetFieldIds.size()==dropDownDetails.size():"Size of sheetFieldIds and dropDownDetails doesn't match ";

                        Map<String,Object> dataToBeFilled = new HashMap<>();

                        Map<String,List<String>> masterData = XLSUtils.getMasterSheetDataUsingColumnId(downloadFilePath,downloadFileName);
                        List<String> exceptColumns = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entity,"exceptfields").split(","));
                        int rowNumber = 6;

                        while (rowNumber<numberOfRowsToFill+6){

                            try {
                                for(int columnNum = 0;columnNum<sheetFieldIds.size();columnNum++){


                                    String fieldId = sheetFieldIds.get(columnNum);

                                    if(fieldId.equalsIgnoreCase("100371")){
                                        logger.info("");
                                    }
                                    if(exceptColumns.contains(fieldId))
                                        continue;

                                    boolean isDropDown = dropDownDetails.get(columnNum).equalsIgnoreCase("Dropdown");

                                    if(fieldId.equalsIgnoreCase("100000001")){
                                        dataToBeFilled.put(fieldId,rowNumber-5);
                                        continue;
                                    }
                                    if(fieldId.equalsIgnoreCase("100000003")){
                                        dataToBeFilled.put(fieldId,rowNumber-5);
                                        continue;
                                    }
                                    if(fieldId.equalsIgnoreCase("100000002")){
                                        continue;
                                    }

                                    assert fieldTypeDetails.containsKey(fieldId):"fieldTypeDetails doesn't contain "+fieldId;

                                    String type = fieldTypeDetails.get(fieldId);

                                    if(isDropDown){
                                        if(masterData.get(fieldId)==null)
                                            continue;
                                        if(masterData.get(fieldId).size()<=0)
                                            continue;
                                        //assert masterData.get(fieldId).size()>0:"Master data not found for field id "+fieldId; todo mandatory check
                                        String tempInsert = masterData.get(fieldId).get(RandomNumbers.getRandomNumberWithinRangeIndex(0,masterData.get(fieldId).size()));
                                        dataToBeFilled.put(fieldId,tempInsert.split("::")[tempInsert.split("::").length-1]);
                                    }
                                    else if(fieldValidationIDDetails.containsKey(fieldId)){
                                        String ids = fieldValidationIDDetails.get(fieldId);
                                        if(ids.length()<=2)
                                            logger.info("ids length <= ");
                                        ids = ids.substring(1,ids.length()-1);
                                        String[] idsList = ids.split(",");
                                        if(idsList.length<=1){
                                            if(idsList[0].isEmpty())
                                                fillData(type,fieldId,dataToBeFilled,200);
                                        }
                                        for(String id : idsList){
                                            if(id.isEmpty())
                                                break;

                                            assert fieldValidationDetails.containsKey(id):"entity field validation id : "+id+" not found in entity_field_validation for field id : "+fieldId;

                                            List<String> validations = fieldValidationDetails.get(id);
                                            if(validations.get(3)!=null){
                                                //checking pattern
                                                String pattern = validations.get(3);
                                                pattern = pattern.charAt(0)=='^'&&pattern.charAt(pattern.length()-1)=='$'?pattern.substring(1,pattern.length()-1)
                                                        :(pattern.charAt(0)=='^'?pattern.substring(1):pattern.charAt(0)=='^'?pattern.substring(0,pattern.length()-1):pattern);
                                                Generex generex = new Generex(pattern);
                                                String value = generex.random();

                                                if(NumberUtils.isParsable(value)){
                                                    dataToBeFilled.put(fieldId,Double.parseDouble(value));
                                                }
                                                else{
                                                    dataToBeFilled.put(fieldId,value);
                                                }

                                            }
                                            else{
                                                assert validations.get(1)!=null:"Cannot find validation name with id "+id+" from table entity_field_validation";

                                                String validationName = validations.get(1);
                                                if(validationName.contains("Max")||validationName.contains("max")){
                                                    String parameter = validations.get(2);
                                                    assert parameter!=null:"Parameter is null for validation  with id "+id+" from table entity_field_validation";

                                                    fillData(type,fieldId,dataToBeFilled,Integer.parseInt(parameter)-1);
                                                }
                                                else if(validationName.contains("Min")||validationName.contains("min")){
                                                    String parameter = validations.get(2);
                                                    assert parameter!=null:"Parameter is null for validation  with id "+id+" from table entity_field_validation";

                                                    fillData(type,fieldId,dataToBeFilled,Integer.parseInt(parameter)+1);
                                                }
                                            }
                                        }
                                    }
                                    else{
                                        fillData(type,fieldId,dataToBeFilled,200);
                                    }
                                }

                                //filling dependent fields
                                String[] dependentFields = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entity,"dependentfields").split(",");
                                for(String dependentField : dependentFields){
                                    String temp=null;
                                    while (dependentField!=null){

                                        if(temp==null) {
                                            if (masterData.get(dependentField) != null)
                                                if (masterData.get(dependentField).size() > 0)
                                                    temp = masterData.get(dependentField).get(RandomNumbers.getRandomNumberWithinRangeIndex(0, masterData.get(dependentField).size()));
                                        }
                                        else
                                            for(int i=0;i<masterData.get(dependentField).size();i++){
                                                if(masterData.get(dependentField).get(i).split("::")[masterData.get(dependentField).get(i).split("::").length-2].equalsIgnoreCase(temp.split("::")[0])){
                                                    temp=masterData.get(dependentField).get(i);
                                                    break;
                                                }
                                            }

                                        //assert temp!=null:"Cannot find data in master sheet for field "+dependentField+" and dependent field"+temp;
                                        if(temp!=null)
                                            dataToBeFilled.put(dependentField,temp.split("::")[temp.split("::").length-1]);

                                        dependentField=ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entity,dependentField);
                                        if(dependentField==null)
                                            temp=null;
                                    }



                                }


                                String[] customValueFields = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entity,"customvaluesfields").split(",");
                                for(String customValueField : customValueFields){
                                    dataToBeFilled.put(customValueField,ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,entity,customValueField));
                                }
                            }
                            catch (Exception e){
                                customAssert.assertTrue(false,"Exception caught "+ Arrays.toString(e.getStackTrace()));
                            }

                            XLSUtils.editRowDataUsingColumnId(downloadFilePath,downloadFileName,sheet,rowNumber,dataToBeFilled);
                            rowNumber++;

                        }
                    }


                    logger.info("Hitting Fetch API.");
                    Fetch fetchObj = new Fetch();
                    fetchObj.hitFetch();
                    List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

                    UploadBulkData uploadBulkData = new UploadBulkData();
                    uploadBulkData.hitUploadBulkData(uploadAPI,downloadFilePath,downloadFileName,Integer.parseInt(parent),Integer.parseInt(parentId));
                    String response = uploadBulkData.getUploadBulkDataJsonStr();

                    logger.info("Upload Response {}",response);

                    assert response.contains("200:;basic:;Your request has been submitted"):"Upload response is not as expected";

                    int schedulerTimeOut = 600000;
                    int pollingTime = 5000;
                    String result = "pass";
                    logger.info("Time Out for Bulk Create Scheduler is {} milliseconds", schedulerTimeOut);
                    long timeSpent = 0;
                    logger.info("Hitting Fetch API.");
                    fetchObj = new Fetch();
                    fetchObj.hitFetch();
                    logger.info("Getting Task Id of Bulk Create Job");
                    int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);
                    String newRequestId = null;
                    if (newTaskId != -1) {

                        JSONObject jsonObject = new JSONObject(fetchObj.getFetchJsonStr());
                        JSONArray jsonArray = jsonObject.getJSONObject("pickedTasksBox").getJSONArray("currentDayUserTasks");
                        for(Object object : jsonArray){
                            JSONObject jsonObject1 = (JSONObject) object;
                            if(jsonObject1.getInt("id")==newTaskId){
                                newRequestId=String.valueOf(jsonObject1.getInt("requestId"));
                                break;
                            }
                        }


                        boolean taskCompleted = false;
                        logger.info("Checking if Bulk Creation Task has completed or not.");

                        while (timeSpent < schedulerTimeOut) {
                            logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                            Thread.sleep(pollingTime);

                            logger.info("Hitting Fetch API.");
                            fetchObj.hitFetch();
                            logger.info("Getting Status of Bulk Create Task.");
                            String newTaskStatus = UserTasksHelper.getStatusFromTaskJobId(fetchObj.getFetchJsonStr(), newTaskId);
                            if (newTaskStatus != null && newTaskStatus.trim().equalsIgnoreCase("Completed")) {
                                taskCompleted = true;
                                logger.info("Bulk Create Task Completed. ");
                                logger.info("Checking if Bulk Create Task failed or not.");
                                if (UserTasksHelper.ifAllRecordsFailedInTask(newTaskId))
                                    result = "fail";

                                break;
                            } else {
                                timeSpent += pollingTime;
                                logger.info("Bulk Create Task is not finished yet.");
                            }
                        }
                        if (!taskCompleted && timeSpent >= schedulerTimeOut) {
                            //Task didn't complete within given time.
                            result = "skip";
                        }
                    } else {
                        logger.info("Couldn't get Bulk Create Task Job Id. Hence waiting for Task Time Out i.e. {}", schedulerTimeOut);
                        Thread.sleep(schedulerTimeOut);
                    }

                    logger.info("The bulk create task status is {}",result);
                    logger.info("Downloading the failed excel");

                    String environment = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"environment");
                    assert environment!=null:"Environment name is null in config file";

                    String host = ParseConfigFile.getValueFromConfigFile(configFilePath,envDetailsConfigFileName,environment,"host");
                    String user = ParseConfigFile.getValueFromConfigFile(configFilePath,envDetailsConfigFileName,environment,"user");
                    String key = ParseConfigFile.getValueFromConfigFile(configFilePath,envDetailsConfigFileName,environment,"key");
                    String withKey = ParseConfigFile.getValueFromConfigFile(configFilePath,envDetailsConfigFileName,environment,"withkey");

                    assert host!=null && user!=null && key!=null && withKey!=null :"Environment details are empty";

                    SCPUtils scpUtils;
                    if(withKey.equalsIgnoreCase("yes"))
                        scpUtils = new SCPUtils(host,user,key,22,withKey.equalsIgnoreCase("yes"));
                    else
                        scpUtils = new SCPUtils(host,user,key,22);

                    scpUtils.downloadExcelFile(newRequestId,outputFileName,outputFilePath);

                    assert FileUtils.fileExists(outputFilePath,outputFileName):"Downloaded file doesn't exist";



                }
            }
        }
        catch (Exception e){
            customAssert.assertTrue(false,"Exception caught "+ Arrays.toString(e.getStackTrace()));
        }

        customAssert.assertAll();

    }

    private void fillData (String type,String fieldId,Map<String,Object> dataToBeFilled, int constraint){

       if(type.equalsIgnoreCase("10")||type.equalsIgnoreCase("1")||type.equalsIgnoreCase("2")){
            dataToBeFilled.put(fieldId,RandomString.getRandomAlphaNumericString(constraint));
        }
        else if(type.equalsIgnoreCase("18")){
            dataToBeFilled.put(fieldId,RandomNumbers.getRandomNumberWithinRangeIndex(constraint/2,constraint));
        }
        else if(type.equalsIgnoreCase("20")){
            dataToBeFilled.put(fieldId, DateTime.now());
        }
        else if(type.equalsIgnoreCase("8")){
            dataToBeFilled.put(fieldId, new Date());
        }
    }

}
