ALTER TABLE ana_visual_asset
    ADD COLUMN related_image_id BIGINT NULL COMMENT 'Source image id related to the generated visual asset' AFTER attachment_id,
    ADD COLUMN tooth_code VARCHAR(32) NULL COMMENT 'Tooth position code related to the visual asset' AFTER related_image_id;

CREATE INDEX idx_visual_related_image ON ana_visual_asset (related_image_id);
