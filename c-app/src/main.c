#include "vault_client.h"
#include "config.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <signal.h>
#include <pthread.h>

// 전역 변수
vault_client_t vault_client;
app_config_t app_config;
volatile int should_exit = 0;

// 시그널 처리
void signal_handler(int sig) {
    printf("\nReceived signal %d. Shutting down...\n", sig);
    should_exit = 1;
    
    // 강제 종료를 위한 추가 시그널 설정
    if (sig == SIGINT) {
        signal(SIGINT, SIG_DFL); // 다음 Ctrl+C는 강제 종료
    }
}

// KV 시크릿 갱신 스레드
void* kv_refresh_thread(void* arg) {
    vault_client_t *client = (vault_client_t*)arg;
    
    while (!should_exit) {
        // 설정된 간격만큼 대기
        int refresh_interval = client->config->secret_kv.refresh_interval;
        for (int i = 0; i < refresh_interval && !should_exit; i++) {
            sleep(1);
        }
        
        if (should_exit) break;
        
        // KV 시크릿 갱신
        if (client->config->secret_kv.enabled) {
            printf("\n=== KV Secret Refresh ===\n");
            vault_refresh_kv_secret(client);
        }
    }
    
    printf("KV refresh thread terminated\n");
    return NULL;
}

// Database Dynamic 시크릿 갱신 스레드
void* db_dynamic_refresh_thread(void* arg) {
    vault_client_t *client = (vault_client_t*)arg;
    
    while (!should_exit) {
        // 설정된 간격만큼 대기
        int refresh_interval = client->config->secret_kv.refresh_interval;
        for (int i = 0; i < refresh_interval && !should_exit; i++) {
            sleep(1);
        }
        
        if (should_exit) break;
        
        // Database Dynamic 시크릿 갱신
        if (client->config->secret_database_dynamic.enabled) {
            printf("\n=== Database Dynamic Secret Refresh ===\n");
            vault_refresh_db_dynamic_secret(client);
        }
    }
    
    printf("Database Dynamic refresh thread terminated\n");
    return NULL;
}

// Database Static 시크릿 갱신 스레드
void* db_static_refresh_thread(void* arg) {
    vault_client_t *client = (vault_client_t*)arg;
    
    while (!should_exit) {
        // 설정된 간격만큼 대기 (Database Static은 자주 변경되지 않으므로 더 긴 간격)
        int refresh_interval = client->config->secret_kv.refresh_interval * 2; // 2배 간격
        for (int i = 0; i < refresh_interval && !should_exit; i++) {
            sleep(1);
        }
        
        if (should_exit) break;
        
        // Database Static 시크릿 갱신
        if (client->config->secret_database_static.enabled) {
            printf("\n=== Database Static Secret Refresh ===\n");
            vault_refresh_db_static_secret(client);
        }
    }
    
    printf("Database Static refresh thread terminated\n");
    return NULL;
}

// 토큰 갱신 스레드 (안전한 갱신 로직)
void* token_renewal_thread(void* arg) {
    vault_client_t *client = (vault_client_t*)arg;
    
    while (!should_exit) {
        // 10초마다 토큰 상태 확인 (짧은 TTL에 대응)
        for (int i = 0; i < 10 && !should_exit; i++) {
            sleep(1);
        }
        
        if (should_exit) break;
        
        // 토큰 상태 출력
        printf("\n=== Token Status Check ===\n");
        vault_print_token_status(client);
        
        // 갱신 필요 여부 확인 (4/5 지점에서 갱신)
        time_t now = time(NULL);
        time_t remaining = client->token_expiry - now;
        time_t total_ttl = client->token_expiry - client->token_issued;
        time_t elapsed = now - client->token_issued;
        time_t renewal_point = total_ttl * 4 / 5;  // 4/5 지점
        
        printf("Token check: elapsed=%ld, total_ttl=%ld, remaining=%ld, renewal_point=%ld\n", 
               elapsed, total_ttl, remaining, renewal_point);
        
        if (elapsed >= renewal_point) {  // 4/5 지점에서만 갱신
            printf("🔄 Token renewal triggered (at %ld%% of TTL, %ld seconds remaining)\n", 
                   (elapsed * 100) / total_ttl, remaining);
            
            if (vault_renew_token(client) != 0) {
                printf("❌ Token renewal failed. Attempting re-login...\n");
                if (vault_login(client, client->config->vault_role_id, client->config->vault_secret_id) != 0) {
                    fprintf(stderr, "❌ Re-login failed. Exiting...\n");
                    should_exit = 1;
                    break;
                } else {
                    printf("✅ Re-login successful\n");
                    vault_print_token_status(client);
                }
            } else {
                printf("✅ Token renewed successfully\n");
                vault_print_token_status(client);
            }
        } else {
            printf("✅ Token is still healthy, no renewal needed\n");
        }
    }
    
    return NULL;
}

