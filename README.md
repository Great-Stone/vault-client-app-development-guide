# Vault Development Guide

[GO TO KOREAN - 한국어 설명으로 이동](./README_KO.md)

## 📖 Overview

This guide covers how to securely manage secrets in applications using Vault. It provides examples for developers on how to dynamically fetch and automatically renew secrets by directly integrating with the Vault API.

### Key Scenarios
- **AppRole Authentication**: Secure Vault access via Role ID + Secret ID.
- **Automatic Secret Injection**: Automatic secret management via Spring Cloud Vault Config.
- **Automatic Token Renewal**: Spring Cloud Vault handles token renewal automatically.
- **Web UI Provision**: Visualization of secret information using Thymeleaf.
- **Real-time Updates**: Automatic secret renewal via @RefreshScope.
- **Database Integration**: Database connection and statistics retrieval using Vault Dynamic Secrets.

### Mandatory Operational Requirements

#### Authentication
- The C, C++, Java, Python, Spring Boot, and Tomcat examples use AppRole authentication.
- After AppRole authentication, the app receives a token from Vault.
- The token has specific permissions and an expiration time, similar to a web session.
- The token must be renewed before it expires, much like a browser session timeout resets on activity.
- If renewal fails, access to Vault is denied.
- If you use a different authentication method than the examples, only the initial login changes; token management is the same.
  - Examples include AWS IAM, Azure AD, Google OAuth, TLS certificate, and Kubernetes service account authentication.
  - Reference: <https://developer.hashicorp.com/vault/api-docs/auth>

#### Secrets
- Even for KV secrets (static values), design the app so it can pick up changes made in Vault.
- For Dynamic, Static Role, PKI, and other TTL-backed secrets, renew or replace them before they expire.
- If you use secret engines other than those in the examples, the REST API usage pattern is the same.
  - Reference: <https://developer.hashicorp.com/vault/api-docs/secret>

### Token Renewal Exceptions

- Spring Cloud Vault Config, used in the Spring Boot example, handles token renewal automatically.
- For scripts, where token renewal is difficult to implement, Vault Proxy is used as a substitute.

## 🗂️ Directory Structure

```
Development-Guide/
├── README.md                           # This file (Overall Guide Overview)
├── README_KO.md                        # Korean version of this guide
├── documents/                          # Documentation
│   ├── English/                        # English documentation
│   │   ├── 01.introduction.md          # Vault Integration Development Overview and Methodology
│   │   ├── 02.sequence.md              # Detailed Operational Sequence and Implementation
│   │   └── 03.run_vault_dev.md         # Vault Development Server Setup Guide
│   └── Korean/                         # Korean documentation
├── samples/                            # Application samples
│   ├── c-app/                          # C Vault Client Example
│   ├── cpp-app/                        # C++ Vault Client Example
│   ├── java-pure-app/                  # Java Vault Client Example
│   ├── python-app/                     # Python Vault Client Example
│   ├── java-web-springboot-app/        # Spring Boot Web Application Example
│   ├── java-web-tomcat-app/            # Tomcat Web Application Example
│   ├── pure-java-app/                  # Pure Java Application Example
│   └── script-sample/                 # Vault Proxy Integration Script Examples
│       ├── README.md                   # Script Usage Guide
│       ├── get_kv_secret_proxy.sh      # Get KV Secret (Proxy)
│       ├── get_db_dynamic_secret_proxy.sh # Get Database Dynamic Secret (Proxy)
│       ├── get_db_static_secret_proxy.sh # Get Database Static Secret (Proxy)
│       ├── get_ssh_otp_proxy.sh        # Generate SSH OTP (Proxy)
│       ├── get_ssh_signed_cert_proxy.sh # Generate SSH Signed Certificate (Proxy)
│       ├── get_ssh_kv_proxy.sh         # Get SSH KV Credentials (Proxy)
│       ├── get_aws_userpass.sh         # Get AWS Credentials (Userpass)
│       ├── get_aws_oidc.sh             # Get AWS Credentials (OIDC)
│       └── vault-proxy-demo/           # Vault Proxy Demo Environment
│           ├── README.md               # Vault Proxy Usage
│           ├── vault-proxy.hcl         # Vault Proxy Configuration File
│           ├── setup-token.sh          # Token File Creation Script
│           ├── start-proxy.sh          # Proxy Start Script
│           └── stop-proxy.sh           # Proxy Stop Script
```

## 📚 Document Guide

