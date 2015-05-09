(ns proxy.main
  "A OkHTTP-based (SPDY-supported) web proxy.
  Redirect headers and request method.
  Trust all the certificates."
  (:import (java.net SocketException)
           (com.squareup.okhttp Request$Builder OkHttpClient RequestBody MediaType)
           (javax.net.ssl X509TrustManager SSLContext)
           (java.security SecureRandom)
           (java.security.cert X509Certificate))
  (:require [compojure.core :refer [defroutes routes ANY]]
            [compojure.handler :refer [site]])
  (:use org.httpkit.server)
  (:gen-class)
  )

; Create a OkHttpClient that trust any certificates
(def client (let [ssl (SSLContext/getInstance "SSL")]
              (.init ssl nil (into-array (list (proxy [X509TrustManager] []
                                                 (getAcceptedIssuers [] (make-array X509Certificate 0))
                                                 (checkClientTrusted [ chain auth-type]
                                                   )
                                                 (checkServerTrusted [ chain auth-type]
                                                   ))))
                     (SecureRandom.))
              (.setSslSocketFactory (OkHttpClient.) (.getSocketFactory ssl) )))

(defonce server (atom nil))


(defn handler [{:keys [params headers body content-type request-method] :as req}]
  (with-channel req channel
                (println req)
                (try (let [request (-> (Request$Builder.)
                                       (.url  (:url params)))
                           ; convert method name
                           method (clojure.string/upper-case  (name request-method))
                           ; redirect headers
                           request (reduce (fn [request [key val]]
                                             (let [key (clojure.string/trim
                                                         (clojure.string/lower-case (name key)))]
                                               ; do not redirect "Host" "Accept-Encoding", and "Connection" headers,
                                               ; which okHTTP will automatically specify
                                               (cond-> request
                                                       (not (#{"host" "accept-encoding" "connection"} key))
                                                       (.header  key val))
                                               )

                                             ) request headers)
                           body (if content-type
                                  (RequestBody/create (MediaType/parse content-type)
                                                      (slurp body)))
                           request (-> request
                                       (.method  method body)
                                       (.build))
                           res (.execute (.newCall client request))
                           response-code (.code res)
                           response-body (-> res
                                             (.body)
                                             (.byteStream))
                           response-headers (.headers res)

                           ]
                       ; Respond the page fetch results
                       (send! channel {:status response-code
                                       :body response-body
                                       ; do not response some transfer-related headers that confuse the client
                                       :header (reduce (fn [ret name]
                                                         (cond-> ret
                                                                 (not (#{"server"
                                                                         "transfer-encoding"
                                                                         "strict-transport-security"
                                                                         "connection"
                                                                         "alternate-protocol"
                                                                         "content-length"} (clojure.string/lower-case name)))
                                                                 (assoc name (.get response-headers name)))
                                                         ) {} (.names response-headers))
                                       })
                       )
                     (catch Exception e (send! channel {:status 500 :body (str e)})))
                )

  )

(defroutes all-routes
           (ANY "/proxy" [] handler))     ;; websocket

(defn stop-server []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))



(defn start  [port]
  (stop-server)
  (reset! server (run-server (site  #'all-routes) {:port port}))
  )

(defn -main
  "Start proxy at the port specified or at port 9999"
  [& args]
  (start (or (first args) 9999))
  (loop []
    (Thread/sleep 100000000)
    (recur)
    )
  )
