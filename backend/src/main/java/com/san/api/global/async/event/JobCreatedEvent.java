package com.san.api.global.async.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import com.san.api.global.async.entity.JobType;

/**
 * 비동기 잡 생성 시 발행되는 이벤트.
 *
 * AsyncJobManager가 발행하고, 각 AsyncJobProcessor 구현체가 수신하여 처리한다.
 */
@Getter
@RequiredArgsConstructor
public class JobCreatedEvent {

    private final UUID jobId;
    private final JobType jobType;
    private final UUID targetId;
}
