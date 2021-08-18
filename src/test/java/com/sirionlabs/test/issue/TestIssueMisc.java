package com.sirionlabs.test.issue;

import com.sirionlabs.api.commonAPI.CreateLinks;
import com.sirionlabs.api.commonAPI.New;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.entityCreation.*;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Listeners(value = MyTestListenerAdapter.class)
public class TestIssueMisc extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestIssueMisc.class);

    private String issueCreateConfigFilePath;
    private String issueCreateConfigFileName;

    @BeforeClass
    public void beforeClass() {
        issueCreateConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("IssueFilePath");
        issueCreateConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("IssueFileName");
    }


    @DataProvider(parallel = true)
    public Object[][] dataProvider() {
        List<Object[]> allTestData = new ArrayList<>();
        String[] allParentEntities = {"invoices", "contracts", "obligations", "service levels", "child service levels", "interpretations",
                "issues", "change requests"};

        for (String parentEntity : allParentEntities) {
            allTestData.add(new Object[]{parentEntity});
        }
        return allTestData.toArray(new Object[0][]);
    }

    /*
    TC-C8254: Verify inheritance of fields Functions, Services, Regions, Countries
     */
    @Test(dataProvider = "dataProvider")
    public void testFieldsInheritance(String parentEntityName) {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Verifying Inheritance of Fields in Issue from Parent Entity {}", parentEntityName);
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(parentEntityName);
            String payload = ListDataHelper.getPayloadForListData(entityTypeId, 50, 0);
            String listDataResponse = ListDataHelper.getListDataResponseVersion2(parentEntityName, payload);

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                ArrayList<Integer> allRecordIds = ListDataHelper.getAllDbIdsFromListPageResponse(listDataResponse);
                boolean permissionToCreateIssueFound = false;

                for (Integer recordId : allRecordIds) {
                    String createLinksResponse = CreateLinks.getCreateLinksV2Response(entityTypeId, recordId);
                    Map<Integer, String> allCreateLinks = CreateLinks.getAllSingleCreateLinksMap(createLinksResponse);

                    if (allCreateLinks != null) {
                        if (allCreateLinks.containsKey(17)) {
                            permissionToCreateIssueFound = true;

                            String createPath = allCreateLinks.get(17);
                            String newResponse = executor.get(createPath, New.getHeaders()).getResponse().getResponseBody();

                            String showResponse = ShowHelper.getShowResponseVersion2(entityTypeId, recordId);
                            String parentShowResponse = null;
                            int parentEntityTypeId = -1;

                            if (entityTypeId == 67) {
                                parentEntityTypeId = Integer.parseInt(ShowHelper.getValueOfField("parententitytypeid", showResponse));
                                String parentId = ShowHelper.getValueOfField("parententityid", showResponse);

                                parentShowResponse = ShowHelper.getShowResponseVersion2(parentEntityTypeId, Integer.parseInt(parentId));
                            }

                            //Verify Fields.
                            String[] fieldsArr = {"tier", "functions", "services", "regions", "countries"};

                            for (String fieldName : fieldsArr) {
                                verifyFieldInheritance(newResponse, fieldName, showResponse, parentShowResponse, parentEntityName, entityTypeId, parentEntityTypeId,
                                        recordId, csAssert);
                            }

                            break;
                        }
                    } else {
                        csAssert.assertTrue(false, "Couldn't get All Create Links from Parent Entity " + parentEntityName +
                                " and Record Id " + recordId);
                    }
                }

                if (!permissionToCreateIssueFound) {
                    throw new SkipException("Couldn't find record of Entity " + parentEntityName + " having Permission to Create Issue.");
                }
            } else {
                csAssert.assertTrue(false, "ListData API Response for Parent Entity " + parentEntityName + " is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Inheritance of Fields. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    private void verifyFieldInheritance(String newResponse, String fieldName, String showResponse, String parentShowResponse, String entityName, int entityTypeId,
                                        int parentEntityTypeId, int recordId, CustomAssert csAssert) {
        try {
            logger.info("verifyFieldInheritance for entity name{" + entityName + "}field name{" + fieldName + "}");
            String issueFieldShowPageObjectName = ShowHelper.getShowPageObjectNameMapping("issues", fieldName);
            List<String> allOptionsOfField = ShowHelper.getAllOptionsOfField(newResponse, ShowHelper.getShowFieldHierarchy(issueFieldShowPageObjectName, 17));

            String showPageObjectName = ShowHelper.getShowPageObjectNameMapping(entityName, fieldName);
            List<String> allSelectedValuesOfField = ShowHelper.getAllSelectValuesOfField(showResponse, showPageObjectName,
                    ShowHelper.getShowFieldHierarchy(showPageObjectName, entityTypeId), recordId, entityTypeId);

            //If Parent Record doesn't contain options then get options from the Parent Record of Parent.
            if (allSelectedValuesOfField == null && !showPageObjectName.equalsIgnoreCase("tier")) {
                String parentEntityName = ConfigureConstantFields.getEntityNameById(parentEntityTypeId);
                showPageObjectName = ShowHelper.getShowPageObjectNameMapping(parentEntityName, fieldName);

                allSelectedValuesOfField = ShowHelper.getAllSelectValuesOfField(parentShowResponse, showPageObjectName,
                        ShowHelper.getShowFieldHierarchy(showPageObjectName, parentEntityTypeId), recordId, entityTypeId);
            }
            if (allOptionsOfField != null && allSelectedValuesOfField != null) {
                if (allOptionsOfField.size() == allSelectedValuesOfField.size()) {
                    csAssert.assertTrue(allSelectedValuesOfField.containsAll(allOptionsOfField),
                            "All Options for Field " + showPageObjectName + " is not present in Parent Entity " + entityName + " Selected Values List.");
                } else {
                    csAssert.assertTrue(false, "Total Options for Field " + showPageObjectName + ": " + allOptionsOfField.size() +
                            " and Total Selected Values in Parent Entity Record: " + allSelectedValuesOfField.size() + " for Parent Entity Type Id: " +
                            parentEntityTypeId + " and Parent Record Id: " + recordId);
                }
            } else if (allOptionsOfField == null && allSelectedValuesOfField != null) {
                csAssert.assertTrue(allSelectedValuesOfField.isEmpty(), "No option found in New API Response but Parent Entity has selected values.");
            } else if (allOptionsOfField != null && !allOptionsOfField.isEmpty()) {
                csAssert.assertTrue(false, "No Selected Value in Parent Entity Type: " + entityTypeId + " and Record Id: " + recordId +
                        " but Options are available in New Response.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Inheritance of Field " + fieldName + " from Parent Entity " + entityName +
                    " and Record Id " + recordId);
        }
    }


    /*
    TC-C8226: Verify that on deleting a supplier all the issues created by supplier are deleted.
     */
    @Test(enabled = false)
    public void testC8226() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C8226: Verify that on deleting a supplier all the issues created by supplier are deleted.");
            String sectionName = "c8226";

            validateIssueAfterParentDeletion("suppliers", sectionName, csAssert);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C8226: " + e.getMessage());
        }

        csAssert.assertAll();
    }


    /*
    TC-C8228: Verify that on deleting a SL all the issues created by SL are deleted.
     */
    @Test(enabled = false)
    public void testC8228() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C8228: Verify that on deleting a SL all the issues created by SL are deleted.");
            String sectionName = "c8228";

            validateIssueAfterParentDeletion("service levels", sectionName, csAssert);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C8228: " + e.getMessage());
        }

        csAssert.assertAll();
    }


    /*
    TC-C8236: Verify that on deleting a CR all the issues created by CR are deleted.
     */
    @Test(enabled = false)
    public void testC8236() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C8236: Verify that on deleting a CR all the issues created by CR are deleted.");
            String sectionName = "c8236";

            validateIssueAfterParentDeletion("change requests", sectionName, csAssert);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C8236: " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C8238: Verify on deleting Issue/Interpretation/Invoice all the issues created by parent entity are deleted.
     */
    @Test(enabled = false)
    public void testC8238() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C8238: Verify that on deleting a Issue/Interpretation/Invoice all the issues created by parent entity are deleted.");

            validateIssueAfterParentDeletion("interpretations", "c8238 part 2", csAssert);
            validateIssueAfterParentDeletion("invoices", "c8238 part 3", csAssert);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C8238: " + e.getMessage());
        }
        csAssert.assertAll();
    }

    private void validateIssueAfterParentDeletion(String parentEntityName, String sectionName, CustomAssert csAssert) {
        int parentNewlyCreatedRecordId = -1;
        int issueId = -1;

        try {
            String parentEntityCreateResponse = null;

            //Create Parent Entity Record.
            switch (parentEntityName) {
                case "suppliers":
                    parentEntityCreateResponse = Supplier.createSupplier(sectionName, true);
                    break;

                case "service levels":
                    parentEntityCreateResponse = ServiceLevel.createServiceLevel(sectionName, true);
                    break;

                case "change requests":
                    parentEntityCreateResponse = ChangeRequest.createChangeRequest(sectionName, true);
                    break;

                case "interpretations":
                    parentEntityCreateResponse = Interpretation.createInterpretation(sectionName, true);
                    break;

                case "invoices":
                    parentEntityCreateResponse = Invoice.createInvoice(sectionName, true);
                    break;
            }

            String parentEntityCreateResponseStatus = ParseJsonResponse.getStatusFromResponse(parentEntityCreateResponse);

            if (!parentEntityCreateResponseStatus.equalsIgnoreCase("success")) {
                throw new SkipException("Couldn't create Parent Entity " + parentEntityName + " using Section: " + sectionName);
            }

            parentNewlyCreatedRecordId = CreateEntity.getNewEntityId(parentEntityCreateResponse);

            if (parentNewlyCreatedRecordId == -1) {
                throw new SkipException("Couldn't get Id of newly created " + parentEntityName);
            }

            ParseConfigFile.updateValueInConfigFile(issueCreateConfigFilePath, issueCreateConfigFileName, sectionName, "sourceid",
                    String.valueOf(parentNewlyCreatedRecordId));

            //Create Issue from Parent Entity
            String issueCreateResponse = Issue.createIssue(sectionName, true);
            String issueCreateResponseStatus = ParseJsonResponse.getStatusFromResponse(issueCreateResponse);

            if (!issueCreateResponseStatus.equalsIgnoreCase("success")) {
                throw new SkipException("Couldn't create Issue using Section: " + sectionName);
            }

            issueId = CreateEntity.getNewEntityId(issueCreateResponse);

            if (issueId == -1) {
                throw new SkipException("Couldn't get Id of newly created Issue.");
            }

            //Delete Parent Entity
            EntityOperationsHelper.deleteEntityRecord(parentEntityName, parentNewlyCreatedRecordId);
            parentNewlyCreatedRecordId = -1;

            //Validate that Issue is also deleted.
            String issueShowResponse = ShowHelper.getShowResponseVersion2(17, issueId);

            if (ShowHelper.isShowPageAccessible(issueShowResponse)) {
                csAssert.assertTrue(false, "Show Page for Issue Id: " + issueId + " is still accessible after Deleting Parent " +
                        parentEntityName + " Id: " + parentNewlyCreatedRecordId);
            } else {
                issueId = -1;
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Issue after Deleting Parent Entity " + parentEntityName + ". " + e.getMessage());
        } finally {
            if (parentNewlyCreatedRecordId != -1) {
                EntityOperationsHelper.deleteEntityRecord(parentEntityName, parentNewlyCreatedRecordId);
            }

            if (issueId != -1) {
                EntityOperationsHelper.deleteEntityRecord("issues", issueId);
            }
        }
    }
}