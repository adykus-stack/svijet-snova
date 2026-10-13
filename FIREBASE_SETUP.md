# Google sinkronizacija — jednokratno povezivanje

Kod za opcionalnu Google prijavu i sinkronizaciju je dio aplikacije. Da bi prijava radila uživo, vlasnik aplikacije mora je jednom povezati s Firebase projektom.

1. U [Firebase konzoli](https://console.firebase.google.com/) napravi poseban projekt **Svijet snova** ili izaberi postojeći projekt koji želiš koristiti.
2. U projektu dodaj Android aplikaciju s nazivom paketa `com.aistudio.svijetsnova.bxzkqp`.
3. Dodaj SHA-1 razvojne verzije: `F4:2E:E3:F9:4E:1E:54:AA:03:91:9D:CD:E8:2A:A9:F0:C9:7D:AC:D7`.
4. Preuzmi `google-services.json` i spremi ga kao `app/google-services.json`.
5. U **Authentication → Sign-in method** uključi **Google**.
6. U **Firestore Database** napravi bazu i zatim objavi pravila iz datoteke `firestore.rules`.
7. Ponovo izgradi i instaliraj aplikaciju.

Prije javne release verzije u Firebase treba dodati i SHA-1 certifikata kojim se potpisuje release APK. Sinkroniziraju se priče, kronike i neosjetljive postavke. API ključevi, PIN i audio datoteke ostaju samo na telefonu.
