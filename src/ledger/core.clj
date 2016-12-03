(ns ledger.core)
(import java.util.Date)

(defn add-account [ ledger id name amt ]
  (cond
    (contains? @ledger id) nil
    :else (dosync
            (alter ledger assoc id { :balance (ref amt) :name name }))))

(defn deposit [ ledger id amt ]
  (cond
    (= (contains? @ledger id) false) nil
    :else (do
            (dosync (alter ((@ledger id) :balance) #(+ amt %)))
            @((@ledger id) :balance))))
        


(defn withdraw [ ledger id amt ]
  (cond
    (= (contains? @ledger id) false) nil
    (< @((@ledger id) :balance) amt) nil
    :else (do
            (dosync (alter ((@ledger id) :balance) #(- % amt)))
            @((@ledger id) :balance))))
            

(defn transfer [ ledger from-id to-id amt ]
  (cond 
    (not= (contains? @ledger from-id) (contains? @ledger to-id)) nil
    (< @((@ledger from-id) :balance) amt) nil
    :else
            (dosync
            (ensure ledger)
            (alter ((@ledger from-id) :balance) #(- % amt))
            (alter ((@ledger to-id) :balance) #(+ % amt))
            @((@ledger to-id) :balance))))


(defn remove-account [ ledger id ]
  (if
    (= (contains? @ledger id) false) (println "Error: Trying to remove non-existent account"))
  (dosync 
    (let [ removed (@ledger id) ]
      (alter ledger dissoc id)
      removed)))

(def ledg (ref {}))

(add-account ledg 2006 "Frances Allen" 1000)
(add-account ledg 2007 "E. Allen Emerson" 1200)
(add-account ledg 2008 "Barbara Liskov" 2000)
(add-account ledg 2009 "Charles P. Thacker" 1800)

(println @ledg)
(println)

(deposit ledg 2009 1000)

(println @ledg)
(println)

(withdraw ledg 2009 1000)

(println @ledg)
(println)

(transfer ledg 2008 2006 1000)

(println @ledg)
(println)

(let [ temp (transfer ledg 2007 2009 1000000) ]
  (println temp))
(println)

(def thacker (remove-account ledg 2009))

(println @ledg)

(println (thacker :name))
(println @(thacker :balance))

(add-account ledg 2009 (thacker :name) @(thacker :balance))

(println @ledg)
(println)

(defn consistency-check [ ledg num-accounts total-balances ]
  (dosync 
    (def temp (atom 0))
    ;;Add up total in all accounts
    (loop [ n (count (vals @ledg)) accts (vals @ledg) total 0 ]
      (cond
        (= n 0) (reset! temp total) 
        :else (recur (- n 1) (rest accts) (+ total @(:balance (first accts))) )
        )
      )
    ;;check consistency
    (cond 
      (not= total-balances @temp) (do (println temp) false)
      (not= num-accounts (count (vals @ledg))) false
      :else true
      )))

(def attempted-transfer-count ( agent 0))
(def successful-transfer-count ( ref 0))
(def remove-and-add-count (ref 0))

;Map for randomly selecting ids
(def acctMap { 0 2006, 1 2007, 2 2008, 3 2009})

(defn now [] (new java.util.Date))

(def start (atom (.getTime (now))) )

;; future 1

(future
  ( while (< (.getTime (now)) (+ @start 10000))
    (cond
      (= (transfer ledg (acctMap (rand-int 4)) (acctMap (rand-int 4)) 1000) nil ) (send attempted-transfer-count inc)
      :else (do 
              (send attempted-transfer-count inc) 
              (dosync (alter successful-transfer-count inc))
              )
      )
    (Thread/sleep (+ 10 (rand-int 31))
                  )
    )
  )

;;future 2
;;reset start
(reset! start (.getTime (now)))

(future
  ( while (< (.getTime (now)) (+ @start 10000))
    (cond 
      (= (transfer ledg (acctMap (rand-int 4)) (acctMap (rand-int 4)) 100) nil) (send attempted-transfer-count inc)
      
      :else (do 
              (send attempted-transfer-count inc)
              (dosync (alter successful-transfer-count inc)))
      )
    (Thread/sleep (+ 20 (rand-int 61)))
    )
  )

;;future 3
(reset! start (.getTime (now)))

(future
  (def removed (atom {}))
  
  ( while (< (.getTime (now)) (+ @start 10000))
    (let [ n (rand-int 4) ]
      (reset! removed (remove-account ledg (acctMap n)))
      (Thread/sleep (+ 10 (rand-int 11)))
      (add-account ledg (acctMap n) (@removed :name) @(@removed :balance))
      (dosync (alter remove-and-add-count inc)))
      
    (Thread/sleep 500))
  )

;;future 4

(reset! start (.getTime (now)))

(future
  ( while (< (.getTime (now)) (+ @start 10000))
    (println "consistency-check = " (consistency-check ledg 4 6000))
    (Thread/sleep 500))
  )

;Have to wait for a long time on my repl or results are messed up
(Thread/sleep 10000)

(println)
(println "Attempted transfer count: " @attempted-transfer-count)
(println "Successful transfer count: " @successful-transfer-count)
(println "Remove and add count: " @remove-and-add-count)

(println)
(println @ledg)
(println)

(println "Final consistency check =" (consistency-check ledg 4 6000))
(println)


