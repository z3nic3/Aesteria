package ais;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.Instant;
import java.time.Duration;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AISAttribution {

    // How far outside the friend's origin_time_start / origin_time_end window
    // we still allow a vessel ping to count.
    static final long BUFFER_MINUTES = 180; // 3 hours

    // ============================================================
    // Haversine formula
    // ============================================================
    public static double calculateDistance(
            double lat1, double lon1,
            double lat2, double lon2) {

        final double EARTH_RADIUS = 6371.0;

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    // ============================================================
    // Read AIS data from CSV
    // ============================================================
    public static List<VesselRecord> readAISData(String filePath)
            throws Exception {

        List<VesselRecord> vessels = new ArrayList<>();

        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String line;
        br.readLine(); // skip header

        while ((line = br.readLine()) != null) {

            if (line.isBlank())
                continue;

            String[] data = line.split(",");

            vessels.add(new VesselRecord(
                    data[0].trim(),
                    Double.parseDouble(data[1].trim()),
                    Double.parseDouble(data[2].trim()),
                    Double.parseDouble(data[3].trim()),
                    Double.parseDouble(data[4].trim()),
                    data[5].trim()
            ));
        }

        br.close();
        return vessels;
    }

    // ============================================================
    // Read AIS data from PostgreSQL
    // ============================================================
    public static List<VesselRecord> readAISDataFromDatabase()
            throws Exception {

        List<VesselRecord> vessels = new ArrayList<>();

        String databaseUrl = System.getenv("DATABASE_URL");

        if (databaseUrl == null || databaseUrl.isBlank()) {
                 throw new Exception("DATABASE_URL is not set.");
        }

        if (!databaseUrl.startsWith("jdbc:")) {
                 databaseUrl = "jdbc:" + databaseUrl;
        }

        String sql = """
            SELECT mmsi, latitude, longitude, sog, cog, timestamp
            FROM ais_positions
            ORDER BY timestamp
            """;

        try (
            Connection connection = DriverManager.getConnection(databaseUrl);
            PreparedStatement statement =
                    connection.prepareStatement(sql);
            ResultSet rs = statement.executeQuery()
        ) {

            while (rs.next()) {

                String timestamp = rs.getTimestamp("timestamp")
                        .toInstant()
                        .toString();

                vessels.add(new VesselRecord(
                        rs.getString("mmsi"),
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude"),
                        rs.getDouble("sog"),
                        rs.getDouble("cog"),
                        timestamp
                ));
            }
        }

        return vessels;
    }

    // ============================================================
    // Read friend's drift_summary.csv
    // ============================================================
    public static List<SpillEvent> readEvents(String filePath)
            throws Exception {

        List<SpillEvent> events = new ArrayList<>();

        try (BufferedReader br =
                     new BufferedReader(new FileReader(filePath))) {

            String headerLine = br.readLine();

            if (headerLine == null) {
                throw new Exception("Events CSV is empty.");
            }

            String[] headers = headerLine.split(",");

            Map<String, Integer> columnIndex = new HashMap<>();

            for (int i = 0; i < headers.length; i++) {
                columnIndex.put(headers[i].trim(), i);
            }

            String line;

            while ((line = br.readLine()) != null) {

                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] values = line.split(",");

                String eventId =
                        values[columnIndex.get("event_id")].trim();

                double detectedLat =
                        Double.parseDouble(
                                values[columnIndex.get("detected_lat")].trim()
                        );

                double detectedLon =
                        Double.parseDouble(
                                values[columnIndex.get("detected_lon")].trim()
                        );

                String detectedTime =
                        values[columnIndex.get("detected_time")].trim();

                double detectionConfidence =
                        Double.parseDouble(
                                values[columnIndex.get(
                                        "detection_confidence")].trim()
                        );

                double originLat =
                        Double.parseDouble(
                                values[columnIndex.get("origin_lat")].trim()
                        );

                double originLon =
                        Double.parseDouble(
                                values[columnIndex.get("origin_lon")].trim()
                        );

                double originRadiusKm =
                        Double.parseDouble(
                                values[columnIndex.get(
                                        "origin_radius_km")].trim()
                        );

                String originTimeStart =
                        values[columnIndex.get(
                                "origin_time_start")].trim();

                String originTimeEnd =
                        values[columnIndex.get(
                                "origin_time_end")].trim();

                double forecastLat =
                        Double.parseDouble(
                                values[columnIndex.get("forecast_lat")].trim()
                        );

                double forecastLon =
                        Double.parseDouble(
                                values[columnIndex.get("forecast_lon")].trim()
                        );

                double forecastRadiusKm =
                        Double.parseDouble(
                                values[columnIndex.get(
                                        "forecast_radius_km")].trim()
                        );

                String forecastTime =
                        values[columnIndex.get("forecast_time")].trim();

                String coordinatesType =
                        values[columnIndex.get(
                                "coordinates_type")].trim();

                events.add(new SpillEvent(
                        eventId,

                        detectedLat,
                        detectedLon,
                        detectedTime,
                        detectionConfidence,

                        originLat,
                        originLon,
                        originRadiusKm,
                        originTimeStart,
                        originTimeEnd,

                        forecastLat,
                        forecastLon,
                        forecastRadiusKm,
                        forecastTime,

                        coordinatesType
                ));
            }
        }

        return events;
    }

    // ============================================================
    // Time difference
    // ============================================================
    public static long calculateTimeDifference(
            String timeA,
            String timeB) {

        Instant a = Instant.parse(timeA);
        Instant b = Instant.parse(timeB);

        return Math.abs(
                Duration.between(a, b).toMinutes()
        );
    }

    // ============================================================
    // Check AIS time window
    // ============================================================
    public static boolean isWithinWindow(
            String vesselTime,
            String windowStart,
            String windowEnd,
            long bufferMinutes) {

        Instant vessel = Instant.parse(vesselTime);

        Instant start = Instant.parse(windowStart)
                .minus(Duration.ofMinutes(bufferMinutes));

        Instant end = Instant.parse(windowEnd)
                .plus(Duration.ofMinutes(bufferMinutes));

        return !vessel.isBefore(start)
                && !vessel.isAfter(end);
    }

    // ============================================================
    // Correlation score
    // ============================================================
    public static double calculateCorrelationScore(
            double distanceKm,
            long timeDifferenceMinutes) {

        double distanceScore =
                Math.max(
                        0,
                        100 - (distanceKm / 10.0) * 100
                );

        double timeScore =
                Math.max(
                        0,
                        100 - (timeDifferenceMinutes / 60.0) * 100
                );

        return (distanceScore * 0.6)
                + (timeScore * 0.4);
    }

    // ============================================================
    // Attribution for ONE spill event
    // ============================================================
    public static List<VesselResult> attributeEvent(
            SpillEvent event,
            List<VesselRecord> vessels) {

        // Keep only the best AIS ping for each unique MMSI.
        Map<String, VesselResult> bestByMmsi =
                new HashMap<>();

        for (VesselRecord vessel : vessels) {

            double distance = calculateDistance(
                    event.originLat,
                    event.originLon,
                    vessel.latitude,
                    vessel.longitude
            );

            boolean inWindow = isWithinWindow(
                    vessel.timestamp,
                    event.originTimeStart,
                    event.originTimeEnd,
                    BUFFER_MINUTES
            );

            if (distance <= event.originRadiusKm
                    && inWindow) {

                long timeDifference =
                        calculateTimeDifference(
                                event.originTimeEnd,
                                vessel.timestamp
                        );

                double score =
                        calculateCorrelationScore(
                                distance,
                                timeDifference
                        );

                VesselResult result =
                        new VesselResult(
                                event.eventId,
                                vessel.mmsi,

                                vessel.latitude,
                                vessel.longitude,

                                distance,
                                timeDifference,

                                vessel.sog,
                                vessel.cog,

                                vessel.timestamp,

                                score
                        );

                // Check whether this MMSI already has a result.
                VesselResult existing =
                        bestByMmsi.get(vessel.mmsi);

                // Keep whichever AIS ping has the higher score.
                if (existing == null
                        || result.correlationScore
                        > existing.correlationScore) {

                    bestByMmsi.put(
                            vessel.mmsi,
                            result
                    );
                }
            }
        }

        // Convert unique MMSIs back into a list.
        List<VesselResult> matches =
                new ArrayList<>(
                        bestByMmsi.values()
                );

        // Highest attribution score first.
        matches.sort(
                Comparator.comparingDouble(
                        (VesselResult r)
                                -> r.correlationScore
                ).reversed()
        );

        return matches;
    }

    // ============================================================
    // Print results
    // ============================================================
    public static void printResults(
            Map<String, List<VesselResult>> resultsByEvent) {

        for (Map.Entry<String, List<VesselResult>> entry
                : resultsByEvent.entrySet()) {

            System.out.println(
                    "\n--- " + entry.getKey() + ": "
                    + entry.getValue().size()
                    + " candidate vessel(s) ---"
            );

            for (VesselResult r : entry.getValue()) {

                System.out.printf(
                        "MMSI: %s | Distance: %.2f km "
                        + "| Time difference: %d minutes "
                        + "| Score: %.2f%n",
                        r.mmsi,
                        r.distance,
                        r.timeDifference,
                        r.correlationScore
                );
            }
        }
    }

    // ============================================================
    // JSON output, grouped by event
    // ============================================================
    public static void saveResultsAsJSON(
            List<SpillEvent> events,
            Map<String, List<VesselResult>> resultsByEvent,
            String filePath) throws Exception {

        FileWriter writer =
                new FileWriter(filePath);

        writer.write("[\n");

        for (int eventIndex = 0;
                eventIndex < events.size();
                eventIndex++) {

            SpillEvent event =
                    events.get(eventIndex);

            writer.write("  {\n");

            writer.write(
                    "      \"eventId\": \""
                    + event.eventId + "\",\n");

            writer.write(
                    "      \"detectedLat\": "
                    + event.detectedLat + ",\n");

            writer.write(
                    "      \"detectedLon\": "
                    + event.detectedLon + ",\n");

            writer.write(
                    "      \"detectedTime\": \""
                    + event.detectedTime + "\",\n");

            writer.write(
                    "      \"detectionConfidence\": "
                    + event.detectionConfidence + ",\n");

            writer.write(
                    "      \"originLat\": "
                    + event.originLat + ",\n");

            writer.write(
                    "      \"originLon\": "
                    + event.originLon + ",\n");

            writer.write(
                    "      \"originRadiusKm\": "
                    + event.originRadiusKm + ",\n");

            writer.write(
                    "      \"originTimeStart\": \""
                    + event.originTimeStart + "\",\n");

            writer.write(
                    "      \"originTimeEnd\": \""
                    + event.originTimeEnd + "\",\n");

            writer.write(
                    "      \"forecastLat\": "
                    + event.forecastLat + ",\n");

            writer.write(
                    "      \"forecastLon\": "
                    + event.forecastLon + ",\n");

            writer.write(
                    "      \"forecastRadiusKm\": "
                    + event.forecastRadiusKm + ",\n");

            writer.write(
                    "      \"forecastTime\": \""
                    + event.forecastTime + "\",\n");

            writer.write(
                    "      \"coordinatesType\": \""
                    + event.coordinatesType + "\",\n");

            writer.write(
                    "      \"vessels\": [\n");

            List<VesselResult> results =
                    resultsByEvent.getOrDefault(
                            event.eventId,
                            new ArrayList<>()
                    );

            for (int i = 0;
                    i < results.size();
                    i++) {

                VesselResult result =
                        results.get(i);

                writer.write("        {\n");

                writer.write(
                        "          \"mmsi\": \""
                        + result.mmsi + "\",\n");

                writer.write(String.format(
                        "          \"latitude\": %.6f,\n",
                        result.latitude));

                writer.write(String.format(
                        "          \"longitude\": %.6f,\n",
                        result.longitude));

                writer.write(String.format(
                        "          \"distance\": %.2f,\n",
                        result.distance));

                writer.write(
                        "          \"timeDifference\": "
                        + result.timeDifference + ",\n");

                writer.write(String.format(
                        "          \"sog\": %.1f,\n",
                        result.sog));

                writer.write(String.format(
                        "          \"cog\": %.1f,\n",
                        result.cog));

                writer.write(
                        "          \"timestamp\": \""
                        + result.timestamp + "\",\n");

                writer.write(String.format(
                        "          \"correlationScore\": %.2f\n",
                        result.correlationScore));

                writer.write(
                        i < results.size() - 1
                        ? "        },\n"
                        : "        }\n"
                );
            }

            writer.write("      ]\n");

            writer.write(
                    eventIndex < events.size() - 1
                    ? "  },\n"
                    : "  }\n"
            );
        }

        writer.write("]\n");

        writer.close();
    }

    // ============================================================
    // Reusable entry point
    // ============================================================
    public static Map<String, List<VesselResult>> runAttribution(
            String eventsFile,
            String aisFile,
            String outFile) throws Exception {

        List<SpillEvent> events =
                readEvents(eventsFile);

        List<VesselRecord> vessels =
                readAISDataFromDatabase();

        System.out.println(
                "Loaded " + events.size()
                + " spill event(s) from "
                + eventsFile
        );

        System.out.println(
                "Loaded " + vessels.size()
                + " AIS ping(s) from PostgreSQL"
        );

        Map<String, List<VesselResult>> resultsByEvent =
                new java.util.LinkedHashMap<>();

        for (SpillEvent event : events) {

            resultsByEvent.put(
                    event.eventId,
                    attributeEvent(event, vessels)
            );
        }

        printResults(resultsByEvent);

        saveResultsAsJSON(
                events,
                resultsByEvent,
                outFile
        );

        System.out.println(
                "\nWrote results to " + outFile
        );

        return resultsByEvent;
    }

    // ============================================================
    // Main
    // ============================================================
    public static void main(String[] args)
            throws Exception {

        String eventsFile =
                args.length > 0
                ? args[0]
                : "data/drift_summary.csv";

        String aisFile =
                args.length > 1
                ? args[1]
                : "data/ais_data.csv";

        String outFile =
                args.length > 2
                ? args[2]
                : "data/results.json";

        runAttribution(
                eventsFile,
                aisFile,
                outFile
        );
    }
}