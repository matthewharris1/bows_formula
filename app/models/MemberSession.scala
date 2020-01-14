package models

import java.time.LocalDateTime
import play.api.libs.json._
import mongoDateTimeFormats.MongoDateTimeFormats

case class MemberSession(_id: String, lastUpdated: LocalDateTime)

object MemberSession extends MongoDateTimeFormats {
  implicit lazy val format: OFormat[MemberSession] = Json.format
}
