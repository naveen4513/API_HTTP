package com.sirionlabs.test.vendorHierarchy;

import com.sirionlabs.api.clientAdmin.ClientShow;
import com.sirionlabs.api.clientAdmin.masterUserRoleGroups.MasterUserRoleGroupsCreate;
import com.sirionlabs.api.clientAdmin.masterUserRoleGroups.MasterUserRoleGroupsList;
import com.sirionlabs.api.clientAdmin.masterUserRoleGroups.MasterUserRoleGroupsUpdate;
import com.sirionlabs.api.commonAPI.Actions;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.CreateForm;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.DefaultUserListMetadataHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
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
public class TestVHMisc extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestVHMisc.class);
    AdminHelper admin;
    private int vendorEntityTypeId;
    private int firstVendorRecordId = -1;

    private AdminHelper adminHelperObj = new AdminHelper();
    private DefaultUserListMetadataHelper defaultUserListHelperObj = new DefaultUserListMetadataHelper();


    @BeforeClass
    public void beforeClass() {
        admin = new AdminHelper();
        vendorEntityTypeId = ConfigureConstantFields.getEntityIdByName("vendors");
    }

    private int getVendorRecordId() {
        try {
            if (firstVendorRecordId != -1) {
                return firstVendorRecordId;
            }

            String listResponse = ListDataHelper.getListDataResponseVersion2("vendors");

            if (ParseJsonResponse.validJsonResponse(listResponse)) {
                JSONArray jsonArr = new JSONObject(listResponse).getJSONArray("data");

                if (jsonArr.length() == 0) {
                    throw new SkipException("No Record found in ListData API for Vendor Hierarchy.");
                }

                int idColumnNo = ListDataHelper.getColumnIdFromColumnName(listResponse, "id");
                String valueStr = jsonArr.getJSONObject(0).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                firstVendorRecordId = ListDataHelper.getRecordIdFromValue(valueStr);

                return firstVendorRecordId;
            } else {
                logger.error("ListData API Response for Vendor Hierarchy is an Invalid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception while Getting First Vendor Record Id. " + e.getMessage());
        }

        return -1;
    }

    /*
    TC-C7928: Verify that there should be two buttons (Edit and Print) on VH Show Page.
     */
    @Test
    public void testC7928() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C7928: Verify that there should be two buttons (Edit and Print) on VH Show Page.");
            int vendorRecordId = getVendorRecordId();

            String actionsV2Response = Actions.getActionsV2Response(vendorEntityTypeId, vendorRecordId);

            if (ParseJsonResponse.validJsonResponse(actionsV2Response)) {
                List<String> allActionNames = Actions.getAllActionNames(actionsV2Response);

                if (allActionNames == null) {
                    throw new SkipException("Couldn't get All Action Names from Actions V2 Response for Vendor Hierarchy Record Id " + vendorRecordId);
                }

                csAssert.assertTrue(allActionNames.contains("Edit"), "Couldn't find Edit Action on Show Page of Vendor Hierarchy Record Id " + vendorRecordId);
                csAssert.assertTrue(allActionNames.contains("Vendor Hierarchy"), "Couldn't find Print Action on Show Page of Vendor Hierarchy Record Id " +
                        vendorRecordId);
            } else {
                csAssert.assertTrue(false, "Actions V2 API Response for Vendor Hierarchy Record Id " + vendorRecordId + " is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C7928. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C7930: Verify that the name of the client/supplier and currency should be visible in Basic Information Group.
    TC-C7981: Verify that client/supplier name should be correct.
     */
    @Test
    public void testC7930() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C7930: Verify that the name of the client/supplier and currency should be visible in Basic Information Group.");
            int vendorRecordId = getVendorRecordId();

            String showResponse = ShowHelper.getShowResponseVersion2(vendorEntityTypeId, vendorRecordId);

            if (ParseJsonResponse.validJsonResponse(showResponse)) {
                JSONObject jsonObj = new JSONObject(showResponse);
                jsonObj = jsonObj.getJSONObject("body").getJSONObject("data");

                //Verify Client/Supplier Field
                if (jsonObj.has("client")) {
                    JSONObject clientObj = jsonObj.getJSONObject("client");

                    if (clientObj.has("values") && !clientObj.isNull("values")) {
                        if (!clientObj.getJSONObject("values").has("name") || clientObj.getJSONObject("values").isNull("name")) {
                            csAssert.assertTrue(false, "Name not present in [client] JsonObject in Show V2 API Response for VH Record Id " + vendorRecordId);
                        } else {
                            String actualClientName = clientObj.getJSONObject("values").getString("name");
                            String clientShowResponse = ClientShow.getClientShowResponse(admin.getClientId());
                            String expectedClientName = ClientShow.getClientName(clientShowResponse);

                            csAssert.assertTrue(actualClientName.equalsIgnoreCase(expectedClientName), "Expected Client Name: [" + expectedClientName +
                                    "] and Actual Client Name: [" + actualClientName + "]");
                        }
                    } else {
                        csAssert.assertTrue(false, "values JsonObject not present in [client] JsonObject in Show V2 API Response for VH Record Id " +
                                vendorRecordId);
                    }
                } else {
                    csAssert.assertTrue(false, "Show V2 API doesn't have JSONObject [client] for VH Record Id " + vendorRecordId);
                }

                //Verify Currency Field
                if (jsonObj.has("currency")) {
                    JSONObject currencyObj = jsonObj.getJSONObject("currency");

                    if (currencyObj.has("values") && !currencyObj.isNull("values")) {
                        if (!currencyObj.getJSONObject("values").has("name") || currencyObj.getJSONObject("values").isNull("name")) {
                            csAssert.assertTrue(false, "Name not present in [currency] JsonObject in Show V2 API Response for VH Record Id " +
                                    vendorRecordId);
                        }
                    } else {
                        csAssert.assertTrue(false, "values JsonObject not present in [currency] JsonObject in Show V2 API Response for VH Record Id " +
                                vendorRecordId);
                    }
                } else {
                    csAssert.assertTrue(false, "Show V2 API doesn't have JSONObject [currency] for VH Record Id " + vendorRecordId);
                }
            } else {
                csAssert.assertTrue(false, "Show V2 API Response for VH Record Id " + vendorRecordId + " is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C7930. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C7935: Verify the mandatory fields for Vendor Hierarchy.
     */
    @Test
    public void testC7935() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C7935: Verify the mandatory fields for Vendor Hierarchy.");
            int clientId = admin.getClientId();

            String createFormResponse = CreateForm.getCreateFormV2Response(clientId, "vendors");

            if (ParseJsonResponse.validJsonResponse(createFormResponse)) {
                List<String> allRequiredFieldNames = CreateForm.getAllRequiredFieldNames(createFormResponse);

                if (allRequiredFieldNames == null) {
                    throw new SkipException("Couldn't get All Required Field Names from Create Form V2 Response.");
                }

                csAssert.assertTrue(allRequiredFieldNames.contains("name"), "Name is not a mandatory field for Vendor Hierarchy.");
                csAssert.assertTrue(allRequiredFieldNames.contains("functions"), "Functions is not a mandatory field for Vendor Hierarchy.");
                csAssert.assertTrue(allRequiredFieldNames.contains("services"), "Services is not a mandatory field for Vendor Hierarchy.");
                csAssert.assertTrue(allRequiredFieldNames.contains("globalRegions"), "Regions is not a mandatory field for Vendor Hierarchy.");
                csAssert.assertTrue(allRequiredFieldNames.contains("globalCountries"), "Countries is not a mandatory field for Vendor Hierarchy.");
            } else {
                csAssert.assertTrue(false, "Create Form V2 API Response for Client Id " + clientId + " and Vendors is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C7935. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C7867: Verify that User is not able to create and edit a VH when permissions are disabled.
     */
    @Test
    public void testC7867() {
        CustomAssert csAssert = new CustomAssert();

        String userName = ConfigureEnvironment.getEnvironmentProperty("j_username");
        String password = ConfigureEnvironment.getEnvironmentProperty("password");
        Set<String> allPermissions = adminHelperObj.getAllPermissionsForUser(userName, admin.getClientId());

        try {
            logger.info("Starting Test TC-C7867: Verify that User is not able to create and edit VH when permissions are disabled.");

            //Remove Create and Edit Permissions
            if (allPermissions.isEmpty()) {
                throw new SkipException("Couldn't get Permissions for User " + userName);
            }

            //Permission Id for Create: 469 and Edit: 470
            //Remove Create and Edit Permissions
            Set<String> newPermissions = new HashSet<>(allPermissions);
            newPermissions.remove("469");
            newPermissions.remove("470");

            String newPermissionsStr = newPermissions.toString().replace("[", "{").replace("]", "}");

            boolean permissionUpdated = adminHelperObj.updatePermissionsForUser(userName, admin.getClientId(), newPermissionsStr);

            if (!permissionUpdated) {
                throw new SkipException("Couldn't Remove Permissions for Create and Edit VH.");
            }

            new Check().hitCheck(userName,password);
            //Verify Create
            String defaultUserListMetadataResponse = defaultUserListHelperObj.getDefaultUserListMetadataResponse("vendors");

            if (ParseJsonResponse.validJsonResponse(defaultUserListMetadataResponse)) {
                JSONObject jsonObj = new JSONObject(defaultUserListMetadataResponse);

                //Verify popUpUrl
                if (jsonObj.has("popupUrl") && !jsonObj.isNull("popupUrl")) {
                    csAssert.assertTrue(false, "PopUpUrl Property in DefaultUserListMetadata API Response for VH is not null.");
                }

                //Verify isPlusIcon
                if (jsonObj.has("isPlusIcon") && !jsonObj.isNull("isPlusIcon")) {
                    csAssert.assertTrue(false, "IsPlusIcon Property in DefaultUserListMetadata API Response for VH is not null.");
                }
            } else {
                csAssert.assertTrue(false, "DefaultUserListMetadata API Response for VH is an Invalid JSON.");
            }

            //Verify Edit
            String listDataResponse = ListDataHelper.getListDataResponseVersion2("vendors");

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                JSONObject jsonObj = new JSONObject(listDataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                if (jsonArr.length() == 0) {
                    throw new SkipException("Couldn't get any data in ListData API Response for VH.");
                }

                int idColumn = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
                String value = jsonArr.getJSONObject(0).getJSONObject(String.valueOf(idColumn)).getString("value");

                int recordId = ListDataHelper.getRecordIdFromValue(value);

                String actionsResponse = Actions.getActionsV2Response(3, recordId);

                if (ParseJsonResponse.validJsonResponse(actionsResponse)) {
                    List<String> allActionNames = Actions.getAllActionNames(actionsResponse);

                    if (allActionNames == null) {
                        csAssert.assertTrue(false, "Couldn't get All Action Names from Actions API Response for VH Record Id " + recordId);
                    }

                    if (allActionNames.contains("Edit")) {
                        csAssert.assertTrue(false, "VH Record Id " + recordId + " has Edit Action on Show Page.");
                    }
                } else {
                    csAssert.assertTrue(false, "Actions API Response for Vendors Record Id " + recordId + " is an Invalid JSON.");
                }
            } else {
                csAssert.assertTrue(false, "ListData API Response for VH is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C7867. " + e.getMessage());
        } finally {
            adminHelperObj.updatePermissionsForUser(userName, admin.getClientId(),
                    allPermissions.toString().replace("[", "{").replace("]", "}"));
        }

        csAssert.assertAll();
    }


    /*
    TC-C7904: Verify VH Group is present after Global Permission in URG.
    TC-C7908: Verify that Permissions Create, Edit, Show, Show Quick Link and Download Quick Link are present in VH.
     */
    @Test
    public void testC7904() {
        CustomAssert csAssert = new CustomAssert();
        String lastLoggedInEndUserName = Check.lastLoggedInUserName;
        String lastLoggedInEndUserPassword = Check.lastLoggedInUserPassword;

        try {
            logger.info("Validating VH Group is present after Global Permission in URG.");
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

            Elements sections = div.children();
            int count = 0;
            boolean flag = false;
            for (Element section: sections) {
                count ++;
                if(section.child(0).child(0).childNode(0).toString().replace(":", "").trim().equalsIgnoreCase("Vendor Hierarchy"))
                {
                    flag = true;break;
                }
            }
            if (flag) {
                //Verify C7908: Permissions
                String[] expectedPermissionNames = {"Create", "Edit", "Show", "ShowQuickLink", "DownloadQuickLink"};
                List<String> expectedPermissionNamesList = new ArrayList<>(Arrays.asList(expectedPermissionNames));

                Elements allPermissionsTab = div.children().get(count).child(0).child(0).child(0).child(0).children();
                List<String> actualPermissionNamesList = new ArrayList<>();

                for (Element permissionTab : allPermissionsTab) {
                    String permissionName = permissionTab.childNode(5).attributes().get("#text").replaceAll("[\n\t\\s]","");
                    actualPermissionNamesList.add(permissionName);
                }

                for (String expectedPermissionName : expectedPermissionNamesList) {
                    if (!actualPermissionNamesList.contains(expectedPermissionName)) {
                        csAssert.assertTrue(false, "Permission [" + expectedPermissionName + "] not found in VH Group");
                    }
                }

            } else {
                csAssert.assertTrue(false, "Vendor Hierarchy Group is not present after Global Permission in URG Update Response.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C7904. " + e.getMessage());
        } finally {
            new Check().hitCheck(lastLoggedInEndUserName, lastLoggedInEndUserPassword);
        }

        csAssert.assertAll();
    }


    /*
    TC-C7905: Verify VH Group is present while Creating New URG.
     */
    @Test
    public void testC7905() {
        CustomAssert csAssert = new CustomAssert();
        String lastLoggedInEndUserName = Check.lastLoggedInUserName;
        String lastLoggedInEndUserPassword = Check.lastLoggedInUserPassword;

        try {
            logger.info("Validating VH Group is present while Creating URG.");
            adminHelperObj.loginWithClientAdminUser();

            String createResponse = executor.get(MasterUserRoleGroupsCreate.getAPIPath(), MasterUserRoleGroupsCreate.getHeaders()).getResponse().getResponseBody();

            Document html = Jsoup.parse(createResponse);
            Element div = html.getElementsByClass("accordion user_permission").get(0);

            Elements sections = div.children();
            boolean flag = false;
            for (Element section: sections) {
                if(section.child(0).child(0).childNode(0).toString().replace(":", "").trim().equalsIgnoreCase("Vendor Hierarchy"))
                {
                    flag = true; break;
                }
            }

            if (!flag) {
                csAssert.assertTrue(false, "Vendor Hierarchy Group is not present after Global Permission in URG Create Response.");
            }

        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C7905. " + e.getMessage());
        } finally {
            new Check().hitCheck(lastLoggedInEndUserName, lastLoggedInEndUserPassword);
        }

        csAssert.assertAll();
    }
}