package ca.umanitoba.libraries.georeferencing;

import java.util.ArrayList;
import java.util.List;

import ca.umanitoba.libraries.georeferencing.exceptions.LocationRequestException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * Stores a location which can be used to perform the lookup.
 * @author whikloj
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class LocationRequest {

    @JsonProperty("country")
    private String country;
    @JsonProperty("province")
    private String province;
    private String county;
    @JsonProperty("city")
    private String city;
    private String citySection;
    private String iso3361Alpha2;
    private String hash;

    /**
     * The country code if any
     * @return the country code or null.
     */
    public String getCountry() {
        return this.country;
    }

    /**
     * Get the province/state if any.
     * @return the province/state or null.
     */
    public String getProvince() {
        return this.province;
    }

    /**
     * Get the county/municipal area if any.
     * @return the county/municipal area or null.
     */
    public String getCounty() {
        return this.county;
    }

    /**
     * Get the city/town if any.
     * @return the city/town name or null.
     */
    public String getCity() {
        return this.city;
    }

    /**
     * Get the city section if any
     * @return a city section or null
     */
    public String getCitySection() {
        return this.citySection;
    }

    /**
     * Get the country ISO-3361 Alpha 2.
     * @return the code.
     */
    public String getIso3361Alpha2() {
        return iso3361Alpha2;
    }

    /**
     * Set the country code ISO-3361 Alpha 2.
     * @param iso3361Alpha2 the country code.
     */
    public void setIso3361Alpha2(final String iso3361Alpha2) {
        this.iso3361Alpha2 = iso3361Alpha2;
    }

    /**
     * Return a hash string to identify this set of location parts.
     * @return a unique hash for this location.
     */
    public String getHashString() {
        if (this.hash == null) {
            calculateHash();
        }
        return this.hash;
    }

    /**
     * Verify the request.
     *
     * @throws LocationRequestException if there is not enough parts to process.
     */
    public void validateLocation() throws LocationRequestException {
        if (this.country == null && this.province == null && this.city == null) {
            throw new LocationRequestException("You must provide at least one of 'country', 'province' or 'city'");
        }
    }

    private void calculateHash() {
        final List<String> hashList = new ArrayList<>();
        if (this.country != null) {
            hashList.add(this.country);
        }
        if (this.province != null) {
            hashList.add(this.province);
        }
        if (this.county != null) {
            hashList.add(this.county);
        }
        if (this.city != null) {
            hashList.add(this.city);
        }
        if (this.citySection != null) {
            hashList.add(this.citySection);
        }
        final String hashString = String.join("|", hashList);
        this.hash = DigestUtils.sha1Hex(hashString.toLowerCase());
    }

    /**
     * Set the country for this location
     * @param country the country code
     */
    public void setCountry(final String country) {
        this.country = country.trim();
    }

    public void setProvince(final String province) {
        this.province = province.trim();
    }

    public void setCounty(final String county) {
        this.county = county.trim();
    }

    public void setCity(final String city) {
        this.city = city.trim();
    }

    public void setCitySection(final String citySection) {
        this.citySection = citySection.trim();
    }

    @Override
    public String toString() {
        return "LocationRequest{" +
                "country='" + country + '\'' +
                ", province='" + province + '\'' +
                ", county='" + county + '\'' +
                ", city='" + city + '\'' +
                ", citySection='" + citySection + '\'' +
                ", iso3361Alpha2='" + iso3361Alpha2 + '\'' +
                ", hash='" + hash + '\'' +
                '}';
    }
}
