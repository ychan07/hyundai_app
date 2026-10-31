# Pleos Connect API 정리 (개발자용)

현대자동차그룹 차량용 앱 개발 공모전([appdevchallenge.auto-hmg.io](https://appdevchallenge.auto-hmg.io/))의 가이드·FAQ·참가 규정·공지와 공식 개발 문서([document.pleos.ai](https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/))를 기준으로 정리했습니다. (2026-10-31 기준, SDK 릴리즈 2026-10-19)

## 1. 공모전 핵심 일정과 조건

- **접수**: 2026.10.21 ~ **11.15(일)**, 이메일 `appdevchallenge@hyundai-partners.com`
  - 제목: `[차량용 앱 개발 공모전] 팀명_대표자명`
- **필수 제출물**: 접수양식(기획서)
- **선택 제출물** (단계별 가산점): 시연 영상(목업·화면 녹화 인정), GitHub 저장소(소스 + APK)
- **일정**
  - 11/23: 1차 서류 결과 발표 (20팀)
  - **12/3~4: 2차 오프라인 PT** (현대자동차 강남대로사옥 1층 UX스튜디오 서울). 실제 동작하는 앱 시연 필수 (에뮬레이터 화면 인정)
  - 12/18: 최종 10팀 발표
  - 2027.02: 현대자동차그룹 행사 전시·시상 (최종 순위 발표)
- **심사 항목**: 차량특화성, 창의성, **기술 활용성(Pleos SDK·API를 핵심 기능에 적절히 활용했는가)**, 사업성(타깃 고객, 예상 MAU, Retention 등). 세부 배점 비공개.

## 2. 사용 가능한 API: Pleos Connect SDK 5종

- 개발 환경: Android(AAOS 기반), **minSdk 28 이상**, Gradle 8.0 이상, Kotlin 또는 Java
- 에뮬레이터: Pleos Connect Emulator (API 34 system image, RAM 4GB, Cold boot)
- Maven 저장소: `https://nexus-playground.pleos.ai/repository/maven-releases/`

| SDK (버전) | 기능 | 권한 | 비고 |
|---|---|---|---|
| **Vehicle** (2.3.2) | 차량 상태 조회·제어. Brake, CarInfo, Display, Door, DrivingMode, EvBattery, Fuel, HVAC, Light, Odometer, Safety, Seat, SideMirror, Steeringwheel, Tire, Toll, TurnSignal, Window, Wiper | `pleos.car.permission.CAR_*`(조회), `CONTROL_CAR_*`(제어) | 차종마다 지원 여부가 다름 → `checkXxxCapabilities`로 확인 |
| **NaviHelper** (3.0.5) | 목적지·경로·경유지 설정, 경로 옵션 변경, 재탐색, TBT·과속카메라·제한속도·충전소·Trip 정보 수신, 내비 화면 요청 | `NAVI_ROUTE`, `NAVI_ROUTE_SEARCH`, `NAVI_CUSTOM_MAP` | 내비 탑재 차량만. **경유지 최대 2개** |
| **TextToSpeech** (3.3.1) | 음성 합성 | `TTS_SERVICE` | 모드 HYBRID / SERVER / ON_DEVICE. locale: ko-KR, en-US, fr-CA, es-US, es-MX |
| **SpeechToText** (3.3.1) | 음성 인식 (텍스트 변환만, 명령 실행 X → Gleo Interface 참고) | `STT_SERVICE` | `sendAudio()`(PCM 직접 입력)는 **ON_DEVICE 모드 전용** |
| **FusedLocation** (1.1.3) | 차량 위치 1회 조회, 위치 업데이트, Proximity Alert(지오펜스) | `FUSED_LOCATION` (dangerous) | `isSupported()` 선확인. 위치값 null 가능 |

- 표준 데이터(속도, 기어, 외기온도, 연료 레벨 등)는 SDK가 아니라 **AAOS `CarPropertyManager`** 사용이 원칙 (AAOS Alignment 정책).
  - 클러스터와 같은 속도: `PERF_VEHICLE_SPEED` (Vehicle SDK의 `VehicleSpeedListener`는 삭제됨)
- 참고: Vehicle Data API(클라우드 REST, OAuth 2.0)와 Fleet API도 있지만 공모전 SDK 목록에는 없고, Vehicle Data API는 현재 유럽 지역 전용.

## 3. ⚠️ SDK 사용 상태 (가장 먼저 확인)

Playground "프로젝트 생성 및 SDK 사용 신청" 문서 기준:

- **신청 없이 바로 사용 가능**: **Vehicle State(조회), NaviHelper**
- **준비 중(Coming Soon), 신청 불가**: **FusedLocation, TextToSpeech, SpeechToText, Vehicle Control(제어)**

SDK 문서·라이브러리는 공개돼 있지만, 기획서 핵심 기능을 TTS/STT·차량 제어에 걸면 위험할 수 있음 → 설명회 다시보기 확인 또는 사무국에 이메일 문의 필요.

- TTS/STT 서버 모드(HYBRID, SERVER)는 Playground 프로젝트의 Client ID / Client Secret으로 `registerApp()` 필요. ON_DEVICE는 등록 불필요.

## 4. 호출 횟수 제한 (Rate limit)

**공개 문서에는 "초당/일일 N회" 같은 숫자 제한이 없음.** (SDK 문서 전체, Vehicle Data API 문서에서 rate limit / 429 / quota / 호출 제한 키워드 검색 결과 없음. SDK는 로컬 IPC 방식)

대신 아래가 사실상의 제약:

### 성능 기준 (앱 심사 가이드)

- CPU: 주행 중 일반 앱 **5% 미만**, 내비 앱 15% 이하, 정차 중 모든 앱 25% 이하
- RAM: **400MB 이하**
- 앱 실행·콘텐츠 로드 10초 이내, 버튼 반응 2초 이내(넘으면 로딩 UI), 터치 응답 0.25초 이내
- "불필요한 타이머·폴링 최소화" 명시 → get 반복 호출 대신 **register 리스너(Callback)로 구독**

### SDK 사용 패턴

- Vehicle get/set/capability 결과는 비동기(`onComplete` / `onFailed`). 콜백 완료 전 `release()` 금지.
- NaviHelper `requestRoute()` 성공 ≠ 탐색 완료. 결과는 이벤트(`onRoutingCompleted`, `onError`)로 확인. 정보 조회 API도 값이 콜백으로 옴.
- STT 스트리밍: 16kHz/16bit mono 기준 100ms마다 3,200B, 마지막에 `completeAudioSend()` 필수.
- 오류: `ERROR_DRIVING_RESTRICTED`(주행 중 제한), `ERROR_SECURITY_PERMISSION`, `ERROR_NOT_SUPPORTED_*` 등 처리. TTS/STT `ErrorType`은 값이 추가될 수 있으니 `when`에 else 분기 필수.

## 5. 개발 시 주의사항

1. **주행 중 제한(CarUxRestrictions)**
   - Distraction Optimization 미선언 앱은 **주행 중 사용 불가**
   - 한국(그외 지역) 주행 중 금지: 동영상, 미디어 앱 로그인/설정/검색, 차량 설정, 대용량 텍스트
   - 동영상·게임·브라우저 앱은 정차 중에만
   - 차량특화성 측면에서 **주행 중엔 음성 중심 UX**가 유리
2. **차량 모델별 차이**: 모델명 분기 금지. Capability / `isSupported()`로 판단하고 미지원 기능은 UI 비활성화.
3. **Vehicle 권한**: App Market 입점 시 사전 협의·승인된 퍼미션만 선언 가능. 필요한 것만 선언.
4. **개인정보**: 위치·음성 수집 시 명시적 동의 절차, 로그의 개인정보 마스킹, 차량/사용자 ID 익명화 정책.
5. **버전 변경 주의 (10/19 릴리즈)**
   - NaviHelper 데이터 모델 생성자 **좌표 순서가 (latitude, longitude)로 변경** (문서 Java 예제 일부는 예전 순서)
   - NaviHelper `onNavigationStatus` 인자 타입 변경, `totalDistance/totalTime` → `remainDistance/remainTime`, `onRouteEnded` → `onRoutingCompleted`
   - TTS/STT `onError` 인자 String → `ErrorType`
   - Vehicle: CruiseControl 상수, `checkHvacFanSpeedCapabilities` 반환 타입, `FanDirectionType` 순서 변경
6. **에뮬레이터 vs 실차**: `Build.DEVICE`·코덱 지원이 다름. 2차 시연 전 Virtual Console로 차량 상태를 바꿔가며 리허설.

## 6. 제출·규정 체크리스트

- **GitHub**
  - APK: `apk/팀명_앱이름_release.apk` (release 빌드). 용량 초과 시 GitHub Releases
  - README: 앱 이름·한 줄 소개, 실행 방법, 테스트 환경·설정, APK 위치
  - 비공개 저장소: 심사용 계정을 Collaborator로 초대 (개인 계정은 **쓰기 권한도 부여됨**), 마감 전 접근 확인
- **수정 제출**: 제목에 `[수정제출]`, 전체 자료 + 서명 동의서 재첨부, 팀원 전원 CC. 마감 전 마지막 제출본이 최종.
- **IP**: 저작권은 응모자. 수상작은 주최측 통상실시권 가능, 발표일로부터 4개월 우선협상권.
- **기타**: 상금은 대표자 일괄 지급(기타소득세 원천징수), 타 공모전 수상작·표절작 제외, 최대 5인 팀, 현대차그룹 임직원은 수상 제외.

## 결론

지금 확실히 쓸 수 있는 건 **Vehicle(조회) + NaviHelper**. 이 둘을 핵심 기능으로 잡고, TTS/STT·FusedLocation·차량 제어는 사용 가능 여부를 사무국에 확인한 뒤 붙이는 게 안전함.

## 참고 링크

- 공모전 개발 환경: https://appdevchallenge.auto-hmg.io/guide/development
- 공모전 FAQ: https://appdevchallenge.auto-hmg.io/faq
- SDK 개요: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/
- SDK 사용 신청: https://document.pleos.ai/docs/pleos-only/create-my-project
- SDK 릴리즈 노트: https://document.pleos.ai/docs/pleos-only/release-note/sdk-release-note
- 앱 심사 가이드(성능): https://document.pleos.ai/docs/pleos-only/distribute/app-review-guide/review-functionality-performance-stability
- 성능·리소스 설계: https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/performance-resource-mgmt
- 주행 규제 정책: https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy
