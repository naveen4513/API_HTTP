package com.sirionlabs.helper.dbHelper;

import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


public class ListChartsDbHelper {

    private final static Logger logger = LoggerFactory.getLogger(ListChartsDbHelper.class);

    private static List<List<String>> getListingChartData(String query) {
        List<List<String>> data = new ArrayList<>();
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
        try {

            data = sqlObj.doSelect(query);

        } catch (Exception e) {
            logger.error("Exception while Getting Data from DB using query [{}]. {}", query, e.getMessage());
        } finally {
            sqlObj.closeConnection();
        }

        return data;
    }


    public static List<List<String>> getAllChartColumns(Integer clientId, int listId, String userLanguageId) {
        String query = "select case when dcm.entity_field_id is not NULL then get_label_by_id(ef.alias, ef.alias_label_id, " + clientId + ", " + userLanguageId + ")\n" +
                "        when dcm.system_label_id is not null then get_label_by_id(dlccm.name, dcm.system_label_id, " + clientId + ", " + userLanguageId + ")\n" +
                "        when dlccm.name is not null then dlccm.name\n" +
                "       else dcm.name end as xaxisDisplay,\n" +
                "        dcm.sql_name as xaxislevel\n" +
                "        from dynamic_column_metadata dcm\n" +
                "        left join entity_field ef on ef.id = dcm.entity_field_id\n" +
                "        left join dynamic_list_column_client_metadata dlccm on dlccm.list_column_id = dcm.id\n" +
                "        left join dynamic_details_per_entity_type ddpet on ddpet.id = dcm.dynamic_detail_id\n" +
                "        where ddpet.chart_support = true and dcm.list_id =" + listId + " and dlccm.client_id = " + clientId + "\n" +
                "        union\n" +
                "        select case when dlccme.entity_field_id is not NULL then get_label_by_id(ef.alias, ef.alias_label_id, " + clientId + ", " + userLanguageId + ")\n" +
                "        when dlccme.system_label_id is not null then get_label_by_id(dlccme.name, dlccme.system_label_id, " + clientId + ", " + userLanguageId + ")\n" +
                "       else dlccme.name end as xaxisDisplay,\n" +
                "        dlccme.sql_name as xaxislevel\n" +
                "        from dynamic_list_column_client_metadata_extended dlccme\n" +
                "        inner join entity_field ef on ef.id = dlccme.entity_field_id\n" +
                "        inner join request_field_mapping rfm on rfm.field_id = ef.id\n" +
                "        inner join dynamic_list_client_metadata dlcm on dlcm.id = dlccme.list_client_id\n" +
                "        where dlcm.list_id = " + listId + " and dlccme.client_id = " + clientId + " and rfm.editable_html_type in (3,4) and dlccme.deleted = false";
        return getListingChartData(query);
    }

    public static List<List<String>> getTotalSavedData(int listId, int clientId, int userId) {
        String query = "select count(id) from dynamic_listing_user_charts where list_id=" + listId + " and client_id=" + clientId + " and user_id=" + userId + " and deleted=false";
        return getListingChartData(query);
    }

    public static List<List<String>> getListingChartSavedData(int listId, int clientId) {
        String query = "select id from dynamic_listing_user_charts where list_id=" + listId + " and client_id=" + clientId + " order by id desc";
        return getListingChartData(query);
    }

    public static List<List<String>> deleteLatestId(int id) {
        String query = "delete from dynamic_listing_user_charts where id=" + id;
        return getListingChartData(query);
    }

}
