# FIXME: tutum/debian will be deprecated soon: https://github.com/tutumcloud/tutum-debian/blob/master/README.md
#FROM tutum/debian:jessie
FROM tutum/debian:jessie

ENV GOPATH "/go"
WORKDIR /go
COPY setup-orbs.sh /go/setup-orbs.sh
COPY stop-orbs.sh /opt/orbs/stop-orbs.sh

RUN apt-get update && apt-get install -y wget build-essential git musl-dev gcc && \
    wget https://dl.google.com/go/go1.10.3.linux-amd64.tar.gz && \
    tar -xvf go1.10.3.linux-amd64.tar.gz && \
    mv go /usr/local && \
    ln -s /usr/local/go/bin/go /usr/local/bin/go && \
    mkdir -p /opt/go && \
    mkdir -p /opt/orbs && \
    apt-get remove -y --purge

RUN ./setup-orbs.sh

RUN rm /etc/apt/apt.conf.d/docker-clean && apt-get update && apt-get install -y git bzip2 curl faketime iproute iptables iputils-ping libzip2 logrotate man man-db net-tools ntpdate psmisc python rsyslog sudo sysvinit sysvinit-core sysvinit-utils tar unzip vim wget && apt-get remove -y --purge --auto-remove systemd



