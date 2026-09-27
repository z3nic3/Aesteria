package ais;

public class VesselResult {

    String eventId;
    String mmsi;

    double latitude;
    double longitude;

    double distance;
    long timeDifference;

    double sog;
    double cog;

    String timestamp;

    double correlationScore;

    public VesselResult(
            String eventId,
            String mmsi,
            double latitude,
            double longitude,
            double distance,
            long timeDifference,
            double sog,
            double cog,
            String timestamp,
            double correlationScore) {

        this.eventId = eventId;
        this.mmsi = mmsi;

        this.latitude = latitude;
        this.longitude = longitude;

        this.distance = distance;
        this.timeDifference = timeDifference;

        this.sog = sog;
        this.cog = cog;

        this.timestamp = timestamp;

        this.correlationScore = correlationScore;
    }
}