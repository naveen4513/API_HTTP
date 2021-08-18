package com.sirionlabs.test.docusign;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.dto.docusignService.SendDTO;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.sirionlabs.api.docusignService.Send.*;


public class TestSend {

    private final static Logger logger = LoggerFactory.getLogger(TestSend.class);
    String testingType;
    private static String configFilePath;
    private static String configFileName;
    public static String entityId;
    public static String entityTypeId;
    public static String[] documentNames;
    public static String[] documentIds;
    public static String documentTypeCommunicationTab;
    public static String integrationId;
    public static String recipients;



    @Parameters({"TestingType"})
    @BeforeClass
    public void beforeClass(String testingType) {

        this.testingType = testingType;
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("DocuSignNewModuleConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("DocuSignNewModuleConfigFileName");
        entityId = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "mic-391", "entityid");
        entityTypeId = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "mic-391", "entitytypeid");
        recipients = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "mic-391", "recipients");
        integrationId = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "mic-391", "integrationid");
        documentTypeCommunicationTab = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "mic-391", "documenttypecommunicationtab");
        documentNames = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "mic-391", "documentnames").split(Pattern.quote(","));
        documentIds = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "mic-391", "documentids").split(Pattern.quote(","));

    }

    @DataProvider
    public Object[][] dataProviderJson() throws IOException {
        List<Object[]> allTestData = new ArrayList<>();

        String dataFilePath = "src/test/resources/TestConfig/Docusign";
        String dataFileName = "sendTestData.json";

        List<SendDTO> dtoObjectList = new ArrayList<>();
        String allJsonData = new FileUtils().getDataInFile(dataFilePath + "/" + dataFileName);

        JSONArray jsonArr = new JSONArray(allJsonData);

        for (int i = 0; i < jsonArr.length(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);

            if (jsonObj.getString("enabled").trim().equalsIgnoreCase("yes")) {
                if (jsonObj.getString("testingType").trim().toLowerCase().contains(testingType.toLowerCase())) {
                    SendDTO dtoObject = getDTOObjectFromJson(jsonObj);

                    if (dtoObject != null) {
                        dtoObjectList.add(dtoObject);
                    }
                }
            }
        }

        for (SendDTO dtoObject : dtoObjectList) {
            allTestData.add(new Object[]{dtoObject});
        }

        return allTestData.toArray(new Object[0][]);
    }

    private SendDTO getDTOObjectFromJson(JSONObject jsonObj) {
        SendDTO dtoObject = null;

        try {
            String testCaseId = jsonObj.getString("testCaseId");
            String description = jsonObj.getString("description");
            String path = jsonObj.getString("path");
            Object entityId = jsonObj.get("entityId");
            Object entityTypeId = jsonObj.get("entityTypeId");
            int expectedStatusCode = jsonObj.getInt("expectedStatusCode");
            String serviceType = jsonObj.getString("serviceType");
            Object documentName = jsonObj.get("documentName");
            Object documentId = jsonObj.get("documentId");
            Object type = jsonObj.get("type");
            Object docOrigin = jsonObj.get("documentType");
            Object name = jsonObj.get("name");
            Object userId = jsonObj.get("userId");
            String responseBody = jsonObj.getString("responseBody");

            dtoObject = new SendDTO(testCaseId, description, serviceType, expectedStatusCode, path, entityId, entityTypeId, documentName, documentId, type, docOrigin, name, userId, responseBody);
        } catch (Exception e) {
            logger.error("Exception while Getting Send Docusign DTO Object. {}", e.getMessage());
        }
        return dtoObject;
    }


    @Test(dataProvider = "dataProviderJson", priority = 1)
    public void testSendAPI(SendDTO dtoObject) {

        logger.info("Executing test case #:{} where case is: {} ", dtoObject.getTestCaseId(), dtoObject.getDescription());

        CustomAssert csAssert = new CustomAssert();

        Object entityId = dtoObject.getEntityId();
        Object entityTypeId = dtoObject.getEntityTypeId();
        Object name = dtoObject.getName();
        Object userId = dtoObject.getUserId();
        int expectedResponseCode = dtoObject.getExpectedStatusCode();
        String expectedresponseBody = dtoObject.getResponseBody();
        String docJson = getDocuments(dtoObject.getDocumentName(),dtoObject.getDocumentId(),dtoObject.getType(),dtoObject.getDocOrigin());


        String payload = "{\"entityTypeInfo\":{\"entityId\":" + entityId + ",\"entityTypeId\": " + entityTypeId + "},\"documents\":"+docJson +",\"recipients\":[{\"name\":\" " + name + " \",\"userId\": " + userId + " }]}";


        String serviceType = dtoObject.getServiceType();

        //  Hitting the API
        APIResponse response = postSendAPI(getNewApiPath(serviceType), getHeaders(), payload);

        // Verification of the response
        String actualresponseBody = response.getResponseBody();
        int actualResponseCode = response.getResponseCode();

        // Verifying valid JSON
        csAssert.assertTrue(ParseJsonResponse.validJsonResponse(actualresponseBody), "Get api response");

        // Actual verification
        csAssert.assertEquals(actualResponseCode, expectedResponseCode);
        csAssert.assertEquals(actualresponseBody, expectedresponseBody);

        csAssert.assertAll();
    }


    // Return the Json Document array as String
    public String getDocuments(Object documentName, Object documentId, Object type, Object docOrigin) {


        JSONObject obj = new JSONObject();
        JSONArray jsonArray =  new JSONArray();

        try {
            List<String> docNames = getListFromString(documentName.toString());
            List<String> docIds = getListFromString(documentId.toString());
            List<String> docTypes = getListFromString(type.toString());
            List<String> docOrigins = getListFromString(docOrigin.toString());

            for(int i =0; i < docNames.size(); i++) {
                obj.put("documentName", docNames.get(i));
                obj.put("documentId", docIds.get(i));
                obj.put("type", docTypes.get(i));
                obj.put("documentType", docOrigins.get(i));
                jsonArray.put(i,obj);
            }
        } catch (Exception e)  {
            logger.error("Exception while creating Document JSON Array for Payload {}", e.getMessage());
            Assert.fail("Failed to create the Document JSON Array for the test case.");
        }

        return jsonArray.toString();
    }

    // Return items from value of a Key mentioned in Test Data JSON in List form.
    public List<String> getListFromString(String s) {

        ArrayList<String> list = new ArrayList<String>();

        char[] arr = s.toCharArray();
        int j = 0;
        for(int i = 0 ; i < s.length(); i++) {
            if(arr[i] == ',') {
                list.add(s.substring(j,i));
                j =  i+2;
            }
        }
        list.add(s.substring(j,s.length()));

        return list;
    }
}
