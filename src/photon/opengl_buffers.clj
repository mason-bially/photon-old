(ns photon.opengl
  "#Buffers

This file handels vertex buffers, vertex arrays, attribute binding, and (element buffers?).")

;;; ##GL Constants

;; Buffer target enums
(defglconst gl4 gl-array-buffer)

;; Buffer data usage hints
(defglconst gl4 gl-static-draw)
(defglconst gl4 gl-dynamic-draw)

;; Buffer Draw Types
(defglconst gl4 gl-triangles)
(defglconst gl4 gl-triangle-strip)
(defglconst gl2 gl-quads)
(defglconst gl2 gl-quad-strip)
(defglconst gl4 gl-points)
(defglconst gl4 gl-lines)
(defglconst gl4 gl-line-strip)


;;; ##Vertex buffers

(defn make-attribute-buffers
  [binding-attributes]
  (let [len (count binding-attributes)
	buffers (int-array len)]
    (.glGenBuffers *gl* len buffers 0)
    (map (fn [gl-int attribute]
	   {:gl-type :buffer
	    :gl-int gl-int
	    :attribute attribute})
	 buffers (keys binding-attributes))))

(defn fill-attribute-buffers
  [buffers binding-data]
  (reduce (fn [reduction next]
	    (assoc reduction (next 0) (next 1)))
	  {}
	  (map (fn [[key value]]
		 (let [data (FloatBuffer/wrap (key binding-data))]
		   (doto *gl*
		     (.glBindBuffer gl-array-buffer (:gl-int value))
		     (.glBufferData gl-array-buffer (* (alength data) 4) data gl-static-draw)
		     (.glBindBuffer gl-array-buffer (:gl-int value))))
		 [key value])
	       buffers)))

(defn bind-attribute-buffers
  [buffers])

;;; ##Vertex arrays

(defn make-buffer-array
  []
  (let [vertex-array (int-array 1)]
    (.glGenVertexArrays *gl* 1 vertex-array 0)))

