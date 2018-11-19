(ns clojure-pong.core
  (:require [clojure.browser.event :as cevent]
            [clojure.browser.dom :as cdom]
            [goog.dom.animationFrame :as ganimation]))

(enable-console-print!)

(def canvas
  (cdom/get-element "canvas"))

(def canvasDimensions
  (let [width (.-offsetWidth canvas)
        height (.-offsetHeight canvas)]
    { :width width, :height height}))

(def context (.getContext canvas "2d"))

(defonce app-state (atom {:playerScore 0
                          :computerScore 0
                          :fps 60
                          :interval (/ 1000 60)
                          :speed 3
                          :speed-mult 1.3
                          :computer-speed 0.75
                          :last-update 0}))

(defonce player-paddle (atom {:x 20
                              :y (- (/ (:height canvasDimensions) 2) (/ 40 2))
                              :width 10
                              :velocity 5
                              :height 40}))

(defonce computer-paddle (atom {:x (- (:width canvasDimensions) 20 10)
                                :y  (- (/ (:height canvasDimensions) 2) (/ 40 2))
                                :width 10
                                :height 40}))

(defn draw-game-shell [context]
  (let [width (:width canvasDimensions)
        height (:height canvasDimensions)]
    (doto context
      (aset "fillStyle" "black")
      (.fillRect 0 0 width height)
      (aset "fillStyle" "white")
      (aset "font" "24px monospace")
      (.fillText (:playerScore @app-state) (* width (/ 3 8)) 30)
      (.fillText (:computerScore @app-state) (* width (/ 5 8)) 30))))

(defn draw-paddles [context]
  (doto context
    (aset "fillStyle" "white")
    (.fillRect (:x @player-paddle) (:y @player-paddle) (:width @player-paddle) (:height @player-paddle))
    (.fillRect (:x @computer-paddle) (:y @computer-paddle) (:width @computer-paddle) (:height @computer-paddle))))

(defn ball-starting-position []
  (let [width (:width canvasDimensions)
        height (:height canvasDimensions)
        startingY (- (/ height 2) (/ 10 2))
        startingX (- (/ width 2) (/ 10 2))]
    { :x startingX :y startingY}))

(defn rand-velocity [] (let [min-angle -30
                             max-angle 30
                             angle (+ min-angle (js/Math.floor (* (rand) (- max-angle (+ 1 min-angle)))))
                             radian (/ js/Math.PI 180)
                             x-velocity (* (:speed @app-state) (js/Math.cos (* angle radian)))
                             rand-x-velocity-direction (if (< 0.5 (rand))
                                                         (unchecked-negate x-velocity)
                                                         x-velocity)
                             y-velocity (* (:speed @app-state) (js/Math.sin (* angle radian)))]
                         {:x-velocity rand-x-velocity-direction :y-velocity y-velocity}))

(defonce velocity (atom (rand-velocity)))

(defonce ball (atom { :x (:x (ball-starting-position)), :y (:y (ball-starting-position)), :width 10, :height 10 }))

(defn reset-ball []
  (swap! ball assoc :x (:x (ball-starting-position)) :y (:y (ball-starting-position)))
  (reset! velocity (rand-velocity)))

(defn intersect [a b]
  (and (> (+ (:y a) (:height a)) (:y b))
       (< (:y a) (+ (:y b) (:height b)))
       (> (+ (:x a) (:width a)) (:x b))
       (< (:x a) (+ (:x b) (:width b)))))

(add-watch ball :ball (fn [_k _r _o n]
                            (cond
                              (intersect n @player-paddle) (do (swap! velocity update-in [:x-velocity] js/Math.abs)
                                                               (if (< (:x-velocity @velocity) 3)
                                                                 (swap! velocity update-in [:x-velocity] * (:speed-mult @app-state))))
                              (intersect n @computer-paddle) (swap! velocity update-in [:x-velocity] (comp unchecked-negate js/Math.abs))
                              (< (:x n) 0) (do (swap! app-state update-in [:computerScore] inc) (reset-ball))
                              (> (:x n) (:width canvasDimensions)) (do (swap! app-state update-in [:playerScore] inc) (reset-ball))
                              (< (:y n) 0) (swap! velocity update-in [:y-velocity] unchecked-negate)
                              (> (+ (:y n) (:height @ball)) (:height canvasDimensions)) (swap! velocity update-in [:y-velocity] unchecked-negate)
                              :else nil)))

(defn draw-ball [context]
  (doto context
    (aset "fillStyle" "white")
    (.fillRect (:x @ball) (:y @ball) (:width @ball) (:height @ball))))

(defn update-ball [interval-percentage]
  (swap! ball assoc
         :x (+ (:x @ball) (* interval-percentage (:x-velocity @velocity)))
         :y (+ (:y @ball) (* interval-percentage (:y-velocity @velocity)))))

(defn update-computer-paddle [_interval-percentage]
  (let [ball-y (+ (:y @ball) (/ (:height @ball) 2))
        computer-y (+ (:y @computer-paddle) (/ (:height @computer-paddle) 2))]
    (cond
      (< (js/Math.abs (- ball-y computer-y)) (/ (:height @ball) 2)) (js/console.log "Computer does not need to move")
      (< ball-y computer-y) (swap! computer-paddle update-in [:y] #(- %1 (:computer-speed @app-state)))
      :else (swap! computer-paddle update-in [:y] #(+ (:computer-speed @app-state) %1)))))

(defn update-game [interval-percentage]
  (do
    (update-ball interval-percentage)
    (update-computer-paddle interval-percentage)))

(def animationTask
    (ganimation/createTask #js {
                                :measure (fn []
                                          (let [current-time (.getTime (js/Date.))
                                                time-delta (- current-time (:last-update @app-state))
                                                interval-percentage (/ time-delta (:interval @app-state))]
                                            (update-game interval-percentage)
                                            (swap! app-state assoc :last-update current-time)
                                            (draw-game-shell context)
                                            (draw-paddles context)
                                            (draw-ball context)
                                            (animationTask)))
                                :mutate  identity }))

(defn reset-scores []
  (swap! app-state assoc :playerScore 0 :computerScore 0))

(cevent/listen canvas "touchstart" #(let [touchY (- (.-clientY %1) (.-offsetTop canvas))]
                                        (js/console.log touchY (:y @player-paddle))
                                        (swap! player-paddle assoc :y touchY)))

(cevent/listen canvas "keydown" #(let [key-pressed (.-key %1)
                                       height (:height canvasDimensions)
                                       atFarEdge (> (:y @player-paddle) (- height 40))
                                       atLowerEdge (< (:y @player-paddle) 0)]
                                   (cond
                                     (= key-pressed "ArrowUp") (if-not atLowerEdge (swap! player-paddle update-in [:y] - (:velocity @player-paddle)))
                                     (= key-pressed "ArrowDown") (if-not atFarEdge (swap! player-paddle update-in [:y] + (:velocity @player-paddle)))
                                     (= key-pressed "r") (do (reset-ball) (reset-scores))
                                     :else (js/console.log "Key not used by game" key-pressed))))

(defn gameLoop []
  (animationTask))

(gameLoop)

(.focus canvas)

(defn on-js-reload []
  (.focus canvas))
