# syntax=docker/dockerfile:1

FROM node:24-alpine AS builder
WORKDIR /app

COPY package*.json ./
RUN --mount=type=cache,target=/root/.npm npm ci

COPY . .
RUN npx nuxt build

FROM node:24-alpine AS release
WORKDIR /app
ENV NODE_ENV=production

LABEL org.opencontainers.image.title="centsible-ui" \
      org.opencontainers.image.description="Centsible Nuxt 4 frontend (SSR)"

USER node
COPY --chown=node:node --from=builder /app/.output ./.output

EXPOSE 3000
CMD ["node", ".output/server/index.mjs"]
