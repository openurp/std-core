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
import org.openurp.edu.grade.model.AuditPlanResult
import org.openurp.edu.program.domain.ProgramProvider
import org.openurp.edu.program.model.Program
import org.openurp.std.graduation.domain.DegreeAuditChecker
import org.openurp.std.graduation.model.DegreeResult

class DegreeAuditPlanChecker extends DegreeAuditChecker {
  var entityDao: EntityDao = _

  override def check(result: DegreeResult, program: Program): (Boolean, String, String) = {
    val std = result.std
    entityDao.findBy(classOf[AuditPlanResult], "std", std).headOption match
      case None => (false, "培养计划", "找不到计划完成情况")
      case Some(rs) =>
        if (rs.passed && rs.owedCredits <= 0) {
          (rs.passed, "培养计划", s"${rs.requiredCredits}完成${rs.passedCredits}")
        } else {
          (rs.passed, "培养计划", s"缺${rs.owedCredits}")
        }
  }

}
