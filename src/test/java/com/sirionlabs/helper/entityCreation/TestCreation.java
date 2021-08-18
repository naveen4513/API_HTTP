package com.sirionlabs.helper.entityCreation;

import com.sirionlabs.api.commonAPI.New;
import com.sirionlabs.helper.api.APIExecutor;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class FieldsWithTypes {

    private int fieldType;
    private String fieldId;
    private boolean isStakeholder,isDynamicField;

    FieldsWithTypes(int type, String id,boolean isDynamicField, boolean isStakeholder) {
        this.fieldId = id;
        this.fieldType = type;
        this.isDynamicField = isDynamicField;
        this.isStakeholder = isStakeholder;
    }

    public boolean isStakeholder() {
        return isStakeholder;
    }

    public boolean isDynamicField() {
        return isDynamicField;
    }

    public int getFieldType() {
        return fieldType;
    }

    public String getFieldId() {
        return fieldId;
    }
}

public class TestCreation extends TestAPIBase {

    private static final Logger logger = LoggerFactory.getLogger(TestCreation.class);

    private List<FieldsWithTypes> requiredFields = new ArrayList<>();

    public String extractRequiredFields(String jsonResponse) {
        setUp();

        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            getNodeFromJsonForValueGeneric(jsonObject.getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent"), "validations");
            JSONObject payload = new JSONObject();
            //payload.put("data",);
            payload.put("body", new JSONObject().put("data", jsonObject.getJSONObject("body").getJSONObject("data")));

            logger.info("Finished");

        } catch (Exception e) {
            logger.error("Exception generated");
        }

        return null;
    }


    private void getNodeFromJsonForValueGeneric(JSONObject jsonObject, String key) {
        Iterator<String> iterator = jsonObject.keys();
        String nextKey;
        while (iterator.hasNext()) {
            nextKey = iterator.next();
            if (key.equalsIgnoreCase(nextKey)) {
                if (jsonObject.get(nextKey) instanceof JSONArray) {
                    if (jsonObject.getJSONArray(nextKey).length() > 0) {
                        for (Object validationObject : jsonObject.getJSONArray(nextKey))
                            if (validationObject instanceof JSONObject)
                                if (((JSONObject) validationObject).has("messageLabelId")) {
                                    if (((JSONObject) validationObject).getInt("messageLabelId") == 8078) {

                                        if (jsonObject.getInt("id") > 100000)
                                            requiredFields.add(new FieldsWithTypes(jsonObject.getInt("editableHtmlType"), jsonObject.getString("name"),false,true));
                                        else
                                            requiredFields.add(new FieldsWithTypes(jsonObject.getInt("editableHtmlType"), String.valueOf(jsonObject.getInt("id")),false,false));
                                    }
                                } else
                                    logger.error("Validation object is not JsonObject type");
                    }
                } else
                    logger.error("String node expected but found {}", jsonObject.get(nextKey));
            } else {
                if (jsonObject.get(nextKey) instanceof JSONObject) {
                    getNodeFromJsonForValueGeneric((JSONObject) jsonObject.get(nextKey), key);
                } else if (jsonObject.get(nextKey) instanceof JSONArray)
                    getNodeFromJsonForValueGeneric((JSONArray) jsonObject.get(nextKey), key);
            }
        }
    }

    private void getNodeFromJsonForValueGeneric(JSONArray jsonArray, String key) {

        for (int index = 0; index < jsonArray.length(); index++) {
            if (jsonArray.get(index) instanceof JSONArray)
                getNodeFromJsonForValueGeneric((JSONArray) jsonArray.get(index), key);
            else if (jsonArray.get(index) instanceof JSONObject)
                getNodeFromJsonForValueGeneric((JSONObject) jsonArray.get(index), key);
        }
    }

