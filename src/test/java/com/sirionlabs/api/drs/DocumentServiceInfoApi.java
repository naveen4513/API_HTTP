package com.sirionlabs.api.drs;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class DocumentServiceInfoApi  extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(DocumentServiceInfoApi.class);

    String queryString;

    public HashMap<String,String > getHeader()
    {
        return ApiHeaders.getDefaultLegacyHeaders();
    }

    public APIResponse getDocumentServiceInfoApiWithoutAuth(String hostUrl,String clientId, String documentId)
    {
        queryString="/drs/document/v1/clients/"+clientId+"/documents/"+documentId+"/info";
        logger.info("Hitting Get API "+ hostUrl+queryString);
        return  executor.getWithoutAuthorization(hostUrl,queryString,ApiHeaders.getDefaultLegacyHeaders()).getResponse();
    }



    public APIResponse getDocumentServiceInfoApi(String hostUrl,String clientId, String documentId)
    {
        queryString="/drs/document/v1/clients/"+clientId+"/documents/"+documentId+"/info";
        logger.info("Hitting Get API "+ hostUrl+queryString);
        return  executor.get(hostUrl,queryString,getHeader()).getResponse();
    }

    public APIResponse getDRSInfoApiWithInvalidMethod(String hostUrl,String clientId, String documentId)
    {
        queryString="/drs/document/v1/clients/"+clientId+"/documents/"+documentId+"/info";
        logger.info("Hitting Post API "+ hostUrl+queryString);
        return  executor.post(hostUrl,queryString,getHeader(),"").getResponse();
    }

    public APIResponse getDRSInfoApiWithInvalidPath(String hostUrl,String clientId, String documentId)
    {
        queryString="/drs/document/v1/clients/"+clientId+"/documents/"+documentId+"/infotest";
        logger.info("Hitting Get API "+ hostUrl+queryString);
        return  executor.get(hostUrl,queryString,getHeader()).getResponse();
    }





}
