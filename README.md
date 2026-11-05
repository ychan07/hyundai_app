# 이야기 드라이브 (가칭)

내비게이션에 목적지를 찍으면 가는 길을 미리 보고 아이 나이에 맞는 동화를 지어, 이동하는 동안 차가 들려주는 Pleos Connect 차량용 앱.
창밖의 실제 호수·산이 이야기 속 장소가 되고, 비·밤·추위 같은 주변 환경에 따라 이야기가 바뀌며, 아이는 목소리로 주인공의 선택을 정한다.
현대자동차그룹 차량용 앱 개발 공모전 출품작.

## 문서
| 문서 | 내용 |
| --- | --- |
| [`planning/story_drive_plan.md`](planning/story_drive_plan.md) | 기획서: 문제, 사용 흐름, 환경 반응, 이야기 생성·재구성, 오프라인 대비, 안전, 기술 구성, 확인 필요 사항 |
| [`planning/pleos_dev_conditions.md`](planning/pleos_dev_conditions.md) | 개발 시 사용 조건·제한: 권한, 호출 규칙, 에러 코드, 주행 제한, 성능·심사 기준 |
| [`planning/pleos_api_guide.md`](planning/pleos_api_guide.md) | Pleos Connect API 정리 |

## 기술 스택
- Android Automotive (Pleos Connect), Kotlin 2.0, Jetpack Compose, Hilt
- Pleos Connect SDK: **Vehicle 2.3.2** (Gear), **NaviHelper 3.0.5** (위치)
- AAOS `CarPropertyManager`(PERF_VEHICLE_SPEED), `CarUxRestrictionsManager`
- 예정: TextToSpeech·SpeechToText·FusedLocation SDK(콘솔 사용 가능 여부 확인 후), Vehicle Wiper·Light, 이야기 생성 서버

## 모듈 구조
```
app/                 진입점, DI, 주행 중 화면 잠금, 홈(차량 데이터 지원 여부)
core/domain/         순수 Kotlin: 위치 모델, 거리 계산
core/vehicle/        VehicleGateway 인터페이스
  ├─ pleos/          실제 구현 (Pleos SDK + AAOS)
  └─ fake/           주행 재생 구현 — 에뮬레이터 시연용
core/ui/             차량 디스플레이용 테마(주/야간, 큰 글씨), 공통 컴포넌트
```
의존 방향: `app → core(vehicle, ui) → core:domain`

## 개발 환경
- Android Studio (JDK 17), Gradle 8.10.2 (wrapper 포함), AGP 8.8.2
- minSdk 28 / compile·targetSdk 35
- Pleos Connect Emulator: Android 14(API 34) system image, RAM 4GB, Cold boot
  ([설정 가이드](https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator))
- SDK 저장소: `https://nexus-playground.pleos.ai/repository/maven-releases/` (`settings.gradle.kts` 에 등록됨)

## 실행 방법
```bash
./gradlew installDebug                    # 실제 SDK 모드 (Pleos 에뮬레이터 Virtual Console 로 차량 상태 변경)
./gradlew installDebug -PfakeVehicle=true # 시연 모드: 홈에서 "주행 재생"
```

## 다음 작업 (TODO)
- Playground 콘솔에서 TTS·STT·FusedLocation 사용 가능 여부 확인
- 에뮬레이터에서 NaviHelper `onRouteStarted`·`onTripInfo`·`onRouteStateInfo`·`onCautionPointInfo` 값 기록 → 경로 좌표·터널 정보 제공 여부 확인
- `VehicleGateway` 에 와이퍼·전조등·외기온, 목적지·남은 시간 추가
- 이야기 생성 서버, 설계도·챕터 재생, 이야기 상태 저장·재구성, 오프라인 버퍼
- 홈 화면을 아이 프로필 → 목적지 확인 → 출발 흐름으로 교체

## 제출물
- APK: `apk/팀명_앱이름_release.apk` (release 빌드, 용량 초과 시 GitHub Releases)
