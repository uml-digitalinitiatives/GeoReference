package ca.umanitoba.libraries.georeferencing.impl;

import ca.umanitoba.libraries.georeferencing.api.CountryCode;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Component;

/**
 * POJO class for country code information
 * @author whikloj
 */
@Component
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataHubCountryCode implements CountryCode {

    @JsonProperty("CLDR display name")
    private String countryName;
    @JsonProperty("Capital")
    private String capital;
    @JsonProperty("Continent")
    private String continent;
    @JsonProperty("ISO3166-1-Alpha-2")
    private String iso3316Alpha2;
    @JsonProperty("ISO3166-1-Alpha-3")
    private String iso3316Alpha3;
    @JsonProperty("ISO3166-1-numeric")
    private String iso3316Numeric;

    public DataHubCountryCode() {
        // This constructor left intentionally blank.
    }

    public DataHubCountryCode(
            final String countryName,
            final String capital,
            final String continent,
            final String alpha2,
            final String alpha3,
            final String alphaNum
    ) {
        this.countryName = countryName;
        this.capital = capital;
        this.continent = continent;
        this.iso3316Alpha2 = alpha2;
        this.iso3316Alpha3 = alpha3;
        this.iso3316Numeric = alphaNum;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(final String countryName) {
        this.countryName = countryName;
    }

    public String getCapital() {
        return capital;
    }

    public void setCapital(final String capital) {
        this.capital = capital;
    }

    public String getContinent() {
        return continent;
    }

    public void setContinent(final String continent) {
        this.continent = continent;
    }

    public String getIso3316Alpha2() {
        return iso3316Alpha2;
    }

    public void setIso3316Alpha2(final String iso3316Alpha2) {
        this.iso3316Alpha2 = iso3316Alpha2;
    }

    public String getIso3316Alpha3() {
        return iso3316Alpha3;
    }

    public void setIso3316Alpha3(final String iso3316Alpha3) {
        this.iso3316Alpha3 = iso3316Alpha3;
    }

    public String getIso3316Numeric() {
        return iso3316Numeric;
    }

    public void setIso3316Numeric(final String iso3316Numeric) {
        this.iso3316Numeric = iso3316Numeric;
    }
}
