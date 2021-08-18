package com.sirionlabs.api.autoExtraction;

import com.sirionlabs.helper.api.ApiHeaders;

import java.util.HashMap;

public class SaveParsedInfo {

    public static String getAPIPath() {
        return "/autoExtraction/saveParsedInfo";
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getContentTypeAsJsonOnlyHeader();
    }

    public static String getPayload(int documentid, int clientid, String text, int textid,int startpageno,int endpageno,int startleft,int starttop) {
        return "{\"parsedData\":{\"clientid\":"+ clientid +",\"documentid\":" + documentid+",\"text\":"+ text + ",\"textid:" + textid + ",\"startpageno\":"+ startpageno+",\"endpageno\":"+ endpageno +",\"startleft\":"+ startleft +",\"starttop\":"+ starttop+", \n" +
                "\"etpercategorylist\" : [ { \"extracttype\" : 1001, \"categoryid\" : 123, \"categoryname\" : \"EFDATE\", \"categoryscore\" : 0 }, { \"extracttype\" : 1002, \"categoryid\" :1234, \"categoryname\" : \"EXPDATE\", \"categoryscore\" : 0 } ] },\n" +
                "\"docParamLinkId\":123.1} ";
    }
}
