package com.sirionlabs.config;

import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.testRail.TestRailHelper;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Parameters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;


public class ConfigureEnvironment {

    private final static Logger logger = LoggerFactory.getLogger(ConfigureEnvironment.class);
    public static boolean useCookies = false;
    public static boolean useCSRFToken = false;
    public static boolean isProxyEnabled = false;
    public static String proxyServerHost = "localhost";
    public static Integer proxyServerPort = 8888;
    public static String environment;
    public static Integer noOfThreads = 1;
    public static Boolean addLatency = false;
    public static Long latencyTime = 1000L;
    public static Boolean jwtEnabled = false;
    public static Integer accessTokenRefreshTimeInterval = 8;
    public static Integer refreshTokenTimeInterval = 240;
    public static Boolean launchTimerTasks = false;
    private static String testingType;
    private static String endUserLoginId;
    private static String dbName;
    private static String dbHostAddress;
    private static String dbPortName;
    private static String dbUserName;
    private static String dbPassword;


    private static Map<String, String> environmentProperties = new HashMap<>();

    public static String getEnvironmentProperty(String key) {
        return environmentProperties.get(key.toLowerCase());
    }

    private static void configureProperties(String Environment) {
        configureProperties(Environment, false);
    }

    public static String getEndUserLoginId() {
        return endUserLoginId;
    }

    public static String getTestingType() {
        return testingType;
    }

