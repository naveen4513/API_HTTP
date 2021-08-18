package com.sirionlabs.utils.commonUtils;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Created by shivashish on 28/6/17.
 */
public class FileUtils {


    private final static Logger logger = LoggerFactory.getLogger(FileUtils.class);
    Date currentDate;
    SimpleDateFormat sdfDate;
    private static List<String> filesListInDir = new ArrayList<String>();

    public FileUtils() {
        currentDate = new Date();
        sdfDate = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
    }

    public static Boolean deleteFile(String path) {

        File file = new File(path);

        if (file.delete()) {
            logger.info("File deleted successfully");
            return true;
        } else {
            logger.error("Failed to delete the file");
            return false;
        }
    }

    public static Boolean clearDirectory(String path) {
        try {
            org.apache.commons.io.FileUtils.cleanDirectory(new File(path));
        }catch (Exception e){
            return false;
        }
        return true;
    }

    // this function will create a Directory with Given Path iff not Exist
    // @Generic
    public boolean createDirIfNotExist(String Path) {
        File directory = new File(Path);
        boolean success = false;
        if (directory.exists()) {
            logger.debug("Directory already exists ...");
            return true;
        } else {
            logger.info("Directory : {} not exists, creating now", Path);
            success = directory.mkdirs();
            if (success) {
                logger.info("Successfully created new directory : {}", Path);
                return true;
            } else {
                logger.info("Failed to create new directory: {}", Path);
                return false;
            }
        }
    }

    // this function will create a File with Given Path in not Exist
    // @Generic
    boolean createFileIfNotExist(String Path) throws IOException {
        File f = new File(Path);
        boolean success = false;
        if (f.exists()) {
            logger.info("File already exists");
            return true;
        } else {
            logger.info("File : {} not exists, creating now", Path);
            success = f.createNewFile();
            if (success) {
                logger.info("Successfully created new File : {}", Path);
                return true;
            } else {
                logger.info("Failed to create new File: {}", Path);
                return false;
            }
        }

    }

