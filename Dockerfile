FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY src ./src
COPY index.html ./index.html

RUN find src -name "*.java" > sources.txt \
    && javac -d out @sources.txt \
    && rm sources.txt

EXPOSE 8080

CMD ["java", "-cp", "out", "com.portfolio.PortfolioServer"]
