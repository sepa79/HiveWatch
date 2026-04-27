create table if not exists hw_meta (
  key text primary key,
  value text not null,
  updated_at timestamptz not null default now()
);

create table if not exists hw_environments (
  id uuid primary key,
  name text not null unique
);

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
  tomcat_version text null,
  java_version text null,
  os text null,
  webapps jsonb not null
);

create table if not exists hw_actuator_targets (
  id uuid primary key,
  server_id uuid not null references hw_servers(id) on delete cascade,
  role text not null,
  base_url text not null,
  port integer not null,
  profile text not null,
  connect_timeout_ms integer not null,
  request_timeout_ms integer not null,
  created_at timestamptz not null default now(),

  constraint hw_actuator_targets_port_range check (port >= 1 and port <= 65535),
  constraint hw_actuator_targets_timeouts check (connect_timeout_ms > 0 and request_timeout_ms > 0),
  constraint hw_actuator_targets_server_role_unique unique (server_id, role)
);

create index if not exists hw_actuator_targets_server_id_idx on hw_actuator_targets(server_id);

create table if not exists hw_actuator_target_scan_state (
  target_id uuid primary key references hw_actuator_targets(id) on delete cascade,
  scanned_at timestamptz not null,
  outcome_kind text not null,
  error_kind text null,
  error_message text null,
  health_status text null,
  app_name text null,
  build_version text null,
  cpu_usage double precision null,
  memory_used_bytes bigint null
);

create table if not exists hw_users (
  id uuid primary key,
  username text not null unique,
  display_name text not null,
  active boolean not null,
  created_at timestamptz not null default now()
);

create table if not exists hw_user_roles (
  id uuid primary key,
  user_id uuid not null references hw_users(id) on delete cascade,
  role text not null,
  created_at timestamptz not null default now(),

  constraint hw_user_roles_user_role_unique unique (user_id, role)
);

create index if not exists hw_user_roles_user_id_idx on hw_user_roles(user_id);

create table if not exists hw_user_environment_visibility (
  id uuid primary key,
  user_id uuid not null references hw_users(id) on delete cascade,
  environment_id uuid not null references hw_environments(id) on delete cascade,
  created_at timestamptz not null default now(),

  constraint hw_user_environment_visibility_unique unique (user_id, environment_id)
);

create index if not exists hw_user_environment_visibility_user_id_idx on hw_user_environment_visibility(user_id);
create index if not exists hw_user_environment_visibility_environment_id_idx on hw_user_environment_visibility(environment_id);

create table if not exists hw_tomcat_expected_webapps (
  id uuid primary key,
  server_id uuid not null references hw_servers(id) on delete cascade,
  role text not null,
  path text not null,
  created_at timestamptz not null default now(),

  constraint hw_tomcat_expected_webapps_path_not_empty check (length(trim(path)) > 0),
  constraint hw_tomcat_expected_webapps_path_format check (left(path, 1) = '/'),
  constraint hw_tomcat_expected_webapps_unique unique (server_id, role, path)
);

create index if not exists hw_tomcat_expected_webapps_server_id_idx on hw_tomcat_expected_webapps(server_id);

create table if not exists hw_expected_set_templates (
  id uuid primary key,
  kind text not null,
  name text not null,
  created_at timestamptz not null default now(),

  constraint hw_expected_set_templates_name_not_empty check (length(trim(name)) > 0),
  constraint hw_expected_set_templates_kind_name_unique unique (kind, name)
);

create index if not exists hw_expected_set_templates_kind_idx on hw_expected_set_templates(kind);

create table if not exists hw_expected_set_template_items (
  id uuid primary key,
  template_id uuid not null references hw_expected_set_templates(id) on delete cascade,
  value text not null,
  created_at timestamptz not null default now(),

  constraint hw_expected_set_template_items_value_not_empty check (length(trim(value)) > 0),
  constraint hw_expected_set_template_items_unique unique (template_id, value)
);

create index if not exists hw_expected_set_template_items_template_id_idx on hw_expected_set_template_items(template_id);

