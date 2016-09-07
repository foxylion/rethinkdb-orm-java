SHELL:=/bin/bash

build:
	./gradlew build

travis-deploy:
	./gradlew upload closeRepository
