package models.project

import models.project.Templates._
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsValue, Json, Reads, Writes}

trait TemplateBody


object TemplateBody {

  implicit val tempBodyR: Reads[TemplateBody] = Json.format[TemplateOne].map(x => x: TemplateBody) or
    Json.format[TemplateTwo].map(x => x: TemplateBody) or
    Json.format[TemplateThree].map(x => x: TemplateBody) or
    Json.format[TemplateFour].map(x => x: TemplateBody)


  implicit val tempBodyW = new Writes[TemplateBody] {
    def writes(c: TemplateBody): JsValue = {
      c match {
        case m: TemplateOne => Json.toJson(m)
        case m: TemplateTwo => Json.toJson(m)
        case m: TemplateThree => Json.toJson(m)
        case m: TemplateFour => Json.toJson(m)
        case _ => Json.obj("error" -> "wrong Json")
      }
    }
  }

}