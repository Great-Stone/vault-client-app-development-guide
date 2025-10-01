package com.example.vault;

import com.example.vault.client.VaultClient;
import com.example.vault.config.VaultConfig;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Vault Java 클라이언트 애플리케이션
 * 
 * 이 애플리케이션은 Vault의 KV v2, Database Dynamic, Database Static 시크릿 엔진을 지원합니다.
 * 개발사에게 배포되어 Vault 연동 개발 시 참고용으로 사용됩니다.
 */
public class VaultApplication {
  private static final Logger logger = LoggerFactory.getLogger(VaultApplication.class);

  private VaultClient vaultClient;
  private ScheduledExecutorService scheduler;
  private volatile boolean running = true;

  public static void main(String[] args) {
    VaultApplication app = new VaultApplication();

    // 종료 시그널 처리
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      logger.info("🛑 애플리케이션 종료 중...");
      app.shutdown();
    }));

    try {
      app.run();
    } catch (Exception e) {
      logger.error("❌ 애플리케이션 실행 중 오류 발생", e);
      System.exit(1);
    }
  }

  public void run() throws Exception {
    logger.info("🚀 Vault Java 클라이언트 애플리케이션 시작");

    // 설정 로드
    VaultConfig config = loadConfig();
    if (config == null) {
      logger.error("❌ 설정 로드 실패");
      return;
    }

    // Vault 클라이언트 초기화
    vaultClient = new VaultClient(config);

    // Vault 로그인
    if (!vaultClient.login()) {
      logger.error("❌ Vault 로그인 실패");
      return;
    }

    // 스케줄러 초기화 (KV, Database Dynamic, Database Static, Token Renewal)
    scheduler = Executors.newScheduledThreadPool(4);

    // 시크릿 갱신 스케줄러 시작
    startSecretRefreshSchedulers(config);

    // 메인 루프
    runMainLoop(config);
  }

  private VaultConfig loadConfig() {
    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("config.properties")) {
      if (inputStream == null) {
        logger.error("❌ config.properties 파일을 찾을 수 없습니다");
        return null;
      }

      Properties props = new Properties();
      props.load(inputStream);

      return VaultConfig.fromProperties(props);
    } catch (IOException e) {
      logger.error("❌ 설정 파일 로드 중 오류 발생", e);
      return null;
    }
  }

  private void startSecretRefreshSchedulers(VaultConfig config) {
    // KV 시크릿 갱신 스케줄러
    if (config.isKvEnabled()) {
      scheduler.scheduleAtFixedRate(() -> {
        if (running) {
          logger.info("\n=== KV Secret Refresh ===");
          JsonNode kvSecret = vaultClient.getKvSecret();
          if (kvSecret != null) {
            int version = vaultClient.getKvSecretVersion();
            if (version > 0) {
              logger.info("📦 KV Secret Data (version: {}):\n{}", version, kvSecret.toString());
            } else {
              logger.info("📦 KV Secret Data: {}", kvSecret.toString());
            }
          }
        }
      }, 0, config.getKvRefreshInterval(), TimeUnit.SECONDS);

      logger.info("✅ KV 시크릿 갱신 스케줄러 시작 (간격: {}초)", config.getKvRefreshInterval());
    }

    // Database Dynamic 시크릿 갱신 스케줄러
    if (config.isDatabaseDynamicEnabled()) {
      scheduler.scheduleAtFixedRate(() -> {
        if (running) {
          logger.info("\n=== Database Dynamic Secret Refresh ===");
          JsonNode dbDynamicSecret = vaultClient.getDatabaseDynamicSecret();
          if (dbDynamicSecret != null) {
            JsonNode username = dbDynamicSecret.get("username");
            JsonNode password = dbDynamicSecret.get("password");
            if (username != null && password != null) {
              int ttl = vaultClient.getDatabaseDynamicSecretTtl();
              if (ttl > 0) {
                logger.info("🗄️ Database Dynamic Secret (TTL: {}초):\n  username: {}\n  password: {}",
                    ttl, username.asText(), password.asText());
              } else {
                logger.info("🗄️ Database Dynamic Secret:\n  username: {}\n  password: {}",
                    username.asText(), password.asText());
              }
            }
          }
        }
      }, 0, config.getKvRefreshInterval(), TimeUnit.SECONDS);

      logger.info("✅ Database Dynamic 시크릿 갱신 스케줄러 시작 (간격: {}초)", config.getKvRefreshInterval());
    }

    // Database Static 시크릿 갱신 스케줄러
    if (config.isDatabaseStaticEnabled()) {
      scheduler.scheduleAtFixedRate(() -> {
        if (running) {
          logger.info("\n=== Database Static Secret Refresh ===");
          JsonNode dbStaticSecret = vaultClient.getDatabaseStaticSecret();
          if (dbStaticSecret != null) {
            JsonNode username = dbStaticSecret.get("username");
            JsonNode password = dbStaticSecret.get("password");
            if (username != null && password != null) {
              int ttl = vaultClient.getDatabaseStaticSecretTtl();
              if (ttl > 0) {
                logger.info("🔒 Database Static Secret (TTL: {}초):\n  username: {}\n  password: {}",
                    ttl, username.asText(), password.asText());
              } else {
                logger.info("🔒 Database Static Secret:\n  username: {}\n  password: {}",
                    username.asText(), password.asText());
              }
            }
          }
        }
      }, 0, config.getKvRefreshInterval() * 2, TimeUnit.SECONDS);

      logger.info("✅ Database Static 시크릿 갱신 스케줄러 시작 (간격: {}초)", config.getKvRefreshInterval() * 2);
    }
  }

  private void runMainLoop(VaultConfig config) {
    // 애플리케이션 정보를 하나의 메시지로 구성
    StringBuilder appInfo = new StringBuilder();
    appInfo.append("📖 예제 목적 및 사용 시나리오\n");
    appInfo.append("이 예제는 Vault 연동 개발을 위한 참고용 애플리케이션입니다.\n");
    appInfo.append("애플리케이션 초기 구동에만 필요한 경우 처음 한번만 API 호출하고 나면 이후 구동시 캐시를 활용하여 메모리 사용을 줄입니다.\n");
    appInfo.append("예제에서는 주기적으로 계속 시크릿을 가져와 갱신하도록 구현되어 있습니다.\n\n");
    appInfo.append("🔧 지원 기능:\n");
    appInfo.append("- KV v2 시크릿 엔진 (버전 기반 캐싱)\n");
    appInfo.append("- Database Dynamic 시크릿 엔진 (TTL 기반 갱신)\n");
    appInfo.append("- Database Static 시크릿 엔진 (시간 기반 캐싱)\n");
    appInfo.append("- 자동 토큰 갱신\n");
    appInfo.append("- Entity 기반 권한 관리\n\n");
    appInfo.append("⚙️ 현재 설정:\n");
    appInfo.append("- Entity: ").append(config.getEntity()).append("\n");
    appInfo.append("- Vault URL: ").append(config.getUrl()).append("\n");
    appInfo.append("- KV Enabled: ").append(config.isKvEnabled()).append("\n");
    appInfo.append("- Database Dynamic Enabled: ").append(config.isDatabaseDynamicEnabled()).append("\n");
    appInfo.append("- Database Static Enabled: ").append(config.isDatabaseStaticEnabled()).append("\n\n");
    appInfo.append("🔄 시크릿 갱신 시작... (Ctrl+C로 종료)");

    logger.info(appInfo.toString());

    try {
      while (running) {
        Thread.sleep(1000);
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private void shutdown() {
    running = false;

    if (scheduler != null) {
      scheduler.shutdown();
      try {
        if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
          scheduler.shutdownNow();
        }
      } catch (InterruptedException e) {
        scheduler.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }

    if (vaultClient != null) {
      vaultClient.close();
    }

    logger.info("✅ 애플리케이션 종료 완료");
  }
}