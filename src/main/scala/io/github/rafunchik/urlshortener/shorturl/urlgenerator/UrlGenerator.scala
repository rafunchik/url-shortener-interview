package io.github.rafunchik.urlshortener.shorturl.urlgenerator


trait UrlGenerator {

  def generateString(url: String, length: Int = 10): String

}
