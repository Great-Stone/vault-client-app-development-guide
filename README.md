# Vault 개발 가이드

## 📖 개요

이 가이드는 Vault를 활용하여 애플리케이션에서 시크릿을 안전하게 관리하는 방법을 다룹니다. 개발자들이 Vault API를 직접 연계하여 시크릿을 동적으로 가져오고, 자동 갱신하는 방법을 개발하기 위한 예시를 제공합니다.

## 🗂️ 디렉토리 구조

```
Development-Guide/
├── README.md                           # 이 파일 (전체 가이드 개요)
├── 01.개요.md                          # Vault 연동 개발 개요 및 방법론
├── 02.동작시퀀스.md                     # 상세한 동작 시퀀스 및 구현 방법
├── 03.Vault개발서버.md                 # Vault 개발 서버 설정 가이드
├── setup-vault-for-my-vault-app.sh     # Vault 서버 자동 설정 스크립트
├── c-app/                              # C언어 Vault 클라이언트 예제
│   ├── README.md                       # C언어 예제 사용법
│   ├── src/                            # 소스 코드
│   │   ├── main.c                      # 메인 애플리케이션
│   │   ├── vault_client.c              # Vault 클라이언트 구현
│   │   ├── vault_client.h              # Vault 클라이언트 헤더
│   │   └── config.c                    # 설정 관리
│   ├── config.ini                      # 설정 파일
│   ├── config.h                        # 설정 헤더
│   └── Makefile                        # 빌드 설정
└── pure-java-app/                      # Java Vault 클라이언트 예제
    ├── README.md                       # Java 예제 사용법
    ├── src/main/java/com/example/vault/ # Java 소스 코드
    │   ├── VaultApplication.java        # 메인 애플리케이션
    │   ├── client/VaultClient.java      # Vault 클라이언트
    │   └── config/VaultConfig.java      # 설정 관리
    ├── src/main/resources/             # 리소스 파일
    │   ├── config.properties            # 설정 파일
    │   └── logback.xml                  # 로깅 설정
    └── pom.xml                          # Maven 설정
```

## 📚 문서 가이드

### 1. [개요 및 방법론](./01.개요.md)
- **대상**: 모든 개발자
- **내용**: Vault 연동의 기본 개념과 3가지 접근 방법
- **핵심**: API 직접 연계, Vault Proxy, Vault Agent 비교

### 2. [동작 시퀀스](./02.동작시퀀스.md)
- **대상**: 구현 담당 개발자
- **내용**: 상세한 인증, 토큰 갱신, 시크릿 조회 과정
- **핵심**: 실제 구현 시 필요한 모든 단계별 설명

### 3. [Vault 개발 서버](./03.Vault개발서버.md)
- **대상**: Vault 관리자 및 개발자
- **내용**: 개발 환경에서 Vault 서버 설정 방법
- **핵심**: AppRole, Entity, 시크릿 엔진 설정

## 🛠️ 코드 예제

### C언어 예제 ([c-app/](./c-app/))
- **언어**: C (libcurl + json-c)
- **특징**: 
  - KV v2, Database Dynamic, Database Static 시크릿 엔진 지원
  - 실시간 갱신, 버전 기반 캐싱, TTL 기반 갱신
  - Entity 기반 권한, 자동 토큰 갱신
- **빌드**: `make` 명령어로 간단 빌드
- **실행**: `./vault-app` 실행

### Java 예제 ([pure-java-app/](./pure-java-app/))
- **언어**: Java 11+ (Apache HttpClient + Jackson)
- **특징**:
  - Maven 기반 프로젝트 구조
  - 실시간 TTL 계산, 멀티스레드 갱신
  - 시스템 프로퍼티 오버라이드 지원
- **빌드**: `mvn clean package`
- **실행**: `java -jar target/vault-java-app.jar`

## 🚀 빠른 시작

### 1. Vault 서버 설정
```bash
# Vault 개발 서버 자동 설정
./setup-vault-for-my-vault-app.sh
```

### 2. 예제 실행
```bash
# C언어 예제
cd c-app
make
./vault-app

# Java 예제
cd pure-java-app
mvn clean package
java -jar target/vault-java-app.jar
```
