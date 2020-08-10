package ca.umanitoba.libraries.georeferencing.impl;

import static org.slf4j.LoggerFactory.getLogger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.sql.DataSource;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import ca.umanitoba.libraries.georeferencing.AdminCodes;
import ca.umanitoba.libraries.georeferencing.Coordinates;
import ca.umanitoba.libraries.georeferencing.LocationRequest;
import ca.umanitoba.libraries.georeferencing.api.NameLookupService;
import ca.umanitoba.libraries.georeferencing.exceptions.InternalApplicationError;
import ca.umanitoba.libraries.georeferencing.exceptions.NameLookupException;
import ca.umanitoba.libraries.georeferencing.utils.DatabaseUtils;
import ca.umanitoba.libraries.georeferencing.utils.DbPlatform;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Implementation of NameLookupService.
 * @author whikloj
 */
@Component
public class NameLookupServiceImpl implements NameLookupService {

    @Autowired
    @Lazy
    private DataSource dataSource;

    @Autowired
    @Lazy
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Value("${datahub.locationfile}")
    private String datahubLocationFileValue;

    private static final Logger LOGGER = getLogger(NameLookupServiceImpl.class);

    @Inject
    private AdminCodes adminCodes;

    private boolean tableExists = false;

    private boolean tableFull = false;

    private static String DEFAULT_DATAHUB_LOCATION_FILE = "data/CA.txt";

    private InputStream datahubLocationStream;

    private ConcurrentHashMap<String, Coordinates> lookups = new ConcurrentHashMap<>();

    private static final String TABLE_NAME = "placeLookup";

    private static final String ALT_NAME_TABLE_NAME = "PlaceLookupAltNames";

    private static final String COUNT_ALL_ROWS = String.format("SELECT COUNT(*) FROM %s", TABLE_NAME);

    private static final String INSERT_MAIN_ROWS = String.format("INSERT INTO %s VALUES (:id, :name, :asciiname, " +
            ":latitude, :longitude, :featureClass, :feature, :adminCode1, :adminCode2, :countryCode, :adminCode3, " +
            ":adminCode4, :pop, :elev, :dem, :tz, :modified)", TABLE_NAME);

    private static final String INSERT_ALT_ROWS = String.format("INSERT INTO %s VALUES (:id, :name)",
            ALT_NAME_TABLE_NAME);

    private static final String TRUNCATE_TABLE = String.format("TRUNCATE TABLE %s", TABLE_NAME);
    private static final String TRUNCATE_ALT_TABLE = String.format("TRUNCATE TABLE %s", ALT_NAME_TABLE_NAME);

