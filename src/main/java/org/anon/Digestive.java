package org.anon;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Digestive {
    public static String md5(String input) {
        return digest(input, "MD5");
    }

    public static String sha1(String input) {
        return digest(input, "SHA-1");
    }

    public static String sha256(String input) {
        return digest(input, "SHA-256");
    }

    private static String digest(String input, String method) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(method);
            byte[] result = messageDigest.digest(input.getBytes());
            StringBuilder hash = new StringBuilder();
            for (byte x : result) hash.append(String.format("%02x", x));
            return hash.toString();
        }
        catch (NoSuchAlgorithmException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
}
