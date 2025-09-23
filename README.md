# 📈 Investory - 투자 정보 플랫폼

> 실시간 주식 정보와 커뮤니티 기능을 제공하는 종합 투자 플랫폼

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.12-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![License](https://img.shields.io/badge/license-MIT-blue.svg)]()

## 🚀 프로젝트 소개

Investory는 투자자들을 위한 종합 정보 플랫폼으로, 실시간 주식 데이터 조회, 투자 정보 공유, 그리고 커뮤니티 기능을 제공합니다.

### ✨ 주요 기능

- 📊 **실시간 주식 정보**: WebSocket을 통한 실시간 주가 데이터 스트리밍
- 👥 **사용자 관리**: OAuth2 기반 소셜 로그인 및 JWT 인증
- 💬 **커뮤니티**: 게시글 작성, 댓글, 좋아요 기능
- 🔐 **보안**: Spring Security를 통한 강력한 보안 체계
- 📱 **RESTful API**: Swagger UI를 통한 API 문서화

## 🛠 기술 스택

### Backend
- **Framework**: Spring Boot 3.3.12
- **Language**: Java 21
- **Database**: Oracle DB, H2 (개발용)
- **Security**: Spring Security, OAuth2, JWT
- **API Documentation**: Swagger (SpringDoc OpenAPI)
- **Build Tool**: Gradle

### 주요 라이브러리
- **WebSocket**: 실시간 데이터 통신
- **WebFlux**: 비동기 웹 처리
- **MyBatis**: SQL 매핑 프레임워크
- **Lombok**: 코드 간소화
- **RxJava**: 리액티브 프로그래밍

## 📁 프로젝트 구조

```
src/
├── main/java/com/project/stock/investory/
│   ├── admin/          # 관리자 기능
│   ├── comment/        # 댓글 기능
│   ├── post/           # 게시글 기능
│   ├── stockInfo/      # 주식 정보 관리
│   ├── user/           # 사용자 관리
│   └── config/         # 설정 파일들
└── test/               # 테스트 코드
```

## 🚀 시작하기

### 사전 요구사항
- Java 21 이상
- Gradle 8.x
- Oracle Database (또는 H2 for development)

### 설치 및 실행

1. **프로젝트 클론**
   ```bash
   git clone https://github.com/your-org/investory.git
   cd investory
   ```

2. **환경 설정**
   ```bash
   # application.properties 또는 application.yml 설정
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   ```

3. **빌드 및 실행**
   ```bash
   ./gradlew build
   ./gradlew bootRun
   ```

4. **API 문서 확인**
   - 애플리케이션 실행 후 `http://localhost:8080/swagger-ui.html` 접속

## 📋 API 문서

Swagger UI를 통해 상세한 API 문서를 확인할 수 있습니다.

- **개발 환경**: `http://localhost:8080/swagger-ui.html`
- **운영 환경**: `https://your-domain.com/swagger-ui.html`

## 🧪 테스트

```bash
# 전체 테스트 실행
./gradlew test

# 특정 테스트 실행
./gradlew test --tests "com.project.stock.investory.*"
```

## 🤝 팀원 소개

### 👨‍💻 개발팀 (4명)

| 이름 | 역할 | 담당 업무 | GitHub                                     |
|------|------|-----------|--------------------------------------------|
| **임희진** | Team Leader & Backend | 프로젝트 총괄, 주식 정보 API 개발 | [@xeexin](https://github.com/xeexin)       |
| **팀원 2** | Backend Developer | 사용자 인증, 보안 시스템 개발 | [@username2](https://github.com/username2) |
| **팀원 3** | Backend Developer | 커뮤니티 기능, 게시판 개발 | [@username3](https://github.com/username3) |
| **팀원 4** | Backend Developer | 실시간 데이터 처리, WebSocket 개발 | [@username4](https://github.com/username4) |

### 🎯 개별 기여도

#### 임희진 (Team Leader)
- [ ] WebSocket 실시간 통신
- [ ] 주식 데이터 스트리밍
- [ ] 비동기 처리 최적화
- [ ] 성능 모니터링

#### 팀원 2 ()


#### 팀원 3 ()


#### 팀원 4 ()


## 🚀 배포

### 개발 환경
- **URL**: `http://localhost:8080`
- **Database**: H2 인메모리 DB

### 운영 환경
- **URL**: `https://investory-prod.com`
- **Database**: Oracle Cloud DB
- **서버**: AWS EC2


<div align="center">
  <sub>Built with ❤️ by Investory Team</sub>
</div>