### 1. [Overview and Methodology](./documents/English/01.introduction.md)
- **Target**: All Developers
- **Content**: Basic concepts of Vault integration and three different approaches.
- **Core**: Comparison of Direct API Integration, Vault Proxy, and Vault Agent.

### 2. [Operational Sequence](./documents/English/02.sequence.md)
- **Target**: Implementation Developers
- **Content**: Detailed processes for authentication, token renewal, and secret retrieval.
- **Core**: Step-by-step explanation of all stages required for actual implementation.

### 3. [Vault Development Server](./documents/English/03.run_vault_dev.md)
- **Target**: Vault Administrators and Developers
- **Content**: How to set up a Vault server in a development environment.
- **Core**: Configuration of AppRole, Entities, and Secret Engines.

## 🛠️ Code Examples

### C Example ([samples/c-app/](./samples/c-app/))
- **Language**: C (libcurl + json-c)
- **Features**:
  - Supports KV v2, Database Dynamic, and Database Static secret engines.
  - Real-time renewal, version-based caching, TTL-based renewal.
  - Entity-based permissions, automatic token renewal.
- **Build**: Simple build with `make` command.
- **Run**: Execute `./vault-app`.

### C++ Example ([samples/cpp-app/](./samples/cpp-app/))
- **Language**: C++17 (libcurl + nlohmann/json)
- **Features**:
  - Modern C++17 style, RAII pattern, smart pointers.
  - CMake-based cross-platform build.
  - Thread safety (std::mutex, std::atomic).
  - Same features and configuration compatibility as the C version.
- **Build**: `mkdir build && cd build && cmake .. && make`
- **Run**: `./vault-app`

### Java Example ([samples/java-pure-app/](./samples/java-pure-app/))
- **Language**: Java 11+ (Apache HttpClient + Jackson)
- **Features**:
  - Maven-based project structure.
  - Real-time TTL calculation, multi-threaded renewal.
  - System property override support.
- **Build**: `mvn clean package`
- **Run**: `java -jar target/vault-java-app.jar`

### Python Example ([samples/python-app/](./samples/python-app/))
- **Language**: Python 3.7+ (hvac + threading)
- **Features**:
  - Uses hvac 2.3.0, a dedicated Vault library.
  - Virtual environment-based package management.
  - Environment variable override support.
  - Real-time TTL calculation, multi-threaded renewal.
- **Install**: `pip install -r requirements.txt`
- **Run**: `python vault_app.py`

### Spring Boot Web Example ([samples/java-web-springboot-app/](./samples/java-web-springboot-app/))
- **Language**: Java 11+ (Spring Boot + Spring Cloud Vault Config)
- **Features**:
  - Vault access via AppRole authentication.
  - Automatic Token Renewal by Spring Cloud Vault.
  - Automatic injection of Database Dynamic Secrets via Spring Cloud Vault Config.
  - Web UI provided through Thymeleaf.
  - Real-time secret updates via @RefreshScope.
  - Direct Vault API calls to fetch KV and Static Secrets.
  - MySQL integration and display of database statistics.
  - **Credential Source Selection**: Choose between KV, Dynamic, and Static (testing complete).
- **Build**: `./gradlew build`
- **Run**: `./gradlew bootRun`
- **Web Access**: `http://localhost:8080/vault-web`

### Tomcat Web Example ([samples/java-web-tomcat-app/](./samples/java-web-tomcat-app/))
- **Language**: Java 11+ (Servlet + JSP + Apache Commons DBCP2)
- **Features**:
  - Traditional Java Web Application (Servlet + JSP).
  - Vault access via AppRole authentication.
  - Token Auto-Renewal (checks every 10s, renews at 80% of TTL).
  - Automatic renewal of Apache Commons DBCP2 Connection Pool.
  - Web UI provided through JSP + JSTL.
  - MySQL integration and display of database statistics.
  - **Credential Source Selection**: Choose between KV, Dynamic, and Static.
- **Build**: `mvn clean package`
- **Run**: Deploy WAR to Tomcat 10.
- **Web Access**: `http://localhost:8080/vault-tomcat-app`

## 🔌 Vault API Types and Usage

This section introduces the main Vault APIs used in this guide.

### 1. **Authentication APIs**

#### AppRole Authentication
- **Endpoint**: `POST /v1/auth/approle/login`
- **Usage**: Allows an application to log in to Vault and obtain a client token.
- **Request Data**: `role_id`, `secret_id`
- **Response**: `client_token`, `lease_duration`

```json
{
  "role_id": "your-role-id",
  "secret_id": "your-secret-id"
}
```

