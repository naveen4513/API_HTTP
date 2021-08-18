package com.sirionlabs.test.api.workflowPod.workflowLayout;

import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class TestWorkflowLayoutCreateFormLayoutInfo {

    private final static Logger logger = LoggerFactory.getLogger(TestWorkflowLayoutCreateFormLayoutInfo.class);


    @DataProvider
    public Object[][] dataProvider() {
        List<Object[]> allTestData = new ArrayList<>();

        String[] allEntities = {"actions", "change requests", "child obligations", "child service levels", "clauses", "consumptions", "contract draft request", "contracts",
                "contract templates", "contract template structure", "disputes", "governance body", "governance body meeting", "interpretations", "invoice line item",
                "invoices", "issues", "obligations", "purchase orders", "service data", "service levels", "suppliers", "vendors", "work order requests"};

        for (String entity : allEntities) {
            allTestData.add(new Object[]{entity});
        }

        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProvider")
    public void testWorkflowLayoutCreateFormLayoutInfoAPI(String entityName) {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating WorkflowLayout CreateForm LayoutInfo API for Entity {}", entityName);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating WorkflowLayout CreateForm LayoutInfo API for Entity " + entityName + ". " +
                    e.getMessage());
        }
        csAssert.assertAll();
    }
}