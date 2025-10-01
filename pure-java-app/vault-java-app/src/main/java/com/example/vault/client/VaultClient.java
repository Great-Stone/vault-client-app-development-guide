package com.example.vault.client;

import com.example.vault.config.VaultConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Vault 클라이언트 - KV, Database Dynamic, Database Static 시크릿 엔진 지원
 */
public class VaultClient {
  private static final Logger logger = LoggerFactory.getLogger(VaultClient.class);

  private final VaultConfig config;
  private final CloseableHttpClient httpClient;
  private final ObjectMapper objectMapper;

  // 인증 토큰
  private String authToken;
  private LocalDateTime tokenExpiry;

  // 캐시된 시크릿들
  private final Map<String, SecretCache> secretCaches = new ConcurrentHashMap<>();

  public VaultClient(VaultConfig config) {
    this.config = config;
    this.httpClient = HttpClients.createDefault();
    this.objectMapper = new ObjectMapper();
  }

  /**
   * Vault에 로그인하여 인증 토큰을 획득
   */
  public boolean login() {
    try {
      String url = config.getUrl() + "/v1/auth/approle/login";

      Map<String, String> loginData = new HashMap<>();
      loginData.put("role_id", config.getRoleId());
      loginData.put("secret_id", config.getSecretId());

      String jsonPayload = objectMapper.writeValueAsString(loginData);

      HttpPost request = new HttpPost(url);
      request.setHeader("Content-Type", "application/json");
      request.setEntity(new StringEntity(jsonPayload, ContentType.APPLICATION_JSON));

      if (config.getNamespace() != null && !config.getNamespace().isEmpty()) {
        request.setHeader("X-Vault-Namespace", config.getNamespace());
      }

      try (CloseableHttpResponse response = httpClient.execute(request)) {
        if (response.getCode() == 200) {
          JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
          JsonNode authNode = responseNode.get("auth");

          if (authNode != null) {
            this.authToken = authNode.get("client_token").asText();
            int ttl = authNode.get("lease_duration").asInt();
            this.tokenExpiry = LocalDateTime.now().plusSeconds(ttl);

            logger.info("✅ Vault 로그인 성공 (TTL: {}초)", ttl);
            return true;
          }
        } else {
          logger.error("❌ Vault 로그인 실패: HTTP {}", response.getCode());
        }
      }
    } catch (Exception e) {
      logger.error("❌ Vault 로그인 중 오류 발생", e);
    }
    return false;
  }

  /**
   * 토큰이 만료되었는지 확인
   */
  public boolean isTokenExpired() {
    return tokenExpiry == null || LocalDateTime.now().isAfter(tokenExpiry.minusMinutes(5));
  }

  /**
   * 토큰 갱신
   */
  public boolean renewToken() {
    if (authToken == null) {
      return login();
    }

    try {
      String url = config.getUrl() + "/v1/auth/token/renew-self";
      HttpPost request = new HttpPost(url);
      request.setHeader("X-Vault-Token", authToken);

      if (config.getNamespace() != null && !config.getNamespace().isEmpty()) {
        request.setHeader("X-Vault-Namespace", config.getNamespace());
      }

      try (CloseableHttpResponse response = httpClient.execute(request)) {
        if (response.getCode() == 200) {
          JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
          JsonNode authNode = responseNode.get("auth");

          if (authNode != null) {
            int ttl = authNode.get("lease_duration").asInt();
            this.tokenExpiry = LocalDateTime.now().plusSeconds(ttl);
            logger.info("✅ 토큰 갱신 성공 (TTL: {}초)", ttl);
            return true;
          }
        }
      }
    } catch (Exception e) {
      logger.error("❌ 토큰 갱신 중 오류 발생", e);
    }
    return false;
  }

  /**
   * KV 시크릿 조회
   */
  public JsonNode getKvSecret() {
    if (!config.isKvEnabled()) {
      return null;
    }

    String cacheKey = "kv:" + config.getKvPath();
    SecretCache cache = secretCaches.get(cacheKey);

    // 캐시 확인
    if (cache != null && !cache.isStale(config.getKvRefreshInterval())) {
      logger.info("✅ KV 시크릿 캐시 사용 (버전: {})", cache.getVersion());
      return cache.getData();
    }

    try {
      if (isTokenExpired()) {
        if (!renewToken()) {
          return null;
        }
      }

      String url = String.format("%s/v1/%s-kv/data/%s",
          config.getUrl(), config.getEntity(), config.getKvPath());

      HttpGet request = new HttpGet(url);
      request.setHeader("X-Vault-Token", authToken);

      if (config.getNamespace() != null && !config.getNamespace().isEmpty()) {
        request.setHeader("X-Vault-Namespace", config.getNamespace());
      }

      try (CloseableHttpResponse response = httpClient.execute(request)) {
        if (response.getCode() == 200) {
          JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
          JsonNode dataNode = responseNode.get("data");

          if (dataNode != null) {
            JsonNode metadataNode = dataNode.get("metadata");
            int version = metadataNode != null ? metadataNode.get("version").asInt() : 0;

            // 캐시 업데이트
            secretCaches.put(cacheKey, new SecretCache(dataNode.get("data"), version));
            logger.info("✅ KV 시크릿 조회 성공 (버전: {})", version);
            return dataNode.get("data");
          }
        } else {
          logger.error("❌ KV 시크릿 조회 실패: HTTP {}", response.getCode());
        }
      }
    } catch (Exception e) {
      logger.error("❌ KV 시크릿 조회 중 오류 발생", e);
    }
    return null;
  }

