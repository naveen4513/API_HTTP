package com.sirionlabs.api.massEmail;

import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;

public class CreateForm extends TestAPIBase {

    public static String getApiPath(int id) {
        String url = "/massEmail/createForm?id=";
        url = (id != -1) ? url.concat(String.valueOf(id)) : url.concat("undefined");

        return url;
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }

    public static String getCreateFormResponse(int id) {
        return executor.get(getApiPath(id), getHeaders()).getResponse().getResponseBody();
    }

    public static int getIdForUser(String createFormResponse, String userName) {
        try {
            JSONObject jsonObj = new JSONObject(createFormResponse);
            JSONArray jsonArr = jsonObj.getJSONArray("allUsers");

            for (int i = 0; i < jsonArr.length(); i++) {
                if (jsonArr.getJSONObject(i).getString("name").equalsIgnoreCase(userName)) {
                    return jsonArr.getJSONObject(i).getInt("id");
                }
            }
        } catch (Exception e) {
            return -1;
        }

        return -1;
    }
}