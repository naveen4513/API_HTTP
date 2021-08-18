package com.sirionlabs.test.microservice.email;

import com.sirionlabs.api.microservices.Email.EmailMicroService;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.microservice.MicroserviceEnvHealper;
import com.sirionlabs.utils.commonUtils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestEmailMicroService extends TestAPIBase {
    private final static Logger logger = LoggerFactory.getLogger(TestEmailMicroService.class);
    String domain;
    String CreatedEmailConfigurationName = "" ;
    Map<String, String> confmap ;
    PostgreSQLJDBC db=null;
    String bulk_name = "";
    String name="";
    EmailMicroService createEmailAPI;


    @BeforeClass
    public void beforeClass() throws IOException {
        logger.debug("in Before Class");
        confmap = MicroserviceEnvHealper.getAllPropertiesOfSection("letterbox");
        domain = confmap.get("domain");
        db = new PostgreSQLJDBC(confmap.get("dbhost"),confmap.get("dbport"),confmap.get("maintenancedb"),confmap.get("dbusername"),confmap.get("dbpassword"));
        createEmailAPI = new EmailMicroService();
    }



    @Test(dataProvider = "FindDefaultTemplate",dataProviderClass = EmailMicroServiceDataProvider.class,priority = 0)
    public void TestfindDefaultTemplate(int rowNum ,String tc_id , String casetype, String clientId,String entityTypeId,String ExpectedStatusCode,String ExpectedMessage){
        CustomAssert csAssert = new CustomAssert();
        try {

            APIValidator validator = createEmailAPI.hitfindDefaultTemplate(executor,domain, clientId, entityTypeId);
            csAssert.assertEquals(validator.getResponse().getResponseCode(), (Integer) Integer.parseInt(ExpectedStatusCode), "status code is not correct");
            if (validator.getResponse().getResponseCode() == 302) {
                int cliId = (int) JSONUtility.parseJson(validator.getResponse().getResponseBody(), "$.clientId");
                int entId = (int) JSONUtility.parseJson(validator.getResponse().getResponseBody(), "$.entityTypeId");
                String subject = (String) JSONUtility.parseJson(validator.getResponse().getResponseBody(), "$.emailSubject");
                String body = (String) JSONUtility.parseJson(validator.getResponse().getResponseBody(), "$.emailBody");
                csAssert.assertEquals(cliId, Integer.parseInt(clientId), "clientId is not correct");
                csAssert.assertEquals(entId, Integer.parseInt(entityTypeId), "entityTypeId is not correct");
                csAssert.assertTrue(!subject.equals(""), "subject Is blank");
                csAssert.assertTrue(!body.equals(""), "body Is blank");
            }

            if (!ExpectedMessage.equals("")) {
                String msg = (String) JSONUtility.parseJson(validator.getResponse().getResponseBody(), "$.message");
                csAssert.assertEquals(msg, ExpectedMessage, "respose message is not correct");
            }
        } catch (SkipException e) {
            addTestResultAsSkip((Integer)Integer.parseInt(tc_id), csAssert);
            throw new SkipException(e.getMessage());
        }
        addTestResult((Integer)Integer.parseInt(tc_id), csAssert);
        csAssert.assertAll();
    }



    @Test(dataProvider = "CreateEmailConfDataProvider",dataProviderClass = EmailMicroServiceDataProvider.class,priority =  1)
    public void TestCreateEmailConfiguration(int rowNum , String tc_type , Map<String,String> valuemap) throws SQLException {

        CustomAssert csAssert = new CustomAssert();

        try{
            String resolved_payload = StringUtils.strSubstitutor(valuemap.get("payload"),valuemap);
            APIValidator validator = createEmailAPI.hitPostCreateEmailConfiguration(executor,domain,resolved_payload);
            csAssert.assertEquals(validator.getResponse().getResponseCode(),(Integer)Integer.parseInt(valuemap.get("ExpectedStatusCode")),"responce code is not correct");
            if(validator.getResponse().getResponseCode()==201){

                List<List<String>> list_indiviual =     db.doSelect("select * from client_entity_action_email where name LIKE '%"+valuemap.get("name")+" (Individual)%'");

                String id  = list_indiviual.get(0).get(0);
                 name  = list_indiviual.get(0).get(3);

                csAssert.assertEquals(list_indiviual.get(0).get(1),valuemap.get("clientId"),"clientId is not correct in client_entity_action_email table(indiviual)");
                csAssert.assertEquals(list_indiviual.get(0).get(2),valuemap.get("entityTypeId"),"entityTypeId is not correct in client_entity_action_email table(indiviual)");
                csAssert.assertEquals(list_indiviual.get(0).get(4),"{"+valuemap.get("toRoleGroups")+"}","toRoleGroups is not correct in client_email_template table(indiviual)");
                csAssert.assertEquals(list_indiviual.get(0).get(5),"{"+valuemap.get("ccRoleGroups")+"}","ccRoleGroups is not correct in client_email_template table(indiviual)");
                csAssert.assertEquals(list_indiviual.get(0).get(6),"{"+valuemap.get("bccRoleGroups")+"}","bccRoleGroups is not correct in client_email_template table(indiviual)");

                List<List<String>> temp_indiv =     db.doSelect("select * from client_email_template where email_id ="+id);

                csAssert.assertEquals(temp_indiv.get(0).get(2),valuemap.get("languageId"),"languageId is not correct in client_email_template table(indiviual)");
                csAssert.assertEquals(temp_indiv.get(0).get(3),name.replace(" ","_")+"_"+valuemap.get("clientId")+"_"+valuemap.get("entityTypeId")+"_"+valuemap.get("languageId")+"_subject","subject_filename is not correct in client_email_template table(indiviual)");
                csAssert.assertEquals(temp_indiv.get(0).get(4),name.replace(" ","_")+"_"+valuemap.get("clientId")+"_"+valuemap.get("entityTypeId")+"_"+valuemap.get("languageId")+"_body","body_filename is not correct in client_email_template table(indiviual)");

                List<List<String>> list_bulk =    db.doSelect("select * from client_entity_action_email where name LIKE '%"+valuemap.get("name")+" (Bulk)%'");

                String bulk_id  = list_bulk.get(0).get(0);
                bulk_name  = list_bulk.get(0).get(3);

                csAssert.assertEquals(list_bulk.get(0).get(1),valuemap.get("clientId"),"clientId is not correct in client_entity_action_email table(BULK)");
                csAssert.assertEquals(list_bulk.get(0).get(2),valuemap.get("entityTypeId"),"entityTypeId is not correct in client_entity_action_email table(BULK)");
                csAssert.assertEquals(list_bulk.get(0).get(4),"{"+valuemap.get("toRoleGroups")+"}","toRoleGroups is not correct in client_email_template table(BULK)");
                csAssert.assertEquals(list_bulk.get(0).get(5),"{"+valuemap.get("ccRoleGroups")+"}","ccRoleGroups is not correct in client_email_template table(BULK)");
                csAssert.assertEquals(list_bulk.get(0).get(6),"{"+valuemap.get("bccRoleGroups")+"}","bccRoleGroups is not correct in client_email_template table(BULK)");

                List<List<String>> temp_bulk =     db.doSelect("select * from client_email_template where email_id ="+bulk_id);

                csAssert.assertEquals(temp_bulk.get(0).get(2),valuemap.get("languageId"),"languageId is not correct in client_email_template table(BULK)");
                csAssert.assertEquals(temp_bulk.get(0).get(3),bulk_name.replace(" ","_")+"_"+valuemap.get("clientId")+"_"+valuemap.get("entityTypeId")+"_"+valuemap.get("languageId")+"_subject","subject_filename is not correct in client_email_template table(BULK)");
                csAssert.assertEquals(temp_bulk.get(0).get(4),bulk_name.replace(" ","_")+"_"+valuemap.get("clientId")+"_"+valuemap.get("entityTypeId")+"_"+valuemap.get("languageId")+"_body","body_filename is not correct in client_email_template table(BULK)");


            }
        }catch (SkipException e) {
        addTestResultAsSkip((Integer)Integer.parseInt(valuemap.get("tc_id")), csAssert);
        throw new SkipException(e.getMessage());
    }
    addTestResult((Integer)Integer.parseInt(valuemap.get("tc_id")), csAssert);
        csAssert.assertAll();

    }







    @Test(dataProvider = "ListEntityActionNames",dataProviderClass = EmailMicroServiceDataProvider.class,priority = 2)
    public void TestlistEntityActionNames(int rowNum ,String tc_id , String casetype, String clientId,String entityTypeId,String ExpectedStatusCode,String ExpectedMessage) throws SQLException {
        CustomAssert csAssert = new CustomAssert();

        try{

            ArrayList<String> actual = new ArrayList<String>();


            APIValidator validator = createEmailAPI.hitlistEntityActionNames(executor,domain, clientId, entityTypeId);


            csAssert.assertEquals(validator.getResponse().getResponseCode(), (Integer) Integer.parseInt(ExpectedStatusCode), "status code is not correct");


            if (validator.getResponse().getResponseCode() == 302) {
                List<String> expected = (List<String>) JSONUtility.parseJson(validator.getResponse().getResponseBody(), "$[*]");

                List<List<String>> entity_action_email_list = db.doSelect("select name from entity_action_email where entity_type_id=" + entityTypeId + " and name NOT LIKE '%default%'");
                List<List<String>> client_entity_action_email_list = db.doSelect("select name from client_entity_action_email where entity_type_id = " + entityTypeId + " and client_id=" + clientId);

                for (List lis : entity_action_email_list) {
                    actual.add((String) lis.get(0));
                }

                if (client_entity_action_email_list != null) {
                    for (List lis : client_entity_action_email_list) {
                        actual.add((String) lis.get(0));
                    }
                }

                csAssert.assertEquals(expected.size(), actual.size(), "No of element in the list is not equal");
                csAssert.assertEquals(expected, actual, "error not matched, expected :: " + expected + "\n actual :: " + actual);

            }
        }catch (SkipException e) {
            addTestResultAsSkip((Integer)Integer.parseInt(tc_id), csAssert);
            throw new SkipException(e.getMessage());
        }
        addTestResult((Integer)Integer.parseInt(tc_id), csAssert);
        csAssert.assertAll();
    }



   @Test(dataProvider = "FindEmailConfiguration",dataProviderClass = EmailMicroServiceDataProvider.class,priority =  3)
    public void TestFindEmailConfiguration(int rowNum , String tc_type , Map<String,String> valuemap) throws SQLException {
        CustomAssert csAssert = new CustomAssert();

        if(!tc_type.contains("invalidname")){
            valuemap.put("name",name);

        }

        try{

            APIValidator validator = createEmailAPI.hitFindEmailConfiguration(executor,domain,valuemap.get("clientId"),valuemap.get("entityTypeId"),valuemap.get("languageId"),valuemap.get("name"));
            System.out.println(validator.getResponse().getResponseBody());
            csAssert.assertEquals(validator.getResponse().getResponseCode(),(Integer)Integer.parseInt(valuemap.get("ExpectedStatusCode")),"responce code is not correct");
            if(validator.getResponse().getResponseCode()==302){

                csAssert.assertTrue(JSONUtility.parseJson(validator.getResponse().getResponseBody(),"$.recipients.toRoleGroups").toString().contains(valuemap.get("toRoleGroups")),"toRoleGroups is not correct");
                csAssert.assertTrue(JSONUtility.parseJson(validator.getResponse().getResponseBody(),"$.recipients.ccRoleGroups").toString().contains(valuemap.get("ccRoleGroups")),"ccRoleGroups is not correct");
                csAssert.assertTrue(JSONUtility.parseJson(validator.getResponse().getResponseBody(),"$.recipients.bccRoleGroups").toString().contains(valuemap.get("bccRoleGroups")),"bccRoleGroups is not correct");
                csAssert.assertEquals((int) JSONUtility.parseJson(validator.getResponse().getResponseBody(),"$.clientId"),Integer.parseInt(valuemap.get("clientId")),"clientId is not correct");
                csAssert.assertEquals((int) JSONUtility.parseJson(validator.getResponse().getResponseBody(),"$.entityTypeId"),Integer.parseInt(valuemap.get("entityTypeId")),"entityTypeId is not correct");
                csAssert.assertEquals(JSONUtility.parseJson(validator.getResponse().getResponseBody(),"$.name"),name,"name is not correct");
                csAssert.assertEquals((int) JSONUtility.parseJson(validator.getResponse().getResponseBody(),"$.languageId"),Integer.parseInt(valuemap.get("languageId")),"languageId is not correct");
                csAssert.assertEquals(JSONUtility.parseJson(validator.getResponse().getResponseBody(),"$.emailSubject"),valuemap.get("emailSubject"),"emailSubject is not correct");
                csAssert.assertTrue(((String)JSONUtility.parseJson(validator.getResponse().getResponseBody(),"$.emailBody")).contains(valuemap.get("emailBody")),"emailBody is not correct");
            }
            if (!valuemap.get("ExpectedMessage").equals("")) {
                String msg = (String) JSONUtility.parseJson(validator.getResponse().getResponseBody(), "$.message");
                csAssert.assertEquals(msg,"Email configuration for action '"+valuemap.get("name")+"' not found for client_id "+valuemap.get("clientId")+" and entity_type_id "+valuemap.get("entityTypeId"), "respose message is not correct");
            }
        }catch (SkipException e) {
            addTestResultAsSkip((Integer)Integer.parseInt(valuemap.get("tc_id")), csAssert);
            throw new SkipException(e.getMessage());
        }
        addTestResult((Integer)Integer.parseInt(valuemap.get("tc_id")), csAssert);

        csAssert.assertAll();





    }





     @Test(dataProvider = "UpdateEmailConfDataProvider",dataProviderClass = EmailMicroServiceDataProvider.class,priority =  4)
    public void TestUpdateEmailConfiguration(int rowNum , String tc_type , Map<String,String> valuemap) throws SQLException {
        CustomAssert csAssert = new CustomAssert();
        try{
            if(!tc_type.equalsIgnoreCase("invalidname")){
            valuemap.put("name",name);}
            String resolved_payload = StringUtils.strSubstitutor(valuemap.get("payload"),valuemap);
            APIValidator validator = createEmailAPI.hitUpdateEmailConfiguration(executor,domain,resolved_payload);
            System.out.println(validator.getResponse().getResponseBody());
            csAssert.assertEquals(validator.getResponse().getResponseCode(),(Integer)Integer.parseInt(valuemap.get("ExpectedStatusCode")),"responce code is not correct");
            if(validator.getResponse().getResponseCode()==200){

                List<List<String>> list_indiviual =     db.doSelect("select * from client_entity_action_email where name LIKE '%"+valuemap.get("name")+"%'");

                String id  = list_indiviual.get(0).get(0);

                csAssert.assertEquals(list_indiviual.get(0).get(1),valuemap.get("clientId"),"clientId is not correct in client_entity_action_email table(indiviual)");
                csAssert.assertEquals(list_indiviual.get(0).get(2),valuemap.get("entityTypeId"),"entityTypeId is not correct in client_entity_action_email table(indiviual)");
                csAssert.assertEquals(list_indiviual.get(0).get(4),"{"+valuemap.get("toRoleGroups")+"}","toRoleGroups is not correct in client_email_template table(indiviual)");
                csAssert.assertEquals(list_indiviual.get(0).get(5),"{"+valuemap.get("ccRoleGroups")+"}","ccRoleGroups is not correct in client_email_template table(indiviual)");
                csAssert.assertEquals(list_indiviual.get(0).get(6),"{"+valuemap.get("bccRoleGroups")+"}","bccRoleGroups is not correct in client_email_template table(indiviual)");

                List<List<String>> temp_indiv =     db.doSelect("select * from client_email_template where email_id ="+id+" and language_id="+valuemap.get("languageId"));

                csAssert.assertEquals(temp_indiv.get(0).get(2),valuemap.get("languageId"),"languageId is not correct in client_email_template table(indiviual)");
                csAssert.assertEquals(temp_indiv.get(0).get(3),name.replace(" ","_")+"_"+valuemap.get("clientId")+"_"+valuemap.get("entityTypeId")+"_"+valuemap.get("languageId")+"_subject","subject_filename is not correct in client_email_template table(indiviual)");
                csAssert.assertEquals(temp_indiv.get(0).get(4),name.replace(" ","_")+"_"+valuemap.get("clientId")+"_"+valuemap.get("entityTypeId")+"_"+valuemap.get("languageId")+"_body","body_filename is not correct in client_email_template table(indiviual)");

                APIValidator find_validator = createEmailAPI.hitFindEmailConfiguration(executor,domain,valuemap.get("clientId"),valuemap.get("entityTypeId"),valuemap.get("languageId"),valuemap.get("name"));
                csAssert.assertEquals(JSONUtility.parseJson(find_validator.getResponse().getResponseBody(),"$.emailSubject"),valuemap.get("emailSubject"),"emailSubject is not correct");
                csAssert.assertTrue(((String)JSONUtility.parseJson(find_validator.getResponse().getResponseBody(),"$.emailBody")).contains(valuemap.get("emailBody")),"emailBody is not correct");


            }
            if (!valuemap.get("ExpectedMessage").equals("")) {
                String msg = (String) JSONUtility.parseJson(validator.getResponse().getResponseBody(), "$.message");
                csAssert.assertEquals(msg,"Email configuration for action '"+valuemap.get("name")+"' not found for client_id "+valuemap.get("clientId")+" and entity_type_id "+valuemap.get("entityTypeId"), "respose message is not correct");
            }
        }catch (SkipException e) {
            addTestResultAsSkip((Integer)Integer.parseInt(valuemap.get("tc_id")), csAssert);
            throw new SkipException(e.getMessage());
        }
        addTestResult((Integer)Integer.parseInt(valuemap.get("tc_id")), csAssert);
        csAssert.assertAll();





    }




    @AfterClass
    public void afterClass(){
        logger.debug("In after class");
        db.closeConnection();
    }


}
