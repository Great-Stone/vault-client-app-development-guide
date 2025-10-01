# Vault Java 클라이언트 애플리케이션

## 📖 예제 목적 및 사용 시나리오

이 예제는 Vault 연동 개발을 위한 참고용 애플리케이션입니다.
애플리케이션 초기 구동에만 필요한 경우 처음 한번만 API 호출하고 나면 이후 구동시 캐시를 활용하여 메모리 사용을 줄입니다.
예제에서는 주기적으로 계속 시크릿을 가져와 갱신하도록 구현되어 있습니다.
Vault만을 위한 라이브러리로 구현하지 않고 API를 직접 호출하는 방식으로 설계되었습니다.

### 🎯 주요 시나리오
- **초기 구동**: 애플리케이션 시작 시 Vault에서 시크릿을 한 번만 조회
- **실시간 갱신**: 주기적으로 시크릿을 갱신하여 최신 상태 유지
- **캐시 활용**: 버전/TTL 기반 캐싱으로 불필요한 API 호출 최소화

### 🔐 지원 시크릿 타입
- **KV v2**: 키-값 저장소 (버전 기반 캐싱)
- **Database Dynamic**: 동적 데이터베이스 자격증명 (TTL 기반 갱신)
- **Database Static**: 정적 데이터베이스 자격증명 (시간 기반 캐싱)

### 💡 개발 고려사항
- **메모리 관리**: 시크릿 캐싱으로 메모리 사용량 최적화
- **오류 처리**: 네트워크 오류, 인증 실패 등 예외 상황 처리
- **보안**: 토큰 갱신, 시크릿 암호화 등 보안 요구사항 충족
- **성능**: 비동기 처리, 연결 풀링 등 성능 최적화

## 🚀 빠른 시작

### 1. 사전 요구사항
- Java 11 이상에서 테스트
- Maven 3.6 이상
- Vault 서버 (개발 서버 설정 완료)

### 2. 빌드 및 실행

```bash
# 프로젝트 빌드
mvn clean package

# 애플리케이션 실행
java -jar target/vault-java-app.jar

# 또는 Maven으로 직접 실행
mvn exec:java -Dexec.mainClass="com.example.vault.VaultApplication"
```

### 3. 설정 파일 수정
`src/main/resources/config.properties` 파일을 수정하여 Vault 연결 설정을 변경할 수 있습니다.

## 📋 출력 예제

```
🚀 Vault Java 클라이언트 애플리케이션 시작
✅ Vault 로그인 성공 (TTL: 60초)
✅ KV 시크릿 갱신 스케줄러 시작 (간격: 5초)
✅ Database Dynamic 시크릿 갱신 스케줄러 시작 (간격: 5초)
✅ Database Static 시크릿 갱신 스케줄러 시작 (간격: 10초)

📖 예제 목적 및 사용 시나리오
이 예제는 Vault 연동 개발을 위한 참고용 애플리케이션입니다.
애플리케이션 초기 구동에만 필요한 경우 처음 한번만 API 호출하고 나면 이후 구동시 캐시를 활용하여 메모리 사용을 줄입니다.
예제에서는 주기적으로 계속 시크릿을 가져와 갱신하도록 구현되어 있습니다.

🔧 지원 기능:
- KV v2 시크릿 엔진 (버전 기반 캐싱)
- Database Dynamic 시크릿 엔진 (TTL 기반 갱신)
- Database Static 시크릿 엔진 (시간 기반 캐싱)
- 자동 토큰 갱신
- Entity 기반 권한 관리

⚙️ 현재 설정:
- Entity: my-vault-app
- Vault URL: http://127.0.0.1:8200
- KV Enabled: true
- Database Dynamic Enabled: true
- Database Static Enabled: true

🔄 시크릿 갱신 시작... (Ctrl+C로 종료)

=== KV Secret Refresh ===
✅ KV 시크릿 조회 성공 (버전: 11)
📦 KV Secret Data (version: 11):
{"api_key":"myapp-api-key-123456","database_url":"postgresql://myapp:securepass@localhost:5432/myappdb"}

=== Database Dynamic Secret Refresh ===
✅ Database Dynamic 시크릿 조회 성공 (TTL: 60초)
🗄️ Database Dynamic Secret (TTL: 60초):
  username: v-approle-db-demo-dy-JRHTDBobE5o
  password: qLteLnVHZdBcmR-sJS1b

=== Database Static Secret Refresh ===
✅ Database Static 시크릿 조회 성공 (TTL: 3600초)
🔒 Database Static Secret (TTL: 3600초):
  username: my-vault-app-static
  password: OfK6S-6R2PiWA0C8Fqxj

=== Database Dynamic Secret Refresh ===
✅ Database Dynamic 시크릿 캐시 사용 (TTL: 60초)
🗄️ Database Dynamic Secret (TTL: 56초):
  username: v-approle-db-demo-dy-JRHTDBobE5o
  password: qLteLnVHZdBcmR-sJS1b
```

