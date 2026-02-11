create table if not exists hw_environments (
  id uuid primary key,
  name text not null unique
);

insert into hw_environments(id, name)
values
  ('11111111-1111-1111-1111-111111111111', 'NFT-01'),
  ('22222222-2222-2222-2222-222222222222', 'NFT-02'),
  ('33333333-3333-3333-3333-333333333333', 'Release-01')
on conflict (id) do nothing;

