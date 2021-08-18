package com.sirionlabs.utils.commonUtils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.testng.annotations.Test;

import java.util.Date;

public class JWTEncode {
        public static String JWTEncode(String secretKey, String issuer, int expiryTimeMin) {
            String token = "";
            Algorithm algorithm = Algorithm.HMAC256(secretKey);
            token = JWT.create()
                    .withIssuer(issuer)
                    .withExpiresAt(new Date(System.currentTimeMillis() + (expiryTimeMin * 60 * 1000)))
                    .sign(algorithm);
            return token;
        }
        @Test
        public void JWTGenerator(){
            System.out.println("jwtsecretkey: "+ JWTEncode("jwtsecretkey","sirion",10));
            System.out.println("s3crtF9Z8K19ftE0Ces: " + JWTEncode("s3crtF9Z8K19ftE0Ces","sirion",15));
        }

    }
