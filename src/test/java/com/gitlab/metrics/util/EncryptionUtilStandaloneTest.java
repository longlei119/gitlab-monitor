package com.gitlab.metrics.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * EncryptionUtil独立测试类
 * 不依赖Spring上下文的测试
 */
public class EncryptionUtilStandaloneTest {
    
    @Test
    public void testGenerateKey() {
        // 测试密钥生成功能
        String key1 = EncryptionUtil.generateKey();
        String key2 = EncryptionUtil.generateKey();
        
        assertNotNull("生成的密钥不应为null", key1);
        assertNotNull("生成的密钥不应为null", key2);
        assertNotEquals("生成的密钥应不同", key1, key2);
        
        // 验证密钥长度（Base64编码的256位密钥）
        assertTrue("密钥长度应合适", key1.length() > 40);
        assertTrue("密钥长度应合适", key2.length() > 40);
        
        System.out.println("Generated key 1: " + key1);
        System.out.println("Generated key 2: " + key2);
    }
    
    @Test
    public void testEncryptionWithManualKey() {
        // 手动创建EncryptionUtil实例进行测试
        EncryptionUtil encryptionUtil = new EncryptionUtil();
        
        // 使用反射设置加密密钥
        try {
            java.lang.reflect.Field field = EncryptionUtil.class.getDeclaredField("encryptionKey");
            field.setAccessible(true);
            field.set(encryptionUtil, "dGVzdC1lbmNyeXB0aW9uLWtleS0yNTYtYml0cy1sb25nLWVub3VnaA==");
            
            String testData = "test@example.com";
            String encrypted = encryptionUtil.encrypt(testData);
            String decrypted = encryptionUtil.decrypt(encrypted);
            
            assertNotNull("加密结果不应为null", encrypted);
            assertNotEquals("加密后的数据应与原文不同", testData, encrypted);
            assertEquals("解密后的数据应与原文相同", testData, decrypted);
            
            System.out.println("Original: " + testData);
            System.out.println("Encrypted: " + encrypted);
            System.out.println("Decrypted: " + decrypted);
            
        } catch (Exception e) {
            fail("测试失败: " + e.getMessage());
        }
    }
}