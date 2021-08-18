package com.sirionlabs.test.api.OAuth;

import com.sirionlabs.api.OAuth.*;
import com.sirionlabs.api.listRenderer.ListDataAPI;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.apache.http.HttpStatus;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;

public class OAuth {
    private final static Logger logger = LoggerFactory.getLogger(OAuth.class);
    private static String payload;

    @BeforeClass()
    public static void createPayload() {
        payload = "{\"userRoleGroups\":[{\"name\":\"Admin\",\"id\":1070}],\"accessCriterias\":[{\"name\":\"System\",\"id\":1004}],\"purpose\":\"Testing purpose\",\"accessTokenValiditySeconds\":300}";
    }

    @Test
    public void apiTesting() {
        CustomAssert customAssert = new CustomAssert();
        try {
            logger.info("**** This POST API will be used to add client details - client Id and secrets ****");
            String newOAuthClientSecret = null;
            AddClientAPI addClientAPI = new AddClientAPI();
            APIResponse apiResponse = addClientAPI.addClientAPI(payload);
            if (apiResponse.getResponseCode() == HttpStatus.SC_OK) {
                String addClientAPIResponse = apiResponse.getResponseBody();
                if (ParseJsonResponse.validJsonResponse(addClientAPIResponse)) {
                    logger.info(addClientAPIResponse);

                    JSONObject jsonObjectAddClientAPIResponse = new JSONObject(addClientAPIResponse);
                    String status = jsonObjectAddClientAPIResponse.getString("status");
                    String message = jsonObjectAddClientAPIResponse.getString("message");

                    logger.info(message);
                    if (status.equalsIgnoreCase("ok") && message.equalsIgnoreCase("OAuth client details created successfully")) {

                        // get OAuth Client Id and OAuth Secret
                        ClientListingAPI clientListingAPI = new ClientListingAPI();
                        APIResponse clientListingAPIResponse = clientListingAPI.getClientList();
                        if (clientListingAPIResponse.getResponseCode() == HttpStatus.SC_OK) {
                            JSONObject jsonObjectClientListingAPIResponse = new JSONObject(clientListingAPIResponse.getResponseBody());
                            if (jsonObjectClientListingAPIResponse.getString("status").equalsIgnoreCase("ok")) {

                                JSONArray jsonArrayClientListingAPIResponse = jsonObjectClientListingAPIResponse.getJSONArray("data");
                                String oauthClientId = jsonArrayClientListingAPIResponse.getJSONObject(0).getString("oauthClientId");
                                String userRoleGroupsName = jsonArrayClientListingAPIResponse.getJSONObject(0).getString("userRoleGroupsName");
                                String accessCriteriasName = jsonArrayClientListingAPIResponse.getJSONObject(0).getString("accessCriteriasName");
                                String purpose = jsonArrayClientListingAPIResponse.getJSONObject(0).getString("purpose");
                                logger.info("OAuth Client Id -> {}",oauthClientId);
                                logger.info("OAuth userRoleGroupsName -> {}",userRoleGroupsName);
                                logger.info("OAuth accessCriteriasName -> {}",accessCriteriasName);
                                logger.info("OAuth  purpose-> {}",purpose);

                                //Client Details API
                                ClientDetailsAPI clientDetailsAPI = new ClientDetailsAPI();
                                APIResponse clientDetails = clientDetailsAPI.getClientDetails(oauthClientId);
                                if (clientDetails.getResponseCode() == HttpStatus.SC_OK) {
                                    JSONObject jsonObjectClientDetailsAPIResponse = new JSONObject(clientDetails.getResponseBody());
                                    if (jsonObjectClientDetailsAPIResponse.getString("status").equalsIgnoreCase("ok")) {
                                        JSONObject jsonObjectClientDetailsResponse = jsonObjectClientDetailsAPIResponse.getJSONObject("data");
                                        String oauthClientSecret = jsonObjectClientDetailsResponse.getString("oauthClientSecret");
                                        int userId = jsonObjectClientDetailsResponse.getInt("userId");
                                        purpose = jsonObjectClientDetailsResponse.getString("purpose");
                                        logger.info("OAuth oauthClientSecret -> {}",oauthClientSecret);
                                        logger.info("OAuth userId -> {}",userId);
                                        logger.info("OAuth  purpose-> {}",purpose);
                                        // verify End User API Response
                                        verifyEndUserAPI(oauthClientId, oauthClientSecret,"success", customAssert);

                                        // verify End User API Response with Regenerate Client Secret
                                        RegenerateClientSecret regenerateClientSecret = new RegenerateClientSecret();
                                        APIResponse regenerateClientSecretResponse = regenerateClientSecret.getRegenerateClientSecret(oauthClientId);
                                        if (regenerateClientSecretResponse.getResponseCode() == HttpStatus.SC_OK) {
                                            JSONObject jsonObjectRegenerateClientSecret = new JSONObject(regenerateClientSecretResponse.getResponseBody());
                                            newOAuthClientSecret = jsonObjectRegenerateClientSecret.getString("data");
                                            customAssert.assertTrue(jsonObjectRegenerateClientSecret.getString("status").equalsIgnoreCase("ok"), "");

                                            logger.info("OAuth newOAuthClientSecret -> {}",newOAuthClientSecret);
                                            verifyEndUserAPI(oauthClientId, newOAuthClientSecret,"success", customAssert);

                                            // verify End User API Response with old Client Secret
                                            verifyEndUserAPI(oauthClientId, oauthClientSecret,"Internal Server Error", customAssert);
                                        }

                                        //verify End User API Response for editing the oauth client
                                        payload = "{\"userRoleGroups\":[{\"name\":\"Admin\",\"id\":1070},{\"name\":\"Admin_clone\",\"id\":1386}],\"accessCriterias\":[{\"name\":\"System\",\"id\":1004},{\"name\":\"System Access\",\"id\":1190}],\"purpose\":\"Testing purpose testing\",\"oauthClientId\":\""+oauthClientId+"\",\"accessTokenValiditySeconds\" : 300}";
                                        UpdateClient updateClient = new UpdateClient();
                                        APIResponse updateClientSecret = updateClient.updateClientAPI(userId, payload);
                                        if (updateClientSecret.getResponseCode() == HttpStatus.SC_OK) {
                                            if (ParseJsonResponse.validJsonResponse(updateClientSecret.getResponseBody())) {

                                                JSONObject jsonObjectUpdateClient = new JSONObject(updateClientSecret.getResponseBody());
                                                if (jsonObjectUpdateClient.getString("message").equalsIgnoreCase("OAuth client details updated successfully") && jsonObjectUpdateClient.getString("status").equalsIgnoreCase("ok")) {

                                                    verifyEndUserAPI(oauthClientId, newOAuthClientSecret,"success", customAssert);
                                                } else {

                                                    customAssert.assertTrue(false, "OAuth client details not updated successfully");
                                                }
                                            }
                                        } else {
                                            customAssert.assertTrue(false, "updateClientSecret API Invalid Response Code");
                                        }


                                    }
                                } else {
                                    customAssert.assertTrue(clientListingAPIResponse.getResponseCode() == HttpStatus.SC_OK, "ClientDetails API Invalid Response Code");
                                }

                                // Delete Client
                                verifyDeleteClient(oauthClientId,customAssert);
                            } else {
                                logger.error("OAuth client details not created successfully");
                                customAssert.assertTrue(false, "OAuth client details not created successfully");
                            }
                        } else {
                            customAssert.assertTrue(apiResponse.getResponseCode() == HttpStatus.SC_OK, "clientListing API Invalid Response code");
                        }
                    } else {
                        customAssert.assertTrue(false, "OAuth client details not created successfully");
                    }
                }
            } else {
                customAssert.assertTrue(apiResponse.getResponseCode() == HttpStatus.SC_OK, "Add Client API Invalid Response Code");
            }

        } catch (Exception e) {
            customAssert.assertTrue(false, e.getMessage());
        }
        customAssert.assertAll();
    }
  private void  verifyDeleteClient(String oauthClientId,CustomAssert customAssert)
  {

     try {
         OAuthDeleteClient oauthDelete = new OAuthDeleteClient();
         APIResponse apiResponse = oauthDelete.deleteClientAPI("[\""+oauthClientId+"\"]");
         if (apiResponse.getResponseCode() == HttpStatus.SC_OK) {
             String deleteClientResponse = apiResponse.getResponseBody();
             if (ParseJsonResponse.validJsonResponse(deleteClientResponse)) {
                 logger.info(deleteClientResponse);
                 JSONObject jsonObjectDeleteClientAPIResponse = new JSONObject(deleteClientResponse);
                 String status = jsonObjectDeleteClientAPIResponse.getString("status");
                 String message = jsonObjectDeleteClientAPIResponse.getString("message");

                 logger.info(message);
                 if (!status.equalsIgnoreCase("ok") && !message.equalsIgnoreCase("OAuth clients deleted successfully")) {
                     customAssert.assertTrue(false,"OAuth client not Deleted successfully");
                 }
             }
         } else {
             customAssert.assertTrue(false, "Delete Client API Invalid Status Code");
         }
     }
     catch (Exception e)
     {
         customAssert.assertTrue(false,e.getMessage());
     }
  }
    private void verifyEndUserAPI(String oauthClientId, String oauthClientSecret,String errorMessage, CustomAssert customAssert) {

        OAuthTokenGenerationAPI oAuthTokenGenerationAPI = new OAuthTokenGenerationAPI();
        String payloadOAuthToken = "{\n" +
                "\t\"oauthClientId\": \"" + oauthClientId + "\",\n" +
                "    \"oauthClientSecret\": \"" + oauthClientSecret + "\"\n" +
                "}";
        APIResponse oAuthTokenGenerationAPIToken = oAuthTokenGenerationAPI.getToken(payloadOAuthToken);
        if (oAuthTokenGenerationAPIToken.getResponseCode() == HttpStatus.SC_OK) {
            if (errorMessage.equalsIgnoreCase("success")) {
                String accessToken = new JSONObject(oAuthTokenGenerationAPIToken.getResponseBody()).getJSONObject("data").getString("access_token");
                verifyListDataResponse(accessToken, 2, HttpStatus.SC_OK, "success", customAssert);
                verifyListDataResponse(accessToken, 211, HttpStatus.SC_OK, "success", customAssert);
                verifyListDataResponse(accessToken, 212, HttpStatus.SC_OK, "success", customAssert);

                // update oauth_client_access_api table
                String query = "INSERT INTO oauth_client_access_api(\n" +
                        "            id, oauth_client_id, api_url, http_method, active, created_date,\n" +
                        "            last_modified_date, last_modified_by, created_by)\n" +
                        "    VALUES (nextVal('oauth_client_access_api_seq'), '" + oauthClientId + "', '/listRenderer/list/2/listdata', null, true, now(),\n" +
                        "            now(), 1, 1)";
                if (updateOAuthClientAccessApi(query, customAssert)) {
                    verifyListDataResponse(accessToken, 2, HttpStatus.SC_OK, "success", customAssert);
                    verifyListDataResponse(accessToken, 211, HttpStatus.SC_UNAUTHORIZED, "Oauth client not authorized to access. Please contact administrator.", customAssert);
                    verifyListDataResponse(accessToken, 212, HttpStatus.SC_UNAUTHORIZED, "Oauth client not authorized to access. Please contact administrator.", customAssert);
                    query = "delete from oauth_client_access_api where oauth_client_id='" + oauthClientId + "'";
                    customAssert.assertTrue(updateOAuthClientAccessApi(query, customAssert), "oauth_client_access_api data is not deleting successfully");
                } else
                    customAssert.assertTrue(false, "Exception while updating OAuth clint id in oauth_client_access_api table");
            }
            else {
                customAssert.assertTrue(new JSONObject(oAuthTokenGenerationAPIToken.getResponseBody()).getString("message").equalsIgnoreCase("Internal Server Error"),"Access Token is generating with old secret code");
            }
        }


    }

