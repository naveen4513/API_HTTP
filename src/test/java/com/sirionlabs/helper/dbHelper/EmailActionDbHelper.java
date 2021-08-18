package com.sirionlabs.helper.dbHelper;

import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EmailActionDbHelper {

    private final static Logger logger = LoggerFactory.getLogger(EmailActionDbHelper.class);

    private static String dbHostAddress = "192.168.2.152";
    private static String dbName = "TestDBBackup14June";
    private static String dbUserName = "postgres";
    private static String dbPassword = "postgres";
    private static String dbPortName = "5432";
    private static String dbEmailName = "letterbox";


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
            logger.error("Exception while Getting Data from DB using query [{}]. {}", query, e.getMessage());
            return null;
        }

        return data;
    }

    private static List<String> getOneRowDataFromSpecifiedDB(String query) {
        List<String> data = new ArrayList<>();

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                data = results.get(0);
            }

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Getting Data from DB using query [{}]. {}", query, e.getMessage());
            return null;
        }

        return data;
    }

    private static List<String> getOneRowDataFromLetterBoxDB(String query) {
        List<String> data = new ArrayList<>();

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbEmailName, dbUserName, dbPassword);
            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                data = results.get(0);
            }

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Getting Data from DB using query [{}]. {}", query, e.getMessage());
            return null;
        }

        return data;
    }

    public static String getAuthToken() {

        String query = "select body from system_emails where subject ilike '%English%' and subject ilike '%review%' and to_email ilike '%srijan%' order by Id desc limit 5";
        String authToken="";
        List<String> lst =  getOneRowDataFromLetterBoxDB(query);
        String item = lst.get(0);
        int num1 = item.indexOf("authToken=") + 10;
        int num2 = item.indexOf("\" type=\"button\">");
        authToken = item.substring(num1, num2);

        return authToken;
    }

    public static String getDataForParam(String columnName, String recipientEmail, String action) {

        String query = "select "+columnName+" from system_emails where to_email = '"+recipientEmail+"' and subject ilike '%"+action+"%' order by Id desc limit 1";
        List<String> lst =  getOneRowDataFromLetterBoxDB(query);
        return lst.get(0);
    }

    public static String getClientEntitySeqId(String contractId) {
        String query = "select client_entity_seq_id from contract where id = "+contractId+";";
        String client_entity_seq_id;
        List<String> lst =  getOneRowDataFromSpecifiedDB(query);

        client_entity_seq_id = lst.get(0);

        return client_entity_seq_id;
    }

    public static void cleanSystemEmailsTable() {
        String query = "truncate system_email_attachments, system_emails;";
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbEmailName, dbUserName, dbPassword);
        Boolean result = sqlObj.updateDBEntry(query);
    }

    public static String fetchUserEmailwithSpecificLanguage(String language, String emailMessage) throws SQLException {
        String item= null;
        String query = "select to_email from system_emails where subject ilike '%"+language+"%' and subject ilike '%"+emailMessage+"%';";
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbEmailName, dbUserName, dbPassword);
        List<String> results = sqlObj.doSelect(query).get(0);
        item = results.get(0);
        return item;
    }

    public static String fetchEmailSubjectForGivenUser(String userEmail) throws SQLException {
        String item= null;
        String query = "select subject from system_emails where to_email = '"+userEmail+"' and subject ilike '%has been archived%';";
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbEmailName, dbUserName, dbPassword);
        List<String> results = sqlObj.doSelect(query).get(0);
        item = results.get(0);
        return item;
    }

    public static String fetchClientNameFromEmailSubject(String email) throws SQLException {
        String item= null;
        String query = "select subject from system_emails where to_email = '"+email+"';";
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbEmailName, dbUserName, dbPassword);
        List<String> results = sqlObj.doSelect(query).get(0);
        item = results.get(0);
        item = item.substring(item.indexOf("new")+4,item.indexOf(","));
        return item;
    }

    public static String fetchContractNameFromEmailBody(String email) throws SQLException {
        String item= null;
        String query = "select body from system_emails where to_email = '"+email+"';";
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbEmailName, dbUserName, dbPassword);
        List<String> results = sqlObj.doSelect(query).get(0);
        item = results.get(0);
        item = item.substring(item.indexOf("The document titled ")+20,item.indexOf("(<a href")-1);
        return item;
    }

    public static Boolean updateUserLanguage(String email, String language) {

        String query = "update app_user set language_id= (select Id from system_language where name ilike '%"+language+"%' order by Id asc limit 1)  where email ilike '%"+email+"%';";
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
        Boolean result = sqlObj.updateDBEntry(query);
        return result;
    }

    public static Boolean contractDeletion(String state) {

        String query = "update contract set deleted = "+state +";";
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
        Boolean result = sqlObj.updateDBEntry(query);
        return result;
    }

    public static String getEmailCountInDB() throws SQLException{
        String item= null;
        String query = "select count(*) from system_emails;";
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbEmailName, dbUserName, dbPassword);
        List<String> results = sqlObj.doSelect(query).get(0);
        item = results.get(0);
        return item;
    }



}