package com.san.api.global.external.github.client;

import com.san.api.global.exception.BusinessException;
import com.san.api.global.exception.errorcode.AuthErrorCode;
import com.san.api.global.exception.errorcode.CommonErrorCode;
import com.san.api.global.external.github.dto.request.GithubCreateContentRequest;
import com.san.api.global.external.github.dto.response.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

/**
 * GitHub OAuth와 사용자 API 통신을 담당하는 클라이언트입니다.
 *
 * 외부 API 실패는 도메인 예외로 변환해 상위 계층이 GitHub 응답 구조에
 * 직접 의존하지 않도록 합니다.
 */
@Slf4j
@Component
public class GithubApiClient {

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String scope;

    public GithubApiClient(
            @Value("${oauth.github.client-id}") String clientId,
            @Value("${oauth.github.client-secret}") String clientSecret,
            @Value("${oauth.github.redirect-uri}") String redirectUri,
            @Value("${oauth.github.scope}") String scope) {
        this.restClient = RestClient.builder().build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.scope = scope;
    }

    /** 백엔드 callback URI와 CSRF 방어용 state를 포함한 GitHub OAuth authorize URL을 생성합니다. */
    public String createAuthorizationUrl(String state) {
        return UriComponentsBuilder.fromUriString("https://github.com/login/oauth/authorize")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", scope)
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    /** GitHub OAuth authorization code를 GitHub access token으로 교환합니다. */
    public GithubAccessTokenResponse requestAccessToken(String code) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("code", code);
        body.add("redirect_uri", redirectUri);

        try {
            GithubAccessTokenResponse response = restClient.post()
                    .uri("https://github.com/login/oauth/access_token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(GithubAccessTokenResponse.class);

            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new BusinessException(AuthErrorCode.GITHUB_OAUTH_FAILED);
            }
            return response;
        } catch (RestClientException e) {
            throw new BusinessException(AuthErrorCode.GITHUB_OAUTH_FAILED);
        }
    }

    /** GitHub access token으로 현재 GitHub 사용자 프로필을 조회합니다. */
    public GithubUserProfileResponse findUserProfile(String accessToken) {
        try {
            GithubUserProfileResponse profile = restClient.get()
                    .uri("https://api.github.com/user")
                    .headers(headers -> setGithubHeaders(headers, accessToken))
                    .retrieve()
                    .body(GithubUserProfileResponse.class);

            if (profile == null || profile.id() == null || profile.login() == null || profile.login().isBlank()) {
                throw new BusinessException(AuthErrorCode.GITHUB_OAUTH_FAILED);
            }
            return profile;
        } catch (RestClientException e) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_API_ERROR);
        }
    }

    /** GitHub access token으로 사용자가 접근 가능한 저장소 목록을 조회합니다. */
    public List<ExternalGithubRepositoryResponse> findRepositories(String accessToken) {
        try {
            List<ExternalGithubRepositoryResponse> repositories = restClient.get()
                    .uri("https://api.github.com/user/repos?visibility=all&affiliation=owner,collaborator&sort=updated&per_page=100")
                    .headers(headers -> setGithubHeaders(headers, accessToken))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            return repositories == null ? List.of() : repositories;
        } catch (RestClientException e) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_API_ERROR);
        }
    }

    /** GitHub 저장소의 특정 경로에 파일이 존재하는지 확인합니다. */
    public boolean existsContent(String accessToken, String owner, String repo, String path, String branch) {
        try {
            GithubContentResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.github.com")
                            .pathSegment("repos", owner, repo, "contents")
                            .path("/" + path)
                            .queryParam("ref", branch)
                            .build())
                    .headers(headers -> setGithubHeaders(headers, accessToken))
                    .retrieve()
                    .body(GithubContentResponse.class);

            return response != null;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return false;
            }
            throw new BusinessException(CommonErrorCode.EXTERNAL_API_ERROR);
        } catch (RestClientException e) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_API_ERROR);
        }
    }

    /** GitHub 저장소에 새 파일을 생성하고 해당 변경을 커밋합니다. */
    @Retryable(
            retryFor = {RestClientException.class},
            noRetryFor = {BusinessException.class, HttpClientErrorException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public GithubCreateContentResponse createContent(
            String accessToken,
            String owner,
            String repo,
            String path,
            String branch,
            String message,
            String base64Content
    ) {
        GithubCreateContentRequest request = new GithubCreateContentRequest(message, base64Content, branch);

        GithubCreateContentResponse response = restClient.put()
                .uri(uriBuilder -> uriBuilder
                        .scheme("https")
                        .host("api.github.com")
                        .pathSegment("repos", owner, repo, "contents")
                        .path("/" + path)
                        .build())
                .headers(headers -> setGithubHeaders(headers, accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(GithubCreateContentResponse.class);

        if (response == null || response.commit() == null || response.commit().sha() == null) {
            throw new BusinessException(CommonErrorCode.EXTERNAL_API_ERROR);
        }
        return response;
    }

    /**
     * 재시도 소진 후 호출되는 복구 메서드.
     * BusinessException은 그대로 전파하고, RestClientException은 도메인 예외로 변환한다.
     *
     * @param e 마지막 시도에서 발생한 예외
     */
    @Recover
    public GithubCreateContentResponse recoverCreateContent(
            Exception e,
            String accessToken, String owner, String repo, String path,
            String branch, String message, String base64Content
    ) {
        if (e instanceof BusinessException be) {
            throw be;
        }
        log.error("GitHub createContent failed after all retries: {}", e.getMessage(), e);
        throw new BusinessException(CommonErrorCode.EXTERNAL_API_ERROR);
    }

    private void setGithubHeaders(HttpHeaders headers, String accessToken) {
        headers.setBearerAuth(accessToken);
        headers.set(HttpHeaders.ACCEPT, "application/vnd.github+json");
        headers.set("X-GitHub-Api-Version", "2022-11-28");
    }
}
