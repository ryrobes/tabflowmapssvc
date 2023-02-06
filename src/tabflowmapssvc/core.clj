(ns tabflowmapssvc.core
  (:require [clj-xml.core :as xml]
            [clojure.string :as cstr]
            [io.pedestal.http :as http]
            [io.pedestal.http.body-params :as body-params]
            [ring.util.response :as ring-resp]
            [io.pedestal.http :as server]
            [puget.printer :as puget])
  (:gen-class))

(defn keypaths
  ([m] (keypaths [] m ()))
  ([prev m result]
   (reduce-kv (fn [res k v] (if (associative? v)
                              (keypaths (conj prev k) v res)
                              (conj res (conj prev k))))
              result
              m)))

(defn pp [x]
  (try (puget/with-options {:width 300}
    (puget/cprint x)) (catch Exception e (println (str "pp failed - " e)))))

(defn process-file [file-str file-name]
  (let [ff (xml/xml-str->edn file-str {:preserve-attrs? true :remove-empty-attrs? true})
        dash-map {:dashboards (try (into {}
                                         (for [r (range (count (get-in ff [:workbook :dashboards])))]
                                           {(get-in ff [:workbook :dashboards r :dashboard-attrs :name])
                                            (get-in ff [:workbook :dashboards r :dashboard])})) (catch Exception e (pp e)))}
        ws-map {:worksheets (into {}
                                  (for [r (range (count (get-in ff [:workbook :worksheets])))]
                                    {(get-in ff [:workbook :worksheets r :worksheet-attrs :name])
                                     (get-in ff [:workbook :worksheets r :worksheet])}))}
        actions-map (into {}
                          (for [r (range (count (get-in ff [:workbook :actions])))]
                            {(get-in ff [:workbook :actions r :action-attrs :caption]
                                     (get-in ff [:workbook :actions r :action-attrs :name]))
                             (get-in ff [:workbook :actions r :action])}))
        nav-actions-map (into {}
                              (for [r (range (count (get-in ff [:workbook :actions])))]
                                {(get-in ff [:workbook :actions r :nav-action-attrs :caption]
                                         (get-in ff [:workbook :actions r :nav-action-attrs :name]))
                                 (merge (get-in ff [:workbook :actions r :nav-action]) {:ttype :nav-action})}))
        param-actions-map (into {}
                                (for [r (range (count (get-in ff [:workbook :actions])))]
                                  {(get-in ff [:workbook :actions r :edit-parameter-action-attrs :caption]
                                           (get-in ff [:workbook :actions r :edit-parameter-action-attrs :name]))
                                   (merge (get-in ff [:workbook :actions r :edit-parameter-action]) {:ttype :edit-parameter-action})}))
        actions-map {:actions (merge actions-map nav-actions-map param-actions-map)}
        windows-map {:windows (into {}
                                    (for [r (range (count (get-in ff [:workbook :windows])))]
                                      {(get-in ff [:workbook :windows r :window-attrs :caption]
                                               (get-in ff [:workbook :windows r :window-attrs :name]))
                                       (get-in ff [:workbook :windows r :window])}))}
        poss-ky (filter #(not (some (fn [x] (or (= :style x)
                                                (= :panes x)
                                                (= :window x) (= :windows x)
                                                (= :zones x)
                                                (= :table x)
                                                (= :layout-options x)
                                                (= :view x)
                                                (= :datasources x))) %)) (distinct (keypaths ff)))
        twb-map (merge ws-map dash-map actions-map windows-map)]
    (pp [:finished file-name :keypaths (count poss-ky)])
    twb-map))

(defn send-edn-success [content]
  (assoc (http/edn-response content) :headers {"Content-Type" "application/edn"}))

(defn load-twb [request]
  (pp [:received-data-from (get-in request [:edn-params :fname])])
  (let [file-name (get-in request [:edn-params :fname])
        fdata (get-in request [:edn-params :image])
        processed-data (process-file fdata file-name)]
    (send-edn-success {:image processed-data
                       :fname file-name})))

(defn static-root [request] (ring-resp/content-type (ring-resp/resource-response "index.html" {:root "public"}) "text/html"))

(def common-interceptors [(body-params/body-params) http/html-body])

(def routes #{["/" :get (conj common-interceptors `static-root)]
              ["/load-twb" :post (conj common-interceptors `load-twb)]})

(def service {:env :prod
              ::http/routes routes
              ::http/allowed-origins {:creds false :allowed-origins (constantly true)}
              ::http/secure-headers {:content-security-policy-settings {:object-src "none"}}
              ::http/resource-path "/public"
              :max-threads 50
              ::http/type :jetty
              ::http/host "0.0.0.0"
              ::http/port 8888
              ::http/container-options {:h2c? true
                                        :h2? false
                                        :ssl? false}})

(defonce runnable-service (server/create-server service))

(def web-server (atom nil))

(defn create-web-server! []
  (pp [:*web (format "starting web server @ %d" 8888)])
  (reset! web-server
          (server/start runnable-service)))


(defn -main
  [& args]
 ; (let [;file-name "./US_Superstore_14.twb" ; "./exec.twb" ; 
 ;       file-name "./exec.twb" ; 
 ;       file-str (str (slurp file-name))]
 ;   (process-file file-str))
  (create-web-server!))
