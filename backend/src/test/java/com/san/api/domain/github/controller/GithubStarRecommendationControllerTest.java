package com.san.api.domain.github.controller;

import com.san.api.domain.github.dto.response.GithubStarRecommendationCollectResponse;
import com.san.api.domain.github.dto.response.GithubStarRecommendationJobResponse;
import com.san.api.domain.github.service.GithubStarRecommendationCollectService;
import com.san.api.domain.github.service.GithubStarRecommendationJobService;
import com.san.api.global.security.filter.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GithubStarRecommendationController.class)
@AutoConfigureMockMvc(addFilters = false)
class GithubStarRecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GithubStarRecommendationJobService githubStarRecommendationJobService;

    @MockitoBean
    private GithubStarRecommendationCollectService githubStarRecommendationCollectService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void requestRecommendation_returnsAcceptedWhenJobCreated() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();

        when(githubStarRecommendationJobService.requestRecommendation(userId))
                .thenReturn(GithubStarRecommendationJobResponse.created(jobId));

        mockMvc.perform(post("/github/star-recommendations")
                        .principal(new TestingAuthenticationToken(userId.toString(), null)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.data.alreadyRecommended").value(false));
    }

    @Test
    void requestRecommendation_returnsOkWhenRecommendationsAlreadyExist() throws Exception {
        UUID userId = UUID.randomUUID();
        GithubStarRecommendationJobResponse response = new GithubStarRecommendationJobResponse(
                null,
                true,
                List.of()
        );

        when(githubStarRecommendationJobService.requestRecommendation(userId)).thenReturn(response);

        mockMvc.perform(post("/github/star-recommendations")
                        .principal(new TestingAuthenticationToken(userId.toString(), null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.jobId").doesNotExist())
                .andExpect(jsonPath("$.data.alreadyRecommended").value(true));
    }

    @Test
    void collectRecommendation_returnsCollectedRecommendation() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID recommendationId = UUID.randomUUID();
        UUID scrapId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        GithubStarRecommendationCollectResponse response = new GithubStarRecommendationCollectResponse(
                recommendationId,
                scrapId,
                cardId,
                true
        );

        when(githubStarRecommendationCollectService.collect(userId, recommendationId)).thenReturn(response);

        mockMvc.perform(post("/github/star-recommendations/{recommendationId}/collect", recommendationId)
                        .principal(new TestingAuthenticationToken(userId.toString(), null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.data.recommendationId").value(recommendationId.toString()))
                .andExpect(jsonPath("$.data.scrapId").value(scrapId.toString()))
                .andExpect(jsonPath("$.data.cardId").value(cardId.toString()))
                .andExpect(jsonPath("$.data.collected").value(true));
    }
}
