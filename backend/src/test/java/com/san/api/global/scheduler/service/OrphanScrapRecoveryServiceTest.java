package com.san.api.global.scheduler.service;

import com.san.api.domain.scrap.entity.Scrap;
import com.san.api.domain.scrap.repository.ScrapRepository;
import com.san.api.global.async.entity.JobType;
import com.san.api.global.async.service.AsyncJobManager;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrphanScrapRecoveryServiceTest {

    @Mock
    private ScrapRepository scrapRepository;

    @Mock
    private AsyncJobManager asyncJobManager;

    @InjectMocks
    private OrphanScrapRecoveryService orphanScrapRecoveryService;

    @Test
    void recover_whenNoOrphans_doesNotEnqueue() {
        when(scrapRepository.findOrphanScraps(eq(JobType.CARD_ANALYSIS), any())).thenReturn(List.of());

        orphanScrapRecoveryService.recover();

        verify(asyncJobManager, never()).enqueue(any(), any());
    }

    @Test
    void recover_enqueuesJobForEachOrphan() {
        Scrap scrap1 = Scrap.builder().rawContent("a").build();
        Scrap scrap2 = Scrap.builder().rawContent("b").build();
        when(scrapRepository.findOrphanScraps(eq(JobType.CARD_ANALYSIS), any()))
                .thenReturn(List.of(scrap1, scrap2));

        orphanScrapRecoveryService.recover();

        verify(asyncJobManager).enqueue(JobType.CARD_ANALYSIS, scrap1.getScrapId());
        verify(asyncJobManager).enqueue(JobType.CARD_ANALYSIS, scrap2.getScrapId());
    }

    @Test
    void recover_whenDuplicateResource_silentlySkips() {
        Scrap scrap = Scrap.builder().rawContent("a").build();
        when(scrapRepository.findOrphanScraps(eq(JobType.CARD_ANALYSIS), any()))
                .thenReturn(List.of(scrap));
        doThrow(new BusinessException(CommonErrorCode.DUPLICATE_RESOURCE))
                .when(asyncJobManager).enqueue(JobType.CARD_ANALYSIS, scrap.getScrapId());

        orphanScrapRecoveryService.recover();

        verify(asyncJobManager).enqueue(JobType.CARD_ANALYSIS, scrap.getScrapId());
    }

    @Test
    void recover_whenUnexpectedException_continuesRemainingOrphans() {
        Scrap scrap1 = Scrap.builder().rawContent("a").build();
        Scrap scrap2 = Scrap.builder().rawContent("b").build();
        when(scrapRepository.findOrphanScraps(eq(JobType.CARD_ANALYSIS), any()))
                .thenReturn(List.of(scrap1, scrap2));
        doThrow(new BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR))
                .when(asyncJobManager).enqueue(JobType.CARD_ANALYSIS, scrap1.getScrapId());

        orphanScrapRecoveryService.recover();

        verify(asyncJobManager).enqueue(JobType.CARD_ANALYSIS, scrap2.getScrapId());
    }
}
