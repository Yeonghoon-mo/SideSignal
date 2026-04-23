package com.sidesignal.common.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class GlobalExceptionHandlerTests {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
            .standaloneSetup(new TestExceptionController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .build();
    }

    @Test
    void businessExceptionReturnsErrorResponse() throws Exception {
        mockMvc.perform(get("/test/business-exception"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("테스트 리소스 없음"))
            .andExpect(jsonPath("$.path").value("/test/business-exception"))
            .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    void authenticationExceptionReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/test/authentication-exception"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.message").value("인증 필요"));
    }

    @Test
    void accessDeniedExceptionReturnsForbidden() throws Exception {
        mockMvc.perform(get("/test/access-denied-exception"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.code").value("FORBIDDEN"))
            .andExpect(jsonPath("$.message").value("접근 권한 없음"));
    }

    @Test
    void validationExceptionReturnsFieldErrors() throws Exception {
        mockMvc.perform(post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name": ""
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    void malformedJsonReturnsInvalidRequest() throws Exception {
        mockMvc.perform(post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.path").value("/test/validation"));
    }

    @Test
    void unexpectedExceptionReturnsInternalServerError() throws Exception {
        mockMvc.perform(get("/test/unexpected-exception"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
            .andExpect(jsonPath("$.message").value("서버 내부 오류"));
    }

    @RestController
    static class TestExceptionController {

        @GetMapping("/test/business-exception")
        void businessException() {
            throw new BusinessException(ErrorCode.NOT_FOUND, "테스트 리소스 없음");
        }

        @GetMapping("/test/authentication-exception")
        void authenticationException() {
            throw new InsufficientAuthenticationException("missing token");
        }

        @GetMapping("/test/access-denied-exception")
        void accessDeniedException() {
            throw new AccessDeniedException("denied");
        }

        @PostMapping("/test/validation")
        void validation(@Valid @RequestBody TestRequest request) {
        }

        @GetMapping("/test/unexpected-exception")
        void unexpectedException() {
            throw new IllegalStateException("unexpected");
        }

    }

    record TestRequest(
        @NotBlank String name
    ) {
    }

}
