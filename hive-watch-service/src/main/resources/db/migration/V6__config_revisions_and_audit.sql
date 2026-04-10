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
