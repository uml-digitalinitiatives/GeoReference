package ca.umanitoba.libraries.georeferencing;

import static org.slf4j.LoggerFactory.getLogger;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
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

@Component
public class GeoNamesFeatureCodes {

    private static final Logger LOGGER = getLogger(GeoNamesFeatureCodes.class);

    @Autowired
    @Lazy
    private JdbcTemplate jdbcTemplate;

    @Autowired
    @Lazy
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    @Lazy
    private DataSource dataSource;

    private static String DEFAULT_GEONAMES_FEATURE_CODE_FILE = "featureCodes_en.txt";

    private InputStream featureCodeStream;

    private static String TABLE_NAME = "featureCodes";

    private static String COUNT_ALL_ROWS = String.format("SELECT count(*) FROM %s", TABLE_NAME);

    private static String INSERT_RECORD = String.format("INSERT INTO %s VALUES (?, ?, ?, ?)", TABLE_NAME);

    private static String SELECT_BY_CLASS_CODE = String.format("SELECT * FROM %s WHERE featureClass = :class AND " +
            "code = :code", TABLE_NAME);

    private static String TRUNCATE_TABLE = String.format("TRUNCATE TABLE %s", TABLE_NAME);

    private boolean tableExists = false;
    private boolean tableFull = false;

    private final static Map<DbPlatform, String> DDL_MAP = Map.of(DbPlatform.MYSQL, "sql/featureCodes_mysql.sql",
            DbPlatform.H2, "sql/featureCodes_h2.sql");

    /**
     * Default constructor.
     */
    public GeoNamesFeatureCodes() {

    }

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
                featureCodeStream = Objects.requireNonNull(this.getClass().getClassLoader()
                        .getResourceAsStream(DEFAULT_GEONAMES_FEATURE_CODE_FILE));
                LOGGER.info("Table {} is empty, parsing file {}", TABLE_NAME, DEFAULT_GEONAMES_FEATURE_CODE_FILE);
                loadFeatures();
            }
            tableFull = true;
        }
    }

    /**
     * Load the tab separated file into the database.
     */
    private void loadFeatures() {
        try {
            final List<Feature> inputData = new ArrayList<>();

            try (final BufferedReader reader = new BufferedReader(new InputStreamReader(featureCodeStream,
                    StandardCharsets.UTF_8));
                 final Stream<String> stream = reader.lines()) {
                stream.forEach(l -> {
                    final String[] p = l.split("\t");
                    if (p.length < 2) {
                        // Need two entries at least.
                        throw new InternalApplicationError(String.format("Only received 1 part from line %s", p[0]));
                    }
                    final String[] f = p[0].split("\\.");
                    final Feature feature;
                    try {
                        if (p.length > 2) {
                            feature = new Feature(f[0], f[1], p[1], p[2]);
                        } else {
                            feature = new Feature(f[0], f[1], p[1]);
                        }
                        inputData.add(feature);
                    } catch (final IndexOutOfBoundsException e) {
                        // Swallow the failure and don't load the record.
                        LOGGER.warn("Unable to load feature {}, skipping", p[0]);
                    }
                });
            }
            jdbcTemplate.batchUpdate(
                    INSERT_RECORD,
                    inputData,
                    50,
                    (ps, arg) -> {
                        ps.setString(1, arg.getFeatureClass());
                        ps.setString(2, arg.getCode());
                        ps.setString(3, arg.getName());
                        ps.setString(4, arg.getDescription());
                    }
            );
        } catch (final DataAccessException | IOException | UncheckedIOException e) {
            throw new InternalApplicationError(e);
        }
    }

    /**
     * Get the feature for a feature class and feature code
     * @param classCharacter feature class character identifier.
     * @param featureCode feature code identifier.
     * @return the Feature or null.
     */
    public Feature getFeature(final Character classCharacter, final String featureCode) {
        initialize();
        final MapSqlParameterSource parameterSource = new MapSqlParameterSource();
        parameterSource.addValue("class", String.valueOf(classCharacter));
        parameterSource.addValue("code", featureCode);
        final Feature feature = jdbcTemplate.queryForObject(SELECT_BY_CLASS_CODE,
            new Object[]{parameterSource},
                (rs, rowNum) -> new Feature(
                        rs.getString(0),
                        rs.getString(1),
                        rs.getString(2),
                        rs.getString(3)
                )
        );
        return feature;
    }

    /**
     * Reset the tables.
     */
    public void reset() {
        jdbcTemplate.execute(TRUNCATE_TABLE);
        loadFeatures();
    }

    /**
     * Class to hold feature information.
     */
    static class Feature {

        private static final Logger LOGGER = getLogger(Feature.class);

        private final String featureCode;
        private final String featureClass;
        private final String featureName;
        private String featureDescription = null;

        public Feature(final String classCode, final String code, final String name) {
            featureClass = classCode;
            featureCode = code;
            featureName = name;
        }
        public Feature(final String classCode, final String code, final String name, final String description) {
            this(classCode, code, name);
            if (description != null && description.length() > 0) {
                if (description.length() > 200) {
                    LOGGER.warn("Feature {}.{} has description length of {}, truncating", getFeatureClass(),
                            getCode(), description.length());
                    featureDescription = description.substring(0, 200);
                } else {
                    featureDescription = description;
                }
            }
        }

        public String getFeatureClass() {
            return featureClass;
        }
        public String getCode() {
            return featureCode;
        }
        public String getName() {
            return featureName;
        }
        public String getDescription() {
            return featureDescription;
        }
    }

}
