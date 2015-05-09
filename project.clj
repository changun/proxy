(defproject proxy "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [compojure "1.3.4"]
                 [ring/ring-core "1.3.2"]
                 [appengine-magic "0.5.0"]
                 [http-kit "2.1.16"]

                 [com.squareup.okhttp/okhttp "2.3.0"]
                 ]
  :plugins [[lein-ring "0.8.10"]]
  :aot :all
  :main proxy.main
  :ring {:handler proxy.core/app
         :init proxy.core/init
         :destroy proxy.core/destroy}


  )
