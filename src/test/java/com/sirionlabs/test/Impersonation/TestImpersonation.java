package com.sirionlabs.test.Impersonation;
import com.sirionlabs.api.clientAdmin.UserAdmin.ImpersonationGetUsersAPI;
import com.sirionlabs.api.clientAdmin.UserAdmin.UserAdminAPI;
import com.sirionlabs.api.clientAdmin.userConfiguration.UserConfigurationShow;
import com.sirionlabs.api.clientAdmin.userConfiguration.UserUpdate;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
public class TestImpersonation {

    private final static Logger logger = LoggerFactory.getLogger(TestImpersonation.class);
    private String configFilePath;
    private String configFileName;

    @BeforeClass
    public void beforeClass() {
        logger.info("read config file path and config file name");
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestImpersonationConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestImpersonationConfigFileName");
        logger.info("config file path {}", configFilePath);
        logger.info("config file name {}", configFileName);

    }

    @Test(enabled = false)
    public void clientAdminTestCase() {
        CustomAssert customAssert = new CustomAssert();
        try {
            String userAdminResponseBody = UserAdminAPI.getUserAdminResponseBody();
            Document document = Jsoup.parse(userAdminResponseBody);
            List<String> fieldName = new ArrayList<>();
            for (int i = 0; i < document.body().getElementsByClass("returnFalse").size(); i++) {
                fieldName.add(document.body().getElementsByClass("returnFalse").get(i).text());
            }
            if (!fieldName.contains("Impersonation")) {
                customAssert.assertTrue(false, "Impersonation not found in User Administration Section");
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while verifying Impersonate User filed on client admin");
            logger.error(e.getMessage());
        }
        customAssert.assertAll();
    }

    @Test(enabled = false)
    public void testCaseVerifyImpersonateUser() {
        CustomAssert customAssert = new CustomAssert();
        try {
            String allImpersonationUsersListResponseBody = ImpersonationGetUsersAPI.getAllImpersonationUsersListResponseBody();
            Document document = Jsoup.parse(allImpersonationUsersListResponseBody);
            List<String> userNames = new ArrayList<>();
            for (int i = 0; i < document.body().getElementsByClass("not-required").get(0).children().size(); i++) {
                userNames.add(document.body().getElementsByClass("not-required").get(0).child(i).text());
            }
            if (!userNames.contains("Impersonation")) {
                customAssert.assertTrue(false, "Impersonation not found in User Administration Section");
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while verifying Impersonate User list on client admin");
            logger.error(e.getMessage());
        }
        customAssert.assertAll();
    }

    @Test()
    public void userAdminTestCase() {
        CustomAssert customAssert = new CustomAssert();
        try {
            Map<String, String> formData;
            formData = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileName, "beforeedit");
            int responseCode = UserUpdate.userUpdateForUserAdmin(formData, "http://sirion.voda.office");
            if (responseCode == 302) {
                boolean impersonateFiled = isImpersonateFiled(customAssert);
                if (impersonateFiled)
                    customAssert.assertTrue(false, "Impersonate User in USER ADMINISTRATION section");
                formData = ParseConfigFile.getAllConstantPropertiesCaseSensitive(configFilePath, configFileName, "afteredit");
                responseCode = UserUpdate.userUpdateForUserAdmin(formData, "http://sirion.voda.office");
                if (responseCode == 302) {
                    impersonateFiled = isImpersonateFiled(customAssert);
                    if (!impersonateFiled)
                        customAssert.assertTrue(false, "Impersonate User not found in USER ADMINISTRATION section");
                } else {
                    customAssert.assertTrue(false, "Client user not successfully updated");
                }
            } else {
                customAssert.assertTrue(false, "Client user not successfully updated");
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while verifying Impersonate User filed");
            logger.error(e.getMessage());
        }
        customAssert.assertAll();
    }

    public boolean isImpersonateFiled(CustomAssert customAssert) {
        try {
            String userConfigurationShowResponseBody = UserConfigurationShow.getUserConfigurationShowResponseBody("1158");
            Document document = Jsoup.parse(userConfigurationShowResponseBody);
            List<String> userNames = new ArrayList<>();
            for (int i = 0; i < document.body().getElementById("userRoleGroupId5").getElementsByClass("select").size(); i++) {
                userNames.add(document.body().getElementById("userRoleGroupId5").getElementsByClass("select").get(i).text());
            }
            if (userNames.contains("Impersonate User")) {
                return true;
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, "Exception while verifying isImpersonate User filed");
            logger.error(e.getMessage());
        }
        return false;
    }
}
