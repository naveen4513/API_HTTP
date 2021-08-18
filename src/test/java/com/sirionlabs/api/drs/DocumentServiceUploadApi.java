package com.sirionlabs.api.drs;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

public class DocumentServiceUploadApi extends APIUtils {

    private final static Logger logger = LoggerFactory.getLogger(DocumentServiceUploadApi.class);

    String queryString;

    public HashMap<String, String> getHeader() {
        return ApiHeaders.getMeetingNoteCreateAPIHeader();
    }

    /*public APIValidator postDocumentServiceUploadApi(HashMap<String,String> params) throws UnsupportedEncodingException {
        String hostUrl = ConfigureEnvironment.getEnvironmentProperty("document_service_url");
        queryString="/document/upload";
        logger.info("Hitting Post  API "+ hostUrl+queryString);
        return  executor.postMultiPartFormData(hostUrl,queryString,null,params);
    }
*/
    public void postDocumentServiceUploadApi(HashMap<String, String> params) throws UnsupportedEncodingException {
        File file = new File("D:\\work@sarthak\\16Jun\\java-api-framework\\src\\test\\resources\\TestData\\DRS\\Upload\\TestDRS.pdf");
        FileBody fileBody = new FileBody(file, ContentType.parse("application/pdf"));

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
        builder.addPart("file", fileBody);
        HttpEntity entity = builder.build();

        HttpPost request = new HttpPost("http://192.168.2.242:8085/document/upload");
        request.setEntity(entity);
        request.addHeader("Authorization", Check.getAuthorization());

        HttpClient client = HttpClientBuilder.create().build();
        try {
            client.execute(request);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



public HashMap<String,String> getParamas(String name, String extension, String clientId, String envDetail ){
        HashMap<String,String> params = new HashMap<>();


        if(name !="" && name !=null){
            params.put("name",name);
        }

        if(extension !="" && extension !=null){
            params.put("extension",extension);
        }
       /* if(multipartFile !="" && multipartFile !=null){
            //params.put("name","");
            params.put("filename",multipartFile);
        }*/
        if(clientId !="" && clientId !=null){
            params.put("clientId",clientId);
        }
    if(envDetail !="" && envDetail !=null){
        params.put("envDetail",envDetail);
    }


        return  params;
    }


}
