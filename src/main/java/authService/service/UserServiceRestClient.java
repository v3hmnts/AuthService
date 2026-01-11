package authService.service;

import authService.dto.ErrorResponse;
import authService.dto.UserServiceUserRegistrationRequestDto;
import authService.dto.UserServiceUserRegistrationResponseDto;
import authService.exception.UserRegistrationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class UserServiceRestClient {

    private final Logger logger = LoggerFactory.getLogger(UserServiceRestClient.class);
    private final ObjectMapper objectMapper;
    private final TokenService tokenService;
    private final String USER_SERVICE_BASE_URL;
    private final RestClient restClient;

    public UserServiceRestClient(@Value("${user-service.base-url}") String userServiceBaseUrl, TokenService tokenService, ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
        this.USER_SERVICE_BASE_URL = userServiceBaseUrl;
        this.restClient = RestClient.builder()
                .baseUrl(USER_SERVICE_BASE_URL)
                .build();
    }


    @CircuitBreaker(name = "userServiceClient")
    @Retry(name = "userServiceClient")
    public UserServiceUserRegistrationResponseDto processUserRegistrationInUserService(UserServiceUserRegistrationRequestDto requestDto) throws Exception {
        UserServiceUserRegistrationResponseDto response = restClient
                .post()
                .uri("/api/v1/users")
                .header("Authorization", "Bearer " + tokenService.createNewServiceToken())
                .body(requestDto)
                .retrieve()
                .onStatus(httpStatusCode -> !httpStatusCode.is2xxSuccessful(), (request, resp) -> {
                    logger.info(resp.getStatusCode().toString());
                    logger.info(resp.getStatusText().toString());
                    logger.info(objectMapper.writeValueAsString(resp.getBody()));
                    logger.warn("Failed to craete new user in userService");
                    throw new UserRegistrationException("Failed to create user");
                })
                .body(UserServiceUserRegistrationResponseDto.class);

        return response;
    }

    @CircuitBreaker(name = "userServiceClient")
    @Retry(name = "userServiceClient")
    public void compensateUserServiceRegistrationIfFailed(Long userId) throws Exception {
        UserServiceUserRegistrationResponseDto response = restClient
                .delete()
                .uri("/api/v1/users/{userId}", userId)
                .header("Authorization", "Bearer " + tokenService.createNewServiceToken())
                .retrieve()
                .onStatus(httpStatusCode -> !httpStatusCode.is2xxSuccessful(), (request, resp) -> {
                    logger.warn("Failed to compensate userRegistration with userId: {}", userId);
                    throw new UserRegistrationException("Failed to compensate user");
                })
                .body(UserServiceUserRegistrationResponseDto.class);
    }


}