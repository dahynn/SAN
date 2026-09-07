package com.san.api.global.async.processor;

import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.event.JobCreatedEvent;

import java.util.UUID;

/**
 * 비동기 잡 처리기 표준 인터페이스.
 *
 * 모든 비동기 작업(카드 분석, 리콜 생성 등) 구현체는 이 인터페이스를 반드시 구현해야 한다.
 */
public interface AsyncJobProcessor {

    JobType supports();

    void process(UUID jobId, UUID targetId);

    default void handleIfSupported(JobCreatedEvent event) {
        if (event.getJobType() != supports()) {
            return;
        }

        process(event.getJobId(), event.getTargetId());
    }

}
