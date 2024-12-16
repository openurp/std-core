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

import org.beangle.commons.collection.Collections
import org.beangle.data.dao.EntityDao
import org.openurp.base.std.model.Student
import org.openurp.code.edu.model.CourseTakeType
import org.openurp.edu.grade.domain.CourseGradeProvider
import org.openurp.edu.grade.model.CourseGrade
import org.openurp.edu.program.model.Program
import org.openurp.std.graduation.domain.DegreeAuditChecker
import org.openurp.std.graduation.model.DegreeResult

/** 学位审核算术平均分
 */
class DegreeAuditScoreChecker extends DegreeAuditChecker {
  var entityDao: EntityDao = _
  var minScore = 70
  var courseGradeProvider: CourseGradeProvider = _

  override def check(result: DegreeResult, program: Program): (Boolean, String) = {
    val std: Student = result.std
    val grades = courseGradeProvider.getPublished(std).toBuffer
    val removes = Collections.newBuffer[CourseGrade]
    for (g <- grades) {
      if !g.course.calgp then removes.addOne(g) //不计算绩点的
      if (g.courseTakeType.id == (CourseTakeType.Exemption) && g.score.isEmpty) { //免修没有分数的
        removes.addOne(g)
      }
    }
    grades.subtractAll(removes)
    var sum: Double = 0
    for (g <- grades) {
      if (g.score.nonEmpty) {
        sum += g.score.get
      }
    }
    var ga: Double = 0
    if (grades.nonEmpty) {
      ga = (sum / grades.size).round
    }
    val passed = java.lang.Double.compare(minScore.doubleValue, ga) < 1.0
    (passed, s"最低${minScore},平均分${ga}")
  }
}
