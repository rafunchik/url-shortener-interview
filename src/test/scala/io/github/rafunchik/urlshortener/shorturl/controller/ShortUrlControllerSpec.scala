package io.github.rafunchik.urlshortener.shorturl.controller

import java.time.{Clock, LocalDateTime, ZoneOffset}

import io.github.rafunchik.urlshortener.shorturl.model.{OriginalUri, ShortenedUri}
import io.github.rafunchik.urlshortener.shorturl.repository.ShortUrlInMemoryRepo
import io.github.rafunchik.urlshortener.shorturl.urlgenerator.UrlGenerator
import org.http4s.Uri
import org.specs2.matcher.EitherMatchers
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

class ShortUrlControllerSpec extends Specification with Mockito with EitherMatchers{
  import ShortUrlControllerSpec._

  "ShortUrlControllerSpec" should {

    "shortenUrl successfully creates a timestamped url" in {
      val mockedRepo = mock[ShortUrlInMemoryRepo]
      val mockedUrlGenerator = mock[UrlGenerator].generateString(OriginalUrlStr) returns ShortUrl
      val controller = ShortUrlController(mockedRepo, mockedUrlGenerator)
      val uri = Uri.fromString(OriginalUrlStr).right.get
      mockedRepo.get(ShortenedUri(ShortUrl)) returns None

      val shortened = controller.shortenUrl(uri)

      there was one(mockedRepo).put(ShortenedUri(ShortUrl), OriginalUri(uri, Some(now)))
      shortened must beRight[ShortenedUriDTO]
    }

    "shortenUrl tries and generates a new url, upon a collision" in {
      val shortUrl1 = "ckfpk34"
      val shortUrl2 = "xcrw222"
      val uri = Uri.fromString(OriginalUrlStr).right.get
      val mockedRepo = mock[ShortUrlInMemoryRepo]
      mockedRepo.get(ShortenedUri(shortUrl1)) returns Some(OriginalUri(Uri.uri("")))
      mockedRepo.get(ShortenedUri(shortUrl2)) returns None
      val mockedUrlGenerator = mock[UrlGenerator]
      mockedUrlGenerator.generateString(OriginalUrlStr) returns shortUrl1
      mockedUrlGenerator.generateString(OriginalUrlStr) returns shortUrl2
      val controller = ShortUrlController(mockedRepo, mockedUrlGenerator)

      val shortened = controller.shortenUrl(uri)

      there was one(mockedRepo).put(ShortenedUri(shortUrl2), OriginalUri(uri, Some(now)))
      shortened must beRight[ShortenedUriDTO]
    }

    "shortenUrl try for a limited amount of time, and then timeout with an error" in {
      val shortUrl1 = "ckfpk34"
      val uri = Uri.fromString(OriginalUrlStr).right.get
      val mockedRepo = mock[ShortUrlInMemoryRepo]
      val mockedUrlGenerator = mock[UrlGenerator]
      for (i <- 1 to 10) {
        mockedRepo.get(ShortenedUri(shortUrl1)) returns Some(OriginalUri(Uri.uri("")))
        mockedUrlGenerator.generateString(OriginalUrlStr) returns shortUrl1
      }
      val controller = ShortUrlController(mockedRepo, mockedUrlGenerator)

      val shortened = controller.shortenUrl(uri)

      there was no(mockedRepo).put(ShortenedUri(shortUrl1), OriginalUri(uri, Some(now)))
      shortened must beLeft
    }

    "getOriginalUrl returns None if there is no record for the url" in {
      val mockedUrlGenerator = mock[UrlGenerator]
      val repo = mock[ShortUrlInMemoryRepo]
      repo.get(ShortenedUri(ShortUrl)) returns None
      val controller = ShortUrlController(repo, mockedUrlGenerator)

      controller.getOriginalUrl(ShortUrl) must beNone
    }

    "getOriginalUrl successfully retrieves a saved url" in {
      val originalUri = OriginalUri(TheOriginalUri)
      val expectedUrlDTO = OriginalUriDTO(originalUri.uri)
      val mockedUrlGenerator = mock[UrlGenerator]
      val repo = mock[ShortUrlInMemoryRepo].get(ShortenedUri(ShortUrl)) returns Some(originalUri)
      val controller = ShortUrlController(repo, mockedUrlGenerator)

      controller.getOriginalUrl(ShortUrl) must beSome(expectedUrlDTO)
    }

  }
}

object ShortUrlControllerSpec {
  private val OriginalUrlStr = "www.ibm.com"
  private val TheOriginalUri = Uri.fromString(OriginalUrlStr).right.get
  private val ShortUrl = "ckfpk34"
  private val now = LocalDateTime.parse("2018-07-28T10:00")
  private implicit val mockedClock: Clock = Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC)
}
