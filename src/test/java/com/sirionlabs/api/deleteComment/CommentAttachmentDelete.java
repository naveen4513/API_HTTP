    package com.sirionlabs.api.deleteComment;

    import com.sirionlabs.helper.api.APIResponse;
    import com.sirionlabs.helper.api.ApiHeaders;
    import com.sirionlabs.helper.api.TestAPIBase;

    import java.util.HashMap;
    public class CommentAttachmentDelete extends TestAPIBase {
        public String getApiPath() {
            return "/commentAttachment/delete/";
        }
        public HashMap<String, String> getHeaders() {

            HashMap<String, String> headers = new HashMap<>();
            headers.put("Accept", "*/*");
            headers.put("Accept-Encoding", "gzip, deflate");

            return headers;
        }
        public APIResponse getResponse(String entityTypeId, String entityId, String auditLogId)
        {
            String apiPath=getApiPath()+entityTypeId+"/"+entityId+"/"+auditLogId;
            APIResponse response = executor.delete(apiPath, getHeaders()).getResponse();
            return response;
        }

        public APIResponse getResponse(String entityTypeId, String entityId, String auditLogId,String queryString)
        {
            String apiPath=getApiPath()+entityTypeId+"/"+entityId+"/"+auditLogId+ queryString;
            APIResponse response = executor.delete(apiPath, getHeaders()).getResponse();
            return response;
        }

    }
