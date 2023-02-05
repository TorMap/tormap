# syntax = docker/dockerfile:1.4.3

FROM node:16-alpine as builder
WORKDIR /app
COPY . .
RUN yarn install --immutable
RUN yarn run build


FROM cmdjulian/httpd:v1.36.0

COPY --link <<EOF /etc/httpd/httpd.conf
E404:index.html
EOF

COPY --link --from=builder /app/build/ /opt/httpd
