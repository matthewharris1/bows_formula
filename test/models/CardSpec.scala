package models

import models.Card.pathBindable
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import play.api.libs.json.Json

class CardSpec extends WordSpec with OptionValues with MustMatchers {

  val validCard = "As2Mn3Jh4Lk1JHj2"

  "Card model" must {

    "Deserialise" in {
      val cardId = Card(_id = validCard)
      val expectedJson = Json.obj("_id" -> validCard)

      Json.toJson(cardId) mustEqual expectedJson
    }

    "Serialise" in {
      val expectedCardId = Card(_id = validCard)
      val json = Json.obj("_id" -> validCard)

      json.as[Card] mustEqual expectedCardId
    }

    "return error if card id does not match regex pattern" in {
      val invalidCard = "4yDUm7Nra7ALHuDr!"
      val result = "The card ID you have entered is invalid"

      pathBindable.bind("", invalidCard) mustBe Left(result)
    }

    "return a string" in {
      pathBindable.unbind("", Card("testId0123456789")) mustEqual "testId0123456789"
    }
  }
}
