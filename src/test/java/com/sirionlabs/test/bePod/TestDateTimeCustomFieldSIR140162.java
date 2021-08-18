package com.sirionlabs.test.bePod;

import com.sirionlabs.api.bulkedit.BulkeditEdit;
import com.sirionlabs.api.clientAdmin.dynamicMetadata.DynamicMetadataCreate;
import com.sirionlabs.api.clientAdmin.dynamicMetadata.DynamicMetadataList;
import com.sirionlabs.api.clientAdmin.dynamicMetadata.DynamicMetadataShow;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.listRenderer.ListRendererDefaultUserListMetaData;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.metadataSearch.MetadataSearch;
import com.sirionlabs.api.reportRenderer.ReportRendererDefaultUserListMetaData;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.EntityOperationsHelper;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import com.sirionlabs.helper.entityCreation.Contract;
import com.sirionlabs.helper.entityCreation.ContractDraftRequest;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by nikhil.haritash on 17-04-2019.
 */
@Listeners(value = MyTestListenerAdapter.class)
public class TestDateTimeCustomFieldSIR140162 extends TestAPIBase {

    private static final Logger logger = LoggerFactory.getLogger(TestDateTimeCustomFieldSIR140162.class);

    private String contractFieldName = "TestDateTimeContract";
    private String cdrFieldName = "TestDateTimeCDR";

    private Map<Integer, Integer> recordsToDelete = new HashMap<>();

    @AfterClass
    public void afterClass() {
        for (Map.Entry<Integer, Integer> record : recordsToDelete.entrySet()) {
            String entityName = ConfigureConstantFields.getEntityNameById(record.getKey());
            EntityOperationsHelper.deleteEntityRecord(entityName, record.getValue());
        }
    }

