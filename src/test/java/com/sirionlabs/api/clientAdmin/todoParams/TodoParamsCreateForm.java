package com.sirionlabs.api.clientAdmin.todoParams;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TodoParamsCreateForm extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(TodoParamsCreateForm.class);

    public static String getAPIPath(int formId) {
        return "/todoparams/createForm/" + formId;
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
    }

    public static String getCreateFormResponse(String apiPath, HashMap<String, String> headers) {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        AdminHelper adminHelperObj = new AdminHelper();
        adminHelperObj.loginWithClientAdminUser();

        String response = executor.get(apiPath, headers).getResponse().getResponseBody();

        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);

        return response;
    }

    public static List<String> getAllSelectedStatusForEntity(String createFormResponse, int entityTypeId) {
        List<String> allSelectedStatus = new ArrayList<>();

        try {
            Document html = Jsoup.parse(createFormResponse);
            Elements allEntityDivs = html.getElementById("_title_fc_com_sirionlabs_model_params_id").child(1).child(0).child(0).child(1).children();

            String expectedFieldLabel = getEntityLabelNameFromEntityName(entityTypeId);

            boolean fieldLabelFound = false;

            for (int i = 1; i < allEntityDivs.size(); i++) {
                String actualLabel = allEntityDivs.get(i).child(0).childNode(0).toString().replace("\n", "");

                if (expectedFieldLabel.equalsIgnoreCase(actualLabel)) {
                    fieldLabelFound = true;

                    Elements allStatusDiv = allEntityDivs.get(i).child(1).child(5).child(0).child(0).child(0).child(0).child(0).child(0).child(0).children()
                            .get(3).child(0).children();

                    for (Element statusDiv : allStatusDiv) {
                        String statusName = statusDiv.childNode(0).toString();
                        allSelectedStatus.add(statusName);
                    }

                    break;
                }
            }

            if (!fieldLabelFound) {
                return null;
            }

        } catch (Exception e) {
            logger.error("Exception while Getting All Selected Status for EntityTypeId {}. {}", entityTypeId, e.getMessage());
            return null;
        }

        return allSelectedStatus;
    }

    private static String getEntityLabelNameFromEntityName(int entityTypeId) {
        String entityLabel = null;

        switch (entityTypeId) {
            case 18:
                entityLabel = "Action";
                break;

            case 1:
                entityLabel = "Supplier";
                break;

            case 12:
                entityLabel = "Obligation";
                break;

            case 13:
                entityLabel = "Child Obligation";
                break;
        }

        return entityLabel;
    }
}