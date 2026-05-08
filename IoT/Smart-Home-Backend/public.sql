/*
 Navicat Premium Dump SQL

 Source Server         : PostgreSQL
 Source Server Type    : PostgreSQL
 Source Server Version : 180001 (180001)
 Source Host           : localhost:5432
 Source Catalog        : smart_home_db
 Source Schema         : public

 Target Server Type    : PostgreSQL
 Target Server Version : 180001 (180001)
 File Encoding         : 65001

 Date: 09/01/2026 17:48:18
*/


-- ----------------------------
-- Type structure for device_status_enum
-- ----------------------------
DROP TYPE IF EXISTS "public"."device_status_enum";
CREATE TYPE "public"."device_status_enum" AS ENUM (
  'online',
  'offline',
  'unknown'
);

-- ----------------------------
-- Type structure for device_type_enum
-- ----------------------------
DROP TYPE IF EXISTS "public"."device_type_enum";
CREATE TYPE "public"."device_type_enum" AS ENUM (
  'sensor',
  'actuator',
  'controller'
);

-- ----------------------------
-- Sequence structure for chatbot_messages_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."chatbot_messages_id_seq";
CREATE SEQUENCE "public"."chatbot_messages_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for devices_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."devices_id_seq";
CREATE SEQUENCE "public"."devices_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for energy_consumption_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."energy_consumption_id_seq";
CREATE SEQUENCE "public"."energy_consumption_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for home_invitations_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."home_invitations_id_seq";
CREATE SEQUENCE "public"."home_invitations_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for home_members_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."home_members_id_seq";
CREATE SEQUENCE "public"."home_members_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for homes_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."homes_id_seq";
CREATE SEQUENCE "public"."homes_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for linked_accounts_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."linked_accounts_id_seq";
CREATE SEQUENCE "public"."linked_accounts_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for migrations_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."migrations_id_seq";
CREATE SEQUENCE "public"."migrations_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for notifications_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."notifications_id_seq";
CREATE SEQUENCE "public"."notifications_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for password_reset_otp_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."password_reset_otp_id_seq";
CREATE SEQUENCE "public"."password_reset_otp_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for rooms_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."rooms_id_seq";
CREATE SEQUENCE "public"."rooms_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for scene_conditions_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."scene_conditions_id_seq";
CREATE SEQUENCE "public"."scene_conditions_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for scene_execution_logs_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."scene_execution_logs_id_seq";
CREATE SEQUENCE "public"."scene_execution_logs_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for scene_tasks_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."scene_tasks_id_seq";
CREATE SEQUENCE "public"."scene_tasks_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for smart_scenes_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."smart_scenes_id_seq";
CREATE SEQUENCE "public"."smart_scenes_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for user_actions_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."user_actions_id_seq";
CREATE SEQUENCE "public"."user_actions_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for user_additional_settings_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."user_additional_settings_id_seq";
CREATE SEQUENCE "public"."user_additional_settings_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for user_linked_assistants_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."user_linked_assistants_id_seq";
CREATE SEQUENCE "public"."user_linked_assistants_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for user_notification_preferences_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."user_notification_preferences_id_seq";
CREATE SEQUENCE "public"."user_notification_preferences_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for user_profile_metadata_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."user_profile_metadata_id_seq";
CREATE SEQUENCE "public"."user_profile_metadata_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for user_security_settings_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."user_security_settings_id_seq";
CREATE SEQUENCE "public"."user_security_settings_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for users_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."users_id_seq";
CREATE SEQUENCE "public"."users_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Sequence structure for voice_assistants_id_seq
-- ----------------------------
DROP SEQUENCE IF EXISTS "public"."voice_assistants_id_seq";
CREATE SEQUENCE "public"."voice_assistants_id_seq" 
INCREMENT 1
MINVALUE  1
MAXVALUE 2147483647
START 1
CACHE 1;

