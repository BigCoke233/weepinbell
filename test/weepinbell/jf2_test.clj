(ns weepinbell.jf2-test
  (:require [clojure.test :refer [deftest testing is]]
            [hickory.core :as hickory]
            [weepinbell.metadata.microformats :refer [parse-microformats]]
            [weepinbell.metadata.json-ld :refer [parse-json-ld]]
            [weepinbell.metadata.html-head :refer [parse-meta]]
            [weepinbell.metadata.jf2 :refer [parse supplement-merge]]))

(def to-hick (comp hickory/as-hickory hickory/parse))

;; microformats -> JF2

(def test-data
  [{:in {:items [{:type ["h-entry"], :properties {:author [{:type ["h-card"], :properties {:name ["A. Developer"], :url ["https://example.com/"]}, :value "A. Developer"}], :name ["Hello World"], :summary ["Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus imperdiet ultrices pulvinar."], :url ["https://example.com/2015/10/21"], :published ["2015-10-21T12:00:00-0700"], :content [{:html "<p>Donec dapibus enim lacus, <i>a vehicula magna bibendum non</i>. Phasellus id lacinia felis, vitae pellentesque enim. Sed at quam dui. Suspendisse accumsan, est id pulvinar consequat, urna ex tincidunt enim, nec sodales lectus nulla et augue. Cras venenatis vehicula molestie. Donec sagittis elit orci, sit amet egestas ex pharetra in.</p>", :value "Donec dapibus enim lacus, a vehicula magna bibendum non. Phasellus id lacinia felis, vitae pellentesque enim. Sed at quam dui. Suspendisse accumsan, est id pulvinar consequat, urna ex tincidunt enim, nec sodales lectus nulla et augue. Cras venenatis vehicula molestie. Donec sagittis elit orci, sit amet egestas ex pharetra in."}]}}]}
    :out {:type "entry", :author {:type "card", :url "https://example.com", :name "A. Developer"}, :url "https://example.com/2015/10/21", :published "2015-10-21T12:00:00-0700", :name "Hello World", :summary "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus imperdiet ultrices pulvinar.", :content {:html "<p>Donec dapibus enim lacus, <i>a vehicula magna bibendum non</i>. Phasellus id lacinia felis, vitae pellentesque enim. Sed at quam dui. Suspendisse accumsan, est id pulvinar consequat, urna ex tincidunt enim, nec sodales lectus nulla et augue. Cras venenatis vehicula molestie. Donec sagittis elit orci, sit amet egestas ex pharetra in.</p>", :text "Donec dapibus enim lacus, a vehicula magna bibendum non. Phasellus id lacinia felis, vitae pellentesque enim. Sed at quam dui. Suspendisse accumsan, est id pulvinar consequat, urna ex tincidunt enim, nec sodales lectus nulla et augue. Cras venenatis vehicula molestie. Donec sagittis elit orci, sit amet egestas ex pharetra in."}, :wm-property :mention}}])

(deftest jf2-parse-microformats-test
  (testing "Parsing example microformats to J2F, from https://jf2.spec.indieweb.org/"
    (doseq [data test-data]
      (is (= (:out data)
             (parse-microformats (:in data)))))))

;; JSON-LD -> JF2

