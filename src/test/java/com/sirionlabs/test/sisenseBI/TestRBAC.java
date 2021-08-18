package com.sirionlabs.test.sisenseBI;

import com.sirionlabs.api.sisenseBI.DataSecurity;
import com.sirionlabs.api.sisenseBI.DataSources;
import com.sirionlabs.api.sisenseBI.ElasticCubes;
import com.sirionlabs.api.sisenseBI.SisenseLogin;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import java.util.*;

public class TestRBAC {

    private final static Logger logger = LoggerFactory.getLogger(TestRBAC.class);
    private String configurationFilePath;
    private String configurationFileName;
    private String server;
    private String format;
    private String accessToken;
    private String dbhostaddress;
    private String dbportname;
    private String dbname;
    private String dbusername;
    private String dbpassword;

    private String[] useridstotest;

    @BeforeClass
    public void beforeClass() {

        configurationFilePath = ConfigureConstantFields.getConstantFieldsProperty("RBACConfigFilePath");
        configurationFileName = ConfigureConstantFields.getConstantFieldsProperty("RBACConfigFileName");
        server = ParseConfigFile.getValueFromConfigFile(configurationFilePath, configurationFileName, "server");
        format = "{\"mask\": {\"type\": \"number\",\"abbreviations\": {\"t\": true,\"b\": true,\"m\": true,\"k\": false},\"separated\": true,\"decimals\": \"auto\",\"isdefault\": true}}";

        accessToken = SisenseLogin.access_token;

        useridstotest = ParseConfigFile.getValueFromConfigFile(configurationFilePath, configurationFileName, "useridstotest").split(",");

        dbhostaddress = ParseConfigFile.getValueFromConfigFile(configurationFilePath, configurationFileName, "dbhostaddress");
        dbportname = ParseConfigFile.getValueFromConfigFile(configurationFilePath, configurationFileName, "dbportname");
        dbname = ParseConfigFile.getValueFromConfigFile(configurationFilePath, configurationFileName, "dbname");
        dbusername = ParseConfigFile.getValueFromConfigFile(configurationFilePath, configurationFileName, "dbusername");
        dbpassword = ParseConfigFile.getValueFromConfigFile(configurationFilePath, configurationFileName, "dbpassword");
    }

    @DataProvider
    public Object[][] dataProviderForRBACUserLevelAccess() {

        List<Object[]> entityToTest = new ArrayList<>();
        String entitytotest = ParseConfigFile.getValueFromConfigFile(configurationFilePath, configurationFileName, "entitytotest");

        String[] entitytotestArray = entitytotest.split(",");
        for (String entity : entitytotestArray) {
            entityToTest.add(new Object[]{entity});
        }
        return entityToTest.toArray(new Object[0][]);
    }

