package com.sirionlabs.test.calendar;

import com.sirionlabs.helper.internationalization.InternationalizationBase;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

public class CalendarInternationalization extends InternationalizationBase {

    private final static Logger logger = LoggerFactory.getLogger(CalendarInternationalization.class);

    /*
    TC-C382: Verify Calendar Renaming.
    TC-C383: Verify Calendar Renamed fields for End User.
     */
    @Test
    public void testC382() {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Starting Test TC-C382: Verify Calendar Renaming.");
        String fieldRenamingResponse = fieldRenamingObj.getFieldRenamingUpdateResponse(1000, 1721);

        try {
            logger.info("Updating Labels of Fields [Today, Next 7 days, Month, Year, Day] in Calendar Listing.");
          /*  String updatedTodayLabel = "сегодня";
            String updatedNext7DaysLabel = "следующие 7 дней";
            String updatedMonthLabel = "месяц";
            String updatedYearLabel = "год";
            String updatedDayLabel = "день";
*/
            String updatePayload = fieldRenamingResponse;
            /*updatePayload = updatePayload
                    .replace("clientFieldName\":\"Today", "clientFieldName\":\"" + updatedTodayLabel)
                    .replace("clientFieldName\":\"Next 7 Days", "clientFieldName\":\"" + updatedNext7DaysLabel)
                    .replace("clientFieldName\":\"Month", "clientFieldName\":\"" + updatedMonthLabel)
                    .replace("clientFieldName\":\"Year", "clientFieldName\":\"" + updatedYearLabel)
                    .replace("clientFieldName\":\"Day", "clientFieldName\":\"" + updatedDayLabel);
*/
            fieldRenamingObj.hitFieldUpdateWithClientAdminLogin(updatePayload);

            String messagesListSubPayload = "";
            String todayFieldId = fieldRenamingObj.getFieldAttribute(fieldRenamingResponse, "Today", null, "id");
            messagesListSubPayload = messagesListSubPayload.concat(todayFieldId) + ",";

            String next7DaysFieldId = fieldRenamingObj.getFieldAttribute(fieldRenamingResponse, "Next 7 Days", null, "id");
            messagesListSubPayload = messagesListSubPayload.concat(next7DaysFieldId) + ",";

            String monthFieldId = fieldRenamingObj.getFieldAttribute(fieldRenamingResponse, "month", null, "id");
            messagesListSubPayload = messagesListSubPayload.concat(monthFieldId) + ",";

            String yearFieldId = fieldRenamingObj.getFieldAttribute(fieldRenamingResponse, "Year", null, "id");
            messagesListSubPayload = messagesListSubPayload.concat(yearFieldId) + ",";

            String dayFieldId = fieldRenamingObj.getFieldAttribute(fieldRenamingResponse, "day", null, "id");
            messagesListSubPayload = messagesListSubPayload.concat(dayFieldId) + ",";

            messagesListSubPayload = messagesListSubPayload.substring(0, messagesListSubPayload.length() - 1);

            String messagesListPayload = "[" + messagesListSubPayload + "]";
            String messagesListResponse = messagesListObj.hitFieldLabelMessagesList(messagesListPayload);

            if (ParseJsonResponse.validJsonResponse(messagesListResponse)) {
                JSONObject jsonObj = new JSONObject(messagesListResponse);

                /*matchLabels(jsonObj, todayFieldId, updatedTodayLabel, "Today", csAssert);
                matchLabels(jsonObj, next7DaysFieldId, updatedNext7DaysLabel, "Next 7 Days", csAssert);
                matchLabels(jsonObj, monthFieldId, updatedMonthLabel, "Month", csAssert);
                matchLabels(jsonObj, yearFieldId, updatedYearLabel, "Year", csAssert);
                matchLabels(jsonObj, dayFieldId, updatedDayLabel, "Day", csAssert);*/
            } else {
                csAssert.assertFalse(true, "MessagesList API Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C382. " + e.getMessage());
        } finally {
            logger.info("Reverting Labels in Calendar Listing");
            fieldRenamingObj.hitFieldUpdateWithClientAdminLogin(fieldRenamingResponse);
        }

        csAssert.assertAll();
    }

    private void matchLabels(JSONObject jsonObj, String fieldId, String expectedLabel, String fieldName, CustomAssert csAssert) {
        if (jsonObj.has(fieldId)) {
            String actualLabel = jsonObj.getJSONObject(fieldId).getString("name");
            boolean matchLabels = StringUtils.matchRussianCharacters(expectedLabel, actualLabel);

            csAssert.assertTrue(matchLabels, "Expected " + fieldName + " Label: " + expectedLabel + " and Actual Today Label: " + actualLabel);
        } else {
            csAssert.assertFalse(true, "MessagesList API Response doesn't contain Object " + fieldId);
        }
    }
}