(ns clojure-pong.core
    (:require ))

(enable-console-print!)

(defn getCanvasDimensions [canvas]
  (let [width (.-offsetWidth canvas)
        height (.-offsetHeight canvas)]
    { :width width, :height height}))

(defn getCanvas []
  (.. js/document (getElementById "canvas")))

(defn getContext []
  (.getContext (getCanvas) "2d"))

;; define your app data so that it doesn't get over-written on reload
(defonce app-state (atom {:playerScore 0
                          :computerScore 0
                          :playerPaddleY 0}))

(defonce playerVelocity (atom 1))

(add-watch app-state :changeVelocity  (fn [_f _k _r o _n] (let [canvasDimensions (getCanvasDimensions (getCanvas))
                                                            height (:height canvasDimensions)
                                                            oldY (:playerPaddleY o)]
                                                         (do
                                                           (if (> oldY (- height 40))
                                                             (swap! playerVelocity unchecked-negate))
                                                           (if (< oldY 0)
                                                             (swap! playerVelocity unchecked-negate))))))


(defn drawGameShell [context]
  (let [canvasDimensions (getCanvasDimensions (getCanvas))
        width (:width canvasDimensions)
        height (:height canvasDimensions)]
    (doto context
      (aset "fillStyle" "black")
      (.fillRect 0 0 width height)
      (aset "fillStyle" "white")
      (aset "font" "24px monospace")
      ;(.fillText (:playerScore @app-state) (* width (/ 3 8)) 50)
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

(defn updateBoard []
  (do
    (swap! app-state update-in [:playerPaddleY] + @playerVelocity)))

(defn gameLoop []
  (js/setInterval #(do
                     (drawGameShell (getContext))
                     (drawPaddles (getContext))
                     (updateBoard)
                       )) 100)

(defonce game-loop (gameLoop))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  (.focus (getCanvas)))
