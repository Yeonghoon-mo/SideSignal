package com.sidesignal.common.security;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import com.sidesignal.auth.domain.UserEntity;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String JWT_ALGORITHM = "HS256";
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
    private static final TypeReference<Map<String, Object>> CLAIMS_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final Clock clock = Clock.systemUTC();

    @Value("${sidesignal.jwt.issuer}")
    private String issuer;

    @Value("${sidesignal.jwt.secret}")
    private String secret;

    @Value("${sidesignal.jwt.access-token-expiration}")
    private Duration accessTokenExpiration;

    // HMAC 서명키 길이 검증
    @PostConstruct
    void validateSecret() {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("jwt secret must be at least 32 characters");
        }
    }

    // access token 발급
    public TokenIssueResult issue(UserEntity user) {
        Instant issuedAt = Instant.now(clock);
        Instant expiresAt = issuedAt.plus(accessTokenExpiration);
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", issuer);
        claims.put("sub", user.getId().toString());
        claims.put("email", user.getEmail());
        claims.put("iat", issuedAt.getEpochSecond());
        claims.put("exp", expiresAt.getEpochSecond());

        String accessToken = encode(claims);

        return new TokenIssueResult(accessToken, accessTokenExpiration.toSeconds());
    }

    // access token 인증 정보 추출
    public AuthenticatedUser authenticate(String token) {
        Map<String, Object> claims = parse(token);
        validateIssuer(claims);
        validateExpiration(claims);

        return new AuthenticatedUser(
                parseUserId(claims),
                stringClaim(claims, "email")
        );
    }

    // JWT header, claims, signature 조립
    private String encode(Map<String, Object> claims) {
        Map<String, Object> header = Map.of(
                "alg", JWT_ALGORITHM,
                "typ", "JWT"
        );
        String encodedHeader = base64UrlEncode(toJson(header));
        String encodedClaims = base64UrlEncode(toJson(claims));
        String signingInput = encodedHeader + "." + encodedClaims;
        String signature = base64UrlEncode(sign(signingInput));

        return signingInput + "." + signature;
    }

    // JWT 구조 분해와 서명 검증
    private Map<String, Object> parse(String token) {
        String[] parts = token.split("\\.");

        // header.claims.signature 형식 검증
        if (parts.length != 3) {
            throw new JwtAuthenticationException("invalid jwt format");
        }

        String signingInput = parts[0] + "." + parts[1];
        validateSignature(signingInput, parts[2]);
        validateHeader(parts[0]);

        return readClaims(parts[1]);
    }

    // JWT 알고리즘 검증
    private void validateHeader(String encodedHeader) {
        Map<String, Object> header = readJson(encodedHeader);
        String algorithm = stringClaim(header, "alg");

        // 지원 알고리즘 제한
        if (!JWT_ALGORITHM.equals(algorithm)) {
            throw new JwtAuthenticationException("unsupported jwt algorithm");
        }
    }

    private void validateSignature(String signingInput, String encodedSignature) {
        byte[] expectedSignature = sign(signingInput);
        byte[] signature = decode(encodedSignature);

        // JWT 서명 비교
        if (!java.security.MessageDigest.isEqual(expectedSignature, signature)) {
            throw new JwtAuthenticationException("invalid jwt signature");
        }
    }

    // JWT 발급자 검증
    private void validateIssuer(Map<String, Object> claims) {
        if (!issuer.equals(stringClaim(claims, "iss"))) {
            throw new JwtAuthenticationException("invalid jwt issuer");
        }
    }

    // JWT 만료 시각 검증
    private void validateExpiration(Map<String, Object> claims) {
        Instant expiresAt = Instant.ofEpochSecond(longClaim(claims));

        if (!expiresAt.isAfter(Instant.now(clock))) {
            throw new JwtAuthenticationException("expired jwt");
        }
    }

    // JWT subject 사용자 ID 변환
    private UUID parseUserId(Map<String, Object> claims) {
        try {
            return UUID.fromString(stringClaim(claims, "sub"));
        } catch (IllegalArgumentException exception) {
            throw new JwtAuthenticationException("invalid jwt subject");
        }
    }

    // JWT JSON 직렬화
    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException exception) {
            throw new IllegalStateException("jwt json serialization failed", exception);
        }
    }

    // JWT claims 파싱
    private Map<String, Object> readClaims(String encodedClaims) {
        return readJson(encodedClaims);
    }

    // Base64URL 인코딩 JSON 파싱
    private Map<String, Object> readJson(String encodedValue) {
        try {
            return objectMapper.readValue(decode(encodedValue), CLAIMS_TYPE);
        } catch (JacksonException exception) {
            throw new JwtAuthenticationException("invalid jwt json");
        }
    }

    // HMAC-SHA256 서명 생성
    private byte[] sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.InvalidKeyException exception) {
            throw new IllegalStateException("jwt signing key invalid", exception);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("jwt signing algorithm unavailable", exception);
        }
    }

    // 문자열 Base64URL 인코딩
    private static String base64UrlEncode(String value) {
        return base64UrlEncode(value.getBytes(StandardCharsets.UTF_8));
    }

    // 바이트 배열 Base64URL 인코딩
    private static String base64UrlEncode(byte[] value) {
        return BASE64_URL_ENCODER.encodeToString(value);
    }

    // Base64URL 디코딩
    private static byte[] decode(String value) {
        try {
            return BASE64_URL_DECODER.decode(value);
        } catch (IllegalArgumentException exception) {
            throw new JwtAuthenticationException("invalid jwt base64");
        }
    }

    // 문자열 claim 추출
    private static String stringClaim(Map<String, Object> claims, String name) {
        Object value = claims.get(name);

        if (value instanceof String stringValue) {
            return stringValue;
        }

        throw new JwtAuthenticationException("missing jwt claim: " + name);
    }

    // 숫자 claim 추출
    private static long longClaim(Map<String, Object> claims) {
        Object value = claims.get("exp");

        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }

        throw new JwtAuthenticationException("missing jwt claim: " + "exp");
    }

}
