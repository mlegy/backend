package actors.contribution

import akka.actor.{Actor, Props}
import messages.ContributionMangerMessages.ValidateContribution

class ContributionValidator extends Actor {
  override def receive = {
    case ValidateContribution(contribution) => ???
  }
}

object ContributionValidator {
  def props(): Props = Props(new ContributionValidator)
}