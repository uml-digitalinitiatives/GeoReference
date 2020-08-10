CREATE TABLE IF NOT EXISTS admin1Codes (
    country varchar(2) not null,
    code varchar(50) not null,
    name varchar(100) not null
);

--- Create index on country, code
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'admin1Codes' AND index_name = 'admin1Codes_idx1' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX admin1Codes_idx1 ON admin1Codes (country, code)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

--- Create index on name
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'admin1Codes' AND index_name = 'admin1Codes_idx2' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX admin1Codes_idx2 ON admin1Codes (name)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
