package com.sirionlabs.test.SL_Stories.CustomFields;

import com.sirionlabs.api.bulkedit.BulkeditEdit;
import com.sirionlabs.api.bulkupload.Download;
import com.sirionlabs.api.bulkupload.UploadBulkData;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.metadataSearch.Search;
import com.sirionlabs.api.usertasks.Fetch;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.servicelevel.ServiceLevelHelper;

import com.sirionlabs.utils.commonUtils.CustomAssert;

import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.kafka.common.protocol.types.Field;
import org.checkerframework.checker.units.qual.C;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

public class TestNegativeValuesForCustomCurrencyFields {

    private final static Logger logger = LoggerFactory.getLogger(TestNegativeValuesForCustomCurrencyFields.class);

    private String slConfigFilePath;
    private String slConfigFileName;

    int slEntityTypeId = 14;
    int cslEntityTypeId = 15;

    String customFieldValueSl = "-10";
    String customFieldValueCSl = "-10";

    String auditLogUser = "Anay User";

    String currency = "INR";

//    int dynamicFieldSlId = 105606;          //AUTO OFFICE
    int dynamicFieldSlId = 102620;          //BA1 ENV
//    int dynamicFieldCSlId = 105607;
    int dynamicFieldCSlId = 102621;
    String dynamicFieldSl = "dyn" + dynamicFieldSlId;
    String dynamicFieldCSl = "dyn" + dynamicFieldCSlId;

    String slEntity = "service levels";
    String cslEntity = "child service levels";

    int serviceLevelId;

    String outputFilePath;

    int childServiceLevelTabId = 7;
    private ArrayList<Integer> slToDelete = new ArrayList<>();
    private ArrayList<Integer> cslToDelete = new ArrayList<>();

    @BeforeClass
    public void BeforeClass(){

        slConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFilePath");
        slConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("SLAutomationConfigFileName");

        outputFilePath = "src\\test\\resources\\TestConfig\\ServiceLevel\\BulkTemplates";

    }

    @Test(enabled = false, priority = 4) //done
    public void TestCreatePageAndShowPageSL(){

        CustomAssert customAssert = new CustomAssert();
        int serviceLevelId = -1;
        try{
            customFieldValueSl = "-10";
            String flowToTest = "sl automation flow";

            String PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"if (doc['exception'].value=='F'){if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.met++; }else{state.map.notMet++}}}else{if(doc['Time Taken (Seconds)'].size() != 0){if(doc['Time Taken (Seconds)'].value < 28800){state.map.notMet++; }else{state.map.met++}}}\",\"init_script\":\"state['map'] = ['met':0, 'notMet':0]\",\"reduce_script\":\"params.result = ['Actual_Numerator':20, 'Actual_Denominator':100, 'Supplier_Numerator':10, 'Supplier_Denominator':0, 'Final_Numerator':10, 'Final_Denominator':30]; if(params.result.Final_Denominator <= 100){params.result.Target = 100; params.result.Breach = 102; params.result.Default = 107;} return params.result\",\"combine_script\":\"return state;\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}}]}}}";
            String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";

            ServiceLevelHelper serviceLevelHelper = new ServiceLevelHelper();

            serviceLevelId = serviceLevelHelper.getServiceLevelId(flowToTest,PCQ,DCQ,customAssert);

            if(serviceLevelId == -1){
                customAssert.assertTrue(false,"Service Level Id not created ");
            }else {

                Show show = new Show();
                show.hitShowVersion2(slEntityTypeId, serviceLevelId);
                String showResponse = show.getShowJsonStr();

                JSONObject showResponseJSon = new JSONObject(showResponse);

                String customCurrencyFieldValue  = showResponseJSon.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject(dynamicFieldSl).get("values").toString();

                if(!customCurrencyFieldValue.equalsIgnoreCase(customFieldValueSl)){
                    customAssert.assertTrue(false,"Expected and Actual Value Of custom field didn't match");

                }
            }
            slToDelete.add(serviceLevelId);

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception occurred while  validating the scenario" + e.getStackTrace());
        }finally {
//            EntityOperationsHelper.deleteEntityRecord(slEntity,serviceLevelId);
        }

        customAssert.assertAll();
    }

