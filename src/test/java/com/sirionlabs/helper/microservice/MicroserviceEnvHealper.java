package com.sirionlabs.helper.microservice;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

public class MicroserviceEnvHealper {
   public static String domain="";


    private static final String MicroserviceConfigFilePath;
    private static final String MicroserviceConfigFileName;
    private static Map<String, String> map;

    static{
        MicroserviceConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("MicroserviceConfigFilePath");
        MicroserviceConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("MicroserviceConfigFileName");

    }




   public static String getDomain(String Microservice_name ) {
       try {


           domain = ParseConfigFile.getValueFromConfigFile(MicroserviceConfigFilePath, MicroserviceConfigFileName, "default", "url");
           if (domain == null) {
               domain = ParseConfigFile.getValueFromConfigFile(MicroserviceConfigFilePath, MicroserviceConfigFileName, Microservice_name, "url");
           }

       } catch (Exception e) {
             System.out.print("error in fetching data from Microservice.cfg");
       }
       return domain;
   }


    public static Map<String, String> getAllPropertiesOfSection(String Microservice_name ) {
        map = ParseConfigFile.getAllConstantProperties(MicroserviceConfigFilePath, MicroserviceConfigFileName, Microservice_name);
        return map;
    }












}
