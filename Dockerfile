FROM jolielang/jolie:1.11.2-dev AS maven_build
WORKDIR /slicer
COPY . .
RUN mvn -B package

FROM jolielang/jolie:1.11.2 AS build
WORKDIR /slicer
COPY . .
COPY --from=maven_build /slicer/lib lib
WORKDIR /app

ENTRYPOINT ["/slicer/launcher.ol"]
