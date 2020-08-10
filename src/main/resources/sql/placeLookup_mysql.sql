CREATE TABLE IF NOT EXISTS placeLookup (
    geonamesId int not null primary key,
    name varchar(200) not null,
    asciiName varchar(200) not null,
    latitude varchar(20) not null,
    longitude varchar(20) not null,
    feature_class varchar(1) not null,
    feature varchar(10) not null,
    adminCode1 varchar(20),
    adminCode2 varchar(80),
    countryCode varchar(2) not null,
    adminCode3 varchar(20),
    adminCode4 varchar(20),
    population BIGINT,
    elevation INT,
    digitalElevationModel INT,
    timezone varchar(100),
    lastModified date
);

CREATE TABLE IF NOT EXISTS placeLookupAltNames (
    geonamesId int not null,
    name varchar(200) not null,
    CONSTRAINT fk_geonamesId FOREIGN KEY (geonamesId) REFERENCES placeLookup(geonamesId) ON DELETE CASCADE
);

--- index on name and countryCode
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'placeLookup' AND index_name = 'placeLookup_idx1' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX placeLookup_idx1 ON placeLookup (name, countryCode)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;

--- index on name, countryCode and adminCode1
SET @exist := (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_name = 'placeLookup' AND index_name = 'placeLookup_idx2' AND table_schema = database());
SET @sqlstmt := IF (@exist > 0, 'SELECT ''INFO: Index already exists.''',
    'CREATE INDEX placeLookup_idx2 ON placeLookup (name, countryCode, adminCode1)');
PREPARE stmt FROM @sqlstmt;
EXECUTE stmt;
