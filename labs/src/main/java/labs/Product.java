package labs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Product {
    private String name;
    @JsonProperty("serving_size_g")
    private float weight;
    @JsonProperty("calories")
    private float calories;
    @JsonProperty("protein_g")
    private float protein;
    @JsonProperty("carbohydrates_total_g")
    private float carbs;
    @JsonProperty("fat_total_g")
    private float fats;
}
