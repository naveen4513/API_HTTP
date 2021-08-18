package com.sirionlabs.test.userAccess;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.UserAccessHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.RandomNumbers;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TestUserLevelTabAccess {

    private final static Logger logger = LoggerFactory.getLogger(TestUserLevelTabAccess.class);
    private String configFilePath = null;
    private String configFileName = null;
    private Boolean applyRandomization = false;
    private int maxFlowsToValidate = 10;
    private Integer maxNoOfRecordsToValidate = 3;
    private Integer listDataSize = 20;
    private Integer listDataOffset = 0;

    @BeforeClass
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("UserLevelTabAccessConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("UserLevelTabAccessConfigFileName");

        Map<String, String> defaultPropertiesMap = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, "default");
        if (!defaultPropertiesMap.isEmpty()) {
            if (defaultPropertiesMap.containsKey("applyrandomization") && defaultPropertiesMap.get("applyrandomization").trim().equalsIgnoreCase("true"))
                applyRandomization = true;

            if (applyRandomization) {
                if (defaultPropertiesMap.containsKey("maxrecordstovalidate") && NumberUtils.isParsable(defaultPropertiesMap.get("maxrecordstovalidate")))
                    maxNoOfRecordsToValidate = Integer.parseInt(defaultPropertiesMap.get("maxrecordstovalidate"));

                if (defaultPropertiesMap.containsKey("maxflowstovalidate") && NumberUtils.isParsable(defaultPropertiesMap.get("maxflowstovalidate")))
                    maxFlowsToValidate = Integer.parseInt(defaultPropertiesMap.get("maxflowstovalidate"));
            }

            if (defaultPropertiesMap.containsKey("offset") && NumberUtils.isParsable(defaultPropertiesMap.get("offset")))
                listDataOffset = Integer.parseInt(defaultPropertiesMap.get("offset"));

            if (defaultPropertiesMap.containsKey("size") && NumberUtils.isParsable(defaultPropertiesMap.get("size")))
                listDataSize = Integer.parseInt(defaultPropertiesMap.get("size"));
        }
    }

    @AfterClass
    public void afterClass() {
        //Login with Original User for Rest of the Automation Suite
        logger.info("Logging back with Original Environment Configuration.");

        String originalUserName = ConfigureEnvironment.getEnvironmentProperty("j_username");
        String originalPassword = ConfigureEnvironment.getEnvironmentProperty("password");

        if (!loginWithUser("Original User Login Rollback", originalUserName, originalPassword)) {
            logger.info("Couldn't Login back with Original UserName [{}] and Password [{}]. Hence aborting Automation Suite.", originalUserName, originalPassword);
            System.exit(0);
        }
    }

    @DataProvider
    public Object[][] dataProviderForUserLevelTabAccess() {
        logger.info("Setting All Flows to Test for User Level Tab Access");

        List<Object[]> allTestData = new ArrayList<>();

        String[] allFlows = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowsToTest").split(Pattern.quote(","));
        int[] randomFlowsToValidate = RandomNumbers.getMultipleRandomNumbersWithinRangeIndex(0, allFlows.length - 1, maxFlowsToValidate);

        for (int flowNo : randomFlowsToValidate) {
            allTestData.add(new Object[]{allFlows[flowNo].trim()});
        }

        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForUserLevelTabAccess")
    public void testUserLevelTabAccess(String flowToTest) {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating User Level Tab Access Flow [{}]", flowToTest);
            Map<String, String> flowProperties = ParseConfigFile.getAllConstantProperties(configFilePath, configFileName, flowToTest);

            if (flowProperties.isEmpty()) {
                logger.info("Couldn't get Fields/Properties for Flow [{}]. Hence skipping test", flowToTest);
                throw new SkipException("Couldn't get Fields/Properties for Flow [" + flowToTest + "]. Hence skipping test");
            }

            //Login with New User to Check User Level Tab Access Flow

            String userName = flowProperties.get("username");
            String userPassword = flowProperties.get("password");

            if (!loginWithUser(flowToTest, userName, userPassword)) {
                logger.info("Couldn't login with UserName [{}] and Password [{}] for Flow [{}]. Hence skipping test.", userName, userPassword, flowToTest);
                throw new SkipException("Couldn't login with UserName [" + userName + "] and Password [" + userPassword + "] for Flow [" + flowToTest +
                        "]. Hence skipping test.");
            }

            UserAccessHelper accessHelperObj = new UserAccessHelper();
            String entityName = flowProperties.get("entity");
            int listId = ConfigureConstantFields.getListIdForEntity(entityName);
            String userType = flowProperties.get("usertype");
            List<String> expectedHiddenTabs = getExpectedHiddenTabs(userType, entityName);

            if (expectedHiddenTabs.isEmpty()) {
                logger.info("Couldn't get Expected Hidden Tabs for User Type {} and Entity {} for Flow [{}]. Hence skipping test.", userType, entityName, flowToTest);
                throw new SkipException("Couldn't get Expected Hidden Tabs for User Type " + userType + " and Entity " + entityName + " for Flow [" + flowToTest +
                        "]. Hence skipping test.");
            }

            //Verify User Level Tab Access in Show Page API Response.
            accessHelperObj.verifyUserLevelTabAccessInShowAPI(entityName, expectedHiddenTabs, listId, listDataOffset, listDataSize, applyRandomization,
                    maxNoOfRecordsToValidate, csAssert);
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            logger.error("Exception while Validating User Level Tab Access Flow [{}]. {}", flowToTest, e.getStackTrace());
            csAssert.assertTrue(false, "Exception while Validating User Level Tab Access Flow [" + flowToTest + "]. " + e.getMessage());
        }
        csAssert.assertAll();
    }

    private List<String> getExpectedHiddenTabs(String userType, String entityName) {
        List<String> expectedHiddenTabs = new ArrayList<>();

        String tabsValue = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, userType + " user entity wise tabs mapping", entityName);

        if (tabsValue == null) {
            logger.info("Couldn't find Hidden Tabs for Entity {} and User Type {}", entityName, userType);
        } else if (tabsValue.trim().equalsIgnoreCase("")) {
            logger.info("No Hidden Tab defined for Entity {} and User Type {}", entityName, userType);
        } else {
            String[] allTabs = tabsValue.trim().split(",");

            for (String tab : allTabs) {
                expectedHiddenTabs.add(tab.trim());
            }
        }

        return expectedHiddenTabs;
    }

    private Boolean loginWithUser(String flowToTest, String userName, String userPassword) {
        logger.info("Logging with UserName [{}] and Password [{}] for Flow [{}]", userName, userPassword, flowToTest);
        Check checkObj = new Check();
        checkObj.hitCheck(userName, userPassword);

        return (Check.getAuthorization() != null);
    }
}