package user

import core.AbstractSpec
import helpers.Helper
import messages.UserManagerMessages.ListProjectsOfUser
import models.Response
import models.errors.GeneralErrors.NotFoundError
import models.project.Project.EmbeddedProject

class UserEnrolledProjectsSpec extends AbstractSpec {

  // User have enrolled in a project or more
  "Receptionist Actor" should "Get user enrolled projects Successfully" in {
    receptionist ! ListProjectsOfUser("user::117521628211683444029", Helper.EnrolledKeyword, 0, 20)
    val response = expectMsgType[Response]
    assert(response.jsonResult.validate[Seq[EmbeddedProject]].isSuccess)
  }

  // User have not enrolled in any projects
  it should "Return NOT FOUND error" in {
    receptionist ! ListProjectsOfUser("invalid-id", Helper.EnrolledKeyword, 0, 20)
    expectMsgType[NotFoundError]
  }
}