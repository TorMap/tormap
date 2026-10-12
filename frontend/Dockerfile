# Build app. The output is static and architecture independent, so always build on the native build platform
# instead of emulating the target platform with QEMU (which is very slow and gets multi-arch builds cancelled).
FROM --platform=$BUILDPLATFORM node:24-slim@sha256:ba849c60be29959425b8734d57b8b4b7d56f98edd9504c9af091d5281095a71e AS builder
WORKDIR /app
COPY . .
RUN yarn install --immutable
RUN yarn run build

# Bundle static assets with nginx. The unprivileged image runs as a non-root user (101) and therefore listens on 8080.
FROM nginxinc/nginx-unprivileged:1.31.3-alpine@sha256:f972e5322b9797dc2a6b830030094426437b1ae7032e4644496395336ac6fdac AS production
# Copy built assets from `builder` image
COPY --from=builder /app/build /usr/share/nginx/html
# Add your nginx.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
