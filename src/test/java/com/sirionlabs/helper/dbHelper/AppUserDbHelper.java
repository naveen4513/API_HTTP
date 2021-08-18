package com.sirionlabs.helper.dbHelper;

import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class AppUserDbHelper {

    private final static Logger logger = LoggerFactory.getLogger(AppUserDbHelper.class);

    private static List<String> getUserDataFromQuery(String query) {
        List<String> data = new ArrayList<>();

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                data = results.get(0);
            }

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Getting User Data from DB using query [{}]. {}", query, e.getMessage());
        }

        return data;
    }

    public static List<String> getUserDataFromUserId(int userId) {
        return getUserDataFromUserId("*", userId);
    }

    public static List<String> getUserDataFromUserId(String selectedColumns, int userId) {
        String query = "select " + selectedColumns + " from app_user where id = " + userId;
        return getUserDataFromQuery(query);
    }

    public static List<String> getUserDataFromUserLoginId(String selectedColumns, String loginId, int clientId) {
        String query = "select " + selectedColumns + " from app_user where client_id = " + clientId + " and login_id = '" + loginId + "'";
        return getUserDataFromQuery(query);
    }

    public static int getUserIdFromLoginIdAndClientId(String loginId, int clientId) {
        String query = "select id from app_user where client_id = " + clientId + " and login_id = '" + loginId + "'";
        List<String> userData = getUserDataFromQuery(query);

        return (userData.isEmpty()) ? -1 : Integer.parseInt(userData.get(0));
    }

    public static int getLatestUserId(int type,int ClientId) {
        String query = "select id from app_user where type = "+type+" and client_id ="+ClientId+" order by id desc limit 1";

        List<String> results = getUserDataFromQuery(query);
        if (results.isEmpty()) {
            return -1;
        }

        return Integer.parseInt(results.get(0));
    }

    public static void deleteAppUser(int userId) throws SQLException {
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        String query = "delete from user_terms_acceptance where user_id =  " + userId;
        sqlObj.deleteDBEntry(query);

        query = "delete from user_home_page where user_id = " + userId;
        sqlObj.deleteDBEntry(query);

        query = "delete from app_user where id = " + userId;
        sqlObj.deleteDBEntry(query);

        sqlObj.closeConnection();
    }

    public static void clientUserPasswordChange(Integer LatestUserId) throws ClassNotFoundException, SQLException, InterruptedException {
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        try {

            File file = new File(ConfigureEnvironment.getEnvironmentProperty("PasswordReset"));
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                final String lineFromFile = scanner.nextLine();
                if (lineFromFile.contains("id =" + LatestUserId + ";")) {
                    logger.info("We found " + LatestUserId + " in file " + file.getName());
                    boolean resultSet = sqlObj.updateDBEntry(lineFromFile);
                }
            }
        } catch (Exception e) {
            logger.info("Failed to Execute" + " " + LatestUserId + " The error is " + e.getMessage());
        } finally {
            sqlObj.closeConnection();
        }
    }

    public static List<String> getUserAccessCriteriaDataUserId(String selectedColumns, int userId) {
        String query = "select " + selectedColumns + " from link_entity_access_criteria where entity_id = " + userId + " and entity_type_id = 20";
        return getUserDataFromQuery(query);
    }

    public static int getLatestEntityId(String entityName, int clientId) {
        String query = "select id from "+entityName+" where client_id = " + clientId + " and deleted=false order by id desc limit 1";
        Integer resultSet=0;
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        try {
            resultSet = sqlObj.viewValue(query);
        } catch (Exception e) {
            logger.info("Failed to Execute" + " " + clientId + " The error is " + e.getMessage());
        } finally {
            sqlObj.closeConnection();
        }
        return resultSet;
    }

    public static int getLatestSupplierId(String entityName, int clientId,int vendorID) {
        String query = "select id from "+entityName+" where client_id = " + clientId + " and deleted=false and vendor_id = "+vendorID+" order by id desc limit 1";
        Integer resultSet=0;
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        try {
            resultSet = sqlObj.viewValue(query);
        } catch (Exception e) {
            logger.info("Failed to Execute" + " " + clientId + " The error is " + e.getMessage());
        } finally {
            sqlObj.closeConnection();
        }
        return resultSet;
    }

    public static String getAppUserDataFromUserId(String selectedColumn, int userId) {
        String query = "select "+selectedColumn+" from app_user where id = " + userId;
        String resultSet=null;
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        try {
            resultSet = sqlObj.viewStringValue(query);
        } catch (Exception e) {
            logger.info("Failed to Execute" + " " + userId + " The error is " + e.getMessage());
        } finally {
            sqlObj.closeConnection();
        }
        return resultSet;
    }

    public static int getLatestContractDocumentId(int clientId) {
        String query = "select cd.id from contract_document cd left join contract c on c.id = cd.contract_id where c.client_id = "+clientId+" and c.deleted = false and cd.deleted = false order by cd.id desc limit 1";
        Integer resultSet=0;
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        try {
            resultSet = sqlObj.viewValue(query);
        } catch (Exception e) {
            logger.info("Failed to Execute" + " " + clientId + " The error is " + e.getMessage());
        } finally {
            sqlObj.closeConnection();
        }
        return resultSet;
    }

    public static String getUserLoginId( int userId) {
        String query = "select login_id from app_user where id = "+userId;
        String resultSet=null;
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        try {
            resultSet = sqlObj.viewStringValue(query);
        } catch (Exception e) {
            logger.info("Failed to Execute" + " " + userId + " The error is " + e.getMessage());
        } finally {
            sqlObj.closeConnection();
        }
        return resultSet;
    }

    public static Boolean toggleTierId(int userId, int tierId) {

        String query = "update app_user set tier_id ="+tierId+" where id="+userId+"";
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        Boolean result = sqlObj.updateDBEntry(query);
        return result;
    }

    public static int getUserRoleGroupId(int clientId) {
        String query = "select id from user_role_group where client_id = " + clientId + " and active = true order by id limit 1";
        Integer resultSet=0;
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        try {
            resultSet = sqlObj.viewValue(query);
        } catch (Exception e) {
            logger.info("Failed to Execute" + " " + clientId + " The error is " + e.getMessage());
        } finally {
            sqlObj.closeConnection();
        }
        return resultSet;
    }

    public static int getAccessCriteriaId(int clientId) {
        String query = "select id from access_criteria where client_id = " + clientId + " and active = true and deleted = false order by id limit 1";
        Integer resultSet=0;
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        try {
            resultSet = sqlObj.viewValue(query);
        } catch (Exception e) {
            logger.info("Failed to Execute" + " " + clientId + " The error is " + e.getMessage());
        } finally {
            sqlObj.closeConnection();
        }
        return resultSet;
    }

    public static String accessCriteriaComputationStatus(int userId, int accessCriteria) {
        String query = "select status from user_access_criteria_computation  where user_id = " + userId + " and access_criteria_id= " + accessCriteria;
        String resultSet=null;
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        try {
            resultSet = sqlObj.viewStringValue(query);
        } catch (Exception e) {
            logger.info("Failed to Execute" + " " + userId + " The error is " + e.getMessage());
        } finally {
            sqlObj.closeConnection();
        }
        return resultSet;
    }

    public static int getLatestContractIdFromMultiSupplierRelation(int vendorId, int clientId) {
        String query = "select id from contract where " + vendorId + "  = ANY(vendor_ids) and client_id = " + clientId + " and deleted = false order by id desc limit 1";
        Integer resultSet=0;
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        try {
            resultSet = sqlObj.viewValue(query);
        } catch (Exception e) {
            logger.info("Failed to Execute" + " " + clientId + " The error is " + e.getMessage());
        } finally {
            sqlObj.closeConnection();
        }
        return resultSet;
    }
}
