package com.gitlab.metrics.converter;

import com.gitlab.metrics.util.EncryptionUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * EncryptedStringConverter测试类
 */
@RunWith(MockitoJUnitRunner.class)
public class EncryptedStringConverterTest {
    
    @Mock
    private EncryptionUtil encryptionUtil;
    
    @InjectMocks
    private EncryptedStringConverter converter;
    
    private String testPlainText;
    private String testEncryptedText;
    
    @Before
    public void setUp() {
        testPlainText = "test@example.com";
        testEncryptedText = "encrypted-data-base64";
    }
    
    @Test
    public void testConvertToDatabaseColumn() {
        // 模拟加密操作
        when(encryptionUtil.encrypt(testPlainText)).thenReturn(testEncryptedText);
        
        String result = converter.convertToDatabaseColumn(testPlainText);
        
        assertEquals("应返回加密后的数据", testEncryptedText, result);
        verify(encryptionUtil, times(1)).encrypt(testPlainText);
    }
    
    @Test
    public void testConvertToDatabaseColumnWithNull() {
        String result = converter.convertToDatabaseColumn(null);
        
        assertNull("null值应返回null", result);
        verify(encryptionUtil, never()).encrypt(any());
    }
    
    @Test
    public void testConvertToEntityAttribute() {
        // 模拟解密操作
        when(encryptionUtil.decrypt(testEncryptedText)).thenReturn(testPlainText);
        
        String result = converter.convertToEntityAttribute(testEncryptedText);
        
        assertEquals("应返回解密后的数据", testPlainText, result);
        verify(encryptionUtil, times(1)).decrypt(testEncryptedText);
    }
    
    @Test
    public void testConvertToEntityAttributeWithNull() {
        String result = converter.convertToEntityAttribute(null);
        
        assertNull("null值应返回null", result);
        verify(encryptionUtil, never()).decrypt(any());
    }
    
    @Test
    public void testRoundTripConversion() {
        // 模拟完整的加密解密流程
        when(encryptionUtil.encrypt(testPlainText)).thenReturn(testEncryptedText);
        when(encryptionUtil.decrypt(testEncryptedText)).thenReturn(testPlainText);
        
        // 转换到数据库列
        String encrypted = converter.convertToDatabaseColumn(testPlainText);
        assertEquals("加密结果应正确", testEncryptedText, encrypted);
        
        // 转换回实体属性
        String decrypted = converter.convertToEntityAttribute(encrypted);
        assertEquals("解密结果应与原文相同", testPlainText, decrypted);
        
        verify(encryptionUtil, times(1)).encrypt(testPlainText);
        verify(encryptionUtil, times(1)).decrypt(testEncryptedText);
    }
}