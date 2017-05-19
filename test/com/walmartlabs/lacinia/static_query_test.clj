(ns com.walmartlabs.lacinia.static-query-test
  (:require
    [clojure.test :refer [deftest is use-fixtures]]
    [com.walmartlabs.lacinia.pedestal :as lp]
    [clj-http.client :as client]
    [clojure.string :as str]
    [cheshire.core :as cheshire]
    [io.pedestal.interceptor :refer [interceptor]]
    [com.walmartlabs.lacinia.test-utils :refer [sample-schema-fixture
                                                send-request
                                                send-json-request]]))


(def queries {"1" "{ echo(value: \"hello\") { value method }}"})


(use-fixtures :once 
  (sample-schema-fixture 
   {:graphiql true 
    :query-lookup
    (interceptor
     {:name ::in-memory-lookup 
      :enter (fn [context]
               (let [id (-> context :request :body :id)]
                 (assoc-in context [:request :body :query] (queries id))))})}))



(deftest can-handle-json-static
  (let [response
        (send-json-request :post
                           {:id "1"})]
    (is (= 200 (:status response)))
    (is (= {:data {:echo {:method "post"
                          :value "hello"}}}
           (:body response)))))


