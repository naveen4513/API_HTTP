package com.sirionlabs.api.clientAdmin.masterRoleGroups;

import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.test.invoice.TestInvoiceMisc;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MasterRoleGroupsCreate {

    private final static Logger logger = LoggerFactory.getLogger(MasterRoleGroupsCreate.class);

    public static String getAPIPath() {
        return "/masterrolegroups/create";
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
    }

    public static List<String> getAllEntityOptions(String createResponse) {
        List<String> allEntityOptions = new ArrayList<>();

        try {
            Document html = Jsoup.parse(createResponse);
            Elements allOptions = html.getElementsByClass("form-container").get(0).child(0).children().get(0).child(1).child(0).children();

            for (int i = 2; i < allOptions.size(); i++) {
                allEntityOptions.add(allOptions.get(i).val());
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Entity Options. " + e.getMessage());
            return null;
        }

        return allEntityOptions;
    }
}