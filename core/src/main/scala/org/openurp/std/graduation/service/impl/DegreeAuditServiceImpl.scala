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

import org.beangle.cdi.{Container, ContainerAware}
import org.beangle.commons.collection.Collections
import org.beangle.commons.lang.Strings
import org.beangle.data.dao.{EntityDao, OqlBuilder}
import org.openurp.base.std.model.Student
import org.openurp.edu.program.domain.ProgramProvider
import org.openurp.std.graduation.domain.DegreeAuditChecker
import org.openurp.std.graduation.model.{DegreeResult, GraduateBatch}
import org.openurp.std.graduation.service.DegreeAuditService

import java.time.Instant

class DegreeAuditServiceImpl extends DegreeAuditService, ContainerAware {

  var entityDao: EntityDao = _
  var checkNames: String = "plan,gpa,certificate"
  var container: Container = _

  var programProvider: ProgramProvider = _

  override def audit(result: DegreeResult): Unit = {
    result.passedItems = None
    result.failedItems = None
    val std = result.std
    programProvider.getProgram(std) match
      case None =>
        result.passed = Some(false)
        result.remark = Some("找不到培养方案")
        result.updatedAt = Instant.now
        entityDao.saveOrUpdate(result)
      case Some(program) =>
        val checkers = Strings.split(checkNames, ",").flatMap(n => container.getBean[DegreeAuditChecker]("DegreeAuditChecker." + n)).toSeq
        checkers foreach { checker =>
          val rs = checker.check(result, program)
          if (rs._1) {
            result.addPassed(rs._2, rs._3)
          } else {
            result.addFailed(rs._2, rs._3)
          }
        }
        result.updatedAt = Instant.now
        result.passed = Some(result.failedItems.isEmpty)
        result.passed foreach { p =>
          if (p) result.degree = program.degree else result.degree = None
        }
        entityDao.saveOrUpdate(result)
  }

  override def getResult(std: Student, batch: GraduateBatch): Option[DegreeResult] = {
    val query = OqlBuilder.from(classOf[DegreeResult], "result")
    query.where("result.std = :std and result.batch=:batch", std, batch)
    entityDao.search(query).headOption
  }

  override def initResult(std: Student, batch: GraduateBatch): DegreeResult = {
    val result = getResult(std, batch).getOrElse(new DegreeResult(std, batch))
    entityDao.saveOrUpdate(result)
    result
  }

  override def initResults(codes: collection.Seq[String], batch: GraduateBatch): Int = {
    val chunks = Collections.split(codes.toList, 500)
    var total = 0
    chunks foreach { chunk =>
      val query = OqlBuilder.from(classOf[Student], "std")
      query.where("std.code in(:codes)", chunk)
        .where("std.project = :project", batch.project)
      query.where(s"not exists (from ${classOf[DegreeResult].getName} gr where gr.std = std and gr.batch = :batch)", batch)
      val results = entityDao.search(query).map(new DegreeResult(_, batch))
      entityDao.saveOrUpdate(results)
      total += results.size
    }
    total
  }

  override def initResults(batch: GraduateBatch): Int = {
    val stdQuery = OqlBuilder.from(classOf[Student], "std")
    stdQuery.where("std.project = :project", batch.project)
      .where("not exists(from  " + classOf[DegreeResult].getName + " ga where ga.std=std and ga.batch = :batch)", batch)
      .where("exists(from  " + classOf[DegreeResult].getName + " ga where ga.std=std and ga.batch = :batch and ga.passed=true)", batch)
      .where("not exists(from  " + classOf[DegreeResult].getName + " ar where ar.std=std and ar.passed=true and ar.batch<>:batch)", batch)

    val results = entityDao.search(stdQuery).map(new DegreeResult(_, batch))
    entityDao.saveOrUpdate(results)
    results.size
  }
}
