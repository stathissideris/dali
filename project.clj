(defproject dali "0.7.6"
  :description "A Clojure library for 2D graphics."
  :url "https://github.com/stathissideris/dali"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  ;;:pedantic? :abort

  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/data.codec "0.1.0"]
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

  :profiles {:dev {:source-paths ["dev" "examples/src"]
                   :dependencies [[org.clojure/tools.namespace "0.2.11"]
                                  [garden "1.3.2"]
                                  [hiccup "1.0.5"]]}})
