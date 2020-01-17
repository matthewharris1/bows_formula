package controllers

import java.time.LocalDateTime
import repositories.{MemberRepository, SessionRepository}
import models.{Card, Member, MemberSession}
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.OptionValues._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{MustMatchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar
import play.api.Application
import play.api.inject._
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.{JsResultException, JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.bson.BSONDocument
import reactivemongo.core.errors.DatabaseException
import scala.concurrent.Future

class MemberControllerSpec extends WordSpec with MustMatchers
  with MockitoSugar with ScalaFutures {

  val mockMemberRespository: MemberRepository = mock[MemberRepository]
  val mockSessionRespository: SessionRepository = mock[SessionRepository]

  private lazy val builder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder().overrides(
      bind[MemberRepository].toInstance(mockMemberRespository),
      bind[SessionRepository].toInstance(mockSessionRespository)
    )

  private val card = Card("testId0123456789")

  "presentCard" must {
    "return an OK response and delete current session if one already exists" in {
      when(mockMemberRespository.findMemberById(any()))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmail", "testMobile", 200, 1234))))

      when(mockSessionRespository.findSessionById(any()))
        .thenReturn(Future.successful(Some(MemberSession("testId0123456789", LocalDateTime.now))))

      when(mockSessionRespository.deleteSessionById(any()))
        .thenReturn(Future.successful(UpdateWriteResult.apply(ok = true, 1, 1, Seq.empty, Seq.empty, None, None, None)))

      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.MemberController.presentCard(Card("testId0123456789")).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe "Goodbye testName."

      app.stop
    }

    "return an OK response and create a new session if a session does not exist" in {
      when(mockMemberRespository.findMemberById(any()))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmail", "testMobile", 200, 1234))))

      when(mockSessionRespository.findSessionById(any()))
        .thenReturn(Future.successful(None))

      when(mockSessionRespository.createSession(any()))
        .thenReturn(Future.successful(UpdateWriteResult.apply(ok = true, 1, 1, Seq.empty, Seq.empty, None, None, None)))

      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.MemberController.presentCard(Card("testId0123456789")).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe "Welcome testName."

      app.stop
    }

    "return a BAD_REQUEST response and the correct message if the member does not exist" in {
      when(mockMemberRespository.findMemberById(any()))
        .thenReturn(Future.successful(None))

      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.MemberController.presentCard(Card("testId0123456789")).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "Your card is not registered. Please register your card."

      app.stop
    }

    "return a BAD_REQUEST response if the data is invalid" in {
      when(mockMemberRespository.findMemberById(any()))
        .thenReturn(Future.failed(JsResultException(Seq.empty)))

      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.MemberController.presentCard(Card("testId0123456789")).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "Incorrect data - unable to parse Json data to the Member model."

      app.stop
    }

    "return a BAD_REQUEST response if something else has failed" in {
      when(mockMemberRespository.findMemberById(any()))
        .thenReturn(Future.failed(new Exception))

      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.MemberController.presentCard(Card("testId0123456789")).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "An issue has occurred resulting in the following exception: java.lang.Exception."

      app.stop
    }
  }

  "findMemberById" must {
    "return an OK response and the member details" in {
      when(mockMemberRespository.findMemberById(any()))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmail", "testMobile", 200, 1234))))
      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.MemberController.findMemberById(Card("testId0123456789")).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe OK
      contentAsString(result) must contain
      """{"_id":card,"name":testName,"email":"testEmail","mobileNumber":"testMobile","funds":200,"securityPin":1234}""".stripMargin

      app.stop
    }

    "return a NOT_FOUND response with correct message when the member could not be found" in {
      when(mockMemberRespository.findMemberById(any()))
        .thenReturn(Future.successful(None))
      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.MemberController.findMemberById(Card("incorrectId12345")).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe NOT_FOUND
      contentAsString(result) mustBe "A member could not be found with that card id."

      app.stop
    }

    "return a BAD_REQUEST response if data is invalid" in {
      when(mockMemberRespository.findMemberById(any()))
        .thenReturn(Future.failed(JsResultException(Seq.empty)))

      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.MemberController.findMemberById(Card("testId0123456789")).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "Incorrect data - unable to parse Json data to the Member model."

      app.stop
    }

    "return a BAD_REQUEST response if something else has failed" in {
      when(mockMemberRespository.findMemberById(any()))
        .thenReturn(Future.failed(new Exception))

      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.MemberController.findMemberById(Card("testId0123456789")).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "An issue has occurred resulting in the following exception: java.lang.Exception."

      app.stop
    }
  }

  "registerMember" must {
    "return an OK response with success message if data is valid" in {
      when(mockMemberRespository.registerMember(any()))
        .thenReturn(Future.successful(UpdateWriteResult.apply(ok = true, 1, 1, Seq.empty, Seq.empty, None, None, None)))

      val memberJson: JsValue = Json.toJson(Member(card, "testName", "testEmail", "testMobile", 200, 1234))

      val app: Application = builder.build()

      val request: FakeRequest[JsValue] =
        FakeRequest(POST, routes.MemberController.registerMember().url).withBody(memberJson)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe "Member registered successfully."

      app.stop
    }

    "Return a BAD_REQUEST response with correct error message when the data is invalid" in {
      val memberJson: JsValue = Json.toJson("Invalid Json")

      val app: Application = builder.build()

      val request = FakeRequest(POST, routes.MemberController.registerMember().url).withBody(memberJson)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "Incorrect data - unable to parse Json data to the Member model."

      app.stop
    }

    "Return a BAD_REQUEST response with correct error message when duplicate data is given" in {
      when(mockMemberRespository.registerMember(any()))
        .thenReturn(Future.failed(new DatabaseException {
          override def originalDocument: Option[BSONDocument] = None

          override def code: Option[Int] = None

          override def message: String = "Duplicate key - unable to parse Json to the Member model."
        }))

      val memberJson: JsValue = Json.toJson(Member(card, "testName", "testEmail", "testMobile", 200, 1234))

      val app: Application = builder.build()

      val request =
        FakeRequest(POST, routes.MemberController.registerMember().url).withBody(memberJson)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "Duplicate key - unable to parse Json to the Member model."

      app.stop
    }

    "Return a BAD_REQUEST response with correct error message when something else fails" in {
      when(mockMemberRespository.registerMember(any()))
        .thenReturn(Future.failed(new Exception))

      val memberJson: JsValue = Json.toJson(Member(card, "testName", "testEmail", "testMobile", 200, 1234))

      val app: Application = builder.build()

      val request =
        FakeRequest(POST, routes.MemberController.registerMember().url).withBody(memberJson)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "An issue has occurred resulting in the following exception: java.lang.Exception."

      app.stop
    }
  }

  "addFunds" must {

    "return an OK response with success message if data is valid" in {
      when(mockMemberRespository.addFundsById(any, any))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmail", "testMobile", 200, 1234))))

      when(mockMemberRespository.findMemberById(any))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmail", "testMobile", 200, 1234))))

      val app: Application = builder.build()

      val request = FakeRequest(POST, routes.MemberController.addFunds(Card("testId0123456789"), 234).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe "Your funds have been added to your card."

      app.stop
    }

    "return a BAD_REQUEST response with correct error message if given a negative amount" in {

      val app: Application = builder.build()

      val request = FakeRequest(POST, routes.MemberController.addFunds(Card("testId0123456789"), -300).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "You must provide a positive amount to increase your funds."

      app.stop
    }

    "return a NOT_FOUND response with correct error message when the member could not be found" in {

      when(mockMemberRespository.findMemberById(Card("incorrectId12345")))
        .thenReturn(Future.successful(None))

      val app: Application = builder.build()

      val request =
        FakeRequest(POST, routes.MemberController.addFunds(Card("incorrectId12345"), 200).url)
      val result: Future[Result] = route(app, request).value

      status(result) mustBe NOT_FOUND
      contentAsString(result) mustBe "A member could not be found with that card id."

      app.stop
    }
  }

  "checkFunds" must {
    "return a NOT_FOUND response with correct message when member could not be found" in {
      when(mockMemberRespository.findMemberById(any()))
        .thenReturn(Future.successful(None))

      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.MemberController.checkFunds
      (Card("testId1234567890")).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe NOT_FOUND
      contentAsString(result) mustBe "A member could not be found with that card id."

      app.stop
    }

    "return an OK response with correct funds when correct card id input" in {
      when(mockMemberRespository.findMemberById(any()))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmail", "testMobile", 200, 1234))))

      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.MemberController
        .checkFunds(Card("testId0123456789")).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe "200"

      app.stop
    }

    "return a BAD_REQUEST response if data is invalid" in {
      when(mockMemberRespository.findMemberById(any()))
        .thenReturn(Future.failed(JsResultException(Seq.empty)))

      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.MemberController.checkFunds(Card("testId0123456789")).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "Incorrect data - unable to parse Json data to the Member model."

      app.stop
    }

    "return a BAD_REQUEST response if something else has failed" in {
      when(mockMemberRespository.findMemberById(any()))
        .thenReturn(Future.failed(new Exception))

      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest(GET, routes.MemberController.checkFunds(Card("testId0123456789")).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "An issue has occurred resulting in the following exception: java.lang.Exception."

      app.stop
    }
  }

  "transaction" must {
    "return an OK response with success message if data is valid" in {
      when(mockMemberRespository.transactionById(any, any))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmail", "testMobile", 200, 1234))))

      when(mockMemberRespository.findMemberById(any))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmail", "testMobile", 200, 1234))))


      val app: Application = builder.build()

      val request = FakeRequest(POST, routes.MemberController.transaction(Card("testId0123456789"), 200).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe "Your transaction was successful."

      app.stop
    }

    "return a BAD_REQUEST response with correct error message if transaction cost is higher than total funds" in {
      when(mockMemberRespository.findMemberById(any))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmail", "testMobile", 200, 1234))))

      when(mockMemberRespository.transactionById(any, any))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmail", "testMobile", 200, 1234))))

      val app: Application = builder.build()

      val request = FakeRequest(POST, routes.MemberController.transaction(Card("testId0123456789"), 300).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "You do not have enough funds to complete this transaction."

      app.stop
    }

    "return a NOT_FOUND response with correct error message when member could not be found" in {
      when(mockMemberRespository.findMemberById(any))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmail", "testMobile", 200, 1234))))

      when(mockMemberRespository.findMemberById(Card("incorrectId12345")))
        .thenReturn(Future.successful(None))

      val app: Application = builder.build()

      val request =
        FakeRequest(POST, routes.MemberController.transaction(Card("incorrectId12345"), 200).url)
      val result: Future[Result] = route(app, request).value

      status(result) mustBe NOT_FOUND
      contentAsString(result) mustBe "A member could not be found with that card id."

      app.stop
    }
  }

  "removeMember" must {
    "return an OK response with success message if data is valid" in {
      when(mockMemberRespository.removeMemberById(any()))
        .thenReturn(Future.successful(Some(Json.obj(
          "_id" -> card,
          "name" -> "testName",
          "email" -> "testEmail",
          "mobileNumber" -> "testNumber",
          "funds" -> 200,
          "securityPin" -> 1234
        ))))

      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest(POST, routes.MemberController.removeMember(Card("testId0123456789")).url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe "Member removed successfully."

      app.stop
    }

    "return a NOT_FOUND response with correct error message when the member could not be found" in {
      when(mockMemberRespository.removeMemberById(any()))
        .thenReturn(Future.successful(None
        ))

      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest(POST, routes.MemberController.removeMember(Card("testId0123456789")).url)
      val result: Future[Result] = route(app, request).value

      status(result) mustBe NOT_FOUND
      contentAsString(result) mustBe "A member could not be found with that card id."

      app.stop
    }

    "return a BAD_REQUEST response if something else has failed" in {
      when(mockMemberRespository.removeMemberById(any()))
        .thenReturn(Future.failed(new Exception))

      val app: Application = builder.build()

      val request: FakeRequest[AnyContentAsEmpty.type] =
        FakeRequest(POST, routes.MemberController.removeMember(Card("testId0123456789")).url)
      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "An issue has occurred resulting in the following exception: java.lang.Exception."

      app.stop
    }
  }

  "updateName" must {
    "return an OK response with success message" in {
      when(mockMemberRespository.updateNameById(any, any))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmail", "testMobile", 200, 1234))))

      when(mockMemberRespository.findMemberById(any))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmail", "testMobile", 200, 1234))))


      val app: Application = builder.build()

      val request = FakeRequest(POST, routes.MemberController.updateName(Card("testId0123456789"), "David").url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe "The name stored for the id testId0123456789 has been updated to David."

      app.stop
    }

    "return a NOT_FOUND response with correct error message when member could not be found" in {
      when(mockMemberRespository.updateNameById(any, any))
        .thenReturn(Future.successful(None))

      val app: Application = builder.build()

      val request =
        FakeRequest(POST, routes.MemberController.updateName(Card("incorrectId12345"), "David").url)
      val result: Future[Result] = route(app, request).value

      status(result) mustBe NOT_FOUND
      contentAsString(result) mustBe "A member could not be found with that card id."

      app.stop
    }

    "return a BAD_REQUEST response with correct error message when something else fails" in {
      when(mockMemberRespository.updateNameById(any, any))
        .thenReturn(Future.failed(new Exception))

      val app: Application = builder.build()

      val request =
        FakeRequest(POST, routes.MemberController.updateName(Card("incorrectId12345"), "David").url)
      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "An issue has occurred resulting in the following exception: java.lang.Exception."

      app.stop
    }
  }

  "updateMobileNumber" must {
    "return an OK response with success message when data is valid" in {
      when(mockMemberRespository.updateMobileNumberById(any, any))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmail", "testMobile", 200, 1234))))

      when(mockMemberRespository.findMemberById(any))
        .thenReturn(Future.successful(Some(Member(card, "testName", "testEmail", "testMobile", 200, 1234))))

      val app: Application = builder.build()

      val request = FakeRequest(POST, routes.MemberController.updateMobileNumber(Card("testId0123456789"), "07123456789").url)

      val result: Future[Result] = route(app, request).value

      status(result) mustBe OK
      contentAsString(result) mustBe "The mobile number stored for the id testId0123456789 has been updated to 07123456789."

      app.stop
    }

    "return a NOT_FOUND response with correct error message when member could not be found" in {
      when(mockMemberRespository.updateMobileNumberById(any, any))
        .thenReturn(Future.successful(None))

      val app: Application = builder.build()

      val request =
        FakeRequest(POST, routes.MemberController.updateMobileNumber(Card("incorrectId12345"), "07123456789").url)
      val result: Future[Result] = route(app, request).value

      status(result) mustBe NOT_FOUND
      contentAsString(result) mustBe "A member could not be found with that card id."

      app.stop
    }

    "return a BAD_REQUEST response with correct error message when something else fails" in {
      when(mockMemberRespository.updateMobileNumberById(any, any))
        .thenReturn(Future.failed(new Exception))

      val app: Application = builder.build()

      val request =
        FakeRequest(POST, routes.MemberController.updateMobileNumber(Card("incorrectId12345"), "07123456789").url)
      val result: Future[Result] = route(app, request).value

      status(result) mustBe BAD_REQUEST
      contentAsString(result) mustBe "An issue has occurred resulting in the following exception: java.lang.Exception."

      app.stop
    }
  }
}
