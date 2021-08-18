package com.sirionlabs.test.bePod;

import com.sirionlabs.api.clientAdmin.QuickLinkViewConfiguration;
import com.sirionlabs.api.clientAdmin.fieldProvisioning.FieldProvisioning;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.listRenderer.ListRendererFilterData;
import com.sirionlabs.helper.ListRenderer.DefaultUserListMetadataHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

@Listeners(value = MyTestListenerAdapter.class)
public class WeekTypeTest {
    final public static Logger logger = LoggerFactory.getLogger(WeekTypeTest.class);
    private String lastUserName;
    private String lastUserPwd;
    // private final int listId = 3;
    //private final int entityTypeId = 1; //supplier entity Type Id is 1
    private AdminHelper adminHelper = new AdminHelper();
    private CustomAssert csAssert = new CustomAssert();

    private void clientAdminLogin() {
        logger.info("Login Client Admin User");
        adminHelper.loginWithClientAdminUser();
    }

    private void loginEndUser() {
        logger.info("Login End User");
        Check check = new Check();
        check.hitCheck(lastUserName, lastUserPwd);
    }

    @DataProvider(name = "listId")
    public Object[][] listIdSupplierAndContract() {

        return new Object[][]{{"suppliers", 3}, {"contracts", 2}};

    }

    @DataProvider(name = "entityTypeId")
    public Object[][] entityTypeId() {
        Object objects[][] = new Object[2][1];
        objects[0][0] = 1;
        objects[1][0] = 61;
        return objects;
    }

    @BeforeTest
    public void endUserProperty() {
        lastUserName = Check.lastLoggedInUserName;
        lastUserPwd = Check.lastLoggedInUserPassword;
    }

    @Test(dataProvider = "entityTypeId", priority = 0)
    public void testCaseC63469(int entityTypeId) {
        logger.info("Check Week Type field should be available in Field Provisioning for Entity {}", entityTypeId);
        clientAdminLogin();
        FieldProvisioning fieldProvisioning = new FieldProvisioning();
        String response = fieldProvisioning.hitFieldProvisioning(entityTypeId);
        JSONObject jsonObject = new JSONObject(response);
        JSONArray jsonArray = jsonObject.getJSONArray("data");
        boolean fieldFound = false;
        for (int i = 0; i < jsonArray.length(); i++) {
            if (jsonArray.getJSONObject(i).get("apiName").toString().contains("weekType")) {
                fieldFound = true;
                break;
            }
        }
        csAssert.assertTrue(fieldFound, "Week type field not found.");
        csAssert.assertAll();
    }

    @Test(dataProvider = "listId", priority = 1)
    public void testCaseWeekTypePresentInQuickLinkView(String entityName, int listId) throws Exception {
        QuickLinkViewConfiguration quickLinkViewConfiguration = new QuickLinkViewConfiguration();
        quickLinkViewConfiguration.hitQuickLinkViewConfigure(listId);
        String configureDataJsonStr = quickLinkViewConfiguration.getConfigureDataJsonStr();
        JSONObject jsonObject = new JSONObject(configureDataJsonStr);
        JSONArray jsonArray = jsonObject.getJSONArray("columns");
        boolean findQueryName = false;
        for (int i = 0; i < jsonArray.length(); i++) {
            if (jsonArray.getJSONObject(i).getString("queryName").contains("weektype")) {
                findQueryName = true;
                break;
            }
        }
        csAssert.assertTrue(findQueryName, "Week Type Not Found In Quick Link View");
        csAssert.assertAll();
    }

    @Test(dataProvider = "listId", priority = 2)
    public void listing_filterC63484(String entityName, int listId) {
        loginEndUser();
        logger.info("Check As an End User one should be able to view the Field Week Type Under Listing and Filter of Supplier Entity.");
        DefaultUserListMetadataHelper list = new DefaultUserListMetadataHelper();
        String response = list.getDefaultUserListMetadataResponse(entityName);
        boolean weekTypeColumnPresent = list.hasColumnQueryName(response, "weektype");
        csAssert.assertTrue(weekTypeColumnPresent, "week type not found in column list");
        String filterDataResponse = getListRenderFilterDataJsonStr(listId);
        boolean weekTypeFilterPresent = hasFilterQueryName(filterDataResponse, "weektypes");
        csAssert.assertTrue(weekTypeFilterPresent, "week type not present in filter");
        csAssert.assertAll();
    }

    private String getListRenderFilterDataJsonStr(int listId) {
        ListRendererFilterData listRendererFilterDataFilter = new ListRendererFilterData();
        listRendererFilterDataFilter.hitListRendererFilterData(listId);
        return listRendererFilterDataFilter.getListRendererFilterDataJsonStr();
    }

    private static boolean hasFilterQueryName(String filterDataResponse, String filterName) {

        JSONObject jsonObj = new JSONObject(filterDataResponse);
        JSONArray jsonArr = jsonObj.names();
        for (int i = 0; i < jsonArr.length(); i++) {
            if (jsonObj.getJSONObject(jsonArr.get(i).toString()).getString("filterName").trim().equalsIgnoreCase(filterName)) {
                return true;
            }
        }
        return false;
    }
}
