COMPOSE ?= $(shell if command -v podman-compose >/dev/null 2>&1; then echo podman-compose; elif command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then echo "docker compose"; elif command -v docker-compose >/dev/null 2>&1; then echo docker-compose; else echo podman compose; fi)
JAVA_HOME ?= $(shell if [ -d "/home/tienvm/.local/jdks/jdk-21" ]; then echo "/home/tienvm/.local/jdks/jdk-21"; elif [ -d "$$JAVA_HOME" ]; then echo "$$JAVA_HOME"; else echo "/usr/lib/jvm/java"; fi)
export JAVA_HOME
export PATH := $(JAVA_HOME)/bin:$(PATH)

.PHONY: infra-up infra-down backend-build backend-run frontend clean

infra-up:
	$(COMPOSE) up -d

infra-down:
	$(COMPOSE) down

backend-build:
	cd be && mvn clean package -DskipTests

backend-run:
	cd be && mvn wildfly:run

frontend:
	python3 -m http.server 3000 -d fe

clean:
	$(COMPOSE) down -v && cd be && mvn clean
