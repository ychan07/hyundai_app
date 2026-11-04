-- Route League 초기 스키마 (Supabase / Postgres)
-- 서버는 원시 위치·속도가 아니라 요약 기록만 보관한다 (기획서 7장: 기본 90일).

create table if not exists routes (
    id              text primary key,
    name            text not null,
    corridor_m      double precision not null default 60,
    created_at      timestamptz not null default now()
);

create table if not exists seasons (
    id              text primary key,             -- 예: 'S1'
    route_id        text not null references routes(id),
    starts_on       date not null,
    weeks           int  not null default 4,
    rule_version    text not null                 -- 순위 재계산 시 사용한 규칙 버전
);

-- 앱의 session id 를 그대로 PK 로 사용 → 재업로드해도 중복되지 않는다 (멱등).
create table if not exists drive_records (
    id              uuid primary key,
    user_id         uuid not null references auth.users(id) on delete cascade,
    route_id        text not null references routes(id),
    powertrain      text not null check (powertrain in ('ICE', 'EV', 'UNKNOWN')),
    vehicle_model   text,                         -- 같은 차종 리그 구분용
    started_at      timestamptz not null,
    ended_at        timestamptz not null,
    status          text not null check (status in ('QUALIFIED', 'DISQUALIFIED')),
    consumption     real,                         -- L/100km 또는 Wh/km
    unit            text,
    reasons         text[],                       -- 제외 사유
    rule_version    text not null,
    created_at      timestamptz not null default now()
);
create index if not exists drive_records_route_week on drive_records (route_id, started_at);

-- 미션 보상 중복 지급 방지: user + season + week + mission
create table if not exists mission_rewards (
    user_id         uuid not null references auth.users(id) on delete cascade,
    season_id       text not null references seasons(id),
    week            int  not null,
    mission_id      text not null,
    status          text not null default 'PENDING' check (status in ('PENDING', 'APPROVED', 'ISSUED')),
    created_at      timestamptz not null default now(),
    primary key (user_id, season_id, week, mission_id)
);

-- 주간 순위: 참여자별 유효 기록 소비량 중앙값 (최소 3회). 공동 순위(오차 범위)는 앱/Edge Function 에서 처리.
create or replace view weekly_leaderboard as
select
    route_id,
    powertrain,
    date_trunc('week', started_at) as week_start,
    user_id,
    count(*)                                             as records,
    percentile_cont(0.5) within group (order by consumption) as median_consumption
from drive_records
where status = 'QUALIFIED'
group by route_id, powertrain, date_trunc('week', started_at), user_id
having count(*) >= 3;

-- RLS: 본인 기록만 쓰기/읽기. 순위 view 는 별도 공개 정책(가명 표시)으로 노출.
alter table drive_records enable row level security;
create policy "own records read"   on drive_records for select using (auth.uid() = user_id);
create policy "own records insert" on drive_records for insert with check (auth.uid() = user_id);

alter table mission_rewards enable row level security;
create policy "own rewards read" on mission_rewards for select using (auth.uid() = user_id);
