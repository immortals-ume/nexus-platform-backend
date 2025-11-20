package com.immortals.usermanagementservice.config.db;


import com.immortals.usermanagementservice.model.enums.DbType;
import com.immortals.usermanagementservice.model.properties.ReadDataSourceProperties;
import com.immortals.usermanagementservice.model.properties.WriteDataSourceProperties;
import com.immortals.usermanagementservice.routing.RoutingDataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties({
        ReadDataSourceProperties.class,
        WriteDataSourceProperties.class
})
@RequiredArgsConstructor
public class DataSourceConfig {

    private final WriteDataSourceProperties writeDataSourceProperties;
    private final ReadDataSourceProperties readDataSourceProperties;

    @Bean("writeDataSource")
    public DataSource writeDataSource(@Qualifier("writeDataSourceProperties") WriteDataSourceProperties props) {
        return DataSourceBuilder.create()
                .url(props.getUrl())
                .username(props.getUsername())
                .password(props.getPassword())
                .driverClassName(props.getDriverClassName())
                .build();
    }

    @Bean("readDataSource")
    public DataSource readDataSource(@Qualifier("readDataSourceProperties") ReadDataSourceProperties props) {
        return DataSourceBuilder.create()
                .url(props.getUrl())
                .username(props.getUsername())
                .password(props.getPassword())
                .driverClassName(props.getDriverClassName())
                .build();
    }


    @Bean
    public DataSource routingDataSource() {
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DbType.WRITE.name(), writeDataSource(writeDataSourceProperties));
        targetDataSources.put(DbType.READ.name(), readDataSource(readDataSourceProperties));

        RoutingDataSource routingDataSource = new RoutingDataSource();
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(writeDataSource(writeDataSourceProperties));
        routingDataSource.afterPropertiesSet();
        return routingDataSource;
    }
}
