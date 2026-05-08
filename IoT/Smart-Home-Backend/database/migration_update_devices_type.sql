-- Migration: Update Devices Type Column
-- Updates existing devices records to use the new specific device type enum values
-- This migrates devices from generic types (camera, electronics) to specific types (cctv, speaker, etc.)

-- Update CCTV devices: change 'camera' to 'cctv' for CCTV-related devices
UPDATE devices 
SET type = 'cctv'::device_type_enum
WHERE type = 'camera'::device_type_enum
  AND (
    LOWER(name) LIKE '%cctv%' 
    OR LOWER(name) LIKE '%security camera%'
    OR LOWER(name) LIKE '%surveillance%'
    OR LOWER(name) LIKE '%ptz%'
    OR LOWER(name) LIKE '%ip camera%'
    OR LOWER(name) LIKE '%dome camera%'
    OR LOWER(name) LIKE '%bullet camera%'
    OR LOWER(name) LIKE '%night vision%'
  );

-- Update speaker devices: change 'electronics' to 'speaker' for speaker-related devices
UPDATE devices 
SET type = 'speaker'::device_type_enum
WHERE type = 'electronics'::device_type_enum
  AND (
    LOWER(name) LIKE '%speaker%'
    OR LOWER(name) LIKE '%soundbar%'
    OR LOWER(name) LIKE '%bluetooth speaker%'
    OR LOWER(name) LIKE '%wireless speaker%'
    OR LOWER(name) LIKE '%portable speaker%'
    OR LOWER(name) LIKE '%home theater%'
    OR LOWER(name) LIKE '%stereo%'
  );

-- Update thermostat devices: change 'electronics' to 'thermostat'
UPDATE devices 
SET type = 'thermostat'::device_type_enum
WHERE type = 'electronics'::device_type_enum
  AND LOWER(name) LIKE '%thermostat%';

-- Update lock devices: change 'electronics' to 'lock'
UPDATE devices 
SET type = 'lock'::device_type_enum
WHERE type = 'electronics'::device_type_enum
  AND (
    LOWER(name) LIKE '%lock%'
    OR LOWER(name) LIKE '%door lock%'
    OR LOWER(name) LIKE '%smart lock%'
  );

-- Update TV devices: change 'electronics' to 'tv'
UPDATE devices 
SET type = 'tv'::device_type_enum
WHERE type = 'electronics'::device_type_enum
  AND (
    LOWER(name) LIKE '%tv%'
    OR LOWER(name) LIKE '%television%'
    OR LOWER(name) LIKE '%smart tv%'
  );

-- Update appliance devices: change 'electronics' to 'appliance' for home appliances
UPDATE devices 
SET type = 'appliance'::device_type_enum
WHERE type = 'electronics'::device_type_enum
  AND (
    LOWER(name) LIKE '%refrigerator%'
    OR LOWER(name) LIKE '%washer%'
    OR LOWER(name) LIKE '%washing machine%'
    OR LOWER(name) LIKE '%air conditioner%'
    OR LOWER(name) LIKE '%ac%'
    OR LOWER(name) LIKE '%fan%'
    OR LOWER(name) LIKE '%dryer%'
    OR LOWER(name) LIKE '%dishwasher%'
    OR LOWER(name) LIKE '%oven%'
    OR LOWER(name) LIKE '%microwave%'
  );

-- Verify the updates
DO $$
DECLARE
    total_devices INTEGER;
    lamp_count INTEGER;
    camera_count INTEGER;
    electronics_count INTEGER;
    cctv_count INTEGER;
    speaker_count INTEGER;
    thermostat_count INTEGER;
    lock_count INTEGER;
    tv_count INTEGER;
    appliance_count INTEGER;
    sensor_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO total_devices FROM devices;
    SELECT COUNT(*) INTO lamp_count FROM devices WHERE type = 'lamp';
    SELECT COUNT(*) INTO camera_count FROM devices WHERE type = 'camera';
    SELECT COUNT(*) INTO electronics_count FROM devices WHERE type = 'electronics';
    SELECT COUNT(*) INTO cctv_count FROM devices WHERE type = 'cctv';
    SELECT COUNT(*) INTO speaker_count FROM devices WHERE type = 'speaker';
    SELECT COUNT(*) INTO thermostat_count FROM devices WHERE type = 'thermostat';
    SELECT COUNT(*) INTO lock_count FROM devices WHERE type = 'lock';
    SELECT COUNT(*) INTO tv_count FROM devices WHERE type = 'tv';
    SELECT COUNT(*) INTO appliance_count FROM devices WHERE type = 'appliance';
    SELECT COUNT(*) INTO sensor_count FROM devices WHERE type = 'sensor';
    
    RAISE NOTICE 'Migration completed successfully!';
    RAISE NOTICE 'Total devices: %', total_devices;
    RAISE NOTICE 'Device types distribution:';
    RAISE NOTICE '  - Lamp: %', lamp_count;
    RAISE NOTICE '  - Camera: %', camera_count;
    RAISE NOTICE '  - Electronics: %', electronics_count;
    RAISE NOTICE '  - CCTV: %', cctv_count;
    RAISE NOTICE '  - Speaker: %', speaker_count;
    RAISE NOTICE '  - Thermostat: %', thermostat_count;
    RAISE NOTICE '  - Lock: %', lock_count;
    RAISE NOTICE '  - TV: %', tv_count;
    RAISE NOTICE '  - Appliance: %', appliance_count;
    RAISE NOTICE '  - Sensor: %', sensor_count;
END $$;
