package com.sirionlabs.test.serviceLevel;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.metadataSearch.Search;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

public class TestServiceLevelSearchRelatedCases {

    private final static Logger logger = LoggerFactory.getLogger(TestServiceLevelSearchRelatedCases.class);


//    C7618
    @Test
    public void TestSearchTargetFieldsWhenRagNo(){

        CustomAssert customAssert = new CustomAssert();
        int slEntityTypeId = 14;

        try{

            Search search = new Search();
            String payload = "{\"body\":{\"data\":{\"ragApplicable\":{\"name\":\"ragApplicable\",\"id\":11996," +
                    "\"multiEntitySupport\":false,\"values\":{\"name\":\"No\",\"id\":1002}}," +
                    "\"entityTypeId\":{\"name\":\"entityTypeId\",\"values\":" + slEntityTypeId + ",\"multiEntitySupport\":false}," +
                    "\"searchParam\":{\"size\":{\"name\":\"size\",\"values\":20,\"multiEntitySupport\":false}," +
                    "\"offset\":{\"name\":\"offset\",\"values\":0,\"multiEntitySupport\":false}}}}}";

            String searchResponse = search.hitSearch(slEntityTypeId,payload);

            JSONObject searchResponseJson = new JSONObject(searchResponse);

            JSONArray dataArray = searchResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("searchResults").getJSONObject("values").getJSONArray("data");

            JSONObject singleData = dataArray.getJSONObject(0);
            JSONArray singleDataJsonArray = JSONUtility.convertJsonOnjectToJsonArray(singleData);

            String columnName;
            String slId = "";
            for(int i =0;i<singleDataJsonArray.length();i++){

                columnName = singleDataJsonArray.getJSONObject(i).get("columnName").toString();
                if(columnName.equalsIgnoreCase("id")) {
                    slId = singleDataJsonArray.getJSONObject(i).get("value").toString().split(":;")[1];
                    break;
                }
            }

            Show show = new Show();
            show.hitShowVersion2(slEntityTypeId,Integer.parseInt(slId));
            String showResponse = show.getShowJsonStr();

            JSONObject showResponseJSon = new JSONObject(showResponse);

            if(showResponseJSon.getJSONObject("body").getJSONObject("data").getJSONObject("threshold").has("values")){
                customAssert.assertTrue(false,"Threshold values are present for RAG No");
            }

            if(showResponseJSon.getJSONObject("body").getJSONObject("data").getJSONObject("expected").has("values")){
                customAssert.assertTrue(false,"Expected values are present for RAG No");
            }

            if(showResponseJSon.getJSONObject("body").getJSONObject("data").getJSONObject("minimum").has("values")){
                customAssert.assertTrue(false,"Minimum values are present for RAG No");
            }

            if(showResponseJSon.getJSONObject("body").getJSONObject("data").getJSONObject("sigMinMax").has("values")){
                customAssert.assertTrue(false,"sigMinMax values are present for RAG No");
            }

        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating the scenario " + e.getMessage());
        }


        customAssert.assertAll();
    }

//    C7835
//To verify Credit/Earnback Values field are visible on the Metadata search
    @Test
    public void TestSearchCreditEarnBackValues(){

        CustomAssert customAssert = new CustomAssert();
        int cslEntityTypeId = 15;

        try{

            Search search = new Search();

            int finalEarnBackAmountFrom = 100;
            int finalEarnBackAmountTo = 120;
            String payload = "{\"body\":{\"data\":{\"entityTypeId\":{\"name\":\"entityTypeId\",\"values\":" +
                    "" + cslEntityTypeId + ",\"multiEntitySupport\":false},\"searchParam\":{\"size\":{\"name\":\"size\",\"values\":20," +
                    "\"multiEntitySupport\":false},\"offset\":{\"name\":\"offset\",\"values\":0," +
                    "\"multiEntitySupport\":false}},\"finalEarnbackAmountFrom\":{\"name\":" +
                    "\"finalEarnbackAmountFrom\",\"multiEntitySupport\":false,\"values\":" + finalEarnBackAmountFrom + "}," +
                    "\"finalEarnbackAmountTo\":{\"name\":\"finalEarnbackAmountTo\",\"multiEntitySupport\":false,\"values\":" + finalEarnBackAmountTo + "}}}}";

            String searchResponse = search.hitSearch(cslEntityTypeId,payload);

            JSONObject searchResponseJson = new JSONObject(searchResponse);

            JSONArray dataArray = searchResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("searchResults").getJSONObject("values").getJSONArray("data");

            JSONObject singleData = dataArray.getJSONObject(0);
            JSONArray singleDataJsonArray = JSONUtility.convertJsonOnjectToJsonArray(singleData);

            String columnName;
            String slId = "";
            for(int i =0;i<singleDataJsonArray.length();i++){

                columnName = singleDataJsonArray.getJSONObject(i).get("columnName").toString();
                if(columnName.equalsIgnoreCase("id")) {
                    slId = singleDataJsonArray.getJSONObject(i).get("value").toString().split(":;")[1];
                    break;
                }
            }

            Show show = new Show();
            show.hitShowVersion2(cslEntityTypeId,Integer.parseInt(slId));
            String showResponse = show.getShowJsonStr();

            JSONObject showResponseJSon = new JSONObject(showResponse);

            String finalEarnBackAmount = showResponseJSon.getJSONObject("body").getJSONObject("data").getJSONObject("finalEarnbackAmount").get("values").toString();

            Double finalEarnBackAmountInteger = Double.parseDouble(finalEarnBackAmount);

            if((finalEarnBackAmountInteger >= finalEarnBackAmountFrom) || (finalEarnBackAmountInteger <= finalEarnBackAmountFrom)){
                customAssert.assertTrue(true,"Expected and Actual Final EarnBack amount are in the expected Range");
            }else {
                customAssert.assertTrue(false,"Expected and Actual Final EarnBack amount are not in the expected Range");
            }


        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating the scenario " + e.getMessage());
        }


        customAssert.assertAll();
    }

