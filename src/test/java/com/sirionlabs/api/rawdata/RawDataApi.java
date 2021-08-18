package com.sirionlabs.api.rawdata;


import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class RawDataApi extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(RawDataApi.class);

    String queryString;

    public HashMap<String,String > getHeader()
    {
        return ApiHeaders.getDefaultLegacyHeaders();
    }


    public String rawDataCreate(String id,String templateId,int entityTypeId,int entityId,String payload)
    {
        queryString="/rawdata/create/" + id + "/" + templateId + "/" + entityTypeId + "/" + entityId;
        logger.info("Hitting Post API "+ queryString);
        return  executor.post(queryString,getHeader(),payload).getResponse().getResponseBody();
    }

    public String rawDataGlobalList(String payload)
    {
        queryString="/rawdata/globallist";
        logger.info("Hitting Post API "+ queryString);
        return  executor.post(queryString,getHeader(),payload).getResponse().getResponseBody();
    }

    public String lineItemIsDocUpdated(int entityTypeId,int entityId)
    {
        queryString="/lineitem/isDocumentUpdated/" + entityTypeId + "/" + entityId;
        logger.info("Hitting Post API "+ queryString);
        return  executor.get(queryString,getHeader()).getResponse().getResponseBody();
    }


}
