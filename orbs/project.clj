(defproject jepsen.orbs "1.0.0"
  :description "A Jepsen test for Orbs - The Hybrid Blockchain"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :main jepsen.orbs
  :jvm-opts ["-Dcom.sun.management.jmxremote"]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]
                 [jepsen "0.1.9-SNAPSHOT"]
                 [verschlimmbesserung "0.1.3"]])