-- ----------------------------
-- Table structure for chatbot_messages
-- ----------------------------
DROP TABLE IF EXISTS "public"."chatbot_messages";
CREATE TABLE "public"."chatbot_messages" (
  "id" int4 NOT NULL DEFAULT nextval('chatbot_messages_id_seq'::regclass),
  "user_id" int4 NOT NULL,
  "role" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "message" text COLLATE "pg_catalog"."default" NOT NULL,
  "metadata" jsonb,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."chatbot_messages"."role" IS 'Message role: user or assistant';
COMMENT ON COLUMN "public"."chatbot_messages"."metadata" IS 'Additional data (e.g., suggested actions, links)';
COMMENT ON TABLE "public"."chatbot_messages" IS 'Stores chatbot conversation history';

-- ----------------------------
-- Records of chatbot_messages
-- ----------------------------

-- ----------------------------
-- Table structure for devices
-- ----------------------------
DROP TABLE IF EXISTS "public"."devices";
CREATE TABLE "public"."devices" (
  "id" int4 NOT NULL DEFAULT nextval('devices_id_seq'::regclass),
  "user_id" int4 NOT NULL,
  "name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "type" "public"."device_type_enum" NOT NULL,
  "status" "public"."device_status_enum" NOT NULL DEFAULT 'unknown'::device_status_enum,
  "mac_address" varchar(17) COLLATE "pg_catalog"."default" NOT NULL,
  "ip_address" varchar(45) COLLATE "pg_catalog"."default",
  "last_seen" timestamp(6),
  "metadata" jsonb,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "room_id" int4
)
;

-- ----------------------------
-- Records of devices
-- ----------------------------
INSERT INTO "public"."devices" VALUES (1, 4, 'Living Room Light', 'actuator', 'online', 'AA:BB:CC:DD:EE:01', '192.168.1.101', '2025-12-24 16:45:52.913176', '{"color": "warm_white", "brightness": 80}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', 1);
INSERT INTO "public"."devices" VALUES (2, 4, 'Bedroom Light', 'actuator', 'online', 'AA:BB:CC:DD:EE:02', '192.168.1.102', '2025-12-24 16:40:52.913176', '{"color": "soft_white", "brightness": 50}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', 2);
INSERT INTO "public"."devices" VALUES (3, 4, 'Kitchen Temperature Sensor', 'sensor', 'online', 'AA:BB:CC:DD:EE:03', '192.168.1.103', '2025-12-24 16:48:52.913176', '{"humidity": 45, "temperature": 22.5}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', 3);
INSERT INTO "public"."devices" VALUES (4, 4, 'Smart Thermostat', 'controller', 'online', 'AA:BB:CC:DD:EE:04', '192.168.1.104', '2025-12-24 16:49:52.913176', '{"mode": "auto", "temperature": 21.0}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', 1);
INSERT INTO "public"."devices" VALUES (5, 4, 'Front Door Lock', 'actuator', 'online', 'AA:BB:CC:DD:EE:05', '192.168.1.105', '2025-12-24 16:20:52.913176', '{"locked": true, "battery": 85}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', NULL);
INSERT INTO "public"."devices" VALUES (6, 4, 'Security Camera', 'sensor', 'online', 'AA:BB:CC:DD:EE:06', '192.168.1.106', '2025-12-24 16:49:52.913176', '{"motion_detected": false}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', NULL);
INSERT INTO "public"."devices" VALUES (7, 4, 'Smoke Detector', 'sensor', 'online', 'AA:BB:CC:DD:EE:07', '192.168.1.107', '2025-12-24 16:45:52.913176', '{"battery": 90, "smoke_level": 0}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', 1);
INSERT INTO "public"."devices" VALUES (8, 4, 'Motion Sensor', 'sensor', 'online', 'AA:BB:CC:DD:EE:08', '192.168.1.108', '2025-12-24 16:47:52.913176', '{"motion": false}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', 2);
INSERT INTO "public"."devices" VALUES (9, 4, 'Window Sensor', 'sensor', 'offline', 'AA:BB:CC:DD:EE:09', '192.168.1.109', '2025-12-24 14:50:52.913176', '{"open": false}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', 2);
INSERT INTO "public"."devices" VALUES (10, 4, 'Smart Speaker', 'controller', 'online', 'AA:BB:CC:DD:EE:0A', '192.168.1.110', '2025-12-24 16:49:52.913176', '{"volume": 50, "playing": false}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', 1);
INSERT INTO "public"."devices" VALUES (11, 4, 'Garage Door Opener', 'actuator', 'online', 'AA:BB:CC:DD:EE:0B', '192.168.1.111', '2025-12-24 16:35:52.913176', '{"open": false}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', 10);
INSERT INTO "public"."devices" VALUES (12, 4, 'Smart TV', 'controller', 'online', 'AA:BB:CC:DD:EE:0C', '192.168.1.112', '2025-12-24 16:45:52.913176', '{"power": false, "channel": 0}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', 1);
INSERT INTO "public"."devices" VALUES (13, 4, 'Refrigerator', 'actuator', 'online', 'AA:BB:CC:DD:EE:0D', '192.168.1.113', '2025-12-24 16:49:52.913176', '{"temperature": 4.0}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', 3);
INSERT INTO "public"."devices" VALUES (14, 4, 'Washing Machine', 'actuator', 'offline', 'AA:BB:CC:DD:EE:0E', '192.168.1.114', '2025-12-23 16:50:52.913176', '{"running": false}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', NULL);
INSERT INTO "public"."devices" VALUES (15, 4, 'Air Conditioner', 'actuator', 'online', 'AA:BB:CC:DD:EE:0F', '192.168.1.115', '2025-12-24 16:48:52.913176', '{"mode": "cool", "power": true, "temperature": 22}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', 1);
INSERT INTO "public"."devices" VALUES (16, 4, 'Humidity Sensor', 'sensor', 'online', 'AA:BB:CC:DD:EE:10', '192.168.1.116', '2025-12-24 16:49:52.913176', '{"humidity": 45}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', 3);
INSERT INTO "public"."devices" VALUES (17, 4, 'Smart Plug', 'actuator', 'online', 'AA:BB:CC:DD:EE:11', '192.168.1.117', '2025-12-24 16:49:52.913176', '{"power": true, "energy": 120}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', 1);
INSERT INTO "public"."devices" VALUES (18, 5, 'Living Room Light', 'actuator', 'online', 'BB:CC:DD:EE:FF:01', '192.168.1.201', '2025-12-24 16:45:52.913176', '{"brightness": 75}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', 15);
INSERT INTO "public"."devices" VALUES (19, 5, 'Bedroom Light', 'actuator', 'online', 'BB:CC:DD:EE:FF:02', '192.168.1.202', '2025-12-24 16:40:52.913176', '{"brightness": 40}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', 16);
INSERT INTO "public"."devices" VALUES (20, 5, 'Kitchen Temperature Sensor', 'sensor', 'online', 'BB:CC:DD:EE:FF:03', '192.168.1.203', '2025-12-24 16:48:52.913176', '{"temperature": 23.0}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', 17);
INSERT INTO "public"."devices" VALUES (21, 5, 'Smart Thermostat', 'controller', 'online', 'BB:CC:DD:EE:FF:04', '192.168.1.204', '2025-12-24 16:49:52.913176', '{"temperature": 22.0}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', 15);
INSERT INTO "public"."devices" VALUES (22, 5, 'Front Door Lock', 'actuator', 'online', 'BB:CC:DD:EE:FF:05', '192.168.1.205', '2025-12-24 16:20:52.913176', '{"locked": true}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', NULL);
INSERT INTO "public"."devices" VALUES (23, 5, 'Security Camera', 'sensor', 'online', 'BB:CC:DD:EE:FF:06', '192.168.1.206', '2025-12-24 16:49:52.913176', '{"motion_detected": false}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', NULL);
INSERT INTO "public"."devices" VALUES (24, 5, 'Smoke Detector', 'sensor', 'online', 'BB:CC:DD:EE:FF:07', '192.168.1.207', '2025-12-24 16:45:52.913176', '{"smoke_level": 0}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', 15);
INSERT INTO "public"."devices" VALUES (25, 5, 'Motion Sensor', 'sensor', 'online', 'BB:CC:DD:EE:FF:08', '192.168.1.208', '2025-12-24 16:47:52.913176', '{"motion": false}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', 16);
INSERT INTO "public"."devices" VALUES (26, 5, 'Window Sensor', 'sensor', 'offline', 'BB:CC:DD:EE:FF:09', '192.168.1.209', '2025-12-24 14:50:52.913176', '{"open": false}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', 16);
INSERT INTO "public"."devices" VALUES (27, 5, 'Smart Speaker', 'controller', 'online', 'BB:CC:DD:EE:FF:0A', '192.168.1.210', '2025-12-24 16:49:52.913176', '{"volume": 60}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', 15);
INSERT INTO "public"."devices" VALUES (28, 5, 'Garage Door Opener', 'actuator', 'online', 'BB:CC:DD:EE:FF:0B', '192.168.1.211', '2025-12-24 16:35:52.913176', '{"open": false}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', NULL);
INSERT INTO "public"."devices" VALUES (29, 5, 'Smart TV', 'controller', 'online', 'BB:CC:DD:EE:FF:0C', '192.168.1.212', '2025-12-24 16:45:52.913176', '{"power": false}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', 15);
INSERT INTO "public"."devices" VALUES (30, 5, 'Refrigerator', 'actuator', 'online', 'BB:CC:DD:EE:FF:0D', '192.168.1.213', '2025-12-24 16:49:52.913176', '{"temperature": 4.0}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', 17);
INSERT INTO "public"."devices" VALUES (31, 5, 'Washing Machine', 'actuator', 'offline', 'BB:CC:DD:EE:FF:0E', '192.168.1.214', '2025-12-23 16:50:52.913176', '{"running": false}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', NULL);
INSERT INTO "public"."devices" VALUES (32, 5, 'Air Conditioner', 'actuator', 'online', 'BB:CC:DD:EE:FF:0F', '192.168.1.215', '2025-12-24 16:48:52.913176', '{"power": true, "temperature": 23}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', 15);
INSERT INTO "public"."devices" VALUES (33, 5, 'Humidity Sensor', 'sensor', 'online', 'BB:CC:DD:EE:FF:10', '192.168.1.216', '2025-12-24 16:49:52.913176', '{"humidity": 50}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', 17);
INSERT INTO "public"."devices" VALUES (34, 5, 'Smart Plug', 'actuator', 'online', 'BB:CC:DD:EE:FF:11', '192.168.1.217', '2025-12-24 16:49:52.913176', '{"power": true}', '2025-12-24 16:50:52.913176', '2025-12-24 16:50:52.913176', 15);

-- ----------------------------
-- Table structure for energy_consumption
-- ----------------------------
DROP TABLE IF EXISTS "public"."energy_consumption";
CREATE TABLE "public"."energy_consumption" (
  "id" int4 NOT NULL DEFAULT nextval('energy_consumption_id_seq'::regclass),
  "device_id" int4 NOT NULL,
  "date" date NOT NULL,
  "consumption_kwh" numeric(10,2) NOT NULL DEFAULT 0.00,
  "cost_usd" numeric(10,2) NOT NULL DEFAULT 0.00,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."energy_consumption"."consumption_kwh" IS 'Energy consumption in kilowatt-hours';
COMMENT ON COLUMN "public"."energy_consumption"."cost_usd" IS 'Cost in USD (calculated based on consumption and electricity rate)';
COMMENT ON TABLE "public"."energy_consumption" IS 'Stores daily energy consumption records for each device';

-- ----------------------------
-- Records of energy_consumption
-- ----------------------------

-- ----------------------------
-- Table structure for home_invitations
-- ----------------------------
DROP TABLE IF EXISTS "public"."home_invitations";
CREATE TABLE "public"."home_invitations" (
  "id" int4 NOT NULL DEFAULT nextval('home_invitations_id_seq'::regclass),
  "home_id" int4 NOT NULL,
  "code" varchar(20) COLLATE "pg_catalog"."default" NOT NULL,
  "created_by" int4 NOT NULL,
  "expires_at" timestamp(6),
  "max_uses" int4 DEFAULT 1,
  "current_uses" int4 DEFAULT 0,
  "is_active" bool DEFAULT true,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."home_invitations"."code" IS 'Unique invitation code (e.g., ABC123)';
COMMENT ON COLUMN "public"."home_invitations"."expires_at" IS 'Expiration timestamp (NULL = never expires)';
COMMENT ON COLUMN "public"."home_invitations"."max_uses" IS 'Maximum number of times this invitation can be used (0 = unlimited)';
COMMENT ON COLUMN "public"."home_invitations"."current_uses" IS 'Number of times this invitation has been used';
COMMENT ON TABLE "public"."home_invitations" IS 'Stores invitation codes for joining homes';

-- ----------------------------
-- Records of home_invitations
-- ----------------------------

-- ----------------------------
-- Table structure for home_members
-- ----------------------------
DROP TABLE IF EXISTS "public"."home_members";
CREATE TABLE "public"."home_members" (
  "id" int4 NOT NULL DEFAULT nextval('home_members_id_seq'::regclass),
  "home_id" int4 NOT NULL,
  "user_id" int4 NOT NULL,
  "role" varchar(20) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'member'::character varying,
  "added_by" int4 NOT NULL,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."home_members"."role" IS 'Member role: owner (full control), admin (manage members/devices), member (view and control devices)';
COMMENT ON COLUMN "public"."home_members"."added_by" IS 'User ID who added this member to the home';
COMMENT ON TABLE "public"."home_members" IS 'Stores home memberships with roles (owner, admin, member)';

-- ----------------------------
-- Records of home_members
-- ----------------------------

-- ----------------------------
-- Table structure for homes
-- ----------------------------
DROP TABLE IF EXISTS "public"."homes";
CREATE TABLE "public"."homes" (
  "id" int4 NOT NULL DEFAULT nextval('homes_id_seq'::regclass),
  "user_id" int4 NOT NULL,
  "name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "address" varchar(255) COLLATE "pg_catalog"."default",
  "latitude" numeric(10,8),
  "longitude" numeric(11,8),
  "country" varchar(100) COLLATE "pg_catalog"."default",
  "is_primary" bool NOT NULL DEFAULT false,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."homes"."is_primary" IS 'Indicates the primary/default home for the user';
COMMENT ON TABLE "public"."homes" IS 'Stores multiple home locations for each user';

-- ----------------------------
-- Records of homes
-- ----------------------------
INSERT INTO "public"."homes" VALUES (19, 4, 'Main Residence', '701 7th Ave, New York, NY 10036, USA', 40.75790000, -73.98770000, 'United States', 't', '2025-12-24 16:50:52.907676', '2025-12-24 16:50:52.907676');
INSERT INTO "public"."homes" VALUES (20, 4, 'Summer House', '123 Ocean Drive, Miami Beach, FL 33139, USA', 25.79070000, -80.13000000, 'United States', 'f', '2025-12-24 16:50:52.907676', '2025-12-24 16:50:52.907676');
INSERT INTO "public"."homes" VALUES (21, 5, 'Downtown Apartment', '456 Broadway, New York, NY 10013, USA', 40.72090000, -74.00070000, 'United States', 't', '2025-12-24 16:50:52.907676', '2025-12-24 16:50:52.907676');
INSERT INTO "public"."homes" VALUES (22, 5, 'Country Home', '789 Country Road, Upstate NY 12550, USA', 41.70000000, -74.00000000, 'United States', 'f', '2025-12-24 16:50:52.907676', '2025-12-24 16:50:52.907676');
INSERT INTO "public"."homes" VALUES (23, 6, 'City Loft', '321 Park Ave, New York, NY 10022, USA', 40.75890000, -73.96920000, 'United States', 't', '2025-12-24 16:50:52.907676', '2025-12-24 16:50:52.907676');
INSERT INTO "public"."homes" VALUES (24, 7, 'Suburban Home', '654 Maple Street, Westchester, NY 10583, USA', 41.00000000, -73.80000000, 'United States', 't', '2025-12-24 16:50:52.907676', '2025-12-24 16:50:52.907676');
INSERT INTO "public"."homes" VALUES (25, 8, 'Beach House', '987 Beach Blvd, Malibu, CA 90265, USA', 34.02590000, -118.77980000, 'United States', 't', '2025-12-24 16:50:52.907676', '2025-12-24 16:50:52.907676');
INSERT INTO "public"."homes" VALUES (26, 9, 'Mountain Cabin', '456 Pine Trail, Aspen, CO 81611, USA', 39.19110000, -106.81750000, 'United States', 't', '2025-12-24 16:50:52.907676', '2025-12-24 16:50:52.907676');
INSERT INTO "public"."homes" VALUES (27, 10, 'Urban Studio', '789 5th Ave, New York, NY 10022, USA', 40.76360000, -73.97440000, 'United States', 't', '2025-12-24 16:50:52.907676', '2025-12-24 16:50:52.907676');
INSERT INTO "public"."homes" VALUES (28, 11, 'Family Home', '123 Elm Street, Boston, MA 02115, USA', 42.33990000, -71.08990000, 'United States', 't', '2025-12-24 16:50:52.907676', '2025-12-24 16:50:52.907676');
INSERT INTO "public"."homes" VALUES (29, 12, 'Lakeside Retreat', '321 Lake Road, Lake Tahoe, CA 96150, USA', 39.09680000, -120.03240000, 'United States', 't', '2025-12-24 16:50:52.907676', '2025-12-24 16:50:52.907676');
INSERT INTO "public"."homes" VALUES (30, 13, 'Desert Oasis', '654 Desert Way, Scottsdale, AZ 85251, USA', 33.49420000, -111.92610000, 'United States', 't', '2025-12-24 16:50:52.907676', '2025-12-24 16:50:52.907676');
INSERT INTO "public"."homes" VALUES (31, 14, 'Historic Townhouse', '987 Heritage Lane, Charleston, SC 29401, USA', 32.77650000, -79.93110000, 'United States', 't', '2025-12-24 16:50:52.907676', '2025-12-24 16:50:52.907676');
INSERT INTO "public"."homes" VALUES (32, 15, 'Modern Condo', '456 Modern Ave, San Francisco, CA 94102, USA', 37.77490000, -122.41940000, 'United States', 't', '2025-12-24 16:50:52.907676', '2025-12-24 16:50:52.907676');
INSERT INTO "public"."homes" VALUES (33, 16, 'Ranch House', '789 Ranch Road, Austin, TX 78701, USA', 30.26720000, -97.74310000, 'United States', 't', '2025-12-24 16:50:52.907676', '2025-12-24 16:50:52.907676');
INSERT INTO "public"."homes" VALUES (34, 17, 'Coastal Villa', '123 Coast Drive, San Diego, CA 92101, USA', 32.71570000, -117.16110000, 'United States', 't', '2025-12-24 16:50:52.907676', '2025-12-24 16:50:52.907676');
INSERT INTO "public"."homes" VALUES (35, 18, 'Garden Home', '321 Garden Street, Portland, OR 97201, USA', 45.51520000, -122.67840000, 'United States', 't', '2025-12-24 16:50:52.907676', '2025-12-24 16:50:52.907676');
INSERT INTO "public"."homes" VALUES (36, 19, 'Skyline Penthouse', '654 Skyline Blvd, Seattle, WA 98101, USA', 47.60620000, -122.33210000, 'United States', 't', '2025-12-24 16:50:52.907676', '2025-12-24 16:50:52.907676');

-- ----------------------------
-- Table structure for linked_accounts
-- ----------------------------
DROP TABLE IF EXISTS "public"."linked_accounts";
CREATE TABLE "public"."linked_accounts" (
  "id" int4 NOT NULL DEFAULT nextval('linked_accounts_id_seq'::regclass),
  "user_id" int4 NOT NULL,
  "provider" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "provider_user_id" varchar(255) COLLATE "pg_catalog"."default",
  "access_token" text COLLATE "pg_catalog"."default",
  "refresh_token" text COLLATE "pg_catalog"."default",
  "expires_at" timestamp(6),
  "metadata" jsonb,
  "connected_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;

-- ----------------------------
-- Records of linked_accounts
-- ----------------------------

-- ----------------------------
-- Table structure for migrations
-- ----------------------------
DROP TABLE IF EXISTS "public"."migrations";
CREATE TABLE "public"."migrations" (
  "id" int4 NOT NULL DEFAULT nextval('migrations_id_seq'::regclass),
  "name" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "executed_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;

-- ----------------------------
-- Records of migrations
-- ----------------------------
INSERT INTO "public"."migrations" VALUES (1, 'migration_add_rooms', '2025-12-02 16:45:55.774038');
INSERT INTO "public"."migrations" VALUES (2, 'migration_add_password_reset', '2025-12-04 00:39:43.425798');
INSERT INTO "public"."migrations" VALUES (3, 'migration_add_energy_consumption', '2025-12-04 00:39:43.459602');
INSERT INTO "public"."migrations" VALUES (4, 'migration_add_notifications', '2025-12-04 01:08:49.243823');
INSERT INTO "public"."migrations" VALUES (5, 'migration_add_chatbot', '2025-12-04 01:08:49.255989');
INSERT INTO "public"."migrations" VALUES (6, 'migration_add_voice_assistants', '2025-12-06 00:53:50.932934');
INSERT INTO "public"."migrations" VALUES (7, 'migration_add_user_settings', '2025-12-06 01:03:04.366323');
INSERT INTO "public"."migrations" VALUES (8, 'migration_add_linked_accounts', '2025-12-06 01:03:04.387601');
INSERT INTO "public"."migrations" VALUES (9, 'migration_add_additional_settings', '2025-12-06 01:03:04.393217');
INSERT INTO "public"."migrations" VALUES (10, 'migration_consolidate_user_settings', '2025-12-06 01:17:17.11422');
INSERT INTO "public"."migrations" VALUES (11, 'migration_add_homes', '2025-12-08 15:16:16.234717');
INSERT INTO "public"."migrations" VALUES (12, 'migration_add_home_id_to_rooms', '2025-12-08 15:16:16.246138');
INSERT INTO "public"."migrations" VALUES (13, 'migration_add_notification_category', '2025-12-08 15:16:16.251563');
INSERT INTO "public"."migrations" VALUES (14, 'migration_separate_user_settings', '2025-12-08 15:19:42.15094');
INSERT INTO "public"."migrations" VALUES (15, 'migration_add_user_actions', '2026-01-05 00:42:23.973908');
INSERT INTO "public"."migrations" VALUES (16, 'migration_add_home_members', '2026-01-05 20:26:42.887152');
INSERT INTO "public"."migrations" VALUES (17, 'migration_add_home_invitations', '2026-01-05 20:26:42.900398');

-- ----------------------------
-- Table structure for notifications
-- ----------------------------
DROP TABLE IF EXISTS "public"."notifications";
CREATE TABLE "public"."notifications" (
  "id" int4 NOT NULL DEFAULT nextval('notifications_id_seq'::regclass),
  "user_id" int4 NOT NULL,
  "title" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "message" text COLLATE "pg_catalog"."default" NOT NULL,
  "type" varchar(50) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'general'::character varying,
  "icon" varchar(50) COLLATE "pg_catalog"."default",
  "is_read" bool NOT NULL DEFAULT false,
  "read_at" timestamp(6),
  "metadata" jsonb,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "category" varchar(20) COLLATE "pg_catalog"."default"
)
;
COMMENT ON COLUMN "public"."notifications"."type" IS 'Notification type: general, security, system, feature, reminder, etc.';
COMMENT ON COLUMN "public"."notifications"."icon" IS 'Icon identifier for UI display';
COMMENT ON COLUMN "public"."notifications"."metadata" IS 'Additional data in JSON format';
COMMENT ON COLUMN "public"."notifications"."category" IS 'Notification category: general or smart_home';
COMMENT ON TABLE "public"."notifications" IS 'Stores user notifications';

-- ----------------------------
-- Records of notifications
-- ----------------------------
INSERT INTO "public"."notifications" VALUES (1, 4, 'Device Status Alert', 'Living Room Light is now online', 'alert', 'device', 'f', NULL, '{"device_id": 1, "device_name": "Living Room Light"}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (2, 4, 'Energy Consumption Alert', 'High energy usage detected in Kitchen', 'reminder', 'energy', 'f', NULL, '{"device_id": 13, "consumption": 250}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (3, 4, 'Security Alert', 'Motion detected in Bedroom', 'security', 'security', 't', '2025-12-24 15:50:52.919461', '{"location": "Bedroom", "sensor_id": 8}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (4, 4, 'System Update', 'New firmware available for Smart Thermostat', 'system', 'system', 'f', NULL, '{"device_id": 4, "firmware_version": "2.1.0"}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (5, 4, 'Feature Update', 'New automation features are now available', 'feature', 'feature', 'f', NULL, '{}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (6, 4, 'Bill Reminder', 'Your monthly energy bill is ready', 'reminder', 'bill', 'f', NULL, '{"amount": 125.50, "due_date": "2024-01-15"}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (7, 4, 'Device Maintenance', 'Smoke Detector battery is running low', 'reminder', 'maintenance', 'f', NULL, '{"device_id": 7, "battery_level": 15}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (8, 4, 'Automation Update', 'Welcome Home Automation executed successfully', 'general', 'automation', 't', '2025-12-24 16:20:52.919461', '{"scene_id": 1}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (9, 4, 'Weather Alert', 'Temperature is expected to drop below 0°C tonight', 'alert', 'weather', 'f', NULL, '{"location": "New York", "temperature": -2}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (10, 4, 'Community Update', 'New neighbors joined the smart home community', 'general', 'community', 'f', NULL, '{}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (11, 4, 'Home Invitation', 'You have been invited to manage another home', 'general', 'invitation', 'f', NULL, '{"home_id": 2, "inviter": "Jane Smith"}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (12, 4, 'User Access Alert', 'New device connected to your network', 'security', 'security', 'f', NULL, '{"device_mac": "CC:DD:EE:FF:00:11"}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (13, 4, 'Customer Support', 'Your support ticket #12345 has been updated', 'general', 'support', 'f', NULL, '{"ticket_id": 12345}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (14, 4, 'Feedback Request', 'How was your experience with our app?', 'general', 'feedback', 'f', NULL, '{}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (15, 4, 'Account Security', 'Login detected from new device', 'security', 'security', 't', '2025-12-24 14:50:52.919461', '{"device": "iPhone 14", "location": "New York"}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (16, 4, 'Device Status Alert', 'Window Sensor is offline', 'alert', 'device', 'f', NULL, '{"device_id": 9, "device_name": "Window Sensor"}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (17, 4, 'Energy Consumption Alert', 'Monthly energy usage is 15% higher than last month', 'reminder', 'energy', 'f', NULL, '{"usage": 450, "increase": 15}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (18, 4, 'System Update', 'App update available - Version 2.5.0', 'system', 'system', 'f', NULL, '{"version": "2.5.0"}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (19, 5, 'Device Status Alert', 'Living Room Light is now online', 'alert', 'device', 'f', NULL, '{"device_id": 19, "device_name": "Living Room Light"}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (20, 5, 'Energy Consumption Alert', 'High energy usage detected in Kitchen', 'reminder', 'energy', 'f', NULL, '{"device_id": 31, "consumption": 280}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (21, 5, 'Security Alert', 'Motion detected in Bedroom', 'security', 'security', 't', '2025-12-24 15:50:52.919461', '{"location": "Bedroom", "sensor_id": 26}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (22, 5, 'System Update', 'New firmware available for Smart Thermostat', 'system', 'system', 'f', NULL, '{"device_id": 22, "firmware_version": "2.1.0"}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (23, 5, 'Feature Update', 'New automation features are now available', 'feature', 'feature', 'f', NULL, '{}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (24, 5, 'Bill Reminder', 'Your monthly energy bill is ready', 'reminder', 'bill', 'f', NULL, '{"amount": 145.75, "due_date": "2024-01-15"}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (25, 5, 'Device Maintenance', 'Smoke Detector battery is running low', 'reminder', 'maintenance', 'f', NULL, '{"device_id": 25, "battery_level": 20}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (26, 5, 'Automation Update', 'Welcome Home Automation executed successfully', 'general', 'automation', 't', '2025-12-24 16:20:52.919461', '{"scene_id": 1}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (27, 5, 'Weather Alert', 'Temperature is expected to drop below 0°C tonight', 'alert', 'weather', 'f', NULL, '{"location": "New York", "temperature": -1}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (28, 5, 'Community Update', 'New neighbors joined the smart home community', 'general', 'community', 'f', NULL, '{}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (29, 5, 'Home Invitation', 'You have been invited to manage another home', 'general', 'invitation', 'f', NULL, '{"home_id": 3, "inviter": "Michael Johnson"}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (30, 5, 'User Access Alert', 'New device connected to your network', 'security', 'security', 'f', NULL, '{"device_mac": "DD:EE:FF:00:11:22"}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (31, 5, 'Customer Support', 'Your support ticket #12346 has been updated', 'general', 'support', 'f', NULL, '{"ticket_id": 12346}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (32, 5, 'Feedback Request', 'How was your experience with our app?', 'general', 'feedback', 'f', NULL, '{}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (33, 5, 'Account Security', 'Login detected from new device', 'security', 'security', 't', '2025-12-24 14:50:52.919461', '{"device": "Samsung Galaxy S23", "location": "New York"}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (34, 5, 'Device Status Alert', 'Window Sensor is offline', 'alert', 'device', 'f', NULL, '{"device_id": 27, "device_name": "Window Sensor"}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (35, 5, 'Energy Consumption Alert', 'Monthly energy usage is 12% higher than last month', 'reminder', 'energy', 'f', NULL, '{"usage": 520, "increase": 12}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (36, 5, 'System Update', 'App update available - Version 2.5.0', 'system', 'system', 'f', NULL, '{"version": "2.5.0"}', '2025-12-24 16:50:52.919461', NULL);
INSERT INTO "public"."notifications" VALUES (37, 4, 'Device Status Alert', 'Living Room Light is now online', 'alert', 'device', 'f', NULL, '{"device_id": 1, "device_name": "Living Room Light"}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (38, 4, 'Energy Consumption Alert', 'High energy usage detected in Kitchen', 'reminder', 'energy', 'f', NULL, '{"device_id": 13, "consumption": 250}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (39, 4, 'Security Alert', 'Motion detected in Bedroom', 'security', 'security', 't', '2025-12-24 15:50:53.967573', '{"location": "Bedroom", "sensor_id": 8}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (40, 4, 'System Update', 'New firmware available for Smart Thermostat', 'system', 'system', 'f', NULL, '{"device_id": 4, "firmware_version": "2.1.0"}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (41, 4, 'Feature Update', 'New automation features are now available', 'feature', 'feature', 'f', NULL, '{}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (42, 4, 'Bill Reminder', 'Your monthly energy bill is ready', 'reminder', 'bill', 'f', NULL, '{"amount": 125.50, "due_date": "2024-01-15"}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (43, 4, 'Device Maintenance', 'Smoke Detector battery is running low', 'reminder', 'maintenance', 'f', NULL, '{"device_id": 7, "battery_level": 15}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (44, 4, 'Automation Update', 'Welcome Home Automation executed successfully', 'general', 'automation', 't', '2025-12-24 16:20:53.967573', '{"scene_id": 1}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (45, 4, 'Weather Alert', 'Temperature is expected to drop below 0°C tonight', 'alert', 'weather', 'f', NULL, '{"location": "New York", "temperature": -2}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (46, 4, 'Community Update', 'New neighbors joined the smart home community', 'general', 'community', 'f', NULL, '{}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (47, 4, 'Home Invitation', 'You have been invited to manage another home', 'general', 'invitation', 'f', NULL, '{"home_id": 2, "inviter": "Jane Smith"}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (48, 4, 'User Access Alert', 'New device connected to your network', 'security', 'security', 'f', NULL, '{"device_mac": "CC:DD:EE:FF:00:11"}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (49, 4, 'Customer Support', 'Your support ticket #12345 has been updated', 'general', 'support', 'f', NULL, '{"ticket_id": 12345}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (50, 4, 'Feedback Request', 'How was your experience with our app?', 'general', 'feedback', 'f', NULL, '{}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (51, 4, 'Account Security', 'Login detected from new device', 'security', 'security', 't', '2025-12-24 14:50:53.967573', '{"device": "iPhone 14", "location": "New York"}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (52, 4, 'Device Status Alert', 'Window Sensor is offline', 'alert', 'device', 'f', NULL, '{"device_id": 9, "device_name": "Window Sensor"}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (53, 4, 'Energy Consumption Alert', 'Monthly energy usage is 15% higher than last month', 'reminder', 'energy', 'f', NULL, '{"usage": 450, "increase": 15}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (54, 4, 'System Update', 'App update available - Version 2.5.0', 'system', 'system', 'f', NULL, '{"version": "2.5.0"}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (55, 5, 'Device Status Alert', 'Living Room Light is now online', 'alert', 'device', 'f', NULL, '{"device_id": 19, "device_name": "Living Room Light"}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (56, 5, 'Energy Consumption Alert', 'High energy usage detected in Kitchen', 'reminder', 'energy', 'f', NULL, '{"device_id": 31, "consumption": 280}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (57, 5, 'Security Alert', 'Motion detected in Bedroom', 'security', 'security', 't', '2025-12-24 15:50:53.967573', '{"location": "Bedroom", "sensor_id": 26}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (58, 5, 'System Update', 'New firmware available for Smart Thermostat', 'system', 'system', 'f', NULL, '{"device_id": 22, "firmware_version": "2.1.0"}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (59, 5, 'Feature Update', 'New automation features are now available', 'feature', 'feature', 'f', NULL, '{}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (60, 5, 'Bill Reminder', 'Your monthly energy bill is ready', 'reminder', 'bill', 'f', NULL, '{"amount": 145.75, "due_date": "2024-01-15"}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (61, 5, 'Device Maintenance', 'Smoke Detector battery is running low', 'reminder', 'maintenance', 'f', NULL, '{"device_id": 25, "battery_level": 20}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (62, 5, 'Automation Update', 'Welcome Home Automation executed successfully', 'general', 'automation', 't', '2025-12-24 16:20:53.967573', '{"scene_id": 1}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (63, 5, 'Weather Alert', 'Temperature is expected to drop below 0°C tonight', 'alert', 'weather', 'f', NULL, '{"location": "New York", "temperature": -1}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (64, 5, 'Community Update', 'New neighbors joined the smart home community', 'general', 'community', 'f', NULL, '{}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (65, 5, 'Home Invitation', 'You have been invited to manage another home', 'general', 'invitation', 'f', NULL, '{"home_id": 3, "inviter": "Michael Johnson"}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (66, 5, 'User Access Alert', 'New device connected to your network', 'security', 'security', 'f', NULL, '{"device_mac": "DD:EE:FF:00:11:22"}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (67, 5, 'Customer Support', 'Your support ticket #12346 has been updated', 'general', 'support', 'f', NULL, '{"ticket_id": 12346}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (68, 5, 'Feedback Request', 'How was your experience with our app?', 'general', 'feedback', 'f', NULL, '{}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (69, 5, 'Account Security', 'Login detected from new device', 'security', 'security', 't', '2025-12-24 14:50:53.967573', '{"device": "Samsung Galaxy S23", "location": "New York"}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (70, 5, 'Device Status Alert', 'Window Sensor is offline', 'alert', 'device', 'f', NULL, '{"device_id": 27, "device_name": "Window Sensor"}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (71, 5, 'Energy Consumption Alert', 'Monthly energy usage is 12% higher than last month', 'reminder', 'energy', 'f', NULL, '{"usage": 520, "increase": 12}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (72, 5, 'System Update', 'App update available - Version 2.5.0', 'system', 'system', 'f', NULL, '{"version": "2.5.0"}', '2025-12-24 16:50:53.967573', NULL);
INSERT INTO "public"."notifications" VALUES (73, 4, 'Device Status Alert', 'Living Room Light is now online', 'alert', 'device', 'f', NULL, '{"device_id": 1, "device_name": "Living Room Light"}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (74, 4, 'Energy Consumption Alert', 'High energy usage detected in Kitchen', 'reminder', 'energy', 'f', NULL, '{"device_id": 13, "consumption": 250}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (75, 4, 'Security Alert', 'Motion detected in Bedroom', 'security', 'security', 't', '2025-12-24 16:02:22.420909', '{"location": "Bedroom", "sensor_id": 8}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (76, 4, 'System Update', 'New firmware available for Smart Thermostat', 'system', 'system', 'f', NULL, '{"device_id": 4, "firmware_version": "2.1.0"}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (77, 4, 'Feature Update', 'New automation features are now available', 'feature', 'feature', 'f', NULL, '{}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (78, 4, 'Bill Reminder', 'Your monthly energy bill is ready', 'reminder', 'bill', 'f', NULL, '{"amount": 125.50, "due_date": "2024-01-15"}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (79, 4, 'Device Maintenance', 'Smoke Detector battery is running low', 'reminder', 'maintenance', 'f', NULL, '{"device_id": 7, "battery_level": 15}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (80, 4, 'Automation Update', 'Welcome Home Automation executed successfully', 'general', 'automation', 't', '2025-12-24 16:32:22.420909', '{"scene_id": 1}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (81, 4, 'Weather Alert', 'Temperature is expected to drop below 0°C tonight', 'alert', 'weather', 'f', NULL, '{"location": "New York", "temperature": -2}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (82, 4, 'Community Update', 'New neighbors joined the smart home community', 'general', 'community', 'f', NULL, '{}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (83, 4, 'Home Invitation', 'You have been invited to manage another home', 'general', 'invitation', 'f', NULL, '{"home_id": 2, "inviter": "Jane Smith"}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (84, 4, 'User Access Alert', 'New device connected to your network', 'security', 'security', 'f', NULL, '{"device_mac": "CC:DD:EE:FF:00:11"}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (85, 4, 'Customer Support', 'Your support ticket #12345 has been updated', 'general', 'support', 'f', NULL, '{"ticket_id": 12345}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (86, 4, 'Feedback Request', 'How was your experience with our app?', 'general', 'feedback', 'f', NULL, '{}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (87, 4, 'Account Security', 'Login detected from new device', 'security', 'security', 't', '2025-12-24 15:02:22.420909', '{"device": "iPhone 14", "location": "New York"}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (88, 4, 'Device Status Alert', 'Window Sensor is offline', 'alert', 'device', 'f', NULL, '{"device_id": 9, "device_name": "Window Sensor"}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (89, 4, 'Energy Consumption Alert', 'Monthly energy usage is 15% higher than last month', 'reminder', 'energy', 'f', NULL, '{"usage": 450, "increase": 15}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (90, 4, 'System Update', 'App update available - Version 2.5.0', 'system', 'system', 'f', NULL, '{"version": "2.5.0"}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (91, 5, 'Device Status Alert', 'Living Room Light is now online', 'alert', 'device', 'f', NULL, '{"device_id": 19, "device_name": "Living Room Light"}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (92, 5, 'Energy Consumption Alert', 'High energy usage detected in Kitchen', 'reminder', 'energy', 'f', NULL, '{"device_id": 31, "consumption": 280}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (93, 5, 'Security Alert', 'Motion detected in Bedroom', 'security', 'security', 't', '2025-12-24 16:02:22.420909', '{"location": "Bedroom", "sensor_id": 26}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (94, 5, 'System Update', 'New firmware available for Smart Thermostat', 'system', 'system', 'f', NULL, '{"device_id": 22, "firmware_version": "2.1.0"}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (95, 5, 'Feature Update', 'New automation features are now available', 'feature', 'feature', 'f', NULL, '{}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (96, 5, 'Bill Reminder', 'Your monthly energy bill is ready', 'reminder', 'bill', 'f', NULL, '{"amount": 145.75, "due_date": "2024-01-15"}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (97, 5, 'Device Maintenance', 'Smoke Detector battery is running low', 'reminder', 'maintenance', 'f', NULL, '{"device_id": 25, "battery_level": 20}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (98, 5, 'Automation Update', 'Welcome Home Automation executed successfully', 'general', 'automation', 't', '2025-12-24 16:32:22.420909', '{"scene_id": 1}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (99, 5, 'Weather Alert', 'Temperature is expected to drop below 0°C tonight', 'alert', 'weather', 'f', NULL, '{"location": "New York", "temperature": -1}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (100, 5, 'Community Update', 'New neighbors joined the smart home community', 'general', 'community', 'f', NULL, '{}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (101, 5, 'Home Invitation', 'You have been invited to manage another home', 'general', 'invitation', 'f', NULL, '{"home_id": 3, "inviter": "Michael Johnson"}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (102, 5, 'User Access Alert', 'New device connected to your network', 'security', 'security', 'f', NULL, '{"device_mac": "DD:EE:FF:00:11:22"}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (103, 5, 'Customer Support', 'Your support ticket #12346 has been updated', 'general', 'support', 'f', NULL, '{"ticket_id": 12346}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (104, 5, 'Feedback Request', 'How was your experience with our app?', 'general', 'feedback', 'f', NULL, '{}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (105, 5, 'Account Security', 'Login detected from new device', 'security', 'security', 't', '2025-12-24 15:02:22.420909', '{"device": "Samsung Galaxy S23", "location": "New York"}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (106, 5, 'Device Status Alert', 'Window Sensor is offline', 'alert', 'device', 'f', NULL, '{"device_id": 27, "device_name": "Window Sensor"}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (107, 5, 'Energy Consumption Alert', 'Monthly energy usage is 12% higher than last month', 'reminder', 'energy', 'f', NULL, '{"usage": 520, "increase": 12}', '2025-12-24 17:02:22.420909', NULL);
INSERT INTO "public"."notifications" VALUES (108, 5, 'System Update', 'App update available - Version 2.5.0', 'system', 'system', 'f', NULL, '{"version": "2.5.0"}', '2025-12-24 17:02:22.420909', NULL);

-- ----------------------------
-- Table structure for password_reset_otp
-- ----------------------------
DROP TABLE IF EXISTS "public"."password_reset_otp";
CREATE TABLE "public"."password_reset_otp" (
  "id" int4 NOT NULL DEFAULT nextval('password_reset_otp_id_seq'::regclass),
  "user_id" int4 NOT NULL,
  "email" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "otp_code" varchar(6) COLLATE "pg_catalog"."default" NOT NULL,
  "expires_at" timestamp(6) NOT NULL,
  "used" bool NOT NULL DEFAULT false,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."password_reset_otp"."otp_code" IS '6-digit OTP code';
COMMENT ON COLUMN "public"."password_reset_otp"."expires_at" IS 'OTP expiration timestamp (typically 10-15 minutes)';
COMMENT ON COLUMN "public"."password_reset_otp"."used" IS 'Whether the OTP has been used';
COMMENT ON TABLE "public"."password_reset_otp" IS 'Stores OTP codes for password reset';

-- ----------------------------
-- Records of password_reset_otp
-- ----------------------------
INSERT INTO "public"."password_reset_otp" VALUES (1, 1, 'tymoshenkovitalii84@gmail.com', '221209', '2025-12-04 00:55:20.159', 'f', '2025-12-04 00:40:20.159627');
INSERT INTO "public"."password_reset_otp" VALUES (2, 1, 'tymoshenkovitalii84@gmail.com', '164793', '2025-12-04 00:56:37.849', 'f', '2025-12-04 00:41:37.850254');
INSERT INTO "public"."password_reset_otp" VALUES (3, 1, 'tymoshenkovitalii84@gmail.com', '826005', '2025-12-04 00:57:18.794', 'f', '2025-12-04 00:42:18.794911');

-- ----------------------------
-- Table structure for rooms
-- ----------------------------
DROP TABLE IF EXISTS "public"."rooms";
CREATE TABLE "public"."rooms" (
  "id" int4 NOT NULL DEFAULT nextval('rooms_id_seq'::regclass),
  "user_id" int4 NOT NULL,
  "name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "home_id" int4
)
;

-- ----------------------------
-- Records of rooms
-- ----------------------------
INSERT INTO "public"."rooms" VALUES (1, 4, 'Living Room', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (2, 4, 'Bedroom', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (3, 4, 'Kitchen', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (4, 4, 'Bathroom', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (5, 4, 'Study Room', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (6, 4, 'Dining Room', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (7, 4, 'Guest Room', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (8, 4, 'Master Bedroom', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (9, 4, 'Home Office', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (10, 4, 'Garage', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (11, 4, 'Basement', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (12, 4, 'Attic', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (13, 4, 'Patio', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (14, 4, 'Garden', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (15, 5, 'Living Room', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (16, 5, 'Bedroom', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (17, 5, 'Kitchen', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (18, 5, 'Bathroom', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (19, 5, 'Study Room', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (20, 5, 'Dining Room', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (21, 5, 'Guest Room', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (22, 5, 'Master Bedroom', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (23, 5, 'Home Office', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (24, 5, 'Balcony', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (25, 5, 'Storage Room', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (26, 5, 'Laundry Room', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (27, 5, 'Pantry', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (28, 5, 'Workshop', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (29, 6, 'Living Room', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (30, 6, 'Bedroom', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (31, 6, 'Kitchen', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (32, 6, 'Bathroom', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (33, 6, 'Study Room', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (34, 6, 'Dining Room', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (35, 6, 'Guest Room', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (36, 6, 'Master Bedroom', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (37, 6, 'Home Office', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (38, 6, 'Rooftop', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (39, 6, 'Media Room', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (40, 6, 'Gym', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (41, 6, 'Wine Cellar', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);
INSERT INTO "public"."rooms" VALUES (42, 6, 'Library', '2025-12-24 16:50:52.909767', '2025-12-24 16:50:52.909767', NULL);

-- ----------------------------
-- Table structure for scene_conditions
-- ----------------------------
DROP TABLE IF EXISTS "public"."scene_conditions";
CREATE TABLE "public"."scene_conditions" (
  "id" int4 NOT NULL DEFAULT nextval('scene_conditions_id_seq'::regclass),
  "scene_id" int4 NOT NULL,
  "type" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "operator" varchar(50) COLLATE "pg_catalog"."default",
  "value" float8,
  "unit" varchar(20) COLLATE "pg_catalog"."default",
  "location" varchar(255) COLLATE "pg_catalog"."default",
  "device_id" int4,
  "device_status" varchar(50) COLLATE "pg_catalog"."default",
  "arm_mode" varchar(50) COLLATE "pg_catalog"."default",
  "metadata" jsonb,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;

-- ----------------------------
-- Records of scene_conditions
-- ----------------------------
INSERT INTO "public"."scene_conditions" VALUES (3, 1, 'schedule_time', 'every_day', 800, NULL, NULL, NULL, NULL, NULL, '{"hour": 8, "minute": 0, "repeat": "every_day"}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (4, 26, 'schedule_time', 'every_day', 800, NULL, NULL, NULL, NULL, NULL, '{"hour": 8, "minute": 0, "repeat": "every_day"}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (5, 51, 'schedule_time', 'every_day', 800, NULL, NULL, NULL, NULL, NULL, '{"hour": 8, "minute": 0, "repeat": "every_day"}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (6, 2, 'location_arrive_at', 'arrive_at', NULL, NULL, '456 Broadway, New York, NY 10013, USA', NULL, NULL, NULL, '{"radius": 50, "latitude": 40.7209, "longitude": -74.0007}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (7, 27, 'location_arrive_at', 'arrive_at', NULL, NULL, '456 Broadway, New York, NY 10013, USA', NULL, NULL, NULL, '{"radius": 50, "latitude": 40.7209, "longitude": -74.0007}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (8, 52, 'location_arrive_at', 'arrive_at', NULL, NULL, '456 Broadway, New York, NY 10013, USA', NULL, NULL, NULL, '{"radius": 50, "latitude": 40.7209, "longitude": -74.0007}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (9, 3, 'arm_mode', 'work', NULL, NULL, NULL, NULL, NULL, 'work', '{"mode": "work"}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (10, 28, 'arm_mode', 'work', NULL, NULL, NULL, NULL, NULL, 'work', '{"mode": "work"}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (11, 53, 'arm_mode', 'work', NULL, NULL, NULL, NULL, NULL, 'work', '{"mode": "work"}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (12, 4, 'tap_to_run', 'manual', NULL, NULL, NULL, NULL, NULL, NULL, '{}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (13, 29, 'tap_to_run', 'manual', NULL, NULL, NULL, NULL, NULL, NULL, '{}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (14, 54, 'tap_to_run', 'manual', NULL, NULL, NULL, NULL, NULL, NULL, '{}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (15, 5, 'schedule_time', 'every_day', 2200, NULL, NULL, NULL, NULL, NULL, '{"hour": 22, "minute": 0, "repeat": "every_day"}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (16, 30, 'schedule_time', 'every_day', 2200, NULL, NULL, NULL, NULL, NULL, '{"hour": 22, "minute": 0, "repeat": "every_day"}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (17, 55, 'schedule_time', 'every_day', 2200, NULL, NULL, NULL, NULL, NULL, '{"hour": 22, "minute": 0, "repeat": "every_day"}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (18, 6, 'temperature', '>', 25, 'celsius', 'New York City', NULL, NULL, NULL, '{"threshold": 25}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (19, 31, 'temperature', '>', 25, 'celsius', 'New York City', NULL, NULL, NULL, '{"threshold": 25}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (20, 56, 'temperature', '>', 25, 'celsius', 'New York City', NULL, NULL, NULL, '{"threshold": 25}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (21, 6, 'humidity', '>', 60, 'percent', 'New York City', NULL, NULL, NULL, '{"threshold": 60}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (22, 31, 'humidity', '>', 60, 'percent', 'New York City', NULL, NULL, NULL, '{"threshold": 60}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (23, 56, 'humidity', '>', 60, 'percent', 'New York City', NULL, NULL, NULL, '{"threshold": 60}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (24, 7, 'location_arrive_at', 'arrive_at', NULL, NULL, '701 7th Ave, New York, NY 10036, USA', NULL, NULL, NULL, '{"radius": 50, "latitude": 40.7579, "longitude": -73.9877}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (25, 32, 'location_arrive_at', 'arrive_at', NULL, NULL, '701 7th Ave, New York, NY 10036, USA', NULL, NULL, NULL, '{"radius": 50, "latitude": 40.7579, "longitude": -73.9877}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (26, 57, 'location_arrive_at', 'arrive_at', NULL, NULL, '701 7th Ave, New York, NY 10036, USA', NULL, NULL, NULL, '{"radius": 50, "latitude": 40.7579, "longitude": -73.9877}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (27, 8, 'schedule_time', 'every_day', 2145, NULL, NULL, NULL, NULL, NULL, '{"hour": 21, "minute": 45, "repeat": "every_day"}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (28, 33, 'schedule_time', 'every_day', 2145, NULL, NULL, NULL, NULL, NULL, '{"hour": 21, "minute": 45, "repeat": "every_day"}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (29, 58, 'schedule_time', 'every_day', 2145, NULL, NULL, NULL, NULL, NULL, '{"hour": 21, "minute": 45, "repeat": "every_day"}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (30, 9, 'location_leave', 'leave', NULL, NULL, '701 7th Ave, New York, NY 10036, USA', NULL, NULL, NULL, '{"radius": 50, "latitude": 40.7579, "longitude": -73.9877}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (31, 34, 'location_leave', 'leave', NULL, NULL, '701 7th Ave, New York, NY 10036, USA', NULL, NULL, NULL, '{"radius": 50, "latitude": 40.7579, "longitude": -73.9877}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (32, 59, 'location_leave', 'leave', NULL, NULL, '701 7th Ave, New York, NY 10036, USA', NULL, NULL, NULL, '{"radius": 50, "latitude": 40.7579, "longitude": -73.9877}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (33, 10, 'schedule_time', 'every_weekday', 700, NULL, NULL, NULL, NULL, NULL, '{"hour": 7, "minute": 0, "repeat": "every_weekday"}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (34, 35, 'schedule_time', 'every_weekday', 700, NULL, NULL, NULL, NULL, NULL, '{"hour": 7, "minute": 0, "repeat": "every_weekday"}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (35, 60, 'schedule_time', 'every_weekday', 700, NULL, NULL, NULL, NULL, NULL, '{"hour": 7, "minute": 0, "repeat": "every_weekday"}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (36, 11, 'schedule_time', 'every_day', 1800, NULL, NULL, NULL, NULL, NULL, '{"hour": 18, "minute": 0, "repeat": "every_day"}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (37, 36, 'schedule_time', 'every_day', 1800, NULL, NULL, NULL, NULL, NULL, '{"hour": 18, "minute": 0, "repeat": "every_day"}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (38, 61, 'schedule_time', 'every_day', 1800, NULL, NULL, NULL, NULL, NULL, '{"hour": 18, "minute": 0, "repeat": "every_day"}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (39, 12, 'schedule_time', 'every_weekend', 900, NULL, NULL, NULL, NULL, NULL, '{"hour": 9, "minute": 0, "repeat": "every_weekend"}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (40, 37, 'schedule_time', 'every_weekend', 900, NULL, NULL, NULL, NULL, NULL, '{"hour": 9, "minute": 0, "repeat": "every_weekend"}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (41, 62, 'schedule_time', 'every_weekend', 900, NULL, NULL, NULL, NULL, NULL, '{"hour": 9, "minute": 0, "repeat": "every_weekend"}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (42, 13, 'arm_mode', 'arm_away', NULL, NULL, NULL, NULL, NULL, 'arm_away', '{"mode": "arm_away"}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (43, 38, 'arm_mode', 'arm_away', NULL, NULL, NULL, NULL, NULL, 'arm_away', '{"mode": "arm_away"}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (44, 63, 'arm_mode', 'arm_away', NULL, NULL, NULL, NULL, NULL, 'arm_away', '{"mode": "arm_away"}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (45, 14, 'tap_to_run', 'manual', NULL, NULL, NULL, NULL, NULL, NULL, '{}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (46, 39, 'tap_to_run', 'manual', NULL, NULL, NULL, NULL, NULL, NULL, '{}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (47, 64, 'tap_to_run', 'manual', NULL, NULL, NULL, NULL, NULL, NULL, '{}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (48, 15, 'schedule_time', 'custom', 2000, NULL, NULL, NULL, NULL, NULL, '{"days": ["friday", "saturday"], "hour": 20, "minute": 0, "repeat": "custom"}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (49, 40, 'schedule_time', 'custom', 2000, NULL, NULL, NULL, NULL, NULL, '{"days": ["friday", "saturday"], "hour": 20, "minute": 0, "repeat": "custom"}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (50, 65, 'schedule_time', 'custom', 2000, NULL, NULL, NULL, NULL, NULL, '{"days": ["friday", "saturday"], "hour": 20, "minute": 0, "repeat": "custom"}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (51, 6, 'device_status', '=', NULL, NULL, NULL, 3, 'online', NULL, '{}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (52, 31, 'device_status', '=', NULL, NULL, NULL, 3, 'online', NULL, '{}', '2025-12-24 17:02:22.424239');
INSERT INTO "public"."scene_conditions" VALUES (53, 56, 'device_status', '=', NULL, NULL, NULL, 3, 'online', NULL, '{}', '2025-12-24 17:02:22.424239');

-- ----------------------------
-- Table structure for scene_execution_logs
-- ----------------------------
DROP TABLE IF EXISTS "public"."scene_execution_logs";
CREATE TABLE "public"."scene_execution_logs" (
  "id" int4 NOT NULL DEFAULT nextval('scene_execution_logs_id_seq'::regclass),
  "scene_id" int4 NOT NULL,
  "scene_name" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "user_id" int4 NOT NULL,
  "home_id" int4,
  "status" varchar(20) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'succeeded'::character varying,
  "error_message" text COLLATE "pg_catalog"."default",
  "execution_timestamp" timestamp(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "metadata" jsonb
)
;

-- ----------------------------
-- Records of scene_execution_logs
-- ----------------------------

-- ----------------------------
-- Table structure for scene_tasks
-- ----------------------------
DROP TABLE IF EXISTS "public"."scene_tasks";
CREATE TABLE "public"."scene_tasks" (
  "id" int4 NOT NULL DEFAULT nextval('scene_tasks_id_seq'::regclass),
  "scene_id" int4 NOT NULL,
  "type" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "device_id" int4,
  "device_name" varchar(255) COLLATE "pg_catalog"."default",
  "room_name" varchar(255) COLLATE "pg_catalog"."default",
  "function" varchar(50) COLLATE "pg_catalog"."default",
  "scene_id_target" int4,
  "scene_name" varchar(255) COLLATE "pg_catalog"."default",
  "arm_mode" varchar(50) COLLATE "pg_catalog"."default",
  "notification_message" text COLLATE "pg_catalog"."default",
  "delay_seconds" int4,
  "order_index" int4 NOT NULL DEFAULT 0,
  "metadata" jsonb,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;

-- ----------------------------
-- Records of scene_tasks
-- ----------------------------
INSERT INTO "public"."scene_tasks" VALUES (1, 1, 'control_device', NULL, 'All Lights', NULL, 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (2, 26, 'control_device', NULL, 'All Lights', NULL, 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (3, 51, 'control_device', NULL, 'All Lights', NULL, 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (4, 2, 'control_device', 4, 'Smart Thermostat', 'Living Room', 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (5, 27, 'control_device', 4, 'Smart Thermostat', 'Living Room', 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (6, 52, 'control_device', 4, 'Smart Thermostat', 'Living Room', 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (7, 2, 'delay', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 30, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (8, 27, 'delay', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 30, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (9, 52, 'delay', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 30, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (10, 3, 'change_arm_mode', NULL, NULL, NULL, NULL, NULL, NULL, 'energy_saver', NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (11, 28, 'change_arm_mode', NULL, NULL, NULL, NULL, NULL, NULL, 'energy_saver', NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (12, 53, 'change_arm_mode', NULL, NULL, NULL, NULL, NULL, NULL, 'energy_saver', NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (13, 3, 'send_notification', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Energy Saver Mode Activated', NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (14, 28, 'send_notification', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Energy Saver Mode Activated', NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (15, 53, 'send_notification', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Energy Saver Mode Activated', NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (16, 4, 'change_arm_mode', NULL, NULL, NULL, NULL, NULL, NULL, 'work', NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (17, 29, 'change_arm_mode', NULL, NULL, NULL, NULL, NULL, NULL, 'work', NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (18, 54, 'change_arm_mode', NULL, NULL, NULL, NULL, NULL, NULL, 'work', NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (19, 5, 'control_device', 2, 'Bedroom Lights', 'Bedroom', 'DIM', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (20, 30, 'control_device', 2, 'Bedroom Lights', 'Bedroom', 'DIM', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (21, 55, 'control_device', 2, 'Bedroom Lights', 'Bedroom', 'DIM', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (22, 5, 'control_device', 4, 'Thermostat', 'Living Room', 'SET_20', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (23, 30, 'control_device', 4, 'Thermostat', 'Living Room', 'SET_20', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (24, 55, 'control_device', 4, 'Thermostat', 'Living Room', 'SET_20', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (25, 6, 'control_device', 15, 'Air Conditioner', 'Living Room', 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (26, 31, 'control_device', 15, 'Air Conditioner', 'Living Room', 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (27, 56, 'control_device', 15, 'Air Conditioner', 'Living Room', 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (28, 7, 'select_scene', NULL, NULL, NULL, NULL, 1, 'Turn ON All the Lights', NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (29, 7, 'select_scene', NULL, NULL, NULL, NULL, 26, 'Turn ON All the Lights', NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (30, 7, 'select_scene', NULL, NULL, NULL, NULL, 51, 'Turn ON All the Lights', NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (31, 32, 'select_scene', NULL, NULL, NULL, NULL, 1, 'Turn ON All the Lights', NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (32, 32, 'select_scene', NULL, NULL, NULL, NULL, 26, 'Turn ON All the Lights', NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (33, 32, 'select_scene', NULL, NULL, NULL, NULL, 51, 'Turn ON All the Lights', NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (34, 57, 'select_scene', NULL, NULL, NULL, NULL, 1, 'Turn ON All the Lights', NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (35, 57, 'select_scene', NULL, NULL, NULL, NULL, 26, 'Turn ON All the Lights', NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (36, 57, 'select_scene', NULL, NULL, NULL, NULL, 51, 'Turn ON All the Lights', NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (37, 8, 'delay', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 930, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (38, 33, 'delay', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 930, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (39, 58, 'delay', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, 930, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (40, 8, 'control_device', NULL, 'All Lights', NULL, 'OFF', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (41, 33, 'control_device', NULL, 'All Lights', NULL, 'OFF', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (42, 58, 'control_device', NULL, 'All Lights', NULL, 'OFF', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (43, 9, 'control_device', NULL, 'All Devices', NULL, 'OFF', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (44, 34, 'control_device', NULL, 'All Devices', NULL, 'OFF', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (45, 59, 'control_device', NULL, 'All Devices', NULL, 'OFF', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (46, 10, 'control_device', NULL, 'All Lights', NULL, 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (47, 35, 'control_device', NULL, 'All Lights', NULL, 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (48, 60, 'control_device', NULL, 'All Lights', NULL, 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (49, 10, 'control_device', 4, 'Thermostat', NULL, 'SET_22', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (50, 35, 'control_device', 4, 'Thermostat', NULL, 'SET_22', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (51, 60, 'control_device', 4, 'Thermostat', NULL, 'SET_22', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (52, 10, 'send_notification', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Good morning! Your home is ready.', NULL, 2, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (53, 35, 'send_notification', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Good morning! Your home is ready.', NULL, 2, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (54, 60, 'send_notification', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Good morning! Your home is ready.', NULL, 2, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (55, 11, 'control_device', 1, 'Living Room Lights', 'Living Room', 'DIM', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (56, 36, 'control_device', 1, 'Living Room Lights', 'Living Room', 'DIM', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (57, 61, 'control_device', 1, 'Living Room Lights', 'Living Room', 'DIM', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (58, 11, 'control_device', 12, 'Smart TV', 'Living Room', 'ON', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (59, 36, 'control_device', 12, 'Smart TV', 'Living Room', 'ON', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (60, 61, 'control_device', 12, 'Smart TV', 'Living Room', 'ON', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (61, 12, 'control_device', NULL, 'All Lights', NULL, 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (62, 37, 'control_device', NULL, 'All Lights', NULL, 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (63, 62, 'control_device', NULL, 'All Lights', NULL, 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (64, 12, 'control_device', 4, 'Thermostat', NULL, 'SET_24', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (65, 37, 'control_device', 4, 'Thermostat', NULL, 'SET_24', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (66, 62, 'control_device', 4, 'Thermostat', NULL, 'SET_24', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (67, 13, 'change_arm_mode', NULL, NULL, NULL, NULL, NULL, NULL, 'arm_away', NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (68, 38, 'change_arm_mode', NULL, NULL, NULL, NULL, NULL, NULL, 'arm_away', NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (69, 63, 'change_arm_mode', NULL, NULL, NULL, NULL, NULL, NULL, 'arm_away', NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (70, 13, 'control_device', NULL, 'All Devices', NULL, 'OFF', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (71, 38, 'control_device', NULL, 'All Devices', NULL, 'OFF', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (72, 63, 'control_device', NULL, 'All Devices', NULL, 'OFF', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (73, 13, 'send_notification', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Vacation mode activated. Your home is secure.', NULL, 2, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (74, 38, 'send_notification', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Vacation mode activated. Your home is secure.', NULL, 2, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (75, 63, 'send_notification', NULL, NULL, NULL, NULL, NULL, NULL, NULL, 'Vacation mode activated. Your home is secure.', NULL, 2, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (76, 14, 'control_device', NULL, 'All Lights', NULL, 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (77, 39, 'control_device', NULL, 'All Lights', NULL, 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (78, 64, 'control_device', NULL, 'All Lights', NULL, 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (79, 14, 'control_device', 10, 'Smart Speaker', NULL, 'PLAY_MUSIC', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (80, 39, 'control_device', 10, 'Smart Speaker', NULL, 'PLAY_MUSIC', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (81, 64, 'control_device', 10, 'Smart Speaker', NULL, 'PLAY_MUSIC', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (82, 15, 'control_device', 1, 'Living Room Lights', 'Living Room', 'DIM', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (83, 40, 'control_device', 1, 'Living Room Lights', 'Living Room', 'DIM', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (84, 65, 'control_device', 1, 'Living Room Lights', 'Living Room', 'DIM', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (85, 15, 'control_device', 12, 'Smart TV', 'Living Room', 'ON', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (86, 40, 'control_device', 12, 'Smart TV', 'Living Room', 'ON', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (87, 65, 'control_device', 12, 'Smart TV', 'Living Room', 'ON', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (88, 16, 'control_device', NULL, 'All Lights', NULL, 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (89, 41, 'control_device', NULL, 'All Lights', NULL, 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (90, 66, 'control_device', NULL, 'All Lights', NULL, 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (91, 17, 'control_device', NULL, 'All Devices', NULL, 'OFF', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (92, 42, 'control_device', NULL, 'All Devices', NULL, 'OFF', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (93, 67, 'control_device', NULL, 'All Devices', NULL, 'OFF', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (94, 18, 'control_device', 2, 'Bedroom Lights', 'Bedroom', 'DIM', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (95, 43, 'control_device', 2, 'Bedroom Lights', 'Bedroom', 'DIM', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (96, 68, 'control_device', 2, 'Bedroom Lights', 'Bedroom', 'DIM', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (97, 19, 'control_device', 1, 'Living Room Lights', 'Living Room', 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (98, 44, 'control_device', 1, 'Living Room Lights', 'Living Room', 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (99, 69, 'control_device', 1, 'Living Room Lights', 'Living Room', 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (100, 20, 'control_device', NULL, 'Office Lights', 'Study Room', 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (101, 45, 'control_device', NULL, 'Office Lights', 'Study Room', 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (102, 70, 'control_device', NULL, 'Office Lights', 'Study Room', 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (103, 21, 'control_device', 1, 'Living Room Lights', 'Living Room', 'DIM', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (104, 46, 'control_device', 1, 'Living Room Lights', 'Living Room', 'DIM', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (105, 71, 'control_device', 1, 'Living Room Lights', 'Living Room', 'DIM', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (106, 21, 'control_device', 10, 'Smart Speaker', NULL, 'PLAY_RELAX', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (107, 46, 'control_device', 10, 'Smart Speaker', NULL, 'PLAY_RELAX', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (108, 71, 'control_device', 10, 'Smart Speaker', NULL, 'PLAY_RELAX', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (109, 22, 'control_device', NULL, 'Office Lights', 'Study Room', 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (110, 47, 'control_device', NULL, 'Office Lights', 'Study Room', 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (111, 72, 'control_device', NULL, 'Office Lights', 'Study Room', 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (112, 22, 'control_device', 10, 'Smart Speaker', NULL, 'MUTE', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (113, 47, 'control_device', 10, 'Smart Speaker', NULL, 'MUTE', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (114, 72, 'control_device', 10, 'Smart Speaker', NULL, 'MUTE', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (115, 23, 'control_device', NULL, 'All Lights', NULL, 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (116, 48, 'control_device', NULL, 'All Lights', NULL, 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (117, 73, 'control_device', NULL, 'All Lights', NULL, 'ON', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (118, 24, 'change_arm_mode', NULL, NULL, NULL, NULL, NULL, NULL, 'arm_stay', NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (119, 49, 'change_arm_mode', NULL, NULL, NULL, NULL, NULL, NULL, 'arm_stay', NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (120, 74, 'change_arm_mode', NULL, NULL, NULL, NULL, NULL, NULL, 'arm_stay', NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (121, 25, 'control_device', 4, 'Thermostat', NULL, 'SET_22', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (122, 50, 'control_device', 4, 'Thermostat', NULL, 'SET_22', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (123, 75, 'control_device', 4, 'Thermostat', NULL, 'SET_22', NULL, NULL, NULL, NULL, NULL, 0, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (124, 25, 'control_device', 1, 'Living Room Lights', 'Living Room', 'ON', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (125, 50, 'control_device', 1, 'Living Room Lights', 'Living Room', 'ON', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');
INSERT INTO "public"."scene_tasks" VALUES (126, 75, 'control_device', 1, 'Living Room Lights', 'Living Room', 'ON', NULL, NULL, NULL, NULL, NULL, 1, '{}', '2025-12-24 17:02:22.42752');

-- ----------------------------
-- Table structure for smart_scenes
-- ----------------------------
DROP TABLE IF EXISTS "public"."smart_scenes";
CREATE TABLE "public"."smart_scenes" (
  "id" int4 NOT NULL DEFAULT nextval('smart_scenes_id_seq'::regclass),
  "user_id" int4 NOT NULL,
  "home_id" int4,
  "name" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "type" varchar(50) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'automation'::character varying,
  "condition_logic" varchar(10) COLLATE "pg_catalog"."default" NOT NULL DEFAULT 'any'::character varying,
  "icon" varchar(100) COLLATE "pg_catalog"."default",
  "color" varchar(20) COLLATE "pg_catalog"."default",
  "is_enabled" bool NOT NULL DEFAULT true,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "order_index" int4 NOT NULL DEFAULT 0
)
;

-- ----------------------------
-- Records of smart_scenes
-- ----------------------------
INSERT INTO "public"."smart_scenes" VALUES (1, 4, 19, 'Turn ON All the Lights', 'automation', 'any', 'ic_sun', '#405FF2', 't', '2025-12-24 16:50:52.92204', '2025-12-24 16:50:52.92204', 0);
INSERT INTO "public"."smart_scenes" VALUES (2, 4, 19, 'Go to Office', 'automation', 'any', 'ic_briefcase', '#26A69A', 't', '2025-12-24 16:50:52.92204', '2025-12-24 16:50:52.92204', 0);
INSERT INTO "public"."smart_scenes" VALUES (3, 4, 19, 'Energy Saver Mode', 'automation', 'any', 'ic_energy', '#FFA726', 'f', '2025-12-24 16:50:52.92204', '2025-12-24 16:50:52.92204', 0);
INSERT INTO "public"."smart_scenes" VALUES (4, 4, 19, 'Work Mode Activate', 'automation', 'any', 'ic_work', '#42A5F5', 't', '2025-12-24 16:50:52.92204', '2025-12-24 16:50:52.92204', 0);
INSERT INTO "public"."smart_scenes" VALUES (5, 4, 19, 'Night Time Bliss', 'automation', 'any', 'ic_moon', '#6C5CE7', 't', '2025-12-24 16:50:52.92204', '2025-12-24 16:50:52.92204', 0);
INSERT INTO "public"."smart_scenes" VALUES (6, 4, 19, 'Turn on the AC', 'automation', 'any', 'ic_air', '#E74C3C', 't', '2025-12-24 16:50:52.92204', '2025-12-24 16:50:52.92204', 0);
INSERT INTO "public"."smart_scenes" VALUES (7, 4, 19, 'Welcome Home Automation', 'automation', 'any', 'ic_sun', '#FF6B35', 't', '2025-12-24 16:50:52.92204', '2025-12-24 16:50:52.92204', 0);
INSERT INTO "public"."smart_scenes" VALUES (8, 4, 19, 'Bedtime Bliss Automation', 'automation', 'any', 'ic_clock', '#9B59B6', 't', '2025-12-24 16:50:52.92204', '2025-12-24 16:50:52.92204', 0);
INSERT INTO "public"."smart_scenes" VALUES (9, 4, 19, 'Leave Home Automation', 'automation', 'any', 'ic_location', '#E74C3C', 't', '2025-12-24 16:50:52.92204', '2025-12-24 16:50:52.92204', 0);
INSERT INTO "public"."smart_scenes" VALUES (10, 4, 19, 'Morning Routine', 'automation', 'all', 'ic_sunrise', '#F39C12', 't', '2025-12-24 16:50:52.92204', '2025-12-24 16:50:52.92204', 0);
INSERT INTO "public"."smart_scenes" VALUES (11, 4, 19, 'Evening Routine', 'automation', 'all', 'ic_sunset', '#E67E22', 't', '2025-12-24 16:50:52.92204', '2025-12-24 16:50:52.92204', 0);
INSERT INTO "public"."smart_scenes" VALUES (12, 4, 19, 'Weekend Mode', 'automation', 'any', 'ic_weekend', '#3498DB', 't', '2025-12-24 16:50:52.92204', '2025-12-24 16:50:52.92204', 0);
INSERT INTO "public"."smart_scenes" VALUES (13, 4, 19, 'Vacation Mode', 'automation', 'all', 'ic_vacation', '#16A085', 'f', '2025-12-24 16:50:52.92204', '2025-12-24 16:50:52.92204', 0);
INSERT INTO "public"."smart_scenes" VALUES (14, 4, 19, 'Party Mode', 'automation', 'any', 'ic_party', '#E91E63', 'f', '2025-12-24 16:50:52.92204', '2025-12-24 16:50:52.92204', 0);
INSERT INTO "public"."smart_scenes" VALUES (15, 4, 19, 'Movie Night', 'automation', 'all', 'ic_movie', '#673AB7', 't', '2025-12-24 16:50:52.92204', '2025-12-24 16:50:52.92204', 0);
INSERT INTO "public"."smart_scenes" VALUES (16, 4, 19, 'Quick Light On', 'tap_to_run', 'any', 'ic_light', '#FFC107', 't', '2025-12-24 16:50:52.92204', '2025-12-24 16:50:52.92204', 0);
INSERT INTO "public"."smart_scenes" VALUES (17, 4, 19, 'All Devices Off', 'tap_to_run', 'any', 'ic_power', '#607D8B', 't', '2025-12-24 16:50:52.92204', '2025-12-24 16:50:52.92204', 0);
INSERT INTO "public"."smart_scenes" VALUES (18, 4, 19, 'Bedtime Prep', 'tap_to_run', 'any', 'ic_moon', '#6C5CE7', 't', '2025-12-24 16:50:52.92204', '2025-12-24 16:50:52.92204', 0);
INSERT INTO "public"."smart_scenes" VALUES (19, 4, 19, 'Evening Chill', 'tap_to_run', 'any', 'ic_sun', '#FFA726', 't', '2025-12-24 16:50:52.92204', '2025-12-24 16:50:52.92204', 0);
INSERT INTO "public"."smart_scenes" VALUES (20, 4, 19, 'Boost Productivity', 'tap_to_run', 'any', 'ic_briefcase', '#26A69A', 't', '2025-12-24 16:50:52.92204', '2025-12-24 16:50:52.92204', 0);
INSERT INTO "public"."smart_scenes" VALUES (21, 4, 19, 'Relaxation Mode', 'tap_to_run', 'any', 'ic_relax', '#8E24AA', 't', '2025-12-24 16:50:52.92204', '2025-12-24 16:50:52.92204', 0);
INSERT INTO "public"."smart_scenes" VALUES (22, 4, 19, 'Focus Mode', 'tap_to_run', 'any', 'ic_focus', '#00ACC1', 't', '2025-12-24 16:50:52.92204', '2025-12-24 16:50:52.92204', 0);
INSERT INTO "public"."smart_scenes" VALUES (23, 4, 19, 'Energy Boost', 'tap_to_run', 'any', 'ic_energy', '#FF9800', 't', '2025-12-24 16:50:52.92204', '2025-12-24 16:50:52.92204', 0);
INSERT INTO "public"."smart_scenes" VALUES (24, 4, 19, 'Security On', 'tap_to_run', 'any', 'ic_security', '#D32F2F', 't', '2025-12-24 16:50:52.92204', '2025-12-24 16:50:52.92204', 0);
INSERT INTO "public"."smart_scenes" VALUES (25, 4, 19, 'Comfort Zone', 'tap_to_run', 'any', 'ic_comfort', '#5C6BC0', 't', '2025-12-24 16:50:52.92204', '2025-12-24 16:50:52.92204', 0);
INSERT INTO "public"."smart_scenes" VALUES (26, 4, 19, 'Turn ON All the Lights', 'automation', 'any', 'ic_sun', '#405FF2', 't', '2025-12-24 16:50:53.969038', '2025-12-24 16:50:53.969038', 0);
INSERT INTO "public"."smart_scenes" VALUES (27, 4, 19, 'Go to Office', 'automation', 'any', 'ic_briefcase', '#26A69A', 't', '2025-12-24 16:50:53.969038', '2025-12-24 16:50:53.969038', 0);
INSERT INTO "public"."smart_scenes" VALUES (28, 4, 19, 'Energy Saver Mode', 'automation', 'any', 'ic_energy', '#FFA726', 'f', '2025-12-24 16:50:53.969038', '2025-12-24 16:50:53.969038', 0);
INSERT INTO "public"."smart_scenes" VALUES (29, 4, 19, 'Work Mode Activate', 'automation', 'any', 'ic_work', '#42A5F5', 't', '2025-12-24 16:50:53.969038', '2025-12-24 16:50:53.969038', 0);
INSERT INTO "public"."smart_scenes" VALUES (30, 4, 19, 'Night Time Bliss', 'automation', 'any', 'ic_moon', '#6C5CE7', 't', '2025-12-24 16:50:53.969038', '2025-12-24 16:50:53.969038', 0);
INSERT INTO "public"."smart_scenes" VALUES (31, 4, 19, 'Turn on the AC', 'automation', 'any', 'ic_air', '#E74C3C', 't', '2025-12-24 16:50:53.969038', '2025-12-24 16:50:53.969038', 0);
INSERT INTO "public"."smart_scenes" VALUES (32, 4, 19, 'Welcome Home Automation', 'automation', 'any', 'ic_sun', '#FF6B35', 't', '2025-12-24 16:50:53.969038', '2025-12-24 16:50:53.969038', 0);
INSERT INTO "public"."smart_scenes" VALUES (33, 4, 19, 'Bedtime Bliss Automation', 'automation', 'any', 'ic_clock', '#9B59B6', 't', '2025-12-24 16:50:53.969038', '2025-12-24 16:50:53.969038', 0);
INSERT INTO "public"."smart_scenes" VALUES (34, 4, 19, 'Leave Home Automation', 'automation', 'any', 'ic_location', '#E74C3C', 't', '2025-12-24 16:50:53.969038', '2025-12-24 16:50:53.969038', 0);
INSERT INTO "public"."smart_scenes" VALUES (35, 4, 19, 'Morning Routine', 'automation', 'all', 'ic_sunrise', '#F39C12', 't', '2025-12-24 16:50:53.969038', '2025-12-24 16:50:53.969038', 0);
INSERT INTO "public"."smart_scenes" VALUES (36, 4, 19, 'Evening Routine', 'automation', 'all', 'ic_sunset', '#E67E22', 't', '2025-12-24 16:50:53.969038', '2025-12-24 16:50:53.969038', 0);
INSERT INTO "public"."smart_scenes" VALUES (37, 4, 19, 'Weekend Mode', 'automation', 'any', 'ic_weekend', '#3498DB', 't', '2025-12-24 16:50:53.969038', '2025-12-24 16:50:53.969038', 0);
INSERT INTO "public"."smart_scenes" VALUES (38, 4, 19, 'Vacation Mode', 'automation', 'all', 'ic_vacation', '#16A085', 'f', '2025-12-24 16:50:53.969038', '2025-12-24 16:50:53.969038', 0);
INSERT INTO "public"."smart_scenes" VALUES (39, 4, 19, 'Party Mode', 'automation', 'any', 'ic_party', '#E91E63', 'f', '2025-12-24 16:50:53.969038', '2025-12-24 16:50:53.969038', 0);
INSERT INTO "public"."smart_scenes" VALUES (40, 4, 19, 'Movie Night', 'automation', 'all', 'ic_movie', '#673AB7', 't', '2025-12-24 16:50:53.969038', '2025-12-24 16:50:53.969038', 0);
INSERT INTO "public"."smart_scenes" VALUES (41, 4, 19, 'Quick Light On', 'tap_to_run', 'any', 'ic_light', '#FFC107', 't', '2025-12-24 16:50:53.969038', '2025-12-24 16:50:53.969038', 0);
INSERT INTO "public"."smart_scenes" VALUES (42, 4, 19, 'All Devices Off', 'tap_to_run', 'any', 'ic_power', '#607D8B', 't', '2025-12-24 16:50:53.969038', '2025-12-24 16:50:53.969038', 0);
INSERT INTO "public"."smart_scenes" VALUES (43, 4, 19, 'Bedtime Prep', 'tap_to_run', 'any', 'ic_moon', '#6C5CE7', 't', '2025-12-24 16:50:53.969038', '2025-12-24 16:50:53.969038', 0);
INSERT INTO "public"."smart_scenes" VALUES (44, 4, 19, 'Evening Chill', 'tap_to_run', 'any', 'ic_sun', '#FFA726', 't', '2025-12-24 16:50:53.969038', '2025-12-24 16:50:53.969038', 0);
INSERT INTO "public"."smart_scenes" VALUES (45, 4, 19, 'Boost Productivity', 'tap_to_run', 'any', 'ic_briefcase', '#26A69A', 't', '2025-12-24 16:50:53.969038', '2025-12-24 16:50:53.969038', 0);
INSERT INTO "public"."smart_scenes" VALUES (46, 4, 19, 'Relaxation Mode', 'tap_to_run', 'any', 'ic_relax', '#8E24AA', 't', '2025-12-24 16:50:53.969038', '2025-12-24 16:50:53.969038', 0);
INSERT INTO "public"."smart_scenes" VALUES (47, 4, 19, 'Focus Mode', 'tap_to_run', 'any', 'ic_focus', '#00ACC1', 't', '2025-12-24 16:50:53.969038', '2025-12-24 16:50:53.969038', 0);
INSERT INTO "public"."smart_scenes" VALUES (48, 4, 19, 'Energy Boost', 'tap_to_run', 'any', 'ic_energy', '#FF9800', 't', '2025-12-24 16:50:53.969038', '2025-12-24 16:50:53.969038', 0);
INSERT INTO "public"."smart_scenes" VALUES (49, 4, 19, 'Security On', 'tap_to_run', 'any', 'ic_security', '#D32F2F', 't', '2025-12-24 16:50:53.969038', '2025-12-24 16:50:53.969038', 0);
INSERT INTO "public"."smart_scenes" VALUES (50, 4, 19, 'Comfort Zone', 'tap_to_run', 'any', 'ic_comfort', '#5C6BC0', 't', '2025-12-24 16:50:53.969038', '2025-12-24 16:50:53.969038', 0);
INSERT INTO "public"."smart_scenes" VALUES (51, 4, 19, 'Turn ON All the Lights', 'automation', 'any', 'ic_sun', '#405FF2', 't', '2025-12-24 17:02:22.422951', '2025-12-24 17:02:22.422951', 0);
INSERT INTO "public"."smart_scenes" VALUES (52, 4, 19, 'Go to Office', 'automation', 'any', 'ic_briefcase', '#26A69A', 't', '2025-12-24 17:02:22.422951', '2025-12-24 17:02:22.422951', 0);
INSERT INTO "public"."smart_scenes" VALUES (53, 4, 19, 'Energy Saver Mode', 'automation', 'any', 'ic_energy', '#FFA726', 'f', '2025-12-24 17:02:22.422951', '2025-12-24 17:02:22.422951', 0);
INSERT INTO "public"."smart_scenes" VALUES (54, 4, 19, 'Work Mode Activate', 'automation', 'any', 'ic_work', '#42A5F5', 't', '2025-12-24 17:02:22.422951', '2025-12-24 17:02:22.422951', 0);
INSERT INTO "public"."smart_scenes" VALUES (55, 4, 19, 'Night Time Bliss', 'automation', 'any', 'ic_moon', '#6C5CE7', 't', '2025-12-24 17:02:22.422951', '2025-12-24 17:02:22.422951', 0);
INSERT INTO "public"."smart_scenes" VALUES (56, 4, 19, 'Turn on the AC', 'automation', 'any', 'ic_air', '#E74C3C', 't', '2025-12-24 17:02:22.422951', '2025-12-24 17:02:22.422951', 0);
INSERT INTO "public"."smart_scenes" VALUES (57, 4, 19, 'Welcome Home Automation', 'automation', 'any', 'ic_sun', '#FF6B35', 't', '2025-12-24 17:02:22.422951', '2025-12-24 17:02:22.422951', 0);
INSERT INTO "public"."smart_scenes" VALUES (58, 4, 19, 'Bedtime Bliss Automation', 'automation', 'any', 'ic_clock', '#9B59B6', 't', '2025-12-24 17:02:22.422951', '2025-12-24 17:02:22.422951', 0);
INSERT INTO "public"."smart_scenes" VALUES (59, 4, 19, 'Leave Home Automation', 'automation', 'any', 'ic_location', '#E74C3C', 't', '2025-12-24 17:02:22.422951', '2025-12-24 17:02:22.422951', 0);
INSERT INTO "public"."smart_scenes" VALUES (60, 4, 19, 'Morning Routine', 'automation', 'all', 'ic_sunrise', '#F39C12', 't', '2025-12-24 17:02:22.422951', '2025-12-24 17:02:22.422951', 0);
INSERT INTO "public"."smart_scenes" VALUES (61, 4, 19, 'Evening Routine', 'automation', 'all', 'ic_sunset', '#E67E22', 't', '2025-12-24 17:02:22.422951', '2025-12-24 17:02:22.422951', 0);
INSERT INTO "public"."smart_scenes" VALUES (62, 4, 19, 'Weekend Mode', 'automation', 'any', 'ic_weekend', '#3498DB', 't', '2025-12-24 17:02:22.422951', '2025-12-24 17:02:22.422951', 0);
INSERT INTO "public"."smart_scenes" VALUES (63, 4, 19, 'Vacation Mode', 'automation', 'all', 'ic_vacation', '#16A085', 'f', '2025-12-24 17:02:22.422951', '2025-12-24 17:02:22.422951', 0);
INSERT INTO "public"."smart_scenes" VALUES (64, 4, 19, 'Party Mode', 'automation', 'any', 'ic_party', '#E91E63', 'f', '2025-12-24 17:02:22.422951', '2025-12-24 17:02:22.422951', 0);
INSERT INTO "public"."smart_scenes" VALUES (65, 4, 19, 'Movie Night', 'automation', 'all', 'ic_movie', '#673AB7', 't', '2025-12-24 17:02:22.422951', '2025-12-24 17:02:22.422951', 0);
INSERT INTO "public"."smart_scenes" VALUES (66, 4, 19, 'Quick Light On', 'tap_to_run', 'any', 'ic_light', '#FFC107', 't', '2025-12-24 17:02:22.422951', '2025-12-24 17:02:22.422951', 0);
INSERT INTO "public"."smart_scenes" VALUES (67, 4, 19, 'All Devices Off', 'tap_to_run', 'any', 'ic_power', '#607D8B', 't', '2025-12-24 17:02:22.422951', '2025-12-24 17:02:22.422951', 0);
INSERT INTO "public"."smart_scenes" VALUES (68, 4, 19, 'Bedtime Prep', 'tap_to_run', 'any', 'ic_moon', '#6C5CE7', 't', '2025-12-24 17:02:22.422951', '2025-12-24 17:02:22.422951', 0);
INSERT INTO "public"."smart_scenes" VALUES (69, 4, 19, 'Evening Chill', 'tap_to_run', 'any', 'ic_sun', '#FFA726', 't', '2025-12-24 17:02:22.422951', '2025-12-24 17:02:22.422951', 0);
INSERT INTO "public"."smart_scenes" VALUES (70, 4, 19, 'Boost Productivity', 'tap_to_run', 'any', 'ic_briefcase', '#26A69A', 't', '2025-12-24 17:02:22.422951', '2025-12-24 17:02:22.422951', 0);
INSERT INTO "public"."smart_scenes" VALUES (71, 4, 19, 'Relaxation Mode', 'tap_to_run', 'any', 'ic_relax', '#8E24AA', 't', '2025-12-24 17:02:22.422951', '2025-12-24 17:02:22.422951', 0);
INSERT INTO "public"."smart_scenes" VALUES (72, 4, 19, 'Focus Mode', 'tap_to_run', 'any', 'ic_focus', '#00ACC1', 't', '2025-12-24 17:02:22.422951', '2025-12-24 17:02:22.422951', 0);
INSERT INTO "public"."smart_scenes" VALUES (73, 4, 19, 'Energy Boost', 'tap_to_run', 'any', 'ic_energy', '#FF9800', 't', '2025-12-24 17:02:22.422951', '2025-12-24 17:02:22.422951', 0);
INSERT INTO "public"."smart_scenes" VALUES (74, 4, 19, 'Security On', 'tap_to_run', 'any', 'ic_security', '#D32F2F', 't', '2025-12-24 17:02:22.422951', '2025-12-24 17:02:22.422951', 0);
INSERT INTO "public"."smart_scenes" VALUES (75, 4, 19, 'Comfort Zone', 'tap_to_run', 'any', 'ic_comfort', '#5C6BC0', 't', '2025-12-24 17:02:22.422951', '2025-12-24 17:02:22.422951', 0);

-- ----------------------------
-- Table structure for user_actions
-- ----------------------------
DROP TABLE IF EXISTS "public"."user_actions";
CREATE TABLE "public"."user_actions" (
  "id" int4 NOT NULL DEFAULT nextval('user_actions_id_seq'::regclass),
  "user_id" int4 NOT NULL,
  "action_type" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "action_category" varchar(50) COLLATE "pg_catalog"."default" NOT NULL,
  "endpoint" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "method" varchar(10) COLLATE "pg_catalog"."default" NOT NULL,
  "request_body" jsonb,
  "response_status" int4,
  "response_body" jsonb,
  "ip_address" varchar(45) COLLATE "pg_catalog"."default",
  "user_agent" text COLLATE "pg_catalog"."default",
  "device_info" jsonb,
  "session_id" varchar(255) COLLATE "pg_catalog"."default",
  "duration_ms" int4,
  "error_message" text COLLATE "pg_catalog"."default",
  "metadata" jsonb,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;
COMMENT ON COLUMN "public"."user_actions"."action_type" IS 'Type of action: login, logout, device_control, settings_update, etc.';
COMMENT ON COLUMN "public"."user_actions"."action_category" IS 'Category: authentication, device_management, settings, reports, etc.';
COMMENT ON COLUMN "public"."user_actions"."endpoint" IS 'API endpoint that was called';
COMMENT ON COLUMN "public"."user_actions"."method" IS 'HTTP method: GET, POST, PUT, DELETE';
COMMENT ON COLUMN "public"."user_actions"."request_body" IS 'Request body data (sanitized, no passwords)';
COMMENT ON COLUMN "public"."user_actions"."response_status" IS 'HTTP response status code';
COMMENT ON COLUMN "public"."user_actions"."response_body" IS 'Response body (may be truncated for large responses)';
COMMENT ON COLUMN "public"."user_actions"."ip_address" IS 'IP address of the request';
COMMENT ON COLUMN "public"."user_actions"."user_agent" IS 'User agent string';
COMMENT ON COLUMN "public"."user_actions"."device_info" IS 'Device information extracted from user agent';
COMMENT ON COLUMN "public"."user_actions"."session_id" IS 'Session identifier if available';
COMMENT ON COLUMN "public"."user_actions"."duration_ms" IS 'Request duration in milliseconds';
COMMENT ON COLUMN "public"."user_actions"."error_message" IS 'Error message if action failed';
COMMENT ON COLUMN "public"."user_actions"."metadata" IS 'Additional metadata in JSON format';
COMMENT ON TABLE "public"."user_actions" IS 'Stores all user actions for audit trail and analytics';

-- ----------------------------
-- Records of user_actions
-- ----------------------------

-- ----------------------------
-- Table structure for user_additional_settings
-- ----------------------------
DROP TABLE IF EXISTS "public"."user_additional_settings";
CREATE TABLE "public"."user_additional_settings" (
  "id" int4 NOT NULL DEFAULT nextval('user_additional_settings_id_seq'::regclass),
  "user_id" int4 NOT NULL,
  "setting_key" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "setting_value" jsonb NOT NULL,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;

-- ----------------------------
-- Records of user_additional_settings
-- ----------------------------

-- ----------------------------
-- Table structure for user_linked_assistants
-- ----------------------------
DROP TABLE IF EXISTS "public"."user_linked_assistants";
CREATE TABLE "public"."user_linked_assistants" (
  "id" int4 NOT NULL DEFAULT nextval('user_linked_assistants_id_seq'::regclass),
  "user_id" int4 NOT NULL,
  "assistant_id" int4 NOT NULL,
  "access_token" text COLLATE "pg_catalog"."default",
  "refresh_token" text COLLATE "pg_catalog"."default",
  "metadata" jsonb,
  "linked_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;

-- ----------------------------
-- Records of user_linked_assistants
-- ----------------------------

-- ----------------------------
-- Table structure for user_notification_preferences
-- ----------------------------
DROP TABLE IF EXISTS "public"."user_notification_preferences";
CREATE TABLE "public"."user_notification_preferences" (
  "id" int4 NOT NULL DEFAULT nextval('user_notification_preferences_id_seq'::regclass),
  "user_id" int4 NOT NULL,
  "preference_key" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "preference_value" jsonb NOT NULL,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;

-- ----------------------------
-- Records of user_notification_preferences
-- ----------------------------
INSERT INTO "public"."user_notification_preferences" VALUES (1, 1, 'bill_reminders', 'true', '2025-12-08 15:19:29.252978', '2025-12-08 15:19:42.15094');
INSERT INTO "public"."user_notification_preferences" VALUES (2, 1, 'security_alerts', 'true', '2025-12-08 15:19:29.252978', '2025-12-08 15:19:42.15094');
INSERT INTO "public"."user_notification_preferences" VALUES (3, 1, 'feedback_updates', 'false', '2025-12-08 15:19:29.252978', '2025-12-08 15:19:42.15094');
INSERT INTO "public"."user_notification_preferences" VALUES (4, 1, 'home_invitations', 'true', '2025-12-08 15:19:29.252978', '2025-12-08 15:19:42.15094');
INSERT INTO "public"."user_notification_preferences" VALUES (5, 1, 'community_updates', 'false', '2025-12-08 15:19:29.252978', '2025-12-08 15:19:42.15094');
INSERT INTO "public"."user_notification_preferences" VALUES (6, 1, 'automation_updates', 'false', '2025-12-08 15:19:29.252978', '2025-12-08 15:19:42.15094');
INSERT INTO "public"."user_notification_preferences" VALUES (7, 1, 'user_access_alerts', 'false', '2025-12-08 15:19:29.252978', '2025-12-08 15:19:42.15094');
INSERT INTO "public"."user_notification_preferences" VALUES (8, 1, 'device_status_alerts', 'true', '2025-12-08 15:19:29.252978', '2025-12-08 15:19:42.15094');
INSERT INTO "public"."user_notification_preferences" VALUES (9, 1, 'customer_support_updates', 'false', '2025-12-08 15:19:29.252978', '2025-12-08 15:19:42.15094');
INSERT INTO "public"."user_notification_preferences" VALUES (10, 1, 'energy_consumption_alerts', 'true', '2025-12-08 15:19:29.252978', '2025-12-08 15:19:42.15094');
INSERT INTO "public"."user_notification_preferences" VALUES (11, 1, 'weather_based_suggestions', 'true', '2025-12-08 15:19:29.252978', '2025-12-08 15:19:42.15094');
INSERT INTO "public"."user_notification_preferences" VALUES (12, 1, 'device_maintenance_reminders', 'false', '2025-12-08 15:19:29.252978', '2025-12-08 15:19:42.15094');

-- ----------------------------
-- Table structure for user_profile_metadata
-- ----------------------------
DROP TABLE IF EXISTS "public"."user_profile_metadata";
CREATE TABLE "public"."user_profile_metadata" (
  "id" int4 NOT NULL DEFAULT nextval('user_profile_metadata_id_seq'::regclass),
  "user_id" int4 NOT NULL,
  "metadata_key" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "metadata_value" jsonb NOT NULL,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;

-- ----------------------------
-- Records of user_profile_metadata
-- ----------------------------

-- ----------------------------
-- Table structure for user_security_settings
-- ----------------------------
DROP TABLE IF EXISTS "public"."user_security_settings";
CREATE TABLE "public"."user_security_settings" (
  "id" int4 NOT NULL DEFAULT nextval('user_security_settings_id_seq'::regclass),
  "user_id" int4 NOT NULL,
  "setting_key" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "setting_value" jsonb NOT NULL,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;

-- ----------------------------
-- Records of user_security_settings
-- ----------------------------
INSERT INTO "public"."user_security_settings" VALUES (1, 1, 'face_id', 'false', '2025-12-08 15:19:29.252978', '2025-12-08 15:19:42.15094');
INSERT INTO "public"."user_security_settings" VALUES (2, 1, 'biometric_id', 'false', '2025-12-08 15:19:29.252978', '2025-12-08 15:19:42.15094');
INSERT INTO "public"."user_security_settings" VALUES (3, 1, 'sms_authenticator', 'false', '2025-12-08 15:19:29.252978', '2025-12-08 15:19:42.15094');
INSERT INTO "public"."user_security_settings" VALUES (4, 1, 'google_authenticator', 'false', '2025-12-08 15:19:29.252978', '2025-12-08 15:19:42.15094');

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS "public"."users";
CREATE TABLE "public"."users" (
  "id" int4 NOT NULL DEFAULT nextval('users_id_seq'::regclass),
  "email" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "password" varchar(255) COLLATE "pg_catalog"."default" NOT NULL,
  "first_name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "last_name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "phone" varchar(20) COLLATE "pg_catalog"."default",
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "notification_preferences" jsonb DEFAULT '{}'::jsonb,
  "security_settings" jsonb DEFAULT '{}'::jsonb,
  "profile_metadata" jsonb DEFAULT '{}'::jsonb,
  "additional_settings" jsonb DEFAULT '{}'::jsonb
)
;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO "public"."users" VALUES (1, 'tymoshenkovitalii84@gmail.com', '$2a$10$V4OguFrQABou5R7j9O61ae2IFnwjxVok4DPatsZlkioyG/OYZ3P6G', 'Test', 'User', NULL, '2025-12-02 16:49:43.99803', '2025-12-06 01:17:17.11422', '{"bill_reminders": true, "security_alerts": true, "feedback_updates": false, "home_invitations": true, "community_updates": false, "automation_updates": false, "user_access_alerts": false, "device_status_alerts": true, "customer_support_updates": false, "energy_consumption_alerts": true, "weather_based_suggestions": true, "device_maintenance_reminders": false}', '{"face_id": false, "biometric_id": false, "sms_authenticator": false, "google_authenticator": false}', '{}', '{}');
INSERT INTO "public"."users" VALUES (4, 'john.doe@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'John', 'Doe', '+1-555-0101', '2025-12-24 16:44:07.23792', '2025-12-24 16:44:07.23792', '{}', '{}', '{}', '{}');
INSERT INTO "public"."users" VALUES (5, 'jane.smith@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Jane', 'Smith', '+1-555-0102', '2025-12-24 16:44:07.23792', '2025-12-24 16:44:07.23792', '{}', '{}', '{}', '{}');
INSERT INTO "public"."users" VALUES (6, 'michael.johnson@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Michael', 'Johnson', '+1-555-0103', '2025-12-24 16:44:07.23792', '2025-12-24 16:44:07.23792', '{}', '{}', '{}', '{}');
INSERT INTO "public"."users" VALUES (7, 'sarah.williams@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Sarah', 'Williams', '+1-555-0104', '2025-12-24 16:44:07.23792', '2025-12-24 16:44:07.23792', '{}', '{}', '{}', '{}');
INSERT INTO "public"."users" VALUES (8, 'david.brown@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'David', 'Brown', '+1-555-0105', '2025-12-24 16:44:07.23792', '2025-12-24 16:44:07.23792', '{}', '{}', '{}', '{}');
INSERT INTO "public"."users" VALUES (9, 'emily.davis@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Emily', 'Davis', '+1-555-0106', '2025-12-24 16:44:07.23792', '2025-12-24 16:44:07.23792', '{}', '{}', '{}', '{}');
INSERT INTO "public"."users" VALUES (10, 'james.miller@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'James', 'Miller', '+1-555-0107', '2025-12-24 16:44:07.23792', '2025-12-24 16:44:07.23792', '{}', '{}', '{}', '{}');
INSERT INTO "public"."users" VALUES (11, 'olivia.wilson@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Olivia', 'Wilson', '+1-555-0108', '2025-12-24 16:44:07.23792', '2025-12-24 16:44:07.23792', '{}', '{}', '{}', '{}');
INSERT INTO "public"."users" VALUES (12, 'william.moore@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'William', 'Moore', '+1-555-0109', '2025-12-24 16:44:07.23792', '2025-12-24 16:44:07.23792', '{}', '{}', '{}', '{}');
INSERT INTO "public"."users" VALUES (13, 'sophia.taylor@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Sophia', 'Taylor', '+1-555-0110', '2025-12-24 16:44:07.23792', '2025-12-24 16:44:07.23792', '{}', '{}', '{}', '{}');
INSERT INTO "public"."users" VALUES (14, 'benjamin.anderson@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Benjamin', 'Anderson', '+1-555-0111', '2025-12-24 16:44:07.23792', '2025-12-24 16:44:07.23792', '{}', '{}', '{}', '{}');
INSERT INTO "public"."users" VALUES (15, 'isabella.thomas@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Isabella', 'Thomas', '+1-555-0112', '2025-12-24 16:44:07.23792', '2025-12-24 16:44:07.23792', '{}', '{}', '{}', '{}');
INSERT INTO "public"."users" VALUES (16, 'lucas.jackson@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Lucas', 'Jackson', '+1-555-0113', '2025-12-24 16:44:07.23792', '2025-12-24 16:44:07.23792', '{}', '{}', '{}', '{}');
INSERT INTO "public"."users" VALUES (17, 'mia.white@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Mia', 'White', '+1-555-0114', '2025-12-24 16:44:07.23792', '2025-12-24 16:44:07.23792', '{}', '{}', '{}', '{}');
INSERT INTO "public"."users" VALUES (18, 'henry.harris@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Henry', 'Harris', '+1-555-0115', '2025-12-24 16:44:07.23792', '2025-12-24 16:44:07.23792', '{}', '{}', '{}', '{}');
INSERT INTO "public"."users" VALUES (19, 'charlotte.martin@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Charlotte', 'Martin', '+1-555-0116', '2025-12-24 16:44:07.23792', '2025-12-24 16:44:07.23792', '{}', '{}', '{}', '{}');
INSERT INTO "public"."users" VALUES (20, 'alexander.thompson@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Alexander', 'Thompson', '+1-555-0117', '2025-12-24 16:44:07.23792', '2025-12-24 16:44:07.23792', '{}', '{}', '{}', '{}');
INSERT INTO "public"."users" VALUES (21, 'amelia.garcia@example.com', '$2b$10$rOzJqZqZqZqZqZqZqZqZqO', 'Amelia', 'Garcia', '+1-555-0118', '2025-12-24 16:44:07.23792', '2025-12-24 16:44:07.23792', '{}', '{}', '{}', '{}');

-- ----------------------------
-- Table structure for voice_assistants
-- ----------------------------
DROP TABLE IF EXISTS "public"."voice_assistants";
CREATE TABLE "public"."voice_assistants" (
  "id" int4 NOT NULL DEFAULT nextval('voice_assistants_id_seq'::regclass),
  "name" varchar(100) COLLATE "pg_catalog"."default" NOT NULL,
  "created_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP,
  "updated_at" timestamp(6) DEFAULT CURRENT_TIMESTAMP
)
;

-- ----------------------------
-- Records of voice_assistants
-- ----------------------------
INSERT INTO "public"."voice_assistants" VALUES (1, 'Google Assistant', '2025-12-06 00:53:50.932934', '2025-12-06 00:53:50.932934');
INSERT INTO "public"."voice_assistants" VALUES (2, 'Amazon Alexa', '2025-12-06 00:53:50.932934', '2025-12-06 00:53:50.932934');
INSERT INTO "public"."voice_assistants" VALUES (3, 'Microsoft Cortana', '2025-12-06 00:53:50.932934', '2025-12-06 00:53:50.932934');
INSERT INTO "public"."voice_assistants" VALUES (4, 'Samsung Bixby', '2025-12-06 00:53:50.932934', '2025-12-06 00:53:50.932934');
INSERT INTO "public"."voice_assistants" VALUES (5, 'Naver Clova', '2025-12-06 00:53:50.932934', '2025-12-06 00:53:50.932934');
INSERT INTO "public"."voice_assistants" VALUES (6, 'Apple Siri', '2025-12-06 00:53:50.932934', '2025-12-06 00:53:50.932934');

-- ----------------------------
-- Function structure for update_linked_accounts_updated_at
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."update_linked_accounts_updated_at"();
CREATE FUNCTION "public"."update_linked_accounts_updated_at"()
  RETURNS "pg_catalog"."trigger" AS $BODY$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;

-- ----------------------------
-- Function structure for update_updated_at_column
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."update_updated_at_column"();
CREATE FUNCTION "public"."update_updated_at_column"()
  RETURNS "pg_catalog"."trigger" AS $BODY$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;

-- ----------------------------
-- Function structure for update_user_additional_settings_updated_at
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."update_user_additional_settings_updated_at"();
CREATE FUNCTION "public"."update_user_additional_settings_updated_at"()
  RETURNS "pg_catalog"."trigger" AS $BODY$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;

-- ----------------------------
-- Function structure for update_user_settings_updated_at
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."update_user_settings_updated_at"();
CREATE FUNCTION "public"."update_user_settings_updated_at"()
  RETURNS "pg_catalog"."trigger" AS $BODY$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;

-- ----------------------------
-- Function structure for update_voice_assistants_updated_at
-- ----------------------------
DROP FUNCTION IF EXISTS "public"."update_voice_assistants_updated_at"();
CREATE FUNCTION "public"."update_voice_assistants_updated_at"()
  RETURNS "pg_catalog"."trigger" AS $BODY$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100;

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."chatbot_messages_id_seq"
OWNED BY "public"."chatbot_messages"."id";
SELECT setval('"public"."chatbot_messages_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."devices_id_seq"
OWNED BY "public"."devices"."id";
SELECT setval('"public"."devices_id_seq"', 102, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."energy_consumption_id_seq"
OWNED BY "public"."energy_consumption"."id";
SELECT setval('"public"."energy_consumption_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."home_invitations_id_seq"
OWNED BY "public"."home_invitations"."id";
SELECT setval('"public"."home_invitations_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."home_members_id_seq"
OWNED BY "public"."home_members"."id";
SELECT setval('"public"."home_members_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."homes_id_seq"
OWNED BY "public"."homes"."id";
SELECT setval('"public"."homes_id_seq"', 72, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."linked_accounts_id_seq"
OWNED BY "public"."linked_accounts"."id";
SELECT setval('"public"."linked_accounts_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."migrations_id_seq"
OWNED BY "public"."migrations"."id";
SELECT setval('"public"."migrations_id_seq"', 17, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."notifications_id_seq"
OWNED BY "public"."notifications"."id";
SELECT setval('"public"."notifications_id_seq"', 108, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."password_reset_otp_id_seq"
OWNED BY "public"."password_reset_otp"."id";
SELECT setval('"public"."password_reset_otp_id_seq"', 3, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."rooms_id_seq"
OWNED BY "public"."rooms"."id";
SELECT setval('"public"."rooms_id_seq"', 126, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."scene_conditions_id_seq"
OWNED BY "public"."scene_conditions"."id";
SELECT setval('"public"."scene_conditions_id_seq"', 53, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."scene_execution_logs_id_seq"
OWNED BY "public"."scene_execution_logs"."id";
SELECT setval('"public"."scene_execution_logs_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."scene_tasks_id_seq"
OWNED BY "public"."scene_tasks"."id";
SELECT setval('"public"."scene_tasks_id_seq"', 126, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."smart_scenes_id_seq"
OWNED BY "public"."smart_scenes"."id";
SELECT setval('"public"."smart_scenes_id_seq"', 75, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."user_actions_id_seq"
OWNED BY "public"."user_actions"."id";
SELECT setval('"public"."user_actions_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."user_additional_settings_id_seq"
OWNED BY "public"."user_additional_settings"."id";
SELECT setval('"public"."user_additional_settings_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."user_linked_assistants_id_seq"
OWNED BY "public"."user_linked_assistants"."id";
SELECT setval('"public"."user_linked_assistants_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."user_notification_preferences_id_seq"
OWNED BY "public"."user_notification_preferences"."id";
SELECT setval('"public"."user_notification_preferences_id_seq"', 48, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."user_profile_metadata_id_seq"
OWNED BY "public"."user_profile_metadata"."id";
SELECT setval('"public"."user_profile_metadata_id_seq"', 1, false);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."user_security_settings_id_seq"
OWNED BY "public"."user_security_settings"."id";
SELECT setval('"public"."user_security_settings_id_seq"', 16, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."users_id_seq"
OWNED BY "public"."users"."id";
SELECT setval('"public"."users_id_seq"', 75, true);

-- ----------------------------
-- Alter sequences owned by
-- ----------------------------
ALTER SEQUENCE "public"."voice_assistants_id_seq"
OWNED BY "public"."voice_assistants"."id";
SELECT setval('"public"."voice_assistants_id_seq"', 6, true);

-- ----------------------------
-- Indexes structure for table chatbot_messages
-- ----------------------------
CREATE INDEX "idx_chatbot_role" ON "public"."chatbot_messages" USING btree (
  "role" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_chatbot_user_created" ON "public"."chatbot_messages" USING btree (
  "user_id" "pg_catalog"."int4_ops" ASC NULLS LAST,
  "created_at" "pg_catalog"."timestamp_ops" DESC NULLS FIRST
);
CREATE INDEX "idx_chatbot_user_id" ON "public"."chatbot_messages" USING btree (
  "user_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table chatbot_messages
-- ----------------------------
ALTER TABLE "public"."chatbot_messages" ADD CONSTRAINT "chatbot_messages_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table devices
-- ----------------------------
CREATE INDEX "idx_device_room_id" ON "public"."devices" USING btree (
  "room_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_mac_address" ON "public"."devices" USING btree (
  "mac_address" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_status" ON "public"."devices" USING btree (
  "status" "pg_catalog"."enum_ops" ASC NULLS LAST
);
CREATE INDEX "idx_type" ON "public"."devices" USING btree (
  "type" "pg_catalog"."enum_ops" ASC NULLS LAST
);
CREATE INDEX "idx_user_id" ON "public"."devices" USING btree (
  "user_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Triggers structure for table devices
-- ----------------------------
CREATE TRIGGER "update_devices_updated_at" BEFORE UPDATE ON "public"."devices"
FOR EACH ROW
EXECUTE PROCEDURE "public"."update_updated_at_column"();

-- ----------------------------
-- Uniques structure for table devices
-- ----------------------------
ALTER TABLE "public"."devices" ADD CONSTRAINT "devices_user_id_mac_address_key" UNIQUE ("user_id", "mac_address");

-- ----------------------------
-- Primary Key structure for table devices
-- ----------------------------
ALTER TABLE "public"."devices" ADD CONSTRAINT "devices_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table energy_consumption
-- ----------------------------
CREATE INDEX "idx_energy_date" ON "public"."energy_consumption" USING btree (
  "date" "pg_catalog"."date_ops" ASC NULLS LAST
);
CREATE INDEX "idx_energy_device_date" ON "public"."energy_consumption" USING btree (
  "device_id" "pg_catalog"."int4_ops" ASC NULLS LAST,
  "date" "pg_catalog"."date_ops" ASC NULLS LAST
);
CREATE INDEX "idx_energy_device_id" ON "public"."energy_consumption" USING btree (
  "device_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Triggers structure for table energy_consumption
-- ----------------------------
CREATE TRIGGER "update_energy_consumption_updated_at" BEFORE UPDATE ON "public"."energy_consumption"
FOR EACH ROW
EXECUTE PROCEDURE "public"."update_updated_at_column"();

-- ----------------------------
-- Uniques structure for table energy_consumption
-- ----------------------------
ALTER TABLE "public"."energy_consumption" ADD CONSTRAINT "energy_consumption_device_id_date_key" UNIQUE ("device_id", "date");

-- ----------------------------
-- Primary Key structure for table energy_consumption
-- ----------------------------
ALTER TABLE "public"."energy_consumption" ADD CONSTRAINT "energy_consumption_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table home_invitations
-- ----------------------------
CREATE INDEX "idx_home_invitations_active" ON "public"."home_invitations" USING btree (
  "is_active" "pg_catalog"."bool_ops" ASC NULLS LAST,
  "expires_at" "pg_catalog"."timestamp_ops" ASC NULLS LAST
) WHERE is_active = true;
CREATE INDEX "idx_home_invitations_code" ON "public"."home_invitations" USING btree (
  "code" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_home_invitations_home_id" ON "public"."home_invitations" USING btree (
  "home_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table home_invitations
-- ----------------------------
ALTER TABLE "public"."home_invitations" ADD CONSTRAINT "home_invitations_code_key" UNIQUE ("code");

-- ----------------------------
-- Primary Key structure for table home_invitations
-- ----------------------------
ALTER TABLE "public"."home_invitations" ADD CONSTRAINT "home_invitations_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table home_members
-- ----------------------------
CREATE INDEX "idx_home_members_home_id" ON "public"."home_members" USING btree (
  "home_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_home_members_role" ON "public"."home_members" USING btree (
  "home_id" "pg_catalog"."int4_ops" ASC NULLS LAST,
  "role" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_home_members_user_id" ON "public"."home_members" USING btree (
  "user_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table home_members
-- ----------------------------
ALTER TABLE "public"."home_members" ADD CONSTRAINT "home_members_home_id_user_id_key" UNIQUE ("home_id", "user_id");

-- ----------------------------
-- Primary Key structure for table home_members
-- ----------------------------
ALTER TABLE "public"."home_members" ADD CONSTRAINT "home_members_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table homes
-- ----------------------------
CREATE INDEX "idx_homes_is_primary" ON "public"."homes" USING btree (
  "user_id" "pg_catalog"."int4_ops" ASC NULLS LAST,
  "is_primary" "pg_catalog"."bool_ops" ASC NULLS LAST
) WHERE is_primary = true;
CREATE INDEX "idx_homes_user_id" ON "public"."homes" USING btree (
  "user_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Triggers structure for table homes
-- ----------------------------
CREATE TRIGGER "update_homes_updated_at" BEFORE UPDATE ON "public"."homes"
FOR EACH ROW
EXECUTE PROCEDURE "public"."update_updated_at_column"();

-- ----------------------------
-- Uniques structure for table homes
-- ----------------------------
ALTER TABLE "public"."homes" ADD CONSTRAINT "homes_user_id_name_key" UNIQUE ("user_id", "name");

-- ----------------------------
-- Primary Key structure for table homes
-- ----------------------------
ALTER TABLE "public"."homes" ADD CONSTRAINT "homes_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table linked_accounts
-- ----------------------------
CREATE INDEX "idx_linked_accounts_provider" ON "public"."linked_accounts" USING btree (
  "provider" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_linked_accounts_user_id" ON "public"."linked_accounts" USING btree (
  "user_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Triggers structure for table linked_accounts
-- ----------------------------
CREATE TRIGGER "trigger_update_linked_accounts_updated_at" BEFORE UPDATE ON "public"."linked_accounts"
FOR EACH ROW
EXECUTE PROCEDURE "public"."update_linked_accounts_updated_at"();

-- ----------------------------
-- Uniques structure for table linked_accounts
-- ----------------------------
ALTER TABLE "public"."linked_accounts" ADD CONSTRAINT "linked_accounts_user_id_provider_key" UNIQUE ("user_id", "provider");

-- ----------------------------
-- Primary Key structure for table linked_accounts
-- ----------------------------
ALTER TABLE "public"."linked_accounts" ADD CONSTRAINT "linked_accounts_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Uniques structure for table migrations
-- ----------------------------
ALTER TABLE "public"."migrations" ADD CONSTRAINT "migrations_name_key" UNIQUE ("name");

-- ----------------------------
-- Primary Key structure for table migrations
-- ----------------------------
ALTER TABLE "public"."migrations" ADD CONSTRAINT "migrations_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table notifications
-- ----------------------------
CREATE INDEX "idx_notification_category" ON "public"."notifications" USING btree (
  "user_id" "pg_catalog"."int4_ops" ASC NULLS LAST,
  "category" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_notification_created_at" ON "public"."notifications" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" DESC NULLS FIRST
);
CREATE INDEX "idx_notification_type" ON "public"."notifications" USING btree (
  "type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_notification_user_id" ON "public"."notifications" USING btree (
  "user_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_notification_user_read" ON "public"."notifications" USING btree (
  "user_id" "pg_catalog"."int4_ops" ASC NULLS LAST,
  "is_read" "pg_catalog"."bool_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table notifications
-- ----------------------------
ALTER TABLE "public"."notifications" ADD CONSTRAINT "notifications_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table password_reset_otp
-- ----------------------------
CREATE INDEX "idx_otp_code" ON "public"."password_reset_otp" USING btree (
  "otp_code" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_otp_email" ON "public"."password_reset_otp" USING btree (
  "email" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_otp_expires_at" ON "public"."password_reset_otp" USING btree (
  "expires_at" "pg_catalog"."timestamp_ops" ASC NULLS LAST
);
CREATE INDEX "idx_otp_user_id" ON "public"."password_reset_otp" USING btree (
  "user_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table password_reset_otp
-- ----------------------------
ALTER TABLE "public"."password_reset_otp" ADD CONSTRAINT "password_reset_otp_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table rooms
-- ----------------------------
CREATE INDEX "idx_room_home_id" ON "public"."rooms" USING btree (
  "home_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_room_user_id" ON "public"."rooms" USING btree (
  "user_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Triggers structure for table rooms
-- ----------------------------
CREATE TRIGGER "update_rooms_updated_at" BEFORE UPDATE ON "public"."rooms"
FOR EACH ROW
EXECUTE PROCEDURE "public"."update_updated_at_column"();

-- ----------------------------
-- Uniques structure for table rooms
-- ----------------------------
ALTER TABLE "public"."rooms" ADD CONSTRAINT "rooms_user_id_name_key" UNIQUE ("user_id", "name");

-- ----------------------------
-- Primary Key structure for table rooms
-- ----------------------------
ALTER TABLE "public"."rooms" ADD CONSTRAINT "rooms_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table scene_conditions
-- ----------------------------
CREATE INDEX "idx_conditions_scene_id" ON "public"."scene_conditions" USING btree (
  "scene_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_conditions_type" ON "public"."scene_conditions" USING btree (
  "type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table scene_conditions
-- ----------------------------
ALTER TABLE "public"."scene_conditions" ADD CONSTRAINT "scene_conditions_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table scene_execution_logs
-- ----------------------------
CREATE INDEX "idx_logs_scene_id" ON "public"."scene_execution_logs" USING btree (
  "scene_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_logs_status" ON "public"."scene_execution_logs" USING btree (
  "status" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_logs_timestamp" ON "public"."scene_execution_logs" USING btree (
  "execution_timestamp" "pg_catalog"."timestamp_ops" DESC NULLS FIRST
);
CREATE INDEX "idx_logs_user_id" ON "public"."scene_execution_logs" USING btree (
  "user_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_logs_user_timestamp" ON "public"."scene_execution_logs" USING btree (
  "user_id" "pg_catalog"."int4_ops" ASC NULLS LAST,
  "execution_timestamp" "pg_catalog"."timestamp_ops" DESC NULLS FIRST
);

-- ----------------------------
-- Primary Key structure for table scene_execution_logs
-- ----------------------------
ALTER TABLE "public"."scene_execution_logs" ADD CONSTRAINT "scene_execution_logs_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table scene_tasks
-- ----------------------------
CREATE INDEX "idx_tasks_order" ON "public"."scene_tasks" USING btree (
  "scene_id" "pg_catalog"."int4_ops" ASC NULLS LAST,
  "order_index" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_tasks_scene_id" ON "public"."scene_tasks" USING btree (
  "scene_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_tasks_type" ON "public"."scene_tasks" USING btree (
  "type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table scene_tasks
-- ----------------------------
ALTER TABLE "public"."scene_tasks" ADD CONSTRAINT "scene_tasks_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table smart_scenes
-- ----------------------------
CREATE INDEX "idx_scenes_enabled" ON "public"."smart_scenes" USING btree (
  "is_enabled" "pg_catalog"."bool_ops" ASC NULLS LAST
);
CREATE INDEX "idx_scenes_home_id" ON "public"."smart_scenes" USING btree (
  "home_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_scenes_order" ON "public"."smart_scenes" USING btree (
  "user_id" "pg_catalog"."int4_ops" ASC NULLS LAST,
  "type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST,
  "order_index" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_scenes_type" ON "public"."smart_scenes" USING btree (
  "type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_scenes_user_id" ON "public"."smart_scenes" USING btree (
  "user_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Triggers structure for table smart_scenes
-- ----------------------------
CREATE TRIGGER "update_scenes_updated_at" BEFORE UPDATE ON "public"."smart_scenes"
FOR EACH ROW
EXECUTE PROCEDURE "public"."update_updated_at_column"();

-- ----------------------------
-- Primary Key structure for table smart_scenes
-- ----------------------------
ALTER TABLE "public"."smart_scenes" ADD CONSTRAINT "smart_scenes_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table user_actions
-- ----------------------------
CREATE INDEX "idx_user_actions_action_category" ON "public"."user_actions" USING btree (
  "action_category" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_user_actions_action_type" ON "public"."user_actions" USING btree (
  "action_type" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_user_actions_created_at" ON "public"."user_actions" USING btree (
  "created_at" "pg_catalog"."timestamp_ops" DESC NULLS FIRST
);
CREATE INDEX "idx_user_actions_endpoint" ON "public"."user_actions" USING btree (
  "endpoint" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_user_actions_user_created" ON "public"."user_actions" USING btree (
  "user_id" "pg_catalog"."int4_ops" ASC NULLS LAST,
  "created_at" "pg_catalog"."timestamp_ops" DESC NULLS FIRST
);
CREATE INDEX "idx_user_actions_user_id" ON "public"."user_actions" USING btree (
  "user_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Primary Key structure for table user_actions
-- ----------------------------
ALTER TABLE "public"."user_actions" ADD CONSTRAINT "user_actions_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table user_additional_settings
-- ----------------------------
CREATE INDEX "idx_additional_setting_user_id" ON "public"."user_additional_settings" USING btree (
  "user_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Triggers structure for table user_additional_settings
-- ----------------------------
CREATE TRIGGER "update_additional_setting_updated_at" BEFORE UPDATE ON "public"."user_additional_settings"
FOR EACH ROW
EXECUTE PROCEDURE "public"."update_updated_at_column"();

-- ----------------------------
-- Uniques structure for table user_additional_settings
-- ----------------------------
ALTER TABLE "public"."user_additional_settings" ADD CONSTRAINT "user_additional_settings_user_id_setting_key_key" UNIQUE ("user_id", "setting_key");

-- ----------------------------
-- Primary Key structure for table user_additional_settings
-- ----------------------------
ALTER TABLE "public"."user_additional_settings" ADD CONSTRAINT "user_additional_settings_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table user_linked_assistants
-- ----------------------------
CREATE INDEX "idx_user_linked_assistants_assistant_id" ON "public"."user_linked_assistants" USING btree (
  "assistant_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);
CREATE INDEX "idx_user_linked_assistants_user_id" ON "public"."user_linked_assistants" USING btree (
  "user_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Uniques structure for table user_linked_assistants
-- ----------------------------
ALTER TABLE "public"."user_linked_assistants" ADD CONSTRAINT "user_linked_assistants_user_id_assistant_id_key" UNIQUE ("user_id", "assistant_id");

-- ----------------------------
-- Primary Key structure for table user_linked_assistants
-- ----------------------------
ALTER TABLE "public"."user_linked_assistants" ADD CONSTRAINT "user_linked_assistants_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table user_notification_preferences
-- ----------------------------
CREATE INDEX "idx_notification_pref_user_id" ON "public"."user_notification_preferences" USING btree (
  "user_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Triggers structure for table user_notification_preferences
-- ----------------------------
CREATE TRIGGER "update_notification_pref_updated_at" BEFORE UPDATE ON "public"."user_notification_preferences"
FOR EACH ROW
EXECUTE PROCEDURE "public"."update_updated_at_column"();

-- ----------------------------
-- Uniques structure for table user_notification_preferences
-- ----------------------------
ALTER TABLE "public"."user_notification_preferences" ADD CONSTRAINT "user_notification_preferences_user_id_preference_key_key" UNIQUE ("user_id", "preference_key");

-- ----------------------------
-- Primary Key structure for table user_notification_preferences
-- ----------------------------
ALTER TABLE "public"."user_notification_preferences" ADD CONSTRAINT "user_notification_preferences_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table user_profile_metadata
-- ----------------------------
CREATE INDEX "idx_profile_metadata_user_id" ON "public"."user_profile_metadata" USING btree (
  "user_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Triggers structure for table user_profile_metadata
-- ----------------------------
CREATE TRIGGER "update_profile_metadata_updated_at" BEFORE UPDATE ON "public"."user_profile_metadata"
FOR EACH ROW
EXECUTE PROCEDURE "public"."update_updated_at_column"();

-- ----------------------------
-- Uniques structure for table user_profile_metadata
-- ----------------------------
ALTER TABLE "public"."user_profile_metadata" ADD CONSTRAINT "user_profile_metadata_user_id_metadata_key_key" UNIQUE ("user_id", "metadata_key");

-- ----------------------------
-- Primary Key structure for table user_profile_metadata
-- ----------------------------
ALTER TABLE "public"."user_profile_metadata" ADD CONSTRAINT "user_profile_metadata_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table user_security_settings
-- ----------------------------
CREATE INDEX "idx_security_setting_user_id" ON "public"."user_security_settings" USING btree (
  "user_id" "pg_catalog"."int4_ops" ASC NULLS LAST
);

-- ----------------------------
-- Triggers structure for table user_security_settings
-- ----------------------------
CREATE TRIGGER "update_security_setting_updated_at" BEFORE UPDATE ON "public"."user_security_settings"
FOR EACH ROW
EXECUTE PROCEDURE "public"."update_updated_at_column"();

-- ----------------------------
-- Uniques structure for table user_security_settings
-- ----------------------------
ALTER TABLE "public"."user_security_settings" ADD CONSTRAINT "user_security_settings_user_id_setting_key_key" UNIQUE ("user_id", "setting_key");

-- ----------------------------
-- Primary Key structure for table user_security_settings
-- ----------------------------
ALTER TABLE "public"."user_security_settings" ADD CONSTRAINT "user_security_settings_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Indexes structure for table users
-- ----------------------------
CREATE INDEX "idx_email" ON "public"."users" USING btree (
  "email" COLLATE "pg_catalog"."default" "pg_catalog"."text_ops" ASC NULLS LAST
);
CREATE INDEX "idx_users_additional_settings" ON "public"."users" USING gin (
  "additional_settings" "pg_catalog"."jsonb_ops"
);
CREATE INDEX "idx_users_notification_preferences" ON "public"."users" USING gin (
  "notification_preferences" "pg_catalog"."jsonb_ops"
);
CREATE INDEX "idx_users_profile_metadata" ON "public"."users" USING gin (
  "profile_metadata" "pg_catalog"."jsonb_ops"
);
CREATE INDEX "idx_users_security_settings" ON "public"."users" USING gin (
  "security_settings" "pg_catalog"."jsonb_ops"
);

-- ----------------------------
-- Triggers structure for table users
-- ----------------------------
CREATE TRIGGER "update_users_updated_at" BEFORE UPDATE ON "public"."users"
FOR EACH ROW
EXECUTE PROCEDURE "public"."update_updated_at_column"();

-- ----------------------------
-- Uniques structure for table users
-- ----------------------------
ALTER TABLE "public"."users" ADD CONSTRAINT "users_email_key" UNIQUE ("email");

-- ----------------------------
-- Primary Key structure for table users
-- ----------------------------
ALTER TABLE "public"."users" ADD CONSTRAINT "users_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Triggers structure for table voice_assistants
-- ----------------------------
CREATE TRIGGER "trigger_update_voice_assistants_updated_at" BEFORE UPDATE ON "public"."voice_assistants"
FOR EACH ROW
EXECUTE PROCEDURE "public"."update_voice_assistants_updated_at"();

-- ----------------------------
-- Uniques structure for table voice_assistants
-- ----------------------------
ALTER TABLE "public"."voice_assistants" ADD CONSTRAINT "voice_assistants_name_key" UNIQUE ("name");

-- ----------------------------
-- Primary Key structure for table voice_assistants
-- ----------------------------
ALTER TABLE "public"."voice_assistants" ADD CONSTRAINT "voice_assistants_pkey" PRIMARY KEY ("id");

-- ----------------------------
-- Foreign Keys structure for table chatbot_messages
-- ----------------------------
ALTER TABLE "public"."chatbot_messages" ADD CONSTRAINT "chatbot_messages_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table devices
-- ----------------------------
ALTER TABLE "public"."devices" ADD CONSTRAINT "devices_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "public"."devices" ADD CONSTRAINT "fk_device_room" FOREIGN KEY ("room_id") REFERENCES "public"."rooms" ("id") ON DELETE SET NULL ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table energy_consumption
-- ----------------------------
ALTER TABLE "public"."energy_consumption" ADD CONSTRAINT "energy_consumption_device_id_fkey" FOREIGN KEY ("device_id") REFERENCES "public"."devices" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table home_invitations
-- ----------------------------
ALTER TABLE "public"."home_invitations" ADD CONSTRAINT "home_invitations_created_by_fkey" FOREIGN KEY ("created_by") REFERENCES "public"."users" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "public"."home_invitations" ADD CONSTRAINT "home_invitations_home_id_fkey" FOREIGN KEY ("home_id") REFERENCES "public"."homes" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table home_members
-- ----------------------------
ALTER TABLE "public"."home_members" ADD CONSTRAINT "home_members_added_by_fkey" FOREIGN KEY ("added_by") REFERENCES "public"."users" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "public"."home_members" ADD CONSTRAINT "home_members_home_id_fkey" FOREIGN KEY ("home_id") REFERENCES "public"."homes" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "public"."home_members" ADD CONSTRAINT "home_members_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table homes
-- ----------------------------
ALTER TABLE "public"."homes" ADD CONSTRAINT "homes_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table linked_accounts
-- ----------------------------
ALTER TABLE "public"."linked_accounts" ADD CONSTRAINT "linked_accounts_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table notifications
-- ----------------------------
ALTER TABLE "public"."notifications" ADD CONSTRAINT "notifications_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table password_reset_otp
-- ----------------------------
ALTER TABLE "public"."password_reset_otp" ADD CONSTRAINT "password_reset_otp_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table rooms
-- ----------------------------
ALTER TABLE "public"."rooms" ADD CONSTRAINT "fk_room_home" FOREIGN KEY ("home_id") REFERENCES "public"."homes" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "public"."rooms" ADD CONSTRAINT "rooms_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table scene_conditions
-- ----------------------------
ALTER TABLE "public"."scene_conditions" ADD CONSTRAINT "scene_conditions_device_id_fkey" FOREIGN KEY ("device_id") REFERENCES "public"."devices" ("id") ON DELETE SET NULL ON UPDATE NO ACTION;
ALTER TABLE "public"."scene_conditions" ADD CONSTRAINT "scene_conditions_scene_id_fkey" FOREIGN KEY ("scene_id") REFERENCES "public"."smart_scenes" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table scene_execution_logs
-- ----------------------------
ALTER TABLE "public"."scene_execution_logs" ADD CONSTRAINT "scene_execution_logs_scene_id_fkey" FOREIGN KEY ("scene_id") REFERENCES "public"."smart_scenes" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "public"."scene_execution_logs" ADD CONSTRAINT "scene_execution_logs_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table scene_tasks
-- ----------------------------
ALTER TABLE "public"."scene_tasks" ADD CONSTRAINT "scene_tasks_device_id_fkey" FOREIGN KEY ("device_id") REFERENCES "public"."devices" ("id") ON DELETE SET NULL ON UPDATE NO ACTION;
ALTER TABLE "public"."scene_tasks" ADD CONSTRAINT "scene_tasks_scene_id_fkey" FOREIGN KEY ("scene_id") REFERENCES "public"."smart_scenes" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "public"."scene_tasks" ADD CONSTRAINT "scene_tasks_scene_id_target_fkey" FOREIGN KEY ("scene_id_target") REFERENCES "public"."smart_scenes" ("id") ON DELETE SET NULL ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table smart_scenes
-- ----------------------------
ALTER TABLE "public"."smart_scenes" ADD CONSTRAINT "smart_scenes_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table user_actions
-- ----------------------------
ALTER TABLE "public"."user_actions" ADD CONSTRAINT "user_actions_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table user_additional_settings
-- ----------------------------
ALTER TABLE "public"."user_additional_settings" ADD CONSTRAINT "user_additional_settings_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table user_linked_assistants
-- ----------------------------
ALTER TABLE "public"."user_linked_assistants" ADD CONSTRAINT "user_linked_assistants_assistant_id_fkey" FOREIGN KEY ("assistant_id") REFERENCES "public"."voice_assistants" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
ALTER TABLE "public"."user_linked_assistants" ADD CONSTRAINT "user_linked_assistants_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table user_notification_preferences
-- ----------------------------
ALTER TABLE "public"."user_notification_preferences" ADD CONSTRAINT "user_notification_preferences_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table user_profile_metadata
-- ----------------------------
ALTER TABLE "public"."user_profile_metadata" ADD CONSTRAINT "user_profile_metadata_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;

-- ----------------------------
-- Foreign Keys structure for table user_security_settings
-- ----------------------------
ALTER TABLE "public"."user_security_settings" ADD CONSTRAINT "user_security_settings_user_id_fkey" FOREIGN KEY ("user_id") REFERENCES "public"."users" ("id") ON DELETE CASCADE ON UPDATE NO ACTION;
