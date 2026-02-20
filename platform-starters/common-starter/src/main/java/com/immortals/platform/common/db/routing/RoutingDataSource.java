package com.immortals.platform.common.db.routing;

import com.immortals.platform.common.db.context.DbContextHolder;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * Spring datasource router that dynamically selects between read and write datasources
 * based on the current thread-local database context.
 * <p>
 * This class extends Spring's AbstractRoutingDataSource and uses DbContextHolder
 * to determine which datasource to route to for each database operation.
 * <p>
 * When no context is set (DbContextHolder.get() returns null), the routing falls back
 * to the default datasource configured in DataSourceAutoConfiguration.
 */
public class RoutingDataSource extends AbstractRoutingDataSource {
    
    /**
     * Determines the current lookup key for datasource routing.
     * 
     * This method is called by Spring's AbstractRoutingDataSource to determine
     * which datasource to use for the current database operation.
     * 
     * @return the current DbType (READ or WRITE) from thread-local context,
     *         or null if no context is set (which triggers default datasource)
     */
    @Override
    protected Object determineCurrentLookupKey() {
        return DbContextHolder.get();
    }
}
