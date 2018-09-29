package com.oembedler.moon.graphql.boot;

import com.coxautodev.graphql.tools.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import graphql.schema.GraphQLScalarType;
import graphql.schema.GraphQLSchema;
import graphql.servlet.GraphQLSchemaProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.coxautodev.graphql.tools.SchemaParserOptions.newOptions;

/**
 * @author Andrew Potter
 */
@Configuration
@ConditionalOnClass(SchemaParser.class)
public class GraphQLJavaToolsAutoConfiguration {

    @Autowired(required = false)
    private SchemaParserDictionary dictionary;

    @Autowired(required = false)
    private GraphQLScalarType[] scalars;

    @Autowired(required = false)
    private SchemaParserOptions options;

    @Bean
    @ConditionalOnMissingBean
    public SchemaStringProvider schemaStringProvider() {
        return new ClasspathResourceSchemaStringProvider();
    }

    @Bean
    @ConditionalOnBean({GraphQLResolver.class})
    @ConditionalOnMissingBean
    public SchemaParser schemaParser(
            List<GraphQLResolver<?>> resolvers,
            SchemaStringProvider schemaStringProvider,
            Optional<PerFieldObjectMapperProvider> perFieldObjectMapperProvider
    ) throws IOException {
        SchemaParserBuilder builder = dictionary != null ? new SchemaParserBuilder(dictionary) : new SchemaParserBuilder();

        List<String> schemaStrings = schemaStringProvider.schemaStrings();
        schemaStrings.forEach(builder::schemaString);

        if (scalars != null) {
            builder.scalars(scalars);
        }

        if (options != null) {
            builder.options(options);
        } else if (perFieldObjectMapperProvider.isPresent()) {
            final SchemaParserOptions.Builder optionsBuilder =
                    newOptions().objectMapperProvider(perFieldObjectMapperProvider.get());
            builder.options(optionsBuilder.build());
        }

        return builder
                .resolvers(resolvers)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "graphql.tools.use-default-objectmapper", havingValue = "true", matchIfMissing = true)
    public PerFieldObjectMapperProvider perFieldObjectMapperProvider(ObjectMapper objectMapper) {
        objectMapper
                .registerModule(new Jdk8Module())
                .registerModule(new KotlinModule());
        return fieldDefinition -> objectMapper;
    }


    @Bean
    @ConditionalOnBean(SchemaParser.class)
    @ConditionalOnMissingBean({GraphQLSchema.class, GraphQLSchemaProvider.class})
    public GraphQLSchema graphQLSchema(SchemaParser schemaParser) {
        return schemaParser.makeExecutableSchema();
    }
}
