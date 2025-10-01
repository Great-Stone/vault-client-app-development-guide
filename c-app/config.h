#ifndef CONFIG_H
#define CONFIG_H

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

// 설정 구조체
typedef struct {
    // Vault 기본 설정
    char vault_url[256];
    char vault_namespace[64];
    char vault_role_id[128];
    char vault_secret_id[128];
    char entity[64];
    
    // 시크릿 엔진 설정
    struct {
        int enabled;
        char kv_path[128];
        int refresh_interval;  // KV 갱신 간격 (초)
    } secret_kv;
    
    struct {
        int enabled;
        char role_id[128];
    } secret_database_dynamic;
    
    struct {
        int enabled;
        char role_id[128];
    } secret_database_static;
    
    // HTTP 설정
    int http_timeout;
    int max_response_size;
} app_config_t;

// 기본값 정의
#define DEFAULT_VAULT_URL "http://127.0.0.1:8200"
#define DEFAULT_VAULT_NAMESPACE ""
#define DEFAULT_ENTITY "my-vault-app"
#define DEFAULT_HTTP_TIMEOUT 30
#define DEFAULT_MAX_RESPONSE_SIZE 4096
#define DEFAULT_KV_REFRESH_INTERVAL 300  // 5분 기본값

// 함수 선언
int load_config(const char *config_file, app_config_t *config);
void print_config(const app_config_t *config);

#endif