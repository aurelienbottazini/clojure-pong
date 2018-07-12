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
                          }))

(defonce playerVelocity (atom 5))

(defonce player-paddle (atom {:x     20
                              :y  (- (/ (:height (getCanvasDimensions (getCanvas))) 2) 20)
                              :width  10
                              :height 40}))

(defonce computer-paddle (atom {
                                :x (- (:width (getCanvasDimensions (getCanvas))) 20 10)
                                :y  (- (/ (:height (getCanvasDimensions (getCanvas))) 2) 20)
                                :width 10
                                :height 40
                                }))


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
      (.fillRect (:x @player-paddle) (:y @player-paddle) (:width @player-paddle) (:height @player-paddle))
      (.fillRect (:x @computer-paddle) (:y @computer-paddle) (:width @computer-paddle) (:height @computer-paddle)))))


(defn ball-starting-position []
  (let [canvasDimensions (getCanvasDimensions (getCanvas))
        width (:width canvasDimensions)
        height (:height canvasDimensions)
        startingY (- (/ height 2) (/ 10 2))
        startingX (- (/ width 2) (/ 10 2))]
    { :x startingX :y startingY})
  )

(defn gen-velocity [] (let [
                        min-angle -30
                        max-angle 30
                        angle (+ min-angle (js/Math.floor (* (rand) (- max-angle (+ 1 min-angle)))))
                        radian (/ js/Math.PI 180)
                        speed 0.4
                        x-velocity (* speed (js/Math.cos (* angle radian)))
                        left-or-right-x-velocity (if (< 0.5 (rand))
                                                   (unchecked-negate x-velocity)
                                                   x-velocity)
                        y-velocity (* speed (js/Math.sin (* angle radian)))
                        ]
                     {:x-velocity left-or-right-x-velocity :y-velocity y-velocity}))

(defonce velocity (atom (gen-velocity)))

(defonce ball-position (atom { :x (:x (ball-starting-position)), :y (:y (ball-starting-position)), :width 10, :height 10 }))

(defn reset-ball []
  (swap! ball-position assoc :x (:x (ball-starting-position)) :y (:y (ball-starting-position)))
  (reset! velocity (gen-velocity)))

(defn intersect [a b]
  (and (> (+ (:y a) (:height a)) (:y b))
       (< (:y a) (+ (:y b) (:height b)))
       (> (+ (:x a) (:width a)) (:x b))
       (< (:x a) (+ (:x b) (:width b)))))

(add-watch ball-position :velocity (fn [_k _r _o n]
                                       (cond
                                         (intersect n @player-paddle) (do
                                                                        (swap! velocity update-in [:x-velocity] unchecked-negate)
                                                                        (swap! velocity update-in [:x-velocity] #(* 1.3 %1))
                                                                        )
                                         (intersect n @computer-paddle) (swap! velocity update-in [:x-velocity] unchecked-negate)
                                         (< (:x n) 0) (do (swap! app-state update-in [:computerScore] inc) (reset-ball))
                                         (> (:x n) (:width (getCanvasDimensions (getCanvas)))) (do (swap! app-state update-in [:playerScore] inc) (reset-ball))
                                         (< (:y n) 0) (swap! velocity update-in [:y-velocity] unchecked-negate)
                                         (> (+ (:y n) (:height @ball-position)) (:height (getCanvasDimensions (getCanvas)))) (swap! velocity update-in [:y-velocity] unchecked-negate)
                                         :else nil)))


(defn drawBall [context]
  (doto context
    (aset "fillStyle" "white")
    (.fillRect (:x @ball-position) (:y @ball-position) (:width @ball-position) (:height @ball-position))))

(defn updateBall []
  (do
    (swap! ball-position assoc :x (+ (:x @ball-position) (:x-velocity @velocity)) :y (+ (:y @ball-position) (:y-velocity @velocity)))))

(defn update-computer-paddle []
  (let [ball-y (+ (:y @ball-position) (/ (:height @ball-position) 2))
        computer-y (+ (:y @computer-paddle) (/ (:height @computer-paddle) 2))]
    (cond
      (< ball-y computer-y) (swap! computer-paddle update-in [:y] #(- %1 0.15))
      (> ball-y computer-y) (swap! computer-paddle update-in [:y] #(+ 0.15 %1))
      :else nil)))

(defn updateBoard []
  (do
    (updateBall)
    (update-computer-paddle)
    ))

(defn gameLoop []
  (js/setInterval #(do
                     (drawGameShell (getContext))
                     (drawPaddles (getContext))
                     (drawBall (getContext))
                     (updateBoard))) 25)


(cevent/listen (getCanvas) "keydown" #(let [key-pressed (.-key %1)
                                            canvasDimensions (getCanvasDimensions (getCanvas))
                                            height (:height canvasDimensions)
                                            atFarEdge (> (:y @player-paddle) (- height 40))
                                            atLowerEdge (< (:y @player-paddle) 0)]
                                        (cond
                                          (= key-pressed "ArrowUp") (if-not atLowerEdge (swap! player-paddle update-in [:y] - @playerVelocity))
                                          (= key-pressed "ArrowDown") (if-not atFarEdge (swap! player-paddle update-in [:y] + @playerVelocity))
                                          (= key-pressed "r") (reset-ball)
                                          :else (js/console.log "Key not used by game" key-pressed))))


(defonce game-loop (gameLoop))

(.focus (getCanvas))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  (.focus (getCanvas)))
