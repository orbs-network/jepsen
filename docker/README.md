Dockerized Jepsen
=================

This docker image attempts to simplify the setup required by Jepsen.
It is intended to be used by a CI tool or anyone with docker who wants to try jepsen themselves.

It contains all the jepsen dependencies and code. It uses [Docker Compose](https://github.com/docker/compose) to spin up the five
containers used by Jepsen.  

To start run

````
    ./up.sh
    docker exec -it jepsen-control bash
````

During development, it's convenient to run with `--dev` option, which mounts `$JEPSEN_ROOT` dir as `/jepsen` on Jepsen control container.

Run `./up.sh --help` for more info.