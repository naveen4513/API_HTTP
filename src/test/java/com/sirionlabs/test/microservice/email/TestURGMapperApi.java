package com.sirionlabs.test.microservice.email;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.sirionlabs.api.microservices.Email.EmailMicroService;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.microservice.MicroserviceEnvHealper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import com.sirionlabs.utils.commonUtils.StringUtils;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

public class TestURGMapperApi extends TestAPIBase {
    String domain;
    String CreatedEmailConfigurationName = "" ;
    Map<String, String> confmap ;
    PostgreSQLJDBC db=null;
    String bulk_name = "";
    String name="";
    EmailMicroService emailAPI;
    String jwtexpirytime = "";

    @BeforeClass
    public void beforeClass() throws IOException {
        confmap = MicroserviceEnvHealper.getAllPropertiesOfSection("mainapp");
        domain = confmap.get("domain");
        jwtexpirytime = confmap.get("jwtexpirytime");
       emailAPI = new EmailMicroService();


    }

   // @Test(dataProvider = "URGMapperAuthentication",dataProviderClass = EmailMicroServiceDataProvider.class)
    public void TestURGMapperApiAuthentication(int rowNum , String tc_type , Map<String,String> valuemap){
        CustomAssert csAssert = new CustomAssert();
        APIValidator validator=null;

        try{
            String authToken  = generateToken("jwtsecretkey","Sirion",Integer.valueOf(jwtexpirytime));
            String resolved_payload = StringUtils.strSubstitutor(valuemap.get("payload"),valuemap);
            if(!tc_type.equals("withoutAuthToken")) {
                 validator = emailAPI.hitURGMapperApi(executor, domain, resolved_payload, valuemap.get("entityTypeId"), valuemap.get("entityId"), Boolean.valueOf(valuemap.get("activeUsersOnly")), Boolean.valueOf(valuemap.get("isWorkflow")), authToken);
            }
            else{
                validator = emailAPI.hitURGMapperApi(executor, domain, resolved_payload, valuemap.get("entityTypeId"), valuemap.get("entityId"), Boolean.valueOf(valuemap.get("activeUsersOnly")), Boolean.valueOf(valuemap.get("isWorkflow")), ApiHeaders.getEmailDefaultHeaders());
            }
            csAssert.assertEquals(validator.getResponse().getResponseCode(),(Integer)Integer.parseInt(valuemap.get("ExpectedStatusCode")),"responce code is not correct:: Actual ["+validator.getResponse().getResponseCode()+"], Expected["+valuemap.get("ExpectedStatusCode")+"] ");
            }catch (SkipException e) {
            addTestResultAsSkip((Integer)Integer.parseInt(valuemap.get("tc_id")), csAssert);
            throw new SkipException(e.getMessage());
            }
    addTestResult((Integer)Integer.parseInt(valuemap.get("tc_id")), csAssert);
        csAssert.assertAll();
        }

    @Test(dataProvider = "URGMapperAPI",dataProviderClass = EmailMicroServiceDataProvider.class)
    public void TestURGMapperApi(int rowNum , String tc_type , Map<String,String> valuemap){
        CustomAssert csAssert = new CustomAssert();
        APIValidator validator=null;

        try{
            String authToken  = generateToken("jwtsecretkey","Sirion",Integer.valueOf(jwtexpirytime));
            String resolved_payload = StringUtils.strSubstitutor(valuemap.get("payload"),valuemap);

            validator = emailAPI.hitURGMapperApi(executor, domain, resolved_payload, valuemap.get("entityTypeId"), valuemap.get("entityId"), Boolean.valueOf(valuemap.get("activeUsersOnly")), Boolean.valueOf(valuemap.get("isWorkflow")), authToken);


            csAssert.assertEquals(validator.getResponse().getResponseCode(),(Integer)Integer.parseInt(valuemap.get("ExpectedStatusCode")),"responce code is not correct:: Actual ["+validator.getResponse().getResponseCode()+"], Expected["+valuemap.get("ExpectedStatusCode")+"] ");
            if(validator.getResponse().getResponseCode()==200){
                csAssert.assertEquals(validator.getResponse().getResponseBody(),valuemap.get("ExpectedResult"),"ExpectedResult is not correct:: Actual ["+validator.getResponse().getResponseBody()+"], Expected["+valuemap.get("ExpectedResult")+"] ");
            }

        }catch (SkipException e) {
            addTestResultAsSkip((Integer)Integer.parseInt(valuemap.get("tc_id")), csAssert);
            throw new SkipException(e.getMessage());
        }
        addTestResult((Integer)Integer.parseInt(valuemap.get("tc_id")), csAssert);
        csAssert.assertAll();
    }



   public String generateToken(String secretKey , String issuer ,int expiryTimeMin ){
        String token = "";
       Algorithm algorithm = Algorithm.HMAC256(secretKey);
        token = JWT.create()
               .withIssuer(issuer)
               .withExpiresAt(new Date(System.currentTimeMillis()+(expiryTimeMin*60*1000)))
               .sign(algorithm);
       return token;
   }




}
