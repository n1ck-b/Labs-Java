package service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import labs.dao.DayDao;
import labs.dto.DayDto;
import labs.exception.NotFoundException;
import labs.exception.ValidationException;
import labs.model.Day;
import labs.service.impl.DayServiceImpl;
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
public class DayServiceImplTest {
    private static final int ID = 1;
    private Day day;
    private Day day2;
    private DayDto dayDto;
    private ResponseEntity<String> responseEntityForDeletion;
    private JsonPatch jsonPatch;
    private JsonPatch jsonPatchWithError;
    private Day updatedDay;

    @Mock
    private DayDao dayDao;

    @InjectMocks
    private DayServiceImpl dayService;

    @BeforeEach
    void setUp() throws IOException {
        day = new Day(ID, LocalDate.of(2025, 4, 15), List.of());
        dayDto = new DayDto(ID, LocalDate.of(2025, 4, 15), List.of());
        day2 = new Day(2, LocalDate.of(2024, 6, 4), List.of());
        responseEntityForDeletion = new ResponseEntity<>("Deleted successfully", HttpStatus.OK);
        updatedDay = new Day(ID, LocalDate.of(2024, 6, 4), List.of());

        String patch = """
                [
                    {
                        "op": "replace",
                        "path": "/date",
                        "value": "2024-06-04"
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
    void testGetDayById_Successfully() {
        Mockito.when(dayDao.getDayById(ID)).thenReturn(day);
        Mockito.when(dayDao.existsById(ID)).thenReturn(true);

        Day result = dayService.getDayById(ID).fromDto();

        assertNotNull(result);
        assertEquals(day, result);
    }

    @Test
    void testGetDayById_NotFound() {
        Mockito.when(dayDao.existsById(ID)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> dayService.getDayById(ID));
        Mockito.verify(dayDao, Mockito.times(0)).getDayById(ID);
    }

    @Test
    void testAddDay_WhenExists() {
        Mockito.when(dayDao.getDayByDate(dayDto.getDate())).thenReturn(List.of(day));

        int result = dayService.addDay(dayDto);

        assertEquals(ID, result);
        Mockito.verify(dayDao, Mockito.times(0)).addDay(dayDto.fromDto());
    }

    @Test
    void testAddDay_WhenNotExists() {
        Mockito.when(dayDao.getDayByDate(dayDto.getDate())).thenReturn(List.of());
        Mockito.when(dayDao.addDay(dayDto.fromDto())).thenReturn(ID);

        int result = dayService.addDay(dayDto);

        assertEquals(ID, result);
        Mockito.verify(dayDao, Mockito.times(1)).addDay(dayDto.fromDto());
    }

    @Test
    void testGetAllDays_WhenExist() {
        Mockito.when(dayDao.getAllDays()).thenReturn(List.of(day, day2));

        List<DayDto> result = dayService.getAllDays();

        assertEquals(List.of(day, day2), result.stream().map(DayDto::fromDto).toList());
    }

    @Test
    void testGetAllDays_WhenNotExist() {
        Mockito.when(dayDao.getAllDays()).thenReturn(List.of());

        assertThrows(NotFoundException.class, () -> dayService.getAllDays());
    }

    @Test
    void testDeleteDayById_WhenExists() {
        Mockito.when(dayDao.existsById(ID)).thenReturn(true);
        Mockito.when(dayDao.deleteDayById(ID)).thenReturn(responseEntityForDeletion);

        ResponseEntity<String> result = dayService.deleteDayById(ID);

        assertEquals(responseEntityForDeletion, result);
    }

    @Test
    void testDeleteDayById_WhenNotExists() {
        Mockito.when(dayDao.existsById(ID)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> dayService.deleteDayById(ID));
        Mockito.verify(dayDao, Mockito.times(0)).deleteDayById(ID);
    }

    @Test
    void testUpdateDayById_WhenExists() throws IOException, JsonPatchException {
        Mockito.when(dayDao.existsById(ID)).thenReturn(true);
        Mockito.when(dayDao.getDayById(ID)).thenReturn(day);
        Mockito.when(dayDao.updateDayById(ID, updatedDay)).thenReturn(updatedDay);

        ResponseEntity<DayDto> result = dayService.updateDayById(ID, jsonPatch);

        assertEquals(new ResponseEntity<>(DayDto.toDto(updatedDay), HttpStatus.OK), result);
        Mockito.verify(dayDao, Mockito.times(1)).updateDayById(ID, updatedDay);
    }

    @Test
    void testUpdateDayById_WhenNotExists() {
        Mockito.when(dayDao.existsById(ID)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> dayService.updateDayById(ID, jsonPatch));
        Mockito.verify(dayDao, Mockito.times(0)).updateDayById(ID, updatedDay);
    }

    @Test
    void testUpdateDayById_WhenJsonContainsRestrictedFields() {
        Mockito.when(dayDao.existsById(ID)).thenReturn(true);

        assertThrows(ValidationException.class, () -> dayService.updateDayById(ID, jsonPatchWithError));
        Mockito.verify(dayDao, Mockito.times(0)).updateDayById(ID, updatedDay);
    }

    @Test
    void testGetDayByDate_WhenExists() {
        Mockito.when(dayDao.getDayByDate(dayDto.getDate())).thenReturn(List.of(day));

        List<DayDto> result = dayService.getDayByDate(dayDto.getDate());

        assertEquals(List.of(day).stream().map(DayDto::toDto).toList(), result);
    }

    @Test
    void testGetDayByDate_WhenNotExists() {
        Mockito.when(dayDao.getDayByDate(dayDto.getDate())).thenReturn(List.of());

        assertThrows(NotFoundException.class, () -> dayService.getDayByDate(dayDto.getDate()));
    }
}
