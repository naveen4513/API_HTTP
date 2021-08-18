package com.sirionlabs.test.internationalization;

import com.sirionlabs.api.sisenseBI.IntlMetadata;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.test.sisenseBI.TestAnalyticsServiceLogin;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

import static com.sirionlabs.test.internationalization.TestDisputeInternationalization.expectedPostFix;

public class FieldLabelSisenseDashboard {

    private final static Logger logger = LoggerFactory.getLogger(FieldLabelSisenseDashboard.class);
    private Integer portNumber=Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("analyticsserviceportnumber"));
    private String configFilePath= ConfigureConstantFields.getConstantFieldsProperty("FieldRenamingConfigFilePath");
    private String configFileName=ConfigureConstantFields.getConstantFieldsProperty("FieldRenamingConfigFileName");
    private String serverName = ConfigureEnvironment.getEnvironmentProperty("serverclient") + "://" + ConfigureEnvironment.getEnvironmentProperty("serverhost") + ":" + ConfigureEnvironment.getEnvironmentProperty("serverport");


    private void validatemetadataResponse(String intlmetadataResponse, String[] linkedTables, String[] columnstoskip, String[] tablestoskip, String entityName,CustomAssert csAssert){

        logger.info("Inside validatemetadataResponse Method");
        String fieldName = "";
        String fieldNameValue;
        String fieldNameMetaDataValue;

        try {
            JSONObject metaDataResponseJson = new JSONObject(intlmetadataResponse);
            JSONObject columnsJson;
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

            for (Map.Entry<String, String> fieldnameKeyValue : fieldsmapMetaData.entrySet()) {

                fieldName = fieldnameKeyValue.getKey();
                fieldNameValue = fieldnameKeyValue.getValue();
                if((columnsToSkip.contains(fieldName)) || (tablesToSkip.contains(fieldName))){
                    continue;
                }
                if (fieldsmapMetaData.containsKey(fieldName)) {
                    fieldNameMetaDataValue = fieldsmapMetaData.get(fieldName);
                    if (fieldNameMetaDataValue.toLowerCase().contains(expectedPostFix.toLowerCase())) {
                        csAssert.assertTrue(false, "Field Label: [" + fieldNameMetaDataValue + "] contain: [" + expectedPostFix + "] under Sisense Dashboard of " + entityName);
                    } else {
                        csAssert.assertTrue(true, "Field Label: [" + fieldNameMetaDataValue + "] does not contain: [" + expectedPostFix + "] under Sisense Dashboard of " + entityName);
                    }
                } else {
                    logger.error("Meta Data Response doesn't contain renamed field " + fieldName);
                    csAssert.assertTrue(false,"Meta Data Response doesn't contain renamed field " + fieldName);
                }
            }
        }catch (Exception e){
            csAssert.assertTrue(false,"Meta Data Response doesn't contain renamed field " + fieldName);
            logger.error("Exception while validating metadata response " + e.getStackTrace());
        }
    }

    public void testfieldRenaming (String entityName, CustomAssert csAssert) throws IOException {
     if(!entityName.toLowerCase().contains("vendors") && !entityName.toLowerCase().contains("child service levels") && !entityName.toLowerCase().contains("governance body")
     && !entityName.toLowerCase().contains("governance body meetings") && !entityName.toLowerCase().contains("purchase orders") && !entityName.toLowerCase().contains("service data")
     && !entityName.toLowerCase().contains("purchase orders") && !entityName.toLowerCase().contains("clauses") && !entityName.toLowerCase().contains("contract templates")
     && !entityName.toLowerCase().contains("consumptions") && !entityName.toLowerCase().contains("contract template structure")
     && !entityName.toLowerCase().contains("creditearnbacks") && !entityName.toLowerCase().contains("applicationgroups")
     && !entityName.toLowerCase().contains("externalcontractingparty"))

    {

        TestAnalyticsServiceLogin analyticsServiceLoginObj = new TestAnalyticsServiceLogin();
        analyticsServiceLoginObj.testAnalyticsServiceLogin();

        logger.info("Getting meta data response");
        IntlMetadata intlMetadata = new IntlMetadata();
        intlMetadata.hitIntlMetaData(serverName, "1", portNumber);
        String intlmetadataResponse = intlMetadata.getintlMetaDataResponseStr();

        String[] linkedTables = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "linkedtables", entityName).split(",");
        String[] columnsToSkip = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fieldnamestoskip", entityName).split(",");
        String[] tablesToSkip = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "tablestoskip", entityName).split(",");

        JSONObject intlmetadataResponseJson = new JSONObject(intlmetadataResponse);
        if (intlmetadataResponseJson.has("error")) {
            logger.error("Unable to fetch metadata response");
        } else {
            logger.info("Meta Data response fetch successfully");
            logger.info("Meta Data response " + intlmetadataResponseJson);
            validatemetadataResponse(intlmetadataResponse, linkedTables, columnsToSkip, tablesToSkip, entityName, csAssert);
            }
        }
    }
}
