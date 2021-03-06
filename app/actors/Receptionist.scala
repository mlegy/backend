package actors

import actors.category.CategoryManager
import actors.contribution.ContributionManager
import actors.enrollment.EnrollmentManager
import actors.project.ProjectManager
import actors.user.UserManager
import akka.actor.{Actor, Props}
import messages.CategoryManagerMessages.CategoryMessage
import messages.ContributionManagerMessages.ContributionMessage
import messages.EnrollmentManagerMessages.EnrollmentMessage
import messages.ProjectManagerMessages.ProjectMessage
import messages.UserManagerMessages.UserMessage
import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Akka

class Receptionist extends Actor {

  val projectManager = context.actorOf(ProjectManager.props(), "projectManager")
  val userManager = context.actorOf(UserManager.props(), "userManager")
  val categoryManager = context.actorOf(CategoryManager.props(), "categoryManager")
  val enrollmentManager = context.actorOf(EnrollmentManager.props(), "enrollmentManager")
  val contributionManager = context.actorOf(ContributionManager.props(), "contributionManager")


  override def receive = {
    case msg: ProjectMessage =>
      Logger.info(s"actor ${self.path} - received msg : $msg ")
      projectManager forward msg

    case msg: UserMessage =>
      Logger.info(s"actor ${self.path} - received msg : $msg ")
      userManager forward msg

    case msg: CategoryMessage =>
      Logger.info(s"actor ${self.path} - received msg : $msg")
      categoryManager forward msg


    case msg: EnrollmentMessage =>
      Logger.info(s"actor ${self.path} - received msg : $msg ")
      enrollmentManager forward msg


    case msg: ContributionMessage =>
      Logger.info(s"actor ${self.path} - received msg : $msg")
      contributionManager forward msg
  }

  override def preStart = {
    Logger.info(s"actor ${self.path} is starting - Initializing DB")
    if (DBUtilities.DBConfig.initDB() == DBUtilities.DBConfig.OPEN_BUCKET_OK)
      Logger.info(s"actor ${self.path} is starting - DB Initialized Successfully")
    else {
      Logger.error(s"actor ${self.path} is starting - DB failed to be Initialized, system is shutting down")
      Akka.system.shutdown()
    }
  }


}

object Receptionist {
  def props(): Props = Props(new Receptionist)
}