    private static final RowMapper<Coordinates> rowMapper = new RowMapper<Coordinates>() {
        @Override
        public Coordinates mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            return new Coordinates(
                    rs.getDouble("latitude"),
                    rs.getDouble("longitude")
            );
        }
    };

    private static final Map<DbPlatform, String> DDL_MAP = Map.of(DbPlatform.MYSQL, "sql/placeLookup_mysql.sql",
            DbPlatform.H2, "sql/placeLookup_h2.sql");

    /**
     * Basic constructor.
     */
    public NameLookupServiceImpl() {
        // This constructor left intentionally blank.
    }

    /**
     * Check if tables are ready to go.
     */
    @PostConstruct
    public void checkTables() {
        if (!tableExists) {
            if (!DatabaseUtils.tableExists(dataSource, TABLE_NAME) ||
                    !DatabaseUtils.tableExists(dataSource, ALT_NAME_TABLE_NAME)) {
                try {
                    LOGGER.info("Table {} or {} does not exist, creating", TABLE_NAME, ALT_NAME_TABLE_NAME);
                    DatabaseUtils.loadDDL(dataSource, DDL_MAP);
                } catch (final DataAccessException e) {
                    throw new InternalApplicationError(e);
                }
            }
            tableExists = true;
        }

        if (!tableFull) {
            if (DatabaseUtils.tableIsEmpty(jdbcTemplate.getJdbcTemplate(), COUNT_ALL_ROWS)) {
                try {
                    final String location = (datahubLocationFileValue == null ? DEFAULT_DATAHUB_LOCATION_FILE :
                            datahubLocationFileValue);
                    if (location == null) {
                        throw new InternalApplicationError("datahub.locationfile cannot be blank.");
                    }
                    if (location.startsWith("classpath:")) {
                        final String relativePath = location.substring(10);
                        datahubLocationStream = Objects.requireNonNull(getClass().getClassLoader()
                                .getResourceAsStream(relativePath));
                    } else if (location.startsWith("file:")) {
                        final String relativePath = location.substring(5);
                        datahubLocationStream = new FileInputStream(relativePath);
                    }
                    LOGGER.info("Table {} is empty, parsing file {}", TABLE_NAME, location);
                    loadRecords();
                } catch (final DataAccessException | FileNotFoundException e) {
                    throw new InternalApplicationError(e);
                }
            }
            tableFull = true;
        }
    }

    @Override
    public Coordinates lookup(final LocationRequest location) throws NameLookupException {
        final StringBuilder sql = new StringBuilder();
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        checkTables();
        if (location.getIso3361Alpha2() != null) {
            sql.append("countryCode = :countryCode");
            parameterSource.addValue("countryCode", location.getIso3361Alpha2());
        }
        if (location.getCity() != null) {
            if (sql.length() > 0) {
                sql.append(" AND ");
            }
            sql.append("lower(name) = :city");
            parameterSource.addValue("city", location.getCity().toLowerCase());
            sql.append(" AND feature_class = 'P' AND (feature in ('PPL', 'PPLC') OR feature LIKE 'PPLA%')");
            if (location.getIso3361Alpha2() != null && location.getProvince() != null) {
                // Need country code and province.
                final String adminCode = adminCodes.findAdminCode(location.getIso3361Alpha2(),
                        location.getProvince());
                sql.append(" AND adminCode1 = :adminCode1");
                parameterSource.addValue("adminCode1", adminCode);
            }
        } else if (location.getProvince() != null) {
            if (sql.length() > 0) {
                sql.append(" AND ");
            }
            sql.append("lower(name) = :province");
            parameterSource.addValue("province", location.getProvince().toLowerCase());
            sql.append(" AND feature_class = 'A' AND feature like 'ADM%'");
        }
        sql.insert(0, String.format("SELECT latitude, longitude FROM %s WHERE ", TABLE_NAME));
        final String queryString = sql.toString();
        try {
            return jdbcTemplate.queryForObject(queryString, parameterSource, rowMapper);
        } catch (final EmptyResultDataAccessException e) {
            throw new NameLookupException("Could not find a location to match the request.");
        }
    }

    /**
     * Load the records from the text file into the database.
     */
    private void loadRecords() {
        try {
            final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
            final List<String[]> lines = new ArrayList<>();
            final List<MapSqlParameterSource> parameterSourcesList = new ArrayList<>();
            final AtomicInteger recordCount = new AtomicInteger(0);
            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(datahubLocationStream,
                    StandardCharsets.UTF_8));
                 final Stream<String> stream = reader.lines()) {
                stream.forEach(l -> {
                    final MapSqlParameterSource ps = new MapSqlParameterSource();
                    final String[] line = l.split("\t");
                    ps.addValue("id", line[0].trim());
                    ps.addValue("name", line[1].trim());
                    ps.addValue("asciiname", line[2].trim());
                    // line[3] is long alternate names
                    ps.addValue("latitude", Float.valueOf(line[4]));
                    ps.addValue("longitude", Float.valueOf(line[5]));
                    ps.addValue("featureClass", line[6].trim());
                    ps.addValue("feature", line[7].trim());
                    ps.addValue("countryCode", line[8].trim());
                    // line[9] is country code 2, we are dumping it.
                    ps.addValue("adminCode1", line[10].trim());
                    ps.addValue("adminCode2", line[11].trim());
                    ps.addValue("adminCode3", line[12].trim());
                    ps.addValue("adminCode4", line[13].trim());
                    ps.addValue("pop", line[14].isEmpty() ? 0 : Integer.parseInt(line[14]));
                    ps.addValue("elev", line[15].isEmpty() ? 0 : Integer.parseInt(line[15]));
                    ps.addValue("dem", line[16].isEmpty() ? 0 : Integer.parseInt(line[16]));
                    ps.addValue("tz", line[17].trim());
                    ps.addValue("modified", line[18].isEmpty() ? formatter.format(LocalDate.now()) :
                            line[18]);
                    parameterSourcesList.add(ps);
                    recordCount.incrementAndGet();
                    if (!line[3].isEmpty()) {
                        lines.add(line);
                    }
                });
            }
            final MapSqlParameterSource[] psArray = parameterSourcesList.toArray(new MapSqlParameterSource[0]);
            jdbcTemplate.batchUpdate(INSERT_MAIN_ROWS, psArray);
            LOGGER.info("Loaded {} records into table {}", recordCount.get(), TABLE_NAME);
            final int secondRCount = lines.size();
            lines.forEach(l -> {
                // If there are alternate names, split them on comma and insert them in to the alternate name table.
                final String[] names = l[3].split(",");
                final String id = l[0].trim();
                for (final String name : names) {
                    final MapSqlParameterSource ps = new MapSqlParameterSource();
                    ps.addValue("id", id);
                    ps.addValue("name", name.trim());
                    jdbcTemplate.update(INSERT_ALT_ROWS, ps);
                }
            });
            LOGGER.info("Loaded {} records into table {}", secondRCount, ALT_NAME_TABLE_NAME);
        } catch (final DataAccessException | IOException e) {
            throw new InternalApplicationError(e);
        }
    }

    /**
     * Truncate the tables and re-load them from files.
     */
    public void reset() {
        try {
            jdbcTemplate.getJdbcTemplate().execute(TRUNCATE_TABLE);
            jdbcTemplate.getJdbcTemplate().execute(TRUNCATE_ALT_TABLE);
            tableFull = false;
            loadRecords();
            tableFull = true;
        } catch (final DataAccessException e) {
            throw new InternalApplicationError(e);
        }
    }
}
