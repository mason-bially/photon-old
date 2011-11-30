(ns photon.renderable
  "Basic renderable helper"
  (use [photon.opengl.jogl]
	[photon.opengl.shaders]
	;[photon.opengl.buffers]
	))



(defn renderable [& {:keys [shader buffer] :as render}]
  render)

(defn render [renderable]
  (