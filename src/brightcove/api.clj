(ns brightcove.api
    "API wrapper library for accessing a sub-set of the Brightcove Media API Ð Video Read API."
    (:use clojure.contrib.json))

(def BASE_URL "http://api.brightcove.com/services/library")

(defn- fetch-url
  "Retrieve the contents of the given URL and returns the content as a string."
  [address]
    (with-open [stream (.openStream (java.net.URL. address))]
        (let  [buf (java.io.BufferedReader. (java.io.InputStreamReader. stream))]
            (apply str (line-seq buf)))))

(defn- get-page
  "Returns a lazy sequence of pages of content."
    ([url] (get-page url 0))
    ([url start-page-id]
        ((fn page [page-id]
            (let [page-url (format "%s&page_number=%d" url page-id)
                  data (read-json (fetch-url page-url))
                  last-page (Math/ceil (/ (get data :total_count 0) (get data :page_size 100)))]
;                (println "request: " page-url)
;                (println "response:" (get data :page_number) last-page)
                (cons
                    (if (> (get data :total_count 0) 0) data nil)
                    (if (< (get data :page_number 0) last-page) (lazy-seq (page (inc page-id))) nil)))) start-page-id)))

(defn- make-query-string
  "Convert the given map to a valid HTTP query string. All keys and values are encoded using the given encoding or UTF-8 if not specified."
  [m & [encoding]]  
  (let [s #(if (instance? clojure.lang.Named %) (name %) %)
        enc (or encoding "UTF-8")]
    (->> (for [[k v] m]
           (str (s k) "=" v))
         (interpose "&")
         (apply str))))

(defn- build-url
  "Returns a string concatenating the given base URL with a query string conversion of the parameter map."
  [url-base query-map & [encoding]]
  (str url-base "?" (make-query-string query-map encoding)))

(defn find-all-videos
  "Find all video for the given read token. Additional parameters can be supplied and can also be used to override the default
   item sorted order of ascending creation date."
  [token & [params]]
    (let [api_params (merge {:command "find_all_videos", :token token, :get_item_count "true", :sort_by "CREATION_DATE", :sort_order "ASC"} params)]
        (get-page (build-url BASE_URL api_params))))

(defn find-modified-videos
  "Find all modified videos for the given read token and timestamp. Note: timestamp must be specified in epoch *minutes*. Additional
   parameters may also be specified."
  [token timestamp & [params]]  
    (let [api_params (merge {:command "find_modified_videos", :token token, :get_item_count "true", :from_date timestamp} params)]
        (get-page (build-url BASE_URL api_params))))

(defn find-video-by-id
  "Find a specific video for the given read token and video id. Additional parameters may also be specified."
  [token id & [params]]
    (let [api_params (merge {:command "find_video_by_id", :token token, :get_item_count "true", :video_id id} params)]
        (get-page (build-url BASE_URL api_params))))

(defn search-videos
  "Searches videos according to the given parameter map. Consider using the search_videos method for video searches rather
   than using the other find_video read methods. The search_videos method offers more flexible search and sorting options
   than the find_video methods."
  [token query-map]
    (let [params (concat
                   query-map
                   {:command "search_videos", :token token, :get_item_count "true"})]
            (get-page (build-url BASE_URL params))))  
             
  
