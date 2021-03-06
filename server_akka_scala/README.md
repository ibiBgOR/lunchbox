Lunchbox Server mit Akka & Scala
================================

Dieses Sub-Projekt beschreibt eine Implementierung des Lunchbox-Servers. Die Umsetzung erfolgt mit [Akka](http://akka.io) und [Scala](http://www.scala-lang.org).



Status
------

Beta-Version:

* automatische Ermittlung der Mittagsangebote von Schweinestall, Hotel am Ring, Suppenkulttour & AOK Cafeteria
* Wiedergabe der Mittagsangebote per REST und Web-Feed



Build
-----

* Projektdateien für IntelliJ IDEA generieren: `sbt gen-idea`
* Projektdateien für Eclipse generieren: `sbt eclipse`

* Projekt übersetzen: `sbt compile`



Benutzung
---------

* Server starten: `sbt run`
* Die REST API ist unter [http://localhost:8080/api/v1/](http://localhost:8080/api/v1/) erreichbar
* Der Web-Feed ist unter [http://localhost:8080/feed](http://localhost:8080/feed) erreichbar



Distribution
------------

* `tar.gz`-File im Verzeichnis `target/universal/` erzeugen: `sbt universal:package-zip-tarball`



TODOs
-----

* Dockerfile generieren ...
  * ... via sbt-native-packager [hier](http://www.scala-sbt.org/sbt-native-packager/archetypes/java_server/my-first-project.html) oder [hier](https://github.com/pussinboots/sbt-rpm/blob/master/project/packaging.scala)
  * ... oder via [sbt-docker](https://github.com/marcuslonnberg/sbt-docker)
* [via Vagrant deployen](https://github.com/pussinboots/sbt-rpm)
* Metrics & Tracing, um Antwortzeiten und Last zu überwachen, mit [ActorStacks](http://de.slideshare.net/EvanChan2/akka-inproductionpnw-scala2013) und Frameworks [Kamon, Graphite, Statsd, ...](http://mukis.de/pages/monitoring-akka-with-kamon/)
* Testing: automatisierte Tests erweitern, siehe [Microservice Testing](http://martinfowler.com/articles/microservice-testing/)
  * Unit-Tests für Aktoren schreiben
  * Unit-Tests für API schreiben (mit akka-http-testkit?)
  * Unit-Tests für external Clients schreiben
* Akka-Supervisioning beschreiben
* LunchResolver: globalen durch eigenen ExecutionContext ersetzen?
* Caching der Requests (spray-caching?)
* Spray-JSON ersetzen durch [akka-http-json & Play-JSON](https://github.com/hseeberger/akka-http-json)
* Publish: sbt-release einsetzen + privates Maven-Repo?
* DI für LunchProviderService
* TODOs in Code abarbeiten
* schnelles Re-Deployment mit sbt-revolver ??
* Stoppen per Maintenance ermöglichen



Wissen
------

* [Akka Dokumentation](http://akka.io/docs/)
* [Akka Logging](http://doc.akka.io/docs/akka/2.3.9/scala/logging.html)
* [Akka HTTP: kompaktes Beispiel inkl. JSON-Marshalling](https://typesafe.com/activator/template/akka-http-microservice)
* [Akka HTTP: Beispiel für XML-Marshalling + Authentication](https://github.com/akka/akka/blob/release-2.3-dev/akka-http-tests/src/test/scala/akka/http/server/TestServer.scala)
* [Akka HTTP: Beispiel für Streams + Server-Side-Events](https://github.com/hseeberger/reactive-flows)
* [Feed: Beschreibung des Atom-Formats](http://atomenabled.org/developers/syndication)
* [Feed: Validator](http://validator.w3.org/feed/)
* [Packaging/Distribution: Tutorial für einfache Applikationen](http://www.scala-sbt.org/sbt-native-packager/archetypes/java_app/my-first-project.html)
* [Packaging/Distribution: viele Beispiele für sbt-native-packager](https://github.com/muuki88/sbt-native-packager-examples)
* [Testing: ScalaTest User Guide](http://www.scalatest.org/user_guide)



Urheberrecht
------------

Dieses Projekt beinhaltet beispielhafte Mittagspläne im HTML- & PDF-Format. Sie dienen dem automatisierten Test der Lunchbox-Server-Funktionalität. Urheber und Eigentümer der Dateien ist der jeweilige Anbieter. Die Dateien waren im betroffenen Zeitraum frei zugänglich über die Internetseite des Anbieters abrufbar.
