package project.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WeatherService {

    @Value("${weather.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getWeather(String city) {
        try {
            // FIX: Encode city name
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
            String url = "https://api.weatherapi.com/v1/current.json?key=" + apiKey + "&q=" + encodedCity;

            return restTemplate.getForObject(url, String.class);
        } catch (Exception e) {
            System.err.println("Weather API error: " + e.getMessage());
            return "{}";
        }
    }
}