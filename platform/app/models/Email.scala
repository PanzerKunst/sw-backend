package models


case class Email(id: Long,
                 subject: Option[String],
                 textContent: Option[String],
                 htmlContent: Option[String],
                 messageId: String,
                 from: InternetAddress,
                 sender: Option[InternetAddress],
                 fromAccountId: Option[Long],
                 encryptionPublicKey: Option[String],
                 creationTimestamp: Long,
                 status: String)


object Email {
  val STATUS_DRAFT = "DRAFT"
  val STATUS_TO_SEND = "TO_SEND"
  val STATUS_SENT = "SENT"
  val STATUS_UNREAD = "UNREAD"
  val STATUS_READ = "READ"
  val STATUS_ARCHIVED = "ARCHIVED"

  val CONTENT_TYPE_TEXT = "text/plain"
  val CONTENT_TYPE_HTML_WITH_TEXT_FALLBACK__PREFIX = "multipart/alternative"
}