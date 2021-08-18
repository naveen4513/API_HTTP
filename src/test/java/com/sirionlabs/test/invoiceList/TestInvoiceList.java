package com.sirionlabs.test.invoiceList;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.sirionlabs.api.IntegrationListing.IntegrationListingGetTypes;
import com.sirionlabs.api.commonAPI.Edit;
import com.sirionlabs.api.commonAPI.Show;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.test.IntegrationListing.TestGetListTypes;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.ParseException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


import javax.swing.text.StyledEditorKit;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TestInvoiceList {

    private final static Logger logger = LoggerFactory.getLogger(TestInvoiceList.class);
    private static String configFilePath = null;
    private static String configFileName = null;
    private static Integer invListId = -1;
    private static Integer paginationListDataSize = 20;
    private static Integer fieldsValidationListDataOffset = 0;
    private static Integer fieldsValidationListDataSize = 20;
    private static Integer maxNoOfRecordsToValidate = 5;
    private static Integer invEntityTypeId = -1;
    public  static Map<String, List<String>> fieldmapping = new HashMap<>();
    @BeforeClass
    public void beforeClass() throws ConfigurationException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("InvoiceListTestConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("InvoiceListTestConfigFileName");
        invListId = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFilePath"),
                ConfigureConstantFields.getConstantFieldsProperty("EntityIdMappingFile"), "invoices", "entity_url_id"));

        String temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "paginationListDataSize");
        if (temp != null && NumberUtils.isParsable(temp.trim()))
            paginationListDataSize = Integer.parseInt(temp);

        invEntityTypeId = ConfigureConstantFields.getEntityIdByName("invoices");

        temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fieldsValidationListDataOffset");
        if (temp != null && NumberUtils.isParsable(temp.trim()))
            fieldsValidationListDataOffset = Integer.parseInt(temp.trim());

        temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "fieldsValidationListDataSize");
        if (temp != null && NumberUtils.isParsable(temp.trim()))
            fieldsValidationListDataSize = Integer.parseInt(temp.trim());

        temp = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "maxRecordstoValidate");
        if (temp != null && NumberUtils.isParsable(temp.trim()))
            maxNoOfRecordsToValidate = Integer.parseInt(temp.trim());
    }

    @Test(priority = 0,enabled = true)
    public void testInvoiceListingPagination() {
        CustomAssert csAssert = new CustomAssert();

        try {
            logger.info("Validating Invoice Listing Pagination");

            //Validate Listing Pagination
            ListDataHelper.verifyListingPagination(invEntityTypeId, invListId, paginationListDataSize, csAssert);
        } catch (Exception e) {
            logger.error("Exception while Validating Invoice Pagination. {}", e.getMessage());
            csAssert.assertTrue(false, "Exception while Validating Invoices Pagination. " + e.getMessage());
        }
        csAssert.assertAll();
    }

    @Test(enabled = true)
    public void testInvoiceShowPageFieldsGeneralTab(){

        CustomAssert csAssert = new CustomAssert();
        logger.info("Validating show page fields for general tab");
        try {
            Show show = new Show();
            int showpageid = Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"showpageidtocheck"));
            show.hitShow(invEntityTypeId, showpageid);//67
            String showresponse = show.getShowJsonStr();

            JSONObject jobj;
            JSONArray jarray = new JSONObject(showresponse).getJSONObject("body").getJSONObject("layoutInfo").getJSONObject("layoutComponent").getJSONArray("fields");
            JSONArray jarray2 = new JSONArray();
            JSONObject jobj2;

            List<String> val;
            String label;
            String key;
            String str;
            String values[];

            for (int i = 0; i < jarray.length(); i++) {
                jobj = jarray.getJSONObject(i);
                if (jobj.getString("label").equals("GENERAL")) {

                    jarray2 = jobj.getJSONArray("fields");
                    break;
                }
            }

            for (int i = 0; i < jarray2.length(); i++) {
                jobj2 = jarray2.getJSONObject(i);
                if(jobj2.has("label")) {
                    label = jobj2.getString("label");
                    fieldmapping = getdisplayfieldsunderlabel(jobj2, label);
                }
            }
            int j=0;
            for (Map.Entry<String, List<String>> pair : fieldmapping.entrySet()) {
                val = pair.getValue();
                key = pair.getKey();
                str = val.toString();

                values = (ParseConfigFile.getValueFromConfigFileCaseSensitive(configFilePath,configFileName,"invoicegeneraltabfields",key)).split(",");

                while (j < values.length){
                    if(str.contains(values[j])){
                        csAssert.assertTrue(true,"Expected Field " + values[j] + "present under: " + key);
                        logger.info("Expected Field {} present under: {}",values[j],key);
                    }else {
                        csAssert.assertTrue(false,"Expected Field " + values[j] + "present under: " + key);
                        logger.error("Extra Field {} present under: {}",values[j],key);
                    }

                    j++;
                }
            }

        }catch (Exception e){
            logger.error("Exception while Validating Purchase Order Listing Data Fields. {}", e.getMessage());
        }
    }

    public Map<String,List<String>> getdisplayfieldsunderlabel(JSONObject jobj,String label) {

        List<String> field = new ArrayList<>();
        String newlabel;
        JSONObject json1;
        JSONArray jsonArray;
        if (jobj.has("fields")) {

            jsonArray = jobj.getJSONArray("fields");
            for (int i = 0; i < jsonArray.length(); i++) {
                json1 = jsonArray.getJSONObject(i);
                if (json1.has("label")) {
                    newlabel = json1.getString("label");
                    field.add(newlabel);
                    if (json1.has("fields")) {
                        fieldmapping = getdisplayfieldsunderlabel(json1, newlabel);
                    }
                    fieldmapping.put(label, field);
                }
                else {
                    newlabel = label;
                    if (json1.has("fields")) {
                        fieldmapping = getdisplayfieldsunderlabel(json1, newlabel);
                    }
                }
            }
        }
        else {
            field.add(jobj.getString("label"));
            fieldmapping.put(label, field);
        }
        return fieldmapping;
    }

}
