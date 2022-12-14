package com.example.cwgl.configs;

import com.example.cwgl.entity.Table;
import com.example.cwgl.utils.JDBC13;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import javax.sql.DataSource;


/**
* description: 跨域配置
* @author zhangsihai
* @date 2020/3/24 15:22
*/
@Configuration
public class CorsConfig {
    private CorsConfiguration buildConfig() {
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedOrigin("*"); // 1允许任何域名使用
        corsConfiguration.addAllowedHeader("*"); // 2允许任何头
        corsConfiguration.addAllowedMethod("*"); // 3允许任何方法（post、get等）
        return corsConfiguration;
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", buildConfig()); // 4
        return new CorsFilter(source);
    }

    @Bean
    public JDBC13 jdbc13(DataSource dataSource){
        JDBC13.setDs(dataSource);
        JDBC13.setEntityPackage(Table.entityPackage);
         JDBC13.setAutoToLine(false);
        return new JDBC13();
    }
}
