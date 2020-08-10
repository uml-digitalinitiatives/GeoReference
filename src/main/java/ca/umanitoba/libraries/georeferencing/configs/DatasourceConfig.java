package ca.umanitoba.libraries.georeferencing.configs;

import javax.sql.DataSource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * Configuration of JDBC beans. Loads lazy to avoid issues around instantiation of classes.
 * @author whikloj
 */
@Configuration
@Lazy
public class DatasourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "database")
    public DataSource getDataSource() {
        return DataSourceBuilder.create()
              .build();
    }

    @Bean
    public NamedParameterJdbcTemplate getNamedTemplate(DataSource dataSource) {
        return new NamedParameterJdbcTemplate(dataSource);
    }

    @Bean
    public JdbcTemplate getTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
