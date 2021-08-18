package com.sirionlabs.test.auditlogreporting;


import com.sirionlabs.api.auditLogs.AuditLog;
import com.sirionlabs.api.auditLogs.FieldHistory;
import com.sirionlabs.api.commonAPI.Actions;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.DateUtils;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import net.minidev.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

public class TestAuditLogListing {

    private final static Logger logger = LoggerFactory.getLogger(TestAuditLogListing.class);
    private String AuditLogListingConfigFilePath;
    private String AuditLogListingConfigFileName;
    private String entityIdMappingFileName;
    private String entityIdConfigFilePath;
    private String entitySectionSplitter = ",";
    private List<String> allEntitySection;
    private String filtermap;
    private String timeZone;


    private ListRendererDefaultUserListMetaData metadata = new ListRendererDefaultUserListMetaData();
    private ListRendererListData listObj = new ListRendererListData();
    private Show show = new Show();
    private Edit edit = new Edit();
    private AuditLog auditlog = new AuditLog();
    private FieldHistory history = new FieldHistory();
    ArrayList<ArrayList<String>> auditLog =
            new ArrayList<ArrayList<String> >();




    @BeforeClass
    public void beforeClass(){
        entityIdConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath");
        entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
        AuditLogListingConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestAuditLogConfigFilePath");
        AuditLogListingConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("TestAuditLogListingConfigFileName");
        allEntitySection = Arrays.asList(ParseConfigFile.getValueFromConfigFile(AuditLogListingConfigFilePath, AuditLogListingConfigFileName, "entitytotest").split(entitySectionSplitter));

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
    public void TestEditAuditLog(String entityTypeName, String entityTypeId, int listId) throws Exception {
        CustomAssert csassert = new CustomAssert();
      int entityId =   getEntityIdfromListingpage(listId,Integer.valueOf(entityTypeId));
      if(entityId!=-1){
           List<String> fields = Arrays.asList(ParseConfigFile.getValueFromConfigFile(AuditLogListingConfigFilePath, AuditLogListingConfigFileName, entityTypeName,"fields").split(entitySectionSplitter));
            editEntity(entityTypeName,fields,entityTypeId,entityId,csassert);
          filtermap = ParseConfigFile.getValueFromConfigFile(AuditLogListingConfigFilePath, AuditLogListingConfigFileName, "filterjson",entityTypeId);
          String auditlog_response = auditlog.hitAuditLogDataApi(entityTypeId,String.valueOf(entityId),filtermap);
          validateHistory(auditlog_response,csassert);
             validateauditlog(auditlog_response,csassert);
      }else {
          csassert.assertTrue(false," no editable entity found on "+ entityTypeName+" Listing Page");
      }
        csassert.assertAll();
    }

    private int getEntityIdfromListingpage(int listId, int entityTypeId){
        int entityId = -1;
        listObj.hitListRendererListData(listId);
        String listRendererJsonStr = listObj.getListDataJsonStr();
        String columnId = JSONUtility.parseJson(listRendererJsonStr,"$.data[0].[*][?(@.columnName=='id')].columnId").toString();
        List<Integer> entityIdList = listObj.getAllRecordDbId(Integer.parseInt(columnId.substring(1,columnId.length()-1)),listRendererJsonStr);
     for (int i = 0; i<entityIdList.size();i++){
          String actionResponse =  Actions.getActionsV3Response(entityTypeId,entityIdList.get(i));
         JSONArray actions = (JSONArray) JSONUtility.parseJson(actionResponse, "$.layoutActions[*][?(@.name=='Edit')]");
           if(actions.size()>0){
               entityId = entityIdList.get(i);
           break;
           }

        }
     return  entityId;
    }


    public void editEntity(String entityTypeName , List<String> fields, String entityTypeId, int entityId, CustomAssert csAssert) throws Exception {
        show.hitShowGetAPI(Integer.parseInt(entityTypeId),entityId);
        String entityShow = show.getShowJsonStr();
        JSONObject data = new JSONObject(entityShow).getJSONObject("body").getJSONObject("data");
        timeZone = data.getJSONObject("timeZone").getJSONObject("values").getString("timeZone");

        for (String field: fields) {
           String fieldJson =  ParseConfigFile.getValueFromConfigFile(AuditLogListingConfigFilePath, AuditLogListingConfigFileName, entityTypeName,field);
            JSONObject fieldJsonObj = new JSONObject(fieldJson);
            String oldvalue = (String) JSONUtility.parseJson(data.toString(),"$."+field+".values");
            String newValue = "updated"+DateUtils.getCurrentTimeStamp();
            fieldJsonObj.put("values",newValue);
            data.put(field, fieldJsonObj);
            ArrayList<String> audit = new ArrayList<>();
            audit.add("Description");
            audit.add("MODIFIED");
            audit.add(oldvalue);
            audit.add(newValue);
            auditLog.add(audit);
        }
        data.remove("history");
        JSONObject payload = new JSONObject();
        JSONObject body = new JSONObject();
        body.put("data",data);
        payload.put("body",body);
        String editResponce = edit.hitEdit(entityTypeName,payload.toString());
        csAssert.assertEquals(JSONUtility.parseJson(editResponce,"$.header.response.status"),
                "success",  entityTypeName+ " "+ entityId+" description is not updated");

    }

    public void validateHistory(String auditlog_response,CustomAssert csAssert){
        String history_url =(String)((LinkedHashMap)((JSONArray)JSONUtility.parseJson(auditlog_response,"$.data[0].[*][?(@.columnName=='history')]")).get(0)).get("value");
        String historyResponse =   history.hitFieldHistory(history_url,false);
       csAssert.assertEquals(JSONUtility.parseJson(historyResponse,"$.value[0].oldValue"),auditLog.get(0).get(2),"old value in history is not correct");
       csAssert.assertEquals(JSONUtility.parseJson(historyResponse,"$.value[0].newValue"),auditLog.get(0).get(3) , "new value in history is not correct");
       csAssert.assertEquals(JSONUtility.parseJson(historyResponse,"$.value[0].property"),auditLog.get(0).get(0) , "Field Name is not correct");
       csAssert.assertEquals(JSONUtility.parseJson(historyResponse,"$.value[0].state").toString().toLowerCase(),auditLog.get(0).get(1).toLowerCase(), "action is not correct");
    }

    public void validateauditlog(String auditlog_response,CustomAssert csAssert){
        String audit_log_date_created =(String)((LinkedHashMap)((JSONArray)JSONUtility.parseJson(auditlog_response,"$.data[0].[*][?(@.columnName=='audit_log_date_created')]")).get(0)).get("value");
        String audit_log_user_date =(String)((LinkedHashMap)((JSONArray)JSONUtility.parseJson(auditlog_response,"$.data[0].[*][?(@.columnName=='audit_log_user_date')]")).get(0)).get("value");

        String expectedDate = DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy",timeZone);

       csAssert.assertTrue(audit_log_date_created.contains(expectedDate),"audit_log_date_created is not correct");
       csAssert.assertTrue(audit_log_user_date.contains(expectedDate),"audit_log_user_date is not correct");

    }

}



