package com.immortals.usermanagementservice.routing;


import com.immortals.usermanagementservice.context.DbContextHolder;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class RoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        return DbContextHolder.get();
    }
}
