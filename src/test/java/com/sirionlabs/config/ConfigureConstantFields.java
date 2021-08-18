package com.sirionlabs.config;

import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeSuite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ConfigureConstantFields {
    private final static Logger logger = LoggerFactory.getLogger(ConfigureConstantFields.class);

    private static Map<String, String> constantFieldsProperties = new HashMap<>();
    private static Map<String, String> entityNameMap = new HashMap<>();
    private static Map<String, String> entityIdMap = new HashMap<>();
    private static Map<String, String> reportIdMap = new HashMap<>();
    private static String configFilePath;
    private static String configFileName;

    public static String getConstantFieldsProperty(String key) {
        return constantFieldsProperties.get(key.toLowerCase());
    }

    public static String getEntityNameById(Integer entityTypeId) {
        return entityIdMap.get(Integer.toString(entityTypeId));
    }

    public static Integer getEntityIdByName(String entityName) {
        if (entityName.equalsIgnoreCase("definition")) {
            entityName = "clauses";
        } else if (entityName.equalsIgnoreCase("sub contracts")) {
            entityName = "contracts";
        }

        String entityId = entityNameMap.get(entityName.toLowerCase());
        if (entityId == null) {
            entityId = "0";
        }

        return Integer.parseInt(entityId);
    }

    public static int getListIdForEntity(String entityName) {
        try {
            if (entityName.equalsIgnoreCase("sub contracts")) {
                return 354;
            }

            return Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "entity_url_id"));
        } catch (Exception e) {
            logger.error("Exception while getting List Id for Entity {}. {}", entityName, e.getStackTrace());
            return -1;
        }
    }

    public static String getUrlNameForEntity(String entityName) {
        return ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "url_name");
    }

    public static String getSearchUrlForEntity(String entityName) {
        return ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "search_url");
    }

    public static String getShortCodeForEntity(String entityName) {
        return ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "short_code");
    }

    public static int getListIdForEntity(int entityTypeId) {
        String entityName = "";
        try {


            return Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, entityName, "entity_url_id"));
        } catch (Exception e) {
            logger.error("Exception while getting List Id for Entity {}. {}", entityName, e.getStackTrace());
            return -1;
        }
    }

    public static List<String> entityTypeIds (){
        ArrayList<String> lst = new ArrayList<>();
        for (Map.Entry entityName : entityNameMap.entrySet() ) {

            lst.add(String.valueOf(entityName.getKey()));
        }
          return lst;
    }



    @BeforeSuite
    public void configureConstantFieldsProperties() {
        try {
            logger.info("configuring ConstantFieldsProperties");
            String baseFilePath = "src//test//resources//CommonConfigFiles";
            String baseFileName = "ConstantFields.cfg";
            String entityIdFile = "EntityId.cfg";
            constantFieldsProperties = ParseConfigFile.getAllDefaultProperties(baseFilePath, baseFileName);
            entityNameMap = ParseConfigFile.getAllProperties(baseFilePath, entityIdFile);

            // Reverse the entityNameMap
            entityIdMap = entityNameMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

            configFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFilePath");
            configFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
        } catch (Exception e) {
            logger.error("Exception while doing ConstantField configuration {}", e.getMessage());
        }
    }
}