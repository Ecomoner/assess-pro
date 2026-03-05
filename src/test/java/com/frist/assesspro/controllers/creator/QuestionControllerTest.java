package com.frist.assesspro.controllers.creator;

import com.frist.assesspro.dto.QuestionDTO;
import com.frist.assesspro.entity.Question;
import com.frist.assesspro.entity.Test;
import com.frist.assesspro.entity.User;
import com.frist.assesspro.service.QuestionService;
import com.frist.assesspro.service.TestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Import(com.frist.assesspro.controllers.GlobalControllerAdvice.class)
@WithMockUser(roles = "CREATOR")
@DisplayName("Тесты контроллера вопросов (QuestionController)")
class QuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuestionService questionService;

    @MockitoBean
    private TestService testService;

    private final Long TEST_ID = 100L;
    private final Long QUESTION_ID = 1L;
    private final String TEST_TITLE = "Sample Test";
    private final String QUESTION_TEXT = "Sample question";
    private final String CREATOR_USERNAME = "user";

    private Test test;
    private User creator;

    @BeforeEach
    void setUp() {
        creator = new User();
        creator.setUsername(CREATOR_USERNAME);

        test = new Test();
        test.setId(TEST_ID);
        test.setTitle(TEST_TITLE);
        test.setCreatedBy(creator);
    }

    // ---------- GET /creator/tests/{testId}/questions ----------

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{testId}/questions: должен вернуть view со списком вопросов")
    void manageQuestions_Success_ShouldReturnView() throws Exception {
        Question question = createQuestion(QUESTION_ID, QUESTION_TEXT, 0);
        List<Question> questions = List.of(question);

        when(questionService.getQuestionsByTestId(eq(TEST_ID), eq(CREATOR_USERNAME))).thenReturn(questions);
        when(questionService.getTestById(eq(TEST_ID), eq(CREATOR_USERNAME))).thenReturn(test);

        mockMvc.perform(get("/creator/tests/{testId}/questions", TEST_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("creator/question-list"))
                .andExpect(model().attribute("test", test))
                .andExpect(model().attribute("questions", questions))
                .andExpect(model().attributeExists("questionDTOs"));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{testId}/questions: при ошибке редирект на список тестов")
    void manageQuestions_Error_ShouldRedirect() throws Exception {
        when(questionService.getQuestionsByTestId(eq(TEST_ID), eq(CREATOR_USERNAME)))
                .thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/creator/tests/{testId}/questions", TEST_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests?error=Error"));
    }

    // ---------- GET /creator/tests/{testId}/questions/new ----------

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{testId}/questions/new: должен вернуть форму создания")
    void showCreateQuestionForm_Success_ShouldReturnView() throws Exception {
        when(questionService.getTestById(eq(TEST_ID), eq(CREATOR_USERNAME))).thenReturn(test);

        mockMvc.perform(get("/creator/tests/{testId}/questions/new", TEST_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("creator/question-form"))
                .andExpect(model().attribute("test", test))
                .andExpect(model().attributeExists("questionDTO"))
                .andExpect(model().attribute("action", "create"))
                .andExpect(model().attribute("formAction", "/creator/tests/" + TEST_ID + "/questions/new"));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{testId}/questions/new: созданный DTO должен иметь два пустых ответа")
    void showCreateQuestionForm_ShouldInitializeWithTwoEmptyAnswers() throws Exception {
        when(questionService.getTestById(eq(TEST_ID), eq(CREATOR_USERNAME))).thenReturn(test);

        mockMvc.perform(get("/creator/tests/{testId}/questions/new", TEST_ID))
                .andExpect(status().isOk())
                .andExpect(model().attribute("questionDTO",
                        org.hamcrest.Matchers.hasProperty("answerOptions",
                                org.hamcrest.Matchers.hasSize(2))));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{testId}/questions/new: при ошибке редирект на список вопросов")
    void showCreateQuestionForm_Error_ShouldRedirect() throws Exception {
        when(questionService.getTestById(eq(TEST_ID), eq(CREATOR_USERNAME)))
                .thenThrow(new RuntimeException("Test not found"));

        mockMvc.perform(get("/creator/tests/{testId}/questions/new", TEST_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests/" + TEST_ID + "/questions?error=Test not found"));
    }

    // ---------- POST /creator/tests/{testId}/questions/new ----------

    @org.junit.jupiter.api.Test
    @DisplayName("POST /creator/tests/{testId}/questions/new: успешное создание вопроса, редирект на список")
    void createQuestion_Success_ShouldRedirect() throws Exception {
        Question savedQuestion = createQuestion(QUESTION_ID, QUESTION_TEXT, 0);
        when(questionService.createQuestion(eq(TEST_ID), any(QuestionDTO.class), eq(CREATOR_USERNAME)))
                .thenReturn(savedQuestion);

        mockMvc.perform(post("/creator/tests/{testId}/questions/new", TEST_ID)
                        .param("text", QUESTION_TEXT)
                        .param("orderIndex", "0")
                        .param("answerOptions[0].text", "Answer 1")
                        .param("answerOptions[0].isCorrect", "true")
                        .param("answerOptions[1].text", "Answer 2")
                        .param("answerOptions[1].isCorrect", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests/" + TEST_ID + "/questions"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("POST /creator/tests/{testId}/questions/new: пустой текст вопроса -> ошибка")
    void createQuestion_EmptyText_ShouldRedirectWithError() throws Exception {
        mockMvc.perform(post("/creator/tests/{testId}/questions/new", TEST_ID)
                        .param("text", "")
                        .param("orderIndex", "0")
                        .param("answerOptions[0].text", "Answer 1")
                        .param("answerOptions[0].isCorrect", "true")
                        .param("answerOptions[1].text", "Answer 2")
                        .param("answerOptions[1].isCorrect", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests/" + TEST_ID + "/questions/new"))
                .andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attributeExists("questionDTO"));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("POST /creator/tests/{testId}/questions/new: все варианты пустые -> ошибка")
    void createQuestion_AllAnswersEmpty_ShouldRedirectWithError() throws Exception {
        mockMvc.perform(post("/creator/tests/{testId}/questions/new", TEST_ID)
                        .param("text", QUESTION_TEXT)
                        .param("orderIndex", "0")
                        .param("answerOptions[0].text", "")
                        .param("answerOptions[0].isCorrect", "true")
                        .param("answerOptions[1].text", "")
                        .param("answerOptions[1].isCorrect", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests/" + TEST_ID + "/questions/new"))
                .andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attributeExists("questionDTO"));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("POST /creator/tests/{testId}/questions/new: нет правильного ответа -> ошибка (сервис)")
    void createQuestion_NoCorrectAnswer_ShouldRedirectWithError() throws Exception {
        when(questionService.createQuestion(eq(TEST_ID), any(QuestionDTO.class), eq(CREATOR_USERNAME)))
                .thenThrow(new IllegalArgumentException("Добавьте хотя бы один правильный ответ"));

        mockMvc.perform(post("/creator/tests/{testId}/questions/new", TEST_ID)
                        .param("text", QUESTION_TEXT)
                        .param("orderIndex", "0")
                        .param("answerOptions[0].text", "Answer 1")
                        .param("answerOptions[0].isCorrect", "false")
                        .param("answerOptions[1].text", "Answer 2")
                        .param("answerOptions[1].isCorrect", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests/" + TEST_ID + "/questions/new"))
                .andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attributeExists("questionDTO"));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("POST /creator/tests/{testId}/questions/new: исключение сервиса -> ошибка")
    void createQuestion_ServiceException_ShouldRedirectWithError() throws Exception {
        when(questionService.createQuestion(eq(TEST_ID), any(QuestionDTO.class), eq(CREATOR_USERNAME)))
                .thenThrow(new RuntimeException("Creation failed"));

        mockMvc.perform(post("/creator/tests/{testId}/questions/new", TEST_ID)
                        .param("text", QUESTION_TEXT)
                        .param("orderIndex", "0")
                        .param("answerOptions[0].text", "Answer 1")
                        .param("answerOptions[0].isCorrect", "true")
                        .param("answerOptions[1].text", "Answer 2")
                        .param("answerOptions[1].isCorrect", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests/" + TEST_ID + "/questions/new"))
                .andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attributeExists("questionDTO"));
    }

    // ---------- GET /creator/tests/{testId}/questions/{questionId}/edit ----------

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{testId}/questions/{questionId}/edit: успешный просмотр формы редактирования")
    void showEditQuestionForm_Success_ShouldReturnView() throws Exception {
        QuestionDTO questionDTO = new QuestionDTO();
        questionDTO.setId(QUESTION_ID);
        questionDTO.setText(QUESTION_TEXT);
        questionDTO.setOrderIndex(0);
        questionDTO.setAnswerOptions(new ArrayList<>());

        when(questionService.getTestById(eq(TEST_ID), eq(CREATOR_USERNAME))).thenReturn(test);
        when(questionService.getQuestionDTO(eq(QUESTION_ID), eq(CREATOR_USERNAME))).thenReturn(questionDTO);

        mockMvc.perform(get("/creator/tests/{testId}/questions/{questionId}/edit", TEST_ID, QUESTION_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("creator/question-form"))
                .andExpect(model().attribute("test", test))
                .andExpect(model().attribute("questionDTO", questionDTO))
                .andExpect(model().attribute("action", "edit"))
                .andExpect(model().attribute("formAction", "/creator/tests/" + TEST_ID + "/questions/" + QUESTION_ID + "/update"));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("GET /creator/tests/{testId}/questions/{questionId}/edit: при ошибке редирект на список вопросов")
    void showEditQuestionForm_Error_ShouldRedirect() throws Exception {
        when(questionService.getTestById(eq(TEST_ID), eq(CREATOR_USERNAME)))
                .thenThrow(new RuntimeException("Test not found"));

        mockMvc.perform(get("/creator/tests/{testId}/questions/{questionId}/edit", TEST_ID, QUESTION_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests/" + TEST_ID + "/questions?error=Test not found"));
    }

    // ---------- POST /creator/tests/{testId}/questions/{questionId}/update ----------

    @org.junit.jupiter.api.Test
    @DisplayName("POST /creator/tests/{testId}/questions/{questionId}/update: успешное обновление, редирект на список")
    void updateQuestion_Success_ShouldRedirect() throws Exception {
        Question updatedQuestion = createQuestion(QUESTION_ID, "Updated text", 1);
        when(questionService.updateQuestion(eq(QUESTION_ID), any(QuestionDTO.class), eq(CREATOR_USERNAME)))
                .thenReturn(updatedQuestion);

        mockMvc.perform(post("/creator/tests/{testId}/questions/{questionId}/update", TEST_ID, QUESTION_ID)
                        .param("text", "Updated text")
                        .param("orderIndex", "1")
                        .param("answerOptions[0].text", "Answer 1")
                        .param("answerOptions[0].isCorrect", "true")
                        .param("answerOptions[1].text", "Answer 2")
                        .param("answerOptions[1].isCorrect", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests/" + TEST_ID + "/questions"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("POST /creator/tests/{testId}/questions/{questionId}/update: ошибка валидации, редирект с ошибкой")
    void updateQuestion_ValidationError_ShouldRedirectWithError() throws Exception {
        mockMvc.perform(post("/creator/tests/{testId}/questions/{questionId}/update", TEST_ID, QUESTION_ID)
                        .param("text", "")
                        .param("orderIndex", "1")
                        .param("answerOptions[0].text", "Answer 1")
                        .param("answerOptions[0].isCorrect", "true")
                        .param("answerOptions[1].text", "Answer 2")
                        .param("answerOptions[1].isCorrect", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests/" + TEST_ID + "/questions/" + QUESTION_ID + "/edit"))
                .andExpect(flash().attributeExists("org.springframework.validation.BindingResult.questionDTO"))
                .andExpect(flash().attributeExists("questionDTO"));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("POST /creator/tests/{testId}/questions/{questionId}/update: все варианты пустые -> ошибка")
    void updateQuestion_AllAnswersEmpty_ShouldRedirectWithError() throws Exception {
        mockMvc.perform(post("/creator/tests/{testId}/questions/{questionId}/update", TEST_ID, QUESTION_ID)
                        .param("text", "Updated text")
                        .param("orderIndex", "1")
                        .param("answerOptions[0].text", "")
                        .param("answerOptions[0].isCorrect", "true")
                        .param("answerOptions[1].text", "")
                        .param("answerOptions[1].isCorrect", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests/" + TEST_ID + "/questions/" + QUESTION_ID + "/edit"))
                .andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attributeExists("questionDTO"));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("POST /creator/tests/{testId}/questions/{questionId}/update: исключение сервиса -> ошибка")
    void updateQuestion_ServiceException_ShouldRedirectWithError() throws Exception {
        doThrow(new RuntimeException("Update failed"))
                .when(questionService).updateQuestion(eq(QUESTION_ID), any(QuestionDTO.class), eq(CREATOR_USERNAME));

        mockMvc.perform(post("/creator/tests/{testId}/questions/{questionId}/update", TEST_ID, QUESTION_ID)
                        .param("text", "Updated text")
                        .param("orderIndex", "1")
                        .param("answerOptions[0].text", "Answer 1")
                        .param("answerOptions[0].isCorrect", "true")
                        .param("answerOptions[1].text", "Answer 2")
                        .param("answerOptions[1].isCorrect", "false"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests/" + TEST_ID + "/questions/" + QUESTION_ID + "/edit"))
                .andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attributeExists("questionDTO"));
    }

    // ---------- POST /creator/tests/{testId}/questions/{questionId}/delete ----------

    @org.junit.jupiter.api.Test
    @DisplayName("POST /creator/tests/{testId}/questions/{questionId}/delete: успешное удаление, редирект с сообщением")
    void deleteQuestion_Success_ShouldRedirect() throws Exception {
        doNothing().when(questionService).deleteQuestion(eq(QUESTION_ID), eq(CREATOR_USERNAME));

        mockMvc.perform(post("/creator/tests/{testId}/questions/{questionId}/delete", TEST_ID, QUESTION_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests/" + TEST_ID + "/questions"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @org.junit.jupiter.api.Test
    @DisplayName("POST /creator/tests/{testId}/questions/{questionId}/delete: исключение сервиса -> ошибка")
    void deleteQuestion_ServiceException_ShouldRedirectWithError() throws Exception {
        doThrow(new RuntimeException("Delete failed"))
                .when(questionService).deleteQuestion(eq(QUESTION_ID), eq(CREATOR_USERNAME));

        mockMvc.perform(post("/creator/tests/{testId}/questions/{questionId}/delete", TEST_ID, QUESTION_ID))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/creator/tests/" + TEST_ID + "/questions"))
                .andExpect(flash().attributeExists("errorMessage"));
    }

    // ---------- Вспомогательные методы ----------

    private Question createQuestion(Long id, String text, int orderIndex) {
        Question q = new Question();
        q.setId(id);
        q.setText(text);
        q.setOrderIndex(orderIndex);
        q.setTest(test);
        return q;
    }
}