IMAGE_NAME = statusteam/linux-desktop-ubuntu:16.04
QT_ARCHIVE = qt-opensource-linux-x64-5.9.1.run

build: $(QT_ARCHIVE)
	docker build -t $(IMAGE_NAME) .

$(QT_ARCHIVE):
	wget https://download.qt.io/archive/qt/5.9/5.9.1/$(QT_ARCHIVE)
	echo "b8dd904894ac6b09076b5f61d8b9d71a  $(QT_ARCHIVE)" > qt.md5
	md5sum --check qt.md5

push: build
	docker push $(IMAGE_NAME)
