FROM ubuntu:20.04
LABEL maintainer="info@openvidu.io"

# Update and install dependencies
RUN apt-get update && apt-get -y upgrade && \
	apt-get install -y gnupg2 && \
	rm -rf /var/lib/apt/lists/*

# Install Kurento Media Server (KMS)
RUN echo "deb [arch=amd64] http://ubuntu.openvidu.io/7.0.1 focal main" | tee /etc/apt/sources.list.d/kurento.list && \
	apt-key adv --keyserver keyserver.ubuntu.com --recv-keys 234821A61B67740F89BFD669FC8A16625AFA7A83 && \
	apt-get update && \
	apt-get -y install kurento-media-server && \
	rm -rf /var/lib/apt/lists/*

# Install Java, supervisor and netstat
RUN apt-get update && apt-get install -y \
	openjdk-11-jre \
	supervisor && \
	rm -rf /var/lib/apt/lists/*

# Configure supervisor
RUN mkdir -p /var/log/supervisor
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf

RUN mkdir -p /opt/openvidu

# Copy OpenVidu Server
COPY openvidu-server-*.jar /opt/openvidu

# Copy KMS entrypoint
COPY kms.sh /kms.sh

EXPOSE 8888
EXPOSE 9091
EXPOSE 4443

WORKDIR /opt/openvidu

# Exec supervisord
CMD ["/usr/bin/supervisord"]
