package ca.umanitoba.libraries.georeferencing.utils;

import static org.slf4j.LoggerFactory.getLogger;

import javax.sql.DataSource;

import java.sql.SQLException;

import org.slf4j.Logger;

/**
 * Enum of supported database platforms.
 * @author whikloj
 */
public enum DbPlatform {

    H2("H2"),
    MYSQL("MySQL");

    private String name;

    private static final Logger LOGGER = getLogger(DbPlatform.class);

    DbPlatform(final String name) {
        this.name = name;
    }

    public static DbPlatform fromString(final String name) {
        for (var platform : values()) {
            if (platform.name.equals(name)) {
                return platform;
            }
        }
        throw new IllegalArgumentException("Unknown database platform: " + name);
    }

    public static DbPlatform fromDataSource(final DataSource dataSource) {
        try (final var connection = dataSource.getConnection()) {
            final var name = connection.getMetaData().getDatabaseProductName();
            LOGGER.debug("Identified database as: {}", name);
            return fromString(name);
        } catch (final SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
