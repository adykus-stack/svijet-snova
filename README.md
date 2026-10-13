# Svijet snova

Android aplikacija za stvaranje i slušanje dječjih priča na bosanskom i njemačkom jeziku. Za priče i prirodne glasove možeš nezavisno izabrati Gemini ili OpenAI. Aplikacija dodaje instrumentalnu muziku prilagođenu raspoloženju priče.

## Instalacija na telefon

1. Otvori datoteku **Svijet-snova-1.4.apk** u ovom projektu.
2. Pritisni dugme za preuzimanje.
3. Na telefonu otvori preuzetu datoteku i dozvoli instalaciju iz preglednika ako Android to zatraži.

Privatni AI ključevi nisu ugrađeni u APK. Svaki korisnik unosi vlastiti ključ unutar roditeljskih postavki na svom telefonu.

## Izgled i postavke

U **Postavkama** (zupčanik) nalaze se jezik aplikacije, početni AI glas, brzina čitanja, opcionalna Google sinkronizacija, roditeljski nadzor, uvoz ChatGPT priče, ručna Google Drive kopija i tri teme: **Tamna**, **Svijetla** i **Mjesečina**. Svako novo pokretanje aplikacije počinje tamnom temom. Jedanaest kategorija prikazano je kao svjetlucavi oblačići, po dvije u redu, uključujući **Naša porodična priča**. Ako izabrana kategorija ima sačuvane priče, dugme **Otvori priče iz ove kategorije** vodi do njih.

Autor priče (Gemini ili OpenAI) bira se na ekranu **Prilagodi priču**, uz uzrast i jezik, prije dugmeta **Stvori moju priču**. Početni glas je Gemini Aoede; promjena glasa pamti se u Postavkama. Pružalac teksta i glasa mogu biti različiti.

Brzina AI čitanja može se mijenjati od **0,75× do 1,25×** u Postavkama ili direktno u čitaču. Početna brzina je 0,90×. Promjena brzine ne šalje novi API zahtjev i ne mijenja brzinu pozadinske muzike. Offline glas uređaja više nije ponuđen: ako Gemini ili OpenAI glas nije dostupan, čitanje se zaustavlja i prikazuje razlog.

## Postojeća serija iz ChatGPT-a

1. Preuzmi ZIP izvoz podataka sa ChatGPT računa na kojem je serija nastajala.
2. Otvori **Postavke → Uvezi priču iz ChatGPT-a → Izaberi ChatGPT ZIP**. Prihvaća se i `conversations.json`, uključujući numerirane JSON datoteke unutar ZIP-a.
3. Prikazuje se najduži tekstualni razgovor, njegov naslov i kraj teksta. Provjeri da je to prava serija; po potrebi izaberi drugi razgovor.
4. Izaberi kategoriju (početno **Naša porodična priča**), naslov, uzrast, jezik budućih poglavlja i AI autora. Uvoz šalje samo odabrani razgovor izabranom pružaocu i koristi njegovu API kvotu.
5. Aplikacija sažima aktivnu granu razgovora hronološki, u dijelovima do 18.000 znakova, i čuva kroniku s likovima, odnosima, pravilima svijeta, događajima i otvorenim zapletima. Originalni tekst ostaje na telefonu. Ne uvoze se odbačene grane odgovora niti sistemske poruke.
6. Nastaje jedno početno poglavlje koje prepričava dosadašnju radnju. Cilj je 800–950 riječi, odnosno oko sedam minuta mirnog čitanja. Trajanje zavisi od glasa; tri mjeseca originalnog teksta nisu cijela sabijena u izgovoreni arhiv.
7. Otvori seriju iz njene kategorije ili Biblioteke i pritisni **Nastavi radnju**. AI sam bira smislen pravac, koristi kroniku i posljednja dva poglavlja te zajedno vraća novo poglavlje i dopunjenu kroniku. Oboje se automatski sprema. Kroniku možeš pregledati i ispraviti u čitaču.

Uvoz koristi uzastopne zahtjeve s pauzom od 12 sekundi; to ne garantuje da je dovoljna besplatna kvota. Prekid ili API greška čuvaju obrađenu kroniku u privatnoj lokalnoj kontrolnoj tački. Za nastavak ponovo izaberi isti ZIP i razgovor, s istim AI autorom. Kontrolna tačka se briše nakon uspješnog uvoza. Veći ZIP s mnogo priloga može zahtijevati da raspakuješ i izabereš samo `conversations.json`.

## Roditeljski PIN

Pri prvom ulasku u roditeljski nadzor kreiraj četverocifreni PIN i potvrdi ga. Poslije pet pogrešnih pokušaja slijedi kratka pauza. U roditeljskom nadzoru možeš promijeniti PIN. Ako ga zaboraviš, sedam puta zaredom dodirni naslov **PIN**, potvrdi reset i upiši novi PIN dva puta. Reset ne briše priče ni API ključeve. Ovaj način oporavka znači da PIN predstavlja jednostavnu dječju prepreku, ne snažnu zaštitu od nekoga ko zna postupak. Na telefonu se čuva posoljeni izvedeni provjerivač, ne čitljiv PIN.

## Sigurnosna kopija preko Google Drivea

Opcionalna prijava Google računom automatski sinkronizira spremljene priče, kronike, jezik, izabrani AI glas, brzinu čitanja i muzičke postavke. Podaci su odvojeni po prijavljenom korisniku. API ključevi, roditeljski PIN i generirani audio nikada se ne šalju. Za razvojnu instalaciju potrebno je povezati Firebase projekt, uključiti Google prijavu i Firestore, dodati `app/google-services.json` te objaviti pravila iz `firestore.rules`.

