(ns dali.constraints)

;;some things that should be expressible:

;;Rectangles of class mybox should:
;; have their tops aligned with the top of the page
;; be the same height as the page
;; have a horizontal distance to each other of 50 units
;; have the same width to each other
;; (modifies/defines position and dimensions)

;;Text object with ID x should have the same center as rectangle with ID y.
;;(modifies position)

;;There should be a line that looks like [:line {:class "arrow"}] that
;;starts at the middle of the right of rectangle x and ends at the
;;middle of the left of rectangle y.
;;(adds an element based on existing bounds)

;;If there is a line that connects the center of rect x to the center
;;of rect y and we call it line c, there should be a line that looks
;;like [:line {:class "arrow"}] that starts at the intersection of
;;line c to rect x and ends at the intersection of line c to rect y.
;;(adds geometry based on theoretical bounds which is based on exists
;;bounds)

