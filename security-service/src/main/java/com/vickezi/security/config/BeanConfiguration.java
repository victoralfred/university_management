package com.vickezi.security.config;


import com.vickezi.security.dao.reads.GroupAndRoleServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.scheduling.annotation.AsyncConfigurer;


@Configuration
public class BeanConfiguration implements AsyncConfigurer {
    @Bean
    public GroupAndRoleServiceImpl groupAndRoleService(DatabaseClient databaseClient){
        return new GroupAndRoleServiceImpl(databaseClient);
    }
}