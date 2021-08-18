package com.sirionlabs.test.workflowPod;

import com.sirionlabs.api.clientAdmin.userConfiguration.*;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class TestAuditLogs extends TestAPIBase {
    private final static Logger logger = LoggerFactory.getLogger(TestAuditLogs.class);
    private int user_id = 2178;

    @BeforeClass
    public void beforeClass() {
        //Login as client admin user
        AdminHelper login_as_admin = new AdminHelper();
        login_as_admin.loginWithClientAdminUser();
    }

    @AfterClass
    public void afterClass() {
        //logging back with End User
        new Check().hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));
    }

    @Test
    public void testUnlock() {
        CustomAssert csAssert = new CustomAssert();

        try {
            //Getting user Locked
           logger.info("Locking the user");
            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
            postgreSQLJDBC.doSelect("update app_user set send_email = true,  account_locked_time = now() where id = 2178");

            //Hit User Show API
            String showResponse = executor.get(UserConfigurationShow.getApiPath(String.valueOf(user_id)),
                    UserConfigurationShow.getHeaders()).getResponse().getResponseBody();
            Document html = Jsoup.parse(showResponse);

            int auditLogsSizeBeforeAction = html.getElementById("l_com_sirionlabs_model_TblAuditLog").child(1).children().size();

            Elements elements = html.getElementsByClass("submit").get(0).children();
            int flag = 0;
            for (Element element : elements) {
                if (element.children().size() > 0 && element.child(0).val().equalsIgnoreCase("Unlock And Reset Password")) {
                    flag = 1;
                    break;
                }
            }
            //Hit API for Unlock user
            if (flag != 1) {
                throw new SkipException("Couldn't Unlock User.");
            }

            logger.info("Unlocking User");
            UserUnlock.hitUsersUnlockAPI(user_id);

            //Check audit logs
            checkAuditLogs("Unlock", auditLogsSizeBeforeAction, csAssert);

            //INACTIVATE API
            showResponse = executor.get(UserConfigurationShow.getApiPath(String.valueOf(user_id)),
                    UserConfigurationShow.getHeaders()).getResponse().getResponseBody();
            html = Jsoup.parse(showResponse);
            auditLogsSizeBeforeAction = html.getElementById("l_com_sirionlabs_model_TblAuditLog").child(1).children().size();

            elements = html.getElementsByClass("submit").get(0).children();

            flag = 0;
            for (Element element : elements) {
                if (element.val().equalsIgnoreCase("Inactivate")) {
                    flag = 1;
                    break;
                }
            }
            if (flag != 1) {
                throw new SkipException("Couldn't Inactivate User.");
            }

            logger.info("Inactivating User");
            UsersInactivate.hitUsersInactivateAPI(user_id);
            //Check Audit Logs
            checkAuditLogs("Inactivate", auditLogsSizeBeforeAction, csAssert);

            //ACTIVATE API
            showResponse = executor.get(UserConfigurationShow.getApiPath(String.valueOf(user_id)),
                    UserConfigurationShow.getHeaders()).getResponse().getResponseBody();
            html = Jsoup.parse(showResponse);
            auditLogsSizeBeforeAction = html.getElementById("l_com_sirionlabs_model_TblAuditLog").child(1).children().size();

            elements = html.getElementsByClass("submit").get(0).children();

            flag = 0;
            for (Element element : elements) {
                if (element.val().equalsIgnoreCase("Activate")) {
                    flag = 1;
                    break;
                }
            }
            if (flag != 1) {
                throw new SkipException("Couldn't Activate User.");
            }

            logger.info("Activating User");
            UsersActivate.hitUsersActivateAPI(user_id);

            //Check audit logs
            checkAuditLogs("activate", auditLogsSizeBeforeAction, csAssert);

            //RESET PASSWORD API
            showResponse = executor.get(UserConfigurationShow.getApiPath(String.valueOf(user_id)),
                    UserConfigurationShow.getHeaders()).getResponse().getResponseBody();
            html = Jsoup.parse(showResponse);
            auditLogsSizeBeforeAction = html.getElementById("l_com_sirionlabs_model_TblAuditLog").child(1).children().size();

            elements = html.getElementsByClass("submit").get(0).children();

            flag = 0;
            for (Element element : elements) {
                if (element.val().equalsIgnoreCase("Reset Password")) {
                    flag = 1;
                    break;
                }
            }
            if (flag != 1) {
                throw new SkipException("Couldn't Reset Password. ");
            }
            postgreSQLJDBC.doSelect("update app_user set send_email = true");
            logger.info("Reset Password checking.");
            UserResetPassword.hitResetPasswordAPI(user_id);

            //Check audit logs
            checkAuditLogs("reset password", auditLogsSizeBeforeAction, csAssert);


        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    private void checkAuditLogs(String actionName, int sizeBeforeAction, CustomAssert csAssert) {
        try {
            String showResponse = executor.get(UserConfigurationShow.getApiPath(String.valueOf(user_id)),
                    UserConfigurationShow.getHeaders()).getResponse().getResponseBody();

            Document html = Jsoup.parse(showResponse);
            int auditLogsSizeAfterAction = html.getElementById("l_com_sirionlabs_model_TblAuditLog").child(1).children().size();


            if (auditLogsSizeAfterAction > sizeBeforeAction) {
                String actualActionName = html.getElementById("l_com_sirionlabs_model_TblAuditLog").child(1).child(0).child(0).child(0).childNode(0).toString().replace("\n", "");
                String expectedActionName = getExpectedActionName(actionName);

                if (expectedActionName == null) {
                    csAssert.assertFalse(true, "Couldn't get Expected Action Name for Action " + actionName);
                } else {
                    csAssert.assertTrue(actualActionName.equalsIgnoreCase(expectedActionName), "Expected Action Name: " + expectedActionName +
                            " and Actual Action Name: " + actualActionName + " for Action: " + actionName);
                }
            } else {
                csAssert.assertFalse(true, "No New Audit Log Created. for "+actionName);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Audit Logs for Action " + actionName + ". " + e.getMessage());
        }
    }

    private String getExpectedActionName(String actionName) {
        switch (actionName.toLowerCase()) {
            case "unlock":
                return "Unlocked";

            case "activate":
                return "Activated";

            case "inactivate":
                return "Inactivated";

            case "reset password":
                return "Reset Password";
            default:
                return null;
        }
    }
}
