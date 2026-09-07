package com.san.api.domain.til.service;

import com.san.api.domain.github.entity.GithubRepositoryConnection;
import com.san.api.domain.til.dto.response.TilGithubContributionCommitResponse;
import com.san.api.domain.til.dto.response.TilGithubContributionDayResponse;
import com.san.api.domain.til.dto.response.TilGithubContributionRepositoryResponse;
import com.san.api.domain.til.dto.response.TilGithubContributionResponse;
import com.san.api.domain.til.entity.TilGithubCommit;
import com.san.api.domain.til.entity.TilGithubCommitStatus;
import com.san.api.domain.til.repository.TilGithubCommitRepository;
import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** 서비스에서 GitHub로 성공적으로 커밋한 TIL 기록을 contribution 잔디 형태로 집계하는 서비스 */
@Service
@RequiredArgsConstructor
public class TilGithubContributionService {

    private static final int DEFAULT_LOOKBACK_DAYS = 365;
    private static final int MAX_RANGE_DAYS = 370;

    private final TilGithubCommitRepository tilGithubCommitRepository;
    private final Clock clock;

    /**
     * 사용자의 TIL GitHub 커밋 이력을 contribution 잔디 데이터로 조회합니다.
     *
     * from/to가 없으면 오늘 기준 최근 365일을 조회하고, 단일 응답에서 날짜별 카운트와
     * 클릭 시 표시할 커밋 상세 목록을 함께 제공합니다.
     *
     * @param userId 조회할 사용자 ID
     * @param from 조회 시작일
     * @param to 조회 종료일
     * @param githubRepositoryId 특정 GitHub 레포지토리만 조회할 때 사용하는 GitHub repository ID
     * @return contribution 잔디 응답
     */
    @Transactional(readOnly = true)
    public TilGithubContributionResponse getContributions(
            UUID userId,
            LocalDate from,
            LocalDate to,
            Long githubRepositoryId
    ) {
        LocalDate today = LocalDate.now(clock);
        LocalDate resolvedTo = to == null || to.isAfter(today) ? today : to;
        LocalDate resolvedFrom = from == null ? resolvedTo.minusDays(DEFAULT_LOOKBACK_DAYS - 1L) : from;
        validateRange(resolvedFrom, resolvedTo);

        List<TilGithubCommit> commits = tilGithubCommitRepository.findContributions(
                userId,
                TilGithubCommitStatus.COMPLETED,
                resolvedFrom.atStartOfDay(),
                resolvedTo.plusDays(1).atStartOfDay(),
                githubRepositoryId
        );
        Map<LocalDate, Integer> countsByDate = countByDate(commits);
        List<TilGithubContributionDayResponse> days = createDays(resolvedFrom, resolvedTo, countsByDate);

        return new TilGithubContributionResponse(
                resolvedFrom,
                resolvedTo,
                commits.size(),
                (int) countsByDate.values().stream().filter(count -> count > 0).count(),
                calculateCurrentStreak(resolvedFrom, resolvedTo, today, countsByDate),
                calculateLongestStreak(resolvedFrom, resolvedTo, countsByDate),
                days,
                createRepositoryStats(commits),
                commits.stream()
                        .map(TilGithubContributionCommitResponse::from)
                        .toList()
        );
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE);
        }
        long days = ChronoUnit.DAYS.between(from, to) + 1L;
        if (days > MAX_RANGE_DAYS) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private Map<LocalDate, Integer> countByDate(List<TilGithubCommit> commits) {
        return commits.stream()
                .filter(commit -> commit.getPushedAt() != null)
                .collect(Collectors.groupingBy(
                        commit -> commit.getPushedAt().toLocalDate(),
                        Collectors.summingInt(commit -> 1)
                ));
    }

    private List<TilGithubContributionDayResponse> createDays(
            LocalDate from,
            LocalDate to,
            Map<LocalDate, Integer> countsByDate
    ) {
        return from.datesUntil(to.plusDays(1))
                .map(date -> {
                    int count = countsByDate.getOrDefault(date, 0);
                    return new TilGithubContributionDayResponse(date, count, contributionLevel(count));
                })
                .toList();
    }

    private int contributionLevel(int count) {
        if (count <= 0) {
            return 0;
        }
        if (count == 1) {
            return 1;
        }
        if (count <= 3) {
            return 2;
        }
        if (count <= 5) {
            return 3;
        }
        return 4;
    }

    private int calculateCurrentStreak(
            LocalDate from,
            LocalDate to,
            LocalDate today,
            Map<LocalDate, Integer> countsByDate
    ) {
        LocalDate startDate = to.equals(today) && countsByDate.getOrDefault(today, 0) == 0
                ? today.minusDays(1)
                : to;
        int streak = 0;
        for (LocalDate date = startDate; !date.isBefore(from); date = date.minusDays(1)) {
            if (countsByDate.getOrDefault(date, 0) == 0) {
                break;
            }
            streak++;
        }
        return streak;
    }

    private int calculateLongestStreak(LocalDate from, LocalDate to, Map<LocalDate, Integer> countsByDate) {
        int longest = 0;
        int current = 0;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            if (countsByDate.getOrDefault(date, 0) > 0) {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 0;
            }
        }
        return longest;
    }

    private List<TilGithubContributionRepositoryResponse> createRepositoryStats(List<TilGithubCommit> commits) {
        Map<Long, RepositoryStat> stats = new HashMap<>();
        commits.forEach(commit -> {
            GithubRepositoryConnection repository = commit.getGithubRepositoryConnection();
            stats.computeIfAbsent(repository.getGithubRepositoryId(), ignored -> new RepositoryStat(repository))
                    .increment();
        });

        return stats.values().stream()
                .sorted(Comparator.comparingInt(RepositoryStat::count).reversed())
                .map(RepositoryStat::toResponse)
                .toList();
    }

    private static class RepositoryStat {

        private final GithubRepositoryConnection repository;
        private int count;

        private RepositoryStat(GithubRepositoryConnection repository) {
            this.repository = repository;
        }

        private void increment() {
            count++;
        }

        private int count() {
            return count;
        }

        private TilGithubContributionRepositoryResponse toResponse() {
            return new TilGithubContributionRepositoryResponse(
                    repository.getGithubRepositoryId(),
                    repository.getName(),
                    repository.getFullName(),
                    repository.getHtmlUrl(),
                    count
            );
        }
    }
}
