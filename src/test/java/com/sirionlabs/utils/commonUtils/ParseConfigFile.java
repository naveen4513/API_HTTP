package com.sirionlabs.utils.commonUtils;

import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

/**
 * Created by akshay.rohilla on 6/27/2017.
 */
public class ParseConfigFile {

	private final static Logger logger = LoggerFactory.getLogger(ParseConfigFile.class);

	public static Map<String, String> parseConfigFile(String filePath, String fileName, String delimiter) throws IOException {
		Map<String, String> properties = new HashMap<>();
		BufferedReader reader = new BufferedReader(new FileReader(filePath + "//" + fileName));
		String oneLine;

		while ((oneLine = reader.readLine()) != null) {
			oneLine = oneLine.trim();
			//Skip Comment Lines
			if (oneLine.length() > 1 && oneLine.charAt(0) == '/' && oneLine.charAt(1) == '/')
				continue;
			String words[] = oneLine.split(delimiter);
			if (words.length == 1 || words[1].trim().equalsIgnoreCase("") || words[1] == null) {
				properties.put(words[0].trim().toLowerCase(), null);
				continue;
			}
			properties.put(words[0].trim().toLowerCase(), words[1].trim());
		}
		return properties;
	}

	public static String getValueFromConfigFile(String filePath, String fileName, String propertyName) {
		return getValueFromConfigFile(filePath, fileName, null, propertyName);
	}

	public static String getValueFromConfigFile(String filePath, String fileName, String sectionName, String propertyName) {
		String columnName = null;

		try {
			propertyName = propertyName.trim();
			Configurations configs = new Configurations();
			INIConfiguration config = configs.ini(filePath + "//" + fileName);

			if (sectionName != null && config.getProperty(sectionName.toLowerCase() + "." + propertyName.toLowerCase()) != null)
				columnName = config.getProperty(sectionName.toLowerCase() + "." + propertyName.toLowerCase()).toString();

			else if (config.getProperty(propertyName.toLowerCase()) != null)
				columnName = config.getProperty(propertyName.toLowerCase()).toString();
		} catch (Exception e) {
			logger.error("Exception while getting Value from Config File {}. {}", (filePath + "/" + fileName), e.getStackTrace());
		}

		return columnName;
	}

	public static String getValueFromConfigFileCaseSensitive(String filePath, String fileName, String propertyName) throws ConfigurationException {
		return getValueFromConfigFileCaseSensitive(filePath, fileName, null, propertyName);
	}

	public static String getValueFromConfigFileCaseSensitive(String filePath, String fileName, String sectionName, String propertyName) {
		String columnName = null;

		try {
			propertyName = propertyName.trim();
			Configurations configs = new Configurations();
			INIConfiguration config = configs.ini(filePath + "//" + fileName);

			if (sectionName != null && config.getProperty(sectionName + "." + propertyName) != null)
				columnName = config.getProperty(sectionName + "." + propertyName).toString();

			else if (config.getProperty(propertyName) != null)
				columnName = config.getProperty(propertyName).toString();

		} catch (Exception e) {
			logger.error("Exception while Getting Value for Property {}. {}", propertyName, e.getStackTrace());
		}

		return columnName;
	}

	public static List<String> getAllPropertiesOfSection(String filePath, String fileName, String sectionName) throws ConfigurationException {
		Configurations configs = new Configurations();
		INIConfiguration config = configs.ini(filePath + "//" + fileName);
		Iterator<String> keys = config.getKeys(sectionName.toLowerCase());
		List<String> allProperties = new ArrayList<>();

		while (keys.hasNext()) {
			String key[] = keys.next().split("\\.");
			allProperties.add(key[1]);
		}

		return allProperties;
	}

	public static boolean containsSection(String filePath, String fileName, String sectionName) throws ConfigurationException {
		Configurations configs = new Configurations();
		INIConfiguration config = configs.ini(filePath + "//" + fileName);

		boolean sectionFound = false;
		Iterator<String> sections = config.getSections().iterator();

		while (sections.hasNext()) {
			String next = sections.next();
			if (next != null && next.equalsIgnoreCase(sectionName)) {
				sectionFound = true;
				break;
			}
		}
		return sectionFound;
	}

