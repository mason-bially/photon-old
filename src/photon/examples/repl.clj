(ns photon.examples.repl
  "A repl helper file"
  (require [photon.app :as app]
	   [photon.matrix :as matrix])
  (use [photon.opengl]))

;;; #Debug tools

;; Frame watcher
(def frames-so-far (ref 0))
(defn kick-fps-watcher []
  "Starts a thread for watching frames, store in a varible to stop when done."
  (doto (new Thread (proxy [Runnable] []
		   (run []
			(while true
			  (println @frames-so-far)
			  (dosync (ref-set frames-so-far 0))
			  (Thread/sleep 1000)))))
    (. start)))


;;; #Actual app

(def state (agent {}))

(defn app-init [drawable]
  (with-gl (app/gl-context drawable)
    (doto *gl*
      (. setSwapInterval 1))
    (send state assoc :scene [])))
  
(defn app-display [drawable]
  (with-gl (app/gl-context drawable)
    (app-shader-hoister)
    (doto *gl*
      (. glClearColor 0.2 0.2 0.4 0.0)
      (. glClear gl-color-buffer-bit))
  (dosync (alter frames-so-far + 1))))

(defn app-dispose [drawable]
  (with-gl (app/gl-context drawable)
    ()))

(defn app-reshape [drawable x y w h]
  (with-gl (app/gl-context drawable)
    (send state assoc :proj-matrix (projection-matrix 1.0 100.0 1.0 (/ h w)))
    (println (@test-fn))
    ))

(defn kick-app
  "Builds the canvas, a basic listener, and lambda wrapped function calls, to allow for dynamic redefinition. Returns AWT frame object."
  []
  (let [canvas (app/canvas)]
    (app/attach-gl-listener canvas
			    :init #'app-init
			    :display #'app-display
			    :dispose #'app-dispose
			    :reshape #'app-reshape)
    (app/start canvas)))