    //This test is to validate the user level access for entity ids for Sisense returning Dashboard Data
    @Test(dataProvider = "dataProviderForRBACUserLevelAccess")
    public void testUserLevelAccessID(String entityName) {

        logger.info("Inside testUserLevelAccessID method");
        logger.info("Validating IDs for User with RBAC permissions");

        CustomAssert csAssert = new CustomAssert();
        //Variable Declarations
        String title;
        String tablename;
        String table;
        String dashboardid;
        String widget;
        String dashboardName;
        String cubeName;
        String column;
        String partyIdtotest;
        String type;
        String partyId;
        String cubeId = "";
        String payloadDataSourcesAPI;
        String datasourceAPIResponse;
        String SQLQueryToHit;
        String datasourceId;
        String dataSecurityPayload;
        String sisenseDataSecurityAPIResponseStr;

        JSONObject datasourceAPIResponseJson;
        JSONArray values;
        JSONArray subvalues;
        JSONArray sisenseDataSecurityAPIResponseArray;
        JSONObject sisenseDataSecurityAPIResponseJsonObject;
        HttpResponse response = null;
        List<List<String>> results;
        //
        String SisenseGetSQLQueryFromEntityTableResponse;
        String SisenseGetSQLQueryFromUserTableResponse;
        ElasticCubes elasticCubes = new ElasticCubes();


        DataSources dataSources = new DataSources();
        DataSecurity dataSecurity = new DataSecurity();
        //TreeSet<Integer> jaqlResultSet = new TreeSet<>();
        //TreeSet<Integer> sqlResultSet = new TreeSet<>();

        HashSet<String> jaqlResultSet = new HashSet<>();
        HashSet<String> sqlResultSet = new HashSet<>();

        try {
            title = ParseConfigFile.getValueFromConfigFile(configurationFilePath, configurationFileName, entityName, "title");
            tablename = ParseConfigFile.getValueFromConfigFile(configurationFilePath, configurationFileName, entityName, "tablename");
            table = ParseConfigFile.getValueFromConfigFile(configurationFilePath, configurationFileName, entityName, "table");
            dashboardid = ParseConfigFile.getValueFromConfigFile(configurationFilePath, configurationFileName, entityName, "dashboardid");
            widget = ParseConfigFile.getValueFromConfigFile(configurationFilePath, configurationFileName, entityName, "widget");
            dashboardName = ParseConfigFile.getValueFromConfigFile(configurationFilePath, configurationFileName, entityName, "dashboardname");
            cubeName = ParseConfigFile.getValueFromConfigFile(configurationFilePath, configurationFileName, entityName, "cubename");
            column = ParseConfigFile.getValueFromConfigFile(configurationFilePath, configurationFileName, entityName, "column");
            partyIdtotest = ParseConfigFile.getValueFromConfigFile(configurationFilePath, configurationFileName, entityName, "partyidtotest");

        } catch (Exception e) {
            logger.error("Exception while fetching data from configuration file " + e.getStackTrace());
            csAssert.assertTrue(false,"Exception while fetching data from configuration file " + e.getStackTrace());
            return;
        }
        try {

            String sqlIdSelect = ParseConfigFile.getValueFromConfigFile(configurationFilePath, configurationFileName, entityName, "sqlidselect");

            elasticCubes.hitSisenseGetSQLQueryFromTableAPI(server, title, tablename, accessToken);
            SisenseGetSQLQueryFromEntityTableResponse = elasticCubes.getSQLQueryFromTableAPIResponseStr();

            JSONObject SisenseGetSQLQueryFromEntityTableResponseJson = new JSONObject(SisenseGetSQLQueryFromEntityTableResponse);
            String SQLQueryEntity = SisenseGetSQLQueryFromEntityTableResponseJson.getJSONObject("table").get("queryString").toString();

            logger.info("SQL Query for getting records from entity table for entity : " + entityName + " [" + SQLQueryEntity + " ]");

            elasticCubes.hitSisenseGetSQLQueryFromTableAPI(server, title, "User%20Access", accessToken);
            SisenseGetSQLQueryFromUserTableResponse = elasticCubes.getSQLQueryFromTableAPIResponseStr();

            JSONObject SisenseGetSQLQueryFromUserAccessTableResponseJson = new JSONObject(SisenseGetSQLQueryFromUserTableResponse);
            String SQLQueryUserAccess = SisenseGetSQLQueryFromUserAccessTableResponseJson.getJSONObject("table").get("queryString").toString();

            logger.info("SQL Query for getting records from user access table for entity : " + entityName + " [ " +SQLQueryUserAccess + " ]" );

            String sqentityselect = ParseConfigFile.getValueFromConfigFile(configurationFilePath, configurationFileName, entityName, "sqentityselect");
            String sqluseridcheck = ParseConfigFile.getValueFromConfigFile(configurationFilePath, configurationFileName, entityName, "sqluseridcheck");
            String sqluseridupdated;

            String sqlendparanthesis = ParseConfigFile.getValueFromConfigFile(configurationFilePath, configurationFileName, entityName, "sqlendparanthesis");

            outerloop:
            for (String useridintest : useridstotest) {

                logger.info("***************************************************************");
                logger.info("Checking the RBAC functionality for the user {}",useridintest);
                logger.info("***************************************************************");

                sqluseridupdated = sqluseridcheck.replace("USERID", useridintest);

                elasticCubes.hitSisenseGetDataSecurityAPI(title, accessToken);

                sisenseDataSecurityAPIResponseStr = elasticCubes.getSisenseGetDataSecurityAPIResponseStr();

                sisenseDataSecurityAPIResponseArray = new JSONArray(sisenseDataSecurityAPIResponseStr);

                innerloop:
                for (int i = 0; i < sisenseDataSecurityAPIResponseArray.length(); i++) {

                    sisenseDataSecurityAPIResponseJsonObject = sisenseDataSecurityAPIResponseArray.getJSONObject(i);

                    type = sisenseDataSecurityAPIResponseJsonObject.getJSONArray("shares").getJSONObject(0).get("type").toString();
                    if (sisenseDataSecurityAPIResponseJsonObject.get("column").toString().equals("User Id") && type.equals("user")) {

                        partyId = sisenseDataSecurityAPIResponseJsonObject.getJSONArray("shares").getJSONObject(0).get("partyId").toString();

                        if (partyId.equals(partyIdtotest)) {
                            cubeId = sisenseDataSecurityAPIResponseJsonObject.get("_id").toString();
                            break innerloop;
                        }
                    }
                }

                //Updating cube userid
                dataSecurityPayload = "{\"members\":[\"" + useridintest + "\"],\"allMembers\":null}";
                try {
                    response = dataSecurity.hitDataSecurity(cubeId, dataSecurityPayload,accessToken);
                } catch (Exception e) {
                    logger.error("Exception while updating datasecurity for the user " + useridintest + e.getStackTrace());
                    csAssert.assertTrue(false,"Exception while updating datasecurity for the user " + useridintest + e.getStackTrace());

                }

                if (!(response.getStatusLine().getStatusCode() == 200)) {
                    logger.error("Unable to update datasecurity for the entity " + entityName + "for user id : " + useridintest);
                    continue outerloop;
                } else {

                    SQLQueryToHit = sqlIdSelect + SQLQueryEntity + " " + sqentityselect + SQLQueryUserAccess + " " + sqluseridupdated + sqlendparanthesis;

                    logger.info("Fetching data for entity [ " + entityName + " ] and user id [ " + useridintest + " ] using SQL [ " + SQLQueryToHit + " ]");

                    PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbhostaddress, dbportname, dbname, dbusername, dbpassword);
                    results = postgreSQLJDBC.doSelect(SQLQueryToHit);

                    postgreSQLJDBC.closeConnection();

                    int resultsSizeSQL = results.size();
                    logger.info("***************************************************************");
                    logger.info("Results from sql query for the entity : ["+ entityName + " ] for user id : [ " + useridintest + " ] equals [ " + resultsSizeSQL + " ]" );
                    logger.info("***************************************************************");
                    for (List<String> idLinkedList : results) {
                        sqlResultSet.addAll(idLinkedList);
                    }

                    payloadDataSourcesAPI = createPayloadforDatasourcesAPI(dashboardid, widget, table, column, cubeName, dashboardName);

                    //Hitting Sisense Data sources API
                    dataSources.hitSisenseDataSourcesAPI(title, payloadDataSourcesAPI, accessToken);
                    datasourceAPIResponse = dataSources.getSisenseDataSourcesAPIResponseStr();

                    datasourceAPIResponseJson = new JSONObject(datasourceAPIResponse);

                    if (datasourceAPIResponseJson.has("error")) {
                        logger.error("Error while hiting datasources JAQL API for entity " + entityName + "for user" + useridintest + datasourceAPIResponseJson.get("details").toString());
                        csAssert.assertTrue(false,"Error while hiting datasources JAQL API for entity " + entityName + datasourceAPIResponseJson.get("details").toString());
                        csAssert.assertAll();
                        return;
                    }
                    values = datasourceAPIResponseJson.getJSONArray("values");

                    for (int i = 0; i < values.length(); i++) {

                        subvalues = values.getJSONArray(i);

                        for (int j = 0; j < subvalues.length(); j++) {

                            datasourceId = subvalues.getJSONObject(j).get("text").toString();
                            jaqlResultSet.add(datasourceId);
                        }
                    }

                    int resultsSizeJaql = jaqlResultSet.size();
                    logger.info("***************************************************************");
                    logger.info("Results from datsources JAQL call for the entity : ["+ entityName + " ]" + " user id : [" + useridintest + " ] equals [ " + resultsSizeJaql + " ]" );
                    logger.info("***************************************************************");

                    if (resultsSizeJaql == resultsSizeSQL) {
                        logger.info("SQL query returned Results and Jaql Response results are equal for entity [ " + entityName + " ]and userid : [ " + useridintest + " ]");
                        logger.debug("Result Size SQL [ {} ] and result size JAQL [ {} ]",resultsSizeSQL,resultsSizeJaql);
                        for(String jaqlValue : jaqlResultSet){

                            if (!sqlResultSet.contains(jaqlValue)) {
                                logger.error("Result SQL does not contain the ID " + jaqlValue + "for entity [ " + entityName + " ]and userid : [ " + useridintest + " ]");
//                                csAssert.assertTrue(false,"Result SQL does not contain the ID " + jaqlValue + "for entity [ " + entityName + " ]and userid : [ " + useridintest + " ]");
                            }
                        }

                        for(String sqlValue : sqlResultSet){

                            if (!jaqlResultSet.contains(sqlValue)) {
                                logger.error("Result JAQL does not contain the ID " + sqlValue + "for entity [ " + entityName + " ]and userid : [ " + useridintest + " ]");
//                                csAssert.assertTrue(false,"Result JAQL does not contain the ID " + sqlValue + "for the entity" + entityName);
                            }
                        }

                    } else if (resultsSizeJaql > resultsSizeSQL) {

                        logger.error("Total Result size from sql and jaql not equalfor entity [ " + entityName + " ]and userid : [ " + useridintest + " ]");
                        logger.debug("Result Size SQL [ {} ] and result size JAQL [ {} ]",resultsSizeSQL,resultsSizeJaql);
                        csAssert.assertTrue(false,"Total Result size from sql and jaql not equal for user " + useridintest + "sql result size [ " + resultsSizeSQL + " ] and jaql result size [ " + resultsSizeJaql+ "  ]");

                        for(String jaqlValue : jaqlResultSet){

                            if (!sqlResultSet.contains(jaqlValue)) {
                                logger.error("Result SQL does not contain the ID " + jaqlValue + "for entity [ " + entityName + " ]and userid : [ " + useridintest + " ]");
//                                csAssert.assertTrue(false,"Result SQL does not contain the ID " + jaqlValue + "for entity [ " + entityName + " ]and userid : [ " + useridintest + " ]");
                            }
                        }

                    } else if (resultsSizeJaql < resultsSizeSQL) {

                        logger.error("Total Result size from sql and jaql not equal for entity [ " + entityName + " ]and userid : [ " + useridintest + " ]");
                        logger.debug("Result Size SQL [ {} ] and result size JAQL [ {} ]",resultsSizeSQL,resultsSizeJaql);

                        csAssert.assertTrue(false,"Total Result size from sql and jaql not equal for user " + useridintest + "sql result size [ " + resultsSizeSQL + " ] and jaql result size [ " + resultsSizeJaql+ "  ]");

                        for(String sqlValue : sqlResultSet){

                            if (!jaqlResultSet.contains(sqlValue)) {
                                logger.error("Result JAQL does not contain the ID " + sqlValue + "for entity [ " + entityName + " ]and userid : [ " + useridintest + " ]");
                                //      csAssert.assertTrue(false,"Result JAQL does not contain the ID " + sqlValue + "for the entity" + entityName);
                            }
                        }
                    }

                    sqlResultSet.clear();
                    jaqlResultSet.clear();
                    results.clear();
                    logger.info("Test completed for entity {} and user id {} ",entityName,useridintest);
                    logger.info("***************************************************************");
                }
            }
            csAssert.assertAll();
        } catch (Exception e) {
            logger.error("Exception while Validating RBAC for the entity " + entityName + e.getStackTrace());
        }

    }

    private String createPayloadforDatasourcesAPI(String dashboardId, String widgetId, String tableName, String column, String cube, String dashboardName) {

        //Creating jaql Json
        JSONObject jaqlJson = new JSONObject();
        jaqlJson.put("table", tableName);
        jaqlJson.put("column", column);
        String dim = "[" + tableName + "." + column + "]";
        jaqlJson.put("dim", dim);
        jaqlJson.put("datatype", "numeric");
        jaqlJson.put("title", column);

        logger.info("Jaql Json created is : " + jaqlJson);

        JSONObject formatJson = new JSONObject(format);
        JSONArray metadatJsonArray = new JSONArray();

        JSONObject metaJsonToAdd = new JSONObject();
        metaJsonToAdd.put("jaql", jaqlJson);
        metaJsonToAdd.put("format", formatJson);


        metadatJsonArray.put(0, metaJsonToAdd);

        //Creating datasource JSON Object
        JSONObject datasourceJson = new JSONObject();
        String fullname = server + "/" + cube;
        datasourceJson.put("fullname", fullname);
        datasourceJson.put("title", cube);
        String cudeid = cube.replace(" ", "IAAa");
        String id = "a" + server.toUpperCase() + "_a" + cudeid;
        datasourceJson.put("id", id);
        datasourceJson.put("address", server);
        datasourceJson.put("database", cudeid);
        datasourceJson.put("lastBuildTime", "");
        datasourceJson.put("dashboard", dashboardId);

        JSONObject payloadJson = new JSONObject();
        payloadJson.put("ungroup", true);
        payloadJson.put("count", 100000);
        payloadJson.put("offset", 0);
        payloadJson.put("isMaskedResult", true);
        payloadJson.put("format", "json");
        payloadJson.put("widget", widgetId + ";");
        String dashboard = dashboardId + ";" + dashboardName;
        payloadJson.put("dashboard", dashboard);
        payloadJson.put("queryGuid", "FAFE2-05E8-D311-383A-C732-46EB-F0B4-6FD4-F");

        payloadJson.put("metadata", metadatJsonArray);
        payloadJson.put("datasource", datasourceJson);

        return payloadJson.toString();
    }
}
