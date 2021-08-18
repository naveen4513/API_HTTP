package com.sirionlabs.test.reportRenderer;

import com.sirionlabs.api.listRenderer.DownloadListWithData;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererFilterData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.reportRenderer.DownloadReportWithData;
import com.sirionlabs.api.reportRenderer.ReportRendererDefaultUserListMetaData;
import com.sirionlabs.api.reportRenderer.ReportRendererFilterData;
import com.sirionlabs.api.reportRenderer.ReportRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.*;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import java.text.SimpleDateFormat;
import java.util.*;


public class TestReportDataDownloadValidations extends APIUtils {

    private final static Logger logger = LoggerFactory.getLogger(TestReportDataDownloadValidations.class);
    static String csrfToken;
    static String outputFilePath;
    static String outputFileFormatForDownloadListWithData;
    static String entityIdMappingFileName;
    static String entityIdConfigFilePath;
    static String orderByColumnName = "id";
    static String orderDirection = "desc";
    static String configFilePath;
    static String configFileName;

    static List<String> allEntitySection;

    static Integer size = 10;
    static int offset = 0;

    String entitySectionSplitter = ",";
    Boolean testForAllEntities = false;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ReportDataTestConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("ReportDataTestConfigFileName");
        csrfToken = ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN");
        outputFilePath = ConfigureConstantFields.getConstantFieldsProperty("DownloadListDataFilePath");
        outputFileFormatForDownloadListWithData = ConfigureConstantFields.getConstantFieldsProperty("DownloadListDataFileFormat");

        entityIdConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath");
        entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");

        // for getting all section
        if (!ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "testforallentities").trim().equalsIgnoreCase(""))
            testForAllEntities = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "testforallentities"));


        if (!testForAllEntities) {
            allEntitySection = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entitytotest").split(entitySectionSplitter));
        } else {
            allEntitySection = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "allentitytotest").split(entitySectionSplitter));
        }
        // getting all section Ends Here


