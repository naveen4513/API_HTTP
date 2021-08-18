package com.sirionlabs.test.delegation;

import com.sirionlabs.api.commonAPI.Actions;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.delegation.CreateDelegation;
import com.sirionlabs.api.delegation.DeleteDelegation;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.dbHelper.AppUserDbHelper;
import com.sirionlabs.helper.entityWorkflowAction.EntityWorkflowActionHelper;
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

public class TestDelegationAccessTransfer {

    private final static Logger logger = LoggerFactory.getLogger(TestDelegationAccessTransfer.class);


    private int listId = 488;
    private int delegationEntityTypeId ;
    private String TestDelegationCreateConfigFilePath;
    private String TestDelegationCreateConfigFileName;
    private String payload;

    private String delegatedusername;
    private String delegateduser;
    private String delegateduserpassword;
    private String sourceusername;
    private String sourceuser;
    private String sourcepassword;


    private ListRendererDefaultUserListMetaData metaData = new ListRendererDefaultUserListMetaData();
    private ListRendererListData listData = new ListRendererListData();
    private Show show = new Show();
    private CreateDelegation createDelegation = new CreateDelegation();
    private AdminHelper admin = new AdminHelper();
    private DeleteDelegation delete = new DeleteDelegation();
    private Check check = new Check();
    private EntityWorkflowActionHelper entityWorkflowActionHelper = new EntityWorkflowActionHelper();

    private int delegationByMeListId = 336;
    private int delegationToMeListId = 335;

    @BeforeClass
    public void beforeClass(){

        delegationEntityTypeId = ConfigureConstantFields.getEntityIdByName("delegation");
        TestDelegationCreateConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestDelegationCreateConfigFilePath");
        TestDelegationCreateConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("TestDelegationAccessTransferConfigFileName");
        payload = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName, "payload");

