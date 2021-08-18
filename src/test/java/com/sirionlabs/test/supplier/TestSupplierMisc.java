package com.sirionlabs.test.supplier;

import com.sirionlabs.api.clientAdmin.masterUserRoleGroups.MasterUserRoleGroupsList;
import com.sirionlabs.api.clientAdmin.masterUserRoleGroups.MasterUserRoleGroupsUpdate;
import com.sirionlabs.api.commonAPI.Actions;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.DefaultUserListMetadataHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.*;

@Listeners(value = MyTestListenerAdapter.class)
public class TestSupplierMisc extends TestAPIBase {
    AdminHelper admin;

    private final static Logger logger = LoggerFactory.getLogger(TestSupplierMisc.class);

    private AdminHelper adminHelperObj = new AdminHelper();
    private DefaultUserListMetadataHelper defaultUserListHelperObj = new DefaultUserListMetadataHelper();


    @BeforeClass
    public void before(){
        admin = new AdminHelper();
    }


    /*
    TC-C7909: Verify that User is not able to create and edit a Supplier when permissions are disabled.
     */
    @Test
    public void testC7909() {
        CustomAssert csAssert = new CustomAssert();

        String userName = ConfigureEnvironment.getEnvironmentProperty("j_username");
        String password = ConfigureEnvironment.getEnvironmentProperty("password");
        Set<String> allPermissions = adminHelperObj.getAllPermissionsForUser(userName, admin.getClientId());

        try {
            logger.info("Starting Test TC-C7909: Verify that User is not able to create and edit Supplier when permissions are disabled.");

            //Remove Create and Edit Permissions
            if (allPermissions.isEmpty()) {
                throw new SkipException("Couldn't get Permissions for User " + userName);
            }

            //Permission Id for Create: 183 and Edit: 184
            //Remove Create and Edit Permissions
            Set<String> newPermissions = new HashSet<>(allPermissions);
            newPermissions.remove("183");
            newPermissions.remove("184");

            String newPermissionsStr = newPermissions.toString().replace("[", "{").replace("]", "}");

            boolean permissionUpdated = adminHelperObj.updatePermissionsForUser(userName, admin.getClientId(), newPermissionsStr);

            if (!permissionUpdated) {
                throw new SkipException("Couldn't Remove Permissions for Create and Edit Supplier.");
            }


            new Check().hitCheck(userName,password);

            //Verify Create
            String defaultUserListMetadataResponse = defaultUserListHelperObj.getDefaultUserListMetadataResponse("suppliers");

            if (ParseJsonResponse.validJsonResponse(defaultUserListMetadataResponse)) {
                JSONObject jsonObj = new JSONObject(defaultUserListMetadataResponse);

                //Verify popUpUrl
                if (jsonObj.has("popupUrl") && !jsonObj.isNull("popupUrl")) {
                    csAssert.assertTrue(false, "PopUpUrl Property in DefaultUserListMetadata API Response for Supplier is not null.");
                }

                //Verify isPlusIcon
                if (jsonObj.has("isPlusIcon") && (!jsonObj.isNull("isPlusIcon") && jsonObj.getBoolean("isPlusIcon"))) {
                    csAssert.assertTrue(false, "IsPlusIcon Property in DefaultUserListMetadata API Response for Supplier is not null.");
                }
            } else {
                csAssert.assertTrue(false, "DefaultUserListMetadata API Response for Supplier is an Invalid JSON.");
            }

            //Verify Edit
            String listDataResponse = ListDataHelper.getListDataResponseVersion2("suppliers");

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                JSONObject jsonObj = new JSONObject(listDataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                if (jsonArr.length() == 0) {
                    throw new SkipException("Couldn't get any data in ListData API Response for Supplier.");
                }
                 int index = -1;
                ArrayList<String> rolegroup =  ( ArrayList<String>)JSONUtility.parseJson(jsonArr.toString(),"$.[*].[*][?(@.columnName=='Suppliers ManagerROLE_GROUP')].value");

                for (int i = 0; i <rolegroup.size() ; i++) {

                    if(!rolegroup.get(i).contains("Anay User")){
                        index = i;
                        break;
                    }

                }

                if(index==-1){
                    throw new SkipException("Couldn't get any data in ListData API Response in which anay user does not exist in Suppliers Manager .");
                }

                int idColumn = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
                String value = jsonArr.getJSONObject(index).getJSONObject(String.valueOf(idColumn)).getString("value");

                int recordId = ListDataHelper.getRecordIdFromValue(value);

                String actionsResponse = Actions.getActionsV2Response(1, recordId);

                if (ParseJsonResponse.validJsonResponse(actionsResponse)) {
                    List<String> allActionNames = Actions.getAllActionNames(actionsResponse);

                    if (allActionNames == null) {
                        csAssert.assertTrue(false, "Couldn't get All Action Names from Actions API Response for Supplier Record Id " + recordId);
                    }

                    if (allActionNames.contains("Edit")) {
                        csAssert.assertTrue(false, "Supplier Record Id " + recordId + " has Edit Action on Show Page.");
                    }
                } else {
                    csAssert.assertTrue(false, "Actions API Response for Supplier Record Id " + recordId + " is an Invalid JSON.");
                }
            } else {
                csAssert.assertTrue(false, "ListData API Response for Supplier is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C7909. " + e.getMessage());
        } finally {
            adminHelperObj.updatePermissionsForUser(userName, admin.getClientId(),
                    allPermissions.toString().replace("[", "{").replace("]", "}"));
        }

        csAssert.assertAll();
    }


    /*
    TC-C7911: Verify that Permissions Create, Edit, Show, Delete, Bulk Edit, Show Quick Link and Download Quick Link are present in Supplier.
     */
    @Test
    public void testC7911() {
        CustomAssert csAssert = new CustomAssert();
        String lastLoggedInEndUserName = Check.lastLoggedInUserName;
        String lastLoggedInEndUserPassword = Check.lastLoggedInUserPassword;

        try {
            adminHelperObj.loginWithClientAdminUser();

            logger.info("Hitting MasterUserRoleGroups List API.");
            String listResponse = executor.get(MasterUserRoleGroupsList.getAPIPath(), MasterUserRoleGroupsList.getHeaders()).getResponse().getResponseBody();

            Integer adminUrgId = MasterUserRoleGroupsList.getUserRoleGroupId(listResponse, "Admin");

            if (adminUrgId == null) {
                throw new SkipException("Couldn't Get Id of Admin Role Group.");
            }

            String updateResponse = executor.get(MasterUserRoleGroupsUpdate.getApiPath(adminUrgId), MasterUserRoleGroupsUpdate.getHeaders()).getResponse().getResponseBody();

            Document html = Jsoup.parse(updateResponse);
            Element div = html.getElementsByClass("accordion user_permission").get(0);

            Elements allGroups = div.children();
            boolean supplierGroupFound = false;

            for (int i = 0; i < allGroups.size(); i = i + 2) {
                String groupName = allGroups.get(i).child(0).child(0).childNode(0).toString().replace(":", "");

                if (groupName.trim().equalsIgnoreCase("Supplier")) {
                    supplierGroupFound = true;
                    String[] expectedPermissionNames = {"Create", "Edit", "Show", "Delete", "Bulk Edit", "Show Quick Link", "Download Quick Link"};
                    List<String> expectedPermissionNamesList = new ArrayList<>(Arrays.asList(expectedPermissionNames));

                    Elements allPermissionsTab = div.children().get(i + 1).child(0).child(0).child(0).child(0).children();
                    List<String> actualPermissionNamesList = new ArrayList<>();

                    for (Element permissionTab : allPermissionsTab) {
                        String permissionName = permissionTab.childNode(5).toString().trim();
                        actualPermissionNamesList.add(permissionName);
                    }

                    for (String expectedPermissionName : expectedPermissionNamesList) {
                        if (!actualPermissionNamesList.contains(expectedPermissionName)) {
                            csAssert.assertTrue(false, "Permission [" + expectedPermissionName + "] not found in VH Group");
                        }
                    }

                }
            }

            if (!supplierGroupFound) {
                csAssert.assertTrue(false, "Supplier Group not found in URG Update Response.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C7911. " + e.getMessage());
        } finally {
            new Check().hitCheck(lastLoggedInEndUserName, lastLoggedInEndUserPassword);
        }

        csAssert.assertAll();
    }
}