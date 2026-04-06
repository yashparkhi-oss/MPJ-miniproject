package project.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AiRecommendationService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.model}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    public String getAiAdvice(String location, double budget, double spent, String themeOrCategories) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String prompt;
            double temperature = 0.3; 

            if ("General".equalsIgnoreCase(location) || location == null) {
                temperature = 0.8; 
                prompt = String.format(
                    "Role: Expert Travel Guide. Task: Give an interesting, detailed travel fact or budget tip about: %s. " +
                    "Constraint: Do NOT mention Bali. Focus on specific cities, local food prices in ₹, or hidden activities. " +
                    "Format: One paragraph, max 40 words.", themeOrCategories);
            } else {
                double remaining = budget - spent;
                prompt = String.format(
                    "Context: Trip to %s. Currency: INR (₹). Total Budget: ₹%.2f. " +
                    "Action: 1. Provide 2 ultra-short budget tips (10 words each). " +
                    "2. Provide a 'Suggested Split' for this location: Tell me what percentage of the budget " +
                    "should go to Travel, Food, Stay, Shopping, Others ... " +
                    "Example: 'Travel (20%%), Food (30%%)...' " +
                    "Strictly use ₹ and keep the total response under 50 words.",
                    location, budget);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("temperature", temperature); 
            body.put("messages", List.of(Map.of("role", "user", "content", prompt)));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, entity, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("choices")) {
                List choices = (List) response.getBody().get("choices");
                Map firstChoice = (Map) choices.get(0);
                Map message = (Map) firstChoice.get("message");
                return (String) message.get("content");
            }
            return "AI tips are currently unavailable.";

        } catch (Exception e) {
            System.err.println("Groq AI Error: " + e.getMessage());
            return "Our AI guide is taking a break!";
        }
    }
}