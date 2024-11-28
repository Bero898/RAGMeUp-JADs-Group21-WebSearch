package controllers

import javax.inject._
import play.api._
import play.api.http.HttpEntity
import play.api.libs.json._
import play.api.mvc._
import play.api.libs.ws._
import java.nio.file.Paths

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps

@Singleton
class HomeController @Inject()(
    cc: ControllerComponents,
    config: Configuration,
    ws: WSClient
)(implicit ec: ExecutionContext) extends AbstractController(cc) {

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index(config, Seq.empty[String]))
  }

  def add() = Action.async { implicit request: Request[AnyContent] =>
    ws
      .url(s"${config.get[String]("server_url")}/get_documents")
      .withRequestTimeout(5.minutes)
      .get()
      .map(files => {
        Ok(views.html.add(files.json.as[Seq[String]]))
      })
  }

  def search() = Action.async { implicit request: Request[AnyContent] =>
    val query = request.body.asFormUrlEncoded.flatMap(_.get("query")).getOrElse(Seq.empty).headOption.getOrElse("")
    val history = Seq.empty[JsObject]
    val docs = Seq.empty[JsObject]

    ws
      .url(s"${config.get[String]("server_url")}/chat")
      .withRequestTimeout(5.minutes)
      .post(Json.obj(
        "prompt" -> query,
        "history" -> history,
        "docs" -> docs
      ))
      .flatMap { response =>
        val jsonResponse = response.json

        val reply = (jsonResponse \ "reply").asOpt[String].getOrElse("")
        val messages = Seq(reply)

        Future.successful(Ok(views.html.index(config, messages)))
      }
  }

  def generateQuiz() = Action.async { implicit request: Request[AnyContent] =>
    val query = request.body.asFormUrlEncoded.flatMap(_.get("query")).getOrElse(Seq.empty).headOption.getOrElse("")

    ws
      .url(s"${config.get[String]("server_url")}/generate_quiz")
      .withRequestTimeout(5.minutes)
      .post(Json.obj("query" -> query))
      .map(response => Ok(response.json))
  }

  def generateAnswers() = Action.async { implicit request: Request[AnyContent] =>
    val questions = request.body.asFormUrlEncoded.flatMap(_.get("questions")).map(_.flatMap(_.split(","))).getOrElse(Seq.empty)
    val history = Seq.empty[JsObject]

    ws
      .url(s"${config.get[String]("server_url")}/generate_answers")
      .withRequestTimeout(5.minutes)
      .post(Json.obj(
        "questions" -> questions,
        "history" -> history
      ))
      .map(response => Ok(response.json))
  }

def checkAnswers() = Action.async { implicit request: Request[AnyContent] =>
  val userAnswers = request.body.asFormUrlEncoded.flatMap(_.get("user_answers")).map(_.flatMap(_.split(","))).getOrElse(Seq.empty)
  val generatedAnswers = request.body.asFormUrlEncoded.flatMap(_.get("generated_answers")).map(_.flatMap(_.split(","))).getOrElse(Seq.empty)

  ws
    .url(s"${config.get[String]("server_url")}/check_answers")
    .withRequestTimeout(5.minutes)
    .post(Json.obj(
      "user_answers" -> userAnswers,
      "generated_answers" -> generatedAnswers
    ))
    .map(response => Ok(response.json))
}
  def download(file: String) = Action.async { implicit request: Request[AnyContent] =>
    ws.url(s"${config.get[String]("server_url")}/get_document")
      .withRequestTimeout(5.minutes)
      .post(Json.obj("filename" -> file))
      .map { response =>
        if (response.status == 200) {
          val contentType = response.header("Content-Type").getOrElse("application/octet-stream")
          val disposition = response.header("Content-Disposition").getOrElse("")
          val filenameRegex = """filename="?(.+)"?""".r
          val downloadFilename = filenameRegex.findFirstMatchIn(disposition).map(_.group(1)).getOrElse(file)

          Result(
            header = ResponseHeader(200, Map(
              "Content-Disposition" -> s"""attachment; filename="$downloadFilename"""",
              "Content-Type" -> contentType
            )),
            body = HttpEntity.Streamed(
              response.bodyAsSource,
              response.header("Content-Length").map(_.toLong),
              Some(contentType)
            )
          )
        } else {
          Status(response.status)(s"Error: ${response.statusText}")
        }
      }
  }

  def upload() = Action(parse.multipartFormData) { implicit request =>
    request.body.file("file").map { file =>
      val filename = Paths.get(file.filename).getFileName.toString
      val dataFolder = config.get[String]("data_folder")
      val filePath = new java.io.File(s"$dataFolder/$filename")

      file.ref.copyTo(filePath, overwrite = true)

      Redirect(routes.HomeController.add()).flashing("success" -> "Added CV to the database.")
    }.getOrElse {
      Redirect(routes.HomeController.add()).flashing("error" -> "Adding CV to database failed.")
    }
  }

  def delete(file: String) = Action.async { implicit request =>
    ws.url(s"${config.get[String]("server_url")}/delete")
      .withRequestTimeout(5.minutes)
      .post(Json.obj("filename" -> file))
      .map { response =>
        val deleteCount = (response.json.as[JsObject] \ "count").asOpt[Int].getOrElse(0)
        Redirect(routes.HomeController.add())
          .flashing("success" -> s"File $file has been deleted ($deleteCount chunks in total).")
      }
  }

  def feedback() = Action { implicit request: Request[AnyContent] =>
    Ok(Json.obj())
  }
}