(ns d-qqt.utils
    (:require [taoensso.timbre :as log]
              #?@(:clj  [[clojure.java.io :as io]
                         [clojure.edn :as edn]]
                  :cljs [[clojure.tools.reader.edn :as edn]
                         [goog.string :as gstring]
                         [goog.string.format]]))
  #?(:clj (:import [java.nio ByteBuffer]
                   [org.lwjgl.glfw GLFW]
                   [org.lwjgl.system MemoryUtil]
                   [org.lwjgl.stb STBImage])))

(def format
  #?(:clj format
     :cljs gstring/format))

(defn read-edn [fname callback]
      #?(:clj (some-> (str "public/" fname)
                      io/resource
                      slurp
                      edn/read-string
                      callback)
         :cljs (some-> (js/fetch fname)
                       (.then (fn [v] (.text v)))
                       (.then (fn [v] (log/info fname v)
                                  (-> v js->clj edn/read-string callback))))))

(defn read-file [fname]
      #?(:clj (some-> (str "public/" fname)
                      io/resource
                      slurp)
         :cljs (some-> (js/fetch fname)
                   (.then (fn [v] (.text v)))
                   (.then (fn [v] (log/info fname v)
                              (js->clj v))))))

(defn get-image [fname callback]
  #?(:clj  (let [is (io/input-stream (io/resource (str "public/" fname)))
                 ^bytes barray (with-open [out (java.io.ByteArrayOutputStream.)]
                                 (io/copy is out)
                                 (.toByteArray out))
                 *width (MemoryUtil/memAllocInt 1)
                 *height (MemoryUtil/memAllocInt 1)
                 *components (MemoryUtil/memAllocInt 1)
                 direct-buffer (doto ^ByteBuffer (ByteBuffer/allocateDirect (alength barray))
                                 (.put barray)
                                 (.flip))
                 decoded-image (STBImage/stbi_load_from_memory
                                 direct-buffer *width *height *components
                                 STBImage/STBI_rgb_alpha)
                 image {:data decoded-image
                        :width (.get *width)
                        :height (.get *height)}]
             (MemoryUtil/memFree *width)
             (MemoryUtil/memFree *height)
             (MemoryUtil/memFree *components)
             (callback image))
     :cljs (let [image (js/Image.)]
             (doto image
               (-> .-src (set! fname))
               (-> .-onload (set! #(callback {:data image
                                              :width image.width
                                              :height image.height})))))))

(defn get-size [game]
  #?(:clj  (let [*width (MemoryUtil/memAllocInt 1)
                 *height (MemoryUtil/memAllocInt 1)
                 _ (GLFW/glfwGetFramebufferSize ^long (:context game) *width *height)
                 w (.get *width)
                 h (.get *height)]
             (MemoryUtil/memFree *width)
             (MemoryUtil/memFree *height)
             [w h])
     :cljs [(-> game :context .-canvas .-clientWidth)
            (-> game :context .-canvas .-clientHeight)]))

(defn get-width [game]
  (first (get-size game)))

(defn get-height [game]
  (second (get-size game)))
