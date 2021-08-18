package com.sirionlabs.test;


import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.*;
import com.sirionlabs.utils.csvutils.DumpResultsIntoCSV;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.http.HttpResponse;

import org.apache.http.client.methods.HttpPost;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import java.awt.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

public class TestPerfAdminUserUpdate extends APIUtils{

    private final static Logger logger = LoggerFactory.getLogger(TestPerfAdminUserUpdate.class);
    private static String perfUserAdminConfigFilePath;
    private static String perfUserAdminConfigFileName;
    private static String perfUsertoTestForFilePath;
    private static String perfUsertoTestForFileName;
    private static String entitySection;
    private String [] users;
    CustomAssert csAssert = new CustomAssert();
    Check checkObj = new Check();
    private String adminusername = null;
    private String adminpassword = null;
    private String username = null;
    private String password = null;

    @BeforeClass(alwaysRun = true)
    public void beforeClass() throws IOException {
        logger.info("In Before Class method");
        try {
            perfUserAdminConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("PerfUserAdminConfigFilePath");
            perfUserAdminConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("PerfUserAdminConfigFileName");
            perfUsertoTestForFilePath = ParseConfigFile.getValueFromConfigFile(perfUserAdminConfigFilePath,perfUserAdminConfigFileName,"userstotestfor","usersfilepath");
            perfUsertoTestForFileName = ParseConfigFile.getValueFromConfigFile(perfUserAdminConfigFilePath,perfUserAdminConfigFileName,"userstotestfor","usersfilename");
            adminusername = ParseConfigFile.getValueFromConfigFile(perfUserAdminConfigFilePath,perfUserAdminConfigFileName,"adminuserconfig","adminusername");
            adminpassword = ParseConfigFile.getValueFromConfigFile(perfUserAdminConfigFilePath,perfUserAdminConfigFileName,"adminuserconfig","adminpassword");
            checkObj.hitCheck(adminusername,adminpassword);

        } catch (Exception e) {
            logger.error("Exception occurred while getting config data for listData api. {}", e.getMessage());
        }
    }

    @Test(dataProvider = "dataproviderforfilenames")
    public void testPerfAdminUserUpdate(String usernamePrev,String usernameNew) {
        CustomAssert csAssert = new CustomAssert();
        hitUserUpdate(usernamePrev);
        hitUserUpdate(usernameNew);

    }

    @DataProvider(name = "dataproviderforfilenames",parallel = true)
    public Object[][] dataproviderforfilenames(){

        try {
            String userstotest = ParseConfigFile.getValueFromConfigFile(perfUsertoTestForFilePath,perfUserAdminConfigFileName,"userstotestfor","users");
            users = userstotest.split(",");
        }
        catch(Exception e){
            logger.error("Got Exception while fetching Date Values from config files for Performance User", e.getMessage(), e.getStackTrace());
            e.printStackTrace();
        }
        Object obj[][] = new Object[4][];
        obj[0] = new Object[2];
        obj[0][0] = users[0]; //"AjayUser1UpdateNew.txt";
        obj[0][1] = users[1]; //"AjayUser1UpdatePrev.txt";

        obj[1] = new Object[2];
        obj[1][0] = users[2];//"AjayUserUpdateNew.txt";
        obj[1][1] = users[3];//"AjayUserUpdatePrev.txt";

        obj[2] = new Object[2];
        obj[2][0] = users[4];   //"VijayUser1UpdateNew.txt";
        obj[2][1] = users[5];   //"VijayUser1UpdatePrev.txt";

        obj[3] = new Object[2];
        obj[3][0] = users[6];   //"VijayUserUpdateNew.txt";
        obj[3][1] = users[7];   //"VijayUser1UpdatePrev.txt";

        return obj;
    }

    public HttpResponse hitUserUpdate(String user) {
        FileUtils fileread = new FileUtils();
        HttpResponse response = null;
        String apiResponseTime;
        try {
            HttpPost postRequest;
            String queryString = "tblusers/update";

            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            Map<String,String> parameters = new HashMap<String,String>();
            parameters = fileread.ReadKeyValueFromFile(perfUsertoTestForFilePath + "//" + perfUsertoTestForFileName,":",user);
            String params = UrlEncodedString.getUrlEncodedString(parameters);
            postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            postRequest.addHeader("Accept", "text/html, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");

            StopWatch watch = new StopWatch(); // calculating api response time
            try {
                watch.start();
                response = super.postRequest(postRequest, params,true);
                if (!(response.toString().contains("302")))
                {
                    logger.error("User information updated unsuccessfully");
                }
                else{
                    logger.info("User information updated successfully");
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                watch.stop();
            }
            //APIUtils utilObj = new APIUtils();
            String time = watch.toString();
            apiResponseTime = changeTimeIntoSec(time);
            logger.info("Response time for hitting update user api for the user" +  user + "is : " + apiResponseTime);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Exception while hitting userupdate Api. {}", e.getMessage());
        }
        return response;
    }
    @AfterClass(alwaysRun = true)
    public void afterClass() throws IOException {
        logger.info("In Before Class method");
        try {
            checkObj.hitCheck(ConfigureEnvironment.getEnvironmentProperty("j_username"), ConfigureEnvironment.getEnvironmentProperty("password"));

        } catch (Exception e) {
            logger.error("Exception occurred while getting config data for listData api. {}", e.getMessage());
        }
    }
}