create table if not exists hw_tomcat_expected_webapp_specs (
  id uuid primary key,
  server_id uuid not null references hw_servers(id) on delete cascade,
  role text not null,
  mode text not null,
  template_id uuid null references hw_expected_set_templates(id) on delete set null,
  created_at timestamptz not null default now(),

  constraint hw_tomcat_expected_webapp_specs_unique unique (server_id, role)
);

create index if not exists hw_tomcat_expected_webapp_specs_server_id_idx on hw_tomcat_expected_webapp_specs(server_id);

create table if not exists hw_docker_expected_service_specs (
  id uuid primary key,
  server_id uuid not null references hw_servers(id) on delete cascade,
  mode text not null,
  template_id uuid null references hw_expected_set_templates(id) on delete set null,
  created_at timestamptz not null default now(),

  constraint hw_docker_expected_service_specs_unique unique (server_id)
);

create index if not exists hw_docker_expected_service_specs_server_id_idx on hw_docker_expected_service_specs(server_id);

create table if not exists hw_docker_expected_services (
  id uuid primary key,
  server_id uuid not null references hw_servers(id) on delete cascade,
  profile text not null,
  created_at timestamptz not null default now(),

  constraint hw_docker_expected_services_profile_not_empty check (length(trim(profile)) > 0),
  constraint hw_docker_expected_services_unique unique (server_id, profile)
);

create index if not exists hw_docker_expected_services_server_id_idx on hw_docker_expected_services(server_id);

create table if not exists hw_config_revisions (
  id uuid primary key,
  environment_id uuid not null references hw_environments(id) on delete cascade,
  revision_type text not null,
  actor_user_id uuid not null references hw_users(id) on delete restrict,
  actor_username text not null,
  source text not null,
  correlation_id text null,
  reason text null,
  plan_hash text not null,
  plan_json text not null,
  summary_json text not null,
  created_at timestamptz not null default now(),

  constraint hw_config_revisions_revision_type_not_empty check (length(trim(revision_type)) > 0),
  constraint hw_config_revisions_actor_username_not_empty check (length(trim(actor_username)) > 0),
  constraint hw_config_revisions_source_not_empty check (length(trim(source)) > 0),
  constraint hw_config_revisions_plan_hash_not_empty check (length(trim(plan_hash)) > 0),
  constraint hw_config_revisions_plan_json_not_empty check (length(trim(plan_json)) > 0),
  constraint hw_config_revisions_summary_json_not_empty check (length(trim(summary_json)) > 0)
);

create index if not exists hw_config_revisions_environment_id_created_at_idx on hw_config_revisions(environment_id, created_at desc);

create table if not exists hw_audit_events (
  id uuid primary key,
  revision_id uuid null references hw_config_revisions(id) on delete set null,
  environment_id uuid null references hw_environments(id) on delete set null,
  actor_user_id uuid not null references hw_users(id) on delete restrict,
  actor_username text not null,
  action text not null,
  object_type text not null,
  object_id uuid null,
  object_label text not null,
  source text not null,
  correlation_id text null,
  details_json text not null,
  created_at timestamptz not null default now(),

  constraint hw_audit_events_actor_username_not_empty check (length(trim(actor_username)) > 0),
  constraint hw_audit_events_action_not_empty check (length(trim(action)) > 0),
  constraint hw_audit_events_object_type_not_empty check (length(trim(object_type)) > 0),
  constraint hw_audit_events_object_label_not_empty check (length(trim(object_label)) > 0),
  constraint hw_audit_events_source_not_empty check (length(trim(source)) > 0),
  constraint hw_audit_events_details_json_not_empty check (length(trim(details_json)) > 0)
);

create index if not exists hw_audit_events_environment_id_created_at_idx on hw_audit_events(environment_id, created_at desc);
create index if not exists hw_audit_events_revision_id_idx on hw_audit_events(revision_id);

insert into hw_environments(id, name)
values
  ('11111111-1111-1111-1111-111111111111', 'NFT-01'),
  ('22222222-2222-2222-2222-222222222222', 'NFT-02'),
  ('33333333-3333-3333-3333-333333333333', 'Release-01')
