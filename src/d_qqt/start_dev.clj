(ns d-qqt.start-dev
  (:require [d-qqt.start :as start]
            [d-qqt.core :as c]
            [clojure.spec.test.alpha :as st]
            [clojure.spec.alpha :as s]
            [play-cljc.gl.core :as pc])
  (:import [org.lwjgl.glfw GLFW]
           [d_qqt.start Window]))

(defn start []
  (st/instrument)
  (let [window (start/->window)
        game (pc/->game (:handle window))]
    (start/start game window)))

(defn -main []
  (start))

