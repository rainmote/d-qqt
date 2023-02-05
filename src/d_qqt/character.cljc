(ns d-qqt.character
  (:require #?(:clj  [d-qqt.macros :refer [load-all-paths]]
               :cljs [d-qqt.macros :refer-macros [load-all-paths]])
            clojure.set
            [d-qqt.utils :as utils]
            [taoensso.timbre :as log]))

(def base-paths (into #{} (load-all-paths "base")))
(def all-body [:head :face :hair :body :leg :foot :cloth])

(defn get-body-y-offset [body]
  (get {:body [0 0]
        :hair [0 -38]
        :head [0 -33]
        :face [0 -33]
        :cloth [0 50]
        :leg [0 30]
        :foot [0 10]} body 0))
(def all-status [:walk :stand :trigger :die :win :cry :lose :wait])
(def direction {:right 0
                :up    1
                :left  2
                :down  3})
(def revert-dir
  (->> direction
       (map (fn [[k v]]
              [(str v) k]))
       (into (hash-map))))

(defn idx->direction [idx]
  (get revert-dir idx))

(def direction-keys (vec (keys direction)))

(defn match-files [prefix]
  (->> base-paths
       (filter #(clojure.string/starts-with? % prefix))))


;; (defn load-images [game *state conf metas callback]
;;   (let [cache-key (:default-idx conf)
;;         _ (log/info "cache key:" cache-key)
;;         down-img (get-in @*state [cache-key :walk :body :down])]
;;     (when-not (some? down-img)
;;       (->> metas
;;            (map (fn [v]
;;                   (log/info "load files: " (:files v))
;;                   (->> (:files v)
;;                        (map #(utils/get-image % callback))
;;                        doall)))
;;            doall))))

(defn load-images [game *state conf metas callback]
  (let [cache-key (:default-idx conf)
        _ (log/info "cache key:" cache-key)
        down-img (get-in @*state [cache-key :walk :body :down])]
    (when-not (some? down-img)
      (doseq [v metas]
        (log/info "load files: " (:files v))
        (doseq [f (:files v)]
          (utils/get-image f callback))))))

(defn load-meta [{:keys [default-idx]
                  :as   conf}]
  (do (let [metas
            (for [status [:walk]
                  body all-body
                  dir [:down]]
              (let [dir-idx (get direction dir)
                    idx (get-in conf [status body] default-idx)
                    prefix (utils/myformat "%s%d_%s_%d_" (name body) idx (name status) dir-idx)
                    mask-prefix (utils/myformat "%s%d_%s_m_%d_" (name body) idx (name status) dir-idx)
                    directory (str "object/" (name body))
                    prefix-path (str directory "/" prefix)
                    mask-prefix-path (str directory "/" mask-prefix)
                    files (match-files prefix-path)
                    mask-files (match-files mask-prefix-path)]
                {:status     status
                 :body       body
                 :direction  dir
                 :files      (->> files (map #(str "base/" %)) sort)
                 :mask-files (->> mask-files (map #(str "base/" %)) sort)}))
                ;;valid-metas
            #_(transduce (comp (filter #(-> % :files not-empty)))
                         (fn ([] {})
                           ([acc] acc)
                           ([acc e] (assoc-in acc [(:status e) (:body e) (:direction e)]
                                              {:files      (:files e)
                                               :mask-files (:mask-files e)})))
                         metas)]
        (->> metas
             (filter #(-> % :files not-empty))))))