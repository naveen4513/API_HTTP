package com.sirionlabs.test.governanceBody;

import com.sirionlabs.api.auditLogs.AuditLog;
import com.sirionlabs.api.auditLogs.FieldHistory;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.auditlog.DownloadAuditLog;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.XLSUtils;
import net.minidev.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.*;

@Listeners(value = MyTestListenerAdapter.class)
public class TestGBAuditLog {

    private final static Logger logger = LoggerFactory.getLogger(TestGBAuditLog.class);
    private String AuditLogConfigFilePath;
    private String AuditLogConfigFileName;
    private String filtermap;
    private DownloadAuditLog downloadAudit;
    private AuditLog auditlog;
    private String outputFilePath;
    private String outputFileName;
    private String entityIdMappingFileName;
    private String entityIdConfigFilePath;
    private String sheetName;
    private String generatedBy;
    private XLSUtils xlsreader;
    private String entitySectionSplitter = ",";
    private ListRendererDefaultUserListMetaData metadata;
    private List<String> allEntitySection;
    private List<String> columntoskip;
    private List<Map<String, String>> columns;
    private  FieldHistory fieldHistory;
    private ListRendererListData listObj;
    private Show show;

    @BeforeClass
    public void beforeClass(){
        entityIdConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath");
        entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
        AuditLogConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestAuditLogConfigFilePath");
        AuditLogConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("TestAuditLogConfigFileName");
        filtermap = ParseConfigFile.getValueFromConfigFile(AuditLogConfigFilePath, AuditLogConfigFileName, "filterjson", "86");
        outputFilePath = ParseConfigFile.getValueFromConfigFile(AuditLogConfigFilePath, AuditLogConfigFileName, "outputfilepath");
        outputFileName = ParseConfigFile.getValueFromConfigFile(AuditLogConfigFilePath, AuditLogConfigFileName,"outputfilename");
        sheetName = ParseConfigFile.getValueFromConfigFile(AuditLogConfigFilePath, AuditLogConfigFileName,"sheetname");
        generatedBy = ParseConfigFile.getValueFromConfigFile(AuditLogConfigFilePath, AuditLogConfigFileName,"generatedby");
        allEntitySection = Arrays.asList(ParseConfigFile.getValueFromConfigFile(AuditLogConfigFilePath, AuditLogConfigFileName, "entitytotest").split(entitySectionSplitter));
        columntoskip = Arrays.asList(ParseConfigFile.getValueFromConfigFile(AuditLogConfigFilePath, AuditLogConfigFileName, "columntoskip").split(entitySectionSplitter));
        columntoskip.replaceAll(String::toLowerCase);
        downloadAudit = new DownloadAuditLog();
        auditlog = new AuditLog();
        fieldHistory = new FieldHistory();
        metadata = new ListRendererDefaultUserListMetaData();
         listObj = new ListRendererListData();
         show = new Show();
    }