    private HashMap<String, String> getHeader(String authorization) {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json;charset=UTF-8");
        headers.put("Accept", "application/json, text/javascript, */*; q=0.01");
        headers.put("Accept-Encoding", "gzip, deflate");
        headers.put("Authorization", authorization);
        headers.put("X-Requested-With", "XMLHttpRequest");
        headers.put("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));

        return headers;
    }

    private void verifyListDataResponse(String accessToken, int listId, int status, String errorMessage, CustomAssert customAssert) {

        try {
            ListDataAPI listDataAPI = new ListDataAPI();
            String listDataPayload = "{\"filterMap\":{}}";
            APIResponse apiResponse = listDataAPI.getListDataResponseWithoutMandatoryHeaders(listId, getHeader(accessToken), listDataPayload);
            if (apiResponse.getResponseCode() == status) {
                String listDataAPIListingResponse = apiResponse.getResponseBody();
                if (ParseJsonResponse.validJsonResponse(listDataAPIListingResponse)) {
                    if (errorMessage.equalsIgnoreCase("success")) {
                        if (new JSONObject(listDataAPIListingResponse).getJSONArray("data").length() == 0) {
                            logger.info(errorMessage);
                        } else {
                            customAssert.assertTrue(false, "Oauth client not authorized to access. Please contact administrator");
                        }
                    } else {
                        if (new JSONObject(listDataAPIListingResponse).getString("errorMessage").equalsIgnoreCase(errorMessage)) {
                            logger.info(errorMessage);
                        } else {
                            customAssert.assertTrue(false, "Oauth client authorized to access. Please contact administrator");
                        }
                    }
                }

            } else {
                customAssert.assertTrue(false, errorMessage);
            }
        } catch (Exception e) {
            customAssert.assertTrue(false, e.getMessage());
        }
    }

    private boolean updateOAuthClientAccessApi(String query, CustomAssert customAssert) {
        final String dbHostAddress = "192.168.2.158";
        final String dbPortName = "5432";
        final String dbName = "auth_db";
        final String dbUserName = "postgres";
        final String dbPassword = "T8k2H){6D$";
        try {
            PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
            logger.info("Entity Data Dump Email Query {}", query);
            return postgreSQLJDBC.updateDBEntry(query);
        } catch (Exception e) {
            customAssert.assertTrue(false, e.getMessage());
        }
        return false;
    }
}
