package ca.umanitoba.libraries.georeferencing.impl;

import static org.slf4j.LoggerFactory.getLogger;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import ca.umanitoba.libraries.georeferencing.exceptions.InternalApplicationError;
import ca.umanitoba.libraries.georeferencing.exceptions.MissingCountryCodeException;
import ca.umanitoba.libraries.georeferencing.api.CountryCodeLookupService;
import ca.umanitoba.libraries.georeferencing.utils.DatabaseUtils;
import ca.umanitoba.libraries.georeferencing.utils.DbPlatform;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Datahub country code implementation.
 * @author whikloj
 */
@Component
public class DataHubCountryCodeLookupService implements CountryCodeLookupService {

    private Logger LOGGER = getLogger(DataHubCountryCodeLookupService.class);

    @Value("${datahub.countrycodefile}")
    private String countryCodeFilePathValue;

    @Autowired
    @Lazy
    private JdbcTemplate jdbcTemplate;

    @Autowired
    @Lazy
    private DataSource dataSource;

    private boolean tableExists = false;
    private boolean tableFull = false;

    private static String DEFAULT_COUNTRY_CODE_FILE = "data/country-codes_json.json";

    private InputStream countryCodeStream;

    private ObjectMapper mapper;

    private static final String TABLE_NAME = "countryCodes";

    private static final String COUNT_ALL_ROWS = String.format("SELECT count(*) FROM %s", TABLE_NAME);

    private static final String INSERT_RECORD = String.format("INSERT INTO %s VALUES " +
            "(?, ?, ?, ?, ?, ?);", TABLE_NAME);

    private static final String LOOKUP_ISO2_BY_NAME = String.format("SELECT * FROM %s WHERE LOWER(name) = :name",
            TABLE_NAME);

    private static final String TRUNCATE_TABLE = String.format("TRUNCATE TABLE %s", TABLE_NAME);

    private static final Map<DbPlatform, String> DDL_MAP = Map.of(DbPlatform.MYSQL, "sql/countryCodes_common.sql",
            DbPlatform.H2, "sql/countryCodes_common.sql");

    /**
     * Default constructor
     */
    public DataHubCountryCodeLookupService() {
        this.mapper = new ObjectMapper();
    }

    @PostConstruct
    public void initialize() {
        checkTables();
    }

    /**
     * Check if tables are ready to go.
     */
    private void checkTables() {
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
                    final String location = (countryCodeFilePathValue == null ? DEFAULT_COUNTRY_CODE_FILE :
                            countryCodeFilePathValue);
                    if (location == null) {
                        throw new InternalApplicationError("datahub.countrycodefile cannot be null.");
                    } else if (location.startsWith("classpath:")) {
                        final String relativePath = location.substring(10);
                        countryCodeStream = Objects.requireNonNull(this.getClass().getClassLoader()
                                .getResourceAsStream(relativePath));
                    } else if (location.startsWith("file:")) {
                        final String relativePath = location.substring(5);
                        countryCodeStream = new FileInputStream(relativePath);
                    }
                    LOGGER.info("Table {} is empty, parsing from file {}", TABLE_NAME, location);
                    parseCountries();
                } catch (final DataAccessException | IOException e) {
                    throw new InternalApplicationError(e);
                }
            }
            tableFull = true;
        }
    }

    @Override
    public DataHubCountryCode lookupCountry(final String countryName) throws MissingCountryCodeException {
        if (countryName == null || countryName.isEmpty()) {
            throw new IllegalArgumentException("country name cannot be null or blank");
        }
        checkTables();
        final String normalizedCountry = countryName.toLowerCase().trim();
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("name", normalizedCountry);
        final NamedParameterJdbcTemplate named = new NamedParameterJdbcTemplate(dataSource);
        try {
            return named.queryForObject(LOOKUP_ISO2_BY_NAME, parameterSource,
                    (resultSet, rowNum) ->
                            new DataHubCountryCode(
                                    resultSet.getString("name"),
                                    resultSet.getString("capital"),
                                    resultSet.getString("continent"),
                                    resultSet.getString("iso3166_alpha2"),
                                    resultSet.getString("iso3166_alpha3"),
                                    resultSet.getString("iso3166_numeric")
                            )
            );
        } catch (final EmptyResultDataAccessException e) {
            throw new MissingCountryCodeException(String.format("Country (%s) not found", normalizedCountry));
        }
    }

    /**
     * Parses the source file and inserts records to the database.
     * @throws IOException Problems parsing the JSON.
     * @throws DataAccessException Problems inserting rows.
     */
    private void parseCountries() throws IOException, DataAccessException{
            final List<DataHubCountryCode> codes = this.mapper.readValue(countryCodeStream,
                    new TypeReference<List<DataHubCountryCode>>() {});
            final List<DataHubCountryCode> finalCodes = codes.parallelStream().filter(c -> c.getCountryName() != null)
                    .collect(Collectors.toList());
            LOGGER.debug("codes has {} elements, finalCodes has {} elements", codes.size(), finalCodes.size());

            jdbcTemplate.batchUpdate(
                    INSERT_RECORD,
                    finalCodes,
                    50,
                    (ps, code) -> {
                        ps.setString(1, code.getCountryName());
                        ps.setString(2, code.getCapital());
                        ps.setString(3, code.getContinent());
                        ps.setString(4, code.getIso3316Alpha2());
                        ps.setString(5, code.getIso3316Alpha3());
                        ps.setString(6, code.getIso3316Numeric());
                    }
            );
            LOGGER.info("Loaded {} records into table {}", finalCodes.size(), TABLE_NAME);
    }

    /**
     * Truncate the tables and reload the data.
     */
    public void reset() {
        try {
            jdbcTemplate.execute(TRUNCATE_TABLE);
            tableFull = false;
            parseCountries();
            tableFull = true;
        } catch (final DataAccessException | IOException e) {
            throw new InternalApplicationError(e);
        }
    }
}
