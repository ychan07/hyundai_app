# backend (Supabase)

계정·시즌·요약 기록·주간 순위·미션 보상을 관리한다. 앱은 원시 위치/속도를 올리지 않는다.

## 구성
- `supabase/migrations/0001_init.sql` — 테이블, 순위 view, RLS
- (예정) `supabase/functions/` — 주간 순위 재계산·공동 순위·보상 확정 Edge Function

## 로컬 실행
```bash
npx supabase init      # 최초 1회 (config.toml 생성)
npx supabase start
npx supabase db reset  # migrations 적용
```

## 앱 연동
`core/data/remote/RemoteSyncApi.kt` 의 `OfflineSyncApi` 를 Supabase 구현으로 교체하고
`app/di/AppModule.kt` 의 `remoteSyncApi()` 바인딩을 바꾼다. URL/anon key 는 `local.properties` 로 주입하고 커밋하지 않는다.
