(ns dali.dev
  (:import [javax.swing SwingUtilities UIManager JFrame ImageIcon]
           [java.awt Frame MouseInfo]
           [java.awt.image BufferedImage]))

(defn do-swing*
  "Runs thunk in the Swing event thread according to schedule:
    - :later => schedule the execution and return immediately
    - :now   => wait until the execution completes."
  [schedule thunk]
  (cond
   (= schedule :later) (SwingUtilities/invokeLater thunk)
   (= schedule :now) (if (SwingUtilities/isEventDispatchThread)
                       (thunk)
                       (SwingUtilities/invokeAndWait thunk)))
  nil)

(defmacro do-swing
  "Executes body in the Swing event thread asynchronously. Returns
  immediately after scheduling the execution."
  [& body]
  `(do-swing* :later (fn [] ~@body)))

(defn get-laf-property
  [key]
  (javax.swing.UIManager/get key))

(def error-icon (get-laf-property "OptionPane.errorIcon"))

(defn- error-image [msg]
  (let [i (BufferedImage. 600 300 BufferedImage/TYPE_INT_RGB)
        g (.getGraphics i)
        icon-width (.getIconWidth error-icon)]
    (.paintIcon error-icon nil g 0 0)
    (.drawString g msg (+ 10 icon-width) 20)
    i))

(defn watch-image
  "Shows the passed java.awt.Image in a frame, and re-paints at 15
  FPS (or the specified FPS). You can also pass a reference to an
  Image, which will be dereferenced at every frame, or an
  image-returning function, which will be called at every frame.  The
  function returns a future which can be cancelled to stop the
  re-painting. Of course the re-painting stops automatically when the
  frame is closed."
  ([image] (watch-image image 15))
  ([image fps]
     (let [get-image (fn [] (cond (instance? clojure.lang.IDeref image) @image
                                  (fn? image)
                                  (try (image)
                                       (catch Exception e
                                         (do
                                           (.printStackTrace e)
                                           (error-image (str (.getName (class e))
                                                             ", check your console")))))
                                  :otherwise image))
           cached-image (ref nil)
           panel (proxy [javax.swing.JPanel] []
                   (paintComponent [g]
                                   (dosync (ref-set cached-image (get-image)))
                                   (if @cached-image
                                         (.drawImage g @cached-image 0 0 this)))
                   (getPreferredSize[] (if @cached-image
                                         (java.awt.Dimension.
                                          (.getWidth @cached-image)
                                          (.getHeight @cached-image))
                                         (java.awt.Dimension. 100 100))))
           updater (future
                    (while true
                      (Thread/sleep (/ 1000 fps))
                      (do-swing (.repaint panel))))]
       (doto (JFrame.)
         (.add panel)
         (.pack)
         (.setVisible true)
         (.addWindowListener
          (reify
            java.awt.event.WindowListener
            (windowClosing [listener event]
              (future-cancel updater))
            (windowIconified [_ _])
            (windowClosed [_ _])
            (windowDeiconified [_ _])
            (windowActivated [_ _])
            (windowOpened [_ _])
            (windowDeactivated [_ _]))))
       updater)))
