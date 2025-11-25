# just displays the recipes
_default:
    @just --list

# cleans all
clean:
    ./gradlew clean

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

run-example:
    ./gradlew :example:shadowJar
    clear
    java -jar example/build/libs/example-1.0.0-SNAPSHOT-all.jar