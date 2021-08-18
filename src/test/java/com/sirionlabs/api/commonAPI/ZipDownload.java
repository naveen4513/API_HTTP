package com.sirionlabs.api.commonAPI;

import com.sirionlabs.utils.commonUtils.APIUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

public class ZipDownload extends APIUtils {

    public static HttpResponse hitZipDownload(String downloadId) {
        try {
            String queryString = "/zipDownload?id=" + downloadId;
            HttpGet request = new HttpGet(queryString);

            return APIUtils.getRequest(request);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean saveZipFile(HttpResponse response, String outputFilePath, String outputFileName) {
        try {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                BufferedInputStream bis = new BufferedInputStream(entity.getContent());
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputFilePath + "/" + outputFileName));
                int inByte;
                while ((inByte = bis.read()) != -1)
                    bos.write(inByte);
                bos.flush();
                bis.close();
                bos.close();
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }
}