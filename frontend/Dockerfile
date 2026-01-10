# Build app
FROM node:22-alpine@sha256:0340fa682d72068edf603c305bfbc10e23219fb0e40df58d9ea4d6f33a9798bf as builder
WORKDIR /app
COPY . .
RUN yarn install --immutable
RUN yarn run build

# Bundle static assets with nginx
FROM nginx:1.29.4-alpine@sha256:ce611bb6e76a1da3576e3875f78acb1064a4624dbf45abcbf14fd152cfb7a943 as production
ENV NODE_ENV production
# Copy built assets from `builder` image
COPY --from=builder /app/build /usr/share/nginx/html
# Add your nginx.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
