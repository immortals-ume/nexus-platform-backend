package com.immortals.platform.common.db.aop;

import com.immortals.platform.common.db.context.DbContextHolder;
import com.immortals.platform.domain.enums.DbType;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class DBInterceptor {

    @Before("@annotation(com.immortals.platform.common.db.annotation.ReadOnly)")
    public void setReadOnlyDataSource() {
        DbContextHolder.set(DbType.READ);
    }

    @Before("@annotation(com.immortals.platform.common.db.annotation.WriteOnly)")
    public void setWriteOnlyDataSource() {
        DbContextHolder.set(DbType.WRITE);
    }

    @After("@annotation(com.immortals.platform.common.db.annotation.ReadOnly) || @annotation(com.immortals.platform.common.db.annotation.WriteOnly)")
    public void clearContext() {
        DbContextHolder.clear();
    }
}
