package authService.service;

import authService.dto.ErrorResponse;
import authService.dto.UserServiceUserRegistrationRequestDto;
import authService.dto.UserServiceUserRegistrationResponseDto;
import authService.exception.UserRegistrationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.annotation.PostConstruct;
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
    @Value("${user-service.base-url}")
    private String USER_SERVICE_BASE_URL;
    @Value("${user.service.timeout.seconds:2}")
    private int userServiceDefaultTimeOut;
    private RestClient restClient;

    public UserServiceRestClient(TokenService tokenService, ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        this.restClient = RestClient.builder()
                .baseUrl(USER_SERVICE_BASE_URL)
                .build();
    }


    @CircuitBreaker(name = "userServiceClient")
    @Retry(name = "userServiceClient")
    public UserServiceUserRegistrationResponseDto processUserRegistrationInUserService(UserServiceUserRegistrationRequestDto requestDto) throws Exception {
        logger.info("POST to "+USER_SERVICE_BASE_URL);
        UserServiceUserRegistrationResponseDto response = restClient
                .post()
                .uri("/api/v1/users")
                .header("Authorization", "Bearer " + tokenService.createNewServiceToken())
                .body(requestDto)
                .retrieve()
                .onStatus(httpStatusCode -> !httpStatusCode.is2xxSuccessful(), (request, resp) -> {
                    logger.warn("Failed to craete new user in userService");
                    throw new UserRegistrationException(objectMapper.convertValue(resp.getBody(), ErrorResponse.class));
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
                    throw new UserRegistrationException(objectMapper.convertValue(resp.getBody(), ErrorResponse.class));
                })
                .body(UserServiceUserRegistrationResponseDto.class);
    }


}