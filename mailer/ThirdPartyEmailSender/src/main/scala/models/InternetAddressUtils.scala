package models

import javax.mail.internet.InternetAddress

object InternetAddressUtils {
  val EMAIL_LEFT_WRAPPING_CHAR = '<'
  val EMAIL_RIGHT_WRAPPING_CHAR = '>'

  def toString(internetAddress: InternetAddress): String = {
    if (internetAddress.getPersonal == null || internetAddress.getPersonal == "") {
      internetAddress.getAddress
    }
    else {
      internetAddress.getPersonal + " " + EMAIL_LEFT_WRAPPING_CHAR + internetAddress.getAddress + EMAIL_RIGHT_WRAPPING_CHAR
    }
  }
}
