package com.sirionlabs.api.clientAdmin.report;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.clientSetup.ClientSetupHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import static com.sirionlabs.helper.api.TestAPIBase.executor;

public class ClientSetupAdminReportList {
    private  static Logger logger= LoggerFactory.getLogger(ClientSetupAdminReportList.class);
    public static String getApiPath() {
        return "/reportRenderer/listsetupjson";
    }

    public static HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json, text/javascript, */*; q=0.01");
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("Accept-Encoding", "gzip, deflate");
        return headers;
    }

    public static boolean isReportPresentInClientSetupAdmin(String entityName, String reportName) {
        String lastUserName = Check.lastLoggedInUserName;
        String lastUserPassword = Check.lastLoggedInUserPassword;

        try {
            ClientSetupHelper clientSetupHelper=new ClientSetupHelper();
            clientSetupHelper.loginWithClientSetupUser();
            String  response = executor.postMultiPartFormData(getApiPath(), getHeaders(), null).getResponse().getResponseBody();
            JSONArray jsonArray=new JSONArray(response);
            for (int i=0; i<jsonArray.length(); i++)
            {
                if(jsonArray.getJSONObject(i).get("name").toString().equalsIgnoreCase(entityName))
                {
                        for (int j=0; j<jsonArray.getJSONObject(i).getJSONArray("listMetaDataJsons").length();j++)
                        {
                            if(jsonArray.getJSONObject(i).getJSONArray("listMetaDataJsons").getJSONObject(j).get("name").toString().equalsIgnoreCase(reportName)){
                                    return true;
                            }
                        }

                }
            }
        } catch (Exception e) {
            logger.info(e.toString());
        }
        finally {
            Check check=new Check();
            check.hitCheck(lastUserName,lastUserPassword);
        }
      return  false;
    }
    public static boolean isReportPresentInClientSetupAdmin(String entityName, String reportName, CustomAssert customAssert) {
        String lastUserName = Check.lastLoggedInUserName;
        String lastUserPassword = Check.lastLoggedInUserPassword;

        try {
            ClientSetupHelper clientSetupHelper=new ClientSetupHelper();
            clientSetupHelper.loginWithClientSetupUser();
            APIResponse apiResponse = executor.postMultiPartFormData(getApiPath(), getHeaders(), null).getResponse();
            int responseCode=apiResponse.getResponseCode();
            String responseBody=apiResponse.getResponseBody();
            if(responseCode==200) {
                if(ParseJsonResponse.validJsonResponse(responseBody)) {
                    JSONArray jsonArray = new JSONArray(responseBody);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        if (jsonArray.getJSONObject(i).get("name").toString().equalsIgnoreCase(entityName)) {
                            for (int j = 0; j < jsonArray.getJSONObject(i).getJSONArray("listMetaDataJsons").length(); j++) {
                                if (jsonArray.getJSONObject(i).getJSONArray("listMetaDataJsons").getJSONObject(j).get("name").toString().equalsIgnoreCase(reportName)) {
                                    return true;
                                }
                            }

                        }
                    }
                }
                else
                {
                    customAssert.assertTrue(false,"API { "+getApiPath()+" } response Body on client setup admin is { "+responseBody+" }");
                }
            }
            else
            {
                customAssert.assertTrue(false,"API { "+getApiPath()+" } response code on client setup admin is { "+responseCode+" }");
            }
        } catch (Exception e) {
            logger.error(e.toString());
        }
        finally {
            Check check=new Check();
            check.hitCheck(lastUserName,lastUserPassword);
        }
        return  false;
    }
}
