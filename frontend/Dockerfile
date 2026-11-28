# Build app. The output is static and architecture independent, so always build on the native build platform
# instead of emulating the target platform with QEMU (which is very slow and gets multi-arch builds cancelled).
FROM --platform=$BUILDPLATFORM node:26.13.1-slim@sha256:abe7e5a82b61a8ef67b315b85497c9e65173387ccf2f4a029248184b8e020be5 AS builder
WORKDIR /app
COPY . .
# Node 25+ images no longer ship Yarn, so run the release that .yarnrc.yml pins (yarnPath, kept up to date by Renovate)
RUN YARN="node $(sed -n 's/^yarnPath: *//p' .yarnrc.yml)" \
    && $YARN install --immutable \
    && $YARN run build

# Serve the static assets with the official nginx image (Renovate bumps it with each nginx release) as the
# unprivileged user nginx (101). nginx-main.conf drops the `user` directive and moves the PID file and temporary files
# to /tmp, so it needs no root and listens on 8080.
FROM nginx:1.31.10-alpine@sha256:7fc0fb4af7bf006aeb31aea88069976060f8f0a0a86b667126742cc17f4e31ae AS production
# Alpine fixes published since the base image was built (images are only built on release)
RUN apk upgrade --no-cache
COPY nginx-main.conf /etc/nginx/nginx.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY security-headers.conf /etc/nginx/snippets/security-headers.conf
COPY --from=builder /app/build /usr/share/nginx/html
USER 101
EXPOSE 8080
CMD ["nginx", "-g", "daemon off;"]
