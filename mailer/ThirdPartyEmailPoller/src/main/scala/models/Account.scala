package models

case class Account(id: Long,
                   firstName: String,
                   lastName: String,
                   username: String,
                   password: String,
                   publicKey: String)
