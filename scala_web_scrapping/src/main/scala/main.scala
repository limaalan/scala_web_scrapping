import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.io._
import java.awt.Desktop
import scala.collection.JavaConverters._
import scala.annotation.tailrec
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration._

object JsoupScraper {
  implicit val ec: ExecutionContext = ExecutionContext.global

  def main(args: Array[String]): Unit = {
    val pesquisa = "dark souls"
    val formularioPesquisa = pesquisa.replace(" ", "+")
    val doc = Jsoup.connect(f"https://notadogame.com/search?order=best-rated&term=${formularioPesquisa}").get()
    val elementos = doc.select(".item-game")

    val links = obterLinks(elementos)
    obterJogos(links, 3)
  }

  def obterLinks(elementos: Elements): List[String] = {
    val links = elementos.select(".item-game-link").eachAttr("href")
    links.asScala.toList
  }

  def obterJogos(links: List[String], limite: Int): Unit = {
    val linksLimitados = links.take(limite)

    val tarefasDeParalelismo = linksLimitados.map { link =>
      Future {
        val doc = Jsoup.connect(link).get()
        val imgJogo = doc.select("#game-cover-image").select("img").attr("src")
        val titulo = doc.select("#game-name").first().select("a").text()
        val descricao = doc.select(".game-description div[data-sumup]").text()
        val avaliacaoComunidade = doc.select("#game-scores-communityScore .item-score").attr("data-score")
        val avaliacaoCritica = doc.select("#game-scores-criticScore .item-score").attr("data-score")
        val nomeAvaliador = doc.select("#game-index-reviews-items .item-review-user-name").eachText().asScala.toList
        val imgAvaliador = doc.select("#game-index-reviews-items .item-user-picture-image").asScala.map(_.attr("src"))
        val avaliacoes = doc.select("#game-index-reviews-items .item-review-header-score .item-score").asScala.map(_.attr("data-score"))
        val comentariosAvaliacao = doc.select("#game-index-reviews-items .item-review-body .item-review-text").eachText().asScala.toList

        val resultadosNuuvem = Nuuvem.scrape(titulo)
        // Transforme os resultados em uma string formatada
        val resultado = nomeAvaliador.zip(imgAvaliador.zip(avaliacoes.zip(comentariosAvaliacao))).take(3).map { case (nome, (img, (avaliacao, comentario))) =>
          s"""
          <li>
            <p>Usuário: $nome</p>
            <img src='$img' width="48" height="60">
            <p>Avaliação: $avaliacao</p>
            <p>Comentário: $comentario</p>
          </li>
          """
        }.mkString("")

        // Retorne a string formatada
        s"""
        <style>
          h2 {
            color: blue;
          }
          p {
            font-size: 14px;
          }
          ul {
            list-style-type: square;
          }
          li {
            margin-bottom: 10px;
          }
        </style>
        <hr></hr>
        <h2>$titulo</h2>
        <img src='$imgJogo'>
        <p>$descricao</p>
        ${resultadosNuuvem.mkString(" ")}
        <p>Avaliação da Comunidade: $avaliacaoComunidade</p>
        <p>Avaliação da Crítica: $avaliacaoCritica</p>
        <ul>
          $resultado
        </ul>
        <br></br>
        """
      }
    }

    val resultadosParalelismo = Future.sequence(tarefasDeParalelismo)

    val todosResultados = Await.result(resultadosParalelismo, 10.seconds)

    // Crie um arquivo HTML e escreva os resultados
    val arquivoHtml = new File("dados.html")
    val escritor = new BufferedWriter(new FileWriter(arquivoHtml))
    escritor.write(todosResultados.mkString(""))
    escritor.close()

    // Abra o arquivo HTML no navegador padrão
    java.awt.Desktop.getDesktop.browse(arquivoHtml.toURI)
  }
}