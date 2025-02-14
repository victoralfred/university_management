package com.vickezi.gateway.util;

import com.vickezi.globals.util.CustomValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ImportedBeans {
    @Bean
    public CustomValidator customValidator(){
        return new CustomValidator();
    }
}
