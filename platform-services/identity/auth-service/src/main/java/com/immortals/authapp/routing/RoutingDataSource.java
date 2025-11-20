package com.immortals.authapp.routing;


import com.immortals.authapp.context.DbContextHolder;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class RoutingDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        return DbContextHolder.get();
    }
}
