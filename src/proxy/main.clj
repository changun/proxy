(ns proxy.main
  (:require [proxy.local :as local]
            [proxy.lambda :as lambda])
  (:gen-class)
  )


(defn -main
  "Start proxy at the port specified or at port 9999"
  [& [type port callback-base]]
  (cond
    (= type "local")
    (local/start (Integer/parseInt port))
    (= type "lambda")
    (lambda/start (Integer/parseInt port) callback-base)
    )

  (loop []
    (Thread/sleep 100000000)
    (recur)
    )
  )
