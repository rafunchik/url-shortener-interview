package io.github.rafunchik.urlshortener.shorturl

import java.time.Clock

import cats.effect.IO
import cats.implicits._
import io.circe.generic.auto._
import io.circe.literal._
import io.github.rafunchik.urlshortener.shorturl.controller.{ShortUrlController, UrlGenerationTimeoutException}
import org.http4s.circe._
import org.http4s.dsl.io._
import org.http4s.util.CaseInsensitiveString
import org.http4s.{Uri, _}
import org.mockito.ArgumentMatchers.{eq => mockitoEq}
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification


class ShortUrlServiceSpec extends Specification with Mockito {
  import ShortUrlServiceSpec._

  implicit val decoder = jsonOf[IO, ShortenedUriResponse]

  "ShortUrlApp" >> {
    "should return 201 upon a POST to shorten_url with a valid JSON body" >> {
      val request =  Request[IO](method = Method.POST, uri = Uri.uri("/shorten_url"))
        .withBody(ValidURLJSON)
        .unsafeRunSync()
      val response = new ShortUrlService[IO].service.orNotFound(request).unsafeRunSync()
      response.status must beEqualTo(Status.Created)
      val shortUrl = response.as[ShortenedUriResponse].unsafeRunSync()
      shortUrl.shortened_url must startWith("http://www.your_service.com/")
    }

    "should return 400 upon a POST to shorten_url with an invalid JSON body" >> {
      val request = Request[IO](method = Method.POST, uri = Uri.uri("/shorten_url"))
        .withBody(InvalidJSON)
        .unsafeRunSync()

      checkStatus(request, Status.BadRequest)
    }

    "should return 400 upon a POST to shorten_url with a malformed URL in the JSON body" >> {
      val request = Request[IO](method = Method.POST, uri = Uri.uri("/shorten_url"))
        .withBody(MalformedURLJSON)
        .unsafeRunSync()

      checkStatus(request, Status.BadRequest)
      checkBody(request, "DecodingFailure at .url: Uri")
    }

    "should return 400 upon a POST to shorten_url with a malformed JSON body" >> {
      val request = Request[IO](method = Method.POST, uri = Uri.uri("/shorten_url"))
        .withBody(MalformedJSON)
        .unsafeRunSync()

      checkStatus(request, Status.BadRequest)
    }

    "should return 307 upon a GET with a found short url" >> {
      val postRequest =  Request[IO](method = Method.POST, uri = Uri.uri("/shorten_url"))
        .withBody(ValidURLJSON)
        .unsafeRunSync()
      val shortUrlService = new ShortUrlService[IO]
      val postResponse = shortUrlService.service.orNotFound(postRequest).unsafeRunSync()
      val shortUrl = postResponse.as[ShortenedUriResponse].unsafeRunSync()

      val getRequest = Request[IO](Method.GET, Uri.fromString(s"/${shortUrl.shortSuffix}").right.get)
      val getResponse = shortUrlService.service.orNotFound(getRequest).unsafeRunSync()

      getResponse.status must beEqualTo(Status.TemporaryRedirect)
      getResponse.headers.get(CaseInsensitiveString("Location")) must beSome[Header](Header("Location", "www.helloworld.com"))
    }

    "should return 404 upon a GET with a not found short url" >> {
      val request = Request[IO](Method.GET, Uri.uri("/unknown_short_url"))

      checkStatus(request, Status.NotFound)
    }

    "should return 404 upon a POST to a wrong URL" >> {
      val request = Request[IO](Method.POST, Uri.uri("/shorten_url/xxx"))

      checkStatus(request, Status.NotFound)
    }

    "should return 500 upon an application error" >> {
      implicit val clock: Clock = Clock.systemUTC()
      val request = Request[IO](Method.POST, Uri.uri("/shorten_url"))
        .withBody(ValidURLJSON)
        .unsafeRunSync()
      val mockController = mock[ShortUrlController]
      mockController.shortenUrl(any())(any()) returns UrlGenerationTimeoutException("Time out").asLeft

      val response = new ShortUrlService[IO].defineService(mockController).orNotFound(request).unsafeRunSync()

      response.status must beEqualTo(Status.InternalServerError)
    }
  }

  private def checkStatus(request: Request[IO], status: Status) = {
    val response = new ShortUrlService[IO].service.orNotFound(request).unsafeRunSync()
    response.status must beEqualTo(status)
  }
  //TODO merge methods
  private def checkBody(request: Request[IO], body: String) = {
    val response = new ShortUrlService[IO].service.orNotFound(request).unsafeRunSync()
    response.as[String].unsafeRunSync() must beEqualTo(body)
  }
}

object ShortUrlServiceSpec {
  val ValidURLJSON = json"""{"url": "www.helloworld.com"}"""
  val InvalidJSON = json"""{"not url": "www.helloworld.com"}"""
  val MalformedURLJSON = json"""{"url": ":::www.helloworld.com"}"""
  val MalformedJSON = "\"url\": \"www.helloworld.com\"}}}}"
}
