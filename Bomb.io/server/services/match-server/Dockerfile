FROM oven/bun:1 AS base
WORKDIR /app

COPY package.json ./
COPY shared/ ./shared/
COPY services/match-server/package.json ./services/match-server/
RUN bun install --frozen-lockfile

COPY services/match-server/ ./services/match-server/

WORKDIR /app/services/match-server
EXPOSE 8001
CMD ["bun", "src/index.ts"]
