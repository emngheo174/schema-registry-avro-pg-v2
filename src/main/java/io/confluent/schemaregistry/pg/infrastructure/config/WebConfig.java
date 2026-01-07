package io.confluent.schemaregistry.pg.infrastructure.config;

import io.confluent.schemaregistry.pg.domain.value.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for Spring MVC.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        // Register converters for value objects so they can be used as @PathVariable and @RequestParam
        registry.addConverter(new StringToSubjectNameConverter());
        registry.addConverter(new StringToVersionConverter());
        registry.addConverter(new IntegerToSchemaIdConverter());
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }

    static class StringToSubjectNameConverter implements Converter<String, SubjectName> {
        @Override
        public SubjectName convert(String source) {
            return SubjectName.of(source);
        }
    }

    static class StringToVersionConverter implements Converter<String, Version> {
        @Override
        public Version convert(String source) {
            return Version.of(source);
        }
    }

    static class IntegerToSchemaIdConverter implements Converter<Integer, SchemaId> {
        @Override
        public SchemaId convert(Integer source) {
            return SchemaId.of(source);
        }
    }
}
