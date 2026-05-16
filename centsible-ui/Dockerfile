FROM node:24-alpine AS base
WORKDIR /app

FROM base AS dev
ENV NODE_ENV=development
EXPOSE 3000
CMD ["sh", "-c", "npm install && npx nuxt dev --host 0.0.0.0"]

FROM base AS builder
COPY package*.json ./
RUN npm ci
COPY . .
RUN npx nuxt build

FROM base AS release
ENV NODE_ENV=production
USER node
COPY --chown=node:node --from=builder /app/.output ./.output
EXPOSE 3000
CMD ["node", ".output/server/index.mjs"]