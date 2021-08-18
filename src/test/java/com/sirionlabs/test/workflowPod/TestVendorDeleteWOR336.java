package com.sirionlabs.test.workflowPod;

import com.sirionlabs.api.clientAdmin.VendorDelete;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.VendorHierarchy;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

@Listeners(value = MyTestListenerAdapter.class)
public class TestVendorDeleteWOR336 {

    private final static Logger logger = LoggerFactory.getLogger(TestVendorDeleteWOR336.class);

    private int clientId;

    @BeforeClass
    public void beforeClass() {
        clientId = new AdminHelper().getClientId();
    }

    @Test
    public void testVendorDeleteAPI() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating Vendor Delete API");

            //Create Vendor
            logger.info("Creating Vendor Hierarchy");
            String createResponse = VendorHierarchy.createVendorHierarchy();

            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                JSONObject jsonObj = new JSONObject(createResponse);
                String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();

                if (!createStatus.equalsIgnoreCase("success")) {
                    throw new SkipException("Couldn't Create Vendor Hierarchy.");
                }

                int vendorId = CreateEntity.getNewEntityId(createResponse, "suppliers");

                logger.info("Vendor Created Successfully with Id {}: ", vendorId);

                String userName = ConfigureEnvironment.getEnvironmentProperty("clientUsername");
                AdminHelper adminHelperObj = new AdminHelper();
                Set<String> allPermissions = adminHelperObj.getAllPermissionsForUser(userName, clientId);

                if (allPermissions.isEmpty()) {
                    throw new SkipException("Couldn't get Permissions for User " + userName);
                }

                Set<String> newPermissions = new HashSet<>(allPermissions);
                String deletePermissionId = "920";
                boolean permissionUpdated;
                String newPermissionsStr;

                if (newPermissions.contains(deletePermissionId)) {
                    newPermissions.remove(deletePermissionId);

                    newPermissionsStr = newPermissions.toString().replace("[", "{").replace("]", "}");

                    permissionUpdated = adminHelperObj.updatePermissionsForUser(userName, clientId, newPermissionsStr);

                    if (!permissionUpdated) {
                        throw new SkipException("Couldn't Remove Permissions for Delete Vendor.");
                    }
                }

                //Validate that without permission the Delete API is not working.
                logger.info("Hitting Delete API when permission is not given to user.");
                String deleteResponse = VendorDelete.getDeleteResponse(vendorId).getResponseBody();

                if (ParseJsonResponse.validJsonResponse(deleteResponse)) {
                    jsonObj = new JSONObject(deleteResponse).getJSONObject("header").getJSONObject("response");

                    csAssert.assertTrue(jsonObj.getString("status").equalsIgnoreCase("applicationError"),
                            "Delete API Validation failed when permission is not given. Expected Status: applicationError and Actual Status: " +
                                    jsonObj.getString("status"));

                    csAssert.assertTrue(jsonObj.getString("errorMessage").toLowerCase().contains("either you do not have the required permissions"),
                            "Delete API Validation failed when permission is not given. Expected Error Message: Either You do not have the required permissions " +
                                    " and Actual Error Message: " + jsonObj.getString("errorMessage"));
                } else {
                    csAssert.assertTrue(false, "Delete API Response when Permission is not given is an Invalid JSON.");
                }

                //Validate that with permission the Delete API is working fine.
                if (!newPermissions.contains(deletePermissionId)) {
                    newPermissions.add(deletePermissionId);

                    newPermissionsStr = newPermissions.toString().replace("[", "{").replace("]", "}");

                    adminHelperObj.updatePermissionsForUser(userName, clientId, newPermissionsStr);
                }

                logger.info("Hitting Delete API when permission is given to user.");
                deleteResponse = VendorDelete.getDeleteResponse(vendorId).getResponseBody();

                if (ParseJsonResponse.validJsonResponse(deleteResponse)) {
                    jsonObj = new JSONObject(deleteResponse);

                    csAssert.assertTrue(jsonObj.getString("status").equalsIgnoreCase("success"),
                            "Couldn't Delete Vendor Id " + vendorId + " when permission is given to user. Status: " + jsonObj.getString("status"));
                } else {
                    csAssert.assertTrue(false, "Delete API Response when Permission is given is an Invalid JSON.");
                }
            } else {
                throw new SkipException("Couldn't create Vendor Hierarchy.");
            }
        } catch (SkipException e) {
            logger.warn("Skipping case: " + e.getMessage());
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while validating Vendor Delete API. " + e.getMessage());
        }

        csAssert.assertAll();
    }
}