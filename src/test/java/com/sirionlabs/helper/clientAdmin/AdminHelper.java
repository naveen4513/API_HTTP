package com.sirionlabs.helper.clientAdmin;

import com.sirionlabs.api.clientAdmin.Admin;
import com.sirionlabs.api.clientAdmin.masterContractTypes.MasterContractTypesList;
import com.sirionlabs.api.clientAdmin.masterContractTypes.MasterContractTypesShow;
import com.sirionlabs.api.clientAdmin.userConfiguration.UserConfigurationList;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Pattern;

public class AdminHelper {

    private final static Logger logger = LoggerFactory.getLogger(AdminHelper.class);

    private static List<String> getclientDataFromQuery(String query) {
        List<String> data = new ArrayList<>();

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                data = results.get(0);
            }

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Getting client Data from DB using query [{}]. {}", query, e.getMessage());
        }

        return data;
    }

    public Integer getClientId() {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        try {
            if (!loginWithClientAdminUser()) {
                logger.error("Couldn't login with Client Admin User.");
                return null;
            }

            logger.info("Hitting Admin API.");
            String adminResponse = Admin.getAdminResponseBody();

            Document html = Jsoup.parse(adminResponse);
            Element div = html.getElementsByClass("menu-box admin").get(1);
            String hrefValue = div.child(1).child(0).child(0).attr("href");

            String[] temp = hrefValue.split(Pattern.quote("/"));
            return Integer.parseInt(temp[temp.length - 1]);
        } catch (Exception e) {
            logger.error("Exception while Fetching Client Id. {}", e.getMessage());
        } finally {
            logger.info("Logging back with Previous User.");
            loginWithUser(lastLoggedInUserName, lastLoggedInUserPassword);
        }

        return null;
    }

    public int getClientIdFromDB() {
        String alias = ConfigureEnvironment.getEnvironmentProperty("Host").split("\\.")[0];
        String query = "select * from client where alias = '" + alias + "'";
        List<String> clientData = getclientDataFromQuery(query);

        return (clientData.isEmpty()) ? -1 : Integer.parseInt(clientData.get(0));
    }

    public String getClientAliasFromDB(int clientId) {
        String query = "select alias from client where id = "+ clientId;
        List<String> clientData = getclientDataFromQuery(query);

        return (clientData.isEmpty()) ? null : String.valueOf(clientData.get(0));
    }



    public boolean loginWithClientAdminUser() {
        String clientUserName = ConfigureEnvironment.getClientAdminUser();
        String clientUserPassword = ConfigureEnvironment.getClientAdminPassword();

        return loginWithClientAdminUser(clientUserName, clientUserPassword);
    }

    public boolean loginWithClientAdminUser(String userName, String userPassword) {
        return loginWithUser(userName, userPassword);
    }

    public boolean loginWithEndUser() {
        String clientUserName = ConfigureEnvironment.getEndUserLoginId();
        String clientUserPassword = ConfigureEnvironment.getEnvironmentProperty("password");

        return loginWithUser(clientUserName, clientUserPassword);
    }

    public boolean loginWithUser(String userName, String userPassword) {
        Check checkObj = new Check();
        checkObj.hitCheck(userName, userPassword);

        return (Check.getAuthorization() != null);
    }

    public Map<Integer, Map<String, String>> getAllServicesMap() {
        Map<Integer, Map<String, String>> allServicesMap = new HashMap<>();
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        try {
            logger.info("Logging In with Client Admin User.");

            if (!loginWithClientAdminUser()) {
                logger.error("Couldn't login with Client Admin User.");
                return null;
            }

            logger.info("Setting All Functions.");
            logger.info("Hitting MasterContractTypes List API.");
            String listResponse = MasterContractTypesList.getMasterContractTypesListResponseBody();

            List<Map<String, String>> allFunctionsList = MasterContractTypesList.getAllFunctionsList(listResponse);

            if (allFunctionsList == null) {
                logger.error("Couldn't set All Functions List.");
                return null;
            }

            for (Map<String, String> functionMap : allFunctionsList) {
                String functionId = functionMap.get("id");
                String functionName = functionMap.get("name");

                logger.info("Hitting MasterContractTypes Show API for Function [{}] having Id {}", functionName, functionId);
                String showResponse = MasterContractTypesShow.getMasterContractTypesShowResponseBody(functionId);

                List<Map<String, String>> allServicesList = MasterContractTypesShow.getAllServicesOfFunction(showResponse, functionName, functionId);

                if (allServicesList == null) {
                    logger.error("Couldn't get All Services List for Function [{}] having Id {}", functionName, functionId);
                    return null;
                }

                for (Map<String, String> serviceMap : allServicesList) {
                    Integer serviceId = Integer.parseInt(serviceMap.get("id"));

                    serviceMap.put("functionId", functionId);
                    serviceMap.put("functionName", functionName);

                    allServicesMap.put(serviceId, serviceMap);
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Setting All Services Map. {}", e.getMessage());
            allServicesMap = null;
        } finally {
            logger.info("Logging back with Previous User.");
            loginWithUser(lastLoggedInUserName, lastLoggedInUserPassword);
        }
        return allServicesMap;
    }

    public List<String> getDbSortedFunctionsList(Integer clientId) {
        List<String> dbSortedFunctionsList = new ArrayList<>();
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();

        try {
            String query = "select name from contract_type where client_id = " + clientId + " order by name";
            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                for (List<String> oneRow : results) {
                    dbSortedFunctionsList.add(oneRow.get(0));
                }
            }

            sqlObj.closeConnection();
            return dbSortedFunctionsList;
        } catch (Exception e) {
            logger.error("Exception while Getting Sorted Functions from DB. " + e.getMessage());
        }

        return null;
    }

    public List<String> getDbSortedServicesList(Integer clientId) {
        List<String> dbSortedServicesList = new ArrayList<>();
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();

        try {
            String query = "select name from contract_sub_type where client_id = " + clientId + " order by name";
            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                for (List<String> oneRow : results) {
                    dbSortedServicesList.add(oneRow.get(0));
                }
            }

            sqlObj.closeConnection();
            return dbSortedServicesList;
        } catch (Exception e) {
            logger.error("Exception while Getting Sorted Services List from DB. " + e.getMessage());
        }

        return null;
    }

    public Map<Integer, Map<String, String>> getAllSupplierTypeUsers() {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        Map<Integer, Map<String, String>> allSupplierTypeUsersMap = new HashMap<>();

        try {
            logger.info("Logging In with Client Admin User.");
            if (!loginWithClientAdminUser()) {
                return null;
            }

            String[] userFields = {"ID", "Type", "First Name", "Last Name", "Vendor Hierarchy"};
            List<String> userFieldsList = new ArrayList<>(Arrays.asList(userFields));

            String listResponse = UserConfigurationList.getUserConfigurationListResponseBody();

            Document html = Jsoup.parse(listResponse);
            Element div = html.getElementById("l_com_sirionlabs_model_TblUser");

            Element allHeaders = div.child(0).child(0);

            Map<String, Integer> userFieldIndexMap = new HashMap<>();

            for (int i = 0; i < allHeaders.children().size(); i++) {
                String headerName = allHeaders.child(i).child(0).childNode(0).toString();
                headerName = headerName.replace("\n", "");

                if (userFieldsList.contains(headerName)) {
                    userFieldIndexMap.put(headerName, i);
                }
            }

            Element allRecordsParentDiv = div.child(1);

            for (Element record : allRecordsParentDiv.children()) {
                String tagName = record.tagName();

                if (tagName.trim().equalsIgnoreCase("script")) {
                    continue;
                }

                Map<String, String> recordMap = new HashMap<>();

                Integer userId = null;

                Integer typeFieldIndex = userFieldIndexMap.get("Type");
                String fieldType = record.child(typeFieldIndex).child(0).childNode(0).toString().replace("\n", "");

                if (!fieldType.equalsIgnoreCase("Supplier")) {
                    continue;
                }

                for (Map.Entry<String, Integer> fieldMap : userFieldIndexMap.entrySet()) {
                    String fieldName = fieldMap.getKey();
                    Integer fieldIndex = fieldMap.getValue();

                    String value;

                    if (fieldName.equalsIgnoreCase("ID")) {
                        value = record.child(fieldIndex).child(0).child(0).childNode(0).toString().replace("\n", "");
                        userId = Integer.parseInt(value);
                    } else {
                        value = record.child(fieldIndex).child(0).childNode(0).toString().replace("\n", "");
                    }

                    recordMap.put(fieldName, value);
                }

                allSupplierTypeUsersMap.put(userId, recordMap);
            }
        } catch (Exception e) {
            logger.error("Exception while getting All Supplier Type Users. {}", e.getMessage());
        } finally {
            logger.info("Logging back with Previous User.");
            loginWithUser(lastLoggedInUserName, lastLoggedInUserPassword);
        }

        return allSupplierTypeUsersMap;
    }

    public Integer getIdFromHrefValue(String hrefValue) {
        try {
            String[] temp = hrefValue.split(Pattern.quote(";"));
            String[] temp2 = temp[0].split(Pattern.quote("/"));
            return Integer.parseInt(temp2[temp2.length - 1]);
        } catch (Exception e) {
            logger.error("Exception while Getting Id from Href Value: {}. {}", hrefValue, e.getStackTrace());
        }

        return null;
    }

    public List<String> getAllClientTypeUserNames() {
        return getAllUserNamesOfType("Client");
    }

    public List<String> getAllSirionlabsTypeUserNames() {
        return getAllUserNamesOfType("SirionLabs");
    }

    public List<String> getAllSupplierTypeUserNames() {
        return getAllUserNamesOfType("Supplier");
    }

    public List<String> getAllNonUserTypeUserNames() {
        return getAllUserNamesOfType("Non-User");
    }

    private List<String> getAllUserNamesOfType(String userType) {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        List<String> allUserNames = new ArrayList<>();

        try {
            logger.info("Logging In with Client Admin User.");
            if (!loginWithClientAdminUser()) {
                return null;
            }

            int userTypeId = -1;

            switch (userType) {
                case "Client":
                    userTypeId = 2;
                    break;

                case "Non-User":
                    userTypeId = 3;
                    break;

                case "SirionLabs":
                    userTypeId = 1;
                    break;

                case "Supplier":
                    userTypeId = 4;
                    break;
            }

            String payloadForListData = "{\"filterMap\":{\"entityTypeId\":20,\"offset\":0,\"size\":500,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\"," +
                    "\"filterJson\":{\"352\":{\"filterId\":\"352\",\"filterName\":\"userType\",\"entityFieldId\":null,\"entityFieldHtmlType\":null," +
                    "\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + userTypeId + "\",\"name\":\"" + userType + "\"}]}}}}}";

            ListRendererListData listDataObj = new ListRendererListData();
            listDataObj.hitListRendererListData(328, payloadForListData);
            String listDataResponse = listDataObj.getListDataJsonStr();

            if (ParseJsonResponse.validJsonResponse(listDataResponse)) {
                int activeColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "active");
                int nameColumnNo = ListDataHelper.getColumnIdFromColumnName(listDataResponse, "name");

                JSONObject jsonObj = new JSONObject(listDataResponse);
                JSONArray jsonArr = jsonObj.getJSONArray("data");

                for (int i = 0; i < jsonArr.length(); i++) {
                    String activeValue = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(activeColumnNo)).getString("value");

                    if (activeValue.equalsIgnoreCase("yes")) {
                        allUserNames.add(jsonArr.getJSONObject(i).getJSONObject(String.valueOf(nameColumnNo)).getString("value"));
                    }
                }
            } else {
                logger.error("ListData API Response for User Configuration List is an Invalid JSON.");
            }
        } catch (Exception e) {
            logger.error("Exception while getting All User Names of User Type {}. {}", userType, e.getMessage());
        } finally {
            logger.info("Logging back with Previous User.");
            loginWithUser(lastLoggedInUserName, lastLoggedInUserPassword);
        }

        return allUserNames;
    }

    public Set<String> getAllPermissionsForUser(String userLoginId, int clientId) {
        Set<String> allPermissions = new HashSet<>();

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            String query = "select permissions from app_user where login_id = '" + userLoginId + "' and client_id = " + clientId;

            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                String[] allPermissionIds = results.get(0).get(0).replace("{", "").replace("}", "").split(",");
                allPermissions.addAll(Arrays.asList(allPermissionIds));
            }

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Getting All Permissions for User {} and Client Id {}. {}", userLoginId, clientId, e.getStackTrace());
        }
        return allPermissions;
    }

    public boolean updatePermissionsForUser(String userLoginId, int clientId, String allPermissions) {
        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            String query = "update app_user set permissions ='" + allPermissions + "' where login_id = '" + userLoginId + "' and client_id = " + clientId;

             return sqlObj.updateDBEntry(query);
        } catch (Exception e) {
            logger.error("Exception while Updating Permissions for User {} and Client Id {}. {}", userLoginId, clientId, e.getStackTrace());
        }

        return false;
    }

    public int getReportIdFromReportName(String reportName) {
        logger.info("Getting Report Id For Report Name");
        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            String query = "select id from report where report_name ='" + reportName + "'";
            List<List<String>> results = sqlObj.doSelect(query);
            if (!results.isEmpty()) {
                return Integer.parseInt(results.get(0).get(0));

            }

        } catch (Exception e) {
            logger.error("Exception while Getting Report Id  For Report Name {}.  {}", reportName, e.getStackTrace());
        }
        return 0;
    }

    public Set<String> getReportsGrantedListForUser(String userLoginId, int clientId) {
        Set<String> allReports = new HashSet<>();
        logger.info("Getting All Report Granted List for User");
        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            String query = "select reports_granted from app_user where login_id ='" + userLoginId + "' and client_id =" + clientId;
            List<List<String>> results = sqlObj.doSelect(query);

            if (!results.isEmpty()) {
                String[] allReportIds = results.get(0).get(0).replace("{", "").replace("}", "").split(",");
                allReports.addAll(Arrays.asList(allReportIds));
            }
        } catch (Exception e) {
            logger.error("Exception While Getting Report_Granted for User {} and Client Id {}. {}", userLoginId, clientId, e.getStackTrace());
        }
        return allReports;
    }

    public boolean updateReportGrantedListForUser(String userLoginId, int clientId, String reportList) {
        logger.info("Update Report Granted List For User");
        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            String query = "update app_user SET reports_granted='" + reportList + "' where login_id='" + userLoginId + "' and client_id =" + clientId;
            return sqlObj.updateDBEntry(query);
        } catch (Exception e) {
            logger.error("Exception while Updating Report Granted List  for User {} and Client Id {}. {}", userLoginId, clientId, e.getStackTrace());
        }
        return false;
    }

    public List<Map<String, String>> getAllServicesList() {
        List<Map<String, String>> allServicesList = new ArrayList<>();
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        try {
            logger.info("Logging In with Client Admin User.");

            if (!loginWithClientAdminUser()) {
                logger.error("Couldn't login with Client Admin User.");
                return null;
            }

            logger.info("Setting All Functions.");
            logger.info("Hitting MasterContractTypes List API.");
            String listResponse = MasterContractTypesList.getMasterContractTypesListResponseBody();

            List<Map<String, String>> allFunctionsList = MasterContractTypesList.getAllFunctionsList(listResponse);

            if (allFunctionsList == null) {
                logger.error("Couldn't set All Functions List.");
                return null;
            }

            for (Map<String, String> functionMap : allFunctionsList) {
                String functionId = functionMap.get("id");
                String functionName = functionMap.get("name");

                logger.info("Hitting MasterContractTypes Show API for Function [{}] having Id {}", functionName, functionId);
                String showResponse = MasterContractTypesShow.getMasterContractTypesShowResponseBody(functionId);

                List<Map<String, String>> servicesList = MasterContractTypesShow.getAllServicesOfFunction(showResponse, functionName, functionId);

                if (servicesList != null) {
                    allServicesList.addAll(servicesList);
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Setting All Services List. {}", e.getMessage());
            allServicesList = null;
        } finally {
            logger.info("Logging back with Previous User.");
            loginWithUser(lastLoggedInUserName, lastLoggedInUserPassword);
        }
        return allServicesList;
    }

    public boolean addPermissionForUser(String userLoginId, int clientId, String permissionId) {
        try {
            Set<String> defaultPermissions = getAllPermissionsForUser(userLoginId, clientId);

            if (!defaultPermissions.contains(permissionId)) {
                defaultPermissions.add(permissionId);

                PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
                String newPermissionsStr = defaultPermissions.toString().replace("[", "{").replace("]", "}");
                String query = "update app_user set permissions ='" + newPermissionsStr + "' where login_id = '" + userLoginId + "' and client_id = " + clientId;

                return sqlObj.updateDBEntry(query);
            }
        } catch (Exception e) {
            logger.error("Exception while Updating Permissions for User {} and Client Id {}. {}", userLoginId, clientId, e.getStackTrace());
        }

        return false;
    }
}