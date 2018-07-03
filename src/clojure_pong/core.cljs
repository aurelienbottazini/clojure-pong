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

(defn drawGameShell [context]
  (let [width (:width (getCanvasDimensions (getCanvas)))
        height (:height (getCanvasDimensions (getCanvas)))]
    (doto context
      (aset "fillStyle" "black")
      (.fillRect 0 0 width height)
      (aset "fillStyle" "white")
      (aset "font" "40px monospace")
      (.fillText 0 (/ (* width 3) 8) 50)
      (.fillText 0 (/ (* width 5) 8) 50))))

;; define your app data so that it doesn't get over-written on reload
(defonce app-state (atom {:text "Hello world!"}))
(drawGameShell (getContext))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  (drawGameShell (getContext))
  (println "foo")
  (.focus (getCanvas)))