	public static Map<String, String> getAllConstantProperties(String filePath, String fileName, String sectionName) {
		Map<String, String> allDefaultProperties = new LinkedHashMap<>();

		try {
			Configurations configs = new Configurations();
			INIConfiguration config = configs.ini(filePath + "//" + fileName);
			Iterator<String> keys;
			if (sectionName == null) {
				keys = config.getKeys();
				while (keys.hasNext()) {
					String nextKey = keys.next();
					String keyValue = config.getString(nextKey);
					allDefaultProperties.put(nextKey.toLowerCase(), keyValue);
				}
			} else {
				keys = config.getKeys(sectionName.toLowerCase());
				while (keys.hasNext()) {
					String nextKey = keys.next();
					// Split keyname because they are section.keyname
					int lengthNextKey=nextKey.length();
					int lengthSectionName=sectionName.length()+1;
					String keyName = nextKey.substring(lengthSectionName,lengthNextKey);
					String keyValue = config.getString(nextKey);
					allDefaultProperties.put(keyName.toLowerCase(), keyValue);
				}
			}

		} catch (Exception e) {
			logger.error("Exception while Getting All Constant Properties from File [{}] and Section {}. {}", filePath + "/" + fileName, sectionName, e.getStackTrace());
		}
		return allDefaultProperties;
	}

	public static Map<String, String> getAllProperties(String filePath, String fileName) {
		return ParseConfigFile.getAllConstantProperties(filePath, fileName, null);
	}

	public static Map<String, String> getAllDefaultProperties(String filePath, String fileName){
		return ParseConfigFile.getAllConstantProperties(filePath, fileName, "default");
	}

	public static List<String> getAllSectionNames(String filePath, String fileName) throws ConfigurationException {
		Configurations configs = new Configurations();
		INIConfiguration config = configs.ini(filePath + "//" + fileName);

		List<String> sectionNameList = new ArrayList<>();
		Iterator<String> sections = config.getSections().iterator();

		while (sections.hasNext()) {
			String sectionName = sections.next();
			if (sectionName != null)
				sectionNameList.add(sectionName.toLowerCase());
		}
		return sectionNameList;
	}

