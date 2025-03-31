package labs.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import java.util.List;
import java.util.stream.Collectors;
import labs.dao.MealDao;
import labs.dto.MealDto;
import labs.model.Day;
import labs.model.Meal;
import labs.service.MealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MealServiceImpl implements MealService {
    private final MealDao mealDao;

    @Autowired
    public MealServiceImpl(MealDao mealDao) {
        this.mealDao = mealDao;
    }

    @Override
    public List<MealDto> getMealsByDayId(int dayId) {
        List<Meal> meals = mealDao.getMealsByDayId(dayId);
        if (meals.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return meals.stream().map(MealDto::toDto).toList();
    }

    @Override
    public int addMeal(int dayId, MealDto meal) {
        return mealDao.addMeal(dayId, meal.fromDto());
    }

    @Override
    public ResponseEntity<String> deleteMealsByDayId(int dayId) {
        return mealDao.deleteMealsByDayId(dayId);
    }

    @Override
    public ResponseEntity<String> deleteMealByDayIdAndMealId(int dayId, int mealId) {
        return mealDao.deleteMealsByDayIdAndMealId(dayId, mealId);
    }

    @Override
    public ResponseEntity<MealDto> updateMealById(JsonPatch json, int id) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        Meal meal = mealDao.getMealById(id);
        if (json.toString().contains("day") || json.toString().contains("id") ||
                json.toString().contains("products")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        JsonNode node;
        try {
            node = json.apply(objectMapper.convertValue(meal, JsonNode.class));
        } catch (JsonPatchException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        Day day = meal.getDay();
        meal = objectMapper.treeToValue(node, Meal.class);
        meal.setDay(day);
        Meal updatedMeal = mealDao.updateMeal(id, meal);
        return ResponseEntity.ok(MealDto.toDto(updatedMeal));
    }

    @Override
    public List<MealDto> getAllMeals() {
        List<Meal> meals = mealDao.getAllMeals();
        if (meals.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return meals.stream().map(MealDto::toDto).toList();
    }

    @Override
    public MealDto getMealById(int id) {
        Meal meal = mealDao.getMealById(id);
        return MealDto.toDto(meal);
    }

    @Override
    public ResponseEntity<String> deleteMealById(int id) {
        return mealDao.deleteMealById(id);
    }

    @Override
    public List<MealDto> getMealsByProductName(String productName) {
        List<Meal> meals = mealDao.getMealsByProductName(productName);
        if (meals.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return meals.stream().map(MealDto::toDto).collect(Collectors.toList());
    }
}
