(ns club.views
  (:require [re-frame.core :as rf]
            [re-frame.db :refer [app-db]]
            [goog.object :refer [getValueByKeys]]
            [webpack.bundle]
            [reagent.core :as r]
            [cljs-time.core :refer [today]]
            [club.utils :refer [t
                                groups-option
                                scholar-comparator
                                series-comparator
                                scholar-progress-comparator
                                FormControlFixed
                                pretty-date
                                today-str
                                moment->str
                                str->cljs-time
                                moment->cljs-time
                                before?=
                                after?=]]
            [club.config :as config]
            [club.db]
            [club.expr :refer [clubexpr
                               natureFromLisp
                               available-ops
                               renderLispAsLaTeX
                               infix-rendition
                               tree-rendition]]
            [club.version]
            [clojure.walk :refer [keywordize-keys]]
            [cljs.pprint :refer [pprint]]))

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
(def DateTime (getValueByKeys js/window "deps" "react-datetime"))
(def CodeMirror (getValueByKeys js/window "deps"
                                          "react-codemirror2"
                                          "Controlled"))

(defn text-input [{:keys [component-class label placeholder help
                          value-id event-id]
                   :or {component-class "input"}}]
  [:> (bs 'FormGroup) {:controlId "formBasicText"
                       :validationState nil}
    [:> (bs 'ControlLabel) label]
    [FormControlFixed {:type "text"
                       :componentClass component-class
                       :value @(rf/subscribe [value-id])
                       :placeholder placeholder
                       :on-change #(rf/dispatch [event-id
                                                 (-> % .-target .-value)])}]
    [:> (bs 'FormControl 'Feedback)]
    [:> (bs 'HelpBlock) help]])

(defn list-pretty
  [l]
  (-> (map #(identity [:span [:code %1] %2]) (butlast l) (repeat ", "))
      (concat (t ["et"]))
      (concat " ")
      (concat (list [:code (last l)]))))

(defn ops-pretty
  [ops]
  [:span
    (t ["Commandes disponibles : "])
    (list-pretty ops)
    "."
    [:br]
    (t ["Touches disponibles : "])
    (list-pretty ["+" "-" "*" "/" "²" "^"])
    ", "
    (t ["ou avec"]) [:code "Ctrl"] " + " (t ["initiale"]) "."])

(defn src-input
  [{:keys [label subs-path evt-handler help]}]
  [:form {:role "form"}
    [:> (bs 'FormGroup) {:controlId "formBasicText"
                         :validationState nil}
      [:> (bs 'ControlLabel) label]
      (let [extraKeys {"'+'" #(.replaceSelection % "(Somme ")
                       "'-'" #(.replaceSelection % "(Diff ")
                       "'*'" #(.replaceSelection % "(Produit ")
                       "'/'" #(.replaceSelection % "(Quotient ")
                       "'²'" #(.replaceSelection % "(Carré ")
                       "'^'" #(.replaceSelection % "(Puissance ")
                       "Ctrl-S" #(.replaceSelection % "(Somme ")
                       "Ctrl-D" #(.replaceSelection % "(Diff ")
                       "Ctrl-P" #(.replaceSelection % "(Produit ")
                       "Ctrl-Q" #(.replaceSelection % "(Quotient ")
                       "Ctrl-O" #(.replaceSelection % "(Opposé ")
                       "Ctrl-I" #(.replaceSelection % "(Inverse ")
                       "Ctrl-C" #(.replaceSelection % "(Carré ")
                       "Ctrl-U" #(.replaceSelection % "(Puissance ")
                       "Ctrl-R" #(.replaceSelection % "(Racine ")}]
        [:> CodeMirror {:value @(rf/subscribe [subs-path])
                        :options {:mode "clubexpr"
                                  :extraKeys extraKeys}
                        :onBeforeChange #(rf/dispatch [evt-handler %3])}])
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
            (if (and authenticated (= quality "teacher"))
              [:> (bs 'NavItem) {:href "#/series"
                                 :class (active page :series)} (t ["Séries"])])
            (if (and authenticated (= quality "teacher"))
              [:> (bs 'NavItem) {:href "#/groups"
                                 :class (active page :groups)} (t ["Groupes"])])
            (if (and authenticated (= quality "teacher"))
              [:> (bs 'NavItem) {:href "#/work"
                                 :class (active page :work)} (t ["Travaux"])])
            (if (and authenticated (= quality "scholar"))
              [:> (bs 'NavItem) {:href "#/work"
                                 :class (active page :work)} (t ["Travail"])])]
          (let [highlight-style {:style {:background-color "#0e3"
                                         :border-radius "6px"}}]
            [:> (bs 'Nav) {:pullRight true}
              (if authenticated
                [:> (bs 'NavItem)
                  (merge {:href "#/profile" :class (active page :profile)}
                         (if (empty? @(rf/subscribe [:profile-lastname]))
                              highlight-style))
                  (t ["Profil"])])
              (if authenticated
                [:> (bs 'NavItem)
                    {:on-click #(rf/dispatch [:logout])}
                     (t ["Déconnexion"])]
                (if (not= :login-signup page)
                  [:> (bs 'NavItem)
                    (merge {:href "#/login-signup"} highlight-style)
                    (t ["Connexion"])]))])]]]
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
          [:h2 (t ["Statut"])]
          [:p
            (t ["Le Club des Expressions est en constante évolution."])
            " "
            (t ["N’hésitez pas à signaler des bugs ou nous faire part de suggestions"])
            " "
            [:a {:href "https://github.com/ClubExpressions/clubexpr-web/issues/new"}
              (t ["sur Github"])]
            " "
            (t ["ou"])
            " "
            [:a {:href "mailto:profgraorg.org@gmail.com"} "par email"]
            "."]
          (let [base "https://github.com/ClubExpressions/clubexpr-web/commit/"
                commit club.version/gitref]
            [:p "Version : " [:a {:href (str base commit)} commit]])]
        [:> (bs 'Col) {:xs 4 :md 4}
          [:h2 (t ["Contact"])]
          [:ul
            [:li "Twitter : "
              [:a {:href "http://twitter.com/clubexpr"} "@ClubExpr"]
              " (" (t ["Publication d’une expression intéressante par semaine !"]) ")"]
            [:li "Email : "
              [:a {:href "mailto:profgraorg.org@gmail.com"} "profgra.org@gmail.com"]]
            [:li "Github : "
              [:a {:href "https://github.com/ClubExpressions/clubexpr-web/"}
                  "ClubExpressions/clubexpr"]]]]
        [:> (bs 'Col) {:xs 4 :md 4}
          [:h2 (t ["Remerciements"])]
          [:p (t ["Réalisé avec l’aide, aimable autant que redoutable, de :"])]
          [:ul
            [:li "Jean-Philippe Rouquès (aide pédagogique)"]
            [:li "Damien Lecan (aide technique)"]
            [:li "tous les collègues et élèves sympathisants"
                 [:br]
                 "(aide morale et premiers tests)"]
            [:li [:a {:href "https://github.com/ClubExpressions/clubexpr-web#technical-stack"} "tous les logiciels sur lesquels est bâti le Club"]
                 [:br]
                 "(épaules de géants)"]]]
       ]]])

(defn landing-game-link
  [idx current]
  [:a {:style {:padding "3px"  ; TODO CSS
               :border-radius "4px"
               :background-color (if (= idx current) "#beb" "#ddd")}
       :on-click #(rf/dispatch [:game-idx idx])}
      (str (t ["n°"]) (+ idx 1))])

(defn page-landing
  []
  (let [label (t ["Modifiez le Code Club ci-dessous pour reconstituer l’expression proposée :"])
        game-idx @(rf/subscribe [:game-idx])
        game-src @(rf/subscribe [:game-src])
        attempt  @(rf/subscribe [:attempt-code])
        help (ops-pretty available-ops)
        task-style {:style {:font-size "120%"}}  ; TODO CSS
        expr-style {:style {:font-size "120%"}}]  ; TODO CSS
    [:div
      [:div.jumbotron
        [:h2 (t ["Première visite ?"])]
        [:> (bs 'Grid)
          ; the only row in the jumbotron, see below the nested grid
          [:> (bs 'Row)
            [:> (bs 'Col) {:xs 7 :md 7}
              ; left part, with everything but the tree:
              [:> (bs 'Grid)
                ; target expr:
                [:> (bs 'Row)
                  [:> (bs 'Col) {:xs 5 :md 5}
                    [:p task-style (t ["Essayez de reconstituer :"])]]
                  [:> (bs 'Col) {:xs 6 :md 6}
                    [:div expr-style [infix-rendition game-src]]]]
                ; Code Club input
                [:> (bs 'Row)
                  [:> (bs 'Col) {:xs 11 :md 11}
                    [src-input {:label label
                                :subs-path :attempt-code
                                :evt-handler :user-code-club-src-change
                                :help help}]]]
                ; attempted expr:
                [:> (bs 'Row)
                  [:> (bs 'Col) {:xs 5 :md 5}
                    [:p task-style (t ["Votre tentative :"])]]
                  [:> (bs 'Col) {:xs 6 :md 6}
                    [:div expr-style [infix-rendition attempt]]
                    (let [target-nature (natureFromLisp game-src)
                          attempt-nature (natureFromLisp attempt)]
                      (if (and (not (empty? attempt-nature))
                               (not= target-nature attempt-nature))
                        [:div {:style {:color "#f00"
                                       :font-size "120%"
                                       :text-align "center"}}  ; TODO CSS
                          (t ["La nature ne correspond pas."])]))
                    (try (if (= (renderLispAsLaTeX game-src)
                                (renderLispAsLaTeX attempt))
                           [:div {:style {:color "#0f0"
                                          :font-size "200%"
                                          :text-align "center"}}  ; TODO CSS
                             (t ["Bravo !"])])
                         (catch js/Object e))]]
                [:> (bs 'Row)
                  [:> (bs 'Col) {:xs 12 :md 12
                                 :style {:padding-top "2em"}}
                    [:p task-style (t ["Expression à reconstituer"]) " : "
                      (landing-game-link 0 game-idx)
                      " "
                      (landing-game-link 1 game-idx)
                      " "
                      (landing-game-link 2 game-idx)]]]
              ]]
            [:> (bs 'Col) {:xs 5 :md 5}
              [:p task-style
                (t ["Pour information, l’arbre de calcul de votre tentative :"])]
              [tree-rendition attempt]]
          ]]]
      [:> (bs 'Grid)
        [:> (bs 'Row)
          [:h1 (t ["Qu’est-ce que le Club des Expressions ?"])]
          [:> (bs 'Col) {:xs 6 :md 6}
            [:h2 (t ["Pour les enseignants"])]
            [:p (t ["Le Club des Expressions permet de faire travailler vos élèves sur le sens et la structure des expressions mathématiques."])]
            [:p (t ["Cliquez sur « Connexion » en haut à droite pour créer votre compte, faites créer un compte à vos élèves, et vous pourrez leur attribuer des séries d’expressions à reconstituer."])]
            [:p (t ["Si vous êtes parent d’élève, vous pourrez aussi faire travailler votre enfant. Pour cela, créez votre compte en cliquant sur « Connexion » en haut à droite, puis déclarez-vous comme professeur sans établissement."])]]
          [:> (bs 'Col) {:xs 6 :md 6}
            [:h2 (t ["Pour les élèves"])]
            [:p (t ["Le Club des Expressions vous permet de travailler sur le sens et la structure des expressions mathématiques."])]
            [:p (t ["Si votre professeur n’utilise pas le Club, vous pourrez quand même obtenir des séries d’expressions à reconstituer. Pour cela, créez votre compte en cliquant sur « Connexion » en haut à droite. Vos parents pourront se créer un compte professeur, sans établissement, pour vous donner du travail."])]
            [:p (t ["Il est préférable bien sûr que votre professeur vous guide, mettez cette personne au courant !"])]]]]]))

(defn page-login-signup
  []
  [:div
    [:div.jumbotron
      [:h2 (t ["Explications concernant la connexion"])]]
    [:p {:style {:text-align "right" :font-size "150%"}}  ; TODO CSS
      [:a {:on-click #(rf/dispatch [:login])}
          (t ["Merci, je connais déjà, je veux juste me connecter."])]]
    [:h1 (t ["Log In ou Sign Up ?"])]
    [:p (t ["Lors de votre toute première connexion, cliquer sur l’onglet Sign Up (plutôt à droite). Pour toutes les autres connexions, rester sur l’onglet Log In."])]
    [:p (t ["Vous trouverez d’autres informations importantes plus bas dans cette page. Lisez-les attentivement."])]
    [:a {:on-click #(rf/dispatch [:login])} (t ["Je veux passer à l’écran de connexion."])]
    [:div {:style {:text-align "center" :margin "2em"}}  ; TODO CSS
      [:img {:src "img/login-signup.png" :alt "screenshots"}]]
    [:h1 (t ["Précautions"])]
    [:p (t ["Si vous n’utilisez pas la méthode email/mot de passe, pensez à vous déconnecter après votre travail si vous n’utilisez pas votre ordinateur."])]
    [:h1 (t ["Connexion"])]
    [:a {:on-click #(rf/dispatch [:login])} (t ["J’ai tout compris, je veux passer à l’écran de connexion."])]
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
        [:a {:href "https://twitter.com/clubexpr"} "@ClubExpr"]]]
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
      [:hr]
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
        [:hr]
        [:h1 (t ["Ce que l’on peut faire au Club"])]
        [:> (bs 'Col) {:xs 4 :md 4}
          [:h2 (t ["Définir son profil"])]
          [:p (t ["Dans la partie « Profil », déclarez votre établissement puis votre professeur."])]
          [:p (t ["Grâce à votre nom, vos élèves pourront vous choisir comme « professeur référent » et apparaîtront dans votre partie « Groupes »."])]
          [:p (t ["En attendant que de vrais élèves vous déclarent comme étant leur professeur, deux élèves fantômes vont vous permettre d’essayer cette interface. Ces élèves n’ont pas de vrai compte et il n’est pas possible de se connecter au site sous leur identité. Vous pouvez cependant vous connecter au site avec un autre compte afin de créer un élève factice qui vous aurait comme professeur."])]
        ]
        [:> (bs 'Col) {:xs 4 :md 4}
          [:h2 (t ["Regrouper ses élèves"])]
          [:p (t ["Dans la partie « Groupes », vous définirez des listes d’élèves. Ces listes peuvent correspondre :"])]
          [:ul
            [:li (t ["à des classes entières ;"])]
            [:li (t ["à des demi-groupes d’une classe ;"])]
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
          [:p (t ["Il est possible de se connecter au Club avec plusieurs comptes. Un de ces comptes sera votre compte principal, avec un profil de professeur. Les autres comptes pourront avoir un profil d’élève et vous déclarer comme professeur."])]
          [:p (t ["Attention, vous ne pouvez pas gérer vos vrais élèves depuis différents comptes, même si ces comptes ont un profil de professeur."])]
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
  ^{:key id}
  [:> (bs 'MenuItem) {:eventKey id} [:span.text-capitalize lastname]])

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
            "teacher" (t ["pour que les élèves puissent vous retrouver (indiquer aussi ici le prénom pour les homonymes dans un même établissement)"])
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
        school [:> (bs 'DropdownButton)  ; TODO use a Select (react-select)
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
           :bsStyle "warning"}
          (t ["Annuler les modifications"])]
        [:> (bs 'Button)
          {:style {:margin "1em"}  ; TODO CSS
           :on-click #(rf/dispatch [:profile-save])
           :bsStyle "success"}
          (t ["Enregistrer les modifications"])]
      ]
    ]))

(defn groups-select
  [scholar-id]
  (let [value @(rf/subscribe [:groups-value scholar-id])]
    [:span {:style {:margin-left "1em"}}  ; TODO CSS
      [:> Creatable
         {:spellcheck false
          :multi true
          :options (map groups-option @(rf/subscribe [:groups]))
          :on-change #(rf/dispatch [:groups-change [scholar-id %]])
          :noResultsText "Un nom pour votre 1er groupe (ex: 2nde1)"
          :promptTextCreator #(str (t ["Créer le groupe"]) " « " % " »")
          :placeholder (t ["Assigner à un groupe existant ou taper un nouveau nom de groupe…"])
          :style (if (= 0 (count value))
                   {:background-color "#fee"}  ; TODO CSS
                   {})
          :value value}]]))

(defn scholar-li-group-input
  [scholar]
  ^{:key (:id scholar)}
  [:li
    [:span.text-capitalize (:lastname scholar)]
    " "
    [:span.text-capitalize (:firstname scholar)]
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
  [:li
    [:span.text-capitalize (:lastname scholar)]
    " "
    [:span.text-capitalize (:firstname scholar)]])

(defn format-group
  [[group scholars]]
  (let [card (count scholars)]
    ^{:key group}
    [:div
      [:h3
        group
        [:span.small " (" (count scholars) " "
          (if (> card 1) (t ["élèves"]) (t ["élève"]))")"]]
      [:ul.nav (map scholar-li (sort scholar-comparator scholars))]]))

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
            [:p (t ["Un groupe peut correspondre : à des classes entières, à des demi-groupes d’une classe, à des élèves ayant des besoins spécifiques (remédiation ou approfondissement) au sein de l’Accompagnement Personnalisé ou non…"])]
            (if (= "Casper" (-> groups-data first second :firstname))
              [:p (t ["En attendant que de vrais élèves vous déclarent comme étant leur professeur, deux élèves fantômes vont vous permettre d’essayer cette interface. Je vous présente Casper et Érika. ! Ces élèves n’ont pas de vrai compte et il n’est pas possible de se connecter au site sous leur identité. Vous pouvez cependant vous connecter au site avec un autre compte afin de créer un élève factice qui vous aurait comme professeur."])])
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
                  (groups-list-of-lists groups-data groups)]
                [:> (bs 'Button)
                  {:style {:margin "1em"}  ; TODO CSS
                   :on-click #(rf/dispatch [:groups-cancel])
                   :bsStyle "warning"}
                  (t ["Annuler les modifications"])]
                [:> (bs 'Button)
                  {:style {:margin "1em"}  ; TODO CSS
                   :on-click #(rf/dispatch [:groups-save])
                   :bsStyle "success"}
                  (t ["Enregistrer les modifications"])]]
            ]])]
    ]))

(defn show-expr-as-li-click
  [expr]
  (let [nom (:nom expr)
        renderExprAsLisp (.-renderExprAsLisp clubexpr)
        lisp (renderExprAsLisp (-> expr :expr clj->js))]
    ^{:key nom}
    [:li
      {:style {:margin "0.5em"}  ; TODO CSS
       :on-double-click #(rf/dispatch [:series-exprs-add lisp])}
      (infix-rendition lisp true)]))

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
    [:div filtering-title-style "Nb d’opérations"]
    [:> Slider
      (merge slider-defaults
             {:value @(rf/subscribe [:series-filtering-nb-ops])
              :onChange #(rf/dispatch [:series-filtering-nb-ops %])})]
    [:div filtering-title-style "Profondeur de l’arbre de calcul"]
    [:> Slider
      (merge slider-defaults
             {:value @(rf/subscribe [:series-filtering-depth])
              :onChange #(rf/dispatch [:series-filtering-depth %])})]
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
  (let [series-data @(rf/subscribe [:series-page])
        sorted-series-data (sort series-comparator series-data)]
    [:div
      [:h2 (t ["Vos séries"])]
      [:ul.nav {:max-height "30em" :overflow-y "scroll"}  ; TODO CSS
        (doall (map series-li sorted-series-data))]]))

(defn show-expr-as-li
  [expr]
  (let [lisp (:content expr)]
    ^{:key lisp}
    [:li {:style {:margin "0.3em"}}  ; TODO CSS
         (infix-rendition lisp true)]))

(defn show-series
  []
  (let [series-data @(rf/subscribe [:series-page])
        series-id  @(rf/subscribe [:current-series-id])
        title @(rf/subscribe [:series-title])
        desc  @(rf/subscribe [:series-desc])
        exprs @(rf/subscribe [:series-exprs])]
    (if (empty? series-id)
      (if (not (empty? series-data))
        [:div
          [:h2 (t ["Aperçu de la série"])]
          [:p (t ["Veuillez sélectionner une série sur la gauche."])]])
      [:div
        [:div.pull-right
          [:> (bs 'Button)
            {:style {:margin-right "1em"}  ; TODO CSS
             :on-click #(rf/dispatch [:series-edit])
             :bsStyle "warning"} (t ["Modifier cette série"])]
          [:> (bs 'Button)
            {:on-click #(rf/dispatch [:series-delete])
             :bsStyle "danger"} (t ["Supprimer cette série"])]]
        [:h3 (if (empty? title) (t ["Sans titre"]) title)]
        (if (empty? desc)
          [:p (t ["Pas de description"])]
          [:p (t ["Description : "]) desc])
        (if (empty? exprs)
          [:p (t ["Pas d’expression dans cette série. Pour en ajouter, cliquer sur « Modifier cette série »."])]
          [:ul.nav (map show-expr-as-li exprs)])])
     ))

(defn edit-series
  []
  (let [exprs @(rf/subscribe [:series-exprs-with-content-key])]
    [:div
      [:h2 (t ["Série en cours de modification"])]
      [:> (bs 'Button)
        {:on-click #(rf/dispatch [:series-cancel])
         :bsStyle "warning"} (t ["Annuler"])]
      [:> (bs 'Button)
        {:style {:margin "1em"}  ; TODO CSS
         :on-click #(rf/dispatch [:series-save])
         :bsStyle "success"} (t ["Enregistrer"])]
      [:> (bs 'Button)
        {:style {:margin "1em"}  ; TODO CSS
         :class "pull-right"
         :on-click #(rf/dispatch [:series-delete])
         :bsStyle "danger"} (t ["Supprimer cette série"])]
      [text-input {:label (t ["Titre"])
                   :placeholder (t ["Découverte du Club"])
                   :help (t ["Titre de la série, vu par les élèves"])
                   :value-id :series-title
                   :event-id :series-title}]
      [text-input {:component-class "textarea"
                   :label (t ["Description"])
                   :placeholder (t ["Expressions triviales pour apprendre à utiliser le Club"])
                   :help (t ["Description de la série, vue seulement par les autres enseignants, mais pas les élèves"])
                   :value-id :series-desc
                   :event-id :series-desc}]
      [:p [:strong (t ["Expressions"])]]
      (if (empty? exprs)
        [:p
          [:strong (t ["La série est vide."])]
          " "
          (t ["En double-cliquant sur une expression sur la gauche, vous pouvez l’ajouter à votre série. "])]
        [:div {:style {:padding-bottom "1em"}}
         [:> Sortable
          {:items exprs
           :moveTransitionDuration 0.3
           :dropBackTransitionDuration 0.3
           :placeholder "< ici >"
           :onSort #(rf/dispatch [:series-exprs-sort %])}]])
      [:p (t ["Pour supprimer une expression de votre série, double-cliquez à nouveau sur celle-ci, mais dans la liste de droite. Il est possible de réordonner vos expression en les glissant chacune au bon endroit."])]
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

(defn swp-prettifier
  [scholar-data]
  (let [{:keys [lastname firstname progress]} scholar-data]
    [:span
      [:span.text-capitalize lastname]
      " "
      [:span.text-capitalize firstname]
      " : "
      (cond
        (nil? progress)
          [:span {:style {:color "#f00"}}  ; TODO CSS
                 (t ["pas commencé"])]
        (= -1 progress)
          [:span {:style {:color "#da0"}}  ; TODO CSS
                 (t ["aucune expr. réussie"])]
        (= -666 progress)
          [:span {:style {:color "#3b0"}}  ; TODO CSS
                 (t ["travail terminé"])]
        (= 0 progress)
          (t ["une expr. réussie"])
        :else
          (str (+ progress 1) (t [" expr. réussies"])))]))

(defn progress-viz
  [scholars progress]
  ; swp for "scholars with progress"
  (let [
        swp (map #(merge (second %) {:progress ((first %) progress)}) scholars)
        swp-sorted (sort scholar-progress-comparator swp)
        swp-pretty (map swp-prettifier swp-sorted)]
    [:ul (doall (map #(identity [:li %]) swp-pretty))]))

(defn work-input
  ([]
    (work-input {:editing true
                 :id ""
                 :to (today-str)
                 :from (today-str)
                 :series-id ""
                 :series-title ""
                 :group ""
                 :scholars {}
                 :progress {}
                 :show-progress false}))
  ([{:keys [editing id to from series-id series-title group scholars progress]
     :as init-state}]
    (let [old-state (r/atom init-state)
          new-state (r/atom init-state)
          datetime-common {:dateFormat "DD/MM/YYYY"
                           :timeFormat false
                           :closeOnSelect true
                           :locale "fr-fr"}
          series-for-select @(rf/subscribe [:series-for-select])
          groups-for-select @(rf/subscribe [:groups-for-select])
          buttons-common {:style {:width "100%"}}  ; TODO CSS
          labels-common {:style {:text-align "center"  ; TODO CSS
                                 :margin "0.5em"}}]
      (fn [{:keys [editing id to from series-id series-title group scholars progress]}]
        ^{:key (if (empty? id) "empty-id" id)}
        [:> (bs 'Row)
          (if (:editing @new-state)
            [:> (bs 'Col) {:xs 1 :md 1}
              (if (empty? (:id @old-state))
                [:div labels-common (t ["Création :"])]
                [:div labels-common (t ["Modif."])])]
            [:> (bs 'Col) {:xs 1 :md 1}
              (let [total (count scholars)
                    finished (count (filter #(= -666 (second %)) progress))
                    wip (- (count progress) finished)
                    nothing (- total (count progress))
                    f-text (t ["travail terminé"])
                    w-text (t ["en cours de travail"])
                    n-text (t ["pas encore vu la série"])]
                [:div labels-common
                  [:a {:on-click #(swap! old-state assoc :show-progress true)
                       :title (str nothing " " n-text "\n"
                                   wip " " w-text "\n"
                                   finished " " f-text)}
                    nothing  "→" wip "→" finished]])])
          ; TO date selection
          [:> (bs 'Col) {:xs 2 :md 2}
            (if (:editing @new-state)
              [:> DateTime
                  (merge datetime-common
                         {:value (:to @new-state)
                          :onChange
                            #(swap! new-state assoc :to (moment->str %))
                          :isValidDate
                            #(and
                               (after?= (moment->cljs-time %)
                                        (today))
                               (after?= (moment->cljs-time %)
                                        (str->cljs-time (:from @new-state))))})]
              [:div labels-common (pretty-date (:to @new-state))])]
          ; SERIES selection
          [:> (bs 'Col) {:xs 2 :md 2}
            (if (:editing @new-state)
              [:> Select
                {:options series-for-select
                 :placeholder (t ["Choisir la série…"])
                 :noResultsText (t ["Pas de série correspondant à cette recherche"])
                 :value (:series-id @new-state)
                 :onChange (fn [sel]
                             (let [sel-clj (-> sel js->clj keywordize-keys)
                                   value (:value sel-clj)
                                   label (:label sel-clj)]
                               (swap! new-state
                                       #(-> % (assoc :series-id value)
                                              (assoc :series-title label)))))}]
              [:div labels-common (:series-title @old-state)])]
          ; GROUP selection
          [:> (bs 'Col) {:xs 2 :md 2}
            (if (:editing @new-state)
              [:> Select
                {:options groups-for-select
                 :placeholder (t ["Choisir le groupe…"])
                 :noResultsText (t ["Pas de groupe correspondant à cette recherche"])
                 :value (:group @new-state)
                 :onChange #(swap! new-state assoc :group
                                   (-> % js->clj keywordize-keys :value))}]
              [:div labels-common (:group @new-state)])]
          ; FROM date selection
          [:> (bs 'Col) {:xs 2 :md 2}
            (if (:editing @new-state)
              [:> DateTime
                  (merge datetime-common
                         {:value (:from @new-state)
                          :onChange #(swap! new-state assoc :from (moment->str %))
                          :isValidDate
                            #(and
                               (after?= (moment->cljs-time %)
                                        (today))
                               (before?= (moment->cljs-time %)
                                         (str->cljs-time (:to @new-state))))})]
              [:div labels-common (pretty-date (:from @new-state))])]
          ; SAVE
          [:> (bs 'Col) {:xs 1 :md 1}
            (if (and (:editing @new-state)
                     (not (empty? (:series-id @new-state)))
                     (not (empty? (:group @new-state))))
              [:> (bs 'Button)
                (merge buttons-common
                       {:on-click
                         #(if (empty? (:id new-state))
                            ; Component used to create new works
                            (do (rf/dispatch [:work-save @new-state])
                                (reset! new-state @old-state))
                            ; Component used for existing works
                            (do (rf/dispatch [:work-save @new-state])
                                (swap! new-state assoc :editing false)
                                (reset! old-state @new-state)))
                        :bsStyle "success"})
                (t ["Enreg."])])]
          ; CANCEL
          [:> (bs 'Col) {:xs 1 :md 1}
            (if (and (:editing @new-state)
                     (if (empty? (:id @new-state))
                       ; creation of a work
                       (or (not (empty? (:series-id @new-state)))
                           (not (empty? (:group @new-state))))
                       ; modification of a work
                       true))
              [:> (bs 'Button)
                (merge buttons-common
                       {:on-click #(reset! new-state @old-state)
                        :bsStyle "warning"})
                (t ["Annuler"])])]
          ; DELETE
          [:> (bs 'Col) {:xs 1 :md 1}
            (if (and (not (empty? (:id @old-state))) (:editing @new-state))
              [:> (bs 'Button)
                (merge buttons-common
                       {:on-click
                         #(do (rf/dispatch [:work-delete @new-state])
                              (reset! new-state @old-state))
                        :bsStyle "danger"})
                (t ["Suppr."])])]
          ; EDIT mode
          [:> (bs 'Col) {:xs 3 :md 3}
            (if (not (:editing @new-state))
              [:> (bs 'Button)
                (merge buttons-common
                       {:on-click #(swap! new-state assoc :editing true)})
                (t ["Modifier ou supprimer"])])]
          [:> (bs 'Modal) {:show (:show-progress @old-state)
                           :onHide #(swap! old-state assoc :show-progress false)}
            [:> (bs 'Modal 'Header) {:closeButton true}
              [:> (bs 'Modal 'Title) (t ["Série "]) (:series-title @old-state)]]
            [:> (bs 'Modal 'Body) [progress-viz scholars progress]]
            [:> (bs 'Modal 'Footer)
              (t ["Si vous voulez voir d’autres informations sur ce travail, prenez contact."])]]]))))

(defn page-work-teacher
  []
  (let [titles-common    {:style {:font-size "170%"
                                  :font-weight "bold"
                                  :text-align "center"  ; TODO CSS
                                  :margin-top "0.5em"}}
        subtitles-common {:style {:color "#999"
                                  :text-align "center"  ; TODO CSS
                                  :margin-bottom "1em"}}
        [past-works future-works] @(rf/subscribe [:works-data-teacher-sorted])]
    [:div
      [:div.jumbotron
        [:h2 (t ["Travaux"])]
        [:p (t ["Attribuez vos séries d’expressions à des groupes d’élèves"])]]
      [:p "Il y a encore quelques bugs d’affichage dans cette page, par exemple après la suppression ou la modification d’un travail. Si un travail perd sa série, se retrouve « en double » ou a une date qui ne s’est pas modifiée, essayez de retourner à l’accueil puis de revenir, peut-être plusieurs fois."]
      [:> (bs 'Grid)
        [:> (bs 'Row)
          [:> (bs 'Col) {:xs 1 :md 1}
            [:div " "]]
          [:> (bs 'Col) {:xs 2 :md 2}
            [:div titles-common (t ["Pour le"])]]
          [:> (bs 'Col) {:xs 2 :md 2}
            [:div titles-common (t ["Série"])]]
          [:> (bs 'Col) {:xs 2 :md 2}
            [:div titles-common (t ["Groupe"])]]
          [:> (bs 'Col) {:xs 2 :md 2}
            [:div titles-common (t ["Depuis le"])]]
          [:> (bs 'Col) {:xs 3 :md 3}
            [:div titles-common (t ["Édition"])]]]
        [:> (bs 'Row)
          [:> (bs 'Col) {:xs 1 :md 1}
            [:div " "]]
          [:> (bs 'Col) {:xs 2 :md 2}
            [:div subtitles-common (t ["(date limite)"])]]
          [:> (bs 'Col) {:xs 2 :md 2}
            [:div subtitles-common (t ["(voir onglet « Séries »)"])]]
          [:> (bs 'Col) {:xs 2 :md 2}
            [:div subtitles-common (t ["(voir onglet « Groupes »)"])]]
          [:> (bs 'Col) {:xs 2 :md 2}
            [:div subtitles-common (t ["(date de publication)"])]]
          [:> (bs 'Col) {:xs 3 :md 3}
            [:div subtitles-common (t [" "])]]]
        [:div {:style {:padding "4px"  ; TODO CSS
                       :border-radius "4px"
                       :background-color "#eee"}}
          ; this first `work-input` to allow the creation of works:
          [work-input]]
        [:hr]
        (if (empty? future-works)
          [:div (t ["Pas de travaux dans le futur"])]
          (doall (map #(identity [work-input %]) future-works))
        )
        [:hr]
        (if (empty? past-works)
          [:div (t ["Pas de travaux dans le passé"])]
          (doall (map #(identity [work-input %]) past-works))
        )
      ]
    ]))

(defn work-todo
  [work]
  ^{:key (:id work)}
  [:li (t ["Pour le "])
       [:strong (pretty-date (:to work))]
       " : "
       [:a {:on-click #(rf/dispatch [:scholar-work work])}
         (t ["Série"])
         " « "
         (:series-title work)
         " »"]])

(defn work-past
  [work]
  ^{:key (:id work)}
  [:li (t ["Fermé le "])
       [:strong (pretty-date (:to work))]
       " : Série « "
       (:series-title work)
       " »"])

(defn scholar-work
  [work-id]
  (let [working @(rf/subscribe [:scholar-working])
        work @(rf/subscribe [:scholar-work work-id])
        exprs (-> work :series :exprs)
        current-expr-idx (:current-expr-idx work)
        current-expr (:current-expr work)
        interactive (:interactive work)
        attempt (:attempt work)
        error (:error work)
        error-style {:style {:color "#f00"}}]  ; TODO CSS
    [:> (bs 'Modal) {:show working
                     :onHide #(rf/dispatch [:close-scholar-work])}
      [:> (bs 'Modal 'Header) {:closeButton true}
        [:> (bs 'Modal 'Title) (t ["Série "]) (-> work :series :title)]]
      [:> (bs 'Modal 'Body)
        (cond
          (empty? exprs)
            [:p (t ["La série est vide !"])]
          (or (= current-expr-idx -666)  ; just finished
              (= current-expr-idx -665)) ; back to a finished work
            [:p (t ["C’est terminé, bravo !"])]
          :else
            [:div
              [:p.pull-right
                (+ 1 current-expr-idx) "/" (count exprs)]
              ; target expr
              [:p (t ["Essayez de reconstituer  :  "])
                (infix-rendition current-expr true)]
              ; Code Club
              [src-input {
                :label (t ["Pour cela tapez du Code Club ci-dessous :"])
                :subs-path :scholar-work-user-attempt
                :evt-handler :scholar-work-attempt-change
                :help (ops-pretty available-ops)}]
              ; current mode
              (if interactive
                [:div
                  [:p (t ["Vous êtes en mode interactif. Votre tentative : "])
                      (infix-rendition attempt true)]]
                [:div
                  [:p (t ["Vous êtes en mode non interactif."])]
                  (if (not (empty? error))
                    [:p.text-center error-style error])])
              ; nature msg
              (if (and (empty? error)
                       (not (= (natureFromLisp current-expr)
                               (natureFromLisp attempt))))
                [:p.text-center error-style
                 (t ["La nature ne correspond pas !"])])
              ; «back to interactive» link
              (if (not interactive)
                [:p.pull-left
                 (t ["Trop difficile !"])
                 " "
                 [:a {:on-click #(rf/dispatch [:back-to-interactive])}
                     (t ["Je repasse en mode interactif."])]])
              ; Check button
              [:div.text-right
                [:> (bs 'Button)
                  {:on-click #(rf/dispatch [:scholar-work-attempt])
                   :disabled (not (empty? error))
                   :bsStyle "primary"}
                  (t ["Vérifier"])]]])]
      [:> (bs 'Modal 'Footer)
        (t ["Vous pouvez fermer cette fenêtre pour continuer plus tard."])]]))

(defn page-work-scholar
  []
  (let [[past-works future-works] @(rf/subscribe [:works-data-scholar-sorted])
        scholar-working @(rf/subscribe [:scholar-working])
        work-id @(rf/subscribe [:scholar-work-id])]
    [:div
      (if scholar-working
        [scholar-work work-id])
      [:div.jumbotron
        [:h2 (t ["Travail à faire"])]
        [:p (t ["Séries d’expressions données par votre professeur"])]]
      [:h2 (t ["Travaux à faire"])]
      (if (empty? future-works)
        [:p (t ["Pas de travail à faire pour le moment."])]
        [:ul (doall (map work-todo future-works))])
      [:p (t ["Vous pouvez vous entraîner avec "])
          [:a {:on-click #(rf/dispatch [:scholar-work {:id "training"}])}
              (t ["ce travail"])
          "."]]
      [:h2 (t ["Travaux passés"])]
      (if (empty? past-works)
        [:p (t ["Pas de travaux dans le passé."])]
        [:ul (doall (map work-past past-works))])
    ]))

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
                  (some #{current-page} [:landing :login-signup :help]))
            (if (= "teacher" quality)
              (case current-page
                :landing [page-landing]
                :login-signup [page-login-signup]
                :help [page-help]
                :profile [page-profile]
                :groups [page-groups]
                :series [page-series]
                :work [page-work-teacher])
              (case current-page
                :landing [page-landing]
                :login-signup [page-login-signup]
                :help [page-help]
                :profile [page-profile]
                :work [page-work-scholar]
                [page-teacher-only]))
            [page-forbidden]
          )
          [footer]
        ]
        (when (and false config/debug?)
          [:pre {:style {:position "absolute" :top "0px" :width "17%"
                         :font-size "50%"}}
            (doall (map #(with-out-str (pprint (% @app-db)))
                        [identity]
                   ))])
      ]
    )))
