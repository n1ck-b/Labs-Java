package labs.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import labs.aspect.LogExecution;
import labs.dao.DayDao;
import labs.dto.DayDto;
import labs.exception.ExceptionMessages;
import labs.exception.NotFoundException;
import labs.exception.ValidationException;
import labs.model.Day;
import labs.service.DayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@LogExecution
@Validated
public class DayServiceImpl implements DayService {
    private final DayDao dayDao;

    @Autowired
    public DayServiceImpl(DayDao dayDao) {
        this.dayDao = dayDao;
    }

    @Override
    public DayDto getDayById(int id) {
        if (!dayDao.existsById(id)) {
            throw new NotFoundException(String.format(ExceptionMessages.DAY_NOT_FOUND, id));
        }
        Day day = dayDao.getDayById(id);
        return DayDto.toDto(day);
    }

    @Override
    public DayDto addDay(@Valid DayDto day) {
        Day dayFromDb;
        try {
            dayFromDb = getDayByDate(day.getDate()).get(0).fromDto();
        } catch (NotFoundException ex) {
            return DayDto.toDto(dayDao.addDay(day.fromDto()));
        }
        return DayDto.toDto(dayFromDb);
    }

    @Override
    public List<DayDto> getAllDays() {
        List<Day> days = dayDao.getAllDays();
        if (days.isEmpty()) {
            throw new NotFoundException(ExceptionMessages.DAYS_NOT_FOUND);
        }
        return days.stream().map(DayDto::toDto).toList();
    }

    @Override
    public ResponseEntity<String> deleteDayById(int id) {
        if (!dayDao.existsById(id)) {
            throw new NotFoundException(String.format(ExceptionMessages.DAY_NOT_FOUND, id));
        }
        return dayDao.deleteDayById(id);
    }

    @Override
    public ResponseEntity<DayDto> updateDayById(int id, JsonPatch json)
            throws JsonProcessingException, JsonPatchException {
        if (!dayDao.existsById(id)) {
            throw new NotFoundException(String.format(ExceptionMessages.DAY_NOT_FOUND, id));
        }
        if (json.toString().contains("id") || json.toString().contains("meals")) {
            throw new ValidationException(String.format(ExceptionMessages.PATCH_VALIDATION_EXCEPTION,
                    "'id' and 'meals'"));
        }
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        Day day = dayDao.getDayById(id);
        JsonNode node;
        node = json.apply(objectMapper.convertValue(day, JsonNode.class));
        day = objectMapper.treeToValue(node, Day.class);
        Day updatedDay = dayDao.updateDayById(id, day);
        return ResponseEntity.ok(DayDto.toDto(updatedDay));
    }

    @Override
    public List<DayDto> getDayByDate(@NotNull LocalDate date) {
        List<Day> days = dayDao.getDayByDate(date);
        if (days.isEmpty()) {
            throw new NotFoundException(ExceptionMessages.DAYS_NOT_FOUND_BY_DATE);
        }
        return days.stream().map(DayDto::toDto).toList();
    }
}