#### Token Renewal
- **Endpoint**: `POST /v1/auth/token/renew-self`
- **Usage**: Renews a client token before it expires.
- **Header**: `X-Vault-Token: <client_token>`
- **Response**: New `lease_duration`

### 2. **Secret Engine APIs**

#### KV v2 Secrets Engine
- **Endpoint**: `GET /v1/{mount-path}/data/{secret-path}`
- **Usage**: Stores and retrieves static key-value secrets.
- **Features**: Versioning, includes metadata.
- **Response Structure**:
```json
{
  "data": {
    "data": {
      "username": "dbuser",
      "password": "secretpassword"
    },
    "metadata": {
      "version": 1,
      "created_time": "2024-01-01T00:00:00Z"
    }
  }
}
```

#### Database Dynamic Secrets Engine
- **Endpoint**: `GET /v1/{mount-path}/creds/{role-name}`
- **Usage**: Generates temporary, dynamically created database credentials.
- **Features**: TTL-based automatic expiration, Lease ID management.
- **Response Structure**:
```json
{
  "data": {
    "username": "v-approle-db-demo-dy-abc123",
    "password": "xyz789"
  },
  "lease_id": "lease-abc123",
  "lease_duration": 3600
}
```

#### Database Static Secrets Engine
- **Endpoint**: `GET /v1/{mount-path}/static-creds/{role-name}`
- **Usage**: Manages statically defined database credentials.
- **Features**: Manual renewal, long-term validity.
- **Response Structure**:
```json
{
  "data": {
    "username": "myapp-static-user",
    "password": "static-password"
  },
  "ttl": 3600
}
```

### 3. **Lease Management APIs**

#### Check Lease Status
- **Endpoint**: `GET /v1/sys/leases/lookup`
- **Usage**: Checks the status and TTL of a specific lease.
- **Header**: `X-Vault-Token: <client_token>`
- **Request Data**: `lease_id`

#### Renew Lease
- **Endpoint**: `PUT /v1/sys/leases/renew`
- **Usage**: Renews the lease of a Database Dynamic secret.
- **Header**: `X-Vault-Token: <client_token>`
- **Request Data**: `lease_id`, `increment` (optional)

### 4. **System APIs**

#### Check Vault Health
- **Endpoint**: `GET /v1/sys/health`
- **Usage**: Checks the health and availability of the Vault server.
- **Response**: Server status, version information.

#### Look Up Token Information
- **Endpoint**: `GET /v1/auth/token/lookup-self`
- **Usage**: Checks the information and expiration time of the current token.
- **Header**: `X-Vault-Token: <client_token>`

## 🚀 Quick Start

### 1. Set Up Vault Server
```bash
# Automatically set up the Vault development server
./setup-vault-for-my-vault-app.sh
```

### 2. Run Examples

#### Programming Language Examples
```bash
# C Example
cd samples/c-app
make
./vault-app

# C++ Example
cd samples/cpp-app
mkdir build && cd build
cmake ..
make
./vault-app

# Java Example
cd samples/java-pure-app
mvn clean package
java -jar target/vault-java-app.jar

# Python Example
cd samples/python-app
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
python vault_app.py

# Spring Boot Web Example
cd samples/java-web-springboot-app
./gradlew bootRun
# Access in web browser: http://localhost:8080/vault-web

# Tomcat Web Example
cd samples/java-web-tomcat-app
mvn clean package
cp target/vault-tomcat-app.war tomcat/webapps/
cd tomcat && bin/catalina.sh start
# Access in web browser: http://localhost:8080/vault-tomcat-app
```

#### Script Examples (with Vault Proxy)
```bash
# Set up and run Vault Proxy
cd samples/script-sample/vault-proxy-demo
./setup-token.sh
./start-proxy.sh

# Move to the parent directory and run scripts
cd ..
./get_kv_secret_proxy.sh
./get_db_dynamic_secret_proxy.sh
./get_db_static_secret_proxy.sh
./get_ssh_otp_proxy.sh
./get_ssh_signed_cert_proxy.sh
./get_ssh_kv_proxy.sh

# Stop Vault Proxy
cd vault-proxy-demo
./stop-proxy.sh
```

### Script Examples (samples/script-sample/)
- **Bash Scripts**: Direct Vault API calls and Vault Proxy integration.
- **Supported Secrets**: KV, Database Dynamic/Static, SSH (KV/OTP/Signed Certificate).
- **Features**: No Vault CLI required, uses pure curl, supports JSON parsing.