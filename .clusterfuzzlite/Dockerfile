# Build image for the backend's Jazzer fuzz targets (backend/src/fuzz), see README.md in this directory.
# Images are pinned by digest and updated by Renovate.
FROM gcr.io/oss-fuzz-base/base-builder-jvm@sha256:f78b70af260b975f3baa39cdd1d76a54305fb4c1600a630701e6f23b9478e355
# The base image only has Java 17, the backend needs Java 25 to build and run
COPY --from=eclipse-temurin:25-jdk@sha256:ce2ef9d2d67a68de5e196d4de9d743327c44a10b57fe02ec39f572c652cacfd3 /opt/java/openjdk $SRC/jdk
COPY . $SRC/tormap
WORKDIR $SRC/tormap
COPY .clusterfuzzlite/build.sh $SRC/
