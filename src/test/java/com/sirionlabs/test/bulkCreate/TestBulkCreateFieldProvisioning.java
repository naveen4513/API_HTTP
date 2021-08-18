package com.sirionlabs.test.bulkCreate;

import com.sirionlabs.api.clientAdmin.fieldProvisioning.AuditLogs;
import com.sirionlabs.api.clientAdmin.fieldProvisioning.CreatePage;
import com.sirionlabs.api.clientAdmin.fieldProvisioning.EntityTypes;
import com.sirionlabs.api.clientAdmin.fieldProvisioning.FieldProvisioning;
import com.sirionlabs.api.clientAdmin.masterRoleGroups.MasterRoleGroupsList;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.bulk.BulkOperationsHelper;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestBulkCreateFieldProvisioning extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestBulkCreateFieldProvisioning.class);
    private String fieldProvisioningResponseForActions;
    private String fieldProvisioningResponseForServiceData;
    private FieldProvisioning fieldProvisioningObj = new FieldProvisioning();
    private List<Map<String, String>> allDisabledBulkCreateFieldsForAction;

    private BulkOperationsHelper bulkOperationsHelperObj = new BulkOperationsHelper();

    @BeforeClass
    public void beforeClass() {
        int actionsEntityTypeId = ConfigureConstantFields.getEntityIdByName("actions");
        fieldProvisioningResponseForActions = fieldProvisioningObj.getFieldProvisioningResponse(actionsEntityTypeId);

        int serviceDataEntityTypeId = ConfigureConstantFields.getEntityIdByName("service data");
        fieldProvisioningResponseForServiceData = fieldProvisioningObj.getFieldProvisioningResponse(serviceDataEntityTypeId);

        allDisabledBulkCreateFieldsForAction = fieldProvisioningObj.getAllDisabledBulkCreateFields(actionsEntityTypeId);
    }

    /*
    TC-C3452: Verify Metadata Field 'Financial Information - Currency' should be disabled for Bulk Create
     */
    @Test
    public void testC3452() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C3452: Verify Metadata Field 'Financial Information - Currency' is disabled for Bulk Create for Action Entity.");

            if (allDisabledBulkCreateFieldsForAction == null || allDisabledBulkCreateFieldsForAction.isEmpty()) {
                throw new SkipException("Couldn't get All Disabled Bulk Create Fields for Action Entity.");
            }

            boolean financialInformationCurrencyFieldFound = false;

            for (Map<String, String> disabledFieldMap : allDisabledBulkCreateFieldsForAction) {
                String id = disabledFieldMap.get("id");

                if (id.equalsIgnoreCase("439")) {
                    financialInformationCurrencyFieldFound = true;
                    break;
                }
            }

            csAssert.assertTrue(financialInformationCurrencyFieldFound,
                    "Financial Information - Currency Field not found in All Disabled Bulk Create Fields for Action Entity.");
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C3452. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C3327: Verify Metadata Field 'Basic Information - GB and GB Meeting' should be disabled for Bulk Create
     */
    @Test
    public void testC3327() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C3327: Verify Metadata Field 'Basic Information - GB and GB Meeting' is disabled for Bulk Create.");

            if (allDisabledBulkCreateFieldsForAction == null || allDisabledBulkCreateFieldsForAction.isEmpty()) {
                throw new SkipException("Couldn't get All Disabled Bulk Create Fields for Action Entity.");
            }

            boolean gbFieldFound = false;
            boolean gbMeetingFieldFound = false;

            for (Map<String, String> disabledFieldMap : allDisabledBulkCreateFieldsForAction) {
                String id = disabledFieldMap.get("id");

                if (id.equalsIgnoreCase("412")) {
                    gbFieldFound = true;
                } else if (id.equalsIgnoreCase("452")) {
                    gbMeetingFieldFound = true;
                }
            }

            csAssert.assertTrue(gbFieldFound, "Basic Information - GB Field not found in All Disabled Bulk Create Fields for Action Entity.");
            csAssert.assertTrue(gbMeetingFieldFound, "Basic Information - GB Meeting Field not found in All Disabled Bulk Create Fields for Action Entity.");
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C3327. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C3280: Verify Metadata Support - Status, Source and Source Title/Name columns in the Field Provisioning should be disabled
     */
    @Test
    public void testC3280() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C3280: Verify Fields Status, Source and Source Title/Name should be disabled in Field Provisioning.");

            if (allDisabledBulkCreateFieldsForAction == null || allDisabledBulkCreateFieldsForAction.isEmpty()) {
                throw new SkipException("Couldn't get All Disabled Bulk Create Fields for Action Entity.");
            }

            boolean statusFieldFound = false;
            boolean sourceFieldFound = false;
            boolean sourceTitleFieldFound = false;

            for (Map<String, String> disabledFieldMap : allDisabledBulkCreateFieldsForAction) {
                String id = disabledFieldMap.get("id");

                if (id.equalsIgnoreCase("402")) {
                    statusFieldFound = true;
                } else if (id.equalsIgnoreCase("407")) {
                    sourceFieldFound = true;
                } else if (id.equalsIgnoreCase("408")) {
                    sourceTitleFieldFound = true;
                }
            }

            csAssert.assertTrue(statusFieldFound, "Basic Information - Status Field not found in All Disabled Bulk Create Fields for Action Entity.");
            csAssert.assertTrue(sourceFieldFound, "Basic Information - Source Field not found in All Disabled Bulk Create Fields for Action Entity.");
            csAssert.assertTrue(sourceTitleFieldFound, "Basic Information - Source Name/Title field not found in All Disabled Bulk Create Fields for Action Entity.");
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C3280. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C3282: Verify Metadata Support - Comments and Attachments (Version 2.0) fields should be disabled for Bulk Create
     */
    @Test
    public void testC3282() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-3282: Verify that Comments and Attachments fields are disabled for Bulk Create.");

            if (allDisabledBulkCreateFieldsForAction == null || allDisabledBulkCreateFieldsForAction.isEmpty()) {
                throw new SkipException("Couldn't get All Disabled Bulk Create Fields for Action Entity.");
            }

            boolean commentsAndAttachmentsAddYourCommentsFieldFound = false;
            boolean commentsAndAttachmentsPrivacyFieldFound = false;
            boolean commentsAndAttachmentsActualDateFieldFound = false;
            boolean commentsAndAttachmentsRequestedByFieldFound = false;
            boolean commentsAndAttachmentsCRFieldFound = false;
            boolean commentsAndAttachmentsWORFieldFound = false;

            logger.info("Validating that Comments And Attachments Version 2.0 Fields - Add Your Comments, Privacy, Actual Date, Requested By, Change Request, " +
                    "Work Order Request are disabled in Field Provisioning for Bulk Create.");

            for (Map<String, String> disabledFieldMap : allDisabledBulkCreateFieldsForAction) {
                String id = disabledFieldMap.get("id");

                if (id.equalsIgnoreCase("12238")) {
                    commentsAndAttachmentsAddYourCommentsFieldFound = true;
                } else if (id.equalsIgnoreCase("12242")) {
                    commentsAndAttachmentsPrivacyFieldFound = true;
                } else if (id.equalsIgnoreCase("12243")) {
                    commentsAndAttachmentsActualDateFieldFound = true;
                } else if (id.equalsIgnoreCase("12244")) {
                    commentsAndAttachmentsRequestedByFieldFound = true;
                } else if (id.equalsIgnoreCase("12246")) {
                    commentsAndAttachmentsCRFieldFound = true;
                } else if (id.equalsIgnoreCase("12247")) {
                    commentsAndAttachmentsWORFieldFound = true;
                }
            }

            csAssert.assertTrue(commentsAndAttachmentsAddYourCommentsFieldFound,
                    "Comments and Attachments Version 2.0 - Add Your Comments Field not found in All Disabled Bulk Create Fields for Action Entity.");
            csAssert.assertTrue(commentsAndAttachmentsPrivacyFieldFound,
                    "Comments and Attachments Version 2.0 - Privacy Field not found in All Disabled Bulk Create Fields for Action Entity.");
            csAssert.assertTrue(commentsAndAttachmentsActualDateFieldFound,
                    "Comments and Attachments Version 2.0 - Actual Date Field not found in All Disabled Bulk Create Fields for Action Entity.");
            csAssert.assertTrue(commentsAndAttachmentsRequestedByFieldFound,
                    "Comments and Attachments Version 2.0 - Requested By Field not found in All Disabled Bulk Create Fields for Action Entity.");
            csAssert.assertTrue(commentsAndAttachmentsCRFieldFound,
                    "Comments and Attachments Version 2.0 - Change Request Field not found in All Disabled Bulk Create Fields for Action Entity.");
            csAssert.assertTrue(commentsAndAttachmentsWORFieldFound,
                    "Comments and Attachments Version 2.0 - Work Order Request Field not found in All Disabled Bulk Create Fields for Action Entity.");
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C3282. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C3304: Verify that All StakeHolders Role Group Fields are present in Field Provisioning
     */
    @Test(enabled = false)
    public void testC3304() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C3304: Verify that All StakeHolder Role Group Fields are present in Field Provisioning.");
            String roleGroupsListResponse = MasterRoleGroupsList.getRoleGroupsListResponse();

            List<Map<String, String>> allRoleGroupsOfEntity = MasterRoleGroupsList.getAllRoleGroupsOfEntity(roleGroupsListResponse, "Actions");

            if (allRoleGroupsOfEntity == null || allRoleGroupsOfEntity.isEmpty()) {
                throw new SkipException("Couldn't get All Role Groups of Entity Actions.");
            }

            List<Map<String, String>> allBulkCreateFields = fieldProvisioningObj.getAllBulkCreateFields(fieldProvisioningResponseForActions);

            if (allBulkCreateFields == null || allBulkCreateFields.isEmpty()) {
                throw new SkipException("Couldn't get All Bulk Create Fields for Action Entity.");
            }

            for (Map<String, String> roleGroupMap : allRoleGroupsOfEntity) {
                String expectedApiName = "rg_" + roleGroupMap.get("roleGroupId");
                boolean roleGroupFound = false;

                logger.info("Validating if Role Group {} is present in Field Provisioning or not.", roleGroupMap.get("displayName"));

                for (Map<String, String> fieldMap : allBulkCreateFields) {
                    String apiName = fieldMap.get("apiName");

                    if (apiName.trim().equalsIgnoreCase(expectedApiName)) {
                        roleGroupFound = true;
                        break;
                    }
                }

                if (!roleGroupFound) {
                    csAssert.assertTrue(false, "Role Group having Display Name [" + roleGroupMap.get("displayName") + "] and RoleGroupId " +
                            roleGroupMap.get("roleGroupId") + " not found in Field Provisioning for Actions Entity.");
                }
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C3304. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C3273: Verify that User is able to Create New Field Provisioning for specific Supplier
     */
    @Test
    public void testC3273() {
        CustomAssert csAssert = new CustomAssert();
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        try {
            logger.info("Starting Test TC-C3273: Verify that User is able to Create New Field Provisioning for Specific Supplier.");
            AdminHelper adminHelperObj = new AdminHelper();
            adminHelperObj.loginWithClientAdminUser();

            String createPageResponse = executor.get(CreatePage.getAPIPath(), CreatePage.getHeaders()).getResponse().getResponseBody();
            List<Integer> allSupplierIds = CreatePage.getAllSupplierIdsFromCreatePageResponse(createPageResponse);

            if (allSupplierIds == null || allSupplierIds.isEmpty()) {
                throw new SkipException("Couldn't get All Supplier Ids from Field Provisioning Create Page Response.");
            }

            int actionsEntityTypeId = ConfigureConstantFields.getEntityIdByName("actions");

            for (Integer supplierId : allSupplierIds) {
                String entityTypesResponse = executor.get(EntityTypes.getAPIPath(supplierId), EntityTypes.getHeaders()).getResponse().getResponseBody();
                Boolean hasEntity = EntityTypes.hasEntityTypeId(entityTypesResponse, actionsEntityTypeId);

                if (hasEntity != null && hasEntity) {
                    String fieldProvisioningResponse = fieldProvisioningObj.hitFieldProvisioning(actionsEntityTypeId, supplierId);
                    JSONObject jsonObj = new JSONObject(fieldProvisioningResponse);
                    String payloadForFieldProvisioning = jsonObj.getJSONArray("data").toString();

                    logger.info("Creating New Field Provisioning for Actions Entity and Supplier Id {}", supplierId);
                    Integer responseCode = fieldProvisioningObj.hitFieldProvisioning(actionsEntityTypeId, supplierId, payloadForFieldProvisioning);

                    if (responseCode == 200) {
                        //Get Newly Created Provisioning Id
                        int provisioningId = fieldProvisioningObj.getNewlyCreatedProvisioningId(1002, supplierId, actionsEntityTypeId);

                        if (provisioningId == -1) {
                            throw new SkipException("Couldn't get Newly Created Provisioning Id for Actions Entity and Supplier Id " + supplierId);
                        }

                        boolean deleteProvisioningData = fieldProvisioningObj.deleteProvisioningData(provisioningId);

                        if (!deleteProvisioningData) {
                            csAssert.assertTrue(false, "Couldn't Delete Provisioning Data for Id " + provisioningId);
                        }
                    } else {
                        throw new SkipException("Couldn't Create New Field Provisioning for Actions Entity and Supplier Id " + supplierId);
                    }

                    break;
                }
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C3273. " + e.getMessage());
        }
        Check checkObj = new Check();
        checkObj.hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);

        csAssert.assertAll();
    }


    /*
    TC-C3274: Verify that all the Custom Field created for Action Entity should come in Field Provisioning Page.
    TC-C3284: Verify field provisioning for Action on inactive dynamic fields.
     */
    @Test
    public void testC3274() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C3274: Verify that all the Custom Field created for Action Entity should come in Field Provisioning Page.");
            List<Map<String, String>> allBulkCreateFields = fieldProvisioningObj.getAllBulkCreateFields(fieldProvisioningResponseForActions);

            if (allBulkCreateFields == null || allBulkCreateFields.isEmpty()) {
                throw new SkipException("Couldn't get All Bulk Create Fields.");
            }

            int actionsEntityTypeId = ConfigureConstantFields.getEntityIdByName("actions");
            List<Map<String, String>> allDynamicFieldsOfAction = bulkOperationsHelperObj.getAllDynamicFieldsOfEntity(actionsEntityTypeId);

            if (allDynamicFieldsOfAction == null) {
                throw new SkipException("Couldn't get All Dynamic Fields of Action Entity.");
            }

            for (Map<String, String> dynamicFieldMap : allDynamicFieldsOfAction) {
                String id = dynamicFieldMap.get("id");
                String active = dynamicFieldMap.get("active");
                String apiName = dynamicFieldMap.get("apiName");
                boolean dynamicFieldFound = false;

                for (Map<String, String> bulkCreateField : allBulkCreateFields) {
                    if (bulkCreateField.get("id").equalsIgnoreCase(id)) {
                        dynamicFieldFound = true;

                        if (!bulkCreateField.get("active").equalsIgnoreCase(active)) {
                            csAssert.assertTrue(false, "Expected Active Status: [" + active + "] and Actual Status: [" +
                                    bulkCreateField.get("active") + "] for Field having Id " + id + " and ApiName " + apiName);
                        }
                        break;
                    }
                }

                csAssert.assertTrue(dynamicFieldFound, "Couldn't find Dynamic Field having Id " + id + ", ApiName: " + apiName);
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C3274. " + e.getMessage());
        }
        csAssert.assertAll();
    }

    /*
    TC-C3278: Verify Audit Log in Field Provisioning
     */
    @Test(enabled = false)
    public void testC3278() {
        CustomAssert csAssert = new CustomAssert();
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        try {
            logger.info("Starting Test TC-C3278: Verify Audit Log in Field Provisioning.");
            JSONObject jsonObj = new JSONObject(fieldProvisioningResponseForActions);
            String payloadForProvisioning = jsonObj.getJSONArray("data").toString();

            AdminHelper adminHelperObj = new AdminHelper();
            adminHelperObj.loginWithClientAdminUser();

            int actionsEntityTypeId = ConfigureConstantFields.getEntityIdByName("actions");

            String auditLogsResponse = executor.get(AuditLogs.getAPIPath(actionsEntityTypeId), AuditLogs.getHeaders()).getResponse().getResponseBody();
            JSONArray jsonArr = new JSONArray(auditLogsResponse);

            int sizeBeforeUpdate = jsonArr.length();
            int responseCode = fieldProvisioningObj.hitFieldProvisioning(actionsEntityTypeId, payloadForProvisioning);

            if (responseCode == 200) {
                auditLogsResponse = executor.get(AuditLogs.getAPIPath(actionsEntityTypeId), AuditLogs.getHeaders()).getResponse().getResponseBody();
                jsonArr = new JSONArray(auditLogsResponse);
                int sizeAfterUpdate = jsonArr.length();

                if (sizeAfterUpdate != sizeBeforeUpdate + 1) {
                    csAssert.assertTrue(false, "No New Entry Created in Audit Log for Field Provisioning of Actions Entity.");
                }
            } else {
                throw new SkipException("Couldn't Update Field Provisioning for Actions Entity.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C3278. " + e.getMessage());
        }
        Check checkObj = new Check();
        checkObj.hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);

        csAssert.assertAll();
    }


    /*
    TC-C4054: Verify User is able to create Field Provisioning for Specific Supplier and Service Data Entity.
     */
    @Test
    public void testC4054() {
        CustomAssert csAssert = new CustomAssert();
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        try {
            logger.info("Starting Test TC-C4054: Verify that User is able to Create New Field Provisioning for Specific Supplier and Service Data Entity.");
            AdminHelper adminHelperObj = new AdminHelper();
            adminHelperObj.loginWithClientAdminUser();

            String createPageResponse = executor.get(CreatePage.getAPIPath(), CreatePage.getHeaders()).getResponse().getResponseBody();
            List<Integer> allSupplierIds = CreatePage.getAllSupplierIdsFromCreatePageResponse(createPageResponse);

            if (allSupplierIds == null || allSupplierIds.isEmpty()) {
                throw new SkipException("Couldn't get All Supplier Ids from Field Provisioning Create Page Response.");
            }

            int serviceDataEntityTypeId = ConfigureConstantFields.getEntityIdByName("service data");

            for (Integer supplierId : allSupplierIds) {
                String entityTypesResponse = executor.get(EntityTypes.getAPIPath(supplierId), EntityTypes.getHeaders()).getResponse().getResponseBody();
                Boolean hasEntity = EntityTypes.hasEntityTypeId(entityTypesResponse, serviceDataEntityTypeId);

                if (hasEntity != null && hasEntity) {
                    JSONObject jsonObj = new JSONObject(fieldProvisioningResponseForServiceData);
                    String payloadForFieldProvisioning = jsonObj.getJSONArray("data").toString();

                    logger.info("Creating New Field Provisioning for Service Data Entity and Supplier Id {}", supplierId);
                    Integer responseCode = fieldProvisioningObj.hitFieldProvisioning(serviceDataEntityTypeId, supplierId, payloadForFieldProvisioning);

                    if (responseCode == 200) {
                        //Get Newly Created Provisioning Id
                        int provisioningId = fieldProvisioningObj.getNewlyCreatedProvisioningId(1002, supplierId, serviceDataEntityTypeId);

                        if (provisioningId == -1) {
                            throw new SkipException("Couldn't get Newly Created Provisioning Id for Service Data Entity and Supplier Id " + supplierId);
                        }

                        boolean deleteProvisioningData = fieldProvisioningObj.deleteProvisioningData(provisioningId);

                        if (!deleteProvisioningData) {
                            csAssert.assertTrue(false, "Couldn't Delete Provisioning Data for Id " + provisioningId);
                        }
                    } else {
                        throw new SkipException("Couldn't Create New Field Provisioning for Service Data Entity and Supplier Id " + supplierId);
                    }

                    break;
                }
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C4054. " + e.getMessage());
        }
        Check checkObj = new Check();
        checkObj.hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);

        csAssert.assertAll();
    }


    /*
    TC-C4155: Verify that all Inactive Custom Fields should be disabled in Bulk Create Field Provisioning.
     */
    @Test
    public void testC4155() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C4155: Verify that all Inactive Custom Fields should be disabled in Bulk Create Field Provisioning.");

            int serviceDataEntityTypeId = ConfigureConstantFields.getEntityIdByName("service data");
            List<Map<String, String>> allDisabledBulkCreateFields = fieldProvisioningObj.getAllDisabledBulkCreateFields(fieldProvisioningResponseForServiceData);

            if (allDisabledBulkCreateFields == null || allDisabledBulkCreateFields.isEmpty()) {
                throw new SkipException("Couldn't get All Disabled Bulk Create Fields.");
            }

            List<Map<String, String>> allDynamicFieldsOfServiceData = bulkOperationsHelperObj.getAllDynamicFieldsOfEntity(serviceDataEntityTypeId);

            if (allDynamicFieldsOfServiceData == null) {
                throw new SkipException("Couldn't get All Dynamic Fields of Service Data Entity.");
            }

            for (Map<String, String> dynamicFieldMap : allDynamicFieldsOfServiceData) {
                String id = dynamicFieldMap.get("id");
                String active = dynamicFieldMap.get("active");
                String apiName = dynamicFieldMap.get("apiName");

                if (active.equalsIgnoreCase("true")) {
                    continue;
                }

                boolean inactiveDynamicFieldFound = false;

                for (Map<String, String> disabledField : allDisabledBulkCreateFields) {
                    if (disabledField.get("id").equalsIgnoreCase(id)) {
                        inactiveDynamicFieldFound = true;

                        if (!disabledField.get("active").equalsIgnoreCase(active)) {
                            csAssert.assertTrue(false, "Expected Active Status: [false] and Actual Status: [" +
                                    disabledField.get("active") + "] for Field having Id " + id + " and ApiName " + apiName);
                        }

                        break;
                    }
                }

                csAssert.assertTrue(inactiveDynamicFieldFound, "Couldn't find Inactive Dynamic Field having Id " + id + ", ApiName: " + apiName);
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C4155. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C4060: Verify Metadata Support - ID, Status, Source, Source Title/Name, Attachments should be disabled in Field Provisioning for Service Data.
     */
    @Test
    public void testC4060() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C4060: Verify Fields ID, Status, Source, Source Title/Name, Attachments should be disabled in Field Provisioning for Service Data.");

            List<Map<String, String>> allDisabledBulkCreateFields = fieldProvisioningObj.getAllDisabledBulkCreateFields(fieldProvisioningResponseForServiceData);

            if (allDisabledBulkCreateFields == null || allDisabledBulkCreateFields.isEmpty()) {
                throw new SkipException("Couldn't get All Disabled Bulk Create Fields.");
            }

            boolean statusFieldFound = false;
            boolean commentsAndAttachmentsAddYourCommentsFieldFound = false;
            boolean commentsAndAttachmentsPrivacyFieldFound = false;
            boolean commentsAndAttachmentsActualDateFieldFound = false;
            boolean commentsAndAttachmentsRequestedByFieldFound = false;
            boolean commentsAndAttachmentsCRFieldFound = false;
            boolean commentsAndAttachmentsWORFieldFound = false;

            for (Map<String, String> disabledFieldMap : allDisabledBulkCreateFields) {
                String id = disabledFieldMap.get("id");

                if (id.equalsIgnoreCase("11775")) {
                    statusFieldFound = true;
                } else if (id.equalsIgnoreCase("12238")) {
                    commentsAndAttachmentsAddYourCommentsFieldFound = true;
                } else if (id.equalsIgnoreCase("12242")) {
                    commentsAndAttachmentsPrivacyFieldFound = true;
                } else if (id.equalsIgnoreCase("12243")) {
                    commentsAndAttachmentsActualDateFieldFound = true;
                } else if (id.equalsIgnoreCase("12244")) {
                    commentsAndAttachmentsRequestedByFieldFound = true;
                } else if (id.equalsIgnoreCase("12246")) {
                    commentsAndAttachmentsCRFieldFound = true;
                } else if (id.equalsIgnoreCase("12247")) {
                    commentsAndAttachmentsWORFieldFound = true;
                }
            }

            csAssert.assertTrue(statusFieldFound, "Basic Information - Status Field not found in All Disabled Bulk Create Fields for Service Data Entity.");
            csAssert.assertTrue(commentsAndAttachmentsAddYourCommentsFieldFound,
                    "Comments and Attachments Version 2.0 - Add Your Comments Field not found in All Disabled Bulk Create Fields for Service Data Entity.");
            csAssert.assertTrue(commentsAndAttachmentsPrivacyFieldFound,
                    "Comments and Attachments Version 2.0 - Privacy Field not found in All Disabled Bulk Create Fields for Service Data Entity.");
            csAssert.assertTrue(commentsAndAttachmentsActualDateFieldFound,
                    "Comments and Attachments Version 2.0 - Actual Date Field not found in All Disabled Bulk Create Fields for Service Data Entity.");
            csAssert.assertTrue(commentsAndAttachmentsRequestedByFieldFound,
                    "Comments and Attachments Version 2.0 - Requested By Field not found in All Disabled Bulk Create Fields for Service Data Entity.");
            csAssert.assertTrue(commentsAndAttachmentsCRFieldFound,
                    "Comments and Attachments Version 2.0 - Change Request Field not found in All Disabled Bulk Create Fields for Service Data Entity.");
            csAssert.assertTrue(commentsAndAttachmentsWORFieldFound,
                    "Comments and Attachments Version 2.0 - Work Order Request Field not found in All Disabled Bulk Create Fields for Service Data Entity.");
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C4060. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C4156: Verify Audit Log in Field Provisioning for Service Data
     */
    @Test
    public void testC4156() {
        CustomAssert csAssert = new CustomAssert();
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        try {
            logger.info("Starting Test TC-C4156: Verify Audit Log in Field Provisioning for Service Data.");
            int serviceDataEntityTypeId = ConfigureConstantFields.getEntityIdByName("service data");

            JSONObject jsonObj = new JSONObject(fieldProvisioningResponseForServiceData);
            String payloadForProvisioning = jsonObj.getJSONArray("data").toString();

            AdminHelper adminHelperObj = new AdminHelper();
            adminHelperObj.loginWithClientAdminUser();

            String auditLogsResponse = executor.get(AuditLogs.getAPIPath(serviceDataEntityTypeId), AuditLogs.getHeaders()).getResponse().getResponseBody();
            JSONArray jsonArr = new JSONArray(auditLogsResponse);

            int sizeBeforeUpdate = jsonArr.length();
            int responseCode = fieldProvisioningObj.hitFieldProvisioning(serviceDataEntityTypeId, payloadForProvisioning);

            if (responseCode == 200) {
                auditLogsResponse = executor.get(AuditLogs.getAPIPath(serviceDataEntityTypeId), AuditLogs.getHeaders()).getResponse().getResponseBody();
                jsonArr = new JSONArray(auditLogsResponse);
                int sizeAfterUpdate = jsonArr.length();

                if (sizeAfterUpdate != sizeBeforeUpdate + 1) {
                    csAssert.assertTrue(false, "No New Entry Created in Audit Log for Field Provisioning of Service Data Entity.");
                }
            } else {
                throw new SkipException("Couldn't Update Field Provisioning for Service Data Entity.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C4156. " + e.getMessage());
        }
        Check checkObj = new Check();
        checkObj.hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);

        csAssert.assertAll();
    }


    /*
    TC-C4055: Verify that the default value of Bulk Create Fields on Create Field Provisioning Page are same as for Global Supplier.
     */
    @Test
    public void testC4055() {
        CustomAssert csAssert = new CustomAssert();
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        try {
            logger.info("Starting Test TC-C4055. Verify that the default value of Bulk Create Fields on Create Field Provisioning Page are same as for Global Supplier.");
            AdminHelper adminHelperObj = new AdminHelper();
            adminHelperObj.loginWithClientAdminUser();

            String createPageResponse = executor.get(CreatePage.getAPIPath(), CreatePage.getHeaders()).getResponse().getResponseBody();
            List<Integer> allSupplierIds = CreatePage.getAllSupplierIdsFromCreatePageResponse(createPageResponse);

            if (allSupplierIds == null || allSupplierIds.isEmpty()) {
                throw new SkipException("Couldn't get All Supplier Ids from Field Provisioning Create Page Response.");
            }

            int serviceDataEntityTypeId = ConfigureConstantFields.getEntityIdByName("service data");

            for (Integer supplierId : allSupplierIds) {
                String entityTypesResponse = executor.get(EntityTypes.getAPIPath(supplierId), EntityTypes.getHeaders()).getResponse().getResponseBody();
                Boolean hasEntity = EntityTypes.hasEntityTypeId(entityTypesResponse, serviceDataEntityTypeId);

                if (hasEntity != null && hasEntity) {
                    JSONObject jsonObj = new JSONObject(fieldProvisioningResponseForServiceData);
                    String payloadForFieldProvisioning = jsonObj.getJSONArray("data").toString();

                    logger.info("Creating New Field Provisioning for Service Data Entity and Supplier Id {}", supplierId);
                    Integer responseCode = fieldProvisioningObj.hitFieldProvisioning(serviceDataEntityTypeId, supplierId, payloadForFieldProvisioning);

                    if (responseCode == 200) {
                        //Get Newly Created Provisioning Id
                        int provisioningId = fieldProvisioningObj.getNewlyCreatedProvisioningId(1002, supplierId, serviceDataEntityTypeId);

                        if (provisioningId == -1) {
                            throw new SkipException("Couldn't get Newly Created Provisioning Id for Service Data Entity and Supplier Id " + supplierId);
                        }

                        List<Map<String, String>> allGlobalBulkCreateFields = fieldProvisioningObj.getAllBulkCreateFields(fieldProvisioningResponseForServiceData);
                        String fieldProvisioningResponse = fieldProvisioningObj.getFieldProvisioningResponse(serviceDataEntityTypeId, supplierId);

                        List<Map<String, String>> allBulkCreateFieldsInNewlyCreatedProvisioning = fieldProvisioningObj.getAllBulkCreateFields(fieldProvisioningResponse);

                        if (allGlobalBulkCreateFields.size() == allBulkCreateFieldsInNewlyCreatedProvisioning.size()) {
                            try {
                                for (int i = 0; i < allBulkCreateFieldsInNewlyCreatedProvisioning.size(); i++) {
                                    String actualFieldName = allBulkCreateFieldsInNewlyCreatedProvisioning.get(i).get("fieldName");
                                    String expectedFieldName = allGlobalBulkCreateFields.get(i).get("fieldName");

                                    csAssert.assertTrue(actualFieldName.equalsIgnoreCase(expectedFieldName), "Field #" + (i + 1) + " Expected Field Name: " +
                                            expectedFieldName + " and Actual Field Name: " + actualFieldName);

                                    String actualMandatory = allBulkCreateFieldsInNewlyCreatedProvisioning.get(i).get("mandatory");
                                    String expectedMandatory = allGlobalBulkCreateFields.get(i).get("mandatory");

                                    csAssert.assertTrue(actualFieldName.equalsIgnoreCase(expectedFieldName), "Field #" + (i + 1) + " Expected Mandatory: " +
                                            expectedMandatory + " and Actual Mandatory: " + actualMandatory);

                                    String actualActive = allBulkCreateFieldsInNewlyCreatedProvisioning.get(i).get("active");
                                    String expectedActive = allGlobalBulkCreateFields.get(i).get("active");

                                    csAssert.assertTrue(actualFieldName.equalsIgnoreCase(expectedFieldName), "Field #" + (i + 1) + " Expected Active: " +
                                            expectedActive + " and Actual Active: " + actualActive);
                                }
                            } catch (Exception e) {
                                csAssert.assertTrue(false,
                                        "Exception while Matching All Bulk Created Fields in Global Supplier and Newly Created Provisioning Id " + provisioningId +
                                                ". " + e.getMessage());
                            }
                        } else {
                            csAssert.assertTrue(false, "Bulk Create Fields Size mismatch. Bulk Create Fields Size for Global Supplier: " +
                                    allGlobalBulkCreateFields.size() + " and Bulk Create Fields Size for Supplier Id " + supplierId + ": " +
                                    allBulkCreateFieldsInNewlyCreatedProvisioning.size());
                        }

                        boolean deleteProvisioningData = fieldProvisioningObj.deleteProvisioningData(provisioningId);

                        if (!deleteProvisioningData) {
                            csAssert.assertTrue(false, "Couldn't Delete Provisioning Data for Id " + provisioningId);
                        }
                    } else {
                        throw new SkipException("Couldn't Create New Field Provisioning for Service Data Entity and Supplier Id " + supplierId);
                    }

                    break;
                }
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C4055. " + e.getMessage());
        }
        Check checkObj = new Check();
        checkObj.hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);

        csAssert.assertAll();
    }


    /*
    TC-C4129: Verify that User is able to Create New Field Provisioning for Specific Supplier and Entities Invoice, Invoice Line Item and Purchase Order.
     */
    @Test
    public void testC4129() {
        CustomAssert csAssert = new CustomAssert();
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        try {
            logger.info("Starting Test TC-C4129: Verify that User is able to Create New Field Provisioning for Specific Supplier and Entities Invoice, Invoice Line Item" +
                    " and Purchase Order.");
            AdminHelper adminHelperObj = new AdminHelper();
            adminHelperObj.loginWithClientAdminUser();

            String createPageResponse = executor.get(CreatePage.getAPIPath(), CreatePage.getHeaders()).getResponse().getResponseBody();
            List<Integer> allSupplierIds = CreatePage.getAllSupplierIdsFromCreatePageResponse(createPageResponse);

            if (allSupplierIds == null || allSupplierIds.isEmpty()) {
                throw new SkipException("Couldn't get All Supplier Ids from Field Provisioning Create Page Response.");
            }

            String[] entitiesArr = {"invoices", "invoice line item", "purchase orders"};

            for (String entityName : entitiesArr) {
                int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);

                for (Integer supplierId : allSupplierIds) {
                    String entityTypesResponse = executor.get(EntityTypes.getAPIPath(supplierId), EntityTypes.getHeaders()).getResponse().getResponseBody();
                    Boolean hasEntity = EntityTypes.hasEntityTypeId(entityTypesResponse, entityTypeId);

                    if (hasEntity != null && hasEntity) {
                        JSONObject jsonObj = new JSONObject(fieldProvisioningResponseForServiceData);
                        String payloadForFieldProvisioning = jsonObj.getJSONArray("data").toString();

                        logger.info("Creating New Field Provisioning for Entity {} and Supplier Id {}", entityName, supplierId);
                        Integer responseCode = fieldProvisioningObj.hitFieldProvisioning(entityTypeId, supplierId, payloadForFieldProvisioning);

                        if (responseCode == 200) {
                            //Get Newly Created Provisioning Id
                            int provisioningId = fieldProvisioningObj.getNewlyCreatedProvisioningId(1002, supplierId, entityTypeId);

                            if (provisioningId == -1) {
                                throw new SkipException("Couldn't get Newly Created Provisioning Id for Entity " + entityName + " and Supplier Id " + supplierId);
                            }

                            boolean deleteProvisioningData = fieldProvisioningObj.deleteProvisioningData(provisioningId);

                            //Verifying Newly Created Provisioning Response.
                            String provisioningResponse = fieldProvisioningObj.hitFieldProvisioning(entityTypeId, supplierId);

                            if (!ParseJsonResponse.validJsonResponse(provisioningResponse)) {
                                csAssert.assertTrue(false, "Provisioning Response for Newly Created Id " + provisioningId + " is an Invalid JSON.");
                            }

                            if (!deleteProvisioningData) {
                                csAssert.assertTrue(false, "Couldn't Delete Provisioning Data for Id " + provisioningId);
                            }
                        } else {
                            throw new SkipException("Couldn't Create New Field Provisioning for Entity " + entityName + " and Supplier Id " + supplierId);
                        }

                        break;
                    }
                }
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C4129. " + e.getMessage());
        }
        Check checkObj = new Check();
        checkObj.hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);

        csAssert.assertAll();
    }


    /*
    TC-C4139: Verify Custom Fields and Role Groups in Bulk Create Field Provisioning for Entities Invoice and Invoice Line Item
     */
    @Test
    public void testC4139() {
        CustomAssert csAssert = new CustomAssert();
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        try {
            logger.info("Starting Test TC-C4117: Verify Custom Fields and Role Groups in Bulk Create Field Provisioning for Entities Invoice and Invoice Line Item.");

            AdminHelper adminHelperObj = new AdminHelper();
            adminHelperObj.loginWithClientAdminUser();

            String[] entitiesArr = {"invoices", "invoice line item"};

            for (String entityName : entitiesArr) {
                int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
                String fieldProvisioningResponse = fieldProvisioningObj.hitFieldProvisioning(entityTypeId);

                List<Map<String, String>> allDisabledBulkCreateFields = fieldProvisioningObj.getAllDisabledBulkCreateFields(fieldProvisioningResponse);

                if (allDisabledBulkCreateFields == null || allDisabledBulkCreateFields.isEmpty()) {
                    throw new SkipException("Couldn't get All Disabled Bulk Create Fields of Entity " + entityName);
                }

                List<Map<String, String>> allDynamicFieldsOfServiceData = bulkOperationsHelperObj.getAllDynamicFieldsOfEntity(entityTypeId);

                if (allDynamicFieldsOfServiceData == null) {
                    throw new SkipException("Couldn't get All Dynamic Fields of Entity " + entityName);
                }

                for (Map<String, String> dynamicFieldMap : allDynamicFieldsOfServiceData) {
                    String id = dynamicFieldMap.get("id");
                    String active = dynamicFieldMap.get("active");
                    String apiName = dynamicFieldMap.get("apiName");

                    if (active.equalsIgnoreCase("true")) {
                        continue;
                    }

                    boolean inactiveDynamicFieldFound = false;

                    for (Map<String, String> disabledField : allDisabledBulkCreateFields) {
                        if (disabledField.get("id").equalsIgnoreCase(id)) {
                            inactiveDynamicFieldFound = true;

                            if (!disabledField.get("active").equalsIgnoreCase(active)) {
                                csAssert.assertTrue(false, "Expected Active Status: [false] and Actual Status: [" +
                                        disabledField.get("active") + "] for Field having Id " + id + " and ApiName " + apiName + " for Entity " + entityName);
                            }

                            break;
                        }
                    }

                    csAssert.assertTrue(inactiveDynamicFieldFound, "Couldn't find Inactive Dynamic Field having Id " + id + ", ApiName: " + apiName +
                            " for Entity " + entityName);
                }

                List<Map<String, String>> allExpectedDisabledFields;
                Map<String, String> expectedDisabledFieldMap;

                if (entityName.equalsIgnoreCase("invoices")) {
                    allExpectedDisabledFields = new ArrayList<>();

                    expectedDisabledFieldMap = new HashMap<>();
                    expectedDisabledFieldMap.put("id", "603");
                    expectedDisabledFieldMap.put("name", "Basic Information - Status");
                    allExpectedDisabledFields.add(expectedDisabledFieldMap);

                    expectedDisabledFieldMap = new HashMap<>();
                    expectedDisabledFieldMap.put("id", "7227");
                    expectedDisabledFieldMap.put("name", "Basic Information - Source");
                    allExpectedDisabledFields.add(expectedDisabledFieldMap);

                    expectedDisabledFieldMap = new HashMap<>();
                    expectedDisabledFieldMap.put("id", "7228");
                    expectedDisabledFieldMap.put("name", "Basic Information - Source Name/Title");
                    allExpectedDisabledFields.add(expectedDisabledFieldMap);

                    expectedDisabledFieldMap = new HashMap<>();
                    expectedDisabledFieldMap.put("id", "610");
                    expectedDisabledFieldMap.put("name", "Basic Information - Invoice Amount");
                    allExpectedDisabledFields.add(expectedDisabledFieldMap);

                    expectedDisabledFieldMap = new HashMap<>();
                    expectedDisabledFieldMap.put("id", "627");
                    expectedDisabledFieldMap.put("name", "Financial Information - No. Of Line Items");
                    allExpectedDisabledFields.add(expectedDisabledFieldMap);

                    expectedDisabledFieldMap = new HashMap<>();
                    expectedDisabledFieldMap.put("id", "628");
                    expectedDisabledFieldMap.put("name", "Financial Information - No. Of Line Items with discrepancy");
                    allExpectedDisabledFields.add(expectedDisabledFieldMap);

                    verifyDisabledFields(entityName, allExpectedDisabledFields, allDisabledBulkCreateFields, csAssert);
                } else {
                    allExpectedDisabledFields = new ArrayList<>();

                    expectedDisabledFieldMap = new HashMap<>();
                    expectedDisabledFieldMap.put("id", "11795");
                    expectedDisabledFieldMap.put("name", "Service Data - Service Category");
                    allExpectedDisabledFields.add(expectedDisabledFieldMap);

                    expectedDisabledFieldMap = new HashMap<>();
                    expectedDisabledFieldMap.put("id", "11634");
                    expectedDisabledFieldMap.put("name", "Service Data - Service Sub Category");
                    allExpectedDisabledFields.add(expectedDisabledFieldMap);

                    expectedDisabledFieldMap = new HashMap<>();
                    expectedDisabledFieldMap.put("id", "11688");
                    expectedDisabledFieldMap.put("name", "Service Data - Service Data Currency");
                    allExpectedDisabledFields.add(expectedDisabledFieldMap);

                    expectedDisabledFieldMap = new HashMap<>();
                    expectedDisabledFieldMap.put("id", "11796");
                    expectedDisabledFieldMap.put("name", "Functions - Functions");
                    allExpectedDisabledFields.add(expectedDisabledFieldMap);

                    expectedDisabledFieldMap = new HashMap<>();
                    expectedDisabledFieldMap.put("id", "11797");
                    expectedDisabledFieldMap.put("name", "Functions - Services");
                    allExpectedDisabledFields.add(expectedDisabledFieldMap);

                    expectedDisabledFieldMap = new HashMap<>();
                    expectedDisabledFieldMap.put("id", "11787");
                    expectedDisabledFieldMap.put("name", "Basic Information - Contract");
                    allExpectedDisabledFields.add(expectedDisabledFieldMap);

                    expectedDisabledFieldMap = new HashMap<>();
                    expectedDisabledFieldMap.put("id", "11788");
                    expectedDisabledFieldMap.put("name", "Basic Information - Supplier");
                    allExpectedDisabledFields.add(expectedDisabledFieldMap);

                    expectedDisabledFieldMap = new HashMap<>();
                    expectedDisabledFieldMap.put("id", "12094");
                    expectedDisabledFieldMap.put("name", "Basic Information - Invoice Number");
                    allExpectedDisabledFields.add(expectedDisabledFieldMap);

                    expectedDisabledFieldMap = new HashMap<>();
                    expectedDisabledFieldMap.put("id", "11803");
                    expectedDisabledFieldMap.put("name", "Important Dates - Invoice Date");
                    allExpectedDisabledFields.add(expectedDisabledFieldMap);

                    expectedDisabledFieldMap = new HashMap<>();
                    expectedDisabledFieldMap.put("id", "11635");
                    expectedDisabledFieldMap.put("name", "Purchase Order - Business Unit");
                    allExpectedDisabledFields.add(expectedDisabledFieldMap);

                    expectedDisabledFieldMap = new HashMap<>();
                    expectedDisabledFieldMap.put("id", "11636");
                    expectedDisabledFieldMap.put("name", "Purchase Order - Cost Center");
                    allExpectedDisabledFields.add(expectedDisabledFieldMap);

                    verifyDisabledFields(entityName, allExpectedDisabledFields, allDisabledBulkCreateFields, csAssert);
                }
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test TC-C4139. " + e.getMessage());
        }
        Check checkObj = new Check();
        checkObj.hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);

        csAssert.assertAll();
    }

    private void verifyDisabledFields(String entityName, List<Map<String, String>> allExpectedDisabledFields, List<Map<String, String>> allDisabledBulkCreateFields,
                                      CustomAssert csAssert) {
        try {
            for (Map<String, String> expectedDisabledField : allExpectedDisabledFields) {
                boolean fieldFound = false;
                String expectedId = expectedDisabledField.get("id");
                String expectedName = expectedDisabledField.get("name");

                for (Map<String, String> disabledFieldMap : allDisabledBulkCreateFields) {
                    String id = disabledFieldMap.get("id");

                    if (id.equalsIgnoreCase(expectedId)) {
                        fieldFound = true;
                        break;
                    }
                }

                csAssert.assertTrue(fieldFound, "Field [" + expectedName + "] not found in All Disabled Bulk Create Fields for Entity " + entityName);
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Disabled Fields for Entity " + entityName);
        }
    }
}