    // this will dump the response of get rating API to given file which can be verified later
    // @generic
    public void dumpResponseInFile(String filename, String output) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            file.createNewFile();
        }

        BufferedWriter bw = null;
        FileWriter fw = null;

        try {
            fw = new FileWriter(filename);
            bw = new BufferedWriter(fw);
            bw.write(output);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bw != null)
                    bw.close();
                if (fw != null)
                    fw.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    // this method will return all the data stored in file
    // @generic
    public String getDataInFile(String filename) throws IOException {
        File file = new File(filename);
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        fis.read(data);
        fis.close();

        String str = new String(data, "UTF-8");
        return str;
    }

    // this function will save the content of Message in Given file Location
    // MailBox Automation Specific
    boolean saveMessageContentInFile(File file, Message message) throws IOException, MessagingException {
        try {
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write("DATE: " + message.getSentDate().toString() + "\n");
            bw.write("<div>");
            bw.write("FROM: " + message.getFrom()[0].toString() + "\n");
            bw.write("<div>");
            bw.write("SUBJECT: " + message.getSubject().toString() + "\n");
            bw.write("<div>");
            bw.write("TO: " + message.getAllRecipients()[0].toString() + "\n");
            bw.write("<div>");
            bw.write("<div>");
            if (message.isMimeType("multipart/*")) {
                MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
                String result = getMessageContentFromMimeMultipart(mimeMultipart);
                bw.write(result);
            }
//			 else if (message.isMimeType("text/plain")) {
//				bw.write(message.getContent().toString());
//			}
            else {
                bw.write(message.getContent().toString());
            }
            bw.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            logger.info("IO exception happened");
            return false;
        }
    }

    // function that will return text from Mime Multipart type Message
    // MailBox Automation Specific
    private String getMessageContentFromMimeMultipart(
            MimeMultipart mimeMultipart) throws MessagingException, IOException {
        String result = "";
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result = result + "\n" + bodyPart.getContent();
                break; // without break same text appears twice in my tests
            } else if (bodyPart.isMimeType("text/html")) {
                String html = (String) bodyPart.getContent();
                result = result + "\n" + html;
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                result = result + getMessageContentFromMimeMultipart((MimeMultipart) bodyPart.getContent());
            }
        }
        return result;
    }

    public Boolean writeResponseIntoFile(HttpResponse response, String outputFile) {
        HttpEntity entity = response.getEntity();
        Boolean status = true;
        try {
            if (entity != null) {
                BufferedInputStream bis = new BufferedInputStream(entity.getContent());
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputFile));
                int inByte;
                while ((inByte = bis.read()) != -1)
                    bos.write(inByte);
                bis.close();
                bos.close();
            }
        } catch (Exception e) {
            status = false;
            logger.error("Exception occurred while dumping response into file {} {}\n", outputFile, e.getMessage());
        }
        return status;
    }

    public Boolean createNewFolder(String outputPath, String folderName) {
        Boolean status = true;
        try {
            File theDir = new File(outputPath + "/" + folderName);

            // if the directory does not exist, create it
            if (!theDir.exists()) {
                logger.debug("creating directory: ", theDir.getName());
                theDir.mkdir();
            }
        } catch (Exception e) {
            status = false;
            logger.error("Exception occurred while creating new folder. {}", e.getMessage());
        }
        return status;
    }

    public static Boolean uploadFileOnSFTPServer(String host, Integer port, String username, String password, String targetDir, File fileToUpload) {
        Boolean flag = false;
        //String type = "sftp";

        Session session = null;
        Channel channel = null;
        ChannelSftp channelSftp = null;
        logger.info("preparing the host information for sftp.");
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            logger.info("Host connected.");
            channel = session.openChannel("sftp");
            channel.connect();
            logger.info("sftp channel opened and connected.");
            channelSftp = (ChannelSftp) channel;
            channelSftp.cd(targetDir);
            //File f = new File(fileName);
            channelSftp.put(new FileInputStream(fileToUpload), fileToUpload.getName());
            flag = true;
            logger.info("File transferred successfully to host.");
        } catch (Exception ex) {
            logger.error("Exception occurred while transfering the file to sftp server. error : {}", ex.getMessage());
        } finally {

            channelSftp.exit();
            logger.info("sftp Channel exited.");
            channel.disconnect();
            logger.info("Channel disconnected.");
            session.disconnect();
            logger.info("Host Session disconnected.");
        }
        return flag;
    }

    /*function to get local copy of file hosted on sftp server*/
    public static Boolean getFileFromSFTPServer(String host, Integer port, String username, String password, String remoteFile, String localFile) {
        Boolean flag = false;
        //String type = "sftp";

        Session session = null;
        Channel channel = null;
        ChannelSftp channelSftp = null;
        logger.info("preparing the host information for sftp.");
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            logger.info("Host connected.");
            channel = session.openChannel("sftp");
            channel.connect();
            logger.info("sftp channel opened and connected.");
            channelSftp = (ChannelSftp) channel;
            //channelSftp.cd(targetDir);

            channelSftp.get(remoteFile, localFile);
            flag = true;
            logger.info("File saved successfully from {} to {}", remoteFile, localFile);
        } catch (Exception ex) {
            logger.error("Exception occurred while getting the file from sftp server. error : {}", ex.getMessage());
        } finally {

            channelSftp.exit();
            logger.info("sftp Channel exited.");
            channel.disconnect();
            logger.info("Channel disconnected.");
            session.disconnect();
            logger.info("Host Session disconnected.");
        }
        return flag;
    }

    public Map<String, String> ReadKeyValueFromFile(String filepath, String delimtier, String section) {

        String str = null;
        Map<String, String> formdata = new HashMap<String, String>();
        try {
            File file = new File(filepath);

            BufferedReader br = new BufferedReader(new FileReader(file));

            String line;
            int count = 0;
            line = br.readLine();
            line.trim();
            do {
                if (line.equalsIgnoreCase("#" + section)) {
                    count = count + 1;
                    line = br.readLine();
                }
                if (count == 1) {

                    String[] columns = line.split(delimtier, 2);
                    String key = columns[0].trim();
                    String val = columns[1].trim();
                    formdata.put(key, val);
                }

            } while ((line = br.readLine()) != null && count <= 1);

        } catch (Exception e) {
            logger.error("Exception occurred while reading from file", e.getMessage());
        }
        return formdata;
    }

    public static String getFileNameWithoutExtension(String fileName) {
        String fileNameWithoutExtension = null;

        try {
            if (fileName.contains(".") && fileName.lastIndexOf(".") != 0)
                fileNameWithoutExtension = fileName.substring(0, fileName.lastIndexOf("."));
        } catch (Exception e) {
            logger.error("Exception while getting Extension of File {}. {}", fileName, e.getStackTrace());
        }
        return fileNameWithoutExtension;
    }

    public static String getFileExtension(String fileName) {
        String fileExtension = null;

        try {
            if (fileName.contains(".") && fileName.lastIndexOf(".") != 0)
                fileExtension = fileName.substring(fileName.lastIndexOf(".") + 1);
        } catch (Exception e) {
            logger.error("Exception while getting Extension of File {}. {}", fileName, e.getStackTrace());
        }
        return fileExtension;
    }

    public static void saveResponseInFile(String fileName, String response) {
        saveResponseInFile("src/test", fileName, response);
    }

    public static void saveResponseInFile(String filePath, String fileName, String response) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath + "/" + fileName));
            writer.write(response);

            writer.close();
        } catch (Exception e) {
            logger.error("Exception while saving Response in File [{}]. {}", fileName, e.getStackTrace());
        }
    }

    public static boolean fileExists(String filePath, String fileName) {
        File file = new File(filePath + "/" + fileName);
        return file.exists();
    }

    public static boolean copyFile(String originalFilePath, String originalFileName, String copiedFilePath, String copiedFileName) {
        File originalFile = new File(originalFilePath + "/" + originalFileName);
        File copiedFile = new File(copiedFilePath + "/" + copiedFileName);

        try {
            org.apache.commons.io.FileUtils.copyFile(originalFile, copiedFile);
            return copiedFile.exists();
        } catch (Exception e) {
            logger.error("Exception while Copying Original File {} to Copied File {}. {}", originalFile.getAbsolutePath(), copiedFile.getAbsolutePath(), e.getStackTrace());
        }

        return false;
    }

    public static void unzip(String zipFilePath, String destDir) {

        File dir = new File(destDir);
        // create output directory if it doesn't exist
        if(!dir.exists()) dir.mkdirs();
        FileInputStream fis;
        //buffer for read and write data to file
        byte[] buffer = new byte[1024];
        try {
            fis = new FileInputStream(zipFilePath);
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry ze = zis.getNextEntry();
            while(ze != null){
                String fileName = ze.getName();
                File newFile = new File(destDir + File.separator + fileName);
                System.out.println("Unzipping to "+newFile.getAbsolutePath());
                //create directories for sub directories in zip
                new File(newFile.getParent()).mkdirs();
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                //close this ZipEntry
                zis.closeEntry();
                ze = zis.getNextEntry();
            }
            //close last ZipEntry
            zis.closeEntry();
            zis.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * This method zips the directory
     * @param dir
     * @param zipDirName
     */
    public static void zipDirectory(File dir, String zipDirName) {
        try {
            populateFilesList(dir);
            //now zip files one by one
            //create ZipOutputStream to write to the zip file
            FileOutputStream fos = new FileOutputStream(zipDirName);
            ZipOutputStream zos = new ZipOutputStream(fos);
            for(String filePath : filesListInDir){
                System.out.println("Zipping "+filePath);
                //for ZipEntry we need to keep only relative file path, so we used substring on absolute path
                ZipEntry ze = new ZipEntry(filePath.substring(dir.getAbsolutePath().length()+1, filePath.length()));
                zos.putNextEntry(ze);
                //read the file and write to ZipOutputStream
                FileInputStream fis = new FileInputStream(filePath);
                byte[] buffer = new byte[1024];
                int len;
                while ((len = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }
                zos.closeEntry();
                fis.close();
            }
            zos.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method populates all the files in a directory to a List
     * @param dir
     * @throws IOException
     */
    private static void populateFilesList(File dir) throws IOException {
        File[] files = dir.listFiles();
        for(File file : files){
            if(file.isFile()) filesListInDir.add(file.getAbsolutePath());
            else populateFilesList(file);
        }
    }

    /**
     * This method compresses the single file to zip format
     * @param file
     * @param zipFileName
     */
    public static void zipSingleFile(File file, String zipFileName) {
        try {
            //create ZipOutputStream to write to the zip file
            FileOutputStream fos = new FileOutputStream(zipFileName);
            ZipOutputStream zos = new ZipOutputStream(fos);
            //add a new Zip Entry to the ZipOutputStream
            ZipEntry ze = new ZipEntry(file.getName());
            zos.putNextEntry(ze);
            //read the file and write to ZipOutputStream
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }

            //Close the zip entry to write to zip file
            zos.closeEntry();
            //Close resources
            zos.close();
            fis.close();
            fos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public static void zipDir(String sourceFile,String destinationFile){

        //String sourceFile = "src\\test\\resources\\TestConfig\\ServiceLevel\\SLAutomation\\BulkUploadRawDataFilesUnZipped\\dft-BulkUploadRawData-05-Jun-2019";
        try {
            FileOutputStream fos = new FileOutputStream(destinationFile);
            ZipOutputStream zipOut = new ZipOutputStream(fos);
            File fileToZip = new File(sourceFile);

            zipFile(fileToZip, fileToZip.getName(), zipOut);
            zipOut.close();
            fos.close();
        }catch (Exception e){
            logger.error("Exception while zipping Directory");
        }
    }
    private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }

    public static Boolean deleteFile(String path , String fileName) {

        try {

            File file = new File(path + "/" + fileName);

            if (file.delete()) {
                logger.info("File deleted successfully");
                return true;
            } else {
                logger.error("Failed to delete the file");
                return false;
            }
        }catch (Exception e){
            logger.error("Exception while deleting file "  + e.getMessage());
            return false;
        }
    }

    public Boolean dumpDownloadListWithDataResponseIntoFile(HttpResponse response, String outputFilePath,String outputFileFormatForDownloadListWithData, String featureName, String entityName) {
        String outputFile = null;
        Boolean status = false;

        FileUtils fileUtil = new FileUtils();
        outputFile = outputFilePath + "/" + featureName  + outputFileFormatForDownloadListWithData;
        status = fileUtil.writeResponseIntoFile(response, outputFile);
        if (status) {
            logger.info("DownloadListWithData file generated at {}", outputFile);
        }
        return status;
    }

    public static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    public ArrayList<String> getPDFFileContent(String pdfFile){

        ArrayList<String> pdfFileContent = new ArrayList<>();
        try (PDDocument document = PDDocument.load(new File(pdfFile))) {

            document.getClass();

            if (!document.isEncrypted()) {

                PDFTextStripperByArea stripper = new PDFTextStripperByArea();
                stripper.setSortByPosition(true);

                PDFTextStripper tStripper = new PDFTextStripper();

                String pdfFileInText = tStripper.getText(document);

                // split by whitespace
                String lines[] = pdfFileInText.split("\\r?\\n");
                for (String line : lines) {
                    pdfFileContent.add(line);
                }
            }

        }catch (Exception e){
            logger.error("Exception while fetching PDF Content");
        }
        return pdfFileContent;
    }

    // Code to create directories recursively
    static void file(String md, String path, int depth,File src) throws IOException {
        // md stores the starting path
        // each character in path represents new
        // directory depth stores the number
        // ofdirectories to be created
        // terminating condition
        if (depth == 0)
            return;
        // decrementing the depth by 1
        depth -= 1;
        // checking if the path exists
        if (path.length() == 0)
            System.out.println("Path does not exist");
            // execute if the path has more directories
        else {
            // appending the next directory
            // would be md = md + "\\" +
            // path.charAt(0) for windows
            md = md + "/" + path.charAt(0);
            // removing the first character
            // from path string
            path = path.substring(1);
            // creating File object
            File f = new File(md);
            // if the directory already exists
            if (f.exists()) {
                System.out.println("The Directory " + "already exists");
            }
            else {
                // creating the directory
                boolean val = f.mkdir();
                copyAllFilesToAnotherDirectory(src,f);
                if (val)
                    System.out.println(md + " created" + " successfully");
                else
                    System.out.println("Unable to " + "create Directory");
            }
        }
        // recursive call
        file(md, path, depth, src);
    }
    // Driver method ob.file("/home/naveenkg/Desktop", "abcd", 4, srcfilelocation);

    public static void copyAllFilesToAnotherDirectory(File srcDir, File destDir) throws IOException {
        try {
            org.apache.commons.io.FileUtils.copyDirectory(srcDir, destDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
