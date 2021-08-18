package com.sirionlabs.test.pod.ca;

import com.sirionlabs.api.clientAdmin.docTypeConfig.DocTypeConfigList;
import com.sirionlabs.api.clientAdmin.docTypeConfig.DocTypeConfigShow;
import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.New;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.PayloadUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class TestAutoPortingCA89 {

    private final static Logger logger = LoggerFactory.getLogger(TestAutoPortingCA89.class);

    private String configFilePath = "src/test/resources/TestConfig/CAPod/AutoPort";
    private String configFileName = "TestAutoPort.cfg";
    private String extraFieldsFileName = "ExtraFields.cfg";

    private Map<String, String> allDocsMap = new HashMap<>();

    @BeforeClass
    public void beforeClass() {
        String docTypeConfigListResponse = DocTypeConfigList.getDocTypeConfigListResponse();
        allDocsMap = DocTypeConfigList.getAllDocTypeMap(docTypeConfigListResponse);
    }

    /*
    TC-C89367: Verify that config is added at client admin to show other folder per client basis.
     */
    @Test(enabled = true)
    public void testC89367() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C89367: Verify that config is added at client admin to show other folder per client basis");

            if (allDocsMap.isEmpty()) {
                csAssert.assertFalse(true, "Couldn't get All Docs from Client Admin.");
            } else {
                for (Map.Entry<String, String> map : allDocsMap.entrySet()) {
                    String docId = map.getValue();
                    Boolean allowInCDRValue = isCDRFlagActiveForDoc(docId);

                    if (allowInCDRValue == null) {
                        csAssert.assertFalse(true, "Couldn't get Allow in CDR Flag Value for Doc Type " + map.getKey());
                    }
                }
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C89367: " + e.getMessage());
        }

        csAssert.assertAll();
    }


    /*
    TC-C89370: Verify that Others Document Type is visible under Contract Information Section at CDR Show Page.
    TC-C89371: Verify Document Type for all Contract Types.
     */
    @Test(enabled = true)
    public void testC89371() {
        CustomAssert csAssert = new CustomAssert();

        logger.info("Starting Test TC-C89371: Verify Contract Document Types are visible under Contract Information Section at CDR Show Page.");

        String[] docTypes = {"other", "msa", "psa", "sow", "work order", "ola"};

        for (String docType : docTypes) {
            try {
                if (allDocsMap.containsKey(docType)) {
                    Boolean allowInCDRValue = isCDRFlagActiveForDoc(allDocsMap.get(docType));

                    if (allowInCDRValue == null) {
                        csAssert.assertFalse(true, "Couldn't get Allow in CDR value for Doc type " + docType);
                    } else if (allowInCDRValue) {
                        validateDocTypePresentInCDRShowPage(docType, true, csAssert);
                    } else {
                        validateDocTypePresentInCDRShowPage(docType, false, csAssert);
                    }
                } else {
                    csAssert.assertFalse(true, "Couldn't find Doc Type " + docType + " in Document Type Configuration List.");
                }
            } catch (Exception e) {
                csAssert.assertFalse(true, "Exception while Validating TC-C89371 for Doc Type " + docType + ". " + e.getMessage());
            }
        }

        csAssert.assertAll();
    }

    private Boolean isCDRFlagActiveForDoc(String docId) {
        String docTypeConfigShowResponse = DocTypeConfigShow.getDocTypeConfigShowResponse(docId);
        String allowInCDRValue = DocTypeConfigShow.getAllowInCDRFlagValue(docTypeConfigShowResponse);

        if (allowInCDRValue == null) {
            return null;
        }

        return allowInCDRValue.equalsIgnoreCase("yes");
    }

    private void validateDocTypePresentInCDRShowPage(String docType, boolean positiveCase, CustomAssert csAssert) {
        try {
            String listDataResponse = ListDataHelper.getListDataResponseVersion2("contract draft request");

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                String idColumn = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
                JSONArray jsonArr = new JSONObject(listDataResponse).getJSONArray("data");

                boolean cdrFoundWithSupplier = false;
                int recordId = -1;
                String showResponse = null;

                for (int i = 0; i < jsonArr.length(); i++) {
                    String idValue = jsonArr.getJSONObject(i).getJSONObject(idColumn).getString("value");
                    recordId = ListDataHelper.getRecordIdFromValue(idValue);

                    showResponse = ShowHelper.getShowResponseVersion2(160, recordId);

                    if (ShowHelper.isShowPageAccessible(showResponse)) {
                        JSONObject jsonObj = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("suppliers");

                        if (jsonObj.has("values") && !jsonObj.isNull("values") && jsonObj.getJSONArray("values").length() > 0) {
                            cdrFoundWithSupplier = true;
                            break;
                        }
                    }
                }

                if (cdrFoundWithSupplier) {
                    JSONObject jsonObj = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("contractParentEntityType")
                            .getJSONObject("options");

                    if (!jsonObj.getBoolean("autoComplete")) {
                        jsonArr = jsonObj.getJSONArray("data");

                        boolean docTypeFound = false;

                        for (int j = 0; j < jsonArr.length(); j++) {
                            if (jsonArr.getJSONObject(j).getString("name").equalsIgnoreCase(docType)) {
                                docTypeFound = true;
                                break;
                            }
                        }

                        if (positiveCase) {
                            csAssert.assertEquals(docTypeFound, true, "Doc Type " + docType + " not present in CDR Show Page of Record Id " + recordId);
                        } else {
                            csAssert.assertEquals(docTypeFound, false, "Doc Type " + docType + " present in CDR Show Page of Record Id " + recordId);
                        }
                    }

                } else {
                    csAssert.assertFalse(true, "Couldn't find CDR having Supplier present.");
                }
            } else {
                csAssert.assertFalse(true, "ListData API Response for CDR entity is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating CDR Show Page for Doc Type " + docType + ". " + e.getMessage());
        }
    }

    /*
    TC-C89372: Verify Inside Document Type configuration, it is dependent on Parent Entity Creation checkbox.

    Hard-coding few things here. For this case to work, Document Type Configuration for MSA doc type must have below flags under Parent Entity Creation Tab.
    CDR = Yes
    Suppliers = Yes
    MSA = No
    PSA = Yes/No
    Other Folder = Yes/No

    TC-C89373: Verify that Contract created has same parent information on show page.
     */
    @Test(enabled = true)
    public void testC89372() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C89372");

            String sectionName = "c89372";

            //Validating Negative case here. For MSA, where the Flag is turned off.
            Map<String, String> properties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, sectionName);
            String newPayload = "{\"documentTypeId\":4,\"parentEntity\":{\"entityIds\":[" + properties.get("contract id").trim() +
                    "],\"entityTypeId\":61},\"sourceEntity\":{\"entityIds\":[" + properties.get("cdr id").trim() +
                    "],\"entityTypeId\":160},\"actualParentEntity\":{\"entityIds\":[" + properties.get("contract id").trim() +
                    "],\"entityTypeId\":61},\"relationIds\":[" + properties.get("supplier id").trim() + "]}";

            New newObj = new New();
            newObj.hitNewV1ForMultiSupplier("contracts", newPayload);
            String newResponse = newObj.getNewJsonStr();

            if (ParseJsonResponse.validJsonResponse(newResponse)) {
                JSONObject jsonObj = new JSONObject(newResponse).getJSONObject("header").getJSONObject("response");
                String status = jsonObj.getString("status");
                String errorMessage = jsonObj.getString("errorMessage");

                if (!status.equalsIgnoreCase("applicationError") ||
                        !errorMessage.equalsIgnoreCase("Porting Not Allowed For Selected Source. Document Type Not Configured")) {
                    csAssert.assertFalse(true, "Error Message validation failed for Negative Flow. Actual Error Message: [" + errorMessage + "]");
                }
            } else {
                csAssert.assertFalse(true, "New API Response for Negative Flow is an Invalid JSON.");
            }

            //Validating Positive case here. For SOW, where the Flag is turned on.
            newPayload = "{\"documentTypeId\":9,\"parentEntity\":{\"entityIds\":[" + properties.get("contract id").trim() +
                    "],\"entityTypeId\":61},\"sourceEntity\":{\"entityIds\":[" + properties.get("cdr id").trim() +
                    "],\"entityTypeId\":160},\"actualParentEntity\":{\"entityIds\":[" + properties.get("contract id").trim() +
                    "],\"entityTypeId\":61},\"relationIds\":[" + properties.get("supplier id").trim() + "]}";

            newObj.hitNewV1ForMultiSupplier("contracts", newPayload);
            newResponse = newObj.getNewJsonStr();

            if (newResponse != null) {
                if (ParseJsonResponse.validJsonResponse(newResponse)) {
                    CreateEntity createEntityHelperObj = new CreateEntity(configFilePath, configFileName, configFilePath, extraFieldsFileName, sectionName);

                    Map<String, String> extraFields = createEntityHelperObj.setExtraRequiredFields("contracts");
                    newObj.setAllRequiredFields(newResponse);
                    Map<String, String> allRequiredFields = newObj.getAllRequiredFields();
                    allRequiredFields = createEntityHelperObj.processAllChildFields(allRequiredFields, newResponse);
                    allRequiredFields = createEntityHelperObj.processNonChildFields(allRequiredFields, newResponse);

                    String createPayload = PayloadUtils.getPayloadForCreate(newResponse, allRequiredFields, extraFields, null, configFilePath,
                            extraFieldsFileName);

                    if (createPayload != null) {
                        logger.info("Hitting Create Api for Entity for Multi Supplier Contract");
                        Create createObj = new Create();
                        createObj.hitCreate("contracts", createPayload);
                        String createResponse = createObj.getCreateJsonStr();
                        String status = ParseJsonResponse.getStatusFromResponse(createResponse);

                        if (status.equalsIgnoreCase("success")) {
                            int newlyCreatedContractId = CreateEntity.getNewEntityId(createResponse);

                            validateC89373(newlyCreatedContractId, 9, Integer.parseInt(properties.get("contract id").trim()),
                                    Integer.parseInt(properties.get("supplier id").trim()), Integer.parseInt(properties.get("cdr id").trim()), csAssert);

                            //Delete Contract Id.
                            EntityOperationsHelper.deleteEntityRecord("contracts", newlyCreatedContractId);
                        } else {
                            csAssert.assertFalse(true, "Couldn't Create Contract for Positive Flow due to " + status);
                        }
                    } else {
                        logger.error("Contract Create Payload is null and hence cannot create SOW Contract for Positive Flow.");
                    }
                } else {
                    logger.error("New V1 API Response is an Invalid JSON for Positive Flow");
                }
            } else {
                logger.error("New API Response is null for Positive Flow.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C89372: " + e.getMessage());
        }

        csAssert.assertAll();
    }

    private void validateC89373(int contractId, int docTypeId, int parentEntityId, int supplierId, int cdrId, CustomAssert csAssert) {
        try {
            logger.info("Validating TC-C89373");
            String showResponse = ShowHelper.getShowResponseVersion2(61, contractId);

            if (ParseJsonResponse.validJsonResponse(showResponse)) {
                JSONObject jsonObj = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data");

                //Validate DocTypeId
                csAssert.assertEquals(docTypeId, jsonObj.getJSONObject("documentType").getJSONObject("values").getInt("id"),
                        "DocType Validation failed on Show Page of Contract Id " + contractId);

                //Validate ParentEntityId
                csAssert.assertEquals(String.valueOf(parentEntityId), jsonObj.getJSONObject("parentEntityIds").getJSONArray("values").get(0).toString(),
                        "ParentEntity Validation failed on Show Page of Contract Id " + contractId);

                //Validate Supplier Id
                csAssert.assertEquals(supplierId, jsonObj.getJSONObject("relations").getJSONArray("values").getJSONObject(0).getInt("id"),
                        "Supplier Id Validation failed on Show Page of Contract Id " + contractId);

                //Validate CDR Id
                csAssert.assertEquals(cdrId, jsonObj.getJSONObject("sourceEntityId").getInt("values"),
                        "CDR Id Validation failed on Show Page of Contract Id " + contractId);
            } else {
                csAssert.assertFalse(true, "Show Response for Contract Id " + contractId + " is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C89373. " + e.getMessage());
        }
    }

}