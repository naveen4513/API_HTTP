package com.sirionlabs.test.filters;

import com.google.inject.internal.cglib.core.$Customizer;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererFilterData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;

public class TestFiltersOptions {

    private final static Logger logger = LoggerFactory.getLogger(TestFiltersOptions.class);

    String listIdFilterIdMappingFilePath;
    String configFilePath;
    String configFileName;

    @BeforeClass
    public void BeforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("FilterOptionFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("FilterOptionFileName");

        listIdFilterIdMappingFilePath = ConfigureConstantFields.getConstantFieldsProperty("ListIdFilterIdMappingFilePath");

    }

    @DataProvider(parallel = false)
    public Object[][] dataProviderForFilterOptions() {

        List<Object[]> allTestData = new ArrayList<>();
        String[] entityToTest = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"entity to test").split(",");

        for (String entity : entityToTest) {

            allTestData.add(new Object[]{entity});
        }


        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForFilterOptions")
    public void Test_FilterOptions(String entityName){

        CustomAssert customAssert = new CustomAssert();

        try {

            int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
            int listId = ConfigureConstantFields.getListIdForEntity(entityName);

            String filtIdListIdMappingFileName = entityName + ".cfg";

            Map<String,String> listIdFilterValues = ParseConfigFile.getAllProperties(listIdFilterIdMappingFilePath,filtIdListIdMappingFileName);

            Map<String,Boolean> filterIdEligibilityMap = createMapFilterEligiForOptions(entityName,listId,customAssert);

            ListRendererFilterData listRendererFilterData = new ListRendererFilterData();
            listRendererFilterData.hitListRendererFilterData(listId);
            String filterDataResponse =  listRendererFilterData.getListRendererFilterDataJsonStr();

            if(!JSONUtility.validjson(filterDataResponse)){
                customAssert.assertTrue(false,"Filter data response is an invalid json for list id " + listId);
            }else {

                JSONObject filterDataResponseJson = new JSONObject(filterDataResponse);

                String filterName = "";
                String filterType;
                int htmlType = -1;
                int entityFieldId = -1;

                String autoComplete;
                JSONArray optionsArray;
                ArrayList<String> optionListId;
                ArrayList<String> optionListName;
                String optionId;
                String optionName;
                try{
                    Iterator<String> filterIds = filterDataResponseJson.keys();

                    filterLoop:
                    while (filterIds.hasNext()) {
                        try {
                            String filterId = filterIds.next();
                            filterName = filterDataResponseJson.getJSONObject(filterId).get("filterName").toString();

                            filterType = filterDataResponseJson.getJSONObject(filterId).get("uitype").toString();
                            try {
                                htmlType = Integer.parseInt(filterDataResponseJson.getJSONObject(filterId).get("entityFieldHtmlType").toString());
                            } catch (Exception e) {
                                htmlType = -1;
                            }
                            try {
                                entityFieldId = Integer.parseInt(filterDataResponseJson.getJSONObject(filterId).get("entityFieldId").toString());
                            } catch (Exception e) {
                                entityFieldId = -1;
                            }


                            if (filterType.equals("MULTISELECT")) {

                                autoComplete = filterDataResponseJson.getJSONObject(filterId).getJSONObject("multiselectValues").getJSONObject("OPTIONS").get("autoComplete").toString();

                                if (autoComplete.equals("false")) {
                                    try {
                                        optionsArray = filterDataResponseJson.getJSONObject(filterId).getJSONObject("multiselectValues").getJSONObject("OPTIONS").getJSONArray("DATA");
                                    } catch (JSONException jse) {
                                        optionsArray = filterDataResponseJson.getJSONObject(filterId).getJSONObject("multiselectValues").getJSONObject("OPTIONS").getJSONArray("data");
                                    }

                                    optionListId = new ArrayList();
                                    optionListName = new ArrayList();
                                    for (int j = 0; j < optionsArray.length(); j++) {
                                        optionId = optionsArray.getJSONObject(j).get("id").toString();
                                        optionName = optionsArray.getJSONObject(j).get("name").toString();
                                        optionListId.add(optionId);
                                        optionListName.add(optionName);
                                    }

                                    if(optionListId.size() == 0) {
                                        logger.debug("Option List Id for Filter " + filterName + " is zero thus skipping the flow");
                                        continue filterLoop;
                                    }else {

                                        Boolean eligibleFilter = filterIdEligibilityMap.get(filterId);

                                        if(eligibleFilter) {

                                            validateBlankFilter(listId, entityTypeId, entityName, filterId, filterName, htmlType, entityFieldId, listIdFilterValues, customAssert);
//
                                            validateIncludeFilter(listId, entityTypeId, entityName, filterId, filterName, htmlType, entityFieldId, optionListId, optionListName, listIdFilterValues, customAssert);
//
                                            validateExcludeFilter(listId, entityTypeId, entityName, filterId, filterName, htmlType, entityFieldId, optionListId, optionListName, listIdFilterValues, customAssert);

                                        }else {
                                            logger.debug("Filter " + filterName + " Not Eligible for Include Exclude and Not");
                                        }
                                    }
                                }
                            }
                        }catch (Exception e){
                            logger.error("Exception while validating filter id " + filterName);
                        }
                    }

                }catch (Exception e){
                    logger.error("Exception while validating filter id " + filterName);
                }

            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception in main test method " + e.getStackTrace());
        }

        customAssert.assertAll();
    }

    private Boolean validateBlankFilter(int listId,int entityTypeId,String entityName,String filterId,String filterName,int htmlType,int entityFieldId,Map<String,String> listIdFilterValues,CustomAssert customAssert){

        Boolean validationStatus = true;
        int size = 200;
        try{
            logger.info("Validating Blank Filter for entity type id " + entityTypeId + " Filter Name " + filterName);

            String payload = "";

            if(entityFieldId == -1 && htmlType == -1) {
                payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":" + size + ",\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                        "\"filterJson\":{\"" + filterId + "\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":0,\"name\":\"blank\"}]}," +
                        "\"filterId\":" + filterId + ",\"filterName\":\"" + filterName + "\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}}," +
                        "\"selectedColumns\":[]}";
            }else if(entityFieldId == -1 && htmlType != -1) {

                payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":" + size + ",\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                        "\"filterJson\":{\"" + filterId + "\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":0,\"name\":\"blank\"}]}," +
                        "\"filterId\":" + filterId + ",\"filterName\":\"" + filterName + "\",\"entityFieldHtmlType\":" + htmlType + ",\"entityFieldId\":null}}}," +
                        "\"selectedColumns\":[]}";
            }else if(entityFieldId != -1 && htmlType == -1) {

                payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":" + size + ",\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                        "\"filterJson\":{\"" + filterId + "\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":0,\"name\":\"blank\"}]}," +
                        "\"filterId\":" + filterId + ",\"filterName\":\"" + filterName + "\",\"entityFieldHtmlType\":null,\"entityFieldId\":" + entityFieldId + "}}}," +
                        "\"selectedColumns\":[]}";
            }else if(entityFieldId != -1 && htmlType != -1) {

                payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":" + size + ",\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                        "\"filterJson\":{\"" + filterId + "\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":0,\"name\":\"blank\"}]}," +
                        "\"filterId\":" + filterId + ",\"filterName\":\"" + filterName + "\",\"entityFieldHtmlType\":" + htmlType + ",\"entityFieldId\":" + entityFieldId + "}}}," +
                        "\"selectedColumns\":[]}";
            }
            ListRendererListData listRendererListData = new ListRendererListData();
            listRendererListData.hitListRendererListDataV2(listId,payload);
            String listResponse = listRendererListData.getListDataJsonStr();

            if(!JSONUtility.validjson(listResponse)){
                customAssert.assertTrue(false,"List Response is an invalid json for filter " + filterName  + " entity name " + entityName);
                return false;
            }

            Map<Integer,Map<String, String>> listIdValueMap = listRendererListData.getListColumnIdValueMap(listResponse);
            Map<String, String> listIdValueMapInvRow;

            if(listIdValueMap.size() == 0){
                logger.debug("Listing Response contains zero records for Filter Name " + filterName + " and Entity " + entityName);
                return true;
            }
            String listColId = listIdFilterValues.get(filterId);

            if(listColId == null){
                logger.info("Skipping the Blank Filter Test For Filter " + filterName + " and entity name " + entityName);
                return true;
            }else if(listColId.equals("cannot test")){
                logger.info("Skipping the Blank Filter Test For Filter " + filterName + " and entity name " + entityName);
                return true;
            }
            String listValue;
            for (Map.Entry<Integer,Map<String, String>> entry: listIdValueMap.entrySet()){

                listIdValueMapInvRow = entry.getValue();

                listValue =  listIdValueMapInvRow.get(listColId);

                if(listValue == null){
                    //Do Nothing
                }else if(listValue.equals("null") || listValue.equals("") || listValue.equals("-")){
                    //Do Nothing
                }else {
                    logger.error("While checking Blank Filters List Value is not null for filter " + filterName + " and entity Name " + entityName);
                    customAssert.assertTrue(false,"While checking Blank Filters List Value is not null for filter " + filterName + " and entity Name " + entityName);
                    break;
                }
            }

        }catch (Exception e){
            logger.error("Exception while validating blank filter");
            customAssert.assertTrue(false,"Exception while validating blank filter " + e.getStackTrace());
            validationStatus = false;
        }
        return validationStatus;
    }

    private Boolean validateIncludeFilter(int listId,int entityTypeId,String entityName,String filterId,String filterName,int htmlType,int entityFieldId,ArrayList<String> optionListId,ArrayList<String> optionListName,Map<String,String> listIdFilterValues,CustomAssert customAssert){

        Boolean validationStatus = true;
        int size = 200;
        try{

            logger.info("Validating Include Filter for entity " + entityName + " Filter Name " + filterName);
            int numOfOptionsToCheck =2;
            String selectDataPayload = getSelectDataPayload(optionListId,optionListName,numOfOptionsToCheck);
            String expectedOption = "";
            ArrayList<String> expectedOptionList = new ArrayList<>();
            for(int i =0;i<optionListName.size();i++){

                if(i == numOfOptionsToCheck){
                    break;
                }
                expectedOption += optionListName.get(i) + ",";
                expectedOptionList.add(optionListName.get(i));
            }
            String payload = "";

            if(entityFieldId == -1 && htmlType == -1) {
                payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":" + size + "," +
                        "\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                        "\"filterJson\":{\"" + filterId + "\":{\"multiselectValues\":{\"SELECTEDDATA\":[" + selectDataPayload + "]}," +
                        "\"filterId\":" + filterId + ",\"filterName\":\"" + filterName + "\",\"entityFieldHtmlType\":null," +
                        "\"entityFieldId\":null,\"operator\":\"and\"}}},\"selectedColumns\":[]}";
            }else if(entityFieldId == -1 && htmlType != -1) {

                payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":" + size + "," +
                        "\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                        "\"filterJson\":{\"" + filterId + "\":{\"multiselectValues\":{\"SELECTEDDATA\":[" + selectDataPayload + "]}," +
                        "\"filterId\":" + filterId + ",\"filterName\":\"" + filterName + "\",\"entityFieldHtmlType\":" + htmlType + "," +
                        "\"entityFieldId\":null,\"operator\":\"and\"}}},\"selectedColumns\":[]}";

            }else if(entityFieldId != -1 && htmlType == -1) {

                payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":" + size + "," +
                        "\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                        "\"filterJson\":{\"" + filterId + "\":{\"multiselectValues\":{\"SELECTEDDATA\":[" + selectDataPayload + "]}," +
                        "\"filterId\":" + filterId + ",\"filterName\":\"" + filterName + "\",\"entityFieldHtmlType\":null," +
                        "\"entityFieldId\":" + entityFieldId + ",\"operator\":\"and\"}}},\"selectedColumns\":[]}";

            }else if(entityFieldId != -1 && htmlType != -1) {

                payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":" + size + "," +
                        "\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                        "\"filterJson\":{\"" + filterId + "\":{\"multiselectValues\":{\"SELECTEDDATA\":[" + selectDataPayload + "]}," +
                        "\"filterId\":" + filterId + ",\"filterName\":\"" + filterName + "\",\"entityFieldHtmlType\":" + htmlType + "," +
                        "\"entityFieldId\":" + entityFieldId + ",\"operator\":\"and\"}}},\"selectedColumns\":[]}";
            }

            ListRendererListData listRendererListData = new ListRendererListData();
            listRendererListData.hitListRendererListDataV2(listId,payload);
            String listResponse = listRendererListData.getListDataJsonStr();

            if(!JSONUtility.validjson(listResponse)){
                customAssert.assertTrue(false,"List Response is an invalid json for filter " + filterName + " Option " + optionListName + " entity name " + entityName);
                return false;
            }

            Map<Integer,Map<String, String>> listIdValueMap = listRendererListData.getListColumnIdValueMap(listResponse);
            Map<String, String> listIdValueMapInvRow;

            if(listIdValueMap.size() == 0){
                logger.debug("Listing Response contains zero records for Filter Name " + filterName + " and Entity " + entityName + " and option List " + expectedOption);
                return true;
            }
            String listColId = listIdFilterValues.get(filterId);

            if(listColId == null){
                logger.info("Skipping the Include Filter Test For Filter " + filterName + " and entity name " + entityName);
                return true;
            }else if(listColId.equals("cannot test")){
                logger.info("Skipping the Include Filter Test For Filter " + filterName + " and entity name " + entityName);
                return true;
            }else if(listColId.equals("")){
                logger.info("Skipping the Include Filter Test For Filter " + filterName + " and entity name " + entityName);
                return true;
            }

            String listValue;
            outerLoop:
            for (Map.Entry<Integer,Map<String, String>> entry: listIdValueMap.entrySet()){

                listIdValueMapInvRow = entry.getValue();

                listValue =  listIdValueMapInvRow.get(listColId);

                if(!expectedOption.contains(listValue)){
                    for(int i =0;i<expectedOptionList.size();i++){
                        if(!listValue.contains(expectedOptionList.get(i))){
                            logger.error("While validating Include Filters Option " + listValue + " present in the List Output It should not be present ");
                            customAssert.assertTrue(false, "While validating Include Filters Option " + listValue + " present in the List Output It should not be present ");
                            break outerLoop;
                        }
                    }

                }else {
                    logger.debug("While validating Include Filters For Filter Name " + filterName + " and Option " + expectedOption + " present in the List Output Working as Expected");
                }

            }


        }catch (Exception e){
            logger.error("Exception while validating Include filter");
            customAssert.assertTrue(false,"Exception while validating Include filter " + e.getStackTrace());
            validationStatus = false;
        }
        return validationStatus;
    }

    private Boolean validateExcludeFilter(int listId,int entityTypeId,String entityName,String filterId,String filterName,int htmlType,int entityFieldId,ArrayList<String> optionListId,ArrayList<String> optionListName,Map<String,String> listIdFilterValues,CustomAssert customAssert){

        Boolean validationStatus = true;
        int size = 200;
        int listDataSizeWithThatOption;
        int listDataSizeWithoutThatOption;
        int numOfOptionsToCheck =1;

        try{

            ListRendererListData listRendererListData = new ListRendererListData();

            logger.info("Validating Exclude for entity " + entityName + " Filter Name " + filterName);
//                      Payload when no filter is selected
            String payloadWithoutAnyFilSelected = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{}},\"selectedColumns\":[]}";

            listRendererListData.hitListRendererListDataV2(listId,payloadWithoutAnyFilSelected);
            String listResponse = listRendererListData.getListDataJsonStr();

            String listColId = listIdFilterValues.get(filterId);
            String expectedOption = "";

            if(listColId == null){
                logger.info("Skipping the Include Filter Test For Filter " + filterName + " and entity name " + entityName);
                return true;
            }else if(listColId.equals("cannot test")){
                logger.info("Skipping the Include Filter Test For Filter " + filterName + " and entity name " + entityName);
                return true;
            }

            ArrayList<String> expectedOptionList = new ArrayList<>();
            for(int i =0;i<optionListName.size();i++){

                if(i == numOfOptionsToCheck){
                    break;
                }
                expectedOption += optionListName.get(i) + ",";
                expectedOptionList.add(optionListName.get(i));
            }

            Map<Integer,Map<String, String>> listIdValueMapWithoutAnyFilSelected = listRendererListData.getListColumnIdValueMap(listResponse);
            Map<String, String> listIdValueMapInvRowWithoutAnyFilSelected;
            String listValue;
            Boolean defaultlistingPageHasValuesOtherThanExpOptions = false;
            for (Map.Entry<Integer,Map<String, String>> entry: listIdValueMapWithoutAnyFilSelected.entrySet()) {

                listIdValueMapInvRowWithoutAnyFilSelected = entry.getValue();

                listValue = listIdValueMapInvRowWithoutAnyFilSelected.get(listColId);

                if (listValue == null) {
                    continue;
                }

                if (listValue != null) {
                    if (expectedOption.contains(listValue)) {
                        for (int i = 0; i < expectedOptionList.size(); i++) {
                            if (!listValue.contains(expectedOptionList.get(i))) {
                                defaultlistingPageHasValuesOtherThanExpOptions = true;
                            }
                        }
                    }else {
                        defaultlistingPageHasValuesOtherThanExpOptions = true;
                    }
                } else {
                    logger.debug("While validating Exclude Filters For Filter Name " + filterName + " and Option " + expectedOption + " present in the List Output Working as Expected");
                }

            }

            int defaultListDataSizeWithoutAnyFilter = listIdValueMapWithoutAnyFilSelected.size();

            String selectDataPayload = getSelectDataPayload(optionListId,optionListName,numOfOptionsToCheck);

            String payloadInclude = "";
            if(entityFieldId == -1 && htmlType == -1) {
                payloadInclude = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":" + size + "," +
                        "\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                        "\"filterJson\":{\"" + filterId + "\":{\"multiselectValues\":{\"SELECTEDDATA\":[" + selectDataPayload + "]}," +
                        "\"filterId\":" + filterId + ",\"filterName\":\"" + filterName + "\",\"entityFieldHtmlType\":null," +
                        "\"entityFieldId\":null,\"operator\":\"and\"}}},\"selectedColumns\":[]}";
            }else if(entityFieldId == -1 && htmlType != -1) {

                payloadInclude = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":" + size + "," +
                        "\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                        "\"filterJson\":{\"" + filterId + "\":{\"multiselectValues\":{\"SELECTEDDATA\":[" + selectDataPayload + "]}," +
                        "\"filterId\":" + filterId + ",\"filterName\":\"" + filterName + "\",\"entityFieldHtmlType\":" + htmlType + "," +
                        "\"entityFieldId\":null,\"operator\":\"and\"}}},\"selectedColumns\":[]}";

            }else if(entityFieldId != -1 && htmlType == -1) {

                payloadInclude = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":" + size + "," +
                        "\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                        "\"filterJson\":{\"" + filterId + "\":{\"multiselectValues\":{\"SELECTEDDATA\":[" + selectDataPayload + "]}," +
                        "\"filterId\":" + filterId + ",\"filterName\":\"" + filterName + "\",\"entityFieldHtmlType\":null," +
                        "\"entityFieldId\":" + entityFieldId + ",\"operator\":\"and\"}}},\"selectedColumns\":[]}";

            }else if(entityFieldId != -1 && htmlType != -1) {

                payloadInclude = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":" + size + "," +
                        "\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                        "\"filterJson\":{\"" + filterId + "\":{\"multiselectValues\":{\"SELECTEDDATA\":[" + selectDataPayload + "]}," +
                        "\"filterId\":" + filterId + ",\"filterName\":\"" + filterName + "\",\"entityFieldHtmlType\":" + htmlType + "," +
                        "\"entityFieldId\":" + entityFieldId + ",\"operator\":\"and\"}}},\"selectedColumns\":[]}";
            }

            listRendererListData.hitListRendererListDataV2(listId,payloadInclude);
            listResponse = listRendererListData.getListDataJsonStr();

            if(!JSONUtility.validjson(listResponse)){
                customAssert.assertTrue(false,"List Response is an invalid json for filter " + filterName + " Option " + optionListName + " entity name " + entityName);
                return false;
            }

            Map<Integer,Map<String, String>> listIdValueMapWithOption = listRendererListData.getListColumnIdValueMap(listResponse);
            Map<String, String> listIdValueMapInvRow;

            listDataSizeWithThatOption =listIdValueMapWithOption.size();
            if(listDataSizeWithThatOption == 0){
                logger.debug("Listing Response contains zero records for Filter Name " + filterName + " and Entity " + entityName + " and option List " + expectedOption);
            }
            String payloadExclude = "";
            if(entityFieldId == -1 && htmlType == -1) {
                payloadExclude = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":" + size + "," +
                        "\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                        "\"filterJson\":{\"" + filterId + "\":{\"multiselectValues\":{\"SELECTEDDATA\":[" + selectDataPayload + "]}," +
                        "\"filterId\":" + filterId + ",\"filterName\":\"" + filterName + "\",\"entityFieldHtmlType\":null," +
                        "\"entityFieldId\":null,\"operator\":\"not\"}}},\"selectedColumns\":[]}";
            }else if(entityFieldId == -1 && htmlType != -1) {

                payloadExclude = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":" + size + "," +
                        "\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                        "\"filterJson\":{\"" + filterId + "\":{\"multiselectValues\":{\"SELECTEDDATA\":[" + selectDataPayload + "]}," +
                        "\"filterId\":" + filterId + ",\"filterName\":\"" + filterName + "\",\"entityFieldHtmlType\":" + htmlType + "," +
                        "\"entityFieldId\":null,\"operator\":\"not\"}}},\"selectedColumns\":[]}";

            }else if(entityFieldId != -1 && htmlType == -1) {

                payloadExclude = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":" + size + "," +
                        "\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                        "\"filterJson\":{\"" + filterId + "\":{\"multiselectValues\":{\"SELECTEDDATA\":[" + selectDataPayload + "]}," +
                        "\"filterId\":" + filterId + ",\"filterName\":\"" + filterName + "\",\"entityFieldHtmlType\":null," +
                        "\"entityFieldId\":" + entityFieldId + ",\"operator\":\"not\"}}},\"selectedColumns\":[]}";

            }else if(entityFieldId != -1 && htmlType != -1) {

                payloadExclude = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":" + size + "," +
                        "\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                        "\"filterJson\":{\"" + filterId + "\":{\"multiselectValues\":{\"SELECTEDDATA\":[" + selectDataPayload + "]}," +
                        "\"filterId\":" + filterId + ",\"filterName\":\"" + filterName + "\",\"entityFieldHtmlType\":" + htmlType + "," +
                        "\"entityFieldId\":" + entityFieldId + ",\"operator\":\"not\"}}},\"selectedColumns\":[]}";
            }
            listRendererListData.hitListRendererListDataV2(listId,payloadExclude);
            listResponse = listRendererListData.getListDataJsonStr();

            Map<Integer,Map<String, String>> listIdValueMapWithouOption = listRendererListData.getListColumnIdValueMap(listResponse);

            listDataSizeWithoutThatOption =listIdValueMapWithouOption.size();
//
            if(listDataSizeWithThatOption == 0 && defaultListDataSizeWithoutAnyFilter!=0){

                if(listDataSizeWithoutThatOption ==0 && defaultlistingPageHasValuesOtherThanExpOptions == true){
                    customAssert.assertTrue(false,"List Data has no response when Exclude option is chosen " + expectedOptionList + " and filter name " + filterName + " but has data when default filters have been applied");
                    return false;
                }
            }

            outerLoop:
            for (Map.Entry<Integer,Map<String, String>> entry: listIdValueMapWithouOption.entrySet()) {

                listIdValueMapInvRow = entry.getValue();

                listValue = listIdValueMapInvRow.get(listColId);

                if (listValue == null) {
                    continue;
                }
                if (listValue != null) {
                    if (expectedOption.contains(listValue)) {
                        for (int i = 0; i < expectedOptionList.size(); i++) {
                            if (listValue.contains(expectedOptionList.get(i))) {
                                logger.error("While validating Exclude Filters Option " + listValue + " present in the List Output It should not be present for filter name " + filterName);
                                customAssert.assertTrue(false, "While validating Exclude Filters Option " + listValue + " present in the List Output It should not be present for filter name " + filterName);
                                ;
                                break outerLoop;
                            }
                        }
                    }
                } else {
                    logger.debug("While validating Exclude Filters For Filter Name " + filterName + " and Option " + expectedOption + " present in the List Output Working as Expected");
                }

            }

        }catch (Exception e){
            logger.error("Exception while validating Exclude filter");
            customAssert.assertTrue(false,"Exception while validating Exclude filter " + e.getStackTrace());
            validationStatus = false;

        }
        return validationStatus;
    }

