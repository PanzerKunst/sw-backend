package db


object DbUtil {
  def generateWhereClause(filters: Option[Map[String, String]]) = {
    filters match {
      case Some(filtrs) => {
        filtrs.map {
          case (k, v) => """%s like '%s'""".format(k, backslashQuotes(v))
        }
          .mkString("\nwhere ", "\nand ", "")
      }
      case None => ""
    }
  }

  def backslashQuotes(string: String): String = {
    string.replaceAll("\"", "\\\\\"")
      .replaceAll("'", "\\\\'")
  }

  def parseToList[T](string: String): List[T] = {
    val arrayOfString = string.split(',')

    val listOfCorrectType = for (item <- arrayOfString) yield item.asInstanceOf[T]

    listOfCorrectType.toList
  }
}
