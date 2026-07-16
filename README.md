# 🍽️ 8282 주문 관리 플랫폼

Spring Boot 기반의 음식 주문 관리 플랫폼입니다.

고객은 메뉴를 주문하고 결제를 진행할 수 있으며,  
가게 사장은 주문과 메뉴를 관리하고  
관리자는 지역과 카테고리, 가게를 관리할 수 있습니다.

도메인 중심 설계와 역할 기반 권한 관리를 적용하여 실제 배달 서비스의 주문 흐름을 구현했습니다.

---

# 프로젝트 소개

* **프로젝트명** : 8282 주문 관리 플랫폼
* **개발 기간** : 2026.07.02 ~ 2026.07.20
* **개발 인원** : 6명
* **개발 형태** : Backend Team Project 
* **아키텍처** : Monolithic Architecture

### 주요 기능

* JWT 기반 로그인 및 권한 관리
* 음식점 및 메뉴 관리
* 주문 및 결제 처리
* 주문 상태 변경 및 상태 이력 관리
* 리뷰 및 리뷰 답글 관리
* AI 메뉴 설명 생성

---

# 프로젝트 개요

실제 배달 플랫폼의 주문 과정을 기반으로 구현한 프로젝트입니다.

고객은 원하는 가게의 메뉴와 옵션을 선택하여 주문 및 결제를 진행할 수 있으며,

가게 사장은 주문을 수락하고 조리·배달 상태를 변경할 수 있습니다.

관리자는 지역, 카테고리, 가게 승인 등 서비스 운영 기능을 담당합니다.

또한 주문과 결제를 하나의 비즈니스 흐름으로 연결하고, 주문 상태 변경 이력을 저장하여 변경 과정을 추적할 수 있도록 구현했습니다.

---

# 프로젝트 구조

```text
src
├── global
│   ├── common
│   ├── config
│   ├── exception
│   └── security
│
├── user
│   ├── application
│   ├── domain
│   └── presentation
├── region
├── category
├── store
├── menu
├── optiongroup
├── option
├── order
├── payment
├── review
├── reviewreply
└── aihistory
```

각 도메인은 **Presentation → Application → Domain** 구조로 구성하여 도메인 간의 책임을 분리했습니다.

---

# 기술 스택

### Backend

| 기술              | 버전                          |
| --------------- |-----------------------------|
| Java            | 17                          |
| Spring Boot     | 3.5.3                       |
| Spring Security | 6.5.1 (Spring Boot Managed) |
| Spring Data JPA | 3.5.1 (Spring Boot Managed) |
| QueryDSL        | 5.1.0                       |
| JWT (jjwt)      | 0.12.3                      |
| Lombok          | 1.18.38 (Latest)            |

### Database

| 기술              |
| --------------- |
| PostgreSQL      |
| Hibernate (JPA) |

### Test

| 기술               |
| ---------------- |
| JUnit5           |
| Mockito          |
| AssertJ          |
| Spring Boot Test |

### DevOps & Collaboration

| 기술             |
| -------------- |
| Git            |
| GitHub         |
| GitHub Actions |
| Docker         |
| AWS EC2        |
| AWS RDS        |
| Notion         |
| Postman        |

---

#  도메인별 주요 기능

## User

* 회원가입 및 로그인
* JWT 기반 인증
* 사용자 정보 관리
* 권한(Role) 관리

---

## Region

* 지역 CRUD
* 지역 검색
* 활성 여부 관리

---

## Category

* 카테고리 CRUD
* 카테고리 중복 검증
* 관리자 권한 관리

---

## Store

* 가게 등록 및 승인
* 가게 정보 수정
* 최소 주문 금액 관리
* 배달비 정책 관리
* 영업 상태 관리

---

## Menu

* 메뉴 CRUD
* 메뉴 검색 및 페이징
* 판매 상태 관리
* AI 메뉴 설명 적용

---

## Option Group / Option

* 옵션 그룹 관리
* 메뉴 옵션 관리
* 옵션 추가 금액 관리

---

## Order

* 주문 생성 및 조회
* 주문 취소
* 주문 상태 변경
* 주문 상태 이력 관리
* Store / Menu / Payment 연동

---

## Payment

* 결제 생성
* 결제 취소 및 환불
* 주문과 결제 상태 연동
* Idempotency-Key 기반 중복 결제 방지

---

## Review

* 리뷰 작성 및 수정
* 리뷰 삭제
* 별점 관리

---

## Review Reply

* 사장 답글 작성
* 답글 수정 및 삭제

---

## AI History

* AI 메뉴 설명 생성
* AI 생성 이력 저장
* 메뉴 설명 즉시 적용

---

# 역할 분담

| 이름   | 담당 도메인                  |
|------|-------------------------|
| 박수연  | User                    |
| 홍태규  | Store                   |
| 송국희  | Menu / Category / Region |
| 김정석  | Order / OrderItem       |
| 윤찬영  | Payment / CICD / AWS    |
| 박영재  | Review / AI             |

---

#  회고

### 잘한 점

* 도메인 중심으로 패키지를 분리하여 유지보수성 향상
* GitHub Issue, Pull Request 기반 코드 리뷰를 통해 협업
* 단위 테스트를 작성하여 주요 비즈니스 로직 검증

### 아쉬운 점

* 프로젝트 기간이 짧아 충분한 코드 리뷰 부족
* AWS 배포 서버에서 실사용 시나리오 API 테스트 부족
* Swagger 미적용으로 API 명세 확인과 테스트 어려움 존재
* 통합 테스트가 부족하여 전체 시나리오 검증 부족

### 배운 점

이번 프로젝트를 통해 단순 CRUD 구현을 넘어 여러 도메인이 하나의 비즈니스 흐름으로 연결되는 과정을 경험했습니다.

객체의 역할과 책임을 고려한 설계와 Git Flow 기반 협업, 코드 리뷰의 중요성을 배울 수 있었습니다.
