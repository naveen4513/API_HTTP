package com.sirionlabs.test.xyz;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.GovernanceBody;
import com.sirionlabs.helper.entityWorkflowAction.EntityWorkflowActionHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.StringUtils;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONObject;
import org.junit.After;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class xyz {
    private static String configFilePath = null;
    private static String configFileName = null;
    List<String> sections = null;
    private static int governanceBodyID;


    private final static org.slf4j.Logger Logger = LoggerFactory.getLogger(xyz.class);
    @BeforeSuite
    public void beforeclass() throws ConfigurationException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("StatusToIncludeFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("StatusToIncludeFileName");
         sections = ParseConfigFile.getAllSectionNames(configFilePath, configFileName);

    }


    @Test(dataProvider = "getSections")
    public void getStatus(String section, int entityId, String status) throws IOException {
        String[] str = status.split(",");
        List<String> statuses = new ArrayList<String>();
        for(int i=0;i<str.length;i++)
        {
            statuses.add(str[i]);
        }
        String response = null;
        response = GovernanceBody.createGB();
         governanceBodyID = CreateEntity.getNewEntityId(response);
        Logger.info("Governance Body Created with Entity id: " + governanceBodyID);

        HttpGet request;
        String query = "/governancebody/edit/"+governanceBodyID;

        request = new HttpGet(query);
        HttpResponse getresponse = APIUtils.getRequest(request);
        JSONObject json = new JSONObject(getresponse.getEntity());
        System.out.println(json);

        String payload = json.toString();


    /*    Map<String, String> properties = ParseConfigFile.getAllConstantProperties(configFilePath,configFileName,"xyz");
        String entityName = properties.get("entity");
        int recordId = Integer.parseInt(properties.get("entityId"));
        String expectedResult = properties.get("expectedresult");*/



        EntityWorkflowActionHelper Ehelper = new EntityWorkflowActionHelper();
        Ehelper.hitWorkflowAction("GB", entityId, governanceBodyID, "Send For Internal Review");

        String editResponse = null;
       /* String payload = createPayload(governanceBodyId ,occurrenceDate ,startTime , timeZone , duration, location );
        editResponse = new Edit().hitEdit("GB",payload);*/



        if(statuses.contains("Send For Internal Review"))
        {

        }
        Ehelper.hitWorkflowAction("GB", entityId, governanceBodyID, "Internal Review Complete");
        Ehelper.hitWorkflowAction("GB", entityId, governanceBodyID, "Send For Client Review");
        Ehelper.hitWorkflowAction("GB", entityId, governanceBodyID, "Approve");
        Ehelper.hitWorkflowAction("GB", entityId, governanceBodyID, "Publish");



    }
    private String createPayload(String governanceBodyId , String occurrenceDate , String startTime , String timeZone , String duration, String location ){
        HashMap<String, String> value = new HashMap<>();
        value.put("governanceBodyId",governanceBodyId);
        value.put("occurrenceDate",occurrenceDate);
        value.put("startTime",startTime);
        value.put("timeZone",timeZone);
        value.put("duration",duration);
        value.put("location",location);
        String payload = "{\"governanceBody\":{\"id\":${governanceBodyId}},\"occurrenceDate\":\"${occurrenceDate} 00:00:00\",\"startTime\":{\"id\":43,\"name\":\"${startTime}\"},\"timeZone\":{\"id\":8,\"name\":\"${timeZone}\"},\"duration\":{\"id\":1,\"name\":\"${duration}\"},\"location\":\"${location}\"}";
        payload = StringUtils.strSubstitutor(payload,value);
        if(JSONUtility.validjson(payload)){
            return payload;
        }else{
            return  null;
        }
    }
    @DataProvider(name = "getSections" )
    public Object[][] getSections() throws IOException

    {

        Object result[][] = new Object[sections.size()][3];
        for (int i=0 ; i< sections.size();i++)
        {
            result[i][0]  = sections.get(i);
            result[i][1] = ConfigureConstantFields.getEntityIdByName(sections.get(i));
            result[i][2] = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,sections.get(i),"status");
 }
    return result;
    }


@After
public  void after(){


    ShowHelper.deleteEntity("governance body" , 86,governanceBodyID );

}








}
