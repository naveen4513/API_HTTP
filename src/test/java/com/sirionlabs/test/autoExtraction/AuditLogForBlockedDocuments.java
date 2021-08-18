package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.api.auditLogs.AuditLog;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.autoextraction.ContractCreationWithDocUploadHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.io.IOException;

public class AuditLogForBlockedDocuments {
    private final static Logger logger = LoggerFactory.getLogger(AuditLogForBlockedDocuments.class);
    static Integer contractId;
    private static String postgresHost;
    private static String postgresPort;
    private static String postgresDbName;
    private static String postgresDbUsername;
    private static String postgresDbPassword;
    static String configAutoExtractionFilePath;
    static String configAutoExtractionFileName;
    private AuditLog auditlog;
    private String filterMap;
    int clientId=1007;
    private String AuditLogConfigFilePath;
    private String AuditLogConfigFileName;
    private int contractsEntityTypeId;

    @BeforeClass
    public void beforeClass() {
        configAutoExtractionFilePath = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFilePath");
        configAutoExtractionFileName = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFileName");
        postgresHost = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "postres sirion db details", "host");
        postgresPort = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "postres sirion db details", "port");
        postgresDbName = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "postres sirion db details", "dbname");
        postgresDbUsername = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "postres sirion db details", "username");
        postgresDbPassword = ParseConfigFile.getValueFromConfigFile(configAutoExtractionFilePath, configAutoExtractionFileName, "postres sirion db details", "password");
        auditlog = new AuditLog();
        AuditLogConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionTestAuditLogConfigFilePath");
        AuditLogConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionTestAuditLogConfigFileName");
        filterMap = ParseConfigFile.getValueFromConfigFile(AuditLogConfigFilePath, AuditLogConfigFileName, "filterjson", "61");
        contractsEntityTypeId = ConfigureConstantFields.getEntityIdByName("contracts");

    }
    public static Boolean allowDuplicateDocs(boolean flag, int clientId) {
        Boolean result = false;
        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(postgresHost, postgresPort, postgresDbName, postgresDbUsername, postgresDbPassword);
        try {
            String query = "update ae_client_configurations set allow_duplicate_documents=" + flag + " where client_id=" + clientId + "";
            result = postgreSQLJDBC.updateDBEntry(query);
        }
        catch (Exception e)
        {
            logger.info("Failed to Execute" + " " + clientId + " The error is " + e.getMessage());

        }
        finally {
            postgreSQLJDBC.closeConnection();

        }
        return result;
    }

    @Parameters("Environment")
    @Test(priority = 0)
    public void auditLogInContract(String environment) throws IOException {
        CustomAssert csAssert = new CustomAssert();
        try {
            logger.info("Before Creating the contract executing the Query: allow_duplicate docs false in ae_client_configurations");
            boolean success = allowDuplicateDocs(false, clientId);
            csAssert.assertTrue(success, "Query has not been executed successfully");
            try {
                logger.info("Creating a contract along with document upload to check deviation score for the document");
                ContractCreationWithDocUploadHelper contractObj = new ContractCreationWithDocUploadHelper();
                //contractId=135389;
                contractId = contractObj.TestAEDocumentUploadFromContracts(environment);
                logger.info(String.valueOf(contractId));
                try{
                    logger.info("Checking Audit Log of Contract After successful porting of metadata for Contract Id: " + contractId);
                    String auditLog_response = auditlog.hitAuditLogDataApi(String.valueOf(61), String.valueOf(contractId), filterMap);
                    JSONObject auditLogJson = new JSONObject(auditLog_response);
                    int actionColumnId = ListDataHelper.getColumnIdFromColumnName(auditLog_response, "action_name");
                    String actionValue = auditLogJson.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(actionColumnId)).getString("value");
                    logger.info("Action name for contractId " + contractId + "is: " + actionValue);
                    csAssert.assertEquals(actionValue,"Extraction Blocked","Not showing Extraction blocked status in the audit Log");
                }
                catch (Exception e)
                {
                    csAssert.assertTrue(false, "Audit Log API is not working : " + e.getMessage());

                }
            }
            catch (Exception e)
            {
                csAssert.assertTrue(false, "Contract Creation is not working : " + e.getMessage());

            }
        }
        catch (Exception e)
        {
            logger.info("Failed to Execute" + " " + clientId + " The error is " + e.getMessage());

        }
    csAssert.assertAll();
    }

    @Test
    public void checkCommentSection()
    {
        CustomAssert csAssert = new CustomAssert();
        String comment = null;
        try{
            TabListData tabListObj = new TabListData();
            String tabListDataResponse = tabListObj.hitTabListData(65, contractsEntityTypeId, contractId);
            JSONObject tabListDataJson = new JSONObject(tabListDataResponse);
            if (ParseJsonResponse.validJsonResponse(tabListDataResponse))
            {
                int columnIdOfComment = ListDataHelper.getColumnIdFromColumnName(tabListDataResponse,"comment");
                String completeComment = tabListDataJson.getJSONArray("data").getJSONObject(0).getJSONObject(String.valueOf(columnIdOfComment)).get("value").toString().trim();
                comment = completeComment.split("-")[0].trim();
            }
            csAssert.assertTrue(comment.contains("doesn't gets extracted as it's duplicate"),"Message is Incorrect in Comment section " + comment);
        }
        catch (Exception e)
        {
            csAssert.assertTrue(false,"Exception occured while hitting comment section API for entity Type Id : " +contractsEntityTypeId + " and Entity Id: " +contractId) ;
        }
        finally {
            //Delete Contract
            logger.info("Deleting Newly Created Contract Entity");
            EntityOperationsHelper.deleteEntityRecord("contracts", contractId);

        }
        csAssert.assertAll();
    }

    @AfterClass
    public void afterClass() {
        logger.debug("in After Class");
        boolean success = allowDuplicateDocs(true, clientId);
        if(success)
        {
            logger.info("Client Configuration has been reset with allow blocked documents");
        }
        else {
            logger.info("Allow Duplicate docs flag is still: " + success);
        }
    }
}
