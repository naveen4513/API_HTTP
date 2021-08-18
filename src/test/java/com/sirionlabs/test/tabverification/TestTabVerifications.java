package com.sirionlabs.test.tabverification;

import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class TestTabVerifications {

    private final static Logger logger = LoggerFactory.getLogger(TestTabVerifications.class);
    private static String configFilePath;
    private static String configFileName;
    private static String entitytotest;
    private static String[] entitytotestarray;
    private static List<String> entitytotestlist;
    private static List<String> wordstoskip = new ArrayList<>();
    static String entityIdMappingFileName;
    static String entityIdConfigFilePath;
    static Integer size = 10;
    static int offset = 0;
    static String orderByColumnName = "id";
    static String orderDirection = "desc";

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("TabListVerficationConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("TabListVerficationConfigFileName");

        entitytotestarray = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"entitytotest").split(",");

        entitytotestlist = Arrays.asList(entitytotestarray);

        entityIdConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath");
        entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");
    }

    @DataProvider(name = "getAllEntitySection", parallel = true)
    public Object[][] getAllEntitySection() throws ConfigurationException {

        int i = 0;
        Object[][] groupArray = new Object[entitytotestlist.size()][];

        for(String entitytotest : entitytotestarray){

            groupArray[i] = new Object[3];
            Integer entitySectionTypeId = ConfigureConstantFields.getEntityIdByName(entitytotest);
            Integer entitySectionListId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdConfigFilePath, entityIdMappingFileName, entitytotest, "entity_url_id"));

            groupArray[i][0] = entitytotest; // EntityName
            groupArray[i][1] = entitySectionTypeId; // EntityTypeId
            groupArray[i][2] = entitySectionListId; // EntityURlId

            i++;
        }

        return groupArray;
    }

    @Test(dataProvider = "getAllEntitySection")
    public void validateTab(String entityname,Integer entitytypeid,Integer listid){

        ListRendererListData listObj = new ListRendererListData();
        String listRendererJsonStr;
        CustomAssert csAssert = new CustomAssert();
        try {
            listObj.hitListRendererListData(entitytypeid, offset, size, orderByColumnName, orderDirection, listid, "");
            listRendererJsonStr = listObj.getListDataJsonStr();

            int listdatasize = getlistdataSize(listRendererJsonStr);

            if(listdatasize > 200){
                listdatasize = 200;
            }
            //API gives HTML response after 2000 size
            listObj.hitListRendererListData(entitytypeid, offset, listdatasize, orderByColumnName, orderDirection, listid, "");
            listRendererJsonStr = listObj.getListDataJsonStr();

            List<Integer> showpageids = getShowPageIds(listRendererJsonStr);
            TabListData tabListData = new TabListData();
            String tablistresponse;
            JSONObject tablistjson;
            JSONArray tablistdataarray;
            JSONObject relatedrequestjson;
            JSONArray relatedrequestjsonarray;
            JSONObject relatedrequestindvdatajson;
            String status;
            Boolean validaterussianstaus;
            for(Integer showpageid : showpageids){

                logger.info("Validating Related Request Tab for show page id :" + showpageid);

                tablistresponse = tabListData.hitTabListData(325,entitytypeid,showpageid);

                tablistdataarray = new JSONObject(tablistresponse).getJSONArray("data");

                for(int i = 0;i<tablistdataarray.length();i++){
                    relatedrequestjson = tablistdataarray.getJSONObject(i);
                    relatedrequestjsonarray = JSONUtility.convertJsonOnjectToJsonArray(relatedrequestjson);
                    innerloop:
                    for(int j = 0;j<relatedrequestjsonarray.length();j++){

                        relatedrequestindvdatajson = relatedrequestjsonarray.getJSONObject(j);
                        if(relatedrequestindvdatajson.get("columnName").toString().equals("status")){
                            status = relatedrequestindvdatajson.get("value").toString();
                            validaterussianstaus = validaterussiancharacters(status,entityname,csAssert);
                            if(validaterussianstaus == false){
                                logger.error("");
                            }
                            break innerloop;
                        }
                    }
                }
            }

        } catch (Exception e){
            logger.error("Exception while hitting list data API");
            csAssert.assertTrue(false,"Exception while hitting list data API");
            return;
        }

    }

    private List<Integer> getShowPageIds(String listdataresponse){

        List<Integer> showpageids = new ArrayList<>();
        JSONArray dataarray = new JSONObject(listdataresponse).getJSONArray("data");
        JSONObject indvdatajson;
        JSONArray indvdataarray;
        JSONObject indvdata;
        Integer showpageid;
        for(int i = 0;i<dataarray.length();i++){
            indvdatajson = dataarray.getJSONObject(i);
            indvdataarray = JSONUtility.convertJsonOnjectToJsonArray(indvdatajson);
            Innerloop:
            for(int j = 0;j<indvdataarray.length();j++){
                indvdata = indvdataarray.getJSONObject(j);
                if(indvdata.get("columnName").toString().equals("id")){
                    showpageid = Integer.parseInt(indvdata.get("value").toString().split(":;")[1]);
                    showpageids.add(showpageid);
                    break Innerloop;
                }
            }
        }
        return showpageids;
    }
    private int getlistdataSize(String listdataresponse){

        JSONObject listdatajson = new JSONObject(listdataresponse);
        int size = Integer.parseInt(listdatajson.get("filteredCount").toString());
        return size;
    }

    private boolean validaterussiancharacters(String englishstring,String entityName,CustomAssert csAssert){

        if(wordstoskip.contains(englishstring)){
            logger.debug("Skipping the word " + englishstring);
            return true;
        }

        logger.info("Validating the string : " + englishstring);
        String specialChars = "-/*!@#$%^&*()\"{}_[]|\\?/<>,. ";

        Boolean flag = true;
        List<Character> nonRussianChar = new ArrayList<>();
        String nonRussianCharacter;
        List<String> nonRussianCharacters = new ArrayList<>();
        innerloop:
        for (int i = 0; i < englishstring.trim().length(); i++) {
            if (!Character.UnicodeBlock.of(englishstring.charAt(i)).equals(Character.UnicodeBlock.CYRILLIC)) {

                nonRussianChar.add(englishstring.charAt(i));

                //for (int j = 0; i < s.length(); j++) {
                if (specialChars.contains(englishstring.substring(i, i + 1))) {

                    logger.info("Special character present in the string field");
                    //flag = false;
                } else {
                    //logger.error("Russian or Special character not present in the string field " + englishstring.substring(i, i + 1));
                    nonRussianCharacter = englishstring.substring(i, i + 1);
                    nonRussianCharacters.add(nonRussianCharacter);
                    flag = false;
                    //break innerloop;
                }
            }
        }
        if (!flag) {
            logger.error("{} category : {} contains non-russian characters : {}", entityName, englishstring, nonRussianCharacters);
            csAssert.assertTrue(false, entityName + " category : " + englishstring + ", contains non-russian characters : " + nonRussianChar);
        } else
            logger.info("{} String : {} contains all russian characters.", entityName, englishstring);

        return flag;
    }
}
