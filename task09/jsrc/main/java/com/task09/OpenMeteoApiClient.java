package com.task09;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class OpenMeteoApiClient {
    private static final String BASE_URL = "https://api.open-meteo.com/v1/forecast";
    private HttpClient httpClient;
    private ObjectMapper objectMapper;

    public OpenMeteoApiClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public Map<String, Object> getWeatherData(double latitude, double longitude) throws IOException, InterruptedException {
        String url = String.format("%s?latitude=%f&longitude=%f&current=temperature_2m,wind_speed_10m&hourly=temperature_2m,relative_humidity_2m,wind_speed_10m", BASE_URL, latitude, longitude);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        return parseJsonToMap(response.body());
    }

    private Map<String, Object> parseJsonToMap(String json) throws IOException {
        return objectMapper.readValue(json, Map.class);
    }
}
