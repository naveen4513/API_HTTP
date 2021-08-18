package com.sirionlabs.test.microservice.email;

import com.sirionlabs.utils.commonUtils.XLSUtils;
import org.testng.annotations.DataProvider;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmailMicroServiceDataProvider {


    static XLSUtils  reader;
    static String filePath =".\\src\\test\\resources\\TestData";
    static String fileName = "EmailMicrosrvice.xlsx";

    static {
        try {
            reader = new XLSUtils(filePath,fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @DataProvider(name = "CreateEmailConfDataProvider" )
    public static Object[][] createEmailConfDataProvider() throws IOException {
        String  sheetName = "Create_Email_Conf";
        int rowcount = reader.getRowCount(sheetName);
        Object result[][] = new Object[rowcount - 1][3];

        for (int i = 2; i <=rowcount ; i++) {
            Map<Object, Object> valueMap = new HashMap<>();
            valueMap.put("tc_id",reader.getCellData(sheetName,"tc_id",i));
            valueMap.put("tc_type",reader.getCellData(sheetName,"tc_type",i));
            valueMap.put("payload",reader.getCellData(sheetName,"payload",i));
            valueMap.put("clientId",reader.getCellData(sheetName,"clientId",i));
            valueMap.put("entityTypeId",reader.getCellData(sheetName,"entityTypeId",i));
            valueMap.put("emailSubject",reader.getCellData(sheetName,"emailSubject",i));
            valueMap.put("emailBody",reader.getCellData(sheetName,"emailBody",i));
            valueMap.put("name",reader.getCellData(sheetName,"name",i)+"_"+System.currentTimeMillis());
            valueMap.put("languageId",reader.getCellData(sheetName,"languageId",i));
            valueMap.put("bulkEmailSubject",reader.getCellData(sheetName,"bulkEmailSubject",i));
            valueMap.put("bulkEmailBody",reader.getCellData(sheetName,"bulkEmailBody",i));
            valueMap.put("toRoleGroups",reader.getCellData(sheetName,"toRoleGroups",i));
            valueMap.put("ccRoleGroups",reader.getCellData(sheetName,"ccRoleGroups",i));
            valueMap.put("bccRoleGroups",reader.getCellData(sheetName,"bccRoleGroups",i));
            valueMap.put("toUserRoleGroups",reader.getCellData(sheetName,"toUserRoleGroups",i));
            valueMap.put("ccUserRoleGroups",reader.getCellData(sheetName,"ccUserRoleGroups",i));
            valueMap.put("bccUserRoleGroups",reader.getCellData(sheetName,"bccUserRoleGroups",i));
            valueMap.put("toEmailIds","");
            valueMap.put("ccEmailIds","");
            valueMap.put("bccEmailIds","");
            valueMap.put("ExpectedStatusCode",reader.getCellData(sheetName,"ExpectedStatusCode",i));
            valueMap.put("ExpectedMessage",reader.getCellData(sheetName,"ExpectedMessage",i));


            result[i - 2][0] = i;
            result[i - 2][1] = valueMap.get("tc_type");
            result[i - 2][2] = valueMap;
        }
        return result;
    }

    @DataProvider(name = "FindDefaultTemplate" )
    public static Object[][] findDefaultTemplate() throws IOException {
        String  sheetName = "Find_Default_Template";
        int rowcount = reader.getRowCount(sheetName);
        Object result[][] = new Object[rowcount - 1][7];
        for (int i = 2; i <=rowcount ; i++) {
            result[i - 2][0] = i;
            result[i - 2][1] = reader.getCellData(sheetName,"tc_id",i);
            result[i - 2][2] = reader.getCellData(sheetName,"tc_type",i);
            result[i - 2][3] = reader.getCellData(sheetName,"clientId",i);
            result[i - 2][4] = reader.getCellData(sheetName,"entityTypeId",i);
            result[i - 2][5] = reader.getCellData(sheetName,"ExpectedStatusCode",i);
            result[i - 2][6] = reader.getCellData(sheetName,"ExpectedMessage",i);


        }
        return result;
    }


    @DataProvider(name = "ListEntityActionNames" )
    public static Object[][] listEntityActionNames() throws IOException {
        String  sheetName = "List_Entity_Action_Names";
        int rowcount = reader.getRowCount(sheetName);
        Object result[][] = new Object[rowcount - 1][7];
        for (int i = 2; i <=rowcount ; i++) {
            result[i - 2][0] = i;
            result[i - 2][1] = reader.getCellData(sheetName,"tc_id",i);
            result[i - 2][2] = reader.getCellData(sheetName,"tc_type",i);
            result[i - 2][3] = reader.getCellData(sheetName,"clientId",i);
            result[i - 2][4] = reader.getCellData(sheetName,"entityTypeId",i);
            result[i - 2][5] = reader.getCellData(sheetName,"ExpectedStatusCode",i);
            result[i - 2][6] = reader.getCellData(sheetName,"ExpectedMessage",i);

        }
        return result;
    }


    @DataProvider(name = "UpdateEmailConfDataProvider" )
    public static Object[][] updateEmailConfDataProvider() throws IOException {
        String  sheetName = "Update_Email_Conf";
        int rowcount = reader.getRowCount(sheetName);
        Object result[][] = new Object[rowcount - 1][3];

        for (int i = 2; i <=rowcount ; i++) {
            Map<Object, Object> valueMap = new HashMap<>();
            valueMap.put("tc_id",reader.getCellData(sheetName,"tc_id",i));
            valueMap.put("tc_type",reader.getCellData(sheetName,"tc_type",i));
            valueMap.put("payload",reader.getCellData(sheetName,"payload",i));
            valueMap.put("clientId",reader.getCellData(sheetName,"clientId",i));
            valueMap.put("entityTypeId",reader.getCellData(sheetName,"entityTypeId",i));
            valueMap.put("emailSubject",reader.getCellData(sheetName,"emailSubject",i));
            valueMap.put("emailBody",reader.getCellData(sheetName,"emailBody",i));
            valueMap.put("name",reader.getCellData(sheetName,"name",i)+"_"+System.currentTimeMillis());
            valueMap.put("languageId",reader.getCellData(sheetName,"languageId",i));
            valueMap.put("toRoleGroups",reader.getCellData(sheetName,"toRoleGroups",i));
            valueMap.put("ccRoleGroups",reader.getCellData(sheetName,"ccRoleGroups",i));
            valueMap.put("bccRoleGroups",reader.getCellData(sheetName,"bccRoleGroups",i));
            valueMap.put("toUserRoleGroups",reader.getCellData(sheetName,"toUserRoleGroups",i));
            valueMap.put("ccUserRoleGroups",reader.getCellData(sheetName,"ccUserRoleGroups",i));
            valueMap.put("bccUserRoleGroups",reader.getCellData(sheetName,"bccUserRoleGroups",i));
            valueMap.put("toEmailIds","");
            valueMap.put("ccEmailIds","");
            valueMap.put("bccEmailIds","");
            valueMap.put("ExpectedStatusCode",reader.getCellData(sheetName,"ExpectedStatusCode",i));
            valueMap.put("ExpectedMessage",reader.getCellData(sheetName,"ExpectedMessage",i));


            result[i - 2][0] = i;
            result[i - 2][1] = valueMap.get("tc_type");
            result[i - 2][2] = valueMap;
        }
        return result;
    }


    @DataProvider(name = "FindEmailConfiguration" )
    public static Object[][] findEmailConfiguration() throws IOException {
        String  sheetName = "Find_Email_Conf";
        int rowcount = reader.getRowCount(sheetName);
        Object result[][] = new Object[rowcount - 1][3];
        for (int i = 2; i <=rowcount ; i++) {
            Map<Object, Object> valueMap = new HashMap<>();
            valueMap.put("tc_id",reader.getCellData(sheetName,"tc_id",i));
            valueMap.put("tc_type",reader.getCellData(sheetName,"tc_type",i));
            valueMap.put("clientId",reader.getCellData(sheetName,"clientId",i));
            valueMap.put("entityTypeId",reader.getCellData(sheetName,"entityTypeId",i));
            valueMap.put("emailSubject",reader.getCellData(sheetName,"emailSubject",i));
            valueMap.put("emailBody",reader.getCellData(sheetName,"emailBody",i));
            valueMap.put("name",reader.getCellData(sheetName,"name",i));
            valueMap.put("languageId",reader.getCellData(sheetName,"languageId",i));
            valueMap.put("toRoleGroups",reader.getCellData(sheetName,"toRoleGroups",i));
            valueMap.put("ccRoleGroups",reader.getCellData(sheetName,"ccRoleGroups",i));
            valueMap.put("bccRoleGroups",reader.getCellData(sheetName,"bccRoleGroups",i));
            valueMap.put("toUserRoleGroups",reader.getCellData(sheetName,"toUserRoleGroups",i));
            valueMap.put("ccUserRoleGroups",reader.getCellData(sheetName,"ccUserRoleGroups",i));
            valueMap.put("bccUserRoleGroups",reader.getCellData(sheetName,"bccUserRoleGroups",i));
            valueMap.put("toEmailIds","");
            valueMap.put("ccEmailIds","");
            valueMap.put("bccEmailIds","");
            valueMap.put("ExpectedStatusCode",reader.getCellData(sheetName,"ExpectedStatusCode",i));
            valueMap.put("ExpectedMessage",reader.getCellData(sheetName,"ExpectedMessage",i));

            result[i - 2][0] = i;
            result[i - 2][1] = valueMap.get("tc_type");
            result[i - 2][2] = valueMap;
        }
        return result;
    }


    @DataProvider(name = "URGMapperAuthentication" )
    public static Object[][] urgMapperAuthentication() throws IOException {
        String  sheetName = "URGMapperAuth";
        int rowcount = reader.getRowCount(sheetName);
        Object result[][] = new Object[rowcount - 1][3];
        for (int i = 2; i <=rowcount ; i++) {
            Map<Object, Object> valueMap = new HashMap<>();
            valueMap.put("tc_id",reader.getCellData(sheetName,"tc_id",i));
            valueMap.put("tc_type",reader.getCellData(sheetName,"tc_type",i));
            valueMap.put("secretKey",reader.getCellData(sheetName,"secretKey",i));
            valueMap.put("issuer",reader.getCellData(sheetName,"issuer",i));
            valueMap.put("payload",reader.getCellData(sheetName,"payload",i));
            valueMap.put("expiryTimeMin",reader.getCellData(sheetName,"expiryTimeMin",i));
            valueMap.put("entityId",reader.getCellData(sheetName,"entityId",i));
            valueMap.put("entityTypeId",reader.getCellData(sheetName,"entityTypeId",i));
            valueMap.put("activeUsersOnly",reader.getCellData(sheetName,"activeUsersOnly",i));
            valueMap.put("isWorkflow",reader.getCellData(sheetName,"isWorkflow",i));
            valueMap.put("toRoleGroups",reader.getCellData(sheetName,"toRoleGroups",i));
            valueMap.put("ccRoleGroups",reader.getCellData(sheetName,"ccRoleGroups",i));
            valueMap.put("bccRoleGroups",reader.getCellData(sheetName,"bccRoleGroups",i));
            valueMap.put("toUserRoleGroups",reader.getCellData(sheetName,"toUserRoleGroups",i));
            valueMap.put("ccUserRoleGroups",reader.getCellData(sheetName,"ccUserRoleGroups",i));
            valueMap.put("bccUserRoleGroups",reader.getCellData(sheetName,"bccUserRoleGroups",i));
            valueMap.put("toEmailIds","");
            valueMap.put("ccEmailIds","");
            valueMap.put("bccEmailIds","");
            valueMap.put("ExpectedStatusCode",reader.getCellData(sheetName,"ExpectedStatusCode",i));
            valueMap.put("ExpectedMessage",reader.getCellData(sheetName,"ExpectedMessage",i));

            result[i - 2][0] = i;
            result[i - 2][1] = valueMap.get("tc_type");
            result[i - 2][2] = valueMap;
        }
        return result;
    }


    @DataProvider(name = "URGMapperAPI" )
    public static Object[][] urgMapperAPI() throws IOException {
        String  sheetName = "URGMapper";
        int rowcount = reader.getRowCount(sheetName);
        Object result[][] = new Object[rowcount - 1][3];
        for (int i = 2; i <=rowcount ; i++) {
            Map<Object, Object> valueMap = new HashMap<>();
            valueMap.put("tc_id",reader.getCellData(sheetName,"tc_id",i));
            valueMap.put("tc_type",reader.getCellData(sheetName,"tc_type",i));
            valueMap.put("secretKey",reader.getCellData(sheetName,"secretKey",i));
            valueMap.put("issuer",reader.getCellData(sheetName,"issuer",i));
            valueMap.put("payload",reader.getCellData(sheetName,"payload",i));
            valueMap.put("expiryTimeMin",reader.getCellData(sheetName,"expiryTimeMin",i));
            valueMap.put("entityId",reader.getCellData(sheetName,"entityId",i));
            valueMap.put("entityTypeId",reader.getCellData(sheetName,"entityTypeId",i));
            valueMap.put("activeUsersOnly",reader.getCellData(sheetName,"activeUsersOnly",i));
            valueMap.put("isWorkflow",reader.getCellData(sheetName,"isWorkflow",i));
            valueMap.put("toRoleGroups",reader.getCellData(sheetName,"toRoleGroups",i));
            valueMap.put("ccRoleGroups",reader.getCellData(sheetName,"ccRoleGroups",i));
            valueMap.put("bccRoleGroups",reader.getCellData(sheetName,"bccRoleGroups",i));
            valueMap.put("toUserRoleGroups",reader.getCellData(sheetName,"toUserRoleGroups",i));
            valueMap.put("ccUserRoleGroups",reader.getCellData(sheetName,"ccUserRoleGroups",i));
            valueMap.put("bccUserRoleGroups",reader.getCellData(sheetName,"bccUserRoleGroups",i));
            valueMap.put("toEmailIds","");
            valueMap.put("ccEmailIds","");
            valueMap.put("bccEmailIds","");
            valueMap.put("ExpectedStatusCode",reader.getCellData(sheetName,"ExpectedStatusCode",i));
            valueMap.put("ExpectedMessage",reader.getCellData(sheetName,"ExpectedMessage",i));
            valueMap.put("ExpectedResult",reader.getCellData(sheetName,"ExpectedResult",i));

            result[i - 2][0] = i;
            result[i - 2][1] = valueMap.get("tc_type");
            result[i - 2][2] = valueMap;
        }
        return result;
    }


}
