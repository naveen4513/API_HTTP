package com.sirionlabs.api.clientAdmin.templateType;

import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TemplateTypeList extends TestAPIBase {

    public static String getAPIPath() {
        return "/templateTypeList/list";
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
    }

    public static String getTemplateTypeListResponse() {
        return executor.get(getAPIPath(), getHeaders()).getResponse().getResponseBody();
    }

    public static List<String> getAllActiveTemplateTypes(String templateTypeListResponse) {
        List<String> allActiveTemplateTypes = new ArrayList<>();

        try {
            Elements nodes = Jsoup.parse(templateTypeListResponse).getElementById("l_com_sirionlabs_clauselibrary_model_TemplateType").child(1).children();

            for (int i = 0; i < nodes.size() - 1; i++) {
                String activeValue = nodes.get(i).child(2).child(0).childNode(0).toString().trim().replace("\n", "");

                if (activeValue.equalsIgnoreCase("Yes")) {
                    String templateTypeName = nodes.get(i).child(1).child(0).childNode(0).toString().trim().replace("\n", "");
                    allActiveTemplateTypes.add(templateTypeName);
                }
            }
        } catch (Exception e) {
            return null;
        }

        return allActiveTemplateTypes;
    }
}