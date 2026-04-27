create table if not exists hw_environment_target_roles (
  id uuid primary key,
  environment_id uuid not null references hw_environments(id) on delete cascade,
  code text not null,
  label text not null,
  sort_order integer not null,
  active boolean not null default true,
  created_at timestamptz not null default now(),

  constraint hw_environment_target_roles_code_not_empty check (length(trim(code)) > 0),
  constraint hw_environment_target_roles_label_not_empty check (length(trim(label)) > 0),
  constraint hw_environment_target_roles_code_unique unique (environment_id, code)
);

create index if not exists hw_environment_target_roles_environment_id_idx on hw_environment_target_roles(environment_id);

insert into hw_environment_target_roles(id, environment_id, code, label, sort_order, active)
select md5(e.id::text || ':' || role.code)::uuid, e.id, role.code, role.label, role.sort_order, true
from hw_environments e
cross join (
  values
    ('AUTH', 'auth', 10),
    ('PAYMENTS', 'payments', 20),
    ('SERVICES', 'services', 30)
) as role(code, label, sort_order)
on conflict (environment_id, code) do nothing;
