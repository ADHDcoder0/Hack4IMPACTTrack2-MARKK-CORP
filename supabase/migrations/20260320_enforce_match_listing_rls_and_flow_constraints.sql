-- Enforce RLS and flow constraints for match/request lifecycle and supplier listing edits.
-- Targets app behaviors:
-- 1) Supplier confirm/reject from pending queue
-- 2) Rejected requests removed from supplier queue (app-side already filters)
-- 3) Buyer can revert only own pending request (delete)
-- 4) Supplier can edit only own listings

-- -----------------------------------------------------------------------------
-- 0) Status guardrails
-- -----------------------------------------------------------------------------
DO $$
BEGIN
    IF to_regclass('public.matches') IS NOT NULL THEN
        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'matches_status_allowed'
        ) THEN
            ALTER TABLE public.matches
            ADD CONSTRAINT matches_status_allowed
            CHECK (status IN ('pending', 'confirmed', 'rejected'));
        END IF;
    END IF;
END $$;

-- -----------------------------------------------------------------------------
-- 1) Performance indexes for request flows
-- -----------------------------------------------------------------------------
DO $$
BEGIN
    IF to_regclass('public.matches') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_matches_buyer_status
            ON public.matches (buyer_id, status, created_at DESC);
        CREATE INDEX IF NOT EXISTS idx_matches_listing_status_created
            ON public.matches (listing_id, status, created_at DESC);
    END IF;

    IF to_regclass('public.listings') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS idx_listings_supplier_created
            ON public.listings (supplier_id, created_at DESC);
    END IF;
END $$;

-- -----------------------------------------------------------------------------
-- 2) Enable RLS
-- -----------------------------------------------------------------------------
DO $$
BEGIN
    IF to_regclass('public.matches') IS NOT NULL THEN
        ALTER TABLE public.matches ENABLE ROW LEVEL SECURITY;
    END IF;

    IF to_regclass('public.listings') IS NOT NULL THEN
        ALTER TABLE public.listings ENABLE ROW LEVEL SECURITY;
    END IF;
END $$;

-- -----------------------------------------------------------------------------
-- 3) MATCHES policies
-- -----------------------------------------------------------------------------
DO $$
BEGIN
    IF to_regclass('public.matches') IS NULL OR to_regclass('public.listings') IS NULL THEN
        RETURN;
    END IF;

    -- Buyer can view own requests; supplier can view requests for own listings.
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies
        WHERE schemaname = 'public'
          AND tablename = 'matches'
          AND policyname = 'matches_select_buyer_or_supplier'
    ) THEN
        CREATE POLICY matches_select_buyer_or_supplier
        ON public.matches
        FOR SELECT
        TO authenticated
        USING (
            auth.uid()::text = buyer_id::text
            OR EXISTS (
                SELECT 1
                FROM public.listings l
                WHERE l.id::text = matches.listing_id::text
                  AND l.supplier_id::text = auth.uid()::text
            )
        );
    END IF;

    -- Buyer can create only own pending request.
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies
        WHERE schemaname = 'public'
          AND tablename = 'matches'
          AND policyname = 'matches_insert_buyer_own'
    ) THEN
        CREATE POLICY matches_insert_buyer_own
        ON public.matches
        FOR INSERT
        TO authenticated
        WITH CHECK (
            auth.uid()::text = buyer_id::text
            AND status = 'pending'
        );
    END IF;

    -- Supplier can update status only for matches on own listings,
    -- and only move from pending -> confirmed/rejected.
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies
        WHERE schemaname = 'public'
          AND tablename = 'matches'
          AND policyname = 'matches_update_supplier_pending_only'
    ) THEN
        CREATE POLICY matches_update_supplier_pending_only
        ON public.matches
        FOR UPDATE
        TO authenticated
        USING (
            EXISTS (
                SELECT 1
                FROM public.listings l
                WHERE l.id::text = matches.listing_id::text
                  AND l.supplier_id::text = auth.uid()::text
            )
            AND status = 'pending'
        )
        WITH CHECK (
            status IN ('confirmed', 'rejected')
            AND EXISTS (
                SELECT 1
                FROM public.listings l
                WHERE l.id::text = matches.listing_id::text
                  AND l.supplier_id::text = auth.uid()::text
            )
        );
    END IF;

    -- Buyer can revert (delete) only own pending request.
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies
        WHERE schemaname = 'public'
          AND tablename = 'matches'
          AND policyname = 'matches_delete_buyer_pending_only'
    ) THEN
        CREATE POLICY matches_delete_buyer_pending_only
        ON public.matches
        FOR DELETE
        TO authenticated
        USING (
            auth.uid()::text = buyer_id::text
            AND status = 'pending'
        );
    END IF;
END $$;

-- -----------------------------------------------------------------------------
-- 4) LISTINGS policies for supplier edit flow
-- -----------------------------------------------------------------------------
DO $$
BEGIN
    IF to_regclass('public.listings') IS NULL THEN
        RETURN;
    END IF;

    -- Listing visibility: supplier sees own, buyers can see active.
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies
        WHERE schemaname = 'public'
          AND tablename = 'listings'
          AND policyname = 'listings_select_owner_or_active'
    ) THEN
        CREATE POLICY listings_select_owner_or_active
        ON public.listings
        FOR SELECT
        TO authenticated
        USING (
            supplier_id::text = auth.uid()::text
            OR status = 'active'
        );
    END IF;

    -- Supplier inserts only own listings.
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies
        WHERE schemaname = 'public'
          AND tablename = 'listings'
          AND policyname = 'listings_insert_supplier_own'
    ) THEN
        CREATE POLICY listings_insert_supplier_own
        ON public.listings
        FOR INSERT
        TO authenticated
        WITH CHECK (
            supplier_id::text = auth.uid()::text
        );
    END IF;

    -- Supplier edits only own listings.
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies
        WHERE schemaname = 'public'
          AND tablename = 'listings'
          AND policyname = 'listings_update_supplier_own'
    ) THEN
        CREATE POLICY listings_update_supplier_own
        ON public.listings
        FOR UPDATE
        TO authenticated
        USING (
            supplier_id::text = auth.uid()::text
        )
        WITH CHECK (
            supplier_id::text = auth.uid()::text
        );
    END IF;
END $$;

-- Optional: keep deletes blocked from app path.
-- Add a controlled delete policy only if product wants soft/hard delete from UI.
