# Pleos Connect 개발 시 사용 조건·제한 가이드

2026-10-31 기준

Pleos Connect SDK에는 공식적으로 호출 횟수 제한이 명시돼 있지 않습니다. 실질적인 사용 조건은 **권한 승인, 차종별 지원 여부, 주행 제한, 앱 리소스 한도** 네 가지이며, 리소스 한도는 주행 중 CPU 5% 미만·RAM 400 MB 이하입니다.

## 요약: 개발 전에 꼭 알아야 할 10가지

| # | 조건 | 어기면 |
| --- | --- | --- |
| 1 | minSdk 28 이상, 에뮬레이터 v2.1.0 | 배포·Virtual Console 불가 |
| 2 | SDK별 콘솔 상태 확인 (TTS·STT·위치·차량 제어는 준비 중 표기) | 기획한 기능이 막힘 |
| 3 | 차량 제어(`CONTROL_CAR_*`) 권한은 마켓 사전 승인 필요 | 선언만 해도 심사 탈락 |
| 4 | 모든 차량 기능은 `checkXxxCapabilities`, 위치는 `isSupported()` 먼저 | 차종별 미지원 오류 |
| 5 | 모든 호출은 비동기, `onConnected`/`onComplete` 이후 진행 | 연결 실패·크래시 |
| 6 | 속도·기어는 AAOS `CarPropertyManager`로 | Vehicle SDK에 없음 |
| 7 | 내비 경유지 최대 2개, 좌표는 위도·경도 순 | 요청 실패·잘못된 안내 |
| 8 | DO 미선언 앱은 주행 중 사용 불가, 영상·차량 설정·장문은 주행 중 항상 차단 | 심사 탈락 |
| 9 | 오디오 포커스 처리 필수, 음성 앞 무음 150~200 ms | 첫 음절 잘림·심사 탈락 |
| 10 | 실행 10초, 버튼 반응 2초, 주행 중 CPU 5% 미만, RAM 400 MB | 심사 탈락 |

## 1. 개발 환경 필수 조건

minSdk 28 미만이면 차량 보안 요건(서명 키 순환·계보) 때문에 아예 배포 대상이 될 수 없으므로 프로젝트 생성 시점에 고정해야 합니다.

| 항목 | 조건 | 비고 |
| --- | --- | --- |
| minSdk | Android API 28 이상 (필수) | 서명 키 순환·lineage 지원 요건 |
| Gradle | 8.0 이상 | |
| 언어 | Java 또는 Kotlin | |
| 에뮬레이터 | Pleos Connect Emulator v2.1.0 (Android 14 / API 34) | 이전 버전이 있으면 삭제 후 설치 |
| AVD 설정 | Form factor Automotive, Default boot = Cold, RAM 4 GB | 콜드 부트 미설정 시 오동작 |
| Virtual Console | 에뮬레이터 v2.1.0 이상에서만 동작 | `adb forward tcp:8088 tcp:8088` 후 브라우저 127.0.0.1:8088 |
| SDK 저장소 | `https://nexus-playground.pleos.ai/repository/maven-releases/` | 공식 import 가이드 기준, `settings.gradle`에 등록 |

### SDK 버전 (2026-10-19 릴리스 기준)

| SDK | 문서 기준 버전 | Maven 아티팩트 |
| --- | --- | --- |
| Vehicle | v2.3.2 (릴리스 노트 표기는 v2.3.1) | `ai.pleos.playground:Vehicle` |
| NaviHelper | v3.0.5 | `ai.pleos.playground:NaviHelper` |
| TextToSpeech | v3.3.1 | `ai.pleos.playground:TextToSpeech` |
| SpeechToText | v3.3.1 | `ai.pleos.playground:SpeechToText` |
| FusedLocation | v1.1.2 (참가팀은 1.1.3 사용 중) | `ai.pleos.playground:FusedLocation` |

- 이번 릴리스에서 LLM SDK, CRN, 블루투스 동글 연동은 지원 종료됐습니다. 예전 블로그나 문서에 나오면 쓰지 않습니다.
- 이전 버전 대비 Breaking change가 많습니다(NaviHelper 좌표 순서 경도·위도 → 위도·경도, TTS/STT 에러 타입 String → ErrorType, Vehicle CruiseControl 상태값 교체 등). 인터넷 예제 코드는 버전을 먼저 확인합니다.
- 에뮬레이터의 블루투스 기능은 제한적입니다. BT 기반 기능은 실증이 어렵습니다.

## 2. 계정·프로젝트·SDK 사용 승인 조건

SDK마다 Playground 콘솔에서 사용 상태가 다르므로, 기획서에 쓸 SDK는 반드시 콘솔에서 상태를 확인한 뒤 확정합니다.

