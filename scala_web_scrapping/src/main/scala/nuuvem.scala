import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.io._
import java.awt.Desktop
import scala.collection.JavaConverters._
import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.util.matching.Regex


object Nuuvem {

  def scrape(titulo: String): ListBuffer[String] = {

    val searchForm = titulo.replace(" ", "%20")
    val doc = Jsoup.connect(f"https://www.nuuvem.com/br-pt/catalog/sort/title/sort-mode/asc/page/1/search/${searchForm}").get()
    val elements = doc.select(".product-card--grid")

    val links = scrapeLinks(elements)
    //println((links))

    return scrapeGames(links, 1,titulo)
  }

  def writeHtmlFile(outputFile: File, htmlContent: String): Unit = {
    val writer = new PrintWriter(outputFile)

    writer.write(htmlContent)
    writer.close()
  }

  def openFile(file: File): Unit = {
    if (Desktop.isDesktopSupported && Desktop.getDesktop.isSupported(Desktop.Action.OPEN)) {
      Desktop.getDesktop.open(file)
    } else {
      println("Erro ao abrir arquivo.")
    }
  }

  def scrapeLinks(elements: Elements): List[String] = {
    val links = elements.select("a.product-card--wrapper").eachAttr("href")
    links.asScala.toList
  }

  def scrapeGames(links: List[String], limit: Int,titulo:String): ListBuffer[String] = {
    val limitedLinks = links.take(limit)
    val results = ListBuffer[String]()
    val regex = new Regex("[!:-]")

    @tailrec
    def scrapeHelper(links: List[String]): Unit = {
      if (links.nonEmpty) {
        val link = links.head

        val doc = Jsoup.connect(link).get()
        val preco_reais = doc.select(".product-widget .integer").text()
        val preco_centavos = doc.select(".product-widget .decimal").text()

        val preco=preco_reais+preco_centavos
        val requisitos = doc.select(".product-system-requirements--items")
        val tituloNuuvem = regex.replaceAllIn(doc.select("header .product-title").text().toLowerCase(), "")
        val dlcs  = doc.select(".product-list--main .product-title")
        //println(dlcs)

        // Armazene os dados em uma string formatada
        println( tituloNuuvem + " , " + titulo + "\n")

        val input = regex.replaceAllIn(titulo, "")
        val iguais = tituloNuuvem.equalsIgnoreCase(input)
        println(f"link $link")
        iguais match {
          case true => {
            val data = //s"<h2>$title</h2>" +
              s"<a  href=${link} >R${'$'}$preco na Nuuvem </a>" +
              s"<p>$requisitos</p>" +
              s"<p>$dlcs</p>"
            results += data
          }
          case _ => {
            val data = "Jogo não disponível na plataforma Nuuvem"
            results += data
          }
        }
        scrapeHelper(links.tail)
      }
    }

    scrapeHelper(limitedLinks)

  //Crie um arquivo HTML e escreva os resultados
    //val htmlFile = new File("dados.html")
    //val writer = new BufferedWriter(new FileWriter(htmlFile))
    //writer.write(results.mkString(""))
    //writer.close()

  // Abra o arquivo HTML no navegador padrão
    //java.awt.Desktop.getDesktop.browse(htmlFile.toURI)

    //println(results)
    return results
  }
}
