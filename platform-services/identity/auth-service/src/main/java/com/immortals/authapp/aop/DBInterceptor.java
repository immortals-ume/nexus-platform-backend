package com.immortals.authapp.aop;

import com.immortals.authapp.context.DbContextHolder;
import com.immortals.platform.domain.enums.DbType;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class DBInterceptor {

    @Before("@annotation(com.immortals.authapp.annotation.ReadOnly)")
    public void setReadOnlyDataSource() {
        DbContextHolder.set(DbType.READ);
    }

    @Before("@annotation(com.immortals.authapp.annotation.WriteOnly)")
    public void setWriteOnlyDataSource() {
        DbContextHolder.set(DbType.WRITE);
    }

    @After("@annotation(com.immortals.authapp.annotation.ReadOnly) || @annotation(com.immortals.authapp.annotation.WriteOnly)")
    public void clearContext() {
        DbContextHolder.clear();
    }
}
