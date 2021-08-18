package com.sirionlabs.api.clientAdmin.docTypeConfig;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class DocTypeConfigShow extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(DocTypeConfigShow.class);

    public static String getApiPath(String docId) {
        return "/doctypeconfig/show/" + docId;
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
    }

    public static String getDocTypeConfigShowResponse(String docId) {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        new AdminHelper().loginWithClientAdminUser();
        logger.info("Hitting DocTypeConfig Show API for Doc Id {}.", docId);
        String response = executor.get(getApiPath(docId), getHeaders()).getResponse().getResponseBody();
        new Check().hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);

        return response;
    }

    public static String getAllowInCDRFlagValue(String docTypeConfigShowResponse) {
        Document html = Jsoup.parse(docTypeConfigShowResponse);

        try {
            return html.getElementById("_s_com_sirionlabs_model_documenttypeprovision_allowincdr_allowInCdr_id").childNode(0).toString().trim();
        } catch (Exception e) {
            logger.error("Exception while Getting Allow In CDR Flag Value. " + e.getMessage());
            return null;
        }
    }
}