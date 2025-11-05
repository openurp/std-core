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
import org.openurp.code.edu.model.GradeType
import org.openurp.edu.grade.domain.CourseGradeProvider
import org.openurp.edu.program.model.Program
import org.openurp.std.graduation.domain.DegreeAuditChecker
import org.openurp.std.graduation.model.DegreeResult

/** 学位审核--补考次数检查
 */
class DegreeAuditMakupCountChecker extends DegreeAuditChecker {
  var maxCount: Int = 5
  var entityDao: EntityDao = _
  var courseGradeProvider: CourseGradeProvider = _

  override def check(result: DegreeResult, program: Program): (Boolean, String) = {
    val std = result.std
    val grades = courseGradeProvider.get(std)
    var makeupCount: Int = 0
    val gradeType = new GradeType(GradeType.Makeup)
    for (grade <- grades) {
      val eg = grade.getExamGrade(gradeType)
      if (eg.nonEmpty) {
        makeupCount += 1
      }
    }
    val passed: Boolean = makeupCount <= maxCount
    var comment: String = ""
    if (passed) {
      comment = "补考次数" + makeupCount + " 不超过 " + maxCount
    }
    else {
      comment = "补考次数" + makeupCount + "超过" + maxCount
    }
    (passed, comment)
  }

}
