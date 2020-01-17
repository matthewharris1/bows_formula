package controllers

import java.time.LocalDateTime
import repositories.{MemberRepository, SessionRepository}
import javax.inject.Inject
import models.{Card, Member, MemberSession}
import play.api.libs.json.{JsResultException, JsValue, Json}
import play.api.mvc._
import reactivemongo.core.errors.DatabaseException
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class MemberController @Inject()(cc: ControllerComponents,
                                 memberRepository: MemberRepository,
                                 sessionRepository: SessionRepository)
                                (implicit ec: ExecutionContext) extends AbstractController(cc) {

  def presentCard(card: Card): Action[AnyContent] = Action.async {
    implicit request =>
      memberRepository.findMemberById(card).flatMap {
        case Some(member) =>
          sessionRepository.findSessionById(card).flatMap {
            case Some(_) =>
              sessionRepository.deleteSessionById(card).map(_ => Ok(s"Goodbye ${member.name}."))
            case None =>
              sessionRepository.createSession(MemberSession(card._id, LocalDateTime.now))
                .map(_ => Ok(s"Welcome ${member.name}."))
          }
        case None => Future.successful(BadRequest("Your card is not registered. Please register your card."))
      } recoverWith {
        case _: JsResultException =>
          Future.successful(BadRequest("Incorrect data - unable to parse Json data to the Member model."))
        case e =>
          Future.successful(BadRequest(s"An issue has occurred resulting in the following exception: $e."))
      }
  }

  def findMemberById(card: Card): Action[AnyContent] = Action.async {
    implicit request: Request[AnyContent] =>
      memberRepository.findMemberById(card).map {
        case None => NotFound("A member could not be found with that card id.")
        case Some(member) => Ok(Json.toJson(member))
      } recoverWith {
        case _: JsResultException =>
          Future.successful(BadRequest("Incorrect data - unable to parse Json data to the Member model."))
        case e =>
          Future.successful(BadRequest(s"An issue has occurred resulting in the following exception: $e."))
      }
  }

  def registerMember: Action[JsValue] = Action.async(parse.json) {
    implicit request =>
      (for {
        member <- Future.fromTry(Try {
          request.body.as[Member]
        })
        _ <- memberRepository.registerMember(member)
      } yield Ok("Member registered successfully.")).recoverWith {
        case _: JsResultException =>
          Future.successful(BadRequest("Incorrect data - unable to parse Json data to the Member model."))
        case _: DatabaseException =>
          Future.successful(BadRequest("Duplicate key - unable to parse Json to the Member model."))
        case e =>
          Future.successful(BadRequest(s"An issue has occurred resulting in the following exception: $e."))
      }
  }

  def addFunds(card: Card, increase: Int): Action[AnyContent] = Action.async {
    memberRepository.findMemberById(card).flatMap {
      case Some(_) =>
        increase match {
          case x if x <= 0 => Future.successful(BadRequest("You must provide a positive amount to increase your funds."))
          case _ =>
            memberRepository.findMemberById(card).flatMap {
              case Some(_) => memberRepository.addFundsById(card, increase)
                .map { _ => Ok("Your funds have been added to your card.") }
            }
        }
      case None => Future.successful(NotFound("A member could not be found with that card id."))
    } recoverWith {
      case _: JsResultException => Future.successful(BadRequest("Incorrect data - unable to parse Json data to the Member model."))
      case e => Future.successful(BadRequest(s"An issue has occurred resulting in the following exception: $e."))
    }
  }

  def checkFunds(card: Card): Action[AnyContent] = Action.async {
    implicit request: Request[AnyContent] =>
      memberRepository.findMemberById(card).map {
        case Some(member) => Ok(Json.toJson(member.funds))
        case None => NotFound("A member could not be found with that card id.")
      } recoverWith {
        case _: JsResultException =>
          Future.successful(BadRequest("Incorrect data - unable to parse Json data to the Member model."))
        case e =>
          Future.successful(BadRequest(s"An issue has occurred resulting in the following exception: $e."))
      }
  }

  def transaction(card: Card, decrease: Int): Action[AnyContent] = Action.async {
    memberRepository.findMemberById(card).flatMap {
      case Some(member) => {
        decrease match {
          case x if x > member.funds => Future.successful(BadRequest("You do not have enough funds to complete this transaction."))
          case _ =>
            memberRepository.findMemberById(card).flatMap {
              case Some(_) =>
                memberRepository.transactionById(card, decrease).map {
                  case Some(_) => Ok("Your transaction was successful.")
                }
            }
        }
      }
      case None => Future.successful(NotFound("A member could not be found with that card id."))
    }.recoverWith {
      case _: JsResultException => Future.successful(BadRequest("Incorrect data - unable to parse Json data to the Member model."))
      case e => Future.successful(BadRequest(s"An issue has occurred resulting in the following exception: $e."))
    }
  }

  def removeMember(card: Card): Action[AnyContent] = Action.async {
    implicit request =>
      memberRepository.removeMemberById(card).map {
        case Some(_) => Ok("Member removed successfully.")
        case _ => NotFound("A member could not be found with that card id.")
      } recoverWith {
        case e =>
          Future.successful(BadRequest(s"An issue has occurred resulting in the following exception: $e."))
      }
  }

  def updateName(card: Card, newName: String): Action[AnyContent] = Action.async {
    implicit request =>
      memberRepository.updateNameById(card, newName).map {
        case Some(member) =>
          Ok(s"The name stored for the id ${member.card._id} has been updated to $newName.")
        case _ =>
          NotFound("A member could not be found with that card id.")
      } recoverWith {
        case e =>
          Future.successful(BadRequest(s"An issue has occurred resulting in the following exception: $e."))
      }
  }

  def updateMobileNumber(card: Card, newNumber: String): Action[AnyContent] = Action.async {
    implicit request =>
      memberRepository.updateMobileNumberById(card, newNumber).map {
        case Some(member) =>
          Ok(s"The mobile number stored for the id ${member.card._id} has been updated to $newNumber.")
        case _ =>
          NotFound("A member could not be found with that card id.")
      } recoverWith {
        case e =>
          Future.successful(BadRequest(s"An issue has occurred resulting in the following exception: $e."))
      }
  }
}
