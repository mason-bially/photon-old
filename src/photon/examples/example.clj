(ns photon.examples.example
  "A list of ideal use cases and explanations. Not fully implemented"
  (require [photon.core :as photon]))

(def renderer
  "Systems wrap the core rendering functions and utilities that come with the library."
  (photon.make-system :name "A Test!" :size [800 600] :default true))

;; The default system lives in `photon/*system*`.

;; This function attaches a debug thread which prints fps for the current system.
(photon.debug-frames)

(def camera
  "Cameras are viewports and determine how the world is rendered."
  (photon.make-camera))

;; The main camera is the one the world is rendered from.
;; Alternatively you can set the :set-main key to true when making the camera.
(photon.set-main-camera camera)

;; Attach photon's simple input handler to the camera.
(photon.attach-input camera)

;; There are a number of different mesh building functions.
(def sphere-mesh
  "Parametric meshes can be built by mapping functions over ranges of numbers"
     (let [sphere-fn (fn [u v]
		       (let [cir (Math/sqrt (- 1.0 (Math/pow v 2)))
			     x (* cir (Math/cos u))
			     y (* cir (Math/sin u))
			     z v]
			 [x y z]))]
       (photon.mesh/parametric-shape)
       :u {:begin 0.0 :end (* Math/PI 4.0) :step (* Math/PI 0.125)}
       :v {:begin -1.0 :end 1.0 :step 0.125}
       :pos sphere-fn
       :norm sphere-fn))

