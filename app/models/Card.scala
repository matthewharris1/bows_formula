package models

import play.api.libs.json._
import play.api.mvc.PathBindable

case class Card(_id: String)

object Card {

  implicit val reads: Reads[Card] = (__ \ "_id").read[String].map(Card(_))
  implicit val writes: OWrites[Card] = (__ \ "_id").write[String].contramap(_._id)

  implicit val pathBindable: PathBindable[Card] = {
    new PathBindable[Card] {
      override def bind(key: String, value: String): Either[String, Card] =
        if (value.matches("^[a-zA-Z0-9]{16}$")) Right(Card(value)) else Left("The card ID you have entered is invalid")

      override def unbind(key: String, value: Card): String = value._id
    }
  }
}
