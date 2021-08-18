package com.sirionlabs.test.common;

import com.sirionlabs.api.clientAdmin.ClientShow;
import com.sirionlabs.api.clientAdmin.fieldLabel.FieldRenaming;
import com.sirionlabs.api.clientAdmin.fieldLabel.CreateForm;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.DefaultUserListMetadataHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.ListRendererFilterDataHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.clientAdmin.FieldLabelHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

public class TestColumnsAndFiltersMisc {

    private final static Logger logger = LoggerFactory.getLogger(TestColumnsAndFiltersMisc.class);

    private AdminHelper adminHelperObj = new AdminHelper();
    private FieldLabelHelper fieldLabelHelperObj = new FieldLabelHelper();
    private FieldRenaming fieldRenamingObj = new FieldRenaming();
    private DefaultUserListMetadataHelper defaultUserListHelperObj = new DefaultUserListMetadataHelper();
    private Check checkObj = new Check();

    private String fieldLabelCreateFormResponse;
    private String lastLoggedInEndUserName;
    private String lastLoggedInEndUserPassword;

    private List<String> entitiesToTest;

    @BeforeClass(groups = { "minor" })
    public void beforeClass() {
        lastLoggedInEndUserName = Check.lastLoggedInUserName;
        lastLoggedInEndUserPassword = Check.lastLoggedInUserPassword;

        adminHelperObj.loginWithClientAdminUser();

        logger.info("Hitting FieldLabel CreateForm API.");
        CreateForm createFormObj = new CreateForm();
        fieldLabelCreateFormResponse = createFormObj.hitFieldLabelCreateForm();

        checkObj.hitCheck(lastLoggedInEndUserName, lastLoggedInEndUserPassword);

        String[] entitiesArr = {"vendors", "suppliers", "issues"};
        entitiesToTest = new ArrayList<>(Arrays.asList(entitiesArr));
    }


    /*
    TC-C7870: Verify Filters and Columns Renaming.
    TC-C7883
     */
    @Test(groups = { "minor" })
    public void testColumnsAndFiltersRename() {
        CustomAssert csAssert = new CustomAssert();
        String fieldRenamingResponse = null;

        for (String entityName : entitiesToTest) {
            try {
                adminHelperObj.loginWithClientAdminUser();
                logger.info("Validating Columns and Filters Rename for Entity {}", entityName);
                String groupName = fieldLabelHelperObj.getEntityGroupName(entityName);
                Integer groupId = fieldLabelHelperObj.getFieldLabelGroupValueFromCreateFormAPI(fieldLabelCreateFormResponse, "Entity", groupName);

                if (groupId == null) {
                    csAssert.assertTrue(false, "Couldn't Get Group Id for Entity " + entityName);
                    continue;
                }

                String columnToBeRenamed = getColumnToBeRenamedForEntity(entityName);

                if (columnToBeRenamed == null) {
                    csAssert.assertTrue(false, "Couldn't Get Column to be Renamed for Entity " + entityName);
                    continue;
                }

                fieldRenamingResponse = fieldRenamingObj.hitFieldRenamingUpdate(1, groupId);

                if (ParseJsonResponse.validJsonResponse(fieldRenamingResponse)) {
                    String currentFieldClientFieldName = fieldRenamingObj.getClientFieldNameFromName(fieldRenamingResponse, columnToBeRenamed);
                    String newClientFieldName = currentFieldClientFieldName + " Rename Column";

                    logger.info("Updating Label for Column {} of Entity {}", columnToBeRenamed, entityName);
                    String payload = fieldRenamingResponse.replace("clientFieldName\":\"" + currentFieldClientFieldName,
                            "clientFieldName\":\"" + newClientFieldName);
                    fieldRenamingObj.hitFieldUpdate(payload);

                    checkObj.hitCheck(lastLoggedInEndUserName, lastLoggedInEndUserPassword);

                    String defaultUserListMetadataResponse = defaultUserListHelperObj.getDefaultUserListMetadataResponse(entityName);

                    List<String> allColumnNames = defaultUserListHelperObj.getAllColumnNames(defaultUserListMetadataResponse);

                    if (allColumnNames.isEmpty()) {
                        csAssert.assertTrue(false, "Couldn't get All Column Names for Entity " + entityName);
                        continue;
                    }

                    List<String> allFilterNames = defaultUserListHelperObj.getAllFilterMetadataNames(defaultUserListMetadataResponse);

                    if (allFilterNames.isEmpty()) {
                        csAssert.assertTrue(false, "Couldn't get All Filter Names for Entity " + entityName);
                        continue;
                    }

                    if (!allColumnNames.contains(newClientFieldName)) {
                        csAssert.assertTrue(false, "Column having New Name [" + newClientFieldName +
                                "] not found in DefaultUserListMetadata API Response for Entity " + entityName);
                    }

                    if (!allFilterNames.contains(newClientFieldName)) {
                        csAssert.assertTrue(false, "Filter having New Name [" + newClientFieldName +
                                "] not found in DefaultUserListMetadata API Response for Entity " + entityName);
                    }

                } else {
                    csAssert.assertTrue(false, "Field Renaming Response for Entity " + entityName + " is an Invalid JSON.");
                }
            } catch (Exception e) {
                csAssert.assertTrue(false, "Exception while Validating Columns and Filters Rename for Entity " + entityName + ". " + e.getMessage());
            } finally {
                adminHelperObj.loginWithClientAdminUser();

                //Revert Field Label
                fieldRenamingObj.hitFieldUpdate(fieldRenamingResponse);

                checkObj.hitCheck(lastLoggedInEndUserName, lastLoggedInEndUserPassword);
            }
        }

        csAssert.assertAll();
    }

