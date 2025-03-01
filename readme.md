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
