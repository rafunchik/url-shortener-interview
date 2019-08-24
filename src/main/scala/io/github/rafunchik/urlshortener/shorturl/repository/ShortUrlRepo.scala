package io.github.rafunchik.urlshortener.shorturl.repository

import io.github.rafunchik.urlshortener.shorturl.model.{OriginalUri, ShortenedUri}

trait ShortUrlRepo {
  def get(url: ShortenedUri): Option[OriginalUri]

  def put(url: ShortenedUri, originalUrl: OriginalUri): Option[OriginalUri]
}
