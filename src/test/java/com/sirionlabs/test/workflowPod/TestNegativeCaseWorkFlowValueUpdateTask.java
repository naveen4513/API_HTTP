package com.sirionlabs.test.workflowPod;
import com.sirionlabs.api.clientAdmin.workflow.WorkFlowCreate;
import com.sirionlabs.api.commonAPI.CreateLinks;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
public class TestNegativeCaseWorkFlowValueUpdateTask {
    private final static Logger logger = LoggerFactory.getLogger(TestWorkFlowTask.class);
    private String configFilePath;
    private String configFileName;
    private String dataFilePath = "src/test/resources/TestData/WorkFlowTask";
    private String dataFileName = "ContractNegativeValueUpdate.xlsx";

    @BeforeClass
    public void beforeClass() {
        WorkFlowCreate workFlowCreate=new WorkFlowCreate();
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        workFlowCreate.hitWorkflowCreate(dataFileName+sdf.format(cal.getTime()),String.valueOf(5636),String.valueOf(61),dataFilePath,dataFileName);
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestWorkFlowTaskConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestWorkFlowTaskNegativeConfigFileName");
    }


    @DataProvider
    public Object[][] dataProvider() {
        List<Object[]> allTestData = new ArrayList<>();
        if (configFileName != null && configFilePath != null) {
            String[] entityToTest = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entitytotest").split(",");
            for (String s : entityToTest) {
                Map<String, String> allConstantProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, s.trim().toLowerCase());
                if (!allConstantProperties.isEmpty()) {
                    allTestData.add(new Object[]{allConstantProperties.get("functions"), allConstantProperties.get("services"), allConstantProperties.get("parententityid"), allConstantProperties.get("parententitytypeid"), allConstantProperties.get("parententityname"), allConstantProperties.get("childentityname"), allConstantProperties.get("childentitytypeid")});
                }

            }
        }

        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProvider")
    public void testInheritanceProperties(String functions, String services, String parentEntityId, String parentEntityTypeId, String parentEntityName, String childEntityName, String childEntityTypeId) {
        CustomAssert customAssert = new CustomAssert();
        try {
            List<String> expectedFunctions = getActualValue(functions);
            List<String> expectedServices = getActualValue(services);
            String createLinksV2Response = CreateLinks.getCreateLinksV2Response(Integer.parseInt(parentEntityTypeId), Integer.parseInt(parentEntityId));
            String createLinkForEntity = CreateLinks.getCreateLinkForEntity(createLinksV2Response, Integer.parseInt(childEntityTypeId));
            String jspApiResponse = CreateLinks.getJSPAPIResponse(createLinkForEntity);
            JSONObject jsonObject = new JSONObject(jspApiResponse).getJSONObject("body").getJSONObject("data");
            List<String> actualFunctions = getValue("functions", jsonObject);
            List<String> actualServices = getValue("services", jsonObject);
            for (String expectedFunction : expectedFunctions) {
                if (!actualFunctions.contains(expectedFunction)) {
                    customAssert.assertTrue(false, "parent function name { " + expectedFunction + " } not found in child entity");
                }
            }
            customAssert.assertTrue(expectedFunctions.size() == actualFunctions.size(), "number of expected function on parent entity and child entity are different");

            for (String expectedService : expectedServices) {
                if (!actualFunctions.contains(expectedService)) {
                    customAssert.assertTrue(false, "parent service name { " + expectedServices + " } not found in child entity");
                }
            }
            customAssert.assertTrue(expectedServices.size() == actualServices.size(), "number of expected services on parent entity and child entity are different");
        } catch (Exception e) {
            logger.error("Exception while verify function and services on child entity");
            customAssert.assertTrue(false, e.getMessage());
        }
        customAssert.assertAll();
    }

    public List<String> getValue(String fieldName, JSONObject data) {
        List<String> value = new ArrayList<>();
        JSONArray jsonArrayName=data.getJSONObject(fieldName).names();
        if(jsonArrayName.toList().contains("values")) {
            JSONArray jsonArray = data.getJSONObject(fieldName).getJSONArray("values");
            for (int i = 0; i < jsonArray.length(); i++) {
                value.add(jsonArray.getJSONObject(i).getString("name"));

            }
            return value;
        }
        return new ArrayList<>();
    }

    private List<String> getActualValue(String value) {

        List<String> allFiledName = new ArrayList<>();
        if(value.isEmpty())
            return new ArrayList<>();
        String[] columnName = value.split(",");
        for (String column : columnName) allFiledName.add(column.toLowerCase().trim());
        return allFiledName;
    }

}
