(defproject petardo.kkbibapi "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [hickory "0.7.1"]
                 [clj-http "3.12.3"]
                 [javax.servlet/servlet-api "2.5"]
                 [ring/ring-core "1.9.4"]
                 [ring/ring-devel "1.9.4"]
                 [ring/ring-jetty-adapter "1.9.4"]]
  :plugins [[io.taylorwood/lein-native-image "0.3.0"]
            [lein-ring "0.12.5"]]
  :main ^:skip-aot petardo.kkbibapi.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}

             :dev     {:resource-paths ["test-resources"]
                       :source-paths ["dev"]
                       :repl-options {:init-ns user}
                       :env          {:profile "dev"}}}

  :native-image {:name "kkbibapi"
                 :opts ["--report-unsupported-elements-at-runtime"
                        "--initialize-at-build-time"
                        "--verbose"]})