    @Test
    public void TestSearchThreshold(){

        CustomAssert customAssert = new CustomAssert();
        int slEntityTypeId = 14;
        String thresholdValue = "Maximum - 1 level";
        int thresholdId = 6;
        try{

            Search search = new Search();

            String payload = "{\"body\":{\"data\":{\"entityTypeId\":{\"name\":\"entityTypeId\",\"values\":" + slEntityTypeId + "," +
                    "\"multiEntitySupport\":false},\"threshold\":{\"name\":\"threshold\",\"id\":216," +
                    "\"multiEntitySupport\":false,\"values\":{\"name\":\"" + thresholdValue + "\",\"id\":" + thresholdId + "}}," +
                    "\"searchParam\":{\"size\":{\"name\":\"size\",\"values\":20,\"multiEntitySupport\":false}," +
                    "\"offset\":{\"name\":\"offset\",\"values\":0,\"multiEntitySupport\":false}}}," +
                    "\"selectedColumns\":[{\"columnId\":110,\"columnQueryName\":\"id\"}," +
                    "{\"columnId\":111,\"columnQueryName\":\"slaid\"},{\"columnId\":12966,\"columnQueryName\":" +
                    "\"threshold\"}]}}";

            String searchResponse = search.hitSearch(slEntityTypeId,payload);

            JSONObject searchResponseJson = new JSONObject(searchResponse);

            JSONArray dataArray = searchResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("searchResults").getJSONObject("values").getJSONArray("data");

            if(dataArray.length() == 0){
                customAssert.assertTrue(false,"Search Did not returned any result");
                customAssert.assertAll();
            }
            JSONObject singleData = dataArray.getJSONObject(0);
            JSONArray singleDataJsonArray = JSONUtility.convertJsonOnjectToJsonArray(singleData);

            String columnName;
            String slId = "";
            for(int i =0;i<singleDataJsonArray.length();i++){

                columnName = singleDataJsonArray.getJSONObject(i).get("columnName").toString();
                if(columnName.equalsIgnoreCase("id")) {
                    slId = singleDataJsonArray.getJSONObject(i).get("value").toString().split(":;")[1];
                    break;
                }
            }

            Show show = new Show();
            show.hitShowVersion2(slEntityTypeId,Integer.parseInt(slId));
            String showResponse = show.getShowJsonStr();

            JSONObject showResponseJSon = new JSONObject(showResponse);

            String threshold = showResponseJSon.getJSONObject("body").getJSONObject("data").getJSONObject("threshold").getJSONObject("values").get("name").toString();

            if(!threshold.equalsIgnoreCase(thresholdValue)){
                customAssert.assertTrue(false,"Expected and Actual Threshold value doesn't match");
            }


        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating the scenario " + e.getMessage());
        }


        customAssert.assertAll();
    }

