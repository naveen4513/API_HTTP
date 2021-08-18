package com.sirionlabs.test.auditlogreporting;


import com.sirionlabs.api.auditlogreporting.AuditLogReportApi;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.JSONUtility;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class TestAuditlogReportApi {
    private AuditLogReportApi api;
    private String AuditLogReportConfigFilePath;
    private String AuditLogReportConfigFileName;
    private PostgreSQLJDBC db = null;

    private final static Logger logger = LoggerFactory.getLogger(TestAuditlogReportApi.class);

    @BeforeClass
    public void beforeClass() {
        api = new AuditLogReportApi();
        AuditLogReportConfigFilePath = ConfigureConstantFields.getConstantFieldsProperty("AuditLogReportConfigFilePath");
        AuditLogReportConfigFileName = ConfigureConstantFields.getConstantFieldsProperty("AuditLogReportConfigFileName");


    }

    @DataProvider(parallel = true)
    public Object[][] dataProviderForAuditLogReport() {
        List<Object[]> allTestData = new ArrayList<>();

        //String[] flows = {"flow1","flow2","flow3","flow4","flow5","flow6","flow7"};
        String[] flows = {"flow7"};

        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForAuditLogReport")
    public void TestAuditlogReportFetchApi(String flow) throws SQLException {
        List<Integer> idList = null;
        PostgreSQLJDBC db = new PostgreSQLJDBC();
        CustomAssert customAssert = new CustomAssert();
        String payload = ParseConfigFile.getValueFromConfigFile(AuditLogReportConfigFilePath, AuditLogReportConfigFileName, "payload", flow);
        String response = api.hitAuditLogReportFetchDataApi(payload);
        if (flow.equals("flow1")) {
            customAssert.assertTrue(response.contains("Error: Required Index does not exits"), "resoponse is " + response);
            List<List<String>> other_audit = db.doSelect("Select * from other_audit_log where client_id= 1003 and indexed= true;");
            List<List<String>> mgmt_audit = db.doSelect("Select * from mgmt_audit_log where client_id= 1003 and indexed= true;");
            List<List<String>> dno_sla_audit = db.doSelect("Select * from dno_sla_audit_log where client_id= 1003 and indexed= true;");
            customAssert.assertEquals(other_audit.size(), 0, response + "while indexed data present in other_audit table  ");
            customAssert.assertEquals(mgmt_audit.size(), 0, response + "while indexed data present in mgmt_audit table  ");
            customAssert.assertEquals(dno_sla_audit.size(), 0, response + "while indexed data present in dno_sla_audit table  ");
        } else if (flow.equals("flow2")) {
            customAssert.assertTrue(response.contains("Size cannot exceed 100"), "resoponse is " + response);
        } else if (flow.equals("flow3")) {

            idList = (ArrayList) JSONUtility.parseJson(response, "$.[*].id");

            int result = 0;
            for (int i = 0; i < idList.size() - 1; i++) {
                if (idList.get(i) > idList.get(i + 1)) {
                    result = -1;
                }
            }

            customAssert.assertFalse(result == -1, "response " + response + " is not in ascending order");


        } else if (flow.equals("flow4")) {
            idList = (ArrayList) JSONUtility.parseJson(response, "$.[*].id");

            int result = 0;
            for (int i = 0; i < idList.size() - 1; i++) {
                if (idList.get(i) < idList.get(i + 1)) {
                    result = -1;
                }
            }

            customAssert.assertFalse(result == -1, "response " + response + " is not in descending order");
        }
        else if (flow.equals("flow5")) {
           List<String> name_List = (ArrayList) JSONUtility.parseJson(response, "$.[*].requested_by_name");

            int result = 0;
            for (int i = 0; i < name_List.size() - 1; i++) {
                if (name_List.get(i).compareTo(name_List.get(i + 1))> 0) {
                    result = -1;
                }
            }

            customAssert.assertFalse(result == -1, "response " + response + " is not in ascending order");
        }
        else if (flow.equals("flow6")) {
            List<Integer> entity_type_id_List = (ArrayList) JSONUtility.parseJson(response, "$.[*].entity_type_id");
            Set<Integer> hSet = new HashSet<Integer>(entity_type_id_List);
            Set<Integer> expected = new HashSet<Integer>();
            expected.add(86);
            expected.add(87);
            if(!expected.containsAll(hSet)){
                customAssert.assertFalse(true, "response " + response + " contains data than 86 and 87 entity type");
            }

        }
        else if (flow.equals("flow7")) {
            List<String> comment_List = (ArrayList) JSONUtility.parseJson(response, "$.[*].comment");;

            int result =0;
            for (String comment : comment_List) {
                if(!comment.contains("test")){
                          result = -1;
                          break;
               }

            }
            customAssert.assertFalse(result == -1, "response "+ response + "does not contain comment having \"test\" keyword");


        }



        customAssert.assertAll();
    }


}
