package com.sirionlabs.helper.OAuthHelper;

import java.util.Base64;

public class OAuthHelper {

    public String getBase64Encoder(String secret) {
        return Base64.getEncoder()
                .encodeToString(secret.getBytes());
    }

   public  String getBase64Decoder(String encoded) {
        byte[] actualByte = Base64.getDecoder()
                .decode(encoded);
        return new String(actualByte);
    }
}
