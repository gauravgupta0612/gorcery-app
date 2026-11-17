# Build: docker build -t gola-backend -f backend/Dockerfile .   (run from repository root)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY database ./database
COPY backend/pom.xml ./backend/pom.xml
RUN cd backend && mvn -q -B dependency:go-offline
COPY backend/src ./backend/src
RUN cd backend && mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd -r -u 1001 gola && mkdir -p /app/uploads && chown gola /app/uploads
COPY --from=build /src/backend/target/gola-grocery-backend-1.0.0.jar app.jar
COPY --from=build /src/database/seed-images /app/seed-images
ENV STORAGE_DIR=/app/uploads SEED_IMAGES_DIR=/app/seed-images
USER gola
EXPOSE 8095
# Tuned for small (512 MB) containers such as Render free
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65", "-XX:+UseSerialGC", "-Xss512k", "-XX:TieredStopAtLevel=1", "-jar", "/app/app.jar"]
