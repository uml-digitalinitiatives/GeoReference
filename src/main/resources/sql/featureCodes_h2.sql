CREATE TABLE IF NOT EXISTS featureCodes (
    featureClass varchar(1) not null,
    code varchar(10) not null,
    name varchar(100) not null,
    description varchar(200)
);

CREATE INDEX IF NOT EXISTS featureCodes_idx ON featureCodes (featureClass, code);
