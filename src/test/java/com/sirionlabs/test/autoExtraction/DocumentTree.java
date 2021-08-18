package com.sirionlabs.test.autoExtraction;

import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.autoextraction.AutoExtractionHelper;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.ParseConfigFile;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;

public class DocumentTree extends TestAPIBase {
    private final static Logger logger = LoggerFactory.getLogger(DocumentTree.class);
    CustomAssert csAssert = new CustomAssert();
    int referencesInTree;
    int parentReferenceFieldOptions;
    static String docId;
    static String configAutoExtractionFilePath;
    static String configAutoExtractionFileName;

    @BeforeClass
    public void beforeClass() {
        CustomAssert csAssert = new CustomAssert();

            configAutoExtractionFilePath = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFilePath");
            configAutoExtractionFileName = ConfigureConstantFields.getConstantFieldsProperty("AutoExtractionConfigFileName");
            docId = ParseConfigFile.getValueFromConfigFileCaseSensitive(configAutoExtractionFilePath, configAutoExtractionFileName, "ae metadata Info","recordid");

    }

    public String docTreeAPI()throws IOException
    {
        HttpResponse docTreeAPIResponse = AutoExtractionHelper.docTreeAPI(docId);
        csAssert.assertTrue(docTreeAPIResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
        String docTreeStr = EntityUtils.toString(docTreeAPIResponse.getEntity());
        return docTreeStr;
    }

    /*Test Case to validate list of documents is being shown in Doc Tree(Hierarchical View) */

    @Test(priority = 1)
    public void testDocumentTree() throws IOException
    {
        CustomAssert csAssert = new CustomAssert();
        try
        {
            logger.info("Hitting the Doc Tree API to check whether it is showing data in document Tree or not:" + "Test Case Id: C152986");
            DocumentTree obj = new DocumentTree();
            String docTreeStr = obj.docTreeAPI();
            JSONArray docTreeArray = new JSONArray(docTreeStr);
            referencesInTree = docTreeArray.length();
            csAssert.assertTrue(referencesInTree>0,"No document has been found in Doc Tree of :" + " " + "Document Id" + docId + " " +
                    "Total References found:" + " " + referencesInTree);
        }
        catch (Exception e)
        {
            csAssert.assertTrue(false, "Document Tree API is not working because of:" + e.getMessage());

        }
        csAssert.assertAll();
    }

    /*Test Case to validate all the documents that are present in Doc Tree should be present as an Option n Parent References field */

    @Test(priority = 2)
    public void parentReferenceFieldOptions() throws IOException
    {
        CustomAssert csAssert = new CustomAssert();
        try {
            logger.info("Validating the list of Options in Parent references should be same as in Document Tree"+ "Test Case Id: C152989");
            HttpResponse fieldOptionsResponse = AutoExtractionHelper.parentReferenceFieldOptions(docId);
            csAssert.assertTrue(fieldOptionsResponse.getStatusLine().getStatusCode()==200,"Response Code is Invalid");
            String fieldOptionsStr = EntityUtils.toString(fieldOptionsResponse.getEntity());
            JSONObject fieldOptionsObj = new JSONObject(fieldOptionsStr);
            parentReferenceFieldOptions = (int) fieldOptionsObj.get("size");
            /*Deleting one value in referencesInTree because the source document(on which user is currently present will not be there in options*/
            csAssert.assertEquals(referencesInTree-1,parentReferenceFieldOptions,"All the documents that are present in Doc tree is not there is parent field options" + " "+
                    "Parent Reference field value is:" + parentReferenceFieldOptions + " References in Tree value is:" +referencesInTree);

        }
        catch (Exception e)
        {
            csAssert.assertTrue(false, "Parent References Field Options API is getting failed because of" + e.getMessage());

        }
        csAssert.assertAll();
    }

    /*Test Case to validate all the documents in Doc Tree should have a status i.e., It should be an assigned Document */
    @Test(priority = 3)
    public void checkDocumentsInTreeAreAssigned() throws IOException
    {
        CustomAssert csAssert = new CustomAssert();
        try {
            logger.info("Hitting the Doc Tree API to check whether it is showing data in document Tree or not" + " " + "Test Case Id:C152990");
            DocumentTree obj = new DocumentTree();
            String docTreeStr = obj.docTreeAPI();
            JSONArray docTreeArray = new JSONArray(docTreeStr);
            int totalEntries = docTreeArray.length();

            for(int i=0;i<totalEntries;i++)
            {
                String workFlowStatus = (String) docTreeArray.getJSONObject(i).get("status");
                int documentId = (int) docTreeArray.getJSONObject(i).get("documentId");
                csAssert.assertTrue(!workFlowStatus.equalsIgnoreCase(null),"Workflow Status is blank for DocumentID" +documentId);
            }

        }
        catch (Exception e)
        {
            csAssert.assertTrue(false, "Doc Tree Hierarchical View API is getting failed because of:" + e.getMessage());

        }
        csAssert.assertAll();
    }

}
