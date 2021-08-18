package com.sirionlabs.test.insights;

import com.sirionlabs.api.insights.EntityList;

import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import java.util.ArrayList;
import java.util.List;


public class TestInsightsValidations {

    private final static Logger logger = LoggerFactory.getLogger(TestInsightsValidations.class);

    private String configFilePath;
    private String configFileName;


    @BeforeClass
    public void configureProperties() {
        try {
            configFilePath = ConfigureConstantFields.getConstantFieldsProperty("InsightsValidationConfigFilePath");
            configFileName = ConfigureConstantFields.getConstantFieldsProperty("InsightsValidationConfigFileName");

        } catch (Exception e) {
            logger.error("Exception occurred while setting the configuration properties for insights validation. error = {}", e.getMessage());
            e.printStackTrace();
        }
    }

    @DataProvider(name = "getInsightListIdComputationId", parallel = true)
    public Object[][] getInsightListIdComputationId() {

        Object[][] groupArray = null;
        //List<String> entitySectionsToTest = null;

        try {

            EntityList entityListObj = new EntityList();
            entityListObj.hitInsightsEntityList();
            String response = entityListObj.getInsightsEntityListJsonStr();
            logger.info("insights entity-list response = {}", response);

            JSONArray jsonArray =  new JSONArray(response);
            JSONObject jobj;
            JSONArray insights;
            //Map<Integer,List<Integer>> listidcomputationid= new HashMap<>();

            Integer listId = 0;
            Integer computationId;
            JSONObject insightObject;
            groupArray = new Object[jsonArray.length()][];
            for(int i = 0;i<jsonArray.length();i++){
                jobj = jsonArray.getJSONObject(i);
                insights = jobj.getJSONArray("insights");
                groupArray[i] = new Object[2];
                List<Integer> computationIdList = new ArrayList<>();
                for(int j =0;j<insights.length();j++){
                    insightObject = insights.getJSONObject(j);
                    listId = insightObject.getInt("listId");
                    groupArray[i][0] = listId;
                    computationId = insightObject.getInt("insightComputationId");
                    computationIdList.add(computationId);

                }
                groupArray[i][1] = computationIdList;
            }

        } catch (Exception e) {
            logger.error("Exception occurred while getting entities section for dataProvider");
            e.printStackTrace();
        }

        return groupArray;
    }


    //added by gaurav bhadani on 25 july 2018
    @Test(dataProvider = "getInsightListIdComputationId",enabled = true)
    public void testInsightFilter(Integer listId,List<Integer> computationIdList){
        logger.info("Validating insights with filters");

        CustomAssert csAssert = new CustomAssert();

        try {

            ListRendererListData listObj = new ListRendererListData();
            String listDataFilterResponse;
            String selectedData = null;
            String name;
            List<Integer> computationIds = computationIdList;

            for(Integer computationId : computationIds){
                String computationIdStr = String.valueOf(computationId);

                listObj.hitListRendererFilterDataComputation(listId,computationId);
                listDataFilterResponse = listObj.getListDataJsonStr();
                JSONObject listDataFilterResponseJson = new JSONObject(listDataFilterResponse);

                String filterIds = getFilterIds(computationId);

                JSONArray jArray = JSONUtility.convertJsonOnjectToJsonArray(listDataFilterResponseJson);

                for(int k = 0;k < jArray.length();k++){

                    JSONObject filterJson = jArray.getJSONObject(k);
                    String actualFilterId = String.valueOf(filterJson.getInt("filterId"));
                    if(filterIds.contains(actualFilterId)){

                        JSONArray selectDataArray = filterJson.getJSONObject("multiselectValues").getJSONArray("SELECTEDDATA");
                        try {
                            System.out.println(computationIdStr);
                            System.out.println(actualFilterId);
                            selectedData = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "filteroptionvalues", computationIdStr + "->" + actualFilterId);
                        }catch (Exception e){
                            logger.error("Exception while fetching filter option values for computationId " + computationIdStr + "filterId " + actualFilterId);
                            csAssert.assertTrue(false,"Exception while fetching filter option values for computationId " + computationIdStr + "filterId " + actualFilterId);
                        }
                        if(selectDataArray.length() >0 && selectedData!=null){
                            for(int i =0;i<selectDataArray.length();i++){

                                name = selectDataArray.getJSONObject(i).getString("name").split(":;")[0];
                                if(selectedData.toLowerCase().contains(name.toLowerCase())){
                                    logger.info("Expected option value {} present for filter id {}" , name,actualFilterId);
                                    csAssert.assertTrue(true,"Expected option value present for filter id " + actualFilterId);
                                }
                                else {
                                    logger.error("Unexpected option value present for filter id " + actualFilterId);
                                    csAssert.assertTrue(false,"Unexpected option value present for filter id " + actualFilterId + " and computation id" + computationIdStr);
                                }
                            }
                        }else if(selectDataArray.length() == 0 && selectedData !=null){
                            logger.error("Filter insight doesn't contain options to validate");
                            csAssert.assertTrue(false,"Filter insight doesn't contain options to validate");
                        }else if(selectDataArray.length() > 0 && selectedData ==null){
                            logger.error("Filter insight contain unexpected options to validate");
                            csAssert.assertTrue(false,"Filter insight contain unexpected options to validate");
                        }

                    }
                    else{
                        if(filterJson.getJSONObject("multiselectValues").getJSONArray("SELECTEDDATA").length() > 0){
                            logger.error("Unexpected values present in SELECTED DATA for the filterId " + actualFilterId + "computation id " + computationId + "list data id " + listId);
                            csAssert.assertTrue(false,"Unexpected values present in SELECTED DATA for the filterId " + actualFilterId + "computation id " + computationId + "list data id " + listId);
                        }
                        else {
                            logger.info("Unexpected values not present in SELECTED DATA for the filterid " + actualFilterId + "computation id " + computationId + "list data id " + listId);
                            csAssert.assertTrue(true,"Unexpected values not present in SELECTED DATA for the filterId " + actualFilterId + "computation id " + computationId + "list data id " + listId);
                        }
                    }
                }
            }
        }catch (Exception e){
            logger.error("Exception while verifying filter data from list data filter response json" + e.getMessage());
            e.printStackTrace();
            csAssert.assertTrue(false,"Exception while verifying filter data from list data filter response json");
        }
        csAssert.assertAll();
    }




    private String getFilterIds(int computationid){
        String filterIds = null;
        try {
            filterIds = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"insightcomputationidfiltermapping",String.valueOf(computationid));

        }catch (Exception e){
            logger.error("Exception occurred while parsing config file", e.getMessage());
            e.printStackTrace();
        }
        return filterIds;
    }
}
