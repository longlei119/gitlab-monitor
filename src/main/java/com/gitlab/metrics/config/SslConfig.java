package com.gitlab.metrics.config;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * SSL/TLS配置类
 * 配置HTTPS传输加密
 */
@Configuration
@Profile("prod")
public class SslConfig {
    
    @Value("${server.ssl.enabled:false}")
    private boolean sslEnabled;
    
    @Value("${server.http.port:8080}")
    private int httpPort;
    
    @Value("${server.port:8443}")
    private int httpsPort;
    
    /**
     * 配置Tomcat服务器，支持HTTP到HTTPS的重定向
     */
    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
        
        if (sslEnabled) {
            // 添加HTTP连接器，用于重定向到HTTPS
            tomcat.addAdditionalTomcatConnectors(createHttpConnector());
        }
        
        return tomcat;
    }
    
    /**
     * 创建HTTP连接器，将HTTP请求重定向到HTTPS
     */
    private Connector createHttpConnector() {
        Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
        connector.setScheme("http");
        connector.setPort(httpPort);
        connector.setSecure(false);
        connector.setRedirectPort(httpsPort);
        return connector;
    }
}