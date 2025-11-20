package com.immortals.usermanagementservice.config.db;

import com.immortals.usermanagementservice.model.properties.JpaProperties;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.PersistenceUnitManager;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(
        basePackages = "com.immortals.usermanagementservice.repository",
        entityManagerFactoryRef = "entityManagerFactory",
        transactionManagerRef = "transactionManager"
)
@EntityScan(basePackages = "com.immortals.usermanagementservice.model.properties")
public class JpaConfig {
    private final JpaProperties jpaProperties;

    public JpaConfig(JpaProperties jpaProperties) {
        this.jpaProperties = jpaProperties;
    }

    @Bean
    public JpaVendorAdapter jpaVendorAdapter() {
        HibernateJpaVendorAdapter adapter = new HibernateJpaVendorAdapter();
        if (jpaProperties.getDatabasePlatform() != null) {
            adapter.setDatabasePlatform(jpaProperties.getDatabasePlatform());
        }
        return adapter;
    }

    @Bean
    public EntityManagerFactoryBuilder entityManagerFactoryBuilder(
            JpaVendorAdapter jpaVendorAdapter,
            ObjectProvider<PersistenceUnitManager> persistenceUnitManager) {

        return new EntityManagerFactoryBuilder(
                jpaVendorAdapter,
                new HashMap<>(),
                persistenceUnitManager.getIfAvailable()
        );
    }

    @Bean(name = "entityManagerFactory")
    @DependsOn("routingDataSource")
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("routingDataSource") DataSource routingDataSource, JpaProperties jpaProperties) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(routingDataSource);
        emf.setPackagesToScan("com.immortals.authapp.model.entity");
        emf.setJpaVendorAdapter(jpaVendorAdapter());


        Map<String, Object> jpaProps = new HashMap<>();

        if (jpaProperties.getHibernate() != null)
            jpaProps.putAll(jpaProperties.getHibernate());

        if (!jpaProps.containsKey("hibernate.dialect")) {
            jpaProps.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        }

        emf.setJpaPropertyMap(jpaProps);

        return emf;
    }


    @Bean(name = "transactionManager")
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") EntityManagerFactory entityManagerFactory) {

        return new JpaTransactionManager(entityManagerFactory);
    }
}
