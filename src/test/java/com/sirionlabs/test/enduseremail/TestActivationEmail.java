package com.sirionlabs.test.enduseremail;

import com.sirionlabs.api.clientAdmin.userConfiguration.UserCreate;
import com.sirionlabs.api.clientAdmin.userConfiguration.UsersActivate;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.dbHelper.AppUserDbHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.After;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.*;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Listeners(value = MyTestListenerAdapter.class)
public class TestActivationEmail  extends TestAPIBase {
    private AdminHelper adminHelperObj;
    private String testEndUserCreatePath;
    private String testEndUserCreateName;
    private String lastLoggedInUserName;
    private String lastLoggedInUserPassword;
    private Check checkObj;


    private final static Logger logger = LoggerFactory.getLogger(TestActivationEmail.class);


    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        adminHelperObj = new AdminHelper();
        testEndUserCreatePath = ConfigureConstantFields.getConstantFieldsProperty("TestEndUserCreatePath");
        testEndUserCreateName = ConfigureConstantFields.getConstantFieldsProperty("TestEndUserCreateName");
        checkObj = new Check();
    }

    @DataProvider()
    public Object[][] dataProviderForCreateUser() {
        List<Object[]> allTestData = new ArrayList<>();

        String[] flows = {"sirion", "sso", "sirion_sso_both"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }


    @Test(dataProvider = "dataProviderForCreateUser")
    public void TestActivationEmail(String flow) throws SQLException, UnsupportedEncodingException {

        CustomAssert csAssert = new CustomAssert();
        String loginId = "testactivationemail_" + flow;

        int oldUserId = AppUserDbHelper.getUserIdFromLoginIdAndClientId(loginId, adminHelperObj.getClientIdFromDB());
        if (oldUserId != -1) {
            logger.info("prerequisites | deleting user from DB as user is already present in DB with loginID -: " + loginId);
            AppUserDbHelper.deleteAppUser(oldUserId);
        }

        logger.info("prerequisites | deleting activation email from sytem_email for loginid -> " + loginId);
        deleteActivationEmail(loginId);


        adminHelperObj.loginWithClientAdminUser();
        logger.info("Creating new App User.");

        Map<String, String> payloadMap = new HashMap<>();
        payloadMap.put("firstName", "API Automation");
        payloadMap.put("lastName", "Test");
        payloadMap.put("loginId", loginId);
        payloadMap.put("email", loginId + "@sirionqa.office");
        payloadMap.put("contactNo", getvalue(flow, "contactno"));
        payloadMap.put("designation", getvalue(flow, "designation"));
        payloadMap.put("timeZone.id", getvalue(flow, "timezone"));
        payloadMap.put("uniqueLoginId", loginId);
        payloadMap.put("thirdPartyId", getvalue(flow, "thirdpartyid"));
        payloadMap.put("tierId", getvalue(flow, "tierid"));
        payloadMap.put("_passwordNeverExpires", getvalue(flow, "_passwordneverexpires"));
        payloadMap.put("_enableTwofactorLogin", getvalue(flow, "_enabletwofactorlogin"));
        payloadMap.put("type", getvalue(flow, "type"));
        payloadMap.put("loginMechanismType", getvalue(flow, "loginmechanismtype"));
        payloadMap.put("vendorId", getvalue(flow, "vendorid"));
        payloadMap.put("userClassificationType", getvalue(flow, "userclassificationtype"));
        payloadMap.put("language", getvalue(flow, "language"));
        payloadMap.put("sendEmail", getvalue(flow, "sendemail"));
        payloadMap.put("_sendEmail", getvalue(flow, "_sendemail"));
        payloadMap.put("_excludeFromFilter", getvalue(flow, "_excludefromfilter"));
        payloadMap.put("userRoleGroups", getvalue(flow, "userrolegroups"));
        payloadMap.put("_userRoleGroups", getvalue(flow, "_userrolegroups"));
        payloadMap.put("_accessCriterias", getvalue(flow, "_accesscriterias"));
        payloadMap.put("_userDepartments", getvalue(flow, "_userdepartments"));
        payloadMap.put("_defaultUITwoDotO", getvalue(flow, "_defaultuitwodoto"));
        payloadMap.put("dynamicMetadataJS", getvalue(flow, "dynamicmetadatajs"));
        payloadMap.put("history", getvalue(flow, "history"));

        APIResponse response = executor.postMultiPartFormData(UserCreate.getApiPath(), UserCreate.getHeaders(), payloadMap).getResponse();
        int responseCode = response.getResponseCode();

        if (responseCode == 302) {
            int userId = AppUserDbHelper.getUserIdFromLoginIdAndClientId(loginId,  adminHelperObj.getClientIdFromDB());
            logger.info("new user created with loginId -> " + loginId);
            UsersActivate.hitUsersActivateAPI(userId);
            logger.info("user with loginID " + loginId + " activated");
            validateActivationEmail(getvalue(flow, "loginmechanismtype"), loginId, csAssert);

            csAssert.assertAll();

            logger.info(" deleting user from DB with loginID -: " + loginId);
            AppUserDbHelper.deleteAppUser(userId);

            logger.info("deleting activation email from sytem_email for loginid -> " + loginId);
            deleteActivationEmail(loginId);


        } else {
            logger.error(response.getResponseBody());
            csAssert.assertTrue(false, "Couldn't Create App User.");
        }

    }


    private void deleteActivationEmail(String loginId) {
        String delete_query = "delete from system_emails where to_mail = '" + loginId + "@sirionqa.office' and subject ilike '%Account Activation%';";

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            sqlObj.deleteDBEntry(delete_query);
            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while delete Data from system_emails from DB using query [{}]. {}", delete_query, e.getMessage());
        }

    }


    private void validateActivationEmail(String mechanismType, String loginId, CustomAssert csAssert) {
        String select_query = "select * from system_emails where to_mail = '" + loginId + "@sirionqa.office' and subject ilike '%Account Activation%'  order by id desc limit 10;";

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            List<List<String>> results = sqlObj.doSelect(select_query);

            String subject = results.get(0).get(3);
            String body = results.get(0).get(4);

            if (results.size() != 0) {
                if (mechanismType.equals("2")) {
                    csAssert.assertEquals(subject, "Account Activation", "subject is not correct --> " + subject);
                    csAssert.assertTrue(body.contains("Please note : You may simply click on SSO Login button to access the tool."), "mail body is not correct");
                } else {
                    csAssert.assertEquals(subject, "Account Activation", "subject is not correct --> " + subject);
                    csAssert.assertTrue(body.contains("Your Sirion user account has been configured and your password is"), "mail body is not correct");
                }
            } else {
                csAssert.assertTrue(false, "activation email not triggered for loginId -> " + loginId);
            }

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Getting User Data from DB using query [{}]. {}", select_query, e.getMessage());
        }
    }


    private String getvalue(String flow, String key) {
        return ParseConfigFile.getValueFromConfigFile(testEndUserCreatePath, testEndUserCreateName, flow, key);

    }

    @AfterClass
    public void afterClass() {
        checkObj.hitCheck( ConfigureEnvironment.getEnvironmentProperty("j_username"),ConfigureEnvironment.getEnvironmentProperty("password"));

    }

}