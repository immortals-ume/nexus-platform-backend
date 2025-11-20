package com.immortals.authapp.context;


import com.immortals.platform.domain.enums.DbType;

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
