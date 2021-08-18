package com.sirionlabs.helper.customField;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

public class CustomFieldHelper {

    private  String customFieldDYN = "";

    public void findCustomFieldNodeFromJson(JSONObject jsonObject, String customField){

        Iterator<String> iterator = jsonObject.keys();
        String nextKey;
        while (iterator.hasNext()){
            nextKey = iterator.next();
            if(nextKey.equalsIgnoreCase("label")){
                if(jsonObject.get("label").toString().equalsIgnoreCase(customField)) {
                    customFieldDYN = jsonObject.get("name").toString();
                    jsonObject.get("name").toString();
                }
            }
            else{
                if(jsonObject.get(nextKey) instanceof JSONObject){
                    findCustomFieldNodeFromJson((JSONObject) jsonObject.get(nextKey), customField);
                }
                else if(jsonObject.get(nextKey) instanceof JSONArray)
                    findCustomFieldNodeFromJson((JSONArray) jsonObject.get(nextKey), customField);
            }
        }
    }

    public void findCustomFieldNodeFromJson(JSONArray jsonArray, String customField){

        for(int index =0;index<jsonArray.length();index++){
            if(jsonArray.get(index) instanceof  JSONArray)
                findCustomFieldNodeFromJson((JSONArray) jsonArray.get(index), customField);
            else if(jsonArray.get(index) instanceof  JSONObject)
                findCustomFieldNodeFromJson((JSONObject) jsonArray.get(index), customField);
        }
    }

    public String getCustomFieldDYN(){
        return customFieldDYN;
    }

}
