import scala.sys.process.{ Process, stringToProcess }

import alascala.Environment
import alascala.Environment.{_, given}

given Environment = "pwd".!!
