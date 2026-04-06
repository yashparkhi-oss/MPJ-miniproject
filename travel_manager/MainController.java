@Controller
public class MainController {

    @Autowired
    TripRepo tripRepo;

    @Autowired
    ExpenseRepo expenseRepo;

    @GetMapping("/")
    public String home(Model m) {
        m.addAttribute("trips", tripRepo.findAll());
        return "index";
    }

    @PostMapping("/addTrip")
    public String addTrip(String name, double budget) {
        Trip t = new Trip();
        t.setName(name);
        t.setBudget(budget);
        tripRepo.save(t);
        return "redirect:/";
    }
}