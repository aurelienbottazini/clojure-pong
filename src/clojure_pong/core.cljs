(ns clojure-pong.core
    (:require ))

(enable-console-print!)

;; define your app data so that it doesn't get over-written on reload
(defonce app-state (atom {:text "Hello world!"
                          :playerScore 0
                          :computerScore 0 }))

(defn getCanvasDimensions [canvas]
  (let [width (.-offsetWidth canvas)
        height (.-offsetHeight canvas)]
    { :width width, :height height}))

(defn getCanvas []
  (.. js/document (getElementById "canvas")))

(defn getContext []
  (.getContext (getCanvas) "2d"))

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
      (.fillText (:playerScore @app-state) (/ width 4) 30)
      (.fillText (:computerScore @app-state) (/ width 2) 30))))

(defn gameLoop []
  (js/setInterval #(drawGameShell (getContext)) 100))

(defonce game-loop (gameLoop))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  (.focus (getCanvas)))
