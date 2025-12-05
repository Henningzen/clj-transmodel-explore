(ns notebooks.netex
  (:require [jansenh.transmodel.parser.core :as parser]
            [jansenh.transmodel.parser.calendar :as cal]
            [jansenh.transmodel.generator.timetable :as tt]
            [jansenh.transmodel.netex.registry :as reg]
            [jansenh.transmodel.netex.line :as line]
            [jansenh.transmodel.netex.explore :as exp]
            [jansenh.transmodel.netex.extract :as ext]
            [jansenh.transmodel.netex.interchanges :as intrc]
            [jansenh.transmodel-explore.api :as api]
            [scicloj.clay.v2.api :as clay]
            [scicloj.clay.v2.main]
            [scicloj.kindly.v4.kind :as kind]
            [tablecloth.api :as tc]
            [clojure.string :as str]))


;;  NeTEx explore notebook
;;  ======================
;;
;;    Notebook for NeTEx dataset exploration.
;;
;;    The implementation is based on Clojure with Noj, Clay, TableCloth et al.
;;    Visualization tools in use are from the JavaScript sphere of tooling.
;;
;;    The api refered in the :require section is a wrapper of a NeTEx parsing
;;    library https://github.com/Henningzen/clj-transmodel
;;
;;    The library is available under EPL 2.0 License but not yet made public.
;;
;; -----------------------------------------------------------------------------
;;  Henning Jansen 2025  Copyright © henning.jansen@jansenh.no  
;;  Distributed under the Eclipse Public License version 2.0 as
;;  described in the README file under the root of this project.


  (def shared-data-file "/home/jansenh/data/rb_norway-aggregated-netex/_KOL_shared_data.xml")
  (def line-data-file
    "/home/jansenh/data/rb_norway-aggregated-netex/KOL_KOL-Line-8_5986_1025_Fogn---Judaberg---Helgoy.xml")

  (def shared-data (parser/parse-xml-file shared-data-file))
  (def line-data (parser/parse-xml-file line-data-file))


  ;; Set date ranges
  (def date-range (cal/weeks-ahead 6))
  (def from-date (:from date-range))
  (def to-date (:to date-range))

  ;; Build calendar index

  (def calendar-index (cal/build-calendar-index shared-data))
date-range
(:stats calendar-index)
;; Check stats calendar-index


  































(comment
  "Clay specifics
   --------------
   - rendering and behaviour"
  (clay/make! {:format [:html]
               :source-path "notebooks/netex.clj"
               :title "NeETx Transmodel"
               :browse true
               :show true
               :live-reload true
               :hide-ui-header false
               :hide-info-line true
               :hide-code true})
  
  ;;
  ;;------------------------------------------------------------------>  comment
  ;;
  )


