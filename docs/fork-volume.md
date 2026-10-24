# Optional TCL volume handling in this fork

This is for the tested TCL Smart TV Pro G05 running Android 11. With Google TV disabled, HomeKeyService restores the app and Settings keys. On this firmware, passing volume through the accessibility filter caused doubled taps and sometimes runaway repeats. Consuming volume events restored one step per tap but removed held repeats.

HomeKeyService can now consume that second pass and provide held-key repeats. This is off unless the theme contains these entries, alongside the existing app mappings:

```json
{
  "key_filter": true,
  "app_keys": ["24:tcl-volume", "25:tcl-volume"]
}
```

Merge the entries into the existing `app_keys` list; do not replace the other shortcuts. Enable only HomeKeyService, since InputKeyService does not implement this target. Keep `gamepad_passthrough` false: that feature can suspend filtering and hide the release event. For that reason held-volume repeats are disabled whenever the gamepad helper is configured. Do not use it on firmware where consuming volume also removes the first step.

The firmware supplies the initial step. After 500 ms, the existing helper adjusts the relevant audio stream by one step per tick. The interval progressively decreases from about 138 ms at the first repeat to 50 ms after four seconds held. There is no artificial limit on the hold duration or number of steps; the TV’s normal volume range still applies. Releasing cancels repetition immediately. A stalled main thread never catches up with a burst. Another key press, standby, cancellation, interruption, unbinding, destruction and config loading also stop it. This mode relies on Android delivering the release or cancellation event; it has no separate duration timeout.

There is no additional service, thread, dependency or permission. No callbacks run while idle. Google TV does not need to run. Audio routes with fixed volume may ignore Android volume adjustments; check the actual output with the physical remote.

On the tested TV, the user reported volume latency in applications, then suspected it was limited to application startup and accepted the current version for now. This remains a known limitation to investigate, rather than a confirmed root cause or a fully resolved symptom. The French field notes below retain the sequence of observations and tests.

Before installing, save the existing APK, theme and accessibility settings privately. To back out safely, disable HomeKeyService first, then restore that APK and theme. Restoring an earlier theme with filtering enabled without this fix can restore the original volume problem. Do not clear launcher data or uninstall it. Keep Home and the screensaver selection unchanged.

## Retour d’expérience : remappage sur notre TCL

Note de suivi du 24 octobre 2026 : **TCL Smart TV Pro G05 (`G05_4K_GB`), Android 11, firmware AR2101/AR21**. Ces observations concernent cet appareil et cette configuration ; elles ne démontrent pas que tous les modèles TCL ont les mêmes défauts.

### Symptômes et essais rencontrés

| Situation | Observation et résultat |
|---|---|
| Accueil Google TV désactivé | Les boutons Netflix, YouTube, Prime Video, TCL Channel, Guard et la roue Paramètres ne répondaient plus ; Media fonctionnait encore. Les raccourcis utiles ont été repris par `HomeKeyService`. Guard a ensuite été laissé volontairement sans action. |
| Service configuré mais pas effectivement lié | Les raccourcis pouvaient rester inactifs. Une entrée dans les paramètres d’accessibilité ne suffit pas : vérifier aussi les services liés dans `dumpsys accessibility`. Après liaison effective du service, les boutons d’applications et Paramètres fonctionnaient. |
| Ancien filtre d’accessibilité actif | Un appui bref sur Volume+ ou Volume− pouvait changer le volume de deux crans au lieu d’un. Ce comportement avait d’abord été accepté, avant la découverte du problème de maintien. |
| Maintien avec l’ancien remappage | Le volume pouvait augmenter très vite, d’environ trente crans après une seconde, et parfois continuer après relâchement. Il ne s’agissait donc pas seulement d’un pas de volume trop grand. |
| Remappage arrêté | La personne a confirmé un volume normal et un arrêt au relâchement. En contrepartie, les raccourcis gérés par le service devenaient inactifs. Désactiver le filtrage n’était donc pas une solution complète. |
| Essai absorbant les événements de volume, sans répétition ajoutée | Les raccourcis fonctionnaient et le volume revenait à un cran par appui, mais maintenir la touche ne répétait plus les crans. Cet essai était un diagnostic, pas la configuration finale. |
| Première répétition contrôlée | Les appuis, maintiens et relâchements fonctionnaient sur l’accueil. La coupure volontaire après deux secondes donnait l’impression d’un plafond d’environ neuf crans. Cette limite et la vitesse constante ont été retirées à la demande de la personne. |
| Version accélérée conservée | Maintien progressif sans plafond artificiel de durée ou de nombre de crans, avec arrêt au relâchement. La plage normale de volume de la télé reste applicable. Les tests avec la vraie télécommande ont permis de conserver cette version. |
| Lancement d’une application ou d’une vidéo | Une impression de latence a été signalée, initialement décrite comme un retour du problème dans les applications. La personne a ensuite précisé qu’elle semblait limitée au lancement de l’application et a demandé de conserver la version actuelle. **Point connu à surveiller : ne pas le considérer comme définitivement résolu.** |

