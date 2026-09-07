package com.san.api.domain.recall.controller;

import com.san.api.domain.recall.dto.response.RecallQuizGenerationJobResponse;
import com.san.api.domain.recall.dto.response.RecallQuizListResponse;
import com.san.api.domain.recall.dto.response.RecallQuizResponse;
import com.san.api.domain.recall.dto.response.RecallQuizSubmitResponse;
import com.san.api.domain.recall.entity.RecallQuizType;
import com.san.api.domain.recall.service.RecallQuizGenerationService;
import com.san.api.domain.recall.service.RecallQuizSubmissionService;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.security.filter.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecallController.class)
@AutoConfigureMockMvc(addFilters = false)
class RecallControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecallQuizGenerationService recallQuizGenerationService;

    @MockitoBean
    private RecallQuizSubmissionService recallQuizSubmissionService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void generateReturnsRecallQuizGenerationJob() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID generationId = UUID.randomUUID();
        UUID quizJobId = UUID.randomUUID();
        LocalDate targetDate = LocalDate.of(2026, 5, 19);
        RecallQuizGenerationJobResponse response = new RecallQuizGenerationJobResponse(
                generationId,
                quizJobId,
                targetDate,
                RecallQuizType.OX
        );

        when(recallQuizGenerationService.requestGeneration(eq(userId), any()))
                .thenReturn(response);

        mockMvc.perform(post("/recall/quizzes")
                        .principal(new TestingAuthenticationToken(userId.toString(), null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "targetDate": "2026-05-19",
                                  "quizType": "OX"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.generationId").value(generationId.toString()))
                .andExpect(jsonPath("$.data.quizJobId").value(quizJobId.toString()))
                .andExpect(jsonPath("$.data.targetDate").value("2026-05-19"))
                .andExpect(jsonPath("$.data.quizType").value("OX"));

        verify(recallQuizGenerationService).requestGeneration(eq(userId), any());
    }

    @Test
    void generateRejectsInvalidRequest() throws Exception {
        mockMvc.perform(post("/recall/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void getQuizzesReturnsRecallQuizzes() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID scrapId = UUID.randomUUID();
        LocalDate targetDate = LocalDate.of(2026, 5, 19);
        String question = "React.memo should not be used for every component.";
        RecallQuizListResponse response = new RecallQuizListResponse(
                targetDate,
                RecallQuizType.OX,
                List.of(new RecallQuizResponse(
                        quizId,
                        scrapId,
                        RecallQuizType.OX,
                        question,
                        false,
                        null,
                        null,
                        null
                ))
        );

        when(recallQuizGenerationService.getQuizzes(userId, targetDate, RecallQuizType.OX))
                .thenReturn(response);

        mockMvc.perform(get("/recall/quizzes")
                        .principal(new TestingAuthenticationToken(userId.toString(), null))
                        .param("targetDate", "2026-05-19")
                        .param("quizType", "OX"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.targetDate").value("2026-05-19"))
                .andExpect(jsonPath("$.data.quizType").value("OX"))
                .andExpect(jsonPath("$.data.quizzes[0].quizId").value(quizId.toString()))
                .andExpect(jsonPath("$.data.quizzes[0].scrapId").value(scrapId.toString()))
                .andExpect(jsonPath("$.data.quizzes[0].question").value(question))
                .andExpect(jsonPath("$.data.quizzes[0].solved").value(false));

        verify(recallQuizGenerationService).getQuizzes(userId, targetDate, RecallQuizType.OX);
    }

    @Test
    void submitReturnsSubmittedQuiz() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        RecallQuizSubmitResponse response = new RecallQuizSubmitResponse(
                quizId,
                RecallQuizType.OX,
                "React.memo는 모든 컴포넌트에 무조건 사용하는 것이 권장된다.",
                true,
                false,
                "X",
                "필요한 경우에만 사용하는 것이 권장됩니다."
        );

        when(recallQuizSubmissionService.submit(eq(userId), eq(quizId), any()))
                .thenReturn(response);

        mockMvc.perform(post("/recall/quizzes/{quizId}/submissions", quizId)
                        .principal(new TestingAuthenticationToken(userId.toString(), null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answer": "X"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.quizId").value(quizId.toString()))
                .andExpect(jsonPath("$.data.quizType").value("OX"))
                .andExpect(jsonPath("$.data.question").value("React.memo는 모든 컴포넌트에 무조건 사용하는 것이 권장된다."))
                .andExpect(jsonPath("$.data.solved").value(true))
                .andExpect(jsonPath("$.data.correct").value(false))
                .andExpect(jsonPath("$.data.submittedAnswer").value("X"))
                .andExpect(jsonPath("$.data.explanation").value("필요한 경우에만 사용하는 것이 권장됩니다."));

        verify(recallQuizSubmissionService).submit(eq(userId), eq(quizId), any());
    }

    @Test
    void submitRejectsInvalidRequest() throws Exception {
        UUID quizId = UUID.randomUUID();

        mockMvc.perform(post("/recall/quizzes/{quizId}/submissions", quizId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "answer": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ok").value(false));
    }

    @Test
    void generateRejectsMissingAuthentication() {
        RecallController controller = new RecallController(recallQuizGenerationService, recallQuizSubmissionService);

        assertThatThrownBy(() -> controller.generate(null, null))
                .isInstanceOf(BusinessException.class);
    }
}
