package models

import java.time.LocalDateTime
import org.scalatest.{FreeSpec, MustMatchers}
import play.api.libs.json.Json
import mongoDateTimeFormats.MongoDateTimeFormats

class MemberSessionSpec extends FreeSpec with MustMatchers with MongoDateTimeFormats {

  "MemberSession model" - {
    val id = "Nr6rwUHsgmBbSwgz"
    val time = LocalDateTime.now

    "must serialise into JSON" in {
      val memberSession = MemberSession(
        _id = id,
        lastUpdated = time
      )

      val expectedJson = Json.obj(
        "_id" -> id,
        "lastUpdated" -> time
      )
      Json.toJson(memberSession) mustEqual expectedJson
    }

    "must deserialise from JSON" in {
      val json = Json.obj(
        "_id" -> id,
        "lastUpdated" -> time
      )

      val expectedUser = MemberSession(
        _id = id,
        lastUpdated = time.minusHours(1)
      )
      json.as[MemberSession] mustEqual expectedUser
    }
  }
}
