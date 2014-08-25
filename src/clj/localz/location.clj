(ns clj.localz.location)

(defn decimal->degrees [value]
  (let [degrees (int value)
        minutes (int (* 60 (- value degrees)))]
    [degrees minutes]))

(defn location-sector-id [{:keys [latitude longitude] :as location}]
  (let [latitude (decimal->degrees latitude)
        longitude (decimal->degrees longitude)]
    (vec (concat latitude longitude))))

