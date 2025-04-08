En-vrac useful infos...

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

# Jython

```shell
sudo apt install jython -y
curl -O http://peak.telecommunity.com/dist/ez_setup.py
jython ez_setup.py
```

```shell
sudo /usr/bin/jython2.5.2b1/bin/pip install bottle
```

# Raspberry Pi AI HAT+

```shell
sudo apt install hailo-all
hailortcli fw-control identify # check
```

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

# Goodies

## Find outdated dependencies

Use a custom config in settings.xml for system-wide effect:

```xml

<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
    <profiles>
        <profile>
            <id>default-values</id>
            <properties>
                <maven.versions.ignore>(?i).*[\.-]?(alpha|beta|rc|preview|SNAPSHOT|M\d+)[\.-]?.*</maven.versions.ignore>
                <processDependencyManagementTransitive>false</processDependencyManagementTransitive>
            </properties>
        </profile>
    </profiles>

    <activeProfiles>
        <activeProfile>default-values</activeProfile>
        ...
    </activeProfiles>
</settings>
```

Then simply invoke:

```shell
mvn versions:display-dependency-updates versions:display-property-updates versions:display-plugin-updates
```

... to get a clear report of updatable dependencies, ignoring snapshots, beta, rc, etc...