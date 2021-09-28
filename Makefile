SOURCES = $(shell find src -name \*.clj)

.PHONY: kkbibapi
kkbibapi: target/default+uberjar/kkbibapi

target/default+uberjar/kkbibapi: $(SOURCES)
	lein native-image

docker:
	docker run -ti --rm -u $(shell id -u):$(shell id -g) -v $(shell pwd):/app -w /app runejuhl/lein-graal make
