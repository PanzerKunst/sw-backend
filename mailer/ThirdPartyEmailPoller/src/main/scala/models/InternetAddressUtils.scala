package models


object InternetAddressUtils {
  val EMAIL_LEFT_WRAPPING_CHAR = '<'
  val EMAIL_RIGHT_WRAPPING_CHAR = '>'

  def toString(internetAddress: InternetAddress): String = {
    internetAddress.name match {
      case Some(name) => name + " " + EMAIL_LEFT_WRAPPING_CHAR + internetAddress.email + EMAIL_RIGHT_WRAPPING_CHAR
      case None => internetAddress.email
    }
  }
}