	public static Map<String, String> getAllConstantPropertiesCaseSensitive(String filePath, String fileName, String sectionName) {
		Map<String, String> allDefaultProperties = new HashMap<>();

		try {
			Configurations configs = new Configurations();
			INIConfiguration config = configs.ini(filePath + "//" + fileName);
			Iterator<String> keys;
			if (sectionName == null) {
				keys = config.getKeys();
				while (keys.hasNext()) {
					String nextKey = keys.next();
					String keyValue = config.getString(nextKey);
					logger.info("Key {} and Value {}", nextKey, keyValue);
					allDefaultProperties.put(nextKey, keyValue);
				}
			} else {
				keys = config.getKeys(sectionName);
				while (keys.hasNext()) {
					String nextKey = keys.next();
					// Split keyname because they are section.keyname
					String keyName = nextKey.split(sectionName+".")[1];
					String keyValue = config.getString(nextKey);
					if(keyValue.contains("_gaurav_value_change_to_semicolon ")){
						keyValue = keyValue.replaceAll("_gaurav_value_change_to_semicolon ",";");
					}
					if(sectionName.equalsIgnoreCase("beforeedit")||sectionName.equalsIgnoreCase("afteredit"))
					{
						keyName=keyName.replace("..",".");
					}
					allDefaultProperties.put(keyName, keyValue);
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Getting All Properties of Section [{}] Case-Sensitive. {}", sectionName, e.getStackTrace());
		}
		return allDefaultProperties;
	}

	public static Map<String, String> AllConstantPropertiesCaseSensitive(String filePath, String fileName, String sectionName) {
		Map<String, String> allDefaultProperties = new HashMap<>();
		try {
			Configurations configs = new Configurations();
			INIConfiguration config = configs.ini(filePath + "//" + fileName);
			Iterator<String> keys;
			if (sectionName == null) {
				keys = config.getKeys();
				while (keys.hasNext()) {
					String nextKey = keys.next();
					String keyValue = config.getString(nextKey);
					logger.info("Key {} and Value {}", nextKey, keyValue);
					allDefaultProperties.put(nextKey, keyValue);
				}
			} else {
				keys = config.getKeys(sectionName);
				while (keys.hasNext()) {
					String nextKey = keys.next();
					// Split keyname because they are section.keyname
					String keyName = nextKey.split(sectionName+".")[1].replace("..",".");
					String keyValue = config.getString(nextKey);
					allDefaultProperties.put(keyName, keyValue);
				}
			}
		} catch (Exception e) {
			logger.error("Exception while Getting All Properties of Section [{}] Case-Sensitive. {}", sectionName, e.getStackTrace());
		}
		return allDefaultProperties;
	}

	public static boolean hasProperty(String filePath, String fileName, String property) throws ConfigurationException {
		return hasProperty(filePath, fileName, null, property);
	}

	public static boolean hasProperty(String filePath, String fileName, String sectionName, String property) throws ConfigurationException {
		boolean propertyFound = false;
		property = property.trim();
		Configurations configs = new Configurations();
		INIConfiguration config = configs.ini(filePath + "//" + fileName);

		if (sectionName != null && config.containsKey(sectionName.toLowerCase() + "." + property.toLowerCase()))
			propertyFound = true;
		else if (sectionName == null && config.containsKey(property.toLowerCase()))
			propertyFound = true;

		return propertyFound;
	}

	public static boolean hasPropertyCaseSensitive(String filePath, String fileName, String property) throws ConfigurationException {
		return hasPropertyCaseSensitive(filePath, fileName, null, property);
	}

	public static boolean hasPropertyCaseSensitive(String filePath, String fileName, String sectionName, String property) throws ConfigurationException {
		boolean propertyFound = false;
		property = property.trim();
		Configurations configs = new Configurations();
		INIConfiguration config = configs.ini(filePath + "//" + fileName);

		if (sectionName != null && config.containsKey(sectionName + "." + property))
			propertyFound = true;
		else if (sectionName == null && config.containsKey(property))
			propertyFound = true;

		return propertyFound;
	}


	public static synchronized void updateValueInConfigFile(String filePath, String fileName, String sectionName, String propertyName, String updatedPropertyValue)
			throws ConfigurationException, FileNotFoundException {
		propertyName = propertyName.trim();
		Configurations configs = new Configurations();

		INIConfiguration config = configs.ini(filePath + "//" + fileName);
		boolean isFileEdited = false;

		if (sectionName != null && config.getProperty(sectionName.toLowerCase() + "." + propertyName.toLowerCase()) != null) {
			//config.getSection(sectionName.toLowerCase()).clearProperty(propertyName.toLowerCase());
			config.getSection(sectionName.toLowerCase()).setProperty(propertyName.toLowerCase(), updatedPropertyValue);
			isFileEdited = true;

		} else if (config.getProperty(propertyName.toLowerCase()) != null) {
			for (Iterator<String> it = config.getKeys(); it.hasNext(); ) {
				String key = it.next();
				if (key.equalsIgnoreCase(propertyName)) {
					config.setProperty(propertyName.toLowerCase(), updatedPropertyValue);
					isFileEdited = true;
				}
			}
		} else {
			logger.warn("Either property name : [ {} ] or section name : [ {} ] doesn't exists, please check again.. ", propertyName, sectionName);
		}

		if (isFileEdited) {
			try {
				config.write(new FileWriter(filePath + "//" + fileName));
			} catch (IOException e) {
				logger.error("Got Exception while updating the config file : [ {} ], Cause : [ {} ], stacktrace : [ {} ]", filePath + "//" + fileName,
						e.getMessage(), e.getStackTrace());
				e.printStackTrace();
			}
		}
	}

	public static synchronized void updateValueInConfigFileCaseSensitive(String filePath, String fileName, String sectionName, String propertyName, String updatedPropertyValue)
			throws ConfigurationException, FileNotFoundException {
		propertyName = propertyName.trim();
		Configurations configs = new Configurations();

		INIConfiguration config = configs.ini(filePath + "//" + fileName);
		boolean isFileEdited = false;

		if (sectionName != null && config.getProperty(sectionName.toLowerCase() + "." + propertyName) != null) {
			//config.getSection(sectionName.toLowerCase()).clearProperty(propertyName.toLowerCase());
			config.getSection(sectionName.toLowerCase()).setProperty(propertyName, updatedPropertyValue);
			isFileEdited = true;

		} else if (config.getProperty(propertyName) != null) {
			for (Iterator<String> it = config.getKeys(); it.hasNext(); ) {
				String key = it.next();
				if (key.equalsIgnoreCase(propertyName)) {
					config.setProperty(propertyName, updatedPropertyValue);
					isFileEdited = true;
				}
			}
		} else {
			logger.warn("Either property name : [ {} ] or section name : [ {} ] doesn't exists, please check again.. ", propertyName, sectionName);
		}

		if (isFileEdited) {
			try {
				config.write(new FileWriter(filePath + "//" + fileName));
			} catch (IOException e) {
				logger.error("Got Exception while updating the config file : [ {} ], Cause : [ {} ], stacktrace : [ {} ]", filePath + "//" + fileName,
						e.getMessage(), e.getStackTrace());
				e.printStackTrace();
			}
		}
	}
}
