package services

import javax.crypto.spec.SecretKeySpec
import javax.crypto.Mac
import org.apache.commons.codec.binary.Hex


object CryptoHelper {
  def hmacSha1(text: String, key: String): String = {
    val signingKey = new SecretKeySpec(key.getBytes, "HmacSHA1")

    // Get an hmac_sha1 Mac instance and initialize with the signing key
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(signingKey)

    // Compute the hmac on input data bytes
    val rawHmac = mac.doFinal(text.getBytes)

    // Convert raw bytes to Hex
    val hexBytes = new Hex().encode(rawHmac)

    //  Covert array of Hex bytes to a String
    new String(hexBytes, "UTF-8")
  }
}
