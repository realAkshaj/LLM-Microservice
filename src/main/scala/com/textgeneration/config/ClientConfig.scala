package com.textgeneration.config

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._

case class ClientConfig(
                         apiGatewayUrl: String = "https://lthllcut0i.execute-api.us-east-1.amazonaws.com",
                         region: String = "us-east-1",
                         connectionTimeout: FiniteDuration = 5.seconds,
                         idleTimeout: FiniteDuration = 60.seconds,
                         requestTimeout: FiniteDuration = 30.seconds
                       )