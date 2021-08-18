package com.sirionlabs.test.governanceBody;

import com.sirionlabs.api.clientAdmin.EntityDumpReport.ElasticDataDump;
import com.sirionlabs.api.clientAdmin.EntityDumpReport.ElasticDataDumpCreateForm;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.DateUtils;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class UserAuditLogDumpReport {
    private final static Logger logger = LoggerFactory.getLogger(UserAuditLogDumpReport.class);
    private final String dbHostAddress = "192.168.2.158";
    private final String dbPortName = "5432";
    private final String dbName = "letterbox_auth";
    private final String dbUserName = "postgres";
    private final String dbPassword = "T8k2H){6D$";

    @DataProvider()
    public Object[][] dataProviderForEntityDumpReport() {
        List<Object[]> allTestData = new ArrayList<>();
        String[] flows = {"User Audit Log Report", "Supplier", "Invoice", "Service Data", "Purchase Order", "SLA", "Child SLA", "Issue Management", "Contract Draft Request", "WorkOrderRequest", "Consumption", "Child Obligations", "Contract Template Structure", "Definition", "Vendor", "Governance Body Meeting", "Contract Interpretation", "Invoice Line Item", "Obligations", "Action Item Management", "Governance Body", "Contract Change Request", "Clause", "Dispute Management", "Contract"};
        for (String entity : flows) {
            allTestData.add(new Object[]{entity.trim()});
        }
        return allTestData.toArray(new Object[0][]);
    }

    @Test(dataProvider = "dataProviderForEntityDumpReport")
    public void testEntityDumpReport(String reportName) {
        CustomAssert customAssert = new CustomAssert();
        try {
            logger.error("Verify Report In Entity Dump Section {}", reportName);

            logger.info("Hitting Data Dump Create Form API");
            APIResponse apiResponse = ElasticDataDumpCreateForm.getElasticDataDumpCreateFormResponse();
            if (apiResponse.getResponseCode() == 200) {
                Elements elements = Jsoup.parse(apiResponse.getResponseBody()).getElementById("_entityTypeId_id").children();
                HashMap<String, String> reportOptionWithId = new HashMap<>();
                for (org.jsoup.nodes.Element element : elements) {
                    reportOptionWithId.put(element.text(), element.val());
                }
                if (reportOptionWithId.containsKey(reportName)) {
                    HashMap<String, String> payload = new HashMap<String, String>();
                    payload.put("entityTypeId", reportOptionWithId.get(reportName).trim());
                    payload.put("email", "kanhaiya.saini@sirionqa.office");
                    payload.put("startTime", "07");
                    payload.put("timeInterval", "2");
                    payload.put("_fullDump", "on");
                    payload.put("_csrf_token", "");
                    APIResponse elasticDataDumpResponse = ElasticDataDump.getElasticDataDumpResponse(payload);
                    if (elasticDataDumpResponse.getResponseCode() == 302) {
                        if (reportName.equalsIgnoreCase("WorkOrderRequest")) {
                            reportName = "Work Order Request";
                        } else if (reportName.equalsIgnoreCase("Contract Draft Request")) {
                            reportName = "Contract Requests";
                        }
                        Thread.sleep(30000);
                        String date = DateUtils.getCurrentDateInAnyFormat("yyyy-MM-dd HH:mm:ss", "UTC");
                        PostgreSQLJDBC postgreSQLJDBC = new PostgreSQLJDBC(dbHostAddress, dbPortName, dbName, dbUserName, dbPassword);
                        String query = "select * from system_emails se where client_id =1007 and subject ilike '%Entity Data Dump Report for " + reportName.replaceAll(" ", "_") + "%' and date_created> '" + date + "' order by id desc\n" +
                                "\n";
                        logger.info("Entity Data Dump Email Query {}", query);
                        List<List<String>> entityDumpReport = postgreSQLJDBC.doSelect(query);
                        if (entityDumpReport.size() <= 0) {
                            customAssert.assertTrue(false, "Entity Data Dump Report Email not triggered for report => " + reportName);
                        }
                        postgreSQLJDBC.closeConnection();
                    }
                } else {
                    customAssert.assertTrue(false, "Report { " + reportName + " } Not Found In Entity Dump Report Section");
                }
            } else {
                customAssert.assertTrue(false, "Create Entity Report Button Not Found/clickable");
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            customAssert.assertTrue(false, "Exception While Verify Report In Entity Report Section");
        }
        customAssert.assertAll();
    }
}
