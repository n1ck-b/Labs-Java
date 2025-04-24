package service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import labs.dao.DayDao;
import labs.dao.MealDao;
import labs.dao.ProductDao;
import labs.dto.MealDto;
import labs.exception.NotFoundException;
import labs.exception.ValidationException;
import labs.model.Day;
import labs.model.Meal;
import labs.service.impl.MealServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
public class MealServiceImplTest {
    private static final int ID1 = 1;
    private static final int ID2 = 2;
    private static final String PRODUCT_NAME = "rice";
    private Meal meal1;
    private Meal meal2;
    private MealDto mealDto;
    private Meal updatedMeal;
    private JsonPatch jsonPatch;
    private JsonPatch jsonPatchWithError;
    private Day day;
    private ResponseEntity<String> responseEntityForDeletion;

    @Mock
    private MealDao mealDao;

    @Mock
    private DayDao dayDao;

    @Mock
    private ProductDao productDao;

    @InjectMocks
    private MealServiceImpl mealService;

    @BeforeEach
    void setUp() throws IOException {
        day = new Day(ID1, LocalDate.of(2025, 4, 15), List.of());
        meal1 = new Meal(ID1, "breakfast", day, List.of());
        meal2 = new Meal(ID2, "lunch", day, List.of());
        updatedMeal = new Meal(ID1, "dinner", day, List.of());
        mealDto = new MealDto(ID1, "breakfast", day.getId(), List.of());
        responseEntityForDeletion = new ResponseEntity<>("Deleted successfully", HttpStatus.OK);

        String patch = """
                [
                    {
                        "op": "replace",
                        "path": "/mealType",
                        "value": "dinner"
                    }
                ]""";

        String patchWithError = """
                [
                    {
                        "op": "replace",
                        "path": "/id",
                        "value": "6"
                    }
                ]""";

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        jsonPatch = JsonPatch.fromJson(objectMapper.readTree(patch));
        jsonPatchWithError = JsonPatch.fromJson(objectMapper.readTree(patchWithError));
    }

    @Test
    void testGetMealsByDayId_WhenExists() {
        Mockito.when(mealDao.getMealsByDayId(ID1)).thenReturn(List.of(meal1, meal2));

        List<MealDto> result = mealService.getMealsByDayId(ID1);

        assertEquals(List.of(meal1, meal2).stream().map(MealDto::toDto).toList(), result);
        Mockito.verify(mealDao, Mockito.times(1)).getMealsByDayId(ID1);
    }

    @Test
    void testGetMealsByDayId_WhenNotExists() {
        Mockito.when(mealDao.getMealsByDayId(ID1)).thenReturn(List.of());

        assertThrows(NotFoundException.class, () -> mealService.getMealsByDayId(ID1));
        Mockito.verify(mealDao, Mockito.times(1)).getMealsByDayId(ID1);
    }

    @Test
    void testAddMealByDayId_WhenDayExists() {
        Mockito.when(dayDao.existsById(ID1)).thenReturn(true);
        Mockito.when(mealDao.addMeal(ID1, mealDto.fromDto())).thenReturn(ID1);

        int result = mealService.addMeal(ID1, mealDto);

        assertEquals(ID1, result);
        Mockito.verify(mealDao, Mockito.times(1)).addMeal(ID1, mealDto.fromDto());
    }

    @Test
    void testAddMealByDayId_WhenDayNotExists() {
        Mockito.when(dayDao.existsById(ID1)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> mealService.addMeal(ID1, mealDto));
        Mockito.verify(mealDao, Mockito.times(0)).addMeal(ID1, mealDto.fromDto());
    }

    @Test
    void testDeleteMealsByDayId_WhenExists() {
        Mockito.when(dayDao.existsById(ID1)).thenReturn(true);
        Mockito.when(mealDao.deleteMealsByDayId(ID1)).thenReturn(responseEntityForDeletion);

        ResponseEntity<String> result = mealService.deleteMealsByDayId(ID1);

        assertEquals(responseEntityForDeletion, result);
        Mockito.verify(mealDao, Mockito.times(1)).deleteMealsByDayId(ID1);
    }

    @Test
    void testDeleteMealsByDayId_WhenNotExists() {
        Mockito.when(dayDao.existsById(ID1)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> mealService.deleteMealsByDayId(ID1));
        Mockito.verify(mealDao, Mockito.times(0)).deleteMealsByDayId(ID1);
    }

    @Test
    void testDeleteMealByDayIdAndMealId_WhenAllExist() {
        Mockito.when(dayDao.existsById(ID1)).thenReturn(true);
        Mockito.when(mealDao.existsById(ID2)).thenReturn(true);
        Mockito.when(mealDao.deleteMealsByDayIdAndMealId(ID1, ID2)).thenReturn(responseEntityForDeletion);

        ResponseEntity<String> result = mealService.deleteMealByDayIdAndMealId(ID1, ID2);

        assertEquals(responseEntityForDeletion, result);
        Mockito.verify(mealDao, Mockito.times(1)).deleteMealsByDayIdAndMealId(ID1, ID2);
    }

