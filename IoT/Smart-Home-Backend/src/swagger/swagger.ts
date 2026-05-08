import swaggerJsdoc from 'swagger-jsdoc';
import { env } from '../config/env';

const options: swaggerJsdoc.Options = {
  definition: {
    openapi: '3.0.0',
    info: {
      title: 'Smart Home Backend API',
      version: '1.0.0',
      description: 'Express + PostgreSQL + Node.js + TypeScript backend for IoT smarthome system',
      contact: {
        name: 'API Support',
      },
    },
    servers: [
      {
        url: env.enableHttps
          ? `https://172.86.88.76:${env.httpsPort || env.port}/api/${env.apiVersion}`
          : `http://172.86.88.76:${env.port}/api/${env.apiVersion}`,
        description: 'Network server (default)',
      },
      {
        url: env.enableHttps
          ? `https://localhost:${env.httpsPort || env.port}/api/${env.apiVersion}`
          : `http://localhost:${env.port}/api/${env.apiVersion}`,
        description: 'Local server',
      },
    ],
    components: {
      securitySchemes: {
        bearerAuth: {
          type: 'http',
          scheme: 'bearer',
          bearerFormat: 'JWT',
        },
      },
      schemas: {
        UserResponse: {
          type: 'object',
          properties: {
            id: {
              type: 'integer',
              example: 1,
            },
            email: {
              type: 'string',
              format: 'email',
              example: 'user@example.com',
            },
            firstName: {
              type: 'string',
              example: 'John',
            },
            lastName: {
              type: 'string',
              example: 'Doe',
            },
            phone: {
              type: 'string',
              nullable: true,
              example: '+1234567890',
            },
            createdAt: {
              type: 'string',
              format: 'date-time',
            },
            updatedAt: {
              type: 'string',
              format: 'date-time',
            },
          },
        },
        LoginResponse: {
          type: 'object',
          properties: {
            user: {
              $ref: '#/components/schemas/UserResponse',
            },
            token: {
              type: 'string',
              example: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...',
            },
          },
        },
        DeviceResponse: {
          type: 'object',
          properties: {
            id: {
              type: 'integer',
              example: 1,
            },
            userId: {
              type: 'integer',
              example: 1,
            },
            name: {
              type: 'string',
              example: 'Living Room Sensor',
            },
            type: {
              type: 'string',
              enum: ['lamp', 'camera', 'electronics'],
              example: 'lamp',
            },
            status: {
              type: 'string',
              enum: ['online', 'offline', 'unknown'],
              example: 'online',
            },
            macAddress: {
              type: 'string',
              example: '00:11:22:33:44:55',
            },
            ipAddress: {
              type: 'string',
              nullable: true,
              example: '192.168.1.100',
            },
            lastSeen: {
              type: 'string',
              format: 'date-time',
              nullable: true,
            },
            metadata: {
              type: 'object',
              additionalProperties: true,
              nullable: true,
            },
            createdAt: {
              type: 'string',
              format: 'date-time',
            },
            updatedAt: {
              type: 'string',
              format: 'date-time',
            },
          },
        },
        DeviceListResponse: {
          type: 'object',
          properties: {
            success: {
              type: 'boolean',
              example: true,
            },
            data: {
              type: 'array',
              items: {
                $ref: '#/components/schemas/DeviceResponse',
              },
            },
            meta: {
              type: 'object',
              properties: {
                pagination: {
                  type: 'object',
                  properties: {
                    page: {
                      type: 'integer',
                      example: 1,
                    },
                    limit: {
                      type: 'integer',
                      example: 10,
                    },
                    total: {
                      type: 'integer',
                      example: 25,
                    },
                    totalPages: {
                      type: 'integer',
                      example: 3,
                    },
                  },
                },
                timestamp: {
                  type: 'string',
                  format: 'date-time',
                },
              },
            },
          },
        },
        CreateDeviceInput: {
          type: 'object',
          required: ['name', 'type', 'macAddress'],
          properties: {
            name: {
              type: 'string',
              example: 'Living Room Sensor',
            },
            type: {
              type: 'string',
              enum: ['lamp', 'camera', 'electronics'],
              example: 'lamp',
            },
            macAddress: {
              type: 'string',
              example: '00:11:22:33:44:55',
            },
            ipAddress: {
              type: 'string',
              nullable: true,
              example: '192.168.1.100',
            },
            metadata: {
              type: 'object',
              additionalProperties: true,
              nullable: true,
            },
          },
        },
        UpdateDeviceInput: {
          type: 'object',
          properties: {
            name: {
              type: 'string',
              example: 'Living Room Sensor',
            },
            status: {
              type: 'string',
              enum: ['online', 'offline', 'unknown'],
              example: 'online',
            },
            ipAddress: {
              type: 'string',
              nullable: true,
              example: '192.168.1.100',
            },
            metadata: {
              type: 'object',
              additionalProperties: true,
              nullable: true,
            },
          },
        },
        RoomResponse: {
          type: 'object',
          properties: {
            id: {
              type: 'integer',
              example: 1,
            },
            userId: {
              type: 'integer',
              example: 1,
            },
            homeId: {
              type: 'integer',
              nullable: true,
              example: 1,
            },
            name: {
              type: 'string',
              example: 'Living Room',
            },
            createdAt: {
              type: 'string',
              format: 'date-time',
            },
            updatedAt: {
              type: 'string',
              format: 'date-time',
            },
          },
        },
        HomeResponse: {
          type: 'object',
          properties: {
            id: {
              type: 'integer',
              example: 1,
            },
            userId: {
              type: 'integer',
              example: 1,
            },
            name: {
              type: 'string',
              example: 'My Home',
            },
            address: {
              type: 'string',
              nullable: true,
              example: '701 7th Ave, New York, 10036, USA',
            },
            latitude: {
              type: 'number',
              nullable: true,
              example: 40.7128,
            },
            longitude: {
              type: 'number',
              nullable: true,
              example: -74.0060,
            },
            country: {
              type: 'string',
              nullable: true,
              example: 'United States',
            },
            isPrimary: {
              type: 'boolean',
              example: true,
            },
            createdAt: {
              type: 'string',
              format: 'date-time',
            },
            updatedAt: {
              type: 'string',
              format: 'date-time',
            },
          },
        },
        NotificationResponse: {
          type: 'object',
          properties: {
            id: {
              type: 'integer',
              example: 1,
            },
            userId: {
              type: 'integer',
              example: 1,
            },
            title: {
              type: 'string',
              example: 'Account Security Alert',
            },
            message: {
              type: 'string',
              example: 'We\'ve noticed some unusual activity...',
            },
            type: {
              type: 'string',
              enum: ['general', 'security', 'system', 'feature', 'reminder', 'alert', 'info'],
              example: 'security',
            },
            category: {
              type: 'string',
              enum: ['general', 'smart_home'],
              example: 'smart_home',
            },
            icon: {
              type: 'string',
              nullable: true,
              example: 'security',
            },
            isRead: {
              type: 'boolean',
              example: false,
            },
            createdAt: {
              type: 'string',
              format: 'date-time',
            },
          },
        },
        ErrorResponse: {
          type: 'object',
          properties: {
            success: {
              type: 'boolean',
              example: false,
            },
            error: {
              type: 'object',
              properties: {
                code: {
                  type: 'string',
                  example: 'VALIDATION_ERROR',
                },
                message: {
                  type: 'string',
                  example: 'Validation failed',
                },
              },
            },
            meta: {
              type: 'object',
              properties: {
                timestamp: {
                  type: 'string',
                  format: 'date-time',
                },
              },
            },
          },
        },
        Scene: {
          type: 'object',
          properties: {
            id: {
              type: 'integer',
              example: 1,
            },
            userId: {
              type: 'integer',
              example: 1,
            },
            homeId: {
              type: 'integer',
              nullable: true,
              example: 1,
            },
            name: {
              type: 'string',
              example: 'Welcome Home Automation',
            },
            type: {
              type: 'string',
              enum: ['automation', 'tap_to_run'],
              example: 'automation',
            },
            conditionLogic: {
              type: 'string',
              enum: ['any', 'all'],
              example: 'any',
            },
            icon: {
              type: 'string',
              nullable: true,
              example: 'ic_sun',
            },
            color: {
              type: 'string',
              nullable: true,
              example: '#405FF2',
            },
            isEnabled: {
              type: 'boolean',
              example: true,
            },
            conditions: {
              type: 'array',
              items: {
                $ref: '#/components/schemas/SceneCondition',
              },
            },
            tasks: {
              type: 'array',
              items: {
                $ref: '#/components/schemas/SceneTask',
              },
            },
            createdAt: {
              type: 'string',
              format: 'date-time',
            },
            updatedAt: {
              type: 'string',
              format: 'date-time',
            },
          },
        },
        SceneCondition: {
          type: 'object',
          properties: {
            id: {
              type: 'integer',
              example: 1,
            },
            type: {
              type: 'string',
              enum: [
                'temperature',
                'humidity',
                'weather',
                'sunrise_sunset',
                'wind_speed',
                'location',
                'location_arrive_at',
                'location_leave',
                'schedule_time',
                'time',
                'device_status',
                'arm_mode',
                'alarm',
                'tap_to_run',
              ],
              example: 'location_arrive_at',
            },
            operator: {
              type: 'string',
              nullable: true,
              example: 'arrive_at',
            },
            value: {
              type: 'number',
              nullable: true,
              example: 2145.0,
            },
            unit: {
              type: 'string',
              nullable: true,
              example: 'celsius',
            },
            location: {
              type: 'string',
              nullable: true,
              example: '701 7th Ave, New York, 10036, USA',
            },
            deviceId: {
              type: 'integer',
              nullable: true,
            },
            deviceStatus: {
              type: 'string',
              nullable: true,
            },
            armMode: {
              type: 'string',
              nullable: true,
            },
            metadata: {
              type: 'object',
              additionalProperties: true,
              nullable: true,
              example: {
                hour: 21,
                minute: 45,
                repeat: 'every_day',
              },
            },
          },
        },
        SceneTask: {
          type: 'object',
          properties: {
            id: {
              type: 'integer',
              example: 1,
            },
            type: {
              type: 'string',
              enum: [
                'control_device',
                'select_scene',
                'change_arm_mode',
                'send_notification',
                'delay',
              ],
              example: 'select_scene',
            },
            deviceId: {
              type: 'integer',
              nullable: true,
            },
            deviceName: {
              type: 'string',
              nullable: true,
              example: 'Living Room Light',
            },
            roomName: {
              type: 'string',
              nullable: true,
              example: 'Living Room',
            },
            function: {
              type: 'string',
              nullable: true,
              example: 'ON',
            },
            sceneIdTarget: {
              type: 'integer',
              nullable: true,
              example: 5,
            },
            sceneName: {
              type: 'string',
              nullable: true,
              example: 'Turn ON All the Lights',
            },
            armMode: {
              type: 'string',
              nullable: true,
            },
            notificationMessage: {
              type: 'string',
              nullable: true,
            },
            delaySeconds: {
              type: 'integer',
              nullable: true,
              example: 930,
            },
            orderIndex: {
              type: 'integer',
              example: 0,
            },
            metadata: {
              type: 'object',
              additionalProperties: true,
              nullable: true,
            },
          },
        },
      },
    },
    tags: [
      {
        name: 'Users',
        description: 'User authentication and profile management',
      },
      {
        name: 'Devices',
        description: 'Device management operations',
      },
      {
        name: 'Rooms',
        description: 'Room management operations',
      },
      {
        name: 'Homes',
        description: 'Home/location management operations',
      },
      {
        name: 'Notifications',
        description: 'Notification management operations',
      },
      {
        name: 'Chatbot',
        description: 'Chatbot/assistant operations',
      },
      {
        name: 'Voice Assistants',
        description: 'Voice assistant integration operations',
      },
      {
        name: 'Reports',
        description: 'Reports and analytics operations',
      },
      {
        name: 'Scenes',
        description: 'Smart scene and automation management operations',
      },
    ],
  },
  apis: ['./src/routes/*.ts'], // Path to the API routes
};

export const swaggerSpec = swaggerJsdoc(options);

