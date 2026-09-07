# ---- Build stage ----
FROM eclipse-temurin:23-jdk AS build
WORKDIR /app

# Copy only the files needed to resolve dependencies first...
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon
# ...so Docker caches this layer and skips re-downloading every dependency
# every time you change a .java file. It only re-runs if build.gradle itself changes.

# NOW copy the actual source and build
COPY src src
RUN ./gradlew build -x test --no-daemon
# -x test: skip tests during the image build. They already ran locally/in CI -
# re-running here just slows down every single build for no extra safety.

# ---- Runtime stage ----
FROM eclipse-temurin:23-jre AS runtime
WORKDIR /app

# Copy ONLY the built jar from the build stage - none of the JDK, Gradle, or source comes along
COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]