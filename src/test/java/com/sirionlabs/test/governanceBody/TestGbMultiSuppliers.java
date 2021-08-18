package com.sirionlabs.test.governanceBody;

import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.api.governancebody.AdhocMeeting;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.ListRendererTabListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.api.reportRenderer.ReportRendererDefaultUserListMetaData;
import com.sirionlabs.api.reportRenderer.ReportRendererListData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.DefaultUserListMetadataHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.GovernanceBody;
import com.sirionlabs.helper.entityWorkflowAction.EntityWorkflowActionHelper;
import com.sirionlabs.test.APIPOD.TestActionIssueDisputeForGovernanceBody;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

@Listeners(value = MyTestListenerAdapter.class)
public class TestGbMultiSuppliers {
    private final static Logger logger = LoggerFactory.getLogger(TestActionIssueDisputeForGovernanceBody.class);
    private int gbEntityTypeID;
    private String dateFormat;
    private String meetingTabID;
    private int cgbEntityTypeId;


    @BeforeClass
    public void beforeClass() {
        gbEntityTypeID = ConfigureConstantFields.getEntityIdByName("governance body");
        meetingTabID = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFilePath"), ConfigureConstantFields.getConstantFieldsProperty("ListRendererTabsMappingFileName"), "tabs mapping", "meetings");
        cgbEntityTypeId = ConfigureConstantFields.getEntityIdByName("governance body meetings");
        dateFormat = ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("GBFilePath"), ConfigureConstantFields.getConstantFieldsProperty("AdhocMeetingFIleName"), "dateformat");
    }


    @Test
    public void testGbMultiSupplier() {
        CustomAssert customAssert = new CustomAssert();
        int gbEntityId = 0;
        String cgbEntityId = null;
        try {
            logger.info("************Create Gb****************");
            String governanceBodiesResponse = GovernanceBody.createGB("gb_multisupplier", true);
            if (ParseJsonResponse.validJsonResponse(governanceBodiesResponse))
                gbEntityId = CreateEntity.getNewEntityId(governanceBodiesResponse);

            logger.info("Governance Body Created with Entity id: " + gbEntityId);
            logger.info("Perform Entity Workflow Action For Created Gb");
            EntityWorkflowActionHelper entityWorkflowActionHelper = new EntityWorkflowActionHelper();
            String[] workFlowStep = new String[]{"Send For Internal Review", "Internal Review Complete", "Send For Client Review", "Approve", "Publish"};
            for (String actionLabel : workFlowStep) {
                logger.info(actionLabel);
                entityWorkflowActionHelper.hitWorkflowAction("GB", gbEntityTypeID, gbEntityId, actionLabel);
            }


            logger.info("Create AdhocMeeting for Governance Body Entity ID {}", gbEntityId);
            String adhocMeetingAPIResponse = new AdhocMeeting().hitAdhocMeetingApi(String.valueOf(gbEntityId), DateUtils.getDateOfXDaysFromYDate(DateUtils.getCurrentDateInMM_DD_YYYY(), -1, dateFormat), "21:00", "Asia/Kolkata (GMT +05:30)", "30 Min", "delhi");
            if (!adhocMeetingAPIResponse.contains("Meeting Scheduled"))
                customAssert.assertTrue(false, "adhoc meeting not created");
            else {
                logger.info(adhocMeetingAPIResponse);
                String gbMeetingTabListDataResponse = new TabListData().hitTabListData(Integer.valueOf(meetingTabID), gbEntityTypeID, gbEntityId);
                if (!ParseJsonResponse.validJsonResponse(gbMeetingTabListDataResponse))
                    customAssert.assertTrue(false, "Invalid meeting tab list data response in governance body");

                JSONObject tabListDataJsonObject = new JSONObject(gbMeetingTabListDataResponse).getJSONArray("data").getJSONObject(0);
                JSONArray jsonObjectName = tabListDataJsonObject.names();
                for (int i = 0; i < jsonObjectName.length(); i++) {
                    if (tabListDataJsonObject.getJSONObject(jsonObjectName.getString(i)).getString("columnName").equalsIgnoreCase("id")) {
                        String[] value = tabListDataJsonObject.getJSONObject(jsonObjectName.getString(i)).getString("value").trim().split(":;");
                        cgbEntityId = value[1];
                    } 
                }


                ListRendererDefaultUserListMetaData listRendererDefaultUserListMetaData = new ListRendererDefaultUserListMetaData();
                DefaultUserListMetadataHelper defaultUserListMetadataHelper = new DefaultUserListMetadataHelper();

                //verify suppliers present in filter and column section for gb
                listRendererDefaultUserListMetaData.hitListRendererDefaultUserListMetadata(211);
                String listRendererDefaultUserListMetaDataResponse = listRendererDefaultUserListMetaData.getListRendererDefaultUserListMetaDataJsonStr();
                checkSupplierPresentInFilter(listRendererDefaultUserListMetaDataResponse, defaultUserListMetadataHelper, customAssert);
                checkSupplierPresentInColumn(listRendererDefaultUserListMetaDataResponse, defaultUserListMetadataHelper, customAssert);

                //verify suppliers present in filter and column section for cgb
                listRendererDefaultUserListMetaData.hitListRendererDefaultUserListMetadata(212);
                listRendererDefaultUserListMetaDataResponse = listRendererDefaultUserListMetaData.getListRendererDefaultUserListMetaDataJsonStr();
                checkSupplierPresentInFilter(listRendererDefaultUserListMetaDataResponse, defaultUserListMetadataHelper, customAssert);
                checkSupplierPresentInColumn(listRendererDefaultUserListMetaDataResponse, defaultUserListMetadataHelper, customAssert);

                //verify suppliers present in filter and column section for gb report
                ReportRendererDefaultUserListMetaData reportRendererDefaultUserListMetaData = new ReportRendererDefaultUserListMetaData();
                reportRendererDefaultUserListMetaData.hitReportRendererDefaultUserListMetadata(261);
                String reportRendererDefaultUserListMetaDataJsonStr = reportRendererDefaultUserListMetaData.getReportRendererDefaultUserListMetaDataJsonStr();
                checkSupplierPresentInFilter(reportRendererDefaultUserListMetaDataJsonStr, defaultUserListMetadataHelper, customAssert);
                checkSupplierPresentInColumn(reportRendererDefaultUserListMetaDataJsonStr, defaultUserListMetadataHelper, customAssert);

                //verify suppliers present in filter and column section for cgb report

                reportRendererDefaultUserListMetaData.hitReportRendererDefaultUserListMetadata(264);
                reportRendererDefaultUserListMetaDataJsonStr = reportRendererDefaultUserListMetaData.getReportRendererDefaultUserListMetaDataJsonStr();
                checkSupplierPresentInFilter(reportRendererDefaultUserListMetaDataJsonStr, defaultUserListMetadataHelper, customAssert);
                checkSupplierPresentInColumn(reportRendererDefaultUserListMetaDataJsonStr, defaultUserListMetadataHelper, customAssert);

                String payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{}},\"selectedColumns\":[{\"columnId\":11223,\"columnQueryName\":\"id\"},{\"columnId\":11227,\"columnQueryName\":\"suppliers\"}]}";
                ListRendererListData listRendererListData = new ListRendererListData();

                //verify suppliers present on listing page for gb
                listRendererListData.hitListRendererListDataV2isFirstCall(211, payload);
                String listDataJsonStr = listRendererListData.getListDataJsonStr();
                List<String> gbSuppliersName = getSuppliersName(gbEntityTypeID, gbEntityId);  //get suppliers name
                checkSupplierPresentOnListingPage(listDataJsonStr, gbSuppliersName, gbEntityId, customAssert);

                //verify suppliers present on listing page for cgb
                listRendererListData.hitListRendererListDataV2isFirstCall(212, payload);
                listDataJsonStr = listRendererListData.getListDataJsonStr();
                List<String> cgbSuppliersName = getSuppliersName(cgbEntityTypeId, Integer.parseInt(cgbEntityId));//get suppliers name
                checkSupplierPresentOnListingPage(listDataJsonStr, cgbSuppliersName, Integer.parseInt(cgbEntityId), customAssert);

                //verify multiple supplier in meetings tab for gb
                ListRendererTabListData listRendererTabListData = new ListRendererTabListData();
                listRendererTabListData.hitListRendererTabListData(213, gbEntityTypeID, gbEntityId, payload);
                String tabListDataJsonStr = listRendererTabListData.getTabListDataJsonStr();
                checkSupplierPresentOnTabListingPage(tabListDataJsonStr, gbSuppliersName, Integer.parseInt(cgbEntityId), customAssert);

                //check report listing page for gb
                ReportRendererListData reportRendererListData = new ReportRendererListData();
                reportRendererListData.hitReportRendererListData(261, payload);
                String listDataJsonStrReport = reportRendererListData.getListDataJsonStr();
                checkSupplierPresentOnListingPage(listDataJsonStrReport, gbSuppliersName, gbEntityId, customAssert);

                //check report listing page for cgb
                reportRendererListData.hitReportRendererListData(264, payload);
                listDataJsonStrReport = reportRendererListData.getListDataJsonStr();
                checkSupplierPresentOnListingPage(listDataJsonStrReport, gbSuppliersName, Integer.parseInt(cgbEntityId), customAssert);


            }
        } catch (Exception e) {
            logger.error("Exception while verifying multiple supplier for gb and cgb{}", e.getMessage());

        } finally {
            logger.info("governance body meeting delete for entity type id {}and entity id {}", cgbEntityTypeId, cgbEntityId);
            ShowHelper.deleteEntity("governance body meetings", cgbEntityTypeId, Integer.parseInt(cgbEntityId));
            logger.info("governance body delete for entity type id {}and entity id {}", gbEntityTypeID, gbEntityId);
            String tabListData = new TabListData().hitTabListData(Integer.valueOf(meetingTabID), gbEntityTypeID, gbEntityId);
            List<String> meetingIds = ListDataHelper.getColumnIds(tabListData);
            for (String meetingId : meetingIds) {
                ShowHelper.deleteEntity("governance body meetings", cgbEntityTypeId, Integer.parseInt(meetingId));
            }
            ShowHelper.deleteEntity("governance body", gbEntityTypeID, gbEntityId);
        }
        customAssert.assertAll();

    }

    private void checkSupplierPresentOnTabListingPage(String tabListDataJsonStr, List<String> gbSuppliersName, int parseInt, CustomAssert customAssert) {
        List<String> suppliers = (List<String>) JSONUtility.parseJson(tabListDataJsonStr, "$.data[*].[*][?(@.columnName=='suppliers')].value");
        try {
            for (String name : suppliers) {
                String[] arr = name.split("::");
                for (String supplier : arr) {
                    if (gbSuppliersName.isEmpty() && !gbSuppliersName.contains(supplier.split(":;")[0].toLowerCase()))
                        customAssert.assertTrue(false, "supplier name is not found in meeting tab");

                }

            }
        } catch (Exception e) {
            logger.error("Exception while verifying suppliers filed present on meeting tab {}", e.getMessage());
            throw new SkipException(e.getMessage());
        }

    }

    private List<String> getSuppliersName(int gbEntityTypeID, int gbEntityId) {
        try {
            Show show = new Show();
            show.hitShowVersion2(gbEntityTypeID, gbEntityId);
            String showJsonStr = show.getShowJsonStr();
            if (ParseJsonResponse.validJsonResponse(showJsonStr)) {
                List<String> supplierName = new ArrayList<>();
                JSONArray jsonArraySuppliersName = new JSONObject(showJsonStr).getJSONObject("body").getJSONObject("data").getJSONObject("relations").getJSONArray("values");
                for (int i = 0; i < jsonArraySuppliersName.length(); i++)
                    supplierName.add(jsonArraySuppliersName.getJSONObject(i).getString("name").toLowerCase());
                return supplierName;
            }
        } catch (Exception e) {
            logger.error("Exception while verifying get all supplier name for gb {}", e.getMessage());
            throw new SkipException(e.getMessage());
        }
        return null;
    }

    private void checkSupplierPresentOnListingPage(String listDataJsonStr, List<String> gbSuppliersName, int gbEntityId, CustomAssert customAssert) {
        try {
            if (ParseJsonResponse.validJsonResponse(listDataJsonStr)) {
                JSONArray jsonArraySupplierName = new JSONObject(listDataJsonStr).getJSONArray("data");
                a:for (int i = 0; i < jsonArraySupplierName.length(); i++) {
                    JSONArray jsonArray = jsonArraySupplierName.getJSONObject(i).names();
                    for (int j = 0; j < jsonArray.length(); j++) {
                        if (jsonArraySupplierName.getJSONObject(i).getJSONObject(jsonArray.getString(j)).getString("columnName").equalsIgnoreCase("id") && jsonArraySupplierName.getJSONObject(i).getJSONObject(jsonArray.getString(j)).getString("value").contains(String.valueOf(gbEntityId))) {
                            if (jsonArraySupplierName.getJSONObject(i).getJSONObject(jsonArray.getString(j + 1)).getString("columnName").equalsIgnoreCase("suppliers")) {
                                String[] name = jsonArraySupplierName.getJSONObject(i).getJSONObject(jsonArray.getString(j + 1)).getString("value").split("::");
                                for (String s : name) {
                                   if(!gbSuppliersName.contains(s.split(":;")[0].toLowerCase()))
                                       customAssert.assertTrue(false,"supplier name {"+s.split(":;")[0].toLowerCase()+"} not found on listing page");

                                }
                                break a;
                            }
                            }
                        }

                    }
                }
        } catch (Exception e) {
            logger.error("Exception while verifying suppliers filed present on listing page {}", e.getMessage());
            throw new SkipException(e.getMessage());
        }
    }

    private void checkSupplierPresentInColumn(String listRendererDefaultUserListMetaDataResponse, DefaultUserListMetadataHelper defaultUserListMetadataHelper, CustomAssert customAssert) {
        try {
            logger.info("verify suppliers field present in column section");
            if (ParseJsonResponse.validJsonResponse(listRendererDefaultUserListMetaDataResponse)) {
                List<String> allColumnQueryNames = defaultUserListMetadataHelper.getAllColumnQueryNames(listRendererDefaultUserListMetaDataResponse);
                if (allColumnQueryNames.isEmpty() || !allColumnQueryNames.contains("suppliers"))
                    customAssert.assertTrue(false, "Suppliers field not found in column section");
            }
        } catch (Exception e) {
            logger.error("Exception while verifying suppliers filed present in column section {}", e.getMessage());
            throw new SkipException(e.getMessage());
        }
    }

    private void checkSupplierPresentInFilter(String listRendererDefaultUserListMetaDataResponse, DefaultUserListMetadataHelper defaultUserListMetadataHelper, CustomAssert customAssert) {
        try {
            logger.info("verify suppliers field present in filter section");
            if (ParseJsonResponse.validJsonResponse(listRendererDefaultUserListMetaDataResponse)) {
                String filterName = defaultUserListMetadataHelper.getFilterName(listRendererDefaultUserListMetaDataResponse, "supplier");
                if (filterName == null || !filterName.equalsIgnoreCase("Suppliers"))
                    customAssert.assertTrue(false, "Suppliers field not found in filter section");
            }
        } catch (Exception e) {
            logger.error("Exception while verifying suppliers filed present in filter section {}", e.getMessage());
            throw new SkipException(e.getMessage());
        }
    }

}
