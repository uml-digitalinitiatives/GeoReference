CREATE TABLE IF NOT EXISTS featureCodes (
    featureClass varchar(1) not null,
    code varchar(10) not null,
    name varchar(100) not null,
    description varchar(200)
);

SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'featureCodes' AND index_name = 'featureCodes_idx' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX featureCodes_idx ON featureCodes (featureClass, code)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