        delegateduser = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName, "delegateduser");
        delegatedusername = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName, "delegatedusername");
        delegateduserpassword = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName, "delegateduserpassword");


        sourceuser = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName, "sourceuser");
        sourceusername = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName, "sourceusername");
        sourcepassword = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName, "sourcepassword");

    }



    @DataProvider()
    public Object[][] dataProviderForDelegation() {
        List<Object[]> allTestData = new ArrayList<>();


        String[] flows = {"flow1"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }


    @Test(dataProvider = "dataProviderForDelegation",description = "C8496,C8506,C8507,C8511,C8518")
    public void TestDelegationAccessTransfer(String flow) throws Exception {
        CustomAssert csAssert = new CustomAssert();
        String expected_status =  ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName, flow, "expectedstatus");
        String expected_message =  ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName, flow, "expectedmessage");
        String noofentitiestodelegate = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName, flow, "noofentitiestodelegate");
        if(noofentitiestodelegate == null || noofentitiestodelegate.equals(""))
            noofentitiestodelegate = "1";
        ArrayList<ArrayList<String>> list = prerequisite(noofentitiestodelegate);

        //get actions available on entity by delegated User before delegation
        check.hitCheck(delegatedusername,delegateduserpassword);
        String beforedelegateActionResponse = Actions.getActionsV3Response(Integer.valueOf(list.get(0).get(1)),Integer.valueOf(list.get(0).get(0)));
        JSONArray beforedelegateActions = (JSONArray) JSONUtility.parseJson(beforedelegateActionResponse,"$.layoutActions[*].name");

        //delegate entity to the delegated user
        check.hitCheck(sourceusername,sourcepassword);
        String createPayload = createPayload(flow,list,noofentitiestodelegate);
        createDelegation.hitCreateDelegationV2(createPayload);
        String createResponse = createDelegation.getcreateDelegationJsonStr();
        String status = (String) JSONUtility.parseJson(createResponse,"$.header.response.status");


        if(status.equals(expected_status) && status.equals("success")){
            String message = (String) JSONUtility.parseJson(createResponse,"$.header.response.properties.notification");
            csAssert.assertEquals(message,expected_message,"expected message: "+expected_message+" is not same as " +
                    "actual message: "+message+" for flow "+flow);

            //validate delegation by me and delegation to me Tab
            validateDelegationByMe(flow, list,noofentitiestodelegate,csAssert);
            validateDelegationToMe(flow, list,noofentitiestodelegate,csAssert);

            //getting action available on source user for entity
            String sourceActionResponse = Actions.getActionsV3Response(Integer.valueOf(list.get(0).get(1)),Integer.valueOf(list.get(0).get(0)));
            JSONArray sourceActions = (JSONArray) JSONUtility.parseJson(sourceActionResponse,"$.layoutActions[*].name");

            //getting actions available on entity by delegated User after delegation
            check.hitCheck(delegatedusername,delegateduserpassword);
            String delegateActionResponse = Actions.getActionsV3Response(Integer.valueOf(list.get(0).get(1)),Integer.valueOf(list.get(0).get(0)));
            JSONArray delegateActions = (JSONArray) JSONUtility.parseJson(delegateActionResponse,"$.layoutActions[*].name");

            //compare action on source and delegate user after delegation
            if(sourceActions.size()== delegateActions.size()){
                for (int i = 0; i <sourceActions.size() ; i++) {
                    csAssert.assertEquals(sourceActions.get(i),delegateActions.get(i),
                            "delegated User:"+delegateActions.get(i)+" do not have  access on entity: "+ list.get(0).get(0)+ " entityType: "+ list.get(0).get(1)
                                    + "as sourceUser" + sourceActions.get(i));
                }
            }else{
                csAssert.assertFalse(true,"delegated User:"+delegateActions.toString()+" do not have all access on entity: "+ list.get(0).get(0)+ " entityType: "+ list.get(0).get(1)
                                    + "as sourceUser" + sourceActions.toString());
            }

           /* //perform action on entity by delegated user
            if(delegateActions.toString().contains("Archive")){
            entityWorkflowActionHelper.hitWorkflowAction(ConfigureConstantFields.getEntityNameById(Integer.parseInt(list.get(0).get(1))),
                    Integer.valueOf(list.get(0).get(1)), Integer.valueOf(list.get(0).get(0)), "Archive");
                String workflowStatus =  (String)JSONUtility.parseJson(entityWorkflowActionHelper.getResponse(),"$.header.response.status");
                    if(workflowStatus.equals("success")){

                    }else{

                    }
            }*/

            //delete the delegation
            check.hitCheck(sourceusername,sourcepassword);
            List<String> delegationId = getDelegationID(list.get(0).get(0), list.get(0).get(1));
            delete.hitDeleteDelegation( delete.getCreatePayload(delegationId));
            String result = (String) JSONUtility.parseJson(delete.getcreateDelegationJsonStr(),"$.message");
            csAssert.assertEquals(result,"Delegation Deleted Successfully","delegation is not deleted successfully");

                if(result.equals("Delegation Deleted Successfully")){
                    //getting actions available on entity by delegated User after deletion
                    check.hitCheck(delegatedusername,delegateduserpassword);
                    String ActionResponseAfterDeletion = Actions.getActionsV3Response(Integer.valueOf(list.get(0).get(1)),Integer.valueOf(list.get(0).get(0)));
                    JSONArray ActionsAfterDeletion = (JSONArray) JSONUtility.parseJson(ActionResponseAfterDeletion,"$.layoutActions[*].name");

                    //compare action for delegate user before delegation and after deletion
                    if(beforedelegateActions.size()== ActionsAfterDeletion.size()){
                        for (int i = 0; i <beforedelegateActions.size() ; i++) {

                            csAssert.assertEquals(ActionsAfterDeletion.get(i),beforedelegateActions.get(i),
                                    "delegated user: "+ delegateduser +" do not have same access: "+ActionsAfterDeletion.toString()
                                            +" after delegation deletion as before: "+beforedelegateActions.toString()  );
                        }

                    }else{
                        csAssert.assertFalse(true,"delegated User:"+ delegateduser +"access is not removed after deletion of delegation of");
                    }

                }

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
                    String expectedEntityStatus = (String) JSONUtility.parseJson(show.getShowJsonStr(),"$.body.data.status.values.name");
                    String expectedDomainUser = delegateduser;
                    String expectedStartDate = getDate(ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName,
                            flow, "startdatevalue")).split(" ")[0];
                    String expectedEndDate  = getDate(ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName,
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
                            "Start Date is not correct in delegated by me tab for User: "+ sourceusername+" and EntityId"+ entityId
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
                    String expectedEntityStatus = (String) JSONUtility.parseJson(show.getShowJsonStr(),"$.body.data.status.values.name");
                    String expectedDomainUser = sourceuser;
                    String expectedStartDate = getDate(ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName,
                            flow, "startdatevalue")).split(" ")[0];
                    String expectedEndDate  = getDate(ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName,
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
                            "Start Date is not correct in delegated by me tab for User: "+ sourceusername+" and EntityId"+ entityId
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

    private String getdelegationListData( int listId){
        String listPayload = "{\"filterMap\":{\"entityTypeId\":"+listId+",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
        listData.hitListRendererListDataV2WithOutParams(String.valueOf(listId),listPayload);
        return   listData.getListDataJsonStr();
    }


    private JSONArray getValueFromColumnId(String listDataResponse , int columnId) {
        JSONArray idArray = (JSONArray) JSONUtility.parseJson(listData.getListDataJsonStr(), "$.data[*]['" + columnId + "'].value");
        return idArray;
    }


    private String createPayload(String flow,  ArrayList<ArrayList<String>> entityList, String noofentitiestodelegate) throws ParseException {
        String createPayload = "";
        HashMap<String, String> valueMap = new HashMap<>();
        String delegationrequesttemp = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName, "delegationrequest");
        String delegationrequests = "";
        for (int i = 0; i <Integer.parseInt(noofentitiestodelegate) ; i++) {
            HashMap<String, String> map = new HashMap<>();
            map.put("entityIdValue", entityList.get(i).get(0));
            map.put("entityTypeIdValue", entityList.get(i).get(1));
            map.put("roleGroupIdValue", entityList.get(i).get(2));
            String delegationrequest =  StringUtils.strSubstitutor(delegationrequesttemp, map);
            delegationrequests = delegationrequests + delegationrequest+ ",";


        }
        delegationrequests = delegationrequests.substring(0, delegationrequests.length()-1);

        String toUserName = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName, flow, "tousername");
        String toUserId = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName, flow, "touserid");
        String endDateValue = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName, flow, "enddatevalue");
        String startDateValue = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName, flow, "startdatevalue");
        String entitytypeidvalue = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName, flow, "entitytypeidvalue");
        String entityidvalue = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName, flow, "entityidvalue");
        String rolegroupidvalue = ParseConfigFile.getValueFromConfigFile(TestDelegationCreateConfigFilePath, TestDelegationCreateConfigFileName, flow, "rolegroupidvalue");

        if (startDateValue.equals("")) valueMap.put("startDateValue", "");
        else valueMap.put("startDateValue", getDate(startDateValue));

        if (endDateValue.equals("")) valueMap.put("endDateValue", "");
        else valueMap.put("endDateValue", getDate(endDateValue));

        valueMap.put("toUserId", toUserId);
        valueMap.put("toUserName", toUserName);

        valueMap.put("delegationRequests",delegationrequests);

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


    private  ArrayList<ArrayList<String>>  prerequisite(String noofentitiestodelegate){
        String entityDbID="", entityTypeID="", roleGroupId = "";
        ArrayList<ArrayList<String>> entityList =  new ArrayList<ArrayList<String>>();
        int columnId = getcolumnIdFromListRendererDefaultUserListMetaData("delegationentity","id",listId);
        int roleGroupColumnId = getcolumnIdFromListRendererDefaultUserListMetaData("rolegroupname","id",listId);
      // int delegatedUserColumnId = getcolumnIdFromListRendererDefaultUserListMetaData("delegateduser","id",listId);
       // int startDateColumnId = getcolumnIdFromListRendererDefaultUserListMetaData("startdate","id",listId);
        String listPayload = "{\"filterMap\":{\"entityTypeId\":314,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
        listData.hitListRendererListData(listId,listPayload);
        JSONArray idArray = getcolumnValueFromListData(listData.getListDataJsonStr(),columnId);
        JSONArray roleGroupArray = getcolumnValueFromListData(listData.getListDataJsonStr(),roleGroupColumnId);
        //JSONArray delegatedUserArray = getcolumnValueFromListData(delegatedUserColumnId);
        //JSONArray startDateArray = getcolumnValueFromListData(startDateColumnId);
        List<String> userType = AppUserDbHelper.getUserDataFromUserLoginId("type", sourceusername, admin.getClientIdFromDB());
        int count =0;
        for (int j = 0; j < Integer.valueOf(noofentitiestodelegate) ; j++) {
            for (int i = count; i <idArray.size() ; i++) {
                count++;
                ArrayList<String> list = new ArrayList<>();
                entityDbID = idArray.get(i).toString().split(":;")[1];
                entityTypeID = idArray.get(i).toString().split(":;")[0];
             //   String delegatedUser = (String) delegatedUserArray.get(i);
              //  String startDate = (String) startDateArray.get(i);
                roleGroupId  = getRoleGroupIdFromShowPage(entityTypeID,entityDbID,String.valueOf(roleGroupArray.get(i)));
                List<String> userTypeinRG = getUserTypeOfRoleGroup(roleGroupId);

                if(userTypeinRG.toString().contains(userType.get(0))){//&& delegatedUser ==null && startDate == null){
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
        params.put("entityTypeId", String.valueOf(delegationEntityTypeId));
        metaData.hitListRendererDefaultUserListMetadata(listId,params);
        metaData.getListRendererDefaultUserListMetaDataJsonStr();
        columnId =  (Integer) ((JSONArray) JSONUtility.parseJson(metaData.getListRendererDefaultUserListMetaDataJsonStr(),
                "$.columns[?(@.queryName=='"+columnQueryName+"')]."+param)).get(0);
        return  columnId ;
    }


    private JSONArray getcolumnValueFromListData(String listDataResponse, int columnId){
        JSONArray idArray = null;
        idArray = (JSONArray) JSONUtility.parseJson(listDataResponse,"$.data[*]['"+columnId+"'].value");
        return idArray;
    }

    private String getRoleGroupIdFromShowPage(String entityTypeID, String entityDbID, String roleGroupLabel){
        roleGroupLabel = getdisplaynameOfRoleGroup(roleGroupLabel).get(0);
        show.hitShowGetAPI(Integer.valueOf(entityTypeID),Integer.valueOf(entityDbID));
        show.getShowJsonStr();
        String roleGroupId = "";
        roleGroupId = (String)((JSONArray) JSONUtility.parseJson(show.getShowJsonStr(),
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
