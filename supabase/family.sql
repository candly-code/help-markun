-- 家族アカウント（schema.sql の後に SQL Editor で実行してください）
--
-- ・アプリはログインなしでも使える。ログインすると「家族グループ」を作る／招待コードで参加できる
-- ・見守る方（help_profiles.family_id）は管理者が登録する（カードからは追加できない）
-- ・誰かがその方を見つけた・カードを読んだ・「家族に知らせる」を押すと help_events に記録され、
--   家族のアプリに通知が届く（家族以外は読めない）

create extension if not exists pgcrypto with schema extensions;

create table if not exists public.families (
    id          uuid primary key default gen_random_uuid(),
    name        text not null check (char_length(name) between 1 and 40),
    invite_code text not null unique,
    created_by  uuid references auth.users on delete set null,
    created_at  timestamptz not null default now()
);

-- 1 人が入れる家族グループは 1 つ
create table if not exists public.family_members (
    family_id    uuid not null references public.families on delete cascade,
    user_id      uuid not null unique references auth.users on delete cascade,
    display_name text not null check (char_length(display_name) between 1 and 40),
    joined_at    timestamptz not null default now(),
    primary key (family_id, user_id)
);

alter table public.help_profiles
    add column if not exists family_id uuid references public.families on delete set null;

create table if not exists public.help_events (
    id          bigint generated always as identity primary key,
    profile_id  uuid not null references public.help_profiles on delete cascade,
    family_id   uuid references public.families on delete cascade,
    kind        text not null check (kind in ('nearby', 'card', 'message')),
    message     text check (char_length(message) <= 200),
    sender_name text check (char_length(sender_name) <= 40),
    reporter    uuid default auth.uid(),
    created_at  timestamptz not null default now()
);
create index if not exists help_events_family_created on public.help_events (family_id, created_at desc);

-- ---------------------------------------------------------------------------
-- 判定用の関数（RLS から呼ぶので security definer）
-- ---------------------------------------------------------------------------

create or replace function public.my_family_id()
returns uuid language sql stable security definer set search_path = public as $$
    select family_id from public.family_members where user_id = auth.uid()
$$;

-- イベントは見守られている方の家族へ届ける。家族未登録の方、または短時間の重複は捨てる（いたずら・連打対策）
create or replace function public.help_events_route()
returns trigger language plpgsql security definer set search_path = public as $$
begin
    select family_id into new.family_id from public.help_profiles where id = new.profile_id;
    if new.family_id is null then
        return null;
    end if;
    new.reporter := auth.uid();
    if exists (
        select 1 from public.help_events e
        where e.profile_id = new.profile_id
          and e.kind = new.kind
          and e.created_at > now() - case new.kind when 'message' then interval '10 seconds' else interval '10 minutes' end
          and (new.kind <> 'message' or e.message is not distinct from new.message)
    ) then
        return null;
    end if;
    return new;
end $$;

drop trigger if exists help_events_route on public.help_events;
create trigger help_events_route before insert on public.help_events
    for each row execute function public.help_events_route();

-- ---------------------------------------------------------------------------
-- 行レベルセキュリティ
-- ---------------------------------------------------------------------------

alter table public.families enable row level security;
alter table public.family_members enable row level security;
alter table public.help_events enable row level security;

drop policy if exists "members read family" on public.families;
create policy "members read family" on public.families
    for select to authenticated using (id = (select public.my_family_id()));

drop policy if exists "members read members" on public.family_members;
create policy "members read members" on public.family_members
    for select to authenticated using (family_id = (select public.my_family_id()));

-- 見つけた人はログインしていなくても家族に知らせられる（宛先はトリガーが決める）
drop policy if exists "anyone reports" on public.help_events;
create policy "anyone reports" on public.help_events
    for insert to anon, authenticated with check (true);

drop policy if exists "family reads events" on public.help_events;
create policy "family reads events" on public.help_events
    for select to authenticated using (family_id = (select public.my_family_id()));

-- ---------------------------------------------------------------------------
-- 操作（RPC）
-- ---------------------------------------------------------------------------

-- 読み間違えにくい文字だけで 6 文字の招待コードを作る
create or replace function public.new_invite_code()
returns text language plpgsql volatile set search_path = public as $$
declare
    alphabet constant text := 'ACDEFGHJKLMNPQRTUVWXY34679';
    code text;
begin
    loop
        code := '';
        for i in 1..6 loop
            code := code || substr(alphabet, 1 + (get_byte(extensions.gen_random_bytes(1), 0) % length(alphabet)), 1);
        end loop;
        exit when not exists (select 1 from public.families where invite_code = code);
    end loop;
    return code;
end $$;

create or replace function public.create_family(p_name text, p_member_name text)
returns public.families language plpgsql security definer set search_path = public as $$
declare
    f public.families;
begin
    if auth.uid() is null then raise exception 'ログインが必要です'; end if;
    if exists (select 1 from public.family_members where user_id = auth.uid()) then
        raise exception 'すでに家族グループに参加しています';
    end if;
    insert into public.families (name, invite_code, created_by)
        values (trim(p_name), public.new_invite_code(), auth.uid())
        returning * into f;
    insert into public.family_members (family_id, user_id, display_name)
        values (f.id, auth.uid(), trim(p_member_name));
    return f;
end $$;

create or replace function public.join_family(p_code text, p_member_name text)
returns public.families language plpgsql security definer set search_path = public as $$
declare
    f public.families;
begin
    if auth.uid() is null then raise exception 'ログインが必要です'; end if;
    select * into f from public.families where invite_code = upper(replace(trim(p_code), ' ', ''));
    if f.id is null then raise exception '招待コードが見つかりません'; end if;
    delete from public.family_members where user_id = auth.uid();
    insert into public.family_members (family_id, user_id, display_name)
        values (f.id, auth.uid(), trim(p_member_name));
    return f;
end $$;

-- 家族グループを抜ける（最後の 1 人なら、グループごと消えて見守りの登録も外れる）
create or replace function public.leave_family()
returns void language plpgsql security definer set search_path = public as $$
declare
    fid uuid := public.my_family_id();
begin
    delete from public.family_members where user_id = auth.uid();
    if fid is not null and not exists (select 1 from public.family_members where family_id = fid) then
        delete from public.families where id = fid;
    end if;
end $$;

-- 見守る方の登録（help_profiles.family_id の設定）は管理者がダッシュボードから行う。
-- カードを読んだだけで誰かを自分の家族に追加できないよう、アプリからの登録手段は用意しない。
drop function if exists public.watch_profile(uuid);

create or replace function public.unwatch_profile(p_profile uuid)
returns void language plpgsql security definer set search_path = public as $$
begin
    update public.help_profiles set family_id = null
        where id = p_profile and family_id = public.my_family_id();
end $$;

revoke execute on function public.my_family_id() from public, anon;
grant execute on function public.my_family_id() to authenticated;
revoke execute on function public.new_invite_code() from public, anon, authenticated;
revoke execute on function public.create_family(text, text) from public, anon;
revoke execute on function public.join_family(text, text) from public, anon;
revoke execute on function public.leave_family() from public, anon;
revoke execute on function public.unwatch_profile(uuid) from public, anon;
revoke execute on function public.help_events_route() from public, anon, authenticated;
grant execute on function public.create_family(text, text) to authenticated;
grant execute on function public.join_family(text, text) to authenticated;
grant execute on function public.leave_family() to authenticated;
grant execute on function public.unwatch_profile(uuid) to authenticated;