on conflict (id) do nothing;

insert into hw_servers(id, environment_id, name)
values
  ('11111111-1111-1111-1111-111111110001', '11111111-1111-1111-1111-111111111111', 'Touchpoint'),
  ('11111111-1111-1111-1111-111111110002', '11111111-1111-1111-1111-111111111111', 'Services'),
  ('11111111-1111-1111-1111-111111110003', '11111111-1111-1111-1111-111111111111', 'Docker Swarm'),
  ('22222222-2222-2222-2222-222222220001', '22222222-2222-2222-2222-222222222222', 'All-in-one'),
  ('22222222-2222-2222-2222-222222220002', '22222222-2222-2222-2222-222222222222', 'Docker Swarm'),
  ('33333333-3333-3333-3333-333333330001', '33333333-3333-3333-3333-333333333333', 'All-in-one'),
  ('33333333-3333-3333-3333-333333330002', '33333333-3333-3333-3333-333333333333', 'Docker Swarm')
on conflict (id) do nothing;

insert into hw_tomcat_targets(
  id, server_id, role, base_url, port, username, password, connect_timeout_ms, request_timeout_ms
)
values
  ('11111111-1111-1111-1111-111111110101', '11111111-1111-1111-1111-111111110001', 'PAYMENTS', 'http://hc-dummy-nft-01-touchpoint-tomcats', 8081, 'hc-manager', 'hc-manager-pass', 1500, 5000),
  ('11111111-1111-1111-1111-111111110102', '11111111-1111-1111-1111-111111110001', 'SERVICES', 'http://hc-dummy-nft-01-touchpoint-tomcats', 8082, 'hc-manager', 'hc-manager-pass', 1500, 5000),
  ('11111111-1111-1111-1111-111111110103', '11111111-1111-1111-1111-111111110001', 'AUTH', 'http://hc-dummy-nft-01-touchpoint-tomcats', 8083, 'hc-manager', 'hc-manager-pass', 1500, 5000),
  ('11111111-1111-1111-1111-111111110201', '11111111-1111-1111-1111-111111110002', 'PAYMENTS', 'http://hc-dummy-nft-01-services-tomcats', 8081, 'hc-manager', 'hc-manager-pass', 1500, 5000),
  ('11111111-1111-1111-1111-111111110202', '11111111-1111-1111-1111-111111110002', 'SERVICES', 'http://hc-dummy-nft-01-services-tomcats', 8082, 'hc-manager', 'hc-manager-pass', 1500, 5000),
  ('11111111-1111-1111-1111-111111110203', '11111111-1111-1111-1111-111111110002', 'AUTH', 'http://hc-dummy-nft-01-services-tomcats', 8083, 'hc-manager', 'hc-manager-pass', 1500, 5000),
  ('22222222-2222-2222-2222-222222220101', '22222222-2222-2222-2222-222222220001', 'PAYMENTS', 'http://hc-dummy-nft-02-all-in-one-tomcats', 8081, 'hc-manager', 'hc-manager-pass', 1500, 5000),
  ('22222222-2222-2222-2222-222222220102', '22222222-2222-2222-2222-222222220001', 'SERVICES', 'http://hc-dummy-nft-02-all-in-one-tomcats', 8082, 'hc-manager', 'hc-manager-pass', 1500, 5000),
  ('22222222-2222-2222-2222-222222220103', '22222222-2222-2222-2222-222222220001', 'AUTH', 'http://hc-dummy-nft-02-all-in-one-tomcats', 8083, 'hc-manager', 'hc-manager-pass', 1500, 5000),
  ('33333333-3333-3333-3333-333333330101', '33333333-3333-3333-3333-333333330001', 'PAYMENTS', 'http://hc-dummy-release-01-all-in-one-tomcats', 8081, 'hc-manager', 'hc-manager-pass', 1500, 5000),
  ('33333333-3333-3333-3333-333333330102', '33333333-3333-3333-3333-333333330001', 'SERVICES', 'http://hc-dummy-release-01-all-in-one-tomcats', 8082, 'hc-manager', 'hc-manager-pass', 1500, 5000),
  ('33333333-3333-3333-3333-333333330103', '33333333-3333-3333-3333-333333330001', 'AUTH', 'http://hc-dummy-release-01-all-in-one-tomcats', 8083, 'hc-manager', 'hc-manager-pass', 1500, 5000)
