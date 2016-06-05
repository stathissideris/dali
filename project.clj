(defproject dali "0.7.1"
  :description "A Clojure library for 2D graphics."
  :url "https://github.com/stathissideris/dali"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  ;;:pedantic? :abort

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.codec "0.1.0"]
                 [hiccup "1.0.5"]
                 [enlive "1.1.6"]
                 [prismatic/schema "1.0.3"]
                 [xerces/xerces "2.4.0"]
                 [org.apache.xmlgraphics/batik-transcoder "1.8"
                  :exclusions [[xerces/xercesImpl]
                               [batik/batik-script]
                               #_[fop]]]
                 [org.apache.xmlgraphics/batik-codec "1.8"]
                 [org.apache.xmlgraphics/batik-anim "1.8"]
                 [org.apache.xmlgraphics/xmlgraphics-commons "2.0.1"]
                 [retrograde "0.10"]]

  :jar-exclusions [#"\.swp|\.swo|\.DS_Store"]

  :profiles {:dev {:source-paths ["dev" "examples/src" "test"]
                   :dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [garden "1.3.2"]
                                  [figwheel "0.5.3-2"]
                                  [figwheel-sidecar "0.5.1"]
                                  [com.cemerick/piggieback "0.2.1"]]
                   :plugins [[lein-figwheel "0.5.3-2"]]
                   :figwheel {:css-dirs ["resources/public/css"]
                              :nrepl-port 7888
                              :nrepl-middleware ["cider.nrepl/cider-middleware"
                                                 "cemerick.piggieback/wrap-cljs-repl"]}
                   :cljsbuild {:builds
                               {:dev {:source-paths ["src"]
                                      :figwheel true
                                      :compiler {:asset-path "js/out"
                                                 :output-to  "resources/public/js/main.js"
                                                 :output-dir "resources/public/js/out"
                                                 :optimizations :none
                                                 :pretty-print true
                                                 :source-map true ;"resources/public/js/source_map.js"
                                                 }}}}}})
