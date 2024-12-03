package controllers

import javax.inject._
import play.api._
import play.api.http.HttpEntity

import java.nio.file.Paths
import play.api.libs.json._
import play.api.mvc._
import play.api.libs.ws._

import scala.concurrent.duration.DurationInt
import scala.concurrent.ExecutionContext
import scala.language.postfixOps

@Singleton
class HomeController @Inject()(
    cc: ControllerComponents,
    config: Configuration,
    ws: WSClient
) (implicit ec: ExecutionContext) extends AbstractController(cc) {

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

  def search() = Action.async(parse.json) { implicit request =>
      val json = request.body.as[JsObject]
      val query = (json \ "query").as[String]
      val history = (json \ "history").as[Seq[JsObject]]
      val docs = (json \ "docs").as[Seq[JsObject]]

      ws
        .url(s"${config.get[String]("server_url")}/chat")
        .withRequestTimeout(5.minutes)
        .post(json)
        .map { response =>
          if (response.status == 200) {
            Ok(response.json)
          } else {
            val errorMsg = s"Server returned status ${response.status}: ${response.body}"
            Logger.error(errorMsg)
            InternalServerError(errorMsg)
          }
        }
        .recover {
          case e: Exception =>
            val errorMsg = s"Exception in search: ${e.getMessage}"
            Logger.error(errorMsg)
            InternalServerError(errorMsg)
        }
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
