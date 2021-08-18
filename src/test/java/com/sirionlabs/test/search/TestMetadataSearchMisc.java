package com.sirionlabs.test.search;

import com.sirionlabs.api.clientAdmin.fieldProvisioning.FieldProvisioning;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.GetEntityId;
import com.sirionlabs.api.metadataSearch.MetadataSearch;
import com.sirionlabs.api.searchLayout.SearchLayoutEntityTypes;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestMetadataSearchMisc {

    private final static Logger logger = LoggerFactory.getLogger(TestMetadataSearchMisc.class);

    private List<Map<String, String>> allEntitiesMap;
    private MetadataSearch searchObj = new MetadataSearch();
    private FieldProvisioning fieldProvisioningObj = new FieldProvisioning();
    private AdminHelper adminHelperObj = new AdminHelper();

    @BeforeClass
    public void beforeClass() {
        SearchLayoutEntityTypes entityTypesObj = new SearchLayoutEntityTypes();
        String entityTypesResponse = entityTypesObj.hitSearchLayoutEntityTypes();

        allEntitiesMap = SearchLayoutEntityTypes.getMetadataEntityTypes(entityTypesResponse);
    }

    @Test
    public void testC89182() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating TC-C89182.");

            if (allEntitiesMap.isEmpty()) {
                throw new SkipException("Couldn't find any entity in Search Layout Entity Types Response.");
            }


            for (Map<String, String> entityMap : allEntitiesMap) {
                int entityTypeId = Integer.parseInt(entityMap.get("id"));
                String entityName = entityMap.get("name");

                logger.info("Validating test for Entity {}", entityName);
                String searchResponse = searchObj.hitMetadataSearch(entityTypeId);

                List<String> allFieldNames = ParseJsonResponse.getAllFieldNames(searchResponse);
                if (allFieldNames.isEmpty()) {
                    throw new SkipException("Couldn't get all Field Names from Search Response for Entity " + entityName);
                }

                csAssert.assertTrue(!allFieldNames.contains("id"), "Id Field Present in Metadata Search for Entity " + entityName);

                //Validate TC-C88914
                validateC88914(entityName, entityTypeId, allFieldNames, csAssert);

            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C89182. " + e.getMessage());
        }
        csAssert.assertAll();
    }

    private void validateC88914(String entityName, int entityTypeId, List<String> allFieldNames, CustomAssert csAssert) {
        try {
            String fieldProvisioningResponse = fieldProvisioningObj.getFieldProvisioningResponse(entityTypeId);
            List<Map<String, String>> allInactiveFields = fieldProvisioningObj.getAllInactiveFields(fieldProvisioningResponse);

            for (Map<String, String> inactiveFieldMap : allInactiveFields) {
                String fieldApiName = inactiveFieldMap.get("apiName");
                csAssert.assertTrue(!allFieldNames.contains(fieldApiName), "Inactive Field " + fieldApiName + " present in Metadata Search for Entity " +
                        entityName);
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C88914 for Entity " + entityName + ". " + e.getMessage());
        }
    }

    /*
    TC-C10199: Verify error message for incorrect entity id search
     */
    @Test
    public void testC10199() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating Test C10199: Verify error message for incorrect entity id search");
            GetEntityId getEntityIdObj = new GetEntityId();
            String response = getEntityIdObj.hitGetEntityId("contracts", "11889900");

            csAssert.assertEquals(response, "", "Expected Empty Response but Actual Response: [" + response + "]");
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C10199. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C10252: Verify Document Tree Search Access can be managed via Client Admin
     */
    @Test
    public void testC10252() {
        CustomAssert csAssert = new CustomAssert();

        String userName = ConfigureEnvironment.getEnvironmentProperty("j_username");
        Set<String> allPermissions = adminHelperObj.getAllPermissionsForUser(userName, 1002);

        try {
            logger.info("Validating Test TC-C10252: Verify Document Tree Search Access can be managed via Client Admin");

            //Remove Document Tree Search Permission
            if (allPermissions.isEmpty()) {
                throw new SkipException("Couldn't get Permissions for User " + userName);
            }

            //Permission Id for Document Tree Search: 518
            Set<String> newPermissions = new HashSet<>(allPermissions);
            newPermissions.remove("518");

            String newPermissionsStr = newPermissions.toString().replace("[", "{").replace("]", "}");

            boolean permissionUpdated = adminHelperObj.updatePermissionsForUser(userName, 1002, newPermissionsStr);

            if (!permissionUpdated) {
                throw new SkipException("Couldn't Remove Document Tree Search Permission.");
            }

            // Refreshing the session
            Check checkObj = new Check();
            checkObj.hitCheck("anay_user", "admin1234a");

            SearchLayoutEntityTypes entityTypesObj = new SearchLayoutEntityTypes();
            String response = entityTypesObj.hitSearchLayoutEntityTypes();

            JSONArray searchTypes = new JSONObject(response).getJSONArray("searchTypes");
            boolean documentTreeSearchFound = false;

            for (int i = 0; i < searchTypes.length(); i++) {
                if (searchTypes.getJSONObject(i).getInt("id") == 1) {
                    documentTreeSearchFound = true;
                    break;
                }
            }

            csAssert.assertFalse(documentTreeSearchFound, "Document Tree still available to user after disabling permission.");
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C10252. " + e.getMessage());
        } finally {
            adminHelperObj.updatePermissionsForUser(userName, 1002,
                    allPermissions.toString().replace("[", "{").replace("]", "}"));
        }

        csAssert.assertAll();
    }


    /*
    TC-C10206: Verify Attachment Search Access can be managed via Client Admin
     */
    @Test
    public void testC10206() {
        CustomAssert csAssert = new CustomAssert();

        String userName = ConfigureEnvironment.getEnvironmentProperty("j_username");
        Set<String> allPermissions = adminHelperObj.getAllPermissionsForUser(userName, 1002);

        try {
            logger.info("Validating Test TC-C10206: Verify Attachment Search Access can be managed via Client Admin");

            //Remove Attachment Search Permission
            if (allPermissions.isEmpty()) {
                throw new SkipException("Couldn't get Permissions for User " + userName);
            }

            //Permission Id for Document Tree Search: 519
            Set<String> newPermissions = new HashSet<>(allPermissions);
            newPermissions.remove("519");

            String newPermissionsStr = newPermissions.toString().replace("[", "{").replace("]", "}");

            boolean permissionUpdated = adminHelperObj.updatePermissionsForUser(userName, 1002, newPermissionsStr);

            if (!permissionUpdated) {
                throw new SkipException("Couldn't Remove Attachment Search Permission.");
            }

            // Refreshing the session
            Check checkObj = new Check();
            checkObj.hitCheck("anay_user", "admin1234a");

            SearchLayoutEntityTypes entityTypesObj = new SearchLayoutEntityTypes();
            String response = entityTypesObj.hitSearchLayoutEntityTypes();

            JSONArray searchTypes = new JSONObject(response).getJSONArray("searchTypes");
            boolean attachmentSearchFound = false;

            for (int i = 0; i < searchTypes.length(); i++) {
                if (searchTypes.getJSONObject(i).getInt("id") == 2) {
                    attachmentSearchFound = true;
                    break;
                }
            }

            csAssert.assertFalse(attachmentSearchFound, "Attachment still available to user after disabling permission.");
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C10206. " + e.getMessage());
        } finally {
            adminHelperObj.updatePermissionsForUser(userName, 1002,
                    allPermissions.toString().replace("[", "{").replace("]", "}"));
        }

        csAssert.assertAll();
    }
}