    public static void configureProperties(String Environment, Boolean isAlternateFlow) {
        try {
            environment = Environment;
            String baseFilePath = ConfigureConstantFields.getConstantFieldsProperty("EnvironmentConfigFilesPath");

            String baseFileName;
            if (isAlternateFlow) {
                logger.info("Taking Environment value from Test config File");
                baseFileName = environment + ".cfg";
            } else if (System.getProperty("Environment") != null) {
                logger.info("Taking Environment value from Command Line");
                environment = System.getProperty("Environment");
                baseFileName = System.getProperty("Environment") + ".cfg";
            } else {
                logger.info("Taking Environment value from Testng XML File");
                baseFileName = environment + ".cfg";
            }
            environmentProperties = ParseConfigFile.getAllDefaultProperties(baseFilePath, baseFileName);

            String temp = ConfigureEnvironment.getEnvironmentProperty("noOfThreads");
            if (temp != null && NumberUtils.isParsable(temp.trim()) && Integer.parseInt(temp.trim()) != 0)
                noOfThreads = Integer.parseInt(temp.trim());

            temp = ConfigureEnvironment.getEnvironmentProperty("addLatency");
            if (temp != null && temp.trim().equalsIgnoreCase("true"))
                addLatency = true;

            temp = ConfigureEnvironment.getEnvironmentProperty("latencyTime");
            if (temp != null && NumberUtils.isParsable(temp.trim()))
                latencyTime = Long.parseLong(temp.trim());

            temp = ConfigureEnvironment.getEnvironmentProperty("jwtEnabled");
            if (temp != null && temp.trim().equalsIgnoreCase("true")) {
                jwtEnabled = true;

                temp = ConfigureEnvironment.getEnvironmentProperty("accessTokenRefreshTimeInterval");
                if (temp != null && NumberUtils.isParsable(temp.trim()))
                    accessTokenRefreshTimeInterval = Integer.parseInt(temp.trim());

                temp = ConfigureEnvironment.getEnvironmentProperty("refreshTokenTimeInterval");
                if (temp != null && NumberUtils.isParsable(temp.trim()))
                    refreshTokenTimeInterval = Integer.parseInt(temp.trim());
                temp = ConfigureEnvironment.getEnvironmentProperty("launchTimerTasks");
                if (temp != null && temp.trim().equalsIgnoreCase("true")) {
                    launchTimerTasks = true;
                }
            }

            String tmpUseCookies = ConfigureEnvironment.getEnvironmentProperty("UseCookies");
            String tmpUseCSRF = ConfigureEnvironment.getEnvironmentProperty("useCSRFToken");
            if (tmpUseCookies != null && tmpUseCookies.equalsIgnoreCase("yes")) {
                logger.info("Using the Cookies {}", tmpUseCookies);
                ConfigureEnvironment.useCookies = true;

                //Set Cookie Field
                FileReader reader = new FileReader(new File(baseFilePath + "//" + baseFileName));
                BufferedReader br = new BufferedReader(reader);
                String line;
                while ((line = br.readLine()) != null) {
                    String[] items = line.split("=");
                    if (items[0].trim().equalsIgnoreCase("Cookie")) {
                        int indexOfDelimiter = line.indexOf("=");
                        String cookie = line.substring(indexOfDelimiter + 1).trim();
                        environmentProperties.put("cookie", cookie);
                        break;
                    }
                }
            }
            if (tmpUseCSRF != null && tmpUseCSRF.equalsIgnoreCase("yes")) {
                logger.info("Using the CSRF {}", tmpUseCSRF);
                ConfigureEnvironment.useCSRFToken = true;
            }

            //Adding Code for ProxyServer Configuration
            if (environmentProperties.containsKey("isproxyenabled")) {
                String proxyEnabled = environmentProperties.get("isproxyenabled");
                if ((!proxyEnabled.equalsIgnoreCase("")) && (proxyEnabled.equalsIgnoreCase("yes"))) {
                    logger.debug("Proxy is enabled for the Environment");
                    isProxyEnabled = true;


                    if (environmentProperties.containsKey("proxyserverhost")) {
                        String proxyServer = environmentProperties.get("proxyserverhost");
                        if (!proxyServer.equalsIgnoreCase("")) {
                            logger.debug("Proxy Server Host is : [ {} ]", proxyServer);
                            proxyServerHost = proxyServer;
                        }
                    }

                    if (environmentProperties.containsKey("proxyserverport")) {
                        String serverPort = environmentProperties.get("proxyserverport");
                        if (!serverPort.equalsIgnoreCase("")) {
                            logger.debug("Proxy Server Port is : [ {} ]", serverPort);
                            proxyServerPort = Integer.parseInt(serverPort);
                        }
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Exception while configuring the environment {}", e.getMessage());
        }
    }

    public static String getCompleteHostUrl() {
        String completeHostUrl = "";

        completeHostUrl = completeHostUrl.concat(environmentProperties.get("scheme")).concat("://").concat(environmentProperties.get("host")).concat(":")
                .concat(ConfigureEnvironment.getEnvironmentProperty("port"));
        return completeHostUrl;
    }

    public static String getClientAdminUser() {
        return environmentProperties.get("clientusername");
    }

    public static String getClientAdminPassword() {
        return environmentProperties.get("clientuserpassword");
    }

    public static String getSuperAdminPassword() {
        return environmentProperties.get("superadminpassword");
    }

    public static String getClientSetupUserName() {
        return environmentProperties.get("clientsetupusername");
    }

    public static String getSuperAdminUserName() {
        return environmentProperties.get("superadminusername");
    }
    public static String getUserAdminName()
    {
        return environmentProperties.get("useradminname");
    }
    public static String getUserAdminPassword()
    {
        return environmentProperties.get("useradminpassword");
    }
    public static String getClientSetupUserPassword() {
        return environmentProperties.get("clientsetupuserpassword");
    }

    @BeforeSuite
    @Parameters({"TestingType", "Environment"})
    public void configureEnvironmentProperties(String TestingType, String Environment) {
        environment = Environment;
        testingType = TestingType;
        ConfigureEnvironment.configureProperties(environment);

        endUserLoginId = environmentProperties.get("j_username");

        TestRailHelper.initializeTestRail();
        TestAPIBase.setUp();
    }

    public static String getdbHostAddress() {
        return environmentProperties.get("dbHostAddress");
    }


}
