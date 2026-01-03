package tn.economic.system.ai;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class PythonForecastClient {

    private final String baseUrl; // ex: http://127.0.0.1:8000
    private final HttpClient http;
    private final ObjectMapper om;

    public PythonForecastClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.http = HttpClient.newHttpClient();
        this.om = new ObjectMapper();
    }

    public ForecastResponse predict(String region, int horizon) throws Exception {
        String url = baseUrl
                + "/predict?region=" + URLEncoder.encode(region, StandardCharsets.UTF_8)
                + "&horizon=" + horizon;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200) {
            throw new RuntimeException("Python API error HTTP " + resp.statusCode() + ": " + resp.body());
        }

        return om.readValue(resp.body(), ForecastResponse.class);
    }

    // JSON response mapping
    public static class ForecastResponse {
        public String region;
        public int horizon_days;
        public String asof_date;
        public double predicted_px_moyen;

        @Override
        public String toString() {
            return "ForecastResponse{" +
                    "region='" + region + '\'' +
                    ", horizon_days=" + horizon_days +
                    ", asof_date='" + asof_date + '\'' +
                    ", predicted_px_moyen=" + predicted_px_moyen +
                    '}';
        }
    }
}