(ns photon.examples.parametric-viewer
  (require [photon.app :as app]
           [incanter.core :as i])
  (use [photon.opengl]
       [clojure.contrib.def :only [defn-memo]])
  (import [javax.media.opengl GL GL4]
	  [java.nio FloatBuffer IntBuffer]
	  [java.awt.event KeyEvent]))

;;;; TODO
;; Generating functions (with buttons)
;; Lighting shader
;; Mouse movement
;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Constants and helpers ;;;
(def-gl-const gl-array-buffer)

(def-gl-const gl-static-draw)

(def-gl-const gl-float)

(def-gl-const gl-triangles)
(def-gl-const gl-triangle-strip)
(def-gl-const gl-quads)
(def-gl-const gl-quad-strip)
(def-gl-const gl-points)
(def-gl-const gl-lines)
(def-gl-const gl-line-strip)

(def-gl-const gl-line)
(def-gl-const gl-fill)
(def-gl-const gl-front-and-back)

(def-gl-const gl-ccw)
(def-gl-const gl-cw)

(def-gl-const gl-cull-face)

(def-gl-const gl-static-draw)

(def-gl-const gl-depth-test)

(defn cross [v1 v2] 
  "Returns the cross product vector for the 3D vectors v1 and v2."
  [ (- (* (v1 1) (v2 2)) (* (v1 2) (v2 1)))
    (- (* (v1 2) (v2 0)) (* (v1 0) (v2 2)))
    (- (* (v1 0) (v2 1)) (* (v1 1) (v2 0))) ])

(defn subtract [v1 v2]
  "Returns the subtraction of two vectors"
  (vec (map - v1 v2)))

(defn add [v1 v2]
  "Returns the addition of two vectors"
  (vec (map + v1 v2)))

;;;;;;;;;;;;;;;;;;;;
;;; Matrix Stuff ;;;
(def identity-matrix (i/matrix [[1 0 0 0]
				[0 1 0 0]
				[0 0 1 0]
				[0 0 0 1]]))

(defn make-projection-matrix [n f w h]
  (i/matrix [[(* 2.0 (/ n w)), 0.0, 0.0, 0.0]
	     [0.0, (* 2.0 (/ n h)), 0.0, 0.0]
	     [0.0, 0.0, (* -1.0 (/ (+ f n) (- f n))), (/ (* -2.0 f n) (- f n))]
	     [0.0, 0.0, -1.0, 0.0]]))

(defn make-translation-matrix [x y z]
  (i/matrix [[1.0, 0.0, 0.0, x]
	     [0.0, 1.0, 0.0, y]
	     [0.0, 0.0, 1.0, z]
	     [0.0, 0.0, 0.0, 1.0]]))

(defn make-rotation-x-matrix [rot]
  (let [s (i/sin rot)
	c (i/cos rot)]
    (i/matrix [[1.0, 0.0, 0.0, 0.0]
	       [0.0, c, (* -1.0 s), 0.0]
	       [0.0, s, c, 0.0]
	       [0.0, 0.0, 0.0, 1.0]])))

(defn make-rotation-y-matrix [rot]
  (let [s (i/sin rot)
	c (i/cos rot)]
    (i/matrix [[c, 0.0, s, 0.0]
	       [0.0, 1.0, 0.0, 0.0]
	       [(* -1.0 s), 0.0, c, 0.0]
	       [0.0, 0.0, 0.0, 1.0]])))

(defn make-rotation-z-matrix [rot]
  (let [s (i/sin rot)
	c (i/cos rot)]
    (i/matrix [[c, (* -1.0 s), 0.0, 0.0]
	       [s, c, 0.0, 0.0]
	       [0.0, 0.0, 1.0, 0.0]
	       [0.0, 0.0, 0.0, 1.0]])))

(defn gl-vector [ivector]
  "Convert the matrix to a flat array"
  (float-array (. ivector toArray)))

(defn gl-matrix [imatrix]
  "Convert the matrix to a flat array"
  (gl-vector (. imatrix vectorize)))

