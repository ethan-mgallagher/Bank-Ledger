(load "core")

(def ledg (ref {}))

(add-account ledg 2006 "Frances Allen" 1000)
(add-account ledg 2007 "E. Allen Emerson" 1200)
(add-account ledg 2008 "Barbara Liskov" 2000)
(add-account ledg 2009 "Charles P. Thacker" 1800)

(println @ledg)
(println)