    private void setNodeValues(JSONObject jsonObject, FieldsWithTypes fieldsWithTypes) {
        Iterator<String> iterator = jsonObject.keys();
        String nextKey;
        if(!fieldsWithTypes.isDynamicField()&&!fieldsWithTypes.isStakeholder()){
            while (iterator.hasNext()) {
                nextKey = iterator.next();
                if (jsonObject.get(nextKey) instanceof JSONObject) {
                    if (jsonObject.getJSONObject(nextKey).has("id")) {
                        if (jsonObject.getJSONObject(nextKey).getInt("id") == Integer.parseInt(fieldsWithTypes.getFieldId())) {
                            if(!(jsonObject.getJSONObject(nextKey).has("value")||jsonObject.getJSONObject(nextKey).has("values"))){
                                editFieldInNode(jsonObject.getJSONObject(nextKey), fieldsWithTypes);
                            }

                        }
                    } else {
                        if (jsonObject.get(nextKey) instanceof JSONObject) {
                            setNodeValues((JSONObject) jsonObject.get(nextKey),fieldsWithTypes );
                        } else if (jsonObject.get(nextKey) instanceof JSONArray)
                            setNodeValues((JSONArray) jsonObject.get(nextKey), fieldsWithTypes);
                    }
                } else
                    setNodeValues((JSONArray) jsonObject.get(nextKey), fieldsWithTypes);
            }
        }
        else if(fieldsWithTypes.isStakeholder()){
            while (iterator.hasNext()) {
                nextKey = iterator.next();
                if (jsonObject.get(nextKey) instanceof JSONObject) {
                    if(jsonObject.getJSONObject(nextKey).has("values")){
                        if (jsonObject.getJSONObject(nextKey).getJSONObject("values").has(fieldsWithTypes.getFieldId())) {
                            if(!(jsonObject.getJSONObject(nextKey).getJSONObject("values").getJSONObject(fieldsWithTypes.getFieldId()).has("value")||jsonObject.getJSONObject(nextKey).getJSONObject("values").getJSONObject(fieldsWithTypes.getFieldId()).has("values")))
                                editFieldInNode(jsonObject.getJSONObject(nextKey), fieldsWithTypes);
                            else if(jsonObject.getJSONObject(nextKey).getJSONObject("values").getJSONObject(fieldsWithTypes.getFieldId()).get("value")==null&&jsonObject.getJSONObject(nextKey).getJSONObject("values").getJSONObject(fieldsWithTypes.getFieldId()).get("values")==null)
                                editFieldInNode(jsonObject.getJSONObject(nextKey), fieldsWithTypes);

                        } else {
                            if (jsonObject.get(nextKey) instanceof JSONObject) {
                                setNodeValues((JSONObject) jsonObject.get(nextKey),fieldsWithTypes );
                            } else if (jsonObject.get(nextKey) instanceof JSONArray)
                                setNodeValues((JSONArray) jsonObject.get(nextKey), fieldsWithTypes);
                        }
                    }
                    else {
                        if (jsonObject.get(nextKey) instanceof JSONObject) {
                            setNodeValues((JSONObject) jsonObject.get(nextKey),fieldsWithTypes );
                        } else if (jsonObject.get(nextKey) instanceof JSONArray)
                            setNodeValues((JSONArray) jsonObject.get(nextKey), fieldsWithTypes);
                    }
                } else
                    setNodeValues((JSONArray) jsonObject.get(nextKey), fieldsWithTypes);
            }
        }
    }

    private void setNodeValues(JSONArray jsonArray, FieldsWithTypes fieldsWithTypes) {

        for (int index = 0; index < jsonArray.length(); index++) {
            if (jsonArray.get(index) instanceof JSONArray)
                setNodeValues((JSONArray) jsonArray.get(index), fieldsWithTypes);
            else if (jsonArray.get(index) instanceof JSONObject)
                setNodeValues((JSONObject) jsonArray.get(index),fieldsWithTypes);
        }
    }

    private Object generateFieldValues(int fieldTypeId) {
        switch (fieldTypeId) {
            case 1:
                return "auto text " + RandomNumbers.getRandomNumberWithinRangeIndex(1000, 9999);
            case 2:
                return "auto text area" + RandomNumbers.getRandomNumberWithinRangeIndex(1000, 9999);
            case 8:
                return "05-01-2018";
            case 10:
                return "auto text label" + RandomNumbers.getRandomNumberWithinRangeIndex(1000, 9999);
            case 18:
            case 19:
                return RandomNumbers.getRandomNumberWithinRangeIndex(100, 999);
            case 20:
                return "05-01-2018 00:00:00";
            default:
                return null;
        }
    }

    private void editFieldInNode(JSONObject jsonObject, FieldsWithTypes fieldsWithTypes) {
        Object valueObject = generateFieldValues(fieldsWithTypes.getFieldType());
        if (valueObject != null)
            jsonObject.put("value", valueObject);
        else {
            if (jsonObject.has("options")) {
                boolean autocomplete = jsonObject.getJSONObject("options").getBoolean("autoComplete");
                if (!autocomplete) {
                    if (jsonObject.getJSONObject("options").has("data"))
                        if (jsonObject.getJSONObject("options").get("data") instanceof JSONArray) {
                            int length = jsonObject.getJSONObject("options").getJSONArray("data").length();
                            if (length > 0) {
                                if (fieldsWithTypes.getFieldType() == 3 || fieldsWithTypes.getFieldType() == 11||fieldsWithTypes.getFieldType() == 4 || fieldsWithTypes.getFieldType() == 12) {
                                    jsonObject.put("value", jsonObject.getJSONObject("options").getJSONArray("data").getJSONObject(RandomNumbers.getRandomNumberWithinRangeIndex(0, length - 1)));
                                }
                            }
                        }
                }
                else{
                    if(fieldsWithTypes.isStakeholder()){

                    }
                }
            }
        }
        jsonObject.put("options", JSONObject.NULL);
    }

    private Object getFieldType(String fieldId) {
        for (FieldsWithTypes fieldsWithTypes : requiredFields) {
            if (fieldsWithTypes.getFieldId().equalsIgnoreCase(fieldId))
                return fieldsWithTypes.getFieldType();
        }
        return null;
    }

}
