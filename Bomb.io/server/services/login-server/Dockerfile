FROM oven/bun:1 AS base
WORKDIR /app

COPY package.json ./
COPY shared/ ./shared/
COPY services/login-server/package.json ./services/login-server/
RUN bun install --frozen-lockfile

COPY services/login-server/ ./services/login-server/

WORKDIR /app/services/login-server
EXPOSE 8000
CMD ["bun", "src/index.ts"]
