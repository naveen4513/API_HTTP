package com.sirionlabs.test;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.DownloadTemplates.BulkTemplate;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;


import java.io.File;

@Listeners(value = MyTestListenerAdapter.class)
public class TestConsumptionDownload {

    private final static Logger logger = LoggerFactory.getLogger(TestConsumptionDownload.class);
    private static String configFilePath;
    private static String configFileName;
    private XLSUtils xlutil;
    private String templateDownloadPath;
    private String templateDownloadName;
    private int templateId;
    private int entityTypeId;
    private String entityIds;
    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ConsumptionConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("ConsumptionConfigFileName");

        templateDownloadPath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "consumptiontemplatedownload",
                "templatePath");
        templateDownloadName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "consumptiontemplatedownload",
                "templateName");
        templateId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "consumptiontemplatedownload",
                "templateId"));
        entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "consumptiontemplatedownload",
                "entityTypeId"));
        entityIds = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "consumptiontemplatedownload",
                "entityIds");

    }

    @Test(enabled = true)
    public void testBulkDownloadConsumptionExcel(){
        CustomAssert csAssert = new CustomAssert();


        try {
            logger.info("Validating Bulk Consumption Download Template Excel");

            Boolean templateDownloaded = BulkTemplate.downloadBulkUpdateTemplate(templateDownloadPath, templateDownloadName, templateId, entityTypeId, entityIds);


            if (!templateDownloaded) {
                logger.error("Bulk Update Template Download failed using Template Id {}, EntityTypeId {} and EntityIds {}.", templateId, entityTypeId,
                        entityIds);
                csAssert.assertTrue(templateDownloaded, "Bulk Update Template Download failed using Template Id " + templateId + ", EntityTypeId " +
                        entityTypeId + " and EntityIds " + entityIds);
            } else {

                xlutil = new XLSUtils(templateDownloadPath,templateDownloadName);

//                if(validateInstructionSheet(xlutil,"Instructions",csAssert)){
//                    logger.info("Valid instructions fetched from excel");
//                    csAssert.assertTrue(true, "Valid instructions fetched from excel");
//                }else {
//                    csAssert.assertTrue(false, "Invalid instructions fetched from excel");
//                    logger.error("Invalid instructions fetched from excel");
//                }

                //Validating Information Sheet for the bulk downloaded sheet
                if(validateInformationSheet(xlutil,"Information",csAssert)){
                    logger.info("Information Tab validated successfully");
                    csAssert.assertTrue(true, "Information Tab validated successfully");
                }else {
                    csAssert.assertTrue(false, "Information Tab validated unsuccessfully");
                    logger.error("Information Tab validated unsuccessfully");
                }

                logger.info("Validating the Data tab on the downloaded excel");
                if(checkDownloadedExcelDataTab(xlutil,"Consumption",csAssert)){
                    csAssert.assertTrue(true, "Consumption sheet validated successfully");
                    logger.info("Consumption sheet validated successfully");

                }else{
                    csAssert.assertTrue(false, "Consumption sheet validated unsuccessfully");
                    logger.error("Consumption sheet validated unsuccessfully");
                }

                File templateFile = new File(templateDownloadPath + "/" + templateDownloadName);
                logger.info("Deleting downloaded template file for Bulk Update. File location: [{}]", templateDownloadPath + "/" + templateDownloadName);
                Boolean templateDeleted = templateFile.delete();
                if (templateDeleted) {
                    logger.info("Bulk Update Template file at Location: [{}] deleted successfully.", templateDownloadPath + "/" + templateDownloadName);
                } else {
                    logger.debug("Couldn't delete Bulk Update Template file at Location: [{}]", templateDownloadPath + "/" + templateDownloadName);
                }
            }
        } catch (Exception e) {
            logger.error("Exception while validating Bulk Update Download Template. {}", e.getMessage());
            csAssert.assertTrue(false, "Exception while validating Bulk Update Download Template. " + e.getMessage());
        }
        csAssert.assertAll();
    }

    @Test(enabled = true)
    public void checkConsumptionVolumeVariance(){

        CustomAssert csAssert = new CustomAssert();
        logger.info("Validating Consumption volume and variance");
        int i = 0;
        int j;
        int row = 0;
        Boolean flag = false;
        try{

            String payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":20,\"orderByColumnName\":\"enddate\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{}}}";

            int listId = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "volumevariance",
                    "listId"));
            int tablistid = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "volumevariance",
                    "tablistid"));
            int id = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "volumevariance",
                    "id"));
            String listdataresponseServiceData = getListTabResponse(listId,tablistid,id,payload);
            JSONObject servicedataObj = new JSONObject(listdataresponseServiceData);
            JSONArray listdataResponseArray = servicedataObj.getJSONArray("data");

            String data[][] = new String[listdataResponseArray.length()][];

            while (i < listdataResponseArray.length()){

                data[i] = new String[3];
                JSONObject listdataresponseObj = listdataResponseArray.getJSONObject(i);
                JSONArray listdatajsonarray = JSONUtility.convertJsonOnjectToJsonArray(listdataresponseObj);
                j = 0;
                //fetching the list data response of service data
                while (j < listdatajsonarray.length()) {
                    String columnName = listdatajsonarray.getJSONObject(j).get("columnName").toString();
                    if(columnName.equals("startdate")){
                        data[row][0] = listdatajsonarray.getJSONObject(j).get("value").toString();
                    }else if(columnName.equals("enddate")){
                        data[row][1] = listdatajsonarray.getJSONObject(j).get("value").toString();

                    }else if(columnName.equals("base_volume")){
                        data[row][2] = listdatajsonarray.getJSONObject(j).get("value").toString();
                    }

                    j++;
                }
                row = row + 1;
                j =0;
                i++;
            }

            i =0;
            Float volumeServiceDataExpected = Float.parseFloat(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "volumevariance",
                    "volumeListServiceData"));
            while(i < data.length){
                Float volumeServiceDataActual = Float.parseFloat(data[i][2]);
                if(volumeServiceDataExpected == volumeServiceDataActual);
                {
                    flag = true;
                }
                if(flag = true){
                    break;
                }
                i++;
            }
            if(flag == true) {
                logger.info("Valid volume data field value on service data");
            }else{
                logger.error("Invalid volume data field value on service data");
            }
            i = 0;
            int entityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "volumevariance",
                    "entityTypeId"));
            int dbId = Integer.parseInt(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "volumevariance",
                    "dbId"));

