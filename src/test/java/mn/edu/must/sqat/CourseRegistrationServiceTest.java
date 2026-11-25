package mn.edu.must.sqat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * CourseRegistrationService-ийн нэгжийн тестүүд.
 * StudentRepository ба EmailSender-ийг Mockito mock-оор орлуулж, бизнес логикийг тусгаарлаж шалгана.
 * Нэрлэх хэв маяг: юуг_ямарНөхцөлд_юуХүлээгдэнэ (Лекц 8).
 */
@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {

    @Mock StudentRepository repository;
    @Mock EmailSender emailSender;
    CourseRegistrationService service;

    // Тестэд ашиглах өгөгдөл
    static final Student ACTIVE = new Student("B001", "b001@must.edu.mn", true, List.of("CS201"));
    static final Student INACTIVE = new Student("B002", "b002@must.edu.mn", false, List.of("CS201"));

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationService(repository, emailSender);
    }

    @Test
    @DisplayName("A1: идэвхтэй оюутан → OK, хадгалж, и-мэйл илгээнэ")
    void register_activeStudentWithPrereqs_returnsOkSavesAndSendsEmail() {
        // Arrange — stub
        when(repository.findStudent("B001")).thenReturn(ACTIVE);
        when(repository.courseExists("CS313")).thenReturn(true);
        when(repository.prerequisites("CS313")).thenReturn(List.of("CS201"));
        // Act
        String result = service.register("B001", "CS313");
        // Assert — үр дүн ба хамаарлуудтай хийсэн харилцаа
        assertEquals("OK", result);
        verify(repository).saveRegistration("B001", "CS313");
        // Matcher хэрэглэсэн тул бүх аргументад matcher (eq/anyString/contains)
        verify(emailSender).send(eq("b001@must.edu.mn"), anyString(), contains("CS313"));
    }

    @Test
    @DisplayName("A2: байхгүй оюутан → ERROR_NO_STUDENT, хадгалаагүй, и-мэйл яваагүй")
    void register_unknownStudent_returnsNoStudentAndSavesNothing() {
        when(repository.findStudent("B404")).thenReturn(null);

        String result = service.register("B404", "CS313");

        assertEquals("ERROR_NO_STUDENT", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("A3: идэвхгүй оюутан → ERROR_INACTIVE_STUDENT, и-мэйлтэй огт харьцаагүй")
    void register_inactiveStudent_returnsInactiveAndNoEmailInteraction() {
        when(repository.findStudent("B002")).thenReturn(INACTIVE);

        String result = service.register("B002", "CS313");

        assertEquals("ERROR_INACTIVE_STUDENT", result);
        verifyNoInteractions(emailSender);
        verify(repository, never()).saveRegistration(anyString(), anyString());
    }

    @Test
    @DisplayName("A4: урьдач хичээл дутуу → ERROR_PREREQUISITES, хадгалаагүй")
    void register_missingPrerequisite_returnsPrereqErrorAndSavesNothing() {
        when(repository.findStudent("B001")).thenReturn(ACTIVE); // CS201 үзсэн
        when(repository.courseExists("CS402")).thenReturn(true);
        when(repository.prerequisites("CS402")).thenReturn(List.of("CS201", "CS313")); // CS313 дутуу

        String result = service.register("B001", "CS402");

        assertEquals("ERROR_PREREQUISITES", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("A5: и-мэйл exception шидсэн ч → OK, хадгалалт хийгдсэн")
    void register_emailSenderThrows_stillReturnsOkAndSaved() {
        when(repository.findStudent("B001")).thenReturn(ACTIVE);
        when(repository.courseExists("CS313")).thenReturn(true);
        when(repository.prerequisites("CS313")).thenReturn(List.of("CS201"));
        // void метод тул doThrow(...).when(mock).method(...) хэлбэр
        doThrow(new RuntimeException("SMTP down"))
                .when(emailSender).send(anyString(), anyString(), anyString());

        String result = service.register("B001", "CS313");

        assertEquals("OK", result);
        verify(repository).saveRegistration("B001", "CS313");
        verify(emailSender).send(anyString(), anyString(), anyString()); // илгээх оролдлого хийгдсэн
    }

    @Test
    @DisplayName("A6: null оролт → IllegalArgumentException, хамаарлуудтай харьцаагүй")
    void register_nullArgument_throwsAndTouchesNoDependency() {
        assertThrows(IllegalArgumentException.class, () -> service.register(null, "CS313"));

        verifyNoInteractions(repository, emailSender);
    }

    @Test
    @DisplayName("A7: илгээсэн и-мэйлийн хаяг, биед хичээлийн код орсон")
    void register_success_emailBodyContainsCourseCode() {
        when(repository.findStudent("B001")).thenReturn(ACTIVE);
        when(repository.courseExists("CS313")).thenReturn(true);
        when(repository.prerequisites("CS313")).thenReturn(List.of("CS201"));
        ArgumentCaptor<String> to = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);

        service.register("B001", "CS313");

        // Бүх аргументад matcher: captor.capture() мөн matcher-т тооцогдоно
        verify(emailSender).send(to.capture(), anyString(), body.capture());
        assertEquals("b001@must.edu.mn", to.getValue());
        assertTrue(body.getValue().contains("CS313"), "Биед хичээлийн код байх ёстой: " + body.getValue());
    }

    @ParameterizedTest(name = "{0} → {1}")
    @DisplayName("A8: алдааны гурван анги → тохирох ERROR, хадгалаагүй, и-мэйлгүй")
    @CsvSource({
            "NO_STUDENT, ERROR_NO_STUDENT",
            "INACTIVE,   ERROR_INACTIVE_STUDENT",
            "NO_COURSE,  ERROR_NO_COURSE"
    })
    void register_errorClass_returnsMatchingErrorAndSavesNothing(String scenario, String expected) {
        // Мөр бүр өөр замаар гардаг: NO_STUDENT үед courseExists stub огт дуудагдахгүй.
        // Strict stubs нь ашиглагдаагүй stub-ийг UnnecessaryStubbingException болгодог тул lenient().
        Student student = switch (scenario) {
            case "NO_STUDENT" -> null;
            case "INACTIVE" -> INACTIVE;
            default -> ACTIVE;
        };
        lenient().when(repository.findStudent("S1")).thenReturn(student);
        lenient().when(repository.courseExists("C1")).thenReturn(!scenario.equals("NO_COURSE"));

        String result = service.register("S1", "C1");

        assertEquals(expected, result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }
}
