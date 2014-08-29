(defproject dali "0.4.0"
  :description "A Clojure library for 2D graphics."
  :url "https://github.com/stathissideris/dali"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :test-paths ["target/test-classes"]
  
  :jar-exclusions [#"\.cljx|\.swp|\.swo|\.DS_Store"]
  :source-paths ["src/cljx"]

  :cljx {:builds [{:source-paths ["src/cljx"]
                   :output-path "target/classes"
                   :rules :clj}
                  
                  {:source-paths ["src/cljx"]
                   :output-path "target/classes"
                   :rules :cljs}
                  
                  {:source-paths ["test/cljx"]
                   :output-path "target/test-classes"
                   :rules :clj}
                  
                  {:source-paths ["test/cljx"]
                   :output-path "target/test-classes"
                   :rules :cljs}]}

  :cljsbuild {:test-commands {"node" ["node" :node-runner "target/testable.js"]}
              :builds [{:source-paths ["target/classes"]
                        :compiler {:output-to "target/testable.js"
                                   :optimizations :advanced
                                   :pretty-print false}}]}

  :hooks [cljx.hooks]

  :profiles {:dev {:plugins [[org.clojure/clojurescript "0.0-2202"]
                             [com.keminglabs/cljx "0.3.2" :exclusions [[org.clojure/clojure]]]
                             [lein-cljsbuild "1.0.3"]]
                   :aliases {"cleantest" ["do" "clean," "cljx" "once," "test,"
                                          "cljsbuild" "test"]
                             "clojars-deploy" ["do" "clean," "cljx" "once," "deploy" "clojars"]}
                   :dependencies [[enlive "1.1.5"]]}}
  
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [hiccup "1.0.5"]
                 [enlive "1.1.5"]
                 [org.clojure/clojurescript "0.0-2202"]
                 [xerces/xerces "2.4.0"]
                 [org.apache.xmlgraphics/batik-transcoder "1.7"
                  :exclusions [[xerces/xercesImpl]
                               [batik/batik-script]
                               #_[fop]]]
                 [org.apache.xmlgraphics/batik-codec "1.7"]
                 [retrograde "0.10"]])
