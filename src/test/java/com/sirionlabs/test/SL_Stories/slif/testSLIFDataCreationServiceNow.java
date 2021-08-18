package com.sirionlabs.test.SL_Stories.slif;


import com.sirionlabs.api.commonAPI.Clone;
import com.sirionlabs.utils.commonUtils.HttpsTrustManager;
import com.sirionlabs.utils.commonUtils.UrlEncodedString;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URLEncoder;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class testSLIFDataCreationServiceNow {

    String authorization = "Basic YWRtaW46QWRtaW5AMTIzNDU=";
    String cookie = "glide_user_route=glide.cca3b0ef70d4acb770d3d67447c4dd33; JSESSIONID=3588FE2AC8C9AA93C2ECB89A6739A8B5; glide_session_store=8A4CD365DBD95010784D29E1CA961943; BIGipServerpool_dev79786=2390775562.47166.0000";

    String env = "dev79786.service-now.com";

    @DataProvider(name = "dataprovider",parallel = true)
    public Object[][] dataprovider() {

        List<Object[]> allTestData = new ArrayList<>();

//        urgency,impact, priority,state, assigned_to,openedAt,assignment_group,category,resolution_code

//        allTestData.add(new Object[]{"2","1","2","103","Problem CordinatorATF","2020-02-01 10:11:01","Problem Analyzers","Inquiry / Help","Fix Applied"});

//        short_description	reason	work_start	sys_updated_on	type	follow_up	sys_updated_by	opened_by	urgency	sys_created_on	sys_domain	scope	company	state	justification	order	phase	closed_at	approval	impact	due_date	active	priority	opened_at	requested_by	escalation	risk	category

//        allTestData.add(new Object[]{"2","1","2","103","Problem CordinatorATF","2020-02-01 10:11:01","Problem Analyzers","Inquiry / Help","Fix Applied"});
        for(int i=0;i<10;i++) {
            allTestData.add(new Object[]{"Performance testing data created for SLIF",
                    "User requested",
                    "2020-05-28 07:58:58",
                    "Standard",
                    "2020-05-28 07:58:58",
                    "admin",
                    "Daniel Zill",
                    "1",
                    "2020-05-28 07:58:58",
                    "global",
                    "Medium",
                    "ACME Americas",
                    "Scheduled",
                    "Test Performance Data",
                    "1",
                    "Requested",
                    "2020-05-28 07:58:58",
                    "Approved",
                    "1",
                    "2020-05-28 07:58:58",
                    "true",
                    "4",
                    "2020-05-28 07:58:58",
                    "Tami Trybus",
                    "Normal",
                    "Moderate",
                    "Applications Software"

            });
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataprovider",enabled = false)
//    public void TestIncidentCreation(String urgency,String impact,
//                                     String priority,String state,
//                                     String assigned_to,String openedAt,
//                                     String assignment_group,String category,
//                                     String resolution_code){
    public void TestDataCreation(String short_description,
                               String reason,
                               String sys_updated_on,
                               String type,
                               String follow_up,
                               String sys_updated_by,
                               String opened_by,
                               String urgency,String sys_created_on,
                               String sys_domain,String scope,
                               String company,String state,
                               String justification,String order,
                               String phase,String closed_at,
                               String approval,String impact,
                               String due_date,String activity,
                               String priority,String opened_at,
                               String requested_by,String escalation,
                               String risk,String category){

//        String path = "/api/now/table/incident";
//        String path = "/api/now/table/problem";
        String path = "/api/now/table/change_request";

        String incidentID;
        ArrayList<String> incidentIDList = new ArrayList<>();
        try {
            HashMap<String, String> createMap = createMapCreate();

//            String payload = getCreatePayload(createMap);
//            String payload = getCreatePayloadProblemTable2(urgency,impact,
//                    priority,state,assigned_to,openedAt,
//                    assignment_group,category, resolution_code);

            String payload = "";
            int highestDate = 28;
            for (int month = 1; month <= 7; month++) {
                if(month == 1 || month == 3 || month == 5 || month == 7){
                    highestDate = 31;
                }else if(month == 4 || month == 6 ) {
                    highestDate = 30;

                }else if(month == 2 ) {
                    highestDate = 29;

                }

                for (int i = 1; i <= highestDate; i++) {


                    sys_created_on = "2020-0" + month + "-" + i + " 07:58:58";
                    sys_updated_on = "2020-0" + month + "-" + i + " 07:58:58";
                    closed_at = "2020-0" + month + "-" + i + " 07:58:58";
                    due_date = "2020-0" + month + "-" + i + " 07:58:58";
                    opened_at = "2020-0" + month + "-" + i + " 07:58:58";
                    order = String.valueOf(i);

                    payload = getCreatePayloadChangeRequest2(short_description, reason, sys_updated_on, type, follow_up, sys_updated_by, opened_by, urgency, sys_created_on, sys_domain, scope, company, state, justification, order, phase, closed_at, approval, impact, due_date, activity, priority, opened_at, requested_by, escalation, risk, category);
                    HttpResponse response = postRequest(path, payload);

                }
            }

        }catch (Exception e){

        }
    }

    public HttpResponse postRequest(String path,String payload) {
        // Read Proxy config from File and control HttpClient
        HttpClient httpClient;

        HttpResponse response = null;
        String incidentID;
        ArrayList<String> incidentIDList = new ArrayList<>();

        try {
//            String hostName = "dev68841.service-now.com";
            String hostName = "dev64639.service-now.com";
            Integer portNumber = 443;
            String protocolScheme = "https";

            String hostUrl = protocolScheme + "://" + hostName + ":" + portNumber;
            HttpPost httpPostRequest = new HttpPost(hostUrl + path);

            if (payload != null) {
                httpPostRequest.setEntity(new StringEntity(payload, "UTF-8"));
            }

            HttpHost target = new HttpHost(hostName, portNumber, protocolScheme);

            SSLContext sslcontext = SSLContexts.custom().useSSL().build();

            sslcontext.init(null, new X509TrustManager[]{new HttpsTrustManager()}, new SecureRandom());

            SSLConnectionSocketFactory factory = SSLConnectionSocketFactory.getSocketFactory();

            httpClient = HttpClients.custom().setSSLSocketFactory(factory).build();

            httpPostRequest.addHeader("Accept", "application/json");
//            httpPostRequest.addHeader("Authorization", "Basic YWRtaW46aFhhaUE1U1ZCcmI3");
            httpPostRequest.addHeader("Authorization", "Basic YWRtaW46QWRtaW5AMTIzNDU=");
            httpPostRequest.addHeader("Content-Type", "application/json");
//            httpPostRequest.addHeader("Cookie", "glide_user_route=glide.bdfedf64b9e9d707bc156f6d44dce6c5; JSESSIONID=C9BD606B6102651E49B36BCCC9E0A66B; glide_session_store=21145869DB8510108BB3A455CA9619D4; BIGipServerpool_dev68841=2441107466.55102.0000");
            httpPostRequest.addHeader("Cookie", "glide_user_route=glide.3c6a6355b9a90c5cb3fc40f897711bf8; BIGipServerpool_dev64639=2558613258.40254.0000; JSESSIONID=570B2B7DD628AC4B3B66F5132E433C3C; glide_user_activity=U0N2MzpjVHVlOUlKVVRpeVhjdnMzeTZrR01IRENqc2lOeWJ2WjpPTmJGSjBCQVM0N1lJclk1UHNCQWliUjNpaWpLQXV2TU1IVkRkVm5VUk80PQ==; glide_session_store=49F3A2E2DB4950101D2248703996190C; __CJ_tabs2_section_change_request=%220%22; __CJ_g_startTime=%221590906948357%22");

            if (payload != null)
                httpPostRequest.setEntity(new StringEntity(payload, "UTF-8"));

            try {

                for(int i =0;i<300;i++) {
                    response = httpClient.execute(target, httpPostRequest);

                    String responseBody = EntityUtils.toString(response.getEntity());

                    try {
                        JSONObject responseBodyJSon = new JSONObject(responseBody);

                        incidentID = responseBodyJSon.getJSONObject("result").get("number").toString();
                        incidentIDList.add(incidentID);

                    } catch (Exception e) {
                        System.out.println("Exception");
                    }

                }
                System.out.println(incidentIDList);

            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (Exception e) {

        }
        return response;
    }

    private String getCreatePayload(HashMap<String,String> createMapCreate){

        String payload = "{\n" +
                "    \n" +
                "    \"state\": \"" +  createMapCreate.get("state") + "\",\n" +
                "    \"sys_created_by\": \"" + createMapCreate.get("sys_created_by") + "\",\n" +
                "    \"order\": \"" + createMapCreate.get("order") + "\",\n" +
                "    \"knowledge\": \"" + createMapCreate.get("knowledge") + "\",\n" +
                "    \"calendar_stc\": \"" + createMapCreate.get("calendar_stc") + "\",\n" +
                "    \"closed_at\": \"" + createMapCreate.get("closed_at") + "\",\n" +
                "    \"delivery_plan\": \"" + createMapCreate.get("delivery_plan") + "\",\n" +
                "    \"cmdb_ci\": \"" + createMapCreate.get("cmdb_ci") + "\",\n" +
                "    \"impact\": \"" + createMapCreate.get("impact") + "\",\n" +
                "    \"work_notes_list\": \"" + createMapCreate.get("work_notes_list") + "\",\n" +
                "    \"active\": \"" + createMapCreate.get("active") + "\",\n" +
                "    \"business_service\": \"" + createMapCreate.get("business_service") + "\",\n" +
                "    \"sys_domain_path\": \"" + createMapCreate.get("sys_domain_path") + "\",\n" +
                "    \"priority\": \"" + createMapCreate.get("priority") + "\",\n" +
                "    \"time_worked\": \"" + createMapCreate.get("time_worked") + "\",\n" +
                "    \"rfc\": \"" + createMapCreate.get("rfc") + "\",\n" +
                "    \"expected_start\": \"" + createMapCreate.get("expected_start") + "\",\n" +
                "    \"opened_at\": \"" + createMapCreate.get("opened_at") + "\",\n" +
                "    \"group_list\": \"" + createMapCreate.get("group_list") + "\",\n" +
                "    \"business_duration\": \"" + createMapCreate.get("business_duration") + "\",\n" +
                "    \"work_end\": \"" + createMapCreate.get("work_end") + "\",\n" +
                "    \"caller_id\": \"" + createMapCreate.get("caller_id") + "\",\n" +
                "    \"resolved_at\": \"" + createMapCreate.get("resolved_at") + "\",\n" +
                "    \"reopened_time\": \"" +createMapCreate.get("reopened_time") + "\",\n" +
                "    \"approval_set\": \"" + createMapCreate.get("approval_set") + "\",\n" +
                "    \"work_notes\": \"" + createMapCreate.get("work_notes") + "\",\n" +
                "    \"subcategory\": \"" + createMapCreate.get("subcategory") + "\",\n" +
                "    \"short_description\": \"" + createMapCreate.get("short_description") + "\",\n" +
                "    \"delivery_task\": \"" + createMapCreate.get("delivery_task") + "\",\n" +
                "    \"work_start\": \"" + createMapCreate.get("work_start") + "\",\n" +
                "    \"correlation_display\": \"" + createMapCreate.get("correlation_display") + "\",\n" +
                "    \"close_code\": \"" + createMapCreate.get("close_code") + "\",\n" +
                "    \"assignment_group\": \"" + createMapCreate.get("assignment_group") + "\",\n" +
                "    \"additional_assignee_list\": \"" + createMapCreate.get("additional_assignee_list") + "\",\n" +
                "    \"business_stc\": \"" + createMapCreate.get("business_stc") + "\",\n" +
                "    \"description\": \"" + createMapCreate.get("description") + "\",\n" +
                "    \"calendar_duration\": \"" + createMapCreate.get("calendar_duration") + "\",\n" +
                "    \"close_notes\": \"" + createMapCreate.get("close_notes") + "\",\n" +
                "    \"sys_class_name\": \"" + createMapCreate.get("sys_class_name") + "\",\n" +
                "    \"notify\": \"" + createMapCreate.get("notify") + "\",\n" +
                "    \"follow_up\": \"" + createMapCreate.get("follow_up") + "\",\n" +
                "    \"closed_by\": \"" + createMapCreate.get("closed_by") + "\",\n" +
                "    \"parent_incident\": \"" + createMapCreate.get("parent_incident") + "\",\n" +
                "    \"contact_type\": \"" + createMapCreate.get("contact_type") +"\",\n" +
                "    \"reopened_by\": \"" + createMapCreate.get("reopened_by") + "\",\n" +
                "    \"incident_state\": \"" + createMapCreate.get("incident_state") + "\",\n" +
                "    \"urgency\": \"" + createMapCreate.get("urgency") + "\",\n" +
                "    \"problem_id\": \"" + createMapCreate.get("problem_id") + "\",\n" +
                "    \"company\": \"" + createMapCreate.get("company") + "\",\n" +
                "    \"reassignment_count\": \"" + createMapCreate.get("reassignment_count") + "\",\n" +
                "    \"activity_due\": \"" + createMapCreate.get("activity_due") + "\",\n" +
                "    \"assigned_to\": \"" + createMapCreate.get("assigned_to") + "\",\n" +
                "    \"severity\": \"" + createMapCreate.get("severity") + "\",\n" +
                "    \"comments\": \"" + createMapCreate.get("comments") + "\",\n" +
                "    \"approval\": \"" + createMapCreate.get("approval") + "\",\n" +
                "    \"sla_due\": \"" + createMapCreate.get("sla_due") + "\",\n" +
                "    \"sys_mod_count\": \"" + createMapCreate.get("sys_mod_count") + "\",\n" +
                "    \"due_date\": \"" + createMapCreate.get("due_date")+ "\",\n" +
                "    \"comments_and_work_notes\": \"" + createMapCreate.get("comments_and_work_notes") + "\",\n" +
                "    \"reopen_count\": \"" + createMapCreate.get("reopen_count") + "\",\n" +
                "    \"sys_tags\": \"" + createMapCreate.get("sys_tags") + "\",\n" +
                "    \"upon_approval\": \"" + createMapCreate.get("upon_approval") + "\",\n" +
                "    \"escalation\": \"" + createMapCreate.get("escalation") + "\",\n" +
                "    \"correlation_id\": \"" + createMapCreate.get("correlation_id") + "\",\n" +
                "    \"location\": \"" + createMapCreate.get("location") + "\",\n" +
                "    \"category\": \"" + createMapCreate.get("category") + "\"\n" +
                "  }\n";

        return payload;
    }

    private HashMap<String,String> createMapCreate(){

        HashMap<String,String> createMap = new HashMap();
//        createMap.put("state","2");           //In Progress
//        createMap.put("state","7");             //Closed
//        createMap.put("state","1");             //New
        createMap.put("state","2");             //New
//        createMap.put("category","Inquiry / Help");
        createMap.put("category","Software");
        createMap.put("assignment_group","Problem Solving");

        createMap.put("caller_id","David Loo");
        createMap.put("assigned_to","Problem Manager");
        createMap.put("severity","3");
//        createMap.put("priority","5");
//        createMap.put("priority","3");
        createMap.put("priority","5");
        createMap.put("impact","3");
        createMap.put("urgency","3");

        createMap.put("incident_state","1");

        createMap.put("opened_at","2020-01-01 10:11:01");
        createMap.put("short_description","Performance Testing Data");

        createMap.put("description","Performance Testing Data");
        createMap.put("sys_class_name","incident");
//        createMap.put("impact","3");

//        createMap.put("urgency","3");
//        createMap.put("urgency","1");
        createMap.put("sys_created_by","admin");

        createMap.put("order","");
        createMap.put("knowledge","false");
        createMap.put("calendar_stc","");
        createMap.put("closed_at","");
        createMap.put("delivery_plan","");
        createMap.put("cmdb_ci","");

        createMap.put("work_notes_list","");
        createMap.put("active","true");
        createMap.put("business_service","");
        createMap.put("sys_domain_path","/");

        createMap.put("time_worked","");
        createMap.put("rfc","");
        createMap.put("expected_start","");

        createMap.put("group_list","");
        createMap.put("business_duration","");
        createMap.put("work_end","");

        createMap.put("resolved_at","");
        createMap.put("reopened_time","");
        createMap.put("approval_set","");
        createMap.put("work_notes","");
        createMap.put("subcategory","");

        createMap.put("delivery_task","");
        createMap.put("work_start","");
        createMap.put("correlation_display","");
        createMap.put("close_code","");
//        createMap.put("close_code","Solved (Work Around)");

        createMap.put("additional_assignee_list","");
        createMap.put("business_stc","");

        createMap.put("calendar_duration","");
        createMap.put("close_notes","");
//        createMap.put("close_notes","Solved WorkAround");

        createMap.put("notify","1");

        createMap.put("follow_up","");
        createMap.put("closed_by","");
        createMap.put("parent_incident","");
        createMap.put("contact_type","");
        createMap.put("reopened_by","");

        createMap.put("problem_id","");
        createMap.put("company","");
        createMap.put("reassignment_count","0");
        createMap.put("activity_due","");

        createMap.put("comments","");
        createMap.put("approval","not requested");
        createMap.put("sla_due","");
        createMap.put("sys_mod_count","0");

        createMap.put("due_date","");
        createMap.put("comments_and_work_notes","");
        createMap.put("reopen_count","0");

        createMap.put("sys_tags","");
        createMap.put("upon_approval","proceed");
        createMap.put("escalation","0");
        createMap.put("correlation_id","");
        createMap.put("location","");

        return createMap;
    }

    private String getCreatePayloadProblemTable1(){

        String payload = "{\n" +
                "  \n" +
                "    \"first_reported_by_task\": \"INC0000019\",\n" +
                "    \"parent\": \"\",\n" +
                "    \"urgency\": \"3\",\n" +
                "    \"impact\": \"3\",\n" +
                "    \"state\": \"7\",\n" +
                "    \"priority\": \"5\",\n" +
                "    \"assigned_to\": \"Problem Coordinator B\",\n" +
                "    \"category\":\"Software\",\n" +
                "    \"short_description\":\"Windows XP SP2 causing errors in Enterprise\",\n" +
                "\t\"cause_notes\":\"\\n\\t\\t\\t <p>Corporate desktop apps do not support SP2</p>\",\n" +
                "\t\"fix_notes\": \"\\n\\t\\t\\t <p>Remove Windows XP SP2</p>\",\n" +
                "\t\"related_incidents\": \"1\",\n" +
                "\t\"rfc\": {\n" +
                "      \"link\": \"https://dev68841.service-now.com/api/now/table/change_request/46e9b4afa9fe198101026e122b85f442\",\n" +
                "      \"value\": \"46e9b4afa9fe198101026e122b85f442\"\n" +
                "    }\n" +
                "}\n" +
                "\n";

        return payload;
    }

    private String getCreatePayloadProblemTable2(String urgency,String impact,
                                                 String priority,String state,
                                                 String assigned_to,String openedAt,
                                                 String assignment_group,String category,
                                                 String resolution_code
    ){

        String payload = "{\n" +
                "  \n" +
                "    \"first_reported_by_task\": \"\",\n" +
                "    \"parent\": \"\",\n" +
                "    \"urgency\": \"" + urgency + "\",\n" +
                "    \"impact\": \"" + impact + "\",\n" +
                "    \"priority\": \"" + priority + "\",\n" +      //Critical
                "    \"state\": \"" + state + "\",\n" +       // 107 closed
                "    \"assigned_to\": \"" + assigned_to + "\",\n" +
                "    \"opened_at\": \"" + openedAt + "\",\n" +
                "    \"assignment_group\": \"" + assignment_group + "\",\n" +
                "    \"category\":\"" + category + "\",\n" +
                "    \"resolution_code\":\"" + resolution_code + "\",\n" +
                "    \"description\": \"Performance Testing Data.\",\n" +
                "    \"short_description\": \"Performance Testing Data\\n\\t\\t\",\n" +
                "\t\"cause_notes\":\"\\n\\t\\t\\t <p>Corporate desktop apps do not support SP2</p>\",\n" +
                "\t\"fix_notes\": \"\\n\\t\\t\\t <p>Remove Windows XP SP2</p>\",\n" +
                "\t\"related_incidents\": \"1\",\n" +
                "\t\"rfc\": {\n" +
                "      \"link\": \"https://dev68841.service-now.com/api/now/table/change_request/46e9b4afa9fe198101026e122b85f442\",\n" +
                "      \"value\": \"46e9b4afa9fe198101026e122b85f442\"\n" +
                "    }\n" +
                "}\n" +
                "\n";

        return payload;
    }

    private String getCreatePayloadChangeRequest2(String short_description,
                                                  String reason,
                                                  String sys_updated_on,
                                                  String type,
                                                  String follow_up,
                                                  String sys_updated_by,
                                                  String opened_by,
                                                  String urgency,String sys_created_on,
                                                  String sys_domain,String scope,
                                                  String company,String state,
                                                  String justification,String order,
                                                  String phase,String closed_at,
                                                  String approval,String impact,
                                                  String due_date,String activity,
                                                  String priority,String opened_at,
                                                  String requested_by,String escalation,
                                                  String risk,String category
    ){

        String payload = "{\n" +
                "  \n" +
                "    \"reason\": \"" + reason + "\",\n" +
                "    \"short_description\": \"" + short_description + "\",\n" +
                "    \"sys_updated_on\": \"" + sys_updated_on + "\",\n" +
                "    \"type\": \"" + type + "\",\n" +
                "    \"follow_up\": \"" + follow_up + "\",\n" +
                "    \"sys_updated_by\": \"" + sys_updated_by + "\",\n" +      //Critical
                "    \"opened_by\": \"" + opened_by + "\",\n" +       // 107 closed
                "    \"urgency\": \"" + urgency + "\",\n" +
                "    \"sys_created_on\": \"" + sys_created_on + "\",\n" +
                "    \"sys_domain\": \"" + sys_domain + "\",\n" +
                "    \"scope\":\"" + scope + "\",\n" +
                "    \"company\":\"" + company + "\",\n" +
                "    \"state\": \"" + state + "\",\n" +
                "    \"justification\": \"" + justification + "\",\n" +
                "    \"order\": \"" + order + "\",\n" +
                "    \"phase\": \"" + phase + "\",\n" +
                "    \"closed_at\": \"" + closed_at + "\",\n" +
                "    \"approval\": \"" + approval + "\",\n" +
                "    \"impact\": \"" + impact + "\",\n" +
                "    \"due_date\": \"" + due_date + "\",\n" +
                "    \"activity\": \"" + activity + "\",\n" +
                "    \"priority\": \"" + priority + "\",\n" +
                "    \"opened_at\": \"" + opened_at + "\",\n" +
                "    \"requested_by\": \"" + requested_by + "\",\n" +
                "    \"escalation\": \"" + escalation + "\",\n" +
                "    \"risk\": \"" + risk + "\",\n" +
                "    \"category\": \"" + category + "\",\n" +
                "    \"assignment_group\": \"" + "Help Desk" + "\",\n" +

                "\t\"rfc\": {\n" +
                "      \"link\": \"https://dev64639.service-now.com/api/now/table/change_request/46e9b4afa9fe198101026e122b85f442\",\n" +
                "      \"value\": \"46e9b4afa9fe198101026e122b85f442\"\n" +
                "    }\n" +
                "}\n" +
                "\n";

        return payload;
    }


    @Test
    public void TestModifyTickets(){

        //Retrieve records from a table
        String table = "incident";

        String filter1 = "short_description" + "IN" + "My disk is still having issues. Can't delete a file";
        String filter2 = "number" + "IN" + "INC0000028" + "OR" + "INC0000030";
        String filter3 = "priority" + "IN" + "1" + "OR" + "2" + "OR" + "3" + "OR" + "4" + "OR" + "5";
        String filter4 = "resolved_at" + "BETWEEN" + "javascript:gs.dateGenerate(" + "2020-05-01 00:00:00" + ")@javascript:gs.dateGenerate(" + "2020-06-12 23:59:59)";

        String columnsString = "&sysparm_fields=sys_id";

        String url = "/api/now/table/" + table + "?";
        //A list of comma seperated Filters
        String query = filter1 + "," + filter2 + "," + filter3 + "," + filter4 + columnsString;

        String path = query;

        ArrayList<String> incidentIdSystemIds = getRecordsBasedOnFilter(path);

        String modifier1 = "\"resolved_at\":\"2020-05-01 00:00:00\"";

        String modifier2= "approval\":\"ac\"";                              //Example
//        String modifyPayload = "{" + modifier1+"," + modifier2 + "}";     //Example

        String modifyPayload = "{" + modifier1+ "}";

        for(int i=0;i<incidentIdSystemIds.size();i++){

            modifyRecords(table,incidentIdSystemIds.get(i),modifyPayload);
        }



    }


    public ArrayList<String> getRecordsBasedOnFilter(String path) {
        // Read Proxy config from File and control HttpClient
        HttpClient httpClient;

        HttpResponse response = null;
        ArrayList<String> incidentIDList = new ArrayList<>();

        try {
            path = URLEncoder.encode(path, "UTF-8");
            String hostName = env;
            Integer portNumber = 443;
            String protocolScheme = "https";
            String url = "/api/now/table/incident?sysparm_query=";

            String hostUrl = protocolScheme + "://" + hostName + ":" + portNumber + url;
            HttpGet httpGetRequest = new HttpGet(hostUrl + path);

            HttpHost target = new HttpHost(hostName, portNumber, protocolScheme);

            SSLContext sslcontext = SSLContexts.custom().useSSL().build();

            sslcontext.init(null, new X509TrustManager[]{new HttpsTrustManager()}, new SecureRandom());

            SSLConnectionSocketFactory factory = SSLConnectionSocketFactory.getSocketFactory();

            httpClient = HttpClients.custom().setSSLSocketFactory(factory).build();

            httpGetRequest.addHeader("Accept", "application/json");

            httpGetRequest.addHeader("Authorization", authorization);
            httpGetRequest.addHeader("Content-Type", "application/json");
            httpGetRequest.addHeader("Cookie", cookie);

            try {
                response = httpClient.execute(target, httpGetRequest);

                String responseBody = EntityUtils.toString(response.getEntity());

                try {
                    JSONObject responseBodyJSon = new JSONObject(responseBody);

                    JSONArray resultArray = responseBodyJSon.getJSONArray("result");

                    for(int i=0;i<resultArray.length();i++){

                        String incidentId = resultArray.getJSONObject(i).get("sys_id").toString();
                        incidentIDList.add(incidentId);
                    }

                } catch (Exception e) {
                    System.out.println("Exception");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            System.out.println("Exception");
        }
        return incidentIDList;
    }

    public Boolean modifyRecords(String table,String serviceNowId,String payload) {

        Boolean updationStatus = true;

        // Read Proxy config from File and control HttpClient
        HttpClient httpClient;

        HttpResponse response;

        try {

            String hostName = env;
            Integer portNumber = 443;
            String protocolScheme = "https";

            String path = "/api/now/table/" + table + "/" + serviceNowId;

            String hostUrl = protocolScheme + "://" + hostName + ":" + portNumber;
            HttpPut httpPutRequest = new HttpPut(hostUrl + path);

            HttpHost target = new HttpHost(hostName, portNumber, protocolScheme);

            SSLContext sslcontext = SSLContexts.custom().useSSL().build();

            sslcontext.init(null, new X509TrustManager[]{new HttpsTrustManager()}, new SecureRandom());

            SSLConnectionSocketFactory factory = SSLConnectionSocketFactory.getSocketFactory();

            httpClient = HttpClients.custom().setSSLSocketFactory(factory).build();

            httpPutRequest.addHeader("Accept", "application/json");

            httpPutRequest.addHeader("Authorization", authorization);
            httpPutRequest.addHeader("Content-Type", "application/json");
            httpPutRequest.addHeader("Cookie", cookie);

            if (payload != null)
                httpPutRequest.setEntity(new StringEntity(payload, "UTF-8"));
            try {
                response = httpClient.execute(target, httpPutRequest);

                try {
                    if(response.getStatusLine().getStatusCode()!=200){
                        System.out.println("Record Updated unsuccessfully");
                        updationStatus = false;
                    }

                } catch (Exception e) {
                    System.out.println("Exception");
                    updationStatus = false;
                }
            } catch (IOException e) {
                e.printStackTrace();
                updationStatus = false;
            }

        } catch (Exception e) {
            System.out.println("Exception");
        }
        return updationStatus;
    }


}
