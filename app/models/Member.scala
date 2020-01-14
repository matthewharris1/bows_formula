package models

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class Member(
                   card: Card,
                   name: String,
                   email: String,
                   mobileNumber: String,
                   funds: Int,
                   securityPin: Int
                 )

object Member {

  implicit val reads: Reads[Member] =
    (__.read[Card] and
      (__ \ "name").read[String] and
      (__ \ "email").read[String] and
      (__ \ "mobileNumber").read[String] and
      (__ \ "funds").read[Int] and
      (__ \ "securityPin").read[Int]) (Member.apply _)

  implicit val writes: OWrites[Member] =
    (__.write[Card] and
      (__ \ "name").write[String] and
      (__ \ "email").write[String] and
      (__ \ "mobileNumber").write[String] and
      (__ \ "funds").write[Int] and
      (__ \ "securityPin").write[Int]) (unlift(Member.unapply))
}
