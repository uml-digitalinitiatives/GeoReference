CREATE TABLE IF NOT EXISTS admin1Codes (
    country varchar(2) not null,
    code varchar(50) not null,
    name varchar(100) not null
);

CREATE INDEX IF NOT EXISTS admin1Codes_idx1 ON admin1Codes (country, code);

CREATE INDEX IF NOT EXISTS admin1Codes_idx2 ON admin1Codes (name);
