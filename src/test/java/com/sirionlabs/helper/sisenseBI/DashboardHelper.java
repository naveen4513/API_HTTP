package com.sirionlabs.helper.sisenseBI;

import com.sirionlabs.api.sisenseBI.Dashboard;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DashboardHelper {

    private final static Logger logger = LoggerFactory.getLogger(DashboardHelper.class);

    public JSONArray getDashboardMetaData(String dashboardId,String title,String authToken){
        String dashboardResponse;
        JSONArray metadata = new JSONArray();
        Dashboard dashboard = new Dashboard();

        try {

            dashboard.hitDashboard(dashboardId, authToken);
            dashboardResponse = dashboard.getDashboardResponse();
            try {
                JSONObject dashboardJson = new JSONObject(dashboardResponse);
                JSONObject indvitemJson;
                JSONArray widgetsJsonArray = dashboardJson.getJSONArray("widgets");
                JSONArray panelsJsonArray;
                JSONArray itemsJsonArray;

                String actualTitle;
                for(int i = 0;i<widgetsJsonArray.length();i++){

                    actualTitle = widgetsJsonArray.getJSONObject(i).get("title").toString();
                    if(!(actualTitle.equals(title))){
                        continue;
                    }

                    panelsJsonArray = widgetsJsonArray.getJSONObject(i).getJSONObject("metadata").getJSONArray("panels");
                    for(int j = 0;j<panelsJsonArray.length();j++){
                        itemsJsonArray = panelsJsonArray.getJSONObject(j).getJSONArray("items");

                        for(int k = 0;k<itemsJsonArray.length();k++){
                            indvitemJson = itemsJsonArray.getJSONObject(k);
                            metadata.put(indvitemJson);
                        }
                    }break;
                }

            }catch (Exception e){
                logger.error("Exception while parsing Dashboard Response");
            }
        }catch (Exception e){
            logger.error("Exception while hitting Sisense dashboard API");

        }
        return metadata;
    }

}
