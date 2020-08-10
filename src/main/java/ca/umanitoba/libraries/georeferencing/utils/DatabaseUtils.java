package ca.umanitoba.libraries.georeferencing.utils;

import static org.slf4j.LoggerFactory.getLogger;

import javax.sql.DataSource;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import ca.umanitoba.libraries.georeferencing.exceptions.InternalApplicationError;
import org.slf4j.Logger;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

/**
 * Utility class of database functions.
 * @author whikloj
 */
public class DatabaseUtils {

    private final static Logger LOGGER = getLogger(DatabaseUtils.class);

    /**
     * Check if the table exists in the database.
     * @return true if the table exists.
     */
    public static boolean tableExists(final DataSource dataSource, final String tableName) {
        try {
            DatabaseMetaData meta = dataSource.getConnection().getMetaData();
            ResultSet res = meta.getTables(null, null, null,
                    new String[]{"TABLE"});
            while (res.next()) {
                if (res.getString("TABLE_NAME").startsWith(tableName)) {
                    res.close();
                    return true;
                }
            }
            res.close();
            return false;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Check if the country code table is empty
     * @param jdbcTemplate the current jdbcTemplate to perform the query.
     * @param countAllRows a query to get a count of all rows.
     * @return true if there is no data.
     */
    public static boolean tableIsEmpty(final JdbcTemplate jdbcTemplate, final String countAllRows) {
        final Integer count = jdbcTemplate.queryForObject(countAllRows,
                (rs, rowNum) -> rs.getInt(1));
        return (count == null || count == 0);
    }

    /**
     * Try to load a DDL file for the current datasource.
     * @param dataSource the current datasource.
     * @param ddlMap Map of database types to ddl files.
     */
    public static void loadDDL(final DataSource dataSource, final Map<DbPlatform, String> ddlMap) {
        final var dbPlatform = DbPlatform.fromDataSource(dataSource);

        if (!ddlMap.containsKey(dbPlatform)) {
            throw new InternalApplicationError(
                    String.format("Missing DDL mapping for %s", dbPlatform)
            );
        }

        final var ddl = ddlMap.get(dbPlatform);
        LOGGER.info("Applying ddl: {}", ddl);
        DatabasePopulatorUtils.execute(
                new ResourceDatabasePopulator(new DefaultResourceLoader().getResource("classpath:" + ddl)),
                dataSource);
    }

    private DatabaseUtils() {
        // This constructor left intentionally blank for static class.
    }
}
