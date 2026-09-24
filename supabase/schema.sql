-- ヘルプアプリ用テーブル
-- Supabase ダッシュボードの SQL Editor に貼り付けて実行してください。

create table if not exists public.help_profiles (
    id                      uuid primary key default gen_random_uuid(),
    display_name            text not null,

    -- FeliCa カードの IDm（16 桁の 16 進数・大文字、区切りなし）例: 012E4CD1A2B3C4D5
    felica_idm              text unique,
    -- BLE タグの MAC アドレス（AA:BB:CC:DD:EE:FF）またはアドバタイズ名
    ble_id                  text unique,

    disability              text,  -- 障がい・特性      例: 聴覚障害
    help_request            text,  -- お手伝いしてほしいこと
    communication           text,  -- コミュニケーション方法 例: 筆談でお願いします
    blood_type              text,
    allergies               text,
    medications             text,
    medical_notes           text,
    emergency_contact_name  text,
    emergency_contact_phone text,

    created_at              timestamptz not null default now()
);

-- 照合キーは大文字にそろえる（アプリ側も大文字で比較する）
create or replace function public.help_profiles_normalize()
returns trigger language plpgsql as $$
begin
    new.felica_idm := upper(replace(trim(new.felica_idm), ' ', ''));
    new.ble_id     := upper(trim(new.ble_id));
    return new;
end $$;

drop trigger if exists help_profiles_normalize on public.help_profiles;
create trigger help_profiles_normalize
    before insert or update on public.help_profiles
    for each row execute function public.help_profiles_normalize();

-- 行レベルセキュリティ
alter table public.help_profiles enable row level security;

-- ⚠ プロトタイプ用：anon キーで読み取りのみ許可。
--   本番では医療情報などが第三者に読まれないよう、認証ユーザー限定や
--   必要な列だけ返す RPC 関数に切り替えてください。
drop policy if exists "read profiles" on public.help_profiles;
create policy "read profiles" on public.help_profiles
    for select to anon, authenticated using (true);

-- サンプルデータ（ble_id / felica_idm は自分のタグ・カードの値に変更してください）
insert into public.help_profiles
    (display_name, felica_idm, ble_id, disability, help_request, communication,
     blood_type, allergies, medications, medical_notes,
     emergency_contact_name, emergency_contact_phone)
values
    ('山田 花子', '012E4CD1A2B3C4D5', 'AA:BB:CC:DD:EE:FF', '聴覚障害',
     '駅のアナウンスが聞こえないので、遅延や変更があったら教えてください',
     '筆談、またはスマホの文字でお願いします',
     'A型', 'そば', null, null, '山田 太郎（夫）', '090-0000-0000')
on conflict do nothing;
