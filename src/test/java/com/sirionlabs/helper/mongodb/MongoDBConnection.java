package com.sirionlabs.helper.mongodb;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.json.JSONObject;

import java.util.*;

public class MongoDBConnection {

    private static String hosturl;
    private static int port;
    private static MongoClient mongoClient;

    public MongoDBConnection(String hostUrl, int port) {
        this.hosturl = hostUrl;
        this.port = port;
        mongoClient = new MongoClient(this.hosturl, this.port);
    }

    public List<Document> getDBResponse(String dataBaseName, String collectionName, String fieldName, int fieldValue) {
        List<Document> uploadedDocsData = new LinkedList<>();
        MongoDatabase db = mongoClient.getDatabase(dataBaseName);
        MongoCollection<Document> table = db.getCollection(collectionName);
        FindIterable<Document> documents = table.find().filter(Filters.eq(fieldName, fieldValue));
        MongoCursor<Document> itr = documents.iterator();

        while (itr.hasNext()) {
            uploadedDocsData.add(itr.next());
        }
        return uploadedDocsData;
    }

    public boolean deleteDocumentFromDB(String dataBaseName, String collectionName, String fieldName, int fieldValue) {
        boolean isDocDeleted = false;
        MongoDatabase db = mongoClient.getDatabase(dataBaseName);
        MongoCollection<Document> table = db.getCollection(collectionName);
        DeleteResult documents = table.deleteOne(Filters.eq(fieldName, fieldValue));
        isDocDeleted =documents.wasAcknowledged();

        return isDocDeleted;
    }

    public List<Document> getPreferredModelDataFromDB(String dataBaseName, String collectionName,int modelTypeId,int modelSubTypeId, int extractionTypeId ) {
        List<Document> uploadedDocsData = new LinkedList<>();
        MongoDatabase db = mongoClient.getDatabase(dataBaseName);
        MongoCollection<Document> table = db.getCollection(collectionName);

        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("modeltype.id",modelTypeId);
        whereQuery.put("modelsubtype.id",modelSubTypeId);
        whereQuery.put("extractiontypeid",extractionTypeId);

        FindIterable<Document> documents = table.find(whereQuery);

        MongoCursor<Document> itr = documents.iterator();

        while (itr.hasNext()) {
            uploadedDocsData.add(itr.next());
        }
        return uploadedDocsData;
    }

    public List<Document> getExtractedDataFromDB(String dataBaseName, String collectionName,int documentid,int textid) {
        List<Document> uploadedDocsData = new LinkedList<>();
        MongoDatabase db = mongoClient.getDatabase(dataBaseName);
        MongoCollection<Document> table = db.getCollection(collectionName);

        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("documentid",documentid);
        whereQuery.put("textid",textid);

        FindIterable<Document> documents = table.find(whereQuery);

        MongoCursor<Document> itr = documents.iterator();

        while (itr.hasNext()) {
            uploadedDocsData.add(itr.next());
        }
        return uploadedDocsData;
    }

    public List<Document> getCategoryInfoFromDB(String dataBaseName, String collectionName,String categoryName,int etId) {
        List<Document> uploadedDocsData = new LinkedList<>();
        MongoDatabase db = mongoClient.getDatabase(dataBaseName);
        MongoCollection<Document> table = db.getCollection(collectionName);

        BasicDBObject whereQuery = new BasicDBObject();
        whereQuery.put("categoryname",categoryName);
        whereQuery.put("etid",etId);

        FindIterable<Document> documents = table.find(whereQuery);

        MongoCursor<Document> itr = documents.iterator();

        while (itr.hasNext()) {
            uploadedDocsData.add(itr.next());
        }
        return uploadedDocsData;
    }

    public List<Object> getDocumentData(List<Document> dbResponse,String jsonObject,String jsonelement) {
            JSONObject jsonObj;
            Object value;
            List<Object> values = new LinkedList<>();
            if(dbResponse.size()==1) {
                jsonObj = new JSONObject(dbResponse.get(0).toJson());
                value = jsonObj.getJSONObject(jsonObject).get(jsonelement);
                values.add(value);
            }
            else{
                for(Document doc : dbResponse){
                    jsonObj = new JSONObject(doc.toJson());
                    value = jsonObj.getJSONObject(jsonObject).get(jsonelement);
                    values.add(value);
                }
            }
            return values;
    }
}




