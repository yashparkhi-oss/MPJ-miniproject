package project.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AQIService {

    @Value("${aqi.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public Integer getAQI(String city) {
        try {
            // FIX: Encode city name to handle spaces (e.g., "Koh Rong")
            String encodedCity = URLEncoder.encode(city, StandardCharsets.UTF_8);
            String url = "https://api.waqi.info/feed/" + encodedCity + "/?token=" + apiKey;

            String response = restTemplate.getForObject(url, String.class);
            JSONObject obj = new JSONObject(response);

            if ("ok".equals(obj.getString("status"))) {
                return obj.getJSONObject("data").getInt("aqi");
            } 
            return null;
        } catch (Exception e) {
            System.err.println("AQI Error: " + e.getMessage());
            return null;
        }
    }
}