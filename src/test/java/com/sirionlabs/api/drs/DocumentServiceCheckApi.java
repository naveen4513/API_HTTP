package com.sirionlabs.api.drs;

import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class DocumentServiceCheckApi extends TestAPIBase {
    private final static Logger logger = LoggerFactory.getLogger(DocumentServiceCheckApi.class);

    String queryString;

    public HashMap<String,String > getHeader()
    {
        return ApiHeaders.getDefaultLegacyHeaders();
    }
    public APIResponse getDocumentServiceCheckApi( String hostUrl)
    {
        queryString="/drs/health";
        logger.info("Hitting Get API "+ hostUrl+queryString);
        return  executor.get(hostUrl,queryString,getHeader()).getResponse();
    }

    public APIResponse getDRSCheckApiWithInvalidMethod(String hostUrl)
    {
        queryString="/drs/health";
        logger.info("Hitting Post API "+ hostUrl+queryString);
        return  executor.post(hostUrl,queryString,getHeader(),"").getResponse();
    }

    public APIResponse getDRSCheckApiWithInvalidPath(String hostUrl)
    {
        queryString="/drs/healthtest";
        logger.info("Hitting Get API "+ hostUrl+queryString);
        return  executor.post(hostUrl,queryString,getHeader(),"").getResponse();
    }

}
