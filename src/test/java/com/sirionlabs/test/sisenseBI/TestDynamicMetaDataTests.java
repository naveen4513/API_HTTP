package com.sirionlabs.test.sisenseBI;

import com.sirionlabs.api.commonAPI.Clone;
import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.sisenseBI.DataSources;
import com.sirionlabs.api.sisenseBI.SisenseLogin;
import com.sirionlabs.config.ConfigureConstantFields;
//import com.sirionlabs.config.ConfigureEntityTypeIdMapping;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.sisenseBI.DashboardHelper;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;


public class TestDynamicMetaDataTests {

    private final static Logger logger = LoggerFactory.getLogger(TestDynamicMetaDataTests.class);
    private String configFilePath;
    private String configFileName;
    private String authToken;

    String[] datasourcescolumnordering;

    List<String> multivaluescolumnorder;

    @BeforeClass
    private void BeforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("DynamicMetaDataConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("DynamicMetaDataConfigFileName");
        authToken = SisenseLogin.access_token;
    }

    @Test()
    public void TestDynamicMetaDataFields(){

        logger.info("Inside TestDynamicMetaDataFields method");

//        String entityName = "contract draft request";
        String entityName = "disputes";
        String showPageResponse;
        String datasourceResponse;

        HashMap<String,String> sisenseMap;
        HashMap<String,String> showpageDynamicFieldsMap = new HashMap<>();

        int entitytypeId = 28;
        int entityidtobecloned;
        int multicolomnField;

        String[] showfieldnames = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"showfieldnames").split(",");

        datasourcescolumnordering = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"datasourcescolumnordering").split(",");
        showPageResponse = getShowPageResponse(entitytypeId,1688);

        showpageDynamicFieldsMap = getFieldIdDynamicMetaData(showPageResponse,showfieldnames,entitytypeId,entityName);

        String dataSourcesPayload = createDataSourcesPayload(entityName);

        datasourceResponse = getDataSourcesResponse(entityName,dataSourcesPayload);