Les traces ont montré des passages répétés des touches de volume dans la gestion des entrées TCL. Le test avec et sans helper établit un lien avec le filtrage d’accessibilité dans cette configuration ; il ne prouve pas à lui seul le mécanisme exact du firmware. Le rôle éventuel du chemin Android `MediaSession` a été envisagé, sans être confirmé. Le remplacement par une commande audio directe n’a pas été essayé ni intégré.

### Configuration conservée

Google TV reste désactivé, conformément à l’objectif d’optimisation. Seul le helper existant `HomeKeyService` assure ces raccourcis ; aucun second service de remappage n’a été ajouté.

| Touche physique | Code observé | Action conservée |
|---|---:|---|
| Netflix | 4062 | Netflix |
| YouTube | 4065 | YouTube |
| Prime Video | 4099 | Prime Video |
| Roue Paramètres | 4077 | Paramètres |
| TCL Channel | 4020 | Free TV, application `net.oqee.androidtv.store` |
| Guard | 4140 | Aucune action, choix volontaire |
| Volume+ / Volume− | 24 / 25 | Cibles `tcl-volume`, décrites ci-dessus |

`key_filter` reste activé et `gamepad_passthrough` désactivé. Ne pas ajouter `InputKeyService` en parallèle pour ces cibles. La répétition s’annule au relâchement, sur un autre événement de touche, en veille ou à l’arrêt du service ; elle dépend de la réception des événements de relâchement ou d’annulation par Android.

### Si le problème revient

1. Noter l’application, si une vidéo joue, si elle vient de démarrer, la sortie audio utilisée et la durée du maintien. Distinguer un retard de la jauge, un retard réel du son et une variation qui continue après relâchement.
2. Comparer l’accueil, le menu de l’application, les premières secondes du lancement et une vidéo déjà stabilisée. Tester d’abord Volume− avec la vraie télécommande ; une touche injectée par ADB ne valide pas ce parcours physique.
3. Vérifier que `HomeKeyService` est réellement lié, que les bonnes cibles sont chargées et qu’aucun autre helper ne filtre les mêmes touches. Un `am force-stop com.gathin.tvhome` coupe aussi les helpers ; les outils officiels de thème et d’installation prévoient leur remise en service lorsqu’ils étaient actifs.
4. Conserver les horodatages des essais avec `dumpsys accessibility`, `dumpsys audio` et les événements d’entrée dans les journaux. Garder ces traces privées et masquer les informations personnelles avant tout partage.
5. Si une variation incontrôlable réapparaît, suspendre le helper avant de poursuivre le diagnostic. Ne pas réinstaller un ancien APK avec son ancien filtre actif comme simple retour arrière, et ne pas réactiver Google TV comme contournement sans une nouvelle décision explicite.