    private String getColumnToBeRenamedForEntity(String entityName) {
        String columnName = null;

        switch (entityName) {
            case "vendors":
                columnName = "Created By";
                break;

            case "suppliers":
                columnName = "Status";
                break;

            case "issues":
                columnName = "Tier";
                break;
        }

        return columnName;
    }

    @Test(groups = { "minor" })
    public void testEnabledColumnsAndFilters() {
        CustomAssert csAssert = new CustomAssert();

        for (String entityName : entitiesToTest) {
            try {
                logger.info("Validating Enabled/Disabled Columns and Filters for Entity {}", entityName);
                String defaultUserListMetadataResponse = defaultUserListHelperObj.getDefaultUserListMetadataResponse(entityName);

                if (ParseJsonResponse.validJsonResponse(defaultUserListMetadataResponse)) {
                    List<String> allEnabledColumnQueryNames = defaultUserListHelperObj.getAllEnabledColumnQueryNames(defaultUserListMetadataResponse);
                    List<String> allEnabledFilterQueryNames = defaultUserListHelperObj.getAllEnabledFilterMetadataQueryNames(defaultUserListMetadataResponse);

                    String listDataResponse = ListDataHelper.getListDataResponse(entityName);

                    if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                        JSONObject jsonObj = new JSONObject(listDataResponse);
                        JSONArray jsonArr = jsonObj.getJSONArray("data");

                        if (jsonArr.length() > 0) {
                            jsonObj = jsonArr.getJSONObject(0);

                            for (String filterObj : JSONObject.getNames(jsonObj)) {
                                String columnName = jsonObj.getJSONObject(filterObj).getString("columnName");

                                if (!allEnabledColumnQueryNames.contains(columnName) && !columnName.equals("bulkcheckbox")) {
                                    csAssert.assertTrue(false, "Column [" + columnName + "] is present in List Data API Response for Entity " +
                                            entityName + " whereas it is disabled from Client Admin.");
                                }
                            }
                        } else {
                            csAssert.assertTrue(false, "Couldn't get any data in List Data API Response for Entity " + entityName);
                        }
                    } else {
                        csAssert.assertTrue(false, "ListData API Response for Entity " + entityName + " is an Invalid JSON.");
                    }

                    String filterDataResponse = ListRendererFilterDataHelper.getFilterDataResponse(entityName);

                    if (ParseJsonResponse.validJsonResponse(filterDataResponse)) {
                        List<String> allFilterNames = ListRendererFilterDataHelper.getAllFilterNames(filterDataResponse);

                        for (String filterName : allFilterNames) {
                            if (!allEnabledFilterQueryNames.contains(filterName)) {
                                csAssert.assertTrue(false, "Filter [" + filterName + "] is present in Filter Data API Response for Entity " +
                                        entityName + " whereas it is disabled from Client Admin.");
                            }
                        }
                    } else {
                        csAssert.assertTrue(false, "Filter Data API Response for Entity " + entityName + " is an Invalid JSON.");
                    }
                } else {
                    csAssert.assertTrue(false, "DefaultUserListMetadata API Response for Entity " + entityName + " is an Invalid JSON.");
                }
            } catch (Exception e) {
                csAssert.assertTrue(false, "Exception while Validating Enabled Columns and Filters for Entity " + entityName + ". " + e.getMessage());
            }
        }