on conflict (id) do nothing;

insert into hw_actuator_targets(
  id, server_id, role, base_url, port, profile, connect_timeout_ms, request_timeout_ms
)
values
  ('11111111-1111-1111-1111-111111110301', '11111111-1111-1111-1111-111111110003', 'PAYMENTS', 'http://hc-dummy-nft-01-docker-swarm-microservices', 8080, 'payments', 1500, 5000),
  ('11111111-1111-1111-1111-111111110302', '11111111-1111-1111-1111-111111110003', 'SERVICES', 'http://hc-dummy-nft-01-docker-swarm-microservices', 8080, 'services', 1500, 5000),
  ('11111111-1111-1111-1111-111111110303', '11111111-1111-1111-1111-111111110003', 'AUTH', 'http://hc-dummy-nft-01-docker-swarm-microservices', 8080, 'auth', 1500, 5000),
  ('22222222-2222-2222-2222-222222220201', '22222222-2222-2222-2222-222222220002', 'PAYMENTS', 'http://hc-dummy-nft-02-docker-swarm-microservices', 8080, 'payments', 1500, 5000),
  ('22222222-2222-2222-2222-222222220202', '22222222-2222-2222-2222-222222220002', 'SERVICES', 'http://hc-dummy-nft-02-docker-swarm-microservices', 8080, 'services', 1500, 5000),
  ('22222222-2222-2222-2222-222222220203', '22222222-2222-2222-2222-222222220002', 'AUTH', 'http://hc-dummy-nft-02-docker-swarm-microservices', 8080, 'auth', 1500, 5000),
  ('33333333-3333-3333-3333-333333330201', '33333333-3333-3333-3333-333333330002', 'PAYMENTS', 'http://hc-dummy-release-01-docker-swarm-microservices', 8080, 'payments', 1500, 5000),
  ('33333333-3333-3333-3333-333333330202', '33333333-3333-3333-3333-333333330002', 'SERVICES', 'http://hc-dummy-release-01-docker-swarm-microservices', 8080, 'services', 1500, 5000),
  ('33333333-3333-3333-3333-333333330203', '33333333-3333-3333-3333-333333330002', 'AUTH', 'http://hc-dummy-release-01-docker-swarm-microservices', 8080, 'auth', 1500, 5000)
on conflict (id) do nothing;

insert into hw_users(id, username, display_name, active)
values
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'local-admin', 'Local Admin', true),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'local-operator', 'Local Operator', true),
  ('cccccccc-cccc-cccc-cccc-cccccccccccc', 'local-viewer', 'Local Viewer', true)
on conflict (id) do nothing;

insert into hw_user_roles(id, user_id, role)
values
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0001', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'ADMIN'),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0001', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'OPERATOR'),
  ('cccccccc-cccc-cccc-cccc-cccccccc0001', 'cccccccc-cccc-cccc-cccc-cccccccccccc', 'VIEWER')
on conflict (id) do nothing;

insert into hw_user_environment_visibility(id, user_id, environment_id)
values
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0101', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '11111111-1111-1111-1111-111111111111'),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0102', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '22222222-2222-2222-2222-222222222222'),
  ('cccccccc-cccc-cccc-cccc-cccccccc0101', 'cccccccc-cccc-cccc-cccc-cccccccccccc', '33333333-3333-3333-3333-333333333333')
on conflict (id) do nothing;

