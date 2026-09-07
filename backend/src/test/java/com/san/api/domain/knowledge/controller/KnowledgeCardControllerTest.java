package com.san.api.domain.knowledge.controller;

import com.san.api.domain.knowledge.dto.response.KnowledgeCardDetailResponse;
import com.san.api.domain.knowledge.service.KnowledgeCardService;
import com.san.api.domain.scrap.entity.SourceType;
import com.san.api.global.security.filter.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(KnowledgeCardController.class)
@AutoConfigureMockMvc(addFilters = false)
class KnowledgeCardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private KnowledgeCardService knowledgeCardService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void updateRefinedContent_returnsUpdatedCardDetail() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        KnowledgeCardDetailResponse response = new KnowledgeCardDetailResponse(
                "Knowledge card title",
                categoryId,
                "Backend",
                SourceType.TEXT,
                "raw content",
                "updated refined content",
                "summary",
                List.of("Spring"),
                LocalDateTime.of(2026, 5, 15, 10, 30)
        );

        when(knowledgeCardService.updateRefinedContent(eq(userId), eq(cardId), any()))
                .thenReturn(response);

        mockMvc.perform(patch("/cards/{cardId}/refined-content", cardId)
                        .principal(new TestingAuthenticationToken(userId.toString(), null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refinedContent\":\"updated refined content\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.title").value("Knowledge card title"))
                .andExpect(jsonPath("$.data.refinedContent").value("updated refined content"))
                .andExpect(jsonPath("$.data.tags[0]").value("Spring"));
    }
}
