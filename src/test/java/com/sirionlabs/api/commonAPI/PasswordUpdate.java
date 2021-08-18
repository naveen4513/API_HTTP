package com.sirionlabs.api.commonAPI;

import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.api.APIValidator;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;


public class PasswordUpdate extends TestAPIBase {

    private static final String CHARSET = "UTF-8";
    private static final String RSA_ALGORITHM = "RSA";
    private static final String queryString="/userPassword/update";


    public APIValidator postUpdatePassword(String payload, String authToken)
    {
        HashMap<String,String> headers = new HashMap<>();
        headers.put("Authorization", authToken);
        headers.put("X-Requested-With", "XMLHttpRequest");
        headers.put("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
        headers.put("Accept-Encoding", "gzip, deflate");
        headers.put("Accept", "application/json, text/javascript, */*; q=0.01");
        headers.put("Content-Type", "application/json;charset=UTF-8");

        String hostUrl = ConfigureEnvironment.getEnvironmentProperty("Scheme") + "://" + ConfigureEnvironment.getEnvironmentProperty("Host") + ":" +
                ConfigureEnvironment.getEnvironmentProperty("Port");

        return  executor.postWithoutMandatoryHeaders(hostUrl,queryString,headers,payload,null);

 }


    public APIValidator postUpdatePasswordInvalidPath(String payload, String authToken)
    {
        String invalidQueryString="/userPassword/updateee";
        HashMap<String,String> headers = new HashMap<>();
        headers.put("Authorization", authToken);
        headers.put("X-Requested-With", "XMLHttpRequest");
        headers.put("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
        headers.put("Accept-Encoding", "gzip, deflate");
        headers.put("Accept", "application/json, text/javascript, */*; q=0.01");
        headers.put("Content-Type", "application/json;charset=UTF-8");

        String hostUrl = ConfigureEnvironment.getEnvironmentProperty("Scheme") + "://" + ConfigureEnvironment.getEnvironmentProperty("Host") + ":" +
                ConfigureEnvironment.getEnvironmentProperty("Port");

        return  executor.postWithoutMandatoryHeaders(hostUrl,invalidQueryString,headers,payload,null);

    }

    public APIValidator updatePasswordInvalidMethod(String payload, String authToken)
    {
        HashMap<String,String> headers = new HashMap<>();
        headers.put("Authorization", authToken);
        headers.put("X-Requested-With", "XMLHttpRequest");
        headers.put("X-CSRF-TOKEN", ConfigureEnvironment.getEnvironmentProperty("X-CSRF-TOKEN"));
        headers.put("Accept-Encoding", "gzip, deflate");
        headers.put("Accept", "application/json, text/javascript, */*; q=0.01");
        headers.put("Content-Type", "application/json;charset=UTF-8");

        String hostUrl = ConfigureEnvironment.getEnvironmentProperty("Scheme") + "://" + ConfigureEnvironment.getEnvironmentProperty("Host") + ":" +
                ConfigureEnvironment.getEnvironmentProperty("Port");

        return  executor.getWithoutAuthorization(hostUrl,queryString,headers);

    }





    private  HashMap<String,String > getHeader()
    {
        return ApiHeaders.getDefaultLegacyHeaders();
    }



    public  String getPayload(String newPassword, String publicKey) throws GeneralSecurityException, IOException {
        String payload = "";
       newPassword = publicEncrypt(newPassword, (RSAPublicKey) loadPublicKey(publicKey));
       String repeatPassword = newPassword;

     payload = "{\"newPassword\":\""+newPassword+"\",\"repeatPassword\":\""+repeatPassword+"\"}";
       return  payload;
    }

    public static String publicEncrypt(String data, RSAPublicKey publicKey){
        try{
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            return Base64.encodeBase64URLSafeString(rsaSplitCodec(cipher, Cipher.ENCRYPT_MODE, data.getBytes(CHARSET), publicKey.getModulus().bitLength()));
        }catch(Exception e){
            throw new RuntimeException("Encryption string [" + data + "] encountered an exception", e);
        }

    }

    private static byte[] rsaSplitCodec(Cipher cipher, int opmode, byte[] datas, int keySize){
        int maxBlock = 0;
        if(opmode == Cipher.DECRYPT_MODE){
            maxBlock = keySize / 8;
        }else{
            maxBlock = keySize / 8 - 11;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        byte[] buff;
        int i = 0;
        try{
            while(datas.length > offSet){
                if(datas.length-offSet > maxBlock){
                    buff = cipher.doFinal(datas, offSet, maxBlock);
                }else{
                    buff = cipher.doFinal(datas, offSet, datas.length-offSet);
                }
                out.write(buff, 0, buff.length);
                i++;
                offSet = i * maxBlock;
            }
        }catch(Exception e){
            throw  new RuntimeException("An exception occurred while encrypting and decrypting the data with a value of ["+maxBlock+"]", e);
        }
        byte[] resultDatas = out.toByteArray();
        IOUtils.closeQuietly(out);
        return resultDatas;
    }

    public static Key loadPublicKey(String stored) throws GeneralSecurityException, IOException
    {
        byte[] data = Base64.decodeBase64(stored);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(data);
        KeyFactory fact = KeyFactory.getInstance("RSA");
        return fact.generatePublic(spec);

    }

}
