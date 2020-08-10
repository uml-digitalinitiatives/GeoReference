package ca.umanitoba.libraries.georeferencing;

/**
 * Simple class to hold a set of coordinates.
 * @author whikloj
 */
public class Coordinates {
    private double latitude;
    private double longitude;

    public Coordinates(final double latitude, final double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLatitude(final double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(final double longitude) {
        this.longitude = longitude;
    }

    public void setLatitude(final String latitude) {
        this.latitude = Double.parseDouble(latitude);
    }

    public void setLongitude(final String longitude) {
        this.longitude = Double.parseDouble(longitude);
    }

}
