package db

import java.text.SimpleDateFormat
import java.util.Date

object DbUtil {
  private val dbDateFormat: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd")
  val dbDatetimeFormat: SimpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

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

  def dateToString(date: Date): String = {
    dbDateFormat.format(date)
  }

  def datetimeToString(date: Date): String = {
    dbDatetimeFormat.format(date)
  }
}
