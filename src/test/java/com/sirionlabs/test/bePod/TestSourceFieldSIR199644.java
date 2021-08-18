package com.sirionlabs.test.bePod;


import com.sirionlabs.api.clientAdmin.dropDownType.DropDownTypeList;
import com.sirionlabs.api.clientAdmin.dropDownType.DropDownTypeShow;
import com.sirionlabs.api.clientAdmin.dropDownType.DropDownTypeUpdate;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.commonAPI.Options;
import com.sirionlabs.api.listRenderer.ListRendererFilterData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;


import java.io.UnsupportedEncodingException;
import java.util.HashMap;

/**
 * Created by nikhil.haritash on 10-04-2019.
 */
@Listeners(value = MyTestListenerAdapter.class)
public class TestSourceFieldSIR199644 extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TestSourceFieldSIR199644.class);

    @Test(enabled = false)
    public void testFilterDataAPI() {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Hitting the Filter Data API :");

        ListRendererFilterData listDataobj = new ListRendererFilterData();
        listDataobj.hitListRendererFilterData(2);
        String filterResponse = listDataobj.getListRendererFilterDataJsonStr();

        JSONObject jsonObject = new JSONObject(filterResponse);
        String temp1 = "2";

        logger.info(temp1);
        JSONObject jsonObject2 = jsonObject.getJSONObject(temp1);

        jsonObject2 = jsonObject2.getJSONObject("multiselectValues").getJSONObject("OPTIONS");

        boolean autoComplete = jsonObject2.getBoolean("autoComplete");
        logger.info("Auto Complete Flag for {} is {}", temp1, autoComplete);
        csAssert.assertTrue(autoComplete, "Test Case Failed");
        csAssert.assertAll();

    }

    // C63072 Verify size field's value is applicable at end user
    @Test(priority = 0,enabled = true)
    public void testC63072() throws UnsupportedEncodingException {

        CustomAssert assertion =  new CustomAssert();

        // Setting Size limit at client admin
        int size = 50;

        String responseList = getDropDownListResponse();
        String Field = "Source";
        String id = DropDownTypeList.listIdFromResponse(responseList,Field);
        logger.info("id: {}", id);

        String responseShow = getDropDownShowResponse(id);
        String fieldType = DropDownTypeShow.showFromResponse(responseShow);
        fieldType = fieldType.toLowerCase().replace(" ","").trim();
        logger.info("FieldType: {}", fieldType);

        AdminHelper AdmObj = new AdminHelper();
        String lastUserName =  Check.lastLoggedInUserName;
        String lastUserPwd = Check.lastLoggedInUserPassword;

        AdmObj.loginWithClientAdminUser();

        String apiPath = DropDownTypeUpdate.getAPIPath();
        HashMap<String,String> headersForCreate = DropDownTypeUpdate.getHeaders();
        HashMap<String,String> params = DropDownTypeUpdate.getParameters(size, fieldType,id);

        int responseCode = executor.postMultiPartFormData(apiPath,headersForCreate,params).getResponse().getResponseCode();
        logger.info("responseCode: {}", responseCode);
        assertion.assertTrue(responseCode==302 , Field + " Could not be updated to Size " + size );

        AdmObj.loginWithUser(lastUserName,lastUserPwd);


        assertion.assertAll();
    }

    private String getDropDownShowResponse(String id) {

        AdminHelper helperObj = new AdminHelper();

        String lastUserName = Check.lastLoggedInUserName;
        String lastUserPassword = Check.lastLoggedInUserPassword;

        helperObj.loginWithClientAdminUser();

        String response = executor.get(DropDownTypeShow.getAPIPath(id), DropDownTypeShow.getHeaders()).getResponse().getResponseBody();
        helperObj.loginWithUser(lastUserName, lastUserPassword);

        return response;
    }

    private String getDropDownListResponse() {
        AdminHelper helperObj = new AdminHelper();

        String lastUserName = Check.lastLoggedInUserName;
        String lastUserPassword = Check.lastLoggedInUserPassword;

        helperObj.loginWithClientAdminUser();

        String response = executor.get(DropDownTypeList.getApiPath(), DropDownTypeList.getHeaders()).getResponse().getResponseBody();
        helperObj.loginWithUser(lastUserName, lastUserPassword);

        return response;
    }

    //C63048 Dropdown field - Add new field named Source
    @Test (priority = 1,enabled = true)
    public void testC63048()
    {
        CustomAssert assertion = new CustomAssert();

        String responseList = getDropDownListResponse();
        String Field = "Source";

        AdminHelper AdmObj = new AdminHelper();

        String lastUserName = Check.lastLoggedInUserName;
        String lastPassword = Check.lastLoggedInUserPassword;

        AdmObj.loginWithClientAdminUser();

        String id = DropDownTypeList.listIdFromResponse(responseList,Field);
        assertion.assertTrue(!id.isEmpty(),Field + " does not exists in Dropdown list API");

        AdmObj.loginWithUser(lastUserName,lastPassword);

        assertion.assertAll();
    }

    //C63052 Size field for Source should be configurable
    @Test(priority = 2, enabled = true)
    public void testC63052()
    {

        CustomAssert assertion = new CustomAssert();

        logger.info("Hitting the DropDown list and show API to get the field type and size");

        String Field = "Source";
        String responseList = getDropDownListResponse();
        String id = DropDownTypeList.listIdFromResponse(responseList,Field);

        String responseShow = getDropDownShowResponse(id);
        String fieldType = DropDownTypeShow.showFromResponse(responseShow);
        String EnableType = DropDownTypeShow.EnableViewFromResponse(responseShow);
        String ShowSize = DropDownTypeShow.SizeFromResponse(responseShow);
        logger.info("Filed type is {} and Enable Type is {} and Size is {} ",fieldType,EnableType,ShowSize);

        logger.info("Hitting the option API for end user to get the size");

        HashMap <String,String> params =  new HashMap<>();
        params.put("relationId","1024");
        params.put("parentEntityTypeId","4");
        params.put("query","test");
        params.put("pageType","1");
        params.put("entityTypeId","315");

        int dropDownType = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ApiPropertiesFilePath"),
                ConfigureConstantFields.getConstantFieldsProperty("OptionsConfigFileName"), "dropdowntype", "sourcefield"));

        Options obj = new Options();
        obj.hitOptions(dropDownType,params);
        String response = obj.getOptionsJsonStr();

        JSONObject JsonObj = new JSONObject(response);
        int OptionsSize = Integer.parseInt(JsonObj.get("sizeLimit").toString());


        logger.info("Option Size is {}",OptionsSize);

        assertion.assertTrue(String.valueOf(OptionsSize).equalsIgnoreCase(ShowSize),"Mismatch in Size for Source field between DropDownShow and OptionShow");

        assertion.assertAll();

    }

    //C63049 Type for the Source field should be configurable
    @Test(priority = 3, enabled = true)
    public void testC63049()
    {
        CustomAssert assertion = new CustomAssert();
    try {

        logger.info("Hitting the DropDown list and show API to get the field type and size");

        String Field = "Source";
        String responseList = getDropDownListResponse();
        String id = DropDownTypeList.listIdFromResponse(responseList, Field);
        logger.info("Id is {}",id);

        String responseShow = getDropDownShowResponse(id);

        String OriginalFieldType = DropDownTypeShow.showFromResponse(responseShow).toLowerCase().replace(" ","").trim();
        String OriginalEnableType = DropDownTypeShow.EnableViewFromResponse(responseShow);
        String OriginalShowSize = DropDownTypeShow.SizeFromResponse(responseShow);

        logger.info("Filed type is {} and Enable Type is {} and Size is {} ",OriginalFieldType,OriginalEnableType,OriginalShowSize);

        AdminHelper AdmObj = new AdminHelper();
        String lastUserName = Check.lastLoggedInUserName;
        String lastUserPwd = Check.lastLoggedInUserPassword;

        AdmObj.loginWithClientAdminUser();

        logger.info("Hitting the dropDown update API to change the Source Field type.");
        String fieldType = OriginalFieldType.equalsIgnoreCase("autocomplete") ? "normal" : "autocomplete";

        String apiPath = DropDownTypeUpdate.getAPIPath();
        HashMap<String, String> headersForCreate = DropDownTypeUpdate.getHeaders();
        logger.info("value to be changed as  {},{},{}",OriginalShowSize,fieldType,id);
        HashMap<String, String> params = DropDownTypeUpdate.getParameters(Integer.parseInt(OriginalShowSize), fieldType, id);
        int responseCode = executor.postMultiPartFormData(apiPath, headersForCreate, params).getResponse().getResponseCode();
        logger.info("responseCode: {}", responseCode);
        assertion.assertTrue(responseCode==302 , Field + " Could not be updated to Type " + fieldType );

        logger.info("Hitting the dropDown update API to update the Original Source Field type.");

        HashMap<String, String> paramsBack = DropDownTypeUpdate.getParameters(Integer.parseInt(OriginalShowSize), OriginalFieldType, id);
        int responseCodeBack = executor.postMultiPartFormData(apiPath, headersForCreate, params).getResponse().getResponseCode();
        AdmObj.loginWithUser(lastUserName,lastUserPwd);

        assertion.assertTrue(responseCodeBack==302 , Field + " Could not be updated to Type " + OriginalFieldType );

    }catch (Exception e)
    {
        assertion.assertTrue(false ,"Exception while validating API" + e.getMessage());
    }

        assertion.assertAll();

    }

}
