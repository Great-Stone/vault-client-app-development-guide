package com.example.vault;

import com.example.vault.config.VaultConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * VaultApplication 테스트 클래스
 */
class VaultApplicationTest {

  @Test
  void testVaultConfigCreation() {
    // VaultConfig 객체 생성 테스트
    VaultConfig config = new VaultConfig();

    assertNotNull(config);
    assertEquals(5, config.getKvRefreshInterval());
    assertEquals(30, config.getHttpTimeout());
    assertEquals(4096, config.getMaxResponseSize());
  }

  @Test
  void testVaultConfigFromProperties() {
    // Properties에서 VaultConfig 생성 테스트
    java.util.Properties props = new java.util.Properties();
    props.setProperty("vault.entity", "test-entity");
    props.setProperty("vault.url", "http://localhost:8200");
    props.setProperty("vault.role_id", "test-role-id");
    props.setProperty("vault.secret_id", "test-secret-id");
    props.setProperty("secret.kv.enabled", "true");
    props.setProperty("secret.kv.path", "test-path");
    props.setProperty("secret.database.dynamic.enabled", "true");
    props.setProperty("secret.database.dynamic.role_id", "test-db-role");

    VaultConfig config = VaultConfig.fromProperties(props);

    assertNotNull(config);
    assertEquals("test-entity", config.getEntity());
    assertEquals("http://localhost:8200", config.getUrl());
    assertEquals("test-role-id", config.getRoleId());
    assertEquals("test-secret-id", config.getSecretId());
    assertTrue(config.isKvEnabled());
    assertEquals("test-path", config.getKvPath());
    assertTrue(config.isDatabaseDynamicEnabled());
    assertEquals("test-db-role", config.getDatabaseDynamicRoleId());
  }
}
