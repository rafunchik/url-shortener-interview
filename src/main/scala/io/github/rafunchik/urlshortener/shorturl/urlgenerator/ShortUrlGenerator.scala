package io.github.rafunchik.urlshortener.shorturl.urlgenerator

import scala.util.Random

object ShortUrlGenerator extends UrlGenerator{

  def generateString(url: String, length: Int = 10): String = {
    val randomStream = Random.alphanumeric
    (randomStream take length).mkString("")
  }
}
