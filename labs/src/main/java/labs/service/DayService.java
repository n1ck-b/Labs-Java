package labs.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import java.time.LocalDate;
import java.util.List;
import labs.dto.DayDto;
import labs.model.Day;
import org.springframework.http.ResponseEntity;

public interface DayService {
    DayDto getDayById(int id);

    int addDay(Day day);

    List<DayDto> getAllDays();

    ResponseEntity<String> deleteDayById(int id);

    ResponseEntity<DayDto> updateDayById(int id, JsonPatch json)
            throws JsonPatchException, JsonProcessingException;

    List<DayDto> getDayByDate(LocalDate date);
}
