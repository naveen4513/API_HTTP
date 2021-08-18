package com.sirionlabs.helper.autoextraction;

import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AEUXAdminHelper {
    private final static Logger logger = LoggerFactory.getLogger(AEUXAdminHelper.class);

    public static HttpResponse CreateAPI(String payload,String type){
        HttpResponse response = null;
        HttpPost postRequest;
        try{
            String queryString = "/metadataautoextraction/"+type+"/create";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Accept","application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding","gzip, deflate");
            postRequest.addHeader("Content-Type","application/json;charset=UTF-8");
            response = APIUtils.postRequest(postRequest,payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Create clause/metadata API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Create clause/metadata API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse uxAdminListing(int listId,String payload){
        HttpResponse response = null;
        HttpPost postRequest;
        try{
            String queryString = "/listRenderer/list/"+listId+"/listdata?version=2.0";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Accept","application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding","gzip, deflate");
            postRequest.addHeader("Content-Type","application/json;charset=UTF-8");
            response = APIUtils.postRequest(postRequest,payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("clause/metadata List API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting List clause/metadata API. {}", e.getMessage());
        }
        return response;
    }

    public static List<String> getAllCategoryList() {
        CustomAssert csAssert = new CustomAssert();
        List<String> allCategoryList=new ArrayList<>();
        try {
            DocumentShowPageHelper.clientAdminUserLogin();
            String listPayload = "{\"filterMap\":{\"offset\":0,\"size\":100,\"orderByColumnName\":\"is_dynamic\",\"orderDirection\":\"asc nulls last\",\"filterJson\":{},\"entityTypeId\":316},\"selectedColumns\":[]}";
            HttpResponse clauseListResponse = AEUXAdminHelper.uxAdminListing(509, listPayload);
            csAssert.assertTrue(clauseListResponse.getStatusLine().getStatusCode() == 200, "Category List API response code in invalid");
            String categoryListResponseStr = EntityUtils.toString(clauseListResponse.getEntity());
            JSONObject categoryListResponseJson = new JSONObject(categoryListResponseStr);
            Integer columnId = ListDataHelper.getColumnIdFromColumnName(categoryListResponseStr, "category_name");
            Integer customColumnId=ListDataHelper.getColumnIdFromColumnName(categoryListResponseStr,"is_dynamic");
            int count = categoryListResponseJson.getJSONArray("data").length();
            for (int i = 0; i < count; i++) {
                String isDynamic = categoryListResponseJson.getJSONArray("data").getJSONObject(i).getJSONObject(Integer.toString(customColumnId)).getString("value");
                if(isDynamic.equals("false"))
                {
                    String[] value = categoryListResponseJson.getJSONArray("data").getJSONObject(i).getJSONObject(Integer.toString(columnId)).getString("value").split(":;");
                    allCategoryList.add(value[1].trim());
                }
            }

        } catch (Exception e) {
            csAssert.assertTrue(false,"Getting exception while hitting category list API on UXAdmin");

        }
        csAssert.assertAll();
        DocumentShowPageHelper.endUserLogin();
        return allCategoryList;
    }

}