insert into hw_tomcat_expected_webapps(id, server_id, role, path)
values
  ('11111111-1111-1111-1111-11111111a101', '11111111-1111-1111-1111-111111110001', 'PAYMENTS', '/PaymentApp1'),
  ('11111111-1111-1111-1111-11111111a102', '11111111-1111-1111-1111-111111110001', 'PAYMENTS', '/PaymentApp2'),
  ('11111111-1111-1111-1111-11111111a103', '11111111-1111-1111-1111-111111110001', 'PAYMENTS', '/SharedPaymentsApp'),
  ('11111111-1111-1111-1111-11111111a111', '11111111-1111-1111-1111-111111110001', 'SERVICES', '/ServicesApp1'),
  ('11111111-1111-1111-1111-11111111a112', '11111111-1111-1111-1111-111111110001', 'SERVICES', '/TouchpointGateway'),
  ('11111111-1111-1111-1111-11111111a113', '11111111-1111-1111-1111-111111110001', 'SERVICES', '/SharedServicesApp'),
  ('11111111-1111-1111-1111-11111111a121', '11111111-1111-1111-1111-111111110001', 'AUTH', '/AuthApp1'),
  ('11111111-1111-1111-1111-11111111a122', '11111111-1111-1111-1111-111111110001', 'AUTH', '/SSOConsole'),
  ('11111111-1111-1111-1111-11111111a123', '11111111-1111-1111-1111-111111110001', 'AUTH', '/SharedAuthApp'),
  ('11111111-1111-1111-1111-11111111b101', '11111111-1111-1111-1111-111111110002', 'PAYMENTS', '/PaymentApp2'),
  ('11111111-1111-1111-1111-11111111b102', '11111111-1111-1111-1111-111111110002', 'PAYMENTS', '/PaymentApp3'),
  ('11111111-1111-1111-1111-11111111b103', '11111111-1111-1111-1111-111111110002', 'PAYMENTS', '/SharedPaymentsApp'),
  ('11111111-1111-1111-1111-11111111b111', '11111111-1111-1111-1111-111111110002', 'SERVICES', '/ServicesApp1'),
  ('11111111-1111-1111-1111-11111111b112', '11111111-1111-1111-1111-111111110002', 'SERVICES', '/ServicesApp2'),
  ('11111111-1111-1111-1111-11111111b113', '11111111-1111-1111-1111-111111110002', 'SERVICES', '/SharedServicesApp'),
  ('11111111-1111-1111-1111-11111111b121', '11111111-1111-1111-1111-111111110002', 'AUTH', '/AuthApp1'),
  ('11111111-1111-1111-1111-11111111b122', '11111111-1111-1111-1111-111111110002', 'AUTH', '/AuthApp2'),
  ('11111111-1111-1111-1111-11111111b123', '11111111-1111-1111-1111-111111110002', 'AUTH', '/SharedAuthApp'),
  ('22222222-2222-2222-2222-22222222a101', '22222222-2222-2222-2222-222222220001', 'PAYMENTS', '/PaymentApp1'),
  ('22222222-2222-2222-2222-22222222a102', '22222222-2222-2222-2222-222222220001', 'PAYMENTS', '/PaymentApp2'),
  ('22222222-2222-2222-2222-22222222a103', '22222222-2222-2222-2222-222222220001', 'PAYMENTS', '/PaymentApp3'),
  ('22222222-2222-2222-2222-22222222a104', '22222222-2222-2222-2222-222222220001', 'PAYMENTS', '/SharedPaymentsApp'),
  ('22222222-2222-2222-2222-22222222a111', '22222222-2222-2222-2222-222222220001', 'SERVICES', '/ServicesApp1'),
  ('22222222-2222-2222-2222-22222222a112', '22222222-2222-2222-2222-222222220001', 'SERVICES', '/ServicesApp2'),
  ('22222222-2222-2222-2222-22222222a113', '22222222-2222-2222-2222-222222220001', 'SERVICES', '/TouchpointGateway'),
  ('22222222-2222-2222-2222-22222222a114', '22222222-2222-2222-2222-222222220001', 'SERVICES', '/SharedServicesApp'),
  ('22222222-2222-2222-2222-22222222a121', '22222222-2222-2222-2222-222222220001', 'AUTH', '/AuthApp1'),
  ('22222222-2222-2222-2222-22222222a122', '22222222-2222-2222-2222-222222220001', 'AUTH', '/AuthApp2'),
  ('22222222-2222-2222-2222-22222222a123', '22222222-2222-2222-2222-222222220001', 'AUTH', '/SSOConsole'),
  ('22222222-2222-2222-2222-22222222a124', '22222222-2222-2222-2222-222222220001', 'AUTH', '/SharedAuthApp'),
  ('33333333-3333-3333-3333-33333333a101', '33333333-3333-3333-3333-333333330001', 'PAYMENTS', '/PaymentApp1'),
  ('33333333-3333-3333-3333-33333333a102', '33333333-3333-3333-3333-333333330001', 'PAYMENTS', '/PaymentApp2'),
  ('33333333-3333-3333-3333-33333333a103', '33333333-3333-3333-3333-333333330001', 'PAYMENTS', '/PaymentApp3'),
  ('33333333-3333-3333-3333-33333333a104', '33333333-3333-3333-3333-333333330001', 'PAYMENTS', '/SharedPaymentsApp'),
  ('33333333-3333-3333-3333-33333333a111', '33333333-3333-3333-3333-333333330001', 'SERVICES', '/ServicesApp1'),
  ('33333333-3333-3333-3333-33333333a112', '33333333-3333-3333-3333-333333330001', 'SERVICES', '/ServicesApp2'),
  ('33333333-3333-3333-3333-33333333a113', '33333333-3333-3333-3333-333333330001', 'SERVICES', '/TouchpointGateway'),
  ('33333333-3333-3333-3333-33333333a114', '33333333-3333-3333-3333-333333330001', 'SERVICES', '/SharedServicesApp'),
  ('33333333-3333-3333-3333-33333333a121', '33333333-3333-3333-3333-333333330001', 'AUTH', '/AuthApp1'),
  ('33333333-3333-3333-3333-33333333a122', '33333333-3333-3333-3333-333333330001', 'AUTH', '/AuthApp2'),
  ('33333333-3333-3333-3333-33333333a123', '33333333-3333-3333-3333-333333330001', 'AUTH', '/SSOConsole'),
  ('33333333-3333-3333-3333-33333333a124', '33333333-3333-3333-3333-333333330001', 'AUTH', '/SharedAuthApp')
