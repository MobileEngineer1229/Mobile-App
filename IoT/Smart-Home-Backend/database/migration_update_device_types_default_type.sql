-- Migration: Update Device Types Default Type Values
-- Updates existing device_types records to use the correct specific enum values
-- This ensures all device types use the new specific types (cctv, speaker, thermostat, etc.)
-- instead of generic ones (camera, electronics)

-- Update CCTV devices to use 'cctv' instead of 'camera'
UPDATE device_types 
SET default_type = 'cctv'::device_type_enum
WHERE default_type = 'camera'::device_type_enum
  AND (template_id LIKE '%cctv%' OR name LIKE '%CCTV%' OR name LIKE '%cctv%');

-- Update speaker devices to use 'speaker' instead of 'electronics'
UPDATE device_types 
SET default_type = 'speaker'::device_type_enum
WHERE default_type = 'electronics'::device_type_enum
  AND (template_id LIKE '%speaker%' OR name LIKE '%Speaker%' OR name LIKE '%speaker%' 
       OR template_id LIKE '%soundbar%' OR name LIKE '%Soundbar%');

-- Update thermostat devices
UPDATE device_types 
SET default_type = 'thermostat'::device_type_enum
WHERE default_type = 'electronics'::device_type_enum
  AND (template_id LIKE '%thermostat%' OR name LIKE '%Thermostat%');

-- Update lock devices
UPDATE device_types 
SET default_type = 'lock'::device_type_enum
WHERE default_type = 'electronics'::device_type_enum
  AND (template_id LIKE '%lock%' OR name LIKE '%Lock%');

-- Update TV devices
UPDATE device_types 
SET default_type = 'tv'::device_type_enum
WHERE default_type = 'electronics'::device_type_enum
  AND (template_id LIKE '%tv%' OR name LIKE '%TV%' OR name LIKE '%Tv%');

-- Update appliance devices (refrigerator, washer, AC, fan, etc.)
UPDATE device_types 
SET default_type = 'appliance'::device_type_enum
WHERE default_type = 'electronics'::device_type_enum
  AND (template_id LIKE '%refrigerator%' OR template_id LIKE '%washer%' 
       OR template_id LIKE '%ac%' OR template_id LIKE '%fan%'
       OR name LIKE '%Refrigerator%' OR name LIKE '%Washer%'
       OR name LIKE '%AC%' OR name LIKE '%Fan%');

-- Verify the updates
DO $$
DECLARE
    cctv_count INTEGER;
    speaker_count INTEGER;
    thermostat_count INTEGER;
    lock_count INTEGER;
    tv_count INTEGER;
    appliance_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO cctv_count FROM device_types WHERE default_type = 'cctv';
    SELECT COUNT(*) INTO speaker_count FROM device_types WHERE default_type = 'speaker';
    SELECT COUNT(*) INTO thermostat_count FROM device_types WHERE default_type = 'thermostat';
    SELECT COUNT(*) INTO lock_count FROM device_types WHERE default_type = 'lock';
    SELECT COUNT(*) INTO tv_count FROM device_types WHERE default_type = 'tv';
    SELECT COUNT(*) INTO appliance_count FROM device_types WHERE default_type = 'appliance';
    
    RAISE NOTICE 'Migration completed successfully!';
    RAISE NOTICE 'Device types by default_type:';
    RAISE NOTICE '  - CCTV: %', cctv_count;
    RAISE NOTICE '  - Speaker: %', speaker_count;
    RAISE NOTICE '  - Thermostat: %', thermostat_count;
    RAISE NOTICE '  - Lock: %', lock_count;
    RAISE NOTICE '  - TV: %', tv_count;
    RAISE NOTICE '  - Appliance: %', appliance_count;
END $$;