    @Test(enabled = false) //done
    public void TestListingForNegativesValues(){

        CustomAssert customAssert = new CustomAssert();

        try{

            int listId = 6;
            int entityTypeId = slEntityTypeId;
            String dynamicFieldId = "dyn" + dynamicFieldSlId;
            int filterName = dynamicFieldSlId;
            int columnId = 113764;
            int filterId = 1000681;
            int minValue = -200;
            int maxValue = 200;

            // Auto office filter
            String filter = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"" +
                    "size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                    "\"filterJson\":{\"" +
                    "" + filterId + "\":{\"filterId\":\"" + filterId + "\"," +
                    "\"filterName\":\"" + filterName + "\"," +
                    "\"entityFieldId\":" + filterName + ",\"entityFieldHtmlType\":19," +
                    "\"min\":\"" + minValue + "\",\"max\":\"" + maxValue + "\"," +
                    "\"suffix\":null}}},\"selectedColumns\":" +
                    "[{\"columnId\":277,\"columnQueryName\":\"bulkcheckbox\"}," +
                    "{\"columnId\":110,\"columnQueryName\":\"id\"},{\"columnId\":" + columnId + ",\"" +
                    "columnQueryName\":\"dyn" + filterName + "\"}]}";

            // BA1 env filter
            String filterPayload = "{\"filterMap\":{\"entityTypeId\":"+entityTypeId+",\"offset\":0,\"size\":20," +
                    "\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":" +
                    "{\"242\":{\"filterId\":242,\"filterName\":\"calendarViewType\",\"multiselectValues\":" +
                    "{\"SELECTEDDATA\":[{\"id\":\"1001\",\"name\":\"Gregorian\",\"$$hashKey\":\"object:708\"}]}}," +
                    "\""+filterId+"\":{\"filterId\":\""+filterId+"\",\"filterName\":\""+filterName+"\",\"entityFieldId\":" +
                    ""+filterName+",\"entityFieldHtmlType\":19,\"min\":\""+minValue+"\",\"max\":\""+maxValue+"\"," +
                    "\"suffix\":null}}},\"selectedColumns\":[{\"columnId\":277,\"columnQueryName\":\"bulkcheckbox\"}," +
                    "{\"columnId\":110,\"columnQueryName\":\"id\"},{\"columnId\":"+columnId+"," +
                    "\"columnQueryName\":\"dyn"+filterName+"\"}]}";

            ListRendererListData listRendererListData = new ListRendererListData();

            listRendererListData.hitListRendererListDataV2(listId,filterPayload);

            String listingResponse =  listRendererListData.getListDataJsonStr();

            JSONObject listingResponseJson = new JSONObject(listingResponse);

            JSONArray dataArray = listingResponseJson.getJSONArray("data");

            String columnName;
            String currencyFieldValue = "-1";
            int showPageId = -1;

            outerLoop:
            for(int i =0;i<dataArray.length();i++){

                JSONArray indDataArray = JSONUtility.convertJsonOnjectToJsonArray(dataArray.getJSONObject(i));

                innerLoop:
                for(int j =0;j<indDataArray.length();j++){

                    columnName = indDataArray.getJSONObject(j).get("columnName").toString();

                    if(columnName.equalsIgnoreCase("id")){
                        showPageId = Integer.parseInt(indDataArray.getJSONObject(j).get("value").toString().split(":;")[1]);
                    }

                    if(columnName.equalsIgnoreCase("dyn" + filterName)){
                        currencyFieldValue = indDataArray.getJSONObject(j).get("value").toString();
                    }
                }

            }

            Float currencyFieldValueInt = Float.parseFloat(currencyFieldValue);

            if( !((minValue <=currencyFieldValueInt) && (currencyFieldValueInt<=maxValue))){
                customAssert.assertTrue(false,"Custom Currency field doesn't lie in the expected range");
            }

            if(showPageId !=- 1 && !(currencyFieldValue.equalsIgnoreCase("-1"))){

                validateFieldsOnShowPage(entityTypeId,showPageId,dynamicFieldId,currencyFieldValue,customAssert);

            }else {
                customAssert.assertTrue(false,"Show Page Id not found from listing page");
            }


        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating the scenario");
        }

        customAssert.assertAll();

    }

