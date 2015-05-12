(ns proxy.lambda
  "A AWS Lambda-based proxy"
  (:import

    (java.net URL)
    (java.util UUID Base64)
    (java.io ByteArrayInputStream)
    (java.nio ByteBuffer)
    (com.fasterxml.jackson.databind.util ByteBufferBackedInputStream))
  (:require [cheshire.core :as json]
            [amazonica.aws.lambda :as lambda]
            [clojure.core.async :as async]
            [ring.util.codec :as codec]
            )
  (:require [compojure.core :refer [defroutes routes ANY POST]]
            [compojure.handler :refer [site]]
            [compojure.route :as route])
  (:use org.httpkit.server)
  )


(defonce server (atom nil))
(def get-cred (memoize (fn [] (clojure.edn/read-string (slurp "aws.cred")))) )


(defn lambda-invoke [payload]
  (lambda/invoke-async (get-cred)
                 :function-name "httpRequest"
                 :invoke-args payload
                 ))
(defn callback-url [uuid] (str "http://andy-hsieh.cloudapp.net/lambda-proxy/callback/" uuid))
(defn lambda
  "Make http request to ptt.cc via AWS Lambda"
  [method url body headers callback]
  (let [payload (-> {:method   method
                     :url url
                     :body     (if body (String. (.encode (Base64/getEncoder) (.getBytes (slurp body)))) "")
                     :headers  headers
                     :callback callback
                     }
                    (json/generate-string)

                    )
        ret (lambda-invoke payload)
        ]
    (println "LAMBDA:" ret)
    )
  )

(def queue (atom {}))
(defn callback-handler [{:keys [params body headers] :as req} uuid]
  (println "CALLBACK: " {:uuid uuid :headers headers})
  (let [client-channel (get @queue uuid)
        lambda-channel (:async-channel req)
        response-status (get headers "statuscode")
        response-headers (json/parse-string (get headers "responseheaders"))
        ]

    (if client-channel
      (do
        (send! client-channel {:status (Integer/parseInt response-status)

                               :body   body
                               ; do not response some transfer-related headers that confuse the client
                               :headers (reduce (fn [ret [name value]]
                                                  (cond-> ret
                                                          (not (#{"server"
                                                                  "transfer-encoding"
                                                                  "strict-transport-security"
                                                                  "connection"
                                                                  "alternate-protocol"
                                                                  "content-length"} (clojure.string/lower-case name)))
                                                          (assoc name value))
                                                  ) {} response-headers)
                               })
        (send!  lambda-channel{:status 200 :body ""})

        )
      (send! lambda-channel {:status 404})
      )
    )
  )
(defn proxy-handler [{:keys [params headers body request-method] :as req}]
  (let [url (get params "url")
        uuid (.toString (UUID/randomUUID))]
    (println "PROXY: " {:uuid uuid :url url :request-method request-method :headers headers})
    (swap! queue assoc uuid (:async-channel req))
    (async/thread
      (lambda (name request-method) url body headers (callback-url uuid)))

      )
  )

(defn stop-server []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))


(defn handler [req]
  (let [req (assoc req :params (try (codec/form-decode (:query-string req)) (catch Exception _ nil)))]

    (with-channel req channel              ; get the channel
                  ;; communicate with client using method defined above
                  (on-close channel (fn [status] (println "channel closed")))
                  (condp #(.startsWith %2 %1)
                    (:uri req)
                    "/proxy"
                      (proxy-handler req)
                    "/callback/"
                      (callback-handler req (subs (:uri req) (count "/callback/")))
                    )
                  ))
  )
(defn start  [port]
  (stop-server)
  (reset! server (run-server handler {:port port}))
  (println "Start Lambda proxy at" port)
  )



