package com.gitlab.metrics.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * SSL证书生成工具类
 * 用于开发环境生成自签名证书
 */
@Component
public class SslCertificateGenerator {
    
    private static final Logger logger = LoggerFactory.getLogger(SslCertificateGenerator.class);
    
    /**
     * 生成自签名SSL证书用于开发环境
     * 
     * @param keystorePath 密钥库文件路径
     * @param keystorePassword 密钥库密码
     * @param alias 证书别名
     * @param commonName 通用名称 (CN)
     * @param validityDays 证书有效期（天）
     */
    public void generateSelfSignedCertificate(String keystorePath, String keystorePassword, 
                                            String alias, String commonName, int validityDays) {
        try {
            // 生成密钥对
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            
            // 创建证书
            X509Certificate certificate = createSelfSignedCertificate(keyPair, commonName, validityDays);
            
            // 创建密钥库
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);
            
            // 添加私钥和证书到密钥库
            keyStore.setKeyEntry(alias, keyPair.getPrivate(), keystorePassword.toCharArray(), 
                               new X509Certificate[]{certificate});
            
            // 保存密钥库到文件
            try (FileOutputStream fos = new FileOutputStream(keystorePath)) {
                keyStore.store(fos, keystorePassword.toCharArray());
            }
            
            logger.info("Self-signed SSL certificate generated successfully: {}", keystorePath);
            logger.info("Certificate details: CN={}, Valid for {} days", commonName, validityDays);
            
        } catch (Exception e) {
            logger.error("Failed to generate SSL certificate", e);
            throw new RuntimeException("Failed to generate SSL certificate", e);
        }
    }
    
    /**
     * 创建自签名X509证书
     */
    private X509Certificate createSelfSignedCertificate(KeyPair keyPair, String commonName, int validityDays) 
            throws Exception {
        
        // 注意：这里使用简化的证书生成方式
        // 在实际生产环境中，应该使用专业的证书颁发机构(CA)签发的证书
        
        // 由于Java标准库不直接支持证书生成，这里提供一个框架
        // 实际实现需要使用第三方库如Bouncy Castle
        
        throw new UnsupportedOperationException(
            "Certificate generation requires Bouncy Castle library. " +
            "For production use, please use certificates from a trusted CA. " +
            "For development, you can use keytool command: " +
            "keytool -genkeypair -alias gitlab-metrics -keyalg RSA -keysize 2048 " +
            "-storetype PKCS12 -keystore keystore.p12 -validity " + validityDays + " " +
            "-dname \"CN=" + commonName + ",OU=Development,O=GitLab Metrics,C=CN\""
        );
    }
    
    /**
     * 验证证书是否即将过期
     * 
     * @param keystorePath 密钥库路径
     * @param keystorePassword 密钥库密码
     * @param alias 证书别名
     * @param warningDays 提前警告天数
     * @return true如果证书即将过期
     */
    public boolean isCertificateExpiringSoon(String keystorePath, String keystorePassword, 
                                           String alias, int warningDays) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(getClass().getResourceAsStream("/" + keystorePath), 
                         keystorePassword.toCharArray());
            
            X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
            if (certificate == null) {
                logger.warn("Certificate not found with alias: {}", alias);
                return true;
            }
            
            Date expirationDate = certificate.getNotAfter();
            LocalDateTime expiration = expirationDate.toInstant()
                                                   .atZone(ZoneId.systemDefault())
                                                   .toLocalDateTime();
            
            LocalDateTime warningDate = LocalDateTime.now().plusDays(warningDays);
            
            boolean expiringSoon = expiration.isBefore(warningDate);
            if (expiringSoon) {
                logger.warn("SSL certificate will expire on: {}. Please renew it soon.", expiration);
            }
            
            return expiringSoon;
            
        } catch (Exception e) {
            logger.error("Failed to check certificate expiration", e);
            return true; // 假设过期以触发更新
        }
    }
}