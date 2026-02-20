package com.immortals.platform.common.db.context;


import com.immortals.platform.common.props.DbType;

/**
 * Thread-local context holder for database routing.
 * Stores the current database type (READ or WRITE) for the executing thread.
 * Used by RoutingDataSource to determine which datasource to route to.
 */
public class DbContextHolder {
    
    private static final ThreadLocal<DbType> contextHolder = new ThreadLocal<>();

    /**
     * Sets the database type for the current thread.
     * 
     * @param dbType the database type (READ or WRITE)
     */
    public static void set(DbType dbType) {
        contextHolder.set(dbType);
    }

    /**
     * Gets the database type for the current thread.
     * 
     * @return the database type, or null if not set
     */
    public static DbType get() {
        return contextHolder.get();
    }

    /**
     * Clears the database type for the current thread.
     * Should be called after method execution to prevent context leakage.
     */
    public static void clear() {
        contextHolder.remove();
    }
}
