# Install OpenCV

For example on ubuntu:

```shell
sudo apt install libopencv*
```

Then add the library to your java lib path:

```shell
java ... -Djava.library.path=/usr/lib/jni ...
```


You may need to adapt the Maven lib version to match the version of opencv installed on your system.

# Tensorflow

## NVIDIA 

Install nvidia toolkit for tensorflow GPU support

```shell
sudo apt install nvidia-cuda-toolkit nvidia-driver-535
```

## Intel

```shell
sudo apt install xserver-xorg-video-intel
```

https://www.kaggle.com/models/tensorflow/faster-rcnn-inception-resnet-v2/tensorFlow2/1024x1024/1

```shell
curl -L -o ~/Downloads/model.tar.gz https://www.kaggle.com/api/v1/models/tensorflow/faster-rcnn-inception-resnet-v2/tensorFlow2/1024x1024/1/download
tar -C ~/models/ -xzvf ~/Downloads/model.tar.gz
```