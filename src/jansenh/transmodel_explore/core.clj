;;-----------------------------------------------------------------------------
;; File: src/jansenh/transmodel/transmodel.clj
;; Author: Henning Jansen - henning.jansen@jansenh.no
;; Copyright: (c) 2025
;; License: Eclipse Public License 2.0 - http://www.eclipse.org/legal/epl-2.0.
;;-----------------------------------------------------------------------------

(ns jansenh.transmodel-explore.core
  "Add comment here."
  (:require [jansenh.transmodel.parser.core :as parser]
            [jansenh.transmodel.parser.calendar :as cal]
            [jansenh.transmodel.generator.timetable :as tt]
            [jansenh.transmodel.netex.registry :as reg]
            [jansenh.transmodel.netex.line :as line]
            [jansenh.transmodel.netex.explore :as exp]
            [jansenh.transmodel.netex.extract :as ext]
            [jansenh.transmodel.netex.interchanges :as intrc]
            )
  (:import [java.time LocalDate]))

;;   Transmodel, NeETx and SIRI.
;;
;;   authors:   Henning Jansen - henning.jansen@jansenh.no;
;;   since:     0.1.0                    2025-08-07
;;   version:   0.1.0
;; ------------------------------------+----------------------------------------

;; data files

(comment
  (def shared-data-file "/home/jansenh/data/rb_norway-aggregated-netex/KOL/_KOL_shared_data.xml")
  (def line-data-file "/home/jansenh/data/rb_norway-aggregated-netex/KOL/KOL_KOL-Line-8_5986_1025_Fogn---Judaberg---Helgoy.xml")
  (def line-data-file "/home/jansenh/data/rb_norway-aggregated-netex/KOL/KOL_KOL-Line-8_5900_518_518.xml")

  ;; the data

  ;; Parse files

  (def shared-data (parser/parse-xml-file shared-data-file))
  (def line-data (parser/parse-xml-file line-data-file))

  ;; Set date ranges
  (def date-range (cal/weeks-ahead 6))
  (def from-date (:from date-range))
  (def to-date (:to date-range))

  ;; Build calendar index

  (def calendar-index (cal/build-calendar-index shared-data))

  ;; Check stats calendar-index

  (map keys [calendar-index])
  (map key (:day-types calendar-index))
  (map key (:operating-periods calendar-index))
  (:stats calendar-index)

  ;; ===========================================================================
  ;;
  ;; Inspect a specific day type
  ;;
  ;; ===========================================================================
  ;;
  ;;   The calendar-index in shared_data.xml has DayTypes, a fundamental concept
  ;;   to some of our routes.

  (cal/describe-day-type calendar-index "KOL:DayType:1050")

  ;; ===========================================================================
  ;;
  ;; Generate all Journeys
  ;;
  ;; ===========================================================================
  ;;
  ;;   The Line file (LineRef)  has ServiceJourneys, the second fundamental
  ;;   concept in NeTEx routes.
  ;;   The ServiceJourney can or can not rely on DayTypes in shared_data.xml
  ;;   If no DayTypes are used in ServiceJourneys, the we need a
  ;;
  ;;   The logic:
  ;;   + DayType with DaysOfWeek + linked OperatingPeriod = Pattern applied
  ;;     within that date range.
  ;;   + DayType without DaysOfWeek + linked OperatingPeriod = Every day in
  ;;      that range.
  ;;

  ;; Get service journeys from line file

  (def journeys (tt/collect-service-journeys line-data))
  (count journeys)

  (let [{:keys [from to]} (cal/weeks-ahead 6)
        timetable (tt/generate-timetable calendar-index journeys from to)]
  ;; Do something with timetable
    (tt/print-timetable timetable :limit 20))

;; Build timetable

  (def timetable
    (tt/generate-timetable calendar-index journeys from-date to-date))
  ;; Check stats timetable

  (first (map keys timetable))
  (count timetable)
  (take 10 timetable)

;; ===========================================================================
  ;;
  ;; Display human-readable timetable
  ;;
  ;; ===========================================================================

  (tt/print-timetable timetable :limit 20)

;; Generate SQL

  (println tt/create-table-ddl)
  (doseq [trip (take 10 timetable)]
    (println (tt/generate-insert-sql trip)))

;;--- comment
  )
