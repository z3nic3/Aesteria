package ais;

public class VesselRecord {

    String mmsi;
    double latitude;
    double longitude;
    double sog;
    double cog;
    String timestamp;

    public VesselRecord(String mmsi, double latitude, double longitude,
                        double sog, double cog, String timestamp) {

        this.mmsi = mmsi;
        this.latitude = latitude;
        this.longitude = longitude;
        this.sog = sog;
        this.cog = cog;
        this.timestamp = timestamp;
    }
}