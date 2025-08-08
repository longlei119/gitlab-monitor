package com.gitlab.metrics.util;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.*;

/**
 * EncryptionUtil测试类
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "app.security.encryption.key=dGVzdC1lbmNyeXB0aW9uLWtleS0yNTYtYml0cy1sb25nLWVub3VnaA=="
})
public class EncryptionUtilTest {
    
    @Autowired
    private EncryptionUtil encryptionUtil;
    
    private String testPlainText;
    private String testEmail;
    private String testSensitiveData;
    
    @Before
    public void setUp() {
        testPlainText = "Hello, World!";
        testEmail = "test@example.com";
        testSensitiveData = "sensitive-api-key-12345";
    }
    
    @Test
    public void testEncryptAndDecrypt() {
        // 测试基本加密解密功能
        String encrypted = encryptionUtil.encrypt(testPlainText);
        assertNotNull("加密结果不应为null", encrypted);
        assertNotEquals("加密后的数据应与原文不同", testPlainText, encrypted);
        
        String decrypted = encryptionUtil.decrypt(encrypted);
        assertEquals("解密后的数据应与原文相同", testPlainText, decrypted);
    }
    
    @Test
    public void testEncryptEmail() {
        // 测试邮箱加密
        String encryptedEmail = encryptionUtil.encrypt(testEmail);
        assertNotNull("加密邮箱不应为null", encryptedEmail);
        assertNotEquals("加密后的邮箱应与原文不同", testEmail, encryptedEmail);
        
        String decryptedEmail = encryptionUtil.decrypt(encryptedEmail);
        assertEquals("解密后的邮箱应与原文相同", testEmail, decryptedEmail);
    }
    
    @Test
    public void testEncryptSensitiveData() {
        // 测试敏感数据加密
        String encrypted = encryptionUtil.encrypt(testSensitiveData);
        assertNotNull("加密敏感数据不应为null", encrypted);
        assertNotEquals("加密后的敏感数据应与原文不同", testSensitiveData, encrypted);
        
        String decrypted = encryptionUtil.decrypt(encrypted);
        assertEquals("解密后的敏感数据应与原文相同", testSensitiveData, decrypted);
    }
    
    @Test
    public void testEncryptNullValue() {
        // 测试null值处理
        String encrypted = encryptionUtil.encrypt(null);
        assertNull("加密null值应返回null", encrypted);
        
        String decrypted = encryptionUtil.decrypt(null);
        assertNull("解密null值应返回null", decrypted);
    }
    
    @Test
    public void testEncryptEmptyString() {
        // 测试空字符串处理
        String emptyString = "";
        String encrypted = encryptionUtil.encrypt(emptyString);
        assertEquals("加密空字符串应返回空字符串", emptyString, encrypted);
        
        String decrypted = encryptionUtil.decrypt(emptyString);
        assertEquals("解密空字符串应返回空字符串", emptyString, decrypted);
    }
    
    @Test
    public void testEncryptionUniqueness() {
        // 测试相同明文的多次加密结果不同（由于随机IV）
        String encrypted1 = encryptionUtil.encrypt(testPlainText);
        String encrypted2 = encryptionUtil.encrypt(testPlainText);
        
        assertNotEquals("相同明文的多次加密结果应不同", encrypted1, encrypted2);
        
        // 但解密结果应相同
        String decrypted1 = encryptionUtil.decrypt(encrypted1);
        String decrypted2 = encryptionUtil.decrypt(encrypted2);
        
        assertEquals("解密结果应相同", decrypted1, decrypted2);
        assertEquals("解密结果应与原文相同", testPlainText, decrypted1);
    }
    
    @Test
    public void testLongTextEncryption() {
        // 测试长文本加密
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            longText.append("This is a long text for encryption testing. ");
        }
        
        String longTextStr = longText.toString();
        String encrypted = encryptionUtil.encrypt(longTextStr);
        assertNotNull("长文本加密结果不应为null", encrypted);
        
        String decrypted = encryptionUtil.decrypt(encrypted);
        assertEquals("长文本解密结果应与原文相同", longTextStr, decrypted);
    }
    
    @Test
    public void testSpecialCharactersEncryption() {
        // 测试特殊字符加密
        String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~中文测试αβγδε";
        String encrypted = encryptionUtil.encrypt(specialChars);
        assertNotNull("特殊字符加密结果不应为null", encrypted);
        
        String decrypted = encryptionUtil.decrypt(encrypted);
        assertEquals("特殊字符解密结果应与原文相同", specialChars, decrypted);
    }
    
    @Test(expected = RuntimeException.class)
    public void testDecryptInvalidData() {
        // 测试解密无效数据
        encryptionUtil.decrypt("invalid-encrypted-data");
    }
    
    @Test
    public void testGenerateKey() {
        // 测试密钥生成
        String key1 = EncryptionUtil.generateKey();
        String key2 = EncryptionUtil.generateKey();
        
        assertNotNull("生成的密钥不应为null", key1);
        assertNotNull("生成的密钥不应为null", key2);
        assertNotEquals("生成的密钥应不同", key1, key2);
        
        // 验证密钥长度（Base64编码的256位密钥）
        assertTrue("密钥长度应合适", key1.length() > 40);
        assertTrue("密钥长度应合适", key2.length() > 40);
    }
}