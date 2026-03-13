FROM node:22-alpine AS builder
WORKDIR /app

COPY demo-ui/package.json demo-ui/package-lock.json ./
RUN npm ci

COPY demo-ui/ ./
RUN npm run build

FROM node:22-alpine AS runner
WORKDIR /app

RUN addgroup -S fluxnova && adduser -S fluxnova -G fluxnova

COPY --from=builder /app/public ./public
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static

USER fluxnova

EXPOSE 3000
ENV PORT=3000
ENV HOSTNAME="0.0.0.0"

CMD ["node", "server.js"]
