create table if not exists hw_tomcat_targets (
  id uuid primary key,
  environment_id uuid not null references hw_environments(id) on delete cascade,
  name text not null,
  base_url text not null,
  port integer not null,
  username text not null,
  password text not null,
  connect_timeout_ms integer not null,
  request_timeout_ms integer not null,
  created_at timestamptz not null default now(),

  constraint hw_tomcat_targets_port_range check (port >= 1 and port <= 65535),
  constraint hw_tomcat_targets_timeouts check (connect_timeout_ms > 0 and request_timeout_ms > 0),
  constraint hw_tomcat_targets_env_name_unique unique (environment_id, name)
);

create index if not exists hw_tomcat_targets_environment_id_idx on hw_tomcat_targets(environment_id);

create table if not exists hw_tomcat_target_scan_state (
  target_id uuid primary key references hw_tomcat_targets(id) on delete cascade,
  scanned_at timestamptz not null,
  outcome_kind text not null,
  error_kind text null,
  error_message text null,
  webapps jsonb not null
);

