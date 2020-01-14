package models

import org.scalatest._
import play.api.libs.json.Json

class MemberSpec extends WordSpec with OptionValues with MustMatchers {

  val card: Card = Card("testId0123456789")

  "Member model" must {
    "Deserialize correctly" in {
      val json = Json.obj(
        "_id" -> "testId0123456789",
        "name" -> "David",
        "email" -> "david@email.co.uk",
        "mobileNumber" -> "07123456789",
        "funds" -> 200,
        "securityPin" -> 1234
      )

      val expectedMember = Member(
        card = card,
        name = "David",
        email = "david@email.co.uk",
        mobileNumber = "07123456789",
        funds = 200,
        securityPin = 1234
      )
      json.as[Member] mustEqual expectedMember
    }

    "Serialize correctly" in {
      val member = Member(
        card = card,
        name = "David",
        email = "david@email.co.uk",
        mobileNumber = "07123456789",
        funds = 200,
        securityPin = 1234
      )

      val expectedJson = Json.obj(
        "_id" -> "testId0123456789",
        "name" -> "David",
        "email" -> "david@email.co.uk",
        "mobileNumber" -> "07123456789",
        "funds" -> 200,
        "securityPin" -> 1234
      )
      Json.toJson(member) mustBe expectedJson
    }
  }
}
