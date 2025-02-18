package com.vickezi.security.config;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.hibernate.reactive.mutiny.Mutiny;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanConfiguration {
    @Bean
    public Mutiny.SessionFactory sessionFactory(){
        try(EntityManagerFactory entityManagerFactory =Persistence.createEntityManagerFactory(
                "production-persistence-unit")){
            return entityManagerFactory.unwrap(Mutiny.SessionFactory.class);
        }
    }
}
