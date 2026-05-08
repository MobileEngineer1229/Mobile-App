-- Migration: Support MQTT auto-registered devices
-- MQTT devices don't have a MAC address or a specific user owner at registration time.

-- 1. Make mac_address nullable (MQTT devices won't have one)
ALTER TABLE devices ALTER COLUMN mac_address DROP NOT NULL;

-- 2. Make user_id nullable (auto-registered devices have no owner initially)
ALTER TABLE devices ALTER COLUMN user_id DROP NOT NULL;

-- 3. Drop the old unique constraint (user_id, mac_address) since both can be null now
ALTER TABLE devices DROP CONSTRAINT IF EXISTS devices_user_id_mac_address_key;

-- 4. Add a unique constraint on metadata productId+deviceNum for MQTT devices
-- This prevents duplicate auto-registration of the same MQTT device.
CREATE UNIQUE INDEX IF NOT EXISTS idx_devices_mqtt_identity
  ON devices (( metadata->>'productId' ), ( metadata->>'deviceNum' ))
  WHERE metadata->>'protocol' = 'mqtt';

-- 5. Add index on metadata protocol for faster MQTT device lookups
CREATE INDEX IF NOT EXISTS idx_devices_metadata_protocol
  ON devices USING btree (( metadata->>'protocol' ));