//            String showPageResponseStr = getShowResponse(entityTypeId, dbId);
            //Checking showpage response of consumption entity
            //change
            String showPageResponseStr = getShowResponse(entityTypeId,dbId);

            JSONObject volumedata = new JSONObject(showPageResponseStr).getJSONObject("body").getJSONObject("data").getJSONObject("baseVolume");
            JSONObject variancedata = new JSONObject(showPageResponseStr).getJSONObject("body").getJSONObject("data").getJSONObject("variance");
            JSONObject finalConsumptiondata = new JSONObject(showPageResponseStr).getJSONObject("body").getJSONObject("data").getJSONObject("finalConsumption");
            JSONObject finalVariancePercentage = new JSONObject(showPageResponseStr).getJSONObject("body").getJSONObject("data").getJSONObject("variancePercentage");

            String voldata = volumedata.get("values").toString();
            float consumptionvolumeActual = Float.parseFloat(voldata);
            float consumptionvarianceActual = Float.parseFloat((variancedata.get("values")).toString());
            String finalVariancePercentageActual = finalVariancePercentage.get("values").toString();
            finalVariancePercentageActual = finalVariancePercentageActual.substring(0,3);

            float finalConsumptionActual = Float.parseFloat(((Object)finalConsumptiondata.get("values")).toString());

            float varianceExpected  = finalConsumptionActual - consumptionvolumeActual;
            String variancePercentageExpected = String.valueOf(finalVariancePercentageActual);
            variancePercentageExpected = variancePercentageExpected.substring(0,3);

            if(finalVariancePercentageActual.equals(variancePercentageExpected)){
                logger.info("Variance percentage calculated successfully for Consumptions");
                csAssert.assertTrue(true,"Expected and Actual Variance percentage are same on show page");
            }
            else{
                logger.error("Variance percentage calculated unsuccessfully for Consumptions");
                csAssert.assertTrue(false,"Expected and Actual Variance percentage are different on show page");
            }
            if(varianceExpected == consumptionvarianceActual){
                logger.info("Variance calculated successfully for Consumptions");
                csAssert.assertTrue(true,"Expected and Actual Variance are same on show page");
            }
            else{
                logger.error("Variance calculated unsuccessfully for Consumptions");
                csAssert.assertTrue(false,"Expected and Actual Variance are different on show page");
            }

            logger.info("Validating consumption values with Service Data");

            if (consumptionvolumeActual == Float.parseFloat(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "volumevariance",
                    "volumeConsumptionShowPage"))){
                logger.info("Valid consumption volume on show page");
                csAssert.assertTrue(true,"Expected and Actual consumption volume are same on show page");
            }
            else{
                logger.error("Incorrect consumption volume on show page");
                csAssert.assertTrue(false,"Expected and Actual consumption volume are different on show page");
            }
        }catch (Exception e){
            logger.error("Exception while Validating Service Data and Consumptions Volume and Variance", e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Validating Service and Consumptions Data volume and variance" + e.getMessage());
        }
    }

    public String getShowResponse(int entityTypeId, int dbId) {
        Show showObj = new Show();
        showObj.hitShow(entityTypeId, dbId);

        return showObj.getShowJsonStr();

    }

    public String getListTabResponse(int listId, int tablistId,int id,String payload) {
        ListRendererListData list = new ListRendererListData();
        list.hitListRendererTabListData(listId,tablistId,id,payload);

        return list.getListDataJsonStr();

    }

    public Boolean checkDownloadedExcelDataTab(XLSUtils xlutil,String sheetName,CustomAssert csAssert) {

        logger.info("Validating Consumption Sheet of the downloaded excel");

        int excelColNum = 0;
        Boolean consumptionSheetValidationStatus = true;

        try {
            String Headers = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "consumptionexceldownloadDataTab",
                    "headers");
            String HeadersArr[] = Headers.split(",");

            String DataString = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "consumptionexceldownloadDataTab",
                    "ActualData");
            String DataStringArr[] = DataString.split(",");

            while(excelColNum < HeadersArr.length){

                if(xlutil.getCellData(sheetName,excelColNum,0).trim().equals(HeadersArr[excelColNum])){

                    logger.info("Valid header in downloaded excel for the header " + xlutil.getCellData("Consumption",excelColNum,0).trim());
                }else {
                    logger.error("Error validating the headers in downloaded excel for the header " + xlutil.getCellData("Consumption",excelColNum,6).trim());
                    consumptionSheetValidationStatus = false;
                }

                if(xlutil.getCellData(sheetName,excelColNum,6).trim().equals(DataStringArr[excelColNum])){
                    csAssert.assertEquals(xlutil.getCellData("Consumption",excelColNum,6).trim(),DataStringArr[excelColNum],"Valid data in current column of data tab of downloaded consumption excel tab");
                    logger.info("Valid data in downloaded excel Data =  " + xlutil.getCellData("Consumption",excelColNum,0).trim());
                }else {
                    csAssert.assertEquals(xlutil.getCellData("Consumption",excelColNum,6).trim(),DataStringArr[excelColNum],"Expected and Actual data mismatch in column [ "+ excelColNum + 1 +" ] of data tab of downloaded consumption excel tab");
                    logger.error("Error validating the data in downloaded excel for the Data =   " + xlutil.getCellData("Consumption",excelColNum,6).trim());
                    consumptionSheetValidationStatus = false;
                }

                excelColNum++;
            }
        }
        catch (Exception e) {
            logger.error("Exception while validating Data Tab of consumption Download Template. {}", e.getMessage());
            csAssert.assertTrue(false, "Exception while validating Consumption Tab of consumption Download Template" + e.getMessage());
            consumptionSheetValidationStatus = false;
        }
        return consumptionSheetValidationStatus;
    }

    private Boolean validateInstructionSheet(XLSUtils xlutil,String sheetName,CustomAssert csAssert){

        String templatetype = xlutil.getCellData(sheetName,5,0);
        Boolean instructionTabValidationStatus = true;
        try {
            String actualtemplateType = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "consumptionexceldownload",
                    "templatetypeText").trim();
            logger.info("Validating template type for downloaded consumption excel");
            if (templatetype.equals(actualtemplateType)) {

                logger.info("Valid templatetypeText fetched from excel");
                csAssert.assertTrue(true, "Valid templatetypeText fetched from excel");
            } else {
                logger.error("Invalid templatetypeText fetched from excel");
                csAssert.assertTrue(false, "Invalid templatetypeText fetched from excel");
            }

            logger.info("Validating the Instructions tab on the downloaded excel");

            String templatversion = xlutil.getCellData(sheetName, 5, 1);
            String actualtemplateVersion = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "consumptionexceldownload",
                    "templateVersionText").trim();
            if (templatversion.equals(actualtemplateVersion)) {
                logger.info("Valid templateVersionText fetched from excel");
                csAssert.assertTrue(true, "Valid templateVersionText fetched from excel");
            } else {
                logger.error("Invalid templateVersionText fetched from excel");
                csAssert.assertTrue(false, "Invalid templateVersionText fetched from excel");
            }

            String instruction = xlutil.getCellData(sheetName, 5, 2);
            String actualInstruction = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "consumptionexceldownload",
                    "instructionText").trim();

            if (instruction.equals(actualInstruction)) {
                logger.info("Valid instructionText fetched from excel");
                csAssert.assertTrue(true, "Valid instructionText fetched from excel");
            } else {
                logger.error("Invalid instructionText fetched from excel");
                csAssert.assertTrue(false, "Invalid instructionText fetched from excel");

            }
            String templateTypeValue = xlutil.getCellData(sheetName, 6, 0);

            if (templateTypeValue.equals(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "consumptionexceldownload",
                    "templatetypeValue").trim())) {

                logger.info("Valid templatetypeValue fetched from excel");
                csAssert.assertTrue(true, "Valid templatetypeValue fetched from excel");

            } else {
                logger.error("Invalid templatetypeValue fetched from excel");
                csAssert.assertTrue(false, "Invalid templatetypeValue fetched from excel");
            }
            String templateVersionValue = xlutil.getCellData(sheetName, 6, 1);
            if (templateVersionValue.equals(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "consumptionexceldownload",
                    "templateVersionValue").trim())) {
                logger.info("Valid templateVersionValue fetched from excel");
                csAssert.assertTrue(true, "Valid templateVersionValue fetched from excel");
            } else {
                logger.error("Invalid templateVersionValue fetched from excel");
                csAssert.assertTrue(false, "Invalid templateVersionValue fetched from excel");
            }
            String instructionfromexceldowloaded = xlutil.getCellData(sheetName, 6, 2);

            String instructionstext = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "consumptionexceldownload",
                    "instructionstext");

            String instructionstextarr[] = instructionstext.toString().split("/");
            String instructionfromexceldowloadedarr[] = instructionfromexceldowloaded.split("\n");

            int i = 0;
            while (i < instructionstextarr.length) {
                if (!(instructionfromexceldowloadedarr[i].equals(instructionstextarr[i]))) {

                    instructionTabValidationStatus = false;
                }
                i++;
            }
        }catch (Exception e){
            logger.error("Exception while validating Instructions Tab for the downloaded excel ");
            instructionTabValidationStatus = false;
        }
        return instructionTabValidationStatus;
    }

    private Boolean validateInformationSheet(XLSUtils xlutil,String sheetName,CustomAssert csAssert){

        logger.info("Validating the Information tab on the downloaded excel");
        Boolean informationSheetValidationStatus = true;
        try {
            String informationTabFirstColFirstRow = xlutil.getCellData(sheetName, 0, 0);

            if (informationTabFirstColFirstRow.equals(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "consumptionexceldownloadInformationTab",
                    "FirstColFirstRowText").trim())) {

                csAssert.assertTrue(true, "Valid data on Information Tab First Column fetched from excel");
                logger.info("Valid data on Information Tab First Column fetched from excel");
            } else {
                csAssert.assertTrue(false, "Invalid data on Information Tab First Column fetched from excel");
                logger.error("Invalid data on Information Tab First Column fetched from excel");
                informationSheetValidationStatus =false;
            }
            String informationTabFirstColThirdRow = xlutil.getCellData(sheetName, 0, 2);

            if (informationTabFirstColThirdRow.equals(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "consumptionexceldownloadInformationTab",
                    "FirstColThirdRowText").trim())) {
                csAssert.assertTrue(true, "Valid data on Information Tab Second Column fetched from excel");
                logger.info("Valid data on Information Tab Second Column fetched from excel");
            } else {
                csAssert.assertTrue(false, "Invalid data on Information Tab Second Column fetched from excel");
                logger.error("Invalid data on Information Tab Second Column fetched from excel");
                informationSheetValidationStatus =false;
            }

            String informationTabFirstColFourthRow = xlutil.getCellData(sheetName, 0, 3);
            String user = ConfigureEnvironment.getEnvironmentProperty("j_username");
            user = user.replace("_", " ");
            if (informationTabFirstColFourthRow.trim().equalsIgnoreCase(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "consumptionexceldownloadInformationTab",
                    "FirstColFourthRowText").trim())) {
                csAssert.assertTrue(true, "Valid data on Information Tab First Column and Fourth Row fetched from excel");
                logger.info("Valid data on Information Tab First Column and Fourth Row fetched from excel");
            } else {
                csAssert.assertTrue(false, "Invalid data on Information Tab First Column and Fourth Row fetched from excel");
                logger.error("Invalid data on Information Tab First Column and Fourth Row fetched from excel");
                informationSheetValidationStatus =false;
            }

            String informationTabSecColFirstRow = xlutil.getCellData(sheetName, 1, 0);

            if (informationTabSecColFirstRow.equalsIgnoreCase(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "consumptionexceldownloadInformationTab",
                    "SecondColFirstRowText").trim())) {
                csAssert.assertTrue(true, "Valid data on Information Tab Second Column and First Row fetched from excel");
                logger.info("Valid data on Information Tab Second Column and First Row fetched from excel");
            } else {
                csAssert.assertTrue(false, "Invalid data on Information Tab Second Column and First Row fetched from excel");
                logger.error("Invalid data on Information Tab Second Column and First Row fetched from excel");
                informationSheetValidationStatus =false;
            }

            String informationTabSecColThirdRow = xlutil.getCellData(sheetName, 1, 2);

            if (informationTabSecColThirdRow.equalsIgnoreCase(user)) {
                csAssert.assertTrue(true, "Valid data on Information Tab Second Column and Third Row fetched from excel");
                logger.info("Valid data on Information Tab Second Column and Third Row fetched from excel");
            } else {
                csAssert.assertTrue(false, "Invalid data on Information Tab Second Column and Third Row fetched from excel");
                logger.error("Invalid data on Information Tab Second Column and Third Row fetched from excel");
                informationSheetValidationStatus =false;
            }

            String informationTabSecColFourthRow = xlutil.getCellData(sheetName, 1, 3);
            informationTabSecColFourthRow = informationTabSecColFourthRow.substring(0, 11);
            String currentDate = DateUtils.getCurrentDateInDDMMYYYY();
            currentDate = currentDate.replace("/", "-");
            String previousDate = DateUtils.getPreviousDateInDDMMYYYY(currentDate);
            previousDate = DateUtils.convertDateToAnyFormat(previousDate, "dd/MMM/yyyy");
            String nextDate = DateUtils.getNextDateInDDMMYYYY(currentDate);
            nextDate = DateUtils.convertDateToAnyFormat(nextDate, "dd/MMM/yyyy");
            currentDate = DateUtils.getCurrentDateInDDMMMYYYY();


            if (informationTabSecColFourthRow.equals(currentDate) || informationTabSecColFourthRow.equals(previousDate) || informationTabSecColFourthRow.equals(nextDate)) {

                csAssert.assertTrue(true, "Valid data on Information Tab Second Column and Fourth Row fetched from excel");
                logger.info("Valid data on Information Tab Second Column and Fourth Row fetched from excel");
            } else {
                csAssert.assertTrue(false, "Invalid data on Information Tab Second Column and Fourth Row fetched from excel");
                logger.error("Invalid data on Information Tab Second Column and Fourth Row fetched from excel");
                informationSheetValidationStatus =false;
            }
        }catch (Exception e){
            logger.error("Exception while validating Information Sheet " + e.getStackTrace());
            csAssert.assertTrue(false,"Exception while validating Information Sheet " + e.getStackTrace());
            informationSheetValidationStatus =false;
        }
        return informationSheetValidationStatus;
    }
}
