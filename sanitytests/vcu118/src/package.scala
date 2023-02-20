package sanitytests

import os._

object utils {
  def resource(file: String): Path = Path(java.nio.file.Paths.get(getClass().getClassLoader().getResource(file).toURI))
}
