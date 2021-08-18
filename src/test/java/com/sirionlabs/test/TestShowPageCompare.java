package com.sirionlabs.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;
import com.flipkart.zjsonpatch.JsonPatch;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.utils.commonUtils.FileUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class TestShowPageCompare {

    private final static Logger logger = LoggerFactory.getLogger(TestShow.class);

    @DataProvider(name = "contract" )
    public static Object[][] contract(){

        ArrayList<Integer> list = new ArrayList<>(Arrays.asList(19463,19465));
        Object result[][] = new Object[list.size()][1];
        for (int i = 0; i < list.size() ; i++) {
            result[i][0] = list.get(i);

        }
        return result;


    }


    @Test(dataProvider = "contract")
    public void TestContractCompare(int contractid) throws IOException {
        FileUtils util = new FileUtils();
        String str = null;
        try {
            str = util.getDataInFile(".\\src\\test\\resources\\TestData\\ignore.txt");
        } catch (IOException e) {
            logger.error("unable to read ignore.txt file");
        }

        String ignore_urls[] = str.split("\\r?\\n");

        String showApiResponse_source = ShowHelper.getShowResponse(61,19464);
        String showApiResponse_target = ShowHelper.getShowResponse(61,contractid);

        JSONObject obj_source = new JSONObject(showApiResponse_source);
        JSONObject body_source = obj_source.getJSONObject("body").getJSONObject("data");
        JSONObject obj_target = new JSONObject(showApiResponse_target);
        JSONObject body_target = obj_target.getJSONObject("body").getJSONObject("data");

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode node_source = objectMapper.readTree(body_source.toString());
        JsonNode node_target = objectMapper.readTree(body_target.toString());

        JsonNode patch = JsonDiff.asJson(node_source, node_target);
        System.out.println(patch);
        for (int i =0; i < ignore_urls.length;i++){
            for (int j= 0; j < patch.size(); j++){
                if (patch.get(j).get("path").asText().equals(ignore_urls[i])){
                    JsonNode node_patch = objectMapper.readTree("[{\"op\":\"remove\",\"path\":\"/"+j+"\"}]");
                    patch = JsonPatch.apply(node_patch, patch);
                    break;
                }
             }
        }
        System.out.println("************");
        System.out.println(patch.toString());
        Assert.assertEquals(patch.toString(),"[]","Error in contract id --> " +contractid );
    }







}
