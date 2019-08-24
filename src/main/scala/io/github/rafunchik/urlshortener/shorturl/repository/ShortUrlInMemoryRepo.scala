package io.github.rafunchik.urlshortener.shorturl.repository

import io.github.rafunchik.urlshortener.shorturl.model.{OriginalUri, ShortenedUri}

import scala.collection.mutable


object ShortUrlInMemoryRepo {

  def apply(): ShortUrlInMemoryRepo = {
    def map = mutable.Map.empty[ShortenedUri, OriginalUri]
    new ShortUrlInMemoryRepo(map)
  }

}

case class ShortUrlInMemoryRepo(map: mutable.Map[ShortenedUri, OriginalUri]) extends ShortUrlRepo {

  //if we are going to partition by key, the key could be the hash directly
  override def get(url: ShortenedUri): Option[OriginalUri] = map.get(url)

  override def put(url: ShortenedUri,
                   originalUrl: OriginalUri): Option[OriginalUri] = map.put(url, originalUrl)
}
