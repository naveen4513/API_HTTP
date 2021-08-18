package com.sirionlabs.test.preSignature;

import com.sirionlabs.api.clientAdmin.report.ClientSetupAdminReportList;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.api.commonAPI.New;
import com.sirionlabs.api.presignature.Template;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.DefaultUserListMetadataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.Reports.ReportsListHelper;
import com.sirionlabs.helper.UserTasksHelper;
import com.sirionlabs.helper.entityCreation.*;
import com.sirionlabs.helper.preSignature.PreSignatureHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class TestPreSignatureNew {

    private final static Logger logger = LoggerFactory.getLogger(TestPreSignatureNew.class);
    private static String configFilePath;
    private static String configFileName;
    private static String contractCreationConfigFilePath;
    private static String contractCreationConfigFileName;
    private Template template = new Template();
    private Check check = new Check();

    @BeforeClass
    public void before() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("preSignatureRegressionConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("preSignatureRegressionConfigFileName");
        contractCreationConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("preSignatureContractCreationConfigFilePath");
        contractCreationConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("preSignatureContractCreationConfigFileName");
    }

    @DataProvider
    public Object[][] clauseTestData() {
        return new Object[][]{
                {RandomString.getRandomAlphaNumericString(200), RandomString.getRandomAlphaNumericString(200)},
                {"5454vmfv8954943@#@#DFdf", "$#$#%$%$dvfv;fppwd./.vvf"}
        };
    }

    // Template and Template Style Cases
    @Test
    public void TestVerifyTemplateFormattingFromClientAdmin() {
        CustomAssert customAssert = new CustomAssert();
        try {
            boolean isEntityConfigurationPresent = false;
            boolean isTemplateFormattingPresent = false;
            HttpResponse httpResponse = check.hitCheck(ConfigureEnvironment.getEnvironmentProperty("clientUsername"),
                    ConfigureEnvironment.getEnvironmentProperty("clientUserPassword"));
            customAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200, "Client Admin Login Response Code is not Valid");

            // Admin Show Page Uri
            String Uri = "/admin";
            httpResponse = template.verifyTemplateFormattingClientAdmin(Uri);
            customAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200, "Show Page Response Code is not Valid");
            String clientAdminShowPageResponseStr = EntityUtils.toString(httpResponse.getEntity());

            // Extracting Html Document
            Document document = Jsoup.parse(clientAdminShowPageResponseStr);

            Elements allHeadings = document.select("#mainContainer h3");

            for (Element e : allHeadings) {
                if (e.text().trim().equals("Entity Configuration")) {
                    isEntityConfigurationPresent = true;
                    List<String> links = e.parent().select("li a").stream().map(m -> m.text().trim()).collect(Collectors.toList());
                    for (String link : links) {
                        if (link.equals("Template Formatting")) {
                            isTemplateFormattingPresent = true;
                            break;
                        }
                    }
                }
            }

            customAssert.assertTrue(isEntityConfigurationPresent, "Entity Configuration is not present in client admin show page");
            customAssert.assertTrue(isTemplateFormattingPresent, "Template Formatting Option is not present inside Entity Configuration in client admin  show page");

            // Word Style List URI
            Uri = "/wordStyle/list";
            httpResponse = template.verifyTemplateFormattingClientAdmin(Uri);
            customAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200, "Word Style List Response Code is not valid");
            String wordStyleResponseStr = EntityUtils.toString(httpResponse.getEntity());

            Document wordStyleDocument = Jsoup.parse(wordStyleResponseStr);

            List<String> allStyles = wordStyleDocument.select("#clientTd option").stream().map(m -> m.text().trim()).collect(Collectors.toList());
            customAssert.assertTrue(allStyles.contains("Default Style"), "Default Style is not present in style dropdown to select");

            List<String> listingHeaders = wordStyleDocument.select("#mainContainer #_title_pl_com_sirionlabs_model_MasterGroup_id thead tr th")
                    .stream().map(m -> m.text().trim()).collect(Collectors.toList());

            customAssert.assertTrue(listingHeaders.contains("Id") && listingHeaders.contains("Name")
                    && listingHeaders.contains("Default") && listingHeaders.contains("Active"), "All Default headers are not present");

            int tableRowsSize = wordStyleDocument.select("#mainContainer #_title_pl_com_sirionlabs_model_MasterGroup_id tbody tr").size();

            List<List<String>> tableRowsData = PreSignatureHelper.getTemplateStyleListingData(tableRowsSize, wordStyleDocument);
            customAssert.assertTrue(tableRowsData.size() >= 1, "There are no style present in listing table i.e default is also missing");

        } catch (Exception ex) {
            logger.info("Exception while verifying template formatting " + ex.getMessage());
        } finally {
            HttpResponse checkResponse = check.hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"),
                    ConfigureEnvironment.getEnvironmentProperty("password"));
            customAssert.assertTrue(checkResponse.getStatusLine().getStatusCode() == 200, "End User Login Check API Response is not Valid");
        }
        customAssert.assertAll();
    }

    @Test(priority = 1)
    public void TestStyleTemplateCreationClientAdmin() throws IOException {
        CustomAssert customAssert = new CustomAssert();
        HttpResponse httpResponse = check.hitCheck(ConfigureEnvironment.getEnvironmentProperty("clientUsername"),
                ConfigureEnvironment.getEnvironmentProperty("clientUserPassword"));
        customAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200, "Client Admin Login Response Code is not Valid.");

        // Create Template Style
        String url = "/wordStyle/create";
        String templateName = "Template" + RandomNumbers.getRandomNumberWithinRange(0, 1000);
        String payload = "{\"templateName\":\"" + templateName + "\",\"generalFont\":{\"id\":1103,\"font\":{\"name\":\"Times New Roman\",\"id\":2}," +
                "\"fontSize\":14,\"paragraphAlignment\":{\"name\":\"Justified\",\"id\":4},\"bold\":true,\"underline\":false,\"italic\":false}," +
                "\"templateTitleFont\":{\"id\":1104,\"font\":{\"name\":\"arial\",\"id\":1},\"fontSize\":20,\"paragraphAlignment\":{\"name\":\"Centre\",\"id\":3}," +
                "\"bold\":true,\"underline\":false,\"italic\":false},\"clauseHeaderFont\":{\"id\":1105,\"font\":{\"name\":\"arial\",\"id\":1},\"fontSize\":14," +
                "\"paragraphAlignment\":{\"name\":\"Left\",\"id\":1},\"bold\":true,\"underline\":false,\"italic\":false},\"clauseSubHeaderFont\":{\"id\":1106," +
                "\"font\":{\"name\":\"arial\",\"id\":1},\"fontSize\":12,\"paragraphAlignment\":{\"name\":\"Left\",\"id\":1},\"bold\":true,\"underline\":false," +
                "\"italic\":false},\"header\":{\"id\":1053,\"fontStyle\":{\"id\":1107,\"font\":{\"name\":\"arial\",\"id\":1},\"fontSize\":12," +
                "\"paragraphAlignment\":{\"name\":\"Left\",\"id\":1},\"bold\":false,\"underline\":false,\"italic\":true},\"text\":\"Sample Header\"," +
                "\"imagePath\":\"\",\"imageAlignment\":{\"name\":\"Right\",\"id\":2}},\"footer\":{\"id\":1054,\"fontStyle\":{\"id\":1108," +
                "\"font\":{\"name\":\"arial\",\"id\":1},\"fontSize\":12,\"paragraphAlignment\":{\"name\":\"Left\",\"id\":1},\"bold\":false,\"underline\":true," +
                "\"italic\":true},\"text\":\"Sample Footer\",\"imagePath\":\"\",\"imageAlignment\":{\"name\":\"Left\",\"id\":1}},\"pageNumberStyle\":{\"id\":1018," +
                "\"alignment\":{\"name\":\"Right\",\"id\":2},\"pageNumberFormatEnum\":{\"name\":\"Page X of Y\",\"id\":2},\"fontSize\":10," +
                "\"position\":{\"name\":\"Bottom of The Page\",\"id\":2}},\"levelToListStyle\":null,\"listLayoutId\":1,\"coverPageText\":\"\"," +
                "\"coverPageHtml\":\"<style>.table-bordered, .table-bordered>tbody>tr>td, .table-bordered>tbody>tr>th, .table-bordered>tfoot>tr>td, " +
                ".table-bordered>tfoot>tr>th, .table-bordered>thead>tr>td, .table-bordered>thead>tr>th{\\t border: 1px solid #ddd;      " +
                "border-collapse: collapse;}.table {   width: 100%;    max-width: 100%;    margin-bottom: 20px;}</style><p><br></p>\",\"docHtmlPath\":null," +
                "\"clientId\":1002,\"active\":true,\"defaultStyle\":false,\"firstLevelNumberingDefinition\":true,\"secondLevelNumberingDefinition\":false," +
                "\"tableOfContent\":true,\"coverPage\":false}";
        HttpResponse templateCreationResponse = template.createOrPreviewOrEditStyleTemplate(url, payload);
        String templateCreationResponseStr = EntityUtils.toString(templateCreationResponse.getEntity());

        JSONObject jsonObject = new JSONObject(templateCreationResponseStr);
        try {
            if (jsonObject.getJSONObject("wordFormatStructure").isNull("templateStyle")) {

                // Verify Error Message for Already Existing Style Template
                logger.warn("This template with name " + templateName + " already exist. Thus, not creating new one");
                customAssert.assertTrue(Integer.parseInt(jsonObject.get("httpStatusCode").toString()) == 400, "Response is not valid");
                customAssert.assertTrue(jsonObject.getJSONArray("error").toList().stream().map(Object::toString).
                                collect(Collectors.toList()).stream().findFirst().get().equals("A Style already exist with same name. Can not save current style.")
                        , "There is some error while creating template style");
                customAssert.assertTrue(jsonObject.getJSONObject("wordFormatStructure").get("templateStyle").toString().equals("null"),
                        "Template Name is different it is not successfully created");

                // Verify Result on Template Style Listing
                int templateId = PreSignatureHelper.isTemplateAddedToListing(templateName);
                customAssert.assertTrue(templateId > 0, "New template is not created");

                // Navigate to the Created Template Show Page
                httpResponse = template.showTemplate(templateId);
                String showTemplateResponseStr = EntityUtils.toString(httpResponse.getEntity());
                jsonObject = new JSONObject(showTemplateResponseStr);
                customAssert.assertTrue(Integer.parseInt(jsonObject.get("httpStatusCode").toString()) == 200, "Response Code is not Valid");

                // Edit Created Template
                url = "/wordStyle/update";
                String newTemplateName = templateName + RandomNumbers.getRandomNumberWithinRange(0, 1000);
                jsonObject.getJSONObject("wordFormatStructure").getJSONObject("templateStyle").put("templateName", newTemplateName);
                template.createOrPreviewOrEditStyleTemplate(url, jsonObject.getJSONObject("wordFormatStructure").getJSONObject("templateStyle").toString());

                // Verify Whether template is Updated
                httpResponse = template.showTemplate(templateId);
                showTemplateResponseStr = EntityUtils.toString(httpResponse.getEntity());
                jsonObject = new JSONObject(showTemplateResponseStr);
                customAssert.assertTrue(Integer.parseInt(jsonObject.get("httpStatusCode").toString()) == 200, "Response Code is not Valid");
                customAssert.assertTrue(jsonObject.getJSONObject("wordFormatStructure").getJSONObject("templateStyle").get("templateName").toString().equals(newTemplateName),
                        "Template is not getting updated");
            } else {
                // Verify Newly Created Style Template
                customAssert.assertTrue(Integer.parseInt(jsonObject.get("httpStatusCode").toString()) == 200, "Response is not valid");
                customAssert.assertTrue(jsonObject.get("error").toString().equals("null"), "There is some error while creating template style");
                customAssert.assertTrue(jsonObject.getJSONObject("wordFormatStructure").getJSONObject("templateStyle").get("templateName")
                        .toString().trim().equals(templateName), "Template Name is different it is not successfully created");

                // Preview Created Template Style
                url = "/wordStyle/preview";
                HttpResponse templatePreviewResponse = template.createOrPreviewOrEditStyleTemplate(url, payload);
                String templatePreviewResponseStr = EntityUtils.toString(templatePreviewResponse.getEntity());

                jsonObject = new JSONObject(templatePreviewResponseStr);
                customAssert.assertTrue(Integer.parseInt(jsonObject.get("httpStatusCode").toString()) == 200, "Response is not valid");
                customAssert.assertTrue(jsonObject.get("error").toString().equals("null"), "There is some error while creating template style");

                customAssert.assertTrue(jsonObject.get("wordFormatStructure").toString().equals("null"),
                        "Template is not in preview state as Word Format Structure already have data in it");

                // Creating Same Template again with same name
                url = "/wordStyle/create";
                templateCreationResponse = template.createOrPreviewOrEditStyleTemplate(url, payload);
                templateCreationResponseStr = EntityUtils.toString(templateCreationResponse.getEntity());

                jsonObject = new JSONObject(templateCreationResponseStr);
                customAssert.assertTrue(Integer.parseInt(jsonObject.get("httpStatusCode").toString()) == 400, "Response is not valid");
                customAssert.assertTrue(jsonObject.getJSONArray("error").toList().stream().map(Object::toString).
                                collect(Collectors.toList()).stream().findFirst().get().equals("A Style already exist with same name. Can not save current style.")
                        , "There is some error while creating template style");

                // Verify Result on Template Style Listing
                int templateId = PreSignatureHelper.isTemplateAddedToListing(templateName);
                customAssert.assertTrue(templateId > 0, "New template is not created");

                // Navigate to the Created Template Show Page
                httpResponse = template.showTemplate(templateId);
                String showTemplateResponseStr = EntityUtils.toString(httpResponse.getEntity());
                jsonObject = new JSONObject(showTemplateResponseStr);
                customAssert.assertTrue(Integer.parseInt(jsonObject.get("httpStatusCode").toString()) == 200, "Response Code is not Valid");

                // Edit Created Template
                url = "/wordStyle/update";
                String newTemplateName = templateName + RandomNumbers.getRandomNumberWithinRange(0, 1000);
                jsonObject.getJSONObject("wordFormatStructure").getJSONObject("templateStyle").put("templateName", newTemplateName);
                template.createOrPreviewOrEditStyleTemplate(url, jsonObject.getJSONObject("wordFormatStructure").getJSONObject("templateStyle").toString());

                // Verify Whether template is Updated
                httpResponse = template.showTemplate(templateId);
                showTemplateResponseStr = EntityUtils.toString(httpResponse.getEntity());
                jsonObject = new JSONObject(showTemplateResponseStr);
                customAssert.assertTrue(Integer.parseInt(jsonObject.get("httpStatusCode").toString()) == 200, "Response Code is not Valid");
                customAssert.assertTrue(jsonObject.getJSONObject("wordFormatStructure").getJSONObject("templateStyle").get("templateName").toString().equals(newTemplateName),
                        "Template is not getting updated");
            }
        } catch (Exception e) {
            logger.error("Error while creating the template " + e.getMessage());
        } finally {
            HttpResponse checkResponse = check.hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"),
                    ConfigureEnvironment.getEnvironmentProperty("password"));
            customAssert.assertTrue(checkResponse.getStatusLine().getStatusCode() == 200, "End User Login Check API Response is not Valid");
        }
        customAssert.assertAll();
    }

    @Test(dataProvider = "clauseTestData", priority = 2)
    public void testClauseCreateAPI(String htmlText, String text) throws IOException {
        SoftAssert softAssert = new SoftAssert();
        check.hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
        String rawPayload = "{\n" + "\"htmlText\" : \"%s\",\n" + "\"text\" : \"%s\"\n" + "}\n";
        String payload = String.format(rawPayload, htmlText, text);
        logger.debug("Query string url formed is {}", htmlText + text);

        HttpPost postRequest;
        String postQueryString = "/createClause";
        postRequest = new HttpPost(postQueryString);
        postRequest.addHeader("Content-Type", "application/json");
        postRequest.addHeader("User-Agent", "web");
        HttpResponse response = APIUtils.postRequest(postRequest, payload);
        softAssert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK);

        String responseJson = EntityUtils.toString(response.getEntity());
        JSONObject jsonObj = new JSONObject(responseJson);
        String Id = jsonObj.get("response").toString();
        String getQueryString = "/clause/newClause/%d?";
        String getRequestURI = String.format(getQueryString, Integer.parseInt(Id));

        HttpGet getReqeust = new HttpGet(getRequestURI);
        response = APIUtils.getRequest(getReqeust);
        responseJson = EntityUtils.toString(response.getEntity());
        jsonObj = new JSONObject(responseJson);

        softAssert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK);
        softAssert.assertEquals(jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("clauseText").getJSONObject("values").get("text").toString(),
                text, "actual value" + jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("clauseText")
                        .getJSONObject("values").get("text").toString());
        softAssert.assertEquals(jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("clauseText").getJSONObject("values").get("htmlText").toString(),
                htmlText, "actual value" + jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("clauseText")
                        .getJSONObject("values").get("htmlText").toString());
        softAssert.assertAll();
    }

    @Test(priority = 3)
    public void testClauseCreateWithNegativeValidation() throws IOException {
        SoftAssert softAssert = new SoftAssert();
        check.hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
        String textData = RandomString.getRandomAlphaNumericString(200) + Runtime.getRuntime().toString();
        String rawPayload = "{\n" + "\"htmlText\" : \"%s\",\n" + "\"text\" : \"%s\"\n" + "}\n";
        String payload = String.format(rawPayload, textData, textData);
        String postQueryString = "/createClause";
        logger.debug("Query string url formed is {}", postQueryString);
        logger.debug("text data is {}", textData);
        JSONObject jsonObject = new JSONObject(payload);
        jsonObject.remove("text");

        HttpPost postRequest = getHttpPostRequestWithDefaultHeader(postQueryString);

        HttpResponse response = APIUtils.postRequest(postRequest, jsonObject.toString());
        String responseJson = EntityUtils.toString(response.getEntity());
        JSONObject jsonObj = new JSONObject(responseJson);
        softAssert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK);
        softAssert.assertEquals(jsonObj.get("responseCode").toString(), "5XX");

        jsonObject.remove("htmlText");
        postRequest = getHttpPostRequestWithDefaultHeader(postQueryString);
        response = APIUtils.postRequest(postRequest, jsonObject.toString());
        responseJson = EntityUtils.toString(response.getEntity());
        jsonObj = new JSONObject(responseJson);
        softAssert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK);
        softAssert.assertEquals(jsonObj.get("responseCode").toString(), "5XX");

        jsonObject = new JSONObject(payload);
        jsonObject.remove("htmlText");
        postRequest = getHttpPostRequestWithDefaultHeader(postQueryString);
        response = APIUtils.postRequest(postRequest, jsonObject.toString());
        responseJson = EntityUtils.toString(response.getEntity());
        jsonObj = new JSONObject(responseJson);
        softAssert.assertEquals(response.getStatusLine().getStatusCode(), HttpStatus.SC_OK);
        softAssert.assertEquals(jsonObj.get("responseCode").toString(), "5XX");
        softAssert.assertAll();
    }

    private HttpPost getHttpPostRequestWithDefaultHeader(String postQueryString) {
        HttpPost postRequest;
        postRequest = new HttpPost(postQueryString);
        postRequest.addHeader("Content-Type", "application/json");
        postRequest.addHeader("User-Agent", "web");
        return postRequest;
    }


    /*
     * Pre-Signature E2E test case covering clause creation, tag creation, clause activation, definition creation, definition activation,
     * contract template creation with created clause and definition, Contract draft request creation, attaching contract template to
     * contract draft request, verifying contract document tab with attached template
     * */

    @Test(priority = 4)
    public void VerifyE2EPreSignatureFlow() {

        logger.info("This test case starts with creating clause, definition , activating them, creating contract template" +
                "with newly created clause and template, creating contract draft request, attaching contract template to CDR");

        logger.info("E2E Pre-Signature Test Started");

        SoftAssert softAssert = new SoftAssert();
        /////// Clause Creation ////////
        PreSignatureHelper.setFieldsInConfigForEntities(configFilePath, configFileName, "clauses", "fields", 0);

        int clauseId = -1;
        int definitionId = -1;
        int contractTemplateId = -1;
        int cdrId = -1;
        int contractId = -1;

        try {
            // Create new Clause with tags
            String createdClauseId = Clause.createClause(null, null, configFilePath, configFileName, "fields", false);
            clauseId = PreSignatureHelper.getNewlyCreatedId(createdClauseId);
            logger.info("Newly Created Clause Id " + clauseId);

            if (clauseId == -1) {
                throw new SkipException("Error in Creating Clause");
            }

            // Get text for which tag has to be created
            String text = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fields constant values", "text");
            String[] wordTags = text.split(" ");
            int word = RandomNumbers.getRandomNumberWithinRangeIndex(0, wordTags.length);
            String wordToTag = wordTags[word];

            //Creating the new tag
            String tagCreationPayload = "{\"name\":\"" + wordToTag + "\",\"tagHTMLType\":{\"id\":1}}";
            HttpResponse createCreationResponse = PreSignatureHelper.createTag(tagCreationPayload);
            softAssert.assertTrue(createCreationResponse.getStatusLine().getStatusCode() == 200, "Tag Creation API Response is not valid");
            JSONObject tagCreationResponseStr = PreSignatureHelper.getJsonObjectForResponse(createCreationResponse);
            int newlyCreatedOrExistingTagId = 0;
            if (tagCreationResponseStr.toString().contains("Tag created successfully")) {
                softAssert.assertTrue(tagCreationResponseStr.getJSONObject("data").get("message").equals("Tag created successfully"),
                        "Tag is not created successfully");
                newlyCreatedOrExistingTagId = Integer.parseInt(tagCreationResponseStr.getJSONObject("data").get("id").toString());
            } else {
                if (tagCreationResponseStr.toString().contains("A tag with the given name already exists")) {
                    softAssert.assertTrue(tagCreationResponseStr.getJSONObject("data").get("message")
                                    .equals("A tag with the given name already exists. Please use the existing tag or create a tag with a different name."),
                            "Tag is not already present");
                    HttpResponse getTagOptionsResponse = PreSignatureHelper.verifyWhetherTagIsAlreadyPresent(wordToTag);
                    softAssert.assertTrue(getTagOptionsResponse.getStatusLine().getStatusCode() == 200, "Tag Options API Response COde is not valid");
                    JSONObject tagOptionsResponseJson = PreSignatureHelper.getJsonObjectForResponse(getTagOptionsResponse);
                    int tagsLength = tagOptionsResponseJson.getJSONArray("data").length();
                    for (int i = 0; i < tagsLength; i++) {
                        if (tagOptionsResponseJson.getJSONArray("data").getJSONObject(i).get("name").toString().equals(wordToTag)) {
                            newlyCreatedOrExistingTagId = Integer.parseInt(tagOptionsResponseJson.getJSONArray("data").getJSONObject(i).get("id").toString());
                        }
                    }
                }
            }

            // Updating clause with tag
            String htmlText = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fields constant values", "html");
            String[] htmlWordBag = htmlText.split(" ");
            String htmlToTag = htmlWordBag[word];

            // Replacing html with tagged html
            String updateTag = "<span class= \"\" contenteditable=\"true\"><span class=\"tag_" + newlyCreatedOrExistingTagId +
                    " tag\" contenteditable=\"false\" htmltagtype=\"1\" htmltagtypeval=\"\"><span style=\"display:none\">${" + newlyCreatedOrExistingTagId +
                    ":</span>" + htmlToTag + "<span style=\"display:none\">}</span></span></span>";
            htmlWordBag[word] = updateTag;

            //Altering html text with newly created tag
            StringBuffer sb = new StringBuffer();
            for (String s : htmlWordBag) {
                sb.append(s + " ");
            }

            // Hitting Edit Get API to get Clause text and Clause tag to update
            HttpResponse getCreatedClauseResponse = PreSignatureHelper.getClause(clauseId);
            softAssert.assertTrue(getCreatedClauseResponse.getStatusLine().getStatusCode() == 200, "Error in fetching the new created clause");
            JSONObject createdClauseJson = PreSignatureHelper.getJsonObjectForResponse(getCreatedClauseResponse);
            // Get the json object needed to update to link newly created tag with clause
            // Updating clause tag
            createdClauseJson.getJSONObject("body").getJSONObject("data").getJSONObject("clauseTags").put("values", new JSONArray());
            JSONObject tagValues = new JSONObject();
            tagValues.put("id", newlyCreatedOrExistingTagId);
            tagValues.put("name", newlyCreatedOrExistingTagId);
            JSONObject tagHtmlType = new JSONObject();
            tagHtmlType.put("id", 1);
            tagHtmlType.put("name", "Text Field");
            tagValues.put("tagHTMLType", tagHtmlType);
            createdClauseJson.getJSONObject("body").getJSONObject("data").getJSONObject("clauseTags").getJSONArray("values").put(tagValues);
            // Updating Clause text
            createdClauseJson.getJSONObject("body").getJSONObject("data").getJSONObject("clauseText").getJSONObject("values").put("htmlText", sb.toString().trim());

            // Edit Clause with tags
            String editClausePayload = "{\"body\":{\"data\":" + createdClauseJson.getJSONObject("body").getJSONObject("data").toString() + "}}";
            HttpResponse editClauseAPIResponse = PreSignatureHelper.editClause(editClausePayload);
            softAssert.assertTrue(editClauseAPIResponse.getStatusLine().getStatusCode() == 200, "Error while editing clause");
            JSONObject editedClauseResponseJson = PreSignatureHelper.getJsonObjectForResponse(editClauseAPIResponse);
            softAssert.assertTrue(editedClauseResponseJson.getJSONObject("header").getJSONObject("response").get("status").toString().equals("success"),
                    "Tag is not updated with clause");

            // Activate Newly Created Clause
            // Send for client review Action
            PreSignatureHelper.activateEntity(clauseId, softAssert);
            // Approve Action
            PreSignatureHelper.activateEntity(clauseId, softAssert);
            // Publish Action
            PreSignatureHelper.activateEntity(clauseId, softAssert);

            /////////////// Definition Creation ////////////////////
            PreSignatureHelper.setFieldsInConfigForEntities(configFilePath, configFileName, "clauses", "definition fields", 1);
            String definitionResponseString = Definition.createDefinition(null, null, configFilePath, configFileName,
                    "definition fields", false);
            definitionId = PreSignatureHelper.getNewlyCreatedId(definitionResponseString);
            logger.info("Newly Created Definition Id " + definitionId);

            // Get Created definition
            HttpResponse getCreatedDefinitionResponse = PreSignatureHelper.getClause(definitionId);
            softAssert.assertTrue(getCreatedDefinitionResponse.getStatusLine().getStatusCode() == 200, "Error in fetching the new created definition");
            JSONObject createdDefinitionJson = PreSignatureHelper.getJsonObjectForResponse(getCreatedDefinitionResponse);

            // Activate Newly Created Definition
            // Send for client review Action
            PreSignatureHelper.activateEntity(definitionId, softAssert);
            // Approve Action
            PreSignatureHelper.activateEntity(definitionId, softAssert);
            // Publish Action
            PreSignatureHelper.activateEntity(definitionId, softAssert);

            ///////////// Contract Template Creation////////////////
            PreSignatureHelper.setFieldsInConfigForEntities(configFilePath, configFileName, "contract templates", "contract template fields", 0);

            // Setting values for clauses to be selected while creating contract template
            JSONObject clauseCategory = new JSONObject(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,
                    "fields", "category"));
            JSONObject definitionCategory = new JSONObject(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,
                    "definition fields", "definitionCategory"));
            int clauseFieldId = PreSignatureHelper.getFieldId("contract templates", "clauses");
            String selectClausePayload = "{\"name\": \"clauses\",\"id\": " + clauseFieldId + ",\"multiEntitySupport\": false,\"values\": [{\"clauseCategory\": " +
                    "{\"name\": \"" + clauseCategory.getJSONObject("values").get("name").toString() + "\",\"id\": \"" + Integer.valueOf(clauseCategory
                    .getJSONObject("values").get("id").toString()) + "\"},\"clause\": {\"name\": \"" + createdClauseJson.getJSONObject("body").getJSONObject("data")
                    .getJSONObject("name").get("values") + "\",\"id\": " + clauseId + "},\"clauseGroup\": {\"name\": \"Clause\",\"id\": 1}," +
                    "\"order\": 1,\"mandatory\": null},{\"clauseCategory\": {\"name\": \"" + definitionCategory.getJSONObject("values").get("name").toString() +
                    "\",\"id\": \"" + Integer.valueOf(definitionCategory.getJSONObject("values").get("id").toString()) + "\"},\"clause\": {\"name\": \"" +
                    createdDefinitionJson.getJSONObject("body").getJSONObject("data").getJSONObject("name").get("values") + "\",\"id\": " + definitionId +
                    "},\"clauseGroup\": {\"name\": \"Clause\",\"id\": 2},\"order\": 2,\"mandatory\": null}]}";

            ParseConfigFile.updateValueInConfigFile(configFilePath, configFileName, "contract template fields", "clauses", selectClausePayload);

            String contractTemplateResponseString = ContractTemplate.createContractTemplate(null, null, configFilePath, configFileName,
                    "contract template fields", false);
            contractTemplateId = PreSignatureHelper.getNewlyCreatedId(contractTemplateResponseString);
            // Get newly created contract template response
            HttpResponse contractTemplateResponse = PreSignatureHelper.getContractTemplateResponse(contractTemplateId);
            JSONObject contractTemplateJson = PreSignatureHelper.getJsonObjectForResponse(contractTemplateResponse);

            int templateTypeId = Integer.parseInt(contractTemplateJson.getJSONObject("body").getJSONObject("data")
                    .getJSONObject("templateType").getJSONObject("values").get("id").toString());
            String contractTemplateName = contractTemplateJson.getJSONObject("body").getJSONObject("data").getJSONObject("name").get("values").toString();
            String contractTemplateDocument = contractTemplateJson.getJSONObject("body").getJSONObject("data").getJSONObject("uploadDocument")
                    .getJSONObject("values").get("name").toString();

            // Create new Contract Draft Request
            String contractDraftRequestResponseString = ContractDraftRequest.createCDR(null, null, configFilePath, configFileName,
                    "contract draft request fields", false);
            cdrId = PreSignatureHelper.getNewlyCreatedId(contractDraftRequestResponseString);

            // Attaching Creating Template to Created Contract Draft Request
            // Get the Created CDR edit page response
            HttpResponse contractDraftRequestResponse = PreSignatureHelper.getContractDraftRequestEditPageResponse(cdrId);
            JSONObject contractDraftRequestJson = PreSignatureHelper.getJsonObjectForResponse(contractDraftRequestResponse);

            JSONArray attachTemplateArray = new JSONArray("[{\"id\":\"" + contractTemplateId + "\",\"name\":\"" + contractTemplateName +
                    "\",\"hasChildren\":\"false\",\"templateTypeId\":\"" + templateTypeId + "\",\"checked\":1,\"mappedContractTemplates\":null," +
                    "\"uniqueIdentifier\":\"186899071638312\",\"$$hashKey\":\"object:1366\",\"mappedTags\":{\"" + newlyCreatedOrExistingTagId +
                    "\":{\"name\":\"" + htmlToTag + "\",\"id\":" + newlyCreatedOrExistingTagId + ",\"identifier\":\"" + htmlToTag +
                    "\",\"tagHTMLType\":{\"name\":\"Text Field\",\"id\":1},\"orderSeq\":100,\"tagTypeId\":2,\"$$hashKey\":\"object:1371\"}}}]");

            contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("mappedContractTemplates").put("values", attachTemplateArray);

            String contractDraftRequestAttachTemplatePayload = "{\"body\":{\"data\":" + contractDraftRequestJson.getJSONObject("body").getJSONObject("data").toString() + "}}";

            // Edit contract draft request with template
            HttpResponse contractDraftRequestUpdateResponse = PreSignatureHelper.editContractDraftRequest(contractDraftRequestAttachTemplatePayload);
            JSONObject contractDraftRequestUpdateJson = PreSignatureHelper.getJsonObjectForResponse(contractDraftRequestUpdateResponse);
            String editStatus = ParseJsonResponse.getStatusFromResponse(contractDraftRequestUpdateJson.toString());
            softAssert.assertTrue(editStatus.equalsIgnoreCase("success"), "Updating the CDR with template is not successful. " + editStatus);

            // Verify Contract Template Section in Contract Draft Request Page
            HttpResponse getContractDraftRequestResponse = PreSignatureHelper.getContractDraftRequestResponse(cdrId);
            softAssert.assertTrue(getContractDraftRequestResponse.getStatusLine().getStatusCode() == 200,
                    "Error in fetching created Contract Draft Request Response");
            JSONObject getContractDraftRequestJson = PreSignatureHelper.getJsonObjectForResponse(getContractDraftRequestResponse);

            // Find Mapped Contract Template API to verify which contract template is linked with CDR
            HttpResponse findMappedCTAPIResponse = PreSignatureHelper.findMappedContractTemplate(cdrId);
            softAssert.assertTrue(findMappedCTAPIResponse.getStatusLine().getStatusCode() == 200, "Find Mapped CT API Response Code is not valid");
            JSONObject findMappedCTAPIJson = PreSignatureHelper.getJsonObjectForResponse(findMappedCTAPIResponse);
            softAssert.assertTrue(findMappedCTAPIJson.getJSONObject("data").getJSONArray("mappedContractTemplates")
                    .getJSONObject(0).get("name").toString().trim().equals(contractTemplateName.trim()), "Not able to see mapped contract template with CDR");
            softAssert.assertTrue(Integer.parseInt(findMappedCTAPIJson.getJSONObject("data")
                            .getJSONArray("mappedContractTemplates").getJSONObject(0).get("id").toString().trim()) == contractTemplateId,
                    "Not able to see mapped contract template with CDR");
            softAssert.assertTrue(findMappedCTAPIJson.getJSONObject("data").getJSONArray("mappedContractTemplates").getJSONObject(0)
                            .getJSONObject("mappedTags").getJSONObject(String.valueOf(newlyCreatedOrExistingTagId)).get("name").toString().trim().equals(wordToTag.trim()),
                    "Created Tag Name is not Linked to Contract Template Linked to CDR");
            softAssert.assertTrue(Integer.parseInt(findMappedCTAPIJson.getJSONObject("data").getJSONArray("mappedContractTemplates")
                    .getJSONObject(0).getJSONObject("mappedTags").getJSONObject(String.valueOf(newlyCreatedOrExistingTagId)).get("id").toString().trim())
                    == newlyCreatedOrExistingTagId, "Created Tag Id is not Linked to Contract Template Linked to CDR");

            // Edit Contract Template from CDR show page
            // Update Tag
            String updatedTagText = RandomString.getRandomAlphaNumericString(5);
            String updateTagPayload = "[{\"id\":" + newlyCreatedOrExistingTagId + ",\"name\":\"" + wordToTag + "\",\"defaultValue\":\"" + updatedTagText + "\"}]";

            HttpResponse updateTagResponse = PreSignatureHelper.updateTagValue(updateTagPayload);

            softAssert.assertTrue(updateTagResponse.getStatusLine().getStatusCode() == 200, "Updated Tag Response Code is not Valid");

            JSONObject updateTagJson = PreSignatureHelper.getJsonObjectForResponse(updateTagResponse);
            softAssert.assertTrue(updateTagJson.get("success").toString().equals("true"), "Tag Update is not successful");
            softAssert.assertTrue(updateTagJson.get("errors").toString().equals("null"), "Tag Update is not successful");
            // Link updated tag value to CDR
            getContractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("mappedContractTemplates").getJSONArray("values")
                    .getJSONObject(0).getJSONObject("mappedTags").getJSONObject(String.valueOf(newlyCreatedOrExistingTagId)).put("defaultValue", updatedTagText);
            String editCDRWithTagPayload = "{\"body\":{\"data\":" + getContractDraftRequestJson.getJSONObject("body").getJSONObject("data").toString() + "}}";

            HttpResponse editCDRWithTagResponse = PreSignatureHelper.editContractDraftRequest(editCDRWithTagPayload);
            softAssert.assertTrue(editCDRWithTagResponse.getStatusLine().getStatusCode() == 200, "CDR is not updated with tag value = " + updatedTagText);
            JSONObject editCDRWithTagJson = PreSignatureHelper.getJsonObjectForResponse(editCDRWithTagResponse);
            softAssert.assertTrue(editCDRWithTagJson.getJSONObject("header").getJSONObject("response").get("status").toString().trim().equals("success"),
                    "CDR is updated with new tag value");

            // Find Mapped Contract Template API
            findMappedCTAPIResponse = PreSignatureHelper.findMappedContractTemplate(cdrId);
            softAssert.assertTrue(findMappedCTAPIResponse.getStatusLine().getStatusCode() == 200, "Find Mapped CT API Response Code is not valid");
            findMappedCTAPIJson = PreSignatureHelper.getJsonObjectForResponse(findMappedCTAPIResponse);
            softAssert.assertTrue(findMappedCTAPIJson.getJSONObject("data").getJSONArray("mappedContractTemplates").getJSONObject(0)
                    .get("name").toString().trim().equals(contractTemplateName.trim()), "Not able to see mapped contract template with CDR");
            softAssert.assertTrue(Integer.parseInt(findMappedCTAPIJson.getJSONObject("data").getJSONArray("mappedContractTemplates")
                    .getJSONObject(0).get("id").toString().trim()) == contractTemplateId, "Not able to see mapped contract template with CDR");
            softAssert.assertTrue(findMappedCTAPIJson.getJSONObject("data").getJSONArray("mappedContractTemplates").getJSONObject(0)
                            .getJSONObject("mappedTags").getJSONObject(String.valueOf(newlyCreatedOrExistingTagId)).get("name").toString().trim().equals(wordToTag.trim()),
                    "Created Tag Name is not Linked to Contract Template Linked to CDR");
            softAssert.assertTrue(Integer.parseInt(findMappedCTAPIJson.getJSONObject("data").getJSONArray("mappedContractTemplates")
                    .getJSONObject(0).getJSONObject("mappedTags").getJSONObject(String.valueOf(newlyCreatedOrExistingTagId)).get("id").toString().trim())
                    == newlyCreatedOrExistingTagId, "Created Tag Id is not Linked to Contract Template Linked to CDR");
            softAssert.assertTrue(findMappedCTAPIJson.getJSONObject("data").getJSONArray("mappedContractTemplates").getJSONObject(0)
                    .getJSONObject("mappedTags").getJSONObject(String.valueOf(newlyCreatedOrExistingTagId)).get("defaultValue").toString().trim()
                    .equals(updatedTagText.trim()), "Not able to see mapped contract template with CDR");

            // Validate Contract Template in Contract Document tab of CDR
            HttpResponse defaultUserListMetaDataResponse = PreSignatureHelper.defaultUserListMetaDataAPI("367", 160, "{}");
            softAssert.assertTrue(defaultUserListMetaDataResponse.getStatusLine().getStatusCode() == 200,
                    "Default User List Meta Data API Response Code is not valid");
            JSONObject defaultUserListMetaDataJson = PreSignatureHelper.getJsonObjectForResponse(defaultUserListMetaDataResponse);
            List<Integer> columnIds = PreSignatureHelper.getDefaultColumns(defaultUserListMetaDataJson.getJSONArray("columns"));

            String tabListDataPayload = "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"asc\",\"filterJson\":{}}}";
            HttpResponse tabListDataResponse = PreSignatureHelper.tabListDataAPI("367", "160", cdrId, tabListDataPayload);
            softAssert.assertTrue(tabListDataResponse.getStatusLine().getStatusCode() == 200, "Tab List Data API Response is not valid");
            JSONObject tabListDataJson = PreSignatureHelper.getJsonObjectForResponse(tabListDataResponse);

            String documentName = null;
            String documentStatus = null;
            String documentId = null;
            String contractDocumentTemplateTypeId = null;

            for (int columnId : columnIds) {
                if (tabListDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("columnName")
                        .toString().equals("documentname")) {
                    documentName = tabListDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("value").toString();
                } else if (tabListDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("columnName")
                        .toString().equals("documentstatus")) {
                    documentStatus = tabListDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("value").toString();
                } else if (tabListDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("columnName").toString().equals("id")) {
                    documentId = tabListDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("value").toString();
                } else if (tabListDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId)).get("columnName")
                        .toString().equals("template_type_id")) {
                    contractDocumentTemplateTypeId = tabListDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnId))
                            .get("value").toString();
                }
            }

            softAssert.assertTrue(documentName.split(":;")[1].trim().equals(contractTemplateName.trim()),
                    "Contract Template document is not getting reflected in CDR contract document tab");
            softAssert.assertTrue(documentStatus.split(":;")[0].trim().equals("1"),
                    "In CDR contract document tab, Contract Template document is not in draft status");

            // Edit Contract Template to final status from Contract Document tab in CDR
            contractDraftRequestResponse = PreSignatureHelper.getContractDraftRequestEditPageResponse(cdrId);
            contractDraftRequestJson = PreSignatureHelper.getJsonObjectForResponse(contractDraftRequestResponse);
            JSONArray contractDocumentStatusChangeJsonArray = new JSONArray("[{\"documentFileId\": \"" + documentStatus.split(":;")[1].trim() +
                    "\",\"editable\": true,\"shareWithSupplierFlag\": false,\"documentStatus\": {\"id\": \"2\",\"name\": \"\"}}]");
            contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("commentDocuments")
                    .put("values", contractDocumentStatusChangeJsonArray);
            String contractDraftRequestContractDocumentStatusChangePayload = "{\"body\":{\"data\":" + contractDraftRequestJson.getJSONObject("body")
                    .getJSONObject("data").toString() + "}}";

            HttpResponse contractDraftRequestContractDocumentStatusChangeResponse =
                    PreSignatureHelper.editContractDraftRequest(contractDraftRequestContractDocumentStatusChangePayload);
            softAssert.assertTrue(contractDraftRequestContractDocumentStatusChangeResponse.getStatusLine().getStatusCode() == 200,
                    "Response Code for Contract Document Status Change in CDR is not valid");

            // Download the Document from Contract Document tab
            Boolean isContractDocumentDownloaded = PreSignatureHelper.getTemplateFromContractDocumentTabCDR(System.getProperty("user.dir") +
                            "\\src\\test\\resources\\TestConfig\\PreSignature\\Files", contractTemplateDocument, documentId, "78", "160",
                    documentStatus.split(":;")[1].trim());
            softAssert.assertTrue(isContractDocumentDownloaded, "Contract Template from Contract Document Tab in CDR is not Downloaded");

            // Upload Document Draft and Change status to executed
            String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
            File fileToUpload = new File(System.getProperty("user.dir") + "\\src\\test\\resources\\TestConfig\\PreSignature\\Files" + "\\" +
                    contractTemplateDocument);
            String fileUploadDraftResponse = PreSignatureHelper.fileUploadDraft(contractTemplateDocument.split("\\.")[0],
                    contractTemplateDocument.split("\\.")[1], randomKeyForFileUpload, "160", String.valueOf(cdrId),
                    String.valueOf(documentId), fileToUpload);
            JSONObject fileUploadDraftJson = new JSONObject(fileUploadDraftResponse);

            softAssert.assertTrue(fileUploadDraftJson.get("documentFileId").toString().trim().equals(documentStatus.split(":;")[1].trim()),
                    "Document File id is not as expected");
            softAssert.assertTrue(fileUploadDraftJson.get("templateTypeId").toString().trim().equals(contractDocumentTemplateTypeId.trim()),
                    "Document template type id is not as expected");
            softAssert.assertTrue(fileUploadDraftJson.getJSONArray("documentStatus").getJSONObject(0).get("name").toString().equals("Final"),
                    "Document initial status is not final");
            softAssert.assertTrue(fileUploadDraftJson.getJSONArray("documentStatus").getJSONObject(0).get("id").toString().equals("2"),
                    "Document initial status is not final");
            softAssert.assertTrue(fileUploadDraftJson.getJSONArray("documentStatus").getJSONObject(1).get("name").toString().equals("Executed"),
                    "Document status is not Executed");
            softAssert.assertTrue(fileUploadDraftJson.getJSONArray("documentStatus").getJSONObject(1).get("id").toString().equals("3"),
                    "Document status is not Executed");
            // Submit Document Draft
            contractDraftRequestResponse = PreSignatureHelper.getContractDraftRequestEditPageResponse(cdrId);
            contractDraftRequestJson = PreSignatureHelper.getJsonObjectForResponse(contractDraftRequestResponse);
            JSONArray contractDocumentChangeStatusJsonArray = new JSONArray("[{\"templateTypeId\":" + contractDocumentTemplateTypeId + ",\"documentFileId\":" +
                    documentStatus.split(":;")[1].trim() + ",\"key\":\"" + randomKeyForFileUpload + "\"," +
                    "\"permissions\":{\"financial\":false,\"legal\":false,\"businessCase\":false},\"documentStatusId\":3,\"performanceData\":false," +
                    "\"searchable\":false,\"shareWithSupplierFlag\":false,\"documentId\":" + documentId + "}]");
            contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("commentDocuments").put("values",
                    contractDocumentChangeStatusJsonArray);
            String submitDraftPayload = "{\"body\":{\"data\":" + contractDraftRequestJson.getJSONObject("body").getJSONObject("data").toString() + "}}";
            HttpResponse submitDraftResponse = PreSignatureHelper.submitFileDraft(submitDraftPayload);
            softAssert.assertTrue(submitDraftResponse.getStatusLine().getStatusCode() == 200, "Submit draft API Response is not valid");

            String sourceEntityId = "{\"name\":\"sourceEntityId\",\"values\":" + cdrId + ",\"multiEntitySupport\":false}";
            ParseConfigFile.updateValueInConfigFileCaseSensitive(configFilePath, configFileName, "contract from cdr", "sourceEntityId",
                    sourceEntityId);

            // Contract Creation from CDR
            String contractCreateSection = "contract from cdr";
            ParseConfigFile.updateValueInConfigFile(contractCreationConfigFilePath, contractCreationConfigFileName, contractCreateSection,
                    "sourceid", String.valueOf(cdrId));

            Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(contractCreationConfigFilePath, contractCreationConfigFileName,
                    contractCreateSection);
            String[] parentSupplierIdsArr = flowProperties.get("supplierids").split(",");
            String payload = "{\"documentTypeId\":4,\"parentEntity\":{\"entityIds\":" + Arrays.toString(parentSupplierIdsArr) +
                    ",\"entityTypeId\":1},\"sourceEntity\":{\"entityIds\":[" + flowProperties.get("sourceid") + "],\"entityTypeId\":160}," +
                    "\"actualParentEntity\":{\"entityIds\":" + Arrays.toString(parentSupplierIdsArr) + ",\"entityTypeId\":1}}";

            String createContractResponse = createContractFromCDRResponse(payload, contractCreateSection);

            if (createContractResponse == null) {
                softAssert.assertFalse(true, "Contract Create API Response is null.");
            }

            contractId = PreSignatureHelper.getNewlyCreatedId(createContractResponse);
            JSONObject createContractJson = new JSONObject(createContractResponse);
            softAssert.assertTrue(createContractJson.getJSONObject("header").getJSONObject("response").get("status").toString().trim().equals("success"),
                    "Contract is not created from CDR successfully");

            // Created Contract Response to get Contract Name
            HttpResponse createdContractResponse = PreSignatureHelper.showCreatedContract(contractId);
            softAssert.assertTrue(createdContractResponse.getStatusLine().getStatusCode() == 200, "Created Contract Show API Response is not valid");
            JSONObject createdContractJson = PreSignatureHelper.getJsonObjectForResponse(createdContractResponse);
            String createdContractName = createdContractJson.getJSONObject("body").getJSONObject("data").getJSONObject("name").get("values").toString().trim();

            // Verify Related Document tab of CDR to verify contract has been created
            HttpResponse relatedDocumentMetaDataTabResponse = PreSignatureHelper.defaultUserListMetaDataAPI("377", 160, "{}");
            softAssert.assertTrue(relatedDocumentMetaDataTabResponse.getStatusLine().getStatusCode() == 200,
                    "Related Contracts Meta Data Tab List Data API Response is not valid");
            JSONObject relatedDocumentMetaDataTabJson = PreSignatureHelper.getJsonObjectForResponse(relatedDocumentMetaDataTabResponse);
            List<Integer> relatedContractsDefaultColumns = PreSignatureHelper.getDefaultColumns(relatedDocumentMetaDataTabJson.getJSONArray("columns"));

            String relatedContractTabPayload = "{\"filterMap\":{\"entityTypeId\":160,\"offset\":0,\"size\":20,\"orderByColumnName\":\"contract_id\"," +
                    "\"orderDirection\":\"asc\",\"filterJson\":{}}}";
            HttpResponse relatedDocumentTabResponse = PreSignatureHelper.tabListDataAPI("377", "160", cdrId, relatedContractTabPayload);
            softAssert.assertTrue(relatedDocumentTabResponse.getStatusLine().getStatusCode() == 200,
                    "Related Contracts Tab List API Response is not valid");
            JSONObject relatedDocumentTabJson = PreSignatureHelper.getJsonObjectForResponse(relatedDocumentTabResponse);
            softAssert.assertTrue(Integer.parseInt(relatedDocumentTabJson.get("filteredCount").toString()) == 1,
                    "Related Contracts is not reflecting in list data");

            HashMap<String, String> relatedContractData = new HashMap<>();
            for (int relatedContractsDefaultColumn : relatedContractsDefaultColumns) {
                relatedContractData.put(relatedDocumentTabJson.getJSONArray("data").getJSONObject(0)
                                .getJSONObject(String.valueOf(relatedContractsDefaultColumn)).get("columnName").toString(),
                        relatedDocumentTabJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(relatedContractsDefaultColumn))
                                .get("value").toString());
            }

            softAssert.assertTrue(relatedContractData.get("contract_id").split(":;")[1].trim().equals(String.valueOf(contractId)),
                    "Created Contract is not reflecting on related document tab in contract draft request");
            softAssert.assertTrue(relatedContractData.get("linkedentitytype").trim().equals("Contracts"),
                    "Created Contract is not reflecting on related document tab in contract draft request");

            // Get all task id before moving contract to tree
            List<Integer> allTaskIdsBeforeSubmittingRequestToMoveToTree = PreSignatureHelper.getAllTaskIds();
            // Removing all task ids before moving contract to tree
            UserTasksHelper.removeAllTasks();

            // Move to Tree from Created Contract
            String moveToTreePayload = "{\"baseEntityId\":" + contractId + ",\"baseEntityTypeId\":61,\"sourceEntityTypeId\":160,\"sourceEntityId\":" +
                    cdrId + ",\"entityTypeId\":61,\"entityId\":" + contractId + ",\"auditLogDocTreeFlowDocs\":[{\"auditLogDocFileId\":\"" +
                    documentName.split(":;")[documentName.split(":;").length - 1].trim() + "\"}],\"sourceTabId\":2,\"statusId\":1}";
            HttpResponse moveToTreeResponse = PreSignatureHelper.moveToTree(moveToTreePayload);
            softAssert.assertTrue(moveToTreeResponse.getStatusLine().getStatusCode() == 200, "Move To Tree Response is not Valid");
            JSONObject moveToTreeJson = PreSignatureHelper.getJsonObjectForResponse(moveToTreeResponse);
            softAssert.assertTrue(moveToTreeJson.toString().contains("Your request has been successfully submitted"), "Move to be tree request is not successful");

            // Get new task id
            int newTaskIdForMovingDocToContractTree = PreSignatureHelper.getNewTaskId(allTaskIdsBeforeSubmittingRequestToMoveToTree);
            // New task created for move to tree
            Map<String, String> docMovementToContractTreeJob;
            docMovementToContractTreeJob = UserTasksHelper.waitForScheduler(Long.parseLong("600000"), Long.parseLong("10000"), newTaskIdForMovingDocToContractTree);

            softAssert.assertTrue(docMovementToContractTreeJob.get("jobPassed").equals("true"), "Job for move to tree is not successful");
            softAssert.assertTrue(docMovementToContractTreeJob.get("errorMessage") == null, "Error while scheduler is executing move to tree job. " +
                    docMovementToContractTreeJob.get("errorMessage"));

            // Verify Document in Contract Document tab of contract after move to tree
            HttpResponse contractDocumentMetaDataTabResponse = PreSignatureHelper.defaultUserListMetaDataAPI("366", 61, "{}");
            softAssert.assertTrue(contractDocumentMetaDataTabResponse.getStatusLine().getStatusCode() == 200,
                    "Contract Document Meta Data Tab List Data API Response is not valid");
            JSONObject contractDocumentMetaDataTabJson = PreSignatureHelper.getJsonObjectForResponse(contractDocumentMetaDataTabResponse);
            List<Integer> contractDocumentTabDefaultColumns = PreSignatureHelper.getDefaultColumns(contractDocumentMetaDataTabJson.getJSONArray("columns"));

            String contractDocumentTabPayload = "{\"filterMap\":{\"entityTypeId\":61,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"asc\",\"filterJson\":{}}}";
            HttpResponse contractDocumentTabResponse = PreSignatureHelper.tabListDataAPI("366", "61", contractId, contractDocumentTabPayload);
            softAssert.assertTrue(relatedDocumentTabResponse.getStatusLine().getStatusCode() == 200,
                    "Related Contracts Tab List API Response is not valid");
            JSONObject contractDocumentTabJson = PreSignatureHelper.getJsonObjectForResponse(contractDocumentTabResponse);
            softAssert.assertTrue(Integer.parseInt(contractDocumentTabJson.get("filteredCount").toString()) == 1,
                    "Related Contracts is not reflecting in list data");

            HashMap<String, String> contractDocumentData = new HashMap<>();
            for (int contractDocumentTabDefaultColumn : contractDocumentTabDefaultColumns) {
                contractDocumentData.put(contractDocumentTabJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(contractDocumentTabDefaultColumn))
                        .get("columnName").toString(), contractDocumentTabJson.getJSONArray("data").getJSONObject(0)
                        .getJSONObject(String.valueOf(contractDocumentTabDefaultColumn)).get("value").toString());
            }
            softAssert.assertTrue(contractDocumentData.get("documentname").split(":;")[1].trim().equals(documentName.split(":;")[1].trim()),
                    "Created Contract is not reflecting on related document tab in contract draft request");

            HttpResponse documentOnTreeResponse = PreSignatureHelper.verifyDocumentOnTree(contractId, "{}");
            softAssert.assertTrue(documentOnTreeResponse.getStatusLine().getStatusCode() == 200, "Document On Tree API Response is not valid");
            JSONObject documentOnTreeJson = PreSignatureHelper.getJsonObjectForResponse(documentOnTreeResponse);

            boolean isDocumentOnTree = false;
            int documentOnTreeChildrenLength = documentOnTreeJson.getJSONObject("body").getJSONObject("data").getJSONArray("children").length();
            for (int i = 0; i < documentOnTreeChildrenLength; i++) {
                if (documentOnTreeJson.getJSONObject("body").getJSONObject("data").getJSONArray("children").getJSONObject(i).get("text").toString().trim()
                        .equals(createdContractName)) {
                    isDocumentOnTree = true;
                    softAssert.assertTrue(Integer.parseInt(documentOnTreeJson.getJSONObject("body").getJSONObject("data").getJSONArray("children")
                            .getJSONObject(i).get("numberOfChild").toString().trim()) == 1, "Document is not reflecting into tree");
                    break;
                }
            }
            softAssert.assertTrue(isDocumentOnTree, "Document should be in tree but it doesn't exist");
            logger.info("E2E Pre-Signature Test Completed");
        } catch (Exception e) {
            softAssert.assertFalse(true, "Exception in Pre Sig E2E. " + e.getMessage());
        } finally {
            //Delete all new created data.
            if (clauseId != -1) {
                EntityOperationsHelper.deleteEntityRecord("clauses", clauseId);
            }

            if (definitionId != -1) {
                EntityOperationsHelper.deleteEntityRecord("definition", definitionId);
            }

            if (contractTemplateId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contract templates", contractTemplateId);
            }

            if (cdrId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contract draft request", cdrId);
            }

            if (contractId != -1) {
                EntityOperationsHelper.deleteEntityRecord("contracts", contractId);
            }
        }

        softAssert.assertAll();
    }

    private String createContractFromCDRResponse(String newPayload, String contractCreateSection) {
        logger.info("Hitting New V1 API for Contracts");
        New newObj = new New();
        newObj.hitNewV1ForMultiSupplier("contracts", newPayload);
        String newResponse = newObj.getNewJsonStr();

        if (newResponse != null) {
            if (ParseJsonResponse.validJsonResponse(newResponse)) {
                CreateEntity createEntityHelperObj = new CreateEntity(configFilePath, configFileName, configFilePath, configFileName,
                        contractCreateSection);

                Map<String, String> extraFields = createEntityHelperObj.setExtraRequiredFields("contracts");
                newObj.setAllRequiredFields(newResponse);
                Map<String, String> allRequiredFields = newObj.getAllRequiredFields();
                allRequiredFields = createEntityHelperObj.processAllChildFields(allRequiredFields, newResponse);
                allRequiredFields = createEntityHelperObj.processNonChildFields(allRequiredFields, newResponse);

                String createPayload = PayloadUtils.getPayloadForCreate(newResponse, allRequiredFields, extraFields, null, configFilePath,
                        configFileName);

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

    @Test(priority = 5)
    public void autoFillSupplierOnChild() {
        // Create new Contract Draft Request
        SoftAssert softAssert = new SoftAssert();
        int entityTypeId = 160;
        String contractDraftRequestResponseString = ContractDraftRequest.createCDR(null, null, configFilePath, configFileName,
                "contract draft request fields for supplier inheritance", false);
        int contractDraftRequestId = PreSignatureHelper.getNewlyCreatedId(contractDraftRequestResponseString);
        int supplierId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(contractCreationConfigFilePath, contractCreationConfigFileName,
                "contract draft request fields for supplier inheritance", "sourceid"));
        // Contract Creation from CDR
        String query = "/contracts/v1/new?version=2.0";
        String payload = "{\n" +
                "\t\"documentTypeId\": 4,\n" +
                "\t\"parentEntity\": {\n" +
                "\t\t\"entityIds\": [" + supplierId + "],\n" +
                "\t\t\"entityTypeId\": 1\n" +
                "\t},\n" +
                "\t\"sourceEntity\": {\n" +
                "\t\t\"entityIds\": [" + contractDraftRequestId + "],\n" +
                "\t\t\"entityTypeId\": " + entityTypeId + "\n" +
                "\t},\n" +
                "\t\"actualParentEntity\": {\n" +
                "\t\t\"entityIds\": [" + supplierId + "],\n" +
                "\t\t\"entityTypeId\": 1\n" +
                "\t}\n" +
                "}";

        HttpResponse contractCreationFromCDRShowPageResponse = PreSignatureHelper.apiContractFromCDR(query, payload);
        softAssert.assertTrue(contractCreationFromCDRShowPageResponse.getStatusLine().getStatusCode() == 200,
                "Contract Creation Show Page From CDR Response Code is not valid");
        JSONObject documentOnTreeJson = PreSignatureHelper.getJsonObjectForResponse(contractCreationFromCDRShowPageResponse);
        softAssert.assertTrue(documentOnTreeJson.getJSONObject("body").getJSONObject("data").getJSONObject("parentShortCodeId").getJSONObject("values")
                .get("id").toString().equals(String.valueOf(supplierId)), "Parent Source Code Id is not valid");
        softAssert.assertTrue(documentOnTreeJson.getJSONObject("body").getJSONObject("data").getJSONObject("parentEntityId").get("values").toString()
                .equals(String.valueOf(supplierId)), "Parent Source Code Id is not valid");
        softAssert.assertTrue(documentOnTreeJson.getJSONObject("body").getJSONObject("data").getJSONObject("relations").getJSONArray("values")
                .getJSONObject(0).get("id").toString().equals(String.valueOf(supplierId)), "Supplier Id is not valid");
        softAssert.assertTrue(documentOnTreeJson.getJSONObject("body").getJSONObject("data").getJSONObject("relations").getJSONArray("values")
                .getJSONObject(0).get("url").toString().trim().equals("/tblrelations/show/" + supplierId), "Supplier Show Page Url is not valid");
        softAssert.assertTrue(documentOnTreeJson.getJSONObject("body").getJSONObject("data").getJSONObject("actualParentEntityId").get("values")
                .toString().trim().equals(String.valueOf(supplierId)), "Parent Entity Id is not valid");
        softAssert.assertAll();
    }

    @Test(priority = 6)
    public void autoFillMultipleSuppliersInheritanceOnChild() {
        // Create new Contract Draft Request
        SoftAssert softAssert = new SoftAssert();
        String contractDraftRequestResponseString = ContractDraftRequest.createCDR(null, null, configFilePath, configFileName,
                "contract draft request fields for multiple supplier inheritance", false);
        int contractDraftRequestId = PreSignatureHelper.getNewlyCreatedId(contractDraftRequestResponseString);

        JSONObject suppliersJson = new JSONObject(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "contract draft request fields for multiple supplier inheritance", "suppliers"));
        JSONArray expectedSuppliers = suppliersJson.getJSONArray("values");
        // Contract Draft Request Show Page
        HttpResponse cdrShowPageResponse = PreSignatureHelper.getContractDraftRequestResponse(contractDraftRequestId);
        softAssert.assertTrue(cdrShowPageResponse.getStatusLine().getStatusCode() == 200, "CDR Show Page Response Code is not Valid");
        JSONObject cdrShowPageJson = PreSignatureHelper.getJsonObjectForResponse(cdrShowPageResponse);
        JSONArray actualSuppliers = cdrShowPageJson.getJSONObject("body").getJSONObject("data").getJSONObject("suppliers").getJSONArray("values");

        for (int i = 0; i < actualSuppliers.length(); i++) {
            actualSuppliers.getJSONObject(i).remove("additionalOption");
        }

        softAssert.assertTrue(expectedSuppliers.toList().stream().map(Object::toString).sorted().collect(Collectors.toList())
                        .equals(actualSuppliers.toList().stream().map(Object::toString).sorted().collect(Collectors.toList())),
                "Selected Suppliers while creating CDR are not present as options in CDR page to select from");
        softAssert.assertAll();
    }

    @Test(enabled = false)
    public void verifyContractDocumentClassification() {
        SoftAssert softAssert = new SoftAssert();
        Document masterRoleGroupHtml = null;

        // Login with client admin
        Check check = new Check();
        HttpResponse checkResponse = check.hitCheck(ConfigureEnvironment.getEnvironmentProperty("clientUsername"),
                ConfigureEnvironment.getEnvironmentProperty("clientUserPassword"));
        softAssert.assertTrue(checkResponse.getStatusLine().getStatusCode() == 302, "Check API Response Code is not Valid");

        String masterRoleGroupId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "contract document classification",
                "masterrolegroupid");

        // Execute master role group get api to verify all classification access are given to user
        HttpResponse masterRoleGroupResponse = PreSignatureHelper.getMasterUserRoleGroup(masterRoleGroupId);
        softAssert.assertTrue(masterRoleGroupResponse.getStatusLine().getStatusCode() == 200, "Master Role Group Response Code is not valid");
        try {
            masterRoleGroupHtml = Jsoup.parse(EntityUtils.toString(masterRoleGroupResponse.getEntity()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Validate all classification access
        softAssert.assertTrue(masterRoleGroupHtml.select("#_financialAccess_id").attr("checked").trim().equals("checked"),
                "Financial Access is provided to Contract Document");
        softAssert.assertTrue(masterRoleGroupHtml.select("#_businessAccess_id").attr("checked").trim().equals("checked"),
                "Business Access is provided to Contract Document");
        softAssert.assertTrue(masterRoleGroupHtml.select("#_legalAccess_id").attr("checked").trim().equals("checked"),
                "Legal Access is provided to Contract Document");

        // Login with end user
        checkResponse = check.hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
        softAssert.assertTrue(checkResponse.getStatusLine().getStatusCode() == 302, "Check Response is not valid");

        // Navigate to random CDR by choosing any from CDR listing
        HttpResponse defaultUserListMetaDataResponse = PreSignatureHelper.defaultUserListMetaDataAPI("279", "{}");
        softAssert.assertTrue(defaultUserListMetaDataResponse.getStatusLine().getStatusCode() == 200,
                "Default User List Meta Data API Response Code is not valid");
        JSONObject defaultUserListMetaDataJson = PreSignatureHelper.getJsonObjectForResponse(defaultUserListMetaDataResponse);

        // Get CDR default id column index
        int cdrDefaultColumnId = 0;
        int noOfColumns = defaultUserListMetaDataJson.getJSONArray("columns").length();
        for (int i = 0; i < noOfColumns; i++) {
            if (defaultUserListMetaDataJson.getJSONArray("columns").getJSONObject(i).get("defaultName").toString().trim().equals("ID")) {
                cdrDefaultColumnId = Integer.parseInt(defaultUserListMetaDataJson.getJSONArray("columns").getJSONObject(i).get("id").toString());
            }
        }

        HttpResponse cdrListDataResponse = PreSignatureHelper.getListDataForEntities("279", "{\"filterMap\":{}}");
        softAssert.assertTrue(cdrListDataResponse.getStatusLine().getStatusCode() == 200, "CDR List Data Response Code is not valid");
        JSONObject cdrListDataJson = PreSignatureHelper.getJsonObjectForResponse(cdrListDataResponse);

        String cdrId = cdrListDataJson.getJSONArray("data").getJSONObject(RandomNumbers.getRandomNumberWithinRangeIndex(0,
                cdrListDataJson.getJSONArray("data").length())).getJSONObject(String.valueOf(cdrDefaultColumnId)).get("value").toString().split(":;")[1].trim();

        HttpResponse contractDraftRequestResponse = PreSignatureHelper.getContractDraftRequestEditPageResponse(Integer.parseInt(cdrId));
        softAssert.assertTrue(contractDraftRequestResponse.getStatusLine().getStatusCode() == 200, "Contract Draft Request Edit Response is not valid");
        JSONObject contractDraftRequestJson = PreSignatureHelper.getJsonObjectForResponse(contractDraftRequestResponse);

        String contractDocumentPayload = "{\"body\":{\"data\":" + contractDraftRequestJson.getJSONObject("body").getJSONObject("data").toString() + "}}";
        // Upload Document Draft and Change status to executed
        String randomKeyForFileUpload = RandomString.getRandomAlphaNumericString(12);
        File fileToUpload = new File(System.getProperty("user.dir") +
                "\\src\\test\\resources\\TestConfig\\PreSignature\\Files\\UploadFiles\\ContractDocumentUpload.docx");
        PreSignatureHelper.fileUploadDraftWithNewDocument("ContractDocumentUpload", "docx",
                randomKeyForFileUpload, "160", cdrId, fileToUpload);

        JSONArray contractDocumentJson = new JSONArray("[{\"templateTypeId\":1001,\"documentFileId\":null,\"key\":\"" + randomKeyForFileUpload +
                "\",\"documentStatusId\":1,\"permissions\":{\"financial\":false,\"legal\":false,\"businessCase\":false}," +
                "\"performanceData\":false,\"searchable\":false,\"shareWithSupplierFlag\":false,\"legal\":true,\"financial\":true,\"businessCase\":true}]");
        contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("commentDocuments").put("values", contractDocumentJson);

        // Submit Document Draft
        HttpResponse submitDraftResponse = PreSignatureHelper.submitFileDraft(contractDocumentPayload);
        softAssert.assertTrue(submitDraftResponse.getStatusLine().getStatusCode() == 200, "Submit draft API Response is not valid");

        softAssert.assertAll();
    }

    // Clause Related Test Cases
    @Test(enabled = false)
    public void TestNewlyCreatedClauseOnListing() {
        SoftAssert softAssert = new SoftAssert();
        try {
            /////// Clause Creation ////////
            PreSignatureHelper.setFieldsInConfigForEntities(configFilePath, configFileName, "clauses", "fields", 0);

            // Create new Clause with tags
            String createdClauseId = Clause.createClause(null, null, configFilePath, configFileName, "fields", false);
            int clauseId = PreSignatureHelper.getNewlyCreatedId(createdClauseId);
            logger.info("Newly Created Clause Id " + clauseId);

            // Default User List Meta Data for extracting filtered columns
            HttpResponse defaultUserListMetaDataResponse = PreSignatureHelper.defaultUserListMetaDataAPI("271", "{}");
            softAssert.assertTrue(defaultUserListMetaDataResponse.getStatusLine().getStatusCode() == 200,
                    "Default User List Meta Data API Response Code is not valid");
            JSONObject defaultUserListMetaDataJson = PreSignatureHelper.getJsonObjectForResponse(defaultUserListMetaDataResponse);
            List<Integer> filterMetaDataColumnIds = PreSignatureHelper.getDefaultColumns(defaultUserListMetaDataJson.getJSONArray("filterMetadatas"));
            List<Integer> listDataColumnIds = PreSignatureHelper.getDefaultColumns(defaultUserListMetaDataJson.getJSONArray("columns"));

            // Filter API For Clause
            String filterDataPayload = "{}";
            HttpResponse filterDataResponse = PreSignatureHelper.filterData("271", filterDataPayload);
            softAssert.assertTrue(filterDataResponse.getStatusLine().getStatusCode() == 200, "Filter Data API Response Code is not valid");
            JSONObject filterDataJson = PreSignatureHelper.getJsonObjectForResponse(filterDataResponse);

            JSONObject listViewFilterJsonPayload = new JSONObject("{\"filterMap\":{\"entityTypeId\":138,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"desc nulls last\"}}");

            // Create Payload for list View Listing
            for (int filterMetaDataColumnId : filterMetaDataColumnIds) {
                if (filterDataJson.getJSONObject(String.valueOf(filterMetaDataColumnId)).get("filterName").toString().equals("status")) {
                    listViewFilterJsonPayload.getJSONObject("filterMap").put("filterJson", new JSONObject(filterMetaDataColumnId));
                    listViewFilterJsonPayload.getJSONObject("filterMap").getJSONObject("filterJson").put(String.valueOf(filterMetaDataColumnId),
                            filterDataJson.getJSONObject(String.valueOf(filterMetaDataColumnId)));
                    break;
                }
            }

            // Verify Clause on List View listing
            HttpResponse tabListDataResponse = PreSignatureHelper.tabListDataAPIForClause("271", listViewFilterJsonPayload.toString(), "List");
            softAssert.assertTrue(tabListDataResponse.getStatusLine().getStatusCode() == 200, "Tab List Data API Response is not valid");
            JSONObject tabListDataJson = PreSignatureHelper.getJsonObjectForResponse(tabListDataResponse);

            HashMap<String, String> newlyAddedClause = new LinkedHashMap<>();
            for (int listDataColumnId : listDataColumnIds) {
                newlyAddedClause.put(tabListDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(listDataColumnId))
                        .get("columnName").toString(), tabListDataJson.getJSONArray("data").getJSONObject(0)
                        .getJSONObject(String.valueOf(listDataColumnId)).get("value").toString());
            }

            softAssert.assertTrue(newlyAddedClause.get("id").split(":;")[1].trim().equals(String.valueOf(clauseId).trim()),
                    "Newly Created Clause is not reflecting in Clause Listing");

            for (int filterMetaDataColumnId : filterMetaDataColumnIds) {
                if (filterDataJson.getJSONObject(String.valueOf(filterMetaDataColumnId)).get("filterName").toString().equals("stakeholder")) {
                    filterDataJson.remove(String.valueOf(filterMetaDataColumnId));
                    break;
                }
            }
            JSONObject gridViewFilterJsonPayload = new JSONObject("{\"filterMap\":{\"offset\":0,\"size\":10,\"orderByColumnName\":null," +
                    "\"orderDirection\":null,\"entityTypeId\":138,\"hasDefinitionCategoryIds\":true,\"isApprovedClauses\":null,\"currentClauseId\":null," +
                    "\"selectedClauses\":[],\"searchName\":\"\",\"agreement_type_id\":[],\"startsWith\":\"\",\"onlyMandatory\":null}}");
            gridViewFilterJsonPayload.getJSONObject("filterMap").put("filterJson", new JSONObject(filterDataJson.toString()));

            // Verify Clause on Grid View listing
            HttpResponse tabListDataGridViewResponse = PreSignatureHelper.tabListDataAPIForClause("271", gridViewFilterJsonPayload.toString(), "Grid");
            softAssert.assertTrue(tabListDataGridViewResponse.getStatusLine().getStatusCode() == 200, "Tab List Data API Response is not valid");

            System.out.println("hvmhv");
        } catch (Exception e) {
            logger.info("Exception while verifying clause listing " + e.getMessage());
        }
        softAssert.assertAll();
    }

    @Test(priority = 7)
    public void TestDocumentMovementStatusClientAdmin() {
        CustomAssert customAssert = new CustomAssert();
        try {
            HttpResponse httpResponse = check.hitCheck(ConfigureEnvironment.getEnvironmentProperty("clientUsername"),
                    ConfigureEnvironment.getEnvironmentProperty("clientUserPassword"));
            customAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 302, "Response Code is not Valid");

            // Admin Show Page Uri
            String Uri = "/admin";
            httpResponse = template.verifyTemplateFormattingClientAdmin(Uri);
            customAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200, "Client Admin home page Response code is not valid");

            String clientAdminShowPageResponseStr = null;
            try {
                clientAdminShowPageResponseStr = EntityUtils.toString(httpResponse.getEntity());
            } catch (IOException e) {
                e.printStackTrace();
            }

            Document document = Jsoup.parse(clientAdminShowPageResponseStr);

            Elements allHeadings = document.select("#mainContainer h3");

            boolean isEntityConfigurationPresent = false;
            boolean isDocumentMovementStatusLinkPresent = false;

            for (Element e : allHeadings) {
                if (e.text().trim().equals("Entity Configuration")) {
                    isEntityConfigurationPresent = true;
                    List<String> links = e.parent().select("li a").stream().map(m -> m.text().trim()).collect(Collectors.toList());
                    for (String link : links) {
                        if (link.equals("Document Movement Status")) {
                            isDocumentMovementStatusLinkPresent = true;
                            break;
                        }
                    }
                }
            }

            customAssert.assertTrue(isEntityConfigurationPresent, "Entity Configuration Tab is not present");
            customAssert.assertTrue(isDocumentMovementStatusLinkPresent, "Document Movement Status is not present in Entity Configuration Tab");

            httpResponse = PreSignatureHelper.getDocumentMovementStatus();
            customAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200, "Document Movement Status List API Response code is not valid");

            String documentMovementStatusListResponseStr = EntityUtils.toString(httpResponse.getEntity());
            document = Jsoup.parse(documentMovementStatusListResponseStr);
            int documentMovementStatusSize = document.select("#l_com_sirionlabs_model_MasterGroup tbody tr").size();
            customAssert.assertTrue(documentMovementStatusSize == 3, "There are more than three Status which is not expected");

            List<String> statusIds = new LinkedList<>();
            List<String> organizationalPreferencesActualValues = new LinkedList<>();
            for (int i = 1; i <= documentMovementStatusSize; i++) {
                statusIds.add(document.select("#l_com_sirionlabs_model_MasterGroup tbody tr:nth-of-type(" + i + ") td a").text());
            }

            for (String statusId : statusIds) {
                httpResponse = PreSignatureHelper.getDocumentMovementStatusByDocumentId(statusId);
                customAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200,
                        "Document Movement Status List API Response code is not valid");

                String documentMovementStatusByStatusIdResponse = EntityUtils.toString(httpResponse.getEntity());
                document = Jsoup.parse(documentMovementStatusByStatusIdResponse);
                organizationalPreferencesActualValues.add(document.select("#_c_com_sirionlabs_model_documentmovementstatus_clientpref_name_id").text());
            }

            HashMap<String, String> formData = new HashMap<>();

            List<String> colorCode = new LinkedList<>();
            List<String> actualUpdatedOrganizationPreferencesValues = new LinkedList<>();
            List<String> expectedUpdatedOrganizationPreferencesValues = new LinkedList<>();
            colorCode.add("#00a650");
            colorCode.add("#fddb00");
            colorCode.add("#ed1b24");
            for (int i = 0; i < statusIds.size(); i++) {
                String updatedStatus = RandomString.getRandomAlphaNumericString(10);
                actualUpdatedOrganizationPreferencesValues.add(updatedStatus);
                formData.put("id", statusIds.get(i));
                formData.put("systemEntity.name", organizationalPreferencesActualValues.get(i));
                formData.put("name", updatedStatus);
                formData.put("colorCode", colorCode.get(i));
                formData.put("_csrf_token", "05b14959-3bd6-42b2-bbcb-13d33abf84ee");
                formData.put("ajax", "true");
                formData.put("history", "{\"12300\":\"" + updatedStatus + "\",\"12302\":\"" + colorCode.get(i) + "\"}");

                httpResponse = PreSignatureHelper.updateDocumentMovementStatus(formData);
                customAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 302, "Update Document Movement Status Response Code is not valid");
            }

            for (String statusId : statusIds) {
                httpResponse = PreSignatureHelper.getDocumentMovementStatusByDocumentId(statusId);
                customAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200,
                        "Document Movement Status List API Response code is not valid");

                String documentMovementStatusByStatusIdResponse = EntityUtils.toString(httpResponse.getEntity());
                document = Jsoup.parse(documentMovementStatusByStatusIdResponse);
                expectedUpdatedOrganizationPreferencesValues.add(document.select("#_c_com_sirionlabs_model_documentmovementstatus_clientpref_name_id").text());
            }

            customAssert.assertTrue(actualUpdatedOrganizationPreferencesValues.stream().sorted().collect(Collectors.toList())
                            .equals(expectedUpdatedOrganizationPreferencesValues.stream().sorted().collect(Collectors.toList())),
                    "Actual and Expected Document Movement Status Values are not Same");
            customAssert.assertTrue(!organizationalPreferencesActualValues.stream().sorted().collect(Collectors.toList())
                            .equals(expectedUpdatedOrganizationPreferencesValues.stream().sorted().collect(Collectors.toList())),
                    "Original and Expected Document Movement Status Values are same which is not expected");

            List<String> resetToOriginalDocumentMovementStatusValue = new LinkedList<>();
            for (int i = 0; i < statusIds.size(); i++) {
                resetToOriginalDocumentMovementStatusValue.add(organizationalPreferencesActualValues.get(i));
                formData.put("id", statusIds.get(i));
                formData.put("systemEntity.name", organizationalPreferencesActualValues.get(i));
                formData.put("name", organizationalPreferencesActualValues.get(i));
                formData.put("colorCode", colorCode.get(i));
                formData.put("_csrf_token", "05b14959-3bd6-42b2-bbcb-13d33abf84ee");
                formData.put("ajax", "true");
                formData.put("history", "{\"12300\":\"" + organizationalPreferencesActualValues.get(i) + "\",\"12302\":\"" + colorCode.get(i) + "\"}");

                httpResponse = PreSignatureHelper.updateDocumentMovementStatus(formData);
                customAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 302, "Update Document Movement Status Response Code is not valid");
            }
            customAssert.assertTrue(organizationalPreferencesActualValues.stream().sorted().collect(Collectors.toList())
                            .equals(resetToOriginalDocumentMovementStatusValue.stream().sorted().collect(Collectors.toList())),
                    "Document Movement Status is not back to Original");
        } catch (Exception e) {
            logger.info("Exception while verifying document movement status in client admin under entity configuration tab " + e.getMessage());
        } finally {
            HttpResponse checkResponse = check.hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"),
                    ConfigureEnvironment.getEnvironmentProperty("password"));
            customAssert.assertTrue(checkResponse.getStatusLine().getStatusCode() == 302, "Response Code is not Valid");
        }
        customAssert.assertAll();
    }

    @Test(priority = 8)
    public void TestActivityReportPermissionFromClientSetupToEndUser() {
        CustomAssert customAssert = new CustomAssert();

        try {
            String reportName = "Contract Draft Request Activity Report";
            String entity = "Contract Draft Request";
            String entityName = "contract draft request";
            int clientId = 1002;
            String masterUserRoleGroupId = "1001";

            enableDisableReport(customAssert, reportName, entity, entityName, clientId, masterUserRoleGroupId, false);
            enableDisableReport(customAssert, reportName, entity, entityName, clientId, masterUserRoleGroupId, true);
        } catch (Exception e) {
            customAssert.assertFalse(true, "Exception. " + e.getMessage());
        } finally {
            new Check().hitCheck(ConfigureEnvironment.getEndUserLoginId(), ConfigureEnvironment.getEnvironmentProperty("password"));
        }

        customAssert.assertAll();
    }

    private void enableDisableReport(CustomAssert customAssert, String reportName, String entity, String entityName, int clientId,
                                     String masterUserRoleGroupId, boolean activateReport) {
        Check check = new Check();
        HttpResponse httpResponse;
        int reportId = 0;

        boolean isReportPresent = ClientSetupAdminReportList.isReportPresentInClientSetupAdmin(entityName, reportName);
        customAssert.assertTrue(isReportPresent, reportName + " is not present in Client Setup Admin");

        check.hitCheckForClientSetup(ConfigureEnvironment.getClientSetupUserName(), ConfigureEnvironment.getClientSetupUserPassword());

        httpResponse = PreSignatureHelper.getReportsAccessForClient(clientId);
        customAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200, "Response Code to get report access for client is not valid");

        JSONObject reportsAccessResponseJson = PreSignatureHelper.getJsonObjectForResponse(httpResponse);

        int cdrReportsList = reportsAccessResponseJson.getJSONArray(entity).length();
        for (int i = 0; i < cdrReportsList; i++) {
            if (reportsAccessResponseJson.getJSONArray(entity).getJSONObject(i).get("name").equals(reportName)) {
                reportId = Integer.parseInt(reportsAccessResponseJson.getJSONArray(entity).getJSONObject(i).get("id").toString());
            }
        }

        List<Integer> allReportsIds = PreSignatureHelper.getAllReportIds(reportsAccessResponseJson);
        if (!activateReport) {
            customAssert.assertTrue(allReportsIds.contains(reportId), reportName + " Permission is Selected for Client");
        } else {
            allReportsIds.add(reportId);
        }

        List<String> reportConfigurePayload = PreSignatureHelper.createPayloadForReports(allReportsIds);
        String reportPayloadWithoutActivityReportId;
        if (!activateReport) {
            reportPayloadWithoutActivityReportId = PreSignatureHelper.createPayloadForReportsAfterRemovingReportId(reportConfigurePayload, reportId);
        } else {
            reportPayloadWithoutActivityReportId = reportConfigurePayload.toString();
        }

        check.hitCheckForClientSetup(ConfigureEnvironment.getClientSetupUserName(), ConfigureEnvironment.getClientSetupUserPassword());

        httpResponse = PreSignatureHelper.reportConfigure(clientId, reportPayloadWithoutActivityReportId);
        customAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200, "Reports are not configured");

        check.hitCheck(ConfigureEnvironment.getClientAdminUser(), ConfigureEnvironment.getClientAdminPassword());

        httpResponse = PreSignatureHelper.getMasterUserRoleGroup(masterUserRoleGroupId);
        String masterUserRoleGroupResponseStr = null;
        try {
            masterUserRoleGroupResponseStr = EntityUtils.toString(httpResponse.getEntity());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Document document = Jsoup.parse(masterUserRoleGroupResponseStr);
        int index = 0;
        Elements elements = document.select("#reportRoles h3 label span");
        for (Element element : elements) {
            index++;
            if (element.text().trim().equals(entity + ":")) {
                index--;
                break;
            }
        }

        List<String> allReports = new LinkedList<>();
        elements = document.select("#reportRoles div:nth-of-type(" + index + ") label");

        for (Element element : elements) {
            allReports.add(element.text().trim());
        }

        if (!activateReport) {
            customAssert.assertTrue(!allReports.contains(reportName), reportName + " Should not present in CDR Reports");
        } else {
            customAssert.assertTrue(allReports.contains(reportName), reportName + " Should present in CDR Reports");
        }

        httpResponse = PreSignatureHelper.getMasterUserRoleGroup(masterUserRoleGroupId);
        customAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200, "Response Code is not Valid");

        FileUtils fileUtils = new FileUtils();
        Map<String, String> formData = new LinkedHashMap<>();
        Map<String, String> keyValuePair;
        if (!activateReport) {
            keyValuePair = fileUtils.ReadKeyValueFromFile("src/test/resources/TestConfig/PreSignature/ActivityReportPermission/ActivityReportPermissionOff.txt",
                    ":", "MasterUserRoleGroup");
        } else {
            keyValuePair = fileUtils.ReadKeyValueFromFile("src/test/resources/TestConfig/PreSignature/ActivityReportPermission/ActivityReportPermissionOn.txt",
                    ":", "MasterUserRoleGroup");
        }

        for (Map.Entry<String, String> m : keyValuePair.entrySet()) {
            formData.put(m.getKey().trim(), m.getValue().trim());
        }

        String params = UrlEncodedString.getUrlEncodedString(formData);

        httpResponse = PreSignatureHelper.updateMasterUserRoleGroup(params);
        customAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 302, "Response Code is not valid");

        check.hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
        ReportsListHelper reportsListHelper = new ReportsListHelper();
        String listReportJsonStr = reportsListHelper.getReportListJsonResponse();

        if (entity.equals("Contract Draft Request")) {
            entity = "Contract Draft Requests";
        }

        List<String> allCDRReports = PreSignatureHelper.getAllReportsNameForEntity(entity, listReportJsonStr);
        if (!activateReport) {
            customAssert.assertTrue(!allCDRReports.contains(reportName), reportName + " is present in CDR Reports which is not expected");
        }

        String contractDraftRequestResponseString = ContractDraftRequest.createCDR(null, null, configFilePath, configFileName,
                "contract draft request fields", false);
        int contractDraftRequestId = PreSignatureHelper.getNewlyCreatedId(contractDraftRequestResponseString);
        String outputFilePath = "src/test/resources/TestConfig/PreSignature/DownloadActivityReport/ContractDraftRequestActivityReport.xlsx";
        httpResponse = PreSignatureHelper.hitDownload(outputFilePath, 160, String.valueOf(contractDraftRequestId));

        customAssert.assertTrue(httpResponse.getStatusLine().getStatusCode() == 200, "Download Report API Response Code is not valid");
    }

    @Test(priority = 9)
    public void checkClauseEditFunctionality() {
        SoftAssert softAssert = new SoftAssert();
        // Create New Clause
        int clauseId = createClauseWithTag(softAssert);
        // Edit Created Clause before Activation
        HashMap<String, String> editClauseDetailsBeforeActivation = editClauseWithName(clauseId, false, softAssert);
        softAssert.assertTrue(!editClauseDetailsBeforeActivation.get("originalClauseName").equals(editClauseDetailsBeforeActivation.get("finalClauseName")),
                "Clause is not Edited");

        HttpResponse httpResponse = PreSignatureHelper.getClause(clauseId);
        JSONObject jsonObject = PreSignatureHelper.getJsonObjectForResponse(httpResponse);

        softAssert.assertTrue(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("clauseTags").getJSONArray("values").length() == 1,
                "Tag is not created successfully");
        String tagNameBeforeTagUpdate = jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("clauseTags").getJSONArray("values")
                .getJSONObject(0).get("name").toString();
        int tagIdBeforeTagUpdate = Integer.parseInt(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("clauseTags")
                .getJSONArray("values").getJSONObject(0).get("id").toString());

        // Update Clause with Text
        updateClauseWithTag(clauseId, softAssert);

        httpResponse = PreSignatureHelper.getClause(clauseId);
        jsonObject = PreSignatureHelper.getJsonObjectForResponse(httpResponse);

        softAssert.assertTrue(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("clauseTags").getJSONArray("values").length() == 1,
                "Tag is not created successfully");
        String tagNameAfterTagUpdate = jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("clauseTags").getJSONArray("values")
                .getJSONObject(0).get("name").toString();
        int tagIdAfterTagUpdate = Integer.parseInt(jsonObject.getJSONObject("body").getJSONObject("data").getJSONObject("clauseTags")
                .getJSONArray("values").getJSONObject(0).get("id").toString());

        softAssert.assertTrue(!tagNameBeforeTagUpdate.equals(tagNameAfterTagUpdate), "Tag is not updated");
        softAssert.assertTrue(tagIdBeforeTagUpdate != tagIdAfterTagUpdate, "Tag is not updated");

        // Activate Clause
        activateClause(clauseId, softAssert);

        // Edit Created Clause After Activation
        HashMap<String, String> editClauseDetailsAfterActivation = editClauseWithName(clauseId, true, softAssert);
        softAssert.assertTrue(editClauseDetailsAfterActivation.size() == 0, "Clause is Edited");
        softAssert.assertAll();
    }

    @DataProvider(name = "clauseAndDefinitionAuditLogData")
    public Object[][] clauseAndDefinitionData() {
        return new Object[][]{
                {"clauses", "fields", 0, 61, 138},
                {"clauses", "definition fields", 1, 61, 138}
        };
    }

    @Test(dataProvider = "clauseAndDefinitionAuditLogData", priority = 10)
    public void verifyClauseAuditLogTab(String entity, String sectionName, int entityIndex, int clauseAuditLogListId, int entityTypeId) {
        SoftAssert softAssert = new SoftAssert();
        Map<String, String> clauseAuditLogListFormData = new HashMap<>();
        clauseAuditLogListFormData.put("entityTypeId", String.valueOf(entityTypeId));
        /////// Clause Creation ////////
        try {
            PreSignatureHelper.setFieldsInConfigForEntities(configFilePath, configFileName, entity, sectionName, entityIndex);

            int entityId;
            // Create new Clause
            if (sectionName.equals("fields")) {
                String createdClauseId = Clause.createClause(null, null, configFilePath, configFileName, "fields", false);
                entityId = PreSignatureHelper.getNewlyCreatedId(createdClauseId);
                logger.info("Newly Created Clause Id " + entityId);
            } else {
                String definitionResponseString = Definition.createDefinition(null, null, configFilePath, configFileName,
                        "definition fields", false);
                entityId = PreSignatureHelper.getNewlyCreatedId(definitionResponseString);
                logger.info("Newly Created Definition Id " + entityId);
            }

            if (entityId == -1) {
                throw new SkipException("Error in Creating Clause/Definition");
            }

            JSONObject auditLogListMetaDataJson = getMetaDataJson(clauseAuditLogListId, clauseAuditLogListFormData);

            List<Integer> defaultColumns = PreSignatureHelper.getDefaultColumns(auditLogListMetaDataJson.getJSONArray("columns"));

            JSONObject auditLogListDataJson = getListDataJson(entityTypeId, entityId);

            softAssert.assertTrue(auditLogListDataJson.getJSONArray("data").length() == 1, "Audit Log Size is not one as it is newly created");

            List<Map<String, String>> auditLogData = getLogTabData(defaultColumns, auditLogListDataJson);
            softAssert.assertTrue(auditLogData.get(0).get("action_name").equals("Newly Created"), "Newly Created Action Name is not Displayed in Audit Log");

            // Activate Newly Created Clause
            // Send for client review Action
            PreSignatureHelper.activateEntity(entityId, softAssert);
            auditLogListDataJson = getListDataJson(entityTypeId, entityId);
            softAssert.assertTrue(auditLogListDataJson.getJSONArray("data").length() == 2,
                    "Audit Log Size is not two as client review action is performed");
            auditLogData = getLogTabData(defaultColumns, auditLogListDataJson);
            softAssert.assertTrue(auditLogData.get(1).get("action_name").equals("Awaiting Client Review"),
                    "Awaiting Client Review Action Name is not Displayed in Audit Log");

            // Edit Created Clause before Activation
            HashMap<String, String> editClauseDetailsBeforeActivation = editClauseWithName(entityId, false, softAssert);
            softAssert.assertTrue(!editClauseDetailsBeforeActivation.get("originalClauseName").equals(editClauseDetailsBeforeActivation.get("finalClauseName")),
                    "Clause is not Edited");
            auditLogListDataJson = getListDataJson(entityTypeId, entityId);
            softAssert.assertTrue(auditLogListDataJson.getJSONArray("data").length() == 3,
                    "Audit Log Size is not three as client review action is performed");
            auditLogData = getLogTabData(defaultColumns, auditLogListDataJson);
            softAssert.assertTrue(auditLogData.get(2).get("action_name").equals("Updated"), "Clause is not updated on audit log");

            int fieldHistoryId = Integer.parseInt(auditLogData.get(2).get("history").split("/")[3]);
            HttpResponse fieldHistoryResponse = PreSignatureHelper.getFieldHistory(fieldHistoryId, entityTypeId);
            softAssert.assertTrue(fieldHistoryResponse.getStatusLine().getStatusCode() == 200, "Field History API Response Code is not Valid");

            JSONObject fieldHistoryJson = PreSignatureHelper.getJsonObjectForResponse(fieldHistoryResponse);
            String nameOldValue = fieldHistoryJson.getJSONArray("value").getJSONObject(0).get("oldValue").toString();
            String nameNewValue = fieldHistoryJson.getJSONArray("value").getJSONObject(0).get("newValue").toString();

            softAssert.assertTrue(nameOldValue.equals(editClauseDetailsBeforeActivation.get("originalClauseName")),
                    "Field History is not Reflecting Original Value");
            softAssert.assertTrue(nameNewValue.equals(editClauseDetailsBeforeActivation.get("finalClauseName")), "Field History is not Reflecting Final Value");

            // Approve Action
            PreSignatureHelper.activateEntity(entityId, softAssert);
            auditLogListDataJson = getListDataJson(entityTypeId, entityId);
            softAssert.assertTrue(auditLogListDataJson.getJSONArray("data").length() == 4,
                    "Audit Log Size is not four as approve action is performed");
            auditLogData = getLogTabData(defaultColumns, auditLogListDataJson);
            softAssert.assertTrue(auditLogData.get(3).get("action_name").equals("Approved"), "Approved Action Name is not Displayed in Audit Log");

            // Publish Action
            PreSignatureHelper.activateEntity(entityId, softAssert);
            auditLogListDataJson = getListDataJson(entityTypeId, entityId);
            softAssert.assertTrue(auditLogListDataJson.getJSONArray("data").length() == 5,
                    "Audit Log Size is not five as publish action is performed");
            auditLogData = getLogTabData(defaultColumns, auditLogListDataJson);
            softAssert.assertTrue(auditLogData.get(4).get("action_name").equals("Active"), "Active Action Name is not Displayed in Audit Log");

            boolean isClauseDeleted = EntityOperationsHelper.deleteEntityRecord(entity, entityId);
            softAssert.assertTrue(isClauseDeleted, "Clause is not Deleted Successfully");
        } catch (Exception e) {
            logger.info("Exception while verifying Audit Log Functionality of Clause");
        }
        softAssert.assertAll();
    }

    @Test(priority = 11)
    public void testClauseDownload() {
        SoftAssert softAssert = new SoftAssert();
        /////// Clause Creation ////////
        PreSignatureHelper.setFieldsInConfigForEntities(configFilePath, configFileName, "clauses", "fields", 0);

        // Create new Clause with tags
        String createdClauseId = Clause.createClause(null, null, configFilePath, configFileName, "fields", false);
        int clauseId = PreSignatureHelper.getNewlyCreatedId(createdClauseId);
        logger.info("Newly Created Clause Id " + clauseId);

        // Activate Newly Created Clause
        // Send for client review Action
        PreSignatureHelper.activateEntity(clauseId, softAssert);

        HttpResponse clauseDownloadResponse = PreSignatureHelper.hitClauseDownload("src/test/resources/TestConfig/PreSignature/ClauseDownload/Clause.docx",
                clauseId);
        softAssert.assertTrue(clauseDownloadResponse.getStatusLine().getStatusCode() == 200, "Clause Download API Response Code is not Valid");

        softAssert.assertTrue(FileUtils.fileExists("src/test/resources/TestConfig/PreSignature/ClauseDownload", "Clause.docx"),
                "File doesn't exist in Path");
        // Approve Action
        PreSignatureHelper.activateEntity(clauseId, softAssert);
        // Publish Action
        PreSignatureHelper.activateEntity(clauseId, softAssert);

        softAssert.assertTrue(FileUtils.deleteFile("src/test/resources/TestConfig/PreSignature/ClauseDownload/Clause.docx"),
                "File is not deleted successfully");

        boolean isClauseDeleted = EntityOperationsHelper.deleteEntityRecord("clauses", clauseId);
        softAssert.assertTrue(isClauseDeleted, "Clause is not Deleted Successfully");
        softAssert.assertAll();
    }

    @DataProvider(name = "clauseForwardReferenceTestData")
    public Object[][] clauseForwardReferenceTestData() {
        return new Object[][]{
                {"clauses", "fields", 0},
                {"clauses", "definition fields", 1}
        };
    }

    @Test(dataProvider = "clauseForwardReferenceTestData", priority = 12)
    public void testClauseForwardReferenceTab(String entityName, String section, int index) {
        SoftAssert softAssert = new SoftAssert();
        int forwardReferenceAffectedTemplateTabId = 306;
        int entityTypeId = 138;
        /////// Clause Creation ////////
        PreSignatureHelper.setFieldsInConfigForEntities(configFilePath, configFileName, entityName, section, index);

        int entityId;
        // Create new Clause with tags
        if (section.equals("fields")) {
            String createdClauseId = Clause.createClause(null, null, configFilePath, configFileName, "fields", false);
            entityId = PreSignatureHelper.getNewlyCreatedId(createdClauseId);
            logger.info("Newly Created Clause Id " + entityId);
        } else {
            String definitionResponseString = Definition.createDefinition(null, null, configFilePath, configFileName,
                    "definition fields", false);
            entityId = PreSignatureHelper.getNewlyCreatedId(definitionResponseString);
            logger.info("Newly Created Definition Id " + entityId);
        }

        // Activate Newly Created Clause
        // Send for client review Action
        PreSignatureHelper.activateEntity(entityId, softAssert);
        // Approve Action
        PreSignatureHelper.activateEntity(entityId, softAssert);
        // Publish Action
        PreSignatureHelper.activateEntity(entityId, softAssert);

        ///////////// Contract Template Creation////////////////
        PreSignatureHelper.setFieldsInConfigForEntities(configFilePath, configFileName, "contract templates", "contract template fields", 0);

        // Hitting Edit Get API to get Clause text and Clause tag to update
        HttpResponse getCreatedClauseResponse = PreSignatureHelper.showCreatedClause(entityId);
        softAssert.assertTrue(getCreatedClauseResponse.getStatusLine().getStatusCode() == 200, "Error in fetching the new created clause");
        JSONObject createdClauseJson = PreSignatureHelper.getJsonObjectForResponse(getCreatedClauseResponse);

        JSONObject entityCategory;
        int entityFieldId;
        String entityPayload;
        // Setting values for clauses to be selected while creating contract template

        if (section.equals("fields")) {
            entityCategory = new JSONObject(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "fields",
                    "category"));
            entityFieldId = PreSignatureHelper.getFieldId("contract templates", "clauses");
            entityPayload = "{\"name\": \"clauses\",\"id\": " + entityFieldId + ",\"multiEntitySupport\": false,\"values\": [{\"clauseCategory\": {\"name\": \"" +
                    entityCategory.getJSONObject("values").get("name").toString() + "\",\"id\": \"" + Integer.valueOf(entityCategory.getJSONObject("values")
                    .get("id").toString()) + "\"},\"clause\": {\"name\": \"" + createdClauseJson.getJSONObject("body").getJSONObject("data").getJSONObject("name")
                    .get("values") + "\",\"id\": " + entityId + "},\"clauseGroup\": {\"name\": \"Clause\",\"id\": 1},\"order\": 1,\"mandatory\": null}]}";
        } else {
            entityCategory = new JSONObject(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "definition fields",
                    "definitionCategory"));
            entityFieldId = PreSignatureHelper.getFieldId("contract templates", "clauses");
            entityPayload = "{\"name\": \"clauses\",\"id\": " + entityFieldId + ",\"multiEntitySupport\": false,\"values\": [{\"clauseCategory\": {\"name\": \"" +
                    entityCategory.getJSONObject("values").get("name").toString() + "\",\"id\": \"" + Integer.valueOf(entityCategory.getJSONObject("values")
                    .get("id").toString()) + "\"},\"clause\": {\"name\": \"" + createdClauseJson.getJSONObject("body").getJSONObject("data").getJSONObject("name")
                    .get("values") + "\",\"id\": " + entityId + "},\"clauseGroup\": {\"name\": \"Clause\",\"id\": 2},\"order\": 2,\"mandatory\": null}]}";
        }
        try {
            ParseConfigFile.updateValueInConfigFile(configFilePath, configFileName, "contract template fields", "clauses", entityPayload);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String contractTemplateResponseString = ContractTemplate.createContractTemplate(null, null, configFilePath, configFileName,
                "contract template fields", false);
        int contractTemplateId = PreSignatureHelper.getNewlyCreatedId(contractTemplateResponseString);

        Map<String, String> forwardReferenceMetaDataListFormData = new HashMap<>();
        forwardReferenceMetaDataListFormData.put("entityTypeId", String.valueOf(entityTypeId));
        JSONObject forwardReferenceMetaDataListJson = getMetaDataJson(forwardReferenceAffectedTemplateTabId, forwardReferenceMetaDataListFormData);
        List<Integer> defaultColumns = PreSignatureHelper.getDefaultColumns(forwardReferenceMetaDataListJson.getJSONArray("columns"));

        JSONObject forwardReferenceTabListDataJson = getForwardReferenceListDataJson(entityTypeId, entityId);
        List<Map<String, String>> forwardReferenceTab = getLogTabData(defaultColumns, forwardReferenceTabListDataJson);

        softAssert.assertTrue(forwardReferenceTab.get(0).get("entityid").contains(String.valueOf(contractTemplateId)),
                "Contract Template is not present in Clause Forward Reference Tab");

        EntityOperationsHelper.deleteEntityRecord("contract templates", contractTemplateId);

        forwardReferenceTabListDataJson = getForwardReferenceListDataJson(entityTypeId, entityId);
        forwardReferenceTab = getLogTabData(defaultColumns, forwardReferenceTabListDataJson);

        softAssert.assertTrue(forwardReferenceTab.size() == 0, "Contract Template is not deleted Successfully");

        EntityOperationsHelper.deleteEntityRecord("clauses", entityId);
        softAssert.assertAll();
    }

    @Test(priority = 13)
    public void testCDRCommunicationTabFunctionality() {
        // Create new Contract Draft Request
        SoftAssert softAssert = new SoftAssert();
        testCommunicationTab("comment", softAssert);
        testCommunicationTab("shareWithSupplier", softAssert);
        testCommunicationTab("requestedBy", softAssert);
        testCommunicationTab("commentWithTag", softAssert);
        softAssert.assertAll();
    }

    @Test(priority = 14)
    public void testContractTemplateStructure() {
        SoftAssert softAssert = new SoftAssert();

        // Hitting New API To get all the clause categories
        New newObj = new New();
        newObj.hitNew("contract template structure");
        String newAPIResponse = newObj.getNewJsonStr();
        JSONObject newAPIResponseJson = new JSONObject(newAPIResponse);
        List<Map<String, String>> allClauseCategories = getAllClauseCategories(newAPIResponseJson);
        List<HashMap<String, String>> allAgreementTypes = getAllAgreementTypes(newAPIResponseJson);
        JSONArray categoryArray = addClauseCategoriesParameters(allClauseCategories);
        List<HashMap<String, String>> selectedClauseCategory = getRandomClauseCategoriesToSelect(categoryArray, 2);

        Map<String, String> agreementTypeToSelect = allAgreementTypes.get(RandomNumbers.getRandomNumberWithinRangeIndex(0, allAgreementTypes.size()));

        newAPIResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("agreementTypes").put("values", new JSONArray());
        JSONObject agreementTypeToAdd = new JSONObject();
        for (Map.Entry<String, String> m : agreementTypeToSelect.entrySet()) {
            agreementTypeToAdd.put(m.getKey(), m.getValue());
        }

        newAPIResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("agreementTypes").getJSONArray("values").put(agreementTypeToAdd);
        newAPIResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("clauseCategories").put("values", categoryArray);
        newAPIResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("clauseCategories").remove("options");
        String templateAttachment = newAPIResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("clauseCategories").toString();
        String agreementType = newAPIResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("agreementTypes").toString();

        try {
            ParseConfigFile.updateValueInConfigFileCaseSensitive(configFilePath, configFileName, "contract template structure",
                    "clauseCategories", templateAttachment);
            ParseConfigFile.updateValueInConfigFileCaseSensitive(configFilePath, configFileName, "contract template structure",
                    "agreementTypes", agreementType);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create new Contract Template Structure
        String createdContractTemplateStructureResponse = ContractTemplateStructure.createContractTemplateStructure(null, null,
                configFilePath, configFileName, "contract template structure", false);
        int contractTemplateStructureId = PreSignatureHelper.getNewlyCreatedId(createdContractTemplateStructureResponse);

        // Get Created Contract Template Structure to verify whether clause categories are attached to Contract Template Structure
        HttpResponse contractTemplateStructureResponse = PreSignatureHelper.getContractTemplateStructureResponse(contractTemplateStructureId);
        JSONObject contractTemplateStructureJson = PreSignatureHelper.getJsonObjectForResponse(contractTemplateStructureResponse);

        List<HashMap<String, String>> allSelectedClauseCategory = getAllClauseCategories(contractTemplateStructureJson.getJSONObject("body").getJSONObject("data")
                .getJSONObject("clauseCategories").getJSONArray("values"));
        allSelectedClauseCategory = allSelectedClauseCategory.stream().filter(m -> m.get("mandatory").equals("true")).collect(Collectors.toList());

        allSelectedClauseCategory.forEach(m -> m.remove("order"));
        allSelectedClauseCategory.forEach(m -> m.remove("mandatory"));

        softAssert.assertTrue(selectedClauseCategory.size() == allSelectedClauseCategory.size(),
                "Selected Clause Categories are not there in Contract Template Structure");
        softAssert.assertTrue(selectedClauseCategory.stream().map(m -> m.get("name")).sorted().collect(Collectors.toList()).equals(allSelectedClauseCategory
                        .stream().map(m -> m.get("name")).sorted().collect(Collectors.toList())),
                "Clause Categories names are not same as selected while creating Contract Template Structure");
        softAssert.assertTrue(selectedClauseCategory.stream().map(m -> m.get("id")).sorted().collect(Collectors.toList()).equals(allSelectedClauseCategory
                        .stream().map(m -> m.get("id")).sorted().collect(Collectors.toList())),
                "Clause Categories Ids are not same as selected while creating Contract Template Structure");

        /////// Clause Creation ////////
        List<Integer> allCreatedClauseIds = new LinkedList<>();
        try {
            for (HashMap<String, String> selectedCategoryMap : allSelectedClauseCategory) {
                PreSignatureHelper.setFieldsInConfigForEntities(configFilePath, configFileName, "clauses", "fields", 0);
                agreementType = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "contract template structure",
                        "agreementTypes");
                ParseConfigFile.updateValueInConfigFileCaseSensitive(configFilePath, configFileName, "fields", "agreementTypes", agreementType);
                for (Map.Entry<String, String> entry : selectedCategoryMap.entrySet()) {
                    String clauseCategory = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fields", "category");
                    JSONObject clauseCategoryJson = new JSONObject(clauseCategory);
                    clauseCategoryJson.getJSONObject("values").put(entry.getKey(), entry.getValue());
                    ParseConfigFile.updateValueInConfigFileCaseSensitive(configFilePath, configFileName, "fields", "category",
                            clauseCategoryJson.toString());
                }
                // Create new Clause
                String createdClauseId = Clause.createClause(null, null, configFilePath, configFileName, "fields", false);
                int clauseId = PreSignatureHelper.getNewlyCreatedId(createdClauseId);
                logger.info("Newly Created Clause Id " + clauseId);
                allCreatedClauseIds.add(clauseId);

                // Activate Newly Created Clause
                // Send for client review Action
                PreSignatureHelper.activateEntity(clauseId, softAssert);
                // Approve Action
                PreSignatureHelper.activateEntity(clauseId, softAssert);
                // Publish Action
                PreSignatureHelper.activateEntity(clauseId, softAssert);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Verify Contract Template Structure linked with agreement type
        HttpResponse getAllContractTemplateStructureLinkedWithAgreementType =
                PreSignatureHelper.getContractTemplateStructure(Integer.parseInt(agreementTypeToSelect.get("id")));
        softAssert.assertTrue(getAllContractTemplateStructureLinkedWithAgreementType.getStatusLine().getStatusCode() == 200,
                "Response Code for Get All Contract Template Structure Linked with Agreement Type is invalid");

        HashMap<String, String> linkedContractTemplateStructureData = new HashMap<>();
        JSONObject getAllContractTemplateStructureLinkedWithAgreementTypeJson =
                PreSignatureHelper.getJsonObjectForResponse(getAllContractTemplateStructureLinkedWithAgreementType);
        int contractTemplateStructureLength = getAllContractTemplateStructureLinkedWithAgreementTypeJson.getJSONObject("body").getJSONArray("data").length();
        for (int i = 0; i < contractTemplateStructureLength; i++) {
            if (getAllContractTemplateStructureLinkedWithAgreementTypeJson.getJSONObject("body").getJSONArray("data").getJSONObject(i).get("id")
                    .toString().trim().equals(String.valueOf(contractTemplateStructureId).trim())) {
                linkedContractTemplateStructureData.put("name", getAllContractTemplateStructureLinkedWithAgreementTypeJson.getJSONObject("body")
                        .getJSONArray("data").getJSONObject(i).get("name").toString());
                linkedContractTemplateStructureData.put("id", getAllContractTemplateStructureLinkedWithAgreementTypeJson.getJSONObject("body")
                        .getJSONArray("data").getJSONObject(i).get("id").toString());
                linkedContractTemplateStructureData.put("url", getAllContractTemplateStructureLinkedWithAgreementTypeJson.getJSONObject("body")
                        .getJSONArray("data").getJSONObject(i).get("url").toString());
            }
        }

        softAssert.assertTrue(linkedContractTemplateStructureData.size() == 3,
                "Contract Template Structure is not linked with Agreement Type Successfully");

        // Verify Mandatory Clauses linked with Contract Template Structure
        HttpResponse contractTemplateStructureClauseDataResponse = PreSignatureHelper.getContractTemplateStructureClauseData(contractTemplateStructureId);
        softAssert.assertTrue(contractTemplateStructureClauseDataResponse.getStatusLine().getStatusCode() == 200,
                "Contract Template Structure Clause Data API Response Code is not valid");
        JSONObject contractTemplateStructureClauseDataJson = PreSignatureHelper.getJsonObjectForResponse(contractTemplateStructureClauseDataResponse);
        List<JSONObject> allMandatoryClauses = PreSignatureHelper.getAllMandatoryClauses(contractTemplateStructureClauseDataJson);

        softAssert.assertTrue(allMandatoryClauses.size() == allSelectedClauseCategory.size(), "Mandate Clause and Selected Clause Size is not equal");
        for (int i = 0; i < allMandatoryClauses.size(); i++) {
            softAssert.assertTrue(allMandatoryClauses.get(i).getJSONObject("category").get("name").toString().equals(allSelectedClauseCategory.get(i).get("name")),
                    "Mandate Clause Name and Selected Clause Name is not same");
            softAssert.assertTrue(allMandatoryClauses.get(i).getJSONObject("category").get("id").toString().equals(allSelectedClauseCategory.get(i).get("id")),
                    "Mandate Clause Id and Selected Clause Id is not same");
        }

        // Create Contract Template with attached Contract Template Structure
        try {
            PreSignatureHelper.setFieldsInConfigForEntities(configFilePath, configFileName, "contract templates",
                    "contract template with contract template structure", 0);
            String agreementTypeForContractTemplateStructure = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,
                    "contract template with contract template structure", "agreementType");
            JSONObject agreementTypeForContractTemplateStructureJson = new JSONObject(agreementTypeForContractTemplateStructure);
            agreementTypeForContractTemplateStructureJson.getJSONObject("values").put("name", agreementTypeToSelect.get("name"));
            agreementTypeForContractTemplateStructureJson.getJSONObject("values").put("id", agreementTypeToSelect.get("id"));
            ParseConfigFile.updateValueInConfigFileCaseSensitive(configFilePath, configFileName, "contract template with contract template structure",
                    "agreementType", agreementTypeForContractTemplateStructureJson.toString());
            String attachedClauses = "{\"name\":\"clauses\",\"id\":7419,\"multiEntitySupport\":false,\"values\":[{\"clauseCategory\":{\"name\":\"Entire Agreement\"," +
                    "\"id\":\"1173\"},\"clause\":{\"name\":\"API Automation 7488557\",\"id\":5826},\"clauseGroup\":{\"name\":\"Clause\",\"id\":1},\"order\":1," +
                    "\"mandatory\":null},{\"clauseCategory\":{\"name\":\"Protection\",\"id\":\"1118\"},\"clause\":{\"name\":\"API Automation 7682894\",\"id\":5827}," +
                    "\"clauseGroup\":{\"name\":\"Clause\",\"id\":1},\"order\":2,\"mandatory\":null}]}";
            ParseConfigFile.updateValueInConfigFileCaseSensitive(configFilePath, configFileName, "contract template with contract template structure",
                    "clauses", attachedClauses);

            String clausesForContractTemplateStructure = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,
                    "contract template with contract template structure", "clauses");
            JSONObject clausesForContractTemplateStructureJson = new JSONObject(clausesForContractTemplateStructure);

            for (int i = 0; i < allCreatedClauseIds.size(); i++) {
                int clauseId = allCreatedClauseIds.get(i);
                HttpResponse createdClauseResponse = PreSignatureHelper.showCreatedClause(clauseId);
                softAssert.assertTrue(createdClauseResponse.getStatusLine().getStatusCode() == 200, "Get Created Clause API Response Code is not valid");
                JSONObject createdClauseResponseJson = PreSignatureHelper.getJsonObjectForResponse(createdClauseResponse);

                clausesForContractTemplateStructureJson.getJSONArray("values").getJSONObject(i).getJSONObject("clauseCategory")
                        .put("name", createdClauseResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("category").getJSONObject("values")
                                .get("name").toString());
                clausesForContractTemplateStructureJson.getJSONArray("values").getJSONObject(i).getJSONObject("clauseCategory")
                        .put("id", createdClauseResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("category").getJSONObject("values")
                                .get("id").toString());
                clausesForContractTemplateStructureJson.getJSONArray("values").getJSONObject(i).getJSONObject("clause")
                        .put("name", createdClauseResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("name").get("values").toString());
                clausesForContractTemplateStructureJson.getJSONArray("values").getJSONObject(i).getJSONObject("clause")
                        .put("id", createdClauseResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("id").get("values").toString());
            }

            ParseConfigFile.updateValueInConfigFileCaseSensitive(configFilePath, configFileName, "contract template with contract template structure",
                    "clauses", clausesForContractTemplateStructureJson.toString());
            String templateStructureDefaultValue = "{\"name\":\"templateStructure\",\"id\":7428,\"multiEntitySupport\":false," +
                    "\"values\":{\"name\":\"API Automation 2794768\",\"id\":1667,\"url\":\"/contracttemplatestructure/getData/1667\"}}";
            ParseConfigFile.updateValueInConfigFileCaseSensitive(configFilePath, configFileName, "contract template with contract template structure",
                    "templateStructure", templateStructureDefaultValue);
            String templateStructure = ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,
                    "contract template with contract template structure", "templateStructure");
            JSONObject templateStructureJson = new JSONObject(templateStructure);
            templateStructureJson.getJSONObject("values").put("name", linkedContractTemplateStructureData.get("name"));
            templateStructureJson.getJSONObject("values").put("id", linkedContractTemplateStructureData.get("id"));
            templateStructureJson.getJSONObject("values").put("url", linkedContractTemplateStructureData.get("url"));
            ParseConfigFile.updateValueInConfigFileCaseSensitive(configFilePath, configFileName, "contract template with contract template structure",
                    "templateStructure", templateStructureJson.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        String contractTemplateResponse = ContractTemplate.createContractTemplate(null, null, configFilePath, configFileName,
                "contract template with contract template structure", false);
        int contractTemplateId = PreSignatureHelper.getNewlyCreatedId(contractTemplateResponse);

        HttpResponse getContractTemplateResponse = PreSignatureHelper.getContractTemplateResponse(contractTemplateId);
        softAssert.assertTrue(getContractTemplateResponse.getStatusLine().getStatusCode() == 200, "Get Contract Template Response Code is not valid");

        // Delete all the above created entities
        boolean isContractTemplateDeleted = EntityOperationsHelper.deleteEntityRecord("contract templates", contractTemplateId);
        softAssert.assertTrue(isContractTemplateDeleted, "Contract Template is not deleted successfully");
        for (int clauseId : allCreatedClauseIds) {
            boolean isClauseDeleted = EntityOperationsHelper.deleteEntityRecord("clauses", clauseId);
            softAssert.assertTrue(isClauseDeleted, "Clause is not deleted successfully");
        }
        boolean isContractTemplateStructureDeleted = EntityOperationsHelper.deleteEntityRecord("contract template structure", contractTemplateStructureId);
        softAssert.assertTrue(isContractTemplateStructureDeleted, "Contract Template Structure is not deleted Successfully");
        softAssert.assertAll();
    }

    private List<HashMap<String, String>> getAllAgreementTypes(JSONObject newAPIResponseJson) {
        List<HashMap<String, String>> allAgreementTypes = new LinkedList<>();
        HashMap<String, String> agreementType;

        int allAgreementTypesLength = newAPIResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("agreementTypes").getJSONObject("options")
                .getJSONArray("data").length();

        for (int i = 0; i < allAgreementTypesLength; i++) {
            agreementType = new LinkedHashMap<>();
            agreementType.put("name", newAPIResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("agreementTypes").getJSONObject("options")
                    .getJSONArray("data").getJSONObject(i).get("name").toString());
            agreementType.put("id", newAPIResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("agreementTypes").getJSONObject("options")
                    .getJSONArray("data").getJSONObject(i).get("id").toString());
            allAgreementTypes.add(agreementType);
        }
        return allAgreementTypes;
    }

    private List<HashMap<String, String>> getAllClauseCategories(JSONArray clauseCategoryArray) {
        List<HashMap<String, String>> allClauseCategory = new LinkedList<>();
        HashMap<String, String> categoryMap;
        for (int i = 0; i < clauseCategoryArray.length(); i++) {
            categoryMap = new LinkedHashMap<>();
            categoryMap.put("name", clauseCategoryArray.getJSONObject(i).getJSONObject("clauseCategory").get("name").toString());
            categoryMap.put("id", clauseCategoryArray.getJSONObject(i).getJSONObject("clauseCategory").get("id").toString());
            categoryMap.put("order", clauseCategoryArray.getJSONObject(i).get("order").toString());
            categoryMap.put("mandatory", clauseCategoryArray.getJSONObject(i).get("mandatory").toString());
            allClauseCategory.add(categoryMap);
        }
        return allClauseCategory;
    }

    private JSONArray addClauseCategoriesParameters(List<Map<String, String>> allClauseCategories) {
        JSONArray selectedCategoriesArray = new JSONArray();
        int count = 1;
        for (Map<String, String> clauseCategoryMap : allClauseCategories) {
            JSONObject categoryData = new JSONObject();
            categoryData.put("order", count++);
            categoryData.put("mandatory", false);
            JSONObject clauseCategory = new JSONObject();
            for (Map.Entry<String, String> entry : clauseCategoryMap.entrySet()) {
                clauseCategory.put(entry.getKey(), entry.getValue());
            }
            categoryData.put("clauseCategory", clauseCategory);
            selectedCategoriesArray.put(categoryData);
            count++;
        }
        return selectedCategoriesArray;
    }

    private List<HashMap<String, String>> getRandomClauseCategoriesToSelect(JSONArray selectedCategoriesArray, int randomCategoriesToSelect) {
        List<HashMap<String, String>> allSelectedClauseCategory = new LinkedList<>();
        for (int i = 0; i < randomCategoriesToSelect; i++) {
            HashMap<String, String> clauseCategory = new HashMap<>();
            int clauseCategoryIndex = RandomNumbers.getRandomNumberWithinRangeIndex(0, selectedCategoriesArray.length());
            selectedCategoriesArray.getJSONObject(clauseCategoryIndex).put("mandatory", true);
            List<String> keys = new ArrayList<>(selectedCategoriesArray.getJSONObject(clauseCategoryIndex).getJSONObject("clauseCategory").keySet());
            for (String key : keys) {
                clauseCategory.put(key, selectedCategoriesArray.getJSONObject(clauseCategoryIndex).getJSONObject("clauseCategory").get(key).toString());
            }
            allSelectedClauseCategory.add(i, clauseCategory);
        }
        return allSelectedClauseCategory;
    }

    private List<Map<String, String>> getAllClauseCategories(JSONObject newAPIResponseJson) {
        int categoryOptionsLength = newAPIResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("clauseCategories").getJSONObject("options")
                .getJSONArray("data").length();
        List<Map<String, String>> allClauseCategories = new LinkedList<>();
        Map<String, String> clauseCategories;
        for (int i = 0; i < categoryOptionsLength; i++) {
            clauseCategories = new HashMap<>();
            List<String> headers = new ArrayList<>(newAPIResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("clauseCategories").getJSONObject("options")
                    .getJSONArray("data").getJSONObject(i).keySet());
            for (String header : headers) {
                clauseCategories.put(header, newAPIResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("clauseCategories")
                        .getJSONObject("options").getJSONArray("data").getJSONObject(i).get(header).toString());
            }
            allClauseCategories.add(clauseCategories);
        }
        return allClauseCategories;
    }

    private void testCommunicationTab(String argument, SoftAssert softAssert) {
        String contractDraftRequestResponseString = ContractDraftRequest.createCDR(null, null, configFilePath, configFileName,
                "contract draft request fields", false);
        int contractDraftRequestId = PreSignatureHelper.getNewlyCreatedId(contractDraftRequestResponseString);

        HttpResponse crdResponse = PreSignatureHelper.getContractDraftRequestResponse(contractDraftRequestId);
        softAssert.assertTrue(crdResponse.getStatusLine().getStatusCode() == 200, "CDR is not created successfully");
        JSONObject cdrResponseJson = PreSignatureHelper.getJsonObjectForResponse(crdResponse);

        String field = null;
        if (argument.equals("comment")) {
            field = "Test_Automation_Comment" + RandomString.getRandomAlphaNumericString(10);
            cdrResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("comments").put("values", field);
        } else if (argument.equals("shareWithSupplier")) {
            cdrResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("shareWithSupplier").put("values", true);
        } else if (argument.equals("requestedBy")) {
            JSONObject requestedByJson = new JSONObject();
            cdrResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("requestedBy").put("values", requestedByJson);
            cdrResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("requestedBy").getJSONObject("values")
                    .put("name", "Akshay User");
            cdrResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("requestedBy").getJSONObject("values").put("id", 1047);
            cdrResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("requestedBy").getJSONObject("values").put("idType", 2);
        } else if (argument.equals("commentWithTag")) {
            field = "test <span class=\"atwho-inserted\" data-atwho-at-query=\"@A\" contenteditable=\"false\"> Akshay User1 <span data-uid=\"1057\" " +
                    "data-urg=\"false\"></span></span>";
            cdrResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("comment").getJSONObject("comments").put("values", field);
        }

        String commentPayload = "{\"body\":{\"data\":" + cdrResponseJson.getJSONObject("body").getJSONObject("data").toString() + "}}";
        HttpResponse cdrCommentResponse = PreSignatureHelper.cdrComment(commentPayload);
        softAssert.assertTrue(cdrCommentResponse.getStatusLine().getStatusCode() == 200, "CDR Comment API Response is not Valid");
        JSONObject cdrCommentJson = PreSignatureHelper.getJsonObjectForResponse(cdrCommentResponse);

        if (argument.equals("comment") || argument.equals("commentWithTag")) {
            softAssert.assertTrue(cdrCommentJson.getJSONObject("header").getJSONObject("response").get("status").toString().equals("success"),
                    "CDR Comment is not successfully updated");
        } else if (argument.equals("shareWithSupplier") || argument.equals("requestedBy")) {
            softAssert.assertTrue(cdrCommentJson.getJSONObject("header").getJSONObject("response").get("status").toString().equals("validationError"),
                    "CDR Comment is successfully updated");
        }

        DefaultUserListMetadataHelper defaultUserListMetadataHelper = new DefaultUserListMetadataHelper();
        Map<String, String> cdrCommentFormData = new HashMap<>();
        cdrCommentFormData.put("entityTypeId", "160");
        cdrCommentFormData.put("entityId", String.valueOf(contractDraftRequestId));
        String commentDefaultListMetaData = defaultUserListMetadataHelper.getDefaultUserListMetadataResponse(65, cdrCommentFormData);
        JSONObject commentDefaultListMetaDataJson = new JSONObject(commentDefaultListMetaData);

        List<Integer> columnIds = PreSignatureHelper.getDefaultColumns(commentDefaultListMetaDataJson.getJSONArray("columns"));

        JSONObject tabListDataJson = new JSONObject(TabListDataHelper.hitTabListDataAPIForCDRCommunicationTab(contractDraftRequestId));

        List<Map<String, String>> commentTabListData = getLogTabData(columnIds, tabListDataJson);

        if (argument.equals("comment")) {
            softAssert.assertTrue(commentTabListData.get(0).get("comment").equals(field), "Comment is not getting reflected in list under Communication Tab");
        } else if (argument.equals("shareWithSupplier") || argument.equals("requestedBy")) {
            softAssert.assertTrue(commentTabListData.size() == 0, "Data has been created");
        } else if (argument.equals("commentWithTag")) {
            String shortCodeId = cdrResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("shortCodeId").get("values").toString();
            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC();
            List<List<String>> emailNotification;
            boolean isCDRIdPresent = false;
            for (int i = 1; i < 50; i++) {
                try {
                    emailNotification = postgreSQLJDBC.doSelect("SELECT * FROM system_emails WHERE to_mail LIKE '%akshay_user1%' ORDER BY id desc LIMIT 1;");
                    String emailSubject = emailNotification.get(0).get(3);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (emailSubject.contains(shortCodeId)) {
                        isCDRIdPresent = true;
                        break;
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            softAssert.assertTrue(isCDRIdPresent, "CDR Id is not present in system emails");
        }

        boolean isCdrDeleted = EntityOperationsHelper.deleteEntityRecord("contract draft request", contractDraftRequestId);
        softAssert.assertTrue(isCdrDeleted, "Created CDR is not deleted successfully");
    }

    private JSONObject getMetaDataJson(int listId, Map<String, String> formData) {
        DefaultUserListMetadataHelper defaultUserListMetadataHelper = new DefaultUserListMetadataHelper();
        String auditLogListMetaData = defaultUserListMetadataHelper.getDefaultUserListMetadataResponse(listId, formData);
        return new JSONObject(auditLogListMetaData);
    }

    private JSONObject getListDataJson(int entityTypeId, int clauseId) {
        String tabListPayload = TabListDataHelper.getDefaultTabListDataPayload(entityTypeId);
        String auditLogTabListData = TabListDataHelper.hitTabListDataAPIForAuditLogTab(entityTypeId, clauseId, tabListPayload);
        return new JSONObject(auditLogTabListData);
    }

    private JSONObject getForwardReferenceListDataJson(int entityTypeId, int clauseId) {
        String tabListPayload = TabListDataHelper.getDefaultTabListDataPayload(entityTypeId);
        String auditLogTabListData = TabListDataHelper.hitTabListDataAPIForClauseForwardReferenceTab(entityTypeId, clauseId, tabListPayload);
        return new JSONObject(auditLogTabListData);
    }

    private List<Map<String, String>> getLogTabData(List<Integer> columnIds, JSONObject auditLogDataJson) {
        List<Map<String, String>> auditLogTabData = new LinkedList<>();
        int auditLogDataLength = auditLogDataJson.getJSONArray("data").length();
        for (int i = 0; i < auditLogDataLength; i++) {
            int dataLength = auditLogDataJson.getJSONArray("data").getJSONObject(i).length();
            Map<String, String> rowData = new LinkedHashMap<>();
            for (int j = 0; j < dataLength; j++) {
                rowData.put(auditLogDataJson.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(columnIds.get(j))).get("columnName").toString(),
                        auditLogDataJson.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(columnIds.get(j))).get("value").toString());
            }
            auditLogTabData.add(rowData);
        }
        return auditLogTabData;
    }

    private int createClauseWithTag(SoftAssert softAssert) {
        /////// Clause Creation ////////
        PreSignatureHelper.setFieldsInConfigForEntities(configFilePath, configFileName, "clauses", "fields", 0);

        // Create new Clause with tags
        String createdClauseId = Clause.createClause(null, null, configFilePath, configFileName, "fields", false);
        int clauseId = PreSignatureHelper.getNewlyCreatedId(createdClauseId);
        logger.info("Newly Created Clause Id " + clauseId);

        if (clauseId == -1) {
            throw new SkipException("Error in Creating Clause");
        }

        // Get text for which tag has to be created
        String text = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fields constant values", "text");
        String[] wordTags = text.split(" ");
        int word = RandomNumbers.getRandomNumberWithinRangeIndex(0, wordTags.length);
        String wordToTag = wordTags[word];

        //Creating the new tag
        String tagCreationPayload = "{\"name\":\"" + wordToTag + "\",\"tagHTMLType\":{\"id\":1}}";
        HttpResponse createCreationResponse = PreSignatureHelper.createTag(tagCreationPayload);
        softAssert.assertTrue(createCreationResponse.getStatusLine().getStatusCode() == 200, "Tag Creation API Response is not valid");
        JSONObject tagCreationResponseStr = PreSignatureHelper.getJsonObjectForResponse(createCreationResponse);
        int newlyCreatedOrExistingTagId = 0;
        if (tagCreationResponseStr.toString().contains("Tag created successfully")) {
            softAssert.assertTrue(tagCreationResponseStr.getJSONObject("data").get("message").equals("Tag created successfully"), "Tag is not created successfully");
            newlyCreatedOrExistingTagId = Integer.parseInt(tagCreationResponseStr.getJSONObject("data").get("id").toString());
        } else {
            if (tagCreationResponseStr.toString().contains("A tag with the given name already exists")) {
                softAssert.assertTrue(tagCreationResponseStr.getJSONObject("data").get("message").
                                equals("A tag with the given name already exists. Please use the existing tag or create a tag with a different name."),
                        "Tag is not already present");
                HttpResponse getTagOptionsResponse = PreSignatureHelper.verifyWhetherTagIsAlreadyPresent(wordToTag);
                softAssert.assertTrue(getTagOptionsResponse.getStatusLine().getStatusCode() == 200, "Tag Options API Response COde is not valid");
                JSONObject tagOptionsResponseJson = PreSignatureHelper.getJsonObjectForResponse(getTagOptionsResponse);
                int tagsLength = tagOptionsResponseJson.getJSONArray("data").length();
                for (int i = 0; i < tagsLength; i++) {
                    if (tagOptionsResponseJson.getJSONArray("data").getJSONObject(i).get("name").toString().equals(wordToTag)) {
                        newlyCreatedOrExistingTagId = Integer.parseInt(tagOptionsResponseJson.getJSONArray("data").getJSONObject(i).get("id").toString());
                    }
                }
            }
        }

        // Updating clause with tag
        String htmlText = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fields constant values", "html");
        String[] htmlWordBag = htmlText.split(" ");
        String htmlToTag = htmlWordBag[word];

        // Replacing html with tagged html
        String updateTag = "<span class= \"\" contenteditable=\"true\"><span class=\"tag_" + newlyCreatedOrExistingTagId +
                " tag\" contenteditable=\"false\" htmltagtype=\"1\" htmltagtypeval=\"\"><span style=\"display:none\">${" +
                newlyCreatedOrExistingTagId + ":</span>" + htmlToTag + "<span style=\"display:none\">}</span></span></span>";
        htmlWordBag[word] = updateTag;

        //Altering html text with newly created tag
        StringBuffer sb = new StringBuffer();
        for (String s : htmlWordBag) {
            sb.append(s + " ");
        }

        // Hitting Edit Get API to get Clause text and Clause tag to update
        HttpResponse getCreatedClauseResponse = PreSignatureHelper.getClause(clauseId);
        softAssert.assertTrue(getCreatedClauseResponse.getStatusLine().getStatusCode() == 200, "Error in fetching the new created clause");
        JSONObject createdClauseJson = PreSignatureHelper.getJsonObjectForResponse(getCreatedClauseResponse);
        // Get the json object needed to update to link newly created tag with clause
        // Updating clause tag
        createdClauseJson.getJSONObject("body").getJSONObject("data").getJSONObject("clauseTags").put("values", new JSONArray());
        JSONObject tagValues = new JSONObject();
        tagValues.put("id", newlyCreatedOrExistingTagId);
        tagValues.put("name", newlyCreatedOrExistingTagId);
        JSONObject tagHtmlType = new JSONObject();
        tagHtmlType.put("id", 1);
        tagHtmlType.put("name", "Text Field");
        tagValues.put("tagHTMLType", tagHtmlType);
        createdClauseJson.getJSONObject("body").getJSONObject("data").getJSONObject("clauseTags").getJSONArray("values").put(tagValues);
        // Updating Clause text
        createdClauseJson.getJSONObject("body").getJSONObject("data").getJSONObject("clauseText").getJSONObject("values").put("htmlText", sb.toString().trim());

        // Edit Clause with tags
        String editClausePayload = "{\"body\":{\"data\":" + createdClauseJson.getJSONObject("body").getJSONObject("data").toString() + "}}";
        HttpResponse editClauseAPIResponse = PreSignatureHelper.editClause(editClausePayload);
        softAssert.assertTrue(editClauseAPIResponse.getStatusLine().getStatusCode() == 200, "Error while editing clause");
        JSONObject editedClauseResponseJson = PreSignatureHelper.getJsonObjectForResponse(editClauseAPIResponse);
        softAssert.assertTrue(editedClauseResponseJson.getJSONObject("header").getJSONObject("response").get("status").toString().equals("success"),
                "Tag is not updated with clause");

        return clauseId;
    }

    private void updateClauseWithTag(int clauseId, SoftAssert softAssert) {
        // Get text for which tag has to be created
        String text = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fields constant values", "text");
        String[] wordTags = text.split(" ");
        int word = RandomNumbers.getRandomNumberWithinRangeIndex(0, wordTags.length);
        String wordToTag = wordTags[word];

        //Creating the new tag
        String tagCreationPayload = "{\"name\":\"" + wordToTag + "\",\"tagHTMLType\":{\"id\":1}}";
        HttpResponse createCreationResponse = PreSignatureHelper.createTag(tagCreationPayload);
        softAssert.assertTrue(createCreationResponse.getStatusLine().getStatusCode() == 200, "Tag Creation API Response is not valid");
        JSONObject tagCreationResponseStr = PreSignatureHelper.getJsonObjectForResponse(createCreationResponse);
        int newlyCreatedOrExistingTagId = 0;
        if (tagCreationResponseStr.toString().contains("Tag created successfully")) {
            softAssert.assertTrue(tagCreationResponseStr.getJSONObject("data").get("message").equals("Tag created successfully"), "Tag is not created successfully");
            newlyCreatedOrExistingTagId = Integer.parseInt(tagCreationResponseStr.getJSONObject("data").get("id").toString());
        } else {
            if (tagCreationResponseStr.toString().contains("A tag with the given name already exists")) {
                softAssert.assertTrue(tagCreationResponseStr.getJSONObject("data").get("message")
                                .equals("A tag with the given name already exists. Please use the existing tag or create a tag with a different name."),
                        "Tag is not already present");
                HttpResponse getTagOptionsResponse = PreSignatureHelper.verifyWhetherTagIsAlreadyPresent(wordToTag);
                softAssert.assertTrue(getTagOptionsResponse.getStatusLine().getStatusCode() == 200, "Tag Options API Response COde is not valid");
                JSONObject tagOptionsResponseJson = PreSignatureHelper.getJsonObjectForResponse(getTagOptionsResponse);
                int tagsLength = tagOptionsResponseJson.getJSONArray("data").length();
                for (int i = 0; i < tagsLength; i++) {
                    if (tagOptionsResponseJson.getJSONArray("data").getJSONObject(i).get("name").toString().equals(wordToTag)) {
                        newlyCreatedOrExistingTagId = Integer.parseInt(tagOptionsResponseJson.getJSONArray("data").getJSONObject(i).get("id").toString());
                    }
                }
            }
        }

        // Updating clause with tag
        String htmlText = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fields constant values", "html");
        String[] htmlWordBag = htmlText.split(" ");
        String htmlToTag = htmlWordBag[word];

        // Replacing html with tagged html
        String updateTag = "<span class= \"\" contenteditable=\"true\"><span class=\"tag_" + newlyCreatedOrExistingTagId +
                " tag\" contenteditable=\"false\" htmltagtype=\"1\" htmltagtypeval=\"\"><span style=\"display:none\">${" + newlyCreatedOrExistingTagId +
                ":</span>" + htmlToTag + "<span style=\"display:none\">}</span></span></span>";
        htmlWordBag[word] = updateTag;

        //Altering html text with newly created tag
        StringBuffer sb = new StringBuffer();
        for (String s : htmlWordBag) {
            sb.append(s + " ");
        }

        // Hitting Edit Get API to get Clause text and Clause tag to update
        HttpResponse getCreatedClauseResponse = PreSignatureHelper.getClause(clauseId);
        softAssert.assertTrue(getCreatedClauseResponse.getStatusLine().getStatusCode() == 200, "Error in fetching the new created clause");
        JSONObject createdClauseJson = PreSignatureHelper.getJsonObjectForResponse(getCreatedClauseResponse);
        // Get the json object needed to update to link newly created tag with clause
        // Updating clause tag
        createdClauseJson.getJSONObject("body").getJSONObject("data").getJSONObject("clauseTags").put("values", new JSONArray());
        JSONObject tagValues = new JSONObject();
        tagValues.put("id", newlyCreatedOrExistingTagId);
        tagValues.put("name", newlyCreatedOrExistingTagId);
        JSONObject tagHtmlType = new JSONObject();
        tagHtmlType.put("id", 1);
        tagHtmlType.put("name", "Text Field");
        tagValues.put("tagHTMLType", tagHtmlType);
        createdClauseJson.getJSONObject("body").getJSONObject("data").getJSONObject("clauseTags").getJSONArray("values").put(tagValues);
        // Updating Clause text
        createdClauseJson.getJSONObject("body").getJSONObject("data").getJSONObject("clauseText").getJSONObject("values").put("htmlText", sb.toString().trim());

        // Edit Clause with tags
        String editClausePayload = "{\"body\":{\"data\":" + createdClauseJson.getJSONObject("body").getJSONObject("data").toString() + "}}";
        HttpResponse editClauseAPIResponse = PreSignatureHelper.editClause(editClausePayload);
        softAssert.assertTrue(editClauseAPIResponse.getStatusLine().getStatusCode() == 200, "Error while editing clause");
        JSONObject editedClauseResponseJson = PreSignatureHelper.getJsonObjectForResponse(editClauseAPIResponse);
        softAssert.assertTrue(editedClauseResponseJson.getJSONObject("header").getJSONObject("response").get("status").toString().equals("success"),
                "Tag is not updated with clause");
    }

    private void activateClause(int clauseId, SoftAssert softAssert) {

        // Activate Newly Created Clause
        // Send for client review Action
        PreSignatureHelper.activateEntity(clauseId, softAssert);
        // Approve Action
        PreSignatureHelper.activateEntity(clauseId, softAssert);
        // Publish Action
        PreSignatureHelper.activateEntity(clauseId, softAssert);
    }

    private HashMap<String, String> editClauseWithName(int clauseId, boolean clauseActive, SoftAssert softAssert) {
        HashMap<String, String> editDetails = new HashMap<>();

        // Hitting Edit Get API to get Clause text and Clause tag to update
        HttpResponse getCreatedClauseResponse = PreSignatureHelper.getClause(clauseId);
        softAssert.assertTrue(getCreatedClauseResponse.getStatusLine().getStatusCode() == 200, "Error in fetching the new created clause");
        JSONObject createdClauseJson = PreSignatureHelper.getJsonObjectForResponse(getCreatedClauseResponse);

        if (!clauseActive) {
            String originalClauseName = createdClauseJson.getJSONObject("body").getJSONObject("data").getJSONObject("name").get("values").toString();
            editDetails.put("originalClauseName", createdClauseJson.getJSONObject("body").getJSONObject("data").getJSONObject("name").get("values").toString());
            String finalClauseName = originalClauseName + RandomString.getRandomAlphaNumericString(5);

            createdClauseJson.getJSONObject("body").getJSONObject("data").getJSONObject("name").put("values", finalClauseName);
            // Edit Clause with tags
            String editClausePayload = "{\"body\":{\"data\":" + createdClauseJson.getJSONObject("body").getJSONObject("data").toString() + "}}";
            HttpResponse editClauseAPIResponse = PreSignatureHelper.editClause(editClausePayload);
            softAssert.assertTrue(editClauseAPIResponse.getStatusLine().getStatusCode() == 200, "Error while editing clause");
            JSONObject editedClauseResponseJson = PreSignatureHelper.getJsonObjectForResponse(editClauseAPIResponse);
            softAssert.assertTrue(editedClauseResponseJson.getJSONObject("header").getJSONObject("response").get("status").toString().equals("success"),
                    "Tag is not updated with clause");
            editDetails.put("finalClauseName", finalClauseName);
        } else {
            softAssert.assertTrue(createdClauseJson.getJSONObject("header").getJSONObject("response").get("errorMessage").toString()
                            .equals("Either you do not have the required permissions or requested page does not exist anymore."),
                    "Clause is Editable which is not desired");
        }

        return editDetails;
    }


    /*
     * test created to test definition download
     */
    @Test(priority = 15)
    public void testDefinitionDownload() {

        SoftAssert softAssert = new SoftAssert();

        int definitionIDasCreated = createDefinitionID(softAssert);

        HttpResponse definitionDownloadResponse = PreSignatureHelper.hitDefinitionDownload(
                "src/test/resources/TestConfig/PreSignature/DefinitionDownload/Definition.docx", definitionIDasCreated);

        softAssert.assertTrue(definitionDownloadResponse.getStatusLine().getStatusCode() == 200,
                "Contract Template Download API Response Code is not Valid");

        //checking whether file really downloaded at the given location by the given name or not
        softAssert.assertTrue(FileUtils.fileExists("src/test/resources/TestConfig/PreSignature/DefinitionDownload", "Definition.docx"),
                "File doesn't exist in Path");

        softAssert.assertTrue(FileUtils.deleteFile("src/test/resources/TestConfig/PreSignature/DefinitionDownload/Definition.docx"),
                "Definition File is not deleted successfully");

        boolean isDefinitionDeleted = EntityOperationsHelper.deleteEntityRecord("clauses", definitionIDasCreated);

        softAssert.assertTrue(isDefinitionDeleted, "Definition ID not deleted");

        softAssert.assertAll();
    }

    /*
     * test created to test definition download
     */
    @Test(priority = 16)
    public void testContractTemplateDownload() {

        SoftAssert softAssert = new SoftAssert();

        int contractTemplateIDasCreated = createContractTemplateID(softAssert);

        HttpResponse contractTemplateResponse = PreSignatureHelper.getContractTemplateResponse(contractTemplateIDasCreated);

        softAssert.assertTrue(contractTemplateResponse.getStatusLine().getStatusCode() == 200, "Contract Template is not created successfully");

        HttpResponse contractTemplateDownloadResponse = PreSignatureHelper.hitContractTemplateDownload(
                "src/test/resources/TestConfig/PreSignature/ContractTemplate.docx", contractTemplateIDasCreated);

        softAssert.assertTrue(contractTemplateDownloadResponse.getStatusLine().getStatusCode() == 200,
                "Contract Template Download API Response Code is not Valid");

        softAssert.assertTrue(FileUtils.fileExists("src/test/resources/TestConfig/PreSignature/", "ContractTemplate.docx"),
                "Contract Template File doesn't exist in Path");

        softAssert.assertTrue(FileUtils.deleteFile("src/test/resources/TestConfig/PreSignature/ContractTemplate.docx"),
                "Contract Template File is not deleted successfully");

        boolean isContractTemplateIdDeleted = EntityOperationsHelper.deleteEntityRecord("contract templates", contractTemplateIDasCreated);

        softAssert.assertTrue(isContractTemplateIdDeleted, "Contract Template not deleted");

        softAssert.assertAll();

    }

    /*
     * Test Tag Wizard in CDR
     */

    /*
     * Test case C89074 automation script
     */
    @Test(enabled = false)
    public void testCreateMetaDataTag() {

        SoftAssert softAssert = new SoftAssert();
        /////// Clause Creation ////////
        PreSignatureHelper.setFieldsInConfigForEntities(configFilePath, configFileName, "clauses", "fields", 0);

        // Create new Clause with tags
        String createdClauseId = Clause.createClause(null, null, configFilePath, configFileName, "fields", false);
        int clauseId = PreSignatureHelper.getNewlyCreatedId(createdClauseId);
        logger.info("Newly Created Clause Id " + clauseId);

        if (clauseId == -1) {
            throw new SkipException("Error in Creating Clause");
        }

        // Get text for which tag has to be created
        String text = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "clause constant values", "text");
        String[] wordTags = text.split(" ");
        int word = RandomNumbers.getRandomNumberWithinRangeIndex(0, wordTags.length);
        String wordToTag = wordTags[word];

        //Creating the new tag
        String tagCreationPayload = "{\"name\":\"" + wordToTag + "\",\"tagHTMLType\":{\"id\":1}}";
        HttpResponse createCreationResponse = PreSignatureHelper.createTag(tagCreationPayload);
        softAssert.assertTrue(createCreationResponse.getStatusLine().getStatusCode() == 200, "Tag Creation API Response is not valid");
        JSONObject tagCreationResponseStr = PreSignatureHelper.getJsonObjectForResponse(createCreationResponse);
        int newlyCreatedOrExistingTagId = 0;
        if (tagCreationResponseStr.toString().contains("Tag created successfully")) {
            softAssert.assertTrue(tagCreationResponseStr.getJSONObject("data").get("message").equals("Tag created successfully"), "Tag is not created successfully");
            newlyCreatedOrExistingTagId = Integer.parseInt(tagCreationResponseStr.getJSONObject("data").get("id").toString());
        } else {
            if (tagCreationResponseStr.toString().contains("A tag with the given name already exists")) {
                softAssert.assertTrue(tagCreationResponseStr.getJSONObject("data").get("message")
                                .equals("A tag with the given name already exists. Please use the existing tag or create a tag with a different name."),
                        "Tag is not already present");
                HttpResponse getTagOptionsResponse = PreSignatureHelper.verifyWhetherTagIsAlreadyPresent(wordToTag);
                softAssert.assertTrue(getTagOptionsResponse.getStatusLine().getStatusCode() == 200, "Tag Options API Response COde is not valid");
                JSONObject tagOptionsResponseJson = PreSignatureHelper.getJsonObjectForResponse(getTagOptionsResponse);
                int tagsLength = tagOptionsResponseJson.getJSONArray("data").length();
                for (int i = 0; i < tagsLength; i++) {
                    if (tagOptionsResponseJson.getJSONArray("data").getJSONObject(i).get("name").toString().equals(wordToTag)) {
                        newlyCreatedOrExistingTagId = Integer.parseInt(tagOptionsResponseJson.getJSONArray("data").getJSONObject(i).get("id").toString());
                    }
                }
            }
        }

        // Updating clause with tag
        String htmlText = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "clause constant values", "html");
        String[] htmlWordBag = htmlText.split(" ");
        String htmlToTag = htmlWordBag[word];

        // Replacing html with tagged html
        String updateTag = "<span class= \"\" contenteditable=\"true\"><span class=\"tag_" + newlyCreatedOrExistingTagId +
                " tag\" contenteditable=\"false\" htmltagtype=\"1\" htmltagtypeval=\"\"><span style=\"display:none\">${" + newlyCreatedOrExistingTagId + ":</span>" +
                htmlToTag + "<span style=\"display:none\">}</span></span></span>";
        htmlWordBag[word] = updateTag;

        //Altering html text with newly created tag
        StringBuffer sb = new StringBuffer();
        for (String s : htmlWordBag) {
            sb.append(s + " ");
        }

        // Hitting Edit Get API to get Clause text and Clause tag to update
        HttpResponse getCreatedClauseResponse = PreSignatureHelper.getClause(clauseId);
        softAssert.assertTrue(getCreatedClauseResponse.getStatusLine().getStatusCode() == 200, "Error in fetching the new created clause");
        JSONObject createdClauseJson = PreSignatureHelper.getJsonObjectForResponse(getCreatedClauseResponse);
        // Get the json object needed to update to link newly created tag with clause
        // Updating clause tag
        createdClauseJson.getJSONObject("body").getJSONObject("data").getJSONObject("clauseTags").put("values", new JSONArray());
        JSONObject tagValues = new JSONObject();
        tagValues.put("id", newlyCreatedOrExistingTagId);
        tagValues.put("name", newlyCreatedOrExistingTagId);
        JSONObject tagHtmlType = new JSONObject();
        tagHtmlType.put("id", 1);
        tagHtmlType.put("name", "Text Field");
        tagValues.put("tagHTMLType", tagHtmlType);
        createdClauseJson.getJSONObject("body").getJSONObject("data").getJSONObject("clauseTags").getJSONArray("values").put(tagValues);
        // Updating Clause text
        createdClauseJson.getJSONObject("body").getJSONObject("data").getJSONObject("clauseText").getJSONObject("values").put("htmlText", sb.toString().trim());

        // Edit Clause with tags
        String editClausePayload = "{\"body\":{\"data\":" + createdClauseJson.getJSONObject("body").getJSONObject("data").toString() + "}}";
        HttpResponse editClauseAPIResponse = PreSignatureHelper.editClause(editClausePayload);
        softAssert.assertTrue(editClauseAPIResponse.getStatusLine().getStatusCode() == 200, "Error while editing clause");
        JSONObject editedClauseResponseJson = PreSignatureHelper.getJsonObjectForResponse(editClauseAPIResponse);
        softAssert.assertTrue(editedClauseResponseJson.getJSONObject("header").getJSONObject("response").get("status").toString().equals("success"),
                "Tag is not updated with clause");

        // Activate Newly Created Clause
        // Send for client review Action
        PreSignatureHelper.activateEntity(clauseId, softAssert);
        // Approve Action
        PreSignatureHelper.activateEntity(clauseId, softAssert);
        // Publish Action
        PreSignatureHelper.activateEntity(clauseId, softAssert);
        softAssert.assertAll();
    }

    /*
     * Test case C89079 automation script
     */
    @Test(enabled = false)
    public void testMetaDataTagConsumedInCT() {

        SoftAssert softAssert = new SoftAssert();
        /////// Clause Creation ////////
        PreSignatureHelper.setFieldsInConfigForEntities(configFilePath, configFileName, "clauses", "fields", 0);

        // Create new Clause with tags
        String createdClauseId = Clause.createClause(null, null, configFilePath, configFileName, "fields", false);
        int clauseId = PreSignatureHelper.getNewlyCreatedId(createdClauseId);
        logger.info("Newly Created Clause Id " + clauseId);

        if (clauseId == -1) {
            throw new SkipException("Error in Creating Clause");
        }

        // Get text for which tag has to be created
        String text = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "clause constant values", "text");
        String[] wordTags = text.split(" ");
        int word = RandomNumbers.getRandomNumberWithinRangeIndex(0, wordTags.length);
        String wordToTag = wordTags[word];

        //Creating the new tag
        String tagCreationPayload = "{\"name\":\"" + wordToTag + "\",\"tagHTMLType\":{\"id\":1}}";
        HttpResponse createCreationResponse = PreSignatureHelper.createTag(tagCreationPayload);
        softAssert.assertTrue(createCreationResponse.getStatusLine().getStatusCode() == 200, "Tag Creation API Response is not valid");
        JSONObject tagCreationResponseStr = PreSignatureHelper.getJsonObjectForResponse(createCreationResponse);
        int newlyCreatedOrExistingTagId = 0;
        if (tagCreationResponseStr.toString().contains("Tag created successfully")) {
            softAssert.assertTrue(tagCreationResponseStr.getJSONObject("data").get("message").equals("Tag created successfully"), "Tag is not created successfully");
            newlyCreatedOrExistingTagId = Integer.parseInt(tagCreationResponseStr.getJSONObject("data").get("id").toString());
        } else {
            if (tagCreationResponseStr.toString().contains("A tag with the given name already exists")) {
                softAssert.assertTrue(tagCreationResponseStr.getJSONObject("data").get("message")
                                .equals("A tag with the given name already exists. Please use the existing tag or create a tag with a different name."),
                        "Tag is not already present");
                HttpResponse getTagOptionsResponse = PreSignatureHelper.verifyWhetherTagIsAlreadyPresent(wordToTag);
                softAssert.assertTrue(getTagOptionsResponse.getStatusLine().getStatusCode() == 200, "Tag Options API Response COde is not valid");
                JSONObject tagOptionsResponseJson = PreSignatureHelper.getJsonObjectForResponse(getTagOptionsResponse);
                int tagsLength = tagOptionsResponseJson.getJSONArray("data").length();
                for (int i = 0; i < tagsLength; i++) {
                    if (tagOptionsResponseJson.getJSONArray("data").getJSONObject(i).get("name").toString().equals(wordToTag)) {
                        newlyCreatedOrExistingTagId = Integer.parseInt(tagOptionsResponseJson.getJSONArray("data").getJSONObject(i).get("id").toString());
                    }
                }
            }
        }

        // Updating clause with tag
        String htmlText = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "clause constant values", "html");
        String[] htmlWordBag = htmlText.split(" ");
        String htmlToTag = htmlWordBag[word];

        // Replacing html with tagged html
        String updateTag = "<span class= \"\" contenteditable=\"true\"><span class=\"tag_" + newlyCreatedOrExistingTagId +
                " tag\" contenteditable=\"false\" htmltagtype=\"1\" htmltagtypeval=\"\"><span style=\"display:none\">${" + newlyCreatedOrExistingTagId + ":</span>" +
                htmlToTag + "<span style=\"display:none\">}</span></span></span>";
        htmlWordBag[word] = updateTag;

        //Altering html text with newly created tag
        StringBuffer sb = new StringBuffer();
        for (String s : htmlWordBag) {
            sb.append(s + " ");
        }

        // Hitting Edit Get API to get Clause text and Clause tag to update
        HttpResponse getCreatedClauseResponse = PreSignatureHelper.getClause(clauseId);
        softAssert.assertTrue(getCreatedClauseResponse.getStatusLine().getStatusCode() == 200, "Error in fetching the new created clause");
        JSONObject createdClauseJson = PreSignatureHelper.getJsonObjectForResponse(getCreatedClauseResponse);
        // Get the json object needed to update to link newly created tag with clause
        // Updating clause tag
        createdClauseJson.getJSONObject("body").getJSONObject("data").getJSONObject("clauseTags").put("values", new JSONArray());
        JSONObject tagValues = new JSONObject();
        tagValues.put("id", newlyCreatedOrExistingTagId);
        tagValues.put("name", newlyCreatedOrExistingTagId);
        JSONObject tagHtmlType = new JSONObject();
        tagHtmlType.put("id", 1);
        tagHtmlType.put("name", "Text Field");
        tagValues.put("tagHTMLType", tagHtmlType);
        createdClauseJson.getJSONObject("body").getJSONObject("data").getJSONObject("clauseTags").getJSONArray("values").put(tagValues);
        // Updating Clause text
        createdClauseJson.getJSONObject("body").getJSONObject("data").getJSONObject("clauseText").getJSONObject("values").put("htmlText", sb.toString().trim());

        // Edit Clause with tags
        String editClausePayload = "{\"body\":{\"data\":" + createdClauseJson.getJSONObject("body").getJSONObject("data").toString() + "}}";
        HttpResponse editClauseAPIResponse = PreSignatureHelper.editClause(editClausePayload);
        softAssert.assertTrue(editClauseAPIResponse.getStatusLine().getStatusCode() == 200, "Error while editing clause");
        JSONObject editedClauseResponseJson = PreSignatureHelper.getJsonObjectForResponse(editClauseAPIResponse);
        softAssert.assertTrue(editedClauseResponseJson.getJSONObject("header").getJSONObject("response").get("status").toString().equals("success"),
                "Tag is not updated with clause");

        // Activate Newly Created Clause
        // Send for client review Action
        PreSignatureHelper.activateEntity(clauseId, softAssert);
        // Approve Action
        PreSignatureHelper.activateEntity(clauseId, softAssert);
        // Publish Action
        PreSignatureHelper.activateEntity(clauseId, softAssert);

        /////////////// Definition Creation ////////////////////
        PreSignatureHelper.setFieldsInConfigForEntities(configFilePath, configFileName, "clauses", "definition fields", 1);
        String definitionResponseString = Definition.createDefinition(null, null, configFilePath, configFileName,
                "definition fields", false);
        int definitionId = PreSignatureHelper.getNewlyCreatedId(definitionResponseString);
        logger.info("Newly Created Definition Id " + definitionId);

        // Get Created definition
        HttpResponse getCreatedDefinitionResponse = PreSignatureHelper.getClause(definitionId);
        softAssert.assertTrue(getCreatedDefinitionResponse.getStatusLine().getStatusCode() == 200, "Error in fetching the new created definition");
        JSONObject createdDefinitionJson = PreSignatureHelper.getJsonObjectForResponse(getCreatedDefinitionResponse);

        // Activate Newly Created Definition
        // Send for client review Action
        PreSignatureHelper.activateEntity(definitionId, softAssert);
        // Approve Action
        PreSignatureHelper.activateEntity(definitionId, softAssert);
        // Publish Action
        PreSignatureHelper.activateEntity(definitionId, softAssert);

        ///////////// Contract Template Creation////////////////
        PreSignatureHelper.setFieldsInConfigForEntities(configFilePath, configFileName, "contract templates", "contract template fields", 0);

        // Setting values for clauses to be selected while creating contract template
        JSONObject clauseCategory = new JSONObject(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "fields",
                "category"));
        JSONObject definitionCategory = new JSONObject(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,
                "definition fields", "definitionCategory"));
        int clauseFieldId = PreSignatureHelper.getFieldId("contract templates", "clauses");
        String selectClausePayload = "{\"name\": \"clauses\",\"id\": " + clauseFieldId + ",\"multiEntitySupport\": false,\"values\": [{\"clauseCategory\": {\"name\": \"" +
                clauseCategory.getJSONObject("values").get("name").toString() + "\",\"id\": \"" + Integer.valueOf(clauseCategory.getJSONObject("values")
                .get("id").toString()) + "\"},\"clause\": {\"name\": \"" + createdClauseJson.getJSONObject("body").getJSONObject("data").getJSONObject("name")
                .get("values") + "\",\"id\": " + clauseId + "},\"clauseGroup\": {\"name\": \"Clause\",\"id\": 1},\"order\": 1,\"mandatory\": null}," +
                "{\"clauseCategory\": {\"name\": \"" + definitionCategory.getJSONObject("values").get("name").toString() + "\",\"id\": \"" +
                Integer.valueOf(definitionCategory.getJSONObject("values").get("id").toString()) + "\"},\"clause\": {\"name\": \"" +
                createdDefinitionJson.getJSONObject("body").getJSONObject("data").getJSONObject("name").get("values") + "\",\"id\": " +
                definitionId + "},\"clauseGroup\": {\"name\": \"Clause\",\"id\": 2},\"order\": 2,\"mandatory\": null}]}";
        try {
            ParseConfigFile.updateValueInConfigFile(configFilePath, configFileName, "contract template fields", "clauses", selectClausePayload);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String contractTemplateResponseString = ContractTemplate.createContractTemplate(null, null, configFilePath, configFileName,
                "contract template fields", false);
        int contractTemplateId = PreSignatureHelper.getNewlyCreatedId(contractTemplateResponseString);
        //Checking for the presence of associated clause and tags with the contract template
        HttpResponse contractTemplateViewerResponse = PreSignatureHelper.getContractTemplateViewerResponse(contractTemplateId);
        JSONObject contractTemplateViewerJson = PreSignatureHelper.getJsonObjectForResponse(contractTemplateViewerResponse);
        //String tagCreated = contractTemplateViewerJson.getJSONObject("clauseTags").getJSONObject("name").toString();
        String clauseTagsCreated = contractTemplateViewerJson.getJSONArray("clauseTags").getJSONObject(0).getString("name");
        softAssert.assertEquals(clauseTagsCreated, wordToTag, "Associated tags found to be different");
        softAssert.assertAll();
    }

    /*
     * Test Case C88444 automation script
     */
    @Test(priority = 17)
    public void testNoTouchContractMetadataInCDR() {
        SoftAssert softAssert = new SoftAssert();
        HttpResponse createNewDraftRequestResponse = PreSignatureHelper.getCreateContractDraftRequestPage();
        softAssert.assertTrue(createNewDraftRequestResponse.getStatusLine().getStatusCode() == 200, "Tag Creation API Response is not valid");
        JSONObject cdrCreationResponseJson = PreSignatureHelper.getJsonObjectForResponse(createNewDraftRequestResponse).getJSONObject("body")
                .getJSONObject("layoutInfo").getJSONObject("layoutComponent").getJSONArray("fields").getJSONObject(0)
                .getJSONArray("fields").getJSONObject(0);

        JSONArray jsonArr = cdrCreationResponseJson.getJSONArray("fields");

        boolean noTouchContractFound = false;

        for (int i = 0; i < jsonArr.length(); i++) {
            if (jsonArr.getJSONObject(i).getString("label").equalsIgnoreCase("No Touch Contract")) {
                noTouchContractFound = true;
                break;
            }
        }

        softAssert.assertTrue(noTouchContractFound, "No Touch Contract Not Found");

        //Now checking No Touch contract in existing CDR ID
        String query = "/listRenderer/list/279/listdata?version=2.0&isFirstCall=true&contractId=&relationId=&vendorId=&_t=1579690671110";
        String payload = "{\"filterMap\":{}}";
        HttpResponse cdrResponse = PreSignatureHelper.getContractDraftRequestID(query, payload);
        JSONObject cdrJsonData = PreSignatureHelper.getJsonObjectForResponse(cdrResponse);
        JSONArray datArray = cdrJsonData.getJSONArray("data");
        int lengthDatAr = datArray.length();
        String CdrID = "";
        if (lengthDatAr > 0) {
            String id = datArray.getJSONObject(0).getJSONObject("12259").getString("value");
            String[] idAr = id.split(";");
            for (String va : idAr) {
                System.out.println(va);
            }
            CdrID = idAr[1];
        } else {
            System.out.println("Contract Draft Request does not exist");
        }
        int cdrIntID = Integer.parseInt(CdrID);
        HttpResponse cdrSpecificResponse = PreSignatureHelper.getContractDraftRequestResponse(cdrIntID);
        JSONObject cdrJsonObj = PreSignatureHelper.getJsonObjectForResponse(cdrSpecificResponse).getJSONObject("body")
                .getJSONObject("layoutInfo").getJSONObject("layoutComponent").getJSONArray("fields").getJSONObject(0)
                .getJSONArray("fields").getJSONObject(1);

        jsonArr = cdrJsonObj.getJSONArray("fields");

        noTouchContractFound = false;

        for (int i = 0; i < jsonArr.length(); i++) {
            if (jsonArr.getJSONObject(i).getString("label").equalsIgnoreCase("No Touch Contract")) {
                noTouchContractFound = true;
                break;
            }
        }

        softAssert.assertTrue(noTouchContractFound, "No Touch Contract Not found");
        softAssert.assertAll();

    }

    /*
     * Test Case C88445 automation script
     */
    @Test(priority = 18)
    public void testBooleanNoTouchContractInCDR() {

        SoftAssert softAssert = new SoftAssert();
        HttpResponse createNewDraftRequestResponse = PreSignatureHelper.getCreateContractDraftRequestPage();
        softAssert.assertTrue(createNewDraftRequestResponse.getStatusLine().getStatusCode() == 200, "Tag Creation API Response is not valid");
        JSONObject cdrCreationResponseJson = PreSignatureHelper.getJsonObjectForResponse(createNewDraftRequestResponse).getJSONObject("body")
                .getJSONObject("layoutInfo").getJSONObject("layoutComponent").getJSONArray("fields").getJSONObject(0)
                .getJSONArray("fields").getJSONObject(0);

        JSONArray jsonArr = cdrCreationResponseJson.getJSONArray("fields");

        boolean checkBoxFound = false;

        for (int i = 0; i < jsonArr.length(); i++) {
            if (jsonArr.getJSONObject(i).getString("type").equalsIgnoreCase("checkbox")) {
                checkBoxFound = true;
                break;
            }
        }

        softAssert.assertTrue(checkBoxFound, "CheckBox not found");
        softAssert.assertAll();

    }

    @Test(priority = 19)
    public void testTagWizard() {

        SoftAssert softAssert = new SoftAssert();
        String wordToTag = "";
        int newlyCreatedOrExistingTagId = 0;
        int contractTemplateId = 0;
        int templateTypeId = 0;
        int cdrDefaultColumnId = 12259;
        String contractTemplateName = "";
        HttpResponse httpResponseCdrs = PreSignatureHelper.getListOfContractDraftRequest();
        JSONObject responseCdrsJson = PreSignatureHelper.getJsonObjectForResponse(httpResponseCdrs);
        String contractDraftRequestIdStr = responseCdrsJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(cdrDefaultColumnId))
                .get("value").toString().split(":;")[1].trim();
        int contractDraftRequestId = Integer.parseInt(contractDraftRequestIdStr);
        HttpResponse findMappedCTAPIResponse = PreSignatureHelper.findMappedContractTemplate(contractDraftRequestId);
        softAssert.assertTrue(findMappedCTAPIResponse.getStatusLine().getStatusCode() == 200, "Find Mapped CT API Response Code is not valid");
        HttpResponse mappedContractTemplateResponse = PreSignatureHelper.findMappedContractTemplate(contractDraftRequestId);
        JSONObject mappedContractTemplateJson = PreSignatureHelper.getJsonObjectForResponse(mappedContractTemplateResponse);
        JSONObject findMappedCTAPIJson = PreSignatureHelper.getJsonObjectForResponse(findMappedCTAPIResponse);
        Object mapStr = findMappedCTAPIJson.getJSONObject("data").get("mappedContractTemplates");
        if (mapStr.equals(null)) {
            System.out.println("Mapped Contract Template not found");
            int contractTemplateNewID = createContractTemplateID(softAssert);
            // Get newly created contract template response
            HttpResponse contractTemplateResponse = PreSignatureHelper.getContractTemplateResponse(contractTemplateNewID);
            JSONObject contractTemplateJson = PreSignatureHelper.getJsonObjectForResponse(contractTemplateResponse);
            templateTypeId = Integer.parseInt(contractTemplateJson.getJSONObject("body").getJSONObject("data").getJSONObject("templateType")
                    .getJSONObject("values").get("id").toString());
            contractTemplateName = contractTemplateJson.getJSONObject("body").getJSONObject("data").getJSONObject("name").get("values").toString();
            HttpResponse contractDraftRequestResponse = PreSignatureHelper.getContractDraftRequestEditPageResponse(contractDraftRequestId);
            JSONObject contractDraftRequestJson = PreSignatureHelper.getJsonObjectForResponse(contractDraftRequestResponse);
            String text = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fields constant values", "text");
            String[] wordTags = text.split(" ");
            String htmlText = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fields constant values", "html");
            String[] htmlWordBag = htmlText.split(" ");
            int word = RandomNumbers.getRandomNumberWithinRangeIndex(0, wordTags.length);
            String htmlToTag = htmlWordBag[word];
            JSONArray attachTemplateArray = new JSONArray("[{\"id\":\"" + contractTemplateId + "\",\"name\":\"" + contractTemplateName +
                    "\",\"hasChildren\":\"false\",\"templateTypeId\":\"" + templateTypeId + "\",\"checked\":1,\"mappedContractTemplates\":null," +
                    "\"uniqueIdentifier\":\"186899071638312\",\"$$hashKey\":\"object:1366\",\"mappedTags\":{\"" + newlyCreatedOrExistingTagId +
                    "\":{\"name\":\"" + htmlToTag + "\",\"id\":" + newlyCreatedOrExistingTagId + ",\"identifier\":\"" + htmlToTag + "\"," +
                    "\"tagHTMLType\":{\"name\":\"Text Field\",\"id\":1},\"orderSeq\":100,\"tagTypeId\":2,\"$$hashKey\":\"object:1371\"}}}]");
            contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("mappedContractTemplates").put("values", attachTemplateArray);
            String contractDraftRequestAttachTemplatePayload = "{\"body\":{\"data\":" + contractDraftRequestJson.getJSONObject("body").getJSONObject("data").toString() + "}}";
            PreSignatureHelper.editContractDraftRequest(contractDraftRequestAttachTemplatePayload);
            HttpResponse getContractDraftRequestResponse = PreSignatureHelper.getContractDraftRequestResponse(contractDraftRequestId);
            softAssert.assertTrue(getContractDraftRequestResponse.getStatusLine().getStatusCode() == 200,
                    "Error in fetching created Contract Draft Request Response");
            logger.info("Created contract template and associated with CDR");
        } else {
            softAssert.assertTrue(findMappedCTAPIJson.getJSONObject("data").getJSONArray("mappedContractTemplates").getJSONObject(0)
                    .get("name").toString().trim().equals(contractTemplateName.trim()), "Not able to see mapped contract template with CDR");
            //fetch tagname and associated tagId
            String mappedTagValue = mappedContractTemplateJson.getJSONObject("data").getJSONArray("mappedContractTemplates").getJSONObject(0)
                    .getJSONObject("mappedTags").toString();
            JSONObject tagNaJso = new JSONObject(mappedTagValue);
            String tagName = "";
            int tagId = 0;
            Set<String> value = tagNaJso.keySet();
            for (String a : value) {
                tagName = tagNaJso.getJSONObject(a).getString("name");
                tagId = tagNaJso.getJSONObject(a).getInt("id");

            }
            wordToTag = tagName;
            newlyCreatedOrExistingTagId = tagId;
            logger.info("Found tag from existing CDR");
        }
        //Check for the presence of tag
        PreSignatureHelper.verifyWhetherTagIsAlreadyPresent(wordToTag);
        String text = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fields constant values", "text");
        String[] wordTags = text.split(" ");
        String htmlText = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fields constant values", "html");
        String[] htmlWordBag = htmlText.split(" ");
        int word = RandomNumbers.getRandomNumberWithinRangeIndex(0, wordTags.length);
        String htmlToTag = htmlWordBag[word];
        HttpResponse contractDraftRequestResponse = PreSignatureHelper.getContractDraftRequestEditPageResponse(contractDraftRequestId);
        JSONObject contractDraftRequestJson = PreSignatureHelper.getJsonObjectForResponse(contractDraftRequestResponse);
        JSONArray attachTemplateArray = new JSONArray("[{\"id\":\"" + contractTemplateId + "\",\"name\":\"" + contractTemplateName +
                "\",\"hasChildren\":\"false\",\"templateTypeId\":\"" + templateTypeId + "\",\"checked\":1,\"mappedContractTemplates\":null," +
                "\"uniqueIdentifier\":\"186899071638312\",\"$$hashKey\":\"object:1366\",\"mappedTags\":{\"" + newlyCreatedOrExistingTagId + "\":{\"name\":\"" +
                htmlToTag + "\",\"id\":" + newlyCreatedOrExistingTagId + ",\"identifier\":\"" + htmlToTag + "\",\"tagHTMLType\":{\"name\":\"Text Field\",\"id\":1}," +
                "\"orderSeq\":100,\"tagTypeId\":2,\"$$hashKey\":\"object:1371\"}}}]");
        contractDraftRequestJson.getJSONObject("body").getJSONObject("data").getJSONObject("mappedContractTemplates").put("values", attachTemplateArray);
        String contractDraftRequestAttachTemplatePayload = "{\"body\":{\"data\":" + contractDraftRequestJson.getJSONObject("body").getJSONObject("data").toString() + "}}";
        // Edit contract draft request with template
        PreSignatureHelper.editContractDraftRequest(contractDraftRequestAttachTemplatePayload);
        // Verify Contract Template Section in Contract Draft Request Page
        HttpResponse getContractDraftRequestResponse = PreSignatureHelper.getContractDraftRequestResponse(contractDraftRequestId);
        softAssert.assertTrue(getContractDraftRequestResponse.getStatusLine().getStatusCode() == 200,
                "Error in fetching created Contract Draft Request Response");
        softAssert.assertAll();

    }

    private int createDefinitionID(SoftAssert softAssert) {

        /////////////// Definition Creation ////////////////////
        PreSignatureHelper.setFieldsInConfigForEntities(configFilePath, configFileName, "clauses", "definition fields", 1);
        String definitionResponseString = Definition.createDefinition(null, null, configFilePath, configFileName,
                "definition fields", false);
        int definitionId = PreSignatureHelper.getNewlyCreatedId(definitionResponseString);
        logger.info("Newly Created Definition Id " + definitionId);

        // Get Created definition
        HttpResponse getCreatedDefinitionResponse = PreSignatureHelper.getClause(definitionId);
        softAssert.assertTrue(getCreatedDefinitionResponse.getStatusLine().getStatusCode() == 200, "Error in fetching the new created definition");

        return definitionId;
    }

    private int createContractTemplateID(SoftAssert softAssert) {
        /////// Clause Creation ////////
        PreSignatureHelper.setFieldsInConfigForEntities(configFilePath, configFileName, "clauses", "fields", 0);

        // Create new Clause with tags
        String createdClauseId = Clause.createClause(null, null, configFilePath, configFileName, "fields", false);
        int clauseId = PreSignatureHelper.getNewlyCreatedId(createdClauseId);
        logger.info("Newly Created Clause Id " + clauseId);

        if (clauseId == -1) {
            throw new SkipException("Error in Creating Clause");
        }

        // Get text for which tag has to be created
        String text = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fields constant values", "text");
        String[] wordTags = text.split(" ");
        int word = RandomNumbers.getRandomNumberWithinRangeIndex(0, wordTags.length);
        String wordToTag = wordTags[word];

        //Creating the new tag
        String tagCreationPayload = "{\"name\":\"" + wordToTag + "\",\"tagHTMLType\":{\"id\":1}}";
        HttpResponse createCreationResponse = PreSignatureHelper.createTag(tagCreationPayload);
        softAssert.assertTrue(createCreationResponse.getStatusLine().getStatusCode() == 200, "Tag Creation API Response is not valid");
        JSONObject tagCreationResponseStr = PreSignatureHelper.getJsonObjectForResponse(createCreationResponse);
        int newlyCreatedOrExistingTagId = 0;
        if (tagCreationResponseStr.toString().contains("Tag created successfully")) {
            softAssert.assertTrue(tagCreationResponseStr.getJSONObject("data").get("message").equals("Tag created successfully"), "Tag is not created successfully");
            newlyCreatedOrExistingTagId = Integer.parseInt(tagCreationResponseStr.getJSONObject("data").get("id").toString());
        } else {
            if (tagCreationResponseStr.toString().contains("A tag with the given name already exists")) {
                softAssert.assertTrue(tagCreationResponseStr.getJSONObject("data").get("message")
                                .equals("A tag with the given name already exists. Please use the existing tag or create a tag with a different name."),
                        "Tag is not already present");
                HttpResponse getTagOptionsResponse = PreSignatureHelper.verifyWhetherTagIsAlreadyPresent(wordToTag);
                softAssert.assertTrue(getTagOptionsResponse.getStatusLine().getStatusCode() == 200, "Tag Options API Response COde is not valid");
                JSONObject tagOptionsResponseJson = PreSignatureHelper.getJsonObjectForResponse(getTagOptionsResponse);
                int tagsLength = tagOptionsResponseJson.getJSONArray("data").length();
                for (int i = 0; i < tagsLength; i++) {
                    if (tagOptionsResponseJson.getJSONArray("data").getJSONObject(i).get("name").toString().equals(wordToTag)) {
                        newlyCreatedOrExistingTagId = Integer.parseInt(tagOptionsResponseJson.getJSONArray("data").getJSONObject(i).get("id").toString());
                    }
                }
            }
        }

        // Updating clause with tag
        String htmlText = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fields constant values", "html");
        String[] htmlWordBag = htmlText.split(" ");
        String htmlToTag = htmlWordBag[word];

        // Replacing html with tagged html
        String updateTag = "<span class= \"\" contenteditable=\"true\"><span class=\"tag_" + newlyCreatedOrExistingTagId +
                " tag\" contenteditable=\"false\" htmltagtype=\"1\" htmltagtypeval=\"\"><span style=\"display:none\">${" + newlyCreatedOrExistingTagId +
                ":</span>" + htmlToTag + "<span style=\"display:none\">}</span></span></span>";
        htmlWordBag[word] = updateTag;

        //Altering html text with newly created tag
        StringBuffer sb = new StringBuffer();
        for (String s : htmlWordBag) {
            sb.append(s + " ");
        }

        // Hitting Edit Get API to get Clause text and Clause tag to update
        HttpResponse getCreatedClauseResponse = PreSignatureHelper.getClause(clauseId);
        softAssert.assertTrue(getCreatedClauseResponse.getStatusLine().getStatusCode() == 200, "Error in fetching the new created clause");
        JSONObject createdClauseJson = PreSignatureHelper.getJsonObjectForResponse(getCreatedClauseResponse);
        // Get the json object needed to update to link newly created tag with clause
        // Updating clause tag
        createdClauseJson.getJSONObject("body").getJSONObject("data").getJSONObject("clauseTags").put("values", new JSONArray());
        JSONObject tagValues = new JSONObject();
        tagValues.put("id", newlyCreatedOrExistingTagId);
        tagValues.put("name", newlyCreatedOrExistingTagId);
        JSONObject tagHtmlType = new JSONObject();
        tagHtmlType.put("id", 1);
        tagHtmlType.put("name", "Text Field");
        tagValues.put("tagHTMLType", tagHtmlType);
        createdClauseJson.getJSONObject("body").getJSONObject("data").getJSONObject("clauseTags").getJSONArray("values").put(tagValues);
        // Updating Clause text
        createdClauseJson.getJSONObject("body").getJSONObject("data").getJSONObject("clauseText").getJSONObject("values").put("htmlText", sb.toString().trim());

        // Edit Clause with tags
        String editClausePayload = "{\"body\":{\"data\":" + createdClauseJson.getJSONObject("body").getJSONObject("data").toString() + "}}";
        HttpResponse editClauseAPIResponse = PreSignatureHelper.editClause(editClausePayload);
        softAssert.assertTrue(editClauseAPIResponse.getStatusLine().getStatusCode() == 200, "Error while editing clause");
        JSONObject editedClauseResponseJson = PreSignatureHelper.getJsonObjectForResponse(editClauseAPIResponse);
        softAssert.assertTrue(editedClauseResponseJson.getJSONObject("header").getJSONObject("response").get("status").toString().equals("success"),
                "Tag is not updated with clause");

        // Activate Newly Created Clause
        // Send for client review Action
        PreSignatureHelper.activateEntity(clauseId, softAssert);
        // Approve Action
        PreSignatureHelper.activateEntity(clauseId, softAssert);
        // Publish Action
        PreSignatureHelper.activateEntity(clauseId, softAssert);

        /////////////// Definition Creation ////////////////////
        PreSignatureHelper.setFieldsInConfigForEntities(configFilePath, configFileName, "clauses", "definition fields", 1);
        String definitionResponseString = Definition.createDefinition(null, null, configFilePath, configFileName,
                "definition fields", false);
        int definitionId = PreSignatureHelper.getNewlyCreatedId(definitionResponseString);
        logger.info("Newly Created Definition Id " + definitionId);

        // Get Created definition
        HttpResponse getCreatedDefinitionResponse = PreSignatureHelper.getClause(definitionId);
        softAssert.assertTrue(getCreatedDefinitionResponse.getStatusLine().getStatusCode() == 200, "Error in fetching the new created definition");
        JSONObject createdDefinitionJson = PreSignatureHelper.getJsonObjectForResponse(getCreatedDefinitionResponse);

        // Activate Newly Created Definition
        // Send for client review Action
        PreSignatureHelper.activateEntity(definitionId, softAssert);
        // Approve Action
        PreSignatureHelper.activateEntity(definitionId, softAssert);
        // Publish Action
        PreSignatureHelper.activateEntity(definitionId, softAssert);

        ///////////// Contract Template Creation////////////////
        PreSignatureHelper.setFieldsInConfigForEntities(configFilePath, configFileName, "contract templates", "contract template fields", 0);

        // Setting values for clauses to be selected while creating contract template
        JSONObject clauseCategory = new JSONObject(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName, "fields",
                "category"));
        JSONObject definitionCategory = new JSONObject(ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath, configFileName,
                "definition fields", "definitionCategory"));
        int clauseFieldId = PreSignatureHelper.getFieldId("contract templates", "clauses");
        String selectClausePayload = "{\"name\": \"clauses\",\"id\": " + clauseFieldId + ",\"multiEntitySupport\": false,\"values\": [{\"clauseCategory\": {\"name\": \"" +
                clauseCategory.getJSONObject("values").get("name").toString() + "\",\"id\": \"" + Integer.valueOf(clauseCategory.getJSONObject("values")
                .get("id").toString()) + "\"},\"clause\": {\"name\": \"" + createdClauseJson.getJSONObject("body").getJSONObject("data").getJSONObject("name").get("values")
                + "\",\"id\": " + clauseId + "},\"clauseGroup\": {\"name\": \"Clause\",\"id\": 1},\"order\": 1,\"mandatory\": null},{\"clauseCategory\": {\"name\": \""
                + definitionCategory.getJSONObject("values").get("name").toString() + "\",\"id\": \"" + Integer.valueOf(definitionCategory.getJSONObject("values")
                .get("id").toString()) + "\"},\"clause\": {\"name\": \"" + createdDefinitionJson.getJSONObject("body").getJSONObject("data").getJSONObject("name")
                .get("values") + "\",\"id\": " + definitionId + "},\"clauseGroup\": {\"name\": \"Clause\",\"id\": 2},\"order\": 2,\"mandatory\": null}]}";
        try {
            ParseConfigFile.updateValueInConfigFile(configFilePath, configFileName, "contract template fields", "clauses", selectClausePayload);
        } catch (Exception e) {
            e.printStackTrace();
        }

        String contractTemplateResponseString = ContractTemplate.createContractTemplate(null, null, configFilePath, configFileName,
                "contract template fields", false);
        return PreSignatureHelper.getNewlyCreatedId(contractTemplateResponseString);
    }

    @AfterClass(alwaysRun = true)
    public void afterClass() {
        check.hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));
    }
}
