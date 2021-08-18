package com.sirionlabs.utils.commonUtils;

import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

/**
 * Created by shivashish on 29/6/17.
 */


public class XLSJsonUtils {

	private final static Logger logger = LoggerFactory.getLogger(XLSJsonUtils.class);
	JSONObject jsonObject;

	// Constructor that will take the JsonParserFile Path and File Name
	public XLSJsonUtils(String parserFilePath, String parserFileName) {
		try {
			File file = new File(parserFilePath + "//" + parserFileName);
			String content = FileUtils.readFileToString(file, "utf-8");
			// Convert JSON string to JSONObject
			jsonObject = new JSONObject(content);
			logger.debug("{}", jsonObject);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// function to createFull Map bases on the standard template that we have for XLS file
	// template will be read from parserConfigFileName
	public HashMap<String, HashMap<String, String>> createFullMap(String EntityName, String parserConfigFilePath, String parserConfigFileName) throws IOException, ConfigurationException {
		logger.info("Internal Method Name is :---->{}", Thread.currentThread().getStackTrace()[1].getMethodName());
		String xlsParserInternalPropertiesDelimiter = ",";

		HashMap<String, HashMap<String, String>> hashMapofEnitity = new HashMap<String, HashMap<String, String>>();
		JSONObject jsonforEntity = jsonObject.getJSONObject(EntityName);

		int columnMax = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(parserConfigFilePath, parserConfigFileName, "columnmax"));
		String[] columnInternalProperties = ParseConfigFile.getValueFromConfigFile(parserConfigFilePath, parserConfigFileName, "columninternalproperties").split(xlsParserInternalPropertiesDelimiter);
		String[] rowColInfo = ParseConfigFile.getValueFromConfigFile(parserConfigFilePath, parserConfigFileName, "rowcolinfo").split(xlsParserInternalPropertiesDelimiter);


		logger.debug("columnMax {}", columnMax);
		logger.debug("columnInternalProperties {}", columnInternalProperties[0]);
		logger.debug("rowColInfo {}", rowColInfo[0]);
		logger.debug("{}", jsonforEntity.keySet());

		for (String key : jsonforEntity.keySet()) {
			logger.debug("{}", jsonforEntity.getJSONObject(key));
		}

		// for putting the Map of Header Column Details in XLS file
		for (int i = 1; i <= columnMax; i++) {
			HashMap<String, String> hashMapInternal = new HashMap<String, String>();
			logger.debug("Value of i -> {}", i);
			hashMapofEnitity.put(String.valueOf(i), null);

			if (jsonforEntity.has(String.valueOf(i))) {
				logger.debug("Json Value of {}--> {}", i, jsonforEntity.getJSONObject(String.valueOf(i)));
				JSONObject internalJsonObj = jsonforEntity.getJSONObject(String.valueOf(i));

				for (String columnsProp : columnInternalProperties) {

					hashMapInternal.put(columnsProp, null);
					if (internalJsonObj.has(columnsProp)) {
						logger.debug("internal key value {}-->{}", columnsProp, internalJsonObj.get(columnsProp).toString());
						hashMapInternal.put(columnsProp, internalJsonObj.get(columnsProp).toString());
					}

				}
				hashMapofEnitity.put(String.valueOf(i), hashMapInternal);
			}
		}

		// for putting the Map of Values of indexes that will have relevent information to verify
		if (jsonforEntity.has("rowColInfo")) {
			HashMap<String, String> hashMapInternal = new HashMap<String, String>();
			JSONObject internalJsonObj = jsonforEntity.getJSONObject("rowColInfo");
			logger.debug("Json Value of rowColInfo {}--> {}", "rowColInfo", internalJsonObj);


			for (String rowColInfoProp : rowColInfo) {
				hashMapInternal.put(rowColInfoProp, null);
				if (internalJsonObj.has(rowColInfoProp)) {
					logger.debug("Internal key value {}-->{}", rowColInfoProp, internalJsonObj.get(rowColInfoProp).toString());
					hashMapInternal.put(rowColInfoProp, internalJsonObj.get(rowColInfoProp).toString());
				}

			}

			hashMapofEnitity.put("rowColInfo", hashMapInternal);
		}


		logger.info("HashMapofEntity is :{}", hashMapofEnitity);
		logger.info("************************************************************");

		return hashMapofEnitity;


	}


}