(comment ;; --------------------------------------------------------------------

  ;; Single day - multiple ways
  (tt/timetable-for-date calendar-index journeys "2025-12-03")
  (tt/timetable-for-date calendar-index journeys (LocalDate/of 2025 11 28))

  ;; Convenience
  (tt/today calendar-index journeys)
  (tt/tomorrow calendar-index journeys)

  ;; Pretty print for traffic coordinator
  (tt/print-daily-timetable calendar-index journeys "2025-12-03")

  ;; Or get data for Tablecloth/notebook
  (def monday-trips (tt/timetable-for-date calendar-index journeys "2025-12-03"))
  (count monday-trips)

  ;; Compare two days
  (let [mon (tt/timetable-for-date calendar-index journeys "2025-12-01")
        sat (tt/timetable-for-date calendar-index journeys "2025-12-03")]
    {:monday (count mon)
     :wednesday (count sat)})
  ;; ------------------------------------------------------------------> comment
  )




(comment ;; --------------------------------------------------------------------

  ;; 1. Load shared data
  (reg/load-file! shared-data-file)
  (reg/stats)

  ;; 2. Parse line file
  (def data (line/parse-line-file line-data-file))

  ;; 3. Inspect
  (count (:journeys data))
  (count (:interchanges data))
  (count (:roundtrips data))

  ;; 4. View roundtrips
  (first (:roundtrips data))

  ;; 5. Standalone journeys (not in any chain)
  (count (line/standalone-journeys data))

  ;; 6. With registry resolution
  (line/resolve-roundtrip (first (:roundtrips data)) (:journeys data))

  ;; ---> comment
  )

