package com.sirionlabs.api.clientAdmin.invoiceCopy;

import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import org.apache.kafka.common.protocol.types.Field;

import java.util.HashMap;
import java.util.Map;

public class Copy extends TestAPIBase {

    public String getAPIPathGenerateCopy(int invoiceId) {
        return "/invoice/generate?invoiceId=" + invoiceId;
    }

    public String getAPIPathCallback() {
        return "/invoice/callback";
    }

    public String getAPIPathDownloadCopy(int id,int entityTypeId,int entityTypeId_Id,int fieldId) {
        return "/download/communicationdocument?id=" + id + "&entityTypeId=" +
                entityTypeId + "&entityType.id=" + entityTypeId_Id + "&fileId=" + fieldId;
    }

    public String getAPIPathInvoiceBulkGenerate() {
        return "/invoice-copy/bulk/generate";
    }

    public HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultLegacyHeaders();
    }

    public HashMap<String, String> getHeadersFormType() {

        HashMap<String, String> headers = new HashMap<>();
        headers.put("Accept","*/*");
        headers.put("Accept-Encoding","gzip, deflate");
        headers.put("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");
        headers.put("X-Requested-With", "XMLHttpRequest");
        return headers;
    }

    public APIResponse generateCopy(int invoiceId) {

        APIResponse response = null;
        try {
            response = executor.get(getAPIPathGenerateCopy(invoiceId), getHeaders()).getResponse();

        }catch (Exception e){

        }
        return response;
    }

    public String invoiceCallback(String payload) {

        String responseBody = null;
        try {
            responseBody = executor.post(getAPIPathCallback(), getHeaders(),payload).getResponse().getResponseBody();

        }catch (Exception e){

        }
        return responseBody;
    }

    public String downloadCopy(int id,int entityTypeId,int entityTypeId_Id,int fieldId,String filePath,String fileName) {

        String responseBody = null;
        try {
            responseBody = executor.get(getAPIPathDownloadCopy(id,entityTypeId,entityTypeId_Id,fieldId), getHeaders()).getResponse().getResponseBody();

        }catch (Exception e){

        }
        return responseBody;
    }

    public String bulkGenerateCopy(Map<String, String> parameters) {

        String responseBody = null;
        try {
            responseBody = executor.post(getAPIPathInvoiceBulkGenerate(), getHeadersFormType(),null,parameters).getResponse().getResponseBody();

        }catch (Exception e){

        }
        return responseBody;
    }


}
