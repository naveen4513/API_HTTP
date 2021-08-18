package com.sirionlabs.test;

import com.sirionlabs.api.commonAPI.Clone;
import com.sirionlabs.api.commonAPI.Create;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.json.JSONObject;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class EntityDataCreation {

    String entityName = "obligations";
    int entityId = 57494;

    @DataProvider(name = "entityCreationParallel")
    public Object[][] entityCreationParallel() {

        List<Object[]> allTestData = new ArrayList<>();
        Clone clone = new Clone();
        String cloneResponse = clone.hitCloneV2(entityName,entityId);

        JSONObject cloneResponseJson = new JSONObject(cloneResponse);
        cloneResponseJson.remove("header");
        cloneResponseJson.remove("session");
        cloneResponseJson.remove("actions");
        cloneResponseJson.remove("createLinks");
        cloneResponseJson.getJSONObject("body").remove("layoutInfo");
        cloneResponseJson.getJSONObject("body").remove("globalData");
        cloneResponseJson.getJSONObject("body").remove("errors");

        String createPayload = cloneResponseJson.toString();

        allTestData.add(new Object[]{createPayload});

        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "entityCreationParallel")
    public void EntityCreationByCloning(String createPayload){


        try{

            Create create = new Create();
            for(int i=0;i<10000;i++) {
                create.hitCreate(entityName, createPayload);

            }

        }catch (Exception e){
            System.out.println("Error while creating entity ");
        }


    }


}