    private String getSelectDataPayload(ArrayList<String> optionListId,ArrayList<String> optionListName,int noOfOptionsToCheck){

        String selectDataPayload = "";
        try{

            for(int i = 0;i<optionListId.size();i++){

                if(i == noOfOptionsToCheck){
                    break;
                }
                selectDataPayload += "{\"id\": \"" + optionListId.get(i) + "\",\"name\": \"" + optionListName.get(i) + "\"},";
            }
            selectDataPayload = selectDataPayload.substring(0,selectDataPayload.length()-1);

        }catch (Exception e){

        }

        return selectDataPayload;
    }

    private HashMap<String,Boolean> createMapFilterEligiForOptions(String entityName,int listId,CustomAssert customAssert){

        HashMap<String,Boolean> filterIdEligiOption = new HashMap<>();
        try{
            ListRendererDefaultUserListMetaData listRendererDefaultUserListMetaData = new ListRendererDefaultUserListMetaData();
            listRendererDefaultUserListMetaData.hitListRendererDefaultUserListMetadata(listId);
            String listMetaDataResp = listRendererDefaultUserListMetaData.getListRendererDefaultUserListMetaDataJsonStr();

            if(!JSONUtility.validjson(listMetaDataResp)){
                customAssert.assertTrue(false,"List Meta Data Response is invalid json for entity " + entityName);
            }else {

                JSONObject listMetaDataRespJson = new JSONObject(listMetaDataResp);

                JSONArray filterMetaDataJsonArray = listMetaDataRespJson.getJSONArray("filterMetadatas");

                for(int i =0;i<filterMetaDataJsonArray.length();i++){

                    String filterName = filterMetaDataJsonArray.getJSONObject(i).get("defaultName").toString();
                    String filterId = filterMetaDataJsonArray.getJSONObject(i).get("id").toString();
                    try{

                        String notSupported = filterMetaDataJsonArray.getJSONObject(i).get("notSupported").toString();
                        String mustInclude =filterMetaDataJsonArray.getJSONObject(i).get("mustInclude").toString();
                        String blankSupport = filterMetaDataJsonArray.getJSONObject(i).get("blankSupport").toString();

                        if(notSupported.equals("false") || mustInclude.equals("false") || blankSupport.equals("false")){
                            filterIdEligiOption.put(filterId,false);
                        }else {
                            filterIdEligiOption.put(filterId,true);
                        }

                    }catch (Exception e){
                        logger.error("Exception while getting Include Exclude and Blank Option for filter name ");
                    }
                }
            }

        }catch (Exception e){
            logger.error("Exception occured while checking if Filter Eligible For Options");
        }
        return filterIdEligiOption;
    }

}
