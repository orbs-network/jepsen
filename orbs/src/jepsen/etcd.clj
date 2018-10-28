(ns jepsen.etcd
  (:gen-class)
  (:require [clojure.tools.logging :refer :all]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [verschlimmbesserung.core :as v]
            [slingshot.slingshot :refer [try+]]
            [knossos.model :as model]
            [jepsen [checker :as checker]
             [cli :as cli]
             [client :as client]
             [control :as c]
             [db :as db]
             [generator :as gen]
             [independent :as independent]
             [nemesis :as nemesis]
             [tests :as tests]
             [util :as util :refer [timeout]]]
            [jepsen.checker.timeline :as timeline]
            [jepsen.control.util :as cu]
            [jepsen.os.debian :as debian]))

(use '[clojure.java.shell :only [sh]])
(def dir     "/opt/orbs")
(def binary  "/opt/orbs/orbs-node")
(def logfile "/opt/orbs/logs/node.log")
(def pidfile (str dir "/orbs.pid"))

(defn node-url
  "An HTTP url for connecting to a node on a particular port."
  [node port]
  (str "http://" (name node) ":" port))

(defn peer-url
  "The HTTP url for other peers to talk to a node."
  [node]
  (node-url node 4400))

(defn client-url
  "The HTTP url clients use to talk to a node."
  [node]
  (node-url node 2379))

(defn initial-cluster
  "Constructs an initial cluster string for a test, like
  \"foo=foo:2380,bar=bar:2380,...\""
  [test]
  (->> (:nodes test)
       (map (fn [node]
              (str (name node) "=" (peer-url node))))
       (str/join ",")))

(def orbs-contract-sdk-basepath "/opt/go/src/github.com/orbs-network/orbs-contract-sdk")
(def singular-contract-basedir (str orbs-contract-sdk-basepath "/go/examples/singular"))
(def gammacli-binary-path (str orbs-contract-sdk-basepath "/gamma-cli"))
(defn gamma-cli-run-call
  "Performs a call op against gamma-cli executable"
  [jsonpath node]
  (let [result (sh gammacli-binary-path "run" "call" jsonpath "-host" (str "http://" node ":9090"))]
    (let [out (get (json/read-str (get result :out)) "OutputArguments")]
      (get (get out 0) "Value"))))

(defn cli-call-successful?
  "Checks the map returned from the sh command for success"
  [exitcode jsonstring]
  (if (= exitcode 0) true false))

(defn gamma-cli-deploy
  "Performs a deploy op against gamma-cli executable"
  [name jsonpath]
  (sh gammacli-binary-path "deploy" name jsonpath "-host" "http://n3:9090" :dir orbs-contract-sdk-basepath))

(defn gamma-cli-read-singular
  "Gets the counter value through the deployed 'Counter' smart contract"
  [node]
  (gamma-cli-run-call (str singular-contract-basedir "/jsons/get.json") node))

(defn gamma-cli-deploy-contract
  "Deploys the contract onto the network using gamma-cli"
  [name jsonpath]
  (let [result (gamma-cli-deploy name jsonpath)]
    (info "the returned map from deployment sh command" result)
    (if (cli-call-successful? (get result :exit) (get result :out))
      (info "Contract deployent is successful!")
      (throw (Exception. "Contract deployment was not successful!" result)))))

(def nodes-keys {"n1" {:public "dfc06c5be24a67adee80b35ab4f147bb1a35c55ff85eda69f40ef827bddec173", :private "93e919986a22477fda016789cca30cb841a135650938714f85f0000a65076bd4dfc06c5be24a67adee80b35ab4f147bb1a35c55ff85eda69f40ef827bddec173"}
                 "n2" {:public "92d469d7c004cc0b24a192d9457836bf38effa27536627ef60718b00b0f33152", :private "3b24b5f9e6b1371c3b5de2e402a96930eeafe52111bb4a1b003e5ecad3fab53892d469d7c004cc0b24a192d9457836bf38effa27536627ef60718b00b0f33152"}
                 "n3" {:public "a899b318e65915aa2de02841eeb72fe51fddad96014b73800ca788a547f8cce0", :private "2c72df84be2b994c32a3f4ded0eab901debd3f3e13721a59eed00fbd1da4cc00a899b318e65915aa2de02841eeb72fe51fddad96014b73800ca788a547f8cce0"}
                 "n4" {:public "58e7ed8169a151602b1349c990c84ca2fb2f62eb17378f9a94e49552fbafb9d8", :private "163987afcee69969cae3528161d84e32f76b09bbf0dd77dd704e5cb915c7d56f58e7ed8169a151602b1349c990c84ca2fb2f62eb17378f9a94e49552fbafb9d8"}
                 "n5" {:public "23f97918acf48728d3f25a39a5f091a1a9574c52ccb20b9bad81306bd2af4631", :private "74b63e4f6f908ac42c1b4c7b3b6028c7b665df4375c1acbf4dce2b1b91aefc5b23f97918acf48728d3f25a39a5f091a1a9574c52ccb20b9bad81306bd2af4631"}
                 "n6" {:public "07492c6612f78a47d7b6a18a17792a01917dec7497bdac1a35c477fbccc3303b", :private "d9fae84f80b842f57770a9ae67c7eb58ce502eb32502d43ddec5da115ccd2e2107492c6612f78a47d7b6a18a17792a01917dec7497bdac1a35c477fbccc3303b"}
                 "n7" {:public "43a4dbbf7a672c6689dbdd662fd89a675214b00d884bb7113d3410b502ecd826", :private "c7c2579fb128bf1d687081600f171060d95da22543920ea3490d8e71980babe943a4dbbf7a672c6689dbdd662fd89a675214b00d884bb7113d3410b502ecd826"}
                 "n8" {:public "469bd276271aa6d59e387018cf76bd00f55c702931c13e80896eec8a32b22082", :private "0d953392b90e5cf5f0162cb289ff1b77a358921201aa5c91c902b38aa22a1878469bd276271aa6d59e387018cf76bd00f55c702931c13e80896eec8a32b22082"}
                 "n9" {:public "102073b28749be1e3daf5e5947605ec7d43c3183edb48a3aac4c9542cdbaf748", :private "57249e0b586083a60df94044971416cb9fdd373855aac9e04bceb4c96e53559e102073b28749be1e3daf5e5947605ec7d43c3183edb48a3aac4c9542cdbaf748"}
                 "n10" {:public "70d92324eb8d24b7c7ed646e1996f94dcd52934a031935b9ac2d0e5bbcfa357c", :private "f1c41ba8a1d78f7cdc4f4ff23f3b736e30c630085697d6503e16ac899646f5ab70d92324eb8d24b7c7ed646e1996f94dcd52934a031935b9ac2d0e5bbcfa357c"}})

(defn federation-member-list
  "Constructs the federation member list"
  [test]
  (->> (:nodes test)
       (map (fn [node]
              (str "{\"Key\": \"" (get (get nodes-keys node) :public) "\",\"IP\":\"" (name node) "\", \"Port\": 4400}")))
       (str/join ",")))

(defn orbs-member-config-json
  "Creates a configuration JSON string for a given node and test"
  [test node]
  (str "{\"node-public-key\":\""
       (get (get nodes-keys node) :public)
       "\",\"node-private-key\":\""
       (get (get nodes-keys node) :private)
       "\",\"constant-consensus-leader\":\""
       (get (get nodes-keys "n2") :public)
       "\", \"federation-nodes\":["
       (federation-member-list test) "]}"))

(defn db
  "Orbs Blockchain install/teardown for a particular version."
  [version]
  (reify db/DB
    (setup! [_ test node]
      (c/su
       (info node "Running the Orbs binary" version)
       (spit (str node ".json") (orbs-member-config-json test node))
       (c/upload (str node ".json") (str "/var/opt/" node ".json"))
       (c/exec "mkdir" "-p" "/opt/orbs/logs")
       (c/exec* "export GOPATH=/go;" "/opt/orbs/orbs-node" "-listen" ":9090" "--silent" "--config" (str "/var/opt/" node ".json")
                "--log" "/opt/orbs/logs/node.log" "&>/dev/null" "&")
       (info "Sleeping for a second to let the blockchain finish initial handshakes")
       (Thread/sleep 1000)
       ))

    (teardown! [_ test node]
      (info node "Stopping Orbs binary..")
      (c/exec "./opt/orbs/stop-orbs.sh"))

    db/Primary
    (setup-primary! [_ test node]
      (info node "Performing a one time setup (Singular contract deployment)")
      (gamma-cli-deploy-contract "Singular" (str singular-contract-basedir "/singular.go")))))

    ; db/LogFiles
    ; (log-files [_ test node]
    ;   [logfile])))

