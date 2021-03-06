(ns com.walmartlabs.lacinia.pedestal-test
  (:require
    [clojure.test :refer [deftest is use-fixtures]]
    [com.walmartlabs.lacinia.pedestal :as lp]
    [clj-http.client :as client]
    [clojure.string :as str]
    [com.walmartlabs.lacinia.test-utils :refer [sample-schema-fixture
                                                send-request
                                                send-json-request]]))


(use-fixtures :once (sample-schema-fixture {:graphiql true}))

(deftest simple-get-query
  (let [response (send-request "{ echo(value: \"hello\") { value method }}")]
    (is (= 200 (:status response)))
    (is (= "application/json"
           (get-in response [:headers "Content-Type"])))
    (is (= {:data {:echo {:method "get"
                          :value "hello"}}}
           (:body response)))))

(deftest simple-post-query
  (let [response (send-request :post "{ echo(value: \"hello\") { value method }}")]
    (is (= 200 (:status response)))
    (is (= {:data {:echo {:method "post"
                          :value "hello"}}}
           (:body response)))))

(deftest includes-content-type-check-on-post
  (let [response
        (send-json-request :post
                           {:query "{ echo(value: \"hello\") { value method }}"}
                           "text/plain")]
    (is (= {:body {:message "Request content type must be application/graphql or application/json."}
            :status 400}
           (select-keys response [:status :body])))))

(deftest can-handle-json
  (let [response
        (send-json-request :post
                           {:query "{ echo(value: \"hello\") { value method }}"})]
    (is (= 200 (:status response)))
    (is (= {:data {:echo {:method "post"
                          :value "hello"}}}
           (:body response)))))


(deftest can-handle-vars-json
  (let [response
        (send-json-request :post
                           {:query "query ($v: String) {
                                      echo(value: $v) { value }
                            }"
                            :variables {:v "Calculon"}})]
    (is (= {:body {:data {:echo {:value "Calculon"}}}
            :status 200}
           (select-keys response [:status :body])))))

(deftest can-handle-operation-name-json
  (let [response
        (send-json-request :post
                           {:query "query stuff($v: String) {
                                      echo(value: $v) { value }
                            }"
                            :variables {:v "Calculon"}
                            :operationName "stuff"})]
    (is (= {:body {:data {:echo {:value "Calculon"}}}
            :status 200}
           (select-keys response [:status :body])))))

(deftest status-set-by-error
  (let [response (send-request "{ echo(value: \"Baked.\", error: 420) { value }}")]
    (is (= {:body {:data {:echo {:value "Baked."}}
                   :errors [{:arguments {:error "420"
                                         :value "Baked."}
                             :locations [{:column 0
                                          :line 1}]
                             :message "Forced error."
                             :query-path ["echo"]}]}
            :status 420}
           (select-keys response [:status :body])))))

(deftest can-handle-vars
  (let [response (send-request :post "query ($v: String) {
    echo(value: $v) { value }
   }" {:v "Calculon"})]
    (is (= {:body {:data {:echo {:value "Calculon"}}}
            :status 200}
           (select-keys response [:status :body])))))

(deftest can-access-graphiql
  (let [response (client/get "http://localhost:8888/" {:throw-exceptions false})]
    (is (= 200 (:status response)))
    (is (str/includes? (:body response) "<html>"))))