(comment ;; --------------------------------------------------------------------
  ;; Explore the explore.clj features
  ;; (I expect to move this to a Notebook anytime soon.)

  (def shared-data (exp/explore-line-file shared-data-file))
  ;; Load line file

  (def data (exp/explore-line-file line-data-file))

  ;; Debug
  (def pub-del (parser/parse-xml-file line-data-file))

  ;; Check top level tag
  (:tag pub-del)
  ;; => :xmlns.http%3A%2F%2Fwww.netex.org.uk%2Fnetex/PublicationDelivery  (or similar)

  ;; Check what's in content (first few)
  (->> (:content pub-del)
       (filter map?)
       (map :tag)
       (take 5))

  (name :xmlns.http%3A%2F%2Fwww.netex.org.uk%2Fnetex/dataObjects)

  ;; Test find-child
  (def data-obj (#'exp/find-child pub-del "dataObjects"))

  data-obj ;; nil or element?

  ;; If nil, check manually:
  (->> (:content pub-del)
       (filter map?)
       (filter #(= (name (:tag %)) "dataObjects"))
       first)

  (def cf (#'exp/find-child (#'exp/find-child pub-del "dataObjects") "CompositeFrame"))

  (def frames (#'exp/find-child cf "frames"))

  ;; What frames exist?
  (->> (:content frames)
       (filter map?)
       (map #(name (:tag %))))

  (def tf (#'exp/find-child frames "TimetableFrame"))

  ;; Check it exists
  (:tag tf)

  ;; Check for vehicleJourneys
  (def vj (#'exp/find-child tf "vehicleJourneys"))

  ;; What's inside?
  (->> (:content vj)
       (filter map?)
       (map #(name (:tag %)))
       (take 5))

  ;; ---------------------------------------------------------------------------
  ;; Verify JourneyInterchanges extraction
  (count (#'exp/find-child tf "journeyInterchanges"))
  ;; The element
  (def ji (#'exp/find-child tf "journeyInterchanges"))

  ;; What's inside?
  (->> (:content ji)
       (filter map?)
       (map #(name (:tag %)))
       #_(take 10)
       count)

  (count (#'exp/extract-interchanges tf))

  ;; ---------------------------------------------------------------------------
  ;; Step by step, exactly as explore-line-file does it:
  (def pub-del (parser/parse-xml-file line-data-file))

  (def cf (first (#'exp/find-children (#'exp/find-child pub-del "dataObjects") "CompositeFrame")))

  cf ;; nil?

  (def frames (#'exp/find-child cf "frames"))

  frames ;; nil?

  (def tf (first (#'exp/find-children frames "TimetableFrame")))

  tf ;; nil?

  (exp/extract-service-journeys tf)

  (#'exp/find-child tf "vehicleJourneys")

  (#'exp/find-children (#'exp/find-child tf "vehicleJourneys") "ServiceJourney")

  (#'exp/find-children (#'exp/find-child tf "vehicleJourneys") "ServiceJourney")

  (def vj (#'exp/find-child tf "vehicleJourneys"))

  ;; Manual filter - does this work?
  (->> (:content vj)
       (filter map?)
       (filter #(= (name (:tag %)) "ServiceJourney"))
       count)

  ;; This returns empty:
  (#'exp/find-children vj "ServiceJourney")

  ;; Let's trace it:
  (->> (:content vj)
       (filter map?)
       (filter #(#'exp/tag-matches? % "ServiceJourney"))
       count)

  (def one-sj (first (filter map? (:content vj))))

  (:tag one-sj)
  ;; => :xmlns.http%3A.../ServiceJourney

  (name (:tag one-sj))
  ;; => "ServiceJourney"

  (#'exp/tag-matches? one-sj "ServiceJourney")
  ;; => true or false?

  ;; What does this return?
  (#'exp/find-children vj "ServiceJourney")

  ;; Empty? Let's check find-children source behavior:
  (count (#'exp/find-children vj "ServiceJourney"))

  ;; Force reload
  ;;(require [jansenh.transmodel.netex.explore :as exp] :reload)

  ;; Then test again
  (count (#'exp/find-children vj "ServiceJourney"))

  (def data (exp/explore-line-file line-data-file))

  (:journey-count data)

  (def pub-del (parser/parse-xml-file line-data-file))

  ;; Step 1
  (def step1 (#'exp/find-child pub-del "dataObjects"))
  (some? step1)

  ;; Step 2
  (def step2 (#'exp/find-children step1 "CompositeFrame"))
  (count step2)

  ;; Step 3
  (def cf (first step2))
  (def step3 (#'exp/find-child cf "frames"))
  (some? step3)

  ;; Step 4
  (def step4 (#'exp/find-children step3 "TimetableFrame"))
  (count step4)

  ;; Step 5
  (def tf (first step4))
  (def step5 (#'exp/find-child tf "vehicleJourneys"))
  (some? step5)

  ;; Step 6 - THE TEST
  (def step6 (#'exp/find-children step5 "ServiceJourney"))
  (count step6)

  (:journey-count data)

  ;; ==============================================================================
  ;;
  ;; Basic statistics - ServiceJourneys and Interchanges:
  
  (:journey-count data)
  (:interchange-count data)
  (:stay-seated-count data)

  ;; Look at specific interchange
  (first (:interchanges data))

  ;; Find journey chain starting from 1011
  (def journey-1011 "KOL:ServiceJourney:5986_250619122708562_1011")
  (exp/find-journey-chain (:graph data) journey-1011)

  ;; Get details of connected journeys
  (let [chain (exp/find-journey-chain (:graph data) journey-1011)]
    (map #(select-keys (get (:journeys data) %) [:name :private-code :passing-times])
         chain))

  ;; See the full passing times sequence across chained journeys
  (let [chain (exp/find-journey-chain (:graph data) journey-1011)
        journeys (:journeys data)]
    (->> chain
         (mapcat #(:passing-times (get journeys %)))
         (map #(select-keys % [:departure-time :arrival-time]))))
 
  ;; Verify:
  ;;
  (:journey-count data)
  (:interchange-count data)
  (:stay-seated-count data)
  (count (:graph data))

  
  ;; ===========================================================================
  ;;
  ;; Working data structure
  ;;
  ;; ===========================================================================

  (def data (exp/explore-line-file line-data-file))

  (:journeys data)
  (:interchanges data)
  (:graph data)

  ;; ===========================================================================
  ;;
  ;; ServiceJourneyInterchange
  ;; -------------------------
  ;;
  ;; Load shared data and line-data in Registry, extracted within the
  ;; clj-transmodel context.
  ;; This is BRITTLE, do not touch!
  ;;
  (reg/load-file! shared-data-file)
  (reg/load-line-file! line-data-file)
  (reg/stats)
  
  ;; After loading, add these checks:
  (println "Loaded service journeys count:" (count (reg/all-service-journeys)))
  (println "Sample service journey:" (first (reg/all-service-journeys)))

  ;; After loading data, add these checks:
  (println "Registry stats:" (reg/stats))
  (println "Sample operator:" (reg/operator "KOL:Operator:316"))
  (println "Sample stop point:" (reg/stop-point "KOL:ScheduledStopPoint:11416420_0"))
  (println "Sample service journey:" (reg/service-journey "KOL:ServiceJourney:5986_250619122708559_3004"))

  ;; After loading shared data, check:
  (println "Registry stats:" (reg/stats))
  (println "Service journeys count:" (count (reg/all-service-journeys)))
  (println "Sample service journey:" (first (reg/all-service-journeys)))

  ;; Extract and visualize interchanges
  (def interchanges (ext/all-interchanges line-data))
  (intrc/visualize-all-interchanges)

  ;; Or for a specific journey
  (def specific-journey "KOL:ServiceJourney:5986_250619122708559_3004")
  (intrc/visualize-journey-interchanges specific-journey)

  ;; ------------------------------------------------------------------> comment
  

  (comment ;; --------------------------------------------------------------------

    ;; Save for later, this can have value, rebuilt as a debug fn
    ;;
    (def pub-del (parser/parse-xml-file line-data-file))

    ;; Step 1

    (def step1 (#'exp/find-child pub-del "dataObjects"))
    (some? step1)

    ;; Step 2

    (def step2 (#'exp/find-children step1 "CompositeFrame"))
    (count step2)

    ;; Step 3

    (def cf (first step2))
    (def step3 (#'exp/find-child cf "frames"))
    (some? step3)

    ;; Step 4

    (def step4 (#'exp/find-children step3 "TimetableFrame"))
    (count step4)

    ;; Step 5

    (def tf (first step4))
    (def step5 (#'exp/find-child tf "vehicleJourneys"))
    (some? step5)

    ;; Step 6 - THE TEST

    (def step6 (#'exp/find-children step5 "ServiceJourney"))
    (count step6)

    (def data (exp/explore-line-file line-data-file))
    (:journey-count data)

    ;;-------------------------------------------------------------------> comment
    ))


(comment
  ;; ---------------------------------------------------------------------------
  ;; Load the data

  (reg/load-file! shared-data-file)
  (reg/load-line-file! line-data-file)

  ;; Check what loaded
  (reg/stats)

  ;; Get a single interchange
  (reg/interchange "KOL:ServiceJourneyInterchange:3332")

  ;; Get a service journey
  (reg/service-journey "KOL:ServiceJourney:5986_250619122708559_3004")

  ;; Get all interchanges
  (count (reg/all-interchanges))

  ;; ------------------------------------------------------------------> comment
  )
(comment
  (require '[jansenh.transmodel.netex.interchanges :as interchanges])

  ;; View statistics
  (interchanges/interchange-stats)

  ;; View a specific journey's interchanges
  (interchanges/visualize-journey-interchanges
   "KOL:ServiceJourney:5986_250619122708559_3004")

  ;; View all interchanges (lots of output!)
  (interchanges/visualize-all-interchanges)

  ;; Find roundtrip chains
  (interchanges/roundtrip-chains))

(comment
  (let [sj (reg/service-journey "KOL:ServiceJourney:5986_250619122708623_4020")]
    (clojure.pprint/pprint (:passing-times sj))))

(comment
  (reg/reset-registry!)
  (reg/load-line-file! line-data-file)

  (let [sj (reg/service-journey "KOL:ServiceJourney:5986_250619122708623_4020")]
    (clojure.pprint/pprint (:passing-times sj))))

(comment
  (reg/reset-registry!)
  (reg/load-file! shared-data-file)
  (reg/load-line-file! line-data-file)

  (reg/stats)

  ;; Check a stop point
  (let [sp (reg/stop-point "KOL:ScheduledStopPoint:11416420_0")]
    (clojure.pprint/pprint sp))

  ;; Check a service journey with times
  (let [sj (reg/service-journey "KOL:ServiceJourney:5986_250619122708623_4020")]
    (clojure.pprint/pprint (:passing-times sj)))

  ;;
  ;; ------------------------------------------------------------------> comment

  )

(comment
  (require '[jansenh.transmodel.parser.core :as parser])
  (require '[jansenh.transmodel.parser.utilities :as u])

  (let [pd (parser/parse-xml-file shared-data-file)
        ;; Find first Operator
        ops (u/find-all-tags pd (keyword "xmlns.http%3A%2F%2Fwww.netex.org.uk%2Fnetex" "Operator"))
        first-op (first ops)
        _ (println "First operator:" first-op)
        name-elem (u/find-child first-op (keyword "xmlns.http%3A%2F%2Fwww.netex.org.uk%2Fnetex" "Name"))
        _ (println "Name element:" name-elem)
        _ (println "Name element content:" (:content name-elem))]
    nil)

  ;;
  ;; ------------------------------------------------------------------> comment
  )

(comment
  (reg/reset-registry!)
  (reg/load-file! shared-data-file)
  (reg/load-line-file! line-data-file)

  (reg/stats)

  ;; Check a stop point
  (reg/stop-point "KOL:ScheduledStopPoint:11416420_0")

  (def service-journey "KOL:ServiceJourney:5900_250619122712490_1010")

  ;; Check an operator
  (reg/operator "KOL:Operator:316")

  ;; Check a service journey with times
  (let [sj (reg/service-journey "KOL:ServiceJourney:5900_250619122707475_1002")]
    (clojure.pprint/pprint (:passing-times sj)))

  ;;
  ;; ------------------------------------------------------------------> comment
  )


(comment
  (require '[jansenh.transmodel.netex.interchanges :as interchanges])

  ;; Check stats
  (interchanges/interchange-stats)

  ;; Visualize a specific journey
  (interchanges/visualize-journey-interchanges
   "KOL:ServiceJourney:5900_250619122707475_1002")
  
  ;; Visualize a specific journey
  (interchanges/visualize-journey-interchanges
   "KOL:ServiceJourney:5900_250619122707482_2002")


  (do 
    (let [sj (reg/service-journey "KOL:ServiceJourney:5900_250619122707475_1002")]
      (clojure.pprint/pprint (:passing-times sj)))

    (println "--------------------------------------------------------------------------------")

    (let [sj (reg/service-journey "KOL:ServiceJourney:5900_250619122707482_2002")]
      (clojure.pprint/pprint (:passing-times sj)))

    )

  
  ;; Visualize all interchanges (first 5)
  (let [all-ic (reg/all-interchanges)]
    (doseq [ic (take 100 all-ic)]
      (interchanges/visualize-interchange ic)))



  (let [sj (reg/service-journey "KOL:ServiceJourneyInterchange:3244")]
    (clojure.pprint/pprint (:passing-times sj)))
  
  ;;
  ;; ------------------------------------------------------------------> comment
  ;;
  )
;;; 
