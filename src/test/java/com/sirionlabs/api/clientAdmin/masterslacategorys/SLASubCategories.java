package com.sirionlabs.api.clientAdmin.masterslacategorys;

import com.sirionlabs.helper.api.TestAPIBase;

import java.util.HashMap;

public class SLASubCategories extends TestAPIBase {



    public static String getApiPath(int slaCategory) {
//        return "/masterslacategorys/show/" + slaCategory + "?ajax=true&_=1589485547681";
        return "/masterslacategorys/show/" + slaCategory;
    }

    public static String getApiPathSubCategory(int slaCategory) {
//        return "/masterslacategorys/show/" + slaCategory + "?ajax=true&_=1589485547681";
        return "/masterslasubcategorys/show/" + slaCategory;
    }

    public static HashMap<String, String> getHeaders() {

        HashMap<String,String> header = new HashMap<>();
        header.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9");

        return header;
    }

    public static String getMasterSlaSubCategorysList(int slaCategory) {

        String responseBody = null;
        try {
            responseBody = executor.get(getApiPath(slaCategory), getHeaders()).getResponse().getResponseBody();
            System.out.println();
        }catch (Exception e){

        }
        return responseBody;
    }

    public static String getMasterSlaSubCategorysListClient(int slaCategory) {

        String responseBody = null;
        try {
            responseBody = executor.get(getApiPathSubCategory(slaCategory), getHeaders()).getResponse().getResponseBody();
            System.out.println();
        }catch (Exception e){

        }
        return responseBody;
    }
}
