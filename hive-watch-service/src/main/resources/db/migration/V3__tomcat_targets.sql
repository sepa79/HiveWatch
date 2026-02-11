create table if not exists hw_servers (
  id uuid primary key,
  environment_id uuid not null references hw_environments(id) on delete cascade,
  name text not null,
  created_at timestamptz not null default now(),

  constraint hw_servers_env_name_unique unique (environment_id, name)
);

create index if not exists hw_servers_environment_id_idx on hw_servers(environment_id);

create table if not exists hw_tomcat_targets (
  id uuid primary key,
  server_id uuid not null references hw_servers(id) on delete cascade,
  role text not null,
  base_url text not null,
  port integer not null,
  username text not null,
  password text not null,
  connect_timeout_ms integer not null,
  request_timeout_ms integer not null,
  created_at timestamptz not null default now(),

  constraint hw_tomcat_targets_port_range check (port >= 1 and port <= 65535),
  constraint hw_tomcat_targets_timeouts check (connect_timeout_ms > 0 and request_timeout_ms > 0),
  constraint hw_tomcat_targets_server_role_unique unique (server_id, role)
);

create index if not exists hw_tomcat_targets_server_id_idx on hw_tomcat_targets(server_id);

create table if not exists hw_tomcat_target_scan_state (
  target_id uuid primary key references hw_tomcat_targets(id) on delete cascade,
  scanned_at timestamptz not null,
  outcome_kind text not null,
  error_kind text null,
  error_message text null,
  webapps jsonb not null
);

-- Dev seed: environment → server → tomcat targets (payments/services/auth)
insert into hw_servers(id, environment_id, name)
values
  ('11111111-1111-1111-1111-111111110001', '11111111-1111-1111-1111-111111111111', 'Touchpoint'),
  ('11111111-1111-1111-1111-111111110002', '11111111-1111-1111-1111-111111111111', 'Services'),
  ('22222222-2222-2222-2222-222222220001', '22222222-2222-2222-2222-222222222222', 'All-in-one'),
  ('33333333-3333-3333-3333-333333330001', '33333333-3333-3333-3333-333333333333', 'All-in-one')
on conflict (id) do nothing;

insert into hw_tomcat_targets(
  id, server_id, role, base_url, port, username, password, connect_timeout_ms, request_timeout_ms
)
values
  -- NFT-01 / Touchpoint
  ('11111111-1111-1111-1111-111111110101', '11111111-1111-1111-1111-111111110001', 'PAYMENTS', 'http://hc-dummy-nft-01-touchpoint-tomcats', 8081, 'hc-manager', 'hc-manager-pass', 1500, 5000),
  ('11111111-1111-1111-1111-111111110102', '11111111-1111-1111-1111-111111110001', 'SERVICES', 'http://hc-dummy-nft-01-touchpoint-tomcats', 8082, 'hc-manager', 'hc-manager-pass', 1500, 5000),
  ('11111111-1111-1111-1111-111111110103', '11111111-1111-1111-1111-111111110001', 'AUTH',     'http://hc-dummy-nft-01-touchpoint-tomcats', 8083, 'hc-manager', 'hc-manager-pass', 1500, 5000),

  -- NFT-01 / Services
  ('11111111-1111-1111-1111-111111110201', '11111111-1111-1111-1111-111111110002', 'PAYMENTS', 'http://hc-dummy-nft-01-services-tomcats', 8081, 'hc-manager', 'hc-manager-pass', 1500, 5000),
  ('11111111-1111-1111-1111-111111110202', '11111111-1111-1111-1111-111111110002', 'SERVICES', 'http://hc-dummy-nft-01-services-tomcats', 8082, 'hc-manager', 'hc-manager-pass', 1500, 5000),
  ('11111111-1111-1111-1111-111111110203', '11111111-1111-1111-1111-111111110002', 'AUTH',     'http://hc-dummy-nft-01-services-tomcats', 8083, 'hc-manager', 'hc-manager-pass', 1500, 5000),

  -- NFT-02 / All-in-one
  ('22222222-2222-2222-2222-222222220101', '22222222-2222-2222-2222-222222220001', 'PAYMENTS', 'http://hc-dummy-nft-02-all-in-one-tomcats', 8081, 'hc-manager', 'hc-manager-pass', 1500, 5000),
  ('22222222-2222-2222-2222-222222220102', '22222222-2222-2222-2222-222222220001', 'SERVICES', 'http://hc-dummy-nft-02-all-in-one-tomcats', 8082, 'hc-manager', 'hc-manager-pass', 1500, 5000),
  ('22222222-2222-2222-2222-222222220103', '22222222-2222-2222-2222-222222220001', 'AUTH',     'http://hc-dummy-nft-02-all-in-one-tomcats', 8083, 'hc-manager', 'hc-manager-pass', 1500, 5000),

  -- Release-01 / All-in-one
  ('33333333-3333-3333-3333-333333330101', '33333333-3333-3333-3333-333333330001', 'PAYMENTS', 'http://hc-dummy-release-01-all-in-one-tomcats', 8081, 'hc-manager', 'hc-manager-pass', 1500, 5000),
  ('33333333-3333-3333-3333-333333330102', '33333333-3333-3333-3333-333333330001', 'SERVICES', 'http://hc-dummy-release-01-all-in-one-tomcats', 8082, 'hc-manager', 'hc-manager-pass', 1500, 5000),
  ('33333333-3333-3333-3333-333333330103', '33333333-3333-3333-3333-333333330001', 'AUTH',     'http://hc-dummy-release-01-all-in-one-tomcats', 8083, 'hc-manager', 'hc-manager-pass', 1500, 5000)
on conflict (id) do nothing;
