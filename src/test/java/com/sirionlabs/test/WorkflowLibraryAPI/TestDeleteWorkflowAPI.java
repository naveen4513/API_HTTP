package com.sirionlabs.test.WorkflowLibraryAPI;

import com.jayway.jsonpath.JsonPath;
import com.sirionlabs.api.TaskLibraryAPI.CreateTaskAPI;
import com.sirionlabs.api.WorkflowLibraryAPI.FetchWorkflowDetailsAPI;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.test.microservice.workflowTwo.TestWorkflowRequestDataAPI;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestDeleteWorkflowAPI extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestWorkflowRequestDataAPI.class);
    String domain;
    Map<String, String> confmap;
    Map<String, String> flowmap;
    PostgreSQLJDBC db = null;
    FetchWorkflowDetailsAPI fetchWorkflowDetailsAPI;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        logger.debug("in Before Class");
        confmap = CreateTaskAPI.getAllConfigForCreateGeneralTaskAPI("envinfo");
        domain = confmap.get("domain");
        db = new PostgreSQLJDBC(confmap.get("dbhost"), confmap.get("dbport"), confmap.get("maintenancedb").toUpperCase(), confmap.get("dbusername"), confmap.get("dbpassword"));
        fetchWorkflowDetailsAPI = new FetchWorkflowDetailsAPI();
    }

    @DataProvider()
    public Object[][] dataProviderForDeleteWorkflowAPI() {
        List<Object[]> allTestData = new ArrayList<>();

        String[] flows = {"flow1", "flow2"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(description = "C153237", dataProvider = "dataProviderForDeleteWorkflowAPI")
    public void createWorkflowDataAPITest(String flow) {
        CustomAssert csAssert = new CustomAssert();
        APIValidator validator;
        boolean statusFromResponse;
        String errorMessage;
        String workflowId;
        List<List<String>> wf_baseDeletionStatusList;

        try {
            flowmap = FetchWorkflowDetailsAPI.getAllConfigForDeleteWorkflowAPI(flow);
            if (flowmap.get("deletedtest").equalsIgnoreCase("yes")) {
                wf_baseDeletionStatusList = db.doSelect("select id from wf_base where deleted=true order by id desc limit 1;");
                workflowId = wf_baseDeletionStatusList.get(0).get(0);
            } else {
                workflowId = flowmap.get("workflowid");
            }

            validator = fetchWorkflowDetailsAPI.hitDeleteWorkflowAPICall(executor, domain, workflowId);
            logger.info(validator.getResponse().getResponseBody());

            csAssert.assertEquals(validator.getResponse().getResponseCode().intValue(), Integer.parseInt(flowmap.get("statuscode")));
            statusFromResponse = JsonPath.read(validator.getResponse().getResponseBody(), "success");

            csAssert.assertAll();
        } catch (Exception exception) {
            exception.printStackTrace();
            Assert.fail(flow + " test case failed");
        }
    }

    @AfterClass
    public void afterClass() {
        logger.debug("in After Class");
        db.closeConnection();
    }
}
