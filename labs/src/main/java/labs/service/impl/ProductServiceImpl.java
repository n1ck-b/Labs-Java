package labs.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import labs.Product;
import labs.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class ProductServiceImpl implements ProductService {
    @Override
    public Product getProductByName(int id) throws IOException {
        long startTime = System.currentTimeMillis();
        String query = switch (id) {
            case 1 -> "soup";
            case 2 -> "milk";
            case 3 -> "bread";
            case 4 -> "cheese";
            case 5 -> "chicken";
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        };
        ObjectMapper mapper = new ObjectMapper();
        JsonNode treeRoot = getJsonFromExternalApi(query);
        log.info("time elapsed for product by name = " + (System.currentTimeMillis() - startTime));
        return mapper.treeToValue(treeRoot.get("items").get(0), Product.class);
    }

    @Override
    public List<Product> getProductByQuery(String query) throws IOException {
        long startTime = System.currentTimeMillis();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = ProductServiceImpl.getJsonFromExternalApi(query);
        log.info("time elapsed for product by query = " + (System.currentTimeMillis() - startTime));
        return mapper.treeToValue(node.get("items"), new TypeReference<List<Product>>() {});
    }

    private static JsonNode getJsonFromExternalApi(String query) throws IOException {
        OkHttpClient client = new OkHttpClient();
        Request requestForExternalApi = new Request.Builder().url("https://api.calorieninjas.com/v1/nutrition?query=" + query).addHeader("X-Api-Key", "").build();
        long startTime = System.currentTimeMillis();
        Response responseFromExternalApi = client.newCall(requestForExternalApi).execute();
        log.info("time elapsed for API = " + (System.currentTimeMillis() - startTime));
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(responseFromExternalApi.body().string());
    }
}
