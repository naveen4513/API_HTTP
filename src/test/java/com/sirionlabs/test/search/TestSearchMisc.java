package com.sirionlabs.test.search;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.metadataSearch.MetadataSearch;
import com.sirionlabs.api.metadataSearch.MetadataSearchDownload;
import com.sirionlabs.api.metadataSearch.Search;
import com.sirionlabs.api.search.ContractDocDownload;
import com.sirionlabs.api.search.SearchAttachment;
import com.sirionlabs.api.search.SearchContractDoc;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.dbHelper.EmailActionDbHelper;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import java.util.*;
import java.util.stream.Collectors;
import static com.sirionlabs.api.search.EntityId.*;
import static com.sirionlabs.api.search.SearchFilter.*;
import static com.sirionlabs.api.workflowRoleGroupFlowDown.WorkflowRoleGroupFlowDownCreate.getCreateResponse;
import static com.sirionlabs.api.workflowRoleGroupFlowDown.WorkflowRoleGroupFlowDownCreate.getPayloadFlowDownRoleGroup;


/**
 * Created by nitun.pachauri on 16/04/2020.
 */
@Listeners(value = MyTestListenerAdapter.class)
public class TestSearchMisc extends TestRailBase {

    private final static Logger logger = LoggerFactory.getLogger(TestSearchMisc.class);
    private static String configFilePath = null;
    private static String configFileName = null;
    private String expectedRoleGroupValue;
    private int parentEntityId;
    private int parentRoleGroupId;
    private String stakeholderName, roleGroupName, parentEntityTypeId;
    private String stakeHolderId, entityName;
    private int entityTypeId = 61;
    private MetadataSearch searchObj = new MetadataSearch();

    @BeforeClass()
    public void beforeClass() throws ConfigurationException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("MicroserviceConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("MicroserviceConfigFileName");
    }

