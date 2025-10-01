package com.example.vault.config;

import java.util.Properties;

/**
 * Vault 클라이언트 설정을 관리하는 클래스
 */
public class VaultConfig {
  private String entity;
  private String url;
  private String namespace;
  private String roleId;
  private String secretId;

  private boolean kvEnabled;
  private String kvPath;
  private int kvRefreshInterval;

  private boolean databaseDynamicEnabled;
  private String databaseDynamicRoleId;

  private boolean databaseStaticEnabled;
  private String databaseStaticRoleId;

  private int httpTimeout;
  private int maxResponseSize;

  public VaultConfig() {
    // 기본값 설정
    this.kvRefreshInterval = 5;
    this.httpTimeout = 30;
    this.maxResponseSize = 4096;
  }

  public static VaultConfig fromProperties(Properties props) {
    VaultConfig config = new VaultConfig();

    // 시스템 프로퍼티 우선, 없으면 Properties 파일 값 사용
    config.setEntity(getProperty("vault.entity", props.getProperty("vault.entity", "")));
    config.setUrl(getProperty("vault.url", props.getProperty("vault.url", "http://127.0.0.1:8200")));
    config.setNamespace(getProperty("vault.namespace", props.getProperty("vault.namespace", "")));
    config.setRoleId(getProperty("vault.role_id", props.getProperty("vault.role_id", "")));
    config.setSecretId(getProperty("vault.secret_id", props.getProperty("vault.secret_id", "")));

    config.setKvEnabled(
        Boolean.parseBoolean(getProperty("secret.kv.enabled", props.getProperty("secret.kv.enabled", "false"))));
    config.setKvPath(getProperty("secret.kv.path", props.getProperty("secret.kv.path", "")));
    config.setKvRefreshInterval(Integer
        .parseInt(getProperty("secret.kv.refresh_interval", props.getProperty("secret.kv.refresh_interval", "5"))));

    config.setDatabaseDynamicEnabled(Boolean.parseBoolean(
        getProperty("secret.database.dynamic.enabled", props.getProperty("secret.database.dynamic.enabled", "false"))));
    config.setDatabaseDynamicRoleId(
        getProperty("secret.database.dynamic.role_id", props.getProperty("secret.database.dynamic.role_id", "")));

    config.setDatabaseStaticEnabled(Boolean.parseBoolean(
        getProperty("secret.database.static.enabled", props.getProperty("secret.database.static.enabled", "false"))));
    config.setDatabaseStaticRoleId(
        getProperty("secret.database.static.role_id", props.getProperty("secret.database.static.role_id", "")));

    config.setHttpTimeout(Integer.parseInt(getProperty("http.timeout", props.getProperty("http.timeout", "30"))));
    config.setMaxResponseSize(
        Integer.parseInt(getProperty("http.max_response_size", props.getProperty("http.max_response_size", "4096"))));

    return config;
  }

  /**
   * 시스템 프로퍼티를 우선적으로 확인하고, 없으면 기본값을 반환
   */
  private static String getProperty(String key, String defaultValue) {
    String systemValue = System.getProperty(key);
    return systemValue != null ? systemValue : defaultValue;
  }

  // Getters and Setters
  public String getEntity() {
    return entity;
  }

  public void setEntity(String entity) {
    this.entity = entity;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getNamespace() {
    return namespace;
  }

  public void setNamespace(String namespace) {
    this.namespace = namespace;
  }

  public String getRoleId() {
    return roleId;
  }

  public void setRoleId(String roleId) {
    this.roleId = roleId;
  }

  public String getSecretId() {
    return secretId;
  }

  public void setSecretId(String secretId) {
    this.secretId = secretId;
  }

  public boolean isKvEnabled() {
    return kvEnabled;
  }

  public void setKvEnabled(boolean kvEnabled) {
    this.kvEnabled = kvEnabled;
  }

  public String getKvPath() {
    return kvPath;
  }

  public void setKvPath(String kvPath) {
    this.kvPath = kvPath;
  }

  public int getKvRefreshInterval() {
    return kvRefreshInterval;
  }

  public void setKvRefreshInterval(int kvRefreshInterval) {
    this.kvRefreshInterval = kvRefreshInterval;
  }

  public boolean isDatabaseDynamicEnabled() {
    return databaseDynamicEnabled;
  }

  public void setDatabaseDynamicEnabled(boolean databaseDynamicEnabled) {
    this.databaseDynamicEnabled = databaseDynamicEnabled;
  }

  public String getDatabaseDynamicRoleId() {
    return databaseDynamicRoleId;
  }

  public void setDatabaseDynamicRoleId(String databaseDynamicRoleId) {
    this.databaseDynamicRoleId = databaseDynamicRoleId;
  }

  public boolean isDatabaseStaticEnabled() {
    return databaseStaticEnabled;
  }

  public void setDatabaseStaticEnabled(boolean databaseStaticEnabled) {
    this.databaseStaticEnabled = databaseStaticEnabled;
  }

  public String getDatabaseStaticRoleId() {
    return databaseStaticRoleId;
  }

  public void setDatabaseStaticRoleId(String databaseStaticRoleId) {
    this.databaseStaticRoleId = databaseStaticRoleId;
  }

  public int getHttpTimeout() {
    return httpTimeout;
  }

  public void setHttpTimeout(int httpTimeout) {
    this.httpTimeout = httpTimeout;
  }

  public int getMaxResponseSize() {
    return maxResponseSize;
  }

  public void setMaxResponseSize(int maxResponseSize) {
    this.maxResponseSize = maxResponseSize;
  }
}
