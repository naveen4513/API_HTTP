package com.sirionlabs.test.governanceBody;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.GovernanceBody;
import com.sirionlabs.helper.entityEdit.EntityEditHelper;
import com.sirionlabs.helper.entityWorkflowAction.EntityWorkflowActionHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Listeners(value = MyTestListenerAdapter.class)
public class TestGBSchedularTask {
    private final static Logger logger = LoggerFactory.getLogger(TestGBSchedularTask.class);
    private int gbEntityId;
    private int gbEntityTypeID;
    private int cgbEntityTypeId;
    private String meetingTabID;
    private Show show;
    private EntityEditHelper editHelperObj;
    private  String editConfigFilePath;
    private String editConfigFileName;
    Map<String, String> editmap;
    private Map<String, String> defaultProperties;

    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        editConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("EntityEditConfigFilePath");
        editConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("EntityEditConfigFileName");
        String configFilePath = ConfigureConstantFields.getConstantFieldsProperty("GBFilePath");
        String extraFieldsConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("GBExtraFieldsFileName");
        editmap = ParseConfigFile.getAllConstantProperties(configFilePath, extraFieldsConfigFileName, "gb_misc_single_sup_edit");
        defaultProperties = ParseConfigFile.getAllDefaultProperties(editConfigFilePath, editConfigFileName);
        gbEntityTypeID = ConfigureConstantFields.getEntityIdByName("governance body");
        cgbEntityTypeId = ConfigureConstantFields.getEntityIdByName("governance body meetings");
        meetingTabID = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFilePath"), ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFileName"), "tabs mapping", "meetings");
        show = new Show();
        editHelperObj = new EntityEditHelper();
    }



    @DataProvider()
    public Object[][] dataProviderForGBSchedularTask() {
        List<Object[]> allTestData = new ArrayList<>();

        String[] flows = {"gb_misc_single_sup","gb_misc_multi_sup"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }


    @Test(dataProvider = "dataProviderForGBSchedularTask")
    public void TestGBTabs(String flow) throws InterruptedException {

        CustomAssert csassert = new CustomAssert();
        gbEntityId = createGB(csassert,flow);

        if (!(gbEntityId==-1))
        {
            logger.info("Governance Body Created with Entity id: " + gbEntityId);
            //performing workflow action on Gb

            logger.info("Perform Entity Workflow Action For Created Gb");
            EntityWorkflowActionHelper entityWorkflowActionHelper = new EntityWorkflowActionHelper();
            String[] workFlowStep = new String[]{"Send For Internal Review", "Internal Review Complete", "Send For Client Review", "Approve", "Publish"};
            for (String actionLabel : workFlowStep) {
                logger.info(actionLabel);
                entityWorkflowActionHelper.hitWorkflowAction("GB", gbEntityTypeID, gbEntityId, actionLabel);
            }

            Thread.sleep(60000);
            List<String> cgbEntityIds = validateMeetingCreation(gbEntityId);

            //validating T479726,T478574

            /*csassert.assertEquals( getCGBStatus(cgbEntityIds.get(0)),"Overdue",
                    cgbEntityIds.get(0) + "CGB status should be Overdue for testcase T479726,T478574");*/
            csassert.assertEquals( getCGBStatus(cgbEntityIds.get(1)),"Upcoming Meeting",
                    cgbEntityIds.get(1) + "CGB status should be Upcoming Meeting testcase T479726,T478574");
            csassert.assertEquals( getCGBStatus(cgbEntityIds.get(2)),"Upcoming Meeting",
                    cgbEntityIds.get(2) + "CGB status should be Upcoming Meeting testcase T479726,T478574");


            //validating T479727,T478576


            for (int i = 0; i <cgbEntityIds.size() ; i++) {
                EntityOperationsHelper.deleteEntityRecord("governance body meetings", Integer.valueOf(cgbEntityIds.get(i)));

            }

            EntityOperationsHelper.deleteEntityRecord("governance body", gbEntityId);


        }else {
            csassert.assertTrue(false,"GB is not Created");
        }




        csassert.assertAll();
    }



    private int createGB(CustomAssert customAssert, String flow) {
        logger.info("***********************************creating GB*************************************");
        try {
            String section = flow;
            boolean isLocal = true;
            String gbResponse = GovernanceBody.createGB(section, isLocal);
            if (ParseJsonResponse.validJsonResponse(gbResponse)) {
                int gbEntityId = CreateEntity.getNewEntityId(gbResponse);
                logger.info("Gb successfully created with ID ->" + gbEntityId);
                return gbEntityId;
            }
        } catch (Exception e) {
            logger.error("GB is not creating");
            customAssert.assertTrue(false, "Exception while GB is creating");
        }
    return  -1;
    }


    private  List<String> validateMeetingCreation(int gbEntityId){
        TabListData listData = new TabListData();
        String gb_res = listData.hitTabListData(Integer.valueOf(meetingTabID), gbEntityTypeID, gbEntityId);
        List<String> meetingIds = ListDataHelper.getColumnIds(gb_res);
        if(meetingIds.size()==0){
            return null;
        }else{
            logger.info("meeting created is :  " + meetingIds);
            return meetingIds;
        }
    }


    private String getCGBStatus(String cdgId){
        String status = "";
        show.hitShowGetAPI(cgbEntityTypeId,Integer.parseInt(cdgId));
        String cgbShow = show.getShowJsonStr();
        status = (String) JSONUtility.parseJson(cgbShow,"$.body.data.status.values.name");
        return status;
    }

}