    @DataProvider(name = "getAllEntitySection", parallel = true)
    public Object[][] getAllEntitySection() {

        int i = 0;
        Object[][] groupArray = new Object[allEntitySection.size()][];

        for (String entitySection : allEntitySection) {
            groupArray[i] = new Object[3];
            String entitySectionTypeId = String.valueOf(ConfigureConstantFields.getEntityIdByName(entitySection));
            Integer entitySectionListId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdConfigFilePath, entityIdMappingFileName, entitySection, "entity_url_id"));
            groupArray[i][0] = entitySection.trim(); // EntityName
            groupArray[i][1] = entitySectionTypeId; // EntityTypeId
            groupArray[i][2] = entitySectionListId;// EntityURlId
            i++;
        }

        return groupArray;
    }


    @Test(dataProvider = "getAllEntitySection")
    public void TestGBAuditLog(String entityTypeName, String entityTypeId, int listId) throws IOException {
        CustomAssert csassert = new CustomAssert();

        listObj.hitListRendererListData(listId);
        String listRendererJsonStr = listObj.getListDataJsonStr();
        String columnId = JSONUtility.parseJson(listRendererJsonStr,"$.data[0].[*][?(@.columnName=='id')].columnId").toString();
        List<Integer> entityIdList = listObj.getAllRecordDbId(Integer.parseInt(columnId.substring(1,columnId.length()-1)),listRendererJsonStr);
        int entityId = entityIdList.get(0);
        show.hitShowGetAPI(Integer.valueOf(entityTypeId),entityId);
        String showResponse = show.getShowJsonStr();
        String clientEntitySeqId = (String) JSONUtility.parseJson(showResponse,"$.body.data.shortCodeId.values");
        Map<String, String> formParam = downloadAudit.getFormParam(filtermap);
        downloadAudit.HitDownloadAuditLog(String.valueOf(entityId),entityTypeId,true,clientEntitySeqId,formParam,outputFilePath,outputFileName);
        String auditlog_response = auditlog.hitAuditLogDataApi(entityTypeId,String.valueOf(entityId),filtermap);
        JSONArray data =(JSONArray) JSONUtility.parseJson(auditlog_response, "$.data");

       String downloadedPath = outputFilePath+"/auditLog/"+entityTypeName;

        xlsreader = new XLSUtils(downloadedPath+"/"+outputFileName);

        validateDownloadedExcel(downloadedPath,outputFileName,clientEntitySeqId,entityTypeName,entityTypeId,data,csassert);

        csassert.assertAll();

    }



    private boolean validateDownloadedExcel(String outputFilePath, String outputFileName,String clientEntitySeqId, String entityTypeName,String entityTypeId, JSONArray data, CustomAssert csAssert) throws IOException {

        validateDownloadedExcelMetaData(generatedBy,clientEntitySeqId,entityTypeName,csAssert);
        validateDownloadedExcelColumn(outputFilePath,outputFileName,entityTypeName,entityTypeId,csAssert);
        ValidateDownloadedExceldata(outputFilePath,outputFileName,data,csAssert);
        return false;
    }


    private void ValidateDownloadedExceldata(String outputFilePath, String outputFileName,JSONArray data, CustomAssert csassert){

        Long data_rows = XLSUtils.getNoOfRows(outputFilePath,outputFileName,sheetName)-7;
        if(data_rows==data.size()){
            String expected_value="" ;
            String actual_value="";
            for (int i = 0; i <data.size() ; i++) {
                for (int j = 0; j <columns.size() ; j++) {
                    if(!columntoskip.contains(columns.get(j).get("name").toLowerCase())) {
                        if(columns.get(j).get("name").toLowerCase().equals("history")){
                            String field_historyURL = (String) JSONUtility.parseJson(data.toJSONString(), "$[" + i + "].[*][?(@.columnName=='" + columns.get(j).get("queryName") + "')].value").toString();
                            Long historyId = Long.valueOf( field_historyURL.split("\\/")[3].replace("\\",""));
                            int entityTypeId = Integer.valueOf(field_historyURL.split("\\/")[4].replace("\"]",""));
                            String history_response = fieldHistory.hitFieldHistory(historyId,entityTypeId);
                            JSONArray valueArray= (JSONArray)JSONUtility.parseJson(history_response,"$.value");
                            expected_value= "";
                            for (int k = 0; k <valueArray.size() ; k++) {

                             String fieldName = (String)JSONUtility.parseJson(valueArray.toString(),"$["+k+"].property") ;
                             String action = (String)JSONUtility.parseJson(valueArray.toString(),"$["+k+"].state") ;
                             String oldValue = (String)JSONUtility.parseJson(valueArray.toString(),"$["+k+"].oldValue");
                             oldValue = (oldValue==null) ? "-" : oldValue;
                             String newValue = (String)JSONUtility.parseJson(valueArray.toString(),"$["+k+"].newValue") ;
                             newValue = (newValue==null) ? "-" : newValue;

                            expected_value =  expected_value + " { Field Name : "+fieldName+" , Action : "+action+" , Old Value : "+oldValue+" , New Value : "+newValue+" }";


                            }

                            actual_value = xlsreader.getCellDataByCustomcolumn(sheetName, columns.get(j).get("name").toUpperCase(), i + 6, 4);
                            if(!expected_value.equals("")) {
                                csassert.assertTrue(expected_value.contains(actual_value.trim()), "value mismatch for column " + columns.get(j).get("name").toUpperCase() + " and row " + (i + 6));
                            }
                            else{
                                csassert.assertEquals("-",actual_value, "value mismatch for column " + columns.get(j).get("name").toUpperCase() + " and row " + (i + 6));

                            }
                            }else {
                            expected_value = (String) JSONUtility.parseJson(data.toJSONString(), "$[" + i + "].[*][?(@.columnName=='" + columns.get(j).get("queryName") + "')].value").toString();
                            actual_value = xlsreader.getCellDataByCustomcolumn(sheetName, columns.get(j).get("name").toUpperCase(), i + 6, 4);
                            csassert.assertTrue(expected_value.replace(" ","").
                                    contains(actual_value.replace(" ","")),
                                    "value mismatch for column " + columns.get(j).get("name").toUpperCase() + " and row " + (i + 6));
                        }
                    }else{
                        logger.info("excel column "+columns.get(j).get("name").toUpperCase()+ " Skipped" );
                    }
                    }

            }


        }else{
          csassert.assertTrue(false, "no of data rows in excel and Api are different" );
        }

    }


    private void validateDownloadedExcelMetaData(String generatedBy, String entityClientSequenceId, String entityTypeName, CustomAssert csassert) throws IOException {
       csassert.assertEquals( xlsreader.getCellData(sheetName,0,0).toLowerCase(),entityTypeName+"-audit log","file name is not correct");
       csassert.assertTrue( xlsreader.getCellData(sheetName,0,1).toLowerCase().contains("entity id"),"Entity Id label is incorrect");
       csassert.assertEquals( xlsreader.getCellData(sheetName,1,1),entityClientSequenceId,"Entity Id value is incorrect");
       csassert.assertEquals( xlsreader.getCellData(sheetName,0,2),"Date :","Date label is incorrect");
      // csassert.assertEquals( xlsreader.getCellData(sheetName,1,2),"","Date value is incorrect");
       csassert.assertEquals( xlsreader.getCellData(sheetName,0,3),"Generated By :","generated by label is incorrect");
       csassert.assertEquals( xlsreader.getCellData(sheetName,1,3),generatedBy,"generated by value is incorrect");
    }

    private void validateDownloadedExcelColumn (String outputFilePath,String outputFileName,String entityTypeName, String entityTypeId, CustomAssert csassert) throws IOException {

        HashMap<String,String> param = new HashMap<>();
        param.put("entityTypeId",entityTypeId);
        metadata.hitListRendererDefaultUserListMetadata(61,param);
        metadata.setColumns(metadata.getListRendererDefaultUserListMetaDataJsonStr());
        columns = metadata.getColumns();
        TreeSet<String> listingcolumns  = new TreeSet<>();
        for (Map<String, String> column:
             columns) {
            listingcolumns.add(column.get("name").toLowerCase());
        }
        List<String> excelcolumns = XLSUtils.getExcelDataOfOneRow(outputFilePath, outputFileName, sheetName, 5);
        excelcolumns.replaceAll(String::toLowerCase);
        TreeSet<String> xlcolumns = new TreeSet<>(excelcolumns);
        if(xlcolumns.size()==columns.size()){

            csassert.assertTrue(xlcolumns.equals(listingcolumns),"column names are different in audit log downloded excel and audit log api");
        }else{
            csassert.assertTrue(false,"No of column is different in auditlog excel and listing ");
        }

    }
}
