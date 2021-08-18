package com.sirionlabs.api.clientAdmin.docTypeConfig;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class DocTypeConfigList extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(DocTypeConfigList.class);

    public static String getApiPath() {
        return "/doctypeconfig/list";
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
    }

    public static String getDocTypeConfigListResponse() {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        new AdminHelper().loginWithClientAdminUser();
        logger.info("Hitting DocTypeConfig List API.");
        String response = executor.get(getApiPath(), getHeaders()).getResponse().getResponseBody();
        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);

        return response;
    }

    public static Map<String, String> getAllDocTypeMap(String docTypeConfigListResponse) {
        Map<String, String> docTypesMap = new HashMap<>();
        Document html = Jsoup.parse(docTypeConfigListResponse);
        Elements allElements = html.getElementById("l_com_sirionlabs_model_documenttypeprovision").child(1).children();

        for (int i = 0; i < allElements.size() - 1; i++) {
            String docType = allElements.get(i).children().get(0).child(0).child(0).childNode(0).toString().replace("\n", "");
            String id = allElements.get(i).children().get(0).child(0).attributes().get("href").split("/")[3];

            docTypesMap.put(docType.toLowerCase(), id);
        }

        return docTypesMap;
    }
}