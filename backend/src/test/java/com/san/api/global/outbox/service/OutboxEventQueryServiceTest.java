package com.san.api.global.outbox.service;

import com.san.api.global.exception.BusinessException;
import com.san.api.global.outbox.dto.request.OutboxEventSearchRequest;
import com.san.api.global.outbox.dto.response.OutboxEventPageResponse;
import com.san.api.global.outbox.dto.response.OutboxEventResponse;
import com.san.api.global.outbox.entity.OutboxEvent;
import com.san.api.global.outbox.entity.OutboxEventStatus;
import com.san.api.global.outbox.entity.OutboxEventType;
import com.san.api.global.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OutboxEventQueryServiceTest {

    @Test
    @SuppressWarnings("unchecked")
    void searchReturnsOutboxEventPage() {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        OutboxEventQueryService service = new OutboxEventQueryService(repository);
        OutboxEvent event = feedbackEvent();
        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event)));

        OutboxEventPageResponse response = service.search(
                new OutboxEventSearchRequest(
                        OutboxEventType.FEEDBACK_MATTERMOST_NOTIFICATION,
                        OutboxEventStatus.PENDING,
                        "FEEDBACK",
                        event.getAggregateId(),
                        LocalDateTime.of(2026, 5, 19, 0, 0),
                        LocalDateTime.of(2026, 5, 20, 0, 0)
                ),
                0,
                20
        );

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().outboxEventId()).isEqualTo(event.getOutboxEventId());
        assertThat(response.content().getFirst().eventTypeDescription())
                .isEqualTo(OutboxEventType.FEEDBACK_MATTERMOST_NOTIFICATION.getDescription());
        assertThat(response.content().getFirst().statusDescription())
                .isEqualTo(OutboxEventStatus.PENDING.getDescription());
        verify(repository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void getReturnsOutboxEvent() {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        OutboxEventQueryService service = new OutboxEventQueryService(repository);
        OutboxEvent event = feedbackEvent();
        when(repository.findById(event.getOutboxEventId())).thenReturn(Optional.of(event));

        OutboxEventResponse response = service.get(event.getOutboxEventId());

        assertThat(response.outboxEventId()).isEqualTo(event.getOutboxEventId());
        assertThat(response.aggregateType()).isEqualTo("FEEDBACK");
        assertThat(response.payload()).containsEntry("content", "save button freezes");
    }

    @Test
    void getThrowsWhenOutboxEventDoesNotExist() {
        OutboxEventRepository repository = mock(OutboxEventRepository.class);
        OutboxEventQueryService service = new OutboxEventQueryService(repository);
        UUID outboxEventId = UUID.randomUUID();
        when(repository.findById(outboxEventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(outboxEventId))
                .isInstanceOf(BusinessException.class);
    }

    private OutboxEvent feedbackEvent() {
        UUID feedbackId = UUID.randomUUID();
        OutboxEvent event = OutboxEvent.builder()
                .eventType(OutboxEventType.FEEDBACK_MATTERMOST_NOTIFICATION)
                .aggregateType("FEEDBACK")
                .aggregateId(feedbackId)
                .payload(Map.of(
                        "feedbackId", feedbackId.toString(),
                        "content", "save button freezes"
                ))
                .build();
        ReflectionTestUtils.setField(event, "createdAt", LocalDateTime.of(2026, 5, 19, 10, 0));
        ReflectionTestUtils.setField(event, "updatedAt", LocalDateTime.of(2026, 5, 19, 10, 1));
        return event;
    }
}
