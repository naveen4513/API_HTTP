package com.sirionlabs.utils.commonUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MySqlDBConnector {
    public String serverip;
    public String port;
    public String dbName;
    public String Username;
    public String Password;


    Connection connection1;
    Statement statement1;
    public  ResultSet rs;

    public Statement connectdatabase(String ip , String sPort , String sdbName , String sUsername , String sPassword ) throws ClassNotFoundException, SQLException
    {
        serverip = ip;
        port = sPort;
        dbName = sdbName;
        Username = sUsername;
        Password = sPassword;
        // try {
        Class.forName("com.mysql.jdbc.Driver");
        connection1 = DriverManager.getConnection("jdbc:mysql://"+serverip+":"+port+"/"+dbName, Username, Password);
        statement1 = connection1.createStatement();
        return statement1;

    }


    public ResultSet selectstatement(String query)
    {

        try {

            rs=statement1.executeQuery(query);

        } catch (SQLException e) {

            e.printStackTrace();
        }

        return rs;

    }


    public void updatestatement(String query)
    {

        try {
            statement1.executeUpdate(query);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

    }


    public void insertStatement(String query)
    {

        try {
            statement1.executeUpdate(query);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }

    }

    public void disconnectFromDataBase(){
        try {
            statement1.close();
            connection1.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



}