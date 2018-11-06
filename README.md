# Orbs Jepsen

[![CircleCI](https://circleci.com/gh/orbs-network/jepsen/tree/master.svg?style=svg)](https://circleci.com/gh/orbs-network/jepsen/tree/master)

Orbs is an Hybrid Blockchain Solution and by nature is decentralized and eventually consistent. The aim of this repository is to test the Go language reference implementation of the Orbs protocol, and make sure it can withstand network partitions and unavailability of peer nodes in is blockchain network and still be able to have consistent results from it's commited ledger.

After all, a blockchain is a distributed database, so one can expect it to have a consistent result from all nodes in the network even with network failures.

This project uses the Jepsen framework developed by [Kyle Kingsbury](https://github.com/aphyr). You can have a look at the official GitHub repository of the project [here](https://github.com/jepsen-io/jepsen) 

## Usage

In order to run the tests you will need to install Docker. We use this to be able to bring up
a few Orbs Nodes as in a working virtual blockchain with a few nodes that we can torture.

The first step of running the project is the setup phase:

    $ sh ./ops/setup-jepsen.sh

This will create the relevant Docker images we need to run the tests. Jepsen consists of 2 images:

* `jepsen-control`

    the control plane which we will be running the test(s) from
* `jepsen-node`

    the node's image which we will be firing up a few of to form a network of nodes.

Once the setup process have finished we will have:

* 5 running Docker containers (running Debian Linux) for the actual Orbs Nodes
* 1 container for the Nemesis (the Jepsen node responsible to issue network faults)
* 1 container for the control plane to run the tests from

The next step is to run the test(s) themselfs by running:

    $ sh ./ops/test.sh

This will download the needed Clojars and begin running the tests in the suite.

It's important to note that currently we have 1 test which:
* creates a blockchain network on top of the 5 available nodes
* deploys the [Singular](https://github.com/orbs-network/orbs-contract-sdk/tree/master/go/examples/singular) contract onto the network.

    This contract allows managing 1 string key and apply `read`, `set` and `cas` (compare and swap) operations against this cell.

* Once deployed, the test starts performing random operations against the nodes in random order, concurrently (at the moment with 10 proccesses)
    
    collecting the actions into a `knossos` [history model](https://github.com/jepsen-io/knossos) which allows us once the running has finished, to check the correctness of the history recorded against the blockchain network which is our goal.

* During the test running, the `nemesis` node kills active nodes in the network, cuts the network in halves and more.

* Once finished, the history is tested for correctness. in case the analysis finds the history to be incorrect, you will receive a brief summary of what went wrong and when. In general, Jepsen reports a global `valid` or `invalid` on the entire analysis to make it easier to understand the state of the  analysis in a glympse.

## Maintainers

At this moment this project is maintained and developed by the core team at [Orbs - the Hybrid Blockchain](https://github.com/orbs-network) and more specifically for any issues feel free to
write to [Itamar Arjuan](https://github.com/itamararjuan) who understands most of the code here since he wrote it.

## License

Copyright © 2018 Orbs

Distributed under the Eclipse Public License version 1.0 
