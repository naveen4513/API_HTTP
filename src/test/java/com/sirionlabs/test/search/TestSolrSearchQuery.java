package com.sirionlabs.test.search;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.api.metadataSearch.MetadataSearch;
import com.sirionlabs.api.metadataSearch.Search;
import com.sirionlabs.api.search.SearchAttachment;
import com.sirionlabs.api.search.SearchContractDoc;
import com.sirionlabs.config.ConfigureConstantFields;
import com.sirionlabs.helper.dbHelper.EmailActionDbHelper;
import com.sirionlabs.helper.testRail.TestRailBase;
import com.sirionlabs.utils.RetryListener.MyTestListenerAdapter;
import com.sirionlabs.utils.commonUtils.*;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by nitun.pachauri on 23/04/2020.
 */
@Listeners(value = MyTestListenerAdapter.class)
public class TestSolrSearchQuery extends TestRailBase {

    private final static Logger logger = LoggerFactory.getLogger(TestSolrSearchQuery.class);
    private static String configFilePath = null;
    private static String configFileName = null;
    private int entityTypeId = 61;

    @BeforeClass()
    public void beforeClass() throws ConfigurationException {
        configFilePath = ConfigureConstantFields.getConstantFieldsProperty("MicroserviceConfigFilePath");
        configFileName = ConfigureConstantFields.getConstantFieldsProperty("MicroserviceConfigFileName");
    }

    // C141330 - AND check
    @Test(enabled = true)
    public void testC141330() {
        CustomAssert csAssert = new CustomAssert();

        // Search attachment check
        boolean first = false, second = false, isFound = false;
        String keyword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "solrquerysearchattach", "keyword");
        keyword = keyword.substring(1,keyword.length()-1);

        String[] keywords = keyword.split(",");
        String keywordAlpha = getKeyWordsWithSymbol(keywords, "AND");
        String keywordSymbol = getKeyWordsWithSymbol(keywords, "&&");

        // AND check
        SearchAttachment attachObj = new SearchAttachment();
        attachObj.hitAttachment(keywordAlpha, entityTypeId);
        String attachmentJsonStr1 = attachObj.getAttachmentJsonStr();

        // && check
        attachObj = new SearchAttachment();
        attachObj.hitAttachment(keywordSymbol, entityTypeId);
        String attachmentJsonStr2 = attachObj.getAttachmentJsonStr();

        csAssert.assertEquals(attachmentJsonStr1,attachmentJsonStr2, "Resultset doesn't match for Logical and Alphabetical operator");

        JSONObject jsonObj = new JSONObject(attachmentJsonStr1);
        JSONArray arr = jsonObj.getJSONArray("searchResults");
        for(int i = 0 ; i < arr.length() ; i++){
            JSONObject obj2 = arr.getJSONObject(i);
            csAssert.assertEquals(obj2.getInt("entityTypeId"), entityTypeId);
            JSONArray arr2 = obj2.getJSONArray("highlightedSnippets");
            for(int j = 0 ; j < arr2.length() ; j++){
                String temp = arr2.getString(j);
                if(temp.toLowerCase().contains(keywords[0].trim().substring(1,keywords[0].trim().length()-1).toLowerCase())) first = true;
                if(temp.toLowerCase().contains(keywords[1].trim().substring(1,keywords[1].trim().length()-1).toLowerCase())) second = true;
                if(first && second) {
                    isFound = true;
                    first = false;
                    second = false;
                    break;
                }
            }
            if(!isFound){
                csAssert.fail("Keywords were not found in the resultset");
            }
        }