(defn parse-long
  "Parses a string to a Long. Passes through `nil`."
  [s]
  (when s (Long/parseLong s)))

(defn client
  "A client for a single compare-and-set register"
  [conn]
  (reify client/Client
    (open! [_ test node]
      (client node))

    (invoke! [this test op]
      (case (:f op)
        :read (assoc op :type :ok, :value (gamma-cli-read-singular conn))))

    ; If our connection were stateful, we'd close it here.
    ; Verschlimmbesserung doesn't hold a connection open, so we don't need to.
    (close! [_ _])

    (setup! [_ _])
    (teardown! [_ _])))


(defn r   [_ _] {:type :invoke, :f :read, :value nil})
(defn w   [_ _] {:type :invoke, :f :write, :value (rand-int 5)})
(defn cas [_ _] {:type :invoke, :f :cas, :value [(rand-int 5) (rand-int 5)]})

(defn orbs-singular-test
  "Given an options map from the command-line runner (e.g. :nodes, :ssh,
  :concurrency, ...), constructs a test map."
  [opts]
  (merge tests/noop-test
         {:name "orbs"
          :os debian/os
          :db (db "alpha")
          :client (client nil)
          :generator (->> r
                          (gen/stagger 1)
                          (gen/nemesis nil)
                          (gen/time-limit 15))}))

(defn -main
  "Handles command line arguments. Can either run a test, or a web server for
  browsing results."
  [& args]
  (cli/run! (merge (cli/single-test-cmd {:test-fn orbs-singular-test})
                   (cli/serve-cmd))
            args))
