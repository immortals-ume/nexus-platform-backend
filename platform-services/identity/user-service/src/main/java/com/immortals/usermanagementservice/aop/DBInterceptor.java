package com.immortals.usermanagementservice.aop;


import com.immortals.usermanagementservice.context.DbContextHolder;
import com.immortals.usermanagementservice.model.enums.DbType;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class DBInterceptor {

    @Before("@annotation(com.immortals.usermanagementservice.annotation.ReadOnly)")
    public void setReadOnlyDataSource() {
        DbContextHolder.set(DbType.READ);
    }

    @Before("@annotation(com.immortals.usermanagementservice.annotation.WriteOnly)")
    public void setWriteOnlyDataSource() {
        DbContextHolder.set(DbType.WRITE);
    }

    @After("@annotation(com.immortals.usermanagementservice.annotation.ReadOnly) || @annotation(com.immortals.usermanagementservice.annotation.WriteOnly)")
    public void clearContext() {
        DbContextHolder.clear();
    }
}
