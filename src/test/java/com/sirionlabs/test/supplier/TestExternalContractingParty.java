package com.sirionlabs.test.supplier;

import com.sirionlabs.api.commonAPI.New;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.helper.ListRenderer.TabListDataHelper;
import com.sirionlabs.helper.ShowHelper;
import com.sirionlabs.helper.entityCreation.CreateEntity;
import com.sirionlabs.helper.entityCreation.ExternalContractingParty;
import com.sirionlabs.helper.entityEdit.EntityEditHelper;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

@Listeners(value = MyTestListenerAdapter.class)
public class TestExternalContractingParty {

    private final static Logger logger = LoggerFactory.getLogger(TestExternalContractingParty.class);

    private int ecpId = -1;
    private int ecpEntityTypeId;
    private int parentSupplierId;
    private String ecpShowResponse;

    @BeforeClass
    public void beforeClass() {
        ecpEntityTypeId = ConfigureConstantFields.getEntityIdByName("externalcontractingparty");
    }

    @AfterClass
    public void afterClass() {
        try {
            logger.info("Deleting ECP having Id {}", ecpId);
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC();
            String query = "delete from contract_entity where id = " + ecpId;
            sqlObj.deleteDBEntry(query);
            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while deleting ECP Id " + ecpId + ". " + e.getMessage());
        }
    }


    /*
    TC-C8520: Verify Creation of External Contracting Party.
     */
    @Test
    public void testC8520() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C8520: Verify Creation of External Contracting Party.");
            //Validate ECP Creation
            logger.info("Creating External Contracting Party using Default Section ");
            String createResponse = ExternalContractingParty.createECP("default");

            if (createResponse == null) {
                throw new SkipException("Couldn't get Create Response for External Contracting Party.");
            }

