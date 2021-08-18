package com.sirionlabs.test.workflowPod;

import com.sirionlabs.api.bulkedit.BulkeditCreate;
import com.sirionlabs.api.clientAdmin.fieldProvisioning.FieldProvisioning;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.DefaultUserListMetadataHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class TestFlowDownBulkFeaturesProvisioning {

    private final static Logger logger = LoggerFactory.getLogger(TestFlowDownBulkFeaturesProvisioning.class);

    private FieldProvisioning fieldProObj = new FieldProvisioning();

    private String[] allChildEntities = {"contracts", "service levels", "child service levels", "obligations", "child obligations", "change requests", "actions", "issues"};
    private int crgId = 2000;
    private String crgNameInProvisioning = null;
    private Map<String, String> entityRGShortNameMap = new HashMap<>();

    private Check checkObj = new Check();
    private DefaultUserListMetadataHelper defaultObj = new DefaultUserListMetadataHelper();

    @BeforeClass
    public void beforeClass() throws SQLException {
        new AdminHelper().loginWithClientAdminUser();

        PostgreSQLJDBC postgresObj = new PostgreSQLJDBC();
        crgNameInProvisioning = "Stakeholders - " + postgresObj.doSelect("select name from role_group where id = " + crgId).get(0).get(0);

        for (String entity : allChildEntities) {
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(entity);
            String rgShortName = postgresObj
                    .doSelect("select short_name from role_group where flowdown_rolegroup = 2000 and entity_type_id =" + entityTypeId).get(0).get(0);

            entityRGShortNameMap.put(entity, rgShortName);
        }

        postgresObj.closeConnection();
    }

    @AfterClass
    public void afterClass() {
        checkObj.hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));
    }

    /*
    TC-C90743: Verify CRG Bulk Edit option is not supported from Client Admin.
     */
    @Test
    public void testC90743() {
        CustomAssert csAssert = new CustomAssert();

        logger.info("Starting Test TC-C90743: Verify CRG Bulk Edit option is not supported from Client Admin.");

        for (String childEntityName : allChildEntities) {
            try {
                int entityTypeId = ConfigureConstantFields.getEntityIdByName(childEntityName);
                String fieldProvisioningResponse = fieldProObj.hitFieldProvisioning(entityTypeId);

                JSONObject jsonObj = new JSONObject(fieldProvisioningResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");
                boolean fieldFound = false;

                for (int i = 0; i < jsonArr.length(); i++) {
                    jsonObj = jsonArr.getJSONObject(i);
                    String fieldName = jsonObj.getString("fieldName");

                    if (fieldName.equalsIgnoreCase(crgNameInProvisioning)) {
                        fieldFound = true;

                        //Verify CRG flag is true.
                        csAssert.assertEquals(jsonObj.getBoolean("crg"), true, "CRG Flag is false for CRG Field " + crgNameInProvisioning +
                                " in Field Provisioning for Child Entity " + childEntityName);

                        //Verify Bulk Edit Option is disabled.
                        boolean bulkEditDisabled = (jsonObj.isNull("bulkEdit") || !jsonObj.getBoolean("bulkEdit"));
                        csAssert.assertEquals(bulkEditDisabled, true, "Bulk Edit not Disabled for CRG Field " +
                                crgNameInProvisioning + " in Field Provisioning for Child Entity " + childEntityName);

                        break;
                    }
                }

                csAssert.assertEquals(fieldFound, true, "CRG Field " + crgNameInProvisioning + " not found in Field Provisioning for Child Entity " +
                        childEntityName);
            } catch (Exception e) {
                csAssert.assertFalse(true, "Exception while Validating TC-C90743 for Child Entity " + childEntityName + ". " + e.getMessage());
            }
        }

        csAssert.assertAll();
    }

    /*
    TC-C90745: Verify CRG Bulk Update option is not supported for both Read and Write mode from Client Admin.
     */
    @Test (priority = 1)
    public void testC90745() {
        CustomAssert csAssert = new CustomAssert();

        logger.info("Starting Test TC-C90745: Verify CRG Bulk Update Option is not supported for both Read and Write mode from Client Admin.");

        for (String childEntityName : allChildEntities) {
            try {
                int entityTypeId = ConfigureConstantFields.getEntityIdByName(childEntityName);
                String fieldProvisioningResponse = fieldProObj.hitFieldProvisioning(entityTypeId);

                JSONObject jsonObj = new JSONObject(fieldProvisioningResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");
                boolean fieldFound = false;

                for (int i = 0; i < jsonArr.length(); i++) {
                    jsonObj = jsonArr.getJSONObject(i);
                    String fieldName = jsonObj.getString("fieldName");

                    if (fieldName.equalsIgnoreCase(crgNameInProvisioning)) {
                        fieldFound = true;

                        //Verify Bulk Update Flag is false.
                        csAssert.assertEquals(jsonObj.getBoolean("bulkUpdate"), false, "Bulk Update Flag is true for CRG Field " +
                                crgNameInProvisioning + " in Field Provisioning for Child Entity " + childEntityName);

                        //Verify Bulk Update Available is null.
                        csAssert.assertEquals(jsonObj.isNull("bulkUpdateAvailable"), true, "Bulk Update Available is not null for CRG Field " +
                                crgNameInProvisioning + " in Field Provisioning for Child Entity " + childEntityName);

                        //Verify Bulk Update Read Only flag is false.
                        csAssert.assertEquals(jsonObj.getBoolean("bulkUpdateReadOnly"), false, "Bulk Update Read Only Flag is true for CRG Field " +
                                crgNameInProvisioning + " in Field Provisioning for Child Entity " + childEntityName);

                        //Verify Bulk Update Read Available is null.
                        csAssert.assertEquals(jsonObj.isNull("bulkUpdateReadAvailable"), true, "Bulk Update Read Available is not null for CRG Field " +
                                crgNameInProvisioning + " in Field Provisioning for Child Entity " + childEntityName);

                        break;
                    }
                }

                csAssert.assertEquals(fieldFound, true, "CRG Field " + crgNameInProvisioning + " not found in Field Provisioning for Child Entity " +
                        childEntityName);
            } catch (Exception e) {
                csAssert.assertFalse(true, "Exception while Validating TC-C90745 for Child Entity " + childEntityName + ". " + e.getMessage());
            }
        }

        csAssert.assertAll();
    }

    /*
    TC-C90747: Verify Bulk Create Option is not supported from Client Admin.
    TC-C90748
     */
    @Test (priority = 2)
    public void testC90747() {
        CustomAssert csAssert = new CustomAssert();

        logger.info("Starting Test TC-C90747: Verify CRG Bulk Create Option is not supported from Client Admin.");

        for (String childEntityName : allChildEntities) {
            try {
                int entityTypeId = ConfigureConstantFields.getEntityIdByName(childEntityName);
                String fieldProvisioningResponse = fieldProObj.hitFieldProvisioning(entityTypeId);

                JSONObject jsonObj = new JSONObject(fieldProvisioningResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");
                boolean fieldFound = false;

                for (int i = 0; i < jsonArr.length(); i++) {
                    jsonObj = jsonArr.getJSONObject(i);
                    String fieldName = jsonObj.getString("fieldName");

                    if (fieldName.equalsIgnoreCase(crgNameInProvisioning)) {
                        fieldFound = true;

                        csAssert.assertEquals(jsonObj.getBoolean("bulkCreate"), false, "Bulk Create is not disabled for CRG Field " +
                                crgNameInProvisioning + " in Field Provisioning for Child Entity " + childEntityName);

                        csAssert.assertEquals(jsonObj.isNull("bulkCreateMandatory"), true, "Bulk Create Mandatory is not null for CRG Field " +
                                crgNameInProvisioning + " in Field Provisioning for Child Entity " + childEntityName);

                        break;
                    }
                }

                csAssert.assertEquals(fieldFound, true, "CRG Field " + crgNameInProvisioning + " not found in Field Provisioning for Child Entity " +
                        childEntityName);
            } catch (Exception e) {
                csAssert.assertFalse(true, "Exception while Validating TC-C90747 for Child Entity " + childEntityName + ". " + e.getMessage());
            }
        }

        csAssert.assertAll();
    }

    /*
    TC-C90470: Verify CRGs are not displayed Bulk Edit, Update and Create form at End-User
     */
    @Test (priority = 3)
    public void testC90470() {
        CustomAssert csAssert = new CustomAssert();

        logger.info("Starting Test TC-C90470: Verify CRGs are not displayed Bulk Edit, Update and Create form at End-User.");
        BulkeditCreate createObj = new BulkeditCreate();

        checkObj.hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));

        for (String childEntityName : allChildEntities) {
            try {
                int entityTypeId = ConfigureConstantFields.getEntityIdByName(childEntityName);
                String listDataResponse = ListDataHelper.getListDataResponseVersion2(childEntityName);
                JSONObject jsonObj = new JSONObject(listDataResponse).getJSONArray("data").getJSONObject(0);
                String idColumn = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
                String idValue = jsonObj.getJSONObject(idColumn).getString("value");
                int recordId = ListDataHelper.getRecordIdFromValue(idValue);

                //Validate Field Disabled for Bulk Edit.
                String defaultUserListMetadataResponse = defaultObj.getDefaultUserListMetadataResponse(childEntityName);
                jsonObj = new JSONObject(defaultUserListMetadataResponse);

                if (jsonObj.getBoolean("bulkEdit")) {
                    String bulkEditCreateResponse = createObj.hitBulkeditCreate(entityTypeId, "{\"entityIds\":[" + recordId + "]}");

                    Map<String, String> fieldProperties = ParseJsonResponse.getFieldByName(bulkEditCreateResponse, entityRGShortNameMap.get(childEntityName));
                    if (!fieldProperties.isEmpty()) {
                        csAssert.assertEquals(fieldProperties.get("displayMode"), "display", "CRG Field having Short Name " +
                                entityRGShortNameMap.get(childEntityName) + " is Editable in Bulk Edit Create Response for Child Entity " + childEntityName);
                    }
                }
            } catch (Exception e) {
                csAssert.assertFalse(true, "Exception while Validating TC-C90470 for Child Entity: " + childEntityName + ". " + e.getMessage());
            }
        }

        csAssert.assertAll();
    }
}