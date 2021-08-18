package com.sirionlabs.helper.autoextraction;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class DocumentShowPageHelper {
    private final static Logger logger = LoggerFactory.getLogger(DocumentShowPageHelper.class);
    public static String clauseDataStr;
    public static String metadataStr;
    public static String showCategoryStr;
    public static String showFieldStr;
    public static String configAutoExtractionFilePath;
    public static String configAutoExtractionFileName;

    public DocumentShowPageHelper(String documentId) throws IOException {
        clauseDataStr=getClauseTabResponse(documentId);
        metadataStr=getMetadataTabResponse(documentId);
        showCategoryStr=showCategoryResponse();
        showFieldStr=showFieldResponse();
        configAutoExtractionFilePath = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFilePath");
        configAutoExtractionFileName = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFileName");
    }

    public DocumentShowPageHelper()
    {
        configAutoExtractionFilePath = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFilePath");
        configAutoExtractionFileName = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFileName");
    }

    public static String getCategoryTextId() throws IOException {
        String textId=" ";
        int textIdColumn= ListDataHelper.getColumnIdFromColumnName(clauseDataStr,"textId");
        JSONObject clauseListJson=new JSONObject(clauseDataStr);
        JSONObject jsonObj=clauseListJson.getJSONArray("data").getJSONObject(0);
        textId=jsonObj.getJSONObject(Integer.toString(textIdColumn)).getString("value");
        return  textId;
    }

    public static String getCategoryText()
    {
        String text="";
        int textColumn= ListDataHelper.getColumnIdFromColumnName(clauseDataStr,"text");
        JSONObject clauseListJson=new JSONObject(clauseDataStr);
        text=clauseListJson.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(textColumn)).getString("value");
        return  text;

    }

    public static String getCategoryId()
    {
        int categoryIdColumn=ListDataHelper.getColumnIdFromColumnName(clauseDataStr,"categoryId");
        JSONObject clauseListJson=new JSONObject(clauseDataStr);
        String categoryId=clauseListJson.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(categoryIdColumn)).getString("value");
        return categoryId;
    }

    public static String getClauseTabResponse(String documentId) throws IOException {
        CustomAssert csAssert=new CustomAssert();
        logger.info("Hitting Clause list data API for document id "+documentId);
        String payload="{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\"," +
                "\"orderDirection\":\"asc nulls first\",\"filterJson\":{\"366\":{\"multiselectValues\":{\"SELECTEDDATA\":[]},\"filterId\":366," +
                "\"filterName\":\"categoryId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null},\"386\":{\"filterId\":\"386\"," +
                "\"filterName\":\"score\",\"min\":\"0\"}}},\"entityId\":"+documentId+"}";
        HttpResponse clauseListDataResponse=AutoExtractionHelper.getTabData(payload,493);
        csAssert.assertTrue(clauseListDataResponse.getStatusLine().getStatusCode()==200,"Clause list data API response is not valid");
        String clauseDataStr= EntityUtils.toString(clauseListDataResponse.getEntity());
        return  clauseDataStr;
    }

    public static String showCategoryResponse() throws IOException {
        CustomAssert csAssert=new CustomAssert();
        logger.info("Hitting show Category API");
        HttpResponse showCategoryResponse=AutoExtractionHelper.showCategory();
        showCategoryStr=EntityUtils.toString(showCategoryResponse.getEntity());
        csAssert.assertTrue(showCategoryResponse.getStatusLine().getStatusCode()==200,"Response code is invalid");
        return showCategoryStr;
    }

    public static String getNewCategoryName() throws IOException {
        String categoryName="";
        JSONObject jsonObj=new JSONObject(showCategoryStr);
        categoryName=jsonObj.getJSONArray("response").getJSONObject(0).get("name").toString();
        return categoryName;

    }

    public static String getNewCategoryId()
    {
        String categoryId="";
        JSONObject jsonObj=new JSONObject(showCategoryStr);
        categoryId=jsonObj.getJSONArray("response").getJSONObject(0).get("id").toString();
        return categoryId;
    }

    public static List<String> getNewCategoryIdList()
    {
        List<String> CategoryIdList=new ArrayList<>();
        JSONObject jsonObj=new JSONObject(showCategoryStr);
        int count=jsonObj.getJSONArray("response").length();
        for(int i=0;i<count;i++) {
            CategoryIdList.add(jsonObj.getJSONArray("response").getJSONObject(i).get("id").toString());
        }
        return CategoryIdList;
    }

    public static Map<String,String> getNewCategoryIdListMap()
    {
        Map<String,String> CategoryMap=new HashMap<>();
        JSONObject jsonObj=new JSONObject(showCategoryStr);
        int count=jsonObj.getJSONArray("response").length();
        for(int i=0;i<count;i++) {
            CategoryMap.put(jsonObj.getJSONArray("response").getJSONObject(i).get("id").toString(),jsonObj.getJSONArray("response").getJSONObject(i).get("name").toString());
        }
        return CategoryMap;
    }

    public static int feedbackCount(String newCategoryId) throws IOException {
        CustomAssert csAssert=new CustomAssert();
        clientAdminUserLogin();
        String feedbackListPayload="{\"filterMap\":{\"entityTypeId\":316,\"customFilter\":{\"aeModelType\":1,\"aeEntityId\":"+newCategoryId+"}}}";
        HttpResponse feedbackListDataResponse=AutoExtractionHelper.extractionFeedbackList(feedbackListPayload,"530");
        csAssert.assertTrue(feedbackListDataResponse.getStatusLine().getStatusCode()==200,"Response code is invalid");
        String feedbackListDataResponseStr=EntityUtils.toString(feedbackListDataResponse.getEntity());
        JSONObject listJson=new JSONObject(feedbackListDataResponseStr);
        int feedbackCount=Integer.valueOf(listJson.get("filteredCount").toString());
        endUserLogin();
        return  feedbackCount;
    }

    public static String getMetadataTabResponse(String documentId) throws IOException {
        CustomAssert csAssert=new CustomAssert();
        logger.info("Hitting Metadata list data API for document id "+documentId);
        String payload="{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":50,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\"," +
                "\"filterJson\":{\"367\":{\"multiselectValues\":{\"SELECTEDDATA\":[]},\"filterId\":367,\"filterName\":\"fieldId\"}," +
                "\"385\":{\"multiselectValues\":{\"SELECTEDDATA\":[]},\"filterId\":385,\"filterName\":\"projectids\"},\"386\":{\"filterId\":\"386\",\"filterName\":\"score\",\"min\":\"0\"}}},\"entityId\":"+documentId+"}";
        HttpResponse metadataListDataResponse=AutoExtractionHelper.getTabData(payload,433);
        csAssert.assertTrue(metadataListDataResponse.getStatusLine().getStatusCode()==200,"Clause list data API response is not valid");
        String metadataDataStr= EntityUtils.toString(metadataListDataResponse.getEntity());
        return  metadataDataStr;
    }

    public static String showFieldResponse() throws IOException {
        CustomAssert csAssert=new CustomAssert();
        logger.info("Hitting show Field API");
        HttpResponse showFieldResponse=AutoExtractionHelper.showField();
        showFieldStr=EntityUtils.toString(showFieldResponse.getEntity());
        csAssert.assertTrue(showFieldResponse.getStatusLine().getStatusCode()==200,"Response code is invalid");
        return showFieldStr;

    }
    public static String getNewFieldId()
    {
        {
            String fieldId="";
            JSONObject jsonObj=new JSONObject(showFieldStr);
            fieldId=jsonObj.getJSONArray("response").getJSONObject(0).get("id").toString();
            return fieldId;
        }
    }

    public static String getMetadataTextId() throws IOException {
        String textId=" ";
        int textIdColumn= ListDataHelper.getColumnIdFromColumnName(metadataStr,"textId");
        JSONObject clauseListJson=new JSONObject(metadataStr);
        JSONObject jsonObj=clauseListJson.getJSONArray("data").getJSONObject(0);
        textId=jsonObj.getJSONObject(Integer.toString(textIdColumn)).getString("value");
        return  textId;
    }

    public static String getMetadataText()
    {
        String text="";
        int textColumn= ListDataHelper.getColumnIdFromColumnName(metadataStr,"text");
        JSONObject clauseListJson=new JSONObject(metadataStr);
        text=clauseListJson.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(textColumn)).getString("value");
        return  text;

    }
    public static List<String> getMetadataAllValue()
    {
        List<String> allValue=new ArrayList<>();
        int textColumn= ListDataHelper.getColumnIdFromColumnName(metadataStr,"extractedtext");
        JSONObject metadataListJson=new JSONObject(metadataStr);
        int count=metadataListJson.getJSONArray("data").length();
        for(int i=0;i<count;i++) {
            allValue.add(metadataListJson.getJSONArray("data").getJSONObject(i).getJSONObject(Integer.toString(textColumn)).getString("value"));
        }
        return  allValue;

    }

    public static List<String> getCategoryAllValue()
    {
        List<String> allValue=new ArrayList<>();
        int categoryIdColumn= ListDataHelper.getColumnIdFromColumnName(clauseDataStr,"categoryId");
        JSONObject clauseListJson=new JSONObject(clauseDataStr);
        int count=clauseListJson.getJSONArray("data").length();
        for(int i=0;i<count;i++) {
            allValue.add(clauseListJson.getJSONArray("data").getJSONObject(i).getJSONObject(Integer.toString(categoryIdColumn)).getString("value"));

        }
        return  allValue;

    }
    public static int metadataFeedbackCount(String newFieldId) throws IOException {
        CustomAssert csAssert=new CustomAssert();
        clientAdminUserLogin();
        String feedbackListPayload="{\"filterMap\":{\"entityTypeId\":316,\"customFilter\":{\"aeModelType\":2,\"aeEntityId\":"+newFieldId+"}}}";
        HttpResponse feedbackListDataResponse=AutoExtractionHelper.extractionFeedbackList(feedbackListPayload,"530");
        csAssert.assertTrue(feedbackListDataResponse.getStatusLine().getStatusCode()==200,"Response code is invalid");
        String feedbackListDataResponseStr=EntityUtils.toString(feedbackListDataResponse.getEntity());
        JSONObject listJson=new JSONObject(feedbackListDataResponseStr);
        int feedbackCount=Integer.valueOf(listJson.get("filteredCount").toString());
        endUserLogin();
        return  feedbackCount;
    }

    public static int metadataCountOnShowPage(String docId) throws IOException {
        Set<String> set=new HashSet<>();
        String metadataDataStr=DocumentShowPageHelper.getMetadataTabResponse(docId);
        int fieldNameCol=ListDataHelper.getColumnIdFromColumnName(metadataDataStr,"fieldname");
        JSONObject metadataListJson=new JSONObject(metadataDataStr);
        int count=metadataListJson.getJSONArray("data").length();
        for(int i=0;i<count;i++)
        {
            String[] value=metadataListJson.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(fieldNameCol)).getString("value").split(":;");
            set.add(value[1]);
        }
        return set.size();
    }
    public static int clauseCountOnShowPage(int docId) throws IOException {
        Set<String> set=new HashSet<>();
        String clauseDataStr=DocumentShowPageHelper.getClauseTabResponse(String.valueOf(docId));
        int categoryNameCol=ListDataHelper.getColumnIdFromColumnName(clauseDataStr,"name");
        JSONObject clauseListJson=new JSONObject(clauseDataStr);
        int count=clauseListJson.getJSONArray("data").length();
        for(int i=0;i<count;i++)
        {
            String[] value=clauseListJson.getJSONArray("data").getJSONObject(i).getJSONObject(String.valueOf(categoryNameCol)).getString("value").split(":;");
            set.add(value[1]);
        }
        return set.size();
    }


    public static void endUserLogin()
    {
        CustomAssert customAssert=new CustomAssert();

        Check check = new Check();
        // Login to End User
        logger.info("login to End user...");
        String endUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "username");
        String endUserPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "end user credentials", "password");
        logger.info("Hitting login API with End user username "+endUserName+" and Password "+endUserPassword);
        HttpResponse loginResponse = check.hitCheck(endUserName, endUserPassword);
        customAssert.assertTrue(loginResponse.getStatusLine().getStatusCode() == 302, "Response Code is not valid");
    }

    public  static void clientAdminUserLogin()
    {
        CustomAssert customAssert=new CustomAssert();
        logger.info("login with  to client Admin user");
        String clientAdminUserName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "client admin credentials", "username");
        String clientAdminPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "client admin credentials", "password");
        Check check = new Check();
        logger.info("Hitting login API with Client Admin user username "+clientAdminUserName+" and Password "+clientAdminPassword);
        HttpResponse loginResponse = check.hitCheck(clientAdminUserName, clientAdminPassword);
        customAssert.assertTrue(loginResponse.getStatusLine().getStatusCode()==302,"Login response code is not valid");
    }

    public static String getClauseIdLinkedToMetadata() {
        String clauseId = "";
        int categoryIdColumn = ListDataHelper.getColumnIdFromColumnName(metadataStr, "categoryId");
        JSONObject metadataListJson = new JSONObject(metadataStr);
        clauseId = metadataListJson.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(categoryIdColumn)).getString("value");
        return clauseId;

    }

    public static List<String> getAllClauseIdLinkedToMetadata() {
        List<String> AllClauseIdLinkedToMetadata=new ArrayList<>();
        int categoryIdColumn = ListDataHelper.getColumnIdFromColumnName(metadataStr, "categoryId");
        JSONObject metadataListJson = new JSONObject(metadataStr);
        int count=metadataListJson.getJSONArray("data").length();
        for(int i=0;i<count;i++) {
            AllClauseIdLinkedToMetadata.add(metadataListJson.getJSONArray("data").getJSONObject(i).getJSONObject(Integer.toString(categoryIdColumn)).getString("value"));
        }
        return AllClauseIdLinkedToMetadata;

    }
    public static String getMetadataId() {
        String metadataId = "";
        int metadataIdColumn = ListDataHelper.getColumnIdFromColumnName(metadataStr, "fieldname");
        JSONObject metadataListJson = new JSONObject(metadataStr);
        metadataId = metadataListJson.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(metadataIdColumn)).getString("value").split(":;")[1];
        return metadataId;

    }

    public static String getCategoryPageNo() throws IOException {
        String ClausePageNo = " ";
        int pageNoColumnId = ListDataHelper.getColumnIdFromColumnName(clauseDataStr, "pageno");
        JSONObject clauseListJson = new JSONObject(clauseDataStr);
        JSONObject jsonObj = clauseListJson.getJSONArray("data").getJSONObject(0);
        ClausePageNo = jsonObj.getJSONObject(Integer.toString(pageNoColumnId)).getString("value");
        return ClausePageNo;
    }

    public static String getClauseTabResponse(String documentId,String payload) throws IOException {
        CustomAssert csAssert=new CustomAssert();
        logger.info("Hitting Clause list data API for document id "+documentId);
        HttpResponse clauseListDataResponse=AutoExtractionHelper.getTabData(payload,493);
        csAssert.assertTrue(clauseListDataResponse.getStatusLine().getStatusCode()==200,"Clause list data API response is not valid");
        String clauseDataStr= EntityUtils.toString(clauseListDataResponse.getEntity());
        return  clauseDataStr;
    }

}