        // Contract Document test
        isFound = false;
        keyword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "solrquerycontractdoc", "keyword");
        keyword = keyword.substring(1,keyword.length()-1);

        keywords = keyword.split(",");
        keywordAlpha = getKeyWordsWithSymbol(keywords, "AND");
        keywordSymbol = getKeyWordsWithSymbol(keywords, "&&");

        // AND check
        SearchContractDoc docTreeObj = new SearchContractDoc();
        docTreeObj.hitSearchContractDoc(keywordAlpha);
        String docTreeJsonStr1 = docTreeObj.getContractDocJsonStr();

        // && check
        docTreeObj = new SearchContractDoc();
        docTreeObj.hitSearchContractDoc(keywordSymbol);
        String docTreeJsonStr2 = docTreeObj.getContractDocJsonStr();

        csAssert.assertEquals(docTreeJsonStr1, docTreeJsonStr2, "Resultset doesn't match for Logical and Alphabetical operator");

        jsonObj = new JSONObject(docTreeJsonStr1);
        arr = jsonObj.getJSONArray("searchResults");
        for(int i = 0 ; i < arr.length() ; i++){
            JSONObject obj2 = arr.getJSONObject(i);
            JSONArray arr2 = obj2.getJSONArray("highlightedSnippets");
            for(int j = 0 ; j < arr2.length() ; j++){
                String temp = arr2.getString(j);
                if(temp.toLowerCase().contains(keywords[0].trim().substring(1,keywords[0].trim().length()-1).toLowerCase())) first = true;
                if(temp.toLowerCase().contains(keywords[1].trim().substring(1,keywords[1].trim().length()-1).toLowerCase())) second = true;
                if(first && second) {
                    isFound = true;
                    first = false;
                    second = false;
                    break;
                }
            }
            if(!isFound){
                csAssert.fail("Keywords were not found in the resultset");
            }
        }





        csAssert.assertAll();
    }

    // C90841 - OR check
    @Test(enabled = true)
    public void testC90841() {
        CustomAssert csAssert = new CustomAssert();

        // Search attachment check
        boolean first = false, second = false, isFound = false;
        String keyword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "solrquerysearchattach", "keyword");
        keyword = keyword.substring(1,keyword.length()-1);

        String[] keywords = keyword.split(",");
        String keywordAlpha = getKeyWordsWithSymbol(keywords, "OR");
        String keywordSymbol = getKeyWordsWithSymbol(keywords, "||");

        // OR check
        SearchAttachment attachObj = new SearchAttachment();
        attachObj.hitAttachment(keywordAlpha, entityTypeId);
        String attachmentJsonStr1 = attachObj.getAttachmentJsonStr();

        // || check
        attachObj = new SearchAttachment();
        attachObj.hitAttachment(keywordSymbol, entityTypeId);
        String attachmentJsonStr2 = attachObj.getAttachmentJsonStr();

        csAssert.assertEquals(attachmentJsonStr1,attachmentJsonStr2, "Resultset doesn't match for Logical and Alphabetical operator");

        JSONObject jsonObj = new JSONObject(attachmentJsonStr1);
        JSONArray arr = jsonObj.getJSONArray("searchResults");
        for(int i = 0 ; i < arr.length() ; i++){
            JSONObject obj2 = arr.getJSONObject(i);
            csAssert.assertEquals(obj2.getInt("entityTypeId"), entityTypeId);
            JSONArray arr2 = obj2.getJSONArray("highlightedSnippets");
            for(int j = 0 ; j < arr2.length() ; j++){
                String temp = arr2.getString(j);
                if(temp.toLowerCase().contains(keywords[0].trim().substring(1,keywords[0].trim().length()-1).toLowerCase())) first = true;
                if(temp.toLowerCase().contains(keywords[1].trim().substring(1,keywords[1].trim().length()-1).toLowerCase())) second = true;
                if(first || second) {
                    isFound = true;
                    first = false;
                    second = false;
                    break;
                }
            }
            if(!isFound){
                csAssert.fail("Keywords were not found in the resultset");
            }
        }

        // Contract Document test
        isFound = false;
        keyword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "solrquerycontractdoc", "keyword");
        keyword = keyword.substring(1,keyword.length()-1);

        keywords = keyword.split(",");
        keywordAlpha = getKeyWordsWithSymbol(keywords, "OR");
        keywordSymbol = getKeyWordsWithSymbol(keywords, "||");

        // OR check
        SearchContractDoc docTreeObj = new SearchContractDoc();
        docTreeObj.hitSearchContractDoc(keywordAlpha);
        String docTreeJsonStr1 = docTreeObj.getContractDocJsonStr();

        // || check
        docTreeObj = new SearchContractDoc();
        docTreeObj.hitSearchContractDoc(keywordSymbol);
        String docTreeJsonStr2 = docTreeObj.getContractDocJsonStr();

        csAssert.assertEquals(docTreeJsonStr1, docTreeJsonStr2, "Resultset doesn't match for Logical and Alphabetical operator");

        jsonObj = new JSONObject(attachmentJsonStr1);
        arr = jsonObj.getJSONArray("searchResults");
        for(int i = 0 ; i < arr.length() ; i++){
            JSONObject obj2 = arr.getJSONObject(i);
            JSONArray arr2 = obj2.getJSONArray("highlightedSnippets");
            for(int j = 0 ; j < arr2.length() ; j++){
                String temp = arr2.getString(j);
                if(temp.toLowerCase().contains(keywords[0].trim().substring(1,keywords[0].trim().length()-1).toLowerCase())) first = true;
                if(temp.toLowerCase().contains(keywords[1].trim().substring(1,keywords[1].trim().length()-1).toLowerCase())) second = true;
                if(first || second) {
                    isFound = true;
                    first = false;
                    second = false;
                    break;
                }
            }
            if(!isFound){
                csAssert.fail("Keywords were not found in the resultset");
            }
        }
        csAssert.assertAll();
    }

    // C141331 - NOT check
    @Test(enabled = true)
    public void testC141331Not() {
        CustomAssert csAssert = new CustomAssert();

        // Search attachment check
        boolean first = false, second = false, isFound = false;
        String keyword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "solrquerysearchattach", "keyword");
        keyword = keyword.substring(1,keyword.length()-1);

        String[] keywords = keyword.split(",");
        String keywordAlpha = getKeyWordsWithSymbol(keywords, "NOT");
        String keywordSymbol = getKeyWordsWithSymbol(keywords, "!");

        // NOT check
        SearchAttachment attachObj = new SearchAttachment();
        attachObj.hitAttachment(keywordAlpha, entityTypeId);
        String attachmentJsonStr1 = attachObj.getAttachmentJsonStr();

        // ! check
        attachObj = new SearchAttachment();
        attachObj.hitAttachment(keywordSymbol, entityTypeId);
        String attachmentJsonStr2 = attachObj.getAttachmentJsonStr();

        csAssert.assertEquals(attachmentJsonStr1,attachmentJsonStr2, "Resultset doesn't match for Logical and Alphabetical operator");

        JSONObject jsonObj = new JSONObject(attachmentJsonStr1);
        JSONArray arr = jsonObj.getJSONArray("searchResults");
        for(int i = 0 ; i < arr.length() ; i++){
            JSONObject obj2 = arr.getJSONObject(i);
            csAssert.assertEquals(obj2.getInt("entityTypeId"), entityTypeId);
            JSONArray arr2 = obj2.getJSONArray("highlightedSnippets");
            for(int j = 0 ; j < arr2.length() ; j++){
                String temp = arr2.getString(j);
                if(temp.toLowerCase().contains(keywords[0].trim().substring(1,keywords[0].trim().length()-1).toLowerCase())) first = true;
                if(temp.toLowerCase().contains(keywords[1].trim().substring(1,keywords[1].trim().length()-1).toLowerCase())) second = true;
                if(first && (!second) && j==arr2.length()-1) {
                    isFound = true;
                    first = false;
                    second = false;
                    break;
                }
            }
            if(!isFound){
                csAssert.fail("Keywords were not found in the resultset");
            }
        }

        // Contract Document test
        isFound = false;
        keyword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "solrquerycontractdoc", "keyword");
        keyword = keyword.substring(1,keyword.length()-1);

        keywords = keyword.split(",");
        keywordAlpha = getKeyWordsWithSymbol(keywords, "NOT");
        keywordSymbol = getKeyWordsWithSymbol(keywords, "!");

        // Not check
        SearchContractDoc docTreeObj = new SearchContractDoc();
        docTreeObj.hitSearchContractDoc(keywordAlpha);
        String docTreeJsonStr1 = docTreeObj.getContractDocJsonStr();

        // ! check
        docTreeObj = new SearchContractDoc();
        docTreeObj.hitSearchContractDoc(keywordSymbol);
        String docTreeJsonStr2 = docTreeObj.getContractDocJsonStr();

        csAssert.assertEquals(docTreeJsonStr1, docTreeJsonStr2, "Resultset doesn't match for Logical and Alphabetical operator");

        jsonObj = new JSONObject(docTreeJsonStr1);
        arr = jsonObj.getJSONArray("searchResults");
        for(int i = 0 ; i < arr.length() ; i++){
            JSONObject obj2 = arr.getJSONObject(i);
            JSONArray arr2 = obj2.getJSONArray("highlightedSnippets");
            for(int j = 0 ; j < arr2.length() ; j++){
                String temp = arr2.getString(j);
                if(temp.toLowerCase().contains(keywords[0].trim().substring(1,keywords[0].trim().length()-1).toLowerCase())) first = true;
                if(temp.toLowerCase().contains(keywords[1].trim().substring(1,keywords[1].trim().length()-1).toLowerCase())) second = true;
                if(first && (!second) && j==arr2.length()-1) {
                    isFound = true;
                    first = false;
                    second = false;
                    break;
                }
            }
            if(!isFound){
                csAssert.fail("Keywords were not found in the resultset");
            }
        }
        csAssert.assertAll();
    }

    // C141331 - + check
    @Test(enabled = true)
    public void testC141331Required() {
        CustomAssert csAssert = new CustomAssert();

        // Search attachment check
        boolean first = false, second = false, isFound = false;
        String keyword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "solrquerysearchattach", "keyword2");
        keyword = keyword.substring(1,keyword.length()-1);

        String[] keywords = keyword.split(",");
        String keywordAlpha = getKeyWordsWithSymbol(keywords, "+");

        // Required check
        SearchAttachment attachObj = new SearchAttachment();
        attachObj.hitAttachment(keywordAlpha, entityTypeId);
        String attachmentJsonStr1 = attachObj.getAttachmentJsonStr();

        JSONObject jsonObj = new JSONObject(attachmentJsonStr1);
        JSONArray arr = jsonObj.getJSONArray("searchResults");
        for(int i = 0 ; i < arr.length() ; i++){
            JSONObject obj2 = arr.getJSONObject(i);
            csAssert.assertEquals(obj2.getInt("entityTypeId"), entityTypeId);
            JSONArray arr2 = obj2.getJSONArray("highlightedSnippets");
            for(int j = 0 ; j < arr2.length() ; j++){
                String temp = arr2.getString(j);

                if(temp.toLowerCase().contains("<b>test</b> <b>contract</b>")) first = true;
                if(first && j==arr2.length()-1) {
                    isFound = true;
                    first = false;
                    break;
                }
            }
            if(!isFound){
                csAssert.fail("Keywords were not found in the resultset");
            }
        }

        // Contract Document test
        isFound = false;
        keyword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "solrquerycontractdoc", "keyword2");
        keyword = keyword.substring(1,keyword.length()-1);

        keywords = keyword.split(",");
        keywordAlpha = getKeyWordsWithSymbol(keywords, "+");

        // Required check
        SearchContractDoc docTreeObj = new SearchContractDoc();
        docTreeObj.hitSearchContractDoc(keywordAlpha);
        String docTreeJsonStr1 = docTreeObj.getContractDocJsonStr();

        jsonObj = new JSONObject(docTreeJsonStr1);
        arr = jsonObj.getJSONArray("searchResults");
        for(int i = 0 ; i < arr.length() ; i++){
            JSONObject obj2 = arr.getJSONObject(i);
            JSONArray arr2 = obj2.getJSONArray("highlightedSnippets");
            for(int j = 0 ; j < arr2.length() ; j++){
                String temp = arr2.getString(j);

                if(temp.toLowerCase().contains("<b>services</b> <b>provided</b>")) first = true;
                if(first && j==arr2.length()-1) {
                    isFound = true;
                    first = false;
                    break;
                }
            }
            if(!isFound){
                csAssert.fail("Keywords were not found in the resultset");
            }
        }
        csAssert.assertAll();
    }

    // C90842 - ~ check
    @Test(enabled = true)
    public void testC90842() {
        CustomAssert csAssert = new CustomAssert();

        // Search attachment check
        String keyword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "solrquerysearchattach", "keyword3");
        keyword = keyword.substring(1,keyword.length()-1);

        String[] keywords = keyword.split(",");
        String keywordAlpha = keywords[0];
        String firstKeyword = keywords[1];
        String secondKeyword = keywords[2];

        SearchAttachment attachObj = new SearchAttachment();
        attachObj.hitAttachment(keywordAlpha, entityTypeId);
        String attachmentJsonStr1 = attachObj.getAttachmentJsonStr();

        JSONObject jsonObj = new JSONObject(attachmentJsonStr1);
        JSONArray arr = jsonObj.getJSONArray("searchResults");
        for(int i = 0 ; i < arr.length() ; i++){
            JSONObject obj2 = arr.getJSONObject(i);
            csAssert.assertEquals(obj2.getInt("entityTypeId"), entityTypeId, "Entity Type doesn't match.");
            JSONArray arr2 = obj2.getJSONArray("highlightedSnippets");
            String boldText = arr2.get(0).toString();
            boldText = boldText.replaceAll("<b>","");
            boldText = boldText.replaceAll("</b>","");
            String[] words= boldText.split(" ");
            if(getIndex(words, firstKeyword)!=getIndex(words, secondKeyword)) {
                csAssert.assertTrue((getIndex(words,secondKeyword)-getIndex(words,firstKeyword))>=2,"testC90842() failed as the ~ operator doesn't work.");
            }
            else {
                csAssert.fail("testC90842() failed as the ~ operator doesn't work for Search attachment");
                break;
            }
        }

        // Contract Document test
        keyword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "solrquerycontractdoc", "keyword3");
        keyword = keyword.substring(1,keyword.length()-1);

        keywords = keyword.split(",");
        keywordAlpha = keywords[0];
        firstKeyword = keywords[1];
        secondKeyword = keywords[2];

        SearchContractDoc docTreeObj = new SearchContractDoc();
        docTreeObj.hitSearchContractDoc(keywordAlpha);
        String docTreeJsonStr1 = docTreeObj.getContractDocJsonStr();

        jsonObj = new JSONObject(docTreeJsonStr1);
        arr = jsonObj.getJSONArray("searchResults");
        for(int i = 0 ; i < arr.length() ; i++){
            JSONObject obj2 = arr.getJSONObject(i);
            JSONArray arr2 = obj2.getJSONArray("highlightedSnippets");
            String boldText = arr2.get(0).toString();
            boldText = boldText.replaceAll("<b>","");
            boldText = boldText.replaceAll("</b>","");
            String[] words= boldText.split(" ");
            if(getIndex(words, firstKeyword)!=getIndex(words, secondKeyword)) {
                csAssert.assertTrue((getIndex(words,secondKeyword)-getIndex(words,firstKeyword))>=2,"testC90842() failed as the ~ operator doesn't work.");
                continue;
            }
            else {
                csAssert.fail("testC90842() failed as the ~ operator doesn't work for Contract Document");
                break;
            }
        }
        csAssert.assertAll();
    }

    // C90843 - ? check
    @Test(enabled = true)
    public void testC90843() {
        CustomAssert csAssert = new CustomAssert();

        // Search attachment check
        String keyword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "solrquerysearchattach", "keyword4");
        keyword = keyword.substring(1,keyword.length()-1);

        SearchAttachment attachObj = new SearchAttachment();
        attachObj.hitAttachment(keyword, entityTypeId);
        String attachmentJsonStr1 = attachObj.getAttachmentJsonStr();

        JSONObject jsonObj = new JSONObject(attachmentJsonStr1);
        JSONArray arr = jsonObj.getJSONArray("searchResults");
        for(int i = 0 ; i < arr.length() ; i++){
            JSONObject obj2 = arr.getJSONObject(i);
            csAssert.assertEquals(obj2.getInt("entityTypeId"), entityTypeId, "Entity Type doesn't match.");
            JSONArray arr2 = obj2.getJSONArray("highlightedSnippets");
            for(int j = 0 ; j<arr2.length();  j++){
                Object[] tempwords = getHighlightedWords(arr2.get(j).toString());
                validate(tempwords, csAssert, keyword);

            }
        }

        // Contract Document test
        keyword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "solrquerycontractdoc", "keyword4");
        keyword = keyword.substring(1,keyword.length()-1);

        SearchContractDoc docTreeObj = new SearchContractDoc();
        docTreeObj.hitSearchContractDoc(keyword);
        String docTreeJsonStr1 = docTreeObj.getContractDocJsonStr();

        jsonObj = new JSONObject(docTreeJsonStr1);
        arr = jsonObj.getJSONArray("searchResults");
        for(int i = 0 ; i < arr.length() ; i++){
            JSONObject obj2 = arr.getJSONObject(i);
            JSONArray arr2 = obj2.getJSONArray("highlightedSnippets");
            for(int j = 0 ; j<arr2.length();  j++){
                Object[] tempwords = getHighlightedWords(arr2.get(j).toString());
                validate(tempwords, csAssert, keyword);

            }
        }
        csAssert.assertAll();
    }

    // C90877 - * check
    @Test(enabled = true)
    public void testC90877() {
        CustomAssert csAssert = new CustomAssert();

        // Search attachment check
        String keywords = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "solrquerysearchattach", "keyword5");
        keywords = keywords.substring(1,keywords.length()-1);
        String[] keywordsArr = keywords.split(",");

        for(int a = 0 ; a < keywordsArr.length ; a++) {
            String keyword = keywordsArr[a];

            SearchAttachment attachObj = new SearchAttachment();
            attachObj.hitAttachment(keyword, entityTypeId);
            String attachmentJsonStr1 = attachObj.getAttachmentJsonStr();

            JSONObject jsonObj = new JSONObject(attachmentJsonStr1);
            JSONArray arr = jsonObj.getJSONArray("searchResults");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj2 = arr.getJSONObject(i);
                JSONArray arr2 = obj2.getJSONArray("highlightedSnippets");
                for (int j = 0; j < arr2.length(); j++) {
                    Object[] tempwords = getHighlightedWords(arr2.get(j).toString());
                    validate(tempwords, csAssert, keyword, a);

                }
            }

            // Contract Document test
            keyword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "solrquerycontractdoc", "keyword5");
            keyword = keyword.substring(1, keyword.length() - 1);

            SearchContractDoc docTreeObj = new SearchContractDoc();
            docTreeObj.hitSearchContractDoc(keyword);
            String docTreeJsonStr1 = docTreeObj.getContractDocJsonStr();

            jsonObj = new JSONObject(docTreeJsonStr1);
            arr = jsonObj.getJSONArray("searchResults");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj2 = arr.getJSONObject(i);
                JSONArray arr2 = obj2.getJSONArray("highlightedSnippets");
                for (int j = 0; j < arr2.length(); j++) {
                    Object[] tempwords = getHighlightedWords(arr2.get(j).toString());
                    validate(tempwords, csAssert, keyword, a);

                }
            }
        }
        csAssert.assertAll();
    }

    // C90844 - ~ check Fuzzy Search
    @Test(enabled = true)
    public void testC90844() {
        CustomAssert csAssert = new CustomAssert();

        // Search attachment check
        String keywords = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "solrquerysearchattach", "keyword6");
        String keyword = keywords.substring(1,keywords.length()-1);

        SearchAttachment attachObj = new SearchAttachment();
        attachObj.hitAttachment(keyword, entityTypeId, 20 , 0);
        String attachmentJsonStr1 = attachObj.getAttachmentJsonStr();

        JSONObject jsonObj = new JSONObject(attachmentJsonStr1);
        JSONArray arr = jsonObj.getJSONArray("searchResults");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj2 = arr.getJSONObject(i);
            JSONArray arr2 = obj2.getJSONArray("highlightedSnippets");
            for (int j = 0; j < arr2.length(); j++) {
                Object[] tempwords = getHighlightedWords(arr2.get(j).toString());
                validateFuzzySearch(tempwords, csAssert, keyword.substring(0,keyword.length()-1));

            }
        }

        // Contract Document test
        keyword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "solrquerycontractdoc", "keyword6");
        keyword = keyword.substring(1, keyword.length() - 1);

        SearchContractDoc docTreeObj = new SearchContractDoc();
        docTreeObj.hitSearchContractDoc(keyword, 20, 0 );
        String docTreeJsonStr1 = docTreeObj.getContractDocJsonStr();

        jsonObj = new JSONObject(docTreeJsonStr1);
        arr = jsonObj.getJSONArray("searchResults");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj2 = arr.getJSONObject(i);
            JSONArray arr2 = obj2.getJSONArray("highlightedSnippets");
            for (int j = 0; j < arr2.length(); j++) {
                Object[] tempwords = getHighlightedWords(arr2.get(j).toString());
                validateFuzzySearch(tempwords, csAssert, keyword.substring(0,keyword.length()-1));

            }
        }
        csAssert.assertAll();
    }

    // C90847 - Phrase Search
    @Test(enabled = true)
    public void testC90847() {
        CustomAssert csAssert = new CustomAssert();

        // Search attachment check
        String keywords = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "solrquerysearchattach", "keyword7");
        String keyword = keywords.substring(1,keywords.length()-1);

        SearchAttachment attachObj = new SearchAttachment();
        attachObj.hitAttachment(keyword, entityTypeId, 20 , 0);
        String attachmentJsonStr1 = attachObj.getAttachmentJsonStr();

        JSONObject jsonObj = new JSONObject(attachmentJsonStr1);
        JSONArray arr = jsonObj.getJSONArray("searchResults");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj2 = arr.getJSONObject(i);
            JSONArray arr2 = obj2.getJSONArray("highlightedSnippets");
            for (int j = 0; j < arr2.length(); j++) {
                Object[] tempwords = getHighlightedWords(arr2.get(j).toString());
                validateC90847(tempwords, csAssert, keyword.substring(1,keyword.length()-1));
            }
        }

        // Contract Document test
        keyword = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "solrquerycontractdoc", "keyword7");
        keyword = keyword.substring(1, keyword.length() - 1);

        SearchContractDoc docTreeObj = new SearchContractDoc();
        docTreeObj.hitSearchContractDoc(keyword, 20, 0 );
        String docTreeJsonStr1 = docTreeObj.getContractDocJsonStr();

        jsonObj = new JSONObject(docTreeJsonStr1);
        arr = jsonObj.getJSONArray("searchResults");
        for (int i = 0; i < arr.length(); i++) {
            JSONObject obj2 = arr.getJSONObject(i);
            JSONArray arr2 = obj2.getJSONArray("highlightedSnippets");
            for (int j = 0; j < arr2.length(); j++) {
                Object[] tempwords = getHighlightedWords(arr2.get(j).toString());
                validateC90847(tempwords, csAssert, keyword.substring(1,keyword.length()-1));            }
        }
        csAssert.assertAll();
    }

    // C90848 - Invalid query check
    @Test(enabled = true)
    public void testC90848() {
        CustomAssert csAssert = new CustomAssert();

        String russianLanguage = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "email", "russianlanguage");
        String defaultUserEmail  = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "email", "defaultuserEmail");
        String englishLanguage = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "email", "englishlanguage");

        boolean languageChanged;
        languageChanged = EmailActionDbHelper.updateUserLanguage(defaultUserEmail, russianLanguage);

        // Refreshing the session
        Check checkObj = new Check();
        checkObj.hitCheck("anay_user", "admin1234a");

        if(languageChanged) {


            String keywords = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "solrquerysearchattach", "keyword8");
            keywords = keywords.substring(1, keywords.length() - 1);
            String[] keywordsArr = keywords.split(",");

            for (int a = 0; a < keywordsArr.length; a++) {
                String keyword = keywordsArr[a];

                // Search attachment check
                SearchAttachment attachObj = new SearchAttachment();
                attachObj.hitAttachment(keyword, entityTypeId);
                String attachmentJsonStr1 = attachObj.getAttachmentJsonStr();

                JSONObject jsonObj = new JSONObject(attachmentJsonStr1);
                csAssert.assertEquals(jsonObj.getInt("errorCode"), 400, "Actual : "+jsonObj.getInt("errorCode")+" | Expected : "+400+"");
                csAssert.assertEquals(jsonObj.getString("errorMessage").toLowerCase(), "Russian Invalid Query: your search query has incorrect syntax, please fix and re-try".toLowerCase(), "Actual : "+jsonObj.getString("errorMessage")+" | Expected : "+"Russian Invalid Query: your search query has incorrect syntax, please fix and re-try".toLowerCase());

                // Contract Document test
                SearchContractDoc docTreeObj = new SearchContractDoc();
                docTreeObj.hitSearchContractDoc(keyword);
                String docTreeJsonStr1 = docTreeObj.getContractDocJsonStr();

                jsonObj = new JSONObject(docTreeJsonStr1);
                csAssert.assertEquals(jsonObj.getInt("errorCode"), 400, "Actual : "+jsonObj.getInt("errorCode")+" | Expected : "+400+"");
                csAssert.assertEquals(jsonObj.getString("errorMessage").toLowerCase(), "Russian Invalid Query: your search query has incorrect syntax, please fix and re-try".toLowerCase(), "Actual : "+jsonObj.getString("errorMessage")+" | Expected : "+"Russian Invalid Query: your search query has incorrect syntax, please fix and re-try".toLowerCase());

            }
        }

        languageChanged = EmailActionDbHelper.updateUserLanguage(defaultUserEmail, englishLanguage);

        // Refreshing the session
        checkObj = new Check();
        checkObj.hitCheck("anay_user", "admin1234a");

        if(languageChanged) {

            String keywords = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "solrquerysearchattach", "keyword8");
            keywords = keywords.substring(1, keywords.length() - 1);
            String[] keywordsArr = keywords.split(",");

            for (int a = 0; a < keywordsArr.length; a++) {
                String keyword = keywordsArr[a];

                // Search attachment check
                SearchAttachment attachObj = new SearchAttachment();
                attachObj.hitAttachment(keyword, entityTypeId);
                String attachmentJsonStr1 = attachObj.getAttachmentJsonStr();

                JSONObject jsonObj = new JSONObject(attachmentJsonStr1);
                csAssert.assertEquals(jsonObj.getInt("errorCode"), 400, "Actual : "+jsonObj.getInt("errorCode")+" | Expected : "+400+"");
                csAssert.assertEquals(jsonObj.getString("errorMessage").toLowerCase(), "Invalid Query: Your Search Query Has Incorrect Syntax, Please Fix And Re-try".toLowerCase(), "Actual : "+jsonObj.getString("errorMessage")+" | Expected : "+"Invalid Query: Your Search Query Has Incorrect Syntax, Please Fix And Re-try".toLowerCase());

                // Contract Document test
                SearchContractDoc docTreeObj = new SearchContractDoc();
                docTreeObj.hitSearchContractDoc(keyword);
                String docTreeJsonStr1 = docTreeObj.getContractDocJsonStr();

                jsonObj = new JSONObject(docTreeJsonStr1);
                csAssert.assertEquals(jsonObj.getInt("errorCode"), 400, "Actual : "+jsonObj.getInt("errorCode")+" | Expected : "+400+"");
                csAssert.assertEquals(jsonObj.getString("errorMessage").toLowerCase(), "Invalid Query: Your Search Query Has Incorrect Syntax, Please Fix And Re-try".toLowerCase(), "Actual : "+jsonObj.getString("errorMessage")+" | Expected : "+"Invalid Query: Your Search Query Has Incorrect Syntax, Please Fix And Re-try".toLowerCase());
            }
        }

        csAssert.assertAll();
    }

    // C90937 - Combination of Queries
    @Test(enabled = true)
    public void testC90937() {
        CustomAssert csAssert = new CustomAssert();

        // Search attachment check
        String data = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "solrquerysearchattach", "keyword9");
        String[] keywords = data.substring(1,data.length()-1).split(",");
        for(int a = 0 ; a < keywords.length; a++) {

            String keyword = keywords[a];

            SearchAttachment attachObj = new SearchAttachment();
            attachObj.hitAttachment(keyword, entityTypeId);
            String attachmentJsonStr1 = attachObj.getAttachmentJsonStr();

            JSONObject jsonObj = new JSONObject(attachmentJsonStr1);
            JSONArray arr = jsonObj.getJSONArray("searchResults");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj2 = arr.getJSONObject(i);
                csAssert.assertEquals(obj2.getInt("entityTypeId"), entityTypeId, "Entity Type doesn't match.");
                JSONArray arr2 = obj2.getJSONArray("highlightedSnippets");
                for (int j = 0; j < arr2.length(); j++) {
                    Object[] tempwords = getHighlightedWords(arr2.get(j).toString());
                    validateC90937(tempwords, csAssert, keyword, a);

                }
            }
        }
        // Contract Document test
        data = ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "solrquerycontractdoc", "keyword9");
        keywords = data.substring(1,data.length()-1).split(",");
        for(int a = 0 ; a < keywords.length; a++) {

            String keyword = keywords[a];

            SearchContractDoc docTreeObj = new SearchContractDoc();
            docTreeObj.hitSearchContractDoc(keyword);
            String docTreeJsonStr1 = docTreeObj.getContractDocJsonStr();

            JSONObject jsonObj = new JSONObject(docTreeJsonStr1);
            JSONArray arr = jsonObj.getJSONArray("searchResults");
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj2 = arr.getJSONObject(i);
                JSONArray arr2 = obj2.getJSONArray("highlightedSnippets");
                for (int j = 0; j < arr2.length(); j++) {
                    Object[] tempwords = getHighlightedWords(arr2.get(j).toString());
                    validateC90937(tempwords, csAssert, keyword, a);
                }
            }
        }
        csAssert.assertAll();
    }

    // C141105 - Static field - Error Message when single field is selected
    @Test(enabled = false)
    public void testC141105() throws ConfigurationException {
        CustomAssert csAssert = new CustomAssert();

        // Get Payload
        String payload =  ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "c141105", "payload");
        String expectedErrorMessage =  ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "c141105", "errormessage");

        // Hit the MetaData API for entity Contract and Incorrect title

        Search searchObj = new Search();
        logger.info("Hitting Search Api for entityTypeId ; {} and Document Type : Other", entityTypeId);
        String searchResponse = searchObj.hitSearch(entityTypeId, payload);

        JSONObject obj = new JSONObject(searchObj.hitSearch(entityTypeId, payload));
        String actualErrorMessage = obj.getJSONObject("body").getJSONObject("errors").getJSONArray("genericErrors").getJSONObject(0).getString("message");

        csAssert.assertEquals(actualErrorMessage, expectedErrorMessage, "Actual : "+actualErrorMessage+" | Expected : "+expectedErrorMessage+"" );

        csAssert.assertAll();
    }

    // C141106 - Static field - Error Message when multiple fields are selected
    @Test(enabled = false)
    public void testC141106() throws ConfigurationException {
        CustomAssert csAssert = new CustomAssert();

        // Get Payload
        String payload =  ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "c141106", "payload");
        String expectedErrorMessage =  ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "c141106", "errormessage");

        // Hit the MetaData API for entity Contract and Incorrect title

        Search searchObj = new Search();
        logger.info("Hitting Search Api for entityTypeId ; {} and Document Type : Other", entityTypeId);
        String searchResponse = searchObj.hitSearch(entityTypeId, payload);

        JSONObject obj = new JSONObject(searchObj.hitSearch(entityTypeId, payload));
        String actualErrorMessage = obj.getJSONObject("body").getJSONObject("errors").getJSONArray("genericErrors").getJSONObject(0).getString("message");

        csAssert.assertEquals(actualErrorMessage, expectedErrorMessage, "Actual : "+actualErrorMessage+" | Expected : "+expectedErrorMessage+"" );

        csAssert.assertAll();
    }

    // C141107 - Custom field - Error Message when single custom field is searched for
    @Test(enabled = false)
    public void testC141107() throws ConfigurationException {
        CustomAssert csAssert = new CustomAssert();

        // Get Payload
        String payload =  ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "c141107", "payload");
        String expectedErrorMessage =  ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "c141107", "errormessage");

        // Hit the MetaData API for entity Contract and Incorrect title

        Search searchObj = new Search();
        logger.info("Hitting Search Api for entityTypeId ; {} and Document Type : Other", entityTypeId);
        String searchResponse = searchObj.hitSearch(entityTypeId, payload);

        JSONObject obj = new JSONObject(searchObj.hitSearch(entityTypeId, payload));
        String actualErrorMessage = obj.getJSONObject("body").getJSONObject("errors").getJSONArray("genericErrors").getJSONObject(0).getString("message");

        csAssert.assertEquals(actualErrorMessage, expectedErrorMessage, "Actual : "+actualErrorMessage+" | Expected : "+expectedErrorMessage+"" );

        csAssert.assertAll();
    }

    // C141108 - Custom field - Error Message when multiple fields including custom fields are searched for
    @Test(enabled = false)
    public void testC141108() throws ConfigurationException {
        CustomAssert csAssert = new CustomAssert();

        // Get Payload
        String payload =  ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "c141108", "payload");
        String expectedErrorMessage =  ParseConfigFile.getValueFromConfigFile(configFilePath, configFileName, "c141108", "errormessage");

        // Hit the MetaData API for entity Contract and Incorrect title

        Search searchObj = new Search();
        logger.info("Hitting Search Api for entityTypeId ; {} and Document Type : Other", entityTypeId);
        String searchResponse = searchObj.hitSearch(entityTypeId, payload);

        JSONObject obj = new JSONObject(searchObj.hitSearch(entityTypeId, payload));
        String actualErrorMessage = obj.getJSONObject("body").getJSONObject("errors").getJSONArray("genericErrors").getJSONObject(0).getString("message");

        csAssert.assertEquals(actualErrorMessage, expectedErrorMessage, "Actual : "+actualErrorMessage+" | Expected : "+expectedErrorMessage+"" );

        csAssert.assertAll();
    }

    private String getKeyWordsWithSymbol(String[] keywords, String symbol){
        String finalStr = "";

        for(String item : keywords){
            finalStr = finalStr + symbol + item;
        }

        return  finalStr.substring(symbol.length());
    }

    private int getIndex(String[] words, String firstKeyword){
        int num = 0;

        for(int i = 0 ; i < words.length ; i++){
            if(words[i].equalsIgnoreCase(firstKeyword)) return i;
        }
        return num;
    }

    private Object[] getHighlightedWords(String str){
        List<String> list = new ArrayList<>();
        for(int i = 0 ; i < str.length() ; i++){

            if(str.charAt(i)=='<' && str.charAt(i+1)=='b'){
                String temp =   str.substring(i+3, str.substring(i+3).indexOf("</")+i+3);
                list.add(temp);
            }
        }

        return list.toArray();
    }

    private void validate(Object[] tempwords, CustomAssert csAssert, String keyword){
        for(Object item: tempwords){
            csAssert.assertEquals(item.toString().length(), keyword.length(),"Length doesn't match for testC90843()");
            csAssert.assertEquals(item.toString().toLowerCase().charAt(0),keyword.toLowerCase().charAt(0), "First character does match for testC90843()" );
        }
    }

    private void validateC90847(Object[] tempwords, CustomAssert csAssert, String keyword){
        for(Object item: tempwords){
            csAssert.assertEquals(item.toString().toLowerCase(), keyword.toLowerCase(),"Highlighted text doesn't match : Actual - "+item.toString().toLowerCase()+"  Expected - "+keyword.toLowerCase()+"");
        }
    }

    private void validateC90937(Object[] tempwords, CustomAssert csAssert, String keyword, int a){

        if(a==0) {
            String[] keywords = keyword.split("AND");
            keywords[0] = keywords[0].substring(2, 5);
            keywords[1] = keywords[1].substring(1, keywords[1].length() - 1);

            for (Object item : tempwords) {
                csAssert.assertTrue(item.toString().toLowerCase().contains(keywords[1].toLowerCase()), "Highlighted text doesn't contain " + keywords[1] + " : Actual - " + item.toString().toLowerCase());
                csAssert.assertFalse(item.toString().toLowerCase().contains(keywords[0].toLowerCase()), "Highlighted text contains " + keywords[0] + " : Actual - " + item.toString().toLowerCase());
            }
        }

        if(a==1){
            String[] keywords = keyword.split("AND");
            keywords[0] = keywords[0].substring(0,1);
            keywords[1] = keywords[1].substring(1);

            for (Object item : tempwords) {
                csAssert.assertTrue(item.toString().toLowerCase().startsWith(keywords[0]), "Highlighted text doesn't start with " + keywords[0] + " : Actual - " + item.toString().toLowerCase());
                csAssert.assertTrue(item.toString().toLowerCase().endsWith(keywords[1]), "Highlighted text doesn't end with " + keywords[1] + " : Actual - " + item.toString().toLowerCase());
            }
        }
        if(a==2){
            String[] keywords = keyword.split("OR");
            keywords[0] = keywords[0].trim().substring(0,1);
            keywords[1] = keywords[1].trim().substring(1);

            for (Object item : tempwords) {
                csAssert.assertTrue((item.toString().toLowerCase().startsWith(keywords[0])) || (item.toString().toLowerCase().endsWith(keywords[1])), "Highlighted text either doesn't start with or ends with" + keywords[0] + " : Actual Text- " + item.toString().toLowerCase());
            }
        }
    }

    private void validateFuzzySearch(Object[] tempwords, CustomAssert csAssert, String keyword){
        for(Object item: tempwords){
            boolean match = false;
            int count = 0;
            if(item.toString().toLowerCase().contains(keyword.toLowerCase())) {
                match = true;
                continue;
            }
            if(keyword.toLowerCase().contains(item.toString().toLowerCase())){
                match = true;
                continue;
            }

            char[] arrItem = item.toString().toLowerCase().toCharArray();
            char[] arrKeyword = keyword.toLowerCase().toCharArray();
            if(arrItem.length <= arrKeyword.length) {
                for (int i = 0; i < arrItem.length; i++) {
                    for (int j = 0; j <  arrKeyword.length; j++) {
                        if(arrItem[i]==arrKeyword[j]){
                            count++;
                            break;
                        }
                    }
                }
            }
            else {
                for (int i = 0; i < arrKeyword.length; i++) {
                    for (int j = 0; j <  arrItem.length; j++) {
                        if(arrKeyword[i]==arrItem[j])count++;
                    }
                }
            }
            csAssert.assertTrue(count>=2,"Fuzzy Search failed for keyword : "+keyword+"");

        }
    }

    private void validate(Object[] tempwords, CustomAssert csAssert, String keyword, int a){
        for(Object item: tempwords){
            if(a==0){
                csAssert.assertEquals(item.toString().toLowerCase().charAt(0),keyword.toLowerCase().charAt(0));
            }
            else if(a==1){
                csAssert.assertTrue(item.toString().toLowerCase().contains(keyword.toLowerCase().substring(1,2)),"Actual - "+item.toString().toLowerCase()+"   Expected - "+keyword.toLowerCase().substring(1,2)+"");
            }
            else {
                csAssert.assertEquals(item.toString().toLowerCase().charAt(item.toString().length()-1),keyword.toLowerCase().charAt(1));
            }
        }
    }
}
