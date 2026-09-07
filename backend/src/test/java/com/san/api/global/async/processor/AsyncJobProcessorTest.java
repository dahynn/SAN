package com.san.api.global.async.processor;

import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.event.JobCreatedEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 비동기 작업 이벤트 라우팅 공통 로직 테스트 */
class AsyncJobProcessorTest {

    @Test
    void handleIfSupported는_지원하는_작업만_process에_전달한다() {
        RecordingAsyncJobProcessor processor = new RecordingAsyncJobProcessor(JobType.SCRAP_REFINE);
        UUID supportedJobId = UUID.randomUUID();
        UUID supportedTargetId = UUID.randomUUID();

        processor.handleIfSupported(new JobCreatedEvent(UUID.randomUUID(), JobType.CARD_ANALYSIS, UUID.randomUUID()));
        processor.handleIfSupported(new JobCreatedEvent(supportedJobId, JobType.SCRAP_REFINE, supportedTargetId));

        assertThat(processor.processCallCount).isEqualTo(1);
        assertThat(processor.processedJobId).isEqualTo(supportedJobId);
        assertThat(processor.processedTargetId).isEqualTo(supportedTargetId);
    }

    private static class RecordingAsyncJobProcessor implements AsyncJobProcessor {

        private final JobType supports;
        private int processCallCount;
        private UUID processedJobId;
        private UUID processedTargetId;

        private RecordingAsyncJobProcessor(JobType supports) {
            this.supports = supports;
        }

        @Override
        public JobType supports() {
            return supports;
        }

        @Override
        public void process(UUID jobId, UUID targetId) {
            processCallCount++;
            processedJobId = jobId;
            processedTargetId = targetId;
        }
    }
}
