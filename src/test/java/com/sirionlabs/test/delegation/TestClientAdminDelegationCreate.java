package com.sirionlabs.test.delegation;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.delegation.CreateDelegation;
import com.sirionlabs.api.delegation.DeleteDelegation;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
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

public class TestClientAdminDelegationCreate {
    private final static Logger logger = LoggerFactory.getLogger(TestClientAdminDelegationCreate.class);

    private int listId = 334;
    private int delegationEntityTypeId ;
    private String TestDelegationCreateConfigFilePath;
    private String TestClientAdminDelegationCreateConfigFileName;
    private String payload;
    private String delegatedusername;
    private String delegateduser;
    private String delegateduserid;
    private String delegateduserpassword;
    private String sourceusername;
    private String sourceuser;
    private String sourceuserid;
    private String sourcepassword;
    private int delegationByMeListId = 336;
    private int delegationToMeListId = 335;


    private ListRendererDefaultUserListMetaData metaData = new ListRendererDefaultUserListMetaData();
    private ListRendererListData listData = new ListRendererListData();
    private Show show = new Show();
    CreateDelegation createDelegation = new CreateDelegation();
    AdminHelper admin = new AdminHelper();
    DeleteDelegation delete = new DeleteDelegation();
    Check check = new Check();



    @BeforeClass
    public void beforeClass(){

        delegationEntityTypeId = ConfigureConstantFields.getEntityIdByName("delegation");
        TestDelegationCreateConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestDelegationCreateConfigFilePath");
        TestClientAdminDelegationCreateConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("TestClientAdminDelegationCreateConfigFileName");
        payload = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestClientAdminDelegationCreateConfigFileName, "payload");

