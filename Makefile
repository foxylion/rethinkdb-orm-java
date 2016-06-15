build:
	./gradlew build

travis-deploy:
	./gradlew upload closeRepository
