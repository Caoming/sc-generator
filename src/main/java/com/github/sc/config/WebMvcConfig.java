package com.github.sc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Created by wuyu on 2017/4/4.
 */
@Configuration
public class WebMvcConfig extends WebMvcConfigurerAdapter {

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.mediaType("adoc", MediaType.parseMediaType("text/asciidoc;charset=utf-8"))
                .mediaType("md", MediaType.parseMediaType("text/markdown;charset=utf-8"))
                .mediaType("html", MediaType.parseMediaType("text/html;charset=utf-8"))
                .mediaType("properties", MediaType.parseMediaType("text/properties;charset=utf-8"))
                .mediaType("yml", MediaType.parseMediaType("text/yaml;charset=utf-8"))
                .mediaType("sql", MediaType.parseMediaType(MediaType.TEXT_PLAIN_VALUE + ";charset=utf-8"))
                .mediaType("jdl", MediaType.parseMediaType(MediaType.TEXT_PLAIN_VALUE + ";charset=utf-8"))
                .mediaType("doc", MediaType.parseMediaType("application/msword"));
        super.configureContentNegotiation(configurer);
    }


}
