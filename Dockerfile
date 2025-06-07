FROM debian:bookworm-slim
WORKDIR /usr/local

# Install dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    bash \
    git \
    openjdk-17-jdk \
    wget \
    unzip \
    zip

# Setup Android SDK environment variables
ENV ANDROID_SDK_ROOT=/usr/local/android-sdk
ENV PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools

# Download command line tools
RUN mkdir -p $ANDROID_SDK_ROOT/cmdline-tools && \
    wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip && \
    unzip cmdline-tools.zip -d $ANDROID_SDK_ROOT/cmdline-tools && \
    mv $ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools $ANDROID_SDK_ROOT/cmdline-tools/latest && \
    rm cmdline-tools.zip

# Accept licenses and install platform-tools
RUN yes | sdkmanager --sdk_root=${ANDROID_SDK_ROOT} --licenses && \
    sdkmanager --sdk_root=${ANDROID_SDK_ROOT} "platform-tools"

WORKDIR /usr/local/stario

# Clone the repo
RUN git clone https://github.com/albu-razvan/stario .

# Setup local.properties file pointing to Android SDK
RUN echo "sdk.dir=$ANDROID_SDK_ROOT" > local.properties

# Make build scripts executable
RUN chmod +x ./build_all.sh && \
    chmod +x ./gradlew

CMD ["/bin/bash"]