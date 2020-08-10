package ca.umanitoba.libraries.georeferencing.api;

/**
 * Interface for a class to store information about a country.
 * @author whikloj
 */
public interface CountryCode {

    /**
     * Get the country name.
     * @return the country name.
     */
    public String getCountryName();

    /**
     * Set the country name.
     * @param countryName the country name.
     */
    public void setCountryName(final String countryName);

    /**
     * Get the capital of the country.
     * @return the capital.
     */
    public String getCapital();

    /**
     * Set the capital of the country.
     * @param capital the capital.
     */
    public void setCapital(final String capital);

    /**
     * Get the continent this country is on.
     * @return the continent.
     */
    public String getContinent();

    /**
     * Set the continent this country is on.
     * @param continent the continent.
     */
    public void setContinent(final String continent);

    /**
     * Get the ISO-3316 Alpha 2 country code.
     * @return the alpha 2 country code
     */
    public String getIso3316Alpha2();

    /**
     * Set the ISO-3316 Alpha 2 country code.
     * @param iso3316Alpha2 the alpha 2 country code
     */
    public void setIso3316Alpha2(final String iso3316Alpha2);

    /**
     * Get the ISO-3316 Alpha 3 country code.
     * @return the alpha 3 country code
     */
    public String getIso3316Alpha3();

    /**
     * Set the ISO-3316 Alpha 3 country code.
     * @param iso3316Alpha3 the alpha 3 country code
     */
    public void setIso3316Alpha3(final String iso3316Alpha3);

    /**
     * Get the ISO-3316 numeric country code.
     * @return the numeric country code
     */
    public String getIso3316Numeric();

    /**
     * Set the ISO-3316 numeric country code.
     * @param iso3316Numeric  the numeric country code
     */
    public void setIso3316Numeric(final String iso3316Numeric);
}