  /**
   * Database Dynamic 시크릿 조회
   */
  public JsonNode getDatabaseDynamicSecret() {
    if (!config.isDatabaseDynamicEnabled()) {
      return null;
    }

    String cacheKey = "db_dynamic:" + config.getDatabaseDynamicRoleId();
    SecretCache cache = secretCaches.get(cacheKey);

    // 캐시 확인 (TTL 기반)
    if (cache != null && !cache.isStale(10)) { // 10초 임계값
      logger.info("✅ Database Dynamic 시크릿 캐시 사용 (TTL: {}초)", cache.getTtl());
      return cache.getData();
    }

    try {
      if (isTokenExpired()) {
        if (!renewToken()) {
          return null;
        }
      }

      String url = String.format("%s/v1/%s-database/creds/%s",
          config.getUrl(), config.getEntity(), config.getDatabaseDynamicRoleId());

      HttpGet request = new HttpGet(url);
      request.setHeader("X-Vault-Token", authToken);

      if (config.getNamespace() != null && !config.getNamespace().isEmpty()) {
        request.setHeader("X-Vault-Namespace", config.getNamespace());
      }

      try (CloseableHttpResponse response = httpClient.execute(request)) {
        if (response.getCode() == 200) {
          JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
          JsonNode dataNode = responseNode.get("data");

          if (dataNode != null) {
            int ttl = responseNode.get("lease_duration").asInt();
            String leaseId = responseNode.get("lease_id").asText();

            // 캐시 업데이트
            secretCaches.put(cacheKey, new SecretCache(dataNode, ttl, leaseId));
            logger.info("✅ Database Dynamic 시크릿 조회 성공 (TTL: {}초)", ttl);
            return dataNode;
          }
        } else {
          logger.error("❌ Database Dynamic 시크릿 조회 실패: HTTP {}", response.getCode());
        }
      }
    } catch (Exception e) {
      logger.error("❌ Database Dynamic 시크릿 조회 중 오류 발생", e);
    }
    return null;
  }

  /**
   * Database Static 시크릿 조회
   */
  public JsonNode getDatabaseStaticSecret() {
    if (!config.isDatabaseStaticEnabled()) {
      return null;
    }

    String cacheKey = "db_static:" + config.getDatabaseStaticRoleId();
    SecretCache cache = secretCaches.get(cacheKey);

    // 캐시 확인 (5분 간격)
    if (cache != null && !cache.isStale(300)) {
      logger.info("✅ Database Static 시크릿 캐시 사용 (TTL: {}초)", cache.getTtl());
      return cache.getData();
    }

    try {
      if (isTokenExpired()) {
        if (!renewToken()) {
          return null;
        }
      }

      String url = String.format("%s/v1/%s-database/static-creds/%s",
          config.getUrl(), config.getEntity(), config.getDatabaseStaticRoleId());

      HttpGet request = new HttpGet(url);
      request.setHeader("X-Vault-Token", authToken);

      if (config.getNamespace() != null && !config.getNamespace().isEmpty()) {
        request.setHeader("X-Vault-Namespace", config.getNamespace());
      }

      try (CloseableHttpResponse response = httpClient.execute(request)) {
        if (response.getCode() == 200) {
          JsonNode responseNode = objectMapper.readTree(response.getEntity().getContent());
          JsonNode dataNode = responseNode.get("data");

          if (dataNode != null) {
            int ttl = responseNode.get("ttl").asInt();

            // 캐시 업데이트
            secretCaches.put(cacheKey, new SecretCache(dataNode, ttl, true));
            logger.info("✅ Database Static 시크릿 조회 성공 (TTL: {}초)", ttl);
            return dataNode;
          }
        } else {
          logger.error("❌ Database Static 시크릿 조회 실패: HTTP {}", response.getCode());
        }
      }
    } catch (Exception e) {
      logger.error("❌ Database Static 시크릿 조회 중 오류 발생", e);
    }
    return null;
  }

  /**
   * 리소스 정리
   */
  public void close() {
    try {
      httpClient.close();
    } catch (IOException e) {
      logger.error("❌ HTTP 클라이언트 종료 중 오류 발생", e);
    }
  }

  /**
   * 시크릿 캐시 클래스
   */
  private static class SecretCache {
    private final JsonNode data;
    private final int version;
    private final int ttl;
    private final String leaseId;
    private final LocalDateTime timestamp;

    public SecretCache(JsonNode data, int version) {
      this.data = data;
      this.version = version;
      this.ttl = 0;
      this.leaseId = null;
      this.timestamp = LocalDateTime.now();
    }

    public SecretCache(JsonNode data, int ttl, boolean isTtl) {
      this.data = data;
      this.version = 0;
      this.ttl = ttl;
      this.leaseId = null;
      this.timestamp = LocalDateTime.now();
    }

    public SecretCache(JsonNode data, int ttl, String leaseId) {
      this.data = data;
      this.version = 0;
      this.ttl = ttl;
      this.leaseId = leaseId;
      this.timestamp = LocalDateTime.now();
    }

    public boolean isStale(int thresholdSeconds) {
      return LocalDateTime.now().isAfter(timestamp.plusSeconds(thresholdSeconds));
    }

    public JsonNode getData() {
      return data;
    }

    public int getVersion() {
      return version;
    }

    public int getTtl() {
      return ttl;
    }

    public String getLeaseId() {
      return leaseId;
    }
  }
}
