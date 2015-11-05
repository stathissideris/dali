(defproject dali "0.7.0"
  :description "A Clojure library for 2D graphics."
  :url "https://github.com/stathissideris/dali"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  ;;:pedantic? :abort
  
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [hiccup "1.0.5"]
                 [enlive "1.1.6"]
                 [prismatic/schema "1.0.3"]
                 [org.clojure/clojurescript "1.7.145"]
                 [xerces/xerces "2.4.0"]
                 [org.apache.xmlgraphics/batik-transcoder "1.7"
                  :exclusions [[xerces/xercesImpl]
                               [batik/batik-script]
                               #_[fop]]]
                 [org.apache.xmlgraphics/batik-codec "1.7"]
                 [retrograde "0.10"]]

  :plugins [[lein-cljsbuild "1.1.1-SNAPSHOT" :exclusions [org.clojure/clojure]]
            [lein-figwheel "0.4.1" :exclusions [org.codehaus.plexus/plexus-utils]]
            [org.codehaus.plexus/plexus-utils "3.0"] ;;figwheel itself has conflicting deps
            ]
  
  :jar-exclusions [#"\.swp|\.swo|\.DS_Store"]

  :profiles {:dev {:source-paths ["dev" "examples/src"]
                   :dependencies [[org.clojure/tools.namespace "0.2.11"]]}}
  
  :cljsbuild
  {:builds [{:id "dali"
             :source-paths ["src"]
             :figwheel true
             :compiler
             {:output-to      "release-js/dali.js"
              :optimizations  :advanced
              :pretty-print   false
              :output-wrapper false}}]})
