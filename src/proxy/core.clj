(ns proxy.core
  "A Google App Engine proxy. Redirect headers, request method, and body."

  (:require [compojure.core :refer [defroutes routes ANY]]
            [compojure.handler :as handler]
            [compojure.route :as route]
            [appengine-magic.services.url-fetch :as fetch]
            )
  (:import (java.io ByteArrayInputStream)))
(defn init [] (println "gae-app-demo is starting"))
(defn destroy [] (println "gae-app-demo is shutting down"))


(defroutes home-routes
           ; a proxy that fetch the webpage specify in url param.
           (ANY "/proxy"  req
                (try
                  (let [body (slurp (:body req))
                        ; fetch webpage using GAE fetch service
                        res (fetch/fetch
                              (:url (:params req))
                              ; :get :post :put , etc.
                              :method (:request-method req)
                              ; redirect all the headers
                              :headers (:headers req)
                              ; redirect body (if any)
                              :body (.getBytes body))]
                    ; redirect response back to the client
                    {:status  (:response-code res)
                     :headers (:headers res)
                     :body    (ByteArrayInputStream. (:content res))}
                    )
                  (catch Exception e
                    ; something went wrong. return 500 and error message
                    {:status 500
                     :body (str e
                                (:url (:params req))
                                (:request-method req)
                                (:headers req)
                                )}))
                )
           ; echo handler for testing
           (ANY "/echo"  req
                {:status 200
                 :body (str req)}))
(def app (-> (routes home-routes)
             (handler/site)))






