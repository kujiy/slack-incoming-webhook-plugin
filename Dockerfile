FROM openjdk:11

WORKDIR /app

COPY ./ .

# Build plugin
RUN ./gradlew
