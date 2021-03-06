package domain.logic

import java.net.URL
import java.time.{DayOfWeek, LocalDate}
import java.time.format.DateTimeFormatter

import domain.models.{LunchOffer, LunchProvider}
import org.apache.commons.lang3.StringEscapeUtils
import org.htmlcleaner._
import org.joda.money.{CurrencyUnit, Money}
import util.PlayDateTimeHelper._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.matching.Regex

class LunchResolverSuppenkulttour(dateValidator: DateValidator) extends LunchResolver {

  implicit class RegexContext(sc: StringContext) {
    def r = new Regex(sc.parts.mkString, sc.parts.tail.map(_ => "x"): _*)
  }

  implicit class RichTagNode(node: TagNode) {
    def hasClassAttr(cssClassName: String): Boolean = Option(node.getAttributeByName("class")) match {
      case Some(classes) => classes.split(" ").contains(cssClassName)
      case None => false
    }
  }

  sealed abstract class Weekday(val name: String, val order: Int)

  object Weekday {
    case object MONTAG extends Weekday("Montag", 0)
    case object DIENSTAG extends Weekday("Dienstag", 1)
    case object MITTWOCH extends Weekday("Mittwoch", 2)
    case object DONNERSTAG extends Weekday("Donnerstag", 3)
    case object FREITAG extends Weekday("Freitag", 4)

    // TODO: improve with macro, see https://github.com/d6y/enumeration-examples & http://underscore.io/blog/posts/2014/09/03/enumerations.html
    val values = List[Weekday](MONTAG, DIENSTAG, MITTWOCH, DONNERSTAG, FREITAG)
  }

  override def resolve: Future[Seq[LunchOffer]] =
    Future {
      resolve(new URL("http://www.suppenkult.com/wochenplan.html"))
    }

  private[logic] def resolve(url: URL): Seq[LunchOffer] = {
    var result = Seq[LunchOffer]()

    val props = new CleanerProperties
    props.setCharset("utf-8")
    val rootNode = new HtmlCleaner(props).clean(url)

    // Die Wochenangebote sind im section-Element mit der class "ce_accordionStart" enthalten
    for (
      wochenplanSection <- rootNode.evaluateXPath("//section").map { case n: TagNode => n } if wochenplanSection.hasClassAttr("ce_accordionStart")
    ) {
      resolveMonday(wochenplanSection).filter(dateValidator.isValid) match {
        case Some(monday) => result ++= parseOffers(wochenplanSection, monday)
        case None =>
      }
    }
    result
  }

  private def resolveMonday(node: TagNode): Option[LocalDate] = {
    val optDateSeq = for (dateDiv <- node.evaluateXPath("/div[@class='toggler']").map { case n: TagNode => n }) yield {
      dateDiv.getText.toString.replace("\n", " ") match {
        case r""".*Suppen vom +(\d{2}.\d{2}.\d{4})$firstDayString.*""" =>
          parseLocalDate(firstDayString, "dd.MM.yyyy").map { firstDay =>
            firstDay.`with`(DayOfWeek.MONDAY)
          }
        case _ => None
      }
    }
    optDateSeq.head // nicht ganz sauber: nur das erste Datum ist relevant
  }

  private def parseOffers(wochenplanSection: TagNode, monday: LocalDate): Seq[LunchOffer] = {
    // die Daten stecken in vielen, undefiniert angeordneten HTML-Elementen, daher lieber als Reintext auswerten (mit Pipes als Zeilenumbrüchen)
    val wochenplanString = html2text(wochenplanSection)

    val wochensuppenStart = wochenplanString.indexOf("Die Wochensuppen")
    val tagessuppenStart = wochenplanString.indexOf("Die Tagessuppen")

    val wochensuppen =
      if (wochensuppenStart > -1 && wochensuppenStart < tagessuppenStart)
        parseWochensuppen(removeLeadingBars(wochenplanString.substring(wochensuppenStart + 16, tagessuppenStart)), monday)
      else
        Nil

    val tagessuppen =
      if (tagessuppenStart > -1)
        parseTagessuppen(removeLeadingBars(wochenplanString.substring(tagessuppenStart + 15)), monday)
      else
        Nil

    val multipliedWochensuppen = multiplyWochenangebote(wochensuppen, tagessuppen.map(_.day))
    tagessuppen ++ multipliedWochensuppen
  }

  private def parseWochensuppen(text: String, monday: LocalDate): Seq[LunchOffer] = {
    var result = Seq[LunchOffer]()

    for (wochensuppeString <- highlightOfferBorders(text).split("""\|\|""")) {
      val offerAsStringArray = wochensuppeString.split("""\|""").map(_.trim).toList
      val (nameOpt, priceOpt) = parseOfferAttributes(offerAsStringArray)
      for (
        price <- priceOpt;
        name <- nameOpt
      ) result :+= LunchOffer(0, name, monday, price, LunchProvider.SUPPENKULTTOUR.id)
    }
    result
  }

