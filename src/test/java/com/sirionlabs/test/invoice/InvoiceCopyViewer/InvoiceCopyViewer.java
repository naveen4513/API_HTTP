package com.sirionlabs.test.invoice.InvoiceCopyViewer;

import com.sirionlabs.api.auditLogs.AuditLog;
import com.sirionlabs.api.auditlogreporting.AuditLogReportApi;
import com.sirionlabs.api.commonAPI.Comment;
import com.sirionlabs.api.listRenderer.ListRendererListData;
import com.sirionlabs.api.listRenderer.TabListData;
import com.sirionlabs.helper.ListRenderer.DefaultUserListMetadataHelper;
import com.sirionlabs.helper.api.APIResponse;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.dbHelper.AuditLogsDbHelper;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.kafka.common.protocol.types.Field;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.regex.Pattern;

//SIR-4016
public class InvoiceCopyViewer {
    private static final Logger logger = LoggerFactory.getLogger(InvoiceCopyViewer.class);
    private String configFilePath = "src/test/resources/TestConfig/Invoice",configFileName="InvoiceCopyViewerConfig.cfg";

    @Test
    //covers C90476,C90328,C90334
    public void testUpload(){
        /*
        Open listing of invoice and pick one invoice to operate on
        upload the file in the comment section
        - check for the uploaded file in the attachment section
        - check the audit log
          Get the column name of the Invoice Copy Column
        - check for the uploaded file in the listing
        - check for the uploaded file in the show page
        - Check for the document streaming Link content

          Replace the invoice copy on the same invoice
        - Check the invoice copy attachment

         */
        int invoiceId = -1,commentTabId = 65;
        String invoiceCopyQueryName = "invoicecopy";
        CustomAssert customAssert = new CustomAssert();
        String fileName = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"documentfilename"),
                filePath =ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"documentfilepath"),
                uploadFileName=null;
        String commentTabListIdForDocuments = ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"commenttablistidfordocuments");
        int columnIdForIDInvoice=Integer.parseInt(ParseConfigFile.getValueFromConfigFile(configFilePath,configFileName,"columnidforidinvoice")),
                invoiceListId=10;

        try {
            logger.info("Firing List data to extract invoice ID");
            ListRendererListData invoiceListRendererListData = new ListRendererListData();
            int invoiceEntityIdType = 67;
            invoiceListRendererListData.hitListRendererListData(invoiceEntityIdType, 0, 10, "id", "desc", invoiceListId);
            String invoiceListDataResponse = invoiceListRendererListData.getListDataJsonStr(); //Calling dateColumnIds API for Invoice

            JSONObject invoiceListRenderResponseJson = new JSONObject(invoiceListDataResponse);

            ParseJsonResponse parseJsonResponse = new ParseJsonResponse();
            try {
                parseJsonResponse.getNodeFromJsonWithValue(invoiceListRenderResponseJson, Collections.singletonList("columnId"), columnIdForIDInvoice); //Extracting columnIdStatus for the first CDR in dateColumnIds
            } catch (Exception e) {
                logger.error("Exception in extracting value of the dateColumnIds data API");
                customAssert.assertTrue(false, "Cannot extract CDR from cdr listing, hence marking it failed");
                customAssert.assertAll();
            }

            if (parseJsonResponse.getJsonNodeValue() instanceof String)
                invoiceId = Integer.parseInt(((String) parseJsonResponse.getJsonNodeValue()).split(":;")[1]); //Extracting id from the column value of the invoice dateColumnIds
            else {
                logger.error("columnId could not be retrieved correctly");
                customAssert.assertTrue(false, "columnId could not be retrieved correctly");
                customAssert.assertAll();
            }

            logger.info("Got invoice ID : {}",invoiceId);

            //Recording the audit log count
            logger.info("Firing audit log for above invoice to record the count");
            AuditLog auditLog = new AuditLog();
            APIResponse apiResponse = auditLog.hitAuditLogDataApi(String.valueOf(invoiceEntityIdType),String.valueOf(invoiceId),10,20,"id","desc");
            String auditLogResponse = apiResponse.getResponseBody();

            JSONObject auditLogResponseJson = new JSONObject(auditLogResponse);
            int auditLogInitCount = auditLogResponseJson.getInt("filteredCount");


            logger.info("Firing tab list API for getting count of the number of comments");

            TabListData tabListData = new TabListData();
            String tabListResponse = tabListData.hitTabListData(commentTabId, invoiceEntityIdType,invoiceId);
            JSONObject tabListJson = new JSONObject(tabListResponse);
            int initialLength = tabListJson.getJSONArray("data").length();

            //File oldFile = new File(filePath+"/"+fileName);
            uploadFileName = RandomString.getRandomAlphaNumericString(10) +"."+fileName.split("[.]")[1];

            logger.info("Create new file for upload with name {}",uploadFileName);
            //File uploadFile = new File(filePath+"/"+uploadFileName);

            assert FileUtils.copyFile(filePath,fileName,filePath,uploadFileName):"OLD file not copied to NEW file";

            //assert oldFile.renameTo(uploadFile):"Old file not renamed to new file";

            logger.info("Firing Upload file + Submit draft API");
            Comment comment = new Comment();
            String payload = comment.createCommentPayload(invoiceEntityIdType,invoiceId,null,null,"<p>test comment</p>",null,null,null,null,null,null,null,null,true,uploadFileName,filePath);
            JSONObject jsonObjectPayload = new JSONObject(payload);
            removeOptionsFields(jsonObjectPayload,"options");
            String invoiceEntityName = "invoices";
            String commentResponse = comment.hitComment(invoiceEntityName,jsonObjectPayload.toString());

            if(!commentResponse.contains("success")){
                logger.error("Comment upload failed. Response doesn't contain success.");
                customAssert.assertTrue(false,"Comment upload failed. Response doesn't contain success.");
                customAssert.assertAll();
            }

            logger.info("File uploaded successfully");

            logger.info("Checking uploaded attachment");

            tabListData = new TabListData();
            tabListResponse = tabListData.hitTabListData(commentTabId, invoiceEntityIdType,invoiceId);
            tabListJson = new JSONObject(tabListResponse);
            boolean invoiceCopyFound = false;
            if(tabListJson.getJSONArray("data").length()>initialLength){
                JSONArray commentsJsonArray = tabListJson.getJSONArray("data");
                for(Object commentObj : commentsJsonArray){
                    parseJsonResponse=new ParseJsonResponse();
                    parseJsonResponse.getNodeFromJsonWithValue((JSONObject)commentObj,Collections.singletonList("columnName"),"invoicecopy");
                    Object foundObj = parseJsonResponse.getJsonNodeValue();
                    if(foundObj==JSONObject.NULL)
                        continue;
                    assert foundObj instanceof String:"The tab list column value of invoice copy is not string";
                    if(((String)foundObj).equalsIgnoreCase("true")){
                        invoiceCopyFound=true;
                        parseJsonResponse.getNodeFromJsonWithValue((JSONObject)commentObj,Collections.singletonList("columnName"),"document");
                        foundObj = parseJsonResponse.getJsonNodeValue();
                        assert foundObj instanceof String:"The tab list column value of document is not string";
                        if(!((String)foundObj).contains(uploadFileName.split("[.]")[0])){
                            logger.info("Found the uploaded comment but could not find attachment");
                            logger.error("Cannot find file/document after commenting attachment.");
                            customAssert.assertTrue(false,"Cannot find file/document after commenting attachment.");
                            customAssert.assertAll();return;
                        }
                    }
                }
            }

            assert invoiceCopyFound:"Invoice copy comment/attachment not found";

            logger.info("Attachment is found.");
            logger.info("Checking Audit log");


            //checking audit log

            auditLog = new AuditLog();
            apiResponse = auditLog.hitAuditLogDataApi(String.valueOf(invoiceEntityIdType),String.valueOf(invoiceId),0,20,"id","desc");
            auditLogResponse = apiResponse.getResponseBody();

            auditLogResponseJson = new JSONObject(auditLogResponse);
            assert auditLogInitCount<auditLogResponseJson.getInt("filteredCount"):"Audit log count didn't increase after adding invoice attachment";
            JSONArray auditLogResponseArrayTemp = auditLogResponseJson.getJSONArray("data");
            assert auditLogResponseArrayTemp.length()>0:"Audit log api data node is of "+auditLogResponseArrayTemp.length()+" length";

            for(Object object : auditLogResponseArrayTemp){
                JSONObject auditLogResponseObjectTemp = (JSONObject)object;

                parseJsonResponse = new ParseJsonResponse();
                parseJsonResponse.getNodeFromJsonWithValue(auditLogResponseObjectTemp,Collections.singletonList("columnName"),"comment");
                Object topAuditLogType = parseJsonResponse.getJsonNodeValue();
                assert topAuditLogType instanceof String:"Top audit log type is not string type";
                String typeValue = (String) topAuditLogType;
//                assert typeValue.equalsIgnoreCase("yes"):"Top audit log value is not comment=yes";
                assert Pattern.compile("yes", Pattern.CASE_INSENSITIVE).matcher(typeValue.toLowerCase()).find():"Top audit log value is not comment=yes";

                break;
            }


            logger.info("Audit log checked, working fine.");


            //Getting all the column types
            DefaultUserListMetadataHelper defaultUserListMetadataHelper = new DefaultUserListMetadataHelper();
            String defaultUserListMetadataHelperResponse = defaultUserListMetadataHelper.getDefaultUserListMetadataResponse("invoices");

            JSONObject defaultUserListMetadataHelperResponseJson = new JSONObject(defaultUserListMetadataHelperResponse);
            JSONArray jsonArrayTemp = defaultUserListMetadataHelperResponseJson.getJSONArray("columns");

            int invoiceCopyColumnId = -1;
            for(Object object : jsonArrayTemp){
                JSONObject jsonObjectTemp = (JSONObject)object;
                if(jsonObjectTemp.getString("queryName").equalsIgnoreCase(invoiceCopyQueryName)){
                    invoiceCopyColumnId = jsonObjectTemp.getInt("id");
                    break;
                }
            }

            assert invoiceCopyColumnId !=-1:"Cannot find Invoice Copy column id";


            //Listing check

            logger.info("Firing List data to extract invoice ID");
            invoiceListRendererListData = new ListRendererListData();
            //invoiceListRendererListData.hitListRendererListData(invoiceEntityIdType, 10, 10, "id", "desc", invoiceListId);
            invoiceListRendererListData.hitListRendererListData(invoiceListId,getInvoiceListingPayloadForInvoiceCopyColumn(invoiceCopyColumnId));
            invoiceListDataResponse = invoiceListRendererListData.getListDataJsonStr(); //Calling listing API for Invoice

            invoiceListRenderResponseJson = new JSONObject(invoiceListDataResponse);

            String invoiceCopyColumnValue=null;

            for(Object childObject : invoiceListRenderResponseJson.getJSONArray("data")){
                parseJsonResponse = new ParseJsonResponse();
                try {
                    parseJsonResponse.getNodeFromJsonWithValue((JSONObject) childObject, Collections.singletonList("columnId"), 203);

                    assert parseJsonResponse.getJsonNodeValue() instanceof String:"Column id of invoice id couldn't be extracted";

                    if(((String)parseJsonResponse.getJsonNodeValue()).contains(String.valueOf(invoiceId))) {
                        parseJsonResponse.getNodeFromJsonWithValue((JSONObject) childObject, Collections.singletonList("columnId"), invoiceCopyColumnId); //Extracting columnIdStatus for the first CDR in dateColumnIds
                        break;
                    }
                } catch (Exception e) {
                    logger.error("Exception in extracting value of the ColumnIds data API");
                    customAssert.assertTrue(false, "Cannot extract Invoice from invoice listing, hence marking it failed");
                    customAssert.assertAll();
                }
            }

            if (parseJsonResponse.getJsonNodeValue() instanceof String)
                invoiceCopyColumnValue = ((String) parseJsonResponse.getJsonNodeValue());//.split(":;")[1]; //Extracting id from the column value of the invoice dateColumnIds
            else {
                logger.error("columnId could not be retrieved correctly");
                customAssert.assertTrue(false, "columnId could not be retrieved correctly");
                customAssert.assertAll();return;
            }

            logger.info("Got invoice Copy column value : {}",invoiceCopyColumnValue);
            assert invoiceCopyColumnValue.contains(uploadFileName);

            logger.info("The uploaded invoice copy is found in the listing");


            //Checking getInvoiceCopyViewerLink
            APIValidator apiValidator = new com.sirionlabs.api.invoice.InvoiceCopyViewer().getInvoiceCopyViewerLink(invoiceId);
            apiResponse = apiValidator.getResponse();
            String linkResponseBody = apiResponse.getResponseBody();

            assert ParseJsonResponse.validJsonResponse(linkResponseBody):"getInvoiceCopyViewerLink returned invalid json";

            JSONObject linkJsonObj = new JSONObject(linkResponseBody);
            try {

                Object documentStreamUrl = linkJsonObj.get("data");
                assert documentStreamUrl!=null:"Cannot find data in document stream URL";
            }
            catch (Exception e){
                assert false:"Incorrect data in documentStreamUrl of getInvoiceCopyViewerLink API ["+linkJsonObj+"]";
            }

            FileUtils.deleteFile(filePath,uploadFileName);

            logger.info("Replacing the existing Invoice Copy");
            logger.info("Firing tab list API for getting count of the number of comments");

            tabListData = new TabListData();
            tabListResponse = tabListData.hitTabListData(commentTabId, invoiceEntityIdType,invoiceId);
            tabListJson = new JSONObject(tabListResponse);
            initialLength = tabListJson.getJSONArray("data").length();

            //File oldFile = new File(filePath+"/"+fileName);
            uploadFileName = RandomString.getRandomAlphaNumericString(10) +"."+fileName.split("[.]")[1];

            logger.info("Create new file again for replacing with name {}",uploadFileName);
            //File uploadFile = new File(filePath+"/"+uploadFileName);

            assert FileUtils.copyFile(filePath,fileName,filePath,uploadFileName):"OLD file not copied to NEW file";

            //assert oldFile.renameTo(uploadFile):"Old file not renamed to new file";

            logger.info("Firing Upload file + Submit draft API");
            comment = new Comment();
            payload = comment.createCommentPayload(invoiceEntityIdType,invoiceId,null,null,"<p>test comment</p>",null,null,null,null,null,null,null,null,true,uploadFileName,filePath);
            jsonObjectPayload = new JSONObject(payload);
            removeOptionsFields(jsonObjectPayload,"options");
            commentResponse = comment.hitComment(invoiceEntityName,jsonObjectPayload.toString());

            if(!commentResponse.contains("success")){
                logger.error("Comment upload failed. Response doesn't contain success.");
                customAssert.assertTrue(false,"Comment upload failed. Response doesn't contain success.");
                customAssert.assertAll();
            }

            logger.info("File uploaded successfully 2nd time");

            logger.info("Checking uploaded attachment");

            tabListData = new TabListData();
            tabListResponse = tabListData.hitTabListData(commentTabId, invoiceEntityIdType,invoiceId);
            tabListJson = new JSONObject(tabListResponse);
            invoiceCopyFound = false;
            if(tabListJson.getJSONArray("data").length()>initialLength){
                JSONArray commentsJsonArray = tabListJson.getJSONArray("data");
                for(Object commentObj : commentsJsonArray){
                    parseJsonResponse.getNodeFromJsonWithValue((JSONObject)commentObj,Collections.singletonList("columnName"),"invoicecopy");
                    Object foundObj = parseJsonResponse.getJsonNodeValue();
                    if(foundObj==JSONObject.NULL)
                        continue;
                    assert foundObj instanceof String:"The tab list column value of invoice copy is not string";
                    if(((String)foundObj).equalsIgnoreCase("true")){
                        invoiceCopyFound=true;
                        parseJsonResponse.getNodeFromJsonWithValue((JSONObject)commentObj,Collections.singletonList("columnName"),"document");
                        foundObj = parseJsonResponse.getJsonNodeValue();
                        assert foundObj instanceof String:"The tab list column value of document is not string";
                        if(!((String)foundObj).contains(uploadFileName.split("[.]")[0])){
                            logger.info("Found the uploaded comment but could not find attachment");
                            logger.error("Cannot find file/document after commenting attachment.");
                            customAssert.assertTrue(false,"Cannot find file/document after commenting attachment.");
                            customAssert.assertAll();
                        }
                    }
                }

            }

            assert invoiceCopyFound:"Invoice copy comment/attachment not found after replacing";

            logger.info("Attachment is found.");

            FileUtils.deleteFile(filePath,uploadFileName);

        }
        catch (Exception e){
            logger.error("Uploading comment failed with Exception");
            customAssert.assertTrue(false,"Uploading comment failed with Exception");
            customAssert.assertAll();
        }

        customAssert.assertAll();
    }


    private void removeOptionsFields(JSONObject jsonObject, String key) {
        Iterator<String> iterator = jsonObject.keys();
        String nextKey;
        while (iterator.hasNext()) {
            nextKey = iterator.next();
            if (key.equalsIgnoreCase(nextKey)) {
                    jsonObject.put(key,JSONObject.NULL);
            }
            else {
                if (jsonObject.get(nextKey) instanceof JSONObject) {
                    removeOptionsFields((JSONObject) jsonObject.get(nextKey),key);
                } else if (jsonObject.get(nextKey) instanceof JSONArray)
                    removeOptionsFields((JSONArray) jsonObject.get(nextKey),key);
            }
        }
    }

    private void removeOptionsFields(JSONArray jsonArray,String key) {

        for (int index = 0; index < jsonArray.length(); index++) {
            if (jsonArray.get(index) instanceof JSONArray)
                removeOptionsFields((JSONArray) jsonArray.get(index),key);
            else if (jsonArray.get(index) instanceof JSONObject)
                removeOptionsFields((JSONObject) jsonArray.get(index),key);
        }
    }

    private String getInvoiceListingPayloadForInvoiceCopyColumn(int columnId){
        return "{ \"filterMap\": { \"entityTypeId\": 67, \"offset\": 0, \"size\": 20, \"orderByColumnName\": \"id\", \"orderDirection\": \"desc nulls last\", \"filterJson\": {} }, \"selectedColumns\": [ { \"columnId\": 203, \"columnQueryName\": \"id\" }, { \"columnId\": "+columnId+", \"columnQueryName\": \"invoicecopy\" } ] }";
    }
}