    @Test
    void testDeleteMealByDayIdAndMealId_WhenDayNotExists() {
        Mockito.when(dayDao.existsById(ID1)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> mealService.deleteMealByDayIdAndMealId(ID1, ID2));
        Mockito.verify(mealDao, Mockito.times(0)).deleteMealsByDayIdAndMealId(ID1, ID2);
    }

    @Test
    void testDeleteMealByDayIdAndMealId_WhenMealNotExists() {
        Mockito.when(dayDao.existsById(ID1)).thenReturn(true);
        Mockito.when(mealDao.existsById(ID2)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> mealService.deleteMealByDayIdAndMealId(ID1, ID2));
        Mockito.verify(mealDao, Mockito.times(0)).deleteMealsByDayIdAndMealId(ID1, ID2);
    }

    @Test
    void testUpdateMealById_WhenExists() throws JsonPatchException, JsonProcessingException {
        Mockito.when(mealDao.existsById(ID1)).thenReturn(true);
        Mockito.when(mealDao.getMealById(ID1)).thenReturn(meal1);
        Mockito.when(mealDao.updateMeal(ID1, updatedMeal)).thenReturn(updatedMeal);

        ResponseEntity<MealDto> result = mealService.updateMealById(jsonPatch, ID1);

        assertEquals(new ResponseEntity<>(MealDto.toDto(updatedMeal), HttpStatus.OK), result);
        Mockito.verify(mealDao, Mockito.times(1)).updateMeal(ID1, updatedMeal);
    }

    @Test
    void testUpdateMealById_WhenNotExists() {
        Mockito.when(mealDao.existsById(ID1)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> mealService.updateMealById(jsonPatch, ID1));
        Mockito.verify(mealDao, Mockito.times(0)).updateMeal(ID1, updatedMeal);
    }

    @Test
    void testUpdateMealById_WhenJsonContainsRestrictedFields() {
        Mockito.when(mealDao.existsById(ID1)).thenReturn(true);
        Mockito.when(mealDao.getMealById(ID1)).thenReturn(meal1);

        assertThrows(ValidationException.class, () -> mealService.updateMealById(jsonPatchWithError, ID1));
        Mockito.verify(mealDao, Mockito.times(0)).updateMeal(ID1, updatedMeal);
    }

    @Test
    void testGetAllMeals_WhenExist() {
        Mockito.when(mealDao.getAllMeals()).thenReturn(List.of(meal1, meal2));

        List<MealDto> result = mealService.getAllMeals();

        assertEquals(List.of(meal1, meal2).stream().map(MealDto::toDto).toList(), result);
        Mockito.verify(mealDao, Mockito.times(1)).getAllMeals();
    }

    @Test
    void testGetAllMeals_WhenNotExist() {
        Mockito.when(mealDao.getAllMeals()).thenReturn(List.of());

        assertThrows(NotFoundException.class, () -> mealService.getAllMeals());
        Mockito.verify(mealDao, Mockito.times(1)).getAllMeals();
    }

    @Test
    void testGetMealById_WhenExists() {
        Mockito.when(mealDao.existsById(ID1)).thenReturn(true);
        Mockito.when(mealDao.getMealById(ID1)).thenReturn(meal1);

        MealDto result = mealService.getMealById(ID1);

        assertEquals(MealDto.toDto(meal1), result);
        Mockito.verify(mealDao, Mockito.times(1)).getMealById(ID1);
    }

    @Test
    void testGetMealById_WhenNotExists() {
        Mockito.when(mealDao.existsById(ID1)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> mealService.getMealById(ID1));
        Mockito.verify(mealDao, Mockito.times(0)).getMealById(ID1);
    }

    @Test
    void testDeleteMealById_WhenExists() {
        Mockito.when(mealDao.existsById(ID1)).thenReturn(true);
        Mockito.when(mealDao.deleteMealById(ID1)).thenReturn(responseEntityForDeletion);

        ResponseEntity<String> result = mealService.deleteMealById(ID1);

        assertEquals(responseEntityForDeletion, result);
        Mockito.verify(mealDao, Mockito.times(1)).deleteMealById(ID1);
    }

    @Test
    void testDeleteMealById_WhenNotExists() {
        Mockito.when(mealDao.existsById(ID1)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> mealService.deleteMealById(ID1));
        Mockito.verify(mealDao, Mockito.times(0)).deleteMealById(ID1);
    }

    @Test
    void testGetMealsByProductName_WhenExist() {
        Mockito.when(mealDao.getMealsByProductName(PRODUCT_NAME)).thenReturn(List.of(meal1, meal2));

        List<MealDto> result = mealService.getMealsByProductName(PRODUCT_NAME);

        assertEquals(List.of(meal1, meal2).stream().map(MealDto::toDto).toList(), result);
        Mockito.verify(mealDao, Mockito.times(1)).getMealsByProductName(PRODUCT_NAME);
    }

    @Test
    void testGetMealsByProductName_WhenNotExist() {
        Mockito.when(mealDao.getMealsByProductName(PRODUCT_NAME)).thenReturn(List.of());

        assertThrows(NotFoundException.class, () -> mealService.getMealsByProductName(PRODUCT_NAME));
        Mockito.verify(mealDao, Mockito.times(1)).getMealsByProductName(PRODUCT_NAME);
    }
}
