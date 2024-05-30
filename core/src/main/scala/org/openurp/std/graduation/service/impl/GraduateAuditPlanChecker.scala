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

import org.openurp.edu.grade.service.AuditPlanService
import org.openurp.std.graduation.domain.GraduateAuditChecker
import org.openurp.std.graduation.model.GraduateResult

class GraduateAuditPlanChecker extends GraduateAuditChecker {

  var auditPlanService: AuditPlanService = _

  override def check(result: GraduateResult): (Boolean, String, String) = {
    val rs = auditPlanService.audit(result.std, Map.empty, true)
    if (rs.passed && rs.owedCredits <= 0) {
      (rs.passed, "培养计划", s"${rs.requiredCredits}完成${rs.passedCredits}")
    } else {
      (rs.passed, "培养计划", s"缺${rs.owedCredits}")
    }
  }

}
