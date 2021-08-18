package com.sirionlabs.utils.commonUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class testCaseCount {

    static int suiteId = 2;

    public static void main (String[] args) {
        try {
            Connection conn = createConnection();
            fetchCount(conn, null,null);
            System.out.println("Done");

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    static Data fetchCount(Connection conn, Integer parentId, String paretName) throws Exception {
        Statement st = conn.createStatement();
        String sql = null;
        if (parentId == null) {
            sql = String.format("select id,name from sections where parent_id is null and suite_id = %s order by id",
                    suiteId);
        }else {
            sql = String.format("select id,name from sections where parent_id = %s and suite_id = %s order by id",
                    parentId, suiteId);
        }
        ResultSet rs = st.executeQuery(sql);

        Data data = new Data();
        while (rs.next()) {
            int childId = rs.getInt(1);
            String childName= rs.getString(2);
            Data childData = fetchCount(conn, childId,childName);
            data.sc += childData.sc;
            data.cc += childData.cc;

            if(parentId==null) {
                System.out.println(String.format("%s $ %s  $ %s $ %s", childId,
                        childName, parentId, childData.cc));
                //System.out.println("::::::::::::::::::::::::::::::::::::::::::::::");
            }
        }
        if(data.sc==0) {
            data.sc=1;
        }
        data.cc = data.cc + caseCount(conn, parentId);

        return data;
    }

    static int caseCount(Connection conn, Integer sectionId) throws Exception{
        String sql = String.format("select count(*) from cases where  section_id = %s and suite_id = 2 and custom_tc_autostatus in (4,5)",
                sectionId, suiteId);
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while(rs.next()) {
            return rs.getInt(1);
        }
        return 0;
    }

    static class Data{
        int sc;
        int cc;
    }

    static Connection createConnection() throws Exception {
        String myDriver = "com.mysql.cj.jdbc.Driver";
        String myUrl = "jdbc:mysql://192.168.2.112/testrail";
        Class.forName(myDriver);
        Connection conn = DriverManager.getConnection(myUrl, "naveen.gupta", "Password8");
        return conn;
    }

}

