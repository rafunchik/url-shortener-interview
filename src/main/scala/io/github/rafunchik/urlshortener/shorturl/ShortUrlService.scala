package io.github.rafunchik.urlshortener.shorturl

import java.time.Clock

import cats.effect.{Effect, IO}
import cats.implicits._
import io.circe.DecodingFailure
import io.circe.generic.auto._
import io.circe.syntax._
import io.github.rafunchik.urlshortener.shorturl.controller._
import io.github.rafunchik.urlshortener.shorturl.repository.ShortUrlInMemoryRepo
import io.github.rafunchik.urlshortener.shorturl.urlgenerator.ShortUrlGenerator
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Location
import org.http4s.{HttpService, Uri}


case class OriginalUrlRequest(url: Uri)

case class ShortenedUriResponse(shortened_url: String) {

  def shortSuffix: String = {
    val i = shortened_url.lastIndexOf("/")
    shortened_url.substring(i + 1)
  }
}

class ShortUrlService[F[_]: Effect] extends Http4sDsl[F] {

  private implicit val decoder = jsonOf[IO, OriginalUrlRequest]

  private implicit val clock: Clock = Clock.systemUTC()

  private val controller = ShortUrlController(ShortUrlInMemoryRepo(), ShortUrlGenerator)

  val service: HttpService[F] = defineService(controller)

  def defineService(shortUrlController: ShortUrlController): HttpService[F] = {

    HttpService[F] {

      case GET -> Root / shortUrl =>

        shortUrlController.getOriginalUrl(shortUrl) match {
          case Some(urlDTO)      => TemporaryRedirect(Location(urlDTO.url))
          case _                 => NotFound("Short URL not found")
        }

      case request@POST -> Root / "shorten_url" =>
        import org.http4s.circe.CirceEntityDecoder._

        val response = request.as[OriginalUrlRequest].attempt.flatMap {
          case Right(originalUrl) =>
            shortUrlController.shortenUrl(originalUrl.url) match {
              case Right(shortenedUrl) => createdSuccessfully(shortenedUrl)
              case Left(e)             => internalServerError(e)
            }
          case Left(error) => badRequest(error)
        }
        response

      case (PUT | PATCH | DELETE) => MethodNotAllowed("Only GET with a url param and POST to shorten_url allowed")
    }

  }

  private def createdSuccessfully(shortenedUrl: ShortenedUriDTO) = {
    Created(ShortenedUriResponse(shortenedUrl.shortenedUrl).asJson)
  }

  private def internalServerError(f: Throwable) = {
    f.getCause match {
      case (e: UrlGenerationTimeoutException) => InternalServerError(e.message)
      case _                                  => InternalServerError("Error generating a shortened url")
    }
  }

  private def badRequest(f: Throwable) = {
    f.getCause match {
      case (e: DecodingFailure) => BadRequest(e.show)
      case _                    => BadRequest(f.getLocalizedMessage)
    }
  }
}
