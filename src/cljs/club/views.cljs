(ns club.views
  (:require [re-frame.core :as rf]
            [re-frame.db :refer [app-db]]
            [goog.object :refer [getValueByKeys]]
            [webpack.bundle]
            [club.utils :refer [groups-option
                                scholar-comparator
                                FormControlFixed]]
            [club.config :as config]
            [club.db]
            [club.expr :refer [clubexpr rendition reified-expressions]]
            [club.version]
            [clojure.walk :refer [keywordize-keys]]
            [cljs.pprint :refer [pprint]]))

; Placeholder for future translation mechanism
(defn t [[txt]] txt)

(defn bs
  ([component]
   (getValueByKeys js/window "deps" "react-bootstrap" (str component)))
  ([c subc]
   (getValueByKeys js/window "deps" "react-bootstrap" (str c) (str subc))))

(def Select (getValueByKeys js/window "deps" "react-select"))
(def Creatable (getValueByKeys Select "Creatable"))
(def Slider (getValueByKeys js/window "deps" "rc-slider" "Range"))
(def CBG (getValueByKeys js/window "deps" "react-checkbox-group"))
(def Checkbox (getValueByKeys CBG "Checkbox"))
(def CheckboxGroup (getValueByKeys CBG "CheckboxGroup"))
(def Sortable (getValueByKeys js/window "deps" "react-drag-sortable" "default"))

