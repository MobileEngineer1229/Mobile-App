FROM oven/bun:1 AS base
WORKDIR /app

COPY package.json ./
COPY shared/ ./shared/
COPY services/rank-server/package.json ./services/rank-server/
RUN bun install --frozen-lockfile

COPY services/rank-server/ ./services/rank-server/

WORKDIR /app/services/rank-server
EXPOSE 8002
CMD ["bun", "src/index.ts"]
