package com.sirionlabs.helper.dbHelper;

import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AccessCriteriaDbHelper {

    private final static Logger logger = LoggerFactory.getLogger(AccessCriteriaDbHelper.class);

    private static List<String> getCriteriaDataFromQuery(String query) {
        List<String> data = new ArrayList<>();
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        try {

            List<List<String>> results = sqlObj.doSelect(query);
            if (!results.isEmpty()) {
                data = results.get(0);
            }
        } catch (Exception e) {
            logger.error("Exception while Getting User Data from DB using query [{}]. {}", query, e.getMessage());
        }finally {
            sqlObj.closeConnection();
        }
        return data;
    }

    private static List<String> getAccessCriteriaDataFromQuery(String query) {
        List<String> data = new ArrayList<>();
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        try {
            List<List<String>> results = sqlObj.doSelect(query);
            if (!results.isEmpty()) {
                data = results.get(0);
            }
        } catch (Exception e) {
            logger.error("Exception while Getting User Data from DB using query [{}]. {}", query, e.getMessage());
        }finally {
            sqlObj.closeConnection();
        }
        return data;
    }

    public static List<String> getCriteriaDataFromId(int uacId) {
        return getCriteriaDataFromId("*", uacId);
    }

    public static List<String> getCriteriaDataFromId(String selectedColumns, int uacId) {
        String query = "select " + selectedColumns + " from access_criteria where id = " + uacId;
        return getCriteriaDataFromQuery(query);
    }

    public static int getLatestUacId() {
        String query = "select id from access_criteria order by id desc limit 1";

        List<String> results = getCriteriaDataFromQuery(query);
        if (results.isEmpty()) {
            return -1;
        }

        return Integer.parseInt(results.get(0));
    }

    public static int getLatestUacIdFromName(Integer clientID) {
        String query = "select id from access_criteria where client_id = "+clientID +" order by id desc limit 1";

        List<String> results = getCriteriaDataFromQuery(query);
        if (results.isEmpty()) {
            return -1;
        }

        return Integer.parseInt(results.get(0));
    }

    public static int getContarctAccessId(int userId,int sourceTypeId) {
        String query = "select entity_id from link_entity_admin_access where source_type_id = "+sourceTypeId+" and source_id = "+userId;

        List<String> results = getAccessCriteriaDataFromQuery(query);
        if (results.isEmpty()) {
            return -1;
        }

        return Integer.parseInt(results.get(0));
    }

    public static void deleteAccessCriteria(int uacId) throws SQLException {
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        String query = "delete from  link_data_access_criteria where access_criteria_id = " + uacId;
        sqlObj.deleteDBEntry(query);

        query = "delete from access_criteria where id = " + uacId;
        sqlObj.deleteDBEntry(query);

        sqlObj.closeConnection();
    }

    public static int deleteAccessCriteriaComputation(int userId, int accessCriteria) throws SQLException {
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        String query = "delete from user_access_criteria_computation  where user_id = " + userId + " and access_criteria_id= "+ accessCriteria;
        try {
            sqlObj.deleteDBEntry(query);
        } catch (Exception e) {
            logger.info("Failed to Execute" + " " + userId + " The error is " + e.getMessage());
        } finally {
            sqlObj.closeConnection();
        }
        return 0;
    }

    public static String getContarctDocumentAccessId(int userId) {
        String query = "select total_accessible_documents from app_user where id = "+userId;
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

    public static List<String> getUserIDsFromName(String Name){

        String query = "select id from app_user where login_id ilike '"+Name+"%'";
        return getAppUserDataFromQuery(query);
    }

    public static void deleteUserDetails(String IDs) throws SQLException {
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        String query = "select hard_delete_app_user_user_access("+IDs+");";
        sqlObj.doSelect(query);
    }

    public static List<String> getAccessCriteriaIDsFromName(){

        String query = "select id from access_criteria   where name  ilike 'API%'";
        return getAppUserDataFromQuery(query);
    }

    public static void deleteAccessCriteriaDetails(String IDs) throws SQLException {
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        String query = "select hard_delete_access_criteria("+IDs+");";
        sqlObj.doSelect(query);
    }

    private static List<String> getAppUserDataFromQuery(String query) {
        List<String> data = new ArrayList<>();
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        try {
            List<String> results = sqlObj.viewListStringValue(query);
            if (!results.isEmpty()) {
                data=results;
            }
        } catch (Exception e) {
            logger.error("Exception while Getting User Data from DB using query [{}]. {}", query, e.getMessage());
        }finally {
            try {
                sqlObj.closeConnection();
            }catch (Exception e){
                logger.error(e.getMessage());
            }

        }
        return data;
    }

    public static void hard_delete_app_user_user_access() throws SQLException {
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        String query = "CREATE OR REPLACE FUNCTION hard_delete_app_user_user_access(userid integer)\n" +
                "  RETURNS void AS\n" +
                "$BODY$\n" +
                "begin\n" +
                "  delete from used_password where user_id = userid;\n" +
                "  delete from user_activity_tracker where user_id = userid;\n" +
                "  delete from user_token_info where user_id = userid;\n" +
                "  delete from user_terms_acceptance where user_id = userid;\n" +
                "  delete from app_user_success_login_attempt where user_id = userid;\n" +
                "  delete from data_access  where user_id = userid;\n" +
                "  delete from mass_email_distribution_list_role_link where distribution_list_id in (select id from mass_email_distribution_list where created_by = userid);\n" +
                "  delete from mass_email_distribution_list_subscriber_detail_link where distribution_list_id in (select id from mass_email_distribution_list where created_by = userid);\n" +
                "  delete from mass_email_distribution_list_supplier_link where distribution_list_id in (select id from mass_email_distribution_list where created_by = userid);\n" +
                "  delete from mass_email_distribution_list_system_users_link where distribution_list_id in (select id from mass_email_distribution_list where created_by = userid);\n" +
                "  delete from mass_email_to_distribution_list_link where distribution_list_id in (select id from mass_email_distribution_list where created_by = userid);\n" +
                "  delete from mass_email_distribution_list where created_by = userid;\n" +
                "  delete from user_data_read_access where user_id = userid;\n" +
                "  delete from user_data_write_access where user_id = userid;\n" +
                "  delete from user_data_access where user_id = userid;\n" +
                "  delete from data_access_user where user_id = userid;\n" +
                "  delete from secondary_data_access where user_id = userid;\n" +
                "  delete from workflow_manual_task_audit_log where user_id = userid;\n" +
                "  delete from insight_user_metadata where user_id = userid;\n" +
                "  delete from client_currency_conversion_datetype where last_modified_by = userid;\n" +
                "  delete from mass_email_to_users_link where user_id = userid;\n" +
                "  delete from email_access where user_id = userid;\n" +
                "  delete from link_user_group_user where user_id = userid;\n" +
                "  delete from other_audit_log_document_file where document_id in (select id from other_audit_log_document where audit_log_id in (select id from other_audit_log where requested_by = userid));\n" +
                "  delete from other_audit_log_document where audit_log_id in (select id from other_audit_log where requested_by = userid);\n" +
                "  delete from other_audit_log where requested_by = userid;\n" +
                "  delete from entity_workflow_migration_pending where client_id = userid;\n" +
                "  delete from bulk_workflow_migration_request where client_id = userid;\n" +
                "  delete from intpn_question where intpn_id in (select id from contract_intpn where created_by_user_id = userid);\n" +
                "  delete from intpn_answer where intpn_id in (select id from contract_intpn where created_by_user_id = userid);\n" +
                "  delete from contract_intpn where created_by_user_id = userid;\n" +
                "  delete from mass_email_external_email where client_id = userid;\n" +
                "  delete from mass_email_to_blacklisted_email_link where mass_email_id in (select id from mass_email where client_id = userid);\n" +
                "  delete from mass_email where client_id = userid;\n" +
                "  delete from workflow_email_audit_log where user_id = userid;\n" +
                "  delete from workflow_value_update_audit_log where user_id = userid;\n" +
                "  delete from auto_create_entity where requested_by = userid;\n" +
                "  delete from mgmt_audit_log_document_file where document_id in (select id from mgmt_audit_log_document where audit_log_id in (select id from mgmt_audit_log where requested_by = userid));\n" +
                "  delete from mgmt_audit_log_document where audit_log_id in (select id from mgmt_audit_log where requested_by = userid);\n" +
                "  delete from mgmt_audit_log where requested_by = userid;\n" +
                "  delete from dno_sla_audit_log_document_file where document_id in (select id from dno_sla_audit_log_document where audit_log_id in (select id from dno_sla_audit_log where requested_by = userid));\n" +
                "  delete from dno_sla_audit_log_document where audit_log_id in (select id from dno_sla_audit_log where requested_by = userid);\n" +
                "  delete from dno_sla_audit_log where requested_by = userid;\n" +
                "  delete from pin_user_preference_metadata where user_id = userid;\n" +
                "  delete from document_download_request where user_id = userid;\n" +
                "  delete from schedule_raw_data_download where user_id = userid;\n" +
                "  delete from schedule_email_user_data where user_id = userid;\n" +
                "  delete from user_tasks where user_id = userid;\n" +
                "  delete from user_home_page where user_id = userid;\n" +
                "  delete from app_user_failed_login_attempt where user_id = userid;\n" +
                "  delete from sso_user_role_info where user_id = userid;\n" +
                "  delete from user_chart_preference where user_id = userid;\n" +
                "  delete from app_user where id =userid;\n" +
                "end;\n" +
                "$BODY$\n" +
                "LANGUAGE plpgsql VOLATILE\n" +
                "COST 100;\n" +
                "ALTER FUNCTION hard_delete_app_user(integer)\n" +
                "OWNER TO postgres;\n";
        sqlObj.doSelect(query);
    }
}