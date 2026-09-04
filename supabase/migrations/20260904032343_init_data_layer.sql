-- Initial relational schema for the Firestore -> Supabase data-layer migration.
-- Mirrors the endpoint shapes consumed by NetworkService today:
--   shopping, shopping/{id}                              -> shopping_lists
--   shopping/{id}/products, shopping/{id}/products/{id}   -> shopping_list_products
--   users/{userId}/common-products, .../{id}              -> common_products
--   users/{id}                                            -> user_profiles
--   notifications                                          -> notifications
--
-- IDs stay `text` (Firebase uids / Firestore doc ids are not guaranteed RFC4122 UUIDs).
-- Timestamps stay `bigint` epoch millis, not `timestamptz`, so the existing Long-typed
-- @Serializable model fields don't need to change (PostgREST returns timestamptz as an
-- ISO-8601 string, which would break decoding).
-- Applied once already via the Supabase SQL Editor against `main` on 2026-09-03;
-- this file makes that schema reproducible/version-controlled.

create table public.shopping_lists (
  id            text primary key,
  title         text not null,
  description   text not null default '',
  price         numeric(12,2) not null default 0,
  status        text not null check (status in ('NEW','IN_PROGRESS','FINISH')),
  users         text[] not null default '{}',
  roles         jsonb not null default '{}',
  created_at    bigint not null,
  is_favorite   boolean not null default false,
  is_categorized boolean not null default true,
  short_code    text unique,
  budget        numeric(12,2),
  position      integer not null default 0,
  emoji         text not null default '🛒'
);
create index shopping_lists_users_gin on public.shopping_lists using gin (users);

create table public.shopping_list_products (
  id            text primary key,
  shopping_id   text not null references public.shopping_lists(id) on delete cascade,
  name          text not null,
  quantity      numeric(10,3) not null default 1,
  price         numeric(12,2),
  is_purchased  boolean not null default false,
  category      text not null default 'Outros',
  barcode       text,
  unit          text not null default 'un'
);
create index shopping_list_products_shopping_id_idx on public.shopping_list_products(shopping_id);

create table public.common_products (
  id       text primary key,
  user_id  text not null,
  name     text not null,
  category text not null default 'Outros',
  unit     text not null default 'un'
);
create index common_products_user_id_idx on public.common_products(user_id);

create table public.user_profiles (
  id        text primary key,
  name      text,
  email     text,
  photo_url text
);

create table public.notifications (
  id        text primary key,
  user_id   text not null,
  title     text not null,
  message   text not null,
  type      text not null,
  is_read   boolean not null default false,
  timestamp bigint not null
);
create index notifications_user_id_idx on public.notifications(user_id);

-- RLS: use (auth.jwt() ->> 'sub') explicitly rather than auth.uid(), since auth.uid()'s
-- behavior for third-party (Firebase) tokens needs live verification against current
-- Supabase docs -- see the Auth bridging follow-up in the migration plan about mapping
-- Firebase JWTs to the `authenticated` Postgres role.

alter table public.shopping_lists enable row level security;
create policy shopping_lists_member_access on public.shopping_lists
  for all using ((auth.jwt() ->> 'sub') = any(users))
  with check ((auth.jwt() ->> 'sub') = any(users));

alter table public.shopping_list_products enable row level security;
create policy shopping_list_products_via_parent on public.shopping_list_products
  for all using (exists (
    select 1 from public.shopping_lists sl
    where sl.id = shopping_list_products.shopping_id
      and (auth.jwt() ->> 'sub') = any(sl.users)
  ));

alter table public.common_products enable row level security;
create policy common_products_owner on public.common_products
  for all using (user_id = (auth.jwt() ->> 'sub'))
  with check (user_id = (auth.jwt() ->> 'sub'));

alter table public.user_profiles enable row level security;
create policy user_profiles_self on public.user_profiles
  for all using (id = (auth.jwt() ->> 'sub'))
  with check (id = (auth.jwt() ->> 'sub'));

alter table public.notifications enable row level security;
create policy notifications_owner on public.notifications
  for all using (user_id = (auth.jwt() ->> 'sub'))
  with check (user_id = (auth.jwt() ->> 'sub'));
