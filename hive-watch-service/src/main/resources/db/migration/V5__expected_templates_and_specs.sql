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

-- Dev seed: templates (Tomcat webapps)
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
  -- touchpoint-payments
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3001', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1001', '/PaymentApp1'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3002', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1001', '/PaymentApp2'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3003', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1001', '/SharedPaymentsApp'),
  -- touchpoint-services
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3011', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1002', '/ServicesApp1'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3012', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1002', '/TouchpointGateway'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3013', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1002', '/SharedServicesApp'),
  -- touchpoint-auth
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3021', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1003', '/AuthApp1'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3022', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1003', '/SSOConsole'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3023', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1003', '/SharedAuthApp'),

  -- services-payments
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3101', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1101', '/PaymentApp2'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3102', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1101', '/PaymentApp3'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3103', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1101', '/SharedPaymentsApp'),
  -- services-services
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3111', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1102', '/ServicesApp1'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3112', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1102', '/ServicesApp2'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3113', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1102', '/SharedServicesApp'),
  -- services-auth
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3121', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1103', '/AuthApp1'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3122', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1103', '/AuthApp2'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3123', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1103', '/SharedAuthApp'),

  -- all-in-one-payments
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3201', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1201', '/PaymentApp1'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3202', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1201', '/PaymentApp2'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3203', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1201', '/PaymentApp3'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3204', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1201', '/SharedPaymentsApp'),
  -- all-in-one-services
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3211', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1202', '/ServicesApp1'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3212', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1202', '/ServicesApp2'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3213', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1202', '/TouchpointGateway'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3214', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1202', '/SharedServicesApp'),
  -- all-in-one-auth
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3221', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1203', '/AuthApp1'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3222', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1203', '/AuthApp2'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3223', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1203', '/SSOConsole'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa3224', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1203', '/SharedAuthApp'),

  -- docker-basic
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa4001', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa2001', 'payments'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa4002', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa2001', 'services'),
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa4003', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa2001', 'auth')
on conflict (id) do nothing;

-- Dev seed: specs (keep defaults explicit; templates are optional)
insert into hw_tomcat_expected_webapp_specs(id, server_id, role, mode, template_id)
values
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0101', '11111111-1111-1111-1111-111111110001', 'PAYMENTS', 'EXPLICIT', null),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0102', '11111111-1111-1111-1111-111111110001', 'SERVICES', 'EXPLICIT', null),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0103', '11111111-1111-1111-1111-111111110001', 'AUTH',     'EXPLICIT', null),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0201', '11111111-1111-1111-1111-111111110002', 'PAYMENTS', 'EXPLICIT', null),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0202', '11111111-1111-1111-1111-111111110002', 'SERVICES', 'EXPLICIT', null),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0203', '11111111-1111-1111-1111-111111110002', 'AUTH',     'EXPLICIT', null),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0301', '22222222-2222-2222-2222-222222220001', 'PAYMENTS', 'EXPLICIT', null),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0302', '22222222-2222-2222-2222-222222220001', 'SERVICES', 'EXPLICIT', null),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0303', '22222222-2222-2222-2222-222222220001', 'AUTH',     'EXPLICIT', null),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0401', '33333333-3333-3333-3333-333333330001', 'PAYMENTS', 'EXPLICIT', null),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0402', '33333333-3333-3333-3333-333333330001', 'SERVICES', 'EXPLICIT', null),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0403', '33333333-3333-3333-3333-333333330001', 'AUTH',     'EXPLICIT', null)
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

