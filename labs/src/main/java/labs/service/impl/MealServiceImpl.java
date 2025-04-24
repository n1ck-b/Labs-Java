package labs.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import labs.aspect.LogExecution;
import labs.dao.DayDao;
import labs.dao.MealDao;
import labs.dao.ProductDao;
import labs.dto.MealDto;
import labs.exception.ExceptionMessages;
import labs.exception.NotFoundException;
import labs.exception.ValidationException;
import labs.model.Day;
import labs.model.Meal;
import labs.model.Product;
import labs.service.MealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@LogExecution
@Validated
public class MealServiceImpl implements MealService {
    private final MealDao mealDao;
    private final DayDao dayDao;
    private final ProductDao productDao;

    @Autowired
    public MealServiceImpl(MealDao mealDao, DayDao dayDao, ProductDao productDao) {
        this.mealDao = mealDao;
        this.dayDao = dayDao;
        this.productDao = productDao;
    }

    @Override
    public List<MealDto> getMealsByDayId(int dayId) {
        List<Meal> meals = mealDao.getMealsByDayId(dayId);
        if (meals.isEmpty()) {
            throw new NotFoundException(String.format(ExceptionMessages.DAY_NOT_FOUND, dayId));
        }
        return meals.stream().map(MealDto::toDto).toList();
    }

    @Override
    public int addMeal(int dayId, @Valid MealDto meal) {
        if (!dayDao.existsById(dayId)) {
            throw new NotFoundException(String.format(ExceptionMessages.DAY_NOT_FOUND, dayId));
        }
        return mealDao.addMeal(dayId, meal.fromDto());
    }

    @Override
    public ResponseEntity<String> deleteMealsByDayId(int dayId) {
        if (!dayDao.existsById(dayId)) {
            throw new NotFoundException(String.format(ExceptionMessages.DAY_NOT_FOUND, dayId));
        }
        return mealDao.deleteMealsByDayId(dayId);
    }

    @Override
    public ResponseEntity<String> deleteMealByDayIdAndMealId(int dayId, int mealId) {
        if (!dayDao.existsById(dayId)) {
            throw new NotFoundException(String.format(ExceptionMessages.DAY_NOT_FOUND, dayId));
        }
        if (!mealDao.existsById(mealId)) {
            throw new NotFoundException(String.format(ExceptionMessages.MEAL_NOT_FOUND, mealId));
        }
        return mealDao.deleteMealsByDayIdAndMealId(dayId, mealId);
    }

    @Override
    public ResponseEntity<MealDto> updateMealById(JsonPatch json, int id) throws
            JsonProcessingException, JsonPatchException {
        if (!mealDao.existsById(id)) {
            throw new NotFoundException(String.format(ExceptionMessages.MEAL_NOT_FOUND, id));
        }
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        Meal meal = mealDao.getMealById(id);
        if (json.toString().contains("day") || json.toString().contains("id") ||
                json.toString().contains("products")) {
            throw new ValidationException(String.format(ExceptionMessages.PATCH_VALIDATION_EXCEPTION,
                    "'day', 'id' and 'products'"));
        }
        JsonNode node;
        node = json.apply(objectMapper.convertValue(meal, JsonNode.class));
        Day day = meal.getDay();
        List<Product> products = new ArrayList<>(meal.getProducts());
        meal = objectMapper.treeToValue(node, Meal.class);
        meal.setDay(day);
        Meal updatedMeal = mealDao.updateMeal(id, meal);
        updatedMeal.setProducts(products);
        productDao.saveProductsWeightToTable(products, updatedMeal.getId());
        return ResponseEntity.ok(MealDto.toDto(updatedMeal));
    }

    @Override
    public List<MealDto> getAllMeals() {
        List<Meal> meals = mealDao.getAllMeals();
        if (meals.isEmpty()) {
            throw new NotFoundException(ExceptionMessages.MEALS_NOT_FOUND);
        }
        return meals.stream().map(MealDto::toDto).toList();
    }

    @Override
    public MealDto getMealById(int id) {
        if (!mealDao.existsById(id)) {
            throw new NotFoundException(String.format(ExceptionMessages.MEAL_NOT_FOUND, id));
        }
        Meal meal = mealDao.getMealById(id);
        return MealDto.toDto(meal);
    }

    @Override
    public ResponseEntity<String> deleteMealById(int id) {
        if (!mealDao.existsById(id)) {
            throw new NotFoundException(String.format(ExceptionMessages.MEAL_NOT_FOUND, id));
        }
        return mealDao.deleteMealById(id);
    }

    @Override
    public List<MealDto> getMealsByProductName(@NotNull String productName) {
        List<Meal> meals = mealDao.getMealsByProductName(productName);
        if (meals.isEmpty()) {
            throw new NotFoundException(ExceptionMessages.MEALS_NOT_FOUND_BY_PRODUCT_NAME);
        }
        return meals.stream().map(MealDto::toDto).collect(Collectors.toList());
    }
}
