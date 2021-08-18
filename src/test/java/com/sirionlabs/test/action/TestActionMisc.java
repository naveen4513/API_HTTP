package com.sirionlabs.test.action;

import com.sirionlabs.api.commonAPI.CreateLinks;
import com.sirionlabs.api.commonAPI.New;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Listeners(value = MyTestListenerAdapter.class)
public class TestActionMisc extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestActionMisc.class);


    @DataProvider(parallel = true)
    public Object[][] dataProvider() {
        List<Object[]> allTestData = new ArrayList<>();
        String[] allParentEntities = {"suppliers", "contracts", "invoices", "obligations", "service levels", "interpretations", "work order requests",
                "issues", "disputes", "change requests"};

        for (String parentEntity : allParentEntities) {
            allTestData.add(new Object[]{parentEntity});
        }
        return allTestData.toArray(new Object[0][]);
    }

    /*
    TC-C7960: Verify inheritance of fields in Action from its parent.
     */
    @Test(dataProvider = "dataProvider")
    public void testC7960(String parentEntityName) {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Verifying Inheritance of Fields in Action from Parent Entity {}", parentEntityName);
            int entityTypeId = ConfigureConstantFields.getEntityIdByName(parentEntityName);
            String payload = ListDataHelper.getPayloadForListData(entityTypeId, 50, 0);
            String listDataResponse = ListDataHelper.getListDataResponseVersion2(parentEntityName, payload);

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                ArrayList<Integer> allRecordIds = ListDataHelper.getAllDbIdsFromListPageResponse(listDataResponse);
                boolean permissionToCreateActionFound = false;

                for (Integer recordId : allRecordIds) {
                    String createLinksResponse = CreateLinks.getCreateLinksV2Response(entityTypeId, recordId);
                    Map<Integer, String> allCreateLinks = CreateLinks.getAllSingleCreateLinksMap(createLinksResponse);

                    if (allCreateLinks != null) {
                        if (allCreateLinks.containsKey(18)) {
                            permissionToCreateActionFound = true;

                            String createPath = allCreateLinks.get(18);
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
                            String[] fieldsArr = {"functions", "services", "regions", "countries", "tier"};

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

                if (!permissionToCreateActionFound) {
                    throw new SkipException("Couldn't find record of Entity " + parentEntityName + " having Permission to create Action.");
                }
            } else {
                csAssert.assertTrue(false, "ListData API Response for Parent Entity " + parentEntityName + " is an Invalid JSON.");
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C7960. " + e.getMessage());
        }

        csAssert.assertAll();
    }

    private void verifyFieldInheritance(String newResponse, String fieldName, String showResponse, String parentShowResponse, String entityName, int entityTypeId,
                                        int parentEntityTypeId, int recordId, CustomAssert csAssert) {
        try {
            String actionsFieldShowPageObjectName = ShowHelper.getShowPageObjectNameMapping("actions", fieldName);
            List<String> allOptionsOfField = ShowHelper.getAllOptionsOfField(newResponse, ShowHelper.getShowFieldHierarchy(actionsFieldShowPageObjectName, 18));

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
                            entityTypeId + " and Parent Record Id: " + recordId);
                }
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Inheritance of Field " + fieldName + " from Parent Entity " + entityName +
                    " and Record Id " + recordId);
        }
    }
}