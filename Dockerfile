# AirSarFusion backend image
# Java + PostgreSQL JDBC driver

FROM eclipse-temurin:21-jdk

WORKDIR /app

# Copy backend source
COPY backend/src ./backend/src

# Copy backend data
COPY backend/data ./backend/data

# Copy PostgreSQL JDBC driver
COPY backend/lib ./backend/lib

# Copy frontend
COPY frontend ./frontend

WORKDIR /app/backend

# Compile Java source with PostgreSQL JDBC driver
RUN mkdir -p bin && \
    javac -cp "lib/postgresql-42.7.13.jar" \
    -d bin \
    src/ais/*.java src/server/*.java

# Railway provides PORT at runtime.
EXPOSE 8080

# Run Java with PostgreSQL JDBC driver available
CMD ["java", "-cp", "bin:lib/postgresql-42.7.13.jar", "server.ApiServer"]