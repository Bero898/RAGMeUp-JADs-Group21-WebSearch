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
  def processQuery(query: String) = {
    ws.url(s"${config.get[String]("server_url")}/chat")
      .withRequestTimeout(5.minutes)
      .withHttpHeaders("Content-Type" -> "application/json")
      .post(Json.obj(
        "query" -> query,
        "history" -> Json.arr(),
        "docs" -> Json.arr()
      ))
      .map { response =>
        try {
          Ok(Json.obj("messages" -> Json.arr((response.json \ "reply").as[String])))
        } catch {
          case e: Exception =>
            logger.error("Error processing response", e)
            InternalServerError(Json.obj("error" -> "Failed to process response"))
        }
      }
      .recover {
        case e: Exception =>
          logger.error("Error calling server", e)
          InternalServerError(Json.obj("error" -> e.getMessage))
      }
  }

  request.body match {
    case AnyContentAsJson(json) =>
      val query = (json \ "query").as[String]
      processQuery(query)
    case AnyContentAsFormUrlEncoded(form) =>
      form.get("query").flatMap(_.headOption) match {
        case Some(query) => processQuery(query)
        case None => Future.successful(BadRequest("Missing query parameter"))
      }
    case _ => Future.successful(BadRequest("Invalid request format"))
  }
}

def generateQuiz() = Action.async { implicit request: Request[AnyContent] =>
  def processQuizRequest(query: String) = {
    ws.url(s"${config.get[String]("server_url")}/generate_quiz")
      .withRequestTimeout(5.minutes)
      .withHttpHeaders("Content-Type" -> "application/json")
      .post(Json.obj("query" -> query))
      .map { response =>
        try {
          val questions = (response.json \ "questions").as[Seq[String]]
          Ok(Json.obj(
            "questions" -> questions,
            "messages" -> Json.arr("Quiz generated successfully")
          ))
        } catch {
          case e: Exception =>
            logger.error("Error processing quiz response", e)
            BadRequest(Json.obj("error" -> "Failed to process quiz response"))
        }
      }
      .recover {
        case e: Exception =>
          logger.error("Error generating quiz", e)
          InternalServerError(Json.obj("error" -> e.getMessage))
      }
  }

  request.body match {
    case AnyContentAsJson(json) =>
      val query = (json \ "query").as[String]
      processQuizRequest(query)
    case AnyContentAsFormUrlEncoded(form) =>
      form.get("query").flatMap(_.headOption) match {
        case Some(query) => processQuizRequest(query)
        case None => Future.successful(BadRequest("Missing query parameter"))
      }
    case _ => Future.successful(BadRequest("Invalid request format"))
  }
}

def checkAnswers() = Action.async { implicit request: Request[AnyContent] =>
  def processAnswers(userAnswers: Seq[String], generatedAnswers: Seq[String]) = {
    ws.url(s"${config.get[String]("server_url")}/check_answers")
      .withRequestTimeout(5.minutes)
      .withHttpHeaders("Content-Type" -> "application/json")
      .post(Json.obj(
        "user_answers" -> userAnswers,
        "generated_answers" -> generatedAnswers
      ))
      .map { response =>
        try {
          val feedback = (response.json \ "feedback").as[Seq[String]]
          Ok(Json.obj("feedback" -> feedback))
        } catch {
          case e: Exception =>
            logger.error("Error processing feedback", e)
            BadRequest(Json.obj("error" -> "Failed to process feedback"))
        }
      }
      .recover {
        case e: Exception =>
          logger.error("Error checking answers", e)
          InternalServerError(Json.obj("error" -> e.getMessage))
      }
  }

  request.body match {
    case AnyContentAsJson(json) =>
      val userAnswers = (json \ "user_answers").as[Seq[String]]
      val generatedAnswers = (json \ "generated_answers").as[Seq[String]]
      processAnswers(userAnswers, generatedAnswers)
    case AnyContentAsFormUrlEncoded(form) =>
      val result = for {
        ua <- form.get("user_answers").map(_.headOption.map(_.split(",").toSeq))
        ga <- form.get("generated_answers").map(_.headOption.map(_.split(",").toSeq))
      } yield (ua, ga)

      result.flatten match {
        case Some((userAnswers, generatedAnswers)) => 
          processAnswers(userAnswers, generatedAnswers)
        case None =>
          Future.successful(BadRequest("Missing answer parameters"))
      }
    case _ => Future.successful(BadRequest("Invalid request format"))
  }
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
    val filePath = Paths.get(s"$dataFolder/$filename")

    file.ref.copyTo(filePath, replace = true)

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
    .recover {
      case e: Exception =>
        logger.error("Error deleting file", e)
        Redirect(routes.HomeController.add())
          .flashing("error" -> s"Failed to delete file: ${e.getMessage}")
    }
}

def feedback() = Action { implicit request: Request[AnyContent] =>
  Ok(Json.obj())
}
}