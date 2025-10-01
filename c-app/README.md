# 🔧 C언어 Vault 클라이언트 애플리케이션

`vault-app`은 C언어로 구현된 Vault 클라이언트 애플리케이션입니다. KV, Database Dynamic, Database Static 시크릿 엔진을 지원하며, 실시간 시크릿 갱신과 캐싱 기능을 제공합니다.

- 예제에서는 KV, Database Dynamic, Database Static 시크릿을 이용하는 예제를 제공합니다.
- 애플리케이션 초기 구동에만 필요한 경우 처음 한번만 API 호출하고 나면 이후 구동시 캐시를 활용하여 메모리 사용을 줄입니다.
- 예제에서는 주기적으로 계속 시크릿을 가져와 갱신하도록 구현되어 있습니다.

## ✨ 주요 기능

- **🔐 다중 시크릿 엔진 지원**: KV v2, Database Dynamic, Database Static
- **⚡ 실시간 갱신**: 백그라운드 스레드를 통한 자동 시크릿 갱신
- **💾 효율적 캐싱**: 버전 기반 KV 캐싱, TTL 기반 Database 캐싱
- **🔄 자동 토큰 갱신**: 4/5 지점에서 자동 토큰 갱신
- **📊 메타데이터 표시**: 버전, TTL 등 유용한 정보 제공
- **🛡️ 보안**: Entity 기반 권한 관리 및 안전한 메모리 처리

## 🏗️ 프로젝트 구조

```
c-app/
├── src/
│   ├── main.c              # 메인 애플리케이션 및 스레드 관리
│   ├── vault_client.h      # Vault 클라이언트 헤더
│   ├── vault_client.c      # Vault 클라이언트 구현
│   └── config.c            # INI 파일 파싱
├── config.h                # 설정 구조체 정의
├── config.ini              # 애플리케이션 설정 파일
├── Makefile                # 빌드 스크립트
└── README.md               # 이 파일
```

## 🚀 빠른 시작

### 1. 필수 라이브러리 설치

**macOS (Homebrew)**:
```bash
brew install curl json-c
```

**Ubuntu/Debian**:
```bash
sudo apt-get install libcurl4-openssl-dev libjson-c-dev
```

**CentOS/RHEL**:
```bash
sudo yum install libcurl-devel json-c-devel
```

### 2. 빌드 및 실행

```bash
# 빌드
make clean && make

# 실행
./vault-app

# 설정 파일 지정 실행
./vault-app custom-config.ini
```

### 3. 설정 파일 구성

`config.ini` 파일을 수정하여 Vault 연결 정보를 설정합니다:

```ini
[vault]
entity = my-vault-app
url = http://127.0.0.1:8200
namespace = 
role_id = your-role-id-here
secret_id = your-secret-id-here

[secret-kv]
enabled = true
kv_path = database
refresh_interval = 5

[secret-database-dynamic]
enabled = true
role_id = db-demo-dynamic

[secret-database-static]
enabled = true
role_id = db-demo-static

[http]
timeout = 30
max_response_size = 4096
```

## 📋 출력 예시

```
=== Vault C Client Application ===
Loading configuration from: config.ini
=== Application Configuration ===
Vault URL: http://127.0.0.1:8200
Vault Namespace: (empty)
Entity: my-vault-app
Vault Role ID: 7fb49dd0-4b87-19cd-7b72-a7e21e5c543e
Vault Secret ID: 475a6500-f9f8-fdd4-ec30-54fadcad926e

--- Secret Engines ---
KV Engine: enabled
  KV Path: database
  Refresh Interval: 5 seconds
Database Dynamic: enabled
  Role ID: db-demo-dynamic
Database Static: enabled
  Role ID: db-demo-static

--- HTTP Settings ---
HTTP Timeout: 30 seconds
Max Response Size: 4096 bytes
=====================================
Logging in to Vault...
Token TTL from Vault: 60 seconds
Login successful. Token expires in 60 seconds
Token status: 60 seconds remaining (expires in 1 minutes)
✅ Token is healthy (at 0% of TTL)
✅ KV refresh thread started (interval: 5 seconds)
✅ Database Dynamic refresh thread started (interval: 5 seconds)
✅ Database Static refresh thread started (interval: 10 seconds)

=== Fetching Secret ===
📦 KV Secret Data (version: 10):
{ "api_key": "myapp-api-key-123456", "database_url": "postgresql://myapp:securepass@localhost:5432/myappdb" }

🗄️ Database Dynamic Secret (TTL: 59 seconds):
  username: v-approle-db-demo-dy-0x50Hgcj5Mj
  password: AdCNFYg6wDV6p8fz-byK

🔒 Database Static Secret (TTL: 2412 seconds):
  username: my-vault-app-static
  password: sntZ-lhR2rZ9GLjgGvry

--- Token Status ---
Token status: 60 seconds remaining (expires in 1 minutes)
✅ Token is healthy (at 0% of TTL)
```

## 🔧 설정 옵션