//        if (!ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
//                "maxrandomoptions").trim().equalsIgnoreCase(""))
//            maxRandomOptions = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
//                    "maxrandomoptions").trim());

        if (!ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "size").trim().equalsIgnoreCase(""))
            size = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "size"));

        if (!ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "offset").trim().equalsIgnoreCase(""))
            offset = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "offset"));

        if (!ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "orderbycolumnname").trim().equalsIgnoreCase(""))
            orderByColumnName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "orderByColumnName");

        if (!ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "orderdirection").trim().equalsIgnoreCase(""))
            orderDirection = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "orderDirection");
    }

    @Test(dataProvider = "getReportIds",enabled = true)
    public void testreportlistdownloadfiltersheet(String entityName,Integer reportid,Integer entityTypeId) {

        CustomAssert csAssert = new CustomAssert();
        Boolean downloadstatus;
        try {

            downloadListDataForAllColumnsfilter(reportid,entityTypeId, entityName, csAssert);

        }catch (Exception e){
            csAssert.assertTrue(false,"Exception while downloading and validating excel for filter sheet for entity " + entityName);
        }
        csAssert.assertAll();

    }

    @Test(dataProvider = "getReportIds",enabled = false)
    public void testreportlistdownload(String entityName,Integer reportid,Integer entityTypeId) {
        CustomAssert csAssert = new CustomAssert();
        try {
            String outputfilevalue = null;


            ReportRendererListData reportRendererListData = new ReportRendererListData();
            reportRendererListData.hitReportRendererListData(reportid);

            String reportlistRendererJsonStr = reportRendererListData.getListDataJsonStr();

            int listdatasize = getlistdataSize(reportlistRendererJsonStr);

            if(listdatasize > 2000){
                listdatasize = 2000;
            }
            //API gives HTML response after 2000 size
            reportRendererListData.hitReportRendererListData(entityTypeId, offset, listdatasize, orderByColumnName, orderDirection, reportid);

            reportlistRendererJsonStr = reportRendererListData.getListDataJsonStr();

            //String listMetaDataResponse = listMetaData.getListRendererDefaultUserListMetaDataJsonStr();

            TreeMap<Integer,Map<Integer,String>> listrowcolmapping = listrowcolmapping(reportlistRendererJsonStr,reportid,entityName);

            downloadListDataForAllColumns(entityTypeId, entityName, reportid);

            Boolean validationstatus = validatedownloadedexcel1(listrowcolmapping,entityName);


            if(validationstatus == true){
                csAssert.assertTrue(true,"Downloaded listing excel verified successfully for entity " + entityName);
            }else {
                csAssert.assertTrue(false,"Downloaded listing excel verified unsuccessfully for entity " + entityName);
            }

        } catch (Exception e) {
            logger.error("Exception while validating downloading list data");
            csAssert.assertTrue(false,"Exception while validating downloading list data for entity " + entityName);
        }
        csAssert.assertAll();
    }

    private int getlistdataSize(String listdataresponse){

        JSONObject listdatajson = new JSONObject(listdataresponse);
        int size = Integer.parseInt(listdatajson.get("filteredCount").toString());
        return size;
    }


    private TreeMap<Integer,String> columnordernamefilters(String listMetaDataResponse){
        JSONArray filtermetadata = new JSONObject(listMetaDataResponse).getJSONArray("filterMetadatas");
        Integer orderid;
        String name;
        TreeMap<Integer,String> filterordernamemap= new TreeMap<>();
        for(int i =0;i<filtermetadata.length();i++ ){
            orderid = (Integer)(filtermetadata.getJSONObject(i).get("order"));
            name = filtermetadata.getJSONObject(i).get("name").toString();
            filterordernamemap.put(orderid,name);
        }
        return filterordernamemap;
    }

    @DataProvider(name = "getReportIds", parallel = true)
    public Object[][] getReportIds() {

        int i = 0;
        Object[][] groupArray = new Object[allEntitySection.size()][];
        try {
            for (String entitySection : allEntitySection) {
                groupArray[i] = new Object[3];
                //Integer entitySectionTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(listRendererConfigFilePath, listRendererConfigFileName, entitySection, "entity_type_id"));
                Integer entitySectionTypeId = ConfigureConstantFields.getEntityIdByName(entitySection);
                String reportids[] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "reportidstotest", entitySection).split(",");

                for (String reportid : reportids) {

                    groupArray[i][0] = entitySection; // EntityName
                    groupArray[i][1] = Integer.parseInt(reportid); // ReportIds
                    groupArray[i][2] = entitySectionTypeId;
                    i++;
                }
            }
        }catch (Exception e){
            logger.error("Exception while getting report ids ");
        }
        return groupArray;
    }

    private void downloadListDataForAllColumns(Integer entityTypeId, String entityName, Integer reportID) {

        try{
            String filterjson = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"filterjson",String.valueOf(entityTypeId));
            String filtersheettobechecked = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"filtersheettobechecked",entityName);

            if(filtersheettobechecked.equals("true")){

                String filterids = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"filtersidstotestforreports",String.valueOf(reportID));
                String[] filterid = filterids.split(",");

                DownloadReportWithData listRendererFilterData = new DownloadReportWithData();
                listRendererFilterData.hitReportRendererFilterData(reportID);
                String filterdataresponse = listRendererFilterData.getReportRendererFilterDataJsonStr();

                Map<String,Map<String,String>> filteridoptionidmapping = filteridoptionidmapping(filterdataresponse,filterid);

            }

            Map<String, String> formParam = getFormParamDownloadList(entityTypeId,null,filterjson);

            logger.info("formParam is : [{}]", formParam);

            DownloadReportWithData downloadReportWithData = new DownloadReportWithData();
            logger.debug("Hitting DownloadListWithData for entity {} [typeid = {}] and [report id = {}]", entityName, entityTypeId, reportID);
            HttpResponse response = downloadReportWithData.hitDownloadReportWithData(formParam, reportID);

            if (response.getStatusLine().toString().contains("200")) {

                //            dumpResultsObj.dumpOneResultIntoCSVFile(resultsMap);
                /*
                 * dumping response into file
                 * */
                dumpDownloadListWithDataResponseIntoFile(response, outputFilePath, "ReportDataDownloadOutput", entityName, "AllColumn");

            } else {
                logger.error("Error while downloading list data for all columns for entity name +" + entityName);
            }
        }catch(Exception e){
            logger.error("Ëxception while downloading list data columns for entity name " + entityName);
        }
    }


    private void downloadListDataForAllColumnsfilter(Integer reportID,Integer entityTypeId, String entityName,CustomAssert csAssert) {

        try{
            String filterpayload;
            String filterjson;
            String filtersheettobechecked = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"filtersheettobechecked",entityName);
            String filtid;
            String filtername;
            String filterkey;
            String outputexceldownloadedpath;
            String dataid;
            String dataoptionvalue;
            Map<String,String> dataidmap;
            Boolean status = true;
            int listdatasize;
            if(filtersheettobechecked.equals("true")){

                String filterids = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"filtersidstotestforreports",String.valueOf(reportID));
                String[] filterid = filterids.split(",");

                ReportRendererFilterData reportRendererFilterData = new ReportRendererFilterData();
                reportRendererFilterData.hitReportRendererFilterData(reportID);
                String filterdataresponse = reportRendererFilterData.getReportRendererFilterDataJsonStr();

                //Getting meta data for the entity
                ReportRendererDefaultUserListMetaData reportRendererDefaultUserListMetaData = new ReportRendererDefaultUserListMetaData();
                reportRendererDefaultUserListMetaData.hitReportRendererDefaultUserListMetadata(reportID);
                String reportlistMetaDataResponse = reportRendererDefaultUserListMetaData.getReportRendererDefaultUserListMetaDataJsonStr();


                //Map<Integer,String> filtercolumnname = filtercolumnnamemapping(metadataresponse);

                Map<String,Map<String,String>> filteridoptionidmapping = filteridoptionidmapping(filterdataresponse,filterid);

                for(Map.Entry<String,Map<String,String>> entry: filteridoptionidmapping.entrySet()){

                    filterkey = entry.getKey();
                    dataidmap = entry.getValue();
                    filtid = filterkey.split("->")[0];
                    filtername = filterkey.split("->")[1];
                    for(Map.Entry<String,String> dataentry : dataidmap.entrySet()){

                        dataid = dataentry.getKey();
                        dataoptionvalue = dataentry.getValue();

                        filterjson = createfilterjson(entityTypeId,filtid,filtername,dataid,dataoptionvalue);

                        filterpayload = createfilterPayload(entityTypeId,filtid,filtername,dataid,dataoptionvalue);

                        ReportRendererListData reportRendererListData = new ReportRendererListData();
                        reportRendererListData.hitReportRendererListData(reportID,filterpayload);

                        String reportlistRendererJsonStr = reportRendererListData.getListDataJsonStr();

                        listdatasize = getlistdataSize(reportlistRendererJsonStr);
                        if(listdatasize == 0){
                            continue;
                        }
                        Map<String, String> formParam = getFormParamDownloadList(entityTypeId,null,filterjson);

                        logger.info("formParam is : [{}]", formParam);

                        DownloadReportWithData downloadReportWithData = new DownloadReportWithData();
                        logger.debug("Hitting DownloadListWithData for entity {} [typeid = {}] and [report id = {}]", entityName, entityTypeId, reportID);
                        HttpResponse response = downloadReportWithData.hitDownloadReportWithData(formParam, reportID);

                        logger.debug("Filter name {} Option Value {} ",filtername,dataoptionvalue);

                        if (response.getStatusLine().toString().contains("200")) {

                            //            dumpResultsObj.dumpOneResultIntoCSVFile(resultsMap);
                            /*
                             * dumping response into file
                             * */

                            status = dumpDownloadListWithDataResponseIntoFile(response, outputFilePath, "ReportDataDownloadOutput", entityName, "AllColumn");
                            if(!(status == true)){
                                continue;
                            }
                            outputexceldownloadedpath = outputFilePath + "/ReportDataDownloadOutput/" + entityName;


                            status = validatefiltersheetofdownloadedlistdata(filterdataresponse,reportlistMetaDataResponse,outputexceldownloadedpath,entityName,filtername,dataoptionvalue);
                            if(status == false){
                                logger.error("Error while validating downloaded excel for entity name " + entityName + " filter name " + filtername + " data option value " + dataoptionvalue);
                                csAssert.assertTrue(false,"Error while validating downloaded excel for entity name" + entityName + " filter name " + filtername + " data option value" + dataoptionvalue);
                            }
                        } else {
                            logger.error("Error while downloading list data for all columns for entity name +" + entityName);
                        }
                    }
                }
            }
        }catch(Exception e){
            logger.error("Ëxception while downloading list data columns for entity name " + entityName);
            csAssert.assertTrue(false,"Ëxception while downloading list data columns for entity name " + entityName);
        }
    }

    private Boolean validatefiltersheetofdownloadedlistdata(String filterdataresponse,String metadataresponse,String outputexceldownloadedpath,String entityName,String filtername,String dataoptionvalue){
        Boolean validationstatus = true;
        try {
            JSONObject jobj = new JSONObject(filterdataresponse);
            JSONObject indvfilterobj;
            JSONArray jarray = JSONUtility.convertJsonOnjectToJsonArray(jobj);
            List <String> filternamelist = new ArrayList<>();
            TreeMap<Integer,String> filternameorder = new TreeMap<>();

            for(int i =0;i<jarray.length();i++){
                indvfilterobj = jarray.getJSONObject(i);
                filternamelist.add(indvfilterobj.get("filterName").toString());
            }

            JSONObject metaresponse = new JSONObject(metadataresponse);
            JSONArray filtermeta = metaresponse.getJSONArray("filterMetadatas");

            String queryname;
            Integer order;
            String filterexcelcolumnname;
            String excelcolumntotesforfiltevalue = null;
            int filtercolumntotest = 0;
            for(int j = 0;j < filtermeta.length();j++){
                queryname = filtermeta.getJSONObject(j).get("queryName").toString();

                if(filternamelist.contains(queryname))
                {
                    if(queryname.equals(filtername)){
                        excelcolumntotesforfiltevalue = filtermeta.getJSONObject(j).get("name").toString();
                    }
                    order = Integer.parseInt(filtermeta.getJSONObject(j).get("order").toString());
                    filterexcelcolumnname = filtermeta.getJSONObject(j).get("name").toString();
                    filternameorder.put(order,filterexcelcolumnname);
                }

            }
            Thread.sleep(3000);
            XLSUtils xlsUtils = new XLSUtils(outputexceldownloadedpath, "AllColumn.xlsx");
            List<String> filternameorderlist = new ArrayList<>();
            for(Map.Entry<Integer,String> entry : filternameorder.entrySet()) {
                filternameorderlist.add(entry.getValue());
            }

            int columncount = xlsUtils.getColumnCount("Filter");
            for(int i=0;i<columncount;i++){
                String expectedfiltercolumnvalue = filternameorderlist.get(i).toString();
                String actualfiltercolumnvalue = xlsUtils.getCellData("Filter",i,3);
                if(excelcolumntotesforfiltevalue.equals(actualfiltercolumnvalue)){
                    filtercolumntotest = i;
                }

                if(expectedfiltercolumnvalue.equalsIgnoreCase(actualfiltercolumnvalue)){
                    logger.info("Filter Column present in the downloaded excel sheet");
                }
                else {
                    logger.error("Filter Column not present in the downloaded excel sheet");
                    validationstatus = false;
                }
            }

            for(int i=0;i<columncount;i++){

                String actualfiltercolumnvalue = xlsUtils.getCellData("Filter",i,4);
                if(i == filtercolumntotest){
                    if(actualfiltercolumnvalue.equals(dataoptionvalue)){
                        logger.info("Valid value of filter for row number {} column number {} ",4,i);
                    }
                    else {
                        logger.error("Valid value of filter for row number {} column number {} ",4,i);
                        validationstatus = false;
                    }

                }else {
                    if(actualfiltercolumnvalue.equals("-")){
                        logger.info("Valid value of filter for row number {} column number {} ",4,i);
                    }
                    else {
                        logger.error("Valid value of filter for row number {} column number {} ",4,i);
                        validationstatus = false;
                    }
                }

            }


        }catch (Exception e){
            logger.error("Exception while validating downloaded excel " + outputexceldownloadedpath + " " + e.getMessage());
            validationstatus = false;
        }
        return validationstatus;
    }
    private Map<Integer,String> filtercolumnnamemapping(String metadataresponse){
        JSONObject metaobj = new JSONObject(metadataresponse);
        JSONArray filtermetaoptions = metaobj.getJSONArray("filterMetadatas");
        JSONObject filtermetaoption;
        String name;
        int order;
        Map<Integer,String> filtercolumnnamemap = new HashMap<>();
        for(int i = 0;i<filtermetaoptions.length();i++){
            filtermetaoption = filtermetaoptions.getJSONObject(i);
            order = Integer.parseInt(filtermetaoption.get("order").toString());
            name = filtermetaoption.get("name").toString();
            filtercolumnnamemap.put(order,name);
        }
        return filtercolumnnamemap;
    }
    private String createfilterjson(Integer entityTypeId, String filtid,String filtername,String dataid,String dataoptionvalue){

        String filterjson = "\""+ filtid +"\": {\"filterId\": \"" + filtid + "\",\"filterName\": \"" + filtername + "\",\"entityFieldId\": null,\"entityFieldHtmlType\": null,\"multiselectValues\": {\"SELECTEDDATA\": [{\"id\": \"" + dataid + "\",\"name\": \"" +dataoptionvalue +"\"}]}}";

        return filterjson;
    }
    private Map<String,Map<String,String>> filteridoptionidmapping(String filterapiresponse,String[] filterid){

        JSONObject filterjson = new JSONObject(filterapiresponse);
        JSONArray dataarray = null;
        JSONObject datajson;
        String dataid;
        String datavalue;
        String filtername;
        String fid;
        Map<String,Map<String,String>> filteriddataidmap = new HashMap();
        try {
            for (int i = 0; i < filterid.length; i++) {
                fid = filterid[i];
                try {
                    dataarray = filterjson.getJSONObject(fid).getJSONObject("multiselectValues").getJSONObject("OPTIONS").getJSONArray("data");
                }catch (Exception e){
                    try {
                        logger.info("'data' array not found in the filter data json....Checking for 'DATA' array");
                        dataarray = filterjson.getJSONObject(fid).getJSONObject("multiselectValues").getJSONObject("OPTIONS").getJSONArray("DATA");
                    }catch (Exception e1){
                        logger.error("Exception while parsing filter data json");
                    }
                }
                filtername = filterjson.getJSONObject(fid).get("filterName").toString();

                Map<String, String> dataidvalmap = new HashMap();
                for (int j = 0; j < dataarray.length(); j++) {
                    datajson = dataarray.getJSONObject(j);
                    dataid = datajson.get("id").toString();
                    datavalue = datajson.get("name").toString();

                    dataidvalmap.put(dataid, datavalue);
                }
                filteriddataidmap.put(fid + "->" + filtername, dataidvalmap);
            }
        }catch (Exception e){
            logger.error("Exception while parsing filter api json " + e.getMessage());
        }
        return filteriddataidmap;
    }

    private Boolean dumpDownloadListWithDataResponseIntoFile(HttpResponse response, String outputFilePath, String featureName, String entityName, String columnStatus) {
        Boolean status = false;
        String outputFile = null;
        FileUtils fileUtil = new FileUtils();
        Boolean isFolderSuccessfullyCreated = fileUtil.createNewFolder(outputFilePath, featureName);
        Boolean isFolderWithEntityNameCreated = fileUtil.createNewFolder(outputFilePath + "/" + featureName + "/", entityName);
        if (isFolderSuccessfullyCreated && isFolderWithEntityNameCreated) {
            outputFile = outputFilePath + "/" + featureName + "/" + entityName + "/" + columnStatus + outputFileFormatForDownloadListWithData;
            status = fileUtil.writeResponseIntoFile(response, outputFile);
            if (status)
                logger.info("DownloadListWithData file generated at {}", outputFile);
        }
        return status;
    }

    private Map<String, String> getFormParamDownloadList(Integer entityTypeId, Map<Integer, String> selectedColumnMap,String filterjson) {

        Map<String, String> formParam = new HashMap<String, String>();
        String jsonData = null;

        if (selectedColumnMap == null) {
            jsonData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\",\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":{" + filterjson +"}}}";
        } else {
            String selectedColumnArray = "\"selectedColumns\":[";
            for (Map.Entry<Integer, String> entryMap : selectedColumnMap.entrySet()) {
                selectedColumnArray += "{\"columnId\":" + entryMap.getKey() + ",\"columnQueryName\":\"" + entryMap.getValue() + "\"},";
            }
            selectedColumnArray = selectedColumnArray.substring(0, selectedColumnArray.length() - 1);
            selectedColumnArray += "]";

            jsonData = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":" + offset + ",\"size\":" + size + ",\"orderByColumnName\":\"" + orderByColumnName + "\",\"orderDirection\":\"" + orderDirection + "\",\"filterJson\":{}}," + selectedColumnArray + "}";
        }

        logger.debug("json for downloading list : [{}]", jsonData);
        formParam.put("jsonData", jsonData);
        formParam.put("_csrf_token", csrfToken);

        return formParam;
    }

    private boolean isNumeric(String str) {
        try
        {
            double d = Double.parseDouble(str);
        }
        catch(NumberFormatException nfe)
        {
            return false;
        }
        return true;
    }

    private Map<String, Map<String, String>> listrowcolumnmapping(String listRendererJsonStr,Map<String,String > columnidname){

        JSONObject rowdatajson;
        JSONArray datajsonarray;

        String columnname = null;
        String columnval = null;
        String columnid;

        //Map<String, String> columnnameval = new HashMap<>();
        Map<String, Map<String, String>> rowidentifierwithcolumnvalues = new HashMap<>();
        //Creating a map of column id and column name
        JSONArray jarray = new JSONObject(listRendererJsonStr).getJSONArray("data");

        for (int i = 0; i < jarray.length(); i++) {

            datajsonarray = JSONUtility.convertJsonOnjectToJsonArray(jarray.getJSONObject(i));
            Map<String, String> columnnameval = new HashMap<>();
            for (int j = 0; j < datajsonarray.length(); j++) {
                rowdatajson = datajsonarray.getJSONObject(j);
                try {
                    columnid = String.valueOf(rowdatajson.get("columnId"));
                    columnval = String.valueOf(rowdatajson.get("value"));
                    columnname = columnidname.get(columnid);
                } catch (Exception e) {
                    logger.error("Error parsing list data json ");
                }
                if (columnval.contains(":;")) {
                    columnval = columnval.split(":;")[0];
                }
                if(!columnname.equals("CHECKBOX")){
                    columnnameval.put(columnname, columnval);
                }
//                if (columnname.equals("id")) {
//                    rowidentifier = columnval;
//                }
                columnval = null;
            }
            rowidentifierwithcolumnvalues.put(String.valueOf(i), columnnameval);

        }
        return rowidentifierwithcolumnvalues;
    }

    private Map<String,String> columnidnamemetadata(String listMetaDataResponse){

        JSONObject jobj = new JSONObject(listMetaDataResponse);
        JSONArray jarray = jobj.getJSONArray("columns");
        JSONObject columndetailobject;
        String id;
        String columnname;
        Map<String,String> columnidnamemetadata = new HashMap<>();

        for(int i = 0;i<jarray.length();i++){
            columndetailobject = jarray.getJSONObject(i);
            id = String.valueOf(columndetailobject.get("id"));
            columnname = String.valueOf(columndetailobject.get("name")).toUpperCase();
            columnidnamemetadata.put(id,columnname);
        }
        return columnidnamemetadata;
    }

    private boolean validatedownloadedexcel(String outputFilePath, String outputfilevalue, Map<String, Map<String, String>> rowidentifierwithcolumnvalues,CustomAssert csAssert,String entityname,TreeMap<Integer,String> filterordermap) {


        int rownum = 3;
        int colnum = 0;

        Map<String,String> excelrowvalue;
        Map<String,String> listrowval;

        String excelcolname;
        String excelcolvalue;
        String listcolvalue = null;
        String excelrownumber;

        String listcolval = null;
        String exceldate;

        Double excelcolvnummvalue;

        Boolean status = true;

        String sheetname = null;
        try {
            //Preparing key value pairs for excel data
            XLSUtils xlsUtils = new XLSUtils(outputFilePath, outputfilevalue);

            status = validatemetadataexcel(xlsUtils,"Data",entityname);

            if(status == true){
                logger.info("Meta data validated successfully for the excel file " + outputFilePath + outputfilevalue);
            }
            else {
                logger.error("Meta data validated unsuccessfully for the excel file " + outputFilePath + outputfilevalue);
            }
            Map<String,Map<String,String>> excelrowcolmapping = new HashMap<>();

            int mapkeyrownum = 0;
            for(int excelrownum =4;excelrownum < xlsUtils.getRowCount("Data") - 2;excelrownum++){

                Map<String,String> colmapping = new HashMap<>();
                for(int columnnum = 0;columnnum<xlsUtils.getColumnCount("Data");columnnum++){

                    colmapping.put(xlsUtils.getCellData("Data",columnnum,rownum),xlsUtils.getCellData("Data",columnnum,excelrownum));

                }
                excelrowcolmapping.put(String.valueOf(mapkeyrownum),colmapping);
                mapkeyrownum++;
            }
            //Checking excel data with list data response
            for(Map.Entry<String,Map<String,String>> excelrow : excelrowcolmapping.entrySet()){

                excelrownumber =  excelrow.getKey();
                excelrowvalue = excelrow.getValue();

                listrowval = rowidentifierwithcolumnvalues.get(excelrownumber);

                for(Map.Entry<String,String > excelrowcolvalue : excelrowvalue.entrySet()){
                    excelcolname = excelrowcolvalue.getKey();
                    excelcolvalue = excelrowcolvalue.getValue();
                    listcolvalue = listrowval.get(excelcolname);
                    if(isNumeric(excelcolvalue)){
                        excelcolvnummvalue = Double.parseDouble(excelcolvalue);
                        listcolvalue = String.valueOf(Double.parseDouble(listrowval.get(excelcolname)));
                        excelcolvalue = String.valueOf(excelcolvnummvalue);
                    }
                    if(excelcolname.equals("ID")){
                        if(excelcolvalue.contains(listcolvalue)){
                            logger.info("Valid value for ID in the excel sheet");
                            csAssert.assertTrue(true,"Valid value for ID in the excel sheet");
                        }else {
                            logger.error("Value of ID does not contain the expected value " + listcolval);
                            csAssert.assertTrue(false,"Value of ID does not contain the expected value " + listcolval);
                            status = false;
                        }
                        continue;
                    }
                    if(!(listcolvalue.equals("null") || listcolvalue.equals("")) && listcolvalue.equals(excelcolvalue))
                    {
                        logger.info("List data column value and excel data column value are same for excel column " + excelcolname + " and row " + (excelrownumber + rownum));
                        csAssert.assertTrue(true,"List data column value and excel data column value are same for excel column " + excelcolname + " and row " + (excelrownumber + rownum));
                    }
                    else if((listcolvalue.equals("null") || listcolvalue.equals("")) && (excelcolvalue.equals("-") || excelcolvalue.equals("")))
                    {
                        logger.info("List data column value and excel data column value is valid for null value of list data for excel column " + excelcolname + " and row " + (excelrownumber + rownum));
                        csAssert.assertTrue(true,"List data column value and excel data column value is valid for null value of list data for excel column " + excelcolname + " and row " + (excelrownumber + rownum));
                    }
                    else if(excelcolname.contains("DATE") || excelcolname.equals("CREATED ON")){

                        exceldate = convertlistdate(excelcolvalue);

                        if(exceldate.equals(listcolvalue)){
                            logger.info("Date values are same for listing page and downloaded excel file for excel column " + excelcolname + " and row " + (excelrownumber + rownum));
                            csAssert.assertTrue(true,"Date values are same for listing page and downloaded excel file for excel column " + excelcolname + " and row " + (excelrownumber + rownum));
                        }
                        else {
                            logger.error("Date values are different for listing page and downloaded excel file for excel column " + excelcolname + " and row " + (excelrownumber + rownum));
                            csAssert.assertTrue(false,"Date values are different for listing page and downloaded excel file for excel column " + excelcolname + " and row " + (excelrownumber + rownum));
                            status = false;
                        }
                    }
                    else{
                        logger.error("List data column value and excel data column value are not same for excel column " + excelcolname + " and row " + (excelrownumber + rownum));
                        csAssert.assertTrue(false,"List data column value and excel data column value are different for excel column " + excelcolname + " and row " + (excelrownumber + rownum));
                        status =false;
                    }

                }
            }
            validatefiltersheet(xlsUtils,filterordermap,csAssert);

        } catch (Exception e) {
            logger.error("Exception while parsing value from downloaded excel sheet " + sheetname + "row num " + rownum + "column number " + colnum);
            status = false;
        }
        return status;
    }

    private boolean validatefiltersheet(XLSUtils xlsUtils,TreeMap<Integer,String> filterordermap,CustomAssert csAssert){

        Boolean status = true;

        int colcount = xlsUtils.getColumnCount("Filter");
        String filtername;
        TreeMap<Integer,String> excelfilterordermap = new TreeMap<>();
        try {
            for (int i = 0; i < colcount; i++) {

                filtername = xlsUtils.getCellData("Filter", i, 3);
                excelfilterordermap.put(i, filtername);
            }

            List<String> excelfilterList = new ArrayList<>(excelfilterordermap.values());
            List<String> listmetafilteridsList = new ArrayList<>(filterordermap.values());

            if(excelfilterList.size() == listmetafilteridsList.size()){
                for(int i =0;i<excelfilterList.size();i++){
                    if(excelfilterList.get(i).equals(listmetafilteridsList.get(i))){

                        logger.info("Expected value of filter column name present in the downloaded excel sheet for the filter sheet for column " + i);
                        csAssert.assertTrue(true,"Expected value of filter column name present in the downloaded excel sheet for the filter sheet for column " + i);
                    }
                    else {
                        logger.error("Expected value of filter column name not present in the downloaded excel sheet for the filter sheet for column " + i);
                        csAssert.assertTrue(false,"Expected value of filter column name not present in the downloaded excel sheet for the filter sheet for column " + i);
                    }
                }
            }
            else {
                logger.error("Column count different for list meta data and downloaded excel");
                csAssert.assertTrue(false,"Column count different for list meta data and downloaded excel");
            }

        }catch (Exception e){
            csAssert.assertTrue(false,"Exception while validating filter column values");
        }
        return status;
    }
    public boolean validatemetadataexcel(XLSUtils xlsUtils,String sheetname,String entityname){

        Boolean status = true;

        String date[] = DateUtils.getCurrentDateInDDMMMYYYY().split("/");

        String downloadedexcelexpetceddate = date[2] + date[1] +date[0];
        try {
            if (entityname.toUpperCase().contains(xlsUtils.getCellData(sheetname, 0, 0))) {
                logger.info("Expected value of entityname present in the downloaded excel sheet for " + entityname + " in the column 0 and row 0 for the sheet " + sheetname);

            } else {
                logger.error("Unexpected value of entity name present in the downloaded excel sheet for " + entityname + "in the column 0 and row 0 for the sheet " + sheetname);
                status = false;
            }

            if (xlsUtils.getCellData(sheetname, 0, 1).equals("Date :")) {
                logger.info("Expected value Date :  present in the downloaded excel sheet for " + entityname + "in the column 0 and row 1 for the sheet " + sheetname);

            } else {
                logger.error("Expected value Date : present in the downloaded excel sheet for " + entityname + "in the column 0 and row 1 for the sheet " + sheetname);
                status = false;
            }

            if (xlsUtils.getCellData(sheetname, 1, 1).equals(downloadedexcelexpetceddate)) {
                logger.info("Expected value of Date :  present in the downloaded excel sheet for " + entityname + "in the column 0 and row 1 for the sheet " + sheetname);

            } else {
                logger.error("Expected value of Date : present in the downloaded excel sheet for " + entityname + "in the column 0 and row 1 for the sheet " + sheetname);
                status = false;
            }

            if (xlsUtils.getCellData(sheetname, 0, 2).equals("Generated By : ")) {
                logger.info("Expected value Generated By :  present in the downloaded excel sheet for " + entityname + "in the column 0 and row 2 for the sheet " + sheetname);

            } else {
                logger.error("Expected value Generated By :  present in the downloaded excel sheet for " + entityname + "in the column 0 and row 2 for the sheet " + sheetname);
                status = false;
            }
            String generatedby = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "generatedby");
            if (xlsUtils.getCellData(sheetname, 1, 2).equals(generatedby)) {
                logger.info("Expected value of Generated By :  present in the downloaded excel sheet for " + entityname + "in the column 0 and row 2 for the sheet " + sheetname);

            } else {
                logger.error("Expected value of Generated By :  not present in the downloaded excel sheet for " + entityname + "in the column 0 and row 2 for the sheet " + sheetname);
                status = false;
            }
        }
        catch (Exception e){
            status = false;
            logger.error("Exception while parsing excel file " + e.getMessage());
            e.printStackTrace();
        }
        return status;
    }

    public String convertlistdate(String date){
        String datearray[] = date.split("/");
        String mon = null;
        try {
            mon = DateUtils.getMonthinMMM(Integer.parseInt(datearray[0]));
            if(Integer.parseInt(datearray[1])<10 ){
                datearray[1] = "0" + datearray[1];
            }
        }catch (Exception e){
            logger.error("Exception while parsing excel date");
            e.printStackTrace();
        }

        return mon + "-" + datearray[1] + "-" + "20" + datearray[2];
    }

    private TreeMap<Integer,Map<Integer,String>> listrowcolmapping(String listresponse,Integer reportid,String entityname){

        JSONArray dataarray = new JSONObject(listresponse).getJSONArray("data");
        JSONObject indvjsonobj;
        JSONObject coljsonobj;
        JSONArray indvjsonarray;
        String colname;
        String colval;
        Map<String,String> metadatacolumnmap = createmetadatacolumnname(reportid);
        TreeMap<Integer,Map<Integer,String>> rowcolmap = new TreeMap<>();
        Integer order;
        String showpageid = null;
        String idstring;
        try{
            for(int i =0;i<dataarray.length();i++){
                indvjsonobj = dataarray.getJSONObject(i);
                indvjsonarray = JSONUtility.convertJsonOnjectToJsonArray(indvjsonobj);
                TreeMap<Integer,String> colnamevaluemap = new TreeMap<>();
                TreeMap<Integer,String> colnamecolunummap = new TreeMap<>();
                for(int j =0;j<indvjsonarray.length();j++){
                    coljsonobj = indvjsonarray.getJSONObject(j);
                    String columnname = coljsonobj.get("columnName").toString();
                    colname = (metadatacolumnmap.get(columnname));
                    if(!(colname == null)){
                        colname = (metadatacolumnmap.get(columnname)).toString().toUpperCase();
                        colval = coljsonobj.get("value").toString();
                        order = Integer.parseInt(colname.split("->")[1]);
                        colname = colname.split("->")[0];
                        colnamecolunummap.put(order,colname);

                        if(colval.contains(":;")){
                            showpageid = colval.split(":;")[1];
                            colval = colval.split(":;")[0];
                        }

                        if(colname.equals("ID")){
                            idstring = ParseConfigFile.getValueFromConfigFile(entityIdConfigFilePath,entityIdMappingFileName,entityname,"short_code");
                            idstring = idstring + "0";
                            colval = idstring  + colval;
//                            colval = colval;
                            //lineitemlisttobedeleted.add(showpageid);
                        }
                        if(colname.equals("SUPPLIER SCORE")){
                            colval = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"supplier_score",colval);
                            if(colval == null){
                                colval = "null";
                            }
//                            colval = colval;
                            //lineitemlisttobedeleted.add(showpageid);
                        }
                        if(colname.equals("CHECKBOX")){
                            continue;
                        }

//						colnamevaluemap.put(colname,colval);
                        colnamevaluemap.put(order,colval);
                        rowcolmap.put(0,colnamecolunummap);
                        rowcolmap.put(i+1,colnamevaluemap);
                    }
                }
            }
        }catch(Exception e){
            logger.error("Exception while parsing meta data response for invoice details tab");
        }

        return rowcolmap;
    }

    private Map<String ,String> createmetadatacolumnname(int reportid){

        String queryname;
        String defaultname;
        Map<String, String> querycolmap = new HashMap<>();
        try {
            //int listid = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdConfigFilePath,entityIdMappingFileName,entityname,"entity_url_id"));
            ReportRendererDefaultUserListMetaData reportRendererDefaultUserListMetaData = new ReportRendererDefaultUserListMetaData();
            reportRendererDefaultUserListMetaData.hitReportRendererDefaultUserListMetadata(reportid);

            String metadataresponse = reportRendererDefaultUserListMetaData.getReportRendererDefaultUserListMetaDataJsonStr();
            JSONArray columnnames = new JSONObject(metadataresponse).getJSONArray("columns");
            String displayformat;

            for (int i = 0; i < columnnames.length(); i++) {
                displayformat = columnnames.getJSONObject(i).get("displayFormat").toString();
                if (displayformat.equals("{\"hideColumn\" : true}") || displayformat.equals("{\"removeExcel\" : true}")) {
                    continue;
                }
                queryname = columnnames.getJSONObject(i).get("queryName").toString();
                if (queryname.equals("bulkcheckbox")) {
                    continue;
                }
                defaultname = columnnames.getJSONObject(i).get("name").toString();
                if (defaultname.equals("null")) {
                    defaultname = columnnames.getJSONObject(i).get("defaultName").toString();
                }
                defaultname = defaultname + "->" + columnnames.getJSONObject(i).get("order").toString();
                querycolmap.put(queryname, defaultname);
            }
        }catch (Exception e){
            logger.error("Exception while creating meta data column name for list data");
        }
        return querycolmap;
    }

    private Boolean validatedownloadedexcel1(TreeMap<Integer,Map<Integer,String>> rowcolmapping,String entityName){

        Boolean excelvalidationstatus = true;
        String excelcolumntoskipvalidation[];
        String excelcoltoskip;
        List<String> excelcolumntoskiplist = new ArrayList<>();
        try {

            List<Integer> columnstoskip = new ArrayList<>();

            String outputFilePath1 = outputFilePath + "/ReportDataDownloadOutput/" + entityName;

            XLSUtils xlsUtils = new XLSUtils(outputFilePath1,"AllColumn.xlsx");
            int datarowcount = xlsUtils.getRowCount("Data");

            int columnum = 0;
            String exceldata;
            String expectedexcelcelldata;
            String date[];
            int rownum = 0;
            try {
                excelcoltoskip = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"excelcolumnstoskipvalidation",entityName);
                if (!(excelcoltoskip.equals(null))) {
                    excelcolumntoskipvalidation = excelcoltoskip.split(",");
                    excelcolumntoskiplist = Arrays.asList(excelcolumntoskipvalidation);
                }
            }catch (Exception e){
                logger.info("No Columns to skip");
            }
            for (Map.Entry<Integer, Map<Integer, String>> entry : rowcolmapping.entrySet()) {

                rownum = entry.getKey() + 3;
                if(rownum > 2003){
                    break;
                }

                System.out.println(rownum);
                Map<Integer,String> colnumval = entry.getValue();
                columnum = 0;
                String month;
                String dateconv;
                String year;
                for(Map.Entry<Integer,String> entry1 : colnumval.entrySet()){
                    expectedexcelcelldata = entry1.getValue();
                    if(isValidDate(expectedexcelcelldata)){
                        date = expectedexcelcelldata.split("-");
                        month = DateUtils.getMonthindigit(date[0]);
                        dateconv = date[1];
                        if(Integer.parseInt(dateconv) >= 0 && Integer.parseInt(dateconv) <= 9){
                            dateconv = dateconv.substring(1);
                        }
                        year = date[2].substring(2);
                        expectedexcelcelldata = month + "/" + dateconv + "/" + year;
                    }
                    System.out.println(expectedexcelcelldata);
                    if(columnstoskip.contains(columnum)){
                        continue;
                    }
                    exceldata = xlsUtils.getCellData("Data",columnum,rownum);

                    if(expectedexcelcelldata.equals("null") || expectedexcelcelldata.equals("")){
                        if(exceldata.equals("-") || exceldata.equals("")){
                            logger.info("Expected and Actual values match for Data Sheet for downloaded excel for rownum " + rownum + " and column number " + columnum);
                        }
                        else {
                            logger.error("Expected and Actual values mismatch for Data Sheet for downloaded excel for rownum " + rownum + " and column number " + columnum);
                            excelvalidationstatus = false;
                        }
                    }else if(isNumeric(expectedexcelcelldata)){
                        try{
                            Double expexcelval = Double.parseDouble(expectedexcelcelldata);
                            Double actexceldata = Double.parseDouble(exceldata);

                            if(expexcelval.equals(actexceldata)){
                                logger.info("Expected and Actual values match for Data Sheet for downloaded excel for rownum " + rownum + " and column number " + columnum);
                            }
                            else {
                                logger.error("Expected and Actual values mismatch for Data Sheet for downloaded excel for rownum " + rownum + " and column number " + columnum);
                                excelvalidationstatus = false;
                            }

                        }catch (Exception e){
                            logger.error("Exception while converting numeric values in the excel");
                            excelvalidationstatus = false;
                        }

                    }
                    else if(exceldata.equals(expectedexcelcelldata)){
                        logger.info("Expected and Actual values match for Data Sheet for downloaded excel for rownum " + rownum + " and column number " + columnum);
                    }
                    else {
                        logger.error("Expected and Actual values mismatch for Data Sheet for downloaded excel for rownum " + rownum + " and column number " + columnum);
                        excelvalidationstatus = false;
                    }

                    if(rownum == 3 && excelcolumntoskiplist.contains(exceldata)){

                        columnstoskip.add(columnum);
                    }
                    columnum = columnum + 1;
                }
            }
            exceldata = xlsUtils.getCellData("Data",0,datarowcount -1);

            expectedexcelcelldata = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"disclaimer");

            if(exceldata.equals(expectedexcelcelldata)){
                logger.info("Disclaimer value validated successfully");
            }else {
                logger.error("Disclaimer value validated unsuccessfully");
            }

        }catch (Exception e){
            logger.error("Exception while validaing data from downloaded excel for invoices details tab " + e.getMessage());
            return false;
        }
        return excelvalidationstatus;
    }

    public boolean isValidDate(String dateString) {

        try {
            SimpleDateFormat df = new SimpleDateFormat("MMM-dd-yyyy");
            df.parse(dateString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Test(dataProvider = "getAllEntitySection",enabled = false)
    public void testreportdownload(String entityName, Integer entityTypeId, Integer entityListId,Integer reportid) {
        CustomAssert csAssert = new CustomAssert();
        try {
            String outputfilevalue = null;
            String filterjson = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "filterjson", String.valueOf(entityTypeId));

            ListRendererListData listObj = new ListRendererListData();
            ListRendererDefaultUserListMetaData listMetaData = new ListRendererDefaultUserListMetaData();

            listMetaData.hitListRendererDefaultUserListMetadata(entityListId);

            listObj.hitListRendererListData(entityTypeId, offset, size, orderByColumnName, orderDirection, entityListId, filterjson);

            String listRendererJsonStr = listObj.getListDataJsonStr();
            int listdatasize = getlistdataSize(listRendererJsonStr);

            if(listdatasize > 2000){
                listdatasize = 2000;
            }
            //API gives HTML response after 2000 size
            listObj.hitListRendererListData(entityTypeId, offset, listdatasize, orderByColumnName, orderDirection, entityListId, filterjson);

            listRendererJsonStr = listObj.getListDataJsonStr();

            TreeMap<Integer,Map<Integer,String>> listrowcolmapping = listrowcolmapping(listRendererJsonStr,reportid,entityName);

            downloadListDataForAllColumns(entityTypeId, entityName, entityListId);

            Boolean validationstatus = validatedownloadedexcel1(listrowcolmapping,entityName);


            if(validationstatus == true){
                csAssert.assertTrue(true,"Downloaded listing excel verified successfully for entity " + entityName);
            }else {
                csAssert.assertTrue(false,"Downloaded listing excel verified unsuccessfully for entity " + entityName);
            }

        } catch (Exception e) {
            logger.error("Exception while validating downloading list data");
            csAssert.assertTrue(false,"Exception while validating downloading list data for entity " + entityName);
        }
        csAssert.assertAll();
    }

    private void downloadReportListDataForAllColumns(Integer entityTypeId, String entityName, Integer reportId) {
        Boolean downloadstatus;
        try{
            String filterjson = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"filterjson",String.valueOf(entityTypeId));
            String filtersheettobechecked = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"filtersheettobechecked",entityName);

            if(filtersheettobechecked.equals("true")){

                String filterids = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"filtersidstotest",entityName);
                String[] filterid = filterids.split(",");

                DownloadReportWithData downloadReportWithData = new DownloadReportWithData();
                downloadReportWithData.hitReportRendererFilterData(reportId);
                String filterdataresponse = downloadReportWithData.getReportRendererFilterDataJsonStr();

                Map<String,Map<String,String>> filteridoptionidmapping = filteridoptionidmapping(filterdataresponse,filterid);

            }

            Map<String, String> formParam = getFormParamDownloadList(entityTypeId,null,filterjson);

            logger.info("formParam is : [{}]", formParam);

            DownloadReportWithData downloadReportWithData = new DownloadReportWithData();

            logger.debug("Hitting DownloadListWithData for entity {} [typeid = {}] and [urlid = {}]", entityName, entityTypeId, reportId);
            HttpResponse response = downloadReportWithData.hitDownloadReportWithData(formParam, reportId);

            if (response.getStatusLine().toString().contains("200")) {

                //            dumpResultsObj.dumpOneResultIntoCSVFile(resultsMap);
                /*
                 * dumping response into file
                 * */
                downloadstatus = dumpDownloadListWithDataResponseIntoFile(response, outputFilePath, "ReportDataDownloadOutput", entityName, "AllColumn");
                if(!(downloadstatus == true)){
                    return;
                }

            } else {
                logger.error("Error while downloading list data for all columns for entity name +" + entityName);
            }
        }catch(Exception e){
            logger.error("Ëxception while downloading list data columns for entity name " + entityName);
        }
    }

    private String createfilterPayload(Integer entitytypeid,String filterid,String filtername,String dataid,String dataoptoinvalue){
        String filterpayload = "{\"filterMap\":{\"entityTypeId\":" + entitytypeid + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"" + filterid +"\":{\"filterId\":\"" + filterid +"\",\"filterName\":\"" + filtername +"\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + dataid + "\",\"name\":\"" + dataoptoinvalue + "\"}]}}}}}";
        return filterpayload;
    }
}

