(ns d-qqt.macros
    (:require
      [play-cljc.gl.core :as c]
      #?(:clj [clojure.java.io :as io])))

#?(:clj (defmacro read-file [fname]
                  (some-> (str "public/" fname)
                          io/resource
                          slurp)))

#?(:clj
   (defmacro
     load-all-paths [dir]
     (let [root (some-> (str "public/" dir)
                        io/resource
                        io/file)]
          (some->> root
                   file-seq
                   (filter #(.isFile %))
                   (mapv (fn [f]
                             (-> root
                                 (.toURI)
                                 (.relativize (.toURI f))
                                 (.getPath))))))))

