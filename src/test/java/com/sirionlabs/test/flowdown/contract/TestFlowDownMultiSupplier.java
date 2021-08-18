package com.sirionlabs.test.flowdown.contract;

import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.New;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.metadataSearch.MetadataSearch;
import com.sirionlabs.api.metadataSearch.Search;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.test.search.TestSearchMetadata;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.PayloadUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.util.*;

public class TestFlowDownMultiSupplier {

    private final static Logger logger = LoggerFactory.getLogger(TestFlowDownMultiSupplier.class);
    private String configFilePath, configFilePath2;
    private String configFileName, configFileName2;
    private String extraFieldsConfigFileName;

    @BeforeClass()
    public void beforeClass(){

        configFilePath2 = ConfigureConstantFields.getConstantFieldsProperty("MicroserviceConfigFilePath");
        configFileName2 = ConfigureConstantFields.getConstantFieldsProperty("MicroserviceConfigFileName");
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TestMultiSupplierContractConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TestMultiSupplierContractConfigFileName");
        extraFieldsConfigFileName = "ExtraFieldsForMultiSupplierContract.cfg";

    }

    // C90473
    @Test(enabled = true)
    public void testC90473() {

        CustomAssert csAssert = new CustomAssert();

        // Creating a MultiSupplier Contract
        Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, "c90473");

        String[] parentSupplierIdsArr = flowProperties.get("sourceid").split(",");
        String payload = "{\"documentTypeId\":4,\"parentEntity\":{\"entityIds\":" + Arrays.toString(parentSupplierIdsArr) + ",\"entityTypeId\":1}," +
                "\"actualParentEntity\":{\"entityIds\":[" + parentSupplierIdsArr[0] + "],\"entityTypeId\":1},\"sourceEntity\":{\"entityIds\":[" +
                parentSupplierIdsArr[0] + "],\"entityTypeId\":1}}";

        String createResponse = multiSupplierCreateResponse(payload, "c90473");
        int entityId = CreateEntity.getNewEntityId(createResponse, "contracts");

        // Getting the Stakeholders from Input Suppliers
        List<String> stakeholdersList = new ArrayList<>();

        String suppliers =  ParseConfigFile.getValueFromConfigFile(configFilePath2, configFileName2, "c90473", "suppliers");
        String roleGroupId =  ParseConfigFile.getValueFromConfigFile(configFilePath2, configFileName2, "c90473", "rolegroupid");
        String[] suppArr = suppliers.substring(1,suppliers.length()-1).split(",");

        for(int i = 0 ; i < suppArr.length ; i++) {

            Show show = new Show();
            show.hitShow(1, Integer.parseInt(suppArr[i]));
            String response = show.getShowJsonStr();
            JSONObject obj = new JSONObject(response);
            JSONObject obj2 = obj.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values").getJSONObject("rg_" + roleGroupId + "");
            JSONArray objArr = obj2.getJSONArray("values");
            for(int j = 0 ; j < objArr.length() ; j++){
                stakeholdersList.add(objArr.getJSONObject(j).get("name").toString());
            }

        }

        // Getting the Stakeholders from the newly created MPC
        List<String> actualStakeholdersList = new ArrayList<>();

        Show show = new Show();
        show.hitShow(61, entityId);
        String response = show.getShowJsonStr();
        JSONObject obj = new JSONObject(response);
        JSONObject obj2 = obj.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values").getJSONObject("rg_" + 3167 + "");
        JSONArray objArr = obj2.getJSONArray("values");
        for(int j = 0 ; j < objArr.length() ; j++){
            actualStakeholdersList.add(objArr.getJSONObject(j).get("name").toString());
        }

        // Compare the stakeholers -- Verification
        Collections.sort(stakeholdersList);
        Collections.sort(actualStakeholdersList);
        csAssert.assertEquals(actualStakeholdersList, stakeholdersList, "Stakeholders don't match for MPC");

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Hit the MetaData Search API to check if the flowndown role group is appearing for CE.
        payload =  ParseConfigFile.getValueFromConfigFile(configFilePath2, configFileName2, "c90473", "payload");

        // Hit the MetaData API with specific payload and verify the result
        Search searchObj = new Search();
        String searchResponse = searchObj.hitSearch(61, payload);

        Boolean isFound = false;

        obj = new JSONObject(searchResponse);
        JSONArray arr = obj.getJSONObject("body").getJSONObject("data").getJSONObject("searchResults").getJSONObject("values").getJSONArray("data");
        for(int i = 0 ; i < arr.length() ; i++) {
            String[] value = arr.getJSONObject(i).getJSONObject("18").getString("value").split(":;");
            if (value[1].equals(String.valueOf(entityId)) || value[1].equals("33124")) {
                isFound = true;
                break;
            }
        }

        csAssert.assertTrue(isFound, "Newly created MPC is not present in the response of MetadataSearch API.");

        // Deleting the record
        EntityOperationsHelper.deleteEntityRecord("contracts", entityId);
        csAssert.assertAll();
    }

    public String multiSupplierCreateResponse(String newPayload, String contractCreateSection) {
        logger.info("Hitting New V1 API for Contracts");
        New newObj = new New();
        newObj.hitNewV1ForMultiSupplier("contracts", newPayload);
        String newResponse = newObj.getNewJsonStr();

        if (newResponse != null) {
            if (ParseJsonResponse.validJsonResponse(newResponse)) {
                CreateEntity createEntityHelperObj = new CreateEntity(configFilePath, configFileName, configFilePath, extraFieldsConfigFileName,
                        contractCreateSection);

                Map<String, String> extraFields = createEntityHelperObj.setExtraRequiredFields("contracts");
                newObj.setAllRequiredFields(newResponse);
                Map<String, String> allRequiredFields = newObj.getAllRequiredFields();
                allRequiredFields = createEntityHelperObj.processAllChildFields(allRequiredFields, newResponse);
                allRequiredFields = createEntityHelperObj.processNonChildFields(allRequiredFields, newResponse);

                String createPayload = PayloadUtils.getPayloadForCreate(newResponse, allRequiredFields, extraFields, null, configFilePath,
                        extraFieldsConfigFileName);

                if (createPayload != null) {
                    logger.info("Hitting Create Api for Entity for Multi Supplier Contract");
                    Create createObj = new Create();
                    createObj.hitCreate("contracts", createPayload);
                    return createObj.getCreateJsonStr();
                } else {
                    logger.error("Contract Create Payload is null and hence cannot create Multi Supplier Contract.");
                }
            } else {
                logger.error("New V1 API Response is an Invalid JSON for Contracts.");
            }
        } else {
            logger.error("New API Response is null.");
        }

        return null;
    }

}

