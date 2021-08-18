package com.sirionlabs.test.workflowbuttons;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
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

public class TestWorkflowValidations {

    private final static Logger logger = LoggerFactory.getLogger(TestWorkflowValidations.class);
    static String orderByColumnName = "id";
    static String orderDirection = "desc";
    static String configFilePath;
    static String configFileName;

    static List<String> allEntitySection;

    static Integer size = 10;
    static int offset = 0;

    String entitySectionSplitter = ",";
    Boolean testForAllEntities = false;

    static String entityIdMappingFileName;
    static String entityIdConfigFilePath;

    static List<String> wordstoskip;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {

        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("WorkFlowButtonsConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("WorkFlowButtonsConfigFileName");

        entityIdConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityIdConfigFilePath");
        entityIdMappingFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile");

        // for getting all section
        if (!ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "testforallentities").trim().equalsIgnoreCase(""))
            testForAllEntities = Boolean.parseBoolean(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "testforallentities"));

        if (!testForAllEntities) {
            allEntitySection = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "entitytotest").split(entitySectionSplitter));
        } else {
            allEntitySection = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "allentitytotest").split(entitySectionSplitter));
        }
        // getting all section Ends Here


//        if (!ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
//                "maxrandomoptions").trim().equalsIgnoreCase(""))
//            maxRandomOptions = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
//                    "maxrandomoptions").trim());

        if (!ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "size").trim().equalsIgnoreCase(""))
            size = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "size"));

        if (!ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "offset").trim().equalsIgnoreCase(""))
            offset = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "offset"));

        if (!ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "orderbycolumnname").trim().equalsIgnoreCase(""))
            orderByColumnName = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "orderByColumnName");

        if (!ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName,
                "orderdirection").trim().equalsIgnoreCase(""))
            orderDirection = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "orderDirection");
    }
    @DataProvider(name = "getAllEntitySection", parallel = true)
    public Object[][] getAllEntitySection() throws ConfigurationException {

        int i = 0;
        Object[][] groupArray = new Object[allEntitySection.size()][];

        for (String entitySection : allEntitySection) {
            groupArray[i] = new Object[3];
            //Integer entitySectionTypeId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(listRendererConfigFilePath, listRendererConfigFileName, entitySection, "entity_type_id"));
            Integer entitySectionTypeId = ConfigureConstantFields.getEntityIdByName(entitySection);
            Integer entitySectionListId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(entityIdConfigFilePath, entityIdMappingFileName, entitySection, "entity_url_id"));

            groupArray[i][0] = entitySection; // EntityName
            groupArray[i][1] = entitySectionTypeId; // EntityTypeId
            groupArray[i][2] = entitySectionListId; // EntityURlId
            i++;
        }

        return groupArray;
    }

    @Test(dataProvider = "getAllEntitySection")
    public void TestWorkflowButtons(String entityName,Integer entityTypeId, Integer listId){

        String listRendererJsonStr;
        String showpagejsonresponse;
        CustomAssert csAssert = new CustomAssert();
        ListRendererListData listObj = new ListRendererListData();
        List<Integer> showpageids = new ArrayList<>();
        Show show = new Show();
        try{
            wordstoskip = Arrays.asList(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"wordstoskip","workflowbuttons").split(","));
        }catch (Exception e){
            logger.error("Exception while getting words to skip ");
            csAssert.assertTrue(false,"Exception while getting words to skip");
        }
        try {
            listObj.hitListRendererListData(entityTypeId, offset, size, orderByColumnName, orderDirection, listId, "");
            listRendererJsonStr = listObj.getListDataJsonStr();

            int listdatasize = getlistdataSize(listRendererJsonStr);

            if(listdatasize > 2000){
                listdatasize = 2000;
            }
            //API gives HTML response after 2000 size
            listObj.hitListRendererListData(entityTypeId, offset, listdatasize, orderByColumnName, orderDirection, listId, "");

            listRendererJsonStr = listObj.getListDataJsonStr();

        } catch (Exception e){
            logger.error("Exception while hitting list data API");
            csAssert.assertTrue(false,"Exception while hitting list data API");
            return;
        }
        try {
            showpageids = getshowpageIds(listRendererJsonStr);
        } catch (Exception e){
            logger.error("Exception while getting show page ids for entity name " + entityName);
            csAssert.assertTrue(false,"Exception while getting show page ids for entity name " + entityName);
        }
        Boolean workflowvalidationstatus;
        Boolean breadcrumvalidationstatus;
        int count = 0;
        for(Integer showpageid : showpageids){

            if(count > 100){
                break;
            }
            count++;

            show.hitShow(entityTypeId,showpageid);
            showpagejsonresponse = show.getShowJsonStr();
//            workflowvalidationstatus = workflowbuttonvalidaterussian(showpageid,showpagejsonresponse,csAssert,entityName);
//
//            if(workflowvalidationstatus == false){
//                logger.error("Russian Language validation failed for show page id " + showpageid);
//                csAssert.assertTrue(false,"Russian Language validation failed for show page id " + showpageid);
//            }

            breadcrumvalidationstatus = breadcrumvalidaterussian(showpageid,showpagejsonresponse,csAssert,entityName);

            if(breadcrumvalidationstatus == false){
                logger.error("Russian Language validation failed for show page id " + showpageid);
                csAssert.assertTrue(false,"Russian Language validation failed for show page id " + showpageid);
            }
        }
        csAssert.assertAll();
    }

    private int getlistdataSize(String listdataresponse){

        JSONObject listdatajson = new JSONObject(listdataresponse);
        int size = Integer.parseInt(listdatajson.get("filteredCount").toString());
        return size;
    }

    private List<Integer> getshowpageIds(String listdataresponse){

        List<Integer> showpageIds = new ArrayList<>();
        JSONObject indvdataobj;
        JSONArray indvdataarray;
        JSONObject columnjson;
        Integer showpageId;
        JSONArray dataarray;
        try {
             dataarray = new JSONObject(listdataresponse).getJSONArray("data");

            for (int i = 0; i < dataarray.length(); i++) {
                indvdataobj = dataarray.getJSONObject(i);
                indvdataarray = JSONUtility.convertJsonOnjectToJsonArray(indvdataobj);
                for (int j = 0; j < indvdataarray.length(); j++) {
                    columnjson = indvdataarray.getJSONObject(j);

                    if (columnjson.get("columnName").toString().equals("id")) {
                        showpageId = Integer.parseInt(columnjson.get("value").toString().split(":;")[1]);
                        showpageIds.add(showpageId);
                    }
                }
            }
        }
        catch (Exception e){
         logger.error("Exception while retrieving showpage ids from list data response " + e.getMessage());
        }
        return showpageIds;
    }

    public boolean workflowbuttonvalidaterussian(Integer showpageid, String showresponse, CustomAssert csAssert, String entityname){

        logger.info("Inside workflowbuttonvalidaterussian method");

        String actualbuttonname;
        Boolean russianvalidationstaus = true;
        try {
            JSONArray actionarray = new JSONObject(showresponse).getJSONObject("body").getJSONObject("layoutInfo").getJSONArray("actions");

            for (int i = 0; i < actionarray.length() - 1; i++) {

                try {
                    actualbuttonname = actionarray.getJSONObject(i).get("label").toString();

                } catch (Exception e) {
                    try {
                        logger.info("name parameter not present in the action array");
                        actualbuttonname = actionarray.getJSONObject(i).get("name").toString();

                    } catch (Exception e1) {
                        logger.error("name and label parameter not present in the action array json ");
                        csAssert.assertTrue(false, "name and label parameter not present in the action array json ");
                        continue;
                    }

                }
                logger.info("Validating workflow button for entity name " + entityname + " show page id "+ showpageid + "and workflow button " + actualbuttonname);
                russianvalidationstaus = validaterussiancharacters(actualbuttonname,entityname,csAssert);

                if(russianvalidationstaus == true){
                    logger.info(actualbuttonname + " contains all russian characters ");
                }else {
                    logger.error(actualbuttonname + " contains non russian characters ");
                }
            }
        }catch (Exception e){
            logger.error("Exception while validating show page response for russian characters " + showpageid + "entity name " + entityname);
            russianvalidationstaus = false;
        }
        return russianvalidationstaus;
    }

    public boolean breadcrumvalidaterussian(Integer showpageid,String showresponse,CustomAssert csAssert,String entityname){

        logger.info("Inside breadcrumvalidaterussian method");

        String breadcrumblabel;
        Boolean russianvalidationstaus = true;
        try {
            JSONArray historyarray = new JSONObject(showresponse).getJSONObject("body").getJSONObject("data").getJSONObject("history").getJSONArray("status");

            for (int i = 0; i < historyarray.length() - 1; i++) {

                try {
                    breadcrumblabel = historyarray.getJSONObject(i).get("label").toString();

                } catch (Exception e) {
                    try {
                        logger.info("name parameter not present in the action array");
                        breadcrumblabel = historyarray.getJSONObject(i).get("label").toString();

                    } catch (Exception e1) {
                        logger.error("name and label parameter not present in the action array json ");
                        csAssert.assertTrue(false, "name and label parameter not present in the action array json ");
                        continue;
                    }

                }
                logger.info("Validating workflow button for entity name " + entityname + " show page id "+ showpageid + "and workflow button " + breadcrumblabel);
                russianvalidationstaus = validaterussiancharacters(breadcrumblabel,entityname,csAssert);

                if(russianvalidationstaus == true){
                    logger.info(breadcrumblabel + " contains all russian characters ");
                }else {
                    logger.error(breadcrumblabel + " contains non russian characters ");
                }
            }
        }catch (Exception e){
            logger.error("Exception while validating show page response for russian characters " + showpageid + "entity name " + entityname);
            russianvalidationstaus = false;
        }
        return russianvalidationstaus;
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
