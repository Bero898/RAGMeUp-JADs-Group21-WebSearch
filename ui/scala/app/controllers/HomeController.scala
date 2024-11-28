package controllers

import javax.inject._
import play.api._
import play.api.http.HttpEntity
import play.api.libs.json._
import play.api.mvc._
import play.api.libs.ws._

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
    Ok(views.html.index(config))
  }

  def add() = Action.async { implicit request: Request[AnyContent] =>
    ws
      .url(s"${config.get[String]("server_url")}/get_documents")
      .withRequestTimeout(5 minutes)
      .get()
      .map(files => {
        Ok(views.html.add(files.json.as[Seq[String]]))
      })
  }

  def search() = Action.async { implicit request: Request[AnyContent] =>
    val json = request.body.asJson.getOrElse(Json.obj()).as[JsObject]
    val query = (json \ "query").as[String]
    val history = (json \ "history").as[Seq[JsObject]]
    val docs = (json \ "docs").as[Seq[JsObject]]

    ws
      .url(s"${config.get[String]("server_url")}/chat")
      .withRequestTimeout(5 minutes)
      .post(Json.obj(
        "prompt" -> query,
        "history" -> history,
        "docs" -> docs
      ))
      .flatMap { response =>
        val jsonResponse = response.json

        // Extract relevant fields from the JSON response
        val reply = (jsonResponse \ "reply").as[String]
        val newHistory = (jsonResponse \ "history").as[Seq[JsObject]]
        val documents = (jsonResponse \ "documents").as[Seq[JsObject]]
        val rewritten = (jsonResponse \ "rewritten").as[Boolean]
        val question = (jsonResponse \ "question").as[String]
        val fetchedNewDocuments = (jsonResponse \ "fetched_new_documents").as[Boolean]

        // Create a JSON object to send back to the client
        val result = Json.obj(
          "reply" -> reply,
          "history" -> newHistory,
          "documents" -> documents,
          "rewritten" -> rewritten,
          "question" -> question,
          "fetched_new_documents" -> fetchedNewDocuments
        )

        Future.successful(Ok(result))
      }
  }

  def generateQuiz() = Action.async { implicit request: Request[AnyContent] =>
    val json = request.body.asJson.getOrElse(Json.obj()).as[JsObject]
    val query = (json \ "query").as[String]

    ws
      .url(s"${config.get[String]("server_url")}/generate_quiz")
      .withRequestTimeout(5 minutes)
      .post(Json.obj("query" -> query))
      .map(response => Ok(response.json))
  }

  def generateAnswers() = Action.async { implicit request: Request[AnyContent] =>
    val json = request.body.asJson.getOrElse(Json.obj()).as[JsObject]
    val questions = (json \ "questions").as[Seq[String]]
    val history = (json \ "history").as[Seq[JsObject]]

    ws
      .url(s"${config.get[String]("server_url")}/generate_answers")
      .withRequestTimeout(5 minutes)
      .post(Json.obj(
        "questions" -> questions,
        "history" -> history
      ))
      .map(response => Ok(response.json))
  }

  def checkAnswers() = Action.async { implicit request: Request[AnyContent] =>
    val json = request.body.asJson.getOrElse(Json.obj()).as[JsObject]
    val userAnswers = (json \ "user_answers").as[Seq[String]]
    val generatedAnswers = (json \ "generated_answers").as[Seq[String]]

    ws
      .url(s"${config.get[String]("server_url")}/check_answers")
      .withRequestTimeout(5 minutes)
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
          // Get the content type and filename from headers
          val contentType = response.header("Content-Type").getOrElse("application/octet-stream")
          val disposition = response.header("Content-Disposition").getOrElse("")
          val filenameRegex = """filename="?(.+)"?""".r
          val downloadFilename = filenameRegex.findFirstMatchIn(disposition).map(_.group(1)).getOrElse(file)

          // Stream the response body to the user
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
          // Handle error cases
          Status(response.status)(s"Error: ${response.statusText}")
        }
      }
  }

  def upload = Action(parse.multipartFormData) { implicit request =>
    request.body.file("file").map { file =>
      val filename = Paths.get(file.filename).getFileName
      val dataFolder = config.get[String]("data_folder")
      val filePath = new java.io.File(s"$dataFolder/$filename")

      file.ref.copyTo(filePath)

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
        val deleteCount = (response.json.as[JsObject] \ "count").as[Int]
        Redirect(routes.HomeController.add())
          .flashing("success" -> s"File ${file} has been deleted (${deleteCount} chunks in total).")
      }
  }

  def feedback() = Action { implicit request: Request[AnyContent] =>
    Ok(Json.obj())
  }
}