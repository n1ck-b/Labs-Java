package labs.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import java.time.LocalDate;
import java.util.List;
import labs.dto.DayDto;
import labs.dto.MealDto;
import labs.service.DayService;
import labs.service.MealService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/days")
public class DayController {
    private final DayService dayService;
    private final MealService mealService;

    @Autowired
    public DayController(DayService dayService, MealService mealService) {
        this.dayService = dayService;
        this.mealService = mealService;
    }

    @GetMapping("/{id}")
    public DayDto getDayById(@PathVariable int id) {
        return dayService.getDayById(id);
    }

    @PostMapping
    public int addDay(@RequestBody DayDto day) {
        if (day.getId() != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return dayService.addDay(day.fromDto());
    }

    @GetMapping
    public List<DayDto> getDays(@RequestParam(name = "date", required = false) LocalDate date) {
        if (date == null) {
            return dayService.getAllDays();
        } else {
            return dayService.getDayByDate(date);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDayById(@PathVariable int id) {
        return dayService.deleteDayById(id);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<DayDto> updateDayById(@PathVariable int id, @RequestBody JsonPatch json)
            throws JsonPatchException, JsonProcessingException {
        return dayService.updateDayById(id, json);
    }

    @GetMapping("/{dayId}/meals")
    public List<MealDto> getMealsByDayId(@PathVariable int dayId) {
        return mealService.getMealsByDayId(dayId);
    }

    @PostMapping("/{dayId}/meals")
    public int addMeal(@PathVariable int dayId, @RequestBody MealDto mealDto) {
        if (mealDto.getId() != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return mealService.addMeal(dayId, mealDto);
    }

    @DeleteMapping("/{dayId}/meals")
    public ResponseEntity<String> deleteMealsByDayId(@PathVariable int dayId, @RequestParam(name = "id",
            required = false) Integer mealId) {
        if (mealId == null) {
            return mealService.deleteMealsByDayId(dayId);
        }
        return mealService.deleteMealsByDayIdAndMealId(dayId, mealId);
    }
}
