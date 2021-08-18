package com.sirionlabs.test.delegation;

import com.mongodb.util.JSON;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.delegation.CreateDelegation;
import com.sirionlabs.api.delegation.DeleteDelegation;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.dbHelper.AppUserDbHelper;
import com.sirionlabs.utils.commonUtils.*;
import net.minidev.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TestDelegationCreateApi {
    private final static Logger logger = LoggerFactory.getLogger(TestDelegationCreateApi.class);

    private int listId = 488;
    private int delegationEntityTypeId ;
    private String TestDelegationCreateConfigFilePath;
    private String TestDelegationCreateConfigFileName;
    private String payload;


    private  ListRendererDefaultUserListMetaData metaData = new ListRendererDefaultUserListMetaData();
    private ListRendererListData listData = new ListRendererListData();
    private Show show = new Show();
    CreateDelegation createDelegation = new CreateDelegation();
    AdminHelper admin = new AdminHelper();
    DeleteDelegation delete = new DeleteDelegation();




    @BeforeClass
    public void beforeClass(){

        delegationEntityTypeId = ConfigureConstantFields.getEntityIdByName("delegation");
        TestDelegationCreateConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestDelegationCreateConfigFilePath");
        TestDelegationCreateConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("TestDelegationCreateConfigFileName");
        payload = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName, "payload");
    }

    @DataProvider()
    public Object[][] dataProviderForDelegation() {
        List<Object[]> allTestData = new ArrayList<>();

        String[] flows = {"flow1","flow2","flow3","flow4","flow5","flow6","flow7","flow8"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForDelegation",description = "C141035,C141054")
    public void TestCreateV2(String flow) throws Exception {
        CustomAssert csAssert = new CustomAssert();
        String expected_status =  ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName, flow, "expectedstatus");
        String expected_message =  ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName, flow, "expectedmessage");

        ArrayList<String> list = prerequisite();
        String createPayload = createPayload(flow,list);
        createDelegation.hitCreateDelegationV2(createPayload);
        String createResponse = createDelegation.getcreateDelegationJsonStr();
        String status = (String)JSONUtility.parseJson(createResponse,"$.header.response.status");

        if(status.equals(expected_status) && status.equals("success")){
            String message = (String)JSONUtility.parseJson(createResponse,"$.header.response.properties.notification");
            csAssert.assertEquals(message,expected_message,"expected message: "+expected_message+" is not same as " +
                    "actual message: "+message+" for flow "+flow);

            List<String> delegationId = getDelegationID(list.get(0), list.get(1));
            delete.hitDeleteDelegation( delete.getCreatePayload(delegationId));
            String result = (String)JSONUtility.parseJson(delete.getcreateDelegationJsonStr(),"$.message");
            csAssert.assertEquals(result,"Delegation Deleted Successfully","delegation is not deleted successfully");

            List<String> deleteInDB = validateDeleteInDB(delegationId.get(0));
            csAssert.assertEquals(deleteInDB.get(0),"t","delete column in delegated_domain_user is not " +
                    "marked as true after deletion of delegationId"+ delegationId.get(0));





        }
        else if(status.equals(expected_status) && status.equals("applicationError")){
            String message = (String) JSONUtility.parseJson(createResponse,"$.header.response.errorMessage");
            csAssert.assertTrue(message.contains(expected_message),"expected message: "+expected_message+" is not same as " +
                    "actual message: "+message+" for flow "+flow);

        }else{
            csAssert.assertTrue(false , "expected status: "+expected_status+" is not same as " +
                    "actual status: "+status+" for flow "+flow);
        }



            csAssert.assertAll();

    }

    private String createPayload(String flow,  ArrayList<String> entityList) throws ParseException {
        String createPayload = "";
        HashMap<String, String> valueMap = new HashMap<>();
        valueMap.put("entityIdValue", entityList.get(0));
        valueMap.put("entityTypeIdValue", entityList.get(1));
        valueMap.put("roleGroupIdValue", entityList.get(2));
        String toUserName = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName, flow, "tousername");
        String toUserId = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName, flow, "touserid");
        String endDateValue = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName, flow, "enddatevalue");
        String startDateValue = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName, flow, "startdatevalue");
        String entitytypeidvalue = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName, flow, "entitytypeidvalue");
        String entityidvalue = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName, flow, "entityidvalue");
        String rolegroupidvalue = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName, flow, "rolegroupidvalue");

        System.out.println("toUserName "+toUserName+" toUserId "+toUserId+" endDateValue "+endDateValue
        +" startDateValue "+startDateValue+" entitytypeidvalue "+entitytypeidvalue+" entityidvalue "+
                entityidvalue+" rolegroupidvalue "+rolegroupidvalue);

        System.out.println(" valueMap "+valueMap);

        if (startDateValue.equals("")) valueMap.put("startDateValue", "");
        else valueMap.put("startDateValue", getDate(startDateValue));

        if (endDateValue.equals("")) valueMap.put("endDateValue", "");
        else valueMap.put("endDateValue", getDate(endDateValue));

        valueMap.put("toUserId", toUserId);
        valueMap.put("toUserName", toUserName);

        System.out.println(" Finalsrc/test/java/com/sirionlabs/test/delegation/TestDelegationCreateApi.javavalueMap "+valueMap);
        createPayload = StringUtils.strSubstitutor(payload, valueMap);

        if (toUserId.equals("null") && toUserName.equals("null")) {

            JSONObject payloadObj  = new JSONObject(createPayload);
            JSONObject body =  payloadObj.getJSONObject("body");
            JSONObject data = body.getJSONObject("data");
            JSONObject users = data.getJSONObject("users");
            users.put("values", JSONObject.NULL);
            data.put("users", users);
            body.put("data",data);
            payloadObj.put("body",body);
            createPayload = payloadObj.toString();

        }

        if(entitytypeidvalue.equals("null") && entityidvalue.equals("null") && rolegroupidvalue.equals("null")){
            JSONObject payloadObj  = new JSONObject(createPayload);
            JSONObject body =  payloadObj.getJSONObject("body");
            JSONObject data = body.getJSONObject("data");
            data.put("delegationRequests", new org.json.JSONArray());
            body.put("data",data);
            payloadObj.put("body",body);
            createPayload = payloadObj.toString();

        }

        if(!entitytypeidvalue.equals("null") && !entityidvalue.equals("null") && rolegroupidvalue.equals("null")){
            JSONObject payloadObj  = new JSONObject(createPayload);
            JSONObject body =  payloadObj.getJSONObject("body");
            JSONObject data = body.getJSONObject("data");
            org.json.JSONArray delegationRequests = data.getJSONArray("delegationRequests");
            JSONObject request = (JSONObject) delegationRequests.get(0);
            request.remove("roleGroupId");
            data.put("delegationRequests", delegationRequests);
            body.put("data",data);
            payloadObj.put("body",body);
            createPayload = payloadObj.toString();

        }


        return  createPayload;
    }
    private String getDate(String noofDays) throws ParseException {
        String date = "";
        date = DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy 00:00:00"),Integer.valueOf(noofDays),"MM-dd-yyyy 00:00:00");
        return  date;
    }


    private  ArrayList<String> prerequisite(){
        String entityDbID="", entityTypeID="", roleGroupId = "";
        ArrayList<String> list = new ArrayList<>();
        int columnId = getcolumnIdFromListRendererDefaultUserListMetaData("entityid","id");
        int roleGroupColumnId = getcolumnIdFromListRendererDefaultUserListMetaData("rolegroupname","id");
        String listPayload = "{\"filterMap\":{\"entityTypeId\":314,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
        listData.hitListRendererListData(listId,listPayload);
        JSONArray idArray = getcolumnValueFromListData(listData.getListDataJsonStr(),columnId);
        JSONArray roleGroupArray = getcolumnValueFromListData(listData.getListDataJsonStr(),roleGroupColumnId);
        List<String> userType = AppUserDbHelper.getUserDataFromUserLoginId("type", ConfigureEnvironment.getEnvironmentProperty("j_username"), admin.getClientIdFromDB());
        for (int i = 0; i <idArray.size() ; i++) {
            entityDbID = idArray.get(i).toString().split(":;")[1];
            entityTypeID = idArray.get(i).toString().split(":;")[2];
            roleGroupId  = getRoleGroupIdFromShowPage(entityTypeID,entityDbID,String.valueOf(roleGroupArray.get(i)));
            List<String> userTypeinRG = getUserTypeOfRoleGroup(roleGroupId);

            if(userTypeinRG.toString().contains(userType.get(0))){
                break;
            }

        }

        list.add(entityDbID);
        list.add(entityTypeID);
        list.add(roleGroupId);
        return  list;

    }


    private Integer getcolumnIdFromListRendererDefaultUserListMetaData(String columnQueryName , String param){
        Integer columnId = -1;
        HashMap<String,String> params = new HashMap<>();
        params.put("entityTypeId", String.valueOf(delegationEntityTypeId));
        metaData.hitListRendererDefaultUserListMetadata(listId,params);
        metaData.getListRendererDefaultUserListMetaDataJsonStr();
        columnId =  (Integer) ((JSONArray) JSONUtility.parseJson(metaData.getListRendererDefaultUserListMetaDataJsonStr(),
                "$.columns[?(@.queryName=='"+columnQueryName+"')]."+param)).get(0);
        return  columnId ;
    }


    private JSONArray getcolumnValueFromListData(String listDataResonse,int columnId){
        JSONArray idArray = null;
        idArray = (JSONArray) JSONUtility.parseJson(listDataResonse,"$.data[*]['"+columnId+"'].value");
        return idArray;
    }

    private String getRoleGroupIdFromShowPage(String entityTypeID, String entityDbID, String roleGroupLabel){
        roleGroupLabel = getdisplaynameOfRoleGroup(roleGroupLabel).get(0);
        show.hitShowGetAPI(Integer.valueOf(entityTypeID),Integer.valueOf(entityDbID));
        show.getShowJsonStr();
        String roleGroupId = "";
        roleGroupId = (String)((JSONArray)JSONUtility.parseJson(show.getShowJsonStr(),
                "$.body.data.stakeHolders.values[*][?(@.label=='"+roleGroupLabel+"')].name")).get(0)
                .toString().split("rg_")[1];

        return  roleGroupId;
    }

    public List<String> validateDeleteInDB( String delegationId){
        List<String> data = new ArrayList<>();
        String query="select deleted from delegated_domain_user  where id ="+delegationId;

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                data = results.get(0);
            }

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Getting client Data from DB using query [{}]. {}", query, e.getMessage());
        }

        return data;
    }

    public List<String> getUserTypeOfRoleGroup(String roleGroupId){
        List<String> data = new ArrayList<>();
        String query="select user_type from role_group where id = "+roleGroupId;

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                data = results.get(0);
            }

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Getting User Type of role group from DB using query [{}]. {}", query, e.getMessage());
        }

        return data;
    }

    private   List<String> getDelegationID(String entityId , String entityTypeId){
        List<String> data = new ArrayList<>();
        String query="select id from delegated_domain_user where entity_type_id ="+entityTypeId+" and entity_id = "+entityId+"  order by date_created desc limit 1;";

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                data = results.get(0);
            }

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Getting User Type of role group from DB using query [{}]. {}", query, e.getMessage());
        }

        return data;
    }

    public List<String> getdisplaynameOfRoleGroup(String roleGroupLabel){
        List<String> data = new ArrayList<>();
        String query="select description from role_group where LOWER(name)=LOWER('"+roleGroupLabel+"')";

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                data = results.get(0);
            }

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Getting description of role group from DB using query [{}]. {}", query, e.getMessage());
        }

        return data;
    }


}
