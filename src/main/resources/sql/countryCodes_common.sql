CREATE TABLE IF NOT EXISTS countryCodes (
    name varchar(200) NOT NULL PRIMARY KEY,
    capital varchar(200) NULL,
    continent varchar(100) NULL,
    iso3166_alpha2 varchar(2) NOT NULL,
    iso3166_alpha3 varchar(3) NOT NULL,
    iso3166_numeric varchar(10) NOT NULL
 );