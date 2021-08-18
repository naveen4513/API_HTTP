package com.sirionlabs.test.RoleGroupCreate;

import com.sirionlabs.api.clientAdmin.masterRoleGroups.MasterRoleGroupSubHeader;
import com.sirionlabs.api.clientAdmin.masterRoleGroups.MasterRoleGroups;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.test.reports.TestStatusTransitionReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TestCreateRoleGroup {

    private final static Logger logger = LoggerFactory.getLogger(TestCreateRoleGroup.class);

    @DataProvider
    public Object[][] dataProviderForAllEntity()
    {
        List<Object[]> allTestData = new ArrayList<>();
       String entitiesToTest ="suppliers, contracts, service data, purchase orders, invoices, contract draft request, obligations, child obligations, service levels, child service levels, actions, issues, disputes, invoice line item, change requests, interpretations, work order requests, governance body, governance body meetings, consumptions, clauses, contract templates, contract template structure, definition";
        String[] arr =entitiesToTest.split(",");
        for(String entityName:arr)
             allTestData.add(new Object[]{entityName.toLowerCase().trim()});
        return allTestData.toArray(new Object[0][]);
    }
    @Test(dataProvider = "dataProviderForAllEntity")
    public void createRoleGroup(String entityName)
    {
        HashMap<String,String> payload=new HashMap<>();
        int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
        payload.put("entityType.id",String.valueOf(entityTypeId));
        payload.put("validUserType","");
        payload.put("_listReportId","");
        payload.put("_chartIds","");
        payload.put("name","");
        payload.put("description","");
        payload.put("sequenceOrder","");
        payload.put("_defaultLoggedInUser","");
        payload.put("_multiValue","");
        payload.put("_excludeFromFilter","");
        payload.put("_active","");
        payload.put("_includeInVendorHierarchyGridView","");
        payload.put("grantAccessOnAssign","");
        payload.put("_grantAccessOnAssign","");
        payload.put("userRoleGroups","");
        payload.put("defaultTaskOwners","");
        payload.put("_userDepartments","");
        payload.put("_groupExclusiveDepartment","");
        payload.put("entityTypeIdsAvailingSecondaryAccess","");
        payload.put("_clauseCategories","");
        payload.put("_csrf_token","");
        payload.put("ajax","");
        payload.put("history","");
        APIResponse subheader= new MasterRoleGroupSubHeader().getMasterRoleGroupHeader(entityTypeId);
        String responseBody=subheader.getResponseBody();
        String[] responseEntity =responseBody.split(",");
        for (String response:responseEntity) {
            String[] arr1 = response.split(":");
            payload.put("subHeader",arr1[0]);
        }
    }
}