(defn text-input [{:keys [label placeholder help value-id event-id]}]
  [:> (bs 'FormGroup) {:controlId "formBasicText"
                       :validationState nil}
    [:> (bs 'ControlLabel) label]
    [FormControlFixed {:type "text"
                       :value @(rf/subscribe [value-id])
                       :placeholder placeholder
                       :on-change #(rf/dispatch [event-id
                                                 (-> % .-target .-value)])}]
    [:> (bs 'FormControl 'Feedback)]
    [:> (bs 'HelpBlock) help]])

(defn src-input
  [{:keys [label help]}]
  [:form {:role "form"}
    [:> (bs 'FormGroup) {:controlId "formBasicText"
                         :validationState nil}
      [:> (bs 'ControlLabel) label]
      [FormControlFixed {:type "text"
                         :value @(rf/subscribe [:attempt-code])
                         :placeholder "(Somme 1 2)"
                         :on-change #(rf/dispatch
                                       [:user-code-club-src-change
                                        (-> % .-target .-value)])}]
      [:> (bs 'FormControl 'Feedback)]
      [:> (bs 'HelpBlock) help]]])

(defn nav-bar
  []
  (let [page @(rf/subscribe [:current-page])
        active #(if (= %1 %2) "active" "")
        authenticated @(rf/subscribe [:authenticated])
        quality @(rf/subscribe [:profile-quality])]
    [:> (bs 'Navbar)
      [:div.container-fluid
        [:> (bs 'Navbar 'Header)
          [:> (bs 'Navbar 'Brand) (t ["Club des Expressions"])]
          [:> (bs 'Navbar 'Toggle)]]
        [:> (bs 'Navbar 'Collapse)
          [:> (bs 'Nav)
            [:> (bs 'NavItem) {:href "#/"
                               :class (active page :landing)} (t ["Accueil"])]
            [:> (bs 'NavItem) {:href "#/help"
                               :class (active page :help)} (t ["Aide"])]
            (if (= quality "teacher")
              [:> (bs 'NavItem) {:href "#/groups"
                                 :class (active page :groups)} (t ["Groupes"])])
            (if (= quality "teacher")
              [:> (bs 'NavItem) {:href "#/series"
                                 :class (active page :series)} (t ["Séries"])])]
          [:> (bs 'Nav) {:pullRight true}
            (if authenticated
              [:> (bs 'NavItem) {:href "#/profile"
                                 :class (active page :profile)} (t ["Profil"])])
            (if authenticated
              [:> (bs 'NavItem)
                  {:on-click #(rf/dispatch [:logout])} (t ["Déconnexion"])]
              [:> (bs 'NavItem)
                  {:on-click #(rf/dispatch [:login])}  (t ["Connexion"])])
          ]]]]
     ))

(defn footer
  []
  [:div.container.small {:style {:color "#aaa"}}  ; TODO CSS
    [:br]
    [:br]
    [:br]
    [:hr]
    [:> (bs 'Grid)
      [:> (bs 'Row)
        [:> (bs 'Col) {:xs 4 :md 4}
          [:h2 (t ["Contact"])]
          [:ul
            [:li "Twitter : "
              [:a {:href "https://twitter"} "@ClubExpr"]
              " (" (t ["Publication d’une expression intéressante par semaine !"]) ")"]
            [:li "Email : "
              [:a {:href "mailto:profgraorg.org@gmail.com"} "profgra@gmail.com"]]
            [:li "Github : "
              [:a {:href "https://github.com/ClubExpressions/clubexpr-web/"}
                  "ClubExpressions/clubexpr"]]]]
        [:> (bs 'Col) {:xs 4 :md 4}
          [:h2 (t ["Statut"])]
          [:p "Le Club des Expressions est en constante évolution. N’hésitez pas à signaler des bugs ou nous faire part de suggestions."]
          [:p "Version : " club.version/gitref]
        ]
        [:> (bs 'Col) {:xs 4 :md 4}
          [:h2 (t ["Remerciements"])]
          [:p (t ["Réalisé avec l’aide, aimable autant que redoutable, de :"])]
          [:ul
            [:li "Jean-Philippe Rouquès (aide pédagogique)"]
            [:li "Damien Lecan (aide technique)"]
            [:li "tous les collègues et élèves sympathisants (aide moral et premiers tests)"]
            [:li "tous les logiciels sur lesquels est bâti le Club (épaules de géants)"]]]
       ]]])

(defn page-landing
  []
  [:div
    [:div.jumbotron
      [:h2 (t ["Nouveau venu ?"])]
      (let [label (t ["Tapez du Code Club ci-dessous pour former une expression mathématique."])
            help [:span (t ["Commandes disponibles :"])
                   [:code "Somme"] ", "
                   [:code "Diff"] ", "
                   [:code "Produit"] ", "
                   [:code "Quotient"] ", "
                   [:code "Oppose"] ", "
                   [:code "Inverse"] ", "
                   [:code "Carre"] ", "
                   [:code "Puissance"] " et "
                   [:code "Racine"] "."]]
        [src-input {:label label :help help}])
      [:br]
      [rendition @(rf/subscribe [:attempt-code])]]
    [:> (bs 'Grid)
      [:> (bs 'Row)
        [:h1 (t ["Qu’est-ce que le Club des Expressions ?"])]
        [:> (bs 'Col) {:xs 6 :md 6}
          [:h2 (t ["Pour les enseignants"])]
          [:p (t ["Le Club des Expressions vous permet de faire travailler vos élèves sur le sens et la structure des expressions mathématiques."])]
          [:p (t ["Cliquez sur « Connexion » en haut à droite pour créer votre compte, faites créer un compte à vos élèves, et vous pourrez leur attribuer des séries d’expressions à reconstituer."])]
          [:p (t ["Si vous êtes parent d’élève, vous pourrez aussi faire travailler votre enfant. Pour cela, créez votre compte en cliquant sur « Connexion » en haut à droite, puis déclarez-vous comme professeur sans établissement."])]]
        [:> (bs 'Col) {:xs 6 :md 6}
          [:h2 (t ["Pour les élèves"])]
          [:p (t ["Le Club des Expressions vous permet de travailler sur le sens et la structure des expressions mathématiques."])]
          [:p (t ["Si votre professeur n’utilise pas le Club, vous pourrez quand même obtenir des séries d’expressions à reconstituer. Pour cela, créez votre compte en cliquant sur « Connexion » en haut à droite. Vos parents pourront se créer un compte professeur, sans établissement, pour vous donner du travail."])]
          [:p (t ["Il est préférable bien sûr que votre professeur vous guide, mettez cette personne au courant !"])]]]]
  ])

(defn page-help-guest
  []
  [:div
    [:div.jumbotron
      [:h2 (t ["Nous ne pouvons pas encore vous donner de l’aide."])]]
    [:h3 (t ["Commencez par vous connecter (bouton en haut à droite)."])]
    [:p (t ["Une aide vous sera proposée en fonction de votre profil."])]
    [:p (t ["En cas de problème pour vous connecter, veuillez nous contacter :"])]
    [:ul
      [:li "par email : "
        [:a {:href "mailto:profgraorg.org@gmail.com"} "profgra@gmail.com"]]
      [:li "sur Github : "
        [:a {:href "https://github.com/ClubExpressions/clubexpr-web/"}
            "ClubExpressions/clubexpr"]]
      [:li "via Twitter : "
        [:a {:href "https://twitter"} "@ClubExpr"]]]
   ])

(defn empty-profile
  []
  [:div
    [:h2 (t ["Votre profil semble vide"])]
    [:p (t ["Vous n’avez pas indiqué votre nom. Veuillez remplir votre profil et revenir sur cette page."])]])

(defn page-help-scholar
  []
  [:div
    [:div.jumbotron
      (if (empty? @(rf/subscribe [:profile-lastname]))
        [empty-profile]
        [:div
          [:h2 (t ["Aide pour les élèves"])]
          [:p (t ["Si vous n’êtes pas élève, modifiez votre profil. Vous pourrez y indiquer votre qualité de professeur."])]])]
    [:> (bs 'Grid)
      [:> (bs 'Row)
        [:h1 (t ["Qu’est-ce que le Club des Expressions ?"])]
        [:> (bs 'Col) {:xs 6 :md 6}
          [:h2 (t ["Pour les élèves"])]
          [:p (t ["Le Club des Expressions vous permet de travailler sur le sens et la structure des expressions mathématiques."])]
          [:p (t ["Si votre professeur n’utilise pas le Club, vous pourrez quand même obtenir des séries d’expressions à reconstituer. Il est préférable bien sûr que votre professeur vous guide, mettez cette personne au courant !"])]
          [:p (t ["Vos parents peuvent se créer un compte professeur, sans établissement, pour vous donner du travail."])]
        ]
        [:> (bs 'Col) {:xs 6 :md 6}
          [:h2 (t ["Pour les enseignants"])]
          [:p (t ["Le Club des Expressions permet aux enseignants de faire travailler leurs élèves sur le sens et la structure des expressions mathématiques."])]
        ]
      ]
      [:> (bs 'Row)
        [:h1 (t ["Ce que l’on peut faire au Club"])]
        [:> (bs 'Col) {:xs 4 :md 4}
          [:h2 (t ["Définir son profil"])]
          [:p (t ["Dans la partie « Profil », déclarez votre établissement puis votre professeur (si vous n’en avez pas, choisissez « Pas de professeur »)."])]
          [:p (t ["Grâce à votre nom et prénom, votre professeur pourra vous inclure dans un ou plusieurs groupes de travail."])]
        ]
        [:> (bs 'Col) {:xs 4 :md 4}
          [:h2 (t ["Faire le travail donné"])]
          [:p (t ["Dans la partie « Travail », vous trouverez le travail que votre professeur vous propose de faire."])]
        ]
        [:> (bs 'Col) {:xs 4 :md 4}
          [:h2 (t ["S’entraîner"])]
          [:p (t ["Le Club des Expressions vous proposera parfois, dans la partie « Travail », des séries à faire pour vous entraîner."])]
        ]
      ]
    ]])

(defn page-help-teacher
  []
  [:div
    [:div.jumbotron
      (if (empty? @(rf/subscribe [:profile-lastname]))
        [empty-profile]
        [:div
          [:h2 (t ["Aide pour les professeurs"])]
          [:p (t ["Si vous n’êtes pas professeur, modifiez votre profil sinon votre professeur ne pourra pas vous retrouver."])]])]
    [:> (bs 'Grid)
      [:> (bs 'Row)
        [:h1 (t ["Qu’est-ce que le Club des Expressions ?"])]
        [:> (bs 'Col) {:xs 6 :md 6}
          [:h2 (t ["Pour les enseignants"])]
          [:p (t ["Le Club des Expressions vous permet de faire travailler vos élèves sur le sens et la structure des expressions mathématiques."])]]
        [:> (bs 'Col) {:xs 6 :md 6}
          [:h2 (t ["Pour les élèves"])]
          [:p (t ["Le Club des Expressions permet aux élèves de travailler sur le sens et la structure des expressions mathématiques."])]
          [:p (t ["Si leur professeur n’utilise pas le Club, les élèves peuvent quand même obtenir des séries d’expressions à reconstituer grâce à des professeurs-robots."])]
          [:p (t ["Il est préférable bien sûr que leur professeur les guide !"])]]]
      [:> (bs 'Row)
        [:h1 (t ["Ce que l’on peut faire au Club"])]
        [:> (bs 'Col) {:xs 4 :md 4}
          [:h2 (t ["Définir son profil"])]
          [:p (t ["Dans la partie « Profil », déclarez votre établissement puis votre professeur."])]
          [:p (t ["Grâce à votre nom, vos élèves pourront vous choisir comme « professeur référent » et apparaîtront dans votre partie « Groupes »."])]
        ]
        [:> (bs 'Col) {:xs 4 :md 4}
          [:h2 (t ["Regrouper ses élèves"])]
          [:p (t ["Dans la partie « Groupes », vous définirez des listes d’élèves. Ces listes peuvent correspondre :"])]
          [:ul
            [:li (t ["à des classes entières ;"])]
            [:li (t ["à des demis-groupes d’une classe ;"])]
            [:li (t ["à des élèves ayant des besoins spécifiques (remédiation ou approfondissement) au sein de l’Accompagnement Personnalisé ou non ;"])]
            [:li (t ["…"])]]
        ]
        [:> (bs 'Col) {:xs 4 :md 4}
          [:h2 (t ["Assigner des séries à ses groupes"])]
          [:p (t ["Une fois que vous aurez créé une série dans la partie « Séries », vous pourrez l’attribuer à un groupe. Cette attribution se fait dans la partie « Groupes »."])]
        ]
      ]
      [:> (bs 'Row)
        [:> (bs 'Col) {:xs 4 :md 4}
          [:h2 (t ["Ce que voit un élève"])]
          [:p (t ["Il est possible de se connecter au Club avec plusieurs comptes. Un de ces comptes sera votre compte principal, avec un profil de professeur. Les autres comptes pourront avoir un profil d’élève."])]
          [:p (t ["Attention, vous ne pouvez pas gérer vos vrais élèves depuis différents comptes, même s’ils ont un profil de professeur."])]
        ]
      ]
    ]
  ])

(defn page-help
  []
  (if (not @(rf/subscribe [:authenticated]))
    [page-help-guest]
    (case @(rf/subscribe [:profile-quality])
      "scholar" [page-help-scholar]
      "teacher" [page-help-teacher]
      [page-help-guest])))

(defn school->menu-item
  [school]
  ^{:key (:id school)} [:> (bs 'MenuItem)
                           {:eventKey (:id school)}
                           (:name school)])

(defn teacher->menu-item
  [{:keys [id lastname]}]
  ^{:key id} [:> (bs 'MenuItem) {:eventKey id} lastname]
  )

(defn teachers-dropdown
  []
  (let [teachers-list @(rf/subscribe [:profile-teachers-list])]
    [:> (bs 'DropdownButton)
        {:title @(rf/subscribe [:profile-teacher-pretty])
         :on-select #(rf/dispatch [:profile-teacher %])}
       ^{:key "no-teacher"} [:> (bs 'MenuItem) {:eventKey "no-teacher"}
                                               (t ["Pas de professeur"])]
       (when (not (empty? teachers-list))
         [:> (bs 'MenuItem) {:divider true}])
       (when (not (empty? teachers-list))
         (map teacher->menu-item teachers-list))]))

(defn page-profile
  []
  (let [profile-quality @(rf/subscribe [:profile-quality])
        help-text-find-you
          (case profile-quality
            "scholar" (t ["pour que votre professeur puisse vous retrouver"])
            "teacher" (t ["pour que les élèves puissent vous retrouver (indiquer aussi ici le prénom pour les homonymes)"])
            (t ["pour que l’on puisse vous retrouver"]))
        lastname  [text-input {:label (t ["Nom"])
                               :placeholder (t ["Klougliblouk"])
                               :help (str (t ["Votre nom de famille"])
                                          " "
                                          help-text-find-you)
                               :value-id :profile-lastname
                               :event-id :profile-lastname}]
        firstname [text-input {:label (t ["Prénom"])
                               :placeholder (t ["Georgette"])
                               :help (str (t ["Votre prénom"])
                                          " "
                                          help-text-find-you)
                               :value-id :profile-firstname
                               :event-id :profile-firstname}]
        school [:> (bs 'DropdownButton)
                   {:title @(rf/subscribe [:profile-school-pretty])
                    :on-select #(rf/dispatch [:profile-school %])}
                  [:> (bs 'MenuItem) {:eventKey "fake-id-no-school"}
                                     (t ["Aucun établissement"])]
                  [:> (bs 'MenuItem) {:divider true}]
                  (map school->menu-item (club.db/get-schools!))
               ]
       ]
    [:div
      [:div.jumbotron
        [:h2 (t ["Votre profil"])]]
      [:form {:role "form"}
        [:div {:style {:margin-bottom "1em"}}  ; TODO CSS
          [:> (bs 'ButtonToolbar)
            [:> (bs 'ToggleButtonGroup)
                {:type "radio"
                 :name "quality"
                 :value @(rf/subscribe [:profile-quality])
                 :defaultValue "scholar"
                 :on-change #(rf/dispatch [:profile-quality %])}
              [:> (bs 'ToggleButton) {:value "scholar"} (t ["Élève"])]
              [:> (bs 'ToggleButton) {:value "teacher"} (t ["Professeur"])]]]]
        [:div {:style {:margin-bottom "1em"}}  ; TODO CSS
          school]
        (if (= "scholar" @(rf/subscribe [:profile-quality]))
          [:div {:style {:margin-bottom "1em"}}  ; TODO CSS
            [:> (bs 'ControlLabel) (t ["Professeur : "])]
            [teachers-dropdown @(rf/subscribe [:profile-school])]]
          "")
        lastname
        (if (= "scholar" @(rf/subscribe [:profile-quality]))
          firstname
          "")
        [:> (bs 'Button)
          {:style {:margin "1em"}  ; TODO CSS
           :on-click #(rf/dispatch [:profile-cancel])
           :bsStyle "danger"} "Annuler les modifications"]
        [:> (bs 'Button)
          {:style {:margin "1em"}  ; TODO CSS
           :on-click #(rf/dispatch [:profile-save])
           :bsStyle "success"} "Enregistrer les modifications"]
      ]
    ]))

(defn groups-select
  [scholar-id]
  (let [value @(rf/subscribe [:groups-value scholar-id])]
    [:span {:style {:margin-left "1em"}}  ; TODO CSS
      [:> Creatable
         {:multi true
          :options (map groups-option @(rf/subscribe [:groups]))
          :on-change #(rf/dispatch [:groups-change [scholar-id %]])
          :noResultsText "Un nom pour votre 1er groupe (ex: 2nde1)"
          :promptTextCreator #(str (t ["Créer le groupe"]) " « " % " »")
          :placeholder (t ["Assigner à un groupe…"])
          :value value}]]))

(defn scholar-li-group-input
  [scholar]
  ^{:key (:id scholar)} [:li (:lastname scholar) " " (:firstname scholar)
                         (groups-select (:id scholar))])

(defn group-link
  [group]
  ^{:key group}
  [:span
    {:style {:margin "1em"}}  ; TODO CSS
    group])

(defn scholar-li
  [scholar]
  ^{:key (str (:lastname scholar) (:firstname scholar))}
  [:li (:lastname scholar) " " (:firstname scholar)])

(defn format-group
  [[group scholars]]
  ^{:key group} [:div
                  [:h3 group]
                  [:ul.nav
                    (map scholar-li (sort scholar-comparator scholars))]])

(defn groups-list-of-lists
  [groups-map groups]
  (let [; {"id1" {:k v} "id2" {:k v}} -> ({:k v} {:k v})
        lifted-groups-map (map second groups-map)
        scholars-in-groups
         (map (fn [group]
                [group (filter #(some #{group} (:groups %))
                                         lifted-groups-map)]) groups)]
    (map format-group scholars-in-groups)))

(defn page-groups
  []
  (let [groups-data @(rf/subscribe [:groups-page])
        groups @(rf/subscribe [:groups])
        lifted-groups (map #(merge {:id (first %)} (second %)) groups-data)]
    [:div
      [:div.jumbotron
        [:h2 (t ["Groupes"])]
        [:p (t ["Assignez chacun de vos élèves à un ou plusieurs groupes"])]]
      [:> (bs 'Grid)
        (if (empty? groups-data)
          [:div
            [:h2 (t ["Aucun élève ne vous a encore déclaré comme professeur."])]
            [:p (t ["En attendant que ce soit le cas, vous pouvez tester cette fonctionnalité en vous connectant avec un autre compte et en vous faisant passer pour un élève vous ayant comme professeur."])]]
          [:div
            [:p (t ["Un groupe peut correspondre : à des classes entières, à des demis-groupes d’une classe, à des élèves ayant des besoins spécifiques (remédiation ou approfondissement) au sein de l’Accompagnement Personnalisé ou non…"])]
            [:> (bs 'Row)
              [:> (bs 'Col) {:xs 6 :md 6}
                [:h2 (t ["Vos élèves"])]
                [:ul.nav {:max-height "30em" :overflow-y "scroll"}  ; TODO CSS
                  (doall (map scholar-li-group-input
                              (sort scholar-comparator lifted-groups)))]]
              [:> (bs 'Col) {:xs 6 :md 6}
                [:h2 (t ["Vos groupes"])]
                [:div (map group-link groups)]
                [:div {:max-height "30em" :overflow-y "scroll"}  ; TODO CSS
                  (groups-list-of-lists groups-data groups)]]
            ]])]
      [:> (bs 'Button)
        {:style {:margin "1em"}  ; TODO CSS
         :on-click #(rf/dispatch [:groups-cancel])
         :bsStyle "danger"}
        "Annuler les modifications"]
      [:> (bs 'Button)
        {:style {:margin "1em"}  ; TODO CSS
         :on-click #(rf/dispatch [:groups-save])
         :bsStyle "success"} "Enregistrer les modifications"]]))

(defn show-expr-as-li-click
  [expr]
  (let [nom (:nom expr)
        renderExprAsLisp (.-renderExprAsLisp clubexpr)
        lisp (renderExprAsLisp (-> expr :expr clj->js))]
    ^{:key nom}
    [:li
      {:on-double-click #(rf/dispatch [:series-exprs-add lisp])}
      (rendition lisp)]))

(defn ops-cb-label
  [name]
  ^{:key name}
  [:label
    {:style {:margin-right "1em" :font-weight "normal"}}  ; TODO CSS
    [:> Checkbox {:value name
                  :style {:margin-right "0.3em"}}]
    name])

(def slider-defaults
  {:style {:margin-bottom "1.5em"}  ; TODO CSS
   :min 1 :max 7 :range true
   :marks {"1" "1", "2" "2", "3" "3", "4" "4", "5" "5", "6" "6", "7" "7"}
   })

(def filtering-title-style
  {:style {:font-weight "bold"
           :margin-top "1.2em"
           :margin-bottom "0.2em"}})  ; TODO CSS

(defn series-filter
  []
  [:div
    [:h2 (t ["Banque d’expressions"])]
    [:> Select
      {:options [{:value "All"       :label "Toutes les natures"}
                 {:value "Somme"     :label "Sommes"}
                 {:value "Diff"      :label "Différences"}
                 {:value "Opposé"    :label "Opposés"}
                 {:value "Produit"   :label "Produits"}
                 {:value "Quotient"  :label "Quotients"}
                 {:value "Inverse"   :label "Inverses"}
                 {:value "Carré"     :label "Carrés"}
                 {:value "Racine"    :label "Racines"}
                 {:value "Puissance" :label "Puissances"}]
       :clearable false
       :noResultsText "Pas de nature correspondant à cette recherche"
       :value @(rf/subscribe [:series-filtering-nature])
       :onChange #(rf/dispatch [:series-filtering-nature %])
       }]
    [:div filtering-title-style "Profondeur"]
    [:> Slider
      (merge slider-defaults
             {:value @(rf/subscribe [:series-filtering-depth])
              :onChange #(rf/dispatch [:series-filtering-depth %])})]
    [:div filtering-title-style "Nb d’opérations"]
    [:> Slider
      (merge slider-defaults
             {:value @(rf/subscribe [:series-filtering-nb-ops])
              :onChange #(rf/dispatch [:series-filtering-nb-ops %])})]
    [:div filtering-title-style "Opérations à ne pas faire apparaître"]
    [:> CheckboxGroup
      {:value @(rf/subscribe [:series-filtering-prevented-ops])
       :onChange #(rf/dispatch [:series-filtering-prevented-ops %])}
      (map ops-cb-label (.-operations clubexpr))]
    [:div filtering-title-style "Expressions correspondantes"]
    (let [filtered-exprs @(rf/subscribe [:series-filtered-exprs])
          exprs-as-li (map show-expr-as-li-click filtered-exprs)]
      (if (empty? exprs-as-li)
        [:p (t ["Aucune expression ne correspond à ce filtrage"])]
        [:ul.nav exprs-as-li]))
  ])

(defn no-series
  []
  [:div
    [:h2 (t ["Vous n’avez pas encore créé de série."])]
    [:p (t ["Pour créer une série, appuyer sur le bouton « Nouvelle série »."])]])

(defn series-li
  [series-obj]
  (let [current-series-id @(rf/subscribe [:current-series-id])
        id (:id series-obj)
        series (:series series-obj)
        title? (-> series :title)
        title (if (empty? title?) (t ["Sans titre"]) title?)
        attrs-base {:on-click #(rf/dispatch [:current-series-id id])}
        attrs (if (= current-series-id id)
                (merge attrs-base {:style {:background "#ddd"}})
                attrs-base)]
    ^{:key id}
    [:> (bs 'NavItem) attrs title]))

(defn series-list
  []
  (let [series-data @(rf/subscribe [:series-page])]
    [:div
      [:h2 (t ["Vos séries"])]
      [:ul.nav {:max-height "30em" :overflow-y "scroll"}  ; TODO CSS
        (doall (map series-li series-data))]]))

(defn show-expr-as-li
  [lisp]
  ^{:key lisp} [:li (rendition lisp)])

(defn show-series
  []
  (let [series-data @(rf/subscribe [:series-page])
        series-id  @(rf/subscribe [:current-series-id])
        title @(rf/subscribe [:series-title])
        desc @(rf/subscribe [:series-desc])]
    (if (empty? series-id)
      (if (not (empty? series-data))
        [:div
          [:h2 (t ["Aperçu de la série"])]
          [:p (t ["Veuillez sélectionner une série sur la gauche."])]])
      [:div
        [:div.pull-right
          [:> (bs 'Button)
            {:style {:margin-right "1em"}
             :on-click #(rf/dispatch [:series-edit])
             :bsStyle "warning"} (t ["Modifier cette série"])]
          [:> (bs 'Button)
            {:on-click #(rf/dispatch [:series-delete])
             :bsStyle "danger"} (t ["Supprimer cette série"])]]
        [:h3 (if (empty? title) (t ["Sans titre"]) title)]
        (if (empty? desc)
          [:p (t ["Pas de description"])]
          [:p (t ["Description : "]) desc])
        [:ul.nav
          (map show-expr-as-li @(rf/subscribe [:series-exprs]))]])
     ))

(defn edit-series
  []
  (let [exprs @(rf/subscribe [:series-exprs-with-content-key])]
    [:div
      [:h2 (t ["Série en cours de modification"])]
      [text-input {:label (t ["Titre"])
                   :placeholder (t ["Découverte du Club"])
                   :help (t ["Titre de la série, vu par les élèves"])
                   :value-id :series-title
                   :event-id :series-title}]
      [text-input {:label (t ["Description"])
                   :placeholder (t ["Expressions triviales pour apprendre à utiliser le Club"])
                   :help (t ["Description de la série, vue seulement par les autres enseignants, mais pas les élèves"])
                   :value-id :series-desc
                   :event-id :series-desc}]
      [:p [:strong (t ["Expressions"])]]
      [:> (bs 'Button)
        {:on-click #(rf/dispatch [:series-cancel])
         :bsStyle "danger"} "Annuler"]
      [:> (bs 'Button)
        {:style {:margin "1em"}  ; TODO CSS
         :on-click #(rf/dispatch [:series-save])
         :bsStyle "success"} "Enregistrer"]
      [:> (bs 'Button)
        {:style {:margin "1em"}  ; TODO CSS
         :class "pull-right"
         :on-click #(rf/dispatch [:series-delete])
         :bsStyle "danger"} "Supprimer cette série"]
      (if (empty? exprs)
        [:p
          [:strong (t ["La série est vide."])]
          " "
          (t ["En double-cliquant sur une expression sur la gauche, vous pouvez l’ajouter à votre série. Pour la supprimer de la série (liste de droite), double-cliquer à nouveau mais dans la liste de droite."])]
        [:> Sortable
          {:items exprs
           :moveTransitionDuration 0.3
           :dropBackTransitionDuration 0.3
           :placeholder "< ici >"
           :onSort #(rf/dispatch [:series-exprs-sort %])}])
     ]))

(defn page-series
  []
  (let [series-data @(rf/subscribe [:series-page])
        editing-series @(rf/subscribe [:editing-series])
        current-series @(rf/subscribe [:current-series])]
    [:div
      [:div.jumbotron
        [:h2 (t ["Séries"])]
        [:p (t ["Construisez des séries d’expressions à faire reconstituer aux élèves"])]]
      [:> (bs 'Grid)
        [:> (bs 'Row)
          [:> (bs 'Col) {:xs 6 :md 6}
            (if editing-series
              (series-filter)
              [:div
                (if (empty? series-data)
                  (no-series)
                  (series-list))
                [:> (bs 'Button)
                  {:style {:margin "1em"}  ; TODO CSS
                   :on-click #(rf/dispatch [:new-series])
                   :bsStyle "success"} "Nouvelle série"]])]
          [:> (bs 'Col) {:xs 6 :md 6}
            (if editing-series
                (edit-series)
                (show-series))]
        ]]]))

(defn page-teacher-only
  []
  [:div.jumbotron
    [:h2 (t ["Désolé, il faut être professeur pour accéder à cette page."])]])

(defn page-forbidden
  []
  [:div.jumbotron
    [:h2 (t ["Désolé, il faut se connecter pour accéder à cette page."])]])

(defn main-panel []
  (fn []
    (let [authenticated  @(rf/subscribe [:authenticated])
          quality        @(rf/subscribe [:profile-quality])
          current-page   @(rf/subscribe [:current-page])]
      [:div
        [:div.container
          [nav-bar]
          (if (or authenticated
                  (some #{current-page} [:landing :help]))
            (if (= "teacher" quality)
              (case current-page
                :landing [page-landing]
                :help [page-help]
                :profile [page-profile]
                :groups [page-groups]
                :series [page-series])
              (case current-page
                :landing [page-landing]
                :help [page-help]
                :profile [page-profile]
                [page-teacher-only]))
            [page-forbidden]
          )
          [footer]
        ]
        (when (and true config/debug?)
          [:pre {:style {:position "absolute" :bottom "0px" :width "100%"}}
            (with-out-str (pprint @app-db))])
      ]
    )))
