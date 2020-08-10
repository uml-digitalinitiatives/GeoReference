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
CREATE INDEX IF NOT EXISTS placeLookup_idx1 ON placeLookup (name, countryCode);

--- index on name, countryCode and adminCode1
CREATE INDEX placeLookup_idx2 ON placeLookup (name, countryCode, adminCode1);
