package com.sirionlabs.test.invoice.internationalization;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.internationalization.InternationalizationBase;

import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TestFieldRenaming extends InternationalizationBase {

    private final static Logger logger = LoggerFactory.getLogger(TestFieldRenaming.class);
    String configFilePath;
    String configFileName;

    @BeforeClass
    public void beforeClass(){
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("listFieldRenameFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("listFieldRenameFileName");

    }
    @DataProvider(name = "EntitiesToTest",parallel = false)
    public Object[][] dataProvider(){
        List<Object[]> allTestData = new ArrayList<>();

        String[] entityToTest = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"entity to test").split(",");
        for(String entity : entityToTest){
            allTestData.add(new Object[]{entity});
        }
        return allTestData.toArray(new Object[0][]);
    }

    /*
    SIR-15091
     */
    @Test(dataProvider = "EntitiesToTest")
    public void testFieldRenaming(String entityName) {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Verify List Column Renaming for " + entityName + " ");

        String[] fieldNamesToTest = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"field names to test").split(",");
        int languageId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"language id"));
        int groupId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,entityName,"group id"));
        int listId = ConfigureConstantFields.getListIdForEntity(entityName);

        String clientId = ConfigureEnvironment.getEnvironmentProperty("client_id");
        String fieldRenamingListingResponse = fieldRenamingObj.getFieldRenamingUpdateResponse(languageId, groupId);
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
        try {
            logger.info("Updating Labels of Fields in Listing.");
            String updatedLabel = "Updatedname" + RandomNumbers.getRandomNumberWithinRangeIndex(1,1000);

            String updatePayload = fieldRenamingListingResponse;

            for(String fieldName : fieldNamesToTest){

                String sql = "select name from client_label where label_id= " +
                        "(select id from system_label where group_id=" +
                        "(select id from system_label_group where parent_id=" + groupId +
                        " and name = 'Metadata') and name = '" + fieldName + "') " +
                        "and client_id=" + clientId + " and language_id=" + languageId + ";";

                List<List<String>> sqlOutput = postgreSQLJDBC.doSelect(sql);
                if(sqlOutput.size() != 0) {
                    String clientName = sqlOutput.get(0).get(0);

                    updatePayload = updatePayload
                            .replace("clientFieldName\":\"" + clientName + "", "clientFieldName\":\"" + updatedLabel);
                }else {
                    csAssert.assertTrue(false,"SQL Query to fetch Client Label fetched zero records for field name " + fieldName);
                }
            }

            fieldRenamingObj.hitFieldUpdateWithClientAdminLogin(updatePayload);

            HashMap<String,String> fieldNamesOnListing = ListDataHelper.getFieldNamesOnListing(listId, csAssert);

            for(String fieldName : fieldNamesToTest){

                if(fieldNamesOnListing.containsKey(fieldName)){
                    String actualFieldName = fieldNamesOnListing.get(fieldName);
                    csAssert.assertEquals(actualFieldName,updatedLabel,"Expected and Actual Field Name not matched for field " + fieldName + " Expected " + updatedLabel + " Actual " + actualFieldName);
                }else if(fieldNamesOnListing.containsKey(fieldName.toUpperCase())){
                    String actualFieldName = fieldNamesOnListing.get(fieldName.toUpperCase());
                    csAssert.assertEquals(actualFieldName,updatedLabel,"Expected and Actual Field Name not matched for field " + fieldName + " Expected " + updatedLabel + " Actual " + actualFieldName);
                }else if(fieldNamesOnListing.containsKey(fieldName.toLowerCase())){
                    String actualFieldName = fieldNamesOnListing.get(fieldName.toLowerCase());
                    csAssert.assertEquals(actualFieldName,updatedLabel,"Expected and Actual Field Name not matched for field " + fieldName + " Expected " + updatedLabel + " Actual " + actualFieldName);
                }else {
                    csAssert.assertTrue(false,"Field Name " + fieldName + " not found in Listing Response");
                }
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Field Renaming " + e.getMessage());
        } finally {
            logger.info("Reverting Labels in Listing");
            fieldRenamingObj.hitFieldUpdateWithClientAdminLogin(fieldRenamingListingResponse);
            postgreSQLJDBC.closeConnection();

        }

        csAssert.assertAll();
    }

    private void matchLabels(JSONObject jsonObj, String fieldId, String expectedLabel, String fieldName, CustomAssert csAssert) {
        if (jsonObj.has(fieldId)) {
            String actualLabel = jsonObj.getJSONObject(fieldId).getString("name");
            boolean matchLabels = StringUtils.matchRussianCharacters(expectedLabel, actualLabel);

            csAssert.assertTrue(matchLabels, "Expected " + fieldName + " Label: " + expectedLabel + " and Actual Today Label: " + actualLabel);
        } else {
            csAssert.assertFalse(true, "MessagesList API Response doesn't contain Object " + fieldId);
        }
    }
}
