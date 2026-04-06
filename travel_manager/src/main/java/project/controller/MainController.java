package project.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import project.entity.Expense;
import project.entity.Trip;
import project.entity.User;
import project.repository.ExpenseRepo;
import project.repository.TripRepo;
import project.repository.UserRepo;
import project.service.AQIService;
import project.service.AiRecommendationService;
import project.service.WeatherService;

@Controller
public class MainController {

    @Autowired
    private TripRepo tripRepo;

    @Autowired
    private ExpenseRepo expenseRepo;

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private AQIService aqiService;

    @Autowired
    private AiRecommendationService aiService;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private BCryptPasswordEncoder encoder;

    /**
     * Safely retrieves the currently logged-in user from the database.
     */
    private User getLoggedInUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName().equals("anonymousUser")) {
            return null;
        }
        return userRepo.findByUsername(auth.getName());
    }

    /**
     * Extracts percentage recommendations from the AI's text and converts them to actual budgets.
     */
    private Map<String, Double> extractAiAllocations(String aiAdvice, double totalBudget) {
        Map<String, Double> allocations = new HashMap<>();
        
        // Default Fallbacks (if the AI fails to generate numbers)
        allocations.put("Stay", totalBudget * 0.30);
        allocations.put("Food", totalBudget * 0.25);
        allocations.put("Travel", totalBudget * 0.20);
        allocations.put("Shopping", totalBudget * 0.15);
        allocations.put("Others", totalBudget * 0.10);

        if (aiAdvice == null || aiAdvice.isEmpty()) return allocations;

        try {
            String[] categories = {"Stay", "Food", "Travel", "Shopping", "Others"};
            Map<String, Double> aiAllocs = new HashMap<>();
            
            for (String cat : categories) {
                // Regex: Looks for the Category name, followed by any non-digits, then captures the digits
                Pattern p = Pattern.compile(cat + "[^0-9]*(\\d+)", Pattern.CASE_INSENSITIVE);
                Matcher m = p.matcher(aiAdvice);
                
                if (m.find()) {
                    double percentage = Double.parseDouble(m.group(1)) / 100.0;
                    aiAllocs.put(cat, totalBudget * Math.min(percentage, 1.0)); // Cap at 100%
                }
            }

            // If AI gave us valid data, map it and fill in any missing gaps with defaults
            if (!aiAllocs.isEmpty()) {
                for (String cat : categories) {
                    if (!aiAllocs.containsKey(cat)) {
                        aiAllocs.put(cat, allocations.get(cat)); 
                    }
                }
                return aiAllocs;
            }
        } catch (Exception e) {
            System.err.println("Could not parse AI advice, using default splits: " + e.getMessage());
        }
        
        return allocations;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/")
    public String home(Model model) {
        User user = getLoggedInUser();
        if (user == null) {
            return "redirect:/login";
        }
        
        try {
            String greeting = aiService.getAiAdvice("General", 0, 0, "Budget Travel Tips");
            model.addAttribute("aiGreeting", greeting);
        } catch (Exception e) {
            model.addAttribute("aiGreeting", "Our AI guide is taking a quick break!");
        }

        model.addAttribute("trips", tripRepo.findByUser(user));
        return "index";
    }

    @PostMapping("/register")
    public String processRegister(@RequestParam String username, @RequestParam String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(encoder.encode(password));
        userRepo.save(user);
        return "redirect:/login";
    }

    @PostMapping("/add-trip")
    public String addTrip(@RequestParam String name,
                        @RequestParam Double budget, 
                        @RequestParam String location) {
        try {
            User user = getLoggedInUser();
            
            if (user == null) {
                System.err.println("ERROR: Attempted to add trip but no user is logged in.");
                return "redirect:/login";
            }

            Trip t = new Trip();
            t.setName(name);
            t.setBudget(budget);
            t.setLocation(location);
            t.setUser(user); 
            
            tripRepo.save(t);
            System.out.println("SUCCESS: Trip '" + name + "' saved for user " + user.getUsername());
            
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR in addTrip:");
            e.printStackTrace(); 
        }
        return "redirect:/";
    }

    @GetMapping("/trip")
    public String trip(@RequestParam Long id, Model model) {
        Trip trip = tripRepo.findById(id).orElse(null);
        if (trip == null) return "redirect:/";

        List<Expense> expenses = trip.getExpenses();
        double total = expenses.stream().mapToDouble(Expense::getAmount).sum();
        double remaining = trip.getBudget() - total;
        boolean overBudget = total > trip.getBudget();

        // Grouping logic for charts/summaries
        Map<String, Double> grouped = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.summingDouble(Expense::getAmount)));

        Expense maxExpense = expenses.stream()
                .max((a, b) -> Double.compare(a.getAmount(), b.getAmount()))
                .orElse(null);

        Map<LocalDate, Double> daily = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getDate,
                        Collectors.summingDouble(Expense::getAmount)));

        // Budget distribution suggestions
        double budget = trip.getBudget();
        double travelB = budget * 0.3;
        double stayB = budget * 0.4;
        double foodB = budget * 0.2;
        double otherB = budget * 0.1;

        String suggestion = "Balanced budget recommended";
        if (trip.getLocation() != null) {
            if (trip.getLocation().equalsIgnoreCase("Goa")) {
                suggestion = "Increase Stay & Food budget";
            } else if (trip.getLocation().equalsIgnoreCase("Manali")) {
                suggestion = "Increase Travel budget";
            }
        }

        // External API Calls (Weather, AQI, AI)
        String aqiDisplay = "Unknown";
        String weatherCondition = "N/A";
        double temperature = 0;

        try {
            Integer aqi = aqiService.getAQI(trip.getLocation());
            if (aqi != null) aqiDisplay = aqi.toString();
        } catch (Exception e) {
            System.err.println("AQI Error: " + e.getMessage());
        }

        try {
            String weatherJsonStr = weatherService.getWeather(trip.getLocation());
            ObjectMapper mapper = new ObjectMapper();
            JsonNode weatherJson = mapper.readTree(weatherJsonStr);
            if (weatherJson.has("current")) {
                JsonNode current = weatherJson.get("current");
                temperature = current.get("temp_c").asDouble();
                weatherCondition = current.get("condition").get("text").asText();
            }
        } catch (Exception e) {
            System.err.println("Weather Error: " + e.getMessage());
        }

        String categoriesList = expenses.stream()
                .map(Expense::getCategory)
                .distinct()
                .collect(Collectors.joining(", "));

        String contextInfo = String.format("Weather: %s (%.1f°C), AQI: %s", 
                                            weatherCondition, temperature, aqiDisplay);

        String aiAdvice = aiService.getAiAdvice(
            trip.getLocation() + " (" + contextInfo + ")", 
            trip.getBudget(), 
            total, 
            categoriesList.isEmpty() ? "None yet" : categoriesList
        );

        // --- DYNAMIC AI BUDGET REBALANCING LOGIC ---
        // Pass the generated AI advice text into our helper method to extract limits!
        Map<String, Double> allocations = extractAiAllocations(aiAdvice, trip.getBudget());

        boolean needsAdjustment = false;
        double totalOverspent = 0;
        Map<String, Double> adjustedAllocations = new HashMap<>(allocations);

        for (String cat : allocations.keySet()) {
            double catSpent = grouped.getOrDefault(cat, 0.0);
            // Check if what they spent is greater than the AI's suggested limit for that category
            if (catSpent > allocations.get(cat)) {
                needsAdjustment = true;
                totalOverspent += (catSpent - allocations.get(cat));
                adjustedAllocations.put(cat, catSpent); 
            }
        }

        if (needsAdjustment) {
            double availableToReduce = 0;
            for (String cat : allocations.keySet()) {
                double catSpent = grouped.getOrDefault(cat, 0.0);
                if (catSpent < allocations.get(cat)) {
                    availableToReduce += (allocations.get(cat) - catSpent);
                }
            }

            if (availableToReduce > 0) {
                for (String cat : allocations.keySet()) {
                    double catSpent = grouped.getOrDefault(cat, 0.0);
                    if (catSpent < allocations.get(cat)) {
                        double room = allocations.get(cat) - catSpent;
                        double reduction = totalOverspent * (room / availableToReduce);
                        adjustedAllocations.put(cat, allocations.get(cat) - reduction);
                    }
                }
            }
        }
        // -------------------------------------------

        // Add attributes to Model
        model.addAttribute("trip", trip);
        model.addAttribute("total", total);
        model.addAttribute("remaining", remaining);
        model.addAttribute("overBudget", overBudget);
        model.addAttribute("categories", grouped.keySet());
        model.addAttribute("amounts", grouped.values());
        model.addAttribute("maxExpense", maxExpense);
        model.addAttribute("dailyData", daily);
        model.addAttribute("travelB", travelB);
        model.addAttribute("stayB", stayB);
        model.addAttribute("foodB", foodB);
        model.addAttribute("otherB", otherB);
        model.addAttribute("suggestion", suggestion);
        model.addAttribute("aqi", aqiDisplay);
        model.addAttribute("weather", weatherCondition + ", " + temperature + "°C");
        model.addAttribute("aiAdvice", aiAdvice);
        
        // Rebalance variables
        model.addAttribute("needsAdjustment", needsAdjustment);
        model.addAttribute("adjLabels", adjustedAllocations.keySet());
        model.addAttribute("adjData", adjustedAllocations.values());
        
        return "trip";
    }

    @PostMapping("/addExpense")
    public String addExpense(@RequestParam Long tripId, @RequestParam String category,
                            @RequestParam double amount, @RequestParam String description,
                            RedirectAttributes redirectAttributes) {
        Trip trip = tripRepo.findById(tripId).orElse(null);
        if (trip == null) return "redirect:/";

        double currentTotal = trip.getExpenses().stream().mapToDouble(Expense::getAmount).sum();

        if (currentTotal + amount > trip.getBudget()) {
            redirectAttributes.addFlashAttribute("budgetError", "Expense rejected! Adding ₹" + amount + " would exceed your total budget of ₹" + trip.getBudget() + ".");
            return "redirect:/trip?id=" + tripId;
        }

        Expense e = new Expense();
        e.setCategory(category);
        e.setAmount(amount);
        e.setDescription(description);
        e.setDate(LocalDate.now());
        e.setTrip(trip);
        expenseRepo.save(e);

        return "redirect:/trip?id=" + tripId;
    }

    @PostMapping("/deleteExpense")
    public String deleteExpense(@RequestParam Long id,
                                @RequestParam Long tripId) {
        expenseRepo.deleteById(id);
        return "redirect:/trip?id=" + tripId;
    }

    @PostMapping("/deleteTrip")
    public String deleteTrip(@RequestParam Long id) {
        tripRepo.deleteById(id);
        return "redirect:/";
    }

    @PostMapping("/updateExpense")
    public String updateExpense(@RequestParam Long id,
                                @RequestParam String category,
                                @RequestParam double amount,
                                @RequestParam String description,
                                @RequestParam Long tripId) {

        Expense e = expenseRepo.findById(id).orElse(null);
        if (e != null) {
            e.setCategory(category);
            e.setAmount(amount);
            e.setDescription(description);
            expenseRepo.save(e);
        }
        return "redirect:/trip?id=" + tripId;
    }
}