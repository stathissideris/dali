[:connect
 {:start [:a]
  :end [:b]
  :path [:|-]}]

[:connect
 {:start {:selector [:a] :anchor :top}
  :end {:selector [:b] :anchor :left}
  :path [:|-]}]

[:connect [:a :-| :b]]

[:connect [:a :-| [:node {:where :near-start} "yes"] :b]]

[:connect {:label {:node "yes"
                   :where :near-start}}
 [:a :-| :b]]

[:connect {:label {:node "yes"
                   :where :near-start}
           :path [:a :-| :b]}]

[:connect {:label ["yes" :near-start]
           :path [:a :-| :b]}]

[:connect {:label {:what [:circle :_ 20]
                   :where :near-start}
           :path [:a :-| :b]}]

[:connect {:pin {:what [:circle :_ 20]
                 :where :near-start}
           :path [:a :-| :b]}]

[:connect {:label ["start" :near-start
                   "end" :near-end]
           :path [:a :-| :b]}]

[:connect [:a :-- :b]]
[:connect [:a :b]]

[:connect {:label [[:text {...} "start"] :near-start
                   [:text {...} "end"] :near-end]
           :path [:a :-| :b]}]

[:connect {:pin [:near-start [:circle :_ 20]
                 :near-end [:circle :_ 10]]
           :path [:a :-| :b]}]

[:connections
 [:a :b]
 [:b :|- :c]
 [:c :-- :d]]


;;;;syntax 1;;;;

[:connections
 [:a :b] {:label [:near-start [:text "start"]]}
 [:b :|- :c]
 [:c :-- :d]]

connections = [:connections connection+]
connection = path-spec attrs?
path-spec = [start-spec line-type? end-spec]
start-spec = id | selector
end-spec = id | selector
line-type? = :-- | :|- | :-|

;;;;syntax 2;;;;

;;this looks more like hiccup, but with the extra concept of having
;;attrs, content, attrs, content, where content is the path-spec for a
;;connection and attrs are optional. This allows you to write a single
;;connection, or multiple connections using the same tag

[:connect {:label [:near-start [:text "start"]]} [:a :b]]

[:connect
 [:c :-- :d]
 {:label [:near-start [:text "start"]]} [:a :b]
 {:label [:near-end [:text "end"]]} [:a :c]]

[:connect
 [:c :-- :d]
 [:e :-| :b]
 [:b :-- :d]
 [:e :-- :d]
 [:c :|- :a]]

connect = [:connect connection+]
connection = attrs? path-spec
path-spec = [start-spec line-type? end-spec]
start-spec = id | selector
end-spec = id | selector
line-type? = :-- | :|- | :-|

;;;;syntax 3;;;;
Syntax 2 is my favourite, but it's not compatible with hiccup.
This allows attributes for all connections and for individual connections:

[:connect
 {:connections
  [[:c :-- :d]
   [:e :-| :b]
   [:b :-- :d]
   [:e :-- :d]
   [:c :|- :a]]}]

;;;matrices


[:ghost]

[:matrix
 :_ :_ [:text "lala"] :_
 ]

[:matrix
 {:elements [:_ :_ [:text "lala"] :_]}]


