package com.sirionlabs.test.sisenseBI;


import com.sirionlabs.api.clientAdmin.fieldLabel.FieldRenaming;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.UpdateAccount;
import com.sirionlabs.api.sisenseBI.IntlMetadata;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.*;


import java.util.*;

public class TestInternationalisationFieldRenaming {

    private final static Logger logger = LoggerFactory.getLogger(TestInternationalisationFieldRenaming.class);
    private String configFilePath;
    private String configFileName;
    private String serverName;
    private String fieldRenamingUpdateResponseInitial;
    private String adminUserName;
    private String adminUserPassword;
    private String enduserUserName;
    private String enduserPassword;
    private Integer portNumber;
    private Map<String,String> fieldNameUpdated = new HashMap<>();

    @BeforeClass
    public void beforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("IntlConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("IntlConfigFileName");

        serverName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"serverclient") + "://" + ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"serverhost") + ":" + ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"serverport");
        portNumber = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"analyticsserviceportnumber"));
        adminUserName = "ekta_admin";
        adminUserPassword = "admin123";

        enduserUserName = "rbac_user1";
        enduserPassword = "admin123";
    }

    @DataProvider
    public Object[][] dataProviderForLanguageIds() {

        logger.info("Setting all Language Ids to Test.");
        String[] languageidstotest;
        String[]  entitiestotest = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"entitiestotest").split(",");

        int rowCount = 0;
        int languageidsCount = 0;
        for(String entity : entitiestotest){
            languageidstotest = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"languageidstotest",entity).split(",");
            languageidsCount = languageidsCount + languageidstotest.length;
        }

        Object[][] allTestData = new Object[languageidsCount][2];

        for(String entity : entitiestotest){
            languageidstotest = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"languageidstotest",entity).split(",");

            for (String languageid : languageidstotest) {
                try {
                    allTestData[rowCount][0] = entity;
                    allTestData[rowCount][1] = languageid;
                }catch (Exception e){
                    logger.error(" " + e.getStackTrace());
                }
                rowCount = rowCount + 1;

            }

        }

        return allTestData;
    }

    @BeforeMethod()
    public void beforeMethod(){

        login(adminUserName,adminUserPassword);
    }

    @Test(dataProvider = "dataProviderForLanguageIds")
    public void testFieldRenamingEntities(String entity,String languageid){

        CustomAssert csAssert = new CustomAssert();
        TestAnalyticsServiceLogin testAnalyticsServiceLogin = new TestAnalyticsServiceLogin();

        try {

            int langId = Integer.parseInt(languageid);
            logger.info("Validating Field Renaming for entity {} and language id {} and ",entity, langId);

            int groupId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"groupids",entity));
            logger.info("Updating field names for entity [ {} ],groupID [ {} ],langID [ {} ] ",entity,groupId,langId);

            login(adminUserName,adminUserPassword);
            String payloadForUpdatingFieldNames = CreateUpdateFieldsPayload(langId, groupId);

            String response = updateFields(payloadForUpdatingFieldNames);

            JSONObject responseJson = new JSONObject(response);

            if (responseJson.get("isSuccess").equals(true)) {
                logger.info("Field Names Updated Successfully");
                csAssert.assertTrue(true, "Field Names Updated Successfully");
            } else {
                logger.error("Unable to update field names ");
                csAssert.assertTrue(false, "Unable to update field names ");
            }

            login(enduserUserName,enduserPassword);
            testAnalyticsServiceLogin.testAnalyticsServiceLogin();

            if(!(updateUserLanguage(languageid))){
                throw new SkipException("Couldn't update User Account Settings for User [ "  + enduserUserName + " . Hence skipping test.");
            }

            logger.info("Getting meta data response");
            IntlMetadata intlMetadata = new IntlMetadata();
            intlMetadata.hitIntlMetaData(serverName,"1",portNumber);
            String intlmetadataResponse = intlMetadata.getintlMetaDataResponseStr();

            String[] linkedTables = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"linkedtables",entity).split(",");
            String[] columnsToSkip = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"fieldnamestoskip",entity).split(",");
            String[] tablesToSkip = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"tablestoskip",entity).split(",");

            JSONObject intlmetadataResponseJson = new JSONObject(intlmetadataResponse);
            if(intlmetadataResponseJson.has("error")){
                logger.error("Unable to fetch metadata response");
            }else {
                logger.info("Meta Data response fetch successfully");
                logger.info("Meta Data response " + intlmetadataResponseJson);
                validatemetadataResponse(intlmetadataResponse,linkedTables,columnsToSkip,tablesToSkip,csAssert);
            }

        }catch (Exception e){
            logger.error("Exception while validating field renaming for the entity {} and language id {} and exception {}",entity,languageid,e.getStackTrace());
            csAssert.assertTrue(false,"Exception while validating field renaming for the entity "+ entity + "and language id" + languageid);

        }finally {
            login(adminUserName,adminUserPassword);
            updateFields(fieldRenamingUpdateResponseInitial);
            login(enduserUserName,enduserPassword);
            testAnalyticsServiceLogin.testAnalyticsServiceLogin();
            fieldNameUpdated.clear();

        }
        csAssert.assertAll();
    }

    private void validatemetadataResponse(String intlmetadataResponse,String[] linkedTables,String[] columnstoskip,String[] tablestoskip,CustomAssert csAssert){

        logger.info("Inside validatemetadataResponse Method");
        String fieldNameAdminName = "";
        String fieldNameAdminValue;
        String fieldNameMetaDataValue;

        try {
            JSONObject metaDataResponseJson = new JSONObject(intlmetadataResponse);

            JSONObject columnsJson;
//            JSONObject foldersJson;
//            JSONObject dashboardsJson;

            String tableName;
            String columnNameMetaData;
            String value;
            Map<String, String> fieldsmapMetaData = new HashMap<>();
            List<String> linkedTableList = Arrays.asList(linkedTables);
            List<String> columnsToSkip = Arrays.asList(columnstoskip);
            List<String> tablesToSkip = Arrays.asList(tablestoskip);

            if (metaDataResponseJson.has("tables")) {

                columnsJson = metaDataResponseJson.getJSONObject("tables");
                Iterator allTables = columnsJson.keys();
                String key;
                while (allTables.hasNext()) {
                    tableName = (String) allTables.next();
                    key = tableName + " Table";
                    value = columnsJson.get(tableName).toString();
                    fieldsmapMetaData.put(key, value);

                }
            }

//
//        if(metaDataResponseJson.has("widgets")){
//
//            JSONObject widgetsJson = metaDataResponseJson.getJSONObject("widgets");
//
//            Iterator itr = widgetsJson.keys();
//
//
//            while (itr.hasNext()) {
//                key = (String) itr.next();
//                value = widgetsJson.get(key).toString();
//
//                fieldsmapMetaData.put(key,value);
//            }
//
//        }

//        if(metaDataResponseJson.has("folders")){
//
//            foldersJson = metaDataResponseJson.getJSONObject("folders");
//
//        }
//
//        if(metaDataResponseJson.has("dashboards")){
//
//            dashboardsJson = metaDataResponseJson.getJSONObject("dashboards");
//        }

            if (metaDataResponseJson.has("columns")) {

                columnsJson = metaDataResponseJson.getJSONObject("columns");
                JSONObject indvcolumnJson;
                Iterator allTables = columnsJson.keys();

                while (allTables.hasNext()) {
                    tableName = (String) allTables.next();

                    if (!(linkedTableList.contains(tableName))){
                        continue;
                    }
                    indvcolumnJson = columnsJson.getJSONObject(tableName);

                    Iterator tableColumns = indvcolumnJson.keys();
                    while (tableColumns.hasNext()) {
                        columnNameMetaData = (String) tableColumns.next();
                        value = indvcolumnJson.get(columnNameMetaData).toString();

                        fieldsmapMetaData.put(columnNameMetaData, value);

                    }
                }
            }

            for (Map.Entry<String, String> fieldnameKeyValue : fieldNameUpdated.entrySet()) {

                fieldNameAdminName = fieldnameKeyValue.getKey();
                fieldNameAdminValue = fieldnameKeyValue.getValue();
                if((columnsToSkip.contains(fieldNameAdminName)) || (tablesToSkip.contains(fieldNameAdminName))){
                    continue;
                }
                if (fieldsmapMetaData.containsKey(fieldNameAdminName)) {
                    fieldNameMetaDataValue = fieldsmapMetaData.get(fieldNameAdminName);
                    if (fieldNameAdminValue.equals(fieldNameMetaDataValue)) {
                        logger.info("Valid value of the field {} and value {}", fieldNameAdminName, fieldNameAdminValue);
                    } else {
                        logger.error("Field renamed values mismatch from metadata response and admin value for the field {} and value {} and actual value from meta data response {}", fieldNameAdminName, fieldNameAdminValue, fieldNameMetaDataValue);
                        csAssert.assertTrue(false,"Field renamed values mismatch for the field " + fieldNameAdminName);
                    }
                } else {
                    logger.error("Meta Data Response doesn't contain renamed field " + fieldNameAdminName);
                    csAssert.assertTrue(false,"Meta Data Response doesn't contain renamed field " + fieldNameAdminName);
                }
            }
        }catch (Exception e){
            csAssert.assertTrue(false,"Meta Data Response doesn't contain renamed field " + fieldNameAdminName);
            logger.error("Exception while validating metadata response " + e.getStackTrace());
        }
    }

    private String updateFields(String payload){
        logger.info("Updating Field Names Using the payload " + payload);
        String response = "";

        try {
            FieldRenaming fieldRenaming = new FieldRenaming();
            response = fieldRenaming.hitFieldUpdate(payload);


        }catch (Exception e){
            logger.error("Exception while updating field names " + e.getStackTrace());
        }
        return response;
    }

    private String CreateUpdateFieldsPayload(int languageid,int groupId){

        logger.info("Inside Update Fields Method");

        JSONArray childGroups;
        JSONObject fieldRenamingUpdateResponseJson = new JSONObject();
        String fieldRenamingUpdateResponseToCreatePayload;
        JSONObject indvfieldlabelJson;
        String name;
        String clientFieldName;
        String clientFieldNameUpdated;

        try {
            FieldRenaming fieldRenaming = new FieldRenaming();
            fieldRenamingUpdateResponseInitial = fieldRenaming.hitFieldRenamingUpdate(languageid, groupId);

            fieldRenamingUpdateResponseToCreatePayload = fieldRenamingUpdateResponseInitial;

            fieldRenamingUpdateResponseJson = new JSONObject(fieldRenamingUpdateResponseToCreatePayload);

            logger.info("Initial Json for field rename is " + fieldRenamingUpdateResponseJson);

            JSONObject analyticsfieldJson = new JSONObject();
            childGroups = fieldRenamingUpdateResponseJson.getJSONArray("childGroups");
            int childgroupupdatepositionforanalyticstable = -1;

            JSONObject childGroupJson = new JSONObject();

            for(int i =0;i<childGroups.length();i++){
                childGroupJson = childGroups.getJSONObject(i);
                if(childGroupJson.get("name").toString().equals("Analytics Table")){

                    analyticsfieldJson = childGroupJson;
                    childgroupupdatepositionforanalyticstable = i;
                    break;

                }
            }

            JSONArray fieldLabelsJsonArray = analyticsfieldJson.getJSONArray("fieldLabels");

            for(int i =0 ;i<fieldLabelsJsonArray.length();i++){

                indvfieldlabelJson = fieldLabelsJsonArray.getJSONObject(i);
                name = indvfieldlabelJson.get("name").toString();
                clientFieldName = indvfieldlabelJson.get("clientFieldName").toString();
                clientFieldNameUpdated = clientFieldName + "apiautomation";
                indvfieldlabelJson.put("clientFieldName",clientFieldNameUpdated);
                fieldNameUpdated.put(name, clientFieldNameUpdated);
                fieldLabelsJsonArray.put(i,indvfieldlabelJson);
            }
            childGroupJson.put("fieldLabels",fieldLabelsJsonArray);

            childGroups.put(childgroupupdatepositionforanalyticstable,childGroupJson);

            fieldRenamingUpdateResponseJson.put("childGroups",childGroups);
            logger.info("Updated json for field renaming is " + fieldRenamingUpdateResponseJson);

        }catch (Exception e){
            logger.error("Exception while parsing field Rename API Response " + e.getStackTrace());
        }

        return fieldRenamingUpdateResponseJson.toString();
    }

    public void login(String username,String password) {
        Check check = new Check();
        check.hitCheck(username, password);
    }

    private Boolean updateUserLanguage(String langId){

        logger.info("Updating user language");
        Map<String, String> params = new LinkedHashMap<>();
        Map<String, String> paramsMap;

        Boolean userSettingsUpdated = false;
        logger.info("Getting Default User Settings");
        try {
            Map<String, String> defaultUserSettingsMap = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileName, "default properties");

            paramsMap = new LinkedHashMap<>();
            for (Map.Entry<String, String> defaultSetting : defaultUserSettingsMap.entrySet()) {
                paramsMap.put(defaultSetting.getKey(), defaultSetting.getValue().trim());
            }

            paramsMap.put("language.id", langId);
            paramsMap.put("timeZone.id", paramsMap.get("timeZone"));
            paramsMap.remove("language");
            paramsMap.remove("timeZone");
            paramsMap.put("_csrf_token", ConfigureEnvironment.getEnvironmentProperty("x-csrf-token"));
            params.putAll(paramsMap);

            UpdateAccount updateObj = new UpdateAccount();
            Integer updateStatusCode = updateObj.hitUpdateAccount(params);

            if (updateStatusCode == 302) {
                userSettingsUpdated = true;
                logger.info("User Account Settings updated for the user [ "+ paramsMap.get("firstName") + paramsMap.get("lastName") + " ]");
            } else {
                throw new SkipException("Couldn't update User Account Settings for User [ "  + paramsMap.get("firstName") + paramsMap.get("lastName") + " ] . Hence skipping test.");
            }
        }catch (Exception e){

            logger.error("Exception while  updating user language");
            userSettingsUpdated = false;
        }
        return userSettingsUpdated;
    }

}