        delegateduser = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestClientAdminDelegationCreateConfigFileName, "delegateduser");
        delegatedusername = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestClientAdminDelegationCreateConfigFileName, "delegatedusername");
        delegateduserid = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestClientAdminDelegationCreateConfigFileName, "delegateduserid");
        delegateduserpassword = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestClientAdminDelegationCreateConfigFileName, "delegateduserpassword");


        sourceuser = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestClientAdminDelegationCreateConfigFileName, "sourceuser");
        sourceusername = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestClientAdminDelegationCreateConfigFileName, "sourceusername");
        sourceuserid = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestClientAdminDelegationCreateConfigFileName, "sourceuserid");
        sourcepassword = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestClientAdminDelegationCreateConfigFileName, "sourcepassword");

    }


    @DataProvider()
    public Object[][] dataProviderForDelegation() {
        List<Object[]> allTestData = new ArrayList<>();



        String[] flows = {"flow1","flow2","flow3","flow4"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForDelegation",description = "C141049,C8493,C8489")
    public void TestCreate(String flow) throws Exception {
        CustomAssert csAssert = new CustomAssert();
        admin.loginWithClientAdminUser();
        String expected_status = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestClientAdminDelegationCreateConfigFileName, flow, "expectedstatus");
        String expected_message = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestClientAdminDelegationCreateConfigFileName, flow, "expectedmessage");
        String noofentitiestodelegate = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestClientAdminDelegationCreateConfigFileName, flow, "noofentitiestodelegate");
        if(noofentitiestodelegate == null || noofentitiestodelegate.equals(""))
            noofentitiestodelegate = "1";

        ArrayList<ArrayList<String>> list =  prerequisite(noofentitiestodelegate);
        String createPayload = createPayload(flow,list,noofentitiestodelegate);
        createDelegation.hitCreateDelegation(createPayload);
        String createResponse = createDelegation.getcreateDelegationJsonStr();
        Boolean status = (Boolean)JSONUtility.parseJson(createResponse,"$.success");
        if(status.equals(Boolean.valueOf(expected_status)) && status.equals(true)){

            validateDelegationByMe(flow, list,noofentitiestodelegate,csAssert);
            validateDelegationToMe(flow, list,noofentitiestodelegate,csAssert);

            admin.loginWithClientAdminUser();
            for (int i = 0; i <Integer.valueOf(noofentitiestodelegate) ; i++) {
                List<String> delegationId = getDelegationID(list.get(i).get(0), list.get(i).get(1));
                delete.hitDeleteDelegation( delete.getClientAdminDeletePayload(delegationId,sourceuserid));
                String result = (String)JSONUtility.parseJson(delete.getcreateDelegationJsonStr(),"$.message");
                csAssert.assertEquals(result,"Delegation Deleted Successfully","delegation is not deleted successfully");

                List<String> deleteInDB = validateDeleteInDB(delegationId.get(0));
                csAssert.assertEquals(deleteInDB.get(0),"t","delete column in delegated_domain_user is not " +
                        "marked as true after deletion of delegationId"+ delegationId.get(0));

            }
        }
        else if(status.equals(Boolean.valueOf(expected_status)) && status.equals(false)){
            JSONArray messagelist = (JSONArray) JSONUtility.parseJson(createResponse,"$.errorMessages[*].errorMessage");
            String message = "";
            for (int i = 0; i <messagelist.size() ; i++) {
                message   = message +  messagelist.get(0)+" ,";
            }

            csAssert.assertEquals(message,expected_message,"expected message: "+expected_message+" is not same as " +
                    "actual message: "+message+" for flow "+flow);
        }
        else{
            csAssert.assertTrue(false , "expected status: "+expected_status+" is not same as " +
                    "actual status: "+status+" for flow "+flow);
        }

        csAssert.assertAll();
    }


    @Test(description = "C8490")
    public void TestMultipleDelegate() throws Exception {
        String flow="flow5";
        CustomAssert csAssert = new CustomAssert();
        admin.loginWithClientAdminUser();
        String expected_status = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestClientAdminDelegationCreateConfigFileName, flow, "expectedstatus");
        String expected_message = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestClientAdminDelegationCreateConfigFileName, flow, "expectedmessage");
        String noofentitiestodelegate = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestClientAdminDelegationCreateConfigFileName, flow, "noofentitiestodelegate");
        if(noofentitiestodelegate == null || noofentitiestodelegate.equals(""))
            noofentitiestodelegate = "1";

        ArrayList<ArrayList<String>> list =  prerequisite(noofentitiestodelegate);
        String createPayload = createPayload(flow,list,noofentitiestodelegate);
        createDelegation.hitCreateDelegation(createPayload);
        String createResponse = createDelegation.getcreateDelegationJsonStr();
        Boolean status = (Boolean)JSONUtility.parseJson(createResponse,"$.success");
        if(status.equals(Boolean.valueOf(expected_status)) && status.equals(true)){

            validateDelegationByMe(flow, list,noofentitiestodelegate,csAssert);
            validateDelegationToMe(flow, list,noofentitiestodelegate,csAssert);
            for (int i = 0; i <Integer.valueOf(noofentitiestodelegate) ; i++) {
                List<String> delegationId = getDelegationID(list.get(i).get(0), list.get(i).get(1));
                delete.hitDeleteDelegation( delete.getClientAdminDeletePayload(delegationId,sourceuserid));
                String result = (String)JSONUtility.parseJson(delete.getcreateDelegationJsonStr(),"$.message");
                csAssert.assertEquals(result,"Delegation Deleted Successfully","delegation is not deleted successfully");

                List<String> deleteInDB = validateDeleteInDB(delegationId.get(0));
                csAssert.assertEquals(deleteInDB.get(0),"t","delete column in delegated_domain_user is not " +
                        "marked as true after deletion of delegationId"+ delegationId.get(0));

            }

        }

        else if(status.equals(Boolean.valueOf(expected_status)) && status.equals(false)){
            JSONArray messagelist = (JSONArray) JSONUtility.parseJson(createResponse,"$.errorMessages[*].errorMessage");
            String message = "";
            for (int i = 0; i <messagelist.size() ; i++) {
                message   = message +  messagelist.get(0)+" ,";
            }

            csAssert.assertEquals(message,expected_message,"expected message: "+expected_message+" is not same as " +
                    "actual message: "+message+" for flow "+flow);
        }
        else{
            csAssert.assertTrue(false , "expected status: "+expected_status+" is not same as " +
                    "actual status: "+status+" for flow "+flow);
        }

        csAssert.assertAll();
    }


    private void validateDelegationByMe(String flow,ArrayList<ArrayList<String>> list,String noofentitiestodelegate, CustomAssert csAssert) throws ParseException {
        check.hitCheck(sourceusername,sourcepassword);
        int entityidColumnId = getcolumnIdFromListRendererDefaultUserListMetaData("entityid","id",delegationByMeListId);
        int entitystatusColumnId = getcolumnIdFromListRendererDefaultUserListMetaData("entitystatus","id",delegationByMeListId);
        int rolegroupnameColumnId = getcolumnIdFromListRendererDefaultUserListMetaData("rolegroupname","id",delegationByMeListId);
        int domainuserId = getcolumnIdFromListRendererDefaultUserListMetaData("domainuser","id",delegationByMeListId);
        int startdateColumnId = getcolumnIdFromListRendererDefaultUserListMetaData("startdate","id",delegationByMeListId);
        int enddateColumnId = getcolumnIdFromListRendererDefaultUserListMetaData("enddate","id",delegationByMeListId);
        int delegationstatusColumnId = getcolumnIdFromListRendererDefaultUserListMetaData("delegationstatus","id",delegationByMeListId);


        String byMeResponse =  getdelegationListData(delegationByMeListId);
        JSONArray entityIds = getValueFromColumnId(byMeResponse, entityidColumnId);

        for (int i = 0; i < Integer.parseInt(noofentitiestodelegate) ; i++) {

            for (int j=0; j < entityIds.size();j++ ){
                String entityId =  (String)entityIds.get(j).toString().split(":;")[1];
                if(entityId.equals(list.get(i).get(0))){

                    String actualEntityTypeId = (String)entityIds.get(j).toString().split(":;")[2];
                    String acutalEntityStatus = (String)getValueFromColumnId(byMeResponse, entitystatusColumnId).get(j);
                    String acutalRoleGroupName = (String)getValueFromColumnId(byMeResponse, rolegroupnameColumnId).get(j);
                    String acutalDomainUser = (String)getValueFromColumnId(byMeResponse,  domainuserId).get(j);
                    String acutalStartDate = (String)getValueFromColumnId(byMeResponse,  startdateColumnId).get(j);
                    String acutalEndDate = (String)getValueFromColumnId(byMeResponse,  enddateColumnId).get(j);
                    String acutalDelegationStatus = (String)getValueFromColumnId(byMeResponse,  delegationstatusColumnId).get(j);

                    show.hitShowVersion2(Integer.parseInt(actualEntityTypeId), Integer.parseInt(entityId));
                    String expectedEntityStatus="";
                    if(Integer.parseInt(actualEntityTypeId)==15)
                        expectedEntityStatus = (String)JSONUtility.parseJson(show.getShowJsonStr(),"$.body.data.performanceStatus.values.name");
                    else
                        expectedEntityStatus = (String)JSONUtility.parseJson(show.getShowJsonStr(),"$.body.data.status.values.name");
                    String expectedDomainUser = delegateduser;
                    String expectedStartDate = getStartDate(ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestClientAdminDelegationCreateConfigFileName,
                            flow, "startdatevalue")).split(" ")[0];
                    String expectedEndDate  = getEndDate(ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestClientAdminDelegationCreateConfigFileName,
                            flow, "enddatevalue")).split(" ")[0];
                    String expectedDelegationStatus = "active";
                    String expectedRoleGroupName = list.get(i).get(3);
                    String expectedEntityTypeId = list.get(i).get(1);

                    csAssert.assertEquals(actualEntityTypeId,expectedEntityTypeId,
                            "entityTypeId is not correct in delegated by me tab for User: "+ sourceusername+" and EntityId"+ entityId
                                    + " entityTypeId "+ actualEntityTypeId);

                    csAssert.assertEquals(acutalRoleGroupName,expectedRoleGroupName,
                            "Role Group Name is not correct in delegated by me tab for User: "+ sourceusername+" and EntityId"+ entityId
                                    + " entityTypeId "+ actualEntityTypeId);

                    csAssert.assertTrue(acutalDelegationStatus.toLowerCase().contains(expectedDelegationStatus),
                            "delegation status is not correct in delegated by me tab for User: "+ sourceusername+" and EntityId"+ entityId
                                    + " entityTypeId "+ actualEntityTypeId);

                    csAssert.assertEquals(acutalEntityStatus,expectedEntityStatus,
                            "Entity status is not correct in delegated by me tab for User: "+ sourceusername+" and EntityId"+ entityId
                                    + " entityTypeId "+ actualEntityTypeId);

                    csAssert.assertEquals(expectedDomainUser,acutalDomainUser,
                            "Delegated To username is not correct in delegated by me tab for User: "+ sourceusername+" and EntityId"+ entityId
                                    + " entityTypeId "+ actualEntityTypeId);

                    csAssert.assertEquals(DateUtils.converDateToAnyFormat(acutalStartDate,"MMM-dd-yyyy","MM-dd-yyyy"),expectedStartDate,
                            "Start Date is not correct in delegated by me tab for User: "+ sourceusername+" and EntityId"+ entityId
                                    + " entityTypeId "+ actualEntityTypeId);

                    csAssert.assertEquals(DateUtils.converDateToAnyFormat(acutalEndDate,"MMM-dd-yyyy","MM-dd-yyyy"),expectedEndDate,
                            "End Date is not correct in delegated by me tab for User: "+ sourceusername+" and EntityId"+ entityId
                                    + " entityTypeId "+ actualEntityTypeId);



                    break;
                }

                if(j== (entityIds.size()-1) && !entityId.equals(list.get(i).get(0))){
                    csAssert.assertFalse(true,list.get(i).get(0)+" is not present in the delegated by me tab " +
                            "of user: " + sourceusername);
                }


            }
        }

    }

    private void validateDelegationToMe(String flow,ArrayList<ArrayList<String>> list,String noofentitiestodelegate, CustomAssert csAssert) throws ParseException {
        check.hitCheck(delegatedusername,delegateduserpassword);
        int entityidColumnId = getcolumnIdFromListRendererDefaultUserListMetaData("entityid","id",delegationToMeListId);
        int entitystatusColumnId = getcolumnIdFromListRendererDefaultUserListMetaData("entitystatus","id",delegationToMeListId);
        int rolegroupnameColumnId = getcolumnIdFromListRendererDefaultUserListMetaData("rolegroupname","id",delegationToMeListId);
        int domainuserId = getcolumnIdFromListRendererDefaultUserListMetaData("domainuser","id",delegationToMeListId);
        int startdateColumnId = getcolumnIdFromListRendererDefaultUserListMetaData("startdate","id",delegationToMeListId);
        int enddateColumnId = getcolumnIdFromListRendererDefaultUserListMetaData("enddate","id",delegationToMeListId);
        int delegationstatusColumnId = getcolumnIdFromListRendererDefaultUserListMetaData("delegationstatus","id",delegationToMeListId);


        String toMeResponse =  getdelegationListData(delegationToMeListId);
        JSONArray entityIds = getValueFromColumnId(toMeResponse, entityidColumnId);

        for (int i = 0; i < Integer.parseInt(noofentitiestodelegate) ; i++) {

            for (int j=0; j < entityIds.size();j++ ){
                String entityId =  (String)entityIds.get(j).toString().split(":;")[1];
                if(entityId.equals(list.get(i).get(0))){

                    String actualEntityTypeId = (String)entityIds.get(j).toString().split(":;")[2];
                    String acutalEntityStatus = (String)getValueFromColumnId(toMeResponse, entitystatusColumnId).get(j);
                    String acutalRoleGroupName = (String)getValueFromColumnId(toMeResponse, rolegroupnameColumnId).get(j);
                    String acutalDomainUser = (String)getValueFromColumnId(toMeResponse,  domainuserId).get(j);
                    String acutalStartDate = (String)getValueFromColumnId(toMeResponse,  startdateColumnId).get(j);
                    String acutalEndDate = (String)getValueFromColumnId(toMeResponse,  enddateColumnId).get(j);
                    String acutalDelegationStatus = (String)getValueFromColumnId(toMeResponse,  delegationstatusColumnId).get(j);

                    show.hitShowVersion2(Integer.parseInt(actualEntityTypeId), Integer.parseInt(entityId));
                    String expectedEntityStatus="";
                    if(Integer.parseInt(actualEntityTypeId)==15)
                        expectedEntityStatus = (String)JSONUtility.parseJson(show.getShowJsonStr(),"$.body.data.performanceStatus.values.name");
                    else
                        expectedEntityStatus = (String)JSONUtility.parseJson(show.getShowJsonStr(),"$.body.data.status.values.name");
                    String expectedDomainUser = sourceuser;
                    String expectedStartDate = getStartDate(ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestClientAdminDelegationCreateConfigFileName,
                            flow, "startdatevalue")).split(" ")[0];
                    String expectedEndDate  = getEndDate(ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestClientAdminDelegationCreateConfigFileName,
                            flow, "enddatevalue")).split(" ")[0];
                    String expectedDelegationStatus = "active";
                    String expectedRoleGroupName = list.get(i).get(3);
                    String expectedEntityTypeId = list.get(i).get(1);

                    csAssert.assertEquals(actualEntityTypeId,expectedEntityTypeId,
                            "entityTypeId is not correct in delegated to me tab for User: "+ sourceusername+" and EntityId"+ entityId
                                    + " entityTypeId "+ actualEntityTypeId);

                    csAssert.assertEquals(acutalRoleGroupName,expectedRoleGroupName,
                            "Role Group Name is not correct in delegated to me tab for User: "+ sourceusername+" and EntityId"+ entityId
                                    + " entityTypeId "+ actualEntityTypeId);

                    csAssert.assertTrue(acutalDelegationStatus.toLowerCase().contains(expectedDelegationStatus),
                            "delegation status is not correct in delegated to me tab for User: "+ sourceusername+" and EntityId"+ entityId
                                    + " entityTypeId "+ actualEntityTypeId);

                    csAssert.assertEquals(acutalEntityStatus,expectedEntityStatus,
                            "Entity status is not correct in delegated to me tab for User: "+ sourceusername+" and EntityId"+ entityId
                                    + " entityTypeId "+ actualEntityTypeId);

                    csAssert.assertEquals(expectedDomainUser,acutalDomainUser,
                            "Delegated To username is not correct in delegated to me tab for User: "+ sourceusername+" and EntityId"+ entityId
                                    + " entityTypeId "+ actualEntityTypeId);

                    csAssert.assertEquals(DateUtils.converDateToAnyFormat(acutalStartDate,"MMM-dd-yyyy","MM-dd-yyyy"),expectedStartDate,
                            "Start Date is not correct in delegated by to tab for User: "+ sourceusername+" and EntityId"+ entityId
                                    + " entityTypeId "+ actualEntityTypeId);

                    csAssert.assertEquals(DateUtils.converDateToAnyFormat(acutalEndDate,"MMM-dd-yyyy","MM-dd-yyyy"),expectedEndDate,
                            "End Date is not correct in delegated by to tab for User: "+ sourceusername+" and EntityId"+ entityId
                                    + " entityTypeId "+ actualEntityTypeId);



                    break;
                }

                if(j== (entityIds.size()-1) && !entityId.equals(list.get(i).get(0))){
                    csAssert.assertFalse(true,list.get(i).get(0)+" is not present in the delegated to me tab " +
                            "of user: " + sourceusername);
                }


            }
        }

    }

    private String createPayload(String flow,  ArrayList<ArrayList<String>> entityList, String noofentitiestodelegate) throws ParseException {
        String createPayload = "";
        HashMap<String, String> valueMap = new HashMap<>();
        String delegationrequesttemp = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestClientAdminDelegationCreateConfigFileName, flow, "delegationrequest");
        String delegationrequests = "";
        for (int i = 0; i <Integer.parseInt(noofentitiestodelegate) ; i++) {
            HashMap<String, String> map = new HashMap<>();
            map.put("entityId", entityList.get(i).get(0));
            map.put("entityTypeId", entityList.get(i).get(1));
            map.put("roleGroupId", entityList.get(i).get(2));
            String delegationrequest =  StringUtils.strSubstitutor(delegationrequesttemp, map);
            delegationrequests = delegationrequests + delegationrequest+ ",";


        }
        delegationrequests = delegationrequests.substring(0, delegationrequests.length()-1);

        String endDateValue = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestClientAdminDelegationCreateConfigFileName, flow, "enddatevalue");
        String startDateValue = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestClientAdminDelegationCreateConfigFileName, flow, "startdatevalue");
        String entitytypeidvalue = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestClientAdminDelegationCreateConfigFileName, flow, "entitytypeidvalue");
        String entityidvalue = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestClientAdminDelegationCreateConfigFileName, flow, "entityidvalue");
        String rolegroupidvalue = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestClientAdminDelegationCreateConfigFileName, flow, "rolegroupidvalue");
        String delegateduserid = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestClientAdminDelegationCreateConfigFileName, flow, "delegateduserid");
        String sourceuserid = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestClientAdminDelegationCreateConfigFileName, flow, "sourceuserid");

        if (startDateValue.equals("")) valueMap.put("startDate", "");
        else valueMap.put("startDate", getStartDate(startDateValue));

        if (endDateValue.equals("")) valueMap.put("endDate", "");
        else valueMap.put("endDate", getEndDate(endDateValue));

        if (delegateduserid.equals("")) valueMap.put("delegatedUser", "");
        else valueMap.put("delegatedUser", delegateduserid);

        if (sourceuserid.equals("")) valueMap.put("sourceUserId", "");
        else valueMap.put("sourceUserId", sourceuserid);

        valueMap.put("delegationRequests",delegationrequests);

        createPayload = StringUtils.strSubstitutor(payload, valueMap);



        if(!entitytypeidvalue.equals("null") && !entityidvalue.equals("null") && rolegroupidvalue.equals("null")){
            JSONObject payloadObj  = new JSONObject(createPayload);
            org.json.JSONArray delegationRequests = payloadObj.getJSONArray("delegationRequests");
            JSONObject request = (JSONObject) delegationRequests.get(0);
            request.remove("roleGroupId");
            payloadObj.put("delegationRequests", delegationRequests);
            createPayload = payloadObj.toString();

        }
        return  createPayload;
    }



    private  ArrayList<ArrayList<String>>  prerequisite(String noofentitiestodelegate){
        String entityDbID="", entityTypeID="", roleGroupId = "";
        ArrayList<ArrayList<String>> entityList =  new ArrayList<ArrayList<String>>();
        int columnId = getcolumnIdFromListRendererDefaultUserListMetaData("delegationentity","id",listId);
        int roleGroupColumnId = getcolumnIdFromListRendererDefaultUserListMetaData("rolegroupname","id",listId);
        int delegatedUserColumnId = getcolumnIdFromListRendererDefaultUserListMetaData("delegateduser","id",listId);
        int startDateColumnId = getcolumnIdFromListRendererDefaultUserListMetaData("startdate","id",listId);

        HashMap<String,String> params = new HashMap<>();
        params.put("domainUserId", String.valueOf(sourceuserid));
        String listPayload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":200,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{}}}";
        listData.hitListRendererListData(listId,listPayload,params);

        JSONArray idArray = getcolumnValueFromListData(listData.getListDataJsonStr(),columnId);
        JSONArray roleGroupArray = getcolumnValueFromListData(listData.getListDataJsonStr(),roleGroupColumnId);
        JSONArray delegatedUserArray = getcolumnValueFromListData(listData.getListDataJsonStr(),delegatedUserColumnId);
        JSONArray startDateArray = getcolumnValueFromListData(listData.getListDataJsonStr(),startDateColumnId);
        List<String> userType = AppUserDbHelper.getUserDataFromUserLoginId("type", sourceusername, admin.getClientIdFromDB());
        int count =0;
        for (int j = 0; j < Integer.valueOf(noofentitiestodelegate) ; j++) {
            for (int i = count; i <idArray.size() ; i++) {
                count++;
                ArrayList<String> list = new ArrayList<>();
                entityDbID = idArray.get(i).toString().split(":;")[1];
                entityTypeID = idArray.get(i).toString().split(":;")[0];
                String delegatedUser = (String) delegatedUserArray.get(i);
                String startDate = (String) startDateArray.get(i);
                roleGroupId  = getRoleGroupIdFromShowPage(entityTypeID,entityDbID,String.valueOf(roleGroupArray.get(i)));
                List<String> userTypeinRG = getUserTypeOfRoleGroup(roleGroupId);

                if(userTypeinRG.toString().contains(userType.get(0))&& delegatedUser ==null && startDate == null){
                    list.add(entityDbID);
                    list.add(entityTypeID);
                    list.add(roleGroupId);
                    list.add(String.valueOf(roleGroupArray.get(i)));
                    entityList.add(list);
                    break;
                }


            }
        }


        return  entityList;

    }

    private Integer getcolumnIdFromListRendererDefaultUserListMetaData(String columnQueryName , String param, int listId){
        Integer columnId = -1;
        HashMap<String,String> params = new HashMap<>();
        metaData.hitListRendererDefaultUserListMetadata(listId);
        columnId =  (Integer) ((JSONArray) JSONUtility.parseJson(metaData.getListRendererDefaultUserListMetaDataJsonStr(),
                "$.columns[?(@.queryName=='"+columnQueryName+"')]."+param)).get(0);
        return  columnId ;
    }

    private String getdelegationListData( int listId){
        String listPayload = "{\"filterMap\":{\"entityTypeId\":"+listId+",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
        listData.hitListRendererListDataV2WithOutParams(String.valueOf(listId),listPayload);
        return   listData.getListDataJsonStr();
    }

    private JSONArray getValueFromColumnId(String listDataResponse , int columnId) {
        JSONArray idArray = (JSONArray) JSONUtility.parseJson(listDataResponse, "$.data[*]['" + columnId + "'].value");
        return idArray;
    }

    private JSONArray getcolumnValueFromListData(String listDataResponse, int columnId){
        JSONArray idArray = null;

        idArray = (JSONArray) JSONUtility.parseJson(listData.getListDataJsonStr(),"$.data[*]['"+columnId+"'].value");
        return idArray;
    }


    private String getRoleGroupIdFromShowPage(String entityTypeID, String entityDbID, String roleGroupLabel){
        roleGroupLabel = getdisplaynameOfRoleGroup(roleGroupLabel).get(0);
        check.hitCheck(sourceusername,sourcepassword);
        show.hitShowGetAPI(Integer.valueOf(entityTypeID),Integer.valueOf(entityDbID));
        show.getShowJsonStr();
        String roleGroupId = "";
        roleGroupId = (String)((JSONArray)JSONUtility.parseJson(show.getShowJsonStr(),
                "$.body.data.stakeHolders.values[*][?(@.label=='"+roleGroupLabel+"')].name")).get(0)
                .toString().split("rg_")[1];
        admin.loginWithClientAdminUser();
        return  roleGroupId;

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


    private String getStartDate(String noofDays) throws ParseException {
        String date = "";
        date = DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy 00:00:00"),Integer.valueOf(noofDays),"MM-dd-yyyy 00:00:00");
        return  date;
    }

    private String getEndDate(String noofDays) throws ParseException {
        String date = "";
        date = DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy 23:59:59"),Integer.valueOf(noofDays),"MM-dd-yyyy 23:59:59");
        return  date;
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

}
