package com.sirionlabs.test.common;

import com.sirionlabs.helper.internationalization.InternationalizationBase;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class TestUserProfileInternationalization extends InternationalizationBase {

    private final static Logger logger = LoggerFactory.getLogger(TestUserProfileInternationalization.class);

    /*
    TC-C3360: Verify all the static contents in general tab of User Profile renamed.
     */
    @Test
    public void testC3360() {
        CustomAssert csAssert = new CustomAssert();
        logger.info("Starting Test TC-C3360: Verify General Tab of User Profile Renaming.");
        String fieldRenamingResponse = fieldRenamingObj.getFieldRenamingUpdateResponse(1000, 1723);

        try {
            logger.info("Updating Labels of Static fields of General Tab of User Profile.");

            Map<String, String> fieldsMap = new HashMap<>();
            /*fieldsMap.put("Time Zone :", "Часовой пояс");
            fieldsMap.put("Account Information", "Информация об учетной записи");
            fieldsMap.put("Last Login :", "последний Войти");
            fieldsMap.put("User Configuration", "Конфигурация пользователя");
            fieldsMap.put("Change Password", "изменить пароль");
            fieldsMap.put("Your password will expire in %s day(s). Please change your password.", "срок действия пароля истекает через несколько дней");
            fieldsMap.put("First Name", "Имя");
            fieldsMap.put("Last Name", "фамилия");
            fieldsMap.put("Login", "Авторизоваться");
            fieldsMap.put("Email", "Эл. адрес");
            fieldsMap.put("Designation", "обозначение");
            fieldsMap.put("Contact Number", "Контактный телефон");
            fieldsMap.put("Security Question", "вопрос безопасности");
            fieldsMap.put("Password Never Expires", "Пароль никогда не истекает");
            fieldsMap.put("Language", "язык");
            fieldsMap.put("Default Tier", "Уровень по умолчанию");
            fieldsMap.put("Current Tier", "текущий уровень");
            fieldsMap.put("SCHEDULED BY ME", "РАСПИСАНИЕ МЕНЯ");
            fieldsMap.put("SCHEDULED JOBS", "РАСПИСАНИЕ РАБОТ");
            fieldsMap.put("Frequency ", "частота");
            fieldsMap.put("Schedules", "Расписание");
            fieldsMap.put("Governance body schedules", "Графики органов управления");
            fieldsMap.put(" Due Date :", "Срок сдачи");
            fieldsMap.put("Schedule Meeting", "Расписание встречи");
            fieldsMap.put("Reminder Emails", "Напоминание по электронной почте");
            fieldsMap.put("Delegation", "Делегация");
            fieldsMap.put("DELEGATED By Me", "ДЕЛЕГИРОВАНО мной");
            fieldsMap.put("DELEGATED To Me", "ОСУЩЕСТВЛЯЕТСЯ мне");*/

            String updatePayload = fieldRenamingResponse;
            String messagesListSubPayload = "";

            Map<String, String> idMap = new HashMap<>();

            for (Map.Entry<String, String> entryMap : fieldsMap.entrySet()) {
                updatePayload = updatePayload.replace("\"clientFieldName\":\"" + entryMap.getKey() + "\"",
                        "\"clientFieldName\":\"" + entryMap.getValue() + "\"");

                String fieldId = fieldRenamingObj.getFieldAttribute(fieldRenamingResponse, entryMap.getKey(), null, "id");
                idMap.put(entryMap.getKey(), fieldId);

                messagesListSubPayload = messagesListSubPayload.concat(fieldId) + ",";
            }

            fieldRenamingObj.hitFieldUpdateWithClientAdminLogin(updatePayload);

            messagesListSubPayload = messagesListSubPayload.substring(0, messagesListSubPayload.length() - 1);

            String messagesListPayload = "[" + messagesListSubPayload + "]";
            String messagesListResponse = messagesListObj.hitFieldLabelMessagesList(messagesListPayload);

            if (ParseJsonResponse.validJsonResponse(messagesListResponse)) {
                JSONObject jsonObj = new JSONObject(messagesListResponse);

                for (Map.Entry<String, String> entryMap : fieldsMap.entrySet()) {
                    matchLabels(jsonObj, idMap.get(entryMap.getKey()), fieldsMap.get(entryMap.getKey()), entryMap.getKey(), csAssert);
                }
            } else {
                csAssert.assertFalse(true, "MessagesList API Response is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertFalse(true, "Exception while Validating TC-C3360. " + e.getMessage());
        } finally {
            logger.info("Reverting Labels in User Profile Page.");
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