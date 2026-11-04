# Route League (가칭)

같은 출퇴근길을 다니는 운전자들이 차량 연비·전비 기록으로 경로별 리그에 참여하는 Pleos Connect 차량용 앱.
현대자동차그룹 차량용 앱 개발 공모전 출품작. 기획 문서는 [`planning/`](planning/) 참고.

## 기술 스택
- Android Automotive (Pleos Connect), Kotlin 2.0, Jetpack Compose, Hilt, Room, WorkManager
- Pleos Connect SDK: **Vehicle 2.3.2** (Fuel/EvBattery 효율, Gear), **NaviHelper 3.0.5** (위치·제한속도)
- AAOS `CarPropertyManager`(PERF_VEHICLE_SPEED, INFO_FUEL_TYPE), `CarUxRestrictionsManager`
- 서버: Supabase (`backend/`)

## 모듈 구조
```
app/                 진입점, 내비게이션, DI, 주행 중 화면 잠금, 동기화 Worker
core/domain/         순수 Kotlin: 경로 일치·기록 자격 판정, 주간 순위(중앙값), 미션/배지  ← 단위 테스트
core/vehicle/        VehicleGateway 인터페이스
  ├─ pleos/          실제 구현 (Pleos SDK + AAOS)
  └─ fake/           시나리오 재생 구현 (정상/과속/데이터 누락) — 에뮬레이터 시연용
core/data/           Room 로컬 기록(원시 샘플 30일), Repository, RemoteSyncApi
core/ui/             차량 디스플레이용 테마(주/야간, 큰 글씨), 공통 컴포넌트
feature/record/      주행 자동 기록 Foreground Service + DriveRecorder
feature/result/      주차 후 결과·미션
feature/league/      주간 리그
feature/passport/    경로 여권
backend/             Supabase 스키마
```
의존 방향: `app → feature → core(data, vehicle, ui) → core:domain`

## 개발 환경
- Android Studio (JDK 17), Gradle 8.10.2 (wrapper 포함), AGP 8.8.2
- minSdk 28 / compile·targetSdk 35
- Pleos Connect Emulator: Android 14(API 34) system image, RAM 4GB, Cold boot
  ([설정 가이드](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator))
- SDK 저장소: `https://nexus-playground.pleos.ai/repository/maven-releases/` (`settings.gradle.kts` 에 등록됨)

## 실행 방법
```bash
./gradlew :core:domain:test               # 규칙 단위 테스트
./gradlew installDebug                    # 실제 SDK 모드 (Pleos 에뮬레이터 Virtual Console 로 차량 상태 변경)
./gradlew installDebug -PfakeVehicle=true # 시연 모드: 홈에서 경로 등록 → 정상/과속/데이터 누락 재생
```

## 확인 필요 (TODO)
- SDK 효율값 단위 (`core/vehicle/EfficiencyNormalizer.kt`) — 현재 km/L, km/kWh 로 가정
- 주행 중 백그라운드 동작 허용 범위와 foregroundServiceType (`feature/record/RecordingService.kt`)
- FusedLocation SDK 신청 가능 시점 — 현재 위치는 NaviHelper 이벤트로 수신
- 공식 Figma 디자인 토큰 반영 (`core/ui/theme/Theme.kt`)

## 제출물
- APK: `apk/팀명_앱이름_release.apk` (release 빌드, 용량 초과 시 GitHub Releases)
