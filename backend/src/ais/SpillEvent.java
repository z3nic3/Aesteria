package ais;

public class SpillEvent {

    // Basic event information
    String eventId;

    // Detected spill information
    double detectedLat;
    double detectedLon;
    String detectedTime;
    double detectionConfidence;

    // Spill origin
    double originLat;
    double originLon;
    double originRadiusKm;
    String originTimeStart;
    String originTimeEnd;

    // Forecast information
    double forecastLat;
    double forecastLon;
    double forecastRadiusKm;
    String forecastTime;

    // Type of coordinates/data
    String coordinatesType;

    public SpillEvent(
            String eventId,

            double detectedLat,
            double detectedLon,
            String detectedTime,
            double detectionConfidence,

            double originLat,
            double originLon,
            double originRadiusKm,
            String originTimeStart,
            String originTimeEnd,

            double forecastLat,
            double forecastLon,
            double forecastRadiusKm,
            String forecastTime,

            String coordinatesType
    ) {

        this.eventId = eventId;

        this.detectedLat = detectedLat;
        this.detectedLon = detectedLon;
        this.detectedTime = detectedTime;
        this.detectionConfidence = detectionConfidence;

        this.originLat = originLat;
        this.originLon = originLon;
        this.originRadiusKm = originRadiusKm;
        this.originTimeStart = originTimeStart;
        this.originTimeEnd = originTimeEnd;

        this.forecastLat = forecastLat;
        this.forecastLon = forecastLon;
        this.forecastRadiusKm = forecastRadiusKm;
        this.forecastTime = forecastTime;

        this.coordinatesType = coordinatesType;
    }
}