  private def parseTagessuppen(text: String, monday: LocalDate): Seq[LunchOffer] = {
    var result = Seq[LunchOffer]()

    for (tagessuppeString <- highlightOfferBorders(text).split("""\|\|""")) {
      val (weekdayOpt, remainingTagessuppeString) = extractWeekday(tagessuppeString, monday)
      val offerAsStringArray = remainingTagessuppeString.split("""\|""").map(_.trim).toList
      val (nameOpt, priceOpt) = parseOfferAttributes(offerAsStringArray)
      for (
        weekday <- weekdayOpt;
        name <- nameOpt;
        price <- priceOpt
      ) result :+= LunchOffer(0, name, weekday, price, LunchProvider.SUPPENKULTTOUR.id)
    }
    result
  }

  private def highlightOfferBorders(text: String): String =
    text
      .replaceAll("""\| *\|""", "||")
      .replaceAll("""groß *(\d{1,}[.,]\d{2}) ?€? *\|""", """groß $1 €||""")
      .replaceAll("""\|\|\|""", "||")

  private def parseOfferAttributes(offerAttributesAsStrings: Seq[String]): (Option[String], Option[Money]) = {
    var description = List[String]()
    var priceOpt: Option[Money] = None

    val clearedParts = offerAttributesAsStrings.map { part =>
      part.replaceAll("""\( ?([a-zA-Z\d]{1,2}, ?)* ?[a-zA-Z\d]{1,2},? ?\)""", "") // Zusatzinfo (i,j,19) entfernen
        .trim.replaceAll("""^([a-zA-Z\d]{1,2}[, ] ?)* ?[a-zA-Z\d]{1,2},?$""", "")
        .trim.replaceAll("  ", " ") // doppelte Leerzeichen entfernen
    }

    val titleOpt = clearedParts.headOption.map {
      _.trim.replaceFirst("lf, gf$", "").replaceFirst("gf, lf$", "").trim
    }
    val remainingParts = if (clearedParts.nonEmpty) clearedParts.tail else Nil

    remainingParts.foreach {
      case zusatz if isZusatzInfo(zusatz) => // erstmal ignorieren
      case r"""(.+)$portion (\d{1,}[.,]\d{2})$priceStr ?€? *""" => if (portion.trim == "mittel") priceOpt = parsePrice(priceStr)
      case descrPart => description :+= descrPart.trim
    }
    description = description.filter(_.nonEmpty)

    val nameOpt = titleOpt.map(title =>
      if (description.nonEmpty) s"$title: ${description.mkString(" ")}" else title)

    (nameOpt, priceOpt)
  }

  private def multiplyWochenangebote(wochenOffers: Seq[LunchOffer], dates: Seq[LocalDate]): Seq[LunchOffer] = {
    val sortedDates = dates.toSet[LocalDate].toList.sorted
    wochenOffers.flatMap(offer => sortedDates.map(date => offer.copy(day = date)))
  }

  private def html2text(node: TagNode): String = {
    val result = new StringBuffer
    // Zeilenumbrüche durch Pipe-Zeichen ausdrücken
    if (node.getName == "br" || node.getName == "p") result.append("|")
    node.getAllChildren.toArray.foreach {
      case content: ContentNode => result.append(adjustText(StringEscapeUtils.unescapeHtml4(content.getContent)))
      case childNode: TagNode => result.append(html2text(childNode))
      case _ =>
    }
    result.toString
  }

  private def removeLeadingBars(text: String) =
    text.trim.replaceFirst("""^\|\|""", "")
      .replaceFirst("""^\|""", "")

  private def adjustText(text: String) =
    text.replaceAll("–", "-")
      .replaceAll(" , ", ", ")
      .replaceAll("\n", "")
      .replaceAll("\\u00a0", " ") // NO-BREAK SPACE durch normales Leerzeichen ersetzen

  private def isZusatzInfo(string: String) = {
    val zusatzInfos = List("(vegan)", "vegan", "glutenfrei", "lf", "gf", "vegetarisch", "laktosefrei", "veg. gf", "veget.gf", "vegan gf")
    string.split(",").exists(elem => zusatzInfos.contains(elem.trim))
  }

  private def extractWeekday(text: String, monday: LocalDate): (Option[LocalDate], String) = {
    val weekdayNames = Weekday.values.map(_.name).mkString("|")
    val Pattern = ("""^ *(""" + weekdayNames + """) *[-|]*(.*)""").r
    text match {
      case Pattern(weekdayString, remaining) => (parseWeekday(weekdayString, monday), remaining)
      case _ => (None, text)
    }
  }

  private def parseWeekday(weekdayString: String, monday: LocalDate): Option[LocalDate] =
    Weekday.values.find(_.name == weekdayString).map(weekday => monday.plusDays(weekday.order))

  private def parsePrice(priceStr: String): Option[Money] = priceStr match {
    case r""".*(\d{1,})$major[.,](\d{2})$minor.*""" => Some(Money.ofMinor(CurrencyUnit.EUR, major.toInt * 100 + minor.toInt))
    case _ => None
  }

  private def parseLocalDate(dateString: String, dateFormat: String): Option[LocalDate] =
    try {
      Some(LocalDate.from(DateTimeFormatter.ofPattern(dateFormat).parse(dateString)))
    } catch {
      case exc: Throwable => None
    }
}
