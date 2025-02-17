package labs;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class Meal {
    private String mealType;
    private List<Product> products;
}
