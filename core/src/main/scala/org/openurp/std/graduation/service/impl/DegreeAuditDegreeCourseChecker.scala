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
import org.openurp.base.edu.model.Course
import org.openurp.edu.grade.domain.CourseGradeProvider
import org.openurp.edu.grade.model.CourseGrade
import org.openurp.edu.program.model.Program
import org.openurp.std.graduation.domain.DegreeAuditChecker
import org.openurp.std.graduation.model.DegreeResult

/** 学位审核--学位课程
 *
 */
class DegreeAuditDegreeCourseChecker extends DegreeAuditChecker {
  var minScore: Float = 70
  var entityDao: EntityDao = _
  var courseGradeProvider: CourseGradeProvider = _

  override def check(result: DegreeResult, program: Program): (Boolean, String) = {
    val std = result.std
    val courses = program.degreeCourses
    if (courses.isEmpty) {
      (true, "计划内没有列出学位课")
    } else {
      val grades = courseGradeProvider.get(std)
      val gradeMap = Collections.newMap[Course, CourseGrade]
      for (grade <- grades) {
        if (grade.score.nonEmpty) {
          gradeMap.get(grade.course) match {
            case None => gradeMap.put(grade.course, grade)
            case Some(g) => if (grade.score.get > g.score.get) gradeMap.put(g.course, grade)
          }
        }
      }
      var passed = true
      val sb = new StringBuilder
      for (course <- courses) {
        gradeMap.get(course) match {
          case None =>
            sb.append(course.name + "的成绩缺失")
            passed = false
          case Some(g) =>
            if (g.score.get < minScore) {
              sb.append(course.name + "的成绩是" + g.score.get + " 低于" + minScore + ";")
              passed = false
            }
            else sb.append(course.name + "的成绩是" + g.scoreText + ";")

        }

      }
      (passed, sb.toString)
    }
  }
}