    // C91123 C91124 C91125 C91126
    @Test(enabled = true)
    public void testSearchFilterFnS() {
        CustomAssert csAssert = new CustomAssert();

        String functions= ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "search", "functions");
        String services = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "search", "services");

        List<String> functionList = Arrays.stream(functions.split("\\,")).collect(Collectors.toList());
        List<String> servicesList = Arrays.stream(services.split("\\,")).collect(Collectors.toList());

        //  Hitting the API
        APIResponse response = getSearchFilter();

        JSONObject jsonObj = new JSONObject(response.getResponseBody());
        JSONArray funcJsonArr = jsonObj.getJSONObject("filterDataMap").getJSONObject("17").getJSONObject("multiselectValues").getJSONObject("OPTIONS").getJSONArray("DATA");
        JSONArray servJsonArr = jsonObj.getJSONObject("filterDataMap").getJSONObject("18").getJSONObject("multiselectValues").getJSONObject("OPTIONS").getJSONArray("data");

        List<String> actualFunctionsList = getList(funcJsonArr);
        List<String> actualServiceList = getList(servJsonArr);

        csAssert.assertEquals(actualFunctionsList, functionList);
        csAssert.assertEquals(actualServiceList, servicesList);
        csAssert.assertAll();
    }

    //C90466 C90467
    @Test(enabled = true)
    public void testFlowDownInMetaDataSearch() {

        CustomAssert csAssert = new CustomAssert();
        for(int k = 0 ; k < 2; k++) {
            entityTypeId = 18;

            // Hit the show API for selected parent and save the role group under test with its value
            parentEntityId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "actionsearchflowdown", "parententityid").split("\\,")[k]);
            parentRoleGroupId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "actionsearchflowdown", "parentrolegroupid").split("\\,")[k]);
            roleGroupName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "actionsearchflowdown", "rolegroupname").split("\\,")[k];
            parentEntityTypeId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "actionsearchflowdown", "parententitytypeid").split("\\,")[k];
            entityName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "actionsearchflowdown", "entityname").split("\\,")[k];

            // Fetch the expected Stakeholder value
            Show show = new Show();
            show.hitShow(Integer.parseInt(parentEntityTypeId), parentEntityId);
            String response = show.getShowJsonStr();
            JSONObject obj = new JSONObject(response);
            JSONObject obj2 = obj.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values").getJSONObject("rg_" + parentRoleGroupId + "");
            JSONArray objArr = obj2.getJSONArray("values");
            expectedRoleGroupValue = objArr.getJSONObject(0).get("name").toString();

            // Hit the flow down API to ensure the role group has been inherited by the child entity
            boolean flowDownAPI = hitFlowDownAPI(new String[]{parentEntityTypeId}, new String[]{String.valueOf(entityTypeId)}, new String[]{String.valueOf(parentRoleGroupId)}, new String[]{"true"}, new String[]{"false"});
            if (!flowDownAPI) {
                throw new SkipException("Flow Down API didn't work");
            }

            // Hit the MetaData Search API to check if the flowndown role group is appearing for CE.
            String searchResponse = searchObj.hitMetadataSearch(entityTypeId);
            csAssert.assertEquals(isRoleGroupFound(searchResponse, roleGroupName), true, "Supplier role group is not found in MetaDataSearch for Actions");

            // Make the Payload for Search API
            Map<String, String> field = ParseJsonResponse.getFieldByLabel(searchResponse, roleGroupName.toLowerCase());
            String payload = TestSearchMetadata.getPayload(field, expectedRoleGroupValue, entityTypeId, field.get("name"), Integer.parseInt(field.get("id")));

            // Hit the MetaData API with specific stakeholders and verify the result
            Search searchObj = new Search();
            logger.info("Hitting Search Api for entityTypeId {} and roleGroup {}", parentEntityTypeId, roleGroupName);
            searchResponse = searchObj.hitSearch(entityTypeId, payload);

            // Verify if the Data in the response has the apt stakeholder
            validateRecords(searchResponse, expectedRoleGroupValue, csAssert);


            try {
                // Editing the parent contract's stakeholder
                stakeholderName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "issuesflowdown", "changedusername");
                stakeHolderId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "issuesflowdown", "changeduserid");
                editEntityWithSpecificStakeholder(entityName, parentEntityId, stakeholderName, stakeHolderId);

                // Fetch the expected Stakeholder value
                show = new Show();
                show.hitShow(Integer.parseInt(parentEntityTypeId), parentEntityId);
                response = show.getShowJsonStr();
                obj = new JSONObject(response);
                obj2 = obj.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values").getJSONObject("rg_" + parentRoleGroupId + "");
                objArr = obj2.getJSONArray("values");
                expectedRoleGroupValue = objArr.getJSONObject(0).get("name").toString();

                // Hit the MetaData Search API to check if the flowndown role group is appearing for CE.
                searchResponse = new MetadataSearch().hitMetadataSearch(entityTypeId);

                // Make the Payload for Search API
                field = ParseJsonResponse.getFieldByLabel(searchResponse, roleGroupName.toLowerCase());
                payload = TestSearchMetadata.getPayload(field, expectedRoleGroupValue, entityTypeId, field.get("name"), Integer.parseInt(field.get("id")));

                // Hit the MetaData API with specific stakeholders and verify the result
                searchObj = new Search();
                logger.info("Hitting Search Api for entityTypeId {} and roleGroup {}", parentEntityTypeId, roleGroupName);
                searchResponse = searchObj.hitSearch(entityTypeId, payload);

                // Verify if the Data in the response has the apt stakeholder
                validateRecords(searchResponse, expectedRoleGroupValue, csAssert);

            } catch (Exception e) {
                csAssert.fail("Something went wrong during edit parent entity check");
            } finally {
                // Restoring the Stakeholder in the parent entity
                stakeholderName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "actionsflowdown", "originalusername");
                stakeHolderId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "actionsflowdown", "originaluserid");
                editEntityWithSpecificStakeholder(entityName, parentEntityId, stakeholderName, stakeHolderId);
            }

            // Deleting the flow Down Link Between the PPE and CE
            flowDownAPI = hitFlowDownAPI(new String[]{parentEntityTypeId}, new String[]{String.valueOf(entityTypeId)}, new String[]{String.valueOf(parentRoleGroupId)}, new String[]{"false"}, new String[]{"true"});
            if (!flowDownAPI) {
                throw new SkipException("Flow Down API didn't work");
            }

            // Hit the MetaData Search API to check if the flowndown role group is appearing for CE.
            searchResponse = new MetadataSearch().hitMetadataSearch(entityTypeId);
            csAssert.assertEquals(isRoleGroupFound(searchResponse, roleGroupName), false, "Supplier role group is not found in MetaDataSearch for Actions");

            //Restoring the flow down
            // Hit the flow down API to ensure the role group has been inherited by the child entity
            flowDownAPI = hitFlowDownAPI(new String[]{parentEntityTypeId}, new String[]{String.valueOf(entityTypeId)}, new String[]{String.valueOf(parentRoleGroupId)}, new String[]{"true"}, new String[]{"false"});
            if (!flowDownAPI) {
                throw new SkipException("Flow Down API didn't work");
            }
        }
        csAssert.assertAll();
    }

    //C90857
    @Test(enabled = true)
    public void testC90857() {

        CustomAssert csAssert = new CustomAssert();


        // Hit the show API for selected parent and save the role group under test with its value
        int   seqId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "searchmisc", "othercontractseqid"));
        int otherContractEntityTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "searchmisc", "othercontractentitytypeid"));
        int otherEntityId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "searchmisc", "otherentityid"));
        String expectedTitle = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "searchmisc", "title");

        APIResponse response = getEntityTypeId(seqId, otherContractEntityTypeId);

        csAssert.assertEquals(response.getResponseBody().toString(), String.valueOf(otherEntityId));
        csAssert.assertEquals(response.getResponseCode().toString(), "200");

        Show show = new Show();
        show.hitShow(61, Integer.parseInt(response.getResponseBody().toString()));
        JSONObject obj = new JSONObject(show.getShowJsonStr());

        String url = obj.getJSONObject("header").getString("source");
        csAssert.assertEquals("/contracts/show/"+otherEntityId+"", url, "URL didn't match for Other Contract");

        String title = obj.getJSONObject("body").getJSONObject("data").getJSONObject("name").get("values").toString();
        csAssert.assertEquals(title, expectedTitle, "Title didn't match for Other Contract");

        csAssert.assertAll();
    }

    //C90865
    @Test(enabled = true)
    public void testC90865() throws ConfigurationException {

        CustomAssert csAssert = new CustomAssert();
        entityTypeId = 61;

        // Make the Payload for Search API
        String searchResponse = searchObj.hitMetadataSearch(entityTypeId);
        Map<String, String> field = ParseJsonResponse.getFieldByLabel(searchResponse, "document Type");
        String payload = TestSearchMetadata.getPayload(field, "Other", entityTypeId, "contracts", "76");
        payload = payload.replaceAll("\"size\",\"values\":5","\"size\",\"values\":100" );

        // Hit the MetaData API for entity Contract and Document Type Other
        Search searchObj = new Search();
        logger.info("Hitting Search Api for entityTypeId ; {} and Document Type : Other {}", parentEntityTypeId);
        searchResponse = searchObj.hitSearch(entityTypeId, payload);

        // Verify if the Data in the response has the apt stakeholder
        validateOtherContract(searchResponse, csAssert);

        csAssert.assertAll();
    }

    //C90864
    @Test(enabled = true)
    public void testC90864() throws ConfigurationException {

        CustomAssert csAssert = new CustomAssert();
        entityTypeId = 61;

        // Make the Payload for Search API
        String searchResponse = searchObj.hitMetadataSearch(entityTypeId);
        Map<String, String> field = ParseJsonResponse.getFieldByLabel(searchResponse, "document Type");
        String payload = TestSearchMetadata.getPayload(field, "Other", entityTypeId, "contracts", "76");
        payload = payload.replaceAll("\"size\",\"values\":5","\"size\",\"values\":100" );

        // Hit the MetaData API for entity Contract and Document Type Other
        Search searchObj = new Search();
        logger.info("Hitting Search Api for entityTypeId ; {} and Document Type : Other {}", parentEntityTypeId);
        searchResponse = searchObj.hitSearch(entityTypeId, payload);

        // Verify if the Data in the response matches the excel that can be downloaded
        validateOtherContractExcel(searchResponse, csAssert);

        csAssert.assertAll();
    }

    //C90859
    @Test(enabled = true)
    public void testC90859() throws ConfigurationException {

        CustomAssert csAssert = new CustomAssert();
        int limit = 5;
        int offset = 0;

        HashMap<String , String> map = new HashMap<>();
        map.put("supplierIds","4584");
        map.put("functionIds","1002");
        map.put("serviceIds","1003");
        map.put("regionIds","1002");

        String keyword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "searchmisc", "keyword");

        // Hitting the Search API
        logger.info("Hitting SearchContractDoc Api for Keyword [{}]", keyword);
        SearchContractDoc docTreeObj = new SearchContractDoc();
        docTreeObj.hitSearchContractDoc(keyword, limit, offset, map);
        String docTreeJsonStr = docTreeObj.getContractDocJsonStr();

        JSONObject obj = new JSONObject(docTreeJsonStr);

        // Downloading the excel file
        logger.info("Downloading Excel for Multi Supplier Contract.");
        String filePath = "src/test";
        String fileName = "SearchResultsOtherContractDocument.xlsx";
        String downloadFile = filePath + "/" + fileName;
        ContractDocDownload downloadObj = new ContractDocDownload();
        HttpResponse downloadResponse= downloadObj.downloadContractDocResultsFile(downloadFile, keyword, limit, map);
        List<List<String>> xlList = XLSUtils.getExcelDataOfMultipleRows(filePath, fileName, "Data",4, new Long(XLSUtils.getNoOfRows(filePath, fileName, "Data")).intValue());

        int loop = obj.getInt("filteredCount");
        for(int i = 0 ; i < loop ; i++) {
            JSONObject newobj = new JSONObject(obj.getJSONArray("searchResults").get(i).toString());

            String actualText =    newobj.getJSONArray("highlightedSnippets").get(0).toString();
            actualText = actualText.substring(3,actualText.length()-4);

            String attachmentName = newobj.getString("documentName");
            String supplierName = newobj.getJSONArray("relationNames").get(0).toString();
            String contractName = newobj.getString("contractName");
            String supplierSeqId= "SP04394";
            String contractSeqId = newobj.get("contractClientSeqId").toString();
            String pageNum = newobj.get("pageNum").toString();

            if (!xlList.isEmpty()) {
                try {
                    for(List<String> item: xlList) {
                        String actualpageNum = item.get(5);
                        String actualContractSeqId = item.get(4);
                        if(actualpageNum.equals(pageNum) && actualContractSeqId.contains(contractSeqId)){
                            csAssert.assertEquals(item.get(0), attachmentName, "Actual Value : "+item.get(0)+", Expected Value : "+attachmentName+"");
                            csAssert.assertEquals(item.get(1), supplierName, "Actual Value : "+item.get(1)+", Expected Value : "+supplierName+"");
                            csAssert.assertEquals(item.get(3), contractName, "Actual Value : "+item.get(3)+", Expected Value : "+contractName+"");
                            csAssert.assertEquals(item.get(2), supplierSeqId, "Actual Value : "+item.get(2)+", Expected Value : "+supplierSeqId+"");
                            break;
                        }
                    }
                } catch (Exception e) {
                    logger.error("Exception while Verifying Other Contract Data", e.getMessage());
                }
            }

            csAssert.assertEquals(actualText,keyword, "Highlighted text doesn't match");
            csAssert.assertTrue(newobj.getString("documentName").contains(keyword),"Keyword doesn't match the filename");


        }
        csAssert.assertAll();

    }

    //C90862
    @Test(enabled = true)
    public void testC90862() throws ConfigurationException {

        CustomAssert csAssert = new CustomAssert();

        String statusJson = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "c90862", "statusjson");
        String titleJson = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "c90862", "titlejson");
        String expectedStatus = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "c90862", "status");
        String expectedTitle = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "c90862", "title");

        // Make the Payload for Search API
        String searchResponse = searchObj.hitMetadataSearch(61);
        Map<String, String> field = ParseJsonResponse.getFieldByLabel(searchResponse, "document Type");
        String payload = TestSearchMetadata.getPayload(field, "Other", entityTypeId, "contracts", "76");
        payload = payload.substring(0,payload.lastIndexOf("}}}"))+", \"status\": "+statusJson+", \"title\": "+titleJson+"}}}";

        // Hit the MetaData API for entity Contract and Document Type Other
        Search searchObj = new Search();
        logger.info("Hitting Search Api for entityTypeId ; {} and Document Type : Other", entityTypeId);
        searchResponse = searchObj.hitSearch(entityTypeId, payload);

        // Verify response Data
        validateC90862(searchResponse, expectedStatus, expectedTitle, csAssert);

        csAssert.assertAll();

    }

    //C90866
    @Test(enabled = true)
    public void testC90866() throws ConfigurationException {

        CustomAssert csAssert = new CustomAssert();

        // Hit the flow down API to ensure the role group has been inherited by the child entity
        boolean flowDownAPI = hitFlowDownAPI(new String[]{"1"}, new String[]{"61"}, new String[]{"2000"}, new String[]{"true"}, new String[]{"false"});
        if (!flowDownAPI)  {
            throw new SkipException("Flow Down API didn't work");
        }

        // Make the Payload for Search API
        String searchResponse = searchObj.hitMetadataSearch(61);
        Map<String, String> field = ParseJsonResponse.getFieldByLabel(searchResponse, "document Type");
        String payload = TestSearchMetadata.getPayload(field, "Other", entityTypeId, "contracts", "76");
        payload = payload.replaceAll("\"size\",\"values\":5","\"size\",\"values\":100" );

        // Hit the MetaData API for entity Contract and Document Type Other
        Search searchObj = new Search();
        logger.info("Hitting Search Api for entityTypeId ; {} and Document Type : Other", entityTypeId);
        searchResponse = searchObj.hitSearch(entityTypeId, payload);

        // Verify response Data
        validateC90862(searchResponse, csAssert);

        csAssert.assertAll();

    }

    //C90867
    @Test(enabled = true)
    public void testC90867() throws ConfigurationException {

        CustomAssert csAssert = new CustomAssert();

        String russianLanguage = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "email", "russianlanguage");
        String defaultUserEmail  = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "email", "defaultuserEmail");
        String englishLanguage = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "email", "englishlanguage");

        // Changing the Language to Russian
        boolean languageChanged;
        Check checkObj;
        try {
            languageChanged = EmailActionDbHelper.updateUserLanguage(defaultUserEmail, russianLanguage);

            // Refreshing the session
            checkObj = new Check();
            checkObj.hitCheck("anay_user", "admin1234a");

            if (languageChanged) {

                // Make the Payload for Search API
                String searchResponse = searchObj.hitMetadataSearch(61);

                JSONObject obj = new JSONObject(searchResponse);
                JSONArray arr = obj.getJSONObject("body").getJSONObject("data").getJSONObject("documentType").getJSONObject("options").getJSONArray("data");

                for (int i = 0; i < arr.length(); i++) {
                    if (arr.getJSONObject(i).getInt("id") == 76) {
                        csAssert.assertEquals(arr.getJSONObject(i).getString("name"), "Russian Other", "Other text does change to Russian.");
                    }
                }
            }
        } catch(Exception e){
            EmailActionDbHelper.updateUserLanguage(defaultUserEmail, englishLanguage);
            csAssert.fail("Language change didn't work");
        }

        // Changing the Language to English
        languageChanged = EmailActionDbHelper.updateUserLanguage(defaultUserEmail, englishLanguage);

        // Refreshing the session
        checkObj = new Check();
        checkObj.hitCheck("anay_user", "admin1234a");

        if(languageChanged) {

            // Make the Payload for Search API
            String searchResponse = searchObj.hitMetadataSearch(61);

            JSONObject obj = new JSONObject(searchResponse);
            JSONArray arr = obj.getJSONObject("body").getJSONObject("data").getJSONObject("documentType").getJSONObject("options").getJSONArray("data");

            for (int i = 0; i < arr.length(); i++) {
                if (arr.getJSONObject(i).getInt("id") == 76) {
                    csAssert.assertEquals(arr.getJSONObject(i).getString("name"), "Other", "Other text does not change to Russian.");
                }
            }
        }

        csAssert.assertAll();

    }

    //C90870
    @Test(enabled = true)
    public void testC90870() throws ConfigurationException {

        CustomAssert csAssert = new CustomAssert();
        String contractName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "c90870", "contractname");
        String supplierName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "c90870", "suppliername");
        String url = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "c90870", "url");
        String queryText = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "c90870", "querytext");
        String contractId = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "c90870", "contractid");

        entityTypeId = 61;
        Map<String, String> filtersMap = new HashMap<>();
        filtersMap.put("contractIds", contractId);

        SearchAttachment attachObj = new SearchAttachment();
        attachObj.hitAttachment(queryText, entityTypeId, 5, 0, filtersMap);
        JSONObject obj = new JSONObject(attachObj.getAttachmentJsonStr());
        JSONObject firstObject = obj.getJSONArray("searchResults").getJSONObject(0);

        String actualContractName = firstObject.getString("contractName");
        String actualSupplierName = firstObject.getJSONArray("relationNames").getString(0);
        String actualEntityURL = firstObject.getString("entityURL");
        String actualDocumentName = firstObject.getString("documentName");

        csAssert.assertEquals(actualContractName, contractName, "Actual : "+actualContractName+" | Expected : "+contractName+"");
        csAssert.assertEquals(actualSupplierName, supplierName, "Actual : "+actualSupplierName+" | Expected : "+supplierName+"");
        csAssert.assertEquals(actualEntityURL, url, "Actual : "+actualEntityURL+" | Expected : "+url+"");
        csAssert.assertTrue(actualDocumentName.contains(queryText), "query text not found in document name");


        csAssert.assertAll();

    }



    private List<String> getList(JSONArray arr) {
        List<String> newList = new ArrayList<>();
        for(int i = 0 ; i < arr.length() ; i++) {
            newList.add(arr.getJSONObject(i).get("name").toString());
        }
        return newList;
    }

    private boolean isRoleGroupFound(String searchResponse, String roleGroupName){
        JSONObject obj = new JSONObject(searchResponse);
        JSONObject obj2 = obj.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values");
        boolean isFound = false;
        Object[] arr = obj2.keySet().toArray();
        for(int i = 0 ; i <arr.length ; i++) {
            if(obj2.getJSONObject(arr[i].toString()).get("label").equals(roleGroupName)) return true;
        }
        return isFound;
    }

    private boolean hitFlowDownAPI(String[] parentEntityTypeIdArr ,String[] childEntityTypeIdArr, String[] roleGroupIdArr, String[] flowEnabled, String[] deletedArr){
        // Hit the flow down API to ensure the role group has been inherited by the child entity
        String payload = getPayloadFlowDownRoleGroup(parentEntityTypeIdArr, childEntityTypeIdArr, roleGroupIdArr, flowEnabled, deletedArr);
        APIResponse apiResponse = getCreateResponse(payload);
        String message = apiResponse.getResponseBody();
        if (message.contains("success")) {
            return true;
        }
        return false;
    }

    private void validateRecords(String searchResponse, String expectedRoleGroupValue, CustomAssert csAssert){

        JSONObject obj = new JSONObject(searchResponse);
        JSONArray arr = obj.getJSONObject("body").getJSONObject("data").getJSONObject("searchResults").getJSONObject("values").getJSONArray("data");
        ArrayList<Integer> list = new ArrayList<Integer>();
        for(int i = 0 ; i <arr.length() ;  i++){
            JSONObject obj2 = arr.getJSONObject(i);
            Object[] set = obj2.keySet().toArray();
            for(int j = 0 ; j < obj2.length(); j++){
                if(obj2.getJSONObject(set[j].toString()).get("columnName").equals("id")) {
                    String temp =  obj2.getJSONObject(set[j].toString()).get("value").toString();
                    temp = temp.substring(temp.indexOf(';')+1);
                    list.add(Integer.parseInt(temp));
                }
            }
        }

        for(Integer item : list) {
            Show show = new Show();
            show.hitShow(entityTypeId, item);
            String showJsonStr = show.getShowJsonStr();
            obj = new JSONObject(showJsonStr);
            JSONObject obj2 = obj.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values");
            Object[] arr2 = obj2.keySet().toArray();
            for (int i = 0; i < arr2.length; i++) {
                if (obj2.getJSONObject(arr2[i].toString()).get("label").equals(roleGroupName)) {
                    String actualStakeholderName = new JSONObject(obj2.getJSONObject(arr2[i].toString()).getJSONArray("values").get(0).toString()).get("name").toString();
                    csAssert.assertEquals(actualStakeholderName, expectedRoleGroupValue, "Stakeholder "+roleGroupName+" didn't match for entityTypeId = "+entityTypeId+", entityId = "+item+", expectedStakeholder= "+expectedRoleGroupValue+", actualStakeholder= "+actualStakeholderName+"");
                }
            }
        }
    }

    private void validateOtherContract(String searchResponse, CustomAssert csAssert){

        String title="", supplier="", docType="", regions="", services="";
        int entityId = 0;

        JSONObject obj = new JSONObject(searchResponse);
        JSONArray arr = obj.getJSONObject("body").getJSONObject("data").getJSONObject("searchResults").getJSONObject("values").getJSONArray("data");
        ArrayList<Integer> list = new ArrayList<Integer>();
        for(int i = 0 ; i <arr.length() ;  i++){
            JSONObject obj2 = arr.getJSONObject(i);

            Object[] set = obj2.keySet().toArray();
            for(int j = 0 ; j < obj2.length(); j++) {
                int temp2 = obj2.getJSONObject(set[j].toString()).getInt("columnId");

                // Id check
                if (obj2.getJSONObject(set[j].toString()).get("columnName").equals("id")) {
                    String temp = obj2.getJSONObject(set[j].toString()).get("value").toString();
                    csAssert.assertEquals("FO", temp.split(":;")[2]);
                    entityId = Integer.parseInt(temp.split(":;")[1]);
                }

                // Title check
                if (obj2.getJSONObject(set[j].toString()).get("columnName").equals("documenttitle")) {
                    title = obj2.getJSONObject(set[j].toString()).get("value").toString();
                    title = title.split(":;")[0];
                }

                // Supplier Check
                if (obj2.getJSONObject(set[j].toString()).get("columnName").equals("relationname")) {
                    supplier = obj2.getJSONObject(set[j].toString()).get("value").toString();
                    supplier = supplier.split(":;")[0];
                }

                // Document Type check
                if (obj2.getJSONObject(set[j].toString()).get("columnName").equals("documenttypename")) {
                    docType = obj2.getJSONObject(set[j].toString()).get("value").toString();
                    csAssert.assertEquals("Other", docType, "Document Type mismatch for " + entityId + "");
                }

                // Region check
                if (obj2.getJSONObject(set[j].toString()).get("columnName").equals("globalregions")) {
                    regions = obj2.getJSONObject(set[j].toString()).get("value").toString();
                }

                // Services check
                if (obj2.getJSONObject(set[j].toString()).get("columnName").equals("contractsubtypes")) {
                    services = obj2.getJSONObject(set[j].toString()).get("value").toString();
                }
            }

            // Validation after hitting Show page
            Show show = new Show();
            show.hitShow(entityTypeId, entityId);
            String showJsonStr = show.getShowJsonStr();
            JSONObject showJson = new JSONObject(showJsonStr);
            JSONObject entityValues = showJson.getJSONObject("body").getJSONObject("data");

            String actualTitle = entityValues.getJSONObject("title").getString("values");
            String actualSupplier = new JSONObject(entityValues.getJSONObject("relations").getJSONArray("values").get(0).toString()).getString("name");
            String actualregions = getRegions(entityValues);
            String actualservices = getServices(entityValues);

            csAssert.assertEquals(actualTitle,title, "Title Mismatch - EntityId : "+entityId+"  Actual : "+actualTitle+" Expected : "+title+"");
            csAssert.assertEquals(actualSupplier,supplier, "Supplier Mismatch - EntityId : "+entityId+"  Actual : "+actualSupplier+" Expected : "+supplier+"");
            csAssert.assertEquals(actualregions,regions," Regions Mismatch - EntityId : "+entityId+"  Actual : "+actualregions+" Expected : "+regions+"");
            csAssert.assertEquals(actualservices, services, "Services Mismatch - EntityId : "+entityId+"  Actual : "+actualservices+" Expected : "+services+"");
        }

    }

    private void validateC90862(String searchResponse, CustomAssert csAssert){

        String contractStakeHolders="", supplierStakeHolders="";
        int contractId = 0, supplierId = 0;
        int parentEntityTypeId=1, childEntityTypeId=61;

        JSONObject obj = new JSONObject(searchResponse);
        JSONArray arr = obj.getJSONObject("body").getJSONObject("data").getJSONObject("searchResults").getJSONObject("values").getJSONArray("data");
        ArrayList<Integer> list = new ArrayList<Integer>();
        for(int i = 0 ; i <arr.length() ;  i++){
            JSONObject obj2 = arr.getJSONObject(i);

            Object[] set = obj2.keySet().toArray();
            for(int j = 0 ; j < obj2.length(); j++) {
                int temp2 = obj2.getJSONObject(set[j].toString()).getInt("columnId");

                // Id check
                if (obj2.getJSONObject(set[j].toString()).get("columnName").equals("id")) {
                    String temp = obj2.getJSONObject(set[j].toString()).get("value").toString();
                    csAssert.assertEquals("FO", temp.split(":;")[2]);
                    contractId = Integer.parseInt(temp.split(":;")[1]);
                }

                // Supplier Check
                if (obj2.getJSONObject(set[j].toString()).get("columnName").equals("relationname")) {
                    supplierId = Integer.parseInt(obj2.getJSONObject(set[j].toString()).get("value").toString().split(":;")[1]);
                }
            }

            // Fetch Stakeholders from Contract
            Show show = new Show();
            show.hitShow(childEntityTypeId, contractId);
            String showJsonStr = show.getShowJsonStr();
            JSONObject showJson = new JSONObject(showJsonStr);
            JSONObject entityValues = showJson.getJSONObject("body").getJSONObject("data");
            contractStakeHolders = getStakeholderForRoleGroup("Suppliers Manager", entityValues);

            // Fetch Stakeholders from Suppliers
            show.hitShow(parentEntityTypeId, supplierId);
            showJsonStr = show.getShowJsonStr();
            showJson = new JSONObject(showJsonStr);
            entityValues = showJson.getJSONObject("body").getJSONObject("data");
            supplierStakeHolders = getStakeholderForRoleGroup("Suppliers Manager", entityValues);

            csAssert.assertEquals(contractStakeHolders, supplierStakeHolders, "Stakeholders Mismatch - EntityId : "+contractId+"  Contract Stakeholders : "+contractStakeHolders+" Supplier Stakeholders : "+supplierStakeHolders+"");
        }

    }

    private void validateC90862(String searchResponse, String expectedStatus, String expectedTitle, CustomAssert csAssert){

        String title="", status="", docType="";
        int entityId = 0;

        JSONObject obj = new JSONObject(searchResponse);
        JSONArray arr = obj.getJSONObject("body").getJSONObject("data").getJSONObject("searchResults").getJSONObject("values").getJSONArray("data");
        ArrayList<Integer> list = new ArrayList<Integer>();
        for(int i = 0 ; i <arr.length() ;  i++){
            JSONObject obj2 = arr.getJSONObject(i);

            Object[] set = obj2.keySet().toArray();
            for(int j = 0 ; j < obj2.length(); j++) {
                int temp2 = obj2.getJSONObject(set[j].toString()).getInt("columnId");

                // Id check
                if (obj2.getJSONObject(set[j].toString()).get("columnName").equals("id")) {
                    String temp = obj2.getJSONObject(set[j].toString()).get("value").toString();
                    csAssert.assertEquals("FO", temp.split(":;")[2]);
                    entityId = Integer.parseInt(temp.split(":;")[1]);
                }

                // Title check
                if (obj2.getJSONObject(set[j].toString()).get("columnName").equals("documenttitle")) {
                    title = obj2.getJSONObject(set[j].toString()).get("value").toString();
                    title = title.split(":;")[0];
                    csAssert.assertTrue(title.toLowerCase().contains(expectedTitle.toLowerCase()), "Actual : "+title+" Expected : "+expectedTitle+" for entityID : "+entityId+" ");
                }

                // Document Type check
                if (obj2.getJSONObject(set[j].toString()).get("columnName").equals("documenttypename")) {
                    docType = obj2.getJSONObject(set[j].toString()).get("value").toString();
                    csAssert.assertEquals("Other", docType, "Document Type mismatch for " + entityId + "");
                }
            }

            // Status Check
            Show show = new Show();
            show.hitShow(entityTypeId, entityId);
            String showJsonStr = show.getShowJsonStr();
            JSONObject showJson = new JSONObject(showJsonStr);
            String actualStatus = showJson.getJSONObject("body").getJSONObject("data").getJSONObject("status").getJSONObject("values").getString("name");
            csAssert.assertEquals(actualStatus, expectedStatus,"Actual : "+actualStatus+" Expected : "+expectedStatus+" for entityID : "+entityId+" ");
        }

    }

    private void validateOtherContractExcel(String searchResponse, CustomAssert csAssert) throws ConfigurationException {

        String title="", supplier="", docType="", regions="", services="", seqId="";
        int entityId = 0;
        entityTypeId = 61;

        // Make the Payload for Search API
        String searchResponse2 = searchObj.hitMetadataSearch(entityTypeId);
        Map<String, String> field = ParseJsonResponse.getFieldByLabel(searchResponse2, "document Type");
        String payload = TestSearchMetadata.getPayload(field, "Other", entityTypeId, "contracts", "76");
        payload = payload.replaceAll("\"size\",\"values\":5","\"size\",\"values\":100" );

        logger.info("Downloading Excel for Multi Supplier Contract.");
        String filePath = "src/test";
        String fileName = "SearchResultsOtherContract.xlsx";

        HttpResponse downloadResponse = new MetadataSearchDownload().downloadMetadataSearchFile(61, payload);
        boolean fileDownloaded = new FileUtils().writeResponseIntoFile(downloadResponse, filePath + "/" + fileName);
        List<List<String>> xlList = XLSUtils.getExcelDataOfMultipleRows(filePath, fileName, "Data",4, new Long(XLSUtils.getNoOfRows(filePath, fileName, "Data")).intValue());

        JSONObject obj = new JSONObject(searchResponse);
        JSONArray array = obj.getJSONObject("body").getJSONObject("data").getJSONObject("searchResults").getJSONObject("values").getJSONArray("data");
        ArrayList<Integer> list = new ArrayList<Integer>();
        for(int i = 0 ; i <array.length() ;  i++){
            JSONObject obj2 = array.getJSONObject(i);

            Object[] set = obj2.keySet().toArray();
            for(int j = 0 ; j < obj2.length(); j++) {
                int temp2 = obj2.getJSONObject(set[j].toString()).getInt("columnId");

                // Fetching SeqId
                if (obj2.getJSONObject(set[j].toString()).get("columnName").equals("id")) {
                    String temp = obj2.getJSONObject(set[j].toString()).get("value").toString();
                    seqId = "FO"+temp.split(":;")[0];
                }

                // Id check
                if (obj2.getJSONObject(set[j].toString()).get("columnName").equals("id")) {
                    String temp = obj2.getJSONObject(set[j].toString()).get("value").toString();
                    csAssert.assertEquals("FO", temp.split(":;")[2]);
                    entityId = Integer.parseInt(temp.split(":;")[1]);
                }

                // Title check
                if (obj2.getJSONObject(set[j].toString()).get("columnName").equals("documenttitle")) {
                    title = obj2.getJSONObject(set[j].toString()).get("value").toString();
                    title = title.split(":;")[0];
                }

                // Supplier Check
                if (obj2.getJSONObject(set[j].toString()).get("columnName").equals("relationname")) {
                    supplier = obj2.getJSONObject(set[j].toString()).get("value").toString();
                    supplier = supplier.split(":;")[0];
                }

                // Document Type check
                if (obj2.getJSONObject(set[j].toString()).get("columnName").equals("documenttypename")) {
                    docType = obj2.getJSONObject(set[j].toString()).get("value").toString();
                    csAssert.assertEquals("Other", docType, "Document Type mismatch for " + entityId + "");
                }

                // Region check
                if (obj2.getJSONObject(set[j].toString()).get("columnName").equals("globalregions")) {
                    regions = obj2.getJSONObject(set[j].toString()).get("value").toString();
                }

                // Services check
                if (obj2.getJSONObject(set[j].toString()).get("columnName").equals("contractsubtypes")) {
                    services = obj2.getJSONObject(set[j].toString()).get("value").toString();
                }
            }
            // Verification of the Data from Response with Data in excel
            if (fileDownloaded) {
                try {
                    for(List<String> item: xlList) {
                        String expseqId = item.get(0);
                        if(seqId.equals(expseqId)){
                            csAssert.assertEquals(title, item.get(1));
                            csAssert.assertEquals(supplier, item.get(3));
                            csAssert.assertEquals(docType, item.get(4));
                            csAssert.assertEquals(regions, item.get(5));
                            csAssert.assertEquals(services, item.get(6));
                            break;
                        }
                    }
                } catch (Exception e) {
                    logger.error("Exception while Verifying Other Contract Data", e.getMessage());
                }
            }
            else{
                csAssert.fail("System was not able to download the file.");
            }
        }
        //Delete Excel
        FileUtils.deleteFile(filePath, fileName);
    }

    public String getRegions(JSONObject entityValues){

        String region = "";

        JSONArray arr =  entityValues.getJSONObject("globalRegions").getJSONArray("values");
        for(int i = 0 ; i < arr.length() ; i++){
            String temp =new JSONObject(arr.get(i).toString()).getString("name");
            if(i==0){
                region = temp;
            }
            else {
                region = region +", "+ temp;
            }
        }

        return region;
    }

    public String getServices(JSONObject entityValues){

        String services = "";

        JSONArray arr =    entityValues.getJSONObject("services").getJSONArray("values");
        for(int i = 0 ; i < arr.length() ; i++){
            String temp =new JSONObject(arr.get(i).toString()).getString("name");
            if(i==0){
                services = temp;
            }
            else {
                services = services +", "+ temp;
            }
        }

        String[] temparr = services.split(", ");
        Arrays.sort(temparr);
        String newStr="";
        for(String item : temparr){
            newStr = newStr +", "+item;
        }
        newStr = newStr.substring(2);

        return newStr;
    }

    private String createEditPayload(String editPayload){

        JSONObject editResponseJson = new JSONObject(editPayload);

        editResponseJson.remove("header");
        editResponseJson.remove("session");
        editResponseJson.remove("actions");
        editResponseJson.remove("createLinks");

        editResponseJson.getJSONObject("body").remove("layoutInfo");
        editResponseJson.getJSONObject("body").remove("globalData");
        editResponseJson.getJSONObject("body").remove("errors");

        return editResponseJson.toString();

    }

    private void editEntityWithSpecificStakeholder(String entityName, int entityId,String stakeholderName, String stakeHolderId){

        CustomAssert csAssert = new CustomAssert();
        String editPayloadOld = "";
        Edit edit = new Edit();
        String contractRoleGroupId = "rg_" + parentRoleGroupId ;

        // Edit the value of the role group under test in the parent
        String editResponse = edit.hitEdit(entityName,entityId);

        editPayloadOld = createEditPayload(editResponse);

        JSONArray valuesJsonArray = new JSONArray();
        String stakeHolderString = "{\"name\":\""+stakeholderName+"\",\"id\":"+stakeHolderId+",\"type\":2,\"email\":\"naveen@sirionqa.office\"}";

        JSONObject stakeHolderJson = new JSONObject(stakeHolderString);
        valuesJsonArray.put(0,stakeHolderJson);

        JSONObject editPayloadNew = new JSONObject(editPayloadOld);

        editPayloadNew.getJSONObject("body").getJSONObject("data").getJSONObject("stakeHolders").getJSONObject("values").getJSONObject(contractRoleGroupId).put("values",valuesJsonArray);

        try {
            editResponse = edit.hitEdit(entityName,editPayloadNew.toString());
        } catch (Exception e) {
            logger.error("Exception while Validating Role Group FlowDown SL CSL Listing " + e.getStackTrace());
            csAssert.assertTrue(false,"Exception while Validating Role Group FlowDown SL CSL Listing " + e.getStackTrace());
        }

        if(!editResponse.contains("success")){
            csAssert.assertTrue(false,"Edit Operation performed unsuccessfully for the parent entity " + entityName);
        }
    }

    private String getStakeholderForRoleGroup(String roleGroupName, JSONObject entityValues){
        JSONObject obj = entityValues.getJSONObject("stakeHolders").getJSONObject("values");

        String stakeHolders = "";
        Object[] obj2 = obj.keySet().toArray();
        for(Object item : obj2){
            if(obj.getJSONObject(item.toString()).getString("label").equals(roleGroupName)) {
                JSONArray stakeHoldersName = obj.getJSONObject(item.toString()).getJSONArray("values");
                for(int i =0 ; i < stakeHoldersName.length() ; i++){
                    String tempName = stakeHoldersName.getJSONObject(i).getString("name");
                    stakeHolders = stakeHolders +","+tempName;
                }
            }
        }
        return stakeHolders.substring(1);
    }
}