int main(int argc, char *argv[]) {
    // 시그널 처리 설정
    signal(SIGINT, signal_handler);
    signal(SIGTERM, signal_handler);
    
    printf("=== Vault C Client Application ===\n");
    
    // 설정 파일 경로 결정
    const char *config_file = "config.ini";
    if (argc > 1) {
        config_file = argv[1];
    }
    
    // 설정 파일 로드
    printf("Loading configuration from: %s\n", config_file);
    if (load_config(config_file, &app_config) != 0) {
        fprintf(stderr, "Failed to load configuration\n");
        return 1;
    }
    
    // 설정 출력
    print_config(&app_config);
    
    // Vault 클라이언트 초기화
    if (vault_client_init(&vault_client, &app_config) != 0) {
        fprintf(stderr, "Failed to initialize Vault client\n");
        return 1;
    }
    
    // AppRole 로그인
    printf("Logging in to Vault...\n");
    if (vault_login(&vault_client, app_config.vault_role_id, app_config.vault_secret_id) != 0) {
        fprintf(stderr, "Login failed\n");
        vault_client_cleanup(&vault_client);
        return 1;
    }
    
    // 토큰 상태 출력
    vault_print_token_status(&vault_client);
    
    // 토큰 갱신 스레드 시작
    pthread_t renewal_thread;
    if (pthread_create(&renewal_thread, NULL, token_renewal_thread, &vault_client) != 0) {
        fprintf(stderr, "Failed to create renewal thread\n");
        vault_client_cleanup(&vault_client);
        return 1;
    }
    
    // KV 갱신 스레드 시작 (KV 엔진이 활성화된 경우)
    pthread_t kv_refresh_thread_handle = 0;
    if (app_config.secret_kv.enabled) {
        if (pthread_create(&kv_refresh_thread_handle, NULL, kv_refresh_thread, &vault_client) != 0) {
            fprintf(stderr, "Failed to create KV refresh thread\n");
            vault_client_cleanup(&vault_client);
            return 1;
        }
        printf("✅ KV refresh thread started (interval: %d seconds)\n", app_config.secret_kv.refresh_interval);
    }
    
    // Database Dynamic 갱신 스레드 시작 (Database Dynamic 엔진이 활성화된 경우)
    pthread_t db_dynamic_refresh_thread_handle = 0;
    if (app_config.secret_database_dynamic.enabled) {
        if (pthread_create(&db_dynamic_refresh_thread_handle, NULL, db_dynamic_refresh_thread, &vault_client) != 0) {
            fprintf(stderr, "Failed to create Database Dynamic refresh thread\n");
            vault_client_cleanup(&vault_client);
            return 1;
        }
        printf("✅ Database Dynamic refresh thread started (interval: %d seconds)\n", app_config.secret_kv.refresh_interval);
    }
    
    // Database Static 갱신 스레드 시작 (Database Static 엔진이 활성화된 경우)
    pthread_t db_static_refresh_thread_handle = 0;
    if (app_config.secret_database_static.enabled) {
        if (pthread_create(&db_static_refresh_thread_handle, NULL, db_static_refresh_thread, &vault_client) != 0) {
            fprintf(stderr, "Failed to create Database Static refresh thread\n");
            vault_client_cleanup(&vault_client);
            return 1;
        }
        printf("✅ Database Static refresh thread started (interval: %d seconds)\n", app_config.secret_kv.refresh_interval * 2);
    }
    
    // 메인 루프
    while (!should_exit) {
        printf("\n=== Fetching Secret ===\n");
        
        // KV 시크릿 가져오기 (캐시 확인)
        if (app_config.secret_kv.enabled) {
            json_object *kv_secret = NULL;
            if (vault_get_kv_secret(&vault_client, &kv_secret) == 0) {
                // data.data 부분만 추출하여 출력
                json_object *data_obj, *data_data;
                if (json_object_object_get_ex(kv_secret, "data", &data_obj) &&
                    json_object_object_get_ex(data_obj, "data", &data_data)) {
                    printf("📦 KV Secret Data (version: %d):\n%s\n", vault_client.kv_version, json_object_to_json_string(data_data));
                }
                vault_cleanup_secret(kv_secret);
            } else {
                fprintf(stderr, "Failed to retrieve KV secret\n");
            }
        }
        
        // Database Dynamic 시크릿 가져오기 (캐시 확인)
        if (app_config.secret_database_dynamic.enabled) {
            json_object *db_dynamic_secret = NULL;
            if (vault_get_db_dynamic_secret(&vault_client, &db_dynamic_secret) == 0) {
                // TTL 정보 가져오기
                time_t expire_time;
                int ttl = 0;
                if (vault_check_lease_status(&vault_client, vault_client.lease_id, &expire_time, &ttl) == 0) {
                    printf("🗄️ Database Dynamic Secret (TTL: %d seconds):\n", ttl);
                } else {
                    printf("🗄️ Database Dynamic Secret:\n");
                }
                
                // data 섹션에서 username과 password만 추출
                json_object *data_obj;
                if (json_object_object_get_ex(db_dynamic_secret, "data", &data_obj)) {
                    json_object *username_obj, *password_obj;
                    if (json_object_object_get_ex(data_obj, "username", &username_obj) &&
                        json_object_object_get_ex(data_obj, "password", &password_obj)) {
                        printf("  username: %s\n", json_object_get_string(username_obj));
                        printf("  password: %s\n", json_object_get_string(password_obj));
                    }
                }
                
                vault_cleanup_secret(db_dynamic_secret);
            } else {
                fprintf(stderr, "Failed to retrieve Database Dynamic secret\n");
            }
        }
        
        // Database Static 시크릿 가져오기 (캐시 확인)
        if (app_config.secret_database_static.enabled) {
            json_object *db_static_secret = NULL;
            if (vault_get_db_static_secret(&vault_client, &db_static_secret) == 0) {
                // TTL 정보 추출
                json_object *ttl_obj;
                int ttl = 0;
                if (json_object_object_get_ex(db_static_secret, "ttl", &ttl_obj)) {
                    ttl = json_object_get_int(ttl_obj);
                }
                
                if (ttl > 0) {
                    printf("🔒 Database Static Secret (TTL: %d seconds):\n", ttl);
                } else {
                    printf("🔒 Database Static Secret:\n");
                }
                
                // data 섹션에서 username과 password만 추출
                json_object *username_obj, *password_obj;
                if (json_object_object_get_ex(db_static_secret, "username", &username_obj) &&
                    json_object_object_get_ex(db_static_secret, "password", &password_obj)) {
                    printf("  username: %s\n", json_object_get_string(username_obj));
                    printf("  password: %s\n", json_object_get_string(password_obj));
                }
                
                vault_cleanup_secret(db_static_secret);
            } else {
                fprintf(stderr, "Failed to retrieve Database Static secret\n");
            }
        }
        
        // 토큰 상태 간단 출력
        printf("\n--- Token Status ---\n");
        vault_print_token_status(&vault_client);
        
        // 10초 대기
        for (int i = 0; i < 10 && !should_exit; i++) {
            sleep(1);
        }
    }
    
    // 정리
    printf("Cleaning up...\n");
    pthread_join(renewal_thread, NULL);
    
    // KV 갱신 스레드 정리
    if (kv_refresh_thread_handle != 0) {
        pthread_join(kv_refresh_thread_handle, NULL);
    }
    
    // Database Dynamic 갱신 스레드 정리
    if (db_dynamic_refresh_thread_handle != 0) {
        pthread_join(db_dynamic_refresh_thread_handle, NULL);
    }
    
    // Database Static 갱신 스레드 정리
    if (db_static_refresh_thread_handle != 0) {
        pthread_join(db_static_refresh_thread_handle, NULL);
    }
    
    vault_client_cleanup(&vault_client);
    
    printf("Application terminated\n");
    return 0;
}