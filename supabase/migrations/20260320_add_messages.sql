-- Migration: create messages table for per-match chat

create table if not exists public.messages (
    id uuid primary key default gen_random_uuid(),
    match_id uuid not null references public.matches(id) on delete cascade,
    sender_id uuid not null references public.users(id) on delete cascade,
    content text,
    image_url text,
    read_at timestamptz default null,
    created_at timestamptz default now()
);

create index if not exists messages_match_id_idx on public.messages(match_id, created_at asc);

do $$
begin
    if not exists (
        select 1
        from pg_publication_tables
        where pubname = 'supabase_realtime'
          and schemaname = 'public'
          and tablename = 'messages'
    ) then
        alter publication supabase_realtime add table public.messages;
    end if;
end $$;

alter table public.messages enable row level security;

do $$
begin
    if not exists (
        select 1 from pg_policies
        where schemaname = 'public'
          and tablename = 'messages'
          and policyname = 'match participants can read messages'
    ) then
        create policy "match participants can read messages"
        on public.messages for select
        using (
            auth.uid() in (
                select buyer_id from public.matches where id = match_id
                union
                select supplier_id from public.listings
                where id = (select listing_id from public.matches where id = match_id)
            )
        );
    end if;

    if not exists (
        select 1 from pg_policies
        where schemaname = 'public'
          and tablename = 'messages'
          and policyname = 'match participants can send messages'
    ) then
        create policy "match participants can send messages"
        on public.messages for insert
        with check (
            auth.uid() = sender_id and
            auth.uid() in (
                select buyer_id from public.matches where id = match_id
                union
                select supplier_id from public.listings
                where id = (select listing_id from public.matches where id = match_id)
            )
        );
    end if;

    if not exists (
        select 1 from pg_policies
        where schemaname = 'public'
          and tablename = 'messages'
          and policyname = 'recipient can mark read'
    ) then
        create policy "recipient can mark read"
        on public.messages for update
        using (auth.uid() != sender_id)
        with check (auth.uid() != sender_id);
    end if;

    if not exists (
        select 1 from pg_policies
        where schemaname = 'public'
          and tablename = 'messages'
          and policyname = 'match participants can delete messages'
    ) then
        create policy "match participants can delete messages"
        on public.messages for delete
        using (
            auth.uid() in (
                select buyer_id from public.matches where id = match_id
                union
                select supplier_id from public.listings
                where id = (select listing_id from public.matches where id = match_id)
            )
        );
    end if;
end $$;
