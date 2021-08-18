package com.sirionlabs.helper.dbHelper;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DocusignHelper {


    static String filePath = ConfigureConstantFields.getConstantFieldsProperty("DocuSignNewModuleConfigFilePath");
    static String fileName = ConfigureConstantFields.getConstantFieldsProperty("DocuSignNewModuleConfigFileName");
    static String dbHostAddress  =  ParseConfigFile.getValueFromConfigFile(filePath,fileName, "docusign","dbHostAddress");
    static String dbPortName = ParseConfigFile.getValueFromConfigFile(filePath,fileName, "docusign","dbPortName");
    static String dbName = ParseConfigFile.getValueFromConfigFile(filePath,fileName, "docusign","dbName");
    static String dbUserName = ParseConfigFile.getValueFromConfigFile(filePath,fileName, "docusign","dbUserName");
    static String dbPassword = ParseConfigFile.getValueFromConfigFile(filePath,fileName, "docusign","dbPass");

    private final static org.slf4j.Logger Logger = LoggerFactory.getLogger(DocusignHelper.class);


    private static List<String> getDataFromQuery(String query) {
        List<String> data = new ArrayList<>();

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                for(int i=0;i<results.size();i++)
                    data.add(results.get(i).get(0));
            } else {
                Logger.error("No results for this query");
                return null;
            }
            sqlObj.closeConnection();
            return data;
        } catch (Exception e) {
            Logger.error("Exception while Getting Data from DB using query [{}]. {}", query, e.getMessage());
            return null;
        }
    }

    private static List<String> getOneRowDataFromQuery(String query) {
        List<String> data = new ArrayList<>();

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                data = results.get(0);
            }

            sqlObj.closeConnection();
        } catch (Exception e) {
            Logger.error("Exception while Getting Data from DB using query [{}]. {}", query, e.getMessage());
            return null;
        }

        return data;
    }

    public static List<String> getIntegrationIdFromDB()  {

        String query = "select id from client_config order by id desc ";
        List<String> data = getDataFromQuery(query);
        return (data!=null) ? data : null;
    }

    public static String getEnvelopeId() {
        String query = "select envelope_id from envelopes order by id desc limit 1";
        List<String> data = getDataFromQuery(query);
        String enID = data.get(0).trim();
        return (enID!=null) ? enID : null;
    }

    public static Integer getEnvelopeStatus(String enID) {
        String query = "select status from envelopes where envelope_id = '" +enID+"'" ;
        List<String> data = getDataFromQuery(query);
        Integer status = Integer.valueOf(data.get(0).trim());
        return (status!=null) ? status : null;
    }

    public static boolean getConnectApiStatus(int Integrationid) {
        String query = "select connect_api_enabled from client_config where id = "+Integrationid ;
        List<String> data = getDataFromQuery(query);
        boolean status;
        if (data.get(0).trim().equals("t"))
        {
            status = true;
        }
        else
            status = false;
        return  status ;
    }

    public static boolean activateConnectApi(int Integrationid) {
        String query = "update client_config set connect_api_enabled=true where Id="+Integrationid ;
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
        Boolean result = sqlObj.updateDBEntry(query);

        return result;
    }

    public static boolean disableConnectApi(int Integrationid) {
        String query = "update client_config set connect_api_enabled=false where Id="+Integrationid ;
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
        Boolean result = sqlObj.updateDBEntry(query);

        return result;
    }

    public static int getLatestIdFromOtherAuditLog(String entityId) {
        String query = "select id from other_audit_log where entity_id = "+entityId +"order by Id desc limit 1;";
        int id;
        List<String> lst =  getOneRowDataFromQuery(query);
        id = Integer.parseInt(lst.get(0));
        return id;
    }

    public static int getLatestIdFromOtherAuditLogDocFile() {
        String query = "select id from other_audit_log_document_file order by Id desc limit 1;";
        int id;
        List<String> lst =  getOneRowDataFromQuery(query);
        id = Integer.parseInt(lst.get(0));
        return id;
    }

    public static List<String> getRecentInfoFromOtherAuditLog(int id, String entityId) {
        String query = "select action_id, comment from other_audit_log where id > "+id +" and entity_id="+entityId+ ";";
        List<String> lst =  getOneRowDataFromQuery(query);
        return lst;
    }

    public static int getRecentInfoFromOtherAuditLogDocFile(int id, String documentName) {
        String query = "select audit_log_id from other_audit_log_document_file where file_path ilike '%"+ documentName +"_SignedOn%' and id > "+id +";";
        int auditLogId;
        List<String> lst =  getOneRowDataFromQuery(query);
        auditLogId = Integer.parseInt(lst.get(0));
        return auditLogId;
    }

    public static int getActionId(int auditLogId) {
        String query = "select action_id from other_audit_log where id = "+auditLogId+" order by Id desc limit 1;";
        int actionId;
        List<String> lst =  getOneRowDataFromQuery(query);
        actionId = Integer.parseInt(lst.get(0));
        return actionId;
    }


    public static List<String> getLatestValuesFromDocSignDB() {
        List<String> list = new ArrayList<>();
        String query = "select id from envelopes order by Id desc limit 1;";
        list =  getDataFromQuery(query);
        query = "select id from documents order by Id desc limit 1;";
        list.add( getDataFromQuery(query).get(0));

        return list;
    }

    public static String getDocumentTypeOrigin(String latestDocumentId) {
        String query = "select document_type from documents where id > "+ latestDocumentId +" order by Id desc limit 10;";
        List<String> list =  getDataFromQuery(query);
        return list.get(0);
    }

}
