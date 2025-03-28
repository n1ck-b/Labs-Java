package labs.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import java.time.LocalDate;
import java.util.List;
import labs.dao.DayDao;
import labs.dto.DayDto;
import labs.model.Day;
import labs.service.DayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DayServiceImpl implements DayService {
    private final DayDao dayDao;

    @Autowired
    public DayServiceImpl(DayDao dayDao) {
        this.dayDao = dayDao;
    }

    @Override
    public DayDto getDayById(int id) {
        Day day = dayDao.getDayById(id);
        return DayDto.toDto(day);
    }

    @Override
    public int addDay(Day day) {
        return dayDao.addDay(day);
    }

    @Override
    public List<DayDto> getAllDays() {
        List<Day> days = dayDao.getAllDays();
        return days.stream().map(DayDto::toDto).toList();
    }

    @Override
    public ResponseEntity<String> deleteDayById(int id) {
        return dayDao.deleteDayById(id);
    }

    @Override
    public ResponseEntity<DayDto> updateDayById(int id, JsonPatch json)
            throws JsonProcessingException {
        if (json.toString().contains("id") | json.toString().contains("meals")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        Day day = dayDao.getDayById(id);
        JsonNode node;
        try {
            node = json.apply(objectMapper.convertValue(day, JsonNode.class));
        } catch (JsonPatchException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        day = objectMapper.treeToValue(node, Day.class);
        Day updatedDay = dayDao.updateDayById(id, day);
        return ResponseEntity.ok(DayDto.toDto(updatedDay));
    }

    @Override
    public List<DayDto> getDayByDate(LocalDate date) {
        List<Day> days = dayDao.getDayByDate(date);
        if (days.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return days.stream().map(DayDto::toDto).toList();
    }
}
