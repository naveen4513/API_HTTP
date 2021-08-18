package com.sirionlabs.test.DualLogin;

import com.sirionlabs.api.clientAdmin.fieldLabel.FieldRenaming;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;



public class TestDualLogin  extends TestRailBase {

    private final static Logger logger = LoggerFactory.getLogger(TestDualLogin.class);
    private  String clientAdminUserName ="";
    private  String clientAdminUserPassword ="";
    private  String endUserName ="";
    private  String endUserPassword ="";
    private  String enduser_clientadmin_both_username ="";
    private  String enduser_clientadmin_both_password ="";
    private Check check = null;
    private FieldRenaming field =null;
    private  ListRendererListData list =null;

    @BeforeClass
    public void beforeClass() {
        String configFilePath = ConfigureConstantFields.getConstantFieldsProperty("DualModeLoginTestConfigFilePath");
        String configFileName = ConfigureConstantFields.getConstantFieldsProperty("DualModeLoginTestConfigFileName");
        clientAdminUserName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "clientadmin_username");
        clientAdminUserPassword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "clientadmin_password");

        endUserName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "enduser_username");
        endUserPassword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "enduser_password");
        enduser_clientadmin_both_username = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "enduser_clientadmin_both_username");
        enduser_clientadmin_both_password = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "enduser_clientadmin_both_password");
        check = new Check();
        field = new FieldRenaming();
        list = new ListRendererListData();
    }


    @Test
    public void TestForOnlyEndUserRole(){
        CustomAssert csAssert = new CustomAssert();
        check.hitCheck(endUserName, endUserPassword);
        String admin_api_response = field.hitFieldRenamingUpdate(1, 11);

        csAssert.assertFalse(JSONUtility.validjson(admin_api_response),"admin api is working for only end user role");
        list.hitListRendererListData(2);
        String listDataResponseStr = list.getListDataJsonStr();

        csAssert.assertTrue(JSONUtility.validjson(listDataResponseStr),"client api is not working for only end user role");
       // addTestResult(, csAssert);
        csAssert.assertAll();
    }

        @Test
    public void TestOnlyForClientAdminRole(){
            CustomAssert csAssert = new CustomAssert();
        check.hitCheck(clientAdminUserName,clientAdminUserPassword);
        String admin_api_response = field.hitFieldRenamingUpdate(1,11);

            csAssert.assertTrue(JSONUtility.validjson(admin_api_response),"admin api is not working for only client admin role");

            list.hitListRendererListData(2);
        String listDataResponseStr = list.getListDataJsonStr();

            csAssert.assertFalse(JSONUtility.validjson(listDataResponseStr),"client api is  working for only client admin role");

// addTestResult(, csAssert);
            csAssert.assertAll();
    }

    @Test
    public void TestbothForEndUserAndClientAdminRole() {
        CustomAssert csAssert = new CustomAssert();
        check.hitCheck(enduser_clientadmin_both_username,enduser_clientadmin_both_password);
        String admin_api_response = field.hitFieldRenamingUpdate(1,11);

        csAssert.assertTrue(JSONUtility.validjson(admin_api_response),"admin api is not working for both client admin role and end user role");

        list.hitListRendererListData(2);
        String listDataResponseStr = list.getListDataJsonStr();

        csAssert.assertFalse(JSONUtility.validjson(listDataResponseStr),"client api is  not working for bpth client admin role and end user role");

// addTestResult(, csAssert);
        csAssert.assertAll();

    }







}
