package com.immortals.usermanagementservice.context;


import com.immortals.usermanagementservice.model.enums.DbType;

public class DbContextHolder {
    private static final ThreadLocal<DbType> contextHolder = new ThreadLocal<>();

    public static void set(DbType dbType) {
        contextHolder.set(dbType);
    }

    public static DbType get() {
        return contextHolder.get();
    }

    public static void clear() {
        contextHolder.remove();
    }
}
