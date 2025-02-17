package labs.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import labs.Product;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl implements ProductService {
    @Override
    public Product getProductByName(String productName) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request requestForExternalApi = new Request.Builder().url("https://api.calorieninjas.com/v1/nutrition?query=" + productName).addHeader("X-Api-Key", "").build();
        Response responseFromExternalApi = client.newCall(requestForExternalApi).execute();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode treeRoot = mapper.readTree(responseFromExternalApi.body().string());
        Product product = mapper.treeToValue(treeRoot.get("items").get(0), Product.class);
        return product;
    }

    @Override
    public List<Product> getProductByQuery(String query) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request requestForExternalApi = new Request.Builder().url("https://api.calorieninjas.com/v1/nutrition?query=" + query).addHeader("X-Api-Key", "").build();
        Response responseFromExternalApi = client.newCall(requestForExternalApi).execute();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(responseFromExternalApi.body().string());
        List<Product> products = mapper.treeToValue(node.get("items"), new TypeReference<List<Product>>() {});
        return products;
    }
}
