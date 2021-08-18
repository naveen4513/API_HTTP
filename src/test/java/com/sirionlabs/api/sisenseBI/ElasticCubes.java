package com.sirionlabs.api.sisenseBI;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElasticCubes extends APIUtils {

    private final static Logger logger = LoggerFactory.getLogger(ElasticCubes.class);
    private String SisenseGetSQLQueryFromTableAPIResponseStr;
    private String SisenseGetDataSecurityAPIResponseStr;

    public HttpResponse hitSisenseGetSQLQueryFromTableAPI(String server,String title,String tableName,String cookie) {
        HttpResponse response = null;
        try {
            HttpGet getRequest;
            String queryString = "/analytics/api/v1/elasticubes/" + server + "/" + title + "/sql_manual_query/" + tableName;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            //getRequest.addHeader("Accept", "application/json");
            getRequest.addHeader("Authorization", "Bearer " + cookie);

            response = super.getRequestSisense(getRequest, false);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.SisenseGetSQLQueryFromTableAPIResponseStr = EntityUtils.toString(response.getEntity());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Search Entity Types response header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Search Entity Types Api. {}", e.getMessage());
        }
        return response;
    }

    public HttpResponse hitSisenseGetDataSecurityAPI(String title,String cookie) {
        HttpResponse response = null;
        try {
            HttpGet getRequest;
            String queryString = "/analytics//api/elasticubes/LocalHost/" + title + "/datasecurity";
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            getRequest.addHeader("Authorization", "Bearer " + cookie);

            response = super.getRequestSisense(getRequest, false);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            this.SisenseGetDataSecurityAPIResponseStr = EntityUtils.toString(response.getEntity());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Search Entity Types response header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Search Entity Types Api. {}", e.getMessage());
        }
        return response;
    }

    public String getSQLQueryFromTableAPIResponseStr(){
        return SisenseGetSQLQueryFromTableAPIResponseStr;
    }

    public String getSisenseGetDataSecurityAPIResponseStr(){
        return SisenseGetDataSecurityAPIResponseStr;
    }

}
