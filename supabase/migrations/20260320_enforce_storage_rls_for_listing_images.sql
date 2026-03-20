-- Enforce storage RLS for listing image ownership.
-- Assumes bucket name: images
-- Object naming convention from app: <supplier_id>/<uuid>.<ext>

-- -----------------------------------------------------------------------------
-- 1) Ensure bucket exists and is private by default
-- -----------------------------------------------------------------------------
INSERT INTO storage.buckets (id, name, public)
VALUES ('images', 'images', false)
ON CONFLICT (id) DO NOTHING;

-- -----------------------------------------------------------------------------
-- 2) Recreate storage policies idempotently
-- -----------------------------------------------------------------------------
DO $$
BEGIN
    -- Bucket read policy (authenticated users can read listing images)
    DROP POLICY IF EXISTS storage_images_select_authenticated ON storage.objects;
    CREATE POLICY storage_images_select_authenticated
    ON storage.objects
    FOR SELECT
    TO authenticated
    USING (bucket_id = 'images');

    -- Upload policy: user can upload only under own folder prefix auth.uid()/...
    DROP POLICY IF EXISTS storage_images_insert_owner_prefix ON storage.objects;
    CREATE POLICY storage_images_insert_owner_prefix
    ON storage.objects
    FOR INSERT
    TO authenticated
    WITH CHECK (
        bucket_id = 'images'
        AND split_part(name, '/', 1) = auth.uid()::text
    );

    -- Update policy: user can update only own folder prefix
    DROP POLICY IF EXISTS storage_images_update_owner_prefix ON storage.objects;
    CREATE POLICY storage_images_update_owner_prefix
    ON storage.objects
    FOR UPDATE
    TO authenticated
    USING (
        bucket_id = 'images'
        AND split_part(name, '/', 1) = auth.uid()::text
    )
    WITH CHECK (
        bucket_id = 'images'
        AND split_part(name, '/', 1) = auth.uid()::text
    );

    -- Delete policy: user can delete only own folder prefix
    DROP POLICY IF EXISTS storage_images_delete_owner_prefix ON storage.objects;
    CREATE POLICY storage_images_delete_owner_prefix
    ON storage.objects
    FOR DELETE
    TO authenticated
    USING (
        bucket_id = 'images'
        AND split_part(name, '/', 1) = auth.uid()::text
    );
END $$;
