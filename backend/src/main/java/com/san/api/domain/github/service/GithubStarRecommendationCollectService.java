package com.san.api.domain.github.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.san.api.domain.github.dto.response.GithubStarRecommendationCollectResponse;
import com.san.api.domain.github.entity.GithubStarRecommendation;
import com.san.api.domain.github.repository.GithubStarRecommendationRepository;
import com.san.api.domain.knowledge.entity.CardTag;
import com.san.api.domain.knowledge.entity.Category;
import com.san.api.domain.knowledge.entity.KnowledgeCard;
import com.san.api.domain.knowledge.entity.Tag;
import com.san.api.domain.knowledge.repository.CardTagRepository;
import com.san.api.domain.knowledge.repository.CategoryRepository;
import com.san.api.domain.knowledge.repository.KnowledgeCardRepository;
import com.san.api.domain.knowledge.repository.TagRepository;
import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.entity.SourceType;
import com.san.api.domain.scrap.repository.ScrapRepository;
import com.san.api.domain.scrap.service.ScrapContentHashPolicy;
import com.san.api.domain.user.entity.User;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AiErrorCode;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.external.ai.dto.response.AiAnalyzeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** GitHub Star 추천 후보 수집 Service */
@Service
@RequiredArgsConstructor
public class GithubStarRecommendationCollectService {

    private final GithubStarRecommendationRepository githubStarRecommendationRepository;
    private final ScrapRepository scrapRepository;
    private final KnowledgeCardRepository knowledgeCardRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final CardTagRepository cardTagRepository;
    private final ScrapContentHashPolicy scrapContentHashPolicy;
    private final ObjectMapper objectMapper;

    /**
     * GitHub Star 추천 후보를 실제 수집 데이터로 저장
     *
     * @param userId 사용자 ID
     * @param recommendationId 추천 후보 ID
     * @return 수집된 원본과 지식카드 ID 응답
     */
    @Transactional
    public GithubStarRecommendationCollectResponse collect(UUID userId, UUID recommendationId) {
        GithubStarRecommendation recommendation = githubStarRecommendationRepository
                .findByIdAndUserIdForUpdate(recommendationId, userId)
                .orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));

        validateNotCollected(recommendation);

        AiAnalyzeResponse analysis = parseAnalysisResult(recommendation.getAnalysisResult());
        Scrap scrap = findOrCreateScrap(recommendation.getUser(), recommendation.getUrl());
        KnowledgeCard card = findOrCreateCard(scrap, recommendation, analysis);

        recommendation.markCollected(card.getCardId());
        return new GithubStarRecommendationCollectResponse(
                recommendation.getGithubStarRecommendationId(),
                scrap.getScrapId(),
                card.getCardId(),
                recommendation.isCollected()
        );
    }

    private void validateNotCollected(GithubStarRecommendation recommendation) {
        if (recommendation.isCollected()) {
            throw new BusinessException(CommonErrorCode.DUPLICATE_RESOURCE);
        }
    }

    private AiAnalyzeResponse parseAnalysisResult(String analysisResult) {
        try {
            return objectMapper.readValue(analysisResult, AiAnalyzeResponse.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(AiErrorCode.AI_ANALYSIS_INVALID_RESPONSE);
        }
    }

    private Scrap findOrCreateScrap(User user, String url) {
        String normalizedUrl = scrapContentHashPolicy.normalize(url);
        String contentHash = scrapContentHashPolicy.createContentHash(normalizedUrl);

        return scrapRepository.findByUser_UserIdAndSourceTypeAndContentHash(
                        user.getUserId(),
                        SourceType.LINK,
                        contentHash
                )
                .orElseGet(() -> saveScrap(Scrap.builder()
                        .user(user)
                        .sourceType(SourceType.LINK)
                        .sourceUrl(normalizedUrl)
                        .rawContent(normalizedUrl)
                        .contentHash(contentHash)
                        .build(), user.getUserId(), contentHash));
    }

    private Scrap saveScrap(Scrap scrap, UUID userId, String contentHash) {
        try {
            return scrapRepository.saveAndFlush(scrap);
        } catch (DataIntegrityViolationException e) {
            return scrapRepository.findByUser_UserIdAndSourceTypeAndContentHash(
                            userId,
                            SourceType.LINK,
                            contentHash
                    )
                    .orElseThrow(() -> e);
        }
    }

    private KnowledgeCard findOrCreateCard(
            Scrap scrap,
            GithubStarRecommendation recommendation,
            AiAnalyzeResponse analysis
    ) {
        return knowledgeCardRepository.findByScrapIdWithCategory(scrap.getScrapId())
                .orElseGet(() -> createCard(scrap, recommendation, analysis));
    }

    private KnowledgeCard createCard(
            Scrap scrap,
            GithubStarRecommendation recommendation,
            AiAnalyzeResponse analysis
    ) {
        Category category = findOrCreateCategory(scrap.getUser(), analysis.category());
        return saveCard(scrap, category, recommendation, analysis);
    }

    private KnowledgeCard saveCard(
            Scrap scrap,
            Category category,
            GithubStarRecommendation recommendation,
            AiAnalyzeResponse analysis
    ) {
        try {
            KnowledgeCard card = knowledgeCardRepository.saveAndFlush(KnowledgeCard.builder()
                    .scrap(scrap)
                    .category(category)
                    .title(firstNotBlank(analysis.title(), recommendation.getTitle()))
                    .summary(firstNotBlank(analysis.summary(), recommendation.getSummary()))
                    .embedding(analysis.embedding())
                    .build());

            saveTags(card, analysis.tags());
            return card;
        } catch (DataIntegrityViolationException e) {
            return knowledgeCardRepository.findByScrapIdWithCategory(scrap.getScrapId())
                    .orElseThrow(() -> e);
        }
    }

    private Category findOrCreateCategory(User user, String categoryName) {
        String normalizedName = firstNotBlank(categoryName, "기타");
        return categoryRepository.findByUser_UserIdAndCategoryName(user.getUserId(), normalizedName)
                .orElseGet(() -> categoryRepository.save(Category.builder()
                        .user(user)
                        .categoryName(normalizedName)
                        .build()));
    }

    private void saveTags(KnowledgeCard card, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return;
        }

        tagNames.stream()
                .filter(tagName -> !isBlank(tagName))
                .map(String::trim)
                .distinct()
                .map(tagName -> findOrCreateTag(card, tagName))
                .forEach(cardTagRepository::save);
    }

    private CardTag findOrCreateTag(KnowledgeCard card, String tagName) {
        Tag tag = tagRepository.findByTagName(tagName)
                .orElseGet(() -> saveTag(tagName));

        return new CardTag(card, tag);
    }

    private Tag saveTag(String tagName) {
        try {
            return tagRepository.saveAndFlush(Tag.builder()
                    .tagName(tagName)
                    .build());
        } catch (DataIntegrityViolationException e) {
            return tagRepository.findByTagName(tagName)
                    .orElseThrow(() -> e);
        }
    }

    private String firstNotBlank(String first, String fallback) {
        if (!isBlank(first)) {
            return first.trim();
        }
        return fallback.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
