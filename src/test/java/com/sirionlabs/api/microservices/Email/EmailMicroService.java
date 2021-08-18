package com.sirionlabs.api.microservices.Email;

import com.sirionlabs.helper.api.APIExecutor;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.ApiHeaders;


import java.util.HashMap;

public class EmailMicroService {




    public APIValidator hitPostCreateEmailConfiguration(APIExecutor executor,String domain,String payload){

        String queryString = "/letterbox/email/config/create";
        return  executor.post(domain,queryString,ApiHeaders.getEmailDefaultHeaders(),payload,null);

    }


    public APIValidator hitfindDefaultTemplate(APIExecutor executor,String domain,String clientId,String entityTypeId){

        String queryString = "/letterbox/email/config/default/"+clientId+"/"+entityTypeId;
        return  executor.get(domain,queryString, ApiHeaders.getEmailDefaultHeaders());


    }

    public APIValidator hitlistEntityActionNames(APIExecutor executor, String domain, String clientId, String entityTypeId){

        String queryString = "/letterbox/email/config/names/"+clientId+"/"+entityTypeId;
        return  executor.get(domain,queryString,ApiHeaders.getEmailDefaultHeaders());


    }

    public  APIValidator hitFindEmailConfiguration(APIExecutor executor, String domain, String clientId, String entityTypeId,String languageId,String name){
        String queryString = "/letterbox/email/config/show/"+clientId+"/"+entityTypeId+"?languageId="+languageId+"&name="+name.replace(" ","%20");
        return  executor.get(domain,queryString,ApiHeaders.getEmailDefaultHeaders());
    }

    public APIValidator hitUpdateEmailConfiguration(APIExecutor executor,String domain,String payload){

        String queryString = "/letterbox/email/config/update";
        return  executor.post(domain,queryString,ApiHeaders.getEmailDefaultHeaders(),payload,null);

    }

    public APIValidator hitURGMapperApi(APIExecutor executor,String domain,String payload,String entityTypeId,String entityId,Boolean activeUser,Boolean workflow,String authToken){

        HashMap<String, String> headers = ApiHeaders.getEmailDefaultHeaders();
        headers.put("Authorization",authToken);

        String queryString = "/tblsystemEmailConfigurations/populateRecipient/"+entityTypeId+"/+"+entityId+"?activeUsersOnly="+activeUser+"&isWorkflow="+workflow;

        return  executor.post(domain,queryString,headers,payload,null);

    }

    public APIValidator hitURGMapperApi(APIExecutor executor,String domain,String payload,String entityTypeId,String entityId,Boolean activeUser,Boolean workflow,HashMap<String, String> headers){


        String queryString = "/tblsystemEmailConfigurations/populateRecipient/"+entityTypeId+"/+"+entityId+"?activeUsersOnly="+activeUser+"&isWorkflow="+workflow;

        return  executor.post(domain,queryString,headers,payload,null);

    }




}
