package org.swabs.core.models.user.events

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.swabs.core.models.money.Money.BTC
import org.swabs.core.models.user.events.Transactions.Note
import org.swabs.core.models.user.events.Transactions.Transaction
import org.swabs.core.models.user.events.Transactions.TransactionDateTime
import play.api.libs.json.Json

import java.time.LocalDateTime

class EventsSpec extends AnyWordSpec with Matchers {
  private val jsonStr = """{"signUp":"2023-03-07T16:19:11","transactions":[{"dateTime":"2023-03-07T16:19:11","money":{"currency":"BTC","amount":1.00001},"note":"satoshi was an agorist!"}]}"""

  "Events#writes" must {
    "work" in {
      val json = Json.toJson(Events(
        SignUp("2023-03-07T16:19:11"),
        List(
          Transaction(
            dateTime = TransactionDateTime(LocalDateTime.parse("2023-03-07T16:19:11")),
            money = BTC(1.00001),
            note = Note("satoshi was an agorist!"))
        )
      ))

      Json.stringify(json) mustBe jsonStr
    }
  }

  "Events#reads" must {
    "work" in {
      Json.parse(jsonStr).asOpt[Events] mustBe Some(Events(
        SignUp("2023-03-07T16:19:11"),
        List(
          Transaction(
            dateTime = TransactionDateTime(LocalDateTime.parse("2023-03-07T16:19:11")),
            money = BTC(1.00001),
            note = Note("satoshi was an agorist!"))
        )
      ))
    }
  }
}
