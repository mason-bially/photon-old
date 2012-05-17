(ns photon.core
  (require [photon.app :as app]
           [photon.matrix :as matrix]
           [photon.opengl :as opengl])
  (use [clojure.core.incubator :only (defmacro-)])
  (import [java.awt.event KeyEvent]))

(def ^{:dynamic true}
  *system*
  "The current default system"
  nil)

(defmacro- make-app-with-gl [drawable & body]
  `(opengl/with-gl (app/gl-context ~drawable)
     ~@body))

(defn- make-app-init
  [state]
  (fn [drawable]
    (make-app-with-gl drawable
      (.setSwapInterval opengl/*gl* 1))))

(defn- make-app-display
  [state]
  (fn [drawable]
    (make-app-with-gl drawable
      ())))

(defn- make-app-dispose
  [state]
  (fn [drawable]
    (make-app-with-gl drawable
      ())))

(defn- make-app-reshape
  [state]
  (fn [drawable x y w h]
    (make-app-with-gl drawable
      ())))

(defn make-system
  "Makes a generic system around all the namespaces. Includes a rendering system and numerous utility options."
  [& {:keys [name size default]
      :or {name "A Photon App" size '[800 600] default true}}]
  (let [state (agent '{})
        canvas (app/canvas)
        frame (app/start canvas :name name :size size)]
    (app/attach-gl-listener canvas
                            :init (make-app-init state)
                            :display (make-app-display state)
                            :dispose (make-app-dispose state)
                            :reshape (make-app-reshape state))
    (-> '{}
        (assoc :state state)
        (assoc :app-canvas canvas)
        (assoc :app-frame frame)
        ((fn [x]
           (when default
             (alter-var-root (var *system*) #(identity %2) x))))
        )
    )
  )

(defmacro with-system
  "Bind the `*system*` var."
  [system & body]
  `(binding [*system* ~system]
     ~@body))

;;; #Camera ;;;;

;;; #Utilities ;;;;

;;; #Debuggers ;;;;

(defn debug-framecount
  "Attaches a frame watcher to thread to debug with"
  [& {:keys [system] :or {system *system*}}]
  (let [state (:state system)]
    ; Intializes state
    (send state assoc :debug-framecount-count (ref 0))
    ; Counts frames, with attached gl-listener
    (app/attach-gl-listener (:app-canvas system)
                            :display (fn [drawable]
                                       (if (agent-error state)
                                         (println (agent-error state))
                                         (send state (fn [dict-state]
                                                       (let [count (:debug-framecount-count @dict-state)]
                                                         (dosync (alter count + 1))))))))
    ; Push thread
;    (let [thread
;          (new Thread
;               (proxy [Runnable] []
;                 (run []
;                   (while true
;                     (let [count (:debug-framecount-count (:state @state))]
;                       (println @count)
;                       (dosync (ref-set count 0))
;                       (Thread/sleep 1000))))))]
;      (send state assoc :debug-framecount-thread thread))
  ))