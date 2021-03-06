(ns frontend.components.integrations
  (:require [cljs.core.async :as async :refer [>! <! alts! chan sliding-buffer close!]]
            [clojure.string :as str]
            [frontend.async :refer [raise!]]
            [frontend.components.common :as common]
            [frontend.components.plans :as plans-component]
            [frontend.components.shared :as shared]
            [frontend.state :as state]
            [frontend.stefon :as stefon]
            [frontend.utils :as utils :include-macros true]
            [frontend.utils.ajax :as ajax]
            [frontend.utils.github :as gh-utils]
            [om.core :as om :include-macros true])
  (:require-macros [frontend.utils :refer [defrender html]]
                   [cljs.core.async.macros :as am :refer [go go-loop alt!]]))

(def circle-logo
  [:svg#Layer_1 {:y "0px", :xml:space "preserve", :x "0px", :viewBox "0 0 100 100", :version "1.1", :enable-background "new 0 0 100 100"}
   [:path#mark_21_ {:fill "#054C64", :d "M49.5,39.2c5.8,0,10.5,4.7,10.5,10.5s-4.7,10.5-10.5,10.5c-5.8,0-10.5-4.7-10.5-10.5\n\tS43.7,39.2,49.5,39.2z M49.5,5.5c-20.6,0-38,14.1-42.9,33.2c0,0.2-0.1,0.3-0.1,0.5c0,1.2,0.9,2.1,2.1,2.1h17.8\n\tc0.9,0,1.6-0.5,1.9-1.2c0,0,0-0.1,0-0.1c3.7-7.9,11.7-13.4,21-13.4c12.8,0,23.2,10.4,23.2,23.2C72.7,62.6,62.3,73,49.5,73\n\tc-9.3,0-17.4-5.5-21-13.4c0,0,0-0.1,0-0.1c-0.3-0.7-1.1-1.2-1.9-1.2H8.7c-1.2,0-2.1,0.9-2.1,2.1c0,0.2,0,0.4,0.1,0.5\n\tC11.6,79.9,28.9,94,49.5,94C74,94,93.8,74.2,93.8,49.8S74,5.5,49.5,5.5z"}"/"]])

(def docker-logo
  [:svg#Layer_1 {:y "0px", :x "0px", :viewBox "0 0 100 100", :version "1.1", :space "preserve", :enable-background "new 0 0 100 100"}
   [:g
    [:rect {:y "39.6", :x "41.4", :width "9", :height "9", :fill "#054C64"}]
    [:rect {:y "39.6", :x "51.7", :width "9", :height "9", :fill "#054C64"}]
    [:rect {:y "39.6", :x "31", :width "9", :height "9", :fill "#054C64"}]
    [:rect {:y "39.6", :x "20.7", :width "9", :height "9", :fill "#054C64"}]
    [:rect {:y "39.6", :x "10.4", :width "9", :height "9", :fill "#054C64"}]
    [:rect {:y "29.2", :x "41.4", :width "9", :height "9", :fill "#054C64"}]
    [:rect {:y "18.8", :x "41.4", :width "9", :height "9", :fill "#054C64"}]
    [:rect {:y "29.2", :x "31", :width "9", :height "9", :fill "#054C64"}]
    [:rect {:y "29.2", :x "20.7", :width "9", :height "9", :fill "#054C64"}]
    [:path {:fill "#054C64",
            :d "M6,70.5l-0.2-0.3c-0.6-0.8-1.1-1.6-1.6-2.4l-0.8-1.4c-0.4-0.8-0.8-1.7-1.2-2.7l-0.1-0.2\r\n\t\tc0-0.1-0.1-0.3-0.1-0.4c-0.1-0.2-0.2-0.5-0.2-0.7c-1-2.9-1.4-6.1-1.4-9.4c0-1,0.1-1.8,0.1-2.6l0.1-0.7h67.1c4.9,0,9.6-1.7,12.1-3.6\r\n\t\tc-1.6-1.7-2.6-4.1-2.9-7c-0.3-3.6,0.6-7.2,2.3-9.2l0.5-0.6l0.6,0.5c2,1.6,6.2,5.6,6.2,11c1.8-0.7,3.8-1,5.8-1\r\n\t\tc2.5,0,4.8,0.5,6.7,1.7l0.6,0.4l-0.3,0.7c-2.2,4.3-6.8,6.7-12.9,6.7l0,0c-0.6,0-1.3,0-1.9-0.1c-8.2,21.2-26.5,32.9-51.5,32.9\r\n\t\tc-4.4,0-8.6-0.6-12.4-1.8c-5.4-1.7-9.8-4.5-13.2-8.3l-0.7-0.8 M37.1,80.4c-6.1-2.7-9.5-6.5-11.5-10.8c-2.3,0.7-4.9,1.2-8.1,1.4\r\n\t\tc-1.2,0.1-2.4,0.1-3.7,0.2c-1.5,0-3.1,0-4.8,0c5.7,5.4,12.6,9.5,25.3,9.3C35.3,80.5,36.2,80.5,37.1,80.4z M30.3,63.5\r\n\t\tc-1.4,0-2.4,1.1-2.4,2.4c0,1.4,1.1,2.4,2.4,2.4c1.4,0,2.4-1.1,2.4-2.4C32.7,64.6,31.7,63.5,30.3,63.5"}]
    [:path {:fill-rule "evenodd",
            :fill "#054C64",
            :d "M30.3,64.2c0.2,0,0.4,0,0.6,0.1c-0.2,0.1-0.3,0.3-0.3,0.6\r\n\t\tc0,0.4,0.3,0.7,0.7,0.7c0.3,0,0.5-0.1,0.6-0.4c0.1,0.2,0.2,0.4,0.2,0.7c0,1-0.8,1.8-1.8,1.8c-1,0-1.7-0.8-1.7-1.8\r\n\t\tC28.6,64.9,29.4,64.2,30.3,64.2",
            :clip-rule "evenodd"}]]])

(def selenium-logo
  [:svg.selenium {:viewBox "0 0 100 100"}
   [:path {:d "M64.6,75.6c0,2.7-3.3,5.6-7.7,5.6 c-5.7,0-9.7-3-9.7-11.5c0-6.8,2.9-11.5,9.7-11.5c9.2,0,8,10.8,8,10.8H47.6 M15.9,73.9c0,4.1,5.2,6.8,10.9,6.8s9.6-3.1,9.6-8 s-3.7-6-9.6-7.8c-5.9-1.9-9.6-2.8-9.6-7.8s3.5-7.4,9.6-7.4c6.7,0,9.6,3.2,9.6,5.2 M44.3,33c0,0,12.7,12.7,12.7,12.7l38-38 M81.3,14.9c-2.3-2-5.4-3.3-8.7-3.3H18.9C11.5,11.7,5,17.9,5,25.2V79c0,7.4,6.5,13.3,13.9,13.3h53.8c7.4,0,13-5.9,13-13.3V25.2 c0-0.7,0.1-1.5,0-2.2"}]])

(def ie-logo
  [:svg.browser.ie {:viewBox "30 30 240 240"} [:path {:d "M256.25,151.953c0-16.968-4.387-32.909-12.08-46.761c32.791-74.213-35.136-63.343-38.918-62.603   c-14.391,2.816-27.705,7.337-39.986,13.068c-1.811-0.102-3.633-0.158-5.469-0.158c-45.833,0-84.198,31.968-94.017,74.823   c24.157-27.101,41.063-38.036,51.187-42.412c-1.616,1.444-3.198,2.904-4.754,4.375c-0.518,0.489-1.017,0.985-1.528,1.477   c-1.026,0.987-2.05,1.975-3.05,2.972c-0.595,0.593-1.174,1.191-1.76,1.788c-0.887,0.903-1.772,1.805-2.638,2.713   c-0.615,0.645-1.215,1.292-1.819,1.938c-0.809,0.866-1.613,1.733-2.402,2.603c-0.613,0.676-1.216,1.352-1.818,2.03   c-0.748,0.842-1.489,1.684-2.22,2.528c-0.606,0.7-1.207,1.4-1.801,2.101c-0.693,0.818-1.377,1.636-2.054,2.454   c-0.599,0.724-1.196,1.447-1.782,2.17c-0.634,0.782-1.254,1.563-1.873,2.343c-0.6,0.756-1.2,1.511-1.786,2.266   c-0.558,0.719-1.1,1.435-1.646,2.152c-0.616,0.81-1.237,1.62-1.837,2.426c-0.429,0.577-0.841,1.148-1.262,1.723   c-3.811,5.2-7.293,10.3-10.438,15.199c-0.008,0.012-0.016,0.024-0.023,0.036c-0.828,1.29-1.627,2.561-2.41,3.821   c-0.042,0.068-0.086,0.137-0.128,0.206c-0.784,1.265-1.541,2.508-2.279,3.738c-0.026,0.043-0.053,0.087-0.079,0.13   c-1.984,3.311-3.824,6.503-5.481,9.506C51.412,176.348,47.183,187.347,47,188c-27.432,98.072,58.184,56.657,70.131,50.475   c12.864,6.355,27.346,9.932,42.666,9.932c41.94,0,77.623-26.771,90.905-64.156h-50.68c-7.499,12.669-21.936,21.25-38.522,21.25   c-24.301,0-44-18.412-44-41.125h137.956C255.979,160.308,256.25,156.162,256.25,151.953z M238.232,57.037   c8.306,5.606,14.968,14.41,3.527,44.059c-10.973-17.647-27.482-31.49-47.104-39.099C203.581,57.686,225.686,48.568,238.232,57.037z    M61.716,238.278c-6.765-6.938-7.961-23.836,6.967-54.628c7.534,21.661,22.568,39.811,42,51.33   C101.019,240.299,75.363,252.275,61.716,238.278z M117.287,138c0.771-22.075,19.983-39.75,43.588-39.75   c23.604,0,42.817,17.675,43.588,39.75H117.287z"}]])

(def safari-logo
  [:svg.browser.safari [:path {:d "M81.8,81.8c-17.6,17.6-46.1,17.6-63.6,0 s-17.6-46.1,0-63.6s46.1-17.6,63.6,0S99.4,64.2,81.8,81.8z M44.7,44.7l-23,33.6l33.6-23l23-33.6L44.7,44.7z M78.3,21.7L21.7,78.3 M55.3,55.3L44.7,44.7 M21.7,21.7l5.7,5.7 M34.7,13l3.1,7.4 M50.1,10l-0.1,8 M64.7,12.8l-3.1,7.4 M13,65.3l7.4-3.1 M18,50.1l-8-0.1 M12.8,35.3l7.4,3.1 M72.6,72.6l5.7,5.7 M62.2,79.6l3.1,7.4 M49.9,90l0.1-8 M38.3,79.8l-3.1,7.4 M79.6,37.8l7.4-3.1 M82,49.9l8,0.1 M87.2,64.7l-7.4-3.1"}]])

(def chrome-logo
  [:svg.browser.chrome [:path {:d "M95,50c0,24.9-20.1,45-45,45S5,74.9,5,50 S25.1,5,50,5S95,25.1,95,50z M50,32.5c-9.7,0-17.5,7.8-17.5,17.5S40.3,67.5,50,67.5S67.5,59.7,67.5,50S59.7,32.5,50,32.5z M50,32.5 h41.5 M44.4,94.7l20.7-35.9 M14.1,22.8l20.7,35.9"}]])

(def firefox-logo
  [:svg.browser.firefox [:path {:d "M95,50c0,24.9-20.1,45-45,45 C25.1,95,5,74.8,5,50c0-7,1.6-13.7,4.5-19.6c0-0.2,0-0.5,0-0.7c-0.1-0.8-0.1-1.6-0.1-2.4c0,0-0.1-8.8,4.9-13.4 c0.1,1.6,0.5,3.6,1.2,4.9c1.8,2.8,3.1,3.5,3.8,4c3-1,6.6-1.2,10.8-0.2c0.4-0.4,6.6-6.5,12.3-5.1c-1.9,1.1-5.4,5-6.3,8.9 c0.1,0.2,0.2,0.5,0.4,0.8c0.1,0.2,0.3,0.4,0.4,0.7c0.8,1.1,2.1,2.2,4,2.2c7.2-0.1,7.7,0.5,7.8,1.3c0.1,1.2-0.2,2-0.5,2.6 c-0.4,0.9-1.2,1.7-1.9,2.2c-0.5,0.4-7.1,3.8-7.5,5.1c0.4,3.7-0.2,6.2-0.2,6.2s-0.3-0.4-1.1-0.8c-0.6-0.3-1.9-0.7-2.4-0.9 c-2,0.9-2.5,2.4-2.5,3.4c0,1.3,0.9,4.3,5.4,7c4.7,1.9,9,0,12.4-0.8c4.4-1.1,7.6,1,9,2.5c1.3,1.5,0.2,3.3-1.5,2.9 c-1.7-0.4-3.6,1.2-6.9,3.5c-3.2,2.2-8.8,3.1-14,1.8c13.7,15.1,37.2,11.3,38.3-10.8c0.3,0.7,0.9,1.9,1.1,3.5 c0.9-3.5,2.1-6.3,0.7-19.5c1.3,1.2,3.2,4.6,4.3,6.7c0.7-13.4-6-22.3-10-25.2c2.7,0.2,6.6,2.7,9,5C80,25,79.5,24.1,79,23.3 c-2.1-3.7-5.6-7.7-12.6-12.4c0,0,6.1,0,11.7,3.9c6,4.8,10.7,11.2,13.6,18.4C93.8,38.4,95,44.1,95,50z M72.4,11.1 C65.8,7.1,58.2,5,50,5c-13,0-24.7,5.4-32.9,14.2"}]])

(defn cta-form [app owner]
  (reify
    om/IInitState
    (init-state [_]
                {:email ""
                 :use-case ""
                 :notice nil
                 :loading? false})
    om/IRenderState
    (render-state [_ {:keys [email use-case notice loading?]}]
                  (let [clear-notice! #(om/set-state! owner [:notice] nil)
                        clear-form! (fn [& [notice]]
                                      (om/update-state! owner (fn [s]
                                                                (merge s
                                                                       (when notice {:notice notice})
                                                                       {:email ""
                                                                        :use-case ""
                                                                        :loading? false}))))]
                    (html
                      [:form
                       [:input {:name "Email",
                                :required true
                                :value email
                                :on-change #(do (clear-notice!) (om/set-state! owner [:email] (.. % -target -value)))
                                :type "text"}]
                       [:label {:alt "Email (required)", :placeholder "Email"}]
                       [:textarea
                        {:name "docker_use__c",
                         :required true
                         :value use-case
                         :on-change #(do (clear-notice!) (om/set-state! owner [:use-case] (.. % -target -value)))
                         :type "text"}]
                       [:label {:placeholder "How do you want to use it?"}]
                       [:div.notice
                        (when notice
                          [:span {:class (:type notice)} (:message notice)])]
                       [:div.submit
                        [:input
                         {:value (if loading? "Submitting.." "Submit"),
                          :class (when loading? "disabled")
                          :on-click #(do (if (or (empty? email) (not (utils/valid-email? email)))
                                           (om/set-state! owner [:notice] {:type "error"
                                                                           :message "Please enter a valid email address."})
                                           (do
                                             (om/set-state! owner [:loading?] true)
                                             (go (let [resp (<! (ajax/managed-form-post "/about/contact"
                                                                                        :params {:email email
                                                                                                 :message {:docker_use__c use-case}}))]
                                                   (if-not (= :success (:status resp))
                                                     (om/update-state! owner (fn [s]
                                                                               (merge s {:loading? false
                                                                                         :notice {:type "error"
                                                                                                  :message "Sorry! There was an error submitting the form. Please try again or email sayhi@circleci.com."}})))
                                                     (clear-form! {:type "success"
                                                                   :message "Thanks! We will be in touch soon."}))))))
                                       false)
                          :type "submit"}]]])))))

(defn docker [app owner]
  (reify
    om/IRender
    (render [_]
      (html
       [:div#integrations.docker
        [:div.section-container
         [:section.integrations-hero-wrapper
          [:article.integrations-hero-title
           [:h1 "Build and deploy Docker containers on CircleCI."]]
          [:article.integrations-hero-units
           [:div.integrations-hero-unit
            circle-logo
            [:p "CircleCI makes modern Continuous Integration and Deployment easy."]]
           [:div.integrations-hero-unit
            docker-logo
            [:p "Docker makes it easy to build, run, and ship applications anywhere.  "]]]
          [:h3.message
           "Together they let you achieve development-production parity. At last.
                    "]]]
        [:div.section-container
         [:section.integrations-hero-wrapper
          [:div.docker-features
           [:div.feature-container
            [:div.feature
             [:img {:src (utils/cdn-path "/icons/cloud-circle.png")}]
             [:h3 "Continuous Deployment of your Docker images"]
             [:p
              "CircleCI makes it easy to deploy images to Docker Hub as well as to continuously deploy applications to AWS Elastic Beanstalk, Google Compute Engine, and others."]
             [:a {:href "/docs/docker"} "docs"]]
            [:div.feature
             [:img {:src (utils/cdn-path "/icons/scale-circle.png")}]

             [:h3 "Dev-production parity"]
             [:p
              "Docker containers let you remove almost all of the variables that differ between your test and production environments. You can specify everything from your Linux distro to what executables run at startup in a Dockerfile, build all of that information into a Docker image, test it, and deploy the exact same image byte-for-byte to production. You can now run this entire process on CircleCI."]
             [:a {:href "/docs/docker"} "docs"]]
            [:div.feature
             [:img {:src (utils/cdn-path "/icons/wrench-circle.png")}]
             [:h3 "Full Docker functionality"]
             [:p
              "You can now use all Docker functionality within the CircleCI build environment. All of the usual Docker command-line commands work as expected, so you can build and run Docker containers to your heart's content."]
             [:a {:href "/docs/docker"} "docs"]]]]]]
        [:div.section-container
         [:section.integrations-hero-wrapper
          [:p.center-text
           "Docker is enabled for all current CircleCI users, everyone else can sign up below!"]
          [:div.docker-cta
           [:div.ctabox {:class "line"}
            [:div
             [:p "All customers get a free container. Additional containers are $50/month."]]
            (shared/home-button owner {:source "integrations/docker"})
            [:div
             [:p
              [:i "CircleCI keeps your code safe. "
               [:a {:href "/security" :title "Security"} "Learn how."]]]]]]]]]))))

(def integration-data
  {:heroku {:hero {:header [:span
                            "Deploy to "
                            [:img {:src (utils/cdn-path "img/outer/integrations/heroku-icon.svg")
                                   :alt "Heroku"
                                   :style {:vertical-align "-35%"}}]
                            " from CircleCI"]
                   :text "Experience a simple, modern continuous delivery workflow now."}
            :bullets [{:title "Test before you deploy. Always."
                       :text [:span
                              "Heroku revolutionized the way developers think about deployment. "
                              "Being able to deploy with a simple "
                              [:code "git push heroku master"]
                              " is an amazing thing. But setting up a proper continuous delivery "
                              "workflow means automating every step of the process. With CircleCI "
                              "whenever you push a commit to master, it will go through a complete "
                              "continuous delivery pipeline. All of your tests will run with our "
                              "blazing fast parallelism, and" [:em " only if they pass, "]
                              "your code will be pushed to Heroku automatically."]
                       :graphic [:img {:src (stefon/data-uri "/img/status-logos/success.svg")}]}
                      {:title "Dead Simple Configuration"
                       :text [:span
                              "The deployment of your application is configured through just a "
                              "few lines of YAML that are kept safe in your source code. All "
                              "you need to do to deploy to Heroku from CircleCI is configure your "
                              "Heroku credentials in our UI, add a simple config file like this "
                              "one into your project, and make a push. You can also easily deploy "
                              "different branches to different Heroku apps (e.g. one for staging "
                              "and one for production)."]
                       :graphic [:div.console
                                 [:pre
                                  "deployment:\n"
                                  "  staging:\n"
                                  "    branch: " [:span.value "master"] "\n"
                                  "      heroku:\n"
                                  "        appname: " [:span.value "my-app"]]]}
                      {:title "Watch how to get started in minutes"
                       :text [:span
                              "This video shows step-by-step how to configure CircleCI to test "
                              "your application and deploy to Heroku, and how CircleCI keeps "
                              "defects from getting into production. "
                              "See our docs for a "
                              [:a {:href (str "/docs/continuous-deployment-with-heroku#part-2-multiple-environments")}
                               "followup video"]
                              " showing how to setup a more robust continuous delivery pipeline "
                              "with staging and prod environments."]
                       :graphic [:div {:dangerouslySetInnerHTML {:__html "<iframe src='//www.youtube.com/embed/Hfs_1yuWDf4?rel=0&showinfo=0' width='300' height='200' frameborder='0' allowfullscreen></iframe>"}}]}]
            :bottom-header "Ready for world-class continuous delivery?"
            :secondary-cta [:span
                            "Or see our "
                            [:a {:href "/docs/continuous-deployment-with-heroku"}
                             "docs on deploying to Heroku."]]}
   :saucelabs {:hero {:header [:span
                               "Test with "
                               [:img {:src (utils/cdn-path "/img/outer/integrations/sauce.png")
                                      :alt "Sauce Labs" :style {:width "300px"}}]
                               " on CircleCI"]
                      :text "Test against hundreds of mobile and desktop browsers."}
               :bullets [{:title "Selenium WebDriver"
                          :text [:span
                                 "Sauce Labs supports automated browser tests using Selenium "
                                 "WebDriver, a widely-adopted browser driving standard. Selenium "
                                 "WebDriver provides a common API for programatically driving "
                                 "browsers implemented in several popular languages, including "
                                 "Java, Python, and Ruby. WebDriver can operate in two modes: "
                                 "local or remote. When run locally, your tests use the Selenium "
                                 "WebDriver library to communicate directly with a browser on the "
                                 "same machine. When run in remote mode, your tests interact with "
                                 "a Selenium Server, and it it is up to the server to drive the "
                                 "browsers. Sauce Labs essentially provides a Selenium Server as a "
                                 "service, with all kinds of browsers available to test. It has "
                                 "some extra goodies like videos of all test runs as well."]
                          :graphic selenium-logo}
                         {:title "All the browsers and platforms you need"
                          :text [:span
                                 "Sauce Labs provides a huge variety of browsers and operating "
                                 "systems. You can choose between combinations of Firefox, Chrome, "
                                 "Safari, and Internet Explorer browsers and OSX, Windows, and "
                                 "Linux operating systems. You can also test against mobile Safari "
                                 "and Android browsers. Pick whatever browsers are important for "
                                 "you, whether you need to ensure critical functionality works on "
                                 "mobile devices or support old versions of IE. Because Selenium "
                                 "WebDriver provides a unified interface to talk to all of these "
                                 "browsers, you only need to write your browser tests once, and "
                                 "you can run them on as many browsers and platforms as you want."]
                          :graphic [:div.browsers firefox-logo safari-logo  ie-logo chrome-logo]}
                         {:title "Test Continuously"
                          :text [:span "CircleCI automatically runs all your tests, against "
                                 "whatever browsers you choose, every time you commit code. You "
                                 "can configure your browser-based tests to run whenever a change "
                                 "is made, before every deployment, or on a certain branch. A "
                                 "Continuous Integration and Delivery workflow with CircleCI and "
                                 "Sauce Labs ensures that browser-specific bugs affecting critical "
                                 "functionality in your app never make it to production."]
                          :graphic [:img {:src (utils/cdn-path "/img/outer/integrations/cycle-black.svg") :style {:width "250px" :height "250px"}}]}
                         {:title "No public test servers required"
                          :text [:span "Sauce Labs operates browsers on a network separate from "
                                 "CircleCI build containers, but there needs to be a way for the "
                                 "browsers to access the web application you want to test. The "
                                 "easiest way to do this is to simply run your server during a "
                                 "CircleCI build and use Sauce Connect to setup a secure tunnel "
                                 "between Sauce Labs' browsers and your build containers on "
                                 "CircleCI. There is an in-depth example of this in "
                                 [:a {:href "/docs/browser-testing-with-sauce-labs"} "our docs."]]
                          :graphic [:div.sauce-connect [:p "Sauce" [:br] "Connect"]]}]
               :bottom-header "Want to get rid of browser bugs?"
               :secondary-cta [:span "Or see our " [:a {:href "/docs/browser-testing-with-sauce-labs"} "docs on Sauce Labs."]]}})

(defrender integration [app owner]
  (let [integration (get-in app [:navigation-data :integration])]
    (if (= integration :docker)
      (om/build docker app)
      (let [data (get integration-data integration)]
        (html [:div#integrations.generic
               [:div.top-section {:class (name integration)}
                [:div.line]
                [:div.hero-wrapper
                 [:div.hero
                  [:div.integration-icon
                   [:img {:src (utils/cdn-path "/img/outer/integrations/integration-icon.svg")}]]
                  [:div
                   [:h1 (get-in data [:hero :header])]
                   [:p (get-in data [:hero :text])]]
                  [:a {:href (gh-utils/auth-url)
                       :role "button"
                       :on-click #(raise! owner [:track-external-link-clicked {:path (gh-utils/auth-url)
                                                                               :event "Auth GitHub"}])}
                   "Sign up for CircleCI"]]]]
               [:div.middle-section
                (for [bullet (:bullets data)]
                  [:div.bullet
                   [:div.graphic
                    (:graphic bullet)]
                   [:div.content
                    [:h2 (:title bullet)]
                    [:p (:text bullet)]]])]
               [:div.bottom-section
                [:h2 (:bottom-header data)]
                [:a {:href (gh-utils/auth-url)
                       :role "button"
                       :on-click #(raise! owner [:track-external-link-clicked {:path (gh-utils/auth-url)
                                                                               :event "Auth GitHub"}])}
                   "Sign up for Free"]
                [:p (:secondary-cta data)]]])))))
