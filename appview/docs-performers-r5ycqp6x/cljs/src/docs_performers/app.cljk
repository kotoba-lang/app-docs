(ns docs-performers.app
  "docs-performers-r5ycqp6x — reagent + re-frame port of the former Svelte
  frontend at appview/docs-performers-r5ycqp6x/svelte/src/.

  Two Svelte files existed:
    - `App.svelte`         — the actual scaffold markup + style (a centered
                              heading + message, \"Vite entry scaffold after
                              SvelteKit cleanup.\").
    - `routes/+page.svelte` — SvelteKit's route entry, which did nothing but
                              `import App from '../App.svelte'` and render
                              `<App />`. It carried no logic or markup of its
                              own beyond that composition.

  Both are ported here: `app-view` is the faithful port of `App.svelte`'s
  markup, and `main` (mount at document load) is the equivalent of the
  route rendering `<App />` as the app's one and only screen. There was no
  second distinct view to preserve as SPA-state data (ADR-2608080100) —
  the route was a pure re-export of the same component, so a single
  `app-view` mounted once is a faithful port, not a simplification."
  (:require [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [jp-go-dds.core :as dds]))

;; --- db ------------------------------------------------------------------

(def initial-db
  {:title "docs-performers-r5ycqp6x"
   :subtitle "Vite entry scaffold after SvelteKit cleanup."})

(rf/reg-event-db
 ::initialize
 (fn [_ _] initial-db))

(rf/reg-sub ::title (fn [db _] (:title db)))
(rf/reg-sub ::subtitle (fn [db _] (:subtitle db)))

;; --- view ------------------------------------------------------------------
;;
;; Faithful port of App.svelte: a full-viewport centered column with a
;; heading and a paragraph message (the Svelte version used
;; `min-height: 100vh; display: grid; place-content: center` on `main`;
;; `dds-ext-hero dds-ext-center` gives the same centered-column composition
;; through jp-go-dds's layout extension classes instead of hand-rolled CSS).

(defn app-view []
  (let [title @(rf/subscribe [::title])
        subtitle @(rf/subscribe [::subtitle])]
    [:main
     (dds/container
      (dds/section {}
        [:div {:class "dds-ext-hero dds-ext-center"}
         (dds/heading 1 title)
         [:p {:class "dds-ext-lead"} subtitle]]))]))

(defn ^:dev/after-load render! []
  (rdom/render [app-view] (.getElementById js/document "app")))

(defn ^:export main []
  (rf/dispatch-sync [::initialize])
  (render!))