            if (ParseJsonResponse.validJsonResponse(createResponse)) {
                JSONObject jsonObj = new JSONObject(createResponse);
                String createStatus = jsonObj.getJSONObject("header").getJSONObject("response").getString("status").trim();

                if (createStatus.equalsIgnoreCase("success")) {
                    ecpId = CreateEntity.getNewEntityId(createResponse, "externalcontractingparty");
                } else {
                    csAssert.assertTrue(false, "Couldn't create External Contracting Party due to " + createStatus);
                }
            } else {
                csAssert.assertTrue(false, "Create API Response for External Contracting Party is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C8520. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C8560: Newly Created ECP should display under ECP Tab of Parent Supplier.
     */
    @Test(dependsOnMethods = "testC8520", priority = 1)
    public void testC8560() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C8560: Verify that Newly Created ECP should display under ECP Tab of Parent Supplier.");
            ecpShowResponse = ShowHelper.getShowResponse(ecpEntityTypeId, ecpId);

            if (ParseJsonResponse.validJsonResponse(ecpShowResponse)) {
                JSONObject jsonObj = new JSONObject(ecpShowResponse);
                parentSupplierId = jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("parentEntityId").getInt("values");
                String tabListDataResponse = TabListDataHelper.getTabListDataResponse(1, parentSupplierId, 310);

                if (ParseJsonResponse.validJsonResponse(tabListDataResponse)) {
                    JSONObject tabJsonObj = new JSONObject(tabListDataResponse);
                    JSONArray jsonArr = tabJsonObj.getJSONArray("data");

                    boolean ecpFound = false;

                    if (jsonArr.length() > 0) {
                        for (int i = 0; i < jsonArr.length(); i++) {
                            int idColumnNo = ListDataHelper.getColumnIdFromColumnName(tabListDataResponse, "id");
                            String idValue = jsonArr.getJSONObject(i).getJSONObject(String.valueOf(idColumnNo)).getString("value");
                            int recordId = ListDataHelper.getRecordIdFromValue(idValue);

                            if (recordId == ecpId) {
                                ecpFound = true;
                            }
                        }

                        csAssert.assertTrue(ecpFound, "Expected ECP Id: " + ecpId + " not found in ECP Tab of Parent Supplier Id: " + parentSupplierId);
                    } else {
                        csAssert.assertTrue(false, "No Data found in TabListData API Response for ECP Tab of Supplier Id " + parentSupplierId);
                    }
                } else {
                    csAssert.assertTrue(false, "TabListData API Response for ECP Tab of Supplier Id " + parentSupplierId + " is an Invalid JSON.");
                }
            } else {
                csAssert.assertTrue(false, "Show API Response for ECP Id " + ecpId + " is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C8560. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C8561: Verify Active option while Creating External Contracting Party.
     */
    @Test(dependsOnMethods = "testC8520", priority = 2)
    public void testC8561() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C8561: Verify Active option while Creating External Contracting Party.");

            if (ParseJsonResponse.validJsonResponse(ecpShowResponse)) {
                JSONObject jsonObj = new JSONObject(ecpShowResponse);
                boolean activeValue = jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("active").getBoolean("values");

                if (activeValue) {
                    //Edit ECP and Update Active value to False.
                    EntityEditHelper editHelperObj = new EntityEditHelper();
                    Map<String, String> extraFields = new HashMap<>();
                    extraFields.put("active", "{\"name\":\"active\",\"id\":11302,\"values\":false}");
                    editHelperObj.validateEntityEdit("externalcontractingparty", ecpId, null, extraFields, null,
                            1, false, csAssert);
                } else {
                    csAssert.assertTrue(false, "Active Value is not true for ECP Id " + ecpId);
                }
            } else {
                csAssert.assertTrue(false, "Show API Response for ECP Id " + ecpId + " is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C8561. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C8562: Verify Vendor Contracting Party options in other Entities.
     */
    @Test(dependsOnMethods = "testC8520", priority = 3)
    public void testC8562() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C8562: Verify Vendor Contracting Party options in other Entities.");
            logger.info("Hitting New API for Contract of MSA Type.");

            New newObj = new New();
            newObj.hitNew("contracts", "suppliers", parentSupplierId, "msa");
            String newResponse = newObj.getNewJsonStr();

            if (ParseJsonResponse.validJsonResponse(newResponse)) {
                JSONObject jsonObj = new JSONObject(newResponse);
                JSONArray jsonArr = jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("vendorContractingParty").getJSONObject("options")
                        .getJSONArray("data");

                boolean optionFound = false;

                for (int i = 0; i < jsonArr.length(); i++) {
                    String optionName = jsonArr.getJSONObject(i).getString("name");

                    if (optionName.equalsIgnoreCase("API Automation ECP")) {
                        optionFound = true;
                        break;
                    }
                }

                csAssert.assertTrue(optionFound, "ECP having Name [API Automation ECP] not found in all Options of Vendor Contracting Party.");
            } else {
                throw new SkipException("Couldn't get New API Response for Contract MSA from Supplier Id " + parentSupplierId);
            }
        } catch (SkipException e) {
            throw new SkipException(e.getMessage());
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C8562. " + e.getMessage());
        }
        csAssert.assertAll();
    }


    /*
    TC-C8563: Verify Visibility of ECP in the Vendor Contracting Party drop down.
     */
    @Test(dependsOnMethods = "testC8520", priority = 4)
    public void testC8563() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Starting Test TC-C8563: Verify Visibility of ECP in the Vendor Contracting Party drop down.");

            if (ParseJsonResponse.validJsonResponse(ecpShowResponse)) {
                JSONObject jsonObj = new JSONObject(ecpShowResponse);
                boolean activeValue = jsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("active").getBoolean("values");

                if (activeValue) {
                    //Edit ECP and Update Active value to False.
                    EntityEditHelper editHelperObj = new EntityEditHelper();
                    Map<String, String> extraFields = new HashMap<>();
                    extraFields.put("active", "{\"name\":\"active\",\"id\":11302,\"values\":false}");
                    editHelperObj.validateEntityEdit("externalcontractingparty", ecpId, null, extraFields, null,
                            1, false, false, csAssert);

                    logger.info("Hitting New API for Contract of MSA Type.");

                    New newObj = new New();
                    newObj.hitNew("contracts", "suppliers", parentSupplierId, "msa");
                    String newResponse = newObj.getNewJsonStr();

                    if (ParseJsonResponse.validJsonResponse(newResponse)) {
                        JSONObject newJsonObj = new JSONObject(newResponse);
                        JSONArray jsonArr = newJsonObj.getJSONObject("body").getJSONObject("data").getJSONObject("vendorContractingParty").getJSONObject("options")
                                .getJSONArray("data");

                        boolean optionFound = false;

                        for (int i = 0; i < jsonArr.length(); i++) {
                            String optionName = jsonArr.getJSONObject(i).getString("name");

                            if (optionName.equalsIgnoreCase("API Automation ECP")) {
                                optionFound = true;
                                break;
                            }
                        }

                        csAssert.assertTrue(!optionFound, "ECP having Name [API Automation ECP] found in all Options of Vendor Contracting Party " +
                                "whereas it wasn't supposed to be.");
                    } else {
                        throw new SkipException("Couldn't get New API Response for Contract MSA from Supplier Id " + parentSupplierId);
                    }
                } else {
                    csAssert.assertTrue(false, "Active Value is not true for ECP Id " + ecpId);
                }
            } else {
                csAssert.assertTrue(false, "Show API Response for ECP Id " + ecpId + " is an Invalid JSON.");
            }
        } catch (Exception e) {
            csAssert.assertTrue(false, "Exception while Validating TC-C8563. " + e.getMessage());
        }
        csAssert.assertAll();
    }
}