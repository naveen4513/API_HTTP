package com.sirionlabs.utils.commonUtils;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.sirionlabs.config.ConfigureConstantFields;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Set;

public class JSONUtility {

	JSONObject jsonOb;

	public JSONUtility(JSONObject obj) {
		jsonOb = obj;
	}

	public static JSONArray convertJsonOnjectToJsonArray(JSONObject jsonObject) {
		Iterator itr = jsonObject.keys();
		JSONArray jsonArray = new JSONArray();

		while (itr.hasNext()) {
			String key = (String) itr.next();
			jsonArray.put(jsonObject.get(key));
		}
		return jsonArray;
	}

	/**
	 * @param key        : this is the key for which value is supposed to be return
	 * @param jsonObject : this is jsonObject in which embedded key need to be search
	 * @param value      : this is the  key name of which value needs to be return
	 * @return String value , will return null if not exist
	 */
	public static String getValueByEmbeddedKey(String key, JSONObject jsonObject, String value) {

		for (String column : JSONObject.getNames(jsonObject)) {
			if (jsonObject.getJSONObject(column).toString().contains(key)) {
				return (String) jsonObject.getJSONObject(column).get(value);
			}
		}
		return null;
	}

	public String getStringArrayValueFromJSONObject(String objectsOrder) {
		String value = null;
		String str = null;
		JSONObject temp = new JSONObject(jsonOb.toString());

		try {
			String order[] = objectsOrder.split(ConfigureConstantFields.getConstantFieldsProperty("JsonObjectsOrderDelimiter"));

			for (int i = 0; i < order.length - 1; i++) {
				str = temp.getJSONObject(order[i].trim()).toString();
				temp = new JSONObject(str);
			}

			value = temp.get(order[order.length - 1].trim()).toString();

		} catch (Exception e) {
			System.out.println("Could not fetch value from Json Object: " + objectsOrder);
		}

		return value;
	}

	public String getStringJsonValue(String key) {
		if (jsonOb.has(key))
			return jsonOb.get(key).toString();

		else
			return null;
	}

	public boolean getBooleanJsonValue(String key) {
		if (jsonOb.has(key))
			return jsonOb.getBoolean(key);

		else
			return false;
	}

	public int getIntegerJsonValue(String key) {
		if (jsonOb.has(key))
			return jsonOb.getInt(key);

		else
			return -1;
	}

	public JSONArray getArrayJsonValue(String key) {
		if (jsonOb.has(key.toLowerCase()))
			return jsonOb.getJSONArray(key.toLowerCase());
		else if (jsonOb.has(key.toUpperCase()))
			return jsonOb.getJSONArray(key.toUpperCase());
		else if (jsonOb.has(key))
			return jsonOb.getJSONArray(key);

		else
			return null;
	}

	public JSONObject getJsonObject(String key) {
		if (jsonOb.has(key.toLowerCase()))
			return jsonOb.getJSONObject(key.toLowerCase());
		else if (jsonOb.has(key.toUpperCase()))
			return jsonOb.getJSONObject(key.toUpperCase());
		else if (jsonOb.has(key))
			return jsonOb.getJSONObject(key);

		else
			return null;
	}

	// this method will take json Object and key and check whether the given key exist in Json or Not even Nested
	public static boolean checkKey(JSONObject object, String searchedKey) {
		boolean exists = object.has(searchedKey);
		if(!exists) {
			Set<String> keys = object.keySet();
			for(String key : keys){
				if ( object.get(key) instanceof JSONObject ) {
					exists = checkKey((JSONObject)object.get(key), searchedKey);
				}
			}
		}
		return exists;
	}

	public static Object parseJson(String jsonStr, String jsonPathExpression){
		Object obj = JsonPath.parse(jsonStr).read(jsonPathExpression);
		return obj;
	}


	public static boolean validjson(String jsonStr){
		try{
			JSONObject obj = new JSONObject(jsonStr);
		return  true;
		}catch (Exception e ){
		    e.printStackTrace();
			System.out.println(e);
		return  false;
		}

	}
}
