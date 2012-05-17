(ns photon.examples.dungeon
  (require [photon.app :as app]
	   [photon.matrix :as matrix])
  (use [photon.opengl])
  (import [java.awt.event KeyEvent]))

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

;; Shader
(defshader core-shader
  :attributes {:Position :vec4
	       :Normal :vec4}
  :vertex-uniforms {:mMatrix :mat4
		    :vMatrix :mat4
		    :pMatrix :mat4}
  :vertex
"
 #version 330
 in vec4 Position;
 in vec4 Normal;

 out vec4 fragNormal;
 out vec4 fragPosition;

 uniform mat4 mMatrix;
 uniform mat4 vMatrix;
 uniform mat4 pMatrix;

 void main()
 {
   vec4 rotatedNormal = normalize(vMatrix * mMatrix * vec4(normalize(-Normal.xyz), 0.0));

   vec4 vertexPosition = mMatrix * Position;

   fragPosition = vertexPosition;
   fragNormal = rotatedNormal;
   gl_Position = pMatrix * vMatrix * vertexPosition;
 }
"
  :fragment-uniforms {:cameraPos :vec4}
  :fragment
"
 #version 330
 in vec4 fragNormal;
 in vec4 fragPosition;

 out vec4 output_color;

 uniform vec4 cameraPos;

 const vec4 light = vec4(0, 1.0, -1.0, 1.0);
 const vec3 light_color = vec3(1.0, 0.0, 1.0);

 const vec4 cameraNorm = vec4(0.0, 0.0, -1.0, 0.0);
 const float specular = 120.0;

 // noise constants
 const int octives = 20;
 const float d_persistence = 0.27;
 const float d_smoothness = 2.0;
 const float pi = 3.1415927;

 float cosMix(float a, float b, float x)
 {
   float f = ((1.0-cos(x * pi)) * 0.5);
 
   return a*(1.0-f) + b*f;
 }
 
 float Noise(int x, int y, int z)
 {
   int n = x + (y * 63311) + (z * 553105243);
   n = (n<<13) ^ n;
   return ( 1.0 - float((n * (n*n*17747 + 104729) + 1376312589) & 0x7fffffff) / 1073741824.0);
 }
 
 float SmoothNoise(int x, int y, int z)
 {
   return Noise(x, y, z);
 }

 float InterpolateNoise(float x, float y, float z)
 {
   int int_x = int(floor(x));
   float frac_x = x - float(int_x);

   int int_y = int(floor(y));
   float frac_y = y - float(int_y);

   int int_z = int(floor(z));
   float frac_z = z - float(int_z);

   float v1 = SmoothNoise(int_x    , int_y    , int_z    );
   float v2 = SmoothNoise(int_x + 1, int_y    , int_z    );
   float v3 = SmoothNoise(int_x    , int_y + 1, int_z    );
   float v4 = SmoothNoise(int_x + 1, int_y + 1, int_z    );
   float v5 = SmoothNoise(int_x    , int_y    , int_z + 1);
   float v6 = SmoothNoise(int_x + 1, int_y    , int_z + 1);
   float v7 = SmoothNoise(int_x    , int_y + 1, int_z + 1);
   float v8 = SmoothNoise(int_x + 1, int_y + 1, int_z + 1);

   float i1 = cosMix(v1, v2, frac_x);
   float i2 = cosMix(v3, v4, frac_x);
   float j1 = cosMix(i1, i2, frac_y);

   float i3 = cosMix(v5, v6, frac_x);
   float i4 = cosMix(v7, v8, frac_x);
   float j2 = cosMix(i3, i4, frac_y);

   return cosMix(j1, j2, frac_z);
 }

 float Perlin(float x, float y, float z, float smoothness, float persistence)
 {
   float total = 0.0;

   float frequency = 1.0;
   float amplitude = 1.0;

   for (int i = 0; i < octives; i++)
   {
     frequency = frequency * smoothness;
     amplitude = amplitude * persistence;

     total = total + InterpolateNoise(x * frequency, y * frequency, z * frequency) * amplitude;
   }

   // -0.5 to 0.5
   return total;
 }

 vec3 Colorize(float noise)
 {
   float unitNoise = noise + 0.5;
   float g = unitNoise * 20.0;

   float bumps = g - floor(g);
   float wave = sin((unitNoise + fragPosition.y) * 3.0 * pi);

   return vec3(((bumps/2.0) + (wave/32.0)), bumps, ((bumps/4.0) + (wave/64.0)));
 }

 void main() 
 {
   vec4 lightNormal = normalize(light);

   float lambert = max(0.0, dot(lightNormal, fragNormal));

   float vdotn = dot(lightNormal, fragNormal);
   vec4 reflection = (vdotn * fragNormal * 2.0) - lightNormal;
   float spec = max(0.0, pow(dot(reflection, cameraNorm), specular)) * clamp(ceil(vdotn), 0.0, 1.0);

   vec4 surfaceColor = vec4(Colorize(Perlin(fragPosition.x, fragPosition.y, fragPosition.z, d_smoothness, d_persistence)), 1.0);

   output_color = mix(
     clamp(surfaceColor * lambert + vec4(light_color, 1.0) * spec, 0.0, 1.0),
     vec4(0.1, 0.1, 0.2, 1.0),
     clamp(distance(cameraPos, fragPosition) / 20.0, 0.0, 1.0));
 }
")

;; # Shapes:

(def Dome
     "Dome roof parametric shape"
     {:u {:begin -1.0
	  :end 1.0
	  :step 0.125}
      :v {:begin -1.0
	  :end 1.0
	  :step 0.125}
      :Position (fn
		  [u v]
		  [v (- 2.5 (+ (* u u) (* v v))) u])
      :Normal (fn
		[u v]
		[(- v) -1.0 (- u)])})

(def Column
     "Column parametric shape"
     {:u {:begin 0.0
	  :end (* 2.0 Math/PI)
	  :step (* 0.250 Math/PI)} ;Around
      :v {:begin 0.0
	  :end 2.0
	  :step 1.0} ;Up
      :Position (fn
		  [u v]
		  [(* 0.1 (Math/cos u)) v (* 0.1 (Math/sin u))])
      :Normal (fn
		[u v]
		[(Math/cos u) 0.0 (Math/sin u)])})

(def Floor
     "Floor parametric shape"
     {:u {:begin -8.0
	  :end 8.0
	  :step 1.0}
      :v {:begin -8.0
	  :end 8.0
	  :step 1.0}
      :Position (fn
		  [u v]
		  [u 0.0 v])
      :Normal (fn
		[u v]
		[0.0 -1.0 0.0])})

;;; #Shape generation

(defn- flatten-shape-data [shape-data ending]
  (concat (reduce concat (interpose [ending] shape-data)) [ending]))

(defn- triangle-strip-mapfn [the-fn]
  (fn [u u+1 v]
    [(the-fn u+1 v)
     (the-fn u v)]))

(defn- generate-shape-range [shape-range & [detail]]
  (let [detailed-range (if detail
			 (assoc shape-range :step (* detail (:step shape-range)))
			 shape-range)]
    (range (:begin detailed-range) (+ (:end detailed-range) (:step detailed-range)) (:step detailed-range)))) 

(defn- generate-shape-data [shape type actual-u-range v-range detail]
  (let [u-range (drop-last 1 actual-u-range)
	u+1-range (drop 1 actual-u-range)]
    (mapcat (fn [u u+1]
	      (mapcat #((triangle-strip-mapfn (type shape)) u u+1 %1) v-range))
	    u-range u+1-range)))

(defn make-shape [shape & [detail]]
  (let [attributes (:attributes @core-shader)
	buffers (make-attribute-buffers attributes)
	u-range (generate-shape-range (:u shape) detail)
	v-range (generate-shape-range (:v shape) detail)
	strip-length (* 2 (count v-range))
	strip-count (count u-range)]
    {:buffers (fill-attribute-buffers buffers
				      {:Position (flatten-shape-data
						  (generate-shape-data shape :Position u-range v-range detail) 1.0)
				       :Normal (flatten-shape-data
						(generate-shape-data shape :Normal u-range v-range detail) 0.0)})
     :draw-firsts (int-array (take strip-count (iterate #(+ strip-length %) 0)))
     :draw-counts (int-array (take strip-count (repeat strip-length)))
     :draw-size strip-count}))

;;; #Shape drawing

(defn draw-shape [made-shape]
  (bind-attribute-buffers (:buffers made-shape))
  (.glMultiDrawArrays *gl* gl-triangle-strip
		      (:draw-firsts made-shape) 0
		      (:draw-counts made-shape) 0
		      (:draw-size made-shape)))

;;; #Camera

(def up-vector (matrix/vector 0.0, 1.0, 0.0))

(defn init-camera []
  {:position (matrix/vector 0.0 1.0 0.0)
   :forward (matrix/vector 0.0, 0.0, -1.0)})

(defn move-camera
  [camera forward]
  (update-in camera [:position]
	     #(matrix/add %
			  (matrix/vscale (:forward camera) forward))))

(defn rotate-camera
  [camera rotate]
  (let [f (matrix/normalize (:forward camera))
	r (matrix/normalize (matrix/cross f up-vector))
	u (matrix/normalize (matrix/cross r f))]
    (assoc camera :forward (matrix/add (matrix/vscale f (Math/cos rotate))
				       (matrix/vscale r (Math/sin rotate))))))

(defn tilt-camera
  [camera rotate]
  (let [f (matrix/normalize (:forward camera))
	r (matrix/normalize (matrix/cross f up-vector))
	u (matrix/normalize (matrix/cross r f))]
    (if (and (< (+ rotate (.y f)) 0.95) (> (+ (.y f) rotate) -0.95))
      (assoc camera :forward (matrix/normalize (matrix/add (matrix/vscale f (Math/cos rotate))
							   (matrix/vscale u (Math/sin rotate)))))
      camera)))

(defn camera-matrix
  [camera]
  (let [p (:position camera)
	f (:forward camera)
	r (matrix/cross f up-vector)
	u (matrix/cross r f)
	T (matrix/translation (- (.x p)) (- (.y p)) (- (.z p)))
	R (matrix/matrix
	   (.x r) (.y r) (.z r) 0.0
	   (.x u) (.y u) (.z u) 0.0
	   (- (.x f)) (- (.y f)) (- (.z f)) 0.0
	   0.0 0.0 0.0 1.0)]
    (matrix/mul R T)))
	   
(defn make-pos
  [camera]
  (let [position (:position camera)]
    (matrix/translation (* 2.0 (Math/floor (* (.x position) 0.125)))
			0.0
			(* 2.0 (Math/floor (* (.z position) 0.125))))))

;;; #Rendering Objects

(defn renderable
  [object matrix]
  {:made-shape object
   :matrix matrix})

(defn draw-renderable
  [renderable mMatrixUniform constant]
  (bind-uniforms :mat4 mMatrixUniform (matrix/mul (:matrix renderable) constant) 1)
  (draw-shape (:made-shape renderable)))

;;; #Scene

(defn make-scene
  []
  (let [column (make-shape Column)
	floor (make-shape Floor)
	dome (make-shape Dome)
	make-room (fn [x z]
		    (let [trans (matrix/mul
				 (matrix/scale 4.0)
				 (matrix/translation x 0.0 z))]
		      [(renderable column (matrix/mul trans (matrix/translation -1.0 0.0 -1.0)))
		       (renderable dome trans)]))]
    (concat
     (mapcat (fn [x]
	       (mapcat #(make-room x %)
		       (range -6.0 7.0 2.0)))
	     (range -6.0 7.0 2.0))
     [(renderable floor (matrix/scale 4))])
    ))

;;; #Actual app

(def state (agent {}))

(defglconst gl2 gl-depth-test)
(defglconst gl2 gl-cull-face)

(defn app-init
  [drawable]
  (send state assoc :old-shader nil :camera (init-camera))
  (with-gl (app/gl-context drawable)
    (doto *gl*
      (.setSwapInterval 1)
      (.glEnable gl-cull-face)
      (.glEnable gl-depth-test))
    ))

(defn app-shader-hoister
  []
  (when (or (not (= (:vertex (:old-shader @state)) (:vertex @core-shader)))
	    (not (= (:fragment (:old-shader @state)) (:fragment @core-shader))))
    (let [shader (try
		   (load-defshader @core-shader)
		   (catch Exception e
		     (.printStackTrace e)
		     0))
	  log (if (not (= 0 shader))
		(get-log shader)
		"Shader failed to hoist")]
      (if (and (not (nil? log)) (clojure.string/blank? log))
	(send state assoc :shader shader)
	(println log)))
    (send state assoc :old-shader @core-shader)))


(defglconst gl2 gl-color-buffer-bit)
(defglconst gl2 gl-depth-buffer-bit)

(defn app-display
  [drawable]
  (with-gl (app/gl-context drawable)
    (app-shader-hoister)
    (doto *gl*
      (. glClearColor 0.1 0.1 0.2 1.0)
      (. glClear (bit-or gl-color-buffer-bit gl-depth-buffer-bit)))
    (let [state @state
	  camera-position (:position (:camera state))]
      (with-shader (:shader state)
	(bind-uniforms :mat4 (get-uniform "vMatrix") (camera-matrix (:camera state)) 1)
	(bind-uniforms :mat4 (get-uniform "pMatrix") (:proj-matrix state) 1)
	(bind-uniform :vec4 (get-uniform "cameraPos") [(.x camera-position) (.y camera-position) (.z camera-position) 1.0]) 
	(let [mMatrixUniform (get-uniform "mMatrix")
	      constant (make-pos (:camera state))]
	  (vec (map #(draw-renderable % mMatrixUniform constant) (:scene state))))
      )))
  (dosync (alter frames-so-far + 1)))

(defn app-dispose
  [drawable]
  (with-gl (app/gl-context drawable)
    ()))

(defn app-reshape
  [drawable x y w h]
  (with-gl (app/gl-context drawable)
    (send state assoc
	  :proj-matrix (matrix/projection 1.0 1000.0 1.0 (/ h w)))
    (send-off state assoc :scene (make-scene))
    ))

(defn on-key [k]
  (condp = k
	 KeyEvent/VK_DOWN (send state update-in [:camera] #(move-camera % 0.25))
	 KeyEvent/VK_UP (send state update-in [:camera] #(move-camera % -0.3))
	 KeyEvent/VK_LEFT (send state update-in [:camera] #(rotate-camera % -0.05))
	 KeyEvent/VK_RIGHT (send state update-in [:camera] #(rotate-camera % 0.05))
	 KeyEvent/VK_A (send state update-in [:camera] #(tilt-camera % 0.05))
	 KeyEvent/VK_Z (send state update-in [:camera] #(tilt-camera % -0.05))
	 k '()
	))

(defn kick-app
  "Builds the canvas, a basic listener, and allows for dynamic redefinition. Returns AWT frame object."
  []
  (let [canvas (app/canvas)
	our-state {}]
    (app/attach-gl-listener canvas
			    :init #'app-init
			    :display #'app-display
			    :dispose #'app-dispose
			    :reshape #'app-reshape)
    (doto canvas
      (.addKeyListener (proxy [java.awt.event.KeyAdapter] []
			 (keyPressed [^KeyEvent e]
				     (on-key (.getKeyCode e))))))
    (app/start canvas)))

(defn -main
  []
  (kick-app)
  (kick-fps-watcher))