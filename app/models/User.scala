package models

import play.api.libs.json.Json

case class About(
                  email: Option[String],
                  bio: Option[String]
                )

object About {
  implicit val AboutF = Json.format[About]
}

case class UserStats(
                  projects: Int,
                  contributions: Int
                )

object UserStats {
  implicit val StatsF = Json.format[UserStats]
}

case class User(
                 id: String,
                 url: String,
                 first_name: Option[String],
                 last_name: Option[String],
                 gender: Option[String],
                 image: Option[String],
                 about: Option[About],
                 stats: Option[UserStats],
                 projects: Option[String], // the User created projects url
                 contributions: Option[String], // the User contributions url
                 created_at: String
               )

object User {
  implicit val UserF = Json.format[User]
}

case class NewUser(
                    id: String,
                    entity_type: String = "user",
                    first_name: Option[String],
                    last_name: Option[String],
                    gender: Option[String],
                    image: Option[String],
                    about: Option[About],
                    stats: Option[UserStats],
                    created_at: String,
                    contributions: String,
                    projects: String,
                    enrolled_projects: List[String]
                  )

object NewUser {
  implicit val NewUserF = Json.format[NewUser]
}

case class EmbeddedOwner(id: String, url: String, name: String, image: String)

object EmbeddedOwner {
  implicit val embeddedOwnerF = Json.format[EmbeddedOwner]
}

case class UserInfo(
                     id: String,
                     first_name: Option[String],
                     last_name: Option[String],
                     gender: Option[String],
                     image: Option[String],
                     about: Option[About],
                     stats: UserStats,
                     projects: String, // the User created projects url
                     contributions: String, // the User created contributions url
                     url: String,
                     enrolled_projects: List[String]
                   )

object UserInfo {
  implicit val UserF = Json.format[UserInfo]
}

case class Contributor(id: String, gender: String)

object Contributor {
  implicit val contF = Json.format[Contributor]
}