1. [Pleos Playground](https://developers.pleos.ai)에서 Pleos 계정 가입
2. Workspace > My Project > New Project (프로젝트명, 개발사명, **카테고리**, 앱 설명 입력). 카테고리는 이후 개발·심사에 쓰이므로 먼저 정합니다.
3. 프로젝트 > SDK 메뉴에서 SDK별 상태 확인, 필요 시 Request

| 콘솔 상태 | 의미 | 공식 가이드상 해당 SDK |
| --- | --- | --- |
| Ready to Use | 신청 없이 바로 사용 또는 승인 완료 | Vehicle State(조회), NaviHelper |
| Request Required | 신청 버튼으로 요청 → Under Review → 승인/Rejected | (상태 전환 시) |
| Not Available Yet (Coming Soon) | 준비 중, 신청 불가 | FusedLocation, TTS, STT, Vehicle Control |
| Affiliation Required | 웹 신청 불가, 제휴 계약 필요 | — |

- **확인 필요:** 가이드는 TTS·STT·FusedLocation을 준비 중으로 적고 있지만, 10/19 SDK 릴리스 노트에는 정식 버전이 있고 참가팀들도 의존성으로 쓰고 있습니다. 반면 Android TTS로 대체 중인 팀도 있습니다. 실제 콘솔 화면이 기준입니다.
- **Client ID / Client Secret**: TTS·STT를 서버 모드(HYBRID, SERVER)로 쓸 때만 필요합니다. 프로젝트 정보 화면에 있습니다. 소스에 하드코딩해 GitHub에 올리지 않습니다.
- **Project ID (Key)**: 나중에 Pleos App Market Console에서 앱 심사를 요청할 때 입력합니다.
- **CRN**: 에뮬레이터 v2.1.0부터 불필요합니다.

## 3. Vehicle SDK 사용 조건

읽기(`CAR_*`) 권한 위주로 설계하고, 제어(`CONTROL_CAR_*`)는 사전 승인 없이는 마켓 심사를 통과할 수 없으므로 데모용 옵션으로만 둡니다.

### 권한

| 구분 | 접두어 | 예시 | 조건 |
| --- | --- | --- | --- |
| 조회(get·구독) | `CAR_*` | `pleos.car.permission.CAR_DOORS` | 쓰는 도메인 권한만 선언 |
| 제어(set) | `CONTROL_CAR_*` | `CONTROL_CAR_DOORS` | Pleos App Market과 사전 협의·승인된 범위만 선언 가능 |

| 도메인 | 조회 권한 |
| --- | --- |
| Door | `CAR_DOORS` |
| Window, Wiper | `CAR_WINDOWS` |
| HVAC | `CAR_CLIMATE` |
| DrivingMode, Brake, Odometer | `CAR_DRIVE` |
| EvBattery, Fuel | `CAR_ENERGY` |
| CarInfo, Display | `CAR_INFO` |
| Light, TurnSignal | `CAR_LIGHTS` |
| SideMirror | `CAR_MIRRORS` |
| Safety, Tire | `CAR_SAFETY` |
| Seat | `CAR_SEATS` |
| Toll | `CAR_TOLL` |

- 위 표는 도메인 대표 권한입니다. API마다 다를 수 있으니 각 메서드의 `@RequiresPermission`을 확인합니다.
- 승인되지 않은 권한을 매니페스트에 선언만 해도 앱 심사 탈락입니다. 쓰지 않는 권한은 지웁니다.
- 권한이 없으면 `onFailed`로 `ERROR_SECURITY_PERMISSION`이 옵니다.

### 호출 규칙

1. `Vehicle(context)` 생성 → `initialize()` → `getXxx()`로 도메인 객체 획득 → 사용 → `release()`
2. 모든 Capability·Get·Set은 IPC 비동기입니다. 결과는 `onComplete` / `onFailed`로 오고, 콜백이 끝나기 전에 `release()`를 호출하면 안 됩니다.
3. Set은 실패할 때만 `onFailed`가 옵니다. 성공 확인이 필요하면 같은 값을 구독(register)해서 변경을 확인합니다.
4. 구독은 `registerXxx` / `unregisterXxx` 쌍으로 관리합니다. 화면 종료·백그라운드 전환 시 해제해 누수를 막습니다.
5. 차종별로 지원이 다르므로 기능마다 `checkXxxCapabilities`를 먼저 호출합니다. 결과의 `AccessType`(NONE / READ / WRITE / READ_WRITE)과 `AreaBounds`(최소·최대값)로 UI를 켜고 끕니다. 차종 이름으로 분기하지 않습니다.

### 에러 코드 처리

| ErrorCode | 의미 | 권장 처리 |
| --- | --- | --- |
| ERROR_NOT_SUPPORTED_PROPERTY | 기능 미지원 | UI 비활성 또는 대체 흐름 |
| ERROR_NOT_SUPPORTED_AREA | 영역(좌석·문 위치 등) 미지원 | 다른 영역 선택 |
| ERROR_NOT_SUPPORTED_VALUE_TYPE | 값 타입 오류 | 파라미터·타입 확인 |
| ERROR_SECURITY_PERMISSION | 권한 부족 | 매니페스트·`@RequiresPermission` 확인 |
| ERROR_SERVICE_CONNECTION_FAILURE | Vehicle Service 연결 실패 | `initialize()`·에뮬레이터 상태 확인 |
| ERROR_INTERNAL_SERVICE | 내부 오류 | 재시도 또는 일시 오류 안내 |
| ERROR_DRIVING_RESTRICTED | 주행 중 제한 기능 | 주행 제한 안내 |
| ERROR_EMPTY_RESULT | 결과 없음 | 빈 상태 UI |
| ERROR_UNDEFINED | 미정의 | 로그 후 재시도 |

`onFailed`로 오는 예외가 항상 `VehicleException`은 아니므로 타입 확인 후 `errorCode`를 봅니다.

### 호출 횟수·주기 제한

공식 문서에는 Vehicle SDK의 호출 횟수나 구독 주기 제한이 명시돼 있지 않습니다. 대신 앱 심사에서 과도한 리소스 사용으로 시스템 성능을 떨어뜨리면 반려되므로(9장), 실무 원칙은 다음과 같습니다.

- 값을 반복 폴링(get 반복)하지 말고 구독(register)으로 받습니다.
- 구독 콜백에서 무거운 연산·UI 갱신을 바로 하지 말고 필요 시 디바운스합니다.
- Set을 연속 호출하지 않습니다(슬라이더 드래그 중 매 프레임 호출 등).

## 4. AAOS 표준 API 사용 조건 (속도·기어 등)

속도·기어·외기온·연료량 같은 표준 데이터는 Pleos SDK가 아니라 Android Automotive의 `CarPropertyManager`로 받습니다. Pleos SDK는 AAOS에 같은 기능이 있으면 중복 제공하지 않으며, 실제로 Vehicle SDK v2.3.x에서 `VehicleSpeedListener`가 제거됐습니다.

| 데이터 | 받는 곳 | 비고 |
| --- | --- | --- |
| 차량 속도 | AAOS `PERF_VEHICLE_SPEED` | Android 표준 권한 `android.car.permission.CAR_SPEED` |
| 기어 | AAOS `GEAR_SELECTION` | 표준 권한 `android.car.permission.CAR_POWERTRAIN` |
| 주행 제한 상태 | AAOS `CarUxRestrictionsManager` | 8장 참고 |
| 문·시트·공조·조명 등 제조사 특화 | Pleos Vehicle SDK | 3장 참고 |

- 속성 지원 여부는 차종마다 다릅니다. 차종명으로 분기하지 말고 `VehiclePropertyIds`와 `CarPropertyManager`로 실제 지원·접근 가능 여부를 확인합니다.
- 연속 값(속도 등)은 구독 시 샘플링 속도를 지정합니다. 화면 표시용이면 낮은 주기로 충분하며, 최고 주기 구독은 리소스 낭비입니다. 속성별 최소·최대 주기는 `CarPropertyConfig`에서 확인합니다.
- Pleos SDK에도 AAOS에도 없는 특수 속성이 필요하면 직접 구현하기 전에 기술지원팀 검토를 요청해야 합니다.

## 5. FusedLocation SDK 사용 조건

차종에 따라 아예 지원하지 않을 수 있으므로 `isSupported()`가 false일 때도 앱이 동작하는 대체 흐름이 필수입니다.

| 항목 | 조건 |
| --- | --- |
| 권한 | `pleos.car.permission.FUSED_LOCATION` 선언 필수 |
| 지원 확인 | 사용 전 `isSupported()` 호출, 미지원이면 위치 기능 숨김·비활성 |
| 생명주기 | 생성 → `isSupported()` → `initialize()` → 조회/구독 → `unregisterFusedLocationCallback()` → `release()` |
| 1회 조회 | `getLastKnownLocation()`: 새로 계산하지 않고 최근 값만 돌려줌, `null` 가능 |
| 연속 수신 | `registerFusedLocationCallback()` → `onChanged(LocationData?)`, 해제 시 등록한 같은 리스너 전달 |
| 데이터 | 위도, 경도, 고도, 방향(heading), 신뢰도, 타임스탬프 |
| 근접 알림 | `addProximityAlert()` / `removeProximityAlert()` (지오펜스 진입·이탈) |
| 에러 | `onError(Exception)` 구현 필수(v1.1.2부터) |
| 접근 차단 | 사용자가 위치 접근을 막은 상태면 `showLocationAccessBlockedPopup()`으로 안내 |

- `onChanged`의 값이 `null`이면 유효한 위치로 쓰지 않습니다. 직전 값 유지 또는 "위치 확인 중" 표시로 처리합니다.
- Android 표준 위치 권한(`ACCESS_FINE_LOCATION`, 백그라운드 위치 등)의 동작은 이 SDK에서 따로 보장하지 않습니다. 구글 Fused Location 문서 기준으로 동작을 가정하지 않습니다.
- **호출 주기:** 업데이트 간격을 지정하는 파라미터나 근접 알림의 최대 개수·반경 제한은 공식 가이드에 명시돼 있지 않습니다. 참가팀 사례에서는 약 1초 간격으로 수신됐습니다. 근접 알림을 여러 개 쓸 계획이면 API Reference에서 제한을 먼저 확인합니다.
- 에뮬레이터 Virtual Console의 기능 목록에는 GPS 경로 재생이 없습니다. 위치 이동 시연 방법은 미리 확인해야 합니다.

## 6. NaviHelper SDK 사용 조건

경유지는 최대 2개(FIRST, SECOND)까지만 넣을 수 있고, 내비가 장착된 차량에서만 완전히 동작하므로 "경유지 여러 곳 자동 추가" 같은 기획은 처음부터 2개 한도로 설계합니다.

### 권한

| 권한 | 쓰는 API |
| --- | --- |
| `pleos.car.permission.NAVI_ROUTE` | `get*Info()`, `requestTrip()` (정보 조회) |
| `pleos.car.permission.NAVI_ROUTE_SEARCH` | `requestRoute()`, `requestRouteWithWaypoints()`, `cancelRoute()`, `requestReRoute()`, `addWaypoint()`, `removeWaypoint()`, `changeRouteOption()` |
| `pleos.car.permission.NAVI_CUSTOM_MAP` | `requestNavigationScreen()` |

### 호출 규칙

1. `NaviHelper` 생성 → `initialize(onConnected, onFailed)` → **`onConnected` 이후에만** 리스너 등록·API 호출 → `removeListener()` → `release()`
2. 요청 결과는 반환값이 아니라 `NaviHelperEventListener` 콜백으로 옵니다. 어떤 이벤트가 오는지는 내비 앱 구현과 현재 상태에 따라 달라집니다.
3. 경유지: `WaypointIndex.FIRST` / `SECOND` 최대 2개, 전체 삭제는 `WaypointIndex.ALL`. 결과는 `onWayPointInfo()`, 도착은 `onWayPointArrived()`.
4. v3.0.5부터 좌표 순서가 **위도, 경도**로 바뀌었습니다(이전 버전 예제는 경도 먼저). 좌표가 뒤집히면 엉뚱한 곳으로 안내됩니다.
5. 경로 완료는 `onRouteEnded` 대신 `onRoutingCompleted`를 씁니다(폐기 예정).

### 에러 코드

| NaviErrorCode | 의미 | 처리 |
| --- | --- | --- |
| `CAUSE_LOW_BATTERY` | 배터리 부족 | 사용자 안내 |
| `CAUSE_SEARCH_ROUTE_OVER_DISTANCE` | 최대 탐색 거리 초과 | 거리 초과 안내 |
| `CAUSE_SEARCH_ROUTE_TOO_SHORT_DISTANCE` | 목적지가 너무 가까움 | 근거리 안내 |
| `CAUSE_SEARCH_ROUTE` | 경로 탐색 서버 오류 | 재시도 또는 안내 |

에러가 나면 연결 완료 후 호출했는지, 권한·좌표·POI ID·경유지 인덱스가 유효한지, 내비 앱이 지금 요청을 처리할 수 있는 상태인지를 확인합니다.

- **호출 횟수:** 공식 문서에 횟수 제한은 없습니다. 다만 경로 탐색은 서버를 타므로 짧은 시간에 재탐색·경유지 변경을 반복 호출하지 않습니다.
- 주행 중 내비 검색(키보드 입력)은 지역에 따라 막힙니다(8장). 목적지는 앱이 좌표로 넘기고, 입력은 음성·프리셋으로 처리합니다.

## 7. SpeechToText / TextToSpeech 사용 조건

공모전 시연은 네트워크·자격증명 변수가 없는 `ON_DEVICE` 모드를 기본으로 하고, 서버 모드는 품질 향상용 옵션으로 두는 것이 가장 안전합니다.

### 모드별 조건

| 모드 | 네트워크 | 앱 등록(Client ID/Secret) | 실패 시 |
| --- | --- | --- | --- |
| `ON_DEVICE` | 불필요 | 불필요 | 온디바이스 엔진 오류 |
| `HYBRID` (기본값) | 서버 우선 | 서버 사용 시 필요 | 네트워크 실패 시 자동으로 온디바이스 전환 |
| `SERVER` | 필수 | 필수 | 전환 없이 에러 이벤트 |

### 공통 호출 규칙

1. 권한: STT는 `pleos.car.permission.STT_SERVICE`, TTS는 `TTS_SERVICE` 권한을 매니페스트에 선언합니다. 없으면 초기화부터 실패하고 `PERMISSION_DENIED`가 옵니다.
2. 순서: 생성(`Mode` 지정, 생략 시 HYBRID) → `initialize()` → 리스너 등록 → (필요 시) `setLocale()` → (서버 모드) `registerApp()` → **`onConnected()` 이후에만** 요청 → `stop()` → 리스너 해제 → `release()`
3. STT는 `request()`로 차량 마이크를 열고 `stop()`으로 닫습니다. 종료 시 `onEndedRecognition()`이 옵니다.
4. STT는 텍스트만 돌려줍니다. 앱 기능 실행("음악 틀어줘" → 재생)은 직접 구현하거나 Gleo Interface(App Schema 등록, function calling 방식)를 붙여야 합니다.
5. TTS `speak()`는 한 번에 보내면 `isFinal` 생략(true)입니다. 나눠 보낼 때는 마지막 호출만 `isFinal = true`로 두고, 중간 호출에 빠뜨리면 거기서 요청이 끝납니다.
6. TTS 지원 로케일: `ko-KR`, `en-US`, `fr-CA`, `es-US`, `es-MX`. `setLocale()`을 안 부르면 이전에 설정한 값, 없으면 시스템 로케일을 씁니다. 지원하지 않는 언어는 `UNAVAILABLE_LANGUAGE` 오류입니다.

### 에러 타입 (TTS·STT 공통)

| ErrorType | 의미 | 처리 |
| --- | --- | --- |
| `PERMISSION_DENIED` | 서비스 권한 없음 | `STT_SERVICE` / `TTS_SERVICE` 선언 확인 |
| `UNAUTHENTICATED` | 앱 인증 실패 | Client ID/Secret 확인 |
| `NETWORK_ERROR`, `TIMEOUT` | 네트워크·시간 초과 | 네트워크 복구 후 재시도 |
| `SERVER_INTERNAL_ERROR` | 서버 내부 오류 | 잠시 후 재시도 |
| `INVALID_REQUEST_ERROR` | 호출 순서·중복 요청·입력값 오류 | 순서 확인, 동시 요청 금지 |
| `MIC_ERROR` | 마이크 시작·처리 오류 | 마이크·센서 개인정보 설정 확인 |
| `AUDIO_ERROR` | 오디오 입력/출력 처리 실패 | 포맷·출력 장치 확인 |
| `ON_DEVICE_ENGINE_ERROR` | 온디바이스 엔진 오류 | 엔진 상태 확인 또는 HYBRID 고려 |
| `NOT_SUPPORTED_API` | 현재 모드·환경 미지원 | `Mode` 확인 |
| `UNAVAILABLE_LANGUAGE` | 언어 미지원 | 지원 로케일로 변경 |
| `UNKNOWN` | 미분류 | 로그 수집 |

새 에러 타입이 추가될 수 있으므로 `when`/`switch`에 반드시 기본 분기를 둡니다.

### 호출 횟수·쿼터

공식 문서에 서버 모드의 호출 횟수·일일 쿼터·최대 텍스트 길이·최대 인식 시간은 명시돼 있지 않습니다. `INVALID_REQUEST_ERROR` 설명에 "중복 요청"이 원인으로 언급되므로, 인식·합성은 한 번에 하나만 진행하고 새 요청 전에 `stop()`합니다. 서버 모드를 주력으로 쓸 계획이면 쿼터를 운영사무국에 문의합니다.

### 오디오 출력 규칙

- 재생 전에 `AudioAttributes` + `AudioFocusRequest`로 오디오 포커스를 요청하고, 포커스 변경 이벤트를 반드시 처리합니다(LOSS → 즉시 중단, LOSS_TRANSIENT → 일시정지, CAN_DUCK → 볼륨 감쇄).
- 내비 안내처럼 음악과 동시에 나와야 하는 음성은 `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`으로 요청합니다.
- Gleo 음성 비서가 호출돼 `AUDIOFOCUS_LOSS_TRANSIENT`를 받으면 즉시 재생을 멈춥니다.
- 음성 안내 앞에 150~200 ms 무음(pre-roll)을 넣어 첫 음절이 잘리지 않게 합니다. 150 ms 미만은 잘릴 수 있고 200 ms 초과는 지연이 느껴집니다.

## 8. 주행 제한(CarUxRestrictions)·운전자 주의 분산 규칙

Distraction Optimization(DO)을 선언하지 않은 앱은 주행 중 아예 사용할 수 없으므로, 주행 중에 쓰이는 기능이 있다면 DO 선언과 `CarUxRestrictions` 처리를 둘 다 구현해야 합니다.

| 상황 | DO 미선언 앱 |
| --- | --- |
| 주차(Parking) | 사용 가능 |
| 주행(Drive) | 사용 불가 |

### 구현 조건

1. AAOS `CarUxRestrictionsManager.registerListener()`로 구독하고 `onUxRestrictionsChanged()`에서 상태를 받습니다.
2. `isRequiresDistractionOptimization()`이 true일 때 `getActiveRestrictions()` 비트 플래그를 보고 기능별로 끕니다. 지역별 규칙은 플랫폼이 플래그로 내려주므로 앱은 받은 플래그만 따르면 됩니다.
3. 주행 판정은 속도·기어·브레이크 신호와 지역 법규를 기준으로 플랫폼이 합니다.

### 주요 제한 플래그와 주행 중 허용 여부

| 기능 | 플래그(값) | 북미 | 유럽 | 기타 지역 |
| --- | --- | --- | --- | --- |
| 키보드 입력 | `NO_KEYBOARD`(8) | 불가 | 가능 | 가능 |
| 다이얼패드 | `NO_DIALPAD`(1) | 불가 | 가능 | 가능 |
| 통합 검색 | `NO_KEYBOARD`(8), `NO_FILTERING`(2) | 음성만 | 가능 | 가능 |
| 미디어 로그인·설정·검색 | `NO_SETUP`(64) 외 | 불가 | 불가 | 가능 |
| 디지털키·문·시트·차량 설정 | `NO_SETUP`(64) | 불가 | 불가 | 불가 |
| 영상 시청 | `NO_VIDEO`(16) | 불가 | 불가 | 불가 |
| 내비 검색 | `NO_KEYBOARD`(8), `NO_FILTERING`(2) | 불가 | 가능 | 가능 |
| QR 로그인 | `NO_SETUP`(64) | 불가 | 불가 | 불가 |
| 장문 텍스트(매뉴얼 등) | `LIMIT_CONTENT`(32), `LIMIT_STRING_LENGTH`(4) | 불가 | 불가 | 불가 |

주차 중에는 모든 지역에서 전부 허용됩니다. 한국이 "기타 지역"에 해당하는지는 문서에 명시돼 있지 않아 확인이 필요하며, 지역과 관계없이 **영상·차량 설정·장문 텍스트는 주행 중 항상 차단**된다고 보고 설계하는 것이 안전합니다.

### 카테고리별 주행 중 예외 허용 예

| 카테고리 | 주행 중 제한적으로 허용될 수 있는 기능 |
| --- | --- |
| Music/Audio | 앨범아트, 재생·일시정지·곡 넘김, 스트리밍, 라디오 채널 변경, 오디오북 |
| Maps/Navigation | 경로·실시간 교통 안내, 간결한 지도, TBT 음성, 목적지 경로 탐색 |
| Mobility Services | 주행 관련 차량 관리, 현재 위치 기반 정보, EV 충전소 검색·경유지 설정, 주유소·주차장 확인 |

그 외 카테고리: Lifestyle, Entertainment, Business, Kids, Social/Communication. 카테고리는 Playground 프로젝트 생성 때 고르며, 서비스 내용과 다른 카테고리나 범위 밖 기능은 심사 반려 사유입니다.

### UX 원칙 (심사 대상)

- 주행 중 영상 재생 시작·재개 금지, 과한 애니메이션·반복 움직임 최소화
- 게임·웹서핑·피트니스처럼 주의를 끄는 활동 유도 금지
- 키보드 대신 음성·프리셋·최근 항목 우선, 자주 쓰는 기능은 첫 화면 상단에
- 큰 터치 영역, 높은 대비, 짧은 텍스트(Pleos UI 디자인 가이드 준수), 다단계 메뉴·드롭다운 남용 금지

## 9. 성능·리소스·응답 시간 기준

이 문서에서 숫자로 정해진 사실상의 "호출·사용량 한도"는 SDK가 아니라 앱 전체 리소스 기준이며, 주행 중 일반 앱은 CPU 5% 미만, RAM 400 MB 이하입니다.

### 리소스 한도

| 항목 | 기준 |
| --- | --- |
| CPU (내비 앱, 주행 중) | 15% 이하 |
| CPU (내비 외 앱, 주행 중) | 5% 미만 |
| CPU (모든 앱, 주차 중) | 25% 이하 |
| RAM | 400 MB 이하 |
| 시스템 입력 응답 | 0.25초 이내 (터치 → 터치 효과 애니메이션) |

### 앱 응답 시간 (심사 기준)

| 항목 | 기준 |
| --- | --- |
| 앱 실행 | 10초 이내 |
| 콘텐츠 로딩 | 10초 이내 |
| 버튼 반응(화면 전환·기능 실행·상태 변경) | 2초 이내 |
| 2초 넘게 걸리는 작업 | 진행 중 표시(로딩 UI) 필수 |
| 백그라운드 후 재진입 | 마지막 상태 복원 (몇 시간 미사용이면 초기 상태 허용) |

### 설계 원칙

- 콜드 스타트 때는 최소한만 초기화하고 나머지는 지연 로딩합니다. 스플래시에 오래 머물지 않습니다.
- 장시간 백그라운드 작업, 불필요한 타이머·폴링을 최소화합니다. SDK 값은 폴링 대신 구독으로 받습니다.
- 리스트·이미지는 RecyclerView, 캐싱으로 메모리를 관리합니다.
- 화면은 운전석 쪽 Driving view(앱이 가릴 수 없음) 옆 앱 영역에 최대 2개 앱이 뜹니다. `screenOrientation` 미지정 시 portrait(창 1개), `landscape`면 창 2개를 씁니다. 두 형태 모두 디자인하고, 창 크기가 바뀌어도 상태·레이아웃이 깨지지 않게 합니다.
- 화면에서 사라져도 종료되지 않고 백그라운드에 남을 수 있습니다. 백그라운드 진입 시 화면 갱신·애니메이션·불필요한 센서 수집을 멈춥니다.
- 레터박스 없이 화면을 꽉 채우고, 호환 모드로 실행되면 안 됩니다. 테스트용 더미 데이터·임시 파일이 남아 있으면 반려됩니다.

## 10. 보안·개인정보·네트워크 조건

차량은 여러 사람이 같이 쓰는 기기이므로 계정 전환과 개인정보 최소 수집을 전제로 설계해야 하고, 주행 중 네트워크 단절을 정상 상황으로 다뤄야 합니다.

### 하드웨어 접근

- 차량 물리 제어·센서에 직접 접근할 수 없습니다. Pleos SDK·AAOS 플랫폼 API만 씁니다.
- 모바일 전용 API는 제한될 수 있습니다: 전화, SMS, 일부 센서(자이로·근접), 캘린더, 연락처 등. 사용 전 지원 여부를 확인하는 선택 구조로 만듭니다.

### 계정·개인정보

- 차주·운전자가 바뀌어도 쉽게 로그아웃·계정 전환이 되어야 합니다.
- 소셜·OTT 로그인 토큰 같은 자격증명은 Keystore 등 보안 저장소에 둡니다. Playground Client Secret도 소스·GitHub에 평문으로 두지 않습니다.
- 위치·음성·계정 정보를 요청할 때 이유를 분명히 설명하고 동의 흐름을 단순하게 만듭니다.
- 불필요한 개인식별정보는 저장하지 않고, 로그에 개인정보가 들어가면 마스킹합니다.
- 차량 밖으로 나가는 텔레메트리는 차량 ID·사용자 ID를 익명·가명 처리하고, 주행 행동 데이터를 마케팅에 쓰지 않도록 분리합니다. (VIN 조회 API가 있지만 외부 전송 시 이 원칙이 적용됩니다.)

### 네트워크

- 주행 중 셀룰러·Wi-Fi가 끊기거나 대역폭이 급감하는 것을 전제로 합니다.
- 최근 콘텐츠는 로컬에 캐시해 오프라인에서도 핵심 기능이 동작하게 합니다.
- 요청에 재시도 로직과 적절한 타임아웃을 둡니다.
- 네트워크가 끊기면 오프라인 상태임을 UI로 알려줍니다.
- 서버 모드 STT/TTS(HYBRID)는 네트워크 실패 시 온디바이스로 넘어가므로, 음성 기능은 HYBRID 또는 ON_DEVICE가 이 원칙에 맞습니다.

## 11. 공모전 제출 관점 체크리스트와 확인 필요 사항

공모전 1차 마감은 2026-11-15이고, 2차 PT(12/3~4)에서는 에뮬레이터 또는 기기로 실제 동작 시연이 필수이므로 아래 항목을 마감 전에 끝냅니다.

### 개발 착수 전

- [ ] Playground 가입, 프로젝트 생성, 카테고리 결정
- [ ] 콘솔에서 사용할 SDK 상태 확인(Ready / Request / Coming Soon), 필요 시 신청
- [ ] 에뮬레이터 v2.1.0 설치, Cold boot·RAM 4 GB 설정, Virtual Console 접속 확인
- [ ] minSdk 28 이상으로 프로젝트 생성, Maven 저장소 등록

### 구현 중

- [ ] 쓰는 도메인의 권한만 매니페스트에 선언 (`CONTROL_CAR_*`는 데모 빌드에만)
- [ ] 모든 Vehicle 기능 앞에 `checkXxxCapabilities`, 위치 앞에 `isSupported()`
- [ ] 모든 SDK의 `onFailed` / `onError`에 에러 코드별 처리와 기본 분기
- [ ] `initialize` → `onConnected` 이후 호출, 화면 종료 시 unregister·`release()`
- [ ] `CarUxRestrictions` 구독, 주행 중 차단 기능 처리 (Virtual Console로 기어 D/P 전환하며 확인)
- [ ] 음성 출력에 오디오 포커스 요청·변경 처리, pre-roll 150~200 ms
- [ ] 네트워크 끊김 시 오프라인 UI와 캐시

### 제출 전

- [ ] 실행 가능한 release APK를 `apk/팀명_앱이름_release.apk`로 GitHub에 업로드
- [ ] README에 앱 소개, 실행 방법, 테스트 환경(에뮬레이터 v2.1.0, SDK 버전), APK 위치 기재
- [ ] Client Secret 등 자격증명이 저장소에 없는지 확인
- [ ] 앱 실행 10초, 버튼 반응 2초 이내 확인
- [ ] Virtual Console로 시나리오를 재현한 시연 영상 녹화

### 확인 필요 사항 (운영사무국 또는 Playground 문의)

| 질문 | 왜 중요한가 |
| --- | --- |
| TTS·STT·FusedLocation이 콘솔에서 지금 바로 사용 가능한가? | 가이드는 Coming Soon, 릴리스 노트는 정식 배포로 서로 다름 |
| 공모전 심사용으로 `CONTROL_CAR_*` 권한을 써도 되는가? | 마켓 심사는 사전 승인 필요, 공모전 기준은 미공지 |
| 서버 모드 STT/TTS의 호출 쿼터는? | 공식 문서에 수치 없음 |
| 한국은 주행 제한 정책상 어느 지역에 속하는가? | 키보드·검색 허용 여부가 달라짐 |
| 근접 알림 최대 개수·반경, 위치 업데이트 주기는? | 문서에 수치 없음 |
| 에뮬레이터에서 GPS 이동 경로를 재현하는 방법은? | Virtual Console 기능 목록에 없음 |

문의처: 공모전은 appdevchallenge@hyundai-partners.com, Pleos 기술 지원은 partnership@pleos.ai.

## 출처

모두 2026-10-31에 열람한 Pleos Playground 공식 문서이며, 별도 표기한 것만 참가팀 공개 저장소입니다.

- [Installing and Running Pleos Connect Emulator](https://document.pleos.ai/en/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator)
- [Pleos Connect Virtual Console Setup Guide](https://document.pleos.ai/en/docs/connect/guide/getting-started/application-development/use-vehicle-emulator)
- [Create a Project and Request SDK Access](https://document.pleos.ai/en/docs/pleos-only/create-my-project)
- [Pleos Connect SDK Release Notes](https://document.pleos.ai/en/docs/pleos-only/release-note/sdk-release-note)
- [Pleos Connect Emulator Release Note](https://document.pleos.ai/en/docs/pleos-only/release-note/emulator-release-note)
- [SDK Overview](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/)
- [SDK API Policy](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/pleos-connect-sdk-api)
- [API Compatibility & Availability](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/api-compatibility-availability)
- [Vehicle SDK Introduction](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/intro)
- [Importing Vehicle SDK](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/project-setup)
- [Vehicle SDK Declaring permission](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-permission)
- [Vehicle SDK Obtaining a domain object](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/obtain-domain-object)
- [Vehicle SDK Capability](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/check-capability)
- [Vehicle SDK Set](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/set-property)
- [Vehicle SDK Callback](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/callback)
- [Vehicle SDK Error code](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/Vehicle/vehicle-error-code)
- [FusedLocation SDK Introduction](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/FusedLocation/intro)
- [FusedLocation Location Data Access](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/FusedLocation/location-data-access)
- [FusedLocation Error Handling](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/FusedLocation/error-handling)
- [NaviHelper SDK Introduction](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/NaviHelper/intro)
- [NaviHelper Declaring permissions](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/NaviHelper/declaring-permissions)
- [NaviHelper Waypoints](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/NaviHelper/waypoint-settings)
- [NaviHelper Error handling](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/NaviHelper/error-handling)
- [SpeechToText SDK Introduction](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/SpeechToText/intro)
- [SpeechToText Declaring permissions](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/SpeechToText/declaring-permissions)
- [SpeechToText App registration](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/SpeechToText/app-registration)
- [SpeechToText Start and stop](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/SpeechToText/start-and-stop)
- [SpeechToText Error code](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/SpeechToText/error-code)
- [TextToSpeech SDK Introduction](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/TextToSpeech/intro)
- [TextToSpeech Set locale](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/TextToSpeech/set-locale)
- [TextToSpeech Start and stop](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/TextToSpeech/start-and-stop)
- [TextToSpeech Error code](https://document.pleos.ai/en/docs/pleos-only/connect-sdk-pleos/TextToSpeech/error-code)
- [Gleo Interface](https://document.pleos.ai/en/docs/pleos-only/app-integration-guide/guide/core-concepts)
- [Audio Focus Development Guide](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-developer-guide/audio-focus-development-guide/audio-focus-development)
- [TTS / Pre-roll Silence Guide](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-developer-guide/voice-guidance-preroll-silence-guide)
- [Driving Restriction Development Guide](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide)
- [Driving Restriction Policy by Region](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/driving-restiction-policy)
- [App Categories](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/app-category)
- [Design Considering Driving Safety](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/driver-distraction-guideline)
- [App Structure and Behavior Model](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/app-structure-screen-model)
- [Hardware Access Policy](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/platform-api-architecture)
- [Design for Performance and Resource Efficiency](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/performance-resource-mgmt)
- [Design Considering Security and Privacy](https://document.pleos.ai/en/docs/pleos-only/vehicle-app-planning-guide/security-privacy)
- [App Review: Functionality and Performance Stability](https://document.pleos.ai/en/docs/pleos-only/distribute/app-review-guide/review-functionality-performance-stability)
- [공모전 지원·제출 방법](https://appdevchallenge.auto-hmg.io/guide/submission)
- 참가팀 저장소: [daedongchajido](https://github.com/geunbonGit/daedongchajido), [chadak-pleos-app](https://github.com/Mujin8623/chadak-pleos-app), [mallo](https://github.com/MotoDNA/mallo)
