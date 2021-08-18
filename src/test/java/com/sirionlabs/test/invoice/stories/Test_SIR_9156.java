package com.sirionlabs.test.invoice.stories;

import com.sirionlabs.api.bulkupload.Download;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.test.bulkCreate.BulkCreateAllEntities;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//Duplicate data in Bulk Create & Bulk Update feature
public class Test_SIR_9156 {

    private final static Logger logger = LoggerFactory.getLogger(Test_SIR_9156.class);

    private String configFilePath;
    private String configFileName;
    private String clientId;

    @BeforeClass
    public void beforeClass(){

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("Test_SIR9156_FilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("Test_SIR9156_FileName");

        clientId = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"client id");
    }

    @DataProvider(name = "entitiesToTestForBulkCreate")
    public Object[][] entitiesToTestForBulkCreate(){

        String[] entityNames = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk create scenarios").split(",");
        String parentEntityName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"parent entity");
        List<Object[]> allTestData = new ArrayList<>();

        for(String entityName : entityNames){

            allTestData.add(new Object[]{entityName,parentEntityName,"true"});
            allTestData.add(new Object[]{entityName,parentEntityName,"false"});
        }
        return allTestData.toArray(new Object[0][]);

    }

    @DataProvider(name = "entitiesToTestForBulkUpdate")
    public Object[][] entitiesToTestForBulkUpdate(){

        String[] entityNames = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk update scenarios").split(",");

        List<Object[]> allTestData = new ArrayList<>();

        for(String entityName : entityNames){

            allTestData.add(new Object[]{entityName,"true"});
            allTestData.add(new Object[]{entityName,"false"});
        }
        return allTestData.toArray(new Object[0][]);

    }

    @Test(dataProvider = "entitiesToTestForBulkCreate",enabled = true)
    public void TestBulkCreateScenarios(String entityName,String parentEntityName,String multiLingual){

        CustomAssert customAssert = new CustomAssert();

        try{
            updateMultiLingualFlag(multiLingual);
            int templateId =  Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk create template ids",entityName));
            int parentEntityTypeId = ConfigureConstantFields.getEntityIdByName(parentEntityName);

            int parentEntityId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk create template ids",parentEntityName + " id for bulk create"));

            Download download = new Download();

            String outputFilePath = configFilePath;
            String outputFileName = "BulkCreateTemplate_" + entityName + ".xlsm";

            Boolean downloadStatus = download.hitDownload(outputFilePath,outputFileName,templateId,parentEntityTypeId,parentEntityId);

            if(!downloadStatus){
                customAssert.assertTrue(false,"Bulk Create Template Download unsuccessful");
            }else {
                String sheetName = "Master Data";
                int numOfRows = XLSUtils.getNoOfRows(outputFilePath,outputFileName,sheetName).intValue();

                String columnIdToCheck = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk create column ids","services " + entityName + " column id");

                if(columnIdToCheck.equals("-1")){
                    throw new SkipException("Column Id not defined in Configuration File");

                }
                List<String> columnIds = XLSUtils.getExcelDataOfOneRow(outputFilePath,outputFileName,sheetName,2);

                int columnNumberToCheck = -1;

                for(int i=0; i<columnIds.size();i++){

                    if(columnIds.get(i).equals(columnIdToCheck)){
                        columnNumberToCheck = i;
                    }
                }
                if(columnNumberToCheck != 1) {
                    List<String> columnData = XLSUtils.getOneColumnDataFromMultipleRowsIncludingEmptyRows(outputFilePath, outputFileName, sheetName,columnNumberToCheck,4,numOfRows );

                    String childDuplicateFieldsToCheck = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"child duplicate fields to check");

                    if(childDuplicateFieldsToCheck != null) {

                        List<String> childDuplicateFieldsToCheckList = Arrays.asList(childDuplicateFieldsToCheck.split(","));
                        List<String> childDuplicateFieldsToRemoveList = new ArrayList<>();
                        int j =0;
                        for (int i = 0; i < columnData.size(); i++) {

                            if( j< childDuplicateFieldsToCheckList.size()) {
                                if (childDuplicateFieldsToCheckList.get(j).contains(columnData.get(i))) {
                                    childDuplicateFieldsToRemoveList.add(columnData.get(i));
                                    j++;
                                }
                            }
                        }

                        if(childDuplicateFieldsToRemoveList.size() != childDuplicateFieldsToCheckList.size()){

                            for(int i =0;i<childDuplicateFieldsToCheckList.size();i++){
                                if(!childDuplicateFieldsToRemoveList.contains(childDuplicateFieldsToCheckList.get(i))){
                                    customAssert.assertTrue(false,childDuplicateFieldsToCheckList.get(i) + " not found int the Bulk Create Down Template Excel");
                                }
                            }

                        }
                    }
                }

            }

        }catch (SkipException ske){
            throw new SkipException(ske.getMessage());
        } catch (Exception e){
            logger.error("Exception while validating the scenario " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while validating the scenario " + e.getStackTrace());
        }

        customAssert.assertAll();

    }

    @Test(dataProvider = "entitiesToTestForBulkUpdate")
    public void TestBulkUpdateScenarios(String entityName,String multiLingual){

        CustomAssert customAssert = new CustomAssert();

        try{
            updateMultiLingualFlag(multiLingual);
            int templateId =  Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk update template ids",entityName));

            Download download = new Download();

            String outputFilePath = configFilePath;
            String outputFileName = "BulkUpdateTemplate_" + entityName + ".xlsm";

            int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
            String entityIds = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk update records",entityName);

            Boolean downloadStatus = download.hitDownload(outputFilePath,outputFileName,templateId,entityTypeId,entityIds);

            if(!downloadStatus){
                customAssert.assertTrue(false,"Bulk Update Template Download unsuccessful");
            }else {
                String sheetName = "Master Data";
                int numOfRows = XLSUtils.getNoOfRows(outputFilePath,outputFileName,sheetName).intValue();

                String columnIdToCheck = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"bulk update column ids","services " + entityName + " column id");

                if(columnIdToCheck.equals("-1")){
                    throw new SkipException("Column Id not defined in Configuration File");

                }
                List<String> columnIds = XLSUtils.getExcelDataOfOneRow(outputFilePath,outputFileName,sheetName,2);

                int columnNumberToCheck = -1;

                for(int i=0; i<columnIds.size();i++){

                    if(columnIds.get(i).equals(columnIdToCheck)){
                        columnNumberToCheck = i;
                    }
                }
                if(columnNumberToCheck != 1) {
                    List<String> columnData = XLSUtils.getOneColumnDataFromMultipleRowsIncludingEmptyRows(outputFilePath, outputFileName, sheetName,columnNumberToCheck,4,numOfRows );

                    String childDuplicateFieldsToCheck = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"child duplicate fields to check");

                    if(childDuplicateFieldsToCheck != null) {

                        List<String> childDuplicateFieldsToCheckList = Arrays.asList(childDuplicateFieldsToCheck.split(","));
                        List<String> childDuplicateFieldsToRemoveList = new ArrayList<>();
                        int j = 0;
                        for (int i = 0; i < columnData.size(); i++) {

                            if( j< childDuplicateFieldsToCheckList.size()) {
                                if (childDuplicateFieldsToCheckList.get(j).contains(columnData.get(i))) {
                                    childDuplicateFieldsToRemoveList.add(columnData.get(i));
                                    j++;
                                }
                            }
                        }


                        for (int i = 0; i < childDuplicateFieldsToCheckList.size(); i++) {
                            if (!childDuplicateFieldsToRemoveList.contains(childDuplicateFieldsToCheckList.get(i))) {
                                customAssert.assertTrue(false, childDuplicateFieldsToCheckList.get(i) + " not found int the Bulk Update Download Template Excel");
                            }
                        }


                    }
                }

            }

        }catch (SkipException ske){
            throw new SkipException(ske.getMessage());
        } catch (Exception e){
            logger.error("Exception while validating the scenario " + e.getStackTrace());
            customAssert.assertTrue(false,"Exception while validating the scenario " + e.getStackTrace());
        }

        customAssert.assertAll();

    }

    private void updateMultiLingualFlag(String multiLingual){

        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();

        postgreSQLJDBC.updateDBEntry("update client SET multilanguage_supported = '" + multiLingual + "' where id = " + clientId + "");

        postgreSQLJDBC.closeConnection();

    }


}
