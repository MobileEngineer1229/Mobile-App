FROM oven/bun:1 AS base
WORKDIR /app

COPY package.json ./
COPY shared/ ./shared/
COPY services/battle-server/package.json ./services/battle-server/
RUN bun install --frozen-lockfile

COPY services/battle-server/ ./services/battle-server/

WORKDIR /app/services/battle-server
EXPOSE 5000/udp
CMD ["bun", "src/index.ts"]
