package ca.umanitoba.libraries.georeferencing.api;

import ca.umanitoba.libraries.georeferencing.exceptions.MissingCountryCodeException;

/**
 * Service to take a country name and return a country code object.
 */
public interface CountryCodeLookupService {

    /**
     * Lookup a country by its name.
     * @param countryName the country name.
     * @return a country code object.
     * @throws MissingCountryCodeException if the country cannot be converted to country code.
     */
    public CountryCode lookupCountry(final String countryName) throws MissingCountryCodeException;

}
