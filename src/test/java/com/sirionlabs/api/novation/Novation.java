package com.sirionlabs.api.novation;

import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class Novation extends TestAPIBase {

    public static Logger logger = LoggerFactory.getLogger(Novation.class);
    private String getAPIPath() {
        return "/novation/submit";
    }

    public HashMap<String, String> getHeaders() {
        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept", "application/json, text/plain, */*");
        headers.put("Content-Type", "application/json; charset=utf-8");

        return headers;
    }

    public String novationSubmit(int sourceId,int sourceParent,int destinationParent,int destEntityTypeId) {

        String responseBody = null;
        try {
            String effectiveNovDate= DateUtils.getCurrentDateInMM_DD_YYYY();

            String payload = "{\"sourceId\":" + sourceId + ",\"sourceParentId\":null,\"onlySupplierAddition\":false,\"" +
                    "replaceRelationPairs\":[{\"left\":" + sourceParent + ",\"right\":" + destinationParent + "}]," +
                    "\"effectiveNovationDate\":\"" + effectiveNovDate + "\",\"destinationEntityTypeId\":" + destEntityTypeId + "}";
            responseBody = executor.post(getAPIPath(), getHeaders(),payload).getResponse().getResponseBody();

        }catch (Exception e){
            logger.error("Exception while performing novation");
        }
        return responseBody;
    }
}