    @Test
    public void TestSearchSlMetStatus(){

        CustomAssert customAssert = new CustomAssert();
        int cslEntityTypeId = 15;
        String expectedSlMet = "Met Sig Min";


        List<String> expectedSLMetStatus = Arrays.asList("Met Expected","Met Minimum","Met Sig Min","Not Met","Low Volume","Not Reported","No Data Available","Work In Progress","Not Applicable");

        try{

            Search search = new Search();

            String searchResponse= search.hitSearch(cslEntityTypeId);

            JSONObject searchResponseJson = new JSONObject(searchResponse);

            JSONArray optionsDataArray = searchResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("slMet").getJSONObject("options").getJSONArray("data");
            String optionName;
            for(int i =0;i<optionsDataArray.length();i++){

                optionName = optionsDataArray.getJSONObject(i).get("name").toString();
                if(!expectedSLMetStatus.contains(optionName)){
                    customAssert.assertTrue(false,optionName + "not present in the list of options");
                }

            }


            String payload = "{\"body\":{\"data\":{\"slMet\":{\"name\":\"slMet\",\"id\":1151,\"multiEntitySupport\":false,\"" +
                    "values\":{\"name\":\"" + expectedSlMet + "\",\"id\":9,\"active\":true,\"blocked\":false,\"createdFromListPage\":false," +
                    "\"summaryGroupData\":false,\"bulkOperation\":false,\"blockedForBulk\":false,\"autoExtracted" +
                    "\":false,\"systemAdmin\":false,\"canOverdue\":false,\"autoCreate\":false,\"validationError\":false," +
                    "\"isReject\":false,\"parentHalting\":false,\"autoTaskFailed\":false,\"compareHistory\":false," +
                    "\"flagForClone\":false,\"createStakeHolder\":false,\"escapeValueUpdateTask\":false,\"excludeFromHoliday\":false," +
                    "\"excludeWeekends\":false,\"datetimeEnabled\":false,\"uploadAllowed\":false,\"downloadAllowed\":false,\"signatureAllowed\"" +
                    ":false,\"saveCommentDocOnValueUpdate\":false,\"colorCode\":\"#F7B74F\",\"shortCode\":\"" +
                    "Met Sig Min\",\"orgPrefShortCode\":\"Met Sig Min\",\"descriptionNeeded\":true,\"orgPrefDesc\":" +
                    "\"Met Significantly Minimum\",\"systemId\":9,\"orderSeq\":3,\"overdue\":false,\"autoTask\":false}}," +
                    "\"entityTypeId\":{\"name\":\"entityTypeId\",\"values\":" + cslEntityTypeId + ",\"multiEntitySupport\":false},\"searchParam\":" +
                    "{\"size\":{\"name\":\"size\",\"values\":20,\"multiEntitySupport\":false},\"offset\":{\"name\":\"offset\",\"values\":0," +
                    "\"multiEntitySupport\":false}}}}}";

            searchResponse = search.hitSearch(cslEntityTypeId,payload);

            searchResponseJson = new JSONObject(searchResponse);

            JSONArray dataArray = searchResponseJson.getJSONObject("body").getJSONObject("data").getJSONObject("searchResults").getJSONObject("values").getJSONArray("data");

            if(dataArray.length() == 0){
                customAssert.assertTrue(false,"Search Did not returned any result");
                customAssert.assertAll();
            }
            JSONObject singleData = dataArray.getJSONObject(0);
            JSONArray singleDataJsonArray = JSONUtility.convertJsonOnjectToJsonArray(singleData);

            String columnName;
            String cslId = "";
            for(int i =0;i<singleDataJsonArray.length();i++){

                columnName = singleDataJsonArray.getJSONObject(i).get("columnName").toString();
                if(columnName.equalsIgnoreCase("id")) {
                    cslId = singleDataJsonArray.getJSONObject(i).get("value").toString().split(":;")[1];
                    break;
                }
            }

            Show show = new Show();
            show.hitShowVersion2(cslEntityTypeId,Integer.parseInt(cslId));
            String showResponse = show.getShowJsonStr();

            JSONObject showResponseJSon = new JSONObject(showResponse);

            String slMet = showResponseJSon.getJSONObject("body").getJSONObject("data").getJSONObject("slMet").getJSONObject("values").get("name").toString();

            if(!expectedSlMet.equalsIgnoreCase(slMet)){
                customAssert.assertTrue(false,"Expected and Actual slMet value doesn't match");
            }


        }catch (Exception e){
            customAssert.assertTrue(false,"Exception while validating the scenario " + e.getMessage());
        }


        customAssert.assertAll();
    }

}
