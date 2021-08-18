package com.sirionlabs.test.delegation;

import com.sirionlabs.api.clientAdmin.userConfiguration.UsersActivate;
import com.sirionlabs.api.clientAdmin.userConfiguration.UsersInactivate;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.delegation.CreateDelegation;
import com.sirionlabs.api.delegation.DelegationCreateForm;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.dbHelper.AppUserDbHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.Obligations;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class TestDelegationMisc {

    private final static Logger logger = LoggerFactory.getLogger(TestDelegationCreateApi.class);

    private static String configFilePath;
    private static String configFileName;
    private static String delegationConfigFilePath;
    private static String delegationConfigFileName;
    private static String extraFieldsConfigFilePath;
    private static String extraFieldsConfigFileName;
    private static Integer obligationEntityTypeId;
    private static Boolean deleteEntity = true;
    private String payload;
    private String sourceLoginId ;
    private String sourceLoginPassword ;
    private String sourceLoginDBId ;


    private AdminHelper adminHelper = new AdminHelper();
    private Check check = new Check();
    private Show show = new Show();
    private Edit edit = new Edit();
    private CreateDelegation createDelegation = new CreateDelegation();


    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        delegationConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestDelegationCreateConfigFilePath");
        delegationConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("TestDelegationMiscConfigFileName");
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("ObligationCreationTestConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("ObligationTestConfigFileName");
        extraFieldsConfigFilePath = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFilePath");
        extraFieldsConfigFileName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "extraFieldsConfigFileName");
        obligationEntityTypeId = ConfigureConstantFields.getEntityIdByName("obligations");
        payload = ParseConfigFile.getValueFromConfigFile(delegationConfigFilePath, delegationConfigFileName, "payload");
        sourceLoginId = ParseConfigFile.getValueFromConfigFile(delegationConfigFilePath, delegationConfigFileName, "sourceuserloginid");;
        sourceLoginPassword = ParseConfigFile.getValueFromConfigFile(delegationConfigFilePath, delegationConfigFileName, "sourceuserpassword");
        sourceLoginDBId = ParseConfigFile.getValueFromConfigFile(delegationConfigFilePath, delegationConfigFileName, "sourceuserdbid");

        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "deleteEntity");
        if (temp != null && temp.trim().equalsIgnoreCase("false"))
            deleteEntity = false;

    }

    @AfterClass
    public void afterClass() {
        String default_user = ConfigureEnvironment.getEnvironmentProperty("j_username");
        String default_password = ConfigureEnvironment.getEnvironmentProperty("password");
        check.hitCheck(default_user,default_password);
    }




     @Test(description = "C46254")
    public void globalPermissionsDelegation(){
        CustomAssert csAssert = new CustomAssert();
        String delegationPermisisonId = "898";
        String user = ConfigureEnvironment.getEnvironmentProperty("j_username");
        String password = ConfigureEnvironment.getEnvironmentProperty("password");

        Set<String> permissions = adminHelper.getAllPermissionsForUser(user, adminHelper.getClientIdFromDB());
        permissions.remove(delegationPermisisonId);
        String newPermissionsStr = permissions.toString().replace("[", "{").replace("]", "}");
        adminHelper.updatePermissionsForUser(user, adminHelper.getClientIdFromDB(),newPermissionsStr);

        check.hitCheck(user,password);

        String withoutPermissionResponse = DelegationCreateForm.getCreateFormV3Response().getResponseBody();

            int count=0;
          while (JSONUtility.validjson(withoutPermissionResponse)&&  (count <5) ){
              check.hitCheck(user,password);
              count++;
          }

          if(JSONUtility.validjson(withoutPermissionResponse)){
            csAssert.assertTrue(false,user+" has delegation create permission even after removal of permission "+delegationPermisisonId+" from app_user table");
        }

        Set<String> afterpermissions = adminHelper.getAllPermissionsForUser(user, adminHelper.getClientIdFromDB());
        afterpermissions.add(delegationPermisisonId);
        String oldPermission = afterpermissions.toString().replace("[", "{").replace("]", "}");
        adminHelper.updatePermissionsForUser(user, adminHelper.getClientIdFromDB(),oldPermission);
        check.hitCheck(user,password);

        String withPermissionresponse = DelegationCreateForm.getCreateFormV3Response().getResponseBody();

        if(!JSONUtility.validjson(withPermissionresponse)){
            csAssert.assertTrue(false,user+" do not have delegation create permission even addition of permission "+delegationPermisisonId+" from app_user table");

        }

        csAssert.assertAll();

    }



   @Test(description = "C46251")
    public void inactiveSourceUser() throws Exception {
        String flow = "C46251";
        CustomAssert csAssert = new CustomAssert();
        String user = sourceLoginId;
        String password = sourceLoginPassword;
        int userId =  Integer.parseInt(sourceLoginDBId);
        check.hitCheck(sourceLoginId,sourceLoginPassword);
        String expected_status =  ParseConfigFile.getValueFromConfigFile(delegationConfigFilePath, delegationConfigFileName, flow, "expectedstatus");
        String expected_message =  ParseConfigFile.getValueFromConfigFile(delegationConfigFilePath, delegationConfigFileName, flow, "expectedmessage");
        int entityId =  createEntity(csAssert);
        ArrayList<String> entityList = new ArrayList<>();
        entityList.add(String.valueOf(entityId));
        entityList.add(String.valueOf(obligationEntityTypeId));
        entityList.add(ParseConfigFile.getValueFromConfigFile(delegationConfigFilePath, delegationConfigFileName, flow, "entityrolegroupid"));
        String createPayload  =   createPayload(flow,entityList);
        createDelegation.hitCreateDelegationV2(createPayload);
        String createResponse = createDelegation.getcreateDelegationJsonStr();
        String status = (String)JSONUtility.parseJson(createResponse,"$.header.response.status");

        if(status.equals(expected_status) && status.equals("success")){
            String message = (String)JSONUtility.parseJson(createResponse,"$.header.response.properties.notification");
            csAssert.assertEquals(message,expected_message,"expected message: "+expected_message+" is not same as " +
                    "actual message: "+message+" for flow "+flow);

            List<String> delegationId = getDelegationID(entityList.get(0), entityList.get(1));
            List<String> deleteInDB = validateDeleteInDB(delegationId.get(0));
            csAssert.assertEquals(deleteInDB.get(0),"f","delete column in delegated_domain_user sholud be " +
                    "false after creation of delegationId"+ delegationId.get(0));

            adminHelper.loginWithClientAdminUser();
            int inactivateStatus = UsersInactivate.hitUsersInactivateAPI(userId);
            if(inactivateStatus==200){
                List<String> afterdeleteInDB = validateDeleteInDB(delegationId.get(0));
                csAssert.assertEquals(afterdeleteInDB.get(0),"t","delete column in delegated_domain_user is not " +
                        "marked as true after deletion of user from role group of delegationId"+ delegationId.get(0));
            }else{
                csAssert.assertFalse(true,"user "+user+"  is not inactivated");
            }
        }else{
            csAssert.assertTrue(false , "expected status: "+expected_status+" is not same as " +
                    "actual status: "+status+" for flow "+flow);
        }
        UsersActivate.hitUsersActivateAPI(userId);
        afterActivateInDB();
        check.hitCheck(user,password);
        csAssert.assertAll();
        EntityOperationsHelper.deleteEntityRecord("obligations", entityId);
       String default_user = ConfigureEnvironment.getEnvironmentProperty("j_username");
       String default_password = ConfigureEnvironment.getEnvironmentProperty("password");
       check.hitCheck(default_user,default_password);

   }


     @Test(description = "C46252")
    public void sourceUserRemovedFromRG() throws Exception {
        String flow = "c46252";
        CustomAssert csAssert = new CustomAssert();
        String expected_status =  ParseConfigFile.getValueFromConfigFile(delegationConfigFilePath, delegationConfigFileName, flow, "expectedstatus");
        String expected_message =  ParseConfigFile.getValueFromConfigFile(delegationConfigFilePath, delegationConfigFileName, flow, "expectedmessage");
        int entityId =  createEntity(csAssert);
        ArrayList<String> entityList = new ArrayList<>();
        entityList.add(String.valueOf(entityId));
        entityList.add(String.valueOf(obligationEntityTypeId));
        entityList.add(ParseConfigFile.getValueFromConfigFile(delegationConfigFilePath, delegationConfigFileName, flow, "entityrolegroupid"));
        String createPayload  =   createPayload(flow,entityList);
        createDelegation.hitCreateDelegationV2(createPayload);
        String createResponse = createDelegation.getcreateDelegationJsonStr();
        String status = (String)JSONUtility.parseJson(createResponse,"$.header.response.status");

        if(status.equals(expected_status) && status.equals("success")){
            String message = (String)JSONUtility.parseJson(createResponse,"$.header.response.properties.notification");
            csAssert.assertEquals(message,expected_message,"expected message: "+expected_message+" is not same as " +
                    "actual message: "+message+" for flow "+flow);

            List<String> delegationId = getDelegationID(entityList.get(0), entityList.get(1));
            List<String> deleteInDB = validateDeleteInDB(delegationId.get(0));
            csAssert.assertEquals(deleteInDB.get(0),"f","delete column in delegated_domain_user sholud be " +
                    "false after creation of delegationId"+ delegationId.get(0));

            editEntity(obligationEntityTypeId,entityId,csAssert);
            List<String> afterdeleteInDB = validateDeleteInDB(delegationId.get(0));
            csAssert.assertEquals(afterdeleteInDB.get(0),"t","delete column in delegated_domain_user is not " +
                    "marked as true after deletion of user from role group of delegationId"+ delegationId.get(0));
        }else{
            csAssert.assertTrue(false , "expected status: "+expected_status+" is not same as " +
                    "actual status: "+status+" for flow "+flow);
        }
        csAssert.assertAll();
         EntityOperationsHelper.deleteEntityRecord("obligations", entityId);
    }


    private int createEntity( CustomAssert csAssert) {
        String flowToTest = "flow 11";
        int entityId = -1;

        try {
            logger.info("Validating Obligation Creation Flow [{}]", flowToTest);

            //Validate Obligation Creation
            logger.info("Creating Obligation for Flow [{}]", flowToTest);
            String createResponse = Obligations.createObligation(configFilePath, configFileName, extraFieldsConfigFilePath, extraFieldsConfigFileName, flowToTest,
                    true);
            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                JSONObject jsonObj = new JSONObject(createResponse);
                String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();
                logger.info("Create Status for Flow [{}]: {}", flowToTest, createStatus);

                if (createStatus.equalsIgnoreCase("success"))
                    entityId = CreateEntity.getNewEntityId(createResponse, "obligations");
            }

        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Obligation Creation Flow [" + flowToTest + "]. " + e.getMessage());
        }

        return  entityId;
    }


    private void editEntity(int entityTypeId,int entityId, CustomAssert csAssert) throws Exception {
        show.hitShowGetAPI(entityTypeId,entityId);
        String showResponse = show.getShowJsonStr();
        JSONObject obligationdata = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data");
        obligationdata.put("stakeHolders", new JSONObject("{\"name\":\"stakeHolders\",\"values\":{\"rg_2004\":{\"values\":[{\"name\":\"Akshay User\",\"id\":1047,\"type\":2,\"email\":\"akshay_user_dft@sirionqa.office\"}],\"name\":\"rg_2004\",\"label\":\"Master Obligations Manager\",\"userType\":[2,1,3,4]},\"rg_3170\":{\"values\":[{\"name\":\"Akshay User\",\"id\":1047,\"type\":2,\"email\":\"akshay_user_dft@sirionqa.office\"},{\"name\":\"pertyy perfrr\",\"id\":1043,\"type\":2,\"email\":\"ajay_user_dft@sirionqa.office\"}],\"name\":\"rg_3170\",\"label\":\"Suppliers Manager\",\"userType\":[2,1,3,4]}},\"options\":{\"api\":\"\\\"\\\"\",\"autoComplete\":true,\"data\":null,\"size\":null,\"sizeLimit\":50,\"enableListView\":false,\"filterName\":\"User\"},\"multiEntitySupport\":false}"));
        obligationdata.remove("history");

        JSONObject payload = new JSONObject();
        JSONObject body = new JSONObject();
        body.put("data",obligationdata);
        payload.put("body",body);
        String editResponce = edit.hitDnoEdit("obligations",payload.toString());
        csAssert.assertEquals(JSONUtility.parseJson(editResponce,"$.header.response.status"),
                "success", "obligation "+ entityId+" stakeholder is not updated");

    }


    private String createPayload(String flow,  ArrayList<String> entityList) throws ParseException {
        String createPayload = "";
        HashMap<String, String> valueMap = new HashMap<>();
        valueMap.put("entityIdValue", entityList.get(0));
        valueMap.put("entityTypeIdValue", entityList.get(1));
        valueMap.put("roleGroupIdValue", entityList.get(2));
        String toUserName = ParseConfigFile.getValueFromConfigFile(delegationConfigFilePath, delegationConfigFileName, flow, "tousername");
        String toUserId = ParseConfigFile.getValueFromConfigFile(delegationConfigFilePath, delegationConfigFileName, flow, "touserid");
        String endDateValue = ParseConfigFile.getValueFromConfigFile(delegationConfigFilePath, delegationConfigFileName, flow, "enddatevalue");
        String startDateValue = ParseConfigFile.getValueFromConfigFile(delegationConfigFilePath, delegationConfigFileName, flow, "startdatevalue");

        valueMap.put("startDateValue", getDate(startDateValue));
        valueMap.put("endDateValue", getDate(endDateValue));
        valueMap.put("toUserId", toUserId);
        valueMap.put("toUserName", toUserName);
        createPayload = StringUtils.strSubstitutor(payload, valueMap);

        return  createPayload;
    }

    private String getDate(String noofDays) throws ParseException {
        String date = "";
        date = DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInAnyFormat("MM-dd-yyyy 00:00:00"),Integer.valueOf(noofDays),"MM-dd-yyyy 00:00:00");
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


    public boolean afterActivateInDB() {
        boolean results=false;
        String query = "update app_user  set temp_pwd_creation_time =null, first_login =false , last_login_time = now() , failed_login_count = 0 , password_never_expires = true , account_locked_time = null, \n" +
                "password= 'e0aab9cd2ec312b421624434538126ba6aba4370e7c5af16841c8c6458a4d6a3' where id ="+sourceLoginDBId+";update app_user set password = '78c4fe012d686809f0fa236e9d80caa41236ecebb1042824daff38f99c9f092b' where id ="+sourceLoginDBId+";update app_user set send_email = true where id ="+sourceLoginDBId+";";

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
             results = sqlObj.updateDBEntry(query);

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Getting client Data from DB using query [{}]. {}", query, e.getMessage());
        }
        return results;
    }


}
