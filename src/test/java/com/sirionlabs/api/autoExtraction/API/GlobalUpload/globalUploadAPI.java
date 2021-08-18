package com.sirionlabs.api.autoExtraction.API.GlobalUpload;

import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.asserts.SoftAssert;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class globalUploadAPI {

    private final static Logger logger = LoggerFactory.getLogger(globalUploadAPI.class);

    public static String getApiPath() {
        return "/autoextraction/api/v1/document/update";
    }

    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        return headers;
    }

    public static JSONObject createJson(String extension, String key, String name){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("extension",extension);
        jsonObject.put("key",key);
        jsonObject.put("name",name);
        return jsonObject;
    }

    public static JSONObject createJson(String extension, String key, String name, JSONArray projectIds){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("extension",extension);
        jsonObject.put("key",key);
        jsonObject.put("name",name);
        jsonObject.put("projectIds",projectIds);
        return jsonObject;
    }

    public static JSONObject createJson(String extension, String key, String name, JSONArray projectIds,JSONArray groupIds,JSONArray tagIds){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("extension",extension);
        jsonObject.put("key",key);
        jsonObject.put("name",name);
        jsonObject.put("projectIds",projectIds);
        jsonObject.put("groupIds",groupIds);
        jsonObject.put("tagIds",tagIds);
        return jsonObject;
    }

    public static JSONObject createJson(String extension, String key, String name, int pageNo,JSONArray projectIds){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("extension",extension);
        jsonObject.put("key",key);
        jsonObject.put("name",name);
        jsonObject.put("projectIds",projectIds);
        return jsonObject;
    }

    public static String getPayload(List<JSONObject> fileJson){
        return fileJson.toString();
    }

    public static HttpResponse hitGlobalUpload(String apiPath,String payload){
        HttpResponse httpResponse = null;
        try {
            HttpPost httpPost = new HttpPost(apiPath);
            httpPost.addHeader("Content-Type", "application/json");
            httpResponse = APIUtils.postRequest(httpPost, payload);
        }
        catch (Exception e){
            logger.error("Error in hitting Global Upload API " + e.getStackTrace());
        }
        return httpResponse;
    }

    /*
    Author: Amit Saxena
    Date: 18-Feb-2020
    Method: To create Group IDs
     */

    public static List<String> getGroupIds() throws IOException {

        SoftAssert softAssert = new SoftAssert();
        // Hit master Data API to verify whether created group and tag are present in master data API
        HttpResponse hitMasterDataAPI = AutoExtractionHelper.hitShowViewerAPI("/autoextraction/masterData");
        String masterDataStr = EntityUtils.toString(hitMasterDataAPI.getEntity());
        softAssert.assertTrue(hitMasterDataAPI.getStatusLine().getStatusCode() == 200,"Response Code for master data api is not valid");
        List<String> groupIds = AutoExtractionHelper.getIdsForEntityFromName(masterDataStr,"groups");
        softAssert.assertAll();
        return groupIds;

    }

    /*
   Author: Amit Saxena
   Date: 18-Feb-2020
   Method: To create Tag IDs
    */

    public static List<String> getTagIds() throws IOException {

        SoftAssert softAssert = new SoftAssert();
        // Hit master Data API to verify whether created group and tag are present in master data API
        HttpResponse hitMasterDataAPI = AutoExtractionHelper.hitShowViewerAPI("/autoextraction/masterData");
        String masterDataStr = EntityUtils.toString(hitMasterDataAPI.getEntity());
        softAssert.assertTrue(hitMasterDataAPI.getStatusLine().getStatusCode() == 200,"Response Code for master data api is not valid");
        List<String> tagIds = AutoExtractionHelper.getIdsForEntityFromName(masterDataStr,"tags");
        softAssert.assertAll();
        return tagIds;

    }

    /*
   Author: Amit Saxena
   Date: 18-Feb-2020
   Method: To create Json File
    */

    public static JSONArray testCreateJsonFileData() throws FileNotFoundException {
        List<String> groupIds = null;
        List<String> tagIds = null;
        try {
            groupIds = getGroupIds();
            tagIds = getTagIds();
        } catch (IOException e) {
            e.printStackTrace();
        }


        String jsonArrayStr = "";
        JSONArray mainJsonArray = new JSONArray();
        JSONObject[] jsoObjArray = new JSONObject[6];
        JSONArray projectIdsArr = new JSONArray();
        projectIdsArr.put(1);
        projectIdsArr.put(2);
        JSONArray groupIdsArr = new JSONArray();
        int counterLoop1 = 0;
        int counterLoop2 = 0;
        for(String s1: groupIds)
        {
            counterLoop1 = counterLoop1 + 1;
            if(counterLoop1 > 2){
                break;
            }
            groupIdsArr.put(Integer.valueOf(s1));
        }


        JSONArray tagIdsArr = new JSONArray();
        for(String s2: tagIds)
        {
            counterLoop2 = counterLoop2 + 1;
            if(counterLoop2 > 2){
                break;
            }
            tagIdsArr.put(Integer.valueOf(s2));
        }


        int lengthJso = jsoObjArray.length;
        //Initialize JSONObject Array
        for(int i=0;i<lengthJso;i++){
            jsoObjArray[i] = new JSONObject();
            mainJsonArray.put(jsoObjArray[i]);
        }
        //1. Creating First Json Object
        jsoObjArray[0].put("testCaseId","1");
        jsoObjArray[0].put("description","Validate Positive Flow for Global Upload API with one file");
        jsoObjArray[0].put("enabled","yes");
        jsoObjArray[0].put("useJsonParams",false);
        jsoObjArray[0].put("extension","");
        jsoObjArray[0].put("key","");
        jsoObjArray[0].put("name","");
        jsoObjArray[0].put("projectIds",projectIdsArr);
        jsoObjArray[0].put("groupIds",groupIdsArr);
        jsoObjArray[0].put("tagIds",tagIdsArr);
        jsoObjArray[0].put("errors", "null");
        jsoObjArray[0].put("success","true");
        jsoObjArray[0].put("numberOfFiles", 1);
        jsoObjArray[0].put("expectedStatusCode","2XX");



        //2. Creating First Json Object

        /*

         */

        jsoObjArray[1].put("testCaseId","2");
        jsoObjArray[1].put("description","Validate Positive Flow for Global Upload API with multiple files");
        jsoObjArray[1].put("enabled","yes");
        jsoObjArray[1].put("useJsonParams",false);
        jsoObjArray[1].put("extension","");
        jsoObjArray[1].put("key","");
        jsoObjArray[1].put("name","");
        jsoObjArray[1].put("projectIds",projectIdsArr);
        jsoObjArray[1].put("groupIds",groupIdsArr);
        jsoObjArray[1].put("tagIds",tagIdsArr);
        jsoObjArray[1].put("errors", "null");
        jsoObjArray[1].put("success","true");
        jsoObjArray[1].put("numberOfFiles", 3);
        jsoObjArray[1].put("expectedStatusCode","2XX");


        JSONArray projectIdsArr1 = new JSONArray();
        JSONArray groupIdsArr1 = new JSONArray();

        JSONArray tagIdsArr1 = new JSONArray();
        //3. Creating First Json Object
        jsoObjArray[2].put("testCaseId","3");
        jsoObjArray[2].put("description","Validate Negative Flow for Global Upload API with all parameters missing for single file");
        jsoObjArray[2].put("enabled","yes");
        jsoObjArray[2].put("useJsonParams",true);
        jsoObjArray[2].put("extension","");
        jsoObjArray[2].put("key","");
        jsoObjArray[2].put("name","");
        jsoObjArray[2].put("projectIds",projectIdsArr1);
        jsoObjArray[2].put("groupIds",groupIdsArr1);
        jsoObjArray[2].put("tagIds",tagIdsArr1);
        jsoObjArray[2].put("errors", "Cannot create document");
        jsoObjArray[2].put("success","false");
        jsoObjArray[2].put("numberOfFiles", 1);
        jsoObjArray[2].put("expectedStatusCode","422");

        /*




         */

        //4. Creating First Json Object
        jsoObjArray[3].put("testCaseId","4");
        jsoObjArray[3].put("description","Validate Negative Flow for Global Upload API with all parameters missing for multiple files");
        jsoObjArray[3].put("enabled","yes");
        jsoObjArray[3].put("useJsonParams",true);
        jsoObjArray[3].put("extension","");
        jsoObjArray[3].put("key","");
        jsoObjArray[3].put("name","");
        jsoObjArray[3].put("projectIds",projectIdsArr1);
        jsoObjArray[3].put("groupIds",groupIdsArr1);
        jsoObjArray[3].put("tagIds",tagIdsArr1);
        jsoObjArray[3].put("errors", "Cannot create document");
        jsoObjArray[3].put("success","false");
        jsoObjArray[3].put("numberOfFiles", 3);
        jsoObjArray[3].put("expectedStatusCode","422");



        //5. Creating First Json Object
        jsoObjArray[4].put("testCaseId","5");
        jsoObjArray[4].put("description","Validate Negative Flow for Global Upload API with all parameters missing for multiple files");
        jsoObjArray[4].put("enabled","yes");
        jsoObjArray[4].put("useJsonParams",true);
        jsoObjArray[4].put("extension","");
        jsoObjArray[4].put("key","");
        jsoObjArray[4].put("name","");
        jsoObjArray[4].put("projectIds",projectIdsArr1);
        jsoObjArray[4].put("groupIds",groupIdsArr1);
        jsoObjArray[4].put("tagIds",tagIdsArr1);
        jsoObjArray[4].put("errors", "Cannot create document");
        jsoObjArray[4].put("success","false");
        jsoObjArray[4].put("numberOfFiles", 3);
        jsoObjArray[4].put("expectedStatusCode","422");


        JSONArray projectIdsArr2 = new JSONArray();
        projectIdsArr2.put(345);


        //6. Creating First Json Object
        jsoObjArray[5].put("testCaseId","6");
        jsoObjArray[5].put("description","Validate Negative Flow for Global Upload API with parameters missing for multiple files");
        jsoObjArray[5].put("enabled","yes");
        jsoObjArray[5].put("useJsonParams",true);
        jsoObjArray[5].put("extension","");
        jsoObjArray[5].put("key","");
        jsoObjArray[5].put("name","");
        jsoObjArray[5].put("projectIds",projectIdsArr2);
        jsoObjArray[5].put("groupIds",groupIdsArr1);
        jsoObjArray[5].put("tagIds",tagIdsArr1);
        jsoObjArray[5].put("errors", "Cannot create document with a blank name");
        jsoObjArray[5].put("success","false");
        jsoObjArray[5].put("numberOfFiles", 3);
        jsoObjArray[5].put("expectedStatusCode","422");

        return mainJsonArray;

    }


}
