(ns clojure-pong.core
  (:require [clojure.browser.event :as cevent]
            [clojure.browser.dom :as cdom]))


(enable-console-print!)

(defn getCanvasDimensions [canvas]
  (let [width (.-offsetWidth canvas)
        height (.-offsetHeight canvas)]
    { :width width, :height height}))

(defn getCanvas []
  (cdom/get-element "canvas"))

(defn getContext []
  (.getContext (getCanvas) "2d"))

;; define your app data so that it doesn't get over-written on reload
(defonce app-state (atom {:playerScore 0
                          :computerScore 0
                          :playerPaddleY 0}))

(defonce playerVelocity (atom 5))

(cevent/listen (getCanvas) "keydown" #(let [key-pressed (.-key %1)
                                            canvasDimensions (getCanvasDimensions (getCanvas))
                                            height (:height canvasDimensions)
                                            atFarEdge (> (:playerPaddleY @app-state) (- height 40))
                                            atLowerEdge (< (:playerPaddleY @app-state) 0)]
                                        (cond
                                          (= key-pressed "ArrowUp") (if-not atLowerEdge (swap! app-state update-in [:playerPaddleY] - @playerVelocity))
                                          (= key-pressed "ArrowDown") (if-not atFarEdge (swap! app-state update-in [:playerPaddleY] + @playerVelocity))
                                          :else (js/console.log "Key not used by game" key-pressed))))

(defn drawGameShell [context]
  (let [canvasDimensions (getCanvasDimensions (getCanvas))
        width (:width canvasDimensions)
        height (:height canvasDimensions)]
    (doto context
      (aset "fillStyle" "black")
      (.fillRect 0 0 width height)
      (aset "fillStyle" "white")
      (aset "font" "24px monospace")
      (.fillText (:playerScore @app-state) (* width (/ 3 8)) 30)
      (.fillText (:computerScore @app-state) (* width (/ 5 8)) 30))))

(defn drawPaddles [context]
  (let [canvasDimensions (getCanvasDimensions (getCanvas))
        width (:width canvasDimensions)
        height (:height canvasDimensions)
        startingY (- (/ height 2) (/ 40 2))]
    (doto context
      (aset "fillStyle" "white")
      (.fillRect 20 (:playerPaddleY @app-state) 10 40)
      (.fillRect (- width 20 10) startingY 10 40))))

(defonce ball-position (let [canvasDimensions (getCanvasDimensions (getCanvas))
                             width (:width canvasDimensions)
                             height (:height canvasDimensions)
                             startingY (- (/ height 2) (/ 10 2))
                             startingX (- (/ width 2) (/ 10 2))
                             min-angle -30
                             max-angle 30
                             angle (+ min-angle (js/Math.floor (* (rand) (- max-angle (+ 1 min-angle)))))
                             radian (/ js/Math.PI 180)
                             speed 1
                             x-velocity (* speed (js/Math.cos (* angle radian)))
                             left-or-right-x-velocity (if (< 0.5 (rand))
                                                        (* -1 x-velocity)
                                                        x-velocity)
                             y-velocity (* speed (js/Math.sin (* angle radian)))
                             ]
                         (atom { :x startingX, :y startingY, :angle angle , :x-velocity left-or-right-x-velocity, :y-velocity y-velocity })))

;(add-watch ball-position :log (fn [k r o n] (println n)))

(defn drawBall [context]
    (doto context
      (aset "fillStyle" "white")
      (.fillRect (:x @ball-position) (:y @ball-position) 10 10)))

(defn updateBall []
  (do
    (swap! ball-position assoc :x (+ (:x @ball-position) (:x-velocity @ball-position)) :y (+ (:y @ball-position) (:y-velocity @ball-position)))))

(defn updateBoard []
  (do
    (updateBall)))

(defn gameLoop []
  (js/setInterval #(do
                     (drawGameShell (getContext))
                     (drawPaddles (getContext))
                     (drawBall (getContext))
                     (updateBoard))) 25)

(defonce game-loop (gameLoop))

(.focus (getCanvas))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  (.focus (getCanvas)))
