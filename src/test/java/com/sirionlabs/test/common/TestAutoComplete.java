package com.sirionlabs.test.common;

import com.sirionlabs.api.clientAdmin.masterContractTypes.MasterContractTypesList;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Options;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.OptionsHelper;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestAutoComplete {

    private final static Logger logger = LoggerFactory.getLogger(TestAutoComplete.class);

    private String[] allFields = {"functions", "services"};
    private String[] allEntities = {"suppliers", "vendors", "contracts"};

    private OptionsHelper optionsHelperObj = new OptionsHelper();
    private Show showObj = new Show();
    private Options optionsObj = new Options();
    private AdminHelper adminHelperObj = new AdminHelper();
    private List<Map<String, String>> allFunctionsList = new ArrayList<>();
    private List<Map<String, String>> allServicesList = new ArrayList<>();

    @BeforeClass
    public void beforeClass() {
        adminHelperObj.loginWithClientAdminUser();
        String listResponse = MasterContractTypesList.getMasterContractTypesListResponseBody();

        new Check().hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));

        allFunctionsList = MasterContractTypesList.getAllFunctionsList(listResponse);
        allServicesList = adminHelperObj.getAllServicesList();
    }

    @DataProvider
    public Object[][] dataProviderForField() {
        List<Object[]> allTestData = new ArrayList<>();

        for (String field : allFields) {
            allTestData.add(new Object[]{field.trim()});
        }

        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForField")
    public void testAutoCompleteField(String fieldName) {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating AutoComplete Field {}", fieldName);
            validateAutoCompleteFieldOnShowPage(fieldName, csAssert);
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating AutoComplete Field: " + fieldName + ". " + e.getMessage());
        }

        csAssert.assertAll();
    }

    private void validateAutoCompleteFieldOnShowPage(String fieldName, CustomAssert csAssert) {
        int dropDownTypeId = optionsHelperObj.getDropDownId(fieldName);

        for (String entityName : allEntities) {
            try {
                String listDataResponse = ListDataHelper.getListDataResponseVersion2(entityName);

                if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                    JSONObject jsonObj = new JSONObject(listDataResponse);
                    String idColumn = TabListDataHelper.getColumnIdFromColumnName(listDataResponse, "id");
                    String idValue = jsonObj.getJSONArray("data").getJSONObject(0).getJSONObject(idColumn).getString("value");
                    int recordId = ListDataHelper.getRecordIdFromValue(idValue);

                    int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
                    showObj.hitShowVersion2(entityTypeId, recordId);
                    String showResponse = showObj.getShowJsonStr();

                    if (ParseJsonResponse.validJsonResponse(showResponse)) {
                        int parentEntityTypeId = getIdFromShowResponse(showResponse, entityName, "parentEntityTypeId", fieldName);
                        int parentEntityId = getIdFromShowResponse(showResponse, entityName, "parentEntityId", fieldName);
                        int fieldId = getIdFromShowResponse(showResponse, entityName, "fieldId", fieldName);

                        String supplierIds = null;

                        if (entityName.equalsIgnoreCase("contracts")) {
                            supplierIds = getSupplierIds(showResponse);
                        }

                        Map<String, String> paramsMap = getOptionsParamsMap("1", parentEntityTypeId, parentEntityId, entityTypeId, recordId, fieldId,
                                supplierIds, "");
                        optionsObj.hitOptions(dropDownTypeId, paramsMap);
                        String optionsResponse = optionsObj.getOptionsJsonStr();

                        matchAllFieldOptions(optionsResponse, fieldName, csAssert, " Show Page Validation for Entity " + entityName +
                                " and Record Id " + recordId);
                    } else {
                        csAssert.assertFalse(true, "Show API Response for Entity " + entityName + " and Record Id " + recordId + " is an Invalid JSON.");
                    }
                } else {
                    csAssert.assertFalse(true, "ListData API Response for Entity " + entityName + " is an Invalid JSON.");
                }
            } catch (Exception e) {
                csAssert.assertFalse(true, "Exception while Validating Auto Complete Field " + fieldName + " on Show Page of Entity " + entityName + ". " +
                        e.getMessage());
            }
        }
    }

    private Map<String, String> getOptionsParamsMap(String pageTypeId, int parentEntityTypeId, int parentEntityId, int entityTypeId, int entityId,
                                                    int fieldId, String supplierIds, String query) {
        Map<String, String> paramsMap = new HashMap<>();

        paramsMap.put("pageType", pageTypeId);
        paramsMap.put("entityTpeId", String.valueOf(entityTypeId));
        paramsMap.put("parentEntityTypeId", String.valueOf(parentEntityTypeId));

        if (parentEntityId != -1) {
            paramsMap.put("parentEntityId", String.valueOf(parentEntityId));
        } else {
            paramsMap.put("parentEntityId", "");
        }

        paramsMap.put("pageEntityTypeId", String.valueOf(entityTypeId));
        paramsMap.put("fieldId", String.valueOf(fieldId));
        paramsMap.put("entityId", String.valueOf(entityId));
        paramsMap.put("pageEntityId", String.valueOf(entityId));
        paramsMap.put("query", query);

        if (supplierIds != null) {
            paramsMap.put("supplierIds", supplierIds);
        }

        return paramsMap;
    }

    private int getIdFromShowResponse(String showResponse, String entityName, String property, String fieldName) {
        JSONObject jsonObj = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data");

        if (entityName.equalsIgnoreCase("vendors")) {
            switch (property.trim().toLowerCase()) {
                case "parententitytypeid":
                    return 2;

                case "parententityid":
                    return 1002;

                case "fieldid":
                    Map<String, String> fieldMap = ParseJsonResponse.getFieldByName(showResponse, fieldName);
                    return (fieldMap != null && fieldMap.containsKey("id")) ? Integer.parseInt(fieldMap.get("id")) : -1;

                default:
                    return -1;
            }
        } else {
            switch (property.trim().toLowerCase()) {
                case "parententitytypeid":
                    return jsonObj.getJSONObject("parentEntityTypeId").getInt("values");

                case "parententityid":
                    if (entityName.equalsIgnoreCase("contracts")) {
                        return -1;
                    }
                    return jsonObj.getJSONObject("parentEntityId").getInt("values");

                case "fieldid":
                    Map<String, String> fieldMap = ParseJsonResponse.getFieldByName(showResponse, fieldName);
                    return (fieldMap != null && fieldMap.containsKey("id")) ? Integer.parseInt(fieldMap.get("id")) : -1;

                default:
                    return -1;
            }
        }
    }

    private void matchAllFieldOptions(String optionsResponse, String fieldName, CustomAssert csAssert, String additionalInfo) {
        JSONArray jsonArr = new JSONObject(optionsResponse).getJSONArray("data");
        List<Integer> actualOptionsList = new ArrayList<>();

        for (int i = 0; i < jsonArr.length(); i++) {
            actualOptionsList.add(jsonArr.getJSONObject(i).getInt("id"));
        }

        List<Map<String, String>> expectedOptionsList = null;

        if (fieldName.equalsIgnoreCase("functions")) {
            expectedOptionsList = allFunctionsList;
        } else if (fieldName.equalsIgnoreCase("services")) {
            expectedOptionsList = allServicesList;
        }

        if (expectedOptionsList != null) {
            if (actualOptionsList.size() == expectedOptionsList.size()) {
                for (Map<String, String> expectedOptionMap : expectedOptionsList) {
                    int expectedId = Integer.parseInt(expectedOptionMap.get("id"));

                    if (!actualOptionsList.contains(expectedId)) {
                        csAssert.assertFalse(true, "Expected Option Id " + expectedId + " not found in Options Response." + additionalInfo);
                    }
                }
            } else {
                csAssert.assertFalse(true, "Expected Options Size: " + expectedOptionsList.size() + " and Actual Options Size: " +
                        actualOptionsList.size() + additionalInfo);
            }
        } else {
            csAssert.assertFalse(true, "Couldn't get All Expected Options." + additionalInfo);
        }
    }

    private String getSupplierIds(String showResponse) {
        String supplierIds = "";

        JSONArray jsonArr = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data").getJSONObject("parentEntityIds").getJSONArray("values");
        for (int i = 0; i < jsonArr.length(); i++) {
            supplierIds = supplierIds.concat(jsonArr.get(i).toString() + ",");
        }

        supplierIds = supplierIds.substring(0, supplierIds.length() - 1);

        return supplierIds;
    }
}