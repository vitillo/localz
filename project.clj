(defproject localz "0.1.0-SNAPSHOT"
  :description "Localz, the stupid geolocalized chat"
  :url "https://github.com/vitillo/localz"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [http-kit  "2.1.16"]
                 [compojure "1.1.6"]
                 [org.clojure/clojurescript  "0.0-2311"]
                 [org.clojure/core.async  "0.1.267.0-0d7780-alpha"]
                 [om "0.7.1"]
                 [org.clojure/core.match  "0.2.1"]
                 [jarohen/chord  "0.4.2"]
                 [sablono "0.2.21"]
                 [cheshire  "5.3.1"]]

  :jvm-opts  ["-Xmx1G"]

  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]
            [com.cemerick/austin "0.1.4"]]

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/cljs/localz"]
                        :compiler {
                                   :output-to "resources/public/localz.js"
                                   :output-dir "resources/public/out"
                                   :optimizations :none
                                   :pretty-print true
                                   :source-map true}}]})
