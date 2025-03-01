package com.vickezi.processor;

import com.vickezi.processor.dao.ReactiveDatabaseService;
import com.vickezi.processor.dao.model.ReactiveDatabaseServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.core.DatabaseClient;
@Configuration
public class ApplicationBeanConfiguration {
    @Bean
    public ReactiveDatabaseService groupAndRoleService(DatabaseClient databaseClient){
        return new ReactiveDatabaseServiceImpl(databaseClient);
    }
}
