package io.github.rafunchik.urlshortener.shorturl.model

import java.time.LocalDateTime

import org.http4s.Uri

case class OriginalUri(uri: Uri, timestamp: Option[LocalDateTime] = None)

