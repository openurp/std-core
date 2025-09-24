/*
 * Copyright (C) 2014, The OpenURP Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openurp.std.graduation.service.impl

import org.beangle.data.dao.EntityDao
import org.openurp.edu.grade.model.StdGpa
import org.openurp.edu.program.model.Program
import org.openurp.std.graduation.domain.DegreeAuditChecker
import org.openurp.std.graduation.model.DegreeResult

class DegreeAuditGpaChecker extends DegreeAuditChecker {
  var entityDao: EntityDao = _
  var defaultGpa: Float = 2.0f

  override def check(result: DegreeResult, program: Program): (Boolean, String) = {
    val std = result.std
    entityDao.findBy(classOf[StdGpa], "std", std).headOption match
      case None => (false, "查不到平均绩点")
      case Some(stat) =>
        val gpa = stat.gpa.floatValue
        result.gpa = Some(stat.gpa.floatValue)
        result.ga = Some(stat.wms.floatValue)
        val standard = program.degreeGpa.getOrElse(defaultGpa)
        if (java.lang.Float.compare(standard, gpa) <= 0) {
          (true, s"${gpa}")
        } else {
          (false, s"${gpa}")
        }
  }

}
