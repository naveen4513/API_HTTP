package com.sirionlabs.test;

import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.massEmail.CreateForm;
import com.sirionlabs.api.massEmail.CreateMassEmail;
import com.sirionlabs.api.massEmail.UpdateForm;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Listeners(value = MyTestListenerAdapter.class)
public class TestMassEmail extends TestRailBase {

    private final static Logger logger = LoggerFactory.getLogger(TestMassEmail.class);
    private String configFilePath = null;
    private String configFileName = null;

    @BeforeClass
    public void beforeClass() {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("MassEmailConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("MassEmailConfigFileName");

        testCasesMap = getTestCasesMapping();
    }

	/*@DataProvider
	public Object[][] dataProviderForMassEmail() {
		List<Object[]> allTestData = new ArrayList<>();
		try {
			logger.info("Setting all Flows to Test.");
			String[] flowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowstotest").split(Pattern.quote(","));

			for (String flow : flowsToTest) {
				allTestData.add(new Object[]{flow.trim()});
			}

			logger.info("Total Flows to Test : {}", allTestData.size());
		} catch (Exception e) {
			logger.error("Exception while Setting all flows to test for Mass Email. {}", e.getMessage());
		}
		return allTestData.toArray(new Object[0][]);
	}*/

    @Test
    public void testMassEmail() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Setting all Flows to Test.");
            String[] allFlowsToTest = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "flowstotest").split(Pattern.quote(","));

