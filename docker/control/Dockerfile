FROM ubuntu:16.04
MAINTAINER jake@apache.org

ENV LEIN_ROOT true
ENV GOPATH "/opt/go"

#
# Jepsen dependencies
#
RUN apt-get -y -q update && \
    apt-get -y -q install software-properties-common && \
    add-apt-repository ppa:openjdk-r/ppa && \
    apt-get -y -q update && \
    apt-get install -qqy \
    openjdk-8-jdk \
    curl \
    libjna-java \
    git \
    gnuplot \
    wget \
    vim # not required by jepsen itself, just for ease of use

RUN wget https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein && \
    mv lein /usr/bin && \
    chmod +x /usr/bin/lein && \
    lein self-install

# You need to locate jepsen in this directory (up.sh does that automatically)
ADD jepsen /jepsen
RUN cd /jepsen/jepsen && lein install

RUN apt-get update && apt-get install -y wget build-essential git && \
    wget https://dl.google.com/go/go1.10.3.linux-amd64.tar.gz && \
    tar -xvf go1.10.3.linux-amd64.tar.gz && \
    mv go /usr/local && \
    ln -s /usr/local/go/bin/go /usr/local/bin/go && \
    mkdir -p /opt/go && \
    mkdir -p /opt/orbs && \
    apt-get remove -y --purge

RUN wget -qO- https://raw.githubusercontent.com/orbs-network/orbs-contract-sdk/master/install.sh | bash

ADD ./bashrc /root/.bashrc
ADD ./init.sh /init.sh
RUN chmod +x /init.sh

CMD /init.sh
