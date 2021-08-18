package com.sirionlabs.test.TestPasswordFlow;

import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

public class LoginUtility {
    private final static Logger logger = LoggerFactory.getLogger(LoginUtility.class);

    public String getPasswordEmailTokenfromEmail(String emailId,String emailSubject,CustomAssert csAssert) {
        String authToken="";
        String select_query = "select * from system_emails where to_email = '" + emailId + "' and subject ilike '%"+emailSubject+"%'  order by id desc limit 10;";

        try {
            PostgreSQLJDBC sqlObj = new PostgreSQLJDBC(ConfigureEnvironment.getEnvironmentProperty("dbHostAddress"),
                    ConfigureEnvironment.getEnvironmentProperty("dbPortName"),ConfigureEnvironment.getEnvironmentProperty("letterboxdbname")
            ,ConfigureEnvironment.getEnvironmentProperty("dbUserName"),ConfigureEnvironment.getEnvironmentProperty("dbPassword"));
            List<List<String>> results = sqlObj.doSelect(select_query);

            int time = 0;
            while(time < 15000){
                if(results.size()>0){
                    break;
                }else{
                    Thread.sleep(4000);
                    results = sqlObj.doSelect(select_query);
                    time = time + 4000;

                }
            }

            String subject = results.get(0).get(6);
            String body = results.get(0).get(7);

            if (results.size() != 0) {
                csAssert.assertEquals(subject, emailSubject,
                        "subject is not correct, Expected "+emailSubject+" but actual --> " + subject);
                Document html = Jsoup.parse(body);
                authToken = html.getElementsByTag("body").get(0).getElementsByTag("tr")
                        .get(13).getElementsByTag("a").get(0).attr("href")
                        .split("=")[1];
                logger.info("AuthToken is " + authToken);

            } else {
                csAssert.assertTrue(false, "email not triggered for emailId -> " + emailId);
            }

            sqlObj.closeConnection();
        } catch (Exception e) {
            logger.error("Exception while Getting User Data from DB using query [{}]. {}", select_query, e.getMessage());
        }
        return authToken;
    }


    public void   deleteEntryFromTable(String emailID, String subject){
        PostgreSQLJDBC sqlObj = new PostgreSQLJDBC(ConfigureEnvironment.getEnvironmentProperty("dbHostAddress"),
                ConfigureEnvironment.getEnvironmentProperty("dbPortName"),ConfigureEnvironment.getEnvironmentProperty("letterboxdbname")
                ,ConfigureEnvironment.getEnvironmentProperty("dbUserName"),ConfigureEnvironment.getEnvironmentProperty("dbPassword"));
        String query = "delete from system_emails where to_email = '"+emailID+"' and subject ilike '%"+subject+"%' ;";
        try {
            sqlObj.deleteDBEntry(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        finally {
            sqlObj.closeConnection();
        }

    }

}
