package mongoDateTimeFormats

import java.time.{LocalDate, LocalDateTime}
import org.scalatest.{FreeSpec, MustMatchers, OptionValues}
import play.api.libs.json.Json

class MongoDateTimeFormatsSpec extends FreeSpec with MustMatchers with OptionValues with MongoDateTimeFormats {

  "LocalDateTime" - {
    val date = LocalDate.of(2019, 10, 8).atStartOfDay

    val dateMillis = 1570492800000L

    val json = Json.obj(
      "$date" -> dateMillis
    )

    "must serialise to Json" in {
      val result = Json.toJson(date)
      result mustEqual Json.obj(
        "$date" -> (dateMillis - 3600000L)
      )
    }

    "must deserialise from Json" in {
      val result = json.as[LocalDateTime]
      result mustEqual date
    }

    "must serialise/deserialise to the same value" in {
      val result = Json.toJson(date).as[LocalDateTime]
      result mustEqual date.minusHours(1)
    }
  }
}