### Vault 설정 (`[vault]`)
- `entity`: Entity 이름 (필수)
- `url`: Vault 서버 주소
- `namespace`: Vault 네임스페이스 (선택사항)
- `role_id`: AppRole Role ID
- `secret_id`: AppRole Secret ID

### KV 시크릿 설정 (`[secret-kv]`)
- `enabled`: KV 엔진 활성화 여부
- `kv_path`: KV 시크릿 경로
- `refresh_interval`: 갱신 간격 (초)

### Database Dynamic 설정 (`[secret-database-dynamic]`)
- `enabled`: Database Dynamic 엔진 활성화 여부
- `role_id`: Database Dynamic Role ID

### Database Static 설정 (`[secret-database-static]`)
- `enabled`: Database Static 엔진 활성화 여부
- `role_id`: Database Static Role ID

### HTTP 설정 (`[http]`)
- `timeout`: HTTP 요청 타임아웃 (초)
- `max_response_size`: 최대 응답 크기 (바이트)

## 🏗️ 아키텍처

### 스레드 구조
- **메인 스레드**: 시크릿 조회 및 출력
- **토큰 갱신 스레드**: 10초마다 토큰 상태 확인, 4/5 지점에서 갱신
- **KV 갱신 스레드**: 설정된 간격마다 KV 시크릿 갱신
- **Database Dynamic 갱신 스레드**: 설정된 간격마다 Dynamic 시크릿 갱신
- **Database Static 갱신 스레드**: 2배 간격으로 Static 시크릿 갱신

### 캐싱 전략
- **KV 시크릿**: 버전 기반 캐싱 (버전 변경 시에만 갱신)
- **Database Dynamic**: TTL 기반 캐싱 (10초 이하 시 갱신)
- **Database Static**: 시간 기반 캐싱 (5분마다 갱신)

### 보안 기능
- **Entity 기반 권한**: `{entity}-{engine}` 경로 패턴 사용
- **자동 토큰 갱신**: 토큰 만료 전 자동 갱신
- **메모리 보안**: 시크릿 데이터 사용 후 즉시 정리
- **에러 처리**: 네트워크 오류, 토큰 만료 시 재시도

## 🔍 개발자 가이드

### 핵심 구현 포인트

**1. 메모리 관리**
```c
// ✅ 올바른 방법: CURL 핸들 생성 및 정리
CURL *curl = curl_easy_init();
// ... 요청 처리 ...
curl_easy_cleanup(curl);

// ✅ JSON 객체 참조 카운트 관리
*secret_data = json_object_get(data_obj);
// ... 사용 후 ...
vault_cleanup_secret(secret_data);
```

**2. 토큰 갱신 로직**
```c
// 4/5 지점에서 갱신 (토큰 TTL의 80% 경과 시)
time_t renewal_point = total_ttl * 4 / 5;
if (elapsed >= renewal_point) {
    vault_renew_token(client);
}
```

**3. 에러 처리**
```c
// 토큰 갱신 실패 시 재로그인
if (vault_renew_token(client) != 0) {
    if (vault_login(client, role_id, secret_id) != 0) {
        should_exit = 1;  // 재로그인도 실패하면 종료
    }
}
```

### 주요 함수

**Vault 클라이언트 함수**
- `vault_client_init()`: 클라이언트 초기화
- `vault_login()`: AppRole 로그인
- `vault_renew_token()`: 토큰 갱신
- `vault_get_kv_secret()`: KV 시크릿 조회
- `vault_get_db_dynamic_secret()`: Database Dynamic 시크릿 조회
- `vault_get_db_static_secret()`: Database Static 시크릿 조회

**캐시 관리 함수**
- `vault_refresh_kv_secret()`: KV 시크릿 갱신
- `vault_refresh_db_dynamic_secret()`: Database Dynamic 시크릿 갱신
- `vault_refresh_db_static_secret()`: Database Static 시크릿 갱신

## 🐛 문제 해결

### 빌드 오류
- **라이브러리 누락**: `brew install curl json-c` (macOS)
- **경로 문제**: Makefile의 include 경로 확인
- **권한 문제**: 실행 파일에 실행 권한 부여

### 실행 오류
- **Vault 연결 실패**: Vault 서버 상태 및 URL 확인
- **인증 실패**: Role ID, Secret ID 유효성 확인
- **권한 오류**: Entity 정책 및 경로 권한 확인

### 성능 최적화
- **메모리 사용량**: 불필요한 시크릿 갱신 방지
- **네트워크 호출**: 캐싱 전략 최적화
- **스레드 관리**: 적절한 갱신 간격 설정

## 📚 참고 자료

- [Vault API 문서](https://developer.hashicorp.com/vault/api-docs)
- [Database Secrets Engine](https://developer.hashicorp.com/vault/api-docs/secret/databases)
- [KV Secrets Engine](https://developer.hashicorp.com/vault/api-docs/secret/kv)
- [AppRole Auth Method](https://developer.hashicorp.com/vault/api-docs/auth/approle)