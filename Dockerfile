FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

RUN apk add --no-cache curl && \
    curl -L https://jdbc.postgresql.org/download/postgresql-42.7.3.jar -o postgresql.jar

COPY src ./src
COPY index.html ./index.html

RUN find src -name "*.java" > sources.txt \
    && javac -cp postgresql.jar -d out @sources.txt \
    && rm sources.txt

EXPOSE 8080

CMD ["java", "-cp", "out:postgresql.jar", "com.portfolio.PortfolioServer"]
