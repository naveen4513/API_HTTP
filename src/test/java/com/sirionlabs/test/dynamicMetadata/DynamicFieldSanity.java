package com.sirionlabs.test.dynamicMetadata;

import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.entityCreation.Contract;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONObject;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Map;

@Listeners(value = MyTestListenerAdapter.class)
public class DynamicFieldSanity {

    private String configFilePath = "src/test/resources/TestConfig/DynamicMetadata/DynamicFieldSanity";
    private String configFileName = "DynamicFieldSanity.cfg";
    private String extraFieldsFileName = "ExtraFields.cfg";

    @Test
    public void testSingleSelectFieldOnCreateEditPage() {
        CustomAssert csAssert = new CustomAssert();

        try {
            String createSectionName = "contracts flow 1";
            String createResponse = Contract.createContract(configFilePath, configFileName, configFilePath, extraFieldsFileName, createSectionName, true);
            String status = ParseJsonResponse.getStatusFromResponse(createResponse);

            Map<String, String> defaultProperties = ParseConfigFile.getAllDefaultProperties(configFilePath, configFileName);

            String expectedDynamicValue = defaultProperties.get("expectedvalue");

            if (status.equalsIgnoreCase("success")) {
                int contractId = CreateEntity.getNewEntityId(createResponse);

                //Validate Value on Show Page.
                String showResponse = ShowHelper.getShowResponseVersion2(61, contractId);
                String dynamicFieldObjectName = "dyn" + defaultProperties.get("dynamicfieldid");
                JSONObject jsonObj = new JSONObject(showResponse).getJSONObject("body").getJSONObject("data")
                        .getJSONObject("dynamicMetadata").getJSONObject(dynamicFieldObjectName);
                String actualValueOnShowPage = jsonObj.getJSONObject("values").getString("name");

                csAssert.assertEquals(actualValueOnShowPage, expectedDynamicValue, "Dynamic Field Value not matching on Show Page of Contract");

                //Validate Value on Edit Page
                Edit editObj = new Edit();
                String editResponse = editObj.hitEdit("contracts", contractId);
                jsonObj = new JSONObject(editResponse).getJSONObject("body").getJSONObject("data").getJSONObject("dynamicMetadata").getJSONObject(dynamicFieldObjectName);

                String actualValueOnEditPage = jsonObj.getJSONObject("values").getString("name");
                csAssert.assertEquals(actualValueOnEditPage, expectedDynamicValue, "Dynamic Field Value not matching on Edit Page of Contract");

                //Delete Contract
                EntityOperationsHelper.deleteEntityRecord("contracts", contractId);
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating Single Select Field on Create/Edit Page for Contract. " + e.getMessage());
        }

        csAssert.assertAll();
    }
}