    @Test(dependsOnMethods = "TestCreatePageAndShowPageSL", enabled = false, priority = 3) //done
    public void TestEditPageSL(){

        CustomAssert customAssert = new CustomAssert();

        try{
            customFieldValueSl = "-30";
            JSONObject editPayload = getEditPayload(slEntity,serviceLevelId);
            JSONObject dynamicFieldJson = editPayload.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject(dynamicFieldSl);

            dynamicFieldJson.put("values",Integer.valueOf(customFieldValueSl));

            editPayload.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject(dynamicFieldSl).put("values",Integer.valueOf(customFieldValueSl));

            editPayload.getJSONObject("body").getJSONObject("data").put(dynamicFieldSl,dynamicFieldJson);

            Edit edit = new Edit();
            String editResponse = edit.hitEdit(slEntity,editPayload.toString());

            if(!editResponse.contains("success")){
                customAssert.assertTrue(false,"Edit Done unsuccessfully");
                customAssert.assertAll();
            }else {

                Show show = new Show();
                show.hitShowVersion2(slEntityTypeId, serviceLevelId);
                String showResponse = show.getShowJsonStr();

                JSONObject showResponseJSon = new JSONObject(showResponse);

                String customCurrencyFieldValue  = showResponseJSon.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject(dynamicFieldSl).get("values").toString();

                if(!customCurrencyFieldValue.equalsIgnoreCase(customFieldValueSl)){
                    customAssert.assertTrue(false,"Expected and Actual Value Of custom field didn't match");

                }
            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while performing Edit");

        }
        customAssert.assertAll();

    }

    @Test(dependsOnMethods = "TestCreatePageAndShowPageSL", enabled = false, priority = 2)
    public void TestBulkEditSL() {

        CustomAssert customAssert = new CustomAssert();

        String flowToTest = "sl automation flow";

        try {
            customFieldValueSl = "-80";
            String PCQ = "{\"aggs\":{\"group_by_sl_met\":{\"scripted_metric\":{\"map_script\":\"if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['Total Business Elapsed Time (Days)'].value < 20){if(doc['exception'].value== false){state.map.met++}else{state.map.notMet++}}else{if(doc['exception'].value== false){state.map.notMet++}else{state.map.met++}}}} if(params['_source']['Problem Type'] == 'Reactive'){if(params['_source']['Priority'] == '3 - Moderate'||params['_source']['Priority'] == '4 - Low'||params['_source']['Priority'] == '5 - Minor'){if(doc['exception'].value== false){state.map.credit += doc['Applicable Credit'].value}}} \",\"init_script\":\"state['map'] =['met': 0.0, 'notMet': 0, 'credit':0]\",\"reduce_script\":\"params.return_map = ['Final_Numerator':0.0, 'Final_Denominator':0, 'Final_Performance':0 ,'Actual_Numerator':0.0, 'Actual_Denominator':0, 'Actual_Performance':0 ,'SL_Met':0 ,     'Calculated_Credit_Amount':0.0]; for (a in states){params.return_map.Final_Numerator += (float)(a.map.met); params.return_map.Final_Denominator += (float)(a.map.met + a.map.notMet) ; params.return_map.Calculated_Credit_Amount += (float)(a.map.credit);}  if(params.return_map.Final_Denominator > 0){params.return_map.Final_Performance = Math.round(((float)(params.return_map.Final_Numerator*100)/params.return_map.Final_Denominator)*100.0)/100.0}else{params.return_map.Final_Performance ='';params.return_map.SL_Met =5} params.return_map.Final_Numerator = Math.round(params.return_map.Final_Numerator *100.0)/100.0 ;params.return_map.Actual_Numerator = params.return_map.Final_Numerator; params.return_map.Actual_Denominator = params.return_map.Final_Denominator; params.return_map.Actual_Performance = params.return_map.Final_Performance; if(params.return_map.Final_Denominator > 0){if(params.return_map.Final_Performance < 90){params.return_map.Calculated_Credit_Amount = params.return_map.Calculated_Credit_Amount} else{params.return_map.Calculated_Credit_Amount = ''}} else{params.return_map.Calculated_Credit_Amount = ''}  return params.return_map\"}}},\"size\":0,\"query\":{\"bool\":{\"must\":[{\"match\":{\"childslaId\":\"childSLAId\"}},{\"match\":{\"useInComputation\":true}}]}}}";
            String DCQ = "{\"query\": {\"bool\": {\"must\": [{\"match\": {\"childslaId\": \"childSLAId\"}}]}}}";
            String entityIdToTest;

            entityIdToTest = String.valueOf(serviceLevelId);

            String bulkEditPayload = "{\"body\":{\"data\":{\"entityTypeId\":{\"name\":\"" +
                    "entityTypeId\",\"values\":" + slEntityTypeId + ",\"multiEntitySupport\":false},\"" +
                    "dyn" + dynamicFieldSlId + "\":{\"name\":\"dyn" + dynamicFieldSlId + "\",\"values\":" + customFieldValueSl + ",\"multiEntitySupport\":false,\"id\":" + dynamicFieldSlId + "}," +
                    "\"dynamicMetadata\":{\"dyn" + dynamicFieldSlId + "\":{\"name\":\"dyn" + dynamicFieldSlId + "\",\"id\":" + dynamicFieldSlId + "," +
                    "\"multiEntitySupport\":false,\"values\":" + customFieldValueSl + "}}}," +
                    "\"globalData\":{\"entityIds\":[" + entityIdToTest + "],\"fieldIds\":[" + dynamicFieldSlId + "],\"isGlobalBulk\":true}}}";

            BulkeditEdit bulkeditEdit = new BulkeditEdit();
            bulkeditEdit.hitBulkeditEdit(slEntityTypeId, bulkEditPayload);
            String bulkEditResponse = bulkeditEdit.getBulkeditEditJsonStr();

            if (!bulkEditResponse.contains("success")) {

                customAssert.assertTrue(false, "Bulk edit done unsuccessfully ");
            }else {

                Boolean bulkJobCompleted = checkBulkJobCompleted(customAssert);
                if(bulkJobCompleted) {
                    Boolean showPageValidation = validateFieldsOnShowPage(slEntityTypeId, serviceLevelId, dynamicFieldSl, customFieldValueSl, customAssert);

                    if (!showPageValidation) {
                        customAssert.assertTrue(false, "Show page validated unsuccessfully ");
                    }
                }
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating Bulk Edit Scenario");
        }

        customAssert.assertAll();
    }

    @Test(dependsOnMethods = "TestCreatePageAndShowPageSL", enabled = false, priority = 1)
    public void TestBulkUpdateSL(){

        CustomAssert customAssert = new CustomAssert();

        try{
            customFieldValueSl = "-60";
            Download download = new Download();
            String fileName = "BulkUpdateSL.xlsm";
            String outputFile = outputFilePath + "//" + fileName;
            int templateId = 1026;

            String entityIds = String.valueOf(serviceLevelId);

            download.hitDownload(outputFile,templateId,slEntityTypeId,entityIds);

            String statusCode = download.getApiStatusCode();

            String sheetName = "Sla";
            Boolean excelUpdateStatus = XLSUtils.updateColumnValue(outputFilePath,fileName,sheetName,6,25,Integer.parseInt(customFieldValueSl));

            if(!excelUpdateStatus){
                customAssert.assertTrue(false,"Excel updated unsuccessfully");
            }else {

                UploadBulkData uploadBulkData = new UploadBulkData();
                Map<String,String> payloadMap = new HashMap<>();

                payloadMap.put("entityTypeId",String.valueOf(slEntityTypeId));
                payloadMap.put("upload","Submit");
                payloadMap.put("_csrf_token","c32411cb-7120-4b80-b70b-c77d160de529");

                uploadBulkData.hitUploadBulkData(slEntityTypeId,templateId,outputFilePath,fileName,payloadMap);

                String bulkUpdateStatus = uploadBulkData.getUploadBulkDataJsonStr();

                if (!bulkUpdateStatus.contains("success")) {

                    customAssert.assertTrue(false, "Bulk update done unsuccessfully ");
                }else {

//                    Boolean bulkJobCompleted = checkBulkJobCompleted(customAssert);
                    Boolean bulkJobCompleted = true;
                    if(bulkJobCompleted) {
                        Boolean showPageValidation = validateFieldsOnShowPage(slEntityTypeId, serviceLevelId, dynamicFieldSl, customFieldValueSl, customAssert);

                        if (!showPageValidation) {
                            customAssert.assertTrue(false, "Show page validated unsuccessfully ");
                        }
                    }
                }

            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while Bulk update " + e.getStackTrace());
        }

        customAssert.assertAll();
    }

    @Test(enabled = false)
    public void TestEditPageCSL(){

        CustomAssert customAssert = new CustomAssert();

        try{
            serviceLevelId = 20795;

            ArrayList<String> childServiceLevelIds = checkIfCSLCreatedOnServiceLevel(serviceLevelId, customAssert);

            int childServiceLevelId = Integer.parseInt(childServiceLevelIds.get(3));

            customFieldValueCSl = "-40";
            JSONObject editPayload = getEditPayload(cslEntity,childServiceLevelId);
            JSONObject dynamicFieldJson = editPayload.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject(dynamicFieldCSl);

            dynamicFieldJson.put("values",Integer.valueOf(customFieldValueCSl));

            editPayload.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject(dynamicFieldCSl).put("values",Integer.valueOf(customFieldValueCSl));

            editPayload.getJSONObject("body").getJSONObject("data").put(dynamicFieldCSl,dynamicFieldJson);

            Edit edit = new Edit();
            String editResponse = edit.hitEdit(cslEntity,editPayload.toString());

            if(!editResponse.contains("success")){
                customAssert.assertTrue(false,"Edit Done unsuccessfully");
            }else {
                validateFieldsOnShowPage(cslEntityTypeId,childServiceLevelId,dynamicFieldCSl,customFieldValueCSl,customAssert);
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while performing Edit");

        }
        customAssert.assertAll();

    }

    @Test(enabled = false) //done
    public void TestBulkEditCSL(){

        CustomAssert customAssert = new CustomAssert();
        customFieldValueCSl = "-60";
        try {

            ArrayList<String> childServiceLevelId = checkIfCSLCreatedOnServiceLevel(serviceLevelId, customAssert);

            int entityIdToTest = Integer.parseInt(childServiceLevelId.get(4));

            String bulkEditPayload = "{\"body\":{\"data\":{\"entityTypeId\":{\"name\":\"" +
                    "entityTypeId\",\"values\":" + cslEntityTypeId + ",\"multiEntitySupport\":false},\"" +
                    "dyn" + dynamicFieldCSlId + "\":{\"name\":\"dyn" + dynamicFieldCSlId + "\",\"values\":" + customFieldValueCSl + ",\"multiEntitySupport\":false,\"id\":" + dynamicFieldCSlId + "}," +
                    "\"dynamicMetadata\":{\"dyn" + dynamicFieldCSlId + "\":{\"name\":\"dyn" + dynamicFieldCSlId + "\",\"id\":" + dynamicFieldCSlId + "," +
                    "\"multiEntitySupport\":false,\"values\":" + customFieldValueCSl + "}}}," +
                    "\"globalData\":{\"entityIds\":[" + entityIdToTest + "],\"fieldIds\":[" + dynamicFieldCSlId + "],\"isGlobalBulk\":true}}}";

            BulkeditEdit bulkeditEdit = new BulkeditEdit();
            bulkeditEdit.hitBulkeditEdit(cslEntityTypeId, bulkEditPayload);
            String bulkEditResponse = bulkeditEdit.getBulkeditEditJsonStr();

            if (!bulkEditResponse.contains("success")) {
                customAssert.assertTrue(false, "Bulk edit done unsuccessfully ");
            }else {

//                Boolean bulkJobCompleted = checkBulkJobCompleted(customAssert);
                Boolean bulkJobCompleted = true;
                if(bulkJobCompleted) {
                    Boolean showPageValidation = validateFieldsOnShowPage(cslEntityTypeId, entityIdToTest, dynamicFieldCSl, customFieldValueCSl, customAssert);

                    if (!showPageValidation) {
                        customAssert.assertTrue(false, "Show page validated unsuccessfully ");
                    }
                }
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while validating Bulk Edit Scenario");
        }

        customAssert.assertAll();

    }

    @Test(enabled = true)
    public void TestSearchNegativeCustomCurrencyFields(){

        CustomAssert customAssert = new CustomAssert();
        int slEntityTypeId = 14;

        try{

            Search search = new Search();

            int minValue = -200;
            int maxValue = 200;
            String dynamicFieldId = "dyn" + dynamicFieldSlId;
            int filterName = dynamicFieldSlId;

            String payload = "{\"body\":{\"data\":{\"entityTypeId\":{\"name\":\"entityTypeId\",\"values\":14,\"" +
                    "multiEntitySupport\":false},\"dynamicMetadata\":" +
                    "{\"dyn" + filterName + "From\":{\"values\":" + minValue + "},\"dyn" + filterName + "" +
                    "To\":{\"values\":" + maxValue + "}},\"" +
                    "searchParam\":{\"size\":{\"name\":\"size\",\"values\":20,\"multiEntitySupport\":false}," +
                    "\"offset\":{\"name\":\"offset\",\"values\":0,\"multiEntitySupport\":false}}}}}";

            String searchResponse = search.hitSearch(slEntityTypeId,payload);

            JSONObject searchResponseJson = new JSONObject(searchResponse);

            JSONArray dataArray = searchResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("searchResults").getJSONObject("values").getJSONArray("data");

            if(dataArray.length() ==0 ){
                customAssert.assertTrue(false,"No record found after search results");
                customAssert.assertAll();
            }

            JSONObject singleData = dataArray.getJSONObject(0);
            JSONArray singleDataJsonArray = JSONUtility.convertJsonOnjectToJsonArray(singleData);

            String columnName;
            String slId = "";
            for(int i =0;i<singleDataJsonArray.length();i++){

                columnName = singleDataJsonArray.getJSONObject(i).get("columnName").toString();
                if(columnName.equalsIgnoreCase("id")) {
                    slId = singleDataJsonArray.getJSONObject(i).get("value").toString().split(":;")[1];
                    break;
                }
            }

            Show show = new Show();
            show.hitShowVersion2(slEntityTypeId,Integer.parseInt(slId));
            String showResponse = show.getShowJsonStr();

            JSONObject showResponseJSon = new JSONObject(showResponse);

            String currencyFieldValue = showResponseJSon.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject(dynamicFieldId).get("values").toString();

            int currencyFieldValueInt = Integer.parseInt(currencyFieldValue);

            if( !((minValue <=currencyFieldValueInt) && (currencyFieldValueInt<=maxValue))){
                customAssert.assertTrue(false,"Custom Currency field doesn't lie in the expected range");
            }


        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating the scenario " + e.getMessage());
        }


        customAssert.assertAll();
    }

    private ArrayList<String> checkIfCSLCreatedOnServiceLevel(int serviceLevelId, CustomAssert customAssert) {

        logger.info("Checking if CSL created on service level");

        long timeSpent = 0;
        long cSLCreationTimeOut = 5000000L;
        long pollingTime = 5000L;
        ArrayList<String> childServiceLevelIds = new ArrayList<>();
        try {
            JSONObject tabListResponseJson;
            JSONArray dataArray = new JSONArray();

            TabListData tabListData = new TabListData();
            tabListData.hitTabListData(childServiceLevelTabId, slEntityTypeId, serviceLevelId);
            String tabListResponse = tabListData.getTabListDataResponseStr();

            if (JSONUtility.validjson(tabListResponse)) {

                while (timeSpent < cSLCreationTimeOut) {
                    logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                    Thread.sleep(pollingTime);

                    tabListData.hitTabListData(childServiceLevelTabId, slEntityTypeId, serviceLevelId);
                    tabListResponse = tabListData.getTabListDataResponseStr();

                    if (!JSONUtility.validjson(tabListResponse)) {

                        customAssert.assertTrue(false, "Service level tab Child Service Level has invalid Json Response for service level id " + serviceLevelId);
                        break;
                    }

                    tabListResponseJson = new JSONObject(tabListResponse);
                    dataArray = tabListResponseJson.getJSONArray("data");

                    if (dataArray.length() > 0) {

                        customAssert.assertTrue(true, "Child Service Level created successfully ");

                        childServiceLevelIds = (ArrayList) ListDataHelper.getColumnIds(tabListResponse);
                        break;
                    } else {
                        timeSpent += pollingTime;
                        logger.info("Child Service Level not created yet ");
                    }
                }
                if (childServiceLevelIds.size() == 0) {
//					customAssert.assertTrue(false, "Child Service level not created in " + cSLCreationTimeOut + " milli seconds for service level id " + serviceLevelId);
                }

            } else {
                customAssert.assertTrue(false, "Service level tab Child Service Level has invalid Json Response for service level id " + serviceLevelId);
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while checking child service level tab on ServiceLevel " + serviceLevelId + " " + e.getMessage());
        }

        return childServiceLevelIds;
    }

    private JSONObject getEditPayload(String entityName,int entityId){

        Edit edit = new Edit();

        String editPayload = null;
        JSONObject editResponseJson;

        try {

            editPayload = edit.hitEdit(entityName, entityId);

            editResponseJson = new JSONObject(editPayload);

            editResponseJson.remove("header");
            editResponseJson.remove("session");
            editResponseJson.remove("actions");
            editResponseJson.remove("createLinks");

            editResponseJson.getJSONObject("body").remove("layoutInfo");
            editResponseJson.getJSONObject("body").remove("globalData");
            editResponseJson.getJSONObject("body").remove("errors");

        }catch (Exception e){
            editResponseJson = null;
        }

        return editResponseJson;
    }

    private synchronized Boolean validateFieldsOnShowPage(int entityTypeId,int entityId,String dynamicField,String customFieldValue,CustomAssert customAssert){

        Boolean validationStatus = true;
        try {
            Show show = new Show();
            show.hitShowVersion2(entityTypeId, entityId);
            String showResponse = show.getShowJsonStr();

            JSONObject showResponseJSon = new JSONObject(showResponse);

            String customCurrencyFieldValue = showResponseJSon.getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject(dynamicField).get("values").toString();

            if (!customCurrencyFieldValue.equalsIgnoreCase(customFieldValue)) {
                customAssert.assertTrue(false, "Expected and Actual Value Of custom field didn't match");
                validationStatus = false;

            }
        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating values on show page " + e.getStackTrace());
            validationStatus = false;
        }
        return validationStatus;
    }

    private void waitForScheduler(String flowToTest, int newTaskId, CustomAssert csAssert) {
        logger.info("Waiting for Scheduler to Complete for Flow [{}].", flowToTest);
        try {
            long timeOut = 1200000;

            logger.info("Time Out for Scheduler is {} milliseconds", timeOut);
            long timeSpent = 0;

            if (newTaskId != -1) {
                logger.info("Checking if Bulk Edit Task has completed or not for Flow [{}]", flowToTest);
                long pollingTime = 5000;


                while (timeSpent < timeOut) {
                    logger.info("Putting Thread on Sleep for {} milliseconds.", pollingTime);
                    Thread.sleep(pollingTime);

                    logger.info("Hitting Fetch API.");
                    Fetch fetchObj = new Fetch();
                    fetchObj.hitFetch();

                    logger.info("Getting Status of Bulk Edit Task for Flow [{}]", flowToTest);
                    String newTaskStatus = UserTasksHelper.getStatusFromTaskJobId(fetchObj.getFetchJsonStr(), newTaskId);
                    if (newTaskStatus != null && newTaskStatus.trim().equalsIgnoreCase("Completed")) {
                        logger.info("Bulk Edit Task Completed for Flow [{}]", flowToTest);
                        break;
                    } else {
                        timeSpent += pollingTime;
                        logger.info("Bulk Edit Task is not finished yet for Flow [{}]", flowToTest);
                    }

                    if (newTaskStatus != null && newTaskStatus.trim().equalsIgnoreCase("In Progress")) {
                        if (!UserTasksHelper.anyRecordFailedInTask(fetchObj.getFetchJsonStr(), newTaskId) &&
                                !UserTasksHelper.anyRecordProcessedInTask(fetchObj.getFetchJsonStr(), newTaskId)) {

                        } else {
                            logger.info("Bulk Edit Task for Flow [{}] is In Progress but At-least One record has been processed or failed. " +
                                    "Hence Not Checking if Show Page is Blocked or not.", flowToTest);
                        }
                    } else {
                        logger.info("Bulk Edit Task for Flow [{}] has not been picked by Scheduler yet.", flowToTest);
                    }
                }
            } else {
                logger.info("Couldn't get Bulk Edit Task Job Id for Flow [{}]. Hence waiting for Task Time Out i.e. {}", flowToTest, timeOut);
                Thread.sleep(timeOut);
            }
        } catch (Exception e) {
            logger.error("Exception while Waiting for Scheduler to Finish for Flow [{}]. {}", flowToTest, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Waiting for Scheduler to Finish for Flow [" + flowToTest + "]. " + e.getMessage());
        }
    }

    //    @Test(enabled = false)
//    public void TestBulkUpdateCSL(){
//        CustomAssert customAssert = new CustomAssert();
//
//        try{
//            customFieldValueCSl = "-50";
//            Download download = new Download();
//            String fileName = "BulkUpdateCSL.xlsm";
//            String outputFile = outputFilePath + "\\" + fileName;
//            int templateId = 1021;
//
//            List<String> childServiceLevelIds = checkIfCSLCreatedOnServiceLevel(serviceLevelId,customAssert);
//
//            String entityIds = childServiceLevelIds.get(0);
//
//            int cslId = Integer.parseInt(childServiceLevelIds.get(0));
//
//            download.hitDownload(outputFile,templateId,cslEntityTypeId,entityIds);
//
//            String statusCode = download.getApiStatusCode();
//
//            if(!statusCode.contains("200")){
//                customAssert.assertTrue(false,"Template File Downloaded unsuccessfully");
//            }
//
//            String sheetName = "Child Sla";
//            Boolean excelUpdateStatus = XLSUtils.updateColumnValue(outputFilePath,fileName,sheetName,6,6,Integer.parseInt(customFieldValueSl));
//
//            if(!excelUpdateStatus){
//                customAssert.assertTrue(false,"Excel updated unsuccessfully");
//            }else {
//
//                UploadBulkData uploadBulkData = new UploadBulkData();
//                Map<String,String> payloadMap = new HashMap<>();
//
//                payloadMap.put("entityTypeId",String.valueOf(cslEntityTypeId));
//                payloadMap.put("upload","Submit");
//                payloadMap.put("_csrf_token","c32411cb-7120-4b80-b70b-c77d160de529");
//
//                uploadBulkData.hitUploadBulkData(cslEntityTypeId,templateId,outputFilePath,fileName,payloadMap);
//
//                String bulkUpdateStatus = uploadBulkData.getUploadBulkDataJsonStr();
//
//                if(!bulkUpdateStatus.equalsIgnoreCase("200")){
//                    customAssert.assertTrue(false,"Error while bulk update");
//                }else {
//                    Thread.sleep(5000);
//
//                    Boolean showPageValidation = validateFieldsOnShowPage(cslEntityTypeId,cslId,dynamicFieldSl,customFieldValueSl,customAssert);
//
//                    if(!showPageValidation){
//                        customAssert.assertTrue(false,"Show page validated unsuccessfully ");
//                    }
//                }
//
//            }
//
//        }catch (Exception e){
//            customAssert.assertTrue(false,"Exception while Bulk update " + e.getStackTrace());
//        }
//
//        customAssert.assertAll();
//
//    }

    private Boolean checkBulkJobCompleted(CustomAssert customAssert){

        Boolean jobStatus = false;

        logger.info("Hitting Fetch API to Get Bulk Edit Job Task Id");
        Fetch fetchObj = new Fetch();
        fetchObj.hitFetch();
        List<Integer> allTaskIds = UserTasksHelper.getAllTaskIds(fetchObj.getFetchJsonStr());

        int newTaskId = UserTasksHelper.getNewTaskId(fetchObj.getFetchJsonStr(), allTaskIds);

        waitForScheduler("Bulk Flow", newTaskId, customAssert);

        logger.info("Hitting Fetch API to get Status of Bulk Job");
        fetchObj.hitFetch();
        String bulkEditJobStatus = UserTasksHelper.getStatusFromTaskJobId(fetchObj.getFetchJsonStr(), newTaskId);

        if (bulkEditJobStatus != null && bulkEditJobStatus.trim().equalsIgnoreCase("Completed")) {
            //Check Status on Show Page
            jobStatus = true;

        } else {

            logger.error("Failing Test as Bulk Job for Flow [{}] is not completed yet");
            customAssert.assertTrue(false, "Failing Test as Bulk Job for Flow [{}] is not completed ");

        }

        return jobStatus;
    }

//    @AfterClass(groups = {"sanity","sprint"})
//    public void afterClass() {
//
//        logger.debug("Number CSL To Delete " + cslToDelete.size());
//        EntityOperationsHelper.deleteMultipleRecords("child service levels", cslToDelete);
//
//        logger.debug("Number SL To Delete " + slToDelete.size());
//        EntityOperationsHelper.deleteMultipleRecords("service levels", slToDelete);
//
//    }

}
