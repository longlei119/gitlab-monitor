package com.gitlab.metrics.util;



/**
 * 简单的加密功能测试主类
 */
public class EncryptionTestMain {
    
    public static void main(String[] args) {
        try {
            System.out.println("=== 加密功能测试 ===");
            
            // 测试密钥生成
            System.out.println("\n1. 测试密钥生成:");
            String key1 = StandaloneEncryptionUtil.generateKey();
            String key2 = StandaloneEncryptionUtil.generateKey();
            System.out.println("Generated key 1: " + key1);
            System.out.println("Generated key 2: " + key2);
            System.out.println("Keys are different: " + !key1.equals(key2));
            
            // 测试加密解密
            System.out.println("\n2. 测试加密解密:");
            // 生成一个有效的256位密钥用于测试
            String testKey = StandaloneEncryptionUtil.generateKey();
            System.out.println("Using test key: " + testKey);
            StandaloneEncryptionUtil encryptionUtil = new StandaloneEncryptionUtil(testKey);
            
            String[] testData = {
                "test@example.com",
                "192.168.1.100",
                "sensitive-api-key-12345",
                "Hello, World! 你好世界！",
                ""
            };
            
            for (String data : testData) {
                if (data.isEmpty()) {
                    System.out.println("\n测试空字符串:");
                    String encrypted = encryptionUtil.encrypt(data);
                    String decrypted = encryptionUtil.decrypt(encrypted);
                    System.out.println("Original: [empty string]");
                    System.out.println("Encrypted: [" + encrypted + "]");
                    System.out.println("Decrypted: [" + decrypted + "]");
                    System.out.println("Success: " + data.equals(decrypted));
                } else {
                    System.out.println("\n测试数据: " + data);
                    String encrypted = encryptionUtil.encrypt(data);
                    String decrypted = encryptionUtil.decrypt(encrypted);
                    System.out.println("Original: " + data);
                    System.out.println("Encrypted: " + encrypted);
                    System.out.println("Decrypted: " + decrypted);
                    System.out.println("Success: " + data.equals(decrypted));
                }
            }
            
            // 测试null值处理
            System.out.println("\n3. 测试null值处理:");
            String nullEncrypted = encryptionUtil.encrypt(null);
            String nullDecrypted = encryptionUtil.decrypt(null);
            System.out.println("Null encrypted: " + nullEncrypted);
            System.out.println("Null decrypted: " + nullDecrypted);
            System.out.println("Null handling success: " + (nullEncrypted == null && nullDecrypted == null));
            
            // 测试加密唯一性
            System.out.println("\n4. 测试加密唯一性:");
            String testText = "test-uniqueness";
            String enc1 = encryptionUtil.encrypt(testText);
            String enc2 = encryptionUtil.encrypt(testText);
            String dec1 = encryptionUtil.decrypt(enc1);
            String dec2 = encryptionUtil.decrypt(enc2);
            System.out.println("Original: " + testText);
            System.out.println("Encrypted 1: " + enc1);
            System.out.println("Encrypted 2: " + enc2);
            System.out.println("Decrypted 1: " + dec1);
            System.out.println("Decrypted 2: " + dec2);
            System.out.println("Encryptions are different: " + !enc1.equals(enc2));
            System.out.println("Decryptions are same: " + dec1.equals(dec2));
            System.out.println("Decryptions match original: " + (testText.equals(dec1) && testText.equals(dec2)));
            
            System.out.println("\n=== 测试完成 ===");
            
        } catch (Exception e) {
            System.err.println("测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}