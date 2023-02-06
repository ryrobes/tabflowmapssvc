(defproject tabflowmapssvc "0.1.0-SNAPSHOT"
  :description "tabflowmapssvc - backend server component (processes the TWB into EDN for the front-end)"
  :url "https://github.com/ryrobes/tabflowmapssvc"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [mvxcvi/puget "1.3.2"] ;; https://github.com/greglook/puget
                 [com.wallbrew/clj-xml "1.7.2"]
                 [io.pedestal/pedestal.service "0.5.10"]
                 [io.pedestal/pedestal.jetty "0.5.10"]
                 [org.clojure/data.xml "0.0.8"]]
  :jvm-opts ["--illegal-access=deny"] ;; due to XML reflection warnings
  :main ^:skip-aot tabflowmapssvc.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
