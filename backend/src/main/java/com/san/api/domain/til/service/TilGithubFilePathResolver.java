package com.san.api.domain.til.service;

import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.TilErrorCode;
import com.san.api.global.external.github.client.GithubApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** 같은 날짜 TIL 제목 중복을 피하기 위해 GitHub 파일 경로를 결정하는 resolver */
@Component
@RequiredArgsConstructor
public class TilGithubFilePathResolver {

    private static final int MAX_SUFFIX = 5;

    private final TilGithubFilePolicy filePolicy;
    private final GithubApiClient githubApiClient;

    /**
     * GitHub 저장소에서 비어 있는 TIL 파일 경로를 찾습니다.
     *
     * @param accessToken GitHub access token
     * @param repositoryFullName owner/repo 형식의 GitHub 저장소 이름
     * @param branch 커밋 대상 브랜치
     * @param targetDate TIL 대상 날짜
     * @param title TIL 제목
     * @return 같은 날짜 디렉터리에서 사용 가능한 TIL 파일 경로
     */
    public String resolve(
            String accessToken,
            String repositoryFullName,
            String branch,
            LocalDate targetDate,
            String title
    ) {
        RepositoryName repositoryName = RepositoryName.from(repositoryFullName);
        String directoryPath = filePolicy.createDirectoryPath(targetDate);
        String slug = filePolicy.createSlug(title);

        for (int suffix = 0; suffix <= MAX_SUFFIX; suffix++) {
            String fileName = suffix == 0 ? slug + ".md" : slug + "-" + suffix + ".md";
            String filePath = directoryPath + "/" + fileName;

            boolean exists = githubApiClient.existsContent(
                    accessToken,
                    repositoryName.owner(),
                    repositoryName.repo(),
                    filePath,
                    branch
            );
            if (!exists) {
                return filePath;
            }
        }

        throw new BusinessException(TilErrorCode.TIL_GITHUB_FILE_PATH_UNAVAILABLE);
    }

    private record RepositoryName(String owner, String repo) {

        private static RepositoryName from(String fullName) {
            if (fullName == null || fullName.isBlank()) {
                throw new IllegalArgumentException("GitHub repository full name is required.");
            }

            String[] parts = fullName.split("/", 2);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                throw new IllegalArgumentException("GitHub repository full name must be owner/repo.");
            }
            return new RepositoryName(parts[0], parts[1]);
        }
    }
}