on conflict (id) do nothing;

insert into hw_expected_set_templates(id, kind, name)
values
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1001', 'TOMCAT_WEBAPP_PATH', 'touchpoint-payments'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1002', 'TOMCAT_WEBAPP_PATH', 'touchpoint-services'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1003', 'TOMCAT_WEBAPP_PATH', 'touchpoint-auth'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1101', 'TOMCAT_WEBAPP_PATH', 'services-payments'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1102', 'TOMCAT_WEBAPP_PATH', 'services-services'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1103', 'TOMCAT_WEBAPP_PATH', 'services-auth'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1201', 'TOMCAT_WEBAPP_PATH', 'all-in-one-payments'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1202', 'TOMCAT_WEBAPP_PATH', 'all-in-one-services'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1203', 'TOMCAT_WEBAPP_PATH', 'all-in-one-auth'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa2001', 'DOCKER_SERVICE_PROFILE', 'docker-basic')
on conflict (id) do nothing;

insert into hw_expected_set_template_items(id, template_id, value)
values
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3001', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1001', '/PaymentApp1'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3002', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1001', '/PaymentApp2'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3003', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1001', '/SharedPaymentsApp'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3011', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1002', '/ServicesApp1'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3012', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1002', '/TouchpointGateway'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3013', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1002', '/SharedServicesApp'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3021', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1003', '/AuthApp1'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3022', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1003', '/SSOConsole'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3023', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1003', '/SharedAuthApp'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3101', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1101', '/PaymentApp2'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3102', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1101', '/PaymentApp3'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3103', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1101', '/SharedPaymentsApp'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1102', '/ServicesApp1'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3112', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1102', '/ServicesApp2'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3113', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1102', '/SharedServicesApp'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3121', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1103', '/AuthApp1'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3122', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1103', '/AuthApp2'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3123', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1103', '/SharedAuthApp'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3201', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1201', '/PaymentApp1'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3202', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1201', '/PaymentApp2'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3203', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1201', '/PaymentApp3'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3204', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1201', '/SharedPaymentsApp'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3211', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1202', '/ServicesApp1'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3212', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1202', '/ServicesApp2'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3213', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1202', '/TouchpointGateway'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3214', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1202', '/SharedServicesApp'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3221', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1203', '/AuthApp1'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3222', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1203', '/AuthApp2'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3223', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1203', '/SSOConsole'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3224', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1203', '/SharedAuthApp'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa4001', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa2001', 'payments'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa4002', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa2001', 'services'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa4003', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa2001', 'auth')
on conflict (id) do nothing;

