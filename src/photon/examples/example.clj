(ns photon.examples.example
  (require [photon.core :as photon]))

(def renderer (photon.make-system :name "A Test!" :size [800 600] :default true))

(def camera (photon.make-camera :default true))

(photon.attach-input camera)

(def sphere-mesh
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


     