            for (String flowToTest : allFlowsToTest) {
                flowToTest = flowToTest.trim();

                logger.info("Validating Flow {} for Mass Email.", flowToTest);
                String subject = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "subject");
                String body = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "body");

                String payload = "{\"subject\":\"" + subject + "\",\"body\":\"<p>" + body + "</p>\",\"status\":\"sent\",\"scheduleDate\":null," +
                        "\"formattedScheduleDate\":null,\"createdBy\":null,\"filePath\":null,\"selectedDistributionLists\":null,\"selectedDisclaimer\":null,\"errors\":null," +
                        "\"key\":\"\"";

                boolean payloadCreated = true;

                if (ParseConfigFile.hasProperty(configFilePath, configFileName, flowToTest, "selectedUsers") &&
                        !ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "selectedUsers")
                                .trim().equalsIgnoreCase("")) {
                    String selectedUsersPayload = getSelectedUsersPayload(flowToTest);

                    if (selectedUsersPayload != null) {
                        payload += ",\"selectedUsers\":" + selectedUsersPayload;
                    } else {
                        logger.error("Couldn't create Selected Users Payload for Flow [{}]. Hence not proceeding further.", flowToTest);
                        csAssert.assertTrue(false, "Couldn't create Selected Users Payload for Flow [" + flowToTest + "]. Hence not proceeding further.");
                        payloadCreated = false;
                    }
                } else {
                    payload += ",\"selectedUsers\": null";
                }

                if (ParseConfigFile.hasProperty(configFilePath, configFileName, flowToTest, "emailIds") &&
                        !ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "emailIds").trim().equalsIgnoreCase("")) {
                    String externalIdsPayload = getExternalIdsPayload(flowToTest);
                    payload += ",\"externalEmails\":" + externalIdsPayload;
                } else {
                    payload += ",\"externalEmails\": null";
                }

                payload += "}";

                if (payloadCreated) {
                    CreateMassEmail createObj = new CreateMassEmail();
                    String jsonStr = createObj.hitCreateMassEmail(payload);

                    if (ParseJsonResponse.validJsonResponse(jsonStr)) {
                        //Verify the Mass Email in List Data Response
                        logger.info("Hitting ListRenderer List Data API for Mass Email Flow [{}]", flowToTest);

                        ListRendererListData listDataObj = new ListRendererListData();
                        int listId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "massEmailListId"));
                        String listDataPayload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":20,\"orderByColumnName\":null,\"orderDirection\":null,\"" +
                                "filterJson\":{}}}";

                        listDataObj.hitListRendererListData(listId, listDataPayload);
                        String listDataJsonStr = listDataObj.getListDataJsonStr();

                        if (ParseJsonResponse.validJsonResponse(listDataJsonStr)) {
                            listDataObj.setListData(listDataJsonStr);
                            List<Map<Integer, Map<String, String>>> listData = listDataObj.getListData();
                            logger.info("Verifying List Data Record for Flow [{}]", flowToTest);

                            if (listData.size() > 0) {
                                int subjectColumnNo = listDataObj.getColumnIdFromColumnName("subject");

                                String subjectValue = listData.get(0).get(subjectColumnNo).get("value").trim();
                                if (subjectValue.toLowerCase().contains(subject.trim().toLowerCase())) {
                                    int statusColumnNo = listDataObj.getColumnIdFromColumnName("status");

                                    if (listData.get(0).get(statusColumnNo).get("value").trim().equalsIgnoreCase("sent")) {
                                        int emailRecordId = Integer.parseInt(listData.get(0).get(subjectColumnNo).get("valueId"));

                                        logger.info("Hitting Update Form API for Flow [{}] and Record Id {}.", flowToTest, emailRecordId);
                                        UpdateForm formObj = new UpdateForm();
                                        String formJsonStr = formObj.hitMassEmailUpdateForm(emailRecordId);

                                        if (ParseJsonResponse.validJsonResponse(formJsonStr)) {
                                            logger.info("Verifying Update Form Record for Flow [{}]", flowToTest);
                                            JSONObject jsonObj = new JSONObject(formJsonStr);

                                            if (!jsonObj.getString("subject").trim().equalsIgnoreCase(subject.trim())) {
                                                logger.error("Expected Subject Value is [{}] and Actual Subject Value is [{}]", subject.trim(), jsonObj.getString("subject"));
                                                csAssert.assertTrue(false, "Expected Subject Value is [" + subject.trim() + "] and Actual Subject Value is ["
                                                        + jsonObj.getString("subject") + "]");
                                            }

                                            if (!jsonObj.getString("body").trim().toLowerCase().contains(body.trim().toLowerCase())) {
                                                logger.error("Expected Body Value is [{}] and Actual Body Value is [{}]", body.trim(), jsonObj.getString("body"));
                                                csAssert.assertTrue(false, "Expected Subject Value is [" + body.trim() + "] and Actual Body Value is [" +
                                                        jsonObj.getString("body") + "]");
                                            }
                                        } else {
                                            logger.error("Mass Email Update Form API Response for Flow [{}] and Record Id {} is an Invalid JSON.", flowToTest, emailRecordId);
                                            csAssert.assertTrue(false, "Mass Email Update Form API Response for Flow [" + flowToTest + "] and Record Id " +
                                                    emailRecordId + " is an Invalid JSON.");
                                        }
                                    } else {
                                        logger.error("Mass Email Status for Flow [{}] is not Sent.", flowToTest);
                                        csAssert.assertTrue(false, "Mass Email Status for Flow [" + flowToTest + "] is not Sent.");
                                    }
                                } else {
                                    logger.error("Couldn't find Mass Email Record in List Data for Flow [{}]", flowToTest);
                                    csAssert.assertTrue(false, "Couldn't find Mass Email Record in List Data for Flow [" + flowToTest + "].");
                                }
                            } else {
                                logger.error("No Record found in List Data Response for Flow [{}]", flowToTest);
                                csAssert.assertTrue(false, "No Record found in List Data Response for Flow [" + flowToTest + "].");
                            }
                        } else {
                            logger.error("List Data Response for Flow [{}] is an Invalid JSON.", flowToTest);
                            csAssert.assertTrue(false, "List Data Response for Flow [" + flowToTest + "] is an Invalid JSON.");
                        }
                    } else {
                        logger.error("Create Mass Email Response for Flow [{}] is an Invalid JSON.", flowToTest);
                        csAssert.assertTrue(false, "Create Mass Email Response for Flow [" + flowToTest + "] is an Invalid JSON.");
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while validating Mass Email. {}", e.getMessage());
            csAssert.assertTrue(false, "Exception while validating Mass Email. " + e.getMessage());
        }

        addTestResult(getTestCaseIdForMethodName("testMassEmail"), csAssert);
        csAssert.assertAll();
    }

    private String getSelectedUsersPayload(String flowToTest) {
        String payload = null;
        try {
            logger.info("Creating Selected Users Payload for Flow [{}]", flowToTest);
            String[] selectedUsers = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "selectedUsers")
                    .trim().split(Pattern.quote(","));
            payload = "[";

            String createFormResponse = CreateForm.getCreateFormResponse(-1);

            for (String user : selectedUsers) {
                int id = CreateForm.getIdForUser(createFormResponse, user);

                if (id != -1) {
                    payload += "{\"id\":" + id + "},";
                } else {
                    logger.error("Couldn't get Id for User {} in Flow [{}].", user.trim(), flowToTest);
                    return null;
                }
            }

            payload = payload.substring(0, payload.length() - 1);
            payload += "]";
        } catch (Exception e) {
            logger.error("Exception while getting Selected Users Payload for Flow [{}]. {}", flowToTest, e.getStackTrace());
        }
        return payload;
    }

    private String getExternalIdsPayload(String flowToTest) {
        String payload = null;
        try {
            logger.info("Creating External Users Payload for Flow [{}]", flowToTest);
            String[] externalIds = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, flowToTest, "emailIds")
                    .trim().split(Pattern.quote(","));
            payload = "[";

            for (String emailId : externalIds) {
                payload = payload.concat("\"" + emailId.trim() + "\",");
            }

            payload = payload.substring(0, payload.length() - 1);
            payload += "]";
        } catch (Exception e) {
            logger.error("Exception while getting External Ids Payload for Flow [{}]. {}", flowToTest, e.getStackTrace());
        }
        return payload;
    }
}
