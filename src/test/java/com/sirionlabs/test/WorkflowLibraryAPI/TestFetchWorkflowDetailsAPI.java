package com.sirionlabs.test.WorkflowLibraryAPI;

import com.jayway.jsonpath.JsonPath;
import com.sirionlabs.api.WorkflowLibraryAPI.FetchWorkflowDetailsAPI;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.test.microservice.workflowTwo.TestNodeDataDetailsAPI;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestFetchWorkflowDetailsAPI extends TestAPIBase {
    private final static Logger logger = LoggerFactory.getLogger(TestNodeDataDetailsAPI.class);
    String domain;
    Map<String, String> confmap;
    FetchWorkflowDetailsAPI fetchWorkflowDetailsAPI;
    PostgreSQLJDBC db = null;
    Map<String, String> flowmap;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        confmap = FetchWorkflowDetailsAPI.getAllConfigForGetWorkflowDataAPI("envinfo");
        domain = confmap.get("domain");
        db = new PostgreSQLJDBC(confmap.get("dbhost"), confmap.get("dbport"), confmap.get("maintenancedb").toUpperCase(), confmap.get("dbusername"), confmap.get("dbpassword"));
        fetchWorkflowDetailsAPI = new FetchWorkflowDetailsAPI();
    }

    @DataProvider()
    public Object[][] dataProviderForFetchWorkflowAPI() {
        List<Object[]> allTestData = new ArrayList<>();

        String[] flows = {"flow1", "flow2", "flow3"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }


    @Test(description = "C153237", dataProvider = "dataProviderForFetchWorkflowAPI")
    public void fetchWorkflowDataAPITest(String flow) {
        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        boolean statusFromResponse;
        String errorMessage;
        String workflowId;
        String workflowIdFromResponse;
        String entityTypeNameFromResponse;
        String entityIdFromResponse;
        String numberOfNodesFromResponse;
        List<List<String>> wf_baseDeletionStatusList;

        try {
            flowmap = FetchWorkflowDetailsAPI.getAllConfigForGetWorkflowDataAPI(flow);

            if (flowmap.get("deletedtest").equalsIgnoreCase("yes")) {
                wf_baseDeletionStatusList = db.doSelect("select id from wf_base where deleted=true order by id desc limit 1;");
                workflowId = wf_baseDeletionStatusList.get(0).get(0);
            } else {
                workflowId = flowmap.get("workflowid");
            }

            validator = fetchWorkflowDetailsAPI.hitFetchWorkflowDataAPICall(executor, domain, workflowId);
            logger.info(validator.getResponse().getResponseBody());

            if (flowmap.get("testcasetype").equalsIgnoreCase("positive")) {

                errorMessage = JsonPath.read(validator.getResponse().getResponseBody(), "errorMessages");
                statusFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "success");
                workflowIdFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "entity.id").toString();
                entityIdFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "entity.entityType.id").toString();
                entityTypeNameFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "entity.entityType.name");
                numberOfNodesFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "entity.wfNodes.length()").toString();

                csAssert.assertEquals(validator.getResponse().getResponseCode().intValue(), Integer.parseInt(flowmap.get("statuscode")));
                csAssert.assertTrue(statusFromResponse, "Response is false,it should be true");
                csAssert.assertTrue(errorMessage == null, "In case of success, errormessage should be null");
                csAssert.assertTrue(workflowId.equalsIgnoreCase(workflowIdFromResponse), "workflowId from response is incorrect");
                csAssert.assertTrue(entityIdFromResponse.equalsIgnoreCase(flowmap.get("entitytypeid")), "entitytypeid is not matching with response");
                csAssert.assertTrue(entityTypeNameFromResponse.equalsIgnoreCase(flowmap.get("entitytypename")), "entitytypeName is not matching with response");
                csAssert.assertTrue(numberOfNodesFromResponse.equalsIgnoreCase(flowmap.get("nodenumber")), "Number of Nodes are unequal");
            } else {
                statusFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "success");
                csAssert.assertEquals(validator.getResponse().getResponseCode().intValue(), Integer.parseInt(flowmap.get("statuscode")));
                errorMessage = JsonPath.read(validator.getResponse().getResponseBody(), "errorMessages[0].errorMessage");
                csAssert.assertTrue(errorMessage.equalsIgnoreCase(flowmap.get("errormessage")), "Error message is incorrect");
                csAssert.assertFalse(statusFromResponse, "Response is true,it should be false");
            }

            csAssert.assertAll();
        } catch (Exception exception) {
            exception.printStackTrace();
            Assert.fail();
        }

    }
}