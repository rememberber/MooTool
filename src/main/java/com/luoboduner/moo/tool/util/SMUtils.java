//package com.luoboduner.moo.tool.util;
//
//import org.bouncycastle.jce.provider.BouncyCastleProvider;
//import org.bouncycastle.util.encoders.Hex;
//
//import javax.crypto.Cipher;
//import javax.crypto.KeyGenerator;
//import javax.crypto.SecretKey;
//import javax.crypto.spec.SecretKeySpec;
//import java.security.*;
//import java.security.spec.ECGenParameterSpec;
//import java.util.Base64;
//
///**
// * SM2/SM3/SM4 Utils
// * <p>
// * SM2: Chinese National Encryption Standard
// * SM3: Chinese National Hashing Standard
// * SM4: Chinese National Block Cipher Standard
// * <p>
// * Bouncy Castle is used as the provider for SM2/SM3/SM4
// */
//public class SMUtils {
//
//    static {
//        Security.addProvider(new BouncyCastleProvider());
//    }
//
//    // SM2 Key Pair Generation
//    public static KeyPair generateSM2KeyPair() throws Exception {
//        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
//        keyPairGenerator.initialize(new ECGenParameterSpec("sm2p256v1"), new SecureRandom());
//        return keyPairGenerator.generateKeyPair();
//    }
//
//    // SM2 Encryption
//    public static byte[] sm2Encrypt(PublicKey publicKey, byte[] data) throws Exception {
//        Cipher cipher = Cipher.getInstance("SM2", "BC");
//        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
//        return cipher.doFinal(data);
//    }
//
//    // SM2 Decryption
//    public static byte[] sm2Decrypt(PrivateKey privateKey, byte[] encryptedData) throws Exception {
//        Cipher cipher = Cipher.getInstance("SM2", "BC");
//        cipher.init(Cipher.DECRYPT_MODE, privateKey);
//        return cipher.doFinal(encryptedData);
//    }
//
//    // SM3 Hashing
//    public static String sm3Hash(String data) throws Exception {
//        MessageDigest digest = MessageDigest.getInstance("SM3", "BC");
//        byte[] hash = digest.digest(data.getBytes());
//        return Hex.toHexString(hash);
//    }
//
//    // SM4 Key Generation
//    public static SecretKey generateSM4Key() throws Exception {
//        KeyGenerator keyGenerator = KeyGenerator.getInstance("SM4", "BC");
//        keyGenerator.init(128, new SecureRandom());
//        return keyGenerator.generateKey();
//    }
//
//    // SM4 Encryption
//    public static byte[] sm4Encrypt(SecretKey key, byte[] data) throws Exception {
//        Cipher cipher = Cipher.getInstance("SM4/ECB/PKCS5Padding", "BC");
//        cipher.init(Cipher.ENCRYPT_MODE, key);
//        return cipher.doFinal(data);
//    }
//
//    // SM4 Decryption
//    public static byte[] sm4Decrypt(SecretKey key, byte[] encryptedData) throws Exception {
//        Cipher cipher = Cipher.getInstance("SM4/ECB/PKCS5Padding", "BC");
//        cipher.init(Cipher.DECRYPT_MODE, key);
//        return cipher.doFinal(encryptedData);
//    }
//
//    // Convert SM4 key to Hex String
//    public static String sm4KeyToHex(SecretKey key) {
//        return Hex.toHexString(key.getEncoded());
//    }
//
//    // Convert Hex String to SM4 key
//    public static SecretKey hexToSM4Key(String hexKey) {
//        byte[] keyBytes = Hex.decode(hexKey);
//        return new SecretKeySpec(keyBytes, "SM4");
//    }
//
//    public static void main(String[] args) {
//        try {
//            // SM2 Key Pair Generation
//            KeyPair keyPair = SMUtils.generateSM2KeyPair();
//            System.out.println("SM2 Key Pair Generated");
//            System.out.println("Public Key: " + Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded()));
//            System.out.println("Private Key: " + Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()));
//
//            // SM2 Encryption
//            String data = "Hello, SM2!";
//            byte[] encryptedData = SMUtils.sm2Encrypt(keyPair.getPublic(), data.getBytes());
//            System.out.println("SM2 Encrypted Data: " + Base64.getEncoder().encodeToString(encryptedData));
//
//            // SM2 Decryption
//            byte[] decryptedData = SMUtils.sm2Decrypt(keyPair.getPrivate(), encryptedData);
//            System.out.println("SM2 Decrypted Data: " + new String(decryptedData));
//
//            // SM3 Hashing
//            String hash = SMUtils.sm3Hash(data);
//            System.out.println("SM3 Hash: " + hash);
//
//            // SM4 Key Generation
//            SecretKey sm4Key = SMUtils.generateSM4Key();
//            System.out.println("SM4 Key Generated: " + SMUtils.sm4KeyToHex(sm4Key));
//
//            // SM4 Encryption
//            byte[] sm4EncryptedData = SMUtils.sm4Encrypt(sm4Key, data.getBytes());
//            System.out.println("SM4 Encrypted Data: " + Base64.getEncoder().encodeToString(sm4EncryptedData));
//
//            // SM4 Decryption
//            byte[] sm4DecryptedData = SMUtils.sm4Decrypt(sm4Key, sm4EncryptedData);
//            System.out.println("SM4 Decrypted Data: " + new String(sm4DecryptedData));
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}