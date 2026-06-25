FROM maven:3.8-openjdk-11

WORKDIR /app

# Копируем зависимости для кеширования
COPY pom.xml .
RUN mvn dependency:go-offline

# Копируем исходники
COPY src ./src

# Запускаем тесты (параметры передаются через CMD)
CMD ["mvn", "clean", "test"]