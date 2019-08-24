package io.github.rafunchik.urlshortener.shorturl.repository

import io.github.rafunchik.urlshortener.shorturl.model.{OriginalUri, ShortenedUri}
import org.http4s.Uri
import org.specs2.mutable.Specification

class ShortUrlInMemoryRepoSpec extends Specification {

  "ShortUrlInMemoryRepo" should {
    "be able to put a short url - url pair twice and get the latest url" in {
      val repo = ShortUrlInMemoryRepo()
      val short = ShortenedUri("xcdcr53")
      val earlierUrl = OriginalUri(Uri.uri("www.yahoo.com"))
      val url = OriginalUri(Uri.uri("www.google.com"))
      repo.put(short, earlierUrl)
      repo.put(short, url)
      repo.get(short).get must beEqualTo(url)
    }

    "get a None if the short url is not present" in {
      val repo = ShortUrlInMemoryRepo()
      val short = ShortenedUri("xcdcr53")
      repo.get(short) must beNone
    }

    "put and get the same short url - url pair" in {
      val repo = ShortUrlInMemoryRepo()
      val short = ShortenedUri("xcdcr53")
      val url = OriginalUri(Uri.uri("www.ibm.com"))

      repo.put(short, url)
      repo.get(short) must beSome(url)
    }
  }
}
