(ns d-qqt.character
    (:require [taoensso.timbre :as log]
      [d-qqt.utils :as utils]
      #?(:clj  [d-qqt.macros :refer [load-all-paths]]
         :cljs [d-qqt.macros :refer-macros [load-all-paths]])))

(def base-paths (into #{} (load-all-paths "base")))
(def all-body [:hair :head :face :body :leg :foot])
(def all-status [:walk :stand :trigger :die :win :cry :lose :wait])
(def direction {:right 0
                :up    1
                :left  2
                :down  3})
(def direction-keys (vec (keys direction)))

(defn match-files [prefix]
      (->> base-paths
           (filter #(clojure.string/starts-with? % prefix))))

(defn load [{:keys [default-idx]
             :as   conf}]
      (do (let [result ()
                metas
                (for [status all-status
                      body all-body
                      dir direction-keys]
                     (let [dir-idx (get direction dir)
                           idx (get-in conf [status body] default-idx)
                           prefix (utils/format "%s%d_%s_%d_" (name body) idx (name status) dir-idx)
                           mask-prefix (utils/format "%s%d_%s_m_%d_" (name body) idx (name status) dir-idx)
                           directory (str "object/" (name body))
                           prefix-path (str directory "/" prefix)
                           mask-prefix-path (str directory "/" mask-prefix)
                           files (match-files prefix-path)
                           mask-files (match-files mask-prefix-path)]
                          {:status     status
                           :body       body
                           :direction  dir
                           :files      (sort files)
                           :mask-files (sort mask-files)}))
                valid-metas (transduce (comp (filter #(-> % :files not-empty)))
                                       (fn ([] {})
                                           ([acc] acc)
                                           ([acc e] (assoc-in acc [(:status e) (:body e) (:direction e)]
                                                              {:files      (:files e)
                                                               :mask-files (:mask-files e)})))
                                       metas)]
               (log/info (count valid-metas))
               (log/info valid-metas)
               valid-metas)))