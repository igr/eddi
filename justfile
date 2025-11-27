# just displays the recipes
_default:
    @just --list

# cleans all
clean:
    ./gradlew clean

# builds the project
build:
    ./gradlew build

# starts the infrastructure
infra-up:
    docker-compose -f docker-compose.yaml up -d

# brings down the infrastructure
infra-down:
    docker-compose -f docker-compose.yaml down

# resets the infrastructure
infra-reset:
    @just infra-down
    docker volume rm eddi_postgres_data
    @just infra-up

run:
    ./gradlew :example:classes
    clear
    java -cp $(./gradlew -q :example:printClasspath) dev.oblac.eddi.example.college.MainKt
