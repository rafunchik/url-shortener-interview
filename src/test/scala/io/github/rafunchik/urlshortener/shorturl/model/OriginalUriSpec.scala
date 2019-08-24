package io.github.rafunchik.urlshortener.shorturl.model

import java.time.LocalDateTime

import org.http4s.Uri
import org.specs2.mutable.Specification

class OriginalUriSpec extends Specification {
  "OriginalUri" should {
    "build with no timestamp" in {
      val uri = Uri.uri("www.ibm.com")
      val instance = OriginalUri(uri = uri, None)
      instance.uri must beEqualTo(uri)
      instance.timestamp must beNone
    }
    "build with timestamp" in {
      val uri = Uri.uri("www.ibm.com")
      val dateTime = LocalDateTime.parse("2018-07-28T10:00")
      val instance = OriginalUri(uri = uri, timestamp = Some(dateTime))
      instance.uri must beEqualTo(uri)
      instance.timestamp must beSome(dateTime)
    }
  }
}
