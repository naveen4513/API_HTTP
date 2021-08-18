package com.sirionlabs.helper.WeeklyReminderHelper;
import com.sirionlabs.utils.commonUtils.PostgreSQLJDBC;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class WeeklyReminderHelper {
    private static final Logger logger = LoggerFactory.getLogger(WeeklyReminderHelper.class);
    private String host;
    private Integer port;
    private String user;
    private String password;
    private JSch jsch;
    private Session session;
    private Channel channel;
    private ChannelSftp sftpChannel;

    public WeeklyReminderHelper() {
    }

    public WeeklyReminderHelper(String host, Integer port, String user, String password) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
    }

    private void connect() {
        logger.info("connecting...{}", host);
        try {
            jsch = new JSch();
            session = jsch.getSession(user, host, port);
            session.setConfig("StrictHostKeyChecking", "no");
            session.setPassword(password);
            session.connect();
            channel = session.openChannel("sftp");
            channel.connect();
            sftpChannel = (ChannelSftp) channel;
        } catch (JSchException e) {
            logger.error("Exception while Connecting Remote Server{}", e.getMessage());
        }

    }

    private void disconnect() {
        logger.info("disconnecting...");
        sftpChannel.disconnect();
        channel.disconnect();
        session.disconnect();
    }

    public void uploadOnServer(String fileName, String remoteDir) {
        FileInputStream fis = null;
        connect();
        try {
            // Change to output directory
            sftpChannel.cd(remoteDir);
            // Upload file
            File file = new File(fileName);
            fis = new FileInputStream(file);
            sftpChannel.put(fis, file.getName());
            fis.close();
            logger.info("File uploaded successfully - {}", file.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Exception while uploading file on server {}", e.getMessage());
        } finally {
            disconnect();
        }
    }

    public Boolean downloadFileFromRemoteServer(String fileName, String localDir) {
        Boolean status = true;
        byte[] buffer = new byte[1024];
        BufferedInputStream bis;
        connect();
        try {
            // Change to output directory
            String cdDir = fileName.substring(0, fileName.lastIndexOf("/") + 1);
            sftpChannel.cd(cdDir);

            File file = new File(fileName);
            bis = new BufferedInputStream(sftpChannel.get(file.getName()));

            File newFile = new File(localDir + "/" + file.getName());

            // Download file
            OutputStream os = new FileOutputStream(newFile);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            int readCount;
            while ((readCount = bis.read(buffer)) > 0) {
                bos.write(buffer, 0, readCount);
            }
            bis.close();
            bos.close();
            logger.info("File downloaded successfully - " + file.getAbsolutePath());
        } catch (Exception e) {
            status = false;
            logger.error("Exception while Download file from Server {}", e.getMessage());
        } finally {
            disconnect();
        }
        return status;
    }

    public Boolean isWeeklyReminderSentSuccessfully(String fileName, String to_mail, Integer clientId) {
        logger.info(" check weekly reminder sent successfully");
        try {
            String query = "select sent_succesfully from system_emails where attachment_name ='" + fileName + "' and to_mail='" + to_mail + "' order by id limit 1";
            List<List<String>> queryData = new PostgreSQLJDBC().doSelect(query);
            if (!queryData.isEmpty() && !queryData.get(0).isEmpty()) {
                return queryData.get(0).get(0).equalsIgnoreCase("t");
            }
        } catch (Exception e) {
            logger.error("Exception while verifying weekly reminder sent successfully");
        }
        return false;
    }


    public ArrayList<String> getColumnData(String outputFilePath, String weeklyReportName, String sheetName,int col) throws IOException {

        ArrayList<String> columndata = new ArrayList<>();
        try {
            File f = new File(outputFilePath + "/" + weeklyReportName);
            FileInputStream ios = new FileInputStream(f);
            HSSFWorkbook workbook = new HSSFWorkbook(ios);
            HSSFSheet sheet = workbook.getSheet(sheetName);
            int ctr = 3;
            Row row = null;
            Cell cell = null;
            boolean isNull = false;
            do {
                try {
                    row = sheet.getRow(ctr);
                    cell = row.getCell(col);
                    columndata.add(cell.toString());
                    ctr++;
                } catch (Exception e) {

                    isNull = true;

                }

            } while (isNull != true);

            ios.close();
        }
        catch (Exception e)
        {
            e.getMessage();
        }
        return columndata;
    }
    public int getRecordId(int entityId)
    {

        try {
            String query="select id from contract where  client_entity_seq_id=" +entityId;
            List<List<String>> queryData = new PostgreSQLJDBC().doSelect(query);
            if (!queryData.isEmpty() && !queryData.get(0).isEmpty()) {
                return Integer.parseInt(queryData.get(0).get(0));
            }
        } catch (Exception e) {
            logger.error("Exception while getting record Id from database");
        }
        return -1;
    }


    public int getRecordIdForConsumption(int entityId)
    {

        try {
            String query="select id from consumption_entity where  client_entity_seq_id=" +entityId;
            List<List<String>> queryData = new PostgreSQLJDBC().doSelect(query);
            if (!queryData.isEmpty() && !queryData.get(0).isEmpty()) {
                return Integer.parseInt(queryData.get(1).get(0));
            }
        } catch (Exception e) {
            logger.error("Exception while getting record Id from database");
        }
        return -1;
    }

    public static Map<String, List<String>> getAllSelectedStatusForEmailReminder(String createResponse, int entityTypeId)
    {
        Document doc= Jsoup.parse(createResponse);
        Map<String, List<String>> status=new HashMap<>();

        for (int i=0; i<doc.body().getElementsByClass("entityStakeholders").size(); i++){
            String a=doc.body().getElementsByClass("entityStakeholders").get(i).child(1).getElementsByAttribute("selected").text();
           List<String> arr=new ArrayList<>();
            for (int j=0; j< (doc.body().getElementsByClass("entityStakeholders").get(i).child(3).getElementsByAttribute("selected").size()); j++)
            {
                arr.add(doc.body().getElementsByClass("entityStakeholders").get(i).child(3).getElementsByAttribute("selected").get(j).text());
            }
            status.put(a,arr);
        }
        return  status;
    }

}