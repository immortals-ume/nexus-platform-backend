package com.immortals.platform.common.db.config;

import com.immortals.platform.common.db.routing.RoutingDataSource;
import com.immortals.platform.domain.enums.DbType;
import com.immortals.platform.domain.properties.ReadDataSourceProperties;
import com.immortals.platform.domain.properties.WriteDataSourceProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@AutoConfiguration
@ConditionalOnClass(DataSource.class)
@EnableConfigurationProperties({
        ReadDataSourceProperties.class,
        WriteDataSourceProperties.class
})
@RequiredArgsConstructor
public class DataSourceAutoConfiguration {

    private final WriteDataSourceProperties writeDataSourceProperties;
    private final ReadDataSourceProperties readDataSourceProperties;

    @Bean("writeDataSource")
    @ConditionalOnMissingBean(name = "writeDataSource")
    public DataSource writeDataSource() {
        return DataSourceBuilder.create()
                .url(writeDataSourceProperties.getUrl())
                .username(writeDataSourceProperties.getUsername())
                .password(writeDataSourceProperties.getPassword())
                .driverClassName(writeDataSourceProperties.getDriverClassName())
                .build();
    }

    @Bean("readDataSource")
    @ConditionalOnMissingBean(name = "readDataSource")
    public DataSource readDataSource() {
        return DataSourceBuilder.create()
                .url(readDataSourceProperties.getUrl())
                .username(readDataSourceProperties.getUsername())
                .password(readDataSourceProperties.getPassword())
                .driverClassName(readDataSourceProperties.getDriverClassName())
                .build();
    }

    @Bean
    @ConditionalOnMissingBean(name = "routingDataSource")
    public DataSource routingDataSource() {
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DbType.WRITE, writeDataSource());
        targetDataSources.put(DbType.READ, readDataSource());

        RoutingDataSource routingDataSource = new RoutingDataSource();
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(writeDataSource());
        routingDataSource.afterPropertiesSet();
        return routingDataSource;
    }
}