U **Postavkama** pritisni **Spremi sigurnosnu kopiju** pa u Androidovom sistemskom prozoru izaberi **Google Drive** i svoj račun. Drive aplikacija treba biti instalirana i prijavljena da bi bila ponuđena. Ova ručna kopija čuva sve spremljene priče, kronike i originalne uvezene razgovore. Nema direktne Drive API prijave niti automatskog zakazivanja. Povezivanje koristi Androidov [Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files).

Na drugom telefonu izaberi **Vrati kopiju s Google Drivea**, otvori kopiju i potvrdi dodavanje u biblioteku. Obnova ne briše postojeće priče i ne dodaje ponovo identične zapise. API ključevi, roditeljski PIN i generirani audio nisu u kopiji; na novom telefonu ključeve unesi ponovo. Baza se nadograđuje s verzije 1 na 2 migracijom koja čuva stare priče.

## Dodavanje privatnog ključa

1. U aplikaciji otvori **Postavke → Roditeljski nadzor** i kreiraj ili unesi svoj PIN.
2. Izaberi **Gemini** ili **OpenAI**, zalijepi ključ i pritisni **Provjeri i spremi ključ**.
3. Autora izaberi pri stvaranju priče; početni AI glas i brzinu čitanja promijeni u Postavkama.

Ključevi su šifrirani pomoću Android Keystore na telefonu. Ne prikazuju se nakon spremanja niti se uključuju u Android sigurnosnu kopiju/prijenos uređaja. Na drugom telefonu unesi ih ponovo. Možeš provjeriti, zamijeniti ili ukloniti pojedinačni ključ bez brisanja priča ili drugog ključa. Uklanjanje s telefona ne poništava ključ kod pružaoca. Zahtjevi za priče/prevod šalju se izabranom pružaocu priča, a tekst za čitanje izabranom pružaocu glasa.

Provjera ključa koristi metapodatke modela, bez generiranja priče ili zvuka; ne potvrđuje saldo ili dostupnu kvotu za generiranje. OpenAI API se obračunava odvojeno od ChatGPT pretplate. Roditelj treba podesiti potrošnju/limite kod pružaoca. Glas je AI-generiran. Modeli su trenutno `gemini-3.8-flash`, `gemini-3.8-flash-lite-tts`, `gpt-6-luna` i `gpt-4o-mini-tts`; dostupnost zavisi od pružaoca i računa. OpenAI govorni model prema trenutnoj dokumentaciji ima najavljeno povlačenje u januaru 2027. i prije tog datuma zahtijeva migraciju.

## Pokretanje izvornog projekta

1. Otvori projekat u Android Studiju.
2. Poveži telefon i pokreni aplikaciju.
3. Unesi API ključ u roditeljskom nadzoru. `.env` se više ne koristi za ključeve u aplikaciji.

Datoteka `.env` nije uključena u GitHub repozitorij.

## Provjera verzije 1.3

Prošlo je 45 provjera na računaru i 20 provjera na povezanom Android telefonu. Dvije provjere stvarnog Gemini glasa (jedna na računaru i jedna na telefonu) namjerno su preskočene da se ne troši API kvota. Provjereni su ZIP/JSON uvoz, aktivna grana razgovora, nastavak prekinutog sažimanja, velika arhiva spremljena u dijelovima, očuvanje starih priča pri nadogradnji baze, obnova bez duplikata, izostavljanje ključeva/PIN-a iz kopije, novi PIN i oporavak sa sedam dodira, izbor autora pri stvaranju te kronika i nastavak serije. Verzija 1.3 instalirana je na telefon bez brisanja podataka. Izgled Postavki i ekrana za stvaranje pregledan je na snimcima s telefona.

Provjere AI odgovora koriste probne podatke. Stvarni korisnikov tromjesečni izvoz još nije uvezen, a spremanje u njegov Google Drive račun treba isprobati u sistemskom biraču datoteka. Kvalitet kronike i trajanje čitanja zavise od AI modela i glasa.

## Provjera verzije 1.2

Prošle su 33 provjere na računaru i 14 provjera na povezanom Android telefonu. Dvije dodatne provjere koje troše stvarnu Gemini API kvotu namjerno su preskočene. Provjereni su početna tamna tema, tri izbora izgleda, očuvanje teme nakon rotacije, jezik i roditeljski nadzor unutar Postavki, raspored kategorija u dvije kolone, odabir svih deset oblačića i nastavak prema uzrastu. Povratak iz Postavki zadržava priču i prethodne izbore. Završni izgled tamne, svijetle i mjesečeve teme pregledan je na snimcima s telefona. Ranije provjere glasa, muzike i privatnih ključeva također su ponovljene.

## Ranija provjera verzije 1.1

Prošlo je 31 provjera na računaru i 12 provjera na povezanom Android telefonu. Dvije dodatne provjere koje troše stvarnu Gemini API kvotu namjerno su preskočene. Provjereni su Android Keystore, odvojeno spremanje/uklanjanje ključeva, očuvanje prethodnog ključa nakon odbijene zamjene, prekid provjere, slanje ključeva samo izabranom pružaocu, ulazak/izlazak iz roditeljskih postavki, skrivanje ključa, čišćenje nacrta i oba jezika. OpenAI zahtjevi za priče, prijevod i govor provjereni su kontroliranim testnim odgovorima; stvarno generiranje i glas treba isprobati vlastitim OpenAI API ključem i dostupnim saldom.

OpenAI povezivanje koristi [Responses API](https://developers.openai.com/api/docs/guides/text) i [Speech API](https://developers.openai.com/api/reference/resources/audio/subresources/speech/methods/create) prema službenoj dokumentaciji.
