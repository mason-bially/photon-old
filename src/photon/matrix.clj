(ns photon.matrix
  "Provides immutable style matrices through the javax.vecmath library, and wrapping them in lispy style functions."
  (import [javax.vecmath Matrix4d Vector3d Quat4d]))

;; This library provides the following capabilities wraped as described:
;;
;;   - Positions: These are represented by a `Vector3d` that is pointing from some reference point (which can be changed by multiplying by the correct matrix).
;;   - Normals: These are represented by a normalized `Vector3d`
;;   - Rotation: These are represented by a `Quat4d`, and can be turned into a `Matrix4d`. They may also be generated using `Vector3d` as axis.
;;   - Scaling: These can be represented by a `double`, can can be turnded into a `Matrix4d`.

;; #Multiplication rules

(defmulti ^{:doc "Multiplies two matrix objects"}
  mul* (fn [x y] [(class x) (class y)]))

(defn mul
  "Multiplies an arbitrary number of matrix objects"
  [x y & args]
  (mul (cons (mul* x y) args)))

(defmethod mul* [Matrix4d Matrix4d] [l r]
	   (let [return (new Matrix4d l)]
	     (. return Matrix4d/mul r)))

;; #Matrix Creation

(defn projection-matrix
  "Makes a projection matrix from `near`, `far`, and the ratio of `width` and `height`."
  [near far width height]
  (let [n near, f far, w width, h height]
    (new Matrix4d (* 2.0 (/ n w)), 0.0            , 0.0                , 0.0
       
                  0.0            , (* 2.0 (/ n h)), 0.0                , 0.0
       
                  0.0            , 0.0            , (/ (* -1.0 (+ f n))
						       (- f n))        , (/ (* -2.0 f n)
									    (- f n))
                  0.0            , 0.0            , -1.0               , 0.0)))

(defn translation-matrix
  "Make a translation matrix from either a `Vector3d`, `Point3d`, or [`x`, `y`, `z`]"
  ([^Vector3d vec]
     (. (new Matrix4d)
	Matrix4d/setTranslation vec))
  ([^double x ^double y ^double z]
     (. (new Matrix4d)
	Matrix4d/setTranslation (new Vector3d x y z))))

(defn transform-matrix
  "Make a general transform matrix from a rotation, position, and scale"
  ([vec quat scale]
     (new Matrix4d quat vec scale)))