## ⚙️ 설정 옵션

### 설정 우선순위

일반적으로 사전 정의되는 구성은 `config.properties` 파일에 정의됩니다. Vault 인증을 위해 사용되는 `secret_id`의 경우 발급 시간 이후 만료가되므로, 실행 시 적용할 수 있도록 시스템 프로퍼티로 오버라이드 할 수 있도록 구현되었습니다.

1. **시스템 프로퍼티** (`-D` 옵션) - 최우선
2. **config.properties 파일** - 기본값

### 시스템 프로퍼티 사용법
```bash
# 실행 시 시스템 프로퍼티로 설정 오버라이드
java -Dvault.role_id=your-role-id \
     -Dvault.secret_id=your-secret-id \
     -Dvault.url=http://your-vault-server:8200 \
     -jar target/vault-java-app.jar

# 또는 개별 설정
java -Dvault.secret_id=3ee5080b-c9b3-2714-799c-f8d45a715625 -jar target/vault-java-app.jar
```

### Vault 서버 설정
```properties
# Entity 이름 (필수)
vault.entity=my-vault-app
# Vault 서버 주소
vault.url=http://127.0.0.1:8200
# Vault 네임스페이스 (선택사항)
vault.namespace=
# AppRole 인증 정보 (필수)
vault.role_id=7fb49dd0-4b87-19cd-7b72-a7e21e5c543e
vault.secret_id=475a6500-f9f8-fdd4-ec30-54fadcad926e
```

### 시크릿 엔진 설정
```properties
# KV Secret 설정
secret.kv.enabled=true
secret.kv.path=database
secret.kv.refresh_interval=5

# Database Dynamic Secret 설정
secret.database.dynamic.enabled=true
secret.database.dynamic.role_id=db-demo-dynamic

# Database Static Secret 설정
secret.database.static.enabled=true
secret.database.static.role_id=db-demo-static
```

### HTTP 설정
```properties
# HTTP 요청 타임아웃 (초)
http.timeout=30
# 최대 응답 크기 (바이트)
http.max_response_size=4096
```

## 🏗️ 아키텍처

### 클래스 구조
```
com.example.vault/
├── VaultApplication.java          # 메인 애플리케이션
├── config/
│   └── VaultConfig.java           # 설정 관리
└── client/
    └── VaultClient.java           # Vault 클라이언트
```

### 주요 컴포넌트
- **VaultApplication**: 메인 애플리케이션 로직, 스케줄러 관리
- **VaultConfig**: 설정 파일 로드 및 관리
- **VaultClient**: Vault API 연동, 시크릿 조회, 캐싱

### 캐싱 전략
- **KV v2**: 버전 기반 캐싱 (버전 변경 시에만 갱신)
- **Database Dynamic**: TTL 기반 캐싱 (10초 임계값)
- **Database Static**: 시간 기반 캐싱 (5분 간격)

### 실시간 TTL 계산
- Database Dynamic/Static Secret의 TTL이 실시간으로 감소하는 것을 표시
- 캐시된 TTL에서 경과 시간을 빼서 현재 남은 TTL 계산
- `Math.max(0, remainingTtl)`로 음수 방지

## 🛠️ 개발자 가이드

### 1. 프로젝트 구조 이해
```
src/main/java/com/example/vault/
├── VaultApplication.java          # 메인 애플리케이션
├── config/VaultConfig.java        # 설정 클래스
└── client/VaultClient.java        # Vault 클라이언트

src/main/resources/
└── config.properties              # 설정 파일
```

### 2. 주요 기능 구현
- **인증**: AppRole 기반 Vault 인증
- **토큰 관리**: 자동 토큰 갱신
- **시크릿 조회**: KV, Database Dynamic/Static 시크릿 조회
- **캐싱**: 효율적인 시크릿 캐싱 전략
- **TTL 관리**: 실시간 TTL 계산 및 표시

### 3. 확장 가능한 구조
- 새로운 시크릿 엔진 추가
- 커스텀 캐싱 전략 구현
- 모니터링 및 로깅 강화

## 🔧 빌드 및 실행

```bash
mvn clean package
java -jar target/vault-java-app.jar
```

## 🐛 문제 해결

1. **Vault 연결 실패**: URL, 네임스페이스 확인
2. **인증 실패**: Role ID, Secret ID 확인
3. **권한 오류**: Entity 정책 확인
4. **시크릿 조회 실패**: 시크릿 엔진 활성화 확인

## 📚 참고 자료

- [Vault API 문서](https://www.vaultproject.io/api-docs)
- [AppRole 인증 방법](https://www.vaultproject.io/docs/auth/approle)
- [KV v2 시크릿 엔진](https://www.vaultproject.io/docs/secrets/kv/kv-v2)
- [Database 시크릿 엔진](https://www.vaultproject.io/docs/secrets/databases)
