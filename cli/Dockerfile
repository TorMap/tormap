# The tormap CLI as a Docker image: docker run --rm tormap/cli relays --country DE --limit 10
#
# Packages the native binaries built by .github/workflows/cli-native.yml, so the image needs no JDK and no build
# runs under emulation. Expects build/docker/linux-amd64/tormap and build/docker/linux-arm64/tormap in the context.
# Locally: ./gradlew nativeCompile && mkdir -p build/docker/linux-amd64 && cp build/native/tormap build/docker/linux-amd64/
#          docker build --platform linux/amd64 -t tormap/cli:local .

# glibc only, no shell, runs as an unprivileged user. The binary brings its own CA certificates and zlib.
FROM gcr.io/distroless/base-nossl-debian13:nonroot@sha256:89815d620ac01d8de13cdaf04d19ed874447ca2296d4985b14b818f6e5e47854

ARG TARGETOS
ARG TARGETARCH
COPY --chmod=0755 build/docker/${TARGETOS}-${TARGETARCH}/tormap /usr/local/bin/tormap

# The cache lives in the container and is lost with --rm; mount a volume here to keep it between runs
ENV XDG_CACHE_HOME=/home/nonroot/.cache

ENTRYPOINT ["/usr/local/bin/tormap"]
CMD ["--help"]
