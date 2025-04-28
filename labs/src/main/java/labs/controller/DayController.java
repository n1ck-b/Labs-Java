package labs.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;
import labs.aspect.CountVisits;
import labs.aspect.LogExecution;
import labs.dto.DayDto;
import labs.dto.ListOfDtoMeals;
import labs.dto.MealDto;
import labs.service.DayService;
import labs.service.MealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CountVisits
@Validated
@RestController
@LogExecution
@RequestMapping("/days")
@Tag(name = "Day controller", description = "API for CRUD operations with days")
public class DayController {
    private final DayService dayService;
    private final MealService mealService;

    @Autowired
    public DayController(DayService dayService, MealService mealService) {
        this.dayService = dayService;
        this.mealService = mealService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get day by ID",
            description = "Returns day, which has specified ID, with all embedded data")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Day was successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter"),
            @ApiResponse(responseCode = "404", description = "Day with specified ID wasn't found")
    })
    public DayDto getDayById(@PathVariable @Positive @Parameter(description = "ID of required day") int id) {
        return dayService.getDayById(id);
    }

    @PostMapping
    @Operation(summary = "Create new day")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Day was successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
    public int addDay(@RequestBody @Parameter(description = "Day object") DayDto day) {
        return dayService.addDay(day);
    }

    @GetMapping
    @Operation(summary = "Get days",
            description = "Returns all days if parameter 'date' is ignored or returns day by required date")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Days were successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter"),
            @ApiResponse(responseCode = "404", description = "Day with specified date wasn't found")
    })
    public List<DayDto> getDays(@RequestParam(name = "date", required = false)
            @Parameter(description = "Optional path parameter, the date in ISO format to get the day by",
                    example = "2025-04-01") LocalDate date) {
        if (date == null) {
            return dayService.getAllDays();
        } else {
            return dayService.getDayByDate(date);
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete day by ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Day was successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter"),
            @ApiResponse(responseCode = "404", description = "Day with specified ID wasn't found")
    })
    public ResponseEntity<String> deleteDayById(@PathVariable @Positive
                @Parameter(description = "ID to delete the day by") int id) {
        return dayService.deleteDayById(id);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update day by ID", description = "Update existing day")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Day was successfully updated"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter or request body"),
            @ApiResponse(responseCode = "404", description = "Day with specified ID wasn't found")
    })
    public ResponseEntity<DayDto> updateDayById(@PathVariable @Positive
            @Parameter(description = "ID of the day being updated") int id, @RequestBody JsonPatch json)
            throws JsonPatchException, JsonProcessingException {
        return dayService.updateDayById(id, json);
    }

    @GetMapping("/{dayId}/meals")
    @Operation(summary = "Get all meals by day ID",
            description = "Returns all meals, that belong to specified day")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Meals were successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter"),
            @ApiResponse(responseCode = "404", description = "Meals were not found for specified day")
    })
    public List<MealDto> getMealsByDayId(@PathVariable @Positive
            @Parameter(description = "ID of the day to get all meals by") int dayId) {
        return mealService.getMealsByDayId(dayId);
    }

    @PostMapping("/{dayId}/meals")
    @Operation(summary = "Create meal", description = "Creates new meal for specified day")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Meal was successfully created"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Day with specified ID wasn't found")
    })
    public int addMeal(@PathVariable @Positive
                       @Parameter(description = "ID of the day to add a meal to") int dayId,
                       @RequestBody MealDto mealDto) {
        return mealService.addMeal(dayId, mealDto);
    }

    @DeleteMapping("/{dayId}/meals")
    @Operation(summary = "Delete meals by day ID",
            description = "Deletes all meals, that belong to specified day. " +
                    "If parameter 'mealId' is specified, than deletes only this meal")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Meals were successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Invalid parameter"),
            @ApiResponse(responseCode = "404", description = "Day with specified ID wasn't found")
    })
    public ResponseEntity<String> deleteMealsByDayId(@PathVariable @Positive
            @Parameter(description = "ID of the day to delete meals/meal by") int dayId,
            @RequestParam(name = "mealId", required = false)
            @Parameter(description = "Optional path parameter, ID of the meal. " +
                    "If it's specified, than only this meal will be deleted") Integer mealId) {
        if (mealId == null) {
            return mealService.deleteMealsByDayId(dayId);
        }
        return mealService.deleteMealByDayIdAndMealId(dayId, mealId);
    }

    @Operation(summary = "Add list of meals by day ID",
            description = "Adds list of meals to specified day")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Meals were successfully added"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "Day with specified ID wasn't found")
    })
    @PostMapping("/{dayId}/meals/bulk")
    public List<MealDto> addListOfMeals(@PathVariable @Positive
            @Parameter(description = "ID of the day to add list of meals to")
            int dayId, @Valid @RequestBody ListOfDtoMeals meals) {
        return meals.getMeals().stream()
                .map(meal -> mealService.addMeal(dayId, meal))
                .toList()
                .stream()
                .map(mealService::getMealById)
                .toList();
    }
}
