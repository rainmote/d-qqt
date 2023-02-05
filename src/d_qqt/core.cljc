(ns d-qqt.core
    (:require #?(:clj  [play-cljc.macros-java :refer [gl]
                        ]
                 :cljs [play-cljc.macros-js :refer-macros [gl math]]
                       [cljs.reader :refer [read-string]])
              [clojure.string]
              [d-qqt.character :as character]
              [d-qqt.move :as move]
              [d-qqt.utils :as utils]
              [play-cljc.gl.core :as c]
              [play-cljc.gl.entities-2d :as e]
              [play-cljc.transforms :as t]
              [taoensso.timbre :as log]
              [play-cljc.instances :as i]))

(defonce *state (atom {:mouse-x          0
                       :mouse-y          0
                       :mouse-button     nil
                       :pressed-keys     #{}
                       :x-velocity       0
                       :y-velocity       0
                       :player-x         0
                       :player-y         0
                       :can-jump?        false
                       :direction        :right
                       :character {}
                       :local-player {:img {}
                                      :status :walk
                                      :direction :down}
                       :player []
                       :player-images    {}
                       :player-image-key :walk1}))

(defn init [game]
      ;; allow transparency in images
      (gl game enable (gl game BLEND))
      (gl game blendFunc (gl game SRC_ALPHA) (gl game ONE_MINUS_SRC_ALPHA))
      (let [name "maomao"
            character-process (fn [v]
                                  (log/debug "character" v)
                                  (let [metas (character/load-meta v)
                                        callback (fn [{:keys [data width height file]}]
                                                   (let [file-part (clojure.string/split file #"/")
                                                         body (keyword (nth file-part 2))
                                                         filename-part (clojure.string/split (nth file-part 3) #"_")
                                                         status (keyword (nth filename-part 1))
                                                         direction (->> (nth filename-part 2)
                                                                        character/idx->direction)
                                                         ;;_ (log/info "load image finish " file (:default-idx v) status body direction width height)
                                                         ;;_ (log/info (.-src data))
                                                         ;;game (assoc game :tex-count (atom 0))
                                                         entity (assoc (c/compile game (e/->image-entity game data width height)) :width width :height height :file file)]
                                                     (swap! *state update-in [:character (:default-idx v) status body direction]
                                                            (comp vec conj)
                                                            entity)))]
                                    (log/info metas)
                                    (character/load-images game *state v metas callback)))
            get-character-fn (fn [player]
                                 (let [p (utils/myformat "object/frame/character/%s.edn" (:character player))]
                                      (utils/read-edn p character-process)))]
            (utils/read-edn (utils/myformat "object/player/%s.edn" name)
                             (fn [v]
                                 (log/info "player callback" v)
                                 (get-character-fn v))))
      ;; load images and put them in the state atom
      #_(doseq [[k path] {:walk1 "player_walk1.png"
                        :walk2 "player_walk2.png"
                        :walk3 "player_walk3.png"}]
             (utils/get-image path
                              (fn [{:keys [data width height]}]
                                 (log/info "origin data" data)
                                  (let [;; create an image entity (a map with info necessary to display it)
                                        entity (e/->image-entity game data width height)
                                        ;; compile the shaders so it is ready to render
                                        entity (c/compile game entity)
                                        ;; assoc the width and height to we can reference it later
                                        entity (assoc entity :width width :height height)]
                                       ;; add it to the state
                                       (swap! *state update :player-images assoc k entity))))))

(def screen-entity
  {:viewport {:x 0 :y 0 :width 0 :height 0}
   :clear    {:color [(/ 173 255) (/ 216 255) (/ 230 255) 1] :depth 1}})

(defn tick [game]
      (let [{:keys [local-player
                    character
                    pressed-keys
                    player-x
                    player-y
                    direction
                    player-images
                    player-image-key]
             :as   state} @*state
            [game-width game-height] (utils/get-size game)]
           (when (and (pos? game-width) (pos? game-height))
                 ;; render the blue background
             (c/render game (update screen-entity :viewport
                                    assoc :width game-width :height game-height))
                 ;; get the current player image to display
             (doseq [body character/all-body]
               (let [imgs (get-in character [10901 :walk body :down])]
                 (if (and (some? imgs) (-> imgs empty? not) (-> imgs first some?))
                   (let [img (first imgs)
                         offset (character/get-body-y-offset body)
                         x (+ 200 (- 20 (/ (:width img) 2)))
                         y  200]
                     (c/render game
                               (-> img
                                   (t/project game-width game-height)
                                   (t/translate (+ x (first offset))
                                                (+ y (second offset)))
                                   (t/scale (:width img) (:height img)))))
                   (log/info "imgs is empty,  body: " body))))
             (when-let [player (get player-images player-image-key)]
               (let [player-width (/ game-width 10)
                     player-height (* player-width (/ (:height player) (:width player)))]
                                ;; render the player
                 #_(c/render game
                             (-> player
                                 (t/project game-width game-height)
                                 (t/translate (cond-> player-x
                                                (= direction :left)
                                                (+ player-width))
                                              player-y)
                                 (t/scale (cond-> player-width
                                            (= direction :left)
                                            (* -1))
                                          player-height)))
                                ;; change the state to move the player
                 (swap! *state
                        (fn [state]
                          (->> (assoc state
                                      :player-width player-width
                                      :player-height player-height)
                               (move/move game)
                               (move/prevent-move game)
                               (move/animate game))))))))
      ;; return the game map
      game)

