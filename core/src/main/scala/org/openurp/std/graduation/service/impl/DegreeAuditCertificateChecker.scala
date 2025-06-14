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
import org.beangle.data.dao.EntityDao
import org.openurp.code.edu.model.Certificate
import org.openurp.edu.extern.model.CertificateGrade
import org.openurp.edu.program.model.Program
import org.openurp.std.graduation.domain.DegreeAuditChecker
import org.openurp.std.graduation.model.DegreeResult

class DegreeAuditCertificateChecker extends DegreeAuditChecker {
  var entityDao: EntityDao = _
  var certificateIds: String = null
  var validityYear = 5

  override def check(result: DegreeResult, program: Program): (Boolean, String) = {
    val std = result.std
    var certificates: collection.Set[Certificate] = Set.empty
    if (Strings.isNotEmpty(certificateIds)) {
      certificates = entityDao.find(classOf[Certificate], Strings.splitToInt(certificateIds)).toSet
    } else {
      certificates = program.degreeCertificates
    }
    if (certificates.isEmpty) {
      (true, "无要求")
    } else {
      val grades = entityDao.findBy(classOf[CertificateGrade], "std" -> std, "certificate" -> certificates)
      var lastDate = result.batch.graduateOn
      lastDate = lastDate.minusYears(validityYear)
      var grade: CertificateGrade = null
      var passed = false
      for (g <- grades; if !passed) {
        if (g.passed && g.acquiredIn.atEndOfMonth().isAfter(lastDate)) {
          grade = g
          passed = true
        }
      }
      if (grades.isEmpty) {
        (false, "缺少证书")
      } else {
        if (null == grade && grades.nonEmpty) grade = grades.head
        if (passed) {
          (true, s"${grade.certificate.name}${grade.scoreText}")
        } else {
          (false, s"${grade.certificate.name}${grade.scoreText}")
        }
      }
    }
  }
}
