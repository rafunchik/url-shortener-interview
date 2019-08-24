package io.github.rafunchik.urlshortener.shorturl.urlgenerator

import org.specs2.mutable.Specification

class ShortUrlGeneratorSpec extends Specification {

  "ShortUrlGeneratorSpec" should {
    "generate a different String of 10 alphanumerical chars every time" in {
      val url = "www.google.com"

      val generated = Set(
        ShortUrlGenerator.generateString(url),
        ShortUrlGenerator.generateString(url),
        ShortUrlGenerator.generateString(url))

      generated.size must beEqualTo(3)
      forall(generated) {
        url => {
          url.length must beEqualTo(10)
          url must not contain "/"
        }
      }
    }
    "generate a String of a given number of alphanumerical" in {
      val url = "www.google.com"
      val generated = ShortUrlGenerator.generateString(url, length = 5)
      generated.length must beEqualTo(5)
    }
  }
}