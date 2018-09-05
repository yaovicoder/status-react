IMAGE_NAME = 'statusteam/linux-desktop-ubuntu:16.04'

build:
	docker build . -t $(IMAGE_NAME)

push: build
	docker push $(IMAGE_NAME)
