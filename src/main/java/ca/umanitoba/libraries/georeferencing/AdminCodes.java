package ca.umanitoba.libraries.georeferencing;

import static org.slf4j.LoggerFactory.getLogger;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import ca.umanitoba.libraries.georeferencing.exceptions.InternalApplicationError;
import ca.umanitoba.libraries.georeferencing.utils.DatabaseUtils;
import ca.umanitoba.libraries.georeferencing.utils.DbPlatform;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * GeoNames administrative level one codes. These equal provinces in Canada.
 * @author whikloj
 */
@Component
public class AdminCodes {
    private static final Logger LOGGER = getLogger(AdminCodes.class);

    @Autowired
    @Lazy
    private JdbcTemplate jdbcTemplate;

    @Autowired
    @Lazy
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    private DataSource dataSource;

    private static String DEFAULT_GEONAMES_ADMIN_CODE_FILE = "admin1CodesASCII.txt";

    private InputStream adminCodeInputStream;

    private static String TABLE_NAME = "admin1Codes";

    private static String COUNT_ALL_ROWS = String.format("SELECT count(*) FROM %s", TABLE_NAME);

    private static String INSERT_RECORD = String.format("INSERT INTO %s VALUES (:country, :code, :name)", TABLE_NAME);

    private static String SELECT_CODE_BY_COUNTRY_NAME = String.format("SELECT code FROM %s WHERE lower(country) = " +
            ":country AND lower(name) = :name", TABLE_NAME);

    private static String TRUNCATE_TABLE = String.format("TRUNCATE TABLE %s", TABLE_NAME);

    private boolean tableExists = false;
    private boolean tableFull = false;

    private static final Map<DbPlatform, String> DDL_MAP = Map.of(
            DbPlatform.MYSQL, "sql/adminCodes_mysql.sql",
            DbPlatform.H2, "sql/adminCodes_h2.sql");

    /**
     * Check and create/load table data.
     */
    @PostConstruct
    public void initialize() {
        if (!tableExists) {
            if (!DatabaseUtils.tableExists(dataSource, TABLE_NAME)) {
                try {
                    LOGGER.info("Table {} does not exist, creating", TABLE_NAME);
                    DatabaseUtils.loadDDL(dataSource, DDL_MAP);
                } catch (final DataAccessException e) {
                    throw new InternalApplicationError(e);
                }
            }
            tableExists = true;
        }
        if (!tableFull) {
            if (DatabaseUtils.tableIsEmpty(jdbcTemplate, COUNT_ALL_ROWS)) {
                try {
                    adminCodeInputStream = Objects.requireNonNull(getClass().getClassLoader()
                            .getResourceAsStream(DEFAULT_GEONAMES_ADMIN_CODE_FILE));
                    LOGGER.info("Table {} is empty, parsing file {}", TABLE_NAME, adminCodeInputStream);
                    loadAdminCodes();
                } catch (final DataAccessException e) {
                    throw new InternalApplicationError(e);
                }
            }
            tableFull = true;
        }
    }

    /**
     * Load the admin codes from the file into the database.
     */
    private void loadAdminCodes() {
        final List<MapSqlParameterSource> parameterSourcesList = new ArrayList<>();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(adminCodeInputStream,
                StandardCharsets.UTF_8));
             final Stream<String> stream = reader.lines()) {
            stream.forEach(l -> {
                final String[] p = l.split("\t");
                if (p.length < 2) {
                    // Need two entries at least.
                    throw new InternalApplicationError(String.format("Only received 1 part from line %s", p[0]));
                }
                final String name = p[1];
                if (p[0].contains(".")) {
                    final String[] f = p[0].split("\\.");
                    final String country = f[0].trim();
                    final String adminCode = f[1].trim();
                    final MapSqlParameterSource ps = new MapSqlParameterSource();
                    ps.addValue("country", country);
                    ps.addValue("code", adminCode);
                    ps.addValue("name", name);
                    parameterSourcesList.add(ps);
                } else {
                    throw new InternalApplicationError(String.format("Country and code does not contain a period, %s",
                            p[0]));
                }
            });
            final MapSqlParameterSource[] psArray = parameterSourcesList.toArray(new MapSqlParameterSource[0]);
            namedParameterJdbcTemplate.batchUpdate(INSERT_RECORD, psArray);
        } catch (final DataAccessException | IOException e) {
            throw new InternalApplicationError(e);
        }
    }

    /**
     * Retrieve an admin1 code for the country and place name.
     * @param countryCode the country code.
     * @param name the administrative division name
     * @return the admin1 code.
     */
    public String findAdminCode(final String countryCode, final String name) {
        try {
            initialize();
            final MapSqlParameterSource ps = new MapSqlParameterSource();
            ps.addValue("country", countryCode.toLowerCase());
            ps.addValue("name", name.toLowerCase());
            return namedParameterJdbcTemplate.queryForObject(SELECT_CODE_BY_COUNTRY_NAME, ps,
                    (rs, rowNum) -> rs.getString(1));
        } catch (final DataAccessException e) {
            throw new InternalApplicationError(e);
        }
    }

    /**
     * Truncate the table and reload.
     */
    public void reset() {
        try {
            LOGGER.info("Resetting Admin Codes");
            jdbcTemplate.execute(TRUNCATE_TABLE);
            tableFull = false;
            loadAdminCodes();
            tableFull = true;
        } catch (final DataAccessException e) {
            throw new InternalApplicationError(e);
        }
    }
}