(def test-jsonld "
        <script type=\"application/ld+json\">{\n\t\"@context\": \"https://schema.org\",\n\t\"@type\": \"Article\",\n\t\"publisher\": {\n\t\t\"@type\": \"Organization\",\n\t\t\"name\": \"freeCodeCamp.org\",\n\t\t\"url\": \"https://www.freecodecamp.org/news/\",\n\t\t\"logo\": {\n\t\t\t\"@type\": \"ImageObject\",\n\t\t\t\"url\": \"https://cdn.freecodecamp.org/platform/universal/fcc_primary.svg\",\n\t\t\t\"width\": 2100,\n\t\t\t\"height\": 240\n\t\t}\n\t},\n\t\"image\": {\n\t\t\"@type\": \"ImageObject\",\n\t\t\"url\": \"https://www.freecodecamp.org/news/content/images/2020/03/open-graph.jpg\",\n\t\t\"width\": 600,\n\t\t\"height\": 400\n\t},\n\t\"url\": \"https://www.freecodecamp.org/news/what-is-open-graph-and-how-can-i-use-it-for-my-website/\",\n\t\"mainEntityOfPage\": {\n\t\t\"@type\": \"WebPage\",\n\t\t\"@id\": \"https://www.freecodecamp.org/news/\"\n\t},\n\t\"datePublished\": \"2020-03-26T14:51:33.000Z\",\n\t\"dateModified\": \"2020-03-26T14:51:33.000Z\",\n\t\"keywords\": \"Digital Marketing , HTML, marketing, open graph, social media, tech , Web Development\",\n\t\"description\": \"It can take a lot of time to build content and maintain a website. How can we make sure our content stands out when getting shared on social feeds around the internet?\\n\\nWhat is Open Graph?\\nWhy do I need it?\\nWhat happens if I don’t have it?\\nStarting w...\",\n\t\"headline\": \"What is Open Graph and how can I use it for my website?\",\n\t\"author\": {\n\t\t\"@type\": \"Person\",\n\t\t\"name\": \"Colby Fayock\",\n\t\t\"url\": \"https://www.freecodecamp.org/news/author/colbyfayock/\",\n\t\t\"sameAs\": [\n\t\t\t\"http://colbyfayock.com/\",\n\t\t\t\"https://x.com/colbyfayock\"\n\t\t],\n\t\t\"image\": {\n\t\t\t\"@type\": \"ImageObject\",\n\t\t\t\"url\": \"https://cdn.hashnode.com/res/hashnode/image/upload/v1601829804572/1eKdHWWIz.jpeg\",\n\t\t\t\"width\": 600,\n\t\t\t\"height\": 600\n\t\t}\n\t}\n}</script>
  ")

(def jsonld->jf2
  {:type "entry", :name "What is Open Graph and how can I use it for my website?", :url "https://www.freecodecamp.org/news/what-is-open-graph-and-how-can-i-use-it-for-my-website/", :author {:type "card", :name "Colby Fayock", :url "https://www.freecodecamp.org/news/author/colbyfayock/", :photo "https://cdn.hashnode.com/res/hashnode/image/upload/v1601829804572/1eKdHWWIz.jpeg"}, :published "2020-03-26T14:51:33.000Z", :content "", :wm-property :mention, :summary "It can take a lot of time to build content and maintain a website. How can we make sure our content stands out when getting shared on social feeds around the internet?\n\nWhat is Open Graph?\nWhy do I need it?\nWhat happens if I don’t have it?\nStarting w..."})

(def test-jsonld-html
  (str "<html><head>" test-jsonld "</head></html>"))

(deftest jf2-parse-jsonld-test
  (testing "parse JSON-LD from a document"
    (is (= (-> test-jsonld-html to-hick parse-json-ld)
           jsonld->jf2)))
  (testing "parse JSON-LD from raw script element"
    (is (= (-> test-jsonld to-hick parse-json-ld)
           jsonld->jf2))))

;; Open Graph, <head> tags -> JF2

(def link-and-meta-tags
  {:open-graph
   "<html><head>
      <title>Fallback title</title>
      <link rel=\"icon\" href=\"https://example.com/favicon.ico\" type=\"image/png\">
      <link rel=\"canonical\" href=\"https://example.com/open-graph\">
      <meta name=\"description\" content=\"Fallback description\">
      <meta property=\"og:title\" content=\"Open Graph title\">
      <meta property=\"og:url\" content=\"https://example.com/open-graph\">
      <meta property=\"og:description\" content=\"Open Graph description\">
      <meta property=\"article:author\" content=\"Open Graph author\">
      <meta property=\"article:published_time\" content=\"2024-01-02T03:04:05Z\">
    </head></html>"

   :twitter
   "<html><head>
      <title>Title fallback</title>
      <link rel=\"canonical\" href=\"https://example.com/twitter\">
      <meta name=\"twitter:title\" content=\"Twitter title\">
      <meta name=\"twitter:url\" content=\"https://example.com/twitter\">
      <meta name=\"twitter:description\" content=\"Twitter description\">
      <meta name=\"twitter:creator\" content=\"@writer\">
    </head></html>"

   :profile-username
   "<html><head>
      <meta property=\"og:title\" content=\"Profile title\">
      <meta property=\"profile:username\" content=\"profile-username\">
    </head></html>"

   :profile-fullname
   "<html><head>
      <meta property=\"profile:first_name\" content=\"Rich\">
      <meta property=\"profile:last_name\" content=\"Hickey\">
    </head></html>"

   :basic
   "<html><head>
      <title>Document title</title>
      <link rel=\"canonical\" href=\"https://example.com/cononical\">
      <meta name=\"description\" content=\"Document description\">
    </head></html>"})

(deftest jf2-parse-head-test
  (testing "Open Graph to JF2"
    (is (= (parse-meta (to-hick (:open-graph link-and-meta-tags)))
           {:type "entry"
            :name "Open Graph title"
            :url "https://example.com/open-graph"
            :author {:type "card"
                     :name "Open Graph author"
                     :url nil
                     :photo "https://example.com/favicon.ico"}
            :published "2024-01-02T03:04:05Z"
            :content ""
            :wm-property :mention
            :summary "Open Graph description"}))
    (is (= (parse-meta (to-hick (:twitter link-and-meta-tags)))
           {:type "entry"
            :name "Twitter title"
            :url "https://example.com/twitter"
            :author {:type "card"
                     :name "@writer"
                     :url nil
                     :photo nil}
            :published nil
            :content ""
            :wm-property :mention
            :summary "Twitter description"}))
    (is (= (parse-meta (to-hick (:profile-username link-and-meta-tags)))
           {:type "entry"
            :name "Profile title"
            :url nil
            :author {:type "card"
                     :name "profile-username"
                     :url nil
                     :photo nil}
            :published nil
            :content ""
            :wm-property :mention
            :summary nil}))
    (is (= (parse-meta (to-hick (:profile-fullname link-and-meta-tags)))
           {:type "entry"
            :name nil
            :url nil
            :author {:type "card"
                     :name "Rich Hickey"
                     :url nil
                     :photo nil}
            :published nil
            :content ""
            :wm-property :mention
            :summary nil}))
    (is (= (parse-meta (to-hick (:basic link-and-meta-tags)))
           {:type "entry"
            :name "Document title"
            :url "https://example.com/cononical"
            :author {:type "card"
                     :name nil
                     :url nil
                     :photo nil}
            :published nil
            :content ""
            :wm-property :mention
            :summary "Document description"}))))

;; utilities

(deftest supplement-merge-test
  (testing "supplement-merge supplements A with values A lacks but B has"
    (is (= (supplement-merge
            {:a nil :b 1} {:a 1 :b 3})
           {:a 1 :b 1})))
  (testing "supplement-merge never overrides existing values, except nil"
    (is (= (supplement-merge {:a "hi"} {:a "bye"}) {:a "hi"})))
  (testing "supplement-merge does deep merge as well"
    (is (= (supplement-merge {:a {:aa "hello" :ab nil}
                              :b nil}
                             {:a {:aa "byebye" :ab "yes"}
                              :b nil})
           {:a {:aa "hello" :ab "yes"} :b nil})))
  (testing "supplement-merge adds missing keys"
    (is (= (supplement-merge {:a "n"} {:a "m" :b "s"})
           {:a "n" :b "s"}))))

;; compile all sources

(def compiling-test-data
  [{:html "<html><head></head><body>
      <article class=\"h-entry\">
        <h1 class=\"p-name\">Microformats title</h1>
        <a class=\"u-url\" href=\"https://example.com/microformats\">Permalink</a>
        <time class=\"dt-published\" datetime=\"2024-02-03T04:05:06Z\">February 3</time>
        <div class=\"p-author h-card\">
          <span class=\"p-name\">Microformats author</span>
          <a class=\"u-url\" href=\"https://example.com/author\">Author</a>
        </div>
        <div class=\"e-content\">Microformats content</div>
        <a class=\"u-in-reply-to\" href=\"https://example.com/original\">Original</a>
      </article>
    </body></html>"
    :out {:type "entry"
          :name "Microformats title"
          :url "https://example.com/microformats"
          :author {:type "card"
                   :name "Microformats author"
                   :url "https://example.com/author"
                   :photo nil}
          :published "2024-02-03T04:05:06Z"
          :content {:html "Microformats content"
                    :text "Microformats content"}
          :wm-property :in-reply-to
          :in-reply-to ["https://example.com/original"]
          :summary nil}}
   {:html "<html><head>
      <title>Document title</title>
      <link rel=\"canonical\" href=\"https://example.com/head-url\">
      <meta property=\"og:title\" content=\"Open Graph title\">
      <meta property=\"og:url\" content=\"https://example.com/og-url\">
      <meta property=\"og:description\" content=\"Open Graph summary\">
      <meta property=\"article:author\" content=\"Head author\">
      <meta property=\"article:published_time\" content=\"2024-03-04T05:06:07Z\">
      <script type=\"application/ld+json\">{
        \"@context\": \"https://schema.org\",
        \"@type\": \"Article\",
        \"headline\": \"JSON-LD title\",
        \"url\": \"https://example.com/json-ld-url\",
        \"datePublished\": \"2024-04-05T06:07:08Z\",
        \"description\": \"JSON-LD summary\",
        \"author\": {
          \"@type\": \"Person\",
          \"name\": \"JSON-LD author\",
          \"url\": \"https://example.com/json-ld-author\",
          \"image\": {\"url\": \"https://example.com/author.jpg\"}
        }
      }</script>
    </head><body>
      <article class=\"h-entry\">
        <h1 class=\"p-name\">Microformats title</h1>
        <a class=\"u-url\" href=\"https://example.com/microformats-url\">Permalink</a>
        <time class=\"dt-published\" datetime=\"2024-01-02T03:04:05Z\">January 2</time>
        <div class=\"p-author h-card\">
          <span class=\"p-name\">Microformats author</span>
          <a class=\"u-url\" href=\"https://example.com/microformats-author\">Author</a>
        </div>
        <div class=\"e-content\">Microformats content</div>
      </article>
    </body></html>"
    :out {:type "entry"
          :name "Microformats title"
          :url "https://example.com/microformats-url"
          :author {:type "card"
                   :name "Microformats author"
                   :url "https://example.com/microformats-author"
                   :photo "https://example.com/author.jpg"}
          :published "2024-01-02T03:04:05Z"
          :content {:html "Microformats content"
                    :text "Microformats content"}
          :wm-property :mention
          :summary "Open Graph summary"}}
   {:html "<html><head>
      <meta name=\"description\" content=\"Head description\">
      <meta name=\"twitter:title\" content=\"Twitter title\">
      <meta name=\"twitter:url\" content=\"https://example.com/twitter-url\">
      <meta name=\"twitter:creator\" content=\"@head-author\">
      <script type=\"application/ld+json\">{
        \"@context\": \"https://schema.org\",
        \"@type\": \"Article\",
        \"headline\": \"JSON-LD title\",
        \"url\": \"https://example.com/json-ld-url\",
        \"datePublished\": \"2024-05-06T07:08:09Z\",
        \"author\": {
          \"@type\": \"Person\",
          \"name\": \"JSON-LD author\",
          \"url\": \"https://example.com/json-ld-author\"
        },
        \"description\": \"JSON-LD description\"
      }</script>
    </head><body></body></html>"
    :out {:type "entry"
          :name "Twitter title"
          :url "https://example.com/twitter-url"
          :author {:type "card"
                   :name "@head-author"
                   :url "https://example.com/json-ld-author"
                   :photo nil}
          :published "2024-05-06T07:08:09Z"
          :content ""
          :wm-property :mention
          :summary "Head description"}}])

(deftest jf2-parse-test
  (testing "compiles JF2 from all available metadata sources"
    (doseq [{:keys [html out]} compiling-test-data]
      (is (= out (parse html))))))
