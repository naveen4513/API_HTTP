package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.api.listRenderer.UserListMetaData;
import com.sirionlabs.api.userPreference.UserPreferenceData;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class TestAutoExtractionView {
    private final static Logger logger = LoggerFactory.getLogger(TestAutoExtractionView.class);
    private UserPreferenceData userPreferenceObj = new UserPreferenceData();
    private int privateViewId = -1;
    private int publicViewId=-1;
    UserPreferenceData userPreferenceData=new UserPreferenceData();

    @Test(priority = 0,enabled = true)
    public void testSavePrivateView()
    {
        CustomAssert csAssert=new CustomAssert();
        try{
            HttpResponse listDataResponse= AutoExtractionHelper.aeDocListing();
            String listDataResponseStr= EntityUtils.toString(listDataResponse.getEntity());
            if(ParseJsonResponse.validJsonResponse(listDataResponseStr))
            {
                logger.info("Validating Private View Creation on Auto Extraction Listing page");
                validatePrivateViewCreation(csAssert);
            }
            else
            {
                csAssert.assertTrue(false, "ListData API Response for Auto Extraction is an Invalid JSON." );
            }

        }
        catch (Exception e)
        {
            csAssert.assertTrue(false, "Exception while Validating Auto Extraction Private Save View. " + e.getMessage());
        }
        csAssert.assertAll();
    }
    //TC: C153790: Verify Private View can be deleted successfully.
    @Test(priority = 1,enabled = true)
    public void testDeletePrivateView()
    {
        CustomAssert csAssert = new CustomAssert();

        try {
            //Edit and Delete Private View.
            if (privateViewId != -1) {
                //Delete Private View
                logger.info("Deleting Private View having Id: {}", privateViewId);
                logger.info("Hitting User Preference Delete API for Private View having Id {}", privateViewId);
                userPreferenceObj.hitUserPreferenceDeleteAPI(432, privateViewId, false);
                String deleteViewResponse = userPreferenceObj.getResponseDeleteUserPreference();

                if (ParseJsonResponse.validJsonResponse(deleteViewResponse)) {
                    JSONArray jsonArr = new JSONArray(deleteViewResponse);

                    for (int i = 0; i < jsonArr.length(); i++) {
                        int viewId = jsonArr.getJSONObject(i).getInt("id");

                        if (viewId == privateViewId) {
                            csAssert.assertTrue(false, "Couldn't Delete Private View having Id " + privateViewId);
                        }
                    }
                } else {
                    csAssert.assertTrue(false, "Delete View API Response for Private View having Id " + privateViewId + " is an Invalid JSON.");
                }
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Private Delete View due to " + e.getMessage());
        }
        csAssert.assertAll();
    }
    @Test(priority = 2,enabled = true)
    public void testSavePublicView()
    {
        CustomAssert csAssert=new CustomAssert();
        try{

            HttpResponse listDataResponse= AutoExtractionHelper.aeDocListing();
            String listDataResponseStr= EntityUtils.toString(listDataResponse.getEntity());
            if(ParseJsonResponse.validJsonResponse(listDataResponseStr))
            {
                logger.info("Validating Public View Creation on Auto Extraction Listing page");
                validatePublicViewCreation(csAssert);
            }
            else
            {
                csAssert.assertTrue(false, "ListData API Response for Auto Extraction is an Invalid JSON." );
            }

        }
        catch (Exception e)
        {
            csAssert.assertTrue(false, "Exception while Validating Auto Extraction Public Save View. " + e.getMessage());
        }
        csAssert.assertAll();
    }

    //TC:C153791: Verify Public preferred View can be deleted successfully.
    @Test(priority = 3,enabled = true)
    public void testDeletePublicView()
    {
        CustomAssert csAssert = new CustomAssert();

        try {
            if (publicViewId != -1) {
                //Delete Public View
                logger.info("Deleting Private View having Id: {}", publicViewId);
                logger.info("Hitting User Preference Delete API for Public View having Id {}", publicViewId);
                userPreferenceObj.hitUserPreferenceDeleteAPI(432, publicViewId, true);
                String deleteViewResponse = userPreferenceObj.getResponseDeleteUserPreference();

                if (ParseJsonResponse.validJsonResponse(deleteViewResponse)) {
                    JSONArray jsonArr = new JSONArray(deleteViewResponse);

                    for (int i = 0; i < jsonArr.length(); i++) {
                        int viewId = jsonArr.getJSONObject(i).getInt("id");

                        if (viewId == publicViewId) {
                            csAssert.assertTrue(false, "Couldn't Delete Public View having Id " + publicViewId);
                        }
                    }
                } else {
                    csAssert.assertTrue(false, "Delete View API Response for Public View having Id " + publicViewId + " is an Invalid JSON.");
                }
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Delete public View due to " + e.getMessage());
        }
        csAssert.assertAll();
    }
    @Test(priority = 4,enabled = true)
    public void testSaveSearchResultView()
    {
        CustomAssert csAssert=new CustomAssert();
        try{
            logger.info("Hitting Automation Doc Listing API");
            HttpResponse listDataResponse= AutoExtractionHelper.aeDocListing();
            String listDataResponseStr= EntityUtils.toString(listDataResponse.getEntity());
            if(ParseJsonResponse.validJsonResponse(listDataResponseStr))
            {
                logger.info("Validating Public View Creation for Search Results on Auto Extraction Listing page");
                validateSearchResultPublicViewCreation(csAssert);
            }
            else
            {
                csAssert.assertTrue(false, "ListData API Response for Auto Extraction is an Invalid JSON." );
            }

        }
        catch (Exception e)
        {
            csAssert.assertTrue(false, "Exception while Validating Auto Extraction Public Save View. " + e.getMessage());
        }
        csAssert.assertAll();
    }
    @Test(priority = 5,enabled = true)
    public void testDeleteSearchPublicView()
    {
        CustomAssert csAssert = new CustomAssert();

        try {
            if (publicViewId != -1) {
                //Delete Public View
                logger.info("Deleting Private View having Id: {}", publicViewId);
                logger.info("Hitting User Preference Delete API for Public View having Id {}", publicViewId);
                userPreferenceObj.hitUserPreferenceDeleteAPI(432, publicViewId, true);
                String deleteViewResponse = userPreferenceObj.getResponseDeleteUserPreference();

                if (ParseJsonResponse.validJsonResponse(deleteViewResponse)) {
                    JSONArray jsonArr = new JSONArray(deleteViewResponse);

                    for (int i = 0; i < jsonArr.length(); i++) {
                        int viewId = jsonArr.getJSONObject(i).getInt("id");

                        if (viewId == publicViewId) {
                            csAssert.assertTrue(false, "Couldn't Delete Public View having Id " + publicViewId);
                        }
                    }
                } else {
                    csAssert.assertTrue(false, "Delete View API Response for Public View having Id " + publicViewId + " is an Invalid JSON.");
                }
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Delete public View due to " + e.getMessage());
        }
        csAssert.assertAll();
    }

    public String getPayloadForViewCreation(String viewName, Boolean isPublic) {
        return "{\"filterJson\":\"{\\\"filterMap\\\":{\\\"entityTypeId\\\":316,\\\"offset\\\":0,\\\"size\\\":20,\\\"orderByColumnName\\\":\\\"id\\\"," +
                "\\\"orderDirection\\\":\\\"desc nulls last\\\",\\\"filterJson\\\":{\\\"368\\\":{\\\"filterId\\\":\\\"368\\\",\\\"filterName\\\":\\\"statusId\\\"," +
                "\\\"entityFieldId\\\":null,\\\"entityFieldHtmlType\\\":null,\\\"multiselectValues\\\":{\\\"SELECTEDDATA\\\":[{\\\"id\\\":\\\"4\\\"," +
                "\\\"name\\\":\\\"COMPLETED\\\"}]}}}},\\\"selectedColumns\\\":[]}\",\"maxNumberOfColumns\":8,\"listId\":\"432\",\"name\":\""+viewName+"\"," +
                "\"columns\":[{},{\"id\":17296,\"listId\":432,\"name\":\"ID\",\"defaultName\":\"ID\",\"order\":2,\"type\":\"TEXT\"," +
                "\"displayFormat\":\"{\\\"type\\\":\\\"url\\\",\\\"entityTypeId\\\":316, \\\"isId\\\":true, \\\"isAEDoc\\\":true}\",\"queryName\":\"id\"," +
                "\"impactsPerformance\":false,\"includeInGridView\":false,\"deleted\":false,\"isExcel\":null,\"entityFieldId\":null,\"fieldId\":null,\"hoverValue\":null}]," +
                "\"listViewType\":{\"name\":\"List\",\"id\":1},\"publicVisibility\":"+isPublic+"}";
    }

    //TC: C153733: Preferred View as -Private
    public void validatePrivateViewCreation(CustomAssert csAssert) throws Exception {
        try {
            String privateViewName = "Test API Auto Extraction Private View";
            logger.info("Getting payload for private View creation");
            String payload = getPayloadForViewCreation(privateViewName, false);
            logger.info("Hitting save view API for Private view creation");
            userPreferenceObj.hitUserPreferenceCreateAPI(432, payload);
            String saveViewResponse = userPreferenceObj.getResponseCreateUserPreference();

            if (ParseJsonResponse.validJsonResponse(saveViewResponse)) {
                JSONArray preferenceJsonArr = new JSONArray(saveViewResponse);
                boolean privateViewCreatedSuccessfully = false;

                for (int j = 0; j < preferenceJsonArr.length(); j++) {
                    JSONObject preferenceJsonObj = preferenceJsonArr.getJSONObject(j);

                    if (preferenceJsonObj.getString("name").equalsIgnoreCase(privateViewName)) {
                        privateViewCreatedSuccessfully = true;

                        String visibility = preferenceJsonObj.getString("visibility");

                        if (!visibility.equalsIgnoreCase("My View")) {
                            csAssert.assertTrue(false, "Created Private View: [" + privateViewName + "] but Visibility is not Private");
                        }

                        privateViewId = preferenceJsonObj.getInt("id");
                        logger.info("Private View created on Auto Extraction listing page with id "+privateViewId);
                        break;
                    }
                }

                csAssert.assertTrue(privateViewCreatedSuccessfully, "Couldn't Save Private View with Name: [" + privateViewName + "]");

                UserListMetaData metaDataObj = new UserListMetaData();

                Map<String, String> params = new HashMap<>();
                params.put("preferenceId", String.valueOf(privateViewId));
                params.put("contractId", "");
                params.put("publicVisibility", "false");
                logger.info("Validating whether created private view can be applied on AE Listing page");
                String userListMetaDataResponseCode = metaDataObj.hitUserListMetaData(432, params);

                if (!userListMetaDataResponseCode.equalsIgnoreCase("200")) {
                    csAssert.assertTrue(false, "Couldn't Apply Private View: [" + privateViewName + "] on Auto Extraction Listing Page.");
                }
            } else {
                csAssert.assertTrue(false, "Couldn't Save Private View with Name: [" + privateViewName + "].");
            }
            verifySaveDefaultView(privateViewId,privateViewName);
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Private View Creation.");
        }
        csAssert.assertAll();
    }

    //TC: C153734: Preferred View as-Public
    private void validatePublicViewCreation(CustomAssert csAssert) {
        try {
            String publicViewName = "Test API Automation Auto Extraction Public View";
            logger.info("Getting payload for public view creation on Auto Extraction Listing page");
            String payload = getPayloadForViewCreation(publicViewName, true);
            logger.info("Hitting save API for Public View creation");
            userPreferenceObj.hitUserPreferenceCreateAPI(432, payload);
            String saveViewResponse = userPreferenceObj.getResponseCreateUserPreference();

            if (ParseJsonResponse.validJsonResponse(saveViewResponse)) {
                JSONArray preferenceJsonArr = new JSONArray(saveViewResponse);
                boolean publicViewCreatedSuccessfully = false;

                for (int j = 0; j < preferenceJsonArr.length(); j++) {
                    JSONObject preferenceJsonObj = preferenceJsonArr.getJSONObject(j);

                    if (preferenceJsonObj.getString("name").equalsIgnoreCase(publicViewName)) {
                        publicViewCreatedSuccessfully = true;

                        String visibility = preferenceJsonObj.getString("visibility");

                        if (!visibility.equalsIgnoreCase("Shared By Me")) {
                            csAssert.assertTrue(false, "Created Public View: [" + publicViewName + "] but Visibility is not Public");
                        }
                        publicViewId = preferenceJsonObj.getInt("id");
                        logger.info("Public View has been created with id "+publicViewName);
                        break;
                    }
                }

                csAssert.assertTrue(publicViewCreatedSuccessfully, "Couldn't Save Public View with Name: [" + publicViewName + "]");

                UserListMetaData metaDataObj = new UserListMetaData();

                Map<String, String> params = new HashMap<>();
                params.put("preferenceId", String.valueOf(publicViewId));
                params.put("contractId", "");
                params.put("publicVisibility", "true");
                logger.info("Applying created view on AE Listing page");
                String userListMetaDataResponseCode = metaDataObj.hitUserListMetaData(432, params);

                if (!userListMetaDataResponseCode.equalsIgnoreCase("200")) {
                    csAssert.assertTrue(false, "Couldn't Apply Public View: [" + publicViewName + "] on Auto Extraction list page.");
                }
            } else {
                csAssert.assertTrue(false, "Couldn't Save Public View with Name: [" + publicViewName + "].");
            }
            verifySaveDefaultView(publicViewId,publicViewName);
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Public View Creation.");
        }
        csAssert.assertAll();
    }

    //TC:C153735: Preferred View as-Default
    public void verifySaveDefaultView(int viewId, String ViewName) throws Exception {
        CustomAssert customAssert=new CustomAssert();

        logger.info("Starting Test :Set saved View as Default View [{}]", Thread.currentThread().getStackTrace()[1].getMethodName());
        CustomAssert csAssert = new CustomAssert();
        logger.info("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
        logger.info("User Preference View Id:{} <---which is getting set as defaultUserPreference ", viewId);

        HttpResponse response = userPreferenceData.hitSaveDefaultUserPreferenceAPI(432, viewId);
        customAssert.assertTrue(userPreferenceData.getStatusCodeFrom(response).contains("200"), "Error :Save Default User Preference API Status Code is Incorrect");

        HttpResponse responseHttp = userPreferenceData.hitUserPreferenceListAPI(432);
        customAssert.assertTrue(userPreferenceData.getStatusCodeFrom(responseHttp).contains("200"), "Error :List User Preference API Status Code is Incorrect");

        String listUserPreferneceAPIResponse = userPreferenceData.getResponselistUserPreference();
        customAssert.assertTrue(userPreferenceData.checkWhetherViewIdIsDefaultUserView(listUserPreferneceAPIResponse, ViewName), "Error: Save Default User View API is Not Setting the given view as Default User View");
        csAssert.assertAll();
        logger.info("---------------------------------------------------------Ending : [{}]---------------------------------------------------------------", Thread.currentThread().getStackTrace()[1].getMethodName());

    }


    private void validateSearchResultPublicViewCreation(CustomAssert csAssert) {
        try {
            String publicViewName = "Test API Automation Search Result Auto Extraction Public View";
            logger.info("Getting payload for public view creation on Auto Extraction Listing page");
            String documentName="Doc File API Automation.doc";
            String payload = getPayloadForViewCreationForSearch(publicViewName, true,documentName);
            logger.info("Hitting save API for Public View creation");
            userPreferenceObj.hitUserPreferenceCreateAPI(432, payload);
            String saveViewResponse = userPreferenceObj.getResponseCreateUserPreference();

            if (ParseJsonResponse.validJsonResponse(saveViewResponse)) {
                JSONArray preferenceJsonArr = new JSONArray(saveViewResponse);
                boolean publicViewCreatedSuccessfully = false;

                for (int j = 0; j < preferenceJsonArr.length(); j++) {
                    JSONObject preferenceJsonObj = preferenceJsonArr.getJSONObject(j);

                    if (preferenceJsonObj.getString("name").equalsIgnoreCase(publicViewName)) {
                        publicViewCreatedSuccessfully = true;

                        String visibility = preferenceJsonObj.getString("visibility");

                        if (!visibility.equalsIgnoreCase("Shared By Me")) {
                            csAssert.assertTrue(false, "Created Public View: [" + publicViewName + "] but Visibility is not Public");
                        }
                        publicViewId = preferenceJsonObj.getInt("id");
                        logger.info("Public View has been created with id "+publicViewName);
                        break;
                    }
                }

                csAssert.assertTrue(publicViewCreatedSuccessfully, "Couldn't Save Public View with Name: [" + publicViewName + "]");

                UserListMetaData metaDataObj = new UserListMetaData();

                Map<String, String> params = new HashMap<>();
                params.put("preferenceId", String.valueOf(publicViewId));
                params.put("contractId", "");
                params.put("publicVisibility", "true");
                logger.info("Applying created view on AE Listing page");
                String userListMetaDataResponseCode = metaDataObj.hitUserListMetaData(432, params);

                if (!userListMetaDataResponseCode.equalsIgnoreCase("200")) {
                    csAssert.assertTrue(false, "Couldn't Apply Public View: [" + publicViewName + "] on Auto Extraction list page.");
                }
            } else {
                csAssert.assertTrue(false, "Couldn't Save Public View with Name: [" + publicViewName + "].");
            }
            verifySaveDefaultView(publicViewId,publicViewName);
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Public View Creation.");
        }
        csAssert.assertAll();
    }
    public String getPayloadForViewCreationForSearch(String viewName, Boolean isPublic,String documentName) {
        return "{\"filterJson\":\"{\\\"filterMap\\\":{\\\"entityTypeId\\\":316,\\\"offset\\\":0,\\\"size\\\":100,\\\"orderByColumnName\\\":\\\"id\\\"," +
                "\\\"orderDirection\\\":\\\"desc nulls last\\\",\\\"filterJson\\\":{},\\\"searchName\\\":\\\"\\\\\\\""+documentName+"\\\\\\\"\\\"}," +
                "\\\"selectedColumns\\\":[]}\",\"maxNumberOfColumns\":8,\"listId\":\"432\",\"name\":\""+viewName+"\",\"columns\":[{\"id\":20265,\"listId\":432," +
                "\"name\":\"ID\",\"defaultName\":\"ID\",\"order\":2,\"type\":\"TEXT\",\"displayFormat\":\"{\\\"type\\\":\\\"url\\\",\\\"entityTypeId\\\":316, " +
                "\\\"isId\\\":true, \\\"isAEDoc\\\":true}\",\"queryName\":\"id\",\"impactsPerformance\":false,\"includeInGridView\":false,\"deleted\":false," +
                "\"isExcel\":null,\"entityFieldId\":null,\"fieldId\":null,\"hoverValue\":null}],\"listViewType\":{\"name\":\"List\",\"id\":1},\"publicVisibility\":"+isPublic+"}";
    }

}
