package com.sirionlabs.helper.localmongo;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.WriteResult;
import com.sirionlabs.utils.commonUtils.ParseJsonResponse;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONUtil;

public class LocalMongoHelper {

    String ipAddress = "192.168.2.154";
    int port = 27017;
    String dbName = "automation";

    //public String getExcelProperties(String id){

    public List<String> getExcelProperties(String collection){


        MongoClient mongo = new MongoClient(ipAddress, port);
        DB db = mongo.getDB(dbName);
        DBCollection col = db.getCollection(collection);

        DBObject query = BasicDBObjectBuilder.start().get();
        DBCursor cursor = col.find(query);

        List<String> list = new ArrayList<>();

        while (cursor.hasNext()) {
            DBObject object = cursor.next();
            System.out.println(object);

            list.add(object.toString());



        }
        return list;

    }

    public void insert(String collection, String idName, Map<String,Object> data){

        BasicDBObjectBuilder docBuilder = BasicDBObjectBuilder.start();
        docBuilder.append("_id", idName);
        for(Map.Entry<String,Object>  entry : data.entrySet()) {
            docBuilder.append(entry.getKey(),entry.getValue());
        }

        MongoClient mongo = new MongoClient(ipAddress, port);
        DB db = mongo.getDB(dbName);

        DBCollection col = db.getCollection(collection);

        WriteResult result = col.insert(docBuilder.get());
        System.out.println(result.getUpsertedId());
        System.out.println(result.getN());
        System.out.println(result.isUpdateOfExisting());
        //System.out.println(result.getLastConcern());

        mongo.close();

    }

//    public void main(String[] args) throws UnknownHostException {
//
//        User user = createUser();
//        DBObject doc = createDBObject(user);
//
//        MongoClient mongo = new MongoClient("localhost", 27017);
//        DB db = mongo.getDB("journaldev");
//
//        DBCollection col = db.getCollection("users");
//
//        //create user
//        WriteResult result = col.insert(doc);
//        System.out.println(result.getUpsertedId());
//        System.out.println(result.getN());
//        System.out.println(result.isUpdateOfExisting());
//        System.out.println(result.getLastConcern());
//
//        //read example
//        DBObject query = BasicDBObjectBuilder.start().add("_id", user.getId()).get();
//        DBCursor cursor = col.find(query);
//        while (cursor.hasNext()) {
//            System.out.println(cursor.next());
//        }
//
//        //update example
//        user.setName("Pankaj Kumar");
//        doc = createDBObject(user);
//        result = col.update(query, doc);
//        System.out.println(result.getUpsertedId());
//        System.out.println(result.getN());
//        System.out.println(result.isUpdateOfExisting());
//        System.out.println(result.getLastConcern());
//
//        //delete example
//        result = col.remove(query);
//        System.out.println(result.getUpsertedId());
//        System.out.println(result.getN());
//        System.out.println(result.isUpdateOfExisting());
//        System.out.println(result.getLastConcern());
//
//        //close resources
//        mongo.close();
//    }

//    private static DBObject createDBObject(User user) {
//        BasicDBObjectBuilder docBuilder = BasicDBObjectBuilder.start();
//
//        docBuilder.append("_id", user.getId());
//        docBuilder.append("name", user.getName());
//        docBuilder.append("role", user.getRole());
//        docBuilder.append("isEmployee", user.isEmployee());
//        return docBuilder.get();
//    }

}

