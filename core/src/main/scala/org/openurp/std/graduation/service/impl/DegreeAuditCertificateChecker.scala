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
import org.openurp.edu.extern.model.CertificateGrade
import org.openurp.edu.program.model.Program
import org.openurp.std.graduation.domain.DegreeAuditChecker
import org.openurp.std.graduation.model.DegreeResult

class DegreeAuditCertificateChecker extends DegreeAuditChecker {
  var entityDao: EntityDao = _

  override def check(result: DegreeResult, program: Program): (Boolean, String, String) = {
    val std = result.std
    if (program.degreeCertificates.isEmpty) {
      (true, "证书", "无要求")
    } else {
      val grades = entityDao.findBy(classOf[CertificateGrade], "std" -> std, "certificate" -> program.degreeCertificates)
      val passed = grades.filter(_.passed)
      if (grades.isEmpty) {
        (false, "证书", "缺少证书")
      } else {
        if (passed.isEmpty) {
          (false, "证书", s"${grades.head.certificate.name}${grades.head.scoreText}")
        } else {
          (true, "证书", s"${passed.head.certificate.name}${passed.head.scoreText}")
        }
      }
    }
  }
}