//        createMapForDataSources(datasourceResponse,entityName,3780);
        sisenseMap = createMapForDataSources(datasourceResponse,entityName,1581);

        showfieldnames = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"showfieldnames").split(",");

        entitytypeId = ConfigureConstantFields.getEntityIdByName(entityName);
        entityidtobecloned = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"entityidtobecloned"));

        String payloadForEntityCreation = createPayloadUsingClone(entityName,entityidtobecloned);
        int entityId =  entityCreationUsingClonePayload(entityName,payloadForEntityCreation);

        if(entityId == 0){
            logger.error("Error while creating entity");
        }else{

            showPageResponse = getShowPageResponse(entitytypeId,entityId);
            showpageDynamicFieldsMap = getFieldIdDynamicMetaData(showPageResponse,showfieldnames,entitytypeId,entityName);
        }

        validateShowPageAndSisenseResponse(showpageDynamicFieldsMap,sisenseMap);
    }


    private String getDataSourcesResponse(String entityName,String payload){

        DataSources dataSources = new DataSources();

        String dataSourceResponse = "";
        try {

            String title = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "datasourcetitle");
            dataSources.hitSisenseDataSourcesAPI(title, payload, authToken);
            dataSourceResponse = dataSources.getSisenseDataSourcesAPIResponseStr();

        }catch (Exception e){
            logger.error("Exception while getting Data Sources Response");
        }
        return dataSourceResponse;
    }
    private String createPayloadUsingClone(String entityName,int entityId ){

        Clone clone = new Clone();
        return clone.hitClone(entityName,entityId);
    }

    private int entityCreationUsingClonePayload(String entityName,String rawPayload){

        JSONObject payloadJson;
        Create create = new Create();
        String statusCode;
        String entityCreationReponse;
        String finalPayload;

        int entityId;
        try {
            payloadJson = new JSONObject(rawPayload);
            payloadJson.remove("header");
            payloadJson.remove("session");
            payloadJson.remove("actions");
            payloadJson.remove("createLinks");
            payloadJson.getJSONObject("body").remove("layoutInfo");
            payloadJson.getJSONObject("body").remove("globalData");
            payloadJson.getJSONObject("body").remove("errors");
            finalPayload = payloadJson.toString();

        }catch (Exception e){
            logger.error("Exception while parsing Payload");
            return 0;
        }
        try {
            create.hitCreate(entityName, finalPayload);
            entityCreationReponse = create.getCreateJsonStr();
            statusCode = create.getApiStatusCode();
            if(!(statusCode.equals("200"))){
                return 0;
            }
            try{
                entityId = CreateEntity.getNewEntityId(entityCreationReponse, entityName);
            }catch (Exception e){
                logger.error("Exception while getting entity id from create Response");
                return 0;
            }

        }catch (Exception e){
            logger.error("Exception while creating entity");
            return 0;
        }
        return entityId;
    }

    private String getShowPageResponse(int entitytypeId,int dbId){

        Show show = new Show();
        show.hitShow(entitytypeId,dbId);
        return show.getShowJsonStr();
    }

    private HashMap getFieldIdDynamicMetaData(String showpageResponse,String[] fieldLabels,int entitytypeid,String entityName){

        HashMap<String,String> fieldIdMap = new HashMap();
        String name;
        String fieldlabelvalue;
        int i = 0;
        try {
            multivaluescolumnorder = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "multivaluescolumnorder").split(","));
            JSONArray multiselectvaluesJsonArray;
            for (String fieldlabel : fieldLabels) {
                name = datasourcescolumnordering[i];
                fieldlabelvalue = ShowHelper.getValueOfField(entitytypeid, fieldlabel, showpageResponse);
                if (multivaluescolumnorder.contains(Integer.toString(i))) {

                    multiselectvaluesJsonArray = new JSONArray(fieldlabelvalue);
                    fieldlabelvalue = "";
                    for(int j =0; j < multiselectvaluesJsonArray.length(); j++){
                        fieldlabelvalue = fieldlabelvalue + "_" + multiselectvaluesJsonArray.getJSONObject(j).get("name").toString();
                    }
                }
                fieldIdMap.put(name, fieldlabelvalue);

                i = i + 1;
            }
        }catch (Exception e){
            logger.error("Exception while creating show page dynamic fields map");
        }
        return fieldIdMap;
    }

    private String createDataSourcesPayload(String entityName){

        Boolean isMaskedResult;

        String title;
        String widgetId;
        String dashboardId;
        String dashboardType;
        String dashboardTable;
        String metadatajsontocreate;
        String dashboardMetaDataString;
        JSONArray dashboardMetaData;
        JSONObject dataSourcesPayload = new JSONObject();

        DashboardHelper dashboardHelper = new DashboardHelper();

        try {
            metadatajsontocreate = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"metadatajsontocreate");
            title = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"title");
            dashboardId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"dashboardid");
            dashboardType = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"dashboardtype");
            widgetId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"widgetid");
            dashboardTable = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"dashboardtable");
            isMaskedResult = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"ismarkedresult"));
            if(metadatajsontocreate.equals("true")) {
                dashboardMetaData = dashboardHelper.getDashboardMetaData(dashboardId, dashboardType, authToken);
            }else {
                dashboardMetaDataString = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"metadatajson");
                dashboardMetaData = new JSONArray(dashboardMetaDataString);
            }
            dataSourcesPayload = createDashboardJson(title,dashboardId,dashboardType,dashboardTable,widgetId,isMaskedResult,dashboardMetaData);

        }catch (Exception e){
            logger.error("Exception while creating dataSources Payload JSON");
        }

        return dataSourcesPayload.toString();
    }

    private JSONObject createDashboardJson(String title,String dashboardId,String dashboardType,
                                           String dashboardTable,String widgetID,Boolean isMaskedResult,
                                           JSONArray metaData){

        String id = "aLOCALHOST_a" + title.replaceAll(" ","IAAa");

        JSONObject datasourceJson = new JSONObject();
        JSONObject dashbboardJson = new JSONObject();

        datasourceJson.put("title",title);
        datasourceJson.put("fullname","LocalHost/" + title);
        datasourceJson.put("id",id);
        datasourceJson.put("address","LocalHost");
        datasourceJson.put("database","a" + title.replaceAll(" ","IAAa"));
        datasourceJson.put("lastBuildTime","2019-01-23T21:33:31");
        datasourceJson.put("dashboard", dashboardId + ";" + dashboardType);
        dashbboardJson.put("datasource",datasourceJson);
        dashbboardJson.put("metadata",metaData);
        dashbboardJson.put("ungroup",true);
        dashbboardJson.put("count",220);
        dashbboardJson.put("offset",0);
        dashbboardJson.put("isMaskedResult", isMaskedResult);
        dashbboardJson.put("format", "json");
        dashbboardJson.put("dashboard",dashboardId + ";" + dashboardTable);
        dashbboardJson.put("queryGuid","52E72-3318-AD15-E098-B612-61C0-3F3C-4C4B-1");
        dashbboardJson.put("widget", widgetID + ";" + dashboardType);


        return dashbboardJson;
    }

    private HashMap<String,String> createMapForDataSources(String dataSourceResponse,String entityName,int entityidtobechecked){

        logger.info("Creating DataSources map from Data Sources JSON");

        JSONObject dataSourceResponseJson;
        JSONArray indvValuesJsonArray;
        JSONArray values;
        LinkedHashMap<String,String> dataSourceMap = new LinkedHashMap<>();

        String currentId;
        String key;
        String value = "";
        String columnvalue;
        String updatedvalue;

        try {
            datasourcescolumnordering = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"datasourcescolumnordering").split(",");
            multivaluescolumnorder = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"multivaluescolumnorder").split(","));
            dataSourceResponseJson = new JSONObject(dataSourceResponse);
            values = dataSourceResponseJson.getJSONArray("values");

            for(int i =0;i<values.length();i++){
                indvValuesJsonArray = values.getJSONArray(i);
                currentId = indvValuesJsonArray.getJSONObject(0).get("text").toString();

                if(currentId.equals(entityidtobechecked)) {

                    for(int j =0;j<indvValuesJsonArray.length();j++) {
                        columnvalue = indvValuesJsonArray.getJSONObject(j).get("text").toString();

                        if(multivaluescolumnorder.contains(Integer.toString(j))) {
                            if(dataSourceMap.containsKey(datasourcescolumnordering[j])){
                                value = dataSourceMap.get(datasourcescolumnordering[j]);
                            }

                            updatedvalue = value + "_" + columnvalue;
                            dataSourceMap.put(datasourcescolumnordering[j],updatedvalue);

                        }else {
                            dataSourceMap.put(datasourcescolumnordering[j], columnvalue);
                        }
                    }
                }
            }

        }catch (Exception e){
            logger.error("Exception while creating Map for Data Sources");
        }
        return dataSourceMap;
    }

    private Boolean validateShowPageAndSisenseResponse(HashMap<String,String> showpageMap,HashMap<String,String> SisenseMap){

        String key;
        Boolean validationresult = true;
        for(Map.Entry<String,String> entry : showpageMap.entrySet()){
            key = entry.getKey();
            if(showpageMap.get(key).equals(SisenseMap.get(key))){
                validationresult = false;
            }
        }
        return validationresult;
    }
}
