package io.github.hligaty.test.config;

import io.github.hligaty.haibaracp.config.ClientProperties;
import io.github.hligaty.haibaracp.config.PoolProperties;
import io.github.hligaty.haibaracp.core.SftpSessionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SftpConfiguration {

    @Bean
    public SftpSessionFactory sftpSessionFactory(ClientProperties clientProperties, PoolProperties poolProperties) {
        return new SftpSessionFactory(clientProperties, poolProperties) {
            @Override
            public SftpShellSession getSftpSession(ClientProperties clientProperties) {
                return new SftpShellSession(clientProperties);
            }
        };
    }
    
}