        csAssert.assertAll();
    }


    @Test(groups = { "minor" })
    public void testAllDataOfFunctionsServicesRegionsCountries() {
        CustomAssert csAssert = new CustomAssert();

        try {
            String clientShowResponse = ClientShow.getClientShowResponse(1002);

            List<String> allExpectedFunctions = ClientShow.getAllFunctions(clientShowResponse);
            if (allExpectedFunctions == null || allExpectedFunctions.isEmpty()) {
                throw new SkipException("Couldn't get Functions data from Client Show Response for Entity ");
            }

            List<String> allExpectedServices = ClientShow.getAllServices(clientShowResponse);
            if (allExpectedServices == null || allExpectedServices.isEmpty()) {
                throw new SkipException("Couldn't get Services data from Client Show Response for Entity ");
            }

           /* List<String> allExpectedRegions = ClientShow.getAllRegions(clientShowResponse);
            if (allExpectedRegions == null || allExpectedRegions.isEmpty()) {
                throw new SkipException("Couldn't get Regions data from Client Show Response for Entity ");
            }

            List<String> allExpectedCountries = ClientShow.getAllCountries(clientShowResponse);
            if (allExpectedCountries == null || allExpectedCountries.isEmpty()) {
                throw new SkipException("Couldn't get Countries data from Client Show Response for Entity ");
            }*/

            for (String entityName : entitiesToTest) {
                logger.info("Validating Data of Functions for Entity {}", entityName);
                String filterDataResponse = ListRendererFilterDataHelper.getFilterDataResponse(entityName);

                if (ParseJsonResponse.validJsonResponse(filterDataResponse)) {
                    //Validate Functions Data.
                    validateFilterData(entityName, filterDataResponse, "functions", allExpectedFunctions, csAssert);

                    //Validate Services Data.
                    validateFilterData(entityName, filterDataResponse, "services", allExpectedServices, csAssert);

                    /*//Validate Regions Data.
                    validateFilterData(entityName, filterDataResponse, "regions", allExpectedRegions, csAssert);

                    //Validate Countries Data.
                    validateFilterData(entityName, filterDataResponse, "countries", allExpectedCountries, csAssert);*/
                } else {
                    csAssert.assertTrue(false, "FilterData API Response for Entity " + entityName + " is an Invalid JSON");
                }
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Data of Functions, Services, Regions and Countries. " + e.getMessage());
        }
        csAssert.assertAll();
    }

    private void validateFilterData(String entityName, String filterDataResponse, String filterName, List<String> allExpectedFilterOptions, CustomAssert csAssert) {
        List<String> allFilterNames = ListRendererFilterDataHelper.getAllFilterNames(filterDataResponse);

        if (allFilterNames.isEmpty()) {
            csAssert.assertTrue(false, "Couldn't get All Filter Names of Entity " + entityName);
            return;
        }

        if (allFilterNames.contains(filterName)) {
            Boolean filterOfAutoCompleteType = ListRendererFilterDataHelper.isFilterOfAutoCompleteType(filterDataResponse, filterName);

            List<Map<String, String>> allOptionsOfFilter;

            if (filterOfAutoCompleteType != null && filterOfAutoCompleteType) {
                int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
                allOptionsOfFilter = ListRendererFilterDataHelper.getAllOptionsOfAutoCompleteFilter(filterName, entityTypeId);
            } else {
                allOptionsOfFilter = ListRendererFilterDataHelper.getAllOptionsOfFilter(filterDataResponse, filterName);
            }

            if (allOptionsOfFilter.isEmpty()) {
                csAssert.assertTrue(false, "Couldn't get Options of Filter " + filterName + " of Entity " + entityName);
                return;
            }

            for (Map<String, String> optionMap : allOptionsOfFilter) {
                String optionName = optionMap.get("name");

                if (!allExpectedFilterOptions.contains(optionName)) {
                    csAssert.assertTrue(false, "Filter Option: [" + optionName + "] is present in Filter Data API Response for Entity " +
                            entityName + " and Filter " + filterName + " whereas it is not defined in Client Admin.");
                }
            }
        }
    }
}