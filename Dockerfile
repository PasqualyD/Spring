# Step 1: Use a Java 21 runtime (matching your current setup)
FROM eclipse-temurin:21-jdk-jammy

# Step 2: Set the working directory inside the container
WORKDIR /app

# Step 3: Copy the maven wrapper and pom.xml first (optimizes build speed)
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline

# Step 4: Copy your source code and build the app
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Step 5: Run the generated JAR file
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "target/demo-0.0.1-SNAPSHOT.jar"]

#change