;;;;;;;;;;;;;;;;;;
;;; The Object ;;;
(def state-debug "For debugging"
     (ref nil))

;; It is notable that the state contains refs.
;; This is because I was experimenting with manipulating the application with many threads.
;; In hindsight making this a (agent {...}) would have been the better choice.
(defn make-state [] {;;Java classes
		     :canvas (ref nil)
		     :frame (ref nil)
		     ;;My state
		     :modelview-matrix (ref identity-matrix)
		     :projection-matrix (ref identity-matrix)
		     :lights (ref '[])
		     :current-scene (ref '[])
		     :future-scene (ref nil)
		     :last-scene-data (ref '[])
		     :detail (ref 1.0)
		     :shader (ref shaders/fixed-pipeline)})

;;;;;;;;;;;;;;;;;
;;; Debugging ;;;
(def frames-so-far (ref 0))
(defn kick-fps-watcher []
  "Starts a thread for watching the frames, store in a varible to stop when done."
  (doto (new Thread (proxy [Runnable] []
		   (run []
			(while true
			  (println @frames-so-far)
			  (dosync (ref-set frames-so-far 0))
			  (Thread/sleep 1000)))))
    (. start)))

;;;;;;;;;;;;;;
;;; Shapes ;;;
(def parametric-shape {})

(def plane (merge parametric-shape
	     {:u [0.0, (* Math/PI 2.0), (* Math/PI 0.125)]
	      :v [-1.0, 1.0, 0.125]
	      :pos (fn [u v]
		     (let [x u
			   y v
			   z 0.0]
		       [x y z]))}))

(def sphere (let [the-fn (fn [u v]
			   (let [x (* (i/sqrt (- 1.0 (i/pow v 2))) (i/cos u))
				 y (* (i/sqrt (- 1.0 (i/pow v 2))) (i/sin u))
				 z v]
			     [x y z]))]
	      (merge parametric-shape
		     {:u [0.0, (* Math/PI 2), (* Math/PI 0.125)]
		      :v [-1.0, 1.0, 0.125]
		      :pos the-fn
		      :norm the-fn})))

(def mobius (merge parametric-shape
	     {:u [0.0, (* Math/PI 4.0), (* Math/PI 0.125)]
	      :v [-1.0, 1.0, 0.125]
	      :pos (fn [u v]
		     (let [half-u (* 0.5 u)
			   craz (+ 1.0 (* 0.5 v (i/cos half-u)))
			   x (* craz (i/cos u))
			   y (* craz (i/sin u))
			   z (* 0.5 v (i/sin half-u))]
		       [x y z]))}))

(def snake (merge parametric-shape
	     {:v [(* Math/PI 0.0), (* Math/PI 2.0), (* Math/PI 0.125)]
	      :u [0.0, 1.0, 0.125]
	      :pos (fn [u v]
		     (let [craz (* (- 1.0 u) (+ 3.0 (i/cos v)))
			   u-2-pi (* 2.0 Math/PI u)
			   x (* craz (i/cos u-2-pi))
			   y (* craz (i/sin u-2-pi))
			   z (+ (* 6 u) (* (- 1.0 u) (i/sin v)))]
		       [x y z]))}))
;;;;;;;;;
;; generating functions

(defn make-shape-strip [point-fn u-data v+0 v+1]
  (reduce (fn [strip u]
	    (concat strip
		    (point-fn u v+1)
		    (point-fn u v+0)))
	  []
	  u-data))

(defn make-shape-data [point-fn u-data v-data]
  (reduce concat
	  (let [v+1-data (rest v-data)
		v+0-data (drop-last v-data)]
	    (map (fn [v+0 v+1]
		   (make-shape-strip point-fn u-data v+0 v+1))
		 v+0-data v+1-data))))

(defn make-normal [point-fn u-1 u u+1 v-1 v v+1]
  "Try to find the best normal by averaging cross products of nehiboring points"
  (let [middle (point-fn u v)
	;build the vectors from points
	vectorize (fn [u v]
		    (if (and u v)
		      (subtract (point-fn u v) middle)
		      nil))
	A (vectorize u-1 v) B (vectorize u v-1)
	C (vectorize u+1 v) D (vectorize u v+1)
	;build the valid quads, by changing the vectors into quads
	quads (remove nil?
		      (map (fn [[X Y]] ;destructing bind of a multipart state
			     (if (and X Y)
			       (vector X Y)
			       nil))
			   [[D A] [A B] [B C] [C D]]))	
	products (map (fn [[a b]]
			(cross (vec a) (vec b)))
		      quads)]
    (reduce add products)
    (nth products 3))) ;return the average

(defn make-normal-strip [point-fn u-data v-1 v+0 v+1 v+2]
  (let [u-1-data (drop-last 2 u-data)
	u+0-data (drop 1 (drop-last 1 u-data))
	u+1-data (drop 2 u-data)]
    (reduce (fn [strip [u-1 u u+1]]
	      (concat strip
		      (make-normal point-fn u-1 u u+1 v+0 v+1 v+2)
		      (make-normal point-fn u-1 u u+1 v-1 v+0 v+1)))
	    []
	    (map vector u-1-data u+0-data u+1-data))))

(defn make-normal-data [point-fn u-data v-data]
  (reduce concat
	  (let [v+2-data (drop 3 v-data)
		v+1-data (drop 2 (drop-last 1 v-data))
		v+0-data (drop 1 (drop-last 2 v-data))
		v-1-data (drop-last 3 v-data)]
	    (map (fn [v-1 v+0 v+1 v+2]
		   (make-normal-strip point-fn u-data v-1 v+0 v+1 v+2))
		 v-1-data v+0-data v+1-data v+2-data))))

;;;;;;;;;;;;;;;
;;; Buffers ;;;
(defn gen-buffers []
  "Generates the buffer objects"
  (let [buffers (int-array 2) ;0 = posistions, 1 = normals
	vertexs (int-array 1)]
    (locking *gl*
      (. *gl* glGenVertexArrays 1 vertexs 0)
      (. *gl* glGenBuffers 2 buffers 0)
      {:gl (get vertexs 0)
       :gl-pos (get buffers 0)
       :gl-norm (get buffers 1)})))

(defn buffer-data [buffer data]
  (let [data-buffer (FloatBuffer/wrap data)]
    (locking *gl*
      (doto *gl*
	(. glBindBuffer gl-array-buffer buffer)
	(. glBufferData gl-array-buffer (* (alength data) 4) data-buffer gl-static-draw)
	(. glBindBuffer gl-array-buffer 0)))))

(defn make-buffers [buffers shape detail]
  "Makes buffer objects containing the data for the shape"
  (let [u-in (:u shape)
	v-in (:v shape)
	u-range (assoc u-in 2 (* detail (u-in 2)))
	v-range (assoc v-in 2 (* detail (v-in 2)))
	u-data (concat (vector (- (u-range 0) (u-range 2)))
		       (apply range u-range)
		       (vector (u-range 1) (+ (u-range 1) (u-range 2))))
	v-data (concat (vector (- (v-range 0) (v-range 2)))
		       (apply range v-range)
		       (vector (v-range 1) (+ (v-range 1) (v-range 2))))
	u-count (- (count u-data) 2)
	v-count (- (count v-data) 2)
	strip-length (* 2 (+ u-count))]
    (let [point-fn (:pos shape)
	  positions (float-array (make-shape-data point-fn (drop 1 (drop-last u-data)) (drop 1 (drop-last v-data))))
	  norms (if (nil? (:norm shape))
		  (float-array (make-normal-data point-fn u-data v-data))
		  (float-array (make-shape-data (:norm shape) (drop 1 (drop-last u-data)) (drop 1 (drop-last v-data)))))]
      (merge buffers {:gl-draw-type gl-triangle-strip
		      :positions positions
		      :norms norms
		      :firsts (int-array (take v-count (iterate #(+ strip-length %) 0)))
		      :counts (int-array (take v-count (repeat strip-length)))
		      :size v-count}))))

(defn bind-vertex-array [buffers & {:keys [position normal]}]
  (doto *gl*
    (. glBindVertexArray (:gl buffers))
    (. glEnableVertexAttribArray position)
    (. glBindBuffer gl-array-buffer (:gl-pos buffers))
    (. glVertexAttribPointer position 3 gl-float false 0 0)
    (. glEnableVertexAttribArray normal)
    (. glBindBuffer gl-array-buffer (:gl-norm buffers))
    (. glVertexAttribPointer normal 3 gl-float false 0 0)
    (. glPolygonMode gl-front-and-back gl-fill)
    (. glBindVertexArray 0)))

(defn draw-buffers [buffers]
  (doto *gl*
    (. glPolygonMode gl-front-and-back gl-fill)
    (. glBindVertexArray (:gl buffers))
    (. glMultiDrawArrays (:gl-draw-type buffers) (:firsts buffers) 0 (:counts buffers) 0 (:size buffers))
    (. glBindVertexArray 0)))

;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Lighting and scene;;;
(def single-light '[6.0 6.0 -3.0, 1.0 1.0 1.0
		    0.0 0.0 0.0, 0.0 0.0 0.0
		    0.0 0.0 0.0, 0.0 0.0 0.0])

(def many-lights '[-6.0 3.0 -3.0, 1.0 0.0 0.0
		    0.0 3.0 -3.0, 0.0 1.0 0.0
		    6.0 3.0 -3.0, 0.0 0.0 1.0])

(defn build-scene [state scene lights]
  "Asynchronously builds the scene"
  (println "Building scene...")
  (dosync
   (if @(:future-scene state)
     (future-cancel @(:future-scene state)))
   (ref-set (:last-scene-data state) [scene lights])
   (ref-set (:future-scene state)
	    (future
	       {:scene (map (fn [scene-object]
			      (make-buffers (gen-buffers) scene-object @(:detail state))) scene)
		:lights lights}))))

(defn rebuild-scene [state]
  (apply build-scene (concat [state] @(:last-scene-data state))))

(defn finalize-scenes [state]
  "This will attempt to pull a future scene into the render loop"
  (dosync
   (let [the-future @(:future-scene state)]
     (if (and (future? the-future) (future-done? the-future))
       ;; The future is here!
       (let [new-scene @the-future]
	 (locking *gl*	   
	   (doseq [scene (:scene new-scene)]
	     (when (not (nil? scene))
	       (buffer-data (:gl-pos scene) (:positions scene))
	       (buffer-data (:gl-norm scene) (:norms scene))
	       (bind-vertex-array scene
				  :position (shaders/getAttrib @(:shader state) "position")
				  :normal (shaders/getAttrib @(:shader state) "normal")))))
	 (ref-set (:current-scene state) (:scene new-scene))
	 (ref-set (:lights state) (:lights new-scene))
	 (ref-set (:future-scene state) nil)
	 (println "Scene finalizing..."))))))

;;;;;;;;;;;;;;
;;; Shader ;;;
(def vertex-source "
     #version 330
     in vec3 position;
     in vec3 normal;

     out vec3 lit_color;

     uniform vec3 lights[6];
     uniform mat4 mvMatrix;
     uniform mat4 pMatrix;

     const vec4 cameraNorm = vec4(0.0, 0.0, -1.0, 0.0);
     const float specular = 120.0;
     void main()
     {
       vec4 rotated_normal = normalize(mvMatrix * vec4(normalize(-normal), 0.0));

       lit_color = vec3(0.2, 0.2, 0.2);

       for (int i = 0; i < 3; i++)
       {
         int index = i * 2;
         vec4 light_pos   = vec4(lights[index], 1.0);
         vec3 light_color = lights[index+1];
         vec4 light_norm  = normalize(light_pos);

         float lambert = max(0.0, dot(light_norm, rotated_normal));

         float vdotn = dot(light_norm, rotated_normal);
         vec4 reflection = (vdotn * rotated_normal * 2.0) - light_norm;
         float spec = max(0.0, pow(dot(reflection, cameraNorm), specular)) * clamp(ceil(vdotn), 0.0, 1.0);

         lit_color += (light_color * lambert) * 0.8 + (light_color + vec3(0.5, 0.5, 0.5)) * spec ;
       }

       gl_Position = pMatrix * mvMatrix * vec4(position, 1.0);
     }
     ")
(def fragment-source "
     #version 330
     in vec3 lit_color;

     out vec4 output_color;
     void main() 
     {
       output_color = clamp(vec4(lit_color, 1.0), 0.0, 1.0);
     }
     ")
    
;;;;;;;;;;;; debug
(def old-vertex-source (ref vertex-source))
(def old-fragment-source (ref fragment-source))
(defn shader-hoister [state]
  "Quick hack to hoist shaders easily by just redefining the sources"
  (if (not (and (= @old-fragment-source fragment-source) (= @old-vertex-source vertex-source)))
    (locking *gl*
      (do
	(let [new-shader (shaders/make :fragment fragment-source :vertex vertex-source)]
	  (if (shaders/valid? new-shader)
	    (dosync
	     ;; Just to be safe, rebind all of the vertex arrays
	     (doseq [scene-object (:scene state)]
	       (bind-vertex-array (:current-scene state)
				  :position (shaders/getAttrib new-shader "position")
				  :normal (shaders/getAttrib new-shader "normal")))
	     (println "Hoisted!")
	     (ref-set (:shader state) new-shader))
	    (println (shaders/format new-shader))))
	(dosync (ref-set old-vertex-source vertex-source)
		(ref-set old-fragment-source fragment-source))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Rendering Callbacks ;;;
(defn init [state canvas]
  (dosync (ref-set (:shader state) (shaders/make :fragment fragment-source :vertex vertex-source))
	  ;; The projection matrix will be reset in the first reshape callback
	  (ref-set (:modelview-matrix state) (make-translation-matrix 0.0 0.0 -10.0)))
  (doto *gl*
    (. setSwapInterval 1) ;vsync
    (. glEnable gl-depth-test)
    (. glEnable gl-cull-face)
    (. glClearColor 0.1 0.2 0.3 0.0))
  (build-scene state (vector sphere) single-light))

(defn destroy [state canvas]
  ())

(defn display [state canvas]
  ;; Pull in state changes that have happened since the last call
  (shader-hoister state)
  (finalize-scenes state)
  ;; Render
  (locking *gl*
    (doto *gl*
      (. glClear (bit-or (int GL/GL_COLOR_BUFFER_BIT) (int GL/GL_DEPTH_BUFFER_BIT))))
    (let [pmatrix (gl-matrix @(:projection-matrix state))
	  mvmatrix (gl-matrix @(:modelview-matrix state))]
      (shaders/with @(:shader state)
		    (shaders/bindMatrix4 "pMatrix" pmatrix)
		    (shaders/bindMatrix4 "mvMatrix" mvmatrix)
		    (shaders/bindVec3 "lights" (float-array @(:lights state)) 6)
		    ;; Flat shaded
		    (doseq [buffers @(:current-scene state)]
		      (draw-buffers buffers))
		    )))
  ;; Count for FPS
  (dosync (ref-set frames-so-far (+ @frames-so-far 1))))

(defn reshape [state canvas x y width height]
  (let [r (double (/ height width))]
    (println "GL_VENDOR: " (. *gl* glGetString GL/GL_VENDOR))
    (println "GL_RENDERER: " (. *gl* glGetString GL/GL_RENDERER))
    (println "GL_VERSION: " (. *gl* glGetString GL/GL_VERSION))
    (dosync
     (ref-set (:projection-matrix state) (make-projection-matrix 1.0 1000.0 1.0 r)))
    ))

(defn on-key [state k]
  "Takes a key and does action based off of it"
  (let [mv (:modelview-matrix state)]
    (cond
     (= k KeyEvent/VK_A) (dosync
			  (ref-set mv (i/mmult (make-translation-matrix 0.1 0.0 0.0) @mv)))
     (= k KeyEvent/VK_D) (dosync
			  (ref-set mv (i/mmult (make-translation-matrix -0.1 0.0 0.0) @mv)))
     (= k KeyEvent/VK_E) (dosync
			  (ref-set mv (i/mmult (make-translation-matrix 0.0 0.1 0.0) @mv)))
     (= k KeyEvent/VK_Q) (dosync
			  (ref-set mv (i/mmult (make-translation-matrix 0.0 -0.1 0.0) @mv)))
     (= k KeyEvent/VK_W) (dosync
			  (ref-set mv (i/mmult (make-translation-matrix 0.0 0.0 0.1) @mv)))
     (= k KeyEvent/VK_S) (dosync
			  (ref-set mv (i/mmult (make-translation-matrix 0.0 0.0 -0.1) @mv)))
     
     (= k KeyEvent/VK_DOWN) (dosync
			     (ref-set mv (i/mmult @mv (make-rotation-x-matrix (* 0.01 Math/PI)))))
     (= k KeyEvent/VK_UP) (dosync
			   (ref-set mv (i/mmult @mv (make-rotation-x-matrix (* -0.01 Math/PI)))))
     (= k KeyEvent/VK_COMMA) (dosync
			      (ref-set mv (i/mmult @mv (make-rotation-y-matrix (* 0.01 Math/PI)))))
     (= k KeyEvent/VK_PERIOD) (dosync
			       (ref-set mv (i/mmult @mv (make-rotation-y-matrix (* -0.01 Math/PI)))))
     (= k KeyEvent/VK_RIGHT) (dosync
			      (ref-set mv (i/mmult @mv (make-rotation-z-matrix (* 0.01 Math/PI)))))
     (= k KeyEvent/VK_LEFT) (dosync
			     (ref-set mv (i/mmult @mv (make-rotation-z-matrix (* -0.01 Math/PI)))))

     (= k KeyEvent/VK_P) (dosync
			  (alter (:detail state) #(* 2.0 %))
			  (rebuild-scene state))
     (= k KeyEvent/VK_O) (do (dosync
			  (alter (:detail state) #(/ % 2.0))
			  (rebuild-scene state)))
     
     (= k KeyEvent/VK_1) (do (build-scene state (vector sphere) single-light) (print "zoomba"))
     (= k KeyEvent/VK_2) (build-scene state (vector sphere) many-lights)
     (= k KeyEvent/VK_3) (build-scene state (vector mobius) single-light)
     (= k KeyEvent/VK_4) (build-scene state (vector mobius) many-lights)
     (= k KeyEvent/VK_5) (build-scene state (vector snake) single-light)
     (= k KeyEvent/VK_6) (build-scene state (vector snake) many-lights)
     (= k KeyEvent/VK_0) (build-scene state (vector plane) many-lights)
     )))

;;;;;;;;;;;;;;;;;;;;;;;;;

(defn -main []
  "Main function, builds canvas and hands it callbacks"
  (let [state (make-state)] ; Add shared state to all callbacks
    (dosync
     (ref-set state-debug state)
     (ref-set (:canvas state) 
	      (app/canvas   ; Allow for dynamic rebinding of functions by wrapping them in a lambda
	       :init    (fn [c]
			    (init state c))
	       :destroy (fn [c] 
			    (destroy state c))
	       :display (fn [c]
			    (display state c))
	       :reshape (fn [c x y w h]
			    (reshape state c x y w h))))
     (ref-set (:frame state)
	      (app/start @(:canvas state)
			 :name "Parametric Viewer"
			 :on-close (fn [frame] (System/exit 0)))))
    (doto @(:canvas state)
      (. addKeyListener (proxy [java.awt.event.KeyAdapter] []
			  (keyPressed [^KeyEvent e]
					(on-key state (. e getKeyCode))))))
    ))