    // C63024 Dynamic Date Time field - Entity Listing
    @Test
    public void testC63024() {
        CustomAssert assertion = new CustomAssert();

        try {
            String[] entitiesArr = {
                    "contracts",
                    "contract draft request"
            };

            for (String entityName : entitiesArr) {
                String fieldName = entityName.equalsIgnoreCase("contracts") ? contractFieldName : cdrFieldName;

                String Id = "-1";
                int flag = 0;
                int flag1 = 0;
                int queryId = 0;
                String queryName = "";

                String response = getDynamicMetadataListResponse();
                Document html = Jsoup.parse(response);

                int size = html.getElementById("l_com_sirionlabs_model_MasterGroup").child(1).childNodeSize();
                for (int i = 0; i < size - 1; i++) {
                    if (html.getElementById("l_com_sirionlabs_model_MasterGroup").child(1).child(i).child(0).child(0).attr("sort").toString().equalsIgnoreCase("-")) {
                        continue;
                    }

                    String FieldName = html.getElementById("l_com_sirionlabs_model_MasterGroup").child(1).child(i).child(0).child(0).child(0).childNodes().toString().replace("[", "").replace("]", "").trim();
                    if (FieldName.equalsIgnoreCase("-")) {
                        continue;
                    }
                    if (FieldName.equalsIgnoreCase(fieldName)) {
                        String href = html.getElementById("l_com_sirionlabs_model_MasterGroup").child(1).child(i).child(0).child(0).child(0).getElementsByAttribute("href").attr("href");
                        String[] FieldText = href.split(Pattern.quote("/"));
                        Id = FieldText[FieldText.length - 1];
                        logger.info("Field Id is {}", Id);
                        break;
                    }
                }

                String responseShow = getDynamicMetadataShowResponse(Id);
                Document htmlShow = Jsoup.parse(responseShow);
                String HtmlType = htmlShow.getElementById("_c_com_sirionlabs_business_entity_model_dynamicmetadata_htmltype_htmlType.name_id").childNodes().toString().replace("[", "").replace("]", "").trim();
                logger.info("HtmlType is {}", HtmlType);

                assertion.assertTrue(HtmlType.equalsIgnoreCase("Date Time"), fieldName + " is not having Html Type as  Date Time.");

                int ListId = ConfigureConstantFields.getListIdForEntity(entityName);
                ListRendererDefaultUserListMetaData Dobj = new ListRendererDefaultUserListMetaData();

                try {
                    Dobj.hitListRendererDefaultUserListMetadata(ListId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String DefaultResponse = Dobj.getListRendererDefaultUserListMetaDataJsonStr();

                JSONObject jsonObject = new JSONObject(DefaultResponse);
                JSONArray JsonArr = jsonObject.getJSONArray("columns");
                for (int i = 0; i < JsonArr.length(); i++) {
                    String Name = JsonArr.getJSONObject(i).getString("name").toString();
                    if (Name.equalsIgnoreCase(fieldName)) {
                        flag = 1;
                        logger.info("flag is {} and Name is {} and i is {}", flag, Name, i);
                        queryId = JsonArr.getJSONObject(i).getInt("id");
                        queryName = JsonArr.getJSONObject(i).getString("queryName").toString();
                        break;
                    }

                }

                assertion.assertTrue(flag == 1, fieldName + " does not exists in DefaultUserListMetaData API");
                int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);

                String payload = "{\"filterMap\":{\"entityTypeId\":" + entityTypeId + ",\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc\",\"filterJson\":{},\"selectedColumns\":[{\"columnId\":" + queryId + ",\"columnQueryName\":\"" + queryName + "\"}]}}";
                logger.info("payload is {}", payload);

                ListRendererListData lisObj = new ListRendererListData();
                lisObj.hitListRendererListData(ListId, payload);
                String ListResponse = lisObj.getListDataJsonStr();

                JSONObject listJson = new JSONObject(ListResponse);
                JSONArray listJsonArray = listJson.getJSONArray("data");

                for (int i = 0; i < listJsonArray.length(); i++) {
                    String columnName = listJsonArray.getJSONObject(i).getJSONObject(String.valueOf(queryId)).getString("columnName");
                    if (!columnName.equalsIgnoreCase(queryName)) {
                        flag1 = 1;
                        break;
                    }
                }
                assertion.assertTrue(flag1 == 0, queryName + " column does not exists in ListData API");
            }
        } catch (Exception e) {
            assertion.assertTrue(false, "Exception while Validating Test. " + e.getMessage());
        }
        assertion.assertAll();
    }

    private String getDynamicMetadataShowResponse(String Id) {
        AdminHelper admobj = new AdminHelper();

        String lastUserName = Check.lastLoggedInUserName;
        String lastUserPassword = Check.lastLoggedInUserPassword;

        admobj.loginWithClientAdminUser();
        String response = executor.get(DynamicMetadataShow.getAPIPath(Id), DynamicMetadataShow.getHeaders()).getResponse().getResponseBody();
        admobj.loginWithUser(lastUserName, lastUserPassword);

        return response;
    }

    private String getDynamicMetadataListResponse() {
        AdminHelper admobj = new AdminHelper();

        String lastUserName = Check.lastLoggedInUserName;
        String lastUserPassword = Check.lastLoggedInUserPassword;

        admobj.loginWithClientAdminUser();
        String response = executor.get(DynamicMetadataList.getApiPath(), DynamicMetadataList.getHeaders()).getResponse().getResponseBody();

        admobj.loginWithUser(lastUserName, lastUserPassword);
        return response;
    }

    @DataProvider(name = "DataProviderfortestC63013")
    public Object[][] DataProviderfortestC63013() {

        return new Object[][]{

                {
                        "contract draft request", "TestDateTimeCDRTest", "TestDateTimeCDRTest", 3507, 20, 1330, 1
                },
                {
                        "contracts", "TestDateTimeContractTest", "TestDateTimeContractTest", 4, 20, 2019, 1
                },
                {
                        "obligations", "TestDateTimeobligationTest", "TestDateTimeobligationTest", 303, 20, 2023, 1
                },
                {
                        "service levels", "TestDateTimeServiceLevelTest", "TestDateTimeServiceLevelTest", 203, 20, 2013, 1
                }
        };
    }

    //C63013 Date Time custom field support for CDR and Contract
    @Test(dataProvider = "DataProviderfortestC63013")
    public void testC63013(String EntityD, String NameD, String LabelD, int HeaderIdD, int HtmlTypeIdD, int ListReportIdD, int _LisReportIdD) {
        CustomAssert assertion = new CustomAssert();

        try {
            String Entity = EntityD;
            String Name = NameD;
            String Label = LabelD;
            int FieldOrder = 8990;
            int EntityId = ConfigureConstantFields.getEntityIdByName(Entity);
            int HeaderId = HeaderIdD;
            int HtmlTypeId = HtmlTypeIdD;
            int ListReportId = ListReportIdD;
            int _ListReportId = _LisReportIdD;

            AdminHelper adminObj = new AdminHelper();
            String lastUserName = Check.lastLoggedInUserName;
            String lastUserPassword = Check.lastLoggedInUserPassword;

            adminObj.loginWithClientAdminUser("naveen_admin", "admin123");

            HashMap<String, String> headersForCreate = DynamicMetadataCreate.getHeaders();
            String apiPath = DynamicMetadataCreate.getAPIPath();

            HashMap<String, String> params = DynamicMetadataCreate.getParameters(Name, Label, FieldOrder, EntityId, HeaderId, HtmlTypeId, ListReportId, _ListReportId);
            Integer responseCode = executor.postMultiPartFormData(apiPath, headersForCreate, params).getResponse().getResponseCode();

            logger.info("Response Code Return is {}", responseCode);
            assertion.assertTrue(responseCode == 302, Name + " has not created for " + Entity + " with Custom Date Time.");
            adminObj.loginWithUser(lastUserName, lastUserPassword);

            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            int fieldId = 0;
            String queryFieldId = "select id from entity_field where alias ='" + Name.trim() + "'";
            List<List<String>> Field = sqlObj.doSelect(queryFieldId);
            if (Field.size() > 0) {
                fieldId = Integer.parseInt(Field.get(0).toString().replace("[", "").replace("]", ""));
            }

            String queryCfpd = "delete from client_field_provisioning_data where field_id =" + fieldId;
            sqlObj.deleteDBEntry(queryCfpd);

            String queryexcel = "delete from excel_columns where entity_field_id=" + fieldId;
            sqlObj.deleteDBEntry(queryexcel);

            String queryEntityClientField = "delete from  entity_client_field  where field_id=" + fieldId;
            sqlObj.deleteDBEntry(queryEntityClientField);

            String querylfg = "delete from link_fields_groups where field_id=" + fieldId;
            sqlObj.deleteDBEntry(querylfg);

            String queryrfm = "delete from request_field_mapping where field_id=" + fieldId;
            sqlObj.deleteDBEntry(queryrfm);

            String querycfp = "delete from client_field_provisioningv2_data where field_id = (select id from entity_field where alias = '" + Name.trim() + "')";
            sqlObj.deleteDBEntry(querycfp);

            String query = "delete from entity_field where alias ='" + Name.trim() + "'";
            int deleteEntries = sqlObj.deleteDBEntry(query);
            logger.info("delete Entry {}", deleteEntries);
            sqlObj.closeConnection();

        } catch (Exception e) {
            assertion.assertTrue(false, "Exception while Validating Test. " + e.getMessage());
        }
        assertion.assertAll();

    }

    //C63096 Date time dynamic field -Report Listing
    @Test
    public void testC63096() {
        CustomAssert assertion = new CustomAssert();

        try {
            String[] entitiesArr = {
                    "contracts",
                    "contract draft request"
            };

            int reportIdForContract = 49;
            int reportIdForCDR = 324;

            for (String entityName : entitiesArr) {
                int reportId = entityName.equalsIgnoreCase("contracts") ? reportIdForContract : reportIdForCDR;
                String fieldName = entityName.equalsIgnoreCase("contracts") ? contractFieldName : cdrFieldName;
                int flag = 0;

                ReportRendererDefaultUserListMetaData ListDataObj = new ReportRendererDefaultUserListMetaData();
                ListDataObj.hitReportRendererDefaultUserListMetadata(reportId);
                String response = ListDataObj.getReportRendererDefaultUserListMetaDataJsonStr();

                JSONObject JsonObj = new JSONObject(response);
                JSONArray JsonArrList = JsonObj.getJSONArray("columns");
                for (int i = 0; i <= JsonArrList.length() - 1; i++) {
                    String name = JsonArrList.getJSONObject(i).get("name").toString();
                    if (fieldName.equalsIgnoreCase(name)) {
                        flag = 1;
                        break;
                    }
                }

                assertion.assertTrue(flag == 1, fieldName + " does  not exist for " + entityName + " Entity in default User List Metadata API. ");
            }

        } catch (Exception e) {
            assertion.assertTrue(false, "Exception while Validating Test. " + e.getMessage());
        }
        assertion.assertAll();

    }

    //C63095 Dynamic Date Time field - Metadata Search
    @Test
    public void testC63095() {
        CustomAssert assertion = new CustomAssert();

        try {
            String[] entitiesArr = {
                    "contracts",
                    "contract draft request"
            };

            for (String entityName : entitiesArr) {
                String fieldName = entityName.equalsIgnoreCase("contracts") ? contractFieldName : cdrFieldName;

                int flag = 0;
                int EntityId = ConfigureConstantFields.getEntityIdByName(entityName);

                MetadataSearch MetaObj = new MetadataSearch();
                String response = MetaObj.hitMetadataSearch(EntityId);

                List<String> AvailableLabels = MetadataSearch.getAllFieldLabels(response);
                for (int i = 0; i < AvailableLabels.size(); i++) {
                    if (AvailableLabels.get(i).equalsIgnoreCase(fieldName)) {
                        flag = 1;
                        break;
                    }
                }

                assertion.assertTrue(flag == 1, fieldName + " does not exists in Metadata Search for Entity " + EntityId);
            }
        } catch (Exception e) {
            assertion.assertTrue(false, "Exception while Validating Test. " + e.getMessage());
        }
        assertion.assertAll();
    }


    //C63027 Dynamic Date Time field - Entity Show Page
    @Test
    public void testC63027() {
        CustomAssert csAssert = new CustomAssert();

        try {
            String[] entitiesArr = {
                    "contracts",
                    "contract draft request"
            };

            for (String entityName : entitiesArr) {
                String fieldName = entityName.equalsIgnoreCase("contracts") ? contractFieldName : cdrFieldName;

                String createResponse;

                if (entityName.equalsIgnoreCase("contracts")) {
                    createResponse = Contract.createContract("contract for date time custom field", true);
                } else {
                    createResponse = ContractDraftRequest.createCDR("cdr for date time custom field", true);
                }

                if (createResponse != null && ParseJsonResponse.validJsonResponse(createResponse)) {
                    JSONObject jsonObj = new JSONObject(createResponse);
                    String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();

                    if (createStatus.equalsIgnoreCase("success")) {
                        int recordId = CreateEntity.getNewEntityId(createResponse, entityName);

                        logger.info("Hitting Show API for {} Id {}", entityName, recordId);
                        int entityTypeId = ConfigureConstantFields.getEntityIdByName(entityName);
                        recordsToDelete.put(entityTypeId, recordId);

                        String showResponse = ShowHelper.getShowResponse(entityTypeId, recordId);

                        if (ParseJsonResponse.validJsonResponse(showResponse)) {
                            Map<String, String> field = ParseJsonResponse.getFieldByLabel(showResponse, fieldName);
                            if (field == null || field.isEmpty()) {
                                csAssert.assertTrue(false, "Couldn't find Field " + fieldName + " in Show Response for Record Id " + recordId +
                                        " of Entity " + entityName);
                            }
                        } else {
                            csAssert.assertTrue(false, "Show API Response for " + entityName + " Id " + recordId + " is an Invalid JSON.");
                        }
                    } else {
                        csAssert.assertTrue(false, "Couldn't create New record for Entity " + entityName);
                    }
                }
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating Test C63027. " + e.getMessage());
        }
        csAssert.assertAll();
    }
}