## Aplikacja

Aplikacja mobilna umożliwiająca przeglądanie, ocenianie oraz zapisywanie przepisów kulinarnych. Projekt skupia się na wygodnym wyszukiwaniu przepisów oraz interakcji użytkowników z treściami.

Opis poszczególnych funkcjonalności:

1. ### 'Login' / 'Register'
  	Rejestracja i logowanie użytkowników oraz możliwość resetowania hasła używając Firebase.

2. ### 'Recipe'
	Szczegółowe wyświetlenie przepisu:
      - zdjęcie,
	  - kaloryczność oraz czas przygotowania,
	  - składniki,
	  - wartości odżywcze,
	  - instrukcje krok po kroku do przygotowania potrawy.
	
3. ### 'SearchFragment'
	Filtrowanie przepisów według:
	  - liczby kalorii,
	  - czasu przygotowania,
	  - wartości odżywczych,
	  - rodzaj posiłku.

4. ### 'Comment'
  	Dodawanie komentarzy, ocen oraz zdjęć (z galerii urządzenia bądź bezpośrednio z aparatu) pod każdym z przepisów.

5. ### 'FavoritesFragment'
	Zapisywanie przepisów do ulubionych.

## Instalacja i uruchomienie

Najlepszym sposobem do przetestowania aplikacji jest pobranie:
  - Android Studio
  - JDK 8 lub nowsze
  - urządzenie z systemem Android lub emulator

Następnie należy:
  1. Sklonować repozytorium:

    git clone https://github.com/xMufc/cookbook.git

  2. Otwórz projekt w Android Studio:

    - File → Open
    - wybierz folder z projektem

  3. Poczekaj na synchronizację Gradle.
  4. Podłącz urządzenie z Androidem lub uruchom emulator (AVD).
  5. Kliknij Run w Android Studio.
  6. Wybierz urządzenie docelowe.
  7. Aplikacja zostanie zainstalowana i uruchomiona automatycznie.
