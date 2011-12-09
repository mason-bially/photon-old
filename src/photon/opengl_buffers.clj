(ns photon.opengl
  "#Buffers
This file handels vertex buffers, vertex arrays, attribute binding, and (element buffers?).")

;;; ##GL Constants

;; Buffer target enums
(defglconst gl2 gl-array-buffer)

;; Buffer data usage hints
(defglconst gl2 gl-static-draw)
(defglconst gl2 gl-dynamic-draw)

;; Buffer Draw Types
(defglconst gl2 gl-triangles)
(defglconst gl2 gl-triangle-strip)
(defglconst gl2 gl-quads)
(defglconst gl2 gl-quad-strip)
(defglconst gl2 gl-points)
(defglconst gl2 gl-lines)
(defglconst gl2 gl-line-strip)

(defglconst gl2 gl-float)

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
  (vec (map (fn [buffer]
	 (let [data (FloatBuffer/wrap (float-array (get binding-data (:attribute buffer))))]
	   (doto *gl*
	     (.glBindBuffer gl-array-buffer (:gl-int buffer))
	     (.glBufferData gl-array-buffer (* (alength (.array data)) 4) data gl-static-draw)))
	 buffer)
       buffers)))

(defn bind-attribute-buffers
  [buffers]
  (vec (map (fn [buffer]
	      (let [attribute (get-attrib (opengl-str (:attribute buffer)))]
		(doto *gl*
		  (.glEnableVertexAttribArray attribute)
		  (.glBindBuffer gl-array-buffer (:gl-int buffer))
		  (.glVertexAttribPointer attribute 4 gl-float false 0 0))))
	    buffers)))



;;; ##Vertex arrays

(defn make-buffer-array
  []
  (let [vertex-array (int-array 1)]
    (.glGenVertexArrays *gl* 1 vertex-array 0)))

