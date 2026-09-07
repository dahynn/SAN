package com.san.api.global.scheduler.service;

import com.san.api.domain.knowledge.repository.KnowledgeCardRepository;
import com.san.api.domain.recall.entity.RecallQuizGeneration;
import com.san.api.domain.recall.entity.RecallQuizType;
import com.san.api.domain.recall.repository.RecallQuizGenerationRepository;
import com.san.api.domain.user.entity.AuthProvider;
import com.san.api.domain.user.entity.User;
import com.san.api.domain.user.repository.UserRepository;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.service.AsyncJobManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecallQuizAutoGenerationScheduleServiceTest {

    @Mock
    private KnowledgeCardRepository knowledgeCardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RecallQuizGenerationRepository recallQuizGenerationRepository;

    @Mock
    private AsyncJobManager asyncJobManager;

    @InjectMocks
    private RecallQuizAutoGenerationScheduleService service;

    @Test
    void generate_whenNoExistingGeneration_enqueuesJobForEachQuizType() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().username("tester").passwordHash("pw").provider(AuthProvider.LOCAL).build();
        when(knowledgeCardRepository.findDistinctUserIdsByScrapCreatedBetween(any(), any()))
                .thenReturn(List.of(userId));
        when(recallQuizGenerationRepository
                .findFirstByUser_UserIdAndTargetDateAndQuizTypeOrderByCreatedAtDesc(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        RecallQuizGeneration generation = RecallQuizGeneration.builder()
                .user(user).targetDate(LocalDate.now().minusDays(1)).quizType(RecallQuizType.OX).build();
        when(recallQuizGenerationRepository.save(any())).thenReturn(generation);

        service.generate();

        // OX, SHORT_ANSWER 두 타입 모두 enqueue
        verify(asyncJobManager, times(RecallQuizType.values().length))
                .enqueue(eq(JobType.RECALL_QUIZ_GENERATION), any());
    }

    @Test
    void generate_whenGenerationAlreadyExists_skipsEnqueue() {
        UUID userId = UUID.randomUUID();
        User user = User.builder().username("tester").passwordHash("pw").provider(AuthProvider.LOCAL).build();
        RecallQuizGeneration existing = RecallQuizGeneration.builder()
                .user(user).targetDate(LocalDate.now().minusDays(1)).quizType(RecallQuizType.OX).build();
        when(knowledgeCardRepository.findDistinctUserIdsByScrapCreatedBetween(any(), any()))
                .thenReturn(List.of(userId));
        when(recallQuizGenerationRepository
                .findFirstByUser_UserIdAndTargetDateAndQuizTypeOrderByCreatedAtDesc(any(), any(), any()))
                .thenReturn(Optional.of(existing));

        service.generate();

        verify(asyncJobManager, never()).enqueue(any(), any());
    }
}