insert into hw_tomcat_expected_webapp_specs(id, server_id, role, mode, template_id)
values
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0101', '11111111-1111-1111-1111-111111110001', 'PAYMENTS', 'EXPLICIT', null),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0102', '11111111-1111-1111-1111-111111110001', 'SERVICES', 'EXPLICIT', null),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0103', '11111111-1111-1111-1111-111111110001', 'AUTH', 'EXPLICIT', null),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0201', '11111111-1111-1111-1111-111111110002', 'PAYMENTS', 'EXPLICIT', null),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0202', '11111111-1111-1111-1111-111111110002', 'SERVICES', 'EXPLICIT', null),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0203', '11111111-1111-1111-1111-111111110002', 'AUTH', 'EXPLICIT', null),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0301', '22222222-2222-2222-2222-222222220001', 'PAYMENTS', 'EXPLICIT', null),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0302', '22222222-2222-2222-2222-222222220001', 'SERVICES', 'EXPLICIT', null),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0303', '22222222-2222-2222-2222-222222220001', 'AUTH', 'EXPLICIT', null),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0401', '33333333-3333-3333-3333-333333330001', 'PAYMENTS', 'EXPLICIT', null),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0402', '33333333-3333-3333-3333-333333330001', 'SERVICES', 'EXPLICIT', null),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0403', '33333333-3333-3333-3333-333333330001', 'AUTH', 'EXPLICIT', null)
on conflict (id) do nothing;

insert into hw_docker_expected_service_specs(id, server_id, mode, template_id)
values
  ('cccccccc-cccc-cccc-cccc-cccccccc0101', '11111111-1111-1111-1111-111111110003', 'EXPLICIT', null),
  ('cccccccc-cccc-cccc-cccc-cccccccc0201', '22222222-2222-2222-2222-222222220002', 'EXPLICIT', null),
  ('cccccccc-cccc-cccc-cccc-cccccccc0301', '33333333-3333-3333-3333-333333330002', 'EXPLICIT', null)
on conflict (id) do nothing;

insert into hw_docker_expected_services(id, server_id, profile)
values
  ('dddddddd-dddd-dddd-dddd-dddddddd0101', '11111111-1111-1111-1111-111111110003', 'payments'),
  ('dddddddd-dddd-dddd-dddd-dddddddd0102', '11111111-1111-1111-1111-111111110003', 'services'),
  ('dddddddd-dddd-dddd-dddd-dddddddd0103', '11111111-1111-1111-1111-111111110003', 'auth'),
  ('dddddddd-dddd-dddd-dddd-dddddddd0201', '22222222-2222-2222-2222-222222220002', 'payments'),
  ('dddddddd-dddd-dddd-dddd-dddddddd0202', '22222222-2222-2222-2222-222222220002', 'services'),
  ('dddddddd-dddd-dddd-dddd-dddddddd0203', '22222222-2222-2222-2222-222222220002', 'auth'),
  ('dddddddd-dddd-dddd-dddd-dddddddd0301', '33333333-3333-3333-3333-333333330002', 'payments'),
  ('dddddddd-dddd-dddd-dddd-dddddddd0302', '33333333-3333-3333-3333-333333330002', 'services'),
  ('dddddddd-dddd-dddd-dddd-dddddddd0303', '33333333-3333-3333-3333-333333330002', 'auth')
on conflict (id) do nothing;
