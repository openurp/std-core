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

import org.beangle.commons.lang.Strings
import org.beangle.data.dao.{EntityDao, OqlBuilder}
import org.openurp.edu.grade.model.{CourseGrade, Grade}
import org.openurp.edu.program.model.Program
import org.openurp.std.graduation.domain.DegreeAuditChecker
import org.openurp.std.graduation.model.DegreeResult

class DegreeAuditThesisChecker extends DegreeAuditChecker {
  var entityDao: EntityDao = null

  var thesisCourseNames = "毕业论文"
  var minScore = 70f

  override def check(result: DegreeResult, program: Program): (Boolean, String) = {
    val std = result.std
    val query = OqlBuilder.from(classOf[CourseGrade], "grade")
    query.where("grade.std = :std", std)
    query.where("grade.status =:status", Grade.Status.Published)
    query.where("grade.course.name in(:courseNames)", Strings.split(thesisCourseNames))
    val grades = entityDao.search(query)
    var best: Option[Float] = None
    for (g <- grades) {
      g.score foreach { score =>
        if (best.isEmpty || score > best.get) best = Some(score)
      }
    }
    var passed = false
    if (best.nonEmpty && java.lang.Float.compare(minScore, best.get) <= 0) passed = true
    (passed, "论文成绩" + String.valueOf(best.map(_.toString).getOrElse("")))
  }

}
