package io.github.rafunchik.urlshortener.shorturl.model

import org.specs2.mutable.Specification

class ShortenedUriSpec extends Specification {
  "ShortenedUri" should {
    "build" in {
      val uriStr = "www.ibm.com"
      val instance = ShortenedUri(url = uriStr)
      instance.url must beEqualTo(uriStr)
    }
  }
}
