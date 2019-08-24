package io.github.rafunchik.urlshortener.shorturl.controller

import java.time.{Clock, LocalDateTime}

import io.github.rafunchik.urlshortener.shorturl.model.{OriginalUri, ShortenedUri}
import io.github.rafunchik.urlshortener.shorturl.repository.ShortUrlRepo
import io.github.rafunchik.urlshortener.shorturl.urlgenerator.UrlGenerator
import org.http4s.Uri
import cats.implicits._
import util.control.Breaks._


case class ShortenedUriDTO(private val url: String) {
  def shortenedUrl = s"http://www.your_service.com/$url"
}

case class OriginalUriDTO(url: Uri)

case class UrlGenerationTimeoutException(message: String) extends Exception

case class ShortUrlController(repo: ShortUrlRepo, generator: UrlGenerator)
                             (implicit clock: Clock) {

  def shortenUrl(originalUrl: Uri)(implicit clock: Clock): Either[Throwable, ShortenedUriDTO] = {
    val originalUri = OriginalUri(originalUrl, Some(LocalDateTime.now(clock)))

    val (randomUrl: String, canPutUrl: Boolean) = tryToGenerateNewShortURL(originalUrl.toString())

    if (canPutUrl){
      repo.put(ShortenedUri(randomUrl), originalUri)
      ShortenedUriDTO(randomUrl).asRight
    }
    else {
      UrlGenerationTimeoutException(s"Timed out generating a new short url for $originalUrl").asLeft
    }
  }

  def getOriginalUrl(shortUrl: String): Option[OriginalUriDTO] = {
    repo.get(ShortenedUri(shortUrl)).map(originalUri => OriginalUriDTO(originalUri.uri))
  }

  private def usedUrl(randomUrl: String): Boolean = {
    repo.get(ShortenedUri(randomUrl)).isDefined
  }

  private def tryToGenerateNewShortURL(originalUrl: String): (String, Boolean) = {
    var randomUrl = generator.generateString(originalUrl)

    var canPutUrl = false
    //TODO make real timeout
    breakable {
      for (i <- 1 to 10) {
        if (usedUrl(randomUrl)) {
          randomUrl = generator.generateString(originalUrl)
        }
        else {
          canPutUrl = true
          break
        }
      }
    }
    (randomUrl, canPutUrl)
  }
}
