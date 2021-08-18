package com.sirionlabs.utils.commonUtils;

import com.google.inject.internal.cglib.core.$ClassNameReader;
import com.jcraft.jsch.*;
import com.sirionlabs.config.ConfigureEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.Vector;

public class SCPUtils {

    private String user, password = null, host;
    private int port;
    private static final Logger logger = LoggerFactory.getLogger(SCPUtils.class);
    private String key;
    private ChannelSftp sftpChannel;
    private Channel channel;
    private Session session;
    private String databaseTestPropertyFileName = "/home/tomcat7/appProperties/trunk/database-test.properties";

    public SCPUtils(){

        this.host = ConfigureEnvironment.getEnvironmentProperty("scheduler host");
        this.user = ConfigureEnvironment.getEnvironmentProperty("scheduler user");
        this.password = ConfigureEnvironment.getEnvironmentProperty("scheduler password");
        this.port = Integer.parseInt(ConfigureEnvironment.getEnvironmentProperty("scheduler port"));

        try {
            JSch jsch = new JSch();
            session = jsch.getSession(user, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            logger.info("Establishing Connection...");
            session.connect();
            logger.info("Connection established.");
            logger.info("Creating SFTP Channel.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SCPUtils(String host, String user, String pass, int port) {
        this.host = host;
        this.password = pass;
        this.user = user;
        this.port = port;


        try {
            JSch jsch = new JSch();
            session = jsch.getSession(user, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            logger.info("Establishing Connection...");
            session.connect();
            logger.info("Connection established.");
            logger.info("Crating SFTP Channel.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public SCPUtils(String host, String user, String key, int port,boolean withKey) {
        this.host = host;
        this.key = key;
        this.user = user;
        this.port = port;

        try {
            JSch jsch = new JSch();
            jsch.addIdentity(key);
            session = jsch.getSession(user, host, port);
            session.setConfig("PreferredAuthentications", "publickey,keyboard-interactive,password");
            session.setConfig("StrictHostKeyChecking", "no");
            logger.info("Establishing Connection...");
            session.connect();
            logger.info("Connection established.");
            logger.info("Crating SFTP Channel.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean uploadFile(String uploadFile,String serverDirectory){
        return uploadFile(uploadFile,serverDirectory,new File(uploadFile).getName(),false);
    }

    public boolean uploadFile(String uploadFile,String serverDirectory,boolean overwrite){
        return uploadFile(uploadFile,serverDirectory,new File(uploadFile).getName(),overwrite);
    }

    public boolean uploadFile(String uploadFile,String serverDirectory,String serverFileName){
        return uploadFile(uploadFile,serverDirectory,serverFileName,false);
    }

    public boolean uploadFile(String uploadFile, String serverDirectory,String serverFileName, boolean overwrite){

        try {
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            logger.info("SFTP Channel created.");

            sftpChannel.cd(serverDirectory);

            File file = new File(uploadFile);
            if(overwrite)
                sftpChannel.put(new FileInputStream(file), serverFileName, ChannelSftp.OVERWRITE);
            else
                sftpChannel.put(new FileInputStream(file), serverFileName);

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean downloadDatabaseTestFile(String outputFileName, String outputFilePath){
        return downloadFile(databaseTestPropertyFileName,outputFileName,outputFilePath);
    }

    public boolean downloadFile(String serverFilePath,String outputFileName, String outputFilePath){
        StringBuilder remoteFile = new StringBuilder(serverFilePath);

        try {
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            logger.info("SFTP Channel created.");

            InputStream inputStream = sftpChannel.get(remoteFile.toString());


            File outputFile = createDownloadFile(outputFilePath + "/" + outputFileName);

            BufferedInputStream bis = new BufferedInputStream(inputStream);
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputFile));
            int inByte;
            while ((inByte = bis.read()) != -1)
                bos.write(inByte);
            bos.flush();
            bis.close();
            bos.close();

            try (Scanner scanner = new Scanner(new InputStreamReader(inputStream))) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    logger.info(line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean downloadExcelFile(String requestId, String outputFileName, String outputFilePath) {

        StringBuilder remoteFile = new StringBuilder("/data/temp-session/bulktask/" + requestId);

        try {


            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            logger.info("SFTP Channel created.");

            Vector vector = sftpChannel.ls(remoteFile.toString());
            remoteFile.append("/");

            for (Object object : vector) {
                if (((ChannelSftp.LsEntry) object).getFilename().contains("xlsm") ||
                        ((ChannelSftp.LsEntry) object).getFilename().contains("xls")) {
                    remoteFile.append(((ChannelSftp.LsEntry) object).getFilename());
                }
            }

            InputStream inputStream = sftpChannel.get(remoteFile.toString());


            File outputFile = createDownloadFile(outputFilePath + "/" + outputFileName);

            BufferedInputStream bis = new BufferedInputStream(inputStream);
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputFile));
            int inByte;
            while ((inByte = bis.read()) != -1)
                bos.write(inByte);
            bos.flush();
            bis.close();
            bos.close();

            try (Scanner scanner = new Scanner(new InputStreamReader(inputStream))) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    logger.info(line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public String runCommand(String command, boolean showResponseLog) {

        logger.info("Executing command {}",command);
        try {
            channel = session.openChannel("shell");

            channel.setInputStream(new ByteArrayInputStream(command.getBytes(StandardCharsets.UTF_8)));
            channel.setOutputStream(System.out);
            InputStream in = channel.getInputStream();
            StringBuilder outBuff = new StringBuilder();
            int exitStatus = -1;

            channel.connect();
            channel.run();
            while (true) {
                for (int c; ((c = in.read()) >= 0);) {
                    outBuff.append((char) c);
                    //System.out.println(outBuff.toString());
                }

                if (channel.isClosed()) {
                    if (in.available() > 0) continue;
                    exitStatus = channel.getExitStatus();
                    break;
                }
            }
            channel.disconnect();

            // print exit status
            //logger.info ("Exit status of the execution: " + exitStatus);
//            if ( exitStatus == 0 ) {
//                logger.info (" (OK)\n");
//            } else {
//                logger.info (" (Not OK)\n");
//            }

            // print the buffer's contents
            if(showResponseLog)
                logger.info("Response for command [{}], [****************{}**************]",command,outBuff.toString());

           return outBuff.toString();

        } catch (IOException | JSchException ioEx) {
            System.err.println(ioEx.toString());
        }

        return null;
    }

    private File createDownloadFile(String sOutputFile) throws Exception {
        File outputFile = new File(sOutputFile);
        if (outputFile.exists()) {
            logger.info("The file : [ {} ] already exist , so deleting and recreating the file.", sOutputFile);
            outputFile.delete();
            outputFile.createNewFile();
        } else {
            logger.info("The file : [ {} ] does not exist , so creating new file.", sOutputFile);
            outputFile.createNewFile();
        }
        return outputFile;
    }

    public boolean getFileFromRemoteServerToLocalServer(String remoteFilePath,String remoteFileName,String localFilePath,String localFileName) {

        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(user, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            logger.info("Establishing Connection...");
            session.connect();
            logger.info("Connection established.");
            logger.info("Crating SFTP Channel.");
            ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            logger.info("SFTP Channel created.");

            String sourceFile = remoteFilePath + "/" + remoteFileName;
            String destinationFile = localFilePath + localFileName;
            File outputFile = new File(localFilePath + "/" + localFileName);

            if (outputFile.exists()) {
                logger.debug("The file : [ {} ] already exist , so deleting and recreating the file.", outputFile);
                outputFile.delete();
            }

            sftpChannel.get(sourceFile,destinationFile);

            if (!outputFile.exists()) {
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean getFileFromRemoteServerToLocalServer(String remoteFilePath,String localFilePath,String localFileName) {

        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(user, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            logger.info("Establishing Connection...");
            session.connect();
            logger.info("Connection established.");
            logger.info("Crating SFTP Channel.");
            ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            logger.info("SFTP Channel created.");

            String sourceFile = remoteFilePath;
            String destinationFile = localFilePath + localFileName;
            File outputFile = new File(localFilePath + "/" + localFileName);

            if (outputFile.exists()) {
                logger.debug("The file : [ {} ] already exist , so deleting and recreating the file.", outputFile);
                outputFile.delete();
            }

            sftpChannel.get(sourceFile,destinationFile);

            if (!outputFile.exists()) {
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean runChmodCommand(String permission,String remoteFilePath) {

        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(user, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            logger.info("Establishing Connection...");
            session.connect();
            logger.info("Connection established.");
            logger.info("Creating SFTP Channel.");
            ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            logger.info("SFTP Channel created.");
            sftpChannel.chmod(Integer.parseInt(permission,8),remoteFilePath);
//            channel.disconnect();
        } catch (Exception e) {
//            channel.disconnect();
            e.printStackTrace();
            return false;
        }

        return true;
    }

    public void closeSession(){
        session.disconnect();
    }
}

