(ns photon.matrix
  "Provides immutable style matrices through the javax.vecmath library, and wrapping them in lispy style functions."
  (import [javax.vecmath Matrix4d Vector3d Quat4d]))

;; Extend the java types with clojure.lang.seqable for easy use:
;(extend Matrix4d
;  clojure.lang.Seqable
;  {:seq (fn [this] (let [row0 (float-array 4)
;			 row1 (float-array 4)
;			 row2 (float-array 4)
;			 row3 (float-array 4)]
;		     (.getRow this 0 row0)
;		     (.getRow this 1 row1)
;		     (.getRow this 1 row2)
;		     (.getRow this 1 row3)
;		     (float-array
;		      (concat
;		       row0 row1 row2 row3))))})

;; This library provides the following capabilities wraped as described:
;;
;;   - Positions: These are represented by a `Vector3d` that is pointing from some reference point (which can be changed by multiplying by the correct matrix). I.e. these may be points, but they are treated like vectors.
;;   - Normals: These are represented by a normalized `Vector3d` or vectors.
;;   - Rotation: These are represented by a `Quat4d`, and can be turned into a `Matrix4d`. They may also be generated using `Vector3d` as axis.
;;   - Scaling: These can be represented by a `double`, can can be turnded into a `Matrix4d`.

;; #Operations

(defn mul
  "Multiplies an arbitrary number of objects"
  [x & args]
  (reduce (fn [r x] (.mul r x) r) (.clone x) args))

(defn add
  "Adds an arbitrary number of vertex objects"
  [x & args]
  (reduce (fn [r x] (.add r x) r) (.clone x) args))

(defn cross
  "Crosses two vectors"
  [x y]
  (let [w (new Vector3d x)]
    (.cross w x y)
    w))

(defn dot
  "Dots two vectors"
  [x y]
  (.dot x y))

(defn normalize
  "Normalizes a vector"
  [x]
  (let [w (new Vector3d x)]
    (.normalize w)
    w))

(defn vscale
  "Scales a vector"
  [x s]
  (let [w (new Vector3d x)]
    (.scale w s)
    w))

;; #Opengl translation
(defmulti ^{:doc "Returns an object for use with opengl"}
  to-opengl class)

(defmethod to-opengl Matrix4d [c]
	   (float-array [(.m00 c) (.m01 c) (.m02 c) (.m03 c)
			 (.m10 c) (.m11 c) (.m12 c) (.m13 c)
			 (.m20 c) (.m21 c) (.m22 c) (.m23 c)
			 (.m30 c) (.m31 c) (.m32 c) (.m33 c)]))

(defmethod to-opengl Object [o]
	   o)

;; #Vector and Positions

(defn vector
  ([]
     (new Vector3d))
  ([x y z]
     (new Vector3d x y z)))

;; #Matrix Creation

(defn identity
  []
  (doto (new Matrix4d)
    (.setIdentity)))

(defn matrix
  [m00 m01 m02 m03
   m10 m11 m12 m13
   m20 m21 m22 m23
   m30 m31 m32 m33]
  (new Matrix4d
       m00 m01 m02 m03
       m10 m11 m12 m13
       m20 m21 m22 m23
       m30 m31 m32 m33))

(defn projection
  "Makes a projection matrix from `near`, `far`, and the ratio of `width` and `height`."
  [near far width height]
  (let [n near, f far, w width, h height]
    (new Matrix4d (* 2.0 (/ n w)), 0.0            , 0.0                 , 0.0
       
                  0.0            , (* 2.0 (/ n h)), 0.0                 , 0.0
       
                  0.0            , 0.0            , (* -1.0 (/ (+ f n)
							       (- f n))), (/ (* -2.0 f n)
									    (- f n))
                  0.0            , 0.0            , -1.0                , 0.0)))

(defn translation
  "Make a translation matrix from either a `Vector3d`, `Point3d`, or [`x`, `y`, `z`]"
  ([^Vector3d vec]
     (doto (identity)
       (.setTranslation vec)))

  ([^double x ^double y ^double z]
     (doto (identity)
       (.setTranslation (new Vector3d x y z)))))

(defn scale
  "Make a scale matrix"
  [s]
  (doto (identity)
    (.setScale s)))

(defn transform
  "Make a general transform matrix from a rotation, position, and scale"
  ([vec quat scale]
     (new Matrix4d quat vec scale)))