import java.net.SocketTimeoutException

import scala.annotation.tailrec
import scala.xml.{Utility, XML}
import scalaj.http.{Http, HttpRequest, HttpResponse}

object Splitter extends App {

  val host = "https://nitro.api.bbci.co.uk"
  val apiKey = "YOUR_API_KEY_HERE"

  case class Tleo(title: String, pid: String, descendants: Long)
  case class Masterbrand(title: String, mid: String, tleos: Seq[Tleo])

  def getNumberOfDescendants(pid: String): Long = {

    val request = Http(host + "/nitro/api/programmes")
      .timeout(10000, 10000)
      .method("GET")
      .param("api_key", apiKey)
      .param("page_size", "1")
      .param("descendants_of", pid)
      .param("availability", "available")
      .proxy("www-cache.reith.bbc.co.uk", 80)

    val maybeResponse: Option[HttpResponse[String]] = try {
      Some(request.asString)
    } catch {
      case e: SocketTimeoutException => None
    }

    maybeResponse match {
      case Some(r) if r.code == 200 =>
        (XML.loadString(r.body) \\ "results" \ "@total").text.toLong
      case _ =>
        println(s"Request failed: ${request.toString} - trying again")
        getNumberOfDescendants(pid)
    }
  }

  @tailrec def loadTleos(request: HttpRequest, prevTleos: Seq[Tleo] = Seq.empty): Seq[Tleo] = {
    val maybeResponse: Option[HttpResponse[String]] = try {
      Some(request
        .timeout(10000, 10000)
        .method("GET")
        .param("api_key", apiKey)
        .proxy("www-cache.reith.bbc.co.uk", 80)
        .asString)
    } catch {
      case e: SocketTimeoutException => None
    }

    maybeResponse match {
      case None =>
        println(s"Request failed: ${request.toString} - trying again")
        loadTleos(request, prevTleos)
      case Some(r) if r.code != 200 =>
        println(s"Request failed: ${request.toString} with response code: ${r.code} - trying again")
        loadTleos(request, prevTleos)
      case Some(r) if r.code == 200 =>
        val xml = Utility.trim(XML.loadString(r.body))

        val tleos = prevTleos ++ (
          for (node <- (xml \\ "results").head.child) yield {

            val pid = (node \\ "pid").text
            val title = (node \\ "title").text

            Tleo(title, pid, getNumberOfDescendants(pid))
          })

        val next = xml \\ "pagination" \ "next" \ "@href"

        if (next.nonEmpty)
          loadTleos(Http(host + next.text), tleos)
        else
          tleos
    }
  }

  @tailrec def loadMasterbrands(request: HttpRequest, prevMasterbrands: Seq[Masterbrand] = Seq.empty): Seq[Masterbrand] = {
    val maybeResponse: Option[HttpResponse[String]] = try {
      Some(request
        .timeout(10000, 10000)
        .method("GET")
        .param("api_key", apiKey)
        .proxy("www-cache.reith.bbc.co.uk", 80)
        .asString)
    } catch {
      case e: SocketTimeoutException => None
    }

    maybeResponse match {
      case None =>
        println(s"Request failed: ${request.toString} - trying again")
        loadMasterbrands(request, prevMasterbrands)
      case Some(r) if r.code != 200 =>
        println(s"Request failed: ${request.toString} with response code: ${r.code} - trying again")
        loadMasterbrands(request, prevMasterbrands)
      case Some(r) if r.code == 200 =>
        val xml = XML.loadString(r.body)
        val masterbrands = prevMasterbrands ++ (
          for (mbNode <- xml \\ "master_brand") yield {
            val mid = (mbNode \ "mid").text

            val tleoRequest = Http(host + "/nitro/api/programmes")
              .param("page_size", "100")
              .param("tleo", "true")
              .param("master_brand", mid)
              .param("availability", "available")

            val mb = Masterbrand((mbNode \ "name").text, mid, loadTleos(tleoRequest))

            println(s"Masterbrand '${mb.title}' has '${mb.tleos.size}' TLEOs...")
            mb.tleos.foreach { tleo => println(s"\tTLEO '${tleo.title}' has '${tleo.descendants}' descendants") }

            mb
          })

        val next = xml \\ "pagination" \ "next" \ "@href"

        if (next.nonEmpty)
          loadMasterbrands(Http(host + next.text), masterbrands)
        else
          masterbrands
    }
  }

  loadMasterbrands(Http(host + "/nitro/api/master_brands").param("page_size", "100"))
}

